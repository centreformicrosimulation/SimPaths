********************************************************************************
* PROJECT:  		SimPaths 
* SECTION:			Validation
* OBJECT: 			Hourly wages
* AUTHORS:			Ashley Burdett 
* LAST UPDATE:		9/25
* COUNTRY: 			UK 

* NOTES: 			This master do file organises do files used for validating 
* 					SimPaths model using UKHLS data. 
********************************************************************************

********************************************************************************
* 1 : Mean values over time
********************************************************************************

********************************************************************************
* 1.1 : Mean values over time - 18-65
********************************************************************************

* Prepare validation data
use year dwt les_c4 valid_wage_hour lhw_flag using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Select sample 
keep if les_c4 == 1

drop if lhw_flag == 1


* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_wage_hour, d
	
	replace valid_wage_hour = . if ///
		valid_wage_hour < r(p1) | valid_wage_hour > r(p99)

}

* Drop very low wages 
drop if valid_wage_hour < 3

* Compute means
collapse (mean) valid_wage_hour [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year les_c4 potential_earnings_hourly using ///
	"$dir_data/simulated_data.dta", clear

* Select sample
keep if les_c4 == "EmployedOrSelfEmployed"

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum potential_earnings_hourly, d

	replace potential_earnings_hourly = . if ///
		potential_earnings_hourly < r(p1) | potential_earnings_hourly > r(p99)

}

* Compute means and sd
collapse (mean) potential_earnings_hourly, by(run year)
collapse (mean) potential_earnings_hourly ///
		 (sd) potential_earnings_hourly_sd = potential_earnings_hourly ///
		 , by(year)
		 
* Approx 95% confidence interval		 
foreach varname in potential_earnings_hourly {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

* Combine datasets 
merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea potential_earnings_hourly_high ///
	potential_earnings_hourly_low year, ///
	sort color(green%20) legend(label(1 "Simulated"))) ///
(line valid_wage_hour year, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	title("Hourly Wage") ///
	subtitle("Ages 18-65") ///
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
"$dir_output_files/wages/validation_${country}_wages_ts_${min_age}_${max_age}_both.jpg", ///
	replace width(2560) height(1440) quality(100)
	
	
********************************************************************************
* 1.2 : Mean values over time - 18-65, by gender
********************************************************************************

* Prepare validation data
use year dwt les_c4 valid_wage_hour dgn using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* select sample 
keep if les_c4 == 1

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_wage_hour, d
	
	replace valid_wage_hour = . if ///
		valid_wage_hour < r(p1) | valid_wage_hour > r(p99)

}

* Drop very low wages 
drop if valid_wage_hour < 3

* Compute mean
collapse (mean) valid_wage_hour [aw = dwt], by(year dgn)

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year les_c4 potential_earnings_hourly dgn using ///
	"$dir_data/simulated_data.dta", clear

* Select sample
keep if les_c4 == "EmployedOrSelfEmployed"

gen dgn2 = 0 if dgn == "Female"
replace dgn2 = 1 if dgn == "Male"

drop dgn
rename dgn2 dgn 

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum potential_earnings_hourly , d
	
	replace potential_earnings_hourly = . if ///
		potential_earnings_hourly < r(p1) | potential_earnings_hourly > r(p99)
		
}

collapse (mean) potential_earnings_hourly, by(run year dgn)
collapse (mean) potential_earnings_hourly ///
		 (sd) potential_earnings_hourly_sd = potential_earnings_hourly ///
		 , by(year dgn)
		 
