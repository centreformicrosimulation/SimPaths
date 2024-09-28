/**************************************************************************************
*
*	PROGRAM TO ANALYSE LIFETIME PROFILES FOR CARE
*
*	Last version:  Justin van de Ven, 18 Jun 2024
*	First version: Justin van de Ven, 18 Jun 2024
*
**************************************************************************************/

clear all
global basedir = "C:\MyFiles\99 DEV ENV\JAS-MINE\SimPaths\output\sc analysis1b\csv"
global zerocostsdir = "C:\MyFiles\99 DEV ENV\JAS-MINE\SimPaths\output\sc analysis2\csv"
global naivedir = "C:\MyFiles\99 DEV ENV\JAS-MINE\SimPaths\output\sc analysis3\csv"
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

gen child = (dag<18)
gen adult = 1 - child
bys time idbenefitunit: egen nk = sum(child) 
bys time idbenefitunit: egen na = sum(adult) 
gen partnered = adult * (na-1)

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

gen child = (dag<18)
gen adult = 1 - child
bys time idbenefitunit: egen nk = sum(child) 
bys time idbenefitunit: egen na = sum(adult) 
gen partnered = adult * (na-1)

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

gen child = (dag<18)
gen adult = 1 - child
bys time idbenefitunit: egen nk = sum(child) 
bys time idbenefitunit: egen na = sum(adult) 
gen partnered = adult * (na-1)

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

gen needCare = (needsocialcare=="True")
gen informalCareHours = carehoursfrompartnerweekly + carehoursfromdaughterweekly + carehoursfromsonweekly + carehoursfromotherweekly + carehoursfromparentweekly
gen totalCareHours = informalCareHours + carehoursfromformalweekly
gen recCare = (totalCareHours>0.01)

save "$outdir/naive1", replace


/**************************************************************************************
*	age profile analysis
**************************************************************************************/
// block a
matrix store1 = J(80-17,9,.)
forvalues kk = 1/3 {
	
	qui {
		
		if (`kk'==1) {
			use "$outdir/zero1", clear
		}
		if (`kk'==2) {
			use "$outdir/naive1", clear
		}
		if (`kk'==3) {
			use "$outdir/base1", clear
		}

		gen byear = time - dag
		gen dbld = (dlltsd=="True")
		gen carer = (socialcareprovision_p!="None")
		gen worker = (les_c4=="EmployedOrSelfEmployed")
		gen dispinc = disposableincomemonthly * 12
		recode careformalexpenditureweekly (-9=0)
		gen careexpend = (careformalexpenditureweekly + childcarecostperweek) * 364.25/7
		
		//gen target = 1
		//gen target = (byear>1989 & byear<2000)
		gen target = (byear>1989 & byear<1995)
		//gen target = (byear==1999)
		
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
			mat store1[`aa'-17,`jj'] = r(mean) * 1.305  // from 2015 to 2024 prices (ONS CPI Annual Average (All Items, D7BT))
			local jj = `jj' + 1

			sum discretionaryconsumptionperyear if (dag==`aa' & target), mean
			mat store1[`aa'-17,`jj'] = r(mean) * 1.305  // from 2015 to 2024 prices
			local jj = `jj' + 1

			sum careexpend if (dag==`aa' & target), mean
			mat store1[`aa'-17,`jj'] = r(mean) * 1.305  // from 2015 to 2024 prices
			local jj = `jj' + 1

			sum liquidwealth if (dag==`aa' & target), mean
			mat store1[`aa'-17,`jj'] = r(mean) * 1.305  // from 2015 to 2024 prices
			local jj = `jj' + 1
		}
	}
	matlist store1
}

// block b
matrix store1 = J(30*3*6,9,.)
forvalues kk = 1/3 {
	
	qui {
		
		if (`kk'==1) {
			use "$outdir/zero1", clear
			noisily display "evaluating statistics for zero1"
		}
		if (`kk'==2) {
			use "$outdir/naive1", clear
			noisily display "evaluating statistics for naive1"
		}
		if (`kk'==3) {
			use "$outdir/base1", clear
			noisily display "evaluating statistics for base1"
		}

		gen byear = time - dag
		gen dbld = (dlltsd=="True")
		gen carer = (socialcareprovision_p!="None")
		gen worker = (les_c4=="EmployedOrSelfEmployed")
		gen dispinc = disposableincomemonthly * 12
		recode careformalexpenditureweekly (-9=0)
		gen careexpend = (careformalexpenditureweekly + childcarecostperweek) * 364.25/7
		forvalues ii = 1/6 {
			
			local by0 = 1999 - 10*(`ii'-1)
			local by1 = 2019 - 10*(`ii'-1)
			local aa0 = 20 + 10*(`ii'-1)
			local aa1 = 49 + 10*(`ii'-1)
			local row = (`aa1'-`aa0'+1) * (6*(`kk'-1) + `ii'-1)
			gen target = (byear>=`by0' & byear<=`by1')
			noisily display "evaluating statistics for age band `aa0' to `aa1'"
			forvalues aa = `aa0'/`aa1' {
				
				local row = `row' + 1
				
				local jj = 1
				sum dbld if (dag==`aa' & target), mean
				mat store1[`row',`jj'] = r(mean)
				local jj = `jj' + 1

				sum needCare if (dag==`aa' & target), mean
				mat store1[`row',`jj'] = r(mean)
				local jj = `jj' + 1

				sum carer if (dag==`aa' & target), mean
				mat store1[`row',`jj'] = r(mean)
				local jj = `jj' + 1

				sum worker if (dag==`aa' & target), mean
				mat store1[`row',`jj'] = r(mean)
				local jj = `jj' + 1

				sum hoursworkedweekly if (dag==`aa' & target), mean
				mat store1[`row',`jj'] = r(mean)
				local jj = `jj' + 1

				sum dispinc if (dag==`aa' & target), mean
				mat store1[`row',`jj'] = r(mean) * 1.305  // from 2015 to 2024 prices (ONS CPI Annual Average (All Items, D7BT))
				local jj = `jj' + 1

				sum discretionaryconsumptionperyear if (dag==`aa' & target), mean
				mat store1[`row',`jj'] = r(mean) * 1.305  // from 2015 to 2024 prices
				local jj = `jj' + 1

				sum careexpend if (dag==`aa' & target), mean
				mat store1[`row',`jj'] = r(mean) * 1.305  // from 2015 to 2024 prices
				local jj = `jj' + 1

				sum liquidwealth if (dag==`aa' & target), mean
				mat store1[`row',`jj'] = r(mean) * 1.305  // from 2015 to 2024 prices
				local jj = `jj' + 1
			}
			drop target
		}
	}
}
matlist store1
