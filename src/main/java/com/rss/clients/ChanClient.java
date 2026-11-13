package com.rss.clients;

import com.rss.utils.RssUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONArray;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class ChanClient {
    public static final long SCAN_INTERVAL_MILLIS = 5000;

    private final OkHttpClient client;
    private final Clock clock;
    private final Map<String, Catalog> catalogs = new HashMap<>();
    private Instant nextRequestAt = Instant.EPOCH;

    @Autowired
    public ChanClient(OkHttpClient client) {
        this(client, Clock.systemUTC());
    }

    ChanClient(OkHttpClient client, Clock clock) {
        this.client = client.newBuilder()
                .followRedirects(false)
                .followSslRedirects(false)
                .retryOnConnectionFailure(false)
                // OkHttp can still retry HTTP 503/Retry-After: 0 internally.
                // Allow only one actual network exchange per scheduled scan.
                .addNetworkInterceptor(chain -> {
                    RequestAttempt attempt = chain.request().tag(RequestAttempt.class);
                    if (attempt == null || attempt.sent) {
                        throw new IOException("4chan automatic retry deferred until the next scan");
                    }
                    attempt.sent = true;
                    return chain.proceed(chain.request());
                })
                .callTimeout(20, TimeUnit.SECONDS)
                .build();
        this.clock = clock;
    }

    // One shared gate across every board, including overlapping batch jobs.
    // A deferred scan returns null without sleeping or making a network request.
    public synchronized JSONArray getCatalog(String board) throws IOException {
        String url = RssUtils.Companion.get4ChanCatalogUrl(board);
        Instant now = clock.instant();
        if (now.isBefore(nextRequestAt)) return null;
        nextRequestAt = now.plusMillis(SCAN_INTERVAL_MILLIS);

        Catalog cached = catalogs.get(board);
        Request.Builder request = new Request.Builder()
                .url(url)
                .tag(RequestAttempt.class, new RequestAttempt())
                .header("User-Agent", "Vinny-RSS/1.0")
                .header("Accept", "application/json");
        if (cached != null && cached.lastModified != null) {
            request.header("If-Modified-Since", cached.lastModified);
        }

        try (Response response = client.newCall(request.build()).execute()) {
            if (response.code() == 429 || response.code() == 503) {
                Instant minimum = clock.instant().plusMillis(SCAN_INTERVAL_MILLIS);
                Instant retryAt = retryAt(response.header("Retry-After"));
                nextRequestAt = retryAt.isAfter(minimum) ? retryAt : minimum;
            }
            if (response.code() == 304) {
                if (cached == null) throw new IOException("4chan returned HTTP 304 without a cached catalog");
                // Reprocess the cached snapshot: a previous processing/database failure
                // must not cause an unchanged response to lose notifications.
                return cached.pages;
            }
            if (response.code() != 200) {
                throw new IOException("4chan catalog request failed (HTTP " + response.code() + ")");
            }
            ResponseBody body = response.body();
            if (body == null) throw new IOException("4chan catalog response had no body");
            JSONArray pages;
            try {
                pages = new JSONArray(body.string());
            } catch (JSONException e) {
                throw new IOException("4chan catalog response was not a JSON array");
            }
            catalogs.put(board, new Catalog(pages, response.header("Last-Modified")));
            return pages;
        }
    }

    private Instant retryAt(String retryAfter) {
        Instant now = clock.instant();
        if (retryAfter != null) {
            String value = retryAfter.trim();
            try {
                long seconds = Long.parseLong(value);
                if (seconds >= 0) return now.plusSeconds(seconds);
            } catch (NumberFormatException | DateTimeException ignored) {
                // Retry-After may be an HTTP date rather than a number of seconds.
            }
            try {
                return ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
            } catch (DateTimeException ignored) {
                // Missing/invalid delays use a conservative one-minute backoff.
            }
        }
        return now.plusSeconds(60);
    }

    private static class RequestAttempt {
        private boolean sent;
    }

    private static class Catalog {
        private final JSONArray pages;
        private final String lastModified;

        private Catalog(JSONArray pages, String lastModified) {
            this.pages = pages;
            this.lastModified = lastModified;
        }
    }
}
