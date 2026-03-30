*******************************************************************************************
* PROJECT:  		SimPaths UK
* SECTION:			Wage regression 
* OBJECT: 			Heckman regressions 
* AUTHORS:			Patryk Bronka, Daria Popova, Justin van de Ven, Aleksandra Kolndrekaj
* LAST UPDATE:		18 Feb 2026 AK 
******************************************************************************************
****************************************************************************************** 
* NOTES: 			Strategy:  
* 					1) Heckman estimated on the sub-sample of individuals 
* 						who are not observed working in previous period. 
*   					=> Wage equation does not controls for lagged wage
* 					2) Heckman estimated on the sub-sample of individuals who 
* 						are observed working in previous period. 
*    					=> Wage equation controls for lagged wage
* 					Specification of selection equation is the same in the 
* 						two samples
* 					
* 					Import labour cost index to create a measure of wage growth. 
* 					Make sure loaded into the external_data subfolder. 
* 
* 					Update the winsorization process if alter data 
* 					 
*******************************************************************************/
clear all
set more off
set mem 200m
set type double
//set maxvar 120000
set maxvar 30000

*******************************************************************
cap log close 
log using "${dir_log}/reg_wages.log", replace
*******************************************************************

*****************************************************************************************************************************
* Set Excel file 
* Info sheet - first stage 
putexcel set "$dir_results/reg_employment_selection", sheet("Info") replace
putexcel A1 = "Description:"
putexcel B1 = "This file contains regression estimates from the first stage of the Heckman selection model used to estimates wages."
putexcel A2 = "Authors:	Patryk Bronka, Justin Van de Ven, Daria Popova, Aleksandra Kolndrekaj" 
putexcel A3 = "Last edit: 18 Feb 2026 AK"

putexcel A5 = "Process:", bold
putexcel B5 = "Description:", bold
putexcel A6 = "W1fa-sel"
putexcel B6 = "First stage Heckman selection estimates for women that do not have an observed wage in the previous year"
putexcel A7 = "W1ma-sel"
putexcel B7 = "First stage Heckman selection estimates for women that do not have an observed wage in the previous year"
putexcel A8 = "W1fb-sel"
putexcel B8 = "First stage Heckman selection estimates for women that have an observed wage in the previous year"
putexcel A9 = "W1mb-sel"
putexcel B9 = "First stage Heckman selection estimates for men that have an observed wage in the previous year"

putexcel A11 = "Notes:", bold
putexcel B11 = "Estimated on panel data unlike the labour supply estimates"
putexcel B12 = "Predicted wages used as input into union parameters and income process estimates"
putexcel B13 = "Two-step Heckman command is used which does not permit weights"

* Info sheet - second stage 
putexcel set "$dir_results/reg_wages", sheet("Info") replace
putexcel A1 = "Description:"
putexcel B1 = "This file contains regression estimates used to calculate potential wages for males and females in the simulation."
putexcel A2 = "Authors:	Patryk Bronka, Daria Popova, Aleksandra Kolndrekaj" 
putexcel A3 = "Last edit: 18 Feb 2026 AK"

putexcel A4 = "Process:", bold
putexcel B4 = "Description:", bold
putexcel A5 = "Process:", bold
putexcel B5 = "Description:", bold
putexcel A6 = "W1fa"
putexcel B6 = "Second stage Heckman selection estimates using women that do not have an observed wage in the previous year"
putexcel A7 = "W1ma"
putexcel B7 = "Second stage Heckman selection estimates using men that do not have an observed wage in the previous year"
putexcel A8 = "W1fb"
putexcel B8 = "Second stage Heckman selection estimates using women that have an observed wage in the previous year"
putexcel A9 = "W1mb"
putexcel B9 = "Second stage Heckman selection estimates using men that have an observed wage in the previous year"

putexcel A11 = "Notes:", bold
putexcel B11 = "Estimation sample: UK_ipop.dta. Two-step Heckman command is used which does not permit weights" 
putexcel B12 = "Conditions for processes are defined as globals in master.do"
putexcel B13 = "Predicted wages sre saved in dataset UK_ipop2.dta and used as input into union parameters and income process estimates"

/**************************************************************/
*	prepare data on real growth of wages 
/**************************************************************/
 
import excel "$dir_external_data/time_series_factor.xlsx", sheet("UK_wage_growth") firstrow clear // Import real wage growth rates
rename Year stm
rename Value real_wage_growth
replace stm = stm - 2000
sum real_wage_growth if stm == 15
gen base = r(mean)
replace real_wage_growth = real_wage_growth / base // Note: switching from 100 base to 1 base as that's what happens in the simulation when rebasing indices
drop base
save "$dir_external_data/wage_growth_rates", replace

/********************************* PREPARE DATA *******************************/

* Load data 
use "${estimation_sample}", clear

