********************************************************************************
* PROJECT:  	SimPaths UK
* SECTION:		Health and wellbeing
* OBJECT: 		Health status and Disability
* AUTHORS:		Andy Baxter, Ashley Burdett 
* LAST UPDATE:	26 Mar 2026 (AB)
* COUNTRY: 		UK 
*
* NOTES:		
*   - This file updates GHQ12 Level (0-36) and Caseness (0-12) variables
********************************************************************************
clear all
set more off
set mem 200m
set maxvar 30000


********************************** SET LOG FILE ********************************
cap log close 
log using "${dir_log}/reg_health_mental.log", replace


****************************** SAMPLE GLOBALS **********************************
* For master file

global HM1_L_if_condition ""
global HM2_Females_L_if_condition "dag >= 25 & dag <= 64 & dgn == 0"
global HM2_Males_L_if_condition "dag >= 25 & dag <= 64 & dgn == 1"
global HM1_C_if_condition "stm != 20 & stm != 21 & dag >= 25 & dag <= 64 & swv != 12"
global HM2_Females_C_if_condition "dag >= 25 & dag <= 64 & dgn == 0"
global HM2_Males_C_if_condition "dag >= 25 & dag <= 64 & dgn == 1"


/******************************* SET EXCEL FILE *******************************/

putexcel set "$dir_results/reg_health_mental", sheet("Info") replace
putexcel A1 = "Description:"
putexcel B1 = "Model parameters governing projection of mental health"
putexcel A2 = "Authors:"
putexcel B2 = "Andy Baxter, Ashley Burdett" 
putexcel A3 = "Last edit: 17 Feb 2026"

putexcel A5 = "Process:", bold
putexcel B5 = "Description:", bold


putexcel A10 = "Notes:", bold


putexcel set "$dir_results/reg_health_mental", sheet("Gof") modify
putexcel A1 = "Goodness of fit", bold	


/********************************* PREPARE DATA *******************************/

* Load data
use "${estimation_sample}", clear

* Set data 
xtset idperson swv
sort idperson swv 

* Adjust variables 
do "${dir_do}/variable_update.do"
/* DP: Household income/poverty/employment transition variables are moved to variable_update.do */

* Remove children 
drop if dag < 16


/********************************** ESTIMATION ********************************/

* Run Stata programs to produce Excel file 
do "${dir_do}/programs.do" 


************************************************************************
* HM1_L: GHQ12 score 0-36 of all working-age adults - baseline effects *
************************************************************************

reg dhm ///
	L.i.dhh_owned L.i.dcpst L.dnc L.dhe_pcs L.ib8.drgn L.i.ydses_c5 ///
	L.dlltsd01 L.dhm ///
	L.dag L.dagsq i.deh_c3 i.dot i.dgn stm [pw=${weight}], ///
	vce(cluster idperson)
	
process_regression, domain("health_mental") process("HM1_L") sheet("HM1_L") ///
	title("Process HM1_L: Mental health score") ///
	gofrow(3) goflabel("HM1_L ") ///
	ifcond("${HM1_L_if_condition}")	

* Save RMSE	
scalar rmse = e(rmse)	
putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A13 = ("HM1_L") B13 = rmse 	
scalar drop rmse
	

***************************************************************
* HM2_Females_L: GHQ12 Score 0-36 - causal employment effects *
***************************************************************

* Stage 2 - Female

* List of variables to be reported in excel 
local vars_for_excel "ib11.exp_emp i.exp_poverty i.exp_incchange D.log_income financial_distress y2020 y2021"
* Number of estimates to be reported in excel 
local n_vars_for_excel "11"
	
reghdfe dhm ///
	`vars_for_excel' ///
	L.i.dhh_owned L.i.dcpst L.dnc L.dhe_pcs L.ib8.drgn L.i.ydses_c5 ///
	L.dlltsd01 L.dhm ///
	L.dag L.dagsq i.deh_c3 stm ///
	if ${HM2_Females_L_if_condition} [pw=${weight}], ///
	absorb(idperson) vce(cluster idperson)	
		
process_regression, domain("health_mental") process("HM2_Females_L") ///
	sheet("HM2_Females_L") ///
	title("Process HM2_Females_L: Mental health score") ///
	gofrow(7) goflabel("HM2_Females_L ") ///
	ifcond("${HM2_Females_L_if_condition}")	///
	gformula maxestimates(`n_vars_for_excel')

* Save RMSE
scalar rmse = e(rmse)	
putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A14 = ("HM2_Females_L") B14 = rmse 
scalar drop rmse
	

***************************************************************
* HM2_Males_L: GHQ12 Score 0-36 - causal employment effects *
***************************************************************

* Stage 2 - Male

