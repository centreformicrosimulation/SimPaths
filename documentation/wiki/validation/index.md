# Model Validation

<p class="page-intro">This section explains the current procedures used to validate SimPaths inputs and outputs, from regression checks to aggregate comparisons against external survey data.</p>

<p class="page-intro page-intro--support">Validation is a core part of maintaining trust in the model after changes to parameters, code, or input data, and it provides a structured way to spot specification or implementation issues early.</p>

## 1. Introduction 

At present, validation is organised into three main steps:

1. **Validating regression estimates**  
   This step assesses the performance of the regression models that govern key behavioural and demographic processes within SimPaths (e.g. leaving the parental home, returning to education). Using the estimated regression coefficients, we generate predicted values for each observation in the estimation sample, compute/plot aggregate statistics, and compare these with the equivalent values from the estimation sample. This provides a straightforward check that the estimated relationships embedded in the model are consistent with observed empirical patterns.

2. **Validating simulated output**  
   The second step examines the simulated output produced by SimPaths. The model is run for a period in which comparable survey data are available (2011–2023), and aggregate measures from the simulated data are compared to benchmarks computed using data from the UK Household Longitudinal Study (UKHLS). The validation focuses on the model's ablility to reproduce **aggregate measures over time** (time-series consistency) and **distributions within years**, rather than the accuracy of individual trajectories through time. 

3. **Validating regression estimates from simulated output**  
   *To be completed.*


## 2. Obtaining the validation scripts

Validation procedures are currently executed in **Stata**. The corresponding do-files are located in the *validation* subfolder on the `develop` branch of the *SimPaths* GitHub repository.  

You can access these files in one of three ways:

1. **Clone the repository** – recommended for developers who want the full version history or plan to contribute changes. See the [Working in GitHub guide](../developer-guide/working-in-github.md).

2. **Download the repository as a ZIP file** – provides a snapshot of all files on the selected branch.  
   - In the GitHub interface, select the `develop` branch, click the green **Code** button, and choose **Download ZIP**.  
   - Extract the ZIP file locally and navigate to the *validation* folder.

3. **Download individual files directly from GitHub** – suitable if you only need a few specific scripts.  
   - Navigate to the desired file in the repository (e.g. *validation/01_estimate_validation/00_master.do*).  
   - Click the **Download raw file** icon (the downward arrow) on the top right of the file viewer to save it locally.

Each method gives you the same file contents; the difference is whether you download just one file, a snapshot of the branch, or the entire version-controlled repository.


## 3. Running the validation scripts

Once you have obtained the relevant validation files, the next step is to run them in **Stata**.  
This section explains how to set up your working environment, what data are required, and how to execute the validation do-files for each stage of validation.

### 3.1 Validating regression estimates

These do-files are contained in the subfolder *01_estimate_validation*.  
Before running these scripts, four preparatory steps are required:

- **a. Run the regression estimation do-files**  
  The validation do-files requires datasets produced during the regression estimation stage.  
  Ensure that the estimation do-files have been run and that the output data are available before proceeding.  

- **b. Set up the file structure**  
  Ensure the downloaded do-files in an **estimate validation** folder in a subfolder called *do_files*.  
  Create additional subfolders *data* and *graphs*.  
  Within the *graphs* subfolder, create the following subfolders:  
  - `education`  
  - `fertility`  
  - `health`  
  - `home_ownership`  
  - `income`  
  - `leave_parental_home`  
  - `partnership`  
  - `retirement`  
  - `wages`  

- **c. Check the location of the input data files**  
  Place the necessary data files in the *data* subfolder.  
  These will contain “sample” in their title (e.g. *E1_sample*) and are produced in the regression estimation do-files.  

- **d. Update directory paths in *00_master.do*** 
  Before running the validation do-files, set up the file directories.  
  Open *00_master.do* and update the global file paths as necessary.  
  If the file structure is set up as above, only the global *dir_work* needs to be changed to correspond to the main folder for estimate validation. 

Once these steps have been completed, you can straightforwardly run the do-files to produce the validation plots.


### 3.2 Validating the simulated output

The do-files for validating the simulated output are contained in the subfolder *02_simulated_output_validation*.  
These should be run **after executing *SimPaths***, as they rely on a number of *.csv* files produced by the model.  

Before running these scripts, complete the following preparatory steps:

- **a. Obtain simulated output from *SimPaths***  
  Ensure that the most recent simulated output is available. See the [Running Your First Simulation page](../getting-started/first-simulation.md) or the [Multiple Runs page](../user-guide/multiple-runs.md) to run SimPaths and obtain the simulation outputs.  

- **b. Set up the file structure**  
  Place the downloaded do-files in a **simulation validation** folder in a subfolder called *do_files*.  
  Also create additional subfolders: *data* and *graphs*.  
  Within the *graphs* subfolder, create the following subfolders:  
  - `care`  
  - `children`  
  - `disability`  
  - `economic_activity`  
  - `education`  
  - `health`  
  - `hours_worked`  
  - `income/capital_income`  
  - `income/disposable_income`  
  - `income/equivalized_disposable_income`  
  - `income/gross_income`  
  - `income/gross_labour_income`  
  - `income/pension_income`  
  - `inequality`  
  - `partnership`  
  - `poverty`  
  - `wages`  

  Each of these subfolders will contain the relevant validation plots produced by the corresponding do-files.  

- **c. Deposit input data**  
  Place the simulated output *.csv* files titled `Person`, `BenefitUnit` and `Household` in the *data* subfolder.  
  You will also need the Understanding Society survey data to compare against the simulated output.  
  For this purpose, we currently use the following initial population files:  
  - *ukhls_pooled_all_obs_01*  
  - *ukhls_pooled_all_obs_09*  

- **d. Update directory paths in *00_master.do***  
  Open *00_master.do* in the *do_files* subfolder and update the global paths.  
  In the “Define directories” section, update the global *dir_path* to point to the location of the main simulation validation folder.  
  Run the file up to (but not including) the “Run do files” section to set directories and parameters (adjust as necessary).  

Once these steps have been completed, you can straightforwardly run the do-files to produce the validation plots.
