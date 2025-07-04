
***************************************************************************************
* PROJECT:              ESPON: regression estimates for SimPaths using UKHLS data 
* DO-FILE NAME:         master.do
* DESCRIPTION:          Main do-file to set the main parameters (country, paths) and call sub-scripts
***************************************************************************************
* COUNTRY:              UK
* DATA:         	    UKHLS EUL version - UKDA-6614-stata [to wave n]
*
* AUTHORS: 				Daria Popova, Justin van de Ven
* LAST UPDATE:          1 july 2025 DP  
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
* NOTES: 				Output formatting automated, however if you decide to 
* 						add or take-away variables from the processes you 
* 						will need to update the labelling in the excel files. 
*                        						 
* 						The income and union parameter do file must be run after
* 						the wage estimates are obtain because they use 
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
global dir_work "D:\Dasha\ESSEX\ESPON 2024\UK\regression_estimates"

* Directory which contains do files
global dir_do "${dir_work}/do"

* Directory which contains log files 
global dir_log "${dir_work}/log"

* Directory which contains raw output: Excel and Word tables 
global dir_raw_results "${dir_work}/raw_results"

* Directory which contains final Excel files read by the model  
global dir_results "${dir_work}/results"

* Directory which contains pooled dataset for estimates  
global dir_ukhls_data "D:\Dasha\ESSEX\ESPON 2024\UK\initial_populations\data"

* Directory containing external input data 
global dir_external_data "$dir_work/external_data"

* Directory containing results of comparison of various weights   
global weight_checks "D:\Dasha\ESSEX\ESPON 2024\UK\regression_estimates\weight_checks"

*********************Internal validation****************************************
* Directory to save data for internal validation 
global dir_validation_data "D:\Dasha\ESSEX\ESPON 2024\UK\regression_estimates\internal_validation\data"

* Directory for internal validation do-files 
global dir_do_validation "D:\Dasha\ESSEX\ESPON 2024\UK\regression_estimates\internal_validation\do_files"

* Directory for internal validation do-files 
global dir_do_validation "D:\Dasha\ESSEX\ESPON 2024\UK\regression_estimates\internal_validation\do_files"

* Directory for internal validation do-files 
global dir_validation_graphs "D:\Dasha\ESSEX\ESPON 2024\UK\regression_estimates\internal_validation\graphs"

global countyy "UK" 

/*******************************************************************************
* ESTIMATION FILES
*******************************************************************************/

do "${dir_do}/reg_education.do"

/*
do "${dir_do}/reg_leaveParentalHome.do"

do "${dir_do}/reg_partnership.do"

do "${dir_do}/reg_fertility.do"

do "${dir_do}/reg_health.do"

do "${dir_do}/reg_home_ownership.do"

do "${dir_do}/reg_retirement.do"

do "${dir_do}/reg_wages.do"

do "${dir_do}/reg_income.do"



/*
*******************************************************************************
* INTERNAL VALIDATION FILES
*******************************************************************************/

do "$dir_do_validation/int_val_education.do"	

do "$dir_do_validation/int_val_leave_parental_home.do"	

do "$dir_do_validation/int_val_partnership.do"	

do "$dir_do_validation/int_val_fertility.do"	

do "$dir_do_validation/int_val_health.do"	

do "$dir_do_validation/int_val_home_ownership.do"	

do "$dir_do_validation/int_val_retirement.do"	

do "$dir_do_validation/int_val_wages.do"	

do "$dir_do_validation/int_val_income.do"	

/**************************************************************************************
* END OF FILE
**************************************************************************************/
