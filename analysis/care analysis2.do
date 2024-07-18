/**************************************************************************************
*
*	PROGRAM TO ANALYSE LIFETIME PROFILES FOR CARE
*
*	Last version:  Justin van de Ven, 18 Jun 2024
*	First version: Justin van de Ven, 18 Jun 2024
*
**************************************************************************************/

clear all
global basedir = "C:\MyFiles\99 DEV ENV\JAS-MINE\SimPaths\output\base\csv"
global zerocostsdir = "C:\MyFiles\99 DEV ENV\JAS-MINE\SimPaths\output\zero\csv"
global naivedir = "C:\MyFiles\99 DEV ENV\JAS-MINE\SimPaths\output\ignore\csv"
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
*	age profile analysis
**************************************************************************************/
matrix store1 = J(80-17,9,.)
forvalues jj = 1/3 {
	
	if (`jj'==1) {
		use "$outdir/zero1", clear
	}
	if (`jj'==2) {
		use "$outdir/naive1", clear
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
	
	gen target = (byear>1989 & byear<2000)
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


/**************************************************************************************
*	supplementary analysis (not reported in paper)
**************************************************************************************/
matrix store1 = J(10,3,.)
forvalues jj = 1/3 {
	
	if (`jj'==1) {
		use "$outdir/zero1", clear
	}
	if (`jj'==2) {
		use "$outdir/naive1", clear
	}
	if (`jj'==3) {
		use "$outdir/base1", clear
	}

	gsort idbenefitunit time idperson
	drop if (dag<18)

	by idbenefitunit time: egen employedHours_bu = sum(hoursworkedweekly)

	gen chk = (les_c4=="EmployedOrSelfEmployed")
	by idbenefitunit time: egen employed_bu = sum(chk)
	drop chk

	gen chk = (dgn=="Female" & dag>17 & dag<38)
	by idbenefitunit time: egen fertile = max(chk)
	drop chk

	gen chk = (dlltsd=="True")
	by idbenefitunit time: egen dlltsd_bu = max(chk)
	drop chk

	gen chk = (socialcareprovision_p!="None")
	by idbenefitunit time: egen carer_bu = max(chk)
	drop chk

	by idbenefitunit time: egen needCare_bu = max(needCare)

	by idbenefitunit time: egen careHoursProvided_bu = sum(carehoursprovidedweekly)

	by idbenefitunit time: egen careHoursReceived_bu = sum(totalCareHours)

	gen nk04 = 0
	forvalues ii = 0/4 {
		replace nk04 = nk04 + n_children_`ii'
	}
	gen nk1517 = 0
	forvalues ii = 15/17 {
		replace nk1517 = nk1517 + n_children_`ii'
	}

	gsort idperson time

	gen carerExperience = 0
	replace carerExperience = 1 if (carer_bu | (idperson==idperson[_n-1] & carerExperience[_n-1]==1))

	gen dbldExperience = 0
	replace dbldExperience = 1 if (dlltsd_bu | (idperson==idperson[_n-1] & dbldExperience[_n-1]==1))

	gen childExperience = 0
	replace childExperience = 1 if (nk>0 | (idperson==idperson[_n-1] & childExperience[_n-1]==1))

	gen careExperience = 0
	replace careExperience = 1 if (needCare_bu | careHoursReceived_bu>0.1 | (idperson==idperson[_n-1] & careExperience[_n-1]==1))
	
	gen timeSinceLastCarer = -1
	replace timeSinceLastCarer = 0 if (carer_bu)
	replace timeSinceLastCarer = timeSinceLastCarer[_n-1] + 1 if (!carer_bu & idperson==idperson[_n-1] & timeSinceLastCarer[_n-1]>=0)

	gen timeSinceLastCare = -1
	replace timeSinceLastCare = 0 if (needCare_bu)
	replace timeSinceLastCare = timeSinceLastCare[_n-1] + 1 if (!needCare_bu & idperson==idperson[_n-1] & timeSinceLastCare[_n-1]>=0)

	gen timeNeedingCare = -1
	replace timeNeedingCare = 0 if (needCare_bu)
	replace timeNeedingCare = timeNeedingCare[_n-1] + 1 if (needCare_bu & idperson==idperson[_n-1] & timeNeedingCare[_n-1]>=0)
	


	/************************************************************************
	*	childcare targets
	************************************************************************/
	//gen target = (nk==0) * fertile * (dbldExperience==0) * (carerExperience==0)	// anticipation1
	//gen target = (nk04>0) * (nk04==nk) * (dbldExperience==0) * (carerExperience==0) * (careExperience==0)		// impact1
	//gen target = (nk1517>0)*(nk1517==nk) * (dbldExperience==0) * (carerExperience==0) * (careExperience==0)	// scaring


	/************************************************************************
	*	social care provision targets
	************************************************************************/
	//gen target = (dgn=="Female" & dag>39 & dag<50 & dbldExperience==0 & carerExperience==0 & childExperience==0 & careExperience==0)
	//gen target = (dgn=="Female" & dag>49 & dag<60 & dbldExperience==0 & carer_bu==1 & childExperience==0 & careExperience==0)
	//gen target = (dgn=="Female" & dag>59 & dag<70 & dbldExperience==0 & childExperience==0 & timeSinceLastCarer>0 & timeSinceLastCarer<6)

	
	/************************************************************************
	*	social care receipt targets
	************************************************************************/
	//gen target = (dag>74 & dag<80 & dbldExperience==0 & carerExperience==0 & childExperience==0 & careExperience==0 )
	gen target = (dag>74 & dag<80 & dbldExperience==0 & carerExperience==0 & childExperience==0 & needCare_bu )
	

	local ii = 1
	sum employedHours_bu if (refbenefitunit & target), mean
	mat store1[`ii',`jj'] = r(mean)
	local ii = `ii' + 1
	sum employed_bu if (refbenefitunit & target), mean
	mat store1[`ii',`jj'] = r(mean)
	local ii = `ii' + 1
	sum partnered if (refbenefitunit & target), mean
	mat store1[`ii',`jj'] = r(mean)
	local ii = `ii' + 1
	sum dlltsd_bu if (refbenefitunit & target), mean
	mat store1[`ii',`jj'] = r(mean)
	local ii = `ii' + 1
	sum careHoursProvided_bu if (refbenefitunit & target), mean
	mat store1[`ii',`jj'] = r(mean)
	local ii = `ii' + 1
	sum childcarecostperweek if (refbenefitunit & target), mean
	mat store1[`ii',`jj'] = r(mean)
	local ii = `ii' + 1
	sum careformalexpenditureweekly if (refbenefitunit & target), mean
	mat store1[`ii',`jj'] = r(mean)
	local ii = `ii' + 1
	sum discretionaryconsumptionperyear if (refbenefitunit & target), mean
	mat store1[`ii',`jj'] = r(mean)
	local ii = `ii' + 1
	sum disposableincomemonthly if (refbenefitunit & target), mean
	mat store1[`ii',`jj'] = r(mean)
	local ii = `ii' + 1
	sum liquidwealth if (refbenefitunit & target), mean
	mat store1[`ii',`jj'] = r(mean)
	local ii = `ii' + 1
}

matlist store1

