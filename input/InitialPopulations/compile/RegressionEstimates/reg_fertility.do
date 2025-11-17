*********************************************************************************
* PROJECT:  		ESPON 
* SECTION:			Fertility
* OBJECT: 			Final Probit Models
* AUTHORS:			Daria Popova, Justin van de Ven
* LAST UPDATE:		21 Oct 2025 DP  
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
putexcel A3 = "Last edit: 3 Nov 2025 DP"

putexcel A4 = "Process:", bold
putexcel B4 = "Description:", bold
putexcel A5 = "F1a"
putexcel B5 = "Probit regression estimates of the probability of  having a child for women aged 18-44 in initial education spell"
putexcel A6 = "F1b"
putexcel B6 = "Probit regression estimates of probability of having a child for women aged 18-44 not in initial education spell"

putexcel A10 = "Notes:", bold
putexcel B10 = "All processes: replaced dhe with dhe_pcs and dhe_mcs, added ethnicity-4 cat (dot), covid dummies (y2020 y2021)"
putexcel B11 = "F1a: only 24 obs having a child when in initial education spell, therefore have to take away some covariates to obtain estimate"
putexcel B12 = "All processes: replaced dcpst with a dummy version (1=partnered 2=single)"

putexcel set "$dir_results/reg_fertility", sheet("Gof") modify
putexcel A1 = "Goodness of fit", bold		

xtset idperson swv

**********************************************
* F1a - Having a child, in initial edu spell * 
**********************************************

* Process F1a: Probabiltiy of having a child 
* Sample: Women aged 18-44, in initial education spell education.
* DV: New born child dummy (note that in the estimation sample dchpd contains the number of newborn children, which could be >1) 
tab sprfm dgn
replace dchpd=1 if dchpd>1 & dchpd<. 
replace dchpd = 0 if dchpd==-9 
tab2 swv dchpd, row

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

probit dchpd Dag /*dhe dhe_mcs dhe_pcs li.Dcpst_Single*/ Year_transformed /*y2020 y2021*/ Ethn_Asian Ethn_Black Ethn_Other if ///
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
putexcel set "$dir_results/reg_fertility", sheet("F1a") modify
putexcel B2 = matrix(b_trimmed)
putexcel C2 = matrix(V_trimmed)


* Labelling 
// Need to variable label when add new variable to model. Order matters. 
local var_list Dag Year_transformed Ethn_Asian Ethn_Black Ethn_Other Constant
	   
	
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


* Export model fit statistics
putexcel set "$dir_results/reg_fertility", sheet("Gof") modify

putexcel A9 = "F1a - Fertility, in initial education spell", bold		

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

probit dchpd Dag Dag_sq Ydses_c5_Q2_L1 Ydses_c5_Q3_L1 Ydses_c5_Q4_L1 Ydses_c5_Q5_L1 ///
    Dnc_L1 Dnc02_L1 ///
	Dhe_pcs Dhe_mcs ///
	Dcpst_Single_L1  ///
	Deh_c3_Medium Deh_c3_Low ///
	FertilityRate ///
	Les_c3_Student_L1 Les_c3_NotEmployed_L1 ///
	UKC UKD UKE UKF UKG UKH UKJ UKK UKL UKM UKN ///
	Year_transformed Y2020 Y2021 Ethn_Asian Ethn_Black Ethn_Other ///
if (sprfm == 1 & ded == 0) [pweight = dimxwt], vce(robust)	
	

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
putexcel set "$dir_results/reg_fertility", sheet("F1b") modify
putexcel B2 = matrix(b_trimmed)
putexcel C2 = matrix(V_trimmed)

* Labelling 
// Need to variable label when add new variable to model. Order matters. 
local var_list Dag Dag_sq Ydses_c5_Q2_L1 Ydses_c5_Q3_L1 Ydses_c5_Q4_L1 Ydses_c5_Q5_L1 ///
    Dnc_L1 Dnc02_L1 ///
	Dhe_pcs Dhe_mcs ///
	Dcpst_Single_L1  ///
	Deh_c3_Medium Deh_c3_Low ///
	FertilityRate ///
	Les_c3_Student_L1 Les_c3_NotEmployed_L1 ///
	UKC UKD UKE UKF UKG UKH UKJ UKK UKL UKM UKN ///
	Year_transformed Y2020 Y2021 Ethn_Asian Ethn_Black Ethn_Other Constant
	   
	
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

* Export model fit statistics
putexcel set "$dir_results/reg_fertility", sheet("Gof") modify

putexcel A9 = "F1b - Fertility, left initial education spell", bold		

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

