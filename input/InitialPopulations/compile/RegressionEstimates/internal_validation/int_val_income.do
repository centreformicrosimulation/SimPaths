********************************************************************************
* PROJECT:  		ESPON 
* SECTION:			Income
* OBJECT: 			Internal validation
* AUTHORS:			Ashley Burdett, daria Popova 
* LAST UPDATE:		July May 2025
* COUNTRY: 			UK   

* NOTES: 			Compares predicted values to the observed values of the 
* 					hurdle models used for the income processes. 
* 					Individual heterogeneity added to the standard predicted 
* 					values using a random draw like in stochasitic 
* 					imputation. The pooled mean is obtained as in multiple 
* 					imputation by repeating the random draw 20 times for each 
* 					process. 
* 
* 					Run after "reg_income.do"
********************************************************************************

* I3a selection - capital income, in initial education spell

use "$dir_validation_data/I3a_selection_sample", clear 

forvalues i = 0/19 {
	local my_seed = 12345 + `i'  
    set seed `my_seed' 	
	gen rnd = runiform() 	
	gen pred_receives_ypncp`i' = 0 
	replace pred_receives_ypncp`i' = 1 if inrange(p,rnd,1)
	drop rnd
}

keep if in_sample == 1 

replace stm = 2000 + stm 
egen pred_receives_ypncp = rowmean(pred_receives_ypncp0-pred_receives_ypncp19)

* Raw prediction vs observed
twoway ///
	(histogram pred_receives_ypncp0, color(red)) ///
	(histogram receives_ypncp, color(none) lcolor(black)), ///
	xtitle (Receives capital income) ///
	legend(lab(1 "Predicted") lab( 2 "Observed")) name(levels, replace) ///
	title("Receives Capital Income") ///
	subtitle("In initial education spell") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	graphregion(color(white)) ///
	legend(size(small)) ///
	note("Notes: Predicted vs observed of dummy indicating capital income is recieved. Estimation sample plotted. Sample contains all" "individual age 16+, who are in their initial education spell. Initial education spell defined generously.", size(vsmall))

graph export "$dir_validation_graphs/income/int_validation_I3a_selection_capital_init_edu_hist_all.png", ///
	as(png) replace width(2560) height(1440) //quality(100)

	
* Year 
preserve

collapse (mean) receives_ypncp pred_receives_ypncp  [aw = dwt], by(stm)

twoway ///
(line pred_receives_ypncp stm, sort color(green) legend(label(1 "Predicted"))) ///
(line receives_ypncp stm, sort color(green) color(green%20) lpattern(dash) ///
	legend(label(2 "Observed"))), ///
title("Receives Captial Income") ///
	subtitle("In initial education spell") ///
	xtitle("Year") ytitle("Share") ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who are observed in their initial education spell, aged 16+ years old." "Initial education spell defined generously. Predictions are the average over 20 random draws.", size(vsmall))	
	
graph export "$dir_validation_graphs/income/int_validation_I3a_selection_capital_init_edu_ts_all_both.png", ///
	as(png) replace width(2560) height(1440) //quality(100)
	
	
restore	
	
graph drop _all	
	

* By gender 	
preserve
	
collapse (mean) receives_ypncp pred_receives_ypncp  [aw = dwt], by(stm dgn)
	
twoway ///
(line pred_receives_ypncp stm if dgn == 0, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line receives_ypncp stm if dgn == 0, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph1) title("Females") xtitle("Year") ytitle("Share") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) 

twoway ///
(line pred_receives_ypncp stm if dgn == 1, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line receives_ypncp stm if dgn == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph2) title("Males") xtitle("Year") ytitle("Share")  ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) 

grc1leg graph1 graph2 ,  ///
	title("Receives Captial Income ") ///
	subtitle("In initial education spell") ///
	legendfrom(graph1) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who are observed in their initial education spell, aged 16+ years old." "Initial education spell defined generously. Predictions are the average over 20 random draws.", size(vsmall))	
	
graph export "$dir_validation_graphs/income/int_validation_I3a_selection_capital_init_edu_ts_all_gender.png", ///
	as(png) replace width(2560) height(1440) //quality(100)
	
restore 

graph drop _all 
	
	
* Share by age 
preserve
	
collapse (mean) receives_ypncp pred_receives_ypncp  [aw = dwt], by(dag)

twoway ///
(line pred_receives_ypncp dag, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line receives_ypncp dag, sort color(green) color(green%20) lpattern(dash) ///
	legend(label(2 "Observed"))), ///
title("Receives Capital Income") subtitle("In initial education spell, share by age") ///
	xtitle("Age") ///
	ytitle("Share") xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who are observed in their initial education spell, aged 16+ years old." "Initial education spell defined generously. Predictions are the average over 20 random draws.", size(vsmall))	

graph export "$dir_validation_graphs/income/int_validation_I3a_selection_capital_init_edu_share_age.png", ///
	as(png) replace width(2560) height(1440) //quality(100)
	
