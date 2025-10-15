********************************************************************************
* PROJECT:  		ESPON 
* SECTION:			Wage regression 
* OBJECT: 			Heckman regressions 
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
log using "${dir_log}/reg_wages.log", replace
*******************************************************************

global min_age = 17
global max_age = 64


/**************************************************************/
*
*	Fit (programs)
*
/**************************************************************/
capture program drop computePredicted
program computePredicted

	local model = "`1'" 
	local filter = "`2'"
	
	capture drop hat
	capture drop epsilon
	capture drop u
	
	if ("`model'" == "heckman") {
		predict hat if `filter'
		scalar sigma = e(sigma)
	}
	else if ("`model'" == "fe") {
		predict hat if `filter', xbu
		scalar sigma = e(sigma_u)
		predict u if  `filter', u
		hist u,  xtitle (estimated individual effect) name(u, replace)
	}
	else display "model not identified correctly (choose between 'fe' and 'heckman')"

	replace lwage_hour_hat = hat if `filter'
	gen epsilon = rnormal()*sigma
	sum epsilon
	replace wage_hour_hat = exp(lwage_hour_hat + epsilon) if `filter'
	
end

capture program drop analyseFit
program analyseFit

	local filter = "`1'"

	sum lwage_hour lwage_hour_hat wage_hour wage_hour_hat if `filter'
	if ("`2'" != "nocorr") {
		corr wage_hour L1.wage_hour if `filter' & previouslyWorking
		corr wage_hour_hat L1.wage_hour_hat if `filter' & previouslyWorking
	}
		
	twoway (hist lwage_hour if `filter', lcolor(gs12) fcolor(gs12)) ///
		(hist lwage_hour_hat if `filter', fcolor(none) lcolor(red)), xtitle (log gross hourly wages (GBP)) legend(lab(1 "observed") lab( 2 "predicted")) name(log, replace)
	
	twoway (hist wage_hour if `filter' & wage_hour < 150, lcolor(gs12) fcolor(gs12)) ///
		(hist wage_hour_hat if `filter' & wage_hour_hat < 150, fcolor(none) lcolor(red)), xtitle (gross hourly wages (GBP)) legend(lab(1 "observed") lab( 2 "predicted")) name(levels, replace)
		
end

capture program drop analyseFit2
program analyseFit2

	local filter = "`1'"

	sum lwage_hour lwage_hour_hat wage_hour wage_hour_hat if `filter'
	if ("`2'" != "nocorr") {
		corr wage_hour L1.wage_hour if `filter' & previouslyWorking
		corr wage_hour_hat L1.wage_hour_hat if `filter' & previouslyWorking
	}
		
	twoway (hist lwage_hour if `filter', lcolor(gs12) fcolor(gs12)) ///
		(hist lwage_hour_hat if `filter', fcolor(none) lcolor(red)), xtitle (log gross hourly wages (GBP)) legend(lab(1 "observed") lab( 2 "predicted")) name(log, replace) title("`3'")
		graph export "${dir_graphs}/log_`4'", replace 
	
	twoway (hist wage_hour if `filter' & wage_hour < 150, lcolor(gs12) fcolor(gs12)) ///
		(hist wage_hour_hat if `filter' & wage_hour_hat < 150, fcolor(none) lcolor(red)), xtitle (gross hourly wages (GBP)) legend(lab(1 "observed") lab( 2 "predicted")) name(levels, replace) title("`3'") 
		graph export "${dir_graphs}/level_`4'", replace

end

capture program drop outputResults
program outputResults

	local outputFile = "`1'"
	
	matrix results = r(table)
	matrix results = results[1..6,1...]'   //extract the first six rows of results, and then transpose results
	putexcel set "$dir_data/`outputFile'.xlsx", sheet("Estimates") replace 
	
	putexcel A3 = matrix(results), names nformat(number_d2)
	
	matrix results = e(V)
	putexcel set "$dir_data/`outputFile'.xlsx", sheet("Varcov") modify
	putexcel A3 = matrix(results), names nformat(number_d2)
		
end


/**************************************************************/
*
*	prepare data on real growth of wages 
*	based on uprating factors from microsimulation's input folder
* 	note: values are kept as in scenario_uprating_factor file to be consistent with the simulation which rebases indices to a specified BASE_PRICE_YEAR (2015)
*
/**************************************************************/
/*
import excel "$microsim_input_dir/scenario_uprating_factor.xlsx", sheet("UK_wage_growth") firstrow clear // Import nominal wage growth rates
rename Year stm
rename Value wage_growth
replace stm = stm - 2000
save "$work_dir/growth_rates", replace

import excel "$microsim_input_dir/scenario_uprating_factor.xlsx", sheet("UK_inflation") firstrow clear // Import inflation rates
rename Year stm
rename Value inflation
replace stm = stm - 2000
save "$work_dir/inflation", replace

use "$work_dir/growth_rates", clear
merge 1:1 stm using "$work_dir/inflation", keep(3) nogen

// Step 1: rebase wage growth index to 2015
sum wage_growth if stm == 15
gen base = r(mean)
replace wage_growth = wage_growth / base // Note: switching from 100 base to 1 base as that's what happens in the simulation when rebasing indices
drop base

// Step 2: generate real wage growth index, rebased to 2015 and adjusted to 2015 price level
replace inflation = inflation/100
gen real_wage_growth = wage_growth/inflation

save "$work_dir/growth_rates", replace
*/

// Note: use code above if calculating real wage growth inside of the simulation, but if loading from excel use values from excel in Stata too. 
//They *should* be the same but it is more consistent to have one source of values. 
import excel "$dir_data/time_series_factor.xlsx", sheet("UK_wage_growth") firstrow clear // Import real wage growth rates
rename Year stm
rename Value real_wage_growth
replace stm = stm - 2000
sum real_wage_growth if stm == 15
gen base = r(mean)
replace real_wage_growth = real_wage_growth / base // Note: switching from 100 base to 1 base as that's what happens in the simulation when rebasing indices
drop base
save "$dir_data/growth_rates", replace


/**************************************************************/
*
*	open and format data for analysis
*
/**************************************************************/
use "$dir_ukhls_data/ukhls_pooled_all_obs_09.dta", clear

drop if dag < $min_age
* screen data to ensure that idperson and swv uniquely identify observations
sort idperson swv
gen chk = 0
replace chk = 1 if (idperson == idperson[_n-1] & swv == swv[_n-1])
drop if chk == 1

* Fill in missing information on year (stm) based on wave (swv)
/*
replace stm = 2009 if swv == 1 & missing(stm)
replace stm = 2010 if swv == 2 & missing(stm)
replace stm = 2011 if swv == 3 & missing(stm)
replace stm = 2012 if swv == 4 & missing(stm)
replace stm = 2013 if swv == 5 & missing(stm)
replace stm = 2014 if swv == 6 & missing(stm)
replace stm = 2015 if swv == 7 & missing(stm)
replace stm = 2016 if swv == 8 & missing(stm)
replace stm = 2017 if swv == 9 & missing(stm)
replace stm = 2018 if swv == 10 & missing(stm)
replace stm = 2019 if swv == 11 & missing(stm)
replace stm = 2020 if swv == 12 & missing(stm)
replace stm = 2021 if swv == 13 & missing(stm)
*/

replace stm = stm - 2000


/**************************************************************/
*
*	merge in real growth index from microsimulation's input folder
*
/**************************************************************/
merge m:1 stm using "$dir_data/growth_rates", keep(3) nogen keepusing(real_wage_growth)

//rename drgnl drgn1 // Rename region variable to drgn1 (one, not "l")

*Variable stm identifies time periods. Need to ensure that combining idperson and stm ensures uniqueness.
duplicates tag idperson stm, gen(dup)
sort idperson stm
//DP: no such cases //

*However, this affects many variables: idhh, dag, ddt, dpd, ddt01, potentially idpartner. Might be best to move entire household. 
*Furthermore, the duplicated observation can occur in a year for which y-1 and y+1 have been observed. 
*This might require moving the y-1 or y+1 observation to y-2 or y+2. Alternatively, in such cases the duplicated observation for y can be dropped. 

*However, moving observations can be problematic because idhh can change. 
*Moving just the observation will result in mismatch between variables and idhh, 
*moving whole household will introduce longitudinal inconsistency for other household members. 
*=> Only move those observations for which the whole household can be moved 

*Generate a variable indicating which year the observation should belong to to maintain longitudinal consistency:

*Can only move if: 1) there is a gap on either side of the duplicate, 2) the whole household is duplicated
*Note: idhh is not longitudinal identifier

*Check if there is a gap:
by idperson: egen min_observed_year = min(stm)
gen count_year = stm - min_observed_year
sort idperson stm swv // Sort interview date in ascending order - earliest interview will be the one with the gap_prev set to 1
by idperson: gen gap_prev = (((count_year - count_year[_n-1]) > 1) & count_year>0) // There is a gap in year -1
by idperson: replace gap_prev = 1 if _n == 1 & dup == 1 & stm > 2009
//DP: 0 cases 

gsort +idperson -stm -swv // Sort years in reverse order. Sort int date in descending order - later interview will be the one with gap_next set to 1
by idperson: gen gap_next = (((count_year - count_year[_n-1]) < -1) & stm != 2018) // There is a gap in year +1
sort idperson stm swv
by idperson: replace gap_next = 1 if _n == _N & dup == 1 
//DP: 1,547 real changes made
by idperson: replace gap_prev = 0 if gap_next[_n-1] == 1 & dup[_n-1] == 1 // If previous observation already has flag set to move to next period, can't move another one to the same period
//DP: 3,193 real changes made

*Check if whole household is duplicated
bys idhh swv: egen min_dup = min(dup) // If == 1, then every observation for that household is duplicated
// 18480 cases 

*Check if whole household can be moved either back or forward:
bys idhh stm: egen hh_gap_prev = min(gap_prev)
bys idhh stm: egen hh_gap_next = min(gap_next)

*Generate identifier for the whole household which should be moved: move the observation from the wave which is closer to the gap
gen move = 1 if dup == 1 & (hh_gap_prev == 1 | hh_gap_next == 1) & min_dup == 1
//DP: 6548 cases 

*Move observations:
replace stm = stm-1 if move == 1 & hh_gap_prev == 1 /*3,425 real changes made*/
replace stm = stm+1 if move == 1 & hh_gap_next == 1 /*3,123 real changes made*/

*Drop households with duplicated observations, keeping observations from more recent waves if duplicated years:
sort stm idperson swv
drop dup
duplicates tag idperson stm, gen(dup)
by stm idperson: egen max_wave = max(swv) // Keep more recent obs
gen drop_idhh = idhh if max_wave == swv & dup == 1 // This identifies idhh which should be dropped
bys idhh stm: egen drop_idhh_max = max(drop_idhh) 
drop if !missing(drop_idhh_max)
//DP: 8,119 observations deleted
duplicates drop idperson stm, force // Few duplicates left, drop


****************************************
* Setting STATA to recognize Panel Data
xtset idperson stm
* total hours work per week (average)
gen hours = 0
replace hours = jbhrs if ((jbhrs > 0) & (jbhrs < .))
replace hours = hours + jshrs if ((jshrs > 0) & (jshrs < .))
replace hours = hours + j2hrs / 4.333 if ((j2hrs > 0) & (j2hrs < .))

* hour groups
gen hrs1 = (hours >   0) * (hours < 10)
gen hrs2 = (hours >= 10) * (hours < 15)
gen hrs3 = (hours >= 15) * (hours < 20)
gen hrs4 = (hours >= 20) * (hours < 25)
gen hrs5 = (hours >= 25) * (hours < 30)
gen hrs6 = (hours >= 30) * (hours < 35)
gen hrs7 = (hours >= 35) * (hours < 40)

gen hrs0_m1 = hours[_n-1] == 0
gen hrs1_m1 = (hours[_n-1] >  0) * (hours[_n-1] <= 29)

/**
gen hrs1_m1 = (hours[_n-1] >   0) * (hours[_n-1] < 10)
gen hrs2_m1 = (hours[_n-1] >= 10) * (hours[_n-1] < 15)
gen hrs3_m1 = (hours[_n-1] >= 15) * (hours[_n-1] < 20)
gen hrs4_m1 = (hours[_n-1] >= 20) * (hours[_n-1] < 25)
gen hrs5_m1 = (hours[_n-1] >= 25) * (hours[_n-1] < 30)
gen hrs6_m1 = (hours[_n-1] >= 30) * (hours[_n-1] < 35)
gen hrs7_m1 = (hours[_n-1] >= 35) * (hours[_n-1] < 40)
**/

* hourly wage
* screens population to individuals working (omits 1.58% of population aged 18+ and working):
*	at least 1 hour per week(455 obs between 0 and 1)
*	no more than 100 hours per week
*	earning at least 50 per month
*	earning no more than 83333 per month (1M per year) 

// Based on yplgrs which is derived from fimnlabgrs_dv in UKHLS
// Change to sinh(yplgrs_dv) which provides yplgrs deflated by CPI

gen yplgrs_dv_level = sinh(yplgrs_dv) 

gen wage_hour = .
replace wage_hour = yplgrs_dv_level / hours / 4.333 if (yplgrs_dv_level >= 50 & yplgrs_dv_level <= 83333 & hours >= 1 & hours <= 100)
sum wage_hour, det
*replace wage_hour = . if wage_hour < 4 | wage_hour > 70
* relationship status (1=cohabitating)
gen mar = (dcpst==1)
* children
gen any02 = dnc02 > 0
gen dnc4p = dnc
replace dnc4p = 1 if (dnc>4)
gen dnc2p = dnc
replace dnc2p = 2 if (dnc>2)
cap gen child = (dnc>0)
* individual weights
by idperson: egen wgt = mean(dimlwt)
* 


/**************************************************************/
*
*	preliminaries
*
/**************************************************************/
gen lwage_hour = ln(wage_hour)
hist lwage_hour if lwage_hour > 0 & lwage_hour < 4.4

gen swage_hour = asinh(wage_hour)
hist swage_hour if (swage_hour > 1 & swage_hour < 5)

replace lwage_hour = . if (wage_hour<5 | wage_hour>1000)

replace les_c3 = 3 if lwage_hour == . & les_c3 ! = 2 // PB: employment status is set on the basis of hourly wage not missing, so recode labour market activity status to match this for non-students
replace les_c3 = 1 if lwage_hour != . // PB: as above, if wage present consider as employed

recode deh_c3 dehm_c3 dehf_c3 drgn1 dhe (-9=.)


/**************************************************************/
*
*	pooled cross-sectional regressions
*
/**************************************************************/
gen pt = (hours >  0) * (hours <= 25)
drop hrs0_m1 hrs1_m1

* Strategy: 
* 1) Heckman estimated on the sub-sample of individuals who were not observed working in previous period. 
*    Wage equation does not controls for lagged wage
* 2) Heckman estimated on the sub-sample of individuals who were observed working in previous period. 
*    Wage equation controls for lagged wage
* Specification of selection equation is the same in the two samples

