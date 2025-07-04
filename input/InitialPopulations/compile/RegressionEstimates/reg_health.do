********************************************************************************
* PROJECT:  		ESPON 
* SECTION:			Health
* OBJECT: 			Health status and Disability
* AUTHORS:			Daria Popova, Justin van de Ven
* LAST UPDATE:		1 July 2025 DP  
* COUNTRY: 			UK 
*
* NOTES:			     
********************************************************************************
clear all
set more off
set mem 200m
set type double
//set maxvar 120000
set maxvar 30000


/*******************************************************************************
*	DEFINE DIRECTORIES
*******************************************************************************/
* Working directory
//global dir_work "C:\MyFiles\99 DEV ENV\JAS-MINE\data work\regression_estimates"
global dir_work "D:\Dasha\ESSEX\ESPON 2024\UK\regression_estimates"

* Directory which contains do files
global dir_do "${dir_work}/do"

* Directory which contains data files 
global dir_data "${dir_work}/data"

* Directory which contains log files 
global dir_log "${dir_work}/log"

* Directory which contains pooled UKHLS dataset 
//global dir_ukhls_data "C:\MyFiles\99 DEV ENV\JAS-MINE\data work\initial_populations\data"
global dir_ukhls_data "D:\Dasha\ESSEX\ESPON 2024\UK\initial_populations\data"

*******************************************************************
cap log close 
log using "${dir_log}/reg_health.log", replace
*******************************************************************
use "$dir_ukhls_data/ukhls_pooled_all_obs_09.dta", clear

do "$dir_do/variable_update"


* Sample selection 
drop if dag < 16


* Set Excel file 

* Info sheet

putexcel set "$dir_work/reg_health", sheet("Info") replace
putexcel A1 = "Description:"
putexcel B1 = "Model parameters governing projection self-reported health status"
putexcel A2 = "Authors:	Patryk Bronka, Justin van de Ven, Daria Popova" 
putexcel A3 = "Last edit: 1 July 2025 DP"

putexcel A4 = "Process:", bold
putexcel B4 = "Description:", bold

putexcel A5 = "H1a"
putexcel B5 = "Generalized ordered logit regression estimates of self reported health status - individuals aged 16-29 in initial education spell"
putexcel B6 = "Covariates that satisfy the parallel lines assumption have one estimate for all categories of the dependent variable and are present once in the table"
putexcel B7 = "Covariates that do not satisfy the parallel lines assumption have an estimate for each estimated category of the dependent variable. These covariates have the dependent variable category appended to their name."

putexcel A8 = "H1b"
putexcel B8 = "Generalized ordered logit regression estimates of self reported health status - individuals aged 16+ not in initial education spell"
putexcel B9 = "Covariates that satisfy the parallel lines assumption have one estimate for all categories of the dependent variable and are present once in the table"
putexcel B10 = "Covariates that do not satisfy the parallel lines assumption have an estimate for each estimated category of the dependent variable. These covariates have the dependent variable category appended to their name."

putexcel A11 = "H2b"
putexcel B11 = "Probit regression estimates of the probability of being long-term sick or disabled - people aged 16+ not in initial education spell"

putexcel A12 = "H1a_raw"
putexcel B12 = "Raw generalized ordered logit regression estimates of self reported health status - individuals aged 16-29 in initial education spell. Useful for the 'Gologit predictor' file."
putexcel A13 = "H1b_raw"
putexcel B13 = "Raw generalized ordered logit regression estimates of self reported health status - individuals aged 16+ not in initial education spell. Useful for the 'Gologit predictor' file."

putexcel A15 = "Notes:", bold
putexcel B15 = "All processes: replaced lagged dhe with lagged dhe_pcs and dhe_mcs, added ethnicity-4 cat (dot), covid dummies (y2020 y2021)"
putexcel B16 = "H1a and H1b: excluded those with imputed values of dhe"
putexcel B17 = "H1a: some covariates had to be dropped to obtain estimates; lagged income quintile is treated as continuous variable"
putexcel B18 = "H2b: used wider definition of disability (Dlltsd01), incl those declaring themselves as disabled or receiving disability benefits"

