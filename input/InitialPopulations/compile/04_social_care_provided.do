***************************************************************************************
* PROJECT:              SimPaths UK: construct initial populations for SimPaths using UKHLS data  
* DO-FILE NAME:         04_social_care_provided.do
* DESCRIPTION:          EXTRACT UKHLS DATA FOR SOCIAL CARE PROVISION TO INCLUDE IN INITIAL POPULATION
***************************************************************************************
* COUNTRY:              UK
* DATA:         	    UKHLS EUL version - UKDA-6614-stata [to wave o]
* AUTHORS: 				Justin van de Ven, Daria Popova 
* LAST UPDATE:          15 Jan 2026 DP
* NOTE:					Called from 00_master.do - see master file for further details
***************************************************************************************

***************************************************************************************
cap log close 
log using "${dir_log}/04_social_care_provided.log", replace
***************************************************************************************

/**********************************************************************
*	start analysis
**********************************************************************/
cd "${dir_data}"
disp "identifying social care provision"

// pooled data
foreach waveid in $scProvWaves {

	local waveno=strpos("abcdefghijklmnopqrstuvwxyz","`waveid'")

	use "${dir_ukhls_data}/`waveid'_indresp.dta", clear
	rename *, l
	rename `waveid'_* *
	gen swv = `waveno'
	keep pidp swv aidhrs aidhu*
	save "${dir_data}/int_temp.dta", replace
	
	use "${dir_ukhls_data}/`waveid'_egoalt.dta", clear
	rename *, l
	rename `waveid'_* *
	sort pidp apno
	forval ii = 1/16 {
		gen rindiv_here = 0
		replace rindiv_here = relationship_dv if (apno == `ii')
		by pidp: egen rindiv`ii' = max(rindiv_here)
		drop rindiv_here
	}
	gen chk = 0
	replace chk = 1 if (pidp == pidp[_n-1])
	drop if (chk==1)
	drop chk
	merge 1:1 pidp using "${dir_data}/int_temp.dta", keep(2 3) nogen
	keep pidp swv aidhrs aidhu* rindiv*
	save "${dir_data}/ukhls_scprov_`waveid'.dta", replace
}
clear all
foreach waveid in $scProvWaves {
	if ("`waveid'" == "f") {
		use "${dir_data}/ukhls_scprov_`waveid'.dta", clear
	}
	else {
		append using "${dir_data}/ukhls_scprov_`waveid'.dta"
	}
}
save "${dir_data}/ukhls_scprov_pooled0.dta", replace


/**************************************************************************************
*	process variables
**************************************************************************************/
use "ukhls_scprov_pooled0.dta", clear

// provision of care
forval ii = 1/16 {
	replace rindiv`ii' = 0 if (missing(rindiv`ii'))
}

