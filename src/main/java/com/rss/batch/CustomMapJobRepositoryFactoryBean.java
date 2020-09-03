package com.rss.batch;

import org.springframework.batch.core.repository.dao.*;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.core.repository.support.SimpleJobRepository;

public class CustomMapJobRepositoryFactoryBean extends MapJobRepositoryFactoryBean {

    @Override
    public MySimpleJobRepository getObject() throws Exception {
        return new MySimpleJobRepository(createJobInstanceDao(), createJobExecutionDao(), createStepExecutionDao(), createExecutionContextDao());
    }

    public class MySimpleJobRepository extends SimpleJobRepository implements CleanableJobRepository {

        MySimpleJobRepository(final JobInstanceDao jobInstanceDao,
                              final JobExecutionDao jobExecutionDao,
                              final StepExecutionDao stepExecutionDao,
                              final ExecutionContextDao ecDao) {
            super(jobInstanceDao, jobExecutionDao, stepExecutionDao, ecDao);
        }

        @Override
        public void clean() {
            ((MapJobInstanceDao) getJobInstanceDao()).clear();
            ((MapJobExecutionDao) getJobExecutionDao()).clear();
            ((MapStepExecutionDao) getStepExecutionDao()).clear();
            ((MapExecutionContextDao) getExecutionContextDao()).clear();
        }

    }
}
