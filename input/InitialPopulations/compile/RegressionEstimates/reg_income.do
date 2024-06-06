********************************************************************************
* PROJECT:  		ESPON
* SECTION:			Non-employment/non-benefit income
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
log using "${dir_log}/reg_income.log", replace
*******************************************************************


import excel "$dir_data/time_series_factor.xlsx", sheet("UK_wage_growth") firstrow clear // Import real growth index
rename Year stm
rename Value growth
gen base_val = growth if stm == 2015
sum base_val
replace base_val = r(mean)
replace growth= growth/base_val
drop base_val
replace stm = stm - 2000
save "$dir_data\growth_rates", replace

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
label variable dhhtp_c4 "Household Type: 4 Category"
label variable dnc "Number of Children in Household"
label variable dnc02 "Number of Children aged 0-2 in Household"
label variable dhe "Self-rated Health"
label variable ydses_c5 "Annual Household Income Quintile" 
label variable dlltsd "Long-term Sick or Disabled"
label variable dcpen "Entered a new Partnership"
label variable dcpex "Partnership dissolution"
label variable lesdf_c4 "Differntial Employment Status"
label variable ypnbihs_dv "Personal Non-benefit Gross Income"

gen  ypnbihs_dv_sq =ypnbihs_dv^2 
 
label variable ypnbihs_dv_sq "Personal Non-benefit Gross Income Squared"
label variable ynbcpdf_dv "Differential Personal Non-Benefit Gross Income"

label value dgn gdr
label value drgn1 rgna
label value les_c3 jbf 
label value deh_c3 edd 
label value dcpen dcpex yn
label value lesdf_c4 dces
label value ded dlltsd yn
label value dhhtp_c4 hht

drop if dag < 16
//replace stm = stm - 2000
sort stm
merge m:1 stm using "$dir_data/growth_rates", keep(3) nogen keepusing(growth)


/**********************************************************************
CLEAN UP VARIABLES FOR REGRESSIONS	
***********************************************************************/
recode  dgn dag dagsq dhe drgn1 stm scedsmpl deh_c3 les_c3 dhhtp_c4 dhe (-9=.)
sum yplgrs_dv ypncp ypnoab pred_hourly_wage

xtset idperson swv
/*
*****************************************************************
*Process I1a: Non-employment income - In continuous education   *
*****************************************************************
regress yptciihs_dv i.dgn dag dagsq l.dhe l.yptciihs_dv ib8.drgn1 stm if scedsmpl==1 [pweight=disclwt], vce(robust)
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_data/Income_mdls", sheet("Income - In education") replace
putexcel A1 = matrix(results), names nformat(number_d2) 
*predict fittedice
*histogram fittedice
*histogram yptciihs_dv

*Getting Variance Covariance Matrix 
matrix i1a=get(VCE)
matrix list i1a
putexcel set "$dir_data/income_vcm", sheet("Process I1a - In education") replace
putexcel A1 = matrix(i1a), names

*******************************************************************
*Process I1b: Non-employment income - Not in continuous education *
*******************************************************************
regress yptciihs_dv i.dgn dag dagsq ib1.deh_c3 i.dlrtrd li.les_c3 lib1.dhhtp_c4 l.dhe l.yplgrs_dv l.yptciihs_dv ///
l2.yplgrs_dv l2.yptciihs_dv l3.yplgrs_dv l3.yptciihs_dv ib8.drgn1 stm if scedsmpl==0 [pweight=disclwt], vce(robust)
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_data/Income_mdls", sheet("Income - Not in education") modify
putexcel A1 = matrix(results), names nformat(number_d2) 
*predict fittednice
*histogram fittednice
*histogram yptciihs_dv

*Getting Variance Covariance Matrix 
matrix i1b=get(VCE)
matrix list i1b
putexcel set "$dir_data/income_vcm", sheet("Process I1b - Not in education") modify
putexcel A1 = matrix(i1b), names
*/


