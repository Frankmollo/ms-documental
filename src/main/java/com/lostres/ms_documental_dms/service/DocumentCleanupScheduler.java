package com.lostres.ms_documental_dms.service;

import com.lostres.ms_documental_dms.config.DmsProperties;
import com.lostres.ms_documental_dms.model.Document;
import com.lostres.ms_documental_dms.model.DocumentStatus;
import com.lostres.ms_documental_dms.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentCleanupScheduler {

    private final DocumentRepository documentRepository;
    private final DmsProperties dmsProperties;

    @Scheduled(cron = "0 0 * * * *")
    public void expirePendingDocuments() {
        if (!dmsProperties.getCleanup().isEnabled()) {
            return;
        }

        LocalDateTime cutoff = LocalDateTime.now()
                .minusHours(dmsProperties.getCleanup().getPendingExpirationHours());

        List<Document> expired = documentRepository.findByDeletedAtIsNullAndStatusAndUploadedAtBefore(
                DocumentStatus.PENDING, cutoff);
        if (expired.isEmpty()) {
            return;
        }

        for (Document document : expired) {
            document.setStatus(DocumentStatus.EXPIRED);
        }
        documentRepository.saveAll(expired);
        log.info("Marcados {} documentos PENDING como EXPIRED", expired.size());
    }
}
