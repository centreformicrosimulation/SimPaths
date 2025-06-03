/********************************************************************************
*
*	FILE TO OBTAIN TRAINING DATA FOR SIMPATHS
*
*	AUTH: Justin van de Ven (JV)
*	LAST EDIT: 30/05/2024 (JV)
*
*******************************************************************************/


set more off

/********************************************************************************
	program parameters - from 00_master.do
*******************************************************************************/

* Working directory
global dir_work "C:\MyFiles\99 DEV ENV\JAS-MINE\data work\initial_populations"

* Directory which contains do files
global dir_do "${dir_work}/do"

* Directory which contains data files 
global dir_data "${dir_work}/data"

global wealthStartYear = 2015
global wealthEndYear = 2019


/********************************************************************************/
* pool data from all years
/********************************************************************************/
forvalues yy = $wealthStartYear/$wealthEndYear {
	
	import delimited using "$dir_data/population_initial_UK_`yy'.csv", clear
	capture confirm variable smp
	if !_rc {
		drop smp rnk mtc
	}
	save "$dir_data/temp_`yy'.dta", replace
}
forvalues yy = $wealthStartYear/$wealthEndYear {

	if (`yy' == $wealthStartYear) {
		use "$dir_data/temp_$wealthStartYear.dta", clear
	} 
	else {
		append using "$dir_data/temp_`yy'.dta"
	}
}
save "$dir_data/temp0", replace


/********************************************************************************/
* select sample to keep
/********************************************************************************/
use "$dir_data/temp0", clear
sort idhh idbenefitunit swv

* omit benefit units with no mature individuals
by idhh idbenefitunit: egen maxage = max(dag)
drop if (maxage<18)
drop maxage

* omit observations where same household and benefit id is reported across different sample waves
gen chk = (idhh==idhh[_n-1] & idbenefitunit==idbenefitunit[_n-1] & swv!=swv[_n-1])
by idhh idbenefitunit: egen maxchk = max(chk)
drop if (maxchk==1)
drop maxchk chk

* omit same-sex households
gen idm = (dgn==0 & dag>17)
gen idf = (dgn==1 & dag>17)
by idhh idbenefitunit: egen msum = sum(idm)
by idhh idbenefitunit: egen fsum = sum(idf)
gen chkm = (msum>1)
gen chkf = (fsum>1)
drop if (chkm==1 | chkf==1)
drop idm idf msum fsum chkm chkf

* randomly select households from remaining sample
gen rnd = runiform()
gen chk = 0
replace chk = 1 if (idhh[_n-1]!=idhh & rnd < (1.0 / ($wealthEndYear - $wealthStartYear + 1.0)))
replace chk = chk[_n-1] if (idhh[_n-1]==idhh)
keep if (chk==1)
drop chk
save "$dir_data/temp1", replace


/********************************************************************************/
* de-identify all data
/********************************************************************************/
use "$dir_data/temp1", clear

// randomise order of households
sort idhh idbenefitunit
replace rnd = rnd[_n-1] if (idhh[_n-1]==idhh)
sort rnd idhh idbenefitunit
drop rnd

// annonymise idhh
gen idhhb = 0
replace idhhb = 1 if (_n==1)
replace idhhb = idhhb[_n-1] + (idhh!=idhh[_n-1]) if (_n>1)
order idhh idhhb
drop idhh
rename idhhb idhh
save "$dir_data/temp2", replace

// annonymise idbenefitunit
use "$dir_data/temp2", clear
gen idtmp = 0
replace idtmp = 1 if (_n==1)
replace idtmp = idtmp[_n-1] + (idbenefitunit!=idbenefitunit[_n-1]) if (_n>1)
order idtmp, a(idbenefitunit)
drop idbenefitunit
rename idtmp idbenefitunit
save "$dir_data/temp3", replace

// annonymise person identifiers
// uniquely identify person identiers
use "$dir_data/temp3", clear
replace idperson = idperson*100 + swv
replace idpartner = idpartner*100 + swv if (idpartner>0)
replace idmother = idmother*100 + swv if (idmother>0)
replace idfather = idfather*100 + swv if (idfather>0)
gen idtmp = 0
replace idtmp = 1 if (_n==1)
replace idtmp = idtmp[_n-1]+1 if (_n>1)
order idtmp, a(idperson)
save "$dir_data/temp4", replace

