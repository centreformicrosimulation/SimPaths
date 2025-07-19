********************************************************************************
* PROJECT:  		ESPON
* SECTION:			Retirement  
* OBJECT: 			Final Regresion Models 
* AUTHORS:			Daria Popova, Justin van de Ven
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
log using "${dir_log}/reg_retirement.log", replace
*******************************************************************

use "$dir_ukhls_data/ukhls_pooled_all_obs_09.dta", clear

do "$dir_do/variable_update"

	
* sample selection 
drop if dag < 16


xtset idperson swv


* Set Excel file 

* Info sheet

putexcel set "$dir_results/reg_retirement", sheet("Info") replace
putexcel A1 = "Description:"
putexcel B1 = "Model parameters governing projection of retirement"
putexcel A2 = "Authors:	Patryk Bronka, Justin van de Ven, Daria Popova" 
putexcel A3 = "Last edit: 1 July 2025 DP"

putexcel A4 = "Process:", bold
putexcel B4 = "Description:", bold

putexcel A5 = "R1a"
putexcel B5 = "Probit regression estimates of the probability of retiring, single individuals aged 50+ not yet retired"

putexcel A6 = "R1b"
putexcel B6 = "Probit regression estimates of the probability of retiring, cohabiting individuals aged 50+ not yet retired"

putexcel A10 = "Notes:", bold
putexcel B10 = "replaced dlltsd with dlltsd01; added dhe_pcs and dhe_mcs, ethnicity-4 cat(dot) and Covid dummies (y2020 y2021)"

putexcel set "$dir_results/reg_retirement", sheet("Gof") modify
putexcel A1 = "Goodness of fit", bold		


****************************
* R1a: Retirement - Single *
****************************

* Process R1a: Probability retire if single 
* Sample: Non-partnered individuals aged 50+ who are not yet retired.
* DV: Enter retirement dummy (have to not be retired last year)

fre drtren if ((dcpst==2 | dcpst==3) & dag>=50)

/*/////////////////////////////////////////////////////////////////////////////////////////////////	 
//check weights //////////////////////////////////////////////////////////////////////////////////	 
probit drtren i.dgn dag dagsq ib1.deh_c3 i.dagpns li.lesnr_c2 ///
    li.ydses_c5 li.dlltsd ib8.drgn1 stm y2020 y2021 i.dot ///
 if ((dcpst==2 | dcpst==3) & dag>=50) [pweight=dimlwt], vce(robust)
outreg2 using "${weight_checks}/weight_comparison_R1a.xls", alpha(0.001, 0.01, 0.05, 0.1) symbol(***, **, *, +) replace ctitle(R1a, dimlwt) side dec(4) 

probit drtren i.dgn dag dagsq ib1.deh_c3 i.dagpns li.lesnr_c2 ///
    li.ydses_c5 li.dlltsd ib8.drgn1 stm y2020 y2021 i.dot ///
 if ((dcpst==2 | dcpst==3) & dag>=50) [pweight=disclwt], vce(robust)
outreg2 using "${weight_checks}/weight_comparison_R1a.xls", alpha(0.001, 0.01, 0.05, 0.1) symbol(***, **, *, +) append ctitle(R1a, disclwt) side dec(4)

probit drtren i.dgn dag dagsq ib1.deh_c3 i.dagpns li.lesnr_c2 ///
    li.ydses_c5 li.dlltsd ib8.drgn1 stm y2020 y2021 i.dot ///
 if ((dcpst==2 | dcpst==3) & dag>=50) [pweight=dimxwt], vce(robust)
outreg2 using "${weight_checks}/weight_comparison_R1a.xls", alpha(0.001, 0.01, 0.05, 0.1) symbol(***, **, *, +) append ctitle(R1a, dimxwt) side dec(4) 
erase "${weight_checks}/weight_comparison_R1a.txt"
//////////////////////////////////////////////////////////////////////////////////////////////////// 
////////////////////////////////////////////////////////////////////////////////////////////////////
*/
probit drtren i.dgn dag dagsq ib1.deh_c3 i.dagpns li.lesnr_c2 ///
    li.ydses_c5 li.dlltsd01 l.dhe_pcs l.dhe_mcs ///
	ib8.drgn1 stm y2020 y2021 i.dot ///
 if ((dcpst==2 | dcpst==3) & dag>=50) [pweight=dimxwt], vce(robust)

   * raw results 
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_raw_results/retirement/retirement", sheet("Process R1a") replace
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))
outreg2 stats(coef se pval) using "$dir_raw_results/retirement/R1a.doc", replace ///
title("Process R1a: Probit regression estimates for retiring - single individuals aged 50+ not yet retired") ///
 ctitle(retiring) label side dec(2) noparen addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll))
