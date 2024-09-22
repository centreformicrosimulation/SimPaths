/**************************************************************************************
*
*	PROGRAM TO EVALUATE INTERTEMPORAL ELASTICITIES OF SUBSTITUTION
*
*	Program analyses output from multirun config file: "config - intertemporal elasticity"
*
*	meta-analysis by Havranek, Horvath, Irsova, and Rusnak (2013) includes 34 studies that 
*	report 242 estimates for the intertemporal elasticity of substitution calculated on UK data, 
*	with a mean of 0.487 and a standard deviation of 1.09
*
*	NOTE: results exhibit substantial volatility
*
*	First version:  Justin van de Ven, 21 May 2024
*	Last version: Justin van de Ven, 16 Sep 2024
*
**************************************************************************************/

clear all

global moddir = "C:\MyFiles\99 DEV ENV\JAS-MINE\SimPaths\output\intertemporal\csv"
global outdir = "C:\MyFiles\99 DEV ENV\JAS-MINE\SimPaths\analysis\"
local dr = 0.0075	// delta interest rate (from SimPathsMultiRun object)


/**************************************************************************************
*	start
**************************************************************************************/
cd "$outdir"
import delimited using "$moddir/BenefitUnit.csv"
save temp0, replace


/**************************************************************************************
*	evaluate run statistics
**************************************************************************************/
forvalues ii = 1/3 {
	
	use temp0, clear
	keep if (run==`ii')
	if (`ii'==1) {
		sum id_benefitunit
		local id0 = r(min)
	} 
	else {
		sum id_benefitunit
		local id1 = r(min)
		replace id_benefitunit = id_benefitunit - `id1' + `id0'
	}
	xtset id_benefitunit time
	gen eqs = disposableincomemonthly * 12 / equivaliseddisposableincomeyearl
	gen tcons = discretionaryconsumptionperyear + (childcarecostperweek + socialcarecostperweek) * 364.25/7 
	gen econs = tcons/eqs
	gen lrecons`ii' = ln(econs / l.econs)
	gen reqs = round(eqs / l.eqs, 0.01)
	keep if (!missing(lrecons))
	keep if (reqs==1)
	keep id_benefitunit lrecons idoriginalbu idoriginalhh
	rename idoriginalbu idorigbu`ii'
	rename idoriginalhh idorighh`ii'
	save temp`ii', replace
}


/**************************************************************************************
*	compile statistics over runs
**************************************************************************************/
use temp1, clear
merge 1:1 id_benefitunit using temp2, keep(3) nogen
merge 1:1 id_benefitunit using temp3, keep(3) nogen
gen chk = (idorigbu1~=idorigbu2) | (idorigbu2~=idorigbu3) | (idorighh1~=idorighh2) | (idorighh2~=idorighh3)
tab chk
drop if (chk==1)
drop chk idorigbu* idorighh*
save temp4, replace


/**************************************************************************************
*	evaluate individual specific elasticities
**************************************************************************************/
use temp4, clear
local r1 = 0.046	// starting average rate of return to saving (slight difference with cost of debt)
local lr1 = ln(1.0 + `r1')
local lr2 = ln(1.0 + `r1' + `dr')
local lr3 = ln(1.0 + `r1' - `dr')
gen elas2 = (lrecons2 - lrecons1) / (`lr2' - `lr1')
gen elas3 = (lrecons3 - lrecons1) / (`lr3' - `lr1')

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