foreach varname in potential_earnings_hourly {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year dgn using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure  
twoway (rarea potential_earnings_hourly_high ///
	potential_earnings_hourly_low year if dgn == 0, ///
	sort color(green%20) legend(label(1 "Simulated"))) ///
(line valid_wage_hour year if dgn == 0, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	subtitle("Females") ///
	name(wages_female, replace) ///
	xtitle("Year", size(small)) ///
	ytitle("GBP per hour", size(small)) ///
	ylabel(,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) 
	
twoway (rarea potential_earnings_hourly_high ///
	potential_earnings_hourly_low year if dgn == 1, ///
	sort color(green%20) legend(label(1 "Simulated"))) ///
(line valid_wage_hour year if dgn == 1, sort color(green) ///
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
	subtitle("Ages 18-65") ///
	legendfrom(wages_female) rows(1) ///
	graphregion(color(white)) ///
	note("Notes: Statistics calculated on sample of employed anf self-employed individuals. Amounts in 2015 prices. Top and bottom percentiles trimmed.", ///
	size(vsmall))
	
* Save figure
graph export ///
"$dir_output_files/wages/validation_${country}_wages_ts_${min_age}_${max_age}_gender.jpg", ///
	replace width(2560) height(1440) quality(100)
	

********************************************************************************
* 2 : Histograms by year
********************************************************************************

********************************************************************************
* 2.1 : Histograms by year - ages 18-65
********************************************************************************

* Prepare validation data
use year dwt les_c4 valid_wage_hour using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Select sample
keep if les_c4 == 1

drop les_c4

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_wage_hour, d
	
	replace valid_wage_hour = . if ///
		valid_wage_hour < r(p1) | valid_wage_hour > r(p99)

}

* Drop very low wages 
drop if valid_wage_hour < 3

* Prepare info needed for dynamic y axis labels 
qui sum year
local min_year = 2011
local max_year = r(max)  

forval year = `min_year'/`max_year' { 

	twoway__histogram_gen valid_wage_hour if year == `year' , ///
		bin(60) den gen(d_valid v2)

	qui sum d_valid
	gen max_d_valid_`year' = r(max) 
	
	drop d_valid v2

}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year les_c4 potential_earnings_hourly using ///
	"$dir_data/simulated_data.dta", clear

* Keep only employed individuals
keep if les_c4 == "EmployedOrSelfEmployed"

drop les_c4

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum potential_earnings_hourly, d
	
	replace potential_earnings_hourly = . if ///
		potential_earnings_hourly < r(p1) | potential_earnings_hourly > r(p99)

}

append using "$dir_data/temp_valid_stats.dta"

qui sum year
local min_year = 2011
local max_year = r(max)  

forval year = `min_year'/`max_year' {
	
	* Prepare info needed for dynamic y axis labels 
	twoway__histogram_gen potential_earnings_hourly if year == `year', ///
		bin(60) den gen(d_sim v1)

	qui sum d_sim 
	gen max_d_sim = r(max)

	gen max_value = max_d_valid_`year' if max_d_valid_`year' > max_d_sim 
	replace max_value = max_d_sim if max_value == . 

	sum max_value 
	local max_y = 1.25*r(max)
	local steps = `max_y'/2

	* Plot all hours
	twoway (hist potential_earnings_hourly if year == `year', ///
		width(0.5) color(green%30)  legend(label(1 "Simulated"))) ///
	(hist valid_wage_hour if year == `year', width(0.5)  color(red%30) ///
		legend(label(2 "UKHLS"))) , ///
		title("Hourly Wage") ///
		subtitle("`year'") ///
		name(hourly_wages_`year'_all, replace) ///
		xtitle("GBP per hour", size(small)) ///
		ytitle("Density", size(small)) ///
		xlabel(,labsize(small)) ///
		ylabel(0(`steps')`max_y', labsize(small)) ///
		legend(size(small)) ///
		graphregion(color(white)) ///
		note("Notes: Statistics calculated on subsample of employed and self-employed individuals aged 18-65. Amounts in 2015 prices.""Top and bottom percentiles trimmed.", size(vsmall))
	
	graph export ///
	"$dir_output_files/wages/validation_${country}_wages_dist_`year'.png", ///
		replace width(2400) height(1350) 

	drop d_sim v1 max_d_sim max_value
	
}

graph drop _all


********************************************************************************
* 2.2 : Histograms by year - ages 18-65 by gender
********************************************************************************

* Females 
* Prepare validation data
use year dwt les_c4 valid_wage_hour dgn hours using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Select sample
keep if les_c4 == 1
keep if dgn == 0 

drop les_c4 dgn 

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_wage_hour, d
	
	replace valid_wage_hour = . if ///
		valid_wage_hour < r(p1) | valid_wage_hour > r(p99)

}

* Drop very low wages 
drop if valid_wage_hour < 3

* Remove those with very high hours of work 
//drop if hours > $max_hours

* Prepare info needed for dynamic y axis labels 
qui sum year
local min_year = 2011
local max_year = r(max)  

forval year = `min_year'/`max_year' { 

	twoway__histogram_gen valid_wage_hour if year == `year' , ///
		bin(60) den gen(d_valid v2)

	qui sum d_valid
	gen max_d_valid_`year' = r(max) 
	
	drop d_valid v2

}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year les_c4 potential_earnings_hourly dgn using ///
	"$dir_data/simulated_data.dta", clear

* Select sample
keep if les_c4 == "EmployedOrSelfEmployed"
keep if dgn == "Female"
drop les_c4 dgn

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum potential_earnings_hourly, d
	
	replace potential_earnings_hourly = . if ///
		potential_earnings_hourly < r(p1) | potential_earnings_hourly > r(p99)

}

append using "$dir_data/temp_valid_stats.dta"

qui sum year
local min_year = 2011
local max_year = r(max)  

