***************************************************************************************
* PROJECT:              ESPON: construct initial populations for SimPaths using UKHLS data 
* DO-FILE NAME:         02_create_UKHLS_variables.do
* DESCRIPTION:          This file creates initial population variables in the UKHLS pooled sample
***************************************************************************************
* COUNTRY:              UK
* DATA:         	    UKHLS EUL version - UKDA-6614-stata [to wave n]
* AUTHORS: 				Daria Popova, Justin van de Ven
* LAST UPDATE:          30 June 2025 DP 
* NOTE:					Called from 00_master.do - see master file for further details
*						Use -9 for missing values 
***************************************************************************************


*****************************************************************************************************
cap log close 
log using "${dir_log}/02_create_UKHLS_variables.log", replace
*****************************************************************************************************

use "$dir_data\ukhls_pooled_all_obs_01.dta", clear
lab define dummy 1 "yes" 0 "no"


/**************************************************************************************
* SAMPLE
**************************************************************************************/

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
**************************************************************************************/


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


/********************************* INDIVIDUALS ID******************************/ 
clonevar idperson=pidp 
la var idperson "Unique cross wave identifier"


/********************************* EUROMOD style interview date*****************/  
// If missing, replace with household min values
bys swv idhh: egen hhintdatd = max(intdatd_dv)
bys swv idhh: egen hhintdatm = max(intdatm_dv)
bys swv idhh: egen hhintdaty = max(intdaty_dv)

replace intdatd_dv = hhintdatd if intdatd_dv == -9
replace intdatm_dv = hhintdatm if intdatm_dv == -9
replace intdaty_dv = hhintdaty if intdaty_dv == -9

replace intdaty_dv = stm if intdaty_dv == -9
replace intdatm_dv = 6 if intdatm_dv == -9
replace intdatd_dv = 15 if intdatd_dv == -9

gen double ddt=intdatd_dv+100*intdatm_dv+10000*intdaty_dv

format ddt %15.0g
la var ddt "date of interview"
fre ddt


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


/**********************ID FATHER (includes natural/step/adoptive)**************/
clonevar idfather= fnspid
la var idfather "Father unique identifier"


/************************ID MOTHER (includes natural/step/adoptive)************/
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
la var dun "=1 if has spouse"
lab val dun dummy 
//fre dun 


/************************* region (NUTS 1) ************************************/ 
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


/***********************country************************************************/
gen dct=15
la var dct "Country code: UK"


/**********************Partner's gender****************************************/
duplicates report idpartner swv if idpartner >0
/*
Duplicates in terms of idpartner swv
		
copies	observations	surplus
		
1	385784	0
*/

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

cap gen dhe_flag = missing(dhe) 
lab var dhe_flag "=1 if dhe is imputed"
replace dhe = round(imp_dhe) if missing(dhe) 

bys dhe_flag: fre dhe if dag<=18 
bys dhe_flag: fre dhe if dag>18  

drop dgn2 dag2 dagsq2 drgn12 _Idgn2_1 _Iswv_* pred_probs* max_prob imp_dhe


**************************Partner's health status******************************/
preserve
keep swv idperson dhe dhe_flag
rename idperson idpartner
rename dhe dhesp
rename dhe_flag dhesp_flag
save "$dir_data/temp_dhe", replace
restore

merge m:1 swv idpartner   using "$dir_data/temp_dhe"
la var dhesp "Partner's health status"
keep if _merge == 1 | _merge == 3 //matched  385,780  (_merge==3)
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
fre dhm_prediction

gen dhm_flag = missing(dhm)
replace dhm = round(dhm_prediction) if missing(dhm) 
bys dhm_flag : sum dhm 

/**************************Subjective wellbeing (GHQ): Caseness ******************************
0: not psychologically distressed, scghq2_dv < 4 
1: psychologically distressed, scghq2_dv >= 4
This measure converts valid answers to 12 questions of the General Health Questionnaire (GHQ) to a single scale by recoding 1 and 2 values 
on individual variables to 0, and 3 and 4 values to 1, and then summing, giving a scale running from 0 (the least distressed) to 12 
(the most distressed). A binary indicator is then created, equal to 1 for values >= 4.*/
fre scghq2_dv
recode scghq2_dv (-9/-1 . = .)
gen scghq2_dv_miss_flag = (scghq2_dv == .)

