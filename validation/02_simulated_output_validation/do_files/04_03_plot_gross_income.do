********************************************************************************
* PROJECT:  		SimPaths UK 
* SECTION:			Validation
* OBJECT: 			Gross income
* AUTHORS:			Ashley Burdett 
* LAST UPDATE:		Jan 2026
* COUNTRY: 			UK 
********************************************************************************
* NOTES: 			
********************************************************************************

********************************************************************************
* 1 : Mean values over time
********************************************************************************

********************************************************************************
* 1.1 : Mean values over time - Benefit unit amounts
********************************************************************************

* Prepare validation data 
use year dwt idBu idPers demAge valid_yGrossBuLevelYear using ///
	"$dir_data/ukhls_validation_sample.dta", clear
	
keep if demAge >= 16

* Keep one observation per benefit unit
bysort year idBu: gen first_person = (_n == 1)
keep if first_person == 1

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_yGrossBuLevelYear, d
	
	replace valid_yGrossBuLevelYear = . if ///
		valid_yGrossBuLevelYear < r(p1) | ///
		valid_yGrossBuLevelYear > r(p99)

}

collapse (mean) valid_yGrossBuLevelYear [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year idPers idBu demAge sim_yGrossBuLevelYear using ///
	"$dir_data/simulation_sample.dta", clear

keep if demAge >= 16

* Keep one observation per benefit unit
bysort run year idBu: gen first_person = (_n == 1)
keep if first_person == 1

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum sim_yGrossBuLevelYear, d
	
	replace sim_yGrossBuLevelYear = . if sim_yGrossBuLevelYear < r(p1) | ///
		sim_yGrossBuLevelYear > r(p99)

}

collapse (mean) sim_yGrossBuLevelYear, by(run year)

collapse (mean) sim_yGrossBuLevelYear ///
		 (sd) sim_yGrossBuLevelYear_sd = sim_yGrossBuLevelYear ///
		 , by(year)
		 
foreach varname in sim_yGrossBuLevelYear {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen


* Plot figure
twoway ///
	(rarea sim_yGrossBuLevelYear_high sim_yGrossBuLevelYear_low year, ///
		sort color(green%20) legend(label(1 "SimPaths"))) ///
	(line valid_yGrossBuLevelYear year, sort color(green) ///
		legend(label(2 "UKHLS"))), ///
	title("Benefit Unity Gross Income") ///
	subtitle("") ///
	xtitle("Year", size(small)) ///
	ytitle("GBP per year", size(small)) ///
	ylabel(,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: Series represents average benefit unit gross income through time. Gross income is the sum of captial income, private" "pension income and employment income. One observation per benefit unit plotted. Amounts in 2015 prices. Top and" "bottom percentiles trimmed.", ///
	size(vsmall))

graph export ///
"$dir_output_files/income/gross_income/validation_${country}_gross_income_bu_ts.jpg", ///
	replace width(2400) height(1350) quality(100)
	
	
********************************************************************************
* 1.2 : Mean values over time, individual level amounts
********************************************************************************	
	
* Prepare validation data 
use year demAge dwt valid_yGrossPersLevelYear using ///
	"$dir_data/ukhls_validation_sample.dta", clear

keep if inrange(demAge,18,65)		

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_yGrossPersLevelYear, d
	
	replace valid_yGrossPersLevelYear = . if ///
		valid_yGrossPersLevelYear < r(p1) | ///
		valid_yGrossPersLevelYear > r(p99)

}

