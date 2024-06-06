********************************************************************************
* PROJECT:  		INAPP
* SECTION:			Leaving Parental Home
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
log using "${dir_log}/reg_leaveParentalHome.log", replace
*******************************************************************

use "$dir_ukhls_data/ukhls_pooled_all_obs_09.dta", clear

/*DP: note that the categories in les_c4 used by Cara are different from the ones currently used 
so the categories in the corresponsing Excel file were updated */

*Labeling and formating variables

label define jbg 1 "Employed" 2 "Student" 3 "Not employed" 4 "Retired"

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
label variable les_c4 "Employment Status: 4 Category" 
label variable dhe "Self-rated Health"
label variable deh_c3 "Educational Attainment: 3 Category"
label variable ydses_c5 "Annual Household Income Quintile" 
label variable dlltsd "Long-term Sick or Disabled"

label value dgn gdr
label value drgn1 rgna
label value dhhtp_c4 hht 
label value les_c4 jbg 
label value deh_c3 edd 
label value ded yn


drop if dag < 16
replace stm = stm - 2000


/*check if all covariates are available in the data*/ 
recode dlftphm dgn dag dagsq deh_c3 les_c4 les_c3 ydses_c5 drgn1 stm (-9=.) 

xtset idperson swv


************************************
*Process LPH1: Leave Parental Home *
************************************
*Process P1a: Probability of leaving the parental home. Sample: All non-student respondents living with a parent.
*Or Probability of leaving the parental home for those who have left education. (Students stay in the parental home).

*sample: All non-student respondents aged 18+ who lived with a parent at t-1
fre dlftphm if (ded==0 & dag>=18 & l.dlftphm==0) 

probit dlftphm i.dgn dag dagsq ib1.deh_c3 li.les_c3 li.ydses_c5 ib8.drgn1 stm if (ded==0 & dag>=18 & l.dlftphm==0) [pweight=disclwt], vce(robust)
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_data/leave_parent_home", sheet("Process P1a male grads") replace
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))
outreg2 stats(coef se pval) using "$dir_data/P1a.doc", replace ///
title("Process P1a: Probability of leaving the parental home. Sample: All non-student respondents living with a parent.") ///
 ctitle(Leave parental home) label side dec(2) noparen addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll))

 
capture log close 
