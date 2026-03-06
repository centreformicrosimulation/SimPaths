/*******************************************************************************
* PROJECT:        SimPaths UK
* SECTION:        ALIGNMENT PROCEDURES
*
* AUTHORS:        Mariia Vartuzova (MV)
* LAST UPDATE:    02/02/2026 (MV)
* COUNTRY:        UK
*
* DATA:           Initial populations			
*
* DESCRIPTION:    This do-file constructs employment targets for different
*                groups of the population using initial population data.
*                It:
*                  - Imports initial population CSV files by year
*                  - Computes student ratio to total population for each year
*                  - Exports the results to Excel 
*
* SET-UP:         1. Update the working directory path (global dir_w)
*                 2. Copy the relevant input data into the /input_data folder
*                    under the country-specific subdirectory
*
*******************************************************************************/

clear all


* --- DEFINE GLOBALS -------------------------------------------------------- *

* Working directory (project root)
global dir_w "D:\BaiduSyncdisk\Job CeMPA\education alignment UK"


* Country code and time span for which targets are produced
global country = "UK"
global min_year 2011
global max_year 2023

* Directory structure
global dir_input_data   "D:\CeMPA\SimPaths\input\InitialPopulations"
global dir_working_data "$dir_w/${country}/working_data"
global dir_output       "$dir_w/${country}"


* Initialise file that will store BU-level employment shares for all years
clear
save "${dir_working_data}/student_shares_${country}_initpopdata.dta", emptyok replace                

* ========================================================================== *
  
//local y = 2011  
// Loop over all years in the requested range
foreach y of numlist $min_year/$max_year {
	
	* Build file name for the given year and import initial population data
	local file = subinstr("population_initial_${country}_YYYY.csv","YYYY","`y'",.)
	import delimited using "${dir_input_data}/`file'", clear
	
	bys idpers: keep if _n == 1 //keep one obs per idperson)]
	
	// Alignment target: share of students among 16-29 age group, defined in SimPathsModel.java
	gen byte isStudent = (labc4 == 2 & demage >= 16 & demage <= 29) 
    collapse (mean) student_share = isStudent if (labc4 != . & demage >= 16 & demage <= 29) [pw = wgthhcross]
	
	
	//collapse (mean) empl_share = bu_fracemployed [pw = bu_w], by(group_code)
	gen year = `y'

	* Append to cumulative file for all years
	append using "${dir_working_data}/student_shares_${country}_initpopdata.dta"
	duplicates drop
	save "${dir_working_data}/student_shares_${country}_initpopdata.dta", replace

}

* -------------------------------------------------------------------------- *
* POST-PROCESSING: export aggregated results to Excel
* -------------------------------------------------------------------------- *


use "${dir_working_data}/student_shares_${country}_initpopdata.dta", clear

* Sort by year for neat export
sort year

* Create/overwrite Excel file that will hold all sheets
putexcel set "${dir_output}/inSchool_targets.xlsx", replace

	
* Build a matrix of all rows for the two variables (year, student_share)
mkmat year student_share, matrix(M)

* Point putexcel at the output file and the group-specific sheet
putexcel set "${dir_output}/inSchool_targets.xlsx", sheet("students") modify

* Write headers
putexcel A1=("year") B1=("student_share")

* Write data from matrix M (Stata 15+ supports varlists here)
putexcel A2=matrix(M)


