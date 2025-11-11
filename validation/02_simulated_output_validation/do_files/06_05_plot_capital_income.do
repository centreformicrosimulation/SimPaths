********************************************************************************
* PROJECT:  		ESPON 
* SECTION:			Validation
* OBJECT: 			Capital income
* AUTHORS:			Ashley Burdett 
* LAST UPDATE:		9/25 (AB)
* COUNTRY: 			UK 

* NOTES: 			This do file plots simulated and UKHLS capital income, 
*					per benefit unit

********************************************************************************

********************************************************************************
* 1 : Time series
********************************************************************************

********************************************************************************
* 1.1 : Mean through time, adult population, bu
********************************************************************************

* Prepare validation data
use year dwt valid_y_gross_capital_bu_yr using /// 
	"$dir_data/ukhls_validation_sample.dta", clear


* Trim outliers
/*
if "$trim_outliers" == "true" {
	
	sum valid_y_gross_capital_bu_yr, d
	
	replace valid_y_gross_capital_bu_yr = . if ///
		valid_y_gross_capital_bu_yr < r(p1) | ///
		valid_y_gross_capital_bu_yr > r(p99)
		
}
*/

collapse (mean) valid_y_gross_capital_bu_yr [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year sim_ypncp_lvl_bu using "$dir_data/simulated_data.dta", clear


* Trim outliers
/*
if "$trim_outliers" == "true" {
	
	sum sim_ypncp_lvl_bu, d
	
	replace sim_ypncp_lvl_bu = . if ///
		sim_ypncp_lvl_bu < r(p1) | sim_ypncp_lvl_bu > r(p99)
		
}
*/

collapse (mean) sim_ypncp_lvl_bu, by(run year)

collapse (mean) sim_ypncp_lvl_bu ///
		 (sd) sim_ypncp_lvl_bu_sd = sim_ypncp_lvl_bu ///
		 , by(year)
		 
foreach varname in sim_ypncp_lvl_bu {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd
	
}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea sim_ypncp_lvl_bu_high sim_ypncp_lvl_bu_low year, sort ///
	color(green%20) legend(label(1 "Simulated"))) ///
(line valid_y_gross_capital_bu_yr year, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	title("Capital income") ///
	subtitle("Ages 18-65") ///
	xtitle("Year", size(small)) ///
	ytitle("GBP per year", size(small)) ///
	ylabel(,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: Series represents average benefit unit capital income. Statistics computed by averaging benefit individual level gross" "capital income over all persons ages 18-65. Amounts in 2015 prices. Top and bottom percentiles trimmed. ", ///
	size(vsmall))

* Save figure
graph export ///
"$dir_output_files/income/capital_income/validation_${country}_capital_income_ts_${min_age}_${max_age}_both.jpg", ///
	replace width(2400) height(1350) quality(100)

	
********************************************************************************
* 1.2 : Share with no capital income 
********************************************************************************

* Share with no capital income 
* Prepare validation data
use year dwt valid_y_gross_capital_bu_yr using /// 
	"$dir_data/ukhls_validation_sample.dta", clear

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_y_gross_capital_bu_yr, d
	
	replace valid_y_gross_capital_bu_yr = . if ///
		valid_y_gross_capital_bu_yr < r(p1) | ///
		valid_y_gross_capital_bu_yr > r(p99)
		
}

gen valid_no_capital = (valid_y_gross_capital_bu_yr == 0)

collapse (mean) valid_no_capital [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year sim_ypncp_lvl_bu using "$dir_data/simulated_data.dta", clear

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum sim_ypncp_lvl_bu, d
	
	replace sim_ypncp_lvl_bu = . if ///
		sim_ypncp_lvl_bu < r(p1) | sim_ypncp_lvl_bu > r(p99)
		
}

gen sim_no_capital = (sim_ypncp_lvl_bu == 0)

collapse (mean) sim_no_capital, by(run year)
collapse (mean) sim_no_capital ///
		 (sd) sim_no_capital_sd = sim_no_capital ///
		 , by(year)
		 
foreach varname in sim_no_capital {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd
	
}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea sim_no_capital_high sim_no_capital_low year, sort ///
	color(green%20) legend(label(1 "Simulated"))) ///
