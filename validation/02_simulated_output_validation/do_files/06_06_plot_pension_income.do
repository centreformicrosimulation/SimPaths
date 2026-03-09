********************************************************************************
* PROJECT:  		ESPON 
* SECTION:			Validation
* OBJECT: 			Pension income
* AUTHORS:			Ashley Burdett 
* LAST UPDATE:		9/25 (AB)
* COUNTRY: 			UK 

* NOTES: 			This do file plots simulated and UKHLS pension income, 
*					per benefit unit

********************************************************************************

********************************************************************************
* 1: Time series
********************************************************************************

********************************************************************************
* 1.1: Mean through time, bu
********************************************************************************

* Prepare validation data
use year dwt valid_y_gross_pension_bu_yr dag using /// 
	"$dir_data/ukhls_validation_full_sample.dta", clear

* Sample selection 
drop if dag < 65
	
* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_y_gross_pension_bu_yr, d
	
	replace valid_y_gross_pension_bu_yr = . if ///
		valid_y_gross_pension_bu_yr < r(p1) | ///
		valid_y_gross_pension_bu_yr > r(p99)
		
}

collapse (mean) valid_y_gross_pension_bu_yr [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year sim_ypnoab_lvl_bu dag using "$dir_data/simulated_data.dta", clear

* Sample selection 
drop if dag < 65

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum sim_ypnoab_lvl_bu, d
	
	replace sim_ypnoab_lvl_bu = . if ///
		sim_ypnoab_lvl_bu < r(p1) | sim_ypnoab_lvl_bu > r(p99)
		
}


collapse (mean) sim_ypnoab_lvl_bu, by(run year)
collapse (mean) sim_ypnoab_lvl_bu ///
		 (sd) sim_ypnoab_lvl_bu_sd = sim_ypnoab_lvl_bu ///
		 , by(year)
		 
foreach varname in sim_ypnoab_lvl_bu {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd
	
}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea sim_ypnoab_lvl_bu_high sim_ypnoab_lvl_bu_low year, sort ///
	color(green%20) legend(label(1 "Simulated"))) ///
(line valid_y_gross_pension_bu_yr year, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	title("Private Pension Income") ///
	subtitle("Ages 65+") ///
	xtitle("Year", size(small)) ///
	ytitle("GBP per year", size(small)) ///
	ylabel(,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: Series represents average benefit unit private pension income. Statistics computed by averaging benefit individual level gross" "private pension income over all persons ages 65+. Amounts in 2015 prices. Top and bottom percentiles trimmed. ", ///
	size(vsmall))

* Save figure
graph export ///
"$dir_output_files/income/pension_income/validation_${country}_pension_income_ts_${max_age}plus_both.jpg", ///
	replace width(2400) height(1350) quality(100)

	
********************************************************************************
* 1.2 : Share with no pension income 
********************************************************************************

* Prepare validation data
use year dwt valid_y_gross_pension_bu_yr dag using /// 
	"$dir_data/ukhls_validation_full_sample.dta", clear

* Sample selection 
drop if dag < 65	
	
* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_y_gross_pension_bu_yr, d
	
	replace valid_y_gross_pension_bu_yr = . if ///
		valid_y_gross_pension_bu_yr < r(p1) | ///
		valid_y_gross_pension_bu_yr > r(p99)
		
}

gen valid_no_pension = (valid_y_gross_pension_bu_yr == 0)

collapse (mean) valid_no_pension [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year sim_ypnoab_lvl_bu dag using ///
	"$dir_data/simulated_data_full.dta", clear

* Sample selection 
drop if dag < 65

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum sim_ypnoab_lvl_bu, d
	
	replace sim_ypnoab_lvl_bu = . if ///
		sim_ypnoab_lvl_bu < r(p1) | sim_ypnoab_lvl_bu > r(p99)
		
}

gen sim_no_pension = (sim_ypnoab_lvl_bu == 0)

collapse (mean) sim_no_pension, by(run year)
collapse (mean) sim_no_pension ///
		 (sd) sim_no_pension_sd = sim_no_pension ///
		 , by(year)
		 
foreach varname in sim_no_pension {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd
	
}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea sim_no_pension_high sim_no_pension_low year, sort ///
	color(green%20) legend(label(1 "Simulated"))) ///
(line valid_no_pension year, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	title("No Private Pension Income") ///
	subtitle("Ages 65+") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	ylabel(,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: Series represents the share of individual who report not receiving any gross private pension income in their benefit unit, annual." "Top and bottom percentiles trimmed.", ///
	size(vsmall))

* Save figure
graph export ///
"$dir_output_files/income/pension_income/validation_${country}_no_pension_income_ts_${max_age}plus_both.jpg", ///
	replace width(2400) height(1350) quality(100)
	

********************************************************************************
* 2 : Histograms 
********************************************************************************

********************************************************************************
* 2.1 : 65+, by year 
********************************************************************************

* Prepare validation data
use year dwt valid_y_gross_pension_bu_yr dag using ///
	"$dir_data/ukhls_validation_full_sample.dta", clear

* Sample selection 
drop if dag < 65
	
* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_y_gross_pension_bu_yr, d
	
	replace valid_y_gross_pension_bu_yr = . if ///
		valid_y_gross_pension_bu_yr < r(p1) | ///
		valid_y_gross_pension_bu_yr > r(p99)

}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year sim_ypnoab_lvl_bu dag using ///
	"$dir_data/simulated_data_full.dta", clear

