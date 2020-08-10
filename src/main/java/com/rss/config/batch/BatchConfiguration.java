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

    private DislogLogger logger = new DislogLogger(this.getClass());

    @Value("${nitter.path}")
    private String nitterPath;

    public BatchConfiguration(
            JobBuilderFactory jobBuilderFactory,
            StepBuilderFactory stepBuilderFactory,
            JobLauncher jobLauncher,
            RssSubscriptionRepository repository,
            HttpClient httpClient,
            MessagingClient messagingClient) {
        this.jobLauncher =  jobLauncher;
        this.stepBuilderFactory = stepBuilderFactory;
        this.repository = repository;
        this.jobBuilderFactory = jobBuilderFactory;
        this.httpClient = httpClient;
        this.messagingClient = messagingClient;
    }

    public RedditRssProcessor redditRssProcessor(RssSubscriptionRepository repository) {
        return new RedditRssProcessor(repository, httpClient);
    }

    public TwitterRssProcessor twitterRssProcessor(RssSubscriptionRepository repository) {
        return new TwitterRssProcessor(repository, nitterPath);
    }

    public YoutubeRssProcessor youtubeRssProcessor(RssSubscriptionRepository repository) {
        return new YoutubeRssProcessor(repository);
    }

    public ChanRssProcessor chanRssProcessor(RssSubscriptionRepository repository) {
        return new ChanRssProcessor(repository);
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

    public Step redditStep() {
        return stepBuilderFactory.get("redditStep")
                .<RssSubscriptionDTO, List<RssUpdate>>chunk(1)
                .reader(reader(RssProvider.REDDIT))
                .processor(redditRssProcessor(repository))
                .writer(writer())
                .build();
    }

    public Step twitterStep() {
        return stepBuilderFactory.get("twitterStep")
                .<RssSubscriptionDTO, List<RssUpdate>> chunk(1)
                .reader(reader(RssProvider.TWITTER))
                .processor(twitterRssProcessor(repository))
                .writer(writer())
                .build();
    }

    public Step youtubeStep() {
        return stepBuilderFactory.get("youtubeStep")
                .<RssSubscriptionDTO, List<RssUpdate>>chunk(1)
                .reader(reader(RssProvider.YOUTUBE))
                .processor(youtubeRssProcessor(repository))
                .writer(writer())
                .build();
    }

    public Step chanStep() {
        return stepBuilderFactory.get("chanStep")
                .<RssSubscriptionDTO, List<RssUpdate>>chunk(1)
                .reader(reader(RssProvider.CHAN))
                .processor(chanRssProcessor(repository))
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

    @Override
    public void setDataSource(DataSource dataSource) {
        // override to do not set datasource even if a datasource exist.
        // initialize will use a Map based JobRepository (instead of database)
    }
}
