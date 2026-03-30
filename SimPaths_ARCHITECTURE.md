# SimPaths — ARCHITECTURE.md

## PART A — Structure and Data Loading

### 1. Entry Points and Execution Flow

#### 1.1 Single-run mode — SimPathsStart

```java
// src/main/java/simpaths/experiment/SimPathsStart.java
public class SimPathsStart implements ExperimentBuilder
```

main() body (lines 62–124, condensed):

```java
public static void main(String[] args) {
    if (!parseCommandLineArgs(args)) { return; }

    if (showGui) {
        runGUIdialog();
    } else {
        try {
            if (doSetup) runGUIlessSetup(4);
        } catch (FileNotFoundException f) { ... }
    }

    if (!doRun) { System.out.println("Setup complete, exiting."); return; }

    MultiKeyCoefficientMap lastDatabaseCountryAndYear =
        ExcelAssistant.loadCoefficientMap(
            Parameters.getInputDirectory() + Parameters.DatabaseCountryYearFilename + ".xlsx",
            "Data", 1);
    // sets country and startYear from the Excel map

    final SimulationEngine engine = SimulationEngine.getInstance();
    SimPathsStart experimentBuilder = new SimPathsStart();
    engine.setExperimentBuilder(experimentBuilder);
    engine.setup();   // triggers buildExperiment() then buildObjects() then buildSchedule()

    if (!showGui) {
        engine.startSimulation();
        engine.quit();
    }
}
```

CLI flags parsed by parseCommandLineArgs():

| **Flag** | **Effect** |
| --- | --- |
| -c CC / --country CC | country = Country.valueOf(CC) |
| -s year / --startYear | Sets startYear |
| -Setup | doSetup=true; doRun=false |
| -Run | doRun=true; doSetup=false |
| -r / --rewrite-policy-schedule | rewritePolicySchedule=true |
| -g true/false / --showGui | Sets showGui |
| -h / --help | Prints usage, returns false |

buildExperiment() (lines 225–237) — JAS-mine callback:

```java
@Override
public void buildExperiment(SimulationEngine engine) {
    SimPathsModel model = new SimPathsModel(country, startYear);
    SimPathsCollector collector = new SimPathsCollector(model);
    SimPathsObserver observer = new SimPathsObserver(model, collector);
    engine.addSimulationManager(model);
    engine.addSimulationManager(collector);
    if (showGui) engine.addSimulationManager(observer);
    model.setCollector(collector);
}
```

Full headless single-run call chain:

```java
SimPathsStart.main()
  → parseCommandLineArgs(args)
  → runGUIlessSetup(4)                          [if doSetup]
      → Parameters.loadTimeSeriesFactorMaps(country)
      → Parameters.instantiateAlignmentMaps()
      → Parameters.databaseSetup(country, showGui, startYear)
          → DataParser.databaseFromCSV(country, showGui)
              → DriverManager.getConnection("jdbc:h2:file:input/input;...", "sa", "")
              → DataParser.createDatabaseForPopulationInitialisationByYearFromCSV(...)
                  → DataParser.parse(csvPath, tableName, conn, country, year)  [per year]
  → ExcelAssistant.loadCoefficientMap(...)
  → SimulationEngine.getInstance()
  → engine.setExperimentBuilder(new SimPathsStart())
  → engine.setup()
      → SimPathsStart.buildExperiment(engine)
          → new SimPathsModel(country, startYear)
          → new SimPathsCollector(model)
          → engine.addSimulationManager(model)
          → engine.addSimulationManager(collector)
          → model.setCollector(collector)
      → SimPathsModel.buildObjects()
      → SimPathsModel.buildSchedule()
      → SimPathsCollector.buildObjects()
      → SimPathsCollector.buildSchedule()
  → engine.startSimulation()
  → engine.quit()
```

#### 1.2 Batch mode — SimPathsMultiRun

```java
// src/main/java/simpaths/experiment/SimPathsMultiRun.java
public class SimPathsMultiRun extends MultiRun
```

