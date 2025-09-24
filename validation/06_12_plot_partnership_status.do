********************************************************************************
* PROJECT:  		ESPON 
* SECTION:			Validation
* OBJECT: 			Partnership
* AUTHORS:			Ashley Burdett 
* LAST UPDATE:		9/25 (AB)
* COUNTRY: 			UK 

* NOTES: 			
********************************************************************************

********************************************************************************
* 1 : Mean values over time
********************************************************************************

********************************************************************************
* 1.1 : Mean values over time - ages 18-65
********************************************************************************

* Prepare validation data
use year dwt valid_dcpst_p valid_dcpst_snm valid_dcpst_prvp ///
	valid_dcpst_snmprvp using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Compute shares	
collapse (mean) valid_dcpst_p valid_dcpst_snm valid_dcpst_prvp ///
	valid_dcpst_snmprvp [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year sim_dcpst_p sim_dcpst_snm sim_dcpst_prvp sim_dcpst_snmprvp ///
	sim_has_partner using "$dir_data/simulated_data.dta", clear

* Compute shares and sd
collapse (mean) sim_dcpst_p sim_dcpst_snm sim_dcpst_prvp sim_dcpst_snmprvp  ///
	sim_has_partner, by(run year)
	
collapse (mean) sim_dcpst_p sim_dcpst_snm sim_dcpst_prvp sim_dcpst_snmprvp ///
	sim_has_partner ///
		 (sd) sim_dcpst_p_sd = sim_dcpst_p ///
		 sim_dcpst_snm_sd = sim_dcpst_snm ///
		 sim_dcpst_prvp_sd = sim_dcpst_prvp ///
		 sim_dcpst_snmprvp_sd = sim_dcpst_snmprvp ///
		 sim_has_partner_sd = sim_has_partner ///
		 , by(year)
	
* Approx 95% confidence interval 	
foreach varname in sim_dcpst_p sim_dcpst_snm sim_dcpst_prvp ///
sim_dcpst_snmprvp sim_has_partner {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

* Combine datasets
merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Share partnered

* Plot figure
twoway (rarea sim_dcpst_p_high sim_dcpst_p_low year, sort color(green%20) ///
	legend(label(1 "Simulated"))) ///
(line valid_dcpst_p year, sort color(green) ///
	legend(label(2 "UKHLS "))), ///
	title("Partnered") ///
	subtitle("Ages 18-65") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	xlabel(,labsize(small)) ///
	ylabel(0[0.1]0.7, labsize(small)) ///
	legend(size(small)) ///	
	graphregion(color(white)) ///
	note("Notes: ", size(vsmall))	

* Save figure
graph export ///
"$dir_output_files/partnership/validation_${country}_partnered_ts_${min_age}_${max_age}_both.jpg", ///
	replace width(2400) height(1350) quality(100)

	
* Partnership status shares	

twoway (rarea sim_dcpst_p_high sim_dcpst_p_low year, sort color(green%20) ///
	legend(label(1 "Partnered, simulated"))) ///
(line valid_dcpst_p year, sort color(green) ///
	legend(label(2 "Partnered, UKHLS "))) ///
(rarea sim_dcpst_snm_high sim_dcpst_snm_low year, sort color(red%20) ///
	legend(label(3 "Single, simulated"))) ///
(line valid_dcpst_snm year, sort color(red) ///
	legend(label(4 "Single, UKHLS "))) ///
(rarea sim_dcpst_prvp_high sim_dcpst_prvp_low year, sort color(blue%20) ///
	legend(label(5 "Prev partnered, simulated"))) ///
(line valid_dcpst_prvp year, sort color(blue) ///
	legend(label(6 "Prev partnered, UKHLS "))) , ///
	title("Partnership status") ///
	subtitle("Ages 18-65") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	xlabel(,labsize(small)) ///
	ylabel(0[0.1]0.7, labsize(small)) ///
	legend(size(small)) ///	
	graphregion(color(white)) ///
	note("Notes: ", size(vsmall))	

graph export ///
"$dir_output_files/partnership/validation_${country}_partnership_ts_${min_age}_${max_age}_both.jpg", ///
	replace width(2400) height(1350) quality(100)	
	
	
********************************************************************************
* 1.2 : Mean values over time - by age group 
********************************************************************************	
* Those in their 20s 	

* Validation data	
use year dwt valid_dcpst_p valid_dcpst_snm valid_dcpst_prvp ///
	valid_dcpst_snmprvp ageGroup using ///
	"$dir_data/ukhls_validation_sample.dta", clear

keep if ageGroup == 2 | ageGroup == 3	
	
* Compute shares 	
collapse (mean) valid_dcpst_p valid_dcpst_snm valid_dcpst_prvp ///
	valid_dcpst_snmprvp [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year sim_dcpst_p sim_dcpst_snm sim_dcpst_prvp sim_dcpst_snmprvp ///
	sim_has_partner ageGroup using "$dir_data/simulated_data.dta", clear

keep if ageGroup == 2 | ageGroup == 3 	
	
* Compute shares snd sd 	
collapse (mean) sim_dcpst_p sim_dcpst_snm sim_dcpst_prvp sim_dcpst_snmprvp  ///
	sim_has_partner, by(run year)
	
collapse (mean) sim_dcpst_p sim_dcpst_snm sim_dcpst_prvp sim_dcpst_snmprvp ///
	sim_has_partner ///
		 (sd) sim_dcpst_p_sd = sim_dcpst_p ///
		 sim_dcpst_snm_sd = sim_dcpst_snm ///
		 sim_dcpst_prvp_sd = sim_dcpst_prvp ///
		 sim_dcpst_snmprvp_sd = sim_dcpst_snmprvp ///
		 sim_has_partner_sd = sim_has_partner ///
		 , by(year)
		
* Approx 95% confidence interval 		
foreach varname in sim_dcpst_p sim_dcpst_snm sim_dcpst_prvp ///
sim_dcpst_snmprvp sim_has_partner {

	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

* Combine datasets
merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea sim_dcpst_p_high sim_dcpst_p_low year, sort color(green%20) ///
	legend(label(1 "Partnered, simulated"))) ///
(line valid_dcpst_p year, sort color(green) ///
	legend(label(2 "Partnered, UKHLS "))) ///
(rarea sim_dcpst_snm_high sim_dcpst_snm_low year, sort color(red%20) ///
	legend(label(3 "Single, simulated"))) ///
(line valid_dcpst_snm year, sort color(red) ///
	legend(label(4 "Single, UKHLS "))) ///
(rarea sim_dcpst_prvp_high sim_dcpst_prvp_low year, sort color(blue%20) ///
	legend(label(5 "Prev partnered, simulated"))) ///
(line valid_dcpst_prvp year, sort color(blue) ///
	legend(label(6 "Prev partnered, UKHLS "))) , ///
	title("Partnership status") ///
	subtitle("Ages 20-29") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	xlabel(,labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///	
	graphregion(color(white)) ///
	note("Notes: ", size(vsmall))	
	
graph export ///
"$dir_output_files/partnership/validation_${country}_partnership_ts_20_29_both.jpg", ///
	replace width(2400) height(1350) quality(100)	
	
	
* Those in their 30s 	
	
* Validation data 	
use year dwt valid_dcpst_p valid_dcpst_snm valid_dcpst_prvp ///
	valid_dcpst_snmprvp ageGroup using ///
	"$dir_data/ukhls_validation_sample.dta", clear

keep if ageGroup == 4 | ageGroup == 5	
	
* Compute shares	
collapse (mean) valid_dcpst_p valid_dcpst_snm valid_dcpst_prvp ///
	valid_dcpst_snmprvp [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year sim_dcpst_p sim_dcpst_snm sim_dcpst_prvp sim_dcpst_snmprvp ///
	sim_has_partner ageGroup using "$dir_data/simulated_data.dta", clear

keep if ageGroup == 4 | ageGroup == 5 	
	
* Compute shares and sd	
collapse (mean) sim_dcpst_p sim_dcpst_snm sim_dcpst_prvp sim_dcpst_snmprvp  ///
	sim_has_partner, by(run year)
	
collapse (mean) sim_dcpst_p sim_dcpst_snm sim_dcpst_prvp sim_dcpst_snmprvp ///
	sim_has_partner ///
		 (sd) sim_dcpst_p_sd = sim_dcpst_p ///
		 sim_dcpst_snm_sd = sim_dcpst_snm ///
		 sim_dcpst_prvp_sd = sim_dcpst_prvp ///
		 sim_dcpst_snmprvp_sd = sim_dcpst_snmprvp ///
		 sim_has_partner_sd = sim_has_partner ///
		 , by(year)
	
* APprox 95% confidence interval 	
foreach varname in sim_dcpst_p sim_dcpst_snm sim_dcpst_prvp ///
sim_dcpst_snmprvp sim_has_partner {

	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

* COmbien datasets
merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea sim_dcpst_p_high sim_dcpst_p_low year, sort color(green%20) ///
	legend(label(1 "Partnered, simulated"))) ///
(line valid_dcpst_p year, sort color(green) ///
	legend(label(2 "Partnered, UKHLS "))) ///
(rarea sim_dcpst_snm_high sim_dcpst_snm_low year, sort color(red%20) ///
	legend(label(3 "Single, simulated"))) ///
(line valid_dcpst_snm year, sort color(red) ///
	legend(label(4 "Single, UKHLS "))) ///
(rarea sim_dcpst_prvp_high sim_dcpst_prvp_low year, sort color(blue%20) ///
	legend(label(5 "Prev partnered, simulated"))) ///
(line valid_dcpst_prvp year, sort color(blue) ///
	legend(label(6 "Prev partnered, UKHLS "))) , ///
	title("Partnership status") ///
	subtitle("Ages 30-39") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	xlabel(,labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///	
	graphregion(color(white)) ///
	note("Notes: ", size(vsmall))	

	
graph export ///
"$dir_output_files/partnership/validation_${country}_partnership_ts_30_39_both.jpg", ///
	replace width(2400) height(1350) quality(100)	
	
	
* Those in their 40-59 	
	
* Validation data 	
use year dwt valid_dcpst_p valid_dcpst_snm valid_dcpst_prvp ///
	valid_dcpst_snmprvp ageGroup using ///
	"$dir_data/ukhls_validation_sample.dta", clear

keep if ageGroup == 6
	
* Compute shares	
collapse (mean) valid_dcpst_p valid_dcpst_snm valid_dcpst_prvp ///
	valid_dcpst_snmprvp [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year sim_dcpst_p sim_dcpst_snm sim_dcpst_prvp sim_dcpst_snmprvp ///
	sim_has_partner ageGroup using "$dir_data/simulated_data.dta", clear

keep if ageGroup == 6

* Compute shares and sd	
collapse (mean) sim_dcpst_p sim_dcpst_snm sim_dcpst_prvp sim_dcpst_snmprvp  ///
	sim_has_partner, by(run year)
	
collapse (mean) sim_dcpst_p sim_dcpst_snm sim_dcpst_prvp sim_dcpst_snmprvp ///
	sim_has_partner ///
		 (sd) sim_dcpst_p_sd = sim_dcpst_p ///
		 sim_dcpst_snm_sd = sim_dcpst_snm ///
		 sim_dcpst_prvp_sd = sim_dcpst_prvp ///
		 sim_dcpst_snmprvp_sd = sim_dcpst_snmprvp ///
		 sim_has_partner_sd = sim_has_partner ///
		 , by(year)
	
* Approx 95% confidence interval 	
foreach varname in sim_dcpst_p sim_dcpst_snm sim_dcpst_prvp ///
sim_dcpst_snmprvp sim_has_partner {

	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

* Combine datasets
merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea sim_dcpst_p_high sim_dcpst_p_low year, sort color(green%20) ///
	legend(label(1 "Partnered, simulated"))) ///
(line valid_dcpst_p year, sort color(green) ///
	legend(label(2 "Partnered, UKHLS "))) ///
(rarea sim_dcpst_snm_high sim_dcpst_snm_low year, sort color(red%20) ///
	legend(label(3 "Single, simulated"))) ///
(line valid_dcpst_snm year, sort color(red) ///
	legend(label(4 "Single, UKHLS "))) ///
(rarea sim_dcpst_prvp_high sim_dcpst_prvp_low year, sort color(blue%20) ///
	legend(label(5 "Prev partnered, simulated"))) ///
(line valid_dcpst_prvp year, sort color(blue) ///
	legend(label(6 "Prev partnered, UKHLS "))) , ///
	title("Partnership status") ///
	subtitle("Ages 40-59") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	xlabel(,labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///	
	graphregion(color(white)) ///
	note("Notes: ", size(vsmall))		
	
graph export ///
"$dir_output_files/partnership/validation_${country}_partnership_ts_40_59_both.jpg", ///
	replace width(2400) height(1350) quality(100)	
	
	
graph drop _all 


********************************************************************************
* 1.3 : Mean values over time - by children 
********************************************************************************	

* Load validation data
use year dwt valid_dcpst_p_children_0 valid_dcpst_p_children_1 ///
	valid_dcpst_p_children_2 valid_dcpst_p_children_3p ///
	valid_dcpst_snm_children_0 valid_dcpst_snm_children_1 ///
	valid_dcpst_snm_children_2 valid_dcpst_snm_children_3p ///
	valid_dcpst_prvp_children_0 valid_dcpst_prvp_children_1 ///
	valid_dcpst_prvp_children_2 valid_dcpst_prvp_children_3p ///
	valid_dcpst_snmprvp_children_0 valid_dcpst_snmprvp_children_1 ///
	valid_dcpst_snmprvp_children_2 valid_dcpst_snmprvp_children_3p using ///
	"$dir_data/ukhls_validation_sample.dta", clear

collapse (mean) valid_dcpst_p_children_0 valid_dcpst_p_children_1 ///
	 valid_dcpst_p_children_2 valid_dcpst_p_children_3p ///
	 valid_dcpst_snm_children_0 valid_dcpst_snm_children_1 ///
	 valid_dcpst_snm_children_2 valid_dcpst_snm_children_3p ///
	 valid_dcpst_prvp_children_0 valid_dcpst_prvp_children_1 ///
	 valid_dcpst_prvp_children_2 valid_dcpst_prvp_children_3p ///
	 valid_dcpst_snmprvp_children_0 valid_dcpst_snmprvp_children_1 ///
	 valid_dcpst_snmprvp_children_2 valid_dcpst_snmprvp_children_3p ///
	 [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Load simulated data
use run year sim_dcpst_p_children_0 sim_dcpst_p_children_1 ///
	sim_dcpst_p_children_2 sim_dcpst_p_children_3p sim_dcpst_snm_children_0 ///
	sim_dcpst_snm_children_1 sim_dcpst_snm_children_2 ///
	sim_dcpst_snm_children_3p sim_dcpst_prvp_children_0 ///
	sim_dcpst_prvp_children_1 sim_dcpst_prvp_children_2 ///
	sim_dcpst_prvp_children_3p sim_dcpst_snmprvp_children_0 ///
	sim_dcpst_snmprvp_children_1 sim_dcpst_snmprvp_children_2 ///
	sim_dcpst_snmprvp_children_3p ///
	using "$dir_data/simulated_data.dta", clear

* Compute shares and sd	
collapse (mean) sim_dcpst_p_children_0 sim_dcpst_p_children_1 ///
	sim_dcpst_p_children_2 sim_dcpst_p_children_3p ///
	sim_dcpst_snm_children_0 sim_dcpst_snm_children_1 ///
	sim_dcpst_snm_children_2 sim_dcpst_snm_children_3p ///
	sim_dcpst_prvp_children_0 sim_dcpst_prvp_children_1 ///
	sim_dcpst_prvp_children_2 sim_dcpst_prvp_children_3p ///
	sim_dcpst_snmprvp_children_0 sim_dcpst_snmprvp_children_1 ///
	sim_dcpst_snmprvp_children_2 sim_dcpst_snmprvp_children_3p, ///
	by(run year)
	
collapse (mean) sim_dcpst_p_children_0 sim_dcpst_p_children_1 ///
	 sim_dcpst_p_children_2 sim_dcpst_p_children_3p sim_dcpst_snm_children_0 ///
	 sim_dcpst_snm_children_1 sim_dcpst_snm_children_2 ///
	 sim_dcpst_snm_children_3p sim_dcpst_prvp_children_0 ///
	 sim_dcpst_prvp_children_1 sim_dcpst_prvp_children_2 ///
	 sim_dcpst_prvp_children_3p sim_dcpst_snmprvp_children_0 ///
	 sim_dcpst_snmprvp_children_1 sim_dcpst_snmprvp_children_2 ///
	 sim_dcpst_snmprvp_children_3p ///
	(sd) sim_dcpst_p_children_0_sd = sim_dcpst_p_children_0 ///
		sim_dcpst_p_children_1_sd = sim_dcpst_p_children_1 ///
		sim_dcpst_p_children_2_sd = sim_dcpst_p_children_2 ///
		sim_dcpst_p_children_3p_sd = sim_dcpst_p_children_3p ///
		sim_dcpst_snm_children_0_sd = sim_dcpst_snm_children_0 ///
		sim_dcpst_snm_children_1_sd = sim_dcpst_snm_children_1 ///
		sim_dcpst_snm_children_2_sd = sim_dcpst_snm_children_2 ///
		sim_dcpst_snm_children_3p_sd = sim_dcpst_snm_children_3 ///
		sim_dcpst_prvp_children_0_sd = sim_dcpst_prvp_children_0 ///
		sim_dcpst_prvp_children_1_sd = sim_dcpst_prvp_children_1 ///
		sim_dcpst_prvp_children_2_sd = sim_dcpst_prvp_children_2 ///
		sim_dcpst_prvp_children_3p_sd = sim_dcpst_prvp_children_3p ///
		sim_dcpst_snmprvp_children_0_sd = sim_dcpst_snmprvp_children_0 ///
		sim_dcpst_snmprvp_children_1_sd = sim_dcpst_snmprvp_children_1 ///
		sim_dcpst_snmprvp_children_2_sd = sim_dcpst_snmprvp_children_2 ///
		sim_dcpst_snmprvp_children_3p_sd = sim_dcpst_snmprvp_children_3p ///
		, by(year)
		 
* Approx 95% confidence interval 		 
foreach varname in sim_dcpst_p_children_0 sim_dcpst_p_children_1 ///
sim_dcpst_p_children_2 sim_dcpst_p_children_3p sim_dcpst_snm_children_0 ///
sim_dcpst_snm_children_1 sim_dcpst_snm_children_2 sim_dcpst_snm_children_3p ///
sim_dcpst_prvp_children_0 sim_dcpst_prvp_children_1 ///
sim_dcpst_prvp_children_2 sim_dcpst_prvp_children_3p ///
sim_dcpst_snmprvp_children_0 sim_dcpst_snmprvp_children_1 ///
sim_dcpst_snmprvp_children_2 sim_dcpst_snmprvp_children_3p {
	
	gen `varname'_h = `varname' + 1.96*`varname'_sd
	gen `varname'_l = `varname' - 1.96*`varname'_sd
	
}

* Combine datasets
merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Label variables
label var sim_dcpst_p_children_0 "Partnered, no children"
label var sim_dcpst_p_children_1 "Partnered, 1 child"
label var sim_dcpst_p_children_2 "Partnered, 2 children"
label var sim_dcpst_p_children_3p "Partnered, 3+ children"
label var sim_dcpst_snmprvp_children_0 "Not partnered, no children"
label var sim_dcpst_snmprvp_children_1 "Not partnered, 1 child"
label var sim_dcpst_snmprvp_children_2 "Not partnered, 2 children"
label var sim_dcpst_snmprvp_children_3p "Not partnered, 3+ children"

* Plot figures
foreach varname in dcpst_p_children_0 dcpst_p_children_1 dcpst_p_children_2 ///
dcpst_p_children_3p {
	
	local vtext : variable label sim_`varname'
	if `"`vtext'"' == "" local vtext "sim_`varname'" 
	twoway (rarea sim_`varname'_h sim_`varname'_l year, sort color(red%20) ///
		legend(label(1 "Simulated") position(6) rows(1))) ///
	(line valid_`varname' year, sort color(red) ///
		legend(label(2 "UKHLS"))), ///
		subtitle("`vtext'") ///
		name(`varname', replace) ///
		ytitle("Share", size(small)) ///
		xtitle("") ///
		ylabel(0[0.1]0.5,labsize(vsmall)) ///
		xlabel(,labsize(vsmall)) ///
		legend(size(small)) ///
		graphregion(color(white)) 

}

* Combine plots
grc1leg dcpst_p_children_0 dcpst_p_children_1 dcpst_p_children_2 ///
	dcpst_p_children_3p , ///
	title("Share Partnered and Number of Children") ///
	legendfrom(dcpst_p_children_0) ///
	rows(2) ///
	graphregion(color(white)) ///
	note("Notes: Samples contains all individual ages 18-65. ", size(vsmall)) 
	
graph export ///
"$dir_output_files/partnership/validation_${country}_partnership_children_ts_${min_age}_${max_age}_both.jpg", ///
	replace width(2400) height(1350) quality(100)
	
graph drop _all 
	
