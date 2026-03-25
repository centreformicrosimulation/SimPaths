********************************************************************************
* PROJECT:  		SimPaths UK
* SECTION:			Validation
* OBJECT: 			Hourly wages
* AUTHORS:			Ashley Burdett 
* LAST UPDATE:		Jan 2026
* COUNTRY: 			UK 
********************************************************************************
* NOTES: 			This master do file organises do files used for validating 
* 					SimPaths model using UKHLS data. 
********************************************************************************

********************************************************************************
* 1 : Mean values over time
********************************************************************************

********************************************************************************
* 1.1 : Mean values over time - 16-65
********************************************************************************

* Prepare validation data
use year demAge dwt labC4 valid_wage using ///
	"$dir_data/ukhls_validation_sample.dta", clear


* Select sample 
keep if labC4 == 1
keep if inrange(demAge,16,65)

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_wage, d
	
	replace valid_wage = . if ///
		valid_wage < r(p1) | valid_wage > r(p99)

}

* Compute means
collapse (mean) valid_wage [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year labC4 sim_pred_wage demAge using ///
	"$dir_data/simulation_sample.dta", clear

* Select sample
keep if labC4 == "EmployedOrSelfEmployed"
keep if inrange(demAge,16,65)

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum sim_pred_wage, d

	replace sim_pred_wage = . if ///
		sim_pred_wage < r(p1) | sim_pred_wage > r(p99)

}

* Compute means and sd
collapse (mean) sim_pred_wage, by(run year)

collapse (mean) sim_pred_wage ///
		 (sd) sim_pred_wage_sd = sim_pred_wage ///
		 , by(year)
		 
* Approx 95% confidence interval		 
foreach varname in sim_pred_wage {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

* Combine datasets 
merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea sim_pred_wage_high ///
	sim_pred_wage_low year, ///
	sort color(green%20) legend(label(1  "SimPaths"))) ///
(line valid_wage year, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	title("Hourly Wage") ///
	subtitle("Ages 16-65") ///
	xtitle("Year", size(small)) ///
	ytitle("GBP per hour", size(small)) ///
	ylabel(,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: Statistics calculated on sample of employed and self-employed individuals. Amounts in 2015 prices.""Top and bottom percentiles trimmed.", ///
	size(vsmall))

* Save figure
graph export ///
"$dir_output_files/wages/validation_${country}_wages_ts_16_65_both.jpg", ///
	replace width(2560) height(1440) quality(100)
	
	
********************************************************************************
* 1.2 : Mean values over time - 16-65, by gender
********************************************************************************

* Prepare validation data
use year demAge dwt labC4 valid_wage demMaleFlag using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* select sample 
keep if labC4 == 1
keep if inrange(demAge,16,65)

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_wage, d
	
	replace valid_wage = . if ///
		valid_wage < r(p1) | valid_wage > r(p99)

}

* Compute mean
collapse (mean) valid_wage [aw = dwt], by(year demMaleFlag)

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year labC4 sim_pred_wage demMaleFlag demAge using ///
	"$dir_data/simulation_sample.dta", clear

* Select sample
keep if labC4 == "EmployedOrSelfEmployed"
keep if inrange(demAge,16,65)

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum sim_pred_wage , d
	
	replace sim_pred_wage = . if ///
		sim_pred_wage < r(p1) | sim_pred_wage > r(p99)
		
}

collapse (mean) sim_pred_wage, by(run year demMaleFlag)

collapse (mean) sim_pred_wage ///
		 (sd) sim_pred_wage_sd = sim_pred_wage ///
		 , by(year demMaleFlag)
		 
