# Data and Outputs

## Data availability model

- Source code and documentation are open.
- Full research input datasets are not freely redistributable.
- Training data is included to support development, local testing, and CI.

## Input directory layout

Key paths:

- `input/`:
  - regression and scenario Excel files (`reg_*.xlsx`, `scenario_*.xlsx`, `align_*.xlsx`)
  - generated setup files (`input.mv.db`, `EUROMODpolicySchedule.xlsx`, `DatabaseCountryYear.xlsx`)
- `input/InitialPopulations/`:
  - `training/population_initial_UK_2019.csv`
  - `compile/` scripts for preparing initial-population inputs
- `input/EUROMODoutput/`:
  - `training/*.txt` policy outputs and schedule artifacts

## Setup-generated artifacts

Running setup mode (`singlerun` setup or `multirun -DBSetup`) creates or refreshes:

- `input/input.mv.db`
- `input/EUROMODpolicySchedule.xlsx`
- `input/DatabaseCountryYear.xlsx`

## Output directory layout

Simulation runs produce timestamped folders under `output/`, typically with:

- `csv/` generated statistics and exported entities
- `database/` run-specific persistence output
- `input/` copied or persisted run input artifacts

Common CSV files include:

- `Statistics1.csv`
- `Statistics21.csv`
- `Statistics31.csv`
- `EmploymentStatistics1.csv`
- `HealthStatistics1.csv`

## Logging output

If `-f` is enabled with `multirun.jar`, logs are written to:

- `output/logs/run_<seed>.txt` (stdout capture)
- `output/logs/run_<seed>.log` (log4j output)

## Validation and analysis assets

- `validation/` contains validation artifacts and graph assets.
- `analysis/` contains `.do` scripts and spreadsheets used for downstream analysis.
