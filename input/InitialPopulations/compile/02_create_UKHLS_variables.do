***************************************************************************************
* PROJECT:              SimPaths UK: construct initial populations for SimPaths using UKHLS data 
* DO-FILE NAME:         02_create_UKHLS_variables.do
* DESCRIPTION:          This file creates initial population variables in the UKHLS pooled sample
***************************************************************************************
* COUNTRY:              UK
* DATA:         	    UKHLS EUL version - UKDA-6614-stata [to wave n]
* AUTHORS: 				Daria Popova, Justin van de Ven
* LAST UPDATE:          15 Jan 2026 DP
* NOTES:				Called from 00_master.do - see master file for further details
*						Use -9 for missing values 
*
*        				This do-file creates the main varaibles used in SimPaths from UKHLS data
*                       Now we also impose consistency with 
* 						simulation assumptions as noted in the master file. 
*****************************************************************************************/

*****************************************************************************************************
cap log close 
log using "${dir_log}/02_create_UKHLS_variables.log", replace
*****************************************************************************************************

use "$dir_data\ukhls_pooled_all_obs_01.dta", clear
lab define dummy 1 "yes" 0 "no"

set seed 12345

/**************************************************************************************
* SAMPLE
*************************************************************************************/

***Drop IEMB: 
fre hhorig
/*hhorig	-- Sample origin, household
1  ukhls gb 2009-10	
2  ukhls ni 2009-10	
3  bhps gb 1991	
4  bhps sco 1999	
5  bhps wal 1999	
6  bhps ni 2001	
7  ukhls emboost 2009-10
8  ukhls iemb 2014-15
21 ukhls gps2 gb 2022-2023
22 ukhls gps2 ni 2022-2023
*/
drop if hhorig == 8

***Keep everyone at this stage as we need info on partners and parents who do not have full/proxi interviews in some waves*/
fre ivfio
/*
ivfio -- Individual level outcome
1  full interview
2  proxy interview
9  lost capi intvw		
10 refusal		
11 other non-intvw		
12 moved		
14 ill/away during survey period				          
15 too infirm/elderly		
16 language difficulties		
18 unknown eligibility		
21 youth interview		
22 youth: refusal		
24 child under 10 
25 youth non-interview	
53 non-cont/non-int hh	
63 chd <16 non-cont/non-int hh	
74 ineligible - aged 10-15	(iemb only)				          
81 prev wave adamant refusal	
84 Ineligible for individual interview as nr in last 3 waves 				          

keep if ivfio == 1 | ivfio == 2 | ivfio == 21 | ivfio == 24 
fre ivfio 
*/


/**************************************************************************************
* CREATE REQUIRED VARIABLES
*************************************************************************************/


/*****************************SYSTEM VARIABLES*********************************/
*swv: Data collection wave (set to match wave defined in macro at the beginning)
//gen swv = $wvno
la var swv "Data collection wave"

*stm: Year
gen stm = intdaty_dv
la var stm "Interview year"
//bysort swv: fre stm 

*interview date 
gen Int_Date = mdy(intdatm_dv, intdatd_dv ,intdaty_dv) 
format Int_Date %d


/**************************** HOUSEHOLD IDENTIFIER*****************************/
clonevar idhh= hidp 
la var idhh "Household identifier"


/********************************* INDIVIDUALS ID*****************************/ 
clonevar idperson=pidp 
la var idperson "Unique cross wave identifier"


*******************************************************************************
xtset idperson swv //Set panel

/********************************** gender*************************************/
gen dgn=sex_dv
la var dgn "Gender" 
recode dgn 2=0 	//dgn = 0 is female, 1 is male
lab define dgn 1 "men" 0 "women"
lab val dgn dgn 

// Impute missing values using lagged/lead values up to 13 previous waves
forvalues i = 1/13 {
    replace dgn = l`i'.dgn if dgn==-9  & (l`i'.dgn !=-9) & idperson==l`i'.idperson
    }
forvalues i = 1/13 {
    replace dgn = f`i'.dgn if dgn==-9 & (f`i'.dgn !=-9) & idperson==f`i'.idperson
    }
//fre dgn


/***************************************ID PARTNER*****************************/ 
clonevar idpartner=ppid
la var idpartner "Unique cross wave identifier of partner"



/**********************ID FATHER (includes natural/step/adoptive)*************/
clonevar idfather= fnspid
la var idfather "Father unique identifier"



/************************ID MOTHER (includes natural/step/adoptive)***********/
clonevar idmother=mnspid 
la var idmother "Mother unique identifier"



/************************ AGE *************************************************/ 
gen dag= age_dv
sort idperson swv 

// Impute missing values using lagged/lead values up to 13 previous waves
forvalues i = 1/13 {
    replace dag = l`i'.dag+`i' if dag==-9 & (l`i'.dag !=-9) & idperson==l`i'.idperson
    }
forvalues i = 1/13 {
    replace dag = f`i'.dag-`i' if dag==-9 & (f`i'.dag !=-9) & idperson==f`i'.idperson
    }
//fre dag
recode dag (-1=-9)

gen dagsq = dag^2
replace dagsq =-9 if dag==-9
la var dagsq "Age squared"


/****************************************Union*********************************/
*Generate union variable to indicate if there is a spouse
*Partnerid are later filled in using ppno information, so dun should distinguish between spouse and just living together
gen dun = 0
replace dun = 1 if sppid > 0
la var dun "=1 if has a spouse"
lab val dun dummy 
//fre dun 

* Check if consistent with simulation assumption form relationship is 16+
tab dag if dun == 1 

gen flag_young_partnership = (dag < ${age_form_partnership} & dun == 1)

replace dun = 0 if dag < ${age_form_partnership}

/*
fre dun 
tab dun stm, col 
bys dun: sum idpartner if idpartner == -9 
bys dun: sum idpartner if idpartner > 0
*/


/************************* region (NUTS 1) ***********************************/ 
//fre gor_dv
gen drgn1=-9
replace drgn1=1 if gor_dv==1 
replace drgn1=2 if gor_dv==2 
replace drgn1=4 if gor_dv==3  // following FRS, code 3 is not used
replace drgn1=5 if gor_dv==4 
replace drgn1=6 if gor_dv==5 
replace drgn1=7 if gor_dv==6 
replace drgn1=8 if gor_dv==7 
replace drgn1=9 if gor_dv==8 
replace drgn1=10 if gor_dv==9 
replace drgn1=11 if gor_dv==10
replace drgn1=12 if gor_dv==11
replace drgn1=13 if gor_dv==12

la var drgn1 "Region"
lab define drgn1 ///
1 "North East" ///
2 "North West" ///
4 "Yorkshire and the Humber" ///
5 "East Midlands" ///
6 "West Midlands" ///
7 "East of England" ///
8 "London" ///
9 "South East" ///
10 "South West" ///
11 "Wales" ///
12 "Scotland" ///
13 "Northern Ireland"
lab values drgn1 drgn1


/***********************country***********************************************/
gen dct=15
la var dct "Country code: UK"



/**********************Partner's gender***************************************/
duplicates report idpartner swv if idpartner >0
preserve
keep swv idperson dgn
rename idperson idpartner
rename dgn dgnsp
save "$dir_data/temp_dgn", replace 
restore

merge m:1 idpartner swv using "$dir_data/temp_dgn" /*m:1 because some people have idpartner=-9*/
la var dgnsp "Partner's gender"
keep if _merge==1 | _merge == 3 //matched N=385,780  (_merge==3)
drop _merge
lab values dgnsp dgn

sort idperson swv 
// Impute missing values using lagged values of dgnsp up to 13 previous waves
forvalues i = 1/13 {
    replace dgnsp = l`i'.dgnsp if dgnsp==-9 & (l`i'.dgnsp!=-9) & idpartner == l`i'.idpartner
    }
forvalues i = 1/13 {
    replace dgnsp = f`i'.dgnsp if dgnsp==-9 & (f`i'.dgnsp!=-9) & idpartner == f`i'.idpartner
    }
//fre dgnsp if idpartner>0 
replace dgnsp=-9 if missing(dgnsp) & idpartner>0



/***************** Partner's age***********************************************/ 
preserve
keep swv idperson dag
rename idperson idpartner 
rename dag dagsp 
save "$dir_data/temp_age", replace
restore

merge m:1 swv idpartner using "$dir_data/temp_age"
la var dagsp "Partner's age"
keep if _merge == 1 | _merge == 3 //matched N=385,780  (_merge==3)
drop _merge

sort idperson swv 
// Impute missing values using lagged values of dagsp  up to 13 previous waves
forvalues i = 1/13 {
    replace dagsp = l`i'.dagsp if dagsp ==-9 & (l`i'.dagsp !=-9) & idpartner == l`i'.idpartner
    }
forvalues i = 1/13 {
    replace dagsp = f`i'.dagsp if dagsp ==-9 & (f`i'.dagsp !=-9) & idpartner == f`i'.idpartner
    }
//fre dagsp if idpartner>0 
replace dagsp=-9 if missing(dagsp)  & idpartner>0



/**********************************Health status*******************************/
*Use scsf1 variable, code negative values to missing, reverse code so 5 = excellent and higher number means better health

*Replace with values of ypsrhlth for youth: note that info on youth health is not available in every wave 
bys swv: fre scsf1
bys swv: fre ypsrhlth
replace scsf1 = ypsrhlth if (scsf1 == . | scsf1 < 0) & (ypsrhlth<. & ypsrhlth>0)

replace scsf1 = . if scsf1 < 0
recode scsf1 (5 = 1 "Poor") ///
	(4 = 2 "Fair") ///
	(3 = 3 "Good") ///
	(2 = 4 "Very good") ///
	(1 = 5 "Excellent") ///
	, into(dhe)
la var dhe "Health status"

*Imputation for all: 
fre dag if missing(dhe)

*ordered probit model to replace linear regression 
recode dgn dag dagsq drgn1 (-9=.) , gen (dgn2 dag2 dagsq2 drgn12)
fre dgn2 dag2 dagsq2 drgn12
xi: oprobit dhe i.dgn2 dag2 dagsq ib8.drgn12 i.swv if dhe < ., vce(robust)
predict pred_probs1 pred_probs2 pred_probs3 pred_probs4 pred_probs5, pr

//Identify the category with the highest predicted probability
egen max_prob = rowmax(pred_probs1 pred_probs2 pred_probs3 pred_probs4 pred_probs5)
//Impute missing values of dhe based on predicted probabilities
gen imp_dhe = .
replace imp_dhe = 1 if max_prob == pred_probs1
replace imp_dhe = 2 if max_prob == pred_probs2
replace imp_dhe = 3 if max_prob == pred_probs3
replace imp_dhe = 4 if max_prob == pred_probs4
replace imp_dhe = 5 if max_prob == pred_probs5

sum imp_dhe if missing(dhe) & dag>0 & dag<18
sum imp_dhe if !missing(dhe) & dag>0 & dag<18
sum imp_dhe if missing(dhe) & dag>=18 
sum imp_dhe if !missing(dhe) & dag>=18

* Add imputation flag 
cap gen flag_dhe_imp = missing(dhe) 
lab var flag_dhe_imp "FLAG: =1 if dhe is imputed"
replace dhe = round(imp_dhe) if missing(dhe) 

bys flag_dhe_imp: fre dhe if dag<=18 
bys flag_dhe_imp: fre dhe if dag>18  

drop dgn2 dag2 dagsq2 drgn12 _Idgn2_1 _Iswv_* pred_probs* max_prob imp_dhe


**************************Partner's health status******************************/
preserve
keep swv idperson dhe flag_dhe_imp
rename idperson idpartner
rename dhe dhesp
rename flag_dhe_imp flag_dhesp_imp
save "$dir_data/temp_dhe", replace
restore

merge m:1 swv idpartner   using "$dir_data/temp_dhe"
la var dhesp "Partner's health status"
keep if _merge == 1 | _merge == 3 
drop _merge

cap lab define dhe 1 "Poor" 2 "Fair" 3 "Good" 4 "Very good" 5 "Excellent"
lab values dhesp dhe 
//fre dhesp if idpartner>0
replace dhesp=-9 if missing(dhesp)  & idpartner>0


/*****************************Subjective well-being ***************************/
/*dhm	scghq1_dv	"DEMOGRAPHIC: Subjective wellbeing (GHQ): Likert Scale from 0 to 36, assumed to be continuous in the simulation."	
This measure converts valid answers to 12 questions of the General Health Questionnaire (GHQ) to a single scale by recoding so that 
the scale for individual variables runs from 0 to 3 instead of 1 to 4, and then summing, 
giving a scale running from 0 (the least distressed) to 36 (the most distressed). 
Values for children (missing in the UKHLS) are imputed using a regression model.*/
gen dhm	= scghq1_dv 
replace dhm = . if scghq1_dv <0
la var dhm "DEMOGRAPHIC: Subjective wellbeing (GHQ)"
fre dhm if dag>0 & dag<=18 
fre dhm if dag>0 & dag<16 

*Imputation for all:  
fre dag if missing(dhm)

preserve
drop if dgn < 0 | dag<0 | dhe<0
eststo predict_dhm: reg dhm c.dag i.dgn i.swv i.dhe, vce(robust) // Physical health has a big impact, so included as covariate.  
restore
estimates restore predict_dhm
predict dhm_prediction
//fre dhm_prediction

gen dhm_flag = missing(dhm)
replace dhm = round(dhm_prediction) if missing(dhm) 
bys dhm_flag : sum dhm 

/**************************Subjective well-being: GHQ 0-12 score *****************************/
/*dhm_ghq   scghq2_dv    "DEMOGRAPHIC: Subjective wellbeing (GHQ): rescaled to 0-12, assumed to be continuous"
This measure converts valid answers to 12 questions of the General Health Questionnaire (GHQ) to a 
single scale by recoding the values of individual variables of 1 and 2 to 0, and values of 3 and 4 to 1 before summing them. 
This produces a scale that ranges from 0 (indicating the least amount of distress) to 12 (indicating the greatest amount of distress).
*/
gen dhm_ghq	= scghq2_dv 
replace dhm_ghq = . if scghq2_dv <0
la var dhm_ghq "DEMOGRAPHIC: Subjective wellbeing (GHQ): 0-12 score"
gen scghq2_dv_miss_flag = (scghq2_dv == .)


preserve
drop if dgn < 0 | dag<0 | dhe<0
eststo predict_dhm_ghq: reg dhm_ghq c.dag i.dgn i.swv i.dhe, vce(robust) // Physical health has a big impact, so included as covariate.  
restore
estimates restore predict_dhm_ghq
predict dhm_ghq_prediction

gen dhm_ghq_flag = missing(dhm_ghq)
replace dhm_ghq = round(dhm_ghq_prediction) if missing(dhm_ghq) 
bys dhm_ghq_flag : sum dhm_ghq



/****************************Self-rated health health - mental and physical component summary scores SF12 ***************************/
/*SF-12 Mental Component Summary (MCS). Continuous scale with a range of 0 (low functioning) to 100 (high functioning)*/
cap gen dhe_mcs = sf12mcs_dv
replace dhe_mcs = . if sf12mcs_dv < 0
lab var dhe_mcs "DEMOGRAPHIC: Subjective Self-rated health - Mental (SF12 MCS)"

*Imputation for all:  
preserve
drop if dgn < 0 | dag<0 | dhe<0
eststo predict_dhe_mcs: reg dhe_mcs c.dag i.dgn i.swv i.dhe, vce(robust) // Physical health has a big impact, so included as covariate.  
restore
estimates restore predict_dhe_mcs
predict dhe_mcs_prediction
//fre dhe_mcs_prediction

gen dhe_mcs_flag = missing(dhe_mcs)
replace dhe_mcs = round(dhe_mcs_prediction) if missing(dhe_mcs) 
bys dhe_mcs_flag : sum dhe_mcs


/*SF-12 Physical Component Summary (PCS). Continuous  scale with a range of 0 (low functioning) to 100 (high functioning)*/    
cap gen dhe_pcs = sf12pcs_dv
replace dhe_pcs = . if sf12pcs_dv < 0
lab var dhe_pcs "DEMOGRAPHIC: Subjective Self-rated health - Physical (SF12 PCS)"

*Imputation for all:  
preserve
drop if dgn < 0 | dag<0 | dhe<0
eststo predict_dhe_pcs: reg dhe_pcs c.dag i.dgn i.swv i.dhe, vce(robust) // Physical health has a big impact, so included as covariate.  
restore
estimates restore predict_dhe_pcs
predict dhe_pcs_prediction
//fre dhe_pcs_prediction

gen dhe_pcs_flag = missing(dhe_pcs)
replace dhe_pcs = round(dhe_pcs_prediction) if missing(dhe_pcs) 
bys dhe_pcs_flag : sum dhe_pcs


/************Partner's Self-rated health health - mental and physical component***************/
preserve
keep swv idperson dhe_mcs dhe_pcs
rename idperson idpartner 
rename dhe_mcs dhe_mcssp 
rename dhe_pcs dhe_pcssp

save "$dir_data/temp_dhe", replace
restore

merge m:1 swv idpartner using "$dir_data/temp_dhe"
la var dhe_mcssp "Partner's Self-rated health health - mental component"
la var dhe_pcssp "Partner's Self-rated health health - physical component"
keep if _merge == 1 | _merge == 3
drop _merge
replace dhe_mcssp=-9 if missing(dhe_mcssp) & idpartner>0
replace dhe_pcssp=-9 if missing(dhe_pcssp) & idpartner>0
//fre dhe_mcssp dhe_pcssp if idpartner>0


/***************************** Life Satisfaction 0-10 ****************************************************/
/* Life satisfaction, self report. Continuous scale.
  UKHLS variable sclfsato records scores 1-7.
  Convert to 0-10 in line with ONS Life Satisfaction scale.
 */


