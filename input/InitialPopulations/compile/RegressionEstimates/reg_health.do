********************************************************************************
* PROJECT:  		SimPaths UK 
* SECTION:			Health
* OBJECT: 			Health status and Disability
* AUTHORS:			Daria Popova, Justin van de Ven
* LAST UPDATE:		4 Feb 2026 DP  
* COUNTRY: 			UK 
*
* NOTES:	  Combined former a and b processes.
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
log using "${dir_log}/reg_health.log", replace
*******************************************************************

* Set Excel file 

* Info sheet

putexcel set "$dir_results/reg_health_UK", sheet("Info") replace
putexcel A1 = "Description:"
putexcel B1 = "Model parameters governing projection self-reported health status"
putexcel A2 = "Authors:	Patryk Bronka, Justin van de Ven, Daria Popova" 
putexcel A3 = "Last edit: 4 Feb 2026 DP"

putexcel A4 = "Process:", bold
putexcel B4 = "Description:", bold

putexcel A5 = "H1"
putexcel B5 = "Generalized ordered logit regression estimates of self reported health status"
putexcel B6 = "Covariates that satisfy the parallel lines assumption have one estimate for all categories of the dependent variable and are present once in the table"
putexcel B7 = "Covariates that do not satisfy the parallel lines assumption have an estimate for each estimated category of the dependent variable. These covariates have the dependent variable category appended to their name."

putexcel A8 = "H1_raw"
putexcel B8 = "Raw generalized ordered logit regression estimates of self reported health status. Useful for the 'Gologit predictor' file."

putexcel A11 = "H2"
putexcel B11 = "Probit regression estimates of the probability of being long-term sick or disabled"

putexcel A15 = "Notes:", bold
putexcel B15 = "Estimation sample: UK_ipop.dta with grossing up weight dwt" 
putexcel B16 = "Conditions for processes are defined as globals in master.do"
putexcel B17 = "Combined former processes H1a and H1b"

putexcel set "$dir_work/reg_health", sheet("Gof") modify
putexcel A1 = "Goodness of fit", bold	


/********************************* PREPARE DATA *******************************/

* Load data 
use ${estimation_sample}, clear

* Set data 
xtset idperson swv
sort idperson swv 

* Remove children 
drop if dag < 16

* Adjust variables 
do "${dir_do}/variable_update.do"
 
 
/********************************** ESTIMATION ********************************/

/********************** H1: SELF-REPORTED HEALTH STATUS ***********************/
display "${h1_if_condition}"

gologit2 dhe Ded Dgn Dag Dag_sq ///  /*Ded_Dag Ded_Dag_sq Ded_Dgn /// */
	     L_Dhe_pcs L_Dhe_mcs ///
		 Deh_c3_Medium Deh_c3_Low ///
	     /*L_Les_c4_Student*/ L_Les_c4_NotEmployed L_Les_c4_Retired ///
		 L_Ydses_c5_Q2 L_Ydses_c5_Q3 L_Ydses_c5_Q4 L_Ydses_c5_Q5 ///
		 L_Dhhtp_c4_CoupleChildren L_Dhhtp_c4_SingleNoChildren L_Dhhtp_c4_SingleChildren ///
		 $regions Year_transformed Y2020 Y2021 $ethnicity if ///
	     ${h1_if_condition} [pw=dwt], autofit
	  
*Note: In gologit2, the coefficients show how covariates affect the log-odds of being above a certain category vs. at or below it.

* Save raw results 
matrix results = r(table)
matrix results = results[1..6,1...]'

putexcel set "$dir_raw_results/health/health", ///
	sheet("Process H1") replace
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))

outreg2 stats(coef se pval) using ///
	"$dir_raw_results/health/H1.doc", replace ///
title("Process H1: Self-Reported Health Status") ///
	ctitle(Health) label side dec(2) noparen ///
	addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll)) ///
	addnote(`"Note: Regression if condition = (${h1_if_condition})"')		

	
* Save sample inclusion indicator and predicted probabilities		
gen in_sample = e(sample)
predict p1 p2 p3 p4 p5
	
* Save sample for estimate validation
save "$dir_validation_data/H1_sample", replace

* Store model summary statistics
scalar r2_p = e(r2_p) 
scalar N_sample = e(N)	 
	
	
* Store results in Excel 

* Store estimates in matrices
matrix b = e(b)	
matrix V = e(V)

* Raw output 
putexcel set "$dir_results/reg_health_UK", sheet("H1_raw") modify
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
* repetitions of coefficients for vars that satify the proportional odds 
* assumptions
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
putexcel set "$dir_results/reg_health_UK", sheet("H1") modify
putexcel A1 = matrix(nonzero_b_structure'), names nformat(number_d2) 


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
putexcel set "$dir_results/reg_health_UK", sheet("H1") modify
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
putexcel set "$dir_results/reg_health_UK", sheet("H1") modify

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
    putexcel set "$dir_results/reg_health_UK", sheet("H1") modify
	
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
    

* Export model fit statistics
putexcel set "$dir_results/reg_health_UK", sheet("Gof") modify

putexcel A3 = "H1 - Health status", bold		

putexcel A5 = "Pseudo R-squared" 
putexcel B5 = r2_p 
putexcel A6 = "N"
putexcel B6 = N_sample
		
* Clean up 		
drop in_sample p1 p2 p3 p4 p5 
scalar drop _all
matrix drop _all


/**************** H2: PROBABILITY LONG-TERM SICK OR DISABLED ******************/
display "${h2_if_condition}"

probit dlltsd01 i.Dgn Dag Dag_sq ///
       Deh_c3_Medium Deh_c3_Low ///
	   L_Ydses_c5_Q2 L_Ydses_c5_Q3 L_Ydses_c5_Q4 L_Ydses_c5_Q5 ///
	   Dhe_pcs Dhe_mcs ///
	   L_Dhe_pcs L_Dhe_mcs ///
	   L_Dlltsd01 ///
	   L_Dhhtp_c4_CoupleChildren L_Dhhtp_c4_SingleNoChildren L_Dhhtp_c4_SingleChildren /// 
	   $regions Year_transformed Y2020 Y2021 $ethnicity ///
	if ${h2_if_condition} [pw = dwt], vce(robust)
	
	
	* raw results 
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_raw_results/health/health", sheet("Process H2") modify
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))
outreg2 stats(coef se pval) using "$dir_raw_results/health/H2.doc", replace ///
title("Process H2b: Probit regression estimates for being long-term sick or disabled - people aged 16+ not in continuous education") ///
 ctitle(long-term sick or disabled) label side dec(2) noparen addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll))

gen in_sample = e(sample)	

predict p 
 
* Save sample for later use (internal validation)
save "$dir_validation_data/H2_sample", replace

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
putexcel set "$dir_results/reg_health_UK", sheet("H2") modify
putexcel B2 = matrix(b_trimmed)
putexcel C2 = matrix(V_trimmed)


* Labels 
putexcel set "$dir_results/reg_health_UK", sheet("H2") modify

putexcel A1 = "REGRESSOR"
putexcel B1 = "COEFFICIENT"


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
putexcel set "$dir_results/reg_health_UK", sheet("H2") modify
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
putexcel set "$dir_results/reg_health_UK", sheet("Gof") modify

putexcel A15 = "H2-Long-term sick/disabled or on disability benefits", bold		
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
 	

capture log close 

