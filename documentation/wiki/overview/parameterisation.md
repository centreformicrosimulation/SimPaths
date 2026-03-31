# Model Parameterisation

The estimates for the utility functions used in the labour supply model are separately described in Richiardi, M. and He, Z. (2021), “_No one left behind: The labour supply behaviour of the entire Italian population_”, Centre for Microsimulation and Policy Analysis, mimeo.

The estimates for the psychological distress models are separately described in Kopasker, D., et al. "_Evaluating the influence of taxation and social security policies on psychological distress: a microsimulation study of the UK during the COVID-19 economic crisis._" Social Science & Medicine (2024): 116953.

The model has been parametrised for the UK using data described in the [SimPaths GitHub data section](https://github.com/simpaths/SimPaths/tree/develop/input).

The most recent parametrisation of the model is available in the [SimPaths GitHub repository input folder](https://github.com/simpaths/SimPaths/tree/develop/input).



## 1. Description of the tax and benefit system display

Description of the tax and benefit system is provided through UKMOD output files stored in the [SimPaths GitHub EUROMODoutput folder](https://github.com/simpaths/SimPaths/tree/develop/input/EUROMODoutput/). A version developed on the basis of test data, which can be shared on GitHub, is available in the [SimPaths GitHub EUROMODoutput training folder](https://github.com/simpaths/SimPaths/tree/develop/input/EUROMODoutput/training).

To learn more about UKMOD, visit the [UKMOD website](https://www.microsimulation.ac.uk/ukmod/).



## 2. Model parameters 

Source: [SimPaths GitHub input folder](https://github.com/simpaths/SimPaths/tree/develop/input).

**align_ files**  
Files listed below contain alignment targets.

* align_educLevel.xlsx
* align_employment.xlsx 
* align_popProjections.xlsx 
* align_student_under30.xlsx

**projections_ files**  
Files listed below contain demographic projections.

* projections_fertility.xlsx 
* projections_mortality.xlsx

**reg_ files**  
Files listed below contain regression estimates for specific processes described on the [Model Description page](model-description.md).

* reg_RMSE.xlsx 
* reg_childcarecost.xlsx 
* reg_education.xlsx 
* reg_employmentSelection.xlsx
* reg_fertility.xlsx 
* reg_health.xlsx 
* reg_health_mental.xlsx 
* reg_home_ownership.xlsx 
* reg_income.xlsx 
* reg_labourCovid19.xlsx 
* reg_labourSupplyUtility.xlsx 
* reg_leaveParentalHome.xlsx 
* reg_partnership.xlsx 
* reg_retirement.xlsx 
* reg_socialcare.xlsx 
* reg_unemployment.xlsx 
* reg_wages.xlsx

**scenario_ files**  
Files listed below contain parameters reflecting specific modelling assumptions. 

* scenario_CPI.xlsx 
* scenario_employments_furloughed.xlsx 
* scenario_parametricMatching.xlsx 
* scenario_retirementAgeFixed.xlsx
