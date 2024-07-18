/**************************************************************************************
*
*	PROGRAM TO ANALYSE FORWARD PROJECTIONS FOR CARE
*
*	Last version:  Justin van de Ven, 18 Jun 2024
*	First version: Justin van de Ven, 18 Jun 2024
*
**************************************************************************************/

clear all
global moddir = "C:\MyFiles\99 DEV ENV\JAS-MINE\SimPaths\output\base\csv"
global outdir = "C:\MyFiles\99 DEV ENV\JAS-MINE\SimPaths\analysis\"
cd "$outdir"


/**************************************************************************************
*	start
**************************************************************************************/

/**************************************************************************************
*	load data
**************************************************************************************/
import delimited using "$moddir/BenefitUnit.csv", clear
rename *, l
rename id_benefitunit idbenefitunit
gsort idbenefitunit time
gen nk = 0
forvalues ii = 0/17 {
	replace nk = nk + n_children_`ii'
}
save "$outdir/temp0", replace
import delimited using "$moddir/Person.csv", clear
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
gen idNotEmployedAdult = (hoursworkedweekly<0.1 & dag>17)

gen led = (deh_c3=="Low")
gen med = (deh_c3=="Medium")
gen hed = (deh_c3=="High")

gen male = (dgn=="Male")

gen partnered = (dcpst=="Partnered")

gen needCare = (needsocialcare=="True")
gen informalCareHours = carehoursfrompartnerweekly + carehoursfromdaughterweekly + carehoursfromsonweekly + carehoursfromotherweekly + carehoursfromparentweekly
gen totalCareHours = informalCareHours + carehoursfromformalweekly
gen recCare = (totalCareHours>0.01)

save "$outdir/temp1", replace


/**************************************************************************************
*	childcare
**************************************************************************************/
use "$outdir/temp1", clear
tab time						// all people
tab time if (dag>17)			// population aged 18+
tab time if (refbenefitunit)	// benefit units
tab time if (nk>0 & refbenefitunit)	// benefit units with children
gen incidenceChildcare = (childcarecostperweek>0)
tab time if (incidenceChildcare & refbenefitunit)		// benefit units paying childcare

gen avChildAge = 0
forvalues aa = 0/17 {
	replace avChildAge = avChildAge + `aa' * n_children_`aa'
}
replace avChildAge = avChildAge / nk if (nk>0)
matrix store1 = J(2069-2018,1,.)
forvalues yy = 2019/2069 {
	sum avChildAge if (time==`yy' & nk>0 & refbenefitunit), mean
	mat store1[`yy'-2018,1] = r(mean)
}
matlist store1

matrix store1 = J(2069-2018,1,.)
gen cc2024prices = childcarecostperweek * 1.2663	// from 2015 prices
forvalues yy = 2019/2069 {
	sum cc2024prices if (time==`yy' & incidenceChildcare & refbenefitunit), mean
	mat store1[`yy'-2018,1] = r(mean)
}
matlist store1

by time idbenefitunit: egen nNotEmpAdult = sum(idNotEmployedAdult)
by time idbenefitunit: egen maxAge = max(dag)

matrix store1 = J(2069-2018,1,.)
gen chk = (nNotEmpAdult>0)
forvalues yy = 2019/2069 {
	sum chk if (time==`yy' & refbenefitunit), mean
	mat store1[`yy'-2018,1] = r(mean)
}
matlist store1

forvalues yy = 2019/2069 {
	sum chk if (time==`yy' & nk>0 & refbenefitunit), mean
	mat store1[`yy'-2018,1] = r(mean)
}
matlist store1

forvalues yy = 2019/2069 {
	sum chk if (time==`yy' & maxAge<55 & refbenefitunit), mean
	mat store1[`yy'-2018,1] = r(mean)
}
matlist store1


/*******************************************************************************
*	social care need
*******************************************************************************/
use "$outdir/temp1", clear
tab time if (dag>44 & dag<65)
tab time if (needCare==1 & dag>44 & dag<65)

tab time if (dag>64 & dag<80)
tab time if (needCare==1 & dag>64 & dag<80)

tab time if (dag>79)
tab time if (needCare==1 & dag>79)

gen dhev = 0
replace dhev = 1 if (dhe=="Poor")
replace dhev = 2 if (dhe=="Fair")
replace dhev = 3 if (dhe=="Good")
replace dhev = 4 if (dhe=="VeryGood")
replace dhev = 5 if (dhe=="Excellent")

/*************** aged 45 to 64 ******************/
matrix store1 = J(2069-2018,1,.)
forvalues yy = 2019/2069 {
	sum male if (time==`yy' & dag>44 & dag<65), mean
	mat store1[`yy'-2018,1] = r(mean)
}
matlist store1

forvalues yy = 2019/2069 {
	sum hed if (time==`yy' & dag>44 & dag<65), mean
	mat store1[`yy'-2018,1] = r(mean)
}
matlist store1

forvalues yy = 2019/2069 {
	sum led if (time==`yy' & dag>44 & dag<65), mean
	mat store1[`yy'-2018,1] = r(mean)
}
matlist store1

forvalues yy = 2019/2069 {
	sum partnered if (time==`yy' & dag>44 & dag<65), mean
	mat store1[`yy'-2018,1] = r(mean)
}
matlist store1

forvalues yy = 2019/2069 {
	sum dag if (time==`yy' & dag>44 & dag<65), mean
	mat store1[`yy'-2018,1] = r(mean)
}
matlist store1

forvalues yy = 2019/2069 {
	sum dhev if (time==`yy' & dag>44 & dag<65), mean
	mat store1[`yy'-2018,1] = r(mean)
}
matlist store1

/*************** aged 65 to 79 ******************/
forvalues yy = 2019/2069 {
	sum male if (time==`yy' & dag>64 & dag<80), mean
	mat store1[`yy'-2018,1] = r(mean)
}
matlist store1

forvalues yy = 2019/2069 {
	sum hed if (time==`yy' & dag>64 & dag<80), mean
	mat store1[`yy'-2018,1] = r(mean)
}
matlist store1

forvalues yy = 2019/2069 {
	sum led if (time==`yy' & dag>64 & dag<80), mean
	mat store1[`yy'-2018,1] = r(mean)
}
matlist store1

forvalues yy = 2019/2069 {
	sum partnered if (time==`yy' & dag>64 & dag<80), mean
	mat store1[`yy'-2018,1] = r(mean)
}
matlist store1

forvalues yy = 2019/2069 {
	sum dag if (time==`yy' & dag>64 & dag<80), mean
	mat store1[`yy'-2018,1] = r(mean)
}
matlist store1

forvalues yy = 2019/2069 {
	sum dhev if (time==`yy' & dag>64 & dag<80), mean
	mat store1[`yy'-2018,1] = r(mean)
}
matlist store1

/*************** aged 80 and over ******************/
forvalues yy = 2019/2069 {
	sum male if (time==`yy' & dag>79), mean
	mat store1[`yy'-2018,1] = r(mean)
}
matlist store1

forvalues yy = 2019/2069 {
	sum hed if (time==`yy' & dag>79), mean
	mat store1[`yy'-2018,1] = r(mean)
}
matlist store1

forvalues yy = 2019/2069 {
	sum led if (time==`yy' & dag>79), mean
	mat store1[`yy'-2018,1] = r(mean)
}
matlist store1

forvalues yy = 2019/2069 {
	sum partnered if (time==`yy' & dag>79), mean
	mat store1[`yy'-2018,1] = r(mean)
}
matlist store1

forvalues yy = 2019/2069 {
	sum dag if (time==`yy' & dag>79), mean
	mat store1[`yy'-2018,1] = r(mean)
}
matlist store1

forvalues yy = 2019/2069 {
	sum dhev if (time==`yy' & dag>79), mean
	mat store1[`yy'-2018,1] = r(mean)
}
matlist store1


/*******************************************************************************
*	social care receipt
*******************************************************************************/
use "$outdir/temp1", clear

tab time if (recCare & dag>44 & dag<65)
tab time if (recCare & dag>64 & dag<80)
tab time if (recCare & needCare & dag>64 & dag<80)

tab time if (recCare & dag>79)
tab time if (recCare & needCare & dag>79)

matrix store1 = J(2069-2018,1,.)
forvalues yy = 2019/2069 {
	sum totalCareHours if (time==`yy' & dag>44 & dag<65 & recCare), mean
	mat store1[`yy'-2018,1] = r(mean)
}
matlist store1

forvalues yy = 2019/2069 {
	sum totalCareHours if (time==`yy' & dag>64 & dag<80 & recCare), mean
	mat store1[`yy'-2018,1] = r(mean)
}
matlist store1

forvalues yy = 2019/2069 {
	sum totalCareHours if (time==`yy' & dag>79 & recCare), mean
	mat store1[`yy'-2018,1] = r(mean)
}
matlist store1

tab time if (informalCareHours>0.1 & carehoursfromformalweekly<0.1)
tab time if (informalCareHours>0.1 & carehoursfromformalweekly>0.1)
tab time if (informalCareHours<0.1 & carehoursfromformalweekly>0.1)

matrix store1 = J(2069-2018,6,.)
gen chk = (dag>44)
forvalues yy = 2019/2069 {
	sum carehoursfromparentweekly if (time==`yy' & chk), mean
	mat store1[`yy'-2018,1] = r(mean)
	sum carehoursfrompartnerweekly if (time==`yy' & chk), mean
	mat store1[`yy'-2018,2] = r(mean)
	sum carehoursfromdaughterweekly if (time==`yy' & chk), mean
	mat store1[`yy'-2018,3] = r(mean)
	sum carehoursfromsonweekly if (time==`yy' & chk), mean
	mat store1[`yy'-2018,4] = r(mean)
	sum carehoursfromotherweekly if (time==`yy' & chk), mean
	mat store1[`yy'-2018,5] = r(mean)
	sum carehoursfromformalweekly if (time==`yy' & chk), mean
	mat store1[`yy'-2018,6] = r(mean)
}
drop chk
matlist store1


/*******************************************************************************
*	social care provision
*******************************************************************************/
use "$outdir/temp1", clear

tab time if (socialcareprovision_p=="OnlyOther")
tab time if (socialcareprovision_p=="OnlyPartner")
tab time if (socialcareprovision_p=="PartnerAndOther")

matrix store1 = J(2069-2018,1,.)
forvalues yy = 2019/2069 {
	sum carehoursprovidedweekly if (time==`yy' & socialcareprovision_p!="None"), mean
	mat store1[`yy'-2018,1] = r(mean)
}
matlist store1


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

