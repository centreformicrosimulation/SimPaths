**********************************************************************************************
*Do file producing estimates for the parametric couple matching process in the Simpaths model
*Author: Patryk Bronka, Daria Popova 
*Last edit: Daria Popova 
*Date: 4 Ju;y 2025  
**********************************************************************************************
clear all
set more off
set mem 200m
set type double
//set maxvar 120000
set maxvar 30000

*ssc install extremes

/*******************************************************************************
*	DEFINE DIRECTORIES
*******************************************************************************/
* Working directory
global dir_work "D:\Dasha\ESSEX\ESPON 2024\UK\regression_estimates\union_parametrisation"

* Directory which contains pooled UKHLS dataset 
global dir_ukhls_data "D:\Dasha\ESSEX\ESPON 2024\UK\initial_populations\data"

* Set Excel file 

* Info sheet
putexcel set "$dir_work/scenario_parametricMatching", sheet("Info") replace
putexcel A1 = "Description:"
putexcel B1 = "Estimates for the parametric couple matching process"
putexcel A2 = "Authors:	Patryk Bronka, Daria Popova" 
putexcel A3 = "Last edit: 4 July 2025 DP"


*******************************************************************************************************************************
*1. Load initial population data 
*import delimited $gitFolder\population_UK_initial.csv, clear
use "$dir_ukhls_data/ukhls_pooled_all_obs_10.dta", clear //note this is a pooled dataset after Heckman has been estimated  

sort idperson stm  
xtset idperson stm 
gen newMarriage = (idpartner > 0 & idpartner<.) & (l.idpartner<= 0 | l.idpartner>=.)
*Note: individuals whose dcpyy (number of years in a partnership) equals 1, are newly married

save "$dir_work/parametricUnionDataset", replace 


*2. Use wages predicted using wage equation:
sum pred_hourly_wage if dgn == 0
sum pred_hourly_wage if dgn == 1

gen predictedWage=pred_hourly_wage

*3. Keep only those above 18 as that's the minimum age to get married in the simulation
keep if dag >= 18

*4. Look at newly matched couples in the initial population (this requires the longitudinal component). 
*This has been added to the input data file as newMarriage variable
tempfile partners
preserve
keep if dgn == 0 //All partners female
keep stm idperson idhh dgn dag predictedWage
rename idperson idpartner
rename dag dagPartner
rename predictedWage predictedWagePartner
rename dgn dgnPartner
save `partners', replace
restore 

//Keep only newly matched people
drop if idpartner < 0 | missing(idpartner) 
keep if newMarriage
keep if dgn == 1

merge 1:1 stm idpartner using `partners', keep(matched)


*4. Look at the difference in wage and age of the newly matched couples
*The first partner should probably always have the same gender, so calculate the difference between male - female
gen dagDifference = dag - dagPartner

/*check for outliers in wages*/
sum predictedWage , d
sum predictedWage [weight=disclwt], d

extremes predictedWage, n(20) freq high
/*
freq:	predict~e	
		
1	193.11859	
1	198.58132	
1	198.68025	
1	204.45788	
1	231.91774	
		
1	236.29345	
1	240.13629	
1	246.56729	
1	307.37445	
1	309.38673	
		
1	335.10219	
1	346.1395	
1	371.56325	
1	426.89188	
1	427.71505	
		
1	452.59122	
1	513.48099	
1	516.99593	
1	696.44839	
1	982.29694	
*/

extremes predictedWagePartner, n(20) freq high
/*
freq:	predict~r	
		
1	148.58268	
1	151.67459	
2	153.68723	
1	154.69337	
1	186.93118	
		
1	191.78429	
1	212.0091	
1	221.97558	
1	222.76736	
1	274.04278	
		
1	277.90405	
1	288.26281	
1	301.90966	
1	305.08388	
1	330.18868	
		
1	426.80633	
1	478.99185	
1	482.67028	
1	641.02564	
1	952.05343	
*/

*Trim outliers 
foreach var in predictedWage predictedWagePartner {
centile `var', centile(1 99)
scalar p1 = r(c_1)
scalar p99 = r(c_2)
replace `var' = p1 if `var' < p1
replace `var' = p99 if `var' > p99 & !missing(`var')
}

gen predictedWageDifference = predictedWage - predictedWagePartner 
drop if missing(dagDifference) | missing(predictedWageDifference)
sum predictedWageDifference, d
//sum predictedWageDifference [weight=disclwt], d


*5. Plot the distribution of wage and age differentials against a normal distribution
hist dagDifference, frequency normal

hist predictedWage, frequency normal
hist predictedWagePartner, frequency normal
hist predictedWageDifference, frequency normal


 
*6. Obtain the parameters for the bivariate normal distribution 
*Sample moments are a good enough approximation to the true parameters?
sum dagDifference predictedWageDifference //Get sample mean and std dev

putexcel set "$dir_work/scenario_parametricMatching", sheet("Parameters") modify 
putexcel A1=("Parameter") 
putexcel A2=("mean_dag_diff")
putexcel A3=("mean_wage_diff")
putexcel A4=("var_dag_diff")
putexcel A5=("var_wage_diff")
putexcel A6=("cov_dag_wage_diff")
putexcel B1=("Value")

qui sum dagDifference 
putexcel B2=matrix(r(mean)')
putexcel B4=matrix(r(Var)')

qui sum predictedWageDifference
putexcel B3=matrix(r(mean)')
putexcel B5=matrix(r(Var)')

corr dagDifference predictedWageDifference, cov 
return list
matrix list r(C) //Get variance-covariance matrix

putexcel B6=matrix(r(cov_12)') 


*rho x,y = cov x,y / (sigma x * sigma y), which is equivalent to corr dagDifference predictedWageDifference
corr dagDifference predictedWageDifference

/* 
Mean dagDifference = -2.19378
Sigma dagDifference = 5.472693 // Variance is 29.950369, Bessel corrected variance is 30.02219242685851, so corrected sigma is 5.47925108266253
Mean predictedWageDifference = -6.563083
Sigma predictedWageDifference = 4.282041 //Variance is 18.335874, Bessel corrected variance is 18.37984492086331, so corrected sigma is 4.287172135669771

rho = cov(x,y) / (sigma(x)*sigma(y)) = 6.1343291 / (5.472693*4.282041) = 0.261767... ~ 0.2618 which is equivalent to correlation of dagDifference and predictedWageDifference

Bessel's correction to get the unbiased estimator:
*/

scalar BesselCorrection = _N / (_N - 1)
di BesselCorrection

*Corrected rho:
qui corr dagDifference predictedWageDifference
di "Small sample corrected rho:"
di r(rho) * BesselCorrection
