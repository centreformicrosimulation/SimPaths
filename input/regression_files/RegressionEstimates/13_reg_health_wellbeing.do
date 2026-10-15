/****************************************************************************/
* PROJECT:  		SimPaths UK
* SECTION:		Wellbeing
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
log using "${dir_log}/reg_health_wellbeing.log", replace


/********************************* SET EXCEL FILE *****************************/

putexcel set "$dir_results/reg_health_wellbeing", sheet("Info") modify //replace
putexcel A1 = "Description:", bold
putexcel B1 = "This file contains regression estimates for wellbeing processes: MCS, PCS and Life Satisfaction scores"
putexcel A2 = "Authors:"	
putexcel B2 = "Andy Baxter"
putexcel A3 = "Last edit: 3 September 2026 (AB)"

putexcel A5 = "Process:", bold
putexcel B5 = "Description:", bold

putexcel A6 = "Process DHE_MCS1"
putexcel B6 = "Mental Wellbeing, Stage 1 - estimated before labour supply :  SF12 MCS Score (0-100)"
putexcel A7 = "Process DHE_MCS2_Females"
putexcel B7 = "Mental Wellbeing, Stage 2 - causal estimate post labour-supply (Females) :  SF12 MCS Score (0-100)"
putexcel A8 = "Process DHE_MCS2_Males"
putexcel B8 = "Mental Wellbeing, Stage 2 - causal estimate post labour-supply (Males) :  SF12 MCS Score (0-100)"
putexcel A9 = "Process DHE_PCS1"
putexcel B9 = "Phsyical Wellbeing, Stage 1 - estimated before labour supply :  SF12 PCS Score (0-100)"
putexcel A10 = "Process DHE_PCS2_Females"
putexcel B10 = "Phsyical Wellbeing, Stage 2 - causal estimate post labour-supply (Females) :  SF12 PCS Score (0-100)"
putexcel A11 = "Process DHE_PCS2_Males"
putexcel B11 = "Phsyical Wellbeing, Stage 2 - causal estimate post labour-supply (Males) :  SF12 PCS Score (0-100)"
putexcel A12 = "Process DLS1"
putexcel B12 = "Life Satisfaction, Stage 1 - estimated before labour supply :  Life Satisfaction Score (0-10)"
putexcel A13 = "Process DLS2_Females"
putexcel B13 = "Life Satisfaction, Stage 2 - causal estimate post labour-supply (Females) :  Life Satisfaction Score (0-10)"
putexcel A14 = "Process DLS2_Males"
putexcel B14 = "Life Satisfaction, Stage 2 - causal estimate post labour-supply (Males) :  Life Satisfaction Score (0-10)"

putexcel set "$dir_results/reg_health_wellbeing", sheet("Gof") modify
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

/**************************** DHE_MCS1: SF12 MCS Score (0-100) ***************************/

reg dhe_mcs ///
	Dhh_owned_L1 Dcpst_Single_L1 Dnc_L1 ///
	L_Dhe_mcs L_Dhe_pcs ///
	$regions L_Ydses_c5_Q2 L_Ydses_c5_Q3 L_Ydses_c5_Q4 L_Ydses_c5_Q5 ///
	L_Dlltsd01 Dgn Dag Dag_sq ///
	i.Deh_c4_Medium i.Deh_c4_Low i.Deh_c4_Na ///
	$ethnicity ///
	Year_transformed ///
	if ${hwb1_if_condition} [pw=${weight}], vce(cluster idperson)

process_regression, domain("health_wellbeing") process("DHE_MCS1") sheet("DHE_MCS1") ///
	title("Process DHE_MCS1: SF12 MCS Score (0-100)") ///
	gofrow(3) goflabel("DHE_MCS1 SF12 MCS Score (0-100)") ///
	ifcond("${hwb1_if_condition}")	

* Calculate RMSE
cap drop residuals squared_residuals  
predict  residuals , residuals
gen squared_residuals = residuals^2

preserve
sum squared_residuals [w = dwt]
di "RMSE for SF12 MCS Score (0-100)" sqrt(r(mean))
putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A14 = ("DHE_MCS1") B14 = (sqrt(r(mean))) 
restore 			

/************************** DHE_MCS2_Females: SF12 MCS Score (0-100) *********************/

*Stage 2
reghdfe dhe_mcs ///
	PersistentEmployed UnemployedToEmployed PersistentUnemployed ///
	NoPoverty PovertyToNonPoverty PersistentPoverty ///
	RealIncomeChange RealIncomeDecrease_D FinancialDistress ///
	Y2020 Y2021 ///
	Dhh_owned_L1 Dcpst_Single_L1 Dnc_L1 ///
	L_Dhe_mcs L_Dhe_pcs ///
	$regions L_Ydses_c5_Q2 L_Ydses_c5_Q3 L_Ydses_c5_Q4 L_Ydses_c5_Q5 ///
	L_Dlltsd01 Dag Dag_sq ///
	i.Deh_c4_Medium i.Deh_c4_Low i.Deh_c4_Na ///
	$ethnicity ///
	Year_transformed ///
	if ${hwb2_if_condition} & Dgn == 0 [pw=${weight}], absorb(idperson) vce(cluster idperson)

