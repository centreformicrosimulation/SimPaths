/**************************************************************************************
*
*	PROGRAM TO EVALUATE SOCIAL CARE SUPPLY FROM UKHLS DATA
*	ANALYSIS BASED ON THE CARE MODULE OF UKHLS
*	First version: Justin van de Ven, 16 Oct 2023
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
global UKHLSWaves "f g h i j k l"


/**************************************************************************************
*	load data
**************************************************************************************/
// pooled data
foreach waveid in $UKHLSWaves {

	local waveno=strpos("abcdefghijklmnopqrstuvwxyz","`waveid'")

	use "$datadir/`waveid'_indresp.dta", clear
	rename *, l
	rename `waveid'_* *
	gen wave = `waveno'
	keep pidp hidp pno intdaty_dv wave sex dvage ppid gor_dv hiqual_dv jbstat aidhh aidxhh aidhrs aidhu* aideft scsf1 indinui_xw naidxhh
	save "$outdir/int_temp.dta", replace
	
	use "$datadir/`waveid'_egoalt.dta", clear
	rename *, l
	rename `waveid'_* *
	sort pidp apno
	forval ii = 1/16 {
		gen rindiv_here = 0
		replace rindiv_here = relationship_dv if (apno == `ii')
		by pidp: egen rindiv`ii' = max(rindiv_here)
		drop rindiv_here
	}
	gen chk = 0
	replace chk = 1 if (pidp == pidp[_n-1])
	drop if (chk==1)
	drop chk
	merge 1:1 pidp using "$outdir/int_temp.dta", keep(2 3) nogen
	keep pidp hidp pno intdaty_dv wave sex dvage ppid gor_dv hiqual_dv jbstat aidhh aidxhh aidhrs aidhu* aideft scsf1 indinui_xw naidxhh rindiv*
	save "$outdir/ukhls_supply_`waveid'.dta", replace
}
clear all
foreach waveid in $UKHLSWaves {
	if ("`waveid'" == "f") {
		use "$outdir/ukhls_supply_`waveid'.dta", clear
	}
	else {
		append using "$outdir/ukhls_supply_`waveid'.dta"
	}
}
save "$outdir/ukhls_supply_pooled0.dta", replace


/**************************************************************************************
*	process variables
**************************************************************************************/
use "ukhls_supply_pooled0.dta", clear

rename intdaty_dv year
rename gor_dv drgn

gen male = (sex==1)
gen partner = (ppid>0)
gen emp = (jbstat==1 | jbstat==2)

// deh_c3 = 1 (degree), 2 (GCSE to other higher degree), 3 (other/no qualification)
gen deh_c3 = .
replace deh_c3 = 1 if (hiqual_dv==1)
replace deh_c3 = 2 if (hiqual_dv>1 & hiqual_dv<5)
replace deh_c3 = 3 if (hiqual_dv>4)

// provision of care
gen provCare = (aidhh==1) | (aidxhh==1)
gen naidhh = 0
forval ii = 1/16 {
	replace naidhh = naidhh + 1 if (aidhua`ii' > 0)
}
gen nAidTot = naidhh + naidxhh * (naidxhh>0)
forval ii = 1/16 {
	replace rindiv`ii' = 0 if (missing(rindiv`ii'))
}

