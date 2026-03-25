********************************************************************************
* PROJECT:  		SimPath UK 
* SECTION:			Validation
* OBJECT: 			Disposable income
* AUTHORS:			Ashley Burdett 
* LAST UPDATE:		Jan 2026
* COUNTRY: 			UK 
********************************************************************************
* NOTES: 			This do file plots simulated and UKHLS disposable income, 
* 					per benefit unit. Individual level data plotted. 
********************************************************************************

********************************************************************************
* 1 : Mean values over time
********************************************************************************

********************************************************************************
* 1.1 : Mean values over time, benefit unit 
********************************************************************************

* Prepare validation data
use year idBu demAge dwt valid_yDispBuLevelYear using ///
	"$dir_data/ukhls_validation_sample.dta", clear

drop if demAge < 16

* Keep one observation per benefit unit
bysort year idBu: gen first_person = (_n == 1)
keep if first_person == 1	

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_yDispBuLevelYear, d
	
	replace valid_yDispBuLevelYear = . if ///
		valid_yDispBuLevelYear < r(p1) | valid_yDispBuLevelYear > r(p99)

}

collapse (mean) valid_yDispBuLevelYear [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run idBu year demAge sim_yDispBuLevelYear using ///
	"$dir_data/simulation_sample.dta", clear

drop if demAge < 16

* Keep one observation per benefit unit
bysort run year idBu: gen first_person = (_n == 1)
keep if first_person == 1


* Trim outliers
if "$trim_outliers" == "true" {
	
	sum sim_yDispBuLevelYear, d
	
	replace sim_yDispBuLevelYear = . if ///
		sim_yDispBuLevelYear < r(p1) | sim_yDispBuLevelYear > r(p99)

}

collapse (mean) sim_yDispBuLevelYear, by(run year)

collapse (mean) sim_yDispBuLevelYear ///
		 (sd) sim_yDispBuLevelYear_sd = sim_yDispBuLevelYear ///
		 , by(year)
		 
foreach varname in sim_yDispBuLevelYear {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea sim_yDispBuLevelYear_high sim_yDispBuLevelYear_low year, ///
	sort color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_yDispBuLevelYear year, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	title("Benefit Unit Disposable Income") ///
	subtitle("") ///
	xtitle("Year", size(small)) ///
	ytitle("GBP per year", size(small)) ///
	ylabel(,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: Amounts in GBP per year, 2015 prices. Top and bottom percentiles trimmed.", ///
	size(vsmall))

graph export ///
"$dir_output_files/income/disposable_income/validation_${country}_disposable_income_bu_ts.jpg", ///
	replace 
	
	
********************************************************************************
* 2 : Histograms 
********************************************************************************

********************************************************************************
* 2.1 : By year, benefit unit
********************************************************************************

* Prepare validation data
use year idBu demAge dwt valid_yDispBuLevelYear using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Sample selection 
drop if demAge < 16
	
* Keep one observation per benefit unit
bysort year idBu: gen first_person = (_n == 1)
keep if first_person == 1	
	
* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_yDispBuLevelYear, d
	
	replace valid_yDispBuLevelYear = . if ///
		valid_yDispBuLevelYear < r(p1) | ///
		valid_yDispBuLevelYear > r(p99)

}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year idBu sim_yDispBuLevelYear demAge using ///
	"$dir_data/simulation_sample.dta", clear

* Sample selection 
drop if demAge < 16	

* Keep one observation per benefit unit
bysort run year idBu: gen first_person = (_n == 1)
keep if first_person == 1

	
* Trim outliers
if "$trim_outliers" == "true" {
	
	sum sim_yDispBuLevelYear, d
	
	replace sim_yDispBuLevelYear = . if ///
		sim_yDispBuLevelYear < r(p1) | sim_yDispBuLevelYear > r(p99)

}

append using "$dir_data/temp_valid_stats.dta"

qui sum year
local min_year = 2011
local max_year = 2023 

forval year = `min_year'/`max_year' {
	
	twoway (hist sim_yDispBuLevelYear if year == `year', width(2000) ///
		color(green%30) legend(label(1 "SimPaths"))) ///
	(hist valid_yDispBuLevelYear if year == `year', ///
		 width(2000) color(red%30) legend(label(2 "UKHLS"))) , ///
		title("Benefit Unit Disposable Income") ///
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
	"$dir_output_files/income/disposable_income/validation_${country}_disposable_income_bu_dist_`year'.png", ///
		replace width(2560) height(1440) 
	
}

graph drop _all 


********************************************************************************
* 2.2 : Histograms - Benefit unit, ages 18-65, by year, by hours of work 
********************************************************************************

* Prepare validation data
use year demAge dwt valid_yDispBuLevelYear valid_labHrsWorkEnumWeek  ///
	using "$dir_data/ukhls_validation_sample.dta", clear

keep if inrange(demAge,18,65)	
	
* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_yDispBuLevelYear, d
	
	replace valid_yDispBuLevelYear = . if ///
		valid_yDispBuLevelYear < r(p1) | valid_yDispBuLevelYear > r(p99)

}

* Prepare info needed for dynamic y axis labels 
qui sum year
local min_year = 2011
local max_year = 2023