Key static fields:

```java
private static Integer popSize = 25000;
private static int endYear = 2020;
private static int maxNumberOfRuns = 25;
private static Long randomSeed = 615L;
public static String configFile = "default.yml";
private static Map<String, Object> modelArgs;
private static Map<String, Object> innovationArgs;
private static Map<String, Object> collectorArgs;
private static Map<String, Object> parameterArgs;
```

main() sequence (lines 73–166):

1. `parseYamlConfig(args)` reads `config/default.yml` via SnakeYAML.
2. `updateParameters(parameterArgs)` applies `parameter_args` via reflection to `Parameters` statics.
3. It reads country/year from `DatabaseCountryYearFilename.xlsx`.
4. `updateLocalParameters(innovationArgs)`.
5. `parseYamlConfig(args)` is called a second time (line 106).
6. `parseCommandLineArgs(args)` applies CLI overrides on top of YAML values.
7. If `-DBSetup` is set, it calls `Parameters.databaseSetup(...)` and exits.
8. Otherwise, it runs `engine.setup()` and then `experimentBuilder.start()`.

Additional CLI flags (beyond single-run):

| **Flag** | **Effect** |
| --- | --- |
| -p int / --popSize | Sets popSize |
| -e year / --endYear | Sets endYear |
| -n int / --maxNumberOfRuns | Sets maxNumberOfRuns |
| -r int / --randomSeed | Sets randomSeed (Long) |
| -config file | YAML file path under config/ |
| -f | Redirects stdout/log4j to output/logs/run_[seed].txt |
| -P root\|run\|none / --persist | Processed-DB persistence mode |
| -DBSetup | Run DB setup only, then exit |

buildExperiment() (lines 532–550):

```java
@Override
public void buildExperiment(SimulationEngine engine) {
    SimPathsModel model = new SimPathsModel(Country.getCountryFromNameString(countryString), startYear);
    if (persist_population) model.setPersistPopulation(true);
    if (persist_root) model.setPersistDatabasePath(Parameters.getInputDirectory() + "input");
    updateLocalParameters(model);
    if (modelArgs != null) updateParameters(model, modelArgs);  // model_args via reflection
    engine.addSimulationManager(model);
    SimPathsCollector collector = new SimPathsCollector(model);
    if (collectorArgs != null) updateParameters(collector, collectorArgs);
    engine.addSimulationManager(collector);
    model.setCollector(collector);
    // NOTE: SimPathsObserver is NOT registered in batch mode
}
```

nextModel() (lines 582–591): increments counter, calls iterateParameters(counter) to bump randomSeed++, returns true while counter < maxNumberOfRuns.

### 2. JAS-mine Engine Integration

#### 2.1 Class hierarchy

| **SimPaths class** | **JAS-mine superclass** | **Additional interface** |
| --- | --- | --- |
| SimPathsModel | AbstractSimulationManager | EventListener |
| SimPathsCollector | AbstractSimulationCollectorManager | EventListener |
| SimPathsObserver | AbstractSimulationObserverManager | EventListener, ILongSource |

#### 2.2 Engine lifecycle

When engine.setup() is called, JAS-mine:

1. Calls `buildExperiment(engine)` on the registered `ExperimentBuilder`.
2. Calls `buildObjects()` on every registered `SimulationManager` in registration order.
3. Calls `buildSchedule()` on every registered `SimulationManager`.
4. Prepares the event queue for `startSimulation()`.

#### 2.3 SimPathsModel.buildObjects() — full sequence (lines 363–469)

