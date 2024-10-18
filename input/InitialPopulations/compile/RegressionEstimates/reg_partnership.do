********************************************************************************
* PROJECT:  		ESPON 
* SECTION:			Unions
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
log using "${dir_log}/reg_partnership.log", replace
*******************************************************************

use "$dir_ukhls_data/ukhls_pooled_all_obs_09.dta", clear

cap gen ypnbihs_dv_sq = ypnbihs_dv^2

*Labeling and formating variables
label define jbf 1 "Employed" 2 "Student" 3 "Not Employed"

label define edd 1 "Degree"	2 "Other Higher/A-level/GCSE" 3 "Other/No Qualification"

label define gdr 1  "Male" 0 "Female"
			
label define rgna 1 "North East" 2 "North West" 4 "Yorkshire and the Humber" 5 "East Midlands" ///
6 "West Midlands" 7 "East of England" 8 "London" 9 "South East" 10 "South West" 11 "Wales" ///
12 "Scotland" 13 "Northern Ireland"
			
label define yn	1 "Yes" 0 "No"

label define dces 1 "Both Employed" 2 "Employed, Spouse Not Employed" 3 "Not Employed, Spouse Employed" 4 "Both Not Employed"

label define hht 1 "Couples with No Children" 2 "Couples with Children" ///
				3 "Single with No Children" 4 "Single with Children"

label variable dgn "Gender"
label variable dag "Age"
label variable dagsq "Age Squared"
label variable drgn1 "Region"
label variable stm "Year"
label variable les_c3 "Employment Status: 3 Category" 
label variable dhe "Self-rated Health"
label variable dcpen "Entered a new Partnership"
label variable dcpex "Partnership dissolution"
label variable deh_c3 "Educational Attainment: 3 Category"
label variable dnc "Number of Children in Household"
label variable dnc02 "Number of Children aged 0-2 in Household"
label variable ydses_c5 "Gross Annual Household Income Quintile" 
label variable lesdf_c4 "Differential Employment Status"
label variable ypnbihs_dv "Personal Non-benefit Gross Income"
label variable ypnbihs_dv_sq "Personal Non-benefit Gross Income Squared"
label variable ynbcpdf_dv "Differential Personal Non-Benefit Gross Income"
label variable dhhtp_c4 "Household Type: 4 Category"

label value dgn gdr
label value drgn1 rgna
label value les_c3 lessp_c3 jbf 
label value deh_c3 dehsp_c3 edd 
label value dcpen dcpex yn
label value lesdf_c4 dces
label value dhhtp_c4 hht

drop if dag < 16
replace stm = stm - 2000

/*check if all covariates are available in the data*/ 
recode dcpen dgn dag dagsq ydses_c5 dnc dnc02 dhe deh_c3 dehsp_c3 les_c3 ///
ypnbihs_dv ypnbihs_dv_sq dnc dnc02 dhe dhesp ynbcpdf_dv dcpyy dcpagdf dhhtp_c4 lesdf_c4 ///
drgn1 stm  (-9=. ) 

xtset idperson swv


***************************************************************
*Process U1a: Entering a partnership - In continuous education *
***************************************************************
*Probability of entering a partnership. 
*Sample: All single respondents aged 18 and older, in continuous education.
fre dcpen if (dag>=18 & ded==1 & ssscp!=1) //exclude same sex couples

probit dcpen i.dgn dag dagsq li.ydses_c5 l.dnc l.dnc02 i.dhe ib8.drgn1 stm if (dag>=16 & ded==1 & ssscp!=1) [pweight=disclwt], vce(robust)
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_data/union", sheet("Process U1a") replace
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))
outreg2 stats(coef se pval) using "$dir_data/U1a.doc", replace ///
title("Process U1a: Probit regression estimates for entering a partnership - single respondents aged 16+ in continuous education") ///
 ctitle(enter partnership) label side dec(2) noparen addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll))

 
********************************************************************
*Process U1b: Entering a partnership - Not in continuous education  *
********************************************************************
*Probability of entering a partnership. 
*Sample: All respondents aged 18+ who were not in a parthership at t-1 and were not in continuous education
fre dcpen if (dag>=18 & ded==0 & ssscp!=1) //exclude same sex couples

probit dcpen i.dgn dag dagsq ib1.deh_c3 li.les_c3 li.ydses_c5 l.dnc l.dnc02 i.dhe ib8.drgn1 stm if (dag>=18 & ded==0 & ssscp!=1) [pweight=disclwt], vce(robust)
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_data/union", sheet("Process U1b") modify
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))
outreg2 stats(coef se pval) using "$dir_data/U1b.doc", replace ///
title("Process U1b: Probit regression estimates for entering a partnership - single respondents aged 18+ not in continuous education") ///
 ctitle(enter partnership) label side dec(2) noparen addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll))


******************************************************************
*Process 2b: Exiting a partnership - Not in continuous education *
******************************************************************
*Probability of partnership break-up.
*Sample: Female member of a couple aged 18+ who were in a partnership at t-1 and not in a partnership at t and were not in continuous education
fre dcpex if (dgn==0 & dag>=18 & ded==0 & ssscp!=1) //exclude same sex couples

probit dcpex dag dagsq lib1.deh_c3 lib1.dehsp_c3 li.dhe li.dhesp l.dcpyy l.dcpagdf l.dnc l.dnc02 lib1.dhhtp_c4 lib1.lesdf_c4 ///
l.ypnbihs_dv l.ynbcpdf_dv ib8.drgn1 stm if (dgn==0 & dag>=18 & ded==0 & ssscp!=1) [pweight=dhhwt], vce(robust)
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_data/union", sheet("Process U2b") modify
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))
outreg2 stats(coef se pval) using "$dir_data/U2b.doc", replace ///
title("Process U2b: Probit regression estimates for exiting a partnership - cohabiting women aged 18+ not in continuous education") ///
 ctitle(enter partnership) label side dec(2) noparen addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll))


capture log close 
