package com.vorzak.rules;

import com.vorzak.model.ComparisonEntity;
import com.vorzak.model.EvidenceEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Component
public class R1EarlierDifferentContextRule implements Rule {

    @Override
    public String getId() {
        return "R1_EARLIER_DIFFERENT_CONTEXT";
    }

    @Override
    public String getDescription() {
        return "An evidence source with a trusted date earlier than the claim's date contains the near-identical image in a different context.";
    }

    @Override
    public RuleResult evaluate(RuleEvaluationContext context) {
        if (context.candidateSources() == null || context.candidateSources().isEmpty()) {
            return new RuleResult(false, getId(), null, null, null, Set.of(), null);
        }

        // Search for candidate sources with trusted dates (dateSource != UNKNOWN)
        for (EvidenceEntity candidate : context.candidateSources()) {
            if (!"UNKNOWN".equalsIgnoreCase(candidate.getDateSource()) && candidate.getDateValue() != null) {
                // Check if aligned comparison exists
                ComparisonEntity comp = findComparison(context.comparisons(), candidate.getId());
                boolean isAligned = comp != null && "ALIGNED".equalsIgnoreCase(comp.getAlignmentStatus());

                if (isAligned) {
                    Set<UUID> evidenceIds = new HashSet<>();
                    evidenceIds.add(candidate.getId());
                    if (context.submittedImage() != null) {
                        evidenceIds.add(context.submittedImage().getId());
                    }

                    String tier = candidate.getSourceTier();
                    boolean isHighTier = "FACT_CHECKER".equalsIgnoreCase(tier) ||
                                         "WIRE".equalsIgnoreCase(tier) ||
                                         "MAJOR_OUTLET".equalsIgnoreCase(tier);
                    String confidence = isHighTier ? "MEDIUM" : "LOW";

                    String explanation = String.format(
                            "Discovered earlier appearance published on %s (%s) from %s (%s). The image visually matches with %d keypoint inliers, but pre-dates the claimed context.",
                            candidate.getDateValue().toString(),
                            candidate.getDateSource(),
                            candidate.getSourceUrl(),
                            candidate.getSourceTier(),
                            comp.getInlierCount() != null ? comp.getInlierCount() : 0
                    );

                    ContradictionAspect aspect = new ContradictionAspect(
                            "DATE_AND_CONTEXT",
                            context.investigation().getClaimText(),
                            candidate.getSourceUrl() + " published on " + candidate.getDateValue(),
                            "CONTRADICTS"
                    );

                    return new RuleResult(true, getId(), "CONTRADICTED", confidence, explanation, evidenceIds, aspect);
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