```java
 1.  Seed RNG (if fixRandomSeed): SimulationEngine.getRnd().setSeed(randomSeedIfFixed)
 2.  Init per-module Random instances: cohabitInnov, initialiseInnov1/2, educationInnov, popAlignInnov
 3.  Parameters.loadParameters(country, maxAge, ...)   [loads all coefficient maps from Excel]
 4.  if lifetimeIncomeGenerate: ManagerProjectLifetimeIncomes.run(...)
 5.  if enableIntertemporalOptimisations: DecisionParams.loadParameters(...)
 6.  RunDatabasePath = DatabaseUtils.databaseInputUrl
 7.  populateTaxdbReferences()
 8.  year = startYear
 9.  createInitialPopulationDataStructures()
         → inputDatabaseInteraction()        [JDBC: create PERSON/BENEFITUNIT/HOUSEHOLD tables]
         → loadStartingPopulation()          [JPA/Hibernate: build object graph]
10.  createDataStructuresForMarriageMatching()
11.  labourMarket = new LabourMarket(benefitUnits)
12.  if !initialisePotentialEarningsFromDatabase:
         initialisePotentialEarningsByWageEquationAndEmployerSocialInsurance()
13.  scalingFactor = populationProjectionTotal / persons.size()
14.  tests = new Tests()
15.  saveRunParameters()
```

#### 2.4 SimPathsModel.buildSchedule() — all registered events in order

The schedule uses two event groups:

```java
  firstYearSched  — scheduled once at startYear
  yearlySchedule  — scheduled to repeat from startYear+1 at interval 1.0
```

Events registered into BOTH years (firstYearSched + yearlySchedule):

| **#** | **Class.method()** | **Description** |
| --- | --- | --- |
| 1 | SimPathsModel.onEvent(StartYear) | Year-start initialisation |
| 2 | SimPathsModel.onEvent(UpdateParameters) | Update time-varying parameters |
| 3 | SimPathsModel.onEvent(GarbageCollection) | Memory management |
| 4 | persons → Person.Processes.Update | Per-person state update |
| 5 | benefitUnits → BenefitUnit.Processes.Update | Per-BU state update |
| 6 | persons → Person.Processes.ConsiderMortality | Mortality |
| 7 | SimPathsModel.onEvent(PopulationAlignment) | Align age/sex/region to projections |
| 8 | persons → Person.Processes.HealthEQ5D | EQ-5D utility score |
| 9 | persons → Person.Processes.UpdateOutputVariables | Update idPartner, dhhtp_c4 |
| 10 | benefitUnits → BenefitUnit.Processes.UpdateOutputVariables | Update dhhtp_c4 |
| 11 | SimPathsModel.onEvent(EndYear) | Year-end processing |
| 12 | SimPathsModel.onEvent(UpdateYear) | Increment year counter |
| 13 | tests → Tests.Processes.RunTests | Diagnostic tests |

Events registered into SUBSEQUENT years only (yearlySchedule):