forval year = `min_year'/`max_year' {
	
	* Prepare info needed for dynamic y axis labels 
	twoway__histogram_gen potential_earnings_hourly if year == `year', ///
		bin(60) den gen(d_sim v1)

	qui sum d_sim 
	gen max_d_sim = r(max)

	gen max_value = max_d_valid_`year' if max_d_valid_`year' > max_d_sim 
	replace max_value = max_d_sim if max_value == . 

	sum max_value 
	local max_y = 1.25*r(max)
	local steps = `max_y'/2

	* Plot all hours
	twoway (hist potential_earnings_hourly if year == `year', ///
		width(0.5) color(green%30)  legend(label(1 "Simulated"))) ///
	(hist valid_wage_hour if year == `year', width(0.5) color(red%30) ///
		legend(label(2 "UKHLS"))) , ///
	title("Hourly Wage") ///
		subtitle("`year', females") ///
		name(hourly_wages_`year'_all, replace) ///
		xtitle("GBP per hour", size(small)) ///
		ytitle("Density", size(small)) ///
		xlabel(,labsize(small)) ///
		ylabel(0(`steps')`max_y', labsize(small)) ///
		legend(size(small)) ///
		graphregion(color(white)) ///
		note("Notes: Statistics calculated on subsample of employed and self-employed individuals aged 18-65. Amounts in 2015 prices.""Top and bottom percentiles trimmed.", size(vsmall))
	
	graph export ///
	"$dir_output_files/wages/validation_${country}_wages_dist_`year'_female.png", ///
		replace width(2400) height(1350) 

	drop d_sim v1 max_d_sim max_value
	
}


* Males 
* Prepare validation data
use year dwt les_c4 valid_wage_hour hours dgn using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Select sample
keep if les_c4 == 1
keep if dgn == 1 

drop les_c4 dgn 

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_wage_hour, d
	
	replace valid_wage_hour = . if ///
		valid_wage_hour < r(p1) | valid_wage_hour > r(p99)

}

* Drop very low wages 
drop if valid_wage_hour < 3

* Remove those with very high hours of work 
//drop if hours > $max_hours


* Prepare info needed for dynamic y axis labels 
qui sum year
local min_year = 2011
local max_year = r(max)  

forval year = `min_year'/`max_year' { 

	twoway__histogram_gen valid_wage_hour if year == `year' , ///
		bin(60) den gen(d_valid v2)

	qui sum d_valid
	gen max_d_valid_`year' = r(max) 
	
	drop d_valid v2

}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year les_c4 potential_earnings_hourly dgn using ///
	"$dir_data/simulated_data.dta", clear

* Keep only employed individuals
keep if les_c4 == "EmployedOrSelfEmployed"
keep if dgn == "Male"
drop les_c4 dgn

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum potential_earnings_hourly, d
	
	replace potential_earnings_hourly = . if ///
		potential_earnings_hourly < r(p1) | potential_earnings_hourly > r(p99)

}

append using "$dir_data/temp_valid_stats.dta"

qui sum year
local min_year = 2011
local max_year = r(max)  

forval year = `min_year'/`max_year' {
	
	* Prepare info needed for dynamic y axis labels 
	twoway__histogram_gen potential_earnings_hourly if year == `year', ///
		bin(60) den gen(d_sim v1)

	qui sum d_sim 
	gen max_d_sim = r(max)

	gen max_value = max_d_valid_`year' if max_d_valid_`year' > max_d_sim 
	replace max_value = max_d_sim if max_value == . 

	sum max_value 
	local max_y = 1.25*r(max)
	local steps = `max_y'/2

	* Plot all hours
	twoway (hist potential_earnings_hourly if year == `year', ///
		width(0.5) color(green%30)  legend(label(1 "Simulated"))) ///
	(hist valid_wage_hour if year == `year', width(0.5) color(red%30) ///
		legend(label(2 "UKHLS"))) , ///
	title("Hourly Wage") ///
		subtitle("`year', males") ///
		name(hourly_wages_`year'_all, replace) ///
		xtitle("GBP per hour", size(small)) ///
		ytitle("Density", size(small)) ///
		xlabel(,labsize(small)) ///
		ylabel(0(`steps')`max_y', labsize(small)) ///
		legend(size(small)) ///
		graphregion(color(white)) ///
		note("Notes: Statistics calculated on subsample of employed and self-employed individuals aged 18-65. Amounts in 2015 prices.""Top and bottom percentiles trimmed.", size(vsmall))

	
	graph export ///
	"$dir_output_files/wages/validation_${country}_wages_dist_`year'_male.png", ///
		replace width(2400) height(1350) 

	drop d_sim v1 max_d_sim max_value
	
}


graph drop _all 
