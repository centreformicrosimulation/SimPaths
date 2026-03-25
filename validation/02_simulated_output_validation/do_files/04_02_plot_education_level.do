********************************************************************************
* SECTION:			Validation
* OBJECT: 			Education
* AUTHORS:			Ashley Burdett 
* LAST UPDATE:		Jan 2026
* COUNTRY: 			UK 
********************************************************************************
* NOTES: 			This do file plots simulated and UKHLS education. 
* 					Unable to look at transitions because use X-sectional 
* 					SILC data. 
********************************************************************************

********************************************************************************
* 0 : Programmes
********************************************************************************

* Time series plot, all 
cap program drop make_edu_plot

program define make_edu_plot
    syntax, subtitle(string) saving(string) note(string)
	
	twoway ///
	(rarea sim_edu_high_high sim_edu_high_low year, sort color(green%20) ///
		legend(label(1 "High education, SimPaths"))) ///
	(line valid_edu_high year, sort color(green) ///
		legend(label(2 "High education, UKHLS"))) ///
	(rarea sim_edu_med_high sim_edu_med_low year, sort color(blue%20) ///
		legend(label(3 "Medium education, SimPaths"))) ///
	(line valid_edu_med year, sort color(blue) ///
		legend(label(4 "Medium education, UKHLS"))) ///
	(rarea sim_edu_low_high sim_edu_low_low year, sort color(red%20) ///
		legend(label(5 "Low education, SimPaths"))) ///
	(line valid_edu_low year, sort color(red) ///
		legend(label(6 "Low education, UKHLS"))) ///
	(rarea sim_edu_na_high sim_edu_na_low year, sort color(purple%20) ///
		legend(label(7 "Initial education spell, SimPaths"))) ///
	(line valid_edu_na year, sort color(purple) ///
		legend(label(8 "Initial education spell, UKHLS"))), ///
		title("Educational Attainment") ///
		subtitle("`subtitle'") ///
		xtitle("Year", size(small)) ///
		ytitle("Share", size(small)) ///
		graphregion(color(white)) ///
		xlabel(, labsize(small)) ///
		ylabel(, labsize(small)) ///
		legend(size(small)) ///
		note(`note', size(vsmall))  
	
	graph export "$dir_output_files/education/`saving'.jpg", replace width(2400) height(1350) quality(100)
end		

********************************************************************************
* 1 : Mean values over time
********************************************************************************

********************************************************************************
* 1.1 : Educational attainment 
********************************************************************************

********************************************************************************
* 1.1.1 : Educational attainment - 16-65
********************************************************************************

* Prepare  validation data
use year dwt valid_edu_high valid_edu_med valid_edu_low valid_edu_na ///
	demAge labC4 using "$dir_data/ukhls_validation_sample.dta", clear

* Select sample
keep if inrange(demAge,16,65)
	
* Compute annual shares 	
collapse (mean) valid_edu_high valid_edu_med valid_edu_low valid_edu_na ///
	[aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_edu_high sim_edu_med sim_edu_low sim_edu_na labC4 ///
	demAge using "$dir_data/simulation_sample.dta", clear

* Select sample
keep if inrange(demAge,16,65)

* Compute shares and standard deviation	
collapse (mean) sim_edu_high sim_edu_med sim_edu_low sim_edu_na,  by(run year)

collapse (mean) sim_edu_high sim_edu_med sim_edu_low sim_edu_na ///
		 (sd) sim_edu_high_sd = sim_edu_high ///
		 sim_edu_med_sd = sim_edu_med ///
		 sim_edu_low_sd = sim_edu_low ///
		 sim_edu_na_sd = sim_edu_na ///
		 , by(year)
		 
* Approx 95% confidence interval 		 
foreach varname in sim_edu_high sim_edu_med sim_edu_low sim_edu_na {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd
	
}

* Combine datasets
merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
make_edu_plot, ///
	subtitle("Ages 16-65") ///
	saving("validation_${country}_education_ts_16_65_both") ///
	note(`""Notes:""')

	
********************************************************************************
* 1.1.2 : Educational attainment - 16-65, by gender
********************************************************************************

* Prepare  validation data
use year dwt valid_edu_high valid_edu_med valid_edu_low valid_edu_na ///
	demMaleFlag labC4 demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Select sample
keep if inrange(demAge,16,65)
	
* Compute annual shares	
collapse (mean) valid_edu_high valid_edu_med valid_edu_low valid_edu_na ///
	[aw = dwt], by(year demMaleFlag)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_edu_high sim_edu_med sim_edu_low sim_edu_na demMaleFlag ///
	labC4 demAge using "$dir_data/simulation_sample.dta", clear
	
* Select sample
keep if inrange(demAge,16,65)
		
* Compute shares and sd 
collapse (mean) sim_edu_high sim_edu_med sim_edu_low sim_edu_na, ///
	by(run year demMaleFlag)

collapse (mean) sim_edu_high sim_edu_med sim_edu_low sim_edu_na ///
		 (sd) sim_edu_high_sd = sim_edu_high ///
		 (sd) sim_edu_med_sd = sim_edu_med ///
		 (sd) sim_edu_low_sd = sim_edu_low ///	 
		 (sd) sim_edu_na_sd = sim_edu_na ///	 
		 , by(year demMaleFlag )
		 
* Approx 95% confidence interval 		 
foreach varname in sim_edu_high sim_edu_med sim_edu_low sim_edu_na  {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd
	
}

* Combine datasets
merge 1:1 year demMaleFlag using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Female 
preserve 

keep if demMaleFlag == 0

* Plot figure
make_edu_plot, ///
	subtitle("Ages 16-65, females") ///
	saving("validation_${country}_education_ts_16_65_female") ///
	note(`""Notes:""')

restore 	

* male 
preserve 

keep if demMaleFlag == 1

* Plot figure
make_edu_plot, ///
	subtitle("Ages 16-65, males") ///
	saving("validation_${country}_education_ts_16_65_male") ///
	note(`""Notes:""')

restore 	
	
	
********************************************************************************
* 1.1.3 : Educational attainment - 16-30
********************************************************************************

* Prepare  validation data
use year dwt valid_edu_high valid_edu_med valid_edu_low valid_edu_na ///
	demAge labC4 demAge using "$dir_data/ukhls_validation_sample.dta", clear
	
* Select sample 
drop if demAge > 30 	
drop if demAge < 16
drop labC4	
	
* Compute shares	
collapse (mean) valid_edu_high valid_edu_med valid_edu_low valid_edu_na ///
	[aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_edu_high sim_edu_med sim_edu_low sim_edu_na demAge labC4 ///
	using "$dir_data/simulation_sample.dta", clear

* Select sample 
drop if demAge > 30 	
drop if demAge < 16 
	
* Compute shares and sd	
collapse (mean) sim_edu_high sim_edu_med sim_edu_low sim_edu_na, by(run year)

collapse (mean) sim_edu_high sim_edu_med sim_edu_low sim_edu_na ///
		 (sd) sim_edu_high_sd = sim_edu_high ///
		 sim_edu_med_sd = sim_edu_med ///
		 sim_edu_low_sd = sim_edu_low ///
		 sim_edu_na_sd = sim_edu_na ///
		 , by(year)
		
* Approx 95% confidence interval 		
foreach varname in sim_edu_high sim_edu_med sim_edu_low sim_edu_na {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

* Combine datasets	
merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

make_edu_plot, ///
	subtitle("Ages 16-30") ///
	saving("validation_${country}_education_ts_16_30_both") ///
	note(`""Notes:""')

	
********************************************************************************
* 1.1.4 : Educational attainment - 16-30, by gender
********************************************************************************	
* Prepare  validation data
use year dwt valid_edu_high valid_edu_med valid_edu_low valid_edu_na demAge ///
	demMaleFlag labC4 demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear
	
* Select smaple 
drop if demAge > 30 
drop if demAge < 16 	
drop labC4

* Compute shares	
collapse (mean) valid_edu_high valid_edu_med valid_edu_low valid_edu_na ///
	[aw = dwt], by(year demMaleFlag)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_edu_high sim_edu_med sim_edu_low sim_edu_na demAge ///
	demMaleFlag labC4 using "$dir_data/simulation_sample.dta", clear

* Select sample 
drop if demAge > 30 
drop if demAge < 16 
	
* Cmpute shares and sd 	
collapse (mean) sim_edu_high sim_edu_med sim_edu_low sim_edu_na, ///
	by(run year demMaleFlag)

collapse (mean) sim_edu_high sim_edu_med sim_edu_low sim_edu_na ///
		 (sd) sim_edu_high_sd = sim_edu_high ///
		 (sd) sim_edu_med_sd = sim_edu_med ///
		 (sd) sim_edu_low_sd = sim_edu_low ///
		 (sd) sim_edu_na_sd = sim_edu_na ///
		 , by(year demMaleFlag)
		 
* Approx 95% confidence interval		 
foreach varname in sim_edu_high sim_edu_med sim_edu_low sim_edu_na {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd
	
}

* Combine datasets
merge 1:1 year demMaleFlag using "$dir_data/temp_valid_stats.dta", keep(3) nogen


* Female 
preserve 

keep if demMaleFlag == 0

* Plot figure
make_edu_plot, ///
	subtitle("Ages 16-30, females") ///
	saving("validation_${country}_education_ts_16_30_female") ///
	note(`""Notes:""')

restore 	

* male 
preserve 

keep if demMaleFlag == 1

* Plot figure
make_edu_plot, ///
	subtitle("Ages 16-30, males") ///
	saving("validation_${country}_education_ts_16_30_male") ///
	note(`""Notes:""')

restore 


********************************************************************************
* 1.1.5 : Educational attainment - 31-40
********************************************************************************

* Prepare  validation data
use year dwt valid_edu_high valid_edu_med valid_edu_low valid_edu_na ///
	demAge labC4 demAge using "$dir_data/ukhls_validation_sample.dta", clear
	
* Select sample 
drop if demAge > 40 	
drop if demAge < 31
drop labC4	
	
* Compute shares	
collapse (mean) valid_edu_high valid_edu_med valid_edu_low valid_edu_na ///
	[aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_edu_high sim_edu_med sim_edu_low sim_edu_na demAge labC4 ///
	using "$dir_data/simulation_sample.dta", clear

* Select sample 
drop if demAge > 40 	
drop if demAge < 31
	
* Compute shares and sd	
collapse (mean) sim_edu_high sim_edu_med sim_edu_low sim_edu_na, by(run year)

collapse (mean) sim_edu_high sim_edu_med sim_edu_low sim_edu_na ///
		 (sd) sim_edu_high_sd = sim_edu_high ///
		 sim_edu_med_sd = sim_edu_med ///
		 sim_edu_low_sd = sim_edu_low ///
		 sim_edu_na_sd = sim_edu_na ///
		 , by(year)
		
* Approx 95% confidence interval 		
foreach varname in sim_edu_high sim_edu_med sim_edu_low sim_edu_na {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

* Combine datasets	
merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

make_edu_plot, ///
	subtitle("Ages 31-40") ///
	saving("validation_${country}_education_ts_31_40_both") ///
	note(`""Notes:""')

	
********************************************************************************
* 1.1.6  : Educational attainment 31-40, by gender
********************************************************************************	
* Prepare  validation data
use year dwt valid_edu_high valid_edu_med valid_edu_low valid_edu_na demAge ///
	demMaleFlag labC4 demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear
	
* Select smaple 
drop if demAge > 40 	
drop if demAge < 31	
drop labC4

* Compute shares	
collapse (mean) valid_edu_high valid_edu_med valid_edu_low valid_edu_na ///
	[aw = dwt], by(year demMaleFlag)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_edu_high sim_edu_med sim_edu_low sim_edu_na demAge ///
	demMaleFlag labC4 using "$dir_data/simulation_sample.dta", clear

* Select sample 
drop if demAge > 40 	
drop if demAge < 31
	
* Cmpute shares and sd 	
collapse (mean) sim_edu_high sim_edu_med sim_edu_low sim_edu_na, ///
	by(run year demMaleFlag)

collapse (mean) sim_edu_high sim_edu_med sim_edu_low sim_edu_na ///
		 (sd) sim_edu_high_sd = sim_edu_high ///
		 (sd) sim_edu_med_sd = sim_edu_med ///
		 (sd) sim_edu_low_sd = sim_edu_low ///
		 (sd) sim_edu_na_sd = sim_edu_na ///
		 , by(year demMaleFlag)
		 
* Approx 95% confidence interval		 
foreach varname in sim_edu_high sim_edu_med sim_edu_low sim_edu_na {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd
	
}

* Combine datasets
merge 1:1 year demMaleFlag using "$dir_data/temp_valid_stats.dta", keep(3) nogen


* Female 
preserve 

keep if demMaleFlag == 0

* Plot figure
make_edu_plot, ///
	subtitle("Ages 31-40, females") ///
	saving("validation_${country}_education_ts_31_40_female") ///
	note(`""Notes:""')

restore 	

* male 
preserve 

keep if demMaleFlag == 1

* Plot figure
make_edu_plot, ///
	subtitle("Ages 31-40, males") ///
	saving("validation_${country}_education_ts_31_40_male") ///
	note(`""Notes:""')

restore 
	
	
********************************************************************************
* 1.1.7 : Educational attainment - 41-65
********************************************************************************

* Prepare  validation data
use year dwt valid_edu_high valid_edu_med valid_edu_low valid_edu_na ///
	demAge labC4 demAge using "$dir_data/ukhls_validation_sample.dta", clear
	
* Select sample 
drop if demAge > 65 	
drop if demAge < 41
drop labC4	
	
* Compute shares	
collapse (mean) valid_edu_high valid_edu_med valid_edu_low valid_edu_na ///
	[aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_edu_high sim_edu_med sim_edu_low sim_edu_na demAge labC4 ///
	using "$dir_data/simulation_sample.dta", clear

* Select sample 
drop if demAge > 65 	
drop if demAge < 41
	
* Compute shares and sd	
collapse (mean) sim_edu_high sim_edu_med sim_edu_low sim_edu_na, by(run year)

collapse (mean) sim_edu_high sim_edu_med sim_edu_low sim_edu_na ///
		 (sd) sim_edu_high_sd = sim_edu_high ///
		 sim_edu_med_sd = sim_edu_med ///
		 sim_edu_low_sd = sim_edu_low ///
		 sim_edu_na_sd = sim_edu_na ///
		 , by(year)
		
* Approx 95% confidence interval 		
foreach varname in sim_edu_high sim_edu_med sim_edu_low sim_edu_na {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

* Combine datasets	
merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

make_edu_plot, ///
	subtitle("Ages 41-65") ///
	saving("validation_${country}_education_ts_41_65_both") ///
	note(`""Notes:""')

	
********************************************************************************
* 1.1.8  : Educational attainment 41-65, by gender
********************************************************************************	
* Prepare  validation data
use year dwt valid_edu_high valid_edu_med valid_edu_low valid_edu_na demAge ///
	demMaleFlag labC4 demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear
	
* Select smaple 
drop if demAge > 65 	
drop if demAge < 41
drop labC4

* Compute shares	
collapse (mean) valid_edu_high valid_edu_med valid_edu_low valid_edu_na ///
	[aw = dwt], by(year demMaleFlag)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_edu_high sim_edu_med sim_edu_low sim_edu_na demAge ///
	demMaleFlag labC4 using "$dir_data/simulation_sample.dta", clear

* Select sample 
drop if demAge > 65 	
drop if demAge < 41
	
* Cmpute shares and sd 	
collapse (mean) sim_edu_high sim_edu_med sim_edu_low sim_edu_na, ///
	by(run year demMaleFlag)

collapse (mean) sim_edu_high sim_edu_med sim_edu_low sim_edu_na ///
		 (sd) sim_edu_high_sd = sim_edu_high ///
		 (sd) sim_edu_med_sd = sim_edu_med ///
		 (sd) sim_edu_low_sd = sim_edu_low ///
		 (sd) sim_edu_na_sd = sim_edu_na ///
		 , by(year demMaleFlag)
		 
* Approx 95% confidence interval		 
foreach varname in sim_edu_high sim_edu_med sim_edu_low sim_edu_na {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd
	
}

* Combine datasets
merge 1:1 year demMaleFlag using "$dir_data/temp_valid_stats.dta", keep(3) nogen


* Female 
preserve 

keep if demMaleFlag == 0

* Plot figure
make_edu_plot, ///
	subtitle("Ages 41-65, females") ///
	saving("validation_${country}_education_ts_41_65_female") ///
	note(`""Notes:""')

restore 	

* Male 
preserve 

keep if demMaleFlag == 1

* Plot figure
make_edu_plot, ///
	subtitle("Ages 41-65, males") ///
	saving("validation_${country}_education_ts_41_65_male") ///
	note(`""Notes:""')

restore 	


********************************************************************************
* 1.1.9 : Educational attainment - 66-70
********************************************************************************

* Prepare  validation data
use year dwt valid_edu_high valid_edu_med valid_edu_low valid_edu_na ///
	demAge labC4 demAge using "$dir_data/ukhls_validation_sample.dta", clear
	
* Select sample 
drop if demAge > 70 	
drop if demAge < 66
drop labC4	
	
* Compute shares	
collapse (mean) valid_edu_high valid_edu_med valid_edu_low valid_edu_na ///
	[aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_edu_high sim_edu_med sim_edu_low sim_edu_na demAge labC4 ///
	using "$dir_data/simulation_sample.dta", clear

* Select sample 
drop if demAge > 70 	
drop if demAge < 66
	
* Compute shares and sd	
collapse (mean) sim_edu_high sim_edu_med sim_edu_low sim_edu_na, by(run year)

collapse (mean) sim_edu_high sim_edu_med sim_edu_low sim_edu_na ///
		 (sd) sim_edu_high_sd = sim_edu_high ///
		 sim_edu_med_sd = sim_edu_med ///
		 sim_edu_low_sd = sim_edu_low ///
		 sim_edu_na_sd = sim_edu_na ///
		 , by(year)
		
* Approx 95% confidence interval 		
foreach varname in sim_edu_high sim_edu_med sim_edu_low sim_edu_na {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

* Combine datasets	
merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

make_edu_plot, ///
	subtitle("Ages 66-70") ///
	saving("validation_${country}_education_ts_66_70_both") ///
	note(`""Notes:""')
	
	
********************************************************************************
* 1.2 : Educational attainment when leave education
********************************************************************************	

********************************************************************************
* 1.2.1 : Educational attainment when leave education - 16 - 65
********************************************************************************	

* Prepare  validation data
use year idPers dwt valid_edu_high valid_edu_med valid_edu_low labC4 ///
	demAge using "$dir_data/ukhls_validation_sample.dta", clear

* Select sample
keep if inrange(demAge,16,65)

* Select relevant observations 
sort idPers year 
gen left_edu = 1 if idPers == idPers[_n-1] & ///
	labC4 != 2 & labC4[_n-1] == 2 & year == year[_n-1]+1 
	
keep if left_edu == 1
	
collapse (mean) valid_edu_high valid_edu_med valid_edu_low [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year idPers sim_edu_high sim_edu_med sim_edu_low labC4 demAge ///
	using "$dir_data/simulation_sample.dta", clear

* Select sample
keep if inrange(demAge,16,65)

* Select relevant observations 
sort idPers year 
gen left_edu_sim = 1 if idPers == idPers[_n-1] & ///
	labC4 != "Student" & labC4[_n-1] == "Student" & year == year[_n-1]+1 

keep if left_edu_sim == 1 	
	
collapse (mean) sim_edu_high sim_edu_med sim_edu_low, by(run year)

collapse (mean) sim_edu_high sim_edu_med sim_edu_low ///
		 (sd) sim_edu_high_sd = sim_edu_high ///
		 sim_edu_med_sd = sim_edu_med ///
		 sim_edu_low_sd = sim_edu_low ///
		 , by(year)
		 
foreach varname in sim_edu_high sim_edu_med sim_edu_low {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd
	
}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway ///
(rarea sim_edu_high_high sim_edu_high_low year, sort color(green%20) ///
	legend(label(1 "High education, simulated"))) ///
(line valid_edu_high year, sort color(green) ///
	legend(label(2 "High education, UKHLS"))) ///
(rarea sim_edu_med_high sim_edu_med_low year, sort color(blue%20) ///
	legend(label(3 "Medium education, simulated"))) ///
(line valid_edu_med year, sort color(blue) ///
	legend(label(4 "Medium education, UKHLS"))) ///
(rarea sim_edu_low_high sim_edu_low_low year, sort color(red%20) ///
	legend(label(5 "Low education, simulated"))) ///
(line valid_edu_low year, sort color(red) ///
	legend(label(6 "Low education, UKHLS"))), ///
	title("Educational Attainment When Leave Education") ///
	subtitle("Ages 16-65") /// 
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: ", ///
	size(vsmall))
	
graph export ///
"$dir_output_files/education/validation_${country}_leave_education_ts_16_65_both.jpg", ///
	replace width(2400) height(1350) quality(100)			
	
graph drop _all 
