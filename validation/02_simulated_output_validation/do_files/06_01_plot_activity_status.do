/*******************************************************************************
* PROJECT:  		ESPON 
* SECTION:			Validation
* OBJECT: 			Economic Activity Status plots
* AUTHORS:			Ashley Burdett 
* LAST UPDATE:		9/25
* COUNTRY: 			UK 

* DESCRIPTION: 		This do file plots validation graphs for economics activity 
* 					status (4 cat). 
* 
* NOTES: 			
*******************************************************************************/

********************************************************************************
* 1 : Mean values over time
********************************************************************************
********************************************************************************
* 1.1 : Mean values over time - Economic Activity Status  
********************************************************************************
********************************************************************************
* 1.1.1 : Young people (17-30)
********************************************************************************

* Prepare validation data
use year dwt valid_employed valid_student valid_inactive dag  ///
	valid_retired using "$dir_data/ukhls_validation_full_sample.dta", ///
	clear
	
drop if dag > 30 
drop if dag < 17 	
	
collapse (mean) valid_employed valid_student valid_inactive valid_retired ///
	[aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_employed sim_student sim_inactive sim_retired dag using ///
	"$dir_data/simulated_data_full.dta", clear

drop if dag > 30 
drop if dag < 17  
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired, ///
	by(run year)
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired ///
		 (sd) sim_employed_sd = sim_employed ///
		 sim_student_sd = sim_student ///
		 sim_inactive_sd = sim_inactive ///
		 sim_retired_sd = sim_retired ///
		 , by(year)
		 
* Compute 95% confidence interval 		 
foreach varname in sim_employed sim_student sim_inactive sim_retired {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen


collapse (mean) sim* valid*, by(year)

* Plot figure 
twoway ///
(rarea sim_employed_high sim_employed_low year, sort color(green%20) ///
	legend(label(1 "Employed, simulated"))) ///
(line valid_employed year, sort color(green) ///
	legend(label(2 "Employed, UKHLS"))) ///
(rarea sim_student_high sim_student_low year, sort color(blue%20) ///
	legend(label(3 "Students, simulated"))) ///
(line valid_student year, sort color(blue) ///
	legend(label(4 "Students, UKHLS"))) ///
(rarea sim_inactive_high sim_inactive_low year, sort color(red%20) ///
	legend(label(5 "Non-employed, simulated"))) ///
(line valid_inactive year, sort color(red) ///
	legend(label(6 "Non-employed, UKHLS"))) ///
(rarea sim_retired_high sim_retired_low year, sort color(grey%20) ///
	legend(label(7 "Retired, simulated"))) ///
(line valid_retired year, sort color(grey) ///
	legend(label(8 "Retired, UKHLS"))), ///
	title("Economic Activity Status") ///
	subtitle("Ages 17-30") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. ", ///
	size(vsmall))

* Save figure
graph export ///
	"$dir_output_files/economic_activity/validation_${country}_activity_status_ts_17_30_both.jpg", ///
	replace width(2400) height(1350) quality(100)

	
** By gender	

** Male 
* Prepare validation data
use year dwt valid_employed valid_student valid_inactive dag dgn  ///
	valid_retired using "$dir_data/ukhls_validation_full_sample.dta", ///
	clear
	
drop if dag > 30 
drop if dag < 17
drop if dgn == 0	
	
collapse (mean) valid_employed valid_student valid_inactive valid_retired ///
	[aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_employed sim_student sim_inactive sim_retired dag dgn using ///
	"$dir_data/simulated_data_full.dta", clear

drop if dag > 30 
drop if dag < 17 
drop if dgn == "Female"
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired, ///
	by(run year)
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired ///
		 (sd) sim_employed_sd = sim_employed ///
		 sim_student_sd = sim_student ///
		 sim_inactive_sd = sim_inactive ///
		 sim_retired_sd = sim_retired ///
		 , by(year)

* Compute 95% confidence interval		 
foreach varname in sim_employed sim_student sim_inactive sim_retired {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen


collapse (mean) sim* valid*, by(year)

* Plot figure 
twoway ///
(rarea sim_employed_high sim_employed_low year, sort color(green%20) ///
	legend(label(1 "Employed, simulated"))) ///
(line valid_employed year, sort color(green) ///
	legend(label(2 "Employed, UKHLS"))) ///
(rarea sim_student_high sim_student_low year, sort color(blue%20) ///
	legend(label(3 "Students, simulated"))) ///
(line valid_student year, sort color(blue) ///
	legend(label(4 "Students, UKHLS"))) ///
(rarea sim_inactive_high sim_inactive_low year, sort color(red%20) ///
	legend(label(5 "Non-employed, simulated"))) ///
(line valid_inactive year, sort color(red) ///
	legend(label(6 "Non-employed, UKHLS"))) ///
(rarea sim_retired_high sim_retired_low year, sort color(grey%20) ///
	legend(label(7 "Retired, simulated"))) ///
(line valid_retired year, sort color(grey) ///
	legend(label(8 "Retired, UKHLS"))), ///
	title("Economic Activity Status") ///
	subtitle("Ages 17-30, males ") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. ", ///
	size(vsmall))

* Save figure
graph export ///
	"$dir_output_files/economic_activity/validation_${country}_activity_status_ts_17_30_male.jpg", ///
	replace width(2400) height(1350) quality(100)

	
** Female 

* Prepare validation data
use year dwt valid_employed valid_student valid_inactive dag dgn  ///
	valid_retired using "$dir_data/ukhls_validation_sample.dta", ///
	clear
	
drop if dag > 30 
drop if dag < 17
drop if dgn == 1	
	
collapse (mean) valid_employed valid_student valid_inactive valid_retired  ///
	dgn [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_employed sim_student sim_inactive sim_retired dag dgn using ///
	"$dir_data/simulated_data.dta", clear

drop if dag > 30 
drop if dag < 17
drop if dgn == "Male"	
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired, ///
	by(run year)
collapse (mean) sim_employed sim_student sim_inactive sim_retired ///
		 (sd) sim_employed_sd = sim_employed ///
		 sim_student_sd = sim_student ///
		 sim_inactive_sd = sim_inactive ///
		 sim_retired_sd = sim_retired ///
		 , by(year)
		
* Compute 95% confidence interval 		
foreach varname in sim_employed sim_student sim_inactive sim_retired {

	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen


collapse (mean) sim* valid*, by(year)

* Plot figure 
twoway ///
(rarea sim_employed_high sim_employed_low year, sort color(green%20) ///
	legend(label(1 "Employed, simulated"))) ///
(line valid_employed year, sort color(green) ///
	legend(label(2 "Employed, UKHLS"))) ///
(rarea sim_student_high sim_student_low year, sort color(blue%20) ///
	legend(label(3 "Students, simulated"))) ///
(line valid_student year, sort color(blue) ///
	legend(label(4 "Students, UKHLS"))) ///
(rarea sim_inactive_high sim_inactive_low year, sort color(red%20) ///
	legend(label(5 "Non-employed, simulated"))) ///
(line valid_inactive year, sort color(red) ///
	legend(label(6 "Non-employed, UKHLS"))) ///
(rarea sim_retired_high sim_retired_low year, sort color(grey%20) ///
	legend(label(7 "Retired, simulated"))) ///
(line valid_retired year, sort color(grey) ///
	legend(label(8 "Retired, UKHLS"))), ///
	title("Economic Activity Status") ///
	subtitle("Ages 17-30, females ") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. ", ///
	size(vsmall))

* Save figure
graph export ///
	"$dir_output_files/economic_activity/validation_${country}_activity_status_ts_17_30_female.jpg", ///
	replace width(2400) height(1350) quality(100)
	
	
********************************************************************************
* 1.1.2 : Working age (17-65)
********************************************************************************

* Prepare validation data
use year dwt valid_employed valid_student valid_inactive dgn dag ///
	valid_retired using "$dir_data/ukhls_validation_full_sample.dta", ///
	clear
	
* Select sample 	
keep if inrange(dag,17,65)
	
collapse (mean) valid_employed valid_student valid_inactive valid_retired ///
	[aw = dwt], by(year dgn)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_employed sim_student sim_inactive sim_retired dgn dag using ///
	"$dir_data/simulated_data_full.dta", clear

* Select sample 	
keep if inrange(dag,17,65)	
	
gen dgn_coded = .
replace dgn_coded = 1 if dgn == "Male"
replace dgn_coded = 0 if dgn == "Female"

drop dgn
rename dgn_coded dgn	
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired, ///
	by(run year dgn)
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired ///
		 (sd) sim_employed_sd = sim_employed ///
		 sim_student_sd = sim_student ///
		 sim_inactive_sd = sim_inactive ///
		 sim_retired_sd = sim_retired ///
		 , by(year dgn)
		 
foreach varname in sim_employed sim_student sim_inactive sim_retired {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year dgn using "$dir_data/temp_valid_stats.dta", keep(3) nogen


** All 

preserve

collapse (mean) sim* valid*, by(year)

* Plot figure 
twoway ///
(rarea sim_employed_high sim_employed_low year, sort color(green%20) ///
	legend(label(1 "Employed, simulated"))) ///
(line valid_employed year, sort color(green) ///
	legend(label(2 "Employed, UKHLS"))) ///
(rarea sim_student_high sim_student_low year, sort color(blue%20) ///
	legend(label(3 "Students, simulated"))) ///
(line valid_student year, sort color(blue) ///
	legend(label(4 "Students, UKHLS"))) ///
(rarea sim_inactive_high sim_inactive_low year, sort color(red%20) ///
	legend(label(5 "Non-employed, simulated"))) ///
(line valid_inactive year, sort color(red) ///
	legend(label(6 "Non-employed, UKHLS"))) ///
(rarea sim_retired_high sim_retired_low year, sort color(grey%20) ///
	legend(label(7 "Retired, simulated"))) ///
(line valid_retired year, sort color(grey) ///
	legend(label(8 "Retired, UKHLS"))), ///
	title("Economic Activity Status") ///
	subtitle("Ages 17-${max_age}") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. ", ///
	size(vsmall))

* Save figure
graph export ///
	"$dir_output_files/economic_activity/validation_${country}_activity_status_ts_17_${max_age}_both.jpg", ///
	replace width(2400) height(1350) quality(100)

restore, preserve
	
** By gender 
* Male
keep if dgn == 1 

* Plot figure 
twoway ///
(rarea sim_employed_high sim_employed_low year, sort color(green%20) ///
	legend(label(1 "Employed, simulated"))) ///
(line valid_employed year, sort color(green) ///
	legend(label(2 "Employed, UKHLS"))) ///
(rarea sim_student_high sim_student_low year, sort color(blue%20) ///
	legend(label(3 "Students, simulated"))) ///
(line valid_student year, sort color(blue) ///
	legend(label(4 "Students, UKHLS"))) ///
(rarea sim_inactive_high sim_inactive_low year, sort color(red%20) ///
	legend(label(5 "Non-employed, simulated"))) ///
(line valid_inactive year, sort color(red) ///
	legend(label(6 "Non-employed, UKHLS"))) ///
(rarea sim_retired_high sim_retired_low year, sort color(grey%20) ///
	legend(label(7 "Retired, simulated"))) ///
(line valid_retired year, sort color(grey) ///
	legend(label(8 "Retired, UKHLS"))), ///
	title("Economic Activity Status") ///
	subtitle("Ages 17-${max_age}, males") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. ", ///
	size(vsmall))
	
* Save figure
graph export ///
	"$dir_output_files/economic_activity/validation_${country}_activity_status_ts_17_${max_age}_male.jpg", ///
	replace width(2400) height(1350) quality(100)


restore, preserve

	
* Female
keep if dgn == 0 

* Plot figure 
twoway ///
(rarea sim_employed_high sim_employed_low year, sort color(green%20) ///
	legend(label(1 "Employed, simulated"))) ///
(line valid_employed year, sort color(green) ///
	legend(label(2 "Employed, UKHLS"))) ///
(rarea sim_student_high sim_student_low year, sort color(blue%20) ///
	legend(label(3 "Students, simulated"))) ///
(line valid_student year, sort color(blue) ///
	legend(label(4 "Students, UKHLS"))) ///
(rarea sim_inactive_high sim_inactive_low year, sort color(red%20) ///
	legend(label(5 "Non-employed, simulated"))) ///
(line valid_inactive year, sort color(red) ///
	legend(label(6 "Non-employed, UKHLS"))) ///
(rarea sim_retired_high sim_retired_low year, sort color(grey%20) ///
	legend(label(7 "Retired, simulated"))) ///
(line valid_retired year, sort color(grey) ///
	legend(label(8 "Retired, UKHLS"))), ///
	title("Economic Activity Status") ///
	subtitle("Ages 17-${max_age}, females") /// 
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. ", ///
	size(vsmall))
	
* Save figure
graph export ///
"$dir_output_files/economic_activity/validation_${country}_activity_status_ts_17_${max_age}_female.jpg", ///
	replace width(2400) height(1350) quality(100)
	
restore 	
	
	
* Females ages 17-60 (before state pension age)

* Prepare validation data
use year dwt valid_employed valid_student valid_inactive dgn dag ///
	valid_retired using "$dir_data/ukhls_validation_full_sample.dta", ///
	clear
	
* Select sample 	
keep if inrange(dag,17,60)	
	
collapse (mean) valid_employed valid_student valid_inactive valid_retired ///
	[aw = dwt], by(year dgn)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_employed sim_student sim_inactive sim_retired dgn dag using ///
	"$dir_data/simulated_data_full.dta", clear
	
* Select sample 	
keep if inrange(dag,17,60)

gen dgn_coded = .
replace dgn_coded = 1 if dgn == "Male"
replace dgn_coded = 0 if dgn == "Female"

drop dgn
rename dgn_coded dgn	
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired, ///
	by(run year dgn)
collapse (mean) sim_employed sim_student sim_inactive sim_retired ///
		 (sd) sim_employed_sd = sim_employed ///
		 sim_student_sd = sim_student ///
		 sim_inactive_sd = sim_inactive ///
		 sim_retired_sd = sim_retired ///
		 , by(year dgn)
		 
foreach varname in sim_employed sim_student sim_inactive sim_retired {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year dgn using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Females 
keep if dgn == 0

* Plot figure 
twoway ///
(rarea sim_employed_high sim_employed_low year, sort color(green%20) ///
	legend(label(1 "Employed, simulated"))) ///
(line valid_employed year, sort color(green) ///
	legend(label(2 "Employed, UKHLS"))) ///
(rarea sim_student_high sim_student_low year, sort color(blue%20) ///
	legend(label(3 "Students, simulated"))) ///
(line valid_student year, sort color(blue) ///
	legend(label(4 "Students, UKHLS"))) ///
(rarea sim_inactive_high sim_inactive_low year, sort color(red%20) ///
	legend(label(5 "Non-employed, simulated"))) ///
(line valid_inactive year, sort color(red) ///
	legend(label(6 "Non-employed, UKHLS"))) ///
(rarea sim_retired_high sim_retired_low year, sort color(grey%20) ///
	legend(label(7 "Retired, simulated"))) ///
(line valid_retired year, sort color(grey) ///
	legend(label(8 "Retired, UKHLS"))), ///
	title("Economic Activity Status") ///
	subtitle("Ages 17-60") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. ", ///
	size(vsmall))

* Save figure
graph export ///
"$dir_output_files/economic_activity/validation_${country}_activity_status_ts_17_60_female.jpg", ///
	replace width(2400) height(1350) quality(100)

	
********************************************************************************
* 1.1.2.1 : Working age by partnership status 
********************************************************************************

* Prepare validation data
use year dwt valid_employed valid_student valid_inactive valid_retired dcpst ///
	dgn dag using "$dir_data/ukhls_validation_full_sample.dta", clear
	
* Select sample 	
keep if inrange(dag,17,65)	
	
collapse (mean) valid_employed valid_student valid_inactive valid_retired ///
	[aw = dwt], by(year dcpst dgn)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_employed sim_student sim_inactive sim_retired dcpst dgn dag ///
	using "$dir_data/simulated_data_full.dta", clear

* Select sample 	
keep if inrange(dag,17,65)	
	
gen dcpst_coded = .
replace dcpst_coded = 1 if dcpst == "Partnered"
replace dcpst_coded = 3 if dcpst == "PreviouslyPartnered"
replace dcpst_coded = 2 if dcpst == "SingleNeverMarried"

drop dcpst
rename dcpst_coded dcpst

gen dgn_coded = .
replace dgn_coded = 1 if dgn == "Male"
replace dgn_coded = 0 if dgn == "Female"

drop dgn
rename dgn_coded dgn

collapse (mean) sim_employed sim_student sim_inactive sim_retired, ///
	by(run year dcpst dgn)
collapse (mean) sim_employed sim_student sim_inactive sim_retired ///
		 (sd) sim_employed_sd = sim_employed ///
		 sim_student_sd = sim_student ///
		 sim_inactive_sd = sim_inactive ///
		 sim_retired_sd = sim_retired ///
		 , by(year dcpst dgn)
		 
foreach varname in sim_employed sim_student sim_inactive sim_retired {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

	}

merge 1:1 year dcpst dgn using "$dir_data/temp_valid_stats.dta", keep(3) nogen


** All 

preserve

collapse (mean) sim* valid*, by(year dcpst)

* Plot figure: dcpst == 1, partnered
keep if dcpst == 1

twoway ///
(rarea sim_employed_high sim_employed_low year, sort color(green%20) ///
	legend(label(1 "Employed, simulated"))) ///
(line valid_employed year, sort color(green) ///
	legend(label(2 "Employed, UKHLS"))) ///
(rarea sim_student_high sim_student_low year, sort color(blue%20) ///
	legend(label(3 "Students, simulated"))) ///
(line valid_student year, sort color(blue) ///
	legend(label(4 "Students, UKHLS"))) ///
(rarea sim_inactive_high sim_inactive_low year, sort color(red%20) ///
	legend(label(5 "Non-employed, simulated"))) ///
(line valid_inactive year, sort color(red) ///
	legend(label(6 "Non-employed, UKHLS"))) ///
(rarea sim_retired_high sim_retired_low year, sort color(grey%20) ///
	legend(label(7 "Retired, simulated"))) ///
(line valid_retired year, sort color(grey) ///
	legend(label(8 "Retired, UKHLS"))), ///
title("Economic Activity Status") ///
	subtitle("Ages 17-${max_age}, partnered") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. ", ///
	size(vsmall))

