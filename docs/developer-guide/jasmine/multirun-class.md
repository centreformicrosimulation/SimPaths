# The MultiRun Class

In a JAS-mine project, the *MultiRun* class replaces the *Start* class when the user wants to repeatedly run simulations using different parameter values, so as to explore the space of solutions and produce sensitivity analyses on the specified parameters. For information on how to use the multi run functionality, see the tutorial. Like the *Start* class, the *MultiRun* class serves to initialize and run the JAS-mine simulation engine and to define the list of models to be used. Unlike the *Start* class, the user will not generally want to make use of the interactive mode and the JAS-mine GUI, as often time constraints require maximising the speed of the simulations in a multi run situation; using the Observer and JAS-mine GUI will only slow the simulation speed down, so these will not likely be used in a standard multi run set-up. The MultiRun class is therefore designed to handle the following:

* performing a multi run of the simulation in **batch mode**, through the creation of the *Model* and possibly the *Collectors*; this involves managing parameter setup, model creation and execution directly, and is aimed at capturing only the simulation's numerical output.

The abstract MultiRun class exists in the JAS-mine-core libraries in the microsim.engine package. In order to implement a multi run simulation, the user must create a concrete class that extends the abstract *MultiRun* class. An example of the concrete MultiRun template class (called '<*Project Name*>MultiRun.java') can be found in the experiment package of a new JAS-mine project, and the class extends the abstract *MultiRun* class from the JAS-mine-core libraries. Below, we create an example of a concrete *TestMultiRun*, which extends the abstract *MultiRun* class. As this class is used to launch simulations instead of the *Start* class, *TestMultiRun* must include a main(String[] args) function in which to launch the multi run version of the simulation. In the example below, to facilitate running in batch mode via the command line interface (such as Window's Command Prompt or the Linux Terminal), we can use program arguments to set parameters via the String[] args. For more information see the section 4 of the MultiRun tutorial. The abstract *MultiRun* class implements the *ExperimentBuilder* interface, so the *TestMultiRun* class must also define the *buildExperiment*() method. This method should create managers and add them to the JAS-mine engine. In the example below, a model called *TestModel* is created along with a collector called *TestCollector*, and the simulation is run in batch mode:

```java
@Override   
public void buildExperiment(SimulationEngine engine) {  
   
    TestModel model = new TestModel();  
  
    engine.addSimulationManager(model);  
  
    TestCollector collector = new TestCollector(model);  
    engine.addSimulationManager(collector);  
                    
}
```

Note that when running multiple runs, it is often the case that the user will want to optimise speed of execution, so it is recommended not to invoke the Observer class in the *buildExperiment*(), nor enable the MicrosimShell gui that is normally used in interactive mode and initiated in the main() method of the Start class. Therefore in the our *TestMultiRun* example, only the model and collector are constructed in the *buildExperiment*() method. Instead of the MicrosimShell gui, a useful progress monitor gui can be used within Eclipse, and is initiated with the *MultiRunFrame* invocation, as seen in the main() method of the *TestMultiRun* class. In this example, the MultiRunFrame can be toggled using the '*executeWithGUI*' boolean field. Here's a screenshot of the MultiRunFrame GUI:

![MultiRunFrame](https://www.microsimulation.ac.uk/wp-content/uploads/page/MultiRunFrame.png)

There are two abstract methods in the abstract *MultiRun* class – *nextModel*() and *setupRunLabel*() – that must be overriden by the concrete *TestMultiRun* class.

The *nextModel*() method should return a boolean which determines whether another new simulation should be launched. In the *TestMultiRun* example, the simulation is repeated a number of times equal to *numberOfRepeatedRuns* for each value *k\***numberOfAgents* of the population size, with *k =* 1*…K*, but the boolean could instead depend, for instance, on whether a loop through a more complicated set of model parameters has terminated, signalling the completion of a parameter search experiment.

```java
@Override  
public boolean nextModel() {  
  
    // Update the values of the parameters for the next experiment  
    counter++;  
  
    if(counter > numberOfRepeatedRuns) {  
        numberOfAgents *= 10;            // Increase the number of agents by a factor of 10   
                                         // for the next experiment  
        counter = 1L;                    // Reset counter  
    }  
  
    // Define the continuation condition  
    if(numberOfAgents < maxNumberOfAgents) {    // Stop when the numberOfAgents goes above                                                            // maxNumberOfAgents  
        return true;  
    }  
    else return false;  
}
```

The *setupRunLabel*() method provides a unique MULTI_RUN_ID name (a string) for each simulation run. This can be the current run number, as provided by *counter.toString()*, or a more "telling" label, as:

```java
@Override  
public String setupRunLabel() {  
    return numberOfAgents.toString() + " agents, count: " + counter.toString();  
}
```

The labels are stored in the output database in the JAS_EXPERIMENT output table. 