* List of variables to be reported in excel 
local vars_for_excel "ib11.exp_emp i.exp_poverty i.exp_incchange D.log_income financial_distress y2020 y2021"
* Number of estimates to be reported in excel 
local n_vars_for_excel "11"

reghdfe dhm ///
	`vars_for_excel' ///
	L.i.dhh_owned L.i.dcpst L.dnc L.dhe_pcs L.ib8.drgn L.i.ydses_c5 ///
	L.dlltsd01 L.dhm L.dag L.dagsq i.deh_c3 stm ///
	if ${HM2_Males_L_if_condition} [pw=${weight}]  ///
	, absorb(idperson) vce(cluster idperson)	
	
process_regression, domain("health_mental") process("HM2_Males_L") ///
	sheet("HM2_Males_L") ///
	title("Process HM2_Males_L: Mental health score") ///
	gofrow(11) goflabel("HM2_Males_L ") ///
	ifcond("${HM2_Males_L_if_condition}") ///
	gformula maxestimates(`n_vars_for_excel')	

* Save RMSE
scalar rmse = e(rmse)	
putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A15 = ("HM2_Males_L") B15 = rmse 
scalar drop rmse


************************************************************************
* HM1_C: GHQ12 score 0-12 of all working-age adults - baseline effects *
************************************************************************

* New ordered logistic regression model, reflecting observed distributions

ologit scghq2_dv ///
	L.i.dhh_owned L.i.dcpst L.dnc L.dhe_pcs L.ib8.drgn L.i.ydses_c5 ///
	L.dlltsd01 L.scghq2_dv ///
	L.dag L.dagsq i.deh_c3 i.dot i.dgn stm ///
	if ${HM1_C_if_condition} ///
	[pw=${weight}]  ///
	, vce(cluster idperson)

process_ologit, domain("health_mental") process("HM1_C") ///
	sheet("HM1_C") ///
	title("Process HM1_C: Mental health score") ///
	gofrow(15) goflabel("HM1_C ") ///
	ifcond("${HM1_C_if_condition}")
	

***************************************************************
* HM2_Females_C: GHQ12 Score 0-12 - causal employment effects *
***************************************************************

* Kept as linear as adding an 'additional' causal effect on baseline

gen RealIncomeDecrease_D = log_income - L.log_income
gen scghq2_dv_L1 = L.scghq2_dv

* Stage 2 - Female

* List of variables to be reported in excel 
local vars_for_excel "ib11.exp_emp i.exp_poverty i.exp_incchange RealIncomeDecrease_D financial_distress y2020 y2021"
* Number of estimates to be reported in excel 
local n_vars_for_excel "11"

reghdfe scghq2_dv ///
	`vars_for_excel' ///
	i.dhh_owned i.dcpst dnc dhe_pcs ib8.drgn i.ydses_c5 dlltsd01 ///
	dag dagsq i.deh_c3 stm ///
	if ${HM2_Females_C_if_condition} [pw=${weight}] ///
	, absorb(idperson) vce(cluster idperson)
	
process_regression, domain("health_mental") process("HM2_Females_C") ///
	sheet("HM2_Females_C") ///
	title("Process HM2_Females_C: Mental health score") ///
	gofrow(19) goflabel("HM2_Females_C ") ///
	ifcond("${HM2_Females_C_if_condition}")	///
	gformula maxestimates(`n_vars_for_excel')
	
* Save RMSE
scalar rmse = e(rmse)	
putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A17 = ("HM2_Females_C") B17 = rmse 	
scalar drop rmse


*************************************************************
* HM2_Males_C: GHQ12 Score 0-12 - causal employment effects *
*************************************************************

* Stage 2 - Male

* List of variables to be reported in excel 
local vars_for_excel "ib11.exp_emp i.exp_poverty i.exp_incchange RealIncomeDecrease_D financial_distress y2020 y2021"
* Number of estimates to be reported in excel 
local n_vars_for_excel "11"

reghdfe scghq2_dv ///
	`vars_for_excel' ///
	i.dhh_owned i.dcpst dnc dhe_pcs ib8.drgn i.ydses_c5 dlltsd01 ///
	dag dagsq i.deh_c3 stm ///
	if ${HM2_Males_C_if_condition} [pw=${weight}]  ///
	, absorb(idperson) vce(cluster idperson)
	
process_regression, domain("health_mental") process("HM2_Males_C") ///
	sheet("HM2_Males_C") ///
	title("Process HM2_Males_C: Mental health score") ///
	gofrow(23) goflabel("HM2_Males_C ") ///
	ifcond("${HM2_Males_C_if_condition}") ///
	gformula maxestimates(`n_vars_for_excel')

* Save RMSE
scalar rmse = e(rmse)	
putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A18 = ("HM2_Males_C") B18 = rmse 	
scalar drop rmse 	
	

display "Mental health analysis complete!"
	 

capture log close 
	
	