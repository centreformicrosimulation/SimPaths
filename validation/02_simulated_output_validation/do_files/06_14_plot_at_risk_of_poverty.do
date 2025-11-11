********************************************************************************
* PROJECT:  		ESPON 
* SECTION:			Validation
* OBJECT: 			Risk of poverty 
* AUTHORS:			Ashley Burdett 
* LAST UPDATE:		9/25 (AB)
* COUNTRY: 			UK 

* NOTES: 			 
********************************************************************************

********************************************************************************
* 1 : Mean values over time
********************************************************************************

********************************************************************************
* 1.1 : Mean values over time, working age (18-65)
********************************************************************************

* Prepare validation data
use year dwt valid_y_eq_disp_bu_yr using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_y_eq_disp_bu_yr, d

	replace valid_y_eq_disp_bu_yr = . if ///
		valid_y_eq_disp_bu_yr < r(p1) | valid_y_eq_disp_bu_yr > r(p99)

}

qui sum year
local min_year = 2011
local max_year = r(max)  

gen poverty_line = .
forval year = `min_year'/`max_year' {
	
	sum valid_y_eq_disp_bu_yr if year == `year', d
	replace poverty_line = 0.6*r(p50) if year == `year'

}

gen arop = (valid_y_eq_disp_bu_yr < poverty_line)

collapse (mean) arop [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year equivalisedincome using "$dir_data/simulated_data.dta", clear

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum equivalisedincome, d
	replace equivalisedincome = . if ///
		equivalisedincome < r(p1) | equivalisedincome > r(p99)

}

bys run year: egen equivincome_median = median(equivalisedincome)
gen poverty_line = 0.6*equivincome_median
gen arop_sim = (equivalisedincome < poverty_line)

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
	legend(label(1 "Simulated"))) ///
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
graph export "$dir_output_files/poverty/validation_${country}_at_risk_of_poverty_${min_age}_${max_age}.jpg", ///
	replace width(2560) height(1440) quality(100)

	
	
********************************************************************************
* 1.1 : Mean values over time, 18+ 
********************************************************************************

* Prepare validation data
use year dwt valid_y_eq_disp_bu_yr dag using ///
	"$dir_data/ukhls_validation_full_sample.dta", clear

* Select sample 
drop if dag < 18	
	
* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_y_eq_disp_bu_yr, d

	replace valid_y_eq_disp_bu_yr = . if ///
		valid_y_eq_disp_bu_yr < r(p1) | valid_y_eq_disp_bu_yr > r(p99)

}

qui sum year
local min_year = 2011
local max_year = r(max)  

gen poverty_line = .
forval year = `min_year'/`max_year' {
	
	sum valid_y_eq_disp_bu_yr if year == `year', d
	replace poverty_line = 0.6*r(p50) if year == `year'

}

gen arop = (valid_y_eq_disp_bu_yr < poverty_line)

collapse (mean) arop [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year equivalisedincome dag using "$dir_data/simulated_data.dta", clear

* Select sample
drop if dag < 18 

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum equivalisedincome, d
	
	replace equivalisedincome = . if ///
		equivalisedincome < r(p1) | equivalisedincome > r(p99)

}

bys run year: egen equivincome_median = median(equivalisedincome)
gen poverty_line = 0.6*equivincome_median
gen arop_sim = (equivalisedincome < poverty_line)

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
	legend(label(1 "Simulated"))) ///
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
graph export "$dir_output_files/poverty/validation_${country}_at_risk_of_poverty_${min_age}plus.jpg", ///
	replace width(2560) height(1440) quality(100)
	
	
graph drop _all 	
