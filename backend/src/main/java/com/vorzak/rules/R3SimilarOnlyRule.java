package com.vorzak.rules;

import com.vorzak.model.ComparisonEntity;
import com.vorzak.model.EvidenceEntity;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Component
public class R3SimilarOnlyRule implements Rule {

    @Override
    public String getId() {
        return "R3_SIMILAR_ONLY";
    }

    @Override
    public String getDescription() {
        return "Images are visually similar or aligned, but discovered sources lack trusted date context.";
    }

    @Override
    public RuleResult evaluate(RuleEvaluationContext context) {
        for (EvidenceEntity candidate : context.candidateSources()) {
            if ("UNKNOWN".equalsIgnoreCase(candidate.getDateSource()) || candidate.getDateValue() == null) {
                ComparisonEntity comp = findComparison(context.comparisons(), candidate.getId());
                if (comp != null && "ALIGNED".equalsIgnoreCase(comp.getAlignmentStatus())) {
                    Set<UUID> evidenceIds = new HashSet<>();
                    evidenceIds.add(candidate.getId());
                    if (context.submittedImage() != null) evidenceIds.add(context.submittedImage().getId());

                    String explanation = "Discovered visually corresponding image on " + candidate.getSourceUrl() +
                            ", but the source lacks verified publication date metadata. Evidence is inconclusive.";

                    ContradictionAspect aspect = new ContradictionAspect(
                            "DATE_METADATA",
                            "Claimed date in claim text",
                            "Date source UNKNOWN",
                            "UNVERIFIED"
                    );

                    return new RuleResult(true, getId(), "INCONCLUSIVE", "LOW", explanation, evidenceIds, aspect);
                }
            }
        }
        return new RuleResult(false, getId(), null, null, null, Set.of(), null);
    }

    private ComparisonEntity findComparison(java.util.List<ComparisonEntity> list, UUID candidateEvidenceId) {
        if (list == null) return null;
        for (ComparisonEntity c : list) {
            if (c.getCandidateEvidenceId().equals(candidateEvidenceId)) return c;
        }
        return null;
    }
}
