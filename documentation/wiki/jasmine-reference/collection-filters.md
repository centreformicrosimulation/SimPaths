# Collection Filters

Filters select the agents that a process or statistic should consider. SimPaths uses Java collections and streams alongside reusable filters in [simpaths.data.filters](https://github.com/simpaths/SimPaths/blob/b223738b9cdf1d814cc3c6f09b04bc4930d3c667/src/main/java/simpaths/data/filters).

## Filtering a SimPaths population

For a `SimPathsModel model`, this example selects people aged 65 or over from the current population:

```java
List<Person> olderPeople = model.getPersons().stream()
    .filter(person -> person.getDemAge() >= 65)
    .collect(Collectors.toList());
```

The example requires imports for `Person`, `List` and `Collectors`. Age 65 is an example selection criterion, not a universal threshold for SimPaths processes. Use the eligibility rule defined by the relevant module.

The list contains references to the selected agents, not copies. Changing an agent through the list changes the same object held by the model. Membership, however, is fixed when the stream is collected.

## Keeping filters current

A list built at startup does not automatically gain people when they become eligible, or lose them when they leave the population. Recompute a selection when it is needed, or update the existing collection before the scheduled process that uses it.

This distinction matters when scheduling collection events: a scheduled event may retain the collection object it was given. Reassigning a Java variable to a new list does not necessarily update that event's target.

Before adding a filter, check:

1. Which agent level the process uses: person, benefit unit or household.
2. Whether eligibility depends on current or lagged state.
3. How missing values and sample exits are handled.
4. Whether the statistic needs survey weights as well as selection.
5. When the selection is refreshed relative to the process and output schedule.

See [The Model and the Schedule](../developer-guide/jasmine/model-and-schedule.md) and [Statistical Package](statistical-package.md) for the surrounding lifecycle.