* Flag to identify observations to be included in the estimation sample 
bys idperson: gen obs_count = _N
gen in_sample = (obs_count > 1 & swv > 1)

* Flag to distinguish the two samples
capture drop previouslyWorking
gen previouslyWorking = (L1.lwage_hour != .) /* PB 07.02.2023: I think this will set previosuly working to 0 for everyone 
who is not observed in the previous period, e.g. all observations at Wave 1. I think the sample should include only individuals 
who are observed for at least two periods, and then the first observation should not be used in the estimation. */

capture drop lwage_hour_hat wage_hour_hat esample
gen lwage_hour_hat = .
gen wage_hour_hat = .
gen esample = .

gen L1les_c3 = L1.les_c3
gen lwage_hour_2 = lwage_hour^2

gen pred_hourly_wage = .

*** 1) Heckman estimated on the sub-sample of individuals who were not observed working in previous period. 
****   Wage equation does not control for lagged wage

* women
global wage_eqn "lwage_hour dag dagsq i.deh_c3 i.deh_c3#c.dag ded i.dehmf_c3 dlltsd i.dhe ib8.drgn1  pt real_wage_growth"
global seln_eqn "i.L1les_c3 dag dagsq i.deh_c3 i.deh_c3#c.dag ded i.dehmf_c3 mar child dlltsd i.dhe ib8.drgn1 " 
local filter = "dgn==0 & dag>=$min_age & dag<=$max_age & !previouslyWorking"
*heckman $wage_eqn if `filter' [pweight=dimxwt], select($seln_eqn) vce(robust)
heckman $wage_eqn if `filter', select($seln_eqn) twostep
outputResults "Not-working women3"

