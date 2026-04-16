********************************************************************************
* PROJECT:  		SimPath UK 
* SECTION:			Validation
* OBJECT: 			Inequality
* AUTHORS:			Ashley Burdett 
* LAST UPDATE:		9/2025 (AB)
* COUNTRY: 			UK 
********************************************************************************
* NOTES: 			Equivalized disposable income used to created ratios 
********************************************************************************

//ssc install ineqdeco

********************************************************************************
* 1 : Income ratios through time 
********************************************************************************

********************************************************************************
* 1.1 : Income ratio, 90/50
********************************************************************************

* Prepare validation data
use year demAge dwt valid_yDispBuEquivYear using ///
	"$dir_data/ukhls_validation_sample.dta", clear

keep if inrange(demAge,18,65)	
	
* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_yDispBuEquivYear, d
	
	replace valid_yDispBuEquivYear = . if ///
		valid_yDispBuEquivYear < r(p1) | valid_yDispBuEquivYear > r(p99)

}

collapse (p90) p90_disp = valid_yDispBuEquivYear ///
	(p50) p50_disp = valid_yDispBuEquivYear ///
	[aw = dwt] , by(year)
	
gen p90_p50_ratio_disp_obs = p90_disp/p50_disp

* Align reference years 
gen l_p90_p50_ratio_disp_obs = p90_p50_ratio_disp_obs[_n+1]

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year demAge sim_yDispEquivYear using ///
	"$dir_data/simulation_sample.dta", clear

keep if inrange(demAge,18,65)

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum sim_yDispEquivYear, d
	
	replace sim_yDispEquivYear = . if ///
		sim_yDispEquivYear < r(p1) | sim_yDispEquivYear > r(p99)

}

collapse (p90) p90_disp = sim_yDispEquivYear ///
	(p50) p50_disp = sim_yDispEquivYear, by(run year)
	
gen p90_p50_ratio_disp = p90_disp/p50_disp

collapse (mean) p90_p50_ratio_disp ///
	(sd) sd_p90_p50_ratio_disp = p90_p50_ratio_disp ///
	 , by(year)

 foreach var in p90_p50_ratio_disp {
 
	gen `var'_high = `var' + 1.96*sd_`var'
	gen `var'_low = `var' - 1.96*sd_`var'
	
}
	 
merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen


