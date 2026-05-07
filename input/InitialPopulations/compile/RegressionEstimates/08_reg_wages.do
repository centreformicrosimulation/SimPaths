********************************************************************************
* PROJECT:  		SimPaths UK
* SECTION:			Wage regression
* OBJECT: 			Heckman regressions
* AUTHORS:			Patryk Bronka, Daria Popova, Justin van de Ven, 
* 					Aleksandra Kolndrekaj, Ashley Burdett
* LAST UPDATE:		28 April 2026 (DP) 
********************************************************************************
********************************************************************************
* NOTES: 			Strategy:
* 					1) Heckman estimated on the sub-sample of individuals
* 						who are not observed working in previous period.
*   					=> Wage equation does not controls for lagged wage
* 					2) Heckman estimated on the sub-sample of individuals who
* 						are observed working in previous period.
*    					=> Wage equation controls for lagged wage
* 					Specification of selection equation is the same in the
* 						two samples
*
* 					Import labour cost index to create a measure of wage growth.
* 					Make sure loaded into the external_data subfolder.
*
*******************************************************************************/
clear all
set more off
set mem 200m
set type double
//set maxvar 120000
set maxvar 30000

*******************************************************************
cap log close
log using "${dir_log}/reg_wages.log", replace
*******************************************************************

* Load helper programs
do "${dir_do}/programs.do"

********************************************************************************
* Set Excel file
* Info sheet - first stage
putexcel set "$dir_results/reg_employment_selection", sheet("Info") replace
putexcel A1 = "Description:", bold
putexcel B1 = "This file contains regression estimates from the first stage of the Heckman selection model used to estimates wages."
putexcel A2 = "Authors:", bold
putexcel B2 = "Patryk Bronka, Justin Van de Ven, Daria Popova, Aleksandra Kolndrekaj, Ashley Burdett"
putexcel A3 = "Last edit: 28 April 2026 (DP) "

putexcel A5 = "Process:", bold
putexcel B5 = "Description:", bold
putexcel A6 = "W1fa-sel"
putexcel B6 = "First stage Heckman selection estimates for women that do not have an observed wage in the previous year"
putexcel A7 = "W1ma-sel"
putexcel B7 = "First stage Heckman selection estimates for women that do not have an observed wage in the previous year"
putexcel A8 = "W1fb-sel"
putexcel B8 = "First stage Heckman selection estimates for women that have an observed wage in the previous year"
putexcel A9 = "W1mb-sel"
putexcel B9 = "First stage Heckman selection estimates for men that have an observed wage in the previous year"

putexcel A11 = "Notes:", bold
putexcel B11 = "Estimated on panel data unlike the labour supply estimates"
putexcel B12 = "Predicted wages used as input into union parameters and income process estimates"
putexcel B13 = "Two-step Heckman command is used which does not permit weights"

* Info sheet - second stage
putexcel set "$dir_results/reg_wages", sheet("Info") replace
putexcel A1 = "Description:"
putexcel B1 = "This file contains regression estimates used to calculate potential wages for males and females in the simulation."
putexcel A2 = "Authors:", bold
putexcel B2 = "Patryk Bronka, Justin Van de Ven, Daria Popova, Aleksandra Kolndrekaj, Ashley Burdett"
putexcel A3 = "Last edit: 28 April 2026 (DP) "

putexcel A5 = "Process:", bold
putexcel B5 = "Description:", bold
putexcel A6 = "W1fa"
putexcel B6 = "Second stage Heckman selection estimates using women that do not have an observed wage in the previous year"
putexcel A7 = "W1ma"
putexcel B7 = "Second stage Heckman selection estimates using men that do not have an observed wage in the previous year"
putexcel A8 = "W1fb"
putexcel B8 = "Second stage Heckman selection estimates using women that have an observed wage in the previous year"
putexcel A9 = "W1mb"
putexcel B9 = "Second stage Heckman selection estimates using men that have an observed wage in the previous year"

putexcel A11 = "Notes:", bold
putexcel B11 = "Estimation sample: UK_ipop.dta. Two-step Heckman command is used which does not permit weights"
putexcel B12 = "Conditions for processes are defined as globals in master.do"
putexcel B13 = "Predicted wages sre saved in dataset UK_ipop2.dta and used as input into union parameters and income process estimates"

