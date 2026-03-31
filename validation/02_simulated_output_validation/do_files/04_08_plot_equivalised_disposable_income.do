********************************************************************************
* PROJECT:  		SimPaths UK 
* SECTION:			Validation
* OBJECT: 			Equivalised disposable income
* AUTHORS:			Ashley Burdett 
* LAST UPDATE:		Jan 2026
* COUNTRY: 			UK 
********************************************************************************
* NOTES: 			This do file plots simulated and UKHLS equivalised 
* 					disposable income, per benefit unit
********************************************************************************

********************************************************************************
* 1 : Mean values over time, benefit unit 
********************************************************************************

* Prepare validation data
use year idBu demAge dwt valid_yDispBuEquivYear using ///
	"$dir_data/ukhls_validation_sample.dta", clear

drop if demAge < 16

* Keep one observation per benefit unit
bysort year idBu: gen first_person = (_n == 1)
keep if first_person == 1	
	
* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_yDispBuEquivYear, d

	replace valid_yDispBuEquivYear = . if ///
		valid_yDispBuEquivYear < r(p1) | valid_yDispBuEquivYear > r(p99)

}

collapse (mean) valid_yDispBuEquivYear [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run idBu year demAge sim_yDispEquivYear using ///
	"$dir_data/simulation_sample.dta", clear

drop if demAge < 16

* Keep one observation per benefit unit
bysort run year idBu: gen first_person = (_n == 1)
keep if first_person == 1

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum sim_yDispEquivYear, d
	
	replace sim_yDispEquivYear = . if ///
		sim_yDispEquivYear < r(p1) | sim_yDispEquivYear > r(p99)

}

collapse (mean) sim_yDispEquivYear, by(run year)

collapse (mean) sim_yDispEquivYear ///
		 (sd) sim_yDispEquivYear_sd = sim_yDispEquivYear, by(year)
		 
foreach varname in sim_yDispEquivYear {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea sim_yDispEquivYear_high sim_yDispEquivYear_low year, ///
	sort color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_yDispBuEquivYear year, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	title("Benefit Unit Equivalised Disposable Income") ///
	subtitle("") ///
	xtitle("Year", size(small)) ///
	ytitle("GBP per year", size(small)) ///
	ylabel(,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: Equivalised disposable income computed by the modified OECD scale. Top and bottom percentiles trimmed. Amounts" "annual, in 2015 prices.", ///
	size(vsmall))

graph export ///
"$dir_output_files/income/equivalised_disposable_income/validation_${country}_equivalised_disposable_income_bu_ts.jpg", ///
	replace width(2400) height(1350) 
	
	
********************************************************************************
* 2 : Histograms 
********************************************************************************

********************************************************************************
* 2.1 : By year, benefit unit
********************************************************************************

* Prepare validation data
use year idBu demAge dwt valid_yDispBuEquivYear using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Sample selection 
drop if demAge < 16
	
* Keep one observation per benefit unit
bysort year idBu: gen first_person = (_n == 1)
keep if first_person == 1	
	
* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_yDispBuEquivYear, d
	
	replace valid_yDispBuEquivYear = . if ///
		valid_yDispBuEquivYear < r(p1) | ///
		valid_yDispBuEquivYear > r(p99)

}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year idBu sim_yDispEquivYear demAge using ///
	"$dir_data/simulation_sample.dta", clear

* Sample selection 
drop if demAge < 16	

* Keep one observation per benefit unit
bysort run year idBu: gen first_person = (_n == 1)
keep if first_person == 1

	
* Trim outliers
if "$trim_outliers" == "true" {
	
	sum sim_yDispEquivYear, d
	
	replace sim_yDispEquivYear = . if ///
		sim_yDispEquivYear < r(p1) | sim_yDispEquivYear > r(p99)

}

append using "$dir_data/temp_valid_stats.dta"

qui sum year
local min_year = 2011
local max_year = 2023 

