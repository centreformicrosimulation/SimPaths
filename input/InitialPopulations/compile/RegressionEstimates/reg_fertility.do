********************************************************************************
* PROJECT:  		ESPON 
* SECTION:			Fertility
* OBJECT: 			Final Probit Models
* AUTHORS:			Daria Popova, Justin van de Ven
* LAST UPDATE:		1 July 2025 DP  
* COUNTRY: 			UK 
*
* NOTES:			    Simplified the fertility process for those in this initial 
* 						education spell.  
********************************************************************************
clear all
set more off
set mem 200m
set type double
//set maxvar 120000
set maxvar 30000

*******************************************************************
cap log close 
log using "${dir_log}/reg_fertility.log", replace
*******************************************************************
use "$dir_ukhls_data/ukhls_pooled_all_obs_09.dta", clear

do "$dir_do/variable_update"


* sample selection 
drop if dag < 16


* Set Excel file 

* Info sheet

putexcel set "$dir_results/reg_fertility", sheet("Info") replace
putexcel A1 = "Description:"
putexcel B1 = "Model parameters governing projection of fertility"
putexcel A2 = "Authors:	Patryk Bronka, Justin van de Ven, Daria Popova" 
putexcel A3 = "Last edit: 1 July 2025 DP"

putexcel A4 = "Process:", bold
putexcel B4 = "Description:", bold
putexcel A5 = "F1a"
putexcel B5 = "Probit regression estimates of the probability of  having a child for women aged 18-44 in initial education spell"
putexcel A6 = "F1b"
putexcel B6 = "Probit regression estimates of probability of having a child for women aged 18-44 not in initial education spell"

putexcel A10 = "Notes:", bold
putexcel B10 = "All processes: replaced dhe with dhe_pcs and dhe_mcs, added ethnicity-4 cat (dot), covid dummies (y2020 y2021)"
putexcel B11 = "F1a: only 24 obs having a child when in initial education spell, therefore have to take away some covariates to obtain estimate"


putexcel set "$dir_results/reg_fertility", sheet("Gof") modify
putexcel A1 = "Goodness of fit", bold		

xtset idperson swv

**********************************************
* F1a - Having a child, in initial edu spell * 
**********************************************

* Process F1a: Probabiltiy of having a child 
* Sample: Women aged 18-44, in initial education spell education.
* DV: New born child dummy (note that in the estimation sample dchpd contains the number of newborn children, which could be >1) 

replace dchpd=1 if dchpd>1 & dchpd<. 
// only 69 ppl meet the condition in total
tab dchpd if (sprfm == 1 & ded == 1) 

/*/////////////////////////////////////////////////////////////////////////////////////////////////	 
//check weights //////////////////////////////////////////////////////////////////////////////////	 
probit dchpd dag /*dhe dhe_mcs dhe_pcs*/ ib1.dcpst stm /*y2020 y2021*/ i.dot if ///
    sprfm == 1 & ded == 1 [pweight=dimlwt], vce(robust)
outreg2 using "${weight_checks}/weight_comparison_F1a.xls", alpha(0.001, 0.01, 0.05, 0.1) symbol(***, **, *, +) replace ctitle(F1a, dimlwt) side dec(4) 

probit dchpd dag /*dhe dhe_mcs dhe_pcs*/ ib1.dcpst stm /*y2020 y2021*/ i.dot if ///
    sprfm == 1 & ded == 1 [pweight=disclwt], vce(robust)
outreg2 using "${weight_checks}/weight_comparison_F1a.xls", alpha(0.001, 0.01, 0.05, 0.1) symbol(***, **, *, +) append ctitle(F1a, disclwt) side dec(4)

probit dchpd dag /*dhe dhe_mcs dhe_pcs*/ ib1.dcpst stm /*y2020 y2021*/ i.dot if ///
    sprfm == 1 & ded == 1 [pweight=dimxwt], vce(robust)
outreg2 using "${weight_checks}/weight_comparison_F1a.xls", alpha(0.001, 0.01, 0.05, 0.1) symbol(***, **, *, +) append ctitle(F1a, dimxwt) side dec(4) 
erase "${weight_checks}/weight_comparison_F1a.txt"
//////////////////////////////////////////////////////////////////////////////////////////////////// 
////////////////////////////////////////////////////////////////////////////////////////////////////
*/

probit dchpd dag /*dhe dhe_mcs dhe_pcs*/ ib1.dcpst stm /*y2020 y2021*/ i.dot if ///
    sprfm == 1 & ded == 1 [pweight=dimxwt], vce(robust)


* raw results 
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_raw_results/fertility/fertility", sheet("Process F1a - In education") replace
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))
outreg2 stats(coef se pval) using "$dir_raw_results/fertility/F1a.doc", replace ///
title("Process F1a: Probability of giving birth to a child. Sample: Women aged 18-44 in initial education spell.") ///
 ctitle(Giving birth) label side dec(2) noparen addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll))
	

