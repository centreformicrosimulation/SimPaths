/**************************************************************************************
*
*	PROGRAM TO ANALYSE FORWARD PROJECTIONS FOR CARE
*
*	Last version:  Justin van de Ven, 18 Jun 2024
*	First version: Justin van de Ven, 18 Jun 2024
*
**************************************************************************************/

clear all
global basedir = "C:\MyFiles\99 DEV ENV\JAS-MINE\SimPaths\output\base1\csv"
global zerocostsdir = "C:\MyFiles\99 DEV ENV\JAS-MINE\SimPaths\output\zero costs\csv"
global naivedir = "C:\MyFiles\99 DEV ENV\JAS-MINE\SimPaths\output\naive expectations\csv"
global outdir = "C:\MyFiles\99 DEV ENV\JAS-MINE\SimPaths\analysis\"
cd "$outdir"


/**************************************************************************************
*	start
**************************************************************************************/

/**************************************************************************************
*	load data
**************************************************************************************/

//////// BASE ////////////////////////
import delimited using "$basedir/BenefitUnit.csv", clear
rename *, l
rename id_benefitunit idbenefitunit
gsort idbenefitunit time
gen nk = 0
forvalues ii = 0/17 {
	replace nk = nk + n_children_`ii'
}
save "$outdir/base0", replace
import delimited using "$basedir/Person.csv", clear
rename *, l
rename id_person idperson
rename socialcareprovision socialcareprovision_p
gsort idbenefitunit time idperson
merge m:1 idbenefitunit time using base0
drop _merge
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

save "$outdir/base1", replace


//////// ZERO COSTS ////////////////////////
import delimited using "$zerocostsdir/BenefitUnit.csv", clear
rename *, l
rename id_benefitunit idbenefitunit
gsort idbenefitunit time
gen nk = 0
forvalues ii = 0/17 {
	replace nk = nk + n_children_`ii'
}
save "$outdir/zero0", replace
import delimited using "$zerocostsdir/Person.csv", clear
rename *, l
rename id_person idperson
rename socialcareprovision socialcareprovision_p
gsort idbenefitunit time idperson
merge m:1 idbenefitunit time using zero0
drop _merge
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

save "$outdir/zero1", replace


