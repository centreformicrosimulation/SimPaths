/*******************************************************************************
*
*	PROGRAM TO EVALUATE SOCIAL CARE RECEIPT FROM UKHLS DATA
*	ANALYSIS BASED ON THE SOCIAL CARE MODULE OF UKHLS
*	First version: Justin van de Ven, 28 Aug 2023
*	Refactored version: Matteo Richiardi, 16 Feb 2026
*
*******************************************************************************/

/* ANALYTICAL STRATEGY
We analyse/simulate the following variables:
- NeedCare
- ReceiveCare
- CareMarket: Formal, Informal, Mixed
- HrsReceivedFormalIHS
- HrsReceivedInformalIHS
- ProvideCare
- HrsProvidedInformalIHS
(IHS stands for Inverse Hyperbolic Sine transformation)

The most complicated case is for Partnered, as an issue of consistency arises (care between partners is most common):

===========================================
		Partner B: Receiving informal care?
___________________________________________
Partner A:		|			No		Yes
providing		|	No	|	(1)		(2)
informal care	|	Yes	|	(3)		(4)
===========================================

In the analysis we do not distinguish whom care is received from, and to whom care is provided. 
However, the cases above imply:
(1) No hrs received, no hrs provided
(2) All hrs received are from non-partner
(3) All hrs provided are to non-partner_socare_hrs
(4) At least some of the hrs received/provided are from/to partner

==========================================================================================
RMK: We first analyse care receipt, and then care provision. This order must be preserved.
==========================================================================================
*/

* CRITICAL: Clear all FIRST
clear all


/********************************* SET DIRECTORIES ****************************/

global dir_work "C:/Users/Matteo/Box/CeMPA shared area/_SimPaths/_SimPathsUK/updated_social_care_estimation"
global dir_results "$dir_work/results"
global dir_raw_results "$dir_work/raw_results"
global dir_data "$dir_work/data"
global dir_input_data "C:/Users/Matteo/Box/CeMPA shared area/_SimPaths/_SimPathsUK/input data preparation_Darias backup folder_keep for now/regression_estimates/data"


/******************************** SET IF CONDITIONS ***************************/
* (see descriptive stats in Regressions section)

global s2a_if_condition "dag > 64 & stm >= 2015 & stm <= 2022"								// Need care
global s2b_if_condition "dag > 64 & stm >= 2016 & stm <= 2021"								// Receive care
global s2c_if_condition "dag > 64 & receive_care & stm >= 2016 & stm <= 2021"				// Care mix received
global s2d_if_condition "dag > 64 & receive_informal_care & stm >= 2016 & stm <= 2021"		// Informal care hours received
global s2e_if_condition "dag > 64 & receive_formal_care & stm >= 2016 & stm <= 2021"		// Formal care hours received

global s3a_if_condition "Single & stm >= 2015"												// Provide care, Singles
global s3b_if_condition "Partnered & stm >= 2015"											// Provide care, Partnered
global s3c_if_condition "provide_informal_care & Single & stm >= 2015"						// Informal care hours provided, Singles
global s3d_if_condition "provide_informal_care & Partnered & stm >= 2015"					// Informal care hours provided, Singles


/********************************* SET EXCEL FILE *****************************/

putexcel set "$dir_results/reg_socialcare", sheet("Info") replace
putexcel A1 = "Description:", bold
putexcel B1 = "Model parameters for social care module"
putexcel A2 = "Authors:", bold
putexcel B2 = "Justin van de Ven, Ashley Burdett, Matteo Richiardi"
putexcel A3 = "Last edit:", bold
putexcel B3 = "16 Feb 2026 MR (Refactored)"

putexcel set "$dir_results/reg_socialcare", sheet("Gof") modify
putexcel A1 = "Goodness of fit", bold


/*==============================================================================
	MATA FUNCTIONS - Define all Mata functions at the top
==============================================================================*/

mata:
mata clear

