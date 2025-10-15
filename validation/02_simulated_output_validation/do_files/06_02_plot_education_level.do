********************************************************************************
* SECTION:			Validation
* OBJECT: 			Education
* AUTHORS:			Ashley Burdett 
* LAST UPDATE:		9/25
* COUNTRY: 			UK 

* NOTES: 			This do file plots simulated and UKHLS education. 
* 					Unable to look at transitions because use X-sectional 
* 					SILC data. 
********************************************************************************

********************************************************************************
* 1 : Mean values over time
********************************************************************************

********************************************************************************
* 1.1 : Educational attainment 
********************************************************************************

********************************************************************************
* 1.1.1 : Educational attainment - 17-65
********************************************************************************

* Prepare  validation data
use year dwt valid_edu_high valid_edu_med valid_edu_low les_c4 dag using ///
	"$dir_data/ukhls_validation_full_sample.dta", clear

* Select sample
drop if les_c4 == 2 	//  | les_c4 == -9? 
drop les_c4	
keep if inrange(dag,17,65)
	
* Compute annual shares 	
collapse (mean) valid_edu_high valid_edu_med valid_edu_low [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_edu_high sim_edu_med sim_edu_low les_c4 dag using ///
	"$dir_data/simulated_data_full.dta", clear

* Select sample
drop if les_c4 == "Student"
drop les_c4	
keep if inrange(dag,17,65)

* Compute shares and standard deviation	
collapse (mean) sim_edu_high sim_edu_med sim_edu_low, by(run year)
collapse (mean) sim_edu_high sim_edu_med sim_edu_low ///
		 (sd) sim_edu_high_sd = sim_edu_high ///
		 sim_edu_med_sd = sim_edu_med ///
		 sim_edu_low_sd = sim_edu_low ///
		 , by(year)
		 
* Approx 95% confidence interval 		 
foreach varname in sim_edu_high sim_edu_med sim_edu_low {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd
	
}

* Combine datasets
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
	title("Educational Attainment") ///
	subtitle("Ages 17-${max_age}") /// 
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Current students excluded from sample", ///
	size(vsmall))
	
graph export ///
"$dir_output_files/education/validation_${country}_education_ts_17_${max_age}_both.jpg", ///
	replace width(2400) height(1350) quality(100)

	
********************************************************************************
* 1.1.2 : Educational attainment - 17-65, by gender
********************************************************************************

* Prepare  validation data
use year dwt valid_edu_high valid_edu_med valid_edu_low dgn les_c4 dag using ///
	"$dir_data/ukhls_validation_full_sample.dta", clear

* Select sample
drop if les_c4 == 2 
drop les_c4	
keep if inrange(dag,17,65)
	
* Compute annual shares	
collapse (mean) valid_edu_high valid_edu_med valid_edu_low [aw = dwt], ///
	by(year dgn)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_edu_high sim_edu_med sim_edu_low dgn les_c4 dag using ///
	"$dir_data/simulated_data_full.dta", clear
	
* Select sample
drop if les_c4 == "Student"	
keep if inrange(dag,17,65)
	
gen dgn2 = 0 if dgn == "Female"
replace dgn2 = 1 if dgn == "Male"

drop dgn
rename dgn2 dgn 		

* Compute shares and sd 
collapse (mean) sim_edu_high sim_edu_med sim_edu_low, by(run year dgn)
collapse (mean) sim_edu_high sim_edu_med sim_edu_low ///
		 (sd) sim_edu_high_sd = sim_edu_high ///
		 (sd) sim_edu_med_sd = sim_edu_med ///
		 (sd) sim_edu_low_sd = sim_edu_low ///	 
		 , by(year dgn )
		 
* Approx 95% confidence interval 		 
foreach varname in sim_edu_high sim_edu_med sim_edu_low {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd
	
}

* Combine datasets
merge 1:1 year dgn using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure - Female
twoway ///
(rarea sim_edu_high_high sim_edu_high_low year if dgn == 0, ///
	sort color(green%20) legend(label(1 "High education, simulated"))) ///
