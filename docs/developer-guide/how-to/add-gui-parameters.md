# GUI Parameters

Managers (typically, the `Model`) can define some variables that are regarded as simulation parameters, in addition to those defined in the input database. These are control parameters that can be changed from the **GUI** before the simulation starts or while the simulation is running in order to experiment with the model behaviour in interactive mode.

To this end, all the properties of the manager class that are marked with the `@GUIparameter` annotation (which replaces the deprecated `@ModelParameter` annotation) are regarded as model parameters. Accordingly, the **JAS-mine engine** generates a window for each defined manager, with the list of variables defined as parameters, so that the user can change them via the GUI before constructing and running the model. These parameters are added to the ones defined in the input database tables, for example to define specific simulation scenarios on the fly, in interactive mode.

The persistence system integrated in JAS-mine records these values in a specific table of the output database, together with the date and time of execution of the simulation run.