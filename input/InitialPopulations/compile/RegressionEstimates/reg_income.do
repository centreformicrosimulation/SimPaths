********************************************************************************
* PROJECT:  		ESPON
* SECTION:			Non-employment/non-benefit income
* OBJECT: 			Final Regresion Models 
* AUTHORS:			Patryk Bronka, Daria Popova, Justin van de Ven
* LAST UPDATE:		3 July 2025 DP  
* COUNTRY: 			UK

* NOTES: 			 Models for split income variable
*                    The goal is to split the current non-labour non-benefit income variable into 3 components  
*                    (capital returns, occupational pension, public pension) and estimate each of them separately, 
*                    using (if possible) current set of controls. We have decided to abstain from estimating transfers at the moment. 
*                       
*                       The income  do file must be run after
* 						the wage estimates are obtain because they use 
* 						predicted wages. 
/*******************************************************************************


*******************************************************************************/
********************************************************************************
clear all
set more off
set mem 200m
set type double
//set maxvar 120000
set maxvar 30000


/*******************************************************************************
*	DEFINE DIRECTORIES
*******************************************************************************/
* Working directory
//global dir_work "C:\MyFiles\99 DEV ENV\JAS-MINE\data work\regression_estimates"
global dir_work "D:\Dasha\ESSEX\ESPON 2024\UK\regression_estimates"

* Directory which contains do files
global dir_do "${dir_work}/do"

* Directory which contains data files 
global dir_data "${dir_work}/data"

* Directory which contains log files 
global dir_log "${dir_work}/log"

* Directory which contains pooled UKHLS dataset 
//global dir_ukhls_data "C:\MyFiles\99 DEV ENV\JAS-MINE\data work\initial_populations\data"
global dir_ukhls_data "D:\Dasha\ESSEX\ESPON 2024\UK\initial_populations\data"

*******************************************************************
cap log close 
log using "${dir_log}/reg_income.log", replace
*******************************************************************

import excel "$dir_external_data/time_series_factor.xlsx", sheet("UK_gdp") firstrow clear // Import real growth index
rename Year stm
rename Value growth
gen base_val = growth if stm == 2015
sum base_val
replace base_val = r(mean)
replace growth= growth/base_val
drop base_val
replace stm = stm - 2000
save "$dir_external_data\growth_rates", replace

use "$dir_ukhls_data/ukhls_pooled_all_obs_10.dta", clear //note this is a pooled dataset after Heckman has been estimated  

sort stm
merge m:1 stm using "$dir_external_data/growth_rates", keep(3) nogen keepusing(growth)

do "$dir_do/variable_update"

*sample selection 
drop if dag < 16

xtset idperson swv


* Set Excel file 

* Info sheet
putexcel set "$dir_results/reg_income", sheet("Info") replace
putexcel A1 = "Description:"
putexcel B1 = "This file contains regression estiamtes used by processes I3 (capital income), I4 (private pension, retired last year), I5 (private pension income, not retired last year) "
putexcel A2 = "Authors:	Patryk Bronka, Justin Van de Ven, Daria Popova" 
putexcel A3 = "Last edit: 1 July 2025 DP"

putexcel A4 = "Process:", bold
putexcel B4 = "Description:", bold
putexcel A5 = "Process I3a selection"
putexcel B5 = "Logit regression estimates of the probability of receiving capital income - aged 16+ in initial education spell"
putexcel A6 = "Process I3b selection"
putexcel B6 = "Logit regression estimates of the probability of receiving capital income - aged 16+ not in initial education spell"
putexcel A7 = "Process I3a amount"
putexcel B7 = "OLS regression estimates (log) capital income amount - aged 16+ in initial education spell and receive capital income"
putexcel A8 = "Process I3b amount"
putexcel B8 = "OLS regression estimates (log) capital income amount - not in initial education spell and receive capital income"
putexcel A9 = "Process I4b amount"
putexcel B9 = "OLS regression estimates (log) private pension income - aged 50+ and were retired last year, receive private pension income"
putexcel A10 = "Process I5a selection"
putexcel B10 = "Logit regression estimates of the probability of receiving private pension income - aged 50+ and not a student or retired last year"
putexcel A11 = "Process I5a amount"
putexcel B11 = "OLS regression estimates (log) private pension income - aged 50+ and not a student or retired last year"


putexcel A15 = "Notes:", bold
putexcel B15 = "All processes: replaced dhe with dhe_pcs and dhe_mcs, added ethnicity-4 cat (dot) and Covid dummies (y2020 y2021)"
putexcel B16 = "All processes: reverted to using stm instead of GDP growth"
putexcel B17 = "All processes for amounts: moved to log transformation"

/**********************************************************************
CAPITAL INCOME 
***********************************************************************/

*****************************************************************
*I3a selection: Probability of receiving capital income, in initial edu spell 
*****************************************************************
* Sample: All individuals 16+ that are in initial edu spell
* DV: Receiving capital income dummy
* Note: Capital income and employment income variables in IHS version 	

