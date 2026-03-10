
***************************************************************************************
* PROJECT:              SimPaths UK: regression estimates for SimPaths using UKHLS data 
* DO-FILE NAME:         master.do
* DESCRIPTION:          Main do-file to set the main parameters (country, paths) and call sub-scripts
***************************************************************************************
* COUNTRY:              UK
* DATA:         	    UKHLS EUL version - UKDA-6614-stata [to wave o]
*
* AUTHORS: 				Daria Popova, Justin van de Ven
* LAST UPDATE:          18 Feb 2026 DP  
***************************************************************************************

***************************************************************************************
* General comments:
* - Note that in the following scripts some standard commands may be 
*   abbreviated: (gen)erate, (tab)ulate, (sum)marize, (di)splay, 
*   (cap)ture, (qui)etly, (noi)sily

*Stata packages to install 
*ssc install fre
*ssc install tsspell 
*ssc install carryforward 
*ssc install outreg2
*ssc install oparallel
*ssc install gologit2
*ssc install winsor
*ssc install reghdfe
*ssc install ftools
*ssc install require
*
* NOTES: 				                        						 
* 						The income and union parameter do file must be run after
* 						the wage estimates are obtained because they use 
* 						predicted wages. The order of the remaining files is
* 						arbitrary. 
***************************************************************************************
***************************************************************************************

clear all
set more off
set type double
set maxvar 30000
set matsize 1000


/**************************************************************************************
* DEFINE DIRECTORIES
**************************************************************************************/

* Working directory
global dir_work "D:\Dasha\ESSEX\_SimPaths\_SimPaths_UK\regression_estimates"

* Directory which contains do files
global dir_do "${dir_work}/do"

* Directory which contains log files 
global dir_log "${dir_work}/log"

* Directory which contains raw output: Excel and Word tables 
global dir_raw_results "${dir_work}/raw_results"

* Directory which contains final Excel files read by the model  
global dir_results "${dir_work}/results"

* Pooled dataset for estimates  
global estimation_sample "D:\Dasha\ESSEX\_SimPaths\_SimPaths_UK\initial_populations\data\UKHLS_pooled_ipop.dta"

* Pooled dataset with predicted wages after Heckman   
global estimation_sample2 "D:\Dasha\ESSEX\_SimPaths\_SimPaths_UK\initial_populations\data\UKHLS_pooled_ipop2.dta"

* Directory containing external input data 
global dir_external_data "$dir_work/external_data"

* Directory containing results of comparison of various weights   
global weight_checks "${dir_work}/weight_checks"

*********************Internal validation****************************************
* Directory to save data for internal validation 
global dir_validation_data "${dir_work}/internal_validation/data"

* Directory for internal validation do-files 
global dir_do_validation "${dir_work}/internal_validation/do_files"

* Directory for internal validation do-files 
global dir_validation_graphs "${dir_work}/internal_validation/graphs"

global country "UK" 

global first_sim_year "2010"

global last_sim_year "2025"

* Globals used for all processes   

global weight "dwt"

global regions "UKC UKD UKE UKF UKG UKH UKJ UKK UKL UKM UKN" //UKI is London (reference)

global ethnicity "Ethn_Asian Ethn_Black Ethn_Other" //White is reference. Mixed race & undefined  are in Other category 


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
  


/*******************************************************************************
* ESTIMATION FILES
*******************************************************************************/
/**/
do "${dir_do}/reg_education.do"

do "${dir_do}/reg_leave_parental_home.do"

do "${dir_do}/reg_partnership.do"

do "${dir_do}/reg_fertility.do"
 
do "${dir_do}/reg_health.do"

do "${dir_do}/reg_home_ownership.do"

do "${dir_do}/reg_retirement.do"

do "${dir_do}/reg_wages.do"

do "${dir_do}/reg_income.do"

do "${dir_do}/reg_socialcare.do" 

do "${dir_do}/reg_financial_distress.do"

do "${dir_do}/reg_health_mental.do"

do "${dir_do}/reg_health_wellbeing.do"


*******************************************************************************
* INTERNAL VALIDATION FILES
******************************************************************************
/*
do "$dir_do_validation/int_val_education.do"	

do "$dir_do_validation/int_val_leave_parental_home.do"	

do "$dir_do_validation/int_val_partnership.do"	

do "$dir_do_validation/int_val_fertility.do"	

do "$dir_do_validation/int_val_health.do"	

do "$dir_do_validation/int_val_home_ownership.do"	

do "$dir_do_validation/int_val_retirement.do"	

do "$dir_do_validation/int_val_wages.do"	

do "$dir_do_validation/int_val_income.do"	
*/

/**************************************************************************************
* END OF FILE
**************************************************************************************/
