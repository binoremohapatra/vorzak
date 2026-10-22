package com.vorzak.rules;

import com.vorzak.model.ComparisonEntity;
import com.vorzak.model.EvidenceEntity;
import com.vorzak.model.FindingEntity;
import com.vorzak.model.InvestigationEntity;
import com.vorzak.repository.FindingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class RulesEngineService {

    private final List<Rule> rules;
    private final FindingRepository findingRepository;

    public RulesEngineService(
            R1EarlierDifferentContextRule r1,
            R2EarlierSameContextRule r2,
            R3SimilarOnlyRule r3,
            R4NoSourceRule r4,
            R5RefuteFoundSupportRule r5,
            FindingRepository findingRepository
    ) {
        this.rules = List.of(r5, r1, r2, r3, r4);
        this.findingRepository = findingRepository;
    }

    public record AssessmentResult(
            String primaryOutcome,
            String primaryConfidence,
            List<FindingEntity> findings,
            List<Rule.ContradictionAspect> contradictionMatrix
    ) {}

    @Transactional
    public AssessmentResult evaluateAndSave(
            InvestigationEntity investigation,
            EvidenceEntity submittedImage,
            List<EvidenceEntity> candidateSources,
            List<ComparisonEntity> comparisons,
            boolean refutePassExecuted
    ) {
        Rule.RuleEvaluationContext ctx = new Rule.RuleEvaluationContext(
                investigation,
                submittedImage,
                candidateSources,
                comparisons,
                refutePassExecuted
        );

        List<FindingEntity> findings = new ArrayList<>();
        List<Rule.ContradictionAspect> contradictionMatrix = new ArrayList<>();
        String primaryOutcome = "INCONCLUSIVE";
        String primaryConfidence = "LOW";

        for (Rule rule : rules) {
            Rule.RuleResult res = rule.evaluate(ctx);
            if (res.fired()) {
                // Rule 2 Enforcement: Every finding MUST have at least one non-null evidence ID
                if (res.evidenceIds() == null || res.evidenceIds().isEmpty()) {
                    throw new IllegalStateException("Non-negotiable violation: Finding " + res.ruleId() + " has zero linked evidence IDs!");
                }

                FindingEntity finding = new FindingEntity(
                        UUID.randomUUID(),
                        investigation.getId(),
                        res.ruleId(),
                        res.outcome(),
                        res.confidence(),
                        res.explanation()
                );
                finding.getEvidenceIds().addAll(res.evidenceIds());

                FindingEntity saved = findingRepository.save(finding);
                findings.add(saved);

                if (res.contradictionAspect() != null) {
                    contradictionMatrix.add(res.contradictionAspect());
                }

                // First decisive rule establishes primary outcome
                if ("INCONCLUSIVE".equals(primaryOutcome) || "CONTRADICTED".equals(res.outcome()) || "SUPPORTED".equals(res.outcome())) {
                    primaryOutcome = res.outcome();
                    primaryConfidence = res.confidence();
                }
            }
        }

        // If no rules fired, default to R4 (NO_COMPARABLE_SOURCE)
        if (findings.isEmpty()) {
            FindingEntity fallback = new FindingEntity(
                    UUID.randomUUID(),
                    investigation.getId(),
                    "R4_NO_SOURCE",
                    "NO_COMPARABLE_SOURCE",
                    "LOW",
                    "No comparable earlier sources discovered across search queries."
            );
            if (submittedImage != null) {
                fallback.getEvidenceIds().add(submittedImage.getId());
            }
            findings.add(findingRepository.save(fallback));
            primaryOutcome = "NO_COMPARABLE_SOURCE";
            primaryConfidence = "LOW";
        }

        return new AssessmentResult(primaryOutcome, primaryConfidence, findings, contradictionMatrix);
    }
}
