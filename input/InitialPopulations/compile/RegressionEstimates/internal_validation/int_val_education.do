********************************************************************************
* PROJECT:  		ESPON 
* SECTION:			Education
* OBJECT: 			Internal validation
* AUTHORS:			Ashley Burdett, Daria Popova  
* LAST UPDATE:		May 2025
* COUNTRY: 			UK   

* NOTES: 			Compares predicted values to the observed values of the 
* 					3 education processes estimated. 
* 					Individual heterogeneity added to the standard predicted 
* 					values using a random draw like in stochasitic 
* 					imputation. The pooled mean is obtained as in multiple 
* 					imputation by repeating the random draw 20 times for each 
* 					process. 
* 
* 					Run after "reg_education.do"
********************************************************************************

*******************************************************
* E1a: Probability of Leaving Initial Education Spell *
*******************************************************

* Year 
use "$dir_validation_data/E1a_sample", clear

// construct multiple versions of the predicted outcome allowing for different 
// random draws 
forvalues i = 0/19 {
	local my_seed = 12345 + `i'  
    set seed `my_seed' 	
	gen rnd = runiform() 	
	gen pred_ded`i' = 0 
	replace pred_ded`i' = 1 if inrange(p,rnd,1)
	drop rnd
}

keep if in_sample == 1 

preserve

// for each iteration calculate the share that leave edu 
collapse (mean) ded pred_ded* [aw = dwt], by(stm)

order pred_ded*

// take the average across datasets 
egen pred_ded = rowmean(pred_ded0-pred_ded19)
replace stm = 2000 + stm 

twoway ///
(line pred_ded stm, sort color(green) legend(label(1 "Predicted"))) ///
(line ded stm, sort color(green) color(green%20) lpattern(dash) ///
	legend(label(2 "Observed"))), ///
title("Continues in Initial Education Spell") xtitle("Year") ytitle("Share") ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who are observed in their initial education spell, aged 16-29" "Initial education spell defined generously.", size(vsmall))

graph export "$dir_validation_graphs/education/int_validation_E1a_continues_edu_ts_16_29_both.png", ///
	as(png) replace width(2560) height(1440) //quality(100)
 
restore  


* Gender 
preserve
collapse (mean) ded pred_ded* [aw = dwt], by(dgn stm)

order pred_ded*

egen pred_ded = rowmean(pred_ded0-pred_ded19)

replace stm = 2000 + stm 

twoway ///
(line pred_ded stm if dgn == 0, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line ded stm if dgn == 0, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph1) title("Females") xtitle("Year") ytitle("Share") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) 


twoway ///
(line pred_ded stm if dgn == 1, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line ded stm if dgn == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph2) title("Males") xtitle("Year") ytitle("Share")  ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) 

grc1leg graph1 graph2 ,  ///
	title("Continues in Initial Education Spell") ///
	legendfrom(graph1) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who are observed in their initial education spell, aged 16-29. Initial education" "spell defined generously.", size(vsmall))
	
graph export "$dir_validation_graphs/education/int_validation_E1a_continues_edu_ts_16_29_gender.png", ///, ///
	as(png) replace width(2560) height(1440) //quality(100)
	
graph drop _all  

restore
 
 
* Age
preserve

collapse (mean) ded pred_ded* [aw = dwt], by(dag)

order pred_ded*

egen pred_ded = rowmean(pred_ded0-pred_ded19)

twoway ///
(line pred_ded dag, sort color(green) legend(label(1 "Predicted"))) ///
(line ded dag, sort color(green) color(green%20) lpattern(dash) ///
	legend(label(2 "Observed"))), ///
title("Continues in Initial Education Spell") subtitle("Share by age") ///
	xtitle("Age") ///
	ytitle("Share") xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who are observed in their initial education spell, aged 16-29." "Initial education spell defined generously.", size(vsmall))

graph export "$dir_validation_graphs/education/int_validation_E1a_continues_edu_share_age.png", ///
     as(png) replace width(2560) height(1440) //quality(100)
	
restore


* Income 
preserve

collapse (mean) ded pred_ded* [aw = dwt], by(ydses_c5 stm)

order pred_ded*

egen pred_ded = rowmean(pred_ded0-pred_ded19)

replace stm = 2000 + stm 

