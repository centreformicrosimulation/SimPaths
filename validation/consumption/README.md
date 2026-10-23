# Consumption model update (FW1a)

This update adds age-specific log-income slopes and a reference-adult student indicator, retaining log broad expenditure, the existing 14 age groups, common quadratic curvature and other controls. Java supplies the new regressors and the trend centred at 2015, preserving the existing 2023 trend freeze.

LCFS benefit-unit own income sums members' personal net incomes independently of expenditure shares. The same adult-share allocation of Council Tax or Northern Ireland domestic rates is deducted from both income and broad expenditure. Monthly amounts are expressed in 2015 prices using CPIH (ONS L522); transfers and exceptional purchases remain included. Estimation uses survey weights and household-within-wave clustered covariance.

The updated `input/regression_files/reg_wealth_financial.xlsx`, sheet `FW1a`, contains all 65 coefficients and the full covariance matrix. The matching RMSE is 0.6457928727473985 in `input/regression_files/reg_RMSE.xlsx`, sheet `UK`, entry `FW1a`. Other regression processes are unchanged. Cash-income and donor-matching algorithms are unchanged.

Dataset-construction and regression scripts, validation scripts, result snapshots and detailed documentation are maintained separately in the team's shared Drive workflow. They are outside this PR. The local documentation is titled `consumption_validation_v2_milestone.md`; subsequent Council Tax and three-run results are documented in `consumption_validation_v3_2026-10-22.md` and `v3_three_runs_20261022/validation_report.md` in the shared consumption validation materials.

## Automated prediction checks

`ConsumptionRegressionTest` checks Java predictions against 700 deterministic synthetic Stata cases, including log predictions, RMSE and conditional means. It also checks age boundaries, the income floor and trend freezing. The fixtures contain no observed LCFS records. Java tests use the checked-in CSV and require neither Stata nor access to the shared Drive.

Run from the repository root:

```powershell
mvn -o -Dtest=ConsumptionRegressionTest test
```

When replacing the estimated model, regenerate the fixtures from the matching saved m4 Stata estimates supplied through the shared Drive:

```stata
do "src/test/stata/export_consumption_fixtures.do" ///
    "D:/consumption-data/output_writing/consumption_model_housing_status.ster" ///
    "src/test/resources/simpaths/model/consumption_stata_fixtures.csv"
```

The helper matches Java's GBP1 monthly income floor, double-precision square and 2023 trend freeze. Update coefficients, covariance, RMSE and fixtures together.
