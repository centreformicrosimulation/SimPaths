/**************************************************************************************
*
*	PROGRAM TO EVALUATE SOCIAL CARE FROM FRS DATA
*	Last version:  Justin van de Ven, 28 Aug 2023
*	First version: Justin van de Ven, 05 Jun 2024
*	
*
**************************************************************************************/


/**************************************************************************************
*
*	DATA SET-UP
*
**************************************************************************************/
clear all
global outdir = "C:\MyFiles\99 DEV ENV\JAS-MINE\data work\regression_estimates\data\"
cd "$outdir"


/**************************************************************************************
*	compile data
**************************************************************************************/
global yy = 2015
foreach datadir in "C:\MyFiles\01 DATA\UK\frs2015-2016\stata\stata11_se" ///
				   "C:\MyFiles\01 DATA\UK\frs2016-2017\stata" ///
				   "C:\MyFiles\01 DATA\UK\frs2017-2018\stata" ///
				   "C:\MyFiles\01 DATA\UK\frs2018-2019\stata" ///
				   "C:\MyFiles\01 DATA\UK\frs2019-2020\stata\stata13_se" ///
				   "C:\MyFiles\01 DATA\UK\frs2021-2022\stata\stata13_se" {

	if ($yy == 2020) global yy = $yy + 1
	use "`datadir'/care.dta", clear
	drop if (NEEDPER > 14)
	rename NEEDPER PERSON
	if ($yy == 2018) {
		sort sernum BENUNIT PERSON
		drop month
		merge 1:1 sernum BENUNIT PERSON using "`datadir'/adult.dta"
	} 
	else {
		sort SERNUM BENUNIT PERSON
		drop MONTH
		merge 1:1 SERNUM BENUNIT PERSON using "`datadir'/adult.dta"
	}
	if ($yy < 2018) {
		
		drop _merge
		merge m:1 SERNUM using "`datadir'/househol.dta"
	}
	rename *, l
	drop if (_merge==1)
	drop if (dvhiqual<0)
	gen dhe_c3 = 0
	gen male = (sex == 1)
	replace dhe_c3 = 3 if (dvhiqual>0 & dvhiqual<4)
	replace dhe_c3 = 2 if (dvhiqual>3 & dvhiqual<40)
	replace dhe_c3 = 1 if (dvhiqual>39)
	gen year = $yy
	keep sernum benunit person hourcare hour17 hour18 hour19 hour20 empstati gross4 age80 dhe_c3 male gvtregno year
	save "frs_$yy.dta", replace
	global yy = $yy + 1
}

clear all
append using "frs_2015.dta" "frs_2016.dta" "frs_2017.dta" "frs_2018.dta" "frs_2019.dta" "frs_2021.dta"
save "frs_pooled_raw.dta", replace


