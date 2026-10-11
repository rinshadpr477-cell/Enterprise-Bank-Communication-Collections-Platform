package com.ford.bank.audit.service;

import com.ford.bank.audit.domain.AuditRecord;
import com.ford.bank.audit.dto.AuditRecordResponse;
import com.ford.bank.audit.repository.AuditRecordRepository;
import com.ford.bank.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditRecordRepository auditRecordRepository;

    @Transactional(propagation = Propagation.REQUIRED)
    public void record(String actorId, String action, String entityType, String entityId, Map<String, Object> metadata) {
        try {
            AuditRecord record = new AuditRecord(actorId, action, entityType, entityId, metadata);
            auditRecordRepository.save(record);
            log.debug("Audit record created: action={} actor={} entityType={} entityId={}",
                    action, actorId, entityType, entityId);
        } catch (Exception e) {
            log.error("Failed to write audit record for action={} entityId={}: {}",
                    action, entityId, e.getMessage());
            // In a bank platform, audit logging failures should be monitored, but depending on policy
            // we log the error to prevent blocking non-critical business transactions.
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditRecordResponse> listAuditRecords(String actorId, String entityType, String entityId, Pageable pageable) {
        Page<AuditRecord> page;
        if (actorId != null) {
            page = auditRecordRepository.findByActorId(actorId, pageable);
        } else if (entityType != null && entityId != null) {
            page = auditRecordRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable);
        } else {
            page = auditRecordRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(AuditRecordResponse::fromEntity));
    }
}
