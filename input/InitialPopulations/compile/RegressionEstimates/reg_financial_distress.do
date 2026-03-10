********************************************************************************
* PROJECT:		  UC and mental health
* SECTION:		  Health and wellbeing
* OBJECT: 		  Financial distress
* AUTHORS:		  Andy Baxter, Erik Igelström
* LAST UPDATE:	25 Feb 2026  
* COUNTRY:		  UK 
*
* NOTES:		
********************************************************************************
clear all
set more off
set mem 200m
set maxvar 30000


*******************************************************************
cap log close 
log using "${dir_log}/reg_financial_distress.log", replace
*******************************************************************


/********************************* PREPARE DATA *******************************/

use "${estimation_sample}", clear

* Set data 
xtset idperson swv
sort idperson swv 

* Adjust variables 
do "${dir_do}/variable_update.do"
/* DP: Household income/poverty/employment transition variables are moved to variable_update.do */

* Run Stata programs to produce Excel file 
do "${dir_do}/programs.do" 

* set label for process

global regression_set "financial_distress"

* Common variables

local employment_transitions "EmployedToUnemployed UnemployedToEmployed PersistentUnemployed"
local income_transitions "NonPovertyToPoverty PovertyToNonPoverty PersistentPoverty RealIncomeChange RealIncomeDecrease_D"
local lagged_financial "L_Ypncp L_Ypnoab D_Econ_benefits"
local labour "Lhw_10 Lhw_20 Lhw_30 Lhw_40"
local education "Deh_c4_High Deh_c4_Medium Deh_c4_Low"
local lagged_deprivation "L_Ydses_c5_Q2 L_Ydses_c5_Q3 L_Ydses_c5_Q4 L_Ydses_c5_Q5"
local lagged_health "L_Dhe_pcs L_Dhe_mcs"
local demographics "L_Dnc D_Home_owner L_Dcpst_Single Dgn L_Dag L_Dag_sq L_Dlltsd01"

**********************************************************************
* Financial Distress - logit model predicting financial distress
**********************************************************************


logit FinancialDistress ///
	`employment_transitions' `income_transitions' `lagged_financial' ///
	`labour' `education' `lagged_deprivation' `lagged_health' ///
	`demographics' ///
	L_FinancialDistress ///
	Year_transformed ///
	${regions} ${ethnicity} ///
	if ${health1_if_condition} [pweight=${weight}], vce(r)

process_regression, process("FinancialDistress") sheet("FinancialDistress") ///
	workbook("reg_financial_distress") ///
	title("Process FinancialDistress: Experiences financial distress") ///
	gofrow(1) goflabel("FinancialDistress - Experiences financial distress") ///
	ifcond("${health1_if_condition}") 


drop regression_set
macro drop _local
