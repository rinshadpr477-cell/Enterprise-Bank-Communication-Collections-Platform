package com.ford.bank.collections.service;

import com.ford.bank.collections.domain.AgentTask;
import com.ford.bank.collections.domain.AgentTaskStatus;
import com.ford.bank.collections.domain.Priority;
import com.ford.bank.collections.dto.AgentTaskResponse;
import com.ford.bank.collections.dto.CreateAgentTaskRequest;
import com.ford.bank.collections.dto.UpdateAgentTaskRequest;
import com.ford.bank.collections.repository.AgentTaskRepository;
import com.ford.bank.collections.repository.CollectionCaseRepository;
import com.ford.bank.common.dto.PageResponse;
import com.ford.bank.common.exception.ResourceNotFoundException;
import com.ford.bank.kafka.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentTaskService {

    private final AgentTaskRepository agentTaskRepository;
    private final CollectionCaseRepository collectionCaseRepository;
    private final OutboxService outboxService;

    @Transactional
    public AgentTaskResponse createTask(CreateAgentTaskRequest request) {
        if (!collectionCaseRepository.existsById(request.collectionCaseId())) {
            throw new ResourceNotFoundException("CollectionCase not found: " + request.collectionCaseId());
        }

        AgentTask task = new AgentTask();
        task.setCollectionCaseId(request.collectionCaseId());
        task.setTaskType(request.taskType());
        task.setStatus(request.assignedAgentId() != null ? AgentTaskStatus.ASSIGNED : AgentTaskStatus.OPEN);
        task.setAssignedAgentId(request.assignedAgentId());
        task.setPriority(request.priority() != null ? request.priority() : Priority.MEDIUM);
        task.setDueAt(request.dueAt());
        task.setNotes(request.notes());

        AgentTask saved = agentTaskRepository.save(task);

        Map<String, Object> payload = new HashMap<>();
        payload.put("taskId", saved.getId().toString());
        payload.put("collectionCaseId", saved.getCollectionCaseId().toString());
        payload.put("taskType", saved.getTaskType().name());
        payload.put("status", saved.getStatus().name());

        outboxService.publishEvent("AgentTask", saved.getId().toString(), "AGENT_TASK_CREATED", payload);
        log.info("Created agent task id={} caseId={}", saved.getId(), saved.getCollectionCaseId());
        return AgentTaskResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public AgentTaskResponse getTask(UUID id) {
        AgentTask task = agentTaskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AgentTask not found: " + id));
        return AgentTaskResponse.fromEntity(task);
    }

    @Transactional(readOnly = true)
    public List<AgentTaskResponse> getTasksByCase(UUID collectionCaseId) {
        return agentTaskRepository.findByCollectionCaseId(collectionCaseId).stream()
                .map(AgentTaskResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<AgentTaskResponse> listTasks(AgentTaskStatus status, String agentId, Pageable pageable) {
        Page<AgentTask> page;
        if (agentId != null && status != null) {
            page = agentTaskRepository.findByAssignedAgentIdAndStatusIn(agentId, List.of(status), pageable);
        } else if (agentId != null) {
            page = agentTaskRepository.findByAssignedAgentId(agentId, pageable);
        } else if (status != null) {
            page = agentTaskRepository.findByStatus(status, pageable);
        } else {
            page = agentTaskRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(AgentTaskResponse::fromEntity));
    }

    @Transactional
    public AgentTaskResponse updateTask(UUID id, UpdateAgentTaskRequest request) {
        AgentTask task = agentTaskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AgentTask not found: " + id));

        boolean statusChanged = false;
        if (request.status() != null && request.status() != task.getStatus()) {
            task.setStatus(request.status());
            statusChanged = true;
            if (request.status() == AgentTaskStatus.COMPLETED || request.status() == AgentTaskStatus.CANCELLED) {
                task.setCompletedAt(OffsetDateTime.now());
            }
        }
        if (request.assignedAgentId() != null) {
            task.setAssignedAgentId(request.assignedAgentId());
            if (task.getStatus() == AgentTaskStatus.OPEN) {
                task.setStatus(AgentTaskStatus.ASSIGNED);
            }
        }
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        if (request.dueAt() != null) {
            task.setDueAt(request.dueAt());
        }
        if (request.notes() != null) {
            task.setNotes(request.notes());
        }

        AgentTask updated = agentTaskRepository.save(task);

        if (statusChanged) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("taskId", updated.getId().toString());
            payload.put("newStatus", updated.getStatus().name());
            outboxService.publishEvent("AgentTask", updated.getId().toString(), "AGENT_TASK_UPDATED", payload);
        }

        return AgentTaskResponse.fromEntity(updated);
    }
}
