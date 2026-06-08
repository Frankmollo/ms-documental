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
public class Document {

    private UUID id;
    private String fileName;
    private String s3Key;
    private String meterId;
    private String tecnicoAsignado;
    private DocumentStatus status;
    private String contentType;
    private Long sizeBytes;
    private String uploadedBy;
    private LocalDateTime uploadedAt;
    private LocalDateTime deletedAt;
}
