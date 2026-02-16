***************************************************************************************
* PROJECT:              SimPaths UK: construct initial populations for SimPaths using UKHLS data  
* DO-FILE NAME:         03_social_care_received.do
* DESCRIPTION:          EXTRACT UKHLS DATA FOR SOCIAL CARE RECEIPT TO INCLUDE IN INITIAL POPULATION
***************************************************************************************
* COUNTRY:              UK
* DATA:         	    UKHLS EUL version - UKDA-6614-stata [to wave o]
* AUTHORS: 				Justin van de Ven, Daria Popova 
* LAST UPDATE:          15 Jan 2026 DP
* NOTE:					Called from 00_master.do - see master file for further details
***************************************************************************************


**************************************************************************************
cap log close 
log using "${dir_log}/03_social-care_received.log", replace
***************************************************************************************

* define seed to ensure replicatability of results
global seedBase = 3141592
global seedAdjust = 0

global careWageRate_minyear = 2010
* social care wage rates (real 2015 prices for consistency with inflation figures)
matrix careHourlyWageRates = (9.04 \ ///	2010
9.12 \ ///	2011
8.91 \ ///	2012
8.71 \ ///	2013
8.58 \ ///	2014
8.79 \ ///	2015
9.13 \ ///	2016
9.22 \ ///	2017
9.37 \ ///	2018
9.61 \ ///	2019
9.97 \ ///	2020
9.92 \ ///	2021
10.01 \ ///  2022
10.01 \ ///  2023
10.01 \ ///  2024
10.01) ///  2025
 
/*TO UPDATE FOR RECENT YEARS */

/**********************************************************************
*	start analysis
*********************************************************************/
cd "${dir_data}"
disp "identifying social care data"


