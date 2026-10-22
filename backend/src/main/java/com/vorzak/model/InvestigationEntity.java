package com.vorzak.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "investigation")
public class InvestigationEntity {

    @Id
    private UUID id;

    @Column(name = "input_type", nullable = false, length = 32)
    private String inputType; // "UPLOAD" | "URL"

    @Column(name = "input_sha256", nullable = false, length = 64)
    private String inputSha256;

    @Column(name = "claim_text", nullable = false, columnDefinition = "TEXT")
    private String claimText;

    @Column(name = "state", nullable = false, length = 32)
    private String state; // CREATED, VALIDATING, DISCOVERING, ACQUIRING, COMPARING, ASSESSING, REFUTING, REPORTING, DONE, PARTIAL, FAILED

    @Column(name = "mode", nullable = false, length = 16)
    private String mode; // "LIVE" | "REPLAY"

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "agent_steps_used", nullable = false)
    private int agentStepsUsed = 0;

    public InvestigationEntity() {}

    public InvestigationEntity(UUID id, String inputType, String inputSha256, String claimText, String state, String mode) {
        this.id = id;
        this.inputType = inputType;
        this.inputSha256 = inputSha256;
        this.claimText = claimText;
        this.state = state;
        this.mode = mode;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getInputType() { return inputType; }
    public void setInputType(String inputType) { this.inputType = inputType; }

    public String getInputSha256() { return inputSha256; }
    public void setInputSha256(String inputSha256) { this.inputSha256 = inputSha256; }

    public String getClaimText() { return claimText; }
    public void setClaimText(String claimText) { this.claimText = claimText; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }

    public int getAgentStepsUsed() { return agentStepsUsed; }
    public void setAgentStepsUsed(int agentStepsUsed) { this.agentStepsUsed = agentStepsUsed; }
}
