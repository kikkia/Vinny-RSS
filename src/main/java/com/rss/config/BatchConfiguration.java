package com.rss.config;

import com.rss.batch.RedditRssProcessor;
import com.rss.batch.RssSubscriptionReader;
import com.rss.batch.RssSubscriptionWriter;
import com.rss.db.dao.RssSubscriptionRepository;
import com.rss.db.model.RssSubscriptionDTO;
import com.rss.model.RssProvider;
import com.rss.model.RssUpdate;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

@Configuration
@EnableBatchProcessing
@EnableScheduling
public class BatchConfiguration {

    private JobBuilderFactory jobBuilderFactory;
    private StepBuilderFactory stepBuilderFactory;
    private JobLauncher jobLauncher;
    private RssSubscriptionRepository repository;

    public BatchConfiguration(
            JobBuilderFactory jobBuilderFactory,
            StepBuilderFactory stepBuilderFactory,
            JobLauncher jobLauncher,
            RssSubscriptionRepository repository) {
        this.jobLauncher =  jobLauncher;
        this.stepBuilderFactory = stepBuilderFactory;
        this.repository = repository;
        this.jobBuilderFactory = jobBuilderFactory;
    }

    @Bean
    public RedditRssProcessor redditRssProcessor(RssSubscriptionRepository repository) {
        return new RedditRssProcessor(repository);
    }

    @Bean
    @StepScope
    public RssSubscriptionReader reader(@Value("#{jobParameter['provider']}}") Long provider) {
        return new RssSubscriptionReader(repository, provider);
    }

    @Bean
    public RssSubscriptionWriter writer() {return new RssSubscriptionWriter();}

    @Bean
    public Job redditRssJob(@Qualifier(value = "redditStep") Step redditStep) {
        return jobBuilderFactory.get("redditRssJob")
                .incrementer(new RunIdIncrementer())
                .flow(redditStep)
                .end()
                .build();
    }

    @Bean("redditStep")
    public Step redditStep(RssSubscriptionRepository repository) {
        return stepBuilderFactory.get("redditStep")
                .<RssSubscriptionDTO, List<RssUpdate>> chunk(1)
                .reader(reader(1L))
                .processor(redditRssProcessor(repository))
                .writer(writer())
                .build();
    }

    @Scheduled(fixedRate = 2000)
    public void launchRedditRssScan() throws Exception {
        jobLauncher.run(
                redditRssJob(redditStep(repository)),
                new JobParametersBuilder().addLong("provider", (long) RssProvider.REDDIT.getValue()).toJobParameters()
        );
    }
}