restore
	
graph drop _all	
	
	
* Hh income 	
preserve
	
collapse (mean) receives_ypncp pred_receives_ypncp  [aw = dwt], by(ydses_c5 stm)

twoway ///
(line pred_receives_ypncp stm if ydses_c5 == 1, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line receives_ypncp stm if ydses_c5 == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph1) title("First quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_receives_ypncp stm if ydses_c5 == 2, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line receives_ypncp stm if ydses_c5 == 2, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph2) title("Second quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_receives_ypncp stm if ydses_c5 == 3, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line receives_ypncp stm if ydses_c5 == 3, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph3) title("Third quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_receives_ypncp stm if ydses_c5 == 4, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line receives_ypncp stm if ydses_c5 == 4, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph4) title("Fourth quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_receives_ypncp stm if ydses_c5 == 5, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line receives_ypncp stm if ydses_c5 == 5, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph5) title("Fifth quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

grc1leg graph1 graph2 graph3 graph4 graph5,  ///
	title("Receives Capital Income ") ///
	subtitle("In initial education spell, by hh disposable income") ///
	legendfrom(graph1) rows(2) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who are observed in their initial education spell, aged 16+ years old." "Initial education spell defined generously. Predictions are the average over 20 random draws.", size(vsmall))	
	
graph export "$dir_validation_graphs/income/int_validation_I3a_selection_capital_init_edu_ts_all_both_income.png", ///
	as(png) replace width(2560) height(1440) //quality(100)

restore
	
graph drop _all		
	
	
* Marital status 
preserve
	
collapse (mean) receives_ypncp pred_receives_ypncp  [aw = dwt], by(dcpst stm)
	
twoway ///
(line pred_receives_ypncp stm if dcpst == 1, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line receives_ypncp stm if dcpst == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph1) title("Partnered") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_receives_ypncp stm if dcpst == 2, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line receives_ypncp stm if dcpst == 2, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph2) title("Single") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_receives_ypncp stm if dcpst == 3, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line receives_ypncp stm if dcpst == 3, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph3) title("Previously partnered") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

grc1leg graph1 graph2 graph3 ,  ///
	title("Receives Capital Income") ///
	subtitle("In initial education spell") ///
	legendfrom(graph1) rows(2) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who are observed in their initial education spell, aged 16+ years old." "Initial education spell defined generously. Predictions are the average over 20 random draws.", size(vsmall))	
	
graph export ///
"$dir_validation_graphs/income/int_validation_I3a_selection_capital_init_edu_ts_all_both_partnership.png", ///
	as(png) replace width(2560) height(1440) //quality(100)

restore 

graph drop _all 			
	
		
* I3b selection - capital income, left initial education spell 

use "$dir_validation_data/I3b_selection_sample", clear 

forvalues i = 0/19 {
	local my_seed = 12345 + `i'  
    set seed `my_seed' 	
	gen rnd = runiform() 	
	gen pred_receives_ypncp`i' = 0 
	replace pred_receives_ypncp`i' = 1 if inrange(p,rnd,1)
	drop rnd
}

keep if in_sample == 1 

replace stm = 2000 + stm 
egen pred_receives_ypncp = rowmean(pred_receives_ypncp0-pred_receives_ypncp19)

* Raw prediction vs observed
twoway ///
	(histogram pred_receives_ypncp0, color(red)) ///
	(histogram receives_ypncp, color(none) lcolor(black)), ///
	xtitle (Receives capital income) ///
	legend(lab(1 "Predicted") lab( 2 "Observed")) name(levels, replace) ///
	title("Receives Capital Income") ///
	subtitle("Left initial education spell") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	graphregion(color(white)) ///
	legend(size(small)) ///
	note("Notes: Predicted vs observed of dummy indicating capital income is recieved. Estimation sample plotted. Sample contains all" "individual age 16+, who have left their initial education spell. Initial education spell defined generously.", size(vsmall))

graph export "$dir_validation_graphs/income/int_validation_I3b_selection_capital_left_edu_hist_all.png", ///
	as(png) replace width(2560) height(1440) //quality(100)

	
* Year 
preserve

collapse (mean) receives_ypncp pred_receives_ypncp  [aw = dwt], by(stm)

twoway ///
(line pred_receives_ypncp stm, sort color(green) legend(label(1 "Predicted"))) ///
(line receives_ypncp stm, sort color(green) color(green%20) lpattern(dash) ///
	legend(label(2 "Observed"))), ///
title("Receives Captial Income") ///
	subtitle("Left initial education spell") ///
	xtitle("Year") ytitle("Share") ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who have left their initial education spell, aged 16+ years old." "Initial education spell defined generously. Predictions are the average over 20 random draws.", size(vsmall))	
	
graph export "$dir_validation_graphs/income/int_validation_I3b_selection_capital_left_edu_ts_all_both.png", ///
	as(png) replace width(2560) height(1440) //quality(100)
	
	
