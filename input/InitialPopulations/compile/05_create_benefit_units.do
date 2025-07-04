***************************************************************************************
* PROJECT:              ESPON: construct initial populations for SimPaths using UKHLS data 
* DO-FILE NAME:         05_create_benefit_units.do
* DESCRIPTION:          Screens data, identifies benefit units and households that are to be dropped 
***************************************************************************************
* COUNTRY:              UK
* DATA:         	    UKHLS EUL version - UKDA-6614-stata [to wave n]
* AUTHORS: 				Daria Popova, Justin van de Ven
* LAST UPDATE:          15 Jan 2025 DP 
* NOTE:					Called from 00_master.do - see master file for further details
***************************************************************************************


********************************************************************************
cap log close 
log using "${dir_log}/05_drop_hholds_create_benefit_units.log", replace
********************************************************************************

use "$dir_data\UKHLS_pooled_all_obs_04.dta", clear 
/*******************************************************************************/
fre ivfio
keep if ivfio == 1 | ivfio == 2 | ivfio == 21 | ivfio == 24 
fre ivfio

/******************************Split households********************************/
*DP: This procedure is revised following the approach taken for the EU-SILC based models  
/**********************Rules and assumptions***********************************
1. Each HH can contain: Responsible Male, and/or Responsible Female, Children, Other members.
In the simulation everyone starts as "Other member" and is assigned one of the roles in the HH.
Note: same sex couples are recoded and treated as singles. 

	1.1. Responsible male and female create a partnership couple leading the HH. Any additional couple 
		 creates new HH. A couple with / composed of people under the age to leave home (18)
		 will still live together and set up a new HH. 
		 
		 1.1.1. Children should follow the mother if she's moving to a new HH. 
		 
	1.2. After the above there should be only singles left in addition to the leading couple.
		 If they are above 18, they will leave and set up their own HH. 
	1.3. After the above there should only be children left in addition to the original HH.
		 Children will live with mother if defined in the data, otherwise with father. If neither
		 exists, they will be considered as orphans. 
	1.4. Orphans are assigned to an adult closest to their age + 20 years.
*/