*imputation for all 
preserve
drop if dgn < 0 | dag<0 | dhe<0 
eststo predict_scghq2: reg scghq2_dv c.dag i.dgn i.swv i.dhe if scghq2_dv>=0, vce(robust)
restore

estimates restore predict_scghq2
predict scghq2_prediction
fre scghq2_prediction

cap gen scghq2_dv_flag=missing(scghq2_dv) 
replace scghq2_dv = round(scghq2_prediction) if missing(scghq2_dv) 
bys scghq2_dv_flag: fre scghq2_dv
recode scghq2_dv(-1=0) 

*create a dummy var 
cap gen dhm_ghq	= . 
replace dhm_ghq = 0 if scghq2_dv>=0 & scghq2_dv<4
replace dhm_ghq = 1 if scghq2_dv>=4 
lab var dhm_ghq "DEMOGRAPHIC: Subjective wellbeing (GHQ): Caseness"
//fre dhm_ghq


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
fre dhe_mcs_prediction

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
fre dhe_pcs_prediction

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


/****************************Ehtnicity*****************************************/
fre ethn_dv
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

* definition used in regression estimates 
cap gen dot = . 
replace dot = 1 if ethn_dv>=1 & ethn_dv <=7  //white and mixed with white//
replace dot = 2 if ethn_dv>=9 & ethn_dv<=13 //asian//
replace dot = 3 if ethn_dv>=14 & ethn_dv<=16 //black//
replace dot = 4 if ethn_dv==17 | ethn_dv==97 | ethn_dv==-9 | ethn_dv==8 //arab, mixed non-white, other and missing  
lab var dot "Ethnicity"
cap label define dot 1 "White" 2 "Asian or Asian British" 3 "Black, Black British, Caribbean, or African" 4 "Other or missing ethnic group"
label values dot dot 
fre dot 

	
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
fre dot01 

/******************************Education status********************************/
*Use hiqual variable, code negative values to missing
*Low education: Other qualification, no qualification
*Medium education: Other higher degree, A-level etc, GCSE etc
*High education: Degree
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

replace deh_c3 = 3 if dag < 16 & dag>-9 //Children have low level of education until they leave school
//fre deh_c3


/***************************Partner's education status*************************/
preserve
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
drop if missing(deh_c3)
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

 
/******************In continuous education (=first education spell)*************************************/
/*
replace ded = 1 if jbstat == 7 & (l.jbstat==7 | l.jbstat>=. | l.jbstat<0) 
*/
/*Decision 25/10/2024: We opted to revise this variable to ensure that individuals who are observed out of continuous education in one year, aren't recorded as being in continuous education in future years. 
But we include current students who were not observed in the previous wave if they are aged<=23 because the average age of obtaining a Bachelor's degree (BSc) in the UK is typically around 21 to 23 years old.
*/
sort idperson swv 
xtset idperson swv
gen ded = 0 
replace ded = 1 if jbstat==7 & l.jbstat==. & dag <= 23 //currently in education and were not participating in the previous wave and aged<=23 years 
replace ded = 1 if jbstat==7 & l.ded == 1   //currently in education and were in education in the previous wave   
replace ded = 1 if dag < 16 //Everyone under 16 should be in education - this inludes some obs where jbstat!=7
la val ded dummy
la var ded "DEMOGRAPHIC : In Continuous Education"
//fre ded 


/****************************Return to education*******************************/
cap gen der = -9 
replace der = 0 if l.jbstat !=7 & l.jbstat<.
replace der = 1 if jbstat==7 & l.jbstat !=7 & l.jbstat<. 
la val der dummy
la var der "Return to education"
//fre der

/*****************************Partnership status*******************************/
recode mastat_dv (2 3 10 = 1 "Partnered") ///
	(0 1 = 2 "Single never married") /// Includes children under 16
	(4 5 6 7 8 9 = 3 "Previously partnered") ///
	, into (dcpst)
la var dcpst "Partnership status"
recode dcpst (-8 -2 -1 = -9)

*If idpartner = 0 (because of household splitting), dcpst should be set to 3 depending on mastat_dv value
replace dcpst = 3 if dcpst == 1 & idpartner <= 0 
replace dcpst = 1 if idpartner > 0 & !missing(idpartner)