* Set data 
xtset idperson swv
sort idperson swv 

* Adjust variables 
do "${dir_do}/variable_update.do"

* merge in real growth index 
merge m:1 stm using "$dir_external_data/wage_growth_rates", keep(3) nogen keepusing(real_wage_growth)

* Hours work per week 
gen hours = 0
replace hours = lhw if ((lhw > 0) & (lhw < .))
label var hours "Hours worked per week"

* Hourly wage	
gen wage_hour = obs_earnings_hourly
	
* Winsorize
sum wage_hour, det
replace wage_hour = . if wage_hour <= 0
replace wage_hour = . if wage_hour >= r(p99)

gen lwage_hour = ln(wage_hour)
label var lwage_hour "Log gross hourly wage"

gen lwage_hour_2 = lwage_hour^2
label var lwage_hour_2 "Squared log gross hourly wage"


* relationship status (1=cohabitating)
gen mar = (dcpst==1)

* children
gen any02 = dnc02 > 0

gen dnc4p = dnc
replace dnc4p = 1 if (dnc>4)

gen dnc2p = dnc
replace dnc2p = 2 if (dnc>2)

cap drop child 
gen child = (dnc>0)

*employment status in previous wave  
sort idperson swv 
gen L1les_c3 = L1.les_c3

*part time work 
gen pt = (hours >  0) * (hours <= 25)

* Flag to identify observations to be included in the estimation sample 
* Need to have been observed at least once in the past and activity information 
* is not missing in the previous observation 
bys idperson (swv): gen obs_count_ttl = _N
bys idperson (swv): gen obs_count = _n

gen in_sample = (obs_count_ttl > 1 & obs_count > 1) 
replace in_sample = 0 if swv != swv[_n-1] +1 & idperson == idperson[_n-1]
replace in_sample = 0 if les_c3 == . | obs_earning == . 
fre in_sample 


* Flag to distinguish the two samples (prev work and not)
capture drop previouslyWorking
gen previouslyWorking = (L1.lwage_hour != .) 
replace previouslyWorking = . if in_sample == 0 
fre previouslyWorking


* Prep storage 
capture drop lwage_hour_hat wage_hour_hat esample
gen lwage_hour_hat = .
gen wage_hour_hat = .
gen esample = .
gen pred_hourly_wage = .

/********************************** ESTIMATION ********************************/

/******************** WAGES: WOMEN, NO PREV WAGE OBSERVED *********************/

* Estimate a predicted wage using a Heckman selection model 
* Sample: Working age (16-75) women who did not receive a wage in t-1
* DV: Log gross hourly wage 

global wage_eqn "lwage_hour dag dagsq ib1.deh_c4 ib1.deh_c4#c.dag i.dehmf_c3 dlltsd01 l.dhe_pcs l.dhe_mcs ib8.drgn1 pt real_wage_growth y2020 y2021 i.dot" //ded
global seln_eqn "i.L1les_c3 dag dagsq ib1.deh_c4 ib1.deh_c4#c.dag i.dehmf_c3 mar child dlltsd01 l.dhe_pcs l.dhe_mcs ib8.drgn1 y2020 y2021 i.dot" //ded

local filter = "${wages_f_no_prev_if_condition}"
display "`filter'"

heckman $wage_eqn if `filter', select($seln_eqn) twostep mills(lambda) 

outreg2 stats(coef se pval) using "$dir_raw_results/wages/Output_NWW.doc", replace ///
title("Heckman-corrected wage equation estimated on the sample of women who were not in employment last year") ///
 ctitle(Not working women) label side dec(2) noparen 
  
/***************************************************************************/
* Eigenvalue stability check 

* Extract variance-covariance matrix
matrix V = e(V)

* Preserve data state
preserve

* Export V to dataset
clear
svmat double V

* Drop zero rows and columns
forvalues r = 1/2 {
    egen rowsum = rowtotal(*)
    drop if rowsum == 0
    drop rowsum
    xpose, clear
}

* Recreate trimmed VCV matrix
mkmat *, matrix(V_trimmed)

restore

* Eigen decomposition
matrix symeigen X lambda = V_trimmed

* Largest eigenvalue
scalar max_eig = lambda[1,1]

* Smallest-to-largest eigenvalue ratio
scalar min_ratio = lambda[1, colsof(lambda)] / max_eig

* Check 1: near singularity
if max_eig < 1.0e-12 {
    display as error "CRITICAL ERROR: Heckman VCV near singular"
    display as error "Max eigenvalue = " max_eig
    exit 999
}

* Check 2: ill-conditioning
if min_ratio < 1.0e-12 {
    display as error "ERROR: Heckman VCV ill-conditioned"
    display as error "Min/Max eigenvalue ratio = " min_ratio
    exit 506
}

display "VCV stability check passed"
display "Max eigenvalue: " max_eig
display "Min/Max ratio: " min_ratio

/***************************************************************************/
  
* Obtain predicted values (log wage) with selection correction

predict pred if `filter', ycond  // ycond -> include IMR in prediction to account for selection into employment
replace lwage_hour_hat = pred if `filter'

gen in_sample_fnpw = e(sample)	

* Correct bias when transforming from log to levels 
cap drop epsilon
gen epsilon = rnormal()*e(sigma) 
replace pred_hourly_wage = exp(lwage_hour_hat + epsilon) if `filter' 


