/*******************************************************************************
* PROJECT:  		ESPON
* SECTION:			Validation
* OBJECT:			Validation data processing
* AUTHORS:			Ashley Burdett
* LAST UPDATE:		9/25
* COUNTRY: 			UK  

* DESCRIPTION:      This file creates the validation target variables 
* 					using UKHLS initial populations data. 

* NOTES: 			The income amounts in annual terms.  
*******************************************************************************/

* Load initial populations data 
use "${dir_data}/ukhls_pooled_all_obs_09.dta", clear 

* Restrict sample to observations up to and including specified maximum year
fre swv
keep if swv <= $max_year
gen year = stm 

* Further adjustments

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
fre ageGroup

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
fre ageGroup2

* Sex
replace dgn = . if dgn < 0

* Health variables
replace dhe = . if dhe < 0
replace dhe_mcs = . if dhe_mcs < 0
replace dhe_pcs = . if dhe_pcs < 0

*** Income variables - all annual unless stated

* Gross income per individual (non-benefit)
gen ypnb = sinh(ypnbihs_dv) 
gen valid_y_gross_ind_yr = ypnb * 12

* Gross income per benefit unit (non-benefit)
bys stm idhh idbenefitunit: ///
	egen valid_y_gross_bu_yr = total(valid_y_gross_ind_yr)

	
* Disposable income per individual 
* Not in the initial popualtions so need to merge in 
* Note the raw variable does not include deductions 
merge 1:1 idperson swv using "$dir_data/ukhls_ind_dispos_inc.dta"
drop if _m == 2 
drop _m

gen valid_y_disp_ind_yr = (fimnnet_dv/CPI) * 12

* Disposable income per benefit unit
bys stm idhh idbenefitunit: ///
	egen valid_y_disp_bu_yr = total(valid_y_disp_ind_yr)


* Gross labour income per individual
// yplgrs = fimnlabgrs_dv (ind)
gen y_gross_labour_ind_yr = sinh(yplgrs_dv) * 12

* Gross labour income per benefit unit 
bys stm idhh idbenefitunit: ///
	egen valid_y_gross_labour_bu_yr = total(y_gross_labour_ind_yr) 

	
* Gross private pension income 
gen y_gross_pension_ind_yr = sinh(ypnoab) * 12

bys stm idhh idbenefitunit: ///
	egen valid_y_gross_pension_bu_yr = total(y_gross_pension_ind_yr) 
	
	
* Capital income per benefit unit 
/* 
ypncp = rowtotal (fimninvnet_dv inc_fm inc_oth fimnprben_dv) 
investment, family payments, other reg payments, trade union, maintanence, 
sickness/accident
*/
gen y_gross_capital_ind_yr = sinh(ypncp) * 12

bys stm idhh idbenefitunit: ///
	egen valid_y_gross_capital_bu_yr = total(y_gross_capital_ind_yr) 

	
* Equivalised disposable income per benefit unit 

* Generate number of dependent children in a benefit unit
gen depChild = 1 if (dag >= 0 & dag < $age_become_responsible) 
bys swv idhh idbenefitunit: egen dnc_bu = sum(depChild)

gen depChild02 = 1 if (dag >= 0 & dag <= 2) 
bys swv idhh idbenefitunit: egen dnc02_bu = sum(depChild02)

lab var dnc02 "Number of dependent children 0 - 2"

* Generate modified-OECD equivalence scale: 1 for the household head, 0.5 for 
* additional adults, 0.3 for children < 14 years old 
bys swv idhh idbenefitunit: gen people_in_bu = _N
cap drop child 
gen child = (dag < 14)
bys swv idhh idbenefitunit: egen children_in_bu = total(child) 
gen other_adults = people_in_bu - children_in_bu - 1 
	// -1 for the household head

gen equiv_factor = 1 + (0.5 * other_adults) + (0.3 * children_in_bu) 
	// Start with 1 because each household must have at least the head
lab var equiv_factor "OECD-modified scale equivalence factor"

gen valid_y_eq_disp_bu_yr = valid_y_disp_bu_yr / equiv_factor 

drop child people_in_bu child children_in_bu other_adults dnc_bu dnc02_bu


** Annual income shares 


**  Gross income deciles (ben unit)

/*
xtile obs_gross_income_group = valid_y_gross_bu_yr, nq(10)  
	This is not correct for pooled data

Problem: if many observations have exactly the same value, xtile would group 
them into a single decile, causing one or more deciles to have very few 
observations. 
Adding a very small random amount can help differentiate tied values enough to 
distribute them more evenly across deciles without distorting the data 
meaningfully.
*/
gen valid_y_gross_bu_yr_jit = valid_y_gross_bu_yr + runiform() * 1e-5

