package com.lostres.ms_documental_dms.dto;

import com.lostres.ms_documental_dms.model.DocumentStatus;

public record UpdateDocumentRequest(
    String fileName,
    String meterId,
    String tecnicoAsignado,
    DocumentStatus status
) {
    public boolean hasUpdates() {
        return (fileName != null && !fileName.isBlank())
                || (meterId != null && !meterId.isBlank())
                || (tecnicoAsignado != null && !tecnicoAsignado.isBlank())
                || status != null;
    }
}