* Plot figure
twoway (rarea p90_p50_ratio_disp_high p90_p50_ratio_disp_low year, sort ///
	color(green%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
(line p90_p50_ratio_disp_obs year, sort color(green)legend(label(2 "UKHLS"))), ///
	title("P90/P50 Disposable Income Ratio") ///
	subtitle("Ages 18-65") ///
	xtitle("Year", size(small)) ///
	ytitle("Ratio", size(small)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: Ratios computed using individual observations of benefit unit measure of equivalized disposable income.", ///
	size(vsmall)) 

* Save figure
graph export "$dir_output_files/inequality/validation_${country}_p90p50.jpg", ///
	replace width(2400) height(1350) quality(100)	
	
	
********************************************************************************
* 1.1 : Income ratio, 90/10
********************************************************************************

* Prepare validation data
use year demAge dwt valid_yDispBuEquivYear using ///
	"$dir_data/ukhls_validation_sample.dta", clear

keep if inrange(demAge,18,65)	

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_yDispBuEquivYear, d
	
	replace valid_yDispBuEquivYear = . if ///
		valid_yDispBuEquivYear < r(p1) | valid_yDispBuEquivYear > r(p99)

}

collapse (p90) p90_disp = valid_yDispBuEquivYear ///
	(p10) p10_disp = valid_yDispBuEquivYear ///
	[aw = dwt], by(year)
	
gen p90_p10_ratio_disp_obs = p90_disp/p10_disp

* Align reference years 
gen l_p90_p10_ratio_disp_obs = p90_p10_ratio_disp_obs[_n+1]

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year demAge sim_yDispEquivYear using ///
	"$dir_data/simulation_sample.dta", clear

keep if inrange(demAge,18,65)	
	
* Trim outliers
if "$trim_outliers" == "true" {
	
	sum sim_yDispEquivYear, d
	
	replace sim_yDispEquivYear = . if ///
		sim_yDispEquivYear < r(p1) | sim_yDispEquivYear > r(p99)

}

collapse (p90) p90_disp = sim_yDispEquivYear ///
	(p10) p10_disp = sim_yDispEquivYear, by(run year)
	
gen p90_p10_ratio_disp = p90_disp/p10_disp

collapse (mean) p90_p10_ratio_disp ///
	(sd) sd_p90_p10_ratio_disp = p90_p10_ratio_disp ///
	 , by(year)

 foreach var in p90_p10_ratio_disp {
 
	gen `var'_high = `var' + 1.96*sd_`var'
	gen `var'_low = `var' - 1.96*sd_`var'
	
}
	 
merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen


* Plot figure
twoway (rarea p90_p10_ratio_disp_high p90_p10_ratio_disp_low year, sort ///
	color(green%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
(line p90_p10_ratio_disp_obs year, sort color(green)legend(label(2 "UKHLS"))), ///
	title("P90/P10 Disposable Income Ratio") ///
	subtitle("Ages 18-65") ///
	xtitle("Year", size(small)) ///
	ytitle("Ratio", size(small)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: Ratios computed using individual observations of benefit unit measure of equivalized disposable income.", ///
	size(vsmall)) 
	
* Save figure
graph export "$dir_output_files/inequality/validation_${country}_p90p10.jpg", ///
	replace width(2400) height(1350) quality(100)
	

********************************************************************************
* 1.3 : Gini coefficeint 
********************************************************************************	
	
* Prepare validation data
use year demAge dwt valid_yDispBuEquivYear using ///
	"$dir_data/ukhls_validation_sample.dta", clear

keep if inrange(demAge,18,65)	
	
* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_yDispBuEquivYear, d
	
	replace valid_yDispBuEquivYear = . if ///
		valid_yDispBuEquivYear < r(p1) | valid_yDispBuEquivYear > r(p99)

}

* Calulate gini for each year 	
statsby gini = r(gini), by(year) clear: ineqdeco valid_yDispBuEquivYear [aw=dwt]	
	
save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year demAge sim_yDispEquivYear using ///
	"$dir_data/simulation_sample.dta", clear

keep if inrange(demAge,18,65)	
	
* Trim outliers
if "$trim_outliers" == "true" {
	
	sum sim_yDispEquivYear, d
	
	replace sim_yDispEquivYear = . if ///
		sim_yDispEquivYear < r(p1) | sim_yDispEquivYear > r(p99)

}

* Calculate gini for each year and run	
statsby gini = r(gini), by(year run) clear: ineqdeco sim_yDispEquivYear 	

* Obtain the mean and standard deviation by year 
collapse (mean) gini ///
	(sd) gini_sd = gini, by(year)
	
* Compute the 95% confidence interval 
gen gini_high = gini + 1.96 * gini_sd
gen gini_low  = gini - 1.96 * gini_sd

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea gini_high gini_low year, sort ///
	color(green%20) legend(label(1 "SimPaths") position(6) rows(1))) ///
(line gini year, sort color(green)legend(label(2 "UKHLS"))), ///
	title("Gini Coefficient") ///
	subtitle("Ages 18-65") ///
	xtitle("Year", size(small)) ///
	ytitle("Coefficient", size(small)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: Gini coefficient computed using individual observations of benefit unit measure of equivalized disposable income.", ///
	size(vsmall)) 
	
* Save figure
graph export "$dir_output_files/inequality/validation_${country}_gini.jpg", ///
	replace width(2400) height(1350) quality(100)

	
graph drop _all 	



********************************************************************************
* 1.4 : Net transfers
********************************************************************************

use "$dir_data/simulation_sample.dta", clear 

* keep only one observation per benefit unit 
sort run idBu year  

* Keep one observation per benefit unit	
* Create a marker for the first observation in each group
bys run year idBu (demAge): gen byte to_keep = (_n == _N)

* Keep only the marked rows
keep if to_keep == 1
drop to_keep 

* Create gross income deciles
sort run idPers year

xtile sim_decile = sim_yNonBenBuGrossLevelYear , n(10)

tab sim_decile


* Plots

* Sources

* All 	
graph bar (mean) sim_net_transfers , over(sim_decile) ///
	title("SimPaths") ///
	name(simulated_net_trans_all, replace) ///
	b1title("BU Gross Income Decile", size(small)) ///
	ytitle("£") ///
    ylabel(#5, format(%12.0fc)) ///
	ylabel(,labsize(small)) ///
	graphregion(color(white)) 


* Oldest <= 65	
preserve 

drop if demAge > 65

graph bar (mean) sim_net_transfers , over(sim_decile) ///
	title("SimPaths") ///
	name(simulated_net_trans_upto65, replace) ///
	b1title("BU Gross Income Decile", size(small)) ///
	ytitle("£") ///
    ylabel(#5, format(%12.0fc)) ///
	ylabel(,labsize(small)) ///
	graphregion(color(white)) 


restore 	

* Oldest > 65
preserve 

drop if demAge <= 65

graph bar (mean) sim_net_transfers , over(sim_decile) ///
	title("SimPaths") ///
	name(simulated_net_trans_66plus, replace) ///
	b1title("BU Gross Income Decile", size(small)) ///
	ytitle("£") ///
    ylabel(#5, format(%12.0fc)) ///
	ylabel(,labsize(small)) ///
	graphregion(color(white)) 

restore 	


use "$dir_data/ukhls_validation_sample.dta", clear 

* keep only one observation per benefit unit 
sort idBu year  

* Keep one observation per benefit unit	
* Create a marker for the first observation in each group
bys year idBu (demAge): gen byte to_keep = (_n == _N)

* Keep only the marked rows
keep if to_keep == 1
drop to_keep 

* Create gross income deciles
sort idPers year

xtile valid_decile = valid_yNonBenBuGrossLevelYear , n(10)

tab valid_decile


* Plots

* Sources

* All 	
graph bar (mean) valid_net_transfers , over(valid_decile) ///
	title("UKHLS") ///
	name(valid_net_trans_all, replace) ///
	b1title("BU Gross Income Decile", size(small)) ///
	ytitle("£") ///
    ylabel(#5, format(%12.0fc)) ///
	ylabel(,labsize(small)) ///
	graphregion(color(white)) 


* Oldest <= 65	
preserve 

drop if demAge > 65

graph bar (mean) valid_net_transfers , over(valid_decile) ///
	title("UKHLS") ///
	name(valid_net_trans_upto65, replace) ///
	b1title("BU Gross Income Decile", size(small)) ///
	ytitle("£") ///
    ylabel(#5, format(%12.0fc)) ///
	ylabel(,labsize(small)) ///
	graphregion(color(white)) 


restore 	

* Oldest > 65
preserve 

drop if demAge <= 65

graph bar (mean) valid_net_transfers , over(valid_decile) ///
	title("UKHLS") ///
	name(valid_net_trans_66plus, replace) ///
	b1title("BU Gross Income Decile", size(small)) ///
	ytitle("£") ///
    ylabel(#5, format(%12.0fc)) ///
	ylabel(,labsize(small)) ///
	graphregion(color(white)) 

restore 	

//net install grc1leg2, from(http://digital.cgdev.org/doc/stata/MO/Misc)


* Shares - Retirement age 
grc1leg2 simulated_net_trans_all valid_net_trans_all, ///
	rows(1) ycommon loff ///
	graphregion(color(white)) ///
	title("Average Net Transfers") ///
	subtitle("All ages") ///
	note("NOTE: ", ///
	size(vsmall)) 
	
graph export ///
	"$dir_output_files/inequality/validation_${country}_net_transfers_all.png", ///
	replace width(2400) height(1350) 	
		
		
grc1leg2 simulated_net_trans_upto65 valid_net_trans_upto65 , ///
	rows(1) ycommon loff ///
	graphregion(color(white)) ///
	title("Average Net Transfers") ///
	subtitle("Oldest Age <= 65") ///	
	note("NOTE: ", ///
	size(vsmall)) 
	
graph export ///
	"$dir_output_files/inequality/validation_${country}_net_transfers_upto65.png", ///
	replace width(2400) height(1350) 	
		
		
grc1leg2 simulated_net_trans_66plus valid_net_trans_66plus , ///
	rows(1) ycommon loff ///
	graphregion(color(white)) ///
	title("Average Net Transfers") ///
	subtitle("Oldest Age > 65") ///
	note("NOTE: ", ///
	size(vsmall)) 
	
graph export ///
	"$dir_output_files/inequality/validation_${country}_net_transfers_66plus.png", ///
		replace width(2400) height(1350) 			
	
graph drop _all 	

