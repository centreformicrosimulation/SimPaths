/*******************************************************************************
* SECTION:			Validation
* OBJECT:			Simulation data processing
* AUTHORS:			Ashley Burdett
* LAST UPDATE:		9/25
* COUNTRY: 			UK  

* DESCRIPTION:      This file creates variables from the simulated data
* 					that are used to generate in the comparison plots. 

* NOTES: 			Income amounts are converted from monthly to annual. 
* 					Two datasets are save, one containing all observations and 
* 					one containing only the adult population (18-65 inc).
*******************************************************************************/


* Generate required variables

use "$dir_data/simulated_data_prep1.dta", clear

* Sex
gen sim_sex = .
replace sim_sex = 1 if dgn == "Male"
replace sim_sex = 2 if dgn == "Female"

la def sim_sex_lb 1 "Male" 2 "Female"
la val sim_sex sim_sex_lb

* Hours worked weekly 
replace hoursworkedweekly = "" if hoursworkedweekly == "null"
destring hoursworkedweekly, replace

* Define age groups
gen ageGroup = .
replace ageGroup = 0 if dag >= 0 & dag <= 14
replace ageGroup = 1 if dag >= 15 & dag <= 19
replace ageGroup = 2 if dag >= 20 & dag <= 24
replace ageGroup = 3 if dag >= 25 & dag <= 29
replace ageGroup = 4 if dag >= 30 & dag <= 34
replace ageGroup = 5 if dag >= 35 & dag <= 39
replace ageGroup = 6 if dag >= 40 & dag <= 59
replace ageGroup = 7 if dag >= 60 & dag <= 79
replace ageGroup = 8 if dag >= 80 & dag <= 100

label def ageGrouplb /// 
	0 "ageGroup_0_14" ///
	1 "ageGroup_15_19" ///
	2 "ageGroup_20_24" ///
	3 "ageGroup_25_29" ///
	4 "ageGroup_30_34" ///
	5 "ageGroup_35_39" ///
	6 "ageGroup_40_59" ///
	7 "ageGroup_60_79" ///
	8 "ageGroup_80_100" ///
	
label val ageGroup ageGrouplb

gen ageGroup2 = .
replace ageGroup2 = 0 if dag >= 16 & dag <= 24
replace ageGroup2 = 1 if dag >= 25 & dag <= 29
replace ageGroup2 = 2 if dag >= 30 & dag <= 34
replace ageGroup2 = 3 if dag >= 35 & dag <= 39
replace ageGroup2 = 4 if dag >= 40 & dag <= 44
replace ageGroup2 = 5 if dag >= 45 & dag <= 49
replace ageGroup2 = 6 if dag >= 50 & dag <= 54
replace ageGroup2 = 7 if dag >= 55 & dag <= 59
replace ageGroup2 = 8 if dag >= 60 & dag <= 65

label def ageGrouplb2 /// 
	0 "ageGroup_16_24" ///
	1 "ageGroup_25_29" ///
	2 "ageGroup_30_34" ///
	3 "ageGroup_35_39" ///
	4 "ageGroup_40_44" ///
	5 "ageGroup_45_49" ///
	6 "ageGroup_50_54" ///
	7 "ageGroup_55_59" ///
	8 "ageGroup_60_65" ///
	
label val ageGroup2 ageGrouplb2


* Partnership status
gen sim_dcpst_p = (dcpst == "Partnered") // partnered
gen sim_dcpst_snm = (dcpst == "SingleNeverMarried") // single never married
gen sim_dcpst_prvp = (dcpst == "PreviouslyPartnered") // previously partnered
gen sim_dcpst_snmprvp = (dcpst == "SingleNeverMarried" | ///
	dcpst == "PreviouslyPartnered") 
	// single never married & previously partnered

replace idpartner = "" if idpartner == "null"
destring idpartner , replace
gen sim_has_partner = (idpartner != .)


* Number of children
gen child = (dag < $age_become_responsible)
bys run year idbenefitunit: egen sim_dnc = total(child)

gen child02 = (dag < 3)
bys run year idbenefitunit: egen sim_dnc02 = total(child02)

gen children_0 = (sim_dnc == 0)
gen children_1 = (sim_dnc == 1)
gen children_2 = (sim_dnc == 2)
gen children_3plus = (sim_dnc >= 3)


* Interact partnership status and number of children
foreach var1 in sim_dcpst_p sim_dcpst_snm sim_dcpst_prvp sim_dcpst_snmprvp {
	foreach var2 in children_0 children_1 children_2 children_3p {
	
		gen `var1'_`var2' = (`var1' & `var2')
	
	}
}


* Economic activity dummies
gen sim_employed = (les_c4 == "EmployedOrSelfEmployed")
gen sim_student = (les_c4 == "Student")
gen sim_inactive = (les_c4 == "NotEmployed")
gen sim_retired = (les_c4 == "Retired")

* Disabled / LT sick 
gen sim_dlltsd = (dlltsd == "True")

* Education dummies
gen sim_edu_high = (deh_c3 == "High")
gen sim_edu_med = (deh_c3 == "Medium")
gen sim_edu_low = (deh_c3 == "Low")

* Hours of work
rename laboursupplyweekly laboursupplyweekly_orig

gen lhw = .
replace lhw = 0 if laboursupplyweekly == "ZERO" 
replace lhw = 10 if laboursupplyweekly == "TEN" 
replace lhw = 20 if laboursupplyweekly == "TWENTY" 
replace lhw = 30 if laboursupplyweekly == "THIRTY" 
replace lhw = 40 if laboursupplyweekly == "FORTY" 

* Potential earnings
rename fulltimehourlyearningspotential potential_earnings_hourly

* Annual benefit unit disposable and gross income 
gen sim_y_disp_yr_bu = disposableincomemonthly * 12
gen sim_y_gross_yr_bu = grossincomemonthly * 12


* Annual individual gross income 
* Combine employment income, pension income and capital income 
foreach var in yplgrs_dv_lvl ypncp_lvl ypnoab_lvl {
	
	replace `var' = `var' * 12
	rename `var' sim_`var'

}

gen sim_y_gross_yr = sim_yplgrs_dv_lvl + sim_ypnoab_lvl + sim_ypncp_lvl


* Annual benefit unit employment income, pension income and capital income 
foreach observed_var in sim_yplgrs_dv_lvl sim_ypnoab_lvl sim_ypncp_lvl {
	
	bys run year idbenefitunit: egen `observed_var'_bu = total(`observed_var')

}

* Max beneift unit age
bys run year idbenefitunit: egen max_age_in_bu = max(dag)


* Save full population 
preserve 

* Restrict sample to observations up to and including specified maximum year
keep if year <= $max_year

save "$dir_data/simulated_data_full.dta", replace

restore 


* Restrict sample to individuals between min and max age defined in 00_master 
keep if dag >= $min_age & dag <= $max_age

* Restrict sample to observations up to and including specified maximum year
keep if year <= $max_year

save "$dir_data/simulated_data.dta", replace

//erase "$dir_data/simulated_data_prep1.dta"
