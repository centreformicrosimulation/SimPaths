********************************************************************************
* PROJECT:  		SimPaths UK
* SECTION:			Leaving Parental Home
* OBJECT: 			Final Probit Regression Model 
* AUTHORS:			Daria Popova, Justin van de Ven, Aleksandra Kolndrekaj
* LAST UPDATE:		18 Feb 2026 AK  
* COUNTRY: 			UK  
* 
* NOTES: 			
**********************************************************************************

clear all
set more off
set mem 200m
set type double
//set maxvar 120000
set maxvar 30000


/********************************* SET LOG FILE *******************************/
cap log close 
log using "${dir_log}/reg_leave_parental_home.log", replace


/********************************* SET EXCEL FILE *****************************/

putexcel set "$dir_results/reg_leave_parental_home", sheet("Info") replace
putexcel A1 = "Description:", bold
putexcel B1 = "Model parameters governing leaving parental home"
putexcel A2 = "Authors:"
putexcel B2 = "Patryk Bronka, Justin van de Ven, Daria Popova, Aleksandra Kolndrekaj"
putexcel A3 = "Last edit: 19 Jan 2026 DP"

putexcel A5 = "Process:", bold
putexcel B5 = "Description:", bold
putexcel A6 = "P1a"
putexcel B6 = "Prob. leave the parental home, transitioning out of adult child status"

putexcel A10 = "Notes:", bold
putexcel B10 = "Estimation sample: UK_ipop.dta with grossing up weight dwt" 
putexcel B11 = "Conditions for processes are defined as globals in master.do"

putexcel set "$dir_results/reg_leave_parental_home", sheet("Gof") modify
putexcel A1 = "Goodness of fit", bold		


/********************************* PREPARE DATA *******************************/

* Load data
use "${estimation_sample}", clear

* Set data 
xtset idperson swv
sort idperson swv 

* Adjust variables 
do "${dir_do}/variable_update.do"


/********************************** ESTIMATION ********************************/

* Run Stata programs to produce Excel file 
do "${dir_do}/programs.do" 


/**************** P1: PROBABILITY OF LEAVING THE PARENTAL HOME ****************/
display "${p1_if_condition}"
	
probit dlftphm i.Dgn Dag Dag_sq li.Deh_c4_Na li.Deh_c4_Medium li.Deh_c4_Low ///
	li.Les_c3_Student li.Les_c3_NotEmployed ///
	li.Ydses_c5_Q2 li.Ydses_c5_Q3 li.Ydses_c5_Q4 li.Ydses_c5_Q5 ///
	$regions Year_transformed Y2020 Y2021 $ethnicity ///
	if ${p1_if_condition} [pw=${weight}], vce(robust)

process_regression, domain("leave_parental_home") process("P1") sheet("P1") ///
	title("Process P1: Prob. leave parental home") ///
	gofrow(3) goflabel("P1 - Leave parental home") ///
	ifcond("${p1_if_condition}") probit	
	
	
display "Leaving parental home analysis complete!"
	
	
cap log close 
