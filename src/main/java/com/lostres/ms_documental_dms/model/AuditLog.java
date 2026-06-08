package com.lostres.ms_documental_dms.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    private UUID id;
    private UUID documentId;
    private AuditAction action;
    private String performedBy;
    private LocalDateTime timestamp;
    private String ipAddress;
}
