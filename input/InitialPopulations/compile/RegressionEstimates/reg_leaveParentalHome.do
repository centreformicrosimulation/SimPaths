********************************************************************************
* PROJECT:  		ESPON
* SECTION:			Leaving Parental Home
* OBJECT: 			Final Probit Regression Model 
* AUTHORS:			Daria Popova, Justin van de Ven
* LAST UPDATE:		1 July 2025 DP  
* COUNTRY: 			UK  
* 
* NOTES: 			
**********************************************************************************

clear all
set more off
set mem 200m
set type double
//set maxvar 120000
set maxvar 30000


*******************************************************************
cap log close 
log using "${dir_log}/reg_leaveParentalHome.log", replace
*******************************************************************

use "$dir_ukhls_data/ukhls_pooled_all_obs_09.dta", clear

do "$dir_do/variable_update"

* sample selection 
drop if dag < 16

 
xtset idperson swv


* Set Excel file 

* Info sheet

putexcel set "$dir_work/reg_leave_parental_home", sheet("Info") replace
putexcel A1 = "Description:"
putexcel B1 = "Model parameters governing leaving parental home"
putexcel A2 = "Authors:	Patryk Bronka, Justin van de Ven, Daria Popova" 
putexcel A3 = "Last edit: 1 July 2025 DP"

putexcel A4 = "Process:", bold
putexcel B4 = "Description:", bold
putexcel A5 = "P1a"
putexcel B5 = "Probit regression estimates for leaving the parental home - 18+, not in intitial education spell, living with parents in t-1"

putexcel A10 = "Notes:", bold
putexcel B10 = "Added: ethnicity-4 cat (dot); covid dummies (y2020 y2021); not partnered condition (dcpst != 1) to be consistent with the simulation"

putexcel set "$dir_work/reg_leave_parental_home", sheet("Gof") modify
putexcel A1 = "Goodness of fit", bold		


************************************
* Process P1a: Leave Parental Home *
************************************

* Process P1a: Probability of leaving the parental home. 
* Sample: All respondents living with a parent in t-1, aged 18+, not in initial 
* 			education spell 
* DV: Left parental home dummy of those who lived with parents in t-1
* Note: Added not partnered condition as well to be consistent with the simulation	
fre dlftphm if (ded == 0 & dag >= 18 & dcpst != 1) //3.65%
 
/*/////////////////////////////////////////////////////////////////////////////////////////////////	 
//check weights //////////////////////////////////////////////////////////////////////////////////	 
probit dlftphm i.dgn dag dagsq ib1.deh_c3 li.les_c3 li.ydses_c5 ib8.drgn1 stm y2020 y2021 i.dot ///
    if (ded==0 & dag>=18 & l.dlftphm==0 & dcpst != 1) [pweight=dimlwt], vce(robust)
outreg2 using "${weight_checks}/weight_comparison_P1a.xls", alpha(0.001, 0.01, 0.05, 0.1) symbol(***, **, *, +) replace ctitle(P1a, dimlwt) side dec(4) 

probit dlftphm i.dgn dag dagsq ib1.deh_c3 li.les_c3 li.ydses_c5 ib8.drgn1 stm y2020 y2021 i.dot ///
    if (ded==0 & dag>=18 & l.dlftphm==0 & dcpst != 1) [pweight=disclwt], vce(robust)
outreg2 using "${weight_checks}/weight_comparison_P1a.xls", alpha(0.001, 0.01, 0.05, 0.1) symbol(***, **, *, +) append ctitle(P1a, disclwt) side dec(4)

probit dlftphm i.dgn dag dagsq ib1.deh_c3 li.les_c3 li.ydses_c5 ib8.drgn1 stm y2020 y2021 i.dot ///
    if (ded==0 & dag>=18 & l.dlftphm==0 & dcpst != 1) [pweight=dimxwt], vce(robust)
outreg2 using "${weight_checks}/weight_comparison_P1a.xls", alpha(0.001, 0.01, 0.05, 0.1) symbol(***, **, *, +) append ctitle(P1a, dimxwt) side dec(4) 
erase "${weight_checks}/weight_comparison_P1a.txt"
//////////////////////////////////////////////////////////////////////////////////////////////////// 
////////////////////////////////////////////////////////////////////////////////////////////////////
*/

