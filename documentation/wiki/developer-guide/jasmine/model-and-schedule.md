# The Model and the Schedule

Note: This Section refers to a generic JAS-mine project

# 1.The SimulationManager interface

All JAS-mine Models should either implement the *SimulationManager* interface or extend the *AbstractSimulationManager* class. 
Two methods are required in a Model class.

* The ***buildObjects*()** method should contain the instructions to create all the agents and the objects that represent the virtual environment for model execution.
* The ***buildSchedule*()** method should contain the planning of the events for the simulation.

Collectors and Observers follow a similar logic, the only difference being that Collectors must implement the *CollectorManager* interface, and Observers must implement the *ObserverManager* interface. The Demo07 model describes those classes in details.

In the following example the first instruction loads a list of agents, instances of the Agent class, from an input database table using the JAS-mine integrated ORM system:

```java
public void buildObjects() {   
  
    agentsList = (List<Agent>) DatabaseUtils.loadTable(Agent.class);   
}
```

Events are planned based on a discrete event simulation paradigm. This means that events can be scheduled dynamically at specific points in time. The frequency of repetition of an event can be specified in case of recurring events characterized by a specific frequency. An event can be created for a specific recipient. In particular, an event can be created and managed by the simulation engine (a system event, e.g. simulation stops), it can be sent to all the components of a collection or list of agents or it can be sent to a specific object/instance. Events can be grouped together if they share the same schedule.

```java
public void buildSchedule() {   
  
    EventGroup s = new EventGroup();   
    s.addCollectionEvent(agentsList, Agent.Processes.Age);   
    s.addEvent(this, Processes.MarriageMatch);   
    getEngine().getEventList().scheduleRepeat(s, 0.0, 0, 1.0);   
    getEngine().getEventList().scheduleOnce(new SingleTargetEvent(this, Processes.Stop), 2.0, Order.AFTER_ALL.getOrdering());   
  
}
```

In the example above a group of events (labeled *s*) is created to be run for the first time at time 0.0 and then repeated at each interval of 1.0 time units. Within an event group, events are run sequentially, as specified in the code. The first event in the event group *s* (*Age*) is sent to all the individuals in the simulated population and entails aging. The second event is targeted to the model itself and entails running the *MarriageMatch*() method, which forms couples based on a some matching algorithm. The event group *s* is added to the model's schedule in repeat mode using the *scheduleRepeat*() method. Finally, the end of the simulation is scheduled once using the *scheduleOnce*() method at time t=2.0 and is notified to the model itself.

# 2. The Ordering of Events

The signature of the *scheduleOnce*() method is:

**scheduleOnce(Event event, double atTime, int withOrdering),**

whilst the signature of the scheduleRepeat() method is:

**scheduleRepeat(Event event, double atTime, int withOrdering, double timeBetweenEvents).**

Note the use of the '*withOrdering*' integer field. This is used to specify the order in which events scheduled at the same time are fired; events with lower values of the *withOrdering* field will fire first. For example, if two events are scheduled to occur at time 10.52, then if event A was scheduled with the *withOrdering* field set to 0 whereas event B was scheduled with *withOrdering* set to -1, event B will fire before event A. If two events are scheduled with both the same *atTime* and *withOrdering*, the event that was added to the schedule earlier in the simulation will be fired first. It is therefore important that the ordering of events scheduled for the same time only share the same value of the *withOrdering* field if it doesn't matter what order the events need to be fired in.

There are two standard ordering values:

* Order.BEFORE_ALL.getOrdering() which should be reserved to schedule events that need to be fired before all other events scheduled at the same time.
* Order.AFTER_ALL.getOrdering() which should be reserved to schedule events that need to be fired after all other events scheduled at the same time. This ordering value is used in the example above, where it specifies that the Processes.Stop event only be fired after all events scheduled for the same time. Note, that it might be desirable to record the data of the simulation just before stopping the Processes.Stop event, which could be achieved by scheduling the collector to dump the persisted data to the database at the same time as the Processes.Stop, but with the *withOrdering* field of the event set as Order.AFTER_ALL.getOrdering() **– 1**, so that it is fired just before the Processes.Stop event.

# 3. The EventListener interface

A class can receive and process events after implementing the *EventListener* interface and defining the onEvent method that will receive specific enumerations to be interpreted.

In the example, the model defines an enum called *Processes* as follows:

```java
public enum Processes {   
  
    MarriageMatching,   
     Stop;   
  
}
```

The *onEvent*() method decodes this object and performs the required action:

```java
public void onEvent(Enum<?> type) {   
  
    switch ((Processes) type) {   
  
        case MarriageMatching:  
            […]   
            break;   
        case Stop:   
            getEngine().pause();   
            break;   
  
        }   
} 
```

Analogously, the Agent class also defines an enum called *Processes*, which in this example contains the *Age* case.


# 4. Dynamic Scheduling

Note that events can be scheduled dynamically and need not be planned in advance when constructing the model. For instance, events can be added by the agents themselves, based on their behavioural rules. This simply requires accessing the event list through a singleton instance of the simulation engine, with the following instruction:

```java
SimulationEngine.instance.getEventList();
```

</details>
