package com.talenthub.job.domain.port.persistence;

import com.talenthub.job.domain.entity.Job;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobPersistence {
    Job save(Job job);

    boolean existsByJobTitle(String jobTitle);

    Optional<Job> findById(UUID id);

    List<Job> findAll();

    void deleteById(UUID id);
}
