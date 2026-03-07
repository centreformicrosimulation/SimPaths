********************************************************************************
* PROJECT:  		ESPON 
* SECTION:			Validation
* OBJECT: 			Disposable income
* AUTHORS:			Ashley Burdett 
* LAST UPDATE:		9/25 
* COUNTRY: 			UK 

* NOTES: 			This do file plots simulated and UKHLS disposable income, 
* 					per benefit unit. Individual level data plotted. 
********************************************************************************

********************************************************************************
* 1 : Mean values over time
********************************************************************************

********************************************************************************
* 1.1 : Mean values over time - benefit uit level 
********************************************************************************

* Prepare validation data
use year dwt valid_y_disp_bu_yr using ///
	"$dir_data/ukhls_validation_sample.dta", clear


* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_y_disp_bu_yr, d
	
	replace valid_y_disp_bu_yr = . if ///
		valid_y_disp_bu_yr < r(p1) | valid_y_disp_bu_yr > r(p99)

}

collapse (mean) valid_y_disp_bu_yr [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year sim_y_disp_yr_bu using "$dir_data/simulated_data.dta", clear

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum sim_y_disp_yr_bu, d
	
	replace sim_y_disp_yr_bu = . if ///
		sim_y_disp_yr_bu < r(p1) | sim_y_disp_yr_bu > r(p99)

}

collapse (mean) sim_y_disp_yr_bu, by(run year)
collapse (mean) sim_y_disp_yr_bu ///
		 (sd) sim_y_disp_yr_bu_sd = sim_y_disp_yr_bu ///
		 , by(year)
		 
foreach varname in sim_y_disp_yr_bu {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea sim_y_disp_yr_bu_high sim_y_disp_yr_bu_low year, ///
	sort color(green%20) legend(label(1 "Simulated"))) ///
(line valid_y_disp_bu_yr year, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	title("Disposable Income") ///
	subtitle("Ages 18-65") ///
	xtitle("Year", size(small)) ///
	ytitle("GBP per year", size(small)) ///
	ylabel(,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: Statistics computed using benefit unit level amounts, averaging using individual level data. Amounts in GBP" "per year, 2015 prices. Top and bottom percentiles trimmed. ", ///
	size(vsmall))

graph export ///
"$dir_output_files/income/disposable_income/validation_${country}_disposable_income_ts_${min_age}_${max_age}_both.jpg", ///
	replace //width(2560) height(1440) quality(100)
	
	
/*	
* EUROMOD	
	
* Prepare EM data
use "$dir_work/${country}_EM_validation_data.dta", clear 


* Trim outliers
if "$trim_outliers" == "true" {
	sum valid_y_disp_yr_bu, d
	replace valid_y_disp_yr_bu = . if ///
		valid_y_disp_yr_bu < r(p1) | valid_y_disp_yr_bu > r(p99)
}


collapse (mean) valid_y_disp_yr_bu [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

 	

* Prepare simulated data
use run year sim_y_disp_yr_bu using "$dir_data/simulated_data.dta", clear


* Trim outliers
if "$trim_outliers" == "true" {
	sum sim_y_disp_yr_bu, d
	replace sim_y_disp_yr_bu = . if ///
		sim_y_disp_yr_bu < r(p1) | sim_y_disp_yr_bu > r(p99)
}


collapse (mean) sim_y_disp_yr_bu, by(run year)
collapse (mean) sim_y_disp_yr_bu ///
		 (sd) sim_y_disp_yr_bu_sd = sim_y_disp_yr_bu ///
		 , by(year)
		 
foreach varname in sim_y_disp_yr_bu {
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd
}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea sim_y_disp_yr_bu_high sim_y_disp_yr_bu_low year, ///
	sort color(green%20) legend(label(1 "Simulated"))) ///
(line valid_y_disp_yr_bu year, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
title("Disposable income") xtitle("Year") ytitle("€ per year (2015 prices)") ///
	ylabel(,labsize(small)) xlabel(,labsize(small)) ///
	graphregion(color(white)) ///
	note("Notes: Statistics computed at the benefit unit level.", size(vsmall))
*/

	
	
********************************************************************************
* 2 : Histograms 
********************************************************************************

********************************************************************************
* 2.1 : Histograms - Ages 18-65, by year 
********************************************************************************

* Prepare validation data
use year dwt valid_y_disp_bu_yr laboursupplyweekly_hu dag  ///
	using "$dir_data/ukhls_validation_sample.dta", clear

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_y_disp_bu_yr, d
	
	replace valid_y_disp_bu_yr = . if ///
		valid_y_disp_bu_yr < r(p1) | valid_y_disp_bu_yr > r(p99)

}

