********************************************************************************
* PROJECT:  	SimPaths UK
* SECTION:		Health and wellbeing
* OBJECT: 		Health status and Disability
* AUTHORS:		Andy Baxter, Ashley Burdett
* LAST UPDATE:	26 Mar 2026 (AB)
* COUNTRY: 		UK 
*
* NOTES:		
*   - This file updates SF12 MCS and PCS, and Life Satisfaction (7 levels)	     
********************************************************************************
clear all
set more off
set mem 200m
set maxvar 30000


********************************** SET LOG FILE ********************************
cap log close 
log using "${dir_log}/reg_health_wellbeing.log", replace


****************************** SAMPLE GLOBALS **********************************
* For master file 

global DHE_MCS1_if_condition ""
global DHE_MCS2_Females_if_condition "dag >= 25 & dag <= 64 & dgn == 0"
global DHE_MCS2_Males_if_condition "dag >= 25 & dag <= 64 & dgn == 1"
global DHE_PCS1_if_condition ""
global DHE_PCS2_Females_if_condition "dag >= 25 & dag <= 64 & dgn == 0"
global DHE_PCS2_Males_if_condition "dag >= 25 & dag <= 64 & dgn == 1"
global DLS1_if_condition ""
global DLS2_Females_if_condition "dag >= 25 & dag <= 64 & dgn == 0"
global DLS2_Males_if_condition "dag >= 25 & dag <= 64 & dgn == 1"


/******************************* SET EXCEL FILE *******************************/

putexcel set "$dir_results/reg_health_wellbeing", sheet("Info") replace
putexcel A1 = "Description:", bold
putexcel B1 = "Model parameters governing projection of well being"
putexcel A2 = "Authors:"
putexcel B2 = "Andy Baxter, Ashley Burdett" 
putexcel A3 = "Last edit: 26 Mar 2026 (AB)"

putexcel A5 = "Process:", bold
putexcel B5 = "Description:", bold


putexcel A10 = "Notes:", bold


putexcel set "$dir_results/reg_health_wellbeing", sheet("Gof") modify
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


********************************************************************************
* DHE_MCS1 - SF12 MCS score 0-100 of all working-age adults - baseline effects *
********************************************************************************

reg dhe_mcs ///
	L.i.dhh_owned L.i.dcpst L.dnc L.dhe_pcs L.ib8.drgn L.i.ydses_c5 ///
	L.dlltsd01 L.dhe_mcs L.dag L.dagsq i.deh_c3 i.dot i.dgn stm ///
	[pw=${weight}], vce(cluster idperson)

process_regression, domain("health_wellbeing") process("DHE_MCS1") ///
	sheet("DHE_MCS1") title("Process DHE_MCS1: Well-being") ///
	gofrow(3) goflabel("DHE_MCS1 ") ///
	ifcond("${DHE_MCS1_if_condition}")	

* Save RMSE	
scalar rmse = e(rmse)	
putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A18 = ("DHE_MCS1") B18 = rmse 	
scalar drop rmse	
	

**********************************************************************
* DHE_MCS2_Females: SF12 MCS score 0-100 - causal employment effects *
**********************************************************************

* Stage 2 - Female

* List of variables to be reported in excel 
local vars_for_excel "ib11.exp_emp i.exp_poverty i.exp_incchange D.log_income financial_distress y2020 y2021"
* Number of estimates to be reported in excel 
local n_vars_for_excel "11"

reghdfe dhe_mcs ///
	`vars_for_excel' ///
	L.i.dhh_owned L.i.dcpst L.dnc L.dhe_pcs L.ib8.drgn L.i.ydses_c5 ///
	L.dlltsd01 L.dhe_mcs L.dag L.dagsq i.deh_c3 stm ///
	if ${DHE_MCS2_Females_if_condition} [pw=${weight}]  ///
	, absorb(idperson) vce(cluster idperson)  

process_regression, domain("health_wellbeing") process("DHE_MCS2_Females") ///
	sheet("DHE_MCS2_Females") ///
	title("Process DHE_MCS2_Females: Well-being health score") ///
	gofrow(7) goflabel("DHE_MCS2_Females ") ///
	ifcond("${DHE_MCS2_Females_if_condition}") ///
	gformula maxestimates(`n_vars_for_excel')	

* Save RMSE
scalar rmse = e(rmse)	
putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A19 = ("DHE_MCS2_Females") B19 = rmse 
scalar drop rmse	
	

