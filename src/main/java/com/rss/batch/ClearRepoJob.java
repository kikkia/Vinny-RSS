package com.rss.batch;

import org.springframework.batch.core.repository.JobRepository;

public class ClearRepoJob implements Runnable {
    private CleanableJobRepository cleanableJobRepository;
    public ClearRepoJob(JobRepository repository) {
        this.cleanableJobRepository = (CleanableJobRepository) repository;
    }

    @Override
    public void run() {
        System.out.println("Clearing repo");
        this.cleanableJobRepository.clean();
    }
}
