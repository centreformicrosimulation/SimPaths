# Querying the Database

SimPaths uses JAS-mine and Hibernate to map Java entities to database records. Distinguish the input and donor databases from the optional databases written by a simulation.

## Querying relationships at run time {#1-querying-the-database-at-run-time}

The model's population hierarchy is:

| Entity | Relationship |
| --- | --- |
| `Household` | Contains benefit units |
| `BenefitUnit` | Belongs to a household and contains persons |
| `Person` | Belongs to a benefit unit |

For an existing `Person person`, navigation through the mapped relationships looks like this:

```java
BenefitUnit benefitUnit = person.getBenefitUnit();
Household household = benefitUnit.getHousehold();
```

See [Person](https://github.com/simpaths/SimPaths/blob/b223738b9cdf1d814cc3c6f09b04bc4930d3c667/src/main/java/simpaths/model/Person.java), [BenefitUnit](https://github.com/simpaths/SimPaths/blob/b223738b9cdf1d814cc3c6f09b04bc4930d3c667/src/main/java/simpaths/model/BenefitUnit.java) and [Household](https://github.com/simpaths/SimPaths/blob/b223738b9cdf1d814cc3c6f09b04bc4930d3c667/src/main/java/simpaths/model/Household.java) for the mappings.

Do not assume that traversing an object graph always loads every related record. Fetch behaviour depends on the relationship annotation and persistence-session state. In-memory simulation state can also differ from the last persisted snapshot.

The entity classes use `PanelEntityKey` composite keys. When writing queries, inspect the actual schema and time/run keys instead of assuming that a single numeric ID uniquely identifies a record across all snapshots.

## Reading coefficients

Most regression parameters are read from Excel rather than queried from a population table. For example, the parameter loader uses:

```java
ExcelAssistant.loadCoefficientMap(
    Parameters.getInputDirectory() + "reg_health.xlsx",
    "H1",
    1
);
```

The workbook and worksheet names are part of the interface between estimation and simulation. See [Model Parameterisation](../overview/parameterisation.md) and [Regression Library](regression-library.md) before changing their structure.

## Inspecting a saved database {#2-inspecting-the-database-before-or-after-a-simulation-has-completed}

1. Identify the database from the run configuration and logs. `input/input.mv.db` holds prepared initial-population and tax-benefit donor tables; output databases depend on persistence and collector settings.
2. Stop the process using the database before opening it separately. Work on a copy for inspection so that research inputs and saved outputs remain intact.
3. Use an H2-compatible client and driver matching the project's dependency. Inspect the available tables and columns before running queries.
4. Use read-only queries. Do not edit IDs, relationships or categories to bypass a setup error.

A database may contain confidential microdata even when the model code is public. Do not upload it to a public issue or third-party query service.

[Multiple Runs](../user-guide/multiple-runs.md) explains population-persistence options. [Saving outputs](saving-outputs.md) describes the separate collector output facilities.
