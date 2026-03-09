********************************************************************************
* PROJECT:  		ESPON 
* SECTION:			Partnership
* OBJECT: 			Internal validation
* AUTHORS:			Ashley Burdett, Daria Popova  
* LAST UPDATE:		July 2025 
* COUNTRY: 			UK  

* NOTES: 			Compares predicted values to the observed values of the 
* 					partnership processes. 
* 					Individual heterogeneity added to the standard predicted 
* 					values using a random draw like in stochasitic 
* 					imputation. The pooled mean is obtained as in multiple 
* 					imputation by repeating the random draw 20 times for each 
* 					process. 
* 
* 					Run after "reg_partnership.do"
********************************************************************************

****************************************************
* U1a: Partnership formation, in initial edu spell *
****************************************************

* Overall 
use "$dir_validation_data/U1a_sample", clear 

set seed 12345
gen rnd = runiform() 	
gen pred_dcpen = 0 
replace pred_dcpen = 1 if inrange(p,rnd,1)

keep if in_sample == 1 

twoway ///
(histogram pred_dcpen, color(red)) ///
(histogram dcpen, color(none) lcolor(black)), ///
	xtitle (Formation) ///
	legend(lab(1 "Predicted") lab( 2 "Observed")) name(levels, replace) ///
	title("Partnership Formation") ///
	subtitle("In initial education spell") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	graphregion(color(white)) ///
	legend(size(small)) ///
	note("Notes: Predicted vs observed of dummy indiciating forming a partnership. Estimation sample plotted. Sample contains individuals" "who are in their initial education spell and 18-29 years old. Initial education spell defined generously.", size(vsmall))

graph export "$dir_validation_graphs/partnership/int_validation_U1a_partnership_init_edu_hist_all.png", ///
	as(png) replace width(2560) height(1440) //quality(100)
	
graph drop _all 	


* Year 
use "$dir_validation_data/U1a_sample", clear 

forvalues i = 0/19 {
	local my_seed = 12345 + `i'  
    set seed `my_seed' 	
	gen rnd = runiform() 	
	gen pred_dcpen`i' = 0 
	replace pred_dcpen`i' = 1 if inrange(p,rnd,1)
	drop rnd
}

keep if in_sample == 1 

preserve

collapse (mean) dcpen pred_dcpen* [aw = dwt], by(stm)

order pred_dcpen*

egen pred_dcpen = rowmean(pred_dcpen0-pred_dcpen19)

replace stm = 2000 + stm 

twoway ///
(line pred_dcpen stm, sort color(green) legend(label(1 "Predicted"))) ///
(line dcpen stm, sort color(green) color(green%20) lpattern(dash) ///
	legend(label(2 "Observed"))), ///
	title("Partnership Formation") ///
	subtitle("In initial education spell") ///
	xtitle("Year") ytitle("Share")  ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who are 18-29 years old and in their initial education spell. Initial" "education spell defined generously.", size(vsmall))

graph export "$dir_validation_graphs/partnership/int_validation_U1a_partnership_init_edu_ts_all_both.png", ///
	as(png) replace width(2560) height(1440) //quality(100)
 
graph drop _all 
 
restore  


* Gender 
preserve

collapse (mean) dcpen pred_dcpen* [aw = dwt], by(dgn stm)

order pred_dcpen*

egen pred_dcpen = rowmean(pred_dcpen0-pred_dcpen19)

replace stm = 2000 + stm 

twoway ///
(line pred_dcpen stm if dgn == 0, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line dcpen stm if dgn == 0, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph1) title("Females") xtitle("Year") ytitle("Share")  ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_dcpen stm if dgn == 1, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line dcpen stm if dgn == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph2) title("Males") xtitle("Year") ytitle("Share")  ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

grc1leg graph1 graph2,  ///
	title("Partnership Formation") ///
	subtitle("In initial education spell") ///
	legendfrom(graph1) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who are 18-29 years old and in their initial education spell. Initial education spell" "defined generously.", size(vsmall))
	
	
graph export "$dir_validation_graphs/partnership/int_validation_U1a_partnership_init_edu_ts_all_gender.png", ///
	as(png) replace width(2560) height(1440) //quality(100)	
	
graph drop _all  

restore 
 
 
* Age
preserve

collapse (mean) dcpen pred_dcpen* [aw = dwt], by(dag)

order pred_dcpen*

egen pred_dcpen = rowmean(pred_dcpen0-pred_dcpen19)

twoway ///
(line pred_dcpen dag, sort color(green) legend(label(1 "Predicted"))) ///
(line dcpen dag, sort color(green) color(green%20) lpattern(dash) ///
	legend(label(2 "Observed"))), ///
