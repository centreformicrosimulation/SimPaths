/*******************************************************************************
* PROJECT:             	SimPaths UK
* DO-FILE NAME:        	00_master_conditions.do
* DESCRIPTION:         	Sets out the assumptions and conditions imposed in the 
* 						creation of the unique dataset and the if conditions 
* 						imposed when estimating the processes for SimPaths.  
********************************************************************************
* COUNTRY:              UK 
* AUTHORS: 				Daria Popova 
* LAST UPDATE:          6 May 2026 DP
********************************************************************************
*   -----------------------------------------------------------------------------
*    Assumptions imposed to align the initial populations with simulation rules 
*   -----------------------------------------------------------------------------
*
*    - Retirement:
*       - Treated as an absorbing state
*       - Must retire by a specified maximum age
*       - Cannot retire before a specified minimum age
*
*   - Education:
*       - Leave education no earlier than a specified minimum age
*       - Must leave the initial education spell by a specified maximum age
*       - Cannot return to education after retirement
*
*   - Work:
*       - Can work from a specified minimum age
*       - Activity status and hours of work populated consistently:
*           → Assume not working if report hours = 0 
*           → Assume hours = 0 if not working
* 		- If missing partial information, don't assume the missing is 0 and 
* 			impute (hot-deck)
*
*   - Leaving the parental home:
*       - Can leave from a specified minimum age
* 		- Become the effective head of hh even when living with parents when 
* 			paretns retire or reach state retirment age
*
*   - Home ownership:
*       - Can own a home from a specified minimum age
*
*   - Partnership formation:
*       - Can form a partnership from a specified minimum age
*
*   - Disability:
*       - Treated as a subsample of the not-employed population
*
*   The relevant age thresholds are defined in globals defined in "DEFINE 
* 	PARAMETERS" section below. 
* 	Throughout also construct relevant flags and produce an Excel file "flag_descriptves" to 
* 	see the extent of the adjustments to the raw data. 
*
*   -----------------------------------------------------------------------
*    Additional notes on implementation: 
*   -----------------------------------------------------------------------
*   Current imputations : 
*   - Self-rated health status (ordered probit model)
*   - Subjective well-being (liner regression) 
*   - Mental and physical component summaries (linear regression)
*   - Impute highest parental education status (ordered probit model) 
*   - Impute education status using lagged observation and generalized ordered logit 
*   - Impute working hours if missing but the person is in work (panel based imputation + hot-deck)
*   - Impute observed hourly wages if missing but the person is in work (panel based imputation + hot-deck) 
*   
*   -----------------------------------------------------------------------
*   Remaining disparities between initial populations and simulation rules:
*   -----------------------------------------------------------------------
*   - Ages at which females can have a child. [Be informed by the sample?]
*	  Permit teenage mothers in this script (deal with in 03_ )
*   - A few higher/older education spells (30+) that last multiple years, whilst 
*     in the simulation can only return to education for single year spells. 
* 	- Should we have people becoming adults at 18 or 16 for income/number of 
* 		children purposes?
* 		Considered a child if live with parents until 18 and in ft education? 
* 	- Don't impose monotoncity on reported educational attainment information.  
* 	- Number of children vars (all ages or 0-2) don't account for feasibility 
* 		of age at birth of the mother. 
*******************************************************************************/

/*******************************************************************************
* DEFINTE PARAMETERS
*******************************************************************************/

global country "UK" 

global first_sim_year "2010"

global last_sim_year "2025"



* Globals used for all processes   
global weight "dwt"

//global regions "UKC UKD UKE UKF UKG UKH UKJ UKK UKL UKM UKN" //UKI is London (reference)
global regions "demRgnUKC demRgnUKD demRgnUKE demRgnUKF demRgnUKG demRgnUKH demRgnUKJ demRgnUKK demRgnUKL demRgnUKM demRgnUKN" //demRgnUKI is London (reference)

//global ethnicity "Ethn_Asian Ethn_Black Ethn_Other" //White is reference. Mixed race & undefined  are in Other category 
global ethnicity "demEthnC4Asian demEthnC4Black demEthnC4Other" //White is reference. Mixed race & undefined  are in Other category 

* Define threshold ages
/*
Ages used for specifying samples. 
ENSURE THE SAME AS THE GLOBALS USED IN THE INTIIAL POPULATIONS MASTER FILE 
*/
 	
* Age become an adult in various dimensions	
global age_becomes_responsible 18 

