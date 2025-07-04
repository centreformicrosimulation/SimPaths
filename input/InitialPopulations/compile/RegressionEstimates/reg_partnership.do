********************************************************************************
* PROJECT:  		ESPON 
* SECTION:			Unions
* OBJECT: 			Final Probit Models
* AUTHORS:			Daria Popova, Justin van de Ven
* LAST UPDATE:		1 July 2025 DP  
* COUNTRY: 			UK  
* 
*NOTES: 			
*                    
* 					Reduced number of covariates in union formation process 
*                   for those in initial education spell to obtain estimaes. 	
********************************************************************************
clear all
set more off
set mem 200m
set type double
//set maxvar 120000
set maxvar 30000


*******************************************************************
cap log close 
log using "${dir_log}/reg_partnership.log", replace
*******************************************************************

use "$dir_ukhls_data/ukhls_pooled_all_obs_09.dta", clear

do "$dir_do/variable_update"



*sample selection 
drop if dag < 16


xtset idperson swv

* Set Excel file 

* Info sheet

putexcel set "$dir_results/reg_partnership", sheet("Info") replace
putexcel A1 = "Description:"
putexcel B1 = "Model parameters for relationship status projection"
putexcel A2 = "Authors:	Patryk Bronka, Justin van de Ven, Daria Popova" 
putexcel A3 = "Last edit: 1 July 2025 DP"

putexcel A4 = "Process:", bold
putexcel B4 = "Description:", bold
putexcel A5 = "U1a"
putexcel B5 = "Probit regression estimates  probability of entering  a partnership - single respondents aged 18+ in initial education spell"
putexcel A6 = "U1b"
putexcel B6 = "Probit regression estimates of probability of entering a partnership - single respondents aged 18+ not in initial education spell"
putexcel A7 = "U2b"
putexcel B7 = "Probit regression estimates of probability of exiting a partnership - cohabiting women aged 18+ not in initial education spell"

putexcel A10 = "Notes:", bold
putexcel B10 = "All processes: replaced dhe with dhe_pcs and dhe_mcs, added ethnicity-4 cat (dot) and Covid dummies (y2020 y2021)"
putexcel B11 = "U1a: Just 73 obs with positive outcome! Cannot include region and covid dummies as covariates. Cannot obtain estimates of the 5th quintile of hh income"
putexcel B12 = "U2b contains a new variable New_rel_L1"

putexcel set "$dir_results/reg_partnership", sheet("Gof") modify
putexcel A1 = "Goodness of fit", bold		

****************************************************
* U1a: Partnership formation, in initial edu spell *
****************************************************
* Probability of entering a partnership. 
* Sample: All single respondents aged 18 +, in continuous education.
* DV: Enter partnership dummy 
* Note: Requirement of being single in the previous year is embedded in the 
* 			dependent variable  
* 		Only 73 observation of relationships forming when still in initial 
* 			education spell and aged 18+.
 
fre dcpen if (dag >= 18 & ded == 1 & ssscp != 1) 

/*/////////////////////////////////////////////////////////////////////////////////////////////////	 
//check weights //////////////////////////////////////////////////////////////////////////////////	 
probit dcpen i.dgn dag dagsq li.ydses_c5 l.dnc l.dnc02 /*dhe*/ dhe_pcs dhe_mcs /*ib8.drgn1*/ stm /*y2020 y2021*/ i.dot ///
if (dag>=18 & ded==1 & ssscp!=1) [pweight=dimlwt], vce(robust)
outreg2 using "${weight_checks}/weight_comparison_U1a.xls", alpha(0.001, 0.01, 0.05, 0.1) symbol(***, **, *, +) replace ctitle(U1a, dimlwt) side dec(4) 

probit dcpen i.dgn dag dagsq li.ydses_c5 l.dnc l.dnc02 /*dhe*/ dhe_pcs dhe_mcs /*ib8.drgn1*/ stm /*y2020 y2021*/ i.dot ///
if (dag>=18 & ded==1 & ssscp!=1) [pweight=disclwt], vce(robust)
outreg2 using "${weight_checks}/weight_comparison_U1a.xls", alpha(0.001, 0.01, 0.05, 0.1) symbol(***, **, *, +) append ctitle(U1a, disclwt) side dec(4)

