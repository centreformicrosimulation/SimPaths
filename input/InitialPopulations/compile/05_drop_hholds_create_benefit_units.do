***************************************************************************************
* PROJECT:              ESPON: construct initial populations for SimPaths using UKHLS data 
* DO-FILE NAME:         05_drop_hholds_create_ukhls_yearly_data.do
* DESCRIPTION:          Screens data and identifies benefit units
***************************************************************************************
* COUNTRY:              UK
* DATA:         	    UKHLS EUL version - UKDA-6614-stata [to wave m]
* AUTHORS: 				Daria Popova, Justin van de Ven
* LAST UPDATE:          10 Apr 2024 (JV)
* NOTE:					Called from 00_master.do - see master file for further details
***************************************************************************************


********************************************************************************
cap log close 
log using "${dir_log}/05_drop_hholds.log", replace
********************************************************************************

use "$dir_data\UKHLS_pooled_all_obs_04.dta", clear 
/*******************************************************************************/
fre ivfio
keep if ivfio == 1 | ivfio == 2 | ivfio == 21 | ivfio == 24 
fre ivfio
//(88,338 observations deleted) 
/******************************Split households********************************/

*DP: script from "Data management replication file"
/**********************Rules and assumptions***********************************
1. Each HH can contain: Responsible Male, and/or Responsible Female, Children, Other members.
In the simulation everyone starts as "Other member" and is assigned one of the roles in the HH.

	1.1. Responsible male and female create a partnership couple leading the HH. Any additional couple 
		 creates new HH. A couple with / composed of people under the age to leave home (18)
		 will still leave together and set up a new HH. 
		 
		 1.1.1. Children should follow the mother if she's moving to a new HH. 
		 
	1.2. After the above there should be only singles left in addition to the leading couple.
		 If they are above 18, they will leave and set up their own HH. 
	1.3. After the above there should only be children left in addition to the original HH.
		 Children will live with mother if defined in the data, otherwise with father. If neither
		 exists, they will be considered as orphans. 
	1.4. Orphans are assigned a woman or a man from the household in which they live as a parent. 
*/


/*
*Create unique partnership identifier within each household
/*Cond(x,a,b)
Description:  a if x is true and nonmissing, b if x is false; a if c is not specified and x evaluates to missing
pno -- person number
ppno -- partner's person number: PNO
*/
gen apartnum = cond(pno<ppno, pno, ppno) if ppno>0


*by idhh, new file with mother id, father id, and apartnum. Then assign that apartnum to child. 
preserve
keep swv idhh idperson apartnum
rename idperson idmother
rename apartnum apartnumm
gen idhhmother = idhh
save "$dir_data/temp_mother", replace
rename idmother idfather
rename apartnum apartnumf
gen idhhfather = idhh
save "$dir_data/temp_father", replace 
restore


merge m:1 swv idhh idmother using "$dir_data/temp_mother"
keep if _merge == 1 | _merge == 3
drop _merge
merge m:1 swv idhh idfather using "$dir_data/temp_father"
keep if _merge == 1 | _merge == 3
drop _merge


*Keep children under age to become responsible with parents unless their partner lives in the hh: 
*(children above age to become responsible will create independent households)
replace apartnum = apartnumm if missing(apartnum) & dag < $age_become_responsible & ppno == 0 //ppno == 0 ensures there is no partner living with them
replace apartnum = apartnumf if missing(apartnum) & dag < $age_become_responsible & ppno == 0
drop apartnumm apartnumf


*Assign new HH numbers where there is more than 1 couple in the HH:
egen newid = group(swv idhh apartnum)
tostring(newid), replace
replace newid = "999999"+newid if newid != "."
destring(newid), replace
replace idhh = newid if apartnum > 1 & !missing(apartnum)


*If a single has a child ==> it should go with them. 
*If aged above the age to become responsible, and pno > 1 (so more than 2 person in the HH) & ppno == 0 (partner not in the HH) should move out
cap drop newid
egen newid = group(swv idhh pno) if dag >= $age_become_responsible & pno > 1 & ppno == 0
tostring(newid), replace
replace newid = "888888"+newid if newid != "."
destring(newid), replace
replace idhh = newid if !missing(newid)
drop newid


*Still some households with 3 adults? 
bys swv idhh: egen adult_count = count(idperson) if dag > $age_become_responsible
fre adult_count
/*
egen newid = group(idhh pno) if adult_count > 2 & ppno == 0 & dag > $age_become_responsible
tostring(newid), replace
replace newid = "777777"+newid if newid != "."
destring(newid), replace
replace idhh = newid if !missing(newid)
drop newid adult_count
*/


*Check for orphans: 
gen orphan_dummy = 1 if dag < $age_become_responsible & idmother <0 & idfather <0
bys swv idhh: egen orphan_hh = max(orphan_dummy)
tab orphan_dummy 

