********************************************************************************
* PROJECT:  		SimPaths UK 
* SECTION:			Validation
* OBJECT: 			Pension income
* AUTHORS:			Ashley Burdett 
* LAST UPDATE:		9/25 (AB)
* COUNTRY: 			UK 

* NOTES: 			This do file plots simulated and UKHLS private penson 
*					income, per benefit unit

********************************************************************************

********************************************************************************
* 1 : Time series
********************************************************************************

********************************************************************************
* 1.1 : Mean through time, benefit unit 
********************************************************************************

* Prepare validation data
use year idBu demAge dwt valid_yPensBuGrossLevelYear using /// 
	"$dir_data/ukhls_validation_sample.dta", clear

keep if demAge < 65

* Keep one observation per benefit unit
bysort year idBu: gen first_person = (_n == 1)
keep if first_person == 1

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_yPensBuGrossLevelYear, d
	
	replace valid_yPensBuGrossLevelYear = . if ///
		valid_yPensBuGrossLevelYear < r(p1) | ///
		valid_yPensBuGrossLevelYear > r(p99)
		
}

collapse (mean) valid_yPensBuGrossLevelYear [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year idBu sim_yPensBuGrossLevelYear demAge using ///
	"$dir_data/simulation_sample.dta", clear

* Sample selection 
drop if demAge < 65

* Keep one observatioon per benefit unit
bysort run year idBu: gen first_person = (_n == 1)
keep if first_person == 1

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum sim_yPensBuGrossLevelYear, d
	
	replace sim_yPensBuGrossLevelYear = . if ///
		sim_yPensBuGrossLevelYear < r(p1) | sim_yPensBuGrossLevelYear > r(p99)
		
}

collapse (mean) sim_yPensBuGrossLevelYear, by(run year)

collapse (mean) sim_yPensBuGrossLevelYear ///
		 (sd) sim_yPensBuGrossLevelYear_sd = sim_yPensBuGrossLevelYear ///
		 , by(year)
		 
foreach varname in sim_yPensBuGrossLevelYear {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd
	
}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea sim_yPensBuGrossLevelYear_high sim_yPensBuGrossLevelYear_low ///
	year, sort color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_yPensBuGrossLevelYear year, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	title("Benefit Unit Private Pension Income") ///
	subtitle("") ///
	xtitle("Year", size(small)) ///
	ytitle("GBP per year", size(small)) ///
	ylabel(,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: Series represents average benefit unit private pension income. Amounts in 2015 prices. Top and bottom" "percentiles trimmed. Those 65+ maintained in sample.", ///
	size(vsmall))

* Save figure
graph export ///
"$dir_output_files/income/pension_income/validation_${country}_pension_income_bu_ts_65plus.jpg", ///
	replace width(2400) height(1350) quality(100)

	
********************************************************************************
* 1.2 : Share with no pension income, benefit unit 
********************************************************************************
 
* Prepare validation data
use year idBu demAge dwt valid_yPensBuGrossLevelYear demAge using /// 
	"$dir_data/ukhls_validation_sample.dta", clear

* Sample selection 
drop if demAge < 65	

* Keep one observation per benefit unit
bysort year idBu: gen first_person = (_n == 1)
keep if first_person == 1
	
* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_yPensBuGrossLevelYear, d
	
	replace valid_yPensBuGrossLevelYear = . if ///
		valid_yPensBuGrossLevelYear < r(p1) | ///
		valid_yPensBuGrossLevelYear > r(p99)
		
}

gen valid_no_pension = (valid_yPensBuGrossLevelYear == 0)

collapse (mean) valid_no_pension [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year idBu sim_yPensBuGrossLevelYear demAge using ///
	"$dir_data/simulation_sample.dta", clear

* Sample selection 
drop if demAge < 65

* Keep one observatioon per benefit unit
bysort run year idBu: gen first_person = (_n == 1)
keep if first_person == 1

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum sim_yPensBuGrossLevelYear, d
	
	replace sim_yPensBuGrossLevelYear = . if ///
		sim_yPensBuGrossLevelYear < r(p1) | sim_yPensBuGrossLevelYear > r(p99)
		
}

gen sim_no_pension = (sim_yPensBuGrossLevelYear == 0)

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
	color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_no_pension year, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	title("No Private Pension Income") ///
	subtitle("") ///
	xtitle("Year", size(small)) ///
	ytitle("Share of benefit units", size(small)) ///
	ylabel(,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: Share of benefit unit units with individual 65+ with no private pension income. Top and bottom percentiles trimmed.", ///
	size(vsmall))

* Save figure
graph export ///
"$dir_output_files/income/pension_income/validation_${country}_no_pension_income_bu_ts_65plus.jpg", ///
	replace width(2400) height(1350) quality(100)
	

********************************************************************************
* 2 : Histograms 
********************************************************************************

********************************************************************************
* 2.1 : 65+, by year, benefit unit
********************************************************************************