* recode same sex couples as singles
replace idpartner = -9 if (ssscp==1)
replace dcpst = 2 if (ssscp==1)
foreach vv in dgnsp dagsp dehsp_c3 dhesp lessp_c3 lessp_c4 {
	replace `vv' = -9 if (ssscp==1)
}
replace ssscp = 0 if idpartner==-9   
//fre ssscp

* adult is defined as 18 or over, or if married  
cap gen adult = (dag>=$age_become_responsible) 
replace adult = 1 if (adult==0 & dcpst==1)
cap gen child = 1 - adult

*count number of dep children of each person 
	//for mother 
	count 
	preserve
	sort swv idhh idperson
	save "$dir_data/motherinfo.dta", replace

	keep swv idhh idmother child
	rename idmother idperson
	bysort swv idperson: egen int n_child_mother = total(child) //number of dependent children who have this idmother
	
	duplicates drop swv idperson, force
	drop child 
	save "$dir_data/motherinfo.dta", replace
	restore 
	
	sort swv idhh idperson
	merge m:1 swv idhh idperson using "$dir_data/motherinfo.dta"
	fre _merge 
	drop if _merge==2 
	drop _merge
	count 
	recode n_child_mother (. = 0)
    
	//for father 
	count 
	preserve
	sort swv idhh idperson
	save "$dir_data/fatherinfo.dta", replace

	keep swv idhh idfather child  
	rename idfather idperson
	bysort swv idperson: egen int n_child_father = total(child) //number of dependent children who have this idfather
		
	duplicates drop swv idperson, force
	drop child 
	save "$dir_data/fatherinfo.dta", replace
	restore 
	
	sort swv idhh idperson
	merge m:1 swv idhh idperson using "$dir_data/fatherinfo.dta"
	fre _merge 
	drop if _merge==2 
	drop _merge
	count 
	recode n_child_father (. = 0)
	
	gen n_child = n_child_mother+n_child_father //n of kids this individual has ==> no double count because father's kids will be in their line while mothers kids will be in their line 
	
	sum n_child_mother if n_child_mother>0 
	sum n_child_father if n_child_father>0 
	sum n_child if n_child>0 
	

count if child==1 & n_child>0 //56 obs who are kids but have their own kids 

/*replace child=0 if n_child>0    
gen adult=1-child 
sum child adult 
*/

//check if there are hhlds without adult? 
assert child==1-adult 
cap drop num_adults  
bys swv idhh: egen num_adults = total(adult)
fre num_adults 

fre idhh if num_adults ==0 //1838 obs 


************************
* define benefit units *
************************
cap gen long idbenefitunit = .
cap gen long idbupartner = .
format idbenefitunit %19.0g
format idbupartner %19.0g

//define first benefit unit 
gen partnered = (idpartner>0) 
order swv idhh idbenefitunit idbupartner idperson idpartner idmother idfather dag n_child, last 
gsort swv idhh -partnered -dag //sort hh members in descending order by partnership status and then age (this ensures that partnered adults go first)
bys swv idhh: replace idbenefitunit = idperson[1] //oldest person becomes head of first benefit unit 
bys swv idhh: replace idbupartner = idpartner[1] //partner of oldest person becomes first benefit unit partner 
replace idbupartner =. if idbupartner==-9


//remove those who do not belong to first benefit unit  
replace idbupartner = .   if (adult==1 & idperson!=idbenefitunit & idpartner!=idbenefitunit) //remove partner id for those who are not head or partner 
replace idbenefitunit = . if (adult==1 & idperson!=idbenefitunit & idpartner!=idbenefitunit) //remove other adults who are not head or partner 
replace idbupartner = .   if (child==1 & idfather!=idbenefitunit & idmother!=idbenefitunit & ((idfather!=idbupartner & idmother!=idbupartner) | idbupartner<0)) //remove partner id for kids that are not head's or partner's
replace idbenefitunit = . if (child==1 & idfather!=idbenefitunit & idmother!=idbenefitunit & ((idfather!=idbupartner & idmother!=idbupartner) & idbupartner>0)) //remove kids that are not head's or partner's

//create new benunit for single adults who are not yet assigned to first benunit 
replace idbenefitunit = idperson if (missing(idbenefitunit) & adult==1 & (missing(idpartner) | idpartner<0)) 

//create second benunit for adults who are partnered  
bys swv idhh: replace idbenefitunit = idperson if (missing(idbenefitunit) & adult==1 & !missing(idbenefitunit[_n-1]) & idpartner!=idbenefitunit[_n-1]) 
replace idbupartner = idpartner if (missing(idbupartner) & idbenefitunit==idperson & !missing(idpartner) & idpartner>0) 
//for partners of head of second benefit unit - add head's id as their benefit unit id 
bys swv idhh: replace idbenefitunit = idpartner if idpartner>0 & (missing(idbenefitunit) & adult==1 & !missing(idbenefitunit[_n-1]) & idpartner==idbenefitunit[_n-1]) //same as above    
replace idbupartner = idperson if (missing(idbupartner) & idbenefitunit==idpartner) 

//create third benunit for adults who are partnered  
count if adult==1 & idbenefitunit==. //138 partnered adults still have no benefit unit ==> these are third couples in multigen hholds
bys swv idhh: replace idbenefitunit = idperson if (missing(idbenefitunit) & adult==1 & !missing(idbenefitunit[_n-1]) & idpartner!=idbenefitunit[_n-1]) 
replace idbupartner = idpartner if (missing(idbupartner) & idbenefitunit==idperson & !missing(idpartner) & idpartner>0) 
//for partners of head of third benefit unit - add head's id as their benefit unit id 
bys swv idhh: replace idbenefitunit = idpartner if idpartner>0 & (missing(idbenefitunit) & adult==1 & !missing(idbenefitunit[_n-1]) & idpartner==idbenefitunit[_n-1]) //same as above    
replace idbupartner = idperson if (missing(idbupartner) & idbenefitunit==idpartner) 

//create fourth benunit for adults who are partnered  
count if adult==1 & idbenefitunit==. //1 ==> these are fourth couple in a multigen hhold but it looks like partner is not in the hh
bys swv idhh: replace idbenefitunit = idperson if (missing(idbenefitunit) & adult==1 & !missing(idbenefitunit[_n-1]) & idpartner!=idbenefitunit[_n-1]) 
replace idbupartner = idpartner if (missing(idbupartner) & idbenefitunit==idperson & !missing(idpartner) & idpartner>0) 
//for partners of head of fourth benefit unit - add head's id as their benefit unit id 
bys swv idhh: replace idbenefitunit = idpartner if idpartner>0 & (missing(idbenefitunit) & adult==1 & !missing(idbenefitunit[_n-1]) & idpartner==idbenefitunit[_n-1]) //same as above    
replace idbupartner = idperson if (missing(idbupartner) & idbenefitunit==idpartner) 

recode idbupartner (.=-9) 

//check if all adults are assigned to benunits 
count  if  adult==1 & idbenefitunit==. 
assert idbenefitunit !=. if adult==1 

//assign children to their mothers' benunits (where they are heads or partners) 
forvalues i=1/13 {
replace idbenefitunit = idbenefitunit[_n-`i'] if idmother>0 & missing(idbenefitunit) & child==1 & (idmother==idbenefitunit[_n-`i'] | idmother==idbupartner[_n-`i'])  
} 
//if some kids are still not assinged - asign them to father's benunits 
forvalues i=1/13 {
replace idbenefitunit = idbenefitunit[_n-`i'] if idfather>0 & missing(idbenefitunit) & child==1 & (idfather==idbenefitunit[_n-`i'] | idfather==idbupartner[_n-`i'])   
} 

//check if all kids are assigned 
count if child==1 & idbenefitunit==. //5,099 kids are still not asigned 

//////////////////////
//deal with orphans //
//////////////////////
*bys swv idhh: replace idbenefitunit = idbenefitunit[1] if missing(idbenefitunit) & orphan ==1  //assign orphans to the first benunit ==> we rejected this  
cap gen orphan = (idfather<0 & idmother<0 & child==1)
fre orphan if idbenefitunit==. //2,168 are orphans   

cap drop n_orphan
bys stm idhh: egen n_orphan = sum(orphan)  //count N of orphans per household ==> can be up to 7!
fre n_orphan	       

order stm idhh idperson idpartner idfather idmother dag dgn adult orphan n_orphan, last 

//create variables storing orphans ages for all orphans 
preserve 
keep if n_orphan>0 
keep stm idhh idperson idpartner idfather idmother dag dgn adult orphan n_orphan
keep if orphan==1
bys stm idhh: gen orphan_number = _n if orphan==1  
forvalues i = 1/7 {  // Loop over each possible orphan and create corresponding age variables
  bys stm idhh: egen temp_dag_orphan`i' = sum(dag) if orphan_number == `i'  
  bys stm idhh: egen dag_orphan`i' = sum(temp_dag_orphan`i')
  drop temp_dag_orphan`i'
}
save "$dir_data/orphans.dta", replace 
restore 

count 
// add info on orphan's age to the main dataset 
merge 1:1 stm idhh idperson using "$dir_data/orphans.dta",	keepusing(dag_orphan* orphan_number)
keep if _merge==1 | _merge ==3 
drop _merge 
count 

//loop over each orphan 
forvalues i=1/7 {
//create age difference between them and each adult in the hh 
gen temp_target_age`i'  = dag_orphan`i'+20 if dag_orphan`i'>0
bys stm idhh: egen target_age`i'  = mean(temp_target_age`i')
gen agediff`i' = abs(dag -target_age`i') if adult==1
//select new parent for each orphan who's age is closest to target age 
sort stm idhh agediff`i' idperson 
by stm idhh: gen newparent`i' = _n 
by stm idhh: replace newparent`i'=0 if _n >1 
replace newparent`i'=. if n_orphan==0

