/*******************************************************************************
* PROJECT:  		SimPaths UK 
* SECTION:			Validation
* OBJECT: 			Hours worked (discrete)
* AUTHORS:			Ashley Burdett 
* LAST UPDATE:		Jan 2026
* COUNTRY: 			UK 
********************************************************************************
* NOTES: 			Need to update to acocunt for additional labour supply 
* 					categories 
*******************************************************************************/

********************************************************************************
* 1 : Distribution
********************************************************************************

********************************************************************************
* 1.1 : Distribution, 16-65
********************************************************************************

* Comparison of the discretized labour supply hours 

* Load UKHLS data 
use year dwt labC4 valid_cat_hours valid_labHrsWorkEnum_no demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear
		
* Select sample
keep if labC4 == 1
drop if valid_labHrsWorkEnum_no == 0
keep if inrange(demAge,16,65)

* Hours dummies
tab valid_cat_hours, gen(hours_cat_)

* Calculate weighted proportions
collapse (mean) hours_cat_* [aw=dwt]

gen sim = 0 

save "$dir_data/valid_props", replace

* Prepare simulated data
use run year labC4 idPers sim_cat_hours sim_labHrsWorkEnum_no demAge using ///
	"$dir_data/simulation_sample.dta", clear

* Select sample
keep if labC4 == "EmployedOrSelfEmployed"
drop if sim_labHrsWorkEnum_no == 0
keep if inrange(demAge,16,65)
				
* Hours dummies
tab sim_cat_hours, gen(hours_cat_)

* Calculate proportions
collapse (mean) hours_cat_*, by(run)

collapse (mean) hours_cat_* ///
		 (sd) hours_cat_1_sd = hours_cat_1 ///
		 hours_cat_2_sd = hours_cat_2 ///
		 hours_cat_3_sd = hours_cat_3 ///
		 hours_cat_4_sd = hours_cat_4 ///
		 hours_cat_5_sd = hours_cat_5 ///
		 hours_cat_6_sd = hours_cat_6 

foreach varname in hours_cat_1 hours_cat_2 hours_cat_3 hours_cat_4 ///
	hours_cat_5 hours_cat_6 {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

} 

gen sim = 1 

* Combine datasets
append using "$dir_data/valid_props"

* Plot
reshape long hours_cat_@ hours_cat_@_high hours_cat_@_low, i(sim) j(category)

gen prop_ukhls = hours_cat_ if sim == 0
gen prop_sim   = hours_cat_ if sim == 1

gen x_ukhls = category - 0.2
gen x_sim   = category + 0.2

twoway (bar prop_ukhls x_ukhls, barw(0.4) color(red%50)) /// 
       (bar prop_sim x_sim, barw(0.4) color(green%50)) /// 
       (rcap hours_cat__high hours_cat__low x_sim, lcolor(green)), ///
       xlabel(1/6, valuelabel) ///
       xtitle("Hours Category", size(small)) ///
	   ytitle("Proportion", size(small)) ///
       title("Share in Each Labour Hours Category") ///
	   subtitle("Ages 16-65") ///
       legend(order(1 "UKHLS" 2 "SimPaths" 3 "95% CI")) ///
		legend(size(small)) ///	
       graphregion(color(white)) ///
	note("Notes: Years 2011-2023. Categories 1 = 6-15 hours, 2 = 16-25 hours, 3 = 26-35 hours , 4 = 36-40 hours, 5 = 41-49 hours," "6 = 55+ hours.", size(vsmall))

graph export ///
	"$dir_output_files/hours_worked/validation_${country}_hours_worked_cat_all.png", ///
	replace width(2400) height(1350) 


********************************************************************************
* 1.2 : Distribution, 16-65  by year
********************************************************************************

* Load UKHLS data 
use year dwt labC4 valid_cat_hours valid_labHrsWorkEnum_no demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear
		
* Select sample		
keep if labC4 == 1
drop if valid_labHrsWorkEnum_no == 0
keep if inrange(demAge,16,65)

tab valid_cat_hours, gen(hours_cat_)

* Calculate weighted proportions by year 
collapse (mean) hours_cat_* [aw=dwt], by(year)

gen sim = 0 

save "$dir_data/valid_props", replace

* Load SimPaths data
use run year labC4 idPers sim_cat_hours sim_labHrsWorkEnum_no demAge using ///
	"$dir_data/simulation_sample.dta", clear

* Select sample	
keep if labC4 == "EmployedOrSelfEmployed"
drop if sim_labHrsWorkEnum_no == 0
keep if inrange(demAge,16,65)
				
tab sim_cat_hours, gen(hours_cat_)