* Prepare info needed for dynamic y axis labels 
qui sum year
local min_year = 2011
local max_year = r(max)  

forval year = `min_year'/`max_year' { 

	twoway__histogram_gen valid_y_disp_bu_yr if year == `year' , ///
		width(500) den gen(d_valid v2)

	qui sum d_valid
	gen max_d_valid_`year' = r(max) 
	
	drop d_valid v2

	foreach ls in $ls_cat {
	
		twoway__histogram_gen valid_y_disp_bu_yr if ///
		year == `year' & labour == "`ls'", width(500) den gen(d_valid v2)

		qui sum d_valid
		gen max_d_valid_`year'_`ls' = r(max) 
		
		drop d_valid v2	
	
	}
}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year sim_y_disp_yr_bu laboursupplyweekly_orig dag using ///
	"$dir_data/simulated_data.dta", clear

	
* Trim outliers
if "$trim_outliers" == "true" {
	
	sum sim_y_disp_yr_bu, d
	
	replace sim_y_disp_yr_bu = . if ///
		sim_y_disp_yr_bu < r(p1) | sim_y_disp_yr_bu > r(p99)

}

keep if run == 1

append using "$dir_data/temp_valid_stats.dta"

* Plot sub-figures
qui sum year
local min_year = 2011
local max_year = r(max)  

