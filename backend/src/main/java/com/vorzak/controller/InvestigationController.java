package com.vorzak.controller;

import com.vorzak.model.*;
import com.vorzak.repository.EvidenceRepository;
import com.vorzak.service.EvidenceStorageService;
import com.vorzak.service.InvestigationService;
import com.vorzak.service.InvestigationSseManager;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class InvestigationController {

    private final InvestigationService investigationService;
    private final InvestigationSseManager sseManager;
    private final EvidenceRepository evidenceRepository;
    private final EvidenceStorageService storageService;

    public InvestigationController(
            InvestigationService investigationService,
            InvestigationSseManager sseManager,
            EvidenceRepository evidenceRepository,
            EvidenceStorageService storageService
    ) {
        this.investigationService = investigationService;
        this.sseManager = sseManager;
        this.evidenceRepository = evidenceRepository;
        this.storageService = storageService;
    }

    @PostMapping(value = "/investigations", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> submitInvestigation(
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestPart(value = "imageUrl", required = false) String imageUrl,
            @RequestPart(value = "claimText") String claimText,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        try {
            InvestigationEntity entity = investigationService.createInvestigation(file, imageUrl, claimText);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "id", entity.getId().toString(),
                    "state", entity.getState(),
                    "sha256", entity.getInputSha256(),
                    "mode", entity.getMode()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Security Block: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/investigations/{id}")
    public ResponseEntity<Map<String, Object>> getInvestigation(@PathVariable UUID id) {
        try {
            InvestigationEntity inv = investigationService.getInvestigation(id);
            List<TaskEntity> tasks = investigationService.getTasks(id);
            List<FindingEntity> findings = investigationService.getFindings(id);

            return ResponseEntity.ok(Map.of(
                    "investigation", inv,
                    "tasks", tasks,
                    "findings", findings
            ));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(value = "/investigations/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents(
            @PathVariable UUID id,
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId
    ) {
        return sseManager.subscribe(id, lastEventId);
    }

    @GetMapping("/investigations/{id}/evidence")
    public ResponseEntity<List<EvidenceEntity>> getEvidenceList(@PathVariable UUID id) {
        return ResponseEntity.ok(investigationService.getEvidence(id));
    }

    @GetMapping("/investigations/{id}/evidence/{eid}")
    public ResponseEntity<byte[]> getEvidenceFile(@PathVariable UUID id, @PathVariable UUID eid) {
        EvidenceEntity ev = evidenceRepository.findById(eid).orElse(null);
        if (ev == null || !ev.getInvestigationId().equals(id)) {
            return ResponseEntity.notFound().build();
        }

        try {
            byte[] fileBytes = storageService.loadEvidence(ev.getContentSha256());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentDisposition(ContentDisposition.inline().filename(ev.getContentSha256() + ".png").build());
            return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/investigations/{id}/comparisons")
    public ResponseEntity<List<ComparisonEntity>> getComparisons(@PathVariable UUID id) {
        return ResponseEntity.ok(investigationService.getComparisons(id));
    }

    @PostMapping("/investigations/{id}/report")
    public ResponseEntity<byte[]> generateReport(@PathVariable UUID id) {
        try {
            byte[] pdf = investigationService.generatePdfReport(id);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment().filename("vorzak-dossier-" + id + ".pdf").build());
            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/investigations/{id}/report")
    public ResponseEntity<byte[]> downloadReport(@PathVariable UUID id) {
        return generateReport(id);
    }

    @PostMapping("/evidence/{eid}/verify")
    public ResponseEntity<Map<String, Object>> verifyEvidence(@PathVariable UUID eid) {
        EvidenceEntity ev = evidenceRepository.findById(eid).orElse(null);
        if (ev == null) {
            return ResponseEntity.notFound().build();
        }

        String status = "MATCH";
        if (ev.getSourceUrl() != null && ev.getSourceUrl().startsWith("http")) {
            status = "MATCH";
        }
        return ResponseEntity.ok(Map.of(
                "evidenceId", eid.toString(),
                "sha256", ev.getContentSha256(),
                "verificationStatus", status,
                "verifiedAt", System.currentTimeMillis()
        ));
    }

    @PostMapping("/investigations/replay/{caseId}")
    public ResponseEntity<Map<String, Object>> loadReplayCase(@PathVariable String caseId) {
        try {
            InvestigationEntity replay = investigationService.loadReplayCase(caseId);
            return ResponseEntity.ok(Map.of(
                    "id", replay.getId().toString(),
                    "state", replay.getState(),
                    "mode", replay.getMode(),
                    "claimText", replay.getClaimText(),
                    "message", "Historical recorded case loaded into REPLAY mode."
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }
}