restore	
	
graph drop _all	
	

* By gender 	
preserve
	
collapse (mean) receives_ypncp pred_receives_ypncp  [aw = dwt], by(stm dgn)
	
twoway ///
(line pred_receives_ypncp stm if dgn == 0, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line receives_ypncp stm if dgn == 0, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph1) title("Females") xtitle("Year") ytitle("Share") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) 

twoway ///
(line pred_receives_ypncp stm if dgn == 1, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line receives_ypncp stm if dgn == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph2) title("Males") xtitle("Year") ytitle("Share")  ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) 

grc1leg graph1 graph2 ,  ///
	title("Receives Captial Income ") ///
	subtitle("Left initial education spell") ///
	legendfrom(graph1) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who have left their initial education spell, aged 16+ years old." "Initial education spell defined generously. Predictions are the average over 20 random draws.", size(vsmall))	
	
graph export "$dir_validation_graphs/income/int_validation_I3b_selection_capital_left_edu_ts_all_gender.png", ///
	as(png) replace width(2560) height(1440) //quality(100)
	
restore 

graph drop _all 
	
	
* Share by age 
preserve
	
collapse (mean) receives_ypncp pred_receives_ypncp  [aw = dwt], by(dag)

twoway ///
(line pred_receives_ypncp dag, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line receives_ypncp dag, sort color(green) color(green%20) lpattern(dash) ///
	legend(label(2 "Observed"))), ///
title("Receives Capital Income") subtitle("In initial education spell, share by age") ///
	xtitle("Age") ///
	ytitle("Share") xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who have their initial education spell, aged 16+ years old." "Initial education spell defined generously. Predictions are the average over 20 random draws.", size(vsmall))	

graph export "$dir_validation_graphs/income/int_validation_I3b_selection_capital_left_edu_share_age.png", ///
	as(png) replace width(2560) height(1440) //quality(100)
	
restore
	
graph drop _all	
	
	
* Hh income 	
preserve
	
collapse (mean) receives_ypncp pred_receives_ypncp  [aw = dwt], by(ydses_c5 stm)

twoway ///
(line pred_receives_ypncp stm if ydses_c5 == 1, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line receives_ypncp stm if ydses_c5 == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph1) title("First quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_receives_ypncp stm if ydses_c5 == 2, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line receives_ypncp stm if ydses_c5 == 2, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph2) title("Second quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_receives_ypncp stm if ydses_c5 == 3, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line receives_ypncp stm if ydses_c5 == 3, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph3) title("Third quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_receives_ypncp stm if ydses_c5 == 4, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line receives_ypncp stm if ydses_c5 == 4, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph4) title("Fourth quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_receives_ypncp stm if ydses_c5 == 5, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line receives_ypncp stm if ydses_c5 == 5, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph5) title("Fifth quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

grc1leg graph1 graph2 graph3 graph4 graph5,  ///
	title("Receives Capital Income ") ///
	subtitle("Left initial education spell, by hh disposable income") ///
	legendfrom(graph1) rows(2) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who have left their initial education spell, aged 16+ years old." "Initial education spell defined generously. Predictions are the average over 20 random draws.", size(vsmall))	
	
graph export "$dir_validation_graphs/income/int_validation_I3b_selection_capital_left_edu_ts_all_both_income.png", ///
	as(png) replace width(2560) height(1440) //quality(100)

restore
	
graph drop _all		
	
	
* Marital status 
preserve
	
collapse (mean) receives_ypncp pred_receives_ypncp  [aw = dwt], by(dcpst stm)
	
twoway ///
(line pred_receives_ypncp stm if dcpst == 1, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line receives_ypncp stm if dcpst == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph1) title("Partnered") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_receives_ypncp stm if dcpst == 2, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line receives_ypncp stm if dcpst == 2, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph2) title("Single") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_receives_ypncp stm if dcpst == 3, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line receives_ypncp stm if dcpst == 3, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph3) title("Previously partnered") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

grc1leg graph1 graph2 graph3 ,  ///
	title("Receives Capital Income") ///
	subtitle("Left initial education spell") ///
	legendfrom(graph1) rows(2) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who have their initial education spell, aged 16+ years old." "Initial education spell defined generously. Predictions are the average over 20 random draws.", size(vsmall))	
	
graph export ///
"$dir_validation_graphs/income/int_validation_I3b_selection_capital_left_edu_ts_all_both_partnership.png", ///
	as(png) replace width(2560) height(1440) //quality(100)

restore 

graph drop _all 			
	
	
******************************************	
* I3a amount - in initial education spell 
******************************************
use "$dir_validation_data/I3a_level_sample", clear

keep if in_sample == 1 

* Obtain predicted log amount 
gen pred_ln_ypncp = p 

* Obtain random component 
cap drop epsilon
gen epsilon = rnormal()*sigma

