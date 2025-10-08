********************************************************************************
* PROJECT:  		ESPON 
* SECTION:			Fertility
* OBJECT: 			Internal validation
* AUTHORS:			Ashley Burdett, Daria Popova  
* LAST UPDATE:		July 2025
* COUNTRY: 			UK

* NOTES: 			Compares predicted values to the observed values of the 
* 					2 fertility processes estimated. 
* 					Individual heterogeneity added to the standard predicted 
* 					values using a random draw like in stochasitic 
* 					imputation. The pooled mean is obtained as in multiple 
* 					imputation by repeating the random draw 20 times for each 
* 					process. 
* 
* 					Run after "reg_fertility.do"
********************************************************************************

**********************************************
* F1a - Having a child, in initial edu spell * 
**********************************************

* Overall 
use "$dir_validation_data/F1a_sample", clear 

set seed 12345
gen rnd = runiform() 	
gen pred_dchpd = 0 
replace pred_dchpd = 1 if inrange(p,rnd,1)

keep if in_sample == 1 

twoway ///
	(histogram pred_dchpd, color(red)) ///
	(histogram dchpd, color(none) lcolor(black)), ///
	xtitle (Had child) ///
	legend(lab(1 "Predicted") lab( 2 "Observed")) name(levels, replace) ///
	title("Fertility in initial education spell") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	graphregion(color(white)) ///
	legend(size(small)) ///
	note("Notes: Predicted vs observed of dummy indicating a female has a new born child. Estimation sample plotted. Sample contains females" "who are in their  initial education spell and fertile (18-30). Initial education spell defined generously.", size(vsmall))

graph export "$dir_validation_graphs/fertility/int_validation_${country}_F1a_fertility_init_edu_hist_18_30.png", ///
	as(png) replace width(2560) height(1440) //quality(100)
	
	
* Year 
use "$dir_validation_data/F1a_sample", clear

// construct multiple versions of the predicted outcome allowing for different 
// random draws 
forvalues i = 0/19 {
	local my_seed = 12345 + `i'  
    set seed `my_seed' 	
	gen rnd = runiform() 	
	gen pred_dchpd`i' = 0 
	replace pred_dchpd`i' = 1 if inrange(p,rnd,1)
	drop rnd
}

keep if in_sample == 1 

preserve

// for each iteration calculate the share that leave edu 
collapse (mean) dchpd pred_dchpd* [aw = dwt], by(stm)

order pred_dchpd*

// take the average across datasets 
egen pred_dchpd = rowmean(pred_dchpd0-pred_dchpd19)
// replace stm= 2000 + stm 

twoway ///
(line pred_dchpd stm, sort color(green) legend(label(1 "Predicted"))) ///
(line dchpd stm, sort color(green) color(green%20) lpattern(dash) ///
	legend(label(2 "Observed"))), ///
title("Fertility in initial education spell") xtitle("Year") ytitle("Share") ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Share of females that have a new born child. Estimation sample plotted. Sample contains females who are in their" "initial education spell and fertile (18-30). Initial education spell defined generously.", size(vsmall))

graph export "$dir_validation_graphs/fertility/int_validation_${country}_F1a_fertility_init_edu_ts_18_30.png", ///
	as(png) replace width(2560) height(1440) //quality(100)
 
restore  
 
 
* Age
preserve

collapse (mean) dchpd pred_dchpd* [aw = dwt], by(dag)

order pred_dchpd*

egen pred_dchpd = rowmean(pred_dchpd0-pred_dchpd19)

twoway ///
(line pred_dchpd dag, sort color(green) legend(label(1 "Predicted"))) ///
(line dchpd dag, sort color(green) color(green%20) lpattern(dash) ///
	legend(label(2 "Observed"))), ///
title("Fertility in initial education spell") ///
	subtitle("Share by age") ///
	xtitle("Age") ///
	ytitle("Share") xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: Share of females that have a new born child. Estimation sample plotted. Sample contains females who are in their" "initial education spell and fertile (18-30). Initial education spell defined generously.", size(vsmall))

graph export "$dir_validation_graphs/fertility/int_validation_${country}_F1a_fertility_init_edu_share_age.png", ///
	as(png) replace width(2560) height(1440) //quality(100)

restore


* Income 
preserve

collapse (mean) dchpd pred_dchpd* [aw = dwt], by(ydses_c5 stm)

order pred_dchpd*

egen pred_dchpd = rowmean(pred_dchpd0-pred_dchpd19)
// replace stm= 2000 + stm 

