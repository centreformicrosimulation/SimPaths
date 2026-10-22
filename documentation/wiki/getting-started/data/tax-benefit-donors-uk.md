# Tax-Benefit Donors (UK)

SimPaths reads precomputed tax-benefit outcomes produced with UKMOD. UKMOD execution is Windows-only; once compatible outputs have been created, SimPaths can read the files on other supported platforms.

1. Obtain an authorised version of [UKMOD](https://www.microsimulation.ac.uk/ukmod/access/) and the corresponding input data. For regional analysis, the workflow uses the pooled three-year FRS datasets described by the UKMOD provider.
2. Run every required policy-system year using the same compatible input dataset and export the variables required by SimPaths.
3. Include the base price year required by the checked code and parameter files. Do not assume that the value documented for another release remains current.
4. Copy the outputs to `input/EUROMODoutput/`, preserving the naming convention expected by the setup code. Keep licensed donor data out of the public repository.
5. Rebuild the donor inputs with the single-run **Load new input data for tax and benefit systems** option or the corresponding headless/database-setup route.
6. Check the generated policy schedule before running. Rewriting it can replace a custom research scenario.

The repository's `input/EUROMODoutput/training/` files support installation checks and development, not substantive interpretation. See [Modifying Tax-Benefit Settings](../../user-guide/tax-benefit-parameters.md) for selecting policy systems and [Repository Guide](../../developer-guide/repository-guide.md#data-pipeline-reference) for the wider data flow.