********************************************************************
* DHE_MCS2_Males: SF12 MCS score 0-100 - causal employment effects *
********************************************************************

* Stage 2 - Male

* List of variables to be reported in excel 
local vars_for_excel "ib11.exp_emp i.exp_poverty i.exp_incchange D.log_income financial_distress y2020 y2021"
* Number of estimates to be reported in excel 
local n_vars_for_excel "11"


reghdfe dhe_mcs ///
	`vars_for_excel' ///
	L.i.dhh_owned L.i.dcpst L.dnc L.dhe_pcs L.ib8.drgn L.i.ydses_c5 ///
	L.dlltsd01 L.dhe_mcs L.dag L.dagsq i.deh_c3 stm ///
	if ${DHE_MCS2_Males_if_condition} [pw=${weight}]  ///
	, absorb(idperson) vce(cluster idperson)

process_regression, domain("health_wellbeing") process("DHE_MCS2_Males") ///
	sheet("DHE_MCS2_Males") ///
	title("Process DHE_MCS2_Males: Well-being health score") ///
	gofrow(11) goflabel("DHE_MCS2_Males ") ///
	ifcond("${DHE_MCS2_Males_if_condition}") ///
	gformula maxestimates(`n_vars_for_excel')	

* Save RMSE
scalar rmse = e(rmse)	
putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A20 = ("DHE_MCS2_Males") B20 = rmse 
scalar drop rmse	
	

********************************************************************************
* DHE_PCS1 - SF12 PCS score 0-100 of all working-age adults - baseline effects *
********************************************************************************

reg dhe_pcs ///
	L.i.dhh_owned L.i.dcpst L.dnc L.dhe_mcs L.ib8.drgn L.i.ydses_c5 ///
	L.dlltsd01 L.dhe_pcs L.dag L.dagsq i.deh_c3 i.dot i.dgn stm ///
	[pw=${weight}], vce(cluster idperson)

process_regression, domain("health_wellbeing") process("DHE_PCS1") ///
	sheet("DHE_PCS1") ///
	title("Process DHE_PCS1: Well-being health score") ///
	gofrow(15) goflabel("DHE_PCS1 ") ///
	ifcond("${DHE_PCS1_if_condition}")	

* Save RMSE	
scalar rmse = e(rmse)	
putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A21 = ("DHE_PCS1") B21 = rmse 	
scalar drop rmse
		

**********************************************************************
* DHE_PCS2_Females: SF12 PCS score 0-100 - causal employment effects *
**********************************************************************

* Stage 2 - Female

* List of variables to be reported in excel 
local vars_for_excel "ib11.exp_emp i.exp_poverty i.exp_incchange D.log_income financial_distress y2020 y2021"
* Number of estimates to be reported in excel 
local n_vars_for_excel "11"

reghdfe dhe_pcs ///
	`vars_for_excel' ///
	L.i.dhh_owned L.i.dcpst L.dnc L.dhe_mcs L.ib8.drgn L.i.ydses_c5 ///
	L.dlltsd01 L.dhe_pcs L.dag L.dagsq i.deh_c3 stm ///
	if ${DHE_PCS2_Females_if_condition} ///
	[pw=${weight}]  ///
	, absorb(idperson) vce(cluster idperson)

process_regression, domain("health_wellbeing") process("DHE_PCS2_Females") ///
	sheet("DHE_PCS2_Females") ///
	title("Process DHE_PCS2_Females: Well-being health score") ///
	gofrow(19) goflabel("DHE_PCS2_Females ") ///
	ifcond("${DHE_PCS2_Females_if_condition}")	///
	gformula maxestimates(`n_vars_for_excel')

* Save RMSE
scalar rmse = e(rmse)	
putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A22 = ("DHE_PCS2_Females") B22 = rmse 
scalar drop rmse
	

********************************************************************
* DHE_PCS2_Males: SF12 PCS score 0-100 - causal employment effects *
********************************************************************

* Stage 2 - Male

* List of variables to be reported in excel 
local vars_for_excel "ib11.exp_emp i.exp_poverty i.exp_incchange D.log_income financial_distress y2020 y2021"
* Number of estimates to be reported in excel 
local n_vars_for_excel "11"