forval year = `min_year'/`max_year' {
	
	twoway (hist sim_yDispEquivYear if year == `year', width(2000) ///
		color(green%30) legend(label(1 "SimPaths"))) ///
	(hist valid_yDispBuEquivYear if year == `year', ///
		 width(2000) color(red%30) legend(label(2 "UKHLS"))) , ///
		title("Benefit Unit Equivalised Disposable Income") ///
		subtitle("`year'") ///
		name(disp_inc_`year'_all, replace) ///
		ylabel(,labsize(small)) ///
		xlabel(,labsize(small)) ///		
		xtitle("GBP", size(small)) ///
		ytitle("Density", size(small)) ///
		legend(size(small)) ///
		graphregion(color(white)) ///
		note("Notes:  Amounts in GBP per year, 2015 prices. Top and bottom percentiles trimmed.", ///
		size(vsmall))
		
	graph export ///
	"$dir_output_files/income/equivalised_disposable_income/validation_${country}_equivalised_disposable_income_bu_dist_`year'.png", ///
		replace width(2560) height(1440) 
	
}

graph drop _all 	

********************************************************************************
* 2 : Histograms by year, and by category of weekly labour supply, ben unit 
********************************************************************************

* Prepare validation data
use year demAge dwt valid_yDispBuEquivYear valid_labHrsWorkEnumWeek using ///
	"$dir_data/ukhls_validation_sample.dta", clear		
	
keep if inrange(demAge,18,65)	
	
* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_yDispBuEquivYear, d
	
	replace valid_yDispBuEquivYear = . if ///
		valid_yDispBuEquivYear < r(p1) | valid_yDispBuEquivYear > r(p99)
		
}

* Prepare info needed for dynamic y axis labels 
qui sum year
local min_year = 2011
local max_year = 2023

forval year = `min_year'/`max_year' { 

	twoway__histogram_gen valid_yDispBuEquivYear if year == `year' , ///
		width(2000) den gen(d_valid v2)

	qui sum d_valid
	gen max_d_valid_`year' = r(max) 
	
	drop d_valid v2

	foreach ls in $ls_cat {
	
		twoway__histogram_gen valid_yDispBuEquivYear if ///
			year == `year' & valid_labHrsWorkEnumWeek == "`ls'", width(2000) ///
			den gen(d_valid v2)

		qui sum d_valid
		gen max_d_valid_`year'_`ls' = r(max) 
		
		drop d_valid v2	
	
	}
}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year demAge sim_yDispEquivYear sim_labHrsWorkEnumWeek using ///
	"$dir_data/simulation_sample.dta", clear

keep if inrange(demAge,18,65)	
	
* Trim outliers
if "$trim_outliers" == "true" {
	
	sum sim_yDispEquivYear, d
	
	replace sim_yDispEquivYear = . if ///
		sim_yDispEquivYear < r(p1) | sim_yDispEquivYear > r(p99)

}

keep if run == 1

append using "$dir_data/temp_valid_stats.dta"

qui sum year
local min_year = 2011
local max_year = 2023  