(line valid_edu_high year if dgn == 0, sort color(green) ///
	legend(label(2 "High education, UKHLS"))) ///
(rarea sim_edu_med_high sim_edu_med_low year if dgn == 0, ///
	sort color(blue%20) legend(label(3 "Medium education, simulated"))) ///
(line valid_edu_med year if dgn == 0, sort color(blue) ///
	legend(label(4 "Medium education, UKHLS"))) ///
(rarea sim_edu_low_high sim_edu_low_low year if dgn == 0, sort color(red%20) ///
	legend(label(5 "Low education, simulated"))) ///
(line valid_edu_low year if dgn == 0, sort color(red) ///
	legend(label(6 "Low education, UKHLS"))), ///
	title("Educational Attainment") ///
	subtitle("Ages 17-${max_age}, females") /// 
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Current students excluded from sample", ///
	size(vsmall))
	
graph export ///
"$dir_output_files/education/validation_${country}_education_ts_17_${max_age}_female.jpg", ///
	replace width(2400) height(1350) quality(100)
	
* Plot figure - Male	
twoway ///
(rarea sim_edu_high_high sim_edu_high_low year if dgn == 1, ///
	sort color(green%20) legend(label(1 "High education, simulated"))) ///
(line valid_edu_high year if dgn == 1, sort color(green) ///
	legend(label(2 "High education, UKHLS"))) ///
(rarea sim_edu_med_high sim_edu_med_low year if dgn == 1, ///
	sort color(blue%20) legend(label(3 "Medium education, simulated"))) ///
(line valid_edu_med year if dgn == 1, sort color(blue) ///
	legend(label(4 "Medium education, UKHLS"))) ///
(rarea sim_edu_low_high sim_edu_low_low year if dgn == 1, sort color(red%20) ///
	legend(label(5 "Low education, simulated"))) ///
(line valid_edu_low year if dgn == 1, sort color(red) ///
	legend(label(6 "Low education, UKHLS"))), ///
	title("Educational Attainment") ///
	subtitle("Ages 17-${max_age}, males") /// 
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Current students excluded from sample", ///
	size(vsmall))
	
graph export ///
"$dir_output_files/education/validation_${country}_education_ts_17_${max_age}_male.jpg", ///
	replace width(2400) height(1350) quality(100)	
	
	
********************************************************************************
* 1.1.3 : Educational attainment - 17-30
********************************************************************************

* Prepare  validation data
use year dwt valid_edu_high valid_edu_med valid_edu_low dag les_c4 dag using ///
	"$dir_data/ukhls_validation_full_sample.dta", clear
	
* Select sample 
drop if les_c4 == 2	
drop if dag > 30 	
drop if dag < 17
drop les_c4	
	
* Compute shares	
collapse (mean) valid_edu_high valid_edu_med valid_edu_low [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_edu_high sim_edu_med sim_edu_low dag les_c4 using ///
	"$dir_data/simulated_data_full.dta", clear

* Select sample 
drop if les_c4 == "Student"	
drop if dag > 30 	
drop if dag < 17 
	
* Compute shares and sd	
collapse (mean) sim_edu_high sim_edu_med sim_edu_low, by(run year)
collapse (mean) sim_edu_high sim_edu_med sim_edu_low ///
		 (sd) sim_edu_high_sd = sim_edu_high ///
		 sim_edu_med_sd = sim_edu_med ///
		 sim_edu_low_sd = sim_edu_low ///
		 , by(year)
		
* Approx 95% confidence interval 		
foreach varname in sim_edu_high sim_edu_med sim_edu_low {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

	}

* Combine datasets	
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
	title("Educational Attainment") ///
	subtitle("Ages 17-30") /// 
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Current students excluded from sample.", ///
	size(vsmall))
	
graph export ///
"$dir_output_files/education/validation_${country}_education_ts_17_30_both.jpg", ///
	replace width(2400) height(1350) quality(100)

	
********************************************************************************
* 1.1.4 : Educational attainment - 17-30, by gender
********************************************************************************	
* Prepare  validation data
use year dwt valid_edu_high valid_edu_med valid_edu_low dag dgn les_c4 dag ///
	using "$dir_data/ukhls_validation_full_sample.dta", clear
	
