/*******************************************************************************
* PROJECT:        SimPaths
* SECTION:        ALIGNMENT PROCEDURES
*
* AUTHORS:        Liang Shi (LS)
* LAST UPDATE:    05/02/2026 (LS)
* COUNTRY:        UK 
*
* DATA:           Initial populations			
*
* DESCRIPTION:    This do-file constructs employment targets for different
*                groups of the population using initial population data.
*                It:
*                  - Imports initial population CSV files by year
*                  - Defines benefit-unit (BU) level groups (household types)
*                  - Computes fractional BU-level employment
*                  - Aggregates to employment shares by group and year
*                  - Exports the results to Excel (one sheet per group)
*
* SET-UP:         1. Update the working directory path (global dir_w)
*                 2. Copy the relevant input data into the /input_data folder
*                    under the country-specific subdirectory
*
*******************************************************************************/

clear all


* --- DEFINE GLOBALS -------------------------------------------------------- *

* Working directory (project root)
global dir_w "D:\BaiduSyncdisk\Job CeMPA\employment alignment UK"


* Country code and time span for which targets are produced
global country = "UK"
global min_year 2011
global max_year 2023

* Directory structure
global dir_input_data   "D:\BaiduSyncdisk\Job CeMPA\employment alignment UK\UK\initial population"
global dir_working_data "$dir_w/${country}/working_data"
global dir_output       "$dir_w/${country}"


* Initialise file that will store BU-level employment shares for all years
clear
save "${dir_working_data}/bu_empl_shares_${country}_allsubgroups_initpopdata.dta", emptyok replace                


* ========================================================================== *