matrix results = r(table)

local exitEmp = results[1,1] * -1

local enterPov = results[1,4] * -1
	
process_regression, domain("health_wellbeing") process("DHE_MCS2_Females") sheet("DHE_MCS2_Females") ///
	title("Process DHE_MCS2_Females: SF12 MCS Score (0-100)") ///
	gofrow(7) goflabel("DHE_MCS2_Females: SF12 MCS Score (0-100)") ///
	ifcond("${hwb2_if_condition}") gformula maxestimates(11)
  
putexcel set "$dir_results/reg_health_wellbeing", sheet("DHE_MCS2_Females") modify

putexcel A2 = "EmployedToUnemployed" // actually "continuous employment" with sign reversed
putexcel C1 = "EmployedToUnemployed" // actually "continuous employment" with sign reversed
putexcel B2 = `exitEmp'
putexcel A5 = "NonPovertyToPoverty" // actually "no poverty" with sign reversed
putexcel F1 = "NonPovertyToPoverty" // actually "no poverty" with sign reversed
putexcel B5 = `enterPov'



/************************** DHE_MCS2_Males: SF12 MCS Score (0-100) *********************/

*Stage 2
reghdfe dhe_mcs ///
	PersistentEmployed UnemployedToEmployed PersistentUnemployed ///
	NoPoverty PovertyToNonPoverty PersistentPoverty ///
	RealIncomeChange RealIncomeDecrease_D FinancialDistress ///
	Y2020 Y2021 ///
	Dhh_owned_L1 Dcpst_Single_L1 Dnc_L1 ///
	L_Dhe_mcs L_Dhe_pcs ///
	$regions L_Ydses_c5_Q2 L_Ydses_c5_Q3 L_Ydses_c5_Q4 L_Ydses_c5_Q5 ///
	L_Dlltsd01 Dag Dag_sq ///
	i.Deh_c4_Medium i.Deh_c4_Low i.Deh_c4_Na ///
	$ethnicity ///
	Year_transformed ///
	if ${hwb2_if_condition} & Dgn == 1 [pw=${weight}], absorb(idperson) vce(cluster idperson)

matrix results = r(table)

local exitEmp = results[1,1] * -1

local enterPov = results[1,4] * -1
	
process_regression, domain("health_wellbeing") process("DHE_MCS2_Males") sheet("DHE_MCS2_Males") ///
	title("Process DHE_MCS2_Males: SF12 MCS Score (0-100)") ///
	gofrow(11) goflabel("DHE_MCS2_Males: SF12 MCS Score (0-100)") ///
	ifcond("${hwb2_if_condition}") gformula maxestimates(11)
  
putexcel set "$dir_results/reg_health_wellbeing", sheet("DHE_MCS2_Males") modify

putexcel A2 = "EmployedToUnemployed" // actually "continuous employment" with sign reversed
putexcel C1 = "EmployedToUnemployed" // actually "continuous employment" with sign reversed
putexcel B2 = `exitEmp'
putexcel A5 = "NonPovertyToPoverty" // actually "no poverty" with sign reversed
putexcel F1 = "NonPovertyToPoverty" // actually "no poverty" with sign reversed
putexcel B5 = `enterPov'



/**************************** DHE_PCS1: SF12 PCS Score (0-100) ***************************/

reg dhe_pcs ///
	Dhh_owned_L1 Dcpst_Single_L1 Dnc_L1 ///
	L_Dhe_mcs L_Dhe_pcs ///
	$regions L_Ydses_c5_Q2 L_Ydses_c5_Q3 L_Ydses_c5_Q4 L_Ydses_c5_Q5 ///
	L_Dlltsd01 Dgn Dag Dag_sq ///
	i.Deh_c4_Medium i.Deh_c4_Low i.Deh_c4_Na ///
	$ethnicity ///
	Year_transformed ///
	if ${hwb1_if_condition} [pw=${weight}], vce(cluster idperson)

process_regression, domain("health_wellbeing") process("DHE_PCS1") sheet("DHE_PCS1") ///
	title("Process DHE_PCS1: SF12 PCS Score (0-100)") ///
	gofrow(15) goflabel("DHE_PCS1 SF12 PCS Score (0-100)") ///
	ifcond("${hwb1_if_condition}")	

