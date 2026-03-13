# Architecture

For a conceptual overview of the simulation (agents, annual cycle, modules, alignment), see [Model Concepts](model-concepts.md). This page covers source-level structure and data flow.

---

## High-level module map

Core package layout under `src/main/java/simpaths/`:

| Package | Contents |
|---------|----------|
| `experiment/` | Entry points, orchestration, and runtime managers (`SimPathsStart`, `SimPathsMultiRun`, `SimPathsCollector`, `SimPathsObserver`) |
| `model/` | Core simulation entities (`Person`, `BenefitUnit`, `Household`), yearly process logic, alignment routines, labour market, union matching, tax evaluation, intertemporal decision module |
| `data/` | Parameters, setup routines, input parsers, filters, statistics helpers |

---

## Primary entry points

### `simpaths.experiment.SimPathsStart`

- Builds or refreshes setup artifacts (H2 database, policy schedule)
- Launches a single simulation run, GUI or headless

### `simpaths.experiment.SimPathsMultiRun`

- Loads a YAML config from `config/`
- Iterates runs with optional seed or innovation logic
- Supports persistence mode switching across runs

---

## Runtime managers

All three are registered with the JAS-mine simulation engine at startup. They live in `simpaths.experiment`:

| Class | Role |
|-------|------|
| `SimPathsModel` | Owns the agent collections, builds the event schedule, fires yearly processes |
| `SimPathsCollector` | Computes and exports statistics at scheduled intervals |
| `SimPathsObserver` | GUI observation layer, only active when GUI is enabled |

---

## Data flow

1. **Setup stage** — `SimPathsStart` or `multirun -DBSetup` generates `input/input.mv.db`, `input/EUROMODpolicySchedule.xlsx`, and `input/DatabaseCountryYear.xlsx`.
2. **Initialisation** — `SimPathsModel.buildObjects()` loads parameters, reads the initial population CSV, and hydrates agent collections.
3. **Yearly loop** — `SimPathsModel.buildSchedule()` registers all process events in fixed order. Each year the engine fires them sequentially across `Person`, `BenefitUnit`, and model-level processes. See [Model Concepts — Annual simulation cycle](model-concepts.md#annual-simulation-cycle) for the full ordered list.
4. **Collection** — `SimPathsCollector` computes cross-sectional statistics and writes CSV outputs at the end of each year.
5. **Output** — files land in timestamped run folders under `output/`.

---

## Configuration flow

`SimPathsMultiRun` applies values in three layers (later layers override earlier ones):

1. Class field defaults
2. Values from `config/<file>.yml`
3. CLI flags provided at invocation

This layered strategy supports reproducible batch runs with targeted command-line overrides without editing YAML files.
