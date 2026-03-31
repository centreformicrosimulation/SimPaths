# Initial Population (UK)

In addition to training data, the model comes supplied with a set of Stata do files that have been written to extract input data from the UKHLS. These do files can be found in the model directory: `SimPaths/input/InitialPopulations/compile/`. 

1. Obtain the most recent version of the UKHLS survey from the [UK Data Service](https://ukdataservice.ac.uk/) (SN6614, in STATA's tab format). Further to this, you need to obtain the most recent version of the Wealth and Assets Survey (WAS) (SN7215, in STATA's tab format).
2. Use Stata to open file 00_master.do, and edit global variables at the top of the file, save and run.
3. Copy the csv files generated following (2) to model directory: `SimPaths/input/InitialPopulations/`.
4. Run SimPathsStart, and select option "Load new input data for starting populations" from the Start-up Options window.