gen in_sample = e(sample)	

predict p

save "$dir_validation_data/R1a_sample", replace

scalar r2_p = e(r2_p) 
scalar N = e(N)	
scalar chi2 = e(chi2)
scalar ll = e(ll)	
	
	
* Rresults	
* Note: Zeros values are eliminated 
	
matrix b = e(b)	
matrix V = e(V)


* Store variance-covariance matrix 

preserve

putexcel set "$dir_raw_results/retirement/var_cov", sheet("var_cov") ///
	replace
putexcel A1 = matrix(V)

import excel "$dir_raw_results/retirement/var_cov", sheet("var_cov") clear

describe
local no_vars = `r(k)'	
	
forvalues i = 1/2 {
	egen row_sum = rowtotal(*)
	drop if row_sum == 0 
	drop row_sum
	xpose, clear	
}	
	
mkmat v*, matrix(var)	
putexcel set "$dir_results/reg_retirement", sheet("R1a") modify
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

putexcel set "$dir_results/reg_retirement", sheet("R1a") modify
putexcel A1 = matrix(nonzero_b'), names nformat(number_d2)  
 
 
* Labelling 
 
putexcel A1 = "REGRESSOR"
putexcel A2 = "Dgn"
putexcel A3 = "Dag"
putexcel A4 = "Dag_sq"
putexcel A5 = "Deh_c3_Medium"
putexcel A6 = "Deh_c3_Low"
putexcel A7 = "Reached_Retirement_Age"
putexcel A8 = "Lesnr_c2_NotEmployed_L1"
putexcel A9 = "Ydses_c5_Q2_L1"
putexcel A10 = "Ydses_c5_Q3_L1"
putexcel A11 = "Ydses_c5_Q4_L1"
putexcel A12 = "Ydses_c5_Q5_L1"
putexcel A13 = "Dlltsd01_L1"
putexcel A14 = "Dhe_pcs_L1"
putexcel A15 = "Dhe_mcs_L1"
putexcel A16 = "UKC"
putexcel A17 = "UKD"
putexcel A18 = "UKE"
putexcel A19 = "UKF"
putexcel A20 = "UKG"
putexcel A21 = "UKH"
putexcel A22 = "UKJ"
putexcel A23 = "UKK"
putexcel A24 = "UKL"
putexcel A25 = "UKM"
putexcel A26 = "UKN"
putexcel A27 = "Year_transformed"
putexcel A28 = "Y2020"
putexcel A29 = "Y2021"
putexcel A30 = "Ethn_Asian"
putexcel A31 = "Ethn_Black"
putexcel A32 = "Ethn_Other"
putexcel A33 = "Constant"

putexcel B1 = "COEFFICIENT"
putexcel C1 = "Dgn"
putexcel D1 = "Dag"
putexcel E1 = "Dag_sq"
putexcel F1 = "Deh_c3_Medium"
putexcel G1 = "Deh_c3_Low"
putexcel H1 = "Reached_Retirement_Age"
putexcel I1 = "Lesnr_c2_NotEmployed_L1"
putexcel J1 = "Ydses_c5_Q2_L1"
putexcel K1 = "Ydses_c5_Q3_L1"
putexcel L1 = "Ydses_c5_Q4_L1"
putexcel M1 = "Ydses_c5_Q5_L1"
putexcel N1 = "Dlltsd01_L1"
putexcel O1 = "Dhe_pcs_L1"
putexcel P1 = "Dhe_mcs_L1"
putexcel Q1 = "UKC"
putexcel R1 = "UKD"
putexcel S1 = "UKE"
putexcel T1 = "UKF"
putexcel U1 = "UKG"
putexcel V1 = "UKH"
putexcel W1 = "UKJ"
putexcel X1 = "UKK"
putexcel Y1 = "UKL"
putexcel Z1 = "UKM"
putexcel AA1 = "UKN"
putexcel AB1 = "Year_transformed"
putexcel AC1 = "Y2020"
putexcel AD1 = "Y2021"
putexcel AE1 = "Ethn_Asian"
putexcel AF1 = "Ethn_Black"
putexcel AG1 = "Ethn_Other"
putexcel AH1 = "Constant"


* Goodness of fit

putexcel set "$dir_results/reg_retirement", sheet("Gof") modify

putexcel A3 = "R1a - Retirement single", bold		

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




******************************
* R1b: Retirement, partnered *
******************************

* Process R1b: Probability retire 
* Sample: Partnered heterosexual individuals aged 50+ who are not yet retired
* DV: Enter retirement dummy (have to not be retired last year)
count if (ssscp!=1 & dcpst==1 & dag>=50) & lessp_c3==2 //115 obs partnered with students 
drop if (ssscp!=1 & dcpst==1 & dag>=50) & lessp_c3==2 //drop partnered with students 

fre drtren if (ssscp!=1 & dcpst==1 & dag>=50)

/*//////////////////////////////////////////////////////////////////////////////////////////////////	 
//check weights //////////////////////////////////////////////////////////////////////////////////	 
probit drtren i.dgn dag dagsq ib1.deh_c3 i.dagpns li.lesnr_c2 ///
     i.dagpns#li.lesnr_c2 li.ydses_c5 li.dlltsd i.dagpns_sp ///
     li.lessp_c3 li.dlltsd_sp ib8.drgn1 stm  y2020 y2021 i.dot if ///
	 (ssscp!=1 & dcpst==1 & dag>=50)  [pweight=dimlwt], vce(robust)
outreg2 using "${weight_checks}/weight_comparison_R1b.xls", alpha(0.001, 0.01, 0.05, 0.1) symbol(***, **, *, +) replace ctitle(R1b, dimlwt) side dec(4) 

probit drtren i.dgn dag dagsq ib1.deh_c3 i.dagpns li.lesnr_c2 ///
     i.dagpns#li.lesnr_c2 li.ydses_c5 li.dlltsd i.dagpns_sp ///
     li.lessp_c3 li.dlltsd_sp ib8.drgn1 stm  y2020 y2021 i.dot if ///
	 (ssscp!=1 & dcpst==1 & dag>=50)  [pweight=disclwt], vce(robust)
outreg2 using "${weight_checks}/weight_comparison_R1b.xls", alpha(0.001, 0.01, 0.05, 0.1) symbol(***, **, *, +) append ctitle(R1b, disclwt) side dec(4)

probit drtren i.dgn dag dagsq ib1.deh_c3 i.dagpns li.lesnr_c2 ///
     i.dagpns#li.lesnr_c2 li.ydses_c5 li.dlltsd i.dagpns_sp ///
     li.lessp_c3 li.dlltsd_sp ib8.drgn1 stm  y2020 y2021 i.dot if ///
	 (ssscp!=1 & dcpst==1 & dag>=50)  [pweight=dimxwt], vce(robust)
outreg2 using "${weight_checks}/weight_comparison_R1b.xls", alpha(0.001, 0.01, 0.05, 0.1) symbol(***, **, *, +) append ctitle(R1b, dimxwt) side dec(4) 
erase "${weight_checks}/weight_comparison_R1b.txt"
//////////////////////////////////////////////////////////////////////////////////////////////////// 
////////////////////////////////////////////////////////////////////////////////////////////////////
*/

probit drtren i.dgn dag dagsq ib1.deh_c3 i.dagpns li.lesnr_c2 ///
     i.dagpns#li.lesnr_c2 li.ydses_c5 li.dlltsd01 l.dhe_pcs l.dhe_mcs i.dagpns_sp ///
     li.lessp_c3 li.dlltsd01_sp ib8.drgn1 stm y2020 y2021 i.dot if ///
	 (ssscp!=1 & dcpst==1 & dag>=50) [pweight=dimxwt], vce(robust)

   * raw results 
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_raw_results/retirement/retirement", sheet("Process R1b") modify
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))
outreg2 stats(coef se pval) using "$dir_raw_results/retirement/R1b.doc", replace ///
title("Process R1b: Probit regression estimates for retiring - cohabiting individuals aged 50+ not yet retired") ///
 ctitle(retiring) label side dec(2) noparen addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll))
	

gen in_sample = e(sample)	

predict p

save "$dir_validation_data/R1b_sample", replace

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

putexcel set "$dir_raw_results/retirement/var_cov", sheet("var_cov") replace
putexcel A1 = matrix(V)

import excel "$dir_raw_results/retirement/var_cov", sheet("var_cov") clear

describe
local no_vars = `r(k)'	
	
