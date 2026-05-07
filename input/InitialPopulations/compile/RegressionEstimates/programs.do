
/*==============================================================================
	MATA FUNCTIONS - Define all Mata functions
==============================================================================*/
mata:
mata clear
mata set matastrict off

// Format labels
void extract_and_export_labels(string scalar domain, string scalar sheet, real scalar max_n, real scalar is_ologit) {
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

    // Write labels via xl() — avoids stata() round-trip overhead per cell
    // Column A rows 2+ (vertical), row 1 cols C+ (horizontal)
    real scalar n_labs, i
    n_labs = rows(labels_no_bl)
    string scalar path
    path = st_global("dir_results") + "/reg_" + domain + ".xlsx"
    class xl scalar xbook
    xbook = xl()
    xbook.load_book(path)
    xbook.set_sheet(sheet)
    for (i=1; i<=n_labs; i++) xbook.put_string(i+1, 1, labels_no_bl[i])
    for (i=1; i<=n_labs; i++) xbook.put_string(1, i+2, labels_no_bl[i])
    xbook.close_book()
}

// Create var diagonal matrix
void write_diagonal_to_excel() {
    // Pull the matrices into Mata
    V_trimmed = st_matrix("V_trimmed")
    b_trimmed = st_matrix("b_trimmed")

    // Write coefficients to Column B
    stata("quietly putexcel B2 = matrix(b_trimmed')")

    printf("Creating diagonal matrix...\n")

    // Create a diagonal version of V_trimmed
    // diag(diagonal(V)) keeps the 'spine' and fills the rest with 0s
    V_diag = diag(diagonal(V_trimmed))

    // Push this modified matrix BACK into Stata's memory
    // This overwrites the old "V_trimmed" with the diagonal version
    st_replacematrix("V_trimmed", V_diag)

    // Now tell Stata to write the matrix it now sees as diagonal
    stata("quietly putexcel C2 = matrix(V_trimmed)")

    printf("Done (Diagonal matrix written)\n")
}

// Ensure cuts are at the end of matrix
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