| **#** | **Class.method()** | **Notes** |
| --- | --- | --- |
| 1 | persons → Person.Processes.Aging |  |
| 2 | persons → Person.Processes.ConsiderRetirement |  |
| 3 | persons → Person.Processes.InSchool |  |
| 4 | SimPathsModel.onEvent(InSchoolAlignment) |  |
| 5 | persons → Person.Processes.LeavingSchool |  |
| 6 | SimPathsModel.onEvent(EducationLevelAlignment) |  |
| 7 | benefitUnits → BenefitUnit.Processes.Homeownership |  |
| 8 | persons → Person.Processes.Health | GenOrderedLogit H1 + Probit H2 |
| 9 | persons → Person.Processes.UpdatePotentialHourlyEarnings | Wage equation |
| 10 | SimPathsModel.onEvent(CohabitationAlignment) |  |
| 11 | persons → Person.Processes.Cohabitation |  |
| 12 | persons → Person.Processes.PartnershipDissolution |  |
| 13 | SimPathsModel.onEvent(UnionMatching) |  |
| 14 | SimPathsModel.onEvent(FertilityAlignment) |  |
| 15 | persons → Person.Processes.Fertility | Probit F1 |
| 16 | persons → Person.Processes.GiveBirth | readOnly=false |
| 17 | persons → Person.Processes.SocialCareReceipt | if projectSocialCare |
| 18 | persons → Person.Processes.SocialCareProvision | if projectSocialCare |
| 19 | persons → Person.Processes.Unemployment | Probit U1a–U1d |
| 20 | benefitUnits → BenefitUnit.Processes.UpdateStates | if IO, readOnly=false |
| 21 | SimPathsModel.onEvent(LabourMarketAndIncomeUpdate) | Labour supply + income imputation |
| 22 | benefitUnits → BenefitUnit.Processes.ReceivesBenefits | EUROMOD donor matching |
| 23 | benefitUnits → BenefitUnit.Processes.ProjectDiscretionaryConsumption | if IO |
| 24 | persons → Person.Processes.ProjectEquivConsumption |  |
| 25 | benefitUnits → BenefitUnit.Processes.CalculateChangeInEDI |  |
| 26 | persons → Person.Processes.ReviseLifetimeIncome | if lifetimeIncomeImpute |
| 27 | persons → Person.Processes.FinancialDistress |  |
| 28 | persons → Person.Processes.HealthMentalHM1 | Mental health step 1 (Linear) |
| 29 | persons → Person.Processes.HealthMentalHM2 | Mental health step 2 (Linear, by gender) |
| 30 | persons → Person.Processes.HealthMentalHM1Case | GHQ caseness step 1 (OrderedLogit) |
| 31 | persons → Person.Processes.HealthMentalHM2Case | GHQ caseness step 2 (Linear, by gender) |
| 32 | persons → Person.Processes.HealthMCS1 | SF-12 MCS step 1 (Linear) |
| 33 | persons → Person.Processes.HealthPCS1 | SF-12 PCS step 1 (Linear) |
| 34 | persons → Person.Processes.LifeSatisfaction1 | Life satisfaction step 1 (Linear) |
| 35 | persons → Person.Processes.HealthMCS2 | SF-12 MCS step 2 (Linear, by gender) |
| 36 | persons → Person.Processes.HealthPCS2 | SF-12 PCS step 2 (Linear, by gender) |
| 37 | persons → Person.Processes.LifeSatisfaction2 | Life satisfaction step 2 (Linear, by gender) |
| 38 | SimPathsModel.onEvent(CheckForImperfectTaxDBMatches) | Log donor-match quality |

End-of-simulation scheduling (lines 630–637):

```java
getEngine().getEventQueue().scheduleOnce(firstYearSched, startYear, ordering);
getEngine().getEventQueue().scheduleRepeat(yearlySchedule, startYear+1, ordering, 1.);
getEngine().getEventQueue().scheduleOnce(
    new SingleTargetEvent(this, Processes.CleanUp), endYear+1, orderEarlier);
SystemEvent end = new SystemEvent(SimulationEngine.getInstance(), SystemEventType.End);
getEngine().getEventQueue().scheduleOnce(end, endYear+1, orderEarlier);
```

### 3. Main Model Class and Object Graph

#### 3.1 Agent hierarchy

```java
Household (1) ──@OneToMany──► Set<BenefitUnit> (many)
                                    │
                              @OneToMany──► Set<Person> members (many)
Person ──@ManyToOne──► BenefitUnit ──@ManyToOne──► Household
```

All three are @Entity classes mapped to H2 tables. Relationships are bidirectional JPA associations using PanelEntityKey composite PKs (fields: id, simulation_time, simulation_run, working_id).

#### 3.2 Person — key declarations

```java
@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.REFRESH)
@JoinColumns({
    @JoinColumn(name = "buid",   referencedColumnName = "id"),
    @JoinColumn(name = "butime", referencedColumnName = "simulation_time"),
    @JoinColumn(name = "burun",  referencedColumnName = "simulation_run"),
    @JoinColumn(name = "prid",   referencedColumnName = "working_id")
})
private BenefitUnit benefitUnit;

private int demAge;
private Gender demMaleFlag;
private Education eduHighestC4;           // InEducation/High/Medium/Low
private Les_c4 labC4;                     // EmployedOrSelfEmployed/NotEmployed/Student/Retired
@Column(name = "HOURS_WORKED_WEEKLY") private Integer labHrsWorkWeek;
private Integer labHrsWorkWeekL1;         // lagged copy
private Labour labHrsWorkEnumWeek;        // discrete enum
private Dhe healthSelfRated;              // Poor/Fair/Good/VeryGood/Excellent
private Indicator healthDsblLongtermFlag;
private Double labWageFullTimeHrly;
private Double wgt;                       // survey weight
```

