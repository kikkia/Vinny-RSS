package com.rss.batch;

import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Configuration
public class BatchRepoConfig {

    // Since we do som any jobs and retry/resume is not a priority we make sure to use
    // an in-memory repo. However slowly that ends up eating memory so we extend some
    // repository functionality to allow us to clear the repo and free memory
    @Bean("rss-repo")
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

    @Bean("rss-launcher")
    public JobLauncher myJobLauncher(JobRepository jobRepository) {
        SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.setTaskExecutor(new SimpleAsyncTaskExecutor());
        return jobLauncher;
    }

    @Bean("clean-repo-executor")
    public ScheduledExecutorService scheduleRepoClean(@Qualifier("rss-repo") JobRepository repository) {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(new ClearRepoJob(repository), 2, 2, TimeUnit.MINUTES);
        return executorService;
    }
}