graph export ///
"$dir_output_files/economic_activity/validation_${country}_activity_status_ts_17_${max_age}_both_partnered.jpg", ///
	replace width(2400) height(1350) quality(100) 

restore, preserve

collapse (mean) sim* valid*, by(year dcpst)

* Plot figure: dcpst == 2, single
keep if dcpst == 2

twoway ///
(rarea sim_employed_high sim_employed_low year, sort color(green%20) ///
	legend(label(1 "Employed, simulated"))) ///
(line valid_employed year, sort color(green) ///
	legend(label(2 "Employed, UKHLS"))) ///
(rarea sim_student_high sim_student_low year, sort color(blue%20) ///
	legend(label(3 "Students, simulated"))) ///
(line valid_student year, sort color(blue) ///
	legend(label(4 "Students, UKHLS"))) ///
(rarea sim_inactive_high sim_inactive_low year, sort color(red%20) ///
	legend(label(5 "Non-employed, simulated"))) ///
(line valid_inactive year, sort color(red) ///
	legend(label(6 "Non-employed, UKHLS"))) ///
(rarea sim_retired_high sim_retired_low year, sort color(grey%20) ///
	legend(label(7 "Retired, simulated"))) ///
(line valid_retired year, sort color(grey) ///
	legend(label(8 "Retired, UKHLS"))), ///
	title("Economic Activity Status") ///
	subtitle("Ages 17-${max_age}, single") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. ", ///
	size(vsmall))

graph export ///
"$dir_output_files/economic_activity/validation_${country}_activity_status_ts_17_${max_age}_both_single.jpg", ///
	replace width(2400) height(1350) quality(100)

restore, preserve

collapse (mean) sim* valid*, by(year dcpst)

* Plot figure: dcpst == 3, previously partnered
keep if dcpst == 3

twoway ///
(rarea sim_employed_high sim_employed_low year, sort color(green%20) ///
	legend(label(1 "Employed, simulated"))) ///
(line valid_employed year, sort color(green) ///
	legend(label(2 "Employed, UKHLS"))) ///
(rarea sim_student_high sim_student_low year, sort color(blue%20) ///
	legend(label(3 "Students, simulated"))) ///
(line valid_student year, sort color(blue) ///
	legend(label(4 "Students, UKHLS"))) ///
(rarea sim_inactive_high sim_inactive_low year, sort color(red%20) ///
	legend(label(5 "Non-employed, simulated"))) ///
(line valid_inactive year, sort color(red) ///
	legend(label(6 "Non-employed, UKHLS"))) ///
(rarea sim_retired_high sim_retired_low year, sort color(grey%20) ///
	legend(label(7 "Retired, simulated"))) ///
(line valid_retired year, sort color(grey) ///
	legend(label(8 "Retired, UKHLS"))), ///
	title("Economic Activity Status") ///
	subtitle("Ages 17-${max_age}, previously partnered") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. ", ///
	size(vsmall))