//Children coded as "Never Married" (17 and under chosen as can marry from 18 years onwards)
replace dcpst = 2 if dag <= 17 & idpartner<0
//fre dcpst


/*****************************Enter partnership********************************/
sort idperson swv 
cap drop dcpen
gen dcpen = -9
replace dcpen=0 if (l.dcpst==2 | l.dcpst==3)
replace dcpen=1 if dcpst==1 & (l.dcpst==2 | l.dcpst==3)
la val dcpen dummy
la var dcpen "Enter partnership"
//fre dcpen


/*****************************Exit partnership*********************************/
sort idperson swv 
cap drop dcpex 
gen dcpex=-9
replace dcpex = 0 if l.dcpst==1
replace dcpex = 1 if dcpst==3 & l.dcpst==1 
la val dcpex dummy
la var dcpex "Exit partnership" 
//fre dcpex


/*****************************Age difference partners**************************/
gen dcpagdf = dag - dagsp if (dag > 0 & dagsp > 0) //Leave with negative values? Or should be absolute?
la var dcpagdf "Partner's age difference"


/*********************************Activity status******************************/
recode jbstat (1 2 5 12 13 14 15 = 1 "Employed or self-employed") ///
	(7 = 2 "Student") ///
	(3 6 8 10 11 97 9 4 = 3 "Not employed") /// /*includes apprenticeships, unpaid family business, govt training scheme+retired */
	, into(les_c3)
la var les "Activity status"

//For people under 16 set activity status to student:
replace les_c3 = 2 if dag <= 16
//People below age to leave home are not at risk of work so set activity status to not employed if not a student
replace les_c3 = 3 if dag < $age_become_responsible & les_c3 != 2


/***********************Activity status variable adding retirement*************/
*Generate les_c4 variable in addition to the les_c3 variable. Les_c4 adds retired status. 
cap drop les_c4
clonevar les_c4 = les_c3
replace les_c4 = 4 if jbstat==4 
lab var les_c4 "LABOUR MARKET: Activity status"
lab define les_c4  1 "Employed or self-employed"  2 "Student"  3 "Not employed"  4 "Retired"
lab val les_c4 les_c4
//tab2 les_c3 les_c4


/****************************Partner's activity status:************************/
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


/***********************Own and Spousal Activity Status************************/
gen lesdf_c4 = -9
replace lesdf_c4 = 1 if les_c3 == 1 & lessp_c3 == 1 & dcpst == 1 //Both employed
replace lesdf_c4 = 2 if les_c3 == 1 & (lessp_c3 == 2 | lessp_c3 == 3) & dcpst == 1 //Employed, spouse not employed
replace lesdf_c4 = 3 if (les_c3 == 2 | les_c3 == 3) & lessp_c3 == 1 & dcpst == 1 //Not employed, and spouse employed
replace lesdf_c4 = 4 if (les_c3 == 2 | les_c3 == 3) & (lessp_c3 == 2 | lessp_c3 == 3) & dcpst == 1 //Both not employed

la def lesdf_c4_lb 1"Both employed" 2"Employed and spouse not employed" 3"Not employed and spouse employed" 4"Both not employed" -9"Missing"
la val lesdf_c4 lesdf_c4_lb

la var lesdf_c4 "Own and spousal activity status"
//fre lesdf_c4


/******************************Civil servant status****************************/
gen lcs=0
// R.K. (11.05.2017) (we can use SIC 2007 condensed version- this is what Paola does for FRS EUROMOD)
replace lcs=1 if jbsic07_cc==84
la var lcs "Civil Servant"
lab val lcs dummy 
//fre lcs 


/***********************************Hours of work******************************/
recode jbhrs (-9/-1 . = .) //is it fine to recode these to 0? don't want to have missing in simulation?
recode jbot (-9/-1 . = .)
recode jshrs (-9/-1 . = .)
//lhw is the sum of the above, but don't want to take -9 into account. Recode into missing value. 
egen lhw=rowtotal(jbhrs jbot jshrs)
replace lhw = ceil(lhw)
la var lhw "Hours worked per week"
//fre lhw 


