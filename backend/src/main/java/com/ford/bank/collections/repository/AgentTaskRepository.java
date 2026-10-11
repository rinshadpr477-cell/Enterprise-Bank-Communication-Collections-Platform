package com.ford.bank.collections.repository;

import com.ford.bank.collections.domain.AgentTask;
import com.ford.bank.collections.domain.AgentTaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AgentTaskRepository extends JpaRepository<AgentTask, UUID> {

    List<AgentTask> findByCollectionCaseId(UUID collectionCaseId);

    Page<AgentTask> findByStatus(AgentTaskStatus status, Pageable pageable);

    Page<AgentTask> findByAssignedAgentId(String assignedAgentId, Pageable pageable);

    Page<AgentTask> findByAssignedAgentIdAndStatusIn(String assignedAgentId, Collection<AgentTaskStatus> statuses, Pageable pageable);
}