/********************************* PREPARE DATA *******************************/

* Prepare data on real growth of wages
import excel "$dir_external_data/time_series_factor.xlsx", ///
	sheet("UK_wage_growth") firstrow clear

rename Year stm
rename Value real_wage_growth

replace stm = stm - 2000

sum real_wage_growth if stm == 15
gen base = r(mean)
replace real_wage_growth = real_wage_growth / base
drop base

save "$dir_external_data/growth_rates", replace

* Load data 
use "${estimation_sample}", clear

* Adjust variables
do "${dir_do}/variable_update.do"

* Merge in real growth index
merge m:1 stm using "$dir_external_data/growth_rates", keep(3) nogen ///
	keepusing(real_wage_growth)
rename real_wage_growth realWageGrowth

* Set data
xtset idperson swv
sort idperson swv

* Hours work per week
gen hours = 0
replace hours = lhw if ((lhw > 0) & (lhw < .))
label var hours "Hours worked per week"

* Hourly wage
gen wage_hour = obs_earnings_hourly

* Winsorize
sum wage_hour, det
replace wage_hour = . if wage_hour <= 0
replace wage_hour = . if wage_hour >= r(p99)

gen lwage_hour = ln(wage_hour)
label var lwage_hour "Log gross hourly wage"

gen lwage_hour_2 = lwage_hour^2
label var lwage_hour_2 "Squared log gross hourly wage"

gen labWageHrlyLog = lwage_hour
gen labWageHrlyLogL1 = l.lwage_hour


* Flag to identify observations to be included in the estimation sample
bys idperson (swv): gen obs_count_ttl = _N
bys idperson (swv): gen obs_count = _n

gen in_sample = (obs_count_ttl > 1 & obs_count > 1)
replace in_sample = 0 if swv != swv[_n-1] +1 & idperson == idperson[_n-1]
replace in_sample = 0 if les_c3 == . | obs_earning == .
fre in_sample

* Flag to distinguish the two samples (prev work and not)
capture drop previouslyWorking
gen previouslyWorking = (L1.lwage_hour != .)
replace previouslyWorking = . if in_sample == 0
fre previouslyWorking

* Prep storage
capture drop lwage_hour_hat wage_hour_hat esample
gen lwage_hour_hat = .
gen wage_hour_hat = .
gen esample = .
gen pred_hourly_wage = .


/********************************** ESTIMATION ********************************/

* Run Stata programs to produce Excel file 
do "${dir_do}/programs.do" 

/******************** WAGES: WOMEN, NO PREV WAGE OBSERVED *********************/
#delimit ;
global wage_eqn 
lwage_hour 
demAge 
demAgeSq 
eduHighestC4LowL1 
eduHighestC4MediumL1 
eduHighestC4HighL1 
eduHighestC4LowL1_demAge
eduHighestC4MediumL1_demAge
eduHighestC4HighL1_demAge
eduHighestParentC3Medium
eduHighestParentC3High
healthDsblLongtermFlag
healthPhysicalPcsL1 
healthMentalMcsL1
labPt
realWageGrowth
$regions 
demYear2020 
demYear2021 
$ethnicity 
;
#delimit cr 

#delimit ;
global seln_eqn
labStatusC3StudentL1
labStatusC3NotEmployedL1 
demAge 
demAgeSq 
eduHighestC4LowL1 
eduHighestC4MediumL1 
eduHighestC4HighL1 
eduHighestC4LowL1_demAge
eduHighestC4MediumL1_demAge
eduHighestC4HighL1_demAge
eduHighestParentC3Medium
eduHighestParentC3High
healthDsblLongtermFlag
healthPhysicalPcsL1 
healthMentalMcsL1
demPartnerStatusPartnered
demNChild
$regions 
demYear2020 
demYear2021 
$ethnicity 
;
#delimit cr 

local filter = "${wages_f_no_prev_if_condition}"

