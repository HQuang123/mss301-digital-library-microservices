package com.talenthub.job.infrastructure.persistence;

import com.talenthub.job.domain.entity.Job;
import com.talenthub.job.domain.port.persistence.JobPersistence;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JobPersistenceAdapter implements JobPersistence {

    private final JobJpaRepository jpaRepository;

    public JobPersistenceAdapter(JobJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Job save(Job job) {
        return jpaRepository.save(job);
    }

    @Override
    public boolean existsByJobTitle(String jobTitle) {
        return jpaRepository.existsByJobTitle(jobTitle);
    }

    @Override
    public Optional<Job> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Job> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
