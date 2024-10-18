/********************************************************************************
*
*	FILE TO EXTRACT UKHLS DATA FOR SOCIAL CARE RECEIPT TO INCLUDE IN INITIAL POPULATION
*
*	AUTH: Justin van de Ven (JV)
*	LAST EDIT: Daria Popova
*
********************************************************************************/


/********************************************************************************
	local data directories - commented out when using master program
********************************************************************************/

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
10.01276101) ///2022


/**********************************************************************
*	start analysis
**********************************************************************/
cd "${dir_data}"
disp "identifying social care data"


/**************************************************************************************
*	load data
**************************************************************************************/
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

		gen nbrNeeded = 0
		// need care if indicate they can only manage with help or not at all
		foreach vv of varlist adla adlb adlc adld adle adlf adlg adlh adli adlj adlk adll adlm adln {

			replace nbrNeeded = nbrNeeded + 1 if (`vv'>1)
		}
		gen need_socare = (nbrNeeded > 1)

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
**************************************************************************************/

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
**************************************************************************************/
disp "merge results with existing data"

qui {
	
	use "UKHLS_pooled_all_obs_02.dta", clear

	merge 1:1 idperson swv using ukhls_socare_poolb, keep(1 3) nogen


	foreach var of varlist need_socare partner_socare_hrs son_socare_hrs daughter_socare_hrs other_socare_hrs formal_socare_hrs {
		replace `var' = -9 if (missing(`var'))
	}


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


	sort idperson swv 

	save "ukhls_pooled_all_obs_03.dta", replace 
}


/**************************************************************************************
* clean-up and exit
**************************************************************************************/
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
	;
#delimit cr // cr stands for carriage return

foreach file of local files_to_drop { 
	erase "$dir_data/`file'"
}
