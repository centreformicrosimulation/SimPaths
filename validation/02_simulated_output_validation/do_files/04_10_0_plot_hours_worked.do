********************************************************************************
* PROJECT:  		SimPaths UK 
* SECTION:			Validation
* OBJECT: 			Hours worked per week
* AUTHORS:			Ashley Burdett 
* LAST UPDATE:		Jan 2026
* COUNTRY: 			UK 
********************************************************************************
* NOTES: 			Current implementation explores the impact how the 
* 					heterogeneity of the upper most category is instructed.
********************************************************************************

set seed  12345

********************************************************************************
* UNIFORM HETEROGENIETY
********************************************************************************

********************************************************************************
* 1 : Mean values over time
********************************************************************************

********************************************************************************
* 1.1 : Mean values over time - Ages 16-65
********************************************************************************

* Prepare validation data
use year dwt labC4 valid_labHrsWorkWeek demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear
	
* Select sample
keep if labC4 == 1
keep if inrange(demAge,16,65)

* Compute mean
collapse (mean) valid_labHrsWorkWeek [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year idPers labC4 sim_labHrsWorkWeek demAge using ///
	"$dir_data/simulation_sample.dta", clear
		
* Select sample
keep if inrange(demAge,16,65)		
		
rename sim_labHrsWorkWeek sim_labHrsWorkWeek

* Keep only employed individuals
keep if labC4 == "EmployedOrSelfEmployed"

* Compute mean and sd 
collapse (mean) sim_labHrsWorkWeek, by(run year)

collapse (mean) sim_labHrsWorkWeek ///
		 (sd) sim_labHrsWorkWeek_sd = sim_labHrsWorkWeek ///
		 , by(year)

* Approx 95% confidence interval 	 
foreach varname in sim_labHrsWorkWeek {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

save "$dir_data/temp_sim_mean_uni.dta", replace

* Combine datasets
merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea sim_labHrsWorkWeek_high sim_labHrsWorkWeek_low year, sort ///
	color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_labHrsWorkWeek year, sort color(green) legend(label(2 "UKHLS"))), ///
	title("Average Weekly Hours Worked") ///
	subtitle("Ages 16-65") ///
	xtitle("Year", size(small)) ///
	ytitle("Hours per week", size(small)) ///
	ylabel(,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///	
	graphregion(color(white)) ///
	note("Note: Statistics calculated on sample of working age employed and self-employed individuals.", ///
	size(vsmall))		

* Save figure
graph export ///
"$dir_output_files/hours_worked/validation_${country}_hours_worked_ts_16_65_both.jpg", ///
	replace width(2560) height(1440) quality(100)
	
	
********************************************************************************
* 1.2 : Mean values over time - Ages 16-65, by gender
********************************************************************************

* Prepare validation data
use year dwt labC4 valid_labHrsWorkWeek demAge demMaleFlag using ///
	"$dir_data/ukhls_validation_sample.dta", clear
	
* Select sample
keep if labC4 == 1
keep if inrange(demAge,16,65)

* Compute mean 
collapse (mean) valid_labHrsWorkWeek [aw = dwt], by(year demMaleFlag)

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year idPers labC4 sim_labHrsWorkWeek demAge demMaleFlag using ///
	"$dir_data/simulation_sample.dta", clear
		
* Select sample 
keep if labC4 == "EmployedOrSelfEmployed"
keep if inrange(demAge,16,65)		

* Compute mean and sd
collapse (mean) sim_labHrsWorkWeek, by(run year demMaleFlag)

collapse (mean) sim_labHrsWorkWeek ///
		 (sd) sim_labHrsWorkWeek_sd = sim_labHrsWorkWeek ///
		 , by(year demMaleFlag)
		 
* Approx 95% confidence interval		 
foreach varname in sim_labHrsWorkWeek {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

* Combine datasets
merge 1:1 year demMaleFlag using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure 

* Males
preserve 

keep if demMaleFlag == 1

twoway (rarea sim_labHrsWorkWeek_high sim_labHrsWorkWeek_low year, sort ///
	color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_labHrsWorkWeek year, sort color(green) legend(label(2 "UKHLS"))), ///
	title("Average Weekly Hours Worked") ///
	subtitle("Males, ages 16-65") ///
	xtitle("Year", size(small)) ///
	ytitle("Hours per week", size(small)) ///
	ylabel(,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///	
	graphregion(color(white)) ///
	note("Note: Statistics calculated on sample of working age employed and self-employed individuals.", ///
	size(vsmall))		

* Save figure
graph export ///
"$dir_output_files/hours_worked/validation_${country}_hours_worked_ts_16_65_male.jpg", ///
	replace width(2560) height(1440) quality(100)
		
restore 

* Females 

keep if demMaleFlag == 0 

twoway (rarea sim_labHrsWorkWeek_high sim_labHrsWorkWeek_low year, sort ///
	color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_labHrsWorkWeek year, sort color(green) legend(label(2 "UKHLS"))), ///
	title("Average Weekly Hours Worked") ///
	subtitle("Females, ages 16-65") ///
	xtitle("Year", size(small)) ///
	ytitle("Hours per week", size(small)) ///
	ylabel(,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///	
	graphregion(color(white)) ///
	note("Note: Statistics calculated on sample of working age employed and self-employed individuals.", ///
	size(vsmall))		

* Save figure
graph export ///
"$dir_output_files/hours_worked/validation_${country}_hours_worked_ts_16_65_female.jpg", ///
	replace width(2560) height(1440) quality(100)
		
		
********************************************************************************
* 1.3 : Mean values over time - Ages 16-75
********************************************************************************

* Prepare validation data
use year dwt labC4 valid_labHrsWorkWeek demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear
	
* Select sample
keep if labC4 == 1
keep if inrange(demAge,16,75)

* Compute mean
collapse (mean) valid_labHrsWorkWeek [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year idPers labC4 sim_labHrsWorkWeek demAge using ///
	"$dir_data/simulation_sample.dta", clear
		
* Select sample
keep if inrange(demAge,16,75)		
		
* Keep only employed individuals
keep if labC4 == "EmployedOrSelfEmployed"

* Compute mean and sd 
collapse (mean) sim_labHrsWorkWeek, by(run year)

collapse (mean) sim_labHrsWorkWeek ///
		 (sd) sim_labHrsWorkWeek_sd = sim_labHrsWorkWeek ///
		 , by(year)

* Approx 95% confidence interval 	 
foreach varname in sim_labHrsWorkWeek {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

save "$dir_data/temp_sim_mean_uni.dta", replace

* Combine datasets
merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea sim_labHrsWorkWeek_high sim_labHrsWorkWeek_low year, sort ///
	color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_labHrsWorkWeek year, sort color(green) legend(label(2 "UKHLS"))), ///
	title("Average Weekly Hours Worked") ///
	subtitle("Ages 16-75") ///
	xtitle("Year", size(small)) ///
	ytitle("Hours per week", size(small)) ///
	ylabel(,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///	
	graphregion(color(white)) ///
	note("Note: Statistics calculated on sample of working age employed and self-employed individuals.", ///
	size(vsmall))		

* Save figure
graph export ///
"$dir_output_files/hours_worked/validation_${country}_hours_worked_ts_16_75_both.jpg", ///
	replace width(2560) height(1440) quality(100)		
		

********************************************************************************
* 1.2 : Mean values over time - Ages 16-75, by gender
********************************************************************************

* Prepare validation data
use year dwt labC4 valid_labHrsWorkWeek demAge demMaleFlag using ///
	"$dir_data/ukhls_validation_sample.dta", clear
	
* Select sample
keep if labC4 == 1
keep if inrange(demAge,16,75)

* Compute mean 
collapse (mean) valid_labHrsWorkWeek [aw = dwt], by(year demMaleFlag)

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year idPers labC4 sim_labHrsWorkWeek demAge demMaleFlag using ///
	"$dir_data/simulation_sample.dta", clear
		
* Select sample 
keep if labC4 == "EmployedOrSelfEmployed"
keep if inrange(demAge,16,75)		

* Compute mean and sd
collapse (mean) sim_labHrsWorkWeek, by(run year demMaleFlag)

collapse (mean) sim_labHrsWorkWeek ///
		 (sd) sim_labHrsWorkWeek_sd = sim_labHrsWorkWeek ///
		 , by(year demMaleFlag)
		 
* Approx 95% confidence interval		 
foreach varname in sim_labHrsWorkWeek {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

* Combine datasets
merge 1:1 year demMaleFlag using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figures 

* Male
preserve

keep if demMaleFlag == 1

twoway (rarea sim_labHrsWorkWeek_high sim_labHrsWorkWeek_low year, sort ///
	color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_labHrsWorkWeek year, sort color(green) legend(label(2 "UKHLS"))), ///
	title("Average Weekly Hours Worked") ///
	subtitle("Males, ages 16-75") ///
	xtitle("Year", size(small)) ///
	ytitle("Hours per week", size(small)) ///
	ylabel(,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///	
	graphregion(color(white)) ///
	note("Note: Statistics calculated on sample of working age employed and self-employed individuals.", ///
	size(vsmall))		

* Save figure
graph export ///
"$dir_output_files/hours_worked/validation_${country}_hours_worked_ts_16_75_male.jpg", ///
	replace width(2560) height(1440) quality(100)

restore 

* Females 

keep if demMaleFlag == 0 

twoway (rarea sim_labHrsWorkWeek_high sim_labHrsWorkWeek_low year, ///
	sort color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_labHrsWorkWeek year, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	title("Average Weekly Hours Worked") ///
	subtitle("Females, ages 16-75") ///
	xtitle("Year", size(small)) ///
	ytitle("Hours per week", size(small)) ///
	ylabel(,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///	
	graphregion(color(white)) ///
	note("Note: Statistics calculated on sample of working age employed and self-employed individuals.", ///
	size(vsmall))		

* Save figure
graph export ///
"$dir_output_files/hours_worked/validation_${country}_hours_worked_ts_16_75_female.jpg", ///
	replace width(2560) height(1440) quality(100)		
		
		
********************************************************************************
* 2 : Histograms by year
********************************************************************************

********************************************************************************
* 2.1 : Histograms by year - ages 16-65
********************************************************************************

* Prepare validation data
use year dwt labC4 valid_labHrsWorkWeek demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear
	
* Select sample
keep if labC4 == 1
keep if inrange(demAge,16,65)

* Prepare info needed for dynamic y axis labels 
qui sum year
local min_year = 2011
local max_year = 2023  

forval year = `min_year'/`max_year' { 

	twoway__histogram_gen valid_labHrsWorkWeek if year == `year' , ///
		bin(60) den gen(d_valid v2)

	qui sum d_valid
	gen max_d_valid_`year' = r(max) 
	
	drop d_valid v2

}

drop labC4

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year labC4 idPers sim_labHrsWorkWeek demAge using ///
	"$dir_data/simulation_sample.dta", clear
	
* Select sample
keep if labC4 == "EmployedOrSelfEmployed"
keep if inrange(demAge,16,65)

* Combine datasets
append using "$dir_data/temp_valid_stats.dta"

qui sum year
local min_year = 2011
local max_year = 2023  

forval year = `min_year'/`max_year' {
	
	* Prepare info needed for dynamic y axis labels 
	twoway__histogram_gen sim_labHrsWorkWeek if year == `year', bin(60) ///
		den gen(d_sim v1)

	qui sum d_sim 
	gen max_d_sim = r(max)

	gen max_value = max_d_valid_`year' if max_d_valid_`year' > max_d_sim 
	replace max_value = max_d_sim if max_value == . 

	sum max_value 
	local max_y = 1.25*r(max)
	local steps = `max_y'/2
	
	twoway (hist sim_labHrsWorkWeek if year == `year', width(1) ///
		color(green%30) legend(label(1 "SimPaths"))) ///
	(hist valid_labHrsWorkWeek if year == `year', width(1)  color(red%30) ///
		legend(label(2 "UKHLS"))), ///
		title("Weekly Hours Worked") ///
		subtitle(" `year', ages 16-65") ///
		xtitle("Hours per week", size(small)) ///
		ytitle("Density", size(small)) ///
		xlabel(,labsize(small)) ///
		ylabel(0(`steps')`max_y', labsize(small)) ///
		legend(size(small)) ///
		graphregion(color(white)) ///
	note("Note: Statistics calculated on sample of employed and self-employed individuals.", ///
	size(vsmall))		
		
	graph export ///
		"$dir_output_files/hours_worked/validation_${country}_hours_worked_hist_`year'.png", ///
		replace width(2400) height(1350) 
	
	drop d_sim v1 max_d_sim max_value
	
}

********************************************************************************
* 2.2 : Histograms by year - ages 16-65, by gender
********************************************************************************

* Female 

* Prepare validation data
use year dwt labC4 valid_labHrsWorkWeek demMaleFlag demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear
	
* Select sample
keep if labC4 == 1
keep if demMaleFlag == 0 
keep if inrange(demAge,16,65)

* Prepare info needed for dynamic y axis labels 
qui sum year
local min_year = 2011
local max_year = 2023  

forval year = `min_year'/`max_year' { 

	twoway__histogram_gen valid_labHrsWorkWeek if year == `year' , ///
		bin(60) den gen(d_valid v2)

	qui sum d_valid
	gen max_d_valid_`year' = r(max) 
	
	drop d_valid v2

}

drop labC4 demMaleFlag

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run idPers year labC4 demMaleFlag sim_labHrsWorkWeek demAge using ///
	"$dir_data/simulation_sample.dta", clear
	
* Select sample
keep if labC4 == "EmployedOrSelfEmployed"
keep if demMaleFlag == 0
keep if inrange(demAge,16,65)

* Combine datasets
append using "$dir_data/temp_valid_stats.dta"

* Plot by year 
qui sum year
local min_year = 2011
local max_year = 2023  

forval year = `min_year'/`max_year' {
	
	* Prepare info needed for dynamic y axis labels 
	twoway__histogram_gen sim_labHrsWorkWeek if year == `year', bin(60) ///
		den gen(d_sim v1)

	qui sum d_sim 
	gen max_d_sim = r(max)

	gen max_value = max_d_valid_`year' if max_d_valid_`year' > max_d_sim 
	replace max_value = max_d_sim if max_value == . 

	sum max_value 
	local max_y = 1.25*r(max)
	local steps = `max_y'/2
	
	twoway (hist sim_labHrsWorkWeek if year == `year', width(1) ///
		color(green%30) legend(label(1 "SimPaths"))) ///
	(hist valid_labHrsWorkWeek if year == `year', width(1) color(red%30) ///
		legend(label(2 "UKHLS"))), ///
		title("Weekly Hours Worked") ///
		subtitle("`year', females, ages 16-65") ///
		xtitle("Hours per week", size(small)) ///
		ytitle("Density", size(small)) ///
		xlabel(,labsize(small)) ///
		ylabel(0(`steps')`max_y', labsize(small)) ///
		legend(size(small)) ///
		graphregion(color(white)) ///
	note("Note: Statistics calculated on sample of employed and self-employed females.", ///
		size(vsmall)) 
		
	graph export ///
		"$dir_output_files/hours_worked/validation_${country}_hours_worked_hist_`year'_female.png", ///
		replace width(2400) height(1350) 
	
	drop d_sim v1 max_d_sim max_value
	
}


* Male 

* Prepare validation data
use year dwt labC4 valid_labHrsWorkWeek demMaleFlag  demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear
	
* Select sample
keep if labC4 == 1
keep if demMaleFlag == 1 
keep if inrange(demAge,16,65)

* Prepare info needed for dynamic y axis labels 
qui sum year
local min_year = 2011
local max_year = 2023  

forval year = `min_year'/`max_year' { 

	twoway__histogram_gen valid_labHrsWorkWeek if year == `year' , ///
		bin(60) den gen(d_valid v2)

	qui sum d_valid
	gen max_d_valid_`year' = r(max) 
	
	drop d_valid v2

}

drop labC4 demMaleFlag

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run idPers year labC4 demMaleFlag sim_labHrsWorkWeek demAge using ///
	"$dir_data/simulation_sample.dta", clear

* Select sample
keep if labC4 == "EmployedOrSelfEmployed"
keep if demMaleFlag == 1
keep if inrange(demAge,16,65)

* Combine datasets
append using "$dir_data/temp_valid_stats.dta"

qui sum year
local min_year = 2011
local max_year = 2023  

forval year = `min_year'/`max_year' {
	
	* Prepare info needed for dynamic y axis labels 
	twoway__histogram_gen sim_labHrsWorkWeek if year == `year', bin(60) ///
		den gen(d_sim v1)

	qui sum d_sim 
	gen max_d_sim = r(max)

	gen max_value = max_d_valid_`year' if max_d_valid_`year' > max_d_sim 
	replace max_value = max_d_sim if max_value == . 

	sum max_value 
	local max_y = 1.25*r(max)
	local steps = `max_y'/2
	
	twoway (hist sim_labHrsWorkWeek if year == `year' , width(1) ///
		color(green%30) legend(label(1 "SimPaths"))) ///
	(hist valid_labHrsWorkWeek if year == `year' , width(1) color(red%30) ///
		legend(label(2 "UKHLS"))), ///
		title("Weekly Hours Worked") ///
		subtitle("`year', males, ages 16-65") ///
		xtitle("Hours per week", size(small)) ///
		ytitle("Density", size(small)) ///
		xlabel(,labsize(small)) ///
		ylabel(0(`steps')`max_y', labsize(small)) ///
		legend(size(small)) ///
		graphregion(color(white)) ///
	note("Note: Statistics calculated on sample of employed and self-employed males.", ///
		size(vsmall)) 
	
		
	graph export ///
		"$dir_output_files/hours_worked/validation_${country}_hours_worked_hist_`year'_male.png", ///
		replace width(2400) height(1350) 
	
	drop d_sim v1 max_d_sim max_value
}	
	
	
graph drop _all 	


********************************************************************************
* 2.3 : Histograms by year - ages 16-75
********************************************************************************

* Prepare validation data
use year dwt labC4 valid_labHrsWorkWeek  demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear
	
* Select sample
keep if labC4 == 1
keep if inrange(demAge,16,75)

* Prepare info needed for dynamic y axis labels 
qui sum year
local min_year = 2011
local max_year = 2023  

forval year = `min_year'/`max_year' { 

	twoway__histogram_gen valid_labHrsWorkWeek if year == `year' , ///
		bin(60) den gen(d_valid v2)

	qui sum d_valid
	gen max_d_valid_`year' = r(max) 
	
	drop d_valid v2

}

drop labC4

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year labC4 idPers sim_labHrsWorkWeek demAge using ///
	"$dir_data/simulation_sample.dta", clear
	
* Select sample
keep if labC4 == "EmployedOrSelfEmployed"
keep if inrange(demAge,16,75)

* Combine datasets
append using "$dir_data/temp_valid_stats.dta"

qui sum year
local min_year = 2011
local max_year = 2023  

forval year = `min_year'/`max_year' {
	
	* Prepare info needed for dynamic y axis labels 
	twoway__histogram_gen sim_labHrsWorkWeek if year == `year', bin(60) ///
		den gen(d_sim v1)

	qui sum d_sim 
	gen max_d_sim = r(max)

	gen max_value = max_d_valid_`year' if max_d_valid_`year' > max_d_sim 
	replace max_value = max_d_sim if max_value == . 

	sum max_value 
	local max_y = 1.25*r(max)
	local steps = `max_y'/2
	
	twoway (hist sim_labHrsWorkWeek if year == `year', width(1) ///
		color(green%30) legend(label(1 "SimPaths"))) ///
	(hist valid_labHrsWorkWeek if year == `year', width(1)  color(red%30) ///
		legend(label(2 "UKHLS"))), ///
		title("Weekly Hours Worked") ///
		subtitle("Ages 16-75, `year'") ///
		xtitle("Hours per week", size(small)) ///
		ytitle("Density", size(small)) ///
		xlabel(,labsize(small)) ///
		ylabel(0(`steps')`max_y', labsize(small)) ///
		legend(size(small)) ///
		graphregion(color(white)) ///
	note("Note: Statistics calculated on sample of employed and self-employed individuals.", ///
	size(vsmall))		
		
	graph export ///
		"$dir_output_files/hours_worked/validation_${country}_hours_worked_hist_`year'_16_75.png", ///
		replace width(2400) height(1350) 
	
	drop d_sim v1 max_d_sim max_value
	
}

graph drop _all 

/*

********************************************************************************
* LOG-NORMAL HETEROGENIETY
********************************************************************************

********************************************************************************
* 0 : IMPUTATION OF valid_labHrsWorkWeek WORK FOR THOSE IN TOP CATEGORY, LOG NORMAL
********************************************************************************

* Estimate parameters of truncated log normal distribution fit to UKHLS
use year dwt labC4 valid_labHrsWorkWeek  demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Select sample
keep if inrange(demAge,16,65)
keep if labC4 == 1
//remove those that report working very low >0 valid_labHrsWorkWeek 

drop labC4 

* Keep those in top valid_labHrsWorkWeek category 
keep if valid_labHrsWorkWeek >= 50 & valid_labHrsWorkWeek != . 

* Proportion of the top group that work 40 valid_labHrsWorkWeek 
gen exact_40 = (valid_labHrsWorkWeek == 40)

preserve 
collapse (mean) exact_40 [aw=dwt]
local valid_share_40 = exact_40
restore

*  Create log variable
gen ln_y = ln(valid_labHrsWorkWeek)

* Non-truncation 
sum ln_y

* Set truncation points (using observed range)
sum valid_labHrsWorkWeek
local a = r(min)
local b = r(max)
local ln_a = ln(`a')
local ln_b = ln(`b')

* Estimate parameters
truncreg ln_y, ll(`ln_a') ul(`ln_b') nolog

* Results
matrix b = e(b)
local mu_hat = b[1,1]
local sigma_hat = b[1,2]
local median_est = exp(`mu_hat')
local mean_est = exp(`mu_hat' + `sigma_hat'^2/2)

* Generate new values directly in simulated dataset
use run idPers year labC4 sim_labHrsWorkWeek using ///
	"$dir_data/simulation_sample.dta", clear

rename sim_labHrsWorkWeek labHrsWorkWeek

* Select sample
keep if labC4 == "EmployedOrSelfEmployed"

* Observations to be adjusted  
gen top = 1 if labHrsWorkWeek > 35  

gen new_sim_labHrsWorkWeek = labHrsWorkWeek  

* Calculate the CDF bounds once
local Fa = normal((`ln_a' - `mu_hat')/`sigma_hat')
local Fb = normal((`ln_b' - `mu_hat')/`sigma_hat')

* For observations with valid_labHrsWorkWeek >= 36, generate random values
replace new_sim_labHrsWorkWeek = exp(`mu_hat' + `sigma_hat' * ///
    invnormal(`Fa' + runiform()*(`Fb' - `Fa'))) ///
    if top == 1
	
keep run year idPers new_sim_labHrsWorkWeek top

save "$dir_data/simulation_sample_hrs_adjusted.dta", replace	


********************************************************************************
* 1 : Mean values over time
********************************************************************************

********************************************************************************
* 1.1 : Mean values over time - Ages 16-65
********************************************************************************

* Prepare validation data
use year dwt labC4 valid_labHrsWorkWeek demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Select sample
keep if labC4 == 1
keep if inrange(demAge,16,65)

* Compute mean
collapse (mean) valid_labHrsWorkWeek [aw = dwt], by(year)

save "$dir_data/temp_valid_mean.dta", replace

* Prepare simulated data
use run year idPers labC4 sim_labHrsWorkWeek demAge using ///
	"$dir_data/simulation_sample.dta", clear
		
* Select sample
keep if inrange(demAge,16,65)		
		
* Merge in new valid_labHrsWorkWeek worked for top category using log normal distribution
merge 1:1 year run idPers using "$dir_data/simulation_sample_hrs_adjusted.dta"

rename sim_labHrsWorkWeek sim_labHrsWorkWeek_orig
rename new_sim_labHrsWorkWeek sim_labHrsWorkWeek

* Keep only employed individuals
keep if labC4 == "EmployedOrSelfEmployed"

* Compute mean and sd 
collapse (mean) sim_labHrsWorkWeek, by(run year)

collapse (mean) sim_labHrsWorkWeek ///
		 (sd) sim_labHrsWorkWeek_sd = sim_labHrsWorkWeek ///
		 , by(year)

* Approx 95% confidence interval 	 
foreach varname in sim_labHrsWorkWeek {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

save "$dir_data/temp_sim_mean_ln.dta", replace

* Combine datasets
merge 1:1 year using "$dir_data/temp_valid_mean.dta", keep(3) nogen

* Plot figure
twoway (rarea sim_labHrsWorkWeek_high sim_labHrsWorkWeek_low year, sort ///
	color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_labHrsWorkWeek year, sort color(green) legend(label(2 "UKHLS"))), ///
	title("Average Weekly Hours Worked") ///
	subtitle("Ages 16-65") ///
	xtitle("Year", size(small)) ///
	ytitle("Hours per week", size(small)) ///
	ylabel(33 [2] 40 ,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///	
	graphregion(color(white)) ///
	note("Note: Statistics calculated on sample of working age employed and self-employed individuals. Log-normal heterogeneity" "imposed on top category.", ///
	size(vsmall))		

* Save figure
graph export ///
"$dir_output_files/hours_worked/validation_${country}_hours_worked_ts_16_65_both_ln.jpg", ///
	replace width(2560) height(1440) quality(100)
	
	
********************************************************************************
* 1.1.1 : Mean values over time - Ages 16-65, by gender
********************************************************************************

* Males

* Prepare validation data
use year dwt labC4 valid_labHrsWorkWeek demAge demMaleFlag using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Select sample
keep if labC4 == 1

keep if inrange(demAge,16,65)
keep if demMaleFlag == 1

* Compute mean 
collapse (mean) valid_labHrsWorkWeek [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year idPers labC4 sim_labHrsWorkWeek demAge demMaleFlag using ///
	"$dir_data/simulation_sample.dta", clear
		
* Merge in new valid_labHrsWorkWeek worked for top category using log normal distribution
merge 1:1 year run idPers using "$dir_data/simulation_sample_hrs_adjusted.dta"
drop _m

rename sim_labHrsWorkWeek sim_labHrsWorkWeek_orig
rename new_sim_labHrsWorkWeek sim_labHrsWorkWeek

* Select sample 
keep if  demMaleFlag == 1
keep if labC4 == "EmployedOrSelfEmployed"
keep if inrange(demAge,16,65)		

* Compute mean and sd
collapse (mean) sim_labHrsWorkWeek, by(run year)

collapse (mean) sim_labHrsWorkWeek ///
		 (sd) sim_labHrsWorkWeek_sd = sim_labHrsWorkWeek ///
		 , by(year)
		 
* Approx 95% confidence interval		 
foreach varname in sim_labHrsWorkWeek {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

* Combine datasets
merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure 
twoway (rarea sim_labHrsWorkWeek_high sim_labHrsWorkWeek_low year, sort ///
	color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_labHrsWorkWeek year, sort color(green) legend(label(2 "UKHLS"))), ///
	title("Average Weekly Hours Worked") ///
	subtitle("Males, ages 16-65") ///
	xtitle("Year", size(small)) ///
	ytitle("Hours per week", size(small)) ///
	ylabel(35 [2] 43 ,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///	
	graphregion(color(white)) ///
	note("Note: Statistics calculated on sample of working age employed and self-employed individuals. Log-normal heterogeneity" "imposed on top category.", ///
	size(vsmall))		

* Save figure
graph export ///
"$dir_output_files/hours_worked/validation_${country}_hours_worked_ts_16_65_male_ln.jpg", ///
	replace width(2560) height(1440) quality(100)
		

* Females 

* Prepare validation data
use year dwt labC4 valid_labHrsWorkWeek  demAge demMaleFlag using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Select sample
keep if labC4 == 1
keep if inrange(demAge,16,65)
keep if demMaleFlag == 0

* Compute mean
collapse (mean) valid_labHrsWorkWeek [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year idPers labC4 sim_labHrsWorkWeek demAge demMaleFlag using ///
	"$dir_data/simulation_sample.dta", clear
		
* Select sample
keep if inrange(demAge,16,65)		

* Merge in update valid_labHrsWorkWeek worked for top category 	
merge 1:1 year run idPers using "$dir_data/simulation_sample_hrs_adjusted.dta"
drop _m

rename sim_labHrsWorkWeek sim_labHrsWorkWeek_orig
rename new_sim_labHrsWorkWeek sim_labHrsWorkWeek

* Select sample
keep if demMaleFlag == 0
keep if labC4 == "EmployedOrSelfEmployed"

* Compute mean and sd
collapse (mean) sim_labHrsWorkWeek, by(run year)

collapse (mean) sim_labHrsWorkWeek ///
		 (sd) sim_labHrsWorkWeek_sd = sim_labHrsWorkWeek ///
		 , by(year)
		 
* Approx 95% confidnece interval 		 
foreach varname in sim_labHrsWorkWeek {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

 *Combine datasets
merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea sim_labHrsWorkWeek_high sim_labHrsWorkWeek_low year, sort color(green%20) ///
	legend(label(1 "SimPaths"))) ///
(line valid_labHrsWorkWeek year, sort color(green) legend(label(2 "UKHLS"))), ///
	title("Average Weekly Hours Worked") ///
	subtitle("Females, ages 16-65") ///
	xtitle("Year", size(small)) ///
	ytitle("Hours per week", size(small)) ///
	ylabel(,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///	
	graphregion(color(white)) ///
	note("Note: Statistics calculated on sample of working age employed and self-employed individuals. Log-normal heterogeneity" "imposed on top category.", ///
	size(vsmall))		

* Save figure
graph export ///
"$dir_output_files/hours_worked/validation_${country}_hours_worked_ts_16_65_female_ln.jpg", ///
	replace width(2560) height(1440) quality(100)
		
		

********************************************************************************
* 2 : Histograms by year
********************************************************************************

********************************************************************************
* 2.1 : Histograms by year - ages 16-65
********************************************************************************

* Prepare validation data
use year dwt labC4 valid_labHrsWorkWeek  demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Select sample
keep if labC4 == 1
keep if inrange(demAge,16,65)

* Prepare info needed for dynamic y axis labels 
qui sum year
local min_year = 2019
local max_year = r(max)  

forval year = `min_year'/`max_year' { 

	twoway__histogram_gen valid_labHrsWorkWeek if year == `year' , ///
		bin(60) den gen(d_valid v2)

	qui sum d_valid
	gen max_d_valid_`year' = r(max) 
	
	drop d_valid v2

}

drop labC4

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year labC4 idPers sim_labHrsWorkWeek demAge using ///
	"$dir_data/simulation_sample.dta", clear
	

* Merge in new valid_labHrsWorkWeek worked for top category using log normal distribution
merge 1:1 year run idPers using "$dir_data/simulation_sample_hrs_adjusted.dta"
drop _m

rename sim_labHrsWorkWeek sim_labHrsWorkWeek_orig
rename new_sim_labHrsWorkWeek sim_labHrsWorkWeek

* Select sample
keep if labC4 == "EmployedOrSelfEmployed"
keep if inrange(demAge,16,65)

* Combine datasets
append using "$dir_data/temp_valid_stats.dta"

qui sum year
local min_year = 2019
local max_year = r(max)  

forval year = `min_year'/`max_year' {
	
	* Prepare info needed for dynamic y axis labels 
	twoway__histogram_gen sim_labHrsWorkWeek if year == `year', bin(60) ///
		den gen(d_sim v1)

	qui sum d_sim 
	gen max_d_sim = r(max)

	gen max_value = max_d_valid_`year' if max_d_valid_`year' > max_d_sim 
	replace max_value = max_d_sim if max_value == . 

	sum max_value 
	local max_y = 1.25*r(max)
	local steps = `max_y'/2
	
	twoway (hist sim_labHrsWorkWeek if year == `year' /*& labHrsWorkWeek <= 65*/, width(1) color(green%30) ///
		legend(label(1 "SimPaths"))) ///
	(hist valid_labHrsWorkWeek if year == `year' /*& valid_labHrsWorkWeek <= 65*/, width(1)  color(red%30) ///
		legend(label(2 "UKHLS"))), ///
		title("Weekly Hours Worked") ///
		subtitle("`year', age 16-65") ///
		xtitle("Hours per week", size(small)) ///
		ytitle("Density", size(small)) ///
		xlabel(,labsize(small)) ///
		ylabel(0(`steps')`max_y', labsize(small)) ///
		legend(size(small)) ///
		graphregion(color(white)) ///
	note("Note: Statistics calculated on sample of employed and self-employed individuals.", ///
	size(vsmall))		
		
	graph export ///
		"$dir_output_files/hours_worked/validation_${country}_hours_worked_hist_`year'_ln.png", ///
		replace width(2400) height(1350) 
	
	drop d_sim v1 max_d_sim max_value
	
}

********************************************************************************
* 2.1.1 : Histograms by year - ages 16-65, by gender
********************************************************************************

* Female 

* Prepare validation data
use year dwt labC4 valid_labHrsWorkWeek demMaleFlag  demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Select sample
keep if labC4 == 1
keep if demMaleFlag == 0 
keep if inrange(demAge,16,65)

* Prepare info needed for dynamic y axis labels 
qui sum year
local min_year = 2019
local max_year = r(max)  

forval year = `min_year'/`max_year' { 

	twoway__histogram_gen valid_labHrsWorkWeek if year == `year' , ///
		bin(60) den gen(d_valid v2)

	qui sum d_valid
	gen max_d_valid_`year' = r(max) 
	
	drop d_valid v2

}

drop labC4 demMaleFlag

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run idPers year labC4 demMaleFlag sim_labHrsWorkWeek demAge using ///
	"$dir_data/simulation_sample.dta", clear

* Merge in new valid_labHrsWorkWeek worked for top category using log normal distribution
merge 1:1 year run idPers using "$dir_data/simulation_sample_hrs_adjusted.dta"
drop _m

rename sim_labHrsWorkWeek sim_labHrsWorkWeek_orig
rename new_sim_labHrsWorkWeek sim_labHrsWorkWeek
	
* Select sample
keep if labC4 == "EmployedOrSelfEmployed"
keep if  demMaleFlag == 0
keep if inrange(demAge,16,65)

* Combine datasets
append using "$dir_data/temp_valid_stats.dta"

* Plot by year 
qui sum year
local min_year = 2019
local max_year = r(max)  

forval year = `min_year'/`max_year' {
	
	* Prepare info needed for dynamic y axis labels 
	twoway__histogram_gen sim_labHrsWorkWeek if year == `year', bin(60) ///
		den gen(d_sim v1)

	qui sum d_sim 
	gen max_d_sim = r(max)

	gen max_value = max_d_valid_`year' if max_d_valid_`year' > max_d_sim 
	replace max_value = max_d_sim if max_value == . 

	sum max_value 
	local max_y = 1.25*r(max)
	local steps = `max_y'/2
	
	twoway (hist sim_labHrsWorkWeek if year == `year', width(1) ///
		color(green%30) legend(label(1 "SimPaths"))) ///
	(hist valid_labHrsWorkWeek if year == `year', width(1) color(red%30) ///
		legend(label(2 "UKHLS"))), ///
		title("Weekly Hours Worked") ///
		subtitle("`year', females, ages 16-65") ///
		xtitle("Hours per week", size(small)) ///
		ytitle("Density", size(small)) ///
		xlabel(,labsize(small)) ///
		ylabel(0(`steps')`max_y', labsize(small)) ///
		legend(size(small)) ///
		graphregion(color(white)) ///
	note("Note: Statistics calculated on sample of employed and self-employed females.", ///
		size(vsmall)) 
		
	graph export ///
		"$dir_output_files/hours_worked/validation_${country}_hours_worked_hist_`year'_female_ln.png", ///
		replace width(2400) height(1350) 
	
	drop d_sim v1 max_d_sim max_value
	
}


* Male 

* Prepare validation data
use year dwt labC4 valid_labHrsWorkWeek demMaleFlag  demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Select sample
keep if labC4 == 1
keep if demMaleFlag == 1 
keep if inrange(demAge,16,65)

* Prepare info needed for dynamic y axis labels 
qui sum year
local min_year = 2019
local max_year = r(max)  

forval year = `min_year'/`max_year' { 

	twoway__histogram_gen valid_labHrsWorkWeek if year == `year' , ///
		bin(60) den gen(d_valid v2)

	qui sum d_valid
	gen max_d_valid_`year' = r(max) 
	
	drop d_valid v2

}

drop labC4 demMaleFlag

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run idPers year labC4 demMaleFlag sim_labHrsWorkWeek demAge using ///
	"$dir_data/simulation_sample.dta", clear

* Merge in new valid_labHrsWorkWeek worked for top category using log normal distribution
merge 1:1 year run idPers using "$dir_data/simulation_sample_hrs_adjusted.dta"
drop _m

rename sim_labHrsWorkWeek sim_labHrsWorkWeek_orig
rename new_sim_labHrsWorkWeek sim_labHrsWorkWeek

* Select sample
keep if labC4 == "EmployedOrSelfEmployed"
keep if  demMaleFlag == 1
keep if inrange(demAge,16,65)

* Combine datasets
append using "$dir_data/temp_valid_stats.dta"

qui sum year
local min_year = 2019
local max_year = r(max)  

forval year = `min_year'/`max_year' {
	
	* Prepare info needed for dynamic y axis labels 
	twoway__histogram_gen sim_labHrsWorkWeek if year == `year', bin(60) ///
		den gen(d_sim v1)

	qui sum d_sim 
	gen max_d_sim = r(max)

	gen max_value = max_d_valid_`year' if max_d_valid_`year' > max_d_sim 
	replace max_value = max_d_sim if max_value == . 

	sum max_value 
	local max_y = 1.25*r(max)
	local steps = `max_y'/2
	
	twoway (hist sim_labHrsWorkWeek if year == `year' , width(1) ///
		color(green%30) legend(label(1 "SimPaths"))) ///
	(hist valid_labHrsWorkWeek if year == `year' , width(1) color(red%30) ///
		legend(label(2 "UKHLS"))), ///
		title("Weekly Hours Worked") ///
		subtitle("`year', males, ages 16-65") ///
		xtitle("Hours per week", size(small)) ///
		ytitle("Density", size(small)) ///
		xlabel(,labsize(small)) ///
		ylabel(0(`steps')`max_y', labsize(small)) ///
		legend(size(small)) ///
		graphregion(color(white)) ///
	note("Note: Statistics calculated on sample of employed and self-employed males.", ///
		size(vsmall)) 
	
		
	graph export ///
		"$dir_output_files/hours_worked/validation_${country}_hours_worked_hist_`year'_male_ln.png", ///
		replace width(2400) height(1350) 
	
	drop d_sim v1 max_d_sim max_value
}	
	
graph drop _all 	


/*
********************************************************************************
* LOG-NORMAL HETEROGENIETY WITH SPIKE AT 40
********************************************************************************

********************************************************************************
* 0 : IMPUTATION OF valid_labHrsWorkWeek WORK FOR THOSE IN TOP CATEGORY, LOG NORMAL WITH MASS
********************************************************************************
/*
* Load data  
use year dwt labC4 valid_labHrsWorkWeek  demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Select sample
keep if inrange(demAge,16,65)
keep if labC4 == 1
//remove those that report working very low >0 valid_labHrsWorkWeek 

drop labC4 

save "$dir_data/temp_valid_stats.dta", replace

twoway (histogram valid_labHrsWorkWeek, fraction color(ltblue)) 

* Top group only
keep if valid_labHrsWorkWeek > 35 & valid_labHrsWorkWeek != . 

* Proportion of the top group that work 40 valid_labHrsWorkWeek 
gen exact_40 = (valid_labHrsWorkWeek == 40)

preserve 
collapse (mean) exact_40 [aw=dwt]
local valid_share_40 = exact_40
restore

* Create log variable
gen ln_y = ln(valid_labHrsWorkWeek)

* Set truncation points (using observed range)
summarize valid_labHrsWorkWeek
local a = r(min)
local b = r(max)
local ln_a = ln(`a')
local ln_b = ln(`b')

* Estimate parameters
truncreg ln_y, ll(`ln_a') ul(`ln_b') nolog

* Results
matrix b = e(b)
local mu_hat = b[1,1]
local sigma_hat = b[1,2]
local median_est = exp(`mu_hat')
local mean_est = exp(`mu_hat' + `sigma_hat'^2/2)

disp "** RESULTS **"
disp "μ (for lnY): " round(`mu_hat', 0.001)
disp "σ (for lnY): " round(`sigma_hat', 0.001)
disp "Estimated median: " round(`median_est', 0.01)
disp "Estimated mean: " round(`mean_est', 0.01)

* Visualization
range y_plot `a' `b' 150
gen pdf_fitted = (1/(y_plot*`sigma_hat')) * ///
	normalden((ln(y_plot)-`mu_hat')/`sigma_hat') / ///
	(normal((`ln_b'-`mu_hat')/`sigma_hat') - ///
	normal((`ln_a'-`mu_hat')/`sigma_hat'))

twoway (histogram valid_labHrsWorkWeek, fraction color(ltblue)) ///
	(line pdf_fitted y_plot, color(red) lwidth(*1)), ///
    title("Truncated Log-Normal Distribution Fit") ///
    legend(order(1 "UKHLS" 2 "Fitted Distribution")) ///
    xtitle("valid_labHrsWorkWeek") ///
	ytitle("Density") ///
	graphregion(color(white)) ///
	note("Note: ", ///
		size(vsmall))	

** Apply to simulated data 
* Load simulation data 
use run idPers year labC4 sim_labHrsWorkWeek demAge using ///
	"$dir_data/simulation_sample.dta", clear

rename sim_labHrsWorkWeek labHrsWorkWeek


* Select sample
keep if labC4 == "EmployedOrSelfEmployed" & inrange(demAge,16,65)

* Observations to be adjusted  
gen top = 1 if labHrsWorkWeek > 35  

* Add log-normal heterogeneity 
gen new_labHrsWorkWeek = labHrsWorkWeek 

* Calculate the CDF bounds once
local Fa = normal((`ln_a' - `mu_hat')/`sigma_hat')
local Fb = normal((`ln_b' - `mu_hat')/`sigma_hat')

* For observations with valid_labHrsWorkWeek >= 36, generate random values
replace new_labHrsWorkWeek = exp(`mu_hat' + `sigma_hat' * ///
    invnormal(`Fa' + runiform()*(`Fb' - `Fa'))) ///
    if top == 1
*/
	
	
use "$dir_data/simulation_sample_hrs_adjusted", clear	
	
replace new_labHrsWorkWeek = round(new_labHrsWorkWeek,1)	
	
twoway(hist new_labHrsWorkWeek)	
	
	
* Proportion at 40
gen sim_exact_40 = (new_labHrsWorkWeek == 40)

preserve 	
mean sim_exact_40
local sim_40_share = el(r(table),1,1)
restore

local add_to_40 = `valid_share_40' - `sim_40_share'

* Identify candidates (36-39 valid_labHrsWorkWeek)
gen candidate = inrange(new_labHrsWorkWeek, 36, 39) if top == 1

* Calculate how many to convert
count if top == 1
local total_top = r(N)
local num_to_convert = round(`total_top' * `add_to_40')

* Randomly select candidates
gen u = runiform() if candidate == 1
gsort u
gen convert = (_n <= `num_to_convert') if candidate == 1

* Apply conversion
replace new_labHrsWorkWeek = 40 if convert == 1

* Clean up
drop u convert candidate

twoway hist new_labHrsWorkWeek

rename new_labHrsWorkWeek new_sim_labHrsWorkWeek

save "$dir_data/simulation_sample_hrs_adjusted_40.dta", replace

graph drop _all 

********************************************************************************
* 1 : Mean values over time
********************************************************************************

********************************************************************************
* 1.1 : Mean values over time - Ages 16-65
********************************************************************************

* Prepare validation data
use year dwt labC4 valid_labHrsWorkWeek  demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Select sample
keep if labC4 == 1

keep if inrange(demAge,16,65)

* Compute mean
collapse (mean) valid_labHrsWorkWeek [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year idPers labC4 sim_labHrsWorkWeek demAge using ///
	"$dir_data/simulation_sample.dta", clear
		
* Select sample
keep if inrange(demAge,16,65)		
		
* Merge in new valid_labHrsWorkWeek worked for top category using log normal distribution
merge 1:1 year run idPers using "$dir_data/simulation_sample_hrs_adjusted_40.dta"
keep if _m == 3 
drop _m 

rename sim_labHrsWorkWeek sim_labHrsWorkWeek_orig
rename new_sim_labHrsWorkWeek sim_labHrsWorkWeek

* Keep only employed individuals
keep if labC4 == "EmployedOrSelfEmployed"

twoway hist sim_labHrsWorkWeek

* Compute mean and sd 
collapse (mean) sim_labHrsWorkWeek, by(run year)

collapse (mean) sim_labHrsWorkWeek ///
		 (sd) sim_labHrsWorkWeek_sd = sim_labHrsWorkWeek ///
		 , by(year)

* Approx 95% confidence interval 	 
foreach varname in sim_labHrsWorkWeek {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

save "$dir_data/temp_sim_mean_ln_40.dta", replace

* Combine datasets
merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea sim_labHrsWorkWeek_high sim_labHrsWorkWeek_low year, sort color(green%20) ///
	legend(label(1 "SimPaths"))) ///
(line valid_labHrsWorkWeek year, sort color(green) legend(label(2 "UKHLS"))), ///
	title("Average Weekly Hours Worked") ///
	subtitle("Ages 16-65 ") ///
	xtitle("Year", size(small)) ///
	ytitle("Hours per week", size(small)) ///
	ylabel(33 [2] 40 ,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///	
	graphregion(color(white)) ///
	note("Note: Statistics calculated on sample of working age employed and self-employed individuals. Log-normal heterogeneity" "imposed on top category with a mass at 40.", ///
	size(vsmall))		

* Save figure
graph export ///
"$dir_output_files/hours_worked/validation_${country}_hours_worked_ts_16_65_both_ln_40.jpg", ///
	replace width(2560) height(1440) quality(100)
	
	
********************************************************************************
* 1.2 : Mean values over time - Ages 18-65, by gender
********************************************************************************

* Males

* Prepare validation data
use year dwt labC4 valid_labHrsWorkWeek  demAge demMaleFlag using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Select sample
keep if labC4 == 1

keep if inrange(demAge,16,65)
keep if demMaleFlag == 1

* Compute mean 
collapse (mean) valid_labHrsWorkWeek [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year idPers labC4 sim_labHrsWorkWeek demAge demMaleFlag using ///
	"$dir_data/simulation_sample.dta", clear
		
* Merge in new valid_labHrsWorkWeek worked for top category using log normal distribution
merge 1:1 year run idPers using "$dir_data/simulation_sample_hrs_adjusted_40.dta"
keep if _m == 3 
drop _m

rename sim_labHrsWorkWeek sim_labHrsWorkWeek_orig
rename new_sim_labHrsWorkWeek sim_labHrsWorkWeek

twoway hist sim_labHrsWorkWeek

* Select sample 
keep if  demMaleFlag == 
keep if labC4 == "EmployedOrSelfEmployed"
keep if inrange(demAge,16,65)		

* Compute mean and sd
collapse (mean) sim_labHrsWorkWeek, by(run year)

collapse (mean) sim_labHrsWorkWeek ///
		 (sd) sim_labHrsWorkWeek_sd = sim_labHrsWorkWeek ///
		 , by(year)
		 
* Approx 95% confidence interval		 
foreach varname in sim_labHrsWorkWeek {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

* Combine datasets
merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure 
twoway (rarea sim_labHrsWorkWeek_high sim_labHrsWorkWeek_low year, sort ///
	color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_labHrsWorkWeek year, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	title("Average Weekly Hours Worked") ///
	subtitle("Males, ages 16-65") ///
	xtitle("Year", size(small)) ///
	ytitle("Hours per week", size(small)) ///
	ylabel(35 [2] 43 ,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///	
	graphregion(color(white)) ///
	note("Note: Statistics calculated on sample of working age employed and self-employed individuals. Log-normal heterogeneity" "imposed on top category with a mass at 40.", ///
	size(vsmall))		

* Save figure
graph export ///
"$dir_output_files/hours_worked/validation_${country}_hours_worked_ts_16_65_male_ln_40.jpg", ///
	replace width(2560) height(1440) quality(100)
		

* Females 

* Prepare validation data
use year dwt labC4 valid_labHrsWorkWeek demAge demMaleFlag using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Select sample
keep if labC4 == 1
keep if inrange(demAge,16,65)
keep if demMaleFlag == 0

* Compute mean
collapse (mean) valid_labHrsWorkWeek [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year idPers labC4 sim_labHrsWorkWeek demAge demMaleFlag using ///
	"$dir_data/simulation_sample.dta", clear
		
* Select sample
keep if inrange(demAge,16,65)		

* Merge in new valid_labHrsWorkWeek worked for top category using log normal distribution
merge 1:1 year run idPers using "$dir_data/simulation_sample_hrs_adjusted_40.dta"
drop _m

rename sim_labHrsWorkWeek sim_labHrsWorkWeek_orig
rename new_sim_labHrsWorkWeek sim_labHrsWorkWeek

* Select sample
keep if  demMaleFlag == 0
keep if labC4 == "EmployedOrSelfEmployed"
keep if inrange(demAge,16,65)	

twoway hist sim_labHrsWorkWeek
	
* Compute mean and sd
collapse (mean) sim_labHrsWorkWeek, by(run year)

collapse (mean) sim_labHrsWorkWeek ///
		 (sd) sim_labHrsWorkWeek_sd = sim_labHrsWorkWeek ///
		 , by(year)
		 
* Approx 95% confidnece interval 		 
foreach varname in sim_labHrsWorkWeek {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

 *Combine datasets
merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea sim_labHrsWorkWeek_high sim_labHrsWorkWeek_low year, ///
	sort color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_labHrsWorkWeek year, sort color(green) legend(label(2 "UKHLS"))), ///
	title("Average Weekly Hours Worked") ///
	subtitle("Females, ages 16-65") ///
	xtitle("Year", size(small)) ///
	ytitle("Hours per week", size(small)) ///
	ylabel(,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///	
	graphregion(color(white)) ///
	note("Note: Statistics calculated on sample of working age employed and self-employed individuals. Log-normal heterogeneity" "imposed on top category with a mass at 40.", ///
	size(vsmall))		

* Save figure
graph export ///
"$dir_output_files/hours_worked/validation_${country}_hours_worked_ts_16_65_female_ln_40.jpg", ///
	replace width(2560) height(1440) quality(100)
		
		

********************************************************************************
* 2 : Histograms by year
********************************************************************************

********************************************************************************
* 2.1 : Histograms by year - ages 16-65
********************************************************************************

* Prepare validation data
use year dwt labC4 valid_labHrsWorkWeek  demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Select sample
keep if labC4 == 1
keep if inrange(demAge,16,65)

* Prepare info needed for dynamic y axis labels 
qui sum year
local min_year = 2019
local max_year = r(max)  

forval year = `min_year'/`max_year' { 

	twoway__histogram_gen valid_labHrsWorkWeek if year == `year' , ///
		bin(60) den gen(d_valid v2)

	qui sum d_valid
	gen max_d_valid_`year' = r(max) 
	
	drop d_valid v2

}

drop labC4

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year labC4 idPers sim_labHrsWorkWeek demAge using ///
	"$dir_data/simulation_sample.dta", clear
	
* Select sample
keep if labC4 == "EmployedOrSelfEmployed"
keep if inrange(demAge,16,65)
	
* Merge in new valid_labHrsWorkWeek worked for top category using log normal distribution
merge 1:1 year run idPers using "$dir_data/simulation_sample_hrs_adjusted_40.dta"
keep if _m == 3 
drop _m

rename sim_labHrsWorkWeek sim_labHrsWorkWeek_orig
rename new_sim_labHrsWorkWeek sim_labHrsWorkWeek

twoway hist sim_labHrsWorkWeek

* Combine datasets
append using "$dir_data/temp_valid_stats.dta"

qui sum year
local min_year = 2019
local max_year = r(max)  

forval year = `min_year'/`max_year' {
	
	* Prepare info needed for dynamic y axis labels 
	twoway__histogram_gen sim_labHrsWorkWeek if year == `year', bin(60) ///
		den gen(d_sim v1)

	qui sum d_sim 
	gen max_d_sim = r(max)

	gen max_value = max_d_valid_`year' if max_d_valid_`year' > max_d_sim 
	replace max_value = max_d_sim if max_value == . 

	sum max_value 
	local max_y = 1.25*r(max)
	local steps = `max_y'/2
	
	twoway (hist sim_labHrsWorkWeek if year == `year', width(1) ///
		color(green%30) legend(label(1 "SimPaths"))) ///
	(hist valid_labHrsWorkWeek if year == `year', width(1)  color(red%30) ///
		legend(label(2 "UKHLS"))), ///
		title("Weekly Hours Worked") ///
		subtitle("Ages 16-65, `year'") ///
		xtitle("Hours per week", size(small)) ///
		ytitle("Density", size(small)) ///
		xlabel(,labsize(small)) ///
		ylabel(0(`steps')`max_y', labsize(small)) ///
		legend(size(small)) ///
		graphregion(color(white)) ///
	note("Note: Statistics calculated on sample of employed and self-employed individuals.", ///
	size(vsmall))		
		
	graph export ///
		"$dir_output_files/hours_worked/validation_${country}_hours_worked_hist_`year'_ln_40.png", ///
		replace width(2400) height(1350) 
	
	drop d_sim v1 max_d_sim max_value
	
}

********************************************************************************
* 2.1 : Histograms by year - ages 16-65, by gender
********************************************************************************

* Female 

* Prepare validation data
use year dwt labC4 valid_labHrsWorkWeek demMaleFlag  demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Select sample
keep if labC4 == 1
keep if demMaleFlag == 0 
keep if inrange(demAge,16,65)


* Prepare info needed for dynamic y axis labels 
qui sum year
local min_year = 2019
local max_year = r(max)  

forval year = `min_year'/`max_year' { 

	twoway__histogram_gen valid_labHrsWorkWeek if year == `year' , ///
		bin(60) den gen(d_valid v2)

	qui sum d_valid
	gen max_d_valid_`year' = r(max) 
	
	drop d_valid v2

}

drop labC4 demMaleFlag

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run idPers year labC4 demMaleFlag sim_labHrsWorkWeek demAge using ///
	"$dir_data/simulation_sample.dta", clear

* Merge in new valid_labHrsWorkWeek worked for top category using log normal distribution
merge 1:1 year run idPers using "$dir_data/simulation_sample_hrs_adjusted_40.dta"
keep if _m == 3 
drop _m

rename sim_labHrsWorkWeek sim_labHrsWorkWeek_orig
rename new_sim_labHrsWorkWeek sim_labHrsWorkWeek
	
* Select sample
keep if labC4 == "EmployedOrSelfEmployed"
keep if  demMaleFlag == 0
keep if inrange(demAge,16,65)

* Combine datasets
append using "$dir_data/temp_valid_stats.dta"

* Plot by year 
qui sum year
local min_year = 2019
local max_year = r(max)  

forval year = `min_year'/`max_year' {
	
	* Prepare info needed for dynamic y axis labels 
	twoway__histogram_gen sim_labHrsWorkWeek if year == `year', bin(60) ///
		den gen(d_sim v1)

	qui sum d_sim 
	gen max_d_sim = r(max)

	gen max_value = max_d_valid_`year' if max_d_valid_`year' > max_d_sim 
	replace max_value = max_d_sim if max_value == . 

	sum max_value 
	local max_y = 1.25*r(max)
	local steps = `max_y'/2
	
	twoway (hist sim_labHrsWorkWeek if year == `year', width(1) ///
		color(green%30) legend(label(1 "SimPaths"))) ///
	(hist valid_labHrsWorkWeek if year == `year', width(1) color(red%30) ///
		legend(label(2 "UKHLS"))), ///
		title("Weekly Hours Worked") ///
		subtitle("`year', females, ages 16-65") ///
		xtitle("Hours per week", size(small)) ///
		ytitle("Density", size(small)) ///
		xlabel(,labsize(small)) ///
		ylabel(0(`steps')`max_y', labsize(small)) ///
		legend(size(small)) ///
		graphregion(color(white)) ///
	note("Note: Statistics calculated on sample of employed and self-employed females.", ///
		size(vsmall)) 
		
	graph export ///
		"$dir_output_files/hours_worked/validation_${country}_hours_worked_hist_`year'_female_ln_40.png", ///
		replace width(2400) height(1350) 
	
	drop d_sim v1 max_d_sim max_value
	
}


* Male 

* Prepare validation data
use year dwt labC4 valid_labHrsWorkWeek demMaleFlag  demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Select sample
keep if labC4 == 1
keep if demMaleFlag == 1 
keep if inrange(demAge,16,65)


* Prepare info needed for dynamic y axis labels 
qui sum year
local min_year = 2019
local max_year = r(max)  

forval year = `min_year'/`max_year' { 

	twoway__histogram_gen valid_labHrsWorkWeek if year == `year' , ///
		bin(60) den gen(d_valid v2)

	qui sum d_valid
	gen max_d_valid_`year' = r(max) 
	
	drop d_valid v2

}

drop labC4 demMaleFlag

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run idPers year labC4 demMaleFlag sim_labHrsWorkWeek demAge using ///
	"$dir_data/simulation_sample.dta", clear

* Merge in new valid_labHrsWorkWeek worked for top category using log normal distribution
merge 1:1 year run idPers using "$dir_data/simulation_sample_hrs_adjusted_40.dta"
keep if _m == 3 
drop _m

rename sim_labHrsWorkWeek sim_labHrsWorkWeek_orig
rename new_sim_labHrsWorkWeek sim_labHrsWorkWeek

* Select sample
keep if labC4 == "EmployedOrSelfEmployed"
keep if  demMaleFlag == 1
keep if inrange(demAge,16,65)

* Combine datasets
append using "$dir_data/temp_valid_stats.dta"

qui sum year
local min_year = 2019
local max_year = r(max)  

forval year = `min_year'/`max_year' {
	
	* Prepare info needed for dynamic y axis labels 
	twoway__histogram_gen sim_labHrsWorkWeek if year == `year', bin(60) ///
		den gen(d_sim v1)

	qui sum d_sim 
	gen max_d_sim = r(max)

	gen max_value = max_d_valid_`year' if max_d_valid_`year' > max_d_sim 
	replace max_value = max_d_sim if max_value == . 

	sum max_value 
	local max_y = 1.25*r(max)
	local steps = `max_y'/2
	
	twoway (hist sim_labHrsWorkWeek if year == `year' , width(1) ///
		color(green%30) legend(label(1 "SimPaths"))) ///
	(hist valid_labHrsWorkWeek if year == `year' , width(1) color(red%30) ///
		legend(label(2 "UKHLS"))), ///
		title("Weekly Hours Worked") ///
		subtitle("`year', males, ages 16-65") ///
		xtitle("Hours per week", size(small)) ///
		ytitle("Density", size(small)) ///
		xlabel(,labsize(small)) ///
		ylabel(0(`steps')`max_y', labsize(small)) ///
		legend(size(small)) ///
		graphregion(color(white)) ///
	note("Note: Statistics calculated on sample of employed and self-employed males.", ///
		size(vsmall)) 
	
		
	graph export ///
		"$dir_output_files/hours_worked/validation_${country}_hours_worked_hist_`year'_male_ln_40.png", ///
		replace width(2400) height(1350) 
	
	drop d_sim v1 max_d_sim max_value
}	
	
	
graph drop _all 	
