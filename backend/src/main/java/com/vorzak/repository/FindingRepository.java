package com.vorzak.repository;

import com.vorzak.model.FindingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FindingRepository extends JpaRepository<FindingEntity, UUID> {
    List<FindingEntity> findByInvestigationIdOrderByCreatedAtAsc(UUID investigationId);
}
