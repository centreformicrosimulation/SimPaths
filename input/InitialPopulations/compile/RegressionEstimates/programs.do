
/*==============================================================================
	MATA FUNCTIONS - Define all Mata functions 
==============================================================================*/
mata:
mata clear
mata set matastrict off
end

mata:
mata clear 
mata set matastrict off

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

void extract_and_export_labels(string scalar sheet, real scalar max_n, real scalar is_ologit) {
    nonzero_b_flag = st_matrix("nonzero_b_flag")'
	
    if (is_ologit) {
        stripe = st_matrixcolstripe("b")
    }
    else {
        stripe = st_matrixcolstripe("e(b)")
    }

    varnames = stripe[.,2]
    varnames_no_bl = select(varnames, nonzero_b_flag :== 1)
    labels_no_bl = usubinstr(varnames_no_bl, "1.", "", 1)
    labels_no_bl = regexr(labels_no_bl, "^_cons", "Constant")
    // Handle lags: L.var -> var_L1
    labels_no_bl = ///
        regexm(labels_no_bl, "^L\.") :* ///
        (regexr(labels_no_bl, "^L\.", "") :+ "_L1") :+ (!regexm(labels_no_bl, "^L\.") :* labels_no_bl)
    // Handle 1L.var
    labels_no_bl = ///
        regexm(labels_no_bl, "^1L\.") :* ///
        (regexr(labels_no_bl, "^1L\.", "") :+ "_L1") :+ ///
        (!regexm(labels_no_bl, "^1L\.") :* labels_no_bl)
    // Handle 2L.var
    labels_no_bl = ///
        regexm(labels_no_bl, "^L2\.") :* ///
        (regexr(labels_no_bl, "^L2\.", "") :+ "_L2") :+ ///
        (!regexm(labels_no_bl, "^L2\.") :* labels_no_bl)

    // Truncate labels if max_n is specified (>0)
    if (max_n > 0 & rows(labels_no_bl) > max_n) {
        labels_no_bl = labels_no_bl[1..max_n, .]
    }

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
		

void write_diagonal_to_excel() {
    V_trimmed = st_matrix("V_trimmed")
    b_trimmed = st_matrix("b_trimmed")
    n = cols(b_trimmed)
    
    // Write coefficients
    for (i=1; i<=n; i++) {
        row = i + 1
        coef = b_trimmed[1,i]
        stata("quietly putexcel B" + strofreal(row) + " = (" + strofreal(coef) + ")")
    }
    
    printf("Writing diagonal V-C matrix\n")
    
    // Write full matrix structure with zeros in off-diagonal
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
            // Only write actual variance on diagonal, zero elsewhere
            if (i == j) val = V_trimmed[i,j]
            else val = 0
            stata("quietly putexcel " + col_name + strofreal(row) + " = (" + strofreal(val) + ")")
        }
        if (mod(i, 5) == 0) printf("  Row %g/%g\n", i, n)
    }
    printf("Done (diagonal matrix)\n")
}

void truncate_to_n(real scalar max_n) {
    b_trimmed = st_matrix("b_trimmed")
    V_trimmed = st_matrix("V_trimmed")
    nonzero_b_flag = st_matrix("nonzero_b_flag")
    n = cols(b_trimmed)
    
    if (n > max_n) {
        b_trimmed = b_trimmed[1, 1..max_n]
        V_trimmed = V_trimmed[1..max_n, 1..max_n]
        
        // Update nonzero_b_flag to only reflect first max_n kept variables
        kept = 0
        for (i=1; i<=rows(nonzero_b_flag); i++) {
            if (nonzero_b_flag[i,1] == 1) kept = kept + 1
            if (kept > max_n) nonzero_b_flag[i,1] = 0
        }
        
        st_matrix("b_trimmed", b_trimmed)
        st_matrix("V_trimmed", V_trimmed)
        st_matrix("nonzero_b_flag", nonzero_b_flag)
        printf("Truncated to first %g non-zero estimates\n", max_n)
    }
    else {
        printf("Fewer than %g non-zero estimates (%g found), no truncation needed\n", max_n, n)
    }
}

