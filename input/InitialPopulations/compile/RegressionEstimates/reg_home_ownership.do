********************************************************************************
* PROJECT:  		SimPaths UK 
* SECTION:			Home ownership 
* OBJECT: 			Final Regresion Models - Weighted
* AUTHORS:			Daria Popova, Justin van de Ven, Aleksandra Kolndrekaj
* LAST UPDATE:		18 Feb 2026 AK  
* COUNTRY: 			UK
*
* NOTES: 			Re-estimated process at benefit unit level to be consistent with SimPaths 
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
log using "${dir_log}/reg_home_ownership.log", replace


/********************************* SET EXCEL FILE *****************************/

putexcel set "$dir_results/reg_home_ownership", sheet("Info") replace
putexcel A1 = "Description:", bold
putexcel B1 = "Model parameters governing projection of home ownership"
putexcel A2 = "Authors:	"
putexcel B2 = "Patryk Bronka, Justin van de Ven, Daria Popova, Aleksandra Kolndrekaj" 
putexcel A3 = "Last edit: 18 Feb 2026 AK"

putexcel A5 = "Process:", bold
putexcel B5 = "Description:", bold
putexcel A6 = "HO1"
putexcel B6 = "Prob. of being a home owner"

putexcel A10 = "Notes:", bold
putexcel B10 = "Estimation sample: UK_ipop.dta with grossing up weight dwt" 
putexcel B11 = "Conditions for processes are defined as globals in master.do"
putexcel B12 = "Re-estimated process at benefit unit level to be consistent with SimPaths"

putexcel set "$dir_results/reg_home_ownership", sheet("Gof") modify
putexcel A1 = "Goodness of fit", bold		


/********************************* PREPARE DATA *******************************/

* Load data 
use "${estimation_sample}", clear

* Set data 
xtset idperson swv
sort idperson swv 

* Adjust variables 
do "${dir_do}/variable_update.do"


* Create sample at benefit unit head 

* Keep adults (18+)
keep if dag >= 18

* Count unique benefit-unit–wave combinations BEFORE head selection
egen tag_bu_wave = tag(idbenefitunit swv)
count if tag_bu_wave
local n_bu_before = r(N)
display "Number of benefit unit–wave combinations BEFORE selecting head: `n_bu_before'"

* Sort benefit unit members within each wave:
* 1. Highest non-benefit income (ypnbihs_dv)
* 2. Highest age (dag)
* 3. Lowest idperson (idperson)
gsort idbenefitunit swv -ypnbihs_dv -dag idperson 

* Tag the first person (the "head") per benefit unit and wave
bysort idbenefitunit swv: gen benunit_head = (_n == 1)

* Keep only benefit unit heads
keep if benunit_head == 1

* Count unique benefit-unit–wave combinations AFTER head selection
drop tag_bu_wave
egen tag_bu_wave = tag(idbenefitunit swv)
count if tag_bu_wave
local n_bu_after = r(N)
display "Number of benefit unit–wave combinations AFTER selecting head: `n_bu_after'"

* Ensure benefit unit–wave counts match before and after head selection
assert `n_bu_before' == `n_bu_after'

* Verify only one head per benefit unit per wave
by idbenefitunit swv, sort: gen n=_N
assert n==1

sort idperson swv 


/********************************** ESTIMATION ********************************/

* Run Stata programs to produce Excel file 
do "${dir_do}/programs.do" 


/********************** HO1: PROBABILITY OF OWNING HOME ***********************/
display "${ho1_if_condition}" 

probit dhh_owned i.Dgn Dag Dag_sq ///
    il.Dhhtp_c8_2 il.Dhhtp_c8_3 il.Dhhtp_c8_4 il.Dhhtp_c8_5 il.Dhhtp_c8_6 ///
	il.Dhhtp_c8_7 il.Dhhtp_c8_8 ///
	il.Les_c4_Student il.Les_c4_NotEmployed il.Les_c4_Retired  ///
	i.Deh_c4_Medium i.Deh_c4_Low i.Deh_c4_Na ///
	l.Dhe_mcs l.Dhe_pcs ///
	li.Ydses_c5_Q2 li.Ydses_c5_Q3 li.Ydses_c5_Q4 li.Ydses_c5_Q5 ///
	l.Yptciihs_dv ///
	l.Dhh_owned ///
	$regions Year_transformed Y2020 Y2021 $ethnicity ///
	if ${ho1_if_condition} [pw=${weight}], vce(cluster idperson)

process_regression, domain("home_ownership") process("HO1") sheet("HO1") ///
	title("Process S2b: Prob. own home") ///
	gofrow(3) goflabel("HO1 - Own home") ///
	ifcond("${ho1_if_condition}") probit	
	
	
display "Home ownership analysis complete!"
	
	
capture log close 	
	
	