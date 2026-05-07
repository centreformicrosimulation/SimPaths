*********************************************************************************
* PROJECT:  		SimPaths UK 
* SECTION:			Fertility
* OBJECT: 			Final Probit Models
* AUTHORS:			Daria Popova, Justin van de Ven, Aleksandra Kolndrekaj
* LAST UPDATE:		15 April 2026 (DP) 
* COUNTRY: 			UK 
*
* NOTES:			     
* 						Combined former a and b processes.
********************************************************************************
clear all
set more off
set mem 200m
set type double
//set maxvar 120000
set maxvar 30000


/********************************* SET LOG FILE *******************************/
cap log close 
log using "${dir_log}/reg_fertility.log", replace


/******************************* SET EXCEL FILE *******************************/

putexcel set "$dir_results/reg_fertility", sheet("Info") replace
putexcel A1 = "Description:", bold
putexcel B1 = "Model parameters governing projection of fertility"
putexcel A2 = "Authors:"
putexcel B2 = "Patryk Bronka, Justin van de Ven, Daria Popova, Aleksandra Kolndrekaj, Ashley Burdett" 
putexcel A3 = "Last edit: 15 April 2026 (DP) "

putexcel A5 = "Process:", bold
putexcel B5 = "Description:", bold
putexcel A6 = "F1"
putexcel B6 = "Prob have a child for women"

putexcel A10 = "Notes:", bold
putexcel B10 = "Estimation sample: UK_ipop.dta with grossing up weight dwt" 
putexcel B11 = "Conditions for processes are defined as globals in master.do"
putexcel B12 = "Combined former processes F1a and F1b"

putexcel set "$dir_results/reg_fertility", sheet("Gof") modify
putexcel A1 = "Goodness of fit", bold		


/********************************* PREPARE DATA *******************************/

* Load data 
use "${estimation_sample}", clear

* Set data 
xtset idperson swv
sort idperson swv 

* Adjust variables 
do "${dir_do}/variable_update.do"

* Any-children dummy (dchpd collapsing)
replace dchpd = 1 if inlist(dchpd, 2, 3, 4, 5)
fre dchpd 


/********************************* ESTIMATION *********************************/

* Run Stata programs to produce Excel file 
do "${dir_do}/programs.do" 

/*********************** F1: PROBABILITY OF HAVING A CHILD ********************/
display "${f1_if_condition}" 

probit dchpd ///
	eduSampleFlag demMaleFlag demAge demAgeSq ///
	healthPhysicalPcsL1 healthMentalMcsL1 ///
	demPartnerStatusSingle demPartnerStatusSingleL1 ///
	eduSampleFlag_Single ///
	yHhQuintilesMonthC5Q2L1 yHhQuintilesMonthC5Q3L1 yHhQuintilesMonthC5Q4L1 yHhQuintilesMonthC5Q5L1 ///
	demNChildL1 demNChild0to2L1 ///
	eduHighestC4HighL1 eduHighestC4MediumL1 eduHighestC4LowL1 ///
	fertilityRate ///
	/*labStatusC3StudentL1*/ labStatusC3NotEmployedL1 ///
	$regions demYear demYear2020 demYear2021 $ethnicity /// 
if ${f1_if_condition} [pw=${weight}], vce(robust)

process_regression, domain("fertility") process("F1") sheet("F1") ///
	title("Process F1: Prob. have a child") ///
	gofrow(3) goflabel("F1 - Have child") ///
	ifcond("${f1_if_condition}") probit	 
	 

display "Fertility analysis complete!"
	 

capture log close 
