/**********************************************************************
*
*	WEIGHT ADJUSTMENT TO ACCOUNT FOR USING HOUSEHOLDS WITHOUT MISSING VALUES
*	
*	AUTH: Patryk Bronka, Daria Popova, Justin van de Ven
*	LAST EDIT: 18/04/2024 (JV)
*
**********************************************************************/

use "$dir_data\ukhls_pooled_all_obs_05.dta", clear

*1. Adjust weights by estimating a probit model for inclusion in the restricted sample of households without missing values.
sort stm idhh

*1.1. Define a dummy variable classifying households as complete or not
cap gen complete_hh = (dropHH != 1)

*1.2. Define independent variables for probit
sum dgn dag drgn1
cap gen drop_indicator = .
replace drop_indicator = 1 if dgn < 0 | dag < 0 | drgn1 < 0

by stm idhh: egen max_drop_indicator = max(drop_indicator)
drop if max_drop_indicator == 1 /*583 observations deleted*/

recode deh_c3 dcpst stm (-9 = .)
sum deh_c3 dcpst

tab deh_c3, gen(educ) //Generate education level dummies
egen dagcat = cut(dag), at(0,20,30,40,50,60,70,80,120)
tab dagcat, gen(dagcat) //Generate age group dummies
tab dcpst, gen(dcpstcat) //Marital status categories

cap drop hh_size
bys stm idhh: gen hh_size = _N
sum hh_size

//Generate dummies for education and age categories, and interact them with gender (1 = male)
//For example, _IdehXdag_1_20 = 1 means that a person has deh == 1, dagcat == 20, and dgn == 1
xi i.deh_c3*i.dagcat
foreach var in _IdehXdag_2_20 _IdehXdag_2_30 _IdehXdag_2_40 _IdehXdag_2_50 _IdehXdag_2_60 _IdehXdag_2_70 ///
_IdehXdag_2_80 _IdehXdag_3_20 _IdehXdag_3_30 _IdehXdag_3_40 _IdehXdag_3_50 _IdehXdag_3_60 _IdehXdag_3_70 _IdehXdag_3_80  {
	replace `var' = `var'*dgn
}

collapse (firstnm) drgn1 (max) _IdehXdag* dcpstcat* complete_hh (mean) hh_size dwt, by(stm idhh)
duplicates report idhh stm 
replace complete_hh = 1 if complete_hh >= 0.5 & complete_hh<1 
replace complete_hh = 0 if complete_hh < 0.5 & complete_hh>0  
bys stm: sum drgn1 _IdehXdag* dcpstcat* hh_size dwt
bys stm: fre complete_hh

recode hh_size (1=1) (2=2) (3=3) (4=4) (5/max=5), gen(hhsize_cat)
recode hh_size (1=1) (2=2) (3=3) (4/max=4) , gen(hhsize_cat2)

/*Household-level probit. Model probabiltiy of being a complete household conditional on presence of people
 of certain education age gender combination, marital status and region.*/
probit complete_hh _Ideh* dcpstcat* ib8.drgn1  i.stm , vce(robust) iterate(20) //i.hhsize_cat2 DP: dropped as otherwise does not converge

*Predict probability of being a complete household
predict pr_comphh

sum pr_comphh if complete_hh == 0
sum pr_comphh if complete_hh == 1

gen inv_pr_comphh = 1/pr_comphh
sum inv_pr_comphh
keep if complete_hh == 1 //Need to only adjust the weights for complete households included in the sample

*2. Multiply individual weights by the inverse of the predicted probability of inclusion
gen dwt_adjusted = dwt*inv_pr_comphh
replace dwt_adjusted = dwt if missing(dwt_adjusted) 
sum dwt*

keep stm idhh dwt_adjusted
count
save "$dir_data/temp_adjusted_dwt", replace


**************************************************************************	
*WEIGHT ADJUSTMENT TO ACCOUNT FOR USING HOUSEHOLDS WITHOUT MISSING VALUES*
**************************************************************************
use "$dir_data\ukhls_pooled_all_obs_05.dta", clear 
count 
cap drop _merge
merge m:1 stm idhh using "$dir_data\temp_adjusted_dwt.dta", keepusing (dwt_adjusted)
keep if _merge==1 | _merge==3

gen dwt_sampling=dwt //keep original weights before any adjustment 
replace dwt=dwt_adjusted if (!missing(dwt_adjusted)) //keep weigths adjusted for probability of being complete hhs 
drop _merge

bys stm: sum dwt dwt_adjusted dwt_sampling
	
*Cannot have missing values in continuous variables - recode to 0 for now: 
*(But note this treatment of missings is generally valid - e.g. people without a partner don't have years in partnership etc.)
recode dcpyy dcpagdf ynbcpdf_dv dnc02 dnc ypnbihs_dv yptciihs_dv ypncp ypnoab yplgrs_dv stm swv /*dhe dhesp*/ dhm scghq2_dv dhm_ghq (-9 . = 0)
    
save "$dir_data\ukhls_pooled_all_obs_06.dta", replace  


/**********************Slice the original pooled dataset into years ********************************************/
forvalues yy = $firstSimYear/$lastSimYear {

	use "$dir_data\ukhls_pooled_all_obs_06.dta", clear

	drop if dwt==0

	* limit year
	keep if stm == `yy' 

	* ensure consistency of benefit unit data
	gsort idbenefitunit -dag
	foreach vv of varlist dwt drgn1 dhhtp_c4 ydses_c5 dnc02 dnc {
		bys idbenefitunit: replace `vv' = `vv'[1] if (`vv'!=`vv'[1])
	}
	
	save "$dir_data/population_initial_fs_UK_`yy'.dta", replace
}


/**************************************************************************************
* clean-up and exit
**************************************************************************************/
#delimit ;
local files_to_drop 
	temp_adjusted_dwt.dta
	;
#delimit cr // cr stands for carriage return

foreach file of local files_to_drop { 
	erase "$dir_data/`file'"
}