/*******************************************************************************

New models for split income variable
The goal is to split the current non-labour non-benefit income variable into 3 components  
(capital returns, occupational pension, public pension) and estimate each of them separately, 
using (if possible) current set of controls. We have decided to abstain from estimating transfers at the moment. 

*******************************************************************************/
bys swv idhh: gen nwa = _N
*Replace l.dhe with dhe if aged 16
gsort +idperson -stm
bys idperson: carryforward dhe if dag <= 16, replace 

//For those who are 16, L1 of the variables below is missing as they were 15 at the time. Use current value to keep them in the sample. 
sort idperson swv
bys idperson: gen dhe_L1 = l.dhe
replace dhe_L1 = dhe if missing(dhe_L1) //For those who have L1.dhe missing, use current dhe

bys idperson: gen yplgrs_L1 = l.yplgrs_dv
replace yplgrs_L1 = yplgrs_dv if missing(yplgrs_L1)

bys idperson: gen ypncp_L1 = l.ypncp
replace ypncp_L1 = ypncp if missing(ypncp_L1)

bys idperson: gen yplgrs_L2 = l2.yplgrs_dv
replace yplgrs_L2 = yplgrs_dv if missing(yplgrs_L2)

bys idperson: gen ypncp_L2 = l2.ypncp
replace ypncp_L2 = ypncp if missing(ypncp_L2)

bys idperson: gen dhhtp_c4_L1 = l.dhhtp_c4
replace dhhtp_c4_L1 = dhhtp_c4 if missing(dhhtp_c4_L1)

bys idperson: gen les_c3_L1 = l.les_c3
replace les_c3_L1 = les_c3 if missing(les_c3_L1)


/**********************************************************************
SELECTION MODELS FOR CAPITAL INCOME 
***********************************************************************/

*****************************************************************
*Process I3a selection: Probability of receiving capital income. 
*****************************************************************
*Sample: Individuals aged 16 - 29 who are in continuous education.
gen receives_ypncp = (ypncp > 0 & !missing(ypncp))
logit receives_ypncp i.dgn dag dagsq l.dhe l.yplgrs_dv l.ypncp ib8.drgn1 stm if scedsmpl==1 [pweight=dimxwt], vce(cluster idperson) base

matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_data/uk_income_split", sheet("Process I3a_selection E") replace
putexcel A1 = matrix(results), names nformat(number_d2)

matrix i1a=get(VCE)
matrix list i1a
putexcel set "$dir_data/uk_income_split_vcm", sheet("Process I3a_selection VCE") replace
putexcel A1 = matrix(i1a), names

outreg2 stats(coef se pval) using "$dir_data/I3a_sel.doc", replace ///
title("Process I3a selection: Probability of receiving capital income. Sample: Individuals aged 16 - 29 who are in continuous education.") ///
ctitle(Probability of capital income) label side dec(2) noparen addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll))

 
********************************************************************
*Process I3b selection: Probability of receiving capital income.
********************************************************************
*Sample: Individuals aged 16+ who are not in continuous education.

logit receives_ypncp i.dgn dag dagsq ib1.deh_c3 li.les_c3 lib1.dhhtp_c4 l.dhe l.yplgrs_dv l.ypncp l2.yplgrs_dv ///
l2.ypncp ib8.drgn1 stm if scedsmpl==0 [pweight=dimxwt], vce(cluster idperson) base

matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_data/uk_income_split", sheet("Process I3b_selection E") modify
putexcel A1 = matrix(results), names nformat(number_d2)

matrix i1a=get(VCE)
matrix list i1a
putexcel set "$dir_data/uk_income_split_vcm", sheet("Process I3b_selection VCE") modify
putexcel A1 = matrix(i1a), names

outreg2 stats(coef se pval) using "$dir_data/I3b_sel.doc", replace ///
title("Process I3b selection: Probability of receiving capital income. Sample: Individuals aged 16+ who are not in continuous education.") ///
ctitle(Probability of capital income) label side dec(2) noparen addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll))

/**********************************************************************/
********************************************
*Process I3a: Amount of capital income. 
********************************************
*Sample: Individuals aged 16 - 29 who are in continuous education and receive capital income.
*Using same controls as Cara - use of lags means those observed for the first time are not taken into account

