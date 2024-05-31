***************************************************************************************
* PROJECT:              ESPON: construct initial populations for SimPaths using UKHLS data 
* DO-FILE NAME:         00_master.do
* DESCRIPTION:          Main do-file to set the main parameters (country, paths) and call sub-scripts
***************************************************************************************
* COUNTRY:              UK
* DATA:         	    UKHLS EUL version - UKDA-6614-stata [to wave m]
*						WAS EUL version - UKDA-7215-stata [to wave 7]
* AUTHORS: 				Daria Popova, Justin van de Ven
* LAST UPDATE:          10 Apr 2024
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
global dir_work "C:\MyFiles\99 DEV ENV\JAS-MINE\data work\initial_populations"

* Directory which contains do files
global dir_do "${dir_work}/do"

* Directory which contains data files 
global dir_data "${dir_work}/data"

* Directory which contains log files 
global dir_log "${dir_work}/log"

* Directory which contains UKHLS data
global dir_ukhls_data "J:\01 DATA\UK\ukhls\wave13\stata\stata13_se\ukhls"

* Directory which contains WAS data
global dir_was_data "J:\01 DATA\UK\was\wave7\stata\stata13_se"

* Directory which contains original initial popultions 
global dir_ipop_orig "${dir_work}/daria_data"


/**************************************************************************************
* DEFINE OTHER GLOBAL VARIABLES
**************************************************************************************/
* Define age to become responsible as defined in the simulation
global age_become_responsible 18

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
*/
global UKHLSwaves "a b c d e f g h i j k l m" /*all waves*/
* waves reporting social care module in ukhls - ADL questions added from wave 7 and then every other wave (from 2016)
global scRecWaves "g i k m"
* waves reporting social care provided in ukhls (from 2015)
global scProvWaves "f g h i j k l m"
global firstSimYear = 2010
global lastSimYear = 2023
global wealthStartYear = 2015
global wealthEndYear = 2019


/**************************************************************************************
* ROUTE TO WORKER FILES 
**************************************************************************************/
* Prepare simulated and observed data
do "${dir_do}/01_prepare_UKHLS_pooled_data.do"
* Process UKHLS data
do "${dir_do}/02_create_UKHLS_variables.do"
* add social care 
do "${dir_do}/03_social_care_received.do"
do "${dir_do}/04_social_care_provided.do"
* screens data and identifies benefit units
do "${dir_do}/05_drop_hholds_create_benefit_units.do"
* reweight data and slice into yearly segments
do "${dir_do}/06_reweight_and_slice.do"
* impute wealth data for selected years
do "${dir_do}/07_was_wealth_data.do"
forvalues year = $wealthStartYear / $wealthEndYear {
	global yearWealth = `year'
	do "${dir_do}/08_wealth_to_ukhls.do"
}
do "${dir_do}/09_finalise_input_data.do"
*do "${dir_do}/10_check_yearly_data.do"


/**************************************************************************************
* END OF FILE
**************************************************************************************/
