package com.ford.bank.communication.scheduler;

import com.ford.bank.communication.domain.CommunicationJob;
import com.ford.bank.communication.repository.CommunicationJobRepository;
import com.ford.bank.communication.service.CommunicationJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Background scheduler that periodically claims pending/retrying jobs
 * using SKIP LOCKED and dispatches them.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class CommunicationJobScheduler {

    private final CommunicationJobRepository jobRepository;
    private final CommunicationJobService jobService;

    @Value("${app.scheduling.job-batch-size:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.scheduling.job-poll-interval-ms:10000}")
    @Transactional
    public void processDueJobs() {
        try {
            List<CommunicationJob> dueJobs = jobRepository.claimDueJobs(OffsetDateTime.now(), batchSize);
            if (dueJobs.isEmpty()) {
                return;
            }

            log.info("Claimed {} due communication jobs for execution", dueJobs.size());
            for (CommunicationJob job : dueJobs) {
                try {
                    jobService.dispatchJob(job.getId());
                } catch (Exception e) {
                    log.error("Failed executing job {}: {}", job.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error in communication job scheduler loop: {}", e.getMessage(), e);
        }
    }
}
