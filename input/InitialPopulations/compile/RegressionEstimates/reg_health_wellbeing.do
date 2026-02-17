********************************************************************************
* PROJECT:  		UC and mental health
* SECTION:		Health and wellbeing
* OBJECT: 		Health status and Disability
* AUTHORS:		Andy Baxter
* LAST UPDATE:	17 Feb 2026
* COUNTRY: 		UK 
*
* NOTES:		
*   - This file updates SF12 MCS and PCS, and Life Satisfaction (7 levels)	     
********************************************************************************
clear all
set more off
set mem 200m
set maxvar 30000


*******************************************************************
cap log close 
log using "${dir_log}/reg_health_wellbeing.log", replace
*******************************************************************

/********************************* PREPARE DATA *******************************/

use ${estimation_sample}, clear

* Set data 
xtset idperson swv
sort idperson swv 

* Remove children 
drop if dag < 16

* Adjust variables 
do "${dir_do}/variable_update.do"
do "${dir_do}/variable_update_legacy.do"

********************************************************************************
* DHE_MCS1 - SF12 MCS score 0-100 of all working-age adults - baseline effects *
********************************************************************************

reg dhe_mcs ///
L.i.dhh_owned L.i.dcpst L.dnc L.dhe_pcs L.ib8.drgn L.i.ydses_c5 L.dlltsd01 L.dhe_mcs ///
L.dag L.dagsq i.deh_c3 i.dot i.dgn stm ///
[pweight=dimxwt]  ///
, vce(cluster idperson)

   * save raw results 
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_raw_results/health_wellbeing/health_wellbeing", sheet("DHE_MCS1") replace
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))

gen in_sample = e(sample)	

predict p

save "$dir_validation_data/DHE_MCS1_sample", replace


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

putexcel set "$dir_raw_results/health_wellbeing/var_cov", sheet("var_cov") replace
putexcel A1 = matrix(V)

import excel "$dir_raw_results/health_wellbeing/var_cov", sheet("var_cov") clear

describe
local no_vars = `r(k)'	
	
forvalues i = 1/2 {
	egen row_sum = rowtotal(*)
	drop if row_sum == 0 
	drop row_sum
	xpose, clear	
}	
	
mkmat v*, matrix(var)	
putexcel set "$dir_results/reg_health_wellbeing", sheet("UK_DHE_MCS1", replace) modify
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

putexcel set "$dir_results/reg_health_wellbeing", sheet("UK_DHE_MCS1") modify
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
putexcel A21 = "Dlltsd01_L1"
putexcel A22 = "Dhe_mcs_L1"
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
putexcel V1 = "Dlltsd01_L1"
putexcel W1 = "Dhe_mcs_L1"
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
putexcel A16 = ("DHE_MCS1") B16 = rmse 
		

drop in_sample p
scalar drop r2_p N chi2 ll


***************************************************************
* DHE_MCS2_Females: SF12 MCS score 0-100 - causal employment effects *
***************************************************************


*Stage 2
*Female
reghdfe dhe_mcs ///
ib11.exp_emp i.exp_poverty i.exp_incchange D.log_income financial_distress ///
y2020 y2021 ///
L.i.dhh_owned L.i.dcpst L.dnc L.dhe_pcs L.ib8.drgn L.i.ydses_c5 L.dlltsd01 L.dhe_mcs ///
L.dag L.dagsq i.deh_c3 stm ///
if dag>=25 & dag<=64 & dgn==0 ///
[pweight=dimxwt]  ///
, absorb(idperson) vce(cluster idperson)  


   * save raw results 
matrix results = r(table)
matrix results = results[1..6,1..10]'
putexcel set "$dir_raw_results/health_wellbeing/health_wellbeing", sheet("DHE_MCS2_Females") replace
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))

gen in_sample = e(sample)	

predict p

save "$dir_validation_data/DHE_MCS2_Females_sample", replace


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

putexcel set "$dir_raw_results/health_wellbeing/var_cov", sheet("var_cov") replace
putexcel A1 = matrix(V)

import excel "$dir_raw_results/health_wellbeing/var_cov", sheet("var_cov") clear

describe
local no_vars = `r(k)'	
	
forvalues i = 1/2 {
	egen row_sum = rowtotal(*)
	drop if row_sum == 0 
	drop row_sum
	xpose, clear	
}	
	
mkmat v*, matrix(var)	
putexcel set "$dir_results/reg_health_wellbeing", sheet("UK_DHE_MCS2_Females", replace) modify
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

