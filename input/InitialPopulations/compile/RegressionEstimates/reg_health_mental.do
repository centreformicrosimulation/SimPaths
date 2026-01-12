********************************************************************************
* PROJECT:  		UC and mental health
* SECTION:		Health and wellbeing
* OBJECT: 		Health status and Disability
* AUTHORS:		Andy Baxter
* LAST UPDATE:		04 Dec 2025  
* COUNTRY: 		UK 
*
* NOTES:		
*   - This file updates GHQ12 Level (0-36) and Caseness (0-12) variables
********************************************************************************
clear all
set more off
set mem 200m
set maxvar 30000


*******************************************************************
cap log close 
log using "${dir_log}/reg_health_mental.log", replace
*******************************************************************

use "$dir_ukhls_data/ukhls_pooled_all_obs_09.dta", clear
do "$dir_do/variable_update"



* Sample selection 
drop if dag < 16


xtset idperson swv


**********************************************************************
* HM1_L: GHQ12 score 0-36 of all working-age adults - baseline effects *
**********************************************************************

reg dhm ///
L.i.dhh_owned L.i.dcpst L.dnc L.dhe_pcs L.ib8.drgn L.i.ydses_c5 L.dlltsd L.dhm ///
L.dag L.dagsq i.deh_c3 i.dot i.dgn stm ///
[pweight=dimxwt]  ///
, vce(cluster idperson)

   * save raw results 
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_raw_results/health_mental/health_mental", sheet("HM1_L") replace
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))

gen in_sample = e(sample)	

predict p

save "$dir_validation_data/HM1_L_sample", replace


scalar r2_p = e(r2_p) 
scalar N = e(N)
scalar rmse = e(rmse)	
scalar chi2 = e(chi2)
scalar ll = e(ll)	


* Results 

* Note: Zeros values are eliminated 
	
matrix b = e(b)	
matrix V = e(V)


* Store variance-covariance matrix 

preserve

putexcel set "$dir_raw_results/health_mental/var_cov", sheet("var_cov") replace
putexcel A1 = matrix(V)

import excel "$dir_raw_results/health_mental/var_cov", sheet("var_cov") clear

describe
local no_vars = `r(k)'	
	
forvalues i = 1/2 {
	egen row_sum = rowtotal(*)
	drop if row_sum == 0 
	drop row_sum
	xpose, clear	
}	
	
mkmat v*, matrix(var)	
putexcel set "$dir_results/reg_health_mental", sheet("UK_HM1_L", replace) modify
putexcel C2 = matrix(var)
		
restore	


* Store estimated coefficients 

// Initialize a counter for non-zero coefficients
local non_zero_count = 0
//local names : colnames b

// Loop through each element in `b` to count non-zero coefficients
forvalues i = 1/`no_vars' {
    if (b[1, `i'] != 0) {
        local non_zero_count = `non_zero_count' + 1
    }
}

