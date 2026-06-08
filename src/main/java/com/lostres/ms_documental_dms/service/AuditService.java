package com.lostres.ms_documental_dms.service;

import com.lostres.ms_documental_dms.model.AuditAction;
import com.lostres.ms_documental_dms.model.AuditLog;
import com.lostres.ms_documental_dms.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    public void logActionAsync(UUID documentId, AuditAction action, String userId, String ipAddress) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .documentId(documentId)
                    .action(action)
                    .performedBy(userId)
                    .ipAddress(ipAddress)
                    .build();
            auditLogRepository.save(auditLog);
            log.info("Audit log saved async: DocID={} Action={}", documentId, action);
        } catch (Exception e) {
            log.error("Failed to save audit log async for DocID={}", documentId, e);
        }
    }
}
