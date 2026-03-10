********************************************************************************
* PROJECT:  		ESPON 
* SECTION:			Validation
* OBJECT: 			Health
* AUTHORS:			Ashley Burdett 
* LAST UPDATE:		9/25
* COUNTRY: 			UK 

* NOTES: 			Simulated data doesn't contain 80-100 year olds which make  
* 					up group 8. 
* 					Adjusted the code so that runs without this group. 
********************************************************************************

********************************************************************************
* 1 : Mean values over time
********************************************************************************

********************************************************************************
* 1.1 : Mean values over time - sf1, 18-65
********************************************************************************

use year dwt dhe dag using ///
	"$dir_data/ukhls_validation_sample.dta", clear

gen health = dhe 

collapse (mean) health [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare Simulated data
use run year dhe dag run using "$dir_data/simulated_data.dta", clear

collapse (mean) dhe, by(year run)

collapse (mean) dhe ///
		 (sd) dhe_sd = dhe ///
		 , by(year)		 

* Compute 95% confidence interval 		 
foreach varname in dhe {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea dhe_high dhe_low year, ///
	sort color(green%20) legend(label(1 "Simulated"))) ///
(line health year, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	title("General Health Score") ///
	subtitle("sf1, ages 18-65") ///
	xtitle("Year", size(small)) ///
	ytitle("Score", size(small)) ///
	ylabel(,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: The health variable is a self-assessed variable and follows a 5-point Likert scale (1 = poor, ..., 5 = excellent). ", ///
	size(vsmall))

graph export ///
"$dir_output_files/health/validation_${country}_sf1_ts_${min_age}_${max_age}_both.jpg", ///
	replace width(2560) height(1440) quality(100)
	
	
********************************************************************************
* 1.2 : Mean values over time - sf1,  18-65, by gender
********************************************************************************

* Prepare validation data	
use year dwt dhe dag dgn using ///
	"$dir_data/ukhls_validation_sample.dta", clear

gen health = dhe 

collapse (mean) health [aw = dwt], by(year dgn)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulation data
use run year dhe dag dgn run using "$dir_data/simulated_data.dta", clear

gen dgn2 = 0 if dgn == "Female"
replace dgn2 = 1 if dgn == "Male"

drop dgn
rename dgn2 dgn 

collapse (mean) dhe, by(year dgn run)

collapse (mean) dhe ///
		 (sd) dhe_sd = dhe ///
		 , by(year dgn)		 

* Compute 95% confidence interval 		 		 
foreach varname in dhe {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year dgn using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea dhe_high dhe_low year if dgn == 0, ///
	sort color(green%20) legend(label(1 "Simulated"))) ///
(line health year if dgn == 0, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	subtitle("Females") ///
	name(health_female, replace) ///
	xtitle("Year", size(small)) ///
	ytitle("Score", size(small)) ///
	ylabel(3[.2]3.8,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	

twoway (rarea dhe_high dhe_low year if dgn == 1, ///
	sort color(green%20) legend(label(1 "Simulated"))) ///
(line health year if dgn == 1, sort color(green) ///
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
	title("General Health Score") ///
	subtitle("sf1, ages 18-65") ///
	legendfrom(health_female) rows(1) ///
	graphregion(color(white)) ///
note("Notes: The health variable is a self-assessed variable and follows a 5-point Likert scale (1 = poor, ..., 5 = excellent). ", ///
	size(vsmall))

graph export ///
"$dir_output_files/health/validation_${country}_sf1_ts_${min_age}_${max_age}_gender.jpg", ///
	replace width(2560) height(1440) quality(100)
	
	

********************************************************************************
* 1.3 : Mean values over time - sf1,  18-65, by age group and gender 
********************************************************************************

* Prepare validation data
use year dwt dgn ageGroup dhe using ///
	"$dir_data/ukhls_validation_full_sample.dta", clear

gen health_m = dhe if dgn == 1
gen health_f = dhe if dgn == 0

drop if ageGroup == 0 | ageGroup == 8 

collapse (mean) health* [aw = dwt], by(ageGroup year)

drop if missing(ageGroup)
reshape wide health*, i(year) j(ageGroup)

forvalues i = 1(1)7 {

	rename health_f`i' health_f_`i'_valid
	rename health_m`i' health_m_`i'_valid

}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare Simulated data
use run year sim_sex ageGroup dhe using "$dir_data/simulated_data.dta", clear

gen health_m = dhe if sim_sex == 1
gen health_f = dhe if sim_sex == 2

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
		 , by(year)
		 /*(sd) health_m_8_sd = health_m8 ///
		 *(sd) health_f_8_sd = health_f8 /// */
		 
forvalues i=1(1)7 {
	
	gen health_f_`i'_sim_high = health_f`i' + 1.96*health_f_`i'_sd
	gen health_f_`i'_sim_low = health_f`i' - 1.96*health_f_`i'_sd
	gen health_m_`i'_sim_high = health_m`i' + 1.96*health_m_`i'_sd
	gen health_m_`i'_sim_low = health_m`i' - 1.96*health_m_`i'_sd	

}
		 

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
foreach vble in "health_f" "health_m" {
	
	twoway (rarea `vble'_1_sim_high `vble'_1_sim_low year, sort ///
		color(red%20) legend(label(1 "Simulated") position(6) rows(1))) ///
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
		color(red%20) legend(label(1 "Simulated") position(6) rows(1))) ///
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
		color(red%20) legend(label(1 "Simulated") position(6) rows(1))) ///
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
		color(red%20) legend(label(1 "Simulated") position(6) rows(1))) ///
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
		color(red%20) legend(label(1 "Simulated") position(6) rows(1))) ///
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
		color(red%20) legend(label(1 "Simulated") position(6) rows(1))) ///
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
		color(red%20) legend(label(1 "Simulated") position(6) rows(1))) ///
	(line `vble'_7_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 60-79") ///
		name(`vble'_7, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Score", size(small)) ///
		ylabel(3 [1] 5, labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))

	/*twoway (rarea `vble'_8_sim_high `vble'_8_sim_low year, sort ///
		color(green%20) legend(label(1 "Simulated") position(6) rows(1))) ///
		(line `vble'_8_valid year, sort color(green) legend(label(2 "UKHLS"))), ///
		title("age 80-100") name(`vble'_8, replace) ylabel(1 [1] 5)*/
}