title("Partnership Formation") ///
	subtitle("In initial education spall, share by age") ///
	xtitle("Age") ///
	ytitle("Share") xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who are 18+ years old and in their initial education spell. Initial" "education spell defined generously.", size(vsmall))

graph export "$dir_validation_graphs/partnership/int_validation_U1a_partnership_init_edu_share_age.png", ///
	as(png) replace width(2560) height(1440) //quality(100)

graph drop _all 

restore


* Income 
preserve

collapse (mean) dcpen pred_dcpen* [aw = dwt], by(ydses_c5 stm)

order pred_dcpen*

egen pred_dcpen = rowmean(pred_dcpen0-pred_dcpen19)

replace stm = 2000 + stm 

twoway ///
(line pred_dcpen stm if ydses_c5 == 1, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line dcpen stm if ydses_c5 == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph1) title("First quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_dcpen stm if ydses_c5 == 2, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dcpen stm if ydses_c5 == 2, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph2) title("Second quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_dcpen stm if ydses_c5 == 3, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dcpen stm if ydses_c5 == 3, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph3) title("Third quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_dcpen stm if ydses_c5 == 4, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dcpen stm if ydses_c5 == 4, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph4) title("Forth quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_dcpen stm if ydses_c5 == 5, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dcpen stm if ydses_c5 == 5, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph5) title("Fifth quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

grc1leg graph1 graph2 graph3 graph4 graph5,  ///
	title("Partnership Formation") ///
	subtitle("In initial education spell, by hh disposable income") /// 
	legendfrom(graph1) rows(2) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who are 18-29 years old and in their initial education spell. Initial education spell" "defined generously.", size(vsmall))
	
graph export "$dir_validation_graphs/partnership/int_validation_U1a_partnership_init_edu_ts_all_both_income.png", ///
	as(png) replace width(2560) height(1440) //quality(100)	

graph drop _all 	
	
restore


******************************************************
* U1b: Partnership formation, left initial edu spell *
******************************************************

* Overall 
use "$dir_validation_data/U1b_sample", clear 

set seed 12345
gen rnd = runiform() 	
gen pred_dcpen = 0 
replace pred_dcpen = 1 if inrange(p,rnd,1)

keep if in_sample == 1 

twoway ///
	(histogram pred_dcpen, color(red)) ///
	(histogram dcpen, color(none) lcolor(black)), ///
	xtitle (Formation) ///
	legend(lab(1 "Predicted") lab( 2 "Observed")) name(levels, replace) ///
	title("Partnership Formation") ///
	subtitle("Left initial education spell") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	graphregion(color(white)) ///
	legend(size(small)) ///
	note("Notes: Predicted vs observed of dummy indicating forming a partnership. Estimation sample plotted. Sample contains individuals" "who are 18+ years old and have left their initial education spell. Initial education spell defined generously.", size(vsmall))

graph export "$dir_validation_graphs/partnership/int_validation_U1b_partnership_left_edu_hist_all.png", ///
	as(png) replace width(2560) height(1440) //quality(100)
	
* Year 
use "$dir_validation_data/U1b_sample", clear 

forvalues i = 0/19 {
	local my_seed = 12345 + `i'  
    set seed `my_seed' 	
	gen rnd = runiform() 	
	gen pred_dcpen`i' = 0 
	replace pred_dcpen`i' = 1 if inrange(p,rnd,1)
	drop rnd
}

keep if in_sample == 1 

preserve

collapse (mean) dcpen pred_dcpen* [aw = dwt], by(stm)

order pred_dcpen*

egen pred_dcpen = rowmean(pred_dcpen0-pred_dcpen19)

replace stm = 2000 + stm 

twoway ///
(line pred_dcpen stm, sort color(green) legend(label(1 "Predicted"))) ///
(line dcpen stm, sort color(green) color(green%20) lpattern(dash) ///
	legend(label(2 "Observed"))), ///
	title("Partnership Formation") ///
	subtitle("Left initial education spell") ///
	xtitle("Year") ytitle("Share") ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who are 18+ years old and have left initial education spell. Initial" "education spell defined generously.", size(vsmall))


graph export "$dir_validation_graphs/partnership/int_validation_U1b_partnership_left_edu_ts_all_both.png", ///
	as(png) replace width(2560) height(1440) //quality(100)
 
restore  


* Gender 
preserve

collapse (mean) dcpen pred_dcpen* [aw = dwt], by(dgn stm)

order pred_dcpen*

egen pred_dcpen = rowmean(pred_dcpen0-pred_dcpen19)

replace stm = 2000 + stm 

twoway ///
(line pred_dcpen stm if dgn == 0, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line dcpen stm if dgn == 0, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph1) title("Females") xtitle("Year") ytitle("Share")  ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_dcpen stm if dgn == 1, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line dcpen stm if dgn == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph2) title("Males") xtitle("Year") ytitle("Share")  ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

