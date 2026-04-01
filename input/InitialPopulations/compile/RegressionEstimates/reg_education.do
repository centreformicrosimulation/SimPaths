/*******************************************************************************
* PROJECT:  		SimPaths UK
* SECTION:			Education
* OBJECT: 			Final Probit & Generalised Logit Models - Weighted
* AUTHORS:			Patryk Bronka, Daria Popova, Justin van de Ven, 
* 					Aleksandra Kolndrekaj
* LAST UPDATE:		26 Mar 2026 (AB)
* COUNTRY: 			UK  
* 
* NOTES: 	                   
*                    
*******************************************************************************/		

clear all
set more off
set mem 200m
set type double
//set maxvar 120000
set maxvar 30000


/********************************* SET LOG FILE *******************************/
cap log close 
log using "${dir_log}/reg_education.log", replace


/******************************* SET EXCEL FILE *******************************/

putexcel set "$dir_results/reg_education", sheet("Info") replace
putexcel A1 = "Description:", bold
putexcel B1 = "Model parameters governing projection of education status"
putexcel A2 = "Authors:"
putexcel B2 = "Patryk Bronka, Justin van de Ven, Daria Popova, Aleksandra Kolndrekaj" "	
putexcel A3 = "Last edit:"
putexcel B3 = "26 Mar 2026 AB"

putexcel A5 = "Process:", bold
putexcel B5 = "Description:", bold

putexcel A6 = "E1a"
putexcel B6 = "Prob. remain in education"

putexcel A7 = "E1b"
putexcel B7 = "Prob. retrun to education"

putexcel A8 = "E2"
putexcel B8 = "Educational attainment when leave education"

putexcel A9 = "E2_raw"
putexcel B9 = "Raw attainment results"

putexcel A11 = "Notes:", bold
putexcel B11 = "Estimation sample: UK_ipop.dta with grossing up weight dwt" 
putexcel B12 = "Conditions for processes are defined as globals in master.do"
//putexcel B13 = "E1a: Compared to the previous version, where age and age squared were used, age is now centered (at age 23) and its effect is allowed to change after age 18." 

putexcel set "$dir_results/reg_education", sheet("Gof") modify
putexcel A1 = "Goodness of fit", bold	


/********************************* PREPARE DATA *******************************/

use "${estimation_sample}", clear

* Set data 
xtset idperson swv
sort idperson swv 

* Adjust variables 
do "${dir_do}/variable_update.do"


/********************************** ESTIMATION ********************************/

* Run Stata programs to produce Excel file 
do "${dir_do}/programs.do" 


/****************** E1a: PROBABILITY OF REMAINING IN EDUCATION ****************/
display "${e1a_if_condition}"	

probit Dst i.Dgn Dag Dag_sq /*Dag_c Dag_c_sq Dag_post18_sq*/  li.Ded  ///
	li.Dehmf_c3_Medium li.Dehmf_c3_Low ///
	li.Ydses_c5_Q2 li.Ydses_c5_Q3 li.Ydses_c5_Q4 li.Ydses_c5_Q5 ///
	$regions Year_transformed Y2020 Y2021 $ethnicity ///
	if ${e1a_if_condition} [pw=${weight}], vce(robust)	
	
process_regression, domain("education") process("E1a") sheet("E1a") ///
	title("Process E1a: Prob. remain in education") ///
	gofrow(3) goflabel("E1a - Remain in education") ///
	ifcond("${e1a_if_condition}") probit	 	
	
 
/****************** E1b: PROBABILITY OF RETURNING TO EDUCATION ****************/
display "${e1b_if_condition}"	

probit der i.Dgn Dag Dag_sq li.Dcpst_Partnered ///
	li.Deh_c4_High li.Deh_c4_Low li.Dehmf_c3_Medium li.Dehmf_c3_Low ///
	li.Les_c3_NotEmployed li.Les_c3_Employed l.Dnc l.Dnc02 ///
	$regions Year_transformed Y2020 Y2021 $ethnicity ///
	if ${e1b_if_condition} [pw=${weight}], vce(robust)

process_regression, domain("education") process("E1b") sheet("E1b") ///
	title("Process E1b: Prob. return to education") ///
	gofrow(7) goflabel("E1b - Return to education") ///
	ifcond("${e1b_if_condition}") probit		


/****************** E2: EDUCATION ATTAINMENT WHEN LEAVE SCHOOL ****************/
display "${e2_if_condition}"	

gologit2 deh_c3_recoded i.Dgn Dag Dag_sq ///
	i.L_Dehmf_c3_Medium i.L_Dehmf_c3_Low ///
	$regions Year_transformed Y2020 Y2021 $ethnicity ///
	if ${e2_if_condition} [pw=${weight}], autofit 

process_gologit, domain("education") process("E2") sheet("E2") ///
    title("Process E2: Educational Attainment When Leave School") ///
    gofrow(11) goflabel("E2 - Education attainment") ///
    outcomes(3) ///
    ifcond("${e2_if_condition}")
		 
		 
display "Education analysis complete!"
		 

capture log close
