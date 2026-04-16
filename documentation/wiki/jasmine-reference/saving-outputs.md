# Saving Outputs

# 1. Persistence

Persistence is achieved by updating a table in the relational database corresponding to a class in the simulation model. Classes to be persisted must be annotated as `@Entity` classes:

```java
@Entity
public class Person {
    […]
}

```

With such a simple operation, a table with the same name as the class is automatically added to the output, and the class is linked to this table. When the class is dumped by the Collector, all properties which are not annotated as `@Transient`, plus all the variables not defined as properties but implicitly defined by getters, are persisted in the database.

Note that `@Entity` classes **MUST** have an empty constructor. If a superclass is involved, call to the super class constructor should be like in the example below:

```java
protected Agent() {
    super(null);
}

```

# 2. Keys

To provide a unique identifier for the table entries, Entity classes must specify a `PanelEntityKey` (annotated as `@Id`), which is a three-dimensional object which identifies the agent id, the simulation time and the simulation run. These three keys uniquely identify each record:

```java
@Id
private PanelEntityKey key;

```

PanelEntityKeys **MUST be annotated as @Id**, though the `PanelEntityKey` object can be given any name, and it is suggested that they are not called *id* (*key* is a better name). This is because `PanelEntityKey` objects contain three objects:

* An **id** field of type `Long` representing the agent’s id.
* A **simulation_run** field storing the number of the simulation run (also of type `Long`), which is useful when running many simulations in sequence.
* A **simulation_time** field (of type `Double`).

The fields of the `PanelEntityKey` are thus used to uniquely identify panel entries in the database tables.

This is why to access the agent id the method `getId()` has to be invoked: if the `PanelEntitykey` is called *id*, this becomes `id.getId()`, which is confusing. If the agent id has to be accessed from other agents (for instance, to perform identity checks), the following `getAgentId()` method should be implemented:

```java
@Entity
public class Agent {
    @Transient
    private static long idCounter = 1000000;

    @Id
    private PanelEntityKey key = new PanelEntityKey(idCounter++);

    public Long getAgentId() {
        return key.getId();
    }
}

```

And then used as `agent.getAgentId()`.

With the JAS-mine persistence engine, (pointers to) objects cannot be persisted (this has to do with the fact that the `PanelEntityKey` is a multiple key). Therefore, the agent’s `PanelEntityKey` should be included as an additional variable and persisted, while the (pointer to the) object should be annotated as `@Transient`:

```java
@Entity
public class Application {

    @Id
    private PanelEntityKey key;

    @Transient
    private Worker worker;

    @Column(name="worker_id")
    private Long workerId;
}

```

The engine expects that the field names in the tables are the same as the property names in the Java class, except when a different name is specified as in:

```java
@Column(name="dur_in_couple")
private Integer durationInCouple;

```

Enumerations can be interpreted both as a string and as ordinal values (0 for the first enum, 1 for the second, etc.), depending on how they are annotated:

```java
@Enumerated(EnumType.STRING)
private WorkState workState;

```


# 3. The `DataExport` class

There are two ways of storing output data from the simulation runs, either to the database or to .csv files. Exporting to .csv is quicker than persisting to the database, so may be preferable when running simulations in time-constrained situations. 

In order to facilitate the exporting of data to .csv files and / or the database, a `DataExport` instance can be created in the `Collector` class for each object or collection of objects whose fields are to be recorded as in the code below. The choice of whether to export to the database and / or the .csv files can be controlled by two boolean fields `exportToDatabase` and `exportToCSV` respectively, which are passed to the constructor of the `DataExport` objects and can be set from the GUI (because they have the `@GUIparameter` annotation, which replaces the deprecated `@ModelParameter` annotation).