* Convert into level with random component 
gen pred_ypncp = exp(pred_ln_ypncp + epsilon) 

* Trim predictions
sum pred_ypncp, d
replace pred_ypncp = . if pred_ypncp < r(p1) | pred_ypncp > r(p99)

twoway (hist pred_ypncp, width(1) color(green)) ///
	(hist ypncp_lvl, width(1) color(none) lcolor(black)), ///
	xtitle (Capital income (GBP)) ///
	legend(lab(1 "Predicted") lab( 2 "Observed")) name(levels, replace) ///
	title("Capital Income Amount") ///
	subtitle("In initial education spell") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	graphregion(color(white)) ///
	legend(size(small)) ///
	note("Notes: Predicted vs observed of capital income received. Estimation sample plotted. Sample contains all" "individual age 16+, who are in their initial education spell. Initial education spell defined generously. GBP per year, in 2015 prices." "Top and bottom percentiles of predicted trimmed.", size(vsmall))

graph export "$dir_validation_graphs/income/int_validation_I3a_amount_capital_init_edu_hist_all.png", ///
	as(png) replace width(2560) height(1440) //quality(100)

graph drop _all
	
	
* By gender 

* Males 
twoway (hist pred_ypncp if dgn == 1, width(1) color(green) ///
	legend(lab(1 "Predicted"))) ///
(histogram ypncp_lvl if dgn == 1, width(1) color(none) lcolor(black) ///
	legend(lab( 2 "Observed"))), ///
	subtitle("Males") name(graph1, replace) ///
	xtitle (Capital income (GBP)) ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	graphregion(color(white)) 
	
* Females 
twoway (hist pred_ypncp if dgn == 0, width(1) color(green) ///
	legend(lab(1 "Predicted"))) ///
(histogram ypncp_lvl if dgn == 0, width(1) color(none) lcolor(black) ///
	legend(lab( 2 "Observed"))), ///
	subtitle("Females") name(graph2, replace) ///
	xtitle (Capital income (GBP)) ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	graphregion(color(white)) 
	
	
grc1leg graph1 graph2 ,  ///
	title("Capital Income Amount") ///
	subtitle("In initial education spell") ///
	legendfrom(graph1) rows(1) ///
	graphregion(color(white)) ///
	note("Notes: Predicted vs observed of capital income received. Estimation sample plotted. Sample contains all" "individual age 16+, who are in their initial education spell. Initial education spell defined generously. GBP per year, in 2015 prices." "Top and bottom percentiles of predicted trimmed.", size(vsmall))

graph export "$dir_validation_graphs/income/int_validation_I3a_amount_capital_init_edu_hist_all_gender.png", ///
	as(png) replace width(2560) height(1440) //quality(100)

graph drop _all 	
	
**********************************************
* I3b amount - left initial education spell 
**********************************************
use "$dir_validation_data/I3b_level_sample", clear

keep if in_sample == 1 

* Obtain predicted log amount 
gen pred_ln_ypncp = p 

* Obtain random component 
cap drop epsilon
gen epsilon = rnormal()*sigma

* Convert into level with random component 
gen pred_ypncp = exp(pred_ln_ypncp + epsilon) 

* Trim predictions
sum pred_ypncp, d
replace pred_ypncp = . if pred_ypncp < r(p1) | pred_ypncp > r(p99)

twoway (hist pred_ypncp, width(1) color(green)) ///
	(hist ypncp_lvl, width(1) color(none) lcolor(black)), ///
	xtitle (Capital income (GBP)) ///
	legend(lab(1 "Predicted") lab( 2 "Observed")) name(levels, replace) ///
	title("Capital Income Amount") ///
	subtitle("Left initial education spell") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	graphregion(color(white)) ///
	legend(size(small)) ///
	note("Notes: Predicted vs observed of capital income received. Estimation sample plotted. Sample contains all" "individual age 16+, who have left their initial education spell. Initial education spell defined generously. GBP per year, in 2015 prices." "Top and bottom percentiles of predicted trimmed.", size(vsmall))

graph export "$dir_validation_graphs/income/int_validation_I3b_amount_capital_left_edu_hist_all.png", ///
	as(png) replace width(2560) height(1440) //quality(100)

graph drop _all
	
	
* By gender 

* Males 
twoway (hist pred_ypncp if dgn == 1, width(1) color(green) ///
	legend(lab(1 "Predicted"))) ///
(histogram ypncp_lvl if dgn == 1, width(1) color(none) lcolor(black) ///
	legend(lab( 2 "Observed"))), ///
	subtitle("Males") name(graph1, replace) ///
	xtitle (Capital income (GBP)) ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	graphregion(color(white)) 
	
* Females 
twoway (hist pred_ypncp if dgn == 0, width(1) color(green) ///
	legend(lab(1 "Predicted"))) ///
