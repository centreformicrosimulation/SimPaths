# Developer Guide

SimPaths is a Java project based on the JAS-mine simulation libraries.

![JAS-mine logo](https://www.microsimulation.ac.uk/wp-content/uploads/2026/01/LOGO_NEW_TEXT.png)

JAS-mine extends Java functionalities and provide an architectural template for dynamic microsimulation and agent-based models, aimed at improving the clarity and transparency of the model structure.

New developers of SimPaths are strongly recommended to familiarise themselves with the JAS-mine architecture, and in particular:
* [JAS-mine GitHub repository](https://github.com/jasmineRepo)
* [JAS-mine core API](https://raw.githack.com/jasmineRepo/JAS-mine-core/master/microsim-core/doc/index.html)
* [JAS-mine GUI API](https://raw.githack.com/jasmineRepo/JAS-mine-gui/master/microsim-gui/doc/index.html)
* [JAS-mine documentation](https://www.microsimulation.ac.uk/jas-mine/)
* [JAS-mine reference paper](http://microsimulation.org/IJM/V10_1/IJM_2017_10_1_4.pdf)

***

# 1. Guiding principles
<details>
  <summary><b>Clarity</b></summary>  
A clear distinction is made in JAS-mine between objects with a modelling content, which specify the structure of the simulation, and objects which perform useful but auxiliary tasks, from enumerating categorical variables to building graphical widgets, from creating filters for the collection of agents to computing aggregate statistics to be saved in the output database.

JAS-mine extends the _Model-Observer_ paradigm introduced by the [Swarm](http://www.swarm.org/wiki/Main_Page) experience and introduces a new layer in simulation modelling, the _Collector_.

* The **Model** deals mainly with specification issues, creating objects, relations between objects, and defining the order of events that take place in the simulation.
* The **Collector **collects the data and compute the statistics both for use by the simulation objects and for post-mortem analysis of the model outcome, after the simulation has completed.
* The **Observer** allows the user to inspect the simulation in real time and monitor some pre-defined outcome variables as the simulation unfolds.
This three-layer methodological protocol allows for extensive re-use of code and facilitates model building, debugging and communication.

Moreover, JAS-mine envisages **strict separation between the code and the data**, with all parameters and input tables stored either in an input database or in specific MS Excel files. The regression package provides tools for simulating outcomes from standard regression models (OLS, probit/logit, multinomial, ordered and generalised ordered models): in particular, there is no need to specify the variables that enter a regression model, as they are directly read from the data files. This greatly facilitates exploration of the parameter space, testing different econometric specifications, and scenario analysis.
</details>

<details>
  <summary><b>Transparency</b></summary>  

Transparent coding for transparent modelling is achieved by

1. enforcing a strict adherence to the **open source** paradigm, which makes it less of a black-box with respect to proprietary software and encourages cooperative development of the platform by the community of users: all functions can be inspected and, if necessary, modified or extended.

2. allowing the user to choose from a wide range of classes and interfaces which **extend the standard Java language**, rather than providing an ad-hoc grammar and syntax. but  The JAS-mine libraries therefore provide open tools to “manufacture” a simulation model, making use whenever possible of solutions already available in the software development community (external functions can also be easily added as plug-ins). This also ensures a maximum amount of flexibility in model building. 
</details>

***

# 2. Architecture
SimPaths shares with all JAS-mine projects some architectural choices.

<details>
  <summary><b> Model-Collector-Observer</b></summary> 

The [Swarm protocol](https://www.swarm.org/wiki/Swarm_main_page) for agent-based platforms architecture recommends splitting the simulation into an internal *Model* and an external *Observer*. These two aspects of the artificial world should remain markedly separate.

The purpose of the *Observer* is to inspect the model's objects. Through the *Observer* the state of simulation can be monitored and graphically represented in real time, while the simulation is running. However, for the purpose of analysis and validation, the *Observer* alone may not be adequate, because it implies the need to define in advance the aggregations on which to analyze the simulation outcome. A variation in perspective requires re-running the experiment.

According to a different approach, the simulation is aimed exclusively at producing numerical outputs which can be analyzed in depth ex-post using ad-hoc statistical-econometric tools.

JAS-mine combines these two different approaches extending the *Model-Observer* paradigm so as to include an intermediate structure that calculates statistical values and persists simulation modelling outputs in the database in the most transparent way, minimizing the impact on model implementation. In the JAS-mine architecture agents are organized and managed by components called managers. There are three types of managers: *Model*, *Collector* and *Observer*.

* The **_Model_** deals mainly with specification issues, creating objects, relations between objects, and defining the order of events that take place in the simulation.
* The **_Collector_** collects the data and computes the statistics both for use by the simulation objects and for post-mortem analysis of the model outcome, after the simulation has completed.
* The **_Observer_** allows the user to inspect the simulation in real time and monitor some pre-defined outcome variables as the simulation unfolds.

This three-layer methodological protocol allows for extensive re-use of code and facilitates model-building, debugging and communication.

JAS-mine allows multiple *Models* (and multiple *Collectors* and *Observers*) to run simultaneously, since they share the same scheduler (known as a singleton). This allows for the creation of complex structures where agents of different *Models* can interact. Each *Model* is implemented in a separate Java class that creates the objects and plans the schedule of events for that *Model*. *Model* classes require the implementation of the `SimulationManager` interface, which implies the specification of a `buildObjects()` method to build objects and agents, and a `buildSchedule()` method for planning the simulation events. Analogously, *Collector* classes must implement the `CollectorManager` interface, and *Observer* classes must implement the `ObserverManager` interface.
</details>


<details>
  <summary><b> The JAS-mine engine</b></summary> 

The core of the JAS-mine toolkit is represented by the simulation engine. It is based on the standard discrete-event simulation paradigm, which allows to manage the time with high flexibility and multi-scale perspective.

The JAS-mine engine is based on the scheduler, which handles all the events in the simulation. The scheduler is a “singleton” (in software engineering, the singleton pattern is a design pattern that restricts the instantiation of a class to one object), which means that all the agents in the simulation share the same scheduler. Events can be scheduled in advance (for instance once every simulation period) or dynamically, by the agents themselves (for instance, job termination is scheduled upon hiring). This allows to implement both continuous-time and discrete-time simulations.

<details>
<summary><b>🔴 FOCUS: Time in simulation</b></summary>

The abstract representation of a continuous phenomenon in a simulation model requires that all events be presented in discrete terms.

With some confusion in the notation, **discrete-event** computer simulations can be cast either in **discrete time** or in **continuous time**.

With **discrete time**, time is broken into regular (equi-spaced) time slices (∆t) and the simulator calculates the variation of state variables for all the elements of the simulated model between one point in time and the next. Nothing is known about the order of the events that happen within each time period: discrete events (marriage, job loss, etc.) could have happened at any moment in ∆t while inherently continuous events (ageing, wealth accumulation, etc.) are best thought to progress linearly between one point in time and the next.

By contrast, simulations cast in **continuous time** are characterized by irregular timeframes that are punctuated by the occurrence of discrete events. Between consecutive events, no change in the system is assumed to occur; thus the simulation can directly jump in time from one event to the next. Inherently continuous events must be discretized.

The event list orders the events and the simulation is performed by extracting the event that is closest in time and submitting it to the model's agents, which change their state according to the signal (corresponding to the event) they have received. In the case of continuous simulations, the order of the processes that are applied must be exogenously assumed (and the assumption must be coherent with the specification of the model used for estimating the coefficients governing each process). The events may also be generated and scheduled not only in the initial planning phase but also while running the simulation.
</details>

</details>

<details>
  <summary><b> Input-Output communication</b></summary> 

Data management is a major factor to be weighed in for the creation of a simulation tool. Building on the vast number of software solutions available, JAS-mine allows the user to separate data representation and management from the implementation of processes and behavioral algorithms.

One distinguishing feature of the platform lies in the integration with relational database management systems (RDBMS) through ad-hoc Java libraries. The management of input data persistence layers and simulation results in JAS-mine is performed using standard database management tools, and the platform takes care of the automatic translation of the relational model of the database into the object-oriented simulation framework thanks to an ORM layer.

<details>
<summary><b>🔴 FOCUS: Object-Relational Mapping (ORM)</b></summary>

The software paradigm that is best suited to represent and manipulate population data is object-oriented programming (OOP). On the other hand, input and output data (especially in complex projects) are best stored in a relational database. Unfortunately, database relational modelling is less intuitive than OOP and requires a specific language (SQL) to retrieve and modify data.

In JAS-mine the interaction between the simulation and the (input and output) data is inspired by Object-Relational Mapping (ORM), a programming approach that facilitates the integration of object-oriented software systems with relational databases. An ORM product (JAS-mine uses [Hibernate](http://hibernate.org/orm/)) constructs an object-oriented interface to provide services on data persistence, while abstracting at the same time from the implementation characteristics of the specific RDBMS (database management software) used. The management of input data persistence layers and simulation results is performed using standard database management tools, and the platform takes care of the automatic translation of the relational model (which is typical of a database) into the object-oriented simulation model, where each category of individuals or objects that populate the model is represented by a specific class, with its own properties and methods.

![Hibernate Position](http://www.tutorialspoint.com/images/hibernate_position.jpg)

The main advantages of using an ORM system are:

1. the masking of the implementation of the relational model in an object-oriented model;
2. high portability compared to the DBMS technology adopted: no need to rewrite data input queries on database when changing DBMS, simply modify a few lines in the configuration of the ORM used;
3. a drastic reduction in the amount of code to be written; the ORM masks the complex activities of data creation, extraction, update and deletion behind simple commands. These activities take up a considerable proportion of the time required for writing, testing and maintenance. Moreover they are inherently repetitive, thus increasing the chance of errors when writing the implementation code.

The most common ORM products available today offer a number of functions that would otherwise be performed manually by the programmer; in particular, the operations of loading the object graph based on association links defined at language level, and reading/writing/deleting are entirely automated. For instance, loading an instance of the `Student` class may result in the automatic loading of data concerning the student's exam grades.

The use of an ORM facilitates the achievement of higher quality software standards, in particular improving its correctness, maintainability, potential evolutions and portability. On the down side, choosing an ORM paradigm introduces a software layer that impacts on performance, an aspect that is relevant to data-intensive applications like simulations. Translating the entity-relational model that is typical of a database into an object-based model requires additional activities that may slow down data upload and reading. Given the continuous increases in the speed and power of modern computers, we opted for a lean architectural structure even at the cost of slowing down the simulation engine.
</details>

This also allows to separate data creation from data analysis, which is crucial for understanding the behaviour of the simulation model. As the statistical analysis of the model output is possibly intensive in computing time, performing it in real time might be an issue, in large-scale applications. A common solution is to limit real-time monitoring of simulation outcomes to a selected subset of output variables. This however requires identifying the output of interest before the simulation is run. If additional computations are required to better understand how the model behaves, the model has to be run again: the bigger the model, the more impractical this solution is.

On the other hand, the power of modern RDBMS make it feasible to keep track of a much larger set of variables, for later analysis. Also, the statistical techniques envisaged, and the specific modeler’s skills, might suggest the use of external software solutions, without the need to integrate them in the simulation machine.

Finally, keeping data analysis conceptually distinct from data production further enhances the brevity, transparency and clarity of the code.
</details>