global age_becomes_semi_responsible 16 

global age_seek_employment 16 
	
global age_leave_school 16 

global age_form_partnership 18 

global age_have_child_min 18 	

global age_leave_parental_home 18

global age_own_home 18

* Age can/must/cannot make various transitions 	
global age_max_dep_child 17     

global age_adult 18 

global age_can_retire 50

global age_force_retire 75             

global age_force_leave_spell1_edu 30   

global age_have_child_max 49  	// allow this to be led by the data  


/*******************************************************************************
* PROCESS IF CONDITIONS 
*******************************************************************************/

* Education 
global e1a_if_condition "dag >= ${age_leave_school} & dag < ${age_force_leave_spell1_edu} & l.les_c4 == 2"

global e1b_if_condition "dag >= ${age_leave_school} & l.les_c4 != 4 & l.les_c4 != 2"

global e2_if_condition "dag >= ${age_leave_school} & l.les_c4 == 2 & les_c4 != 2"

* Leave the parental home 
global p1_if_condition "ded == 0 & dag >= ${age_leave_parental_home}"

* Partnership 
global u1_if_condition "dag >= ${age_form_partnership} & ssscp != 1"

global u2_if_condition "dgn == 0 & dag >= ${age_form_partnership} & l.ssscp != 1"

* Fertility 
global f1_if_condition "dag >= ${age_have_child_min} & dag <= ${age_have_child_max} & dgn == 0"

* Health 
global h1_if_condition "dag >= ${age_becomes_semi_responsible} & flag_dhe_imp == 0"

global h2_if_condition "dag >= ${age_becomes_semi_responsible} & ded == 0"

* Home ownership
global ho1_if_condition "dag >= ${age_own_home}"

* Retirment 
global r1a_if_condition "dcpst == 2  & dag >= ${age_can_retire}"

global r1b_if_condition "ssscp != 1 & dcpst == 1 & dag >= ${age_can_retire}"


* WAGES
global wages_f_no_prev_if_condition "dgn == 0 & dag >= ${age_seek_employment} & dag <= ${age_force_retire} & previouslyWorking == 0 & deh_c4>0"

global wages_m_no_prev_if_condition "dgn == 1 & dag >= ${age_seek_employment} & dag <= ${age_force_retire} & previouslyWorking == 0 & deh_c4>0"

global wages_f_prev_if_condition "dgn == 0 & dag >= ${age_seek_employment} & dag <= ${age_force_retire} & previouslyWorking == 1 & deh_c4>0"

global wages_m_prev_if_condition "dgn == 1 & dag >= ${age_seek_employment} & dag <= ${age_force_retire} & previouslyWorking == 1 & deh_c4>0"


* CAPITAL INCOME 
global i1a_if_condition "dag >= ${age_becomes_semi_responsible}" 

global i1b_if_condition "dag >= ${age_becomes_semi_responsible} & receives_ypncp == 1" 

* PRIVATE PENSION INCOME 
global i2b_if_condition "dag >= ${age_can_retire} & dlrtrd == 1 & l.dlrtrd==1 & receives_ypnoab==1"  

global i3a_if_condition "dag >= ${age_can_retire} & dlrtrd == 1 & l.dlrtrd!=1 & l.les_c4 != 2"

global i3b_if_condition "dag >= ${age_can_retire} & dlrtrd == 1 & l.dlrtrd!=1 & l.les_c4 != 2 & receives_ypnoab==1"


* SOCIAL CARE  
global s2a_if_condition "dag > 64 & stm >= 15 & stm <= 22"								// Need care

global s2b_if_condition "dag > 64 & stm >= 16 & stm <= 21"								// Receive care

global s2c_if_condition "dag > 64 & receive_care & stm >= 16 & stm <= 21"				// Care mix received

global s2d_if_condition "dag > 64 & receive_informal_care & stm >= 16 & stm <= 21"		// Informal care hours received

global s2e_if_condition "dag > 64 & receive_formal_care & stm >= 16 & stm <= 21"		// Formal care hours received


global s3a_if_condition "Single & stm >= 15"												// Provide care, Singles

global s3b_if_condition "Partnered & stm >= 15"											// Provide care, Partnered

global s3c_if_condition "provide_informal_care & Single & stm >= 15"						// Informal care hours provided, Singles

global s3d_if_condition "provide_informal_care & Partnered & stm >= 15"					// Informal care hours provided, Singles


* Finanicial distress and health processes 
* TO ADD 
