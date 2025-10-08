********************************************************************************
* PROJECT:  		ESPON 
* SECTION:			Health 
* OBJECT: 			Internal validation
* AUTHORS:			Ashley Burdett, Daria Popova 
* LAST UPDATE:		July 2025
* COUNTRY: 			UK   

* NOTES: 			Compares predicted values to the observed values of the 
* 					3 health processes estimated. 
* 					Individual heterogeneity added to the standard predicted 
* 					values using a random draw like in stochasitic 
* 					imputation. The pooled mean is obtained as in multiple 
* 					imputation by repeating the random draw 20 times for each 
* 					process. 
* 
* 					Run after "reg_health.do"
********************************************************************************

********************************************
* H1a: Health status, in initial edu spell *
********************************************

* Overall 
use "$dir_validation_data/H1a_sample", clear

sum p1-p5 // inspect negative values 
		
gen p1p2 = p1 + p2 
gen p1p2p3 = p1p2 + p3
gen p1p2p3p4 = p1p2p3 + p4 // generate cumulative probabilities for all options

gen rnd = runiform()
gen pred_health = cond((rnd < p1), 1, cond(rnd < p1p2, 2, ///
	cond(rnd < p1p2p3, 3, cond(rnd < p1p2p3p4, 4, 5))))

keep if in_sample == 1	
	
twoway (histogram pred_health if in_sample == 1, color(red)) ///
	(histogram dhe if in_sample == 1, color(none) lcolor(black) ), ///
	xtitle (Self-rated health) ///
	legend(lab(1 "Predicted") lab( 2 "Observed")) name(levels, replace) ///
	title("Health Status") ///
	subtitle("In initial education spell ") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	graphregion(color(white)) ///
	legend(size(small)) ///
	note("Notes: Predicted vs observed of self-reported health status. Estimation sample plotted. Sample contains individuals" "who are in their initial education spell and aged 18-29. Initial education spell defined generously. 1 = Poor, 5 = Excellent.  ", size(vsmall))
	
graph export "$dir_validation_graphs/health/int_validation_H1a_health_init_edu_hist_18_29.png", ///
	as(png) replace	width(2560) height(1440) //quality(100)
	
	
* Year 
use "$dir_validation_data/H1a_sample", clear

sum p1-p5 // inspect negative values 
		
gen p1p2 = p1 + p2 
gen p1p2p3 = p1p2 + p3
gen p1p2p3p4 = p1p2p3 + p4 // generate cumulative probabilities for all options

forvalues i = 0/19 {
	local my_seed = 12345 + `i'  
    set seed `my_seed' 	
	gen rnd = runiform() 	
	gen pred_health`i' = cond((rnd < p1), 1, cond(rnd < p1p2, 2, ///
		cond(rnd < p1p2p3, 3, cond(rnd < p1p2p3p4, 4, 5))))
	gen pred_health_poor`i' = (pred_health`i' == 1)
	gen pred_health_fair`i' = (pred_health`i' == 2)
	gen pred_health_good`i' = (pred_health`i' == 3)
	gen pred_health_vgood`i' = (pred_health`i' == 4)
	gen pred_health_excel`i' = (pred_health`i' == 5)
	drop rnd
}

keep if in_sample == 1 

gen health_poor = (dhe == 1)
gen health_fair = (dhe == 2)
gen health_good = (dhe == 3)
gen health_vgood = (dhe == 4)
gen health_excel = (dhe == 5)

preserve 

collapse (mean) health_* pred_health_*  [aw = dwt], by(stm)

order pred_health_poor* pred_health_fair* pred_health_good* ///
	pred_health_vgood* pred_health_excel*

egen pred_health_poor = rowmean(pred_health_poor0-pred_health_poor19)
egen pred_health_fair = rowmean(pred_health_fair0-pred_health_fair19)
egen pred_health_good = rowmean(pred_health_good0-pred_health_good19)
egen pred_health_vgood = rowmean(pred_health_vgood0-pred_health_vgood19)
egen pred_health_excel = rowmean(pred_health_excel0-pred_health_excel19)

replace stm = 2000+stm 