/**************************************************************************************
*	generate variables
**************************************************************************************/
use "frs_pooled_raw.dta", clear
gen recCare = (hourcare > 0.1)
gen hourFormCare = 0
foreach vv of varlist hour17 hour18 hour19 hour20 {
	replace hourFormCare = hourFormCare + 2.5 if (`vv'==1)
	replace hourFormCare = hourFormCare + 7.5 if (`vv'==2)
	replace hourFormCare = hourFormCare + 15 if (`vv'==3)
	replace hourFormCare = hourFormCare + 27.5 if (`vv'==4)
	replace hourFormCare = hourFormCare + 42.5 if (`vv'==5)
	replace hourFormCare = hourFormCare + 75 if (`vv'==6)
	replace hourFormCare = hourFormCare + 150 if (`vv'==7)
	replace hourFormCare = hourFormCare + 10 if (`vv'==8)
	replace hourFormCare = hourFormCare + 27.5 if (`vv'==9)
	replace hourFormCare = hourFormCare + 95 if (`vv'==10)
}
gen hourInfCare = hourcare*(hourcare>0) - hourFormCare
gen recFormCare = (hourFormCare>0)
gen recInfCare = (hourInfCare>0)
replace hourcare = 0 if (hourcare<0)

gen ltsd = (empstati==9)
save "frs_pooled.dta", replace


/**************************************************************************************
*	basic statistical analysis
**************************************************************************************/
/*
use "frs_pooled.dta", clear
tab dhe_c3 year [fweight=gross4]
tab ltsd recCare [fweight=gross4] if (age80>19 & age80<40)
tab ltsd recCare [fweight=gross4] if (age80>39 & age80<50)
tab ltsd recCare [fweight=gross4] if (age80>49 & age80<60)
tab ltsd recCare [fweight=gross4] if (age80>59 & age80<65)

matrix store = J(7,7,.)
forval ii = 1/7{
	sum recCare [fweight=gross4] if (age80>19+10*(`ii'-1) & age80<30+10*(`ii'-1)) 
	matrix store[`ii',1] = r(mean)
	sum recInfCare [fweight=gross4] if (age80>19+10*(`ii'-1) & age80<30+10*(`ii'-1)) 
	matrix store[`ii',2] = r(mean)
	sum recFormCare [fweight=gross4] if (age80>19+10*(`ii'-1) & age80<30+10*(`ii'-1)) 
	matrix store[`ii',3] = r(mean)
	sum hourInfCare [fweight=gross4] if (recInfCare==1 & age80>19+10*(`ii'-1) & age80<30+10*(`ii'-1)) 
	matrix store[`ii',4] = r(mean)
	sum hourFormCare [fweight=gross4] if (recFormCare==1 & age80>19+10*(`ii'-1) & age80<30+10*(`ii'-1)) 
	matrix store[`ii',5] = r(mean)
	sum hourInfCare [fweight=gross4] if (recInfCare==1 & recFormCare==1 & age80>19+10*(`ii'-1) & age80<30+10*(`ii'-1)) 
	matrix store[`ii',6] = r(mean)
	sum hourFormCare [fweight=gross4] if (recInfCare==1 & recFormCare==1 & age80>19+10*(`ii'-1) & age80<30+10*(`ii'-1)) 
	matrix store[`ii',7] = r(mean)
}
matlist store

matrix store = J(14,7,.)
forval ii = 1/14{
	qui{
		sum recCare [fweight=gross4] if (age80>14+5*(`ii'-1) & age80<20+5*(`ii'-1)) 
		matrix store[`ii',1] = r(mean)
		sum recInfCare [fweight=gross4] if (age80>14+5*(`ii'-1) & age80<20+5*(`ii'-1)) 
		matrix store[`ii',2] = r(mean)
		sum recFormCare [fweight=gross4] if (age80>14+5*(`ii'-1) & age80<20+5*(`ii'-1)) 
		matrix store[`ii',3] = r(mean)
		sum hourInfCare [fweight=gross4] if (recInfCare==1 & age80>14+5*(`ii'-1) & age80<20+5*(`ii'-1)) 
		matrix store[`ii',4] = r(mean)
		sum hourFormCare [fweight=gross4] if (recFormCare==1 & age80>14+5*(`ii'-1) & age80<20+5*(`ii'-1)) 
		matrix store[`ii',5] = r(mean)
		sum hourInfCare [fweight=gross4] if (recInfCare==1 & recFormCare==1 & age80>14+5*(`ii'-1) & age80<20+5*(`ii'-1)) 
		matrix store[`ii',6] = r(mean)
		sum hourFormCare [fweight=gross4] if (recInfCare==1 & recFormCare==1 & age80>14+5*(`ii'-1) & age80<20+5*(`ii'-1)) 
		matrix store[`ii',7] = r(mean)
	}
}
matlist store
*/

/**************************************************************************************
*	regression analysis
**************************************************************************************/
use "frs_pooled.dta", clear
/*
gen ageg5 = 0
forval ii = 1/10 {
	replace ageg5 = `ii' if (age80>14+5*(`ii'-1) & age80<20+5*(`ii'-1))
}
probit recInfCare i.dhe_c3 i.male i.ageg5 ib8.gvtregno i.year if (ltsd==1 & age80<65) [fweight=gross4]
regress hourInfCare i.dhe_c3 i.male i.ageg5 ib8.gvtregno i.year if (ltsd==1 & age80<65 & recInfCare==1) [fweight=gross4]

gen agega = 0
replace agega = 1 if (age80<18)
replace agega = 2 if (age80>17 & age80<20)
replace agega = 3 if (age80>19 & age80<22)
replace agega = 4 if (age80>21 & age80<24)
replace agega = 5 if (age80>23 & age80<26)
replace agega = 6 if (age80>25 & age80<28)
replace agega = 7 if (age80>27 & age80<30)
replace agega = 8 if (age80>29 & age80<32)
replace agega = 9 if (age80>31 & age80<34)
replace agega = 10 if (age80>33 & age80<36)
replace agega = 11 if (age80>35 & age80<38)
replace agega = 12 if (age80>37 & age80<40)
replace agega = 13 if (age80>39 & age80<42)
replace agega = 14 if (age80>41 & age80<44)
replace agega = 15 if (age80>43 & age80<46)
probit recInfCare i.dhe_c3 i.male i.agega ib8.gvtregno if (ltsd==1 & age80<65) [fweight=gross4]
regress hourInfCare i.dhe_c3 i.male i.agega ib8.gvtregno if (ltsd==1 & age80<65 & recInfCare==1) [fweight=gross4]
*/

// Process S1a
gen aged = (age80<25)
probit recInfCare i.dhe_c3 male aged ib8.gvtregno if (ltsd==1 & age80<65) [fweight=gross4], vce(r)
putexcel set "$outdir/probitCareUnder65", modify
putexcel A1 = matrix(e(b))
putexcel A2 = matrix(e(V))

// Process S1b
gen agee = (age80<25) + 2*(age80>24)*(age80<40) + 3*(age80>39)
gen lhourInfCare = ln(hourInfCare) if (recInfCare==1)
regress lhourInfCare i.dhe_c3 i.male i.agee ib8.gvtregno if (ltsd==1 & age80<65 & recInfCare==1) [fweight=gross4], vce(r)
putexcel set "$outdir/linearCareUnder65", modify
putexcel A1 = matrix(e(b))
putexcel A2 = matrix(e(V))
