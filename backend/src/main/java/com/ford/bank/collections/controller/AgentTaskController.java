package com.ford.bank.collections.controller;

import com.ford.bank.collections.domain.AgentTaskStatus;
import com.ford.bank.collections.dto.AgentTaskResponse;
import com.ford.bank.collections.dto.CreateAgentTaskRequest;
import com.ford.bank.collections.dto.UpdateAgentTaskRequest;
import com.ford.bank.collections.service.AgentTaskService;
import com.ford.bank.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Tag(name = "Agent Tasks", description = "Human-agent task queue management")
@RestController
@RequestMapping("/api/v1/collections/tasks")
@RequiredArgsConstructor
public class AgentTaskController {

    private final AgentTaskService agentTaskService;

    @Operation(summary = "Create an agent task")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'COLLECTION_AGENT')")
    public ResponseEntity<AgentTaskResponse> createTask(@Valid @RequestBody CreateAgentTaskRequest request) {
        AgentTaskResponse response = agentTaskService.createTask(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Get task by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'COLLECTION_AGENT', 'AUDITOR')")
    public ResponseEntity<AgentTaskResponse> getTask(@PathVariable UUID id) {
        return ResponseEntity.ok(agentTaskService.getTask(id));
    }

    @Operation(summary = "Get tasks by collection case")
    @GetMapping("/case/{caseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'COLLECTION_AGENT', 'AUDITOR')")
    public ResponseEntity<List<AgentTaskResponse>> getTasksByCase(@PathVariable UUID caseId) {
        return ResponseEntity.ok(agentTaskService.getTasksByCase(caseId));
    }

    @Operation(summary = "List agent tasks with filtering and pagination")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'COLLECTION_AGENT', 'AUDITOR')")
    public ResponseEntity<PageResponse<AgentTaskResponse>> listTasks(
            @RequestParam(required = false) AgentTaskStatus status,
            @RequestParam(required = false) String agentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        size = Math.min(size, 100);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        return ResponseEntity.ok(agentTaskService.listTasks(status, agentId, pageable));
    }

    @Operation(summary = "Update task status, assignment, or notes")
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'COLLECTION_AGENT')")
    public ResponseEntity<AgentTaskResponse> updateTask(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAgentTaskRequest request) {
        return ResponseEntity.ok(agentTaskService.updateTask(id, request));
    }
}