graph export ///
"$dir_output_files/economic_activity/validation_${country}_activity_status_ts_17_${max_age}_both_prev_partnered.jpg", ///
	replace width(2400) height(1350) quality(100)

restore


** Males

* Plot figure: dcpst == 1, partnered
preserve

keep if dcpst == 1 & dgn == 1

twoway ///
(rarea sim_employed_high sim_employed_low year, sort color(green%20) ///
	legend(label(1 "Employed, simulated"))) ///
(line valid_employed year, sort color(green) ///
	legend(label(2 "Employed, UKHLS"))) ///
(rarea sim_student_high sim_student_low year, sort color(blue%20) ///
	legend(label(3 "Students, simulated"))) ///
(line valid_student year, sort color(blue) ///
	legend(label(4 "Students, UKHLS"))) ///
(rarea sim_inactive_high sim_inactive_low year, sort color(red%20) ///
	legend(label(5 "Non-employed, simulated"))) ///
(line valid_inactive year, sort color(red) ///
	legend(label(6 "Non-employed, UKHLS"))) ///
(rarea sim_retired_high sim_retired_low year, sort color(grey%20) ///
	legend(label(7 "Retired, simulated"))) ///
(line valid_retired year, sort color(grey) ///
	legend(label(8 "Retired, UKHLS"))), ///
	title("Economic Activity Status") ///
	subtitle("Ages 17-${max_age}, partnered males") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. ", ///
	size(vsmall))

graph export ///
"$dir_output_files/economic_activity/validation_${country}_activity_status_ts_17_${max_age}_male_partnered.jpg", ///
	replace width(2400) height(1350) quality(100)

restore, preserve

* Plot figure: dcpst == 2, single
keep if dcpst == 2 & dgn == 1

twoway ///
(rarea sim_employed_high sim_employed_low year, sort color(green%20) ///
	legend(label(1 "Employed, simulated"))) ///
(line valid_employed year, sort color(green) ///
	legend(label(2 "Employed, UKHLS"))) ///
(rarea sim_student_high sim_student_low year, sort color(blue%20) ///
	legend(label(3 "Students, simulated"))) ///
(line valid_student year, sort color(blue) ///
	legend(label(4 "Students, UKHLS"))) ///
(rarea sim_inactive_high sim_inactive_low year, sort color(red%20) ///
	legend(label(5 "Non-employed, simulated"))) ///
(line valid_inactive year, sort color(red) ///
	legend(label(6 "Non-employed, UKHLS"))) ///
(rarea sim_retired_high sim_retired_low year, sort color(grey%20) ///
	legend(label(7 "Retired, simulated"))) ///
(line valid_retired year, sort color(grey) ///
	legend(label(8 "Retired, UKHLS"))), ///
	title("Economic Activity Status") ///
	subtitle("Ages 17-${max_age}, single males") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. ", ///
	size(vsmall))

graph export ///
"$dir_output_files/economic_activity/validation_${country}_activity_status_ts_17_${max_age}_male_single.jpg", ///
	replace width(2400) height(1350) quality(100)

restore, preserve

* Plot figure: dcpst == 3, previously partnered
keep if dcpst == 3 & dgn == 1

twoway ///
(rarea sim_employed_high sim_employed_low year, sort color(green%20) ///
	legend(label(1 "Employed, simulated"))) ///
(line valid_employed year, sort color(green) ///
	legend(label(2 "Employed, UKHLS"))) ///
(rarea sim_student_high sim_student_low year, sort color(blue%20) ///
	legend(label(3 "Students, simulated"))) ///
(line valid_student year, sort color(blue) ///
	legend(label(4 "Students, UKHLS"))) ///
(rarea sim_inactive_high sim_inactive_low year, sort color(red%20) ///
	legend(label(5 "Non-employed, simulated"))) ///
(line valid_inactive year, sort color(red) ///
	legend(label(6 "Non-employed, UKHLS"))) ///
(rarea sim_retired_high sim_retired_low year, sort color(grey%20) ///
	legend(label(7 "Retired, simulated"))) ///
(line valid_retired year, sort color(grey) ///
	legend(label(8 "Retired, UKHLS"))), ///
	title("Economic Activity Status") ///
	subtitle("Ages 17-${max_age}, prevously partnered males") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. ", ///
	size(vsmall))

graph export ///
"$dir_output_files/economic_activity/validation_${country}_activity_status_ts_17_${max_age}_male_prev_partnered.jpg", ///
	replace width(2400) height(1350) quality(100)

restore


** Females

* Plot figure: dcpst == 1, partnered
preserve

keep if dcpst == 1 & dgn == 0

twoway ///
(rarea sim_employed_high sim_employed_low year, sort color(green%20) ///
	legend(label(1 "Employed, simulated"))) ///
(line valid_employed year, sort color(green) ///
	legend(label(2 "Employed, UKHLS"))) ///
(rarea sim_student_high sim_student_low year, sort color(blue%20) ///
	legend(label(3 "Students, simulated"))) ///
(line valid_student year, sort color(blue) ///
	legend(label(4 "Students, UKHLS"))) ///
(rarea sim_inactive_high sim_inactive_low year, sort color(red%20) ///
	legend(label(5 "Non-employed, simulated"))) ///
(line valid_inactive year, sort color(red) ///
	legend(label(6 "Non-employed, UKHLS"))) ///
(rarea sim_retired_high sim_retired_low year, sort color(grey%20) ///
	legend(label(7 "Retired, simulated"))) ///
(line valid_retired year, sort color(grey) ///
	legend(label(8 "Retired, UKHLS"))), ///
	title("Economic Activity Status") ///
	subtitle("Ages 17-${max_age}, partnered females") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. ", ///
	size(vsmall))

graph export ///
"$dir_output_files/economic_activity/validation_${country}_activity_status_ts_17_${max_age}_female_partnered.jpg", ///
	replace width(2400) height(1350) quality(100)

restore, preserve

* Plot figure: dcpst == 2, single
keep if dcpst == 2 & dgn == 0

twoway ///
(rarea sim_employed_high sim_employed_low year, sort color(green%20) ///
	legend(label(1 "Employed, simulated"))) ///
(line valid_employed year, sort color(green) ///
	legend(label(2 "Employed, UKHLS"))) ///
(rarea sim_student_high sim_student_low year, sort color(blue%20) ///
	legend(label(3 "Students, simulated"))) ///
(line valid_student year, sort color(blue) ///
	legend(label(4 "Students, UKHLS"))) ///
(rarea sim_inactive_high sim_inactive_low year, sort color(red%20) ///
	legend(label(5 "Non-employed, simulated"))) ///
(line valid_inactive year, sort color(red) ///
	legend(label(6 "Non-employed, UKHLS"))) ///
(rarea sim_retired_high sim_retired_low year, sort color(grey%20) ///
	legend(label(7 "Retired, simulated"))) ///
(line valid_retired year, sort color(grey) ///
	legend(label(8 "Retired, UKHLS"))), ///
	title("Economic Activity Status") ///
	subtitle("Ages 17-${max_age}, single females") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. ", ///
	size(vsmall))

graph export ///
"$dir_output_files/economic_activity/validation_${country}_activity_status_ts_17_${max_age}_female_single.jpg", ///
	replace width(2400) height(1350) quality(100)

restore, preserve

* Plot figure: dcpst == 3, previously partnered
keep if dcpst == 3 & dgn == 0

twoway ///
(rarea sim_employed_high sim_employed_low year, sort color(green%20) ///
	legend(label(1 "Employed, simulated"))) ///
(line valid_employed year, sort color(green) ///
	legend(label(2 "Employed, UKHLS"))) ///
(rarea sim_student_high sim_student_low year, sort color(blue%20) ///
	legend(label(3 "Students, simulated"))) ///
(line valid_student year, sort color(blue) ///
	legend(label(4 "Students, UKHLS"))) ///
(rarea sim_inactive_high sim_inactive_low year, sort color(red%20) ///
	legend(label(5 "Non-employed, simulated"))) ///
(line valid_inactive year, sort color(red) ///
	legend(label(6 "Non-employed, UKHLS"))) ///
(rarea sim_retired_high sim_retired_low year, sort color(grey%20) ///
	legend(label(7 "Retired, simulated"))) ///
(line valid_retired year, sort color(grey) ///
	legend(label(8 "Retired, UKHLS"))), ///
	title("Economic Activity Status") ///
	subtitle("Ages 17-${max_age}, previously partnered females") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. ", ///
	size(vsmall))

graph export ///
"$dir_output_files/economic_activity/validation_${country}_activity_status_ts_17_${max_age}_female_prev_partnered.jpg", ///
	replace width(2400) height(1350) quality(100)

graph drop _all	

restore 
	
	
********************************************************************************
* 1.1.3 : All ages 
********************************************************************************

* Prepare validation data
use year dwt valid_employed valid_student valid_inactive dgn ///
	valid_retired dag les_c4 using ///
	"$dir_data/ukhls_validation_full_sample.dta", clear
		
collapse (mean) valid_employed valid_student valid_inactive valid_retired ///
	[aw = dwt], by(year dgn)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_employed sim_student sim_inactive sim_retired dgn dag ///
	les_c4 using "$dir_data/simulated_data_full.dta", clear

gen dgn_coded = .
replace dgn_coded = 1 if dgn == "Male"
replace dgn_coded = 0 if dgn == "Female"

drop dgn
rename dgn_coded dgn	
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired, ///
	by(run year dgn)
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired ///
		 (sd) sim_employed_sd = sim_employed ///
		 sim_student_sd = sim_student ///
		 sim_inactive_sd = sim_inactive ///
		 sim_retired_sd = sim_retired ///
		 , by(year dgn)
		 
foreach varname in sim_employed sim_student sim_inactive sim_retired {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year dgn using "$dir_data/temp_valid_stats.dta", keep(3) nogen
 
** All  
 
preserve

collapse (mean) sim* valid*, by(year)

* Plot figure 
twoway ///
(rarea sim_employed_high sim_employed_low year, sort color(green%20) ///
	legend(label(1 "Employed, simulated"))) ///
(line valid_employed year, sort color(green) ///
	legend(label(2 "Employed, UKHLS"))) ///
(rarea sim_student_high sim_student_low year, sort color(blue%20) ///
	legend(label(3 "Students, simulated"))) ///
(line valid_student year, sort color(blue) ///
	legend(label(4 "Students, UKHLS"))) ///
(rarea sim_inactive_high sim_inactive_low year, sort color(red%20) ///
	legend(label(5 "Non-employed, simulated"))) ///
(line valid_inactive year, sort color(red) ///
	legend(label(6 "Non-employed, UKHLS"))) ///
(rarea sim_retired_high sim_retired_low year, sort color(grey%20) ///
	legend(label(7 "Retired, simulated"))) ///
(line valid_retired year, sort color(grey) ///
	legend(label(8 "Retired, UKHLS"))), ///
	title("Economic Activity Status") ///
	subtitle("All ages") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. ", ///
	size(vsmall))

