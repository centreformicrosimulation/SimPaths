/*********************************************************************
 MASTER VARIABLE CONSTRUCTION AND TRANSFORMATIONS DO-FILE
*********************************************************************/

*==================================================
* Ensure missing is coded as missing
*==================================================
foreach var in idhh idperson idpartner idfather idmother dct drgn1 dwt dnc02 dnc dgn dgnsp dag dagsq dhe dhesp dcpst ///
    ded deh_c3 deh_c4 der dehsp_c3 dehsp_c4 dehm_c3 dehf_c3 dehmf_c3 dcpen dcpyy dcpex ///
    dlltsd dlltsd01 dlrtrd drtren dlftphm dhhtp_c4 dhhtp_c8 dhm dhm_ghq ///
    jbhrs jshrs j2hrs jbstat les_c3 les_c4 lessp_c3 lessp_c4 lesdf_c4 ydses_c5 scghq2_dv ///
    ypnbihs_dv yptciihs_dv yplgrs_dv swv sedex ssscp sprfm sedag stm dagsp lhw l1_lhw ///
    pno ppno hgbioad1 hgbioad2 der obs_earnings_hourly l1_obs_earnings_hourly ///
    dhh_owned econ_benefits econ_benefits_nonuc econ_benefits_uc ///
    scghq2_dv_miss_flag dchpd dagpns dagpns_sp CPI lesnr_c2 dlltsd01 dlltsd_sp dlltsd01_sp ///
    ypnoab flag* dhe_mcs dhe_pcs dhe_mcssp dhe_pcssp dls dot dot01 unemp new_rel {
        qui recode `var' (-9/-1=.)
}

*==================================================
* Student flag
*==================================================
cap drop Dst
gen Dst = .
replace Dst = 0 if les_c3 != 2
replace Dst = 1 if les_c3 == 2
replace Dst = . if les_c3 == .

*==================================================
* Age transformations
*==================================================

gen Dag    = dag
gen L_Dag = L.dag
gen Dag_sq = dagsq
gen Age = dag
gen AgeSquared = dag^2
gen L_Dag_sq = L.dagsq

gen Dag_c    = dag - 23
gen Dag_c_sq = Dag_c^2

gen Dag_post25 = (dag > 25) * (dag - 25)

gen Dag_post21    = (dag > 21) * (dag - 21)
gen Dag_post18_sq = (dag > 18) * (dag - 18)^2
gen Dag_post21_sq = (dag > 21) * (dag - 21)^2
gen Dag_post25_sq = (dag > 25) * (dag - 25)^2
gen Dag_post26_sq = (dag > 26) * (dag - 26)^2

mkspline rcs = dag, cubic knots(18 21 23 26)

*==================================================
* Time transformations
*==================================================

replace stm = stm - 2000

foreach y of numlist 11/25 {
    gen y20`y' = (stm == `y')
}

foreach y of numlist 2011/2025 {
    gen Y`y' = y`y'
}

gen year_post2020 = (stm > 20) * (stm - 20)
gen Y2223 = inlist(stm, 22, 23)
gen Year_transformed = stm

gen Year = stm
gen YearSquared = stm^2

*==================================================
* Income variables
*==================================================

gen receives_ypncp = ypncp > 0 & !missing(ypncp) //capital income

gen receives_ypnoab = ypnoab > 0 & !missing(ypnoab) //private pension income 

recode ynbcpdf_dv (-999 = .)

tab ydses_c5 , gen(Ydses_c5_Q)

*==================================================
* Education recoding
*==================================================
cap drop deh_c3_recoded
recode deh_c3 (1 = 3) (3 = 1), gen(deh_c3_recoded)

cap lab define deh_c3_recoded   1 "Low" 2 "Medium" 3 "High"
label values deh_c3_recoded deh_c3_recoded


*==================================================
* Region dummies
*==================================================

gen Dgn = dgn
tab drgn1, gen(UK)

rename UK1  UKC
rename UK2  UKD
rename UK3  UKE
rename UK4  UKF
rename UK5  UKG
rename UK6  UKH
rename UK7  UKI
rename UK8  UKJ
rename UK9  UKK
rename UK10 UKL
rename UK11 UKM
rename UK12 UKN

*==================================================
* Employment dummies
*==================================================