void trim_matrices() {
	V = st_matrix("V")
	b = st_matrix("b")
	keep = (b :!= 0)
	b_trimmed = select(b, keep)
	V_trimmed = select(V, keep)
	V_trimmed = select(V_trimmed', keep)'
	st_matrix("b_trimmed", b_trimmed)
	st_matrix("V_trimmed", V_trimmed)
	st_matrix("nonzero_b_flag", keep)
	printf("Matrices transferred successfully\n")
}

void write_all_to_excel() {
	b_trimmed = st_matrix("b_trimmed")
	V_trimmed = st_matrix("V_trimmed")
	n = cols(b_trimmed)
	for (i=1; i<=n; i++) {
		row = i + 1
		coef = b_trimmed[1,i]
		stata("quietly putexcel B" + strofreal(row) + " = (" + strofreal(coef) + ")")
	}
	printf("Writing V-C matrix\n")
	for (i=1; i<=n; i++) {
		for (j=1; j<=n; j++) {
			row = i + 1
			col_num = j + 2
			col_name = ""
			temp = col_num
			while (temp > 0) {
				rem = mod(temp - 1, 26)
				col_name = char(65 + rem) + col_name
				temp = floor((temp - 1) / 26)
			}
			val = V_trimmed[i,j]
			stata("quietly putexcel " + col_name + strofreal(row) + " = (" + strofreal(val) + ")")
		}
		if (mod(i, 5) == 0) printf("  Row %g/%g\n", i, n)
	}
	printf("Done\n")
}

void extract_and_export_labels(string scalar sheet) {
	nonzero_b_flag = st_matrix("nonzero_b_flag")'
	stripe = st_matrixcolstripe("e(b)")
	varnames = stripe[.,2]
	varnames_no_bl = select(varnames, nonzero_b_flag :== 1)
	labels_no_bl = usubinstr(varnames_no_bl, "1.", "", 1)
	labels_no_bl = regexr(labels_no_bl, "^_cons", "Constant")
	n_labs = rows(labels_no_bl)
	for (i=1; i<=n_labs; i++) {
		row = i + 1
		stata("quietly putexcel A" + strofreal(row) + " = " + char(34) + labels_no_bl[i] + char(34))
	}
	for (j=1; j<=n_labs; j++) {
		col_num = j + 2
		col_name = ""
		n_temp = col_num
		while (n_temp > 0) {
			rem = mod(n_temp - 1, 26)
			col_name = char(65 + rem) + col_name
			n_temp = floor((n_temp - 1) / 26)
		}
		stata("quietly putexcel " + col_name + "1 = " + char(34) + labels_no_bl[j] + char(34))
	}
}

end


/*==============================================================================
	HELPER PROGRAMS - Modular functions for common operations
==============================================================================*/

* Check matrix eigenvalues for stability
capture program drop check_matrix_stability
program define check_matrix_stability
	
	matrix symeigen X lambda = V_trimmed
	scalar max_eig = lambda[1,1]
	scalar min_ratio = lambda[1, colsof(lambda)] / max_eig
	
	if max_eig < 1.0e-12 {
		display as error "CRITICAL ERROR: Maximum eigenvalue is too small (`max_eig')."
		display as error "The Variance-Covariance matrix is likely singular."
		exit 999
	}
	
	display "Stability Check Passed: Max Eigenvalue is " max_eig
	
	if min_ratio < 1.0e-12 {
		display as error "Matrix is ill-conditioned. Min/Max ratio: " min_ratio
		exit 506
	}
	
	display "Stability Check Passed. Min/Max ratio: " min_ratio
	
end


* Export labels to Excel (both vertical and horizontal)
capture program drop export_labels_to_excel
program define export_labels_to_excel
	
	syntax, sheet(string)
	
	* Set up Excel file
	putexcel set "$dir_results/reg_socialcare", sheet("`sheet'") modify
	
	* Vertical labels
	forvalues i = 1/`n_labels' {
		local row = `i' + 1
		quietly putexcel A`row' = "`lbl`i''"
	}
	
	* Horizontal labels - use Mata to generate column names
	mata: {
		n = strtoreal(st_local("n_labels"))
		for (j=1; j<=n; j++) {
			col_num = j + 2
			col_name = ""
			n_temp = col_num
			while (n_temp > 0) {
				rem = mod(n_temp - 1, 26)
				col_name = char(65 + rem) + col_name
				n_temp = floor((n_temp - 1) / 26)
			}
			st_local("col_" + strofreal(j), col_name)
		}
	}
	
	* Now write using the column names from Mata
	forvalues j = 1/`n_labels' {
		quietly putexcel `col_`j''1 = "`lbl`j''"
	}
	
	display "Exported `n_labels' labels to sheet `sheet'"
	