grc1leg graph1 graph2,  ///
	title("Partnership Formation") ///
	subtitle("Left initial education spell") ///
	legendfrom(graph1) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who are 18+ years old and have left their initial education spall. Initial" "education spell defined generously.", size(vsmall))

graph export "$dir_validation_graphs/partnership/int_validation_U1b_partnership_left_edu_ts_all_gender.png", ///
	as(png) replace width(2560) height(1440) //quality(100)

graph drop _all  

restore 
 
 
* Age
preserve

collapse (mean) dcpen pred_dcpen* [aw = dwt], by(dag)

order pred_dcpen*

egen pred_dcpen = rowmean(pred_dcpen0-pred_dcpen19)

twoway ///
(line pred_dcpen dag, sort color(green) legend(label(1 "Predicted"))) ///
(line dcpen dag, sort color(green) color(green%20) lpattern(dash) ///
	legend(label(2 "Observed"))), ///
	title("Partnership Formation") ///
	subtitle("Left initial education spell, share by age") ///
	xtitle("Age") ///
	ytitle("Share") xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who are 18+ years old and have left their initial education spell. Initial" "education spell defined generously.", size(vsmall))

graph export "$dir_validation_graphs/partnership/int_validation_U1b_partnership_left_edu_share_age.png", ///
	as(png) replace width(2560) height(1440) //quality(100)

restore


* Income 
preserve

collapse (mean) dcpen pred_dcpen* [aw = dwt], by(ydses_c5 stm)

order pred_dcpen*

egen pred_dcpen = rowmean(pred_dcpen0-pred_dcpen19)

replace stm = 2000 + stm 

