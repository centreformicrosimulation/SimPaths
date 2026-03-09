********************************************************************************
* PROJECT:  		ESPON 
* SECTION:			Validation
* OBJECT: 			Inequality
* AUTHORS:			Ashley Burdett 
* LAST UPDATE:		9/2025 (AB)
* COUNTRY: 			Greece 

* NOTES: 			Equivalized disposable income used to create ratios 
********************************************************************************

//ssc install ineqdeco

********************************************************************************
* 1 : Income ratios through time 
********************************************************************************

********************************************************************************
* 1.1 : Income ratio, 90/50
********************************************************************************

* Prepare validation data
use year dwt valid_y_eq_disp_bu_yr using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_y_eq_disp_bu_yr, d
	
	replace valid_y_eq_disp_bu_yr = . if ///
		valid_y_eq_disp_bu_yr < r(p1) | valid_y_eq_disp_bu_yr > r(p99)

}

collapse (p90) p90_disp = valid_y_eq_disp_bu_yr ///
	(p50) p50_disp = valid_y_eq_disp_bu_yr ///
	[aw = dwt] , by(year)
	
gen p90_p50_ratio_disp_obs = p90_disp/p50_disp

* Align reference years 
gen l_p90_p50_ratio_disp_obs = p90_p50_ratio_disp_obs[_n+1]

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year equivalisedincome using "$dir_data/simulated_data.dta", clear

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum equivalisedincome, d
	
	replace equivalisedincome = . if ///
		equivalisedincome < r(p1) | equivalisedincome > r(p99)

}

collapse (p90) p90_disp = equivalisedincome ///
	(p50) p50_disp = equivalisedincome, by(run year)
	
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
	color(green%20) legend(label(1 "Simulated") position(6) rows(1))) ///
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
use year dwt valid_y_eq_disp_bu_yr using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_y_eq_disp_bu_yr, d
	
	replace valid_y_eq_disp_bu_yr = . if ///
		valid_y_eq_disp_bu_yr < r(p1) | valid_y_eq_disp_bu_yr > r(p99)

}

collapse (p90) p90_disp = valid_y_eq_disp_bu_yr ///
	(p10) p10_disp = valid_y_eq_disp_bu_yr ///
	[aw = dwt], by(year)
	
gen p90_p10_ratio_disp_obs = p90_disp/p10_disp

* Align reference years 
gen l_p90_p10_ratio_disp_obs = p90_p10_ratio_disp_obs[_n+1]

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year equivalisedincome using "$dir_data/simulated_data.dta", clear

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum equivalisedincome, d
	
	replace equivalisedincome = . if ///
		equivalisedincome < r(p1) | equivalisedincome > r(p99)

}

collapse (p90) p90_disp = equivalisedincome ///
	(p10) p10_disp = equivalisedincome, by(run year)
	
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
	color(green%20) legend(label(1 "Simulated") position(6) rows(1))) ///
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
use year dwt valid_y_eq_disp_bu_yr using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum valid_y_eq_disp_bu_yr, d
	
	replace valid_y_eq_disp_bu_yr = . if ///
		valid_y_eq_disp_bu_yr < r(p1) | valid_y_eq_disp_bu_yr > r(p99)

}

* Calulate gini for each year 	
statsby gini = r(gini), by(year) clear: ineqdeco valid_y_eq_disp_bu_yr [aw=dwt]	
	
save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year equivalisedincome using "$dir_data/simulated_data.dta", clear

* Trim outliers
if "$trim_outliers" == "true" {
	
	sum equivalisedincome, d
	
	replace equivalisedincome = . if ///
		equivalisedincome < r(p1) | equivalisedincome > r(p99)

}

* Calulate gini for each year and run	
statsby gini = r(gini), by(year run) clear: ineqdeco equivalisedincome 	

* Obtain the mean and standard deviation by year 
collapse (mean) gini ///
	(sd) gini_sd = gini, by(year)
	
* Compute the 95% confidence interval 
gen gini_high = gini + 1.96 * gini_sd
gen gini_low  = gini - 1.96 * gini_sd

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure
twoway (rarea gini_high gini_low year, sort ///
	color(green%20) legend(label(1 "Simulated") position(6) rows(1))) ///
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

