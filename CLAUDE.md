# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build (skip tests)
mvn clean package -DskipTests

# Run unit tests
mvn test

# Run a single test class
mvn test -Dtest=PersonTest

# Run all tests including integration tests
mvn verify
```

The build produces two runnable JARs:
- `target/singlerun.jar` — single simulation run (GUI or headless)
- `target/multirun.jar` — batch runs from a YAML config file

## Running the Simulation

```bash
# Single run (headless, UK, setup from scratch)
java -jar target/singlerun.jar -g false -c UK -Setup

# Multi-run batch from config
java -jar target/multirun.jar -config config/default.yml -g false
```

Key CLI flags: `-c` (country), `-s` (start year), `-e` (end year), `-g` (GUI true/false), `-Setup` (rebuild database), `-r` (random seed), `-p` (population size).

## Architecture

SimPaths is a discrete-time (annual steps) agent-based microsimulation framework built on the [JAS-mine](https://www.jas-mine.net/) engine. It projects life histories forward across labour, family, health, and financial domains.

### Agent Hierarchy

```
Household  →  BenefitUnit(s)  →  Person(s)
```

- **Person** (`simpaths/model/Person.java`) — individual agent; carries all demographics, health, education, labour, and income state.
- **BenefitUnit** (`simpaths/model/BenefitUnit.java`) — tax/benefit assessment unit (one or two adults + dependents).
- **Household** (`simpaths/model/Household.java`) — grouping of benefit units at the same address.

### Package Map

| Package | Responsibility |
|---|---|
| `simpaths/experiment/` | Entry points and orchestration: `SimPathsStart`, `SimPathsMultiRun`, `SimPathsCollector`, `SimPathsObserver` |
| `simpaths/model/` | Core simulation logic: agent classes, annual process methods, alignment, labour market, tax evaluation, intertemporal decisions |
| `simpaths/data/` | Parameters, setup routines, input parsers, filters, statistics helpers, regression managers, EUROMOD donor matching |

### Simulation Engine

`SimPathsModel.java` is the central manager registered with JAS-mine. It owns all agent collections and builds the ordered event schedule. Each simulated year runs **44 ordered processes** covering:
1. Year setup / parameter updates
2. Demographic events (ageing, mortality, fertility, education)
3. Labour market transitions
4. Partnership dynamics (cohabitation, separation, union matching via `UnionMatching.java`)
5. Health and wellbeing
6. Tax-benefit evaluation (via EUROMOD donor matching in `TaxEvaluation.java`)
7. Financial outcomes and aggregate alignment to calibration targets

### Configuration System

Runtime parameters live in `config/default.yml` (template) and are loaded by `SimPathsMultiRun`. The layered override order is: **class defaults → YAML values → CLI flags**.

Key top-level YAML keys: `maxNumberOfRuns`, `executeWithGui`, `randomSeed`, `startYear`, `endYear`, `popSize`. Model-specific keys toggle alignment, time-trend controls, and individual module switches.

### Data / Database

The initial population and EUROMOD donor data are stored in an embedded **H2 database** built during the `-Setup` phase. Integration tests that rebuild or query the database are in `src/test/java/simpaths/integrationtest/`.

## Key Tech

- **Java 19**, Maven 3.x
- **JAS-mine 4.3.25** — microsimulation engine and GUI
- **JUnit 5 + Mockito 5** for tests
- **Apache Commons Math3, CLI, CSV** and **SnakeYAML** for utilities

## Documentation

Detailed guides are in `documentation/`:
- `model-concepts.md` — agent lifecycle and annual-cycle detail
- `configuration.md` — YAML structure, config keys, and how to write your own
- `data-pipeline.md` — how input data is prepared and loaded
- `validation-guide.md` — model validation procedures
- `cli-reference.md` — full CLI argument reference