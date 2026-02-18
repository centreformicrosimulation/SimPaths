
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
	save "$dir_validation_data/`process'_sample", replace
	
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
	
	save "$dir_validation_data/`process'_sample", replace
	
	* Export results to Excel
	export_results_to_excel, sheet("`sheet'") probit
	export_gof_probit, row(`gofrow') label("`goflabel'")
	
	* Clean up
	drop in_sample p*
	scalar drop _all
	matrix drop _all
	
end
