********************************************************************************
* PROJECT:  		SimPaths UK 
* SECTION:			Health
* OBJECT: 			Health status and Disability
* AUTHORS:			Daria Popova, Justin van de Ven, Aleksandra Kolndrekaj, 
* 					Ashley Burdett
* LAST UPDATE:		15 April 2026 (DP) 
* COUNTRY: 			UK 
*
* NOTES:	  Combined former a and b processes.
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
log using "${dir_log}/reg_health.log", replace


/******************************* SET EXCEL FILE *******************************/

putexcel set "$dir_results/reg_health", sheet("Info") replace
putexcel A1 = "Description:", bold
putexcel B1 = "Model parameters governing projection self-reported health status"
putexcel A2 = "Authors:" 
putexcel B2 = "Justin van de Ven, Daria Popova, Aleksandra Kolndrekaj, Ashley Burdett" 
putexcel A3 = "Last edit: 15 April 2026 (DP) "

putexcel A5 = "Process:", bold
putexcel B5 = "Description:", bold

putexcel A6 = "H1"
putexcel B6 = "Self rated health (5 cat)"
putexcel B7 = "Covariates that satisfy the parallel lines assumption have one estimate for all categories of the dependent variable and are present once in the table"
putexcel B8 = "Covariates that do not satisfy the parallel lines assumption have an estimate for each estimated category of the dependent variable. These covariates have the dependent variable category appended to their name."

putexcel A9 = "H1_raw"
putexcel B9 = "elf rated health (5 cat) - unformatted output"

putexcel A10 = "H2"
putexcel B10 = "Prob. long-term sick or disabled"

putexcel A15 = "Notes:", bold
putexcel B15 = "Estimation sample: UK_ipop.dta with grossing up weight dwt" 
putexcel B16 = "Conditions for processes are defined as globals in master.do"
putexcel B17 = "Combined former processes H1a and H1b"

putexcel set "$dir_results/reg_health", sheet("Gof") modify
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


/********************** H1: SELF-REPORTED HEALTH STATUS ***********************/
display "${h1_if_condition}"

gologit2 dhe ///
         eduSampleFlag demMaleFlag demAge demAgeSq ///  
		 healthPhysicalPcsL1 healthMentalMcsL1 ///
		 eduHighestC4NaL1 eduHighestC4MediumL1 eduHighestC4LowL1 ///
		 /*labStatusC4StudentL1*/ labStatusC4EmployedL1 labStatusC4RetiredL1 /// 
		 yHhQuintilesMonthC5Q2L1 yHhQuintilesMonthC5Q3L1 yHhQuintilesMonthC5Q4L1 yHhQuintilesMonthC5Q5L1 ///
		 demCompHhC4CoupleChL1 demCompHhC4SingleNoChL1 demCompHhC4L1SingleChL1 ///
		 healthDsblLongtermFlagL1 ///
		 $regions demYear demYear2020 demYear2021 $ethnicity /// 
	 if ${h1_if_condition} [pw=${weight}], autofit
  
/* 
Note: In gologit2, the coefficients show how covariates affect the log-odds of
being above a certain category vs. at or below it.
*/

process_gologit, domain("health") process("H1") sheet("H1") ///
    title("Process H1: Self Rated Health") ///
    gofrow(3) goflabel("H1 - Self-rated health") ///
    outcomes(5) ///
    ifcond("${h1_if_condition}")

/**************** H2: PROBABILITY LONG-TERM SICK OR DISABLED ******************/
display "${h2_if_condition}"

probit dlltsd01 demMaleFlag demAge demAgeSq ///
       eduHighestC4Medium eduHighestC4Low eduHighestC4Na ///
	   yHhQuintilesMonthC5Q2L1 yHhQuintilesMonthC5Q3L1 yHhQuintilesMonthC5Q4L1 yHhQuintilesMonthC5Q5L1 ///
	   healthPhysicalPcsL1 healthMentalMcsL1 ///
	   healthDsblLongtermFlagL1 ///
	   demCompHhC4CoupleChL1 demCompHhC4SingleNoChL1 demCompHhC4L1SingleChL1 ///
	   $regions demYear demYear2020 demYear2021 $ethnicity /// 
	if ${h2_if_condition} [pw=${weight}], vce(robust)
	
process_regression, domain("health") process("H2") sheet("H2") ///
	title("Process H2: Prob.disabled or long term sick") ///
	gofrow(7) goflabel("H2 - Disabled or long term sick") ///
	ifcond("${h2_if_condition}") probit	 	
		
	
display "Self-rated health analysis complete!"
		 

capture log close
