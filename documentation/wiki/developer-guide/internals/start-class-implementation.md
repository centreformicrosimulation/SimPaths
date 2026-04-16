# Start Class Implementation

The [SimPathsStart](https://github.com/simpaths/SimPaths/blob/main/src/main/java/simpaths/experiment/SimPathsStart.java) class is the entry point for running the SimPaths microsimulation model. It provides essential functionalities for initializing the simulation environment and offers methods for configuring simulation parameters, database setup, and user interactions.

# 1. Overview

This class handles the following primary functionalities:

1. **Displaying a GUI:** Users can define startup processes, such as selecting policies, modifying policies, or rebuilding the database, using the dialog box presented by this class.

2. **Selecting simulation country and start year:** The class adjusts country and start year based on user's choice. 

3. **Starting the Simulation Engine:** It initializes the JAS-mine simulation engine, optionally creating and displaying a graphical user interface for the simulation.

4. **Selecting and Starting an Experiment:** The `buildExperiment` method configures various components of the SimPaths model, including the model itself, a collector, and an observer.

5. **Creating Database Tables:** The `createDatabaseTables` method facilitates the creation of initial and donor population database tables based on user choices.


# 2. Methods and Functionality

<details markdown>
<summary><b>The <code>main</code> method</b></summary>

The `main` method serves as the entry point for running the SimPaths microsimulation model. It initializes simulation parameters, displays a GUI, and starts the JAS-mine simulation engine.

</details>

<details markdown>
<summary><b>The <code>buildExperiment</code> method</b></summary>

This method is called by the JAS-mine simulation engine to configure the components of the SimPaths model, including the model itself, a collector, and an observer.

</details>

<details markdown>
<summary><b>The <code>runGUIdialog</code> method</b></summary>

The `runGUIdialog` method allows users to define startup processes for the simulation through a dialog box. Options include running the GUI, selecting policies, modifying policies, and rebuilding the database.

</details>

<details markdown>
<summary><b>The <code>createDatabaseTables</code> method</b></summary>

This method is responsible for creating database tables required for the simulation. Users can choose to create initial population tables, donor population tables, or both, thus setting up the necessary database environment.

</details>

<details markdown>
<summary><b>The <code>chooseCountryAndStartYear</code> method</b></summary>

This method displays a GUI for selecting the country and starting year for the simulation. Users make choices via combo-boxes, and the selected values set the simulation's country and starting year. Additionally, the method saves these choices to an Excel file for future use.

</details>

<details markdown>
<summary><b>The <code>constructAggregatePopulationCSVfile(Country country)</code> method</b></summary>

This method constructs a CSV file by aggregating data from multiple UKMOD/EUROMOD output text files for a specific country. It extracts relevant columns and creates a CSV file that serves as input data for the creation of donor database tables.

</details>

<details markdown>
<summary><b>The <code>createInitialDatabaseTablesFromCSVfile(Country country)</code> method</b></summary>

This method builds initial population database tables from initial population CSV files. These tables represent the initial population for a specific country and starting year and are foundational for running simulations in the JAS-mine model.

</details>

<details markdown>
<summary><b>The <code>populateDonorTaxUnitTables(Country country)</code> method</b></summary>

This method populates donor tax unit tables with data from UKMOD/EUROMOD. It gathers information on gross and net income, demographic characteristics and benefits and stores it in the database. The method calculates various attributes related to tax units and adds them to the database tables.

</details>


# 3. Usage
Compiling and running the `SimPathsStart` class launches the app. 