putexcel set "$dir_results/reg_health_wellbeing", sheet("UK_DHE_MCS2_Females") modify
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
putexcel A17 = ("DHE_MCS2_Females") B17 = rmse 

		
drop in_sample p
scalar drop r2_p N chi2 ll	

***************************************************************
* DHE_MCS2_Males: SF12 MCS score 0-100 - causal employment effects *
***************************************************************


*Stage 2
*Male
reghdfe dhe_mcs ///
ib11.exp_emp i.exp_poverty i.exp_incchange D.log_income financial_distress ///
y2020 y2021 ///
L.i.dhh_owned L.i.dcpst L.dnc L.dhe_pcs L.ib8.drgn L.i.ydses_c5 L.dlltsd01 L.dhe_mcs ///
L.dag L.dagsq i.deh_c3 stm ///
if dag>=25 & dag<=64 & dgn==1 ///
[pweight=dimxwt]  ///
, absorb(idperson) vce(cluster idperson)


   * save raw results 
matrix results = r(table)
matrix results = results[1..6,1..10]'
putexcel set "$dir_raw_results/health_wellbeing/health_wellbeing", sheet("DHE_MCS2_Males") replace
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))

gen in_sample = e(sample)	

predict p

save "$dir_validation_data/DHE_MCS2_Males_sample", replace


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

putexcel set "$dir_raw_results/health_wellbeing/var_cov", sheet("var_cov") replace
putexcel A1 = matrix(V)

import excel "$dir_raw_results/health_wellbeing/var_cov", sheet("var_cov") clear

describe
local no_vars = `r(k)'	
	
forvalues i = 1/2 {
	egen row_sum = rowtotal(*)
	drop if row_sum == 0 
	drop row_sum
	xpose, clear	
}	
	
mkmat v*, matrix(var)	
putexcel set "$dir_results/reg_health_wellbeing", sheet("UK_DHE_MCS2_Males", replace) modify
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

putexcel set "$dir_results/reg_health_wellbeing", sheet("UK_DHE_MCS2_Males") modify
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
putexcel A18 = ("DHE_MCS2_Males") B18 = rmse 

		
drop in_sample p
scalar drop r2_p N chi2 ll	


*******************************************************************************
* DHE_PCS1 - SF12 PCS score 0-100 of all working-age adults - baseline effects *
********************************************************************************

reg dhe_pcs ///
L.i.dhh_owned L.i.dcpst L.dnc L.dhe_mcs L.ib8.drgn L.i.ydses_c5 L.dlltsd01 L.dhe_pcs ///
L.dag L.dagsq i.deh_c3 i.dot i.dgn stm ///
[pweight=dimxwt]  ///
, vce(cluster idperson)

   * save raw results 
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_raw_results/health_wellbeing/health_wellbeing", sheet("DHE_PCS1") replace
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))

gen in_sample = e(sample)	

predict p

save "$dir_validation_data/DHE_PCS1_sample", replace


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

putexcel set "$dir_raw_results/health_wellbeing/var_cov", sheet("var_cov") replace
putexcel A1 = matrix(V)

import excel "$dir_raw_results/health_wellbeing/var_cov", sheet("var_cov") clear

describe
local no_vars = `r(k)'	
	
forvalues i = 1/2 {
	egen row_sum = rowtotal(*)
	drop if row_sum == 0 
	drop row_sum
	xpose, clear	
}	
	
mkmat v*, matrix(var)	
putexcel set "$dir_results/reg_health_wellbeing", sheet("UK_DHE_PCS1", replace) modify
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

putexcel set "$dir_results/reg_health_wellbeing", sheet("UK_DHE_PCS1") modify
putexcel A1 = matrix(nonzero_b'), names nformat(number_d2) 


* Labelling 

putexcel A1 = "REGRESSOR"
putexcel A2 = "D_Home_owner_L1"
putexcel A3 = "Dcpst_Single_L1"
putexcel A4 = "Dnc_L1"
putexcel A5 = "Dhe_mcs_L1"
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
putexcel A21 = "Dlltsd01_L1"
putexcel A22 = "Dhe_pcs_L1"
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
putexcel F1 = "Dhe_mcs_L1"
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
putexcel V1 = "Dlltsd01_L1"
putexcel W1 = "Dhe_pcs_L1"
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
putexcel A19 = ("DHE_PCS1") B19 = rmse 

		
drop in_sample p
scalar drop r2_p N chi2 ll	


***************************************************************
* DHE_PCS2_Females: SF12 PCS score 0-100 - causal employment effects *
***************************************************************


*Stage 2
*Female
reghdfe dhe_pcs ///
ib11.exp_emp i.exp_poverty i.exp_incchange D.log_income financial_distress ///
y2020 y2021 ///
L.i.dhh_owned L.i.dcpst L.dnc L.dhe_mcs L.ib8.drgn L.i.ydses_c5 L.dlltsd01 L.dhe_pcs ///
L.dag L.dagsq i.deh_c3 stm ///
if dag>=25 & dag<=64 & dgn==0 ///
[pweight=dimxwt]  ///
, absorb(idperson) vce(cluster idperson)


   * save raw results 
matrix results = r(table)
matrix results = results[1..6,1..10]'
putexcel set "$dir_raw_results/health_wellbeing/health_wellbeing", sheet("DHE_PCS2_Females") replace
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))