* Save figure
graph export ///
"$dir_output_files/economic_activity/validation_${country}_activity_status_ts_all_both.jpg", ///
	replace width(2400) height(1350) quality(100) 	
	
restore, preserve


** By gender 	
* Males 

keep if dgn == 1 

* Plot figure 
twoway ///
(rarea sim_employed_high sim_employed_low year, sort color(green%20) ///
	legend(label(1 "Employed, simulated"))) ///
(line valid_employed year, sort color(green) ///
	legend(label(2 "Employed, UKHLS"))) ///
(rarea sim_student_high sim_student_low year, sort color(blue%20) ///
	legend(label(3 "Students, simulated"))) ///
(line valid_student year, sort color(blue) ///
	legend(label(4 "Students, UKHLS"))) ///
(rarea sim_inactive_high sim_inactive_low year, sort color(red%20) ///
	legend(label(5 "Non-employed, simulated"))) ///
(line valid_inactive year, sort color(red) ///
	legend(label(6 "Non-employed, UKHLS"))) ///
(rarea sim_retired_high sim_retired_low year, sort color(grey%20) ///
	legend(label(7 "Retired, simulated"))) ///
(line valid_retired year, sort color(grey) ///
	legend(label(8 "Retired, UKHLS"))), ///
	title("Economic Activity Status") ///
	subtitle("All ages, males") /// 
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. ", ///
	size(vsmall))

* Save figure
graph export ///
"$dir_output_files/economic_activity/validation_${country}_activity_status_ts_all_male.jpg", ///
	replace width(2400) height(1350) quality(100)

restore, preserve

	
* Females 

keep if dgn == 0 

* Plot figure 
twoway ///
(rarea sim_employed_high sim_employed_low year, sort color(green%20) ///
	legend(label(1 "Employed, simulated"))) ///
(line valid_employed year, sort color(green) ///
	legend(label(2 "Employed, UKHLS"))) ///
(rarea sim_student_high sim_student_low year, sort color(blue%20) ///
	legend(label(3 "Students, simulated"))) ///
(line valid_student year, sort color(blue) ///
	legend(label(4 "Students, UKHLS"))) ///
(rarea sim_inactive_high sim_inactive_low year, sort color(red%20) ///
	legend(label(5 "Non-employed, simulated"))) ///
(line valid_inactive year, sort color(red) ///
	legend(label(6 "Non-employed, UKHLS"))) ///
(rarea sim_retired_high sim_retired_low year, sort color(grey%20) ///
	legend(label(7 "Retired, simulated"))) ///
(line valid_retired year, sort color(grey) ///
	legend(label(8 "Retired, UKHLS"))), ///
	title("Economic Activity Status") ///
	subtitle("All ages, females") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. ", ///
	size(vsmall))

* Save figure
graph export ///
"$dir_output_files/economic_activity/validation_${country}_activity_status_ts_all_female.jpg", ///
	replace width(2400) height(1350) quality(100)
	
restore 

graph drop _all 	


********************************************************************************
* 1.1.4 : Adult population 17+
********************************************************************************

* Prepare validation data
use year dwt valid_employed valid_student valid_inactive dgn dag ///
	valid_retired using ///
	"$dir_data/ukhls_validation_full_sample.dta", ///
	clear
		
* Select sample		
drop if dag < 17 		
		
collapse (mean) valid_employed valid_student valid_inactive valid_retired ///
	[aw = dwt], by(year dgn)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_employed sim_student sim_inactive sim_retired dgn dag using ///
	"$dir_data/simulated_data_full.dta", clear

drop if dag < 17		
	
gen dgn_coded = .
replace dgn_coded = 1 if dgn == "Male"
replace dgn_coded = 0 if dgn == "Female"

drop dgn
rename dgn_coded dgn	
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired, ///
	by(run year dgn)
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired ///
		 (sd) sim_employed_sd = sim_employed ///
		 sim_student_sd = sim_student ///
		 sim_inactive_sd = sim_inactive ///
		 sim_retired_sd = sim_retired ///
		 , by(year dgn)
		 
foreach varname in sim_employed sim_student sim_inactive sim_retired {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year dgn using "$dir_data/temp_valid_stats.dta", keep(3) nogen
 
 
** All  
 
preserve

collapse (mean) sim* valid*, by(year)

* Plot figure 
twoway ///
(rarea sim_employed_high sim_employed_low year, sort color(green%20) ///
	legend(label(1 "Employed, simulated"))) ///
(line valid_employed year, sort color(green) ///
	legend(label(2 "Employed, UKHLS"))) ///
(rarea sim_student_high sim_student_low year, sort color(blue%20) ///
	legend(label(3 "Students, simulated"))) ///
(line valid_student year, sort color(blue) ///
	legend(label(4 "Students, UKHLS"))) ///
(rarea sim_inactive_high sim_inactive_low year, sort color(red%20) ///
	legend(label(5 "Non-employed, simulated"))) ///
(line valid_inactive year, sort color(red) ///
	legend(label(6 "Non-employed, UKHLS"))) ///
(rarea sim_retired_high sim_retired_low year, sort color(grey%20) ///
	legend(label(7 "Retired, simulated"))) ///
(line valid_retired year, sort color(grey) ///
	legend(label(8 "Retired, UKHLS"))), ///
	title("Economic Activity Status") ///
	subtitle("Ages 17+") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. ", ///
	size(vsmall))

* Save figure
graph export ///
"$dir_output_files/economic_activity/validation_${country}_activity_status_ts_17plus_both.jpg", ///
	replace width(2400) height(1350) quality(100) 	
	
restore, preserve


** By gender 	
* Males 

keep if dgn == 1 

* Plot figure 
twoway ///
(rarea sim_employed_high sim_employed_low year, sort color(green%20) ///
	legend(label(1 "Employed, simulated"))) ///
(line valid_employed year, sort color(green) ///
	legend(label(2 "Employed, UKHLS"))) ///
(rarea sim_student_high sim_student_low year, sort color(blue%20) ///
	legend(label(3 "Students, simulated"))) ///
(line valid_student year, sort color(blue) ///
	legend(label(4 "Students, UKHLS"))) ///
(rarea sim_inactive_high sim_inactive_low year, sort color(red%20) ///
	legend(label(5 "Non-employed, simulated"))) ///
(line valid_inactive year, sort color(red) ///
	legend(label(6 "Non-employed, UKHLS"))) ///
(rarea sim_retired_high sim_retired_low year, sort color(grey%20) ///
	legend(label(7 "Retired, simulated"))) ///
(line valid_retired year, sort color(grey) ///
	legend(label(8 "Retired, UKHLS"))), ///
	title("Economic Activity Status") ///
	subtitle("Ages 17+, males") /// 
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. ", ///
	size(vsmall))

* Save figure
graph export ///
"$dir_output_files/economic_activity/validation_${country}_activity_status_ts_17plus_male.jpg", ///
	replace width(2400) height(1350) quality(100)

restore, preserve

	
* Females 

keep if dgn == 0 

* Plot figure 
twoway ///
(rarea sim_employed_high sim_employed_low year, sort color(green%20) ///
	legend(label(1 "Employed, simulated"))) ///
(line valid_employed year, sort color(green) ///
	legend(label(2 "Employed, UKHLS"))) ///
(rarea sim_student_high sim_student_low year, sort color(blue%20) ///
	legend(label(3 "Students, simulated"))) ///
(line valid_student year, sort color(blue) ///
	legend(label(4 "Students, UKHLS"))) ///
(rarea sim_inactive_high sim_inactive_low year, sort color(red%20) ///
	legend(label(5 "Non-employed, simulated"))) ///
(line valid_inactive year, sort color(red) ///
	legend(label(6 "Non-employed, UKHLS"))) ///
(rarea sim_retired_high sim_retired_low year, sort color(grey%20) ///
	legend(label(7 "Retired, simulated"))) ///
(line valid_retired year, sort color(grey) ///
	legend(label(8 "Retired, UKHLS"))), ///
	title("Economic Activity Status") ///
	subtitle("Ages 17+, females") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. ", ///
	size(vsmall))

* Save figure
graph export ///
"$dir_output_files/economic_activity/validation_${country}_activity_status_ts_17plus_female.jpg", ///
	replace width(2400) height(1350) quality(100)
	
restore 

graph drop _all 	


********************************************************************************
* 1.2 : Mean values over time - Share Employed
********************************************************************************

********************************************************************************
* 1.2.1 : Working age (17-65)
********************************************************************************

* Prepare validation data
use year dwt valid_employed valid_student valid_inactive dgn dag  ///
	valid_retired using "$dir_data/ukhls_validation_full_sample.dta", ///
	clear
	
* Select sample		
keep if inrange(dag,17,65)	
	
collapse (mean) valid_employed valid_student valid_inactive valid_retired ///
	[aw = dwt], by(year dgn)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_employed sim_student sim_inactive sim_retired dgn dag using ///
	"$dir_data/simulated_data_full.dta", clear
	
* Select sample		
keep if inrange(dag,17,65)	

gen dgn_coded = .
replace dgn_coded = 1 if dgn == "Male"
replace dgn_coded = 0 if dgn == "Female"

drop dgn
rename dgn_coded dgn	
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired, ///
	by(run year dgn)
collapse (mean) sim_employed sim_student sim_inactive sim_retired ///
		 (sd) sim_employed_sd = sim_employed ///
		 sim_student_sd = sim_student ///
		 sim_inactive_sd = sim_inactive ///
		 sim_retired_sd = sim_retired ///
		 , by(year dgn)
		 
foreach varname in sim_employed sim_student sim_inactive sim_retired {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

	}

merge 1:1 year dgn using "$dir_data/temp_valid_stats.dta", keep(3) nogen


** All 

preserve

collapse (mean) sim* valid*, by(year)

* Plot figure 
twoway ///
(rarea sim_employed_high sim_employed_low year, sort color(green%20) ///
	legend(label(1 "Employed, simulated"))) ///
(line valid_employed year, sort color(green) ///
	legend(label(2 "Employed, UKHLS"))), ///
	title("Share Employed") ///
	subtitle("Ages 17-${max_age}") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note(Notes:, size(vsmall))

* Save figure
graph export ///
"$dir_output_files/economic_activity/validation_${country}_employed_ts_17_${max_age}_both.jpg", ///
	replace width(2400) height(1350) quality(100)

