/*******************************************************************************
* PROJECT:  		SimPaths UK
* SECTION:			Validation
* OBJECT: 			Master file
* AUTHORS:			Ashley Burdett 
* LAST UPDATE:		Jan 2026
* COUNTRY: 			UK 
* DESCRIPTION: 		This master file sets the globals, directories and 
* 					parameters, and runs the do files to construct the 
* 					validation datasets and plots the comparison graphs. 
********************************************************************************
* NOTES: 			UKHLS initial populations data is used to create the 
* 					validaton targets. 
*******************************************************************************/
clear all

set logtype smcl
set more off
set mem 200m
set type double


/*******************************************************************************
* 1 - STATIC SET UP 
*******************************************************************************/

/*******************************************************************************
* DEFINE COUNTRY & RUN GLOBALS
*******************************************************************************/

global country = "UK"		 						
global country_lower = "uk"
display in y "Country selected: ${country}"

global silc_UDB = "UDB_c"	


/*******************************************************************************
* DEFINE DIRECTORIES
*******************************************************************************/

/*
"/Users/ashleyburdett/Library/CloudStorage/Box-Box/CeMPA shared area/_SimPaths/_SimPathsUK"
"C:\Users\aburde\Box\CeMPA shared area\_SimPaths\_SimPathsUK"
*/

* Individual directory 
global dir_ind "/Users/ashleyburdett/Library/CloudStorage/Box-Box/CeMPA shared area/_SimPaths/_SimPathsUK"

* Main folder
global path "$dir_ind/validation/02_simulated_output_validation"

* Do files folder 
global dir_do_files "$path/do_files" 

* Output files folder 
global dir_work "$path/data" 

* UKHLS dataset folder 
global dir_UKHLS_data "$dir_ind/input 2026.03.04"

* Data folder 
global dir_data "$path/data"


/*******************************************************************************
* DEFINE SAMPLE PARAMETERS
*******************************************************************************/

global use_assert "0"

* Trim outliers
global trim_outliers true

* Observations up to and including this simulated year to be kept in the sample
global min_sim_year 2011
global max_sim_year 2023

* Define age to become responsible as defined in the simulation
global age_become_responsible 18

* Set labour supply categories 
global ls_cat "ZERO TEN TWENTY THIRTY THIRTY_EIGHT FORTY_FIVE FIFTY_FIVE" 
// works if the genders are symmetric
// still need to alter code in specific do files to print graphs 

global ls_cat_labour ///
	"TEN TWENTY THIRTY THIRTY_EIGHT FORTY_FIVE FIFTY_FIVE" 

* Number of runs (N-1 because numbering starts at 0)
global max_n_runs 4

* Run commons folder name 
global folder 20260306


/*******************************************************************************
* RUN DO FILES 
*******************************************************************************/

* Prepare observed data
do "${dir_do_files}/03_create_UKHLS_validation_targets.do"


/*******************************************************************************
* 2 - DYNAMIC SET UP 
*******************************************************************************/

* List of SimPath Set ups to loop through 
/*
Permits looping over output from multiple different model set-ups e.g. with and 
without ferility alignment, with and without employemnet alignment
*/
local alignments "0_default"

/*
  0_default 1_all_alignments_off 2_pop_on 3_pop_fertility_on 4_pop_fertility_inschool_on  5_pop_fertility_inschool_cohabit_on 6_pop_fertility_inschool_cohabit_empl_on
*/

foreach align in `alignments' {
	
	
/*******************************************************************************
* DEFINE DIRECTORIES
*******************************************************************************/

	* Simulated data CSV files folder
	global dir_simulated_data "${dir_ind}/_new_release/output/`align'"
		
	* Graphs folder 
	global dir_output_files "$path/graphs/`align'" 	


/*******************************************************************************
* CREATE OUTPUT FOLDERS
*******************************************************************************/	
	
	/*
	mkdir "$path/graphs/`align'"

	mkdir "$path/graphs/`align'/children" 
	mkdir "$path/graphs/`align'/correlations" 
	mkdir "$path/graphs/`align'/disability" 
	mkdir "$path/graphs/`align'/economic_activity" 
	mkdir "$path/graphs/`align'/education"
	mkdir "$path/graphs/`align'/health" 
	mkdir "$path/graphs/`align'/hours_worked" 
	mkdir "$path/graphs/`align'/income"
	mkdir "$path/graphs/`align'/income/capital_income"
	mkdir "$path/graphs/`align'/income/pension_income"
	mkdir "$path/graphs/`align'/income/disposable_income"
	mkdir "$path/graphs/`align'/income/equivalised_disposable_income"
	mkdir "$path/graphs/`align'/income/gross_income"
	mkdir "$path/graphs/`align'/income/gross_labour_income"
	mkdir "$path/graphs/`align'/income/income_shares"
	mkdir "$path/graphs/`align'/inequality" 
	mkdir "$path/graphs/`align'/partnership" 
	mkdir "$path/graphs/`align'/poverty" 
	mkdir "$path/graphs/`align'/wages" 
	mkdir "$path/graphs/`align'/social_care" 
	
}
	*/
	

/*******************************************************************************
* RUN DO FILES 
*******************************************************************************/

	* Prepare simulated data
	do "${dir_do_files}/01_prepare_simulated_data.do"
	do "${dir_do_files}/02_create_simulated_variables.do"


	* Plot figures	
	do "${dir_do_files}/04_01_plot_activity_status.do"
	do "${dir_do_files}/04_02_plot_education_level.do"
	do "${dir_do_files}/04_03_plot_gross_income.do"
	do "${dir_do_files}/04_04_plot_gross_labour_income.do"
	do "${dir_do_files}/04_05_plot_capital_income.do"
	do "${dir_do_files}/04_06_plot_pension_income.do"
	do "${dir_do_files}/04_07_plot_disposable_income.do"
	do "${dir_do_files}/04_08_plot_equivalised_disposable_income.do"
	do "${dir_do_files}/04_09_plot_hourly_wages.do"
	do "${dir_do_files}/04_10_0_plot_hours_worked.do"
	do "${dir_do_files}/04_10_1_plot_hours_worked_discrete.do"
	do "${dir_do_files}/04_11_plot_income_shares.do" 
	do "${dir_do_files}/04_12_plot_partnership_status.do"	
	do "${dir_do_files}/04_13_plot_health.do"
	do "${dir_do_files}/04_14_plot_at_risk_of_poverty.do"
	do "${dir_do_files}/04_15_plot_inequality.do"
	do "${dir_do_files}/04_16_plot_number_children.do"
	do "${dir_do_files}/04_17_plot_disability.do"
	do "${dir_do_files}/04_18_plot_social_care.do"


	* Calculate other statistics
	//do "${dir_do_files}/07_01_correlations.do"

}
