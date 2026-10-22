package com.vorzak.rules;

import com.vorzak.model.ComparisonEntity;
import com.vorzak.model.EvidenceEntity;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Component
public class R2EarlierSameContextRule implements Rule {

    @Override
    public String getId() {
        return "R2_EARLIER_SAME_CONTEXT";
    }

    @Override
    public String getDescription() {
        return "Discovered earlier source matches the claimed context; historical syndication does not contradict.";
    }

    @Override
    public RuleResult evaluate(RuleEvaluationContext context) {
        // If candidate sources match claim keywords and date aligns
        for (EvidenceEntity candidate : context.candidateSources()) {
            if (candidate.getSourceUrl() != null && "WIRE".equalsIgnoreCase(candidate.getSourceTier())) {
                ComparisonEntity comp = findComparison(context.comparisons(), candidate.getId());
                if (comp != null && "ALIGNED".equalsIgnoreCase(comp.getAlignmentStatus())) {
                    Set<UUID> evidenceIds = new HashSet<>();
                    evidenceIds.add(candidate.getId());
                    if (context.submittedImage() != null) evidenceIds.add(context.submittedImage().getId());

                    String explanation = "Earliest appearance discovered on wire archive " + candidate.getSourceUrl() +
                            ". The image was syndicated within the claimed event timeframe; presence of archived image does not contradict the claim.";

                    ContradictionAspect aspect = new ContradictionAspect(
                            "EVENT_TIMELINE",
                            context.investigation().getClaimText(),
                            candidate.getSourceUrl(),
                            "SUPPORTS"
                    );

                    return new RuleResult(true, getId(), "INCONCLUSIVE", "MEDIUM", explanation, evidenceIds, aspect);
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