gen dls = sclfsato
replace dls = . if sclfsato < 0
replace dls = (dls-1) * 10/6  // Change to 0-10 scale
lab var dls "DEMOGRAPHIC: Life Satisfaction"
// fre dls if dag>0 & dag<16

preserve
drop if dgn < 0 | dag<0 | dhe<0
eststo predict_dls: reg dls c.dag i.dgn i.swv i.dhe c.dhm c.dhe_mcs, vce(robust) // Physical and mental health have a big impact, so included as covariate.  
restore
estimates restore predict_dls
predict dls_prediction
// fre dls_prediction

gen dls_flag = missing(dls)
replace dls = round(dls_prediction) if missing(dls) 
bys dls_flag : sum dls 


/****************************Ehtnicity*****************************************/
/*Ethnic group derived from multiple sources such as self-reported as an adult, self-reported as a youth, reported by a household member, and ethnic group of biological parents.
ethn_dv	-- Ethnic group (derived from multiple sources)
	-9 missing	
	1  british/english/scottish/welsh/northern irish
	2  irish
	3  gypsy or irish traveller	
	4  any other white background
	5  white and black caribbean	
	6  white and black african	
	7  white and asian	
	8  any other mixed background	
	9  indian		
	10 pakistani	
	11 bangladeshi	
	12 chinese	
	13 any other asian background	
	14 caribbean	
	15 african	
	16 any other black background	
	17 arab	
	97 any other ethnic group  	  
*/		
*ONS style definition (but missing is kept as a separate category)  	
cap gen dot01 = . 
replace dot01 = 1 if ethn_dv>=1 & ethn_dv <=4 //white//
replace dot01 = 2 if ethn_dv>=5 & ethn_dv<=8 //mixed //
replace dot01 = 3 if ethn_dv>=9 & ethn_dv<=13 //asian//
replace dot01 = 4 if ethn_dv>=14 & ethn_dv<=16 //black//
replace dot01 = 5 if ethn_dv==17 | ethn_dv==97 //other, arab//  
replace dot01 = 6 if ethn_dv==-9 //missing// 
lab var dot01 "Ethnicity"
cap label define dot01  1 "White" 2 "Mixed or Multiple ethnic groups" 3 "Asian or Asian British" 4 "Black, Black British, Caribbean, or African" 5 "Other ethnic group" 6 "Missing"
label values dot01 dot01 
//fre dot01 

/************Partner's ethnicity***************/
preserve
keep swv idperson dot01
rename idperson idpartner 
rename dot01 dot01_sp  
save "$dir_data/temp_dot01", replace
restore

merge m:1 swv idpartner using "$dir_data/temp_dot01"
la var dot01_sp "Partner's Ethnicity (6 cat)"
keep if _merge == 1 | _merge == 3
drop _merge
replace dot01_sp=6 if missing(dot01_sp) & idpartner>0
replace dot01_sp=-69 if missing(dot01_sp) 
//fre dot01_sp

//impute missing status of a respondent by spouses status if not missing 
fre dot01_sp if dot01==6
replace dot01=dot01_sp if dot01==6 & (dot01_sp>=1 & dot01_sp<=5) //(9,499 real changes made) out of  21914 = 43% of missing is imputed by partner's ethnicity  


* Ethnicity definition used in regression estimates 
cap gen dot = . 
replace dot = 1 if ethn_dv>=1 & ethn_dv <=4  //white//
replace dot = 2 if ethn_dv>=9 & ethn_dv<=13 //asian//
replace dot = 3 if ethn_dv>=14 & ethn_dv<=16 //black//
replace dot = 4 if ethn_dv==17 | ethn_dv==97 | ethn_dv==-9 | (ethn_dv>=5 & ethn_dv<=8) //arab, mixed, other and missing  
lab var dot "Ethnicity"
cap label define dot 1 "White" 2 "Asian or Asian British" 3 "Black, Black British, Caribbean, or African" 4 "Other or missing ethnic group"
label values dot dot 
//fre dot 

/******************In first education spell*************************************/
/*
replace ded = 1 if jbstat == 7 & (l.jbstat==7 | l.jbstat>=. | l.jbstat<0) 
*/
/*Decision 25/10/2024: We opted to revise this variable to ensure that individuals 
who are observed out of continuous education in one year, aren't recorded as being 
in continuous education in future years. 
But we include current students who were not observed in the previous wave 
if they are aged<=23 because the average age of obtaining a Bachelor's degree (BSc) 
in the UK is typically around 21 to 23 years old.
*/
sort idperson swv 
xtset idperson swv
gen ded = 0 
replace ded = 1 if jbstat==7 & l.jbstat==. & dag <= 23 //currently in education and were not participating in the previous wave and aged<=23 years 
replace ded = 1 if jbstat==7 & l.ded == 1   //currently in education and were in education in the previous wave   
replace ded = 1 if dag < ${age_leave_school}  //Everyone under 16 should be in education - this inludes some obs where jbstat!=7
la val ded dummy
lab var ded "In initial education spell"
* Cannot be in initial education spell above a specific age in simulation
//replace les_c3 = 3 if ded == 1 & dag >= ${age_force_leave_spell1_edu}
//replace les_c4 = 3 if ded == 1 & dag >= ${age_force_leave_spell1_edu}

replace ded = 0 if dag >= ${age_force_leave_spell1_edu}

/*
fre ded //  23.59% 
tab ded swv, col 
tab dag ded, row /*up to age 29 where 1 person is in initial spell*/
bys swv: sum ded
*/

* Ensure don't return to initial education spell once left 
sort idperson swv 

count if ded == 1 & ded[_n-1] == 0 & idperson == idperson[_n-1] 	//  2,049 obs 

* Age in estimation limited to 16-29
//tab dag ded 


/*********************************Activity status*****************************/
recode jbstat (1 2 5 12 13 14 15 = 1 "Employed or self-employed") ///
	(7 = 2 "Student") ///
	(3 6 8 10 11 97 9 4 = 3 "Not employed") /// /*includes apprenticeships, unpaid family business, govt training scheme+retired */
	, into(les_c3)
la var les "Activity status"
replace les_c3 = -9 if les_c3<0

//For people under 16 set activity status to student:
replace les_c3 = 2 if dag <= ${age_leave_school}
//People below age to leave home are not at risk of work so set activity status to not employed if not a student
replace les_c3 = 3 if dag < ${age_becomes_responsible} & les_c3 != 2
//fre les_c3
replace les_c3 = -9 if les_c3 == . //(69,043 real changes made)


/******************************** Student dummy *********************************/
gen studentflag = -9 
replace studentflag = 0 if les_c3 == 1 | les_c3 == 3
replace studentflag = 1 if les_c3 == 2 

label var studentflag "Student"
/*
tab les_c3 student 
tab les_c4 student 
*/
* Non-student dummy 
gen non_student = (les_c3 != 2)
replace non_student = . if les_c3 == -9		


/******************************Education status*******************************/
/*Use hiqual variable, code negative values to missing
Low education: Other qualification, no qualification
Medium education: Other higher degree, A-level etc, GCSE etc
High education: Degree
*/

replace hiqual_dv = . if hiqual_dv < 0
sort  idperson swv

// Impute missing values using lagged values of up to 13 previous waves
forvalues i = 1/13 {
replace hiqual_dv = l`i'.hiqual_dv if missing(hiqual_dv) & !missing(l`i'.hiqual_dv) & jbstat != 7 
}
recode hiqual_dv (1 = 1 "High") ///
	(2 3 4 = 2 "Medium") ///
	(5 9 = 3 "Low") ///
	, into(deh_c3)
la var deh_c3 "Education status"
label list deh_c3

replace deh_c3 = 3 if dag < ${age_leave_school}  & dag>-9 //Children have low level of education until they leave school
replace deh_c3 = -9 if deh_c3 == . 

fre deh_c3	// 5.3% missing
/*
fre dag if deh_c3 == -9  
fre swv if deh_c3 == -9  //mosty first and last wave 
tab deh_c3 swv, col
bys swv: sum deh_c3 if deh_c3 > 0 
*/

gen deh_orig  = deh_c3 


* Impute missing values 
/* 
Impute missing values using the monotonicity of education. Individuals can only 
increase their educaiton level over time and there is a min and a mix, 
therefore can use lagged and lead values for those who have not been students 
in the intervening period (and sometimes before and after given max/min).
Max of previous observations per indiviudal determined automatically.
*/

sort idperson swv
xtset idperson swv 


* Variable containing imputed values 
gen imp_deh_mono = deh_c3 if deh_c3 > 0 

* get max number of waves in the data
bysort idperson (swv): gen count = _n
summ count, meanonly
local maxwaves = r(max)

sort idperson swv