regress ypncp i.dgn dag dagsq l.dhe l.yplgrs_dv l.ypncp ib8.drgn1 stm if scedsmpl==1 & receives_ypncp == 1 [pweight=dimxwt], ///
vce(cluster idperson) base
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_data/uk_income_split", sheet("Process I3a CapIn E") modify
putexcel A1 = matrix(results), names nformat(number_d2)

matrix i1a=get(VCE)
matrix list i1a
putexcel set "$dir_data/uk_income_split_vcm", sheet("Process I3a CapIn E VCE") modify
putexcel A1 = matrix(i1a), names

outreg2 stats(coef se pval) using "$dir_data/I3a.doc", replace ///
title("Process I3a: Amount of capital income. Sample: Individuals aged 16 - 29 who are in continuous education and receive capital income.") ///
 ctitle(Amount of capital income) label side dec(2) noparen addstat(R2, e(r2), RMSE, e(rmse))

*******************************************
*Process I3b: Amount of capital income. 
*******************************************
*Sample: Individuals aged 16+ who are not in continuous education and receive capital income.
*Using same controls as Cara
regress ypncp i.dgn dag dagsq ib1.deh_c3 li.les_c3 lib1.dhhtp_c4 l.dhe l.yplgrs_dv l.ypncp l2.yplgrs_dv l2.ypncp ib8.drgn1 stm ///
 if scedsmpl==0 & receives_ypncp == 1 [pweight=dimxwt], vce(cluster idperson) base
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_data/uk_income_split", sheet("Process I3b CapIn NiE") modify
putexcel A1 = matrix(results), names nformat(number_d2)

matrix i1a=get(VCE)
matrix list i1a
putexcel set "$dir_data/uk_income_split_vcm", sheet("Process I3b CapIn NiE VCE") modify
putexcel A1 = matrix(i1a), names

outreg2 stats(coef se pval) using "$dir_data/I3b.doc", replace ///
title("Process I3b: Amount of capital income. Sample: Individuals aged 16+ who are not in continuous education and receive capital income.") ///
ctitle(Amount of capital income) label side dec(2) noparen addstat(R2, e(r2), RMSE, e(rmse))

replace les_c3 = 4 if dlrtrd == 1

label define jbf 4 "Retired", add 

/**********************************************************************
PRIVATE PENSION INCOME
***********************************************************************/
***************************************************
*Process I4b: Amount of pension income. 
***************************************************
*Sample: Retired individuals who were retired in the previous year.
gen state_pension_age = (dag >= 68)
gen receives_ypnoab = (ypnoab_lvl > 0 & !missing(ypnoab_lvl))

regress ypnoab dag dagsq ib1.deh_c3 lib1.dhhtp_c4 l.dhe l.ypnoab l2.ypnoab ib8.drgn1 c.growth stm ///
if dag >= 50 & les_c3 == 4 & l.les_c3 == 4 [pweight=dimxwt], vce(cluster idperson) base

matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_data/uk_income_split", sheet("Process I4b Pension Next") modify
putexcel A1 = matrix(results), names nformat(number_d2)

matrix i1a=get(VCE)
matrix list i1a
putexcel set "$dir_data/uk_income_split_vcm", sheet("Process I4b Pension Next VCE") modify
putexcel A1 = matrix(i1a), names

outreg2 stats(coef se pval) using "$dir_data/14b.doc", ///
replace title("Process I4b: Amount of pension income. Sample: Retired individuals who were retired in the previous year.") ///
ctitle(Retired) label side dec(2) noparen addstat(R2, e(r2), RMSE, e(rmse))



/**********************************************************************
PRIVATE PENSION INCOME VERSION 2: 
	-selection equation for recipiency of private pension income
	-followed by level of private pension income using linear model
***********************************************************************/
**************************************************************************
*Process I5a: Probability of receiving private pension income. 
**************************************************************************
*Sample: Retired individuals who were not retired in the previous year.
/*
Estimated on a sample of individuals retired at time t, who were not retired at t-1.
I.e. this is probability of receiving private pension income upon retirement. 
*/