forval year = `min_year'/`max_year' {
	
    * Prepare info needed for dynamic y axis labels 
	twoway__histogram_gen sim_yDispEquivYear if year == `year', width(2000) ///
		den gen(d_sim v1)

	qui sum d_sim 
	gen max_d_sim = r(max)

	gen max_value = max_d_valid_`year' if max_d_valid_`year' > max_d_sim 
	replace max_value = max_d_sim if max_value == . 

	sum max_value 
	local max_y = 1.25*r(max)
	local steps = `max_y'/2
	
	* Plot all hours
	twoway (hist sim_yDispEquivYear if year == `year', width(2000) ///
		color(green%30) legend(label(1 "SimPaths"))) ///
	(hist valid_yDispBuEquivYear if year == `year', width(2000) color(red%30) ///
		legend(label(2 "UKHLS"))) , ///
		subtitle("ALL hours") ///
		xtitle("GBP", size(small)) ///
		ytitle("Density", size(small)) ///	
		name(eqdisp_inc_`year'_all, replace) ///
		xlabel(,labsize(vsmall) angle(forty_five)) ///
		ylabel(0(`steps')`max_y', labsize(vsmall)) ///
		legend(size(small)) ///		
		graphregion(color(white))		
		
	drop d_sim v1 max_d_sim max_value	
	
	foreach ls in $ls_cat {
	
		* Prepare info needed for dynamic y axis labels 
		twoway__histogram_gen sim_yDispEquivYear if year == `year' & ///
			sim_labHrsWorkEnumWeek == "`ls'", width(2000) den gen(d_sim v1)

		qui sum d_sim 
		gen max_d_sim = r(max)

		gen max_value = max_d_valid_`year'_`ls' if ///
			max_d_valid_`year'_`ls' > max_d_sim 
		replace max_value = max_d_sim if max_value == . 

		sum max_value 
		local max_y = 1.25*r(max)
		local steps = `max_y'/2		
	
		twoway (hist sim_yDispEquivYear if year == `year' &  ///
			sim_labHrsWorkEnumWeek == "`ls'", width(2000) ///
			color(green%30) legend(label(1 "SimPaths"))) ///
		(hist valid_yDispBuEquivYear if year == `year' & ///
			valid_labHrsWorkEnumWeek == "`ls'", width(2000) color(red%30) ///
			legend(label(2 "UKHLS"))) , ///
			subtitle("`ls' hours")  ///
			name(eqdisp_inc_`year'_`ls', replace) ///
			xtitle("GBP", size(small)) ///
			ytitle("Density", size(small)) ///
			xlabel(,labsize(vsmall) angle(forty_five)) ///
			ylabel(0(`steps')`max_y', labsize(vsmall)) ///
			legend(size(small)) ///
			graphregion(color(white)) 
			
		drop d_sim v1 max_d_sim max_value
		
	}
}

qui sum year
local min_year = 2011
local max_year = 2023  

forvalues year = `min_year'/`max_year' {
	
	grc1leg eqdisp_inc_`year'_all ///
		eqdisp_inc_`year'_ZERO ///
		eqdisp_inc_`year'_TEN , ///
		title("Equivalised Disposable Income") ///
		subtitle("`year'") ///
		legendfrom(eqdisp_inc_`year'_all) rows(1) ///
		graphregion(color(white)) ///		
		note("Notes: Distribution of benefit unit equivalised disposable income. Individual level data plotted 18-65 year olds included in sample. Amounts in" "GBP per year, 2015 prices. Top and bottom percentiles trimmed.", ///
		size(vsmall)) 
		
	graph export ///
 "$dir_output_files/income/equivalised_disposable_income/validation_${country}_equivalised_disposable_inc_dist_`year'_1.png", ///
		replace width(2560) height(1440) 

		
	grc1leg ///
		eqdisp_inc_`year'_TWENTY ///
		eqdisp_inc_`year'_THIRTY ///
		eqdisp_inc_`year'_THIRTY_EIGHT ///
		eqdisp_inc_`year'_FORTY_FIVE ///		
		eqdisp_inc_`year'_FIFTY_FIVE, ///
		title("Equivalised Disposable Income") ///
		subtitle("`year'") ///
		legendfrom(eqdisp_inc_`year'_TWENTY) rows(2) ///
		graphregion(color(white)) ///		
		note("Notes: Distribution of benefit unit equivalised disposable income. Individual level data plotted 18-65 year olds included in sample. Amounts in" "GBP per year, 2015 prices. Top and bottom percentiles trimmed.", ///
		size(vsmall)) 
		
	graph export ///
 "$dir_output_files/income/equivalised_disposable_income/validation_${country}_equivalised_disposable_inc_dist_`year'_2.png", ///
		replace width(2560) height(1440) 
		
}

graph drop _all 

