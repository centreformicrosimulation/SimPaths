********************************************************************************
* PROJECT:  		ESPON
* SECTION:			Home ownership 
* OBJECT: 			Final Regresion Models - Weighted
* AUTHORS:			Daria Popova, Justin van de Ven
* LAST UPDATE:		21/04/2024 (JV)
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
global dir_work "C:\MyFiles\99 DEV ENV\JAS-MINE\data work\regression_estimates"

* Directory which contains do files
global dir_do "${dir_work}/do"

* Directory which contains data files 
global dir_data "${dir_work}/data"

* Directory which contains log files 
global dir_log "${dir_work}/log"

* Directory which contains pooled UKHLS dataset 
global dir_ukhls_data "C:\MyFiles\99 DEV ENV\JAS-MINE\data work\initial_populations\data"


*******************************************************************
cap log close 
log using "${dir_log}/reg_home_ownership.log", replace
*******************************************************************

use "$dir_ukhls_data/ukhls_pooled_all_obs_09.dta", clear

*Labeling and formating variables
label define jbf 1 "Employed" 2 "Student" 3 "Not Employed"

label define edd 1 "Degree"	2 "Other Higher/A-level/GCSE" 3 "Other/No Qualification"

label define gdr 1  "Male" 0 "Female"
			
label define rgna 1 "North East" 2 "North West" 4 "Yorkshire and the Humber" 5 "East Midlands" ///
6 "West Midlands" 7 "East of England" 8 "London" 9 "South East" 10 "South West" 11 "Wales" ///
12 "Scotland" 13 "Northern Ireland"
			
label define yn	1 "Yes" 0 "No"

label define hht 1 "Couples with No Children" 2 "Couples with Children" ///
				3 "Single with No Children" 4 "Single with Children"

label variable dgn "Gender"
label variable dag "Age"
label variable dagsq "Age Squared"
label variable drgn1 "Region"
label variable stm "Year"
label variable les_c3 "Employment Status: 5 Category" 
label variable dhe "Self-rated Health"
label variable deh_c3 "Educational Attainment: 3 Category"
label variable dhhtp_c4 "Household Type: 4 Category"

label value dgn gdr
label value drgn1 rgna
label value les_c3 lessp_c3 jbf 
label value deh_c3 dehsp_c3 edd 
label value dcpen dcpex dlrtrd yn
label value dhhtp_c4 hht

drop if dag < 16
replace stm = stm - 2000


*check if all covariates are available and recode missing values 
recode dhh_owned dgn dag dagsq les_c3 deh_c3 dhe yptciihs_dv ydses_c5 drgn1 dhhtp_c4 lessp_c3 stm (-9=.)

xtset idperson swv


*************************************************
*Process HO1: Probability of being a homeowner. *
*************************************************
*Sample: Individuals aged 16 and above.

probit dhh_owned dgn dag dagsq il.dhhtp_c4 il.les_c3 il.lessp_c3 i.deh_c3 il.dhe il.ydses_c5 l.yptciihs_dv ib8.drgn1 stm if dag>=16 [pweight=disclwt], vce(cluster idperson)
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_data/homeownership", sheet("Process HO1a") replace
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))
outreg2 stats(coef se pval) using "$dir_data/U1a.doc", replace ///
title("Process HO1a: Probability of being a home owner - people aged 16+") ///
 ctitle(home owner) label side dec(2) noparen addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll))


capture log close 