probit dcpen i.dgn dag dagsq li.ydses_c5 l.dnc l.dnc02 /*dhe*/ dhe_pcs dhe_mcs /*ib8.drgn1*/ stm /*y2020 y2021*/ i.dot ///
if (dag>=18 & ded==1 & ssscp!=1) [pweight=dimxwt], vce(robust)
outreg2 using "${weight_checks}/weight_comparison_U1a.xls", alpha(0.001, 0.01, 0.05, 0.1) symbol(***, **, *, +) append ctitle(U1a, dimxwt) side dec(4) 
erase "${weight_checks}/weight_comparison_U1a.txt"
//////////////////////////////////////////////////////////////////////////////////////////////////// 
////////////////////////////////////////////////////////////////////////////////////////////////////
*/

probit dcpen i.dgn dag dagsq li.ydses_c5 l.dnc l.dnc02 /*dhe*/ dhe_pcs dhe_mcs /*ib8.drgn1*/ stm /*y2020 y2021*/ i.dot ///
if (dag>=18 & ded==1 & ssscp!=1) [pweight=dimxwt], vce(robust)
 
* raw results 
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_raw_results/partnership/partnership", sheet("U1a") replace
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))
outreg2 stats(coef se pval) using "$dir_raw_results/partnership/U1a.doc", replace ///
title("Process U1a: Probit regression estimates for entering a partnership - single respondents aged 18+ in continuous education") ///
 ctitle(enter partnership) label side dec(2) noparen addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll))
 
gen in_sample = e(sample)	

predict p

save "$dir_validation_data/U1a_sample", replace

scalar r2_p = e(r2_p) 
scalar N = e(N)	
scalar chi2 = e(chi2)
scalar ll = e(ll)	

		
* Results		
* Note: Zeros values are eliminated 

matrix b = e(b)	
matrix V = e(V)


* Store variance-covariance matrix 

preserve

putexcel set "$dir_raw_results/partnership/var_cov", sheet("var_cov") replace
putexcel A1 = matrix(V)

import excel "$dir_raw_results/partnership/var_cov", sheet("var_cov") clear