//////// NAIVE ////////////////////////
import delimited using "$naivedir/BenefitUnit.csv", clear
rename *, l
rename id_benefitunit idbenefitunit
gsort idbenefitunit time
gen nk = 0
forvalues ii = 0/17 {
	replace nk = nk + n_children_`ii'
}
save "$outdir/naive0", replace
import delimited using "$naivedir/Person.csv", clear
rename *, l
rename id_person idperson
rename socialcareprovision socialcareprovision_p
gsort idbenefitunit time idperson
merge m:1 idbenefitunit time using naive0
drop _merge
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

save "$outdir/naive1", replace


/**************************************************************************************
*	childcare
**************************************************************************************/

use "$outdir/base1", clear
gsort idbenefitunit time idperson
drop if (dag<18)
by idbenefitunit time: egen hoursEmpBu = sum(hoursworkedweekly)
gen employed = (les_c4=="EmployedOrSelfEmployed")
by idbenefitunit time: egen noEmpBu = sum(employed)
gen chk = (dgn=="Female" & dag>17 & dag<38)
by idbenefitunit time: egen fertile = max(chk)
drop chk
gen chk = (dlltsd=="True")
by idbenefitunit time: egen dbld = max(chk)
drop chk
by idbenefitunit time: egen careHours = sum(carehoursprovidedweekly)
gen nk04 = 0
forvalues ii = 0/4 {
	replace nk04 = nk04 + n_children_`ii'
}
gen nk1517 = 0
forvalues ii = 15/17 {
	replace nk1517 = nk1517 + n_children_`ii'
}

matrix store1 = J(10,1,.)
//gen target = (nk==0) * fertile				// anticipation1
//gen target = (nk==0) * fertile * (1-dbld)		// anticipation2
//gen target = (nk==0) * fertile * (1-dbld) * (careHours<0.1)		// anticipation3
//gen target = (nk04==1)*(nk04==nk)				// impact1
//gen target = (nk04>0)*(nk04==nk)				// impact2
//gen target = (nk04>0)*(nk04==nk) * (1-dbld)	// impact3
//gen target = (nk04>0)*(nk04==nk) * (1-dbld) * (careHours<0.1)	// impact4
gen target = (nk1517>0)*(nk1517==nk) * (1-dbld) * (careHours<0.1)	// scaring

local ii = 1
sum hoursEmpBu if (refbenefitunit & target), mean
mat store1[`ii',1] = r(mean)
local ii = `ii' + 1
sum noEmpBu if (refbenefitunit & target), mean
mat store1[`ii',1] = r(mean)
local ii = `ii' + 1
sum partnered if (refbenefitunit & target), mean
mat store1[`ii',1] = r(mean)
local ii = `ii' + 1
sum dbld if (refbenefitunit & target), mean
mat store1[`ii',1] = r(mean)
local ii = `ii' + 1
sum careHours if (refbenefitunit & target), mean
mat store1[`ii',1] = r(mean)
local ii = `ii' + 1
sum childcarecostperweek if (refbenefitunit & target), mean
mat store1[`ii',1] = r(mean)
local ii = `ii' + 1
sum careformalexpenditureweekly if (refbenefitunit & target), mean
mat store1[`ii',1] = r(mean)
local ii = `ii' + 1
sum discretionaryconsumptionperyear if (refbenefitunit & target), mean
mat store1[`ii',1] = r(mean)
local ii = `ii' + 1
sum disposableincomemonthly if (refbenefitunit & target), mean
mat store1[`ii',1] = r(mean)
local ii = `ii' + 1
sum liquidwealth if (refbenefitunit & target), mean
mat store1[`ii',1] = r(mean)
local ii = `ii' + 1

matlist store1


/**************************************************************************************
*	childcare - anticipation effects
*	women aged 18 to 28 without children
**************************************************************************************/

use "$outdir/zero1", clear
keep if (dgn=="Female" & dag>17 & dag<29 & time>2023 & nk==0)

// keep observations with full histories (balanced population)
gsort idperson time
by idperson: egen obs = count(idperson)
keep if (obs==11)
drop obs

// generate variables
order idperson dag time dcpst nk n_children_0 n_children_1 n_children_2 n_children_3 n_children_4 n_children_5 n_children_6 n_children_7 n_children_8 n_children_9 n_children_10 n_children_11 n_children_12 n_children_13 n_children_14 n_children_15 n_children_15 n_children_17
gen employed = (les_c4=="EmployedOrSelfEmployed")
gen notemployed = (les_c4=="NotEmployed")
gen student = (les_c4=="Student")
gen lowWage = (lowwageoffer=="true")
gen dbld = (dlltsd=="True")
gen employedsp = (lessp_c4=="EmployedOrSelfEmployed")
gen careProvider = (socialcareprovision_p!="None")

gen dhev = 0
replace dhev = 1 if (dhe=="Poor")
replace dhev = 2 if (dhe=="Fair")
replace dhev = 3 if (dhe=="Good")
replace dhev = 4 if (dhe=="VeryGood")
replace dhev = 5 if (dhe=="Excellent")

// analyse employment, consumption and saving
matrix store1 = J(28-17,21,.)
forvalues aa = 18/28 {
	local jj = 1
	sum hoursworkedweekly if (dag==`aa'), mean
	mat store1[`aa'-17,`jj'] = r(mean)

	local jj = `jj' + 1
	sum employed if (dag==`aa'), mean
	mat store1[`aa'-17,`jj'] = r(mean)

	local jj = `jj' + 1
	sum notemployed if (dag==`aa'), mean
	mat store1[`aa'-17,`jj'] = r(mean)

	local jj = `jj' + 1
	sum student if (dag==`aa'), mean
	mat store1[`aa'-17,`jj'] = r(mean)

	local jj = `jj' + 1
	sum lowWage if (dag==`aa'), mean
	mat store1[`aa'-17,`jj'] = r(mean)

	local jj = `jj' + 1
	sum partnered if (dag==`aa'), mean
	mat store1[`aa'-17,`jj'] = r(mean)

	local jj = `jj' + 1
	sum dhev if (dag==`aa'), mean
	mat store1[`aa'-17,`jj'] = r(mean)

	local jj = `jj' + 1
	sum dbld if (dag==`aa'), mean
	mat store1[`aa'-17,`jj'] = r(mean)
	
	local jj = `jj' + 1
	sum needCare if (dag==`aa'), mean
	mat store1[`aa'-17,`jj'] = r(mean)
	
	local jj = `jj' + 1
	sum carehoursprovidedweekly if (dag==`aa'), mean
	mat store1[`aa'-17,`jj'] = r(mean)
	
	local jj = `jj' + 1
	sum careProvider if (dag==`aa'), mean
	mat store1[`aa'-17,`jj'] = r(mean)
	
	local jj = `jj' + 1
	sum employedsp if (dag==`aa'), mean
	mat store1[`aa'-17,`jj'] = r(mean)
	
	local jj = `jj' + 1
	sum yearlyequivalisedconsumption if (dag==`aa'), mean
	mat store1[`aa'-17,`jj'] = r(mean)
	
	local jj = `jj' + 1
	sum childcarecostperweek if (dag==`aa'), mean
	mat store1[`aa'-17,`jj'] = r(mean)
	
	local jj = `jj' + 1
	sum discretionaryconsumptionperyear if (dag==`aa'), mean
	mat store1[`aa'-17,`jj'] = r(mean)
	
	local jj = `jj' + 1
	sum equivaliseddisposableincomeyearl if (dag==`aa'), mean
	mat store1[`aa'-17,`jj'] = r(mean)
	
	local jj = `jj' + 1
	sum grossincomemonthly if (dag==`aa'), mean
	mat store1[`aa'-17,`jj'] = r(mean)
	
	local jj = `jj' + 1
	sum liquidwealth if (dag==`aa'), mean
	mat store1[`aa'-17,`jj'] = r(mean)
	
	local jj = `jj' + 1
	sum led if (dag==`aa'), mean
	mat store1[`aa'-17,`jj'] = r(mean)
	
	local jj = `jj' + 1
	sum med if (dag==`aa'), mean
	mat store1[`aa'-17,`jj'] = r(mean)
	
	local jj = `jj' + 1
	sum hed if (dag==`aa'), mean
	mat store1[`aa'-17,`jj'] = r(mean)
}
matlist store1



gsort idperson time
gen birthAge29i = (dag==29 & n_children_0==1)
by idperson: egen birthAge29 = max(birthAge29i)
keep if (dgn=="Female" & dag>28 & dag<36 & time>2023 & nk==1 & birthAge29)

// keep observations with full histories (balanced population)
by idperson: egen obs = count(idperson)
tab obs
order idperson dag time obs dcpst nk n_children_0 n_children_1 n_children_2 n_children_3 n_children_4 n_children_5 n_children_6 n_children_7 n_children_8 n_children_9 n_children_10 n_children_11 n_children_12 n_children_13 n_children_14 n_children_15 
//keep if (obs==15)
drop obs

// generate variables
gen employed = (les_c4=="EmployedOrSelfEmployed")
gen notemployed = (les_c4=="NotEmployed")
gen student = (les_c4=="Student")
gen lowWage = (lowwageoffer=="true")
gen dbld = (dlltsd=="True")
gen employedsp = (lessp_c4=="EmployedOrSelfEmployed")
gen careProvider = (socialcareprovision_p!="None")

gen dhev = 0
replace dhev = 1 if (dhe=="Poor")
replace dhev = 2 if (dhe=="Fair")
replace dhev = 3 if (dhe=="Good")
replace dhev = 4 if (dhe=="VeryGood")
replace dhev = 5 if (dhe=="Excellent")

// analyse employment, consumption and saving
matrix store1 = J(43-28,21,.)
forvalues aa = 29/43 {
	local jj = 1
	sum hoursworkedweekly if (dag==`aa'), mean
	mat store1[`aa'-28,`jj'] = r(mean)

	local jj = `jj' + 1
	sum employed if (dag==`aa'), mean
	mat store1[`aa'-28,`jj'] = r(mean)

	local jj = `jj' + 1
	sum notemployed if (dag==`aa'), mean
	mat store1[`aa'-28,`jj'] = r(mean)

	local jj = `jj' + 1
	sum student if (dag==`aa'), mean
	mat store1[`aa'-28,`jj'] = r(mean)

	local jj = `jj' + 1
	sum lowWage if (dag==`aa'), mean
	mat store1[`aa'-28,`jj'] = r(mean)

	local jj = `jj' + 1
	sum partnered if (dag==`aa'), mean
	mat store1[`aa'-28,`jj'] = r(mean)

	local jj = `jj' + 1
	sum dhev if (dag==`aa'), mean
	mat store1[`aa'-28,`jj'] = r(mean)

	local jj = `jj' + 1
	sum dbld if (dag==`aa'), mean
	mat store1[`aa'-28,`jj'] = r(mean)
	
	local jj = `jj' + 1
	sum needCare if (dag==`aa'), mean
	mat store1[`aa'-28,`jj'] = r(mean)
	
	local jj = `jj' + 1
	sum carehoursprovidedweekly if (dag==`aa'), mean
	mat store1[`aa'-28,`jj'] = r(mean)
	
	local jj = `jj' + 1
	sum careProvider if (dag==`aa'), mean
	mat store1[`aa'-28,`jj'] = r(mean)
	
	local jj = `jj' + 1
	sum employedsp if (dag==`aa'), mean
	mat store1[`aa'-28,`jj'] = r(mean)
	
	local jj = `jj' + 1
	sum yearlyequivalisedconsumption if (dag==`aa'), mean
	mat store1[`aa'-28,`jj'] = r(mean)
	
	local jj = `jj' + 1
	sum childcarecostperweek if (dag==`aa'), mean
	mat store1[`aa'-28,`jj'] = r(mean)
	
	local jj = `jj' + 1
	sum discretionaryconsumptionperyear if (dag==`aa'), mean
	mat store1[`aa'-28,`jj'] = r(mean)
	
	local jj = `jj' + 1
	sum equivaliseddisposableincomeyearl if (dag==`aa'), mean
	mat store1[`aa'-28,`jj'] = r(mean)
	
	local jj = `jj' + 1
	sum grossincomemonthly if (dag==`aa'), mean
	mat store1[`aa'-28,`jj'] = r(mean)
	
	local jj = `jj' + 1
	sum liquidwealth if (dag==`aa'), mean
	mat store1[`aa'-28,`jj'] = r(mean)
	
	local jj = `jj' + 1
	sum led if (dag==`aa'), mean
	mat store1[`aa'-28,`jj'] = r(mean)
	
	local jj = `jj' + 1
	sum med if (dag==`aa'), mean
	mat store1[`aa'-28,`jj'] = r(mean)
	
	local jj = `jj' + 1
	sum hed if (dag==`aa'), mean
	mat store1[`aa'-28,`jj'] = r(mean)
}
matlist store1









// analyse zero data
use "$outdir/naive1", clear

// identify women who have their first child at age 29 between 2030 and 2039
gen chk1 = (dgn=="Female")*(dag==29)*(time>2033)*(time<2044)*(n_children_0==1)*(nk==1)
// keep only women who have just one child 
gsort idperson time
by idperson: egen chk2 = max(chk1)
keep if chk2==1
drop chk*
order idperson dag time dcpst nk n_children_0 n_children_1 n_children_2 n_children_3 n_children_4 n_children_5 n_children_6 n_children_7 n_children_8 n_children_9 n_children_10 n_children_11 n_children_12 n_children_13 n_children_14 n_children_15 n_children_15 n_children_17
drop if dag<18
by idperson: egen chk = max(nk)
drop if (chk!=1)
drop chk
gen ref = 0
replace ref = 1 if (idperson!=idperson[_n-1])
save "$outdir/childcarenaive", replace

// generate variables
use "$outdir/childcarenaive", clear
gen employed = (les_c4=="EmployedOrSelfEmployed")
gen notemployed = (les_c4=="NotEmployed")
gen student = (les_c4=="Student")
gen lowWage = (lowwageoffer=="true")
gen dbld = (dlltsd=="True")
gen employedsp = (lessp_c4=="EmployedOrSelfEmployed")
gen careProvider = (socialcareprovision_p!="None")

gen dhev = 0
replace dhev = 1 if (dhe=="Poor")
replace dhev = 2 if (dhe=="Fair")
replace dhev = 3 if (dhe=="Good")
replace dhev = 4 if (dhe=="VeryGood")
replace dhev = 5 if (dhe=="Excellent")


// analyse employment, consumption and saving
matrix store1 = J(54-17,18,.)
forvalues aa = 18/54 {
	local jj = 1
	sum hoursworkedweekly if (dag==`aa'), mean
	mat store1[`aa'-17,`jj'] = r(mean)

	local jj = `jj' + 1
	sum employed if (dag==`aa'), mean
	mat store1[`aa'-17,`jj'] = r(mean)

	local jj = `jj' + 1
	sum notemployed if (dag==`aa'), mean
	mat store1[`aa'-17,`jj'] = r(mean)

	local jj = `jj' + 1
	sum student if (dag==`aa'), mean
	mat store1[`aa'-17,`jj'] = r(mean)

	local jj = `jj' + 1
	sum lowWage if (dag==`aa'), mean
	mat store1[`aa'-17,`jj'] = r(mean)

	local jj = `jj' + 1
	sum partnered if (dag==`aa'), mean
	mat store1[`aa'-17,`jj'] = r(mean)

	local jj = `jj' + 1
	sum dhev if (dag==`aa'), mean
	mat store1[`aa'-17,`jj'] = r(mean)

	local jj = `jj' + 1
	sum dbld if (dag==`aa'), mean
	mat store1[`aa'-17,`jj'] = r(mean)
	
	local jj = `jj' + 1
	sum needCare if (dag==`aa'), mean
	mat store1[`aa'-17,`jj'] = r(mean)
	
	local jj = `jj' + 1
	sum carehoursprovidedweekly if (dag==`aa'), mean
	mat store1[`aa'-17,`jj'] = r(mean)
	
	local jj = `jj' + 1
	sum careProvider if (dag==`aa'), mean
	mat store1[`aa'-17,`jj'] = r(mean)
	
	local jj = `jj' + 1
	sum employedsp if (dag==`aa'), mean
	mat store1[`aa'-17,`jj'] = r(mean)
	
	local jj = `jj' + 1
	sum yearlyequivalisedconsumption if (dag==`aa'), mean
	mat store1[`aa'-17,`jj'] = r(mean)
	
	local jj = `jj' + 1
	sum childcarecostperweek if (dag==`aa'), mean
	mat store1[`aa'-17,`jj'] = r(mean)
	
	local jj = `jj' + 1
	sum discretionaryconsumptionperyear if (dag==`aa'), mean
	mat store1[`aa'-17,`jj'] = r(mean)
	
	local jj = `jj' + 1
	sum equivaliseddisposableincomeyearl if (dag==`aa'), mean
	mat store1[`aa'-17,`jj'] = r(mean)
	
	local jj = `jj' + 1
	sum grossincomemonthly if (dag==`aa'), mean
	mat store1[`aa'-17,`jj'] = r(mean)
	
	local jj = `jj' + 1
	sum liquidwealth if (dag==`aa'), mean
	mat store1[`aa'-17,`jj'] = r(mean)
}
matlist store1










use "$outdir/zero1", clear

// identify women who have their first child at age 29 between 2030 and 2039
gen chk1 = (dgn=="Female")*(dag==29)*(time>2033)*(time<2044)*(n_children_0==1)*(nk==1)

gsort idperson time
by idperson: egen chk2 = max(chk1)
keep if chk2==1
drop chk*
order idperson dag time dcpst nk n_children_0 n_children_1 n_children_2 n_children_3 n_children_4 n_children_5 n_children_6 n_children_7 n_children_8 n_children_9 n_children_10 n_children_11 n_children_12 n_children_13 n_children_14 n_children_15 n_children_15 n_children_17
drop if dag<18
by idperson: egen chk = max(nk)
drop if (chk!=1)
drop chk
gen ref = 0
replace ref = 1 if (idperson!=idperson[_n-1])

save "$outdir/childcarezero", replace



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
*	in need of social care
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