forvalues stm = 2011/$max_year {
	
	xtile obs_gross_income_group_`stm' = valid_y_gross_bu_yr_jit if ///
		depChild != 1 & stm == `stm', nq(10)
		
	bys idhh: egen temp_obs_gross_income_group_`stm' = ///
		max(obs_gross_income_group_`stm') if stm == `stm'
		
	replace obs_gross_income_group_`stm' = ///
		temp_obs_gross_income_group_`stm' if ///
		missing(obs_gross_income_group_`stm')
	drop temp_obs_gross_income_group_`stm'

} 

* Unify into a single variable 
egen obs_gross_income_group = rowtotal(obs_gross_income_group_2011 ///
	obs_gross_income_group_2012 obs_gross_income_group_2013 ///
	obs_gross_income_group_2014 obs_gross_income_group_2015 ///
	obs_gross_income_group_2016 obs_gross_income_group_2017 ///
	obs_gross_income_group_2018 obs_gross_income_group_2019 ///
	obs_gross_income_group_2020 obs_gross_income_group_2021 ///
	obs_gross_income_group_2022 obs_gross_income_group_2023)

drop obs_gross_income_group_2*
bys stm: fre obs_gross_income_group


**  Activity status 

* Add adoption leave 
replace les_c4 = 1 if les_c4 == 15

* Activity dummies 
gen valid_employed = (les_c4 == 1)
gen valid_student = (les_c4 == 2)
gen valid_inactive = (les_c4 == 3)
gen valid_retired = (les_c4 == 4)

replace valid_employed = . if les_c4 < 0 | les_c4 == . 
replace valid_student = . if les_c4 < 0 | les_c4 == . 
replace valid_inactive = . if les_c4 < 0 | les_c4 == . 
replace valid_retired = . if les_c4 < 0 | les_c4 == . 


**  Education level 

* Attainment dummies 
gen valid_edu_high = (deh_c3 == 1)
gen valid_edu_med = (deh_c3 == 2)
gen valid_edu_low = (deh_c3 == 3) 

replace valid_edu_high = . if deh_c3 == . | deh_c3 < 0 
replace valid_edu_med = . if deh_c3 == . | deh_c3 < 0 
replace valid_edu_low = . if deh_c3 == . | deh_c3 < 0 


**  Family

* Partnership status  
gen valid_dcpst_p = (dcpst == 1) // partnered
gen valid_dcpst_snm = (dcpst == 2) // single never married
gen valid_dcpst_prvp = (dcpst == 3) // previously partnered
gen valid_dcpst_snmprvp = (dcpst == 2 | dcpst == 3) 
	// single never married & previously partnered

replace valid_dcpst_p = . if dcpst == . | dcpst < 0 
replace valid_dcpst_snm = . if dcpst == . | dcpst < 0 
replace valid_dcpst_prvp = . if dcpst == . | dcpst < 0 
replace valid_dcpst_snmprvp = . if dcpst == . | dcpst < 0 
	
	
* Number of children
gen children_0 = (dnc == 0)
gen children_1 = (dnc == 1)
gen children_2 = (dnc == 2)
gen children_3plus = (dnc >= 3 & dnc != .)


* Interaction of partnership status and number of children
foreach var1 in valid_dcpst_p valid_dcpst_snm valid_dcpst_prvp ///
	valid_dcpst_snmprvp {
	
	foreach var2 in children_0 children_1 children_2 children_3p {
	
		gen `var1'_`var2' = (`var1' & `var2')
	
	}

}

** Hours worked  (weekly) 

* Impose consistency with les_c4 
* Prioritize les_c4 as we did with the European models
replace lhw = . if les_c4 != 1

count if (lhw == 0 | lhw == .) & les_c4 == 1
// note that 0s could be generated from missing values in rowtotal function

gen lhw_flag = 1 if (lhw < 6 | lhw == .) & les_c4 == 1

replace lhw = . if lhw == 0 & les_c4 == 1
replace lhw = . if lhw < 6 & les_c4 == 1

egen hours_mode = mode(lhw) 
replace lhw = hours_mode if les_c4 == 1 & lhw == . 

drop hours_mode

tab les_c4 if lhw != 0 & lhw != .

gen hours = lhw

* Labour supply categories
gen laboursupplyweekly_hu = "ZERO" 
replace laboursupplyweekly_hu = "TEN" if hours >= 6 & hours <= 15 
replace laboursupplyweekly_hu = "TWENTY" if hours > 15 & hours <= 25
replace laboursupplyweekly_hu = "THIRTY" if hours > 25 & hours <= 35 
replace laboursupplyweekly_hu = "FORTY" if hours > 35 & hours != .


* Hourly wages 
/*
There is only a very few missing values in the raw variable therefore treat the 
variable as if no missing information 
Zero values are possible if report negative gross labour income (self-employed)
*/
count if les_c4 == 1 & yplgrs_dv == 0 

gen valid_wage_hour = (sinh(yplgrs_dv)/4.345)/hours 

* Consistency check 
tab hours if les_c4 != 1
tab valid_wage_hour if les_c4 != 1

tab hours if les_c4 == 1


drop if dag < 0 

	
save "$dir_data/ukhls_validation_full_sample.dta", replace

	
* Restrict sample to individuals between min and max age defined in 
* 00_master file
keep if dag>= $min_age & dag <= $max_age

save "$dir_data/ukhls_validation_sample.dta", replace


