/*******************************************************************************
* PROJECT:  		SimPaths UK 
* SECTION:			Validation
* OBJECT: 			Social care
* AUTHORS:			Ashley Burdett 
* LAST UPDATE:		Feb 2026
* COUNTRY: 			UK 
********************************************************************************
* NOTES: 			
*******************************************************************************/

clear all 

********************************************************************************
* 0: Programs
********************************************************************************

* Time series plot, 
cap program drop make_care_plot

program define make_care_plot

    * Added 'string' to title
	syntax, var(string) title(string) subtitle(string) saving(string) ///
	note(string) [name(string)]
    
	twoway (rarea sim_`var'_h sim_`var'_l year, ///
        sort color(green%20) legend(label(1 "SimPaths"))) ///
        (line valid_`var' year, sort color(green) ///
        legend(label(2 "UKHLS"))), ///
            title("`title'") ///
            subtitle("`subtitle'") ///
            xtitle("Year", size(small)) ///
            ytitle("Share", size(small)) ///  
            xlabel(, labsize(small)) ///
            ylabel(, labsize(small)) ///
            legend(size(small)) ///
			name(`name', replace) ///
            graphregion(color(white)) ///
            note("`note'", size(vsmall))  
    
    graph export "$dir_output_files/social_care/`saving'.jpg", ///
        replace width(2400) height(1350) quality(100)
end


* Program for quantile mean plots 



********************************************************************************
* 1: Mean values over time
********************************************************************************

********************************************************************************
* 1.1: Mean values over time - Share need care 
********************************************************************************

* Prepare validation data 
use year idBu dwt valid_careNeedFlag demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Select sample 	
keep if demAge > 64 & demAge != . 
	
* Compute mean 
collapse (mean) valid_careNeedFlag [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data 
use run year idBu sim_careNeedFlag demAge using ///
	"$dir_data/simulation_sample.dta", clear

* Select sample 	
keep if demAge > 64
	
* Compute mean and sd 
collapse (mean) sim_careNeedFlag, by(year run)

collapse (mean) sim_careNeedFlag ///
		 (sd) sim_careNeedFlag_sd = sim_careNeedFlag, by(year)

* Compute 95% confidence intervals
gen sim_careNeedFlag_h = sim_careNeedFlag + 1.96*sim_careNeedFlag_sd
gen sim_careNeedFlag_l = sim_careNeedFlag - 1.96*sim_careNeedFlag_sd

* Combine datasets 
merge 1:1 year using "$dir_data/temp_valid_stats.dta"

* Plot figure 
make_care_plot, ///
	var("careNeedFlag") ///
	title("In Need of Care") ///
	subtitle("Ages 65+") ///
	saving("validation_${country}_need_care_ts_65plus_both") ///
	note(`""Notes: ""') 

graph drop _all 	
	
	
********************************************************************************
* 1.1.1 : Mean values over time  - Share need care, by gender
********************************************************************************

* Prepare validation data 
use year idBu dwt valid_careNeedFlag demAge demMaleFlag using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Select sample 	
keep if demAge > 64 & demAge != . 
	
* Compute mean 
collapse (mean) valid_careNeedFlag [aw = dwt], by(year demMaleFlag)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data 
use run year idBu sim_careNeedFlag demAge demMaleFlag using ///
	"$dir_data/simulation_sample.dta", clear

* Select sample 	
keep if demAge > 64
	
* Compute mean and sd 
collapse (mean) sim_careNeedFlag, by(year demMaleFlag run)

collapse (mean) sim_careNeedFlag ///
		 (sd) sim_careNeedFlag_sd = sim_careNeedFlag, by(year demMaleFlag)

* Compute 95% confidence intervals
gen sim_careNeedFlag_h = sim_careNeedFlag + 1.96*sim_careNeedFlag_sd
gen sim_careNeedFlag_l = sim_careNeedFlag - 1.96*sim_careNeedFlag_sd

* Combine datasets 
merge 1:1 year demMaleFlag using "$dir_data/temp_valid_stats.dta"

* Plot figures

* Females
twoway (rarea sim_careNeedFlag_h sim_careNeedFlag_l year if ///
	demMaleFlag == 0, sort color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_careNeedFlag year if demMaleFlag == 0, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	subtitle("Females") ///
	name(health_female, replace) ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	ylabel(0[0.1]0.5,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	
* Males
twoway (rarea sim_careNeedFlag_h sim_careNeedFlag_l year if ///
	demMaleFlag == 1, sort color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_careNeedFlag year if demMaleFlag == 1, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	subtitle("Males") ///
	name(health_male, replace) ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	ylabel(0[0.1]0.5,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///	
	
* Combine	
grc1leg health_female health_male, ///
	title("In Need of Care") ///
	subtitle("Ages 65+") ///
	legendfrom(health_female) rows(1) ///
	ycomm ///
	graphregion(color(white)) ///
	note("Notes:  ", ///
	size(vsmall))	
	
* Save figure
graph export ///
"$dir_output_files/social_care/validation_${country}_need_care_ts_65plus_gender.jpg", ///
	replace width(2400) height(1350) quality(100)
	
graph drop _all 


********************************************************************************
* 1.1.2 : Mean values over time  - Share need care, by gender and age group
********************************************************************************

* Prepare validation data
use year dwt demMaleFlag ageGroup valid_careNeedFlag demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear
		
drop ageGroup
gen ageGroup = 1 if inrange(demAge,65,69)	
replace ageGroup = 2 if inrange(demAge,70,74)
replace ageGroup = 3 if inrange(demAge,75,79)
replace ageGroup = 4 if inrange(demAge,80,84)
replace ageGroup = 5 if inrange(demAge,85,89)
replace ageGroup = 6 if inrange(demAge,90,110)	

* Select sample 
drop if demAge < 65
	
* Exstensive margin dummy 		
replace valid_careNeedFlag = . if valid_careNeedFlag < 0 
	
gen care_m = valid_careNeedFlag if demMaleFlag == 1
gen care_f = valid_careNeedFlag if demMaleFlag == 0

* Compute means 
collapse (mean) care* [aw = dwt], by(ageGroup year)

* Restructure data 
drop if missing(ageGroup)
reshape wide care*, i(year) j(ageGroup)

forvalues i = 1(1)6 {

	rename care_f`i' care_f_`i'_valid
	rename care_m`i' care_m_`i'_valid

}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare Simulated data
use run year demMaleFlag ageGroup sim_careNeedFlag demAge using ///
	"$dir_data/simulation_sample.dta", clear

drop ageGroup
gen ageGroup = 1 if inrange(demAge,65,69)	
replace ageGroup = 2 if inrange(demAge,70,74)
replace ageGroup = 3 if inrange(demAge,75,79)
replace ageGroup = 4 if inrange(demAge,80,84)
replace ageGroup = 5 if inrange(demAge,85,89)
replace ageGroup = 6 if inrange(demAge,90,110)	

* Select sample 
drop if demAge < 65	
	
* Gender specific vars
gen care_m = sim_careNeedFlag if demMaleFlag == 1
gen care_f = sim_careNeedFlag if demMaleFlag == 0

drop sim_careNeedFlag

* Compute means 
collapse (mean) care*, by(ageGroup run year)

* Restructure data 
drop if missing(ageGroup)
reshape wide care*, i(year run) j(ageGroup)

* COmpute sd 
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

	
* Approx 95% confidence interval 
forvalues i=1(1)6 {
	
	gen care_f_`i'_sim_high = care_f`i' + 1.96*care_f_`i'_sd
	gen care_f_`i'_sim_low = care_f`i' - 1.96*care_f_`i'_sd
	gen care_m_`i'_sim_high = care_m`i' + 1.96*care_m_`i'_sd
	gen care_m_`i'_sim_low = care_m`i' - 1.96*care_m_`i'_sd	

}		 

* Combine datasets 
merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
foreach vble in "care_f" "care_m" {
	
	twoway (rarea `vble'_1_sim_high `vble'_1_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_1_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 65-69") ///
		name(`vble'_1, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Share", size(small)) ///
		ylabel(0(0.3)0.9, labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))
		
	twoway (rarea `vble'_2_sim_high `vble'_2_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_2_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 70-74") ///
		name(`vble'_2, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Share", size(small)) ///
		ylabel(0(0.3)0.9, labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))
		
	twoway (rarea `vble'_3_sim_high `vble'_3_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_3_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 75-79") ///
		name(`vble'_3, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Share", size(small)) ///
		ylabel(0(0.3)0.9, labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))

	twoway (rarea `vble'_4_sim_high `vble'_4_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_4_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 80-84") ///
		name(`vble'_4, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Share", size(small)) ///
		ylabel(0(0.3)0.9, labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))

	twoway (rarea `vble'_5_sim_high `vble'_5_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_5_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 85-89") ///
		name(`vble'_5, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Share", size(small)) ///
		ylabel(0(0.3)0.9, labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))

	twoway (rarea `vble'_6_sim_high `vble'_6_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_6_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 90+") ///
		name(`vble'_6, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Share", size(small)) ///
		ylabel(0(0.3)0.9, labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))


}

* Save figures
grc1leg care_f_1 care_f_2 care_f_3 care_f_4 care_f_5 care_f_6 , ///
	title("In Need of Care") ///
	subtitle("Females") ///
	legendfrom(care_f_1) ///
	ycomm ///
	graphregion(color(white)) ///
note("Notes:  ", ///
	size(vsmall)) 	
	
graph export ///
"$dir_output_files/social_care/validation_${country}_need_care_ts_65plus_female.jpg", ///
	replace width(2400) height(1350) quality(100)
		

grc1leg care_m_1 care_m_2 care_m_3 care_m_4 care_m_5 care_m_6, ///
	title("In Need of Care") ///
	subtitle("Males") ///
	legendfrom(care_m_1) ///
	ycomm ///
	graphregion(color(white)) ///
note("Notes: ", ///
	size(vsmall)) 	
	
graph export ///
"$dir_output_files/social_care/validation_${country}_need_care_ts_65plus_male.jpg", ///
	replace width(2400) height(1350) quality(100)

graph drop _all 	


/*
* Define the subtitles in a local macro
local titles "65-69" "70-74" "75-79" "80-84" "85-89" "90+"

foreach vble in "care_f" "care_m" {
    forvalues i = 1/6 {
		
        local t : word `i' of `titles'  
        
        twoway (rarea `vble'_`i'_sim_high `vble'_`i'_sim_low year, ///
			sort color(red%20)) ///
               (line `vble'_`i'_valid year, sort color(red)), ///
               subtitle("Age `t'") name(`vble'_`i', replace) ///
               ylabel(0(0.3)0.9, labsize(vsmall)) ///  
               graphregion(color(white)) legend(off) 
			   
    }
}
*/


********************************************************************************
* 1.2: Mean values over time - Share receive care 
********************************************************************************

* Prepare validation data 
use year idBu dwt valid_careReceiveFlag demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Select sample 	
keep if demAge > 64 & demAge != . 
	
* Compute mean 
collapse (mean) valid_careReceiveFlag [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data 
use run year idBu sim_careReceiveFlag demAge using ///
	"$dir_data/simulation_sample.dta", clear

* Select sample 	
keep if demAge > 64
	
* Compute mean and sd 
collapse (mean) sim_careReceiveFlag, by(year run)

collapse (mean) sim_careReceiveFlag ///
		 (sd) sim_careReceiveFlag_sd = sim_careReceiveFlag, by(year)

* Compute 95% confidence intervals
gen sim_careReceiveFlag_h = sim_careReceiveFlag + 1.96*sim_careReceiveFlag_sd
gen sim_careReceiveFlag_l = sim_careReceiveFlag - 1.96*sim_careReceiveFlag_sd

* Combine datasets 
merge 1:1 year using "$dir_data/temp_valid_stats.dta"

* Plot figure 
make_care_plot, ///
	var("careReceiveFlag") ///
	title("Receive Care") ///
	subtitle("Ages 65+") ///
	saving("validation_${country}_receive_care_ts_65plus_both") ///
	note(`""Notes: ""') 
	
	
********************************************************************************
* 1.2.1: Mean values over time - Share receive care, by gender
********************************************************************************

* Prepare validation data 
use year idBu dwt valid_careReceiveFlag demAge demMaleFlag using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Select sample 	
keep if demAge > 64 & demAge != . 
	
* Compute mean 
collapse (mean) valid_careReceiveFlag [aw = dwt], by(year demMaleFlag)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data 
use run year idBu sim_careReceiveFlag demAge demMaleFlag using ///
	"$dir_data/simulation_sample.dta", clear

* Select sample 	
keep if demAge > 64
	
* Compute mean and sd 
collapse (mean) sim_careReceiveFlag, by(year demMaleFlag run)

collapse (mean) sim_careReceiveFlag ///
		 (sd) sim_careReceiveFlag_sd = sim_careReceiveFlag, by(year demMaleFlag)

* Compute 95% confidence intervals
gen sim_careReceiveFlag_h = sim_careReceiveFlag + 1.96*sim_careReceiveFlag_sd
gen sim_careReceiveFlag_l = sim_careReceiveFlag - 1.96*sim_careReceiveFlag_sd

* Combine datasets 
merge 1:1 year demMaleFlag using "$dir_data/temp_valid_stats.dta"

* Plot figures

* Females
twoway (rarea sim_careReceiveFlag_h sim_careReceiveFlag_l year if ///
	demMaleFlag == 0, sort color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_careReceiveFlag year if demMaleFlag == 0, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	subtitle("Females") ///
	name(health_female, replace) ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	ylabel(0[0.1]0.5,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	
* Males
twoway (rarea sim_careReceiveFlag_h sim_careReceiveFlag_l year if ///
	demMaleFlag == 1, sort color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_careReceiveFlag year if demMaleFlag == 1, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	subtitle("Males") ///
	name(health_male, replace) ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	ylabel(0[0.1]0.5,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///	
	
* Combine	
grc1leg health_female health_male, ///
	title("Receive Care") ///
	subtitle("Ages 65+") ///
	legendfrom(health_female) rows(1) ///
	ycomm ///
	graphregion(color(white)) ///
	note("Notes:  ", ///
	size(vsmall))	
	
* Save figure
graph export ///
"$dir_output_files/social_care/validation_${country}_receive_care_ts_65plus_gender.jpg", ///
	replace width(2400) height(1350) quality(100)
	
graph drop _all 


********************************************************************************
* 1.2.2: Mean values over time - Share receive care, by gender and age group 
********************************************************************************

* Prepare validation data
use year dwt demMaleFlag ageGroup valid_careReceiveFlag demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear
		
drop ageGroup
gen ageGroup = 1 if inrange(demAge,65,69)	
replace ageGroup = 2 if inrange(demAge,70,74)
replace ageGroup = 3 if inrange(demAge,75,79)
replace ageGroup = 4 if inrange(demAge,80,85)
replace ageGroup = 5 if inrange(demAge,85,89)
replace ageGroup = 6 if inrange(demAge,90,110)	

* Select sample 
drop if demAge < 65
	
* Exstensive margin dummy 		
gen care_m = valid_careReceiveFlag if demMaleFlag == 1
gen care_f = valid_careReceiveFlag if demMaleFlag == 0

* Compute means 
collapse (mean) care* [aw = dwt], by(ageGroup year)

* Restructure data 
drop if missing(ageGroup)
reshape wide care*, i(year) j(ageGroup)

forvalues i = 1(1)6 {

	rename care_f`i' care_f_`i'_valid
	rename care_m`i' care_m_`i'_valid

}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare Simulated data
use run year demMaleFlag ageGroup sim_careReceiveFlag demAge using ///
	"$dir_data/simulation_sample.dta", clear

drop ageGroup
gen ageGroup = 1 if inrange(demAge,65,69)	
replace ageGroup = 2 if inrange(demAge,70,74)
replace ageGroup = 3 if inrange(demAge,75,79)
replace ageGroup = 4 if inrange(demAge,80,85)
replace ageGroup = 5 if inrange(demAge,85,89)
replace ageGroup = 6 if inrange(demAge,90,110)	

* Select sample 
drop if demAge < 65	
	
* Gender specific vars
gen care_m = sim_careReceiveFlag if demMaleFlag == 1
gen care_f = sim_careReceiveFlag if demMaleFlag == 0

drop sim_careReceiveFlag

* Compute means 
collapse (mean) care*, by(ageGroup run year)

* Restructure data 
drop if missing(ageGroup)
reshape wide care*, i(year run) j(ageGroup)

* COmpute sd 
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

	
* Approx 95% confidence interval 
forvalues i=1(1)6 {
	
	gen care_f_`i'_sim_high = care_f`i' + 1.96*care_f_`i'_sd
	gen care_f_`i'_sim_low = care_f`i' - 1.96*care_f_`i'_sd
	gen care_m_`i'_sim_high = care_m`i' + 1.96*care_m_`i'_sd
	gen care_m_`i'_sim_low = care_m`i' - 1.96*care_m_`i'_sd	

}		 

* Combine datasets 
merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
foreach vble in "care_f" "care_m" {
	
	twoway (rarea `vble'_1_sim_high `vble'_1_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_1_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 65-69") ///
		name(`vble'_1, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Share", size(small)) ///
		ylabel(0(0.3)0.9, labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))
		
	twoway (rarea `vble'_2_sim_high `vble'_2_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_2_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 70-74") ///
		name(`vble'_2, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Share", size(small)) ///
		ylabel(0(0.3)0.9, labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))
		
	twoway (rarea `vble'_3_sim_high `vble'_3_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_3_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 75-79") ///
		name(`vble'_3, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Share", size(small)) ///
		ylabel(0(0.3)0.9, labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))

	twoway (rarea `vble'_4_sim_high `vble'_4_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_4_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 80-84") ///
		name(`vble'_4, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Share", size(small)) ///
		ylabel(0(0.3)0.9, labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))

	twoway (rarea `vble'_5_sim_high `vble'_5_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_5_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 85-89") ///
		name(`vble'_5, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Share", size(small)) ///
		ylabel(0(0.3)0.9, labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))

	twoway (rarea `vble'_6_sim_high `vble'_6_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_6_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 90+") ///
		name(`vble'_6, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Share", size(small)) ///
		ylabel(0(0.3)0.9, labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))

}

* Save figures
grc1leg care_f_1 care_f_2 care_f_3 care_f_4 care_f_5 care_f_6 , ///
	title("Receive Care") ///
	subtitle("Females") ///
	legendfrom(care_f_1) ///
	ycomm ///
	graphregion(color(white)) ///
note("Notes:  ", ///
	size(vsmall)) 	
	
graph export ///
"$dir_output_files/social_care/validation_${country}_receive_care_ts_65plus_female.jpg", ///
	replace width(2400) height(1350) quality(100)
		

grc1leg care_m_1 care_m_2 care_m_3 care_m_4 care_m_5 care_m_6, ///
	title("Receive Care") ///
	subtitle("Males") ///
	legendfrom(care_m_1) ///
	ycomm ///
	graphregion(color(white)) ///
note("Notes: ", ///
	size(vsmall)) 	
	
graph export ///
"$dir_output_files/social_care/validation_${country}_receive_care_ts_65plus_male.jpg", ///
	replace width(2400) height(1350) quality(100)

graph drop _all 


********************************************************************************
* 1.3: Mean values over time - Share type of care received
********************************************************************************

* Prepare validation data 
use year idBu dwt valid_careReceiveFlag valid_careRecFormalOnly ///
	valid_careRecInformalOnly valid_careRecMix demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Select sample 	
keep if demAge > 64 & demAge != . 
keep if valid_careReceiveFlag == 1 

* Compute mean 
collapse (mean) valid_careRecFormalOnly valid_careRecInformalOnly ///
	valid_careRecMix [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data 
use run year idBu sim_careReceiveFlag sim_careRecFormalOnly ///
	sim_careRecInformalOnly sim_careRecMix demAge using ///
	"$dir_data/simulation_sample.dta", clear

* Select sample 	
keep if demAge > 64
keep if sim_careReceiveFlag == 1 
	
* Compute mean and sd 
collapse (mean) sim_careRecFormalOnly sim_careRecInformalOnly ///
	sim_careRecMix, by(year run)

collapse (mean) sim_careRecFormalOnly sim_careRecInformalOnly ///
	sim_careRecMix ///
		 (sd) sim_careRecFormalOnly_sd = sim_careRecFormalOnly ///
		 (sd) sim_careRecInformalOnly_sd = sim_careRecInformalOnly ///
		 (sd) sim_careRecMix_sd = sim_careRecMix ///
		 , by(year)

* Compute 95% confidence intervals
foreach varname in sim_careRecFormalOnly sim_careRecInformalOnly ///
	sim_careRecMix {
	
	gen `varname'_h = `varname' + 1.96*`varname'_sd
	gen `varname'_l = `varname' - 1.96*`varname'_sd

}
	
* Combine datasets 
merge 1:1 year using "$dir_data/temp_valid_stats.dta", nogen

* Plot figure 
twoway ///
    (rarea sim_careRecFormalOnly_h sim_careRecFormalOnly_l year, ///
		sort color(green%20) legend(label(1 "Formal care only, SimPaths"))) ///
    (line valid_careRecFormalOnly year, sort color(green) ///	
		legend(label(2 "Formal care only, UKHLS"))) ///
    (rarea sim_careRecInformalOnly_h sim_careRecInformalOnly_l year, ///
		sort color(blue%20) legend(label(3 "Informal care only, SimPaths"))) ///
    (line valid_careRecInformalOnly year, sort color(blue) ///
		legend(label(4 "Informal care only, UKHLS"))) ///
    (rarea sim_careRecMix_h sim_careRecMix_l year, sort color(red%20) ///
		legend(label(5 "Mixed care, SimPaths"))) ///
    (line valid_careRecMix year, sort color(red) ///
		legend(label(6 "Mixed care, UKHLS"))), ///
        title("Type of Care Received") ///
		subtitle("Ages 65+") ///
        xtitle("Year", size(small)) ///
		ytitle("Share", size(small)) ///
		xlabel(, labsize(small)) ///
		ylabel(, labsize(small)) ///			
        graphregion(color(white)) ///
		legend(size(small)) ///
	note("Note: Only those receiving care included in the sample. ", ///
		size(vsmall))  
    
graph export ///
"$dir_output_files/social_care/validation_${country}_care_type_ts_65plus_both.jpg", ///
	replace width(2400) height(1350) quality(100)	
	
graph drop _all 

	
********************************************************************************
* 1.3.1: Mean values over time - Share type of care received, by gender
********************************************************************************

* Prepare validation data 
use year idBu dwt valid_careReceiveFlag valid_careRecFormalOnly ///
	valid_careRecInformalOnly valid_careRecMix demAge demMaleFlag using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Select sample 	
keep if demAge > 64 & demAge != . 
keep if valid_careReceiveFlag == 1 

* Compute mean 
collapse (mean) valid_careRecFormalOnly valid_careRecInformalOnly ///
	valid_careRecMix [aw = dwt], by(year demMaleFlag)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data 
use run year idBu sim_careReceiveFlag sim_careRecFormalOnly ///
	sim_careRecInformalOnly sim_careRecMix demAge demMaleFlag using ///
	"$dir_data/simulation_sample.dta", clear

* Select sample 	
keep if demAge > 64
keep if sim_careReceiveFlag == 1 
	
* Compute mean and sd 
collapse (mean) sim_careRecFormalOnly sim_careRecInformalOnly ///
	sim_careRecMix, by(year demMaleFlag run)

collapse (mean) sim_careRecFormalOnly sim_careRecInformalOnly ///
	sim_careRecMix ///
		 (sd) sim_careRecFormalOnly_sd = sim_careRecFormalOnly ///
		 (sd) sim_careRecInformalOnly_sd = sim_careRecInformalOnly ///
		 (sd) sim_careRecMix_sd = sim_careRecMix ///
		 , by(year demMaleFlag)

* Compute 95% confidence intervals
foreach varname in sim_careRecFormalOnly sim_careRecInformalOnly ///
	sim_careRecMix {
	
	gen `varname'_h = `varname' + 1.96*`varname'_sd
	gen `varname'_l = `varname' - 1.96*`varname'_sd

}
	
* Combine datasets 
merge 1:1 year demMaleFlag using "$dir_data/temp_valid_stats.dta"

* Plot figure 

* Males
preserve 

keep if demMaleFlag == 1

twoway ///
    (rarea sim_careRecFormalOnly_h sim_careRecFormalOnly_l year, ///
		sort color(green%20) legend(label(1 "Formal care only, SimPaths"))) ///
    (line valid_careRecFormalOnly year, sort color(green) ///	
		legend(label(2 "Formal care only, UKHLS"))) ///
    (rarea sim_careRecInformalOnly_h sim_careRecInformalOnly_l year, ///
		sort color(blue%20) legend(label(3 "Informal care only, SimPaths"))) ///
    (line valid_careRecInformalOnly year, sort color(blue) ///
		legend(label(4 "Informal care only, UKHLS"))) ///
    (rarea sim_careRecMix_h sim_careRecMix_l year, sort color(red%20) ///
		legend(label(5 "Mixed care, SimPaths"))) ///
    (line valid_careRecMix year, sort color(red) ///
		legend(label(6 "Mixed care, UKHLS"))), ///
        title("Type of Care Received") ///
		subtitle("Ages 65+, males") ///
        xtitle("Year", size(small)) ///
		ytitle("Share", size(small)) ///
		xlabel(, labsize(small)) ///
		ylabel(, labsize(small)) ///			
        graphregion(color(white)) ///
		legend(size(small)) ///
	note("Note: Only those receiving care included in the sample. ", ///
		size(vsmall))  
    
graph export ///
"$dir_output_files/social_care/validation_${country}_care_type_ts_65plus_male.jpg", ///
	replace width(2400) height(1350) quality(100)

restore 	
	
* Females
preserve 

keep if demMaleFlag == 0

twoway ///
    (rarea sim_careRecFormalOnly_h sim_careRecFormalOnly_l year, ///
		sort color(green%20) legend(label(1 "Formal care only, SimPaths"))) ///
    (line valid_careRecFormalOnly year, sort color(green) ///	
		legend(label(2 "Formal care only, UKHLS"))) ///
    (rarea sim_careRecInformalOnly_h sim_careRecInformalOnly_l year, ///
		sort color(blue%20) legend(label(3 "Informal care only, SimPaths"))) ///
    (line valid_careRecInformalOnly year, sort color(blue) ///
		legend(label(4 "Informal care only, UKHLS"))) ///
    (rarea sim_careRecMix_h sim_careRecMix_l year, sort color(red%20) ///
		legend(label(5 "Mixed care, SimPaths"))) ///
    (line valid_careRecMix year, sort color(red) ///
		legend(label(6 "Mixed care, UKHLS"))), ///
        title("Type of Care Received") ///
		subtitle("Ages 65+, females") ///
        xtitle("Year", size(small)) ///
		ytitle("Share", size(small)) ///
		xlabel(, labsize(small)) ///
		ylabel(, labsize(small)) ///			
        graphregion(color(white)) ///
		legend(size(small)) ///
	note("Note: Only those recieving care included in the sample. ", ///
		size(vsmall))  
    
graph export ///
"$dir_output_files/social_care/validation_${country}_care_type_ts_65plus_female.jpg", ///
	replace width(2400) height(1350) quality(100)

restore 		
	
graph drop _all 


********************************************************************************
* 1.4: Mean values over time - Average hours of care received
********************************************************************************

* Prepare validation data 
use year idBu dwt valid_careReceiveHrs valid_careReceiveFlag demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Select sample 	
keep if demAge > 64 & demAge != .
keep if valid_careReceiveFlag == 1
 
* Compute mean 
collapse (mean) valid_careReceiveHrs [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data 
use run year idBu sim_careReceiveHrs sim_careReceiveFlag demAge using ///
	"$dir_data/simulation_sample.dta", clear

* Select sample 	
keep if demAge > 64
keep if sim_careReceiveFlag == 1
	
* Compute mean and sd 
collapse (mean) sim_careReceiveHrs, by(year run)

collapse (mean) sim_careReceiveHrs ///
		 (sd) sim_careReceiveHrs_sd = sim_careReceiveHrs, by(year)

* Compute 95% confidence intervals
gen sim_careReceiveHrs_h = sim_careReceiveHrs + 1.96*sim_careReceiveHrs_sd
gen sim_careReceiveHrs_l = sim_careReceiveHrs - 1.96*sim_careReceiveHrs_sd

* Combine datasets 
merge 1:1 year using "$dir_data/temp_valid_stats.dta"

* Plot figure 
make_care_plot, ///
	var("careReceiveHrs") ///
	title("Hours of Care Received") ///
	subtitle("Ages 65+") ///
	saving("validation_${country}_care_hours_received_ts_65plus_both") ///
	note(`""Notes: ""') 

graph drop _all 


********************************************************************************
* 1.4.1: Mean values over time - Average hours of care received, by gender
********************************************************************************

* Prepare validation data 
use year idBu dwt valid_careReceiveFlag valid_careReceiveHrs demAge ///
	demMaleFlag using "$dir_data/ukhls_validation_sample.dta", clear

* Select sample 	
keep if demAge > 64 & demAge != . 
keep if valid_careReceiveFlag == 1
	
* Compute mean 
collapse (mean) valid_careReceiveHrs [aw = dwt], by(year demMaleFlag)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data 
use run year idBu sim_careReceiveFlag sim_careReceiveHrs demAge demMaleFlag ///
	using "$dir_data/simulation_sample.dta", clear

* Select sample 	
keep if demAge > 64
keep if sim_careReceiveFlag == 1
	
* Compute mean and sd 
collapse (mean) sim_careReceiveHrs, by(year demMaleFlag run)

collapse (mean) sim_careReceiveHrs ///
		 (sd) sim_careReceiveHrs_sd = sim_careReceiveHrs, by(year demMaleFlag)

* Compute 95% confidence intervals
gen sim_careReceiveHrs_h = sim_careReceiveHrs + 1.96*sim_careReceiveHrs_sd
gen sim_careReceiveHrs_l = sim_careReceiveHrs - 1.96*sim_careReceiveHrs_sd

* Combine datasets 
merge 1:1 year demMaleFlag using "$dir_data/temp_valid_stats.dta"

* Plot figures

* Females
twoway (rarea sim_careReceiveHrs_h sim_careReceiveHrs_l year if ///
	demMaleFlag == 0, sort color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_careReceiveHrs year if demMaleFlag == 0, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	subtitle("Females") ///
	name(health_female, replace) ///
	xtitle("Year", size(small)) ///
	ytitle("Hours per week", size(small)) ///
	ylabel(,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	
* Males
twoway (rarea sim_careReceiveHrs_h sim_careReceiveHrs_l year if ///
	demMaleFlag == 1, sort color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_careReceiveHrs year if demMaleFlag == 1, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	subtitle("Males") ///
	name(health_male, replace) ///
	xtitle("Year", size(small)) ///
	ytitle("Hours per week", size(small)) ///
	ylabel(,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///	
	
* Combine	
grc1leg health_female health_male, ///
	title("Hours of Care Received") ///
	subtitle("Ages 65+") ///
	legendfrom(health_female) rows(1) ///
	ycomm ///
	graphregion(color(white)) ///
	note("Notes: Only those receiving care included in sample.  ", ///
	size(vsmall))	
	
* Save figure
graph export ///
"$dir_output_files/social_care/validation_${country}_care_hours_received_ts_65plus_gender.jpg", ///
	replace width(2400) height(1350) quality(100)
	
graph drop _all 


********************************************************************************
* 1.5: Mean values over time - Quantile means of hours of care received
********************************************************************************

* Prepare validation data 
use year idBu dwt valid_careReceiveHrs valid_careReceiveFlag demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Select sample 	
keep if demAge > 64 & demAge != .
keep if valid_careReceiveFlag == 1
 
* Compute mean 
collapse 	(p25) valid_p25 = valid_careReceiveHrs ///
			(p50) valid_p50 = valid_careReceiveHrs	///
			(p75) valid_p75 = valid_careReceiveHrs [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data 
use run year idBu sim_careReceiveHrs sim_careReceiveFlag demAge using ///
	"$dir_data/simulation_sample.dta", clear

* Select sample 	
keep if demAge > 64
keep if sim_careReceiveFlag == 1
	
* Compute quantiles and sd 
bysort year run: egen sim_p25_run = pctile(sim_careReceiveHrs), p(25)
bysort year run: egen sim_p50_run = pctile(sim_careReceiveHrs), p(50) 
bysort year run: egen sim_p75_run = pctile(sim_careReceiveHrs), p(75)

collapse (mean) sim_p25 = sim_p25_run ///
				sim_p50 = sim_p50_run ///
				sim_p75 = sim_p75_run ///
         (sd) 	sim_p25_sd = sim_p25 ///
				sim_p50_sd = sim_p50 ///
				sim_p75_sd = sim_p75, by(year)
		 
* Approx 95% confidence intervals
foreach p in 25 50 75 {
	
    gen sim_p`p'_lo = sim_p`p' - 1.96*sim_p`p'_sd
    gen sim_p`p'_hi = sim_p`p' + 1.96*sim_p`p'_sd

}

* Combine datasets 
merge 1:1 year using "$dir_data/temp_valid_stats.dta"

* Plot figure 
twoway (rarea sim_p25_lo sim_p25_hi year, ///
	sort color(green%20) legend(label(1 "SimPaths, p25"))) ///
(line valid_p25 year, sort color(green) ///
	legend(label(2 "UKHLS, p25"))) ///
(rarea sim_p50_lo sim_p50_hi year, ///
	sort color(blue%20) legend(label(3 "SimPaths, p50"))) ///
(line valid_p50 year, sort color(blue) ///
	legend(label(4 "UKHLS, p50"))) ///
(rarea sim_p75_lo sim_p75_hi year, ///
	sort color(red%20) legend(label(5 "SimPaths, p75"))) ///
(line valid_p75 year, sort color(red) ///
	legend(label(6 "UKHLS, p75"))), ///
	title("Hours of Care Received ") ///
	subtitle("Ages 65+") ///
	xtitle("Year", size(small)) ///
	ytitle("Hours per week", size(small)) ///) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: Percentiles computed on the 65+ population that report receiving positive hours of informal care.", size(vsmall))

* Save figure
graph export ///
"$dir_output_files/social_care/validation_${country}_care_hours_received_quantilests_65plus_both.jpg", ///
	replace width(2400) height(1350) quality(100)

graph drop _all 
	
	
********************************************************************************
* 1.5.1: Mean values over time - Quantile means of hours of care 
*	 		received, by gender
********************************************************************************



********************************************************************************
* 1.6: Mean values over time - Quantile means of hours of informal care received
********************************************************************************

* Prepare validation data 
use year idBu dwt valid_careHrsInformal demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Select sample 	
keep if demAge > 64 & demAge != .
keep if valid_careHrsInformal > 0 
 
* Compute mean 
collapse 	(p25) valid_p25 = valid_careHrsInformal ///
			(p50) valid_p50 = valid_careHrsInformal	///
			(p75) valid_p75 = valid_careHrsInformal [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data 
use run year idBu sim_careHrsInformal  demAge using ///
	"$dir_data/simulation_sample.dta", clear

* Select sample 	
keep if demAge > 64
keep if sim_careHrsInformal > 0
	
* Compute quantiles and sd 
bysort year run: egen sim_p25_run = pctile(sim_careHrsInformal), p(25)
bysort year run: egen sim_p50_run = pctile(sim_careHrsInformal), p(50) 
bysort year run: egen sim_p75_run = pctile(sim_careHrsInformal), p(75)

collapse (mean) sim_p25 = sim_p25_run ///
				sim_p50 = sim_p50_run ///
				sim_p75 = sim_p75_run ///
         (sd) 	sim_p25_sd = sim_p25 ///
				sim_p50_sd = sim_p50 ///
				sim_p75_sd = sim_p75, by(year)
		 

* Approx 95% confidence intervals
foreach p in 25 50 75 {
	
    gen sim_p`p'_lo = sim_p`p' - 1.96*sim_p`p'_sd
    gen sim_p`p'_hi = sim_p`p' + 1.96*sim_p`p'_sd

}

* Combine datasets 
merge 1:1 year using "$dir_data/temp_valid_stats.dta"

* Plot figure 
twoway (rarea sim_p25_lo sim_p25_hi year, ///
	sort color(green%20) legend(label(1 "SimPaths, p25"))) ///
(line valid_p25 year, sort color(green) ///
	legend(label(2 "UKHLS, p25"))) ///
(rarea sim_p50_lo sim_p50_hi year, ///
	sort color(blue%20) legend(label(3 "SimPaths, p50"))) ///
(line valid_p50 year, sort color(blue) ///
	legend(label(4 "UKHLS, p50"))) ///
(rarea sim_p75_lo sim_p75_hi year, ///
	sort color(red%20) legend(label(5 "SimPaths, p75"))) ///
(line valid_p75 year, sort color(red) ///
	legend(label(6 "UKHLS, p75"))), ///
	title("Hours of Informal Care Received ") ///
	subtitle("Ages 65+") ///
	xtitle("Year", size(small)) ///
	ytitle("Hours per week", size(small)) ///) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: Percentiles computed on the 65+ population that report receiving positive hours of informal care.", size(vsmall))

* Save figure
graph export ///
"$dir_output_files/social_care/validation_${country}_care_hours_received_informal_ quantiles_ts_65plus_both.jpg", ///
	replace width(2400) height(1350) quality(100)

graph drop _all 	

********************************************************************************
* 1.6.1: Mean values over time - Quantile means of hours of informal care 
*	 		received, by gender
********************************************************************************



********************************************************************************
* 1.7: Mean values over time - Quantile means of hours of formal care received
********************************************************************************

* Prepare validation data 
use year idBu dwt valid_careHrsFormal demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Select sample 	
keep if demAge > 64 & demAge != .
keep if valid_careHrsFormal > 0 
 
* Compute mean 
collapse 	(p25) valid_p25 = valid_careHrsFormal ///
			(p50) valid_p50 = valid_careHrsFormal	///
			(p75) valid_p75 = valid_careHrsFormal [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data 
use run year idBu sim_careHrsFormal  demAge using ///
	"$dir_data/simulation_sample.dta", clear

* Select sample 	
keep if demAge > 64
keep if sim_careHrsFormal > 0
	
* Compute quantiles and sd 
bysort year run: egen sim_p25_run = pctile(sim_careHrsFormal), p(25)
bysort year run: egen sim_p50_run = pctile(sim_careHrsFormal), p(50) 
bysort year run: egen sim_p75_run = pctile(sim_careHrsFormal), p(75)

collapse (mean) sim_p25 = sim_p25_run ///
				sim_p50 = sim_p50_run ///
				sim_p75 = sim_p75_run ///
         (sd) 	sim_p25_sd = sim_p25 ///
				sim_p50_sd = sim_p50 ///
				sim_p75_sd = sim_p75, by(year)
		 
* Approx 95% confidence intervals
foreach p in 25 50 75 {
	
    gen sim_p`p'_lo = sim_p`p' - 1.96*sim_p`p'_sd
    gen sim_p`p'_hi = sim_p`p' + 1.96*sim_p`p'_sd

}

* Combine datasets 
merge 1:1 year using "$dir_data/temp_valid_stats.dta"

* Plot figure 
twoway (rarea sim_p25_lo sim_p25_hi year, ///
	sort color(green%20) legend(label(1 "SimPaths, p25"))) ///
(line valid_p25 year, sort color(green) ///
	legend(label(2 "UKHLS, p25"))) ///
(rarea sim_p50_lo sim_p50_hi year, ///
	sort color(blue%20) legend(label(3 "SimPaths, p50"))) ///
(line valid_p50 year, sort color(blue) ///
	legend(label(4 "UKHLS, p50"))) ///
(rarea sim_p75_lo sim_p75_hi year, ///
	sort color(red%20) legend(label(5 "SimPaths, p75"))) ///
(line valid_p75 year, sort color(red) ///
	legend(label(6 "UKHLS, p75"))), ///
	title("Hours of Formal Care Received ") ///
	subtitle("Ages 65+") ///
	xtitle("Year", size(small)) ///
	ytitle("Hours per week", size(small)) ///) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: Percentiles computed on the 65+ population that report receiving positive hours of formal care.", size(vsmall))

* Save figure
graph export ///
"$dir_output_files/social_care/validation_${country}_care_hours_received_formal_ quantiles_ts_65plus_both.jpg", ///
	replace width(2400) height(1350) quality(100)

graph drop _all	

********************************************************************************
* 1.7.1: Mean values over time - Quantile means of hours of formal care 
*	 		received, by gender
********************************************************************************



********************************************************************************
* 1.8: Mean values over time - Share provide care 
********************************************************************************

* Prepare validation data 
use year idBu dwt valid_careProvideFlag demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear
	
keep if demAge >= 16	
	
* Compute mean 
collapse (mean) valid_careProvideFlag [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data 
use run year idBu sim_careProvideFlag demAge using ///
	"$dir_data/simulation_sample.dta", clear
	
keep if demAge >= 16		
	
* Compute mean and sd 
collapse (mean) sim_careProvideFlag, by(year run)

collapse (mean) sim_careProvideFlag ///
		 (sd) sim_careProvideFlag_sd = sim_careProvideFlag, by(year)

* Compute 95% confidence intervals
gen sim_careProvideFlag_h = sim_careProvideFlag + 1.96*sim_careProvideFlag_sd
gen sim_careProvideFlag_l = sim_careProvideFlag - 1.96*sim_careProvideFlag_sd

* Combine datasets 
merge 1:1 year using "$dir_data/temp_valid_stats.dta"

* Plot figure 
make_care_plot, ///
	var("careProvideFlag") ///
	title("Provide Care") ///
	subtitle("Ages 16+") ///
	saving("validation_${country}_provide_care_ts_16plus_both") ///
	note(`""Notes: ""') 

graph drop _all 		


********************************************************************************
* 1.8.1: Mean values over time - Share provide care, by gender
********************************************************************************

* Prepare validation data 
use year idBu dwt valid_careProvideFlag demAge demMaleFlag using ///
	"$dir_data/ukhls_validation_sample.dta", clear

keep if demAge >= 16		
	
* Compute mean 
collapse (mean) valid_careProvideFlag [aw = dwt], by(year demMaleFlag)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data 
use run year idBu sim_careProvideFlag demAge demMaleFlag using ///
	"$dir_data/simulation_sample.dta", clear
	
keep if demAge >= 16		
	
* Compute mean and sd 
collapse (mean) sim_careProvideFlag, by(year demMaleFlag run)

collapse (mean) sim_careProvideFlag ///
		 (sd) sim_careProvideFlag_sd = sim_careProvideFlag, by(year demMaleFlag)

* Compute 95% confidence intervals
gen sim_careProvideFlag_h = sim_careProvideFlag + 1.96*sim_careProvideFlag_sd
gen sim_careProvideFlag_l = sim_careProvideFlag - 1.96*sim_careProvideFlag_sd

* Combine datasets 
merge 1:1 year demMaleFlag using "$dir_data/temp_valid_stats.dta"

* Plot figures

* Females
twoway (rarea sim_careProvideFlag_h sim_careProvideFlag_l year if ///
	demMaleFlag == 0, sort color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_careProvideFlag year if demMaleFlag == 0, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	subtitle("Females") ///
	name(care_female, replace) ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	ylabel(0[0.1]0.5,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	
* Males
twoway (rarea sim_careProvideFlag_h sim_careProvideFlag_l year if ///
	demMaleFlag == 1, sort color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_careProvideFlag year if demMaleFlag == 1, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	subtitle("Males") ///
	name(care_male, replace) ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	ylabel(0[0.1]0.5,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///	
	
* Combine	
grc1leg care_female care_male, ///
	title("Provide Care") ///
	subtitle("Age 16+") ///
	legendfrom(care_female) rows(1) ///
	ycomm ///
	graphregion(color(white)) ///
	note("Notes:  ", ///
	size(vsmall))	
	
* Save figure
graph export ///
"$dir_output_files/social_care/validation_${country}_provide_care_ts_16plus_gender.jpg", ///
	replace width(2400) height(1350) quality(100)
	
graph drop _all 


********************************************************************************
* 1.8.2: Mean values over time - Share provide care, by gender and  
* 			age group 
********************************************************************************

* Prepare validation data
use year dwt demMaleFlag ageGroup valid_careProvideFlag demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear
		
drop ageGroup
gen ageGroup = 1 if inrange(demAge,16,20)	
replace ageGroup = 2 if inrange(demAge,21,40)
replace ageGroup = 3 if inrange(demAge,41,50)
replace ageGroup = 4 if inrange(demAge,51,60)
replace ageGroup = 5 if inrange(demAge,61,70)
replace ageGroup = 6 if inrange(demAge,70,110)	

* Select sample 
	
* Exstensive margin dummy 		
replace valid_careProvideFlag = . if valid_careProvideFlag < 0 
	
gen care_m = valid_careProvideFlag if demMaleFlag == 1
gen care_f = valid_careProvideFlag if demMaleFlag == 0

* Compute means 
collapse (mean) care* [aw = dwt], by(ageGroup year)

* Restructure data 
drop if missing(ageGroup)
reshape wide care*, i(year) j(ageGroup)

forvalues i = 1(1)6 {

	rename care_f`i' care_f_`i'_valid
	rename care_m`i' care_m_`i'_valid

}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare Simulated data
use run year demMaleFlag ageGroup sim_careProvideFlag demAge using ///
	"$dir_data/simulation_sample.dta", clear

drop ageGroup
gen ageGroup = 1 if inrange(demAge,16,20)	
replace ageGroup = 2 if inrange(demAge,21,40)
replace ageGroup = 3 if inrange(demAge,41,50)
replace ageGroup = 4 if inrange(demAge,51,60)
replace ageGroup = 5 if inrange(demAge,61,70)
replace ageGroup = 6 if inrange(demAge,70,110)	
	
* Gender specific vars
gen care_m = sim_careProvideFlag if demMaleFlag == 1
gen care_f = sim_careProvideFlag if demMaleFlag == 0

drop sim_careProvideFlag

* Compute means 
collapse (mean) care*, by(ageGroup run year)

* Restructure data 
drop if missing(ageGroup)
reshape wide care*, i(year run) j(ageGroup)

* COmpute sd 
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

	
* Approx 95% confidence interval 
forvalues i=1(1)6 {
	
	gen care_f_`i'_sim_high = care_f`i' + 1.96*care_f_`i'_sd
	gen care_f_`i'_sim_low = care_f`i' - 1.96*care_f_`i'_sd
	gen care_m_`i'_sim_high = care_m`i' + 1.96*care_m_`i'_sd
	gen care_m_`i'_sim_low = care_m`i' - 1.96*care_m_`i'_sd	

}		 

* Combine datasets 
merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
foreach vble in "care_f" "care_m" {
	
	twoway (rarea `vble'_1_sim_high `vble'_1_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_1_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 16-20") ///
		name(`vble'_1, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Share", size(small)) ///
		ylabel(, labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))
		
	twoway (rarea `vble'_2_sim_high `vble'_2_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_2_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 21-40") ///
		name(`vble'_2, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Share", size(small)) ///
		ylabel(, labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))
		
	twoway (rarea `vble'_3_sim_high `vble'_3_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_3_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 41-50") ///
		name(`vble'_3, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Share", size(small)) ///
		ylabel(, labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))

	twoway (rarea `vble'_4_sim_high `vble'_4_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_4_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 51-60") ///
		name(`vble'_4, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Share", size(small)) ///
		ylabel(, labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))

	twoway (rarea `vble'_5_sim_high `vble'_5_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_5_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 61-70") ///
		name(`vble'_5, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Share", size(small)) ///
		ylabel(, labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))

	twoway (rarea `vble'_6_sim_high `vble'_6_sim_low year, sort ///
		color(red%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
	(line `vble'_6_valid year, sort color(red) legend(label(2 "UKHLS"))), ///
		subtitle("Age 70+") ///
		name(`vble'_6, replace) ///
		xtitle("Year", size(small)) ///
		ytitle("Share", size(small)) ///
		ylabel(, labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white))

}

* Save figures
grc1leg care_f_1 care_f_2 care_f_3 care_f_4 care_f_5 care_f_6 , ///
	title("Provide Care") ///
	subtitle("Females") ///
	legendfrom(care_f_1) ///
	ycomm ///
	graphregion(color(white)) ///
note("Notes:  ", ///
	size(vsmall)) 	
	
graph export ///
"$dir_output_files/social_care/validation_${country}_provide_care_ts_all_ages_female.jpg", ///
	replace width(2400) height(1350) quality(100)
		

grc1leg care_m_1 care_m_2 care_m_3 care_m_4 care_m_5 care_m_6, ///
	title("Provide Care") ///
	subtitle("Males") ///
	legendfrom(care_m_1) ///
	ycomm ///
	graphregion(color(white)) ///
note("Notes: ", ///
	size(vsmall)) 	
	
graph export ///
"$dir_output_files/social_care/validation_${country}_provide_care_ts_all_ages_male.jpg", ///
	replace width(2400) height(1350) quality(100)

graph drop _all 	


********************************************************************************
* 1.9: Mean values over time - Quantile means of hours of care provided
********************************************************************************

* Prepare validation data 
use year idBu dwt valid_careProvideFlag valid_careHrsProvidedWeek demAge ///
	using "$dir_data/ukhls_validation_sample.dta", clear
	
keep if demAge >= 16
keep if valid_careProvideFlag == 1	
 
* Compute mean 
collapse 	(p25) valid_p25 = valid_careHrsProvidedWeek ///
			(p50) valid_p50 = valid_careHrsProvidedWeek	///
			(p75) valid_p75 = valid_careHrsProvidedWeek [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data 
use run year idBu sim_careHrsProvidedWeek  demAge using ///
	"$dir_data/simulation_sample.dta", clear
	
* Compute quantiles and sd 
bysort year run: egen sim_p25_run = pctile(sim_careHrsProvidedWeek), p(25)
bysort year run: egen sim_p50_run = pctile(sim_careHrsProvidedWeek), p(50) 
bysort year run: egen sim_p75_run = pctile(sim_careHrsProvidedWeek), p(75)

collapse (mean) sim_p25 = sim_p25_run ///
				sim_p50 = sim_p50_run ///
				sim_p75 = sim_p75_run ///
         (sd) 	sim_p25_sd = sim_p25 ///
				sim_p50_sd = sim_p50 ///
				sim_p75_sd = sim_p75, by(year)

* Approx 95% confidence intervals
foreach p in 25 50 75 {
	
    gen sim_p`p'_lo = sim_p`p' - 1.96*sim_p`p'_sd
    gen sim_p`p'_hi = sim_p`p' + 1.96*sim_p`p'_sd

}

* Combine datasets 
merge 1:1 year using "$dir_data/temp_valid_stats.dta"

* Plot figure 
twoway (rarea sim_p25_lo sim_p25_hi year, ///
	sort color(green%20) legend(label(1 "SimPaths, p25"))) ///
(line valid_p25 year, sort color(green) ///
	legend(label(2 "UKHLS, p25"))) ///
(rarea sim_p50_lo sim_p50_hi year, ///
	sort color(blue%20) legend(label(3 "SimPaths, p50"))) ///
(line valid_p50 year, sort color(blue) ///
	legend(label(4 "UKHLS, p50"))) ///
(rarea sim_p75_lo sim_p75_hi year, ///
	sort color(red%20) legend(label(5 "SimPaths, p75"))) ///
(line valid_p75 year, sort color(red) ///
	legend(label(6 "UKHLS, p75"))), ///
	title("Hours of Care Provided per Week ") ///
	subtitle("All ages") ///
	xtitle("Year", size(small)) ///
	ytitle("Hours per week", size(small)) ///) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: Percentiles computed on those age 16+ and provide care.", size(vsmall))
	
* Save figure
graph export ///
"$dir_output_files/social_care/validation_${country}_care_hours_provided_quantiles_ts_16plus_both.jpg", ///
	replace width(2400) height(1350) quality(100)


graph drop _all 


********************************************************************************
* 1.9.1: Mean values over time - Quantile means of hours of care provided, by 
*			gender
********************************************************************************


********************************************************************************
* 1.10: Mean values over time - Amount spent on formal social care, those that
* 			receive formal care 
********************************************************************************

* Load validaiton data
use year idBu dwt valid_careFormalX valid_careHrsFormal demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Select sample 	
keep if demAge > 64 & demAge != . 
keep if valid_careHrsFormal > 0 & valid_careHrsFormal != . 
	
* Compute mean 
collapse (mean) valid_careFormalX [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data 
use run year idBu sim_careFormalX sim_careHrsFormal demAge using ///
	"$dir_data/simulation_sample.dta", clear

* Select sample 	
keep if demAge > 64
keep if sim_careHrsFormal > 0 & sim_careHrsFormal != . 
	
* Compute mean and sd 
collapse (mean) sim_careFormalX, by(year run)

collapse (mean) sim_careFormalX ///
		 (sd) sim_careFormalX_sd = sim_careFormalX, by(year)

* Compute 95% confidence intervals
gen sim_careFormalX_h = sim_careFormalX + 1.96*sim_careFormalX_sd
gen sim_careFormalX_l = sim_careFormalX - 1.96*sim_careFormalX_sd

* Combine datasets 
merge 1:1 year using "$dir_data/temp_valid_stats.dta"

* Plot
twoway (rarea sim_careFormalX_h sim_careFormalX_l year, ///
	sort color(green%20) legend(label(1 "SimPaths"))) ///
	(line valid_careFormalX year, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	title("Social Care Expenditure") ///
	subtitle("") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///  
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	name(`name', replace) ///
	graphregion(color(white)) ///
	note("Notes:", size(vsmall))  
    
    graph export "$dir_output_files/social_care/validation_${country}_formal_care_expenditure_both.jpg", ///
        replace width(2400) height(1350) quality(100)


graph drop _all 		