/**************************************************************************************
*	load data
*************************************************************************************/
global firstWave = 7
foreach waveid in $scRecWaves {
	
	disp "evaluating social care for survey wave `waveid'"
	qui {
	
		local waveno=strpos("abcdefghijklmnopqrstuvwxyz","`waveid'")

		use "${dir_ukhls_data}/`waveid'_indresp.dta", clear
		rename *, l
		rename `waveid'_* *

		gen year = intdaty_dv
		if (`waveno' < 11) {
			gen helphours9 = -1
			gen helphoursb9 = -1
			gen helpcode9 = -1
		}

		gen swv = `waveno'

		gen need_socare = 0
		// need care if indicate they can only manage with help or not at all
		foreach vv of varlist adla adlb adlc adld adle adlf adlg adlh adli adlj adlk adll adlm adln {

			replace need_socare = 1 if (`vv'>1)
		}

		gen formal_socare_hrs = 0
		gen hourInfCare = 0
		gen partner_socare_hrs = 0
		gen daughter_socare_hrs = 0
		gen son_socare_hrs = 0
		forval ii = 1/9 {

			gen hrs = 0
			replace hrs = 2.5 if (helphours`ii' == 3)
			replace hrs = 7.0 if (helphours`ii' == 4)
			replace hrs = 14.5 if (helphours`ii' == 5)
			replace hrs = 27.0 if (helphours`ii' == 6)
			replace hrs = 42.0 if (helphours`ii' == 7)
			replace hrs = 74.5 if (helphours`ii' == 8)
			replace hrs = 125.0 if (helphours`ii' == 9)
			gen cc = 0
			replace cc = 1 if (helphours`ii' > 2)
			if (helphours`ii' < 2) {
				replace hrs = 14.5 if (helphoursb`ii' == 1)
				replace hrs = 27.0 if (helphoursb`ii' == 2)
				replace hrs = 100.0 if (helphoursb`ii' == 3)
				replace cc = 1 if (helphoursb`ii' > 0)
			}
			replace partner_socare_hrs = partner_socare_hrs + hrs if (helpcode`ii'==1 & hrs>0)
			replace son_socare_hrs = son_socare_hrs + hrs if (helpcode`ii'==2 & hrs>0)
			replace daughter_socare_hrs = daughter_socare_hrs + hrs if (helpcode`ii'==3 & hrs>0)
			replace hourInfCare = hourInfCare + hrs if (helpcode`ii'>=1 & helpcode`ii'<=10 & hrs>0)
			replace formal_socare_hrs = formal_socare_hrs + hrs if (helpcode`ii'>=11 & hrs>0)
			
			drop hrs cc
		}
		gen other_socare_hrs = hourInfCare - partner_socare_hrs - son_socare_hrs - daughter_socare_hrs
		keep pidp swv need_socare partner_socare_hrs son_socare_hrs daughter_socare_hrs other_socare_hrs formal_socare_hrs
		save "ukhls_socare_`waveid'.dta", replace
		global lastWave = `waveno'
	}
}

// pool data
disp "pooling social care data"
qui {
	foreach waveid in $scRecWaves {
		
		local waveno=strpos("abcdefghijklmnopqrstuvwxyz","`waveid'")
		if (`waveno'==7) {
			use "ukhls_socare_`waveid'.dta", clear
		}
		else {
			append using "ukhls_socare_`waveid'.dta"
		}
	}
	keep pidp
	sort pidp
	keep if (pidp!=pidp[_n-1])
	save sample_temp, replace
}


/**************************************************************************************
*	interpolate missing data
*************************************************************************************/

// identify gaps in data
disp "filling gaps in data"
qui {
	forvalues waveno = $firstWave/$lastWave {

		use sample_temp, clear
		local waveid = substr("abcdefghijklmnopqrstuvwxyz", `waveno', 1)
		local chk = strpos("$scRecWaves", "`waveid'")
		if ( `chk' > 0 ) {
		
			merge 1:1 pidp using "ukhls_socare_`waveid'.dta", nogen
			replace swv = `waveno' if missing(swv)
		}
		else {
			gen swv = `waveno'
		}
		save "ukhls_socareb_`waveid'.dta", replace
	}

	// pool data
	forvalues waveno = $firstWave/$lastWave {

		local waveid = substr("abcdefghijklmnopqrstuvwxyz", `waveno', 1)
		if (`waveno'==7) {
			use "ukhls_socareb_`waveid'.dta", clear
		}
		else {
			append using "ukhls_socareb_`waveid'.dta"
		}
	}
	save "ukhls_socare_pool.dta", replace

	// use interpolation and extrapolation to fill in missing data 
	use "ukhls_socare_pool.dta", clear
	gsort pidp swv
	local seed = $seedBase + $seedAdjust
	global seedAdjust = $seedAdjust + 1
	set seed `seed'
	gen ee = runiform()
	replace ee = ee[_n-1] if (pidp==pidp[_n-1])

	foreach var of varlist need_socare partner_socare_hrs son_socare_hrs daughter_socare_hrs other_socare_hrs formal_socare_hrs {

		disp "resolving missing values for `var'"
		qui {
		
			gsort pidp swv
			replace `var' = `var'[_n-1] if (missing(`var') & pidp[_n-1]==pidp & pidp[_n+1]==pidp & !missing(`var'[_n-1]) & (`var'[_n-1]==`var'[_n+1]))

			// interpolate single year gaps if change by random selection 
			replace `var' = `var'[_n-1] if (missing(`var') & pidp[_n-1]==pidp & pidp[_n+1]==pidp & !missing(`var'[_n-1]) & !missing(`var'[_n+1]) & ee<0.5)
			replace `var' = `var'[_n+1] if (missing(`var') & pidp[_n-1]==pidp & pidp[_n+1]==pidp & !missing(`var'[_n-1]) & !missing(`var'[_n+1]) & ee>=0.5)

			// identify observations for extrapolation backward
			gen missing_backward = missing(`var') * (pidp!=pidp[_n-1])
			replace missing_backward = missing_backward[_n-1] + 1 if (missing(`var') & pidp==pidp[_n-1])

			// identify observations for extrapoloation forward
			gsort pidp -swv
			gen missing_forward = missing(`var') * (pidp!=pidp[_n-1])
			replace missing_forward = missing_forward[_n-1] + 1 if (missing(`var') & pidp==pidp[_n-1])

			// extrapolate backward if all previous observations also missing
			replace `var' = `var'[_n-1] if (missing(`var') & (missing_backward==(swv-6)))

			// extrapolate forward if all prospective observations also missing
			gsort pidp swv
			replace `var' = `var'[_n-1] if (missing(`var') & (missing_forward==($lastWave+1-swv)))

			// interpolate between multi-year gaps if no change
			replace `var' = `var'[_n-missing_backward] if (missing(`var') & `var'[_n-missing_backward]==`var'[_n+missing_forward])

			// interpolate between multi-year gaps if change by random selection 
			gen chk = missing(`var')
			by pidp: egen mm = sum(chk)
			gen threshold = 0
			replace threshold = threshold[_n-1] + 1/(mm+1) if (missing(`var') & mm>0)
			replace `var' = `var'[_n+missing_forward] if (missing(`var') & ee<threshold)
			replace `var' = `var'[_n-missing_backward] if (missing(`var') & ee>=threshold)
			
			drop chk missing_backward missing_forward mm threshold
		}
	}
	drop ee
	rename pidp idperson
	save "ukhls_socare_poolb.dta", replace
}


/**************************************************************************************
*	merge with main data set
*************************************************************************************/
disp "merge results with existing data"

qui {
	
	use "UKHLS_pooled_all_obs_02.dta", clear

	merge 1:1 idperson swv using ukhls_socare_poolb, keep(1 3) nogen

/*TO UPDATE FOR RECENT YEARS */

	foreach var of varlist need_socare partner_socare_hrs son_socare_hrs daughter_socare_hrs other_socare_hrs formal_socare_hrs {
		replace `var' = -9 if (missing(`var'))
	}

	sort idperson swv 

	save "ukhls_pooled_all_obs_care.dta", replace 
}


/**************************************************************************************
* extend beyond interpolated sample (defined by firstWave and lastWave)
*************************************************************************************/
disp "impute data beyond interpolated sample"
use "ukhls_pooled_all_obs_care.dta", clear 

// age variables
gen dage1 = 0
forval ii = 1/10 {
	replace dage1 = `ii' if (dag>=65+2*(`ii'-1) & dag<=67+2*(`ii'-1))
}
replace dage1 = 11 if (dag>85)