* Prepare validation data
use year idBu demAge dwt valid_yPensBuGrossLevelYear using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Sample selection 
drop if demAge < 65
	
* Keep one observation per benefit unit
bysort year idBu: gen first_person = (_n == 1)
keep if first_person == 1	
	
* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_yPensBuGrossLevelYear, d
	
	replace valid_yPensBuGrossLevelYear = . if ///
		valid_yPensBuGrossLevelYear < r(p1) | ///
		valid_yPensBuGrossLevelYear > r(p99)

}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year idBu sim_yPensBuGrossLevelYear demAge using ///
	"$dir_data/simulation_sample.dta", clear

* Sample selection 
drop if demAge < 65	

* Keep one observation per benefit unit
bysort run year idBu: gen first_person = (_n == 1)
keep if first_person == 1

	
* Trim outliers
if "$trim_outliers" == "true" {
	
	sum sim_yPensBuGrossLevelYear, d
	
	replace sim_yPensBuGrossLevelYear = . if ///
		sim_yPensBuGrossLevelYear < r(p1) | sim_yPensBuGrossLevelYear > r(p99)

}

append using "$dir_data/temp_valid_stats.dta"

qui sum year
local min_year = 2011
local max_year = 2023 

forval year = `min_year'/`max_year' {
	
	twoway (hist sim_yPensBuGrossLevelYear if year == `year', width(1000) ///
		color(green%30) legend(label(1 "SimPaths"))) ///
	(hist valid_yPensBuGrossLevelYear if year == `year', ///
		 width(1000) color(red%30) legend(label(2 "UKHLS"))) , ///
		title("Benefit Unit Private Pension Income") ///
		subtitle("`year'") ///
		name(capital_inc_`year'_all, replace) ///
		ylabel(,labsize(small)) ///
		xlabel(,labsize(small)) ///		
		xtitle("GBP", size(small)) ///
		ytitle("Density", size(small)) ///
		legend(size(small)) ///
		graphregion(color(white)) ///
		note("Notes: Sample includes benefit units with individuals age 65+. Amounts in GBP per year, 2015 prices. Top and bottom" "percentiles trimmed.", ///
		size(vsmall))
		
	graph export ///
	"$dir_output_files/income/pension_income/validation_${country}_pension_income_bu_dist_`year'.png", ///
		replace width(2560) height(1440) 
	
}
	

********************************************************************************
* 2.2 : Ages 65+, positive amounts only, benefit unit
********************************************************************************
	
* Prepare validation data
use year idBu demAge dwt valid_yPensBuGrossLevelYear using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Select sample 
drop if demAge < 65	
	
* Keep one observation per benefit unit
bysort year idBu: gen first_person = (_n == 1)
keep if first_person == 1	
	
* Trim outliers

if "$trim_outliers" == "true" {
	
	sum valid_yPensBuGrossLevelYear, d
	
	replace valid_yPensBuGrossLevelYear = . if ///
		valid_yPensBuGrossLevelYear < r(p1) | ///
		valid_yPensBuGrossLevelYear > r(p99)

}
*/

drop if valid_yPensBuGrossLevelYear == 0 

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year idBu sim_yPensBuGrossLevelYear demAge using ///
	"$dir_data/simulation_sample.dta", clear

* Select sample 
drop if demAge < 65	

* Keep one observation per benefit unit
bysort run year idBu: gen first_person = (_n == 1)
keep if first_person == 1

* Trim outliers

if "$trim_outliers" == "true" {
	
	sum sim_yPensBuGrossLevelYear, d
	
	replace sim_yPensBuGrossLevelYear = . if ///
		sim_yPensBuGrossLevelYear < r(p1) | sim_yPensBuGrossLevelYear > r(p99)

}
*/

drop if sim_yPensBuGrossLevelYear == 0 

append using "$dir_data/temp_valid_stats.dta"

qui sum year
local min_year = 2011
local max_year = 2023  

forval year = `min_year'/`max_year' {
	
	twoway (hist sim_yPensBuGrossLevelYear if year == `year', ///
		width(1000) color(green%30) legend(label(1 "SimPaths"))) ///
	(hist valid_yPensBuGrossLevelYear if year == `year',  width(1000) ///
		color(red%30) legend(label(2 "UKHLS"))) , ///
		title("Benefit Unit Private Pension Income") ///
		subtitle("Positive amounts, `year'") ///
		name(capital_inc_`year'_all, replace) ///
		ylabel(,labsize(small)) ///
		xlabel(,labsize(small)) ///		
		xtitle("GBP", size(small)) ///
		ytitle("Density", size(small)) ///
		legend(size(small)) ///
		graphregion(color(white)) ///
		note("Notes: Sample includes benefit units with individuals age 65+. Amounts in GBP per year, 2015 prices. Top and bottom" "percentiles trimmed.", ///
		size(vsmall))
		
	graph export ///
"$dir_output_files/income/pension_income/validation_${country}_positive_pension_income_bu_dist_`year'.png", ///
		replace width(2560) height(1440) 
	
}
	
graph drop _all 	
