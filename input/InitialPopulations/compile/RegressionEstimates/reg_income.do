********************************************************************************
* PROJECT:  		SimPaths UK
* SECTION:			Non-employment/non-benefit income
* OBJECT: 			Final Regresion Models 
* AUTHORS:			Patryk Bronka, Daria Popova, Justin van de Ven
* LAST UPDATE:		21 Jan 2026 DP   
* COUNTRY: 			UK

* NOTES: 			 Models for split income variable
*                    - Capital returns
*                    - Private pension income  
*                       
*                       The income  do file must be run after
* 						reg_wages.do because it uses predicted wages. 
/*******************************************************************************


*******************************************************************************/
********************************************************************************
clear all
set more off
set mem 200m
set type double
//set maxvar 120000
set maxvar 30000

*******************************************************************
cap log close 
log using "${dir_log}/reg_income.log", replace
*******************************************************************

* Set Excel file 
* Info sheet
putexcel set "$dir_results/reg_income", sheet("Info") replace
putexcel A1 = "Description:"
putexcel B1 = "This file contains regression estiamtes used by processes I1 (capital income), I2 (private pension, retired last year), I3 (private pension income, not retired last year) "
putexcel A2 = "Authors:	Patryk Bronka, Justin Van de Ven, Daria Popova, Aleksandra Kolndrekaj" 
putexcel A3 = "Last edit: 18 Feb 2026 AK"

putexcel A4 = "Process:", bold
putexcel B4 = "Description:", bold

putexcel A6 = "Process I1a"
putexcel B6 = "Logit regression estimates of the probability of receiving capital income "

putexcel A8 = "Process I1b"
putexcel B8 = "OLS regression estimates (ihs) capital income amount -  who receive capital income"

putexcel A10 = "Process I2b"
putexcel B10 = "OLS regression estimates (ihs) private pension income amount - aged 50+ and were retired last yeare"

putexcel A12 = "Process I3a"
putexcel B12 = "Logit regression estimates of the probability of receiving private pension income - aged 50+ and not a student or retired last year"

putexcel A14 = "Process I3b"
putexcel B14 = "OLS regression estimates (ihs) private pension income - aged 50+ and not a student or retired last year"


putexcel A17 = "Notes:", bold
putexcel B17 = "Estimation sample: UK_ipop2.dta with grossing up weight dwt" 
putexcel B18 = "Conditions for processes are defined as globals in master.do"
putexcel B19 = "Combined former capital income processes I3a and I3b and renamed as I1a and I1b"
putexcel B20 = "Income variables are IHS transformed."


/**************************************************************/
*	prepare data on real growth of wages 
/**************************************************************/

import excel "$dir_external_data/time_series_factor.xlsx", sheet("UK_gdp") firstrow clear // Import real growth index
rename Year stm
rename Value growth
gen base_val = growth if stm == 2015
sum base_val
replace base_val = r(mean)
replace growth= growth/base_val
drop base_val
replace stm = stm - 2000
save "$dir_external_data\growth_rates", replace


/********************************* PREPARE DATA *******************************/

* Load data 
use "${estimation_sample2}", clear //panel with predicted wages 

* Merge in growth rates 
merge m:1 stm using "$dir_external_data/growth_rates", keep(3) nogen keepusing(growth)

* Set data 
xtset idperson swv
sort idperson swv 

* adjust capital income 
sum ypncp, det
scalar p99 = r(p99)
replace ypncp = . if ypncp >= p99

* adjust pension income 
sum ypnoab, det
scalar p99 = r(p99)
replace ypnoab = . if ypnoab >= p99

*rename pedicted wage 
capture confirm variable pred_hourly_wage
if _rc == 0 {
    gen Hourly_wage = pred_hourly_wage
}

/********************************** ESTIMATION ********************************/

/*************** I1a: PROBABILITY OF RECEIVEING CAPITAL INCOME ****************/

display "${i1a_if_condition}"

logit receives_ypncp ///
    i.Ded i.Dgn c.Dag c.Dag_sq ///
	l.Dhe_pcs l.Dhe_mcs ///
	lc.Ypncp lc.Yplgrs_dv ///
	l2c.Yplgrs_dv l2c.Ypncp ///
	Ded_Dgn /*Ded_Dag Ded_Dag_sq*/ ///
	l.Ded_Dhe_pcs l.Ded_Dhe_mcs ///
	l.Ded_Ypncp l.Ded_Yplgrs_dv l2.Ded_Yplgrs_dv l2.Ded_Ypncp ///
	i.Deh_c4_Low i.Deh_c4_Medium i.Deh_c4_High ///
	li.Les_c4_Student li.Les_c4_NotEmployed li.Les_c4_Retired ///
	li.Dhhtp_c4_CoupleChildren li.Dhhtp_c4_SingleNoChildren li.Dhhtp_c4_SingleChildren ///
	$regions Year_transformed Y2020 Y2021 $ethnicity if ///
	 ${i1a_if_condition} [pweight = dwt], ///
	 vce(cluster idperson) base

 