* Calculate proportions by run and year
collapse (mean) hours_cat_*, by(run year)

* Calculate Mean and SD across runs by year
collapse (mean) hours_cat_* ///
		 (sd) hours_cat_1_sd = hours_cat_1 ///
		 hours_cat_2_sd = hours_cat_2 ///
		 hours_cat_3_sd = hours_cat_3 ///
		 hours_cat_4_sd = hours_cat_4 ///
		 hours_cat_5_sd = hours_cat_5 ///
		 hours_cat_6_sd = hours_cat_6, by(year)

foreach varname in hours_cat_1 hours_cat_2 hours_cat_3 hours_cat_4 ///
	hours_cat_5 hours_cat_6 {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd
} 

gen sim = 1 

* Combine datasets 
append using "$dir_data/valid_props"

* Plot
reshape long hours_cat_@ hours_cat_@_high hours_cat_@_low, i(sim year) ///
	j(category)

gen prop_ukhls = hours_cat_ if sim == 0
gen prop_sim   = hours_cat_ if sim == 1
gen x_ukhls = category - 0.2
gen x_sim   = category + 0.2

levelsof year, local(years)
foreach y in `years' {
	
	twoway (bar prop_ukhls x_ukhls if year == `y', barw(0.4) color(red%50)) ///  
	       (bar prop_sim x_sim if year == `y', barw(0.4) color(green%50)) ///  
	       (rcap hours_cat__high hours_cat__low x_sim if year == `y', lcolor(green)), ///
	        xlabel(1/6, valuelabel) ///
	        xtitle("Hours Category", size(small)) ///
	        ytitle("Proportion", size(small)) ///
		 	title("Share in Each Labour Hours Category") ///
			subtitle("`y'") ///
	        legend(order(1 "UKHLS" 2 "SimPaths" 3 "95% CI")) ///
			legend(size(small)) ///	
	        graphregion(color(white)) ///
			note("Notes: Ages 16-65. Categories 1 = 6-15 hours, 2 = 16-25 hours, 3 = 26-35 hours , 4 = 36-40 hours, 5 = 41-49 hours," "6 = 55+ hours.", size(vsmall))

	graph export ///
		"$dir_output_files/hours_worked/validation_${country}_hours_worked_cat_`y'.png", ///
		replace width(2400) height(1350) 
}

graph drop _all

	
********************************************************************************
* 1.3 : Distribution, 16-65  by year, by gender
********************************************************************************

* Load UKHLS data 
use year dwt labC4 valid_cat_hours valid_labHrsWorkEnum_no demAge ///
	demMaleFlag using "$dir_data/ukhls_validation_sample.dta", clear

* Select sample	
keep if labC4 == 1
drop if valid_labHrsWorkEnum_no == 0
keep if inrange(demAge,16,65)

tab valid_cat_hours, gen(hours_cat_)

* Calculate weighted proportions by year and gender
collapse (mean) hours_cat_* [aw=dwt], by(year demMaleFlag)

gen sim = 0 

save "$dir_data/valid_props", replace

* Load SimPaths data
use run year labC4 idPers sim_cat_hours sim_labHrsWorkEnum_no demAge ///
	demMaleFlag using "$dir_data/simulation_sample.dta", clear

* Select sample	
keep if labC4 == "EmployedOrSelfEmployed"
drop if sim_labHrsWorkEnum_no == 0
keep if inrange(demAge,16,65)
				
tab sim_cat_hours, gen(hours_cat_)

* Calculate proportions and SD by run, year and gender
collapse (mean) hours_cat_*, by(run year demMaleFlag)

* Calculate Mean and SD across runs by year and gender 
collapse (mean) hours_cat_* ///
		 (sd) hours_cat_1_sd = hours_cat_1 ///
		 hours_cat_2_sd = hours_cat_2 ///
		 hours_cat_3_sd = hours_cat_3 ///
		 hours_cat_4_sd = hours_cat_4 ///
		 hours_cat_5_sd = hours_cat_5 ///
		 hours_cat_6_sd = hours_cat_6, by(year demMaleFlag)

foreach varname in hours_cat_1 hours_cat_2 hours_cat_3 hours_cat_4 ///
	hours_cat_5 hours_cat_6 {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd
} 

gen sim = 1 

* Combine datasets 
append using "$dir_data/valid_props"

* PLot
* Note: Added demMaleFlag to the identifier i()
reshape long hours_cat_@ hours_cat_@_high hours_cat_@_low, ///
	i(sim year demMaleFlag) j(category)

gen prop_ukhls = hours_cat_ if sim == 0
gen prop_sim   = hours_cat_ if sim == 1
gen x_ukhls = category - 0.2
gen x_sim   = category + 0.2