tab les_c3, gen(Les_c3_)
rename Les_c3_1 Les_c3_Employed
rename Les_c3_2 Les_c3_Student
rename Les_c3_3 Les_c3_NotEmployed

tab lessp_c3, gen(Lessp_c3_)
rename Lessp_c3_1 Lessp_c3_Employed
rename Lessp_c3_2 Lessp_c3_Student
rename Lessp_c3_3 Lessp_c3_NotEmployed

tab les_c4, gen(Les_c4_)
rename Les_c4_1 Les_c4_Employed
rename Les_c4_2 Les_c4_Student
rename Les_c4_3 Les_c4_NotEmployed
rename Les_c4_4 Les_c4_Retired

tab lesdf_c4, gen(Lesdf_c4_)
rename Lesdf_c4_1 Lesdf_c4_BothEmployed
rename Lesdf_c4_2 Lesdf_c4_EmpSpouseNotEmp
rename Lesdf_c4_3 Lesdf_c4_NotEmpSpouseEmp
rename Lesdf_c4_4 Lesdf_c4_BothNotEmployed

*==================================================
* Education dummies
*==================================================

tab deh_c3, gen(Deh_c3_)
rename Deh_c3_1 Deh_c3_High
rename Deh_c3_2 Deh_c3_Medium
rename Deh_c3_3 Deh_c3_Low

tab deh_c4, gen(Deh_c4_)
rename Deh_c4_1 Deh_c4_Na
rename Deh_c4_2 Deh_c4_High
rename Deh_c4_3 Deh_c4_Medium
rename Deh_c4_4 Deh_c4_Low

tab dehmf_c3, gen(Dehmf_c3_)
rename Dehmf_c3_1 Dehmf_c3_High
rename Dehmf_c3_2 Dehmf_c3_Medium
rename Dehmf_c3_3 Dehmf_c3_Low

tab dehsp_c3, gen(Dehsp_c3_)
rename Dehsp_c3_1 Dehsp_c3_High
rename Dehsp_c3_2 Dehsp_c3_Medium
rename Dehsp_c3_3 Dehsp_c3_Low

*==================================================
* Health dummies
*==================================================
cap lab define dhe 1 "Poor" 2 "Fair" 3 "Good" 4 "VeryGood" 5 "Excellent" , modify 
lab values dhe dhe 

tab dhe, gen(Dhe_)
rename Dhe_1 Dhe_Poor
rename Dhe_2 Dhe_Fair
rename Dhe_3 Dhe_Good
rename Dhe_4 Dhe_VeryGood
rename Dhe_5 Dhe_Excellent

tab dhesp, gen(Dhesp_)
rename Dhesp_1 Dhesp_Poor
rename Dhesp_2 Dhesp_Fair
rename Dhesp_3 Dhesp_Good
rename Dhesp_4 Dhesp_VeryGood
rename Dhesp_5 Dhesp_Excellent

gen Dhe_pcs   = dhe_pcs
gen Dhe_mcs   = dhe_mcs
gen Dhe_pcssp = dhe_pcssp
gen Dhe_mcssp = dhe_mcssp

gen FinancialDistress = financial_distress
gen L_FinancialDistress = L.financial_distress

*==================================================
* Long-term sick or disabled
*==================================================

gen Dlltsd   = dlltsd
gen Dlltsd01 = dlltsd01


*==================================================
* Ethnicity
*==================================================

tab dot, gen(dot_)
rename dot_1 Ethn_White
rename dot_2 Ethn_Asian
rename dot_3 Ethn_Black
rename dot_4 Ethn_Other

*==================================================
* Household type and relationship dynamics
*==================================================

tab dhhtp_c4, gen(Dhhtp_c4_)
rename Dhhtp_c4_1 Dhhtp_c4_CoupleNoChildren
rename Dhhtp_c4_2 Dhhtp_c4_CoupleChildren
rename Dhhtp_c4_3 Dhhtp_c4_SingleNoChildren
rename Dhhtp_c4_4 Dhhtp_c4_SingleChildren

tab dhhtp_c8, gen(Dhhtp_c8_)

gen New_rel = new_rel

tab dcpst, gen(Dcpst_)
rename Dcpst_1 Dcpst_Partnered
rename Dcpst_2 Dcpst_Single
gen Partnered = Dcpst_Partnered
gen Single = Dcpst_Single

gen L_Dcpst_Partnered = L.Dcpst_Partnered
gen L_Dcpst_Single = L.Dcpst_Single

