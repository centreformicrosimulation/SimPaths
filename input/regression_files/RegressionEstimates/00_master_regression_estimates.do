
***************************************************************************************
* PROJECT:              SimPaths UK: regression estimates for SimPaths using UKHLS data 
* DO-FILE NAME:         master.do
* DESCRIPTION:          Main do-file to set the main parameters (country, paths) and call sub-scripts
***************************************************************************************
* COUNTRY:              UK
* DATA:         	    UKHLS EUL version - UKDA-6614-stata [to wave o]
*
* AUTHORS: 				Daria Popova, Justin van de Ven
* LAST UPDATE:          6 May 2026 DP  
***************************************************************************************

***************************************************************************************
* General comments:
* - Note that in the following scripts some standard commands may be 
*   abbreviated: (gen)erate, (tab)ulate, (sum)marize, (di)splay, 
*   (cap)ture, (qui)etly, (noi)sily

*Stata packages to install 
*ssc install fre
*ssc install tsspell 
*ssc install carryforward 
*ssc install outreg2
*ssc install oparallel
*ssc install gologit2
*ssc install winsor
*ssc install reghdfe
*ssc install ftools
*ssc install require
*
* NOTES: 				                        						 
* 						The income and union parameter do file must be run after
* 						the wage estimates are obtained because they use 
* 						predicted wages. The order of the remaining files is
* 						arbitrary. 
***************************************************************************************
***************************************************************************************

clear all
set more off
set type double
set maxvar 30000
set matsize 1000


/**************************************************************************************
* DEFINE DIRECTORIES
**************************************************************************************/

* Working directory

global path "D:\Dasha\ESSEX\_SimPaths\_SimPaths_UK\input_processing"

global dir_work "${path}\regression_estimates"

* Directory which contains do files
global dir_do "${dir_work}\do"

* Directory which contains log files 
global dir_log "${dir_work}\log"

* Directory which contains raw output: Excel and Word tables 
global dir_raw_results "${dir_work}\raw_results"

* Directory which contains final Excel files read by the model  
global dir_results "${dir_work}\results"

* Pooled dataset for estimates  
global estimation_sample "${path}\initial_populations\data\ukhls_pooled_ipop.dta"

* Pooled dataset with predicted wages after Heckman   
global estimation_sample2 "${path}\initial_populations\data\UKHLS_pooled_ipop2.dta"

* Directory containing external data used for the estimates (e.g. fertility rates, wage growth) 
global dir_external_data "${dir_work}/external_data"

* Directory to save data for internal validation 
global dir_validation_data "${dir_work}/internal_validation/data"

/*******************************************************************************
* DEFINE PARAMETERS & PROCESS IF CONDITIONS 
*******************************************************************************/

do "${path}\00_master_conditions.do"


/*******************************************************************************
* ESTIMATION FILES
*******************************************************************************/
/* 
Two additional do-files are called from each of these do-files
- variable_update.do refactors variable names 
- programs.do contains Stata programs to process the output of regressions and create Excel files with results used by Simpaths 
 */

do "${dir_do}/01_reg_education.do"

do "${dir_do}/02_reg_leave_parental_home.do"

do "${dir_do}/03_reg_partnership.do"

do "${dir_do}/04_reg_fertility.do"

do "${dir_do}/05_reg_health.do"

do "${dir_do}/06_reg_home_ownership.do"

do "${dir_do}/07_reg_retirement.do"

do "${dir_do}/08_reg_wages.do" 

do "${dir_do}/09_reg_income.do"

do "${dir_do}/10_reg_socialcare.do" 

/*Note that the do-files below are not yet refactored  */
do "${dir_do}/11_reg_financial_distress.do"

do "${dir_do}/12_reg_health_mental.do"

do "${dir_do}/13_reg_health_wellbeing.do"


/**************************************************************************************
* END OF FILE
**************************************************************************************/
