package com.rss.config;

import com.rss.batch.RedditRssProcessor;
import com.rss.batch.RssSubscriptionReader;
import com.rss.db.model.RssSubscriptionDTO;
import com.rss.model.RssUpdate;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration {

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Bean
    public RedditRssProcessor redditRssProcessor() {
        return new RedditRssProcessor();
    }

    @Bean
    public RssSubscriptionReader reader() {
        return new RssSubscriptionReader();
    }

    @Bean
    public Job redditRssJob(@Qualifier(value = "redditStep") Step redditStep) {
        return jobBuilderFactory.get("redditRssJob")
                .incrementer(new RunIdIncrementer())
                .flow(redditStep)
                .end()
                .build();
    }

    @Bean("redditStep")
    public Step redditStep() {
        return stepBuilderFactory.get("redditStep")
                .<RssSubscriptionDTO, RssUpdate> chunk(1)
                .reader(reader())
                .processor(redditRssProcessor())
                .build();
    }

}