(histogram ypncp_lvl if dgn == 0, width(1) color(none) lcolor(black) ///
	legend(lab( 2 "Observed"))), ///
	subtitle("Females") name(graph2, replace) ///
	xtitle (Capital income (GBP)) ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	graphregion(color(white)) 
	
	
grc1leg graph1 graph2 ,  ///
	title("Capital Income Amount") ///
	subtitle("Left initial education spell") ///
	legendfrom(graph1) rows(1) ///
	graphregion(color(white)) ///
	note("Notes: Predicted vs observed of capital income received. Estimation sample plotted. Sample contains all" "individual age 16+, who have left their initial education spell. Initial education spell defined generously. GBP per year, in 2015 prices." "Top and bottom percentiles of predicted trimmed.", size(vsmall))

graph export "$dir_validation_graphs/income/int_validation_I3b_amount_capital_left_edu_hist_all_gender.png", ///
	as(png) replace width(2560) height(1440) //quality(100)


* By education 	
	
* Low 
twoway (hist pred_ypncp if deh_c3 == 3, width(1) color(green) ///
	legend(lab(1 "Predicted"))) ///
(histogram ypncp_lvl if dgn == 1, width(1) color(none) lcolor(black) ///
	legend(lab( 2 "Observed"))), ///
	subtitle("Low education") name(graph1, replace) ///
	xtitle (Capital income (GBP)) ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	graphregion(color(white)) 
	
* Medium
twoway (hist pred_ypncp if deh_c3 == 2, width(1) color(green) ///
	legend(lab(1 "Predicted"))) ///
(histogram ypncp_lvl if dgn == 0, width(1) color(none) lcolor(black) ///
	legend(lab( 2 "Observed"))), ///
	subtitle("Medium education") name(graph2, replace) ///
	xtitle (Capital income (GBP)) ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	graphregion(color(white)) 
	
* High	
twoway (hist pred_ypncp if deh_c3 == 1, width(1) color(green) ///
	legend(lab(1 "Predicted"))) ///
(histogram ypncp_lvl if dgn == 0, width(1) color(none) lcolor(black) ///
	legend(lab( 2 "Observed"))), ///
	subtitle("High education") name(graph3, replace) ///
	xtitle (Capital income (GBP)) ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	graphregion(color(white)) 	
	
	
grc1leg graph1 graph2 graph3 ,  ///
	title("Capital Income Amount") ///
	subtitle("Left initial education spell") ///
	legendfrom(graph1) rows(2) ///
	graphregion(color(white)) ///
	note("Notes: Predicted vs observed of capital income received. Estimation sample plotted. Sample contains all" "individual age 16+, who have left their initial education spell. Initial education spell defined generously. GBP per year, in 2015 prices." "Top and bottom percentiles of predicted trimmed.", size(vsmall))

graph export "$dir_validation_graphs/income/int_validation_I3b_amount_capital_left_edu_hist_all_edu.png", ///
	as(png) replace width(2560) height(1440) //quality(100)
	
********************************
* I4b: Amount of pension income.
******************************** 

use "$dir_validation_data/I4b_level_sample", clear

keep if in_sample == 1 

* Obtain predicted log amount 
gen pred_ln_ypnoab = p 

* Obtain random component 
cap drop epsilon
gen epsilon = rnormal()*sigma

* Convert into level with random component 
gen pred_ypnoab = exp(pred_ln_ypnoab + epsilon) 

* Trim predictions
sum pred_ypnoab, d
replace pred_ypnoab = . if pred_ypnoab < r(p1) | pred_ypnoab > r(p99)

twoway (hist pred_ypnoab, width(1) color(green)) ///
	(hist ypnoab_lvl, width(1) color(none) lcolor(black)), ///
	xtitle (Private Pension Income (GBP)) ///
	legend(lab(1 "Predicted") lab( 2 "Observed")) name(levels, replace) ///
	title("Private Pension Income Amount") ///
	subtitle("Retired in the past year") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	graphregion(color(white)) ///
	legend(size(small)) ///
	note("Notes: Predicted vs observed of Private Pension Income received. Estimation sample plotted. Sample contains all" "individual who were retired in the previous year. GBP per year, in 2015 prices." "Top and bottom percentiles of predicted trimmed.", size(vsmall))

graph export "$dir_validation_graphs/income/int_validation_I4b_amount_pension_retired_hist_all.png", ///
	as(png) replace width(2560) height(1440) //quality(100)

graph drop _all
	
	
* By gender 

* Males 
twoway (hist pred_ypnoab if dgn == 1, width(1) color(green) ///
	legend(lab(1 "Predicted"))) ///
(histogram ypnoab_lvl if dgn == 1, width(1) color(none) lcolor(black) ///
	legend(lab( 2 "Observed"))), ///
	subtitle("Males") name(graph1, replace) ///
	xtitle (Private Pension Income (GBP)) ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	graphregion(color(white)) 
	
* Females 
twoway (hist pred_ypnoab if dgn == 0, width(1) color(green) ///
	legend(lab(1 "Predicted"))) ///