* Sample selection 
drop if dag < 65	
	
* Trim outliers
if "$trim_outliers" == "true" {
	
	sum sim_ypnoab_lvl_bu, d
	
	replace sim_ypnoab_lvl_bu = . if ///
		sim_ypnoab_lvl_bu < r(p1) | sim_ypnoab_lvl_bu > r(p99)

}

append using "$dir_data/temp_valid_stats.dta"

qui sum year
local min_year = 2011
local max_year = r(max)  

forval year = `min_year'/`max_year' {
	
	twoway (hist sim_ypnoab_lvl_bu if year == `year' & ///
		sim_ypnoab_lvl_bu < 100, width(1) color(green%30) ///
		legend(label(1 "Simulated"))) ///
	(hist valid_y_gross_pension_bu_yr if year == `year' & ///
		valid_y_gross_pension_bu_yr < 100, ///
		 width(1) color(red%30) legend(label(2 "UKHLS"))) , ///
		title("Private Pension Income") ///
		subtitle("`year'") ///
		name(capital_inc_`year'_all, replace) ///
		ylabel(,labsize(small)) ///
		xlabel(,labsize(small)) ///		
		xtitle("GBP", size(small)) ///
		ytitle("Density", size(small)) ///
		legend(size(small)) ///
		graphregion(color(white)) ///
		note("Notes: Benefit unit gross private pension income reported, individual observations plotted. Sample includes individuals age 65+." "Amounts in GBP per year, 2015 prices. X axis range limited to 100. Top and bottom percentiles trimmed.", ///
		size(vsmall))
		
	graph export ///
	"$dir_output_files/income/pension_income/validation_${country}_pension_income_dist_`year'.png", ///
		replace width(2560) height(1440) 
	
}
	

********************************************************************************
* 2.2 : Ages 65+, positive amounts only 
********************************************************************************
	
* Prepare validation data
use year dwt valid_y_gross_pension_bu_yr dag using ///
	"$dir_data/ukhls_validation_full_sample.dta", clear

* Select sample 
drop if dag < 65	
	

* Trim outliers
/*
if "$trim_outliers" == "true" {
	
	sum valid_y_gross_capital_bu_yr, d
	
	replace valid_y_gross_capital_bu_yr = . if ///
		valid_y_gross_capital_bu_yr < r(p1) | ///
		valid_y_gross_capital_bu_yr > r(p99)

}
*/

drop if valid_y_gross_pension_bu_yr == 0 

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year sim_ypnoab_lvl_bu dag using ///
	"$dir_data/simulated_data_full.dta", clear

* Select sample 
drop if dag < 65	

* Trim outliers
/*
if "$trim_outliers" == "true" {
	
	sum sim_ypncp_lvl_bu, d
	
	replace sim_ypncp_lvl_bu = . if ///
		sim_ypncp_lvl_bu < r(p1) | sim_ypncp_lvl_bu > r(p99)

}
*/

drop if sim_ypnoab_lvl_bu == 0 

append using "$dir_data/temp_valid_stats.dta"

qui sum year
local min_year = 2011
local max_year = r(max)  

forval year = `min_year'/`max_year' {
	
	twoway (hist sim_ypnoab_lvl_bu if year == `year' & ///
		sim_ypnoab_lvl_bu < 35000, ///
		width(200) color(green%30) legend(label(1 "Simulated"))) ///
	(hist valid_y_gross_pension_bu_yr if year == `year' & ///
		valid_y_gross_pension_bu_yr < 35000,  width(200) color(red%30) ///
		legend(label(2 "UKHLS"))) , ///
		title("Private Pension Income") ///
		subtitle("Positive amounts, `year'") ///
		name(capital_inc_`year'_all, replace) ///
		ylabel(,labsize(small)) ///
		xlabel(,labsize(small)) ///		
		xtitle("GBP", size(small)) ///
		ytitle("Density", size(small)) ///
		legend(size(small)) ///
		graphregion(color(white)) ///
		note("Notes: Benefit unit gross private pension income reported, individual observations plotted. Sample includes individuals age 65+." "Amounts in GBP per year, 2015 prices. X axis range limited to 35000.", ///
		size(vsmall))
		
	graph export ///
"$dir_output_files/income/pension_income/validation_${country}_positive_pension_income_dist_`year'.png", ///
		replace width(2560) height(1440) 
	
}
	
graph drop _all 	