gen in_sample = e(sample)	

predict p

save "$dir_validation_data/DHE_PCS2_Females_sample", replace


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

putexcel set "$dir_raw_results/health_wellbeing/var_cov", sheet("var_cov") replace
putexcel A1 = matrix(V)

import excel "$dir_raw_results/health_wellbeing/var_cov", sheet("var_cov") clear

describe
local no_vars = `r(k)'	
	
forvalues i = 1/2 {
	egen row_sum = rowtotal(*)
	drop if row_sum == 0 
	drop row_sum
	xpose, clear	
}	
	
mkmat v*, matrix(var)	
putexcel set "$dir_results/reg_health_wellbeing", sheet("UK_DHE_PCS2_Females", replace) modify
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

putexcel set "$dir_results/reg_health_wellbeing", sheet("UK_DHE_PCS2_Females") modify
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
putexcel A20 = ("DHE_PCS2_Females") B20 = rmse 

		
		
drop in_sample p
scalar drop r2_p N chi2 ll	

***************************************************************
* DHE_PCS2_Males: SF12 PCS score 0-100 - causal employment effects *
***************************************************************


*Stage 2
*Male
reghdfe dhe_pcs ///
ib11.exp_emp i.exp_poverty i.exp_incchange D.log_income financial_distress ///
y2020 y2021 ///
L.i.dhh_owned L.i.dcpst L.dnc L.dhe_mcs L.ib8.drgn L.i.ydses_c5 L.dlltsd01 L.dhe_pcs ///
L.dag L.dagsq i.deh_c3 stm ///
if dag>=25 & dag<=64 & dgn==1 ///
[pweight=dimxwt]  ///
, absorb(idperson) vce(cluster idperson)


   * save raw results 
matrix results = r(table)
matrix results = results[1..6,1..10]'
putexcel set "$dir_raw_results/health_wellbeing/health_wellbeing", sheet("DHE_PCS2_Males") replace
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))

gen in_sample = e(sample)	

predict p

save "$dir_validation_data/DHE_PCS2_Males_sample", replace


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

putexcel set "$dir_raw_results/health_wellbeing/var_cov", sheet("var_cov") replace
putexcel A1 = matrix(V)

import excel "$dir_raw_results/health_wellbeing/var_cov", sheet("var_cov") clear

describe
local no_vars = `r(k)'	
	
forvalues i = 1/2 {
	egen row_sum = rowtotal(*)
	drop if row_sum == 0 
	drop row_sum
	xpose, clear	
}	
	
mkmat v*, matrix(var)	
putexcel set "$dir_results/reg_health_wellbeing", sheet("UK_DHE_PCS2_Males", replace) modify
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

putexcel set "$dir_results/reg_health_wellbeing", sheet("UK_DHE_PCS2_Males") modify
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
putexcel A21 = ("DHE_PCS2_Males") B21 = rmse 

		
drop in_sample p
scalar drop r2_p N chi2 ll	


*******************************************************************************
* DLS1 - Life Satisfaction 1-7 of all working-age adults - baseline effects *
********************************************************************************

reg dls ///
L.i.dhh_owned L.i.dcpst L.dnc L.dhe_pcs L.ib8.drgn L.i.ydses_c5 L.dlltsd01 L.dls ///
L.dag L.dagsq i.deh_c3 i.dot i.dgn stm ///
[pweight=dimxwt]  ///
, vce(cluster idperson)

   * save raw results 
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_raw_results/health_wellbeing/health_wellbeing", sheet("DLS1") replace
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))

gen in_sample = e(sample)	

predict p

save "$dir_validation_data/DLS1_sample", replace


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

putexcel set "$dir_raw_results/health_wellbeing/var_cov", sheet("var_cov") replace
putexcel A1 = matrix(V)

