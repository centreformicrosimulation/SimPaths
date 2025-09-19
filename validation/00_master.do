/*******************************************************************************
* PROJECT:  		SIMPATHS
* SECTION:			Validation
* OBJECT: 			Master file
* AUTHORS:			Ashley Burdett 
* LAST UPDATE:		9/25
* COUNTRY: 			UK 

* DESCRIPTION: 		This master file sets the globals, directories and 
* 					parameters, and runs the do files to construct the 
* 					validation datasets and plots the comparison graphs. 

* NOTES: 			UKHLS initial populations data is used to create the 
* 					validaton targets. 
*******************************************************************************/
clear all

set logtype smcl
set more off
set mem 200m
set type double


/*******************************************************************************
* DEFINE COUNTRY GLOBALS
*******************************************************************************/

global country = "UK"		 						
global country_lower = "uk"
display in y "Country selected: ${country}"

global silc_UDB = "UDB_c"	


/*******************************************************************************
* DEFINE DIRECTORIES
*******************************************************************************/

/*
Currently save data locally 
*/

* VM
//global path "C:\Users\aburde\Box\ESPON - OVERLAP\_countries\HU\validation"

* Mac

* Main folder
global path "/Users/ashleyburdett/Documents/ESPON/${country}/validation"

* Observed data 
global EUSILC_original_crosssection "N:\CeMPA\data\EU_SILC\2024\_Cross_2004-2023_full_set\_Cross_2004-2023_full_set"  

* Do files folder 
global dir_do_files "$path/do_files" 

* Output files folder 
global dir_work  "$path/data" 

* Simulated data folder
global dir_simulated_data "$path/data"

* Data folder 
global dir_data "$path/data"

* Graphs folder 
global dir_output_files "$path/graphs" 


/*******************************************************************************
* DEFINE SAMPLE PARAMETERS
*******************************************************************************/

global use_assert "0"

* Trim outliers
global trim_outliers true

* Min age of individuals included in plots
global min_age 18

* Max age of individuals included in plots
global max_age 65

* Observations up to and including this simulated year to be kept in the sample
global max_year 2023

* Define age to become responsible as defined in the simulation
global age_become_responsible 18

* Set labour supply categories 
global ls_cat "ZERO TEN TWENTY THIRTY FORTY" 
// works if the genders are symmetric
// still need to alter code in specific do files to print graphs 

global ls_cat_labour "TEN TWENTY THIRTY FORTY" 


* Max hours work per week in sim 
global max_hours 48

/*******************************************************************************
RUN DO FILES 
*******************************************************************************/

* Prepare simulated data
do "${dir_do_files}/01_prepare_simulated_data.do"
do "${dir_do_files}/02_create_simulated_variables.do"

* Prepare observed data
do "${dir_do_files}/03_prepare_UKHLS_data.do"
do "${dir_do_files}/05_create_UKHLS_validation_targets.do"


* Plot figures
do "${dir_do_files}/06_01_plot_activity_status.do"
do "${dir_do_files}/06_02_plot_education_level.do"
do "${dir_do_files}/06_03_plot_gross_income.do"
do "${dir_do_files}/06_04_plot_gross_labour_income.do"
do "${dir_do_files}/06_05_plot_capital_income.do"
do "${dir_do_files}/06_05_plot_pension_income.do"
do "${dir_do_files}/06_07_plot_disposable_income.do"
do "${dir_do_files}/06_08_plot_equivalised_disposable_income.do"
do "${dir_do_files}/06_09_plot_hourly_wages.do"
do "${dir_do_files}/06_10_plot_hours_worked.do"
do "${dir_do_files}/06_11_plot_income_shares.do" 
do "${dir_do_files}/06_12_plot_partnership_status.do"
do "${dir_do_files}/06_13_plot_health.do"
do "${dir_do_files}/06_14_plot_at_risk_of_poverty.do"
do "${dir_do_files}/06_15_plot_income_ratios.do"
do "${dir_do_files}/06_16_plot_number_children.do"
do "${dir_do_files}/06_17_plot_disability"

* Calculate other statistics
do "${dir_do_files}/07_01_correlations.do"

