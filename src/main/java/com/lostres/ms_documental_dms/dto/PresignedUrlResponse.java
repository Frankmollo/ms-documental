package com.lostres.ms_documental_dms.dto;

import java.util.UUID;

public record PresignedUrlResponse(
    String url,
    String s3Key,
    UUID documentId
) {}
