# SimPaths Documentation

These files are a **quick reference** for working directly with the repository — building, running, configuring, and troubleshooting from the command line. For the full model documentation (simulated modules, parameterisation, GUI usage, research), see the [website](../documentation/wiki/index.md).

---

## Recommended reading order

1. [Model Concepts](model-concepts.md) — what SimPaths simulates, agents, annual cycle, alignment, EUROMOD
2. [Configuration](configuration.md) — prerequisites, quick run, YAML structure, config keys
3. [Repository Structure](repository-structure.md) — directory layout, input files, output files
4. [CLI Reference](cli-reference.md) — all flags for `singlerun.jar` and `multirun.jar`
5. [Troubleshooting](troubleshooting.md) — common errors and fixes

For contributors and advanced users:

- [Data Pipeline](data-pipeline.md) — how input files are generated from UKHLS/EUROMOD/WAS survey data
- [Validation Guide](validation-guide.md) — two-stage validation workflow (estimate validation + simulated output validation)

## Conventions

- Commands are shown from the repository root.
- Paths are relative to the repository root.
- `default.yml` refers to `config/default.yml`.