(histogram ypnoab_lvl if dgn == 0, width(1) color(none) lcolor(black) ///
	legend(lab( 2 "Observed"))), ///
	subtitle("Females") name(graph2, replace) ///
	xtitle (Private Pension Income (GBP)) ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	graphregion(color(white)) 
	
	
grc1leg graph1 graph2 ,  ///
	title("Private Pension Income Amount") ///
	subtitle("Retired in the past year") ///
	legendfrom(graph1) rows(1) ///
	graphregion(color(white)) ///
	note("Notes: Predicted vs observed of Private Pension Income received. Estimation sample plotted. Sample contains all" "individuals who were retired in the previous year. GBP per year, in 2015 prices." "Top and bottom percentiles of predicted trimmed.", size(vsmall))

graph export "$dir_validation_graphs/income/int_validation_I4b_amount_pension_retired_hist_all_gender.png", ///
	as(png) replace width(2560) height(1440) //quality(100)


* By education 	
	
* Low 
twoway (hist pred_ypnoab if deh_c3 == 3, width(1) color(green) ///
	legend(lab(1 "Predicted"))) ///
(histogram ypnoab_lvl if dgn == 1, width(1) color(none) lcolor(black) ///
	legend(lab( 2 "Observed"))), ///
	subtitle("Low education") name(graph1, replace) ///
	xtitle (Private Pension Income (GBP)) ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	graphregion(color(white)) 
	
* Medium
twoway (hist pred_ypnoab if deh_c3 == 2, width(1) color(green) ///
	legend(lab(1 "Predicted"))) ///
(histogram ypnoab_lvl if dgn == 0, width(1) color(none) lcolor(black) ///
	legend(lab( 2 "Observed"))), ///
	subtitle("Medium education") name(graph2, replace) ///
	xtitle (Private Pension Income (GBP)) ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	graphregion(color(white)) 
	
* High	
twoway (hist pred_ypnoab if deh_c3 == 1, width(1) color(green) ///
	legend(lab(1 "Predicted"))) ///
(histogram ypnoab_lvl if dgn == 0, width(1) color(none) lcolor(black) ///
	legend(lab( 2 "Observed"))), ///
	subtitle("High education") name(graph3, replace) ///
	xtitle (Private Pension Income (GBP)) ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	graphregion(color(white)) 	
	
	
grc1leg graph1 graph2 graph3 ,  ///
	title("Private Pension Income Amount") ///
	subtitle("Retired in the past year") ///
	legendfrom(graph1) rows(2) ///
	graphregion(color(white)) ///
	note("Notes: Predicted vs observed of Private Pension Income received. Estimation sample plotted. Sample contains all" "individuals who were retired in the previous year. GBP per year, in 2015 prices." "Top and bottom percentiles of predicted trimmed.", size(vsmall))

graph export "$dir_validation_graphs/income/int_validation_I4b_amount_pension_retired_hist_all_edu.png", ///
	as(png) replace width(2560) height(1440) //quality(100)	
		

***********************************************************************
* I5a selection - private pension income, not retired in the past year  
***********************************************************************
use "$dir_validation_data/I5a_selection_sample", clear 

forvalues i = 0/19 {
	local my_seed = 12345 + `i'  
    set seed `my_seed' 	
	gen rnd = runiform() 	
	gen pred_receives_ypnoab`i' = 0 
	replace pred_receives_ypnoab`i' = 1 if inrange(p,rnd,1)
	drop rnd
}

keep if in_sample == 1 

replace stm = 2000 + stm 
egen pred_receives_ypnoab = rowmean(pred_receives_ypnoab0-pred_receives_ypnoab19)

* Raw prediction vs observed
twoway ///
	(histogram pred_receives_ypnoab0, color(red)) ///
	(histogram receives_ypnoab, color(none) lcolor(black)), ///
	xtitle (Receives private pension income) ///
	legend(lab(1 "Predicted") lab( 2 "Observed")) name(levels, replace) ///
	title("Receives private pension income") ///
	subtitle("In initial education spell") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	graphregion(color(white)) ///
	legend(size(small)) ///
	note("Notes: Predicted vs observed of dummy indicating private pension income is recieved. Estimation sample plotted. Sample contains all" "individuals who were not retired last year.", size(vsmall))

graph export "$dir_validation_graphs/income/int_validation_I5a_selection_private_pension_notretired_hist_all.png", ///
	as(png) replace width(2560) height(1440) //quality(100)

	
* Year 
preserve

collapse (mean) receives_ypnoab pred_receives_ypnoab  [aw = dwt], by(stm)

twoway ///
(line pred_receives_ypnoab stm, sort color(green) legend(label(1 "Predicted"))) ///
(line receives_ypnoab stm, sort color(green) color(green%20) lpattern(dash) ///
	legend(label(2 "Observed"))), ///