describe
local no_vars = `r(k)'	
	
forvalues i = 1/2 {
	egen row_sum = rowtotal(*)
	drop if row_sum == 0 
	drop row_sum
	xpose, clear	
}	
	
mkmat v*, matrix(var)	
putexcel set "$dir_results/reg_partnership", sheet("U1a") modify
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

putexcel set "$dir_results/reg_partnership", sheet("U1a") modify
putexcel A1 = matrix(nonzero_b'), names nformat(number_d2) 
	

* Labelling

putexcel A1 = "REGRESSOR"
putexcel A2 = "Dgn"
putexcel A3 = "Dag"
putexcel A4 = "Dag_sq"
putexcel A5 = "Ydses_c5_Q2_L1"
putexcel A6 = "Ydses_c5_Q3_L1"
putexcel A7 = "Ydses_c5_Q4_L1"
putexcel A8 = "Dnc_L1"
putexcel A9 = "Dnc02_L1"
putexcel A10 = "Dhe_pcs"
putexcel A11 = "Dhe_mcs"
putexcel A12 = "Year_transformed"
putexcel A13 = "Ethn_Asian"
putexcel A14 = "Ethn_Black"
putexcel A15 = "Ethn_Other"
putexcel A16 = "Constant"

putexcel B1 = "COEFFICIENT"
putexcel C1 = "Dgn"
putexcel D1 = "Dag"
putexcel E1 = "Dag_sq"
putexcel F1 = "Ydses_c5_Q2_L1"
putexcel G1 = "Ydses_c5_Q3_L1"
putexcel H1 = "Ydses_c5_Q4_L1"
putexcel I1 = "Dnc_L1"
putexcel J1 = "Dnc02_L1"
putexcel K1 = "Dhe_pcs"
putexcel L1 = "Dhe_mcs"
putexcel M1 = "Year_transformed"
putexcel N1 = "Ethn_Asian"
putexcel O1 = "Ethn_Black"
putexcel P1 = "Ethn_Other"
putexcel Q1 = "Constant"
 
* Goodness of fit

putexcel set "$dir_results/reg_partnership", sheet("Gof") modify

putexcel A3 = "U1a - Partnership formation, in initial education spell", ///
	bold		

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


********************************************************
* U1b: Partnership formation, not in initial edu spell *
********************************************************
* Process U1b: Probability of entering a partnership. 
* Sample: All respondents aged 18+, left initial education spell and not in a 
* 			same sex relationship 
* DV: Enter partnership dummy (requires not having been in a relationship last 
* 		year)	
* Note: Requirement of being single in the previous year is embedded in the 
* 			dependent variable  
* 		Income captured by hh quintiles. 

fre dcpen if (dag >= 18 & ded == 0 & ssscp != 1)

/*/////////////////////////////////////////////////////////////////////////////////////////////////	 
//check weights //////////////////////////////////////////////////////////////////////////////////	 
probit dcpen i.dgn dag dagsq li.ydses_c5 l.dnc l.dnc02 /*dhe*/ dhe_pcs dhe_mcs /*ib8.drgn1*/ stm /*y2020 y2021*/ i.dot ///
if (dag >= 18 & ded == 0 & ssscp != 1) [pweight=dimlwt], vce(robust)
outreg2 using "${weight_checks}/weight_comparison_U1b.xls", alpha(0.001, 0.01, 0.05, 0.1) symbol(***, **, *, +) replace ctitle(U1b, dimlwt) side dec(4) 

probit dcpen i.dgn dag dagsq li.ydses_c5 l.dnc l.dnc02 /*dhe*/ dhe_pcs dhe_mcs /*ib8.drgn1*/ stm /*y2020 y2021*/ i.dot ///
if (dag >= 18 & ded == 0 & ssscp != 1) [pweight=disclwt], vce(robust)
outreg2 using "${weight_checks}/weight_comparison_U1b.xls", alpha(0.001, 0.01, 0.05, 0.1) symbol(***, **, *, +) append ctitle(U1b, disclwt) side dec(4)

probit dcpen i.dgn dag dagsq li.ydses_c5 l.dnc l.dnc02 /*dhe*/ dhe_pcs dhe_mcs /*ib8.drgn1*/ stm /*y2020 y2021*/ i.dot ///
if (dag >= 18 & ded == 0 & ssscp != 1) [pweight=dimxwt], vce(robust)
outreg2 using "${weight_checks}/weight_comparison_U1b.xls", alpha(0.001, 0.01, 0.05, 0.1) symbol(***, **, *, +) append ctitle(U1b, dimxwt) side dec(4) 
erase "${weight_checks}/weight_comparison_U1b.txt"
//////////////////////////////////////////////////////////////////////////////////////////////////// 
////////////////////////////////////////////////////////////////////////////////////////////////////
*/

probit dcpen i.dgn dag dagsq li.ydses_c5 l.dnc l.dnc02 /*dhe*/ dhe_pcs dhe_mcs ib8.drgn1 stm y2020 y2021 i.dot ///
if (dag >= 18 & ded == 0 & ssscp != 1) [pweight=dimxwt], vce(robust)

* raw results 
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_raw_results/partnership/partnership", sheet("Process U1b") replace
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))
outreg2 stats(coef se pval) using "$dir_raw_results/partnership/U1b.doc", replace ///
title("Process U1b: Probit regression estimates for entering a partnership - single respondents aged 18+ not in continuous education") ///
 ctitle(enter partnership) label side dec(2) noparen addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll))
 
gen in_sample = e(sample)	

predict p

save "$dir_validation_data/U1b_sample", replace

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

putexcel set "$dir_raw_results/partnership/var_cov", sheet("var_cov") replace
putexcel A1 = matrix(V)

import excel "$dir_raw_results/partnership/var_cov", sheet("var_cov") clear

describe
local no_vars = `r(k)'	
	
forvalues i = 1/2 {
	egen row_sum = rowtotal(*)
	drop if row_sum == 0 
	drop row_sum
	xpose, clear	
}	
	
mkmat v*, matrix(var)	
putexcel set "$dir_results/reg_partnership", sheet("U1b") modify
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

