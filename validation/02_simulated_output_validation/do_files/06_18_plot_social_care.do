********************************************************************************
* PROJECT:  		ESPON 
* SECTION:			Validation
* OBJECT: 			Social care
* AUTHORS:			Ashley Burdett 
* LAST UPDATE:		9/25
* COUNTRY: 			UK 

* NOTES: 			
********************************************************************************

********************************************************************************
* 1 : Mean values over time
********************************************************************************

********************************************************************************
* 1.1 : Mean values over time - need care 
********************************************************************************

* Prepare validation data 
use year idbenefitunit dwt need_socare dag using ///
	"$dir_data/ukhls_validation_full_sample.dta", clear

keep if dag > 64 & dag != . 
	
gen valid_need_care = 0 
replace valid_need_care = 1 if need_socare == 1
replace valid_need_care = . if need_socare < 0 | need_socare == . 


collapse (mean) valid_need_care [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data 
use run year idbenefitunit needsocialcare dag using ///
	"$dir_data/simulated_data_full.dta", clear

keep if dag > 64
	
gen sim_need_care = 0 
replace sim_need_care = 1 if needsocialcare == "True"

collapse (mean) sim_need_care, by(year run)

collapse (mean) sim_need_care ///
		 (sd) sim_need_care_sd = sim_need_care, by(year)


* Compute 95% confidence intervals
gen sim_need_care_h = sim_need_care + 1.96*sim_need_care_sd
gen sim_need_care_l = sim_need_care - 1.96*sim_need_care_sd

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure 
twoway (rarea sim_need_care_h sim_need_care_l year, ///
	sort color(green%20) legend(label(1 "Simulated"))) ///
(line valid_need_care year, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	title("Need Social Care") ///
	subtitle("Ages 65+") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: ", size(vsmall))
	
	
* Save figure
graph export ///
"$dir_output_files/care/validation_${country}_need_care_ts_65plus_both.jpg", ///
	replace width(2400) height(1350) quality(100)

	
********************************************************************************
* 1.2 : Mean values over time - need care, by gender
********************************************************************************

* Prepare validation data 
use year idbenefitunit dwt dgn need_socare dag using ///
	"$dir_data/ukhls_validation_full_sample.dta", clear
	
keep if dag > 64 & dag != . 	
	
gen valid_need_care = 0 
replace valid_need_care = 1 if need_socare == 1
replace valid_need_care = . if need_socare < 0 | need_socare == . 

collapse (mean) valid_need_care [aw = dwt], by(year dgn)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data 
use run year idbenefitunit needsocialcare dgn dag using ///
	"$dir_data/simulated_data_full.dta", clear
	
keep if dag > 64	
	
gen dgn2 = 0 if dgn == "Female"
replace dgn2 = 1 if dgn == "Male"

drop dgn
rename dgn2 dgn 	

gen sim_need_care = 0 
replace sim_need_care = 1 if needsocialcare == "True"

collapse (mean) sim_need_care, by(year dgn run)

collapse (mean) sim_need_care ///
		 (sd) sim_need_care_sd = sim_need_care, by(year dgn)

* Compute 95% confidence intervals
gen sim_need_care_h = sim_need_care + 1.96*sim_need_care_sd
gen sim_need_care_l = sim_need_care - 1.96*sim_need_care_sd

merge 1:1 year dgn using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea sim_need_care_h sim_need_care_l year if dgn == 0, ///
	sort color(green%20) legend(label(1 "Simulated"))) ///
(line valid_need_care year if dgn == 0, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	subtitle("Females") ///
	name(health_female, replace) ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	ylabel(0[0.1]0.5,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	

twoway (rarea sim_need_care_h sim_need_care_l year if dgn == 1, ///
	sort color(green%20) legend(label(1 "Simulated"))) ///
(line valid_need_care year if dgn == 1, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	subtitle("Males") ///
	name(health_male, replace) ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	ylabel(0[0.1]0.5,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///	
	
grc1leg health_female health_male, ///
	title("Need Social Care") ///
	subtitle("Ages 65+") ///
	legendfrom(health_female) rows(1) ///
	graphregion(color(white)) ///
note("Notes:  ", ///
	size(vsmall))	
	
* Save figure
graph export ///
"$dir_output_files/care/validation_${country}_need_care_ts_65plus_gender.jpg", ///
	replace width(2400) height(1350) quality(100)
	

graph drop _all 


********************************************************************************
* 1.3 : Mean values over time - need care age group and gender 
********************************************************************************

* Prepare validation data
use year dwt dgn ageGroup need_socare dag using ///
	"$dir_data/ukhls_validation_full_sample.dta", clear
	
drop ageGroup
gen ageGroup = 1 if inrange(dag,65,69)	
replace ageGroup = 2 if inrange(dag,70,74)
replace ageGroup = 3 if inrange(dag,75,79)
replace ageGroup = 4 if inrange(dag,80,85)
replace ageGroup = 5 if inrange(dag,85,89)
replace ageGroup = 6 if inrange(dag,90,100)	

drop if dag < 65
	
gen valid_need_care = 0 
replace valid_need_care = 1 if need_socare == 1
replace valid_need_care = . if need_socare < 0 | need_socare == . 
	
gen care_m = valid_need_care if dgn == 1
gen care_f = valid_need_care if dgn == 0

drop if ageGroup == 0 

collapse (mean) care* [aw = dwt], by(ageGroup year)

drop if missing(ageGroup)
reshape wide care*, i(year) j(ageGroup)

forvalues i = 1(1)6 {

	rename care_f`i' care_f_`i'_valid
	rename care_m`i' care_m_`i'_valid

}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare Simulated data
use run year sim_sex ageGroup needsocialcare dag using ///
	"$dir_data/simulated_data_full.dta", clear

drop ageGroup
gen ageGroup = 1 if inrange(dag,65,69)	
replace ageGroup = 2 if inrange(dag,70,74)
replace ageGroup = 3 if inrange(dag,75,79)
replace ageGroup = 4 if inrange(dag,80,85)
replace ageGroup = 5 if inrange(dag,85,89)
replace ageGroup = 6 if inrange(dag,90,100)	

drop if dag < 65	
	
gen sim_need_care = 0 
replace sim_need_care = 1 if needsocialcare == "True"

gen care_m = sim_need_care if sim_sex == 1
gen care_f = sim_need_care if sim_sex == 2

collapse (mean) care*, by(ageGroup run year)
drop if missing(ageGroup)
reshape wide care*, i(year run) j(ageGroup)

collapse (mean) care* ///
		 (sd) care_m_1_sd = care_m1 ///
		 (sd) care_f_1_sd = care_f1 ///
		 (sd) care_m_2_sd = care_m2 ///
		 (sd) care_f_2_sd = care_f2 ///
		 (sd) care_m_3_sd = care_m3 ///
		 (sd) care_f_3_sd = care_f3 ///
		 (sd) care_m_4_sd = care_m4 ///
		 (sd) care_f_4_sd = care_f4 ///
		 (sd) care_m_5_sd = care_m5 ///
		 (sd) care_f_5_sd = care_f5 ///
		 (sd) care_m_6_sd = care_m6 ///
		 (sd) care_f_6_sd = care_f6 ///
		 , by(year)
		 /*(sd) care_m_8_sd = care_m8 ///
		 *(sd) care_f_8_sd = care_f8 /// */
		 
forvalues i=1(1)6 {
	
	gen care_f_`i'_sim_high = care_f`i' + 1.96*care_f_`i'_sd
	gen care_f_`i'_sim_low = care_f`i' - 1.96*care_f_`i'_sd
	gen care_m_`i'_sim_high = care_m`i' + 1.96*care_m_`i'_sd
	gen care_m_`i'_sim_low = care_m`i' - 1.96*care_m_`i'_sd	

}		 

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
foreach vble in "care_f" "care_m" {
	
	twoway (rarea `vble'_1_sim_high `vble'_1_sim_low year, sort ///
		color(red%20) legend(label(1 "Simulated") position(6) rows(1))) ///
	(line `vble'_1_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 65-69") ///
		name(`vble'_1, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Share", size(small)) ///
		ylabel(0[0.3]0.9, labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))
		
	twoway (rarea `vble'_2_sim_high `vble'_2_sim_low year, sort ///
		color(red%20) legend(label(1 "Simulated") position(6) rows(1))) ///
	(line `vble'_2_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 70-74") ///
		name(`vble'_2, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Share", size(small)) ///
		ylabel(0[0.3]0.9, labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))
		
	twoway (rarea `vble'_3_sim_high `vble'_3_sim_low year, sort ///
		color(red%20) legend(label(1 "Simulated") position(6) rows(1))) ///
	(line `vble'_3_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 75-79") ///
		name(`vble'_3, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Share", size(small)) ///
		ylabel(0[0.3]0.9, labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))

	twoway (rarea `vble'_4_sim_high `vble'_4_sim_low year, sort ///
		color(red%20) legend(label(1 "Simulated") position(6) rows(1))) ///
	(line `vble'_4_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 80-84") ///
		name(`vble'_4, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Share", size(small)) ///
		ylabel(0[0.3]0.9, labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))

	twoway (rarea `vble'_5_sim_high `vble'_5_sim_low year, sort ///
		color(red%20) legend(label(1 "Simulated") position(6) rows(1))) ///
	(line `vble'_5_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 85-89") ///
		name(`vble'_5, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Share", size(small)) ///
		ylabel(0[0.3]0.9, labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))

	twoway (rarea `vble'_6_sim_high `vble'_6_sim_low year, sort ///
		color(red%20) legend(label(1 "Simulated") position(6) rows(1))) ///
	(line `vble'_6_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 90-100") ///
		name(`vble'_6, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Share", size(small)) ///
		ylabel(0[0.3]0.9, labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))


}

* Save figures
grc1leg care_f_1 care_f_2 care_f_3 care_f_4 care_f_5 care_f_6 , ///
	title("Need Social Care") ///
	subtitle("Females") ///
	legendfrom(care_f_1) ///
	graphregion(color(white)) ///
note("Notes:  ", ///
	size(vsmall)) 	
	
graph export ///
"$dir_output_files/care/validation_${country}_need_care_ts_65plus_female.jpg", ///
	replace width(2400) height(1350) quality(100)
		

grc1leg care_m_1 care_m_2 care_m_3 care_m_4 care_m_5 care_m_6, ///
	title("Need Social Care") ///
	subtitle("Males") ///
	legendfrom(care_m_1) ///
	graphregion(color(white)) ///
note("Notes: ", ///
	size(vsmall)) 	
	
graph export ///
"$dir_output_files/care/validation_${country}_need_care_ts_65plus_male.jpg", ///
	replace width(2400) height(1350) quality(100)


graph drop _all 	


/*
********************************************************************************
* 1.4 : Mean values over time - provide care 
********************************************************************************

* Prepare validation data
use year dwt dgn ageGroup careHours ProvidedWeekly dag using ///
	"$dir_data/ukhls_validation_full_sample.dta", clear



	
	
* Prepare simualted data 	
