********************************************************************************
* PROJECT:  		SimPaths UK 
* SECTION:			Unions
* OBJECT: 			Final Probit Models
* AUTHORS:			Daria Popova, Justin van de Ven
* LAST UPDATE:		4 Feb 2026 DP  
* COUNTRY: 			UK  
* 
*NOTES: 			
* 					Combined former a and b processes.                 	
********************************************************************************

clear all
set more off
set mem 200m
set type double
//set maxvar 120000
set maxvar 30000

/********************************* SET LOG FILE *******************************/
cap log close 
log using "${dir_log}/reg_partnership.log", replace


/********************************* SET EXCEL FILE *****************************/

putexcel set "$dir_results/reg_partnership", sheet("Info") replace
putexcel A1 = "Description:", bold
putexcel B1 = "Model parameters for relationship status projection"
putexcel A2 = "Authors:"
putexcel B2 = "Patryk Bronka, Justin van de Ven, Daria Popova, Aleksandra Kolndrekaj" 	
putexcel A3 = "Last edit: 18 Feb 2026 AK"

putexcel A5 = "Process:", bold
putexcel B5 = "Description:", bold
putexcel A6 = "U1 "
putexcel B6 = "Prob enter partnership"
putexcel A7 = "U2"
putexcel B7 = "Prob exit partnership"

putexcel A10 = "Notes:", bold
putexcel B10 = "Estimation sample: UK_ipop.dta with grossing up weight dwt" 
putexcel B11 = "Conditions for processes are defined as globals in master.do"
putexcel B12 = "Combined former processes U1a and U1b"

putexcel set "$dir_results/reg_partnership", sheet("Gof") modify
putexcel A1 = "Goodness of fit", bold		


/********************************* PREPARE DATA *******************************/

* Load data 
use "${estimation_sample}", clear

* Set data 
xtset idperson swv
sort idperson swv 

* Adjust variables 
do "${dir_do}/variable_update.do"

* Run Stata programs to produce Excel file 
do "${dir_do}/programs.do" 

/********************************** ESTIMATION ********************************/

/******************** U1: PROBABILITY FORMING PARTNERSHIP *********************/
display "${u1_if_condition}"	

probit dcpen i.Ded Dgn Dag Dag_sq lc.Dnc lc.Dnc02 ///
    li.Ydses_c5_Q2 li.Ydses_c5_Q3 li.Ydses_c5_Q4 li.Ydses_c5_Q5 ///
	/*Ded_Dag Ded_Dag_sq*/ Ded_Dgn Ded_Dnc_L1 Ded_Dnc02_L1 ///
	Ded_Ydses_c5_Q2_L1 Ded_Ydses_c5_Q3_L1 Ded_Ydses_c5_Q4_L1 ///
	Ded_Ydses_c5_Q5_L1  i.Deh_c4_Na i.Deh_c4_High i.Deh_c4_Medium ///
	i.Deh_c4_Low li.Les_c4_Student li.Les_c4_NotEmployed li.Les_c4_Retired /// 
	li.Les_c4_Student_Dgn li.Les_c4_NotEmployed_Dgn ///
	li.Les_c4_Retired_Dgn ///
	l.Dhe_pcs l.Dhe_mcs ///
	$regions Year_transformed Y2020 Y2021 $ethnicity ///
	if ${u1_if_condition} [pw=${weight}], vce(robust)

process_regression, domain("partnership") process("U1") sheet("U1") ///
	title("Process U1: Prob. form partnership") ///
	gofrow(3) goflabel("U1 - Form partnership") ///
	ifcond("${u1_if_condition}") probit	
	

/******************* U2: PROBABILITY TERMINATE PARTNERSHIP ********************/
display "${u2_if_condition}"	
	
probit dcpex i.Ded Dag Dag_sq /*Ded_Dag Ded_Dag_sq*/ ///
    li.Deh_c4_Na li.Deh_c4_Low  li.Deh_c4_Medium li.Deh_c4_High ///
	li.Dehsp_c3_Medium li.Dehsp_c3_Low ///
	li.Dhe_Fair li.Dhe_Good li.Dhe_VeryGood li.Dhe_Excellent  ///
	l.Dhe_pcs l.Dhe_mcs ///
	l.Dhe_pcssp l.Dhe_mcssp ///
	l.Dcpyy l.New_rel l.Dcpagdf l.Dnc l.Dnc02 ///
	li.Lesdf_c4_EmpSpouseNotEmp li.Lesdf_c4_NotEmpSpouseEmp ///
	li.Lesdf_c4_BothNotEmployed ///
	l.Ypnbihs_dv l.Ynbcpdf_dv ///
	$regions Year_transformed Y2020 Y2021 $ethnicity ///
	if ${u2_if_condition} [pw=${weight}], vce(robust)

process_regression, domain("partnership") process("U2") sheet("U2") ///
	title("Process U2: Prob. end partnership") ///
	gofrow(7) goflabel("U2 - End partnership") ///
	ifcond("${u2_if_condition}") probit	
		
		
display "Partnership analysis complete!"


capture log close 

