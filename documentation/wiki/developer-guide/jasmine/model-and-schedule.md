# The Model and the Schedule

JAS-mine managers prepare objects and schedule events. In SimPaths, the central coordinator is [SimPathsModel](https://github.com/simpaths/SimPaths/blob/b223738b9cdf1d814cc3c6f09b04bc4930d3c667/src/main/java/simpaths/model/SimPathsModel.java).

## The manager lifecycle {#1the-simulationmanager-interface}

`buildObjects()` prepares the initial population, parameters and supporting state. `buildSchedule()` specifies when processes run. The collector and observer have their own object-building and scheduling methods.

This separation matters: a setting used only when objects are built will not necessarily take effect if changed during a run.

## Event ordering {#2-the-ordering-of-events}

The event queue supports one-off and repeating events. Within an `EventGroup`, events are added in execution order. Events scheduled at the same simulation time also have an ordering value.

SimPaths schedules a first-year group separately from the later yearly sequence. The relevant source contains:

```java
getEngine().getEventQueue().scheduleOnce(
    firstYearSched, startYear, ordering);
getEngine().getEventQueue().scheduleRepeat(
    yearlySchedule, startYear + 1, ordering, 1.);
```

This is an excerpt, not a complete schedule. Do not replace it with a generic demo's annual loop: initial-year treatment, lag updates, alignment, behavioural processes and output timing have model-specific dependencies.

Before changing the order, consult `buildSchedule()` and [SimPathsUK_Schedule.xlsx](https://github.com/simpaths/SimPaths/blob/b223738b9cdf1d814cc3c6f09b04bc4930d3c667/documentation/SimPathsUK_Schedule.xlsx). Where a workbook and code differ, investigate the difference rather than silently choosing the more convenient version.

## Event dispatch {#3-the-eventlistener-interface}

Events identify the process to execute. An agent's or manager's `onEvent()` dispatches that process to the corresponding method. For example, the health event is handled in `Person`, while aggregate alignment and matching are coordinated by the model.

Trace both the scheduled event and the dispatched implementation. Finding a method in an agent class does not establish that it is called in every configuration.

## Extending a schedule {#4-dynamic-scheduling}

1. Identify the state the new process reads and writes.
2. Place it after its prerequisites and before processes that use its results.
3. Check whether it belongs in the first-year schedule, later years, or both.
4. Update the relevant lag handling and schedule documentation.
5. Test output timing as well as the process outcome, including the final simulation year.

JAS-mine also supports dynamically added events, but standard SimPaths process order should remain coordinated in `SimPathsModel.buildSchedule()`. See [The SimPathsModel Class](../internals/simpaths-model.md) for further implementation detail.