twoway ///
(line pred_dchpd stm if ydses_c5 == 1, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line dchpd stm if ydses_c5 == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph1) title("First quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))
 
twoway ///
(line pred_dchpd stm if ydses_c5 == 2, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dchpd stm if ydses_c5 == 2, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph2) title("Second quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_dchpd stm if ydses_c5 == 3, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dchpd stm if ydses_c5 == 3, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph3) title("Third quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_dchpd stm if ydses_c5 == 4, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dchpd stm if ydses_c5 == 4, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph4) title("Forth quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))
 
twoway ///
(line pred_dchpd stm if ydses_c5 == 5, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dchpd stm if ydses_c5 == 5, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph5) title("Fifth quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

grc1leg graph1 graph2 graph3 graph4 graph5,  ///
	title("Fertility in initial education spell") ///
	subtitle("By hh disposable income") ///
	legendfrom(graph1) rows(2) ///
	graphregion(color(white)) ///
	note("Notes: Share of females that have a new born child. Estimation sample plotted. Sample contains females who are in their" "initial education spell and fertile (18-30). Initial education spell defined generously.", size(vsmall))
	
graph export "$dir_validation_graphs/fertility/int_validation_${country}_F1a_fertility_init_edu_ts_18_30_both_income.png", ///
	as(png) replace width(2560) height(1440) //quality(100)	
	
graph drop _all 	
	
restore


************************************************
* F1b - Having a child, left initial edu spell *
************************************************

* Overall 
use "$dir_validation_data/F1b_sample", clear 

set seed 12345
gen rnd = runiform() 	
gen pred_dchpd = 0 
replace pred_dchpd = 1 if inrange(p,rnd,1)

keep if in_sample == 1 

twoway ///
	(histogram pred_dchpd, color(red)) ///
	(histogram dchpd, color(none) lcolor(black)), ///
	xtitle (Had child) ///
	legend(lab(1 "Predicted") lab( 2 "Observed")) name(levels, replace) ///
	title("Fertility") ///
	subtitle("Left initial education spell") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	graphregion(color(white)) ///
	legend(size(small)) ///
	note("Notes: Predicted vs observed of dummy indicating a female has a new born child. Estimation sample plotted. Sample"" contains females who have left their initial education spell and are in their fertile years (18-45). Initial education spell defined" "generously.", size(vsmall))

graph export "$dir_validation_graphs/fertility/int_validation_${country}_F1b_fertility_left_edu_hist_18_45.png", ///
	as(png) replace width(2560) height(1440) //quality(100)	

	
* Year 
use "$dir_validation_data/F1b_sample", clear

// construct multiple versions of the predicted outcome allowing for different 
// random draws 
forvalues i = 0/19 {
	local my_seed = 12345 + `i'  
    set seed `my_seed' 	
	gen rnd = runiform() 	
	gen pred_dchpd`i' = 0 
	replace pred_dchpd`i' = 1 if inrange(p,rnd,1)
	drop rnd
}

keep if in_sample == 1 

preserve

collapse (mean) dchpd pred_dchpd* [aw = dwt], by(stm)

order pred_dchpd*

egen pred_dchpd = rowmean(pred_dchpd0-pred_dchpd19)

// replace stm= 2000 + stm 

twoway ///
(line pred_dchpd stm, sort color(green) legend(label(1 "Predicted"))) ///
(line dchpd stm, sort color(green) color(green%20) lpattern(dash) ///
	legend(label(2 "Observed"))), ///
title("Fertility") ///
	subtitle("Left initial education spell") ///
	xtitle("Year") ytitle("Share") ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Share of females that have a new born child. Estimation sample plotted. Sample contains females who have left their" "initial education spell and are in their fertile years (18-45). Initial education spell defined generously.", size(vsmall))

graph export "$dir_validation_graphs/fertility/int_validation_${country}_F1b_fertility_left_edu_ts_18_45.png", ///
	as(png) replace width(2560) height(1440) //quality(100)
 
restore  
 
 
* Age
preserve

collapse (mean) dchpd pred_dchpd* [aw = dwt], by(dag)

order pred_dchpd*

egen pred_dchpd = rowmean(pred_dchpd0-pred_dchpd19)

twoway ///
(line pred_dchpd dag, sort color(green) legend(label(1 "Predicted"))) ///
(line dchpd dag, sort color(green) color(green%20) lpattern(dash) ///
	legend(label(2 "Observed"))), ///
title("Fertility ") ///
	subtitle("Left initial education spell, share by age") ///
	xtitle("Age") ///
	ytitle("Share") xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: Share of females that have a new born child. Estimation sample plotted. Sample contains females who have left their initial" "education spell and are in their fertile years (18-45). Initial education spell defined generously.", size(vsmall))

graph export "$dir_validation_graphs/fertility/int_validation_${country}_F1b_fertility_left_edu_share_age.png", ///
	as(png) replace width(2560) height(1440) //quality(100)
	
restore


* Income 
preserve

collapse (mean) dchpd pred_dchpd* [aw = dwt], by(ydses_c5 stm)

order pred_dchpd*

egen pred_dchpd = rowmean(pred_dchpd0-pred_dchpd19)

// replace stm= 2000 + stm 

twoway ///
(line pred_dchpd stm if ydses_c5 == 1, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line dchpd stm if ydses_c5 == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph1) title("First quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_dchpd stm if ydses_c5 == 2, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dchpd stm if ydses_c5 == 2, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph2) title("Second quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_dchpd stm if ydses_c5 == 3, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dchpd stm if ydses_c5 == 3, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph3) title("Third quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_dchpd stm if ydses_c5 == 4, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dchpd stm if ydses_c5 == 4, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph4) title("Fourth quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_dchpd stm if ydses_c5 == 5, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dchpd stm if ydses_c5 == 5, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph5) title("Fifth quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

grc1leg graph1 graph2 graph3 graph4 graph5,  ///
	title("Fertility") ///
	subtitle("Left initial education spell, by hh dispoable income") ///
	legendfrom(graph1) rows(2) ///
	graphregion(color(white)) ///
	note("Notes: Share of females that have a new born child. Estimation sample plotted. Sample contains females who have left their initial education" "spell and are in their fertile years (18-45). Initial education spell defined generously.", size(vsmall))

graph export "$dir_validation_graphs/fertility/int_validation_${country}_F1b_fertility_left_edu_ts_18_45_income.png", ///
	as(png) replace width(2560) height(1440) //quality(100)
	
graph drop _all 	
	
restore


* Education
preserve

collapse (mean) dchpd pred_dchpd* [aw = dwt], by(deh_c3 stm)

order pred_dchpd*

egen pred_dchpd = rowmean(pred_dchpd0-pred_dchpd19)

// replace stm= 2000 + stm 

twoway ///
(line pred_dchpd stm if deh_c3 == 1, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line dchpd stm if deh_c3 == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph1) title("High education") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_dchpd stm if deh_c3 == 2, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dchpd stm if deh_c3 == 2, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph2) title("Medium education") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_dchpd stm if deh_c3 == 3, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dchpd stm if deh_c3 == 3, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph3) title("Low education") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

grc1leg graph1 graph2 graph3,  ///
	title("Fertility") ///
	subtitle("Left initial education spell") ///
	legendfrom(graph1) rows(2) ///
	graphregion(color(white)) ///
	note("Notes: Share of females that have a new born child. Estimation sample plotted. Sample contains females who have left their initial education" "spell and are in their fertile years (18-45). Initial education spell defined generously.", size(vsmall))

graph export "$dir_validation_graphs/fertility/int_validation_${country}_F1b_fertility_left_edu_ts_18_45_edu.png", ///
	as(png) replace width(2560) height(1440) //quality(100)
	
graph drop _all 	
	
restore


* Marital status 
preserve

collapse (mean) dchpd pred_dchpd* [aw = dwt], by(dcpst stm)

order pred_dchpd*

egen pred_dchpd = rowmean(pred_dchpd0-pred_dchpd19)

// replace stm= 2000 + stm 

twoway ///
(line pred_dchpd stm if dcpst == 1, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line dchpd stm if dcpst == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph1) title("Partnered") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_dchpd stm if dcpst == 2, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dchpd stm if dcpst == 2, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph2) title("Single") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_dchpd stm if dcpst == 3, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dchpd stm if dcpst == 3, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph3) title("Previously partnered") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

grc1leg graph1 graph2 graph3,  ///
	title("Fertility ") ///
	subtitle("Left initial education spell") ///
	legendfrom(graph1) rows(2) ///
	graphregion(color(white)) ///
	note("Notes: Share of females that have a new born child. Estimation sample plotted.Sample contains females who have left their initial education" "spell and are in their fertile years (18-45). Initial education spell defined generously.", size(vsmall))

graph export "$dir_validation_graphs/fertility/int_validation_${country}_F1b_fertility_left_edu_ts_18_45_partnership.png", ///
	as(png) replace width(2560) height(1440) //quality(100)
	
	
graph drop _all 	
	
restore
