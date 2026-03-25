/*******************************************************************************
* PROJECT:  		SimPaths UK 
* SECTION:			Validation
* OBJECT: 			Disability 
* AUTHORS:			Ashley Burdett 
* LAST UPDATE:		Jan 2026
* COUNTRY: 			UK 
********************************************************************************
* NOTES: 			
*******************************************************************************/

********************************************************************************
* 1 : Mean values over time
********************************************************************************

********************************************************************************
* 1.1 : Mean values over time, working age (18-65)
********************************************************************************

* Prepare validation data 
use year demAge dwt valid_healthDsblLongtermFlag using ///
	"$dir_data/ukhls_validation_sample.dta", clear
	
keep if inrange(demAge,18,65)	
	
collapse (mean) valid_healthDsblLongtermFlag [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

	
* Prepare simulation data 
use year demAge sim_healthDsblLongtermFlag run using ///
	"$dir_data/simulation_sample.dta", clear

keep if inrange(demAge,18,65)		
	
collapse (mean) sim_healthDsblLongtermFlag, by(run year)

collapse (mean) sim_healthDsblLongtermFlag ///
	(sd) sim_healthDsblLongtermFlag_sd = sim_healthDsblLongtermFlag, ///
	by(year)

gen sim_healthDsblLongtermFlag_high = ///
	sim_healthDsblLongtermFlag + 1.96*sim_healthDsblLongtermFlag_sd
gen sim_healthDsblLongtermFlag_low = ///
	sim_healthDsblLongtermFlag - 1.96*sim_healthDsblLongtermFlag_sd

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen


* Plot figure 
twoway (rarea sim_healthDsblLongtermFlag_high ///
	sim_healthDsblLongtermFlag_low year, sort color(green%20) ///
	legend(label(1 "Simulated "))) ///
(line valid_healthDsblLongtermFlag year, sort color(green) ///
	legend(label(2 "UKHLS "))), ///
	title("Disabled/Long-term Sick ") ///
	subtitle("Ages 18-65") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small))  ///
	graphregion(color(white)) ///
	note("Notes: ", size(vsmall)) 
	
graph export ///
"$dir_output_files/disability/validation_${country}_disability_ts_18_65_both.jpg", ///
	replace width(2400) height(1350) quality(100)
		
	
********************************************************************************
* 1.2 : Mean values over time, working age (18-65), by gender
********************************************************************************

* Prepare validation data 
use year demAge dwt valid_healthDsblLongtermFlag demMaleFlag using ///
	"$dir_data/ukhls_validation_sample.dta", clear
	
keep if inrange(demAge,18,65)		
	
collapse (mean) valid_healthDsblLongtermFlag [aw = dwt], by(year demMaleFlag)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulation data 
use year demAge sim_healthDsblLongtermFlag run demMaleFlag using ///
	"$dir_data/simulation_sample.dta", clear
	
keep if inrange(demAge,18,65)		

collapse (mean) sim_healthDsblLongtermFlag, by(run year demMaleFlag)

collapse (mean) sim_healthDsblLongtermFlag ///
	(sd) sim_healthDsblLongtermFlag_sd = sim_healthDsblLongtermFlag, ///
	by(year demMaleFlag)

gen sim_healthDsblLongtermFlag_high = sim_healthDsblLongtermFlag + ///
	1.96*sim_healthDsblLongtermFlag_sd
	
gen sim_healthDsblLongtermFlag_low = sim_healthDsblLongtermFlag - ///
	1.96*sim_healthDsblLongtermFlag_sd

merge 1:1 year demMaleFlag using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure 
twoway (rarea sim_healthDsblLongtermFlag_high ///
	sim_healthDsblLongtermFlag_low year if demMaleFlag == 0, ///
	sort color(green%20) legend(label(1 "Female, SimPaths"))) ///
(line valid_healthDsblLongtermFlag year if demMaleFlag == 0, sort color(green) ///
	legend(label(2 "Female, UKHLS "))) ///
	(rarea sim_healthDsblLongtermFlag_high sim_healthDsblLongtermFlag_low ///
	year if demMaleFlag == 1, sort color(red%20) ///
	legend(label(3 "Male, SimPaths"))) ///
