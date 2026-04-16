# Collection Filters

The most basic way to filter a collection (say, a list of workers) is by iterating over its elements and check whether the filtering condition (say, whether they are employed) applies:
```java
List<Worker> employedWorkerList = new ArrayList<Worker>(); 
for (Worker w : workerList)
    if ( w.getEmployed() ) employedWorkerList.add(w);
```

Collections can also be filtered without the iteration, by using the Apache [CollectionUtils](https://commons.apache.org/proper/commons-collections/javadocs/api-3.2.1/index.html?org/apache/commons/collections/CollectionUtils.html) and [Predicate](https://commons.apache.org/proper/commons-collections/javadocs/api-3.2.1/index.html?org/apache/commons/collections/Predicate.html) libraries:
```java
import org.apache.commons.collections.CollectionUtils;
```
```java
import org.apache.commons.collections.Predicate;
```

and applying a JAS-mine filtering class implementing the `ICollectionFilter` interface, as follows:
```java
List<Worker> employedWorkerList = new ArrayList<Worker>(); 
CollectionUtils.select(
    workerList, filter, employedWorkerList
);
```

where the filter is implemented with the following *Closure:*
```java
new Predicate() {
    public boolean evaluate(Object obj) {
        Worker w = (Worker) obj;
        return (w.getEmployed());
    }
}
```

**RMK:** At present, it is not possible to schedule an event for a filtered collection with an automatic evaluation of the filter at each scheduled time. That is,
```java
EventGroup eventGroup = new EventGroup();
eventGroup.addCollectionEvent(CollectionUtils.select(workerList, new Predicate(){...}), 
    Agent.Process.DoSomething);
```

filters the list at t=0 -when the schedule is built- based on the characteristics of the objects in the list at t=0. If the filter has to be reevaluated at each time the call for the event is broadcasted, this must be done as a separate process, as with
```java
eventGroup.addEvent(this, Processes.UpdateEmployedWorkerList);
eventGroup.addCollectionEvent(employedWorkerList, Agent.Processes.DoSomething);
```

Starting with Java 8 (which requires Eclipse version Luna or later), it is possible to simplify further by using a Stream. A Stream is a data structure that is computed on-demand. A Stream doesn't store data, it operates on the source data structure (collection and array) and produce pipelined data that we can use and perform specific operations. As such, we can create a Stream from the list and filter it based on a condition:
```java
List<Worker> employedWorkerList = workerList.stream().filter(
    w -> w.getEmployed()).collect(Collectors.toList()
);
```