putexcel set "$dir_results/reg_partnership", sheet("U1b") modify
putexcel A1 = matrix(nonzero_b'), names nformat(number_d2) 
		
* Labelling

putexcel A1 = "REGRESSOR"
putexcel A2 = "Dgn"
putexcel A3 = "Dag"
putexcel A4 = "Dag_sq"
putexcel A5 = "Ydses_c5_Q2_L1"
putexcel A6 = "Ydses_c5_Q3_L1"
putexcel A7 = "Ydses_c5_Q4_L1"
putexcel A8 = "Ydses_c5_Q5_L1"
putexcel A9 = "Dnc_L1"
putexcel A10 = "Dnc02_L1"
putexcel A11 = "Dhe_pcs"
putexcel A12 = "Dhe_mcs"
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

putexcel B1 = "Dgn"
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
putexcel M1 = "UKC"
putexcel N1 = "UKD"
putexcel O1 = "UKE"
putexcel P1 = "UKF"
putexcel Q1 = "UKG"
putexcel R1 = "UKH"
putexcel S1 = "UKJ"
putexcel T1 = "UKK"
putexcel U1 = "UKL"
putexcel V1 = "UKM"
putexcel W1 = "UKN"
putexcel X1 = "Year_transformed"
putexcel Y1 = "Y2020"
putexcel Z1 = "Y2021"
putexcel AA1 = "Ethn_Asian"
putexcel AB1 = "Ethn_Black"
putexcel AC1 = "Ethn_Other"
putexcel AD1 = "Constant"


* Goodness of fit 

putexcel set "$dir_results/reg_partnership", sheet("Gof") modify

putexcel A9 = "U1b - Partnership formation, left initial education spell", ///
	bold		

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


**********************************************************
* U2b: Partnership termination, not in initial edu spell *
**********************************************************

* Process U2b: Probability of partnership break-up.
* Sample: 	Female member of a heterosexual couple in t-1 aged 18+ and not in 
* 			continuous education
* DV: Exit partnership dummy
* Note:	Requirement to be in a relationship last year is embedded in the DV.
* 		The ded condition refers to the female partner only. 
* 		If take away the ded condition doesn't make any difference because there
* 		are not splits by those in their initial education spell. 
		
fre dcpex if (dgn == 0 & dag >= 18 & ded == 0 & ssscp != 1) 

/*/////////////////////////////////////////////////////////////////////////////////////////////////	 
//check weights //////////////////////////////////////////////////////////////////////////////////	 
probit dcpex dag dagsq lib1.deh_c3 lib1.dehsp_c3 /*li.dhe li.dhesp*/ l.dhe_pcs l.dhe_mcs l.dhe_pcssp l.dhe_mcssp l.dcpyy l.new_rel l.dcpagdf l.dnc l.dnc02  lib1.lesdf_c4 ///
     l.ypnbihs_dv l.ynbcpdf_dv ib8.drgn1 stm y2020 y2021 i.dot ///
	 if (dgn==0 & dag>=18 & ded==0 & ssscp!=1) [pweight=dimlwt], vce(robust)
outreg2 using "${weight_checks}/weight_comparison_U2b.xls", alpha(0.001, 0.01, 0.05, 0.1) symbol(***, **, *, +) replace ctitle(U2b, dimlwt) side dec(4) 

probit dcpex dag dagsq lib1.deh_c3 lib1.dehsp_c3 /*li.dhe li.dhesp*/ l.dhe_pcs l.dhe_mcs l.dhe_pcssp l.dhe_mcssp l.dcpyy l.new_rel l.dcpagdf l.dnc l.dnc02  lib1.lesdf_c4 ///
     l.ypnbihs_dv l.ynbcpdf_dv ib8.drgn1 stm y2020 y2021 i.dot ///
	 if (dgn==0 & dag>=18 & ded==0 & ssscp!=1) [pweight=disclwt], vce(robust)
outreg2 using "${weight_checks}/weight_comparison_U2b.xls", alpha(0.001, 0.01, 0.05, 0.1) symbol(***, **, *, +) append ctitle(U2b, disclwt) side dec(4)

probit dcpex dag dagsq lib1.deh_c3 lib1.dehsp_c3 /*li.dhe li.dhesp*/ l.dhe_pcs l.dhe_mcs l.dhe_pcssp l.dhe_mcssp l.dcpyy l.new_rel l.dcpagdf l.dnc l.dnc02  lib1.lesdf_c4 ///
     l.ypnbihs_dv l.ynbcpdf_dv ib8.drgn1 stm y2020 y2021 i.dot ///
	 if (dgn==0 & dag>=18 & ded==0 & ssscp!=1) [pweight=dhhwt], vce(robust)
outreg2 using "${weight_checks}/weight_comparison_U2b.xls", alpha(0.001, 0.01, 0.05, 0.1) symbol(***, **, *, +) append ctitle(U2b, dhhwt) side dec(4) 
probit dcpex dag dagsq lib1.deh_c3 lib1.dehsp_c3 /*li.dhe li.dhesp*/ l.dhe_pcs l.dhe_mcs l.dhe_pcssp l.dhe_mcssp l.dcpyy l.new_rel l.dcpagdf l.dnc l.dnc02  lib1.lesdf_c4 ///
     l.ypnbihs_dv l.ynbcpdf_dv ib8.drgn1 stm y2020 y2021 i.dot ///
	 if (dgn==0 & dag>=18 & ded==0 & ssscp!=1) [pweight=dimxwt], vce(robust)
outreg2 using "${weight_checks}/weight_comparison_U2b.xls", alpha(0.001, 0.01, 0.05, 0.1) symbol(***, **, *, +) append ctitle(U2b, dimxwt) side dec(4) 
erase "${weight_checks}/weight_comparison_U2b.txt"
//////////////////////////////////////////////////////////////////////////////////////////////////// 
////////////////////////////////////////////////////////////////////////////////////////////////////
*/
probit dcpex dag dagsq lib1.deh_c3 lib1.dehsp_c3 /*li.dhe li.dhesp*/ l.dhe_pcs l.dhe_mcs l.dhe_pcssp l.dhe_mcssp l.dcpyy l.new_rel l.dcpagdf l.dnc l.dnc02  lib1.lesdf_c4 ///
     l.ypnbihs_dv l.ynbcpdf_dv ib8.drgn1 stm y2020 y2021 i.dot ///
	 if (dgn==0 & dag>=18 & ded==0 & ssscp!=1) [pweight=dimxwt], vce(robust)

	* raw results 
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_raw_results/partnership/partnership", sheet("Process U2b") modify
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))
outreg2 stats(coef se pval) using "$dir_raw_results/partnership/U2b.doc", replace ///
title("Process U2b: Probit regression estimates for exiting a partnership - cohabiting women aged 18+ not in continuous education") ///
 ctitle(enter partnership) label side dec(2) noparen addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll))
	
	