*Try to assign adult female id as mother, if not available adult male: 
bys swv idhh: gen long idmother2 = idperson if dgn == 0 & dag > 18 //Keep at 18 and not age to become responsible as minimum age to give birth is 18?
gsort +swv +idhh -dag
by swv idhh: carryforward idmother2, replace
replace idmother = idmother2 if dag < $age_become_responsible & idmother<0 & idfather<0 & !missing(idmother2)

bys swv idhh: gen long idfather2 = idperson if dgn == 1 & dag > 18 
gsort +swv +idhh -dag
by swv idhh: carryforward idfather2, replace
replace idfather = idfather2 if dag < $age_become_responsible & idmother<0 & idfather<0 & !missing(idfather2)

/**************************Drop remaining orphans **********************************************/
count if dag < $age_become_responsible & idmother<0 & idfather<0
/*143 cases in total*/
bys swv: count if dag < $age_become_responsible & idmother<0 & idfather<0
drop if dag < $age_become_responsible & idmother<0 & idfather<0
/***********************************************************************************************/


*Check for same-sex couples
preserve
keep idhh ppno dgn
rename dgn dgn_partner
rename ppno pno 
drop if pno == 0
save "$dir_data/temp_sex", replace
restore

merge 1:1 idhh pno using "$dir_data/temp_sex"
keep if _merge == 1 | _merge == 3
drop _merge
gen same_sex_couple = 1 if dgn == dgn_partner & !missing(dgn) & !missing(dgn_partner)


*Check same-sex couples for children: father/mother should stay with children 
*Double-check as might not work properly 
bys swv idhh: egen samesex_hh = max(same_sex_couple) 
tab samesex_hh if dag < $age_become_responsible //HH where same-sex couple is with someone < 18, N=19 


gen long parent_id_temp = idmother if samesex_hh == 1 & dag < 18
replace parent_id_temp = idfather if parent_id_temp == -9 & samesex_hh == 1
by swv idhh: egen long max_parent_id = max(parent_id_temp)
replace parent_id_temp = idperson if missing(parent_id_temp) & samesex_hh == 1 & !missing(max_parent_id)
drop max_parent_id
egen ss_parent = group(idhh parent_id_temp)


replace idmother = . if idmother < 0
replace idfather = . if idfather < 0  

*Break-up same-sex couples into separate households:
gen long idmother3 = idmother if dag < $age_become_responsible
replace idmother3 = idperson if missing(idmother3) & dgn == 0 
gen long idfather3 = idfather if dag < $age_become_responsible
replace idfather3 = idperson if missing(idfather3) & dgn == 1

sort swv idhh pno 
bys swv idhh: gen dgn_hh = dgn if pno == 1 & samesex_hh == 1
by swv idhh: carryforward dgn_hh, replace


//bys idhh same_sex_couple: replace same_sex_couple = . if _n != 1 //Assign new ID to one of the couple
egen newid2 = group(swv idhh idmother3) if samesex_hh == 1 & dgn_hh == 0
egen newid3 = group(swv idhh idfather3) if samesex_hh == 1 & dgn_hh == 1
replace newid3 = newid3 + 5000
replace newid2 = newid3 if missing(newid2)
egen newid = group(swv idhh newid2) if samesex_hh == 1

replace idpartner = 0 if !missing(newid) == 1 //We don't allow same-sex partnerships in the simulation
tostring(newid), replace
replace newid = "666666"+newid if newid != "."
destring(newid), replace
replace idhh = newid if !missing(newid)
drop newid same_sex_couple dgn_partner


/************Drop same sex households (still do the split above in case we wanted to revert)*****************/
count if samesex_hh==1 
bys swv: fre samesex_hh
/* 2,855 hhds in total, aprox 230 -250 in each wave */
drop if samesex_hh == 1
/*************************************************************************************************************/


* Clean up
*Set idpartner = 0 if single HH:
bys swv idhh: egen count = count(idperson) if dag >= $age_become_responsible | (dag < $age_become_responsible & ppno != 0)
fre count
replace idpartner = 0 if count == 1


*"Home" variable
/*We decided to distinguish between a household (fiscal unit) and a "home". 
For example children living with their parents will share the same "home". 
Create a new variable that contains the original household id for adult children 
1-For those who are not adult children, home id should be the same as idhh 
2-For those who are adult children, home id should be the idhh before the split
(But: in the data there can be multigenerational families etc. that should still be split (?)
So the home variable can only be defined after household splitting
home id == idhh for everyone (the one we modified), but for households with adult children home id == hidp
*/
gen double idhome = idhh
format idhome %15.0g
replace idhome = idhhmother if adultChildFlag == 1 & !missing(idhhmother)
replace idhome = idhhfather if adultChildFlag == 1 & missing(idhhmother) & !missing(idhhfather)
*/

