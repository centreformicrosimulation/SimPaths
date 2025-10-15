********************************************************************************
* PROJECT:  		ESPON 
* SECTION:			Validation
* OBJECT: 			Disability 
* AUTHORS:			Ashley Burdett 
* LAST UPDATE:		9/25 (AB)
* COUNTRY: 			UK 

* NOTES: 			
********************************************************************************

********************************************************************************
* 1 : Mean values over time
********************************************************************************

********************************************************************************
* 1.1 : Mean values over time, working age (18-65)
********************************************************************************

* Prepare validation data 
use year dwt dlltsd using ///
	"$dir_data/ukhls_validation_sample.dta", clear
	
collapse (mean) dlltsd [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

	
* Prepare simulation data 
use year sim_dlltsd run using "$dir_data/simulated_data.dta", clear

collapse (mean) sim_dlltsd, by(run year)

collapse (mean) sim_dlltsd (sd) sim_dlltsd_sd = sim_dlltsd, by(year)

gen sim_dlltsd_high = sim_dlltsd + 1.96*sim_dlltsd_sd
gen sim_dlltsd_low = sim_dlltsd - 1.96*sim_dlltsd_sd

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen


* Plot figure 
twoway (rarea sim_dlltsd_high sim_dlltsd_low year, sort color(green%20) ///
	legend(label(1 "Simulated "))) ///
(line dlltsd year, sort color(green) ///
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
"$dir_output_files/disability/validation_${country}_disability_ts_${min_age}_${max_age}_both.jpg", ///
	replace width(2400) height(1350) quality(100)
		
	
********************************************************************************
* 1.2 : Mean values over time, working age (18-65), by gender
********************************************************************************

* Prepare validation data 
use year dwt dlltsd dgn using ///
	"$dir_data/ukhls_validation_sample.dta", clear
	
collapse (mean) dlltsd [aw = dwt], by(year dgn)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulation data 
use year sim_dlltsd run dgn using "$dir_data/simulated_data.dta", clear

gen dgn2 = 0 if dgn == "Female"
replace dgn2 = 1 if dgn == "Male"

drop dgn
rename dgn2 dgn 

collapse (mean) sim_dlltsd, by(run year dgn)

collapse (mean) sim_dlltsd (sd) sim_dlltsd_sd = sim_dlltsd, by(year dgn)

gen sim_dlltsd_high = sim_dlltsd + 1.96*sim_dlltsd_sd
gen sim_dlltsd_low = sim_dlltsd - 1.96*sim_dlltsd_sd

merge 1:1 year dgn using "$dir_data/temp_valid_stats.dta", keep(3) nogen


twoway (rarea sim_dlltsd_high sim_dlltsd_low year if dgn == 0, sort color(green%20) ///
	legend(label(1 "Female, simulated"))) ///
(line dlltsd year if dgn == 0, sort color(green) ///
	legend(label(2 "Female, UKHLS "))) ///
	(rarea sim_dlltsd_high sim_dlltsd_low year if dgn == 1, sort color(red%20) ///
	legend(label(3 "Male, simulated"))) ///
(line dlltsd year if dgn == 1, sort color(red) ///
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
"$dir_output_files/disability/validation_${country}_disability_ts_${min_age}_${max_age}_male_female.jpg", ///
	replace width(2560) height(1440) quality(100)


********************************************************************************
* 1.2 : Mean values over time, working age (18-65), by age
********************************************************************************

* Prepare validation data 
use year dwt dlltsd dgn dag ageGroup using ///
	"$dir_data/ukhls_validation_full_sample.dta", clear
	
drop if ageGroup == 0 | ageGroup == 8  

collapse (mean) dlltsd [aweight=dwt], by(ageGroup year)

drop if missing(ageGroup)

reshape wide dlltsd, i(year) j(ageGroup)

forvalues i = 1(1)7 {
	
	rename dlltsd`i' dlltsd_`i'_valid

}

save "$dir_data/temp_valid_stats_full.dta", replace


* Prepare simulation data
use run year sim_dlltsd ageGroup using "$dir_data/simulated_data.dta", clear

collapse (mean) sim_dlltsd, by(ageGroup run year)

drop if missing(ageGroup)

reshape wide sim_dlltsd, i(year run) j(ageGroup)

forvalues i = 1(1)7{
	
	rename sim_dlltsd`i' dlltsd_`i'_sim

}

collapse (mean) dlltsd*  ///
	(sd) sd_dlltsd_1_sim = dlltsd_1_sim ///
		 sd_dlltsd_2_sim = dlltsd_2_sim ///
		 sd_dlltsd_3_sim = dlltsd_3_sim ///
	     sd_dlltsd_4_sim = dlltsd_4_sim ///
		 sd_dlltsd_5_sim = dlltsd_5_sim ///
		 sd_dlltsd_6_sim = dlltsd_6_sim ///
		 sd_dlltsd_7_sim = dlltsd_7_sim ///
		 , by(year)
		 
forvalues i = 1(1)7 {

	gen dlltsd_`i'_sim_high = dlltsd_`i'_sim + 1.96*sd_dlltsd_`i'_sim
	gen dlltsd_`i'_sim_low = dlltsd_`i'_sim - 1.96*sd_dlltsd_`i'_sim

}

recast double year 

merge 1:1 year using "$dir_data/temp_valid_stats_full.dta", keep(3) nogen

* Plot figures
foreach vble in "dlltsd" {
	
	twoway (rarea `vble'_1_sim_high `vble'_1_sim_low year, ///
		sort color(red%20) legend(label(1 "Simulated") position(6) rows(1))) ///
	(line `vble'_1_valid year, sort legend(label(2 "UKHLS"))), ///
		title("Age 15-19") ///
		name(`vble'_1, replace) ///
		ylabel(0 [0.1] 0.2, labsize(vsmall)) ///
		xlabel(, labsize(vsmall)) ///
		ytitle("Share", size(small)) ///
		xtitle("Year", size(small)) ///
		legend(size(small)) ///
		graphregion(color(white))
	
	twoway (rarea `vble'_2_sim_high `vble'_2_sim_low year, ///
		sort color(red%20) legend(label(1 "Simulated") position(6) rows(1))) ///
		(line `vble'_2_valid year, sort legend(label(2 "UKHLS"))), ///
		title("Age 20-24") ///
		name(`vble'_2, replace) ///
		ylabel(0 [0.1] 0.2, labsize(vsmall)) ///
		xlabel(, labsize(vsmall)) ///
		ytitle("Share", size(small)) ///
		xtitle("Year", size(small)) ///
		legend(size(small)) ///
		graphregion(color(white))
	
	twoway (rarea `vble'_3_sim_high `vble'_3_sim_low year, ///
		sort color(red%20) legend(label(1 "Simulated") position(6) rows(1))) ///
		(line `vble'_3_valid year, sort legend(label(2 "UKHLS"))), ///
		title("Age 25-29") ///
		name(`vble'_3, replace) ///
		ylabel(0 [0.1] 0.2, labsize(vsmall)) ///
		xlabel(, labsize(vsmall)) ///
		ytitle("Share", size(small)) ///
		xtitle("Year", size(small)) ///
		legend(size(small)) ///
		graphregion(color(white)) ///
	graphregion(color(white))
	
	twoway (rarea `vble'_4_sim_high `vble'_4_sim_low year, ///
		sort color(red%20) legend(label(1 "Simulated") position(6) rows(1))) ///
		(line `vble'_4_valid year, sort legend(label(2 "UKHLS"))), ///
		title("Age 30-34") ///
		name(`vble'_4, replace) ///
		ylabel(0 [0.1] 0.2, labsize(vsmall)) ///
		xlabel(, labsize(vsmall)) ///
		ytitle("Share", size(small)) ///
		xtitle("Year", size(small)) ///
		legend(size(small)) ///
		graphregion(color(white)) ///
	graphregion(color(white))
	
	twoway (rarea `vble'_5_sim_high `vble'_5_sim_low year, ///
		sort color(red%20) legend(label(1 "Simulated") position(6) rows(1))) ///
		(line `vble'_5_valid year, sort legend(label(2 "UKHLS"))), ///
		title("Age 35-39") ///
		name(`vble'_5, replace) ///
		ylabel(0 [0.1] 0.2, labsize(vsmall)) ///
		xlabel(, labsize(vsmall)) ///
		ytitle("Share", size(small)) ///
		xtitle("Year", size(small)) ///
		legend(size(small)) ///
		graphregion(color(white)) ///
	graphregion(color(white)) 
	
	twoway (rarea `vble'_6_sim_high `vble'_6_sim_low year, ///
		sort color(red%20) legend(label(1 "Simulated") position(6) ///
		rows(1)))(line `vble'_6_valid year, sort ///
		legend(label(2 "UKHLS"))), ///
		title("Age 40-59") ///
		name(`vble'_6, replace) ///
		ylabel(0 [0.1] 0.2, labsize(vsmall)) ///
		xlabel(, labsize(vsmall)) ///
		ytitle("Share", size(small)) ///
		xtitle("Year", size(small)) ///
		legend(size(small)) ///
		graphregion(color(white)) ///
	graphregion(color(white))
	
	twoway (rarea `vble'_7_sim_high `vble'_7_sim_low year, ///
		sort color(red%20) legend(label(1 "Simulated") position(6) rows(1))) ///
		(line `vble'_7_valid year, sort legend(label(2 "UKHLS"))), ///
		title("Age 60-65") ///
		name(`vble'_7, replace) ///
		ylabel(0 [0.1] 0.2, labsize(vsmall)) ///
		xlabel(, labsize(vsmall)) ///
		ytitle("Share", size(small)) ///
		xtitle("Year", size(small)) ///
		legend(size(small)) ///
		graphregion(color(white)) ///
	graphregion(color(white))
	
}

grc1leg dlltsd_1 dlltsd_2 dlltsd_3 dlltsd_4 dlltsd_5 dlltsd_6 dlltsd_7 , ///
	title("Disabled/Long-term Sick by Age Group") ///
	legendfrom(dlltsd_1) ///
	graphregion(color(white)) ///
	note("Notes:", size(vsmall))

	
graph export ///
"$dir_output_files/disability/validation_${country}_disability_ts_all_both.jpg", ///
	replace width(2400) height(1350) quality(100)

	
graph drop _all
