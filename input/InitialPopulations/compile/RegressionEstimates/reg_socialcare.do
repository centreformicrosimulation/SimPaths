********************************************************************************
* PROJECT:  		SimPaths UK
* SECTION:			SOCIAL CARE RECEIPT
* AUTHORS:			Justin van de Ven, Matteo Richiardi, Daria Popova 
* LAST UPDATE:		19 Feb 2026 DP  
* COUNTRY: 			UK  
*
*   NOTES: 		
*	PROGRAM TO EVALUATE SOCIAL CARE RECEIPT FROM UKHLS DATA
*	ANALYSIS BASED ON THE SOCIAL CARE MODULE OF UKHLS
*	First version: Justin van de Ven, 28 Aug 2023
*	Refactored version: Matteo Richiardi, 16 Feb 2026
*   Integration into the pipeline: Daria Popova 18 Feb 2026 DP 
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
set more off
set mem 200m
set type double
//set maxvar 120000
set maxvar 30000

*******************************************************************
cap log close 
log using "${dir_log}/reg_socialcare.log", replace
*******************************************************************


/********************************* SET EXCEL FILE *****************************/

putexcel set "$dir_results/reg_socialcare", sheet("Info") replace
putexcel A1 = "Description:", bold
putexcel B1 = "Model parameters for social care module"
putexcel A2 = "Authors:", bold
putexcel B2 = "Justin van de Ven, Ashley Burdett, Matteo Richiardi, Daria Popova"
putexcel A3 = "Last edit:", bold
putexcel B3 = "16 Feb 2026 MR (Refactored)"
putexcel B3 = "18 Feb 2026 DP (Integrated into the pipeline)"

putexcel A5 = "Process:", bold
putexcel B5 = "Description:", bold

putexcel A6 = "S2a" B6 = "Prob. need care"
putexcel A7 = "S2b" B7 = "Prob. receive care"
putexcel A8 = "S2c" B8 = "Prob. receive Formal/informal care"
putexcel A9 = "S2d" B9 = "Informal care hours received"
putexcel A10 = "S2e" B10 = "Hours of formal care received"

putexcel A11 = "S3a" B11 = "Prob. provide care, Singles"
putexcel A12 = "S3b" B12 = "Prob. provide care, Partnered"
putexcel A13 = "S3c" B13 = "Hours of informal care provided, Singles"
putexcel A14 = "S3d" B14 = "Hours of informal care provided, Partnered"

putexcel A20 = "Notes:", bold
putexcel B20 = "Estimation sample: UK_ipop.dta with grossing up weight dwt" 
putexcel B21 = "Conditions for processes are defined as globals in master.do"

putexcel set "$dir_results/reg_socialcare", sheet("Gof") modify
putexcel A1 = "Goodness of fit", bold



/*==============================================================================
	MAIN ANALYSIS
==============================================================================*/

use ${estimation_sample}, clear

* Time series structure
gsort idperson stm
xtset idperson stm

* Adjust variables 
do "${dir_do}/variable_update.do"

* Run Stata programs to produce Excel file 
do "${dir_do}/programs.do" 

/*==============================================================================
	REGRESSIONS 
==============================================================================*/

* Stats for if conditions
/*
table stm, stat (count NeedCare) stat (mean NeedCare)								// [2015, 2022]
table stm, stat (count ReceiveCare) stat (mean ReceiveCare)							// [2016, 2021] but with significant decrease in 2020 and 2021
table stm, stat (count receive_formal_care) stat (mean receive_formal_care)			// [2016, 2021] but with significant decrease in 2020 and 2021
table stm, stat (count receive_informal_care) stat (mean receive_informal_care)		// [2016, 2021] but with significant decrease in 2020 and 2021
table stm, stat (count provide_informal_care) stat (mean provide_informal_care)		// [2015, 2024] also 2014, but fewer hours
*/
table stm, c(count NeedCare mean NeedCare)
table stm, c(count ReceiveCare mean ReceiveCare)
table stm, c(count receive_formal_care mean receive_formal_care)
table stm, c(count receive_informal_care mean receive_informal_care)
table stm, c(count provide_informal_care mean provide_informal_care)


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
	Deh_c4_Medium Deh_c4_Low ///
	Y2020 Y2021  ${regions} ${ethnicity} ///
	if ${s2a_if_condition} [pweight=${weight}], vce(r)
	
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
	Deh_c4_Medium Deh_c4_Low ///
	HHincomeQ2 HHincomeQ3 HHincomeQ4 HHincomeQ5 ///
	Y2020 Y2021 ${regions} ${ethnicity} ///
	if ${s2b_if_condition} [pweight=${weight}], vce(r)

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
	Deh_c4_Medium Deh_c4_Low ///
	HHincomeQ2 HHincomeQ3 HHincomeQ4 HHincomeQ5 ///	
	Y2020 Y2021 ${regions} ${ethnicity} ///
	if ${s2c_if_condition} [pweight=${weight}], vce(r) base(2)