twoway (hist wage_hour if `filter', width(0.5) ///
	lcolor(gs12) fcolor(gs12)) ///
	(hist pred_hourly_wage if `filter' & (!missing(wage_hour)), width(0.5) ///
		fcolor(none) lcolor(red)), ///
	title("Gross Hourly Wage (Level)") ///
	subtitle("Females, No previously observed wage") ///
	xtitle("GBP") ///
	legend(lab(1 "UKHLS") lab(2 "Prediction")) ///
	note("Notes: Sample condition ${wages_f_no_prev_if_condition}", size(vsmall))	

graph export "${dir_raw_results}/wages/W1fa_hist.png", replace

graph drop _all 

sum wage_hour if `filter' [aw=dwt]
sum pred_hourly_wage if `filter' & (!missing(wage_hour)) [aw=dwt]
 
* Save sample validation 
save "$dir_validation_data/Female_NPW_sample", replace 
	
cap drop pred epsilon	
 
 
* Formatted results
* Clean up matrix of estimates 
* Note: Zeros values are eliminated 
matrix b = e(b)	
matrix V = e(V)

* Store variance-covariance matrix 
preserve

putexcel set "$dir_raw_results/wages/var_cov", sheet("var_cov") replace
putexcel A1 = matrix(V)

import excel "$dir_raw_results/wages/var_cov", sheet("var_cov") clear

describe
local no_vars = `r(k)'	
	
forvalues i = 1/2 {
	egen row_sum = rowtotal(*)
	drop if row_sum == 0 
	drop row_sum
	xpose, clear	
}	
	
mkmat v*, matrix(var)	

* Second stage
putexcel set "$dir_raw_results/wages/reg_wages", sheet("Females_NLW") replace
putexcel C2 = matrix(var)
		
restore	

* Store estimated coefficients 
* Initialize a counter for non-zero coefficients
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

putexcel set "$dir_raw_results/wages/reg_wages", sheet("Females_NLW") modify
putexcel A1 = matrix(nonzero_b'), names //nformat(number_d2) 

preserve

import excel "$dir_raw_results/wages/reg_wages", sheet("Females_NLW") firstrow ///
	clear
ds 
//define which cells are to be dropped 
drop if C == 0 // UPDATE 
drop A 
drop AG-BL // UPDATE


mkmat *, matrix(Females_NLW)
putexcel set "$dir_results/reg_wages", sheet("W1fa") modify 
putexcel B2 = matrix(Females_NLW)

restore 


* Labelling 
putexcel set "$dir_results/reg_wages", ///
	sheet("W1fa") modify 

local var_list Dag Dag_sq Deh_c4_Medium Deh_c4_Low Deh_c4_Medium_Dag ///
	Deh_c4_Low_Dag Dehmf_c3_Medium Dehmf_c3_Low Dlltsd01 Dhe_pcs_L1 Dhe_mcs_L1 ///
	UKC UKD UKE UKF UKG UKH UKJ UKK UKL UKM UKN Pt RealWageGrowth Y2020 Y2021 ///
	Ethn_Asian Ethn_Black Ethn_Other  Constant InverseMillsRatio

	
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


* First stage
preserve

import excel "$dir_raw_results/wages/reg_wages", sheet("Females_NLW") firstrow ///
	clear
ds 

drop if AG == 0 // UPDATE
drop A 
drop C-AF // UPDATE
drop BM // UPDATE


mkmat *, matrix(Females_NLW)
putexcel set "$dir_results/reg_employment_selection", ///
	sheet("W1fa-sel") modify 
putexcel B2 = matrix(Females_NLW)

restore 

* Labelling 
putexcel set "$dir_results/reg_employment_selection", sheet("W1fa-sel") modify 
	
local var_list Les_c3_Student_L1 Les_c3_NotEmployed_L1 Dag Dag_sq Deh_c4_Medium Deh_c4_Low Deh_c4_Medium_Dag ///
	Deh_c4_Low_Dag  Dehmf_c3_Medium Dehmf_c3_Low Dcpst_Partnered D_Children Dlltsd01 Dhe_pcs_L1 Dhe_mcs_L1  ///
	UKC UKD UKE UKF UKG UKH UKJ UKK UKL UKM UKN Y2020 Y2021 ///
	Ethn_Asian Ethn_Black Ethn_Other Constant 
	

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

cap drop lambda


* Calculate RMSE 
cap drop residuals squared_residuals  
gen residuals = lwage_hour - lwage_hour_hat
gen squared_residuals = residuals^2

preserve 
keep if `filter'
sum squared_residuals 
di "RMSE for Not employed women:  " sqrt(r(mean))
putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A1=("REGRESSOR") B1=("COEFFICIENT") ///
A2=("W1fa") B2=(sqrt(r(mean))) 
restore 


