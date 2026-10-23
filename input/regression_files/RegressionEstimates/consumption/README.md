# LCFS consumption estimation (FW1a)

FW1a models log real monthly benefit-unit broad expenditure using LCFS waves indexed 2008–2023. It retains the existing age groups, allows age-specific linear log-income slopes, and uses common quadratic curvature and a reference-adult student indicator. Other controls cover composition, work, pension receipt, housing status and region. The age reference category is under 20 (18–19 in the retained adult sample).

## Construction and estimation

`collect_lcfs.do` uses household expenditure P550tp, recorded personal expenditure and the existing benefit-unit housing/residual attribution rule. Benefit-unit own income sums members' P177/P177p personal net income, independently of expenditure shares. The same adult-share allocation of Council Tax or Northern Ireland domestic rates is deducted from both income and broad expenditure. Transfers and exceptional purchases remain included. The active data tags are `personal_net_ct` and `broad_net_ct`.

`consumption_modelling_lcfs.do` converts weekly amounts to monthly equivalents in 2015 prices using CPIH (ONS L522). It requires complete income scope, positive expenditure/income and one or two adults. Estimation uses `weighta` and covariance clustered by household within survey wave. The trend is centred at 2015; 2008 is the reference year and the 2023 indicator is additionally omitted for collinearity with the trend. The script exports all 65 coefficients, the complete clustered covariance, RMSE, fit statistics, saved estimates, reader tables and age-specific elasticity contrasts.

The installed v3 estimates have N=97,107, 82,923 clusters, R-squared 0.5523814416590156 and RMSE 0.6457928727473985. The `lcfs_model_sample.dta` working file is saved before the final one/two-adult regression restriction and contains 97,918 rows; it is not exactly `e(sample)`. Elasticity reference incomes are the unweighted percentiles calculated in that log-usable working sample.

Run from the repository root, supplying an external consumption data root:

```stata
local source_root "D:/your/consumption-data"
do "input/regression_files/RegressionEstimates/consumption/collect_lcfs.do" "`source_root'"
do "input/regression_files/RegressionEstimates/consumption/consumption_modelling_lcfs.do" "`source_root'"
```

The first command requires the original `data/LCFS.../stata` wave folders and filenames listed in the collection script. If prepared `output_lcfs/lcfs_sorted.dta` is already available, only the second command is needed. Both scripts accept an optional second argument for a separate existing export root. They use the current working directory if the first argument is omitted. Raw/prepared survey data and generated intermediate files are not part of this directory. Tested with StataNow 19.

Install the coefficient/covariance CSV into `input/regression_files/reg_wealth_financial.xlsx`, sheet `FW1a`, and the matching RMSE into `input/regression_files/reg_RMSE.xlsx`, sheet `UK`, entry `FW1a`. Preserve other processes and workbook layouts. The similarly named CSV under `output_writing` is a reader table, not the computational matrix.

## Synthetic Java–Stata checks

`export_synthetic_fixtures.do` reads saved m4 estimates and creates a deterministic grid of **700 synthetic cases**, not observed LCFS rows. It spans 14 age bands, two student states, five income levels and five years, with varying region, housing and composition controls. It matches Java's GBP1 monthly income floor, double-precision square and 2023 trend freeze. Future-year indicators remain zero, matching the omitted 2023 indicator.

```stata
do "input/regression_files/RegressionEstimates/consumption/export_synthetic_fixtures.do" ///
    "`source_root'/output_writing/consumption_model_housing_status.ster" ///
    "src/test/resources/simpaths/model/consumption_stata_fixtures.csv"
```

`ConsumptionRegressionTest` loads the actual workbooks and compares log predictions, RMSE and conditional means with these Stata results. It also checks age boundaries, the income floor and trend freezing. Regenerate the fixtures whenever the estimated model changes.

See [consumption validation](../../../../validation/consumption/README.md) for the three-run comparison and remaining gaps.
