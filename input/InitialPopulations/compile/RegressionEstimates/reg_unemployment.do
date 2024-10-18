********************************************************************************
* PROJECT:  		ESPON 
* SECTION:			Unemployment
* OBJECT: 			Final Probit Models
* AUTHORS:			Justin van de Ven
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
log using "${dir_log}/reg_unemployment.log", replace
*******************************************************************


/*******************************************************************************
*	START ANALYSIS
*******************************************************************************/


/*******************************************************************************
*	IMPORT UNEMPLOYMENT RATES
*******************************************************************************/
import delimited "${dir_data}/unemp_rates.csv", clear
save "${dir_data}/unemp_rates", replace


/*******************************************************************************
*	LOAD WORKING DATA
*******************************************************************************/
use "$dir_ukhls_data/ukhls_pooled_all_obs_09.dta", clear
keep if (dag>15 & dag<75)

// append unemployment rates to data
merge m:1 dgn deh_c3 dag stm using "${dir_data}/unemp_rates", keep(3) nogen
label variable dukue "UK unemployment rate by age, year, gender, and graduate status"

gen unemp = (jbstat==3)
label variable unemp "labour status unemployed"
gen nemp = (jbstat!=1 & jbstat!=2 & jbstat!=10 & jbstat!=11)
replace nemp = . if (jbstat==4 | jbstat==5 | jbstat==7 | jbstat==8 | jbstat==9 | jbstat==12 | jbstat==13 | jbstat==14)
label variable nemp "labour status not employed"
label variable dgn "Gender"
gen ageGroup = floor(dag/5)
label variable ageGroup "five year age band"

recode careWho dcpst drgn1 (-9=.)
recode formal_socare_hrs partner_socare_hrs daughter_socare_hrs son_socare_hrs other_socare_hrs (-9=0)
gen carer = (careWho>0)
gen recare = (formal_socare_hrs + partner_socare_hrs + daughter_socare_hrs + son_socare_hrs + other_socare_hrs > 0)

gen ageUnder20 = (dag<20)
gen age20to24 = (dag>19) * (dag<25)

gen dnc1 = (dnc==1)
gen dnc2 = (dnc==2)
gen dnc3 = (dnc>2)
gen dnc2p = (dnc>1)
gen dc02 = (dnc02>0)


/*******************************************************************************
*	CALCULATE REGRESSION
*******************************************************************************/
xtset idperson swv
probit unemp dukue i.dhe l.nemp ib8.drgn1 if (dgn==1 & dag>17 & dag<65 & deh_c3==1) [pweight=disclwt], vce(robust)
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_data/unempoyment", sheet("Process U1a male grads") replace
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))
outreg2 stats(coef se pval) using "$dir_data/U1a.doc", replace ///
title("Process U1a: Probability of unemployment. Sample: Men aged 18-64 with graduate education.") ///
 ctitle(Giving birth) label side dec(2) noparen addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll))

probit unemp dukue i.dhe l.nemp ib8.drgn1 if (dgn==1 & dag>17 & dag<65 & deh_c3>1) [pweight=disclwt], vce(robust)
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_data/unempoyment", sheet("Process U1b male ngrads") modify
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))
outreg2 stats(coef se pval) using "$dir_data/U1b.doc", replace ///
title("Process U1b: Probability of unemployment. Sample: Men aged 18-64 with non-graduate education.") ///
 ctitle(Giving birth) label side dec(2) noparen addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll))

probit unemp dukue i.dhe l.nemp ib8.drgn1 if (dgn==0 & dag>17 & dag<65 & deh_c3==1) [pweight=disclwt], vce(robust)
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_data/unempoyment", sheet("Process U1c female grads") modify
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))
outreg2 stats(coef se pval) using "$dir_data/U1c.doc", replace ///
title("Process U1c: Probability of unemployment. Sample: Women aged 18-64 with graduate education.") ///
 ctitle(Giving birth) label side dec(2) noparen addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll))

probit unemp dukue i.dhe l.nemp ib8.drgn1 if (dgn==0 & dag>17 & dag<65 & deh_c3>1) [pweight=disclwt], vce(robust)
matrix results = r(table)
matrix results = results[1..6,1...]'
putexcel set "$dir_data/unempoyment", sheet("Process U1d female ngrads") modify
putexcel A3 = matrix(results), names nformat(number_d2) 
putexcel J4 = matrix(e(V))
outreg2 stats(coef se pval) using "$dir_data/U1d.doc", replace ///
title("Process U1d: Probability of unemployment. Sample: Women aged 18-64 with non-graduate education.") ///
 ctitle(Giving birth) label side dec(2) noparen addstat(R2, e(r2_p), Chi2, e(chi2), Log-likelihood, e(ll))


// exploratory regressions
probit unemp i.ageGroup dukue carer recare i.dhe l.unemp ib1.dcpst dnc dnc02 i.drgn1 if (dgn==0 & deh_c3==1 & stm>2017)
probit unemp i.ageGroup dukue carer recare i.dhe l.unemp ib1.dcpst dnc dnc02 i.drgn1 if (dgn==0 & deh_c3>1 & stm>2017)
probit unemp i.ageGroup dukue carer recare i.dhe l.unemp ib1.dcpst dnc dnc02 i.drgn1 if (dgn==1 & deh_c3==1 & stm>2017)
probit unemp i.ageGroup dukue carer recare i.dhe l.unemp ib1.dcpst dnc dnc02 i.drgn1 if (dgn==1 & deh_c3>1 & stm>2017)



capture log close 



