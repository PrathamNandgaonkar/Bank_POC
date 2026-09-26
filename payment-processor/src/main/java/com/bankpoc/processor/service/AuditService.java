package com.bankpoc.processor.service;

import com.bankpoc.processor.model.entity.AuditLog;
import com.bankpoc.processor.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void logStateChange(String entityType, String entityId, String action, String oldStatus, String newStatus, Map<String, Object> details) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .action(action)
                    .oldStatus(oldStatus)
                    .newStatus(newStatus)
                    .details(details != null ? objectMapper.writeValueAsString(details) : null)
                    .traceId(MDC.get("traceId"))
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to save audit log for {}", entityId, e);
        }
    }

    @Transactional
    public void logError(String entityType, String entityId, String action, Map<String, Object> errorDetails) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .action(action)
                    .details(errorDetails != null ? objectMapper.writeValueAsString(errorDetails) : null)
                    .traceId(MDC.get("traceId"))
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to save audit error log for {}", entityId, e);
        }
    }

    public List<AuditLog> getAuditTrail(String entityId) {
        return auditLogRepository.findByEntityIdOrderByCreatedAtDesc(entityId);
    }
}
