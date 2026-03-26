# The SimPathsModel Class

This page complements **[4.05 - The Model and the Schedule]** by explaining how SimPaths instantiates the generic JAS-mine scheduling framework in practice. It documents current SimPaths practice, not theory or design justification. It is not a complete specification of the model's behaviour, and should be read together with the codebase and validation documentation.

## 1. The SimPaths model manager

The core simulation logic in SimPaths is implemented in a model manager class that extends _AbstractSimulationManager_ and implements EventListener.

The SimPaths model class functions primarily as a coordinator of simulation structure. Its primary responsibilities are to initialise the population and global data structures, define the temporal structure of the simulation via schedules, and respond to aggregate-level events.

### Model construction

The **buildObjects()** method defines the initial simulation state before any time evolution takes place.

In SimPaths, this phase typically includes:

- Initialising random number generators, including the creation of separate random streams for different modules (e.g. matching, alignment, initialisation), to reduce unintended coupling between processes.
- Loading global parameters and projections for the simulation horizon.
- Preparing auxiliary infrastructures, such as indices for tax-benefit donor data.
- Creating the initial population by loading or constructing `Person`, `BenefitUnit`, and `Household` objects, including any required population expansion or preprocessing.
- Initialising internal state variables, such as the simulation year counter.


### Simulation execution schedule:

The temporal structure of the simulation is defined in the **buildSchedule()** method. This method specifies the sequence of events that govern the evolution of the simulated population and determines how processes are executed over time. All model dynamics are implemented as events managed by the JAS-mine discrete-event simulation engine.

``` java
@Override
public void buildSchedule() {

    addEventToAllYears(Processes.StartYear);

    addEventToAllYears(Processes.UpdateParameters);
    addEventToAllYears(Processes.GarbageCollection);

    addCollectionEventToAllYears(benefitUnits, BenefitUnit.Processes.Update);
    addCollectionEventToAllYears(persons, Person.Processes.Update);

    // ...
}

```

The schedule distinguishes between two execution regimes. A first schedule is applied to the initial simulation year, when many individual attributes are inherited directly from the input data and only a subset of behavioural processes must be evaluated. A second schedule governs all subsequent years and is repeated at regular annual intervals until the end of the simulation horizon.

``` java
EventGroup firstYearSched = new EventGroup();
EventGroup yearlySchedule = new EventGroup();

// ...

getEngine().getEventQueue().scheduleOnce(firstYearSched, startYear, ordering);
getEngine().getEventQueue().scheduleRepeat(yearlySchedule, startYear+1, ordering, 1.);
``` 

Model processes are executed before data collection and observation components operating at the same simulation time. This ensures that monitoring and persistence routines operate on fully updated system states. The initial-year schedule runs once at the simulation start year, after which the standard yearly schedule repeats at fixed annual intervals.

``` java
int orderEarlier = -1;

getEngine().getEventQueue().scheduleOnce(
    new SingleTargetEvent(this, Processes.CleanUp), endYear+1, orderEarlier
);

SystemEvent end = new SystemEvent(SimulationEngine.getInstance(), SystemEventType.End);
getEngine().getEventQueue().scheduleOnce(end, endYear+1, orderEarlier);
``` 

In SimPaths, this mechanism is implemented through the JAS-mine EventListener interface.
The model defines an enumeration (Processes) that lists all model-level operations that can be triggered during simulation. When an event fires, the simulation engine delivers the corresponding enumeration value to the target object. The object’s onEvent() method interprets this identifier and invokes the associated process.

Scheduling, process identification, and process implementation are therefore separated. The event schedule determines when processes occur, the enumeration defines which processes exist, and the event handler executes them.

## 2. Dynamic event scheduling

Events in SimPaths may be defined during model construction or generated while the simulation is running. Processes can schedule additional events through the simulation engine when future actions must occur conditionally or at non-regular times.

All dynamically created events are inserted into the same global event list and are executed according to simulation time and event ordering rules. Once scheduled, dynamically generated events are treated identically to pre-scheduled events. This allows model evolution to depend on realised states rather than only on predetermined schedules.