twoway ///
(line pred_health_poor stm, sort color(green) legend(label(1 "Predicted"))) ///
(line health_poor stm, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph1) title("Poor ") xtitle("Year") ytitle("Share") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_health_fair stm, sort color(green) legend(label(1 "Pred"))) ///
(line health_fair stm, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph2) title("Fair ") xtitle("Year") ytitle("Share") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_health_good stm, sort color(green) legend(label(1 "Pred"))) ///
(line health_good stm, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph3) title("Good ") xtitle("Year") ytitle("Share") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_health_vgood stm, sort color(green) legend(label(1 "Pred"))) ///
(line health_vgood stm, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph4) title("Very good") xtitle("Year") ytitle("Share") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_health_excel stm, sort color(green) legend(label(1 "Pred"))) ///
(line health_excel stm, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph5) title("Excellent") xtitle("Year") ytitle("Share") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

grc1leg graph1 graph2 graph3 graph4 graph5,  ///
	title("Health Status") ///
	subtitle("In initial education spell ") ///
	legendfrom(graph1) rows(2) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who are in their initial education spell (18-29). Initial education spell" "defined generously.", size(vsmall))
		
graph export "$dir_validation_graphs/health/int_validation_H1a_health_init_edu_ts_18_29_both.png", ///
	as(png) replace width(2560) height(1440) //quality(100)
	
	
graph drop _all 	
	
restore	

* Gender
preserve 
	
collapse (mean) health_* pred_health_*  [aw = dwt], by(stm dgn)
	
order pred_health_poor* pred_health_fair* pred_health_good* ///
	pred_health_vgood* pred_health_excel*

egen pred_health_poor = rowmean(pred_health_poor0-pred_health_poor19)
egen pred_health_fair = rowmean(pred_health_fair0-pred_health_fair19)
egen pred_health_good = rowmean(pred_health_good0-pred_health_good19)
egen pred_health_vgood = rowmean(pred_health_vgood0-pred_health_vgood19)
egen pred_health_excel = rowmean(pred_health_excel0-pred_health_excel19)

replace stm = 2000 + stm 


* Females 
twoway ///
(line pred_health_poor stm if dgn == 0, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line health_poor stm if dgn == 0, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph1) title("Poor") xtitle("Year") ytitle("Share")  ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_health_fair stm if dgn == 0, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line health_fair stm if dgn == 0, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph2) title("Fair") xtitle("Year") ytitle("Share")  ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_health_good stm if dgn == 0, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line health_good stm if dgn == 0, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph3) title("Good") xtitle("Year") ytitle("Share")  ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_health_vgood stm if dgn == 0, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line health_vgood stm if dgn == 0, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph4) title("Very good") xtitle("Year") ytitle("Share")  ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_health_excel stm if dgn == 0, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line health_excel stm if dgn == 0, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph5) title("Excellent ") xtitle("Year") ytitle("Share")  ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

grc1leg graph1 graph2 graph3 graph4 graph5,  ///
	title("Health Status") ///
	subtitle("In initial education spell, females") ///
	legendfrom(graph1) rows(2) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who are in their initial education spell (18-29). Initial education spell" "defined generously.", size(vsmall))
	
graph export "$dir_validation_graphs/health/int_validation_H1a_health_init_edu_ts_18_29_female.png", ///
	as(png) replace width(2560) height(1440) //quality(100)
	
graph drop _all 		

twoway ///
(line pred_health_poor stm if dgn == 1, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line health_poor stm if dgn == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph1) title("Poor") xtitle("Year") ytitle("")  ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_health_fair stm if dgn == 1, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line health_fair stm if dgn == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph2) title("Fair") xtitle("Year") ytitle("")  ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_health_good stm if dgn == 1, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line health_good stm if dgn == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph3) title("Good ") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_health_vgood stm if dgn == 1, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line health_vgood stm if dgn == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph4) title("Very good ") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_health_excel stm if dgn == 1, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line health_excel stm if dgn == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph5) title("Excellent ") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

