package com.rss.batch.processors;

import com.rss.clients.ChanClient;
import com.rss.db.dao.RssSubscriptionRepository;
import com.rss.db.model.RssChannelSubscriptionDTO;
import com.rss.db.model.RssSubscriptionDTO;
import com.rss.model.RssProvider;
import com.rss.model.RssUpdate;
import com.rss.service.MetricsService;
import com.rss.utils.DislogLogger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.web.util.HtmlUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ChanRssProcessor implements ItemProcessor<RssSubscriptionDTO, List<RssUpdate>> {

    private final RssSubscriptionRepository repository;
    private final ChanClient client;
    private final MetricsService metricsService;
    private final DislogLogger logger = new DislogLogger(this.getClass());

    public ChanRssProcessor(RssSubscriptionRepository repository, ChanClient client, MetricsService metricsService) {
        this.repository = repository;
        this.client = client;
        this.metricsService = metricsService;
    }

    @Override
    public List<RssUpdate> process(RssSubscriptionDTO rssSubscriptionDTO) throws Exception {
        try {
            JSONArray pages = client.getCatalog(rssSubscriptionDTO.getUrl());
            if (pages == null) return null;

            Instant lastScan = rssSubscriptionDTO.getLastScanComplete();
            Instant newestThread = lastScan;
            List<RssChannelSubscriptionDTO> subs = repository.getChannelSubsForSubcriptionId(rssSubscriptionDTO.getId());
            ArrayList<RssUpdate> toUpdate = new ArrayList<>();
            for (int page = 0; page < pages.length(); page++) {
                JSONArray threads = pages.getJSONObject(page).getJSONArray("threads");
                for (int index = 0; index < threads.length(); index++) {
                    JSONObject thread = threads.getJSONObject(index);
                    long id = thread.getLong("no");
                    long timestamp = thread.getLong("time");
                    if (id <= 0 || timestamp <= 0) throw new JSONException("Invalid 4chan thread ID or timestamp");
                    // A bump changes last_modified, not the thread's creation time.
                    Instant posted = Instant.ofEpochSecond(timestamp);
                    if (!posted.isAfter(lastScan)) continue;
                    if (posted.isAfter(newestThread)) newestThread = posted;

                    String title = thread.optString("sub");
                    if (title.isEmpty()) title = thread.optString("com");
                    title = HtmlUtils.htmlUnescape(title
                            .replaceAll("(?i)<br\\s*/?>", "\n")
                            .replaceAll("<[^>]*>", ""));
                    for (RssChannelSubscriptionDTO dto : subs) {
                        if (dto.getKeyword() == null || title.contains(dto.getKeyword())) {
                            toUpdate.add(new RssUpdate(
                                    rssSubscriptionDTO.getId(),
                                    dto.getChannelId(),
                                    "https://boards.4chan.org/" + rssSubscriptionDTO.getUrl() + "/thread/" + id,
                                    rssSubscriptionDTO.getProvider(),
                                    rssSubscriptionDTO.getUrl(),
                                    "/" + rssSubscriptionDTO.getUrl() + "/"
                            ));
                        }
                    }
                }
            }
            // Preserve the watermark when there are no matching notifications.
            if (toUpdate.isEmpty()) return null;
            // Catalogs can be cached upstream: wall-clock time could skip threads
            // that were created before this scan but appear in a later snapshot.
            if (!repository.updateLastScanComplete(rssSubscriptionDTO.getId(), newestThread)) {
                logger.error("Failed to mark the last completed time, failing job");
                metricsService.markExecutionFailed(RssProvider.CHAN);
                return null;
            }
            return toUpdate;
        } catch (Exception e) {
            logger.error("Failed to scan 4chan board " + rssSubscriptionDTO.getUrl(), e);
            metricsService.markExecutionFailed(RssProvider.CHAN);
            throw e;
        }
    }
}
