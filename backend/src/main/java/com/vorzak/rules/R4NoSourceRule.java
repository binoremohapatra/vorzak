package com.vorzak.rules;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Component
public class R4NoSourceRule implements Rule {

    @Override
    public String getId() {
        return "R4_NO_SOURCE";
    }

    @Override
    public String getDescription() {
        return "No comparable earlier source or visual alignment candidate discovered across index queries.";
    }

    @Override
    public RuleResult evaluate(RuleEvaluationContext context) {
        boolean hasAligned = false;
        if (context.comparisons() != null) {
            for (var c : context.comparisons()) {
                if ("ALIGNED".equalsIgnoreCase(c.getAlignmentStatus())) {
                    hasAligned = true;
                    break;
                }
            }
        }

        if (context.candidateSources() == null || context.candidateSources().isEmpty() || !hasAligned) {
            Set<UUID> evidenceIds = new HashSet<>();
            if (context.submittedImage() != null) {
                evidenceIds.add(context.submittedImage().getId());
            }

            String explanation = "Automated discovery queries yielded no comparable earlier appearance or valid visual alignment candidate across search index tiers. VORZAK cannot verify prior appearance.";

            ContradictionAspect aspect = new ContradictionAspect(
                    "PROVENANCE_TRACE",
                    context.investigation().getClaimText(),
                    "Zero comparable ancestors discovered",
                    "UNVERIFIED"
            );

            return new RuleResult(true, getId(), "NO_COMPARABLE_SOURCE", "LOW", explanation, evidenceIds, aspect);
        }

        return new RuleResult(false, getId(), null, null, null, Set.of(), null);
    }
}
