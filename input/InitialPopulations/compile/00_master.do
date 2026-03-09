
***************************************************************************************
* PROJECT:              SimPaths UK: construct initial populations for SimPaths using UKHLS data 
* DO-FILE NAME:         00_master.do
* DESCRIPTION:          Main do-file to set the main parameters (country, paths) and call sub-scripts
***************************************************************************************
* COUNTRY:              UK
* DATA:         	    UKHLS EUL version - UKDA-6614-stata [to wave o]
*						WAS EUL version - UKDA-7215-stata [to wave 7]
* AUTHORS: 				Daria Popova, Justin van de Ven
* LAST UPDATE:          21 Jan 2026 DP 
***************************************************************************************
*   -----------------------------------------------------------------------------
*    Assumptions imposed to align the initial populations with simulation rules 
*   -----------------------------------------------------------------------------
*
*    - Retirement:
*       - Treated as an absorbing state
*       - Must retire by a specified maximum age
*       - Cannot retire before a specified minimum age
*
*   - Education:
*       - Leave education no earlier than a specified minimum age
*       - Must leave the initial education spell by a specified maximum age
*       - Cannot return to education after retirement
*
*   - Work:
*       - Can work from a specified minimum age
*       - Activity status and hours of work populated consistently:
*           → Assume not working if report hours = 0 
*           → Assume hours = 0 if not working
* 		- If missing partial information, don't assume the missing is 0 and 
* 			impute (hot-deck)
*
*   - Leaving the parental home:
*       - Can leave from a specified minimum age
* 		- Become the effective head of hh even when living with parents when 
* 			paretns retire or reach state retirment age
*
*   - Home ownership:
*       - Can own a home from a specified minimum age
*
*   - Partnership formation:
*       - Can form a partnership from a specified minimum age
*
*   - Disability:
*       - Treated as a subsample of the not-employed population
*
*   The relevant age thresholds are defined in globals defined in "DEFINE 
* 	PARAMETERS" section below. 
* 	Throughout also construct relevant flags and produce an Excel file "flag_descriptves" to 
* 	see the extent of the adjustments to the raw data. 
*
*   -----------------------------------------------------------------------
*    Additional notes on implementation: 
*   -----------------------------------------------------------------------
*   Current imputations : 
*   - Self-rated health status (ordered probit model)
*   - Subjective well-being (liner regression) 
*   - Mental and physical component summaries (linear regression)
*   - Impute highest parental education status (ordered probit model) 
*   - Impute education status using lagged observation and generalized ordered logit 
*   - Impute working hours if missing but the person is in work (panel based imputation + hot-deck)
*   - Impute observed hourly wages if missing but the person is in work (panel based imputation + hot-deck) 
*   
*   -----------------------------------------------------------------------
*   Remaining disparities between initial populations and simulation rules:
*   -----------------------------------------------------------------------
*   - Ages at which females can have a child. [Be informed by the sample?]
*	  Permit teenage mothers in this script (deal with in 03_ )
*   - A few higher/older education spells (30+) that last multiple years, whilst 
*     in the simulation can only return to education for single year spells. 
* 	- Should we have people becoming adults at 18 or 16 for income/number of 
* 		children purposes?
* 		Considered a child if live with parents until 18 and in ft education? 
* 	- Don't impose monotoncity on reported educational attainment information.  
* 	- Number of children vars (all ages or 0-2) don't account for feasibility 
* 		of age at birth of the mother. 
*******************************************************************************/
/*
* Stata packages to install 
ssc install fre
ssc install tsspell 
ssc install carryforward 
ssc install outreg2
ssc install filelist
ssc install gologit2
ssc install estout
*/

clear all
set more off
set type double
set maxvar 30000
set matsize 1000


/**************************************************************************************
* DEFINE DIRECTORIES
*************************************************************************************/

* Working directory
global dir_work "C:\MyFiles\99 DEV ENV\JAS-MINE\data work\initial_populations"
*global dir_work "D:\Dasha\ESSEX\_SimPaths\_SimPaths_UK\initial_populations"

* Directory containing do files
global dir_do "${dir_work}/do"

* Directory containing processed data
global dir_data "${dir_work}/data"

* Directory containing log files 
global dir_log "${dir_work}/log"

* Directory containing graphs  
global dir_graphs "${dir_work}/graphs"

* Directory containing UKHLS data
global dir_ukhls_data "J:\01 DATA\UK\ukhls\wave15\stata\stata14_se\ukhls"
*global dir_ukhls_data "D:\Dasha\UK-original-data\USoc\UKDA-6614-stata\stata\stata14_se\ukhls" //original_data