* Calculate RMSE
cap drop residuals squared_residuals  
predict  residuals , residuals
gen squared_residuals = residuals^2

preserve
sum squared_residuals [w = dwt]
di "RMSE for SF12 PCS Score (0-100)" sqrt(r(mean))
putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A15 = ("DHE_PCS1") B15 = (sqrt(r(mean))) 
restore 			

/************************** DHE_PCS2_Females: SF12 PCS Score (0-100) *********************/

*Stage 2
reghdfe dhe_pcs ///
	PersistentEmployed UnemployedToEmployed PersistentUnemployed ///
	NoPoverty PovertyToNonPoverty PersistentPoverty ///
	RealIncomeChange RealIncomeDecrease_D FinancialDistress ///
	Y2020 Y2021 ///
	Dhh_owned_L1 Dcpst_Single_L1 Dnc_L1 ///
	L_Dhe_mcs L_Dhe_pcs ///
	$regions L_Ydses_c5_Q2 L_Ydses_c5_Q3 L_Ydses_c5_Q4 L_Ydses_c5_Q5 ///
	L_Dlltsd01 Dag Dag_sq ///
	i.Deh_c4_Medium i.Deh_c4_Low i.Deh_c4_Na ///
	$ethnicity ///
	Year_transformed ///
	if ${hwb2_if_condition} & Dgn == 0 [pw=${weight}], absorb(idperson) vce(cluster idperson)

matrix results = r(table)

local exitEmp = results[1,1] * -1

local enterPov = results[1,4] * -1
	
process_regression, domain("health_wellbeing") process("DHE_PCS2_Females") sheet("DHE_PCS2_Females") ///
	title("Process DHE_PCS2_Females: SF12 PCS Score (0-100)") ///
	gofrow(19) goflabel("DHE_PCS2_Females: SF12 PCS Score (0-100)") ///
	ifcond("${hwb2_if_condition}") gformula maxestimates(11)
  
putexcel set "$dir_results/reg_health_wellbeing", sheet("DHE_PCS2_Females") modify

putexcel A2 = "EmployedToUnemployed" // actually "continuous employment" with sign reversed
putexcel C1 = "EmployedToUnemployed" // actually "continuous employment" with sign reversed
putexcel B2 = `exitEmp'
putexcel A5 = "NonPovertyToPoverty" // actually "no poverty" with sign reversed
putexcel F1 = "NonPovertyToPoverty" // actually "no poverty" with sign reversed
putexcel B5 = `enterPov'



/************************** DHE_PCS2_Males: SF12 PCS Score (0-100) *********************/

*Stage 2
reghdfe dhe_pcs ///
	PersistentEmployed UnemployedToEmployed PersistentUnemployed ///
	NoPoverty PovertyToNonPoverty PersistentPoverty ///
	RealIncomeChange RealIncomeDecrease_D FinancialDistress ///
	Y2020 Y2021 ///
	Dhh_owned_L1 Dcpst_Single_L1 Dnc_L1 ///
	L_Dhe_mcs L_Dhe_pcs ///
	$regions L_Ydses_c5_Q2 L_Ydses_c5_Q3 L_Ydses_c5_Q4 L_Ydses_c5_Q5 ///
	L_Dlltsd01 Dag Dag_sq ///
	i.Deh_c4_Medium i.Deh_c4_Low i.Deh_c4_Na ///
	$ethnicity ///
	Year_transformed ///
	if ${hwb2_if_condition} & Dgn == 1 [pw=${weight}], absorb(idperson) vce(cluster idperson)

matrix results = r(table)

local exitEmp = results[1,1] * -1

local enterPov = results[1,4] * -1
	
process_regression, domain("health_wellbeing") process("DHE_PCS2_Males") sheet("DHE_PCS2_Males") ///
	title("Process DHE_PCS2_Males: SF12 PCS Score (0-100)") ///
	gofrow(23) goflabel("DHE_PCS2_Males: SF12 PCS Score (0-100)") ///
	ifcond("${hwb2_if_condition}") gformula maxestimates(11)
  
putexcel set "$dir_results/reg_health_wellbeing", sheet("DHE_PCS2_Males") modify

putexcel A2 = "EmployedToUnemployed" // actually "continuous employment" with sign reversed
putexcel C1 = "EmployedToUnemployed" // actually "continuous employment" with sign reversed
putexcel B2 = `exitEmp'
putexcel A5 = "NonPovertyToPoverty" // actually "no poverty" with sign reversed
putexcel F1 = "NonPovertyToPoverty" // actually "no poverty" with sign reversed
putexcel B5 = `enterPov'



/**************************** DLS1: Life Satisfaction Score (0-10) ***************************/

