# SimPaths Documentation

These files are a **CLI- and developer-workflow quick reference** for working directly with the repository — building, running, configuring, and troubleshooting from the command line. For the full model documentation (simulated modules, parameterisation, GUI usage, country variants, research), see the [website](../documentation/wiki/index.md).

---

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

## Conventions

- Commands are shown from the repository root.
- Paths are relative to the repository root.
- `default.yml` refers to `config/default.yml`.
