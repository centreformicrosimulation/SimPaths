********************************************************************************
* PROJECT:  		SimPaths UK 
* SECTION:			Validation
* OBJECT: 			Children
* AUTHORS:			Ashley Burdett 
* LAST UPDATE:		Jan 2026
* COUNTRY: 			UK 
********************************************************************************
* NOTES: 			This do file plots simulated and UKHLS % of benefit units
*			 		with a given number of children 
********************************************************************************

********************************************************************************
* 1 : Mean values over time
********************************************************************************

********************************************************************************
* 1.1 : Mean values over time, working age (18-65), children < 18
********************************************************************************

* Prepare validation data
use year demAge idPers idBu dwt children_* using ///
	"$dir_data/ukhls_validation_sample.dta", clear
	
keep if inrange(demAge,18,65)

* Calculate weighted share of benefit units with 0, 1, 2, 3 or more children
collapse (mean) children_* [aw = dwt], by(year)

foreach varname in children_0 children_1 children_2 children_3p  {
	
	rename `varname' valid_`varname'
	
}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year demAge idBu children_* using ///
	"$dir_data/simulation_sample.dta", clear

keep if inrange(demAge,18,65)
	
collapse (mean) children_*, by(run year)

rename children_3plus children_3p

collapse (mean) children_* ///
		 (sd) children_0_sd = children_0 ///
			  children_1_sd = children_1 ///
			  children_2_sd = children_2 ///
			  children_3p_sd = children_3p ///
		 , by(year)
		 
foreach varname in children_0 children_1 children_2 children_3p  {
	
	gen sim_`varname'_h = `varname' + 1.96*`varname'_sd
	gen sim_`varname'_l = `varname' - 1.96*`varname'_sd
	rename `varname' sim_`varname'

}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figures
label var sim_children_0 "No children"
label var sim_children_1 "1 child"
label var sim_children_2 "2 children"
label var sim_children_3p "3+ children"


twoway (rarea sim_children_0_h sim_children_0_l year, ///
	sort color(green%20) legend(label(1 "No children, SimPaths"))) ///
(line valid_children_0 year, sort color(green) ///
	legend(label(2 "No children, UKHLS"))) ///
	(rarea sim_children_1_h sim_children_1_l year, sort color(blue%20) ///
	legend(label(3 "1 child, SimPaths"))) ///
(line valid_children_1 year, sort color(blue) ///
	legend(label(4 "1 child, UKHLS"))) ///
(rarea sim_children_2_h sim_children_2_l year, sort color(red%20) ///
	legend(label(5 "2 children, SimPaths"))) ///
(line valid_children_2 year, sort color(red) ///
	legend(label(6 "2 children, UKHLS"))) ///
(rarea sim_children_3p_h sim_children_3p_l year, sort color(grey%20) ///
	legend(label(7 "3+ children, SimPaths"))) ///
(line valid_children_3p year, sort color(grey) ///
	legend(label(8 "3+ children, UKHLS"))), ///
	title("Number of Children") ///
	subtitle("Ages 18-65") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: Individual observations plotted.", size(vsmall))

* Save figure
graph export "$dir_output_files/children/validation_${country}_children_ts_18_65_both.jpg", ///
	replace width(2400) height(1350) quality(100)

	
********************************************************************************
* 1.2 : Mean values over time, working age (18-65), children < 18, by gender
********************************************************************************

* Prepare validation data
use year demAge idPers idBu dwt children_* demMaleFlag using ///
	"$dir_data/ukhls_validation_sample.dta", clear
	
keep if inrange(demAge,18,65)

* Calculate weighted share of benefit units with 0, 1, 2, 3 or more children
collapse (mean) children_* [aw = dwt], by(year demMaleFlag)

foreach varname in children_0 children_1 children_2 children_3p  {
	
	rename `varname' valid_`varname'
	
}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year demAge idBu children_*  demMaleFlag using ///
	"$dir_data/simulation_sample.dta", clear

keep if inrange(demAge,18,65)
	
collapse (mean) children_*, by(run year demMaleFlag) 

rename children_3plus children_3p