* Label gender for plot titles
label define sex_lbl 0 "Females" 1 "Males"
label values demMaleFlag sex_lbl

levelsof year, local(years)
levelsof demMaleFlag, local(sexes)

foreach y in `years' {
	foreach s in `sexes' {
		
		* Get the label text 
		local sextext : label sex_lbl `s'
		
		twoway (bar prop_ukhls x_ukhls if year == `y' & demMaleFlag == `s', ///
			barw(0.4) color(red%50)) ///  
			(bar prop_sim x_sim if year == `y' & demMaleFlag == `s', ///
			barw(0.4) color(green%50)) ///  
			(rcap hours_cat__high hours_cat__low x_sim if year == `y' & ///
			demMaleFlag == `s', lcolor(green)), ///
		    xlabel(1/6, valuelabel) ///
			xtitle("Hours Category", size(small)) ///
			ytitle("Proportion", size(small)) ///
		 	title("Share in Each Labour Hours Category") ///
			subtitle("`y', `sextext'") ///
			legend(order(1 "UKHLS" 2 "SimPaths" 3 "95% CI")) ///
			legend(size(small)) ///	
			graphregion(color(white)) ///
			note("Notes: Comparison for `sextext' in `y'." "Categories: 1=6-15, 2=16-25, 3=26-35, 4=36-40, 5=41-49, 6=55+ hours.", size(vsmall))

		graph export ///
			"$dir_output_files/hours_worked/validation_${country}_hours_worked_cat_`y'_`sextext'.png", ///
			replace width(2400) height(1350) 
	}
	
}	
	
graph drop _all
	
	
/*		
********************************************************************************
* 3 : Mean hours of work, comparison across all options explored, 16-655
********************************************************************************
	
use "$dir_data/temp_valid_mean_disc", replace
	
merge 1:1 year using "$dir_data/temp_valid_mean", nogen

rename hours valid_hours

merge 1:1 year using "$dir_data/temp_sim_mean_uni.dta", nogen

drop sim_labHrsWorkWeek_sim sim_labHrsWorkWeek_sim_sd
rename sim_labHrsWorkWeek_sim_high sim_labHrsWorkWeek_sim_u_high
rename sim_labHrsWorkWeek_sim_low sim_labHrsWorkWeek_sim_u_low

merge 1:1 year using "$dir_data/temp_sim_mean_ln.dta", nogen

drop sim_labHrsWorkWeek_sim sim_labHrsWorkWeek_sim_sd
rename sim_labHrsWorkWeek_sim_high sim_labHrsWorkWeek_sim_ln_high
rename sim_labHrsWorkWeek_sim_low sim_labHrsWorkWeek_sim_ln_low

merge 1:1 year using "$dir_data/temp_sim_mean_ln_40.dta", nogen

drop sim_labHrsWorkWeek_sim sim_labHrsWorkWeek_sim_sd
rename sim_labHrsWorkWeek_sim_high sim_labHrsWorkWeek_sim_40_high
rename sim_labHrsWorkWeek_sim_low sim_labHrsWorkWeek_sim_40_low


* Plot comparison 
drop if year < 2011

twoway (line valid_hours year, sort color(cranberry) ///
	legend(label(1 "UKHLS, continuous"))) ///
(rarea sim_labHrsWorkWeek_sim_u_high sim_labHrsWorkWeek_sim_u_low year, sort color(purple%20) ///
	legend(label(2 "Simulated, uniform"))) ///
(rarea sim_labHrsWorkWeek_sim_ln_high sim_labHrsWorkWeek_sim_ln_low year, sort color(blue%20) ///
	legend(label(3 "Simulated, log normal"))) ///
(rarea sim_labHrsWorkWeek_sim_40_high sim_labHrsWorkWeek_sim_40_low year, sort color(green%20) ///
	legend(label(4 "Simulated, log normal + 40"))), ///	
	title("Average Weekly Hours Worked") ///
	subtitle("Ages 16-65 ") ///
	xtitle("Year", size(small)) ///
	ytitle("Hours per week", size(small)) ///
	ylabel(33 [2] 40 ,labsize(small)) ///
	xlabel(,labsize(small)) ///
	legend(size(small)) ///	
	graphregion(color(white)) ///
	note("Note: Statistics calculated on sample of working age employed and self-employed individuals.", ///
	size(vsmall))	
	
graph export ///
		"$dir_output_files/hours_worked/validation_${country}_hours_worked_comparison.png", ///
		replace width(2400) height(1350) 	
		
		
//(line valid_disc_hours year, sort color(blue) ///
//	legend(label(2 "UKHLS, discretized"))) ///
		