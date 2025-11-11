********************************************************************************
* PROJECT:  		ESPON 
* SECTION:			Validation
* OBJECT: 			Equivalised disposable income
* AUTHORS:			Ashley Burdett 
* LAST UPDATE:		9/25 (AB)
* COUNTRY: 			UK 

* NOTES: 			This do file plots simulated and UKHLS equivalised 
* 					disposable income, per benefit unit
********************************************************************************

********************************************************************************
* 1 : Mean values over time
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


collapse (mean) valid_y_eq_disp_bu_yr [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year equivalisedincome using "$dir_data/simulated_data.dta", clear

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum equivalisedincome, d
	
	replace equivalisedincome = . if ///
		equivalisedincome < r(p1) | equivalisedincome > r(p99)

}


collapse (mean) equivalisedincome, by(run year)
collapse (mean) equivalisedincome ///
		 (sd) equivalisedincome_sd = equivalisedincome, by(year)
		 
foreach varname in equivalisedincome {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea equivalisedincome_high equivalisedincome_low year, ///
	sort color(green%20) legend(label(1 "Simulated"))) ///
(line valid_y_eq_disp_bu_yr year, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	title("Equivalised Disposable Income") ///
	subtitle("Ages 18-65") ///
	xtitle("Year", size(small)) ///
	ytitle("GBP per year", size(small)) ///
	ylabel(,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: Equivalised disposable income computed by the modified OECD scale. Amount at the benefit unit level, individual data" "plotted. Top and bottom percentiles trimmed. Amounts annual, in 2015 prices.", ///
	size(vsmall))

graph export ///
"$dir_output_files/income/equivalised_disposable_income/validation_${country}_equivalised_disposable_income_ts_${min_age}_${max_age}_both.jpg", ///
	replace width(2400) height(1350) 
	
/*	
* Males 

* Prepare validation data
use year dwt valid_y_eq_disp_yr_bu dgn using ///
	"$dir_data/ukhls_validation_sample.dta", clear

keep if dgn == 1

* Trim outliers
if "$trim_outliers" == "true" {
	sum valid_y_eq_disp_yr_bu, d
	replace valid_y_eq_disp_yr_bu = . if ///
		valid_y_eq_disp_yr_bu < r(p1) | valid_y_eq_disp_yr_bu > r(p99)
}

collapse (mean) valid_y_eq_disp_yr_bu [aw = dwt], by(year)


save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year equivalisedincome dgn using "$dir_data/simulated_data.dta", clear

keep if dgn == "Male"

* Trim outliers
if "$trim_outliers" == "true" {
	sum equivalisedincome, d
	replace equivalisedincome = . if ///
		equivalisedincome < r(p1) | equivalisedincome > r(p99)
}

collapse (mean) equivalisedincome, by(run year)
collapse (mean) equivalisedincome ///
		 (sd) equivalisedincome_sd = equivalisedincome, by(year)
		 
foreach varname in equivalisedincome {
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd
}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea equivalisedincome_high equivalisedincome_low year, ///
	sort color(green%20) legend(label(1 "Simulated"))) ///
(line valid_y_eq_disp_yr_bu year, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	title("Equivalised disposable income") subtitle("Males") ///
	xtitle("Year") ///
	ytitle("€ per year.") ///
	ylabel(,labsize(small)) xlabel(,labsize(small)) ///
	graphregion(color(white)) ///
	note("Notes: Equivalised disposable income assigned to each person computed by adjusting benefit unit's disposable income by the" "modified OECD scale. Samples includes males ages 18-65. Trimmed sample. Amounts in 2015 prices.", ///
	size(vsmall))

graph export ///
"$dir_output_files/income/equivalised_disposable_income/validation_${country}_equivalised_disposable_income_ts_${min_age}_${max_age}_male.jpg", ///
	replace width(2400) height(1350) 	
	
	
* Females 

* Prepare validation data
use year dwt valid_y_eq_disp_yr_bu dgn using ///
	"$dir_data/ukhls_validation_sample.dta", clear

keep if dgn == 0

* Trim outliers
if "$trim_outliers" == "true" {
	sum valid_y_eq_disp_yr_bu, d
	replace valid_y_eq_disp_yr_bu = . if ///
		valid_y_eq_disp_yr_bu < r(p1) | valid_y_eq_disp_yr_bu > r(p99)
}

collapse (mean) valid_y_eq_disp_yr_bu [aw = dwt], by(year)


save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year equivalisedincome dgn using "$dir_data/simulated_data.dta", clear

keep if dgn == "Female"

* Trim outliers
if "$trim_outliers" == "true" {
	sum equivalisedincome, d
	replace equivalisedincome = . if ///
		equivalisedincome < r(p1) | equivalisedincome > r(p99)
}

collapse (mean) equivalisedincome, by(run year)
collapse (mean) equivalisedincome ///
		 (sd) equivalisedincome_sd = equivalisedincome, by(year)
		 
foreach varname in equivalisedincome {
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd
}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea equivalisedincome_high equivalisedincome_low year, ///
	sort color(green%20) legend(label(1 "Simulated"))) ///
(line valid_y_eq_disp_yr_bu year, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	title("Equivalised disposable income") subtitle("Females") ///
	xtitle("Year") ///
	ytitle("€ per year.") ///
	ylabel(,labsize(small)) xlabel(,labsize(small)) ///
	graphregion(color(white)) ///
	note("Notes: Equivalised disposable income assigned to each person computed by adjusting benefit unit's disposable income by the" "modified OECD scale. Samples includes females ages 18-65. Trimmed sample. Amounts in 2015 prices.", ///
	size(vsmall))

graph export ///
"$dir_output_files/income/equivalised_disposable_income/validation_${country}_equivalised_disposable_income_ts_${min_age}_${max_age}_female.jpg", ///
	replace width(2400) height(1350) 		
*/

********************************************************************************
* 2 : Histograms by year, and by category of weekly labour supply 
********************************************************************************


* Prepare validation data
use year dwt valid_y_eq_disp_bu_yr laboursupplyweekly_hu using ///
	"$dir_data/ukhls_validation_sample.dta", clear		
	
* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_y_eq_disp_bu_yr, d
	
	replace valid_y_eq_disp_bu_yr = . if ///
		valid_y_eq_disp_bu_yr < r(p1) | valid_y_eq_disp_bu_yr > r(p99)
		
}

