********************************************************************************
* PROJECT:  		ESPON 
* SECTION:			Education
* OBJECT: 			Final Probit & Generalised Logit Models - Weighted
* AUTHORS:			Patryk Bronka, Daria Popova, Justin van de Ven
* LAST UPDATE:		1 July 2025 DP  
* COUNTRY: 			UK  
* 
* NOTES: 	                   
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
log using "${dir_log}/reg_education.log", replace
*******************************************************************

use "$dir_ukhls_data/ukhls_pooled_all_obs_09.dta", clear

do "$dir_do/variable_update"



* Sample selection 
drop if dag < 16


xtset idperson swv

* Set Excel file 

* Info sheet
putexcel set "$dir_results/reg_education", sheet("Info") replace
putexcel A1 = "Description:"
putexcel B1 = "Model parameters governing projection of education status"
putexcel A2 = "Authors:	Patryk Bronka, Justin van de Ven, Daria Popova" 
putexcel A3 = "Last edit: 1 July 2025 DP"

putexcel A4 = "Process:", bold
putexcel B4 = "Description:", bold

putexcel A5 = "E1a"
putexcel B5 = "Probit regression estimates of remaining in continuous education - individuals aged 16-29 in initial education spell"

putexcel A6 = "E1b"
putexcel B6 = "Probit regression estimates of returning to education - individuals aged 16-35 not in initial education spell"

putexcel A7 = "E2a"
putexcel B7 = "Generalized ordered logit regression estimates of education attainment - individuals aged 16-29 exiting education that were in initial education spell in t-1 but not in t"
putexcel B8 = "Covariates that satisfy the parallel lines assumption have one estimate for all categories of the dependent variable and are present once in the table"
putexcel B9 = "Covariates that do not satisfy the parallel lines assumption have an estimate for each estimated category of the dependent variable. These covariates have the dependent variable category appended to their name."

putexcel A10 = "Notes:", bold
putexcel B10 = "Added:  ethnicity-4 cat (dot); covid dummies (y2020 y2021)"


putexcel set "$dir_results/reg_education", sheet("Gof") modify
putexcel A1 = "Goodness of fit", bold	


************************************************************
* E1a: Probability of Remaining in Initial Education Spell *
************************************************************
* Process E1a: Remaining in the initial education spell. 
* Sample: Individuals aged 16-29 who have not left their initial education spell
* DV: In continuous education dummy 
* Note: Condition implies some persistence - education for the last 2 years. 

fre ded if (dag >= 16 & dag <= 29 & l.ded == 1) 
// was in initial education spell in the previous wave 
// 70.1% remain in education 

/*//////////////////////////////////////////////////////////////////////////////////////////////////	 
//check weights //////////////////////////////////////////////////////////////////////////////////	 
probit ded i.dgn dag dagsq ib1.dehmf_c3 ib8.drgn1 stm y2020 y2021 i.dot ///
   if (dag>=16 & dag<=29 & l.ded==1) [pweight=dimlwt], vce(robust)
outreg2 using "${weight_checks}/weight_comparison_E1a.xls", alpha(0.001, 0.01, 0.05, 0.1) symbol(***, **, *, +) replace ctitle(E1b, dimlwt) side dec(4) 

probit ded i.dgn dag dagsq ib1.dehmf_c3 ib8.drgn1 stm y2020 y2021 i.dot ///
   if (dag>=16 & dag<=29 & l.ded==1) [pweight=disclwt], vce(robust)
outreg2 using "${weight_checks}/weight_comparison_E1a.xls", alpha(0.001, 0.01, 0.05, 0.1) symbol(***, **, *, +) append ctitle(E1b, disclwt) side dec(4) 

probit ded i.dgn dag dagsq ib1.dehmf_c3 ib8.drgn1 stm y2020 y2021 i.dot ///
   if (dag>=16 & dag<=29 & l.ded==1)  [pweight=dimxwt], vce(robust)
outreg2 using "${weight_checks}/weight_comparison_E1a.xls", alpha(0.001, 0.01, 0.05, 0.1) symbol(***, **, *, +) append ctitle(E1b, dimxwt) side dec(4) 
erase "${weight_checks}/weight_comparison_E1a.txt"
//////////////////////////////////////////////////////////////////////////////////////////////////// 
////////////////////////////////////////////////////////////////////////////////////////////////////
*/
probit ded i.dgn dag dagsq ib1.dehmf_c3 ib8.drgn1 stm y2020 y2021 i.dot ///
   if (dag>=16 & dag<=29 & l.ded==1) [pweight=dimxwt], vce(robust)

   * save raw results 
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_raw_results/education/education", sheet("Process E1a") replace
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))
outreg2 stats(coef se pval) using "$dir_raw_results/education/E1a.doc", replace ///
title("Process E1a: Probability of remaining in initial education spell - individuals aged 16-29 in initial education spell.") ///
 ctitle(Continuing student) label side dec(2) noparen addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll))