gen in_sample = e(sample)	

predict p

save "$dir_validation_data/F1a_sample", replace

scalar r2_p = e(r2_p) 
scalar N = e(N)	
scalar chi2 = e(chi2)
scalar ll = e(ll)	


* Results 	
* Note: Zeros eliminated 
	
matrix b = e(b)	
matrix V = e(V)


*  Store variance-covariance matrix 

preserve

putexcel set "$dir_raw_results/fertility/var_cov", sheet("var_cov") replace
putexcel A1 = matrix(V)

import excel "$dir_raw_results/fertility/var_cov", sheet("var_cov") clear

describe
local no_vars = `r(k)'	
	
forvalues i = 1/2 {
	egen row_sum = rowtotal(*)
	drop if row_sum == 0 
	drop row_sum
	xpose, clear	
}	
	
mkmat v*, matrix(var)	
putexcel set "$dir_results/reg_fertility", sheet("F1a") modify
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

putexcel set "$dir_results/reg_fertility", sheet("F1a") modify
putexcel A1 = matrix(nonzero_b'), names nformat(number_d2) 
	
	
* Labelling	

putexcel A1 = "REGRESSOR"
putexcel A2 = "Dag"
putexcel A3 = "Dcpst_Single"
putexcel A4 = "Year_transformed"
putexcel A5 = "Ethn_Black"
putexcel A6 = "Constant"

putexcel B1 = "COFFICIENT"
putexcel C1 = "Dag"
putexcel D1 = "Dcpst_Single"
putexcel E1 = "Year_transformed"
putexcel F1 = "Ethn_Black"	
putexcel G1 = "Constant"	

	
* Goodness of fit

putexcel set "$dir_results/reg_fertility", sheet("Gof") modify

putexcel A3 = "F1a - Fertility in initial education spell", bold		

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

************************************************
* F1b - Having a child, left initial edu spell *
************************************************

* Process F1b: Probabiltiy of having a child 
* Sample:	Women aged 18-44, left initial education spell
* DV:	New born child dummy 

tab dchpd if (sprfm == 1 & ded == 0) 

/*/////////////////////////////////////////////////////////////////////////////////////////////////	 
//check weights //////////////////////////////////////////////////////////////////////////////////	 
probit dchpd dag dagsq li.ydses_c5 l.dnc l.dnc02 /*ib1.dhe*/ dhe_pcs dhe_mcs /*ib1.dcpst*/ ///
    lib1.dcpst ib1.deh_c3 dukfr li.les_c3 ib8.drgn1 stm y2020 y2021 i.dot if ///
    (sprfm == 1 & ded == 0) [pweight=dimlwt], vce(robust)
outreg2 using "${weight_checks}/weight_comparison_F1b.xls", alpha(0.001, 0.01, 0.05, 0.1) symbol(***, **, *, +) replace ctitle(F1b, dimlwt) side dec(4) 

probit dchpd dag dagsq li.ydses_c5 l.dnc l.dnc02 /*ib1.dhe*/ dhe_pcs dhe_mcs /*ib1.dcpst*/ ///
    lib1.dcpst ib1.deh_c3 dukfr li.les_c3 ib8.drgn1 stm y2020 y2021 i.dot if ///
    (sprfm == 1 & ded == 0) [pweight=disclwt], vce(robust)
outreg2 using "${weight_checks}/weight_comparison_F1b.xls", alpha(0.001, 0.01, 0.05, 0.1) symbol(***, **, *, +) append ctitle(F1b, disclwt) side dec(4)

probit dchpd dag dagsq li.ydses_c5 l.dnc l.dnc02 /*ib1.dhe*/ dhe_pcs dhe_mcs /*ib1.dcpst*/ ///
    lib1.dcpst ib1.deh_c3 dukfr li.les_c3 ib8.drgn1 stm y2020 y2021 i.dot if ///
    (sprfm == 1 & ded == 0) [pweight=dimxwt], vce(robust)
outreg2 using "${weight_checks}/weight_comparison_F1b.xls", alpha(0.001, 0.01, 0.05, 0.1) symbol(***, **, *, +) append ctitle(F1b, dimxwt) side dec(4) 
erase "${weight_checks}/weight_comparison_F1b.txt"
//////////////////////////////////////////////////////////////////////////////////////////////////// 
////////////////////////////////////////////////////////////////////////////////////////////////////
*/

probit dchpd dag dagsq li.ydses_c5 l.dnc l.dnc02 /*ib1.dhe*/ dhe_pcs dhe_mcs /*ib1.dcpst*/ ///
    lib1.dcpst ib1.deh_c3 dukfr li.les_c3 ib8.drgn1 stm y2020 y2021 i.dot if ///
    (sprfm == 1 & ded == 0) [pweight=dimxwt], vce(robust)

	* raw results 
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_raw_results/fertility/fertility", sheet("Process F1b - Not in education") modify
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))
outreg2 stats(coef se pval) using "$dir_raw_results/fertility/F1b.doc", replace ///
title("Process F1b: Probability of giving birth to a child. Sample: Women aged 18-44 not in initial education spell.") ///
 ctitle(Giving birth) label side dec(2) noparen addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll))

 
gen in_sample = e(sample)	

predict p

save "$dir_validation_data/F1b_sample", replace

scalar r2_p = e(r2_p) 
scalar N = e(N)	 
scalar chi2 = e(chi2)
scalar ll = e(ll)

	
* Results 
* Note: Zeros eliminated 
	
matrix b = e(b)	
matrix V = e(V)


* Store variance-covariance matrix 

preserve

putexcel set "$dir_raw_results/fertility/var_cov", sheet("var_cov") replace
putexcel A1 = matrix(V)

import excel "$dir_raw_results/fertility/var_cov", sheet("var_cov") clear

describe
local no_vars = `r(k)'	
	