twoway ///
(line pred_ded stm if ydses_c5 == 1, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line ded stm if ydses_c5 == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph1) title("First quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_ded stm if ydses_c5 == 2, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line ded stm if ydses_c5 == 2, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph2) title("Second quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_ded stm if ydses_c5 == 3, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line ded stm if ydses_c5 == 3, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph3) title("Third quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_ded stm if ydses_c5 == 4, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line ded stm if ydses_c5 == 4, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph4) title("Fourth quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_ded stm if ydses_c5 == 5, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line ded stm if ydses_c5 == 5, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph5) title("Fifth quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

grc1leg graph1 graph2 graph3 graph4 graph5,  ///
	title("Continues in Initial Education Spell") ///
	subtitle("By hh dispoable income") ///
	legendfrom(graph1) rows(2) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who are observed in their initial education spell, aged 16-29. Initial education" "spell defined generously.", size(vsmall))

graph export ///
"$dir_validation_graphs/education/int_validation_E1a_continues_edu_ts_16_29_both_income.png", ///	
	as(png) replace width(2560) height(1440) //quality(100)
	
graph drop _all 	
	
restore


* Marital status 
preserve

collapse (mean) ded pred_ded* [aw = dwt], by(dcpst stm)

order pred_ded*

egen pred_ded = rowmean(pred_ded0-pred_ded19)

replace stm = 2000 + stm 

twoway ///
(line pred_ded stm if dcpst == 1, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line ded stm if dcpst == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph1) title("Partnered") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_ded stm if dcpst == 2, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line ded stm if dcpst == 2, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph2) title("Single") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_ded stm if dcpst == 3, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line ded stm if dcpst == 3, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph3) title("Previously partnered") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

grc1leg graph1 graph2 graph3 ,  ///
	title("Continues in Initial Education Spell") ///
	subtitle("By partnership status") ///
	legendfrom(graph1) rows(2) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who are observed in their initial education spell, aged 16-29. Initial education" "spell defined generously.", size(vsmall))	
	
graph export ///
"$dir_validation_graphs/education/int_validation_E1a_continues_edu_ts_16_29_both_partnership.png", ///	
	as(png) replace width(2560) height(1440) //quality(100)

		
graph drop _all 	
	
restore


**********************************************
* E1b: Probability of Returning to Education *
**********************************************

* Year
use "$dir_validation_data/E1b_sample", clear

forvalues i = 0/19 {
	local my_seed = 12345 + `i'  
    set seed `my_seed' 	
	gen rnd = runiform() 	
	gen pred_der`i' = 0 
	replace pred_der`i' = 1 if inrange(p,rnd,1)
	drop rnd
}

keep if in_sample == 1 

preserve

collapse (mean) der pred_der* [aw = dwt], by(stm)

order pred_der*

egen pred_der = rowmean(pred_der0-pred_der19)
replace stm = 2000 + stm 

twoway ///
(line pred_der stm, sort color(green) legend(label(1 "Predicted"))) ///
(line der stm, sort color(green) color(green%20) lpattern(dash) ///
	legend(label(2 "Observed"))), ///
title("Returns to Education") xtitle("Year") ytitle("Share") ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who are not observed in their initial education spell in their previous" "observation, aged 16-35. Initial education spell defined generously.", size(vsmall))

graph export "$dir_validation_graphs/education/int_validation_E1b_returns_edu_ts_16_35_both.png", ///
	as(png) replace width(2560) height(1440) //quality(100)
   
restore  


* Gender 
preserve

collapse (mean) der pred_der* [aw = dwt], by(dgn stm)

order pred_der*

egen pred_der = rowmean(pred_der0-pred_der19)

replace stm = 2000 + stm 

twoway ///
(line pred_der stm if dgn == 0, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line der stm if dgn == 0, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph1) title("Females") xtitle("Year") ytitle("Share") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_der stm if dgn == 1, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line der stm if dgn == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph2) title("Males") xtitle("Year") ytitle("Share") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

grc1leg graph1 graph2, ///
	title("Returns to education") ///
	legendfrom(graph1) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who are not observed in their initial education spell in their previous" "observation, aged 16-35. Initial education spell defined generously.", size(vsmall))

