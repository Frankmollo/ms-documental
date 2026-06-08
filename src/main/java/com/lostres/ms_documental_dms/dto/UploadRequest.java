package com.lostres.ms_documental_dms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UploadRequest(
    @NotBlank String fileName,
    @NotBlank String s3Key,
    String meterId,
    @NotBlank String contentType,
    @NotNull @Positive Long sizeBytes
) {}
