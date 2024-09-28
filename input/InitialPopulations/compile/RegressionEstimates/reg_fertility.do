********************************************************************************
* PROJECT:  		ESPON 
* SECTION:			Fertility
* OBJECT: 			Final Probit Models
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
log using "${dir_log}/reg_fertility.log", replace
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
label variable dnc "Number of Children in Household"
label variable dnc02 "Number of Children aged 0-2 in Household"
label variable ydses_c5 "Annual Household Income Quintile" 
label variable dukfr "UK Fertility Rate"

label value dgn gdr
label value drgn1 rgna
label value dhhtp_c4 hht 
label value les_c3 jbf 
label value deh_c3 edd 
label value ded yn

drop if dag < 16
replace stm = stm - 2000

/*check if all covariates are available in the data*/ 
recode dhe dnc dnc02 deh_c3 les_c3 ydses_c5 dcpst drgn1 sprfm scedsmpl dukfr    (-9=. )
recode dchpd (-9=0)

xtset idperson swv


**********************************************************************
*Proces F1a - Probability of Having a Child - In continuous education
**********************************************************************
*Sample: Women aged 18-44 not in continuous education.
probit dchpd dag l.dnc il.dnc02 ib1.dcpst if (sprfm==1 & scedsmpl==1) [pweight=disclwt], vce(robust)
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_data/Fertility_w", sheet("Process F1a - In education") replace
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))
outreg2 stats(coef se pval) using "$dir_data/F1a.doc", replace ///
title("Process F1a: Probability of giving birth to a child. Sample: Women aged 18-44 in continuous education.") ///
 ctitle(Giving birth) label side dec(2) noparen addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll))


************************************************************************
*Proces F1b Probability of Having a Child - Not in continuous education
*************************************************************************
*Sample: Women aged 18-44 not in continuous education.
gen ddnc02 = (dnc02 > 0)
probit dchpd dag dagsq l.dnc l.ddnc02 ib1.dhe ib1.dcpst dukfr li.les_c3 ib8.drgn1 if (sprfm==1 & scedsmpl==0) [pweight=disclwt], vce(robust)

matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_data/Fertility_w", sheet("Process F1b - Not in education") modify
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))

outreg2 stats(coef se pval) using "$dir_data/F1b.doc", replace ///
title("Process F1b: Probability of giving birth to a child. Sample: Women aged 18-44 not in continuous education.") ///
 ctitle(Giving birth) label side dec(2) noparen addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll))


 
capture log close 




