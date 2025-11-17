********************************************************************************
* PROJECT:  		ESPON
* SECTION:			Home ownership 
* OBJECT: 			Final Regresion Models - Weighted
* AUTHORS:			Daria Popova, Justin van de Ven
* LAST UPDATE:		26 Aug 2025 DP  
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


/*sample selection 
drop if dag < 16

xtset idperson swv
*/

* Set Excel file 

* Info sheet

putexcel set "$dir_results/reg_home_ownership", sheet("Info") replace
putexcel A1 = "Description:"
putexcel B1 = "Model parameters governing projection of home ownership"
putexcel A2 = "Authors:	Patryk Bronka, Justin van de Ven, Daria Popova" 
putexcel A3 = "Last edit: 4 Nov 2025 DP"

putexcel A4 = "Process:", bold
putexcel B4 = "Description:", bold
putexcel A5 = "HO1a"
putexcel B5 = "Probit regression estimates of the probability of being a home owner, aged 18+"

putexcel A10 = "Notes:", bold
putexcel B10 = "Have combined dhhtp_c4 and lessp_c3 into a single variable with 8 categories, dhhtp_c8"
putexcel B11 = "Added lagged home ownership, replaced dhe with dhe_pcs and dhe_mcs, added ethnicity (dot) and covid dummies (y2020 2021)"
putexcel B12 = "Re-estimated process at benefit unit level to be consistent with SimPaths"

putexcel set "$dir_results/reg_home_ownership", sheet("Gof") modify
putexcel A1 = "Goodness of fit", bold		


************************
* HO1a: Home ownership *
************************

* Process HO1a: Probability of being a home owner 
* Sample: Individuals aged 18+ who are benefit unit heads 
* DV: Home ownerhip dummy

/*
fre dhh_owned if dag >= 18

/////////////////////////////////////////////////////////////////////////////////////////////////	 
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

	
probit dhh_owned dgn dag dagsq il.dhhtp_c8 il.les_c3 ///
i.deh_c3 /*il.dhe*/ l.dhe_mcs l.dhe_pcs il.ydses_c5 l.yptciihs_dv l.dhh_owned ib8.drgn1 stm y2020 y2021 i.dot if ///
dag>=18 [pweight=dimxwt], vce(cluster idperson)
*/	

* DEFINE BENEFIT UNIT HEAD (AGED 18+)

* Keep adults (18+)
keep if dag >= 18


* Count unique benefit-unit–wave combinations BEFORE head selection
egen tag_bu_wave = tag(idbenefitunit swv)
count if tag_bu_wave
local n_bu_before = r(N)
display "Number of benefit unit–wave combinations BEFORE selecting head: `n_bu_before'"


* Sort benefit unit members within each wave:
* 1. Highest non-benefit income (ypnbihs_dv)
* 2. Highest age (dag)
* 3. Lowest idperson (idperson)
gsort idbenefitunit swv -ypnbihs_dv -dag idperson 

* Tag the first person (the "head") per benefit unit and wave
bysort idbenefitunit swv: gen benunit_head = (_n == 1)

* Keep only benefit unit heads
keep if benunit_head == 1

* Count unique benefit-unit–wave combinations AFTER head selection
drop tag_bu_wave
egen tag_bu_wave = tag(idbenefitunit swv)
count if tag_bu_wave
local n_bu_after = r(N)
display "Number of benefit unit–wave combinations AFTER selecting head: `n_bu_after'"

* Ensure benefit unit–wave counts match before and after head selection
assert `n_bu_before' == `n_bu_after'

* Verify only one head per benefit unit per wave
by idbenefitunit swv, sort: gen n=_N
assert n==1

* Declare panel 
xtset idperson swv 


********************************************************************************
* SET EXCEL OUTPUT FILES
********************************************************************************

* Info sheet
putexcel set "$dir_results/reg_home_ownership", sheet("Info") replace
putexcel A1 = "Description:"
putexcel B1 = "Model parameters governing projection of home ownership"
putexcel A2 = "Authors:	Patryk Bronka, Justin van de Ven, Daria Popova" 
putexcel A3 = "Last edit: 4 Nov 2025 DP"

putexcel A4 = "Process:", bold
putexcel B4 = "Description:", bold
putexcel A5 = "HO1a"
putexcel B5 = "Probit regression estimates of the probability of being a home owner, benefit unit heads aged 18+"

putexcel A10 = "Notes:", bold
putexcel B10 = "Have combined dhhtp_c4 and lessp_c3 into a single variable with 8 categories, dhhtp_c8"
putexcel B11 = "Added lagged home ownership, replaced dhe with dhe_pcs and dhe_mcs, added ethnicity (dot) and covid dummies (y2020, y2021)"
putexcel B12 = "Re-estimated process at benefit unit level using heads defined by highest personal non-benefit income, or age, or lowest idperson"

putexcel set "$dir_results/reg_home_ownership", sheet("Gof") modify
putexcel A1 = "Goodness of fit", bold		


********************************************************************************
* HO1a: Home ownership 
********************************************************************************
	
probit dhh_owned Dgn Dag Dag_sq ///
       Dhhtp_c8_2_L1 Dhhtp_c8_3_L1 Dhhtp_c8_4_L1 Dhhtp_c8_5_L1 Dhhtp_c8_6_L1 Dhhtp_c8_7_L1 Dhhtp_c8_8_L1 ///
       Les_c3_Student_L1 Les_c3_NotEmployed_L1 ///
	   Deh_c3_Medium Deh_c3_Low ///
	   Dhe_mcs_L1 Dhe_pcs_L1 ///
	   Ydses_c5_Q2_L1 Ydses_c5_Q3_L1 Ydses_c5_Q4_L1 Ydses_c5_Q5_L1 ///
	   Yptciihs_dv_L1 ///
	   Dhh_owned_L1 ///
	   UKC UKD UKE UKF UKG UKH UKJ UKK UKL UKM UKN /// 
	   Year_transformed Y2020 Y2021 Ethn_Asian Ethn_Black Ethn_Other ///
	   [pweight = dimxwt], vce(cluster idperson)  
	   

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
putexcel set "$dir_results/reg_home_ownership", sheet("UK_HO1a") modify 
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

putexcel set "$dir_results/reg_home_ownership", sheet("UK_HO1a") modify
putexcel A1 = matrix(nonzero_b'), names nformat(number_d2) 	
	

* Labelling 
// Need to variable label when add new variable to model. Order matters. 
local var_list Dgn Dag Dag_sq ///
       Dhhtp_c8_2_L1 Dhhtp_c8_3_L1 Dhhtp_c8_4_L1 Dhhtp_c8_5_L1 Dhhtp_c8_6_L1 Dhhtp_c8_7_L1 Dhhtp_c8_8_L1 ///
       Les_c3_Student_L1 Les_c3_NotEmployed_L1 ///
	   Deh_c3_Medium Deh_c3_Low ///
	   Dhe_mcs_L1 Dhe_pcs_L1 ///
	   Ydses_c5_Q2_L1 Ydses_c5_Q3_L1 Ydses_c5_Q4_L1 Ydses_c5_Q5_L1 ///
	   Yptciihs_dv_L1 ///
	   Dhh_owned_L1 ///
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

