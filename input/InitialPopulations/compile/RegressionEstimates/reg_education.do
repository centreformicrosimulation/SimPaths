******************************************************************************************
* PROJECT:  		SimPaths UK
* SECTION:			Education
* OBJECT: 			Final Probit & Generalised Logit Models - Weighted
* AUTHORS:			Patryk Bronka, Daria Popova, Justin van de Ven, Aleksandra Kolndrekaj
* LAST UPDATE:		18 Feb 2026 AK  
* COUNTRY: 			UK  
* 
* NOTES: 	                   
*                    
*****************************************************************************************		

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

* Set Excel file 

* Info sheet
putexcel set "$dir_results/reg_education_UK", sheet("Info") replace
putexcel A1 = "Description:"
putexcel B1 = "Model parameters governing projection of education status"
putexcel A2 = "Authors:	Patryk Bronka, Justin van de Ven, Daria Popova, Aleksandra Kolndrekaj" 
putexcel A3 = "Last edit: 18 Feb 2026 AK"

putexcel A4 = "Process:", bold
putexcel B4 = "Description:", bold

putexcel A5 = "E1a"
putexcel B5 = "Probit regression estimates of exiting education"

putexcel A6 = "E1b"
putexcel B6 = "Probit regression estimates of returning to education"

putexcel A7 = "E2"
putexcel B7 = "Generalized ordered logit regression estimates of education attainment - individuals aged 16+ exiting education."

putexcel A8 = "E2_raw"
putexcel B8 = "Raw generalized ordered logit regression estimates of education attainment - individuals aged 16+ exiting education. Useful for the 'Gologit predictor' file."

putexcel A10 = "Notes:", bold
putexcel B10 = "Estimation sample: UK_ipop.dta with grossing up weight dwt" 
putexcel B11 = "Conditions for processes are defined as globals in master.do"
//putexcel B12 = "E1a: Compared to the previous version, where age and age squared were used, age is now centered (at age 23) and its effect is allowed to change after age 18." 

putexcel set "$dir_results/reg_education_UK", sheet("Gof") modify
putexcel A1 = "Goodness of fit", bold	



/********************************* PREPARE DATA *******************************/

use "${estimation_sample}", clear

* Set data 
xtset idperson swv
sort idperson swv 

* Remove children 
drop if dag < 16

* Adjust variables 
do "${dir_do}/variable_update.do"


/********************************** ESTIMATION ********************************/

/****************** E1a: PROBABILITY OF REMAINING IN EDUCATION ****************/
display "${e1a_if_condition}"	

probit Dst i.Dgn Dag Dag_sq /*Dag_c Dag_c_sq Dag_post18_sq*/  li.Ded  ///
	li.Dehmf_c3_Medium li.Dehmf_c3_Low ///
	li.Ydses_c5_Q2 li.Ydses_c5_Q3 li.Ydses_c5_Q4 li.Ydses_c5_Q5 ///
	$regions Year_transformed Y2020 Y2021 $ethnicity ///
	if ${e1a_if_condition} ///
	[pw=dwt], vce(robust)	
	

* Save raw results 
matrix results = r(table)
matrix results = results[1..6,1...]'

putexcel set "$dir_raw_results/education/education", sheet("Process E1a") ///
	replace
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))

outreg2 stats(coef se pval) using "$dir_raw_results/education/E1a.doc", ///
	replace ///
title("Process E1a: Probability Remaining In Education") ///
	ctitle(Continuing student) label side dec(2) noparen ///
	addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll)) ///
	addnote(`"Note: Regression if condition = (${e1a_if_condition})"')
	
* Save sample inclusion indicator and predicted probabilities	
gen in_sample = e(sample)	
predict p

* Save sample estimate validation 
save "$dir_validation_data/E1a_sample", replace

* Store model summary statistics
scalar r2_p = e(r2_p) 
scalar N_sample = e(N)	
scalar chi2 = e(chi2)
scalar ll = e(ll)	


* Save estimates for use in SimPaths 

* Store estimates
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

* Eigenvalue tests for var-cov invertablility in SimPaths
matrix symeigen X lambda = V_trimmed

scalar max_eig = lambda[1,1]

scalar min_ratio = lambda[1, colsof(lambda)] / max_eig

* Outcome of max eigenvalue test 
if max_eig < 1.0e-12 {
	
    display as error "CRITICAL ERROR: Maximum eigenvalue is too small (`max_eig')."
    display as error "The Variance-Covariance matrix is likely singular."
    exit 999

}