/*****************************Number of children*******************************/
//Number of children aged 0-2 (Checked against manually generating count of children 0-2 per HH - same numbers, but nch02_dv distinguishes missing and 0)
gen dnc02 = nch02_dv
recode dnc02 (-9 = 0)
la var dnc02 "Number of children aged 0-2"

//Number of dependent children aged 0-18 (dependent doesn't include children who have spouse / child but live with parents)
//Gen flag for a dependent child aged 0-18, with at least one parent and classified as dependent child
/*pns1pid = Biological/step/adoptive parent 1: Cross-wave person identifier (PIDP). 
  pns2pid = Biological/step/adoptive parent 2: Cross-wave person identifier (PIDP). 
  If there is more than one parent, the parent with the lowest PNO is the first parent and the parent with the higher PNO is the second parent*/
gen depChild = 1 if (age_dv >= 0 & age_dv <= 18) & (pns1pid > 0 | pns2pid > 0) & (depchl_dv == 1)
bys swv idhh: egen dnc = sum(depChild)
*drop depChild
la var dnc "Number of dependent children 0 - 18"


/*******************************Flag for adult children************************/
preserve
keep if dgn == 0
keep swv idhh idperson dag
rename idperson idmother
rename dag dagmother
save "$dir_data/temp_mother_dag", replace
restore, preserve
keep if dgn == 1
keep swv idhh idperson dag
rename idperson idfather
rename dag dagfather
save "$dir_data/temp_father_dag", replace 
restore

merge m:1 swv idhh idmother using "$dir_data/temp_mother_dag"
keep if _merge == 1 | _merge == 3
drop _merge
merge m:1 swv idhh idfather using "$dir_data/temp_father_dag"
keep if _merge == 1 | _merge == 3
drop _merge

//Adult child is identified on the successful merge with mother / father in the same household and age
gen adultchildflag = (!missing(dagmother) | !missing(dagfather)) & dag >= $age_become_responsible & idpartner <= 0
*Introduce a condition that (adult) children cannot be older than parents-15 year of age
replace adultchildflag = 0 if dag >= dagfather-15 | dag >= dagmother-15 


/************************Household composition*********************************/
cap gen dhhtp_c4 = -9
replace dhhtp_c4 = 1 if dcpst == 1 & dnc == 0 //Couple, no children
replace dhhtp_c4 = 2 if dcpst == 1 & dnc > 0 & !missing(dnc) //Couple, children
replace dhhtp_c4 = 3 if (dcpst == 2 | dcpst == 3) & (dnc == 0 | dag <= $age_become_responsible | adultchildflag== 1) 
/*Single, no children (Note: adult children and children below age to become responsible 
should be assigned "no children" category, even if there are some children in the household)*/
replace dhhtp_c4 = 4 if (dcpst == 2 | dcpst == 3) & dnc > 0 & !missing(dnc) & dhhtp_c4 != 3 //Single, children

la def dhhtp_c4_lb 1"Couple with no children" 2"Couple with children" 3"Single with no children" 4"Single with children"
la values dhhtp_c4 dhhtp_c4_lb
la var dhhtp_c4 "Household composition"
//fre dhhtp_c4


/************************Long-term sick or disabled****************************/
gen dlltsd = 0
replace dlltsd = 1 if jbstat == 8
sort idperson swv 
replace dlltsd = 1 if missing(jbstat) & l.jbstat == 8
//replace dlltsd = 1 if missing(jbstat) & missing(l.jbstat) & l2.jbstat == 8
la var dlltsd "DEMOGRAPHIC: LT sick/disabled"
//fre dlltsd

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

//select those who report being didabled & in receipt of disability benefits according to both checks  
gen dlltsd01 = (dlltsd==1 | (disben==1 & disben2==1)) 
la var dlltsd01 "DEMOGRAPHIC: LT sick/disabled or receives disability benefits"
//fre dlltsd01
//tab2 dlltsd01 dlltsd


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


/*******************************Retired****************************************/
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
lab var dagpns "Reached state retirement age"

/****************************Pension age of a spouse***************************/
preserve
keep swv idperson idhh dagpns
rename dagpns dagpns_sp
rename idperson idpartner
save "$dir_data/temp_dagpns", replace
restore
merge m:1 swv idpartner idhh using "$dir_data/temp_dagpns"
keep if _merge == 1 | _merge == 3
la var dagpns_sp "Reached state retirement age - partner"
drop _merge
replace dagpns_sp=-9 if idpartner<0


