********************************************************************************
* PROJECT:  		SimPaths UK 
* SECTION:			Validation
* OBJECT: 			Partnership
* AUTHORS:			Ashley Burdett 
* LAST UPDATE:		Jan 2026
* COUNTRY: 			UK 
********************************************************************************
* NOTES: 			
********************************************************************************

********************************************************************************
* 1 : Mean values over time
********************************************************************************

********************************************************************************
* 1.1 : Mean values over time - ages 18-65
********************************************************************************

* Prepare validation data
use year demAge dwt valid_partnered valid_single  using ///
	"$dir_data/ukhls_validation_sample.dta", clear

keep if inrange(demAge,18,65)	
	
* Compute shares	
collapse (mean) valid_partnered valid_single [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace


* Prepare SimPaths data
use run year demAge sim_partnered sim_single using ///
	"$dir_data/simulation_sample.dta", clear

keep if inrange(demAge,18,65)	
	
* Compute shares and sd
collapse (mean) sim_partnered sim_single    ///
	, by(run year)
	
collapse (mean) sim_partnered sim_single  ///
		 (sd) sim_partnered_sd = sim_partnered ///
		 sim_single_sd = sim_single ///
		 , by(year)
	
* Approx 95% confidence interval 	
foreach varname in sim_partnered sim_single  {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

* Combine datasets
merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen


* Plot figures 
* Share partnered
twoway (rarea sim_partnered_high sim_partnered_low year, sort color(green%20) ///
	legend(label(1 "SimPaths"))) ///
(line valid_partnered year, sort color(green) ///
	legend(label(2 "UKHLS "))), ///
	title("Partnered") ///
	subtitle("Ages 18-65") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	xlabel(,labsize(small)) ///
	ylabel(0[0.1]0.9, labsize(small)) ///
	legend(size(small)) ///	
	graphregion(color(white)) ///
	note("Notes: ", size(vsmall))	

* Save figure
graph export ///
"$dir_output_files/partnership/validation_${country}_partnered_ts_18_65_both.jpg", ///
	replace width(2400) height(1350) quality(100)

* Partnership status shares	
twoway (rarea sim_partnered_high sim_partnered_low year, sort color(green%20) ///
	legend(label(1 "Partnered, SimPaths"))) ///
(line valid_partnered year, sort color(green) ///
	legend(label(2 "Partnered, UKHLS "))) ///
(rarea sim_single_high sim_single_low year, sort color(red%20) ///
	legend(label(3 "Single, SimPaths"))) ///
(line valid_single year, sort color(red) ///
	legend(label(4 "Single, UKHLS "))), ///
	title("Partnership status") ///
	subtitle("Ages 18-65") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	xlabel(,labsize(small)) ///
	ylabel(0[0.1]0.8, labsize(small)) ///
	legend(size(small)) ///	
	graphregion(color(white)) ///
	note("Notes: ", size(vsmall))	

graph export ///
"$dir_output_files/partnership/validation_${country}_partnership_ts_18_65_both.jpg", ///
	replace width(2400) height(1350) quality(100)	
	
	
********************************************************************************
* 1.2 : Mean values over time - by age group 
********************************************************************************

* Define the groupings using a semi-colon or a specific delimiter
local age_cond1 "ageGroup == 2 | ageGroup == 3"
local age_sub1  "Ages 20-29"
local age_suff1 "20_29"

local age_cond2 "ageGroup == 4 | ageGroup == 5"
local age_sub2  "Ages 30-39"
local age_suff2 "30_39"

local age_cond3 "ageGroup == 6"
local age_sub3  "Ages 40-59"
local age_suff3 "40_59"

* Loop through the 3 groups
forvalues i = 1/3 {
    
   * Validation data 
    use year demAge dwt valid_partnered valid_single ageGroup using ///
        "$dir_data/ukhls_validation_sample.dta", clear
	
	* Select sample 
    keep if `age_cond`i''
    
    collapse (mean) valid_partnered valid_single [aw = dwt], by(year)
    tempfile valid_stats
    save `valid_stats'

    * Simuated data 
    use run year demAge sim_partnered sim_single ageGroup using ///
        "$dir_data/simulation_sample.dta", clear

	* Select sample	
    keep if `age_cond`i''
    
    collapse (mean) sim_partnered sim_single, by(run year)
    
    collapse (mean) sim_partnered sim_single ///
             (sd)   sim_partnered_sd = sim_partnered ///
                    sim_single_sd    = sim_single, by(year)
            
    foreach varname in sim_partnered sim_single {
	
        gen `varname'_high = `varname' + 1.96*`varname'_sd
        gen `varname'_low  = `varname' - 1.96*`varname'_sd
    
	}

    * Combine
    merge 1:1 year using `valid_stats', keep(3) nogen

    twoway (rarea sim_partnered_high sim_partnered_low year, sort color(green%20) ///
        legend(label(1 "Partnered, SimPaths"))) ///
    (line valid_partnered year, sort color(green) ///
        legend(label(2 "Partnered, UKHLS"))) ///
    (rarea sim_single_high sim_single_low year, sort color(red%20) ///
        legend(label(3 "Single, SimPaths"))) ///
    (line valid_single year, sort color(red) ///
        legend(label(4 "Single, UKHLS"))), ///
        title("Partnership status") ///
        subtitle("`age_sub`i''") ///
        xtitle("Year", size(small)) ///
        ytitle("Share", size(small)) ///
        xlabel(, labsize(small)) ///
        ylabel(0(0.2)1, labsize(small)) ///
        legend(size(small)) ///    
        graphregion(color(white)) ///
        note("Notes: ", size(vsmall))    
        
    graph export ///
    "$dir_output_files/partnership/validation_${country}_partnership_ts_`age_suff`i''_both.jpg", ///
        replace width(2400) height(1350) quality(100)    
		
}

graph drop _all 


********************************************************************************
* 1.3 : Mean values over time - by children 
********************************************************************************	

* Load validation data
use year demAge dwt valid_partnered_children_0 valid_partnered_children_1 ///
	valid_partnered_children_2 valid_partnered_children_3plus ///
	valid_single_children_0 valid_single_children_1 ///
	valid_single_children_2 valid_single_children_3plus  using ///
	"$dir_data/ukhls_validation_sample.dta", clear

collapse (mean) valid_partnered_children_0 valid_partnered_children_1 ///
	 valid_partnered_children_2 valid_partnered_children_3plus ///
	 valid_single_children_0 valid_single_children_1 ///
	 valid_single_children_2 valid_single_children_3plus ///
	 [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Load SimPaths data
use run year demAge sim_partnered_children_0 sim_partnered_children_1 ///
	sim_partnered_children_2 sim_partnered_children_3plus ///
	sim_single_children_0 sim_single_children_1 sim_single_children_2 ///
	sim_single_children_3plus ///
	using "$dir_data/simulation_sample.dta", clear

* Compute shares and sd	
collapse (mean) sim_partnered_children_0 sim_partnered_children_1 ///
	sim_partnered_children_2 sim_partnered_children_3plus ///
	sim_single_children_0 sim_single_children_1 ///
	sim_single_children_2 sim_single_children_3plus, ///
	by(run year)
	
collapse (mean) sim_partnered_children_0 sim_partnered_children_1 ///
	 sim_partnered_children_2 sim_partnered_children_3plus ///
	 sim_single_children_0 sim_single_children_1 sim_single_children_2 ///
	 sim_single_children_3plus ///
	(sd) sim_partnered_children_0_sd = sim_partnered_children_0 ///
		sim_partnered_children_1_sd = sim_partnered_children_1 ///
		sim_partnered_children_2_sd = sim_partnered_children_2 ///
		sim_partnered_children_3plus_sd = sim_partnered_children_3plus ///
		sim_single_children_0_sd = sim_single_children_0 ///
		sim_single_children_1_sd = sim_single_children_1 ///
		sim_single_children_2_sd = sim_single_children_2 ///
		sim_single_children_3plus_sd = sim_single_children_3plus ///
		, by(year)
		 
* Approx 95% confidence interval 		 
foreach varname in sim_partnered_children_0 sim_partnered_children_1 ///
	sim_partnered_children_2 sim_partnered_children_3plus sim_single_children_0 ///
	sim_single_children_1 sim_single_children_2 sim_single_children_3plus  {
	
	gen `varname'_h = `varname' + 1.96*`varname'_sd
	gen `varname'_l = `varname' - 1.96*`varname'_sd
	
}

* Combine datasets
merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Label variables
label var sim_partnered_children_0 "No children"
label var sim_partnered_children_1 "1 child"
label var sim_partnered_children_2 "2 children"
label var sim_partnered_children_3plus "3+ children"
label var sim_single_children_0 "No children"
label var sim_single_children_1 "1 child"
label var sim_single_children_2 "2 children"
label var sim_single_children_3plus "3+ children"

* Plot figures

* Partnered 
foreach varname in partnered_children_0 partnered_children_1 ///
	partnered_children_2 partnered_children_3plus {
	
	local vtext : variable label sim_`varname'
	if `"`vtext'"' == "" local vtext "sim_`varname'" 
	twoway (rarea sim_`varname'_h sim_`varname'_l year, sort color(red%20) ///
		legend(label(1 "SimPaths") position(6) rows(1))) ///
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
grc1leg partnered_children_0 partnered_children_1 partnered_children_2 ///
	partnered_children_3plus , ///
	title("Share Partnered and Number of Children") ///
	legendfrom(partnered_children_0) ///
	rows(2) ///
	graphregion(color(white)) ///
	ycomm ///
	note("Notes: Samples contains all individual ages 18-65. ", size(vsmall)) 
	
graph export ///
"$dir_output_files/partnership/validation_${country}_partnership_children_ts_18_65_partnered.jpg", ///
	replace width(2400) height(1350) quality(100)
	
	
* Single	
foreach varname in single_children_0 single_children_1 single_children_2 ///
	single_children_3plus {
	
	local vtext : variable label sim_`varname'
	if `"`vtext'"' == "" local vtext "sim_`varname'" 
	twoway (rarea sim_`varname'_h sim_`varname'_l year, sort color(red%20) ///
		legend(label(1 "SimPaths") position(6) rows(1))) ///
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
grc1leg single_children_0 single_children_1 single_children_2 ///
	single_children_3plus , ///
	title("Share Single and Number of Children") ///
	legendfrom(single_children_0) ///
	rows(2) ///
	graphregion(color(white)) ///
	ycomm ///
	note("Notes: Samples contains all individual ages 18-65. ", size(vsmall)) 
	
graph export ///
"$dir_output_files/partnership/validation_${country}_partnership_children_ts_18_65_single.jpg", ///
	replace width(2400) height(1350) quality(100)	
	
	
graph drop _all 
	
