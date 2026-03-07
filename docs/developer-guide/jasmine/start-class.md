# The Start Class

In a JAS-mine project, the *Start* class serves to initialize and run the JAS-mine simulation engine and to define the list of models to be used. The *Start* class is designed to handle two types of situations:

* performing a single run of the simulation in **interactive mode**, through the creation of a Model and related *Collectors* and *Observers*, with their GUIs;
* performing a single run of the simulation in **batch mode**, through the creation of the *Model* and possibly the *Collectors*; this involves managing parameter setup, model creation and execution directly, and is aimed at capturing only the simulation's numerical output;

Note that in order to run the simulation many times, it is necessary to use the the MultiRun class instead of the Start class. For more information, see this tutorial.

The *Start* class must implement the *ExperimentBuilder* interface, which defines the *buildExperiment*() method. This method should create managers and add them to the JAS-mine engine. In the example below, a model called *DemoModel* is created and run in interactive mode:

```java
public static void main(String[] args) {   
  
    boolean showGui = true;   
    SimulationEngine engine = SimulationEngine.getInstance();   
    MicrosimShell gui = null;   
    if (showGui) {   
        gui = new MicrosimShell(engine);   
        gui.setVisible(true);   
    }   
    engine.setBuilderClass(StartDemo.class);   
    engine.setup();   
  
}   
  
@Override   
public void buildExperiment(SimulationEngine engine) {  
   
    DemoModel model = new DemoModel();   
    PersonsCollector collector = new PersonsCollector(model);   
    PersonsObserver observer = new PersonsObserver(model, collector);
    engine.addSimulationManager(model);
    engine.addSimulationManager(collector);
    engine.addSimulationManager(observer);   
  
}
```