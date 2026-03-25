/*******************************************************************************
* PROJECT:  		SimPaths UK 
* SECTION:			Validation
* OBJECT: 			Income shares
* AUTHORS:			Patryk Bronka, Ashley Burdett 
* LAST UPDATE:		Jan 2026 
* COUNTRY: 			UK 
********************************************************************************
* NOTES: 			This do file plots simulated and observed income shares and 
* 					incomes by deciles of gross income
* 					Altered pension age to 65 

					TO UPDATE
*******************************************************************************/

** SimPaths output 


* Create variables  
 
use "$dir_data/simulation_sample.dta", clear 

* keep only one observation per benefit unit 
sort run idBu year  

* Check data structure
count if year == year[_n-1] & idBu == idBu[_n-1] & ///
		sim_yEmpBuGrossLevelYear != sim_yEmpBuGrossLevelYear[_n-1]

count if year == year[_n-1] & idBu == idBu[_n-1] & ///
		sim_yCapitalBuLevelYear != sim_yCapitalBuLevelYear[_n-1]
		
count if year == year[_n-1] & idBu == idBu[_n-1] & ///
		sim_yPensBuGrossLevelYear != sim_yPensBuGrossLevelYear[_n-1]

	
* Variables of interest 
	
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

gen zero_gross = (sim_yNonBenBuGrossLevelYear == 0) 
tab zero_gross 

/*
8.6% of observations have 0 gross non-benefit income.  
Equal shares in deciles. 
*/

* Genrate share variables 
gen sim_share_emp = sim_yEmpBuGrossLevelYear / sim_yNonBenBuGrossLevelYear
gen sim_share_cap = sim_yCapitalBuLevelYear / sim_yNonBenBuGrossLevelYear
gen sim_share_pen = sim_yPensBuGrossLevelYear / sim_yNonBenBuGrossLevelYear

order idPers idBu year sim_yNonBenBuGrossLevelYear sim_yEmpBuGrossLevelYear ///
	sim_yCapitalBuLevelYear sim_yPensBuGrossLevelYear sim_yDispBuLevelYear ///
	sim_share_*

gen check1 = sim_share_emp + sim_share_cap + sim_share_pen
sum check1, det

order idPers idBu year sim_decile sim_yNonBenBuGrossLevelYear ///
	sim_yEmpBuGrossLevelYear sim_yCapitalBuLevelYear ///
	sim_yPensBuGrossLevelYear check1 sim_share_*	

drop check*
	
	
* Plots

* Sources

* All 	
graph bar (mean) sim_yEmpBuGrossLevelYear (mean) sim_yCapitalBuLevelYear ///
	(mean) sim_yPensBuGrossLevelYear, over(sim_decile) stack ///
	title("SimPaths") ///
	legend(order(1 "Labour" 2 "Capital" 3 "Private Pension") ///
	position(6) rows(1)) ///
	name(simulated_income_comp_all, replace) ///
	b1title("Decile", size(small)) ///
	ytitle("£") ///
	ylabel(,labsize(small)) ///
	graphregion(color(white)) 

* Oldest <= 65	
preserve 

drop if demAge > 65

