package com.rss.config.batch;

import com.rss.batch.*;
import com.rss.clients.MessagingClient;
import com.rss.db.dao.RssSubscriptionRepository;
import com.rss.db.model.RssSubscriptionDTO;
import com.rss.model.RssProvider;
import com.rss.model.RssUpdate;
import com.rss.clients.HttpClient;
import com.rss.utils.DislogLogger;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.sql.DataSource;
import java.util.Date;
import java.util.List;

@Configuration
@EnableAutoConfiguration
@EnableBatchProcessing
@EnableScheduling
public class BatchConfiguration extends DefaultBatchConfigurer {

    private JobBuilderFactory jobBuilderFactory;
    private StepBuilderFactory stepBuilderFactory;
    private JobLauncher jobLauncher;
    private RssSubscriptionRepository repository;
    private HttpClient httpClient;
    private MessagingClient messagingClient;
    private RedditRssProcessor redditRssProcessor;
    private TwitterRssProcessor twitterRssProcessor;
    private YoutubeRssProcessor youtubeRssProcessor;
    private ChanRssProcessor chanRssProcessor;
    private TwitchRssProcessor twitchRssProcessor;

    private DislogLogger logger = new DislogLogger(this.getClass());

    public BatchConfiguration(
            JobBuilderFactory jobBuilderFactory,
            StepBuilderFactory stepBuilderFactory,
            @Qualifier("rss-launcher") JobLauncher jobLauncher,
            RssSubscriptionRepository repository,
            HttpClient httpClient,
            MessagingClient messagingClient,
            @Value("${nitter.path}") String nitterPath,
            @Value("${twitch.clientId}") String twitchClientId) {
        this.jobLauncher =  jobLauncher;
        this.stepBuilderFactory = stepBuilderFactory;
        this.repository = repository;
        this.jobBuilderFactory = jobBuilderFactory;
        this.httpClient = httpClient;
        this.messagingClient = messagingClient;
        this.redditRssProcessor = new RedditRssProcessor(repository, httpClient);
        this.chanRssProcessor = new ChanRssProcessor(repository);
        this.youtubeRssProcessor = new YoutubeRssProcessor(repository);
        this.twitterRssProcessor = new TwitterRssProcessor(repository, nitterPath);
        this.twitchRssProcessor = new TwitchRssProcessor(repository, twitchClientId, httpClient);
    }

    public RssSubscriptionReader reader(RssProvider provider) {
        return new RssSubscriptionReader(repository, provider);
    }

    public RssSubscriptionWriter writer() {return new RssSubscriptionWriter(messagingClient);}

    public Job redditRssJob() {
        return jobBuilderFactory.get("redditRssJob")
                .incrementer(new RunIdIncrementer())
                .flow(redditStep())
                .end()
                .build();
    }

    public Job twitterRssJob() {
        return jobBuilderFactory.get("twitterRssJob")
                .incrementer(new RunIdIncrementer())
                .flow(twitterStep())
                .end()
                .build();
    }

    public Job youtubeRssJob() {
        return jobBuilderFactory.get("youtubeRssJob")
                .incrementer(new RunIdIncrementer())
                .flow(youtubeStep())
                .end()
                .build();
    }

    public Job chanJob() {
        return jobBuilderFactory.get("chanRssJob")
                .incrementer(new RunIdIncrementer())
                .flow(chanStep())
                .end()
                .build();
    }

    public Job twitchJob() {
        return jobBuilderFactory.get("twitchRssJob")
                .incrementer(new RunIdIncrementer())
                .flow(twitchStep())
                .end()
                .build();
    }

    public Step redditStep() {
        return stepBuilderFactory.get("redditStep")
                .<RssSubscriptionDTO, List<RssUpdate>>chunk(1)
                .reader(reader(RssProvider.REDDIT))
                .processor(this.redditRssProcessor)
                .writer(writer())
                .build();
    }

    public Step twitterStep() {
        return stepBuilderFactory.get("twitterStep")
                .<RssSubscriptionDTO, List<RssUpdate>> chunk(1)
                .reader(reader(RssProvider.TWITTER))
                .processor(this.twitterRssProcessor)
                .writer(writer())
                .build();
    }

    public Step youtubeStep() {
        return stepBuilderFactory.get("youtubeStep")
                .<RssSubscriptionDTO, List<RssUpdate>>chunk(1)
                .reader(reader(RssProvider.YOUTUBE))
                .processor(this.youtubeRssProcessor)
                .writer(writer())
                .build();
    }

    public Step chanStep() {
        return stepBuilderFactory.get("chanStep")
                .<RssSubscriptionDTO, List<RssUpdate>>chunk(1)
                .reader(reader(RssProvider.CHAN))
                .processor(this.chanRssProcessor)
                .writer(writer())
                .build();
    }

    public Step twitchStep() {
        return stepBuilderFactory.get("twitchStep")
                .<RssSubscriptionDTO, List<RssUpdate>>chunk(1)
                .reader(reader(RssProvider.TWITCH))
                .processor(this.twitchRssProcessor)
                .writer(writer())
                .build();
    }

    @Scheduled(fixedRate = 1000)
    public void launchRedditRssScan() throws Exception {
        jobLauncher.run(
                redditRssJob(),
                new JobParametersBuilder()
                        .addDate("date", new Date())
                        .toJobParameters()
        );
    }

    @Scheduled(fixedRate = 1000)
    public void launchTwitterRssScan() throws Exception {
        jobLauncher.run(
                twitterRssJob(),
                new JobParametersBuilder()
                        .addDate("date", new Date())
                        .toJobParameters()
        );
    }

    @Scheduled(fixedRate = 1000)
    public void launchYoutubeRssScan() throws Exception {
        jobLauncher.run(
                youtubeRssJob(),
                new JobParametersBuilder()
                        .addDate("date", new Date())
                        .toJobParameters()
        );
    }

    @Scheduled(fixedRate = 1000)
    public void launchChanRssScan() throws Exception {
        jobLauncher.run(
                chanJob(),
                new JobParametersBuilder()
                        .addDate("date", new Date())
                        .toJobParameters()
        );
    }

    @Scheduled(fixedRate = 1000)
    public void launchTwitchRssScan() throws Exception {
        jobLauncher.run(
                twitchJob(),
                new JobParametersBuilder()
                        .addDate("date", new Date())
                        .toJobParameters()
        );
    }

    @Override
    public void setDataSource(DataSource dataSource) {
        // override to do not set datasource even if a datasource exist.
        // initialize will use a Map based JobRepository (instead of database)
    }
}