* Select smaple 
drop if les_c4 == 2	
drop if dag > 30 
drop if dag < 17 	
drop les_c4

* Compute shares	
collapse (mean) valid_edu_high valid_edu_med valid_edu_low [aw = dwt], ///
	by(year dgn)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_edu_high sim_edu_med sim_edu_low dag dgn les_c4 using ///
	"$dir_data/simulated_data_full.dta", clear

* Select sample 
drop if les_c4 == "Student"	
drop if dag > 30 
drop if dag < 17 

gen dgn2 = 0 if dgn == "Female"
replace dgn2 = 1 if dgn == "Male"

drop dgn les_c4
rename dgn2 dgn 	
	
* Cmpute shares and sd 	
collapse (mean) sim_edu_high sim_edu_med sim_edu_low, by(run year dgn)
collapse (mean) sim_edu_high sim_edu_med sim_edu_low ///
		 (sd) sim_edu_high_sd = sim_edu_high ///
		 (sd) sim_edu_med_sd = sim_edu_med ///
		 (sd) sim_edu_low_sd = sim_edu_low ///
		 , by(year dgn)
		 
* Approx 95% confidence interval		 
foreach varname in sim_edu_high sim_edu_med sim_edu_low {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd
	
}

* Combine datasets
merge 1:1 year dgn using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure - female
twoway ///
(rarea sim_edu_high_high sim_edu_high_low year if dgn == 0, ///
	sort color(green%20) legend(label(1 "High education, simulated"))) ///
(line valid_edu_high year  if dgn == 0, sort color(green) ///
	legend(label(2 "High education, UKHLS"))) ///
(rarea sim_edu_med_high sim_edu_med_low year if dgn == 0, ///
	sort color(blue%20) legend(label(3 "Medium education, simulated"))) ///
(line valid_edu_med year if dgn == 0, sort color(blue) ///
	legend(label(4 "Medium education, UKHLS"))) ///
(rarea sim_edu_low_high sim_edu_low_low year if dgn == 0, sort color(red%20) ///
	legend(label(5 "Low education, simulated"))) ///
(line valid_edu_low year if dgn == 0, sort color(red) ///
	legend(label(6 "Low education, UKHLS"))), ///
	title("Educational Attainment") ///
	subtitle("Ages 17-30, females") /// 
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Current students excluded from sample", ///
	size(vsmall))
	
graph export ///
"$dir_output_files/education/validation_${country}_education_ts_17_30_female.jpg", ///
	replace width(2400) height(1350) quality(100)	
	
* Plot figure - male 
twoway ///
(rarea sim_edu_high_high sim_edu_high_low year if dgn == 1, ///
	sort color(green%20) legend(label(1 "High education, simulated"))) ///
(line valid_edu_high year  if dgn == 1, sort color(green) ///
	legend(label(2 "High education, UKHLS"))) ///
(rarea sim_edu_med_high sim_edu_med_low year if dgn == 1, ///
	sort color(blue%20) legend(label(3 "Medium education, simulated"))) ///
(line valid_edu_med year if dgn == 1, sort color(blue) ///
	legend(label(4 "Medium education, UKHLS"))) ///
(rarea sim_edu_low_high sim_edu_low_low year if dgn == 1, sort color(red%20) ///
	legend(label(5 "Low education, simulated"))) ///
(line valid_edu_low year if dgn == 1, sort color(red) ///
	legend(label(6 "Low education, UKHLS"))), ///
	title("Educational Attainment") ///
	subtitle("Ages 17-30, females") /// 
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Current students excluded from sample.", ///
	size(vsmall))
	
graph export ///
"$dir_output_files/education/validation_${country}_education_ts_17_30_male.jpg", ///
	replace width(2400) height(1350) quality(100)		
	
	
********************************************************************************
* 1.1.5 : Educational attainment - 66-70
********************************************************************************

* Prepare  validation data
use year dwt valid_edu_high valid_edu_med valid_edu_low dag les_c4 using ///
	"$dir_data/ukhls_validation_full_sample.dta", clear