* Looking backwards 
forvalues i = 2/`maxwaves' {
	
	* High in the past, high today (max and monotonic)
	replace imp_deh_mono = imp_deh_mono[_n-1] if ///
		idperson == idperson[_n-1] & imp_deh_mono[_n-1] == 1 & ///
		imp_deh_mono == . & count == `i' 
	
	* Populate with previous observation if:
	
	* Remain a non-student 
	replace imp_deh_mono = imp_deh_mono[_n-1] if ///
		idperson == idperson[_n-1] & non_student[_n-1] == 1 & ///
		non_student == 1 & imp_deh_mono == . & imp_deh_mono[_n-1] != . & ///
		count == `i' 
		
	* Remain a student 	
	replace imp_deh_mono = imp_deh_mono[_n-1] if ///
		idperson == idperson[_n-1] & non_student[_n-1] == 0 & ///
		non_student == 0 & imp_deh_mono == . & imp_deh_mono[_n-1] != . & ///
		count == `i' 
		
	* Transition into education 	
	replace imp_deh_mono = imp_deh_mono[_n-1] if ///
		idperson == idperson[_n-1] & non_student[_n-1] == 1 & ///
		non_student == 0 & imp_deh_mono == . & imp_deh_mono[_n-1] != . & ///
		count == `i'

	* Student current, missing previous 
	replace imp_deh_mono = imp_deh_mono[_n-1] if ///
		idperson == idperson[_n-1] & non_student[_n-1] == . & ///
		non_student == 0 & imp_deh_mono == . & imp_deh_mono[_n-1] != . & ///
		count == `i' 		
		
	* Missing current, non-student previous 
	replace imp_deh_mono = imp_deh_mono[_n-1] if ///
		idperson == idperson[_n-1] & non_student[_n-1] == 1 & ///
		non_student == . & imp_deh_mono == . & imp_deh_mono[_n-1] != . & ///
		count == `i' 	
}

* Looking forwards

* Reverse sort
gsort idperson -swv 

forvalues i = `=`maxwaves'-1'(-1)1 {
	
	* Low in the future, low today (min and monotonicity)
	replace imp_deh_mono = imp_deh_mono[_n-1] if ///
		idperson == idperson[_n-1] & imp_deh[_n-1] == 3 & ///
		imp_deh_mono == . & count == `i' 	
		
	* Populate with future observation if:
	
	* Remain a non-student 
	replace imp_deh_mono = imp_deh_mono[_n-1] if ///
		idperson == idperson[_n-1] & non_student[_n-1] == 1 & ///
		non_student == 1 & imp_deh_mono == . & imp_deh_mono[_n-1] != . & ///
		count == `i' 
		
	* Remain a student 
	replace imp_deh_mono = imp_deh_mono[_n-1] if ///
		idperson == idperson[_n-1] & non_student[_n-1] == 0 & ///
		non_student == 0 & imp_deh_mono == . & imp_deh_mono[_n-1] != . & ///
		count == `i' 	
		
	* Transition into education next year
	replace imp_deh_mono = imp_deh_mono[_n-1] if ///
		idperson == idperson[_n-1] & non_student[_n-1] == 0 & ///
		non_student == 1 & imp_deh_mono == . & imp_deh_mono[_n-1] != . & ///
		count == `i' 		
	
	* Missing current, student next
	replace imp_deh_mono = imp_deh_mono[_n-1] if ///
		idperson == idperson[_n-1] & non_student[_n-1] == 0 & ///
		non_student == . & imp_deh_mono == . & imp_deh_mono[_n-1] != . & ///
		count == `i' 	
	
	* Non-student current, missing next
	replace imp_deh_mono = imp_deh_mono[_n-1] if ///
		idperson == idperson[_n-1] & non_student[_n-1] == . & ///
		non_student == 1 & imp_deh_mono == . & imp_deh_mono[_n-1] != . & ///
		count == `i' 	
}

sort idperson swv 

fre deh_c3 //5.39% 
 
* Missing 
replace imp_deh_mono = -9 if imp_deh_mono == . 

fre imp_deh_mono // 4.56%

gen flag_deh_imp_mono = (imp_deh_mono!= -9 & deh_c3 == -9) 
lab var flag_deh_imp_mono "FLAG: =1, impute age using logical deduction"

* Comparison plot
twoway ///
    (histogram deh_c3 if deh_c3 > 0, percent ///
        fcolor(blue) lcolor(blue) barwidth(0.8)) ///
    (histogram imp_deh if imp_deh > 0, percent ///
        fcolor(none) lcolor(red) lwidth(medthick) barwidth(0.8)), ///
    title("Observed vs imputed education") ///
    legend(order(1 "Observed" 2 "Imputed")) ///
    graphregion(color(white))

twoway ///
    (histogram deh_c3 if deh_c3 > 0 & dag < 30 & dag > 16, percent ///
        fcolor(blue) lcolor(blue) barwidth(0.8)) ///
    (histogram imp_deh if imp_deh > 0 & dag < 30 & dag > 16, percent ///
        fcolor(none) lcolor(red) lwidth(medthick) barwidth(0.8)), ///
    title("Observed vs imputed education (ages 17–29)") ///
    legend(order(1 "Observed" 2 "Imputed")) ///
    graphregion(color(white))

graph drop _all 	

* Add imputed values to variable 
//tab deh_c3 imp_deh, m
//fre imp_deh if  deh_c3==-9

replace deh_c3 = imp_deh if deh_c3 == -9 & imp_deh!=-9 //(6,524 real changes made)
/*
fre deh_c3
fre dag if deh_c3 == -9  
fre swv if deh_c3 == -9  
tab deh_c3 swv, col
bys swv: sum deh_c3 if deh_c3 > 0 
*/
count if deh_c3 == -9  // 35,812 obs

/*
Still missing education level information if: 
- Missing education when transition out of education 
- Individual does not report any education level in their panel 
- Missing activity status and missing education level 
- Missing previous activity status (and now a non-student)

Use regression based imputation at the end of the file to impute the remaining
missing values. 
*/
//fre deh_c3


/************************Education level (4 categories)************************/
* Create four category version with an unassigned cat for those in initital edu spell 
gen deh_c4 = deh_c3 

replace deh_c4 = 0 if dag < ${age_leave_school} 
replace deh_c4 = 0 if ded == 1

lab var deh_c4 "Education status, 4 cat"
lab define deh_c4 3 "low" 2 "medium" 1 "high" 0 "initial spell"
lab values deh_c4 deh_c4

//fre deh_c4 
 
 
/***************************Partner's education status*************************/
/*preserve
keep swv idperson deh_c3
rename idperson idpartner 
rename deh_c3 dehsp_c3 
save "$dir_data/temp_deh", replace
restore

merge m:1 swv idpartner using "$dir_data/temp_deh"
la var dehsp_c3 "Education status partner"
keep if _merge == 1 | _merge == 3
drop _merge
lab define deh 1 "High" 2 "Medium" 3 "Low"
lab val dehsp_c3 deh
replace dehsp_c3=-9 if missing(dehsp_c3) & idpartner>0
//fre dehsp_c3
*/

/********************************Parents' education status*********************/ 
//bys swv: fre maedqf paedqf
replace maedqf = . if maedqf < 0 
replace paedqf = . if paedqf < 0

recode maedqf (5 = 1 "High") ///
	(4 3 = 2 "Medium") ///
	(2 1 97 = 3 "Low") ///
	, into(dehm_c3) //dehm_c3 is mother's highest qualification
la var dehm_c3 "Education status mother"

recode paedqf (5 = 1 "High") ///
	(4 3 = 2 "Medium") ///
	(2 1 97 = 3 "Low") ///
	, into(dehf_c3) //dehf_c3 is father's highest qualification
la var dehf_c3 "Education status father"

save "$dir_data/temp", replace

*If missing and living with parents, use their current level of education:
*1. Create mothers and fathers education levels in new file with person and hh id 
*2. Merge by father and mother id and hh id
keep swv idperson idhh deh_c3
drop if deh_c3==-9  | missing(deh_c3)
rename idperson idmother
rename deh_c3 mother_educ
save "$dir_data/mother_edu", replace

rename idmother idfather
rename mother_educ father_educ
save "$dir_data/father_edu", replace

use "$dir_data/temp", clear
merge m:1 swv idmother idhh using "$dir_data/mother_edu" 
keep if _merge == 1 | _merge == 3
drop _merge
merge m:1 swv idfather idhh using "$dir_data/father_edu"
keep if _merge == 1 | _merge == 3
drop _merge

replace dehm_c3 = mother_educ if missing(dehm_c3)
replace dehf_c3 = father_educ if missing(dehf_c3)

fre dehm_c3 if dgn>0 & dag>0
fre dehf_c3 if dgn>0 & dag>0

*Identify the highest parental education status 
//recode dehm_c3 dehf_c3 (.=0) 
cap drop dehmf_c3
egen dehmf_c3 = rowmax(dehm_c3 dehf_c3)
lab var dehmf_c3 "highest parental education status"
fre dehmf_c3
//recode dehm_c3 dehf_c3 (0=.) 
fre dehmf_c3 if dehm_c3==. 
fre dehmf_c3 if dehf_c3==. 

*predict highest parental education status if missing 
//Recode education level (outcome variable) so 1 = Low education, 2 = Medium education, 3 = High education
recode dehmf_c3 ///
	(1 = 3) ///
	(3 = 1) ///
	, gen(dehmf_c3_recoded)
	
la def dehmf_c3_recoded 1 "Low" 2 "Medium" 3 "High"
la val dehmf_c3_recoded dehmf_c3_recoded
fre dehmf_c3_recoded

*ordered probit model to replace missing values  
recode dgn dag drgn1 (-9=.) , gen (dgn2 dag2 drgn12)
fre dgn2 dag2 drgn12

xi: oprobit dehmf_c3_recoded i.dgn2 dag2 ib8.drgn12 i.swv, vce(robust)
predict pred_probs1 pred_probs2 pred_probs3, pr

//Identify the category with the highest predicted probability
egen max_prob = rowmax(pred_probs1 pred_probs2 pred_probs3)
//Impute missing values based on predicted probabilities
gen imp_dehmf_c3_recoded = .
replace imp_dehmf_c3_recoded = 1 if max_prob == pred_probs1
replace imp_dehmf_c3_recoded = 2 if max_prob == pred_probs2
replace imp_dehmf_c3_recoded = 3 if max_prob == pred_probs3

fre imp_dehmf_c3_recoded if missing(dehmf_c3_recoded) 
fre imp_dehmf_c3_recoded if !missing(dehmf_c3_recoded)

recode imp_dehmf_c3_recoded ///
	(1 = 3) ///
	(3 = 1) ///
	, gen(imp_dehmf_c3)

tab2 imp_dehmf_c3_recoded imp_dehmf_c3

cap gen dehmf_c3_flag = missing(dehmf_c3) 
lab var dehmf_c3_flag "=1 if dehmf_c3 is imputed"
replace dehmf_c3 = round(imp_dehmf_c3) if missing(dehmf_c3) 
lab define dehmf_c3 1 "High" 2 "Medium" 3 "Low"

bys dehmf_c3_flag: fre dehmf_c3
drop dehmf_c3_recoded dgn2 dag2 drgn12 _Idgn2_1 _Iswv_* pred_probs* max_prob imp_dehmf_c3_recoded imp_dehmf_c3

 

/****************************Return to education*******************************/
sort idperson swv
xtset idperson swv 

cap gen der = -9 
replace der = 0 if l.jbstat !=7 & l.jbstat<.
replace der = 1 if jbstat==7 & l.jbstat !=7 & l.jbstat<. 
la val der dummy
la var der "Return to education"
//fre der


/*****************************Partnership status*******************************/
gen dcpst = -9 
replace dcpst = 1 if idpartner > 0 & !missing(idpartner) //partnered 
replace dcpst = 2 if idpartner < 0 | missing(idpartner)
lab var dcpst "Partnership status"
lab def dcpst 1 "partnered" 2 "single" 
lab val dcpst dcpst 

/*fre dcpst  
tab dcpst swv, col
bys swv: sum dcpst if dcpst > 0 
*/
 
* Impose min partnership formation age (own and partner)
replace flag_young_partnership = 1 if dcpst == 1 & dag < ${age_form_partnership}
replace flag_young_partnership = 1 if dcpst == 1 & dagsp < ${age_form_partnership}

lab var flag_young_partnership ///
	"FLAG: Made single because stated in a partnership below the age permitted to form in simulation"
	
replace dcpst = 2 if dag < ${age_form_partnership}

//count if dcpst == . 
//tab dcpst 

* Check consistency 
tab dun dcpst


/****************************** WIDOW STATUS **********************************/
fre mastat_dv /*mastat_dv -- De facto marital status
	0  Child under 16
	1  Single and never married/in	
	civil partnership				          
	2  Married	
	3  In a registered same-sex civil	
	partnership				          
	4  Separated but legally married	
	5  Divorced	
	6  Widowed	
	7  Separated from civil partner	
	8  A former civil partner	
	9  A surviving civil partner	
	10 Living as couple	
*/		          
cap gen widow = -9 if mastat_dv<0
replace widow = 0 if mastat_dv>0
replace widow =1 if mastat_dv==6 | mastat_dv==9 
lab var widow "Widow flag" 
fre widow
* Check consistency 
tab dcpst widow

replace widow = 0 if dcpst == 1

/**************************** ENTER PARTNERSHIP *******************************/
/*
Only populated if able to transition into a relationship
*/
sort idperson swv 
xtset idperson swv 

gen dcpen = -9
replace dcpen = 0 if (l.dcpst == 2)
replace dcpen = 1 if (dcpst == 1 & l.dcpst == 2)
replace dcpen = 1 if dcpst == 1 & dag == ${age_form_partnership}

lab val dcpen dummy
lab var dcpen "Enter partnership"
/*
fre dcpen 
tab dcpen year, col
bys swv: sum dcpen if dcpen >= 0 
*/


/****************************** NEW PARTNERSHIP *******************************/
gen new_rel = 0 if dcpst == 1
replace new_rel = 1 if dcpen == 1

lab var new_rel "Partnership in first year"
/*
tab new_rel year, col 
bys swv: sum new_rel if new_rel >= 0 
*/

/**************************** EXIT PARTNERSHIP ********************************/
/*
Only populated if can transition out of a partnership (not because of death of a partner)
*/
sort idperson swv 
xtset idperson swv 

gen dcpex = -9
replace dcpex = 0 if l.dcpst == 1
replace dcpex = 1 if dcpst == 2 & l.dcpst == 1 
replace dcpex = -9 if widow == 1 & dcpex == 1
lab val dcpex dummy
lab var dcpex "Exit partnership" 
/*
fre dcpex  
tab dcpex year, col
bys swv: sum dcpex if dcpex >= 0 
*/
 

/*****************************Age difference partners*************************/
gen dcpagdf = dag - dagsp if (dag > 0 & dagsp > 0) //Leave with negative values? Or should be absolute?
la var dcpagdf "Partner's age difference"



/***********************Activity status variable with retirement*************/
*Les_c4 adds retired status to les_c3. 
clonevar les_c4 = les_c3
replace les_c4 = 4 if jbstat==4 
lab var les_c4 "LABOUR MARKET: Activity status"
lab define les_c4  1 "Employed or self-employed"  2 "Student"  3 "Not employed"  4 "Retired"
lab val les_c4 les_c4
//fre les_c4

/*
Conditions below are imposed for consistency with SimPaths: 
	- Can not retire below a given age 
	- Retirement is an absorbing state
	- Force retirement above a given age
*/
tab2 les_c3 les_c4

* Impose consistency across les_c3 and les_c4
replace les_c3 = 3 if les_c4 == 4  //(4 real changes made)

* Rule out retirement before a certain age 
gen flag_no_retire_young = (dag < ${age_can_retire} & les_c4 == 4) 

lab var flag_no_retire_young ///
	"FLAG: Made non-employed because stated to retire before the age of 50"

replace les_c4 = 3 if dag < ${age_can_retire} & les_c4 == 4 	// (220 real changes made)

* Make retirement an absorbing state - primarily eliminates returning to 
* education among the retired 
sort idperson swv 

gen flag_retire_absorb = 0 if les_c4 == 4
replace flag_retire_absorb = 0 if idperson == idperson[_n-1] & ///
	flag_retire_absorb[_n-1] == 0
	
replace flag_retire_absorb = 1 if les_c4 != 4 & flag_retire_absorb == 0 	
replace flag_retire_absorb = 0 if flag_retire_absorb == . 

lab var flag_retire_absorb ///
	"FLAG: Changed activity status due to retirement absorbing assumption"

replace les_c4 = 4 if idperson == idperson[_n-1] & les_c4[_n-1] == 4 & ///
	les_c4 != 4  // (11,876 real changes made)

* Force retirement above a certain age 
gen flag_retire_force = 0 
replace flag_retire_force = 1 if dag >= ${age_force_retire} & les_c4 != 4
	// (2,738 real changes made)

lab var flag_retire_force ///
	"FLAG: Forced into retirement due to age (after absorbign assumption)"

replace les_c3 = 3 if dag >= ${age_force_retire}	//(5,069 real changes made)
replace les_c4 = 4 if dag >= ${age_force_retire}	//(2,738 real changes made)
	
* Make les_c3 consistent with change made to les_c4
replace les_c3 = 3 if les_c4 == 4 	// (5,905 real changes made)	

* Check consistency 
tab2 les_c3 les_c4, row

replace les_c4 = -9 if les_c4 == . //(69,043 real changes made)
/*
fre les_c4 	
tab les_c4 swv, col
bys swv: sum les_c4
*/
 


/****************************Partner's activity status:***********************/
preserve
keep swv idperson idhh les_c3
rename les_c3 lessp_c3
rename idperson idpartner
save "$dir_data/temp_lesc3", replace
restore
merge m:1 swv idpartner idhh using "$dir_data/temp_lesc3"
keep if _merge == 1 | _merge == 3
la var lessp_c3 "Partner's activity status"
drop _merge
//fre lessp_c3


/**********************Partner's activity status adding retirement*************/
preserve
keep swv idperson idhh les_c4
rename les_c4 lessp_c4
rename idperson idpartner
save "$dir_data/temp_lesc4", replace
restore
merge m:1 swv idpartner idhh using "$dir_data/temp_lesc4"
keep if _merge == 1 | _merge == 3
la var lessp_c4 "LABOUR MARKET: Partner's activity status"
lab val lessp_c4 les_c4
drop _merge
//fre lessp_c4


/***********************Own and Spousal Activity Status***********************/
gen lesdf_c4 = -9
replace lesdf_c4 = 1 if les_c3 == 1 & lessp_c3 == 1 & dcpst == 1 //Both employed
replace lesdf_c4 = 2 if les_c3 == 1 & (lessp_c3 == 2 | lessp_c3 == 3) & dcpst == 1 //Employed, spouse not employed
replace lesdf_c4 = 3 if (les_c3 == 2 | les_c3 == 3) & lessp_c3 == 1 & dcpst == 1 //Not employed, and spouse employed
replace lesdf_c4 = 4 if (les_c3 == 2 | les_c3 == 3) & (lessp_c3 == 2 | lessp_c3 == 3) & dcpst == 1 //Both not employed

la def lesdf_c4_lb 1"Both employed" 2"Employed and spouse not employed" 3"Not employed and spouse employed" 4"Both not employed" -9"Missing"
la val lesdf_c4 lesdf_c4_lb

la var lesdf_c4 "Own and spousal activity status"
//fre lesdf_c4


/******************************Civil servant status***************************/
gen lcs=0
// R.K. (11.05.2017) (we can use SIC 2007 condensed version- this is what Paola does for FRS EUROMOD)
replace lcs=1 if jbsic07_cc==84
la var lcs "Civil Servant"
lab val lcs dummy 
//fre lcs 


/***********************************Hours of work*****************************/
//lhw is the sum of the below, but don't want to take -9 into account. Recode into missing value. 
recode jbhrs (-9/-1 . = .) 
recode jbot (-9/-1 . = .)
recode jshrs (-9/-1 . = .)

egen lhw=rowtotal(jbhrs jbot jshrs)
replace lhw = ceil(lhw)
replace lhw=. if missing(jbhrs) & missing(jbot) & missing(jshrs)


la var lhw "Hours worked per week (capped at 126)"
replace lhw = 126 if lhw > 126 & !missing(lhw) //ensure lhw doesn't go above weekly max 168 minus 6*7 hours of sleep.


// Lag(1) of hours of work
xtset // check if xtset correct 
gen l1_lhw = l1.lhw

replace l1_lhw = lhw if l1.les_c4 == 1 & les_c4 == 1 & missing(l1_lhw) // replace lagged value with current value if employed last period and this period
replace l1_lhw = lhw if les_c4 == 1 & missing(l1_lhw) // replace lagged value with current value if above not successful 
replace l1_lhw = 0 if l1.les_c4 != 1 // replace lagged value with zero if not compatible with lagged employment state
replace l1_lhw = 0 if les_c4 != 1 & missing(l1_lhw) // replace with zero if not working and l1_lhw still missing

* Impose age restrictions
* Cannot work when a child
replace lhw = 0 if dag < ${age_seek_employment} //(20 real changes made)

* Cannot work above a certain age
replace lhw = 0 if dag >= ${age_force_retire}	//(1,263 real changes made)

* Check consistency - how many non-workers report positive hours? 
bys les_c3: fre lhw 
bys les_c4: fre lhw 
	
sort idperson swv	
	
/*
Imposing consistency: We decided to assume the "non-working" response is true,
this implies: 
	- zero hours => not working activity status 
	- not working activity status => zero hours

We also have many observations with missing information that require an 
additional rule. 
	- positive hours and missing activity => employed 
	- working and missing hours => impute hours 

Impute hours using surrounding observations for longitudinal consistency and 
then use hot deck imputation by age group and sex. 

*/	
	
* Consistency of zero hours cases
tab les_c3 if lhw == 0 	
tab les_c4 if lhw == 0 	

sum lhw if les_c3 == 2	
sum lhw if les_c4 == 4	
	
* Overwrite hours work if report not working 
gen flag_impose_zero_hours_ne = (lhw > 0 & lhw != . & les_c4 == 3)
gen flag_impose_zero_hours_retire = (lhw > 0 & lhw != . & les_c4 == 4)
gen flag_impose_zero_hours_student = (lhw > 0 & lhw != . & les_c3 == 2)

lab var flag_impose_zero_hours_ne ///
	"FLAG: Replaced +ive hours of work with 0 as report not-employed"
lab var flag_impose_zero_hours_retire ///
	"FLAG: Replaced +ive hours of work with 0 as report retired"
lab var flag_impose_zero_hours_student ///
	"FLAG: Replaced +ive hours of work with 0 as report student"

replace lhw = 0 if les_c3 == 3	
replace lhw = 0 if les_c3 == 2	

* Overwrite activity status if report zero hours 
gen flag_not_work_hours = (lhw  == 0 & les_c3 == 1)

lab var flag_not_work_hours ///
	"FLAG: Replaced activity status with non-employed as report 0 hours"

replace les_c3 = 3 if lhw  == 0 & les_c3 == 1 
replace les_c4 = 3 if lhw  == 0 & les_c4 == 1 

* Consistency of missing hours cases 
tab les_c3 if lhw == .
tab les_c4 if lhw == . 
		
* Overwrite les_c* if report hours but missing activity status information
gen flag_missing_act_hours = (lhw > 0 & lhw != . & les_c3 == -9)

lab var flag_missing_act_hours ///
"FLAG: Replaced missing activity status with working as report positive hours"

replace les_c3 = 1 if lhw > 0 & lhw != . & les_c3 == -9
replace les_c4 = 1 if lhw > 0 & lhw != . & les_c4 == -9	//& les_c3 == 1	
		
* Investigate the characteristics of those missing hours and reporting to work
gen x = (lhw == .)
tab swv x if les_c4 == 1, row // up to 10% missing in one of the waves but lower than 5% in others 

tab dag if les_c4 == 1 & lhw == . // distributed across all ages <65 (16-74)

/*
Not as many missing values as in SILC. 
Suggest to impute these missing hours because treating these observations as not
employed would be creating a bias given the magnitude of the issue. 

Below, first impose longitudinal consistency - use own adjacent values. 
For the remaining observations use empirical hot deck imputation within strata. 
(age group and gender)
*/		
		
* Longitudinal consistency 
sort idperson swv

* Backwards
* Direct 
gen flag_missing_hours_act_adj = (lhw == . & les_c3 == 1 & ///
	les_c3[_n-1] == 1 & lhw[_n-1] != . & idperson == idperson[_n-1] & ///
	swv == swv[_n-1] + 1)

* Fill 	
replace flag_missing_hours_act_adj = 1 if lhw == . & les_c3 == 1 & ///
	flag_missing_hours_act_adj[_n-1] == 1 & idperson == idperson[_n-1] & ///
	swv == swv[_n-1] + 1	
//(1,116 real changes made)
replace lhw = lhw[_n-1] if lhw == . & les_c3 == 1 & les_c3[_n-1] == 1 & ///
	lhw[_n-1] != . & idperson == idperson[_n-1] & swv == swv[_n-1] + 1
//(7,326 real changes made)
		
count if lhw == . & les_c4 == 1  	//  4,336	

* Forwards
* Direct
replace	flag_missing_hours_act_adj = 1 if lhw == . & les_c3 == 1 & ///
	les_c3[_n+1] == 1 & lhw[_n+1] != . & idperson == idperson[_n+1] & ///
	swv == swv[_n+1] - 1
	//(1,261 real changes made)
* Fill 	
replace flag_missing_hours_act_adj = 1 if lhw == . & les_c3 == 1 & ///
	flag_missing_hours_act_adj[_n+1] == 1   & idperson == idperson[_n+1] & ///
	swv == swv[_n-1] - 1		
//(3 real changes made)
replace lhw = lhw[_n+1] if lhw == . & les_c3 == 1 & les_c3[_n+1] == 1 & ///
	lhw[_n+1] != . & idperson == idperson[_n+1] & swv == swv[_n+1] - 1
//(1,261 real changes made)
		
lab var flag_missing_hours_act_adj ///
"FLAG: Replaced missing hours with positive amount using info from adjacent cells as report working "

count if lhw == . & les_c4 == 1  	// 3,075 obs to impute 

* Imputation 
set seed 102345

sort idperson swv

* Observations to be imputed 
gen need_imp = (les_c4 == 1 & lhw == .) 
 
* Strata
gen ageband = floor(dag/10)*10

egen stratum = group(ageband dgn), label   

* Donor pool 
preserve 

keep if les_c4 == 1 & lhw > 0 & lhw != .
keep lhw stratum idperson swv
bys stratum (idperson swv): gen draw = _n
bys stratum (idperson swv): gen n_donors  = _N
rename lhw donor_lhw
drop idperson
save "$dir_data/temp_lhw_donors", replace

* Counts lookup (one row per stratum)
keep stratum n_donors
bys stratum: keep if _n == 1
save "$dir_data/temp_donorsN", replace

restore

merge m:1 stratum using "$dir_data/temp_donorsN", nogen

* Assign random donor 
gen draw = . 

sort stratum idperson swv

bys stratum (idperson swv): replace draw = ceil(runiform()*n_donors[1]) if ///
	need_imp == 1 & n_donors > 0 

merge m:1 stratum draw using "$dir_data/temp_lhw_donors", ///
	keepusing(donor_lhw draw) 

drop if _m == 2 //1 obs w/t donor because their sex is undefined 
drop _m
	
replace lhw = donor_lhw if need_imp == 1 

tab lhw if need_imp == 1
		 		
rename need_imp	flag_missing_hours_act_imp

lab var flag_missing_hours_act_imp	///
"FLAG: Replaced hours from missing to positive amount using hot deck imputation"	
			
drop x donor_lhw n_donor draw 			
			
count if lhw == . & les_c3 == -9 	// 69,409 obs

/* Check consistency - how many workers do not report hours? 
tab les_c3 if lhw == . 
tab les_c3 if lhw > 0 & lhw != . 
tab les_c3 if lhw == 0 

tab les_c4 if lhw == . 
tab les_c4 if lhw > 0 & lhw != .
tab les_c4 if lhw == 0 

tab les_c3 les_c4

count if les_c3 == .
count if les_c4 == .

count if les_c3 == -9
count if les_c4 == -9 
count if les_c4 == -9  & lhw == . 	// 69,409
*/

/*****************************Number of children*******************************/
/* 
Note idmother and idfather are not just reported if the bioloigcal parent but 
also the step parent etc. 
Doesn't account for the age of the mother, therefore permits teenage and old
mothers. 
*/
/*Number of children aged 0-2 (Checked against manually generating count of children 0-2 per HH - same numbers, 
but nch02_dv distinguishes missing and 0)*/

gen dnc02 = nch02_dv
recode dnc02 (-9 = 0)
la var dnc02 "Number of children aged 0-2"

/*Number of dependent children aged 0-18 (dependent doesn't include children who have spouse / child but live with parents)
Gen flag for a dependent child aged 0-18, with at least one parent and classified as dependent child
  pns1pid = Biological/step/adoptive parent 1: Cross-wave person identifier (PIDP). 
  pns2pid = Biological/step/adoptive parent 2: Cross-wave person identifier (PIDP). 
  If there is more than one parent, the parent with the lowest PNO is the first parent and the parent with the higher PNO is the second parent*/
  
gen depChild = 1 if (age_dv >= 0 & age_dv < ${age_max_dep_child}) & (pns1pid > 0 | pns2pid > 0) & (depchl_dv == 1)
bys swv idhh: egen dnc = sum(depChild)
*drop depChild
la var dnc "Number of dependent children 0-${age_max_dep_child}"

/*
No age consistency imposed here 
count if dag > 42 & dgn == 0 & dnc02 > 0 & dnc02 != . //  2,265 cases 
count if dag > 44 & dgn == 0 & dnc02 > 0 & dnc02 != . // 1,840 cases 
*/

/****************************Pension Age***************************************/
/*cap gen bdt = mdy(1, 15, birthy) /*month of birth is available in special license only*/
*/
/*State Retirement Ages for Men in the UK (2009-2023):
2009-2010: 65
2010-2011: 65
2011-2012: 65
2012-2013: 65
2013-2014: 65
2014-2015: 65
2015-2016: 65
2016-2017: 65
2017-2018: 65
2018-2019: 65
2019-2020: 65
2020-2021: 66
2021-2022: 66
2022-2023: 66
State Retirement Ages for Women in the UK (2009-2023):
2009-2010: 60
2010-2011: 60
2011-2012: 60
2012-2013: 61
2013-2014: 61
2014-2015: 62
2015-2016: 62
2016-2017: 63
2017-2018: 63
2018-2019: 64
2019-2020: 65
2020-2021: 65
2021-2022: 66
2022-2023: 66
*/
gen dagpns = 0
//for men
replace dagpns = 1 if dgn==1 & dag>=65 & stm>=2009 & stm<2020 
replace dagpns = 1 if dgn==1 & dag>=66 & stm>=2020 
//for women 
replace dagpns = 1 if dgn==0 & dag>=60 & stm>=2009 & stm<2012
replace dagpns = 1 if dgn==0 & dag>=61 & stm>=2012 & stm<2014
replace dagpns = 1 if dgn==0 & dag>=62 & stm>=2014 & stm<2016
replace dagpns = 1 if dgn==0 & dag>=63 & stm>=2016 & stm<2018
replace dagpns = 1 if dgn==0 & dag>=64 & stm>=2018 & stm<2019
replace dagpns = 1 if dgn==0 & dag>=65 & stm>=2019 & stm<2021
replace dagpns = 1 if dgn==0 & dag>=66 & stm>=2021 
//fre dagpns


/****************************Pension age of a spouse***************************/
preserve
keep swv idperson idhh dagpns
rename dagpns dagpns_sp
rename idperson idpartner
save "$dir_data/temp_dagpns", replace
restore
merge m:1 swv idpartner idhh using "$dir_data/temp_dagpns"
keep if _merge == 1 | _merge == 3
la var dagpns_sp "Pension age - partner"
drop _merge
replace dagpns_sp=-9 if idpartner<0


/*******************************Flag for adult children***********************/
//add parental ages & retirement status
preserve
keep if dgn == 0
keep swv idhh idperson dag dagpns les_c4
rename idperson idmother
rename dag dagmother
rename dagpns dagpnsmother
rename les_c4 les_c4mother
save "$dir_data/temp_mother_dag", replace
restore, preserve
keep if dgn == 1
keep swv idhh idperson dag dagpns les_c4
rename idperson idfather
rename dag dagfather
rename dagpns dagpnsfather
rename les_c4 les_c4father
save "$dir_data/temp_father_dag", replace 
restore

merge m:1 swv idhh idmother using "$dir_data/temp_mother_dag"
keep if _merge == 1 | _merge == 3
drop _merge
merge m:1 swv idhh idfather using "$dir_data/temp_father_dag"
keep if _merge == 1 | _merge == 3
drop _merge


/*Individual is considered as adult child if 
- they have at least one parent in the household (i.e. non-missing parental age) 
- aged 18+
- do not have a partner living in the same household 
- is at least 15 years younger than either of their parents
- neither of their parents is of the state retirement age in that particular year & neither is retired   
*/
cap gen  adultchildflag = 0

replace  adultchildflag = 1 if (idmother > 0 | idfather > 0) ///
    & dag >= ${age_leave_parental_home} & dag<=${age_force_retire} & idpartner <= 0  /*added upper age filter as the one used in LS model */

replace adultchildflag = 1 if (!missing(dagmother) | !missing(dagfather)) ///
	& dag >= ${age_leave_parental_home} & idpartner <= 0		
	
	
/* Exclude if both parents retired or at statutory retirement age */
replace  adultchildflag = 0 if dagpnsmother == 1 & dagpnsfather == .
replace  adultchildflag = 0 if dagpnsmother == . & dagpnsfather == 1
replace  adultchildflag = 0 if dagpnsmother == 1 & dagpnsfather == 1

replace  adultchildflag = 0 if les_c4mother == 4 & les_c4father == .
replace  adultchildflag = 0 if les_c4mother == . & les_c4father == 4
replace  adultchildflag = 0 if les_c4mother == 4 & les_c4father == 4

replace  adultchildflag = 0 if les_c4mother == 4 & dagpnsfather == 1
replace  adultchildflag = 0 if les_c4father == 4 & dagpnsmother == 1

/* Exclude if both parents < 15 years older than child */
replace  adultchildflag = 0 if (dagfather-dag)<=15 & dagmother == .
replace  adultchildflag = 0 if dagfather == . & (dagmother-dag) <= 15 
replace  adultchildflag = 0 if (dagfather-dag) <= 15 & (dagmother-dag)<= 15

//fre  adultchildflag

/*Account for cases missing information
replace adultchildflag = -9 if idmother>0 & ///
	(dagmother==. | dagmother<0 | les_c4mother==. | les_c4mother<0) & dag >= (${age_leave_parental_home} - 1) & dag<=${age_force_retire}
replace adultchildflag = -9 if idfather>0 & ///
	(dagfather==. | dagfather<0 | les_c4father==. | les_c4father<0) & dag >= (${age_leave_parental_home} - 1) & dag<=${age_force_retire}
fre adultchildflag*/
//2.7% have missing info on one of their parents, not sure if it is worth dropping them 



/************************Household composition*********************************/
cap gen dhhtp_c4 = -9
replace dhhtp_c4 = 1 if dcpst == 1 & dnc == 0 //Couple, no children
replace dhhtp_c4 = 2 if dcpst == 1 & dnc > 0 & !missing(dnc) //Couple, children
replace dhhtp_c4 = 3 if (dcpst == 2) & (dnc == 0 | dag <= ${age_becomes_responsible} | adultchildflag== 1) 
/*Single, no children (Note: adult children and children below age to become responsible 
should be assigned "no children" category, even if there are some children in the household)*/
replace dhhtp_c4 = 4 if (dcpst == 2) & dnc > 0 & !missing(dnc) & dhhtp_c4 != 3 //Single, children

la def dhhtp_c4_lb 1"Couple with no children" 2"Couple with children" 3"Single with no children" 4"Single with children"
la values dhhtp_c4 dhhtp_c4_lb
la var dhhtp_c4 "Household composition"
//fre dhhtp_c4


* With economic activity 
gen dhhtp_c8 = . 

replace dhhtp_c8 = 1 if dhhtp_c4 == 1 & lessp_c3 == 1
replace dhhtp_c8 = 2 if dhhtp_c4 == 1 & lessp_c3 == 2
replace dhhtp_c8 = 3 if dhhtp_c4 == 1 & lessp_c3 == 3	
replace dhhtp_c8 = 4 if dhhtp_c4 == 2 & lessp_c3 == 1
replace dhhtp_c8 = 5 if dhhtp_c4 == 2 & lessp_c3 == 2
replace dhhtp_c8 = 6 if dhhtp_c4 == 2 & lessp_c3 == 3	
replace dhhtp_c8 = 7 if dhhtp_c4 == 3
replace dhhtp_c8 = 8 if dhhtp_c4 == 4

lab def dhhtp_c8 	1 "Couple with no children, spouse employed" ///
					2 "Couple with no children, spouse student" ///
					3 "Couple with no children, spouse not employed" ///
					4 "Couple with children, spouse employed" ///
					5 "Couple with children, spouse student" ///
					6 "Couple with children, spouse not employed" ///
					7 "Single with no children" ///
					8 "Single with children" 
lab val dhhtp_c8 dhhtp_c8	

lab var dhhtp_c8 "Household composition with economic activity info"

fre dhhtp_c8 // 3.03% single parents
/*
tab dhhtp_c8 year, col 	
bys swv: sum dhhtp_c8 
*/


/************************Long-term sick or disabled***************************/
/*
Effectively treat disabled/long-term sick as a mutually exclusive activity 
status.
*/
gen dlltsd = 0
replace dlltsd = 1 if jbstat == 8
sort idperson swv 
replace dlltsd = 1 if missing(jbstat) & l.jbstat == 8
//replace dlltsd = 1 if missing(jbstat) & missing(l.jbstat) & l2.jbstat == 8
la var dlltsd "DEMOGRAPHIC: LT sick or disabled"


//check if in receipt of disability benefits 
/*
fre bendis1 //Income: Disability benefits: Incapacity Benefit
fre bendis2 //Income: Disability benefits: Employment and Support Allowance
fre bendis3 //Income: Disability benefits: Severe Disablement Allowance
fre bendis4 //Income: Disability benefits: Carer's Allowance
fre bendis5 //Income: Disability benefits: Disability Living Allowance
fre bendis6 //Income: Disability benefits: Return to work credit
fre bendis7 //Income: Disability benefits: Attendance Allowance
fre bendis8 //Income: Disability benefits: Industrial Injury Disablement Benefit
fre bendis9 //Income: Disability benefits: War disablement pension
fre bendis10 //Income: Disability benefits: Sickness and Accident Insurance
fre bendis11 //Income: Disability benefits: Universal Credit
fre bendis12 //Income: Disability benefits: Personal Independence Payments
fre bendis13 //Income: Disability benefits: Child Disability Payment
fre bendis14 //Income: Disability benefits: Adult Disability Payment
fre bendis15 //Income: Disability benefits: Pension Age Disability Payment
fre bendis97 //Income: Disability benefits: Any other disability related benefit or payment
*/
gen disben = 0
replace disben = 1 if inlist(1, bendis1, bendis2, bendis3, bendis4, bendis5, bendis6, bendis7, bendis8, bendis9, ///
                             bendis10,  bendis12, bendis13, bendis14, bendis15)
/*Note: exclude bendis11 (Universal credit) as it can be jointly received  and bendis97 (any other) 
bysort swv idhh (idhh): gen hhsize = _N
tab2 hhsize disben
tab2 dlltsd disben */

//second check: disability income based on ficode (disability income is computed in 01_prepare_ukhls_pooled_data)
gen disben2 = (inc_disab>0 & inc_disab<.) 

//select those who report being disabled & in receipt of disability benefits according to both checks  
gen dlltsd01 = (dlltsd==1 | (disben==1 & disben2==1)) 
la var dlltsd01 "DEMOGRAPHIC: LT sick/disabled or receives disability benefits"
//fre dlltsd01
//tab2 dlltsd01 dlltsd


* Check consistency with les_c3
tab dlltsd les_c3 
tab dlltsd les_c4
tab dlltsd01 les_c3 
tab dlltsd01 les_c4


* Impose consistency 
replace dlltsd = -9 if les_c3 == -9 
replace dlltsd01 = -9 if les_c3 == -9 

* Check consistency with les_c4
* Assume mutual exclusivity, retirement and disabled
gen flag_disabled_to_retire = (les_c4 == 4 & dlltsd01 == 1)

lab var flag_disabled_to_retire ///
"FLAG: Replaced disabled status with 0 due to conflict with imposed retirement"

replace dlltsd = 0 if les_c4 == 4	// (1,596 real changes made)
replace dlltsd01 = 0 if les_c4 == 4	// (21,372 real changes made)
	
/*
tab les_c3 les_c4
	
fre dlltsd01 
tab dlltsd01 swv, col 
bys swv: sum dlltsd

tab les_c3 dlltsd01 
tab les_c4 dlltsd01
*/


/*******************Long-term sick or disabled - spouse ***********************/
preserve
keep swv idperson dlltsd dlltsd01
rename idperson idpartner
rename dlltsd dlltsd_sp
rename dlltsd01 dlltsd01_sp
save "$dir_data/temp_dlltsd", replace
restore

merge m:1 swv idpartner using "$dir_data/temp_dlltsd"
la var dlltsd_sp "Partner's long-term sick/disabled"
la var dlltsd01_sp "Partner's long-term sick/disabled or receives disability benefits"
keep if _merge == 1 | _merge == 3
drop _merge
//fre dlltsd_sp


/*******************************Retired***************************************/
gen dlrtrd = 0
replace dlrtrd = 1 if jbstat == 4
sort idperson swv 
// Impute missing values using lagged values up to 13 previous waves
forvalues i = 1/13 {
    replace dlrtrd = 1 if dlrtrd==0 & l`i'.jbstat == 4
	}
