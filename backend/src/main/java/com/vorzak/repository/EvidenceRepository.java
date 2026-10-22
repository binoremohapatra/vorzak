package com.vorzak.repository;

import com.vorzak.model.EvidenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EvidenceRepository extends JpaRepository<EvidenceEntity, UUID> {
    List<EvidenceEntity> findByInvestigationIdOrderByRetrievedAtAsc(UUID investigationId);
    Optional<EvidenceEntity> findByInvestigationIdAndKind(UUID investigationId, String kind);
    List<EvidenceEntity> findByContentSha256(String contentSha256);
}