*==================================================
* Explicit lags
*==================================================
cap drop l_* L_*

xtset idperson swv

foreach v in ydses_c5 dhe dhe_pcs dhe_mcs les_c3 les_c4 dhhtp_c4 dlltsd01 need_socare dehmf_c3 {
    gen l_`v' = L.`v'
}

gen L_Ydses_c5 = l_ydses_c5
gen L_Dhe      = l_dhe
gen L_Dhe_pcs  = l_dhe_pcs
gen L_Dhe_mcs  = l_dhe_mcs
gen L_Dlltsd01 = l_dlltsd01
gen L_Need_socare = l_need_socare
gen L_Dehmf_c3 = l_dehmf_c3

tab l_les_c4, gen(L_Les_c4_)
rename L_Les_c4_1 L_Les_c4_Employed
rename L_Les_c4_2 L_Les_c4_Student
rename L_Les_c4_3 L_Les_c4_NotEmployed
rename L_Les_c4_4 L_Les_c4_Retired

tab l_ydses_c5, gen(L_Ydses_c5_Q)

tab l_dhhtp_c4, gen(L_Dhhtp_c4_)
rename L_Dhhtp_c4_1 L_Dhhtp_c4_CoupleNoChildren
rename L_Dhhtp_c4_2 L_Dhhtp_c4_CoupleChildren
rename L_Dhhtp_c4_3 L_Dhhtp_c4_SingleNoChildren
rename L_Dhhtp_c4_4 L_Dhhtp_c4_SingleChildren

tab l_dehmf_c3, gen(L_Dehmf_c3_)
rename L_Dehmf_c3_1 L_Dehmf_c3_High
rename L_Dehmf_c3_2 L_Dehmf_c3_Medium
rename L_Dehmf_c3_3 L_Dehmf_c3_Low



*==================================================
* Copy variables for nice labels 
*==================================================
gen Dnc       = dnc
gen Dnc_L1    = L.dnc
gen L_Dnc    = L.dnc
gen Dnc02     = dnc02
gen Ded       = ded
gen Dhe       = dhe
gen Ydses_c5  = ydses_c5
gen Dcpyy     = dcpyy
gen Dcpagdf   = dcpagdf
gen FertilityRate = dukfr
gen Dhh_owned = dhh_owned

gen Elig_pen    = dagpns
gen Elig_pen_L1 = l.dagpns

gen Reached_Retirement_Age    = dagpns
gen Reached_Retirement_Age_Sp = dagpns_sp

gen Dlltsdsp   = dlltsd_sp
gen Dlltsd01sp = dlltsd01_sp

gen Ypncp = ypncp
gen L_Ypncp = L.ypncp
gen Ypnoab =  ypnoab
gen L_Ypnoab =  L.ypnoab
gen Yplgrs_dv = yplgrs_dv
gen Ypnbihs_dv = ypnbihs_dv
gen Ypnbihs_dv_sq = ypnbihs_dv^2
gen Ynbcpdf_dv   = ynbcpdf_dv
gen Yptciihs_dv  = yptciihs_dv


*==================================================
* Interactions
*==================================================
cap drop Les_c4_*_Dgn Ded_* Reached_Retirement_Age_Les

gen Les_c4_Student_Dgn     = Dgn * Les_c4_Student
gen Les_c4_NotEmployed_Dgn = Dgn * Les_c4_NotEmployed
gen Les_c4_Retired_Dgn     = Dgn * Les_c4_Retired

gen Ded_Dag    = Ded * Dag
gen Ded_Dag_sq = Ded * Dag_sq
gen Ded_Dgn    = Ded * Dgn

gen Ded_Dnc_L1_  = Ded * l.Dnc
gen Ded_Dnc02_L1 = Ded * l.Dnc02

forvalues i = 1/5 {
    gen Ded_Ydses_c5_Q`i'_L1 = Ded * l.Ydses_c5_Q`i'
}

gen Ded_Dehsp_c3_Medium_L1 = l.Dehsp_c3_Medium * Ded
gen Ded_Dehsp_c3_Low_L1    = l.Dehsp_c3_Low * Ded

gen Ded_Dhesp_Good_L1 = l.Dhesp_Good * Ded
gen Ded_Dhesp_Fair_L1 = l.Dhesp_Fair * Ded