/***************************************************************************************************************************/
*DP: script from "UK Compile do-file" - a more recent version of a split 
/***************************************************************************************************************************/
* recode same sex couples as singles
replace idpartner = -9 if (ssscp==1)
replace dcpst = 2 if (ssscp==1)
foreach vv in dgnsp dagsp dehsp_c3 dhesp lessp_c3 lessp_c4 {
	replace `vv' = -9 if (ssscp==1)
}

* adult defined as 18 or over, or if married
cap gen adult = (dag>=$age_become_responsible) 
replace adult = 1 if (adult==0 & dcpst==1)
cap gen child = 1 - adult

* define benefit units
cap gen long idbenefitunit = .
cap gen long idbupartner = .

order swv idhh idbenefitunit idbupartner idperson idpartner idmother idfather dag adult child
gsort swv idhh -dag
bys swv idhh: replace idbenefitunit = idperson[1]
bys swv idhh: replace idbupartner = idpartner[1]

replace idbupartner = .   if (adult==1 & idperson!=idbenefitunit & idpartner!=idbenefitunit)
replace idbenefitunit = . if (adult==1 & idperson!=idbenefitunit & idpartner!=idbenefitunit)
replace idbupartner = .   if (child==1 & idfather!=idbenefitunit & idmother!=idbenefitunit & idfather!=idbupartner & idmother!=idbupartner)
replace idbenefitunit = . if (child==1 & idfather!=idbenefitunit & idmother!=idbenefitunit & idfather!=idbupartner & idmother!=idbupartner)
replace idbenefitunit = idperson if (missing(idbenefitunit) & adult==1 & (missing(idpartner) | idpartner<0))
bys swv idhh: replace idbenefitunit = idperson if (missing(idbenefitunit) & adult==1 & !missing(idbenefitunit[_n-1]) & idpartner!=idbenefitunit[_n-1])
replace idbupartner = idpartner if (missing(idbupartner) & idbenefitunit==idperson & !missing(idpartner) & idpartner>0)
bys swv idhh: replace idbenefitunit = idpartner if (missing(idbenefitunit) & adult==1 & !missing(idbenefitunit[_n-1]) & idpartner==idbenefitunit[_n-1])
replace idbupartner = idperson if (missing(idbupartner) & idbenefitunit==idpartner)
replace idbenefitunit = idmother if (missing(idbenefitunit) & child==1 & idmother==idbenefitunit[_n-1])
replace idbenefitunit = idfather if (missing(idbenefitunit) & child==1 & idfather==idbenefitunit[_n-1])

drop if idbenefitunit == . 	// 4403 observations deleted
drop if idbenefitunit<0 	// 0 observations deleted
drop idbupartner

// screen out benefit units with multiple adults of same sex
gen adultMan = adult * (dgn==1)
gen adultWoman = adult * (dgn==0)
gsort swv idbenefitunit
bys swv idbenefitunit: egen sumMen = sum(adultMan)
bys swv idbenefitunit: egen sumWomen = sum(adultWoman)
tab swv sumMen
tab swv sumWomen
drop if (sumWomen>1)	// 1638 obserations
drop if (sumMen>1)		// 14 observations

// adjust bu identifiers to allow for possiblity that units are sampled in same calendar year
order idbenefitunit stm swv
gsort idbenefitunit stm swv
gen idtemp = 1
gen switch = 0
replace switch = 1 if (idbenefitunit!=idbenefitunit[_n-1] | swv!=swv[_n-1])
order idbenefitunit stm swv idtemp switch
replace idtemp = idtemp[_n-1] + switch if (_n>1)

drop idbenefitunit switch
rename idtemp idbenefitunit

sum idbenefitunit, d

gsort idbenefitunit -dag
by idbenefitunit: replace idhh = idhh[1] if (idhh != idhh[1])

order idbenefitunit, after(idhh)
sort idhh idbenefitunit


***************************************************
*check for duplicates in terms of stm amd idperson*
***************************************************	
duplicates report swv idperson //no such cases//

duplicates report stm idperson //16420 cases have duplicates//
duplicates report stm idhh idperson //16420 cases have duplicates, they also have the same hh ids //
	
cap drop duplicate 
duplicates tag stm idperson , generate(duplicate)
fre duplicate 

order stm idperson idhh idbenefitunit Int_Date, last /*duplicates appear due to the same persons interviewed twice 
during the same calendar year, typically in Jan-Feb and then in Nov-Dec, so assume that the first interview is a catching up interview from previous wave
and keep only the second one . the alternative is to change the stm for the first interview to stm-1*/

sort duplicate stm idperson Int_Date 
cap drop keep 
by duplicate stm idperson: gen todrop = (_n>1) //16,423 obs will be dropped 	

*drop duplicate observations 
drop if todrop==1 

duplicates report stm idperson 
duplicates report stm idhh idperson 

	
/********************************Drop households with missing values***********/
cap gen dropObs = . //Generate variable indicating whether household should be dropped

count if idbenefitunit == .
*bys swv: count if idbenefitunit == .
* 1,964 observations missing idbenefitunit identifier - mostly children living with grandparents
* 100-150 per wave 
* drop these from sample, as beyond the modelling scope
replace dropObs = 1 if (idbenefitunit == .)


*4,250 obs (approx 300-400 by wave) have zero adults - these are children who were interviewed in a year other than that of their parents
cap drop adult_count
bys idhh stm: egen adult_count = sum(adult)
count if adult_count==0 
*bys swv: count if adult_count==0 
replace dropObs = 1 if (adult_count==0)


*Remove household if missing values present: (not using previous wave's values as migration possible) (353  obs)
count if drgn1 == -9 
replace dropObs = 1 if drgn1 == -9


*Missing age (140 obs):
count if dag == -9
replace dropObs = 1 if dag == -9

*Missing age of partner (but has a partner, 46 cases):
count if dagsp == -9 & idpartner != -9
replace dropObs = 1 if dagsp == -9 & idpartner != -9


*Health status - remove household if missing for adults - 0 cases due to imputation  
count if (dhe == -9 ) & dag > $age_become_responsible 
count if (dhe == -9 ) & dag>0 & dag<= $age_become_responsible 
/*no missing cases due to imputations */
replace dropObs = 1 if (dhe == -9) & dag > $age_become_responsible

*Mental health status (1 obs):
count if dhm == -9 & dag > $age_become_responsible
count if dhm_ghq == -9 & dag > $age_become_responsible
/*no missing cases due to imputations */
replace dropObs = 1 if dhm == -9 & dag > $age_become_responsible
replace dropObs = 1 if dhm_ghq == -9 & dag > $age_become_responsible 

*Health status of spouse - remove household if missing but individual has a spouse (46 obs)
count if dhesp == -9 & idpartner != -9 
/*no missing cases due to imputations */
replace dropObs = 1 if (dhesp == -9) & idpartner != -9

*Education - remove household if missing education level for adults who are not in education (1,918 cases):
count if deh_c3 == -9 & dag >= $age_become_responsible & ded == 0
replace dropObs = 1 if deh_c3 == -9 & dag >= $age_become_responsible & ded == 0

*Education of spouse - remove household if missing but individual has a spouse (14,720 obs)
count if dehsp_c3 == -9 & idpartner != -9
replace dropObs = 1 if dehsp_c3 == -9 & idpartner != -9

*Parental education - 0 obs removed due to imputation
count if  dehmf_c3 == -9 
replace dropObs = 1 if dehmf_c3 == -9 

*Partnership status (808 obs):
count if dcpst == -9 
replace dropObs = 1 if dcpst == -9 

*Activity status (392 cases):
count if les_c3 == -9 & dag >= $age_become_responsible
replace dropObs = 1 if les_c3 == -9 & dag >= $age_become_responsible

*Activity status with retirement as a separate category (392 cases)
count if les_c4 == -9 & dag >= $age_become_responsible
replace dropObs = 1 if les_c4 == -9 & dag >= $age_become_responsible

*Partner's activity status (30,481 cases) 
count if lessp_c3 == -9 & idpartner != -9
replace dropObs = 1 if lessp_c3 == -9 & idpartner != -9

*Own and spousal activity status (30,614) 
count if lesdf_c4 == -9 & idpartner != -9
replace dropObs = 1 if lesdf_c4 == -9 & idpartner != -9

*Household composition (808 cases):
count if dhhtp_c4 == -9
replace dropObs = 1 if dhhtp_c4 == -9

*Income (14 cases):
count if ypnbihs_dv == -9 & dag >= $age_become_responsible //530 obs 
count if yplgrs_dv == -9 & dag >= $age_become_responsible //704 obs 
count if ydses_c5 == -9 //286 obs 
count if ypncp == -9 & dag >= $age_become_responsible //0 obs 

replace dropObs = 1 if ypnbihs_dv == -9 & dag >= $age_become_responsible
replace dropObs = 1 if yplgrs_dv == -9 & dag >= $age_become_responsible
replace dropObs = 1 if ydses_c5 == -9
replace dropObs = 1 if ypncp == -9 & dag >= $age_become_responsible
	
*Indicator for households with missing values 
cap drop dropHH
bys stm idhh: egen dropHH = max(dropObs)
bys stm: tab dropHH, mis
drop if stm<0
save "$dir_data\ukhls_pooled_all_obs_05.dta", replace  
