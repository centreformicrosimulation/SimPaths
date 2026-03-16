# SimPaths Documentation

These files are a **CLI- and developer-workflow quick reference** for working directly with the repository — building, running, configuring, and troubleshooting from the command line. For the full model documentation (simulated modules, parameterisation, GUI usage, country variants, research), see the [website](../documentation/wiki/index.md).

---

## Recommended reading order

1. [Model Concepts](model-concepts.md) — what SimPaths simulates, agents, annual cycle, alignment, EUROMOD
2. [Getting Started](getting-started.md) — prerequisites, build, first run
3. [CLI Reference](cli-reference.md) — all flags for `singlerun.jar` and `multirun.jar`
4. [Configuration](configuration.md) — YAML structure, config keys, and how to write your own
5. [Data and Outputs](data-and-outputs.md) — input layout, setup artifacts, output files
6. [Troubleshooting](troubleshooting.md) — common errors and fixes

For contributors and advanced users:

- [Development and Testing](development.md) — build, tests, CI, contributor workflow
- [Data Pipeline](data-pipeline.md) — how input files are generated from UKHLS/EUROMOD/WAS survey data
- [Validation Guide](validation-guide.md) — two-stage validation workflow (estimate validation + simulated output validation)

## Conventions

- Commands are shown from the repository root.
- Paths are relative to the repository root.
- `default.yml` refers to `config/default.yml`.
