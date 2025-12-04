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
putexcel set "$dir_results/reg_health_mental", sheet("UK_HM1_L") modify
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
putexcel A4 = "Dcpst_PreviouslyPartnered_L1"
putexcel A5 = "Dnc_L1"
putexcel A6 = "Dhe_pcs_L1"
putexcel A7 = "UKC"
putexcel A8 = "UKD"
putexcel A9 = "UKE"
putexcel A10 = "UKF"
putexcel A11 = "UKG"
putexcel A12 = "UKH"
putexcel A13 = "UKJ"
putexcel A14 = "UKK"
putexcel A15 = "UKL"
putexcel A16 = "UKM"
putexcel A17 = "UKN"
putexcel A18 = "Ydses_c5_Q2_L1"
putexcel A19 = "Ydses_c5_Q3_L1"
putexcel A20 = "Ydses_c5_Q4_L1"
putexcel A21 = "Ydses_c5_Q5_L1"
putexcel A22 = "Dlltsd_L1"
putexcel A23 = "Dhm_L1"
putexcel A24 = "Dag_L1"
putexcel A25 = "Dag_sq_L1"
putexcel A26 = "Deh_c3_Medium"
putexcel A27 = "Deh_c3_Low"
putexcel A28 = "EthnicityAsian"
putexcel A29 = "EthnicityBlack"
putexcel A30 = "EthnicityOther"
putexcel A31 = "Dgn"
putexcel A32 = "Year_transformed"
putexcel A33 = "Constant"

putexcel B1 = "COEFFICIENT"
putexcel C1 = "D_Home_owner_L1"
putexcel D1 = "Dcpst_Single_L1"
putexcel E1 = "Dcpst_PreviouslyPartnered_L1"
putexcel F1 = "Dnc_L1"
putexcel G1 = "Dhe_pcs_L1"
putexcel H1 = "UKC"
putexcel I1 = "UKD"
putexcel J1 = "UKE"
putexcel K1 = "UKF"
putexcel L1 = "UKG"
putexcel M1 = "UKH"
putexcel N1 = "UKJ"
putexcel O1 = "UKK"
putexcel P1 = "UKL"
putexcel Q1 = "UKM"
putexcel R1 = "UKN"
putexcel S1 = "Ydses_c5_Q2_L1"
putexcel T1 = "Ydses_c5_Q3_L1"
putexcel U1 = "Ydses_c5_Q4_L1"
putexcel V1 = "Ydses_c5_Q5_L1"
putexcel W1 = "Dlltsd_L1"
putexcel X1 = "Dhm_L1"
putexcel Y1 = "Dag_L1"
putexcel Z1 = "Dag_sq_L1"
putexcel AA1 = "Deh_c3_Medium"
putexcel AB1 = "Deh_c3_Low"
putexcel AC1 = "EthnicityAsian"
putexcel AD1 = "EthnicityBlack"
putexcel AE1 = "EthnicityOther"
putexcel AF1 = "Dgn"
putexcel AG1 = "Year_transformed"
putexcel AH1 = "Constant"
		
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
putexcel set "$dir_results/reg_health_mental", sheet("UK_HM2_Females_L") modify
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
putexcel set "$dir_results/reg_health_mental", sheet("UK_HM2_Males_L") modify
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

reg scghq2_dv ///
L.i.dhh_owned L.i.dcpst L.dnc L.dhe_pcs L.ib8.drgn L.i.ydses_c5 L.dlltsd L.scghq2_dv ///
L.dag L.dagsq i.deh_c3 i.dot i.dgn stm ///
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
putexcel set "$dir_results/reg_health_mental", sheet("UK_HM1_C") modify
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
putexcel A4 = "Dcpst_PreviouslyPartnered_L1"
putexcel A5 = "Dnc_L1"
putexcel A6 = "Dhe_pcs_L1"
putexcel A7 = "UKC"
putexcel A8 = "UKD"
putexcel A9 = "UKE"
putexcel A10 = "UKF"
putexcel A11 = "UKG"
putexcel A12 = "UKH"
putexcel A13 = "UKJ"
putexcel A14 = "UKK"
putexcel A15 = "UKL"
putexcel A16 = "UKM"
putexcel A17 = "UKN"
putexcel A18 = "Ydses_c5_Q2_L1"
putexcel A19 = "Ydses_c5_Q3_L1"
putexcel A20 = "Ydses_c5_Q4_L1"
putexcel A21 = "Ydses_c5_Q5_L1"
putexcel A22 = "Dlltsd_L1"
putexcel A23 = "Dhm_L1"
putexcel A24 = "Dag_L1"
putexcel A25 = "Dag_sq_L1"
putexcel A26 = "Deh_c3_Medium"
putexcel A27 = "Deh_c3_Low"
putexcel A28 = "EthnicityAsian"
putexcel A29 = "EthnicityBlack"
putexcel A30 = "EthnicityOther"
putexcel A31 = "Dgn"
putexcel A32 = "Year_transformed"
putexcel A33 = "Constant"

putexcel B1 = "COEFFICIENT"
putexcel C1 = "D_Home_owner_L1"
putexcel D1 = "Dcpst_Single_L1"
putexcel E1 = "Dcpst_PreviouslyPartnered_L1"
putexcel F1 = "Dnc_L1"
putexcel G1 = "Dhe_pcs_L1"
putexcel H1 = "UKC"
putexcel I1 = "UKD"
putexcel J1 = "UKE"
putexcel K1 = "UKF"
putexcel L1 = "UKG"
putexcel M1 = "UKH"
putexcel N1 = "UKJ"
putexcel O1 = "UKK"
putexcel P1 = "UKL"
putexcel Q1 = "UKM"
putexcel R1 = "UKN"
putexcel S1 = "Ydses_c5_Q2_L1"
putexcel T1 = "Ydses_c5_Q3_L1"
putexcel U1 = "Ydses_c5_Q4_L1"
putexcel V1 = "Ydses_c5_Q5_L1"
putexcel W1 = "Dlltsd_L1"
putexcel X1 = "Dhm_L1"
putexcel Y1 = "Dag_L1"
putexcel Z1 = "Dag_sq_L1"
putexcel AA1 = "Deh_c3_Medium"
putexcel AB1 = "Deh_c3_Low"
putexcel AC1 = "EthnicityAsian"
putexcel AD1 = "EthnicityBlack"
putexcel AE1 = "EthnicityOther"
putexcel AF1 = "Dgn"
putexcel AG1 = "Year_transformed"
putexcel AH1 = "Constant"
		
* save RMSE
putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A13 = ("HM1_C") B13 = rmse 

drop in_sample p
scalar drop r2_p N chi2 ll	
***************************************************************
* HM2_Females_C: GHQ12 Score 0-12 - causal employment effects *
***************************************************************

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
putexcel set "$dir_results/reg_health_mental", sheet("UK_HM2_Females_C") modify
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
putexcel set "$dir_results/reg_health_mental", sheet("UK_HM2_Males_C") modify
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