/************************************JBSTAT: Not Retired***********************/
gen lesnr_c2 = . 
replace lesnr_c2 = 1 if (jbstat ==1 | jbstat==2) /*employed*/
replace lesnr_c2 = 2 if jbstat==3 | jbstat==5 | jbstat==6 | jbstat==8 | jbstat==9 | jbstat==10 | jbstat==11 | jbstat==14 | jbstat==97 
lab var lesnr_c2 "Not retired work status"
lab define lesnr_c2 1 "in work" 2 "not in work"
lab val lesnr_c2 lesnr_c2 


/************************Exited parental home**********************************/
/*Generated from fnspid and/or mnspid. 1 means that individual no longer lives with a parent (fnspid & mnspid is equal to missing)
 when in the previous wave they lived with a parent  (fnspid or mnspid not equal to missing).*/
/*
bysort swv: fre mnspid if mnspid<=0 
bysort swv: fre fnspid if fnspid<=0 
bysort swv: fre mnspid if mnspid>=. 
bysort swv: fre fnspid if fnspid>=. 
*/
sort idperson swv 
gen dlftphm = -9 if (l.fnspid<0 & l.mnspid<0) //those who did not live with parents in the same hh
replace dlftphm=0 if (l.fnspid>0 | l.mnspid>0) //those who lived with at least one parent
replace dlftphm =1 if (fnspid<0 & mnspid<0) & (l.fnspid>0 | l.mnspid>0) //lived with at least one parent but not anymore 
bys idperson: replace dlftphm =-9 if _n==1  //this condition will not be applicable for first year in the panel//
la val dlftphm dummy
la var dlftphm "DEMOGRAPHIC: Exited Parental Home"
//bys swv: fre dlftphm


/*********************************Left education*******************************/
sort idperson swv 
gen sedex = -9
replace sedex = 0 if l.jbstat == 7
replace sedex = 1 if jbstat != 7 & l.jbstat == 7
la val sedex dummy
la var sedex "Left education"


/****************************Same-sex partnership******************************/
gen ssscp = 0 if idpartner>0
replace ssscp = 1 if idpartner>0 & (dgn == dgnsp) & dgn>=0 & dgn<. & dgnsp>=0 & dgnsp<.
la val ssscp dummy
la var ssscp "Same-sex partnership"
//fre ssscp

/****************************Year prior to exiting partnership******************/
cap gen scpexpy = 0
replace scpexpy = 1 if f.dcpex==1 
replace scpexpy=-9 if swv==14 //Impossible to know for the most recent wave 
la val scpexpy dummy
la var scpexpy "Year prior to exiting partnership"
//fre scpexpy


/*****************************Women aged 18 - 44*******************************/
gen sprfm = 0
replace sprfm = 1 if dgn==0 &  dag >= 18 & dag <= 44
lab val sprfm dummy
la var sprfm "Woman in fertility range dummy (18- 44)"


/************************UK General Fertility Rate: From ONS 2019**************/
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
replace dukfr=49.8 if stm>=2023
lab var dukfr "UK General fertility rate (ONS)"
fre dukfr


/************************Number of newborn*************************/
cap gen child0 = 0
replace child0=1 if dag<=1 

cap drop dchpd
bysort idmother swv: egen dchpd= total(child0) if idmother>0
fre dchpd

preserve 
keep swv idmother dchpd
rename idmother idperson 
rename dchpd mother_dchpd
drop if idperson<0
collapse (max) mother_dchpd, by(idperson swv)
fre mother_dchpd
duplicates report idperson swv
save "$dir_data/mother_dchpd", replace
restore 

merge 1:1 swv idperson using "$dir_data/mother_dchpd" , keepusing (mother_dchpd)
keep if _merge == 1 | _merge == 3
drop _merge
replace mother_dchpd=0 if dgn==1
drop dchpd
rename mother_dchpd dchpd
lab var dchpd "Women's number of newborn children"


/*****************************In educational age range*************************/
gen sedag = 1 if dvage >= 16 & dvage <= 29
replace sedag = 0 if missing(sedag)
la val sedag dummy
la var sedag "Educ age range"


/****************************Partnership duration******************************/
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