forval year = `min_year'/`max_year' {
	
    * Prepare info needed for dynamic y axis labels 
	twoway__histogram_gen sim_y_disp_yr_bu if year == `year', width(500) ///
		den gen(d_sim v1)

	qui sum d_sim 
	gen max_d_sim = r(max)

	gen max_value = max_d_valid_`year' if max_d_valid_`year' > max_d_sim 
	replace max_value = max_d_sim if max_value == . 

	sum max_value 
	local max_y = 1.25*r(max)
	local steps = `max_y'/2

	* Plot all hours
	twoway (hist sim_y_disp_yr_bu if year == `year', width(500) ///
		color(green%30) legend(label(1 "Simulated"))) ///
	(hist valid_y_disp_bu_yr if year == `year', width(500) color(red%30) ///
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
		twoway__histogram_gen sim_y_disp_yr_bu if year == `year' & ///
			laboursupplyweekly_orig == "`ls'", width(500) den gen(d_sim v1)

		qui sum d_sim 
		gen max_d_sim = r(max)

		gen max_value = max_d_valid_`year'_`ls' if ///
			max_d_valid_`year'_`ls' > max_d_sim 
		replace max_value = max_d_sim if max_value == . 

		sum max_value 
		local max_y = 1.25*r(max)
		local steps = `max_y'/2	
		
		* Plot by weekly hours work
		twoway (hist sim_y_disp_yr_bu if year == `year' & ///
			laboursupplyweekly_orig == "`ls'", width(500) color(green%30) ///
			legend(label(1 "Simulated"))) ///
		(hist valid_y_disp_bu_yr if year == `year' & ///
			laboursupplyweekly_hu == "`ls'", width(500) color(red%30) ///
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
local max_year = r(max)  

forvalues year = `min_year'/`max_year' {
	
	grc1leg disp_inc_`year'_all ///
		disp_inc_`year'_ZERO ///
		disp_inc_`year'_TEN ///
		disp_inc_`year'_TWENTY ///
		disp_inc_`year'_THIRTY ///
		disp_inc_`year'_FORTY, ///
		title("Disposable Income by Weekly Hours of Work") ///
		subtitle("`year',") ///		
		legendfrom(disp_inc_`year'_ZERO) rows(2) ///
		graphregion(color(white)) ///		
		note("Notes:  Amounts in GBP per year, 2015 prices. Indiviudal level data of benefit level variable plotted." "Top and bottom percentiles trimmed. Weekly hours worked categories: ZERO = 0, TWENTY = [1,39], FORTY = 40, FIFTY = 41+", ///
		size(vsmall)) 
		
	graph export ///
	"$dir_output_files/income/disposable_income/validation_${country}_disposable_income_dist_`year'.png", ///
		replace width(2400) height(1350) 
}


graph drop _all 


/*

* Males 

* Prepare validation data
use year dwt valid_y_disp_yr_bu laboursupplyweekly_hu dag dgn ///
	using "$dir_data/ukhls_validation_sample.dta", clear

keep if dgn == 1 
drop dgn 			

* Trim outliers
if "$trim_outliers" == "true" {
	sum valid_y_disp_yr_bu, d
	
	replace valid_y_disp_yr_bu = . if ///
		valid_y_disp_yr_bu < r(p1) | valid_y_disp_yr_bu > r(p99)
}

* Prepare info needed for dynamic y axis labels 
qui sum year
local min_year = r(min)  
local max_year = r(max)  

forval year = `min_year'/`max_year' { 

	twoway__histogram_gen valid_y_disp_yr_bu if year == `year' , ///
		width(500) den gen(d_valid v2)

	qui sum d_valid
	gen max_d_valid_`year' = r(max) 
	
	drop d_valid v2

	foreach ls in $ls_cat {
	
		twoway__histogram_gen valid_y_disp_yr_bu if ///
		year == `year' & labour == "`ls'", width(500) den gen(d_valid v2)

		qui sum d_valid
		gen max_d_valid_`year'_`ls' = r(max) 
		
		drop d_valid v2	
	
	}
}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year sim_y_disp_yr_bu laboursupplyweekly dag dgn using ///
	"$dir_data/simulated_data.dta", clear

keep if dgn == "Male"
drop dgn		
	
* Trim outliers
if "$trim_outliers" == "true" {
	sum sim_y_disp_yr_bu, d
	replace sim_y_disp_yr_bu = . if ///
		sim_y_disp_yr_bu < r(p1) | sim_y_disp_yr_bu > r(p99)
}

keep if run == 1

append using "$dir_data/temp_valid_stats.dta"

* Plot sub-figures
qui sum year
local min_year = r(min)  
local max_year = r(max)  

forval year = `min_year'/`max_year' {
	
    * Prepare info needed for dynamic y axis labels 
	twoway__histogram_gen sim_y_disp_yr_bu if year == `year', width(500) ///
		den gen(d_sim v1)

	qui sum d_sim 
	gen max_d_sim = r(max)

	gen max_value = max_d_valid_`year' if max_d_valid_`year' > max_d_sim 
	replace max_value = max_d_sim if max_value == . 

	sum max_value 
	local max_y = 1.25*r(max)
	local steps = `max_y'/2

	* Plot all hours
	twoway (hist sim_y_disp_yr_bu if year == `year', width(500) ///
		color(green%30) legend(label(1 "Simulated"))) ///
	(hist valid_y_disp_yr_bu if year == `year' , width(500) color(red%30) ///
		legend(label(2 "UKHLS"))) , ///
	subtitle("ALL hours") name(disp_inc_`year'_all, replace) ///
		xlabel(,labsize(vsmall) angle(forty_five)) ///
		ylabel(0(`steps')`max_y', labsize(vsmall)) ///
		graphregion(color(white))		
	
	drop d_sim v1 max_d_sim max_value
	
	foreach ls in $ls_cat {
		
		* Prepare info needed for dynamic y axis labels 
		twoway__histogram_gen sim_y_disp_yr_bu if year == `year' & ///
			laboursupplyweekly == "`ls'", width(500) den gen(d_sim v1)

		qui sum d_sim 
		gen max_d_sim = r(max)

		gen max_value = max_d_valid_`year'_`ls' if ///
			max_d_valid_`year'_`ls' > max_d_sim 
		replace max_value = max_d_sim if max_value == . 

		sum max_value 
		local max_y = 1.25*r(max)
		local steps = `max_y'/2	
		
		* Plot by weekly hours work
		twoway (hist sim_y_disp_yr_bu if year == `year' & ///
			laboursupplyweekly == "`ls'", width(500) color(green%30) ///
			legend(label(1 "Simulated"))) ///
		(hist valid_y_disp_yr_bu if year == `year' & ///
			laboursupplyweekly_hu == "`ls'", width(500) color(red%30) ///
				legend(label(2 "UKHLS"))) , ///
		subtitle("`ls' hours")  name(disp_inc_`year'_`ls', replace) ///
			xlabel(,labsize(vsmall) angle(forty_five)) ///
			ylabel(0(`steps')`max_y', labsize(vsmall)) ///
			graphregion(color(white))		

		drop d_sim v1 max_d_sim max_value	
		
	}
}

* Combine plots by  year 
qui sum year
local min_year = r(min)  
local max_year = r(max)  

forvalues year = `min_year'/`max_year' {
	
	grc1leg disp_inc_`year'_all disp_inc_`year'_ZERO disp_inc_`year'_TWENTY ///
		disp_inc_`year'_FORTY ///
		disp_inc_`year'_FIFTY, ///
		title("Disposable income by weekly hours of work") ///
		subtitle("`year', Males") ///		
		legendfrom(disp_inc_`year'_all) rows(2) ///
		graphregion(color(white)) ///		
		note("Notes: Sample includes all males aged 18-65. Values in € per year (2015 prices). Indiviudal level data for benefit level variable. Samples" "trimmed. Weekly hours worked categories: ZERO = 0, TWENTY = [1,39], FORTY = 40, FIFTY = 41+", ///
		size(vsmall)) 
		
	graph export ///
	"$dir_output_files/income/disposable_income/validation_${country}_disposable_income_dist_`year'_male.png", ///
		replace width(2400) height(1350) 
}


graph drop _all 


* Females 

* Prepare validation data
use year dwt valid_y_disp_yr_bu laboursupplyweekly_hu dag dgn ///
	using "$dir_data/ukhls_validation_sample.dta", clear

keep if dgn == 0 
drop dgn 			

* Trim outliers
if "$trim_outliers" == "true" {
	sum valid_y_disp_yr_bu, d
	
	replace valid_y_disp_yr_bu = . if ///
		valid_y_disp_yr_bu < r(p1) | valid_y_disp_yr_bu > r(p99)
}

* Prepare info needed for dynamic y axis labels 
qui sum year
local min_year = r(min)  
local max_year = r(max)  

forval year = `min_year'/`max_year' { 

	twoway__histogram_gen valid_y_disp_yr_bu if year == `year' , ///
		width(500) den gen(d_valid v2)

	qui sum d_valid
	gen max_d_valid_`year' = r(max) 
	
	drop d_valid v2

	foreach ls in $ls_cat {
	
		twoway__histogram_gen valid_y_disp_yr_bu if ///
		year == `year' & labour == "`ls'", width(500) den gen(d_valid v2)

		qui sum d_valid
		gen max_d_valid_`year'_`ls' = r(max) 
		
		drop d_valid v2	
	
	}
}

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_y_disp_yr_bu laboursupplyweekly dag dgn using ///
	"$dir_data/simulated_data.dta", clear

keep if dgn == "Female"
drop dgn		
	
* Trim outliers
if "$trim_outliers" == "true" {
	sum sim_y_disp_yr_bu, d
	replace sim_y_disp_yr_bu = . if ///
		sim_y_disp_yr_bu < r(p1) | sim_y_disp_yr_bu > r(p99)
}

keep if run == 1


append using "$dir_data/temp_valid_stats.dta"

* Plot sub-figures
qui sum year
local min_year = r(min)  
local max_year = r(max)  

forval year = `min_year'/`max_year' {
	
    * Prepare info needed for dynamic y axis labels 
	twoway__histogram_gen sim_y_disp_yr_bu if year == `year', width(500) ///
		den gen(d_sim v1)

	qui sum d_sim 
	gen max_d_sim = r(max)

	gen max_value = max_d_valid_`year' if max_d_valid_`year' > max_d_sim 
	replace max_value = max_d_sim if max_value == . 

	sum max_value 
	local max_y = 1.25*r(max)
	local steps = `max_y'/2

	* Plot all hours
	twoway (hist sim_y_disp_yr_bu if year == `year' , width(500) ///
		color(green%30) legend(label(1 "Simulated"))) ///
	(hist valid_y_disp_yr_bu if year == `year' , width(500) color(red%30) ///
		legend(label(2 "UKHLS"))) , ///
	subtitle("ALL hours") name(disp_inc_`year'_all, replace) ///
		xlabel(,labsize(vsmall) angle(forty_five)) ///
		ylabel(0(`steps')`max_y', labsize(vsmall)) ///
		graphregion(color(white))		
	
	drop d_sim v1 max_d_sim max_value
	
	foreach ls in $ls_cat {
		
		* Prepare info needed for dynamic y axis labels 
		twoway__histogram_gen sim_y_disp_yr_bu if year == `year' & ///
			laboursupplyweekly == "`ls'", width(500) den gen(d_sim v1)

		qui sum d_sim 
		gen max_d_sim = r(max)

		gen max_value = max_d_valid_`year'_`ls' if ///
			max_d_valid_`year'_`ls' > max_d_sim 
		replace max_value = max_d_sim if max_value == . 

		sum max_value 
		local max_y = 1.25*r(max)
		local steps = `max_y'/2	
		
		* Plot by weekly hours work
		twoway (hist sim_y_disp_yr_bu if year == `year' & ///
			laboursupplyweekly == "`ls'", width(500) color(green%30) ///
			legend(label(1 "Simulated"))) ///
		(hist valid_y_disp_yr_bu if year == `year' & ///
			laboursupplyweekly_hu == "`ls'", width(500) color(red%30) ///
				legend(label(2 "UKHLS"))) , ///
		subtitle("`ls' hours")  name(disp_inc_`year'_`ls', replace) ///
			xlabel(,labsize(vsmall) angle(forty_five)) ///
			ylabel(0(`steps')`max_y', labsize(vsmall)) ///
			graphregion(color(white))		

		drop d_sim v1 max_d_sim max_value	
		
	}
}

* Combine plots by  year 
qui sum year
local min_year = r(min)  
local max_year = r(max)  

forvalues year = `min_year'/`max_year' {
	
	grc1leg disp_inc_`year'_all disp_inc_`year'_ZERO disp_inc_`year'_TWENTY ///
		disp_inc_`year'_FORTY  ///
		disp_inc_`year'_FIFTY, ///
		title("Disposable income by weekly hours of work") ///
		subtitle("`year', Females") ///		
		legendfrom(disp_inc_`year'_all) rows(2) ///
		graphregion(color(white)) ///		
		note("Notes: Sample includes all females aged 18-65. Values in € per year (2015 prices). Indiviudal level data for benefit level variable. Samples" "trimmed. Weekly hours worked categories: ZERO = 0, TWENTY = [1,39], FORTY = 40, FIFTY = 41+.", ///
		size(vsmall)) 
		
	graph export ///
	"$dir_output_files/income/disposable_income/validation_${country}_disposable_income_dist_`year'_female.png", ///
		replace width(2400) height(1350) 
}


graph drop _all 