collapse (mean) valid_yGrossPersLevelYear [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year demAge sim_yGrossPersLevelYear using ///
	"$dir_data/simulation_sample.dta", clear

keep if inrange(demAge,18,65)	
	
* Trim outliers
if "$trim_outliers" == "true" {
	
	sum sim_yGrossPersLevelYear, d
	
	replace sim_yGrossPersLevelYear = . if ///
		sim_yGrossPersLevelYear < r(p1) | ///
		sim_yGrossPersLevelYear > r(p99)

}

collapse (mean) sim_yGrossPersLevelYear, by(run year)

collapse (mean) sim_yGrossPersLevelYear ///
		 (sd) sim_yGrossPersLevelYear_sd = sim_yGrossPersLevelYear, ///
		 by(year)
		 
foreach varname in sim_yGrossPersLevelYear{
	
	gen `varname'_hi = `varname' + 1.96*`varname'_sd
	gen `varname'_lo = `varname' - 1.96*`varname'_sd

}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway ///
(rarea sim_yGrossPersLevelYear_hi sim_yGrossPersLevelYear_lo ///
	year, sort color(green%20) ///
	legend(label(1 "SimPaths"))) ///
(line valid_yGrossPersLevelYear year, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	title("Individual Gross Income") ///
	subtitle("Ages 18-65") ///
	xtitle("Year", size(small)) ///
	ytitle("GBP per year", size(small)) ///
	ylabel(,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: Series represents average individual gross income through time. Gross income is the sum of captial income, private pension" "income and employment income. Values in 2015 prices. Top and bottom percentiles trimmed.", ///
	size(vsmall))
		
graph export ///
"$dir_output_files/income/gross_income/validation_${country}_ind_gross_income_ts_18_65_both.jpg", ///
	replace width(2400) height(1350) quality(100)

	
********************************************************************************
* 1.2.1 : Mean values over time, individual level amounts, by gender
********************************************************************************	

* Prepare validation data 
use year demAge dwt valid_yGrossPersLevelYear demMaleFlag using ///
	"$dir_data/ukhls_validation_sample.dta", clear

keep if inrange(demAge,18,65)		
keep if demMaleFlag == 1	

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_yGrossPersLevelYear, d
	
	replace valid_yGrossPersLevelYear = . if ///
		valid_yGrossPersLevelYear < r(p1) | ///
		valid_yGrossPersLevelYear > r(p99)
		
}

collapse (mean) valid_yGrossPersLevelYear [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year demAge sim_yGrossPersLevelYear demMaleFlag using ///
	"$dir_data/simulation_sample.dta", clear

keep if inrange(demAge,18,65)		
keep if demMaleFlag == 1

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum sim_yGrossPersLevelYear, d
	
	replace sim_yGrossPersLevelYear = . if ///
		sim_yGrossPersLevelYear < r(p1) | ///
		sim_yGrossPersLevelYear > r(p99)
		
}

collapse (mean) sim_yGrossPersLevelYear, by(run year)

collapse (mean) sim_yGrossPersLevelYear ///
		 (sd) sim_yGrossPersLevelYear_sd = ///
		 sim_yGrossPersLevelYear, by(year)
		 
foreach varname in sim_yGrossPersLevelYear {
	
	gen `varname'_hi = `varname' + 1.96*`varname'_sd
	gen `varname'_lo = `varname' - 1.96*`varname'_sd

}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway ///
(rarea sim_yGrossPersLevelYear_hi sim_yGrossPersLevelYear_lo ///
	year, sort color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_yGrossPersLevelYear year, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	title("Individual Gross Income") ///
	subtitle("Ages 18-65, males") ///
	xtitle("Year", size(small)) ///
	ytitle("GBP per year", size(small)) ///
	ylabel(,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: Series represents average individual gross income through time. Gross income is the sum of captial income, private pension" "income and employment income. Values in 2015 prices. Top and bottom percentiles trimmed.", ///
	size(vsmall))
		
graph export ///
"$dir_output_files/income/gross_income/validation_${country}_ind_gross_income_ts_18_65_male.jpg", ///
	replace width(2400) height(1350) quality(100)

	
* Female 
* Prepare validation data 
use year demAge dwt valid_yGrossPersLevelYear demMaleFlag using ///
	"$dir_data/ukhls_validation_sample.dta", clear

keep if inrange(demAge,18,65)
	
keep if demMaleFlag == 0	

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_yGrossPersLevelYear, d
	
	replace valid_yGrossPersLevelYear = . if ///
		valid_yGrossPersLevelYear < r(p1) | ///
		valid_yGrossPersLevelYear > r(p99)
		
}

collapse (mean) valid_yGrossPersLevelYear [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year demAge sim_yGrossPersLevelYear demMaleFlag using ///
	"$dir_data/simulation_sample.dta", clear

keep if inrange(demAge,18,65)	

keep if demMaleFlag == 0

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum sim_yGrossPersLevelYear, d
	
	replace sim_yGrossPersLevelYear= . if ///
		sim_yGrossPersLevelYear< r(p1) | ///
		sim_yGrossPersLevelYear> r(p99)
		
}

collapse (mean) sim_yGrossPersLevelYear, by(run year)

collapse (mean) sim_yGrossPersLevelYear ///
		 (sd) sim_yGrossPersLevelYear_sd = ///
		 sim_yGrossPersLevelYear, by(year)
		 
foreach varname in sim_yGrossPersLevelYear{
	
	gen `varname'_hi = `varname' + 1.96*`varname'_sd
	gen `varname'_lo = `varname' - 1.96*`varname'_sd
	
}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway ///
(rarea sim_yGrossPersLevelYear_hi ///
	sim_yGrossPersLevelYear_lo year, sort color(green%20) ///
	legend(label(1 "SimPaths"))) ///
(line valid_yGrossPersLevelYear year, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	title("Individual Gross Income") ///
	subtitle("Ages 18-65, females") ///
	xtitle("Year", size(small)) ///
	ytitle("GBP per year", size(small)) ///
	ylabel(,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: Series represents average individual gross through time. Gross income is the sum of captial income, private pension" "income and employment income. Values in 2015 prices. Top and bottom percentiles trimmed.", ///
	size(vsmall))
		
graph export ///
"$dir_output_files/income/gross_income/validation_${country}_ind_gross_income_ts_18_65_female.jpg", ///
	replace width(2400) height(1350) quality(100)	
	
graph drop _all 	
	

/*******************************************************************************
* 2 : Histograms 
*******************************************************************************/

/*******************************************************************************
* 2.1 : Histograms - Benefit unit gross income by year, and by category of  
weekly labour supply 
*******************************************************************************/

* Prepare validation data 
use year demAge dwt valid_yGrossBuLevelYear valid_labHrsWorkEnumWeek ///
	using "$dir_data/ukhls_validation_sample.dta", clear

keep if inrange(demAge,18,65)	
	
* Trim outliers
if "$trim_outliers" == "true" {	
	
	sum valid_yGrossBuLevelYear, d

	replace valid_yGrossBuLevelYear = . if ///
		valid_yGrossBuLevelYear < r(p1) | ///
		valid_yGrossBuLevelYear > r(p99)		

}

* Prepare info needed for dynamic y axis labels 
qui sum year
local min_year = 2011
local max_year = 2023

forval year = `min_year'/`max_year' {

	twoway__histogram_gen valid_yGrossBuLevelYear if year == `year', ///
		width(2500) den gen(d_valid v2)

	qui sum d_valid
	gen max_d_valid_`year' = r(max) 
	
	drop d_valid v2

	foreach ls in $ls_cat {
	
		twoway__histogram_gen valid_yGrossBuLevelYear if ///
			year == `year' & valid_labHrsWorkEnumWeek == "`ls'", width(2500) ///
			den gen(d_valid v2)

		qui sum d_valid
		gen max_d_valid_`year'_`ls' = r(max) 
		
		drop d_valid v2	
	
	}
	
}

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year demAge sim_yGrossBuLevelYear sim_labHrsWorkEnumWeek using ///
	"$dir_data/simulation_sample.dta", clear

keep if inrange(demAge,18,65)	
	
* Trim outliers
if "$trim_outliers" == "true" {
	
	sum sim_yGrossBuLevelYear, d
	
	replace sim_yGrossBuLevelYear = . if ///
		sim_yGrossBuLevelYear < r(p1) | sim_yGrossBuLevelYear > r(p99)

}

keep if run == 1

append using "$dir_data/temp_valid_stats.dta"

* Plot sub-figures 
qui sum year
local min_year = 2011  
local max_year = 2023 

//local year = 2010

forval year = `min_year'/`max_year' {
	
	* Prepare info needed for dynamic y axis labels
	twoway__histogram_gen sim_yGrossBuLevelYear if year == `year', ///
		width(2500) den gen(d_sim v1)

	qui sum d_sim 
	gen max_d_sim = r(max)

	gen max_value = max_d_valid_`year' if max_d_valid_`year' > max_d_sim 
	replace max_value = max_d_sim if max_value == . 

	sum max_value 
	local max_y = 1.25*r(max)
	local steps = `max_y'/3	

	
	twoway (hist sim_yGrossBuLevelYear if year == `year', width(2500) ///
		color(green%30) legend(label(1 "SimPaths"))) ///
	(hist valid_yGrossBuLevelYear if year == `year', width(2500) ///
		color(red%30) legend(label(2 "UKHLS"))) , ///
		title("ALL hours") ///
		name(gross_inc_`year'_all, replace) ///
		xtitle("GBP", size(small)) ///
		ytitle("Density", size(small)) ///
		xlabel(,labsize(vsmall) angle(forty_five)) ///
		ylabel(0(`steps')`max_y',labsize(vsmall)) ///
		graphregion(color(white))		

	drop d_sim v1 max_d_sim max_value
	
	foreach ls in $ls_cat {
	
		* Prepare info needed for dynamic y axis labels 	
		twoway__histogram_gen sim_yGrossBuLevelYear if ///
			year == `year' & sim_labHrsWorkEnumWeek == "`ls'", width(2500) ///
			den gen(d_sim v1)
		
		qui sum d_sim 
		gen max_d_sim = r(max)

		gen max_value = max_d_valid_`year'_`ls' if ///
			max_d_valid_`year'_`ls' > max_d_sim 
		replace max_value = max_d_sim if max_value == . 

		sum max_value 
		local max_y = 1.25*r(max)
		local steps = `max_y'/3	
	
		* Plot by weekly hours work
		twoway (hist sim_yGrossBuLevelYear if year == `year' & ///
			sim_labHrsWorkEnumWeek == "`ls'", width(2500) color(green%30) ///
			legend(label(1 "SimPaths"))) ///
		(hist valid_yGrossBuLevelYear if year == `year' & ///
			valid_labHrsWorkEnumWeek == "`ls'", width(2500) color(red%30) ///
			legend(label(2 "UKHLS"))) , ///
			title("`ls' hours") ///
			name(gross_inc_`year'_`ls', replace) ///
			xtitle("GBP", size(small)) ///
			ytitle("Density", size(small)) ///
			xlabel(,labsize(vsmall) angle(forty_five)) ///
			ylabel(0(`steps')`max_y',labsize(vsmall)) ///
			legend(size(small)) ///
			graphregion(color(white)) 
		
		drop d_sim v1 max_d_sim max_value
		
	}
}

* Combine plots by year 
qui sum year
local min_year = 2011
local max_year = 2023 

forvalues year = `min_year'/`max_year' {
	
	grc1leg gross_inc_`year'_all ///
		gross_inc_`year'_ZERO ///
		gross_inc_`year'_TEN , ///
		title("Benefit Unit Gross Income by Weekly Hours of Work") ///
		subtitle("`year'") ///
		legendfrom(gross_inc_`year'_ZERO) rows(1) ///
		graphregion(color(white)) ///
		note("Notes: Amounts in GBP per year, 2015 prices. Gross income is the sum of capital income, private pension income and employment income." "Individual observations of benefit unit amounts plotted.", ///
		size(vsmall)) 

	graph export "$dir_output_files/income/gross_income/validation_${country}_gross_income_bu_dist_`year'_18_65_1.png", ///
		replace width(2400) height(1350) 
		
	grc1leg ///
		gross_inc_`year'_TWENTY ///
		gross_inc_`year'_THIRTY ///
		gross_inc_`year'_THIRTY_EIGHT ///
		gross_inc_`year'_FORTY_FIVE ///		
		gross_inc_`year'_FIFTY_FIVE, ///
		title("Benefit Unit Gross Income by Weekly Hours of Work") ///
		subtitle("`year'") ///
		legendfrom(gross_inc_`year'_TWENTY) rows(2) ///
		graphregion(color(white)) ///
		note("Notes: Amounts in GBP per year, 2015 prices. Gross income is the sum of capital income, private pension income and employment income." "Individual observations of benefit unit amounts plotted.", ///
		size(vsmall)) 

	graph export "$dir_output_files/income/gross_income/validation_${country}_gross_income_bu_dist_`year'_18_65_2.png", ///
		replace width(2400) height(1350) 		
		
}

graph drop _all


/*******************************************************************************
* 2.2 : Histograms - Individual gross income by year, and by category of weekly 
labour supply, by gender
*******************************************************************************/

* Males 

* Prepare validation data 
use year demAge dwt valid_yGrossBuLevelYear valid_labHrsWorkEnumWeek ///
	demMaleFlag using "$dir_data/ukhls_validation_sample.dta", clear

keep if inrange(demAge,18,65)		
	
keep if demMaleFlag == 1 
drop demMaleFlag 			
	
* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_yGrossBuLevelYear, d
	
	replace valid_yGrossBuLevelYear = . if ///
		valid_yGrossBuLevelYear < r(p1) | ///
		valid_yGrossBuLevelYear > r(p99)

}


* Prepare info needed for dynamic y axis labels 
qui sum year
local min_year = 2011   
local max_year = r(max) 

forval year = `min_year'/`max_year' { 

	twoway__histogram_gen valid_yGrossBuLevelYear if ///
		year == `year', width(2500) den gen(d_valid v2)

	qui sum d_valid
	gen max_d_valid_`year' = r(max) 
	
	drop d_valid v2

	foreach ls in $ls_cat {
	
		twoway__histogram_gen valid_yGrossBuLevelYear if ///
			year == `year' & valid_labHrsWorkEnumWeek == "`ls'", width(2500) ///
			den gen(d_valid v2)

		qui sum d_valid
		gen max_d_valid_`year'_`ls' = r(max) 
		
		drop d_valid v2	
	
	}
}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year demAge sim_yGrossBuLevelYear sim_labHrsWorkEnumWeek ///
	demMaleFlag using "$dir_data/simulation_sample.dta", clear

keep if inrange(demAge,18,65)		
	
keep if demMaleFlag == 1
drop demMaleFlag	
	
* Trim outliers
if "$trim_outliers" == "true" {
	
	sum sim_yGrossBuLevelYear, d
	
	replace sim_yGrossBuLevelYear= . if ///
		sim_yGrossBuLevelYear < r(p1) | ///
		sim_yGrossBuLevelYear > r(p99)

}

keep if run == 1

append using "$dir_data/temp_valid_stats.dta"

* Plot sub-figures 
qui sum year
local min_year = 2011
local max_year = 2023

forval year =  `min_year'/`max_year' { 

	* Prepare info needed for dynamic y axis labels 
	twoway__histogram_gen sim_yGrossBuLevelYear if year == `year', ///
		width(2500) den gen(d_sim v1)

	qui sum d_sim 
	gen max_d_sim = r(max)

	gen max_value = max_d_valid_`year' if max_d_valid_`year' > max_d_sim 
	replace max_value = max_d_sim if max_value == . 

	sum max_value 
	local max_y = 1.25*r(max)
	local steps = `max_y'/3	
	
	* Plot all hours
	twoway (hist sim_yGrossBuLevelYear if year == `year', width(2500) ///
		color(green%30) legend(label(1 "SimPaths"))) ///
	(hist valid_yGrossBuLevelYear if year == `year', width(2500) ///
		color(red%30) legend(label(2 "UKHLS"))) , ///
		title("ALL hours") ///
		name(ind_gross_inc_`year'_all, replace) ///
		xtitle("GBP", size(small)) ///
		ytitle("Density", size(small)) ///
		xlabel(,labsize(vsmall) angle(forty_five)) ///
		ylabel(0(`steps')`max_y', labsize(vsmall)) ///
		graphregion(color(white)) 
	
	drop d_sim v1 max_d_sim max_value
		
	foreach ls in $ls_cat {
	
		* Prepare info needed for dynamic y axis labels 
		twoway__histogram_gen sim_yGrossBuLevelYear if ///
			year == `year' & sim_labHrsWorkEnumWeek == "`ls'", width(2500) ///
			den gen(d_sim v1)

		qui sum d_sim 
		gen max_d_sim = r(max)

		gen max_value = max_d_valid_`year'_`ls' if ///
			max_d_valid_`year'_`ls' > max_d_sim 
		replace max_value = max_d_sim if max_value == . 

		sum max_value 
		local max_y = 1.25*r(max)
		local steps = `max_y'/3	
		
		* Plot by weekly hours work
		twoway (hist sim_yGrossBuLevelYear if year == `year' & ///
			sim_labHrsWorkEnumWeek == "`ls'", width(2500) color(green%30) ///
			legend(label(1 "SimPaths"))) ///
		(hist valid_yGrossBuLevelYear if year == `year' & ///
			valid_labHrsWorkEnumWeek == "`ls'", width(2500) color(red%30) ///
			legend(label(2 "UKHLS"))) , ///
			title("`ls' hours")  ///
			name(ind_gross_inc_`year'_`ls', replace) ///
			xtitle("GBP", size(small)) ///
			ytitle("Density", size(small)) ///
			xlabel(,labsize(vsmall) angle(forty_five)) ///
			ylabel(0(`steps')`max_y', labsize(vsmall)) ///
			legend(size(small)) ///
			graphregion(color(white)) 
		
		drop d_sim v1 max_d_sim max_value

	}
}

* Combine plots by year 
qui sum year
local min_year = 2011   
local max_year = 2023 

forvalues year = `min_year'/`max_year' {
	
	grc1leg ind_gross_inc_`year'_all ///
		ind_gross_inc_`year'_ZERO  ///
		ind_gross_inc_`year'_TEN , ///
		title("Individual Gross Income by Weekly Hours of Work") ///
		subtitle("`year', males") ///
		legendfrom(ind_gross_inc_`year'_ZERO) ///
		rows(1) ///
		graphregion(color(white)) ///
		note("Notes: Amounts in GBP per year, 2015 prices. Sample include males age 18-65. Top and bottom percentiles trimmed. Gross income is" "the sum of capital income, private pension income and employment income.", ///
		size(vsmall)) 
			
	graph export "$dir_output_files/income/gross_income/validation_${country}_ind_gross_income_dist_`year'_male_1.png", ///
		replace width(2400) height(1350) 
		
	grc1leg ///
		ind_gross_inc_`year'_TWENTY ///
		ind_gross_inc_`year'_THIRTY ///
		ind_gross_inc_`year'_THIRTY_EIGHT ///
		ind_gross_inc_`year'_FORTY_FIVE ///		
		ind_gross_inc_`year'_FIFTY_FIVE, ///
		title("Individual Gross Income by Weekly Hours of Work") ///
		subtitle("`year', males") ///
		legendfrom(ind_gross_inc_`year'_TWENTY) ///
		rows(2) ///
		graphregion(color(white)) ///
		note("Notes: Amounts in GBP per year, 2015 prices. Sample include males age 18-65. Top and bottom percentiles trimmed. Gross income is" "the sum of capital income, private pension income and employment income.", ///
		size(vsmall)) 
			
	graph export "$dir_output_files/income/gross_income/validation_${country}_ind_gross_income_dist_`year'_male_2.png", ///
		replace width(2400) height(1350) 		
		
}
	
graph drop _all


* Females 

* Prepare validation data 
use year demAge dwt valid_yGrossBuLevelYear valid_labHrsWorkEnumWeek ///
	demMaleFlag using "$dir_data/ukhls_validation_sample.dta", clear

keep if inrange(demAge,18,65)	
	
keep if demMaleFlag == 0
drop demMaleFlag 			
	
* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_yGrossBuLevelYear, d
	
	replace valid_yGrossBuLevelYear = . if ///
		valid_yGrossBuLevelYear < r(p1) | ///
		valid_yGrossBuLevelYear > r(p99)
		
}


* Prepare info needed for dynamic y axis labels 
qui sum year
local min_year = 2011
local max_year = 2023 

forval year = `min_year'/`max_year' { 

	twoway__histogram_gen valid_yGrossBuLevelYear if ///
		year == `year' , width(2500) den gen(d_valid v2)

	qui sum d_valid
	gen max_d_valid_`year' = r(max) 
	
	drop d_valid v2

	foreach ls in $ls_cat {
	
		twoway__histogram_gen valid_yGrossBuLevelYear if ///
			year == `year' & valid_labHrsWorkEnumWeek == "`ls'", width(2500) ///
			den gen(d_valid v2)

		qui sum d_valid
		gen max_d_valid_`year'_`ls' = r(max) 
		
		drop d_valid v2	
	
	}
}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year demAge sim_yGrossBuLevelYear sim_labHrsWorkEnumWeek ///
	demMaleFlag using "$dir_data/simulation_sample.dta", clear

keep if inrange(demAge,18,65)		
	
keep if demMaleFlag == 0
drop demMaleFlag	
	
* Trim outliers
if "$trim_outliers" == "true" {
	
	sum sim_yGrossBuLevelYear, d
	
	replace sim_yGrossBuLevelYear = . if sim_yGrossBuLevelYear< r(p1) | ///
		sim_yGrossBuLevelYear> r(p99)

}

keep if run == 1


append using "$dir_data/temp_valid_stats.dta"

* Plot sub-figures 
qui sum year
local min_year = 2011
local max_year = 2023 

forval year = `min_year'/`max_year' { 

	* Prepare info needed for dynamic y axis labels 
	twoway__histogram_gen sim_yGrossBuLevelYear if year == `year', ///
		width(2500) den gen(d_sim v1)

	qui sum d_sim 
	gen max_d_sim = r(max)

	gen max_value = max_d_valid_`year' if max_d_valid_`year' > max_d_sim 
	replace max_value = max_d_sim if max_value == . 

	sum max_value 
	local max_y = 1.25*r(max)
	local steps = `max_y'/3	
	
	* Plot all hours
	twoway (hist sim_yGrossBuLevelYear if year == `year', width(2500) ///
		color(green%30) legend(label(1 "SimPaths"))) ///
	(hist valid_yGrossBuLevelYear if year == `year', width(2500) ///
		color(red%30) legend(label(2 "UKHLS"))) , ///
		title("ALL hours") ///
		name(ind_gross_inc_`year'_all, replace) ///
		xtitle("GBP", size(small)) ///
		ytitle("Density", size(small)) ///
		xlabel(,labsize(vsmall) angle(forty_five)) ///
		ylabel(0(`steps')`max_y', labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white)) 
	
	drop d_sim v1 max_d_sim max_value
	
	foreach ls in $ls_cat {
	
		* Prepare info needed for dynamic y axis labels 
		twoway__histogram_gen sim_yGrossBuLevelYear if ///
			year == `year' & sim_labHrsWorkEnumWeek == "`ls'", ///
			width(2500) den gen(d_sim v1)

		qui sum d_sim 
		gen max_d_sim = r(max)

		gen max_value = max_d_valid_`year'_`ls' if ///
			max_d_valid_`year'_`ls' > max_d_sim 
		replace max_value = max_d_sim if max_value == . 

		sum max_value 
		local max_y = 1.25*r(max)
		local steps = `max_y'/3	
		
		* Plot by weekly hours work
		twoway (hist sim_yGrossBuLevelYear if year == `year' & ///
			sim_labHrsWorkEnumWeek == "`ls'", width(2500) ///
			color(green%30) legend(label(1 "SimPaths"))) ///
		(hist valid_yGrossBuLevelYear if year == `year' & ///
			valid_labHrsWorkEnumWeek == "`ls'", width(2500) color(red%30) ///
			legend(label(2 "UKHLS"))) , ///
			title("`ls' hours")  ///
			name(ind_gross_inc_`year'_`ls', replace) ///
			xtitle("GBP", size(small)) ///
			ytitle("Density", size(small)) ///
			xlabel(,labsize(vsmall) angle(forty_five)) ///
			ylabel(0(`steps')`max_y', labsize(vsmall)) ///
			legend(size(small)) ///
			graphregion(color(white)) 
		
		drop d_sim v1 max_d_sim max_value

	}
}

* Combine plots by year 
qui sum year
local min_year = 2011  
local max_year = 2023 

forvalues year = `min_year'/`max_year' {
	
	grc1leg ind_gross_inc_`year'_all ///
		ind_gross_inc_`year'_ZERO  ///
		ind_gross_inc_`year'_TEN , ///
		title("Individual Gross Income by Weekly Hours of Work") ///
		subtitle("`year', females") ///
		legendfrom(ind_gross_inc_`year'_ZERO) ///
		rows(1) ///
		graphregion(color(white)) ///
		note("Notes: Amounts in GBP per year, 2015 prices. Sample include females age 18-65. Top and bottom percentiles trimmed. Gross income is" "the sum of capital income, private pension income and employment income.", ///
		size(vsmall)) 
			
	graph export "$dir_output_files/income/gross_income/validation_${country}_ind_gross_income_dist_`year'_female_1.png", ///
		replace width(2400) height(1350) 
		
		
	grc1leg  ///
		ind_gross_inc_`year'_TWENTY ///
		ind_gross_inc_`year'_THIRTY ///
		ind_gross_inc_`year'_THIRTY_EIGHT ///
		ind_gross_inc_`year'_FORTY_FIVE ///		
		ind_gross_inc_`year'_FIFTY_FIVE, ///
		title("Individual Gross Income by Weekly Hours of Work") ///
		subtitle("`year', females") ///
		legendfrom(ind_gross_inc_`year'_TWENTY) ///
		rows(2) ///
		graphregion(color(white)) ///
		note("Notes: Amounts in GBP per year, 2015 prices. Sample include females age 18-65. Top and bottom percentiles trimmed. Gross income is" "the sum of capital income, private pension income and employment income.", ///
		size(vsmall)) 
			
	graph export "$dir_output_files/income/gross_income/validation_${country}_ind_gross_income_dist_`year'_female_2.png", ///
		replace width(2400) height(1350) 		
		
}
	
graph drop _all



/*

* Investigation into who the people are with high working hours and low gross 
* income 
/*
Note plot ben unit observations using individual level data. 

Components of gross income. 

Gross personal income components 
• PY010G - Gross employee cash or near cash employee income 
• PY050G - Gross cash benefits or losses from self-employment 
			(including royalties) 
• PY080G - Pensions received from individual private plans (other than those 
			covered under ESSPROS)
 
Plus gross income components at household level 
• HY040G - Income from rental of a property or land 
• HY080G - Regular inter-household cash transfers received 
• HY090G - Interests, dividends, profit from capital investments in 
			unincorporated business 
• HY110G - Income received by people aged under 16 
*/

* Explore 2018 FIFTY hours 
use "$dir_data/ukhls_validation_full_sample.dta", clear

keep if year == 2018 & labHrsWorkEnumWeek == "FIFTY" 

order idperson idbenefit lhw valid_yGrossBuLevelYear ///
	y_gross_labour_person valid_wage_hour ///
	py010g* py050g py080g ///
	hy080g_pc hy110g_pc hy040g_pc hy090g_pc	missing*
	
fre missing_py010g missing_py050g missing_py080g missing_hy080g ///
	missing_hy110g missing_hy040g missing_hy090g missing_lhw if ///
	valid_yGrossBuLevelYear == 0 	// none missing seems to be in the data 
	
	