collapse (mean) children_* ///
		 (sd) children_0_sd = children_0 ///
			  children_1_sd = children_1 ///
			  children_2_sd = children_2 ///
			  children_3p_sd = children_3p ///
		 , by(year demMaleFlag)
		 
foreach varname in children_0 children_1 children_2 children_3p  {
	
	gen sim_`varname'_h = `varname' + 1.96*`varname'_sd
	gen sim_`varname'_l = `varname' - 1.96*`varname'_sd
	rename `varname' sim_`varname'

}

merge 1:1 year demMaleFlag using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figures
label var sim_children_0 "No children"
label var sim_children_1 "1 child"
label var sim_children_2 "2 children"
label var sim_children_3p "3+ children"

* Males 

preserve 

keep if demMaleFlag == 1

twoway (rarea sim_children_0_h sim_children_0_l year, ///
	sort color(green%20) legend(label(1 "No children, SimPaths"))) ///
(line valid_children_0 year, sort color(green) ///
	legend(label(2 "No children, UKHLS"))) ///
	(rarea sim_children_1_h sim_children_1_l year, sort color(blue%20) ///
	legend(label(3 "1 child, SimPaths"))) ///
(line valid_children_1 year, sort color(blue) ///
	legend(label(4 "1 child, UKHLS"))) ///
(rarea sim_children_2_h sim_children_2_l year, sort color(red%20) ///
	legend(label(5 "2 children, SimPaths"))) ///
(line valid_children_2 year, sort color(red) ///
	legend(label(6 "2 children, UKHLS"))) ///
(rarea sim_children_3p_h sim_children_3p_l year, sort color(grey%20) ///
	legend(label(7 "3+ children, SimPaths"))) ///
(line valid_children_3p year, sort color(grey) ///
	legend(label(8 "3+ children, UKHLS"))), ///
	title("Number of Children") ///
	subtitle("Ages 18-65, males") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: Individual observations plotted.", size(vsmall))

* Save figure
graph export "$dir_output_files/children/validation_${country}_children_ts_18_65_male.jpg", ///
	replace width(2400) height(1350) quality(100)	
	
restore 	
	
* Females

keep if demMaleFlag== 0

twoway (rarea sim_children_0_h sim_children_0_l year, ///
	sort color(green%20) legend(label(1 "No children, SimPaths"))) ///
(line valid_children_0 year, sort color(green) ///
	legend(label(2 "No children, UKHLS"))) ///
	(rarea sim_children_1_h sim_children_1_l year, sort color(blue%20) ///
	legend(label(3 "1 child, SimPaths"))) ///
(line valid_children_1 year, sort color(blue) ///
	legend(label(4 "1 child, UKHLS"))) ///
(rarea sim_children_2_h sim_children_2_l year, sort color(red%20) ///
	legend(label(5 "2 children, SimPaths"))) ///
(line valid_children_2 year, sort color(red) ///
	legend(label(6 "2 children, UKHLS"))) ///
(rarea sim_children_3p_h sim_children_3p_l year, sort color(grey%20) ///
	legend(label(7 "3+ children, SimPaths"))) ///
(line valid_children_3p year, sort color(grey) ///
	legend(label(8 "3+ children, UKHLS"))), ///
	title("Number of Children") ///
	subtitle("Ages 18-65, females") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: Individual observations plotted.", size(vsmall))

* Save figure
graph export "$dir_output_files/children/validation_${country}_children_ts_18_65_female.jpg", ///
	replace width(2400) height(1350) quality(100)		
	
	
********************************************************************************
* 1.3 : Mean values over time, working age (18-65), children < 3
********************************************************************************

* Prepare validation data
use year demAge idBu dwt demNChild0to2 using ///
	"$dir_data/ukhls_validation_sample.dta", clear

keep if inrange(demAge,18,65)	
		
gen child02 = . 
replace child02 = 0 if demNChild0to2 == 0 
replace child02 = 1 if demNChild0to2 > 0 & demNChild0to2 != . 

* Calculate weighted share of benefit units with 0, 1, 2, 3 or more children
collapse (mean) child02 [aw = dwt], by(year)