#### 3.3 BenefitUnit — key declarations

```java
@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.REFRESH)
@JoinColumns({...})
private Household household;

@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "benefitUnit")
@Fetch(org.hibernate.annotations.FetchMode.SUBSELECT)
private Set<Person> members = new LinkedHashSet<>();

private Region region;
private Double yDispMonth;            // monthly disposable income
private Double yGrossMonth;
private Ydses_c5 yHhQuintilesMonthC5; // income quintile 1–5
private Long idtaxDbDonor;            // matched EUROMOD donor ID
```

#### 3.4 Household — key declarations

```java
@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "household")
@OrderBy("key ASC")
@Fetch(FetchMode.SUBSELECT)
private Set<BenefitUnit> benefitUnits = new LinkedHashSet<>();
```

Note from Javadoc: "Household class is a 'wrapper' bundling multiple benefitUnits into one household. Currently it is used to keep track of adult children who create separate benefitUnits in the simulation for technical reasons, but still live with their parents in 'reality'."

### 4. Database Initialisation and Population Loading

#### 4.1 JDBC connection strings

DataParser (setup phase):

```java
// DataParser.databaseFromCSV(), line 447
conn = DriverManager.getConnection(
    "jdbc:h2:file:" + Parameters.getInputDirectory() + "input"
    + ";TRACE_LEVEL_FILE=0;TRACE_LEVEL_SYSTEM_OUT=0;AUTO_SERVER=TRUE",
    "sa", "");
```

SimPathsModel — runtime JDBC (inputDatabaseInteraction):

```java
// line 2544
conn = DriverManager.getConnection(
    "jdbc:h2:" + RunDatabasePath + ";TRACE_LEVEL_FILE=0;TRACE_LEVEL_SYSTEM_OUT=0;AUTO_SERVER=TRUE",
    "sa", "");
```

JPA/Hibernate (loadStartingPopulation):

```java
// lines 3691–3692
propertyMap.put("hibernate.connection.url",
    "jdbc:h2:file:" + RunDatabasePath + ";...");
EntityManager em =
    Persistence.createEntityManagerFactory("starting-population", propertyMap)
               .createEntityManager();
```

The physical file is input/input.mv.db (H2 MVStore). The URL omits .mv.db per H2 convention.

#### 4.2 Setup-phase table creation (DataParser)

databaseFromCSV() → createDatabaseForPopulationInitialisationByYearFromCSV() → parse(csvFilePath, tableName, conn, country, year) for each year.

In parse(), the raw CSV is ingested via H2's built-in CSVREAD:

```java
CREATE TABLE {inputFileName} AS SELECT * FROM CSVREAD('{inputFileLocation}')
```

Then a chain of ALTER TABLE / INSERT INTO SELECT statements produces three tables per (country, year): person_{c}_{y}, benefitUnit_{c}_{y}, household_{c}_{y}.

#### 4.3 Runtime-phase table creation (inputDatabaseInteraction)

```java
stat.execute("DROP TABLE IF EXISTS PERSON CASCADE");
stat.execute("CREATE TABLE PERSON AS SELECT * FROM PERSON_" + country + "_" + year);
// same for BENEFITUNIT, HOUSEHOLD

String query2 = "SELECT ID, " + Parameters.HOURS_WORKED_WEEKLY + " FROM PERSON";
ResultSet rs2 = stat.executeQuery(query2);
while (rs2.next()) {
    initialHoursWorkedWeekly.put(rs2.getLong("ID"), rs2.getDouble(Parameters.HOURS_WORKED_WEEKLY));
}
```