drop dag_orphan`i' temp_target_age`i' target_age`i' agediff`i'

//now assign this parent's idperson as orphan's idmother or idfather 
cap drop temp_idmother_orphan`i' 
gen double temp_idmother_orphan`i' = idperson if newparent`i'==1 & dgn==0
bys stm idhh: egen idmother_orphan`i' = max(temp_idmother_orphan`i')
format idmother_orphan`i' %19.0g
replace idmother_orphan`i'=0 if orphan==0 
drop temp_idmother_orphan`i'

cap drop temp_idfather_orphan`i'
gen double temp_idfather_orphan`i' = idperson if newparent`i'==1 & dgn==1
bys stm idhh: egen idfather_orphan`i' = max(temp_idfather_orphan`i')
format idfather_orphan`i' %19.0g
replace idfather_orphan`i'=0 if orphan==0 
drop temp_idfather_orphan`i' 
}

//finally create newidmother for orphans  
cap gen newidmother=.
cap gen newidfather=.
forvalues i=1/7 {
replace newidmother = idmother_orphan`i' if orphan_number==`i'
replace newidfather = idfather_orphan`i' if orphan_number==`i'
}
format newidmother %19.0g
format newidfather %19.0g

//replace idmother/idfather of former orphans 
replace idmother = newidmother if orphan==1 
replace idfather = newidfather if orphan==1 

