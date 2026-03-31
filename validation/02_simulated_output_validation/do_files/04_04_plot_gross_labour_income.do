********************************************************************************
* PROJECT:  		SimPaths UK 
* SECTION:			Validation
* OBJECT: 			Gross labour income 
* AUTHORS:			Ashley Burdett 
* LAST UPDATE:		Feb 2026
* COUNTRY: 			UK 
********************************************************************************
* NOTES: 			Plotted using individual level data 
* 						=> multiple observations per ben unit.
********************************************************************************

********************************************************************************
* 1 : Mean labour income
********************************************************************************

********************************************************************************
* 1.1: Mean labour income - benefit unit
********************************************************************************

* Prepare validation data
use year idBu idPers demAge dwt labC4 valid_yEmpBuGrossLevelYear using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Keep only employed individuals
keep if labC4 == 1

keep if demAge >= 16

* Keep one observatioon per benefit unit
bysort year idBu: gen first_person = (_n == 1)
keep if first_person == 1

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_yEmpBuGrossLevelYear, d
	
	replace valid_yEmpBuGrossLevelYear = . if ///
		valid_yEmpBuGrossLevelYear < r(p1) | valid_yEmpBuGrossLevelYear > r(p99)
		
}

collapse (mean) valid_yEmpBuGrossLevelYear [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace


* Prepare SimPaths data
use run year idPers idBu demAge labC4 sim_yEmpBuGrossLevelYear using ///
	"$dir_data/simulation_sample.dta", clear

* Keep only employed individuals
keep if labC4 == "EmployedOrSelfEmployed"

keep if demAge >= 16

* Keep one observatioon per benefit unit
bysort run year idBu: gen first_person = (_n == 1)
keep if first_person == 1

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum sim_yEmpBuGrossLevelYear, d
	
	replace sim_yEmpBuGrossLevelYear = . if ///
		sim_yEmpBuGrossLevelYear < r(p1) | sim_yEmpBuGrossLevelYear > r(p99)

}

collapse (mean) sim_yEmpBuGrossLevelYear, by(run year)

collapse (mean) sim_yEmpBuGrossLevelYear ///
		 (sd) sim_yEmpBuGrossLevelYear_sd = sim_yEmpBuGrossLevelYear ///
		 , by(year)
		 