/******************** WAGES: MEN, NO PREV WAGE OBSERVED *********************/

* Estimate a predicted wage using a Heckman selection model 
* Sample: Working age (16-75) men who did not receive a wage in t-1
* DV: Log gross hourly wage 

global wage_eqn "lwage_hour dag dagsq ib1.deh_c4 ib1.deh_c4#c.dag i.dehmf_c3 dlltsd01 l.dhe_pcs l.dhe_mcs  ib8.drgn1 pt real_wage_growth y2020 y2021 i.dot" //ded 
global seln_eqn "i.L1les_c3 dag dagsq ib1.deh_c4 ib1.deh_c4#c.dag i.dehmf_c3 mar child dlltsd01 l.dhe_pcs l.dhe_mcs ib8.drgn1 y2020 y2021 i.dot" //ded 

local filter = "${wages_m_no_prev_if_condition}"
display "`filter'"

heckman $wage_eqn if `filter', select($seln_eqn) twostep mills(lambda) 

outreg2 stats(coef se pval) using "$dir_raw_results/wages/Output_NWM.doc", replace ///
title("Heckman-corrected wage equation estimated on the sample of men who were not in employment last year") ///
 ctitle(Not working men) label side dec(2) noparen 

/***************************************************************************/
* Eigenvalue stability check 

* Extract variance-covariance matrix
matrix V = e(V)

* Preserve data state
preserve

* Export V to dataset
clear
svmat double V

* Drop zero rows and columns
forvalues r = 1/2 {
    egen rowsum = rowtotal(*)
    drop if rowsum == 0
    drop rowsum
    xpose, clear
}

* Recreate trimmed VCV matrix
mkmat *, matrix(V_trimmed)

restore

* Eigen decomposition
matrix symeigen X lambda = V_trimmed

* Largest eigenvalue
scalar max_eig = lambda[1,1]

* Smallest-to-largest eigenvalue ratio
scalar min_ratio = lambda[1, colsof(lambda)] / max_eig

* Check 1: near singularity
if max_eig < 1.0e-12 {
    display as error "CRITICAL ERROR: Heckman VCV near singular"
    display as error "Max eigenvalue = " max_eig
    exit 999
}

* Check 2: ill-conditioning
if min_ratio < 1.0e-12 {
    display as error "ERROR: Heckman VCV ill-conditioned"
    display as error "Min/Max eigenvalue ratio = " min_ratio
    exit 506
}

display "VCV stability check passed"
display "Max eigenvalue: " max_eig
display "Min/Max ratio: " min_ratio

/***************************************************************************/ 
 
 
* Obtain predicted values (log wage) with selection correction
predict pred if `filter', ycond 	// ycond -> include IMR in prediction to account for selection into employment
replace lwage_hour_hat = pred if `filter'

gen in_sample_mnpw = e(sample)	

* Correct bias transforming from log to levels 
gen epsilon = rnormal()*e(sigma) 

replace pred_hourly_wage = exp(lwage_hour_hat + epsilon) if `filter' 
 
twoway (hist wage_hour if `filter', width(0.5) ///
	lcolor(gs12) fcolor(gs12)) ///
	(hist pred_hourly_wage if `filter' & (!missing(wage_hour)), width(0.5) ///
		fcolor(none) lcolor(red)), ///
	title("Gross Hourly Wage (Level)") ///
	subtitle("Males, No previously observed wage") ///	
	xtitle("GBP") ///
	legend(lab(1 "UKHLS") lab(2 "Prediction")) ///
	note("Notes: Sample condition ${wages_m_no_prev_if_condition}", size(vsmall))	

graph export "${dir_raw_results}/wages/W1ma_hist.png", replace

graph drop _all 

sum wage_hour if `filter' [aw=dwt]
sum pred_hourly_wage if `filter' & (!missing(wage_hour)) [aw=dwt] 
 

* Save sample for validation
save "$dir_validation_data/Male_NPW_sample", replace 
cap drop pred epsilon


* Formatted results
* Clean up matrix of estimates 
* Note: Zeros values are eliminated 
matrix b = e(b)	
matrix V = e(V)

* Store variance-covariance matrix 
preserve