graph export "$dir_validation_graphs/education/int_validation_E1b_returns_edu_ts_16_35_gender.png", ///	
as(png) replace width(2560) height(1440) //quality(100)
	
graph drop _all  

restore
 
 
* Age
preserve

collapse (mean) der pred_der* [aw = dwt], by(dag)

order pred_der*

egen pred_der = rowmean(pred_der0-pred_der19)

twoway ///
(line pred_der dag, sort color(green) legend(label(1 "Predicted"))) ///
(line der dag, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
title("Returns to Education") subtitle("Share by age") ///
	xtitle("Age") ///
	ytitle("Share") xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who are not observed in their initial education spell in their previous" "observation, aged 16-35. Initial education spell defined generously.", size(vsmall))

graph export "$dir_validation_graphs/education/int_validation_E1b_returns_edu_share_age.png", ///	
	as(png) replace width(2560) height(1440) //quality(100)	

restore


* Income 
preserve

collapse (mean) der pred_der* [aw = dwt], by(ydses_c5 stm)

order pred_der*

egen pred_der = rowmean(pred_der0-pred_der19)
replace stm = 2000 + stm 

twoway ///
(line pred_der stm if ydses_c5 == 1, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line der stm if ydses_c5 == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph1) title("First quintile") xtitle("Year") ytitle("")  ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_der stm if ydses_c5 == 2, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line der stm if ydses_c5 == 2, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph2) title("Second quintile") xtitle("Year") ytitle("")  ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_der stm if ydses_c5 == 3, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line der stm if ydses_c5 == 3, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph3) title("Third quintile") xtitle("Year") ytitle("")  ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_der stm if ydses_c5 == 4, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line der stm if ydses_c5 == 4, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph4) title("Fourth quintile") xtitle("Year") ytitle("")  ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_der stm if ydses_c5 == 5, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line der stm if ydses_c5 == 5, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph5) title("Fifth quintile") xtitle("Year") ytitle("")  ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

grc1leg graph1 graph2 graph3 graph4 graph5, ///
	title("Returns to Education") ///
	subtitle("By hh disposable income") ///
	legendfrom(graph1) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who are not observed in their initial education spell in their previous observation," "aged 16-35. Initial education spell defined generously.", size(vsmall))

graph export ///
"$dir_validation_graphs/education/int_validation_E1b_returns_edu_ts_16_35_both_income.png", ///	
	as(png) replace width(2560) height(1440) //quality(100)	
	
graph drop _all 	
	
restore


* Marital status 
preserve

collapse (mean) der pred_der* [aw = dwt], by(dcpst stm)

order pred_der*

egen pred_der = rowmean(pred_der0-pred_der19)

replace stm = 2000 + stm 

twoway ///
(line pred_der stm if dcpst == 1, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line der stm if dcpst == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph1) title("Partnered") xtitle("Year") ytitle("")  ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_der stm if dcpst == 2, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line der stm if dcpst == 2, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph2) title("Single") xtitle("Year") ytitle("")  ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_der stm if dcpst == 3, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line der stm if dcpst == 3, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph3) title("Previously partnered") xtitle("Year") ytitle("")  ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

grc1leg graph1 graph2 graph3, ///
	title("Returns to Education") ///
	subtitle("By partnership status") ///
	legendfrom(graph1) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who are not observed in their initial education spell in their previous observation," "aged 16-35.Initial education spell defined generously.", size(vsmall))
	
graph export "$dir_validation_graphs/education/int_validation_E1b_returns_edu_ts_16_35_both_partnership.png", ///	
	as(png) replace width(2560) height(1440) //quality(100)
	
graph drop _all 	
	
restore


*************************************************
* E2a Educational Level After Leaving Education *
*************************************************

* Overall 
use "$dir_validation_data/E2a_sample", clear

sum p1-p3 // inspect negative values  

gen p1p2 = p1 + p2 // create cdf

gen rnd = runiform()
gen edu_pred = cond((rnd < p1), 1, cond(rnd < p1p2, 2, 3)) 

keep if in_sample == 1 