foreach varname in sim_pred_wage {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year demMaleFlag using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure  
twoway (rarea sim_pred_wage_high ///
	sim_pred_wage_low year if demMaleFlag == 0, ///
	sort color(green%20) legend(label(1  "SimPaths"))) ///
(line valid_wage year if demMaleFlag == 0, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	subtitle("Females") ///
	name(wages_female, replace) ///
	xtitle("Year", size(small)) ///
	ytitle("GBP per hour", size(small)) ///
	ylabel(,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) 
	
twoway (rarea sim_pred_wage_high ///
	sim_pred_wage_low year if demMaleFlag == 1, ///
	sort color(green%20) legend(label(1  "SimPaths"))) ///
(line valid_wage year if demMaleFlag == 1, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	subtitle("Males") ///
	name(wages_male, replace)	///
	xtitle("Year", size(small)) ///
	ytitle("GBP per hour", size(small)) ///
	ylabel(,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) 
	
	
grc1leg wages_female wages_male, ///
	title("Hourly Wage") ///
	subtitle("Ages 16-65") ///
	legendfrom(wages_female) rows(1) ///
	graphregion(color(white)) ///
	ycomm ///
	note("Notes: Statistics calculated on sample of employed anf self-employed individuals. Amounts in 2015 prices. Top and bottom" "percentiles trimmed.", ///
	size(vsmall))
	
* Save figure
graph export ///
"$dir_output_files/wages/validation_${country}_wages_ts_16_65_gender.jpg", ///
	replace width(2560) height(1440) quality(100)
	
graph drop _all	
	

********************************************************************************
* 2 : Histograms by year
********************************************************************************

********************************************************************************
* 2.1 : Histograms by year - ages 16-65
********************************************************************************

* Prepare validation data
use year demAge dwt labC4 valid_wage demAge flag_wage_imp_panel ///
	flag_wage_hotdeck using "$dir_data/ukhls_validation_sample.dta", clear

* Select sample
keep if labC4 == 1
keep if inrange(demAge,16,65)

drop labC4

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_wage, d
	
	replace valid_wage = . if ///
		valid_wage < r(p1) | valid_wage > r(p99)

}

* Prepare info needed for dynamic y axis labels 
qui sum year
local min_year = 2011
local max_year = 2023

forval year = `min_year'/`max_year' { 

	twoway__histogram_gen valid_wage if year == `year' , ///
		bin(10) den gen(d_valid v2)

	qui sum d_valid
	gen max_d_valid_`year' = r(max) 
	
	drop d_valid v2

}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year labC4 sim_pred_wage demAge using ///
	"$dir_data/simulation_sample.dta", clear

* Select sample 
keep if labC4 == "EmployedOrSelfEmployed"
keep if inrange(demAge,16,65)

drop labC4

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum sim_pred_wage, d
	
	replace sim_pred_wage = . if ///
		sim_pred_wage < r(p1) | sim_pred_wage > r(p99)

}

append using "$dir_data/temp_valid_stats.dta"

qui sum year
local min_year = 2011
local max_year = 2023

forval year = `min_year'/`max_year' {
	
	* Prepare info needed for dynamic y axis labels 
	twoway__histogram_gen sim_pred_wage if year == `year', ///
		bin(5) den gen(d_sim v1)

	qui sum d_sim 
	gen max_d_sim = r(max)

	gen max_value = max_d_valid_`year' if max_d_valid_`year' > max_d_sim 
	replace max_value = max_d_sim if max_value == . 

	sum max_value 
	local max_y = 1.25*r(max)
	local steps = `max_y'/2

	* Plot all hours
	twoway (hist sim_pred_wage if year == `year', ///
		width(1) color(green%30)  legend(label(1  "SimPaths"))) ///
	(hist valid_wage if year == `year', width(1)  color(red%30) ///
		legend(label(2 "UKHLS"))) , ///
		title("Hourly Wage") ///
		subtitle("`year'") ///
		name(hourly_wages_`year'_all, replace) ///
		xtitle("GBP per hour", size(small)) ///
		ytitle("Density", size(small)) ///
		xlabel(,labsize(small)) ///
		ylabel(, labsize(small)) ///
		legend(size(small)) ///
		graphregion(color(white)) ///
		note("Notes: Statistics calculated on subsample of employed and self-employed individuals aged 16-65. Amounts in 2015 prices.""Top percentiles and bottom percentiles trimmed.", size(vsmall))
	
	graph export ///
	"$dir_output_files/wages/validation_${country}_wages_dist_`year'.png", ///
		replace width(2400) height(1350) 

	drop d_sim v1 max_d_sim max_value
	
}

graph drop _all


********************************************************************************
* 2.2 : Histograms by year - ages 16-65 by gender
********************************************************************************

* Females 
* Prepare validation data
use year demAge dwt labC4 valid_wage demMaleFlag demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Select sample
keep if labC4 == 1
keep if demMaleFlag == 0 
keep if inrange(demAge,16,65)

drop labC4 demMaleFlag 

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_wage, d
	
	replace valid_wage = . if ///
		valid_wage < r(p1) | valid_wage > r(p99)

}

* Prepare info needed for dynamic y axis labels 
qui sum year
local min_year = 2011
local max_year = 2023

forval year = `min_year'/`max_year' { 

	twoway__histogram_gen valid_wage if year == `year' , ///
		bin(10) den gen(d_valid v2)

	qui sum d_valid
	gen max_d_valid_`year' = r(max) 
	
	drop d_valid v2

}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year labC4 sim_pred_wage demMaleFlag demAge using ///
	"$dir_data/simulation_sample.dta", clear

* Select sample
keep if labC4 == "EmployedOrSelfEmployed"
keep if demMaleFlag == 0
keep if inrange(demAge,16,65)

