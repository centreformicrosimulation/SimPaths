********************************************************************************
* PROJECT:		UC and mental health
* SECTION:		Health and wellbeing
* OBJECT: 		Financial distress
* AUTHORS:		Andy Baxter, Erik Igelström
* LAST UPDATE:	17 Feb 2026  
* COUNTRY:		UK 
*
* NOTES:		
********************************************************************************
clear all
set more off
set mem 200m
set maxvar 30000

/********************************* SET LOG FILE *******************************/
cap log close 
log using "${dir_log}/reg_financial_distress.log", replace


****************************** SAMPLE GLOBALS **********************************
* For master file

global HM1_L_if_condition ""


/******************************* SET EXCEL FILE *******************************/

putexcel set "$dir_results/reg_financial_distress", sheet("Info") replace
putexcel A1 = "Description:", bold
putexcel B1 = "Model parameters governing projection of financial distress"
putexcel A2 = "Authors:"
putexcel B2 = "Andy Baxter, Erik Igelström"
putexcel A3 = "Last edit: 17 Feb 2026"

putexcel A5 = "Process:", bold
putexcel B5 = "Description:", bold


putexcel A10 = "Notes:", bold


putexcel set "$dir_results/reg_financial_distress", sheet("Gof") modify
putexcel A1 = "Goodness of fit", bold	


/********************************* PREPARE DATA *******************************/

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

logit financial_distress ///
	ib11.exp_emp  i.lhw_c5 D.log_income i.exp_incchange ///
	ib0.exp_poverty L.ypncp L.ypnoab ///
	L.i.econ_benefits L.i.dhh_owned L.i.dcpst L.dnc L.dhe_pcs L.dhe_mcs ///
	L.ib8.drgn L.i.ydses_c5 L.dlltsd01  L.financial_distress ///
	i.dgn L.dag L.dagsq i.deh_c3 i.dot stm ///
	[pw=${weight}]  ///
	, vce(cluster idperson)

	
process_regression, domain("financial_distress") process("HM1_L") ///
	sheet("HM1_L") ///
	title("Process HM1_L: Financial distress") ///
	gofrow(3) goflabel("HM1_L - Financial distress") ///
	ifcond("${HM1_L_if_condition}") probit	 		
	

display "Financial distress analysis complete!"
		 

capture log close
	