grc1leg graph1 graph2 graph3 graph4 graph5,  ///
	title("Health Status") ///
	subtitle("In initial education spell, males ") ///
	legendfrom(graph1) rows(2) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who are in their initial education spell (18-29). Initial education spell" "defined generously.", size(vsmall))
	
graph export "$dir_validation_graphs/health/int_validation_H1a_health_init_edu_ts_18_29_male.png", ///
	as(png) replace width(2560) height(1440) //quality(100)
	
graph drop _all 		
	
restore


**********************************************
* H1b: Health status, left initial edu spell *
**********************************************

* Overall 
use "$dir_validation_data/H1b_sample", clear

sum p1-p5 // inspect negative values 
		
gen p1p2 = p1 + p2 
gen p1p2p3 = p1p2 + p3
gen p1p2p3p4 = p1p2p3 + p4 // generate cumulative probabilities for all options

gen rnd = runiform()
gen pred_health = cond((rnd < p1), 1, cond(rnd < p1p2, 2, ///
	cond(rnd < p1p2p3, 3, cond(rnd < p1p2p3p4, 4, 5))))

keep if in_sample == 1 	
	
twoway (histogram pred_health if in_sample == 1, color(red)) ///
	(histogram dhe if in_sample == 1, color(none) lcolor(black)), ///
	xtitle (Self-rated health) ///
	legend(lab(1 "Predicted") lab( 2 "Observed")) name(levels, replace) ///
	title("Health Status") ///
	subtitle("Left initial education spell ") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	graphregion(color(white)) ///
	legend(size(small)) ///
	note("Notes: Predicted vs observed of self-reported health status. Estimation sample plotted. Sample contains individual" "who have left their initial education spell and aged 18+. Initial education spell defined generously. 1 = Poor, 5 = Excellent.  ", size(vsmall))
	
graph export "$dir_validation_graphs/health/int_validation_H1a_health_left_edu_hist_all.png", ///
	as(png) replace	width(2560) height(1440) //quality(100)	
	
	
* Year 
use "$dir_validation_data/H1b_sample", clear

sum p1-p5 // inspect negative values 
		
gen p1p2 = p1 + p2 
gen p1p2p3 = p1p2 + p3
gen p1p2p3p4 = p1p2p3 + p4 // generate cumulative probabilities for all options

forvalues i = 0/19 {
	local my_seed = 12345 + `i'  
    set seed `my_seed' 	
	gen rnd = runiform() 	
	gen pred_health`i' = cond((rnd < p1), 1, cond(rnd < p1p2, 2, ///
		cond(rnd < p1p2p3, 3, cond(rnd < p1p2p3p4, 4, 5))))
	gen pred_health_poor`i' = (pred_health`i' == 1)
	gen pred_health_fair`i' = (pred_health`i' == 2)
	gen pred_health_good`i' = (pred_health`i' == 3)
	gen pred_health_vgood`i' = (pred_health`i' == 4)
	gen pred_health_excel`i' = (pred_health`i' == 5)
	drop rnd
}

keep if in_sample == 1 

gen health_poor = (dhe == 1)
gen health_fair = (dhe == 2)
gen health_good = (dhe == 3)
gen health_vgood = (dhe == 4)
gen health_excel = (dhe == 5)

preserve 

collapse (mean) health_* pred_health_*  [aw = dwt], by(stm)

order pred_health_poor* pred_health_fair* pred_health_good* ///
	pred_health_vgood* pred_health_excel*

egen pred_health_poor = rowmean(pred_health_poor0-pred_health_poor19)
egen pred_health_fair = rowmean(pred_health_fair0-pred_health_fair19)
egen pred_health_good = rowmean(pred_health_good0-pred_health_good19)
egen pred_health_vgood = rowmean(pred_health_vgood0-pred_health_vgood19)
egen pred_health_excel = rowmean(pred_health_excel0-pred_health_excel19)

replace stm = 2000 + stm 

