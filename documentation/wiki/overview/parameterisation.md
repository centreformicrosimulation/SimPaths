# Model Parameterisation

The UK model combines estimated behavioural relationships, demographic and alignment targets, tax-benefit donor data, and scenario assumptions. A simulation's specification depends on both its code revision and the input files supplied to it.

The [repository input directory](https://github.com/simpaths/SimPaths/tree/develop/input) is the canonical file inventory. The [parameter-loading code](https://github.com/simpaths/SimPaths/blob/b649c6967db2d9cced068c8878a2979725dc39e0/src/main/java/simpaths/data/Parameters.java) identifies the workbooks and worksheets used by the implementation. The existence of a file alone does not establish that every run uses it: country, module switches and scenario settings also matter.

## Tax and benefit inputs {#1-description-of-the-tax-and-benefit-system-display}

SimPaths imputes tax-benefit outcomes from precomputed UKMOD donor files in `input/EUROMODoutput/`. The policy schedule maps simulation years to policy systems. [Tax-Benefit Donors (UK)](../getting-started/data/tax-benefit-donors-uk.md) explains how to obtain and prepare the files; [Modifying Tax-Benefit Settings](../user-guide/tax-benefit-parameters.md) explains scenario configuration.

Training donor data are available in the repository for testing. They are not a substitute for authorised research data.

## Parameters and targets {#2-model-parameters}

| File family | Role |
| --- | --- |
| `reg_*.xlsx` | Regression coefficients and, where provided, covariance matrices. Some workbooks contain multiple processes; `reg_RMSE.xlsx` contains residual error scales rather than a separate behavioural equation. |
| `align_*.xlsx` and `alignment_targets_*.xlsx` | Targets used to align selected simulated outcomes. |
| `projections_*.xlsx` | Demographic projections, including fertility and mortality inputs. |
| `scenario_*.xlsx` | Time paths and assumptions for the selected scenario. |
| `EUROMODpolicySchedule.xlsx` | Maps simulation years to tax-benefit systems. |
| `social_care_parameters.xlsx` | Social-care policy parameters. |
| `economic_time_series.xlsx` | Price and wage indices, returns, debt costs and care wages. |
| `alignment_adjustment_series.xlsx` | Time-varying adjustments for alignment processes. |

The [Repository Guide's estimation reference](../developer-guide/repository-guide.md#data-pipeline-reference) maps UK Stata scripts to the outcomes they estimate. This is distinct from the workbook inventory: a script can update several workbooks, and a workbook can supply several processes.

For example, `reg_health.xlsx` supplies self-rated health and disability; `reg_health_wellbeing.xlsx` supplies SF-12 physical and mental health scores and life satisfaction; and `reg_financial_distress.xlsx` supplies financial distress. These should not be treated as interchangeable health specifications.

## Interpreting estimates and references

The [SimPaths reference paper](https://microsimulation.pub/articles/00318) provides the model-level description. The [Labour supply](modules/labour-income.md) and [Mental health](modules/mental-health.md) pages explain the corresponding processes.

Richiardi and He's 2021 paper, “No one left behind: The labour supply behaviour of the entire Italian population”, concerns the Italian application. It should not be read as an inventory of the current UK coefficient estimates. For UK work, consult the labour-supply specification and the parameter files for the revision being used.

Kopasker et al. (2024), [Evaluating the influence of taxation and social security policies on psychological distress](https://doi.org/10.1016/j.socscimed.2024.116953), describes a UK application. Later development versions can change its implementation or extend the outcomes; the paper and current code are not automatically identical.

## Reproducible use

Record the code revision, parameter-file versions, initial population, donor data, policy schedule and configuration with each analysis. After re-estimation, check coefficient names, outcome coding, worksheet names and covariance matrices against the Java loader, then perform the relevant [validation](../validation/index.md). Do not mix parameter files from different revisions without checking compatibility.

See [Uncertainty Analysis](../user-guide/uncertainty-analysis.md) for the distinction between coefficient resampling, simulation randomness and sensitivity to assumptions.
