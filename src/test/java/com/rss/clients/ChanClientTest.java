package com.rss.clients;

import com.rss.support.SimulatedHttp;
import com.sun.net.httpserver.HttpServer;
import okhttp3.OkHttpClient;
import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ChanClientTest {
    private SimulatedHttp http;
    private MutableClock clock;
    private ChanClient client;

    @BeforeEach
    void setUp() {
        http = new SimulatedHttp();
        clock = new MutableClock();
        client = new ChanClient(http.client(), clock);
    }

    @Test
    void requestsJsonCatalogWithAnApplicationUserAgent() throws Exception {
        http.enqueue(200, "[{\"page\":1,\"threads\":[]}]");

        assertEquals(1, client.getCatalog("sci").length());
        assertEquals("https://a.4cdn.org/sci/catalog.json", http.requests().get(0).url().toString());
        assertEquals("Vinny-RSS/1.0", http.requests().get(0).header("User-Agent"));
        assertEquals("application/json", http.requests().get(0).header("Accept"));
        assertNull(http.requests().get(0).header("If-Modified-Since"));
    }

    @Test
    void spacesRequestsAcrossAllBoardsByAtLeastFiveSeconds() throws Exception {
        http.enqueue(200, "[]");
        http.enqueue(200, "[]");
        client.getCatalog("sci");

        assertNull(client.getCatalog("g"));
        clock.advanceMillis(4999);
        assertNull(client.getCatalog("g"));
        assertEquals(1, http.requests().size());

        clock.advanceMillis(1);
        assertNotNull(client.getCatalog("g"));
        assertEquals(2, http.requests().size());
    }

    @Test
    void failedRequestsAlsoConsumeTheFiveSecondSlot() throws Exception {
        http.enqueue(404, "<html>Not found</html>");
        http.enqueue(200, "[]");

        IOException failure = assertThrows(IOException.class, () -> client.getCatalog("sci"));
        assertTrue(failure.getMessage().contains("404"));
        assertNull(client.getCatalog("g"));
        assertEquals(1, http.requests().size());
        clock.advanceMillis(5000);
        assertNotNull(client.getCatalog("g"));
    }

    @Test
    void reusesUnchangedCatalogsAndKeepsConditionalHeadersSeparatePerBoard() throws Exception {
        String sciModified = "Wed, 11 Nov 2026 10:00:00 GMT";
        String gModified = "Wed, 11 Nov 2026 11:00:00 GMT";
        http.enqueue(200, "[{\"page\":1,\"threads\":[]}]", "Last-Modified", sciModified);
        http.enqueue(200, "[]", "Last-Modified", gModified);
        http.enqueue(304, "");
        JSONArray sci = client.getCatalog("sci");

        clock.advanceMillis(5000);
        client.getCatalog("g");
        assertNull(http.requests().get(1).header("If-Modified-Since"));
        clock.advanceMillis(5000);
        assertSame(sci, client.getCatalog("sci"));
        assertEquals(sciModified, http.requests().get(2).header("If-Modified-Since"));
    }

    @Test
    void rejectsUnchangedResponseWithoutACachedSnapshot() {
        http.enqueue(304, "");
        assertThrows(IOException.class, () -> client.getCatalog("sci"));
    }

    @Test
    void malformedJsonDoesNotPoisonTheConditionalCache() throws Exception {
        http.enqueue(200, "<html>Not JSON</html>", "Last-Modified", "Wed, 11 Nov 2026 10:00:00 GMT");
        http.enqueue(200, "[]");
        assertThrows(IOException.class, () -> client.getCatalog("sci"));

        clock.advanceMillis(5000);
        assertNotNull(client.getCatalog("sci"));
        assertNull(http.requests().get(1).header("If-Modified-Since"));
    }

    @Test
    void honorsRetryAfterSecondsAcrossAllBoards() throws Exception {
        http.enqueue(429, "Rate limited", "Retry-After", "12");
        http.enqueue(200, "[]");
        assertThrows(IOException.class, () -> client.getCatalog("sci"));

        clock.advanceMillis(5000);
        assertNull(client.getCatalog("g"));
        clock.advanceMillis(6999);
        assertNull(client.getCatalog("g"));
        assertEquals(1, http.requests().size());
        clock.advanceMillis(1);
        assertNotNull(client.getCatalog("g"));
    }

    @Test
    void honorsRetryAfterHttpDates() throws Exception {
        String retryAfter = DateTimeFormatter.RFC_1123_DATE_TIME.format(
                clock.instant().plusSeconds(30).atZone(ZoneOffset.UTC));
        http.enqueue(503, "Unavailable", "Retry-After", retryAfter);
        http.enqueue(200, "[]");
        assertThrows(IOException.class, () -> client.getCatalog("sci"));

        clock.advanceMillis(29999);
        assertNull(client.getCatalog("g"));
        clock.advanceMillis(1);
        assertNotNull(client.getCatalog("g"));
    }

    @Test
    void missingOrInvalidRetryAfterBacksOffForOneMinute() throws Exception {
        for (String retryAfter : new String[]{null, "invalid", "-1", "999999999999999999999"}) {
            setUp();
            if (retryAfter == null) http.enqueue(429, "Rate limited");
            else http.enqueue(429, "Rate limited", "Retry-After", retryAfter);
            http.enqueue(200, "[]");
            assertThrows(IOException.class, () -> client.getCatalog("sci"));

            clock.advanceMillis(59999);
            assertNull(client.getCatalog("g"));
            clock.advanceMillis(1);
            assertNotNull(client.getCatalog("g"));
        }
    }

    @Test
    void zeroRetryAfterCannotBypassTheFiveSecondMinimum() throws Exception {
        http.enqueue(429, "Rate limited", "Retry-After", "0");
        http.enqueue(200, "[]");
        assertThrows(IOException.class, () -> client.getCatalog("sci"));
        clock.advanceMillis(4999);
        assertNull(client.getCatalog("g"));
        clock.advanceMillis(1);
        assertNotNull(client.getCatalog("g"));
    }

    @Test
    void invalidBoardNamesCannotChangeTheRequestPathOrHost() throws Exception {
        for (String board : new String[]{"../g", "sci/", "https://example.com", "", "sci?x=1"}) {
            assertThrows(IllegalArgumentException.class, () -> client.getCatalog(board));
        }
        assertTrue(http.requests().isEmpty());
        http.enqueue(200, "[]");
        assertNotNull(client.getCatalog("r9k"));
    }

    @Test
    void overlappingScansCannotMakeConcurrentRequests() throws Exception {
        http.enqueue(200, "[]");
        http.enqueue(200, "[]");
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        OkHttpClient transport = new OkHttpClient.Builder().addInterceptor(chain -> {
            started.countDown();
            try {
                if (!release.await(5, TimeUnit.SECONDS)) throw new IOException("Test response timed out");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException(e);
            }
            return http.intercept(chain);
        }).build();
        ChanClient shared = new ChanClient(transport, clock);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<JSONArray> first = executor.submit(() -> shared.getCatalog("sci"));
            assertTrue(started.await(5, TimeUnit.SECONDS));
            clock.advanceMillis(5000);
            Future<JSONArray> second = executor.submit(() -> shared.getCatalog("g"));
            assertThrows(TimeoutException.class, () -> second.get(100, TimeUnit.MILLISECONDS));
            release.countDown();
            assertNotNull(first.get(5, TimeUnit.SECONDS));
            assertNotNull(second.get(5, TimeUnit.SECONDS));
            assertEquals(2, http.requests().size());
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void preventsOkHttpFromImmediatelyRetrying503WithZeroRetryAfter() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/sci/catalog.json", exchange -> {
            requests.incrementAndGet();
            exchange.getResponseHeaders().set("Retry-After", "0");
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
        });
        server.start();
        OkHttpClient transport = new OkHttpClient.Builder().addInterceptor(chain -> chain.proceed(
                chain.request().newBuilder().url("http://127.0.0.1:" + server.getAddress().getPort()
                        + chain.request().url().encodedPath()).build())).build();
        try {
            ChanClient local = new ChanClient(transport, clock);
            assertThrows(IOException.class, () -> local.getCatalog("sci"));
            assertEquals(1, requests.get());
            assertNull(local.getCatalog("g"));
        } finally {
            server.stop(0);
            transport.connectionPool().evictAll();
        }
    }

    private static class MutableClock extends Clock {
        private volatile Instant now = Instant.parse("2026-11-11T12:00:00Z");

        void advanceMillis(long millis) {
            now = now.plusMillis(millis);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(now, zone);
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