end


* Export goodness of fit statistics for probit/logit
capture program drop export_gof_probit
program define export_gof_probit
	
	syntax, row(integer) label(string)
	
	putexcel set "$dir_results/reg_socialcare", sheet("Gof") modify
	
	local row1 = `row'
	local row2 = `row' + 1
	local row3 = `row' + 2
	
	putexcel A`row1' = "`label'", bold
	putexcel A`row2' = "Pseudo R-squared"
	putexcel B`row2' = r2_p
	putexcel A`row3' = "N"
	putexcel B`row3' = N_sample
	putexcel E`row2' = "Chi^2"
	putexcel F`row2' = chi2
	putexcel E`row3' = "Log likelihood"
	putexcel F`row3' = ll
	
end


* Export goodness of fit statistics for OLS
capture program drop export_gof_ols
program define export_gof_ols
	
	syntax, row(integer) label(string)
	
	putexcel set "$dir_results/reg_socialcare", sheet("Gof") modify
	
	local row1 = `row'
	local row2 = `row' + 1
	local row3 = `row' + 2
	
	putexcel A`row1' = "`label'", bold
	putexcel A`row2' = "R-squared"
	putexcel B`row2' = r2
	putexcel A`row3' = "N"
	putexcel B`row3' = N_sample
	
end


* Save raw results to Excel and Word
capture program drop save_raw_results
program define save_raw_results
	
	syntax, process(string) title(string) [ifcond(string)]
	
	* Save to Excel
	matrix results = r(table)
	matrix results = results[1..6,1...]'
	
	putexcel set "$dir_raw_results/social_care/socialcare", ///
		sheet("Process `process'") replace
	putexcel A3 = matrix(results), names nformat(number_d2)
	putexcel J4 = matrix(e(V))
	
	* Save to Word (conditional on outreg2 being installed)
	capture which outreg2
	if _rc == 0 {
		if "`ifcond'" != "" {
			local note `"addnote("Note: Regression if condition = (`ifcond')")"'
		}
		
		* Check if probit/logit or OLS
		if "`e(cmd)'" == "probit" | "`e(cmd)'" == "logit" {
			outreg2 stats(coef se pval) using ///
				"$dir_raw_results/social_care/`process'.doc", replace ///
				title("`title'") ctitle(Model) label side dec(2) noparen ///
				addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll)) `note'
		}
		else {
			outreg2 stats(coef se pval) using ///
				"$dir_raw_results/social_care/`process'.doc", replace ///
				title("`title'") ctitle(Model) label side dec(2) noparen ///
				addstat(R2, e(r2)) `note'
		}
	}
	
end


* Main export routine: combines matrix operations, stability checks, and Excel export
capture program drop export_results_to_excel
program define export_results_to_excel
	
	syntax, sheet(string) [probit]
	
	* Store estimates
	matrix b = e(b)
	matrix V = e(V)
	
	* Trim zero coefficients
	mata: trim_matrices()
	
	* Check matrix stability
	check_matrix_stability
	
	* Export to Excel - use modify mode (file already created in setup)
	putexcel set "$dir_results/reg_socialcare", sheet("`sheet'") modify
	putexcel A1 = "REGRESSOR"
	putexcel B1 = "COEFFICIENT"
	
	* Write coefficients cell-by-cell
	mata: write_all_to_excel()
	
	* Extract and export labels
	mata: extract_and_export_labels("`sheet'")
	
	* Store model statistics
	if "`probit'" == "probit" {
		scalar r2_p = e(r2_p)
		scalar chi2 = e(chi2)
		scalar ll = e(ll)
	}
	else {
		scalar r2 = e(r2)
	}
	scalar N_sample = e(N)
	
end