/**************************OECD equivalence scale******************************/
//Temporary number of children 0-13 and 14-18 to create household OECD equivalence scale
gen depChild_013 = 1 if (dag >= 0 & dag <= 13) & (pns1pid > 0 | pns2pid > 0) & (depchl_dv == 1)
gen depChild_1418 = 1 if (dag >= 14 & dag <= 18) & (pns1pid > 0 | pns2pid > 0) & (depchl_dv == 1)
bys swv idhh: egen dnc013 = sum(depChild_013)
bys swv idhh: egen dnc1418 = sum(depChild_1418)
drop depChild_013 depChild_1418

gen moecd_eq = . //Modified OECD equivalence scale
replace moecd_eq = 1.5 if dhhtp_c4 == 1
replace moecd_eq = 0.3*dnc013 + 0.5*dnc1418 + 1.5 if dhhtp_c4 == 2
replace moecd_eq = 1 if dhhtp_c4 == 3
replace moecd_eq = 0.3*dnc013 + 0.5*dnc1418 + 1 if dhhtp_c4 == 4

//Drop children variables used to calculate moecd_eq as no longer needed
drop dnc013 dnc1418


/********************************Income variables***************************************/

/*Gross personal non-benefit income 
Note: This is supposed to mirror UKMOD market income 

1/ fimnlabgrs_dv: total personal monthly labour income gross	
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
recode fimnlabgrs_dv fimnpen_dv inc_pp inc_tu inc_ma inc_fm inc_oth (-9=.) (-1=.)
egen ypnb = rowtotal(fimnlabgrs_dv fimnpen_dv inc_pp inc_tu inc_ma inc_fm inc_oth) //Gross personal non-benefit income
fre ypnb if  ypnb <0 //obs with negative income (due to negative self-employment income) but many of these are close to zero ==> recode them to zero 
replace ypnb=0 if  ypnb <0 
sum ypnb 
assert ypnb>=0 

/*Gross personal non-employment, non-benefit income*/
egen yptc = rowtotal(fimnpen_dv inc_pp inc_tu inc_ma inc_fm inc_oth) //Gross personal non-employment, non-benefit income
assert yptc>=0 

/*Gross personal employment income */
gen yplgrs = fimnlabgrs_dv 
fre yplgrs if  yplgrs <0 
replace yplgrs=0 if yplgrs<0 //obs with negative income (due to negative self-employment income) but many of these are close to zero ==> recode them to zero  
assert yplgrs>=0 


/*Gross personal non-benefit income for a spouse */
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


/*Household income */
egen yhhnb = rowtotal(ypnb ypnbsp) if dhhtp_c4 == 1 | dhhtp_c4 == 2 //Household income is sum of individual income and partner's income if coupled
replace yhhnb = ypnb if dhhtp_c4 == 3 | dhhtp_c4 == 4 //If single, household income is equal to individual income


*Income CPI 
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
replace CPI = 1.286 if intdaty_dv == 2024 //yearly index is not available yet//

*For household income, equivalise and adjust for inflation:
replace yhhnb = (yhhnb/moecd_eq)/CPI

*Adjust for inflation:
replace ypnb = ypnb/CPI
replace yptc = yptc/CPI
replace yplgrs = yplgrs/CPI
replace ypnbsp = ypnbsp/CPI

*Inverse hyperbolic sine transformation:
/*This transformation is useful for data that exhibit highly skewed distributions, 
as it can help stabilize variance and normalize the distribution.*/
gen yhhnb_asinh = asinh(yhhnb)
gen ypnbihs_dv = asinh(ypnb)
gen ypnbihs_dv_sp = asinh(ypnbsp)
gen yptciihs_dv = asinh(yptc)
gen yplgrs_dv = asinh(yplgrs)


