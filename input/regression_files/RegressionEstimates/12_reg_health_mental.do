/****************************************************************************/
* PROJECT:  		SimPaths UK
* SECTION:		Mental Health
* OBJECT: 			Final Regresion Models 
* AUTHORS:			Andy Baxter
* LAST UPDATE:		3 September 2026 (AB) 
* COUNTRY: 			UK
******************************************************************************/

clear all
set more off
set mem 200m
set type double
//set maxvar 120000
set maxvar 30000


/********************************* SET LOG FILE *******************************/
cap log close 
log using "${dir_log}/reg_health_mental.log", replace


/********************************* SET EXCEL FILE *****************************/

putexcel set "$dir_results/reg_health_mental", sheet("Info") modify //replace
putexcel A1 = "Description:", bold
putexcel B1 = "This file contains regression estimates used by mental health (HM*) processes"
putexcel A2 = "Authors:"	
putexcel B2 = "Andy Baxter"
putexcel A3 = "Last edit: 3 September 2026 (AB)"

putexcel A5 = "Process:", bold
putexcel B5 = "Description:", bold

putexcel A6 = "Process HM1_L"
putexcel B6 = "Mental Health (level), Stage 1 - estimated before labour supply :  GHQ score 0-36"
putexcel A7 = "Process HM2_Females_L"
putexcel B7 = "Mental Health (level), Stage 2 - causal estimate post labour-supply (Females) :  GHQ score 0-36"
putexcel A8 = "Process HM2_Males_L"
putexcel B8 = "Mental Health (level), Stage 2 - causal estimate post labour-supply (Males) :  GHQ score 0-36"
putexcel A9 = "Process HM1_C"
putexcel B9 = "Mental Health (categorical), Stage 1 - estimated before labour supply :  GHQ score 0-12"
putexcel A10 = "Process HM2_Females_C"
putexcel B10 = "Mental Health (categorical), Stage 2 - causal estimate post labour-supply (Females) :  GHQ score 0-12"
putexcel A11 = "Process HM2_Males_C"
putexcel B11 = "Mental Health (categorical), Stage 2 - causal estimate post labour-supply (Males) :  GHQ score 0-12"

putexcel set "$dir_results/reg_health_mental", sheet("Gof") modify
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


/**************************** HM1_L: GHQ score 0-36 ***************************/

reg dhm ///
	Dhh_owned_L1 Dcpst_Single_L1 Dnc_L1 ///
	Dhm_L1 L_Dhe_pcs ///
	$regions L_Ydses_c5_Q2 L_Ydses_c5_Q3 L_Ydses_c5_Q4 L_Ydses_c5_Q5 ///
	L_Dlltsd01 Dgn Dag Dag_sq ///
	i.Deh_c4_Medium i.Deh_c4_Low i.Deh_c4_Na ///
	$ethnicity ///
	Year_transformed ///
	if ${hwb1_if_condition} [pw=${weight}], vce(cluster idperson)

process_regression, domain("health_mental") process("HM1_L") sheet("HM1_L") ///
	title("Process HM1_L: GHQ score 0-36") ///
	gofrow(3) goflabel("HM1_L GHQ score 0-36") ///
	ifcond("${hwb1_if_condition}")	

* Calculate RMSE
cap drop residuals squared_residuals  
predict  residuals , residuals
gen squared_residuals = residuals^2

preserve
sum squared_residuals [w = dwt]
di "RMSE for GHQ score 0-36" sqrt(r(mean))
putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A13 = ("HM1_L") B13 = (sqrt(r(mean))) 
restore 			

/************************** HM2_Females_L: GHQ score 0-36 *********************/

*Stage 2
reghdfe dhm ///
	PersistentEmployed UnemployedToEmployed PersistentUnemployed ///
	NoPoverty PovertyToNonPoverty PersistentPoverty ///
	RealIncomeChange RealIncomeDecrease_D FinancialDistress ///
	Y2020 Y2021 ///
	Dhh_owned_L1 Dcpst_Single_L1 Dnc_L1 ///
	Dhm_L1 L_Dhe_pcs ///
	$regions L_Ydses_c5_Q2 L_Ydses_c5_Q3 L_Ydses_c5_Q4 L_Ydses_c5_Q5 ///
	L_Dlltsd01 Dag Dag_sq ///
	i.Deh_c4_Medium i.Deh_c4_Low i.Deh_c4_Na ///
	$ethnicity ///
	Year_transformed ///
	if ${hwb2_if_condition} & Dgn == 0 [pw=${weight}], absorb(idperson) vce(cluster idperson)

