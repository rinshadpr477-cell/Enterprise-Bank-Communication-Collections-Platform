package com.ford.bank.audit.repository;

import com.ford.bank.audit.domain.AuditRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditRecordRepository extends JpaRepository<AuditRecord, UUID> {

    Page<AuditRecord> findByActorId(String actorId, Pageable pageable);

    Page<AuditRecord> findByEntityTypeAndEntityId(String entityType, String entityId, Pageable pageable);

    Page<AuditRecord> findByAction(String action, Pageable pageable);
}