*Household income Quintiles:
/*Problem: if many observations in yhhnb_asinh have exactly the same value, xtile would group them into a single quintile, causing one or more quintiles to have very few observations. 
This results in 2nd quintile being extremely small compared to the first quintile, which probably has many similar values 
Adding a very small random amount to yhhnb_asinh can help differentiate tied values enough to distribute them more evenly across quintiles without distorting the data meaningfully.
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
//bys stm: fre ydses_c5
//fre ydses_c5


*Difference between own and spouse's gross personal non-benefit income
//gen ynbcpdf_dv = asinh(sinh(ypnbihs_dv) - sinh(ypnbihs_dv_sp))
//Keep as simple difference between the two for compatibility with estimates
gen ynbcpdf_dv = ypnbihs_dv - ypnbihs_dv_sp
recode ynbcpdf_dv (.=-999) if idpartner <0
recode ynbcpdf_dv (.=-999) 
//sum ynbcpdf_dv 

la var ydses_c5 "Household income quintiles"
la var ypnbihs_dv "Gross personal non-benefit income"
la var yptciihs_dv "Gross personal non-employment, non-benefit income"
la var yplgrs_dv "Gross personal employment income"
la var ynbcpdf_dv "Difference between own and spouse's gross personal non-benefit income"

*Gross-to-net ratio  
gen gross_net_ratio = fimngrs_dv/fimnnet_dv 
replace gross_net_ratio = 1 if missing(gross_net_ratio) 
replace gross_net_ratio = 0 if gross_net_ratio<0 

/*Gross personal capital income
1/ fimninvnet_dv: investment income
2/ fimnmisc_dv: net miscellaneous income. [24] educational grant (not student loan or tuition fee loan), [27] payments from a family member not living here, or [38] any other regular payment (not asked in Wave 1).
3/ fimnprben_dv: net private benefit income. [25] trade union / friendly society payment, [26] maintenance or alimony, or [35] sickness and accident insurance.  
Instead of (2), use :  
gen inc_fm = frmnthimp_dv if ficode == 27 //payments from a family member not living here
gen inc_oth = frmnthimp_dv if ficode == 38 //any other regular payment (not asked in Wave 1)
*/
recode fimninvnet_dv fimnprben_dv inc_fm inc_oth (-1=.) (-9=.)
egen ypncp_lvl = rowtotal (fimninvnet_dv inc_fm inc_oth fimnprben_dv) 
assert ypncp_lvl>=0
cap gen ypncp = asinh( ypncp_lvl*gross_net_ratio*(1/CPI) ) //IHS - inverse hyperbolic sine transformation
lab var ypncp "Gross personal non-employment capital income"
//sum ypncp


/* Private pension income
fimnpen_dv: monthly amount of net pension income
inc_pp = frmnthimp_dv if ficode == 4 //A Private Pension/Annuity	 
*/
egen ypnoab_lvl = rowtotal(fimnpen_dv inc_pp) 
assert ypnoab_lvl>=0 
cap gen ypnoab = asinh( ypnoab_lvl *gross_net_ratio*(1/CPI) )
lab var  ypnoab "Gross personal private pension income"
//sum ypnoab


/******************************Home ownership dummy****************************/
*Dhh_owned is the definition used in the initial population and in the model predicting house ownership 
*in the homeownership process of the simulation.
bys swv: fre hsownd
gen dhh_owned=0 
replace dhh_owned=1 if hsownd>=1 & hsownd<=3 
lab var dhh_owned "Home ownership dummy"


/******************************Disability benefit******************************/
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


/******************************Unemployment dummy****************************/
gen unemp = (jbstat==3)
label variable unemp "Labour status: unemployed"


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


/**********************Return to education sample******************************/
//Generated from age_dv and drtren 
gen sedrsmpl =0 
replace sedrsmpl = 1 if (dag>=16 & dag<=35 & ded==0) 
lab var sedrsmpl "SYSTEM : Return to education sample"
lab define  sedrsmpl  1 "Aged 16-35 and not in continuous education"
lab values sedrsmpl sedrsmpl

/**********************In Continuous education sample**************************/
//Generated from sedcsmpl and ded variables. Sample: Respondents who were in continious education and left it. 
cap gen scedsmpl = 0 
replace scedsmpl=1 if sedcsmpl==1 & ded == 0 /*were but currently not in continuous full-time education*/
lab var scedsmpl "SYSTEM : Not in continuous education sample"
lab define  scedsmpl  1 "Left continuous education"
lab values scedsmpl scedsmpl


/*****************************Weights******************************************/
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