void reorder_cuts_to_end() {
    b = st_matrix("b")
    V = st_matrix("V")
    stripe = st_matrixcolstripe("e(b)")
    
    // Identify cut point columns
    is_cut = (stripe[.,1] :== "/")
    not_cut = (is_cut :== 0)
    is_cut_row = is_cut'
    not_cut_row = not_cut'
    
    // Reorder b and V
    b_reordered = select(b, not_cut_row), select(b, is_cut_row)
    V_temp = select(V, not_cut_row), select(V, is_cut_row)
    V_reordered = select(V_temp', not_cut_row), select(V_temp', is_cut_row)
    V_reordered = V_reordered'
    
    // Reorder stripe and rename cuts before writing back
    stripe_reordered = (select(stripe, not_cut) \ select(stripe, is_cut))
    for (i=1; i<=rows(stripe_reordered); i++) {
        if (stripe_reordered[i,1] == "/" & regexm(stripe_reordered[i,2], "^cut([0-9]+)")) {
            stripe_reordered[i,2] = "Cut" + regexs(1)
            stripe_reordered[i,1] = ""
        }
    }
    
    // Write back with correct stripe
    st_matrix("b", b_reordered)
    st_matrix("V", V_reordered)
    st_matrixcolstripe("b", stripe_reordered)
    st_matrixcolstripe("V", stripe_reordered)
    st_matrixrowstripe("V", stripe_reordered)
    
}