la var dlrtrd "DEMOGRAPHIC : Retired"


/*************************Enter retirement*************************************/
sort idperson swv 
cap gen drtren = -9
replace drtren = 0 if l.dlrtrd==0
replace drtren = 1 if dlrtrd==1 & l.dlrtrd==0 
la val drtren dummy
la var drtren "DEMOGRAPHIC: Enter retirement"
//fre drtren


/************************************Not Retired***********************/
gen lesnr_c2 = -9 
replace lesnr_c2 = 1 if les_c3==1  
replace lesnr_c2 = 2 if les_c3==2 | les_c3==3  
lab var lesnr_c2 "Not retired work status"
lab define lesnr_c2 1 "in work" 2 "not in work"
lab val lesnr_c2 lesnr_c2 


/************************Exited parental home*********************************/
/*Generated from fnspid and/or mnspid. 
Only populated if eligable for transition.
1 means that individual no longer lives with a parent (fnspid & mnspid is equal to missing)
when in the previous wave they lived with a parent  (fnspid or mnspid not equal to missing).
NOTE: Leaving the parental home was synchronised with the definition of adult child; 
an individual can leave the parental home unless they are a "responsible adult" (their both parents retired). 
*/
sort idperson swv 
gen dlftphm = -9 
replace dlftphm = 0 if adultchildflag[_n-1] == 1  & idperson == idperson[_n-1] & swv == swv[_n-1] + 1
replace dlftphm = 0 if dag == ${age_leave_parental_home} & adultchildflag == 1 
replace dlftphm = 1 if adultchildflag == 0 & adultchildflag[_n-1] == 1 & idperson == idperson[_n-1]  & swv == swv[_n-1] + 1
lab var dlftphm "DEMOGRAPHIC: Exit the Parental Home"
/*
tab dlftphm swv, col
tab dlftphm stm, col
tab dlftphm dun 
tab dlftphm adultchildflag 
*/
* Correct age for adult child flag 
replace adultchildflag = 0 if dag == ${age_leave_parental_home} - 1


/*********************************Left education*******************************/
sort idperson swv 
gen sedex = -9
replace sedex = 0 if l.jbstat == 7
replace sedex = 1 if jbstat != 7 & l.jbstat == 7
la val sedex dummy
la var sedex "Left education"