This pre-populates SimPathsModel.initialHoursWorkedWeekly (LinkedHashMap<Long, Double>) before JPA loads the object graph.

#### 4.4 JPA object-graph loading (loadStartingPopulation)

```java
String query = "SELECT households FROM Household households";
households = em.createQuery(query).getResultList();
```

Hibernate lazily resolves Set<BenefitUnit> and Set<Person> members using SUBSELECT strategy (avoids N+1 queries). The objects are then stored in SimPathsModel.households, .benefitUnits, .persons.

#### 4.5 Loading order and why it matters

1. `DataParser` must run first to create year-specific tables from CSV.
2. `inputDatabaseInteraction()` runs before JPA to create unified `PERSON`/`BENEFITUNIT`/`HOUSEHOLD` views.
3. JPA/Hibernate builds the object graph last, relying on FK columns (`buid`, `hhid`) added by `DataParser`.

### 5. Variable Mapping: HOURS_WORKED_WEEKLY as a Worked Example

#### 5.1 End-to-end trace

Constant — Parameters.java (line 372):

```java
public static final String HOURS_WORKED_WEEKLY = "HOURS_WORKED_WEEKLY";
```

Input CSV column name: labHrsWorkWeek

Rename in DataParser.parse() (line 238):

```java
+ "ALTER TABLE " + personTable
+ " ALTER COLUMN labHrsWorkWeek RENAME TO " + Parameters.HOURS_WORKED_WEEKLY + ";";
```

JPA field on Person.java (line 150):

```java
@Column(name = "HOURS_WORKED_WEEKLY") private Integer labHrsWorkWeek;
private Integer labHrsWorkWeekL1;   // lagged copy, updated each year-start
```

Pre-JPA read (runtime JDBC):

```java
String query2 = "SELECT ID, " + Parameters.HOURS_WORKED_WEEKLY + " FROM PERSON";
ResultSet rs2 = stat.executeQuery(query2);
while (rs2.next()) {
    initialHoursWorkedWeekly.put(rs2.getLong("ID"), rs2.getDouble(Parameters.HOURS_WORKED_WEEKLY));
}
```

Runtime usage: labHrsWorkWeek is accessed via Person.DoublesVariables enum entries inside regression evaluation. labHrsWorkWeekL1 holds the prior year's value for lagged regressors.

#### 5.2 General pattern for Person variable mapping

| **Stage** | **What happens** |
| --- | --- |
| Input CSV | Column in original survey notation (e.g. labHrsWorkWeek, les_c4, dhe) |
| DataParser.parse() | Column renamed and/or value-transformed (int codes → enum strings, 0/1 → False/True) |
| H2 DB column | Upper-case name used as @Column(name=...) annotation target |
| JPA field | Java camelCase field on Person, annotated @Column(name="...") |
| Lag field | *L1 counterpart updated at year-start by Person.Processes.Update |
| Regression covariate | Person.DoublesVariables enum wraps field access; JAS-mine calls getDoubleValue(var) |
| Output | @Column-annotated fields exported to Person.csv by DataExport |

Specific transforms applied by DataParser.parse():

```java
labC4 (int 1–4)           → VARCHAR (EmployedOrSelfEmployed, Student, NotEmployed, Retired)
healthSelfRated/dhe (1–5) → VARCHAR (Poor, Fair, Good, VeryGood, Excellent)
eduHighestC4 (int 0–3)    → VARCHAR (InEducation, High, Medium, Low)
demMaleFlag (0/1)         → VARCHAR (Female, Male)
Indicator fields (0/1)    → VARCHAR (False, True)
FK columns added:         buid/butime/burun/prid (person→BenefitUnit)
                          hhid/hhtime/hhrun/prid (benefitUnit→Household)
```

### 6. Validation

