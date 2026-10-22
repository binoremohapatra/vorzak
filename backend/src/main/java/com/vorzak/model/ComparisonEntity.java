package com.vorzak.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "comparison")
public class ComparisonEntity {

    @Id
    private UUID id;

    @Column(name = "investigation_id", nullable = false)
    private UUID investigationId;

    @Column(name = "submitted_evidence_id", nullable = false)
    private UUID submittedEvidenceId;

    @Column(name = "candidate_evidence_id", nullable = false)
    private UUID candidateEvidenceId;

    @Column(name = "alignment_status", nullable = false, length = 32)
    private String alignmentStatus; // ALIGNED, NOT_COMPARABLE

    @Column(name = "inlier_count")
    private Integer inlierCount;

    @Column(name = "ssim_normalized")
    private Double ssimNormalized;

    @Column(name = "diff_region_count")
    private Integer diffRegionCount;

    @Column(name = "diff_image_path", columnDefinition = "TEXT")
    private String diffImagePath;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "params_json", columnDefinition = "JSONB")
    private String paramsJson;

    public ComparisonEntity() {}

    public ComparisonEntity(UUID id, UUID investigationId, UUID submittedEvidenceId, UUID candidateEvidenceId, String alignmentStatus) {
        this.id = id;
        this.investigationId = investigationId;
        this.submittedEvidenceId = submittedEvidenceId;
        this.candidateEvidenceId = candidateEvidenceId;
        this.alignmentStatus = alignmentStatus;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getInvestigationId() { return investigationId; }
    public void setInvestigationId(UUID investigationId) { this.investigationId = investigationId; }

    public UUID getSubmittedEvidenceId() { return submittedEvidenceId; }
    public void setSubmittedEvidenceId(UUID submittedEvidenceId) { this.submittedEvidenceId = submittedEvidenceId; }

    public UUID getCandidateEvidenceId() { return candidateEvidenceId; }
    public void setCandidateEvidenceId(UUID candidateEvidenceId) { this.candidateEvidenceId = candidateEvidenceId; }

    public String getAlignmentStatus() { return alignmentStatus; }
    public void setAlignmentStatus(String alignmentStatus) { this.alignmentStatus = alignmentStatus; }

    public Integer getInlierCount() { return inlierCount; }
    public void setInlierCount(Integer inlierCount) { this.inlierCount = inlierCount; }

    public Double getSsimNormalized() { return ssimNormalized; }
    public void setSsimNormalized(Double ssimNormalized) { this.ssimNormalized = ssimNormalized; }

    public Integer getDiffRegionCount() { return diffRegionCount; }
    public void setDiffRegionCount(Integer diffRegionCount) { this.diffRegionCount = diffRegionCount; }

    public String getDiffImagePath() { return diffImagePath; }
    public void setDiffImagePath(String diffImagePath) { this.diffImagePath = diffImagePath; }

    public String getParamsJson() { return paramsJson; }
    public void setParamsJson(String paramsJson) { this.paramsJson = paramsJson; }
}