outreg2 stats(coef se pval) using "$dir_data/Output_NWW.doc", replace ///
title("Heckman-corrected wage equation estimated on the sample of women who were not in employment last year") ///
 ctitle(In education) label side dec(2) noparen 

*xtheckmanfe $wage_eqn if `filter', select($seln_eqn) reps(2)
computePredicted "heckman" `filter'
analyseFit "e(sample)" "nocorr"
replace esample = 1 if e(sample)
replace pred_hourly_wage = wage_hour_hat if e(sample)



* men
global wage_eqn "lwage_hour dag dagsq i.deh_c3 i.deh_c3#c.dag ded i.dehmf_c3 dlltsd i.dhe ib8.drgn1  pt real_wage_growth"
global seln_eqn "i.L1les_c3 dag dagsq i.deh_c3 i.deh_c3#c.dag ded i.dehmf_c3 mar child dlltsd i.dhe ib8.drgn1 " 
local filter = "dgn==1 & dag>=$min_age & dag<=$max_age & !previouslyWorking"
*heckman $wage_eqn if `filter' [pweight=dimxwt], select($seln_eqn) vce(robust)
heckman $wage_eqn if `filter', select($seln_eqn) twostep
outputResults "Not-working men3"

outreg2 stats(coef se pval) using "$dir_data/Output_NWM.doc", replace ///
title("Heckman-corrected wage equation estimated on the sample of men who were not in employment in the previous year") ///
ctitle(Wage equation coef.) label side dec(2) noparen 

