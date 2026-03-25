/*******************************************************************************
* PROJECT:  		SimPaths UK 
* SECTION:			Validation
* OBJECT: 			Risk of poverty 
* AUTHORS:			Ashley Burdett 
* LAST UPDATE:		Jan 2026
* COUNTRY: 			UK 
********************************************************************************
* NOTES: 			 
*******************************************************************************/

********************************************************************************
* 1 : Mean values over time
********************************************************************************

********************************************************************************
* 1.1 : Mean values over time, working age (18-65)
********************************************************************************

* Prepare validation data
use year demAge dwt valid_yDispBuEquivYear using ///
	"$dir_data/ukhls_validation_sample.dta", clear

keep if inrange(demAge,18,65)	
	
* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_yDispBuEquivYear, d

	replace valid_yDispBuEquivYear = . if ///
		valid_yDispBuEquivYear < r(p1) | valid_yDispBuEquivYear > r(p99)

}

qui sum year
local min_year = 2011
local max_year = r(max)  

gen poverty_line = .
forval year = `min_year'/`max_year' {
	
	sum valid_yDispBuEquivYear if year == `year', d
	replace poverty_line = 0.6*r(p50) if year == `year'

}

gen arop = (valid_yDispBuEquivYear < poverty_line)

collapse (mean) arop [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year demAge sim_yDispEquivYear using ///
	"$dir_data/simulation_sample.dta", clear

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum sim_yDispEquivYear, d
	
	replace sim_yDispEquivYear = . if ///
		sim_yDispEquivYear < r(p1) | sim_yDispEquivYear > r(p99)

}

bys run year: egen equivincome_median = median(sim_yDispEquivYear)

gen poverty_line = 0.6*equivincome_median

gen arop_sim = (sim_yDispEquivYear < poverty_line)

collapse (mean) arop_sim, by(run year)

collapse (mean) arop_sim ///
		 (sd) arop_sim_sd = arop_sim ///
		 , by(year)
		 
foreach varname in arop_sim {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea arop_sim_high arop_sim_low year, sort color(green%20) ///
	legend(label(1 "SimPaths"))) ///
(line arop year, sort color(green) legend(label(2 "UKHLS"))), ///
	title("At Risk of Poverty") ///
	subtitle("Ages 18-65") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Note: Poverty line calculated within each year as 60% of the median equivalised disposable income of benefit unit. Calculated" "using individual level observations.", ///
	size(vsmall))

* Save figure
graph export "$dir_output_files/poverty/validation_${country}_at_risk_of_poverty_18_${max_age}.jpg", ///
	replace width(2560) height(1440) quality(100)

	
	
********************************************************************************
* 1.1 : Mean values over time, 18+ 
********************************************************************************

* Prepare validation data
use year demAge dwt valid_yDispBuEquivYear using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Select sample 
drop if demAge < 18	
	
* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_yDispBuEquivYear, d

	replace valid_yDispBuEquivYear = . if ///
		valid_yDispBuEquivYear < r(p1) | valid_yDispBuEquivYear > r(p99)

}

qui sum year
local min_year = 2011
local max_year = r(max)  

gen poverty_line = .

forval year = `min_year'/`max_year' {
	
	sum valid_yDispBuEquivYear if year == `year', d
	replace poverty_line = 0.6*r(p50) if year == `year'

}

gen arop = (valid_yDispBuEquivYear < poverty_line)

collapse (mean) arop [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year demAge sim_yDispEquivYear using ///
	"$dir_data/simulation_sample.dta", clear

* Select sample
drop if demAge < 18 

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum sim_yDispEquivYear, d
	
	replace sim_yDispEquivYear = . if ///
		sim_yDispEquivYear < r(p1) | sim_yDispEquivYear > r(p99)

}

bys run year: egen equivincome_median = median(sim_yDispEquivYear)

gen poverty_line = 0.6*equivincome_median

gen arop_sim = (sim_yDispEquivYear < poverty_line)

collapse (mean) arop_sim, by(run year)

collapse (mean) arop_sim ///
		 (sd) arop_sim_sd = arop_sim ///
		 , by(year)
		 
foreach varname in arop_sim {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea arop_sim_high arop_sim_low year, sort color(green%20) ///
	legend(label(1 "SimPaths"))) ///
(line arop year, sort color(green) legend(label(2 "UKHLS"))), ///
	title("At Risk of Poverty") ///
	subtitle("Ages 18+") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Note: Poverty line calculated within each year as 60% of the median equivalised disposable income of benefit unit. Calculated" "using individual level observations.", ///
	size(vsmall))

* Save figure
graph export "$dir_output_files/poverty/validation_${country}_at_risk_of_poverty_18plus.jpg", ///
	replace width(2560) height(1440) quality(100)
	
	
graph drop _all 	
