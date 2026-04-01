********************************************************************************
* PROJECT:  		SimPaths UK
* SECTION:			Retirement  
* OBJECT: 			Probit Regresion Models 
* AUTHORS:			Daria Popova, Justin van de Ven, Aleksandra Kolndrekaj
* LAST UPDATE:		18 Feb 2026 AK 
* COUNTRY: 			UK  
*
* NOTES: 			
* 
********************************************************************************
clear all
set more off
set mem 200m
set type double
//set maxvar 120000
set maxvar 30000


/********************************* SET LOG FILE *******************************/

cap log close 
log using "${dir_log}/reg_retirement.log", replace


/********************************* SET EXCEL FILE *****************************/

putexcel set "$dir_results/reg_retirement", sheet("Info") replace
putexcel A1 = "Description:", bold
putexcel B1 = "Model parameters governing projection of retirement"
putexcel A2 = "Authors:	"
putexcel B2 = "Patryk Bronka, Justin van de Ven, Daria Popova, Aleksandra Kolndrekaj" 
putexcel A3 = "Last edit: 26 jan 2026 DP"

putexcel A5 = "Process:", bold
putexcel B5 = "Description:", bold

putexcel A6 = "R1a"
putexcel B6 = "Prob of retiring, singles"

putexcel A7 = "R1b"
putexcel B7 = "Prob of retiring, partnered"

putexcel A10 = "Notes:", bold
//putexcel B10 = ""

putexcel set "$dir_results/reg_retirement", sheet("Gof") modify
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


/****************** R1a: PROBABILITY OF RETIREMENT, SINLGE ********************/
display "${r1a_if_condition}"

probit drtren i.Dgn Dag Dag_sq ///
     li.Deh_c4_Medium li.Deh_c4_Low li.Deh_c4_Na ///
	l.Dhe_pcs l.Dhe_mcs  ///
	i.Reached_Retirement_Age ///
	li.Les_c3_NotEmployed ///
	li.Ydses_c5_Q2 li.Ydses_c5_Q3 li.Ydses_c5_Q4 li.Ydses_c5_Q5 li.Dlltsd01 ///
	$regions Year_transformed Y2020 Y2021 $ethnicity ///
	if ${r1a_if_condition} [pw=${weight}], vce(robust)
	
process_regression, domain("retirement") process("R1a") sheet("R1a") ///
	title("Process R1a: Prob. retire, singles") ///
	gofrow(3) goflabel("R1a - Retire, singles") ///
	ifcond("${r1a_if_condition}") probit	
	
	 
/***************** R1b: PROBABILITY OF RETIREMENT, PARTNERED ******************/
display "${r1b_if_condition}"
	
probit drtren i.Dgn Dag Dag_sq ///
     li.Deh_c4_Medium li.Deh_c4_Low li.Deh_c4_Na ///
	l.Dhe_pcs l.Dhe_mcs  ///
	i.Reached_Retirement_Age i.Reached_Retirement_Age_Les ///
	li.Les_c3_NotEmployed li.Lessp_c3_NotEmployed ///
	i.Reached_Retirement_Age_Sp ///
	li.Ydses_c5_Q2 li.Ydses_c5_Q3 li.Ydses_c5_Q4 li.Ydses_c5_Q5 li.Dlltsd01 ///
	$regions Year_transformed Y2020 Y2021 $ethnicity ///
	if ${r1b_if_condition} [pw=${weight}], vce(robust)	
	
process_regression, domain("retirement") process("R1b") sheet("R1b") ///
	title("Process R1b: Prob. retire, partnered") ///
	gofrow(7) goflabel("R1a - Retire, partnered") ///
	ifcond("${r1b_if_condition}") probit		
	
	
display "Retirement analysis complete!"
	
		
capture log close 	 
	 