twoway (histogram edu_pred if in_sample == 1, color(green)) ///
	(histogram deh_c3_recoded if in_sample == 1, color(none) lcolor(black)), ///
	xtitle (Education level) /// 
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(lab(1 "Predicted") lab( 2 "Observed")) name(levels, replace) ///
	title("Educational Attainment when Leave Initial Education Spell") ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who are observed leaving their initial education spell in the" "current observation, aged 16-29. Initial education spell defined generously. 1 = Low education, 2 = Medium education," "3 = High education.", size(vsmall))

graph export "$dir_validation_graphs/education/int_validation_E2a_edu_attainment_hist_16_29_both.png", ///
	as(png) replace width(2560) height(1440) //quality(100)
		

* Year 
use "$dir_validation_data/E2a_sample", clear

sum p1-p3 

gen p1p2 = p1 + p2 

forvalues i = 0/19 {
	local my_seed = 12345 + `i'  
    set seed `my_seed' 	
	gen rnd = runiform() 	
	gen edu_pred`i' = cond((rnd < p1), 1, cond(rnd < p1p2, 2, 3)) 
	gen pred_edu_low`i' = (edu_pred`i' == 1)
	gen pred_edu_med`i' = (edu_pred`i' == 2)
	gen pred_edu_high`i' = (edu_pred`i' == 3)
	drop rnd
}

keep if in_sample == 1 

gen edu_low = (deh_c3_recoded == 1)
gen edu_med = (deh_c3_recoded == 2)
gen edu_high = (deh_c3_recoded == 3)

preserve 

collapse (mean) edu_low edu_med edu_high pred_edu_low* pred_edu_med* ///
	pred_edu_high* [aw = dwt], by(stm)

order pred_edu_low* pred_edu_med* pred_edu_high*	
	
egen pred_edu_low = rowmean(pred_edu_low0-pred_edu_low19)
egen pred_edu_med = rowmean(pred_edu_med0-pred_edu_med19)
egen pred_edu_high = rowmean(pred_edu_high0-pred_edu_high19)

replace stm = 2000 + stm 

twoway ///
(line pred_edu_low stm, sort color(red) legend(label(1 "Low education, predicted"))) ///
(line edu_low stm, sort color(red) color(red%20) ///
	lpattern(dash) legend(label(2 "Low education, observed"))) ///
(line pred_edu_med stm, sort color(blue) legend(label(3 "Medium education, predicted"))) ///
(line edu_med stm, sort color(blue) color(blue%20) ///
	lpattern(dash) legend(label(4 "Medium education, observed"))) ///
(line pred_edu_high stm, sort color(green) legend(label(5 "High education, predicted"))) ///
(line edu_high stm, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(6 "High education, observed"))) , ///
 title("Educational Attainment when Leave Initial Education Spell") ///
	subtitle("Ages 16-29" ) ///
	xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who are observed leaving their initial education spell in the current" "observation, aged 16-29. Initial education spell defined generously.", size(vsmall))
	
graph export "$dir_validation_graphs/education/int_validation_E2a_edu_attainment_ts_16_29_both.png", ///
	as(png) replace width(2560) height(1440) //quality(100)	

graph drop _all 	
	
restore


* Gender 
preserve 

collapse (mean) edu_low edu_med edu_high pred_edu_low* pred_edu_med* ///
	pred_edu_high* [aw = dwt], by(stm dgn)

order pred_edu_low* pred_edu_med* pred_edu_high*		
	
egen pred_edu_low = rowmean(pred_edu_low0-pred_edu_low19)
egen pred_edu_med = rowmean(pred_edu_med0-pred_edu_med19)
egen pred_edu_high = rowmean(pred_edu_high0-pred_edu_high19)

replace stm = 2000 + stm 

twoway ///
(line pred_edu_low stm if dgn == 0, sort color(red) legend(label(1 "Low education, predicted"))) ///
(line edu_low stm if dgn == 0, sort color(red) color(red%20) ///
	lpattern(dash) legend(label(2 "Low education, observed"))) ///
(line pred_edu_med stm if dgn == 0, sort color(blue) legend(label(3 "Medium education, predicted"))) ///
(line edu_med stm if dgn == 0, sort color(blue) color(blue%20) ///
	lpattern(dash) legend(label(4 "Medium education, observed"))) ///
(line pred_edu_high stm if dgn == 0, sort color(green) legend(label(5 "High education, predicted"))) ///
(line edu_high stm if dgn == 0, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(6 "High education, observed"))) , ///
	name(edu_attainment_female, replace) ///
 title("Females") ///
	xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) 
	
