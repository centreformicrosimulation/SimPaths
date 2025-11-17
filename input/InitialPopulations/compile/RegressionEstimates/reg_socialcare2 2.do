/**************************************************************************************
*
*	PROGRAM TO EVALUATE SOCIAL CARE RECEIPT FROM UKHLS DATA
*	ANALYSIS BASED ON THE SOCIAL CARE MODULE OF UKHLS
*	First version: Justin van de Ven, 28 Aug 2023
*	Last version:  Justin van de Ven, 05 Jun 2024
*
**************************************************************************************/


/**************************************************************************************
*
*	DATA SET-UP
*
**************************************************************************************/
clear all
global datadir = "C:\MyFiles\01 DATA\UK\ukhls\wave13\stata\stata13_se\ukhls\"
global outdir = "C:\MyFiles\99 DEV ENV\JAS-MINE\data work\regression_estimates\data\"
cd "$outdir"


/**************************************************************************************
*	load data
**************************************************************************************/
// pooled data
local ww = 1
foreach waveid in "g" "i" "k" {

	use "$datadir/`waveid'_indresp.dta", clear
	rename *, l
	rename `waveid'_* *
	
	gen year = intdaty_dv
	if (`ww' < 3) {
		gen helphours9 = -1
		gen helphoursb9 = -1
		gen helpcode9 = -1
	}
	gen wave = `ww'
	gen male = (sex==1)
	gen partner = (ppid>0)
	gen emp = (jbstat==1 | jbstat==2)
	rename gor_dv drgn
	
	keep pidp hidp pno year wave male partner drgn hiqual_dv dvage scsf1 adl* hlpinf* hlpform* helphours* helpcode* emp indinui_xw
	save "ukhls_`waveid'.dta", replace
	local ww = `ww' + 1
}

clear all
append using "ukhls_g.dta" "ukhls_i.dta" "ukhls_k.dta"
save "ukhls_pooled_raw.dta", replace


/**************************************************************************************
*	generate variables
**************************************************************************************/
use "ukhls_pooled_raw.dta", clear
gen needCare1 = 0	// any help
gen needCare2 = 0	// not possible to do
foreach vv of varlist adla adlb adlc adld adle adlf adlg adlh adli adlj adlk adll adlm adln {

	replace needCare1 = needCare1 + 0.5 if (`vv'>1)
	replace needCare2 = 1 if (`vv'==3)
}
replace needCare1 = 1 if (needCare1>1)
replace needCare1 = 0 if (needCare1<0.9)
gen recInfCareb = 0
forval ii = 1/10 {
	
	replace recInfCareb = 1 if (hlpinfa`ii' == 1)
	replace recInfCareb = 1 if (hlpinfb`ii' == 1)
}
gen recFormCareb = 0
forval ii = 1/7 {

	replace recFormCareb = 1 if (hlpforma`ii' == 1)
	replace recFormCareb = 1 if (hlpformb`ii' == 1)
}
replace recFormCareb = 1 if (hlpforma97==1)
replace recFormCareb = 1 if (hlpformb97==1)
gen recCareb = max(recFormCareb, recInfCareb)

gen hourFormCare = 0
gen hourInfCare = 0
gen hourPartner = 0
gen hourDaughter = 0
gen hourSon = 0
gen hourParent = 0
gen numbFormCare = 0
gen numbInfCare = 0
forval ii = 1/9 {

	gen hrs = 0
	replace hrs = 2.5 if (helphours`ii' == 3)
	replace hrs = 7.0 if (helphours`ii' == 4)
	replace hrs = 14.5 if (helphours`ii' == 5)
	replace hrs = 27.0 if (helphours`ii' == 6)
	replace hrs = 42.0 if (helphours`ii' == 7)
	replace hrs = 74.5 if (helphours`ii' == 8)
	replace hrs = 125.0 if (helphours`ii' == 9)
	gen cc = 0
	replace cc = 1 if (helphours`ii' > 2)
	if (helphours`ii' < 2) {
		replace hrs = 14.5 if (helphoursb`ii' == 1)
		replace hrs = 27.0 if (helphoursb`ii' == 2)
		replace hrs = 100.0 if (helphoursb`ii' == 3)
		replace cc = 1 if (helphoursb`ii' > 0)
	}
	replace hourPartner = hourPartner + hrs if (helpcode`ii'==1 & hrs>0)
	replace hourSon = hourSon + hrs if (helpcode`ii'==2 & hrs>0)
	replace hourDaughter = hourDaughter + hrs if (helpcode`ii'==3 & hrs>0)
	replace hourParent = hourParent + hrs if (helpcode`ii'==7 & hrs>0)
	replace hourInfCare = hourInfCare + hrs if (helpcode`ii'>=1 & helpcode`ii'<=10 & hrs>0)
	replace hourFormCare = hourFormCare + hrs if (helpcode`ii'>=11 & hrs>0)
	
	replace numbFormCare = numbFormCare + cc if (helpcode`ii'>=11)
	replace numbInfCare = numbInfCare + cc if (helpcode`ii'>=1 & helpcode`ii'<=10)

	drop hrs cc
}
gen hourOtherCare = hourInfCare - hourPartner - hourSon - hourDaughter
gen recInfCare = (hourInfCare > 0)
gen recFormCare = (hourFormCare > 0)
gen recCare = (hourInfCare + hourFormCare > 0)
gen recDaughter = (hourDaughter>0)
gen recSon = (hourSon>0)
gen recPartner = (hourPartner>0)
gen recOther = (hourOther>0)
save "ukhls_pooled.dta", replace


/**************************************************************************************
*	basic statistical analysis
**************************************************************************************/
/*
use "ukhls_pooled.dta", clear
keep if year < 2020
tab numbInfCare numbFormCare [aweight=indinui_xw] if (recCare > 0)
matrix store = J(4,7,.)
matrix store2 = J(4,3,.)
forval ii = 11/14{
	qui {
		if (`ii'==14) {
			local maxAge = 150
		}
		else {
			local maxAge = 20+5*(`ii'-1)
		}

		sum needCare1 [aweight=indinui_xw] if (dvage>14+5*(`ii'-1) & dvage<`maxAge') 
		matrix store2[`ii'-10,1] = r(mean)
		sum needCare2 [aweight=indinui_xw] if (dvage>14+5*(`ii'-1) & dvage<`maxAge') 
		matrix store2[`ii'-10,2] = r(mean)
		sum recCareb [aweight=indinui_xw] if (dvage>14+5*(`ii'-1) & dvage<`maxAge') 
		matrix store2[`ii'-10,3] = r(mean)

		sum recCare [aweight=indinui_xw] if (dvage>14+5*(`ii'-1) & dvage<`maxAge') 
		matrix store[`ii'-10,1] = r(mean)
		sum recInfCare [aweight=indinui_xw] if (dvage>14+5*(`ii'-1) & dvage<`maxAge') 
		matrix store[`ii'-10,2] = r(mean)
		sum recFormCare [aweight=indinui_xw] if (dvage>14+5*(`ii'-1) & dvage<`maxAge') 
		matrix store[`ii'-10,3] = r(mean)
		sum hourInfCare [aweight=indinui_xw] if (recInfCare==1 & dvage>14+5*(`ii'-1) & dvage<`maxAge') 
		matrix store[`ii'-10,4] = r(mean)
		sum hourFormCare [aweight=indinui_xw] if (recFormCare==1 & dvage>14+5*(`ii'-1) & dvage<`maxAge') 
		matrix store[`ii'-10,5] = r(mean)
		sum hourInfCare [aweight=indinui_xw] if (recInfCare==1 & recFormCare==1 & dvage>14+5*(`ii'-1) & dvage<`maxAge') 
		matrix store[`ii'-10,6] = r(mean)
		sum hourFormCare [aweight=indinui_xw] if (recInfCare==1 & recFormCare==1 & dvage>14+5*(`ii'-1) & dvage<`maxAge') 
		matrix store[`ii'-10,7] = r(mean)
	}
}
matlist store
matlist store2


matrix store3 = J(5,4,.)
local jj = 0
foreach vv of varlist recPartner recDaughter recSon recOther {

	qui {
		local jj = `jj' + 1
		local ii = 1
		sum `vv' [aweight=indinui_xw] if (recCare==1)
		matrix store3[`ii',`jj'] = r(mean)
		foreach ww of varlist recPartner recDaughter recSon recOther {
		
			local ii = `ii' + 1
			sum `ww' [aweight=indinui_xw] if (`vv'==1)
			matrix store3[`ii',`jj'] = r(mean)
		}
	}
}
matlist store3
*/

/**************************************************************************************
*	prepare time-series data for regression analysis
**************************************************************************************/
clear all
forval ii = 1/3 {
	use "ukhls_pooled.dta", clear
	keep if (wave == `ii')
	save "ukhlst_`ii'.dta", replace
}
forval ii = 1/2 {
	use "ukhlst_`ii'.dta", clear
	rename * *_l
	rename pidp_l pidp
	local jj = `ii' + 1
	merge 1:1 pidp using "ukhlst_`jj'.dta"
	save "ukhlstml_`jj'.dta", replace
}

clear all
append using "ukhlstml_2.dta" "ukhlstml_3.dta"
save "ukhlstml_pooled.dta", replace


/**************************************************************************************
*	regression variables
**************************************************************************************/
clear all
use "ukhlstml_pooled.dta", clear
keep if (_merge==3 & dvage>64)
replace drgn = . if (drgn<0)
replace scsf1 = . if (scsf1<0)
gen deh_c3 = .
replace deh_c3 = 1 if (hiqual_dv == 1)
replace deh_c3 = 2 if (hiqual_dv>1 & hiqual_dv<5)
replace deh_c3 = 3 if (hiqual_dv>4)
gen dage1 = 0
forval ii = 1/10 {
	replace dage1 = `ii' if (dvage>=65+2*(`ii'-1) & dvage<=67+2*(`ii'-1))
}
replace dage1 = 11 if (dvage>85)
gen dage2 = 0
replace dage2 = 1 if (dvage>=85 & dvage<=89)
replace dage2 = 2 if (dvage>89)
gen dage3 = (dvage>=85)

gen careMarket = .
replace careMarket = 0 if (recInfCare==0 & recFormCare==0)
replace careMarket = 1 if (recInfCare==1 & recFormCare==0)
replace careMarket = 2 if (recInfCare==1 & recFormCare==1)
replace careMarket = 3 if (recInfCare==0 & recFormCare==1)
gen careMarket_l = .
replace careMarket_l = 0 if (recInfCare_l==0 & recFormCare_l==0)
replace careMarket_l = 1 if (recInfCare_l==1 & recFormCare_l==0)
replace careMarket_l = 2 if (recInfCare_l==1 & recFormCare_l==1)
replace careMarket_l = 3 if (recInfCare_l==0 & recFormCare_l==1)

gen whoCares = .
replace whoCares = 0 if (recPartner==1)
replace whoCares = 3 if (recOther==1)
replace whoCares = 2 if (recSon==1)
replace whoCares = 1 if (recDaughter==1)
gen whoCares_l = .
replace whoCares_l = 0 if (recPartner_l==1)
replace whoCares_l = 3 if (recOther_l==1)
replace whoCares_l = 2 if (recSon_l==1)
replace whoCares_l = 1 if (recDaughter_l==1)

gen whoCares2 = .
replace whoCares2 = 0 if (recDaughter==0 & recSon==0 & recOther==0)
replace whoCares2 = 1 if (recDaughter==1 & recSon==0 & recOther==0)
replace whoCares2 = 2 if (recDaughter==1 & recSon==1 & recOther==0)
replace whoCares2 = 3 if (recDaughter==1 & recSon==0 & recOther==1)
replace whoCares2 = 4 if (recDaughter==0 & recSon==1 & recOther==0)
replace whoCares2 = 5 if (recDaughter==0 & recSon==1 & recOther==1)
replace whoCares2 = 6 if (recDaughter==0 & recSon==0 & recOther==1)
gen whoCares2_l = .
replace whoCares2_l = 0 if (recDaughter_l==0 & recSon_l==0 & recOther_l==0)
replace whoCares2_l = 1 if (recDaughter_l==1 & recSon_l==0 & recOther_l==0)
replace whoCares2_l = 2 if (recDaughter_l==1 & recSon_l==1 & recOther_l==0)
replace whoCares2_l = 3 if (recDaughter_l==1 & recSon_l==0 & recOther_l==1)
replace whoCares2_l = 4 if (recDaughter_l==0 & recSon_l==1 & recOther_l==0)
replace whoCares2_l = 5 if (recDaughter_l==0 & recSon_l==1 & recOther_l==1)
replace whoCares2_l = 6 if (recDaughter_l==0 & recSon_l==0 & recOther_l==1)

gen poorHealth = (scsf1==5)
save "ukhlstmlb_pooled.dta", replace


/**************************************************************************************
*	adjust estimated sample to control for data observed every second year
*
*	any observation that does not change its status between observations is assumed to remain
*	invariant for intervening year
*	any observation that changes status between observations is replicated twice for intervening 
*	year - once to take previous value another to take altered value, with survey weights of 
*	each observation halved.
*	
**************************************************************************************/
// start by selecting only population that alters status between observations
// this is the replicated sample
use "ukhlstmlb_pooled.dta", clear
gen wgt = indinui_xw
replace wgt = wgt / 2 if (needCare1!=needCare1_l | recCare!=recCare_l | careMarket!=careMarket_l | recPartner!=recPartner_l | whoCares!=whoCares_l | whoCares2!=whoCares2_l)
save "ukhlstmc_pooled.dta", replace
keep if (needCare1!=needCare1_l | recCare!=recCare_l | careMarket!=careMarket_l | recPartner!=recPartner_l | whoCares!=whoCares_l | whoCares2!=whoCares2_l)
replace indinui_xw = .
gen needCare1_l1 = needCare1
gen recCare_l1 = recCare
gen careMarket_l1 = careMarket
gen recPartner_l1 = recPartner
gen whoCares_l1 = whoCares
gen whoCares2_l1 = whoCares2
save "ukhlstmd_pooled.dta", replace
use "ukhlstmc_pooled.dta", clear
gen needCare1_l1 = needCare1_l
gen recCare_l1 = recCare_l
gen careMarket_l1 = careMarket_l
gen recPartner_l1 = recPartner_l
gen whoCares_l1 = whoCares_l
gen whoCares2_l1 = whoCares2_l
append using "ukhlstmd_pooled.dta"
save "ukhlstme_pooled.dta", replace


/**************************************************************************************
*	evaluate regressions
**************************************************************************************/
use "ukhlstme_pooled.dta", clear

// london is drgn=7
// probit for need care (2a)
//probit needCare1 male i.deh_c3 partner needCare1_l i.scsf1 i.dage1 ib7.drgn [pweight=indinui_xw], vce(r)
probit needCare1 male i.deh_c3 partner needCare1_l1 i.scsf1 i.dage1 ib7.drgn [pweight=wgt], vce(r)
putexcel set "$outdir/probitNeedCareOver64", modify
putexcel A1 = matrix(e(b))
putexcel A2 = matrix(e(V))

// probit for receive care (2b)
//probit recCare male i.deh_c3 partner recCare_l i.scsf1 i.dage1 ib7.drgn [pweight=indinui_xw], vce(r)
probit recCare male i.deh_c3 partner recCare_l1 i.scsf1 i.dage1 ib7.drgn [pweight=wgt], vce(r)
putexcel set "$outdir/probitRecCareOver64", modify
putexcel A1 = matrix(e(b))
putexcel A2 = matrix(e(V))

// mlogit for formal/informal care (2c)
//mlogit careMarket i.deh_c3 partner i.careMarket_l dage3 ib7.drgn if (recCare==1) [pweight=indinui_xw], vce(r)
mlogit careMarket i.deh_c3 partner i.careMarket_l1 dage3 ib7.drgn if (recCare==1) [pweight=wgt], vce(r)
putexcel set "$outdir/mlogitCareMarketOver64", modify
putexcel A1 = matrix(e(b))
putexcel A2 = matrix(e(V))

// probit of partner care (2d)
//probit recPartner male recPartner_l dage3 ib7.drgn [pweight=indinui_xw] if (recCare==1 & partner==1), vce(r) 
probit recPartner male recPartner_l1 recFormCare dage3 ib7.drgn [pweight=wgt] if (recInfCare==1 & partner==1), vce(r) 
putexcel set "$outdir/probitParnerCareOver64", modify
putexcel A1 = matrix(e(b))
putexcel A2 = matrix(e(V))

// mlogit of other carers for people who have partners caring for them (2e - allowing for formal care not significant)
//mlogit whoCares i.whoCares_l if (recPartner==1) [pweight=indinui_xw], vce(r)
mlogit whoCares i.whoCares_l1 recPartner_l1 if (recPartner==1) [pweight=wgt], vce(r)
putexcel set "$outdir/mlogitPartnerOthCareOver64", modify
putexcel A1 = matrix(e(b))
putexcel A2 = matrix(e(V))

// mlogit of other carers for people who have partners caring for them (2f - lag partner and formal care dummies not significant)
//mlogit whoCares2 i.whoCares2_l if (recPartner==0) [pweight=indinui_xw], vce(r)
mlogit whoCares2 i.whoCares2_l1 if (recPartner==0 & recInfCare==1) [pweight=wgt], vce(r)
putexcel set "$outdir/mlogitNoPartnerOthCareOver64", modify
putexcel A1 = matrix(e(b))
putexcel A2 = matrix(e(V))

// linear regression for partner hours (2g)
gen lhourPartner = ln(hourPartner) if (recPartner==1)
//regress hourPartner male i.deh_c3 recDaughter recSon recOther recFormCare i.scsf1 i.dage1 ib7.drgn if (recPartner==1) [pweight=indinui_xw], vce(r)
//regress hourPartner male i.deh_c3 recDaughter recSon recOther recFormCare i.scsf1 if (recPartner==1) [pweight=indinui_xw], vce(r)
//regress hourPartner male i.deh_c3 recDaughter recSon recOther recFormCare poorHealth ib7.drgn if (recPartner==1) [pweight=indinui_xw], vce(r)
regress lhourPartner male i.deh_c3 recDaughter recSon recOther recFormCare poorHealth ib7.drgn if (recPartner==1) [pweight=indinui_xw], vce(r)
putexcel set "$outdir/linearPartnerHoursOver64", modify
putexcel A1 = matrix(e(b))
putexcel A2 = matrix(e(V))

// linear regression for daughter hours (2h)
gen lhourDaughter = ln(hourDaughter) if (recDaughter==1)
//regress hourDaughter male i.deh_c3 recPartner recSon recOther recFormCare poorHealth ib7.drgn if (recDaughter==1) [pweight=indinui_xw], vce(r)
regress lhourDaughter male i.deh_c3 recPartner recSon recOther recFormCare poorHealth ib7.drgn if (recDaughter==1) [pweight=indinui_xw], vce(r)
putexcel set "$outdir/linearDaughterHoursOver64", modify
putexcel A1 = matrix(e(b))
putexcel A2 = matrix(e(V))

// linear regression for son hours (2i)
gen lhourSon = ln(hourSon) if (recSon==1)
//regress hourSon male i.deh_c3 recPartner recDaughter recOther recFormCare poorHealth ib7.drgn if (recSon==1) [pweight=indinui_xw], vce(r)
regress lhourSon male i.deh_c3 recPartner recDaughter recOther recFormCare poorHealth ib7.drgn if (recSon==1) [pweight=indinui_xw], vce(r)
putexcel set "$outdir/linearSonHoursOver64", modify
putexcel A1 = matrix(e(b))
putexcel A2 = matrix(e(V))

// linear regression for other hours (2j)
gen lhourOther = ln(hourOtherCare) if (recOther==1)
//regress hourOtherCare male i.deh_c3 recPartner recDaughter recSon recFormCare poorHealth ib7.drgn if (recOther==1) [pweight=indinui_xw], vce(r)
regress lhourOther male i.deh_c3 recPartner recDaughter recSon recFormCare poorHealth ib7.drgn if (recOther==1) [pweight=indinui_xw], vce(r)
putexcel set "$outdir/linearOtherHoursOver64", modify
putexcel A1 = matrix(e(b))
putexcel A2 = matrix(e(V))

// linear regression for formal hours (2k)
gen lhourFormal = ln(hourFormCare) if (recFormCare==1)
//regress hourFormCare male i.deh_c3 recPartner recDaughter recSon recOther poorHealth ib7.drgn if (recFormCare==1) [pweight=indinui_xw], vce(r)
//regress lhourFormal male i.deh_c3 recPartner recDaughter recSon recOther poorHealth ib7.drgn if (recFormCare==1) [pweight=indinui_xw], vce(r)
regress lhourFormal male i.deh_c3 recInfCare poorHealth ib7.drgn if (recFormCare==1) [pweight=indinui_xw], vce(r)
putexcel set "$outdir/linearFormalHoursOver64", modify
putexcel A1 = matrix(e(b))
putexcel A2 = matrix(e(V))


