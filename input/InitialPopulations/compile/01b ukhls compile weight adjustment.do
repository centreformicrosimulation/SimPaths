/**********************************************************************
*
*	WEIGHT ADJUSTMENT TO ACCOUNT FOR USING HOUSEHOLDS WITHOUT MISSING VALUES
*	
*
*	AUTH: Patryk Bronka (PB)
*	LAST EDIT: 07/11/2023 (PB)
*
**********************************************************************/

*1. Adjust weights by estimating a probit model for inclusion in the restricted sample of households without missing values.

*1.1. Define a dummy variable classifying households as complete or not
gen complete_hh = (dropHH != 1)

*1.2. Define independent variables for probit
gen drop_indicator = .
replace drop_indicator = 1 if dgn < 0 | dag < 0 | drgn1 < 0
by idhh: egen max_drop_indicator = max(drop_indicator)
drop if max_drop_indicator == 1

recode deh_c3 dcpst (-9 = .)

tab deh_c3, gen(educ) //Generate education level dummies
egen dagcat = cut(dag), at(0,20,30,40,50,60,70,80,120)
tab dagcat, gen(dagcat) //Generate age group dummies
tab dcpst, gen(dcpstcat) //Marital status categories

bys idhh: gen hh_size = _N

//Generate dummies for education and age categories, and interact them with gender (1 = male)
//For example, _IdehXdag_1_20 = 1 means that a person has deh == 1, dagcat == 20, and dgn == 1
xi i.deh_c3*i.dagcat
foreach var in _IdehXdag_2_20 _IdehXdag_2_30 _IdehXdag_2_40 _IdehXdag_2_50 _IdehXdag_2_60 _IdehXdag_2_70 _IdehXdag_2_80 _IdehXdag_3_20 _IdehXdag_3_30 _IdehXdag_3_40 _IdehXdag_3_50 _IdehXdag_3_60 _IdehXdag_3_70 _IdehXdag_3_80  {
	replace `var' = `var'*dgn
}

collapse (firstnm) drgn1 (max) _IdehXdag* dcpstcat* (mean) complete_hh hh_size dwt, by(idhh)

//Household-level probit. Model probabiltiy of being a complete household conditional on presence of people of certain education age gender combination, marital status and region.
probit complete_hh _Ideh* dcpstcat* i.drgn1 i.hh_size

*Predict probability of being a complete household
predict pr_comphh

sum pr_comphh if complete_hh == 0
sum pr_comphh if complete_hh == 1

gen inv_pr_comphh = 1/pr_comphh
keep if complete_hh == 1 //Need to only adjust the weights for complete households included in the sample

*2. Multiply individual weights by the inverse of the predicted probability of inclusion
gen dwt_adjusted = dwt*inv_pr_comphh
replace dwt_adjusted = dwt if missing(dwt_adjusted) 
drop dwt
rename dwt_adjusted dwt

keep idhh dwt
save temp_adjusted_dwt, replace