// adjusted life satisfaction
gen dls_adj = round(dls)

// partner age difference
gen age_diff = 0
replace age_diff = 0 if (dcpagdf < -10)
replace age_diff = 1 if (dcpagdf < -5) * (dcpagdf >= -10)
replace age_diff = 2 if (dcpagdf < 0) * (dcpagdf >= -5)
replace age_diff = 3 if (dcpagdf < 5) * (dcpagdf >= 0)
replace age_diff = 4 if (dcpagdf < 10) * (dcpagdf >= 5)
replace age_diff = 5 if (dcpagdf >= 10)

// partner
gen partnered = (dcpst==1)

// replace missing
replace dlltsd01 = 0 if (dlltsd01<0)			// missing set to zero - not disabled
replace dlltsd01_sp = 0 if (dlltsd01_sp<0)		// mostly single people - missing set to zero and partner dummy picks up the variation
replace dehsp_c3 = 0 if (dehsp_c3<0)			// mostly single people - missing set to zero and partner dummy picks up the variation
replace dhesp = 0 if (dhesp<0)					// mostly single people - missing set to zero and partner dummy picks up the variation
replace dhm_ghq=0 if (dhm_ghq<0)				// missing set to zero
replace drgn1=7 if (drgn1<0)					// missing set to london
replace jbstat = 4 if (jbstat<0)				// missing set to retired

// need social care (basic probit)
gen need_socare_adj = need_socare
replace need_socare_adj = 0 if (need_socare<0)
probit need_socare_adj dgn i.deh_c3 i.dlltsd01 i.dlltsd01_sp i.dhhtp_c4 i.ydses_c5 unemp i.dehsp_c3 partnered i.dhe i.dhesp i.dhm_ghq i.dls_adj i.dot01 i.dage1 i.age_diff ib7.drgn1 i.jbstat widow [pweight=dimxwt] if (dag>64 & swv>=$firstWave & swv<=$lastWave) 
predict nsc_tmp if (dag>64 & (swv<$firstWave | swv>$lastWave))
gen ee = runiform()
gen need_socare2 = 0
replace need_socare2 = 1 if (ee<nsc_tmp & dag>64 & (swv<$firstWave | swv>$lastWave))
rename need_socare need_socare_original
gen need_socare = need_socare_original
replace need_socare = need_socare2 if (dag>64 & (swv<$firstWave | swv>$lastWave))
drop ee nsc_tmp
*tab swv need_socare

