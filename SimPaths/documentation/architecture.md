# Architecture

## High-level module map

Core package layout under `src/main/java/simpaths/`:

- `experiment/`: simulation entry points and orchestration
- `model/`: core simulation entities and yearly process logic
- `data/`: parameters, setup routines, filters, statistics helpers

## Primary entry points

- `simpaths.experiment.SimPathsStart`
  - Builds/refreshes setup artifacts
  - Launches single simulation run (GUI or headless)
- `simpaths.experiment.SimPathsMultiRun`
  - Loads YAML config
  - Iterates runs with optional seed/innovation logic
  - Supports persistence mode switching

## Runtime managers

The simulation engine registers:

- `SimPathsModel`: state evolution and process scheduling
- `SimPathsCollector`: statistics computation and export
- `SimPathsObserver`: GUI observation layer (when GUI is enabled)

## Data flow

1. Setup stage prepares policy schedule and input database.
2. Runtime model loads parameters and input maps.
3. Collector computes and exports statistics at scheduled intervals.
4. Output files are written to run folders under `output/`.

## Configuration flow

`SimPathsMultiRun` combines:

- defaults in class fields
- overrides from `config/<file>.yml`
- final CLI overrides at invocation time

This layered strategy supports reproducible batch runs with targeted command-line changes.
