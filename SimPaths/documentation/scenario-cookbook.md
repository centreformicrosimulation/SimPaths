# Scenario Cookbook

This guide maps every provided YAML scenario in `config/` to its intended use.

All commands below assume you are running from repository root after building jars.

## Baseline and testing scenarios

### `default.yml`

Use when you want the standard baseline run with conservative defaults.

Command:

```bash
java -jar multirun.jar -config default.yml -g false
```

### `test_create_database.yml`

Use for test-oriented database setup with training data (`trainingFlag: true`).

Command:

```bash
java -jar multirun.jar -DBSetup -config test_create_database.yml
```

### `test_run.yml`

Use for integration-style short runs (2 runs, test settings).

Command:

```bash
java -jar multirun.jar -config test_run.yml -P root
```

### `programming test.yml`

Use for quick developer smoke runs with smaller population and simplified behavior flags.

Command:

```bash
java -jar multirun.jar -config "programming test.yml" -g false
```

## Setup-focused scenario

### `create database.yml`

Use to build a full database object set for UK long-horizon work. This file sets `flagDatabaseSetup: true` in `innovation_args`, so it runs setup mode.

Command:

```bash
java -jar multirun.jar -config "create database.yml"
```

## Sensitivity and robustness scenarios

### `random seed.yml`

Use to run multiple replications with random-seed iteration enabled.

Command:

```bash
java -jar multirun.jar -config "random seed.yml" -g false
```

### `intertemporal elasticity.yml`

Use for intertemporal elasticity sensitivity (3 runs with interest-rate innovation pattern).

Command:

```bash
java -jar multirun.jar -config "intertemporal elasticity.yml" -g false
```

### `labour supply elasticity.yml`

Use for labour-supply elasticity sensitivity (3 runs with labour-income innovation pattern).

Command:

```bash
java -jar multirun.jar -config "labour supply elasticity.yml" -g false
```

## Targeted output scenarios

### `employmentTransStats.yml`

Use when you mainly want employment transition statistics and minimal other persisted outputs.

Command:

```bash
java -jar multirun.jar -config employmentTransStats.yml -g false
```

## Social care scenario family

### `sc calibration.yml`

Use to calibrate preference parameters for social care analysis.

Command:

```bash
java -jar multirun.jar -config "sc calibration.yml" -g false
```

### `sc analysis0.yml`

Base social care analysis run with social care enabled and alignment on.

Command:

```bash
java -jar multirun.jar -config "sc analysis0.yml" -g false
```

### `sc analysis1.yml`

Main social care analysis run with named behavioral grid output (`saveBehaviour: true`, `readGrid: "sc analysis1"`).

Command:

```bash
java -jar multirun.jar -config "sc analysis1.yml" -g false
```

### `sc analysis1b.yml`

Variant of analysis1 with `alignPopulation: false` and `useSavedBehaviour: true` for comparison.

Command:

```bash
java -jar multirun.jar -config "sc analysis1b.yml" -g false
```

### `sc analysis2.yml`

Zero-costs social care scenario (`flagSuppressChildcareCosts: true`, `flagSuppressSocialCareCosts: true`).

Command:

```bash
java -jar multirun.jar -config "sc analysis2.yml" -g false
```

### `sc analysis3.yml`

Ignore-costs response scenario that reuses behavior from analysis2 (`useSavedBehaviour: true`, `readGrid: "sc analysis2"`).

Command:

```bash
java -jar multirun.jar -config "sc analysis3.yml" -g false
```

## Practical notes

- Use quotes around config filenames that contain spaces.
- Add `-f` to write run logs to `output/logs/`.
- Override config values via CLI flags when needed (for example `-n`, `-r`, `-P`, `-g`).
