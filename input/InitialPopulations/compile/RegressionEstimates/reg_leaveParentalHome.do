********************************************************************************
* PROJECT:  		ESPON
* SECTION:			Leaving Parental Home
* OBJECT: 			Final Probit Regression Model 
* AUTHORS:			Daria Popova, Justin van de Ven
* LAST UPDATE:		26 Aug 2025 DP  
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

putexcel set "$dir_work/reg_leaveParentalHome", sheet("Info") replace
putexcel A1 = "Description:"
putexcel B1 = "Model parameters governing leaving parental home"
putexcel A2 = "Authors:	Patryk Bronka, Justin van de Ven, Daria Popova" 
putexcel A3 = "Last edit: 4 Nov 2025 DP"

putexcel A4 = "Process:", bold
putexcel B4 = "Description:", bold
putexcel A5 = "P1a"
putexcel B5 = "Probit regression estimates for leaving the parental home - 18+, not in intitial education spell, living with parents in t-1"

putexcel A10 = "Notes:", bold
putexcel B10 = "Added: ethnicity-4 cat (dot); covid dummies (y2020 y2021)"
putexcel B11 = "DV is synchronised with the adult child definition"

putexcel set "$dir_work/reg_leaveParentalHome", sheet("Gof") modify
putexcel A1 = "Goodness of fit", bold		


********************************************************************************
* Process P1a: Leave Parental Home 
********************************************************************************
* Process P1a: Probability of leaving the parental home. 
* Sample: All respondents adult child in t-1 and not currently in initial 
* 			education spell 
* DV: Observed transitioning from adult child to non-adult child

xtset idperson swv		
//fre dlftphm if (ded == 0 & dag >= 18 & dcpst != 1) //3.65%
fre dlftphm if (ded == 0 & dag >= 18 ) 
tab2 stm dlftphm if (ded == 0 & dag >= 18), r 

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

probit dlftphm Dgn Dag Dag_sq Deh_c3_Medium Deh_c3_Low ///
	Les_c3_Student_L1 Les_c3_NotEmployed_L1 ///
	Ydses_c5_Q2_L1 Ydses_c5_Q3_L1 Ydses_c5_Q4_L1 Ydses_c5_Q5_L1 ///
	UKC UKD UKE UKF UKG UKH UKJ UKK UKL UKM UKN /// 
	Year_transformed Y2020 Y2021 Ethn_Asian Ethn_Black Ethn_Other ///
	if (ded == 0 & dag >= 18 /*& dagpns!=1 & les_c4!=4*/ ) [pw = dimxwt], vce(robust)

	
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
putexcel set "$dir_results/reg_leaveParentalHome", sheet("UK_P1a") modify
putexcel C2 = matrix(var)
		
restore	


* Store results in Excel 

* Store estimates
matrix b = e(b)	
matrix V = e(V)

mata:
	// Call matrices into mata 
    V = st_matrix("V")
    b = st_matrix("b")

    // Find which coefficients are nonzero
    keep = (b :!= 0)
	
	// Eliminate zeros
	b_trimmed = select(b, keep)
    V_trimmed = select(V, keep)
    V_trimmed = select(V_trimmed', keep)'

	// Inspection
	b_trimmed 
	V_trimmed 
	
    // Return to Stata
    st_matrix("b_trimmed", b_trimmed')
    st_matrix("V_trimmed", V_trimmed)
	st_matrix("nonzero_b_flag", keep)
end	

* Export into Excel 
putexcel set "$dir_results/reg_leaveParentalHome", sheet("UK_P1a") modify
putexcel B2 = matrix(b_trimmed)
putexcel C2 = matrix(V_trimmed)

* Labelling 
// Need to variable label when add new variable to model. Order matters. 
local var_list Dgn Dag Dag_sq ///
    Deh_c3_Medium Deh_c3_Low ///
	Les_c3_Student_L1 Les_c3_NotEmployed_L1 ///
	Ydses_c5_Q2_L1 Ydses_c5_Q3_L1 Ydses_c5_Q4_L1 Ydses_c5_Q5_L1 ///
	UKC UKD UKE UKF UKG UKH UKJ UKK UKL UKM UKN /// 
	Year_transformed Y2020 Y2021 Ethn_Asian Ethn_Black Ethn_Other ///
	Constant
	
	
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

putexcel set "$dir_results/reg_leaveParentalHome", sheet("Gof") modify

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
