package com.rss.batch;

import com.rss.config.batch.BatchConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Configuration
public class BatchRepoConfig {

    @Bean("clean-repo-executor")
    public ScheduledExecutorService scheduleRepoClean(BatchConfiguration configurer) {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(new ClearRepoJob(configurer.getJobRepository()), 10, 10, TimeUnit.MINUTES);
        return executorService;
    }
}