twoway ///
(line pred_dcpen stm if ydses_c5 == 1, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line dcpen stm if ydses_c5 == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph1) title("First quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_dcpen stm if ydses_c5 == 2, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dcpen stm if ydses_c5 == 2, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph2) title("Second quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_dcpen stm if ydses_c5 == 3, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dcpen stm if ydses_c5 == 3, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph3) title("Third quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_dcpen stm if ydses_c5 == 4, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dcpen stm if ydses_c5 == 4, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph4) title("Forth quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_dcpen stm if ydses_c5 == 5, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dcpen stm if ydses_c5 == 5, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph5) title("Fifth quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

grc1leg graph1 graph2 graph3 graph4 graph5,  ///
	title("Partnership Formation") ///
	subtitle("Left initial education spell, by hh disposable income") ///
	legendfrom(graph1) rows(2) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individual who are 18+ years old and have left their initial education spell. Initial" "education spell defined generously.", size(vsmall))

graph export "$dir_validation_graphs/partnership/int_validation_U1b_partnership_left_edu_ts_all_both_income.png", ///
	as(png) replace width(2560) height(1440) //quality(100)

graph drop _all 	
	
restore


**********************************************************
* U2b: Partnership termination, not in initial edu spell *
**********************************************************

* Overall 
use "$dir_validation_data/U2b_sample", clear 

set seed 12345
gen rnd = runiform() 	
gen pred_dcpex = 0 
replace pred_dcpex = 1 if inrange(p,rnd,1)

keep if in_sample == 1 

twoway ///
	(histogram pred_dcpex, color(red)) ///
	(histogram dcpex, color(none) lcolor(black)), ///
	xtitle (Formation) ///
	legend(lab(1 "Predicted") lab( 2 "Observed")) name(levels, replace) ///
	title("Partnership Termination") ///
	subtitle("Left initial education spell")  ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	graphregion(color(white)) ///
	legend(size(small)) ///
	note("Notes: Predicted vs observed of dummy indiciating ending a partnership. Estimation sample plotted. Sample contains individuals" "who have left their initial education spell and are 18+ years old. Initial education spell defined generously.", size(vsmall))

graph export "$dir_validation_graphs/partnership/int_validation_U2b_separation_left_edu_hist_all.png", ///
	as(png) replace width(2560) height(1440) //quality(100)

	
* Year 
use "$dir_validation_data/U2b_sample", clear 

forvalues i = 0/19 {
	local my_seed = 12345 + `i'  
    set seed `my_seed' 	
	gen rnd = runiform() 	
	gen pred_dcpex`i' = 0 
	replace pred_dcpex`i' = 1 if inrange(p,rnd,1)
	drop rnd
}

keep if in_sample == 1 

preserve

collapse (mean) dcpex pred_dcpex* [aw = dwt], by(stm)

order pred_dcpex*

egen pred_dcpex = rowmean(pred_dcpex0-pred_dcpex19)

replace stm = 2000 + stm 

twoway ///
(line pred_dcpex stm, sort color(green) legend(label(1 "Predicted"))) ///
(line dcpex stm, sort color(green) color(green%20) lpattern(dash) ///
	legend(label(2 "Observed"))), ///
title("Partnership Termination") ///
	subtitle("Left initial education spell")  ///
	xtitle("Year") ytitle("Share")  ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who are 18+ years old and have left their initial education spell." "Initial education spell defined generously.", size(vsmall))

graph export "$dir_validation_graphs/partnership/int_validation_U2b_separation_left_edu_ts_all_both.png", ///
	as(png) replace width(2560) height(1440) //quality(100)

graph drop _all
	
restore  

 
* Age
preserve

collapse (mean) dcpex pred_dcpex* [aw = dwt], by(dag)

order pred_dcpex*

egen pred_dcpex = rowmean(pred_dcpex0-pred_dcpex19)

twoway ///
(line pred_dcpex dag, sort color(green) legend(label(1 "Predicted"))) ///
(line dcpex dag, sort color(green) color(green%20) lpattern(dash) ///
	legend(label(2 "Observed"))), ///
title("Partnership Termination") ///
	subtitle("Left initial education spell")  ///
	xtitle("Age") ///
	ytitle("Share") xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who are 18+ years old and have left their initial education spell." "Initial education spell defined generously.", size(vsmall))

graph export "$dir_validation_graphs/partnership/int_validation_U2b_separation_left_edu_share_age.png", ///
	as(png) replace width(2560) height(1440) //quality(100)

restore


* Income 
preserve

collapse (mean) dcpex pred_dcpex* [aw = dwt], by(ydses_c5 stm)

order pred_dcpex*

egen pred_dcpex = rowmean(pred_dcpex0-pred_dcpex19)

replace stm = 2000 + stm 

twoway ///
(line pred_dcpex stm if ydses_c5 == 1, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line dcpex stm if ydses_c5 == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph1) title("First quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))


twoway ///
(line pred_dcpex stm if ydses_c5 == 2, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dcpex stm if ydses_c5 == 2, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph2) title("Second quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))


twoway ///
(line pred_dcpex stm if ydses_c5 == 3, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dcpex stm if ydses_c5 == 3, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph3) title("Third quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))


twoway ///
(line pred_dcpex stm if ydses_c5 == 4, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dcpex stm if ydses_c5 == 4, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph4) title("Forth quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))


twoway ///
(line pred_dcpex stm if ydses_c5 == 5, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dcpex stm if ydses_c5 == 5, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph5) title("Fifth quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

grc1leg graph1 graph2 graph3 graph4 graph5,  ///
title("Partnership Termination") ///
	subtitle("Left initial education spell by hh disposable income") ///
	legendfrom(graph1) rows(2) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who are 18+ years old and have left their initial education spell." "Initial education spell defined generously.", size(vsmall))

graph export "$dir_validation_graphs/partnership/int_validation_U2b_separation_init_edu_ts_all_both_income.png", ///
	as(png) replace width(2560) height(1440) //quality(100)

graph drop _all 	
	
restore


* Education
preserve

collapse (mean) dcpex pred_dcpex* [aw = dwt], by(deh_c3 stm)

order pred_dcpex*

egen pred_dcpex = rowmean(pred_dcpex0-pred_dcpex19)

replace stm = 2000 + stm 

twoway ///
(line pred_dcpex stm if deh_c3 == 1, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line dcpex stm if deh_c3 == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph1) title("High education") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_dcpex stm if deh_c3 == 2, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dcpex stm if deh_c3 == 2, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph2) title("Medium education") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_dcpex stm if deh_c3 == 3, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dcpex stm if deh_c3 == 3, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph3) title("Low education") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

grc1leg graph1 graph2 graph3,  ///
title("Partnership Termination") ///
	subtitle("Left initial education spell")  ///
	legendfrom(graph1) rows(2) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who are 18+ years old and have left their initial education spell." "Initial education spell defined generously.", size(vsmall))

graph export "$dir_validation_graphs/partnership/int_validation_U2b_separation_init_edu_ts_all_both_edu.png", ///
	as(png) replace width(2560) height(1440) //quality(100)

graph drop _all 	
	
restore