restore, preserve

	
** Males 

keep if dgn == 1 

* Plot figure 
twoway ///
(rarea sim_employed_high sim_employed_low year, sort color(green%20) ///
	legend(label(1 "Employed, simulated"))) ///
(line valid_employed year, sort color(green) ///
	legend(label(2 "Employed, UKHLS"))), ///
	title("Share Employed") ///
	subtitle("Ages 17-${max_age}, males") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note(Notes:, size(vsmall))
	
* Save figure
graph export ///
	"$dir_output_files/economic_activity/validation_${country}_employed_ts_17_${max_age}_male.jpg", ///
	replace width(2400) height(1350) quality(100)


restore, preserve

	
** Females 

keep if dgn == 0 

* Plot figure 
twoway ///
(rarea sim_employed_high sim_employed_low year, sort color(green%20) ///
	legend(label(1 "Employed, simulated"))) ///
(line valid_employed year, sort color(green) ///
	legend(label(2 "Employed, UKHLS"))), ///
	title("Share Employed") ///
	subtitle("Ages 17-${max_age}, females") /// 
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note(Notes:, size(vsmall))
	
* Save figure
graph export ///
"$dir_output_files/economic_activity/validation_${country}_employed_ts_17_${max_age}_female.jpg", ///
	replace width(2400) height(1350) quality(100)
	
restore 	
	
	
** Females ages 17-60 (before state pension age)

* Prepare validation data
use year dwt valid_employed valid_student valid_inactive dgn dag ///
	valid_retired using "$dir_data/ukhls_validation_full_sample.dta", ///
	clear
	
* Select sample		
keep if inrange(dag,17,60)
	
collapse (mean) valid_employed valid_student valid_inactive valid_retired ///
	[aw = dwt], by(year dgn)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_employed sim_student sim_inactive sim_retired dgn dag using ///
	"$dir_data/simulated_data_full.dta", clear
	
* Select sample		
keep if inrange(dag,17,60)

gen dgn_coded = .
replace dgn_coded = 1 if dgn == "Male"
replace dgn_coded = 0 if dgn == "Female"

drop dgn
rename dgn_coded dgn	
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired, ///
	by(run year dgn)
collapse (mean) sim_employed sim_student sim_inactive sim_retired ///
		 (sd) sim_employed_sd = sim_employed ///
		 sim_student_sd = sim_student ///
		 sim_inactive_sd = sim_inactive ///
		 sim_retired_sd = sim_retired ///
		 , by(year dgn)
		 
foreach varname in sim_employed sim_student sim_inactive sim_retired {
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd
}

merge 1:1 year dgn using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Females 
keep if dgn == 0

* Plot figure 
twoway ///
(rarea sim_employed_high sim_employed_low year, sort color(green%20) ///
	legend(label(1 "Employed, simulated"))) ///
(line valid_employed year, sort color(green) ///
	legend(label(2 "Employed, UKHLS"))), ///
	title("Share Employed") ///
	subtitle("Ages 17-60") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note(Notes:, size(vsmall))

* Save figure
graph export ///
"$dir_output_files/economic_activity/validation_${country}_employed_ts_17_60_female.jpg", ///
	replace width(2400) height(1350) quality(100)

	
********************************************************************************
* 1.2.2 : All ages
********************************************************************************

* Prepare validation data
use year dwt valid_employed valid_student valid_inactive dgn ///
	valid_retired using ///
	"$dir_data/ukhls_validation_full_sample.dta", ///
	clear
		
collapse (mean) valid_employed valid_student valid_inactive valid_retired ///
	[aw = dwt], by(year dgn)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_employed sim_student sim_inactive sim_retired dgn using ///
	"$dir_data/simulated_data_full.dta", clear

gen dgn_coded = .
replace dgn_coded = 1 if dgn == "Male"
replace dgn_coded = 0 if dgn == "Female"

drop dgn
rename dgn_coded dgn	
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired, ///
	by(run year dgn)
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired ///
		 (sd) sim_employed_sd = sim_employed ///
		 sim_student_sd = sim_student ///
		 sim_inactive_sd = sim_inactive ///
		 sim_retired_sd = sim_retired ///
		 , by(year dgn)
		 
foreach varname in sim_employed sim_student sim_inactive sim_retired {
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd
}

merge 1:1 year dgn using "$dir_data/temp_valid_stats.dta", keep(3) nogen
 
 
** All  
 
preserve

collapse (mean) sim* valid*, by(year)

* Plot figure 
twoway ///
(rarea sim_employed_high sim_employed_low year, sort color(green%20) ///
	legend(label(1 "Employed, simulated"))) ///
(line valid_employed year, sort color(green) ///
	legend(label(2 "Employed, UKHLS"))), ///
	title("Share Employed") ///
	subtitle("All ages") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note(Notes:, size(vsmall))

* Save figure
graph export ///
"$dir_output_files/economic_activity/validation_${country}_employed_ts_all_both.jpg", ///
	replace width(2400) height(1350) quality(100) 	
	
restore, preserve

	
** Males 

keep if dgn == 1 

* Plot figure 
twoway ///
(rarea sim_employed_high sim_employed_low year, sort color(green%20) ///
	legend(label(1 "Employed, simulated"))) ///
(line valid_employed year, sort color(green) ///
	legend(label(2 "Employed, UKHLS"))), ///
	title("Share Employed") ///
	subtitle("All ages, males") /// 
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note(Notes:, size(vsmall))

* Save figure
graph export ///
"$dir_output_files/economic_activity/validation_${country}_employed_ts_all_male.jpg", ///
	replace width(2400) height(1350) quality(100)

restore, preserve

	
** Females 

keep if dgn == 0 

* Plot figure 
twoway ///
(rarea sim_employed_high sim_employed_low year, sort color(green%20) ///
	legend(label(1 "Employed, simulated"))) ///
(line valid_employed year, sort color(green) ///
	legend(label(2 "Employed, UKHLS"))), ///
	title("Share Employed") ///
	subtitle("All ages, females") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note(Notes:, size(vsmall))

* Save figure
graph export ///
"$dir_output_files/economic_activity/validation_${country}_employed_ts_all_female.jpg", ///
	replace width(2400) height(1350) quality(100)
	
restore 

graph drop _all 	


********************************************************************************
* 1.2.3 : Adult population (17+)
********************************************************************************

* Prepare validation data
use year dwt valid_employed valid_student valid_inactive dgn dag ///
	valid_retired using ///
	"$dir_data/ukhls_validation_full_sample.dta", ///
	clear
		
drop if dag < 17
		
collapse (mean) valid_employed valid_student valid_inactive valid_retired ///
	[aw = dwt], by(year dgn)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_employed sim_student sim_inactive sim_retired dgn dag using ///
	"$dir_data/simulated_data_full.dta", clear

drop if dag < 17 	
	
gen dgn_coded = .
replace dgn_coded = 1 if dgn == "Male"
replace dgn_coded = 0 if dgn == "Female"

drop dgn
rename dgn_coded dgn	
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired, ///
	by(run year dgn)
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired ///
		 (sd) sim_employed_sd = sim_employed ///
		 sim_student_sd = sim_student ///
		 sim_inactive_sd = sim_inactive ///
		 sim_retired_sd = sim_retired ///
		 , by(year dgn)
		 
foreach varname in sim_employed sim_student sim_inactive sim_retired {
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd
}

merge 1:1 year dgn using "$dir_data/temp_valid_stats.dta", keep(3) nogen
 
 
** All  
 
preserve

collapse (mean) sim* valid*, by(year)

* Plot figure 
twoway ///
(rarea sim_employed_high sim_employed_low year, sort color(green%20) ///
	legend(label(1 "Employed, simulated"))) ///
(line valid_employed year, sort color(green) ///
	legend(label(2 "Employed, UKHLS"))), ///
	title("Share Employed") ///
	subtitle("Ages 17+") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note(Notes:, size(vsmall))

* Save figure
graph export ///
"$dir_output_files/economic_activity/validation_${country}_employed_ts_17plus_both.jpg", ///
	replace width(2400) height(1350) quality(100) 	
	
restore, preserve

	
** Males 

keep if dgn == 1 

* Plot figure 
twoway ///
(rarea sim_employed_high sim_employed_low year, sort color(green%20) ///
	legend(label(1 "Employed, simulated"))) ///
(line valid_employed year, sort color(green) ///
	legend(label(2 "Employed, UKHLS"))), ///
	title("Share Employed") ///
	subtitle("Ages 17+, males") /// 
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note(Notes:, size(vsmall))

* Save figure
graph export ///
"$dir_output_files/economic_activity/validation_${country}_employed_ts_17plus_male.jpg", ///
	replace width(2400) height(1350) quality(100)

restore, preserve

	
** Females 

keep if dgn == 0 

* Plot figure 
twoway ///
(rarea sim_employed_high sim_employed_low year, sort color(green%20) ///
	legend(label(1 "Employed, simulated"))) ///
(line valid_employed year, sort color(green) ///
	legend(label(2 "Employed, UKHLS"))), ///
	title("Share Employed") ///
	subtitle("Ages 17+, females") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note(Notes:, size(vsmall))

* Save figure
graph export ///
"$dir_output_files/economic_activity/validation_${country}_employed_ts_17plus_female.jpg", ///
	replace width(2400) height(1350) quality(100)
	
restore 

graph drop _all 	

********************************************************************************
* 1.2.4 : By age group 
********************************************************************************

* Prepare validation data
use year dwt dgn ageGroup valid_employed dag using ///
	"$dir_data/ukhls_validation_full_sample.dta", clear

keep if inrange(dag,16,79)	
	
gen employed_f = (valid_employed) if dgn == 0
gen employed_m = (valid_employed) if dgn == 1

drop if ageGroup == 0 | ageGroup == 8  

collapse (mean) employed_f employed_m [aweight=dwt], ///
	by(ageGroup year)
	
drop if missing(ageGroup)

reshape wide employed_f employed_m, i(year) j(ageGroup)

forvalues i = 1(1)7 {
	
	rename employed_f`i' employed_f_`i'_valid
	rename employed_m`i' employed_m_`i'_valid

}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year sim_sex ageGroup sim_employed using ///
	"$dir_data/simulated_data.dta", clear

gen employed_f = (sim_employed) if sim_sex == 2
gen employed_m = (sim_employed) if sim_sex == 1

collapse (mean)  employed_f employed_m, by(ageGroup run year)
drop if missing(ageGroup)

reshape wide employed_f employed_m, i(year run) j(ageGroup)

forvalues i = 1(1)7 { 
	
	rename employed_f`i' employed_f_`i'_sim
	rename employed_m`i' employed_m_`i'_sim
	
}