// hours informal care (double hurdle model)
gen informal_hrs = partner_socare_hrs*(partner_socare_hrs > 0) + son_socare_hrs*(son_socare_hrs > 0) + daughter_socare_hrs*(daughter_socare_hrs > 0) + other_socare_hrs*(other_socare_hrs > 0)
gen rec_informal = (informal_hrs > 0)
probit rec_informal dgn i.deh_c3 i.dlltsd01 i.dlltsd01_sp i.dhhtp_c4 i.ydses_c5 unemp i.dehsp_c3 partnered i.dhe i.dhesp i.dhm_ghq i.dls_adj i.dot01 i.dage1 i.age_diff ib7.drgn1 i.jbstat widow [pweight=dimxwt] if (dag>64 & swv>=$firstWave & swv<=$lastWave) 
predict ip if (dag>64 & (swv<$firstWave | swv>$lastWave))
gen ee = runiform()
gen ip2 = 0
replace ip2 = 1 if (ee<ip & dag>64 & (swv<$firstWave | swv>$lastWave))
drop ee ip
*tab swv rec_informal
*tab swv ip2

// use categorical variable to mitigate sparse distribution
gen hrs_grp = 0
replace hrs_grp = 1 if (informal_hrs>0 &   informal_hrs<=5)
replace hrs_grp = 2 if (informal_hrs>5 &   informal_hrs<=9)
replace hrs_grp = 3 if (informal_hrs>9 &   informal_hrs<=20)
replace hrs_grp = 4 if (informal_hrs>20 &  informal_hrs<=34)
replace hrs_grp = 5 if (informal_hrs>34 &  informal_hrs<=50)
replace hrs_grp = 6 if (informal_hrs>50 &  informal_hrs<=100)
replace hrs_grp = 7 if (informal_hrs>100 & informal_hrs<=110)
replace hrs_grp = 8 if (informal_hrs>110)
oprobit hrs_grp dgn i.deh_c3 i.dlltsd01 i.dlltsd01_sp i.dhhtp_c4 i.ydses_c5 unemp i.dehsp_c3 partnered i.dhe i.dhesp i.dhm_ghq i.dls_adj i.dot01 i.dage1 i.age_diff ib7.drgn1 i.jbstat widow [pweight=dimxwt] if (dag>64 & swv>=$firstWave & swv<=$lastWave & rec_informal==1)
forvalues ii = 1/7 {
	
	predict pp`ii' if (dag>64 & (swv<$firstWave | swv>$lastWave) & ip2==1), outcome(#`ii')
}
gen ee = runiform()
gen informal_hrs2 = 0
replace pp2 = pp2 + pp1
replace pp3 = pp3 + pp2
replace pp4 = pp4 + pp3
replace pp5 = pp5 + pp4
replace pp6 = pp6 + pp5
replace pp7 = pp7 + pp6
replace informal_hrs2 = 2.5 if (ee<=pp1 & dag>64 & (swv<$firstWave | swv>$lastWave) & ip2==1)
replace informal_hrs2 = 7 if (ee>pp1 & ee<=pp2 & dag>64 & (swv<$firstWave | swv>$lastWave) & ip2==1)
replace informal_hrs2 = 14.5 if (ee>pp2 & ee<=pp3 & dag>64 & (swv<$firstWave | swv>$lastWave) & ip2==1)
replace informal_hrs2 = 27.0 if (ee>pp3 & ee<=pp4 & dag>64 & (swv<$firstWave | swv>$lastWave) & ip2==1)
replace informal_hrs2 = 42.0 if (ee>pp4 & ee<=pp5 & dag>64 & (swv<$firstWave | swv>$lastWave) & ip2==1)
replace informal_hrs2 = 74.5 if (ee>pp5 & ee<=pp6 & dag>64 & (swv<$firstWave | swv>$lastWave) & ip2==1)
replace informal_hrs2 = 100  if (ee>pp6 & ee<=pp7 & dag>64 & (swv<$firstWave | swv>$lastWave) & ip2==1)
replace informal_hrs2 = 125  if (ee>pp7 & dag>64 & (swv<$firstWave | swv>$lastWave) & ip2==1)
replace informal_hrs2 = -9 if (dag<65 & (swv<$firstWave | swv>$lastWave) & ip2==0)