forvalues i = 1/2 {
	egen row_sum = rowtotal(*)
	drop if row_sum == 0 
	drop row_sum
	xpose, clear	
}	
	
mkmat v*, matrix(var)	
putexcel set "$dir_results/reg_retirement", sheet("R1b") modify
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

putexcel set "$dir_results/reg_retirement", sheet("R1b") modify
putexcel A1 = matrix(nonzero_b'), names nformat(number_d2) 
	

* Labelling

putexcel A1 = "REGRESSOR"
putexcel A2 = "Dgn"
putexcel A3 = "Dag"
putexcel A4 = "Dag_sq"
putexcel A5 = "Deh_c3_Medium"
putexcel A6 = "Deh_c3_Low"
putexcel A7 = "Reached_Retirement_Age"
putexcel A8 = "Lesnr_c2_NotEmployed_L1"
putexcel A9 = "Reached_Retirement_Age_Lesnr_c2_NotEmployed_L1"
putexcel A10 = "Ydses_c5_Q2_L1"
putexcel A11 = "Ydses_c5_Q3_L1"
putexcel A12 = "Ydses_c5_Q4_L1"
putexcel A13 = "Ydses_c5_Q5_L1"
putexcel A14 = "Dlltsd01_L1"
putexcel A15 = "Dhe_pcs_L1"
putexcel A16 = "Dhe_mcs_L1"
putexcel A17 = "Reached_Retirement_Age_Sp"
putexcel A18 = "Lessp_c3_NotEmployed_L1"
putexcel A19 = "Dlltsd01_sp_L1"
putexcel A20 = "UKC"
putexcel A21 = "UKD"
putexcel A22 = "UKE"
putexcel A23 = "UKF"
putexcel A24 = "UKG"
putexcel A25 = "UKH"
putexcel A26 = "UKJ"
putexcel A27 = "UKK"
putexcel A28 = "UKL"
putexcel A29 = "UKM"
putexcel A30 = "UKN"
putexcel A31 = "Year_transformed"
putexcel A32 = "Y2020"
putexcel A33 = "Y2021"
putexcel A34 = "Ethn_Asian"
putexcel A35 = "Ethn_Black"
putexcel A36 = "Ethn_Other"
putexcel A37 = "Constant"

putexcel B1 = "COEFFICIENT"
putexcel C1 = "Dgn"
putexcel D1 = "Dag"
putexcel E1 = "Dag_sq"
putexcel F1 = "Deh_c3_Medium"
putexcel G1 = "Deh_c3_Low"
putexcel H1 = "Reached_Retirement_Age"
putexcel I1 = "Lesnr_c2_NotEmployed_L1"
putexcel J1 = "Reached_Retirement_Age_Les_c3_NotEmployed_L1"
putexcel K1 = "Ydses_c5_Q2_L1"
putexcel L1 = "Ydses_c5_Q3_L1"
putexcel M1 = "Ydses_c5_Q4_L1"
putexcel N1 = "Ydses_c5_Q5_L1"
putexcel O1 = "Dlltsd01_L1"
putexcel P1 = "Dhe_pcs_L1"
putexcel Q1 = "Dhe_mcs_L1"
putexcel R1 = "Reached_Retirement_Age_Sp"
putexcel S1 = "Lessp_c3_NotEmployed_L1"
putexcel T1 = "Dlltsd01_sp_L1"
putexcel U1 = "UKC"
putexcel V1 = "UKD"
putexcel W1 = "UKE"
putexcel X1 = "UKF"
putexcel Y1 = "UKG"
putexcel Z1 = "UKH"
putexcel AA1 = "UKJ"
putexcel AB1 = "UKK"
putexcel AC1 = "UKL"
putexcel AD1 = "UKM"
putexcel AE1 = "UKN"
putexcel AF1 = "Year_transformed"
putexcel AG1 = "Y2020"
putexcel AH1 = "Y2021"
putexcel AI1 = "Ethn_Asian"
putexcel AJ1 = "Ethn_Black"
putexcel AK1 = "Ethn_Other"
putexcel AL1 = "Constant"


* Goodness of fit

putexcel set "$dir_results/reg_retirement", sheet("Gof") modify

putexcel A9 = "R1b - Retirement partnered", bold		

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

