package com.vorzak.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "finding")
public class FindingEntity {

    @Id
    private UUID id;

    @Column(name = "investigation_id", nullable = false)
    private UUID investigationId;

    @Column(name = "rule_id", nullable = false, length = 64)
    private String ruleId;

    @Column(name = "outcome", nullable = false, length = 32)
    private String outcome; // SUPPORTED, CONTRADICTED, INCONCLUSIVE, NO_COMPARABLE_SOURCE

    @Column(name = "confidence", nullable = false, length = 16)
    private String confidence; // LOW, MEDIUM, HIGH

    @Column(name = "explanation", nullable = false, columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "finding_evidence", joinColumns = @JoinColumn(name = "finding_id"))
    @Column(name = "evidence_id", nullable = false)
    private Set<UUID> evidenceIds = new HashSet<>();

    public FindingEntity() {}

    public FindingEntity(UUID id, UUID investigationId, String ruleId, String outcome, String confidence, String explanation) {
        this.id = id;
        this.investigationId = investigationId;
        this.ruleId = ruleId;
        this.outcome = outcome;
        this.confidence = confidence;
        this.explanation = explanation;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getInvestigationId() { return investigationId; }
    public void setInvestigationId(UUID investigationId) { this.investigationId = investigationId; }

    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }

    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }

    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Set<UUID> getEvidenceIds() { return evidenceIds; }
    public void setEvidenceIds(Set<UUID> evidenceIds) { this.evidenceIds = evidenceIds; }
}