(line valid_healthDsblLongtermFlag year if demMaleFlag == 1, sort color(red) ///
	legend(label(4 "Male, UKHLS"))), ///
	title("Disabled/Long-term Sick ") ///
	subtitle("Ages 18-65") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small))  ///
	graphregion(color(white)) ///
	note("Notes: ", size(vsmall)) 

graph export ///
"$dir_output_files/disability/validation_${country}_disability_ts_18_65_male_female.jpg", ///
	replace width(2560) height(1440) quality(100)


********************************************************************************
* 1.2 : Mean values over time, by age group
********************************************************************************

* Prepare validation data 
use year demAge dwt valid_healthDsblLongtermFlag demMaleFlag demAge ///
	ageGroup using "$dir_data/ukhls_validation_sample.dta", clear
	
drop if ageGroup == 0 

collapse (mean) valid_healthDsblLongtermFlag [aw=dwt], by(ageGroup year)

drop if missing(ageGroup)

reshape wide valid_healthDsblLongtermFlag, i(year) j(ageGroup)

forvalues i = 1(1)8 {
	
	rename valid_healthDsblLongtermFlag`i' ///
		valid_healthDsblLongtermFlag_`i'

}

save "$dir_data/temp_valid_stats_full.dta", replace


* Prepare simulation data
use run year sim_healthDsblLongtermFlag ageGroup using ///
	"$dir_data/simulation_sample.dta", clear

collapse (mean) sim_healthDsblLongtermFlag, by(ageGroup run year)

drop if missing(ageGroup)

reshape wide sim_healthDsblLongtermFlag, i(year run) j(ageGroup)

forvalues i = 1(1)8 {
	
	rename sim_healthDsblLongtermFlag`i' sim_healthDsblLongtermFlag_`i'

}

collapse (mean) sim_healthDsblLongtermFlag*  ///
	(sd) sd_sim_healthDsblLongtermFlag_1 = sim_healthDsblLongtermFlag_1 ///
		 sd_sim_healthDsblLongtermFlag_2 = sim_healthDsblLongtermFlag_2 ///
		 sd_sim_healthDsblLongtermFlag_3 = sim_healthDsblLongtermFlag_3 ///
	     sd_sim_healthDsblLongtermFlag_4 = sim_healthDsblLongtermFlag_4 ///
		 sd_sim_healthDsblLongtermFlag_5 = sim_healthDsblLongtermFlag_5 ///
		 sd_sim_healthDsblLongtermFlag_6 = sim_healthDsblLongtermFlag_6 ///
		 sd_sim_healthDsblLongtermFlag_7 = sim_healthDsblLongtermFlag_7 ///
		 sd_sim_healthDsblLongtermFlag_8 = sim_healthDsblLongtermFlag_8 ///
		 , by(year)
		 
forvalues i = 1(1)8 {

	gen sim_healthDsblLongtermFlag_`i'_h = ///
		sim_healthDsblLongtermFlag_`i' + 1.96*sd_sim_healthDsblLongtermFlag_`i'
	gen sim_healthDsblLongtermFlag_`i'_l = ///
		sim_healthDsblLongtermFlag_`i' - 1.96*sd_sim_healthDsblLongtermFlag_`i'

}

recast double year 

merge 1:1 year using "$dir_data/temp_valid_stats_full.dta", keep(3) nogen