/****************************Same-sex partnership*****************************/
gen ssscp = 0 if idpartner>0
replace ssscp = 1 if idpartner>0 & (dgn == dgnsp) & dgn>=0 & dgn<. & dgnsp>=0 & dgnsp<.
la val ssscp dummy
la var ssscp "Same-sex partnership"
//fre ssscp

/****************************Year prior to exiting partnership*****************/
cap gen scpexpy = 0
replace scpexpy = 1 if f.dcpex==1 
replace scpexpy=-9 if swv==14 //Impossible to know for the most recent wave 
la val scpexpy dummy
la var scpexpy "Year prior to exiting partnership"
//fre scpexpy


/*****************************Women aged 18 - 44*******************************/
gen sprfm = 0
replace sprfm = 1 if dgn==0 &  dag >= 18 & dag <= ${age_have_child_max}
lab val sprfm dummy
la var sprfm "Woman in fertility range dummy"


/************************UK General Fertility Rate: From ONS 2019*************/
/*Source: https://www.ons.gov.uk/peoplepopulationandcommunity/birthsdeathsandmarriages/livebirths/datasets/birthsummarytables
for 2023: https://www.ons.gov.uk/peoplepopulationandcommunity/birthsdeathsandmarriages/livebirths/datasets/birthsinenglandandwalesbirthregistrations
*/
gen dukfr =.
replace dukfr=62.5 if stm==2009 
replace dukfr=64.0 if stm==2010
replace dukfr=64.0 if stm==2011
replace dukfr=64.6 if stm==2012
replace dukfr=61.7 if stm==2013
replace dukfr=61.4 if stm==2014
replace dukfr=61.5 if stm==2015
replace dukfr=61.2 if stm==2016
replace dukfr=59.7 if stm==2017
replace dukfr=57.6 if stm==2018
replace dukfr=55.8 if stm==2019
replace dukfr=53.4 if stm==2020
replace dukfr=54.1 if stm==2021
replace dukfr=51.8 if stm==2022
replace dukfr=49.8 if stm==2023
replace dukfr=48.9 if stm>=2024
lab var dukfr "UK General fertility rate (ONS)"
//fre dukfr

save "$dir_data\ukhls_pooled_all_obs_02.dta", replace

 
************************************************************************
* Number of newborn from "newborn" datasets 
************************************************************************
/*This code uses the UKHLS newborn module, where each row directly represents a birth event (not inferred from child age).
Each record corresponds to a child newly reported since the last interview. We exclude BHPS "legacy" infants in wave B to prevent overcounting at the merge.
- It is more conceptually exact – counts actual reported births, not inferred ones.
- No double-counting across waves – each newborn appears only once.
- Handles BHPS transition properly – avoids inflating wave B with pre-existing BHPS babies (note that in original Cara's SAS code 
  all BHPS newborns were dropped which I think shoudn't happen,
  so Cara's version was underestimating number of newborns. 
*/

* Combine newborn files (b–n) into one long-format dataset
clear

local firstwave : word 1 of $UKHLS_panel_waves

* --- Load the first wave ---
use "${dir_ukhls_data}/`firstwave'_newborn.dta", clear
gen swv = "`firstwave'"

* Remove wave prefix from variable names
local prefix = "`firstwave'_"
foreach var of varlist `firstwave'_* {
    local base = subinstr("`var'", "`prefix'", "", .)
    rename `var' `base'
}

* Save as base file
save "${dir_data}/temp_uknbrn.dta", replace

* --- Append remaining waves ---
foreach w of global UKHLS_panel_waves {
    if "`w'" != "`firstwave'" {
        di as text "Appending wave `w'..."
        use "${dir_ukhls_data}/`w'_newborn.dta", clear
        gen swv = "`w'"

        * Remove wave prefix 
        local prefix = "`w'_"
        capture unab prefixed : `w'_*
        if _rc == 0 {
            foreach var of local prefixed {
                local base = subinstr("`var'", "`prefix'", "", .)
                rename `var' `base'
            }
        }

        * Append to the long dataset 
        append using "${dir_data}/temp_uknbrn.dta"
        save "${dir_data}/temp_uknbrn.dta", replace
    }
}
//convert wave number to numeric 
gen swv_num = .
local i = 1
foreach w of global UKHLS_panel_waves {
    local num : word `i' of $UKHLS_panel_waves_numbers
    replace swv_num = `num' if swv == "`w'"
    local ++i
}
drop swv
rename swv_num swv
save "${dir_data}/temp_uknbrn.dta", replace

* Count all genuine newborns (UKHLS + BHPS), excludes BHPS legacy infants in wave B
use "${dir_data}/temp_uknbrn.dta", clear

keep pidp swv memorig lchlv
keep if lchlv == 1

* Define newborn indicator
gen byte nbrn = 0
* UKHLS-origin respondents (memorig = 1, 2, 7, 8):
* Always count their newborns. These are all part of the original or ethnic minority boost samples.
replace nbrn = 1 if inlist(memorig, 1, 2, 7, 8)
* BHPS-origin respondents (memorig = 3, 4, 5, 6):
* The BHPS sample was integrated into UKHLS starting from wave B (2010–2012).
* Infants recorded at that point include "legacy" BHPS babies already born before
* the merge — not genuine new births within the UKHLS observation window.
** To avoid overcounting these legacy infants, we exclude BHPS-origin newborns
* only in their first UKHLS wave (wave B). From wave C onward, BHPS households
* are fully integrated, so new births are genuine new births and should be counted.
replace nbrn = 1 if inlist(memorig, 3, 4, 5, 6) & swv != 2

* Collapse to parent-wave level ==> both parents may report the same child of they are in the same hh
bys pidp swv: egen dchpd = total(nbrn)
label var dchpd "Number of newborn children (UKHLS + BHPS, excl. BHPS legacy infants in wave B)"
bys pidp swv: keep if _n == 1 //(376 observations deleted)
rename pidp idperson
save "${dir_data}/temp_parent_dchpd.dta", replace

* Merge into main person-wave dataset
use "${dir_data}\ukhls_pooled_all_obs_02.dta", clear
merge 1:1 idperson swv using "${dir_data}/temp_parent_dchpd.dta"
keep if _merge ==1 | _merge==3
drop _merge

* After merging: fill missing with 0 
replace dchpd = 0 if missing(dchpd) 
label var dchpd "Number of newborn children (UKHLS + BHPS, excl. BHPS legacy infants in wave B)"

/*check how many hh reported same newborn twice because both parents are respondents
preserve 
* Keep only cases with at least one newborn
keep if dchpd > 0
* Keep only core identifiers and gender
keep idperson idhh swv dgn dchpd
* Count households with both male and female respondents reporting newborns
bysort idhh swv: egen hh_births = total(dchpd>0)
bysort idhh swv: egen men_births = total(dchpd>0 & dgn==1)
bysort idhh swv: egen women_births = total(dchpd>0 & dgn==0)
* Mark households where both genders reported at least one newborn
gen both_parents = (men_births>0 & women_births>0)
* Summarise how common these are
tab men_births
tab women_births
tab both_parents
*No such cases, new births are reported by women only 
restore 
*/

tab dag dchpd if dgn == 0     
/*      
    45	6,085	   14		1		1	6,101 
	46	6,201		12		3		0	6,216 
	47	6,123		3		0		0	6,126 
	48	6,191		2		0		0	6,193 
	49	6,182		0		1		0	6,183 
	50	6,343		0		0		0	6,343 
	51	6,184		1		0		0	6,185 
*/

gen flag_old_mother = (dchpd == 1 & dag > ${age_have_child_max} & dgn == 0)

lab var flag_old_mother "FLAG: Have a new born child above the max fertile age"

replace dchpd = -9 if flag_old_mother == 1

//tab dag dchpd if dgn == 0, row

gen give_birth = (dchpd > 0 & dchpd < 4)

//tab dag give_birth if dgn == 0, col
//hist dag if give_birth == 1 &  dgn == 0


/*****************************In educational age range*************************/
cap gen sedag = 1 if dvage >= 16 & dvage <= 29
replace sedag = 0 if missing(sedag)
la val sedag dummy
la var sedag "Educ age range"


/****************************Partnership duration*****************************/
*idpartner in wave a 
clonevar idpartner_a=a_ppid
la var idpartner_a "Unique cross wave identifier of partner in wave a"

*Interview Date in wave a
cap gen Int_Date_a = mdy(a_intdatm_dv, a_intdatd_dv ,a_intdaty_dv) //interview date in wave a

*lcmarm month of current marriage - waves 1& 6 only 
*lcmary4 year of current marriage- waves 1& 6 only 
*lcmcbm month began cohabiting before current marriage- waves 1& 6 only 
*lcmcby4 year began cohabiting before current marriage- waves 1& 6 only 

*started partnership in wave a 
gen D_Cohab_a = mdy(a_lcmcbm,15,a_lcmcby4) //started cohabitation in wave a
gen D_Mrrg_a = mdy(a_lcmarm,15,a_lcmary4) //started marriage in wave a

*Partnership duration in wave a 
cap gen Cohab_Dur_a = .
replace Cohab_Dur_a = (year(Int_Date_a) - year(D_Cohab_a)) + (month(Int_Date_a) < month(D_Cohab_a))
replace Cohab_Dur_a = (year(Int_Date_a) - year(D_Mrrg_a)) + (month(Int_Date_a) < month(D_Mrrg_a)) if Cohab_Dur_a ==. & D_Mrrg_a!=. 
replace Cohab_Dur_a = 0 if Cohab_Dur_a==.

/*
*Length of cohabitation since last wave;
forvalues i=1/7 {
cap gen chb`i' = currpart`i' /*cohab no. 1: check if current partner*/
cap gen chbms`i' =  lmcbm`i' /*cohab no. 1: month began cohabitation spell*/
cap gen chbys`i' =  lmcby4`i' /*cohab no. 1: year began cohabitation spell*/

  if (chbms`i' ==. | chbms`i'<0) & (chbys`i' !=. & chbys`i'>0)  { //start of month imputation loop
  replace chbms`i'=10
  replace chbms`i'=3 if mod(pno, 2) == 1 
      } //end of month imputation loop 
    
 cap gen D_CChb`i' = mdy(chbms`i', 15, chbys`i') if chb`i'==1 //create date variable represented as the number of days since January 1, 1960
 
} //end of if loop 

cap drop D_CChb
egen D_CChb = rowmax(D_CChb1 D_CChb2 D_CChb3 D_CChb4 D_CChb5 D_CChb6 D_CChb7)
*/

preserve 
keep pidp ppid swv Cohab_Dur_a
replace ppid = . if ppid < 0

xtset pidp swv //Set panel
tsspell ppid //Count spells of having partner with the same id. 
rename _seq partnershipDuration 
replace partnershipDuration = . if ppid == .
//keep if swv == 13
keep swv pidp partnershipDuration 
save "$dir_data/tmp_partnershipDuration", replace
restore

merge 1:1 swv pidp using "$dir_data\tmp_partnershipDuration", keep(1 3) nogen 

gen dcpyy = partnershipDuration if idpartner >0
replace dcpyy = partnershipDuration + Cohab_Dur_a if (idpartner >0 & idpartner==idpartner_a) /*for those with same partner id in wave a*/
la var dcpyy "Years in partnership"
//by swv: fre dcpyy


/**************************OECD equivalence scale*****************************/
//Temporary number of children 0-13 and 14-18 to create household OECD equivalence scale
gen depChild_013 = 1 if (dag >= 0 & dag <= 13) & (pns1pid > 0 | pns2pid > 0) & (depchl_dv == 1)
gen depChild_1418 = 1 if (dag >= 14 & dag <= 18) & (pns1pid > 0 | pns2pid > 0) & (depchl_dv == 1)
bys swv idhh: egen dnc013 = sum(depChild_013)
bys swv idhh: egen dnc1418 = sum(depChild_1418)
drop depChild_013 depChild_1418

//Modified OECD equivalence scale
gen moecd_eq = . 
replace moecd_eq = 1.5 if dhhtp_c4 == 1
replace moecd_eq = 0.3*dnc013 + 0.5*dnc1418 + 1.5 if dhhtp_c4 == 2
replace moecd_eq = 1 if dhhtp_c4 == 3
replace moecd_eq = 0.3*dnc013 + 0.5*dnc1418 + 1 if dhhtp_c4 == 4

//Drop children variables used to calculate moecd_eq as no longer needed
drop dnc013 dnc1418


/************************Income CPI********************************************/ 
/*CPIH INDEX 00: ALL ITEMS 2015=100
CDID	L522
Source dataset ID	MM23
PreUnit	
Unit	Index, base year = 100
Release date	20-03-2024
Next release	17 April 2024
https://www.ons.gov.uk/economy/inflationandpriceindices/timeseries/l522/mm23
*/
gen CPI = .
replace CPI = 0.879 if intdaty_dv == 2009
replace CPI = 0.901 if intdaty_dv == 2010
replace CPI = 0.936 if intdaty_dv == 2011
replace CPI = 0.96  if intdaty_dv == 2012
replace CPI = 0.982 if intdaty_dv == 2013
replace CPI = 0.996 if intdaty_dv == 2014
replace CPI = 1     if intdaty_dv == 2015
replace CPI = 1.01  if intdaty_dv == 2016
replace CPI = 1.036 if intdaty_dv == 2017
replace CPI = 1.06  if intdaty_dv == 2018
replace CPI = 1.078 if intdaty_dv == 2019
replace CPI = 1.089 if intdaty_dv == 2020
replace CPI = 1.116 if intdaty_dv == 2021
replace CPI = 1.205 if intdaty_dv == 2022
replace CPI = 1.286 if intdaty_dv == 2023
replace CPI = 1.329 if intdaty_dv == 2024 
replace CPI = 1.329 if intdaty_dv == 2025 //to update when becomes available  




/**************************** Hourly labour income ***************************************/
/* Used to predict wages for non-working, aligned with working hours and self-reported status */ 

//fimnlabgrs_dv=total monthly labour income gross

* Firstly, compute cross personal employment income 
gen yplgrs = fimnlabgrs_dv 
* impose non-negativity 
gen flag_neg_labour = (yplgrs < 0)
lab var flag_neg_labour "FLAG: negative labour income reported"
//fre yplgrs if  yplgrs <0 
replace yplgrs=0 if yplgrs<0 //obs with negative income (due to negative self-employment income) but many of these are close to zero ==> recode them to zero  
assert yplgrs>=0 
//adjust for inflation
replace yplgrs = yplgrs/CPI
sum yplgrs

* missing labour income 
gen flag_missing_lbr_income = (yplgrs== . & les_c3==1)
lab var flag_missing_lbr_income ///
	"FLAG: missing info for labour income"
	
* Compute hourly earnings 
xtset idperson swv
sort idperson swv 

replace lhw = . if lhw == -9 

gen obs_earnings_hourly = .

replace obs_earnings_hourly = yplgrs/(lhw*4.33) if les_c4 == 1

lab var obs_earnings_hourly ///
	"Observed hourly wages, emp and self-emp"
sum obs_earnings_hourly if les_c3 ==1 & obs_earnings_hourly>0 

* Impose consistency  
replace obs_earnings_hourly = 0 if les_c3 == 2 | les_c3 == 3 
    // Individuals not in employment (inactive or unemployed):
    // hourly earnings set to zero by definition

/* At this point, remaining missing wages occur only among those
 recorded as employed or with missing activity (les_c3 == -9),
and/or due to panel structure issues (first/last observation) */

count if obs_earnings_hourly == .		
    // Total number of missing hourly wage observations

count if obs_earnings_hourly == . & idperson != idperson[_n+1] 	
    // (1) Last observation in the individual's panel:
    // no adjacent year available to construct or impute wages

count if obs_earnings_hourly == . & idperson == idperson[_n+1] & les_c3 == -9  
    // (2) Missing or inconsistent labour market status information:
    // employment reported as missing, so wages cannot be constructed

count if obs_earnings_hourly == . & idperson == idperson[_n+1] & ///
    les_c3 == 1 & swv != swv[_n+1] - 1
    // (3) Panel gap:
    // individual observed in multiple waves, but adjacent year is missing

// All missing-wage cases accounted for by panel structure or missing activity

count if obs_earnings_hourly == 0 & les_c3 == 1 	
    // Zero hourly wages among individuals recorded as employed

count if obs_earnings_hourly == 0 & les_c3 == 1 & idperson == idperson[_n+1]
    // Zero wages where a next-year observation exists for the same individual

count if obs_earnings_hourly == 0 & les_c3 == 1 & ///
    idperson == idperson[_n+1] & ///
    flag_missing_lbr_income[_n+1] == 1
    // (4) Next year labour income information is missing:
    // zero wage likely reflects reporting or data availability issue,
    // rather than true zero earnings

count if obs_earnings_hourly == 0 & les_c3 == 1 & ///
    idperson == idperson[_n+1] & ///
    yplgrs[_n+1] == 0 & ///
    flag_missing_lbr_income[_n+1] == 0
    // (5) Next year reports zero labour income with valid data:
    // consistent zero earnings across adjacent waves

// All zero-wage cases accounted for by data availability or true zero earnings

/*
Missing wage observations:
1- almost all due to being the last observation in individual's panel 
2- missing activity information 
3- missing adjacent observation 

Zero wage observations
4- next year is missing labour income information 
5- next year reports zero labour income 

How to address each case:
- uprate previously reported wages 
- use last years earnings and this years hours
- use next years wages 

- use hot deck imputation
*/ 

* Reset zero wages among employed to missing
replace obs_earnings_hourly = . if obs_earnings_hourly == 0 & les_c3 == 1 

* Flag observations eligible for imputation
gen x = 1 if les_c3 == 1 & obs_earnings_hourly == . 