* Select sample 
drop if les_c4 == 2 	
drop if dag < 66  
drop if dag > 70 	
	
drop les_c4	
	
* Compute shares 	
collapse (mean) valid_edu_high valid_edu_med valid_edu_low [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_edu_high sim_edu_med sim_edu_low dag les_c4 using ///
	"$dir_data/simulated_data_full.dta", clear

* Select sample
drop if les_c4 == "Student"
drop if dag < 66  
drop if dag > 70 
	
drop les_c4 	
	
* Compute shares and sd 	
collapse (mean) sim_edu_high sim_edu_med sim_edu_low, by(run year)
collapse (mean) sim_edu_high sim_edu_med sim_edu_low ///
		 (sd) sim_edu_high_sd = sim_edu_high ///
		 sim_edu_med_sd = sim_edu_med ///
		 sim_edu_low_sd = sim_edu_low ///
		 , by(year)

* Approx 95% confidence interval 
foreach varname in sim_edu_high sim_edu_med sim_edu_low {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd
	
}

* Combine datasets
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
	title("Educational Attainment") ///
	subtitle("Ages 66-70") /// 
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note(Notes:, size(vsmall))
	
graph export ///
"$dir_output_files/education/validation_${country}_education_ts_66_70_both.jpg", ///
	replace width(2400) height(1350) quality(100)	
	
	
** 60-70 

* Prepare  validation data
use year dwt valid_edu_high valid_edu_med valid_edu_low dag les_c4 using ///
	"$dir_data/ukhls_validation_full_sample.dta", clear
	
* Selec sample	
drop if les_c4 == 2
drop if dag < 60  
drop if dag > 70 	
drop if valid_edu_high == 0 & valid_edu_med == 0 & valid_edu_low == 0 

drop les_c4
		
collapse (mean) valid_edu_high valid_edu_med valid_edu_low [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_edu_high sim_edu_med sim_edu_low dag les_c4 using ///
	"$dir_data/simulated_data_full.dta", clear

* Select sample 
drop if les_c4 == "Student"	
drop if dag < 60  
drop if dag > 70 
	
drop les_c4	
	
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
	title("Educational Attainment") ///
	subtitle("Ages 60-70") /// 
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note(Notes:, size(vsmall))
					
	
********************************************************************************
* 1.2 : Educational attainment when leave education
********************************************************************************	

********************************************************************************
* 1.2.1 : Educational attainment when leave education - 17-65
********************************************************************************	

* Prepare  validation data
use year idperson dwt valid_edu_high valid_edu_med valid_edu_low deh_c3 ///
	les_c4 dag using "$dir_data/ukhls_validation_full_sample.dta", clear

* Select sample
keep if inrange(dag,16,65)

* Select relevant observations 
sort idperson year 
gen left_edu = 1 if idperson == idperson[_n-1] & ///
	les_c4 != 2 & les_c4[_n-1] == 2 & year == year[_n-1]+1 
	
* Get rid of observations with missing values 	
drop if deh_c3 == -9 | deh_c3 == . 

* Get rid of observations with missing values 	
drop if valid_edu_high == 0 & valid_edu_med == 0 & valid_edu_low == 0 

keep if left_edu == 1
	
collapse (mean) valid_edu_high valid_edu_med valid_edu_low [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year idperson sim_edu_high sim_edu_med sim_edu_low les_c4 dag ///
	deh_c3 using "$dir_data/simulated_data.dta", clear

* Select sample
keep if inrange(dag,16,65)

* Select relevant observations 
sort idperson year 
gen left_edu_sim = 1 if idperson == idperson[_n-1] & ///
	les_c4 != "Student" & les_c4[_n-1] == "Student" & year == year[_n-1]+1 

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
	subtitle("Ages 17-${max_age}") /// 
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: ", ///
	size(vsmall))
	
graph export ///
"$dir_output_files/education/validation_${country}_leave_education_ts_17_65_both.jpg", ///
	replace width(2400) height(1350) quality(100)			
	
graph drop _all 	