* Complete workflow: save sample, export results, and clean up
capture program drop process_regression
program define process_regression
	
	syntax, process(string) sheet(string) title(string) gofrow(integer) ///
		goflabel(string) [ifcond(string) probit]
	
	* Save raw results
	save_raw_results, process("`process'") title("`title'") ifcond("`ifcond'")
	
	* Save sample for validation
	gen in_sample = e(sample)
	predict p
	save "$dir_data/`process'_sample", replace
	
	* Export results to Excel
	if "`probit'" == "probit" {
		export_results_to_excel, sheet("`sheet'") probit
		export_gof_probit, row(`gofrow') label("`goflabel'")
	}
	else {
		export_results_to_excel, sheet("`sheet'")
		export_gof_ols, row(`gofrow') label("`goflabel'")
	}
	
	* Clean up
	drop in_sample p
	scalar drop _all
	matrix drop _all
	
end

* Specialized workflow for multinomial logit models
capture program drop process_mlogit
program define process_mlogit
	
	syntax, process(string) sheet(string) title(string) gofrow(integer) ///
		goflabel(string) outcomes(integer) [ifcond(string)]
	
	* Save raw results (skip outreg2 for mlogit - it has issues)
	matrix results = r(table)
	matrix results = results[1..6,1...]'
	putexcel set "$dir_raw_results/social_care/socialcare", ///
		sheet("Process `process'") replace
	putexcel A3 = matrix(results), names nformat(number_d2)
	putexcel J4 = matrix(e(V))
	
	* Save sample for validation
	gen in_sample = e(sample)
	
	* Generate predictions (number depends on outcomes)
	if `outcomes' == 3 {
		predict p1 p2 p3
	}
	else if `outcomes' == 4 {
		predict p1 p2 p3 p4
	}
	else if `outcomes' == 5 {
		predict p1 p2 p3 p4 p5
	}
	
	save "$dir_data/`process'_sample", replace
	
	* Export results to Excel
	export_results_to_excel, sheet("`sheet'") probit
	export_gof_probit, row(`gofrow') label("`goflabel'")
	
	* Clean up
	drop in_sample p*
	scalar drop _all
	matrix drop _all
	
end


/*==============================================================================
	MAIN ANALYSIS
==============================================================================*/


/********************************* PREPARE DATA *******************************/

use "$dir_input_data/ukhls_pooled_ipop.dta", clear

* Time series structure
gsort idperson stm
xtset idperson stm

* Care received variables (Processes S2)

rename need_socare need_care

gen receive_formal_care = formal_socare_hrs > 0
gen receive_informal_care = (partner_socare_hrs + daughter_socare_hrs + son_socare_hrs + other_socare_hrs) > 0
gen receive_care = max(receive_informal_care, receive_formal_care)

gen CareMarket = .
replace CareMarket = 1 if (receive_informal_care == 0 & receive_formal_care == 0)
replace CareMarket = 2 if (receive_informal_care == 1 & receive_formal_care == 0)
replace CareMarket = 3 if (receive_informal_care == 1 & receive_formal_care == 1)
replace CareMarket = 4 if (receive_informal_care == 0 & receive_formal_care == 1)

lab def labCareMarket 1 "None" 2 "Informal" 3 "Mixed" 4 "Formal"
lab val CareMarket labCareMarket

gen HrsReceivedFormalIHS = asinh(formal_socare_hrs)
gen informal_socare_hrs = partner_socare_hrs + daughter_socare_hrs + son_socare_hrs + other_socare_hrs
gen HrsReceivedInformalIHS = asinh(informal_socare_hrs)


* Care provided variables (Processes S3)

gen HrsProvidedInformalIHS = asinh(careHoursProvidedWeekly)
gen provide_informal_care = (careWho >= 1)

* Age variables 
* - Categorical: 15-19, 20-24, ..., 80-84, 85+  
gen dage5 = 0
forval ii = 1/14 {
	replace dage5 = `ii' if (dag>=15+5*(`ii'-1) & dag<=19+5*(`ii'-1))
}
replace dage5 = 15 if (dag >= 85)
table dage5, stat(min dag) stat(max dag)

