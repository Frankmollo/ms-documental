package com.lostres.ms_documental_dms.service;

import com.lostres.ms_documental_dms.dto.DocumentResponse;
import com.lostres.ms_documental_dms.dto.PresignedUrlResponse;
import com.lostres.ms_documental_dms.dto.UpdateDocumentRequest;
import com.lostres.ms_documental_dms.dto.UploadRequest;
import com.lostres.ms_documental_dms.exception.DocumentNotFoundException;
import com.lostres.ms_documental_dms.exception.InvalidDocumentStateException;
import com.lostres.ms_documental_dms.model.AuditAction;
import com.lostres.ms_documental_dms.model.Document;
import com.lostres.ms_documental_dms.model.DocumentStatus;
import com.lostres.ms_documental_dms.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.lostres.ms_documental_dms.util.AppClock;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final S3Service s3Service;
    private final AuditService auditService;
    private final UploadValidator uploadValidator;
    private final BlockchainNotaryService blockchainNotaryService;

    public PresignedUrlResponse requestUploadUrl(
            String fileName, String contentType, String meterId, String tecnicoAsignado,
            String userId, String ipAddress) {
        uploadValidator.validateUserId(userId);
        uploadValidator.validateMeterId(meterId);
        uploadValidator.validateContentType(contentType);

        String s3Key = UUID.randomUUID() + "_" + fileName;

        Document document = Document.builder()
                .fileName(fileName)
                .s3Key(s3Key)
                .meterId(meterId)
                .tecnicoAsignado(tecnicoAsignado)
                .contentType(contentType)
                .status(DocumentStatus.PENDING)
                .uploadedBy(userId)
                .build();

        Document savedDoc = documentRepository.save(document);
        auditService.logActionAsync(savedDoc.getId(), AuditAction.UPLOAD_REQUEST, userId, ipAddress);

        String url = s3Service.generatePresignedPutUrl(fileName, contentType, s3Key);
        return new PresignedUrlResponse(url, s3Key, savedDoc.getId());
    }

    public DocumentResponse confirmUpload(UploadRequest request, String userId, String ipAddress) {
        uploadValidator.validateUserId(userId);
        uploadValidator.validateContentType(request.contentType());
        uploadValidator.validateSize(request.sizeBytes());

        Document document = documentRepository.findByS3KeyAndDeletedAtIsNull(request.s3Key())
                .orElseThrow(() -> new DocumentNotFoundException("Documento pendiente no encontrado para s3Key: " + request.s3Key()));

        if (document.getStatus() != DocumentStatus.PENDING) {
            throw new InvalidDocumentStateException("El documento no está en estado PENDING");
        }

        S3ObjectMetadata metadata = s3Service.getObjectMetadata(request.s3Key());
        uploadValidator.validateSize(metadata.contentLength());

        document.setSizeBytes(metadata.contentLength());
        document.setStatus(DocumentStatus.AVAILABLE);

        Document updatedDoc = documentRepository.save(document);
        auditService.logActionAsync(updatedDoc.getId(), AuditAction.UPLOAD_CONFIRMED, userId, ipAddress);

        // Notarizar evidencia en la Blockchain para inmutabilidad
        // En un entorno real se usaría SHA-256 calculando el archivo o el ETag de S3
        String fileHash = "SHA256-" + request.s3Key().hashCode() + metadata.contentLength(); 
        blockchainNotaryService.notarizeDocument(updatedDoc.getId(), fileHash);
        
        // Para que el frontend pueda visualizarlo, guardamos un TxHash de prueba
        updatedDoc.setBlockchainTxHash("0xabc" + fileHash.hashCode() + "def1234");
        documentRepository.save(updatedDoc);

        return mapToResponse(updatedDoc);
    }

    public PresignedUrlResponse requestDownloadUrl(UUID documentId, String userId, String ipAddress) {
        uploadValidator.validateUserId(userId);

        Document document = requireVisibleDocument(documentId);

        if (document.getStatus() != DocumentStatus.AVAILABLE) {
            throw new InvalidDocumentStateException("El documento no está disponible para descarga");
        }

        auditService.logActionAsync(document.getId(), AuditAction.DOWNLOAD, userId, ipAddress);

        String url = s3Service.generatePresignedGetUrl(document.getS3Key());
        return new PresignedUrlResponse(url, document.getS3Key(), document.getId());
    }

    public DocumentResponse getDocumentById(UUID documentId, String userId, String ipAddress) {
        uploadValidator.validateUserId(userId);

        Document document = requireVisibleDocument(documentId);

        auditService.logActionAsync(document.getId(), AuditAction.VIEW, userId, ipAddress);
        return mapToResponse(document);
    }

    public DocumentResponse updateDocument(UUID documentId, UpdateDocumentRequest request, String userId, String ipAddress) {
        uploadValidator.validateUserId(userId);

        if (!request.hasUpdates()) {
            throw new IllegalArgumentException(
                    "Debe proporcionar al menos un campo para actualizar (fileName, meterId, tecnicoAsignado o status)");
        }

        Document document = requireVisibleDocument(documentId);

        boolean hasMetadataUpdate = (request.fileName() != null && !request.fileName().isBlank())
                || (request.meterId() != null && !request.meterId().isBlank())
                || (request.tecnicoAsignado() != null && !request.tecnicoAsignado().isBlank());

        if (hasMetadataUpdate && document.getStatus() != DocumentStatus.AVAILABLE) {
            throw new InvalidDocumentStateException("Solo se pueden editar metadatos en documentos Disponibles");
        }

        if (request.fileName() != null && !request.fileName().isBlank()) {
            document.setFileName(request.fileName().trim());
        }
        if (request.meterId() != null && !request.meterId().isBlank()) {
            document.setMeterId(request.meterId().trim());
        }
        if (request.tecnicoAsignado() != null && !request.tecnicoAsignado().isBlank()) {
            document.setTecnicoAsignado(request.tecnicoAsignado().trim());
        }
        if (request.status() != null) {
            applyManualStatusChange(document, request.status());
        }

        Document updated = documentRepository.save(document);
        auditService.logActionAsync(updated.getId(), AuditAction.EDIT, userId, ipAddress);
        return mapToResponse(updated);
    }

    private void applyManualStatusChange(Document document, DocumentStatus newStatus) {
        DocumentStatus current = document.getStatus();
        if (newStatus == current) {
            return;
        }
        if (newStatus == DocumentStatus.PENDING || newStatus == DocumentStatus.EXPIRED) {
            throw new InvalidDocumentStateException(
                    "No se puede asignar manualmente el estado " + newStatus);
        }
        if (current == DocumentStatus.PENDING) {
            throw new InvalidDocumentStateException(
                    "Un documento pendiente debe confirmarse con la subida antes de cambiar su estado");
        }
        if (newStatus == DocumentStatus.ARCHIVED) {
            document.setStatus(DocumentStatus.ARCHIVED);
            return;
        }
        if (newStatus == DocumentStatus.AVAILABLE) {
            document.setStatus(DocumentStatus.AVAILABLE);
            document.setDeletedAt(null);
        }
    }

    public Page<DocumentResponse> listDocuments(
            String meterId, String tecnicoAsignado, DocumentStatus status, Pageable pageable) {
        DocumentStatus effectiveStatus = status != null ? status : DocumentStatus.AVAILABLE;

        boolean hasMeter = meterId != null && !meterId.isBlank();
        boolean hasTecnico = tecnicoAsignado != null && !tecnicoAsignado.isBlank();

        Page<Document> documents;
        if (hasMeter && hasTecnico) {
            documents = documentRepository.findByDeletedAtIsNullAndMeterIdContainingIgnoreCaseAndTecnicoAsignadoContainingIgnoreCaseAndStatus(
                    meterId.trim(), tecnicoAsignado.trim(), effectiveStatus, pageable);
        } else if (hasMeter) {
            documents = documentRepository.findByDeletedAtIsNullAndMeterIdContainingIgnoreCaseAndStatus(
                    meterId.trim(), effectiveStatus, pageable);
        } else if (hasTecnico) {
            documents = documentRepository.findByDeletedAtIsNullAndTecnicoAsignadoContainingIgnoreCaseAndStatus(
                    tecnicoAsignado.trim(), effectiveStatus, pageable);
        } else {
            documents = documentRepository.findByDeletedAtIsNullAndStatus(effectiveStatus, pageable);
        }
        return documents.map(this::mapToResponse);
    }

    private Document requireVisibleDocument(UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Documento no encontrado con ID: " + documentId));
        if (document.getDeletedAt() != null) {
            throw new DocumentNotFoundException("Documento no encontrado con ID: " + documentId);
        }
        return document;
    }

    public void deleteDocument(UUID documentId, String userId, String ipAddress) {
        uploadValidator.validateUserId(userId);

        Document document = requireVisibleDocument(documentId);

        if (document.getStatus() == DocumentStatus.ARCHIVED) {
            throw new InvalidDocumentStateException("El documento ya fue eliminado");
        }

        document.setStatus(DocumentStatus.ARCHIVED);
        document.setDeletedAt(AppClock.now());
        documentRepository.save(document);

        auditService.logActionAsync(document.getId(), AuditAction.DELETE, userId, ipAddress);
    }

    private DocumentResponse mapToResponse(Document doc) {
        return new DocumentResponse(
                doc.getId(),
                doc.getFileName(),
                doc.getMeterId(),
                doc.getTecnicoAsignado(),
                doc.getStatus(),
                doc.getContentType(),
                doc.getSizeBytes(),
                doc.getUploadedBy(),
                doc.getUploadedAt(),
                doc.getBlockchainTxHash()
        );
    }
}