reghdfe dhe_pcs ///
	`vars_for_excel' ///
	L.i.dhh_owned L.i.dcpst L.dnc L.dhe_mcs L.ib8.drgn L.i.ydses_c5 ///
	L.dlltsd01 L.dhe_pcs L.dag L.dagsq i.deh_c3 stm ///
	if ${DHE_PCS2_Males_if_condition} [pw=${weight}]  ///
	, absorb(idperson) vce(cluster idperson)

process_regression, domain("health_wellbeing") process("DHE_PCS2_Males") ///
	sheet("DHE_PCS2_Males") ///
	title("Process DHE_PCS2_Males: Well-being health score") ///
	gofrow(23) goflabel("DHE_PCS2_Males ") ///
	ifcond("${DHE_PCS2_Males_if_condition}")	///
	gformula maxestimates(`n_vars_for_excel')

* Save RMSE
scalar rmse = e(rmse)	
putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A23 = ("DHE_PCS2_Males") B23 = rmse 
scalar drop rmse	
	

*****************************************************************************
* DLS1 - Life Satisfaction 1-7 of all working-age adults - baseline effects *
*****************************************************************************

reg dls ///
	L.i.dhh_owned L.i.dcpst L.dnc L.dhe_pcs L.ib8.drgn L.i.ydses_c5 ///
	L.dlltsd01 L.dls L.dag L.dagsq i.deh_c3 i.dot i.dgn stm ///
	[pw=${weight}], vce(cluster idperson)
	
process_regression, domain("health_wellbeing") process("DLS1") sheet("DLS1") ///
	title("Process DLS1: Well-being health score") ///
	gofrow(27) goflabel("DLS1 ") ///
	ifcond("${DLS1_if_condition}")	

* Save RMSE	
scalar rmse = e(rmse)	
putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A24 = ("DLS1") B24 = rmse 	
scalar drop rmse
		
	
*******************************************************************
* DLS2_Females: Life Satisfaction 1-7 - causal employment effects *
*******************************************************************

* Stage 2 - Female

* List of variables to be reported in excel 
local vars_for_excel "ib11.exp_emp i.exp_poverty i.exp_incchange D.log_income financial_distress y2020 y2021"
* Number of estimates to be reported in excel 
local n_vars_for_excel "11"

reghdfe dls ///
	`vars_for_excel' ///
	L.i.dhh_owned L.i.dcpst L.dnc L.dhe_pcs L.ib8.drgn L.i.ydses_c5 ///
	L.dlltsd01 L.dls L.dag L.dagsq i.deh_c3 stm ///
	if ${DLS2_Females_if_condition} [pw=${weight}]  ///
	, absorb(idperson) vce(cluster idperson)

process_regression, domain("health_wellbeing") process("DLS2_Females") ///
	sheet("DLS2_Females") ///
	title("Process DLS2_Females: Well-being health score") ///
	gofrow(31) goflabel("DLS2_Females ") ///
	ifcond("${DLS2_Females_if_condition}")	///
	gformula maxestimates(`n_vars_for_excel')

* Save RMSE
scalar rmse = e(rmse)	
putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A25 = ("DLS2_Females") B25 = rmse 
scalar drop rmse
	

*****************************************************************
* DLS2_Males: Life Satisfaction 1-7 - causal employment effects *
*****************************************************************

* Stage 2 - Male

* List of variables to be reported in excel 
local vars_for_excel "ib11.exp_emp i.exp_poverty i.exp_incchange D.log_income financial_distress y2020 y2021"
* Number of estimates to be reported in excel 
local n_vars_for_excel "11"

reghdfe dls ///
	`vars_for_excel' ///
	L.i.dhh_owned L.i.dcpst L.dnc L.dhe_pcs L.ib8.drgn L.i.ydses_c5 ///
	L.dlltsd01 L.dls L.dag L.dagsq i.deh_c3 stm ///
	if ${DLS2_Males_if_condition} [pw=${weight}]  ///
	, absorb(idperson) vce(cluster idperson)

process_regression, domain("health_wellbeing") process("DLS2_Males") ///
	sheet("DLS2_Males") ///
	title("Process DLS2_Males: Well-being health score") ///
	gofrow(35) goflabel("DLS2_Males ") ///
	ifcond("${DLS2_Males_if_condition}")	///
	gformula maxestimates(`n_vars_for_excel')

* Save RMSE
scalar rmse = e(rmse)	
putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A26 = ("DLS2_Males") B26 = rmse 
scalar drop rmse


display "Well-being analysis complete!"
	 

capture log close 
	
	