// Loop over all years in the requested range
foreach y of numlist $min_year/$max_year {
	
	* Build file name for the given year and import initial population data
	local file = subinstr("population_initial_${country}_YYYY.csv","YYYY","`y'",.)
	import delimited using "${dir_input_data}/`file'", clear
	
	* Identify responsible males/females (18+)
	gen byte is_resp_male   = (demage >= 18 & demmaleflag == 1)
    gen byte is_resp_female = (demage >= 18 & demmaleflag == 0)
	
	* Adult–child flag by sex for adults only
	gen male_adultchildflag   = 0
	gen female_adultchildflag = 0
	replace male_adultchildflag   = demadultchildflag if (demmaleflag == 1 & demage >= 18)
	replace female_adultchildflag = demadultchildflag if (demmaleflag == 0 & demage >= 18)
	
	* BU-level (benefit-unit) aggregates of adult–child flags
	bys idbu: egen byte bu_male_AC   = max(male_adultchildflag)
	bys idbu: egen byte bu_female_AC = max(female_adultchildflag)

	* BU-level indicators of whether there is a responsible male/female
	bys idbu: egen byte has_resp_male   = max(is_resp_male)
	bys idbu: egen byte has_resp_female = max(is_resp_female)
	
	* Benefit-unit occupancy type (couples vs single male/female)
	gen str7 occcupancy = ""
	replace occcupancy = "Couples"       if (has_resp_male==1 & has_resp_female==1)
	replace occcupancy = "Single_male"   if (has_resp_male==1 & has_resp_female==0)
	replace occcupancy = "Single_female" if (has_resp_female==1 & has_resp_male==0)
	
	* Individual "at risk of employment" (working-age, not retired, not student, do not need care)
	gen byte maleAtRisk   = ( (demmaleflag == 1) & !(labc4 == 2 | labc4 == 4 | demage < 16 | demage > 75 ) )
	gen byte femaleAtRisk = ( (demmaleflag == 0) & !(labc4 == 2 | labc4 == 4 | demage < 16 | demage > 75 ) )
	
	* BU-level indicators of whether there is at least one male/female at risk
	bys idbu: egen byte bu_maleAtRisk   = max(maleAtRisk)
	bys idbu: egen byte bu_femaleAtRisk = max(femaleAtRisk)
	
	* Group codes for benefit-units (by occupancy and dependency/AC status)
	gen str6 group_code = ""
	replace group_code = "Couples"           if (occcupancy == "Couples"       & bu_maleAtRisk==1   & bu_femaleAtRisk==1)
	replace group_code = "SingleDep_Males"   if (occcupancy == "Couples"       & bu_maleAtRisk==1   & bu_femaleAtRisk!=1)
	replace group_code = "SingleDep_Females" if (occcupancy == "Couples"       & bu_maleAtRisk!=1   & bu_femaleAtRisk==1)
	
	replace group_code = "Single_male"       if (occcupancy == "Single_male"   & bu_male_AC==0)
	replace group_code = "SingleAC_Males"    if (occcupancy == "Single_male"   & bu_male_AC==1)

	replace group_code = "Single_female"     if (occcupancy == "Single_female" & bu_female_AC==0)
	replace group_code = "SingleAC_Females"  if (occcupancy == "Single_female" & bu_female_AC==1)
		
	
	* ---------- BU-LEVEL FRACTIONAL EMPLOYMENT ----------------------------- *
	* Person-level employment indicator (1 if employed)
	gen byte employed = (labc4 == 1)
		
	* Restrict to the relevant BU groups
	keep if inlist(group_code,"Couples","SingleDep_Males","SingleDep_Females","Single_male","Single_female","SingleAC_Males","SingleAC_Females")
	
	* Employment of responsible male/female adults, consistent with Java labour module:
	* if an adult is not at risk of work, treat them as non-employed in target construction.
	gen byte male_emp   = employed if (demmaleflag==1 & demage>=18)
	gen byte female_emp = employed if (demmaleflag==0 & demage>=18)
	replace male_emp   = 0 if (demmaleflag==1 & demage>=18 & bu_maleAtRisk!=1)
	replace female_emp = 0 if (demmaleflag==0 & demage>=18 & bu_femaleAtRisk!=1)

	* Collapse to BU: whether the responsible male/female (if present) is employed
	bys idbu: egen byte bu_male_emp   = max(male_emp)
	bys idbu: egen byte bu_female_emp = max(female_emp)

	* Replace missing BU employment with 0 (no employed responsible adult of that sex)
	replace bu_male_emp   = 0 if missing(bu_male_emp)
	replace bu_female_emp = 0 if missing(bu_female_emp)

	* Number of responsible adults in the BU
	gen byte bu_nresp = has_resp_male + has_resp_female

	* Fractional BU employment: 0, 0.5, or 1 depending on how many responsible adults work
	gen double bu_fracemployed = .
	replace bu_fracemployed = (bu_male_emp + bu_female_emp) / bu_nresp if bu_nresp>0

	* Safety check: if no responsible adult (should not happen), set to 0
	replace bu_fracemployed = 0 if bu_nresp==0

	* Consistency guard: SingleDep groups cannot exceed 0.5 by construction
	assert bu_fracemployed <= 0.5 if inlist(group_code,"SingleDep_Males","SingleDep_Females")
	
	* ---------- END BU-LEVEL FRACTIONAL EMPLOYMENT ------------------------ *

	* Partner-specific employment targets for SingleDep groups
	gen double bu_target_emp = bu_fracemployed
//     replace bu_target_emp = bu_male_emp if group_code == "SingleDep_Males"
// 	replace bu_target_emp = bu_female_emp if group_code == "SingleDep_Females"
	
	* BU-level weight: sum of person-level weights within each BU
	bys idbu: egen double bu_w = total(wgthhcross)
	
	* Keep one record per BU (so employment shares are BU-level, not person-level)
	bys idbu: gen byte bu_tag = _n == 1
	keep if bu_tag
	
	* Compute (weighted) mean employment share by group
	collapse (mean) empl_share = bu_target_emp [pw = bu_w], by(group_code)
	gen year = `y'

	* Append to cumulative file for all years
	append using "${dir_working_data}/bu_empl_shares_${country}_allsubgroups_initpopdata.dta"
	duplicates drop
	save "${dir_working_data}/bu_empl_shares_${country}_allsubgroups_initpopdata.dta", replace

}

* -------------------------------------------------------------------------- *
* POST-PROCESSING: export aggregated results to Excel
* -------------------------------------------------------------------------- *

* Load aggregated BU-level employment shares for all years
use "${dir_working_data}/bu_empl_shares_${country}_allsubgroups_initpopdata.dta", clear

* Sort by year for neat export
sort year

* Create/overwrite Excel file that will hold all sheets
putexcel set "${dir_output}/employment_targets.xlsx", replace

* Identify all BU group codes
levelsof group_code, local(groups)

* Loop over groups and export each to its own sheet
foreach g of local groups {
	preserve
	keep if group_code == "`g'"
	sort year
	
	* Build a matrix of all rows for the two variables (year, empl_share)
	mkmat year empl_share, matrix(M)
	
	* Point putexcel at the output file and the group-specific sheet
	putexcel set "${dir_output}/employment_targets.xlsx", sheet("`g'") modify

	* Write headers
	putexcel A1=("year") B1=("empl_share")

	* Write data from matrix M (Stata 15+ supports varlists here)
	putexcel A2=matrix(M)

	restore
}