// relationship to care recipient in household
gen care_partner = 0
gen care_parent = 0
gen care_child = 0
gen care_sibling = 0
gen care_grandchild = 0
gen care_grandparent = 0
gen care_otherfamily = 0
gen care_other = 0
forval ii = 1/16 {
	// loop over each individual
	
	replace care_partner = 1 if (aidhua`ii'>0 & rindiv`ii' > 0 & rindiv`ii' < 4)
	replace care_parent = 1 if (aidhua`ii'>0 & rindiv`ii' > 3 & rindiv`ii' < 9)
	replace care_child = 1 if (aidhua`ii'>0 & rindiv`ii' > 8 & rindiv`ii' < 14)
	replace care_sibling = 1 if (aidhua`ii'>0 & rindiv`ii' > 13 & rindiv`ii' < 20)
	replace care_grandchild = 1 if (aidhua`ii'>0 & rindiv`ii' == 20)
	replace care_grandparent = 1 if (aidhua`ii'>0 & rindiv`ii' == 21)
	replace care_otherfamily = 1 if (aidhua`ii'>0 & rindiv`ii' > 21 & rindiv`ii' < 26)
	replace care_other = 1 if (aidhua`ii'>0 & rindiv`ii' > 25)
}
replace care_parent = 1 if (aidhu1 == 1 | aidhu2 == 1)
replace care_grandparent = 1 if (aidhu1 == 2 | aidhu2 == 2)
replace care_otherfamily = 1 if (aidhu1 == 3 | aidhu2 == 3)
replace care_otherfamily = 1 if (aidhu1 == 4 | aidhu2 == 4)
replace care_other = 1 if (aidhu1 > 4 | aidhu2 > 4) 
gen care_others = care_sibling + care_grandchild + care_grandparent + care_otherfamily + care_other

gen aidhrs_adj = 0
replace aidhrs_adj = 2 if (aidhrs==1)
replace aidhrs_adj = 7 if (aidhrs==2)
replace aidhrs_adj = 14.5 if (aidhrs==3)
replace aidhrs_adj = 27 if (aidhrs==4)
replace aidhrs_adj = 42 if (aidhrs==5)
replace aidhrs_adj = 74.5 if (aidhrs==6)
replace aidhrs_adj = 120 if (aidhrs==7)
replace aidhrs_adj = 5.48 if (aidhrs==8) //weighted average of 1 to 3
replace aidhrs_adj = 71.5 if (aidhrs==9) //weighted average of 4 to 7

gen care_nonpartner = (care_parent + care_child + care_others > 0)
gen careWho = 0
replace careWho = 1 if (care_partner==1 & care_nonpartner==0)
replace careWho = 2 if (care_partner==1 & care_nonpartner==1)
replace careWho = 3 if (care_partner==0 & care_nonpartner==1)
label variable careWho "who person provides care to"
label define careWho 1 "partner only" 2 "partner and non-partner" 3 "non-partner only"

keep pidp swv careWho aidhrs_adj
rename aidhrs_adj aidhrs

rename pidp idperson
save "ukhls_scprov_pooled1.dta", replace


/**************************************************************************************
*	merge with main data set
**************************************************************************************/
disp "merge results with existing data"

use "UKHLS_pooled_all_obs_03.dta", clear

merge 1:1 idperson swv using ukhls_scprov_pooled1, keep(1 3) nogen

foreach var of varlist careWho /*aidhrs*/  {
	replace `var' = -9 if (missing(`var'))
}
recode aidhrs (.=0) 

//Add variable for capped care hours provided (as used in new labour supply estimates) 
cap gen max_possible_aidhrs = 168 - lhw - 42 //subrtact work and sleep time 
fre max_possible_aidhrs
gen aidhrs_excess = (aidhrs - max_possible_aidhrs) if aidhrs > max_possible_aidhrs
list aidhrs lhw max_possible_aidhrs aidhrs_excess if aidhrs_excess <. 
count if aidhrs_excess <. 
gen careHoursProvidedWeekly = aidhrs
replace careHoursProvidedWeekly = max_possible_aidhrs if aidhrs > max_possible_aidhrs & aidhrs <. 
assert careHoursProvidedWeekly <= max_possible_aidhrs
assert lhw+careHoursProvidedWeekly+42 <=168 
lab var careHoursProvidedWeekly "Weekly hours of care provided (capped)"
fre careHoursProvidedWeekly

save "ukhls_pooled_all_obs_care2.dta", replace 


/**************************************************************************************
* extend beyond interpolated sample (defined by firstWave and lastWave)
*************************************************************************************/
disp "impute data beyond interpolated sample"
use "ukhls_pooled_all_obs_care2.dta", clear 

// age variables
gen dage1 = 0
forval ii = 1/13 {
	replace dage1 = `ii' if (dag>=20+5*(`ii'-1) & dag<=25+5*(`ii'-1))
}
replace dage1 = 14 if (dag>85)

// adjusted life satisfaction
gen dls_adj = round(dls)

// partner age difference
gen age_diff = 0
replace age_diff = 0 if (dcpagdf < -10)
replace age_diff = 1 if (dcpagdf < -5) * (dcpagdf >= -10)
replace age_diff = 2 if (dcpagdf < 0) * (dcpagdf >= -5)
replace age_diff = 3 if (dcpagdf < 5) * (dcpagdf >= 0)
replace age_diff = 4 if (dcpagdf < 10) * (dcpagdf >= 5)
replace age_diff = 5 if (dcpagdf >= 10)

// partner
gen partnered = (dcpst==1)

// replace missing
replace dlltsd01 = 0 if (dlltsd01<0)			// missing set to zero - not disabled
replace dlltsd01_sp = 0 if (dlltsd01_sp<0)		// mostly single people - missing set to zero and partner dummy picks up the variation
replace dehsp_c3 = 0 if (dehsp_c3<0)			// mostly single people - missing set to zero and partner dummy picks up the variation
replace dhesp = 0 if (dhesp<0)					// mostly single people - missing set to zero and partner dummy picks up the variation
replace dhm_ghq=0 if (dhm_ghq<0)				// missing set to zero
replace drgn1=7 if (drgn1<0)					// missing set to london
replace jbstat = 4 if (jbstat<0)				// missing set to retired
replace ydses_c5 = 1 if (ydses_c5<0)			// missing set to lowest quintile (very few observations)

// hours care provided (double hurdle model)
gen prov_care = (careHoursProvidedWeekly > 0)
probit prov_care dgn i.deh_c3 i.dlltsd01 i.dlltsd01_sp i.dhhtp_c4 i.ydses_c5 unemp i.dehsp_c3 partnered i.dhe i.dhesp i.dhm_ghq i.dls_adj i.dot01 i.dage1 i.age_diff ib7.drgn1 i.jbstat widow [pweight=dimxwt] if (swv>6 & dag>17) 
predict ip if (swv<6 & dag>17)
gen ee = runiform()
gen ip2 = 0
replace ip2 = 1 if (ee<ip & swv<6 & dag>17)
drop ee ip
*tab swv prov_care if (dag>17)
*tab swv ip2 if (dag>17)

// use categorical variable to mitigate sparse distribution
gen hrs_grp = 0
replace hrs_grp = 1 if (careHoursProvidedWeekly>0 &   careHoursProvidedWeekly<=4)
replace hrs_grp = 2 if (careHoursProvidedWeekly>4 &   careHoursProvidedWeekly<=9)
replace hrs_grp = 3 if (careHoursProvidedWeekly>9 &   careHoursProvidedWeekly<=19)
replace hrs_grp = 4 if (careHoursProvidedWeekly>19 &  careHoursProvidedWeekly<=34)
replace hrs_grp = 5 if (careHoursProvidedWeekly>34 &  careHoursProvidedWeekly<=49)
replace hrs_grp = 6 if (careHoursProvidedWeekly>49 &  careHoursProvidedWeekly<=99)
replace hrs_grp = 7 if (careHoursProvidedWeekly>99)
oprobit hrs_grp dgn i.deh_c3 i.dlltsd01 i.dlltsd01_sp i.dhhtp_c4 i.ydses_c5 unemp i.dehsp_c3 partnered i.dhe i.dhesp i.dhm_ghq i.dls_adj i.dot01 i.dage1 i.age_diff ib7.drgn1 i.jbstat widow [pweight=dimxwt] if (swv>6 & prov_care==1 & dag>17)
forvalues ii = 1/7 {
	
	predict pp`ii' if (dag>17 & swv<6 & ip2==1), outcome(#`ii')
}
gen ee = runiform()
gen careHoursProvidedWeekly2 = 0
replace pp2 = pp2 + pp1
replace pp3 = pp3 + pp2
replace pp4 = pp4 + pp3
replace pp5 = pp5 + pp4
replace pp6 = pp6 + pp5
replace pp7 = pp7 + pp6
replace careHoursProvidedWeekly2 = 2 if (ee<=pp1 & swv<6 & ip2==1)
replace careHoursProvidedWeekly2 = 7 if (ee>pp1 & ee<=pp2 & swv<6 & ip2==1)
replace careHoursProvidedWeekly2 = 14.5 if (ee>pp2 & ee<=pp3 & swv<6 & ip2==1)
replace careHoursProvidedWeekly2 = 27.0 if (ee>pp3 & ee<=pp4 & swv<6 & ip2==1)
replace careHoursProvidedWeekly2 = 42.0 if (ee>pp4 & ee<=pp5 & swv<6 & ip2==1)
replace careHoursProvidedWeekly2 = 74.5 if (ee>pp5 & ee<=pp6 & swv<6 & ip2==1)
replace careHoursProvidedWeekly2 = 120  if (ee>pp6 & swv<6 & ip2==1)

rename careHoursProvidedWeekly careHoursProvidedWeekly_original
gen careHoursProvidedWeekly = careHoursProvidedWeekly_original
replace careHoursProvidedWeekly = careHoursProvidedWeekly2 if (swv<6)
drop ip2 pp1 pp2 pp3 pp4 pp5 pp6 pp7 ee
*tab hrs_grp if (swv>6)
*tab careHoursProvidedWeekly2 if (swv<6)


/**************************************************************************************
* save results
*************************************************************************************/
lab var careHoursProvidedWeekly "number of hours of informal social care provided per week (capped, includes imputed data)"
keep idperson swv careHoursProvidedWeekly
sort idperson swv
save temp, replace
use "ukhls_pooled_all_obs_care2.dta", clear
drop careHoursProvidedWeekly
merge 1:1 idperson swv using temp, keep(1 3) nogen
save "ukhls_pooled_all_obs_04.dta", replace 
sum careHoursProvidedWeekly
cap log close 


/**************************************************************************************
* clean-up and exit
**************************************************************************************/
#delimit ;
local files_to_drop 
	int_temp.dta
	ukhls_scprov_f.dta
	ukhls_scprov_g.dta
	ukhls_scprov_h.dta
	ukhls_scprov_i.dta
	ukhls_scprov_j.dta
	ukhls_scprov_k.dta
	ukhls_scprov_l.dta
	ukhls_scprov_m.dta
	ukhls_scprov_n.dta
	ukhls_scprov_o.dta
	ukhls_scprov_pooled0.dta
	ukhls_scprov_pooled1.dta
	temp.dta
	;
#delimit cr // cr stands for carriage return

foreach file of local files_to_drop { 
	erase "$dir_data/`file'"
}

/*check if filled in for all years 
need_socare formal_socare_hrs informal_socare_hrs formal_socare_cost carehoursprovidedweekly 
*/
sum need_socare formal_socare_hrs informal_socare_hrs formal_socare_cost careHoursProvidedWeekly

tab  stm need_socare
/*
           |  indicator variable for someone
           | who can only manage at least one
 Interview |         ADL with help or
      year |        -9          0          1 |     Total
-----------+---------------------------------+----------
        -9 |        10          0          0 |        10 
      2009 |    33,328      3,393      1,606 |    38,327 
      2010 |    69,130      7,270      3,982 |    80,382 
      2011 |    64,345      7,103      3,867 |    75,315 
      2012 |    56,570      6,655      3,686 |    66,911 
      2013 |    52,158      6,435      3,628 |    62,221 
      2014 |    49,469      6,670      3,473 |    59,612 
      2015 |    26,582     24,898      3,231 |    54,711 
      2016 |    12,828     37,234      3,149 |    53,211 
      2017 |    10,482     35,727      3,141 |    49,350 
      2018 |    10,064     34,196      3,233 |    47,493 
      2019 |     9,746     32,362      3,206 |    45,314 
      2020 |     9,437     31,014      2,760 |    43,211 
      2021 |     8,956     28,738      2,490 |    40,184 
      2022 |    25,552     14,400      3,105 |    43,057 
      2023 |    39,211      7,264      3,888 |    50,363 
      2024 |    16,309      2,958      1,653 |    20,920 
      2025 |       627          0        156 |       783 
-----------+---------------------------------+----------
     Total |   494,804    286,317     50,254 |   831,375 

*/

gen formal_socare_hrs_dummy = (formal_socare_hrs>0)
tab stm formal_socare_hrs_dummy
/*
           | formal_socare_hrs_dum
 Interview |          my
      year |         0          1 |     Total
-----------+----------------------+----------
        -9 |        10          0 |        10 
      2009 |    38,036        291 |    38,327 
      2010 |    79,710        672 |    80,382 
      2011 |    74,585        730 |    75,315 
      2012 |    66,268        643 |    66,911 
      2013 |    61,587        634 |    62,221 
      2014 |    58,950        662 |    59,612 
      2015 |    54,112        599 |    54,711 
      2016 |    52,628        583 |    53,211 
      2017 |    48,756        594 |    49,350 
      2018 |    46,902        591 |    47,493 
      2019 |    44,737        577 |    45,314 
      2020 |    42,813        398 |    43,211 
      2021 |    39,872        312 |    40,184 
      2022 |    42,531        526 |    43,057 
      2023 |    49,570        793 |    50,363 
      2024 |    20,634        286 |    20,920 
      2025 |       627        156 |       783 
-----------+----------------------+----------
     Total |   822,328      9,047 |   831,375 
*/

gen informal_socare_hrs_dummy = (informal_socare_hrs>0)
tab stm informal_socare_hrs_dummy
/*
           | informal_socare_hrs_d
 Interview |         ummy
      year |         0          1 |     Total
-----------+----------------------+----------
        -9 |        10          0 |        10 
      2009 |    37,504        823 |    38,327 
      2010 |    78,196      2,186 |    80,382 
      2011 |    73,104      2,211 |    75,315 
      2012 |    64,900      2,011 |    66,911 
      2013 |    60,170      2,051 |    62,221 
      2014 |    57,622      1,990 |    59,612 
      2015 |    52,914      1,797 |    54,711 
      2016 |    51,531      1,680 |    53,211 
      2017 |    47,706      1,644 |    49,350 
      2018 |    45,855      1,638 |    47,493 
      2019 |    43,712      1,602 |    45,314 
      2020 |    41,864      1,347 |    43,211 
      2021 |    38,960      1,224 |    40,184 
      2022 |    41,324      1,733 |    43,057 
      2023 |    48,179      2,184 |    50,363 
      2024 |    20,040        880 |    20,920 
      2025 |       627        156 |       783 
-----------+----------------------+----------
     Total |   804,218     27,157 |   831,375 
*/

gen formal_socare_cost_dummy = (formal_socare_cost>0)
tab stm formal_socare_cost_dummy
/*
           | formal_socare_cost_du
 Interview |          mmy
      year |         0          1 |     Total
-----------+----------------------+----------
        -9 |        10          0 |        10 
      2009 |    38,327          0 |    38,327 
      2010 |    79,710        672 |    80,382 
      2011 |    74,585        730 |    75,315 
      2012 |    66,268        643 |    66,911 
      2013 |    61,587        634 |    62,221 
      2014 |    58,950        662 |    59,612 
      2015 |    54,711          0 |    54,711 
      2016 |    52,628        583 |    53,211 
      2017 |    48,756        594 |    49,350 
      2018 |    46,902        591 |    47,493 
      2019 |    44,737        577 |    45,314 
      2020 |    42,813        398 |    43,211 
      2021 |    39,872        312 |    40,184 
      2022 |    42,531        526 |    43,057 
      2023 |    49,570        793 |    50,363 
      2024 |    20,634        286 |    20,920 
      2025 |       627        156 |       783 
-----------+----------------------+----------
     Total |   823,218      8,157 |   831,375 
*/
gen carehours_dummy = (careHoursProvidedWeekly>0)
tab stm carehours_dummy

/*
 Interview |    carehours_dummy
      year |         0          1 |     Total
-----------+----------------------+----------
        -9 |        10          0 |        10 
      2009 |    34,646      3,681 |    38,327 
      2010 |    72,106      8,276 |    80,382 
      2011 |    67,422      7,893 |    75,315 
      2012 |    59,629      7,282 |    66,911 
      2013 |    55,470      6,751 |    62,221 
      2014 |    52,440      7,172 |    59,612 
      2015 |    48,213      6,498 |    54,711 
      2016 |    47,041      6,170 |    53,211 
      2017 |    43,578      5,772 |    49,350 
      2018 |    42,113      5,380 |    47,493 
      2019 |    40,334      4,980 |    45,314 
      2020 |    38,392      4,819 |    43,211 
      2021 |    36,113      4,071 |    40,184 
      2022 |    38,668      4,389 |    43,057 
      2023 |    45,011      5,352 |    50,363 
      2024 |    18,675      2,245 |    20,920 
      2025 |       686         97 |       783 
-----------+----------------------+----------
     Total |   740,547     90,828 |   831,375 
*/