heckman $wage_eqn if `filter', select($seln_eqn) twostep mills(lambda)

process_heckman,  ///
    process("W1fa") ///
    ifcond("`filter'") ///
    savefile("Female_NPW_sample") ///
    graphsubtitle("Females, No previously observed wage") ///
    wordfile("$dir_raw_results/wages/Output_NWW.doc") ///
    wordtitle("Heckman-corrected wage equation: women not in employment last year") ///
    wordctitle("Not working women") ///
    sheet2("W1fa") sheet1("W1fa-sel") ///
    rmserow(2)


/********************** WAGES: MEN, NO PREV WAGE OBSERVED *********************/
* globals are the same as for women 

local filter = "${wages_m_no_prev_if_condition}"

heckman $wage_eqn if `filter', select($seln_eqn) twostep mills(lambda)

process_heckman, ///
    process("W1ma") ///
    ifcond("`filter'") ///
    savefile("Male_NPW_sample") ///
    graphsubtitle("Males, No previously observed wage") ///
    wordfile("$dir_raw_results/wages/Output_NWM.doc") ///
    wordtitle("Heckman-corrected wage equation: men not in employment last year") ///
    wordctitle("Not working men") ///
    sheet2("W1ma") sheet1("W1ma-sel") ///
    rmserow(3)


/********************** WAGES: WOMEN, PREV WAGE OBSERVED **********************/

#delimit ;
global wage_eqn2
lwage_hour 
labWageHrlyLogL1 
demAge 
demAgeSq 
eduHighestC4LowL1 
eduHighestC4MediumL1 
eduHighestC4HighL1 
eduHighestC4LowL1_demAge
eduHighestC4MediumL1_demAge
eduHighestC4HighL1_demAge
eduHighestParentC3Medium
eduHighestParentC3High
healthDsblLongtermFlag
healthPhysicalPcsL1 
healthMentalMcsL1
labPt
realWageGrowth
$regions 
demYear2020 
demYear2021 
$ethnicity 
;
#delimit cr 

#delimit ;
global seln_eqn2
demAge 
demAgeSq 
eduHighestC4LowL1 
eduHighestC4MediumL1 
eduHighestC4HighL1 
eduHighestC4LowL1_demAge
eduHighestC4MediumL1_demAge
eduHighestC4HighL1_demAge
eduHighestParentC3Medium
eduHighestParentC3High
healthDsblLongtermFlag
healthPhysicalPcsL1 
healthMentalMcsL1
demPartnerStatusPartnered
demNChild
$regions 
demYear2020 
demYear2021 
$ethnicity 
;
#delimit cr 

local filter = "${wages_f_prev_if_condition}"

heckman $wage_eqn2 if `filter', select($seln_eqn2) twostep mills(lambda)

process_heckman, ///
    process("W1fb") ///
    ifcond("`filter'") ///
    savefile("Female_PW_sample") ///
    graphsubtitle("Females, Previously observed wage") ///
    wordfile("$dir_raw_results/wages/Output_WW.doc") ///
    wordtitle("Heckman-corrected wage equation: women in employment last year") ///
    wordctitle("Working women") ///
    sheet2("W1fb") sheet1("W1fb-sel") ///
    rmserow(4)


/********************** WAGES: MEN, PREV WAGE OBSERVED ************************/
* globals are the same as for women 

local filter = "${wages_m_prev_if_condition}"

heckman $wage_eqn2 if `filter', select($seln_eqn2) twostep mills(lambda)

process_heckman, ///
    process("W1mb") ///
    ifcond("`filter'") ///
    savefile("Male_PW_sample") ///
    graphsubtitle("Male, Previously observed wage") ///
    wordfile("$dir_raw_results/wages/Output_WM.doc") ///
    wordtitle("Heckman-corrected wage equation: men in employment last year") ///
    wordctitle("Working men") ///
    sheet2("W1mb") sheet1("W1mb-sel") ///
    rmserow(5)


* Save predicted wages to dataset 
	
* Use predicted wage for all; fall back to observed wage for those with no
* prediction (first observation for an individual)
replace pred_hourly_wage = exp(lwage_hour) if missing(pred_hourly_wage)

gen labWageHrly = pred_hourly_wage


save "${estimation_sample2}", replace

display "Wage analysis complete!"

capture log close

