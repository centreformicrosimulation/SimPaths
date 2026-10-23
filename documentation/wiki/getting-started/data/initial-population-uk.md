# Initial Population (UK)

UK research populations are compiled from licensed survey data. The repository supplies the Stata pipeline and de-identified training data, but not the underlying restricted records.

1. Obtain the UKHLS release supported by the checked compilation scripts from the [UK Data Service](https://ukdataservice.ac.uk/) (study 6614), plus the Wealth and Assets Survey inputs used by that revision (study 7215).
2. Open `input/InitialPopulations/compile/00_master.do`, review its data-release assumptions and set the local paths without committing them.
3. Run the master pipeline in Stata and review its validation and consistency checks.
4. Place the generated `population_initial_UK_<year>.csv` files in the initial-population input directory expected by the run configuration.
5. Rebuild the population database using the intended country and start year.

Do not treat “most recent survey release” as automatically compatible with the current scripts. Record the data release, build revision, start year and weighting choices with the analysis. Never commit licensed input records or generated research populations.

For the sequence of compilation stages and regression-estimation inputs, see the [Repository Guide](../../developer-guide/repository-guide.md#data-pipeline-reference). For an installation check without restricted data, use the [bundled training workflow](../first-simulation.md).
