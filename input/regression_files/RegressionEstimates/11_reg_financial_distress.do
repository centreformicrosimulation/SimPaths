********************************************************************************
* PROJECT:		UC and mental health
* SECTION:		Health and wellbeing
* OBJECT: 		Financial distress
* AUTHORS:		Andy Baxter, Erik Igelström
* LAST UPDATE:	4 September 2026  
* COUNTRY:		UK 
*
* NOTES:		
********************************************************************************
clear all
set more off
set mem 200m
set maxvar 30000


*******************************************************************
cap log close 
log using "${dir_log}/reg_financial_distress.log", replace
*******************************************************************

/********************************* SET EXCEL FILE *****************************/

putexcel set "$dir_results/reg_financial_distress", sheet("Info") modify //replace
putexcel A1 = "Description:", bold
putexcel B1 = "This file contains regression estimates for Financial Distress"
putexcel A2 = "Authors:"	
putexcel B2 = "Andy Baxter, Erik Igelström"
putexcel A3 = "Last edit: 4 September 2026 (AB)"

putexcel A5 = "Process:", bold
putexcel B5 = "Description:", bold

putexcel A6 = "Process FinDis"
putexcel B6 = "Financial Distress (0/1) - logit model"

putexcel set "$dir_results/reg_financial_distress", sheet("Gof") modify
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

**********************************************************************
* FinDis - Financial Distress
**********************************************************************

logit financial_distress ///
    EmployedToUnemployed UnemployedToEmployed PersistentUnemployed ///
    Lhw_10 Lhw_20 Lhw_30 Lhw_40 ///
    RealIncomeChange RealIncomeDecrease_D NonPovertyToPoverty PovertyToNonPoverty PersistentPoverty ///
    L_Ypncp L_Ypnoab ///
    D_Econ_benefits Dhh_owned_L1 Dcpst_Single_L1 Dnc_L1 ///
    L_Dhe_pcs L_Dhe_mcs ///
    L_Ydses_c5_Q2 L_Ydses_c5_Q3 L_Ydses_c5_Q4 L_Ydses_c5_Q5 ///
    L_Dlltsd01 L_FinancialDistress ///
    Dgn Dag_L1 Dag_sq_L1 ///
    i.Deh_c4_Medium i.Deh_c4_Low i.Deh_c4_Na ///
    $regions Year_transformed ///
    Y2020 Y2021 $ethnicity ///
    if ${findis_if_condition} [pw=${weight}], vce(cluster idperson)

process_regression, domain("financial_distress") process("FinDis") sheet("FinDis") ///
	title("Process FinDis: Financial Distress (0/1)") ///
	gofrow(3) goflabel("Financial Distress") ///
	ifcond("${findis_if_condition}")	


**************************************************************************
* END
**************************************************************************