drop newparent* idmother_orphan* idfather_orphan*

//assign orphans to their new mothers' benunits (where they are heads or partners) 
gsort swv idhh -dag -partnered
forvalues i=1/13 {
replace idbenefitunit = idbenefitunit[_n-`i'] if idmother>0 & missing(idbenefitunit) & orphan==1 & (idmother==idbenefitunit[_n-`i'] | idmother==idbupartner[_n-`i'])  
} 
//if some orphans are still not assinged - asign them to father's benunits 
forvalues i=1/13 {
replace idbenefitunit = idbenefitunit[_n-`i'] if idfather>0 & missing(idbenefitunit) & orphan==1 & (idfather==idbenefitunit[_n-`i'] | idfather==idbupartner[_n-`i'])   
} 
bys stm idhh idbenefitunit: egen temp_idbupartner = max(idbupartner) 
fre temp_idbupartner if orphan==1 
replace idbupartner = temp_idbupartner if orphan==1 //fill in benefit unit partner id 
//assign them a second parent if first parent partnered
replace idfather=idbenefitunit if idmother==idbupartner & orphan==1 
replace idfather=idbupartner if idmother==idbenefitunit & orphan==1 

//check if all kilds are assigned 
count if child==1 & idbenefitunit==. //3,091 obs 

//check data after orphans are assigned   
count if idbenefitunit == . 
fre adult child orphan dag if  idbenefitunit == . //benunits are missing only for kids out of which 164 are orphans ==> these are hholds without adults , teenage parents, etc
fre idperson if orphan==1 & missing(idbenefitunit)   

//assign orphaned kids living in hhlds w/t adults to the same beunit having the oldest one as head  
bys swv idhh: replace idbenefitunit=idperson[1] if orphan==1 & missing(idbenefitunit)  

/*recode the first child in benunit as adult? 
bys swv idhh: replace child=0 if child==1 & idperson==idperson[1] & orphan==1 & num_adults==0  
bys swv idhh: replace adult=1 if idperson==idperson[1] & orphan==1 & num_adults==0 
*/

//check if everyone is assigned to benunits at this point  
count if idbenefitunit == .  //2,931 obs 
count if child==1 & idbenefitunit==. //2,931 obs  
count if adult==1 & idbenefitunit==. //0 obs

 
//check that everyone in benuint has the same benunit partner id assigned   
assert idbupartner !=. 
bys swv idbenefitunit: replace idbupartner=idbupartner[1] if idbupartner!=idbupartner[1] 
assert idbupartner!=idbenefitunit