rename informal_hrs informal_hrs_original
gen informal_socare_hrs = informal_hrs_original
replace informal_socare_hrs = informal_hrs2 if (dag>64 & (swv<$firstWave | swv>$lastWave))
drop ip2 pp1 pp2 pp3 pp4 pp5 pp6 pp7 ee
*tab hrs_grp if (dag>64 & swv>=$firstWave & swv<=$lastWave)
*tab informal_hrs2 if (dag>64 & (swv<$firstWave | swv>$lastWave))

// hours of formal care
gen rec_formal = (formal_socare_hrs > 0)
gen rec_informal_intensive = (informal_socare_hrs>10)
probit rec_formal rec_informal rec_informal_intensive dgn i.deh_c3 i.dlltsd01 i.dlltsd01_sp i.dhhtp_c4 i.ydses_c5 unemp i.dehsp_c3 partnered i.dhe i.dhesp i.dhm_ghq i.dls_adj i.dot01 i.dage1 i.age_diff ib7.drgn1 i.jbstat widow [pweight=dimxwt] if (dag>64 & swv>=$firstWave & swv<=$lastWave) 
predict ip if (dag>64 & (swv<$firstWave | swv>$lastWave))
gen ee = runiform()
gen ip2 = 0
replace ip2 = 1 if (ee<ip*1.9 & dag>64 & swv<$firstWave)
replace ip2 = 1 if (ee<ip & dag>64 & swv>$lastWave)
drop ee ip
*tab swv rec_informal
*tab swv ip2

gen formal_hrs_grp = 0
replace formal_hrs_grp = 1 if (formal_socare_hrs>0 & formal_socare_hrs<=5)
replace formal_hrs_grp = 2 if (formal_socare_hrs>5 & formal_socare_hrs<=9)
replace formal_hrs_grp = 3 if (formal_socare_hrs>9 & formal_socare_hrs<=20)
replace formal_hrs_grp = 4 if (formal_socare_hrs>20 & formal_socare_hrs<=34)
replace formal_hrs_grp = 5 if (formal_socare_hrs>34 & formal_socare_hrs<=50)
replace formal_hrs_grp = 6 if (formal_socare_hrs>50 & formal_socare_hrs<=100)
replace formal_hrs_grp = 7 if (formal_socare_hrs>100)
oprobit formal_hrs_grp rec_informal rec_informal_intensive dgn i.deh_c3 i.dlltsd01 i.dlltsd01_sp i.dhhtp_c4 i.ydses_c5 unemp i.dehsp_c3 partnered i.dhe i.dhesp i.dhm_ghq i.dls_adj i.dot01 i.dage1 i.age_diff ib7.drgn1 i.jbstat widow [pweight=dimxwt] if (dag>64 & swv>=$firstWave & swv<=$lastWave & rec_formal==1)
forvalues ii = 1/7 {
	
	predict pp`ii' if (dag>64 & (swv<$firstWave | swv>$lastWave) & ip2==1), outcome(#`ii')
}
gen ee = runiform()
gen formal_socare_hrs2 = 0
replace pp2 = pp2 + pp1
replace pp3 = pp3 + pp2
replace pp4 = pp4 + pp3
replace pp5 = pp5 + pp4
replace pp6 = pp6 + pp5
replace pp7 = pp7 + pp6
replace formal_socare_hrs2 = 2.5 if (ee<=pp1 & dag>64 & (swv<$firstWave | swv>$lastWave) & ip2==1)
replace formal_socare_hrs2 = 7 if (ee>pp1 & ee<=pp2 & dag>64 & (swv<$firstWave | swv>$lastWave) & ip2==1)
replace formal_socare_hrs2 = 14.5 if (ee>pp2 & ee<=pp3 & dag>64 & (swv<$firstWave | swv>$lastWave) & ip2==1)
replace formal_socare_hrs2 = 27.0 if (ee>pp3 & ee<=pp4 & dag>64 & (swv<$firstWave | swv>$lastWave) & ip2==1)
replace formal_socare_hrs2 = 42.0 if (ee>pp4 & ee<=pp5 & dag>64 & (swv<$firstWave | swv>$lastWave) & ip2==1)
replace formal_socare_hrs2 = 74.5 if (ee>pp5 & ee<=pp6 & dag>64 & (swv<$firstWave | swv>$lastWave) & ip2==1)
replace formal_socare_hrs2 = 125  if (ee>pp6 & dag>64 & (swv<$firstWave | swv>$lastWave) & ip2==1)
replace formal_socare_hrs2 = -9 if (dag<65 & (swv<$firstWave | swv>$lastWave) & ip2==0)