import excel "$dir_raw_results/health_wellbeing/var_cov", sheet("var_cov") clear

describe
local no_vars = `r(k)'	
	
forvalues i = 1/2 {
	egen row_sum = rowtotal(*)
	drop if row_sum == 0 
	drop row_sum
	xpose, clear	
}	
	
mkmat v*, matrix(var)	
putexcel set "$dir_results/reg_health_wellbeing", sheet("UK_DLS1", replace) modify
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

putexcel set "$dir_results/reg_health_wellbeing", sheet("UK_DLS1") modify
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
putexcel A21 = "Dlltsd01_L1"
putexcel A22 = "Dls_L1"
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
putexcel V1 = "Dlltsd01_L1"
putexcel W1 = "Dls_L1"
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
putexcel A22 = ("DLS1") B22 = rmse 

		
drop in_sample p
scalar drop r2_p N chi2 ll	


***************************************************************
* DLS2_Females: Life Satisfaction 1-7 - causal employment effects *
***************************************************************


*Stage 2
*Female
reghdfe dls ///
ib11.exp_emp i.exp_poverty i.exp_incchange D.log_income financial_distress ///
y2020 y2021 ///
L.i.dhh_owned L.i.dcpst L.dnc L.dhe_pcs L.ib8.drgn L.i.ydses_c5 L.dlltsd01 L.dls ///
L.dag L.dagsq i.deh_c3 stm ///
if dag>=25 & dag<=64 & dgn==0 ///
[pweight=dimxwt]  ///
, absorb(idperson) vce(cluster idperson)


   * save raw results 
matrix results = r(table)
matrix results = results[1..6,1..10]'
putexcel set "$dir_raw_results/health_wellbeing/health_wellbeing", sheet("DLS2_Females") replace
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))

gen in_sample = e(sample)	

predict p

save "$dir_validation_data/DLS2_Females_sample", replace


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

putexcel set "$dir_raw_results/health_wellbeing/var_cov", sheet("var_cov") replace
putexcel A1 = matrix(V)

import excel "$dir_raw_results/health_wellbeing/var_cov", sheet("var_cov") clear

describe
local no_vars = `r(k)'	
	
forvalues i = 1/2 {
	egen row_sum = rowtotal(*)
	drop if row_sum == 0 
	drop row_sum
	xpose, clear	
}	
	
mkmat v*, matrix(var)	
putexcel set "$dir_results/reg_health_wellbeing", sheet("UK_DLS2_Females", replace) modify
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

putexcel set "$dir_results/reg_health_wellbeing", sheet("UK_DLS2_Females") modify
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
putexcel A23 = ("DLS2_Females") B23 = rmse 

		
drop in_sample p
scalar drop r2_p N chi2 ll	

***************************************************************
* DLS2_Males: Life Satisfaction 1-7 - causal employment effects *
***************************************************************


*Stage 2
*Male
reghdfe dls ///
ib11.exp_emp i.exp_poverty i.exp_incchange D.log_income financial_distress ///
y2020 y2021 ///
L.i.dhh_owned L.i.dcpst L.dnc L.dhe_pcs L.ib8.drgn L.i.ydses_c5 L.dlltsd01 L.dls ///
L.dag L.dagsq i.deh_c3 stm ///
if dag>=25 & dag<=64 & dgn==1 ///
[pweight=dimxwt]  ///
, absorb(idperson) vce(cluster idperson)


   * save raw results 
matrix results = r(table)
matrix results = results[1..6,1..10]'
putexcel set "$dir_raw_results/health_wellbeing/health_wellbeing", sheet("DLS2_Males") replace
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))

gen in_sample = e(sample)	

predict p

save "$dir_validation_data/DLS2_Males_sample", replace


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

putexcel set "$dir_raw_results/health_wellbeing/var_cov", sheet("var_cov") replace
putexcel A1 = matrix(V)

import excel "$dir_raw_results/health_wellbeing/var_cov", sheet("var_cov") clear

describe
local no_vars = `r(k)'	
	
forvalues i = 1/2 {
	egen row_sum = rowtotal(*)
	drop if row_sum == 0 
	drop row_sum
	xpose, clear	
}	
	
mkmat v*, matrix(var)	
putexcel set "$dir_results/reg_health_wellbeing", sheet("UK_DLS2_Males", replace) modify
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

putexcel set "$dir_results/reg_health_wellbeing", sheet("UK_DLS2_Males") modify
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
putexcel A24 = ("DLS2_Males") B24 = rmse 

		
drop in_sample p
scalar drop r2_p N chi2 ll	