```java
@GUIparameter(description = "Toggle to export snapshot to .csv files")
boolean exportToCSV = true; //If true, data will be recorded to .csv files in the output directory

@GUIparameter(description = "Toggle to export snapshot to output database")
boolean exportToDatabase = true; //If true, data will be recorded in the output database in the // output directory

@GUIparameter(description = "Set the time at which to start exporting snaphots to the database and/or .csv files")
Double timeToStartDataExport = 0.;

@GUIparameter(description = "Set the time between snapshots to be exported to the database and/or .csv files")
Double timestepsBetweenDataDumps = 1.;

// collectionOfAgents is a Java Collection of agents e.g. an ArrayList, LinkedList or Set containing 
// instances of the Agent.class
DataExport populationOutput = new DataExport(collectionOfAgents, exportToDatabase, exportToCSV);

// agent is an instance of the Agent.class and has PanelEntityKey with id = 123
DataExport agentOutput = new DataExport(agent, exportToDatabase, exportToCSV);

```

# 4. Export to csv

When executed with the `exportToCSV` Boolean set to true, separate .csv files will be created corresponding to the `populationOutput` and `agentOutput` objects. The name of the .csv files will match the name of the class of object or entries of the collection of objects which were passed to the `DataExport` constructor. In the case above for instance, the `collectionOfAgents` is a Java Collection such as a list or set whose entries are of the `Agent` class, so the corresponding Agent.csv file will be created. On the other hand, the agent object is a single instance of the `Agent` class (not a collection), so a file named Agent123.csv will be created, with the suffix '123' matching the agent's id number in its `PanelEntityKey` instance.

When created, the .csv files will contain a first (header) line with the comma-separated names of all the fields of the underlying class to be recorded. These include numerical values, strings, Booleans and enum constants, and both private fields and those inherited from the superclass are recorded. References to objects, however, are not exported, although the internal fields of the PanelEntityKey associated with the object whose data is being recorded will be exported.

In order to export the objects' data to either the .csv files or the database, the `export()` method must be invoked on the `DataExport` instances. This can be placed in the event schedule, so that the objects' data can be recorded at regular times in the future thus providing a snapshot of the simulation run, or be invoked at any time and by any object in the simulation:

```java
public void buildSchedule() {
    // Dump info from year 'timeToStartDataExport' onwards, with 'timestepsBetweenDataDumps' 
    // specifying the period between data dumps thereafter
    getEngine().getEventList().scheduleRepeat(new SingleTargetEvent(this, Processes.DumpInfo), timeToStartDataExport, Order.AFTER_ALL.getOrdering(), timestepsBetweenDataDumps);

    // Dump data at the (scheduled) end of the simulation
    getEngine().getEventList().schedule(new SingleTargetEvent(this, Processes.DumpInfo), endYear(), Order.AFTER_ALL.getOrdering(), 0.);
}

////////////////////////////////////////////////////////////
// Event Listener
////////////////////////////////////////////////////////////
public enum Processes {
    DumpInfo,
}

public void onEvent(Enum<?> type) {
    switch ((Processes) type) {
        case DumpInfo:
            populationOutput.export();
            agentOutput.export();
            break;
    }
}

```

When the `exportToCSV` boolean is set to true, the `.export()` invocation will dump comma-separated data to the .csv files. Again, the data included is either numerical, strings, Booleans or enum constants, and includes private and inherited fields belonging to the object or it's superclasses. In the case of the Agent.csv, one line will be added for each of the agent instances contained in the `collectionOfAgents` object, with each line referenced by values of the `PanelEntityKey`:- the simulation run number, the simulation time and the agent's id. In the case of Agent123.csv, a single line will be added containing the comma-separated data of the fields of the agent whose id is 123.

# 5. Export to database

When the `exportToDatabase` Boolean is set to true, the `DatabaseUtils.snap()` method will be invoked in the Collector, and JAS-mine's database functionality will kick in to export the data to the appropriate tables in the output database:

```java
DatabaseUtils.snap( ( (PersonsModel) getManager()).getPersons() );

```