twoway ///
(line pred_health_poor stm, sort color(green) legend(label(1 "Predicted"))) ///
(line health_poor stm, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph1) title("Poor ") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_health_fair stm, sort color(green) legend(label(1 "Pred"))) ///
(line health_fair stm, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph2) title("Fair ") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_health_good stm, sort color(green) legend(label(1 "Pred"))) ///
(line health_good stm, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph3) title("Good ") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_health_vgood stm, sort color(green) legend(label(1 "Pred"))) ///
(line health_vgood stm, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph4) title("Very good ") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_health_excel stm, sort color(green) legend(label(1 "Pred"))) ///
(line health_excel stm, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph5) title("Excellent ") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))


grc1leg graph1 graph2 graph3 graph4 graph5,  ///
	title("Health Status") ///
	subtitle("Left initial education spell ") ///
	legendfrom(graph1) rows(2) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who have left their initial education spell and aged 18+. Initial education spell" "defined generously.", size(vsmall))
		
graph export "$dir_validation_graphs/health/int_validation_H1b_health_left_edu_ts_all_both.png", ///
	as(png) replace width(2560) height(1440) //quality(100)

graph drop _all 	
	
restore	


* Gender
preserve 
	
collapse (mean) health_* pred_health_*  [aw = dwt], by(stm dgn)
	
order pred_health_poor* pred_health_fair* pred_health_good* ///
	pred_health_vgood* pred_health_excel*

egen pred_health_poor = rowmean(pred_health_poor0-pred_health_poor19)
egen pred_health_fair = rowmean(pred_health_fair0-pred_health_fair19)
egen pred_health_good = rowmean(pred_health_good0-pred_health_good19)
egen pred_health_vgood = rowmean(pred_health_vgood0-pred_health_vgood19)
egen pred_health_excel = rowmean(pred_health_excel0-pred_health_excel19)

replace stm = 2000 + stm 

* Female
twoway ///
(line pred_health_poor stm if dgn == 0, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line health_poor stm if dgn == 0, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph1) title("Poor ") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_health_fair stm if dgn == 0, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line health_fair stm if dgn == 0, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph2) title("Fair ") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_health_good stm if dgn == 0, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line health_good stm if dgn == 0, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph3) title("Good ") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_health_vgood stm if dgn == 0, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line health_vgood stm if dgn == 0, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph4) title("Very good ") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_health_excel stm if dgn == 0, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line health_excel stm if dgn == 0, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph5) title("Excellent ") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

grc1leg graph1 graph2 graph3 graph4 graph5,  ///
	title("Health Status") ///
	subtitle("Left initial education spell, females ") ///
	legendfrom(graph1) rows(2) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who have left their initial education spell and aged 18+. Initial education spell" "defined generously.", size(vsmall))
	
graph export "$dir_validation_graphs/health/int_validation_H1b_health_left_edu_ts_all_female.png", ///
	as(png) replace width(2560) height(1440) //quality(100)

graph drop _all 	

twoway ///
(line pred_health_poor stm if dgn == 1, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line health_poor stm if dgn == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph1) title("Poor ") xtitle("Year") ytitle("")  ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_health_fair stm if dgn == 1, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line health_fair stm if dgn == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph2) title("Fair ") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_health_good stm if dgn == 1, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line health_good stm if dgn == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph3) title("Good ") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_health_vgood stm if dgn == 1, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line health_vgood stm if dgn == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph4) title("Very good ") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_health_excel stm if dgn == 1, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line health_excel stm if dgn == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph5) title("Excellent ") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

grc1leg graph1 graph2 graph3 graph4 graph5,  ///
	title("Health Status") ///
	subtitle("Left initial education spell, males ") ///
	legendfrom(graph1) rows(2) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who have left their initial education spell and aged 18+. Initial education spell" "defined generously.", size(vsmall))
	
graph export "$dir_validation_graphs/health/int_validation_H1b_health_left_edu_ts_all_male.png", ///
	as(png) replace width(2560) height(1440) //quality(100)

graph drop _all 	
	
restore


***********************************************************
* H2b: Long-term sick or disabled, left initial edu spell *
***********************************************************

* Overall 
use "$dir_validation_data/H2b_sample", clear 

set seed 12345
gen rnd = runiform() 	
gen pred_dlltsd01 = 0 
replace pred_dlltsd01 = 1 if inrange(p,rnd,1)

