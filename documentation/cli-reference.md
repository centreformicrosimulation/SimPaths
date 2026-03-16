# CLI Reference

## `singlerun.jar` (`SimPathsStart`)

Usage:

```bash
java -jar singlerun.jar [options]
```

### Options

| Option | Meaning |
|---|---|
| `-s`, `--startYear <year>` | Simulation start year |
| `-Setup` | Setup only (do not run simulation) |
| `-Run` | Run only (skip setup) |
| `-r`, `--rewrite-policy-schedule` | Rebuild policy schedule from policy files |
| `-g`, `--showGui <true/false>` | Enable or disable GUI |
| `-h`, `--help` | Print help |

Notes:

- `-Setup` and `-Run` are mutually exclusive.
- For non-GUI environments, use `-g false`.

### Examples

Setup only:

```bash
java -jar singlerun.jar -s 2019 -g false -Setup --rewrite-policy-schedule
```

Run only (after setup exists):

```bash
java -jar singlerun.jar -g false -Run
```

## `multirun.jar` (`SimPathsMultiRun`)

Usage:

```bash
java -jar multirun.jar [options]
```

### Options

| Option | Meaning |
|---|---|
| `-p`, `--popSize <int>` | Simulated population size |
| `-s`, `--startYear <year>` | Start year |
| `-e`, `--endYear <year>` | End year |
| `-DBSetup` | Database setup mode |
| `-n`, `--maxNumberOfRuns <int>` | Number of sequential runs |
| `-r`, `--randomSeed <int>` | Seed for first run |
| `-g`, `--executeWithGui <true/false>` | Enable or disable GUI |
| `-config <file>` | Config file in `config/` (default: `default.yml`) |
| `-f` | Write stdout and logs to `output/logs/` |
| `-P`, `--persist <root|run|none>` | Persistence strategy for processed dataset |
| `-h`, `--help` | Print help |

Persistence modes:

- `root` (default): persist to root input area for reuse
- `run`: persist per run output folder
- `none`: no processed-data persistence

### Examples

Create setup database using config:

```bash
java -jar multirun.jar -DBSetup -config test_create_database.yml
```

Run two simulations with root persistence:

```bash
java -jar multirun.jar -config test_run.yml -P root
```

Run without persistence and with file logging:

```bash
java -jar multirun.jar -config default.yml -P none -f
```
