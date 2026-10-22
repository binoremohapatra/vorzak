package com.vorzak.rules;

import com.vorzak.model.ComparisonEntity;
import com.vorzak.model.EvidenceEntity;
import com.vorzak.model.InvestigationEntity;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface Rule {
    String getId();
    String getDescription();
    RuleResult evaluate(RuleEvaluationContext context);

    record RuleEvaluationContext(
            InvestigationEntity investigation,
            EvidenceEntity submittedImage,
            List<EvidenceEntity> candidateSources,
            List<ComparisonEntity> comparisons,
            boolean refutePassExecuted
    ) {}

    record RuleResult(
            boolean fired,
            String ruleId,
            String outcome, // SUPPORTED, CONTRADICTED, INCONCLUSIVE, NO_COMPARABLE_SOURCE
            String confidence, // LOW, MEDIUM, HIGH
            String explanation,
            Set<UUID> evidenceIds,
            ContradictionAspect contradictionAspect
    ) {}

    record ContradictionAspect(
            String aspect, // "DATE", "LOCATION", "EVENT", "VISUAL_MUTATION"
            String claimClaimed,
            String evidenceFound,
            String status // "CONTRADICTS", "SUPPORTS", "UNVERIFIED"
    ) {}
}