* Save raw results 
matrix results = r(table)
matrix results = results[1..6,1...]'

putexcel set "$dir_raw_results/income/income", ///
	sheet("Process I1") replace
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))

outreg2 stats(coef se pval) using ///
	"$dir_raw_results/income/Selection_I1a.doc", replace ///
title("Process I1a: Probability Receiving Capital Income") ///
	ctitle(Receives capital income) label side dec(2) noparen ///
	addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll)) ///
	addnote(`"Note: Regression if condition = (${i1a_if_condition})"')		 
	
	
* Save sample inclusion indicator and predicted probabilities	
cap drop in_sample
cap drop p
gen in_sample = e(sample)	
predict p

* Save sample for estimates validation
save "$dir_validation_data/I1_selection_sample", replace

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
putexcel set "$dir_results/reg_income", sheet("I1a") modify
putexcel B2 = matrix(b_trimmed)
putexcel C2 = matrix(V_trimmed)


* Labels 
preserve 

putexcel set "$dir_results/reg_income", sheet("I1a") modify

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
    putexcel set "$dir_results/reg_income", sheet("I1a") modify 	
	
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
putexcel set "$dir_results/reg_income", sheet("Gof") modify

putexcel A3 = ///
	"I1a - Receiving capital income ", ///
	bold		

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

 	

/********************** I1b: AMOUNT OF CAPITAL INCOME *************************/

* DV: ypncp = Inverse hyperbolic sine (IHS) of gross capital income
display "${i1b_if_condition}"

reg ypncp i.Dgn c.Dag c.Dag_sq ///
          i.Deh_c4_Low i.Deh_c4_Medium i.Deh_c4_High /// 
          li.Les_c4_Student li.Les_c4_NotEmployed li.Les_c4_Retired /// 
          li.Dhhtp_c4_CoupleChildren li.Dhhtp_c4_SingleNoChildren li.Dhhtp_c4_SingleChildren ///
		  l.Dhe_pcs l.Dhe_mcs ///
		  lc.Ypncp l2c.Ypncp lc.Yplgrs_dv l2c.Yplgrs_dv  ///
          Ded_Dgn /*Ded_Dag Ded_Dag_sq*/ ///
		  l.Ded_Ypncp l.Ded_Yplgrs_dv l2.Ded_Yplgrs_dv l2.Ded_Ypncp ///
		  $regions Year_transformed Y2020 Y2021 $ethnicity ///
		  if ${i1b_if_condition} [pw=dwt], vce(cluster idperson)

		  
	* Save raw results 
matrix results = r(table)
matrix results = results[1..6,1...]'

putexcel set "$dir_raw_results/income/income", sheet("Process I1b") modify
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))

outreg2 stats(coef se pval) using ///
	"$dir_raw_results/income/Amount_I1b.doc", replace ///
title("Process I1b: Capital Income Amount") ///
	ctitle(Capital amount) label side dec(2) noparen ///
	addstat("R2", e(r2)) ///
	addnote(`"Note: Regression if condition = (${i1b_if_condition})"')	
	
	
* Save sample inclusion indicator and predicted probabilities	
cap drop in_sample  
cap drop p
gen in_sample = e(sample)	
predict p
cap drop sigma
gen sigma = e(rmse)

* Save sample for estimate validation
save "$dir_validation_data/I1_level_sample", replace

* Store model summary statistics
scalar r2 = e(r2) 
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
putexcel set "$dir_results/reg_income", sheet("I1b") modify
putexcel B2 = matrix(b_trimmed)
putexcel C2 = matrix(V_trimmed)


* Labels 
preserve 

putexcel set "$dir_results/reg_income", sheet("I1b") modify

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
    putexcel set "$dir_results/reg_income", sheet("I1b") modify 	
	
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



* Calculate RMSE
cap drop residuals squared_residuals  
predict  residuals , residuals
gen squared_residuals = residuals^2

preserve 
keep if receives_ypncp == 1
sum squared_residuals [w = dwt]
di "RMSE for Amount of capital income" sqrt(r(mean))
putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A6 = ("I1b") B6 = (sqrt(r(mean))) 
restore 

* Export model fit statistics
putexcel set "$dir_results/reg_income", sheet("Gof") modify

putexcel A9 = "I1b - Capital income amount", ///
	bold		
	
putexcel A11 = "R-squared" 
putexcel B11 = r2 
putexcel A12 = "N"
putexcel B12 = N_sample 
		
* Clean up 		
drop in_sample p
scalar drop _all
matrix drop _all


		  
/******************************* I2b: Amount of pension income *********************************************/		  

*Sample: Retired individuals who were retired in the previous year. 
*ypnoab = Inverse hyperbolic sine transformation of Gross personal private pension income

display "${i2b_if_condition}"

reg ypnoab i.Dgn c.Dag ///
           i.Deh_c4_High i.Deh_c4_Medium  i.Deh_c4_Na /// 
          li.Dhhtp_c4_CoupleChildren li.Dhhtp_c4_SingleNoChildren li.Dhhtp_c4_SingleChildren ///
          l.Dhe_pcs l.Dhe_mcs ///
		  lc.Ypnoab l2c.Ypnoab ///
          $regions Year_transformed Y2020 Y2021 $ethnicity ///
		  if ${i2b_if_condition} [pw=dwt], vce(cluster idperson)

	* Save raw results 
matrix results = r(table)
matrix results = results[1..6,1...]'

putexcel set "$dir_raw_results/income/income", sheet("Process I2b") modify
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))