logit receives_ypncp i.dgn dag dagsq /*l.dhe*/ dhe_pcs_L1 dhe_mcs_L1 yplgrs_dv_L1 ypncp_L1 ib8.drgn1 stm y2020 y2021 i.dot ///
 if ded == 1 & dag >= 16 [pweight=dimxwt], ///
 vce(cluster idperson) base

* raw results 
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_raw_results/income/income_split", sheet("Process I3a_selection E") replace
putexcel A1 = matrix(results), names nformat(number_d2)
matrix i3a=get(VCE)
matrix list i3a
putexcel set "$dir_raw_results/income/income_split_vcm", sheet("Process I3a_selection VCE") replace
putexcel A1 = matrix(i3a), names
outreg2 stats(coef se pval) using "$dir_raw_results/income/I3a_sel.doc", replace ///
title("Process I3a selection: Probability of receiving capital income. Sample: Individuals aged 16+ who are in initial education spell.") ///
ctitle(Probability of capital income) label side dec(2) noparen addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll))

cap drop in_sample
gen in_sample = e(sample)	

predict p

save "$dir_validation_data/I3a_selection_sample", replace

scalar r2_p = e(r2_p) 
scalar N = e(N)	
scalar chi2 = e(chi2)
scalar ll = e(ll)	

* Results
* Note: Zeros values are eliminated 	
matrix b = e(b)	
matrix V = e(V)

* Store variance-covariance matrix 
preserve

putexcel set "$dir_raw_results/income/var_cov", sheet("var_cov") replace
putexcel A1 = matrix(V)

import excel "$dir_raw_results/income/var_cov", sheet("var_cov") clear

describe
local no_vars = `r(k)'	
	
forvalues i = 1/2 {
	egen row_sum = rowtotal(*)
	drop if row_sum == 0 
	drop row_sum
	xpose, clear	
}	
	
mkmat v*, matrix(var)	
putexcel set "$dir_results/reg_income", sheet("I3a_selection") modify
putexcel C2 = matrix(var)
		
restore	


* Store estimated coefficients 
// Initialize a counter for non-zero coefficients
local non_zero_count = 0
//local names : colnames b

* Loop through each element in `b` to count non-zero coefficients
forvalues i = 1/`no_vars' {
    if (b[1, `i'] != 0) {
        local non_zero_count = `non_zero_count' + 1
    }
}