twoway ///
(line pred_edu_low stm if dgn == 1, sort color(red) legend(label(1 "Low education, predicted"))) ///
(line edu_low stm if dgn == 1, sort color(red) color(red%20) ///
	lpattern(dash) legend(label(2 "Low education, observed"))) ///
(line pred_edu_med stm if dgn == 1, sort color(blue) legend(label(3 "Medium education, predicted"))) ///
(line edu_med stm if dgn == 1, sort color(blue) color(blue%20) ///
	lpattern(dash) legend(label(4 "Medium education, observed"))) ///
(line pred_edu_high stm if dgn == 1, sort color(green) legend(label(5 "High education, predicted"))) ///
(line edu_high stm if dgn == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(6 "High education, observed"))) , ///
	name(edu_attainment_male, replace) ///
 title("Males") ///
	xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) 	
	
grc1leg edu_attainment_female edu_attainment_male, ///
	title("Educational Attainment when Leave Initial Education Spell") ///
	legendfrom(edu_attainment_male) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who are observed leaving their initial education spell in the current" "observation, aged 16-29. Initial education spell defined generously.", size(vsmall))

graph export "$dir_validation_graphs/education/int_validation_E2a_edu_attainment_ts_16_29_gender.png", ///
	as(png) replace width(2560) height(1440) //quality(100)	
	
graph drop _all 	
	
restore
	
	
* Age
preserve 

collapse (mean) edu_low edu_med edu_high pred_edu_low* pred_edu_med* ///
	pred_edu_high* [aw = dwt], by(dag)

order pred_edu_low* pred_edu_med* pred_edu_high*		
	
egen pred_edu_low = rowmean(pred_edu_low0-pred_edu_low19)
egen pred_edu_med = rowmean(pred_edu_med0-pred_edu_med19)
egen pred_edu_high = rowmean(pred_edu_high0-pred_edu_high19)


twoway ///
(line pred_edu_low dag, sort color(red) ///
	legend(label(1 "Low education, predicted"))) ///
(line edu_low dag, sort color(red) color(red%20) ///
	lpattern(dash) legend(label(2 "Low education, observed"))) ///
(line pred_edu_med dag, sort color(blue) ///
	legend(label(3 "Medium education, predicted"))) ///
(line edu_med dag, sort color(blue) color(blue%20) ///
	lpattern(dash) legend(label(4 "Medium education, observed"))) ///
(line pred_edu_high dag, sort color(green) ///
	legend(label(5 "High education, predicted"))) ///
(line edu_high dag, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(6 "High education, observed"))), ///
	title("Educational Attainment when Leave Initial Education Spell") ///
	subtitle("By age") ///
	xtitle("Age") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who are observed leaving their initial education spell in the current" "observation, aged 16-29. Initial education spell defined generously.", size(vsmall))

graph export "$dir_validation_graphs/education/int_validation_E2a_edu_attainment_share_age.png", ///
	as(png) replace width(2560) height(1440) //quality(100)
	
graph drop _all 	
	
restore


* Income  
preserve 

collapse (mean) edu_low edu_med edu_high pred_edu_low* pred_edu_med* ///
	pred_edu_high* [aw = dwt], by(stm ydses_c5)

order pred_edu_low* pred_edu_med* pred_edu_high*	
	
egen pred_edu_low = rowmean(pred_edu_low0-pred_edu_low19)
egen pred_edu_med = rowmean(pred_edu_med0-pred_edu_med19)
egen pred_edu_high = rowmean(pred_edu_high0-pred_edu_high19)

replace stm = 2000 + stm 

twoway ///
(line pred_edu_low stm if ydses_c5 == 1, sort color(red) ///
	legend(label(1 "Low education, predicted"))) ///
(line edu_low stm if ydses_c5 == 1, sort color(red) color(red%20) ///
	lpattern(dash) legend(label(2 "Low education, observed"))) ///
(line pred_edu_med stm if ydses_c5 == 1, sort color(blue) ///
	legend(label(3 "Medium education, predictedd"))) ///
(line edu_med stm if ydses_c5 == 1, sort color(blue) color(blue%20) ///
	lpattern(dash) legend(label(4 "Medium education, observed")))	///