rename formal_socare_hrs formal_socare_hrs_original
gen formal_socare_hrs = formal_socare_hrs_original
replace formal_socare_hrs = formal_socare_hrs2 if (dag>64 & (swv<$firstWave | swv>$lastWave))
drop ip2 pp1 pp2 pp3 pp4 pp5 pp6 ee
*tab formal_hrs_grp if (dag>64 & swv>=$firstWave & swv<=$lastWave)
*tab formal_socare_hrs2 if (dag>64 & (swv<$firstWave | swv>$lastWave))


/**************************************************************************************
* evaluate formal care costs
*************************************************************************************/
cap gen formal_socare_cost = -9
//replace formal_socare_cost = $careHourlyWageRates[`year' - $careWageRate_minyear + 1] * formal_socare_hrs if (formal_socare_hrs>0)
replace formal_socare_cost = 9.04 * formal_socare_hrs if (formal_socare_hrs>0) & stm==2010
replace formal_socare_cost = 9.12 * formal_socare_hrs if (formal_socare_hrs>0) & stm==2010
replace formal_socare_cost = 8.91 * formal_socare_hrs if (formal_socare_hrs>0) & stm==2011
replace formal_socare_cost = 8.71 * formal_socare_hrs if (formal_socare_hrs>0) & stm==2012
replace formal_socare_cost = 8.58 * formal_socare_hrs if (formal_socare_hrs>0) & stm==2013
replace formal_socare_cost = 8.79 * formal_socare_hrs if (formal_socare_hrs>0) & stm==2014
replace formal_socare_cost = 9.13 * formal_socare_hrs if (formal_socare_hrs>0) & stm==2016
replace formal_socare_cost = 9.22 * formal_socare_hrs if (formal_socare_hrs>0) & stm==2017
replace formal_socare_cost = 9.37 * formal_socare_hrs if (formal_socare_hrs>0) & stm==2018
replace formal_socare_cost = 9.61 * formal_socare_hrs if (formal_socare_hrs>0) & stm==2019
replace formal_socare_cost = 9.97 * formal_socare_hrs if (formal_socare_hrs>0) & stm==2020		  
replace formal_socare_cost = 9.92 * formal_socare_hrs if (formal_socare_hrs>0) & stm==2021
replace formal_socare_cost = 10.01 * formal_socare_hrs if (formal_socare_hrs>0) & stm==2022
replace formal_socare_cost = 10.01 * formal_socare_hrs if (formal_socare_hrs>0) & stm==2023
replace formal_socare_cost = 10.01 * formal_socare_hrs if (formal_socare_hrs>0) & stm==2024
replace formal_socare_cost = 10.01 * formal_socare_hrs if (formal_socare_hrs>0) & stm==2025


/**************************************************************************************
* save results
*************************************************************************************/
keep idperson swv need_socare informal_socare_hrs formal_socare_hrs formal_socare_cost
sort idperson swv
save temp, replace
use "ukhls_pooled_all_obs_care.dta", clear
merge 1:1 idperson swv using temp, keep(1 3) nogen
save "ukhls_pooled_all_obs_03.dta", replace 


/**************************************************************************************
* clean-up and exit
*************************************************************************************/
cap log close 
#delimit ;
local files_to_drop 
	sample_temp.dta
	ukhls_socare_g.dta 
	ukhls_socare_i.dta 
	ukhls_socare_k.dta 
	ukhls_socare_m.dta
	ukhls_socare_pool.dta
	ukhls_socare_poolb.dta
	ukhls_socareb_g.dta
	ukhls_socareb_h.dta
	ukhls_socareb_i.dta
	ukhls_socareb_j.dta
	ukhls_socareb_k.dta
	ukhls_socareb_l.dta
	ukhls_socareb_m.dta
	temp.dta
	;
#delimit cr // cr stands for carriage return

foreach file of local files_to_drop { 
	erase "$dir_data/`file'"
}

