# VORZAK Provenance Benchmark Suite

A standardized benchmark template evaluating VORZAK across known, public fact-checked image cases.
Every case tests whether the discovery, date extraction, and rule evaluation determine the earliest discovered appearance without guessing or hallucinating.

## Benchmark Matrix

| Case ID | Image Subject / Claim | Claimed Date & Place | Actual Earliest Discovered Source | Expected Finding | Expected Alignment | Notes / Challenge |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **CASE-001** | Military tank column during 2024 conflict | Oct 2024, Eastern Europe | 2018 Training Exercise (Reuters) | CONTRADICTED | ALIGNED | Outdated footage re-captioned with new conflict context. |
| **CASE-002** | Flooded city square after hurricane | Sep 2024, Florida | Sep 2024, Florida (AP Wire) | SUPPORTED | ALIGNED | Authentic news photo matching claimed event and place. |
| **CASE-003** | AI-generated protest crowd with deformed hands | Nov 2024, London | First indexed on social platform, no news match | NO_COMPARABLE_SOURCE | NOT_COMPARABLE | Synthetic image with zero prior historical appearances. |
| **CASE-004** | Protest banner with spliced inflammatory text | Jun 2023, Paris | Jun 2023 Paris protest photo without banner text (AFP) | CONTRADICTED | ALIGNED (diff_region_count >= 1) | Real protest context, but banner altered; image worker detects spliced region. |
| **CASE-005** | Wildfire smoke plume over landmark | Aug 2023, Maui | Aug 2023 Maui wildfire (Local News) | SUPPORTED | ALIGNED | Multiple wire syndications confirm date and location. |
| **CASE-006** | Celebrity wedding event | Dec 2022, Venice | Dec 2022 movie shoot (Film Archive) | CONTRADICTED | ALIGNED | Commercial production frame falsely claimed as private wedding. |
| **CASE-007** | Extreme earthquake rubble damage | Feb 2023, Turkey | 2015 Nepal earthquake (Getty Images) | CONTRADICTED | ALIGNED | Historic catastrophe photo recycled in breaking disaster coverage. |
| **CASE-008** | Obscure street mural photo | Unknown date / local neighborhood | No discovery matches found in index | NO_COMPARABLE_SOURCE | NOT_COMPARABLE | Private photograph never publicly indexed online. |

## Evaluation Criteria
1. **Never says "Fake" or "Real"**: Reports are strictly based on earliest discovered date and visual differences.
2. **Missing Dates**: If a source lacks an explicit timestamp, `date_value = NULL` and `date_source = UNKNOWN`.
3. **No Hallucinated Ancestors**: If zero matches exist, status must be `NO_COMPARABLE_SOURCE`.
4. **Traceability**: Every finding references specific `evidence_id` records in the evidence ledger.