collapse (mean) employed* ///
	(sd) sd_employed_f_1_sim=employed_f_1_sim ///
		 sd_employed_f_2_sim=employed_f_2_sim ///
		 sd_employed_f_3_sim=employed_f_3_sim ///
		 sd_employed_f_4_sim=employed_f_4_sim ///
		 sd_employed_f_5_sim=employed_f_5_sim ///
		 sd_employed_f_6_sim=employed_f_6_sim ///
		 sd_employed_f_7_sim=employed_f_7_sim ///
		 sd_employed_m_1_sim=employed_m_1_sim ///
		 sd_employed_m_2_sim=employed_m_2_sim ///
		 sd_employed_m_3_sim=employed_m_3_sim ///
		 sd_employed_m_4_sim=employed_m_4_sim ///
		 sd_employed_m_5_sim=employed_m_5_sim ///
		 sd_employed_m_6_sim=employed_m_6_sim ///
		 sd_employed_m_7_sim=employed_m_7_sim ///
		 , by(year)
		 
		 /* 
		 sd_employed_f_8_sim=employed_f_8_sim ///
		 sd_employed_m_8_sim=employed_m_8_sim /// */

forvalues i = 1(1)7 {
	
	gen employed_f_`i'_sim_high = ///
		employed_f_`i'_sim + 1.96*sd_employed_f_`i'_sim
	gen employed_f_`i'_sim_low = ///
		employed_f_`i'_sim - 1.96*sd_employed_f_`i'_sim
	gen employed_m_`i'_sim_high = ///
		employed_m_`i'_sim + 1.96*sd_employed_m_`i'_sim
	gen employed_m_`i'_sim_low = ///
		employed_m_`i'_sim - 1.96*sd_employed_m_`i'_sim	

		}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figures