putexcel set "$dir_work/reg_health", sheet("Gof") modify
putexcel A1 = "Goodness of fit", bold	


xtset idperson swv

********************************************
* H1a: Health status, in initial edu spell *
********************************************

* Process H1a: Probability of each self-rated health status for those who 
* 				are in their initial education spell 
* Sample: 16-29 year olds who are in their initial education spell 
* DV: Categorical health status (5)	

fre dhe if (dag>=16 & dag<=29 & ded==1 )

/* Ordered probit models to replace linear regression 
oprobit dhe i.dgn dag dagsq li.ydses_c5 ilb5.dhe ib8.drgn1 stm if (dag>=16 & dag<=29 & ded==1) [pweight=disclwt], vce(robust)
*/

* Generalized ordered logit			
gologit2 dhe i.Dgn Dag Dag_sq L_Ydses_c5 L_Dhe_pcs L_Dhe_mcs i.UKC i.UKD i.UKE i.UKF i.UKG i.UKH i.UKJ i.UKK i.UKL i.UKM i.UKN Year_transformed Y2020 Y2021 i.Ethn_Asian i.Ethn_Black i.Ethn_Other ///
    if dag >= 16 & dag <= 29 & ded == 1 & dhe_flag != 1 ///
	[pweight = dimxwt], autofit
*Note: In gologit2, the coefficients show how covariates affect the log-odds of being above a certain category vs. at or below it.


	*raw results 
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_raw_results/health/health", sheet("Process H1a") replace
putexcel A3 = matrix(results), names //nformat(number_d2) 
putexcel J4 = matrix(e(V))
outreg2 stats(coef se pval) using "$dir_raw_results/health/H1a.doc", replace ///
title("Process H1a: Generalised ordered logit regression estimates of self reported health status - individuals aged 16-29 in continuous education") ///
 ctitle(health status) label side dec(2) noparen addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll))
	
* Save sample inclusion indicator and predicted probabilities		
gen in_sample = e(sample)
predict p1 p2 p3 p4 p5
	
* Save sample for later use (internal validation)	
save "$dir_validation_data/H1a_sample", replace

* Store model summary statistics
scalar r2_p = e(r2_p) 
scalar N_sample = e(N)	 
	
* Store results in Excel 

* Store estimates in matrices
matrix b = e(b)	
matrix V = e(V)

* Raw output 
putexcel set "$dir_results/reg_health", sheet("H1a_raw") modify
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
putexcel set "$dir_results/reg_health", sheet("H1a") modify
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
putexcel set "$dir_results/reg_health", sheet("H1a") modify
putexcel C2 = matrix(nonzero_var_structure)
		
			
* Labels
putexcel set "$dir_results/reg_health", sheet("H1a") modify

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
    putexcel set "$dir_results/reg_health", sheet("H1a") modify
	
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


* Export model fit statistics
putexcel set "$dir_results/reg_health", sheet("Gof") modify

putexcel A3 = "H1a - Health status, in initial education spell", bold		

putexcel A5 = "Pseudo R-squared" 
putexcel B5 = r2_p 
putexcel A6 = "N"
putexcel B6 = N_sample

restore	
* Clean up 		
drop in_sample p1 p2 p3 p4 p5 
scalar drop _all
matrix drop _all
//frame drop temp_frame 	


******************************************************
* Process H1b: Health status, left intital edu spell *
******************************************************

* Process H1b: Probability of each self-rated health status for those who 
* 				have left their initial education spell 
* Sample: 16 or older who have left their initial education spell 
* DV: Categorical health status (5)

