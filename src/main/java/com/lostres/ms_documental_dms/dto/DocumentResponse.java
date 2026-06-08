package com.lostres.ms_documental_dms.dto;

import com.lostres.ms_documental_dms.model.DocumentStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentResponse(
    UUID id,
    String fileName,
    String meterId,
    String tecnicoAsignado,
    DocumentStatus status,
    String contentType,
    Long sizeBytes,
    String uploadedBy,
    LocalDateTime uploadedAt
) {}