reg dls ///
	Dhh_owned_L1 Dcpst_Single_L1 Dnc_L1 ///
	Dls_L1 L_Dhe_pcs ///
	$regions L_Ydses_c5_Q2 L_Ydses_c5_Q3 L_Ydses_c5_Q4 L_Ydses_c5_Q5 ///
	L_Dlltsd01 Dgn Dag Dag_sq ///
	i.Deh_c4_Medium i.Deh_c4_Low i.Deh_c4_Na ///
	$ethnicity ///
	Year_transformed ///
	if ${hwb1_if_condition} [pw=${weight}], vce(cluster idperson)

process_regression, domain("health_wellbeing") process("DLS1") sheet("DLS1") ///
	title("Process DLS1: Life Satisfaction Score (0-10)") ///
	gofrow(27) goflabel("DLS1 Life Satisfaction Score (0-10)") ///
	ifcond("${hwb1_if_condition}")	

* Calculate RMSE
cap drop residuals squared_residuals  
predict  residuals , residuals
gen squared_residuals = residuals^2

preserve
sum squared_residuals [w = dwt]
di "RMSE for Life Satisfaction Score (0-10)" sqrt(r(mean))
putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A16 = ("DLS1") B16 = (sqrt(r(mean))) 
restore 			

/************************** DLS2_Females: Life Satisfaction Score (0-10) *********************/

*Stage 2
reghdfe dls ///
	PersistentEmployed UnemployedToEmployed PersistentUnemployed ///
	NoPoverty PovertyToNonPoverty PersistentPoverty ///
	RealIncomeChange RealIncomeDecrease_D FinancialDistress ///
	Y2020 Y2021 ///
	Dhh_owned_L1 Dcpst_Single_L1 Dnc_L1 ///
	Dls_L1 L_Dhe_pcs ///
	$regions L_Ydses_c5_Q2 L_Ydses_c5_Q3 L_Ydses_c5_Q4 L_Ydses_c5_Q5 ///
	L_Dlltsd01 Dag Dag_sq ///
	i.Deh_c4_Medium i.Deh_c4_Low i.Deh_c4_Na ///
	$ethnicity ///
	Year_transformed ///
	if ${hwb2_if_condition} & Dgn == 0 [pw=${weight}], absorb(idperson) vce(cluster idperson)

matrix results = r(table)

local exitEmp = results[1,1] * -1

local enterPov = results[1,4] * -1
	
process_regression, domain("health_wellbeing") process("DLS2_Females") sheet("DLS2_Females") ///
	title("Process DLS2_Females: Life Satisfaction Score (0-10)") ///
	gofrow(31) goflabel("DLS2_Females: Life Satisfaction Score (0-10)") ///
	ifcond("${hwb2_if_condition}") gformula maxestimates(11)
  
putexcel set "$dir_results/reg_health_wellbeing", sheet("DLS2_Females") modify

putexcel A2 = "EmployedToUnemployed" // actually "continuous employment" with sign reversed
putexcel C1 = "EmployedToUnemployed" // actually "continuous employment" with sign reversed
putexcel B2 = `exitEmp'
putexcel A5 = "NonPovertyToPoverty" // actually "no poverty" with sign reversed
putexcel F1 = "NonPovertyToPoverty" // actually "no poverty" with sign reversed
putexcel B5 = `enterPov'



/************************** DLS2_Males: Life Satisfaction Score (0-10) *********************/

*Stage 2
reghdfe dls ///
	PersistentEmployed UnemployedToEmployed PersistentUnemployed ///
	NoPoverty PovertyToNonPoverty PersistentPoverty ///
	RealIncomeChange RealIncomeDecrease_D FinancialDistress ///
	Y2020 Y2021 ///
	Dhh_owned_L1 Dcpst_Single_L1 Dnc_L1 ///
	Dls_L1 L_Dhe_pcs ///
	$regions L_Ydses_c5_Q2 L_Ydses_c5_Q3 L_Ydses_c5_Q4 L_Ydses_c5_Q5 ///
	L_Dlltsd01 Dag Dag_sq ///
	i.Deh_c4_Medium i.Deh_c4_Low i.Deh_c4_Na ///
	$ethnicity ///
	Year_transformed ///
	if ${hwb2_if_condition} & Dgn == 1 [pw=${weight}], absorb(idperson) vce(cluster idperson)

matrix results = r(table)

local exitEmp = results[1,1] * -1

local enterPov = results[1,4] * -1
	
process_regression, domain("health_wellbeing") process("DLS2_Males") sheet("DLS2_Males") ///
	title("Process DLS2_Males: Life Satisfaction Score (0-10)") ///
	gofrow(35) goflabel("DLS2_Males: Life Satisfaction Score (0-10)") ///
	ifcond("${hwb2_if_condition}") gformula maxestimates(11)
  
putexcel set "$dir_results/reg_health_wellbeing", sheet("DLS2_Males") modify

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