/* Ordered probit models to replace linear regression 
oprobit dhe i.dgn dag dagsq ib1.deh_c3 li.les_c3 li.ydses_c5 ilb5.dhe lib1.dhhtp_c4 ib8.drgn1 stm if (dag>=16 & ded==0)  [pweight=disclwt], vce(robust)
*/

 * Generalized ordered logit	
sort idperson swv

gologit2 dhe i.Dgn Dag Dag_sq ///
i.Deh_c3_Medium i.Deh_c3_Low ///
	i.L_Les_c3_Student i.L_Les_c3_NotEmployed ///
	/*L_Ydses_c5*/ i.L_Ydses_c5_Q2 i.L_Ydses_c5_Q3 i.L_Ydses_c5_Q4 i.L_Ydses_c5_Q5 ///
	L_Dhe_pcs L_Dhe_mcs  ///
	i.L_Dhhtp_c4_CoupleChildren i.L_Dhhtp_c4_SingleNoChildren i.L_Dhhtp_c4_SingleChildren ///
	i.UKC i.UKD i.UKE i.UKF i.UKG i.UKH i.UKJ i.UKK i.UKL i.UKM i.UKN ///
	Year_transformed Y2020 Y2021 ///
	i.Ethn_Asian i.Ethn_Black i.Ethn_Other ///
	if dhe_flag != 1 & ///
	dag >= 16 & ded == 0 [pweight = dimxwt], autofit
*Note: In gologit2, the coefficients show how covariates affect the log-odds of being above a certain category vs. at or below it.


* raw results 
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_raw_results/health/health", sheet("Process H1b") modify
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))
outreg2 stats(coef se pval) using "$dir_raw_results/health/H1b.doc", replace ///
title("Process H1b: Generalised Ordered logit regression estimates of self reported health status - individuals aged 16+ not in continuous education") ///
 ctitle(health status) label side dec(2) noparen addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll))
	
* Save sample inclusion indicator and predicted probabilities		
gen in_sample = e(sample)
predict p1 p2 p3 p4 p5
	
* Save sample for later use (internal validation)	
save "$dir_validation_data/H1b_sample", replace

* Store model summary statistics
scalar r2_p = e(r2_p) 
scalar N_sample = e(N)	 
	
* Store results in Excel 

* Store estimates in matrices
matrix b = e(b)	
matrix V = e(V)

* Raw output 
putexcel set "$dir_results/reg_health", sheet("H1b_raw") modify
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
putexcel set "$dir_results/reg_health", sheet("H1b") modify
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
putexcel set "$dir_results/reg_health", sheet("H1b") modify
putexcel C2 = matrix(nonzero_var_structure)
		
			
* Labels
putexcel set "$dir_results/reg_health", sheet("H1b") modify

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
    putexcel set "$dir_results/reg_health", sheet("H1b") modify
	
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


	* Export model fit statistics
putexcel set "$dir_results/reg_health", sheet("Gof") modify

putexcel A9 = "H1b - Health status, left initial education spell", bold		

putexcel A11 = "Pseudo R-squared" 
putexcel B11 = r2_p 
putexcel A12 = "N"
putexcel B12 = N_sample

restore		
* Clean up 		
drop in_sample p1 p2 p3 p4 p5 
scalar drop _all
matrix drop _all
//frame drop temp_frame 	

 

***********************************************************
* H2b: Long-term sick or disabled, left initial edu spell *
***********************************************************

* Process H2b: Probability of being long-term sick or disabled for those 
* 				not in continuous education.
* Sample: 16 or older who have left their initial education spell 
* DV: Long term sick/disabled dummy ==> plus those on disability benefits 
tab2 dlltsd dlltsd01

