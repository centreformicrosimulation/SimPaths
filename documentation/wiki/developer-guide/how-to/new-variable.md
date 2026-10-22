# How to Introduce a New Variable

Adding a variable involves its definition, initial data, runtime updates, persistence, regressors and outputs. Changing a field declaration alone is not sufficient.

Start with the [variable codebook](https://github.com/simpaths/SimPaths/blob/b223738b9cdf1d814cc3c6f09b04bc4930d3c667/documentation/SimPaths_Variable_Codebook.xlsx). Its Rules, Modules and Coding Style sheets define the naming convention, and the Variables sheet records ownership and meanings. Check the [Stata parameter mapping](https://github.com/simpaths/SimPaths/blob/b223738b9cdf1d814cc3c6f09b04bc4930d3c667/documentation/SimPaths_Stata_Parameters.xlsx) when a variable depends on shared thresholds or units.

## Define the variable

Decide whether it belongs to `Person`, `BenefitUnit` or `Household`, and document its units, categories, missing values and update timing. Distinguish persistent state from temporary calculations and lagged values.

Use the codebook's naming convention instead of introducing an independent abbreviation. The `i_` prefix denotes local or temporary variables; these are not a mechanism for retaining state across restarts.

## Trace a current example

Ethnicity is represented by `demEthnC6`, not the historical `dot01` name. In [Person](https://github.com/simpaths/SimPaths/blob/b223738b9cdf1d814cc3c6f09b04bc4930d3c667/src/main/java/simpaths/model/Person.java):

```java
@Enumerated(EnumType.STRING)
private Ethnicity demEthnC6;
```

The initial-population [DataParser](https://github.com/simpaths/SimPaths/blob/b223738b9cdf1d814cc3c6f09b04bc4930d3c667/src/main/java/simpaths/data/startingpop/DataParser.java) maps the six numeric input codes to `White`, `Mixed`, `Asian`, `Black`, `Other` and `Missing`. The cloning constructor copies the field and the newborn constructor takes its value from the mother:

```java
demEthnC6 = originalPerson.demEthnC6;
```

```java
demEthnC6 = mother.getDemEthnC6();
```

These are excerpts from different constructors, not code to paste together. They illustrate why every route that creates an agent needs an explicit initial value.

## Update inputs and persistence

1. Add the variable to the appropriate data-construction stage and document the source and coding.
2. If it is read from the initial population, update the relevant input-column definition in `Parameters` and the parsing or conversion in `DataParser`.
3. Declare the entity field and accessors. Decide whether it is persisted or `@Transient`, and check any enum or column mapping.
4. Update construction, cloning and other entry routes such as birth or population alignment.
5. Rebuild the affected input database and test compatibility with existing saved populations.

`PERSON_VARIABLES_INITIAL` defines imported person columns; it is not a complete specification of every runtime or output field. Do not rename persisted enum values without addressing database compatibility.

## Update time-dependent state

For a lag, inspect both the field declaration and `Person.updateLaggedVariables()`, including any annotation-based and explicit updates used by your revision. Ensure the value is initialised before its first use and updated at the correct point in the yearly schedule. A transient lag is not restored automatically from a saved database.

Consult [SimPathsUK_Schedule.xlsx](https://github.com/simpaths/SimPaths/blob/b223738b9cdf1d814cc3c6f09b04bc4930d3c667/documentation/SimPathsUK_Schedule.xlsx) and the actual `SimPathsModel.buildSchedule()` before changing update order. Never reset the static entity ID counters to accommodate a new variable.

## Connect regressions and outputs

If a coefficient workbook refers to the variable, add the matching identifier and value mapping to the regressor interface on the relevant agent. For `Person`, inspect `DoublesVariables` and `getDoubleValue()`; do not assume a renamed model field automatically renames the exported Stata regressor.

Use `ManagerRegressions` and `RegressionName` when dispatching new regression-based processes. Check the exact workbook worksheet, outcome coding and coefficient names.

For reporting, review `SimPathsCollector` and the exported entity fields. A new summary statistic and a new person-level field require different changes.

## Verify before submitting

- Test initialisation, cloning and applicable birth or entry cases.
- Test lag updates and save/reload behaviour where relevant.
- Check input and output units, missing values and regression mappings.
- Run `mvn test` and `mvn verify`; integration tests are not included in `mvn test`.
- Run the affected process with training data and complete the relevant [validation](../../validation/index.md).
- Update the codebook and the user-facing process description with the implementation.

Do not commit restricted survey records, generated databases or research output files. See [Working in GitHub](../working-in-github.md) for the contribution workflow.