putexcel set "$dir_raw_results/wages/var_cov", sheet("var_cov") replace
putexcel A1 = matrix(V)

import excel "$dir_raw_results/wages/var_cov", sheet("var_cov") clear

describe
local no_vars = `r(k)'	
	
forvalues i = 1/2 {
	egen row_sum = rowtotal(*)
	drop if row_sum == 0 
	drop row_sum
	xpose, clear	
}	
	
mkmat v*, matrix(var)	

* Second stage
putexcel set "$dir_raw_results/wages/reg_wages", sheet("Males_NLW") replace
putexcel C2 = matrix(var)
		
restore	

* Store estimated coefficients 
* Initialize a counter for non-zero coefficients
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

putexcel set "$dir_raw_results/wages/reg_wages", sheet("Males_NLW") modify
putexcel A1 = matrix(nonzero_b'), names //nformat(number_d2) 

preserve

import excel "$dir_raw_results/wages/reg_wages", sheet("Males_NLW") firstrow ///
	clear
ds 

drop if C == 0 // UPDATE 
drop A 
drop AG-BL // UPDATE



mkmat *, matrix(Males_NLW)
putexcel set "$dir_results/reg_wages", ///
	sheet("W1ma") modify 
putexcel B2 = matrix(Males_NLW)

restore 

* Labelling 
putexcel set "$dir_results/reg_wages", ///
	sheet("W1ma") modify 

local var_list Dag Dag_sq Deh_c4_Medium Deh_c4_Low Deh_c4_Medium_Dag ///
	Deh_c4_Low_Dag Dehmf_c3_Medium Dehmf_c3_Low Dlltsd01 Dhe_pcs_L1 Dhe_mcs_L1  ///
	UKC UKD UKE UKF UKG UKH UKJ UKK UKL UKM UKN Pt RealWageGrowth Y2020 Y2021 ///
	Ethn_Asian Ethn_Black Ethn_Other  Constant InverseMillsRatio

	
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


* First stage
preserve

import excel "$dir_raw_results/wages/reg_wages", sheet("Males_NLW") firstrow ///
	clear
ds 

drop if AG == 0 // UPDATE
drop A 
drop C-AF // UPDATE
drop BM // UPDATE


mkmat *, matrix(Males_NLW)
putexcel set "$dir_results/reg_employment_selection", ///
	sheet("W1ma-sel") modify 
putexcel B2 = matrix(Males_NLW)

restore 

* Labelling 
putexcel set "$dir_results/reg_employment_selection", ///
	sheet("W1ma-sel") modify 
	
local var_list Les_c3_Student_L1 Les_c3_NotEmployed_L1 Dag Dag_sq Deh_c4_Medium Deh_c4_Low Deh_c4_Medium_Dag ///
	Deh_c4_Low_Dag Dehmf_c3_Medium Dehmf_c3_Low Dcpst_Partnered D_Children Dlltsd01 Dhe_pcs_L1 Dhe_mcs_L1  ///
	UKC UKD UKE UKF UKG UKH UKJ UKK UKL UKM UKN Y2020 Y2021 ///
	Ethn_Asian Ethn_Black Ethn_Other Constant 
	

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

cap drop lambda

* Calculate RMSE 
cap drop residuals squared_residuals  
gen residuals = lwage_hour - lwage_hour_hat
gen squared_residuals = residuals^2

preserve 
keep if `filter'
sum squared_residuals 
di "RMSE for Not employed men:  " sqrt(r(mean))
putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A1=("REGRESSOR") B1=("COEFFICIENT") ///
A3=("W1ma") B3=(sqrt(r(mean))) 
restore 


/******************** WAGES: WOMEN, PREV WAGE OBSERVED *********************/

* Estimate a predicted wage using a Heckman selection model 
* Sample: Working age (16-75) women who received a wage in t-1
* DV: Log gross hourly wage 

global wage_eqn "lwage_hour L1.lwage_hour dag dagsq ib1.deh_c4 ib1.deh_c4#c.dag  i.dehmf_c3 dlltsd01 l.dhe_pcs l.dhe_mcs  ib8.drgn1 pt real_wage_growth y2020 y2021 i.dot" //ded
global seln_eqn "dag dagsq ib1.deh_c4 ib1.deh_c4#c.dag i.dehmf_c3 mar child dlltsd01 l.dhe_pcs l.dhe_mcs ib8.drgn1 y2020 y2021 i.dot" //ded

local filter = "${wages_f_prev_if_condition}"
display "`filter'"