keep idtmp idperson
rename idtmp idtmp2
rename idperson idpartner
merge 1:m idpartner using "$dir_data/temp4", keep(2 3) nogen
order idpartner idtmp2, a(idtmp)
sort idhh idbenefitunit idtmp
save "$dir_data/temp5", replace

keep idperson idtmp
rename idperson idmother
rename idtmp idtmp3
merge 1:m idmother using "$dir_data/temp5", keep(2 3) nogen
order idmother idtmp3, a(idtmp2)
sort idhh idbenefitunit idtmp
save "$dir_data/temp6", replace

keep idperson idtmp
rename idperson idfather
rename idtmp idtmp4
merge 1:m idfather using "$dir_data/temp6", keep(2 3) nogen
order idfather idtmp4, a(idtmp3)
sort idhh idbenefitunit idtmp
save "$dir_data/temp7", replace

recode idtmp2 idtmp3 idtmp4 (mis=-9)
drop idperson idpartner idmother idfather
rename idtmp idperson
rename idtmp2 idpartner
rename idtmp3 idmother
rename idtmp4 idfather
save "$dir_data/temp8", replace

// adjust wave identifiers
gen swvb = 1
order swvb, a(swv)
drop swv
rename swvb swv
gen stmb = $wealthEndYear
order stmb, a(stm)
drop stm
rename stmb stm
save "$dir_data/temp9", replace

// adjust weights
drop dwt_sampling multiplier
gen ldwt = ln(dwt)
sum ldwt
gen dwt2 = round(exp(r(mean) + r(sd) * rnormal()))
order dwt2, a(dwt)
drop dwt ldwt
rename dwt2 dwt
save "$dir_data/temp10", replace

// adjust continuous variables
foreach vv of varlist yplgrs_dv ypnbihs_dv yptciihs_dv ynbcpdf_dv liquid_wealth tot_pen nvmhome ypncp ypnoab formal_socare_hrs partner_socare_hrs daughter_socare_hrs son_socare_hrs other_socare_hrs formal_socare_cost aidhrs {
	gen tmp = `vv'
	order tmp, a(`vv')
	recode tmp (0=.)
	sum tmp
	replace tmp = tmp + r(sd) * rnormal() * 0.15 if (!missing(tmp))
	recode tmp (.=0)
	drop `vv'
	rename tmp `vv'
}
foreach vv of varlist yplgrs_dv ypnbihs_dv yptciihs_dv tot_pen ypncp ypnoab formal_socare_hrs partner_socare_hrs daughter_socare_hrs son_socare_hrs other_socare_hrs formal_socare_cost aidhrs {
	replace `vv' = 0 if (`vv'<0.0)
}
gen tmp = ln(potential_earnings_hourly)
order tmp, a(potential_earnings_hourly)
sum tmp
replace tmp = tmp + r(sd) * rnormal() * 0.15 if (!missing(tmp))
replace tmp = exp(tmp)
recode tmp (.=0)
drop potential_earnings_hourly l1_potential_earnings_hourly
rename tmp potential_earnings_hourly
gen l1_potential_earnings_hourly = potential_earnings_hourly
order l1_potential_earnings_hourly, a(potential_earnings_hourly)
save "$dir_data/temp11", replace

// set benefit unit level variables
use "$dir_data/temp11", clear
foreach vv of varlist drgn1 ydses_c5 dhh_owned dwt liquid_wealth tot_pen nvmhome {
	
	rename `vv' `vv'i
	bys idbenefitunit: egen `vv' = mean(`vv'i)
	drop `vv'i
}


/**************************************************************************************
*	export training data
*************************************************************************************/
recode dgn liquid_wealth tot_pen nvmhome need_socare formal_socare_hrs partner_socare_hrs daughter_socare_hrs son_socare_hrs other_socare_hrs formal_socare_cost aidhrs carewho (-9=0)
export delimited using "$dir_data/training_population_initial_UK_$wealthEndYear.csv", nolabel replace


/**************************************************************************************
*	clean-up
*************************************************************************************/
forvalues yy = $wealthStartYear/$wealthEndYear {
	
	rm "$dir_data/temp_`yy'.dta"
}
forvalues ii = 0/11 {
	rm "$dir_data/temp`ii'.dta"
}