* Prepare info needed for dynamic y axis labels 
qui sum year
local min_year = 2011
local max_year = r(max)  

forval year = `min_year'/`max_year' { 

	twoway__histogram_gen valid_y_eq_disp_bu_yr if year == `year' , ///
		width(500) den gen(d_valid v2)

	qui sum d_valid
	gen max_d_valid_`year' = r(max) 
	
	drop d_valid v2

	foreach ls in $ls_cat {
	
		twoway__histogram_gen valid_y_eq_disp_bu_yr if ///
		year == `year' & labour == "`ls'", width(500) den gen(d_valid v2)

		qui sum d_valid
		gen max_d_valid_`year'_`ls' = r(max) 
		
		drop d_valid v2	
	
	}
}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year equivalisedincome laboursupplyweekly using ///
	"$dir_data/simulated_data.dta", clear

	
* Trim outliers
if "$trim_outliers" == "true" {
	
	sum equivalisedincome, d
	
	replace equivalisedincome = . if ///
		equivalisedincome < r(p1) | equivalisedincome > r(p99)

}

keep if run == 1

append using "$dir_data/temp_valid_stats.dta"

qui sum year
local min_year = 2011
local max_year = r(max)  

forval year = `min_year'/`max_year' {
	
    * Prepare info needed for dynamic y axis labels 
	twoway__histogram_gen equivalisedincome if year == `year', width(500) ///
		den gen(d_sim v1)

	qui sum d_sim 
	gen max_d_sim = r(max)

	gen max_value = max_d_valid_`year' if max_d_valid_`year' > max_d_sim 
	replace max_value = max_d_sim if max_value == . 

	sum max_value 
	local max_y = 1.25*r(max)
	local steps = `max_y'/2
	
	* Plot all hours
	twoway (hist equivalisedincome if year == `year', width(500) ///
		color(green%30) legend(label(1 "Simulated"))) ///
	(hist valid_y_eq_disp_bu_yr if year == `year', width(500) color(red%30) ///
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
		twoway__histogram_gen equivalisedincome if year == `year' & ///
			laboursupplyweekly_orig == "`ls'", width(500) den gen(d_sim v1)

		qui sum d_sim 
		gen max_d_sim = r(max)

		gen max_value = max_d_valid_`year'_`ls' if ///
			max_d_valid_`year'_`ls' > max_d_sim 
		replace max_value = max_d_sim if max_value == . 

		sum max_value 
		local max_y = 1.25*r(max)
		local steps = `max_y'/2		
	
		twoway (hist equivalisedincome if year == `year' &  ///
			laboursupplyweekly_orig == "`ls'", width(500) color(green%30) ///
			legend(label(1 "Simulated"))) ///
		(hist valid_y_eq_disp_bu_yr if year == `year' & ///
			laboursupplyweekly_hu == "`ls'", width(500) color(red%30) ///
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
local max_year = r(max)  

forvalues year = `min_year'/`max_year' {
	
	grc1leg eqdisp_inc_`year'_all ///
		eqdisp_inc_`year'_ZERO ///
		eqdisp_inc_`year'_TEN ///
		eqdisp_inc_`year'_TWENTY ///
		eqdisp_inc_`year'_THIRTY ///
		eqdisp_inc_`year'_FORTY, ///
		title("Equivalised Disposable Income") ///
		subtitle("`year'") ///
		legendfrom(eqdisp_inc_`year'_all) rows(2) ///
		graphregion(color(white)) ///		
		note("Notes: Distribution of benefit unit equivalised disposable income. Individual level data plotted 18-65 year olds included in sample. Amounts in" "GBP per year, 2015 prices. Top and bottom percentiles trimmed. Weekly hours worked categories:" "ZERO = [0,5], TEN = [6,15], TWENTY = [16,25], THIRTY = [26,34], FORTY = 36+.", ///
		size(vsmall)) 
		
	graph export ///
 "$dir_output_files/income/equivalised_disposable_income/validation_${country}_equivalised_disposable_inc_dist_`year'.png", ///
		replace width(2560) height(1440) 

}

graph drop _all 



/*
* Males 

* Prepare validation data
use year dwt valid_y_eq_disp_yr_bu laboursupplyweekly_hu dgn using ///
	"$dir_data/ukhls_validation_sample.dta", clear

keep if dgn == 1 
drop dgn 			
		
	
* Trim outliers
if "$trim_outliers" == "true" {
	sum valid_y_eq_disp_yr_bu, d
	
	replace valid_y_eq_disp_yr_bu = . if ///
		valid_y_eq_disp_yr_bu < r(p1) | valid_y_eq_disp_yr_bu > r(p99)
}

* Prepare info needed for dynamic y axis labels 
qui sum year
local min_year = r(min)  
local max_year = r(max)  

forval year = `min_year'/`max_year' { 

	twoway__histogram_gen valid_y_eq_disp_yr_bu if year == `year' , ///
		width(500) den gen(d_valid v2)

	qui sum d_valid
	gen max_d_valid_`year' = r(max) 
	
	drop d_valid v2

	foreach ls in $ls_cat {
	
		twoway__histogram_gen valid_y_eq_disp_yr_bu if ///
		year == `year' & labour == "`ls'", width(500) den gen(d_valid v2)

		qui sum d_valid
		gen max_d_valid_`year'_`ls' = r(max) 
		
		drop d_valid v2	
	
	}
}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year equivalisedincome laboursupplyweekly dgn using ///
	"$dir_data/simulated_data.dta", clear

keep if dgn == "Male"
drop dgn	
	
	
* Trim outliers
if "$trim_outliers" == "true" {
	sum equivalisedincome, d
	replace equivalisedincome = . if ///
		equivalisedincome < r(p1) | equivalisedincome > r(p99)
}

keep if run == 1

append using "$dir_data/temp_valid_stats.dta"

qui sum year
local min_year = r(min)  
local max_year = r(max)  

forval year = `min_year'/`max_year' {
	
    * Prepare info needed for dynamic y axis labels 
	twoway__histogram_gen equivalisedincome if year == `year', width(500) ///
		den gen(d_sim v1)

	qui sum d_sim 
	gen max_d_sim = r(max)

	gen max_value = max_d_valid_`year' if max_d_valid_`year' > max_d_sim 
	replace max_value = max_d_sim if max_value == . 

	sum max_value 
	local max_y = 1.25*r(max)
	local steps = `max_y'/2
	
	* Plot all hours
	twoway (hist equivalisedincome if year == `year' , ///
		color(green%30) legend(label(1 "Simulated"))) ///
	(hist valid_y_eq_disp_yr_bu if year == `year' , color(red%30) ///
		legend(label(2 "UKHLS"))) , ///
	subtitle("ALL hours") name(eqdisp_inc_`year'_all, replace) ///
		xlabel(,labsize(vsmall) angle(forty_five)) ///
		ylabel(0(`steps')`max_y', labsize(vsmall)) ///
		graphregion(color(white))		
		
	drop d_sim v1 max_d_sim max_value	
	
	foreach ls in $ls_cat {
	
		* Prepare info needed for dynamic y axis labels 
		twoway__histogram_gen equivalisedincome if year == `year' & ///
			laboursupplyweekly == "`ls'", width(500) den gen(d_sim v1)

		qui sum d_sim 
		gen max_d_sim = r(max)

		gen max_value = max_d_valid_`year'_`ls' if ///
			max_d_valid_`year'_`ls' > max_d_sim 
		replace max_value = max_d_sim if max_value == . 

		sum max_value 
		local max_y = 1.25*r(max)
		local steps = `max_y'/2		
	
		twoway (hist equivalisedincome if year == `year' &  ///
			laboursupplyweekly == "`ls'", color(green%30) ///
			legend(label(1 "Simulated"))) ///
		(hist valid_y_eq_disp_yr_bu if year == `year' & ///
			laboursupplyweekly_hu == "`ls'", color(red%30) ///
			legend(label(2 "UKHLS"))) , ///
		subtitle("`ls' hours")  name(eqdisp_inc_`year'_`ls', replace) ///
			xlabel(,labsize(vsmall) angle(forty_five)) ///
			ylabel(0(`steps')`max_y', labsize(vsmall)) ///
			graphregion(color(white)) 
			
		drop d_sim v1 max_d_sim max_value
		
	}
}

qui sum year
local min_year = r(min)  
local max_year = r(max)  

forvalues year = `min_year'/`max_year' {
	
	grc1leg eqdisp_inc_`year'_all eqdisp_inc_`year'_ZERO ///
		eqdisp_inc_`year'_TWENTY eqdisp_inc_`year'_FORTY ///
		eqdisp_inc_`year'_FIFTY, ///
		title("Equivalised disposable income") ///
		subtitle("`year', Males") ///
		legendfrom(eqdisp_inc_`year'_all) rows(2) ///
		graphregion(color(white)) ///		
		note("Notes: Series represents average benefit unit equivalised disposable income for all persons ages 18-65. Individual observations plotted." "Values in € per year, 2015 prices. Sample trimmed. Weekly hours worked categories:" "ZERO = 0, TWENTY = [1,39], FORTY = 40, FIFTY = 41+", ///
		size(vsmall)) 
		
	graph export ///
 "$dir_output_files/income/equivalised_disposable_income/validation_${country}_equivalised_disposable_inc_dist_`year'_male.png", ///
		replace width(2560) height(1440) 

}

graph drop _all 


* Females 

* Prepare validation data
use year dwt valid_y_eq_disp_yr_bu laboursupplyweekly_hu dgn using ///
	"$dir_data/ukhls_validation_sample.dta", clear

keep if dgn == 0 
drop dgn 			
		
	
* Trim outliers
if "$trim_outliers" == "true" {
	sum valid_y_eq_disp_yr_bu, d
	replace valid_y_eq_disp_yr_bu = . if ///
		valid_y_eq_disp_yr_bu < r(p1) | valid_y_eq_disp_yr_bu > r(p99)
}

* Prepare info needed for dynamic y axis labels 
qui sum year
local min_year = r(min)  
local max_year = r(max)  

forval year = `min_year'/`max_year' { 

	twoway__histogram_gen valid_y_eq_disp_yr_bu if year == `year' , ///
		width(500) den gen(d_valid v2)

	qui sum d_valid
	gen max_d_valid_`year' = r(max) 
	
	drop d_valid v2

	foreach ls in $ls_cat {
	
		twoway__histogram_gen valid_y_eq_disp_yr_bu if ///
		year == `year' & labour == "`ls'", width(500) den gen(d_valid v2)

		qui sum d_valid
		gen max_d_valid_`year'_`ls' = r(max) 
		
		drop d_valid v2	
	
	}
}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year equivalisedincome laboursupplyweekly dgn using ///
	"$dir_data/simulated_data.dta", clear

keep if dgn == "Female"
drop dgn	
	
	
* Trim outliers
if "$trim_outliers" == "true" {
	sum equivalisedincome, d
	replace equivalisedincome = . if ///
		equivalisedincome < r(p1) | equivalisedincome > r(p99)
}

keep if run == 1

append using "$dir_data/temp_valid_stats.dta"

qui sum year
local min_year = r(min)  
local max_year = r(max)  

forval year = `min_year'/`max_year' {
	
    * Prepare info needed for dynamic y axis labels 
	twoway__histogram_gen equivalisedincome if year == `year', width(500) ///
		den gen(d_sim v1)

	qui sum d_sim 
	gen max_d_sim = r(max)

	gen max_value = max_d_valid_`year' if max_d_valid_`year' > max_d_sim 
	replace max_value = max_d_sim if max_value == . 

	sum max_value 
	local max_y = 1.25*r(max)
	local steps = `max_y'/2
	
	* Plot all hours
	twoway (hist equivalisedincome if year == `year' , ///
		color(green%30) legend(label(1 "Simulated"))) ///
	(hist valid_y_eq_disp_yr_bu if year == `year' , color(red%30) ///
		legend(label(2 "UKHLS"))) , ///
	subtitle("ALL hours") name(eqdisp_inc_`year'_all, replace) ///
		xlabel(,labsize(vsmall) angle(forty_five)) ///
		ylabel(0(`steps')`max_y', labsize(vsmall)) ///
		graphregion(color(white))		
		
	drop d_sim v1 max_d_sim max_value	
	
	foreach ls in $ls_cat {
	
		* Prepare info needed for dynamic y axis labels 
		twoway__histogram_gen equivalisedincome if year == `year' & ///
			laboursupplyweekly == "`ls'", width(500) den gen(d_sim v1)

		qui sum d_sim 
		gen max_d_sim = r(max)

		gen max_value = max_d_valid_`year'_`ls' if ///
			max_d_valid_`year'_`ls' > max_d_sim 
		replace max_value = max_d_sim if max_value == . 

		sum max_value 
		local max_y = 1.25*r(max)
		local steps = `max_y'/2		
	
		twoway (hist equivalisedincome if year == `year' &  ///
			laboursupplyweekly == "`ls'", color(green%30) ///
			legend(label(1 "Simulated"))) ///
		(hist valid_y_eq_disp_yr_bu if year == `year' & ///
			laboursupplyweekly_hu == "`ls'", color(red%30) ///
			legend(label(2 "UKHLS"))) , ///
		subtitle("`ls' hours")  name(eqdisp_inc_`year'_`ls', replace) ///
			xlabel(,labsize(vsmall) angle(forty_five)) ///
			ylabel(0(`steps')`max_y', labsize(vsmall)) ///
			graphregion(color(white)) 
			
		drop d_sim v1 max_d_sim max_value
		
	}
}

qui sum year
local min_year = r(min)  
local max_year = r(max)  

forvalues year = `min_year'/`max_year' {
	
	grc1leg eqdisp_inc_`year'_all eqdisp_inc_`year'_ZERO ///
		eqdisp_inc_`year'_TWENTY eqdisp_inc_`year'_FORTY ///
		eqdisp_inc_`year'_FIFTY, ///
		title("Equivalised disposable income") ///
		subtitle("`year', Females") ///
		legendfrom(eqdisp_inc_`year'_all) rows(2) ///
		graphregion(color(white)) ///		
		note("Notes: Series represents average benefit unit equivalised disposable income for all persons ages 18-65. Individual observations plotted" "Values in € per year, 2015 prices. Sample trimmed. Weekly hours worked categories:" "ZERO = 0, TWENTY = [1,39], FORTY = 40, FIFTY = 41+.", ///
		size(vsmall)) 
		
	graph export ///
 "$dir_output_files/income/equivalised_disposable_income/validation_${country}_equivalised_disposable_inc_dist_`year'_female.png", ///
		replace width(2560) height(1440) 

}

graph drop _all 
