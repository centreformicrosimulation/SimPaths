********************************************************************************
* PROJECT:  		ESPON 
* SECTION:			Validation
* OBJECT: 			Hours worked 
* AUTHORS:			Ashley Burdett 
* LAST UPDATE:		06/2025 (AB)
* COUNTRY: 			UK 

* NOTES: 		
********************************************************************************

********************************************************************************
* 1 : Mean values over time
********************************************************************************

********************************************************************************
* 1.1 : Mean values over time - Ages 18-65
********************************************************************************

* Prepare validation data
use year dwt les_c4 lhw hours lhw_flag using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Select sample
keep if les_c4 == 1
drop if lhw_flag == 1 

* Censor those who work very large number of hours
replace hours = $max_hours if hours > $max_hours & hours != .  

* Compute mean
collapse (mean) hours [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year les_c4 hoursworkedweekly using ///
	"$dir_data/simulated_data.dta", clear

rename hoursworkedweekly lhw_sim

* Keep only employed individuals
keep if les_c4 == "EmployedOrSelfEmployed"

* Compute mean and sd 
collapse (mean) lhw_sim, by(run year)
collapse (mean) lhw_sim ///
		 (sd) lhw_sim_sd = lhw_sim ///
		 , by(year)

* Approx 95% confidence interval 	 
foreach varname in lhw_sim {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

* Combine datasets
merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea lhw_sim_high lhw_sim_low year, sort color(green%20) ///
	legend(label(1 "Simulated"))) ///
(line hours year, sort color(green) legend(label(2 "UKHLS"))), ///
	title("Average Weekly Hours Worked") ///
	subtitle("Ages 18-65") ///
	xtitle("Year", size(small)) ///
	ytitle("Hours per week", size(small)) ///
	ylabel(33 [2] 40 ,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///	
	graphregion(color(white)) ///
	note("Note: Statistics calculated on sample of wokring age employed and self-employed individuals.", ///
	size(vsmall))		

* Save figure
graph export ///
"$dir_output_files/hours_worked/validation_${country}_hours_worked_ts_${min_age}_${max_age}_both.jpg", ///
	replace width(2560) height(1440) quality(100)
	
	
********************************************************************************
* 1.2 : Mean values over time - Ages 18-65, by gender
********************************************************************************

* Males

* Prepare validation data
use year dwt les_c4 hours dgn lhw_flag using ///
	"$dir_data/ukhls_validation_sample.dta", clear
	
* Select sample
keep if dgn == 1	
keep if les_c4 == 1
drop if lhw_flag == 1 

replace hours = $max_hours if hours > $max_hours & hours != .  

* Compute mean 
collapse (mean) hours [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year les_c4 hoursworkedweekly dgn  using ///
	"$dir_data/simulated_data.dta", clear

rename hoursworkedweekly lhw

* Select sample 
keep if dgn == "Male"
keep if les_c4 == "EmployedOrSelfEmployed"

*Compute mean and sd
collapse (mean) lhw, by(run year)
collapse (mean) lhw ///
		 (sd) lhw_sd = lhw ///
		 , by(year)
		 
* Approx 95% confidence interval		 
foreach varname in lhw {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

* Combine datasets
merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure 
twoway (rarea lhw_high lhw_low year, sort color(green%20) ///
	legend(label(1 "Simulated"))) ///
(line hours year, sort color(green) legend(label(2 "UKHLS"))), ///
	title("Average Weekly Hours Worked") ///
	subtitle("Ages 18-65, males") ///
	xtitle("Year", size(small)) ///
	ytitle("Hours per week", size(small)) ///
	ylabel(35 [2] 43 ,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///	
	graphregion(color(white)) ///
	note("Note: Statistics calculated on sample of employed and self-employed males", ///
	size(vsmall))		

* Save figure
graph export ///
"$dir_output_files/hours_worked/validation_${country}_hours_worked_ts_${min_age}_${max_age}_male.jpg", ///
	replace width(2560) height(1440) quality(100)
		

* Females 

* Prepare validation data
use year dwt les_c4 hours dgn lhw_flag using ///
	"$dir_data/ukhls_validation_sample.dta", clear
	
* Select sample
keep if dgn == 0	
keep if les_c4 == 1
drop if lhw_flag == 1 

replace hours = $max_hours if hours > $max_hours & hours != .  

* Compute mean
collapse (mean) hours [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year les_c4 hoursworkedweekly dgn  using ///
	"$dir_data/simulated_data.dta", clear

rename hoursworkedweekly lhw

* Select sample
keep if dgn == "Female"

* Keep only employed individuals
keep if les_c4 == "EmployedOrSelfEmployed"

* Compute mean and sd
collapse (mean) lhw, by(run year)
collapse (mean) lhw ///
		 (sd) lhw_sd = lhw ///
		 , by(year)
		 
* Approx 95% confidnece interval 		 
foreach varname in lhw {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

 *Combine datasets
merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea lhw_high lhw_low year, sort color(green%20) ///
	legend(label(1 "Simulated"))) ///
(line hours year, sort color(green) legend(label(2 "UKHLS"))), ///
	title("Average Weekly Hours Worked") ///
	subtitle("Ages 18-65, females") ///
	xtitle("Year", size(small)) ///
	ytitle("Hours per week", size(small)) ///
	ylabel(29 [1] 34 ,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///	
	graphregion(color(white)) ///
	note("Note: Statistics calculated on sample of employed and self-employed females", ///
	size(vsmall))		

* Save figure
graph export ///
"$dir_output_files/hours_worked/validation_${country}_hours_worked_ts_${min_age}_${max_age}_female.jpg", ///
	replace width(2560) height(1440) quality(100)
		
		

********************************************************************************
* 2 : Histograms by year
********************************************************************************

********************************************************************************
* 2.1 : Histograms by year - ages 18-65
********************************************************************************

* Prepare validation data
use year dwt les_c4 hours lhw_flag using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Select sample
keep if les_c4 == 1
drop if lhw_flag == 1 //remove those that 

replace hours = $max_hours if hours > $max_hours & hours != . 

* Prepare info needed for dynamic y axis labels 
qui sum year
local min_year = 2011
local max_year = r(max)  

forval year = `min_year'/`max_year' { 

	twoway__histogram_gen hours if year == `year' , ///
		bin(60) den gen(d_valid v2)

	qui sum d_valid
	gen max_d_valid_`year' = r(max) 
	
	drop d_valid v2

}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run idperson year les_c4 hoursworkedweekly using ///
	"$dir_data/simulated_data.dta", clear

rename hoursworkedweekly lhw

* Select sample
keep if les_c4 == "EmployedOrSelfEmployed"

collapse (mean) lhw, by(idperson year)

append using "$dir_data/temp_valid_stats.dta"

qui sum year
local min_year = 2011
local max_year = r(max)  

forval year = `min_year'/`max_year' {
	
	* Prepare info needed for dynamic y axis labels 
	twoway__histogram_gen lhw if year == `year', bin(60) ///
		den gen(d_sim v1)

	qui sum d_sim 
	gen max_d_sim = r(max)

	gen max_value = max_d_valid_`year' if max_d_valid_`year' > max_d_sim 
	replace max_value = max_d_sim if max_value == . 

	sum max_value 
	local max_y = 1.25*r(max)
	local steps = `max_y'/2
	
	twoway (hist lhw if year == `year' /*& lhw <= 65*/, width(1) color(green%30) ///
		legend(label(1 "Simulated"))) ///
	(hist hours if year == `year' /*& hours <= 65*/, width(1)  color(red%30) ///
		legend(label(2 "UKHLS"))), ///
		title("Weekly Hours Worked") ///
		subtitle("`year'") ///
		xtitle("Hours per week", size(small)) ///
		ytitle("Density", size(small)) ///
		xlabel(,labsize(small)) ///
		ylabel(0(`steps')`max_y', labsize(small)) ///
		legend(size(small)) ///
		graphregion(color(white)) ///
	note("Note: Statistics calculated on sample of employed and self-employed individuals age 18-65. UKHLS hours unrestricted.", ///
	size(vsmall))		
		
	graph export ///
		"$dir_output_files/hours_worked/validation_${country}_hours_worked_hist_`year'_unrestricted.png", ///
		replace width(2400) height(1350) 
	
	drop d_sim v1 max_d_sim max_value
	
}

* Restricted 

forval year = `min_year'/`max_year' {
	
	* Prepare info needed for dynamic y axis labels 
	twoway__histogram_gen lhw if year == `year', bin(60) ///
		den gen(d_sim v1)

	qui sum d_sim 
	gen max_d_sim = r(max)

	gen max_value = max_d_valid_`year' if max_d_valid_`year' > max_d_sim 
	replace max_value = max_d_sim if max_value == . 

	sum max_value 
	local max_y = 1.25*r(max)
	local steps = `max_y'/2
	
	twoway (hist lhw if year == `year' & lhw <= 65, width(1) color(green%30) ///
		legend(label(1 "Simulated"))) ///
	(hist hours if year == `year' & hours <= 65, width(1)  color(red%30) ///
		legend(label(2 "UKHLS"))), ///
		title("Weekly Hours Worked") ///
		subtitle("`year'") ///
		xtitle("Hours per week", size(small)) ///
		ytitle("Density", size(small)) ///
		xlabel(,labsize(small)) ///
		ylabel(0(`steps')`max_y', labsize(small)) ///
		legend(size(small)) ///
		graphregion(color(white)) ///
	note("Note: Statistics calculated on sample of employed and self-employed individuals age 18-65. UKHLS hours restricted to be" "at most 65 hours per week.", ///
	size(vsmall))		
		
	graph export ///
		"$dir_output_files/hours_worked/validation_${country}_hours_worked_hist_`year'.png", ///
		replace width(2400) height(1350) 
	
	drop d_sim v1 max_d_sim max_value

}


********************************************************************************
* 2.1 : Histograms by year - ages 18-65, by gender
********************************************************************************

* Female 
* Prepare validation data
use year dwt les_c4 hours dgn using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Select sample
keep if les_c4 == 1
keep if dgn == 0 

drop if lhw_flag == 1 

replace hours = $max_hours if hours > $max_hours & hours != .  

* Prepare info needed for dynamic y axis labels 
qui sum year
local min_year = 2011
local max_year = r(max)  

forval year = `min_year'/`max_year' { 

	twoway__histogram_gen hours if year == `year' , ///
		bin(60) den gen(d_valid v2)

	qui sum d_valid
	gen max_d_valid_`year' = r(max) 
	
	drop d_valid v2

}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run idperson year les_c4 dgn hoursworkedweekly using ///
	"$dir_data/simulated_data.dta", clear

rename hoursworkedweekly lhw

* Keep only employed individuals
keep if les_c4 == "EmployedOrSelfEmployed"
keep if dgn == "Female"

collapse (mean) lhw, by(idperson year)

append using "$dir_data/temp_valid_stats.dta"

qui sum year
local min_year = 2011
local max_year = r(max)  

forval year = `min_year'/`max_year' {
	
	* Prepare info needed for dynamic y axis labels 
	twoway__histogram_gen lhw if year == `year', bin(60) ///
		den gen(d_sim v1)

	qui sum d_sim 
	gen max_d_sim = r(max)

	gen max_value = max_d_valid_`year' if max_d_valid_`year' > max_d_sim 
	replace max_value = max_d_sim if max_value == . 

	sum max_value 
	local max_y = 1.25*r(max)
	local steps = `max_y'/2
	
	twoway (hist lhw if year == `year' /*& lhw <= 65*/, width(1) ///
		color(green%30) legend(label(1 "Simulated"))) ///
	(hist hours if year == `year' /*& hours <= 65*/, width(1) color(red%30) ///
		legend(label(2 "UKHLS"))), ///
		title("Weekly Hours Worked") ///
		subtitle("`year', females") ///
		xtitle("Hours per week", size(small)) ///
		ytitle("Density", size(small)) ///
		xlabel(,labsize(small)) ///
		ylabel(0(`steps')`max_y', labsize(small)) ///
		legend(size(small)) ///
		graphregion(color(white)) ///
	note("Note: Statistics calculated on sample of employed and self-employed females age 18-65. UKHLS hours unrestricted.", ///
		size(vsmall)) 
		
	graph export ///
		"$dir_output_files/hours_worked/validation_${country}_hours_worked_hist_`year'_female.png", ///
		replace width(2400) height(1350) 
	
	drop d_sim v1 max_d_sim max_value
	
}


* Male 
* Prepare validation data
use year dwt les_c4 hours dgn using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Select sample
keep if les_c4 == 1
keep if dgn == 1 

* Prepare info needed for dynamic y axis labels 
qui sum year
local min_year = 2011
local max_year = r(max)  

forval year = `min_year'/`max_year' { 

	twoway__histogram_gen hours if year == `year' , ///
		bin(60) den gen(d_valid v2)

	qui sum d_valid
	gen max_d_valid_`year' = r(max) 
	
	drop d_valid v2

}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run idperson year les_c4 dgn hoursworkedweekly using ///
	"$dir_data/simulated_data.dta", clear

rename hoursworkedweekly lhw

* Select sample
keep if les_c4 == "EmployedOrSelfEmployed"
keep if dgn == "Male"

collapse (mean) lhw, by(idperson year)

append using "$dir_data/temp_valid_stats.dta"

qui sum year
local min_year = 2011
local max_year = r(max)  

forval year = `min_year'/`max_year' {
	
	* Prepare info needed for dynamic y axis labels 
	twoway__histogram_gen lhw if year == `year', bin(60) ///
		den gen(d_sim v1)

	qui sum d_sim 
	gen max_d_sim = r(max)

	gen max_value = max_d_valid_`year' if max_d_valid_`year' > max_d_sim 
	replace max_value = max_d_sim if max_value == . 

	sum max_value 
	local max_y = 1.25*r(max)
	local steps = `max_y'/2
	
	twoway (hist lhw if year == `year' /*& lhw <= 65*/, width(1) ///
		color(green%30) legend(label(1 "Simulated"))) ///
	(hist hours if year == `year' /*& lhw <= 65*/, width(1) color(red%30) ///
		legend(label(2 "UKHLS"))), ///
		title("Weekly Hours Worked") ///
		subtitle("`year', males") ///
		xtitle("Hours per week", size(small)) ///
		ytitle("Density", size(small)) ///
		xlabel(,labsize(small)) ///
		ylabel(0(`steps')`max_y', labsize(small)) ///
		legend(size(small)) ///
		graphregion(color(white)) ///
	note("Note: Statistics calculated on sample of employed and self-employed males age 18-65. UKHLS hours unrestricted.", ///
		size(vsmall)) 
	
		
	graph export ///
		"$dir_output_files/hours_worked/validation_${country}_hours_worked_hist_`year'_male.png", ///
		replace width(2400) height(1350) 
	
	drop d_sim v1 max_d_sim max_value
}	
	
	
graph drop _all 	