gen in_sample = e(sample)	

predict p

save "$dir_validation_data/U2b_sample", replace

scalar r2_p = e(r2_p) 
scalar N = e(N)	 
scalar chi2 = e(chi2)
scalar ll = e(ll)


* Results 	
* Note: Zeros values are eliminated 
	
matrix b = e(b)	
matrix V = e(V)

matrix list  V

*  Store variance-covariance matrix 

preserve

putexcel set "$dir_raw_results/partnership/var_cov", sheet("var_cov") replace
putexcel A1 = matrix(V)

import excel "$dir_raw_results/partnership/var_cov", sheet("var_cov") clear

describe
local no_vars = `r(k)'	
	
forvalues i = 1/2 {
	egen row_sum = rowtotal(*)
	drop if row_sum == 0 
	drop row_sum
	xpose, clear	
}	
	
mkmat v*, matrix(var)	
putexcel set "$dir_results/reg_partnership", sheet("U2b") modify
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

putexcel set "$dir_results/reg_partnership", sheet("U2b") modify
putexcel A1 = matrix(nonzero_b'), names nformat(number_d2) 
	
	
* Labelling 

putexcel A1 = "REGRESSOR"
putexcel A2 = "Dag"
putexcel A3 = "Dag_sq"
putexcel A4 = "Deh_c3_Medium_L1"
putexcel A5 = "Deh_c3_Low_L1"
putexcel A6 = "Dehsp_c3_Medium_L1"
putexcel A7 = "Dehsp_c3_Low_L1"
putexcel A8 = "Dhe_pcs"
putexcel A9 = "Dhe_mcs"
putexcel A10 = "Dhe_pcssp"
putexcel A11 = "Dhe_mcssp"
putexcel A12 = "Dcpyy_L1"
putexcel A13 = "New_rel_L1"
putexcel A14 = "Dcpagdf_L1"
putexcel A15 = "Dnc_L1"
putexcel A16 = "Dnc02_L1"
putexcel A17 = "Lesdf_c4_EmployedSpouseNotEmployed_L1"
putexcel A18 = "Lesdf_c4_NotEmployedSpouseEmployed_L1"
putexcel A19 = "Lesdf_c4_BothNotEmployed_L1"
putexcel A20 = "Ypnbihs_dv_L1"
putexcel A21 = "Ynbcpdf_dv_L1"
putexcel A22 = "UKC"
putexcel A23 = "UKD"
putexcel A24 = "UKE"
putexcel A25 = "UKF"
putexcel A26 = "UKG"
putexcel A27 = "UKH"
putexcel A28 = "UKJ"
putexcel A29 = "UKK"
putexcel A30 = "UKL"
putexcel A31 = "UKM"
putexcel A32 = "UKN"
putexcel A33 = "Year_transformed"
putexcel A34 = "Y2020"
putexcel A35 = "Y2021"
putexcel A36 = "Ethn_Asian"
putexcel A37 = "Ethn_Black"
putexcel A38 = "Ethn_Other"
putexcel A39 = "Constant"


putexcel B1 = "COEFFICIENT"
putexcel C1 = "Dag"
putexcel D1 = "Dag_sq"
putexcel E1 = "Deh_c3_Medium_L1"
putexcel F1 = "Deh_c3_Low_L1"
putexcel G1 = "Dehsp_c3_Medium_L1"
putexcel H1 = "Dehsp_c3_Low_L1"
putexcel I1 = "Dhe_pcs"
putexcel J1 = "Dhe_mcs"
putexcel K1 = "Dhe_pcssp"
putexcel L1 = "Dhe_mcssp"
putexcel M1 = "Dcpyy_L1"
putexcel N1 = "New_rel_L1"
putexcel O1 = "Dcpagdf_L1"
putexcel P1 = "Dnc_L1"
putexcel Q1 = "Dnc02_L1"
putexcel R1 = "Lesdf_c4_EmployedSpouseNotEmployed_L1"
putexcel S1 = "Lesdf_c4_NotEmployedSpouseEmployed_L1"
putexcel T1 = "Lesdf_c4_BothNotEmployed_L1"
putexcel U1 = "Ypnbihs_dv_L1"
putexcel V1 = "Ynbcpdf_dv_L1"
putexcel W1 = "UKC"
putexcel X1 = "UKD"
putexcel Y1 = "UKE"
putexcel Z1 = "UKF"
putexcel AA1 = "UKG"
putexcel AB1 = "UKH"
putexcel AC1 = "UKJ"
putexcel AD1 = "UKK"
putexcel AE1 = "UKL"
putexcel AF1 = "UKM"
putexcel AG1 = "UKN"
putexcel AH1 = "Year_transformed"
putexcel AI1 = "Y2020"
putexcel AJ1 = "Y2021"
putexcel AK1 = "Ethn_Asian"
putexcel AL1 = "Ethn_Black"
putexcel AM1 = "Ethn_Other"
putexcel AN1 = "Constant"

* Goodness of fit

putexcel set "$dir_results/reg_partnership", sheet("Gof") modify

putexcel A15 = ///
	"U2b - Partnership termination, left initial education spell", bold		

putexcel A17 = "Pseudo R-squared" 
putexcel B17 = r2_p 
putexcel A18 = "N"
putexcel B18 = N 
putexcel E17 = "Chi^2"		
putexcel F17 = chi2
putexcel E18 = "Log likelihood"		
putexcel F18 = ll		

drop in_sample p
scalar drop r2_p N chi2 ll	
	
	
capture log close 