gen Ded_Dhe_Fair_L1      = l.Dhe_Fair * Ded
gen Ded_Dhe_Good_L1      = l.Dhe_Good * Ded
gen Ded_Dhe_VeryGood_L1  = l.Dhe_VeryGood * Ded
gen Ded_Dhe_Excellent_L1 = l.Dhe_Excellent * Ded

gen Ded_Dhe_pcs = Ded * Dhe_pcs
gen Ded_Dhe_mcs = Ded * Dhe_mcs

gen Ded_Dhe             = Dhe * Ded
gen Ded_Dcpst_Single    = Dcpst_Single * Ded
gen Ded_Dcpst_Single_L1 = l.Dcpst_Single * Ded

gen Reached_Retirement_Age_Les = Reached_Retirement_Age * l.Les_c3_NotEmployed

gen Ded_Ypncp= Ded * Ypncp
gen Ded_Yplgrs_dv = Ded *Yplgrs_dv


*==================================================
* Prepare data for social care regressions 
*==================================================
* Care received variables (Processes S2)

rename need_socare need_care

gen receive_formal_care = formal_socare_hrs > 0
gen receive_informal_care = (partner_socare_hrs + daughter_socare_hrs + son_socare_hrs + other_socare_hrs) > 0
gen receive_care = max(receive_informal_care, receive_formal_care)

gen CareMarket = .
replace CareMarket = 1 if (receive_informal_care == 0 & receive_formal_care == 0)
replace CareMarket = 2 if (receive_informal_care == 1 & receive_formal_care == 0)
replace CareMarket = 3 if (receive_informal_care == 1 & receive_formal_care == 1)
replace CareMarket = 4 if (receive_informal_care == 0 & receive_formal_care == 1)

lab def labCareMarket 1 "None" 2 "Informal" 3 "Mixed" 4 "Formal"
lab val CareMarket labCareMarket

gen HrsReceivedFormalIHS = asinh(formal_socare_hrs)
cap drop informal_socare_hrs
gen informal_socare_hrs = partner_socare_hrs + daughter_socare_hrs + son_socare_hrs + other_socare_hrs
gen HrsReceivedInformalIHS = asinh(informal_socare_hrs)


* Care provided variables (Processes S3)

gen HrsProvidedInformalIHS = asinh(careHoursProvidedWeekly)
gen provide_informal_care = (careWho >= 1)

* Age variables 
* - Categorical: 15-19, 20-24, ..., 80-84, 85+  
gen dage5 = 0
forval ii = 1/14 {
	replace dage5 = `ii' if (dag>=15+5*(`ii'-1) & dag<=19+5*(`ii'-1))
}
replace dage5 = 15 if (dag >= 85)
//table dage5, stat(min dag) stat(max dag)
tabstat dag, by(dage5) stats(min max)

* - Categorical: <35, 35-44, 45-54, 55-64, 65+ 
gen dage10prime = 0
replace dage10prime = 1 if (dag>34 & dag<45)
replace dage10prime = 2 if (dag>44 & dag<55)
replace dage10prime = 3 if (dag>54 & dag<65)
replace dage10prime = 4 if (dag>64)
//table dage10prime, stat(min dag) stat(max dag)
table dage10prime, c(min dag max dag)

* - Categorical: 65-66, 67-68, 69-70, 71-72..., 85+
gen dage2old = 0
forval ii = 1/10 {
	replace dage2old = `ii' if (dag >= 65+2*(`ii'-1) & dag < 67+2*(`ii'-1))
}
replace dage2old = 11 if (dag >= 85)
//table dage2old, stat(min dag) stat(max dag)
table dage2old, c(min dag max dag)

* Poor health flag
gen poor_health = (dhe == 1)

* Adjust for missing values
replace need_care = . if (need_care<0)
foreach var of varlist formal_socare_hrs partner_socare_hrs daughter_socare_hrs son_socare_hrs other_socare_hrs {
	replace `var' = 0 if (`var' < 0)
}

* Prepare vars for automatic labelling
xtset idperson stm

tab dage5, gen(Age_)
//table dage5, stat(min dag) stat(max dag)	// RMK: AgeXX categories start at 1, hence shifted by 1
tabstat dag, by(dage5) stats(min max)

