Use the `flowchart-update` skill.

Review committed SimPaths code change `e08fbbf46`.

## Detector Result

Revision: `HEAD`
Commit: `e08fbbf46`

Changed code files:
- src/main/java/simpaths/model/SimPathsModel.java

Priority modules:
- none

File-level candidate modules:
- household_composition (Household Composition) -> documentation/flowcharts/modules/household_composition.md [state: up_to_date]
- cohabitation (Cohabitation UK Case) -> documentation/flowcharts/modules/cohabitation.md [state: up_to_date]
- full_time_hourly_earnings (Full-Time Hourly Earnings) -> documentation/flowcharts/modules/full_time_hourly_earnings.md [state: up_to_date]
- fertility_give_birth (Fertility and GiveBirth) -> documentation/flowcharts/modules/fertility_give_birth.md [state: up_to_date]
- health_long_term_sick (Health and Long-Term Sick) -> documentation/flowcharts/modules/health_long_term_sick.md [state: up_to_date]
- health_mental_hm1_hm2_cases (Health Mental HM1 and HM2 Cases) -> documentation/flowcharts/modules/health_mental_hm1_hm2_cases.md [state: up_to_date]
- health_mental_hm1_hm2_level (Health Mental HM1 and HM2 Level) -> documentation/flowcharts/modules/health_mental_hm1_hm2_level.md [state: up_to_date]
- union_matching (Union Matching) -> documentation/flowcharts/modules/union_matching.md [state: up_to_date]
- inschool (InSchool) -> documentation/flowcharts/modules/inschool.md [state: up_to_date]

## Manifest Flagging

Manifest updates: 9
- household_composition: up_to_date -> candidate_for_review; last_trigger_commit 4f9935ce8 -> e08fbbf46
- cohabitation: up_to_date -> candidate_for_review; last_trigger_commit 4f9935ce8 -> e08fbbf46
- full_time_hourly_earnings: up_to_date -> candidate_for_review; last_trigger_commit 4f9935ce8 -> e08fbbf46
- fertility_give_birth: up_to_date -> candidate_for_review; last_trigger_commit 4f9935ce8 -> e08fbbf46
- health_long_term_sick: up_to_date -> candidate_for_review; last_trigger_commit 4f9935ce8 -> e08fbbf46
- health_mental_hm1_hm2_cases: up_to_date -> candidate_for_review; last_trigger_commit 4f9935ce8 -> e08fbbf46
- health_mental_hm1_hm2_level: up_to_date -> candidate_for_review; last_trigger_commit 4f9935ce8 -> e08fbbf46
- union_matching: up_to_date -> candidate_for_review; last_trigger_commit 4f9935ce8 -> e08fbbf46
- inschool: up_to_date -> candidate_for_review; last_trigger_commit 4f9935ce8 -> e08fbbf46
Manifest skips: 0

## Tasks

1. Inspect the committed code diff for `e08fbbf46`.
2. Review priority modules first.
3. Treat non-priority file-level matches as candidates, not confirmed documentation changes.
4. Update flowchart Markdown only where documented logic, schedule context, state dependencies, notes, or traceability metadata changed.
5. If Mermaid is unchanged, say so explicitly.
6. Update `documentation/flowcharts/modules.yml` review state:
   - false positives -> `up_to_date`
   - updated modules -> `up_to_date` after checking
   - use `updated_unverified` only if not fully checked
7. Rerun the detector with `--update-manifest` and confirm idempotency.

Do not update flowcharts for formatting-only, comment-only, or implementation-only changes that do not alter documented logic. Redraw Mermaid only when control flow changes.
