package com.lostres.ms_documental_dms.service;

import com.lostres.ms_documental_dms.dto.PresignedUrlResponse;
import com.lostres.ms_documental_dms.dto.UploadRequest;
import com.lostres.ms_documental_dms.exception.DocumentNotFoundException;
import com.lostres.ms_documental_dms.exception.InvalidDocumentStateException;
import com.lostres.ms_documental_dms.model.AuditAction;
import com.lostres.ms_documental_dms.model.Document;
import com.lostres.ms_documental_dms.model.DocumentStatus;
import com.lostres.ms_documental_dms.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private S3Service s3Service;

    @Mock
    private AuditService auditService;

    @Mock
    private UploadValidator uploadValidator;

    @InjectMocks
    private DocumentService documentService;

    private UUID docId;

    @BeforeEach
    void setUp() {
        docId = UUID.randomUUID();
    }

    @Test
    void requestUploadUrl_returnsPresignedUrlWithDocumentId() {
        Document savedDoc = Document.builder()
                .id(docId)
                .fileName("test.pdf")
                .s3Key("key_test.pdf")
                .status(DocumentStatus.PENDING)
                .build();

        when(documentRepository.save(any(Document.class))).thenReturn(savedDoc);
        when(s3Service.generatePresignedPutUrl(anyString(), anyString(), anyString()))
                .thenReturn("http://s3.local/put-url");

        PresignedUrlResponse result = documentService.requestUploadUrl(
                "test.pdf", "application/pdf", "M-001", null, "user123", "127.0.0.1"
        );

        assertEquals("http://s3.local/put-url", result.url());
        assertEquals(docId, result.documentId());
        verify(uploadValidator).validateContentType("application/pdf");
        verify(auditService).logActionAsync(docId, AuditAction.UPLOAD_REQUEST, "user123", "127.0.0.1");
    }

    @Test
    void confirmUpload_validatesS3AndMarksAvailable() {
        Document pending = Document.builder()
                .id(docId)
                .s3Key("key_test.pdf")
                .status(DocumentStatus.PENDING)
                .build();

        UploadRequest request = new UploadRequest("test.pdf", "key_test.pdf", "M-001", "application/pdf", 1024L);

        when(documentRepository.findByS3KeyAndDeletedAtIsNull("key_test.pdf")).thenReturn(Optional.of(pending));
        when(s3Service.getObjectMetadata("key_test.pdf")).thenReturn(new S3ObjectMetadata(1024L, "application/pdf"));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = documentService.confirmUpload(request, "user123", "127.0.0.1");

        assertEquals(DocumentStatus.AVAILABLE, result.status());
        assertEquals(1024L, result.sizeBytes());
        verify(auditService).logActionAsync(docId, AuditAction.UPLOAD_CONFIRMED, "user123", "127.0.0.1");
    }

    @Test
    void confirmUpload_throwsWhenNotPending() {
        Document available = Document.builder()
                .id(docId)
                .s3Key("key_test.pdf")
                .status(DocumentStatus.AVAILABLE)
                .build();

        UploadRequest request = new UploadRequest("test.pdf", "key_test.pdf", null, "application/pdf", 1024L);
        when(documentRepository.findByS3KeyAndDeletedAtIsNull("key_test.pdf")).thenReturn(Optional.of(available));

        assertThrows(InvalidDocumentStateException.class,
                () -> documentService.confirmUpload(request, "user123", "127.0.0.1"));
    }

    @Test
    void requestDownloadUrl_throwsWhenNotFound() {
        when(documentRepository.findById(docId)).thenReturn(Optional.empty());

        assertThrows(DocumentNotFoundException.class,
                () -> documentService.requestDownloadUrl(docId, "user123", "127.0.0.1"));
    }

    @Test
    void deleteDocument_archivesAndAudits() {
        Document document = Document.builder()
                .id(docId)
                .status(DocumentStatus.AVAILABLE)
                .build();

        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        documentService.deleteDocument(docId, "user123", "127.0.0.1");

        assertEquals(DocumentStatus.ARCHIVED, document.getStatus());
        assertNotNull(document.getDeletedAt());
        verify(auditService).logActionAsync(docId, AuditAction.DELETE, "user123", "127.0.0.1");
    }

    @Test
    void getDocumentById_auditsView() {
        Document document = Document.builder()
                .id(docId)
                .fileName("test.pdf")
                .status(DocumentStatus.AVAILABLE)
                .build();

        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));

        var result = documentService.getDocumentById(docId, "user123", "127.0.0.1");

        assertEquals("test.pdf", result.fileName());
        verify(auditService).logActionAsync(docId, AuditAction.VIEW, "user123", "127.0.0.1");
    }

    @Test
    void updateDocument_auditsEdit() {
        Document document = Document.builder()
                .id(docId)
                .fileName("old.pdf")
                .meterId("M-001")
                .status(DocumentStatus.AVAILABLE)
                .build();

        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = new com.lostres.ms_documental_dms.dto.UpdateDocumentRequest("new.pdf", "M-002", "Juan Pérez", null);
        var result = documentService.updateDocument(docId, request, "user123", "127.0.0.1");

        assertEquals("new.pdf", result.fileName());
        assertEquals("M-002", result.meterId());
        assertEquals("Juan Pérez", result.tecnicoAsignado());
        verify(auditService).logActionAsync(docId, AuditAction.EDIT, "user123", "127.0.0.1");
    }

    @Test
    void updateDocument_changesStatusToArchived() {
        Document document = Document.builder()
                .id(docId)
                .fileName("test.pdf")
                .status(DocumentStatus.AVAILABLE)
                .build();

        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = new com.lostres.ms_documental_dms.dto.UpdateDocumentRequest(null, null, null, DocumentStatus.ARCHIVED);
        var result = documentService.updateDocument(docId, request, "user123", "127.0.0.1");

        assertEquals(DocumentStatus.ARCHIVED, result.status());
    }

    @Test
    void updateDocument_rejectsManualExpiredStatus() {
        Document document = Document.builder()
                .id(docId)
                .status(DocumentStatus.AVAILABLE)
                .build();

        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));

        var request = new com.lostres.ms_documental_dms.dto.UpdateDocumentRequest(null, null, null, DocumentStatus.EXPIRED);

        assertThrows(InvalidDocumentStateException.class,
                () -> documentService.updateDocument(docId, request, "user123", "127.0.0.1"));
    }

    @Test
    void getDocumentById_allowsArchivedStatus() {
        Document document = Document.builder()
                .id(docId)
                .fileName("archived.pdf")
                .status(DocumentStatus.ARCHIVED)
                .build();

        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));

        var result = documentService.getDocumentById(docId, "user123", "127.0.0.1");

        assertEquals(DocumentStatus.ARCHIVED, result.status());
        verify(auditService).logActionAsync(docId, AuditAction.VIEW, "user123", "127.0.0.1");
    }
}
