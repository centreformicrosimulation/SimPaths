********************************************************************************
* PROJECT:  		ESPON 
* SECTION:			Validation
* OBJECT: 			Children
* AUTHORS:			Ashley Burdett 
* LAST UPDATE:		06/2025 (AB)
* COUNTRY: 			UK 

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
use year idbenefitunit dwt children_* using ///
	"$dir_data/ukhls_validation_sample.dta", clear
	
bys year idbenefitunit: keep if _n == 1

* Calculate weighted share of benefit units with 0, 1, 2, 3 or more children
collapse (mean) children_* [aw = dwt], by(year)

foreach varname in children_0 children_1 children_2 children_3p  {
	
	rename `varname' valid_`varname'
	
}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year idbenefitunit children_*  using ///
	"$dir_data/simulated_data.dta", clear

bys run year idbenefitunit: keep if _n == 1

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
	sort color(green%20) legend(label(1 "No children, simulated"))) ///
(line valid_children_0 year, sort color(green) ///
	legend(label(2 "No children, UKHLS"))) ///
	(rarea sim_children_1_h sim_children_1_l year, sort color(blue%20) ///
	legend(label(3 "1 child, simulated"))) ///
(line valid_children_1 year, sort color(blue) ///
	legend(label(4 "1 child, UKHLS"))) ///
(rarea sim_children_2_h sim_children_2_l year, sort color(red%20) ///
	legend(label(5 "2 children, simulated"))) ///
(line valid_children_2 year, sort color(red) ///
	legend(label(6 "2 children, UKHLS"))) ///
(rarea sim_children_3p_h sim_children_3p_l year, sort color(grey%20) ///
	legend(label(7 "3+ children, simulated"))) ///
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
	note("Notes: Statistics computed at the benefit unit level.", size(vsmall))

* Save figure
graph export "$dir_output_files/children/validation_${country}_children_ts_18_65_both.jpg", ///
	replace width(2400) height(1350) quality(100)

	
********************************************************************************
* 1.2 : Mean values over time, working age (18-65), children < 3
********************************************************************************

* Prepare validation data
use year idbenefitunit dwt dnc02 using ///
	"$dir_data/ukhls_validation_sample.dta", clear

gen child02 = . 
replace child02 = 0 if dnc02 == 0 
replace child02 = 1 if dnc02 > 0 & dnc02 != . 

bys year idbenefitunit: keep if _n == 1

* Calculate weighted share of benefit units with 0, 1, 2, 3 or more children
collapse (mean) child02 [aw = dwt], by(year)

foreach varname in child02  {
	
	rename `varname' valid_`varname'

}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year idbenefitunit sim_dnc02 using ///
	"$dir_data/simulated_data.dta", clear

gen sim_child02 = . 
replace sim_child02 = 0 if sim_dnc02 == 0 
replace sim_child02 = 1 if sim_dnc02 > 0 & sim_dnc02 != . 
	
bys run year idbenefitunit: keep if _n == 1

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
	sort color(green%20) legend(label(1 "Simulated"))) ///
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
	note("Notes: Statistics computed at the benefit unit level.", size(vsmall)) 
	
* Save figure
graph export "$dir_output_files/children/validation_${country}_young_child_ts_18_65_both.jpg", ///
	replace width(2400) height(1350) quality(100)

graph drop _all 