logit receives_ypnoab i.dgn i.state_pension_age ib1.deh_c3 lib4.les_c3 lib1.dhhtp_c4 l.dhe l.pred_hourly_wage ib8.drgn1 c.growth stm ///
if scedsmpl==0 & dag >= 50 & dlrtrd == 1 & l.les_c3 != 2 & l.les_c3 != 4 [pweight=dimxwt], vce(cluster idperson) base

matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_data/uk_income_split", sheet("Process I5a Select") modify
putexcel A1 = matrix(results), names nformat(number_d2)

matrix i1a=get(VCE)
matrix list i1a
putexcel set "$dir_data/uk_income_split_vcm", sheet("Process I5a Select") modify
putexcel A1 = matrix(i1a), names

outreg2 stats(coef se pval) using "$dir_data/I5a.doc", replace ///
title("Process I5a: Probability of receiving private pension income. Sample: Retired individuals who were not retired in the previous year.") ///
ctitle(Probability of private pension income) label side dec(2) noparen addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll))

****************************************************
*Process I5b: Amount of private pension income. 
****************************************************
*Sample: Retired individuals who were not retired in the previous year and receive private pension income.
regress ypnoab_lvl i.dgn i.state_pension_age ib1.deh_c3 lib4.les_c3 lib1.dhhtp_c4 l.dhe l.pred_hourly_wage ib8.drgn1 c.growth stm ///
if scedsmpl==0 & dag >= 50 & dlrtrd == 1 & l.les_c3 != 2 & l.les_c3 != 4 & receives_ypnoab [pweight=dimxwt], vce(cluster idperson) base

matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_data/uk_income_split", sheet("Process I5b Amount") modify
putexcel A1 = matrix(results), names nformat(number_d2)

matrix i1a=get(VCE)
matrix list i1a
putexcel set "$dir_data/uk_income_split_vcm", sheet("Process I5b Amount") modify
putexcel A1 = matrix(i1a), names

outreg2 stats(coef se pval) using "$dir_data/I5b.doc", replace ///
title("Process I5b: Amount of private pension income. Sample: Retired individuals who were not retired in the previous year and receive private pension income.") ///
ctitle(Amount of private pension income) label side dec(2) noparen addstat(R2, e(r2), RMSE, e(rmse))

capture log close 

/*
********************
*I6a: selection
********************
/*

Processes I6a and I6b are used to estimate private pension income among those continue retirement (retired at t and at t-1), 
*and have not received private pension income in the previous year

Estimated on a sample of individuals retired at time t
I.e. this is probability of receiving private pension in retirement, if not received private pension income in the initial population data 
*/

logit receives_ypnoab i.dgn i.state_pension_age ib1.deh_c3 lib4.les_c3 lib1.dhhtp_c4 cl.ypncp l.dhe ib8.drgn1 c.growth stm ///
if dag >= 50 & les_c3 == 4 & l.les_c3 == 4 & l.receives_ypnoab == 0  [pweight=dimxwt], vce(cluster idperson) base

matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_data/uk_income_split", sheet("Process I6a Select") modify
putexcel A1 = matrix(results), names nformat(number_d2)

matrix i1a=get(VCE)
matrix list i1a
putexcel set "$dir_data/uk_income_split_vcm", sheet("Process I6a Select") modify
putexcel A1 = matrix(i1a), names

***********************************************************************************
*I6b: amount of private pension income for those receiving private pension income
***********************************************************************************

regress ypnoab_lvl i.dgn i.state_pension_age ib1.deh_c3 lib1.dhhtp_c4 l.dhe ib8.drgn1 cl.ypncp c.growth stm ///
if dag >= 50 & les_c3 == 4 & l.les_c3 == 4 & l.receives_ypnoab == 0 & receives_ypnoab == 1 [pweight=dimxwt], vce(cluster idperson) base

matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_data/uk_income_split", sheet("Process I6b Amount") modify
putexcel A1 = matrix(results), names nformat(number_d2)

matrix i1a=get(VCE)
matrix list i1a
putexcel set "$dir_data/uk_income_split_vcm", sheet("Process I6b Amount") modify
putexcel A1 = matrix(i1a), names

*/


