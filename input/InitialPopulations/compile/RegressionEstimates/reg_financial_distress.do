********************************************************************************
* PROJECT:		UC and mental health
* SECTION:		Health and wellbeing
* OBJECT: 		Financial distress
* AUTHORS:		Andy Baxter, Erik Igelström
* LAST UPDATE:		08 Jan 2026  
* COUNTRY:		UK 
*
* NOTES:		
********************************************************************************
clear all
set more off
set mem 200m
set maxvar 30000


*******************************************************************
cap log close 
log using "${dir_log}/reg_financial_distress.log", replace
*******************************************************************

use "$dir_ukhls_data/ukhls_pooled_all_obs_09.dta", clear
do "$dir_do/variable_update"



* Sample selection 
drop if dag < 16


xtset idperson swv


**********************************************************************
* HM1_L: GHQ12 score 0-36 of all working-age adults - baseline effects *
**********************************************************************

logit financial_distress ///
ib11.exp_emp  i.lhw_c5 D.log_income i.exp_incchange ib0.exp_poverty L.ypncp L.ypnoab ///
L.i.econ_benefits L.i.dhh_owned L.i.dcpst L.dnc L.dhe_pcs L.dhe_mcs L.ib8.drgn L.i.ydses_c5 L.dlltsd  L.financial_distress ///
i.dgn L.dag L.dagsq i.deh_c3 i.dot stm ///
[pweight=dimxwt]  ///
, vce(cluster idperson)

   * save raw results 
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_raw_results/financial_distress/financial_distress", sheet("UK") replace
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))

gen in_sample = e(sample)	

predict p

save "$dir_validation_data/financial_distress", replace


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

putexcel set "$dir_raw_results/financial_distress/var_cov", sheet("var_cov") replace
putexcel A1 = matrix(V)

import excel "$dir_raw_results/financial_distress/var_cov", sheet("var_cov") clear