// screen out benefit units with multiple adults of same sex
gen adultMan = adult * (dgn==1)
gen adultWoman = adult * (dgn==0)
gsort swv idbenefitunit
bys swv idbenefitunit: egen sumMen = sum(adultMan)
bys swv idbenefitunit: egen sumWomen = sum(adultWoman)
tab swv sumMen
tab swv sumWomen
assert sumMen<2 & sumWomen<2 //no such cases 

//adjust bu identifiers to allow for possiblity that units are sampled in same calendar year
order idbenefitunit stm swv
gsort idbenefitunit stm swv
gen idtemp = 1
gen switch = 0
replace switch = 1 if (idbenefitunit!=idbenefitunit[_n-1] | swv!=swv[_n-1])
order idhh idperson idbenefitunit stm swv idtemp switch
replace idtemp = idtemp[_n-1] + switch if (_n>1)

drop idbenefitunit switch
rename idtemp idbenefitunit

sum idbenefitunit, d

gsort idbenefitunit -dag
by idbenefitunit: replace idhh = idhh[1] if (idhh != idhh[1])

order idbenefitunit, after(idhh)
sort idhh idbenefitunit

//check if someone still not assigned 
assert idbenefitunit !=. 


//check for duplicates in terms of stm and idperson
duplicates report swv idperson //no such cases//
duplicates report stm idperson //16869 cases have duplicates//
duplicates report stm idhh idperson //16869 cases have duplicates, they also have the same hh ids //
	
cap drop duplicate 
duplicates tag stm idperson , generate(duplicate)
fre duplicate 

order stm idperson idhh idbenefitunit Int_Date, last /*duplicates appear due to the same persons interviewed twice 
during the same calendar year, typically in Jan-Feb and then in Nov-Dec, so assume that the first interview is a catching up interview from previous wave
and keep only the second one . the alternative is to change the stm for the first interview to stm-1*/

sort duplicate stm idperson Int_Date 
cap drop keep 
by duplicate stm idperson: gen todrop = (_n>1) //16869 obs will be dropped 	

*drop duplicate observations 
drop if todrop==1 

duplicates report stm idperson 
duplicates report stm idhh idperson 


**************************************
*Identify hholds with missing values *
**************************************
cap gen dropObs = . //Generate variable indicating whether household should be dropped

*Identify remaining orphans   
gen orphan_check = 1 if (idfather<0 & idmother<0) & (dag>0 & dag<$age_become_responsible ) //57 obs 
fre dag if orphan_check == 1 
fre n_child if orphan_check == 1
replace dropObs = 1 if orphan_check ==1 //these are kids who have partners or their own kids 

*Identify single adults saying they are partnered & coupled adults saying they are not partnered 
/*dcpst	-- Partnership	status
1 partnered	
2 single		
3 previously	
*/
//single adult with nonmissing idpartner 
bys stm idbenefitunit : egen na = sum(adult)
gen chk = (na==1 & dcpst==1 & adult==1) 
bys stm idbenefitunit : egen chk2 = max(chk)
fre chk2 // 58,098 obs => this is due to some partners not having full interviews   
replace dropObs = 1 if chk2 == 1
//two adults in benunit but not partnered 
gen chk3 = (na==2 & dcpst!=1 & adult==1) 
bys stm idbenefitunit : egen chk4 = max(chk3)
fre chk4 //0 obs 
replace dropObs = 1 if chk4 == 1
drop na chk chk2 chk3 chk4 


*Missing region (353  obs)
count if drgn1 == -9 
replace dropObs = 1 if drgn1 == -9

*Missing age (197 obs):
count if dag == -9
replace dropObs = 1 if dag == -9

*Missing age of partner (but has a partner, 71 cases):
count if dagsp == -9 & idpartner != -9
replace dropObs = 1 if dagsp == -9 & idpartner != -9