outreg2 stats(coef se pval) using ///
	"$dir_raw_results/income/Amount_I2b.doc", replace ///
title("Process I2b: Capital Income Amount") ///
	ctitle(Private Pension Income amount) label side dec(2) noparen ///
	addstat("R2", e(r2)) ///
	addnote(`"Note: Regression if condition = (${i2b_if_condition})"')	
	
	
* Save sample inclusion indicator and predicted probabilities	  
cap drop in_sample
cap drop p
gen in_sample = e(sample)	
predict p
cap drop sigma
gen sigma = e(rmse)

* Save sample for estimate validation
save "$dir_validation_data/I2_level_sample", replace

* Store model summary statistics
scalar r2 = e(r2) 
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
putexcel set "$dir_results/reg_income", sheet("I2b") modify
putexcel B2 = matrix(b_trimmed)
putexcel C2 = matrix(V_trimmed)


* Labels 
preserve 

putexcel set "$dir_results/reg_income", sheet("I2b") modify

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
    putexcel set "$dir_results/reg_income", sheet("I2b") modify 	
	
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


* Calculate RMSE
cap drop residuals squared_residuals  
predict  residuals , residuals
gen squared_residuals = residuals^2

preserve 
keep if receives_ypncp == 1
sum squared_residuals [w = dwt]
di "RMSE for Amount of private pension income" sqrt(r(mean))
putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A7 = ("I2b") B7 = (sqrt(r(mean))) 
restore 

* Export model fit statistics
putexcel set "$dir_results/reg_income", sheet("Gof") modify

putexcel A15 = ///
	"I2b - Private Pension income amount", ///
	bold		
	
putexcel A17 = "R-squared" 
putexcel B17 = r2 
putexcel A18 = "N"
putexcel B18 = N_sample 
		
* Clean up 		
drop in_sample p
scalar drop _all
matrix drop _all

		  
/*************************** I3a: PROBABILITY OF RECEIVEING PRIVATE PENSION INCOME ***********************************/
*Sample: Retired individuals who were not retired in the previous year.

display "${i3a_if_condition}"

logit receives_ypnoab ///
    i.Dgn i.Reached_Retirement_Age ///
	 i.Deh_c4_High i.Deh_c4_Medium i.Deh_c4_Na ///
	li.Les_c4_NotEmployed  ///
	li.Dhhtp_c4_CoupleChildren li.Dhhtp_c4_SingleNoChildren li.Dhhtp_c4_SingleChildren ///
	l.Dhe_pcs l.Dhe_mcs ///
	l.Hourly_wage ///
	$regions Year_transformed Y2020 Y2021 $ethnicity if ///
	${i3a_if_condition} [pweight = dwt], vce(cluster idperson) base
	
	
* Save raw results 
matrix results = r(table)
matrix results = results[1..6,1...]'

putexcel set "$dir_raw_results/income/income", ///
	sheet("Pension Income selection") replace
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))

outreg2 stats(coef se pval) using ///
	"$dir_raw_results/income/Selection_I3a.doc", replace ///
    title("Process I3a: Probability Receiving Private Pension Income") ///
	ctitle(Receives private pesnion income) label side dec(2) noparen ///
	addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll)) ///
	addnote(`"Note: Regression if condition = (${i3a_if_condition})"')		 

* Save sample inclusion indicator and predicted probabilities	
cap drop in_sample
cap drop p
gen in_sample = e(sample)	
predict p

* Save sample for estimates validation
save "$dir_validation_data/I3_selection_sample", replace

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
putexcel set "$dir_results/reg_income", sheet("I3a") modify
putexcel B2 = matrix(b_trimmed)
putexcel C2 = matrix(V_trimmed)


* Labels 
preserve 

putexcel set "$dir_results/reg_income", sheet("I3a") modify

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
    putexcel set "$dir_results/reg_income", sheet("I3a") modify 	
	
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
putexcel set "$dir_results/reg_income", sheet("Gof") modify

putexcel A21 = ///
	"I3a - Receiving private pension income", ///
	bold		
putexcel A23 = "Pseudo R-squared" 
putexcel B23 = r2_p 
putexcel A24 = "N"
putexcel B24 = N_sample 
putexcel E23 = "Chi^2"		
putexcel F23 = chi2
putexcel E24 = "Log likelihood"		
putexcel F24 = ll			

	
* Clean up 		
drop in_sample p
scalar drop _all
matrix drop _all


/***************************** I3b: Amount of pension income ********************************************/		  

*Sample: Retired individuals who were not retired in the previous year.
*ypnoab = Inverse hyperbolic sine transformation of Gross personal private pension income

display "${i3b_if_condition}"

reg ypnoab i.Dgn c.Dag ///
           i.Deh_c4_High i.Deh_c4_Medium i.Deh_c4_Na /// 
          li.Les_c4_NotEmployed ///
          li.Dhhtp_c4_CoupleChildren li.Dhhtp_c4_SingleNoChildren li.Dhhtp_c4_SingleChildren ///
          l.Dhe_pcs l.Dhe_mcs ///
		  l.Hourly_wage ///
		  $regions Year_transformed Y2020 Y2021 $ethnicity ///
		  if ${i3b_if_condition} [pw=dwt], vce(cluster idperson)
		  
 
	* Save raw results 
matrix results = r(table)
matrix results = results[1..6,1...]'

putexcel set "$dir_raw_results/income/income", sheet("Process I3b") modify
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))