* Plot figures
foreach vble in "healthDsblLongtermFlag" {
	
	twoway (rarea sim_`vble'_1_h sim_`vble'_1_l year, ///
		sort color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line valid_`vble'_1 year, sort legend(label(2 "UKHLS"))), ///
		title("Age 15-19") ///
		name(`vble'_1, replace) ///
		ylabel(, labsize(vsmall)) ///
		xlabel(, labsize(vsmall)) ///
		ytitle("Share", size(small)) ///
		xtitle("Year", size(small)) ///
		legend(size(small)) ///
		graphregion(color(white))
	
	twoway (rarea sim_`vble'_2_h sim_`vble'_2_l year, ///
		sort color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
		(line valid_`vble'_2 year, sort legend(label(2 "UKHLS"))), ///
		title("Age 20-24") ///
		name(`vble'_2, replace) ///
		ylabel(, labsize(vsmall)) ///
		xlabel(, labsize(vsmall)) ///
		ytitle("Share", size(small)) ///
		xtitle("Year", size(small)) ///
		legend(size(small)) ///
		graphregion(color(white))
	
	twoway (rarea sim_`vble'_3_h sim_`vble'_3_l year, ///
		sort color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
		(line valid_`vble'_3 year, sort legend(label(2 "UKHLS"))), ///
		title("Age 25-29") ///
		name(`vble'_3, replace) ///
		ylabel(, labsize(vsmall)) ///
		xlabel(, labsize(vsmall)) ///
		ytitle("Share", size(small)) ///
		xtitle("Year", size(small)) ///
		legend(size(small)) ///
		graphregion(color(white)) ///
	graphregion(color(white))
	
	twoway (rarea sim_`vble'_4_h sim_`vble'_4_l year, ///
		sort color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
		(line valid_`vble'_4 year, sort legend(label(2 "UKHLS"))), ///
		title("Age 30-34") ///
		name(`vble'_4, replace) ///
		ylabel(, labsize(vsmall)) ///
		xlabel(, labsize(vsmall)) ///
		ytitle("Share", size(small)) ///
		xtitle("Year", size(small)) ///
		legend(size(small)) ///
		graphregion(color(white)) ///
	graphregion(color(white))
	
	twoway (rarea sim_`vble'_5_h sim_`vble'_5_l year, ///
		sort color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
		(line valid_`vble'_5 year, sort legend(label(2 "UKHLS"))), ///
		title("Age 35-39") ///
		name(`vble'_5, replace) ///
		ylabel(, labsize(vsmall)) ///
		xlabel(, labsize(vsmall)) ///
		ytitle("Share", size(small)) ///
		xtitle("Year", size(small)) ///
		legend(size(small)) ///
		graphregion(color(white)) ///
	graphregion(color(white)) 
	
	twoway (rarea sim_`vble'_6_h sim_`vble'_6_l year, ///
		sort color(red%20) legend(label(1 "SimPaths") position(6) ///
		rows(1))) ///
		(line valid_`vble'_6 year, sort legend(label(2 "UKHLS"))), ///
		title("Age 40-59") ///
		name(`vble'_6, replace) ///
		ylabel(, labsize(vsmall)) ///
		xlabel(, labsize(vsmall)) ///
		ytitle("Share", size(small)) ///
		xtitle("Year", size(small)) ///
		legend(size(small)) ///
		graphregion(color(white)) ///
	graphregion(color(white))
	
	twoway (rarea sim_`vble'_7_h sim_`vble'_7_l year, ///
		sort color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
		(line valid_`vble'_7 year, sort legend(label(2 "UKHLS"))), ///
		title("Age 60-79") ///
		name(`vble'_7, replace) ///
		ylabel(, labsize(vsmall)) ///
		xlabel(, labsize(vsmall)) ///
		ytitle("Share", size(small)) ///
		xtitle("Year", size(small)) ///
		legend(size(small)) ///
		graphregion(color(white)) ///
	graphregion(color(white))
	
	twoway (rarea sim_`vble'_8_h sim_`vble'_8_l year, ///
		sort color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
		(line valid_`vble'_8 year, sort legend(label(2 "UKHLS"))), ///
		title("Age 80-100") ///
		name(`vble'_8, replace) ///
		ylabel(, labsize(vsmall)) ///
		xlabel(, labsize(vsmall)) ///
		ytitle("Share", size(small)) ///
		xtitle("Year", size(small)) ///
		legend(size(small)) ///
		graphregion(color(white)) ///
	graphregion(color(white))
	
}

grc1leg healthDsblLongtermFlag_1 healthDsblLongtermFlag_2 ///
	healthDsblLongtermFlag_3 healthDsblLongtermFlag_4 ///
	healthDsblLongtermFlag_5 healthDsblLongtermFlag_6 ///
	healthDsblLongtermFlag_7 healthDsblLongtermFlag_8, ///
	title("Disabled/Long-term Sick by Age Group") ///
	legendfrom(healthDsblLongtermFlag_1) ///
	graphregion(color(white)) ///
	ycomm ///
	note("Notes:", size(vsmall))

graph export ///
"$dir_output_files/disability/validation_${country}_disability_ts_age_groups_both.jpg", ///
	replace width(2400) height(1350) quality(100)

	
graph drop _all
