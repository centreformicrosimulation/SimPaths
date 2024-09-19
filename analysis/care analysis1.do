/**************************************************************************************
*
*	PROGRAM TO ANALYSE FORWARD PROJECTIONS FOR CARE
*
*	Last version:  Justin van de Ven, 18 Jun 2024
*	First version: Justin van de Ven, 18 Jun 2024
*
**************************************************************************************/

clear all
global moddir = "C:\MyFiles\99 DEV ENV\JAS-MINE\SimPaths\output\sc analysis1\csv"
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

gen idna = (dag>17)
gen idnk = (dag<18)
bys time idbenefitunit: egen partnered = sum(idna)
replace partnered = partnered - 1
bys time idbenefitunit: egen nk = sum(idnk)

gen needCare = (needsocialcare=="True")
gen informalCareHours = carehoursfrompartnerweekly + carehoursfromdaughterweekly + carehoursfromsonweekly + carehoursfromotherweekly + carehoursfromparentweekly
gen totalCareHours = informalCareHours + carehoursfromformalweekly
gen recCare = (totalCareHours>0.01)

save "$outdir/temp1", replace


/**************************************************************************************
*	childcare
**************************************************************************************/
use "$outdir/temp1", clear

// block 1 statistics
matrix store1 = J(2070-2018,3,.)
forvalues yy = 2019/2070 {
	qui{
		count if (time==`yy')
		mat store1[`yy'-2018,1] = r(N)
		count if (time==`yy' & dag>17)
		mat store1[`yy'-2018,2] = r(N)
		count if (time==`yy' & dag<18)
		mat store1[`yy'-2018,3] = r(N)
	}
}
matlist store1

// block 2 statistics
matrix store2 = J(2070-2018,3,.)
gen incidenceChildcare = (childcarecostperweek>0)
forvalues yy = 2019/2070 {
	qui{
		count if (time==`yy' & refbenefitunit)
		mat store2[`yy'-2018,1] = r(N)
		count if (time==`yy' & refbenefitunit & nk>0)
		mat store2[`yy'-2018,2] = r(N)
		count if (time==`yy' & refbenefitunit & incidenceChildcare)
		mat store2[`yy'-2018,3] = r(N)
	}
}
matlist store2

gen avChildAge = 0
forvalues aa = 0/17 {
	qui {
		gen child`aa' = (dag==`aa')
		bys time idbenefitunit: egen n_children_`aa' = sum(child`aa')
		replace avChildAge = avChildAge + `aa' * n_children_`aa'
	}
}
replace avChildAge = avChildAge / nk if (nk>0)
gen cc2024prices = childcarecostperweek * 1.2663	// from 2015 prices
by time idbenefitunit: egen nNotEmpAdult = sum(idNotEmployedAdult)
by time idbenefitunit: egen maxAge = max(dag)
gen chk = (nNotEmpAdult>0)

// block 3 statistics
matrix store3 = J(2070-2018,6,.)
forvalues yy = 2019/2070 {
	qui{
		sum avChildAge if (time==`yy' & nk>0 & refbenefitunit), mean
		mat store3[`yy'-2018,1] = r(mean)
		sum nk if (time==`yy' & nk>0 & refbenefitunit), mean
		mat store3[`yy'-2018,2] = r(mean)
		sum cc2024prices if (time==`yy' & incidenceChildcare & refbenefitunit), mean
		mat store3[`yy'-2018,3] = r(mean)
		sum chk if (time==`yy' & refbenefitunit), mean
		mat store3[`yy'-2018,4] = r(mean)
		sum chk if (time==`yy' & nk>0 & refbenefitunit), mean
		mat store3[`yy'-2018,5] = r(mean)
		sum chk if (time==`yy' & maxAge<55 & refbenefitunit), mean
		mat store3[`yy'-2018,6] = r(mean)
	}
}
matlist store3
drop chk


/*******************************************************************************
*	social care need
*******************************************************************************/
use "$outdir/temp1", clear

// block 4 statistics
matrix store4 = J(2070-2018,6,.)
forvalues yy = 2019/2070 {
	qui{
		count if (time==`yy' & dag>44 & dag<65)
		mat store4[`yy'-2018,1] = r(N)
		count if (time==`yy' & dag>44 & dag<65 & needCare)
		mat store4[`yy'-2018,2] = r(N)
		count if (time==`yy' & dag>64 & dag<80)
		mat store4[`yy'-2018,3] = r(N)
		count if (time==`yy' & dag>64 & dag<80 & needCare)
		mat store4[`yy'-2018,4] = r(N)
		count if (time==`yy' & dag>79)
		mat store4[`yy'-2018,5] = r(N)
		count if (time==`yy' & dag>79 & needCare)
		mat store4[`yy'-2018,6] = r(N)
	}
}
matlist store4

gen dhev = 0
replace dhev = 1 if (dhe=="Poor")
replace dhev = 2 if (dhe=="Fair")
replace dhev = 3 if (dhe=="Good")
replace dhev = 4 if (dhe=="VeryGood")
replace dhev = 5 if (dhe=="Excellent")

/*************** aged 45 to 64 ******************/
// block 5 statistics
matrix store5 = J(2070-2018,3,.)
gen id = (dag>44 & dag<65)
forvalues yy = 2019/2070 {
	qui {
		local jj = 1
		sum hed if (time==`yy' & id), mean
		mat store5[`yy'-2018,`jj'] = r(mean)
		local jj = `jj' + 1
		sum led if (time==`yy' & id), mean
		mat store5[`yy'-2018,`jj'] = r(mean)
		local jj = `jj' + 1
		sum partnered if (time==`yy' & id), mean
		mat store5[`yy'-2018,`jj'] = r(mean)
		local jj = `jj' + 1
	}
}
matlist store5
drop id

/*************** aged 65 to 79 ******************/
// block 6 statistics
matrix store6 = J(2070-2018,3,.)
gen id = (dag>64 & dag<80)
forvalues yy = 2019/2070 {
	qui {
		local jj = 1
		sum hed if (time==`yy' & id), mean
		mat store6[`yy'-2018,`jj'] = r(mean)
		local jj = `jj' + 1
		sum led if (time==`yy' & id), mean
		mat store6[`yy'-2018,`jj'] = r(mean)
		local jj = `jj' + 1
		sum partnered if (time==`yy' & id), mean
		mat store6[`yy'-2018,`jj'] = r(mean)
		local jj = `jj' + 1
	}
}
matlist store6
drop id

/*************** aged 80 and over ******************/
// block 7 statistics
matrix store7 = J(2070-2018,3,.)
gen id = (dag>79)
forvalues yy = 2019/2070 {
	qui {
		local jj = 1
		sum hed if (time==`yy' & id), mean
		mat store7[`yy'-2018,`jj'] = r(mean)
		local jj = `jj' + 1
		sum led if (time==`yy' & id), mean
		mat store7[`yy'-2018,`jj'] = r(mean)
		local jj = `jj' + 1
		sum partnered if (time==`yy' & id), mean
		mat store7[`yy'-2018,`jj'] = r(mean)
		local jj = `jj' + 1
	}
}
matlist store7
drop id


/*******************************************************************************
*	social care receipt
*******************************************************************************/
use "$outdir/temp1", clear

// block 8 statistics
matrix store8 = J(2070-2018,5,.)
forvalues yy = 2019/2070 {
	qui {
		local jj = 1
		count if (time==`yy' & recCare & dag>44 & dag<65)
		mat store8[`yy'-2018,`jj'] = r(N)
		local jj = `jj' + 1
		count if (time==`yy' & recCare & dag>64 & dag<80)
		mat store8[`yy'-2018,`jj'] = r(N)
		local jj = `jj' + 1
		count if (time==`yy' & recCare & needCare & dag>64 & dag<80)
		mat store8[`yy'-2018,`jj'] = r(N)
		local jj = `jj' + 1
		count if (time==`yy' & recCare & dag>79)
		mat store8[`yy'-2018,`jj'] = r(N)
		local jj = `jj' + 1
		count if (time==`yy' & recCare & needCare & dag>79)
		mat store8[`yy'-2018,`jj'] = r(N)
		local jj = `jj' + 1
	}
}
matlist store8

// block 9 statistics
matrix store9 = J(2070-2018,3,.)
forvalues yy = 2019/2070 {
	qui {
		sum totalCareHours if (time==`yy' & dag>44 & dag<65 & recCare), mean
		mat store9[`yy'-2018,1] = r(mean)
		sum totalCareHours if (time==`yy' & dag>64 & dag<80 & recCare), mean
		mat store9[`yy'-2018,2] = r(mean)
		sum totalCareHours if (time==`yy' & dag>79 & recCare), mean
		mat store9[`yy'-2018,3] = r(mean)
	}
}
matlist store9

// block 10 statistics
matrix store10 = J(2070-2018,3,.)
forvalues yy = 2019/2070 {
	qui {
		count if (time==`yy' & informalCareHours>0.1 & carehoursfromformalweekly<0.1)
		mat store10[`yy'-2018,1] = r(N)
		count if (time==`yy' & informalCareHours>0.1 & carehoursfromformalweekly>0.1)
		mat store10[`yy'-2018,2] = r(N)
		count if (time==`yy' & informalCareHours<0.1 & carehoursfromformalweekly>0.1)
		mat store10[`yy'-2018,3] = r(N)
	}
}
matlist store10

// block 11 statistics
matrix store11 = J(2070-2018,6,.)
gen chk = (dag>44)
forvalues yy = 2019/2070 {
	sum carehoursfromparentweekly if (time==`yy' & chk), mean
	mat store11[`yy'-2018,1] = r(mean)
	sum carehoursfrompartnerweekly if (time==`yy' & chk), mean
	mat store11[`yy'-2018,2] = r(mean)
	sum carehoursfromdaughterweekly if (time==`yy' & chk), mean
	mat store11[`yy'-2018,3] = r(mean)
	sum carehoursfromsonweekly if (time==`yy' & chk), mean
	mat store11[`yy'-2018,4] = r(mean)
	sum carehoursfromotherweekly if (time==`yy' & chk), mean
	mat store11[`yy'-2018,5] = r(mean)
	sum carehoursfromformalweekly if (time==`yy' & chk), mean
	mat store11[`yy'-2018,6] = r(mean)
}
drop chk
matlist store11


/*******************************************************************************
*	social care provision
*******************************************************************************/
use "$outdir/temp1", clear

tab time if (socialcareprovision_p=="OnlyOther")
tab time if (socialcareprovision_p=="OnlyPartner")
tab time if (socialcareprovision_p=="PartnerAndOther")

matrix store1 = J(2070-2018,1,.)
forvalues yy = 2019/2070 {
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