* Save figures
grc1leg health_f_1 health_f_2 health_f_3 health_f_4 health_f_5 ///
	health_f_6 health_f_7 /*health_f_8*/, ///
	title("General Health Score") ///
	subtitle("sf1, females") ///
	legendfrom(health_f_1) ///
	graphregion(color(white)) ///
note("Notes: The health variable is a self-assessed variable and follows a 5-point Likert scale (1 = poor, ..., 5 = excellent). ", ///
	size(vsmall)) 	

graph export ///
	"$dir_output_files/health/validation_${country}_sf1_ts_all_female.jpg", ///
	replace width(2400) height(1350) quality(100)
		

grc1leg health_m_1 health_m_2 health_m_3 health_m_4 health_m_5 ///
	health_m_6 health_m_7 /*health_m_8*/, ///
	title("General Health Score") ///
	subtitle("sf1, males") ///
	legendfrom(health_m_1) ///
	graphregion(color(white)) ///
note("Notes: The health variable is a self-assessed variable and follows a 5-point Likert scale (1 = poor, ..., 5 = excellent). ", ///
	size(vsmall)) 	
	
graph export ///
	"$dir_output_files/health/validation_${country}_sf1_ts_all_male.jpg", ///
	replace width(2400) height(1350) quality(100)
	
	
********************************************************************************
* 1.4 : Mean values over time - pcs, 18-65
********************************************************************************

use year dwt dhe_pcs dag using ///
	"$dir_data/ukhls_validation_sample.dta", clear

gen health = dhe_pcs 