gen in_sample = e(sample)	

predict p

save "$dir_validation_data/E1a_sample", replace


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

putexcel set "$dir_raw_results/education/var_cov", sheet("var_cov") replace
putexcel A1 = matrix(V)

import excel "$dir_raw_results/education/var_cov", sheet("var_cov") clear

describe
local no_vars = `r(k)'	
	
forvalues i = 1/2 {
	egen row_sum = rowtotal(*)
	drop if row_sum == 0 
	drop row_sum
	xpose, clear	
}	
	
mkmat v*, matrix(var)	
putexcel set "$dir_results/reg_education", sheet("E1a") modify
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

putexcel set "$dir_results/reg_education", sheet("E1a") modify
putexcel A1 = matrix(nonzero_b'), names nformat(number_d2) 


* Labelling 

putexcel A1 = "REGREESOR"
putexcel A2 = "Dgn"
putexcel A3 = "Dag"
putexcel A4 = "Dag_sq"
putexcel A5 = "Dehmf_c3_Medium"
putexcel A6 = "Dehmf_c3_Low"
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
putexcel A18 = "Year_transformed"
putexcel A19 = "Y2020"
putexcel A20 = "Y2021"
putexcel A21 = "Ethn_Asian"
putexcel A22 = "Ethn_Black"
putexcel A23 = "Ethn_Other"
putexcel A24 = "Constant"

putexcel B1 = "COEFFICIENT"
putexcel C1 = "Dgn"
putexcel D1 = "Dag"
putexcel E1 = "Dag_sq"
putexcel F1 = "Dehmf_c3_Medium"
putexcel G1 = "Dehmf_c3_Low"
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
putexcel S1 = "Year_transformed" 
putexcel T1 = "Y2020" 
putexcel U1 = "Y2021"
putexcel V1 = "Ethn_Asian" 
putexcel W1 = "Ethn_Black"
putexcel X1 = "Ethn_Other"
putexcel Y1 = "Constant"

	
* Goodness of fit

putexcel set "$dir_results/reg_education", sheet("Gof") modify

putexcel A3 = "E1a - Remaining in initial education spell", bold		

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


**********************************************
* E1b: Probability of Returning to Education *
**********************************************

* Process E1b: Retraining having previously entered the labour force. 
* Sample: Individuals aged 16-35 who have left their initial education spell 
*  			and not a student last year 
* DV: Return to education 

fre der if (dag >= 16 & dag <= 35 & ded == 0) 
// 69.3% remain out of education 

/*//////////////////////////////////////////////////////////////////////////////////////////////////	 
//check weights //////////////////////////////////////////////////////////////////////////////////	 
probit der i.dgn dag dagsq lib1.deh_c3 li.les_c3 l.dnc l.dnc02 ib1.dehmf_c3  ib8.drgn1 stm y2020 y2021 i.dot ///
if (dag >= 16 & dag <= 35 & ded==0  & l.der==0)  [pweight=dimlwt], vce(robust)
outreg2 using "${weight_checks}/weight_comparison_E1b.xls", alpha(0.001, 0.01, 0.05, 0.1) symbol(***, **, *, +) replace ctitle(E1b, dimlwt) side dec(4)

probit der i.dgn dag dagsq lib1.deh_c3 li.les_c3 l.dnc l.dnc02 ib1.dehmf_c3  ib8.drgn1 stm y2020 y2021 i.dot ///
if (dag >= 16 & dag <= 35 & ded==0  & l.der==0)	 [pweight=disclwt], vce(robust)
outreg2 using "${weight_checks}/weight_comparison_E1b.xls", alpha(0.001, 0.01, 0.05, 0.1) symbol(***, **, *, +) append ctitle(E1b, disclwt) side dec(4) 

probit der i.dgn dag dagsq lib1.deh_c3 li.les_c3 l.dnc l.dnc02 ib1.dehmf_c3  ib8.drgn1 stm y2020 y2021 i.dot ///
if (dag >= 16 & dag <= 35 & ded==0  & l.der==0)  [pweight=dimxwt], vce(robust)
outreg2 using "${weight_checks}/weight_comparison_E1b.xls", alpha(0.001, 0.01, 0.05, 0.1) symbol(***, **, *, +) append ctitle(E1b, dimxwt) side dec(4) 
erase "${weight_checks}/weight_comparison_E1b.txt"
//////////////////////////////////////////////////////////////////////////////////////////////////// 
////////////////////////////////////////////////////////////////////////////////////////////////////
*/
probit der i.dgn dag dagsq lib1.deh_c3 li.les_c3 l.dnc l.dnc02 ib1.dehmf_c3  ib8.drgn1 stm y2020 y2021 i.dot ///
if (dag >= 16 & dag <= 35 & ded==0  & l.der==0) ///
	 [pweight=dimxwt], vce(robust)

	* save raw results 
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_raw_results/education/education", sheet("Process E1b") modify
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))
outreg2 stats(coef se pval) using "$dir_raw_results/education/E1b.doc", replace ///
title("Process E1b: Probability of returning to education - individuals aged 16-35 not in continuous education.") ///
 ctitle(Returning student) label side dec(2) noparen addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll))

gen in_sample = e(sample)	

predict p

save "$dir_validation_data/E1b_sample", replace

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

putexcel set "$dir_raw_results/education/var_cov", sheet("var_cov") ///
	replace
putexcel A1 = matrix(V)

import excel "$dir_raw_results/education/var_cov", sheet("var_cov") clear

describe
local no_vars = `r(k)'	
	
