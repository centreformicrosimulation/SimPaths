/********************************************************************************
*
*	FILE TO OBTAIN TRAINING DATA FOR SIMPATHS
*
*	AUTH: Justin van de Ven (JV)
*	LAST EDIT: 20/01/2026 (JV)
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
	
	use "$dir_data/population_initial_UK_`yy'.dta", clear
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
sort idHh idBu statCollectionWave

* omit benefit units with no mature individuals
by idHh idBu: egen maxage = max(demAge)
drop if (maxage<18)
drop maxage

* omit observations where same household and benefit id is reported across different sample waves
gen chk = (idHh==idHh[_n-1] & idBu==idBu[_n-1] & statCollectionWave!=statCollectionWave[_n-1])
by idHh idBu: egen maxchk = max(chk)
drop if (maxchk==1)
drop maxchk chk

* omit same-sex households
gen idm = (demMaleFlag==0 & demAge>17)
gen idf = (demMaleFlag==1 & demAge>17)
by idHh idBu: egen msum = sum(idm)
by idHh idBu: egen fsum = sum(idf)
gen chkm = (msum>1)
gen chkf = (fsum>1)
drop if (chkm==1 | chkf==1)
drop idm idf msum fsum chkm chkf

* randomly select households from remaining sample
gen rnd = runiform()
gen chk = 0
replace chk = 1 if (idHh[_n-1]!=idHh & rnd < (1.0 / ($wealthEndYear - $wealthStartYear + 1.0)))
replace chk = chk[_n-1] if (idHh[_n-1]==idHh)
keep if (chk==1)
drop chk
save "$dir_data/temp1", replace


/********************************************************************************/
* de-identify all data
/********************************************************************************/
use "$dir_data/temp1", clear

// randomise order of households
sort idHh idBu
replace rnd = rnd[_n-1] if (idHh[_n-1]==idHh)
sort rnd idHh idBu
drop rnd

// annonymise idHh
gen idHhb = 0
replace idHhb = 1 if (_n==1)
replace idHhb = idHhb[_n-1] + (idHh!=idHh[_n-1]) if (_n>1)
order idHh idHhb
drop idHh
rename idHhb idHh
save "$dir_data/temp2", replace

// annonymise idBu
use "$dir_data/temp2", clear
gen idtmp = 0
replace idtmp = 1 if (_n==1)
replace idtmp = idtmp[_n-1] + (idBu!=idBu[_n-1]) if (_n>1)
order idtmp, a(idBu)
drop idBu
rename idtmp idBu
save "$dir_data/temp3", replace

// annonymise person identifiers
// uniquely identify person identiers
use "$dir_data/temp3", clear
replace idPers = idPers*100 + statCollectionWave
replace idPartner = idPartner*100 + statCollectionWave if (idPartner>0)
replace idMother = idMother*100 + statCollectionWave if (idMother>0)
replace idFather = idFather*100 + statCollectionWave if (idFather>0)
gen idtmp = 0
replace idtmp = 1 if (_n==1)
replace idtmp = idtmp[_n-1]+1 if (_n>1)
order idtmp, a(idPers)
save "$dir_data/temp4", replace

keep idtmp idPers
rename idtmp idtmp2
rename idPers idPartner
merge 1:m idPartner using "$dir_data/temp4", keep(2 3) nogen
order idPartner idtmp2, a(idtmp)
sort idHh idBu idtmp
save "$dir_data/temp5", replace

keep idPers idtmp
rename idPers idMother
rename idtmp idtmp3
merge 1:m idMother using "$dir_data/temp5", keep(2 3) nogen
order idMother idtmp3, a(idtmp2)
sort idHh idBu idtmp
save "$dir_data/temp6", replace

keep idPers idtmp
rename idPers idFather
rename idtmp idtmp4
merge 1:m idFather using "$dir_data/temp6", keep(2 3) nogen
order idFather idtmp4, a(idtmp3)
sort idHh idBu idtmp
save "$dir_data/temp7", replace

recode idtmp2 idtmp3 idtmp4 (mis=-9)
drop idPers idPartner idMother idFather
rename idtmp idPers
rename idtmp2 idPartner
rename idtmp3 idMother
rename idtmp4 idFather
save "$dir_data/temp8", replace

// adjust wave identifiers
gen statCollectionWaveb = 1
order statCollectionWaveb, a(statCollectionWave)
drop statCollectionWave
rename statCollectionWaveb statCollectionWave
gen stmb = $wealthEndYear
order stmb, a(statInterviewYear)
drop statInterviewYear
rename stmb statInterviewYear
save "$dir_data/temp9", replace

// adjust weights
gen ldwt = ln(wgtHhCross)
sum ldwt
gen dwt2 = round(exp(r(mean) + r(sd) * rnormal()))
order dwt2, a(wgtHhCross)
drop wgtHhCross ldwt
rename dwt2 wgtHhCross
save "$dir_data/temp10", replace

// adjust continuous variables
foreach vv of varlist yEmpPersGrossMonth yNonBenPersGrossMonth yMiscPersGrossMonth yPersAndPartnerGrossDiffMonth wealthTotValue wealthPensValue wealthPrptyValue wealthMortgageDebtValue ///
yDispMonth yCapitalPersMonth yPensPersGrossMonth careHrsFormal careHrsFromPartner careHrsFromDaughter careHrsFromSon careHrsFromOther careCareFormal aidhrs {
	gen tmp = `vv'
	order tmp, a(`vv')
	recode tmp (0=.)
	sum tmp
	replace tmp = tmp + r(sd) * rnormal() * 0.15 if (!missing(tmp))
	recode tmp (.=0)
	drop `vv'
	rename tmp `vv'
}
foreach vv of varlist yEmpPersGrossMonth yNonBenPersGrossMonth yMiscPersGrossMonth yCapitalPersMonth yPensPersGrossMonth careHrsFormal careHrsFromPartner careHrsFromDaughter ///
careHrsFromSon careHrsFromOther careCareFormal aidhrs {
	replace `vv' = 0 if (`vv'<0.0)
}
gen tmp = ln(labWageHrly)
order tmp, a(labWageHrly)
sum tmp
replace tmp = tmp + r(sd) * rnormal() * 0.15 if (!missing(tmp))
replace tmp = exp(tmp)
recode tmp (.=0)
drop labWageHrly labWageHrlyL1
rename tmp labWageHrly
gen labWageHrlyL1 = labWageHrly
order labWageHrlyL1, a(labWageHrly)
save "$dir_data/temp11", replace

// set benefit unit level variables
use "$dir_data/temp11", clear
foreach vv of varlist demRgn yHhQuintilesMonthC5 wealthPrptyFlag wgtHhCross wealthTotValue wealthPensValue wealthPrptyValue wealthMortgageDebtValue yDispMonth {
	
	rename `vv' `vv'i
	bys idBu: egen `vv' = mean(`vv'i)
	drop `vv'i
}


/**************************************************************************************
*	export training data
*************************************************************************************/
recode demMaleFlag wealthTotValue wealthPensValue wealthPrptyValue wealthMortgageDebtValue careNeedFlag careHrsFormal careHrsFromPartner ///
careHrsFromDaughter careHrsFromSon careHrsFromOther careCareFormal aidhrs careWho (-9=0)
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
