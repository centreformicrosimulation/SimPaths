********************************************************************************
* PROJECT:  		ESPON 
* SECTION:			Health
* OBJECT: 			Final Probit and Linear Regression Models - Weighted
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
log using "${dir_log}/reg_health.log", replace
*******************************************************************

use "$dir_ukhls_data/ukhls_pooled_all_obs_09.dta", clear


*Labeling and formating variables
label define jbf 1 "Employed" 2 "Student" 3 "Not Employed"

label define edd 1 "Degree"	2 "Other Higher/A-level/GCSE" 3 "Other/No Qualification"

label define hht 1 "Couples with No Children" 2 "Couples with Children" ///
				3 "Single with No Children" 4 "Single with Children"
			
label define gdr 1  "Male" 0 "Female"
				
label define rgna 1 "North East" 2 "North West" 4 "Yorkshire and the Humber" 5 "East Midlands" ///
6 "West Midlands" 7 "East of England" 8 "London" 9 "South East" 10 "South West" 11 "Wales" ///
12 "Scotland" 13 "Northern Ireland"
			
label define yn	1 "Yes" 0 "No"

label variable dgn "Gender"
label variable dag "Age"
label variable dagsq "Age Squared"
label variable drgn1 "Region"
label variable dhhtp_c4 "Household Type: 4 Category"
label variable stm "Year"
label variable les_c3 "Employment Status: 3 Category" 
label variable dhe "Self-rated Health"
label variable deh_c3 "Educational Attainment: 3 Category"
label variable ydses_c5 "Annual Household Income Quintile" 
label variable dlltsd "Long-term Sick or Disabled"

label value dgn gdr
label value drgn1 rgna
label value dhhtp_c4 hht 
label value les_c3 jbf 
label value deh_c3 edd 
label value ded yn


drop if dag < 16
replace stm = stm - 2000

/*check if all covariates are available in the data*/ 
recode dhe  deh_c3 les_c3 ydses_c5 dhhtp_c4 drgn1 stm  (0= .) (-9=. ) 
recode dgn dag dagsq (-9=.)

xtset idperson swv


**********************************
*Process 1a: Those in education  *
**********************************
*
*Self-rated health status for those in continuous education.
*sample: 16-29 year olds who have always been in education without a break
fre dhe if (dag>=16 & dag<=29 & ded==1 )

/*
regress dhe i.dgn dag dagsq li.ydses_c5 l.dhe ib8.drgn1 stm if scedsmpl==1 [pweight=disclwt], vce(robust)
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_data/health.xlsx", sheet("Process H1a - Self-rated Health") replace
putexcel A1 = matrix(results), names nformat(number_d2) 
putexcel A1 = matrix(results), names nformat(number_d2)
*/

* Ordered probit models to replace linear regression 
oprobit dhe i.dgn dag dagsq li.ydses_c5 ilb5.dhe ib8.drgn1 stm if (dag>=16 & dag<=29 & ded==1) [pweight=disclwt], vce(robust)
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_data/health", sheet("Process H1a") replace
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))
outreg2 stats(coef se pval) using "$dir_data/H1a.doc", replace ///
title("Process H1a: Ordered probit regression estimates of self reported health status - individuals aged 16-29 in continuous education") ///
 ctitle(health status) label side dec(2) noparen addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll))


****************************************
*Process 1b: Those in not in education *
****************************************
*
*Self-rated health status for those not in continuous education (out of education or returned having left education in the past).
*sample: 16 or older who are not in continuous education
fre dhe if (dag>=16 & ded==0 )
/*
regress dhe i.dgn dag dagsq ib1.deh_c3 li.les_c3 li.ydses_c5 l.dhe lib1.dhhtp_c4 ib8.drgn1 stm if scedsmpl==0 [pweight=disclwt], vce(robust)
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set health, sheet("Process H1b - Not in education") modify
putexcel A1 = matrix(results), names nformat(number_d2) 
*/

* Ordered probit models to replace linear regression 
oprobit dhe i.dgn dag dagsq ib1.deh_c3 li.les_c3 li.ydses_c5 ilb5.dhe lib1.dhhtp_c4 ib8.drgn1 stm if (dag>=16 & ded==0)  [pweight=disclwt], vce(robust)
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_data/health", sheet("Process H1b") modify
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))
outreg2 stats(coef se pval) using "$dir_data/H1b.doc", replace ///
title("Process H1b: Ordered probit regression estimates of self reported health status - individuals aged 16+ not in continuous education") ///
 ctitle(health status) label side dec(2) noparen addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll))

 
**********************************************************************************************
*Process 2b: Probability of being long-term sick or disabled amongst those not in education  *
**********************************************************************************************
*
*Probability of becoming long-term sick or disabled for those not in continuous education.
*sample: 16 or older who are not in continuous education
fre dhe if (dag>=16 & ded==0 )

probit dlltsd i.dgn dag dagsq ib1.deh_c3 li.ydses_c5 ib5.dhe ilb5.dhe l.dlltsd lib1.dhhtp_c4 ib8.drgn1 stm if (dag>=16 & ded==0 & dag<56) [pweight=disclwt], vce(robust)
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_data/health", sheet("Process H2b") modify
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))
outreg2 stats(coef se pval) using "$dir_data/H2b.doc", replace ///
title("Process H2b: Probit regression estimates for being long-term sick or disabled - people aged 16+ not in continuous education") ///
 ctitle(long-term sick or disabled) label side dec(2) noparen addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll))


 
capture log close 