// Create a new row vector to hold only non-zero coefficients
matrix nonzero_b = J(1, `non_zero_count', .)

// Populate nonzero_b with non-zero coefficients from b
local index = 1
forvalues i = 1/`no_vars' {
    if (b[1, `i'] != 0) {
        matrix nonzero_b[1, `index'] = b[1, `i']
        local index = `index' + 1
    }
}

putexcel set "$dir_results/reg_health_mental", sheet("UK_HM1_L") modify
putexcel A1 = matrix(nonzero_b'), names nformat(number_d2) 


* Labelling 

putexcel A1 = "REGRESSOR"
putexcel A2 = "D_Home_owner_L1"
putexcel A3 = "Dcpst_Single_L1"
putexcel A4 = "Dnc_L1"
putexcel A5 = "Dhe_pcs_L1"
putexcel A6 = "UKC"
putexcel A7 = "UKD"
putexcel A8 = "UKE"
putexcel A9 = "UKF"
putexcel A10 = "UKG"
putexcel A11 = "UKH"
putexcel A12 = "UKJ"
putexcel A13 = "UKK"
putexcel A14 = "UKL"
putexcel A15 = "UKM"
putexcel A16 = "UKN"
putexcel A17 = "Ydses_c5_Q2_L1"
putexcel A18 = "Ydses_c5_Q3_L1"
putexcel A19 = "Ydses_c5_Q4_L1"
putexcel A20 = "Ydses_c5_Q5_L1"
putexcel A21 = "Dlltsd_L1"
putexcel A22 = "Dhm_L1"
putexcel A23 = "Dag_L1"
putexcel A24 = "Dag_sq_L1"
putexcel A25 = "Deh_c3_Medium"
putexcel A26 = "Deh_c3_Low"
putexcel A27 = "EthnicityAsian"
putexcel A28 = "EthnicityBlack"
putexcel A29 = "EthnicityOther"
putexcel A30 = "Dgn"
putexcel A31 = "Year_transformed"
putexcel A32 = "Constant"

putexcel B1 = "COEFFICIENT"
putexcel C1 = "D_Home_owner_L1"
putexcel D1 = "Dcpst_Single_L1"
putexcel E1 = "Dnc_L1"
putexcel F1 = "Dhe_pcs_L1"
putexcel G1 = "UKC"
putexcel H1 = "UKD"
putexcel I1 = "UKE"
putexcel J1 = "UKF"
putexcel K1 = "UKG"
putexcel L1 = "UKH"
putexcel M1 = "UKJ"
putexcel N1 = "UKK"
putexcel O1 = "UKL"
putexcel P1 = "UKM"
putexcel Q1 = "UKN"
putexcel R1 = "Ydses_c5_Q2_L1"
putexcel S1 = "Ydses_c5_Q3_L1"
putexcel T1 = "Ydses_c5_Q4_L1"
putexcel U1 = "Ydses_c5_Q5_L1"
putexcel V1 = "Dlltsd_L1"
putexcel W1 = "Dhm_L1"
putexcel X1 = "Dag_L1"
putexcel Y1 = "Dag_sq_L1"
putexcel Z1 = "Deh_c3_Medium"
putexcel AA1 = "Deh_c3_Low"
putexcel AB1 = "EthnicityAsian"
putexcel AC1 = "EthnicityBlack"
putexcel AD1 = "EthnicityOther"
putexcel AE1 = "Dgn"
putexcel AF1 = "Year_transformed"
putexcel AG1 = "Constant"
		
* save RMSE
putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A10 = ("HM1_L") B10 = rmse 

drop in_sample p

scalar drop r2_p N chi2 ll	
***************************************************************
* HM2_Females_L: GHQ12 Score 0-36 - causal employment effects *
***************************************************************


*Stage 2
*Female
reghdfe dhm ///
ib11.exp_emp i.exp_poverty i.exp_incchange D.log_income financial_distress ///
y2020 y2021 ///
L.i.dhh_owned L.i.dcpst L.dnc L.dhe_pcs L.ib8.drgn L.i.ydses_c5 L.dlltsd L.dhm ///
L.dag L.dagsq i.deh_c3 stm ///
if dag>=25 & dag<=64 & dgn==0 ///
[pweight=dimxwt]  ///
, absorb(idperson) vce(cluster idperson)


   * save raw results 
matrix results = r(table)
matrix results = results[1..6,1..10]'
putexcel set "$dir_raw_results/health_mental/health_mental", sheet("HM2_Females_L") replace
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))

gen in_sample = e(sample)	

predict p

save "$dir_validation_data/HM2_Females_L_sample", replace


scalar r2_p = e(r2_p) 
scalar N = e(N)
scalar rmse = e(rmse)	
scalar chi2 = e(chi2)
scalar ll = e(ll)	


* Results 

* Note: Zeros values are eliminated 
	
matrix b = e(b)	
matrix V = e(V)
matrix V = V[1..14,1..14]

forvalues i = 1/14 {
    forvalues j = 1/14 {
        if `i' == `j' {
          continue
        }
        matrix V[`i',`j'] = 0
    }
}

* Store variance-covariance matrix 

preserve

putexcel set "$dir_raw_results/health_mental/var_cov", sheet("var_cov") replace
putexcel A1 = matrix(V)

import excel "$dir_raw_results/health_mental/var_cov", sheet("var_cov") clear

describe
local no_vars = `r(k)'	
	
forvalues i = 1/2 {
	egen row_sum = rowtotal(*)
	drop if row_sum == 0 
	drop row_sum
	xpose, clear	
}	
	
mkmat v*, matrix(var)	
putexcel set "$dir_results/reg_health_mental", sheet("UK_HM2_Females_L", replace) modify
putexcel C2 = matrix(var)
		
restore	


* Store estimated coefficients 

// Initialize a counter for non-zero coefficients
local non_zero_count = 0
//local names : colnames b

// Loop through each element in `b` to count non-zero coefficients
forvalues i = 1/`no_vars' {
    if (b[1, `i'] != 0) {
        local non_zero_count = `non_zero_count' + 1
    }
}

// Create a new row vector to hold only non-zero coefficients
matrix nonzero_b = J(1, `non_zero_count', .)

// Populate nonzero_b with non-zero coefficients from b
local index = 1
forvalues i = 1/`no_vars' {
    if (b[1, `i'] != 0) {
        matrix nonzero_b[1, `index'] = b[1, `i']
        local index = `index' + 1
    }
}

putexcel set "$dir_results/reg_health_mental", sheet("UK_HM2_Females_L") modify
putexcel A1 = matrix(nonzero_b'), names nformat(number_d2) 

* Labelling 

putexcel A1 = "REGRESSOR"
putexcel A2 = "EmployedToUnemployed"
putexcel A3 = "UnemployedToEmployed"
putexcel A4 = "PersistentUnemployed"
putexcel A5 = "NonPovertyToPoverty"
putexcel A6 = "PovertyToNonPoverty"
putexcel A7 = "PersistentPoverty"
putexcel A8 = "RealIncomeChange"
putexcel A9 = "RealIncomeDecrease_D"
putexcel A10 = "FinancialDistress"
putexcel A11 = "Covid_2020_D"
putexcel A12 = "Covid_2021_D"


putexcel B1 = "COEFFICIENT"
putexcel C1 = "EmployedToUnemployed"
putexcel D1 = "UnemployedToEmployed"
putexcel E1 = "PersistentUnemployed"
putexcel F1 = "NonPovertyToPoverty"
putexcel G1 = "PovertyToNonPoverty"
putexcel H1 = "PersistentPoverty"
putexcel I1 = "RealIncomeChange"
putexcel J1 = "RealIncomeDecrease_D"
putexcel K1 = "FinancialDistress"
putexcel L1 = "Covid_2020_D"
putexcel M1 = "Covid_2021_D"
		
* save RMSE
putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A11 = ("HM2_Females_L") B11 = rmse 

drop in_sample p
scalar drop r2_p N chi2 ll	

***************************************************************
* HM2_Males_L: GHQ12 Score 0-36 - causal employment effects *
***************************************************************


*Stage 2
*Male
reghdfe dhm ///
ib11.exp_emp i.exp_poverty i.exp_incchange D.log_income financial_distress ///
y2020 y2021 ///
L.i.dhh_owned L.i.dcpst L.dnc L.dhe_pcs L.ib8.drgn L.i.ydses_c5 L.dlltsd L.dhm ///
L.dag L.dagsq i.deh_c3 stm ///
if dag>=25 & dag<=64 & dgn==1 ///
[pweight=dimxwt]  ///
, absorb(idperson) vce(cluster idperson)


   * save raw results 
matrix results = r(table)
matrix results = results[1..6,1..10]'
putexcel set "$dir_raw_results/health_mental/health_mental", sheet("HM2_Males_L") replace
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))

gen in_sample = e(sample)	

predict p

save "$dir_validation_data/HM2_Males_L_sample", replace


scalar r2_p = e(r2_p) 
scalar N = e(N)
scalar rmse = e(rmse)	
scalar chi2 = e(chi2)
scalar ll = e(ll)	


* Results 

* Note: Zeros values are eliminated 
	
matrix b = e(b)	
matrix V = e(V)
matrix V = V[1..14,1..14]

forvalues i = 1/14 {
    forvalues j = 1/14 {
        if `i' == `j' {
          continue
        }
        matrix V[`i',`j'] = 0
    }
}

* Store variance-covariance matrix 

preserve

putexcel set "$dir_raw_results/health_mental/var_cov", sheet("var_cov") replace
putexcel A1 = matrix(V)

import excel "$dir_raw_results/health_mental/var_cov", sheet("var_cov") clear

describe
local no_vars = `r(k)'	
	
forvalues i = 1/2 {
	egen row_sum = rowtotal(*)
	drop if row_sum == 0 
	drop row_sum
	xpose, clear	
}	
	
mkmat v*, matrix(var)	
putexcel set "$dir_results/reg_health_mental", sheet("UK_HM2_Males_L", replace) modify
putexcel C2 = matrix(var)
		
restore	


* Store estimated coefficients 

// Initialize a counter for non-zero coefficients
local non_zero_count = 0
//local names : colnames b

// Loop through each element in `b` to count non-zero coefficients
forvalues i = 1/`no_vars' {
    if (b[1, `i'] != 0) {
        local non_zero_count = `non_zero_count' + 1
    }
}

// Create a new row vector to hold only non-zero coefficients
matrix nonzero_b = J(1, `non_zero_count', .)

// Populate nonzero_b with non-zero coefficients from b
local index = 1
forvalues i = 1/`no_vars' {
    if (b[1, `i'] != 0) {
        matrix nonzero_b[1, `index'] = b[1, `i']
        local index = `index' + 1
    }
}

putexcel set "$dir_results/reg_health_mental", sheet("UK_HM2_Males_L") modify
putexcel A1 = matrix(nonzero_b'), names nformat(number_d2) 

* Labelling 

putexcel A1 = "REGRESSOR"
putexcel A2 = "EmployedToUnemployed"
putexcel A3 = "UnemployedToEmployed"
putexcel A4 = "PersistentUnemployed"
putexcel A5 = "NonPovertyToPoverty"
putexcel A6 = "PovertyToNonPoverty"
putexcel A7 = "PersistentPoverty"
putexcel A8 = "RealIncomeChange"
putexcel A9 = "RealIncomeDecrease_D"
putexcel A10 = "FinancialDistress"
putexcel A11 = "Covid_2020_D"
putexcel A12 = "Covid_2021_D"


putexcel B1 = "COEFFICIENT"
putexcel C1 = "EmployedToUnemployed"
putexcel D1 = "UnemployedToEmployed"
putexcel E1 = "PersistentUnemployed"
putexcel F1 = "NonPovertyToPoverty"
putexcel G1 = "PovertyToNonPoverty"
putexcel H1 = "PersistentPoverty"
putexcel I1 = "RealIncomeChange"
putexcel J1 = "RealIncomeDecrease_D"
putexcel K1 = "FinancialDistress"
putexcel L1 = "Covid_2020_D"
putexcel M1 = "Covid_2021_D"
		
* save RMSE
putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A12 = ("HM2_Males_L") B12 = rmse 

drop in_sample p
scalar drop r2_p N chi2 ll	


**********************************************************************
* HM1_C: GHQ12 score 0-12 of all working-age adults - baseline effects *
**********************************************************************

* New ordered logistic regression model, reflecting observed distributions

ologit scghq2_dv ///
L.i.dhh_owned L.i.dcpst L.dnc L.dhe_pcs L.ib8.drgn L.i.ydses_c5 L.dlltsd L.scghq2_dv ///
L.dag L.dagsq i.deh_c3 i.dot i.dgn stm ///
if stm!=20 & stm!=21 & dag>=25 & dag<=64 & swv!=12 ///
[pweight=dimxwt]  ///
, vce(cluster idperson)

   * save raw results 
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_raw_results/health_mental/health_mental", sheet("HM1_C") replace
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))

gen in_sample = e(sample)	

predict p

save "$dir_validation_data/HM1_C_sample", replace


scalar r2_p = e(r2_p) 
scalar N = e(N)
scalar rmse = e(rmse)	
scalar chi2 = e(chi2)
scalar ll = e(ll)	


* Results 

* Note: Zeros values are eliminated 
	
matrix b = e(b)	
matrix V = e(V)


* Store variance-covariance matrix 

preserve

putexcel set "$dir_raw_results/health_mental/var_cov", sheet("var_cov") replace
putexcel A1 = matrix(V)

import excel "$dir_raw_results/health_mental/var_cov", sheet("var_cov") clear

describe
local no_vars = `r(k)'	
	
forvalues i = 1/2 {
	egen row_sum = rowtotal(*)
	drop if row_sum == 0 
	drop row_sum
	xpose, clear	
}	
	
mkmat v*, matrix(var)	
putexcel set "$dir_results/reg_health_mental", sheet("UK_HM1_C", replace) modify
putexcel C2 = matrix(var)
		
restore	


* Store estimated coefficients 

// Initialize a counter for non-zero coefficients
local non_zero_count = 0
//local names : colnames b

// Loop through each element in `b` to count non-zero coefficients
forvalues i = 1/`no_vars' {
    if (b[1, `i'] != 0) {
        local non_zero_count = `non_zero_count' + 1
    }
}

// Create a new row vector to hold only non-zero coefficients
matrix nonzero_b = J(1, `non_zero_count', .)

// Populate nonzero_b with non-zero coefficients from b
local index = 1
forvalues i = 1/`no_vars' {
    if (b[1, `i'] != 0) {
        matrix nonzero_b[1, `index'] = b[1, `i']
        local index = `index' + 1
    }
}

putexcel set "$dir_results/reg_health_mental", sheet("UK_HM1_C") modify
putexcel A1 = matrix(nonzero_b'), names nformat(number_d2) 


* Labelling 

putexcel A1 = "REGRESSOR"
putexcel A2 = "D_Home_owner_L1"
putexcel A3 = "Dcpst_Single_L1"
putexcel A4 = "Dnc_L1"
putexcel A5 = "Dhe_pcs_L1"
putexcel A6 = "UKC"
putexcel A7 = "UKD"
putexcel A8 = "UKE"
putexcel A9 = "UKF"
putexcel A10 = "UKG"
putexcel A11 = "UKH"
putexcel A12 = "UKJ"
putexcel A13 = "UKK"
putexcel A14 = "UKL"
putexcel A15 = "UKM"
putexcel A16 = "UKN"
putexcel A17 = "Ydses_c5_Q2_L1"
putexcel A18 = "Ydses_c5_Q3_L1"
putexcel A19 = "Ydses_c5_Q4_L1"
putexcel A20 = "Ydses_c5_Q5_L1"
putexcel A21 = "Dlltsd_L1"
putexcel A22 = "Dhm_L1"
putexcel A23 = "Dag_L1"
putexcel A24 = "Dag_sq_L1"
putexcel A25 = "Deh_c3_Medium"
putexcel A26 = "Deh_c3_Low"
putexcel A27 = "EthnicityAsian"
putexcel A28 = "EthnicityBlack"
putexcel A29 = "EthnicityOther"
putexcel A30 = "Dgn"
putexcel A31 = "Year_transformed"
putexcel A32 = "Cut1"
putexcel A33 = "Cut2"
putexcel A34 = "Cut3"
putexcel A35 = "Cut4"
putexcel A36 = "Cut5"
putexcel A37 = "Cut6"
putexcel A38 = "Cut7"
putexcel A39 = "Cut8"
putexcel A40 = "Cut9"
putexcel A41 = "Cut10"
putexcel A42 = "Cut11"
putexcel A43 = "Cut12"

putexcel B1 = "COEFFICIENT"
putexcel C1 = "D_Home_owner_L1"
putexcel D1 = "Dcpst_Single_L1"
putexcel E1 = "Dnc_L1"
putexcel F1 = "Dhe_pcs_L1"
putexcel G1 = "UKC"
putexcel H1 = "UKD"
putexcel I1 = "UKE"
putexcel J1 = "UKF"
putexcel K1 = "UKG"
putexcel L1 = "UKH"
putexcel M1 = "UKJ"
putexcel N1 = "UKK"
putexcel O1 = "UKL"
putexcel P1 = "UKM"
putexcel Q1 = "UKN"
putexcel R1 = "Ydses_c5_Q2_L1"
putexcel S1 = "Ydses_c5_Q3_L1"
putexcel T1 = "Ydses_c5_Q4_L1"
putexcel U1 = "Ydses_c5_Q5_L1"
putexcel V1 = "Dlltsd_L1"
putexcel W1 = "Dhm_L1"
putexcel X1 = "Dag_L1"
putexcel Y1 = "Dag_sq_L1"
putexcel Z1 = "Deh_c3_Medium"
putexcel AA1 = "Deh_c3_Low"
putexcel AB1 = "EthnicityAsian"
putexcel AC1 = "EthnicityBlack"
putexcel AD1 = "EthnicityOther"
putexcel AE1 = "Dgn"
putexcel AF1 = "Year_transformed"
putexcel AG1 = "Cut1"
putexcel AH1 = "Cut2"
putexcel AI1 = "Cut3"
putexcel AJ1 = "Cut4"
putexcel AK1 = "Cut5"
putexcel AL1 = "Cut6"
putexcel AM1 = "Cut7"
putexcel AN1 = "Cut8"
putexcel AO1 = "Cut9"
putexcel AP1 = "Cut10"
putexcel AQ1 = "Cut11"
putexcel AR1 = "Cut12"
		
* save RMSE - not strictly needed for ologit predictions
putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A11 = ("HM1_C") B11 = rmse 

drop in_sample p
scalar drop r2_p N chi2 ll	

***************************************************************
* HM2_Females_C: GHQ12 Score 0-12 - causal employment effects *
***************************************************************

* Kept as linear as adding an 'additional' causal effect on baseline

gen RealIncomeDecrease_D = log_income - L.log_income
gen scghq2_dv_L1 = L.scghq2_dv

*Stage 2
*Female
reghdfe scghq2_dv ///
ib11.exp_emp i.exp_poverty i.exp_incchange RealIncomeDecrease_D financial_distress ///
y2020 y2021 ///
i.dhh_owned i.dcpst dnc dhe_pcs ib8.drgn i.ydses_c5 dlltsd ///
dag dagsq i.deh_c3 stm ///
if dag>=25 & dag<=64 & dgn==0 ///
, absorb(idperson) vce(cluster idperson)


   * save raw results 
matrix results = r(table)
matrix results = results[1..6,1..10]'
putexcel set "$dir_raw_results/health_mental/health_mental", sheet("HM2_Females_C") replace
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))

gen in_sample = e(sample)	

predict p

save "$dir_validation_data/HM2_Females_C_sample", replace


scalar r2_p = e(r2_p) 
scalar N = e(N)
scalar rmse = e(rmse)	
scalar chi2 = e(chi2)
scalar ll = e(ll)	


* Results 

* Note: Zeros values are eliminated 
	
matrix b = e(b)	
matrix V = e(V)
matrix V = V[1..14,1..14]

forvalues i = 1/14 {
    forvalues j = 1/14 {
        if `i' == `j' {
          continue
        }
        matrix V[`i',`j'] = 0
    }
}

* Store variance-covariance matrix 

preserve

putexcel set "$dir_raw_results/health_mental/var_cov", sheet("var_cov") replace
putexcel A1 = matrix(V)

import excel "$dir_raw_results/health_mental/var_cov", sheet("var_cov") clear

describe
local no_vars = `r(k)'	
	
forvalues i = 1/2 {
	egen row_sum = rowtotal(*)
	drop if row_sum == 0 
	drop row_sum
	xpose, clear	
}	
	
mkmat v*, matrix(var)	
putexcel set "$dir_results/reg_health_mental", sheet("UK_HM2_Females_C", replace) modify
putexcel C2 = matrix(var)
		
restore	


* Store estimated coefficients 

// Initialize a counter for non-zero coefficients
local non_zero_count = 0
//local names : colnames b

// Loop through each element in `b` to count non-zero coefficients
forvalues i = 1/`no_vars' {
    if (b[1, `i'] != 0) {
        local non_zero_count = `non_zero_count' + 1
    }
}

// Create a new row vector to hold only non-zero coefficients
matrix nonzero_b = J(1, `non_zero_count', .)

// Populate nonzero_b with non-zero coefficients from b
local index = 1
forvalues i = 1/`no_vars' {
    if (b[1, `i'] != 0) {
        matrix nonzero_b[1, `index'] = b[1, `i']
        local index = `index' + 1
    }
}

putexcel set "$dir_results/reg_health_mental", sheet("UK_HM2_Females_C") modify
putexcel A1 = matrix(nonzero_b'), names nformat(number_d2) 

* Labelling 

putexcel A1 = "REGRESSOR"
putexcel A2 = "EmployedToUnemployed"
putexcel A3 = "UnemployedToEmployed"
putexcel A4 = "PersistentUnemployed"
putexcel A5 = "NonPovertyToPoverty"
putexcel A6 = "PovertyToNonPoverty"
putexcel A7 = "PersistentPoverty"
putexcel A8 = "RealIncomeChange"
putexcel A9 = "RealIncomeDecrease_D"
putexcel A10 = "FinancialDistress"
putexcel A11 = "Covid_2020_D"
putexcel A12 = "Covid_2021_D"


putexcel B1 = "COEFFICIENT"
putexcel C1 = "EmployedToUnemployed"
putexcel D1 = "UnemployedToEmployed"
putexcel E1 = "PersistentUnemployed"
putexcel F1 = "NonPovertyToPoverty"
putexcel G1 = "PovertyToNonPoverty"
putexcel H1 = "PersistentPoverty"
putexcel I1 = "RealIncomeChange"
putexcel J1 = "RealIncomeDecrease_D"
putexcel K1 = "FinancialDistress"
putexcel L1 = "Covid_2020_D"
putexcel M1 = "Covid_2021_D"
		
* save RMSE
putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A14 = ("HM2_Females_C") B14 = rmse 

drop in_sample p
scalar drop r2_p N chi2 ll	

***************************************************************
* HM2_Males_C: GHQ12 Score 0-12 - causal employment effects *
***************************************************************


*Stage 2
*Male
reghdfe scghq2_dv ///
ib11.exp_emp i.exp_poverty i.exp_incchange RealIncomeDecrease_D financial_distress ///
y2020 y2021 ///
i.dhh_owned i.dcpst dnc dhe_pcs ib8.drgn i.ydses_c5 dlltsd ///
dag dagsq i.deh_c3 stm ///
if dag>=25 & dag<=64 & dgn==1 ///
, absorb(idperson) vce(cluster idperson)

   * save raw results 
matrix results = r(table)
matrix results = results[1..6,1..10]'
putexcel set "$dir_raw_results/health_mental/health_mental", sheet("HM2_Males_C") replace
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))

gen in_sample = e(sample)	

predict p

save "$dir_validation_data/HM2_Males_C_sample", replace


scalar r2_p = e(r2_p) 
scalar N = e(N)
scalar rmse = e(rmse)	
scalar chi2 = e(chi2)
scalar ll = e(ll)	


* Results 

* Note: Zeros values are eliminated 
	
matrix b = e(b)	
matrix V = e(V)
matrix V = V[1..14,1..14]

forvalues i = 1/14 {
    forvalues j = 1/14 {
        if `i' == `j' {
          continue
        }
        matrix V[`i',`j'] = 0
    }
}

* Store variance-covariance matrix 

preserve

putexcel set "$dir_raw_results/health_mental/var_cov", sheet("var_cov") replace
putexcel A1 = matrix(V)

import excel "$dir_raw_results/health_mental/var_cov", sheet("var_cov") clear

describe
local no_vars = `r(k)'	
	
forvalues i = 1/2 {
	egen row_sum = rowtotal(*)
	drop if row_sum == 0 
	drop row_sum
	xpose, clear	
}	
	
mkmat v*, matrix(var)	
putexcel set "$dir_results/reg_health_mental", sheet("UK_HM2_Males_C", replace) modify
putexcel C2 = matrix(var)
		
restore	


* Store estimated coefficients 

// Initialize a counter for non-zero coefficients
local non_zero_count = 0
//local names : colnames b

// Loop through each element in `b` to count non-zero coefficients
forvalues i = 1/`no_vars' {
    if (b[1, `i'] != 0) {
        local non_zero_count = `non_zero_count' + 1
    }
}

// Create a new row vector to hold only non-zero coefficients
matrix nonzero_b = J(1, `non_zero_count', .)

// Populate nonzero_b with non-zero coefficients from b
local index = 1
forvalues i = 1/`no_vars' {
    if (b[1, `i'] != 0) {
        matrix nonzero_b[1, `index'] = b[1, `i']
        local index = `index' + 1
    }
}

putexcel set "$dir_results/reg_health_mental", sheet("UK_HM2_Males_C") modify
putexcel A1 = matrix(nonzero_b'), names nformat(number_d2) 

* Labelling 

putexcel A1 = "REGRESSOR"
putexcel A2 = "EmployedToUnemployed"
putexcel A3 = "UnemployedToEmployed"
putexcel A4 = "PersistentUnemployed"
putexcel A5 = "NonPovertyToPoverty"
putexcel A6 = "PovertyToNonPoverty"
putexcel A7 = "PersistentPoverty"
putexcel A8 = "RealIncomeChange"
putexcel A9 = "RealIncomeDecrease_D"
putexcel A10 = "FinancialDistress"
putexcel A11 = "Covid_2020_D"
putexcel A12 = "Covid_2021_D"


putexcel B1 = "COEFFICIENT"
putexcel C1 = "EmployedToUnemployed"
putexcel D1 = "UnemployedToEmployed"
putexcel E1 = "PersistentUnemployed"
putexcel F1 = "NonPovertyToPoverty"
putexcel G1 = "PovertyToNonPoverty"
putexcel H1 = "PersistentPoverty"
putexcel I1 = "RealIncomeChange"
putexcel J1 = "RealIncomeDecrease_D"
putexcel K1 = "FinancialDistress"
putexcel L1 = "Covid_2020_D"
putexcel M1 = "Covid_2021_D"
		
* save RMSE
putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A15 = ("HM2_Males_C") B15 = rmse 

drop in_sample p
scalar drop r2_p N chi2 ll	
