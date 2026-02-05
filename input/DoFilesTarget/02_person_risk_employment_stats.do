/*******************************************************************************
* PROJECT:        SimPaths
* SECTION:        ALIGNMENT PROCEDURES
*
* AUTHORS:        Liang Shi (LS)
* LAST UPDATE:    05/02/2026
* COUNTRY:        UK
*
* DATA:           Initial populations
*
* DESCRIPTION:    This do-file constructs person-level diagnostics by year
*                and benefit-unit subgroup:
*                  - n_person, n_employed, n_atrisk, n_atrisk_employed
*                  - n_student, n_student_employed
*                  - n_retired, n_retired_employed
*                  - n_disabled, n_disabled_employed
*                It exports the results to CSV.
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
global dir_input_data   "D:\CeMPA\SimPaths\input\InitialPopulations"
global dir_working_data "$dir_w/${country}/working_data"
global dir_output       "$dir_w/${country}"


* Initialise file that will store person-level risk/employment stats for all years
clear
save "${dir_working_data}/person_risk_emp_${country}_allsubgroups_initpopdata.dta", emptyok replace                

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
	
	* Individual "at risk of employment" (working-age, not retired, not student, not permanently disabled)
	gen byte maleAtRisk   = ( (demmaleflag == 1) & !(labc4 == 2 | labc4 == 4 | healthdsbllongtermflag == 1 | demage < 16 | demage > 75 | careneedflag == 1) )
	gen byte femaleAtRisk = ( (demmaleflag == 0) & !(labc4 == 2 | labc4 == 4 | healthdsbllongtermflag == 1 | demage < 16 | demage > 75 | careneedflag == 1) )
	
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

	* Restrict to relevant BU groups
	keep if inlist(group_code,"Couples","SingleDep_Males","SingleDep_Females","Single_male","Single_female","SingleAC_Males","SingleAC_Females")

	* Person-level flags
	gen byte employed = (labc4 == 1)
	gen byte at_risk = .
	replace at_risk = maleAtRisk if demmaleflag == 1
	replace at_risk = femaleAtRisk if demmaleflag == 0
	gen byte student = (labc4 == 4)
	gen byte retired = (labc4 == 2)
	gen byte disabled = (healthdsbllongtermflag == 1)
	gen byte careneeded = (careneedflag == 1)

	gen byte at_risk_employed = employed & at_risk
	gen byte student_employed = employed & student
	gen byte retired_employed = employed & retired
	gen byte disabled_employed = employed & disabled
	gen byte careneeded_employed = employed & careneeded
	gen byte n_person = 1

	* Aggregate to subgroup counts
	  collapse (sum) ///
	  n_person ///
      n_employed=employed ///
      n_atrisk=at_risk ///
      n_atrisk_employed=at_risk_employed ///
      n_student=student ///
      n_student_employed=student_employed ///
      n_retired=retired ///
      n_retired_employed=retired_employed ///
      n_disabled=disabled ///
      n_disabled_employed=disabled_employed ///
	  n_careneeded = careneeded ///
	  n_careneeded_employed = careneeded_employed, by(group_code)
	gen year = `y'

	append using "${dir_working_data}/person_risk_emp_${country}_allsubgroups_initpopdata.dta"
	duplicates drop
	save "${dir_working_data}/person_risk_emp_${country}_allsubgroups_initpopdata.dta", replace
}

* -------------------------------------------------------------------------- *
* POST-PROCESSING: export person-level risk/employment diagnostics to CSV
* -------------------------------------------------------------------------- *

use "${dir_working_data}/person_risk_emp_${country}_allsubgroups_initpopdata.dta", clear
sort year group_code
export delimited using "${dir_output}/employment_risk_emp_stats.csv", replace
