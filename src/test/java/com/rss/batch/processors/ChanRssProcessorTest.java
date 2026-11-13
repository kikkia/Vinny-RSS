package com.rss.batch.processors;

import com.rss.clients.ChanClient;
import com.rss.db.dao.RssSubscriptionRepository;
import com.rss.db.model.RssChannelSubscriptionDTO;
import com.rss.db.model.RssSubscriptionDTO;
import com.rss.model.RssProvider;
import com.rss.model.RssUpdate;
import com.rss.service.MetricsService;
import com.rss.support.SimulatedHttp;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChanRssProcessorTest {
    private SimulatedHttp http;
    private RecordingRepository repository;
    private RecordingMetrics metrics;
    private ChanRssProcessor processor;
    private RssSubscriptionDTO subscription;

    @BeforeEach
    void setUp() {
        http = new SimulatedHttp();
        repository = new RecordingRepository();
        metrics = new RecordingMetrics();
        processor = new ChanRssProcessor(repository, new ChanClient(http.client()), metrics);
        subscription = new RssSubscriptionDTO(7, "sci", RssProvider.CHAN.getValue(),
                Instant.ofEpochSecond(90), Instant.ofEpochSecond(100), null);
    }

    @Test
    void scansEveryCatalogPageAndIgnoresOldOrMerelyBumpedThreads() throws Exception {
        http.enqueue(200, "["
                + "{\"page\":1,\"threads\":["
                + "{\"no\":10,\"time\":99,\"last_modified\":200,\"sub\":\"Old bumped thread\"},"
                + "{\"no\":11,\"time\":100,\"sub\":\"Already seen\"},"
                + "{\"no\":12,\"time\":101,\"sub\":\"New thread\"}]},"
                + "{\"page\":2,\"threads\":[{\"no\":3000000000,\"time\":102,\"sub\":\"Another thread\"}]}]");
        repository.channels = Arrays.asList(channel("all-a", null), channel("all-b", null));

        List<RssUpdate> updates = processor.process(subscription);

        assertEquals(4, updates.size());
        assertEquals("https://boards.4chan.org/sci/thread/12", updates.get(0).getUrl());
        assertEquals("https://boards.4chan.org/sci/thread/3000000000", updates.get(2).getUrl());
        assertEquals("all-a", updates.get(0).getChannel());
        assertEquals("all-b", updates.get(1).getChannel());
        for (RssUpdate update : updates) {
            assertEquals(7, update.getId());
            assertEquals(RssProvider.CHAN.getValue(), update.getProvider());
            assertEquals("sci", update.getSubject());
            assertEquals("/sci/", update.getDisplayName());
        }
        assertEquals(1, repository.completed);
        assertEquals(Instant.ofEpochSecond(102), repository.completedAt);
        assertEquals(0, metrics.failures);
    }

    @Test
    void cachedCatalogsCheckpointTheNewestObservedThreadNotTheCurrentTime() throws Exception {
        http.enqueue(200, "[{\"page\":1,\"threads\":[{\"no\":12,\"time\":101}]}]");

        assertNotNull(processor.process(subscription));
        assertEquals(Instant.ofEpochSecond(101), repository.completedAt);
        // A thread at time 102 may be absent from this cached snapshot. Keeping
        // the checkpoint at 101 leaves it eligible when it appears later.
        assertTrue(Instant.ofEpochSecond(102).isAfter(repository.completedAt));
    }

    @Test
    void preservesCaseSensitiveKeywordFiltersAndDecodesHtmlSubjects() throws Exception {
        http.enqueue(200, "[{\"page\":1,\"threads\":[{\"no\":12,\"time\":101,"
                + "\"sub\":\"Tom&#039;s &amp; <b>quantum</b>\",\"com\":\"Not the title\"}]}]");
        repository.channels = Arrays.asList(
                channel("all", null),
                channel("match", "Tom's & quantum"),
                channel("case-mismatch", "Quantum"),
                channel("body-only", "Not the title"));

        List<RssUpdate> updates = processor.process(subscription);

        assertEquals(2, updates.size());
        assertEquals("all", updates.get(0).getChannel());
        assertEquals("match", updates.get(1).getChannel());
    }

    @Test
    void subjectlessThreadsUseTheirCommentForKeywordMatching() throws Exception {
        http.enqueue(200, "[{\"page\":1,\"threads\":[{\"no\":12,\"time\":101,"
                + "\"com\":\"<span class=\\\"quote\\\">&gt;quantum</span><br>physics\"}]}]");
        repository.channels = Arrays.asList(channel("match", ">quantum\nphysics"), channel("miss", "chemistry"));

        List<RssUpdate> updates = processor.process(subscription);

        assertEquals(1, updates.size());
        assertEquals("match", updates.get(0).getChannel());
    }

    @Test
    void threadsWithoutSubjectOrCommentStillReachUnfilteredChannels() throws Exception {
        http.enqueue(200, "[{\"page\":1,\"threads\":[{\"no\":12,\"time\":101}]}]");
        repository.channels = Arrays.asList(channel("all", null), channel("filtered", "quantum"));

        List<RssUpdate> updates = processor.process(subscription);

        assertEquals(1, updates.size());
        assertEquals("all", updates.get(0).getChannel());
    }

    @Test
    void emptyOldAndNonMatchingCatalogsDoNotAdvanceTheWatermark() throws Exception {
        for (String catalog : new String[]{
                "[]",
                "[{\"page\":1,\"threads\":[]}]",
                "[{\"page\":1,\"threads\":[{\"no\":12,\"time\":99}]}]",
                "[{\"page\":1,\"threads\":[{\"no\":12,\"time\":101,\"sub\":\"Not a match\"}]}]"}) {
            setUp();
            repository.channels = Collections.singletonList(channel("filtered", "quantum"));
            http.enqueue(200, catalog);
            assertNull(processor.process(subscription));
            assertEquals(0, repository.completed);
            assertEquals(0, metrics.failures);
        }
    }

    @Test
    void malformedCatalogsFailWithoutAdvancingTheWatermark() {
        for (String catalog : new String[]{
                "[{}]",
                "[{\"threads\":[{\"no\":12}]}]",
                "[{\"threads\":[{\"time\":101}]}]",
                "[{\"threads\":[{\"no\":0,\"time\":101}]}]",
                "[{\"threads\":[{\"no\":12,\"time\":0}]}]"}) {
            setUp();
            http.enqueue(200, catalog);
            assertThrows(JSONException.class, () -> processor.process(subscription));
            assertEquals(0, repository.completed);
            assertEquals(1, metrics.failures);
            assertEquals(RssProvider.CHAN, metrics.failedProvider);
        }
    }

    @Test
    void httpFailuresAreRecordedWithoutDeletingTheSubscription() {
        http.enqueue(404, "<html>Not found</html>");

        assertThrows(IOException.class, () -> processor.process(subscription));
        assertEquals(0, repository.completed);
        assertEquals(0, repository.channelReads);
        assertEquals(1, metrics.failures);
    }

    @Test
    void databaseFailureDoesNotReturnNotifications() throws Exception {
        http.enqueue(200, "[{\"page\":1,\"threads\":[{\"no\":12,\"time\":101}]}]");
        repository.completeSucceeds = false;

        assertNull(processor.process(subscription));
        assertEquals(1, repository.completed);
        assertEquals(1, metrics.failures);
    }

    @Test
    void deferredScansDoNotTouchTheDatabaseOrCountAsFailures() throws Exception {
        http.enqueue(200, "[]");
        assertNull(processor.process(subscription));
        int channelReads = repository.channelReads;

        assertNull(processor.process(subscription));
        assertEquals(1, http.requests().size());
        assertEquals(channelReads, repository.channelReads);
        assertEquals(0, repository.completed);
        assertEquals(0, metrics.failures);
    }

    private static RssChannelSubscriptionDTO channel(String id, String keyword) {
        return new RssChannelSubscriptionDTO(1, 7, id, keyword);
    }

    private static class RecordingRepository extends RssSubscriptionRepository {
        private List<RssChannelSubscriptionDTO> channels = Collections.singletonList(channel("all", null));
        private int completed;
        private Instant completedAt;
        private int channelReads;
        private boolean completeSucceeds = true;

        @Override
        public List<RssChannelSubscriptionDTO> getChannelSubsForSubcriptionId(int subscriptionId) {
            assertEquals(7, subscriptionId);
            channelReads++;
            return channels;
        }

        @Override
        public boolean updateLastScanComplete(int id) {
            fail("4chan must checkpoint the observed catalog, not wall-clock time");
            return false;
        }

        @Override
        public boolean updateLastScanComplete(int id, Instant completedAt) {
            assertEquals(7, id);
            completed++;
            this.completedAt = completedAt;
            return completeSucceeds;
        }

        @Override
        public void delete(int id) {
            fail("A failed board request must not delete subscriptions");
        }
    }

    private static class RecordingMetrics extends MetricsService {
        private int failures;
        private RssProvider failedProvider;

        private RecordingMetrics() {
            super(null);
        }

        @Override
        public void markExecutionFailed(RssProvider provider) {
            failures++;
            failedProvider = provider;
        }
    }
}