void build_gologit_structure(real scalar n_outcomes) {
    b = st_matrix("b")
    V = st_matrix("V")
    
    // Step 1: Remove zero coefficients (baseline categories)
    keep = (b :!= 0)
    nonzero_b = select(b, keep)
    V_trimmed = select(V, keep)
    V_trimmed = select(V_trimmed', keep)'
    st_matrix("nonzero_b", nonzero_b)
    st_matrix("nonzero_b_flag", keep)
    
    // Step 2: Detect repeated coefficients (proportional odds vars)
    n = cols(nonzero_b)
    n_per = n / (n_outcomes - 1)
    repetition_flag = J(n, 1, 0)
    tol = 1e-8
    for (i=1; i<=n; i++) {
        found = 0
        for (j=1; j<=n; j++) {
            if (found == 0 & i != j & abs(nonzero_b[1,i] - nonzero_b[1,j]) < tol) {
                repetition_flag[i] = 1
                found = 1
            }
        }
    }
    unique_flag = 1 :- repetition_flag    
	st_matrix("repetition_flag", repetition_flag')
    st_matrix("unique_flag", unique_flag')

    
    // Step 3: Build structure vector
    structure_a = J(1, n_per, 1)
    structure_b = unique_flag[n_per+1::n]'
    structure = structure_a, structure_b
    st_matrix("structure", structure)
    
    // Step 4: Apply structure to b
    b_structure = structure :* nonzero_b
    keep2 = (b_structure :!= 0)
    nonzero_b_structure = select(b_structure, keep2)
    st_matrix("nonzero_b_structure", nonzero_b_structure)
    
    // Step 5: Apply structure to V
    square_structure_a = J(n, 1, 1) * structure
    square_structure_b = square_structure_a'
    square_structure = square_structure_a :* square_structure_b
    var_structure = square_structure :* V_trimmed
    row_keep = (rowsum(abs(var_structure)) :!= 0)
    col_keep = (colsum(abs(var_structure)) :!= 0)
    nonzero_var_structure = select(select(var_structure, col_keep), row_keep)
    st_matrix("nonzero_var_structure", nonzero_var_structure)
    
    printf("Gologit structure built: %g unique coefficients\n", cols(nonzero_b_structure))
}

void export_labels_gologit(string scalar sheet) {
    nonzero_b_flag = st_matrix("nonzero_b_flag")'
    unique_flag = st_matrix("unique_flag")'
    structure = st_matrix("structure")'
    stripe = st_matrixcolstripe("e(b)")
    
    catnames = stripe[.,1]
    varnames = stripe[.,2]
    varnames_no_bl = select(varnames, nonzero_b_flag :== 1)
    catnames_no_bl = select(catnames, nonzero_b_flag :== 1)
    
    // Clean variable names
    labels_no_bl = usubinstr(varnames_no_bl, "1.", "", 1)
    labels_no_bl = regexr(labels_no_bl, "^_cons", "Constant")
    labels_no_bl = (regexm(labels_no_bl, "^L\.") :*
        (regexr(labels_no_bl, "^L\.", "") :+ "_L1")) :+
        (!regexm(labels_no_bl, "^L\.") :* labels_no_bl)
    
    // Add category suffix only for non-proportional odds vars (unique_flag == 1)
    labels_no_bl = labels_no_bl :+
        (("_" :+ catnames_no_bl) :* (unique_flag[1::rows(labels_no_bl)] :== 1))
    
    // Filter by structure
    final_labels = select(labels_no_bl, structure[1::rows(labels_no_bl)] :== 1)
    
    n_labs = rows(final_labels)
    for (i=1; i<=n_labs; i++) {
        row = i + 1
        stata("quietly putexcel A" + strofreal(row) + " = " + char(34) + final_labels[i] + char(34))
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
        stata("quietly putexcel " + col_name + "1 = " + char(34) + final_labels[j] + char(34))
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
	
	syntax, domain(string) sheet(string)
	
	* Set up Excel file
	putexcel set "$dir_results/reg_`domain'", sheet("`sheet'") modify
	
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
	
	syntax, domain(string) row(integer) label(string)
	
	putexcel set "$dir_results/reg_`domain'", sheet("Gof") modify
	
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
	
	syntax, domain(string) row(integer) label(string)
	
	putexcel set "$dir_results/reg_`domain'", sheet("Gof") modify
	
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
	
	syntax, domain(string) process(string) title(string) [ifcond(string)]
	
	* Save to Excel
	matrix results = r(table)
	matrix results = results[1..6,1...]'
	
	putexcel set "$dir_raw_results/`domain'/`domain'", ///
		sheet("Process `process'") replace
	putexcel A3 = matrix(results), names nformat(number_d2)
	putexcel J4 = matrix(e(V))
	
	* Save to Word (conditional on outreg2 being installed)
	capture which outreg2
	if _rc == 0 {
		if "`ifcond'" != "" {
			local note `"addnote("Note: Regression if condition = (`ifcond')")"'
		}
		
		* Check if probit/logit/ologit or OLS
		if "`e(cmd)'" == "probit" | "`e(cmd)'" == "logit" | "`e(cmd)'" == "ologit" | "`e(cmd)'" == "gologit2" {
			outreg2 stats(coef se pval) using ///
				"$dir_raw_results/`domain'/`process'.doc", replace ///
				title("`title'") ctitle(Model) label side dec(2) noparen ///
				addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll)) `note'
		}
		else {
			outreg2 stats(coef se pval) using ///
				"$dir_raw_results/`domain'/`process'.doc", replace ///
				title("`title'") ctitle(Model) label side dec(2) noparen ///
				addstat(R2, e(r2)) `note'
		}
	}
	
end


* Main export routine: combines matrix operations, stability checks, and Excel export
capture program drop export_results_to_excel
program define export_results_to_excel
	
	syntax, domain(string) sheet(string) [probit ologit gformula maxestimates(integer 11)]
	
	* Store estimates
	matrix b = e(b)
	matrix V = e(V)
	
	* For ologit, reorder cuts to end before trimming
	if "`ologit'" == "ologit" {
		mata: reorder_cuts_to_end()
	}
	
	* Trim zero coefficients
	mata: trim_matrices()
	
	* For gformula, further truncate to first maxestimates non-zero estimates
	if "`gformula'" == "gformula" {
		mata: truncate_to_n(`maxestimates')
	}
	
	* Check matrix stability (skip for gformula)
	if "`gformula'" != "gformula" {
		check_matrix_stability
	}
	
	* Export to Excel - use modify mode (file already created in setup)
	putexcel set "$dir_results/reg_`domain'", sheet("`sheet'") modify
	putexcel A1 = "REGRESSOR"
	putexcel B1 = "COEFFICIENT"
	
	* Write coefficients cell-by-cell
	if "`gformula'" == "gformula" {
        putexcel C1 = "VARIANCE"
        mata: write_diagonal_to_excel()
    }
    else {
        mata: write_all_to_excel()
    }
	
	* Extract and export labels
	if "`ologit'" == "ologit" {
		mata: extract_and_export_labels("`sheet'", 0, 1)
	}
	else if "`gformula'" == "gformula" {
		mata: extract_and_export_labels("`sheet'", `maxestimates', 0)
	}
	else {
		mata: extract_and_export_labels("`sheet'", 0, 0)
	}
	
	* Store model statistics
	if "`probit'" == "probit" | "`ologit'" == "ologit" {
		scalar r2_p = e(r2_p)
		scalar chi2 = e(chi2)
		scalar ll = e(ll)
	}
	else {
		scalar r2 = e(r2)
	}
	scalar N_sample = e(N)
	
end


/*==============================================================================
	COMPLETE WORKFLOW PROGRAMS 
==============================================================================*/

* Complete workflow: save sample, export results, and clean up
capture program drop process_regression
program define process_regression
	
	syntax, domain(string) process(string) sheet(string) title(string) ///
		gofrow(integer) goflabel(string) [ifcond(string) ///
		probit gformula maxestimates(integer 11)]	
	
	* Save raw results
	save_raw_results, domain("`domain'") process("`process'") ///
		title("`title'") ifcond("`ifcond'")
	
	* Save sample for validation
	gen in_sample = e(sample)
	predict p
	save "$dir_validation_data/`process'_sample", replace
	
	* Export results to Excel
	if "`gformula'" == "gformula" {
		export_results_to_excel, domain("`domain'") sheet("`sheet'") ///
			gformula maxestimates(`maxestimates')	
		export_gof_ols, domain("`domain'") row(`gofrow') label("`goflabel'")
	}
	else if "`probit'" == "probit" {
		export_results_to_excel, domain("`domain'") sheet("`sheet'") probit
		export_gof_probit, domain("`domain'") row(`gofrow') label("`goflabel'")
	}
	else {
		export_results_to_excel, domain("`domain'") sheet("`sheet'")
		export_gof_ols, domain("`domain'") row(`gofrow') label("`goflabel'")
	}
	
	* Clean up
	drop in_sample p
	scalar drop _all
	matrix drop _all
	
end


* Specialized workflow for ordered logit models
capture program drop process_ologit
program define process_ologit

    syntax, domain(string) process(string) sheet(string) title(string) ///
        gofrow(integer) goflabel(string) [ifcond(string)]

    * Save raw results
    save_raw_results, domain("`domain'") process("`process'") ///
        title("`title'") ifcond("`ifcond'")

    * Save sample for validation
    gen in_sample = e(sample)
    predict p
    save "$dir_validation_data/`process'_sample", replace

    * reorder_cuts_to_end removed - handled inside export_results_to_excel

    * Export results to Excel
    export_results_to_excel, domain("`domain'") sheet("`sheet'") ologit

    * Export GoF
    export_gof_probit, domain("`domain'") row(`gofrow') label("`goflabel'")

    * Clean up
    drop in_sample p
    scalar drop _all
    matrix drop _all

end


* Specialized workflow for generalized ordered logit models
capture program drop process_gologit
program define process_gologit

    syntax, domain(string) process(string) sheet(string) title(string) ///
        gofrow(integer) goflabel(string) outcomes(integer) [ifcond(string)]
    * Note: outcomes() = total number of categories INCLUDING the base category

    * Save raw results
    matrix results = r(table)
    matrix results = results[1..6,1...]'
    putexcel set "$dir_raw_results/`domain'/`domain'", ///
        sheet("Process `process'") modify
    putexcel A3 = matrix(results), names nformat(number_d2)
    putexcel J4 = matrix(e(V))

    * Save to Word
    capture which outreg2
    if _rc == 0 {
        if "`ifcond'" != "" {
            local note `"addnote("Note: Regression if condition = (`ifcond')")"'
        }
        outreg2 stats(coef se pval) using ///
            "$dir_raw_results/`domain'/`process'.doc", replace ///
            title("`title'") ctitle(Education level) label side dec(2) noparen ///
            addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll)) `note'
    }

    * Save sample and predictions
    gen in_sample = e(sample)
    local plist ""
    forvalues k = 1/`outcomes' {
        local plist "`plist' p`k'"
    }
    predict `plist'
    save "$dir_validation_data/`process'_sample", replace

    * Store model summary statistics
    scalar r2_p = e(r2_p)
    scalar chi2 = e(chi2)
    scalar ll = e(ll)
    scalar N_sample = e(N)

    * Store estimates in matrices
    matrix b = e(b)
    matrix V = e(V)

    * Raw output
    putexcel set "$dir_results/reg_`domain'", sheet("`sheet'_raw") modify
    putexcel A1 = matrix(b'), names nformat(number_d2)
    putexcel A1 = "CATEGORY"
    putexcel B1 = "REGRESSOR"
    putexcel C1 = "COEFFICIENT"

    * Build gologit structure
    mata: build_gologit_structure(`outcomes')

    * Eigenvalue stability check
    matrix symeigen X lambda = nonzero_var_structure
    scalar max_eig = lambda[1,1]
    scalar min_ratio = lambda[1, colsof(lambda)] / max_eig
    if max_eig < 1.0e-12 {
        display as error "CRITICAL ERROR: Variance-covariance matrix is near singular."
        exit 999
    }
    if min_ratio < 1.0e-12 {
        display as error "Matrix is ill-conditioned. Min/Max ratio: " min_ratio
        exit 506
    }
    display "VCV stability check passed. Max eigenvalue: " max_eig
    display "Min/Max ratio: " min_ratio

    * Export to Excel
    putexcel set "$dir_results/reg_`domain'", sheet("`sheet'") modify
    putexcel A1 = "REGRESSOR"
    putexcel B1 = "COEFFICIENT"
    putexcel B2 = matrix(nonzero_b_structure')
    putexcel C2 = matrix(nonzero_var_structure)

    * Extract and export labels
    mata: export_labels_gologit("`sheet'")

    * Goodness of fit
    putexcel set "$dir_results/reg_`domain'", sheet("Gof") modify
    local row2 = `gofrow' + 1
    local row3 = `gofrow' + 2
    putexcel A`gofrow' = "`goflabel'", bold
    putexcel A`row2' = "Pseudo R-squared"
    putexcel B`row2' = r2_p
    putexcel A`row3' = "N"
    putexcel B`row3' = N_sample
    putexcel E`row2' = "Chi^2"
    putexcel F`row2' = chi2
    putexcel E`row3' = "Log likelihood"
    putexcel F`row3' = ll

    * Clean up
    drop in_sample `plist'
    scalar drop _all
    matrix drop _all

end

