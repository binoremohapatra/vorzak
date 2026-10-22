package com.vorzak.rules;

import com.vorzak.model.ComparisonEntity;
import com.vorzak.model.EvidenceEntity;
import com.vorzak.model.FindingEntity;
import com.vorzak.model.InvestigationEntity;
import com.vorzak.repository.FindingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class RulesEngineTest {

    private FindingRepository findingRepository;
    private RulesEngineService rulesEngineService;

    @BeforeEach
    void setUp() {
        findingRepository = Mockito.mock(FindingRepository.class);
        when(findingRepository.save(any(FindingEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        rulesEngineService = new RulesEngineService(
                new R1EarlierDifferentContextRule(),
                new R2EarlierSameContextRule(),
                new R3SimilarOnlyRule(),
                new R4NoSourceRule(),
                new R5RefuteFoundSupportRule(),
                findingRepository
        );
    }

    @Test
    void testR1ContradictedWhenEarlierDateAndAligned() {
        UUID invId = UUID.randomUUID();
        InvestigationEntity inv = new InvestigationEntity(invId, "UPLOAD", "sha_sub", "Photo shows 2024 flood in City X", "ASSESSING", "LIVE");

        UUID subId = UUID.randomUUID();
        EvidenceEntity sub = new EvidenceEntity(subId, invId, "SUBMITTED_IMAGE", "sha_sub", Instant.now(), "LIVE");

        UUID candId = UUID.randomUUID();
        EvidenceEntity cand = new EvidenceEntity(candId, invId, "CANDIDATE_IMAGE", "sha_cand", Instant.now(), "LIVE");
        cand.setSourceTier("FACT_CHECKER");
        cand.setSourceUrl("https://snopes.com/fact-check/flood-2019");
        cand.setDateValue(Instant.parse("2019-05-10T10:00:00Z"));
        cand.setDateSource("PAGE_METADATA");

        ComparisonEntity comp = new ComparisonEntity(UUID.randomUUID(), invId, subId, candId, "ALIGNED");
        comp.setInlierCount(45);
        comp.setSsimNormalized(0.91);
        comp.setDiffRegionCount(0);

        var res = rulesEngineService.evaluateAndSave(inv, sub, List.of(cand), List.of(comp), false);

        assertEquals("CONTRADICTED", res.primaryOutcome());
        assertEquals("MEDIUM", res.primaryConfidence());
        assertFalse(res.findings().isEmpty());
        // Enforce evidence trace constraint
        assertTrue(res.findings().get(0).getEvidenceIds().contains(candId));
    }

    @Test
    void testR4NoSourceWhenZeroComparisons() {
        UUID invId = UUID.randomUUID();
        InvestigationEntity inv = new InvestigationEntity(invId, "UPLOAD", "sha_sub", "Private family snapshot", "ASSESSING", "LIVE");
        EvidenceEntity sub = new EvidenceEntity(UUID.randomUUID(), invId, "SUBMITTED_IMAGE", "sha_sub", Instant.now(), "LIVE");

        var res = rulesEngineService.evaluateAndSave(inv, sub, List.of(), List.of(), false);

        assertEquals("NO_COMPARABLE_SOURCE", res.primaryOutcome());
        assertEquals("LOW", res.primaryConfidence());
        assertEquals(1, res.findings().size());
        assertEquals("R4_NO_SOURCE", res.findings().get(0).getRuleId());
    }
}
