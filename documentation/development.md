# Development and Testing

## Build

Compile and package:

```bash
mvn clean package
```

## Tests

### Unit tests

Run unit tests (Surefire):

```bash
mvn test
```

### Integration tests

Run integration tests (Failsafe):

```bash
mvn verify
```

Integration tests exercise setup and run flows and compare generated CSV outputs to expected files in:

- `src/test/java/simpaths/integrationtest/expected/`

## CI workflows

GitHub workflows in `.github/workflows/` run:

- build and package on pull requests to `main` and `develop`
- integration tests (`mvn verify`)
- smoke runs for `singlerun.jar` and `multirun.jar` with persistence variants
- Javadoc generation and publish (on `develop` pushes)

## Javadoc

Generate locally:

```bash
mvn javadoc:javadoc
```

## Typical contributor flow

1. Create a feature branch in your fork.
2. Implement and test changes.
3. Run `mvn verify` before opening a PR.
4. Open a PR against `develop` (or `main` for stable fixes, when appropriate).

## Debugging tips

- Use `-g false` on headless systems.
- Use `-f` with `multirun.jar` to capture logs in `output/logs/`.
- Start from `config/test_create_database.yml` and `config/test_run.yml` when reproducing CI behavior.