(line valid_no_capital year, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	title("No Capital Income") ///
	subtitle("Ages 18-65") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	ylabel(,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: Series represents share of individual that report not receiveing any capital income in their benefit unit, annual. Top and bottom" "percentiles trimmed.", ///
	size(vsmall))

* Save figure
graph export ///
"$dir_output_files/income/capital_income/validation_${country}_no_capital_income_ts_${min_age}_${max_age}_both.jpg", ///
	replace width(2400) height(1350) quality(100)
	

********************************************************************************
* 2 : Histograms 
********************************************************************************

********************************************************************************
* 2.1 : Ages 18-65, by year 
********************************************************************************

* Prepare validation data
use year dwt valid_y_gross_capital_bu_yr using ///
	"$dir_data/ukhls_validation_sample.dta", clear


* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_y_gross_capital_bu_yr, d
	
	replace valid_y_gross_capital_bu_yr = . if ///
		valid_y_gross_capital_bu_yr < r(p1) | ///
		valid_y_gross_capital_bu_yr > r(p99)

}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year sim_ypncp_lvl_bu using "$dir_data/simulated_data.dta", clear

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum sim_ypncp_lvl_bu, d
	
	replace sim_ypncp_lvl_bu = . if ///
		sim_ypncp_lvl_bu < r(p1) | sim_ypncp_lvl_bu > r(p99)

}

append using "$dir_data/temp_valid_stats.dta"

qui sum year
local min_year = 2011
local max_year = r(max)  

forval year = `min_year'/`max_year' {
	
	twoway (hist sim_ypncp_lvl_bu if year == `year' & ///
		sim_ypncp_lvl_bu < 100, width(1) color(green%30) ///
		legend(label(1 "Simulated"))) ///
	(hist valid_y_gross_capital_bu_yr if year == `year' & ///
		valid_y_gross_capital_bu_yr < 100, ///
		 width(1) color(red%30) legend(label(2 "UKHLS"))) , ///
		title("Capital Income") ///
		subtitle("`year'") ///
		name(capital_inc_`year'_all, replace) ///
		ylabel(,labsize(small)) ///
		xlabel(,labsize(small)) ///		
		xtitle("GBP", size(small)) ///
		ytitle("Density", size(small)) ///
		legend(size(small)) ///
		graphregion(color(white)) ///
		note("Notes: Benefit unit capital income reported, individual observations plotted. Sample includes individuals age 18-65. Amounts in GBP" "per year, 2015 prices. X axis range limited to 100. Top and bottom percentiles trimmed.", ///
		size(vsmall))
		
	graph export ///
	"$dir_output_files/income/capital_income/validation_${country}_capital_income_dist_`year'.png", ///
		replace width(2560) height(1440) 
	
}
	

********************************************************************************
* 2.2 : Ages 18-65, positive amounts only 
********************************************************************************
	
* Prepare validation data
use year dwt valid_y_gross_capital_bu_yr using ///
	"$dir_data/ukhls_validation_sample.dta", clear


* Trim outliers
/*
if "$trim_outliers" == "true" {
	
	sum valid_y_gross_capital_bu_yr, d
	
	replace valid_y_gross_capital_bu_yr = . if ///
		valid_y_gross_capital_bu_yr < r(p1) | ///
		valid_y_gross_capital_bu_yr > r(p99)

}
*/

drop if valid_y_gross_capital_bu_yr == 0 

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year sim_ypncp_lvl_bu using "$dir_data/simulated_data.dta", clear

* Trim outliers
/*
if "$trim_outliers" == "true" {
	
	sum sim_ypncp_lvl_bu, d
	
	replace sim_ypncp_lvl_bu = . if ///
		sim_ypncp_lvl_bu < r(p1) | sim_ypncp_lvl_bu > r(p99)

}
*/

drop if sim_ypncp_lvl_bu == 0 

append using "$dir_data/temp_valid_stats.dta"

qui sum year
local min_year = 2011
local max_year = r(max)  

forval year = `min_year'/`max_year' {
	
	twoway (hist sim_ypncp_lvl_bu if year == `year' & ///
		sim_ypncp_lvl_bu < 4000, ///
		width(25) color(green%30) legend(label(1 "Simulated"))) ///
	(hist valid_y_gross_capital_bu_yr if year == `year' & ///
		valid_y_gross_capital_bu_yr < 4000,  width(25) color(red%30) ///
		legend(label(2 "UKHLS"))) , ///
		title("Capital Income") ///
		subtitle("Positive amounts, `year'") ///
		name(capital_inc_`year'_all, replace) ///
		ylabel(,labsize(small)) ///
		xlabel(,labsize(small)) ///		
		xtitle("GBP", size(small)) ///
		ytitle("Density", size(small)) ///
		legend(size(small)) ///
		graphregion(color(white)) ///
		note("Notes: Benefit unit capital income reported, individual observations plotted. Sample includes individuals age 18-65. Amounts in GBP" "per year, 2015 prices. X axis range limited to 4000. ", ///
		size(vsmall))
		
	graph export ///
"$dir_output_files/income/capital_income/validation_${country}_positive_capital_income_dist_`year'.png", ///
		replace width(2560) height(1440) 
	
}
	
graph drop _all 	