foreach varname in child02  {
	
	rename `varname' valid_`varname'

}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year demAge idBu sim_demNChild0to2 using ///
	"$dir_data/simulation_sample.dta", clear
	
gen sim_child02 = . 
replace sim_child02 = 0 if sim_demNChild0to2 == 0 
replace sim_child02 = 1 if sim_demNChild0to2 > 0 & sim_demNChild0to2 != . 
	
collapse (mean) sim_child02, by(run year)

collapse (mean) sim_child02 ///
		 (sd) sim_child02_sd = sim_child02 ///
		 , by(year)
		 
foreach varname in sim_child02  {
	
	gen `varname'_h = `varname' + 1.96*`varname'_sd
	gen `varname'_l = `varname' - 1.96*`varname'_sd
	rename `varname' sim_`varname'

}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen


* Plot figures
twoway (rarea sim_child02_h sim_child02_l year, ///
	sort color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_child02 year, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	title("Share With Child 0-2 Years Old") ///
	subtitle("Ages 18-65") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes:", size(vsmall)) 
	
* Save figure
graph export "$dir_output_files/children/validation_${country}_young_child_ts_18_65_both.jpg", ///
	replace width(2400) height(1350) quality(100)

graph drop _all 


********************************************************************************
* 1.4 : Mean values over time, working age (18-65), children < 3, by gender
********************************************************************************

* Prepare validation data
use year demAge idBu dwt demNChild0to2 demMaleFlag using ///
	"$dir_data/ukhls_validation_sample.dta", clear

keep if inrange(demAge,18,65)	
	
gen child02 = . 
replace child02 = 0 if demNChild0to2 == 0 
replace child02 = 1 if demNChild0to2 > 0 & demNChild0to2 != . 

* Calculate weighted share of benefit units with 0, 1, 2, 3 or more children
collapse (mean) child02 [aw = dwt], by(year demMaleFlag)

foreach varname in child02  {
	
	rename `varname' valid_`varname'

}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year demAge idBu sim_demNChild0to2 demMaleFlag using ///
	"$dir_data/simulation_sample.dta", clear
	
gen sim_child02 = . 
replace sim_child02 = 0 if sim_demNChild0to2 == 0 
replace sim_child02 = 1 if sim_demNChild0to2 > 0 & sim_demNChild0to2 != . 
	
collapse (mean) sim_child02, by(run year demMaleFlag)

collapse (mean) sim_child02 ///
		 (sd) sim_child02_sd = sim_child02 ///
		 , by(year demMaleFlag)
		 
foreach varname in sim_child02  {
	
	gen `varname'_h = `varname' + 1.96*`varname'_sd
	gen `varname'_l = `varname' - 1.96*`varname'_sd
	rename `varname' sim_`varname'

}

merge 1:1 year demMaleFlag using "$dir_data/temp_valid_stats.dta", keep(3) nogen


* Plot figures

* Males
preserve 

keep if demMaleFlag == 1

twoway (rarea sim_child02_h sim_child02_l year, ///
	sort color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_child02 year, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	title("Share With Child 0-2 Years Old") ///
	subtitle("Ages 18-65, males") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: ", size(vsmall)) 
	
* Save figure
graph export "$dir_output_files/children/validation_${country}_young_child_ts_18_65_male.jpg", ///
	replace width(2400) height(1350) quality(100)

restore	

* Females
keep if demMaleFlag == 0

twoway (rarea sim_child02_h sim_child02_l year, ///
	sort color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_child02 year, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	title("Share With Child 0-2 Years Old") ///
	subtitle("Ages 18-65, females") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: ", size(vsmall)) 
	
* Save figure
graph export "$dir_output_files/children/validation_${country}_young_child_ts_18_65_female.jpg", ///
	replace width(2400) height(1350) quality(100)
	
	
graph drop _all 



********************************************************************************
* 1.5 : Mean values over time, working age (18-65), new born child
********************************************************************************

* Prepare validation data
use year demAge idBu dwt demNChild0 using ///
	"$dir_data/ukhls_validation_sample.dta", clear

keep if inrange(demAge,18,65)	
		
gen child0 = . 
replace child0 = 0 if demNChild0 == 0 
replace child0 = 1 if demNChild0 > 0 & demNChild0 != . 

* Calculate weighted share of benefit units with 0, 1, 2, 3 or more children
collapse (mean) child0 [aw = dwt], by(year)

foreach varname in child0  {
	
	rename `varname' valid_`varname'

}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year demAge idBu sim_demNChild0 using ///
	"$dir_data/simulation_sample.dta", clear
	
gen sim_child0 = . 
replace sim_child0 = 0 if sim_demNChild0 == 0 
replace sim_child0 = 1 if sim_demNChild0 > 0 & sim_demNChild0 != . 
	
collapse (mean) sim_child0, by(run year)

collapse (mean) sim_child0 ///
		 (sd) sim_child0_sd = sim_child0 ///
		 , by(year)
		 
foreach varname in sim_child0  {
	
	gen `varname'_h = `varname' + 1.96*`varname'_sd
	gen `varname'_l = `varname' - 1.96*`varname'_sd
	rename `varname' sim_`varname'

}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen


* Plot figures
twoway (rarea sim_child0_h sim_child0_l year, ///
	sort color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_child0 year, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	title("Share With New Born Child") ///
	subtitle("Ages 18-65") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: COnstructed from benefit unit information.", size(vsmall)) 
	
* Save figure
graph export "$dir_output_files/children/validation_${country}_new_born_child_ts_18_65_both.jpg", ///
	replace width(2400) height(1350) quality(100)

graph drop _all 


********************************************************************************
* 1.6 : Mean values over time, working age (18-65), new born child, by gender
********************************************************************************

* Prepare validation data
use year demAge idBu dwt demNChild0 demMaleFlag using ///
	"$dir_data/ukhls_validation_sample.dta", clear

keep if inrange(demAge,18,65)	
	
gen child0 = . 
replace child0 = 0 if demNChild0 == 0 
replace child0 = 1 if demNChild0 > 0 & demNChild0 != . 

* Calculate weighted share of benefit units 
collapse (mean) child0 [aw = dwt], by(year demMaleFlag)

foreach varname in child0  {
	
	rename `varname' valid_`varname'

}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year demAge idBu sim_demNChild0 demMaleFlag using ///
	"$dir_data/simulation_sample.dta", clear
	
gen sim_child0 = . 
replace sim_child0 = 0 if sim_demNChild0 == 0 
replace sim_child0 = 1 if sim_demNChild0 > 0 & sim_demNChild0 != . 
	
collapse (mean) sim_child0, by(run year demMaleFlag)

collapse (mean) sim_child0 ///
		 (sd) sim_child0_sd = sim_child0 ///
		 , by(year demMaleFlag)
		 
foreach varname in sim_child0  {
	
	gen `varname'_h = `varname' + 1.96*`varname'_sd
	gen `varname'_l = `varname' - 1.96*`varname'_sd
	rename `varname' sim_`varname'

}

merge 1:1 year demMaleFlag using "$dir_data/temp_valid_stats.dta", keep(3) nogen


* Plot figures

* Males
preserve 

keep if demMaleFlag == 1

twoway (rarea sim_child0_h sim_child0_l year, ///
	sort color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_child0 year, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	title("Share With New Born Child") ///
	subtitle("Ages 18-65, males") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: ", size(vsmall)) 
	
* Save figure
graph export "$dir_output_files/children/validation_${country}_new_born_child_ts_18_65_male.jpg", ///
	replace width(2400) height(1350) quality(100)

restore	

* Females
keep if demMaleFlag == 0

twoway (rarea sim_child0_h sim_child0_l year, ///
	sort color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_child0 year, sort color(green) ///
	legend(label(2 "UKHLS"))), ///
	title("Share With New Born Child") ///
	subtitle("Ages 18-65, females") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: ", size(vsmall)) 
	
* Save figure
graph export "$dir_output_files/children/validation_${country}_new_bron_child_ts_18_65_female.jpg", ///
	replace width(2400) height(1350) quality(100)
	
	
graph drop _all 