matrix results = r(table)

local exitEmp = results[1,1] * -1

local enterPov = results[1,4] * -1
	
process_regression, domain("health_mental") process("HM2_Females_L") sheet("HM2_Females_L") ///
	title("Process HM2_Females_L: GHQ score 0-36") ///
	gofrow(7) goflabel("HM2_Females_L: GHQ score 0-36") ///
	ifcond("${hwb2_if_condition}") gformula maxestimates(11)
  
putexcel set "$dir_results/reg_health_mental", sheet("HM2_Females_L") modify

putexcel A2 = "EmployedToUnemployed" // actually "continuous employment" with sign reversed
putexcel C1 = "EmployedToUnemployed" // actually "continuous employment" with sign reversed
putexcel B2 = `exitEmp'
putexcel A5 = "NonPovertyToPoverty" // actually "no poverty" with sign reversed
putexcel F1 = "NonPovertyToPoverty" // actually "no poverty" with sign reversed
putexcel B5 = `enterPov'



/************************** HM2_Males_L: GHQ score 0-36 *********************/

*Stage 2
reghdfe dhm ///
	PersistentEmployed UnemployedToEmployed PersistentUnemployed ///
	NoPoverty PovertyToNonPoverty PersistentPoverty ///
	RealIncomeChange RealIncomeDecrease_D FinancialDistress ///
	Y2020 Y2021 ///
	Dhh_owned_L1 Dcpst_Single_L1 Dnc_L1 ///
	Dhm_L1 L_Dhe_pcs ///
	$regions L_Ydses_c5_Q2 L_Ydses_c5_Q3 L_Ydses_c5_Q4 L_Ydses_c5_Q5 ///
	L_Dlltsd01 Dag Dag_sq ///
	i.Deh_c4_Medium i.Deh_c4_Low i.Deh_c4_Na ///
	$ethnicity ///
	Year_transformed ///
	if ${hwb2_if_condition} & Dgn == 1 [pw=${weight}], absorb(idperson) vce(cluster idperson)

matrix results = r(table)

local exitEmp = results[1,1] * -1

local enterPov = results[1,4] * -1
	
process_regression, domain("health_mental") process("HM2_Males_L") sheet("HM2_Males_L") ///
	title("Process HM2_Males_L: GHQ score 0-36") ///
	gofrow(11) goflabel("HM2_Males_L: GHQ score 0-36") ///
	ifcond("${hwb2_if_condition}") gformula maxestimates(11)
  
putexcel set "$dir_results/reg_health_mental", sheet("HM2_Males_L") modify

putexcel A2 = "EmployedToUnemployed" // actually "continuous employment" with sign reversed
putexcel C1 = "EmployedToUnemployed" // actually "continuous employment" with sign reversed
putexcel B2 = `exitEmp'
putexcel A5 = "NonPovertyToPoverty" // actually "no poverty" with sign reversed
putexcel F1 = "NonPovertyToPoverty" // actually "no poverty" with sign reversed
putexcel B5 = `enterPov'



/************************** HM1_C: GHQ score 0-12 *********************/

*Stage 1
ologit dhm_ghq ///
	Dhh_owned_L1 Dcpst_Single_L1 Dnc_L1 ///
	Dhmghq_L1 L_Dhe_pcs ///
	$regions L_Ydses_c5_Q2 L_Ydses_c5_Q3 L_Ydses_c5_Q4 L_Ydses_c5_Q5 ///
	L_Dlltsd01 Dag Dag_sq ///
	i.Deh_c4_Medium i.Deh_c4_Low i.Deh_c4_Na ///
	$ethnicity ///
	Year_transformed ///
	if ${hwb2_if_condition} [pw=${weight}], vce(robust)

process_ologit, domain("health_mental") process("HM1_C") sheet("HM1_C") ///
	title("Process HM1_C:Post-labour supply GHQ score 0-12") ///
	gofrow(15) goflabel("HM1_C: GHQ score 0-12") ///
	ifcond("${hwb2_if_condition}")



