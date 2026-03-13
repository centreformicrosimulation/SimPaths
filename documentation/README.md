# SimPaths Documentation

This documentation is structured to support both first-time users and contributors.

## Recommended reading order

1. [Model Concepts](model-concepts.md) — what SimPaths simulates, agents, annual cycle, alignment, EUROMOD
2. [Getting Started](getting-started.md) — prerequisites, build, first run
3. [CLI Reference](cli-reference.md) — all flags for `singlerun.jar` and `multirun.jar`
4. [Configuration](configuration.md) — YAML structure and all config keys
5. [Scenario Cookbook](scenario-cookbook.md) — provided configs and how to build your own
6. [Data and Outputs](data-and-outputs.md) — input layout, setup artifacts, output files
7. [Troubleshooting](troubleshooting.md) — common errors and fixes

For contributors and advanced users:

- [Architecture](architecture.md) — source package structure and data flow
- [Development and Testing](development.md) — build, tests, CI, contributor workflow

## Scope

These guides cover:

- Understanding the simulation model and its mechanisms
- Building SimPaths with Maven
- Running single-run and multi-run workflows
- Configuring model, collector, and runtime behavior via YAML
- Understanding expected input/output files and generated artifacts
- Running unit and integration tests locally and in CI

## Conventions

- Commands are shown from the repository root.
- Paths are relative to the repository root.
- `default.yml` refers to `config/default.yml`.
