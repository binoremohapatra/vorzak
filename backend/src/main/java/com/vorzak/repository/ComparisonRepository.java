package com.vorzak.repository;

import com.vorzak.model.ComparisonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ComparisonRepository extends JpaRepository<ComparisonEntity, UUID> {
    List<ComparisonEntity> findByInvestigationId(UUID investigationId);
}
