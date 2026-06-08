package com.lostres.ms_documental_dms.controller;

import com.lostres.ms_documental_dms.config.PageableSupport;
import com.lostres.ms_documental_dms.model.AuditLog;
import com.lostres.ms_documental_dms.repository.AuditLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Tag(name = "Auditoría", description = "API para consultar registros de auditoría de los documentos")
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    @Operation(summary = "Consultar Logs por Documento (CU-17)", description = "Devuelve de forma paginada los registros de auditoría de un documento.")
    @GetMapping("/documents/{documentId}")
    public ResponseEntity<Page<AuditLog>> getLogsForDocument(
            @PathVariable UUID documentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "timestamp,desc") String sort) {
        Page<AuditLog> logs = auditLogRepository.findByDocumentId(documentId, PageableSupport.of(page, size, sort));
        return ResponseEntity.ok(logs);
    }
}
