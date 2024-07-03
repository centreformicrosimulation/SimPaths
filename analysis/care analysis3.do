/**************************************************************************************
*
*	PROGRAM TO ANALYSE FORWARD PROJECTIONS FOR CARE
*
*	Last version:  Justin van de Ven, 18 Jun 2024
*	First version: Justin van de Ven, 18 Jun 2024
*
**************************************************************************************/

clear all
global basedir = "C:\MyFiles\99 DEV ENV\JAS-MINE\SimPaths\output\base\csv"
global zerodir = "C:\MyFiles\99 DEV ENV\JAS-MINE\SimPaths\output\zero\csv"
global ignoredir = "C:\MyFiles\99 DEV ENV\JAS-MINE\SimPaths\output\ignore\csv"
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
import delimited using "$zerodir/BenefitUnit.csv", clear
rename *, l
rename id_benefitunit idbenefitunit
gsort idbenefitunit time
gen nk = 0
forvalues ii = 0/17 {
	replace nk = nk + n_children_`ii'
}
save "$outdir/zero0", replace
import delimited using "$zerodir/Person.csv", clear
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


//////// ignore ////////////////////////
import delimited using "$ignoredir/BenefitUnit.csv", clear
rename *, l
rename id_benefitunit idbenefitunit
gsort idbenefitunit time
gen nk = 0
forvalues ii = 0/17 {
	replace nk = nk + n_children_`ii'
}
save "$outdir/ignore0", replace
import delimited using "$ignoredir/Person.csv", clear
rename *, l
rename id_person idperson
rename socialcareprovision socialcareprovision_p
gsort idbenefitunit time idperson
merge m:1 idbenefitunit time using ignore0
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

save "$outdir/ignore1", replace


/**************************************************************************************
*	age profile analysis
**************************************************************************************/
matrix store1 = J(80-17,9,.)
forvalues jj = 1/3 {
	
	if (`jj'==1) {
		use "$outdir/zero1", clear
	}
	if (`jj'==2) {
		use "$outdir/ignore1", clear
	}
	if (`jj'==3) {
		use "$outdir/base1", clear
	}

	gen byear = time - dag
	gen dbld = (dlltsd=="True")
	gen carer = (socialcareprovision_p!="None")
	gen worker = (les_c4=="EmployedOrSelfEmployed")
	gen dispinc = disposableincomemonthly * 12
	recode careformalexpenditureweekly (-9=0)
	gen careexpend = (careformalexpenditureweekly + childcarecostperweek) * 364.25/7
	
	gsort idperson time
	gen childpre29i = 0
	replace childpre29i = 1 if (nk>0 & dag<29)
	by idperson: egen childpre29 = max(childpre29i)
	
	//gen target = (byear>1999 & byear<2010 & childpre29==0)
	//gen target = (byear>1999 & byear<2010)
	//gen target = (byear>1989 & byear<2000 & childpre29==0)
	//gen target = (byear>1989 & byear<2000)
	gen target = (time==2019)
	//gen target = 1
	
	forvalues aa = 18/80 {
		
		local jj = 1
		sum dbld if (dag==`aa' & target), mean
		mat store1[`aa'-17,`jj'] = r(mean)
		local jj = `jj' + 1

		sum needCare if (dag==`aa' & target), mean
		mat store1[`aa'-17,`jj'] = r(mean)
		local jj = `jj' + 1

		sum carer if (dag==`aa' & target), mean
		mat store1[`aa'-17,`jj'] = r(mean)
		local jj = `jj' + 1

		sum worker if (dag==`aa' & target), mean
		mat store1[`aa'-17,`jj'] = r(mean)
		local jj = `jj' + 1

		sum hoursworkedweekly if (dag==`aa' & target), mean
		mat store1[`aa'-17,`jj'] = r(mean)
		local jj = `jj' + 1

		sum dispinc if (dag==`aa' & target), mean
		mat store1[`aa'-17,`jj'] = r(mean) * 1.2663  // from 2015 to 2022 prices
		local jj = `jj' + 1

		sum discretionaryconsumptionperyear if (dag==`aa' & target), mean
		mat store1[`aa'-17,`jj'] = r(mean) * 1.2663  // from 2015 to 2022 prices
		local jj = `jj' + 1

		sum careexpend if (dag==`aa' & target), mean
		mat store1[`aa'-17,`jj'] = r(mean) * 1.2663  // from 2015 to 2022 prices
		local jj = `jj' + 1

		sum liquidwealth if (dag==`aa' & target), mean
		mat store1[`aa'-17,`jj'] = r(mean) * 1.2663  // from 2015 to 2022 prices
		local jj = `jj' + 1
	}
	matlist store1
}


/*******************************************************************************
*	clean-up
*******************************************************************************/
#delimit ;
local files_to_drop 
	base0.dta
	base1.dta
	ignore0.dta
	ignore1.dta
	zero0.dta
	zero1.dta
	;
#delimit cr // cr stands for carriage return

foreach file of local files_to_drop { 
	erase "$outdir/`file'"
}