/***************************Keep required variables****************************/
keep ivfio idhh idperson idpartner idfather idmother dct drgn1 dwt dnc02 dnc dgn dgnsp dag dagsq dhe dhesp dcpst  ///
	ded deh_c3 der dehsp_c3 dehm_c3 dehf_c3 dehmf_c3 dcpen dcpyy dcpex dcpagdf dlltsd dlltsd01  dlrtrd drtren dlftphm dhhtp_c4 dhm dhm_ghq dimlwt disclwt ///
	dimxwt dhhwt jbhrs jshrs j2hrs jbstat les_c3 les_c4 lessp_c3 lessp_c4 lesdf_c4 ydses_c5 month scghq2_dv ///
	ypnbihs_dv yptciihs_dv yplgrs_dv ynbcpdf_dv ypncp ypnoab swv sedex ssscp sprfm sedag stm dagsp lhw pno ppno hgbioad1 hgbioad2 der adultchildflag ///
	sedcsmpl sedrsmpl scedsmpl dhh_owned dukfr dchpd dagpns dagpns_sp CPI lesnr_c2 dlltsd_sp dlltsd01_sp ypnoab_lvl *_flag  Int_Date dhe_mcs dhe_pcs dot dot01 unemp dhe_mcssp dhe_pcssp ddt

sort swv idhh idperson 


/**************************Recode missing values*******************************/
foreach var in idhh idperson idpartner idfather idmother dct drgn1 dwt dnc02 dnc dgn dgnsp dag dagsq dhe dhesp dcpst ///
	ded deh_c3 der dehsp_c3 dehm_c3 dehf_c3 dehmf_c3 dcpen dcpyy dcpex dlltsd dlltsd01 dlrtrd drtren dlftphm dhhtp_c4 dhm dhm_ghq ///
	jbhrs jshrs j2hrs jbstat les_c3 les_c4 lessp_c3 lessp_c4 lesdf_c4 ydses_c5 scghq2_dv ///
	ypnbihs_dv yptciihs_dv yplgrs_dv swv sedex ssscp sprfm sedag stm dagsp lhw pno ppno hgbioad1 hgbioad2 der dhh_owned ///
	scghq2_dv_miss_flag dchpd dagpns dagpns_sp CPI lesnr_c2 dlltsd_sp dlltsd01_sp ypnoab_lvl *_flag dhe_mcs dhe_pcs dot dot01 unemp dhe_mcssp dhe_pcssp {
		qui recode `var' (-9/-1=-9) (.=-9) 
}
replace ddt = -9 if ddt<0
*recode missings in weights to zero. 
foreach var in dimlwt disclwt dimxwt dhhwt {
	qui recode `var' (.=0) (-9/-1=0) 
} 

*add potential hourly earnings
gen potential_earnings_hourly = 0
gen l1_potential_earnings_hourly = 0

replace potential_earnings_hourly = sinh(yplgrs_dv)/(lhw*4.33) if les_c4 == 1
replace l1_potential_earnings_hourly = potential_earnings_hourly

replace potential_earnings_hourly = 0 if missing(potential_earnings_hourly)
replace l1_potential_earnings_hourly = 0 if missing(l1_potential_earnings_hourly)
		
* initialise wealth to missing 
gen liquid_wealth = -9
gen smp = -9
gen rnk = -9
gen mtc = -9

*check for duplicates in the pooled dataset 
duplicates tag idperson idhh swv, gen(dup)
fre dup
drop if dup == 1 //0 duplicates 
drop dup
isid idperson idhh swv	

duplicates tag idperson ddt, gen(dup2)
fre dup2

/*******************************************************************************
* save the whole pooled dataset that will be used for regression estimates
*******************************************************************************/
save "$dir_data\ukhls_pooled_all_obs_02.dta", replace 
cap log close 


/**************************************************************************************
* clean-up and exit
**************************************************************************************/
#delimit ;
local files_to_drop 
	father_edu.dta
	mother_dchpd.dta 
	mother_edu.dta 
	temp.dta
	temp_age.dta
	temp_dagpns.dta
	temp_deh.dta
	temp_dgn.dta
	temp_dhe.dta
	temp_dlltsd.dta
	temp_father_dag.dta
	temp_lesc3.dta
	temp_lesc4.dta
	temp_mother_dag.dta
	temp_ypnb.dta
	tmp_partnershipDuration.dta
	;
#delimit cr // cr stands for carriage return

foreach file of local files_to_drop { 
	erase "$dir_data/`file'"
}


