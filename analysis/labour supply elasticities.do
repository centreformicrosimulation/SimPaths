/**************************************************************************************
*
*	PROGRAM TO EVALUATE LABOUR SUPPLY ELASTICITIES
*
*	Program analyses output from multirun config file: "config - labour supply elasticity"
*
*	Review by Keane (2011) and meta analysis by Bargain and Peichl (2016) report typical 
*	estimates for the Marshallian labour supply elasticity between -0.12 and 0.28
*	
*	Last version:  Justin van de Ven, 23 May 2024
*	First version: Justin van de Ven, 13 Jun 2024
*
**************************************************************************************/

clear all

global moddir = "C:\MyFiles\99 DEV ENV\JAS-MINE\SimPaths\output\labour elas\csv"
global outdir = "C:\MyFiles\99 DEV ENV\JAS-MINE\SimPaths\analysis\"
local dinc = 0.01	// assumed adjustment factor for disposable income of employed people (from SimPathsMultiRun object)


/**************************************************************************************
*	start
**************************************************************************************/
cd "$outdir"
import delimited using "$moddir/Person.csv"
drop if (time!=2019)
save temp0, replace


/**************************************************************************************
*	evaluate run statistics
**************************************************************************************/
forvalues ii = 1/3 {
	
	use temp0, clear
	keep if (run==`ii')
	if (`ii'==1) {
		sum id_person
		local idp0 = r(min)
		sum idbenefitunit
		local idb0 = r(min)
	} 
	else {
		sum id_person
		local idp1 = r(min)
		replace id_person = id_person - `idp1' + `idp0'
		sum idbenefitunit
		local idb1 = r(min)
		replace idbenefitunit = idbenefitunit - `idb1' + `idb0'
	}
	gsort idbenefitunit id_person
	gen hoursworked = real(hoursworkedweekly)
	recode hoursworked (missing=0)
	by idbenefitunit: egen thours = sum(hoursworked)
	gen chk = 0
	replace chk = 1 if (idbenefitunit!=idbenefitunit[_n-1])
	keep if (chk==1)
	drop chk
	gen lhours = ln(thours)
	keep if (!missing(lhours))
	keep idbenefitunit idoriginalbu lhours
	rename idoriginalbu idorigbu`ii'
	rename lhours lhours`ii'
	save temp`ii', replace
}


/**************************************************************************************
*	compile statistics over runs
**************************************************************************************/
use temp1, clear
merge 1:1 idbenefitunit using temp2, keep(3) nogen
merge 1:1 idbenefitunit using temp3, keep(3) nogen
gen chk = (idorigbu1!=idorigbu2) | (idorigbu2!=idorigbu3)
tab chk
drop if (chk==1)
save temp4, replace


/**************************************************************************************
*	evaluate individual specific elasticities
**************************************************************************************/
local dinc = 0.01	// factor adjustment to disposable income of employed people assumed for analysis
gen elas2 = (lhours2 - lhours1) / `dinc'
gen elas3 = (lhours3 - lhours1) / -`dinc'

sum elas2
local elas2 = r(mean)
sum elas3
local elas3 = r(mean)

local elasAv = (`elas2' + `elas3') / 2.0
disp `elasAv'


/**************************************************************************************
*	clean-up
**************************************************************************************/
#delimit ;
local files_to_drop 
	temp0.dta
	temp1.dta
	temp2.dta
	temp3.dta
	temp4.dta
	;
#delimit cr // cr stands for carriage return

foreach file of local files_to_drop { 
	erase "`file'"
}