*Health status - remove household if missing for adults - 0 cases due to imputation  
count if (dhe == -9 ) & dag > $age_become_responsible 
count if (dhe == -9 ) & dag>0 & dag<= $age_become_responsible 
/*no missing cases due to imputations */
replace dropObs = 1 if (dhe == -9) & dag > $age_become_responsible

*Mental health status - 0 cases due to imputation
count if dhm == -9 & dag > $age_become_responsible
count if dhm_ghq == -9 & dag > $age_become_responsible
/*no missing cases due to imputations */
replace dropObs = 1 if dhm == -9 & dag > $age_become_responsible
replace dropObs = 1 if dhm_ghq == -9 & dag > $age_become_responsible 

*Health status of spouse - remove household if missing but individual has a spouse (4 obs)
count if dhesp == -9 & idpartner != -9 
/*no missing cases due to imputations */
replace dropObs = 1 if (dhesp == -9) & idpartner != -9

*Education - remove household if missing education level for adults who are not in education ( 2,684 cases):
count if deh_c3 == -9 & dag >= $age_become_responsible & ded == 0
replace dropObs = 1 if deh_c3 == -9 & dag >= $age_become_responsible & ded == 0

*Education of spouse - remove household if missing but individual has a spouse (16,792 obs)
count if dehsp_c3 == -9 & idpartner != -9
replace dropObs = 1 if dehsp_c3 == -9 & idpartner != -9

*Parental education - 0 obs removed due to imputation
count if  dehmf_c3 == -9 
replace dropObs = 1 if dehmf_c3 == -9 

*Partnership status (692 obs):
count if dcpst == -9 
replace dropObs = 1 if dcpst == -9 

*Activity status (504 cases):
count if les_c3 == -9 & dag >= $age_become_responsible
replace dropObs = 1 if les_c3 == -9 & dag >= $age_become_responsible

*Activity status with retirement as a separate category (504 cases)
count if les_c4 == -9 & dag >= $age_become_responsible
replace dropObs = 1 if les_c4 == -9 & dag >= $age_become_responsible

*Partner's activity status (33,358 cases) 
count if lessp_c3 == -9 & idpartner != -9
replace dropObs = 1 if lessp_c3 == -9 & idpartner != -9

*Own and spousal activity status (33,530 cases) 
count if lesdf_c4 == -9 & idpartner != -9
replace dropObs = 1 if lesdf_c4 == -9 & idpartner != -9

*Household composition (692 cases):
count if dhhtp_c4 == -9
replace dropObs = 1 if dhhtp_c4 == -9

*Income (14 cases):
count if ypnbihs_dv == -9 & dag >= $age_become_responsible //5 obs 
count if yplgrs_dv == -9 & dag >= $age_become_responsible //79 obs 
count if ydses_c5 == -9 //469 obs 
count if ypncp == -9 & dag >= $age_become_responsible //0 obs 

replace dropObs = 1 if ypnbihs_dv == -9 & dag >= $age_become_responsible
replace dropObs = 1 if yplgrs_dv == -9 & dag >= $age_become_responsible
replace dropObs = 1 if ydses_c5 == -9
replace dropObs = 1 if ypncp == -9 & dag >= $age_become_responsible
	
*Indicator for households with missing values 
cap drop dropHH
bys stm idhh: egen dropHH = max(dropObs)
tab dropHH, mis 
/*	tab dropHH,	mis 

	dropHH	Freq.	Percent	Cum.
				
	1	74,706	11.20	11.20
	.	592,608	88.80	100.00
				
	Total	667,314	100.00
*/
drop if stm<0 

save "$dir_data\ukhls_pooled_all_obs_05.dta", replace  

cap log close 
/**************************************************************************************
* clean-up and exit
**************************************************************************************/
#delimit ;
local files_to_drop 
	fatherinfo.dta
	motherinfo.dta
	orphans.dta
	;
#delimit cr // cr stands for carriage return

foreach file of local files_to_drop { 
	erase "$dir_data/`file'"
}