forvalues i = 1/2 {
	egen row_sum = rowtotal(*)
	drop if row_sum == 0 
	drop row_sum
	xpose, clear	
}	
	
mkmat v*, matrix(var)	
putexcel set "$dir_results/reg_education", sheet("E1b") modify
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

putexcel set "$dir_results/reg_education", sheet("E1b") modify
putexcel A1 = matrix(nonzero_b'), names nformat(number_d2) 
		
		
* Labelling 
putexcel A1 = "REGRESSOR"
putexcel A2 = "Dgn"
putexcel A3 = "Dag"
putexcel A4 = "Dag_sq"
putexcel A5 = "Deh_c3_Medium_L1"
putexcel A6 = "Deh_c3_Low_L1"
putexcel A7 = "Les_c3_NotEmployed_L1"
putexcel A8 = "Dnc_L1"
putexcel A9 = "Dnc02_L1"
putexcel A10 = "Dehmf_c3_Medium"
putexcel A11 = "Dehmf_c3_Low"
putexcel A12 = "UKC"
putexcel A13 = "UKD"
putexcel A14 = "UKE"
putexcel A15 = "UKF"
putexcel A16 = "UKG"
putexcel A17 = "UKH"
putexcel A18 = "UKJ"
putexcel A19 = "UKK"
putexcel A20 = "UKL"
putexcel A21 = "UKM"
putexcel A22 = "UKN"
putexcel A23 = "Year_transformed"
putexcel A24 = "Y2020"
putexcel A25 = "Y2021"
putexcel A26 = "Ethn_Asian"
putexcel A27 = "Ethn_Black"
putexcel A28 = "Ethn_Other"
putexcel A29 = "Constant"

putexcel B1 = "COEFFICIENT"
putexcel C1 = "Dgn"
putexcel D1 = "Dag"
putexcel E1 = "Dag_sq"
putexcel F1 = "Deh_c3_Medium_L1"
putexcel G1 = "Deh_c3_Low_L1"
putexcel H1 = "Les_c3_NotEmployed_L1"
putexcel I1 = "Dnc_L1"
putexcel J1 = "Dnc02_L1"
putexcel K1 = "Dehmf_c3_Medium"
putexcel L1 = "Dehmf_c3_Low"
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

putexcel set "$dir_results/reg_education", sheet("Gof") modify

putexcel A8 = "E1b - Returning to education", bold		

putexcel A10 = "Pseudo R-squared" 
putexcel B10 = r2_p 
putexcel A11 = "N"
putexcel B11 = N 
putexcel E10 = "Chi^2"		
putexcel F10 = chi2
putexcel E11 = "Log likelihood"		
putexcel F11 = ll
		
drop in_sample p
scalar drop r2_p N chi2 ll	


*************************************************
* E2a Educational Level After Leaving Education *
*************************************************

* Process E2a: Educational level achieved when leaving the initial spell of 
* 				education  
* Sample: Those 16-29 who have left their initial education spell in current 
* 			year 
* DV: Education level (3 cat)  
* Note: Previously tried a multinomial probit, now use a generalised ordered logit 

fre deh_c3 if (dag >= 16 & dag <= 29) & l.ded == 1 & ded == 0

recode deh_c3 (1 = 3) (3 = 1), gen(deh_c3_recoded)	
lab def deh_c3_recoded 1 "Low" 2 "Medium" 3 "High"
lab val deh_c3_recoded deh_c3_recoded


/* Model specification tests 

local model_specification_test=0 

if `model_specification_test' == 0 {

	* Option 1 - Ordered logit  

	* Testing the parallel lines assumption 
	* 	- the model asssumes that coefs (apart for the constant) when estimating  
	* 		a series of binary probits for 1 vs higher, 1&2 vs higher, 1&2&3 vs 
	* 		higher
	*	- Brant test null: the slope coefficients are the same across response  
	* 		all categories (p<0.05 -> violating the prop odds assumption)

	sort idperson swv


	ologit deh_c3_recoded i.dgn dag dagsq ib1.dehmf_c3 ib8.drgn1 stm y2020 y2021 i.dot if ///
		dag >= 16 & dag <= 29 & l.ded == 1 & ded == 0 ///
		[pweight = dimxwt], vce(robust)
	 
	oparallel, ic /*note: all tests have very high Chi2 statistics with p-values of 0.000.the parallel lines assumption is violated.*/
 
 
	* Option 2 - Linear model 

	xtset idperson swv

	reg deh_c3_recoded i.dgn dag dagsq ib1.dehmf_c3 ib8.drgn1 stm y2020 y2021 i.dot if ///
		dag >= 16 & dag <= 29 & l.ded == 1 & ded == 0 [pweight = dimxwt], vce(robust)


	// obtain distribution of predicted values plot 
	// make sure to add in sampling variance
	gen in_sample = e(sample)

	scalar sigma = e(rmse)
	gen epsilon = rnormal()*sigma
	sum epsilon 
	predict pred_edu if in_sample == 1
	replace pred_edu = pred_edu + epsilon if in_sample == 1

	twoway (hist deh_c3_recoded if in_sample == 1 , lcolor(gs12) ///
		fcolor(gs12)) (hist pred_edu if in_sample == 1 , ///
		fcolor(none) lcolor(red)), xtitle (Education level) ///
		legend(lab(1 "Observed") lab( 2 "Predicted")) name(levels, replace) ///
		graphregion(color(white))

	drop in_sample pred_edu epsilon

	sort idperson swv
 
 
	* Option 3 - Generalized ordered logit  
	
	gologit2 deh_c3_recoded i.dgn dag dagsq ib1.dehmf_c3 ib8.drgn1 stm y2020 y2021 i.dot if ///
		dag >= 16 & dag <= 29 & l.ded == 1 & ded == 0 [pweight = dimxwt], vce(robust) autofit 
	// does the	model produce any negative probabilities? 
	// if so, 
	//	1 - play around with the controls 
	//  2 - consider in the simulation converting the negative probabilities 
	//		to be zero and rescaling the cdf to sum to 1
	 
}
*/