drop Age_1 Age_2
cap rename Age_3 Age20to24
cap rename Age_4 Age25to29
cap rename Age_5 Age30to34
cap rename Age_6 Age35to39
cap rename Age_7 Age40to44
cap rename Age_8 Age45to49
cap rename Age_9 Age50to54
cap rename Age_10 Age55to59
cap rename Age_11 Age60to64
cap rename Age_12 Age65to69
cap rename Age_13 Age70to74
cap rename Age_14 Age75to79
cap rename Age_15 Age80to84
cap rename Age_16 Age85plus

tab dage10prime, gen(Age_)
//table dage10prime, stat(min dag) stat(max dag)	// RMK: AgeXX categories start at 1, hence shifted by 1
table dage10prime, c(min dag max dag)	
drop Age_1
rename Age_2 Age35to44
rename Age_3 Age45to54
rename Age_4 Age55to64
rename Age_5 Age65plus

tab dage2old, gen(Age_)
//table dage2old, stat(min dag) stat(max dag)	// RMK: AgeXX categories start at 1, hence shifted by 1
table dage2old, c(min dag max dag)
drop Age_1	
rename Age_2 Age65to66
rename Age_3 Age67to68
rename Age_4 Age69to70
rename Age_5 Age71to72
rename Age_6 Age73to74
rename Age_7 Age75to76
rename Age_8 Age77to78
rename Age_9 Age79to80
rename Age_10 Age81to82
rename Age_11 Age83to84
drop Age_12

gen NeedCare = need_care
gen ReceiveCare = receive_care
gen ProvideCare = provide_informal_care

tab CareMarket
gen CareMarketInformal = (CareMarket == 2)
gen CareMarketMixed = (CareMarket == 3)
gen CareMarketFormal = (CareMarket == 4)

tab ydses_c5, gen(HHincomeQ)

gen NeedCare_L1 = L.NeedCare
gen ReceiveCare_L1 = L.ReceiveCare
gen CareMarketFormal_L1 = L.CareMarketFormal
gen CareMarketInformal_L1 = L.CareMarketInformal
gen CareMarketMixed_L1 = L.CareMarketMixed
gen HrsReceivedFormalIHS_L1 = L.HrsReceivedFormalIHS
gen HrsReceivedInformalIHS_L1 = L.HrsReceivedInformalIHS
gen ProvideCare_L1 = L.ProvideCare
gen HrsProvidedInformalIHS_L1 = L.HrsProvidedInformalIHS

* Add partner's outcome variables
preserve
drop if idpartner == -9
keep idperson stm NeedCare ReceiveCare CareMarketFormal CareMarketInformal CareMarketMixed
rename idperson idpartner
rename NeedCare NeedCarePartner
rename ReceiveCare ReceiveCarePartner
rename CareMarketFormal CareMarketFormalPartner
rename CareMarketInformal CareMarketInformalPartner
rename CareMarketMixed CareMarketMixedPartner
save "$dir_work/partner.dta", replace
restore

merge m:1 idpartner stm using "$dir_work/partner.dta"
keep if _merge == 1 | _merge==3 
drop _merge

erase "$dir_work/partner.dta"


/*********************************************************************
 Additional variables required for the following estimation scripts:
 - reg_financial_distress.do
 - reg_health_mental.do
 - reg_health_wellbeing.do
*********************************************************************/

*==================================================
* Modified OECD equivalence scale
*==================================================

bysort swv idhh: egen temp_NinHH0013 = sum(dag >= 0 & dag <= 13)
bysort swv idhh: egen temp_NinHH14up = sum(dag >= 14)

* There needs to be at least one adult in every household, so that
* (temp_NinHH14up - 1) gives us the number of "additional" adults for the
* purposes of the OECD equivalence scale.
assert temp_NinHH14up >= 1

* Modified OECD equivalence scale: 1 for the first adult in the household
* (dag >= 14), 0.5 for each additional adult (dag >= 14), 0.3 for each child
* (dag <= 13)
gen moecd_eq = 1 + (temp_NinHH14up - 1) * 0.5 + (temp_NinHH0013) * 0.3

drop temp*

*==================================================
* Real equivalised household income
*==================================================

bysort swv idhh: egen temp_HH_ydisp = sum(ydisp)
gen temp_realnetinc=temp_HH_ydisp/CPI

*Winsorise income variable
winsor temp_realnetinc, gen(temp_inc_wins) p(0.001)
summ temp_inc_wins, detail