computePredicted "heckman" `filter'
analyseFit "e(sample)" "nocorr"
replace esample = 1 if e(sample)
replace pred_hourly_wage = wage_hour_hat if e(sample)

*** 2) Heckman estimated on the sub-sample of individuals who were observed working in previous period. 
***    Wage equation controls for lagged wage

* women
global wage_eqn "lwage_hour L1.lwage_hour dag dagsq i.deh_c3 i.deh_c3#c.dag ded i.dehmf_c3 dlltsd i.dhe ib8.drgn1 pt real_wage_growth"
global seln_eqn "dag dagsq i.deh_c3 i.deh_c3#c.dag ded i.dehmf_c3 mar child dlltsd i.dhe ib8.drgn1 " 
local filter = "dgn==0 & dag>=$min_age & dag<=$max_age & swv > 1 & previouslyWorking"
*heckman $wage_eqn if `filter' [pweight=dimxwt], select($seln_eqn) vce(robust)
heckman $wage_eqn if `filter', select($seln_eqn) twostep
outputResults "Working women3"

outreg2 stats(coef se pval) using "$dir_data/Output_WW.doc", replace ///
title("Heckman-corrected wage equation estimated on the sample of women who were in employment in the previous year") ///
 ctitle(Wage equation coef.) label side dec(2) noparen 

computePredicted "heckman" `filter'
analyseFit "e(sample)" 
replace esample = 1 if e(sample)
replace pred_hourly_wage = wage_hour_hat if e(sample)

