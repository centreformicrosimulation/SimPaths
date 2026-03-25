/*******************************************************************************
* PROJECT:  		SimPaths UK 
* SECTION:			Validation
* OBJECT: 			Health
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
* 1.1 : Mean values over time - Self rated health, 16-65
********************************************************************************

use year dwt valid_healthSelfRated demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear

keep if inrange(demAge,16,65)	
	
gen health = valid_healthSelfRated 

collapse (mean) health [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare Simulated data
use run year sim_healthSelfRated demAge run using ///
	"$dir_data/simulation_sample.dta", clear
	
keep if inrange(demAge,16,65)	

gen healthSelfRated = sim_healthSelfRated

collapse (mean) healthSelfRated, by(year run)

collapse (mean) healthSelfRated ///
		 (sd) healthSelfRated_sd = healthSelfRated ///
		 , by(year)		 

* Compute 95% confidence interval 		 
foreach varname in healthSelfRated {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea healthSelfRated_high healthSelfRated_low year, ///
	sort color(green%20) legend(label(1 "SimPaths"))) ///
(line health year, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	title("Self Rated Health") ///
	subtitle("Ages 16-65") ///
	xtitle("Year", size(small)) ///
	ytitle("Score", size(small)) ///
	ylabel(,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: The health variable is a self-assessed variable and follows a 5-point Likert scale (1 = poor, ..., 5 = excellent). ", ///
	size(vsmall))

graph export ///
"$dir_output_files/health/validation_${country}_self_rated_ts_16_65_both.jpg", ///
	replace width(2560) height(1440) quality(100)
	
	
********************************************************************************
* 1.2 : Mean values over time - Self rated health,  16-65, by gender
********************************************************************************

* Prepare validation data	
use year dwt valid_healthSelfRated demAge demMaleFlag using ///
	"$dir_data/ukhls_validation_sample.dta", clear

keep if inrange(demAge,16,65)
	
gen health = valid_healthSelfRated 

collapse (mean) health [aw = dwt], by(year demMaleFlag)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulation data
use run year sim_healthSelfRated demAge demMaleFlag run using "$dir_data/simulation_sample.dta", clear

keep if inrange(demAge,16,65)

gen healthSelfRated = sim_healthSelfRated

collapse (mean) healthSelfRated, by(year demMaleFlag run)

collapse (mean) healthSelfRated ///
		 (sd) healthSelfRated_sd = healthSelfRated ///
		 , by(year demMaleFlag)		 

* Compute 95% confidence interval 		 		 
foreach varname in healthSelfRated {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year demMaleFlag using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea healthSelfRated_high healthSelfRated_low year if ///
	demMaleFlag == 0, sort color(green%20) legend(label(1 "SimPaths"))) ///
(line health year if demMaleFlag == 0, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	subtitle("Females") ///
	name(health_female, replace) ///
	xtitle("Year", size(small)) ///
	ytitle("Score", size(small)) ///
	ylabel(3[.2]3.8,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	

twoway (rarea healthSelfRated_high healthSelfRated_low year if ///
	demMaleFlag == 1, sort color(green%20) legend(label(1 "SimPaths"))) ///
(line health year if demMaleFlag == 1, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	subtitle("Males") ///
	name(health_male, replace) ///
	xtitle("Year", size(small)) ///
	ytitle("Score", size(small)) ///
	ylabel(3[.2]3.8,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///	
	
grc1leg health_female health_male, ///
	title("Self Rated Health") ///
	subtitle("Ages 16-65") ///
	legendfrom(health_female) rows(1) ///
	graphregion(color(white)) ///
	ycomm ///
note("Notes: The health variable is a self-assessed variable and follows a 5-point Likert scale (1 = poor, ..., 5 = excellent). ", ///
	size(vsmall))

graph export ///
"$dir_output_files/health/validation_${country}_self_rated_ts_16_65_gender.jpg", ///
	replace width(2560) height(1440) quality(100)
	

********************************************************************************
* 1.3 : Mean values over time - self rated health, by age group and gender 
********************************************************************************

* Prepare validation data
use year dwt demAge demMaleFlag ageGroup valid_healthSelfRated using ///
	"$dir_data/ukhls_validation_sample.dta", clear	
	
gen health_m = valid_healthSelfRated if demMaleFlag == 1
gen health_f = valid_healthSelfRated if demMaleFlag == 0

drop if ageGroup == 0 

collapse (mean) health* [aw = dwt], by(ageGroup year)

drop if missing(ageGroup)
reshape wide health*, i(year) j(ageGroup)

forvalues i = 1(1)8 {

	rename health_f`i' health_f_`i'_valid
	rename health_m`i' health_m_`i'_valid

}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare Simulated data
use run year demMaleFlag ageGroup sim_healthSelfRated using ///
	"$dir_data/simulation_sample.dta", clear

gen health_m = sim_healthSelfRated if demMaleFlag == 1
gen health_f = sim_healthSelfRated if demMaleFlag == 0

collapse (mean) health*, by(ageGroup run year)

drop if missing(ageGroup)

reshape wide health*, i(year run) j(ageGroup)

collapse (mean) health* ///
		 (sd) health_m_1_sd = health_m1 ///
		 (sd) health_f_1_sd = health_f1 ///
		 (sd) health_m_2_sd = health_m2 ///
		 (sd) health_f_2_sd = health_f2 ///
		 (sd) health_m_3_sd = health_m3 ///
		 (sd) health_f_3_sd = health_f3 ///
		 (sd) health_m_4_sd = health_m4 ///
		 (sd) health_f_4_sd = health_f4 ///
		 (sd) health_m_5_sd = health_m5 ///
		 (sd) health_f_5_sd = health_f5 ///
		 (sd) health_m_6_sd = health_m6 ///
		 (sd) health_f_6_sd = health_f6 ///
		 (sd) health_m_7_sd = health_m7 ///
		 (sd) health_f_7_sd = health_f7 ///
		 (sd) health_m_8_sd = health_m8 ///
		 (sd) health_f_8_sd = health_f8 ///
		 , by(year)
		 
forvalues i=1(1)8 {
	
	gen health_f_`i'_sim_high = health_f`i' + 1.96*health_f_`i'_sd
	gen health_f_`i'_sim_low = health_f`i' - 1.96*health_f_`i'_sd
	gen health_m_`i'_sim_high = health_m`i' + 1.96*health_m_`i'_sd
	gen health_m_`i'_sim_low = health_m`i' - 1.96*health_m_`i'_sd	

}
		 

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
foreach vble in "health_f" "health_m" {
	
	twoway (rarea `vble'_1_sim_high `vble'_1_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_1_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 15-19") ///
		name(`vble'_1, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Score", size(small)) ///
		ylabel(3 [1] 5, labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))
		
	twoway (rarea `vble'_2_sim_high `vble'_2_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_2_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 20-24") ///
		name(`vble'_2, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Score", size(small)) ///
		ylabel(3 [1] 5, labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))
		
	twoway (rarea `vble'_3_sim_high `vble'_3_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_3_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 25-29") ///
		name(`vble'_3, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Score", size(small)) ///
		ylabel(3 [1] 5, labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))

	twoway (rarea `vble'_4_sim_high `vble'_4_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_4_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 30-34") ///
		name(`vble'_4, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Score", size(small)) ///
		ylabel(3 [1] 5, labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))

	twoway (rarea `vble'_5_sim_high `vble'_5_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_5_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 35-39") ///
		name(`vble'_5, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Score", size(small)) ///
		ylabel(3 [1] 5, labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))

	twoway (rarea `vble'_6_sim_high `vble'_6_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_6_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 40-59") ///
		name(`vble'_6, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Score", size(small)) ///
		ylabel(3 [1] 5, labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))

	twoway (rarea `vble'_7_sim_high `vble'_7_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_7_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 60-79") ///
		name(`vble'_7, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Score", size(small)) ///
		ylabel(3 [1] 5, labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))

	twoway (rarea `vble'_8_sim_high `vble'_8_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_7_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 80-100") ///
		name(`vble'_8, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Score", size(small)) ///
		ylabel(3 [1] 5, labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))
}

* Save figures
grc1leg health_f_1 health_f_2 health_f_3 health_f_4 health_f_5 ///
	health_f_6 health_f_7 health_f_8, ///
	title("Self Rated Health") ///
	subtitle("Females") ///
	legendfrom(health_f_1) ///
	graphregion(color(white)) ///
	ycomm ///
note("Notes: The health variable is a self-assessed variable and follows a 5-point Likert scale (1 = poor, ..., 5 = excellent). ", ///
	size(vsmall)) 	

graph export ///
	"$dir_output_files/health/validation_${country}_self_rated_ts_age_groups_female.jpg", ///
	replace width(2400) height(1350) quality(100)
		

grc1leg health_m_1 health_m_2 health_m_3 health_m_4 health_m_5 ///
	health_m_6 health_m_7 health_m_8, ///
	title("Self Rated Health") ///
	subtitle("Males") ///
	legendfrom(health_m_1) ///
	graphregion(color(white)) ///
	ycomm ///
note("Notes: The health variable is a self-assessed variable and follows a 5-point Likert scale (1 = poor, ..., 5 = excellent). ", ///
	size(vsmall)) 	
	
graph export ///
	"$dir_output_files/health/validation_${country}_self_rated_ts_age_groups_male.jpg", ///
	replace width(2400) height(1350) quality(100)
	
graph drop _all 
	
	
********************************************************************************
* 1.4 : Mean values over time - pcs, 16-65
********************************************************************************

use year dwt valid_healthPhysicalPcs demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear

keep if inrange(demAge,16,65)	
	
collapse (mean) valid_healthPhysicalPcs [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare Simulated data
use run year sim_healthPhysicalPcs demAge run using ///
	"$dir_data/simulation_sample.dta", clear

keep if inrange(demAge,16,65)		
	
collapse (mean) sim_healthPhysicalPcs, by(year run)

collapse (mean) sim_healthPhysicalPcs ///
		 (sd) sim_healthPhysicalPcs_sd = sim_healthPhysicalPcs ///
		 , by(year)		 

* Compute 95% confidence interval 		 
foreach varname in sim_healthPhysicalPcs {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea sim_healthPhysicalPcs_high sim_healthPhysicalPcs_low year, ///
	sort color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_healthPhysicalPcs year, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	title("Physical Health") ///
	subtitle("PCS, ages 16-65") ///
	xtitle("Year", size(small)) ///
	ytitle("Health score", size(small)) ///
	ylabel(,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: PCS is the SF-12 Physical Health Component. This is a measure of physical function ranging from 0 (low functioning)" "to 100 (high functioning).  ", ///
	size(vsmall))

graph export ///
"$dir_output_files/health/validation_${country}_pcs_ts_16_65_both.jpg", ///
	replace width(2560) height(1440) quality(100)
	
	
********************************************************************************
* 1.5 : Mean values over time - pcs,  16-65, by gender
********************************************************************************

* Prepare validation data	
use year dwt valid_healthPhysicalPcs demAge demMaleFlag using ///
	"$dir_data/ukhls_validation_sample.dta", clear

keep if inrange(demAge,16,65)	
		
collapse (mean) valid_healthPhysicalPcs [aw = dwt], by(year demMaleFlag)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulation data
use run year sim_healthPhysicalPcs demAge demMaleFlag run using ///
	"$dir_data/simulation_sample.dta", clear

keep if inrange(demAge,16,65)	

collapse (mean) sim_healthPhysicalPcs, by(year demMaleFlag run)

collapse (mean) sim_healthPhysicalPcs ///
		 (sd) sim_healthPhysicalPcs_sd = sim_healthPhysicalPcs ///
		 , by(year demMaleFlag)		 

* Compute 95% confidence interval 		 		 
foreach varname in sim_healthPhysicalPcs {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year demMaleFlag using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea sim_healthPhysicalPcs_high sim_healthPhysicalPcs_low year if ///
	demMaleFlag == 0, sort color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_healthPhysicalPcs year if demMaleFlag == 0, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	subtitle("Females") ///
	name(health_female, replace) ///
	xtitle("Year", size(small)) ///
	ytitle("Health score", size(small)) ///
	ylabel(49[0.5]52.5,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	
twoway (rarea sim_healthPhysicalPcs_high sim_healthPhysicalPcs_low year if ///
	demMaleFlag == 1, sort color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_healthPhysicalPcs year if demMaleFlag == 1, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	subtitle("Males") ///
	name(health_male, replace) ///
	xtitle("Year", size(small)) ///
	ytitle("Helath score", size(small)) ///
	ylabel(49[0.5]52.5,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///	
	
grc1leg health_female health_male, ///
	title("Physical Health") ///
	subtitle("PCS, ages 16-65") ///
	legendfrom(health_female) rows(1) ///
	graphregion(color(white)) ///
	ycomm ///
	note("Notes: PCS is the SF-12 Physical Health Component. This is a measure of physical function ranging from 0 (low functioning)" "to 100 (high functioning).  ", ///
	size(vsmall))

graph export ///
"$dir_output_files/health/validation_${country}_pcs_ts_16_65_gender.jpg", ///
	replace width(2560) height(1440) quality(100)
	
	
********************************************************************************
* 1.6 : Mean values over time - pcs,  16-65, by age group and gender 
********************************************************************************

* Prepare validation data
use year dwt demMaleFlag ageGroup valid_healthPhysicalPcs using ///
	"$dir_data/ukhls_validation_sample.dta", clear

gen health_m = valid_healthPhysicalPcs if demMaleFlag == 1
gen health_f = valid_healthPhysicalPcs if demMaleFlag == 0

drop if ageGroup == 0 

collapse (mean) health* [aw = dwt], by(ageGroup year)

drop if missing(ageGroup)
reshape wide health*, i(year) j(ageGroup)
		 
forvalues i = 1(1)8 {

	rename health_f`i' health_f_`i'_valid
	rename health_m`i' health_m_`i'_valid

}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare Simulated data
use run year demMaleFlag ageGroup sim_healthPhysicalPcs using ///
	"$dir_data/simulation_sample.dta", clear

gen health_m = sim_healthPhysicalPcs if demMaleFlag == 1
gen health_f = sim_healthPhysicalPcs if demMaleFlag == 0

collapse (mean) health*, by(ageGroup run year)
drop if missing(ageGroup)
reshape wide health*, i(year run) j(ageGroup)

collapse (mean) health* ///
		 (sd) health_m_1_sd = health_m1 ///
		 (sd) health_f_1_sd = health_f1 ///
		 (sd) health_m_2_sd = health_m2 ///
		 (sd) health_f_2_sd = health_f2 ///
		 (sd) health_m_3_sd = health_m3 ///
		 (sd) health_f_3_sd = health_f3 ///
		 (sd) health_m_4_sd = health_m4 ///
		 (sd) health_f_4_sd = health_f4 ///
		 (sd) health_m_5_sd = health_m5 ///
		 (sd) health_f_5_sd = health_f5 ///
		 (sd) health_m_6_sd = health_m6 ///
		 (sd) health_f_6_sd = health_f6 ///
		 (sd) health_m_7_sd = health_m7 ///
		 (sd) health_f_7_sd = health_f7 ///
		 (sd) health_m_8_sd = health_m8 ///
		 (sd) health_f_8_sd = health_f8 /// 
		 , by(year)
		 
* Compute 19% confidence intervals		 
forvalues i=1(1)8 {
	
	gen health_f_`i'_sim_high = health_f`i' + 1.96*health_f_`i'_sd
	gen health_f_`i'_sim_low = health_f`i' - 1.96*health_f_`i'_sd
	gen health_m_`i'_sim_high = health_m`i' + 1.96*health_m_`i'_sd
	gen health_m_`i'_sim_low = health_m`i' - 1.96*health_m_`i'_sd	

}
		 

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
foreach vble in "health_f" "health_m" {
	
	twoway (rarea `vble'_1_sim_high `vble'_1_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_1_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 15-19") ///
		name(`vble'_1, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Score", size(small)) ///
		ylabel(,labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))
		
	twoway (rarea `vble'_2_sim_high `vble'_2_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_2_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 20-24") ///
		name(`vble'_2, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Score", size(small)) ///
		ylabel(,labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))
		
	twoway (rarea `vble'_3_sim_high `vble'_3_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_3_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 25-29") ///
		name(`vble'_3, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Score", size(small)) ///
		ylabel(,labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))

	twoway (rarea `vble'_4_sim_high `vble'_4_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_4_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 30-34") ///
		name(`vble'_4, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Score", size(small)) ///
		ylabel(,labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))

	twoway (rarea `vble'_5_sim_high `vble'_5_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_5_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 35-39") ///
		name(`vble'_5, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Score", size(small)) ///
		ylabel(,labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))

	twoway (rarea `vble'_6_sim_high `vble'_6_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_6_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 40-59") ///
		name(`vble'_6, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Score", size(small)) ///
		ylabel(,labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))

	twoway (rarea `vble'_7_sim_high `vble'_7_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_7_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 60-79") ///
		name(`vble'_7, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Score", size(small)) ///
		ylabel(,labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))

	twoway (rarea `vble'_8_sim_high `vble'_8_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_8_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 80-100") ///
		name(`vble'_8, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Score", size(small)) ///
		ylabel(,labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))
}

* Save figures
grc1leg health_f_1 health_f_2 health_f_3 health_f_4 health_f_5 ///
	health_f_6 health_f_7 health_f_8, ///
	title("Physical Health") ///
	subtitle("PCS, females") ///
	legendfrom(health_f_1) ///
	graphregion(color(white)) ///
	ycomm ///
	note("Notes: PCS is the SF-12 Physical Health Component. This is a measure of physical function ranging from 0 (low functioning) to" "100 (high functioning). ", ///
	size(vsmall)) 	

graph export ///
	"$dir_output_files/health/validation_${country}_pcs_ts_all_female.jpg", ///
	replace width(2400) height(1350) quality(100)
		

grc1leg health_m_1 health_m_2 health_m_3 health_m_4 health_m_5 ///
	health_m_6 health_m_7 health_m_8, ///
	title("Physical Health") ///
	subtitle("PCS, males") ///
	legendfrom(health_m_1) ///
	graphregion(color(white)) ///
	ycomm ///
	note("Notes: PCS is the SF-12 Physical Health Component. This is a measure of physical function ranging from 0 (low functioning) to" "100 (high functioning). ", ///
	size(vsmall)) 	
	
graph export ///
	"$dir_output_files/health/validation_${country}_pcs_ts_all_male.jpg", ///
	replace width(2400) height(1350) quality(100)	
	
graph drop _all	

	
********************************************************************************
* 1.7 : Mean values over time - mcs, 16-65
********************************************************************************

use year dwt valid_healthMentalMcs demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear

keep if inrange(demAge,16,65)
	
collapse (mean) valid_healthMentalMcs [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare Simulated data
use run year sim_healthMentalMcs demAge run using ///
	"$dir_data/simulation_sample.dta", clear

keep if inrange(demAge,16,65)
	
collapse (mean) sim_healthMentalMcs, by(year run)

collapse (mean) sim_healthMentalMcs ///
		 (sd) sim_healthMentalMcs_sd = sim_healthMentalMcs ///
		 , by(year)		 

* Compute 95% confidence interval 		 
foreach varname in sim_healthMentalMcs {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea sim_healthMentalMcs_high sim_healthMentalMcs_low year, ///
	sort color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_healthMentalMcs year, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	title("Mental Health") ///
	subtitle("MCS, ages 16-65") ///
	xtitle("Year", size(small)) ///
	ytitle("Score", size(small)) ///
	ylabel(,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: mcs is the SF-12 Mental Health Component. This is a measure of Mental function ranging from 0 (low functioning)" "to 100 (high functioning).  ", ///
	size(vsmall))

graph export ///
"$dir_output_files/health/validation_${country}_mcs_ts_16_65_both.jpg", ///
	replace width(2560) height(1440) quality(100)
	
	
********************************************************************************
* 1.8 : Mean values over time - mcs,  16-65, by gender
********************************************************************************

* Prepare validation data	
use year dwt valid_healthMentalMcs demAge demMaleFlag using ///
	"$dir_data/ukhls_validation_sample.dta", clear

keep if inrange(demAge,16,65)
	
collapse (mean) valid_healthMentalMcs [aw = dwt], by(year demMaleFlag)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulation data
use run year sim_healthMentalMcs demAge demMaleFlag run using ///
	"$dir_data/simulation_sample.dta", clear

keep if inrange(demAge,16,65)

collapse (mean) sim_healthMentalMcs, by(year demMaleFlag run)

collapse (mean) sim_healthMentalMcs ///
		 (sd) sim_healthMentalMcs_sd = sim_healthMentalMcs ///
		 , by(year demMaleFlag)		 

* Compute 95% confidence interval 		 		 
foreach varname in sim_healthMentalMcs {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year demMaleFlag using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea sim_healthMentalMcs_high sim_healthMentalMcs_low year if ///
	demMaleFlag == 0, sort color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_healthMentalMcs year if demMaleFlag == 0, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	subtitle("Females") ///
	name(health_female, replace) ///
	xtitle("Year", size(small)) ///
	ytitle("Score", size(small)) ///
	ylabel(44.5[2]50.5,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	
twoway (rarea sim_healthMentalMcs_high sim_healthMentalMcs_low year if ///
	demMaleFlag == 1, sort color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_healthMentalMcs year if demMaleFlag == 1, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	subtitle("Males") ///
	name(health_male, replace) ///
	xtitle("Year", size(small)) ///
	ytitle("Score", size(small)) ///
	ylabel(44.5[2]50.5,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///	
	
grc1leg health_female health_male, ///
	title("Mental Health") ///
	subtitle("MCS, ages 16-65") ///
	legendfrom(health_female) rows(1) ///
	graphregion(color(white)) ///
	ycomm ///
	note("Notes: mcs is the SF-12 Mental Health Component. This is a measure of Mental function ranging from 0 (low functioning)" "to 100 (high functioning).  ", ///
	size(vsmall))

graph export ///
"$dir_output_files/health/validation_${country}_mcs_ts_16_65_gender.jpg", ///
	replace width(2560) height(1440) quality(100)
	
	
********************************************************************************
* 1.9 : Mean values over time - mcs, 16-65, by age group and gender 
********************************************************************************

* Prepare validation data
use year dwt demMaleFlag ageGroup valid_healthMentalMcs using ///
	"$dir_data/ukhls_validation_sample.dta", clear

gen health_m = valid_healthMentalMcs if demMaleFlag == 1
gen health_f = valid_healthMentalMcs if demMaleFlag == 0

drop if ageGroup == 0 

collapse (mean) health* [aw = dwt], by(ageGroup year)

drop if missing(ageGroup)
reshape wide health*, i(year) j(ageGroup)
		 
forvalues i = 1(1)8 {

	rename health_f`i' health_f_`i'_valid
	rename health_m`i' health_m_`i'_valid

}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare Simulated data
use run year demMaleFlag ageGroup sim_healthMentalMcs using ///
	"$dir_data/simulation_sample.dta", clear

gen health_m = sim_healthMentalMcs if demMaleFlag == 1
gen health_f = sim_healthMentalMcs if demMaleFlag == 0

collapse (mean) health*, by(ageGroup run year)

drop if missing(ageGroup)

reshape wide health*, i(year run) j(ageGroup)

collapse (mean) health* ///
		 (sd) health_m_1_sd = health_m1 ///
		 (sd) health_f_1_sd = health_f1 ///
		 (sd) health_m_2_sd = health_m2 ///
		 (sd) health_f_2_sd = health_f2 ///
		 (sd) health_m_3_sd = health_m3 ///
		 (sd) health_f_3_sd = health_f3 ///
		 (sd) health_m_4_sd = health_m4 ///
		 (sd) health_f_4_sd = health_f4 ///
		 (sd) health_m_5_sd = health_m5 ///
		 (sd) health_f_5_sd = health_f5 ///
		 (sd) health_m_6_sd = health_m6 ///
		 (sd) health_f_6_sd = health_f6 ///
		 (sd) health_m_7_sd = health_m7 ///
		 (sd) health_f_7_sd = health_f7 ///
		 (sd) health_m_8_sd = health_m8 ///
		 (sd) health_f_8_sd = health_f8 ///
		 , by(year)
		 
* Compute 19% confidence intervals		 
forvalues i = 1(1)8 {
	
	gen health_f_`i'_sim_high = health_f`i' + 1.96*health_f_`i'_sd
	gen health_f_`i'_sim_low = health_f`i' - 1.96*health_f_`i'_sd
	gen health_m_`i'_sim_high = health_m`i' + 1.96*health_m_`i'_sd
	gen health_m_`i'_sim_low = health_m`i' - 1.96*health_m_`i'_sd	

}
		 

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
foreach vble in "health_f" "health_m" {
	
	twoway (rarea `vble'_1_sim_high `vble'_1_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_1_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 15-19") ///
		name(`vble'_1, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Score", size(small)) ///
		ylabel(,labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))
		
	twoway (rarea `vble'_2_sim_high `vble'_2_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_2_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 20-24") ///
		name(`vble'_2, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Score", size(small)) ///
		ylabel(,labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))
		
	twoway (rarea `vble'_3_sim_high `vble'_3_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_3_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 25-29") ///
		name(`vble'_3, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Score", size(small)) ///
		ylabel(,labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))

	twoway (rarea `vble'_4_sim_high `vble'_4_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_4_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 30-34") ///
		name(`vble'_4, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Score", size(small)) ///
		ylabel(,labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))

	twoway (rarea `vble'_5_sim_high `vble'_5_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_5_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 35-39") ///
		name(`vble'_5, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Score", size(small)) ///
		ylabel(,labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))

	twoway (rarea `vble'_6_sim_high `vble'_6_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_6_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 40-59") ///
		name(`vble'_6, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Score", size(small)) ///
		ylabel(,labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))

	twoway (rarea `vble'_7_sim_high `vble'_7_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_7_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 60-79") ///
		name(`vble'_7, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Score", size(small)) ///
		ylabel(,labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))

	twoway (rarea `vble'_8_sim_high `vble'_8_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_8_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 80-100") ///
		name(`vble'_8, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Score", size(small)) ///
		ylabel(,labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))
}

* Save figures
grc1leg health_f_1 health_f_2 health_f_3 health_f_4 health_f_5 ///
	health_f_6 health_f_7 health_f_8, ///
	title("Mental Health") ///
	subtitle("MCS, females") ///
	legendfrom(health_f_1) ///
	graphregion(color(white)) ///
	ycomm ///
	note("Notes: mcs is the SF-12 Mental Health Component. This is a measure of Mental function ranging from 0 (low functioning) to" "100 (high functioning). ", ///
	size(vsmall)) 	

graph export ///
	"$dir_output_files/health/validation_${country}_mcs_ts_all_female.jpg", ///
	replace width(2400) height(1350) quality(100)
		

grc1leg health_m_1 health_m_2 health_m_3 health_m_4 health_m_5 ///
	health_m_6 health_m_7 health_m_8, ///
	title("Mental Health") ///
	subtitle("MCS, males") ///
	legendfrom(health_m_1) ///
	graphregion(color(white)) ///
	ycomm ///
	note("Notes: mcs is the SF-12 Mental Health Component. This is a measure of Mental function ranging from 0 (low functioning) to" "100 (high functioning). ", ///
	size(vsmall)) 	
	
graph export ///
	"$dir_output_files/health/validation_${country}_mcs_ts_all_male.jpg", ///
	replace width(2400) height(1350) quality(100)		
	
graph drop _all
	
	
********************************************************************************
* 2 : Histograms
********************************************************************************	

********************************************************************************
* 2.1 : Histograms - Working age
********************************************************************************

* Working age 
use year dwt valid_healthSelfRated demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear

keep if inrange(demAge,16,65)	
	
gen health = valid_healthSelfRated 

save "$dir_data/temp_valid_stats.dta", replace

* Prepare Simulated data
use run year sim_healthSelfRated demAge run using ///
	"$dir_data/simulation_sample.dta", clear

keep if inrange(demAge,16,65)

gen healthSelfRated = sim_healthSelfRated

keep if run == 1 

append using "$dir_data/temp_valid_stats.dta"


* Plot figure
twoway (hist healthSelfRated, width(0.2) color(green%30) ///
	legend(label(1 "SimPaths"))) ///
(hist health, width(0.2) color(red%30) legend(label(2 "UKHLS"))), ///
	title("Self Rated Health") ///
	subtitle("Ages 16-65") ///
	xtitle("Score", size(small)) ///
	ytitle("Year", size(small)) ///
	xlabel(,labsize(small)) ///
	ylabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: The health variable is a self-assessed variable and follows a 5-point Likert scale (1 = poor, ..., 5 = excellent). ", ///
	size(vsmall)) 

graph export ///
"$dir_output_files/health/validation_${country}_self_rated_hist_16_65_both.jpg", ///
	replace width(2560) height(1440) quality(100)	

	
********************************************************************************
* 2.2 : Histograms - Working age, by gender
********************************************************************************

use year dwt demAge valid_healthSelfRated demMaleFlag using ///
	"$dir_data/ukhls_validation_sample.dta", clear

keep if inrange(demAge,16,65)	
	
gen health = valid_healthSelfRated 

save "$dir_data/temp_valid_stats.dta", replace

* Prepare Simulated data
use run year demAge sim_healthSelfRated demMaleFlag run using ///
	"$dir_data/simulation_sample.dta", clear

keep if inrange(demAge,16,65)	
	
gen healthSelfRated = sim_healthSelfRated	
	
keep if run == 1 

append using "$dir_data/temp_valid_stats.dta"


* Plot figure

twoway (hist healthSelfRated if demMaleFlag == 0, width(0.2) color(green%30) ///
	legend(label(1 "SimPaths"))) ///
(hist health if demMaleFlag == 0, width(0.2) color(red%30) ///
	legend(label(2 "UKHLS"))), ///
	subtitle("Females") ///
	name(health_female, replace) ///
	xtitle("Score", size(small)) ///
	ytitle("Density", size(small)) ///
	xlabel(,labsize(small)) ///
	ylabel(0[.5]2.5,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))


twoway (hist healthSelfRated if demMaleFlag == 1, width(0.2) color(green%30) ///
	legend(label(1 "SimPaths"))) ///
(hist health if demMaleFlag == 1, width(0.2) color(red%30) ///
	legend(label(2 "UKHLS"))), ///
	subtitle("Males") ///
	name(health_male, replace) ///
	xtitle("Score", size(small)) ///
	ytitle("Density", size(small)) ///
	xlabel(,labsize(small)) ///
	ylabel(0[.5]2.5,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) 
	
grc1leg health_female health_male, ///
	title("Self Rated Health") ///
	subtitle("Ages 16-65") ///
	legendfrom(health_male) rows(1) ///
	graphregion(color(white)) ///
	ycomm ///
	note("Notes: The health variable is a self-assessed variable and follows a 5-point Likert scale (1 = poor, ..., 5 = excellent). ", ///
	size(vsmall)) 
	

graph export ///
"$dir_output_files/health/validation_${country}_self_rated_hist_16_65_gender.jpg", ///
	replace width(2560) height(1440) quality(100)	
	
graph drop _all 	
