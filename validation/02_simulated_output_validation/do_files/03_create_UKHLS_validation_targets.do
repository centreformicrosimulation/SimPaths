/*******************************************************************************
* PROJECT:  		SimPaths UK
* SECTION:			Validation
* OBJECT:			Validation data processing
* AUTHORS:			Ashley Burdett
* LAST UPDATE:		Feb 2026
* COUNTRY: 			UK  
* DESCRIPTION:      This file creates the validation target variables 
* 					using UKHLS initial populations data. 
********************************************************************************
* NOTES: 			The income amounts in annual terms.  
* 					Currently construct gross income from components. 
* 
*******************************************************************************/

* Generate/Tidy required variables

* Load UKHLS data
use "${dir_UKHLS_data}/UKHLS_pooled_ipop.dta", clear


** IDENTIFIERS
rename idhh idHh
rename idbenefitunit idBu
rename idperson idPers
rename idpartner idPartner
rename idmother idMother
rename idfather idFather
rename swv statCollectionWave


** TIME

fre stm 

gen year = stm 
//replace year = year + 2000


** DEMOGRAPHICS

* Gender 
rename dgn demMaleFlag

replace demMaleFlag = . if demMaleFlag < 0


* Age 
rename dag demAge

replace demAge = . if demAge < 0

* Define age groups
gen ageGroup = .
replace ageGroup = 0 if demAge >= 0 & demAge < 15
replace ageGroup = 1 if demAge >= 15 & demAge < 20
replace ageGroup = 2 if demAge >= 20 & demAge < 25
replace ageGroup = 3 if demAge >= 25 & demAge < 30
replace ageGroup = 4 if demAge >= 30 & demAge < 35
replace ageGroup = 5 if demAge >= 35 & demAge < 40
replace ageGroup = 6 if demAge >= 40 & demAge < 60
replace ageGroup = 7 if demAge >= 60 & demAge < 80
replace ageGroup = 8 if demAge >= 80 & demAge <= 100

label def ageGrou /// 
	0 "ageGroup_0_14" ///
	1 "ageGroup_15_19" ///
	2 "ageGroup_20_24" ///
	3 "ageGroup_25_29" ///
	4 "ageGroup_30_34" ///
	5 "ageGroup_35_39" ///
	6 "ageGroup_40_59" ///
	7 "ageGroup_60_79" ///
	8 "ageGroup_80_100" ///
	
label val ageGroup ageGroup
fre ageGroup

gen ageGroup2 = .
replace ageGroup2 = 0 if demAge >= 16 & demAge < 25
replace ageGroup2 = 1 if demAge >= 25 & demAge < 30
replace ageGroup2 = 2 if demAge >= 30 & demAge < 35
replace ageGroup2 = 3 if demAge >= 35 & demAge < 40
replace ageGroup2 = 4 if demAge >= 40 & demAge < 45
replace ageGroup2 = 5 if demAge >= 45 & demAge < 50
replace ageGroup2 = 6 if demAge >= 50 & demAge < 55
replace ageGroup2 = 7 if demAge >= 55 & demAge < 60
replace ageGroup2 = 8 if demAge >= 60 & demAge <= 65

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


* Partnership status  
rename dcpst demPartnerStatus

gen valid_partnered = (demPartnerStatus == 1) 
gen valid_single = (demPartnerStatus == 2) 

replace valid_partnered = . if demPartnerStatus == . | demPartnerStatus < 0 
replace valid_single = . if demPartnerStatus == . | demPartnerStatus < 0 
	
	
* Number of children
rename dnc demNChild
rename dnc02 demNChild0to2	// same within a BU 

gen new_born = (demAge == 0 & demNChild0to2 != 0)
bysort idBu year (new_born): gen demNChild0 = (new_born[_N] == 1)

gen children_0 = (demNChild == 0)
gen children_1 = (demNChild == 1)
gen children_2 = (demNChild == 2)
gen children_3plus = (demNChild >= 3 & demNChild != .)