describe
local no_vars = `r(k)'	
	
forvalues i = 1/2 {
	egen row_sum = rowtotal(*)
	drop if row_sum == 0 
	drop row_sum
	xpose, clear	
}	
	
mkmat v*, matrix(var)	
putexcel set "$dir_results/reg_financial_distress", sheet("UK") modify
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

putexcel set "$dir_results/reg_financial_distress", sheet("UK") modify
putexcel A1 = matrix(nonzero_b'), names nformat(number_d2) 


* Labelling 

putexcel A1  = "REGRESSOR"
putexcel A2  = "EmployedToUnemployed"       // 13.exp_emp
putexcel A3  = "UnemployedToEmployed"       // 31.exp_emp
putexcel A4  = "PersistentUnemployed"       // 33.exp_emp
putexcel A5  = "Lhw_10"                     // 10.lhw_c5
putexcel A6  = "Lhw_20"                     // 20.lhw_c5
putexcel A7  = "Lhw_30"                     // 30.lhw_c5
putexcel A8  = "Lhw_40"                     // 40.lhw_c5
putexcel A9  = "RealIncomeChange"           // D.log_income
putexcel A10 = "RealIncomeDecrease_D"       // 1.exp_incchange
putexcel A11 = "NonPovertyToPoverty"        // 1.exp_poverty
putexcel A12 = "PovertyToNonPoverty"        // 2.exp_poverty
putexcel A13 = "PersistentPoverty"          // 3.exp_poverty
putexcel A14 = "Ypncp_L1"                   // L.ypncp
putexcel A15 = "Ypnoab_L1"                  // L.ypnoab
putexcel A16 = "D_Econ_benefits"            // 1L.econ_benefits
putexcel A17 = "D_Home_owner_L1"            // 1L.dhh_owned
putexcel A18 = "Dcpst_Single_L1"            // 2L.dcpst
putexcel A19 = "Dnc_L1"                     // L.dnc
putexcel A20 = "Dhe_pcs_L1"                 // L.dhe_pcs
putexcel A21 = "Dhe_mcs_L1"                 // L.dhe_mcs
putexcel A22 = "UKC"                        // 1L.drgn1
putexcel A23 = "UKD"                        // 2L.drgn1
putexcel A24 = "UKE"                        // 4L.drgn1
putexcel A25 = "UKF"                        // 5L.drgn1
putexcel A26 = "UKG"                        // 6L.drgn1
putexcel A27 = "UKH"                        // 7L.drgn1
putexcel A28 = "UKJ"                        // 9L.drgn1
putexcel A29 = "UKK"                        // 10L.drgn1
putexcel A30 = "UKL"                        // 11L.drgn1
putexcel A31 = "UKM"                        // 12L.drgn1
putexcel A32 = "UKN"                        // 13L.drgn1
putexcel A33 = "Ydses_c5_Q2_L1"             // 2L.ydses_c5
putexcel A34 = "Ydses_c5_Q3_L1"             // 3L.ydses_c5
putexcel A35 = "Ydses_c5_Q4_L1"             // 4L.ydses_c5
putexcel A36 = "Ydses_c5_Q5_L1"             // 5L.ydses_c5
putexcel A37 = "Dlltsd_L1"                  // L.dlltsd
putexcel A38 = "FinancialDistress"          // L.financial_distress
putexcel A39 = "Dgn"                        // 1.dgn
putexcel A40 = "Dag_L1"                     // L.dag
putexcel A41 = "Dag_sq_L1"                  // L.dagsq
putexcel A42 = "Deh_c3_Medium"              // 2.deh_c3
putexcel A43 = "Deh_c3_Low"                 // 3.deh_c3
putexcel A44 = "EthnicityAsian"             // 2.dot
putexcel A45 = "EthnicityBlack"             // 3.dot
putexcel A46 = "EthnicityOther"             // 4.dot
putexcel A47 = "Year_transformed"           // stm
putexcel A48 = "Constant"                   // _cons

putexcel B1 =  "COEFFICIENT"
putexcel C1  = "EmployedToUnemployed"       // 13.exp_emp
putexcel D1  = "UnemployedToEmployed"       // 31.exp_emp
putexcel E1  = "PersistentUnemployed"       // 33.exp_emp
putexcel F1  = "Lhw_10"                     // 10.lhw_c5
putexcel G1  = "Lhw_20"                     // 20.lhw_c5
putexcel H1  = "Lhw_30"                     // 30.lhw_c5
putexcel I1  = "Lhw_40"                     // 40.lhw_c5
putexcel J1  = "RealIncomeChange"           // D.log_income
putexcel K1  = "RealIncomeDecrease_D"       // 1.exp_incchange
putexcel L1  = "NonPovertyToPoverty"        // 1.exp_poverty
putexcel M1  = "PovertyToNonPoverty"        // 2.exp_poverty
putexcel N1  = "PersistentPoverty"          // 3.exp_poverty
putexcel O1  = "Ypncp_L1"                   // L.ypncp
putexcel P1  = "Ypnoab_L1"                  // L.ypnoab
putexcel Q1  = "D_Econ_benefits"            // 1L.econ_benefits
putexcel R1  = "D_Home_owner_L1"            // 1L.dhh_owned
putexcel S1  = "Dcpst_Single_L1"            // 2L.dcpst
putexcel T1  = "Dnc_L1"                     // L.dnc
putexcel U1  = "Dhe_pcs_L1"                 // L.dhe_pcs
putexcel V1  = "Dhe_mcs_L1"                 // L.dhe_mcs
putexcel W1  = "UKC"                        // 1L.drgn1
putexcel X1  = "UKD"                        // 2L.drgn1
putexcel Y1  = "UKE"                        // 4L.drgn1
putexcel Z1  = "UKF"                        // 5L.drgn1
putexcel AA1 = "UKG"                        // 6L.drgn1
putexcel AB1 = "UKH"                        // 7L.drgn1
putexcel AC1 = "UKJ"                        // 9L.drgn1
putexcel AD1 = "UKK"                        // 10L.drgn1
putexcel AE1 = "UKL"                        // 11L.drgn1
putexcel AF1 = "UKM"                        // 12L.drgn1
putexcel AG1 = "UKN"                        // 13L.drgn1
putexcel AH1 = "Ydses_c5_Q2_L1"             // 2L.ydses_c5
putexcel AI1 = "Ydses_c5_Q3_L1"             // 3L.ydses_c5
putexcel AJ1 = "Ydses_c5_Q4_L1"             // 4L.ydses_c5
putexcel AK1 = "Ydses_c5_Q5_L1"             // 5L.ydses_c5
putexcel AL1 = "Dlltsd_L1"                  // L.dlltsd
putexcel AM1 = "FinancialDistress"          // L.financial_distress
putexcel AN1 = "Dgn"                        // 1.dgn
putexcel AO1 = "Dag_L1"                     // L.dag
putexcel AP1 = "Dag_sq_L1"                  // L.dagsq
putexcel AQ1 = "Deh_c3_Medium"              // 2.deh_c3
putexcel AR1 = "Deh_c3_Low"                 // 3.deh_c3
putexcel AS1 = "EthnicityAsian"             // 2.dot
putexcel AT1 = "EthnicityBlack"             // 3.dot
putexcel AU1 = "EthnicityOther"             // 4.dot
putexcel AV1 = "Year_transformed"           // stm
putexcel AW1 = "Constant"                   // _cons
		
drop in_sample p
scalar drop r2_p N chi2 ll	