* Ensure correct panel ordering
xtset idperson swv
sort idperson swv

* Imputation 
/* Note that in EU version, correction is applied for wage growth but I opted against it 
because hourly wages are already adjusted for inflation and survey waves do not correspond cleanly to calendar years, 
making it unclear how to define meaningful real wage growth between observations*/

* 1. Use previous wave wage (same individual, employed in both waves)
replace obs_earnings_hourly = obs_earnings_hourly[_n-1] ///
    if idperson == idperson[_n-1] & ///
       les_c3 == 1 & les_c3[_n-1] == 1 & ///
       obs_earnings_hourly == . 
	   //(6,798 real changes made)

* 2. Use next wave wage (same individual, employed in both waves)
replace obs_earnings_hourly = obs_earnings_hourly[_n+1] ///
    if idperson == idperson[_n+1] & ///
       les_c3 == 1 & les_c3[_n+1] == 1 & ///
       obs_earnings_hourly == . 
	   //(1,077 real changes made)

* 3. Use current-wave earnings and hours (fallback)
replace obs_earnings_hourly = yplgrs/lhw*4.33 ///
    if obs_earnings_hourly == . & ///
       les_c3 == 1 & ///
       yplgrs != 0 & ///
       lhw > 0 
	   //(0 real changes made)

* Flag successful panel-based imputations
gen flag_wage_imp_panel = (x == 1 & obs_earnings_hourly != . )

label var flag_wage_imp_panel ///
    "FLAG: wage imputed using surrounding panel information (no wage growth adjustment)"

count if obs_earnings_hourly == .		
count if obs_earnings_hourly == . & idperson != idperson[_n+1] 	 
count if obs_earnings_hourly == . & idperson == idperson[_n+1] & ///
	les_c3 == -9  
count if obs_earnings_hourly == . & idperson == idperson[_n+1] & ///
	les_c3 == 1 & swv != swv[_n+1] - 1 		
	
count if obs_earnings_hourly == . & les_c3 == 1 & yplgrs[_n+1] == 0 & ///
	flag_missing_lbr_income[_n+1] == 1 & idperson == idperson[_n+1] 
count if obs_earnings_hourly == . & les_c3 == 1 & yplgrs[_n+1] == 0 & ///
	flag_missing_lbr_income[_n+1] == 0 & idperson == idperson[_n+1]	
	
count if obs_earnings_hourl == 0 & les_c3 == 1	// 0
count if obs_earnings_hourl == . & les_c3 == 1	// 1,983
	
* Use hot deck imputation for the remaining missing observations among the 
* working

gen flag_wage_hotdeck = (les_c3 == 1 & missing(obs_earnings_hourly))
lab var flag_wage_hotdeck "FLAG: wage imputed using hotdeck imputation"

* Strata
cap drop ageband 
gen ageband = floor(dag/10)*10
replace ageband = 60 if ageband == 70  
	// group 70+ year olds with 60+ to ensure matches 

cap drop stratum 
egen stratum = group(ageband drgn1 dgn swv)/*, label (stratum, replace)*/  

* Define donor pool
preserve

keep if les_c3 == 1 & obs_earnings_hourly != . 
keep obs_earnings_hourly stratum idperson swv 
bys stratum (idperson swv): gen draw = _n
bys stratum (idperson swv): gen n_donors  = _N
rename obs_earnings_hourly donor_wages
drop idperson swv
save "$dir_data/temp_wages_donors", replace

keep stratum n_donors
bys stratum: keep if _n == 1
save "$dir_data/temp_donorsN", replace

restore

* Attached number of donors in each stratum
merge m:1 stratum using "$dir_data/temp_donorsN", nogen

* Assign random donor 
gen draw = . 

sort stratum idperson swv

by stratum (idperson swv): replace draw = ceil(runiform()*n_donors[1]) if ///
	flag_wage_hotdeck == 1 & n_donors > 0 

* Attach donor	
merge m:1 stratum draw using "$dir_data/temp_wages_donors", ///
	keepusing(donor_wages draw) 

drop if _m == 2 
drop _m
	
replace obs_earnings_hourly = donor_wage if flag_wage_hotdeck == 1 

drop donor_wage ageband stratum dra n_donor

count if obs_earnings_hourly == . & les_c3 == 1 //just 3 obs of working with missing wage left 

* Lagged wage 
xtset idperson swv 

gen l1_obs_earnings_hourly = .

replace l1_obs_earnings_hourly = l.obs_earnings_hourly 
lab var l1_obs_earnings_hourly ///
	"Observed hourly wages, emp and self-emp, t-1"

sum obs_earnings_hourly if les_c3 == 1
sum obs_earnings_hourly if les_c3 == 2
sum obs_earnings_hourly if les_c3 == 3
sum obs_earnings_hourly if les_c3 == -9
sum obs_earnings_hourly if les_c3 == .

 

/********************************Income variables***************************************/
/*All income variables are adjusted for inflation and transformed using the inverse hyperbolic sine transformation. 
This transformation is well suited to highly skewed data, as it helps stabilise variance and improves distributional symmetry.*/


/************************Gross personal employment income ***************************************/
/*Has to be recalculated now to be consitent with hourly wages*/
cap drop yplgrs
gen yplgrs = obs_earnings_hourly *(lhw*4.33) 
//already adjusted for inflation as hourly wages have been adjusted by CPI
assert yplgrs>=0  

//Inverse hyperbolic sine transformation:
gen yplgrs_dv = asinh(yplgrs)
la var yplgrs_dv "Gross personal employment income"

sum yplgrs_dv if les_c3 == 1
sum yplgrs_dv if les_c3 == 2
sum yplgrs_dv if les_c3 == 3
sum yplgrs_dv if les_c3 == -9
sum yplgrs_dv if les_c3 == .


count if  yplgrs_dv==.
//  75,877

fre les_c4 if yplgrs_dv==.
/*
les_c4 -- LABOUR MARKET: Activity status
----------------------------------------------------------------------------------
                                     |      Freq.    Percent      Valid       Cum.
-------------------------------------+--------------------------------------------
Valid   -9                           |      75874     100.00     100.00     100.00
        1  Employed or self-employed |          3       0.00       0.00     100.00
        Total                        |      75877     100.00     100.00           
----------------------------------------------------------------------------------
*/

assert yplgrs_dv==0 if les_c4!=1 & les_c4>0
assert obs_earnings_hourly==0 if les_c4!=1 & les_c4>0

/**********************Gross personal non-benefit income *********************************/
/*Note: This is supposed to mirror UKMOD market income 

1/ fimnlabgrs_dv: total personal monthly labour income gross ==> use temp_yplgrs instead 
2/ fimnpen_dv: monthly amount of net pension income	
   This includes receipts reported in the income data file where w_ficode equals  
   [2] “a pension from a previous employer”, or 
   [3] “a pension from a spouse’s previous employer”. This is assumed to be reported net of tax. 
3/ fimnmisc_dv: monthly amount of net miscellaneous income. This includes receipts reported in the income data file where w_ficode equals 
   [24] "educational grant (not student loan or tuition fee loan)", ==> needs to be removed from market income 
   [27] "payments from a family member not living here", or 
   [38] "any other regular payment (not asked in Wave 1)". This is assumed to be reported net of tax. 
4/ variables “inc_stp” “inc_tu” “inc_ma” are generated in the UK  do-file called “01_prepare_ukhls_pooled_data” 
   gen inc_stp = frmnthimp_dv if ficode == 1 (NI Retirement/State Retirement (Old Age) Pension) ==> needs to be removed from market income  
   gen inc_tu = frmnthimp_dv if ficode == 25 (Trade Union / Friendly Society Payment)
   gen inc_ma = frmnthimp_dv if ficode == 26 (Maintenance or Alimony)
Instead of (3) and (4) , use :  

gen inc_pp = frmnthimp_dv if ficode == 4 //A Private Pension/Annuity
gen inc_tu = frmnthimp_dv if ficode == 25 //Trade Union / Friendly Society Payment
gen inc_ma = frmnthimp_dv if ficode == 26 //Maintenance or Alimony
gen inc_fm = frmnthimp_dv if ficode == 27 //payments from a family member not living here
gen inc_oth = frmnthimp_dv if ficode == 38 //any other regular payment (not asked in Wave 1)
*/

cap drop temp_yplgrs
gen temp_yplgrs = obs_earnings_hourly *(lhw*4.33)*CPI //go back to unadjusted level 
assert temp_yplgrs>=0  
sum yplgrs
sum temp_yplgrs

recode temp_yplgrs fimnpen_dv inc_pp inc_tu inc_ma inc_fm inc_oth (-9=.) (-1=.)

egen ypnb = rowtotal(temp_yplgrs fimnpen_dv inc_pp inc_tu inc_ma inc_fm inc_oth) //Gross personal non-benefit income
fre ypnb if  ypnb <0 //obs with negative income (due to negative self-employment income) but many of these are close to zero ==> recode them to zero 
replace ypnb=0 if  ypnb <0 
sum ypnb 
assert ypnb>=0 
//adjust for inflation
replace ypnb = ypnb/CPI

//Inverse hyperbolic sine transformation:
gen ypnbihs_dv = asinh(ypnb)
la var ypnbihs_dv "Gross personal non-benefit income"


/********************Gross personal non-employment, non-benefit income***************************/
egen yptc = rowtotal(fimnpen_dv inc_pp inc_tu inc_ma inc_fm inc_oth) //Gross personal non-employment, non-benefit income
assert yptc>=0 
//adjust for inflation:
replace yptc = yptc/CPI

//Inverse hyperbolic sine transformation:
gen yptciihs_dv = asinh(yptc)
la var yptciihs_dv "Gross personal non-employment, non-benefit income"

	
/*********************Gross personal non-benefit income for a spouse *******************************/
//tempfile temp_ypnb
preserve
keep swv idperson idhh ypnb
rename ypnb ypnbsp
rename idperson idpartner
save "$dir_data\temp_ypnb", replace
restore
merge m:1 swv idpartner idhh using "$dir_data\temp_ypnb"
keep if _merge == 1 | _merge == 3
drop _merge
//adjust for inflation 
replace ypnbsp = ypnbsp/CPI

//Inverse hyperbolic sine transformation:
gen ypnbihs_dv_sp = asinh(ypnbsp)

/************************************Household income ********************************************/
//Household income is sum of individual income and partner's income if coupled
//If single, household income is equal to individual income
egen yhhnb = rowtotal(ypnb ypnbsp) if dhhtp_c4 == 1 | dhhtp_c4 == 2 
replace yhhnb = ypnb if dhhtp_c4 == 3 | dhhtp_c4 == 4 
//equivalise and adjust for inflation:
replace yhhnb = (yhhnb/moecd_eq)/CPI

//Inverse hyperbolic sine transformation:
gen yhhnb_asinh = asinh(yhhnb)


/********************************Disposable income ************************************************/
sum fimnnet_dv fimnlabgrs_dv temp_yplgrs
misstable summarize fimnnet_dv fimnlabgrs_dv temp_yplgrs 

fre fimnlabgrs_dv if fimnnet_dv==.
/*
fimnlabgrs_dv -- total monthly labour income gross
-----------------------------------------------------------
              |      Freq.    Percent      Valid       Cum.
--------------+--------------------------------------------
Missing .     |     253176     100.00                      
-----------------------------------------------------------
*/
fre temp_yplgrs if fimnnet_dv==.
/*
temp_yplgrs
-------------------------------------------------------
          |      Freq.    Percent      Valid       Cum.
----------+--------------------------------------------
Valid   0 |     177763      70.21     100.00     100.00
Missing . |      75413      29.79                      
Total     |     253176     100.00                      
-------------------------------------------------------
*/

sort hidp
gen ydisp = fimnnet_dv - fimnlabgrs_dv + temp_yplgrs //deduct reported gross labour income , add adjusted gross labour income 
sum fimnnet_dv ydisp

//residual income 
recode ydisp (missing = 0)
by hidp: egen hhinc = sum(ydisp)
gen res = fihhmnnet1_dv - hhinc

/*Adults get residual household income first.
If no adults with missing income, allocate to teens.
*/
gen mis = (fimnnet_dv>=.)*(age_dv>17.5)
by hidp: egen nmis = sum(mis)
replace ydisp = res / nmis if (res>0.1 & res<. & mis==1)

drop hhinc res mis nmis
by hidp: egen hhinc = sum(ydisp)
gen res = fihhmnnet1_dv - hhinc

gen mis = (fimnnet_dv>=.)*(age_dv>14.5)*(age_dv<17.5)
by hidp: egen nmis = sum(mis)
replace ydisp = res / nmis if (res>0.1 & res<. & mis==1)
recode ydisp (missing=0)

//adjust for inflation 
replace ydisp = ydisp/CPI
la var ydisp "Disposable income (individual)"

/* checks 
by hidp: egen hhinc2 = sum(ydisp)
gen res2 = fihhmnnet1_dv - hhinc2
gen chk = (abs(res2) > 0.1)*(fihhmnnet1_dv<.)
order age_dv fihhmnnet1_dv fimnnet_dv ydisp hhinc res res2 mis nmis chk, a(hidp)
tab chk
drop hhinc2 res2 chk
*/
drop hhinc res mis nmis


/*****************************Household income Quintiles ***************************************/
/*Problem: if many observations in yhhnb_asinh have exactly the same value, xtile would group them into a single quintile, 
causing one or more quintiles to have very few observations. 
This results in 2nd quintile being extremely small compared to the first quintile, which probably has many similar values 
Adding a very small random amount to yhhnb_asinh can help differentiate tied values enough to distribute them more evenly 
across quintiles without distorting the data meaningfully.
*/
//sum yhhnb_asinh
gen yhhnb_asinh_jittered = yhhnb_asinh + runiform() * 1e-5

cap drop ydses*
forvalues stm=2009/2024 {
xtile ydses_c5_`stm' = yhhnb_asinh_jittered if depChild != 1 & stm==`stm', nq(5)
bys idhh: egen ydses_c5_tmp_`stm' = max(ydses_c5_`stm') if stm==`stm'
replace ydses_c5_`stm' = ydses_c5_tmp_`stm' if missing(ydses_c5_`stm')
drop ydses_c5_tmp_`stm'
} 
egen ydses_c5 = rowtotal(ydses_c5_2009 ydses_c5_2010 ydses_c5_2011 ydses_c5_2012 ydses_c5_2013 ydses_c5_2014 ydses_c5_2015 ydses_c5_2016 ydses_c5_2017 ydses_c5_2018 ydses_c5_2019 ///
ydses_c5_2020 ydses_c5_2021 ydses_c5_2022 ydses_c5_2023 ydses_c5_2024)
recode ydses_c5 (0=-9) 
drop ydses_c5_2*
la var ydses_c5 "Household income quintiles"

//bys stm: fre ydses_c5
//fre ydses_c5


/********************Difference between own and spouse's gross personal non-benefit income*********************/
//gen ynbcpdf_dv = asinh(sinh(ypnbihs_dv) - sinh(ypnbihs_dv_sp))
//Keep as simple difference between the two for compatibility with estimates
gen ynbcpdf_dv = ypnbihs_dv - ypnbihs_dv_sp
recode ynbcpdf_dv (.=-999) if idpartner <0
recode ynbcpdf_dv (.=-999) 
la var ynbcpdf_dv "Difference between own and spouse's gross personal non-benefit income"
//sum ynbcpdf_dv 

/**************************Gross-to-net ratio*********************************************/  
gen gross_net_ratio = fimngrs_dv/fimnnet_dv 
replace gross_net_ratio = 1 if missing(gross_net_ratio) 
replace gross_net_ratio = 0 if gross_net_ratio<0 


/********************************Gross personal capital income*******************************/
/* Assumed to be reported net of tax: 
1/ fimninvnet_dv: investment income
2/ fimnmisc_dv: net miscellaneous income. [24] educational grant (not student loan or tuition fee loan), [27] payments from a family member not living here, or [38] any other regular payment (not asked in Wave 1).
3/ fimnprben_dv: net private benefit income. [25] trade union / friendly society payment, [26] maintenance or alimony, or [35] sickness and accident insurance.  
Instead of (2), use :  
gen inc_fm = frmnthimp_dv if ficode == 27 //payments from a family member not living here
gen inc_oth = frmnthimp_dv if ficode == 38 //any other regular payment (not asked in Wave 1)
*/
recode fimninvnet_dv fimnprben_dv inc_fm inc_oth (-1=.) (-9=.)
egen ypncp_temp = rowtotal (fimninvnet_dv inc_fm inc_oth fimnprben_dv) 
assert ypncp_temp>=0
//adjust for inflation and gross up 
gen ypncp_lvl = ypncp_temp/CPI*gross_net_ratio
lab var ypncp_lvl "Gross personal non-employment capital income (GBP)"
//Inverse hyperbolic sine transformation  
gen ypncp = asinh( ypncp_lvl) 
lab var ypncp "Gross personal non-employment capital income (IHS)"
//sum ypncp ypncp_lvl


/****************************** Private pension income **************************************/
/*fimnpen_dv: monthly amount of net pension income
inc_pp = frmnthimp_dv if ficode == 4 //A Private Pension/Annuity	 
*/
egen ypnoab_temp = rowtotal(fimnpen_dv inc_pp) 
assert ypnoab_temp>=0 
//adjust for inflation and gross up 
gen ypnoab_lvl= ypnoab_temp/CPI*gross_net_ratio
//Inverse hyperbolic sine transformation  
gen ypnoab = asinh( ypnoab_lvl)
lab var  ypnoab_lvl "Gross personal private pension income"
lab var  ypnoab "Gross personal private pension income"
//sum ypnoab ypnoab_lvl

/******************************Home ownership dummy***************************/
*Dhh_owned is the definition used in the initial population and in the model predicting house ownership 
*in the homeownership process of the simulation.
bys swv: fre hsownd
gen dhh_owned=0 
replace dhh_owned=1 if hsownd>=1 & hsownd<=3 
lab var dhh_owned "Home ownership dummy"


/******************************Disability benefit*****************************/
/*If any of bendis1-bendis3, bendis5-bendis12, or bendis97 = 1 then received benefits

Availability of vars across waves: 

bendis2	Employment and Support Allowance	indresp	12345678910111213
bendis3	Severe Disablement Allowance	indresp	12345678910111213

bendis5	Disability Living Allowance	indresp	12345678910111213
bendis6	return to work credit?	indresp	12345
bendis7	Attendance Allowance	indresp	12345678910111213
bendis8	Industrial Injury Disablement Benefit	indresp	12345678910111213
bendis9	war disablement pension?	indresp	12345
bendis10	Sickness and Accident Insurance	indresp	12345678910111213
bendis11	mc universal credit	indresp	1245
bendis12	Personal Independence Payments	indresp	345678910111213

bendis97	Any other disability related benefit or payment	indresp	345678910111213
*/
gen bdi = 0 
foreach var in 2 3 5 6 7 8 9 10 11 12 97 { 
replace bdi = 1 if bendis`var' == 1
}
la val bdi dummy
la var bdi "Disability benefits"