keep if in_sample == 1 

twoway ///
	(histogram pred_dlltsd01, color(red)) ///
	(histogram dlltsd01, color(none) lcolor(black)), ///
	xtitle (Disabled/long-term sick ) ///
	legend(lab(1 "Predicted") lab( 2 "Observed")) name(levels, replace) ///
	title("Disability/long-term sick") ///
	subtitle("Left initial education spell") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	graphregion(color(white)) ///
	legend(size(small)) ///
	note("Notes: Predicted vs observed of disability/long-term sick dummy. Estimation sample plotted. Sample contains individuals" "who have left their initial education spell and aged 18+. Initial education spell defined generously.", size(vsmall))

graph export "$dir_validation_graphs/health/int_validation_H2b_disablilty_left_edu_hist_all.png", ///
	as(png) replace width(2560) height(1440) //quality(100)


* Year 
use "$dir_validation_data/H2b_sample", clear

// construct multiple versions of the predicted outcome allowing for different 
// random draws 
forvalues i = 0/19 {
	local my_seed = 12345 + `i'  
    set seed `my_seed' 	
	gen rnd = runiform() 	
	gen pred_dlltsd01`i' = 0 
	replace pred_dlltsd01`i' = 1 if inrange(p,rnd,1)
	drop rnd
}

keep if in_sample == 1 

preserve

// for each iteration calculate the share that leave edu 
collapse (mean) dlltsd01 pred_dlltsd01* [aw = dwt], by(stm)

order pred_dlltsd01*

// take the average across datasets 
egen pred_dlltsd01 = rowmean(pred_dlltsd010-pred_dlltsd0119)
replace stm = 2000 + stm 

twoway ///
(line pred_dlltsd01 stm, sort color(green) legend(label(1 "Predicted"))) ///
(line dlltsd01 stm, sort color(green) color(green%20) lpattern(dash) ///
	legend(label(2 "Observed"))), ///
	title("Disability/long-term sick") ///
	subtitle("Left initial education spell") ///
	xtitle("Year") ytitle("Share")  ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who have left their initial education spell and aged 18+. Initial" "education spell defined generously.", size(vsmall))

graph export "$dir_validation_graphs/health/int_validation_H2b_disablilty_left_edu_ts_all_both.png", ///
	as(png) replace width(2560) height(1440) //quality(100)
 
graph drop _all 
 
restore  
 
 
* Age
preserve

collapse (mean) dlltsd01 pred_dlltsd01* [aw = dwt], by(dag)

order pred_dlltsd01*

egen pred_dlltsd01 = rowmean(pred_dlltsd010-pred_dlltsd0119)

twoway ///
(line pred_dlltsd01 dag, sort color(green) legend(label(1 "Predicted"))) ///
(line dlltsd01 dag, sort color(green) color(green%20) lpattern(dash) ///
	legend(label(2 "Observed"))), ///
	title("Disability/long-term sick") ///
	subtitle("Left initial education spell, share by age") ///
	xtitle("Age") ///
	ytitle("Share")  ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who have left their initial education spell and aged 18+. Initial education spell " "defined generously.", size(vsmall))

graph export "$dir_validation_graphs/health/int_validation_H2b_disablilty_left_edu_share_age.png", ///
	as(png) replace width(2560) height(1440) //quality(100)

graph drop _all 	
	
restore


* Income 
preserve

collapse (mean) dlltsd01 pred_dlltsd01* [aw = dwt], by(ydses_c5 stm)

order pred_dlltsd01*

egen pred_dlltsd01 = rowmean(pred_dlltsd010-pred_dlltsd0119)

replace stm = 2000 + stm 

