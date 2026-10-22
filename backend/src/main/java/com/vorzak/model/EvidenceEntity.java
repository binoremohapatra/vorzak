package com.vorzak.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "evidence")
public class EvidenceEntity {

    @Id
    private UUID id;

    @Column(name = "investigation_id", nullable = false)
    private UUID investigationId;

    @Column(name = "kind", nullable = false, length = 32)
    private String kind; // SUBMITTED_IMAGE, SOURCE_PAGE, CANDIDATE_IMAGE, COMPARISON_RESULT, SEARCH_RESULT

    @Column(name = "source_url", columnDefinition = "TEXT")
    private String sourceUrl;

    @Column(name = "retrieved_at", nullable = false)
    private Instant retrievedAt;

    @Column(name = "content_sha256", nullable = false, length = 64)
    private String contentSha256;

    @Column(name = "file_path", columnDefinition = "TEXT")
    private String filePath;

    @Column(name = "extraction_method", length = 64)
    private String extractionMethod;

    @Column(name = "source_tier", length = 32)
    private String sourceTier; // FACT_CHECKER, WIRE, MAJOR_OUTLET, OTHER, SOCIAL, UNKNOWN

    @Column(name = "date_value")
    private Instant dateValue;

    @Column(name = "date_source", nullable = false, length = 32)
    private String dateSource = "UNKNOWN"; // PAGE_METADATA, VISIBLE_TEXT, UNKNOWN

    @Column(name = "mode", nullable = false, length = 16)
    private String mode; // "LIVE" | "REPLAY"

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "raw_json", columnDefinition = "JSONB")
    private String rawJson;

    public EvidenceEntity() {}

    public EvidenceEntity(UUID id, UUID investigationId, String kind, String contentSha256, Instant retrievedAt, String mode) {
        this.id = id;
        this.investigationId = investigationId;
        this.kind = kind;
        this.contentSha256 = contentSha256;
        this.retrievedAt = retrievedAt;
        this.mode = mode;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getInvestigationId() { return investigationId; }
    public void setInvestigationId(UUID investigationId) { this.investigationId = investigationId; }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public Instant getRetrievedAt() { return retrievedAt; }
    public void setRetrievedAt(Instant retrievedAt) { this.retrievedAt = retrievedAt; }

    public String getContentSha256() { return contentSha256; }
    public void setContentSha256(String contentSha256) { this.contentSha256 = contentSha256; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getExtractionMethod() { return extractionMethod; }
    public void setExtractionMethod(String extractionMethod) { this.extractionMethod = extractionMethod; }

    public String getSourceTier() { return sourceTier; }
    public void setSourceTier(String sourceTier) { this.sourceTier = sourceTier; }

    public Instant getDateValue() { return dateValue; }
    public void setDateValue(Instant dateValue) { this.dateValue = dateValue; }

    public String getDateSource() { return dateSource; }
    public void setDateSource(String dateSource) { this.dateSource = dateSource; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public String getRawJson() { return rawJson; }
    public void setRawJson(String rawJson) { this.rawJson = rawJson; }
}
