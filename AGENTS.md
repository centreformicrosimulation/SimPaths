# AGENTS.md

Guidance for Codex when working with the SimPaths repository.

## Project Overview

SimPaths is a Java-based dynamic microsimulation framework for modelling individual and household life course events (careers, families, health, finances) over time. It is built on the [JAS-mine](https://www.jas-mine.net/) agent-based modelling framework and supports multiple country models (UK, Italy, Greece, Hungary, Poland). Country-specific logic is gated by the `Country` enum.

## Build & Run Commands

```bash
# Build (produces singlerun.jar and multirun.jar at repo root)
mvn clean package

# Unit tests only
mvn test

# All tests including integration tests
mvn verify

# Run a single simulation (with GUI)
java -jar singlerun.jar

# Run headless (no GUI)
java -jar singlerun.jar -c UK -s 2019 -g false --rewrite-policy-schedule

# Set up H2 database for multi-run
java -jar multirun.jar -DBSetup -config config/default.yml

# Run multiple sequential simulations
java -jar multirun.jar -config config/default.yml --persist=root

# Generate Javadoc
mvn javadoc:javadoc
```

## Branching & Workflow

- `main` — releases
- `develop` — active development; PRs target here by default
- `documentation` — MkDocs site source; pushes deploy to GitHub Pages
- Javadoc is published from `develop` to the `javadoc` branch

## Essential Reference Files

Before modifying code, consult the relevant reference file. These are authoritative and well-maintained:

| File | Location | Contents | When to consult |
|------|----------|----------|-----------------|
| **SimPaths_Variable_Codebook.xlsx** | `documentation/` | 398 variables with naming rules, prefix conventions, module abbreviations, coding style, and country-specific variables. Sheets: Variables, Rules, Modules, Abbreviations, Coding Style, Country Specific Vars | Before adding or modifying any field in Person, BenefitUnit, or Household |
| **SimPathsUK_Schedule.xlsx** | `documentation/` | All 48 simulation events in execution order, with outcome variables, target agents (model/persons/benefitUnits), regression file references, and Excel sheet names | Before modifying event scheduling in SimPathsModel, or to understand which regression drives which process |
| **SimPaths_Stata_Parameters.xlsx** | `documentation/` | Mapping between Stata parameter names/values and Java `Parameters` class constants | When working on Stata do-files or tracing parameter values between Stata and Java |
| **SimPaths_ARCHITECTURE.md** | repo root | Detailed execution flow, event scheduling, agent hierarchy, variable mapping patterns | For deep architecture questions beyond the quick reference below |

## Documentation Website

The full documentation is published at https://simpaths.org/. Read the relevant pages before working on unfamiliar areas.

**Always read first when onboarding to a new area:**
- https://simpaths.org/developer-guide/repository-guide/ — Complete repository structure, Java packages, Stata data pipeline (3 parts: data compilation, regression estimation, alignment targets), input file conventions, development workflow
- https://simpaths.org/developer-guide/internals/ — SimPathsModel class, API, Start/MultiRun implementation details

**Read when working on specific topics:**
- Read relevant pages at https://simpaths.org before working on unfamiliar areas

## Common Pitfalls

- Don't modify static ID counters (`personIdCounter`, `benefitUnitIdCounter`, `householdIdCounter`) during a simulation run
- Don't call `Parameters.getRegXxx()` directly for regression lookups — always use the `ManagerRegressions` dispatcher via `RegressionName` enum
- Lag variables (`L1`/`L2` suffix fields) are `@Transient` and NOT auto-populated; they must be explicitly set in the yearly update cycle before being read
- Enum values in `simpaths.model.enums` are used in JPA `@Enumerated(EnumType.STRING)` — renaming or reordering breaks database compatibility
- EUROMOD tax-benefit calculations only run on Windows; on other platforms, policy inputs are pre-computed Excel files
- `@Transient` fields (all `i_` prefix fields and lag fields) are not persisted to the H2 database
- Integration test CSV baselines use forced LF line endings (`.gitattributes`) — do not convert to CRLF
- The JAS-mine engine drives the event loop — changes to event scheduling belong in `SimPathsModel.buildSchedule()`, not in individual agent classes


## Regression System

- `RegressionName` enum defines all ~40 regression identifiers
- `ManagerRegressions` dispatches by enum to the correct regression routine in `Parameters`
- Coefficients are loaded from `reg_*.xlsx` files via `MultiKeyCoefficientMap` (JAS-mine)
- `Parameters.bootstrapAll` enables uncertainty quantification via coefficient resampling
- See `SimPathsUK_Schedule.xlsx` column "Regressions/name of excel sheets" for which regressions drive which simulation processes

## Testing

- Integration tests are excluded from `mvn test` (surefire); they run only with `mvn verify` (failsafe) and are named `*IntegrationTest.java`
- `RunSimPathsIntegrationTest.java` runs 3 scenarios: database setup, single-run, multi-run
- Expected CSV baselines live in `src/test/java/simpaths/integrationtest/expected/`
- `.gitattributes` in that directory forces LF line endings for reproducible CSV comparison
- Test configs: `config/test_run.yml` and `config/test_create_database.yml` (reduced population, training data)

## CI/CD

Defined in `.github/workflows/`:

- **`SimPathsBuild.yml`** — triggers on PRs to `main`/`develop`; builds, runs unit tests (`mvn verify`), then runs three integration test scenarios (single-run, multi-run with persist, multi-run without persist)
- **`deploy-docs.yml`** — triggers on push to `documentation` branch; deploys MkDocs site to GitHub Pages
- **`publish-javadoc.yml`** — triggers on push to `develop`; publishes Javadoc to `javadoc` branch

## MCP Tooling

- Always use the `stata-mcp` MCP server when a request involves Stata code, `.do` files, data inspection, regression output, or stored Stata results such as `r()` / `e()`
- Prefer running Stata commands via `stata-mcp` rather than answering from general knowledge when the request depends on actual Stata execution or dataset state

## Development Notes

- Java 19 target; Maven 3.8+ required
- Adding a new country requires adding a `Country` enum value and corresponding input data files
