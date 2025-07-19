********************************************************************************
* PROJECT:  		ESPON
* SECTION:			Home ownership 
* OBJECT: 			Final Regresion Models - Weighted
* AUTHORS:			Daria Popova, Justin van de Ven
* LAST UPDATE:		15 May 2025 DP  
* COUNTRY: 			UK
*
* NOTES: 			Removed spousal education to include singles, combined it with hh composition instead, added lagged home ownership as a predictor 
*                  
********************************************************************************
clear all
set more off
set mem 200m
set type double
//set maxvar 120000
set maxvar 30000

*******************************************************************
cap log close 
log using "${dir_log}/reg_home_ownership.log", replace
*******************************************************************

use "$dir_ukhls_data/ukhls_pooled_all_obs_09.dta", clear

do "$dir_do/variable_update"


*sample selection 
drop if dag < 16


xtset idperson swv


* Set Excel file 

* Info sheet

putexcel set "$dir_results/reg_home_ownership", sheet("Info") replace
putexcel A1 = "Description:"
putexcel B1 = "Model parameters governing projection of home ownership"
putexcel A2 = "Authors:	Patryk Bronka, Justin van de Ven, Daria Popova" 
putexcel A3 = "Last edit: 1 July 2025 DP"

putexcel A4 = "Process:", bold
putexcel B4 = "Description:", bold
putexcel A5 = "HO1a"
putexcel B5 = "Probit regression estimates of the probability of being a home owner, aged 18+"

putexcel A10 = "Notes:", bold
putexcel B10 = "Have combined dhhtp_c4 and lessp_c3 into a single variable with 8 categories, dhhtp_c8"
putexcel B11 = "Added lagged home ownership, replaced dhe with dhe_pcs and dhe_mcs, added ethnicity (dot) and covid dummies (y2020 2021)"

putexcel set "$dir_results/reg_home_ownership", sheet("Gof") modify
putexcel A1 = "Goodness of fit", bold		


************************
* HO1a: Home ownership *
************************

* Process HO1a: Probability of being a home owner 
* Sample: Individuals aged 18+
* DV: Home ownerhip dummy

fre dhh_owned if dag >= 18

/*/////////////////////////////////////////////////////////////////////////////////////////////////	 
//check weights //////////////////////////////////////////////////////////////////////////////////	 
probit dhh_owned dgn dag dagsq il.dhhtp_c8 il.les_c3 ///
i.deh_c3 /*il.dhe*/ l.dhe_mcs l.dhe_pcs il.ydses_c5 l.yptciihs_dv l.dhh_owned ib8.drgn1 stm y2020 y2021 i.dot if ///
dag>=18 [pweight=dimlwt], vce(robust)
outreg2 using "${weight_checks}/weight_comparison_HO1a.xls", alpha(0.001, 0.01, 0.05, 0.1) symbol(***, **, *, +) replace ctitle(HO1a, dimlwt) side dec(4) 

probit dhh_owned dgn dag dagsq il.dhhtp_c8 il.les_c3 ///
i.deh_c3 /*il.dhe*/ l.dhe_mcs l.dhe_pcs il.ydses_c5 l.yptciihs_dv l.dhh_owned ib8.drgn1 stm y2020 y2021 i.dot if ///
dag>=18 [pweight=disclwt], vce(robust)
outreg2 using "${weight_checks}/weight_comparison_HO1a.xls", alpha(0.001, 0.01, 0.05, 0.1) symbol(***, **, *, +) append ctitle(HO1a, disclwt) side dec(4)

probit dhh_owned dgn dag dagsq il.dhhtp_c8 il.les_c3 ///
i.deh_c3 /*il.dhe*/ l.dhe_mcs l.dhe_pcs il.ydses_c5 l.yptciihs_dv l.dhh_owned ib8.drgn1 stm y2020 y2021 i.dot if ///
dag>=18 [pweight=dimxwt], vce(robust)
outreg2 using "${weight_checks}/weight_comparison_HO1a.xls", alpha(0.001, 0.01, 0.05, 0.1) symbol(***, **, *, +) append ctitle(HO1a, dimxwt) side dec(4) 
erase "${weight_checks}/weight_comparison_HO1a.txt"
//////////////////////////////////////////////////////////////////////////////////////////////////// 
////////////////////////////////////////////////////////////////////////////////////////////////////	
*/	
	
probit dhh_owned dgn dag dagsq il.dhhtp_c8 il.les_c3 ///
i.deh_c3 /*il.dhe*/ l.dhe_mcs l.dhe_pcs il.ydses_c5 l.yptciihs_dv l.dhh_owned ib8.drgn1 stm y2020 y2021 i.dot if ///
dag>=18 [pweight=dimxwt], vce(cluster idperson)


* raw results 
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_raw_results/home_ownership/homeownership", sheet("Process HO1a") replace
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))
outreg2 stats(coef se pval) using "$dir_raw_results/home_ownership/HO1a.doc", replace ///
title("Process HO1a: Probability of being a home owner - individuals aged 18+") ///
 ctitle(home owner) label side dec(2) noparen addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll))
gen in_sample = e(sample)	

predict p

save "$dir_validation_data/HO1a_sample", replace

