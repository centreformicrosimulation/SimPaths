********************************************************************************
* PROJECT:  		SimPaths UK 
* SECTION:			Validation
* OBJECT: 			Capital income
* AUTHORS:			Ashley Burdett 
* LAST UPDATE:		9/25 (AB)
* COUNTRY: 			UK 
********************************************************************************
* NOTES: 			This do file plots simulated and UKHLS capital income, 
*					per benefit unit

********************************************************************************

********************************************************************************
* 1 : Time series
********************************************************************************

********************************************************************************
* 1.1 : Mean through time, benefit unit
********************************************************************************

* Prepare validation data
use year idPers idBu demAge dwt valid_yCapitalBuLevelYear using /// 
	"$dir_data/ukhls_validation_sample.dta", clear

keep if demAge >= 16

* Keep one observatioon per benefit unit
bysort year idBu: gen first_person = (_n == 1)
keep if first_person == 1

* Trim outliers
/*
if "$trim_outliers" == "true" {
	
	sum valid_yCapitalBuLevelYear, d
	
	replace valid_yCapitalBuLevelYear = . if ///
		valid_yCapitalBuLevelYear < r(p1) | ///
		valid_yCapitalBuLevelYear > r(p99)
		
}
*/

collapse (mean) valid_yCapitalBuLevelYear [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year idPers idBu demAge sim_yCapitalBuLevelYear using ///
	"$dir_data/simulation_sample.dta", clear

keep if demAge >= 16

* Keep one observatioon per benefit unit
bysort run year idBu: gen first_person = (_n == 1)
keep if first_person == 1

* Trim outliers
/*
if "$trim_outliers" == "true" {
	
	sum sim_yCapitalBuLevelYear, d
	
	replace sim_yCapitalBuLevelYear = . if ///
		sim_yCapitalBuLevelYear < r(p1) | sim_yCapitalBuLevelYear > r(p99)
		
}
*/

collapse (mean) sim_yCapitalBuLevelYear, by(run year)

collapse (mean) sim_yCapitalBuLevelYear ///
		 (sd) sim_yCapitalBuLevelYear_sd = sim_yCapitalBuLevelYear ///
		 , by(year)
		 
foreach varname in sim_yCapitalBuLevelYear {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd
	
}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea sim_yCapitalBuLevelYear_high sim_yCapitalBuLevelYear_low year, ///
	sort color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_yCapitalBuLevelYear year, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	title("Benefit Unit Capital income") ///
	subtitle("") ///
	xtitle("Year", size(small)) ///
	ytitle("GBP per year", size(small)) ///
	ylabel(,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: Series represents average benefit unit capital income per year. Amounts in 2015 prices.", ///
	size(vsmall))

* Save figure
graph export ///
"$dir_output_files/income/capital_income/validation_${country}_capital_income_bu_ts.jpg", ///
	replace width(2400) height(1350) quality(100)

	
********************************************************************************
* 1.2 : Share with no capital income, benefit unit 
********************************************************************************

* Share with no capital income 
* Prepare validation data
use year idPers idBu demAge dwt valid_yCapitalBuLevelYear using /// 
	"$dir_data/ukhls_validation_sample.dta", clear

keep if demAge >= 16

* Keep one observatioon per benefit unit
bysort year idBu: gen first_person = (_n == 1)
keep if first_person == 1
	
/*	
* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_yCapitalBuLevelYear, d
	
	replace valid_yCapitalBuLevelYear = . if ///
		valid_yCapitalBuLevelYear < r(p1) | ///
		valid_yCapitalBuLevelYear > r(p99)
		
}
*/

gen valid_no_capital = (valid_yCapitalBuLevelYear == 0)

collapse (mean) valid_no_capital [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year idPers idBu demAge sim_yCapitalBuLevelYear using ///
	"$dir_data/simulation_sample.dta", clear

keep if demAge >= 16

* Keep one observatioon per benefit unit
bysort run year idBu: gen first_person = (_n == 1)
keep if first_person == 1

/*	
* Trim outliers
if "$trim_outliers" == "true" {
	
	sum sim_yCapitalBuLevelYear, d
	
	replace sim_yCapitalBuLevelYear = . if ///
		sim_yCapitalBuLevelYear < r(p1) | sim_yCapitalBuLevelYear > r(p99)
		
}
*/

gen sim_no_capital = (sim_yCapitalBuLevelYear == 0)

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
	color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_no_capital year, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	title("No Capital Income") ///
	subtitle("") ///
	xtitle("Year", size(small)) ///
	ytitle("Share of benefit units", size(small)) ///
	ylabel(,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: ", ///
	size(vsmall))

* Save figure
graph export ///
"$dir_output_files/income/capital_income/validation_${country}_no_capital_income_bu_ts.jpg", ///
	replace width(2400) height(1350) quality(100)
	

********************************************************************************
* 2 : Histograms 
********************************************************************************


********************************************************************************
* 2.1 : Benefit unit by year, 
********************************************************************************

* Prepare validation data
use year idBu demAge dwt valid_yCapitalBuLevelYear using ///
	"$dir_data/ukhls_validation_sample.dta", clear

drop if demAge < 16

* Keep one observation per benefit unit
bysort year idBu: gen first_person = (_n == 1)
keep if first_person == 1		

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_yCapitalBuLevelYear, d
	
	replace valid_yCapitalBuLevelYear = . if ///
		valid_yCapitalBuLevelYear < r(p1) | ///
		valid_yCapitalBuLevelYear > r(p99)

}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year idBu demAge sim_yCapitalBuLevelYear using ///
	"$dir_data/simulation_sample.dta", clear

drop if demAge < 16

* Keep one observation per benefit unit
bysort run year idBu: gen first_person = (_n == 1)
keep if first_person == 1
	
* Trim outliers
if "$trim_outliers" == "true" {
	
	sum sim_yCapitalBuLevelYear, d
	
	replace sim_yCapitalBuLevelYear = . if ///
		sim_yCapitalBuLevelYear < r(p1) | sim_yCapitalBuLevelYear > r(p99)

}

append using "$dir_data/temp_valid_stats.dta"

qui sum year
local min_year = 2011
local max_year = 2023

forval year = `min_year'/`max_year' {
	
	twoway (hist sim_yCapitalBuLevelYear if year == `year', width(250) ///
		color(green%30) legend(label(1 "SimPaths"))) ///
	(hist valid_yCapitalBuLevelYear if year == `year', ///
		 width(250) color(red%30) legend(label(2 "UKHLS"))) , ///
		title("Benefit Unit Capital Income") ///
		subtitle("`year'") ///
		name(capital_inc_`year'_all, replace) ///
		ylabel(,labsize(small)) ///
		xlabel(,labsize(small)) ///		
		xtitle("GBP", size(small)) ///
		ytitle("Density", size(small)) ///
		legend(size(small)) ///
		graphregion(color(white)) ///
		note("Notes: Amounts in GBP per year, 2015 prices. Top and bottom percentiles trimmed.", ///
		size(vsmall))
		
	graph export ///
	"$dir_output_files/income/capital_income/validation_${country}_capital_income_bu_dist_`year'.png", ///
		replace width(2560) height(1440) 
	
}

********************************************************************************
* 2.2 : Positive amounts only, benefit unit
********************************************************************************
	
* Prepare validation data
use year idBu demAge dwt valid_yCapitalBuLevelYear using ///
	"$dir_data/ukhls_validation_sample.dta", clear

keep if demAge >= 16

* Keep one observatioon per benefit unit
bysort year idBu: gen first_person = (_n == 1)
keep if first_person == 1

* Trim outliers

if "$trim_outliers" == "true" {
	
	sum valid_yCapitalBuLevelYear, d
	
	replace valid_yCapitalBuLevelYear = . if ///
		valid_yCapitalBuLevelYear < r(p1) | ///
		valid_yCapitalBuLevelYear > r(p99)

}


drop if valid_yCapitalBuLevelYear == 0 

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year idBu demAge sim_yCapitalBuLevelYear using ///
	"$dir_data/simulation_sample.dta", clear

keep if inrange(demAge,18,65)	
	
* Keep one observatioon per benefit unit
bysort run year idBu: gen first_person = (_n == 1)
keep if first_person == 1	
	
* Trim outliers

if "$trim_outliers" == "true" {
	
	sum sim_yCapitalBuLevelYear, d
	
	replace sim_yCapitalBuLevelYear = . if ///
		sim_yCapitalBuLevelYear < r(p1) | sim_yCapitalBuLevelYear > r(p99)

}

drop if sim_yCapitalBuLevelYear == 0 

append using "$dir_data/temp_valid_stats.dta"

qui sum year
local min_year = 2011
local max_year = 2023  

forval year = `min_year'/`max_year' {
	
	twoway (hist sim_yCapitalBuLevelYear if year == `year', ///
		width(500) color(green%30) legend(label(1 "SimPaths"))) ///
	(hist valid_yCapitalBuLevelYear if year == `year',  width(500) ///
		color(red%30) legend(label(2 "UKHLS"))) , ///
		title("Benefit Unit Capital Income") ///
		subtitle("Positive amounts, `year'") ///
		name(capital_inc_`year'_all, replace) ///
		ylabel(,labsize(small)) ///
		xlabel(,labsize(small)) ///		
		xtitle("GBP", size(small)) ///
		ytitle("Density", size(small)) ///
		legend(size(small)) ///
		graphregion(color(white)) ///
		note("Notes: Amounts in GBP per year, 2015 prices. Top and bottom percentailes trimmed. ", ///
		size(vsmall))
		
	graph export ///
"$dir_output_files/income/capital_income/validation_${country}_positive_capital_income_bu_dist_`year'.png", ///
		replace width(2560) height(1440) 
	
}
	
graph drop _all 	