* Directory containing BHPS data
global dir_bhps_data "J:\01 DATA\UK\ukhls\wave15\stata\stata14_se\bhps"
*global dir_bhps_data  "D:\Dasha\UK-original-data\USoc\UKDA-6614-stata\stata\stata14_se\bhps" //original_data_bhps

* Directory containing WAS data
global dir_was_data "J:\01 DATA\UK\was\wave8\stata\stata13_se"
*global dir_was_data "D:\Dasha\UK-original-data\WAS\UKDA-7215-stata\stata\stata13_se"

* Directory containing processed employment history data
global dir_data_emphist "${dir_data}/emphist"

* Directory containing employment history do-files
global dir_do_emphist "${dir_do}/do_emphist"	

* Directory containing employment history log files 
global dir_log_emphist "${dir_log}/emphist"


/**************************************************************************************
* DEFINE OTHER GLOBAL VARIABLES
*************************************************************************************/
/*
* UKHLS Wave letters: we start from 2010 - wave 2 - b
first initial population starts in 2011 (as some variables are using data from the previous wave)
wave 1  a 2009-2011 (last year is small)
wave 2  b 2010-2012
wave 3  c 2011-2013
wave 4  d 2012-2014
wave 5  e 2013-2015
wave 6  f 2014-2016
wave 7  g 2015-2017
wave 8  h 2016-2018
wave 9  i 2017-2019
wave 10 j 2018-2020
wave 11 k 2019-2021
wave 12 l 2020-2022
wave 13 m 2021-2023
wave 14 n 2022-2024
wave 15 o 2023-2025
*/
global UKHLSwaves "a b c d e f g h i j k l m n o" /*all waves*/
global UKHLSwaves_numbers "1 2 3 4 5 6 7 8 9 10 11 12 13 14 15" //ukhls_all_waves_numbers

global UKHLS_panel_waves "b c d e f g h i j k l m n o"
global UKHLS_panel_waves_numbers "2 3 4 5 6 7 8 9 10 11 12 13 14 15" //ukhls_waves_numbers
global UKHLS_waves_prefixed "b_ c_ d_ e_ f_ g_ h_ i_ j_ k_ l_ m_ n_ o_"
global BHPS_waves "l m n o p q r"

* waves reporting social care module in ukhls - ADL questions added from wave 7 and then every other wave (from 2016)
global scRecWaves "g i k m" //Next time available in wave 16 p

* waves reporting social care provided in ukhls (from 2015)
global scProvWaves "f g h i j k l m n o" //Next time available in wave 16 p 

global firstSimYear = 2010
global lastSimYear = 2024
global wealthStartYear = 2015
global wealthEndYear = 2019


* Define threshold ages
/*
The thresholds defined below ensure consistency between the assumptions 
applied in the simulation and the structure of the initial population data. 
They specify the ages at which certain life-course transitions are permitted 
or enforced within the model. These limits reflect both modelling conventions 
and empirical considerations drawn from observed data.
*/
 	
* Age become an adult in various dimensions	
global age_becomes_responsible 18 

global age_seek_employment 16 
	
global age_leave_school 16 

global age_form_partnership 18 

global age_have_child_min 18 	

global age_leave_parental_home 18

	
* Age can/must/cannot make various transitions 	
global age_max_dep_child 17     

global age_adult 18 

global age_can_retire 50

global age_force_retire 75             

global age_force_leave_spell1_edu 30   

global age_have_child_max 49  	// allow this to be led by the data  	

       
/**************************************************************************************
* ROUTE TO WORKER FILES 
**************************************************************************************/
/* prepare simulated and observed data */
do "${dir_do}/01_prepare_UKHLS_pooled_data.do"
* process UKHLS data
do "${dir_do}/02_create_UKHLS_variables.do"
* add social care 
do "${dir_do}/03_social_care_received.do"
do "${dir_do}/04_social_care_provided.do"
* screens data and identifies benefit units 
do "${dir_do}/05_create_benefit_units.do"
* reweight data and slice into yearly segments
do "${dir_do}/06_reweight_and_slice.do"

* impute wealth data for selected years
do "${dir_do}/07_was_wealth_data.do"
forvalues year = $wealthStartYear / $wealthEndYear {
	global yearWealth = `year'
	do "${dir_do}/08_wealth_to_ukhls.do"
}
* check data and slice into initial populations
do "${dir_do}/09_finalise_input_data.do"
* descriptives for initial populations and full sample
*do "${dir_do}/10_check_yearly_data.do"


/**************************************************************************************
* END OF FILE
**************************************************************************************/