foreach varname in sim_yEmpBuGrossLevelYear {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway ///
(rarea sim_yEmpBuGrossLevelYear_high sim_yEmpBuGrossLevelYear_low year, ///
	sort color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_yEmpBuGrossLevelYear year, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	title("Benefit Unit Gross Labour Income") ///
	subtitle("") ///
	xtitle("Year", size(small)) ///
	ytitle("GBP per year", size(small)) ///
	ylabel(,labsize(small)) ///
	xlabel(,labsize(small)) ///
	graphregion(color(white)) ///
	legend(size(small)) ///
	note("Note: Amounts in 2015 prices. Top and bottom percentiles trimmed.", ///
	size(vsmall))

* Save figure
graph export ///	
"$dir_output_files/income/gross_labour_income/validation_${country}_gross_labour_income_bu_ts.jpg", ///
	replace width(2400) height(1350) quality(100)
	
	
********************************************************************************
* 1.1: Mean labour income - individual 
********************************************************************************

* Prepare validation data
use year demAge dwt labC4 valid_yEmpPersGrossLevelYear using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Keep only employed individuals
keep if labC4 == 1

keep if inrange(demAge,18,65)

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_yEmpPersGrossLevelYear, d
	
	replace valid_yEmpPersGrossLevelYear = . if ///
		valid_yEmpPersGrossLevelYear < r(p1) | ///
		valid_yEmpPersGrossLevelYear > r(p99)
		
}

collapse (mean) valid_yEmpPersGrossLevelYear [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace


* Prepare SimPaths data
use run year demAge labC4 sim_yEmpPersGrossLevelYear using ///
	"$dir_data/simulation_sample.dta", clear

* Keep only employed individuals
keep if labC4 == "EmployedOrSelfEmployed"

keep if inrange(demAge,18,65)

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum sim_yEmpPersGrossLevelYear, d
	
	replace sim_yEmpPersGrossLevelYear = . if ///
		sim_yEmpPersGrossLevelYear < r(p1) | sim_yEmpPersGrossLevelYear > r(p99)

}

collapse (mean) sim_yEmpPersGrossLevelYear, by(run year)

collapse (mean) sim_yEmpPersGrossLevelYear ///
		 (sd) sim_yEmpPersGrossLevelYear_sd = sim_yEmpPersGrossLevelYear ///
		 , by(year)
		 
foreach varname in sim_yEmpPersGrossLevelYear {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway ///
(rarea sim_yEmpPersGrossLevelYear_high sim_yEmpPersGrossLevelYear_low year, ///
	sort color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_yEmpPersGrossLevelYear year, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	title("Gross Labour Income") ///
	subtitle("Ages 18-65") ///
	xtitle("Year", size(small)) ///
	ytitle("GBP per year", size(small)) ///
	ylabel(,labsize(small)) ///
	xlabel(,labsize(small)) ///
	graphregion(color(white)) ///
	legend(size(small)) ///
	note("Note: Amounts at the individual level, individual data plotted. Statistics calculated on the sample of employed individuals" "ages 18-65. Amounts in 2015 prices. Top and bottom percentiles trimmed.", ///
	size(vsmall))

* Save figure
graph export ///	
"$dir_output_files/income/gross_labour_income/validation_${country}_ind_gross_labour_income_ts_18_65.jpg", ///
	replace width(2400) height(1350) quality(100)
	
	
********************************************************************************
* 2 : Histograms 
********************************************************************************

********************************************************************************
* 2.1 : Histograms - working age, benefit unit 
********************************************************************************

* Prepare validation data
use year idPers idBu demAge dwt labC4 valid_yEmpBuGrossLevelYear ///
	valid_labHrsWorkEnumWeek  using ///
	"$dir_data/ukhls_validation_sample.dta", clear
	
* Keep only employed individuals
keep if labC4 == 1
drop labC4

keep if inrange(demAge,18,65)

* Keep one observatioon per benefit unit
bysort year idBu: gen first_person = (_n == 1)
keep if first_person == 1

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_yEmpBuGrossLevelYear, d
	
	replace valid_yEmpBuGrossLevelYear = . if ///
		valid_yEmpBuGrossLevelYear < r(p1) | valid_yEmpBuGrossLevelYear > r(p99)

}

* Prepare info needed for dynamic y axis labels 
qui sum year
local min_year = 2011 
local max_year = 2023 

forval year = `min_year'/`max_year' { 

	twoway__histogram_gen valid_yEmpBuGrossLevelYear if year == `year', ///
		width(2500) den gen(d_valid v2)

	qui sum d_valid
	gen max_d_valid_`year' = r(max) 
	
	drop d_valid v2

	foreach ls in $ls_cat {
	
		twoway__histogram_gen valid_yEmpBuGrossLevelYear if ///
			year == `year' & valid_labHrsWorkEnumWeek == "`ls'", width(2500) ///
			den gen(d_valid v2)

		qui sum d_valid
		gen max_d_valid_`year'_`ls' = r(max) 
		
		drop d_valid v2	
	
	}
	
}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare SimPaths data
use run year idPers idBu demAge labC4 sim_yEmpBuGrossLevelYear ///
	sim_labHrsWorkEnumWeek using "$dir_data/simulation_sample.dta", clear	
	
* Keep only employed individuals
keep if labC4 == "EmployedOrSelfEmployed"
drop labC4

keep if inrange(demAge,18,65)

* Keep one observatioon per benefit unit
bysort run year idBu: gen first_person = (_n == 1)
keep if first_person == 1

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum sim_yEmpBuGrossLevelYear, d
	
	replace sim_yEmpBuGrossLevelYear = . if ///
		sim_yEmpBuGrossLevelYear < r(p1) | sim_yEmpBuGrossLevelYear > r(p99)

		}

keep if run == 1

append using "$dir_data/temp_valid_stats.dta"

* Plot sub-figures 
qui sum year
local min_year = 2011
local max_year = 2023  

