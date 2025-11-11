********************************************************************************
* PROJECT:  		ESPON 
* SECTION:			Home ownership
* OBJECT: 			Internal validation
* AUTHORS:			Ashley Burdett, Daria Popova 
* LAST UPDATE:		July 2025 
* COUNTRY: 			UK 

* NOTES: 			Compares predicted values to the observed values of the 
* 					home ownership process estimated. 
* 					Individual heterogeneity added to the standard predicted 
* 					values using a random draw like in stochasitic 
* 					imputation. The pooled mean is obtained as in multiple 
* 					imputation by repeating the random draw 20 times for each 
* 					process. 
* 
* 					Run after "reg_home_ownership.do"
********************************************************************************

************************
* HO1a: Home ownership *
************************

* Overall 
use "$dir_validation_data/HO1a_sample", clear 

set seed 12345
gen rnd = runiform() 	
gen pred_dhh_owned = 0 
replace pred_dhh_owned = 1 if inrange(p,rnd,1)

keep if in_sample == 1 

twoway ///
	(histogram pred_dhh_owned, color(red)) ///
	(histogram dhh_owned, color(none) lcolor(black) ), ///
	xtitle (Home ownership) ///
	legend(lab(1 "Predicted") lab( 2 "Observed")) name(levels, replace) ///
	title("Home Ownership")  ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	graphregion(color(white)) ///
	legend(size(small)) ///
	note("Notes: Predicted vs observed of dummy indicating homeownership. Estimation sample plotted. Sample contains all individuals 18+" "years old.", size(vsmall))

graph export "$dir_validation_graphs/home_ownership/int_validation_HO1a_homeownership_hist_all.png", ///
	as(png) replace width(2560) height(1440) //quality(100)
	
graph drop _all 	
	
* Year 
use "$dir_validation_data/HO1a_sample", clear 

forvalues i = 0/19 {
	local my_seed = 12345 + `i'  
    set seed `my_seed' 	
	gen rnd = runiform() 	
	gen pred_dhh_owned`i' = 0 
	replace pred_dhh_owned`i' = 1 if inrange(p,rnd,1)
	drop rnd
}

keep if in_sample == 1 

preserve

collapse (mean) dhh_owned pred_dhh_owned* [aw = dwt], by(stm)

order pred_dhh_owned*

egen pred_dhh_owned = rowmean(pred_dhh_owned0-pred_dhh_owned19)
replace stm = 2000 + stm 

twoway ///
(line pred_dhh_owned stm, sort color(green) legend(label(1 "Predicted"))) ///
(line dhh_owned stm, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
title("Home Ownership")  ///
	xtitle("Year") ytitle("Share") ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Estimation sample plotted. Sample contains all individuals 18+ years old.", size(vsmall))

graph export "$dir_validation_graphs/home_ownership/int_validation_HO1a_homeownership_ts_all_both.png", ///
	as(png) replace width(2560) height(1440) //quality(100) 
 
graph drop _all  
 
restore  


* Gender 
preserve

collapse (mean) dhh_owned pred_dhh_owned* [aw = dwt], by(dgn stm)

order pred_dhh_owned*

egen pred_dhh_owned = rowmean(pred_dhh_owned0-pred_dhh_owned19)

replace stm = 2000 + stm 

twoway ///
(line pred_dhh_owned stm if dgn == 0, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line dhh_owned stm if dgn == 0, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph1) title("Females") xtitle("Year") ytitle("Share") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_dhh_owned stm if dgn == 1, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line dhh_owned stm if dgn == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph2) title("Males") xtitle("Year") ytitle("Share") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

grc1leg graph1 graph2,  ///
	title("Home Ownership")  ///
	legendfrom(graph1) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains all individuals 18+ years old.", size(vsmall))

graph export "$dir_validation_graphs/home_ownership/int_validation_HO1a_homeownership_ts_all_gender.png", ///
	as(png) replace width(2560) height(1440) //quality(100)

graph drop _all  

restore 
 
 
* Age
preserve

collapse (mean) dhh_owned pred_dhh_owned* [aw = dwt], by(dag)

order pred_dhh_owned*

egen pred_dhh_owned = rowmean(pred_dhh_owned0-pred_dhh_owned19)

