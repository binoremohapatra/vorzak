package com.vorzak.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "task")
public class TaskEntity {

    @Id
    private UUID id;

    @Column(name = "investigation_id", nullable = false)
    private UUID investigationId;

    @Column(name = "channel", nullable = false, length = 32)
    private String channel; // SEARCH, FETCH, AGENT, DOWNLOAD, COMPARE, REFUTE

    @Column(name = "status", nullable = false, length = 32)
    private String status; // QUEUED, RUNNING, OK, EMPTY, BLOCKED, TIMEOUT, ERROR

    @Column(name = "tinyfish_run_id", length = 128)
    private String tinyfishRunId;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "deadline_at")
    private Instant deadlineAt;

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @Column(name = "error_text", columnDefinition = "TEXT")
    private String errorText;

    public TaskEntity() {}

    public TaskEntity(UUID id, UUID investigationId, String channel, String status) {
        this.id = id;
        this.investigationId = investigationId;
        this.channel = channel;
        this.status = status;
        this.startedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getInvestigationId() { return investigationId; }
    public void setInvestigationId(UUID investigationId) { this.investigationId = investigationId; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTinyfishRunId() { return tinyfishRunId; }
    public void setTinyfishRunId(String tinyfishRunId) { this.tinyfishRunId = tinyfishRunId; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getDeadlineAt() { return deadlineAt; }
    public void setDeadlineAt(Instant deadlineAt) { this.deadlineAt = deadlineAt; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    public String getErrorText() { return errorText; }
    public void setErrorText(String errorText) { this.errorText = errorText; }
}