/*
process_mlogit, process("S2c") sheet("S2c") ///
	title("Process S2c: Formal/informal care") ///
	gofrow(11) goflabel("S2c - Formal/informal") ///
	outcomes(3) ifcond("${s2c_if_condition}")
*/

/* DP: Use this routine as program for MLogit does not display labels corectly in Excel ==> to replace by program later on ? */  		
* Save raw results 
matrix results = r(table)
matrix results = results[1..6,1...]'

putexcel set "$dir_raw_results/social_care/socialcare", sheet("Process S2c") ///
	modify
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))


* Save sample inclusion indicator and predicted probabilities	
gen in_sample = e(sample)	
predict p1 p2 p3

* Save sample for estimates validation
save "$dir_validation_data/S2c_sample", replace

* Store model summary statistics	
scalar r2_p = e(r2_p) 
scalar N_sample = e(N)	 
scalar chi2 = e(chi2)
scalar ll = e(ll)	


* Store results in Excel 

* Store estimates in matrices
matrix b = e(b)	
matrix V = e(V)

* Raw output 
putexcel set "$dir_results/reg_socialcare", sheet("S2c_raw") modify
putexcel A1 = matrix(b'), names nformat(number_d2) 
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
scalar no_nonzero_b_per = no_nonzero_b / 2

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

* Generate vector to multiply the coef vector with to eliminate the repetitions
* of coefficients for vars that satify the proportional odds assumptions
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
putexcel set "$dir_results/reg_socialcare", sheet("S2c") modify
putexcel A1 = matrix(nonzero_b_structure'), names //nformat(number_d2) 


* Variance-covariance matrix 
* Eliminate zeros (baseline categories)
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
putexcel set "$dir_results/reg_socialcare", sheet("S2c") modify 
putexcel C2 = matrix(nonzero_var_structure)

*=======================================================================
* Eigenvalue stability check for trimmed variance-covariance matrix

matrix symeigen X lambda = nonzero_var_structure

* Largest eigenvalue
scalar max_eig = lambda[1,1]

* Ratio of smallest to largest eigenvalue
scalar min_ratio = lambda[1, colsof(lambda)] / max_eig

* Check 1: near-singularity
if max_eig < 1.0e-12 {
    display as error "CRITICAL ERROR: Variance-covariance matrix is near singular."
    display as error "Max eigenvalue = " max_eig
    exit 999
}

* Check 2: ill-conditioning
if min_ratio < 1.0e-12 {
    display as error "Matrix is ill-conditioned."
    display as error "Min/Max eigenvalue ratio = " min_ratio
    exit 506
}

display "VCV stability check passed."
display "Max eigenvalue: " max_eig
display "Min/Max ratio: " min_ratio
*=======================================================================			
		
* Labels
preserve

putexcel set "$dir_results/reg_socialcare", sheet("S2c") modify

putexcel A1 = "REGRESSOR"
putexcel B1 = "COEFFICIENT"


 * Use Mata to extract nice labels from colstripe of e(b) (replacement for Stata 14)
local dir_results "$dir_results"  
cap erase "$dir_results/temp_labels.txt"

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
    putexcel set "$dir_results/reg_socialcare", sheet("S2c") modify
	
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
    cap erase "$dir_results/temp_labels.txt"

restore		


* Goodness of fit

export_gof_probit, row(11) label("Process S2c: Formal/informal care")

* Clean up 		
drop in_sample p1 p2 p3
scalar drop _all
matrix drop _all


/******************** OLS informal care hours received (S2d) ******************/

reg HrsReceivedInformalIHS HrsReceivedInformalIHS_L1 CareMarketMixed Dgn ///
	Age AgeSquared ///
	Dhe_Fair Dhe_Good Dhe_VeryGood Dhe_Excellent ///
	Partnered ///
	Deh_c4_Medium Deh_c4_Low ///
	HHincomeQ2 HHincomeQ3 HHincomeQ4 HHincomeQ5 ///
	Y2020 Y2021 ${regions} /*${ethnicity} Ethn_White*/ ///
	if ${s2d_if_condition} [pweight=${weight}], vce(r)
	
process_regression, process("S2d") sheet("S2d") ///
	title("Process S2d: Informal care hours received") ///
	gofrow(15) goflabel("S2d - Hours of informal care received") ///
	ifcond("${s2d_if_condition}")


* Calculate RMSE
cap drop residuals squared_residuals  
predict residuals, residuals
gen squared_residuals = residuals^2

preserve
keep if ${s2d_if_condition}

sum squared_residuals [w=${weight}], meanonly
scalar rmse = sqrt(r(mean))
di "RMSE for Informal care hours received: " rmse

putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A9 = ("S2d") B9 = (rmse)

restore	

/********************* OLS formal care hours received (S2e) *******************/

reg HrsReceivedFormalIHS HrsReceivedFormalIHS_L1 CareMarketMixed Dgn ///
	Dhe_Fair Dhe_Good Dhe_VeryGood Dhe_Excellent ///
	Partnered ///
	Deh_c4_Medium Deh_c4_Low ///
	HHincomeQ2 HHincomeQ3 HHincomeQ4 HHincomeQ5 ///
	Y2020 Y2021 ${regions} ${ethnicity} ///
	if ${s2e_if_condition} [pweight=${weight}], vce(r)

process_regression, process("S2e") sheet("S2e") ///
	title("Process S2e: Formal care hours received") ///
	gofrow(19) goflabel("S2e - Hours of formal care received") ///
	ifcond("${s2e_if_condition}")

* Calculate RMSE
cap drop residuals squared_residuals  
predict residuals, residuals
gen squared_residuals = residuals^2

preserve
keep if ${s2e_if_condition}

sum squared_residuals [w=${weight}], meanonly
scalar rmse = sqrt(r(mean))
di "RMSE for Formal care hours received: " rmse

putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A10 = ("S2e") B10 = (rmse)

restore	


/***************** Probit provide care, Singles (S3a) *************************/

probit ProvideCare ProvideCare_L1 NeedCare ReceiveCare Dgn ///
	Age30to34 Age35to39 Age40to44 Age45to49 Age50to54 ///
	Age55to59 Age60to64 Age65to69 Age70to74 Age75to79 Age80to84 Age85plus ///
	Dhe_Fair Dhe_Good Dhe_VeryGood Dhe_Excellent ///
	Deh_c4_High Deh_c4_Medium Deh_c4_Low ///
	HHincomeQ2 HHincomeQ3 HHincomeQ4 HHincomeQ5 ///
	Y2020 Y2021 ${regions} ${ethnicity} ///
	if ${s3a_if_condition} [pweight=${weight}], vce(r)
	
process_regression, process("S3a") sheet("S3a") ///
	title("Process S3a: Prob. provide care, Singles") ///
	gofrow(23) goflabel("S3a - Provide care, Singles") ///
	ifcond("${s3a_if_condition}") probit
	

/***************** Probit provide care, Partnered (S3b) ***********************/
/*
tab CareMarket ProvideCare if ${s3b_if_condition}
tab deh_c4 ProvideCare if ${s3b_if_condition}
deh_c4 =0 is excluded because there's just 1 obs providing care and probit would not converge 
*/

capture drop in_sample p
probit ProvideCare ProvideCare_L1 NeedCare ReceiveCare Dgn ///
	ReceiveCarePartner CareMarketFormalPartner CareMarketInformalPartner CareMarketMixedPartner ///
	Dhe_Poor Dhe_Fair Dhe_Good Dhe_VeryGood ///
	Dhesp_Fair Dhesp_Good Dhesp_VeryGood Dhesp_Excellent ///
	Deh_c4_High Deh_c4_Medium  ///
	HHincomeQ2 HHincomeQ3 HHincomeQ4 HHincomeQ5 ///
	Y2020 Y2021 ${regions} ${ethnicity} ///
	if ${s3b_if_condition} [pweight=${weight}], vce(r)
	
process_regression, process("S3b") sheet("S3b") ///
	title("Process S3b: Prob. provide care, Partnered") ///
	gofrow(27) goflabel("S3b - Provide care, Partnered") ///
	ifcond("${s3b_if_condition}") probit
		
	
	
/******************* OLS care hours provided, Singles  (S3c) ******************/

reg HrsProvidedInformalIHS HrsProvidedInformalIHS_L1 Dgn ///
	Age20to24 Age25to29 Age30to34 Age35to39 Age40to44 Age45to49 Age50to54 ///
	Age55to59 Age60to64 Age65to69 Age70to74 Age75to79 Age80to84 Age85plus ///
	Dhe_Fair Dhe_Good Dhe_VeryGood Dhe_Excellent ///
	Deh_c4_High Deh_c4_Medium Deh_c4_Low ///
	HHincomeQ2 HHincomeQ3 HHincomeQ4 HHincomeQ5 ///
	Y2020 Y2021 ${regions} ${ethnicity} ///
	if ${s3c_if_condition} [pweight=${weight}], vce(r)

process_regression, process("S3c") sheet("S3c") ///
	title("Process S3c: Informal care hours provided, Singles") ///
	gofrow(31) goflabel("S3c - Hours of informal care provided, Singles") ///
	ifcond("${s3c_if_condition}")
	
	* Calculate RMSE
cap drop residuals squared_residuals  
predict residuals, residuals
gen squared_residuals = residuals^2

preserve
keep if ${s3c_if_condition}

sum squared_residuals [w=${weight}], meanonly
scalar rmse = sqrt(r(mean))
di "RMSE for Informal care hours provided, Singles: " rmse

putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A11 = ("S3c") B11 = (rmse)

restore	

		
	
/****************** OLS care hours provided, Partnered  (S3d) *****************/

reg HrsProvidedInformalIHS HrsProvidedInformalIHS_L1 Dgn ///
	Age20to24 Age25to29 Age30to34 Age35to39 Age40to44 Age45to49 Age50to54 ///
	Age55to59 Age60to64 Age65to69 Age70to74 Age75to79 Age80to84 Age85plus ///
	ReceiveCarePartner CareMarketFormalPartner CareMarketInformalPartner CareMarketMixedPartner ///
	Dhe_Poor Dhe_Fair Dhe_Good Dhe_VeryGood ///
	Dhesp_Fair Dhesp_Good Dhesp_VeryGood Dhesp_Excellent ///
	Deh_c4_High Deh_c4_Medium Deh_c4_Low ///
	HHincomeQ2 HHincomeQ3 HHincomeQ4 HHincomeQ5 ///
	Y2020 Y2021 ${regions} ${ethnicity} ///
	if ${s3d_if_condition} [pweight=${weight}], vce(r)

process_regression, process("S3d") sheet("S3d") ///
	title("Process S3d: Informal care hours provided, Partnered") ///
	gofrow(35) goflabel("S3d - Hours of informal care provided, Partnered") ///
	ifcond("${s3d_if_condition}")
	
	* Calculate RMSE
cap drop residuals squared_residuals  
predict residuals, residuals
gen squared_residuals = residuals^2

preserve
keep if ${s3d_if_condition}

sum squared_residuals [w=${weight}], meanonly
scalar rmse = sqrt(r(mean))
di "RMSE for Informal care hours provided, Partnered: " rmse

putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A12 = ("S3d") B12 = (rmse)

restore		
	
	

display "Analysis complete!"