* - Categorical: <35, 35-44, 45-54, 55-64, 65+ 
gen dage10prime = 0
replace dage10prime = 1 if (dag>34 & dag<45)
replace dage10prime = 2 if (dag>44 & dag<55)
replace dage10prime = 3 if (dag>54 & dag<65)
replace dage10prime = 4 if (dag>64)
table dage10prime, stat(min dag) stat(max dag)

* - Categorical: 65-66, 67-68, 69-70, 71-72..., 85+
gen dage2old = 0
forval ii = 1/10 {
	replace dage2old = `ii' if (dag >= 65+2*(`ii'-1) & dag < 67+2*(`ii'-1))
}
replace dage2old = 11 if (dag >= 85)
table dage2old, stat(min dag) stat(max dag)

* Poor health flag
gen poor_health = (dhe == 1)

* Adjust for missing values
replace need_care = . if (need_care<0)
foreach var of varlist formal_socare_hrs partner_socare_hrs daughter_socare_hrs son_socare_hrs other_socare_hrs {
	replace `var' = 0 if (`var' < 0)
}

* Prepare vars for automatic labelling
xtset idperson stm
gen Dgn = dgn
gen Year = stm
gen YearSquared = stm^2
gen Age = dag
gen AgeSquared = dag^2

tab dage5, gen(Age_)
table dage5, stat(min dag) stat(max dag)	// RMK: AgeXX categories start at 1, hence shifted by 1
drop Age_1 Age_2
rename Age_3 Age20to24
rename Age_4 Age25to29
rename Age_5 Age30to34
rename Age_6 Age35to39
rename Age_7 Age40to44
rename Age_8 Age45to49
rename Age_9 Age50to54
rename Age_10 Age55to59
rename Age_11 Age60to64
rename Age_12 Age65to69
rename Age_13 Age70to74
rename Age_14 Age75to79
rename Age_15 Age80to84
rename Age_16 Age85plus

tab dage10prime, gen(Age_)
table dage10prime, stat(min dag) stat(max dag)	// RMK: AgeXX categories start at 1, hence shifted by 1
drop Age_1
rename Age_2 Age35to44
rename Age_3 Age45to54
rename Age_4 Age55to64
rename Age_5 Age65plus

tab dage2old, gen(Age_)
table dage2old, stat(min dag) stat(max dag)	// RMK: AgeXX categories start at 1, hence shifted by 1
drop Age_1	
rename Age_2 Age65to66
rename Age_3 Age67to68
rename Age_4 Age69to70
rename Age_5 Age71to72
rename Age_6 Age73to74
rename Age_7 Age75to76
rename Age_8 Age77to78
rename Age_9 Age79to80
rename Age_10 Age81to82
rename Age_11 Age83to84
drop Age_12

tab deh_c3, gen(Deh_c3_)
rename Deh_c3_1 Deh_c3_High
rename Deh_c3_2 Deh_c3_Medium
rename Deh_c3_3 Deh_c3_Low

tab deh_c4, gen(Deh_c4_)
rename Deh_c4_1 Deh_c4_Na
rename Deh_c4_2 Deh_c4_High
rename Deh_c4_3 Deh_c4_Medium
rename Deh_c4_4 Deh_c4_Low

tab dhe, gen(Dhe_)
rename Dhe_1 Dhe_Poor
rename Dhe_2 Dhe_Fair
rename Dhe_3 Dhe_Good
rename Dhe_4 Dhe_VeryGood
rename Dhe_5 Dhe_Excellent

tab dcpst, gen(Dcpst_)
rename Dcpst_1 Partnered
rename Dcpst_2 Single

tab drgn1, gen(UK)
rename UK1 UKC
rename UK2 UKD
rename UK3 UKE
rename UK4 UKF
rename UK5 UKG
rename UK6 UKH
rename UK7 UKI
rename UK8 UKJ
rename UK9 UKK
rename UK10 UKL
rename UK11 UKM
rename UK12 UKN

gen Y2020 = (stm == 2020)
gen Y2021 = (stm == 2021)
gen Y2022 = (stm == 2022)

gen NeedCare = need_care
gen ReceiveCare = receive_care
gen ProvideCare = provide_informal_care