forval year = `min_year'/`max_year' {
	
	* Prepare info needed for dynamic y axis labels
	twoway__histogram_gen sim_yEmpBuGrossLevelYear if year == `year', ///
		width(2500) den gen(d_sim v1)

	qui sum d_sim 
	gen max_d_sim = r(max)

	gen max_value = max_d_valid_`year' if max_d_valid_`year' > max_d_sim 
	replace max_value = max_d_sim if max_value == . 

	sum max_value 
	local max_y = 1.25*r(max)
	local steps = `max_y'/2	
	
	
    * Plot all hours
	twoway (hist sim_yEmpBuGrossLevelYear if year == `year' , width(2500) ///
		color(green%30) legend(label(1 "SimPaths"))) ///
	(hist valid_yEmpBuGrossLevelYear if year == `year' , width(2500) ///
		color(red%30) legend(label(2 "UKHLS"))) , ///
		subtitle("ALL hours") ///
		name(gross_lab_inc_`year'_all, replace) ///
		xtitle("GBP", size(small)) ///
		ytitle("Density", size(small)) ///
		xlabel(,labsize(vsmall) angle(forty_five)) ///
		ylabel(0(`steps')`max_y',labsize(vsmall)) ///
		graphregion(color(white)) 

	
	drop d_sim v1 max_d_sim max_value
	
	* Plot by weekly hours work
	foreach ls in $ls_cat_labour {
	
		* Prepare info needed for dynamic y axis labels 	
		twoway__histogram_gen sim_yEmpBuGrossLevelYear if year == `year' & ///
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
		twoway (hist sim_yEmpBuGrossLevelYear if year == `year' & ///
			sim_labHrsWorkEnumWeek == "`ls'", width(2500) color(green%30) ///
			legend(label(1 "SimPaths"))) ///
		(hist valid_yEmpBuGrossLevelYear if year == `year' & ///
			valid_labHrsWorkEnumWeek == "`ls'", width(2500) color(red%30) ///
			legend(label(2 "UKHLS"))) , ///
			subtitle("`ls' hours")  ///
			name(gross_lab_inc_`year'_`ls', replace) ///
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
forvalues year = 2011/2023 {
	
	grc1leg gross_lab_inc_`year'_all ///  
		gross_lab_inc_`year'_TEN  ///
		gross_lab_inc_`year'_TWENTY, ///
		title("Benefit Unit Gross Labour Income by Weekly Hours of Work") ///
		subtitle("`year'") ///
		legendfrom(gross_lab_inc_`year'_TEN) rows(1) ///
		graphregion(color(white)) ///
		note("Notes: Amounts in GBP per year, 2015 prices. Top and bottom percentiles trimmed. Individual observations of benefit unit amount plotted",  ///
		size(vsmall)) 
		
	graph export "$dir_output_files/income/gross_labour_income/validation_${country}_gross_labour_income_bu_dist_`year'_18_65_1.png", ///
		replace width(2400) height(1350) 
		
		
	grc1leg ///
		gross_lab_inc_`year'_THIRTY ///
		gross_lab_inc_`year'_THIRTY_EIGHT ///
		gross_lab_inc_`year'_FORTY_FIVE ///		
		gross_lab_inc_`year'_FIFTY_FIVE, ///
		title("Gross Labour Income by Weekly Hours of Work") ///
		subtitle("`year'") ///
		legendfrom(gross_lab_inc_`year'_THIRTY) rows(2) ///
		graphregion(color(white)) ///
		note("Notes: Amounts in GBP per year, 2015 prices. Top and bottom percentiles trimmed. Individual observations of benefit unit amount plotted",  ///
		size(vsmall)) 
		
	graph export "$dir_output_files/income/gross_labour_income/validation_${country}_gross_labour_income_bu_dist_`year'_18_65_2.png", ///
		replace width(2400) height(1350) 
				
}

graph drop _all


********************************************************************************
* 2.1 : Histograms - working age, individual 
********************************************************************************

* Prepare validation data
use year demAge dwt labC4 valid_yEmpPersGrossLevelYear ///
	valid_labHrsWorkEnumWeek  using ///
	"$dir_data/ukhls_validation_sample.dta", clear
	
* Keep only employed individuals
keep if labC4 == 1
drop labC4

keep if inrange(demAge,18,65)

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_yEmpPersGrossLevelYear, d
	
	replace valid_yEmpPersGrossLevelYear = . if ///
		valid_yEmpPersGrossLevelYear < r(p1) | ///
		valid_yEmpPersGrossLevelYear > r(p99)

}

* Prepare info needed for dynamic y axis labels 
qui sum year
local min_year = 2011 
local max_year = 2023  

forval year = `min_year'/`max_year' { 

	twoway__histogram_gen valid_yEmpPersGrossLevelYear if year == `year', ///
		width(2500) den gen(d_valid v2)

	qui sum d_valid
	gen max_d_valid_`year' = r(max) 
	
	drop d_valid v2

	foreach ls in $ls_cat {
	
		twoway__histogram_gen valid_yEmpPersGrossLevelYear if ///
			year == `year' & valid_labHrsWorkEnumWeek == "`ls'", width(2500) ///
			den gen(d_valid v2)

		qui sum d_valid
		gen max_d_valid_`year'_`ls' = r(max) 
		
		drop d_valid v2	
	
	}
	
}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare SimPaths data
use run year demAge labC4 sim_yEmpPersGrossLevelYear sim_labHrsWorkEnumWeek ///
	using "$dir_data/simulation_sample.dta", clear	
	
* Keep only employed individuals
keep if labC4 == "EmployedOrSelfEmployed"
drop labC4

keep if inrange(demAge,18,65)

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum sim_yEmpPersGrossLevelYear, d
	
	replace sim_yEmpPersGrossLevelYear = . if ///
		sim_yEmpPersGrossLevelYear < r(p1) | sim_yEmpPersGrossLevelYear > r(p99)

		}

keep if run == 1

append using "$dir_data/temp_valid_stats.dta"

* Plot sub-figures 
qui sum year
local min_year = 2011
local max_year = 2023  

forval year = `min_year'/`max_year' {
	
	* Prepare info needed for dynamic y axis labels
	twoway__histogram_gen sim_yEmpPersGrossLevelYear if year == `year', ///
		width(2500) den gen(d_sim v1)

	qui sum d_sim 
	gen max_d_sim = r(max)

	gen max_value = max_d_valid_`year' if max_d_valid_`year' > max_d_sim 
	replace max_value = max_d_sim if max_value == . 

	sum max_value 
	local max_y = 1.25*r(max)
	local steps = `max_y'/2	
	
	
    * Plot all hours
	twoway (hist sim_yEmpPersGrossLevelYear if year == `year' , width(2500) ///
		color(green%30) legend(label(1 "SimPaths"))) ///
	(hist valid_yEmpPersGrossLevelYear if year == `year' , width(2500) ///
		color(red%30) legend(label(2 "UKHLS"))) , ///
		subtitle("ALL hours") ///
		name(gross_lab_inc_`year'_all, replace) ///
		xtitle("GBP", size(small)) ///
		ytitle("Density", size(small)) ///
		xlabel(,labsize(vsmall) angle(forty_five)) ///
		ylabel(0(`steps')`max_y',labsize(vsmall)) ///
		graphregion(color(white)) 

	
	drop d_sim v1 max_d_sim max_value
	
	* Plot by weekly hours work
	foreach ls in $ls_cat_labour {
	
		* Prepare info needed for dynamic y axis labels 	
		twoway__histogram_gen sim_yEmpPersGrossLevelYear if year == `year' & ///
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
		twoway (hist sim_yEmpPersGrossLevelYear if year == `year' & ///
			sim_labHrsWorkEnumWeek == "`ls'", width(2500) color(green%30) ///
			legend(label(1 "SimPaths"))) ///
		(hist valid_yEmpPersGrossLevelYear if year == `year' & ///
			valid_labHrsWorkEnumWeek == "`ls'", width(2500) color(red%30) ///
			legend(label(2 "UKHLS"))) , ///
			subtitle("`ls' hours")  ///
			name(gross_lab_inc_`year'_`ls', replace) ///
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
forvalues year = `min_year'/`max_year' {
	
	grc1leg gross_lab_inc_`year'_all  ///
		gross_lab_inc_`year'_TEN  ///
		gross_lab_inc_`year'_TWENTY, ///
		title("Individual Gross Labour Income by Weekly Hours of Work") ///
		subtitle("`year'") ///
		legendfrom(gross_lab_inc_`year'_TEN) rows(1) ///
		graphregion(color(white)) ///
		note("Notes: Amounts in GBP per year, 2015 prices. Employed 18-65 years olds included in the sample. Top and bottom percentiles trimmed.",  ///
		size(vsmall)) 
		
	graph export "$dir_output_files/income/gross_labour_income/validation_${country}_ind_gross_labour_income_dist_`year'_both_1.png", ///
		replace width(2400) height(1350) 
		
		
	grc1leg ///
		gross_lab_inc_`year'_THIRTY ///
		gross_lab_inc_`year'_THIRTY_EIGHT ///
		gross_lab_inc_`year'_FORTY_FIVE ///		
		gross_lab_inc_`year'_FIFTY_FIVE, ///
		title("Individual Gross Labour Income by Weekly Hours of Work") ///
		subtitle("`year'") ///
		legendfrom(gross_lab_inc_`year'_THIRTY) rows(2) ///
		graphregion(color(white)) ///
		note("Notes: Amounts in GBP per year, 2015 prices. Employed 18-65 years olds included in the sample. Top and bottom percentiles trimmed.",  ///
		size(vsmall)) 
		
	graph export "$dir_output_files/income/gross_labour_income/validation_${country}_ind_gross_labour_income_dist_`year'_both_2.png", ///
		replace width(2400) height(1350) 
			
}

graph drop _all