twoway ///
(line pred_dhh_owned dag, sort color(green) legend(label(1 "Predicted"))) ///
(line dhh_owned dag, sort color(green) color(green%20) lpattern(dash) ///
	legend(label(2 "Observed"))), ///
	title("Home Ownership")  ///
	subtitle("Share by age") ///
	xtitle("Age") ytitle("Share") xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains all individuals 18+ years old.", size(vsmall))

graph export "$dir_validation_graphs/home_ownership/int_validation_HO1a_homeownership_share_age.png", ///
	as(png) replace width(2560) height(1440) //quality(100)	
	
restore

* Income 
preserve

collapse (mean) dhh_owned pred_dhh_owned* [aw = dwt], by(ydses_c5 stm)

order pred_dhh_owned*

egen pred_dhh_owned = rowmean(pred_dhh_owned0-pred_dhh_owned19)

replace stm = 2000 + stm 

twoway ///
(line pred_dhh_owned stm if ydses_c5 == 1, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line dhh_owned stm if ydses_c5 == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph1) title("First quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_dhh_owned stm if ydses_c5 == 2, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dhh_owned stm if ydses_c5 == 2, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph2) title("Second quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_dhh_owned stm if ydses_c5 == 3, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dhh_owned stm if ydses_c5 == 3, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph3) title("Third quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_dhh_owned stm if ydses_c5 == 4, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dhh_owned stm if ydses_c5 == 4, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
	name(graph4) title("Forth quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_dhh_owned stm if ydses_c5 == 5, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dhh_owned stm if ydses_c5 == 5, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph5) title("Fifth quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

grc1leg graph1 graph2 graph3 graph4 graph5,  ///
	title("Home Ownership")  ///
	subtitle("By hh disposable income") ///
	legendfrom(graph1) rows(2) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains all individuals 18+ years old.", size(vsmall))

graph export "$dir_validation_graphs/home_ownership/int_validation_HO1a_homeownership_ts_all_both_income.png", ///
	as(png) replace width(2560) height(1440) //quality(100)
	
graph drop _all 	
	
restore


* Education 
preserve

collapse (mean) dhh_owned pred_dhh_owned* [aw = dwt], by(deh_c3 stm)

order pred_dhh_owned*

egen pred_dhh_owned = rowmean(pred_dhh_owned0-pred_dhh_owned19)

replace stm = 2000 + stm 

twoway ///
(line pred_dhh_owned stm if deh_c3 == 1, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line dhh_owned stm if deh_c3 == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph1) title("High education") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_dhh_owned stm if deh_c3 == 2, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dhh_owned stm if deh_c3 == 2, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph2) title("Medium education") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_dhh_owned stm if deh_c3 == 3, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dhh_owned stm if deh_c3 == 3, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph3) title("Low education") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

grc1leg graph1 graph2 graph3,  ///
	title("Home Ownership")  ///
	legendfrom(graph1) rows(2) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains all individuals 18+ years old.", size(vsmall))

graph export "$dir_validation_graphs/home_ownership/int_validation_HO1a_homeownership_ts_all_both_edu.png", ///
	as(png) replace width(2560) height(1440) //quality(100)

graph drop _all 	
	
restore


* Partnership status 
preserve

collapse (mean) dhh_owned pred_dhh_owned* [aw = dwt], by(dcpst stm)

order pred_dhh_owned*

egen pred_dhh_owned = rowmean(pred_dhh_owned0-pred_dhh_owned19)

replace stm = 2000 + stm 

twoway ///
(line pred_dhh_owned stm if dcpst == 1, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line dhh_owned stm if dcpst == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph1) title("Partnered") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_dhh_owned stm if dcpst == 2, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dhh_owned stm if dcpst == 2, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph2) title("Single") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_dhh_owned stm if dcpst == 3, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dhh_owned stm if dcpst == 3, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph3) title("Previously partnered") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

grc1leg graph1 graph2 graph3,  ///
	title("Home Ownership")  ///
	legendfrom(graph1) rows(2) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains all individuals 18+ years old.", size(vsmall))

graph export "$dir_validation_graphs/home_ownership/int_validation_HO1a_homeownership_ts_all_both_partnership.png", ///
	as(png) replace width(2560) height(1440) //quality(100)

graph drop _all 	
	
restore
