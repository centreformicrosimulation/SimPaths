********************************************************************************
* PROJECT:  		ESPON 
* SECTION:			Education
* OBJECT: 			Final Probit Models - Weighted
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
log using "${dir_log}/reg_education.log", replace
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
label variable les_c3 "Employment Status: 3 Category" 
label variable deh_c3 "Educational Attainment: 3 Category"
/*
label variable dehm_c3 "Mother's Educational Attainment: 3 Category"
label variable dehf_c3 "Father's Educational Attainment: 3 Category"
*/
label variable dehmf_c3 "Highest Parental Educational Attainment: 3 Category"
label variable dhhtp_c4 "Household Type: 4 Category"
label variable dnc "Number of Children in Household"
label variable dnc02 "Number of Children aged 0-2 in Household"

label value dgn gdr
label value drgn1 rgna
label value les_c3 jbf 
label value deh_c3 dehmf_c3  /*dehm_c3 dehf_c3*/ edd 
label value ded yn
label value dhhtp_c4 hht

drop if dag < 16

replace stm = stm - 2000
fre stm 

/*check if all covariates are available in the data*/ 
recode ded dgn dag dagsq dehmf_c3 drgn1 stm deh_c3 les_c3 (-9=.) 


xtset idperson swv


**********************************
*Probability of Being a Student  *
**********************************
*Process E1a: Probability of being in education. Sample: Individuals aged 16-29 in continuous education.
*or probability of remaining in education for those who have always been in education without interruptions.

*sample: Individuals aged 16-29 in continuous education.	
fre ded if (dag>=16 & dag<=29 & l.ded==1) /*was in continious education in the previous wave  */

probit ded i.dgn dag dagsq ib1.dehmf_c3 /*ib1.dehm_c3 ib1.dehf_c3*/ ib8.drgn1 stm if (dag>=16 & dag<=29 & l.ded==1) [pweight=dimxwt], vce(robust)
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_data/education", sheet("Process E1a") replace
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))
outreg2 stats(coef se pval) using "$dir_data/E1a.doc", replace ///
title("Process E1a: Probability of remaining in continuous education - individuals aged 16-29 in continuous education.") ///
 ctitle(Continuing student) label side dec(2) noparen addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll))


****************************************
*Probability of Returning to education *
****************************************
*Process E1b: Probability of being in education. Sample: Individuals aged 16-35 not in continuous education.
*Or probability of returning to education for those who had left school.

*sample: Individuals aged 16-35 not in continuous education. 
fre der if (dag>=16 & dag<=35 & ded==0) 

probit der i.dgn dag dagsq lib1.deh_c3 li.les_c3 l.dnc l.dnc02 ib1.dehmf_c3 /*ib1.dehm_c3 ib1.dehf_c3*/ ib8.drgn1 stm if (dag>=16 & dag<=35 & ded==0)  [pweight=dimlwt], vce(robust)
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_data/education", sheet("Process E1b") modify
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))
outreg2 stats(coef se pval) using "$dir_data/E1b.doc", replace ///
title("Process E1b: Probability of returning to education - individuals aged 16-35 not in continuous education.") ///
 ctitle(Returning student) label side dec(2) noparen addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll))


********************************************
*Educational level after leaving education *
********************************************
*Process E2: Educational attainment. Sample: Respondents from Process 1a who have left education.
*Or Level of education for those leaving education.

*sample: Individuals aged 16-29 who were in continuous education and left it. 
fre deh_c3 if (dag>=16 & dag<=29) & l.ded==1 & ded==0

/*
mprobit deh_c3 i.dgn dag dagsq ib1.dehm_c3 ib1.dehf_c3 ib8.drgn1 stm if sedcsmpl==1 [pweight=dimxwt], vce(robust)
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_data/education.xlsx", sheet("Process E2 - Education Level") modify
putexcel A1 = matrix(results), names nformat(number_d2) 

mprobit deh_c3 i.dgn dag dagsq ib1.dehm_c3 ib1.dehf_c3 ib8.drgn1 stm if sedcsmpl==1 [pweight=dimxwt], vce(robust)
matrix e2=get(VCE)
matrix list e2
putexcel set "$dir_data/edu_vcm.xlsx", sheet("Process E2 - Education Level") modify
putexcel A1 = matrix(e2), names 

//capture log close
*/


/*******************************************************************************
* Ordered probit model to replace multinomial probit E2a
*******************************************************************************/

*1. Recode education level (outcome variable) so 1 = Low education, 2 = Medium education, 3 = High education
recode deh_c3 ///
	(1 = 3) ///
	(3 = 1) ///
	, gen(deh_c3_recoded)
	
la def deh_c3_recoded 1 "Low" 2 "Medium" 3 "High"
la val deh_c3_recoded deh_c3_recoded

//oprobit deh_c3_recoded i.dgn dag dagsq ib1.dehm_c3 ib1.dehf_c3 ib8.drgn1 stm if (dag>=16 & ded == 0) [pweight=dimxwt], vce(robust)
oprobit deh_c3_recoded i.dgn dag dagsq ib1.dehmf_c3 /*ib1.dehm_c3 ib1.dehf_c3*/ ib8.drgn1 stm if (dag>=16 & dag<=29 & l.ded==1 & ded==0) [pweight=dimxwt], vce(robust)
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_data/education", sheet("Process E2a") modify
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))
outreg2 stats(coef se pval) using "$dir_data/E2a.doc", replace ///
title("Process E2a: Ordered probit for educational attainment - individuals aged 16-29 exiting education.") ///
 ctitle(Education attainment) label side dec(2) noparen addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll))


capture log close
