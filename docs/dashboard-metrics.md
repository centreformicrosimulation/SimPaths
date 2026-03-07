# Dashboard Metrics (V1)

This document defines the metrics shown in the SimPaths dashboard shell.

## Scope
- Intended use: default outcomes dashboard when domain-specific entities/metrics are not provided.
- UI hierarchy: headline KPIs -> trends -> region breakdown table.

## Metrics
- `Weighted Population`
Formula: `sum(person_weight_scaled)` across all persons.
`person_weight_scaled = person.weight` when `useWeights = true`, otherwise `person.weight * scalingFactor`.

- `Employed Share`
Formula: `sum(person_weight_scaled * employed_flag) / sum(person_weight_scaled)`.
`employed_flag` is `Person.getEmployed()` (1 if employed, else 0).

- `Median EDI`
National KPI source: `SimPathsCollector.getStats().getEdi_p50()`.
Region table source: weighted median of valid `BenefitUnit.getEquivalisedDisposableIncomeYearly()` in each region.

- `Poverty Share`
Formula: `sum(benefit_unit_weight_scaled * at_risk_flag) / sum(benefit_unit_weight_scaled)` over valid-income benefit units.
`at_risk_flag` is `BenefitUnit.getAtRiskOfPoverty()` (0/1).

## Validity Rules
- A benefit unit is considered income-valid when:
  - `equivalisedDisposableIncomeYearly` is finite
  - `equivalisedDisposableIncomeYearly >= 0`
  - `equivalisedDisposableIncomeYearly < 1,000,000`

## Wiring
- Provider class: `simpaths.experiment.DashboardMetricsProvider`
- UI class: `simpaths.experiment.DashboardShellFrame`
- Observer integration: `simpaths.experiment.SimPathsObserver`
