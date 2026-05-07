********************************************************************************
* PROJECT:  		SimPaths UK 
* SECTION:			Unions
* OBJECT: 			Final Probit Models
* AUTHORS:			Daria Popova, Justin van de Ven, Aleksandra Kolndrekaj, Ashley Burdett
* LAST UPDATE:		15 April 2026 (DP) 
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
putexcel B2 = "Patryk Bronka, Justin van de Ven, Daria Popova, Aleksandra Kolndrekaj, Ashley Burdett" 	
putexcel A3 = "Last edit: 15 April 2026 (DP)"

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

probit dcpen ///
       eduSampleFlag demMaleFlag demAge demAgeSq ///
       demNChildL1 demNChild0to2L1 ///
       yHhQuintilesMonthC5Q2L1 yHhQuintilesMonthC5Q3L1 yHhQuintilesMonthC5Q4L1 yHhQuintilesMonthC5Q5L1 ///
       eduSampleFlag_demMaleFlag eduSampleFlag_demNChildL1 eduSampleFlag_demNChild0to2L1 ///
	   eduSampleFlag_Q2L1   eduSampleFlag_Q3L1  eduSampleFlag_Q4L1  eduSampleFlag_Q5L1 ///
	   eduHighestC4NaL1 eduHighestC4HighL1 eduHighestC4MediumL1 eduHighestC4LowL1 ///
	   labStatusC4EmployedL1 labStatusC4StudentL1 labStatusC4RetiredL1 /// 
       labStatusC4EmployedL1_Male labStatusC4StudentL1_Male labStatusC4RetiredL1_Male ///
       healthPhysicalPcsL1 healthMentalMcsL1  ///
	   $regions demYear demYear2020 demYear2021 $ethnicity /// 
    if ${u1_if_condition} [pw=${weight}], vce(robust)
	
process_regression, domain("partnership") process("U1") sheet("U1") ///
	title("Process U1: Prob. form partnership") ///
	gofrow(3) goflabel("U1 - Form partnership") ///
	ifcond("${u1_if_condition}") probit	
	

/******************* U2: PROBABILITY TERMINATE PARTNERSHIP ********************/
display "${u2_if_condition}"	
	
probit dcpex ///
      eduSampleFlag demMaleFlag demAge demAgeSq ///
      eduHighestC4NaL1 eduHighestC4HighL1 eduHighestC4MediumL1 eduHighestC4LowL1 ///
      eduHighestPartnerC3MediumL1 eduHighestPartnerC3LowL1 ///
      healthPhysicalPcsL1 healthMentalMcsL1  ///
	  healthPhysicalPartnerPcsL1 healthMentalPartnerMcsL1 ///
	  demPartnerNYearL1 demEnterPartnerFlagL1 demAgePartnerDiffL1 ///
	  demNChildL1 demNChild0to2L1 ///
	  labStatusPartnerAndOwnC42L1 labStatusPartnerAndOwnC43L1 labStatusPartnerAndOwnC44L1 ///
	  yNonBenPersGrossMonthL1 yPersAndPartnerGrossDiffMonthL1 ///
	  $regions demYear demYear2020 demYear2021 $ethnicity /// 
	if ${u2_if_condition} [pw=${weight}], vce(robust)

process_regression, domain("partnership") process("U2") sheet("U2") ///
	title("Process U2: Prob. end partnership") ///
	gofrow(7) goflabel("U2 - End partnership") ///
	ifcond("${u2_if_condition}") probit	
		
		
display "Partnership analysis complete!"


capture log close 

