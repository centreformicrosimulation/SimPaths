********************************************************************************
* PROJECT:  		ESPON 
* SECTION:			Validation
* OBJECT: 			Income shares
* AUTHORS:			Patryk Bronka, Ashley Burdett 
* LAST UPDATE:		06/2025 (AB)
* COUNTRY: 			Greece 

* NOTES: 			This do file plots simulated and observed income shares and 
* 					incomes by deciles of gross income
* 					Altered pension age to 65 
********************************************************************************

********************************************************************************
* Observed
********************************************************************************

use year dwt max_age_in_bu gross_labour_income_share_bu ///
	pension_income_share_bu capital_income_share_bu social_income_share_bu ///
	net_income_share_bu gross_income_bu net_income_bu gross_labour_income_bu ///
	pension_income_bu capital_income_bu social_income_bu ///
	gross_income_bu_jittered using ///
	"$dir_data/${country}-eusilc_validation_sample.dta", clear

xtile obs_gross_income_group = gross_income_bu, nq(10)
//xtile obs_gross_income_group = gross_income_bu_jittered, nq(10)


* All ages

* Graph income shares (gross)
graph bar (mean) gross_labour_income_share_bu (mean) ///
	pension_income_share_bu (mean) capital_income_share_bu [aweight = dwt], ///
	over(obs_gross_income_group) stack title(`"Observed (all ages)"', ///
	size(medium)) ///
	legend(order(1 "Labour" 2 "Pension" 3 "Capital") position(6) rows(1)) ///
	name(observed_income_shares, replace) b1title("Decile", size(small)) ///
	ytitle("Share", size(small)) ylabel(0 [0.5] 1) ///
	graphregion(color(white))

* Graph income level (gross)
graph bar (mean) gross_labour_income_bu (mean) pension_income_bu (mean) ///
	capital_income_bu [aweight = dwt], ///
	over(obs_gross_income_group) stack title(`"Observed (all ages)"', ///
	size(medium)) ///
	legend(order(1 "Labour" 2 "Pension" 3 "Capital") position(6) rows(1)) ///
	name(observed_incomes, replace) ylabel(0 [100000] 100000) ///
	ytitle("€", size(small)) b1title("Decile", size(small)) ///
	graphregion(color(white))
	