* Interaction of partnership status and number of children
foreach var1 in valid_partnered valid_single {
	
	foreach var2 in children_0 children_1 children_2 children_3plus {
	
		gen `var1'_`var2' = (`var1' & `var2')
	
	}

}


** EDUCATION
* Education dummies
rename deh_c4 eduHighestC4

gen valid_edu_na = (eduHighestC4 == 0)
gen valid_edu_high = (eduHighestC4 == 1)
gen valid_edu_med = (eduHighestC4 == 2)
gen valid_edu_low = (eduHighestC4 == 3) 

replace valid_edu_na = . if eduHighestC4 == . | eduHighestC4 < 0 
replace valid_edu_high = . if eduHighestC4 == . | eduHighestC4 < 0 
replace valid_edu_med = . if eduHighestC4 == . | eduHighestC4 < 0 
replace valid_edu_low = . if eduHighestC4 == . | eduHighestC4 < 0 


** HEALTH 

* Disabed / LT sick 
rename dlltsd01 healthDsblLongtermFlag

gen valid_healthDsblLongtermFlag = healthDsblLongtermFlag
replace valid_healthDsblLongtermFlag = . if valid_healthDsblLongtermFlag < 0 


* Self rated health
rename dhe valid_healthSelfRated 

replace valid_healthSelfRated = . if valid_healthSelfRated < 0

* MCS
rename dhe_mcs valid_healthMentalMcs

replace valid_healthMentalMcs = . if valid_healthMentalMcs < 0 

* PCS
rename dhe_pcs valid_healthPhysicalPcs

replace valid_healthPhysicalPcs = . if valid_healthPhysicalPcs < 0


** LABOUR MARKET 

* Economic activity dummies
rename les_c3 labC3
rename les_c4 labC4

gen valid_employed = (labC4 == 1)
gen valid_student = (labC4 == 2)
gen valid_inactive = (labC4 == 3)
gen valid_retired = (labC4 == 4)

replace valid_employed = . if labC4 < 0 | labC4 == . 
replace valid_student = . if labC4 < 0 | labC4 == . 
replace valid_inactive = . if labC4 < 0 | labC4 == . 
replace valid_retired = . if labC4 < 0 | labC4 == . 

* Hours worked weekly (continuous)
rename lhw labHrsWorkWeek

gen valid_labHrsWorkWeek = labHrsWorkWeek

* Hours workd weekly (categories)
/*
"This version uses 7 labour supply alternatives:")
("0 hours ==> non-employment alternative.")
("10 hours ==> 6–15 hours bracket.")
("20 hours ==> 16–25 hours bracket.")
("30 hours ==> 26–35 hours bracket.")
("38 hours ==> 36–40 hours bracket.")
("45 hours ==> 41–49 hours bracket.")
("55 hours ==> 50+ hours bracket.")
*/
gen valid_labHrsWorkEnumWeek = "ZERO" 
replace valid_labHrsWorkEnumWeek = "TEN" if ///
	labHrsWorkWeek >= 6 & labHrsWorkWeek <= 15 
replace valid_labHrsWorkEnumWeek = "TWENTY" if ///
	labHrsWorkWeek > 15 & labHrsWorkWeek <= 25
replace valid_labHrsWorkEnumWeek = "THIRTY" if ///
	labHrsWorkWeek > 25 & labHrsWorkWeek <= 35 
replace valid_labHrsWorkEnumWeek = "THIRTY_EIGHT" if ///
	labHrsWorkWeek > 35 & labHrsWorkWeek <= 40 
replace valid_labHrsWorkEnumWeek = "FORTY_FIVE" if ///
	labHrsWorkWeek > 40 & labHrsWorkWeek <= 49
replace valid_labHrsWorkEnumWeek = "FIFTY_FIVE" if ///
	labHrsWorkWeek > 49 & labHrsWorkWeek != .

gen valid_labHrsWorkEnum_no = .
replace valid_labHrsWorkEnum_no = 0 if valid_labHrsWorkEnumWeek == "ZERO" 
replace valid_labHrsWorkEnum_no = 10 if valid_labHrsWorkEnumWeek == "TEN" 
replace valid_labHrsWorkEnum_no = 20 if valid_labHrsWorkEnumWeek == "TWENTY" 
replace valid_labHrsWorkEnum_no = 30 if valid_labHrsWorkEnumWeek == "THIRTY" 
replace valid_labHrsWorkEnum_no = 38 if valid_labHrsWorkEnumWeek == "THIRTY_EIGHT" 
replace valid_labHrsWorkEnum_no = 45 if valid_labHrsWorkEnumWeek == "FORTY_FIVE" 
replace valid_labHrsWorkEnum_no = 55 if valid_labHrsWorkEnumWeek == "FIFTY_FIVE" 

* Categorical variable 
gen valid_cat_hours = .
replace valid_cat_hours = 1 if valid_labHrsWorkEnumWeek == "ZERO" 
replace valid_cat_hours = 2 if valid_labHrsWorkEnumWeek == "TEN" 
replace valid_cat_hours = 3 if valid_labHrsWorkEnumWeek == "TWENTY" 
replace valid_cat_hours = 4 if valid_labHrsWorkEnumWeek == "THIRTY" 
replace valid_cat_hours = 5 if valid_labHrsWorkEnumWeek == "THIRTY_EIGHT" 
replace valid_cat_hours = 6 if valid_labHrsWorkEnumWeek == "FORTY_FIVE" 
replace valid_cat_hours = 7 if valid_labHrsWorkEnumWeek == "FIFTY_FIVE" 

* Hourly wage 
// obs_earnings_hourly - alternative only containing observed wages
// pred_hourly_wage - alternative containing predicted wages
gen valid_wage =  obs_earnings_hourly


** INCOME (ANNUAL)
/*
Amounts of personal income stored with the IHS transformation. 
Benefit Unit level measure (gross and disposable income) are stored without the 
transformation. 

No missing observations in income amounts.
*/

* Annual individual employment gross income 
rename yplgrs_dv yEmpPersGrossMonth

sum yEmpPersGrossMonth
count if yEmpPersGrossMonth == . 

* Convert to levels 
gen yEmpPersGrossLevelMonth = sinh(yEmpPersGrossMonth) 
* Convert to annual 
gen valid_yEmpPersGrossLevelYear = yEmpPersGrossLevelMonth * 12

* Annual benefit unit gross employment income
bys year idBu: egen valid_yEmpBuGrossLevelYear = ///
	total(valid_yEmpPersGrossLevelYear)

	
* Annual individual capital income 
rename ypncp yCapitalPersMonth

sum yCapitalPersMonth
count if yCapitalPersMonth == . 

* Convert to levels
gen yCapitalPersLevelMonth = sinh(yCapitalPersMonth) 
* Convert to annual 
gen valid_yCapitalPersLevelYear = yCapitalPersLevelMonth * 12

* Annual benefit unit capital income
bys year idBu: egen valid_yCapitalBuLevelYear = ///
	total(valid_yCapitalPersLevelYear)


* Annual individual gross private pension income 
rename ypnoab yPensPersGrossMonth

sum yPensPersGrossMonth
count if yPensPersGrossMonth == . 

* Convert to levels
gen yPensPersGrossLevelMonth = sinh(yPensPersGrossMonth) 
* Convert to annual 
gen valid_yPensPersGrossLevelYear = yPensPersGrossLevelMonth * 12

* Annual benefit unit gross private pension income
bys year idBu: egen valid_yPensBuGrossLevelYear = ///
	total(valid_yPensPersGrossLevelYear)


* Annual individual gross non-benefit income 
/*
rename ypnbihs_dv yNonBenPersGrossMonth
* Convert to levels
gen yNonBenPersGrossLevelMonth = sinh(yNonBenPersGrossMonth) 
* Convert to annual
gen valid_yNonBenPersGrossLevelYear = yNonBenPersGrossLevelMonth * 12
*/
egen valid_yNonBenPersGrossLevelYear = ///
	rowtotal(valid_yPensPersGrossLevelYear valid_yCapitalPersLevelYear ///
	valid_yEmpPersGrossLevelYear)

* Annual benefit unit gross non-benefit income 
bys year idBu: egen valid_yNonBenBuGrossLevelYear = ///
	total(valid_yNonBenPersGrossLevelYear)

	
* Annual benefit unit gross income (level, non-benefit)
/*
Gross income is the same as non-benefit private income. 
*/
gen valid_yGrossBuLevelYear = valid_yNonBenBuGrossLevelYear

gen valid_yGrossPersLevelYear = valid_yNonBenPersGrossLevelYear
	
	
* Annual benefit unit disposable income (level)
rename ydisp yDispPersMonth		
* Convert to annual 
gen valid_yDispPersYear = yDispPersMonth * 12 

* Convert to benefit unit
bys year idBu: egen valid_yDispBuLevelYear = total(valid_yDispPersYear)

	
* Benefit unit - Net transfers 	
gen valid_net_transfers = valid_yDispBuLevelYear - valid_yNonBenBuGrossLevelYear
	
	
* Equivalised disposable income per benefit unit 

* Compute equivalence scale 
* Idenifty types of children 
gen is_older_child = 1 if inrange(demAge,14,18) & (idMother < . | idFather < .)
gen is_child = 1 if demAge < 14 & (idMother < . | idFather < .)

* Sum up number in hh 
bysort idHh: egen num_older_children = total(is_older_child)
bysort idHh: egen num_children = total(is_child)

* Compute Modified OECD equivalence scale 

gen moecd_eq = . 
replace moecd_eq = 1.5 if dhhtp_c4 == 1
replace moecd_eq = 0.3 * num_children + 0.5 * num_older_children + 1.5 if ///
	dhhtp_c4 == 2
replace moecd_eq = 1 if dhhtp_c4 == 3
replace moecd_eq = 0.3 * num_children + 0.5 * num_older_children + 1 if ///
	dhhtp_c4 == 4 

	
* Apply equivalence scale   
gen valid_yDispBuEquivYear = valid_yDispBuLevelYear / moecd_eq 

drop is_older_child is_child moecd_eq

	
** SOCIAL CARE 
/*
To align with the simulation set valud of those who aren't eligable equal to 0. 
Demand variables populated with 0 if age < 65.
Supply variables populated with 0 if age < 18. 
Missing = . 
Note: If missing any care demand info age > 64, then missing all. 
*/

* Care need flag 
rename need_socare valid_careNeedFlag 

replace valid_careNeedFlag = 0 if demAge < 65
replace valid_careNeedFlag = . if valid_careNeedFlag == -9

* Hours of informal care received 
/*
replace partner_socare_hrs = 0 if demAge < 65
replace daughter_socare_hrs = 0 if demAge < 65
replace son_socare_hrs = 0 if demAge < 65
replace other_socare_hrs = 0 if demAge < 65

replace partner_socare_hrs = . if partner_socare_hrs == -9 
replace daughter_socare_hrs = . if daughter_socare_hrs == -9 
replace son_socare_hrs = . if son_socare_hrs == -9 
replace other_socare_hrs = . if other_socare_hrs == -9 

egen valid_careHrsInformal = rowtotal(partner_socare_hrs ///
	daughter_socare_hrs son_socare_hrs other_socare_hrs)
	
replace valid_careHrsInformal = . if partner_socare_hrs == . | ///
	daughter_socare_hrs == . | son_socare_hrs == . | other_socare_hrs == . 
*/
rename informal_socare_hrs valid_careHrsInformal
replace valid_careHrsInformal = 0 if demAge < 65 
replace valid_careHrsInformal = . if valid_careHrsInformal == -9 
	
* Hours of formal care 	
rename formal_socare_hrs valid_careHrsFormal 
replace valid_careHrsFormal = 0 if demAge < 65
replace valid_careHrsFormal = 0 if valid_careHrsFormal == -9 

* Formal care cost 
rename formal_socare_cost valid_careFormalX 
replace valid_careFormalX = 0 if demAge < 65
replace valid_careFormalX = . if valid_careFormalX < 0 

* Hours of care provided
rename careHoursProvidedWeekly valid_careHrsProvidedWeek 
replace valid_careHrsProvidedWeek = 0 if demAge < 16
replace valid_careHrsProvidedWeek = . if valid_careHrsProvidedWeek == -9  


* Receive care flag 
gen valid_careReceiveFlag = . 
replace valid_careReceiveFlag = 0 if valid_careHrsInformal != .  | ///
	valid_careHrsFormal != . 
replace valid_careReceiveFlag = 1 if ///
	(valid_careHrsInformal > 0  & valid_careHrsInformal != .)| ///
	(valid_careHrsFormal > 0 & valid_careHrsFormal != . )
replace valid_careReceiveFlag = 0 if demAge < 65

	
* Receive only formal care flag 	
gen valid_careRecFormalOnly = ///
	(valid_careHrsInformal == 0 & valid_careHrsFormal > 0 & ///
		valid_careHrsFormal != .)
	
* Receive only informal care flag 	
gen valid_careRecInformalOnly = ///
	(valid_careHrsInformal > 0 & valid_careHrsInformal != . & ///
	valid_careHrsFormal == 0)		
	
* Receive both informal and formal care flag 	
gen valid_careRecMix = (valid_careHrsInformal > 0 & ///
	valid_careHrsFormal > 0 & valid_careHrsInformal != . & ///
	valid_careHrsFormal != .)
	
* Total care hours received 	
egen valid_careReceiveHrs = rowtotal(valid_careHrsInformal valid_careHrsFormal)
replace valid_careReceiveHrs = 0 if demAge < 65 
replace valid_careReceiveHrs = . if valid_careHrsInformal == . | ///
	valid_careHrsFormal == . 


* Provide care flag 
gen valid_careProvideFlag = (valid_careHrsProvidedWeek > 0 & ///
	valid_careHrsProvidedWeek != . ) 
replace valid_careProvideFlag = . if valid_careHrsProvidedWeek == . 

* Restrict sample to relevant valdiation years 
drop if year < ${min_sim_year}
drop if year > ${max_sim_year}
	
save "$dir_data/ukhls_validation_sample.dta", replace


graph drop _all 