tab CareMarket
gen CareMarketInformal = (CareMarket == 2)
gen CareMarketMixed = (CareMarket == 3)
gen CareMarketFormal = (CareMarket == 4)

tab ydses_c5, gen(HHincomeQ)

gen NeedCare_L1 = L.NeedCare
gen ReceiveCare_L1 = L.ReceiveCare
gen CareMarketFormal_L1 = L.CareMarketFormal
gen CareMarketInformal_L1 = L.CareMarketInformal
gen CareMarketMixed_L1 = L.CareMarketMixed
gen HrsReceivedFormalIHS_L1 = L.HrsReceivedFormalIHS
gen HrsReceivedInformalIHS_L1 = L.HrsReceivedInformalIHS
gen ProvideCare_L1 = L.ProvideCare
gen HrsProvidedInformalIHS_L1 = L.HrsProvidedInformalIHS

* Add partner's outcome variables
preserve
drop if idpartner == -9
keep idperson stm NeedCare ReceiveCare CareMarketFormal CareMarketInformal CareMarketMixed
rename idperson idpartner
rename NeedCare NeedCarePartner
rename ReceiveCare ReceiveCarePartner
rename CareMarketFormal CareMarketFormalPartner
rename CareMarketInformal CareMarketInformalPartner
rename CareMarketMixed CareMarketMixedPartner
save "$dir_work/partner.dta", replace
restore

merge m:1 idpartner stm using "$dir_work/partner.dta"
assert _merge == 1 if idpartner == -9
drop _merge

tab dhesp, gen(Dhesp_)
rename Dhesp_1 Dhesp_Poor
rename Dhesp_2 Dhesp_Fair
rename Dhesp_3 Dhesp_Good
rename Dhesp_4 Dhesp_VeryGood
rename Dhesp_5 Dhesp_Excellent

erase "$dir_work/partner.dta"

/*==============================================================================
	REGRESSIONS 
==============================================================================*/

* Stats for if conditions
table stm, stat (count NeedCare) stat (mean NeedCare)								// [2015, 2022]
table stm, stat (count ReceiveCare) stat (mean ReceiveCare)							// [2016, 2021] but with significant decrease in 2020 and 2021
table stm, stat (count receive_formal_care) stat (mean receive_formal_care)			// [2016, 2021] but with significant decrease in 2020 and 2021
table stm, stat (count receive_informal_care) stat (mean receive_informal_care)		// [2016, 2021] but with significant decrease in 2020 and 2021
table stm, stat (count provide_informal_care) stat (mean provide_informal_care)		// [2015, 2024] also 2014, but fewer hours


/* Age variables (for experimenting -> copy and paste in the specification)
	Dag Dagsq ///
	Age67to68 Age69to70 Age71to72 Age73to74 Age75to76 ///
	Age77to78 Age79to80 Age81to82 Age83to84 Age85plus ///
*/
	
/************************ Probit need care (S2a) ******************************/

probit NeedCare NeedCare_L1 Dgn ///
	Age67to68 Age69to70 Age71to72 Age73to74 Age75to76 ///
	Age77to78 Age79to80 Age81to82 Age83to84 Age85plus ///	
	Dhe_Fair Dhe_Good Dhe_VeryGood Dhe_Excellent ///
	Partnered ///
	Deh_c3_Medium Deh_c3_Low ///
	UKC UKD UKE UKF UKG UKH UKJ UKK UKL UKM UKN ///
	Y2020 Y2021 ///
	if ${s2a_if_condition} [pweight=dimxwt], vce(r)
	
process_regression, process("S2a") sheet("S2a") ///
	title("Process S2a: Prob. need care") ///
	gofrow(3) goflabel("S2a - Need care") ///
	ifcond("${s2a_if_condition}") probit


/************************ Probit receive care (S2b) ***************************/