(line pred_edu_high stm if ydses_c5 == 1, sort color(green) ///
	legend(label(5 "High education, predicted"))) ///
(line edu_high stm if ydses_c5 == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(6 "HIgh education, observed"))), ///
name(graph1) title("First quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) 	

twoway ///
(line pred_edu_low stm if ydses_c5 == 2, sort color(red) ///
	legend(label(1 "L Pred"))) ///
(line edu_low stm if ydses_c5 == 2, sort color(red) color(red%20) ///
	lpattern(dash) legend(label(2 "L Obs"))) ///
(line pred_edu_med stm if ydses_c5 == 2, sort color(blue) ///
	legend(label(3 "M Pred"))) ///
(line edu_med stm if ydses_c5 == 2, sort color(blue) color(blue%20) ///
	lpattern(dash) legend(label(4 "M Obs")))	///
(line pred_edu_high stm if ydses_c5 == 2, sort color(green) ///
	legend(label(5 "H Pred"))) ///
(line edu_high stm if ydses_c5 == 2, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(6 "H Obs"))), ///
name(graph2) title("Second quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) 

twoway ///
(line pred_edu_low stm if ydses_c5 == 3, sort color(red) ///
	legend(label(1 "L Pred"))) ///
(line edu_low stm if ydses_c5 == 3, sort color(red) color(red%20) ///
	lpattern(dash) legend(label(2 "L Obs"))) ///
(line pred_edu_med stm if ydses_c5 == 3, sort color(blue) ///
	legend(label(3 "M Pred"))) ///
(line edu_med stm if ydses_c5 == 3, sort color(blue) color(blue%20) ///
	lpattern(dash) legend(label(4 "M Obs")))	///
(line pred_edu_high stm if ydses_c5 == 3, sort color(green) ///
	legend(label(5 "H Pred"))) ///
(line edu_high stm if ydses_c5 == 3, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(6 "H Obs"))), ///
name(graph3) title("Third quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) 

twoway ///
(line pred_edu_low stm if ydses_c5 == 4, sort color(red) ///
	legend(label(1 "L Pred"))) ///
(line edu_low stm if ydses_c5 == 4, sort color(red) color(red%20) ///
	lpattern(dash) legend(label(2 "L Obs"))) ///
(line pred_edu_med stm if ydses_c5 == 4, sort color(blue) ///
	legend(label(3 "M Pred"))) ///
(line edu_med stm if ydses_c5 == 4, sort color(blue) color(blue%20) ///
	lpattern(dash) legend(label(4 "M Obs")))	///
(line pred_edu_high stm if ydses_c5 == 4, sort color(green) ///
	legend(label(5 "H Pred"))) ///
(line edu_high stm if ydses_c5 == 4, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(6 "H Obs"))), ///
name(graph4) title("Fourth quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) 
	
twoway ///
(line pred_edu_low stm if ydses_c5 == 5, sort color(red) ///
	legend(label(1 "L Pred"))) ///
(line edu_low stm if ydses_c5 == 5, sort color(red) color(red%20) ///
	lpattern(dash) legend(label(2 "L Obs"))) ///
(line pred_edu_med stm if ydses_c5 == 5, sort color(blue) ///
	legend(label(3 "M Pred"))) ///
(line edu_med stm if ydses_c5 == 5, sort color(blue) color(blue%20) ///
	lpattern(dash) legend(label(4 "M Obs")))	///
(line pred_edu_high stm if ydses_c5 == 5, sort color(green) ///
	legend(label(5 "H Pred"))) ///
(line edu_high stm if ydses_c5 == 5, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(6 "H Obs"))), ///
name(graph5) title("Fifth quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) 

grc1leg graph1 graph2 graph3 graph4 graph5 , ///
	title("Educational Attainment when Leave Initial Education Spell") ///
	subtitle("By hh disposable income") ///
	legendfrom(graph1) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who are observed leaving their initial education spell in the current" "observation, aged 16-29. Initial education spell defined generously.", size(vsmall))	
	
graph export "$dir_validation_graphs/education/int_validation_E2a_edu_attainment_ts_16_29_both_income.png", ///
	as(png) replace width(2560) height(1440) //quality(100)	
	
graph drop _all 	
	
restore 
