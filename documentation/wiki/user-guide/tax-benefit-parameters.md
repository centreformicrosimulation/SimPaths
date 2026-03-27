# Modifying Tax-Benefit Parameters

SimPaths does not hard-code a full tax-benefit system inside Java. Instead, tax and benefit outcomes are mainly read from donor data generated outside the model and then matched back onto simulated benefit units during a run.

In practice, changing tax-benefit settings usually means changing one of three things:

1. the EUROMOD / UKMOD output files stored in `input/EUROMODoutput/`
2. the policy schedule in `input/EUROMODpolicySchedule.xlsx`
3. the runtime settings that control how donor information is used

## 1. Donor policy files

The donor database is built from policy output files such as:

- `input/EUROMODoutput/*.txt`

During setup, `TaxDonorDataParser.constructAggregateTaxDonorPopulationCSVfile()` reads those text files, extracts the required policy-dependent and policy-invariant variables, and writes an aggregate donor CSV that is then loaded into the H2 input database.

Two important assumptions in this process are:

- all policy files for a given country must be based on the same donor input population
- the files must have consistent row counts across policy systems

If that consistency breaks, database setup fails.

## 2. Policy schedule

`input/EUROMODpolicySchedule.xlsx` tells SimPaths which donor policy should apply from which simulation year onward.

The relevant columns are:

- `Filename`
- `Policy_Start_Year`
- `Policy_System_Year`
- `Description`

In code, this file is loaded by `Parameters.calculateEUROMODpolicySchedule()`, which builds the map from simulation years to policy names.

The most important rules are:

- each policy start year must be unique
- the policy system year must match the system year used when the donor file was produced
- policies with no start year are ignored
- at least one policy file must exist for the model's base price year, currently `2015`

If no policy begins exactly in the simulation start year, SimPaths uses the earliest available policy as the prevailing regime.

## 3. Runtime settings that affect tax-benefit use

Some settings in `config/default.yml` change how donor-based tax-benefit outcomes are used inside the simulation.

The most relevant ones are:

- `donorPoolAveraging` in `model_args`
  - if `true`, SimPaths averages over nearest-neighbour donors rather than using a single closest donor
- `projectFormalChildcare`
  - whether formal childcare costs are simulated
- `projectSocialCare`
  - whether the social-care module is active
- `flagSuppressChildcareCosts` and `flagSuppressSocialCareCosts`
  - scenario-style switches that suppress those cost components

These are model settings rather than replacements for a new policy schedule. If you want a different tax-benefit regime, the donor files and policy schedule still need to reflect it.

## Typical workflow for updating policy inputs

If you want to introduce a new policy system or adjust an existing one, the usual sequence is:

1. generate the required EUROMOD / UKMOD output `.txt` files
2. copy them into `input/EUROMODoutput/`
3. update `input/EUROMODpolicySchedule.xlsx`
4. rebuild the donor database
5. rerun the simulation

For a headless rebuild, the clearest commands are:

```bash
java -jar singlerun.jar -Setup -c UK -s 2019 -g false --rewrite-policy-schedule
```

or:

```bash
java -jar multirun.jar -DBSetup -config config/default.yml
```

## When you must rebuild the database

You should rebuild `input/input.mv.db` whenever you change:

- any donor `.txt` file in `input/EUROMODoutput/`
- `input/EUROMODpolicySchedule.xlsx`
- donor-data paths in `parameter_args`

Changing only higher-level run controls such as `startYear`, `endYear`, or output settings does not by itself require a rebuild.

## What this page does not cover

This page is about how SimPaths consumes tax-benefit inputs. It does not document the full external policy-modelling workflow inside EUROMOD / UKMOD itself. For donor-data preparation, see [Input Data](../getting-started/data/index.md).

## Related pages

- [Input Data](../getting-started/data/index.md)
- [Environment Setup](../getting-started/environment-setup.md)
- [Multiple Runs](multiple-runs.md)