display "Stability Check Passed: Max Eigenvalue is " max_eig

* Outcome of eigenvalue ratio test 
if min_ratio < 1.0e-12 {
	
    display as error "Matrix is ill-conditioned. Min/Max ratio: " min_ratio
    exit 506

}

display "Stability Check Passed. Min/Max ratio: " min_ratio

* Export into Excel 
putexcel set "$dir_results/reg_education_UK", sheet("E1a") modify
putexcel B2 = matrix(b_trimmed)
putexcel C2 = matrix(V_trimmed)


* Labels 
preserve
putexcel set "$dir_results/reg_education_UK", sheet("E1a") modify 	

putexcel A1 = "REGRESSOR"
putexcel B1 = "COEFFICIENT"


* Use Mata to extract nice labels from colstripe of e(b)

local dir_results "$dir_results"
cap erase "$dir_results/temp_labels.txt"

mata:
    // --------------------------------------------------
    // Import objects from Stata
    // --------------------------------------------------
    nonzero_b_flag = st_matrix("nonzero_b_flag")
    stripe         = st_matrixcolstripe("e(b)")

    // Ensure column vector
    nonzero_b_flag = nonzero_b_flag'

    // --------------------------------------------------
    // Extract variable names
    // --------------------------------------------------
    varnames = stripe[.,2]

    // Keep non-baseline coefficients
    varnames_no_bl = select(varnames, nonzero_b_flag :== 1)

    // --------------------------------------------------
    // Clean labels
    // --------------------------------------------------
    labels_no_bl = usubinstr(varnames_no_bl, "1.", "", 1)
    labels_no_bl = regexr(labels_no_bl, "^_cons", "Constant")

    // Handle lags: L.var -> var_L1
    labels_no_bl = ///
        regexm(labels_no_bl, "^L\.") :* ///
        (regexr(labels_no_bl, "^L\.", "") :+ "_L1") :+ ///
        (!regexm(labels_no_bl, "^L\.") :* labels_no_bl)

    // Handle 1L.var
    labels_no_bl = ///
        regexm(labels_no_bl, "^1L\.") :* ///
        (regexr(labels_no_bl, "^1L\.", "") :+ "_L1") :+ ///
        (!regexm(labels_no_bl, "^1L\.") :* labels_no_bl)

    // --------------------------------------------------
    // Add header
    // --------------------------------------------------
    labels_out = "v1" \ labels_no_bl

    // --------------------------------------------------
    // Write to temp file
    // --------------------------------------------------
    outfile = st_local("dir_results") + "/temp_labels.txt"
    fh = fopen(outfile, "w")
    for (i=1; i<=rows(labels_out); i++) {
        fput(fh, labels_out[i])
    }
    fclose(fh)
end


    * Import cleaned labels into Stata
    import delimited "$dir_results/temp_labels.txt", clear varnames(1) ///
		encoding(utf8)
	gen n = _n
    
    * Export labels to Excel
    putexcel set "$dir_results/reg_education_UK", sheet("E1a") modify 	
	
	* Vertical labels
    summarize n, meanonly
	local N = r(max)+1
	forvalue i = 2/`N' {
	
		local j = `i' - 1
		putexcel A`i' = v1[`j'] 
	
	}	
	
	* Horizontal labels 
	summarize n, meanonly
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
	
    * Clean up
    cap erase "$dir_results/temp_labels.txt"

restore 
	
* Export model fit statistics
putexcel set "$dir_results/reg_education_UK", sheet("Gof") modify

putexcel A3 = "E1a - Leaving education", bold		

putexcel A5 = "Pseudo R-squared" 
putexcel B5 = r2_p 
putexcel A6 = "N"
putexcel B6 = N_sample
putexcel E5 = "Chi^2"		
putexcel F5 = chi2
putexcel E6 = "Log likelihood"		
putexcel F6 = ll		

* Clean up 
drop in_sample p
scalar drop _all
matrix drop _all
	

 
/****************** E1b: PROBABILITY OF RETURNING TO EDUCATION ****************/
display "${e1b_if_condition}"	

