package com.lostres.ms_documental_dms.controller;

import com.lostres.ms_documental_dms.config.PageableSupport;
import com.lostres.ms_documental_dms.dto.DocumentResponse;
import com.lostres.ms_documental_dms.dto.PresignedUrlResponse;
import com.lostres.ms_documental_dms.dto.UpdateDocumentRequest;
import com.lostres.ms_documental_dms.dto.UploadRequest;
import com.lostres.ms_documental_dms.model.DocumentStatus;
import com.lostres.ms_documental_dms.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Documentos", description = "API REST para gestión documental usando URLs Pre-firmadas")
public class DocumentController {

    private final DocumentService documentService;

    @Operation(summary = "Paso 1: Obtener URL de Subida", description = "Registra un documento como PENDING y retorna la URL para subirlo directo a S3.")
    @GetMapping("/upload-url")
    public ResponseEntity<PresignedUrlResponse> getUploadUrl(
            @RequestParam String fileName,
            @RequestParam String contentType,
            @RequestParam(required = false) String meterId,
            @RequestParam(required = false) String tecnicoAsignado,
            @RequestHeader(value = "X-User-Id") String userId,
            @RequestHeader(value = "X-Forwarded-For", defaultValue = "127.0.0.1") String ipAddress) {
        
        PresignedUrlResponse response = documentService.requestUploadUrl(
                fileName, contentType, meterId, tecnicoAsignado, userId, ipAddress);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Paso 2: Confirmar Subida", description = "Cambia el estado de PENDING a AVAILABLE y actualiza el tamaño tras subir el archivo.")
    @PostMapping("/confirm-upload")
    public ResponseEntity<DocumentResponse> confirmUpload(
            @Valid @RequestBody UploadRequest request,
            @RequestHeader(value = "X-User-Id") String userId,
            @RequestHeader(value = "X-Forwarded-For", defaultValue = "127.0.0.1") String ipAddress) {
        
        DocumentResponse response = documentService.confirmUpload(request, userId, ipAddress);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Consultar documento por ID (CU-15)", description = "Obtiene metadatos del documento y registra auditoría de acceso (VIEW).")
    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocument(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id") String userId,
            @RequestHeader(value = "X-Forwarded-For", defaultValue = "127.0.0.1") String ipAddress) {

        DocumentResponse response = documentService.getDocumentById(id, userId, ipAddress);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Editar metadatos del documento (CU-17)", description = "Actualiza fileName, meterId, tecnicoAsignado y/o status. Registra auditoría EDIT.")
    @PatchMapping("/{id}")
    public ResponseEntity<DocumentResponse> updateDocument(
            @PathVariable UUID id,
            @RequestBody UpdateDocumentRequest request,
            @RequestHeader(value = "X-User-Id") String userId,
            @RequestHeader(value = "X-Forwarded-For", defaultValue = "127.0.0.1") String ipAddress) {

        DocumentResponse response = documentService.updateDocument(id, request, userId, ipAddress);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Obtener URL de Descarga", description = "Retorna una URL Pre-firmada para descargar o visualizar el documento, auditando la acción.")
    @GetMapping("/{id}/download-url")
    public ResponseEntity<PresignedUrlResponse> getDownloadUrl(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id") String userId,
            @RequestHeader(value = "X-Forwarded-For", defaultValue = "127.0.0.1") String ipAddress) {
        
        PresignedUrlResponse response = documentService.requestDownloadUrl(id, userId, ipAddress);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Listar documentos paginados", description = "Devuelve documentos por estado (default: AVAILABLE), con filtros opcionales por medidor/cuenta y técnico.")
    @GetMapping
    public ResponseEntity<Page<DocumentResponse>> listDocuments(
            @Parameter(description = "Medidor o N° cuenta (opcional)") @RequestParam(required = false) String meterId,
            @Parameter(description = "Técnico asignado (opcional)") @RequestParam(required = false) String tecnicoAsignado,
            @Parameter(description = "Estado del documento (default: AVAILABLE)") @RequestParam(required = false) DocumentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "uploadedAt,desc") String sort) {

        Page<DocumentResponse> response = documentService.listDocuments(
                meterId, tecnicoAsignado, status, PageableSupport.of(page, size, sort));
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Eliminar documento (Soft Delete)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id") String userId,
            @RequestHeader(value = "X-Forwarded-For", defaultValue = "127.0.0.1") String ipAddress) {
        
        documentService.deleteDocument(id, userId, ipAddress);
        return ResponseEntity.noContent().build();
    }
}
