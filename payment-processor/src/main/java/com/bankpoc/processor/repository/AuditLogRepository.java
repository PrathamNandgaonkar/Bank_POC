package com.bankpoc.processor.repository;

import com.bankpoc.processor.model.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, String entityId);
    
    List<AuditLog> findByEntityIdOrderByCreatedAtDesc(String entityId);
}
