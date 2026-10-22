package com.vorzak.repository;

import com.vorzak.model.InvestigationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvestigationRepository extends JpaRepository<InvestigationEntity, UUID> {
    Optional<InvestigationEntity> findByInputSha256(String inputSha256);
}
