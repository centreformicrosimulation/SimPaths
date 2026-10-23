# Consumption validation: LCFS and three v3 runs

The supplied results compare benefit-unit broad expenditure/disposable income (C/Y) in LCFS with three 50,000-person simulations for 2015–2026, seeds 606, 607 and 608. They use the v3 consumption estimates described in the [estimation README](../../input/regression_files/RegressionEstimates/consumption/README.md). Simulation coefficient bootstrapping and intertemporal optimisation were off; other recorded settings were identical apart from seed. The results retain the original validation conventions so that the change in consumption estimates can be assessed consistently.

## Definition and results

The statistic is the mean of benefit-unit C/Y, not aggregate expenditure divided by aggregate income. National income quintiles are defined separately within each year and dataset/run, before splitting by reference age. Survey uses `weighta`, complete-income scope, one/two-adult units and nominal weekly income of at least GBP10. Simulation is unweighted and uses real monthly income of at least GBP10; these income screens remain unequal in units and prices. Missing reference ages enter all-age statistics and are excluded from age-specific groups. The older reporting group is **65–84**, including where earlier workbook labels say “65+”.

Survey income sums personal net income and both income and broad expenditure deduct the same allocated Council Tax/domestic rates. The 2015 simulation has initialisation zeros, exported as missing; usable simulated consumption starts in 2016. Survey results cover 2015–2023; simulation results extend through 2026. Comparisons below use equal-year averages of annual means over the common usable period **2016–2023**.

| Reference age | LCFS bottom quintile | Three-run simulation | Seed 606 | Seed 607 | Seed 608 |
|---|---:|---:|---:|---:|---:|
| All ages | 1.339 | 1.446 | 1.449 | 1.444 | 1.445 |
| Under 45 | 1.165 | 1.516 | 1.516 | 1.515 | 1.518 |
| 45–64 | 1.894 | 1.635 | 1.650 | 1.622 | 1.631 |
| 65–84 | 1.137 | 0.976 | 0.976 | 0.980 | 0.973 |

At ages 45–64, aligning Council Tax boundaries and re-estimating reduces the average bottom-quintile gap compared with the saved v2 comparison (LCFS/simulation 2.155/1.634 before that revision). Most of this narrowing comes from the changed survey benchmark and quintile membership. The three-run v3 result remains below LCFS, while young-age overprediction and the older-age shortfall remain. The three runs' variability is smaller than these remaining gaps. These are descriptive comparisons; they do not supply uncertainty intervals for survey estimates, fitted parameters or model specification.

## Replication

Run the standalone Stata scripts from the repository root. Each validator requires a source directory and a separate output directory:

```stata
do "validation/consumption/consumption_sim_v3.do" "D:/simulation-run/csv" "D:/validation-output/seed_606"
do "validation/consumption/consumption_surv_v3.do" "D:/consumption-data/output_lcfs" "D:/validation-output"
```

The simulation output directory must already exist under its parent; the script creates only its final directory. The survey validator requires the after-local-tax `lcfs_sorted.dta`. Both preserve the existing sample/quintile rules and export full-precision statistics. They do not edit Excel workbooks.

`average_three_runs.do` calls both validators and exports the equal-weight average of run-specific annual statistics, together with individual results and sample standard deviations across runs:

```stata
do "validation/consumption/average_three_runs.do" ///
    "D:/run606/csv" "D:/run607/csv" "D:/run608/csv" ///
    "D:/consumption-data/output_lcfs" "D:/validation-output"
```

The wrapper labels the three inputs as seeds 606/607/608; supply them in that order. It does not recalculate quintiles after pooling runs. Three-run averaging changes validation statistics, not the fitted coefficients or covariance.

The aggregate CSV snapshots under `results` contain the survey benchmark, three-run mean, per-run statistics, annual across-run SDs and the common-period comparison. No survey or simulated person/benefit-unit microdata are included. Their source runs were `20261022175956_606_0`, `20261022181110_607_1` and `20261022182226_608_2`. Reproducing a 2015-start simulation also requires compatible starting-population and tax-donor inputs; this change does not alter the global run configuration or database/policy-year setup.

## Scope

Java changes supply the age-income interactions, reference-adult student status and centred trend required by the new FW1a coefficient names. The complete coefficient/covariance workbook and matching RMSE are installed together. Cash-income, pension double-counting and donor-selection mechanisms are unchanged by this update and remain separate follow-up work.
