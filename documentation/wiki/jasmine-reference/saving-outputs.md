# Saving Outputs

SimPaths separates collector exports for analysis from population persistence used to initialise or restart runs. For run settings, see [Statistical display](../overview/modules/statistical-display.md) and [Multiple Runs](../user-guide/multiple-runs.md). This page explains the implementation.

## Entity mappings {#1-persistence}

The model uses Jakarta Persistence annotations on `Person`, `BenefitUnit` and `Household`. Inspect the actual field and relationship mappings before extending output or persistence. An `@Entity` annotation alone is not a complete output configuration.

Transient state is not automatically restored from a database. Lags and temporary process variables may need explicit initialisation when starting or restarting a simulation. See [Introduce a New Variable](../developer-guide/how-to/new-variable.md) for the full change checklist.

## Keys and relationships {#2-keys}

In the [checked Person implementation](https://github.com/simpaths/SimPaths/blob/b223738b9cdf1d814cc3c6f09b04bc4930d3c667/src/main/java/simpaths/model/Person.java), the key is declared as:

```java
@EmbeddedId
@Column(unique = true, nullable = false)
private final PanelEntityKey key;
```

Relationships use mapped joins. For example, the person-to-benefit-unit mapping includes ID, simulation time, simulation run and working-ID join columns. Do not assume that a single agent ID uniquely identifies a saved record across all snapshots.

Use the entity constructors and existing relationship-update methods. Never reset static entity-ID counters during a run. Enum values persisted as strings also form part of the data interface; renaming them can make earlier databases incompatible.

## Collector export {#3-the-dataexport-class}

The [collector](https://github.com/simpaths/SimPaths/blob/b223738b9cdf1d814cc3c6f09b04bc4930d3c667/src/main/java/simpaths/experiment/SimPathsCollector.java) creates a `DataExport` for each enabled output collection. For example:

```java
if (persistPersons) {
    exportPersons = new DataExport(
        model.getPersons(), exportToDatabase, exportToCSV);
}
```

Scheduled dump events call `export()` on the corresponding object. Summary-statistic events update the statistic before exporting it. Correct event order is therefore part of output correctness, not just presentation.

`dataDumpStartTime` is used as an offset from the model's start year in the checked collector schedule. `dataDumpTimePeriod` sets the interval between exports. Inspect `buildSchedule()` when changing either, and check which years appear in a short test run.

## CSV files {#4-export-to-csv}

`exportToCSV` enables CSV export under the run's output directory. Entity-level and summary-statistic switches select the tables. The collector also writes an output README describing the exported data.

Inspect the generated headers, identifiers and years before combining runs. Do not treat filename suffixes as a universal run-number convention. Java object relationships and transient fields are not a substitute for explicit analysis variables in the export.

Retain the source revision, configuration, seeds and input versions alongside outputs. Keep licensed or confidential microdata out of public repositories and issue attachments.

## Database output {#5-export-to-database}

`exportToDatabase` enables collector database output separately from CSV. This is not the same option as multi-run population persistence. Choose the facility required by the analysis or restart workflow rather than enabling database output indiscriminately.

See [Querying the Database](querying-database.md) for read-only inspection of saved databases and [Statistical Package](statistical-package.md) when adding a statistic.
