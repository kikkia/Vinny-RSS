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
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.sql.DataSource;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

    private DislogLogger logger = new DislogLogger(this.getClass());

    public BatchConfiguration(
            JobBuilderFactory jobBuilderFactory,
            StepBuilderFactory stepBuilderFactory,
            JobLauncher jobLauncher,
            RssSubscriptionRepository repository,
            HttpClient httpClient,
            MessagingClient messagingClient,
            @Value("${nitter.path}") String nitterPath) {
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


    // Since we do som any jobs and retry/resume is not a priority we make sure to use
    // an in-memory repo. However slowly that ends up eating memory so we extend some
    // repository functionality to allow us to clear the repo and free memory
    @Bean
    @Primary
    public JobRepository myJobRepository() {
        MapJobRepositoryFactoryBean factoryBean = new CustomMapJobRepositoryFactoryBean();
        try {
            JobRepository jobRepository = factoryBean.getObject();
            return jobRepository;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Bean
    @Primary
    public JobLauncher myJobLauncher(JobRepository jobRepository) {
        SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        return jobLauncher;
    }

    @Bean("clean-repo-executor")
    public ScheduledExecutorService scheduleRepoClean(JobRepository repository) {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(new ClearRepoJob(repository), 2, 2, TimeUnit.MINUTES);
        return executorService;
    }
}
