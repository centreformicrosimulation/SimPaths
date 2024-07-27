/**************************************************************************************
*
*	PROGRAM TO DIAGNOSE PROBLEMS WITH FORWARD PROJECTIONS FOR CARE
*
*	Last version:  Justin van de Ven, 17 Jul 2024
*	First version: Justin van de Ven, 17 Jul 2024
*
**************************************************************************************/

clear all
global basedir = "C:\MyFiles\99 DEV ENV\JAS-MINE\SimPaths\output\test1\csv"
global naivedir = "C:\MyFiles\99 DEV ENV\JAS-MINE\SimPaths\output\test2\csv"
global outdir = "C:\MyFiles\99 DEV ENV\JAS-MINE\SimPaths\analysis\"
cd "$outdir"


/**************************************************************************************
*	start
**************************************************************************************/

/**************************************************************************************
*	load data
**************************************************************************************/
import delimited using "$basedir/BenefitUnit.csv", clear
rename *, l
rename id_benefitunit idbenefitunit
gsort idbenefitunit time
gen nk = 0
forvalues ii = 0/17 {
	replace nk = nk + n_children_`ii'
}
save "$outdir/temp0", replace
import delimited using "$basedir/Person.csv", clear
rename *, l
rename id_person idperson
rename socialcareprovision socialcareprovision_p
gsort idbenefitunit time idperson
merge m:1 idbenefitunit time using temp0
gsort time idbenefitunit idperson
gen refbenefitunit = 0
replace refbenefitunit = 1 if (idbenefitunit != idbenefitunit[_n-1])

foreach vv of varlist carehoursfrompartnerweekly carehoursfromdaughterweekly carehoursfromsonweekly carehoursfromotherweekly carehoursfromparentweekly carehoursfromformalweekly {
	destring `vv', replace force
	recode `vv' (missing=0)
}

destring hoursworkedweekly, replace force
recode hoursworkedweekly (missing=0)
save "$outdir/test1", replace


import delimited using "$naivedir/BenefitUnit.csv", clear
rename *, l
rename id_benefitunit idbenefitunit
gsort idbenefitunit time
gen nk = 0
forvalues ii = 0/17 {
	replace nk = nk + n_children_`ii'
}
save "$outdir/temp0", replace
import delimited using "$naivedir/Person.csv", clear
rename *, l
rename id_person idperson
rename socialcareprovision socialcareprovision_p
gsort idbenefitunit time idperson
merge m:1 idbenefitunit time using temp0
gsort time idbenefitunit idperson
gen refbenefitunit = 0
replace refbenefitunit = 1 if (idbenefitunit != idbenefitunit[_n-1])

foreach vv of varlist carehoursfrompartnerweekly carehoursfromdaughterweekly carehoursfromsonweekly carehoursfromotherweekly carehoursfromparentweekly carehoursfromformalweekly {
	destring `vv', replace force
	recode `vv' (missing=0)
}

destring hoursworkedweekly, replace force
recode hoursworkedweekly (missing=0)
save "$outdir/test2", replace


/**************************************************************************************
*	analysis
**************************************************************************************/
use "$outdir/test_base_noalign", clear
order time idhousehold idbenefitunit idperson dag sampleentry sampleexit dcpst idpartner nk
gsort idhousehold idbenefitunit time idperson
drop run


/*******************************************************************************
*	clean-up
*******************************************************************************/
#delimit ;
local files_to_drop 
	temp0.dta
	temp1.dta
	;
#delimit cr // cr stands for carriage return

foreach file of local files_to_drop { 
	erase "$outdir/`file'"
}

