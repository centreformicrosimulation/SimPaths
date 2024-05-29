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
*	First version: Justin van de Ven, 23 May 2024
*
**************************************************************************************/

clear all

global moddir = "C:\MyFiles\99 DEV ENV\JAS-MINE\SimPaths\output\labsupply\csv"
global outdir = "C:\MyFiles\99 DEV ENV\temp\"


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
	keep if (!missing(lrecons))
	keep id_benefitunit lrecons
	save temp`ii', replace
}


/**************************************************************************************
*	compile statistics over runs
**************************************************************************************/
use temp1, clear
merge 1:1 id_benefitunit using temp2, keep(3) nogen
merge 1:1 id_benefitunit using temp3, keep(3) nogen
save temp4, replace


/**************************************************************************************
*	evaluate individual specific elasticities
**************************************************************************************/
local r1 = 0.046	// starting average rate of return to saving (slight difference with cost of debt)
local dr = 0.005	// delta interest rate
local lr1 = ln(1.0 + `r1')
local lr2 = ln(1.0 + `r1' + `dr')
local lr2 = ln(1.0 + `r1' - `dr')
gen elas2 = (lrecons2 - lrecons1) / (`lr2' - `lr1')
gen elas3 = (lrecons3 - lrecons1) / (`lr3' - `lr1')

sum elas2
local elas2 = r(mean)
sum elas3
local elas3 = r(mean)

local elasAv = (`elas2' + `elas3') / 2.0
disp `elasAv'