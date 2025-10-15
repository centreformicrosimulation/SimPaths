/*******************************************************************************
* PROJECT:  		ESPON 
* SECTION:			Validation
* OBJECT: 			Validation data pre-processing
* AUTHORS:			Ashley Burdett 
* LAST UPDATE:		9/25
* COUNTRY: 			UK 

* DESCRIPTION: 		This file obtains the monthly individual disposable income 
* 					variable from the UKHLS to merge into the processed data 

* NOTES: 			Unlike with the European models, we use the same data 
* 					that was used to estimate the transition parameters. 
* 					For now use the exact same dataset that does not drop 
* 					household that have some missing information and thus the 
* 					weights have not yet been adjusted.  
*******************************************************************************/
clear all

* Will use the below file which is used to estimate the transition parameters 
* This is before missing values are dropped and weights adjusted
//"$dir_data/ukhls_pooled_all_obs_09.dta"



* Prepare dataset with dispoable income to merge in 
use "$dir_data/ukhls_pooled_all_obs_01.dta", clear

* Drop booster sample
drop if hhorig == 8   

* From initial populations 2_ script

lab var swv "Data collection wave"

* Year
gen stm = intdaty_dv
la var stm "Interview year"

* Interview date 
gen Int_Date = mdy(intdatm_dv, intdatd_dv ,intdaty_dv) 
format Int_Date %d

* Household ID
clonevar idhh = hidp 
la var idhh "Household identifier"

* Individal ID 
clonevar idperson = pidp 
lab var idperson "Unique cross wave identifier"

keep idperson swv fimnnet_dv

save "$dir_data/ukhls_ind_dispos_inc.dta", replace