probit ReceiveCare ReceiveCare_L1 Dgn ///
	Age67to68 Age69to70 Age71to72 Age73to74 Age75to76 ///
	Age77to78 Age79to80 Age81to82 Age83to84 Age85plus ///
	Dhe_Fair Dhe_Good Dhe_VeryGood Dhe_Excellent ///
	Partnered ///
	Deh_c3_Medium Deh_c3_Low ///
	UKC UKD UKE UKF UKG UKH UKJ UKK UKL UKM UKN ///
	HHincomeQ2 HHincomeQ3 HHincomeQ4 HHincomeQ5 ///
	Y2020 Y2021 ///
	if ${s2b_if_condition} [pweight=dimxwt], vce(r)

process_regression, process("S2b") sheet("S2b") ///
	title("Process S2b: Prob. receive care") ///
	gofrow(7) goflabel("S2b - Receive care") ///
	ifcond("${s2b_if_condition}") probit


	
/************************ Mlogit formal/informal (S2c) ************************/

/*  
	Informal is base outcome
	Mixed is 1st outcome
	Formal is 2nd outcomes
*/
	
mlogit CareMarket CareMarketFormal_L1 CareMarketInformal_L1 CareMarketMixed_L1 Dgn ///
	Age67to68 Age69to70 Age71to72 Age73to74 Age75to76 ///
	Age77to78 Age79to80 Age81to82 Age83to84 Age85plus ///
	Dhe_Fair Dhe_Good Dhe_VeryGood Dhe_Excellent ///
	Partnered ///
	Deh_c3_Medium Deh_c3_Low ///
	HHincomeQ2 HHincomeQ3 HHincomeQ4 HHincomeQ5 ///
	UKC UKD UKE UKF UKG UKH UKJ UKK UKL UKM UKN ///
	Y2020 Y2021 ///
	if ${s2c_if_condition} [pweight=dimxwt], vce(r) base(2)

process_mlogit, process("S2c") sheet("S2c") ///
	title("Process S2c: Formal/informal care") ///
	gofrow(11) goflabel("S2c - Formal/informal") ///
	outcomes(3) ifcond("${s2c_if_condition}")


	
/******************** OLS informal care hours received (S2d) ******************/

reg HrsReceivedInformalIHS HrsReceivedInformalIHS_L1 CareMarketMixed Dgn ///
	Age AgeSquared ///
	Dhe_Fair Dhe_Good Dhe_VeryGood Dhe_Excellent ///
	Partnered ///
	Deh_c3_Medium Deh_c3_Low ///
	HHincomeQ2 HHincomeQ3 HHincomeQ4 HHincomeQ5 ///
	UKC UKD UKE UKF UKG UKH UKJ UKK UKL UKM UKN ///
	Y2020 Y2021 ///
	if ${s2d_if_condition} [pweight=dimxwt], vce(r)

process_regression, process("S2d") sheet("S2d") ///
	title("Process S2d: Informal care hours received") ///
	gofrow(15) goflabel("S2d - Hours of informal care received") ///
	ifcond("${s2d_if_condition}")



/********************* OLS formal care hours received (S2e) *******************/

reg HrsReceivedFormalIHS HrsReceivedFormalIHS_L1 CareMarketMixed Dgn ///
	Dhe_Fair Dhe_Good Dhe_VeryGood Dhe_Excellent ///
	Partnered ///
	Deh_c3_Medium Deh_c3_Low ///
	HHincomeQ2 HHincomeQ3 HHincomeQ4 HHincomeQ5 ///
	UKC UKD UKE UKF UKG UKH UKJ UKK UKL UKM UKN ///
	Y2020 Y2021 ///
	if ${s2e_if_condition} [pweight=dimxwt], vce(r)

process_regression, process("S2e") sheet("S2e") ///
	title("Process S2e: Formal care hours received") ///
	gofrow(19) goflabel("S2e - Hours of formal care received") ///
	ifcond("${s2e_if_condition}")

	

/***************** Probit provide care, Singles (S3a) *************************/

probit ProvideCare ProvideCare_L1 NeedCare ReceiveCare Dgn ///
	Age30to34 Age35to39 Age40to44 Age45to49 Age50to54 ///
	Age55to59 Age60to64 Age65to69 Age70to74 Age75to79 Age80to84 Age85plus ///
	Dhe_Fair Dhe_Good Dhe_VeryGood Dhe_Excellent ///
	Deh_c3_Medium Deh_c3_Low ///
	HHincomeQ2 HHincomeQ3 HHincomeQ4 HHincomeQ5 ///
	UKC UKD UKE UKF UKG UKH UKJ UKK UKL UKM UKN ///
	Y2020 Y2021 ///
	if ${s3a_if_condition} [pweight=dimxwt], vce(r)
	
