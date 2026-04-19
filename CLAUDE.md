# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Important: Directory Path Contains Spaces

The project root is `C:\MyFiles\99 DEV ENV\JAS-MINE\SimPaths`. The path contains spaces, so always wrap paths in double quotes in shell/Bash commands, e.g.:

```bash
cd "C:\MyFiles\99 DEV ENV\JAS-MINE\SimPaths"
mvn -f "C:\MyFiles\99 DEV ENV\JAS-MINE\SimPaths\pom.xml" compile
```

## Build & Run Commands

```bash
# Compile only
mvn compile

# Run unit tests (excludes integration tests)
mvn test

# Run a single test class
mvn test -Dtest=MahalanobisDistanceTest

# Run a single test method
mvn test -Dtest=PersonTest#methodName

# Run integration tests (requires pre-built JARs)
mvn failsafe:integration-test

# Build shaded JARs (produces singlerun.jar and multirun.jar in project root)
mvn package

# Run simulation headless via multirun.jar (uses config/default.yml)
java -jar multirun.jar -config config/default.yml

# Run database setup only
java -jar multirun.jar -DBSetup -config config/default.yml

# Run with GUI (single run)
java -jar singlerun.jar
```

Java 19 is required (configured in pom.xml). Dependencies are pulled from Maven Central and JitPack (for JAS-mine).

## Architecture Overview

SimPaths is a **dynamic microsimulation model** built on the [JAS-mine](https://github.com/jasmineRepo) simulation framework. It projects individual and household life histories year-by-year across domains: labour market, health, demographics, social care, and taxes/benefits.

### Simulation entity hierarchy

```
Household          — groups BenefitUnits sharing a dwelling (parent–adult-child links)
  └─ BenefitUnit  — the tax/benefit assessment unit (couple or single + dependent children)
       └─ Person  — the individual agent (all life-course state is stored here)
```

All three are JPA `@Entity` classes persisted via Hibernate/H2. Each carries a `PanelEntityKey` (id + simulation_time + simulation_run).

### Simulation lifecycle

`SimPathsModel` (extends `AbstractSimulationManager`) owns the main simulation loop. Each year it fires ordered events that update entities via `EventListener.onEvent()`. The sequence covers: mortality → union formation/dissolution → fertility → education → labour market → taxes/benefits → health → social care → statistics collection.

### Key classes to know

| Class | Role |
|---|---|
| `SimPathsModel` | Orchestrates yearly event schedule; holds all population lists |
| `SimPathsStart` | Entry point for single runs (GUI or headless) |
| `SimPathsMultiRun` | Entry point for multi-run / sensitivity analysis |
| `SimPathsCollector` | Collects and exports output statistics each year |
| `Parameters` | Static store for all model parameters and regression coefficients loaded from Excel |
| `ManagerRegressions` | Routes regression evaluation calls to the correct JAS-mine regression objects |
| `Person` / `BenefitUnit` | Core simulation agents — most behavioural logic lives here |

### Tax-benefit imputation (`model/taxes/`)

Tax and benefit outcomes are not calculated analytically. Instead, the model uses **statistical matching**: simulated households are matched to a pre-built donor database derived from EUROMOD/UKMOD output. `TaxDonorDataParser` builds this database; `DonorTaxImputation` performs the matching using a nearest-neighbour key function (`KeyFunction1`–`KeyFunction4`).

### Intertemporal optimisation (`model/decisions/`)

An optional backward-induction module solves for optimal lifetime consumption and labour supply. `ManagerSolveGrids` runs the solution over a multi-dimensional state-space grid (age, health, education, pension, region, etc.). `ManagerPopulateGrids` sets up the grid geometry. This is computationally expensive and disabled by default (`enableIntertemporalOptimisations: false`).

### Regression system

All behavioural equations are estimated externally and loaded at startup from Excel files in `input/`. `Parameters` loads them via JAS-mine's `ExcelAssistant`/`MultiKeyCoefficientMap`. `RegressionName` (enum) names every equation; `ManagerRegressions` dispatches calls by type (linear, probit, ordered probit, multinomial logit, etc.).

### Alignment

Several demographic processes are aligned to external targets (ONS projections, LFS shares) via `ActivityAlignmentV2`, `FertilityAlignment`, `PartnershipAlignment`, `InSchoolAlignment`, `SocialCareAlignment`. Alignment factors are exported to `AlignmentAdjustmentFactors1.csv` each run.

### Configuration

Runs are configured via YAML files in `config/`. `default.yml` documents all available keys with their defaults. CLI flags override YAML values. `SimPathsMultiRun` reads the YAML and reflectively sets fields on `SimPathsModel`, `SimPathsCollector`, and `Parameters`.

### Output

Output is written to `output/<run-timestamp>/csv/` by `SimPathsCollector`. Key files:
- `Statistics1.csv` — income distribution (Gini, percentiles, S-Index)
- `Statistics2.csv` — demographic validation (partnership, employment, health by age/gender)
- `EmploymentStatistics.csv`, `HealthStatistics.csv` — domain-specific time series
- `AlignmentAdjustmentFactors1.csv` — alignment diagnostics

### Integration tests

`RunSimPathsIntegrationTest` runs the full simulation end-to-end using the built JARs and compares CSV output against reference files in `src/test/java/simpaths/integrationtest/expected/`. If a substantive change shifts the output, update the expected files and commit them.