probit der i.Dgn Dag Dag_sq li.Dcpst_Partnered ///
li.Deh_c4_High li.Deh_c4_Low ///
li.Dehmf_c3_Medium li.Dehmf_c3_Low ///
li.Les_c3_NotEmployed li.Les_c3_Employed ///
l.Dnc l.Dnc02 ///
$regions Year_transformed Y2020 Y2021 $ethnicity ///
if ${e1b_if_condition} ///
	 [pw=dwt], vce(robust)

	
* Save raw results 
matrix results = r(table)
matrix results = results[1..6,1...]'

putexcel set "$dir_raw_results/education/education", sheet("Process E1b") ///
	modify
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))

outreg2 stats(coef se pval) using "$dir_raw_results/education/E1b.doc", ///
	replace ///
title("Process E1b: Probability Returning To Education") ///
	ctitle(Returning student) label side dec(2) noparen ///
	addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll)) ///
	addnote(`"Note: Regression if condition = (${e1b_if_condition})"')	 
	 

* Save sample inclusion indicator and predicted probabilities	 
gen in_sample = e(sample)	
predict p

* Save sample for later use (internal validation)
save "$dir_validation_data/E1b_sample", replace

* Store model summary statistics
scalar r2_p = e(r2_p) 
scalar N_sample = e(N)	 
scalar chi2 = e(chi2)
scalar ll = e(ll)
	 
* Prepare to store results in Excel 

* Eliminate rows and columns containing zeros (baseline cats) 
matrix b = e(b)	
matrix V = e(V)


mata:
    V = st_matrix("V")
    b = st_matrix("b")

    // Find which coefficients are nonzero
    keep = (b :!= 0)
	
	// Eliminate zeros
	b_trimmed = select(b, keep)
    V_trimmed = select(V, keep)
    V_trimmed = select(V_trimmed', keep)'

	b_trimmed 
	V_trimmed 
	
    // Return to Stata
    st_matrix("b_trimmed", b_trimmed')
    st_matrix("V_trimmed", V_trimmed)
	st_matrix("nonzero_b_flag", keep)
end	


* Eigenvalue tests for var-cov invertablility in SimPaths
matrix symeigen X lambda = V_trimmed

scalar max_eig = lambda[1,1]

scalar min_ratio = lambda[1, colsof(lambda)] / max_eig

* Outcome of max eigenvalue test 
if max_eig < 1.0e-12 {
	
    display as error "CRITICAL ERROR: Maximum eigenvalue is too small (`max_eig')."
    display as error "The Variance-Covariance matrix is likely singular."
    exit 999

}

display "Stability Check Passed: Max Eigenvalue is " max_eig

* Outcome of eigenvalue ratio test 
if min_ratio < 1.0e-12 {
	
    display as error "Matrix is ill-conditioned. Min/Max ratio: " min_ratio
    exit 506

}

display "Stability Check Passed. Min/Max ratio: " min_ratio

* Export into Excel 
putexcel set "$dir_results/reg_education_UK", sheet("E1b") modify
putexcel B2 = matrix(b_trimmed)
putexcel C2 = matrix(V_trimmed)


* Labels 
preserve 
putexcel set "$dir_results/reg_education_UK", sheet("E1b") modify 	

putexcel A1 = "REGRESSOR"
putexcel B1 = "COEFFICIENT"


* Use Mata to extract nice labels from colstripe of e(b) (replacement for Stata 14)

local dir_results "$dir_results"
cap erase "$dir_results/temp_labels.txt"

mata:
    // --------------------------------------------------
    // Import objects from Stata
    // --------------------------------------------------
    nonzero_b_flag = st_matrix("nonzero_b_flag")
    stripe         = st_matrixcolstripe("e(b)")

    // Ensure column vector
    nonzero_b_flag = nonzero_b_flag'

    // --------------------------------------------------
    // Extract variable names
    // --------------------------------------------------
    varnames = stripe[.,2]

    // Keep non-baseline coefficients
    varnames_no_bl = select(varnames, nonzero_b_flag :== 1)

    // --------------------------------------------------
    // Clean labels
    // --------------------------------------------------
    labels_no_bl = usubinstr(varnames_no_bl, "1.", "", 1)
    labels_no_bl = regexr(labels_no_bl, "^_cons", "Constant")

    // Handle lags: L.var -> var_L1
    labels_no_bl = ///
        regexm(labels_no_bl, "^L\.") :* ///
        (regexr(labels_no_bl, "^L\.", "") :+ "_L1") :+ ///
        (!regexm(labels_no_bl, "^L\.") :* labels_no_bl)

    // Handle 1L.var
    labels_no_bl = ///
        regexm(labels_no_bl, "^1L\.") :* ///
        (regexr(labels_no_bl, "^1L\.", "") :+ "_L1") :+ ///
        (!regexm(labels_no_bl, "^1L\.") :* labels_no_bl)

    // --------------------------------------------------
    // Add header
    // --------------------------------------------------
    labels_out = "v1" \ labels_no_bl

    // --------------------------------------------------
    // Write to temp file
    // --------------------------------------------------
    outfile = st_local("dir_results") + "/temp_labels.txt"
    fh = fopen(outfile, "w")
    for (i=1; i<=rows(labels_out); i++) {
        fput(fh, labels_out[i])
    }
    fclose(fh)