// relationship to care recipient in household
gen care_partner = 0
gen care_parent = 0
gen care_child = 0
gen care_sibling = 0
gen care_grandchild = 0
gen care_grandparent = 0
gen care_otherfamily = 0
gen care_other = 0
forval ii = 1/16 {
	// loop over each individual
	
	replace care_partner = 1 if (aidhua`ii'>0 & rindiv`ii' > 0 & rindiv`ii' < 4)
	replace care_parent = 1 if (aidhua`ii'>0 & rindiv`ii' > 3 & rindiv`ii' < 9)
	replace care_child = 1 if (aidhua`ii'>0 & rindiv`ii' > 8 & rindiv`ii' < 14)
	replace care_sibling = 1 if (aidhua`ii'>0 & rindiv`ii' > 13 & rindiv`ii' < 20)
	replace care_grandchild = 1 if (aidhua`ii'>0 & rindiv`ii' == 20)
	replace care_grandparent = 1 if (aidhua`ii'>0 & rindiv`ii' == 21)
	replace care_otherfamily = 1 if (aidhua`ii'>0 & rindiv`ii' > 21 & rindiv`ii' < 26)
	replace care_other = 1 if (aidhua`ii'>0 & rindiv`ii' > 25)
}
replace care_parent = 1 if (aidhu1 == 1 | aidhu2 == 1)
replace care_grandparent = 1 if (aidhu1 == 2 | aidhu2 == 2)
replace care_otherfamily = 1 if (aidhu1 == 3 | aidhu2 == 3)
replace care_otherfamily = 1 if (aidhu1 == 4 | aidhu2 == 4)
replace care_other = 1 if (aidhu1 > 4 | aidhu2 > 4) 
gen care_others = care_sibling + care_grandchild + care_grandparent + care_otherfamily + care_other

gen aidhrs_adj = 0
replace aidhrs_adj = 2 if (aidhrs==1)
replace aidhrs_adj = 7 if (aidhrs==2)
replace aidhrs_adj = 14.5 if (aidhrs==3)
replace aidhrs_adj = 27.5 if (aidhrs==4)
replace aidhrs_adj = 42 if (aidhrs==5)
replace aidhrs_adj = 74.5 if (aidhrs==6)
replace aidhrs_adj = 120 if (aidhrs==7)
replace aidhrs_adj = 5.48 if (aidhrs==8) //weighted average of 1 to 3
replace aidhrs_adj = 71.5 if (aidhrs==9) //weighted average of 4 to 7

replace drgn = . if (drgn<0)
replace scsf1 = . if (scsf1<0)
gen dage1 = 0
forval ii = 1/14 {
	replace dage1 = `ii' if (dvage>=15+5*(`ii'-1) & dvage<=19+5*(`ii'-1))
}
replace dage1 = 15 if (dvage>=85)

gen poorHealth = (scsf1==5)

gen care_nonpartner = (care_parent + care_child + care_others > 0)
gen careWho = 0
replace careWho = 1 if (care_partner==1 & care_nonpartner==0)
replace careWho = 2 if (care_partner==1 & care_nonpartner==1)
replace careWho = 3 if (care_partner==0 & care_nonpartner==1)

save "ukhls_supply_pooled.dta", replace


/**************************************************************************************
*	basic statistical analysis
**************************************************************************************/
/*
use "ukhls_supply_pooled.dta", clear
keep if (year<2021 & year>2014)
matrix store = J(14,4,.)
forval ii = 1/14{
	qui {
		if (`ii'==14) {
			local maxAge = 150
		}
		else {
			local maxAge = 20+5*(`ii'-1)
		}
		sum provCare [aweight=indinui_xw] if (dvage>15+5*(`ii'-1) & dvage<`maxAge' & male==1) 
		matrix store[`ii',1] = r(mean)
		sum aidhrs_adj [aweight=indinui_xw] if (dvage>15+5*(`ii'-1) & dvage<`maxAge' & provCare>0 & male==1) 
		matrix store[`ii',2] = r(mean)
		sum provCare [aweight=indinui_xw] if (dvage>15+5*(`ii'-1) & dvage<`maxAge' & male==0) 
		matrix store[`ii',3] = r(mean)
		sum aidhrs_adj [aweight=indinui_xw] if (dvage>15+5*(`ii'-1) & dvage<`maxAge' & provCare>0 & male==0) 
		matrix store[`ii',4] = r(mean)
	}
}
matlist store

matrix store1 = J(5,4,.)
forval ii = 1/5{
	qui {
		sum provCare [aweight=indinui_xw] if (scsf1==`ii' & male==1) 
		matrix store1[`ii',1] = r(mean)
		sum aidhrs_adj [aweight=indinui_xw] if (scsf1==`ii' & provCare>0 & male==1) 
		matrix store1[`ii',2] = r(mean)
		sum provCare [aweight=indinui_xw] if (scsf1==`ii' & male==0) 
		matrix store1[`ii',3] = r(mean)
		sum aidhrs_adj [aweight=indinui_xw] if (scsf1==`ii' & provCare>0 & male==0) 
		matrix store1[`ii',4] = r(mean)
	}
}
matlist store1

matrix store2 = J(4,4,.)
forval ii = 1/3{
	qui {
		sum provCare [aweight=indinui_xw] if (deh_c3==`ii' & male==1) 
		matrix store2[`ii',1] = r(mean)
		sum aidhrs_adj [aweight=indinui_xw] if (deh_c3==`ii' & provCare>0 & male==1) 
		matrix store2[`ii',2] = r(mean)
		sum provCare [aweight=indinui_xw] if (deh_c3==`ii' & male==0) 
		matrix store2[`ii',3] = r(mean)
		sum aidhrs_adj [aweight=indinui_xw] if (deh_c3==`ii' & provCare>0 & male==0) 
		matrix store2[`ii',4] = r(mean)
	}
}
qui {
	sum provCare [aweight=indinui_xw] if (male==1) 
	matrix store2[4,1] = r(mean)
	sum aidhrs_adj [aweight=indinui_xw] if (provCare>0 & male==1) 
	matrix store2[4,2] = r(mean)
	sum provCare [aweight=indinui_xw] if (male==0) 
	matrix store2[4,3] = r(mean)
	sum aidhrs_adj [aweight=indinui_xw] if (provCare>0 & male==0) 
	matrix store2[4,4] = r(mean)
}
matlist store2

matrix store3 = J(4,3,.)
forval ii = 1/4 {
	qui {
		if (`ii'<4) {
			gen chk = (nAidTot == `ii')
		}
		else {
			gen chk = (nAidTot > 3)
		}
		sum chk [aweight=indinui_xw] if (nAidTot>0 & male==1)
		matrix store3[`ii',1] = r(mean)
		sum chk [aweight=indinui_xw] if (nAidTot>0 & male==0)
		matrix store3[`ii',2] = r(mean)
		sum chk [aweight=indinui_xw] if (nAidTot>0)
		matrix store3[`ii',3] = r(mean)
		drop chk
	}
}
matlist store3

matrix store4 = J(8,3,.)
local ii = 1
foreach vv of varlist care_partner care_parent care_child care_sibling care_grandchild care_grandparent care_otherfamily care_other {
	qui {
		sum `vv' [aweight=indinui_xw] if (provCare>0 & male==1)
		matrix store4[`ii',1] = r(mean)
		sum `vv' [aweight=indinui_xw] if (provCare>0 & male==0)
		matrix store4[`ii',2] = r(mean)
		sum `vv' [aweight=indinui_xw] if (provCare>0)
		matrix store4[`ii',3] = r(mean)
		local ii = `ii' + 1
	}
}
matlist store4

matrix store5 = J(7,6,.)
gen chk2 = (jbstat==1 | jbstat==2)
gen chk3 = (jbstat==3)
gen chk4 = (aideft==1)
gen chk5 = (aideft==2)
gen chk6 = (aideft==3 | jbstat==6)
forval ii = 1/6 {

	if (`ii' == 1) {
		gen chk1 = (aidhrs_adj<5)
	}
	else if (`ii' == 2) {
		gen chk1 = (aidhrs_adj>4) * (aidhrs_adj<10)
	}
	else if (`ii' == 3) {
		gen chk1 = (aidhrs_adj>9) * (aidhrs_adj<20)
	}
	else if (`ii' == 4) {
		gen chk1 = (aidhrs_adj>19) * (aidhrs_adj<40)
	}
	else if (`ii' == 5) {
		gen chk1 = (aidhrs_adj>39) * (aidhrs_adj<80)
	}
	else if (`ii' == 6) {
		gen chk1 = (aidhrs_adj>79)
	}
	sum chk1 [aweight=indinui_xw] if (provCare>0 & dvage>19 & dvage<60 & jbstat!=4 & jbstat!=8)
	matrix store5[`ii',1] = r(mean)
	sum chk2 [aweight=indinui_xw] if (provCare>0 & dvage>19 & dvage<60 & jbstat!=4 & jbstat!=8 & chk1==1)
	matrix store5[`ii',2] = r(mean)
	sum chk3 [aweight=indinui_xw] if (provCare>0 & dvage>19 & dvage<60 & jbstat!=4 & jbstat!=8 & chk1==1)
	matrix store5[`ii',3] = r(mean)
	sum chk4 [aweight=indinui_xw] if (provCare>0 & dvage>19 & dvage<60 & jbstat!=4 & jbstat!=8 & chk1==1)
	matrix store5[`ii',4] = r(mean)
	sum chk5 [aweight=indinui_xw] if (provCare>0 & dvage>19 & dvage<60 & jbstat!=4 & jbstat!=8 & chk1==1)
	matrix store5[`ii',5] = r(mean)
	sum chk6 [aweight=indinui_xw] if (provCare>0 & dvage>19 & dvage<60 & jbstat!=4 & jbstat!=8 & chk1==1)
	matrix store5[`ii',6] = r(mean)
	drop chk1
}
matrix store5[7,1] = 1
sum chk2 [aweight=indinui_xw] if (provCare>0 & dvage>19 & dvage<60 & jbstat!=4 & jbstat!=8)
matrix store5[7,2] = r(mean)
sum chk3 [aweight=indinui_xw] if (provCare>0 & dvage>19 & dvage<60 & jbstat!=4 & jbstat!=8)
matrix store5[7,3] = r(mean)
sum chk4 [aweight=indinui_xw] if (provCare>0 & dvage>19 & dvage<60 & jbstat!=4 & jbstat!=8)
matrix store5[7,4] = r(mean)
sum chk5 [aweight=indinui_xw] if (provCare>0 & dvage>19 & dvage<60 & jbstat!=4 & jbstat!=8)
matrix store5[7,5] = r(mean)
sum chk6 [aweight=indinui_xw] if (provCare>0 & dvage>19 & dvage<60 & jbstat!=4 & jbstat!=8)
matrix store5[7,6] = r(mean)
matlist store5
*/


/**************************************************************************************
*	prepare time-series data for regression analysis
**************************************************************************************/
clear all
forval ii = 6/12 {
	use "ukhls_supply_pooled.dta", clear
	keep if (wave == `ii')
	save "ukhlst_supply_`ii'.dta", replace
}
forval ii = 6/11 {
	use "ukhlst_supply_`ii'.dta", clear
	rename * *_l
	rename pidp_l pidp
	local jj = `ii' + 1
	merge 1:1 pidp using "ukhlst_supply_`jj'.dta"
	save "ukhlstml_supply_`jj'.dta", replace
}

clear all
append using "ukhlstml_supply_7.dta" "ukhlstml_supply_8.dta" "ukhlstml_supply_9.dta" "ukhlstml_supply_10.dta" "ukhlstml_supply_11.dta" "ukhlstml_supply_12.dta"
save "ukhlstml_supply_pooled.dta", replace


/**************************************************************************************
*	evaluate regressions
**************************************************************************************/
use "ukhlstml_supply_pooled.dta", clear

// probit regression for people providing care to partners (3a)
probit care_nonpartner male i.deh_c3 i.scsf1 i.careWho_l ib1.dage1 ib7.drgn [pweight=indinui_xw] if (care_partner==1 & dvage>17), vce(r)
putexcel set "$outdir/probitCareWithPartner", modify
putexcel A1 = matrix(e(b))
putexcel A2 = matrix(e(V))

// probit regression for people not providing care to partners (3b)
probit care_nonpartner male i.deh_c3 i.scsf1 i.careWho_l partner ib1.dage1 ib7.drgn [pweight=indinui_xw] if (care_partner==0 & dvage>17), vce(r)
putexcel set "$outdir/probitCareWithoutPartner", modify
putexcel A1 = matrix(e(b))
putexcel A2 = matrix(e(V))

// probit for single people (cannot provide care to partners) - needed because pooled mlogit doesn't converge (3c)
probit careWho male i.deh_c3 i.scsf1 i.careWho_l ib1.dage1 ib7.drgn [pweight=indinui_xw] if (partner==0 & dvage>17), vce(r)
putexcel set "$outdir/probitCareNoPartner", modify
putexcel A1 = matrix(e(b))
putexcel A2 = matrix(e(V))

// multinomial logit regression for social care provision of people with partners (3d)
gen dage2 = 0
replace dage2 = 1 if (dvage<35)
replace dage2 = 2 if (dvage>34 & dvage<45)
replace dage2 = 3 if (dvage>44 & dvage<55)
replace dage2 = 4 if (dvage>54 & dvage<65)
replace dage2 = 5 if (dvage>64)
mlogit careWho male i.deh_c3 i.scsf1 i.careWho_l ib1.dage2 ib7.drgn [pweight=indinui_xw] if (dvage>17 & partner==1), vce(r)
putexcel set "$outdir/mlogitCareWho", modify
putexcel A1 = matrix(e(b))
putexcel A2 = matrix(e(V))

// hours regression (3e)
gen lhourCare = ln(aidhrs_adj) if (aidhrs_adj>0)
regress lhourCare male i.deh_c3 i.scsf1 i.careWho partner ib1.dage1 ib7.drgn [pweight=indinui_xw] if (dvage>17 & careWho>0), vce(r)
putexcel set "$outdir/linearHoursCareAnyone", modify
putexcel A1 = matrix(e(b))
putexcel A2 = matrix(e(V))


/*****************************************************************
*	working variants
*****************************************************************/

// london is drgn=7
probit care_partner male i.deh_c3 partner care_partner_l i.scsf1 ib1.dage1 ib7.drgn [pweight=indinui_xw] if (partner==1 & dvage>17), vce(r)
putexcel set "$outdir/probitCarePartner", modify
putexcel A1 = matrix(e(b))
putexcel A2 = matrix(e(V))

probit care_parent male i.deh_c3 partner care_parent_l i.scsf1 ib1.dage1 ib7.drgn [pweight=indinui_xw] if (dvage>17), vce(r)
putexcel set "$outdir/probitCareParent", modify
putexcel A1 = matrix(e(b))
putexcel A2 = matrix(e(V))

probit care_child male i.deh_c3 partner care_child_l i.scsf1 ib1.dage1 ib7.drgn [pweight=indinui_xw] if (dvage>17), vce(r)
putexcel set "$outdir/probitCareChild", modify
putexcel A1 = matrix(e(b))
putexcel A2 = matrix(e(V))

probit care_others male i.deh_c3 partner care_others_l i.scsf1 ib1.dage1 ib7.drgn [pweight=indinui_xw] if (dvage>17), vce(r)
putexcel set "$outdir/probitCareOther", modify
putexcel A1 = matrix(e(b))
putexcel A2 = matrix(e(V))

probit provCare male i.deh_c3 partner provCare_l i.scsf1 ib1.dage1 ib7.drgn [pweight=indinui_xw] if (dvage>17), vce(r)
putexcel set "$outdir/probitCareAny", modify
putexcel A1 = matrix(e(b))
putexcel A2 = matrix(e(V))

gen lhourCare = ln(aidhrs_adj) if (aidhrs_adj>0)
regress lhourCare male i.deh_c3 partner provCare_l i.scsf1 ib1.dage1 ib7.drgn [pweight=indinui_xw] if (dvage>17), vce(r)
putexcel set "$outdir/linearHoursCareAnyone", modify
putexcel A1 = matrix(e(b))
putexcel A2 = matrix(e(V))