/******************************Unemployment dummy***************************/
gen unemp = (jbstat==3)
replace unemp = -9 if les_c3 == -9
replace unemp = -9 if dag < $age_seek_employment 

lab var unemp "Unemployed dummy"

replace unemp = -9 if les_c3 == -9 
/* 
fre unemp
tab unemp swv, col 
bys swv: sum unemp
*/

* Check consistency 
tab unemp les_c3 
tab unemp les_c4 

* Impose consistency with retirement 
gen flag_unemp_to_retire = (les_c4 == 4  & unemp == 1)

lab var flag_unemp_to_retire ///
	"FLAG: Replaced unemployed with 0 due to retirement status enforcement"

replace unemp = 0 if les_c4 == 4 & unemp == 1 	// (440 real changes made)
/*
tab unemp dlltsd01
tab unemp les_c3 
tab unemp les_c4 
*/


/***************************** UC and Non-UC receipt ***********************/

gen econ_benefits = .
replace econ_benefits = 1 if fihhmnsben_dv > 0 & fihhmnsben_dv!=.
replace econ_benefits = 0 if fihhmnsben_dv==0
label var econ_benefits "Household income includes any benefits"

replace benefits_uc=0 if benefits_uc==.
* Ensure all with known UC receipt also are benefit recipients
replace econ_benefits=1 if benefits_uc==1

* Generate benefits marker without UC
gen econ_benefits_nonuc=econ_benefits
replace econ_benefits_nonuc=0 if benefits_uc==1
label var econ_benefits_nonuc "Household income includes non-UC benefits"

* Generate benefits marker with UC
gen econ_benefits_uc=econ_benefits
replace econ_benefits_uc=0 if benefits_uc==0
label var econ_benefits_uc "Household income includes UC benefits"

gen econ_benefits_lb=benefits_lb
replace econ_benefits_lb=0 if benefits_lb==.
replace econ_benefits_lb=0 if econ_benefits_uc==1
label var econ_benefits_lb "Household income includes Legacy Benefits"


/***************************** Financial Distress ***************************************************************************/
// This is a measure of subjective financial distress, corresponding to answering 4 or 5 to the question below:
// How well would you say you yourself are managing financially these days? Would you say you are...
// 1. Living comfortably
// 2. Doing alright
// 3. Just about getting by
// 4. Finding it quite difficult
// 5. Finding it very difficult

recode finnow (1 2 3 = 0) (4 5 = 1) (else = .), gen(financial_distress)
lab var financial_distress "DEMOGRAPHIC: Financial Distress"

// Impute financial distress when missing
preserve
drop if dgn < 0 | dag < 0 | dhe < 0 | drgn1 < 0 | unemp < 0 
eststo predict_financial_distress: logit financial_distress c.dag i.dgn i.drgn1 i.swv i.dhe c.dls i.unemp i.dhh_owned c.yhhnb_asinh, vce(robust)
restore
estimates restore predict_financial_distress
predict financial_distress_prediction

replace financial_distress = 1 if missing(financial_distress) & financial_distress_prediction >= 0.5
replace financial_distress = 0 if missing(financial_distress) & financial_distress_prediction < 0.5


/*****************Was in continuous education sample***************************/
//Generated from age_dv and ded variables. 1 includes first instance of not being in education.
/*This variable is created in Cara’s SAS file in the following way: 
if 16 le age_dv le 29 then do;
	if inEducStart = 1 and EDU_End in (0,1) then sedcsmpl = 1;
end;
if sedcsmpl = . then sedcsmpl = 0;
*/
/* sample: being observed in education in all preceding periods t-1,t-2,t-n”?, where n is the number of observations of a particular individual we have*/
sort idperson swv 
cap gen sedcsmpl=0
replace sedcsmpl = 1 if (dag>=16 & dag<=29) & l.ded==1 /*was in continious education in the previous wave  */
lab var sedcsmpl "SYSTEM: Continuous education sample"
lab define sedcsmpl  1 "Aged 16-29 and were in continuous education"	
lab values sedcsmpl sedcsmpl


/**********************Return to education sample*****************************/
//Generated from age_dv and drtren 
gen sedrsmpl =0 
replace sedrsmpl = 1 if (dag>=16 & dag<=35 & ded==0) 
lab var sedrsmpl "SYSTEM : Return to education sample"
lab define  sedrsmpl  1 "Aged 16-35 and not in continuous education"
lab values sedrsmpl sedrsmpl


/**********************In Continuous education sample*************************/
//Generated from sedcsmpl and ded variables. Sample: Respondents who were in continious education and left it. 
cap gen scedsmpl = 0 
replace scedsmpl=1 if sedcsmpl==1 & ded == 0 /*were but currently not in continuous full-time education*/
lab var scedsmpl "SYSTEM : Not in continuous education sample"
lab define  scedsmpl  1 "Left continuous education"
lab values scedsmpl scedsmpl


/*****************************Weights*****************************************/
/*dimlwt	indinus_l DEMOGRAPHIC : Individual Longitudinal Weight - Main survey	
Longitudinal individual main survey weight from indinus_lw (waves 2 onward) variable*/
gen dimlwt = indinus_lw 
recode dimlwt (.=0)
lab var dimlwt "DEMOGRAPHIC : Individual Longitudinal Weight - Main survey"
/*
disclwt	indscus_lw	DEMOGRAPHIC : Individual Longitudinal Weight - Self-Completion	
Longitudinal individual self-completion weight from indscus_lw (waves 2 onward) variable.
*/
gen disclwt = indscus_lw 
recode disclwt (.=0)
lab var disclwt "DEMOGRAPHIC : Individual Longitudinal Weight - Self-Completion"
/*dimxwt	indpxub_xw; indpxui_xw	DEMOGRAPHIC : Individual Cross-sectional Weight - Main survey	
Cross-sectional individual main survey weight from indpxub_xw (waves 2-5) and indpxui_xw (waves 6-13) variables
*/
gen dimxwt = indpxub_xw
replace dimxwt = indpxui_xw if missing(dimxwt)
replace dimxwt = indpxg2_xw if missing(dimxwt)
lab var dimxwt "DEMOGRAPHIC : Individual Cross-sectional Weight - Main survey"	
/*dhhwt	hhdenub_xw; hhdenui_xw	DEMOGRAPHIC : Household Cross-sectional Weight	
Cross-sectional household weight from hdenub_xw (waves 2-7) and hhdenui_xw (waves 6-13)
*/
gen dhhwt = hhdenub_xw
replace dhhwt = hhdenui_xw if missing(dhhwt)
replace dhhwt = hhdeng2_xw if missing(dhhwt)
lab var dhhwt "DEMOGRAPHIC : Household Cross-sectional Weight"	


/*********************** household level weight *******************************/
clonevar dwt= dhhwt 
bys swv idhh: egen max_dwt = max(dhhwt )
replace dwt = max_dwt if missing(dhhwt )
replace dwt = 0 if missing(dwt)


/*********************** EDUCATION STATUS - IMPUTATION 2 **********************/
/* Implemented as in EU models: 
At the point missing education level for those that transition out of
education or have all missing observations. */
fre deh_c3

gen orig_deh = deh_c3

* Investigate characterisitcs - are missing observations plausibly random?
gen missing_edu = (deh_c4 == -9)

recode dgn dag dagsq drgn1 les_c4 dcpst ydses_c5 (-9 = .), ///
	gen (dgn2 dag2 dagsq2 drgn12 les_c42 dcpst2 ydses_c52)
fre dgn2 dag2 dagsq2 drgn12

logit missing_edu i.dgn2 dag2 dagsq ib3.drgn12 i.swv i.les_c42 i.dcpst2 ///
	i.ydses_c52 if dag > 16 

predict p_miss
kdensity p_miss if missing_edu == 1, ///
	addplot(kdensity p_miss if missing_edu == 0)

/* Overlap is good => supports match, but shape is different suggesting that 
ppl missing education cluster at covaraiate combinations that produce higher
probability of missing than observations for which we observe education */

* Generte adjusted weight 
gen p_obs = 1 - p_miss

gen ipw = 1/p_obs if p_obs < . 

* Create addition controls 
gen les_c43 = les_c4 
replace les_c43 = 5 if les_c43 == -9 
fre les_c43

sort idperson swv 
gen l_les_c43 = les_c43[_n-1] if idperson == idperson[_n-1]
replace l_les_c43 = 5 if idperson != idperson[_n-1]

gen exit_edu = 0 
replace exit_edu = 1 if idperson == idperson[_n-1] & les_c3[_n-1] == 2 & ///
	les_c3 != 2 & les_c3 != -9 

gen ydses_c53 = ydses_c5
replace ydses_c53 = 6 if ydses_c53 < 0 

* Generalized ordered probit - estimate on those that have left their initial 
* education spell 
gologit2 deh_c3 i.dgn2 dag2 dagsq i.drgn12 swv i.les_c43 i.exit_edu i.dcpst ///
	i.ydses_c53 if deh_c3 != -9 & dag >= 16 & ded == 0 ///
	[pweight = ipw]
	//, autofit 

predict p1 p2 p3

* Create CDF
gen p1p2 = p1 + p2 

sort idperson swv

* Add heterogenity
set seed 123567
gen rnd = runiform() 

* Create imputation
gen imp_deh_pred = cond((rnd < p1), 1, cond(rnd < p1p2, 2, 3))

* Inspection 

* Predicting high education  
twoway ///
    (kdensity p1 if deh_c3 == 1, lcolor(red)) ///
    (kdensity p1 if deh_c3 == 2, lcolor(blue)) ///
    (kdensity p1 if deh_c3 == 3, lcolor(green)) ///
    , title("Density of p1 by true category")

* Predicting medium education  
twoway ///
    (kdensity p2 if deh_c3 == 1, lcolor(red)) ///
    (kdensity p2 if deh_c3 == 2, lcolor(blue)) ///
    (kdensity p2 if deh_c3 == 3, lcolor(green)) ///
    , title("Density of p2 by true category")


* Predicting low education  
twoway ///
    (kdensity p3 if deh_c3 == 1, lcolor(red)) ///
    (kdensity p3 if deh_c3 == 2, lcolor(blue)) ///
    (kdensity p3 if deh_c3 == 3, lcolor(green)) ///
    , title("Density of p3 by true category")

graph drop _all 

foreach k in 1 2 3 {
	
    sum p`k' if deh_c3 == `k'

}


* Impute 
cap drop missing_edu 
gen missing_edu = (deh_c3 == -9)

* All missing
cap drop missing_count
bysort idperson (swv): egen missing_count = sum(missing_edu)
bysort idperson (swv): gen all_missing = 1 if missing_count[_N] == count[_N]

* Populate
gen imp_deh_all = deh_c3 if deh_c3 != -9 

* Impose monotonicity on those with all observations missing 

* Populate first observation with predicted value 
replace imp_deh_all = imp_deh_pred if imp_deh_all == . & count == 1 & ///
	all_missing == 1 
	
* get max number of waves in the data
//bysort idperson (swv): gen count = _n
summ count, meanonly
local maxwaves = r(max)

sort idperson swv 
	
forvalues i = 2/`maxwaves' {

	* Carry forward education if remain a student 
	replace imp_deh_all = imp_deh_all[_n-1] if imp_deh_all == . & ///
		count == `i' & non_student[_n-1] == 0 & non_student == 0 & ///
		all_missing == 1 & idperson == idperson[_n-1]
	
	* Carry forward education if remain a non_student 
	replace imp_deh_all = imp_deh_all[_n-1] if imp_deh_all == . & ///
		count == `i' & non_student[_n-1] == 1 & non_student == 1 & ///
		all_missing == 1 & idperson == idperson[_n-1]	
	
	* Carry forward education if become a student 
	replace imp_deh_all = imp_deh_all[_n-1] if imp_deh_all == . & ///
		count == `i' & non_student[_n-1] == 1 & non_student == 0 & ///
		all_missing == 1 & idperson == idperson[_n-1]	
	
	* Transition out of eduction - min rule 
	* Lagged 
	replace imp_deh_all = imp_deh_all[_n-1] if imp_deh_all == . & ///
		count == `i' & non_student[_n-1] == 0 & non_student == 1 & ///
		all_missing == 1 & imp_deh_all[_n-1] <= imp_deh_pred & ///
		idperson == idperson[_n-1]
		
	* Predcited	
	replace imp_deh_all = imp_deh_pred if imp_deh_all == . & ///
		count == `i' & non_student[_n-1] == 0 & non_student == 1 & ///
		all_missing == 1 & imp_deh_all[_n-1] > imp_deh_pred	& ///
		idperson == idperson[_n-1]
		
}		

				
* Those with some missing observations simply impose monotocity accounting 
* whilst imposing a cap on educaiton level using any future observed level

* Next highest observation variable to enforce consistency 
gsort idperson -count 

cap gen next_max_deh = imp_deh_all 
replace next_max_deh = next_max_deh[_n-1] if idperson == idperson[_n-1] & ///
	next_max_deh == . 

sort idperson count 

* If no more future observations set to zero 
replace next_max_deh = 0 if next_max_deh == . 


* First observation 

* Use predicted value if predicts lower edu level that in the future 
replace imp_deh_all = imp_deh_pred if imp_deh_all == . & count == 1 & ///
	next_max_deh <= imp_deh_pred 
	
* Use next observed max edu level if lower than predicted 	
replace imp_deh_all = next_max_deh if imp_deh_all == . & count == 1 & ///
	next_max_deh > imp_deh_pred & next_max_deh != . 


* Later observations 
summ count, meanonly
local maxwaves = r(max)	