* Create a new row vector to hold only non-zero coefficients
matrix nonzero_b = J(1, `non_zero_count', .)

* Populate nonzero_b with non-zero coefficients from b
local index = 1
forvalues i = 1/`no_vars' {
    if (b[1, `i'] != 0) {
        matrix nonzero_b[1, `index'] = b[1, `i']
        local index = `index' + 1
    }
}

putexcel set "$dir_results/reg_income", sheet("I3a_selection") modify
putexcel A1 = matrix(nonzero_b'), names nformat(number_d2) 
	
	
* Labelling 
// Need to variable label when add new variable to model. Order matters. 
local var_list Dgn Dag Dag_sq Dhe_pcs_L1 Dhe_mcs_L1 Yplgrs_dv_L1 Ypncp_L1 UKC UKD UKE UKF UKG UKH UKJ UKK UKL UKM UKN ///
	Year_transformed Y2020 Y2021 Ethn_Asian Ethn_Black Ethn_Other Constant 
	
putexcel A1 = "REGRESSOR"
putexcel B1 = "COEFFICIENT"
	
local i = 1 	
foreach var in `var_list' {
	local ++i
	
	putexcel A`i' = "`var'"
	
} 	

local i = 2 	
foreach var in `var_list' {
    local ++i

    if `i' <= 26 {
        local letter = char(64 + `i')  // Convert 1=A, 2=B, ..., 26=Z
        putexcel `letter'1 = "`var'"
    }
    else {
        local first = char(64 + int((`i' - 1) / 26))  // First letter: A-Z
        local second = char(65 + mod((`i' - 1), 26)) // Second letter: A-Z
        putexcel `first'`second'1 = "`var'"  // Correctly places AA-ZZ
    }
}
		
* Goodness of fit
putexcel set "$dir_results/reg_income", sheet("Gof") modify

putexcel A3 = ///
	"I3a selection - Receiving capital income in initial education spell ", ///
	bold		
	
putexcel A5 = "Pseudo R-squared" 
putexcel B5 = r2_p 
putexcel A6 = "N"
putexcel B6 = N 
putexcel E5 = "Chi^2"		
putexcel F5 = chi2
putexcel E6 = "Log likelihood"		
putexcel F6 = ll		

drop in_sample p
scalar drop r2_p N chi2 ll			


*********************************************************************
* I3b selection: Probability of receiving capital income, not in initial edu spell *
*********************************************************************
* Sample: All individuals 16+, not in initial edu spell
* DV: Receiving capital income dummy
* Note: Capital income and employment income variables in IHS version 	

logit receives_ypncp i.dgn dag dagsq ib1.deh_c3 li.les_c4 lib1.dhhtp_c4 /*l.dhe*/ dhe_pcs_L1 dhe_mcs_L1 ///
yplgrs_dv_L1 ypncp_L1 yplgrs_dv_L2 ypncp_L2 ib8.drgn1 stm /*c.growth*/ y2020 y2021 i.dot ///
 if ded == 0 [pweight=dimxwt], ///
 vce(cluster idperson) base

* raw results 
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_raw_results/income/income_split", sheet("Process I3b_selection E") replace
putexcel A1 = matrix(results), names nformat(number_d2)
matrix i3b=get(VCE)
matrix list i3b
putexcel set "$dir_raw_results/income/income_split_vcm", sheet("Process I3b_selection VCE") replace
putexcel A1 = matrix(i3b), names
outreg2 stats(coef se pval) using "$dir_raw_results/income/I3b_sel.doc", replace ///
title("Process I3b selection: Probability of receiving capital income. Sample: Individuals aged who are not in initial education spell.") ///
ctitle(Probability of capital income) label side dec(2) noparen addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll))

//cap drop in_sample
gen in_sample = e(sample)	

predict p

save "$dir_validation_data/I3b_selection_sample", replace

scalar r2_p = e(r2_p) 
scalar N = e(N)	
scalar chi2 = e(chi2)
scalar ll = e(ll)	

* Results
* Note: Zeros values are eliminated 	
matrix b = e(b)	
matrix V = e(V)

* Store variance-covariance matrix 
preserve

putexcel set "$dir_raw_results/income/var_cov", sheet("var_cov") replace
putexcel A1 = matrix(V)

import excel "$dir_raw_results/income/var_cov", sheet("var_cov") clear

describe
local no_vars = `r(k)'	
	
forvalues i = 1/2 {
	egen row_sum = rowtotal(*)
	drop if row_sum == 0 
	drop row_sum
	xpose, clear	
}	
	
mkmat v*, matrix(var)	
putexcel set "$dir_results/reg_income", sheet("I3b_selection") modify
putexcel C2 = matrix(var)
		
restore	


* Store estimated coefficients 
// Initialize a counter for non-zero coefficients
local non_zero_count = 0
//local names : colnames b

* Loop through each element in `b` to count non-zero coefficients
forvalues i = 1/`no_vars' {
    if (b[1, `i'] != 0) {
        local non_zero_count = `non_zero_count' + 1
    }
}

* Create a new row vector to hold only non-zero coefficients
matrix nonzero_b = J(1, `non_zero_count', .)

* Populate nonzero_b with non-zero coefficients from b
local index = 1
forvalues i = 1/`no_vars' {
    if (b[1, `i'] != 0) {
        matrix nonzero_b[1, `index'] = b[1, `i']
        local index = `index' + 1
    }
}

putexcel set "$dir_results/reg_income", sheet("I3b_selection") modify
putexcel A1 = matrix(nonzero_b'), names nformat(number_d2) 
	
	
* Labelling 
// Need to variable label when add new variable to model. Order matters. 

local var_list Dgn Dag Dag_sq Deh_c3_Medium Deh_c3_Low Les_c4_Student_L1 ///
	Les_c4_NotEmployed_L1 Les_c4_Retired_L1 Dhhtp_c4_CoupleChildren_L1 ///
	Dhhtp_c4_SingleNoChildren_L1 Dhhtp_c4_SingleChildren_L1 ///
	Dhe_pcs_L1 Dhe_mcs_L1 Yplgrs_dv_L1 Ypncp_L1 Yplgrs_dv_L2 Ypncp_L2 ///
	UKC UKD UKE UKF UKG UKH UKJ UKK UKL UKM UKN ///
	Year_transformed Y2020 Y2021 Ethn_Asian Ethn_Black Ethn_Other Constant
	
	
putexcel A1 = "REGRESSOR"
putexcel B1 = "COEFFICIENT"
	
local i = 1 	
foreach var in `var_list' {
	local ++i
	
	putexcel A`i' = "`var'"
	
} 	

local i = 2 	
foreach var in `var_list' {
    local ++i

    if `i' <= 26 {
        local letter = char(64 + `i')  // Convert 1=A, 2=B, ..., 26=Z
        putexcel `letter'1 = "`var'"
    }
    else {
        local first = char(64 + int((`i' - 1) / 26))  // First letter: A-Z
        local second = char(65 + mod((`i' - 1), 26)) // Second letter: A-Z
        putexcel `first'`second'1 = "`var'"  // Correctly places AA-ZZ
    }
}
		
* Goodness of fit
putexcel set "$dir_results/reg_income", sheet("Gof") modify

putexcel A9 = ///
	"I3b selection - Receiving capital income left initial education spell ", ///
	bold		
	
putexcel A11 = "Pseudo R-squared" 
putexcel B11 = r2_p 
putexcel A12 = "N"
putexcel B12 = N 
putexcel E11 = "Chi^2"		
putexcel F11 = chi2
putexcel E12 = "Log likelihood"		
putexcel F12 = ll		

drop in_sample p
scalar drop r2_p N chi2 ll		


*******************************************************
* I3a: Amount of capital income, in initial edu spell * 
*******************************************************
* Sample: All individuals 16+ that received capital income, in initial education spell
* DV: IHS of capital income 

regress ln_ypncp i.dgn dag dagsq /*l.dhe*/ dhe_pcs_L1 dhe_mcs_L1 yplgrs_dv_L1 ypncp_L1 ///
ib8.drgn1 stm /*c.growth*/ y2020 y2021 i.dot if dag >= 16 & receives_ypncp == 1 & ded == 1 ///
	[pweight = dimxwt], vce(cluster idperson) 

* raw results 	
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_raw_results/income/income_split", sheet("Process I3a_amount E") replace
putexcel A1 = matrix(results), names nformat(number_d2)
matrix i3a=get(VCE)
matrix list i3a
putexcel set "$dir_raw_results/income/income_split_vcm", sheet("Process I3a_amount VCE") replace
putexcel A1 = matrix(i3a), names
outreg2 stats(coef se pval) using "$dir_raw_results/income/I3a.doc", replace ///
title("Process I3a: Amount of capital income. Sample: Individuals aged 16+ who are in initial education spell abd receive capital income.") ///
 ctitle(Amount of capital income) label side dec(2) noparen addstat(R2, e(r2), RMSE, e(rmse))	
		
	
* Save sample inclusion indicator and predicted probabilities	
gen in_sample = e(sample)	
predict p 
gen sigma = e(rmse)

save "$dir_validation_data/I3a_level_sample", replace

scalar r2 = e(r2) 
scalar N = e(N)		
scalar rmse= e(rmse)

* Results 
* Note: Zeros values are eliminated 	
matrix b = e(b)	
matrix V = e(V)

* Store variance-covariance matrix 
preserve

putexcel set "$dir_raw_results/income/var_cov", sheet("var_cov") replace
putexcel A1 = matrix(V)

import excel "$dir_raw_results/income/var_cov", sheet("var_cov") clear

describe
local no_vars = `r(k)'	
	
forvalues i = 1/2 {
	egen row_sum = rowtotal(*)
	drop if row_sum == 0 
	drop row_sum
	xpose, clear	
}	
	
mkmat v*, matrix(var)	
putexcel set "$dir_results/reg_income", sheet("I3a_amount") modify
putexcel C2 = matrix(var)
		
restore	

* Store estimated coefficients 
// Initialize a counter for non-zero coefficients
local non_zero_count = 0
//local names : colnames b

* Loop through each element in `b` to count non-zero coefficients
forvalues i = 1/`no_vars' {
    if (b[1, `i'] != 0) {
        local non_zero_count = `non_zero_count' + 1
    }
}

* Create a new row vector to hold only non-zero coefficients
matrix nonzero_b = J(1, `non_zero_count', .)

* Populate nonzero_b with non-zero coefficients from b
local index = 1
forvalues i = 1/`no_vars' {
    if (b[1, `i'] != 0) {
        matrix nonzero_b[1, `index'] = b[1, `i']
        local index = `index' + 1
    }
}

putexcel set "$dir_results/reg_income", sheet("I3a_amount") modify
putexcel A1 = matrix(nonzero_b'), names nformat(number_d2) 		
 	
* Labelling 
// Need to variable label when add new variable to model. Order matters. 
local var_list Dgn Dag Dag_sq Dhe_pcs_L1 Dhe_mcs_L1 Yplgrs_dv_L1 Ypncp_L1 ///
UKC UKD UKE UKF UKG UKH UKJ UKK UKL UKM UKN ///
Year_transformed Y2020 Y2021 Ethn_Asian Ethn_Black Ethn_Other Constant
	
putexcel A1 = "REGRESSOR"
putexcel B1 = "COEFFICIENT"
	
local i = 1 	
foreach var in `var_list' {
	local ++i
	
	putexcel A`i' = "`var'"
	
} 	

local i = 2 	
foreach var in `var_list' {
    local ++i

    if `i' <= 26 {
        local letter = char(64 + `i')  // Convert 1=A, 2=B, ..., 26=Z
        putexcel `letter'1 = "`var'"
    }
    else {
        local first = char(64 + int((`i' - 1) / 26))  // First letter: A-Z
        local second = char(65 + mod((`i' - 1), 26)) // Second letter: A-Z
        putexcel `first'`second'1 = "`var'"  // Correctly places AA-ZZ
    }
}
		
* save RMSE
putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A6 = ("I3a") B6 = rmse 


* Goodness of fit
putexcel set "$dir_results/reg_income", sheet("Gof") modify

putexcel A15 = ///
	"I3a level - Receiving capital income in initial education spell ", ///
	bold		
	
putexcel A17 = "R-squared" 
putexcel B17 = r2 
putexcel A18 = "N"
putexcel B18 = N 

drop in_sample p sigma 
scalar drop r2 N 


***********************************************************
* I3b: Amount of capital income, not in initial edu spell * 
*********************************************************** 
* Sample: Individuals aged 16+ who are not in their initial education spell and 
* 	receive capital income.

regress ln_ypncp i.dgn dag dagsq ib1.deh_c3 li.les_c4 lib1.dhhtp_c4 /*l.dhe*/ dhe_pcs_L1 dhe_mcs_L1 ///
	yplgrs_dv_L1 ypncp_L1 yplgrs_dv_L2 ypncp_L2 ib8.drgn1 stm /*c.growth*/ y2020 y2021 i.dot ///
	if ded == 0 & receives_ypncp == 1 [pweight = dimxwt], ///
	vce(cluster idperson)
	
* raw results 	
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_raw_results/income/income_split", sheet("Process I3b_amount E") replace
putexcel A1 = matrix(results), names nformat(number_d2)
matrix i3b=get(VCE)
matrix list i3b
putexcel set "$dir_raw_results/income/income_split_vcm", sheet("Process I3b_amount VCE") replace
putexcel A1 = matrix(i3b), names
outreg2 stats(coef se pval) using "$dir_raw_results/income/I3b.doc", replace ///
title("Process I3b: Amount of capital income. Sample: Individuals aged 16+ who are not in initial education spell abd receive capital income.") ///
 ctitle(Amount of capital income) label side dec(2) noparen addstat(R2, e(r2), RMSE, e(rmse))	
		
	
* Save sample inclusion indicator and predicted probabilities	
gen in_sample = e(sample)	
predict p 
gen sigma = e(rmse)

save "$dir_validation_data/I3b_level_sample", replace

scalar r2 = e(r2) 
scalar N = e(N)	
scalar rmse= e(rmse)

* Results
* Note: Zeros values are eliminated 	
matrix b = e(b)	
matrix V = e(V)

* Store variance-covariance matrix 
preserve

putexcel set "$dir_raw_results/income/var_cov", sheet("var_cov") replace
putexcel A1 = matrix(V)

import excel "$dir_raw_results/income/var_cov", sheet("var_cov") clear

describe
local no_vars = `r(k)'	
	
forvalues i = 1/2 {
	egen row_sum = rowtotal(*)
	drop if row_sum == 0 
	drop row_sum
	xpose, clear	
}	
	
mkmat v*, matrix(var)	
putexcel set "$dir_results/reg_income", sheet("I3b_amount") modify
putexcel C2 = matrix(var)
		
restore	

* Store estimated coefficients 
// Initialize a counter for non-zero coefficients
local non_zero_count = 0
//local names : colnames b

* Loop through each element in `b` to count non-zero coefficients
forvalues i = 1/`no_vars' {
    if (b[1, `i'] != 0) {
        local non_zero_count = `non_zero_count' + 1
    }
}

* Create a new row vector to hold only non-zero coefficients
matrix nonzero_b = J(1, `non_zero_count', .)

* Populate nonzero_b with non-zero coefficients from b
local index = 1
forvalues i = 1/`no_vars' {
    if (b[1, `i'] != 0) {
        matrix nonzero_b[1, `index'] = b[1, `i']
        local index = `index' + 1
    }
}

putexcel set "$dir_results/reg_income", sheet("I3b_amount") modify
putexcel A1 = matrix(nonzero_b'), names nformat(number_d2) 		
	
* Labelling 
// Need to variable label when add new variable to model. Order matters. 
local var_list Dgn Dag Dag_sq Deh_c3_Medium Deh_c3_Low Les_c4_Student_L1 ///
	Les_c4_NotEmployed_L1 Les_c4_Retired_L1  Dhhtp_c4_CoupleChildren_L1 ///
	Dhhtp_c4_SingleNoChildren_L1  Dhhtp_c4_SingleChildren_L1 ///
	Dhe_pcs_L1 Dhe_mcs_L1 Yplgrs_dv_L1 Ypncp_L1 Yplgrs_dv_L2 Ypncp_L2 ///
	UKC UKD UKE UKF UKG UKH UKJ UKK UKL UKM UKN ///
    Year_transformed Y2020 Y2021 Ethn_Asian Ethn_Black Ethn_Other Constant
		
putexcel A1 = "REGRESSOR"
putexcel B1 = "COEFFICIENT"
	
local i = 1 	
foreach var in `var_list' {
	local ++i
	
	putexcel A`i' = "`var'"
	
} 	

local i = 2 	
foreach var in `var_list' {
    local ++i

    if `i' <= 26 {
        local letter = char(64 + `i')  // Convert 1=A, 2=B, ..., 26=Z
        putexcel `letter'1 = "`var'"
    }
    else {
        local first = char(64 + int((`i' - 1) / 26))  // First letter: A-Z
        local second = char(65 + mod((`i' - 1), 26)) // Second letter: A-Z
        putexcel `first'`second'1 = "`var'"  // Correctly places AA-ZZ
    }
}
	
* Save RMSE
putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A7 = ("I3b") B7 = rmse 


* Goodness of fit
putexcel set "$dir_results/reg_income", sheet("Gof") modify

putexcel A21 = ///
	"I3b level - Receiving capital income left initial education spell ", ///
	bold		
	
putexcel A23 = "R-squared" 
putexcel B23 = r2 
putexcel A24 = "N"
putexcel B24 = N 

drop in_sample p sigma 
scalar drop r2 N 


/**********************************************************************
PRIVATE PENSION INCOME
***********************************************************************/

***************************************************
*I4b: Amount of pension income. 
***************************************************
*Sample: Retired individuals who were retired in the previous year.

regress ln_ypnoab i.dgn dag dagsq ib1.deh_c3 lib1.dhhtp_c4 /*l.dhe*/ dhe_pcs_L1 dhe_mcs_L1 ///
ypnoab_L1 ypnoab_L2 ib8.drgn1 stm  /*c.growth*/ y2020 y2021 i.dot ///
if dag >= 50 & receives_ypnoab & dlrtrd==1 & l.dlrtrd==1 [pweight=dimxwt], ///
vce(cluster idperson) base


* raw results 	
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_raw_results/income/income_split", sheet("Process I4b_amount E") replace
putexcel A1 = matrix(results), names nformat(number_d2)
matrix i4b=get(VCE)
matrix list i4b
putexcel set "$dir_raw_results/income/income_split_vcm", sheet("Process I4b_amount VCE") replace
putexcel A1 = matrix(i4b), names
outreg2 stats(coef se pval) using "$dir_raw_results/income/I4b.doc", replace ///
title("Process I4b: Amount of private pension income. Sample: Individuals aged 50+ who were retired in the previous year and receive private pension income.") ///
 ctitle(Amount of private pension income) label side dec(2) noparen addstat(R2, e(r2), RMSE, e(rmse))	
				
	
* Save sample inclusion indicator and predicted probabilities	
gen in_sample = e(sample)	
predict p 
gen sigma = e(rmse)

save "$dir_validation_data/I4b_level_sample", replace

scalar r2 = e(r2) 
scalar N = e(N)	
scalar rmse= e(rmse)

* Results
* Note: Zeros values are eliminated 	
matrix b = e(b)	
matrix V = e(V)

* Store variance-covariance matrix 
preserve

putexcel set "$dir_raw_results/income/var_cov", sheet("var_cov") replace
putexcel A1 = matrix(V)

import excel "$dir_raw_results/income/var_cov", sheet("var_cov") clear

describe
local no_vars = `r(k)'	
	
forvalues i = 1/2 {
	egen row_sum = rowtotal(*)
	drop if row_sum == 0 
	drop row_sum
	xpose, clear	
}	
	
mkmat v*, matrix(var)	
putexcel set "$dir_results/reg_income", sheet("I4b_amount") modify
putexcel C2 = matrix(var)
		
restore	

* Store estimated coefficients 
// Initialize a counter for non-zero coefficients
local non_zero_count = 0
//local names : colnames b

* Loop through each element in `b` to count non-zero coefficients
forvalues i = 1/`no_vars' {
    if (b[1, `i'] != 0) {
        local non_zero_count = `non_zero_count' + 1
    }
}

* Create a new row vector to hold only non-zero coefficients
matrix nonzero_b = J(1, `non_zero_count', .)

* Populate nonzero_b with non-zero coefficients from b
local index = 1
forvalues i = 1/`no_vars' {
    if (b[1, `i'] != 0) {
        matrix nonzero_b[1, `index'] = b[1, `i']
        local index = `index' + 1
    }
}

putexcel set "$dir_results/reg_income", sheet("I4b_amount") modify
putexcel A1 = matrix(nonzero_b'), names nformat(number_d2) 		
	
* Labelling 
// Need to variable label when add new variable to model. Order matters. 
local var_list Dgn Dag Dag_sq Deh_c3_Medium Deh_c3_Low ///
	Dhhtp_c4_CoupleChildren_L1 	Dhhtp_c4_SingleNoChildren_L1  Dhhtp_c4_SingleChildren_L1 ///
	Dhe_pcs_L1 Dhe_mcs_L1 Ypnoab_L1 Ypnoab_L2 ///
	UKC UKD UKE UKF UKG UKH UKJ UKK UKL UKM UKN ///
    Year_transformed Y2020 Y2021 Ethn_Asian Ethn_Black Ethn_Other Constant
		

putexcel A1 = "REGRESSOR"
putexcel B1 = "COEFFICIENT"
	
local i = 1 	
foreach var in `var_list' {
	local ++i
	
	putexcel A`i' = "`var'"
	
} 	

local i = 2 	
foreach var in `var_list' {
    local ++i

    if `i' <= 26 {
        local letter = char(64 + `i')  // Convert 1=A, 2=B, ..., 26=Z
        putexcel `letter'1 = "`var'"
    }
    else {
        local first = char(64 + int((`i' - 1) / 26))  // First letter: A-Z
        local second = char(65 + mod((`i' - 1), 26)) // Second letter: A-Z
        putexcel `first'`second'1 = "`var'"  // Correctly places AA-ZZ
    }
}
	
* Save RMSE
putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A8 = ("I4b") B8 = rmse  


* Goodness of fit
putexcel set "$dir_results/reg_income", sheet("Gof") modify

putexcel A26 = ///
	"I4b level - Receiving private pension income: was retired last year", ///
	bold		
	
putexcel A27 = "R-squared" 
putexcel B27 = r2 
putexcel A28 = "N"
putexcel B28 = N 

drop in_sample p sigma 
scalar drop r2 N 


**************************************************************************
*I5a: Probability of receiving private pension income. 
**************************************************************************
*Sample: Retired individuals who were not retired in the previous year.
* DV: Receiving private pension income dummy
/*
Estimated on a sample of individuals retired at time t, who were not retired at t-1.
I.e. this is probability of receiving private pension income upon retirement. 
*/

logit receives_ypnoab i.dgn i.state_pension_age ib1.deh_c3 li.les_c4 lib1.dhhtp_c4 /*l.dhe*/ dhe_pcs_L1 dhe_mcs_L1 ///
l.pred_hourly_wage ib8.drgn1 stm /*c.growth*/  y2020 y2021 i.dot ///
if dag >= 50 & dlrtrd == 1 & l.dlrtrd!=1 & l.les_c4 != 2 [pweight=dimxwt], ///
vce(cluster idperson) base
 
* raw results 
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_raw_results/income/income_split", sheet("Process I5a_selection E") replace
putexcel A1 = matrix(results), names nformat(number_d2)
matrix i5a=get(VCE)
matrix list i5a
putexcel set "$dir_raw_results/income/income_split_vcm", sheet("Process I5a_selection VCE") replace
putexcel A1 = matrix(i5a), names
outreg2 stats(coef se pval) using "$dir_raw_results/income/I5a_sel.doc", replace ///
title("Process I5a selection: Probability of receiving capital income. Sample: Individuals aged 50+ who were not retired last year.") ///
ctitle(Probability receiving capital income) label side dec(2) noparen addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll))

//cap drop in_sample
gen in_sample = e(sample)	

predict p

save "$dir_validation_data/I5a_selection_sample", replace

scalar r2_p = e(r2_p) 
scalar N = e(N)	
scalar chi2 = e(chi2)
scalar ll = e(ll)	

* Results
* Note: Zeros values are eliminated 	
matrix b = e(b)	
matrix V = e(V)

* Store variance-covariance matrix 
preserve

putexcel set "$dir_raw_results/income/var_cov", sheet("var_cov") replace
putexcel A1 = matrix(V)

import excel "$dir_raw_results/income/var_cov", sheet("var_cov") clear

describe
local no_vars = `r(k)'	
	
forvalues i = 1/2 {
	egen row_sum = rowtotal(*)
	drop if row_sum == 0 
	drop row_sum
	xpose, clear	
}	
	
mkmat v*, matrix(var)	
putexcel set "$dir_results/reg_income", sheet("I5a_selection") modify
putexcel C2 = matrix(var)
		
restore	


* Store estimated coefficients 
// Initialize a counter for non-zero coefficients
local non_zero_count = 0
//local names : colnames b

* Loop through each element in `b` to count non-zero coefficients
forvalues i = 1/`no_vars' {
    if (b[1, `i'] != 0) {
        local non_zero_count = `non_zero_count' + 1
    }
}

* Create a new row vector to hold only non-zero coefficients
matrix nonzero_b = J(1, `non_zero_count', .)

* Populate nonzero_b with non-zero coefficients from b
local index = 1
forvalues i = 1/`no_vars' {
    if (b[1, `i'] != 0) {
        matrix nonzero_b[1, `index'] = b[1, `i']
        local index = `index' + 1
    }
}

putexcel set "$dir_results/reg_income", sheet("I5a_selection") modify
putexcel A1 = matrix(nonzero_b'), names nformat(number_d2) 
	
	
* Labelling 
// Need to variable label when add new variable to model. Order matters. 

local var_list Dgn StatePensionAge Deh_c3_Medium Deh_c3_Low ///
	Les_c4_NotEmployed_L1 ///
	Dhhtp_c4_CoupleChildren_L1 	Dhhtp_c4_SingleNoChildren_L1 Dhhtp_c4_SingleChildren_L1 ///
	Dhe_pcs_L1 Dhe_mcs_L1 Hourly_wage_L1  ///
	UKC UKD UKE UKF UKG UKH UKJ UKK UKL UKM UKN ///
	Year_transformed Y2020 Y2021 Ethn_Asian Ethn_Black Ethn_Other Constant


putexcel A1 = "REGRESSOR"
putexcel B1 = "COEFFICIENT"
	
local i = 1 	
foreach var in `var_list' {
	local ++i
	
	putexcel A`i' = "`var'"
	
} 	

local i = 2 	
foreach var in `var_list' {
    local ++i

    if `i' <= 26 {
        local letter = char(64 + `i')  // Convert 1=A, 2=B, ..., 26=Z
        putexcel `letter'1 = "`var'"
    }
    else {
        local first = char(64 + int((`i' - 1) / 26))  // First letter: A-Z
        local second = char(65 + mod((`i' - 1), 26)) // Second letter: A-Z
        putexcel `first'`second'1 = "`var'"  // Correctly places AA-ZZ
    }
}
		
* Goodness of fit
putexcel set "$dir_results/reg_income", sheet("Gof") modify

putexcel A30 = ///
	"I5a selection - Receiving private pension income: was not retited last year", ///
	bold		
	
putexcel A32 = "Pseudo R-squared" 
putexcel B32 = r2_p 
putexcel A33 = "N"
putexcel B33 = N 
putexcel E32 = "Chi^2"		
putexcel F32 = chi2
putexcel E33 = "Log likelihood"		
putexcel F33 = ll		

drop in_sample p
scalar drop r2_p N chi2 ll		



****************************************************
*I5a: Amount of private pension income. 
****************************************************
*Sample: Retired individuals who were not retired in the previous year and receive private pension income.

regress ln_ypnoab i.dgn dag dagsq /*i.state_pension_age*/ ib1.deh_c3 li.les_c4 lib1.dhhtp_c4 /*l.dhe*/ dhe_pcs_L1 dhe_mcs_L1 ///
l.pred_hourly_wage ib8.drgn1 stm /*c.growth*/ y2020 y2021 i.dot ///
if  dag >= 50 & dlrtrd == 1 & l.dlrtrd!=1 & l.les_c4 != 2 & receives_ypnoab [pweight=dimxwt], ///
vce(cluster idperson) base

* raw results 	
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_raw_results/income/income_split", sheet("Process I5a_amount E") replace
putexcel A1 = matrix(results), names nformat(number_d2)
matrix i5a=get(VCE)
matrix list i5a
putexcel set "$dir_raw_results/income/income_split_vcm", sheet("Process I5a_amount VCE") replace
putexcel A1 = matrix(i5a), names
outreg2 stats(coef se pval) using "$dir_raw_results/income/I5a.doc", replace ///
title("Process I5a: Amount of private pension income. Sample: Individuals aged 50+ who were not retired in the previous year and receive private pension income.") ///
 ctitle(Amount of private pension income) label side dec(2) noparen addstat(R2, e(r2), RMSE, e(rmse))	
				
	
* Save sample inclusion indicator and predicted probabilities	
gen in_sample = e(sample)	
predict p 
gen sigma = e(rmse)

save "$dir_validation_data/I5a_level_sample", replace

scalar r2 = e(r2) 
scalar N = e(N)	
scalar rmse= e(rmse)

* Results
* Note: Zeros values are eliminated 	
matrix b = e(b)	
matrix V = e(V)

* Store variance-covariance matrix 
preserve

putexcel set "$dir_raw_results/income/var_cov", sheet("var_cov") replace
putexcel A1 = matrix(V)

import excel "$dir_raw_results/income/var_cov", sheet("var_cov") clear

describe
local no_vars = `r(k)'	
	
forvalues i = 1/2 {
	egen row_sum = rowtotal(*)
	drop if row_sum == 0 
	drop row_sum
	xpose, clear	
}	
	
mkmat v*, matrix(var)	
putexcel set "$dir_results/reg_income", sheet("I5a_amount") modify
putexcel C2 = matrix(var)
		
restore	

* Store estimated coefficients 
// Initialize a counter for non-zero coefficients
local non_zero_count = 0
//local names : colnames b

* Loop through each element in `b` to count non-zero coefficients
forvalues i = 1/`no_vars' {
    if (b[1, `i'] != 0) {
        local non_zero_count = `non_zero_count' + 1
    }
}

* Create a new row vector to hold only non-zero coefficients
matrix nonzero_b = J(1, `non_zero_count', .)

* Populate nonzero_b with non-zero coefficients from b
local index = 1
forvalues i = 1/`no_vars' {
    if (b[1, `i'] != 0) {
        matrix nonzero_b[1, `index'] = b[1, `i']
        local index = `index' + 1
    }
}

putexcel set "$dir_results/reg_income", sheet("I5a_amount") modify
putexcel A1 = matrix(nonzero_b'), names nformat(number_d2) 		
	
* Labelling 
// Need to variable label when add new variable to model. Order matters. 
local var_list Dgn Dag Dag_sq Deh_c3_Medium Deh_c3_Low ///
	Les_c4_NotEmployed_L1 Dhhtp_c4_CoupleChildren_L1 Dhhtp_c4_SingleNoChildren_L1  Dhhtp_c4_SingleChildren_L1 ///
	Dhe_pcs_L1 Dhe_mcs_L1 Hourly_wage_L1 ///
	UKC UKD UKE UKF UKG UKH UKJ UKK UKL UKM UKN ///
    Year_transformed Y2020 Y2021 Ethn_Asian Ethn_Black Ethn_Other Constant
		

putexcel A1 = "REGRESSOR"
putexcel B1 = "COEFFICIENT"
	
local i = 1 	
foreach var in `var_list' {
	local ++i
	
	putexcel A`i' = "`var'"
	
} 	

local i = 2 	
foreach var in `var_list' {
    local ++i

    if `i' <= 26 {
        local letter = char(64 + `i')  // Convert 1=A, 2=B, ..., 26=Z
        putexcel `letter'1 = "`var'"
    }
    else {
        local first = char(64 + int((`i' - 1) / 26))  // First letter: A-Z
        local second = char(65 + mod((`i' - 1), 26)) // Second letter: A-Z
        putexcel `first'`second'1 = "`var'"  // Correctly places AA-ZZ
    }
}
	
* Save RMSE
putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A9 = ("I5a") B9 = rmse  


* Goodness of fit
putexcel set "$dir_results/reg_income", sheet("Gof") modify

putexcel A35 = ///
	"I5a level - Receiving private pension income: was not retired last year", ///
	bold		
	
putexcel A37 = "R-squared" 
putexcel B37 = r2 
putexcel A38 = "N"
putexcel B38 = N 

drop in_sample p sigma 
scalar drop r2 N 


//end 

capture log close 

graph drop _all 