SimPaths uses a two-stage validation workflow in validation/. Stage 1 checks that each estimated regression model is well-specified before simulation; stage 2 checks that full simulation output matches observed survey data. For the conceptual overview and detailed setup instructions, see Model Validation on the website.

**Stage 1 — Estimate validation (validation/01_estimate_validation/)**

**When to run:** After updating or re-estimating any regression module (i.e. after re-running scripts in input/InitialPopulations/compile/RegressionEstimates/).

**What it does:** For each behavioural module, the script loads the estimation sample, computes predicted values from the estimated coefficients, adds individual heterogeneity via 20 stochastic draws (as in multiple imputation), and overlays the predicted and observed distributions as histograms.

| **Script** | **Module validated** |
| --- | --- |
| int_val_wages.do | Hourly wages — Heckman selection model, separately for males/females with and without previous wage history |
| int_val_education.do | Education transitions (3 processes) |
| int_val_fertility.do | Fertility (2 processes) |
| int_val_health.do | Physical health transitions |
| int_val_home_ownership.do | Homeownership transitions |
| int_val_income.do | Income processes — hurdle models (selection and amount) |
| int_val_leave_parental_home.do | Leaving parental home |
| int_val_partnership.do | Partnership formation and dissolution |
| int_val_retirement.do | Retirement transitions |

**Outputs:** PNG graphs saved under validation/01_estimate_validation/graphs/<module>/. Each graph shows predicted (red) vs observed (black outline) distributions.

**Stage 2 — Simulated output validation (validation/02_simulated_output_validation/)**

**When to run:** After completing a baseline simulation run that you want to assess for plausibility.

**What it does:** Loads your simulation output CSVs, loads UKHLS initial population data as an observational benchmark, and produces side-by-side time-series plots comparing 18 simulated outcomes against the observed distributions with confidence intervals.

**Comparison plots (18 scripts, 06_01 through 06_18):**

| **Script** | **What is compared** |
| --- | --- |
| 06_01_plot_activity_status.do | Economic activity: employed, student, inactive, retired by age group |
| 06_02_plot_education_level.do | Completed education distribution over time |
| 06_03_plot_gross_income.do | Gross benefit-unit income |
| 06_04_plot_gross_labour_income.do | Gross labour income |
| 06_05_plot_capital_income.do | Capital income (interest, dividends) |
| 06_06_plot_pension_income.do | Pension income |
| 06_07_plot_disposable_income.do | Disposable income after taxes and benefits |
| 06_08_plot_equivalised_disposable_income.do | Household-size-adjusted disposable income |
| 06_09_plot_hourly_wages.do | Hourly wages for employees |
| 06_10_plot_hours_worked.do | Weekly hours worked by employment status |
| 06_11_plot_income_shares.do | Income distribution across quintiles |
| 06_12_plot_partnership_status.do | Partnership status (single, married, cohabiting, previously partnered) |
| 06_13_plot_health.do | Physical and mental health (SF-12 PCS and MCS) |
| 06_14_plot_at_risk_of_poverty.do | At-risk-of-poverty rate |
| 06_15_plot_inequality.do | Income inequality (p90/p50 ratio) |
| 06_16_plot_number_children.do | Number of dependent children |
| 06_17_plot_disability.do | Disability prevalence |
| 06_18_plot_social_care.do | Social care receipt |

**Outputs:** PNG graphs saved under validation/02_simulated_output_validation/graphs/<run-id>/. A reference set from a baseline run (20250909_run) is already committed for comparison.

**Interpreting results**

- **Stage 1:** Predicted and observed histograms should broadly overlap. Systematic divergence indicates a problem with the estimation or variable construction.
- **Stage 2:** Simulated time-series should track UKHLS trends within reasonable uncertainty bounds. Large divergence in levels suggests a miscalibration; divergence in trends suggests a missing time-series process or a misspecified time-trend parameter.

The validation suite does not produce a single pass/fail metric — it is a diagnostic tool to inform judgement about whether a given parameterisation is fit for the intended research purpose.