* men
global wage_eqn "lwage_hour L1.lwage_hour dag dagsq i.deh_c3 i.deh_c3#c.dag ded i.dehmf_c3 dlltsd i.dhe ib8.drgn1 pt real_wage_growth"
global seln_eqn "dag dagsq i.deh_c3 i.deh_c3#c.dag ded i.dehmf_c3 mar child dlltsd i.dhe ib8.drgn1" 
local filter = "dgn==1 & dag>=$min_age & dag<=$max_age & swv > 1 & previouslyWorking"
*heckman $wage_eqn if `filter' [pweight=dimxwt], select($seln_eqn) vce(robust)
heckman $wage_eqn if `filter', select($seln_eqn) twostep
outputResults "Working men3"

outreg2 stats(coef se pval) using "$dir_data/Output_WM.doc", replace ///
title("Heckman-corrected wage equation estimated on the sample of men who were in employment in the previous year") ///
 ctitle(Wage equation coef.) label side dec(2) noparen 


computePredicted "heckman" `filter'
analyseFit "e(sample)"
replace esample = 1 if e(sample)
replace pred_hourly_wage = wage_hour_hat if e(sample)

* all
analyseFit "esample == 1"
analyseFit "esample == 1 & dgn == 0"	// women
analyseFit "esample == 1 & dgn == 1"	// men


* Analyse fit per year:
forvalues year = 11/23 {
	di "Current year: `year'"
	analyseFit2 "esample == 1 & stm == `year'" "nocorr" "Year 20`year' all obs prv emp" "all_`year'_graph.png"
	analyseFit2 "esample == 1 & dgn == 0 & stm == `year'" "nocorr" "Year 20`year' women prv emp"	"women_`year'_graph.png"  // women
	analyseFit2 "esample == 1 & dgn == 1 & stm == `year'" "nocorr" "Year 20`year' men prv emp" "men_`year'_graph.png"	// men
	analyseFit2 "esample == 1 & dgn == 1 & deh_c3 == 1 & stm == `year'" "nocorr" "Year 20`year' men prv emp high ed" "men_highed_`year'_graph.png"	// men
}



// Note: sigma reported in the estimated regressions is the standard deviation of the residuals (=RMSE, assuming residuals are normally distributed)

*** Save for use in the do file estimating non-employment income
replace pred_hourly_wage = exp(lwage_hour) if missing(pred_hourly_wage)

save "$dir_ukhls_data/ukhls_pooled_all_obs.dta", replace


*** Calculate the proportion of "true zero" hours of work among those in the "ZERO" weekly hours of labour supply bracket. 
*I.e. the share of zero hours among 0-5 hours for those at risk of work. 

capture log close 