title("Receives Captial Income") ///
	subtitle("In initial education spell") ///
	xtitle("Year") ytitle("Share") ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals individuals who were not retired last year." "Predictions are the average over 20 random draws.", size(vsmall))	
	
graph export "$dir_validation_graphs/income/int_validation_I5a_selection_private_pension_notretired_ts_all_both.png", ///
	as(png) replace width(2560) height(1440) //quality(100)
	
	
restore	
	
graph drop _all	
	

* By gender 	
preserve
	
collapse (mean) receives_ypnoab pred_receives_ypnoab  [aw = dwt], by(stm dgn)
	
twoway ///
(line pred_receives_ypnoab stm if dgn == 0, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line receives_ypnoab stm if dgn == 0, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph1) title("Females") xtitle("Year") ytitle("Share") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) 

twoway ///
(line pred_receives_ypnoab stm if dgn == 1, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line receives_ypnoab stm if dgn == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph2) title("Males") xtitle("Year") ytitle("Share")  ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) 

grc1leg graph1 graph2 ,  ///
	title("Receives Captial Income ") ///
	subtitle("In initial education spell") ///
	legendfrom(graph1) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals individuals who were not retired last year." "Predictions are the average over 20 random draws.", size(vsmall))	
	
graph export "$dir_validation_graphs/income/int_validation_I5a_selection_private_pension_notretired_ts_all_gender.png", ///
	as(png) replace width(2560) height(1440) //quality(100)
	
restore 

graph drop _all 
	
	
* Share by age 
preserve
	
collapse (mean) receives_ypnoab pred_receives_ypnoab  [aw = dwt], by(dag)

twoway ///
(line pred_receives_ypnoab dag, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line receives_ypnoab dag, sort color(green) color(green%20) lpattern(dash) ///
	legend(label(2 "Observed"))), ///
title("Receives private pension income") subtitle("In initial education spell, share by age") ///
	xtitle("Age") ///
	ytitle("Share") xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who individuals who were not retired last year." "Predictions are the average over 20 random draws.", size(vsmall))	

graph export "$dir_validation_graphs/income/int_validation_I5a_selection_private_pension_notretired_share_age.png", ///
	as(png) replace width(2560) height(1440) //quality(100)
	
restore
	
graph drop _all	
	
	
* Hh income 	
preserve
	
collapse (mean) receives_ypnoab pred_receives_ypnoab  [aw = dwt], by(ydses_c5 stm)

twoway ///
(line pred_receives_ypnoab stm if ydses_c5 == 1, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line receives_ypnoab stm if ydses_c5 == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph1) title("First quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_receives_ypnoab stm if ydses_c5 == 2, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line receives_ypnoab stm if ydses_c5 == 2, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph2) title("Second quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_receives_ypnoab stm if ydses_c5 == 3, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line receives_ypnoab stm if ydses_c5 == 3, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph3) title("Third quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_receives_ypnoab stm if ydses_c5 == 4, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line receives_ypnoab stm if ydses_c5 == 4, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph4) title("Fourth quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_receives_ypnoab stm if ydses_c5 == 5, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line receives_ypnoab stm if ydses_c5 == 5, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph5) title("Fifth quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

grc1leg graph1 graph2 graph3 graph4 graph5,  ///
	title("Receives private pension income ") ///
	subtitle("In initial education spell, by hh disposable income") ///
	legendfrom(graph1) rows(2) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who were not retired last year." "Predictions are the average over 20 random draws.", size(vsmall))	
	
graph export "$dir_validation_graphs/income/int_validation_I5a_selection_private_pension_notretired_ts_all_both_income.png", ///
	as(png) replace width(2560) height(1440) //quality(100)

restore
	
graph drop _all		
	
	
* Marital status 
preserve
	
collapse (mean) receives_ypnoab pred_receives_ypnoab  [aw = dwt], by(dcpst stm)
	
twoway ///
(line pred_receives_ypnoab stm if dcpst == 1, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line receives_ypnoab stm if dcpst == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph1) title("Partnered") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_receives_ypnoab stm if dcpst == 2, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line receives_ypnoab stm if dcpst == 2, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph2) title("Single") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_receives_ypnoab stm if dcpst == 3, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line receives_ypnoab stm if dcpst == 3, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph3) title("Previously partnered") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

grc1leg graph1 graph2 graph3 ,  ///
	title("Receives private pension income") ///
	subtitle("In initial education spell") ///
	legendfrom(graph1) rows(2) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who were not retired last year." "Predictions are the average over 20 random draws.", size(vsmall))	
	
graph export ///
"$dir_validation_graphs/income/int_validation_I5a_selection_private_pension_notretired_ts_all_both_partnership.png", ///
	as(png) replace width(2560) height(1440) //quality(100)

restore 

graph drop _all 			
	
********************************
* I5b: Amount of pension income.
******************************** 

use "$dir_validation_data/I5a_level_sample", clear

keep if in_sample == 1 

