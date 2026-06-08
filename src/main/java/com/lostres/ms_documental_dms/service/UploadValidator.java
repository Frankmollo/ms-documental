package com.lostres.ms_documental_dms.service;

import com.lostres.ms_documental_dms.config.DmsProperties;
import com.lostres.ms_documental_dms.exception.FileSizeExceededException;
import com.lostres.ms_documental_dms.exception.InvalidContentTypeException;
import com.lostres.ms_documental_dms.exception.InvalidUserException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UploadValidator {

    private final DmsProperties dmsProperties;

    public void validateUserId(String userId) {
        if (!dmsProperties.getSecurity().isRequireUserId()) {
            return;
        }
        if (userId == null || userId.isBlank() || "anonymous".equalsIgnoreCase(userId.trim())) {
            throw new InvalidUserException();
        }
    }

    public void validateContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new InvalidContentTypeException("(vacío)");
        }
        if (!dmsProperties.getAllowedContentTypesList().contains(contentType.trim())) {
            throw new InvalidContentTypeException(contentType);
        }
    }

    public void validateSize(long sizeBytes) {
        if (sizeBytes > dmsProperties.getUpload().getMaxSizeBytes()) {
            throw new FileSizeExceededException(sizeBytes, dmsProperties.getUpload().getMaxSizeBytes());
        }
    }

    public void validateMeterId(String meterId) {
        if (!dmsProperties.getUpload().isRequireMeterId()) {
            return;
        }
        if (meterId == null || meterId.isBlank()) {
            throw new IllegalArgumentException("El campo meterId es obligatorio para operaciones móviles (CU-18)");
        }
    }
}