drop labC4 demMaleFlag

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum sim_pred_wage, d
	
	replace sim_pred_wage = . if ///
		sim_pred_wage < r(p1) | sim_pred_wage > r(p99)

}

append using "$dir_data/temp_valid_stats.dta"

qui sum year
local min_year = 2011
local max_year = 2023

forval year = `min_year'/`max_year' {
	
	* Prepare info needed for dynamic y axis labels 
	twoway__histogram_gen sim_pred_wage if year == `year', ///
		bin(10) den gen(d_sim v1)

	qui sum d_sim 
	gen max_d_sim = r(max)

	gen max_value = max_d_valid_`year' if max_d_valid_`year' > max_d_sim 
	replace max_value = max_d_sim if max_value == . 

	sum max_value 
	local max_y = 1.25*r(max)
	local steps = `max_y'/2

	* Plot all hours
	twoway (hist sim_pred_wage if year == `year', ///
		width(1) color(green%30)  legend(label(1  "SimPaths"))) ///
	(hist valid_wage if year == `year', width(1) color(red%30) ///
		legend(label(2 "UKHLS"))) , ///
	title("Hourly Wage") ///
		subtitle("`year', females") ///
		name(hourly_wages_`year'_all, replace) ///
		xtitle("GBP per hour", size(small)) ///
		ytitle("Density", size(small)) ///
		xlabel(,labsize(small)) ///
		legend(size(small)) ///
		graphregion(color(white)) ///
		note("Notes: Statistics calculated on subsample of employed and self-employed individuals aged 16-65. Amounts in 2015 prices.""Top and bottom percentails trimmed.", size(vsmall))
	
	graph export ///
	"$dir_output_files/wages/validation_${country}_wages_dist_`year'_female.png", ///
		replace width(2400) height(1350) 

	drop d_sim v1 max_d_sim max_value
	
}


* Males 
* Prepare validation data
use year demAge dwt labC4 valid_wage demMaleFlag using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Select sample
keep if labC4 == 1
keep if demMaleFlag == 1 
drop if inrange(demAge,16,65)

drop labC4 demMaleFlag 

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_wage, d
	
	replace valid_wage = . if ///
		valid_wage < r(p1) | valid_wage > r(p99)

}

* Prepare info needed for dynamic y axis labels 
qui sum year
local min_year = 2011
local max_year = 2023

forval year = `min_year'/`max_year' { 

	twoway__histogram_gen valid_wage if year == `year' , ///
		bin(10) den gen(d_valid v2)

	qui sum d_valid
	gen max_d_valid_`year' = r(max) 
	
	drop d_valid v2

}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year labC4 sim_pred_wage demMaleFlag demAge using ///
	"$dir_data/simulation_sample.dta", clear

* Select sample
keep if labC4 == "EmployedOrSelfEmployed"
keep if demMaleFlag == 1
keep if inrange(demAge,16,65)

drop labC4 demMaleFlag

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum sim_pred_wage, d
	
	replace sim_pred_wage = . if ///
		sim_pred_wage < r(p1) | sim_pred_wage > r(p99)

}

append using "$dir_data/temp_valid_stats.dta"

qui sum year
local min_year = 2011
local max_year = 2023

forval year = `min_year'/`max_year' {
	
	* Prepare info needed for dynamic y axis labels 
	twoway__histogram_gen sim_pred_wage if year == `year', ///
		bin(10) den gen(d_sim v1)

	qui sum d_sim 
	gen max_d_sim = r(max)

	gen max_value = max_d_valid_`year' if max_d_valid_`year' > max_d_sim 
	replace max_value = max_d_sim if max_value == . 

	sum max_value 
	local max_y = 1.25*r(max)
	local steps = `max_y'/2

	* Plot all hours
	twoway (hist sim_pred_wage if year == `year', ///
		width(1) color(green%30)  legend(label(1  "SimPaths"))) ///
	(hist valid_wage if year == `year', width(1) color(red%30) ///
		legend(label(2 "UKHLS"))) , ///
	title("Hourly Wage") ///
		subtitle("`year', males") ///
		name(hourly_wages_`year'_all, replace) ///
		xtitle("GBP per hour", size(small)) ///
		ytitle("Density", size(small)) ///
		xlabel(,labsize(small)) ///
		legend(size(small)) ///
		graphregion(color(white)) ///
		note("Notes: Statistics calculated on subsample of employed and self-employed individuals aged 16-65. Amounts in 2015 prices.""Top and bottom percentiles trimmed.", size(vsmall))

	
	graph export ///
	"$dir_output_files/wages/validation_${country}_wages_dist_`year'_male.png", ///
		replace width(2400) height(1350) 

	drop d_sim v1 max_d_sim max_value
	
	
}


graph drop _all 