forval year = `min_year'/`max_year' { 

	twoway__histogram_gen valid_yDispBuLevelYear if year == `year' , ///
		width(2500) den gen(d_valid v2)

	qui sum d_valid
	gen max_d_valid_`year' = r(max) 
	
	drop d_valid v2

	foreach ls in $ls_cat {
	
		twoway__histogram_gen valid_yDispBuLevelYear if ///
			year == `year' & valid_labHrsWorkEnumWeek == "`ls'", width(2500) ///
			den gen(d_valid v2)

		qui sum d_valid
		gen max_d_valid_`year'_`ls' = r(max) 
		
		drop d_valid v2	
	
	}
}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year sim_yDispBuLevelYear sim_labHrsWorkEnumWeek demAge using ///
	"$dir_data/simulation_sample.dta", clear

keep if inrange(demAge,18,65)	
	
* Trim outliers
if "$trim_outliers" == "true" {
	
	sum sim_yDispBuLevelYear, d
	
	replace sim_yDispBuLevelYear = . if ///
		sim_yDispBuLevelYear < r(p1) | sim_yDispBuLevelYear > r(p99)

}

keep if run == 1

append using "$dir_data/temp_valid_stats.dta"

* Plot sub-figures
qui sum year
local min_year = 2011
local max_year = 2023 

forval year = `min_year'/`max_year' {
	
    * Prepare info needed for dynamic y axis labels 
	twoway__histogram_gen sim_yDispBuLevelYear if year == `year', width(2500) ///
		den gen(d_sim v1)

	qui sum d_sim 
	gen max_d_sim = r(max)

	gen max_value = max_d_valid_`year' if max_d_valid_`year' > max_d_sim 
	replace max_value = max_d_sim if max_value == . 

	sum max_value 
	local max_y = 1.25*r(max)
	local steps = `max_y'/2

	* Plot all hours
	twoway (hist sim_yDispBuLevelYear if year == `year', width(2500) ///
		color(green%30) legend(label(1 "SimPaths"))) ///
	(hist valid_yDispBuLevelYear if year == `year', width(2500) color(red%30) ///
		legend(label(2 "UKHLS"))) , ///
		subtitle("ALL hours") ///
		name(disp_inc_`year'_all, replace) ///
		xtitle("GBP", size(small)) ///
		ytitle("Density", size(small)) ///
		xlabel(,labsize(vsmall) angle(forty_five)) ///
		ylabel(0(`steps')`max_y', labsize(vsmall)) ///
		graphregion(color(white))		
	
	drop d_sim v1 max_d_sim max_value
	
	foreach ls in $ls_cat {
		
		* Prepare info needed for dynamic y axis labels 
		twoway__histogram_gen sim_yDispBuLevelYear if year == `year' & ///
			sim_labHrsWorkEnumWeek == "`ls'", width(2500) den gen(d_sim v1)

		qui sum d_sim 
		gen max_d_sim = r(max)

		gen max_value = max_d_valid_`year'_`ls' if ///
			max_d_valid_`year'_`ls' > max_d_sim 
		replace max_value = max_d_sim if max_value == . 

		sum max_value 
		local max_y = 1.25*r(max)
		local steps = `max_y'/2	
		
		* Plot by weekly hours work
		twoway (hist sim_yDispBuLevelYear if year == `year' & ///
			sim_labHrsWorkEnumWeek == "`ls'", width(2500) color(green%30) ///
			legend(label(1 "SimPaths"))) ///
		(hist valid_yDispBuLevelYear if year == `year' & ///
			valid_labHrsWorkEnumWeek == "`ls'", width(2500) color(red%30) ///
				legend(label(2 "UKHLS"))) , ///
			subtitle("`ls' hours")  ///
			name(disp_inc_`year'_`ls', replace) ///
			xtitle("GBP", size(small)) ///
			ytitle("Density", size(small)) ///		
			xlabel(,labsize(vsmall) angle(forty_five)) ///
			ylabel(0(`steps')`max_y', labsize(vsmall)) ///
			legend(size(small)) ///
			graphregion(color(white))		

		drop d_sim v1 max_d_sim max_value	
		
	}
}

* Combine plots by  year 
qui sum year
local min_year = 2011
local max_year = 2023

forvalues year = `min_year'/`max_year' {
	
	grc1leg disp_inc_`year'_all ///
		disp_inc_`year'_ZERO ///
		disp_inc_`year'_TEN , ///
		title("Benefit Unit Disposable Income by Weekly Hours of Work") ///
		subtitle("`year'") ///		
		legendfrom(disp_inc_`year'_ZERO) rows(1) ///
		graphregion(color(white)) ///		
		note("Notes: Amounts in GBP per year, 2015 prices. Indiviudal level data of benefit level amount plotted." "Top and bottom percentiles trimmed.", ///
		size(vsmall)) 
		
	graph export ///
	"$dir_output_files/income/disposable_income/validation_${country}_disposable_income_bu_dist_`year'_hrs_work_1.png", ///
		replace width(2400) height(1350) 
		
		grc1leg  ///
		disp_inc_`year'_TWENTY ///
		disp_inc_`year'_THIRTY ///
		disp_inc_`year'_THIRTY_EIGHT ///
		disp_inc_`year'_FORTY_FIVE ///		
		disp_inc_`year'_FIFTY_FIVE, ///
		title("Disposable Income by Weekly Hours of Work") ///
		subtitle("`year'") ///		
		legendfrom(disp_inc_`year'_TWENTY) rows(2) ///
		graphregion(color(white)) ///		
		note("Notes:  Amounts in GBP per year, 2015 prices. Indiviudal level data of benefit level amount plotted." "Top and bottom percentiles trimmed.", ///
		size(vsmall)) 
		
	graph export ///
	"$dir_output_files/income/disposable_income/validation_${country}_disposable_income_bu_dist_`year'_hrs_work_2.png", ///
		replace width(2400) height(1350) 
	
}


graph drop _all 