graph bar (mean) sim_yEmpBuGrossLevelYear (mean) sim_yCapitalBuLevelYear ///
	(mean) sim_yPensBuGrossLevelYear, over(sim_decile) stack ///
	title("SimPaths") ///
	legend(order(1 "Labour" 2 "Capital" 3 "Private Pension") ///
	position(6) rows(1)) ///
	name(simulated_income_comp_upto65, replace) ///
	b1title("Decile", size(small)) ///
	ytitle("£") ///
    ylabel(#5, format(%12.0fc)) ///
	ylabel(,labsize(small)) ///
	graphregion(color(white)) 

restore 	

* Oldest > 65
preserve 

drop if demAge <= 65

graph bar (mean) sim_yEmpBuGrossLevelYear (mean) sim_yCapitalBuLevelYear ///
	(mean) sim_yPensBuGrossLevelYear, over(sim_decile) stack ///
	title("SimPaths") ///
	legend(order(1 "Labour" 2 "Capital" 3 "Private Pension") ///
	position(6) rows(1)) ///
	name(simulated_income_comp_66plus, replace) ///
	b1title("Decile", size(small)) ///
	ytitle("£") ///
    ylabel(#5, format(%12.0fc)) ///
	ylabel(,labsize(small)) ///
	graphregion(color(white)) 

restore 	

* Shares		
preserve	
	
collapse (mean) sim_share_emp sim_share_cap sim_share_pen, ///
	by(sim_decile)

graph bar (asis) sim_share_emp sim_share_cap sim_share_pen, ///
    over(sim_decile) stack ///
    title("SimPaths") ///
    legend(label(1 "Employment") label(2 "Capital") ///
		label(3 "Private Pension")) ///
	name(simulated_income_share_all, replace) ///	
	graphregion(color(white)) 

restore 	

* Age <= 65	
preserve	

keep if demAge <= 65
	
collapse (mean) sim_share_emp sim_share_cap sim_share_pen, ///
	by(sim_decile)

graph bar (asis) sim_share_emp sim_share_cap sim_share_pen, ///
    over(sim_decile) stack ///
    title("SimPaths") ///
    legend(label(1 "Employment") label(2 "Capital") ///
		label(3 "Private Pension")) ///
	name(simulated_income_share_upto65, replace) ///	
	graphregion(color(white)) 

restore 	
	
* Age > 65	
preserve	

keep if demAge > 65
	
collapse (mean) sim_share_emp sim_share_cap sim_share_pen, ///
	by(sim_decile)

graph bar (asis) sim_share_emp sim_share_cap sim_share_pen, ///
    over(sim_decile) stack ///
    title("SimPaths") ///
    legend(label(1 "Employment") label(2 "Capital") ///
		label(3 "Private Pension")) ///
	name(simulated_income_share_66plus, replace) ///	
	graphregion(color(white)) 
	
restore 
	
	
** UKHLS data 	
	
use "$dir_data/ukhls_validation_sample.dta", clear
	
* keep only one observation per benefit unit 
sort idBu year  

* Check data structure
count if year == year[_n-1] & idBu == idBu[_n-1] & ///
		valid_yEmpBuGrossLevelYear != valid_yEmpBuGrossLevelYear[_n-1]

count if year == year[_n-1] & idBu == idBu[_n-1] & ///
		valid_yCapitalBuLevelYear != valid_yCapitalBuLevelYear[_n-1]
		
count if year == year[_n-1] & idBu == idBu[_n-1] & ///
		valid_yPensBuGrossLevelYear != valid_yPensBuGrossLevelYear[_n-1]

* Variable of interest 
	
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

gen zero_gross = (valid_yNonBenBuGrossLevelYear == 0) 
tab zero_gross 

/*
19.36% of observations have 0 gross non-benefit income.  
Non-equal shares in in bottom two deciles
*/

* Genrate share variables 
gen valid_share_emp = valid_yEmpBuGrossLevelYear / valid_yNonBenBuGrossLevelYear
gen valid_share_cap = valid_yCapitalBuLevelYear / valid_yNonBenBuGrossLevelYear
gen valid_share_pen = valid_yPensBuGrossLevelYear / valid_yNonBenBuGrossLevelYear

order idPers idBu year valid_yNonBenBuGrossLevelYear valid_yEmpBuGrossLevelYear ///
	valid_yCapitalBuLevelYear valid_yPensBuGrossLevelYear valid_yDispBuLevelYear ///
	valid_share_*

gen check1 = valid_share_emp + valid_share_cap + valid_share_pen
sum check1, det

order idPers idBu year valid_decile valid_yNonBenBuGrossLevelYear ///
	valid_yEmpBuGrossLevelYear valid_yCapitalBuLevelYear ///
	valid_yPensBuGrossLevelYear check1 valid_share_*	

drop check*	
	
* Plots

* Sources

* All 	
graph bar (mean) valid_yEmpBuGrossLevelYear (mean) valid_yCapitalBuLevelYear ///
	(mean) valid_yPensBuGrossLevelYear [aw = dwt], over(valid_decile) stack ///
    title("UKHLS") ///
	legend(order(1 "Labour" 2 "Capital" 3 "Private Pension") ///
	position(6) rows(1)) ///
	name(UKHLS_income_comp_all, replace) ///
	b1title("Decile", size(small)) ///
	ytitle("£") ///
    ylabel(#5, format(%12.0fc)) ///
	ylabel(,labsize(small)) ///
	graphregion(color(white)) 

* Oldest <= 65	
preserve 

drop if demAge > 65

graph bar (mean) valid_yEmpBuGrossLevelYear (mean) valid_yCapitalBuLevelYear ///
	(mean) valid_yPensBuGrossLevelYear [aw = dwt], over(valid_decile) stack ///
    title("UKHLS") ///
	legend(order(1 "Labour" 2 "Capital" 3 "Private Pension") ///
	position(6) rows(1)) ///
	name(UKHLS_income_comp_upto65, replace) ///
	b1title("Decile", size(small)) ///
	ytitle("£") ///
    ylabel(#5, format(%12.0fc)) ///
	ylabel(,labsize(small)) ///
	graphregion(color(white)) 

restore 	

* Oldest > 65
preserve 

drop if demAge <= 65

graph bar (mean) valid_yEmpBuGrossLevelYear (mean) valid_yCapitalBuLevelYear ///
	(mean) valid_yPensBuGrossLevelYear [aw = dwt], over(valid_decile) stack ///
	title("UKHLS") ///
	legend(order(1 "Labour" 2 "Capital" 3 "Private Pension") ///
	position(6) rows(1)) ///
	name(UKHLS_income_comp_66plus , replace) ///
	b1title("Decile", size(small)) ///
	ytitle("£") ///
    ylabel(#5, format(%12.0fc)) ///
	ylabel(,labsize(small)) ///
	graphregion(color(white))

restore 	

* Shares	

* All ages	
preserve	
	
collapse (mean) valid_share_emp valid_share_cap valid_share_pen [aw = dwt], ///
	by(valid_decile)

graph bar (asis) valid_share_emp valid_share_cap valid_share_pen, ///
    over(valid_decile) stack ///
    title("UKHLS") ///
    legend(label(1 "Employment") label(2 "Capital") ///
		label(3 "Private Pension")) ///
	name(UKHLS_income_share_all, replace) ///	
	graphregion(color(white)) 
restore 	

* Age <= 65	
preserve	

keep if demAge <= 65
	
collapse (mean) valid_share_emp valid_share_cap valid_share_pen [aw = dwt], ///
	by(valid_decile)

graph bar (asis) valid_share_emp valid_share_cap valid_share_pen, ///
    over(valid_decile) stack ///
    title("UKHLS") ///
    legend(label(1 "Employment") label(2 "Capital") ///
		label(3 "Private Pension")) ///
	name(UKHLS_income_share_upto65, replace) ///	
	graphregion(color(white)) 

restore 	
	
* Age > 65	
preserve	

keep if demAge > 65
	
collapse (mean) valid_share_emp valid_share_cap valid_share_pen [aw = dwt], ///
	by(valid_decile)

graph bar (asis) valid_share_emp valid_share_cap valid_share_pen, ///
    over(valid_decile) stack ///
    title("UKHLS") ///
    legend(label(1 "Employment") label(2 "Capital") ///
		label(3 "Private Pension")) ///
	name(UKHLS_income_share_66plus, replace) ///	
	graphregion(color(white)) 

restore	

	
* Combine plots 

* Composition  - All
grc1leg simulated_income_comp_all UKHLS_income_comp_all, ///
	rows(1) ycommon ///
	legendfrom(simulated_income_comp_all) ///
	graphregion(color(white)) ///
	title("Average Benefit Unit Gross Income Sources") ///
	subtitle("All age") ///
	note("NOTE: Benefit income excluded. Values in real 2015 amounts.", ///
	size(vsmall)) 
	
graph export ///
	"$dir_output_files/income/income_shares/validation_${country}_income_levels_all.png", ///
		replace width(2400) height(1350) 	
		
* Composition - Working age 
grc1leg simulated_income_comp_upto65 UKHLS_income_comp_upto65, ///
	rows(1) ycommon ///
	legendfrom(simulated_income_comp_upto65) ///
	graphregion(color(white)) ///
	title("Average Benefit Unit Gross Income Sources") ///
	subtitle("Oldest Working Age, <=65") ///
	note("NOTE: Benefit income excluded. Values in real 2015 amounts.", ///
	size(vsmall)) 
	
graph export ///
	"$dir_output_files/income/income_shares/validation_${country}_income_levels_upto65.png", ///
		replace width(2400) height(1350) 		
		

* Composition - Retirement age 
grc1leg simulated_income_comp_66plus UKHLS_income_comp_66plus, ///
	rows(1) ycommon ///
	legendfrom(simulated_income_comp_66plus) ///
	graphregion(color(white)) ///
	title("Average Benefit Unit Gross Income Sources") ///
	subtitle("Oldest Age > 65") ///
	note("NOTE: Benefit income excluded. Values in real 2015 amounts.", ///
	size(vsmall)) 
	
graph export ///
	"$dir_output_files/income/income_shares/validation_${country}_income_levels_66plus.png", ///
		replace width(2400) height(1350) 				
		


* Shares  - All
grc1leg simulated_income_share_all UKHLS_income_share_all, ///
	rows(1) ycommon ///
	legendfrom(simulated_income_share_all) ///
	graphregion(color(white)) ///
	title("Average Benefit Unit Gross Income Shares") ///
	subtitle("All age") ///
	note("NOTE: Benefit income excluded. Values in real 2015 amounts.", ///
	size(vsmall)) 
	
graph export ///
	"$dir_output_files/income/income_shares/validation_${country}_income_shares_all.png", ///
		replace width(2400) height(1350) 	
		
* Shares - Working age 
grc1leg simulated_income_share_upto65 UKHLS_income_share_upto65, ///
	rows(1) ycommon ///
	legendfrom(simulated_income_share_upto65) ///
	graphregion(color(white)) ///
	title("Average Benefit Unit Gross Income Shares") ///
	subtitle("Oldest Working Age, <=65") ///
	note("NOTE: Benefit income excluded. Values in real 2015 amounts.", ///
	size(vsmall)) 
	
graph export ///
	"$dir_output_files/income/income_shares/validation_${country}_income_sharess_upto65.png", ///
		replace width(2400) height(1350) 		
		

* Shares - Retirement age 
grc1leg simulated_income_share_66plus UKHLS_income_share_66plus, ///
	rows(1) ycommon ///
	legendfrom(simulated_income_share_66plus) ///
	graphregion(color(white)) ///
	title("Average Benefit Unit Gross Income Shares") ///
	subtitle("Oldest Age > 65") ///
	note("NOTE: Benefit income excluded. Values in real 2015 amounts.", ///
	size(vsmall)) 
	
graph export ///
	"$dir_output_files/income/income_shares/validation_${country}_income_sharess_66plus.png", ///
		replace width(2400) height(1350) 				
			

graph drop _all	