collapse (mean) health [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare Simulated data
use run year dhe_pcs dag run using "$dir_data/simulated_data.dta", clear

collapse (mean) dhe_pcs, by(year run)

collapse (mean) dhe_pcs ///
		 (sd) dhe_pcs_sd = dhe_pcs ///
		 , by(year)		 

* Compute 95% confidence interval 		 
foreach varname in dhe_pcs {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea dhe_pcs_high dhe_pcs_low year, ///
	sort color(green%20) legend(label(1 "Simulated"))) ///
(line health year, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	title("Physical Health") ///
	subtitle("PCS, ages 18-65") ///
	xtitle("Year", size(small)) ///
	ytitle("Health score", size(small)) ///
	ylabel(,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: PCS is the SF-12 Physical Health Component. This is a measure of physical function ranging from 0 (low functioning)" "to 100 (high functioning).  ", ///
	size(vsmall))

graph export ///
"$dir_output_files/health/validation_${country}_pcs_ts_${min_age}_${max_age}_both.jpg", ///
	replace width(2560) height(1440) quality(100)
	
	
********************************************************************************
* 1.5 : Mean values over time - pcs,  18-65, by gender
********************************************************************************

* Prepare validation data	
use year dwt dhe_pcs dag dgn using ///
	"$dir_data/ukhls_validation_sample.dta", clear

gen health = dhe_pcs 

collapse (mean) health [aw = dwt], by(year dgn)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulation data
use run year dhe_pcs dag dgn run using "$dir_data/simulated_data.dta", clear

gen dgn2 = 0 if dgn == "Female"
replace dgn2 = 1 if dgn == "Male"

drop dgn
rename dgn2 dgn 

collapse (mean) dhe_pcs, by(year dgn run)

collapse (mean) dhe_pcs ///
		 (sd) dhe_pcs_sd = dhe_pcs ///
		 , by(year dgn)		 

* Compute 95% confidence interval 		 		 
foreach varname in dhe_pcs {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year dgn using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea dhe_pcs_high dhe_pcs_low year if dgn == 0, ///
	sort color(green%20) legend(label(1 "Simulated"))) ///
(line health year if dgn == 0, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	subtitle("Females") ///
	name(health_female, replace) ///
	xtitle("Year", size(small)) ///
	ytitle("Health score", size(small)) ///
	ylabel(49[0.5]52.5,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	

twoway (rarea dhe_pcs_high dhe_pcs_low year if dgn == 1, ///
	sort color(green%20) legend(label(1 "Simulated"))) ///
(line health year if dgn == 1, sort color(green) ///
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
	title("Phyisucal Health") ///
	subtitle("PCS, ages 18-65") ///
	legendfrom(health_female) rows(1) ///
	graphregion(color(white)) ///
	note("Notes: PCS is the SF-12 Physical Health Component. This is a measure of physical function ranging from 0 (low functioning)" "to 100 (high functioning).  ", ///
	size(vsmall))

graph export ///
"$dir_output_files/health/validation_${country}_pcs_ts_${min_age}_${max_age}_gender.jpg", ///
	replace width(2560) height(1440) quality(100)
	
	
********************************************************************************
* 1.6 : Mean values over time - pcs,  18-65, by age group and gender 
********************************************************************************

* Prepare validation data
use year dwt dgn ageGroup dhe_pcs using ///
	"$dir_data/ukhls_validation_full_sample.dta", clear

gen health_m = dhe_pcs if dgn == 1
gen health_f = dhe_pcs if dgn == 0

drop if ageGroup == 0 | ageGroup == 8 

collapse (mean) health* [aw = dwt], by(ageGroup year)

drop if missing(ageGroup)
reshape wide health*, i(year) j(ageGroup)
		 
forvalues i = 1(1)7 {

	rename health_f`i' health_f_`i'_valid
	rename health_m`i' health_m_`i'_valid

}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare Simulated data
use run year sim_sex ageGroup dhe_pcs using "$dir_data/simulated_data.dta", clear

gen health_m = dhe_pcs if sim_sex == 1
gen health_f = dhe_pcs if sim_sex == 2

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
		 , by(year)
		 /*(sd) health_m_8_sd = health_m8 ///
		 *(sd) health_f_8_sd = health_f8 /// */
		 
* Compute 19% confidence intervals		 
forvalues i=1(1)7 {
	
	gen health_f_`i'_sim_high = health_f`i' + 1.96*health_f_`i'_sd
	gen health_f_`i'_sim_low = health_f`i' - 1.96*health_f_`i'_sd
	gen health_m_`i'_sim_high = health_m`i' + 1.96*health_m_`i'_sd
	gen health_m_`i'_sim_low = health_m`i' - 1.96*health_m_`i'_sd	

}
		 

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
foreach vble in "health_f" "health_m" {
	
	twoway (rarea `vble'_1_sim_high `vble'_1_sim_low year, sort ///
		color(red%20) legend(label(1 "Simulated") position(6) rows(1))) ///
	(line `vble'_1_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 15-19") ///
		name(`vble'_1, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Score", size(small)) ///
		ylabel(40[5]58,labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))
		
	twoway (rarea `vble'_2_sim_high `vble'_2_sim_low year, sort ///
		color(red%20) legend(label(1 "Simulated") position(6) rows(1))) ///
	(line `vble'_2_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 20-24") ///
		name(`vble'_2, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Score", size(small)) ///
		ylabel(40[5]58,labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))
		
	twoway (rarea `vble'_3_sim_high `vble'_3_sim_low year, sort ///
		color(red%20) legend(label(1 "Simulated") position(6) rows(1))) ///
	(line `vble'_3_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 25-29") ///
		name(`vble'_3, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Score", size(small)) ///
		ylabel(40[5]58,labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))

	twoway (rarea `vble'_4_sim_high `vble'_4_sim_low year, sort ///
		color(red%20) legend(label(1 "Simulated") position(6) rows(1))) ///
	(line `vble'_4_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 30-34") ///
		name(`vble'_4, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Score", size(small)) ///
		ylabel(40[5]58,labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))

	twoway (rarea `vble'_5_sim_high `vble'_5_sim_low year, sort ///
		color(red%20) legend(label(1 "Simulated") position(6) rows(1))) ///
	(line `vble'_5_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 35-39") ///
		name(`vble'_5, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Score", size(small)) ///
		ylabel(40[5]58,labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))

	twoway (rarea `vble'_6_sim_high `vble'_6_sim_low year, sort ///
		color(red%20) legend(label(1 "Simulated") position(6) rows(1))) ///
	(line `vble'_6_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 40-59") ///
		name(`vble'_6, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Score", size(small)) ///
		ylabel(40[5]58,labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))

	twoway (rarea `vble'_7_sim_high `vble'_7_sim_low year, sort ///
		color(red%20) legend(label(1 "Simulated") position(6) rows(1))) ///
	(line `vble'_7_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 60-79") ///
		name(`vble'_7, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Score", size(small)) ///
		ylabel(40[5]58,labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))

	/*twoway (rarea `vble'_8_sim_high `vble'_8_sim_low year, sort ///
		color(green%20) legend(label(1 "Simulated") position(6) rows(1))) ///
		(line `vble'_8_valid year, sort color(green) legend(label(2 "UKHLS"))), ///
		title("age 80-100") name(`vble'_8, replace) ylabel(1 [1] 5)*/
}

* Save figures
grc1leg health_f_1 health_f_2 health_f_3 health_f_4 health_f_5 ///
	health_f_6 health_f_7 /*health_f_8*/, ///
	title("Physical Health") ///
	subtitle("PCS, females") ///
	legendfrom(health_f_1) ///
	graphregion(color(white)) ///
	note("Notes: PCS is the SF-12 Physical Health Component. This is a measure of physical function ranging from 0 (low functioning) to" "100 (high functioning). ", ///
	size(vsmall)) 	

graph export ///
	"$dir_output_files/health/validation_${country}_pcs_ts_all_female.jpg", ///
	replace width(2400) height(1350) quality(100)
		

grc1leg health_m_1 health_m_2 health_m_3 health_m_4 health_m_5 ///
	health_m_6 health_m_7 /*health_m_8*/, ///
	title("Physical Health") ///
	subtitle("PCS, males") ///
	legendfrom(health_m_1) ///
	graphregion(color(white)) ///
	note("Notes: PCS is the SF-12 Physical Health Component. This is a measure of physical function ranging from 0 (low functioning) to" "100 (high functioning). ", ///
	size(vsmall)) 	
	
graph export ///
	"$dir_output_files/health/validation_${country}_pcs_ts_all_male.jpg", ///
	replace width(2400) height(1350) quality(100)	
	
graph drop _all	
	
********************************************************************************
* 1.7 : Mean values over time - mcs, 18-65
********************************************************************************

use year dwt dhe_mcs dag using ///
	"$dir_data/ukhls_validation_sample.dta", clear

gen health = dhe_mcs 

collapse (mean) health [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare Simulated data
use run year dhe_mcs dag run using "$dir_data/simulated_data.dta", clear

collapse (mean) dhe_mcs, by(year run)

collapse (mean) dhe_mcs ///
		 (sd) dhe_mcs_sd = dhe_mcs ///
		 , by(year)		 

* Compute 95% confidence interval 		 
foreach varname in dhe_mcs {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea dhe_mcs_high dhe_mcs_low year, ///
	sort color(green%20) legend(label(1 "Simulated"))) ///
(line health year, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	title("Mental Health") ///
	subtitle("MCS, ages 18-65") ///
	xtitle("Year", size(small)) ///
	ytitle("Health score", size(small)) ///
	ylabel(,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: mcs is the SF-12 Mental Health Component. This is a measure of Mental function ranging from 0 (low functioning)" "to 100 (high functioning).  ", ///
	size(vsmall))

graph export ///
"$dir_output_files/health/validation_${country}_mcs_ts_${min_age}_${max_age}_both.jpg", ///
	replace width(2560) height(1440) quality(100)
	
	
********************************************************************************
* 1.8 : Mean values over time - mcs,  18-65, by gender
********************************************************************************

* Prepare validation data	
use year dwt dhe_mcs dag dgn using ///
	"$dir_data/ukhls_validation_sample.dta", clear

gen health = dhe_mcs 

collapse (mean) health [aw = dwt], by(year dgn)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulation data
use run year dhe_mcs dag dgn run using "$dir_data/simulated_data.dta", clear

gen dgn2 = 0 if dgn == "Female"
replace dgn2 = 1 if dgn == "Male"

drop dgn
rename dgn2 dgn 

collapse (mean) dhe_mcs, by(year dgn run)

collapse (mean) dhe_mcs ///
		 (sd) dhe_mcs_sd = dhe_mcs ///
		 , by(year dgn)		 

* Compute 95% confidence interval 		 		 
foreach varname in dhe_mcs {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year dgn using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea dhe_mcs_high dhe_mcs_low year if dgn == 0, ///
	sort color(green%20) legend(label(1 "Simulated"))) ///
(line health year if dgn == 0, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	subtitle("Females") ///
	name(health_female, replace) ///
	xtitle("Year", size(small)) ///
	ytitle("Health score", size(small)) ///
	ylabel(44.5[2]50.5,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	

twoway (rarea dhe_mcs_high dhe_mcs_low year if dgn == 1, ///
	sort color(green%20) legend(label(1 "Simulated"))) ///
(line health year if dgn == 1, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	subtitle("Males") ///
	name(health_male, replace) ///
	xtitle("Year", size(small)) ///
	ytitle("Health score", size(small)) ///
	ylabel(44.5[2]50.5,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///	
	
grc1leg health_female health_male, ///
	title("Physical Health") ///
	subtitle("MCS, ages 18-65") ///
	legendfrom(health_female) rows(1) ///
	graphregion(color(white)) ///
	note("Notes: mcs is the SF-12 Mental Health Component. This is a measure of Mental function ranging from 0 (low functioning)" "to 100 (high functioning).  ", ///
	size(vsmall))

graph export ///
"$dir_output_files/health/validation_${country}_mcs_ts_${min_age}_${max_age}_gender.jpg", ///
	replace width(2560) height(1440) quality(100)
	
	
********************************************************************************
* 1.9 : Mean values over time - mcs,  18-65, by age group and gender 
********************************************************************************

* Prepare validation data
use year dwt dgn ageGroup dhe_mcs using ///
	"$dir_data/ukhls_validation_full_sample.dta", clear

gen health_m = dhe_mcs if dgn == 1
gen health_f = dhe_mcs if dgn == 0

drop if ageGroup == 0 | ageGroup == 8 

collapse (mean) health* [aw = dwt], by(ageGroup year)

drop if missing(ageGroup)
reshape wide health*, i(year) j(ageGroup)
		 
forvalues i = 1(1)7 {

	rename health_f`i' health_f_`i'_valid
	rename health_m`i' health_m_`i'_valid

}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare Simulated data
use run year sim_sex ageGroup dhe_mcs using "$dir_data/simulated_data.dta", clear

gen health_m = dhe_mcs if sim_sex == 1
gen health_f = dhe_mcs if sim_sex == 2

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
		 , by(year)
		 /*(sd) health_m_8_sd = health_m8 ///
		 *(sd) health_f_8_sd = health_f8 /// */
		 
* Compute 19% confidence intervals		 
forvalues i=1(1)7 {
	
	gen health_f_`i'_sim_high = health_f`i' + 1.96*health_f_`i'_sd
	gen health_f_`i'_sim_low = health_f`i' - 1.96*health_f_`i'_sd
	gen health_m_`i'_sim_high = health_m`i' + 1.96*health_m_`i'_sd
	gen health_m_`i'_sim_low = health_m`i' - 1.96*health_m_`i'_sd	

}
		 

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
foreach vble in "health_f" "health_m" {
	
	twoway (rarea `vble'_1_sim_high `vble'_1_sim_low year, sort ///
		color(red%20) legend(label(1 "Simulated") position(6) rows(1))) ///
	(line `vble'_1_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 15-19") ///
		name(`vble'_1, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Score", size(small)) ///
		ylabel(40[5]58,labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))
		
	twoway (rarea `vble'_2_sim_high `vble'_2_sim_low year, sort ///
		color(red%20) legend(label(1 "Simulated") position(6) rows(1))) ///
	(line `vble'_2_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 20-24") ///
		name(`vble'_2, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Score", size(small)) ///
		ylabel(40[5]58,labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))
		
	twoway (rarea `vble'_3_sim_high `vble'_3_sim_low year, sort ///
		color(red%20) legend(label(1 "Simulated") position(6) rows(1))) ///
	(line `vble'_3_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 25-29") ///
		name(`vble'_3, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Score", size(small)) ///
		ylabel(40[5]58,labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))

	twoway (rarea `vble'_4_sim_high `vble'_4_sim_low year, sort ///
		color(red%20) legend(label(1 "Simulated") position(6) rows(1))) ///
	(line `vble'_4_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 30-34") ///
		name(`vble'_4, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Score", size(small)) ///
		ylabel(40[5]58,labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))

	twoway (rarea `vble'_5_sim_high `vble'_5_sim_low year, sort ///
		color(red%20) legend(label(1 "Simulated") position(6) rows(1))) ///
	(line `vble'_5_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 35-39") ///
		name(`vble'_5, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Score", size(small)) ///
		ylabel(40[5]58,labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))

	twoway (rarea `vble'_6_sim_high `vble'_6_sim_low year, sort ///
		color(red%20) legend(label(1 "Simulated") position(6) rows(1))) ///
	(line `vble'_6_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 40-59") ///
		name(`vble'_6, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Score", size(small)) ///
		ylabel(40[5]58,labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))

	twoway (rarea `vble'_7_sim_high `vble'_7_sim_low year, sort ///
		color(red%20) legend(label(1 "Simulated") position(6) rows(1))) ///
	(line `vble'_7_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 60-79") ///
		name(`vble'_7, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Score", size(small)) ///
		ylabel(40[5]58,labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))

	/*twoway (rarea `vble'_8_sim_high `vble'_8_sim_low year, sort ///
		color(green%20) legend(label(1 "Simulated") position(6) rows(1))) ///
		(line `vble'_8_valid year, sort color(green) legend(label(2 "UKHLS"))), ///
		title("age 80-100") name(`vble'_8, replace) ylabel(1 [1] 5)*/
}

* Save figures
grc1leg health_f_1 health_f_2 health_f_3 health_f_4 health_f_5 ///
	health_f_6 health_f_7 /*health_f_8*/, ///
	title("Mental Health") ///
	subtitle("MCS, females") ///
	legendfrom(health_f_1) ///
	graphregion(color(white)) ///
	note("Notes: mcs is the SF-12 Mental Health Component. This is a measure of Mental function ranging from 0 (low functioning) to" "100 (high functioning). ", ///
	size(vsmall)) 	

graph export ///
	"$dir_output_files/health/validation_${country}_mcs_ts_all_female.jpg", ///
	replace width(2400) height(1350) quality(100)
		

grc1leg health_m_1 health_m_2 health_m_3 health_m_4 health_m_5 ///
	health_m_6 health_m_7 /*health_m_8*/, ///
	title("Mental Health") ///
	subtitle("mcs, males") ///
	legendfrom(health_m_1) ///
	graphregion(color(white)) ///
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
use year dwt dhe dag using ///
	"$dir_data/ukhls_validation_sample.dta", clear

gen health = dhe 

save "$dir_data/temp_valid_stats.dta", replace

* Prepare Simulated data
use run year dhe dag run using "$dir_data/simulated_data.dta", clear

keep if run == 1 

append using "$dir_data/temp_valid_stats.dta"


* Plot figure
twoway (hist dhe, width(0.2) color(green%30) legend(label(1 "Simulated"))) ///
(hist health, width(0.2) color(red%30) legend(label(2 "UKHLS"))), ///
	title("General Health Score") ///
	subtitle("sf1, ages 18-65") ///
	xtitle("Score", size(small)) ///
	ytitle("Year", size(small)) ///
	xlabel(,labsize(small)) ///
	ylabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: The health variable is a self-assessed variable and follows a 5-point Likert scale (1 = poor, ..., 5 = excellent). ", ///
	size(vsmall)) 

graph export ///
"$dir_output_files/health/validation_${country}_sf1_hist_${min_age}_${max_age}_both.jpg", ///
	replace width(2560) height(1440) quality(100)	

	
********************************************************************************
* 2.2 : Histograms - Working age, by gender
********************************************************************************

use year dwt dhe dgn using ///
	"$dir_data/ukhls_validation_sample.dta", clear

gen health = dhe 

save "$dir_data/temp_valid_stats.dta", replace

* Prepare Simulated data
use run year dhe dgn run using "$dir_data/simulated_data.dta", clear

keep if run == 1 

gen dgn2 = 0 if dgn == "Female"
replace dgn2 = 1 if dgn == "Male"

drop dgn
rename dgn2 dgn 

append using "$dir_data/temp_valid_stats.dta"


* Plot figure

twoway (hist dhe if dgn == 0, width(0.2) color(green%30) ///
	legend(label(1 "Simulated"))) ///
(hist health if dgn == 0, width(0.2) color(red%30) ///
	legend(label(2 "UKHLS"))), ///
	subtitle("Females") ///
	name(health_female, replace) ///
	xtitle("Score", size(small)) ///
	ytitle("Density", size(small)) ///
	xlabel(,labsize(small)) ///
	ylabel(0[.5]2.5,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///


twoway (hist dhe if dgn == 1, width(0.2) color(green%30) ///
	legend(label(1 "Simulated"))) ///
(hist health if dgn == 1, width(0.2) color(red%30) ///
	legend(label(2 "UKHLS"))), ///
	subtitle("Males") ///
	name(health_male, replace) ///
	xtitle("Score", size(small)) ///
	ytitle("Density", size(small)) ///
	xlabel(,labsize(small)) ///
	ylabel(0[.5]2.5,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///

	
grc1leg health_female health_male, ///
	title("General Health Score") ///
	subtitle("sf1, ages 18-65") ///
	legendfrom(health_male) rows(1) ///
	graphregion(color(white)) ///
	note("Notes: The health variable is a self-assessed variable and follows a 5-point Likert scale (1 = poor, ..., 5 = excellent). ", ///
	size(vsmall)) 
	

graph export ///
"$dir_output_files/health/validation_${country}_sf1_hist_${min_age}_${max_age}_gender.jpg", ///
	replace width(2560) height(1440) quality(100)	
	
graph drop _all 	
