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

logit econ_dist ///
ib11.exp_emp  i.lhw_c5 D.log_income i.exp_incchange ib0.exp_poverty L.ypncp L.ypnoab ///
L.i.econ_benefits L.i.dhh_owned L.i.dcpst L.dnc L.dhe_pcs L.dhe_mcs L.ib8.drgn L.i.ydses_c5 L.dlltsd  L.econ_dist ///
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

// TODO: update labels

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
/* putexcel A10 = ("HM1_L") B10 = rmse  */ // TODO: make sure this doesn't overwrite existing stuff

drop in_sample p
scalar drop r2_p N chi2 ll	