* Generalized ordered logit 
sort idperson swv
/*
//////////////////////////////////////////////////////////////////////////////////////////////////	 
//check weights //////////////////////////////////////////////////////////////////////////////////	 
gologit2 deh_c3_recoded i.dgn dag dagsq ib1.dehmf_c3 ib8.drgn1 stm y2020 y2021 i.dot if ///
	dag >= 16 & dag <= 29 & l.ded == 1 & ded == 0 [pweight=dimlwt], vce(robust) autofit 
outreg2 using "${weight_checks}/weight_comparison_E2a.xls", alpha(0.001, 0.01, 0.05, 0.1) symbol(***, **, *, +) replace ctitle(E2a, dimlwt) side dec(4) 

gologit2 deh_c3_recoded i.dgn dag dagsq ib1.dehmf_c3 ib8.drgn1 stm y2020 y2021 i.dot if ///
	dag >= 16 & dag <= 29 & l.ded == 1 & ded == 0 [pweight = disclwt], vce(robust) autofit 
outreg2 using "${weight_checks}/weight_comparison_E2a.xls", alpha(0.001, 0.01, 0.05, 0.1) symbol(***, **, *, +) append ctitle(E2a, disclwt) side dec(4) 

gologit2 deh_c3_recoded i.dgn dag dagsq ib1.dehmf_c3 ib8.drgn1 stm y2020 y2021 i.dot if ///
	dag >= 16 & dag <= 29 & l.ded == 1 & ded == 0 [pweight = dimxwt], vce(robust) autofit 
outreg2 using "${weight_checks}/weight_comparison_E2a.xls", alpha(0.001, 0.01, 0.05, 0.1) symbol(***, **, *, +) append ctitle(E2a, dimxwt) side dec(4)  
erase "${weight_checks}/weight_comparison_E2a.txt"
//////////////////////////////////////////////////////////////////////////////////////////////////// 
////////////////////////////////////////////////////////////////////////////////////////////////////
*/
gologit2 deh_c3_recoded i.Dgn Dag Dag_sq ///
         i.Dehmf_c3_Medium i.Dehmf_c3_Low ///
         i.UKC i.UKD i.UKE i.UKF i.UKG i.UKH i.UKJ i.UKK i.UKL i.UKM i.UKN ///
         Year_transformed Y2020 Y2021 ///
         i.Ethn_Asian i.Ethn_Black i.Ethn_Other ///
