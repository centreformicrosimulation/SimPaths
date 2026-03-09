* Checks on the initial population variables 

* Economic activity 
preserve 

keep if dag >= 18 & dag <= 65

gen valid_employed = (les_c4 == 1)
gen valid_student = (les_c4 == 2)
gen valid_inactive = (les_c4 == 3)
gen valid_retired = (les_c4 == 4)

replace valid_employed = . if les_c4 == -9 | les_c4 == . 
replace valid_student = . if les_c4 == -9 | les_c4 == . 
replace valid_inactive = . if les_c4 == -9 | les_c4 == . 
replace valid_retired = . if les_c4 == -9 | les_c4 == . 

collapse (mean) valid_employed valid_student valid_inactive valid_retired ///
	[aw = dwt], by(swv)
	
twoway ///
(line valid_employed swv, sort color(green) legend(label(1 "Employed, observed"))) ///
(line valid_student swv, sort color(blue) legend(label(2 "Students, observed"))) ///
(line valid_inactive swv, sort color(red) legend(label(3 "Inactive, observed"))) ///
(line valid_retired swv, sort color(grey) legend(label(4 "Retired, observed"))) ///
, title("Activity status") xtitle("swv") ytitle("Share") ///
legend(position(6))

graph export "$dir_graphs/initial_pop_check_activity_status.png", replace ///
	width(2560) height(1440) 
	
restore	
	
* Partnership 
preserve 
	
keep if dag >= 18 & dag <= 65

gen valid_partnered = (dcpst == 1)
gen valid_single = (dcpst == 2)

collapse (mean) valid_partnered valid_single [aw = dwt], by(swv)

twoway ///
(line valid_partnered swv, sort color(green) legend(label(1 "Partnered, observed"))) ///
(line valid_single swv, sort color(red) legend(label(2 "Single, observed"))) ///
, title("Partnership status") xtitle("swv") ytitle("Share") ylabel(0.5[0.1]0.7) ///
legend(position(6))
	
graph export "$dir_graphs/initial_pop_check_partnership.png", replace ///
	width(2560) height(1440) 

restore 

* Educational attainment 
preserve 	

keep if dag >= 18 & dag <= 65

gen valid_edu_high = (deh_c3 == 1)
gen valid_edu_med = (deh_c3 == 2)
gen valid_edu_low = (deh_c3 == 3)

replace valid_edu_high = . if deh_c3 == -9 
replace valid_edu_med = . if deh_c3 == -9 
replace valid_edu_low = . if deh_c3 == -9 

collapse (mean) valid_edu_high valid_edu_med valid_edu_low  ///
	[aw = dwt], by(swv)

twoway ///
(line valid_edu_high swv, sort color(green) legend(label(1 "High , observed"))) ///
(line valid_edu_med swv, sort color(blue) legend(label(2 "Medium, observed"))) ///
(line valid_edu_low swv, sort color(red) legend(label(3 "Low, observed"))) ///
, title("Educational attainment") xtitle("swv") ytitle("Share") ///
legend(position(6))
	
graph export "$dir_graphs/initial_pop_check_edu_attainment.png", replace ///
	width(2560) height(1440) 
	
restore 
	
* Hours worked 
preserve 

keep if dag >= 18 & dag <= 65
keep if les_c3 == 1 

replace lhw = . if lhw == -9 

collapse (mean) lhw [aw = dwt], by(swv)

twoway ///
(line lhw swv, sort color(green) legend(label(1 "Observed"))), ///
	title("Hours worked") xtitle("swv") ytitle("Hours per week") ///
	ylabel(34 [2] 44) ///
	note("Note: Statistics calculated on sample of employed individuals")

graph export "$dir_graphs/initial_pop_check_wkly_hrs_worked.png", replace ///
	width(2560) height(1440) 
	
restore 	

preserve

keep if dag >= 18 & dag <= 65
keep if les_c3 == 1 

replace lhw = . if lhw == -9 

twoway /// 
(hist lhw, color(red%20) ///
	legend(label(2 "Observed"))), ///
	title("Weekly hours worked") xtitle("Hours") ///
	ytitle("Density") ///
	note("Note: Statistics calculated on sample of employed individuals") 

graph export "$dir_graphs/initial_pop_check_hist_wkly_hrs_worked.png", replace ///
	width(2560) height(1440) 
	
restore 


* Hourly wage 
preserve 

keep if dag >= 18 & dag <= 65
keep if les_c3 == 1 

replace obs_earnings_hourly = . if obs_earnings_hourly == -9 

* Trim outliers 
sum obs_earnings_hourly, d
replace obs_earnings_hourly = . if ///
	obs_earnings_hourly < r(p1) | obs_earnings_hourly > r(p99)
	
collapse (mean) obs_earnings_hourly [aw = dwt], by(swv)
	
twoway ///
(line obs_earnings_hourly swv, sort color(green) ///
	legend(label(2 "Observed"))), ///
title("Mean hourly wages") xtitle("swv") ytitle("GBP per hour (2015 prices)") ///
	note("Note: Statistics calculated on sample of employed individuals, trimmed")
	
graph export "$dir_graphs/initial_pop_check_hrly_wages.png", replace ///
	width(2560) height(1440) 
	
	
restore 	


preserve 

keep if dag >= 18 & dag <= 65
keep if les_c3 == 1 

replace obs_earnings_hourly = . if obs_earnings_hourly == -9 

sum obs_earnings_hourly, d
replace obs_earnings_hourly = . if ///
	obs_earnings_hourly < r(p1) | obs_earnings_hourly > r(p99)

twoway /// 
(hist obs_earnings_hourly if obs_earnings_hourly, color(red%20) ///
	legend(label(2 "Observed"))), ///
	title("Hourly wages") xtitle("Wages") ///
	ytitle("Density") ///
	note("Note: Statistics calculated on sample of employed individuals, trimmed") 

graph export "$dir_graphs/initial_pop_check_hist_hrly_wages.png", replace ///
	width(2560) height(1440) 
	
	
restore	
