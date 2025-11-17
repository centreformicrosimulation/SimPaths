********************************************************************************
* PROJECT:  		ESPON 
* SECTION:			Wages
* OBJECT: 			Internal validation
* AUTHORS:			Ashley Burdett, Daria Popova 
* LAST UPDATE:		May 2025
* COUNTRY: 			UK   

* NOTES: 			Compares predicted values to the observed values. 
* 					Individual heterogeneity added to the standard predicted 
* 					values using a random draw like in stochasitic 
* 					imputation. The pooled mean is obtained as in multiple 
* 					imputation by repeating the random draw 20 times for each 
* 					process. 
* 
* 					Run after "reg_wages.do"
********************************************************************************

* Female - No previous wage 

use "$dir_validation_data/Female_NPW_sample", clear

* Correct bias when transforming from log to levels 
cap drop epsilon
gen epsilon = rnormal()*e(sigma) 

replace pred_hourly_wage = exp(lwage_hour_hat + epsilon) if in_sample_fnpw 
 
twoway (hist pred_hourly_wage if pred_hourly_wage<150 & in_sample_fnpw == 1, ///
		width(1) color(red)) ///
	(hist wage_hour if wage_hour<150 & in_sample_fnpw == 1, width(1) ///
	color(none) lcolor(black)), ///
	title("Hourly Wages") ///
	subtitle("Females, no previous wage observed") ///
	xtitle (Gross hourly wages (GBP)) legend(lab(1 "Observed") ///
	lab( 2 "Predicted")) name(log, replace) ///
	graphregion(color(white)) ///
	legend(size(small)) ///
	note("Notes: Sample includes working age (18-64) females. Predictions obtained from the estimates of  a Heckman model.", size(vsmall))
	
graph export ///
	"$dir_validation_graphs/wages/int_validation_wages_hist_f_npw.png", replace as(png) width(2560) height(1440)
	
	
* Male - No previous wage 

use "$dir_validation_data/Male_NPW_sample", clear

* Correct bias when transforming from log to levels 
cap drop epsilon
gen epsilon = rnormal()*e(sigma) 

replace pred_hourly_wage = exp(lwage_hour_hat + epsilon) if in_sample_mnpw 
 
twoway (hist pred_hourly_wage if  pred_hourly_wage<150 & in_sample_mnpw == 1, ///
		width(1) color(red)) ///
	(hist wage_hour if  wage_hour<150 & in_sample_mnpw == 1, width(1) ///
	color(none) lcolor(black)), ///
	title("Hourly Wages") ///
	subtitle("Males, no previous wage observed") ///
	xtitle (Gross hourly wages (GBP)) legend(lab(1 "Observed") ///
	lab( 2 "Predicted")) name(log, replace) ///
	graphregion(color(white)) ///
	legend(size(small)) ///
	note("Notes: Sample includes working age (18-64) males. Predictions obtained from the estimates of  a Heckman model.", size(vsmall))
	
graph export ///
	"$dir_validation_graphs/wages/int_validation_wages_hist_m_npw.png", replace as(png) width(2560) height(1440)
	
	
* Female - Previous wage 

use "$dir_validation_data/Female_PW_sample", clear

* Correct bias when transforming from log to levels 
cap drop epsilon
gen epsilon = rnormal()*e(sigma) 

replace pred_hourly_wage = exp(lwage_hour_hat + epsilon) if in_sample_fpw 
 
twoway (hist pred_hourly_wage if  pred_hourly_wage<150 & in_sample_fpw == 1, ///
		width(1) color(red)) ///
	(hist wage_hour if  wage_hour<150 & in_sample_fpw == 1, width(1) ///
	color(none) lcolor(black)), ///
	title("Hourly Wages") ///
	subtitle("Females, previous wage observed") ///
	xtitle (Gross hourly wages (GBP)) legend(lab(1 "Observed") ///
	lab( 2 "Predicted")) name(log, replace) ///
	graphregion(color(white)) ///
	legend(size(small)) ///
	note("Notes: Sample includes working age (18-64) females. Predictions obtained from the estimates of  a Heckman model.", size(vsmall))
	
graph export ///
	"$dir_validation_graphs/wages/int_validation_wages_hist_f_pw.png", replace as(png) width(2560) height(1440)
	
	
* Male - Previous wage 

use "$dir_validation_data/Male_PW_sample", clear

* Correct bias when transforming from log to levels 
cap drop epsilon
gen epsilon = rnormal()*e(sigma) 

replace pred_hourly_wage = exp(lwage_hour_hat + epsilon) if in_sample_mpw 
 
twoway (hist pred_hourly_wage if  pred_hourly_wage<150 & in_sample_fpw == 1, ///
		width(1) color(red)) ///
	(hist wage_hour if  wage_hour<150 & in_sample_fpw == 1, width(1) ///
	color(none) lcolor(black)), ///
	title("Hourly Wages") ///
	subtitle("Males, previous wage observed") ///
	xtitle (Gross hourly wages (GBP)) legend(lab(1 "Observed") ///
	lab( 2 "Predicted")) name(log, replace) ///
	graphregion(color(white)) ///
	legend(size(small)) ///
	note("Notes: Sample includes working age (18-64) males. Predictions obtained from the estimates of  a Heckman model.", size(vsmall))
	
graph export ///
	"$dir_validation_graphs/wages/int_validation_wages_hist_m_pw.png", replace as(png) width(2560) height(1440)	
	

graph drop _all