if dag >= 16 & dag <= 29 & l.ded == 1 & ded == 0 [pweight = dimxwt], vce(robust) autofit 
	
*Note: In gologit2, the coefficients show how covariates affect the log-odds of being above a certain category vs. at or below it.

	
 * raw results 
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_raw_results/education/education", sheet("Process E2a") modify
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))
outreg2 stats(coef se pval) using "$dir_raw_results/education/E2a.doc", replace ///
title("Process E2a: Generalized ordered logit for educational attainment - individuals aged 16-29 who have left initial education spell.") ///
 ctitle(Education attainment) label side dec(2) noparen addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll))

* Save sample inclusion indicator and predicted probabilities		
gen in_sample = e(sample)
predict p1 p2 p3 
	
* Save sample for later use (internal validation)	
save "$dir_validation_data/E2a_sample", replace

* Store model summary statistics
scalar r2_p = e(r2_p) 
scalar N_sample = e(N)	 
	
* Store results in Excel 

* Store estimates in matrices
matrix b = e(b)	
matrix V = e(V)

* Raw output 
putexcel set "$dir_results/reg_education", sheet("E2a_raw") modify
putexcel A1 = matrix(b'), names //nformat(number_d2) 
putexcel A1 =  "CATEGORY"
putexcel B1 =  "REGRESSOR"
putexcel C1 =  "COEFFICIENT"

* Estimated coefficients 
scalar no_coefs_all = colsof(b)

* Eliminate rows and columns containing zeros (baseline cats) 
mata:
	// Call matrices into mata 
    b = st_matrix("b")

    // Find which coefficients are nonzero
    keep = (b :!= 0)
	
    // Eliminate zeros
	nonzero_b = select(b, keep)

	// Inspect
	nonzero_b 
	
    // Return to Stata
    st_matrix("nonzero_b", nonzero_b)
	st_matrix("nonzero_b_flag", keep)
end	

* Inspect
matrix list b 
matrix list nonzero_b
matrix list nonzero_b_flag

* Save dimensions
scalar no_nonzero_b = colsof(nonzero_b)
scalar no_nonzero_b_per = no_nonzero_b / 4 // number of categories-1 

* Address repetition of proportional odds covariates

* Generate repetition/unique observation flag
mata:
	// Import matrices into mata
	nonzero_b_mata = st_matrix("nonzero_b")
	
	// Generate binary vector =1 if coefficient repeated 
	n = cols(nonzero_b_mata)
	repetition_flag = J(n, 1, 0)

	// use tolerance based comparison to avoid precision errors 
	tol = 1e-8

		for (i = 1; i <= n; i++) {
			for (j = 1; j <= n; j++) {
				if (i != j && abs(nonzero_b_mata[i] - nonzero_b_mata[j]) < tol) {
					repetition_flag[i] = 1
					break
				}
			}
	}
	repetition_flag

	// Generate binary vector =1 if coefficient not repeated 
	unique_flag  = 1 :- repetition_flag

	// Return to Stata
	st_matrix("repetition_flag", repetition_flag')
	st_matrix("unique_flag", unique_flag')

end

* Generate vector to multiply the coef vector with to eliminate the 
* repetitions of coefficients for vars that satify the proportional odds assumptions
matrix structure_a = J(1,no_nonzero_b_per,1)
matrix structure_b = unique_flag[1,no_nonzero_b_per+1..no_nonzero_b]
matrix structure = structure_a, structure_b

* Inspect
matrix list structure_a
matrix list structure_b
matrix list structure
matrix list nonzero_b

* Eliminate repetitions 
mata:
	// Call matrices into mata 
	var = st_matrix("var")
	structure = st_matrix("structure")
	nonzero_b = st_matrix("nonzero_b")
	
	// Convert reptitions into zeros 
	b_structure = structure :* nonzero_b

	b_structure 
	
	// Eliminate zeros 
	keep = (b_structure :!= 0)
	
	nonzero_b_structure = select(b_structure, keep)
	
	// Export to Stata
	st_matrix("b_structure", b_structure)
	st_matrix("nonzero_b_structure", nonzero_b_structure)

end

matrix list nonzero_b_structure

* Export into Excel 
putexcel set "$dir_results/reg_education", sheet("E2a") modify
putexcel A1 = matrix(nonzero_b_structure'), names //nformat(number_d2) 



* Variance-covariance matrix 
* ELiminate zeros (baseline categories)
mata:
    V = st_matrix("V")
    b = st_matrix("b")

    // Find which coefficients are nonzero
    keep = (b :!= 0)
	
	// Eliminate zeros 
    V_trimmed = select(V, keep)
    V_trimmed = select(V_trimmed', keep)'

	V_trimmed 
	
    // Return to Stata
    st_matrix("var", V_trimmed)
end			

matrix list var

* Address repetition due to proportional odds being satisfied for some covars
matrix square_structure_a = J(no_nonzero_b,1,1) * structure
matrix square_structure_b = square_structure_a'

matrix list square_structure_a
matrix list square_structure_b
mata:
	// Call matrices into mata 
	var = st_matrix("var")
	
	// Create structure matrix (0 = eliminate)
	square_structure_a = st_matrix("square_structure_a")
	square_structure_b = st_matrix("square_structure_b")
	
	// Element-by-element multiplication
	square_structure = square_structure_a :* square_structure_b 
	var_structure = square_structure :* var
	
	// Eliminate zeros 
	row_keep = rowsum(abs(var_structure)) :!= 0
	col_keep = colsum(abs(var_structure)) :!= 0

	nonzero_var_structure = select(select(var_structure, row_keep), col_keep)

	// Return to Stata
	st_matrix("nonzero_var_structure", nonzero_var_structure)
end

matrix list nonzero_var_structure

* Export to Excel 
putexcel set "$dir_results/reg_education", sheet("E2a") modify
putexcel C2 = matrix(nonzero_var_structure)
		
			
* Labels
putexcel set "$dir_results/reg_education", sheet("E2a") modify

putexcel A1 = "REGRESSOR"
putexcel B1 = "COEFFICIENT"

/* Create temporary frame ==> not available in stata 14
frame create temp_frame
frame temp_frame: {
    
    mata: 
		// Import matrices from Stata
		nonzero_b_flag = st_matrix("nonzero_b_flag")'
		unique_flag = st_matrix("unique_flag")'
		structure = st_matrix("structure")'
		stripe = st_matrixcolstripe("e(b)")
		
		// Extract variable and category names
		catnames = stripe[.,1]
		varnames = stripe[.,2]
		varnames_no_bl = select(varnames, nonzero_b_flag :== 1)
		catnames_no_bl = select(catnames, nonzero_b_flag :== 1)
		
		// Create and clean labels 
		// Address lags
		labels_no_bl = regexm(varnames_no_bl, "^L_") :* (regexr(varnames_no_bl, "^L_", "") :+ "_L1") :+ (!regexm(varnames_no_bl, "^L_") :* varnames_no_bl)
		
		// Add category 
		labels_no_bl = labels_no_bl :+ "_" :+ (catnames_no_bl :* (unique_flag[1::rows(labels_no_bl)] :!= 0))
		
		// Remove 1. 
		labels_no_bl = usubinstr(labels_no_bl, "1.", "", 1)
		
		// Constant 
		labels_no_bl = regexr(labels_no_bl, "^_cons", "Constant")
					
		nonzero_labels_structure = select(labels_no_bl, structure[1::rows(labels_no_bl)] :== 1)
		
		// Add v1
		nonzero_labels_structure = "v1"\nonzero_labels_structure
		
		// Create temp file with results
		fh = fopen("$dir_results/temp_labels.txt", "w")
		for (i=1; i<=rows(nonzero_labels_structure); i++) {
			fput(fh, nonzero_labels_structure[i])
		}
		fclose(fh)
    end
 */
 * Here's a replacement for stata 14: 
local dir_results "$dir_results"  

preserve
* Run Mata block
mata: 
    // Import matrices from Stata
    nonzero_b_flag = st_matrix("nonzero_b_flag")'
    unique_flag = st_matrix("unique_flag")'
    structure = st_matrix("structure")'
    stripe = st_matrixcolstripe("e(b)")
    
    // Extract variable and category names
    catnames = stripe[.,1]
    varnames = stripe[.,2]
    varnames_no_bl = select(varnames, nonzero_b_flag :== 1)
    catnames_no_bl = select(catnames, nonzero_b_flag :== 1)
    
    // Handle lags
    labels_no_bl = regexm(varnames_no_bl, "^L_") :* (regexr(varnames_no_bl, "^L_", "") :+ "_L1") :+ (!regexm(varnames_no_bl, "^L_") :* varnames_no_bl)
    
    // Add category name when flag is not unique
    labels_no_bl = labels_no_bl :+ "_" :+ (catnames_no_bl :* (unique_flag[1::rows(labels_no_bl)] :!= 0))
    
    // Clean labels
    labels_no_bl = usubinstr(labels_no_bl, "1.", "", 1)
    labels_no_bl = regexr(labels_no_bl, "^_cons", "Constant")
    
    // Filter for structure == 1
    nonzero_labels_structure = select(labels_no_bl, structure[1::rows(labels_no_bl)] :== 1)
    
    // Add header row
    nonzero_labels_structure = "v1"\nonzero_labels_structure
    
    // Write to temporary file
    fh = fopen(st_local("dir_results") + "/temp_labels.txt", "w")
    for (i=1; i<=rows(nonzero_labels_structure); i++) {
        fput(fh, nonzero_labels_structure[i])
    }
    fclose(fh)
end

    * Import cleaned labels into Stata as new dataset
    import delimited "$dir_results/temp_labels.txt", clear varnames(1) encoding(utf8)
	gen n = _n
    
    * Export labels to Excel
    putexcel set "$dir_results/reg_education", sheet("E2a") modify
	
	* Vertical labels
    sum n, meanonly
	local N = r(max)+1
	
	forvalue i = 2/`N' {
		local j = `i' - 1
		putexcel A`i' = v1[`j'] 
	}
	
	* Horizontal labels
	sum n, meanonly
	local N = r(max) + 1  // Adjusted since we're working across columns

	forvalues j = 1/`N' {
		local n = `j'+2 // Shift by 2 to start from column C
		local col ""
		
		while `n' > 0 {
			local rem = mod(`n' - 1, 26)
			local col = char(65 + `rem') + "`col'"
			local n = floor((`n' - 1)/26)
		}

		putexcel `col'1 = v1[`j']
	}	
		
    *Clean up
    erase "$dir_results/temp_labels.txt"


* Goodness of fit

putexcel set "$dir_results/reg_education", sheet("Gof") modify

putexcel A13 = "E2a - Education attainment, not in initial education spell", bold		

putexcel A15 = "Pseudo R-squared" 
putexcel B15 = r2_p 
putexcel A16 = "N"
putexcel B16 = N_sample

restore		
* Clean up 		
drop in_sample p1 p2 p3
scalar drop _all
matrix drop _all
//frame drop temp_frame 	


capture log close