fre dlltsd if (dag >= 16 & ded == 0)
fre dlltsd01 if (dag >= 16 & ded == 0)
fre les* if dlltsd01==1 
/*fre les* if dlltsd01==1
les_c4 -- LABOUR MARKET: Activity status
---------------------------------------------------------------------------------
                                    |      Freq.    Percent      Valid       Cum.
------------------------------------+--------------------------------------------
Valid   1 Employed or self-employed |       5549      11.46      11.47      11.47
        2 Student                   |        646       1.33       1.34      12.81
        3 Not employed              |      24806      51.25      51.28      64.09
        4 Retired                   |      17368      35.88      35.91     100.00
        Total                       |      48369      99.93     100.00           
Missing .                           |         32       0.07                      
Total                               |      48401     100.00                      
---------------------------------------------------------------------------------
*/

/*probit dlltsd01 i.dgn dag dagsq ib1.deh_c3 li.ydses_c5 ib5.dhe ilb5.dhe l.dlltsd lib1.dhhtp_c4 ib8.drgn1 stm if (dag>=16 & ded==0) [pweight=disclwt], vce(robust) */

probit dlltsd01 i.Dgn Dag Dag_sq ///
       i.Deh_c3_Medium i.Deh_c3_Low ///
	   li.Ydses_c5_Q2 li.Ydses_c5_Q3 li.Ydses_c5_Q4 li.Ydses_c5_Q5 ///
	   Dhe_pcs Dhe_mcs ///
	   L_Dhe_pcs L_Dhe_mcs ///
	   l.Dlltsd01 ///
	   li.Dhhtp_c4_CoupleChildren li.Dhhtp_c4_SingleNoChildren li.Dhhtp_c4_SingleChildren /// 
	   i.UKC i.UKD i.UKE i.UKF i.UKG i.UKH i.UKJ i.UKK i.UKL i.UKM i.UKN ///
	   Year_transformed Y2020 Y2021 ///
	   i.Ethn_Asian i.Ethn_Black i.Ethn_Other ///
if (dag >= 16 & ded == 0) ///
  [pweight = dimxwt], vce(robust)


  
	* raw results 
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_raw_results/health/health", sheet("Process H2b") modify
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))
outreg2 stats(coef se pval) using "$dir_raw_results/health/H2b.doc", replace ///
title("Process H2b: Probit regression estimates for being long-term sick or disabled - people aged 16+ not in continuous education") ///
 ctitle(long-term sick or disabled) label side dec(2) noparen addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll))

gen in_sample = e(sample)	

predict p 
 
* Save sample for later use (internal validation)
save "$dir_validation_data/H2b_sample", replace

* Store model summary statistics	
scalar r2_p = e(r2_p) 
scalar N_sample = e(N)	
scalar chi2 = e(chi2)
scalar ll = e(ll)	

* Store results in Excel 

* Store estimates in matrices
matrix b = e(b)	
matrix V = e(V)

* Eliminate rows and columns containing zeros (baseline cats) 
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
putexcel set "$dir_results/reg_health", sheet("H2b") modify
putexcel B2 = matrix(b_trimmed)
putexcel C2 = matrix(V_trimmed)


* Labels 
putexcel set "$dir_results/reg_health", sheet("H2b") modify

putexcel A1 = "REGRESSOR"
putexcel B1 = "COEFFICIENT"