/************************** HM2_Females_C: GHQ score 0-12 *********************/

*Stage 2
reghdfe dhm_ghq ///
	PersistentEmployed UnemployedToEmployed PersistentUnemployed ///
	NoPoverty PovertyToNonPoverty PersistentPoverty ///
	RealIncomeChange RealIncomeDecrease_D FinancialDistress ///
	Y2020 Y2021 ///
	Dhh_owned_L1 Dcpst_Single_L1 Dnc_L1 ///
	Dhmghq_L1 L_Dhe_pcs ///
	$regions L_Ydses_c5_Q2 L_Ydses_c5_Q3 L_Ydses_c5_Q4 L_Ydses_c5_Q5 ///
	L_Dlltsd01 Dag Dag_sq ///
	i.Deh_c4_Medium i.Deh_c4_Low i.Deh_c4_Na ///
	$ethnicity ///
	Year_transformed ///
	if ${hwb2_if_condition} & Dgn == 0 [pw=${weight}], absorb(idperson) vce(cluster idperson)

matrix results = r(table)

local exitEmp = results[1,1] * -1

local enterPov = results[1,4] * -1
	
process_regression, domain("health_mental") process("HM2_Females_C") sheet("HM2_Females_C") ///
	title("Process HM2_Females_C: GHQ score 0-12") ///
	gofrow(19) goflabel("HM2_Females_C: GHQ score 0-12") ///
	ifcond("${hwb2_if_condition}") gformula maxestimates(11)
  
putexcel set "$dir_results/reg_health_mental", sheet("HM2_Females_C") modify

putexcel A2 = "EmployedToUnemployed" // actually "continuous employment" with sign reversed
putexcel C1 = "EmployedToUnemployed" // actually "continuous employment" with sign reversed
putexcel B2 = `exitEmp'
putexcel A5 = "NonPovertyToPoverty" // actually "no poverty" with sign reversed
putexcel F1 = "NonPovertyToPoverty" // actually "no poverty" with sign reversed
putexcel B5 = `enterPov'



/************************** HM2_Males_C: GHQ score 0-12 *********************/

*Stage 2
reghdfe dhm_ghq ///
	PersistentEmployed UnemployedToEmployed PersistentUnemployed ///
	NoPoverty PovertyToNonPoverty PersistentPoverty ///
	RealIncomeChange RealIncomeDecrease_D FinancialDistress ///
	Y2020 Y2021 ///
	Dhh_owned_L1 Dcpst_Single_L1 Dnc_L1 ///
	Dhmghq_L1 L_Dhe_pcs ///
	$regions L_Ydses_c5_Q2 L_Ydses_c5_Q3 L_Ydses_c5_Q4 L_Ydses_c5_Q5 ///
	L_Dlltsd01 Dag Dag_sq ///
	i.Deh_c4_Medium i.Deh_c4_Low i.Deh_c4_Na ///
	$ethnicity ///
	Year_transformed ///
	if ${hwb2_if_condition} & Dgn == 1 [pw=${weight}], absorb(idperson) vce(cluster idperson)

matrix results = r(table)

local exitEmp = results[1,1] * -1

local enterPov = results[1,4] * -1
	
process_regression, domain("health_mental") process("HM2_Males_C") sheet("HM2_Males_C") ///
	title("Process HM2_Males_C: GHQ score 0-12") ///
	gofrow(23) goflabel("HM2_Males_C: GHQ score 0-12") ///
	ifcond("${hwb2_if_condition}") gformula maxestimates(11)
  
putexcel set "$dir_results/reg_health_mental", sheet("HM2_Males_C") modify

putexcel A2 = "EmployedToUnemployed" // actually "continuous employment" with sign reversed
putexcel C1 = "EmployedToUnemployed" // actually "continuous employment" with sign reversed
putexcel B2 = `exitEmp'
putexcel A5 = "NonPovertyToPoverty" // actually "no poverty" with sign reversed
putexcel F1 = "NonPovertyToPoverty" // actually "no poverty" with sign reversed
putexcel B5 = `enterPov'





capture log close

*******************************************************
*** End
*******************************************************
