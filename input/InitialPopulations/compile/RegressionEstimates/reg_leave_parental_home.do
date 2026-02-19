********************************************************************************
* PROJECT:  		SimPaths UK
* SECTION:			Leaving Parental Home
* OBJECT: 			Final Probit Regression Model 
* AUTHORS:			Daria Popova, Justin van de Ven, Aleksandra Kolndrekaj
* LAST UPDATE:		18 Feb 2026 AK  
* COUNTRY: 			UK  
* 
* NOTES: 			
**********************************************************************************

clear all
set more off
set mem 200m
set type double
//set maxvar 120000
set maxvar 30000


*******************************************************************
cap log close 
log using "${dir_log}/reg_leave_parental_home.log", replace
*******************************************************************

* Set Excel file 

* Info sheet

putexcel set "$dir_results/reg_leave_parental_home_UK", sheet("Info") replace
putexcel A1 = "Description:"
putexcel B1 = "Model parameters governing leaving parental home"
putexcel A2 = "Authors:	Patryk Bronka, Justin van de Ven, Daria Popova, Aleksandra Kolndrekaj" 
putexcel A3 = "Last edit: 19 Jan 2026 DP"

putexcel A4 = "Process:", bold
putexcel B4 = "Description:", bold
putexcel A5 = "P1a"
putexcel B5 = "Probit regression estimates for leaving the parental home, transitioning out of adult child status"

putexcel A10 = "Notes:", bold
putexcel B10 = "Estimation sample: UK_ipop.dta with grossing up weight dwt" 
putexcel B11 = "Conditions for processes are defined as globals in master.do"

putexcel set "$dir_results/reg_leave_parental_home_UK", sheet("Gof") modify
putexcel A1 = "Goodness of fit", bold		


/********************************* PREPARE DATA *******************************/

* Load data
use "${estimation_sample}", clear

* Set data 
xtset idperson swv
sort idperson swv 

* Remove children 
drop if dag < 16

* Adjust variables 
do "${dir_do}/variable_update.do"


/********************************** ESTIMATION ********************************/

/**************** P1: PROBABILITY OF LEAVING THE PARENTAL HOME ****************/
display "${p1_if_condition}"
	
probit dlftphm i.Dgn Dag Dag_sq li.Deh_c4_Na li.Deh_c4_Medium li.Deh_c4_Low ///
	li.Les_c3_Student li.Les_c3_NotEmployed ///
	li.Ydses_c5_Q2 li.Ydses_c5_Q3 li.Ydses_c5_Q4 li.Ydses_c5_Q5 ///
	$regions Year_transformed Y2020 Y2021 $ethnicity ///
	if ${p1_if_condition} [pw=dwt], vce(robust)

	* Save raw results 
matrix results = r(table)
matrix results = results[1..6,1...]'

putexcel set "$dir_raw_results/leave_parental_home/leave_parental_home", ///
	sheet("Process P1") replace
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))

outreg2 stats(coef se pval) using ///
	"$dir_raw_results/leave_parental_home/P1.doc", replace ///
title("Process P1: Probability Leave the Parental Home") ///
	ctitle(Leave home) label side dec(2) noparen ///
	addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll)) ///
	addnote(`"Note: Regression if condition = (${p1_if_condition})"')	
	
* Save sample inclusion indicator and predicted probabilities	
gen in_sample = e(sample)	
predict p

* Save sample for estiamte validation
save "$dir_validation_data/P1_sample", replace
	
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
putexcel set "$dir_results/reg_leave_parental_home_UK", sheet("P1") modify
putexcel B2 = matrix(b_trimmed)
putexcel C2 = matrix(V_trimmed)


* Labels 
preserve 
putexcel set "$dir_results/reg_leave_parental_home_UK", sheet("P1") modify

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
    putexcel set "$dir_results/reg_leave_parental_home_UK", sheet("P1") modify 	
	
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
putexcel set "$dir_results/reg_leave_parental_home_UK", sheet("Gof") modify

putexcel A3 = "P1 - Leaving the parental home ", bold		

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
	

capture log close 