forvalues i = 1/2 {
	egen row_sum = rowtotal(*)
	drop if row_sum == 0 
	drop row_sum
	xpose, clear	
}	
	
mkmat v*, matrix(var)	
putexcel set "$dir_results/reg_fertility", sheet("F1b") modify
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

putexcel set "$dir_results/reg_fertility", sheet("F1b") modify
putexcel A1 = matrix(nonzero_b'), names nformat(number_d2) 	
 
 
* Labelling 
 
putexcel A1 = "REGRESSOR"
putexcel A2 = "Dag"
putexcel A3 = "Dag_sq"
putexcel A4 = "Ydses_c5_Q2_L1"
putexcel A5 = "Ydses_c5_Q3_L1"
putexcel A6 = "Ydses_c5_Q4_L1"
putexcel A7 = "Ydses_c5_Q5_L1"
putexcel A8 = "Dnc_L1"
putexcel A9 = "Dnc02_L1"
putexcel A10 = "Dhe_pcs"
putexcel A11 = "Dhe_mcs"
putexcel A12 = "Dcpst_Single_L1"
putexcel A13 = "Dcpst_PreviouslyPartnered_L1"
putexcel A14 = "Deh_c3_Medium"
putexcel A15 = "Deh_c3_Low"
putexcel A16 = "FertilityRate"
putexcel A17 = "Les_c3_Student_L1"
putexcel A18 = "Les_c3_NotEmployed_L1"
putexcel A19 = "UKC"
putexcel A20 = "UKD"
putexcel A21 = "UKE"
putexcel A22 = "UKF"
putexcel A23 = "UKG"
putexcel A24 = "UKH"
putexcel A25 = "UKJ"
putexcel A26 = "UKK"
putexcel A27 = "UKL"
putexcel A28 = "UKM"
putexcel A29 = "UKN"
putexcel A30 = "Year_transformed"
putexcel A31 = "Y2020"
putexcel A32 = "Y2021"
putexcel A33 = "Ethn_Asian"
putexcel A34 = "Ethn_Black"
putexcel A35 = "Ethn_Other"
putexcel A36 = "Constant"

putexcel B1 = "COFFICIENT"
putexcel C1 = "Dag"
putexcel D1 = "Dag_sq"
putexcel E1 = "Ydses_c5_Q2_L1"
putexcel F1 = "Ydses_c5_Q3_L1"
putexcel G1 = "Ydses_c5_Q4_L1"
putexcel H1 = "Ydses_c5_Q5_L1"
putexcel I1 = "Dnc_L1"
putexcel J1 = "Dnc02_L1"
putexcel K1 = "Dhe_pcs"
putexcel L1 = "Dhe_mcs"
putexcel M1 = "Dcpst_Single_L1"
putexcel N1 = "Dcpst_PreviouslyPartnered_L1"
putexcel O1 = "Deh_c3_Medium"
putexcel P1 = "Deh_c3_Low"
putexcel Q1 = "FertilityRate"
putexcel R1 = "Les_c3_Student_L1"
putexcel S1 = "Les_c3_NotEmployed_L1"
putexcel T1 = "UKC"
putexcel U1 = "UKD"
putexcel V1 = "UKE"
putexcel W1 = "UKF"
putexcel X1 = "UKG"
putexcel Y1 = "UKH"
putexcel Z1 = "UKJ"
putexcel AA1 = "UKK"
putexcel AB1 = "UKL"
putexcel AC1 = "UKM"
putexcel AD1 = "UKN"
putexcel AE1 = "Year_transformed"
putexcel AF1 = "Y2020"
putexcel AG1 = "Y2021"
putexcel AH1 = "Ethn_Asian"
putexcel AI1 = "Ethn_Black"
putexcel AJ1 = "Ethn_Other"
putexcel AK1 = "Constant"

 
* Goodness of fit

putexcel set "$dir_results/reg_fertility", sheet("Gof") modify

putexcel A9 = "F1b - Fertility left initial education spell", bold		

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
 
 
capture log close 