probit dlftphm i.dgn dag dagsq ib1.deh_c3 li.les_c3 li.ydses_c5 ib8.drgn1 stm y2020 y2021 i.dot ///
    if (ded==0 & dag>=18 & l.dlftphm==0 & dcpst != 1) [pweight=dimxwt], vce(robust)	
	
	
	* save raw results 	
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_raw_results/leave_parental_home/leave_parental_home", sheet("Process P1a") replace
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))
outreg2 stats(coef se pval) using "$dir_raw_results/leave_parental_home/P1a.doc", replace ///
title("Process P1a: Probability of leaving the parental home. Sample: All respondents living with a parent and not in initial education spell.") ///
 ctitle(Leave parental home) label side dec(2) noparen addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll))		
gen in_sample = e(sample)


predict p 

save "$dir_validation_data/P1a_sample", replace
	
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

putexcel set "$dir_raw_results/leave_parental_home/var_cov", sheet("var_cov") ///
	replace
putexcel A1 = matrix(V)

import excel "$dir_raw_results/leave_parental_home/var_cov", sheet("var_cov") ///
	clear

describe
local no_vars = `r(k)'	
	
forvalues i = 1/2 {
	egen row_sum = rowtotal(*)
	drop if row_sum == 0 
	drop row_sum
	xpose, clear	
}	
	
mkmat v*, matrix(var)	
putexcel set "$dir_results/reg_leave_parental_home", sheet("P1a") modify
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

putexcel set "$dir_results/reg_leave_parental_home", sheet("P1a") modify
putexcel A1 = matrix(nonzero_b'), names //nformat(number_d2) 
	
	
* Labeling 

putexcel A1 = "REGRESSOR"
putexcel A2 = "Dgn"
putexcel A3 = "Dag"
putexcel A4 = "Dag_sq"
putexcel A5 = "Deh_c3_Medium"
putexcel A6 = "Deh_c3_Low"
putexcel A7 = "Les_c3_Student_L1"
putexcel A8 = "Les_c3_NotEmployed_L1"
putexcel A9 = "Ydses_c5_Q2_L1"
putexcel A10 = "Ydses_c5_Q3_L1"
putexcel A11 = "Ydses_c5_Q4_L1"
putexcel A12 = "Ydses_c5_Q5_L1"
putexcel A13 = "UKC"
putexcel A14 = "UKD"
putexcel A15 = "UKE"
putexcel A16 = "UKF"
putexcel A17 = "UKG"
putexcel A18 = "UKH"
putexcel A19 = "UKJ"
putexcel A20 = "UKK"
putexcel A21 = "UKL"
putexcel A22 = "UKM"
putexcel A23 = "UKN"
putexcel A24 = "Year_transformed"
putexcel A25 = "Y2020"
putexcel A26 = "Y2021"
putexcel A27 = "Ethn_Asian"
putexcel A28 = "Ethn_Black"
putexcel A29 = "Ethn_Other"
putexcel A30 = "Constant"

putexcel B1 = "COEFFICIENT"
putexcel C1 = "Dgn"
putexcel D1 = "Dag"
putexcel E1 = "Dag_sq"
putexcel F1 = "Deh_c3_Medium"
putexcel G1 = "Deh_c3_Low"
putexcel H1 = "Les_c3_Student_L1"
putexcel I1 = "Les_c3_NotEmployed_L1"
putexcel J1 = "Ydses_c5_Q2_L1"
putexcel K1 = "Ydses_c5_Q3_L1"
putexcel L1 = "Ydses_c5_Q4_L1"
putexcel M1 = "Ydses_c5_Q5_L1"
putexcel N1 = "UKC"
putexcel O1 = "UKD"
putexcel P1 = "UKE"
putexcel Q1 = "UKF"
putexcel R1 = "UKG"
putexcel S1 = "UKH"
putexcel T1 = "UKJ"
putexcel U1 = "UKK"
putexcel V1 = "UKL"
putexcel W1 = "UKM"
putexcel X1 = "UKN"
putexcel Y1 = "Year_transformed"
putexcel Z1 = "Y2020"
putexcel AA1 = "Y2021"
putexcel AB1 = "Ethn_Asian"
putexcel AC1 = "Ethn_Black"
putexcel AD1 = "Ethn_Other"
putexcel AE1 = "Constant"

	
* Goodness of fit 

putexcel set "$dir_results/reg_leave_parental_home", sheet("Gof") modify

putexcel A3 = "P1a - Leaving parental home", bold		

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