/* Use frame and Mata to extract nice labels from colstripe of e(b) ==> not working in stata 14
frame create temp_frame
frame temp_frame: {

    mata: 
		// Import matrices from Stata
		nonzero_b_flag = st_matrix("nonzero_b_flag")'
		stripe = st_matrixcolstripe("e(b)")
		
		// Extract and variable and category names
		varnames = stripe[.,2]
		varnames_no_bl = select(varnames, nonzero_b_flag :== 1)
		
		// Create label vector
		labels_no_bl = usubinstr(varnames_no_bl, "1.", "", 1)
		labels_no_bl = regexr(labels_no_bl, "^_cons", "Constant")
		labels_no_bl = regexm(labels_no_bl, "^L\.") :* (regexr(labels_no_bl, "^L\.", "") :+ "_L1") :+ (!regexm(labels_no_bl, "^L\.") :* labels_no_bl)
		labels_no_bl = regexm(labels_no_bl, "^1L.") :* (regexr(labels_no_bl, "^1L.", "") :+ "_L1") :+ (!regexm(labels_no_bl, "1L.") :* labels_no_bl)
		labels_no_bl = regexr(labels_no_bl, "_Dgn_L1$", "_Dgn")
		
		labels_no_bl
		
		nonzero_labels_structure = "v1"\labels_no_bl
		
		// Create temp file 
		fh = fopen("$dir_results/temp_labels.txt", "w")
		for (i=1; i<=rows(nonzero_labels_structure); i++) {
			fput(fh, nonzero_labels_structure[i])
		}
		fclose(fh)
    end
*/
* STATA 14-COMPATIBLE LABEL EXTRACTION AND FILE EXPORT 
* Mata: extract and clean labels
mata: 
    // Import matrices
    nonzero_b_flag = st_matrix("nonzero_b_flag")'
    stripe = st_matrixcolstripe("e(b)")

    // Extract varnames from stripe (2nd column)
    varnames = stripe[.,2]
    varnames_no_bl = select(varnames, nonzero_b_flag :== 1)

    // Clean label vector
    labels_no_bl = usubinstr(varnames_no_bl, "1.", "", 1)
    labels_no_bl = regexr(labels_no_bl, "^_cons", "Constant")
    labels_no_bl = regexm(labels_no_bl, "^L\\.") :* (regexr(labels_no_bl, "^L\\.", "") :+ "_L1") :+ ///
                   (!regexm(labels_no_bl, "^L\\.") :* labels_no_bl)
    labels_no_bl = regexm(labels_no_bl, "^1L\\.") :* (regexr(labels_no_bl, "^1L\\.", "") :+ "_L1") :+ ///
                   (!regexm(labels_no_bl, "^1L\\.") :* labels_no_bl)
    labels_no_bl = regexr(labels_no_bl, "_Dgn_L1$", "_Dgn")

    // Save as macro for writing labels from Stata
    st_local("nice_labels", invtokens(labels_no_bl'))
end

* Save cleaned labels into your original file 
capture file close labelout
file open labelout using "$dir_results/temp_labels.txt", write replace
file write labelout "v1" _n  // header for import
foreach lbl in `nice_labels' {
    file write labelout "`lbl'" _n
}
file close labelout

* Import cleaned labels from your file
import delimited "$dir_results/temp_labels.txt", clear varnames(1) encoding(utf8)
gen n = _n

* Export to Excel (vertical layout in column A)
putexcel set "$dir_results/reg_health", sheet("H2b") modify
summarize n, meanonly
local N = r(max) + 1
forvalue i = 2/`N' {
    local j = `i' - 1
    putexcel A`i' = v1[`j']
}

* Export to Excel (horizontal layout in row 1, starting at column C)
forvalues j = 1/`N' {
    local n = `j' + 2  // shift index: col C = 3
    local col ""
    local nn = `n'
    while `nn' > 0 {
        local rem = mod(`nn' - 1, 26)
        local col = char(65 + `rem') + "`col'"
        local nn = floor((`nn' - 1)/26)
    }
    putexcel `col'1 = v1[`j']
}

* Clean up original file
erase "$dir_results/temp_labels.txt"


* Export model fit statistics	
putexcel set "$dir_results/reg_health", sheet("Gof") modify

putexcel A15 = "H2b -  Long-term sick/disabled or on disability benefits, left initial edu spell", bold		
putexcel A17 = "Pseudo R-squared" 
putexcel B17 = r2_p 
putexcel A18 = "N"
putexcel B18 = N_sample  
putexcel E17 = "Chi^2"		
putexcel F17 = chi2
putexcel E18 = "Log likelihood"		
putexcel F18 = ll		
	
* Clean up 		
//drop in_sample p
scalar drop _all
matrix drop _all
//frame drop temp_frame 	

capture log close 

cap erase "$dir_results/temp.dta"
	