// Format gologit estimates and var-cov matricies
void build_gologit_structure(real scalar n_outcomes) {
    b = st_matrix("b")
    V = st_matrix("V")

    // Remove zero coefficients (baseline categories)
    keep = (b :!= 0)
    nonzero_b = select(b, keep)
    V_trimmed = select(V, keep)
    V_trimmed = select(V_trimmed', keep)'
    st_matrix("nonzero_b", nonzero_b)
    st_matrix("nonzero_b_flag", keep)

    // Detect repeated coefficients (proportional odds vars)
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

    // Build structure vector
    structure_a = J(1, n_per, 1)
    structure_b = unique_flag[n_per+1::n]'
    structure = structure_a, structure_b
    st_matrix("structure", structure)

    // Apply structure to b
    b_structure = structure :* nonzero_b
    keep2 = (b_structure :!= 0)
    nonzero_b_structure = select(b_structure, keep2)
    st_matrix("nonzero_b_structure", nonzero_b_structure)

    // Apply structure to V
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

// Format gologit labels
void export_labels_gologit(string scalar domain, string scalar sheet) {
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

    // Add category suffix only for non-prop odds vars (unique_flag == 1)
    labels_no_bl = labels_no_bl :+
        (("_" :+ catnames_no_bl) :* (unique_flag[1::rows(labels_no_bl)] :== 1))

    // Filter by structure
    final_labels = select(labels_no_bl, structure[1::rows(labels_no_bl)] :== 1)

    // Write labels via xl() — avoids stata() round-trip overhead per cell
    // Column A rows 2+ (vertical), row 1 cols C+ (horizontal)
    real scalar n_labs, i
    n_labs = rows(final_labels)
    string scalar path
    path = st_global("dir_results") + "/reg_" + domain + ".xlsx"
    class xl scalar xbook
    xbook = xl()
    xbook.load_book(path)
    xbook.set_sheet(sheet)
    for (i=1; i<=n_labs; i++) xbook.put_string(i+1, 1, final_labels[i])
    for (i=1; i<=n_labs; i++) xbook.put_string(1, i+2, final_labels[i])
    xbook.close_book()
}

// Auto-extract labels for one stage of a Heckman model from e(b) column stripe.
// is_outcome=1 - outcome equation (all columns except "select");
// is_outcome=0 - selection equation (columns where eq=="select").
// Applies the same formatting as extract_and_export_labels.
// Writes vertically to col A (from row 2) and horizontally to row 1 (from col C).
void extract_heckman_stage_labels(string scalar domain, string scalar sheet, real scalar is_outcome) {
    string matrix stripe
    real matrix b_full
    stripe = st_matrixcolstripe("e(b)")
    b_full = st_matrix("e(b)")

    real scalar n, i
    n = rows(stripe)

    string colvector labels_raw
    labels_raw = J(0, 1, "")

    for (i = 1; i <= n; i++) {
        string scalar eq_i
        eq_i = stripe[i, 1]

        real scalar include
        if (is_outcome) include = (eq_i != "select")
        else            include = (eq_i == "select")

        // Exclude base-category coefficients (zero b)
        if (include & b_full[1, i] != 0) {
            labels_raw = labels_raw \ stripe[i, 2]
        }
    }

    // Apply same formatting as extract_and_export_labels
    string colvector labels
    labels = usubinstr(labels_raw, "1.", "", 1)
    labels = regexr(labels, "^_cons", "Constant")
    // Handle lags: L.var -> var_L1
    labels = regexm(labels, "^L\.") :* (regexr(labels, "^L\.", "") :+ "_L1") :+
             (!regexm(labels, "^L\.") :* labels)
    // Handle 1L.var
    labels = regexm(labels, "^1L\.") :* (regexr(labels, "^1L\.", "") :+ "_L1") :+
             (!regexm(labels, "^1L\.") :* labels)
    // Handle L2.var
    labels = regexm(labels, "^L2\.") :* (regexr(labels, "^L2\.", "") :+ "_L2") :+
             (!regexm(labels, "^L2\.") :* labels)

    // Rename the inverse Mills ratio variable to a readable label
    for (i = 1; i <= rows(labels); i++) {
        if (labels[i] == "lambda") labels[i] = "InvMillsRatio"
    }

    // Write via xl()
    real scalar n_labs
    n_labs = rows(labels)
    string scalar path
    path = st_global("dir_results") + "/reg_" + domain + ".xlsx"
    class xl scalar xbook
    xbook = xl()
    xbook.load_book(path)
    xbook.set_sheet(sheet)
    for (i = 1; i <= n_labs; i++) xbook.put_string(i+1, 1, labels[i])
    for (i = 1; i <= n_labs; i++) xbook.put_string(1, i+2, labels[i])
    xbook.close_book()
}

end


/*==============================================================================
	HELPER PROGRAMS - Modular functions for common operations
==============================================================================*/

* Load matrices and remove zero coefficients (baseline categories)
capture program drop trim_matrices
program define trim_matrices

    local k = colsof(b)

    // Identify non-zero coefficient column indices
    local keep_idx ""
    forvalues j = 1/`k' {
        if b[1,`j'] != 0 {
            local keep_idx "`keep_idx' `j'"
        }
    }
    local n_keep = wordcount("`keep_idx'")

    // Build nonzero_b_flag as 1 x k row vector
    matrix nonzero_b_flag = J(1, `k', 0)
    foreach j of local keep_idx {
        matrix nonzero_b_flag[1, `j'] = 1
    }

    // Build b_trimmed: 1 x n_keep
    matrix b_trimmed = J(1, `n_keep', 0)
    local c = 1
    foreach j of local keep_idx {
        matrix b_trimmed[1, `c'] = b[1, `j']
        local ++c
    }

    // Build V_trimmed: n_keep x n_keep
    matrix V_trimmed = J(`n_keep', `n_keep', 0)
    local r = 1
    foreach i of local keep_idx {
        local c = 1
        foreach j of local keep_idx {
            matrix V_trimmed[`r', `c'] = V[`i', `j']
            local ++c
        }
        local ++r
    }

    display "Matrices transferred successfully"

end


* Truncate trimmed matrices to first max_n non-zero estimates
capture program drop truncate_to_n
program define truncate_to_n

    syntax, maxn(integer)

    local n = colsof(b_trimmed)

    if `n' > `maxn' {

        // Truncate b_trimmed and V_trimmed to first maxn columns/rows
        matrix b_trimmed = b_trimmed[1, 1..`maxn']
        matrix V_trimmed = V_trimmed[1..`maxn', 1..`maxn']

        // Update nonzero_b_flag: zero out entries beyond the first maxn kept variables
        local k = colsof(nonzero_b_flag)
        local kept = 0
        forvalues i = 1/`k' {
            if nonzero_b_flag[1, `i'] == 1 {
                local kept = `kept' + 1
            }
            if `kept' > `maxn' {
                matrix nonzero_b_flag[1, `i'] = 0
            }
        }

        display "Truncated to first `maxn' non-zero estimates"
    }
    else {
        display "Fewer than `maxn' non-zero estimates (`n' found), no truncation needed"
    }

end


* Check var-cov matrix eigenvalues for stability (conformability)
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


* Paste estimates and var-cov matrices to Excel
capture program drop write_all_to_excel
program define write_all_to_excel

	display "Writing Var-Cov matrix"
	quietly putexcel B2 = matrix(b_trimmed')
	quietly putexcel C2 = matrix(V_trimmed)
	display "Done"

end


* Export labels to Excel (both vertical and horizontal)
capture program drop export_labels_to_excel
program define export_labels_to_excel

	syntax, domain(string) sheet(string)

	// Set up Excel file
	putexcel set "$dir_results/reg_`domain'", sheet("`sheet'") modify

	// Collect labels from locals and write via xl() — avoids stata() overhead per cell
	mata: {
		real scalar n, i
		n = strtoreal(st_local("n_labels"))
		string scalar path
		path = st_global("dir_results") + "/reg_" + st_local("domain") + ".xlsx"
		class xl scalar xbook
		xbook = xl()
		xbook.load_book(path)
		xbook.set_sheet(st_local("sheet"))
		for (i=1; i<=n; i++) xbook.put_string(i+1, 1, st_local("lbl" + strofreal(i)))
		for (i=1; i<=n; i++) xbook.put_string(1, i+2, st_local("lbl" + strofreal(i)))
		xbook.close_book()
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

	// Save to Excel
	matrix results = r(table)
	matrix results = results[1..6,1...]'

	putexcel set "$dir_raw_results/`domain'/`domain'", ///
		sheet("Process `process'") replace
	putexcel A3 = matrix(results), names nformat(number_d2)
	putexcel J4 = matrix(e(V))

	// Save to Word (conditional on outreg2 being installed)
	capture which outreg2
	if _rc == 0 {
		if "`ifcond'" != "" {
			local note `"addnote("Note: Regression if condition = (`ifcond')")"'
		}

		// Check if probit/logit/ologit or OLS
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

	// Store estimates
	matrix b = e(b)
	matrix V = e(V)

	// For ologit, reorder cuts to end before trimming
	if "`ologit'" == "ologit" {
		mata: reorder_cuts_to_end()
	}

	// Trim zero coefficients
	trim_matrices

	// For gformula, further truncate to first maxestimates non-zero estimates
	if "`gformula'" == "gformula" {
		truncate_to_n, maxn(`maxestimates')
	}

	// Check matrix stability (skip for gformula)
	if "`gformula'" != "gformula" {
		check_matrix_stability
	}

	// Export to Excel - use modify mode (file already created in setup)
	putexcel set "$dir_results/reg_`domain'", sheet("`sheet'") modify
	putexcel A1 = "REGRESSOR"
	putexcel B1 = "COEFFICIENT"

	// Write coefficient and variance-covariance matrices
	if "`gformula'" == "gformula" {
        putexcel C1 = "VARIANCE"
        mata: write_diagonal_to_excel()
    }
    else {
        write_all_to_excel
    }

	// Extract and export labels using bulk xl() writes (domain passed for file path)
	if "`ologit'" == "ologit" {
		mata: extract_and_export_labels("`domain'", "`sheet'", 0, 1)
	}
	else if "`gformula'" == "gformula" {
		mata: extract_and_export_labels("`domain'", "`sheet'", `maxestimates', 0)
	}
	else {
		mata: extract_and_export_labels("`domain'", "`sheet'", 0, 0)
	}

	// Store model statistics
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


* Split full Heckman e(b)/e(V) into outcome and selection equation sub-matrices.
* Stores: b_outcome, V_outcome, b_select, V_select.
* Reads from matrices named b and V (set from e(b) and e(V) before calling).
* Identifies equations by the column stripe: all non-"select" columns go to the
* outcome equation (this includes the IMR/lambda coefficient); "select" columns
* go to the selection equation.
capture program drop split_heckman_matrices
program define split_heckman_matrices

    local k = colsof(b)
    local outcome_cols ""
    local select_cols ""

    forvalues j = 1/`k' {
        local eq : word `j' of `: coleq b'
        if "`eq'" == "select" {
            local select_cols "`select_cols' `j'"
        }
        else {
            local outcome_cols "`outcome_cols' `j'"
        }
    }

    local n_out = wordcount("`outcome_cols'")
    local n_sel = wordcount("`select_cols'")

    // Build b_outcome and V_outcome
    matrix b_outcome = J(1, `n_out', 0)
    local c = 1
    foreach j of local outcome_cols {
        matrix b_outcome[1, `c'] = b[1, `j']
        local ++c
    }

    matrix V_outcome = J(`n_out', `n_out', 0)
    local r = 1
    foreach i of local outcome_cols {
        local c = 1
        foreach j of local outcome_cols {
            matrix V_outcome[`r', `c'] = V[`i', `j']
            local ++c
        }
        local ++r
    }

    // Build b_select and V_select
    matrix b_select = J(1, `n_sel', 0)
    local c = 1
    foreach j of local select_cols {
        matrix b_select[1, `c'] = b[1, `j']
        local ++c
    }

    matrix V_select = J(`n_sel', `n_sel', 0)
    local r = 1
    foreach i of local select_cols {
        local c = 1
        foreach j of local select_cols {
            matrix V_select[`r', `c'] = V[`i', `j']
            local ++c
        }
        local ++r
    }

    display "Split Heckman matrices: `n_out' outcome eq params, `n_sel' selection eq params"

end


* Write a label list to an Excel sheet both vertically (col A from row 2)
* and horizontally (row 1 from col C). Overwrites A1/B1 with standard headers.
capture program drop write_labels_to_excel
program define write_labels_to_excel

    syntax, domain(string) sheet(string) labels(string)

    putexcel set "$dir_results/reg_`domain'", sheet("`sheet'") modify
    putexcel A1 = "REGRESSOR"
    putexcel B1 = "COEFFICIENT"

    // Vertical: A2, A3, ...
    local row = 1
    foreach var of local labels {
        local ++row
        putexcel A`row' = "`var'"
    }

    // Horizontal: C1, D1, ... (offset by 2 since cols A and B are used)
    local col = 2
    foreach var of local labels {
        local ++col
        if `col' <= 26 {
            local letter = char(64 + `col')
            putexcel `letter'1 = "`var'"
        }
        else {
            local first = char(64 + int((`col' - 1) / 26))
            local second = char(65 + mod((`col' - 1), 26))
            putexcel `first'`second'1 = "`var'"
        }
    }

end


* Export one stage of a Heckman model to Excel.
* Caller must set matrices b and V to the relevant stage before calling.
* Trims zeros, checks stability, writes coefs + var-cov, then writes labels.
capture program drop export_heckman_stage
program define export_heckman_stage

    syntax, domain(string) sheet(string) equation(string)

    trim_matrices
    check_matrix_stability

    putexcel set "$dir_results/reg_`domain'", sheet("`sheet'") modify
    putexcel A1 = "REGRESSOR"
    putexcel B1 = "COEFFICIENT"
    write_all_to_excel

    local is_outcome = ("`equation'" == "outcome")
    mata: extract_heckman_stage_labels("`domain'", "`sheet'", `is_outcome')

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

	//Save raw results
	save_raw_results, domain("`domain'") process("`process'") ///
		title("`title'") ifcond("`ifcond'")

	// Save sample for validation
	gen in_sample = e(sample)
	predict p
	save "$dir_validation_data/`process'_sample", replace

	// Export results to Excel
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

	// Clean up
	drop in_sample p
	scalar drop _all
	matrix drop _all

end


* Specialized workflow for ordered logit models
capture program drop process_ologit
program define process_ologit

    syntax, domain(string) process(string) sheet(string) title(string) ///
        gofrow(integer) goflabel(string) [ifcond(string)]

    // Save raw results
    save_raw_results, domain("`domain'") process("`process'") ///
        title("`title'") ifcond("`ifcond'")

    // Save sample for validation
    gen in_sample = e(sample)
    predict p
    save "$dir_validation_data/`process'_sample", replace

    // Export results to Excel
    export_results_to_excel, domain("`domain'") sheet("`sheet'") ologit

    // Export GoF
    export_gof_probit, domain("`domain'") row(`gofrow') label("`goflabel'")

    // Clean up
    drop in_sample p
    scalar drop _all
    matrix drop _all

end


* Specialized workflow for generalized ordered logit models
capture program drop process_gologit
program define process_gologit

    syntax, domain(string) process(string) sheet(string) title(string) ///
        gofrow(integer) goflabel(string) outcomes(integer) [ifcond(string)]
    // Note: outcomes() = total number of categories INCLUDING the base category

    // Save raw results
    matrix results = r(table)
    matrix results = results[1..6,1...]'
    putexcel set "$dir_raw_results/`domain'/`domain'", ///
        sheet("Process `process'") modify
    putexcel A3 = matrix(results), names nformat(number_d2)
    putexcel J4 = matrix(e(V))

    // Save to Word
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

    // Save sample and predictions
    gen in_sample = e(sample)
    local plist ""
    forvalues k = 1/`outcomes' {
        local plist "`plist' p`k'"
    }
    predict `plist'
    save "$dir_validation_data/`process'_sample", replace

    // Store model summary statistics
    scalar r2_p = e(r2_p)
    scalar chi2 = e(chi2)
    scalar ll = e(ll)
    scalar N_sample = e(N)

    // Store estimates in matrices
    matrix b = e(b)
    matrix V = e(V)

    // Raw output
    putexcel set "$dir_results/reg_`domain'", sheet("`sheet'_raw") modify
    putexcel A1 = matrix(b'), names nformat(number_d2)
    putexcel A1 = "CATEGORY"
    putexcel B1 = "REGRESSOR"
    putexcel C1 = "COEFFICIENT"

    // Build gologit structure
    mata: build_gologit_structure(`outcomes')

    // Eigenvalue stability check
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

    // Export matrices to Excel
    putexcel set "$dir_results/reg_`domain'", sheet("`sheet'") modify
    putexcel A1 = "REGRESSOR"
    putexcel B1 = "COEFFICIENT"
    putexcel B2 = matrix(nonzero_b_structure')
    putexcel C2 = matrix(nonzero_var_structure)

    // Extract and export labels using bulk xl() writes (domain passed for file path)
    mata: export_labels_gologit("`domain'", "`sheet'")

    // Goodness of fit
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

    // Clean up
    drop in_sample `plist'
    scalar drop _all
    matrix drop _all

end

* Specialized workflow for Heckman wage model
capture program drop process_heckman
program define process_heckman

    syntax, process(string) ifcond(string) savefile(string) ///
            graphsubtitle(string) ///
            wordfile(string) wordtitle(string) ///
            wordctitle(string) ///
            sheet2(string) sheet1(string) ///
            rmserow(integer)

    // Raw Word output
    capture which outreg2
    if _rc == 0 {
        outreg2 stats(coef se pval) using "`wordfile'", replace ///
            title("`wordtitle'") ctitle("`wordctitle'") ///
            label side dec(2) noparen
    }

    // Stability check on full joint var-cov (captures e() before any matrix work)
    local sigma_val = e(sigma)
    matrix b = e(b)
    matrix V = e(V)
    trim_matrices
    check_matrix_stability

    // Predictions and bias correction (log to level)
    cap drop pred epsilon
    predict pred if `ifcond', ycond
    replace lwage_hour_hat = pred if `ifcond'
    gen in_sample_`process' = e(sample)
    gen epsilon = rnormal() * `sigma_val'
    replace pred_hourly_wage = exp(lwage_hour_hat + epsilon) if `ifcond'

    // Diagnostic histogram
    twoway ///
        (hist wage_hour if `ifcond', width(0.5) lcolor(gs12) fcolor(gs12)) ///
        (hist pred_hourly_wage if `ifcond' & (!missing(wage_hour)), ///
            width(0.5) fcolor(none) lcolor(red)), ///
        title("Gross Hourly Wage (Level)") subtitle("`graphsubtitle'") ///
        xtitle("GBP") legend(lab(1 "UKHLS") lab(2 "Prediction")) ///
        note("Notes: Sample condition `ifcond'", size(vsmall))
    graph export "${dir_raw_results}/wages/`process'_hist.png", replace
    graph drop _all

    sum wage_hour if `ifcond' [aw=${weight}]
    sum pred_hourly_wage if `ifcond' & (!missing(wage_hour)) [aw=$weight]

    // Save validation data
    save "$dir_validation_data/`savefile'", replace
    cap drop pred epsilon

    // Split and export results to Excel
    matrix b = e(b)
    matrix V = e(V)
    split_heckman_matrices

    // Second stage, reg_wages
    matrix b = b_outcome
    matrix V = V_outcome
    export_heckman_stage, domain("wages") sheet("`sheet2'") equation("outcome")

    // First stage, reg_employment_selection
    matrix b = b_select
    matrix V = V_select
    export_heckman_stage, domain("employment_selection") sheet("`sheet1'") ///
		equation("select")

    // RMSE
    cap drop residuals squared_residuals
    gen residuals = lwage_hour - lwage_hour_hat
    gen squared_residuals = residuals^2
    preserve
    keep if `ifcond'
    sum squared_residuals
    local rmse = sqrt(r(mean))
    di "RMSE for `process': " `rmse'
    putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
    putexcel A1 = "REGRESSOR"
    putexcel B1 = "COEFFICIENT"
    putexcel A`rmserow' = "`process'"
    putexcel B`rmserow' = `rmse'
    restore
    cap drop residuals squared_residuals

    cap drop lambda
    scalar drop _all
    matrix drop _all

end