heckman $wage_eqn if `filter', select($seln_eqn) twostep mills(lambda) 

outreg2 stats(coef se pval) using "$dir_raw_results/wages/Output_WW.doc", replace ///
title("Heckman-corrected wage equation estimated on the sample of women who were in employment last year") ///
 ctitle(Working women) label side dec(2) noparen 
 
 /***************************************************************************/
* Eigenvalue stability check 

* Extract variance-covariance matrix
matrix V = e(V)

* Preserve data state
preserve

* Export V to dataset
clear
svmat double V

* Drop zero rows and columns
forvalues r = 1/2 {
    egen rowsum = rowtotal(*)
    drop if rowsum == 0
    drop rowsum
    xpose, clear
}

* Recreate trimmed VCV matrix
mkmat *, matrix(V_trimmed)

restore

* Eigen decomposition
matrix symeigen X lambda = V_trimmed

* Largest eigenvalue
scalar max_eig = lambda[1,1]

* Smallest-to-largest eigenvalue ratio
scalar min_ratio = lambda[1, colsof(lambda)] / max_eig

* Check 1: near singularity
if max_eig < 1.0e-12 {
    display as error "CRITICAL ERROR: Heckman VCV near singular"
    display as error "Max eigenvalue = " max_eig
    exit 999
}

* Check 2: ill-conditioning
if min_ratio < 1.0e-12 {
    display as error "ERROR: Heckman VCV ill-conditioned"
    display as error "Min/Max eigenvalue ratio = " min_ratio
    exit 506
}

display "VCV stability check passed"
display "Max eigenvalue: " max_eig
display "Min/Max ratio: " min_ratio

/***************************************************************************/
 
 * Obtain predicted values (log wage) with selection correction
predict pred if `filter', ycond // ycond -> include IMR in prediction 
replace lwage_hour_hat = pred if `filter'

gen in_sample_fpw = 1 if e(sample) == 1

* Correct bias transforming from log to levels 
gen epsilon = rnormal()* e(sigma) 
replace pred_hourly_wage = exp(lwage_hour_hat + epsilon) if `filter'	

twoway (hist wage_hour if `filter', width(0.5) ///
	lcolor(gs12) fcolor(gs12)) ///
	(hist pred_hourly_wage if `filter' & (!missing(wage_hour)), width(0.5) ///
		fcolor(none) lcolor(red)), ///
	title("Gross Hourly Wage (Level)") ///
	subtitle("Females, Previously observed wage") ///	
	xtitle("GBP") ///
	legend(lab(1 "UKHLS") lab(2 "Prediction")) ///
	note("Notes: Sample condition ${wages_f_prev_if_condition}", ///
	size(vsmall))	

graph export "${dir_raw_results}/wages/W1fb_hist.png", replace

graph drop _all 

sum wage_hour if `filter' [aw=dwt]
sum pred_hourly_wage if `filter' & (!missing(wage_hour)) [aw=dwt] 


* Save sample for validation
save "$dir_validation_data/Female_PW_sample", replace 	

cap drop pred epsilon
 
* Formatted results
* Clean up matrix of estimates 
* Note: Zeros values are eliminated 
matrix b = e(b)	
matrix V = e(V)

* Store variance-covariance matrix 
preserve

putexcel set "$dir_raw_results/wages/var_cov", sheet("var_cov") replace
putexcel A1 = matrix(V)

import excel "$dir_raw_results/wages/var_cov", sheet("var_cov") clear

describe
local no_vars = `r(k)'	
	
forvalues i = 1/2 {
	egen row_sum = rowtotal(*)
	drop if row_sum == 0 
	drop row_sum
	xpose, clear	
}	
	
mkmat v*, matrix(var)	

* Second stage
putexcel set "$dir_raw_results/wages/reg_wages", sheet("Females_LW") replace
putexcel C2 = matrix(var)
		
restore	

* Store estimated coefficients 
* Initialize a counter for non-zero coefficients
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

putexcel set "$dir_raw_results/wages/reg_wages", sheet("Females_LW") modify
putexcel A1 = matrix(nonzero_b'), names //nformat(number_d2) 

preserve

import excel "$dir_raw_results/wages/reg_wages", sheet("Females_LW") firstrow ///
	clear
ds 
drop if C == 0 // UPDATE 
drop A 
drop AH-BK // UPDATE

mkmat *, matrix(Females_LW)
putexcel set "$dir_results/reg_wages", sheet("W1fb") modify 
putexcel B2 = matrix(Females_LW)

restore 

* Labelling 
putexcel set "$dir_results/reg_wages", ///
	sheet("W1fb") modify 

local var_list L1_log_hourly_wage Dag Dag_sq Deh_c4_Medium Deh_c4_Low Deh_c4_Medium_Dag ///
	Deh_c4_Low_Dag Dehmf_c3_Medium Dehmf_c3_Low Dlltsd01 Dhe_pcs_L1 Dhe_mcs_L1  ///
	UKC UKD UKE UKF UKG UKH UKJ UKK UKL UKM UKN Pt RealWageGrowth Y2020 Y2021 ///
	Ethn_Asian Ethn_Black Ethn_Other Constant InverseMillsRatio

	
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


* First stage
preserve

import excel "$dir_raw_results/wages/reg_wages", sheet("Females_LW") firstrow ///
	clear
ds 
drop if AH == 0 // UPDATE
drop A 
drop C-AG // UPDATE
drop BL // UPDATE

mkmat *, matrix(Females_LW)
putexcel set "$dir_results/reg_employment_selection", sheet("W1fb-sel") modify 
putexcel B2 = matrix(Females_LW)

restore 

* Labelling 
putexcel set "$dir_results/reg_employment_selection", sheet("W1fb-sel") modify 
	
local var_list Dag Dag_sq Deh_c4_Medium Deh_c4_Low Deh_c4_Medium_Dag ///
	Deh_c4_Low_Dag Dehmf_c3_Medium Dehmf_c3_Low Dcpst_Partnered D_Children Dlltsd01 Dhe_pcs_L1 Dhe_mcs_L1  ///
	UKC UKD UKE UKF UKG UKH UKJ UKK UKL UKM UKN Y2020 Y2021 ///
	Ethn_Asian Ethn_Black Ethn_Other  Constant 
	

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

cap drop lambda


* Calculate RMSE 
cap drop residuals squared_residuals  
gen residuals = lwage_hour - lwage_hour_hat
gen squared_residuals = residuals^2

preserve 
keep if `filter'
sum squared_residuals 
di "RMSE for Employed women:  " sqrt(r(mean))
putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A1=("REGRESSOR") B1=("COEFFICIENT") ///
A4=("W1fb") B4=(sqrt(r(mean))) 
restore 