twoway ///
(line pred_dlltsd01 stm if ydses_c5 == 1, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line dlltsd01 stm if ydses_c5 == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph1) title("First quintile") xtitle("Year") ytitle("")  ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_dlltsd01 stm if ydses_c5 == 2, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dlltsd01 stm if ydses_c5 == 2, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph2) title("Second quintile") xtitle("Year") ytitle("")  ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_dlltsd01 stm if ydses_c5 == 3, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dlltsd01 stm if ydses_c5 == 3, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph3) title("Third quintile") xtitle("Year") ytitle("")  ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_dlltsd01 stm if ydses_c5 == 4, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dlltsd01 stm if ydses_c5 == 4, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph4) title("Forth quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_dlltsd01 stm if ydses_c5 == 5, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dlltsd01 stm if ydses_c5 == 5, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph5) title("Fifth quintile") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

grc1leg graph1 graph2 graph3 graph4 graph5,  ///
	title("Disability/long-term sick") ///
	subtitle("Left initial education spell, by hh disposable income") ///
	legendfrom(graph1) rows(2) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who have left their initial education spell and aged 18+. Initial education spell defined" "generously.", size(vsmall))
	
graph export "$dir_validation_graphs/health/int_validation_H2b_disablilty_left_edu_ts_all_both_income.png", ///
	as(png) replace width(2560) height(1440) //quality(100)	
	

graph drop _all 	
	
restore


* Education
preserve

collapse (mean) dlltsd01 pred_dlltsd01* [aw = dwt], by(deh_c3 stm)

order pred_dlltsd01*

egen pred_dlltsd01 = rowmean(pred_dlltsd010-pred_dlltsd0119)

replace stm = 2000 + stm 

twoway ///
(line pred_dlltsd01 stm if deh_c3 == 1, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line dlltsd01 stm if deh_c3 == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), /// 
name(graph1) title("High education") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_dlltsd01 stm if deh_c3 == 2, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dlltsd01 stm if deh_c3 == 2, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph2) title("Medium education") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_dlltsd01 stm if deh_c3 == 3, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dlltsd01 stm if deh_c3 == 3, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph3) title("Low education") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

grc1leg graph1 graph2 graph3 ,  ///
	title("Disability/long-term sick") ///
	subtitle("Left initial education spell") ///
	legendfrom(graph1) rows(2) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who have left their initial education spell and aged 18+. Initial education spell defined" "generously.", size(vsmall))
	
graph export "$dir_validation_graphs/health/int_validation_H2b_disablilty_left_edu_ts_all_both_edu.png", ///
	as(png) replace width(2560) height(1440) //quality(100)	
	
graph drop _all 	
	
restore


* Marital status 
preserve

collapse (mean) dlltsd01 pred_dlltsd01* [aw = dwt], by(dcpst stm)

order pred_dlltsd01*

egen pred_dlltsd01 = rowmean(pred_dlltsd010-pred_dlltsd0119)

replace stm = 2000 + stm 

twoway ///
(line pred_dlltsd01 stm if dcpst == 1, sort color(green) ///
	legend(label(1 "Predicted"))) ///
(line dlltsd01 stm if dcpst == 1, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Observed"))), ///
name(graph1) title("Partnered") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_dlltsd01 stm if dcpst == 2, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dlltsd01 stm if dcpst == 2, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph2) title("Single") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

twoway ///
(line pred_dlltsd01 stm if dcpst == 3, sort color(green) ///
	legend(label(1 "Pred"))) ///
(line dlltsd01 stm if dcpst == 3, sort color(green) color(green%20) ///
	lpattern(dash) legend(label(2 "Obs"))), ///
name(graph3) title("Previously partnered") xtitle("Year") ytitle("") ///
	xlabel(, labsize(small)) ylabel(, labsize(small)) ///
	legend(size(small)) ///
	graphregion(color(white))

grc1leg graph1 graph2 graph3,  ///
	title("Disability/long-term sick") ///
	subtitle("Left initial education spell") ///
	legendfrom(graph1) rows(2) ///
	graphregion(color(white)) ///
	note("Notes: Estimation sample plotted. Sample contains individuals who have left their initial education spell and aged 18+. Initial education spell defined" "generously.", size(vsmall))
	
graph export "$dir_validation_graphs/health/int_validation_H2b_disablilty_left_edu_ts_all_both_partnership.png", ///
	as(png) replace width(2560) height(1440) //quality(100)	

graph drop _all 	
	
restore
