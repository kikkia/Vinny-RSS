package com.rss.config.batch;

import com.rss.batch.*;
import com.rss.batch.processors.*;
import com.rss.clients.MessagingClient;
import com.rss.db.dao.RssSubscriptionRepository;
import com.rss.db.model.RssSubscriptionDTO;
import com.rss.model.RssProvider;
import com.rss.model.RssUpdate;
import com.rss.clients.HttpClient;
import com.rss.utils.DislogLogger;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
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
    private RunIdIncrementer runIdIncrementer;
    private JobRepository jobRepository;

    private DislogLogger logger = new DislogLogger(this.getClass());

    public BatchConfiguration(
            JobBuilderFactory jobBuilderFactory,
            StepBuilderFactory stepBuilderFactory,
            RssSubscriptionRepository repository,
            HttpClient httpClient,
            MessagingClient messagingClient,
            @Value("${nitter.path}") String nitterPath,
            @Value("${twitch.clientId}") String twitchClientId) {
        this.jobLauncher = getJobLauncher();
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
        this.runIdIncrementer = new RunIdIncrementer();
    }

    public RssSubscriptionReader reader(RssProvider provider) {
        return new RssSubscriptionReader(repository, provider);
    }

    public RssSubscriptionWriter writer() {return new RssSubscriptionWriter(messagingClient);}

    public Job redditRssJob() {
        return jobBuilderFactory.get("redditRssJob")
                .incrementer(runIdIncrementer)
                .flow(redditStep())
                .end()
                .build();
    }

    public Job twitterRssJob() {
        return jobBuilderFactory.get("twitterRssJob")
                .incrementer(runIdIncrementer)
                .flow(twitterStep())
                .end()
                .build();
    }

    public Job youtubeRssJob() {
        return jobBuilderFactory.get("youtubeRssJob")
                .incrementer(runIdIncrementer)
                .flow(youtubeStep())
                .end()
                .build();
    }

    public Job chanJob() {
        return jobBuilderFactory.get("chanRssJob")
                .incrementer(runIdIncrementer)
                .flow(chanStep())
                .end()
                .build();
    }

    public Job twitchJob() {
        return jobBuilderFactory.get("twitchRssJob")
                .incrementer(runIdIncrementer)
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
        getJobLauncher().run(
                redditRssJob(),
                new JobParametersBuilder()
                        .addDate("date", new Date())
                        .toJobParameters()
        );
    }

    @Scheduled(fixedRate = 1000)
    public void launchTwitterRssScan() throws Exception {
        getJobLauncher().run(
                twitterRssJob(),
                new JobParametersBuilder()
                        .addDate("date", new Date())
                        .toJobParameters()
        );
    }

    @Scheduled(fixedRate = 1000)
    public void launchYoutubeRssScan() throws Exception {
        getJobLauncher().run(
                youtubeRssJob(),
                new JobParametersBuilder()
                        .addDate("date", new Date())
                        .toJobParameters()
        );
    }

    @Scheduled(fixedRate = 1000)
    public void launchChanRssScan() throws Exception {
        getJobLauncher().run(
                chanJob(),
                new JobParametersBuilder()
                        .addDate("date", new Date())
                        .toJobParameters()
        );
    }

    @Scheduled(fixedRate = 1000)
    public void launchTwitchRssScan() throws Exception {
        getJobLauncher().run(
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

    /**
     * Overriding all of this stuff to allow us to clear out the in memory metadata repo.
     * If left unchecked this will grow forever, causing memory issues.
     * Reasons why we use in-memory over JDBC repo:
     * 1. We have a LOT of batch jobs going, this would cause non-negligible load on the db
     * 2. We have no need for resuming or retrying jobs
     * 3. No need for metadata to be persisted and take up storage
     * 4. Using a jdbc repo add latency to each job, even at <1ms of db ping, it was adding a fair amount
     */
    @Override
    protected JobLauncher createJobLauncher() throws Exception {
        SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
        jobLauncher.setJobRepository(createJobRepository());
        jobLauncher.setTaskExecutor(new SimpleAsyncTaskExecutor());
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }

    @Override
    protected JobRepository createJobRepository() throws Exception {
        MapJobRepositoryFactoryBean factoryBean = new CustomMapJobRepositoryFactoryBean();
        jobRepository = factoryBean.getObject();
        return jobRepository;
    }

    public JobRepository getJobRepository() {
        return jobRepository;
    }
}