/******************** WAGES: MEN, PREV WAGE OBSERVED *********************/

* Estimate a predicted wage using a Heckman selection model 
* Sample: Working age (16-75) men who received a wage in t-1
* DV: Log gross hourly wage 

global wage_eqn "lwage_hour L1.lwage_hour dag dagsq ib1.deh_c4 ib1.deh_c4#c.dag i.dehmf_c3 dlltsd01 l.dhe_pcs l.dhe_mcs  ib8.drgn1 pt real_wage_growth y2020 y2021 i.dot" //ded 
global seln_eqn "dag dagsq ib1.deh_c4 ib1.deh_c4#c.dag  i.dehmf_c3 mar child dlltsd01 l.dhe_pcs l.dhe_mcs ib8.drgn1 y2020 y2021 i.dot" //ded

local filter = "${wages_m_prev_if_condition}"
display "`filter'"

heckman $wage_eqn if `filter', select($seln_eqn) twostep mills(lambda) 

outreg2 stats(coef se pval) using "$dir_raw_results/wages/Output_WM.doc", replace ///
title("Heckman-corrected wage equation estimated on the sample of men who were in employment last year") ///
 ctitle(Working women) label side dec(2) noparen 

/***************************************************************************/
* Eigenvalue stability check 

* Extract variance-covariance matrix
matrix V = e(V)

* Preserve data state
preserve

* Export V to dataset
clear
svmat double V

* Drop zero rows and columns
forvalues r = 1/2 {
    egen rowsum = rowtotal(*)
    drop if rowsum == 0
    drop rowsum
    xpose, clear
}

* Recreate trimmed VCV matrix
mkmat *, matrix(V_trimmed)

restore

* Eigen decomposition
matrix symeigen X lambda = V_trimmed

* Largest eigenvalue
scalar max_eig = lambda[1,1]

* Smallest-to-largest eigenvalue ratio
scalar min_ratio = lambda[1, colsof(lambda)] / max_eig

* Check 1: near singularity
if max_eig < 1.0e-12 {
    display as error "CRITICAL ERROR: Heckman VCV near singular"
    display as error "Max eigenvalue = " max_eig
    exit 999
}

* Check 2: ill-conditioning
if min_ratio < 1.0e-12 {
    display as error "ERROR: Heckman VCV ill-conditioned"
    display as error "Min/Max eigenvalue ratio = " min_ratio
    exit 506
}

display "VCV stability check passed"
display "Max eigenvalue: " max_eig
display "Min/Max ratio: " min_ratio

/***************************************************************************/ 
 * Obtain predicted values (log wage) with selection correction
predict pred if `filter', ycond // ycond -> include IMR in prediction 

replace lwage_hour_hat = pred if `filter'

gen in_sample_mpw = e(sample)	

* Correct bias transforming from log to levels 
gen epsilon = rnormal()*e(sigma) 
replace pred_hourly_wage = exp(lwage_hour_hat + epsilon) if `filter'	
	 