outreg2 stats(coef se pval) using ///
	"$dir_raw_results/income/Amount_I3b.doc", replace ///
title("Process I3b: Private Pension Income Amount") ///
	ctitle(Private Pension Income amount) label side dec(2) noparen ///
	addstat("R2", e(r2)) ///
	addnote(`"Note: Regression if condition = (${i3b_if_condition})"')	
	
	
* Save sample inclusion indicator and predicted probabilities	 
cap drop in_sample 
cap drop p
gen in_sample = e(sample)	
predict p
cap drop sigma
gen sigma = e(rmse)

* Save sample for estimate validation
save "$dir_validation_data/I3_level_sample", replace

* Store model summary statistics
scalar r2 = e(r2) 
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
putexcel set "$dir_results/reg_income", sheet("I3b") modify
putexcel B2 = matrix(b_trimmed)
putexcel C2 = matrix(V_trimmed)


* Labels 
preserve 

putexcel set "$dir_results/reg_income", sheet("I3b") modify

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
    putexcel set "$dir_results/reg_income", sheet("I3b") modify 	
	
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


* Calculate RMSE
cap drop residuals squared_residuals  
predict  residuals , residuals
gen squared_residuals = residuals^2

preserve 
keep if receives_ypncp == 1
sum squared_residuals [w = dwt]
di "RMSE for Amount of private pension income" sqrt(r(mean))
putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A8 = ("I3b") B8 = (sqrt(r(mean))) 
restore 

* Export model fit statistics
putexcel set "$dir_results/reg_income", sheet("Gof") modify

putexcel A27 = ///
	"I3b - Private Pension income amount", ///
	bold		
	
putexcel A28 = "R-squared" 
putexcel B28 = r2 
putexcel A29 = "N"
putexcel B29 = N_sample 

		
* Clean up 		
drop in_sample p
scalar drop _all
matrix drop _all


//end 

capture log close 
