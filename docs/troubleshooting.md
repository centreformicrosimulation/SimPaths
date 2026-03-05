# Troubleshooting

## `Config file <name> not found`

Cause:

- `-config` points to a file not present in `config/`.

Fix:

- Verify filename and extension.
- Example:

```bash
java -jar multirun.jar -config default.yml
```

## Missing `EUROMODpolicySchedule.xlsx`

Cause:

- Setup has not generated schedule files yet.

Fix:

- Re-run setup with rewrite enabled:

```bash
java -jar singlerun.jar -c UK -s 2019 -g false --rewrite-policy-schedule -Setup
```

## GUI errors on server or CI

Cause:

- Running GUI mode in headless environment.

Fix:

- Disable GUI:

```bash
-g false
```

## Start year rejected or inconsistent

Cause:

- Chosen year is outside available input/training data bounds.

Fix:

- Use a year covered by available input files.
- For training-only mode, use the provided training start year (2019 in this repository setup).

## Expected CSV files not found after run

Cause:

- Collector settings disabled certain exports.
- Run failed before collector dump phase.

Fix:

- Check `collector_args` in YAML.
- Re-run with `-f` and inspect `output/logs/run_<seed>.txt` and `.log`.

## Integration test output mismatch

Cause:

- Simulation behavior changed or output schema changed.

Fix:

1. Confirm differences are intended.
2. Replace expected files in `src/test/java/simpaths/integrationtest/expected/` with verified new outputs.
3. Re-run:

```bash
mvn verify
```
