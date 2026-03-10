# How to Perform MultiRun Simulations

# 1. The MultiRun feature of JAS-mine

There are many reasons why we may want to run and re-run our simulation many times. This may be to understand how the output of the model changes as a response to changes in the parameters of the model allowing for detailed design of experiments (DOE), parameter search / sensitivity analysis or optimization. It may also be because we want to understand the (Monte-Carlo) error or uncertainty in the output statistics of our model. In the first case, experimental design usually involves spanning over the values of the parameters, keeping the random number seed fixed. In the latter, it involves running the simulation a given number of times, without changing the values of the parameters (but changing the random number seed).

JAS-mine provides the '[MultiRun](https://www.microsimulation.ac.uk/jas-mine/resources/cookbook/the-multirun-class/)' functionality to enable users to deal with these cases.

Note that this functionality involves running a simulation many times in sequence on a single computer, as opposed to on parallel nodes. Future development work is intended to enable users of JAS-mine to easily set their simulations to run on parallel architecture, and tutorials will be added to describe this feature when it is ready.

The MultiRun functionality of JAS-mine is created by a wrapper class that wraps around the model, thus removing the need to change any of the internal workings of the model itself. A MultiRun template class is included in the new JAS-mine project created using the [JAS-mine Plugin for Eclipse IDE](https://marketplace.eclipse.org/content/jas-mine-plugin-eclipse-ide), so that users can see the general structure of a MultiRun class:

![JAS-mine MultiRun template](https://www.microsimulation.ac.uk/wp-content/uploads/2019/06/JAS-mine-multirun.png)

# 2. The components of the MultiRun concrete class

The abstract `MultiRun` class exists in the JAS-mine-core libraries in the `microsim.engine` package. In order to implement a multi run simulation, the user must create a concrete class that extends the abstract `MultiRun` class. An example of the concrete MultiRun template class (called '<*Project Name*>MultiRun.java') can be found in the experiment package of a new JAS-mine project, and the class extends the abstract 'MultiRun' class from the JAS-mine-core libraries. The abstract methods `nextModel()` and `setupRunLabel()` must be overriden by the concrete template class.

The `nextModel()` method should return a boolean which determines whether another new simulation should be launched. In the template example, the simulation is repeated *n* times for each of a specified population size of agents, but the boolean could instead depend, for instance, on whether a loop through a more complicated set of model parameters has terminated, signalling the completion of a parameter search experiment.

The `setupRunLabel()` method provides a unique MULTI_RUN_ID name for each simulation run, which could signify a parameter in the model such as the country represented in the simulation for example; it is stored in the output database in the JAS_EXPERIMENT output table, as in the screenshot below:

![JAS-mine MultiRun ID](https://www.microsimulation.ac.uk/wp-content/uploads/2019/06/JAS-mine-multirun-id.png)

The concrete `MultiRun` class must include a `main(String[] args)` function in which to launch the MultiRun version of the simulation. As this class is used to launch the simulations instead of the Start class, the concrete MultiRun class also needs to include a `buildExperiment()` method to replace the one used in the Start class. Note that when running multiple runs, it is often the case that the user will want to optimise speed of execution, so it is recommended not to invoke the Observer class in the `buildExperiment()`, nor enable the normal MicrosimShell gui that is normally initiated in the `main()` method of the Start class. Therefore in the template only the model and collector are constructed in the `buildExperiment()` method. And instead of the MicrosimShell gui, a useful progress monitor gui can be used within Eclipse, and is initiated with the `MultiRunFrame` invocation, as seen in the `main()` method of the MultiRun template class. In this example. the MultiRunFrame can be toggled using the '*executeWithGUI*' boolean field. See below for a screenshot of the MultiRunFrame:

![JAS-mine MultiRun Frame](https://www.microsimulation.ac.uk/wp-content/uploads/2019/06/JAS-mine-multirun-frame.png)


# 3. Executing the MultiRun simulation mode

## 3.1 Within an IDE

Running a MultiRun version of a JAS-mine project in an IDE is very easy. Here we refer to Eclipse IDE. Just right-click on the concrete class (named '<*Project Name*>MultiRun.java' if using the JAS-mine plugin for Eclipse IDE), and select 'Run As / Java Application' menu. The MultiRunFrame (as shown in the screenshot above) should pop up, and the simulation sequence can be started by clicking the 'Start' button.

The progress bar of the MultiRunFrame will oscillate between empty and full colour, showing that progress is being made behind the scenes. The 'Current run number' represents the id of the current run, which counts the total number of simulations that have so far completed plus one. The 'Current run step' indicates the simulation run's internal time clock, i.e. a run step of 10.52 indicates that the events currently firing in the simulation were scheduled to occur at 10.52 time units. The 'Current step' is the run label, which is stored in the MULTI_RUN_ID of the JAS_EXPERIMENT table, and is the unique id of this particular simulation. As discussed in section 2 above, this could represent the combination of a model parameter such as the country that the run represents, and an index label, as in the case of the screenshots of the database and MultiRunFrame above.

Once the MultiRun simulation is complete (i.e. the `nextModel()` method returns the false boolean), the MultiRunFrame will disappear and the MultiRun is over.

## 3.2 Batch mode

An alternative to executing the MultiRun functionality of a JAS-mine project from within an IDE is to use the batch environment (e.g. the command prompt in Windows, or the terminal in Linux). Indeed it may be necessary to use batch mode to launch JAS-mine simulations when running on a high performance computing facility. Users may also find that it is faster to execute their simulations in this way on their personal computers.

When using Batch mode, users should disable the MultiRunFrame (in the template MultiRun class of the new JAS-mine project, this can be achieved by setting '*executeWithGUI*' boolean to false, see the next paragraph) and they should ensure that the Observer class is not invoked in the `buildExperiment()` method of the concrete MultiRun class; these are unnecessary in Batch mode and could slow the simulations down.

#4. Setting the program arguments

The number of times to run a simulation and use of the MultiRunFrame can be determined by passing program arguments to the run environment when launching the application (which are then used as the 'args field in the `main(String[] args)` method. This is done either:

i) From the command prompt (in Windows) or terminal (in Linux) when launching the compiled classes with the command:
```
java TestMultiRun -n 1000 -g false
```

where the `-n` flag sets the number of runs to 1000, and the `-g` flag sets the `executeWithGUI` field to false (which disables the MultiRunFrame).

ii) Alternatively, the program arguments can be set in the IDE by clicking on the 'Run / Run Configurations' menu and setting the values in the 'Program arguments' window under the Arguments tab for the MultiRun application, as in the screenshot below:

![JAS-mine program arguments](https://www.microsimulation.ac.uk/wp-content/uploads/2019/06/JAS-mine-program-arguments.png)