* Graph share of net income in gross income	
graph bar (mean) net_income_share_bu [aweight = dwt], ///
	over(obs_gross_income_group) stack title(`"Observed (all ages)"', ///
	size(medium)) legend(order(1 "Net income") position(6) rows(1)) ///
	name(observed_net_income_shares, replace) ytitle("Share", size(small)) ///
	b1title("Decile", size(small)) ///
	graphregion(color(white))
	
* Graph income level (net)
graph bar (mean) net_income_bu [aweight = dwt], ///
	over(obs_gross_income_group) ///
	stack title(`"Observed (all ages)"', size(medium)) ///
	legend(order(1 "Net income") position(6) rows(1)) ///
	name(observed_net_income, replace) ylabel(0 [80000] 80000) ///
	ytitle("€", size(small)) b1title("Decile", size(small)) legend(off) ///
	graphregion(color(white))

	
* Oldest person above65 (pension age)

preserve
drop  obs_gross_income_group
keep if max_age_in_bu >= 65
xtile obs_gross_income_group = gross_income_bu, nq(10)

* Graph income shares (gross)
graph bar (mean) gross_labour_income_share_bu (mean) ///
	pension_income_share_bu (mean) capital_income_share_bu [pweight = dwt], ///
	over(obs_gross_income_group) stack title(`"Observed (oldest age >=65)"', ///
	size(medium)) ///
	legend(order(1 "Labour" 2 "Pension" 3 "Capital") position(6) rows(1)) ///
	name(observed_income_shares_o68, replace) ///
	b1title("Decile", size(small)) ///
	ytitle("Share", size(small)) ylabel(0 [0.5] 1) ///
	graphregion(color(white))

* Graph income level (gross)
graph bar (mean) gross_labour_income_bu (mean) pension_income_bu ///
	(mean) capital_income_bu [pweight = dwt], ///
	over(obs_gross_income_group) ///
	stack title(`"Observed (oldest age >=65)"', size(medium)) ///
	legend(order(1 "Labour" 2 "Pension" 3 "Capital") position(6) rows(1)) ///
	name(observed_incomes_o68, replace) ylabel(0 [100000] 100000) ///
	ytitle("€") b1title("Decile", size(small)) ///
	graphregion(color(white))

* Graph share of net income in gross income	
graph bar (mean) net_income_share_bu [pweight = dwt], ///
	over(obs_gross_income_group) ///
	stack title(`"Observed (oldest age >=65)"', size(medium)) ///
	legend(order(1 "Net income") position(6) rows(1)) ///
	name(observed_net_income_shares_o68, replace) ///
	b1title("Decile", size(small)) ytitle("Share", size(small)) ///
	graphregion(color(white))
	//ylabel(0 [0.5] 1)

// Graph income level (net)
graph bar (mean) net_income_bu [pweight = dwt], ///
	over(obs_gross_income_group) ///
	stack title(`"Observed (oldest age >=65)"', size(medium)) ///
	legend(order(1 "Net income") position(6) rows(1)) ///
	name(observed_net_income_o68, replace) ylabel(0 [80000] 80000) ///
	ytitle("£") b1title("Decile", size(small)) legend(off) ///
	graphregion(color(white))
	
	
* Oldest person below65 (pension age)

restore
drop  obs_gross_income_group
keep if max_age_in_bu <65
xtile obs_gross_income_group = gross_income_bu, nq(10)

* Graph income shares (gross)
graph bar (mean) gross_labour_income_share_bu (mean) ///
	pension_income_share_bu (mean) capital_income_share_bu [pweight = dwt], ///
	over(obs_gross_income_group) ///
	stack title(`"Observed (oldest age <65)"', size(medium)) ///
	legend(order(1 "Labour" 2 "Pension" 3 "Capital") position(6) rows(1)) ///
	name(observed_income_shares_u68, replace) ///
	b1title("Decile", size(small)) ytitle("Share", size(small)) ///
	ylabel(0 [0.5] 1) ///
	graphregion(color(white))

* Graph income level (gross)
graph bar (mean) gross_labour_income_bu (mean) pension_income_bu ///
	(mean) capital_income_bu [pweight = dwt], ///
	over(obs_gross_income_group) ///
	stack title(`"Observed (oldest age <65)"', size(medium)) ///
	legend(order(1 "Labour" 2 "Pension" 3 "Capital") position(6) rows(1)) ///
	name(observed_incomes_u68, replace) ylabel(0 [100000] 100000) ///
	ytitle("£") b1title("Decile", size(small)) ///
	graphregion(color(white))

* Graph share of net income in gross income	
graph bar (mean) net_income_share_bu [pweight = dwt], ///
	over(obs_gross_income_group) ///
	stack title(`"Observed (oldest age <65)"', size(medium)) ///
	legend(order(1 "Net income") position(6) rows(1)) ///
	name(observed_net_income_shares_u68, replace) ///
	b1title("Decile", size(small)) ytitle("Share", size(small)) ///
	graphregion(color(white))
	
* Graph income level (net)
graph bar (mean) net_income_bu [pweight = dwt], ///
	over(obs_gross_income_group) ///
	stack title(`"Observed (oldest age <65)"', size(medium)) ///
	legend(order(1 "Net income") position(6) rows(1)) ///
	name(observed_net_income_u68, replace) ylabel(0 [80000] 80000) ///
	ytitle("€") b1title("Decile", size(small)) legend(off) ///
	graphregion(color(white))


********************************************************************************
* Simulated
********************************************************************************

* Load simulated data
use run year idperson max_age_in_bu sim_yplgrs_dv_lvl_bu sim_ypnoab_lvl_bu ///
	sim_ypncp_lvl_bu sim_y_disp_yr_bu sim_y_gross_yr_bu using ///
	"$dir_data/simulated_data.dta", clear

gen calc_bu_gross_income = sim_yplgrs_dv_lvl_bu + sim_ypncp_lvl_bu + ///
	sim_ypnoab_lvl_bu 

collapse max_age_in_bu sim_yplgrs_dv_lvl_bu sim_ypnoab_lvl_bu ///
	sim_ypncp_lvl_bu sim_y_disp_yr_bu sim_y_gross_yr_bu ///
	calc_bu_gross_income, by(idperson year)

* Income shares:
gen gross_labour_income_share_bu = sim_yplgrs_dv_lvl_bu / calc_bu_gross_income
gen pension_income_share_bu = sim_ypnoab_lvl_bu / calc_bu_gross_income
gen capital_income_share_bu = sim_ypncp_lvl_bu / calc_bu_gross_income
gen net_income_share_bu = sim_y_disp_yr_bu / calc_bu_gross_income
replace net_income_share_bu = . if net_income_share_bu >= 50

xtile sim_gross_income_group = calc_bu_gross_income, nq(10)

* All ages

* Graph income shares (gross)
graph bar (mean) gross_labour_income_share_bu (mean) pension_income_share_bu ///
	(mean) capital_income_share_bu, over(sim_gross_income_group) ///
	stack title(`"Simulated (all ages)"', size(medium)) ///
	legend(order(1 "Labour" 2 "Pension" 3 "Capital") ///
	position(6) rows(1)) name(simulated_income_shares, replace) ///
	b1title("Decile", size(small)) ytitle("Share", size(small)) ///
	ylabel(0 [0.5] 1) ///
	graphregion(color(white))

* Graph income level (gross)
graph bar (mean) sim_yplgrs_dv_lvl_bu (mean) sim_ypnoab_lvl_bu ///
	(mean) sim_ypncp_lvl_bu, over(sim_gross_income_group) ///
	stack title(`"Simulated (all ages)"', size(medium)) ///
	legend(order(1 "Labour" 2 "Pension" 3 "Capital") ///
	position(6) rows(1)) name(simulated_incomes, replace) ///
	ylabel(0 [100000] 100000) ytitle("€") b1title("Decile", size(small)) ///
	graphregion(color(white))

* Graph share of net income in gross income	
graph bar (mean) net_income_share_bu, over(sim_gross_income_group) ///
	stack title(`"Simulated (all ages)"', size(medium)) ///
	legend(order(1 "Net income") position(6) rows(1)) ///
	name(simulated_net_income_shares, replace) ///
	b1title("Decile", size(small)) ///
	graphregion(color(white))

* Graph income level (net)
graph bar (mean) sim_y_disp_yr_bu, over(sim_gross_income_group) ///
	stack title(`"Simulated (all ages)"', size(medium)) ///
	legend(order(1 "Net income") position(6) rows(1)) ///
	legend(off) name(simulated_net_income, replace) ///
	ylabel(0 [80000] 80000) ytitle("€") ///
	b1title("Decile", size(small)) ytitle("€") ///
	graphregion(color(white))


* Oldest person above65 (pension age)

preserve
drop  sim_gross_income_group
keep if max_age_in_bu >= 65
xtile sim_gross_income_group = calc_bu_gross_income, nq(10)

* Graph income shares (gross)
graph bar (mean) gross_labour_income_share_bu (mean) pension_income_share_bu ///
	(mean) capital_income_share_bu, over(sim_gross_income_group) ///
	stack title(`"Simulated (oldest age >=65)"', size(medium)) ///
	legend(order(1 "Labour" 2 "Pension" 3 "Capital") position(6) rows(1)) ///
	name(simulated_income_shares_o68, replace) ///
	b1title("Decile", size(small)) ytitle("Share", size(small)) ///
	ylabel(0 [0.5] 1)  ///
	graphregion(color(white))

* Graph income level (gross)
graph bar (mean) sim_yplgrs_dv_lvl_bu (mean) sim_ypnoab_lvl_bu ///
	(mean) sim_ypncp_lvl_bu, over(sim_gross_income_group) ///
	stack title(`"Simulated (oldest age >=65)"', size(medium)) ///
	legend(order(1 "Labour" 2 "Pension" 3 "Capital") position(6) rows(1)) ///
	name(simulated_incomes_o68, replace) ylabel(0 [100000] 100000) ///
	ytitle("€") b1title("Decile", size(small)) ///
	graphregion(color(white))

* Graph share of net income in gross income	
graph bar (mean) net_income_share_bu, over(sim_gross_income_group) ///
	stack title(`"Simulated (oldest age >=65)"', size(medium)) ///
	legend(order(1 "Net income") position(6) rows(1)) ///
	name(simulated_net_income_shares_o68, replace) ///
	b1title("Decile", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white))

* Graph income level (net)
graph bar (mean) sim_y_disp_yr_bu, over(sim_gross_income_group) ///
	stack title(`"Simulated (oldest age >=65)"', size(medium)) ///
	legend(order(1 "Net income") position(6) rows(1)) ///
	name(simulated_net_income_o68, replace) ylabel(0 [80000] 80000) ///
	ytitle("€") b1title("Decile", size(small)) legend(off) ///
	graphregion(color(white))


* Oldest person below 68 (pension age)

restore
drop  sim_gross_income_group
keep if max_age_in_bu < 65
xtile sim_gross_income_group = calc_bu_gross_income, nq(10)

* Graph income shares (gross)
graph bar (mean) gross_labour_income_share_bu (mean) ///
	pension_income_share_bu (mean) capital_income_share_bu, ///
	over(sim_gross_income_group) ///
	stack title(`"Simulated (oldest age <65)"', size(medium)) ///
	legend(order(1 "Labour" 2 "Pension" 3 "Capital") ///
	position(6) rows(1)) name(simulated_income_shares_u68, replace) ///
	b1title("Decile", size(small)) ytitle("Share", size(small)) ///
	ylabel(0 [0.5] 1) ///
	graphregion(color(white))

* Graph income level (gross)
graph bar (mean) sim_yplgrs_dv_lvl_bu (mean) sim_ypnoab_lvl_bu ///
	(mean) sim_ypncp_lvl_bu, over(sim_gross_income_group) stack ///
	title(`"Observed (oldest age <65)"', size(medium)) ///
	legend(order(1 "Labour" 2 "Pension" 3 "Capital") position(6) rows(1)) ///
	name(simulated_incomes_u68, replace) ylabel(0 [100000] 100000) ///
	ytitle("") b1title("Decile", size(small)) ///
	graphregion(color(white))

* Graph share of net income in gross income	
graph bar (mean) net_income_share_bu, over(sim_gross_income_group) stack ///
	title(`"Observed (oldest age <65)"', size(medium)) ///
	legend(order(1 "Net income") position(6) rows(1)) ///
	name(simulated_net_income_shares_u68, replace) ///
	b1title("Decile", size(small)) ytitle("Share", size(small)) ///
	graphregion(color(white))

* Graph income level (net)
graph bar (mean) sim_y_disp_yr_bu, over(sim_gross_income_group) ///
	stack title(`"Observed (oldest age <65)"', ///
	size(medium)) legend(order(1 "Net income") ///
	position(6) rows(1)) legend(off) name(simulated_net_income_u68, replace) ///
	ylabel(0 [80000] 80000) ytitle("€") b1title("Decile", size(small)) ///
	graphregion(color(white))


********************************************************************************
* Combine graphs
********************************************************************************

* Simulated and observed gross income shares, 
* all / above pension age / below pension age
grc1leg simulated_income_shares observed_income_shares ///
	simulated_income_shares_o68 observed_income_shares_o68 ///
	simulated_income_shares_u68 observed_income_shares_u68, ///
	legendfrom(observed_income_shares) rows(3) ///
	graphregion(color(white)) ///
	title("Gross income shares") ///
	subtitle("By age and benefit unit gross income decile") ///
	note("Notes: Statistics computed at the benefit unit level. Based on values in € per year (2015 prices).", ///
	size(vsmall)) 

graph export "$dir_output_files/income/validation_${country}_income_shares.png", ///
		replace width(2400) height(1350)

		
* Simulated and observed income levels, 
* all / above pension age / below pension age
grc1leg simulated_incomes observed_incomes simulated_incomes_o68 ///
	observed_incomes_o68 simulated_incomes_u68 observed_incomes_u68, ///
	legendfrom(observed_incomes) rows(3) ///
	graphregion(color(white)) ///
	subtitle("Gross income sources, by age and ben unit gross income decile") ///
	note("Statistics computed at the benefit unit level. Values in € per year (2015 prices).", ///
	size(vsmall)) 

graph export ///
	"$dir_output_files/income/validation_${country}_combined_income_levels.png", ///
		replace width(2400) height(1350) 

* Simulated and observed net income share in gross,
*  all / above pension age / below pension age
grc1leg simulated_net_income_shares observed_net_income_shares ///
	simulated_net_income_shares_o68 observed_net_income_shares_o68 ///
	simulated_net_income_shares_u68 observed_net_income_shares_u68, ///
	legendfrom(observed_net_income_shares) rows(3) ///
	graphregion(color(white)) ///
	subtitle("Net income shares, by age and ben unit gross income decile") ///
	note("Statistics computed at the benefit unit level. Based on values in € per year (2015 prices).", ///
	size(vsmall)) 

//graph export "$dir_output_files/combined_net_income_share.jpg", ///
//	replace width(2560) height(1440) quality(100) 

// Simulated and observed net income level, 
* all / above pension age / below pension age
grc1leg simulated_net_income observed_net_income simulated_net_income_o68 ///
	observed_net_income_o68 simulated_net_income_u68 ///
	observed_net_income_u68, rows(3) ycommon ///
	subtitle("Net income, by age and ben unit gross income decile") ///
	legendfrom(simulated_net_income) ///
	graphregion(color(white)) ///
	note("Statistics computed at the benefit unit level. Values in € per year (2015 prices).", ///
	size(vsmall)) 

graph export ///
	"$dir_output_files/income/validation_${country}_combined_net_income_levels.png", ///
		replace width(2400) height(1350) 


graph drop _all 


/*
** Investigate components of capital income fpr third decile 

use "$dir_data/${country}-eusilc_validation_sample.dta", clear

// how many BU have no gross income?
sum gross_income_bu
count if gross_income_bu == 0 
	// 65,775 obs have no gross income from labour, capital or private pension 
	// 20% of bs have no gross income 
	
cap drop obs_gross_income_group
xtile obs_gross_income_group = gross_income_bu, nq(10)

//26% of BU have no gross income 
sum hy080g_pc if obs_gross_income_group == 3, de //inter-hh transfers 90% pop
sum hy110g_pc if obs_gross_income_group == 3, de //child income 95% pop
sum hy040g_pc if obs_gross_income_group == 3, de //property income 99% pop
sum hy090g_pc if obs_gross_income_group == 3, de //cap investments 99%
// inter hh transfer and child income are the largest sources 

sum py010g if obs_gross_income_group == 3, de //wages 90% pop 
sum py050g if obs_gross_income_group == 3, de //self emp 90% pop
// most don't have labour income 

// pension income small for all 


foreach var in hy080g_pc hy110g_pc hy040g_pc hy090g_pc py010g py050g {
	
	gen d_`var' = (`var' != 0)
	
}

tab d_hy080g_pc if obs_gross_income_group == 3 // 35% inter-hh transfers
tab d_hy110g_pc if obs_gross_income_group == 3 // 13% child income
tab d_hy040g_pc if obs_gross_income_group == 3 // 6% property income
tab d_hy090g_pc if obs_gross_income_group == 3 // 7% capital investments
tab d_py010g if obs_gross_income_group == 3 // 16% wages 
tab d_py050g if obs_gross_income_group == 3 // 17% self employment  

gen ind_work_income = (d_py010g == 1 | d_py050g == 1)
tab ind_work_income if obs_gross_income_group == 3

// => 68% report no income from work 



tab hhsize if obs_gross_income_group == 3

/*
     hhsize |      Freq.     Percent        Cum.
------------+-----------------------------------
          1 |        747        8.93        8.93
          2 |      1,624       19.41       28.33
          3 |      1,848       22.08       50.42
          4 |      1,660       19.84       70.26
          5 |      1,168       13.96       84.21
          6 |        739        8.83       93.04
          7 |        343        4.10       97.14
          8 |        130        1.55       98.70
          9 |         44        0.53       99.22
         10 |         47        0.56       99.78
         11 |         12        0.14       99.93
         12 |          4        0.05       99.98
         15 |          2        0.02      100.00	*/

* Age 
histogram dag if obs_gross_income_group == 3 // U-shaped 

* Activity 
tab les_c3 if obs_gross_income_group == 3 // 57% not employed, 19% students 


graph drop _all 