process_regression, process("S3a") sheet("S3a") ///
	title("Process S3a: Prob. provide care, Singles") ///
	gofrow(23) goflabel("S3a - Provide care, Singles") ///
	ifcond("${s3a_if_condition}") probit
	

/***************** Probit provide care, Partnered (S3b) ***********************/

capture drop in_sample p
probit ProvideCare ProvideCare_L1 NeedCare ReceiveCare Dgn ///
	ReceiveCarePartner CareMarketFormalPartner CareMarketInformalPartner CareMarketMixedPartner ///
	Dhe_Poor Dhe_Fair Dhe_Good Dhe_VeryGood ///
	Dhesp_Fair Dhesp_Good Dhesp_VeryGood Dhesp_Excellent ///
	Deh_c3_Medium Deh_c3_Low ///
	HHincomeQ2 HHincomeQ3 HHincomeQ4 HHincomeQ5 ///
	UKC UKD UKE UKF UKG UKH UKJ UKK UKL UKM UKN ///
	Y2020 Y2021 ///
	if ${s3b_if_condition} [pweight=dimxwt], vce(r)
	
process_regression, process("S3b") sheet("S3b") ///
	title("Process S3b: Prob. provide care, Partnered") ///
	gofrow(27) goflabel("S3b - Provide care, Partnered") ///
	ifcond("${s3b_if_condition}") probit
		
	
	
/******************* OLS care hours provided, Singles  (S3c) ******************/

reg HrsProvidedInformalIHS HrsProvidedInformalIHS_L1 Dgn ///
	Age20to24 Age25to29 Age30to34 Age35to39 Age40to44 Age45to49 Age50to54 ///
	Age55to59 Age60to64 Age65to69 Age70to74 Age75to79 Age80to84 Age85plus ///
	Dhe_Fair Dhe_Good Dhe_VeryGood Dhe_Excellent ///
	Deh_c3_Medium Deh_c3_Low ///
	HHincomeQ2 HHincomeQ3 HHincomeQ4 HHincomeQ5 ///
	UKC UKD UKE UKF UKG UKH UKJ UKK UKL UKM UKN ///
	Y2020 Y2021 ///
	if ${s3c_if_condition} [pweight=dimxwt], vce(r)

process_regression, process("S3c") sheet("S3c") ///
	title("Process S3c: Informal care hours provided, Singles") ///
	gofrow(31) goflabel("S3c - Hours of informal care provided, Singles") ///
	ifcond("${s3c_if_condition}")
	
	
/****************** OLS care hours provided, Partnered  (S3d) *****************/

reg HrsProvidedInformalIHS HrsProvidedInformalIHS_L1 Dgn ///
	Age20to24 Age25to29 Age30to34 Age35to39 Age40to44 Age45to49 Age50to54 ///
	Age55to59 Age60to64 Age65to69 Age70to74 Age75to79 Age80to84 Age85plus ///
	ReceiveCarePartner CareMarketFormalPartner CareMarketInformalPartner CareMarketMixedPartner ///
	Dhe_Poor Dhe_Fair Dhe_Good Dhe_VeryGood ///
	Dhesp_Fair Dhesp_Good Dhesp_VeryGood Dhesp_Excellent ///
	Deh_c3_Medium Deh_c3_Low ///
	HHincomeQ2 HHincomeQ3 HHincomeQ4 HHincomeQ5 ///
	UKC UKD UKE UKF UKG UKH UKJ UKK UKL UKM UKN ///
	Y2020 Y2021 ///
	if ${s3d_if_condition} [pweight=dimxwt], vce(r)

process_regression, process("S3d") sheet("S3d") ///
	title("Process S3d: Informal care hours provided, Partnered") ///
	gofrow(35) goflabel("S3c - Hours of informal care provided, Partnered") ///
	ifcond("${s3d_if_condition}")
	

display "Analysis complete!"
