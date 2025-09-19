********************************************************************************
* PROJECT:  		ESPON 
* SECTION:			Validation
* OBJECT: 			Correlation
* AUTHORS:			Patryk Bronka, Ashley Burdett 
* LAST UPDATE:		06/2025 (AB)
* COUNTRY: 			Greece 

* NOTES: 			This file calculates correlations between variables of 
* 					interest, in observed and simulated data
* 
* 					List of variables considered. Name : simulated : validation
* 
* 					1. disposable income per benefit unit : sim_y_disp_yr_bu : 
* 						valid_y_disp_yr_bu 
* 					2. labour market status : sim_employed, sim_student, 
* 						sim_inactive, sim_retired : valid_employed, 
* 						valid_student, valid_inactive, valid_retired

********************************************************************************

global sim_varlist sim_employed sim_inactive sim_retired sim_edu_high ///
	sim_edu_med sim_edu_low sim_y_gross_yr_bu sim_yplgrs_dv_lvl_bu ///
	sim_ypncp_lvl_bu sim_ypnoab_lvl_bu sim_y_disp_yr_bu equivalisedincome ///
	/*potential_earnings_hourly*/ hoursworkedweekly sim_dcpst_snmprvp ///
	sim_dcpst_p dhe

global valid_varlist valid_employed valid_inactive valid_retired ///
	valid_edu_high valid_edu_med valid_edu_low valid_y_gross_nsbc_yr_bu ///
	valid_y_gross_labour_yr_bu capital_income_bu pension_income_bu ///
	valid_y_disp_yr_bu valid_y_eq_disp_yr_bu /*valid_wage_hour*/ valid_lhw ///
	valid_dcpst_snmprvp valid_dcpst_p dhe

/*
Simulated correlations 
*/

use run year ${sim_varlist} using "$dir_data/simulated_data.dta", clear

lab var dhe "Health"
lab var sim_employed "Employed"
lab var sim_inactive "Non-employed"
lab var sim_retired "Retired"
lab var sim_edu_high "High education"
lab var sim_edu_med "Medium education"
lab var sim_edu_low "Low education"
lab var sim_y_disp_yr_bu "Disposable income"
lab var sim_y_gross_yr_bu "Gross income"
lab var sim_yplgrs_dv_lvl_bu "Gross labour income"
lab var sim_ypncp_lvl_bu "Capital income"
lab var sim_ypnoab_lvl_bu "Private pension income"
lab var equivalisedincome "Equivalised disposable income"
//lab var potential_earnings_hourly "Hourly wage"
lab var hoursworkedweekly "Hours worked"
lab var sim_dcpst_snmprvp "Single"
lab var sim_dcpst_p "Partnered"

keep if run == 1

quietly correlate ${sim_varlist}
matrix CS = r(C)

heatplot CS, values(format(%3.2f) size(1.1)) cuts(-1.05(.1)1.05) ///
	color(hcl diverging, intensity(.6)) legend(off) aspectratio(1) ///
	lower label xlabel(, angle(90) labsize(vsmall)) ///
	ylabel(, labsize(vsmall)) title("Simulated") name(sim_corr, replace) ///
	graphregion(color(white))

* Save figure
graph export ///
	"$dir_output_files/correlations/validation_correlations_simulated_${min_age}_${max_age}.jpg", ///
	replace width(2560) height(1440) quality(100)

/*
Observed correlations 
*/

use year dwt ${valid_varlist} using ///
	"$dir_data/${country}-eusilc_validation_sample.dta", clear

lab var dhe "Health"
lab var valid_employed "Employed"
lab var valid_inactive "Non-employed"
lab var valid_retired "Retired"
lab var valid_edu_high "High education"
lab var valid_edu_med "Medium education"
lab var valid_edu_low "Low education"
lab var valid_y_disp_yr_bu "Disposable income"
lab var valid_y_gross_nsbc_yr_bu "Gross income"
lab var valid_y_gross_labour_yr_bu "Gross labour income"
lab var capital_income_bu "Capital income"
lab var pension_income_bu "Private pension income"
lab var valid_y_eq_disp_yr_bu "Equivalised disposable income"
//lab var valid_wage_hour "Hourly wage"
lab var valid_lhw "Hours worked"
lab var valid_dcpst_p "Partnered"
lab var valid_dcpst_snmprvp "Single"

replace valid_lhw = 0 if valid_inactive == 1 | valid_retired == 1 

quietly correlate ${valid_varlist}

matrix CV = r(C)

heatplot CV, values(format(%3.2f) size(1.1)) cuts(-1.05(.1)1.05) ///
	color(hcl diverging, intensity(.6)) legend(off) aspectratio(1) ///
	lower label xlabel(, angle(90) labsize(vsmall)) ///
	ylabel(, labsize(vsmall)) title("Observed") name(obs_corr, replace) ///
	graphregion(color(white))


* Save figure
graph combine sim_corr obs_corr, title("Correlation coefficients") /// 
	note("Notes: Ages 18-65 included. ", size(vsmall)) graphregion(color(white))


graph export ///
"$dir_output_files/correlations/validation_correlations_simulated_observed_${min_age}_${max_age}.png", ///
	replace width(2560) height(1440)

/*
Calculate the difference and absolute difference matrix 
*/

matrix CDiff = CS - CV
//matewmf CDiff CDiffAbs, f(abs)

/*
* Heatplot for the distance matrix
heatplot CDiff, values(format(%3.2f) size(tiny)) cuts(-1.05(.1)1.05) ///
	color(hcl diverging, intensity(.6)) legend(off) aspectratio(1) ///
	lower label xlabel(, angle(45) labsize(small)) ///
	ylabel(, labsize(small)) ///
	title("Distance between simulated" "and observed correlations")
	
* Save figure
graph export ///
 "$dir_output_files/validation_correlations_distance_${min_age}_${max_age}.jpg", ///
	replace width(2560) height(1440) quality(100)
*/

* Heatplot for the difference matrix
heatplot CDiff, values(format(%3.2f) size(tiny)) cuts(-1.05(.1)1.05) ///
	color(hcl diverging, intensity(.6)) legend(off) aspectratio(1) ///
	lower label xlabel(, angle(90) labsize(vsmall)) ///
	ylabel(, labsize(vsmall)) ///
	title("Difference between simulated" "and observed correlations") ///
	note("Positive values indicate that simulated correlation was stronger than observed", size(vsmall)) graphregion(color(white))

* Save figure
graph export /// 
"$dir_output_files/correlations/validation_correlations_difference_${min_age}_${max_age}.jpg", ///
	replace width(2560) height(1440) quality(100) 


graph drop _all 