forvalues i = 2/`maxwaves' {	
	
	replace imp_deh_all = imp_deh_pred if imp_deh_all == . & count == `i' & ///
		next_max_deh <= imp_deh_pred & imp_deh_pred <= imp_deh_all[_n-1]
		
	replace imp_deh_all = imp_deh_all[_n-1] if imp_deh_all == . & ///
		count == `i' & next_max_deh <= imp_deh_all[_n-1] & ///
		imp_deh_all[_n-1] <= imp_deh_pred 
		
	replace imp_deh_all = imp_deh_all[_n-1] if imp_deh_all == . & ///
		count == `i' & imp_deh_pred <= next_max_deh & ///
		next_max_deh <= imp_deh_all[_n-1]  
		
	replace imp_deh_all = next_max_deh if imp_deh_all == . & count == `i' 
		
}

count if imp_deh_all == . 
count if imp_deh_all == -9 

count if idperson == idperson[_n-1] & imp_deh_all > imp_deh_all[_n-1]  

* All due observatsions breaking the monotoncity rule are due to inconsistencies
* in the raw data 
gen flag_deh_imp_reg = (deh_c3 == -9 & imp_deh_all != .)
lab var flag_deh_imp_reg "FLAG: -1, if education imputed using gologit"
fre flag_deh_imp_reg

 
* Impute remaining missing values 
replace deh_c3 = imp_deh_all if deh_c3 == -9 
replace deh_c4 = imp_deh_all if deh_c4 == -9 

count if deh_c3 == -9 	// 0 
count if deh_c4 == -9 	// 0 


* Distributions
twoway ///
    (histogram orig_deh if orig_deh > 0, discrete percent ///
        fcolor(blue) lcolor(blue) barwidth(0.8)) ///
    (histogram deh_c3 if deh_c3 > 0, discrete percent ///
        fcolor(none) lcolor(red) lwidth(thick) barwidth(0.8)), ///
    legend(order(1 "Observed" 2 "Final distribution")) ///
    title("Observed and final education distributions") ///
    xlabel(1 "High" 2 "Medium" 3 "Low") ///
    graphregion(color(white))

graph drop _all 	
	
drop dgn2 dag2 dagsq2 drgn12 les_c42 dcpst2 ydses_c52 p1* p2 p3 rnd imp_deh*


/******************** UPDATE PARTNER'S EDUCATION STATUS ***********************/
preserve

keep swv idperson deh_c3 deh_c4 flag_deh_imp_mono flag_deh_imp_reg

rename idperson idpartner
rename deh_c3 dehsp_c3 
rename deh_c4 dehsp_c4
rename flag_deh_imp_mono flag_dehsp_imp_mono
rename flag_deh_imp_reg flag_dehsp_imp_reg

save "$dir_data/temp_dehsp", replace

restore

merge m:1 swv idpartner using "$dir_data/temp_dehsp"

lab var dehsp_c3 "Education status partner"
lab var dehsp_c4 "Education status partner"
	
keep if _merge == 1 | _merge == 3
drop _merge
/*
fre dehsp_c3 if idpartner > 0 
tab dehsp_c3 swv, col
bys swv: sum dehsp_c3 if dehsp_c3 > 0 

fre dehsp_c4 if idpartner > 0 
tab dehsp_c4 swv, col
bys swv: sum dehsp_c4 if dehsp_c4 > 0 
*/
sort idperson swv 




/*************************** CONSISTENCY CHECKS *******************************/
* Economic activity 
tab les_c3 les_c4 
tab dag if les_c3 == 2 
count if les_c3 == . 
count if les_c4 == . 

tab les_c3 ded
tab les_c4 ded

tab les_c3 der
tab les_c4 der

tab les_c3 non_student 
tab les_c4 non_student 

tab ded der 
tab ded non_student 

sum lhw if les_c3 == 1
sum lhw if les_c3 != 1
sum lhw if les_c4 == 1
sum lhw if les_c4 != 1

tab les_c3 dlltsd01
tab les_c4 dlltsd01

tab les_c3 dlrtrd
tab les_c4 dlrtrd

tab les_c3 sedex
tab les_c4 sedex

tab les_c3 unemp 
tab les_c4 unemp 


* Partnership 
tab dun dcpst

gen temp_idp_pop = (idpartner> -9)

tab dun temp_idp_pop 
tab dcpst temp_idp_pop 

* Fertility 


* Education 
tab ded deh_c3
tab ded deh_c4 

tab deh_c3 deh_c4


/**************************** SENSE CHECK PLOTS *******************************/

do "$dir_do/02_01_checks"

graph drop _all 


/*********************** CREATE ASSUMPTION DESCRIPTIVES  **********************/

* Health imputation 
tab flag_dhe_imp if dag >= 16, matcell(freq) matrow(names)

scalar total = r(N)

matrix percent = (freq/total)*100

putexcel set "$dir_work/flag_descriptives", sheet("UK") replace
putexcel B1 = ("Count") C1 = ("Percent") D1 = ("Sample")
putexcel A2 = ("Health imputed using ordered probit")
putexcel A3 = matrix(names) B3 = matrix(freq) C3 = matrix(percent) 
putexcel D3 = ("16+")

* Health imputation partner 
tab flag_dhesp_imp if dag >= 16 & idpartner > 0, matcell(freq) matrow(names)

scalar total = r(N)

matrix percent = (freq/total)*100

putexcel set "$dir_work/flag_descriptives", sheet("UK") modify
putexcel A5 = ("Partner's health imputed")
putexcel A6 = matrix(names) B6 = matrix(freq) C6 = matrix(percent) 
putexcel D6 = ("16+, has a partner")

* Report retiring too young  
tab flag_no_retire_young if dag >= 16 & dag < 50, matcell(freq) matrow(names)

scalar total = r(N)

matrix percent = (freq/total)*100

putexcel set "$dir_work/flag_descriptives", sheet("UK") modify
putexcel A8 = ("Report being retired too young")
putexcel A9 = matrix(names) B9 = matrix(freq) C9 = matrix(percent) 
putexcel D9 = ("16-49")

* Forced to remain retired 
tab flag_retire_absorb if dag >= 50, matcell(freq) matrow(names)

scalar total = r(N)

matrix percent = (freq/total)*100

putexcel set "$dir_work/flag_descriptives", sheet("UK") modify
putexcel A11 = ("Forced to remain retired")
putexcel A12 = matrix(names) B12 = matrix(freq) C12 = matrix(percent) 
putexcel D12 = ("50+")

* Force into retirement 
tab flag_retire_force if dag >= 75, matcell(freq) matrow(names)

scalar total = r(N)

matrix percent = (freq/total)*100

putexcel set "$dir_work/flag_descriptives", sheet("UK") modify
putexcel A15 = ("Forced into retirement")
putexcel A16 = matrix(names) B16 = matrix(freq) C16 = matrix(percent) 
putexcel D16 = ("75+")

*  Replaced > 0 hours of work with 0 as report not-employed
tab flag_impose_zero_hours_ne if dag >= 16 & dag < 75, matcell(freq) ///
	matrow(names)

scalar total = r(N)

matrix percent = (freq/total)*100

putexcel set "$dir_work/flag_descriptives", sheet("UK") modify
putexcel A18 = ("Replaced >0 hours of work with 0 as report not-employed")
putexcel A19 = matrix(names) B19 = matrix(freq) C19 = matrix(percent) 
putexcel D19 = ("16-75")

*  Replaced > 0 hours of work with 0 as report retired
tab flag_impose_zero_hours_retire if dag >= 16 & dag < 75, matcell(freq) ///
	matrow(names)

scalar total = r(N)

matrix percent = (freq/total)*100

putexcel set "$dir_work/flag_descriptives", sheet("UK") modify
putexcel A21 = ("Replaced >0 hours of work with 0 as report retired")
putexcel A22 = matrix(names) B22 = matrix(freq) C22 = matrix(percent) 
putexcel D22 = ("16-75")

*  Replaced > 0 hours of work with 0 as report student
tab flag_impose_zero_hours_student if dag >= 16 & dag < 75, matcell(freq) ///
	matrow(names)

scalar total = r(N)

matrix percent = (freq/total)*100

putexcel set "$dir_work/flag_descriptives", sheet("UK") modify
putexcel A24 = ("Replaced >0 hours of work with 0 as report student")
putexcel A25 = matrix(names) B25 = matrix(freq) C25 = matrix(percent) 
putexcel D25 = ("16-75")

* Replaced activity status as report 0 hours
tab flag_not_work_hours if dag >= 16 & dag < 75, matcell(freq) matrow(names)

scalar total = r(N)

matrix percent = (freq/total)*100

putexcel set "$dir_work/flag_descriptives", sheet("UK") modify
putexcel A27 = ("Replaced activity status as report 0 hours")
putexcel A28 = matrix(names) B28 = matrix(freq) C28 = matrix(percent) 
putexcel D28 = ("16-75")

* Replaced activity status from missing to working as report >0 hours
tab flag_missing_act_hours if dag >= 16 & dag < 75, matcell(freq) matrow(names)

scalar total = r(N)

matrix percent = (freq/total)*100

putexcel set "$dir_work/flag_descriptives", sheet("UK") modify
putexcel A30 = ///
	("Replaced activity status from missing to working as report >0 hours")
putexcel A31 = matrix(names) B31 = matrix(freq) C31 = matrix(percent) 
putexcel D31 = ("16-75")

* Replaced missing hours with >0 amount using adjacent cells as report working
tab flag_missing_hours_act_adj if dag >= 16 & dag < 75, matcell(freq) ///
	matrow(names)

scalar total = r(N)

matrix percent = (freq/total)*100

putexcel set "$dir_work/flag_descriptives", sheet("UK") modify
putexcel A33 = ///
("Replaced missing hours with >0 amount using adjacent cells as report working")
putexcel A34 = matrix(names) B34 = matrix(freq) C34 = matrix(percent) 
putexcel D34 = ("16-75")

* Replaced hours from missing to >0 amount using hot deck imputation
tab flag_missing_hours_act_imp if dag >= 16 & dag < 75, matcell(freq) ///
	matrow(names)

scalar total = r(N)

matrix percent = (freq/total)*100

putexcel set "$dir_work/flag_descriptives", sheet("UK") modify
putexcel A36 = ///
	("Replaced hours from missing to >0 amount using hot deck imputation")
putexcel A37 = matrix(names) B37 = matrix(freq) C37 = matrix(percent) 
putexcel D37 = ("16-75")

* Replaced disabled status with 0 due to retirement status
tab flag_disabled_to_retire if dag >= 50, matcell(freq) matrow(names)

scalar total = r(N)

matrix percent = (freq/total)*100

putexcel set "$dir_work/flag_descriptives", sheet("UK") modify
putexcel A39 = ("Replaced disabled status with 0 due to retirement status")
putexcel A40 = matrix(names) B40 = matrix(freq) C40 = matrix(percent) 
putexcel D40 = ("50+")

* Replaced unemployed with 0 due to retirement status enforcement
tab flag_unemp_to_retire if dag >= 50, matcell(freq) matrow(names)

scalar total = r(N)

matrix percent = (freq/total)*100

putexcel set "$dir_work/flag_descriptives", sheet("UK") modify
putexcel A42 = ///
	("Replaced unemployed with 0 due to retirement status enforcement")
putexcel A43 = matrix(names) B43 = matrix(freq) C43 = matrix(percent) 
putexcel D43 = ("50+")

* Old mother to new born 
tab flag_old_mother if dag >= 50 & dgn == 0, matcell(freq) ///
	matrow(names)

scalar total = r(N)

matrix percent = (freq/total)*100

putexcel set "$dir_work/flag_descriptives", sheet("UK") modify
putexcel A45 = ///
	("Reports being a new mother but above max fertile age")
putexcel A46 = matrix(names) B46 = matrix(freq) C46 = matrix(percent) 
putexcel D46 = ("Females, 50+")

* Education level imputed using deductive reasoning 
tab flag_deh_imp_mono if dag >= 16 & ded == 0, matcell(freq) matrow(names)

scalar total = r(N)

matrix percent = (freq/total)*100

putexcel set "$dir_work/flag_descriptives", sheet("UK") modify
putexcel A48 = ("Education level imputed using deductive logic")
putexcel A49 = matrix(names) B49 = matrix(freq) C49 = matrix(percent) 
putexcel D49 = ("16+, not in initial education spell")

* Education level imputed using regresssion model
tab flag_deh_imp_reg if dag >= 16 & ded == 0, matcell(freq) matrow(names)

scalar total = r(N)

matrix percent = (freq/total)*100

putexcel set "$dir_work/flag_descriptives", sheet("UK") modify
putexcel A51 = ("Education level imputed using generalized ordered logit predicted value")
putexcel A52 = matrix(names) B52 = matrix(freq) C52 = matrix(percent) 
putexcel D52 = ("16+, not in initial education spell")

* Partner's education level imputed using deductive reasoning
tab flag_dehsp_imp_mono if dag >= 16 & idpartner != . & idpartner != -9, ///
	matcell(freq) matrow(names)

scalar total = r(N)

matrix percent = (freq/total)*100

putexcel set "$dir_work/flag_descriptives", sheet("UK") modify
putexcel A54 = ///
	("Partner's education level imputed using ordered probit predicted value")
putexcel A55 = matrix(names) B55 = matrix(freq) C55 = matrix(percent) 
putexcel D55 = ("16+, has a partner")

* Partner's education level imputed using regression model 
tab flag_dehsp_imp_reg if dag >= 16 & idpartner != . & idpartner != -9, ///
	matcell(freq) matrow(names)

scalar total = r(N)

matrix percent = (freq/total)*100

putexcel set "$dir_work/flag_descriptives", sheet("UK") modify
putexcel A57 = ///
	("Partner's education level imputed using ordered probit predicted value")
putexcel A58 = matrix(names) B58= matrix(freq) C58 = matrix(percent) 
putexcel D58 = ("16+, has a partner")

* Wage imputed using adjacent observations in panel 
tab flag_wage_imp_panel if les_c3 == 1 , matcell(freq) matrow(names)

scalar total = r(N)

matrix percent = (freq/total)*100

putexcel set "$dir_work/flag_descriptives", sheet("UK") modify
putexcel A60 = ("Wage imputed using adjacent cell in individual panel")
putexcel A61 = matrix(names) B61 = matrix(freq) C61 = matrix(percent) 
putexcel D61 = ("Employed")

* Wage imputed using hot deck imputation 
tab flag_wage_hotdeck if les_c3 == 1 , matcell(freq) matrow(names)

scalar total = r(N)

matrix percent = (freq/total)*100

putexcel set "$dir_work/flag_descriptives", sheet("UK") modify
putexcel A63 = ("Wage imputed using hot deck imputation")
putexcel A64 = matrix(names) B64 = matrix(freq) C64 = matrix(percent) 
putexcel D64 = ("Employed")

 
/***************************Keep required variables***************************/
keep ivfio idhh idperson idpartner idfather idmother dct drgn1 dwt dnc02 dnc dgn dgnsp dag dagsq dhe dhesp dcpst  ///
	ded deh_c3 der dehsp_c3 dehm_c3 dehf_c3 deh_c4 dehsp_c4 dehmf_c3 dcpen dcpyy dcpex widow dcpagdf dlltsd dlltsd01 dlrtrd drtren dlftphm dhhtp_c4 dhhtp_c8 dhm dhm_ghq ///
	dimlwt disclwt dimxwt dhhwt ///
	jbhrs jshrs j2hrs jbstat les_c3 les_c4 lessp_c3 lessp_c4 lesdf_c4 ydses_c5 month scghq2_dv ydisp ///
	ypnbihs_dv yptciihs_dv yplgrs_dv ynbcpdf_dv ypncp ypncp_lvl ypnoab ypnoab_lvl swv sedex ssscp sprfm sedag stm dagsp lhw l1_lhw pno ppno hgbioad1 hgbioad2 der adultchildflag ///
    econ_benefits econ_benefits_nonuc econ_benefits_uc ///
	sedcsmpl sedrsmpl scedsmpl obs_earnings_hourly l1_obs_earnings_hourly ///
	dhh_owned dukfr dchpd dagpns dagpns_sp CPI lesnr_c2 dlltsd_sp dlltsd01_sp  flag* scghq2_dv_miss_flag ///
	Int_Date dhe_mcs dhe_pcs dhe_mcssp dhe_pcssp dls dot dot01 unemp financial_distress new_rel

sort swv idhh idperson 


/**************************Recode missing values*******************************/
foreach var in idhh idperson idpartner idfather idmother dct drgn1 dwt dnc02 dnc dgn dgnsp dag dagsq dhe dhesp dcpst ///
	ded deh_c3 deh_c4 der dehsp_c3  dehsp_c4 dehm_c3 dehf_c3 dehmf_c3 dcpen dcpyy dcpex widow dlltsd dlltsd01 dlrtrd drtren dlftphm dhhtp_c4 dhhtp_c8 dhm dhm_ghq ///
	jbhrs jshrs j2hrs jbstat les_c3 les_c4 lessp_c3 lessp_c4 lesdf_c4 ydses_c5 scghq2_dv ///
	ypnbihs_dv yptciihs_dv yplgrs_dv swv sedex ssscp sprfm sedag stm dagsp lhw l1_lhw pno ppno hgbioad1 hgbioad2 der obs_earnings_hourly l1_obs_earnings_hourly ///
	dhh_owned econ_benefits econ_benefits_nonuc econ_benefits_uc ///
	scghq2_dv_miss_flag dchpd dagpns dagpns_sp CPI lesnr_c2 dlltsd_sp dlltsd01_sp ypncp ypncp_lvl ypnoab ypnoab_lvl flag* dhe_mcs dhe_pcs dhe_mcssp dhe_pcssp dls ///
	dot dot01 unemp new_rel {
		qui recode `var' (-9/-1=-9) (.=-9) 
}

*recode missings in weights to zero. 
foreach var in dimlwt disclwt dimxwt dhhwt {
	qui recode `var' (.=0) (-9/-1=0) 
} 

		
* initialise wealth to missing 
gen total_wealth = -9
gen total_pensions = -9
gen housing_wealth = -9
gen mortgage_debt = -9
gen smp = -9
gen rnk = -9
gen mtc = -9
label var total_wealth "total wealth net of liabilities of benefit unit including housing, business and private (personal and occupational) pensions"
label var total_pensions "value of all private (personal and occupational) pensions of benefit unit"
label var housing_wealth "value of main home gross of mortgage debt of benefit unit"
label var mortgage_debt "total mortgage debt owed on main home of benefit unit"

*check for duplicates in the pooled dataset 
duplicates tag idperson idhh swv, gen(dup)
fre dup
drop if dup == 1 //0 duplicates 
drop dup
isid idperson idhh swv	


/*******************************************************************************
* save pooled dataset 
*******************************************************************************/
save "$dir_data\ukhls_pooled_all_obs_02.dta", replace 


/*********************** Run employment history do-files to produce liwwh *******************************/
* 01_Intdate.do: set up cross-wave file of interview dates 
* ==> needed to link previous wave interview date to each respondent*/
do "${dir_do_emphist}/00_Master_emphist.do"  

use "$dir_data\ukhls_pooled_all_obs_02.dta", clear 

merge 1:1 idperson swv using "${dir_data_emphist}/temp_liwwh", keepusing (liwwh)
//This is done analogous to UKMOD input data 
drop if _merge==2
replace liwwh=12 if _merge==1 
replace liwwh=0 if _merge==1 & les_c3 !=1 //assume zero months if not in employment  
replace liwwh=-9 if swv==1

replace liwwh = liwwh/12  
label var liwwh  "Total years in employment since Jan 2007"

bys swv: fre liwwh if dag<16
bys swv: fre liwwh if dag>=16

drop _merge
save "$dir_data\ukhls_pooled_all_obs_02.dta", replace 

cap log close 


************************************************************************************
* clean-up and exit
************************************************************************************
cd "$dir_data"
dir *.dta

#delimit ;
local files_to_drop 
father_edu.dta    
mother_edu.dta    
temp.dta          
temp_age.dta      
temp_dagpns.dta   
temp_dehsp.dta    
temp_dgn.dta      
temp_dhe.dta      
temp_dlltsd.dta   
temp_donorsN.dta  
temp_dot01.dta    
temp_father_dag.dta
temp_lesc3.dta    
temp_lesc4.dta    
temp_lhw_donors.dta
temp_mother_dag.dta
temp_parent_dchpd.dta
temp_uknbrn.dta   
temp_wages_donors.dta
temp_ypnb.dta     
tmp_partnershipDuration.dta
	;
#delimit cr // cr stands for carriage return

foreach file of local files_to_drop { 
	erase "$dir_data/`file'"
}