foreach vble in "employed_f" "employed_m" {
	
	twoway (rarea `vble'_1_sim_high `vble'_1_sim_low year, ///
		sort color(green%20) ///
		legend(label(1 "Simulated") position(6) rows(1))) ///
	(line `vble'_1_valid year, sort color(green) ///
		legend(label(2 "UKHLS"))), ///
		title("Age 16-19") ///
		name(`vble'_1, replace) ///
		ylabel(0.2 [0.4] 1) ///
		xtitle("") ///
		graphregion(color(white)) 
	
	twoway (rarea `vble'_2_sim_high `vble'_2_sim_low year, ///
		sort color(green%20) ///
		legend(label(1 "Simulated") position(6) rows(1))) ///
	(line `vble'_2_valid year, sort color(green) ///
		legend(label(2 "UKHLS"))), ///
		title("Age 20-24") ///
		name(`vble'_2, replace) ///
		ylabel(0.2 [0.4] 1) ///
		xtitle("") ///
		graphregion(color(white)) 
	
	twoway (rarea `vble'_3_sim_high `vble'_3_sim_low year, ///
		sort color(green%20) ///
		legend(label(1 "Simulated") position(6) rows(1))) ///
	(line `vble'_3_valid year, sort color(green) ///
		legend(label(2 "UKHLS"))), ///
		title("Age 25-29") ///
		name(`vble'_3, replace) ///
		ylabel(0.2 [0.4] 1) ///
		xtitle("")  ///
		graphregion(color(white)) 
	
	twoway (rarea `vble'_4_sim_high `vble'_4_sim_low year, ///
		sort color(green%20) ///
		legend(label(1 "Simulated") position(6) rows(1))) ///
	(line `vble'_4_valid year, sort color(green) ///
		legend(label(2 "UKHLS"))), ///
		title("Age 30-34") ///
		name(`vble'_4, replace) ///
		ylabel(0.2 [0.4] 1) ///
		xtitle("") ///
		graphregion(color(white)) 
	
	twoway (rarea `vble'_5_sim_high `vble'_5_sim_low year, ///
		sort color(green%20) ///
		legend(label(1 "Simulated") position(6) rows(1))) ///
	(line `vble'_5_valid year, sort color(green) ///
		legend(label(2 "UKHLS"))), ///
		title("Age 35-39") ///
		name(`vble'_5, replace) ///
		ylabel(0.2 [0.4] 1) ///
		xtitle("") ///
		graphregion(color(white)) 
	
	twoway (rarea `vble'_6_sim_high `vble'_6_sim_low year, ///
		sort color(green%20) ///
		legend(label(1 "Simulated") position(6) ///
		rows(1)))(line `vble'_6_valid year, sort color(green) ///
		legend(label(2 "UKHLS"))), ///
		title("Age 40-59") ///
		name(`vble'_6, replace) ///
		ylabel(0.2 [0.4] 1) ///
		xtitle("")  ///
		graphregion(color(white)) 
	
	twoway (rarea `vble'_7_sim_high `vble'_7_sim_low year, ///
		sort color(green%20) ///
		legend(label(1 "Simulated") position(6) rows(1))) ///
		(line `vble'_7_valid year, sort color(green) ///
		legend(label(2 "UKHLS"))), ///
		title("Age 60-79") ///
		name(`vble'_7, replace) ///
		ylabel(0.2 [0.4] 1) ///
		xtitle("") ///
		graphregion(color(white)) 
	
}

* Save figures
	
* Share employed males 	
grc1leg employed_m_1 employed_m_2 employed_m_3 employed_m_4 employed_m_5 ///
	employed_m_6 employed_m_7 , ///
	title("Share Employed by Age Group") ///
	subtitle("Males") ///
	legendfrom(employed_m_1) ///
	graphregion(color(white)) ///
	note("Notes: ", size(vsmall))

graph export ///
"$dir_output_files/economic_activity/validation_${country}_employed_ts_all_male.jpg", ///
	replace width(2400) height(1350) quality(100)

	
* Share employed females 	
grc1leg employed_f_1 employed_f_2 employed_f_3 employed_f_4 employed_f_5 ///
	employed_f_6 employed_f_7 , ///
	title("Share Employed by Age Group") ///
	subtitle("Females") ///
	legendfrom(employed_f_1) ///
	graphregion(color(white)) ///
	note("Notes: ", size(vsmall))
	
graph export ///
"$dir_output_files/economic_activity/validation_${country}_employed_ts_all_female.jpg", ///
	replace width(2400) height(1350) quality(100)	
	
graph drop _all 	



********************************************************************************
* 1.3 : Mean values over time -  Non-employed shares
********************************************************************************

********************************************************************************
* 1.3.1 : Working age (17-65)
********************************************************************************

* Prepare validation data
use year dwt valid_employed valid_student valid_inactive ///
	valid_retired dag using "$dir_data/ukhls_validation_full_sample.dta", ///
	clear
	
* Select sample 
keep if inrange(dag,17,65)	
	
drop if valid_employed == 1 
drop valid_employed	
	
collapse (mean) valid_student valid_inactive valid_retired ///
	[aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_employed sim_student sim_inactive sim_retired dag using ///
	"$dir_data/simulated_data_full.dta", clear

* Select sample 
keep if inrange(dag,17,65)
	
drop if sim_employed == 1 
drop sim_employed	
	
collapse (mean)  sim_student sim_inactive sim_retired, ///
	by(run year)
	
collapse (mean) sim_student sim_inactive sim_retired ///
		(sd)  sim_student_sd = sim_student ///
		 sim_inactive_sd = sim_inactive ///
		 sim_retired_sd = sim_retired ///
		 , by(year)
		 
foreach varname in  sim_student sim_inactive sim_retired {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd
	
}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen


** All 

preserve

collapse (mean) sim* valid*, by(year)

* Plot figure 
twoway ///
(rarea sim_student_high sim_student_low year, sort color(blue%20) ///
	legend(label(1 "Students, simulated"))) ///
(line valid_student year, sort color(blue) ///
	legend(label(2 "Students, UKHLS"))) ///
(rarea sim_inactive_high sim_inactive_low year, sort color(red%20) ///
	legend(label(3 "Non-employed, simulated"))) ///
(line valid_inactive year, sort color(red) ///
	legend(label(4 "Non-employed, UKHLS"))) ///
(rarea sim_retired_high sim_retired_low year, sort color(grey%20) ///
	legend(label(5 "Retired, simulated"))) ///
(line valid_retired year, sort color(grey) ///
	legend(label(6 "Retired, UKHLS"))), ///
	title("Economic Activity of the Non-Employed") ///
	subtitle("Ages 17-${max_age}") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.) minus" "students and retired. ", ///
	size(vsmall))

* Save figure
graph export ///
	"$dir_output_files/economic_activity/validation_${country}_activity_status_not_employed_ts_17_${max_age}_both.jpg", ///
	replace width(2400) height(1350) quality(100)


	
** Males 

* Prepare validation data
use year dwt valid_employed valid_student valid_inactive valid_retired ///
	 dgn dag using "$dir_data/ukhls_validation_full_sample.dta", ///
	clear
	
* Select sample 
keep if inrange(dag,17,65)
	
drop if dgn == 0	
drop if valid_employed == 1 
drop valid_employed	
	
collapse (mean) valid_student valid_inactive valid_retired ///
	[aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_employed sim_student sim_inactive sim_retired dgn dag using ///
	"$dir_data/simulated_data_full.dta", clear

* Select sample 
keep if inrange(dag,17,65)
	
drop if dgn == "Female"
drop if sim_employed == 1 
drop sim_employed	
	
collapse (mean)  sim_student sim_inactive sim_retired, ///
	by(run year)
	
collapse (mean) sim_student sim_inactive sim_retired ///
		(sd)  sim_student_sd = sim_student ///
		 sim_inactive_sd = sim_inactive ///
		 sim_retired_sd = sim_retired ///
		 , by(year)
		 
foreach varname in  sim_student sim_inactive sim_retired {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd
	
}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen


* Plot figure 
twoway ///
(rarea sim_student_high sim_student_low year, sort color(blue%20) ///
	legend(label(1 "Students, simulated"))) ///
(line valid_student year, sort color(blue) ///
	legend(label(2 "Students, UKHLS"))) ///
(rarea sim_inactive_high sim_inactive_low year, sort color(red%20) ///
	legend(label(3 "Non-employed, simulated"))) ///
(line valid_inactive year, sort color(red) ///
	legend(label(4 "Non-employed, UKHLS"))) ///
(rarea sim_retired_high sim_retired_low year, sort color(grey%20) ///
	legend(label(5 "Retired, simulated"))) ///
(line valid_retired year, sort color(grey) ///
	legend(label(6 "Retired, UKHLS"))), ///
	title("Economic Activity of the Non-Employed") ///
	subtitle("Ages 17-${max_age}, males") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. ", ///
	size(vsmall))
	
* Save figure
graph export ///
	"$dir_output_files/economic_activity/validation_${country}_activity_status_not_employed_ts_17_${max_age}_male.jpg", ///
	replace width(2400) height(1350) quality(100)


** Females 

* Prepare validation data
use year dwt valid_employed valid_student valid_inactive ///
	valid_retired dgn dag using "$dir_data/ukhls_validation_full_sample.dta", ///
	clear
	
* Select sample 
keep if inrange(dag,17,65)
	
drop if dgn == 1	
drop if valid_employed == 1 
drop valid_employed	
	
collapse (mean) valid_student valid_inactive valid_retired ///
	[aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_employed sim_student sim_inactive sim_retired dgn dag using ///
	"$dir_data/simulated_data_full.dta", clear

* Select sample 
keep if inrange(dag,17,65)
	
drop if dgn == "Male"
drop if sim_employed == 1 
drop sim_employed	
	
collapse (mean)  sim_student sim_inactive sim_retired, ///
	by(run year)
	
collapse (mean) sim_student sim_inactive sim_retired ///
		(sd)  sim_student_sd = sim_student ///
		 sim_inactive_sd = sim_inactive ///
		 sim_retired_sd = sim_retired ///
		 , by(year)
		 
foreach varname in  sim_student sim_inactive sim_retired {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

	}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure 
twoway ///
(rarea sim_student_high sim_student_low year, sort color(blue%20) ///
	legend(label(1 "Students, simulated"))) ///
(line valid_student year, sort color(blue) ///
	legend(label(2 "Students, UKHLS"))) ///
(rarea sim_inactive_high sim_inactive_low year, sort color(red%20) ///
	legend(label(3 "Non-employed, simulated"))) ///
(line valid_inactive year, sort color(red) ///
	legend(label(4 "Non-employed, UKHLS"))) ///
(rarea sim_retired_high sim_retired_low year, sort color(grey%20) ///
	legend(label(5 "Retired, simulated"))) ///
(line valid_retired year, sort color(grey) ///
	legend(label(6 "Retired, UKHLS"))), ///
	title("Economic Activity of the Non-Employed") ///
	subtitle("Ages 17-${max_age}, females") /// 
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. ", ///
	size(vsmall))
	
* Save figure
graph export ///
"$dir_output_files/economic_activity/validation_${country}_activity_status_not_employed_ts_17_${max_age}_female.jpg", ///
	replace width(2400) height(1350) quality(100)
	 	
	
	
** Females ages 17-60 (before state pension age)

* Prepare validation data
use year dwt valid_employed valid_student valid_inactive dgn dag ///
	valid_retired using "$dir_data/ukhls_validation_full_sample.dta", ///
	clear
	
* Select sample 
keep if inrange(dag,17,60)

drop if dgn == 1	
drop if valid_employed == 1 
drop valid_employed	dgn 
	
collapse (mean) valid_student valid_inactive valid_retired ///
	[aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_employed sim_student sim_inactive sim_retired dgn dag using ///
	"$dir_data/simulated_data_full.dta", clear
	
* Select sample 
keep if inrange(dag,17,60)

drop if dgn == "Male"
drop if sim_employed == 1 
drop sim_employed dgn 

	
collapse (mean) sim_student sim_inactive sim_retired, ///
	by(run year)
	
collapse (mean)  sim_student sim_inactive sim_retired ///
		 (sd) sim_student_sd = sim_student ///
		 sim_inactive_sd = sim_inactive ///
		 sim_retired_sd = sim_retired ///
		 , by(year)
		 
foreach varname in sim_student sim_inactive sim_retired {
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd
}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen


* Plot figure 
twoway ///
(rarea sim_student_high sim_student_low year, sort color(blue%20) ///
	legend(label(1 "Students, simulated"))) ///
(line valid_student year, sort color(blue) ///
	legend(label(2 "Students, UKHLS"))) ///
(rarea sim_inactive_high sim_inactive_low year, sort color(red%20) ///
	legend(label(3 "Non-employed, simulated"))) ///
(line valid_inactive year, sort color(red) ///
	legend(label(4 "Non-employed, UKHLS"))) ///
(rarea sim_retired_high sim_retired_low year, sort color(grey%20) ///
	legend(label(5 "Retired, simulated"))) ///
(line valid_retired year, sort color(grey) ///
	legend(label(6 "Retired, UKHLS"))), ///
	title("Economic Activity of the Non-Employed") ///
	subtitle("Ages 17-60") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. ", ///
	size(vsmall))

* Save figure
graph export ///
"$dir_output_files/economic_activity/validation_${country}_activity_status_not_employed_ts_17_60_female.jpg", ///
	replace width(2400) height(1350) quality(100)

restore

	
********************************************************************************
* 1.3.2 : All ages
********************************************************************************

* Prepare validation data
use year dwt valid_employed valid_student valid_inactive dgn ///
	valid_retired using ///
	"$dir_data/ukhls_validation_full_sample.dta", ///
	clear
		
drop if valid_employed == 1 
drop valid_employed
		
collapse (mean) valid_student valid_inactive valid_retired ///
	[aw = dwt], by(year dgn)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_employed sim_student sim_inactive sim_retired dgn using ///
	"$dir_data/simulated_data_full.dta", clear

drop if sim_employed == 1 
drop sim_employed	
	
gen dgn_coded = .
replace dgn_coded = 1 if dgn == "Male"
replace dgn_coded = 0 if dgn == "Female"

drop dgn
rename dgn_coded dgn	
	
collapse (mean) sim_student sim_inactive sim_retired, ///
	by(run year dgn)
	
collapse (mean) sim_student sim_inactive sim_retired ///
		 (sd) sim_student_sd = sim_student ///
		 sim_inactive_sd = sim_inactive ///
		 sim_retired_sd = sim_retired ///
		 , by(year dgn)
		 
foreach varname in  sim_student sim_inactive sim_retired {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

	}

merge 1:1 year dgn using "$dir_data/temp_valid_stats.dta", keep(3) nogen
 
** All  
 
preserve

collapse (mean) sim* valid*, by(year)

* Plot figure 
twoway ///
(rarea sim_student_high sim_student_low year, sort color(blue%20) ///
	legend(label(1 "Students, simulated"))) ///
(line valid_student year, sort color(blue) ///
	legend(label(2 "Students, UKHLS"))) ///
(rarea sim_inactive_high sim_inactive_low year, sort color(red%20) ///
	legend(label(3 "Non-employed, simulated"))) ///
(line valid_inactive year, sort color(red) ///
	legend(label(4 "Non-employed, UKHLS"))) ///
(rarea sim_retired_high sim_retired_low year, sort color(grey%20) ///
	legend(label(5 "Retired, simulated"))) ///
(line valid_retired year, sort color(grey) ///
	legend(label(6 "Retired, UKHLS"))), ///
	title("Economic Activity of the Non-Employed") ///
	subtitle("All ages") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. ", ///
	size(vsmall))

* Save figure
graph export ///
"$dir_output_files/economic_activity/validation_${country}_activity_status_not_employed_ts_all_both.jpg", ///
	replace width(2400) height(1350) quality(100) 	
	
restore, preserve

	
** Males 

keep if dgn == 1 

* Plot figure 
twoway ///
(rarea sim_student_high sim_student_low year, sort color(blue%20) ///
	legend(label(1 "Students, simulated"))) ///
(line valid_student year, sort color(blue) ///
	legend(label(2 "Students, UKHLS"))) ///
(rarea sim_inactive_high sim_inactive_low year, sort color(red%20) ///
	legend(label(3 "Non-employed, simulated"))) ///
(line valid_inactive year, sort color(red) ///
	legend(label(4 "Non-employed, UKHLS"))) ///
(rarea sim_retired_high sim_retired_low year, sort color(grey%20) ///
	legend(label(5 "Retired, simulated"))) ///
(line valid_retired year, sort color(grey) ///
	legend(label(6 "Retired, UKHLS"))), ///
	title("Economic Activity of the Non-Employed") ///
	subtitle("All ages, males") /// 
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. ", ///
	size(vsmall))

* Save figure
graph export ///
"$dir_output_files/economic_activity/validation_${country}_activity_status_not_employed_ts_all_male.jpg", ///
	replace width(2400) height(1350) quality(100)


restore, preserve

	
** Females 

keep if dgn == 0 

* Plot figure 
twoway ///
(rarea sim_student_high sim_student_low year, sort color(blue%20) ///
	legend(label(1 "Students, simulated"))) ///
(line valid_student year, sort color(blue) ///
	legend(label(2 "Students, UKHLS"))) ///
(rarea sim_inactive_high sim_inactive_low year, sort color(red%20) ///
	legend(label(3 "Non-employed, simulated"))) ///
(line valid_inactive year, sort color(red) ///
	legend(label(4 "Non-employed, UKHLS"))) ///
(rarea sim_retired_high sim_retired_low year, sort color(grey%20) ///
	legend(label(5 "Retired, simulated"))) ///
(line valid_retired year, sort color(grey) ///
	legend(label(6 "Retired, UKHLS"))), ///
	title("Economic Activity of the Non-Employed") ///
	subtitle("All ages, females") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. ", ///
	size(vsmall))

* Save figure
graph export ///
"$dir_output_files/economic_activity/validation_${country}_activity_status_not_employed_ts_all_female.jpg", ///
	replace width(2400) height(1350) quality(100)
	
restore 


********************************************************************************
* 1.3.3 : Adult population 17+ 
********************************************************************************

* Prepare validation data
use year dwt valid_employed valid_student valid_inactive dgn dag ///
	valid_retired using ///
	"$dir_data/ukhls_validation_full_sample.dta", ///
	clear
		
* Select sample 
drop if dag < 17
		
drop if valid_employed == 1 
drop valid_employed
		
collapse (mean) valid_student valid_inactive valid_retired ///
	[aw = dwt], by(year dgn)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_employed sim_student sim_inactive sim_retired dgn dag using ///
	"$dir_data/simulated_data_full.dta", clear

* Select sample 
drop if dag < 17
	
drop if sim_employed == 1 
drop sim_employed	
	
gen dgn_coded = .
replace dgn_coded = 1 if dgn == "Male"
replace dgn_coded = 0 if dgn == "Female"

drop dgn
rename dgn_coded dgn	
	
collapse (mean) sim_student sim_inactive sim_retired, ///
	by(run year dgn)
	
collapse (mean) sim_student sim_inactive sim_retired ///
		 (sd) sim_student_sd = sim_student ///
		 sim_inactive_sd = sim_inactive ///
		 sim_retired_sd = sim_retired ///
		 , by(year dgn)
		 
foreach varname in  sim_student sim_inactive sim_retired {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year dgn using "$dir_data/temp_valid_stats.dta", keep(3) nogen
 
** All  
 
preserve

collapse (mean) sim* valid*, by(year)

* Plot figure 
twoway ///
(rarea sim_student_high sim_student_low year, sort color(blue%20) ///
	legend(label(1 "Students, simulated"))) ///
(line valid_student year, sort color(blue) ///
	legend(label(2 "Students, UKHLS"))) ///
(rarea sim_inactive_high sim_inactive_low year, sort color(red%20) ///
	legend(label(3 "Non-employed, simulated"))) ///
(line valid_inactive year, sort color(red) ///
	legend(label(4 "Non-employed, UKHLS"))) ///
(rarea sim_retired_high sim_retired_low year, sort color(grey%20) ///
	legend(label(5 "Retired, simulated"))) ///
(line valid_retired year, sort color(grey) ///
	legend(label(6 "Retired, UKHLS"))), ///
	title("Economic Activity of the Non-Employed") ///
	subtitle("Ages 17+") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. ", ///
	size(vsmall))

* Save figure
graph export ///
"$dir_output_files/economic_activity/validation_${country}_activity_status_not_employed_ts_17plus_both.jpg", ///
	replace width(2400) height(1350) quality(100) 	
	
restore, preserve

	
** Males 

keep if dgn == 1 

* Plot figure 
twoway ///
(rarea sim_student_high sim_student_low year, sort color(blue%20) ///
	legend(label(1 "Students, simulated"))) ///
(line valid_student year, sort color(blue) ///
	legend(label(2 "Students, UKHLS"))) ///
(rarea sim_inactive_high sim_inactive_low year, sort color(red%20) ///
	legend(label(3 "Non-employed, simulated"))) ///
(line valid_inactive year, sort color(red) ///
	legend(label(4 "Non-employed, UKHLS"))) ///
(rarea sim_retired_high sim_retired_low year, sort color(grey%20) ///
	legend(label(5 "Retired, simulated"))) ///
(line valid_retired year, sort color(grey) ///
	legend(label(6 "Retired, UKHLS"))), ///
	title("Economic Activity of the Non-Employed") ///
	subtitle("Ages 17+, males") /// 
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. ", ///
	size(vsmall))

* Save figure
graph export ///
"$dir_output_files/economic_activity/validation_${country}_activity_status_not_employed_ts_17plus_male.jpg", ///
	replace width(2400) height(1350) quality(100)


restore, preserve

	
** Females 

keep if dgn == 0 

* Plot figure 
twoway ///
(rarea sim_student_high sim_student_low year, sort color(blue%20) ///
	legend(label(1 "Students, simulated"))) ///
(line valid_student year, sort color(blue) ///
	legend(label(2 "Students, UKHLS"))) ///
(rarea sim_inactive_high sim_inactive_low year, sort color(red%20) ///
	legend(label(3 "Non-employed, simulated"))) ///
(line valid_inactive year, sort color(red) ///
	legend(label(4 "Non-employed, UKHLS"))) ///
(rarea sim_retired_high sim_retired_low year, sort color(grey%20) ///
	legend(label(5 "Retired, simulated"))) ///
(line valid_retired year, sort color(grey) ///
	legend(label(6 "Retired, UKHLS"))), ///
	title("Economic Activity of the Non-Employed") ///
	subtitle("Ages 17+, females") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. ", ///
	size(vsmall))

* Save figure
graph export ///
"$dir_output_files/economic_activity/validation_${country}_activity_status_not_employed_ts_17plus_female.jpg", ///
	replace width(2400) height(1350) quality(100)
	
restore 


********************************************************************************
* 1.4 Mean values over time -  Share students 
********************************************************************************

********************************************************************************
* 1.4.1 By age group 
********************************************************************************

* Prepare validation data
use year dwt dgn ageGroup valid_student dag using ///
	"$dir_data/ukhls_validation_full_sample.dta", clear

keep if inrange(dag,16,79)
gen student = valid_student

drop if ageGroup == 0 | ageGroup == 8  

collapse (mean) student [aweight=dwt], ///
	by(ageGroup year)
drop if missing(ageGroup)
reshape wide student , i(year) j(ageGroup)

forvalues i = 1(1)7 {
	
	rename student`i' student_`i'_valid

}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year sim_sex ageGroup sim_student using ///
	"$dir_data/simulated_data.dta", clear

gen student = sim_student

collapse (mean) student, by(ageGroup run year)
drop if missing(ageGroup)
reshape wide student, i(year run) j(ageGroup)

forvalues i=1(1)7{
	
	rename student`i' student_`i'_sim

}

collapse (mean) student* ///
	(sd) sd_student_1_sim =student_1_sim ///
		 sd_student_2_sim = student_2_sim ///
		 sd_student_3_sim = student_3_sim ///
	     sd_student_4_sim = student_4_sim ///
		 sd_student_5_sim = student_5_sim ///
		 sd_student_6_sim = student_6_sim ///
		 sd_student_7_sim = student_7_sim ///
		 , by(year)
		 
		 /* sd_student_8_sim=student_8_sim ///
		 sd_employed_f_8_sim=employed_f_8_sim ///
		 sd_employed_m_8_sim=employed_m_8_sim /// */

forvalues i = 1(1)7 {
	gen student_`i'_sim_high = student_`i'_sim + 1.96*sd_student_`i'_sim
	gen student_`i'_sim_low = student_`i'_sim - 1.96*sd_student_`i'_sim
}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figures
	
twoway (rarea student_1_sim_high student_1_sim_low year, ///
	sort color(blue%20) ///
	legend(label(1 "Simulated") position(6) rows(1))) ///
	(line student_1_valid year, sort color(blue) ///
	legend(label(2 "UKHLS"))), ///
	title("Age 16-19") ///
	name(student_1, replace) ///
	ylabel(0 [0.4] 0.8) ///
	xtitle("") ///
	graphregion(color(white)) 
	
twoway (rarea student_2_sim_high student_2_sim_low year, ///
	sort color(blue%20) ///
	legend(label(1 "Simulated") position(6) rows(1))) ///
	(line student_2_valid year, sort color(blue) ///
	legend(label(2 "UKHLS"))), ///
	title("Age 20-24") ///
	name(student_2, replace) ///
	ylabel(0 [0.4] 0.8) ///
	xtitle("") ///
	graphregion(color(white)) 

	
twoway (rarea student_3_sim_high student_3_sim_low year, ///
	sort color(blue%20) ///
	legend(label(1 "Simulated") position(6) rows(1))) ///
	(line student_3_valid year, sort color(blue) ///
	legend(label(2 "UKHLS"))), ///
	title("Age 25-29") ///
	name(student_3, replace) ///
	ylabel(0 [0.4] 0.8) ///
	xtitle("") ///
	graphregion(color(white)) 
	
twoway (rarea student_4_sim_high student_4_sim_low year, ///
	sort color(blue%20) ///
	legend(label(1 "Simulated") position(6) rows(1))) ///
	(line student_4_valid year, sort color(blue) ///
	legend(label(2 "UKHLS"))), ///
	title("Age 30-34") ///
	name(student_4, replace) ///
	ylabel(0 [0.4] 0.8) ///
	xtitle("") ///
	graphregion(color(white)) 
	
twoway (rarea student_5_sim_high student_5_sim_low year, ///
	sort color(blue%20) ///
	legend(label(1 "Simulated") position(6) rows(1))) ///
	(line student_5_valid year, sort color(blue) ///
	legend(label(2 "UKHLS"))), ///
	title("Age 35-39") ///
	name(student_5, replace) ///
	ylabel(0 [0.4] 0.8) ///
	xtitle("") ///
	graphregion(color(white)) 
	
twoway (rarea student_6_sim_high student_6_sim_low year, ///
	sort color(blue%20) ///
	legend(label(1 "Simulated") position(6) ///
	rows(1)))(line student_6_valid year, sort color(blue) ///
	legend(label(2 "UKHLS"))), ///
	title("Age 40-59") ///
	name(student_6, replace) ///
	ylabel(0 [0.4] 0.8) ///
	xtitle("") ///
	graphregion(color(white)) 
	
twoway (rarea student_7_sim_high student_7_sim_low year, ///
	sort color(blue%20) ///
	legend(label(1 "Simulated") position(6) rows(1))) ///
	(line student_7_valid year, sort color(blue) ///
	legend(label(2 "UKHLS"))), ///
	title("Age 60-79") ///
	name(student_7, replace) ///
	ylabel(0 [0.4] 0.8) ///
	xtitle("") ///
	graphregion(color(white)) 
	

* Save figures

* Share students
grc1leg student_1 student_2 student_3 student_4 student_5 student_6 ///
	student_7 , ///
	title("Share of Students by Age Group") ///
	legendfrom(student_1) ///
	graphregion(color(white)) ///
	note("Notes: ", size(vsmall))
	
graph export ///
"$dir_output_files/economic_activity/validation_${country}_students_ts_all_both.jpg", ///
	replace width(2400) height(1350) quality(100)
	
	
twoway (rarea student_1_sim_high student_1_sim_low year, ///
	sort color(blue%20) ///
	legend(label(1 "Simulated") position(6) rows(1))) ///
	(line student_1_valid year, sort color(blue) ///
	legend(label(2 "UKHLS"))), ///
	title("Age 16-19") ///
	name(student_1a, replace) ///
	ylabel(0.4 [0.1] 0.75) ///
	xtitle("") ///
	graphregion(color(white)) 
	
twoway (rarea student_2_sim_high student_2_sim_low year, ///
	sort color(blue%20) ///
	legend(label(1 "Simulated") position(6) rows(1))) ///
	(line student_2_valid year, sort color(blue) ///
	legend(label(2 "UKHLS"))), ///
	title("Age 20-24") ///
	name(student_2a, replace) ///
	ylabel(0.1 [0.05] 0.3) ///
	xtitle("") ///
	graphregion(color(white)) 
	
twoway (rarea student_3_sim_high student_3_sim_low year, ///
	sort color(blue%20) ///
	legend(label(1 "Simulated") position(6) rows(1))) ///
	(line student_3_valid year, sort color(blue) ///
	legend(label(2 "UKHLS"))), ///
	title("Age 25-29") ///
	name(student_3a, replace) ///
	ylabel(0 [0.025] 0.1) ///
	xtitle("") ///
	graphregion(color(white)) 
	
grc1leg student_1a student_2a student_3a , ///
	title("Share of Students by Age Group") ///
	legendfrom(student_1a) ///
	graphregion(color(white)) ///
	note("Notes: ", size(vsmall))
	
graph export ///
	"$dir_output_files/economic_activity/validation_${country}_students_ts_15_29_both.jpg", ///
	replace width(2400) height(1350) quality(100)
	
graph drop _all 	