twoway (hist wage_hour if `filter', width(0.5) ///
	lcolor(gs12) fcolor(gs12)) ///
	(hist pred_hourly_wage if `filter' & (!missing(wage_hour)), width(0.5) ///
		fcolor(none) lcolor(red)), ///
	title("Gross Hourly Wage (Level)") ///
	subtitle("Male, Previously observed wage") ///
	xtitle("GBP") ///
	legend(lab(1 "UKHLS") lab(2 "Prediction")) ///
	note("Notes: Sample condition ${wages_m_prev_if_condition}", ///
	size(vsmall))	

graph export "${dir_raw_results}/wages/W1mb_hist.png", replace

graph drop _all 

sum wage_hour if `filter' [aw=dwt]
sum pred_hourly_wage if `filter' & (!missing(wage_hour)) [aw=dwt] 

* Save sample for validation
save "$dir_validation_data/Male_PW_sample", replace 

cap drop pred epsilon	
 

 * Formatted results
* Clean up matrix of estimates 
* Note: Zeros values are eliminated 
matrix b = e(b)	
matrix V = e(V)

* Store variance-covariance matrix 
preserve

putexcel set "$dir_raw_results/wages/var_cov", sheet("var_cov") replace
putexcel A1 = matrix(V)

import excel "$dir_raw_results/wages/var_cov", sheet("var_cov") clear

describe
local no_vars = `r(k)'	
	
forvalues i = 1/2 {
	egen row_sum = rowtotal(*)
	drop if row_sum == 0 
	drop row_sum
	xpose, clear	
}	
	
mkmat v*, matrix(var)	

* Second stage
putexcel set "$dir_raw_results/wages/reg_wages", sheet("Males_LW") replace
putexcel C2 = matrix(var)
		
restore	

* Store estimated coefficients 
* Initialize a counter for non-zero coefficients
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

putexcel set "$dir_raw_results/wages/reg_wages", sheet("Males_LW") modify
putexcel A1 = matrix(nonzero_b'), names //nformat(number_d2) 

preserve

import excel "$dir_raw_results/wages/reg_wages", sheet("Males_LW") firstrow ///
	clear
ds 
drop if C == 0 // UPDATE 
drop A 
drop AH-BK // UPDATE


mkmat *, matrix(Males_LW)
putexcel set "$dir_results/reg_wages", sheet("W1mb") modify 
putexcel B2 = matrix(Males_LW)

restore 

* Labelling 
putexcel set "$dir_results/reg_wages", ///
	sheet("W1mb") modify 

local var_list L1_log_hourly_wage Dag Dag_sq Deh_c4_Medium Deh_c4_Low Deh_c4_Medium_Dag ///
	Deh_c4_Low_Dag Dehmf_c3_Medium Dehmf_c3_Low Dlltsd01 Dhe_pcs_L1 Dhe_mcs_L1  ///
	UKC UKD UKE UKF UKG UKH UKJ UKK UKL UKM UKN Pt RealWageGrowth Y2020 Y2021 ///
	Ethn_Asian Ethn_Black Ethn_Other  Constant InverseMillsRatio

	
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


* First stage
preserve

import excel "$dir_raw_results/wages/reg_wages", sheet("Males_LW") firstrow ///
	clear
ds 
drop if AH == 0 // UPDATE
drop A 
drop C-AG // UPDATE
drop BL // UPDATE

mkmat *, matrix(Males_LW)
putexcel set "$dir_results/reg_employment_selection", sheet("W1mb-sel") modify 
putexcel B2 = matrix(Males_LW)

restore 

* Labelling 
putexcel set "$dir_results/reg_employment_selection", sheet("W1mb-sel") modify 
	
local var_list Dag Dag_sq Deh_c4_Medium Deh_c4_Low Deh_c4_Medium_Dag ///
	Deh_c4_Low_Dag Dehmf_c3_Medium Dehmf_c3_Low Dcpst_Partnered D_Children Dlltsd01 Dhe_Pcs_L1 Dhe_Mcs_L1  ///
	UKC UKD UKE UKF UKG UKH UKJ UKK UKL UKM UKN Y2020 Y2021 ///
	Ethn_Asian Ethn_Black Ethn_Other Constant 
	

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

cap drop lambda


* Calculate RMSE 
cap drop residuals squared_residuals  
gen residuals = lwage_hour - lwage_hour_hat
gen squared_residuals = residuals^2

preserve 
keep if `filter'
sum squared_residuals 
di "RMSE for Employed men:  " sqrt(r(mean))
putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A1=("REGRESSOR") B1=("COEFFICIENT") ///
A5=("W1mb") B5=(sqrt(r(mean))) 
restore 

* Save for use in the do-file "reg_income" estimating non-employment incomes
// use predicted wage for all 
// use the observed wage for those that are working today and not in any 
//	estimation sample above (first observation for an individual)
replace pred_hourly_wage = exp(lwage_hour) if missing(pred_hourly_wage)

save "${estimation_sample2}", replace

capture log close 