scalar r2_p = e(r2_p) 
scalar N = e(N)	
scalar chi2 = e(chi2)
scalar ll = e(ll)	


* Results 	
* Note: Zeros values are eliminated 
	
matrix b = e(b)	
matrix V = e(V)


*  Store variance-covariance matrix 

preserve

putexcel set "$dir_raw_results/home_ownership/var_cov", sheet("var_cov") ///
	replace
putexcel A1 = matrix(V)

import excel "$dir_raw_results/home_ownership/var_cov", sheet("var_cov") clear

describe
local no_vars = `r(k)'	
	
forvalues i = 1/2 {
	egen row_sum = rowtotal(*)
	drop if row_sum == 0 
	drop row_sum
	xpose, clear	
}	
	
mkmat v*, matrix(var)	
putexcel set "$dir_results/reg_home_ownership", sheet("HO1a") modify 
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

putexcel set "$dir_results/reg_home_ownership", sheet("HO1a") modify
putexcel A1 = matrix(nonzero_b'), names nformat(number_d2) 	
	

* Labelling 
 
putexcel A1 = "REGRESSOR"
putexcel A2 = "Dgn"
putexcel A3 = "Dag"
putexcel A4 = "Dag_sq"
putexcel A5 = "Dhhtp_c8_2_L1"
putexcel A6 = "Dhhtp_c8_3_L1"
putexcel A7 = "Dhhtp_c8_4_L1"
putexcel A8 = "Dhhtp_c8_5_L1"
putexcel A9 = "Dhhtp_c8_6_L1"
putexcel A10 = "Dhhtp_c8_7_L1"
putexcel A11 = "Dhhtp_c8_8_L1"
putexcel A12 = "Les_c3_Student_L1"
putexcel A13 = "Les_c3_NotEmployed_L1"
putexcel A14 = "Deh_c3_Medium"
putexcel A15 = "Deh_c3_Low"
putexcel A16 = "Dhe_mcs"
putexcel A17 = "Dhe_pcs"
putexcel A18 = "Ydses_c5_Q2_L1"
putexcel A19 = "Ydses_c5_Q3_L1"
putexcel A20 = "Ydses_c5_Q4_L1"
putexcel A21 = "Ydses_c5_Q5_L1"
putexcel A22 = "Yptciihs_dv_L1"
putexcel A23 = "Dhh_owned_L1"
putexcel A24 = "UKC"
putexcel A25 = "UKD"
putexcel A26 = "UKE"
putexcel A27 = "UKF"
putexcel A28 = "UKG"
putexcel A29 = "UKH"
putexcel A30 = "UKJ"
putexcel A31 = "UKK"
putexcel A32 = "UKL"
putexcel A33 = "UKM"
putexcel A34 = "UKN"
putexcel A35 = "Year_transformed"
putexcel A36 = "Y2020"
putexcel A37 = "Y2021"
putexcel A38 = "Ethn_Asian"
putexcel A39 = "Ethn_Black"
putexcel A40 = "Ethn_Other"
putexcel A41 = "Constant"

putexcel B1 = "COFFICIENT"
putexcel C1 = "Dgn"
putexcel D1 = "Dag"
putexcel E1 = "Dag_sq"
putexcel F1 = "Dhhtp_c8_2_L1"
putexcel G1 = "Dhhtp_c8_3_L1"
putexcel H1 = "Dhhtp_c8_4_L1"
putexcel I1 = "Dhhtp_c8_5_L1"
putexcel J1 = "Dhhtp_c8_6_L1"
putexcel K1 = "Dhhtp_c8_7_L1"
putexcel L1 = "Dhhtp_c8_8_L1"
putexcel M1 = "Les_c3_Student_L1"
putexcel N1 = "Les_c3_NotEmployed_L1"
putexcel O1 = "Deh_c3_Medium"
putexcel P1 = "Deh_c3_Low"
putexcel Q1 = "Dhe_mcs"
putexcel R1 = "Dhe_pcs"
putexcel S1 = "Ydses_c5_Q2_L1"
putexcel T1 = "Ydses_c5_Q3_L1"
putexcel U1 = "Ydses_c5_Q4_L1"
putexcel V1 = "Ydses_c5_Q5_L1"
putexcel W1 = "Yptciihs_dv_L1"
putexcel X1 = "Dhh_owned_L1"
putexcel Y1 = "UKC"
putexcel Z1 = "UKD"
putexcel AA1 = "UKE"
putexcel AB1 = "UKF"
putexcel AC1 = "UKG"
putexcel AD1 = "UKH"
putexcel AE1 = "UKJ"
putexcel AF1 = "UKK"
putexcel AG1 = "UKL"
putexcel AH1 = "UKM"
putexcel AI1 = "UKN"
putexcel AJ1 = "Year_transformed"
putexcel AK1 = "Y2020"
putexcel AL1 = "Y2021"
putexcel AM1 = "Ethn_Asian"
putexcel AN1 = "Ethn_Black"
putexcel AO1 = "Ethn_Other"
putexcel AP1 = "Constant"


* Goodness of fit

putexcel set "$dir_results/reg_home_ownership", sheet("Gof") modify

putexcel A3 = "HO1a - Home ownership", bold		

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

capture log close 