* Generate equivalised household income
gen econ_realequivinc=temp_inc_wins/moecd_eq
label var econ_realequivinc "Real equivalised household income"
drop temp_*

*==================================================
* Log income
*==================================================

gen log_income=ln(econ_realequivinc)
label var log_income "Log of real equivalised household net income"

sort idperson swv
xtset idperson swv
gen RealIncomeDecrease_D = D.log_income

*==================================================
* Income change (binary, increased or decreased)
*==================================================

sort idperson swv
xtset idperson swv
gen temp_incchange=econ_realequivinc - L.econ_realequivinc

gen RealIncomeChange=.
replace RealIncomeChange=1 if (econ_realequivinc < L.econ_realequivinc) & econ_realequivinc!=. & L.econ_realequivinc!=.
replace RealIncomeChange=0 if (econ_realequivinc == L.econ_realequivinc) & econ_realequivinc!=. & L.econ_realequivinc!=.
replace RealIncomeChange=0 if (econ_realequivinc > L.econ_realequivinc) & econ_realequivinc!=. & L.econ_realequivinc!=.

drop temp_*

*==================================================
* Poverty transition
*==================================================

* Generate median income for sample
bysort swv: egen temp_swvMedianIncome = wpctile(econ_realequivinc), p(50) weights(${weight})
* ONS uses net income, before or after housing costs.
* Here we use disposable income (ydisp).
gen temp_swvPovertyThreshold = temp_swvMedianIncome*0.60
label var temp_swvPovertyThreshold "Poverty threshold"

tabstat temp_swvPovertyThreshold, by(swv)
summ temp_swvPovertyThreshold, detail

* Generate poverty marker
gen temp_HHinPoverty = (econ_realequivinc <= temp_swvPovertyThreshold)
replace temp_HHinPoverty=. if missing(econ_realequivinc) | missing(temp_swvPovertyThreshold)
tab temp_HHinPoverty swv, col

* Generate poverty transition variable
sort idperson swv
gen PersistentNonPoverty = 0
gen NonPovertyToPoverty  = 0
gen PovertyToNonPoverty  = 0
gen PersistentPoverty    = 0

replace PersistentNonPoverty =1 if temp_HHinPoverty==0 & L.temp_HHinPoverty==0
replace NonPovertyToPoverty  =1 if temp_HHinPoverty==1 & L.temp_HHinPoverty==0
replace PovertyToNonPoverty  =1 if temp_HHinPoverty==0 & L.temp_HHinPoverty==1
replace PersistentPoverty    =1 if temp_HHinPoverty==1 & L.temp_HHinPoverty==1
drop temp_*

*==================================================
* Employment transitions
*==================================================

* Generate employment volatility exposure
* Only interested in 
* employment (1) to employment (1)
* employment (1) to not employed (3)
* not employed (3) to employed (1)
* not employed (3) to not employed (3)
sort idperson swv
gen  exp_emp=.
gen PersistentEmployed=0
gen EmployedToUnemployed=0
gen UnemployedToEmployed=0
gen PersistentUnemployed=0
* Starting state: employed or self-employed
replace PersistentEmployed=1 if L.les_c4==1 & les_c4==1
replace EmployedToUnemployed=1 if L.les_c4==1 & les_c4==3

* Starting state: not employed
replace UnemployedToEmployed=1 if L.les_c4==3 & les_c4==1
replace PersistentUnemployed=1 if L.les_c4==3 & les_c4==3

*==================================================
* Working hour categories
*==================================================

gen Lhw_0= 0
gen Lhw_10= 0
gen Lhw_20= 0
gen Lhw_30= 0
gen Lhw_40= 0
replace Lhw_0  = 1 if (lhw<=5)
replace Lhw_10 = 1 if (lhw>=6 & lhw<=15)
replace Lhw_20 = 1 if (lhw>=16 & lhw<=25)
replace Lhw_30 = 1 if (lhw>=26 & lhw<=35)
replace Lhw_40 = 1 if (lhw>=36 & lhw!=.)

*==================================================
*  Benefits
*==================================================

sort idperson swv
gen D_Econ_benefits = L.econ_benefits
gen D_Econ_benefits_UC = L.econ_benefits_uc
gen D_Econ_benefits_nonUC = L.econ_benefits_nonuc

gen D_Home_owner = dhh_owned