* Obtain predicted log amount 
gen pred_ln_ypnoab = p 

* Obtain random component 
cap drop epsilon
gen epsilon = rnormal()*sigma

* Convert into level with random component 
gen pred_ypnoab = exp(pred_ln_ypnoab + epsilon) 

* Trim predictions
sum pred_ypnoab, d
replace pred_ypnoab = . if pred_ypnoab < r(p1) | pred_ypnoab > r(p99)

twoway (hist pred_ypnoab, width(1) color(green)) ///
	(hist ypnoab_lvl, width(1) color(none) lcolor(black)), ///
	xtitle (Private Pension Income (GBP)) ///
	legend(lab(1 "Predicted") lab( 2 "Observed")) name(levels, replace) ///
	title("Private Pension Income Amount") ///
	subtitle("Retired in the past year") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	graphregion(color(white)) ///
	legend(size(small)) ///
	note("Notes: Predicted vs observed of Private Pension Income received. Estimation sample plotted. Sample contains all" "individual who were not retired in the previous year. GBP per year, in 2015 prices." "Top and bottom percentiles of predicted trimmed.", size(vsmall))

graph export "$dir_validation_graphs/income/int_validation_I5a_amount_pension_retired_hist_all.png", ///
	as(png) replace width(2560) height(1440) //quality(100)

graph drop _all
	
	
* By gender 

* Males 
twoway (hist pred_ypnoab if dgn == 1, width(1) color(green) ///
	legend(lab(1 "Predicted"))) ///
(histogram ypnoab_lvl if dgn == 1, width(1) color(none) lcolor(black) ///
	legend(lab( 2 "Observed"))), ///
	subtitle("Males") name(graph1, replace) ///
	xtitle (Private Pension Income (GBP)) ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	graphregion(color(white)) 
	
* Females 
twoway (hist pred_ypnoab if dgn == 0, width(1) color(green) ///
	legend(lab(1 "Predicted"))) ///
(histogram ypnoab_lvl if dgn == 0, width(1) color(none) lcolor(black) ///
	legend(lab( 2 "Observed"))), ///
	subtitle("Females") name(graph2, replace) ///
	xtitle (Private Pension Income (GBP)) ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	graphregion(color(white)) 
	
	
grc1leg graph1 graph2 ,  ///
	title("Private Pension Income Amount") ///
	subtitle("Retired in the past year") ///
	legendfrom(graph1) rows(1) ///
	graphregion(color(white)) ///
	note("Notes: Predicted vs observed of Private Pension Income received. Estimation sample plotted. Sample contains all" "individuals who were not retired in the previous year. GBP per year, in 2015 prices." "Top and bottom percentiles of predicted trimmed.", size(vsmall))

graph export "$dir_validation_graphs/income/int_validation_I5a_amount_pension_retired_hist_all_gender.png", ///
	as(png) replace width(2560) height(1440) //quality(100)


* By education 	
	
* Low 
twoway (hist pred_ypnoab if deh_c3 == 3, width(1) color(green) ///
	legend(lab(1 "Predicted"))) ///
(histogram ypnoab_lvl if dgn == 1, width(1) color(none) lcolor(black) ///
	legend(lab( 2 "Observed"))), ///
	subtitle("Low education") name(graph1, replace) ///
	xtitle (Private Pension Income (GBP)) ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	graphregion(color(white)) 
	
* Medium
twoway (hist pred_ypnoab if deh_c3 == 2, width(1) color(green) ///
	legend(lab(1 "Predicted"))) ///
(histogram ypnoab_lvl if dgn == 0, width(1) color(none) lcolor(black) ///
	legend(lab( 2 "Observed"))), ///
	subtitle("Medium education") name(graph2, replace) ///
	xtitle (Private Pension Income (GBP)) ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	graphregion(color(white)) 
	
* High	
twoway (hist pred_ypnoab if deh_c3 == 1, width(1) color(green) ///
	legend(lab(1 "Predicted"))) ///
(histogram ypnoab_lvl if dgn == 0, width(1) color(none) lcolor(black) ///
	legend(lab( 2 "Observed"))), ///
	subtitle("High education") name(graph3, replace) ///
	xtitle (Private Pension Income (GBP)) ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	graphregion(color(white)) 	
	
	
grc1leg graph1 graph2 graph3 ,  ///
	title("Private Pension Income Amount") ///
	subtitle("Retired in the past year") ///
	legendfrom(graph1) rows(2) ///
	graphregion(color(white)) ///
	note("Notes: Predicted vs observed of Private Pension Income received. Estimation sample plotted. Sample contains all" "individuals who were not retired in the previous year. GBP per year, in 2015 prices." "Top and bottom percentiles of predicted trimmed.", size(vsmall))

graph export "$dir_validation_graphs/income/int_validation_I5a_amount_pension_retired_hist_all_edu.png", ///
	as(png) replace width(2560) height(1440) //quality(100)	
		

	
	graph drop _all 	
