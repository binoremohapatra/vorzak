package com.vorzak.rules;

import com.vorzak.model.EvidenceEntity;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Component
public class R5RefuteFoundSupportRule implements Rule {

    @Override
    public String getId() {
        return "R5_REFUTE_FOUND_SUPPORT";
    }

    @Override
    public String getDescription() {
        return "Counter-evidence search discovered credible wire or fact-checker documentation affirming claim elements.";
    }

    @Override
    public RuleResult evaluate(RuleEvaluationContext context) {
        if (!context.refutePassExecuted()) {
            return new RuleResult(false, getId(), null, null, null, Set.of(), null);
        }

        for (EvidenceEntity candidate : context.candidateSources()) {
            if ("FACT_CHECKER".equalsIgnoreCase(candidate.getSourceTier()) &&
                candidate.getRawJson() != null && candidate.getRawJson().contains("SUPPORTS")) {
                Set<UUID> evidenceIds = new HashSet<>();
                evidenceIds.add(candidate.getId());
                if (context.submittedImage() != null) evidenceIds.add(context.submittedImage().getId());

                String explanation = "Refutation pass uncovered corroborating fact check analysis: " + candidate.getSourceUrl() +
                        ". Claim elements are independently supported by verified records.";

                ContradictionAspect aspect = new ContradictionAspect(
                        "REFUTATION_ANALYSIS",
                        context.investigation().getClaimText(),
                        candidate.getSourceUrl(),
                        "SUPPORTS"
                );

                return new RuleResult(true, getId(), "SUPPORTED", "MEDIUM", explanation, evidenceIds, aspect);
            }
        }

        return new RuleResult(false, getId(), null, null, null, Set.of(), null);
    }
}