end


    * Import cleaned labels into Stata
    import delimited "$dir_results/temp_labels.txt", clear varnames(1) encoding(utf8)
	
	gen n = _n
    
    * Export labels to Excel
    putexcel set "$dir_results/reg_education_UK", sheet("E1b") modify 	
		
	* Vertical labels
    summarize n, meanonly
	local N = r(max)+1
	forvalue i = 2/`N' {
	
		local j = `i' - 1
		putexcel A`i' = v1[`j'] 
	
	}	
	
	* Horizontal labels 
	summarize n, meanonly
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
	
	
    * Clean up
    cap erase "$dir_results/temp_labels.txt"
	
restore 

* Export model fit statistics
putexcel set "$dir_results/reg_education_UK", sheet("Gof") modify

putexcel A8 = "E1b - Returning to education", bold		

putexcel A10 = "Pseudo R-squared" 
putexcel B10 = r2_p 
putexcel A11 = "N"
putexcel B11 = N_sample 
putexcel E10 = "Chi^2"		
putexcel F10 = chi2
putexcel E11 = "Log likelihood"		
putexcel F11 = ll
		
* Clean up 		
drop in_sample p
scalar drop _all
matrix drop _all



/****************** E2: EDUCATION ATTAINMENT WHEN LEAVE SCHOOL ****************/
display "${e2_if_condition}"	

gologit2 deh_c3_recoded i.Dgn Dag Dag_sq ///
         i.L_Dehmf_c3_Medium i.L_Dehmf_c3_Low ///
         $regions Year_transformed Y2020 Y2021 $ethnicity ///
         if ${e2_if_condition} [pw=dwt], autofit 

* Save raw results 
matrix results = r(table)
matrix results = results[1..6,1...]'

putexcel set "$dir_raw_results/education/education", sheet("Process E2") ///
	modify
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))


outreg2 stats(coef se pval) using "$dir_raw_results/education/E2.doc", ///
	replace ///
title("Process E2: Educational Attainment When Leave School") ///
	ctitle(Education level) label side dec(2) noparen ///
	addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll)) ///
	addnote(`"Note: Regression if condition = (${e2_if_condition})"')	 	
	
	
* Save sample inclusion indicator and predicted probabilities	
gen in_sample = e(sample)	
predict p1 p2 p3 

* Save sample for estimates validation
save "$dir_validation_data/E2_sample", replace

* Store model summary statistics	
scalar r2_p = e(r2_p) 
scalar N_sample = e(N)	 

* Store results in Excel 

* Store estimates in matrices
matrix b = e(b)	
matrix V = e(V)

* Raw output 
putexcel set "$dir_results/reg_education_UK", sheet("E2_raw") modify
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
putexcel set "$dir_results/reg_education_UK", sheet("E2") modify
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
putexcel set "$dir_results/reg_education_UK", sheet("E2") modify 
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

putexcel set "$dir_results/reg_education_UK", sheet("E2") modify

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
    putexcel set "$dir_results/reg_education_UK", sheet("E2") modify
	
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

putexcel set "$dir_results/reg_education_UK", sheet("Gof") modify

putexcel A13 = "E2 - Education attainment", bold		

putexcel A15 = "Pseudo R-squared" 
putexcel B15 = r2_p 
putexcel A16 = "N"
putexcel B16 = N_sample


* Clean up 		
drop in_sample p1 p2 p3
scalar drop _all
matrix drop _all


capture log close
