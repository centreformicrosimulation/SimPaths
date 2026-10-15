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

* Set data
xtset idperson swv
sort idperson swv


*==================================================
* Student flag
*==================================================
cap drop Dst
gen Dst = .
replace Dst = 0 if les_c3 != 2
replace Dst = 1 if les_c3 == 2
replace Dst = . if les_c3 == .

*==================================================
* In first education spell 
*==================================================
gen Ded       = ded
gen eduSampleFlag = ded
gen eduSampleFlagL1 = l.ded


*==================================================
* Sex and Age transformations
*==================================================

/*--------------------------------------------------*/
/* Sex                                               */
/*--------------------------------------------------*/

gen Dgn = dgn
gen demMaleFlag = dgn


/*--------------------------------------------------*/
/* Raw age variables                                */
/*--------------------------------------------------*/

gen Dag = dag
gen Dag_sq = dagsq

gen Age = dag
gen AgeSquared = dag^2 if !missing(dag)

gen demAge = dag
gen demAgeSq = dagsq


/*--------------------------------------------------*/
/* Centered age terms                               */
/*--------------------------------------------------*/

gen Dag_c = dag - 23 if !missing(dag)
gen Dag_c_sq = Dag_c^2 if !missing(Dag_c)


/*--------------------------------------------------*/
/* Piecewise linear age terms                       */
/*--------------------------------------------------*/

gen Dag_post21 = (dag > 21) * (dag - 21) if !missing(dag)
gen Dag_post25 = (dag > 25) * (dag - 25) if !missing(dag)


/*--------------------------------------------------*/
/* Piecewise quadratic age terms                    */
/*--------------------------------------------------*/

gen Dag_post18_sq = (dag > 18) * (dag - 18)^2 if !missing(dag)
gen Dag_post21_sq = (dag > 21) * (dag - 21)^2 if !missing(dag)
gen Dag_post25_sq = (dag > 25) * (dag - 25)^2 if !missing(dag)
gen Dag_post26_sq = (dag > 26) * (dag - 26)^2 if !missing(dag)


/*--------------------------------------------------*/
/* Restricted cubic splines                         */
/*--------------------------------------------------*/

mkspline rcs = dag, cubic knots(18 21 23 26)


/*--------------------------------------------------*/
/* Pension age eligibility                          */
/*--------------------------------------------------*/

gen Elig_pen = dagpns
gen Elig_pen_L1 = L1.dagpns

gen Reached_Retirement_Age = dagpns
gen Reached_Retirement_Age_Sp = dagpns_sp

gen demPensAgeFlag = dagpns
gen demPensPartnerAgeFlag = dagpns_sp


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

*refactored 

gen demYearTransformed = stm

gen demYear= stm
foreach y of numlist 11/25 {
    gen demYear20`y' = (demYear == `y')
	}

/*==================================================*/
/* INCOME                                           */
/*==================================================*/

/*--------------------------------------------------*/
/* Raw income variables                             */
/*--------------------------------------------------*/

gen Ydses_c5 = ydses_c5

gen Ypncp = ypncp
gen L_Ypncp = L.ypncp if !missing(L.Ypncp)
gen Ypnoab = ypnoab
gen L_Ypnoab = L.ypnoab if !missing(L.Ypnoab)
gen Yplgrs_dv = yplgrs_dv
gen Ypnbihs_dv = ypnbihs_dv
gen Ypnbihs_dv_sq = ypnbihs_dv^2 if !missing(ypnbihs_dv)
gen Ynbcpdf_dv = ynbcpdf_dv
gen Yptciihs_dv = yptciihs_dv


/*--------------------------------------------------*/
/* Income receipt indicators                        */
/*--------------------------------------------------*/

gen receives_ypncp = (ypncp > 0) if !missing(ypncp)
gen receives_ypnoab = (ypnoab > 0) if !missing(ypnoab)


/*--------------------------------------------------*/
/* Household income quintiles                       */
/*--------------------------------------------------*/

gen Ydses_c5_Q1 = (ydses_c5 == 1) if !missing(ydses_c5)
gen Ydses_c5_Q2 = (ydses_c5 == 2) if !missing(ydses_c5)
gen Ydses_c5_Q3 = (ydses_c5 == 3) if !missing(ydses_c5)
gen Ydses_c5_Q4 = (ydses_c5 == 4) if !missing(ydses_c5)
gen Ydses_c5_Q5 = (ydses_c5 == 5) if !missing(ydses_c5)

gen L_Ydses_c5 = L1.ydses_c5

gen L_Ydses_c5_Q1 = (L1.ydses_c5 == 1) if !missing(L1.ydses_c5)
gen L_Ydses_c5_Q2 = (L1.ydses_c5 == 2) if !missing(L1.ydses_c5)
gen L_Ydses_c5_Q3 = (L1.ydses_c5 == 3) if !missing(L1.ydses_c5)
gen L_Ydses_c5_Q4 = (L1.ydses_c5 == 4) if !missing(L1.ydses_c5)
gen L_Ydses_c5_Q5 = (L1.ydses_c5 == 5) if !missing(L1.ydses_c5)

gen yHhQuintilesMonthC5 = ydses_c5

gen yHhQuintilesMonthC5Q1 = (ydses_c5 == 1) if !missing(ydses_c5)
gen yHhQuintilesMonthC5Q2 = (ydses_c5 == 2) if !missing(ydses_c5)
gen yHhQuintilesMonthC5Q3 = (ydses_c5 == 3) if !missing(ydses_c5)
gen yHhQuintilesMonthC5Q4 = (ydses_c5 == 4) if !missing(ydses_c5)
gen yHhQuintilesMonthC5Q5 = (ydses_c5 == 5) if !missing(ydses_c5)

gen yHhQuintilesMonthC5L1 = L1.ydses_c5

gen yHhQuintilesMonthC5Q1L1 = (L1.ydses_c5 == 1) if !missing(L1.ydses_c5)
gen yHhQuintilesMonthC5Q2L1 = (L1.ydses_c5 == 2) if !missing(L1.ydses_c5)
gen yHhQuintilesMonthC5Q3L1 = (L1.ydses_c5 == 3) if !missing(L1.ydses_c5)
gen yHhQuintilesMonthC5Q4L1 = (L1.ydses_c5 == 4) if !missing(L1.ydses_c5)
gen yHhQuintilesMonthC5Q5L1 = (L1.ydses_c5 == 5) if !missing(L1.ydses_c5)


/*--------------------------------------------------*/
/* Continuous income variables                      */
/*--------------------------------------------------*/

gen yNonBenPersGrossMonth = Ypnbihs_dv
gen yNonBenPersGrossMonthL1 = L1.Ypnbihs_dv

gen yPersAndPartnerGrossDiffMonth = Ynbcpdf_dv
gen yPersAndPartnerGrossDiffMonthL1 = L1.Ynbcpdf_dv


/*--------------------------------------------------*/
/* Capital income                                   */
/*--------------------------------------------------*/

sum ypncp, detail

scalar p99 = r(p99)

replace ypncp = . if ypncp >= p99 & !missing(ypncp)
replace Ypncp = . if Ypncp >= p99 & !missing(Ypncp)


gen yCapitalPersMonth = ypncp
gen yCapitalPersMonthL1 = L1.ypncp
gen yCapitalPersMonthL2 = L2.ypncp


/*--------------------------------------------------*/
/* Employment income                                */
/*--------------------------------------------------*/

gen yEmpPersGrossMonth = yplgrs_dv

gen yEmpPersGrossMonthL1 = L1.yplgrs_dv
gen yEmpPersGrossMonthL2 = L2.yplgrs_dv
gen yEmpPersGrossMonthL3 = L3.yplgrs_dv


/*--------------------------------------------------*/
/* Pension income                                   */
/*--------------------------------------------------*/

sum ypnoab, detail

scalar p99 = r(p99)

replace ypnoab = . if ypnoab >= p99 & !missing(ypnoab)
replace Ypnoab = . if Ypnoab >= p99 & !missing(Ypnoab)

gen yPensPersGrossMonth = ypnoab
gen yPensPersGrossMonthL1 = L1.ypnoab
gen yPensPersGrossMonthL2 = L2.ypnoab


/*--------------------------------------------------*/
/* Miscellaneous income                             */
/*--------------------------------------------------*/

gen yMiscPersGrossMonth = yptciihs_dv
gen yMiscPersGrossMonthL1 = L1.yptciihs_dv


/*--------------------------------------------------*/
/* Housing wealth                                   */
/*--------------------------------------------------*/

gen Dhh_owned = dhh_owned

gen wealthPrptyFlag = dhh_owned
gen wealthPrptyFlagL1 = L1.dhh_owned


*==================================================
* Region dummies
*==================================================
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

*refactored 
gen demRgn = drgn1

tab drgn1, gen(demRgn_)

rename demRgn_1  demRgnUKC
rename demRgn_2  demRgnUKD
rename demRgn_3  demRgnUKE
rename demRgn_4  demRgnUKF
rename demRgn_5  demRgnUKG
rename demRgn_6  demRgnUKH
rename demRgn_7  demRgnUKI
rename demRgn_8  demRgnUKJ
rename demRgn_9  demRgnUKK
rename demRgn_10 demRgnUKL
rename demRgn_11 demRgnUKM
rename demRgn_12 demRgnUKN


*==================================================
* Employment dummies
*==================================================

/*--------------------------------------------------
Own labour status C3
--------------------------------------------------*/
gen Les_c3 = les_c3
gen L1les_c3 = l.les_c3

gen Les_c3_Employed = (les_c3 == 1) if !missing(les_c3)
gen Les_c3_Student = (les_c3 == 2) if !missing(les_c3)
gen Les_c3_NotEmployed = (les_c3 == 3) if !missing(les_c3)

gen labStatusC3 = les_c3
gen labStatusC3L1 = L1.les_c3

gen labStatusC3Employed    = (les_c3 == 1) if !missing(les_c3)
gen labStatusC3Student     = (les_c3 == 2) if !missing(les_c3)
gen labStatusC3NotEmployed = (les_c3 == 3) if !missing(les_c3)

gen labStatusC3EmployedL1    = (L1.les_c3 == 1) if !missing(L1.les_c3)
gen labStatusC3StudentL1     = (L1.les_c3 == 2) if !missing(L1.les_c3)
gen labStatusC3NotEmployedL1 = (L1.les_c3 == 3) if !missing(L1.les_c3)


/*--------------------------------------------------
Partner labour status C3
--------------------------------------------------*/

gen Lessp_c3_Employed    = (lessp_c3 == 1) if !missing(lessp_c3)
gen Lessp_c3_Student     = (lessp_c3 == 2) if !missing(lessp_c3)
gen Lessp_c3_NotEmployed = (lessp_c3 == 3) if !missing(lessp_c3)

gen labStatusPartnerC3 = lessp_c3
gen labStatusPartnerC3L1 = L1.lessp_c3

gen labStatusPartnerC3Employed    = (lessp_c3 == 1) if !missing(lessp_c3)
gen labStatusPartnerC3Student     = (lessp_c3 == 2) if !missing(lessp_c3)
gen labStatusPartnerC3NotEmployed = (lessp_c3 == 3) if !missing(lessp_c3)

gen labStatusPartnerC3EmployedL1    = (L1.lessp_c3 == 1) if !missing(L1.lessp_c3)
gen labStatusPartnerC3StudentL1     = (L1.lessp_c3 == 2) if !missing(L1.lessp_c3)
gen labStatusPartnerC3NotEmployedL1 = (L1.lessp_c3 == 3) if !missing(L1.lessp_c3)


/*--------------------------------------------------
Own labour status C4
--------------------------------------------------*/

gen Les_c4 = les_c4
gen Les_c4_Employed = (les_c4 == 1) if !missing(les_c4)
gen Les_c4_Student = (les_c4 == 2) if !missing(les_c4)
gen Les_c4_NotEmployed = (les_c4 == 3) if !missing(les_c4)
gen Les_c4_Retired = (les_c4 == 4) if !missing(les_c4)

/* lagged */
gen L_Les_c4 = L1.les_c4
gen L_Les_c4_Employed = (L1.les_c4 == 1) if !missing(L1.les_c4)
gen L_Les_c4_Student = (L1.les_c4 == 2) if !missing(L1.les_c4)
gen L_Les_c4_NotEmployed = (L1.les_c4 == 3) if !missing(L1.les_c4)
gen L_Les_c4_Retired = (L1.les_c4 == 4) if !missing(L1.les_c4)

gen labStatusC4 = les_c4
gen labStatusC4L1 = L1.les_c4

gen labStatusC4Employed    = (les_c4 == 1) if !missing(les_c4)
gen labStatusC4Student     = (les_c4 == 2) if !missing(les_c4)
gen labStatusC4NotEmployed = (les_c4 == 3) if !missing(les_c4)
gen labStatusC4Retired     = (les_c4 == 4) if !missing(les_c4)

gen labStatusC4EmployedL1    = (L1.les_c4 == 1) if !missing(L1.les_c4)
gen labStatusC4StudentL1     = (L1.les_c4 == 2) if !missing(L1.les_c4)
gen labStatusC4NotEmployedL1 = (L1.les_c4 == 3) if !missing(L1.les_c4)
gen labStatusC4RetiredL1     = (L1.les_c4 == 4) if !missing(L1.les_c4)


/*--------------------------------------------------
Joint labour status
--------------------------------------------------*/
gen Lesdf_c4 = lesdf_c4
gen Lesdf_c4_BothEmployed = (lesdf_c4 == 1) if !missing(lesdf_c4)
gen Lesdf_c4_EmpSpouseNotEmp = (lesdf_c4 == 2) if !missing(lesdf_c4)
gen Lesdf_c4_NotEmpSpouseEmp = (lesdf_c4 == 3) if !missing(lesdf_c4)
gen Lesdf_c4_BothNotEmployed = (lesdf_c4 == 4) if !missing(lesdf_c4)

gen labStatusPartnerAndOwnC4 = lesdf_c4
gen labStatusPartnerAndOwnC4L1 = L1.lesdf_c4

gen labStatusPartnerAndOwnC41 = (lesdf_c4 == 1) if !missing(lesdf_c4)
gen labStatusPartnerAndOwnC42 = (lesdf_c4 == 2) if !missing(lesdf_c4)
gen labStatusPartnerAndOwnC43 = (lesdf_c4 == 3) if !missing(lesdf_c4)
gen labStatusPartnerAndOwnC44 = (lesdf_c4 == 4) if !missing(lesdf_c4)

gen labStatusPartnerAndOwnC41L1 = (L1.lesdf_c4 == 1) if !missing(L1.lesdf_c4)
gen labStatusPartnerAndOwnC42L1 = (L1.lesdf_c4 == 2) if !missing(L1.lesdf_c4)
gen labStatusPartnerAndOwnC43L1 = (L1.lesdf_c4 == 3) if !missing(L1.lesdf_c4)
gen labStatusPartnerAndOwnC44L1 = (L1.lesdf_c4 == 4) if !missing(L1.lesdf_c4)


/*--------------------------------------------------
Interactions with sex
--------------------------------------------------*/

gen labStatusC4Employed_Male    = Dgn * labStatusC4Employed
gen labStatusC4Student_Male     = Dgn * labStatusC4Student
gen labStatusC4NotEmployed_Male = Dgn * labStatusC4NotEmployed
gen labStatusC4Retired_Male     = Dgn * labStatusC4Retired

gen labStatusC4Employed_MaleL1    = L1.labStatusC4Employed_Male
gen labStatusC4Student_MaleL1     = L1.labStatusC4Student_Male
gen labStatusC4NotEmployed_MaleL1 = L1.labStatusC4NotEmployed_Male
gen labStatusC4Retired_MaleL1     = L1.labStatusC4Retired_Male


*==================================================
* Education dummies
*==================================================

/*--------------------------------------------------
Education recoding C3
--------------------------------------------------*/
cap drop deh_c3_recoded
recode deh_c3 (1 = 3) (3 = 1), gen(deh_c3_recoded)

cap lab define deh_c3_recoded   1 "Low" 2 "Medium" 3 "High"
label values deh_c3_recoded deh_c3_recoded


/*--------------------------------------------------
Own education C3
--------------------------------------------------*/

gen eduHighestC3 = deh_c3
gen eduHighestC3L1 = L1.deh_c3

gen eduHighestC3High   = (deh_c3 == 1) if !missing(deh_c3)
gen eduHighestC3Medium = (deh_c3 == 2) if !missing(deh_c3)
gen eduHighestC3Low    = (deh_c3 == 3) if !missing(deh_c3)

gen eduHighestC3HighL1   = (L1.deh_c3 == 1) if !missing(L1.deh_c3)
gen eduHighestC3MediumL1 = (L1.deh_c3 == 2) if !missing(L1.deh_c3)
gen eduHighestC3LowL1    = (L1.deh_c3 == 3) if !missing(L1.deh_c3)


/*--------------------------------------------------
Own education C4
--------------------------------------------------*/
gen Deh_c4_Na = (deh_c4 == 0) if !missing(deh_c4)
gen Deh_c4_High = (deh_c4 == 1) if !missing(deh_c4)
gen Deh_c4_Medium = (deh_c4 == 2) if !missing(deh_c4)
gen Deh_c4_Low = (deh_c4 == 3) if !missing(deh_c4)

gen eduHighestC4 = deh_c4
gen eduHighestC4L1 = L1.deh_c4

gen eduHighestC4Na     = (deh_c4 == 0) if !missing(deh_c4)
gen eduHighestC4High   = (deh_c4 == 1) if !missing(deh_c4)
gen eduHighestC4Medium = (deh_c4 == 2) if !missing(deh_c4)
gen eduHighestC4Low    = (deh_c4 == 3) if !missing(deh_c4)

gen eduHighestC4NaL1     = (L1.deh_c4 == 0) if !missing(L1.deh_c4)
gen eduHighestC4HighL1   = (L1.deh_c4 == 1) if !missing(L1.deh_c4)
gen eduHighestC4MediumL1 = (L1.deh_c4 == 2) if !missing(L1.deh_c4)
gen eduHighestC4LowL1    = (L1.deh_c4 == 3) if !missing(L1.deh_c4)


/*--------------------------------------------------
Partner education C3
--------------------------------------------------*/

gen Dehsp_c3_High = (dehsp_c3 == 1) if !missing(dehsp_c3)
gen Dehsp_c3_Medium = (dehsp_c3 == 2) if !missing(dehsp_c3)
gen Dehsp_c3_Low = (dehsp_c3 == 3) if !missing(dehsp_c3)

gen eduHighestPartnerC3 = dehsp_c3
gen eduHighestPartnerC3L1 = L1.dehsp_c3

gen eduHighestPartnerC3High   = (dehsp_c3 == 1) if !missing(dehsp_c3)
gen eduHighestPartnerC3Medium = (dehsp_c3 == 2) if !missing(dehsp_c3)
gen eduHighestPartnerC3Low    = (dehsp_c3 == 3) if !missing(dehsp_c3)

gen eduHighestPartnerC3HighL1   = (L1.dehsp_c3 == 1) if !missing(L1.dehsp_c3)
gen eduHighestPartnerC3MediumL1 = (L1.dehsp_c3 == 2) if !missing(L1.dehsp_c3)
gen eduHighestPartnerC3LowL1    = (L1.dehsp_c3 == 3) if !missing(L1.dehsp_c3)


/*--------------------------------------------------
Highest parental education
--------------------------------------------------*/
gen Dehmf_c3_High = (dehmf_c3 == 1) if !missing(dehmf_c3)
gen Dehmf_c3_Medium = (dehmf_c3 == 2) if !missing(dehmf_c3)
gen Dehmf_c3_Low = (dehmf_c3 == 3) if !missing(dehmf_c3)

gen L_Dehmf_c3 = L1.dehmf_c3

gen L_Dehmf_c3_High = (L1.dehmf_c3 == 1) if !missing(L1.dehmf_c3)
gen L_Dehmf_c3_Medium = (L1.dehmf_c3 == 2) if !missing(L1.dehmf_c3)
gen L_Dehmf_c3_Low = (L1.dehmf_c3 == 3) if !missing(L1.dehmf_c3)

gen eduHighestParentC3 = dehmf_c3
gen eduHighestParentC3L1 = L1.dehmf_c3

gen eduHighestParentC3High   = (dehmf_c3 == 1) if !missing(dehmf_c3)
gen eduHighestParentC3Medium = (dehmf_c3 == 2) if !missing(dehmf_c3)
gen eduHighestParentC3Low    = (dehmf_c3 == 3) if !missing(dehmf_c3)

gen eduHighestParentC3HighL1   = (L1.dehmf_c3 == 1) if !missing(L1.dehmf_c3)
gen eduHighestParentC3MediumL1 = (L1.dehmf_c3 == 2) if !missing(L1.dehmf_c3)
gen eduHighestParentC3LowL1    = (L1.dehmf_c3 == 3) if !missing(L1.dehmf_c3)



*==================================================
* Health dummies
*==================================================
gen Dhe       = dhe
gen Dhe_pcs   = dhe_pcs
gen Dhe_mcs   = dhe_mcs
gen Dhe_pcssp = dhe_pcssp
gen Dhe_mcssp = dhe_mcssp

/*--------------------------------------------------
Value labels
--------------------------------------------------*/

cap lab define dhe ///
    1 "Poor" ///
    2 "Fair" ///
    3 "Good" ///
    4 "VeryGood" ///
    5 "Excellent", modify

lab values dhe dhe

/*--------------------------------------------------
Own self-rated health
--------------------------------------------------*/
gen Dhe_Poor = (dhe == 1) if !missing(dhe)
gen Dhe_Fair = (dhe == 2) if !missing(dhe)
gen Dhe_Good = (dhe == 3) if !missing(dhe)
gen Dhe_VeryGood = (dhe == 4) if !missing(dhe)
gen Dhe_Excellent = (dhe == 5) if !missing(dhe)

gen healthSelfRated = dhe
gen healthSelfRatedL1 = L1.dhe

gen healthSelfRatedPoor      = (dhe == 1) if !missing(dhe)
gen healthSelfRatedFair      = (dhe == 2) if !missing(dhe)
gen healthSelfRatedGood      = (dhe == 3) if !missing(dhe)
gen healthSelfRatedVeryGood  = (dhe == 4) if !missing(dhe)
gen healthSelfRatedExcellent = (dhe == 5) if !missing(dhe)

gen healthSelfRatedPoorL1      = (L1.dhe == 1) if !missing(L1.dhe)
gen healthSelfRatedFairL1      = (L1.dhe == 2) if !missing(L1.dhe)
gen healthSelfRatedGoodL1      = (L1.dhe == 3) if !missing(L1.dhe)
gen healthSelfRatedVeryGoodL1  = (L1.dhe == 4) if !missing(L1.dhe)
gen healthSelfRatedExcellentL1 = (L1.dhe == 5) if !missing(L1.dhe)

/*--------------------------------------------------
Partner self-rated health
--------------------------------------------------*/

gen Dhesp_Poor = (dhesp == 1) if !missing(dhesp)
gen Dhesp_Fair = (dhesp == 2) if !missing(dhesp)
gen Dhesp_Good = (dhesp == 3) if !missing(dhesp)
gen Dhesp_VeryGood = (dhesp == 4) if !missing(dhesp)
gen Dhesp_Excellent = (dhesp == 5) if !missing(dhesp)

gen healthPartnerSelfRated = dhesp
gen healthPartnerSelfRatedL1 = L1.dhesp

gen healthPartnerSelfRatedPoor      = (dhesp == 1) if !missing(dhesp)
gen healthPartnerSelfRatedFair      = (dhesp == 2) if !missing(dhesp)
gen healthPartnerSelfRatedGood      = (dhesp == 3) if !missing(dhesp)
gen healthPartnerSelfRatedVeryGood  = (dhesp == 4) if !missing(dhesp)
gen healthPartnerSelfRatedExcellent = (dhesp == 5) if !missing(dhesp)
/*
gen healthPartnerSelfRatedPoorL1      = (L1.dhesp == 1) if !missing(L1.dhesp)
gen healthPartnerSelfRatedFairL1      = (L1.dhesp == 2) if !missing(L1.dhesp)
gen healthPartnerSelfRatedGoodL1      = (L1.dhesp == 3) if !missing(L1.dhesp)
gen healthPartnerSelfRatedVeryGoodL1  = (L1.dhesp == 4) if !missing(L1.dhesp)
gen healthPartnerSelfRatedExcelL1 = (L1.dhesp == 5) if !missing(L1.dhesp)
*/

/*--------------------------------------------------
Continuous health measures
--------------------------------------------------*/
foreach v in dhe dhe_pcs dhe_mcs {
    gen l_`v' = L.`v'
}

gen L_Dhe      = l_dhe
gen L_Dhe_pcs  = l_dhe_pcs
gen L_Dhe_mcs  = l_dhe_mcs


gen healthPhysicalPcs = dhe_pcs
gen healthMentalMcs   = dhe_mcs

gen healthPhysicalPcsL1 = L1.dhe_pcs
gen healthMentalMcsL1   = L1.dhe_mcs

gen healthPhysicalPartnerPcs = dhe_pcssp
gen healthMentalPartnerMcs   = dhe_mcssp

gen healthPhysicalPartnerPcsL1 = L1.dhe_pcssp
gen healthMentalPartnerMcsL1   = L1.dhe_mcssp


*==================================================
* Long-term sick or disabled
*==================================================

gen Dlltsd   = dlltsd
gen Dlltsd01 = dlltsd01

gen L_Dlltsd01 = l.dlltsd01

gen Dlltsdsp   = dlltsd_sp
gen Dlltsd01sp = dlltsd01_sp
					
gen healthDsblLongtermFlag = dlltsd01
gen healthDsblLongtermFlagL1 = l.dlltsd01

*==================================================
* Ethnicity
*==================================================

tab dot, gen(dot_)
rename dot_1 Ethn_White
rename dot_2 Ethn_Asian
rename dot_3 Ethn_Black
rename dot_4 Ethn_Other

gen demEthnC4 = dot 
tab demEthnC4, gen(demEthnC4_)

rename demEthnC4_1 demEthnC4White
rename demEthnC4_2 demEthnC4Asian
rename demEthnC4_3 demEthnC4Black
rename demEthnC4_4 demEthnC4Other
	
	
*==================================================
* Household type and relationship dynamics
*==================================================

/*--------------------------------------------------*/
/* Raw variables                                    */
/*--------------------------------------------------*/

gen New_rel = new_rel

gen Dnc = dnc
gen Dnc02 = dnc02

gen Dcpyy = dcpyy
gen Dcpagdf = dcpagdf

gen fertilityRate = dukfr
gen FertilityRate = dukfr

/*--------------------------------------------------*/
/* Household type C4                                */
/*--------------------------------------------------*/

gen Dhhtp_c4_CoupleNoChildren = (dhhtp_c4 == 1) if !missing(dhhtp_c4)
gen Dhhtp_c4_CoupleChildren = (dhhtp_c4 == 2) if !missing(dhhtp_c4)
gen Dhhtp_c4_SingleNoChildren = (dhhtp_c4 == 3) if !missing(dhhtp_c4)
gen Dhhtp_c4_SingleChildren = (dhhtp_c4 == 4) if !missing(dhhtp_c4)

gen L_Dhhtp_c4 = L1.dhhtp_c4

gen L_Dhhtp_c4_CoupleNoChildren = (L1.dhhtp_c4 == 1) if !missing(L1.dhhtp_c4)
gen L_Dhhtp_c4_CoupleChildren = (L1.dhhtp_c4 == 2) if !missing(L1.dhhtp_c4)
gen L_Dhhtp_c4_SingleNoChildren = (L1.dhhtp_c4 == 3) if !missing(L1.dhhtp_c4)
gen L_Dhhtp_c4_SingleChildren = (L1.dhhtp_c4 == 4) if !missing(L1.dhhtp_c4)

gen demCompHhC4 = dhhtp_c4

gen demCompHhC4L1 = L1.dhhtp_c4

gen demCompHhC4CoupleNoChL1 = (L1.dhhtp_c4 == 1) if !missing(L1.dhhtp_c4)
gen demCompHhC4CoupleChL1 = (L1.dhhtp_c4 == 2) if !missing(L1.dhhtp_c4)
gen demCompHhC4SingleNoChL1 = (L1.dhhtp_c4 == 3) if !missing(L1.dhhtp_c4)
gen demCompHhC4SingleChL1 = (L1.dhhtp_c4 == 4) if !missing(L1.dhhtp_c4)

gen householdTypeC4 = dhhtp_c4
gen householdTypeC4L1 = L1.dhhtp_c4

gen householdTypeC4CoupleNoCh = (dhhtp_c4 == 1) if !missing(dhhtp_c4)
gen householdTypeC4CoupleCh = (dhhtp_c4 == 2) if !missing(dhhtp_c4)
gen householdTypeC4SingleNoC = (dhhtp_c4 == 3) if !missing(dhhtp_c4)
gen householdTypeC4SingleCh = (dhhtp_c4 == 4) if !missing(dhhtp_c4)

gen householdTypeC4CoupleNoChL1 = (L1.dhhtp_c4 == 1) if !missing(L1.dhhtp_c4)
gen householdTypeC4CoupleChL1 = (L1.dhhtp_c4 == 2) if !missing(L1.dhhtp_c4)
gen householdTypeC4SingleNoChL1 = (L1.dhhtp_c4 == 3) if !missing(L1.dhhtp_c4)
gen householdTypeC4SingleCh1 = (L1.dhhtp_c4 == 4) if !missing(L1.dhhtp_c4)


/*--------------------------------------------------*/
/* Household type C8                                */
/*--------------------------------------------------*/

gen Dhhtp_c8_1 = (dhhtp_c8 == 1) if !missing(dhhtp_c8)
gen Dhhtp_c8_2 = (dhhtp_c8 == 2) if !missing(dhhtp_c8)
gen Dhhtp_c8_3 = (dhhtp_c8 == 3) if !missing(dhhtp_c8)
gen Dhhtp_c8_4 = (dhhtp_c8 == 4) if !missing(dhhtp_c8)
gen Dhhtp_c8_5 = (dhhtp_c8 == 5) if !missing(dhhtp_c8)
gen Dhhtp_c8_6 = (dhhtp_c8 == 6) if !missing(dhhtp_c8)
gen Dhhtp_c8_7 = (dhhtp_c8 == 7) if !missing(dhhtp_c8)
gen Dhhtp_c8_8 = (dhhtp_c8 == 8) if !missing(dhhtp_c8)

gen householdTypeC8 = dhhtp_c8

gen demCompHhC8 = dhhtp_c8
gen demCompHhC8L1 = L1.dhhtp_c8

gen demCompHhC81 = (dhhtp_c8 == 1) if !missing(dhhtp_c8)
gen demCompHhC82 = (dhhtp_c8 == 2) if !missing(dhhtp_c8)
gen demCompHhC83 = (dhhtp_c8 == 3) if !missing(dhhtp_c8)
gen demCompHhC84 = (dhhtp_c8 == 4) if !missing(dhhtp_c8)
gen demCompHhC85 = (dhhtp_c8 == 5) if !missing(dhhtp_c8)
gen demCompHhC86 = (dhhtp_c8 == 6) if !missing(dhhtp_c8)
gen demCompHhC87 = (dhhtp_c8 == 7) if !missing(dhhtp_c8)
gen demCompHhC88 = (dhhtp_c8 == 8) if !missing(dhhtp_c8)


gen demCompHhC81L1 = (L1.dhhtp_c8 == 1) if !missing(L1.dhhtp_c8)
gen demCompHhC82L1 = (L1.dhhtp_c8 == 2) if !missing(L1.dhhtp_c8)
gen demCompHhC83L1 = (L1.dhhtp_c8 == 3) if !missing(L1.dhhtp_c8)
gen demCompHhC84L1 = (L1.dhhtp_c8 == 4) if !missing(L1.dhhtp_c8)
gen demCompHhC85L1 = (L1.dhhtp_c8 == 5) if !missing(L1.dhhtp_c8)
gen demCompHhC86L1 = (L1.dhhtp_c8 == 6) if !missing(L1.dhhtp_c8)
gen demCompHhC87L1 = (L1.dhhtp_c8 == 7) if !missing(L1.dhhtp_c8)
gen demCompHhC88L1 = (L1.dhhtp_c8 == 8) if !missing(L1.dhhtp_c8)


/*--------------------------------------------------*/
/* Relationship status                              */
/*--------------------------------------------------*/
gen mar = (dcpst == 1)

gen Dcpst = dcpst

gen Partnered = (dcpst == 1) if !missing(dcpst)
gen Single = (dcpst == 2) if !missing(dcpst)

gen Dcpst_Partnered = (dcpst == 1) if !missing(dcpst)
gen Dcpst_Single = (dcpst == 2) if !missing(dcpst)

gen demPartnerStatus = dcpst
gen demPartnerStatusL1 = L1.dcpst

gen demPartnerStatusPartnered = (dcpst == 1) if !missing(dcpst)
gen demPartnerStatusSingle = (dcpst == 2) if !missing(dcpst)

gen demPartnerStatusPartneredL1 = (L1.dcpst == 1) if !missing(L1.dcpst)
gen demPartnerStatusSingleL1 = (L1.dcpst == 2) if !missing(L1.dcpst)


/*--------------------------------------------------*/
/* Relationship dynamics                            */
/*--------------------------------------------------*/

gen demPartnerNYear = dcpyy
gen demPartnerNYearL1 = L1.dcpyy

gen demEnterPartnerFlag = new_rel
gen demEnterPartnerFlagL1 = L1.new_rel

gen demAgePartnerDiff = dcpagdf
gen demAgePartnerDiffL1 = L1.dcpagdf


/*--------------------------------------------------*/
/* Children                                         */
/*--------------------------------------------------*/
cap drop child 
gen child = (dnc>0)

gen demNChild = dnc
gen demNChild0to2 = dnc02

gen demNChildL1 = L1.dnc
gen demNChild0to2L1 = L1.dnc02

gen demDChild = (dnc > 0) if !missing(dnc)
gen demDChild0to2 = (dnc02 > 0) if !missing(dnc02)

gen demDChildL1 = (L1.dnc > 0) if !missing(L1.dnc)
gen demDChild0to2L1 = (L1.dnc02 > 0) if !missing(L1.dnc02)



/*==================================================*/
/* INTERACTIONS          */
/*==================================================*/

/*--------------------------------------------------*/
/* Education × demographics                         */
/*--------------------------------------------------*/

gen Ded_Dag = Ded * Dag if !missing(Ded, Dag)

gen Ded_Dag_sq = Ded * Dag_sq if !missing(Ded, Dag_sq)

gen Ded_Dgn = Ded * Dgn if !missing(Ded, Dgn)

gen eduSampleFlag_demMaleFlag = eduSampleFlag * demMaleFlag if !missing(eduSampleFlag, demMaleFlag)

gen eduSampleFlag_Male = eduSampleFlag * demMaleFlag if !missing(eduSampleFlag, demMaleFlag)


/*--------------------------------------------------*/
/* Education × children                             */
/*--------------------------------------------------*/

gen Ded_Dnc_L1_ = Ded * L1.Dnc if !missing(Ded, L1.Dnc)

gen Ded_Dnc02_L1 = Ded * L1.Dnc02 if !missing(Ded, L1.Dnc02)

gen eduSampleFlag_demNChildL1 = eduSampleFlag * demNChildL1 if !missing(eduSampleFlag, demNChildL1)

gen eduSampleFlag_demNChild0to2L1 = eduSampleFlag * demNChild0to2L1 if !missing(eduSampleFlag, demNChild0to2L1)


/*--------------------------------------------------*/
/* Education × partnership status                   */
/*--------------------------------------------------*/

gen Ded_Dcpst_Single = Dcpst_Single * Ded if !missing(Dcpst_Single, Ded)

gen Ded_Dcpst_Single_L1 = L1.Dcpst_Single * Ded if !missing(L1.Dcpst_Single, Ded)

gen eduSampleFlag_Single = eduSampleFlag * demPartnerStatusSingle if !missing(eduSampleFlag, demPartnerStatusSingle)


/*--------------------------------------------------*/
/* Education × income quintiles                     */
/*--------------------------------------------------*/

forvalues i = 1/5 {
    gen Ded_Ydses_c5_Q`i'_L1 = Ded * L1.Ydses_c5_Q`i' if !missing(Ded, L1.Ydses_c5_Q`i')
}

gen eduSampleFlag_Q2L1 = eduSampleFlag * yHhQuintilesMonthC5Q2L1 if !missing(eduSampleFlag, yHhQuintilesMonthC5Q2L1)

gen eduSampleFlag_Q3L1 = eduSampleFlag * yHhQuintilesMonthC5Q3L1 if !missing(eduSampleFlag, yHhQuintilesMonthC5Q3L1)

gen eduSampleFlag_Q4L1 = eduSampleFlag * yHhQuintilesMonthC5Q4L1 if !missing(eduSampleFlag, yHhQuintilesMonthC5Q4L1)

gen eduSampleFlag_Q5L1 = eduSampleFlag * yHhQuintilesMonthC5Q5L1 if !missing(eduSampleFlag, yHhQuintilesMonthC5Q5L1)


/*--------------------------------------------------*/
/* Education × spouse education                     */
/*--------------------------------------------------*/

gen Ded_Dehsp_c3_Medium_L1 = L1.Dehsp_c3_Medium * Ded if !missing(L1.Dehsp_c3_Medium, Ded)

gen Ded_Dehsp_c3_Low_L1 = L1.Dehsp_c3_Low * Ded if !missing(L1.Dehsp_c3_Low, Ded)


/*--------------------------------------------------*/
/* Education × health                               */
/*--------------------------------------------------*/

gen Ded_Dhesp_Good_L1 = L1.Dhesp_Good * Ded if !missing(L1.Dhesp_Good, Ded)

gen Ded_Dhesp_Fair_L1 = L1.Dhesp_Fair * Ded if !missing(L1.Dhesp_Fair, Ded)

gen Ded_Dhe_Fair_L1 = L1.Dhe_Fair * Ded if !missing(L1.Dhe_Fair, Ded)

gen Ded_Dhe_Good_L1 = L1.Dhe_Good * Ded if !missing(L1.Dhe_Good, Ded)

gen Ded_Dhe_VeryGood_L1 = L1.Dhe_VeryGood * Ded if !missing(L1.Dhe_VeryGood, Ded)

gen Ded_Dhe_Excellent_L1 = L1.Dhe_Excellent * Ded if !missing(L1.Dhe_Excellent, Ded)

gen Ded_Dhe_pcs = Ded * Dhe_pcs if !missing(Ded, Dhe_pcs)

gen Ded_Dhe_mcs = Ded * Dhe_mcs if !missing(Ded, Dhe_mcs)

gen Ded_Dhe = Dhe * Ded if !missing(Dhe, Ded)


/*--------------------------------------------------*/
/* Retirement interactions                          */
/*--------------------------------------------------*/

gen Reached_Retirement_Age_Les = Reached_Retirement_Age * L1.Les_c3_NotEmployed if !missing(Reached_Retirement_Age, L1.Les_c3_NotEmployed)

gen demPensAgeFlag_NotEmployedL1 = demPensAgeFlag * labStatusC3NotEmployedL1 if !missing(demPensAgeFlag, labStatusC3NotEmployedL1)


/*--------------------------------------------------*/
/* Education × income                               */
/*--------------------------------------------------*/

gen Ded_Ypncp = Ded * Ypncp if !missing(Ded, Ypncp)

gen Ded_Yplgrs_dv = Ded * Yplgrs_dv if !missing(Ded, Yplgrs_dv)

gen eduSampleFlag_yCapitalPers = eduSampleFlag * yCapitalPersMonth if !missing(eduSampleFlag, yCapitalPersMonth)

gen eduSampleFlag_yCapitalPersL1 = L1.eduSampleFlag_yCapitalPers

gen eduSampleFlag_yCapitalPersL2 = L2.eduSampleFlag_yCapitalPers

gen eduSampleFlag_yEmpPersGross = eduSampleFlag * yEmpPersGrossMonth if !missing(eduSampleFlag, yEmpPersGrossMonth)

gen eduSampleFlag_yEmpPersGrossL1 = L1.eduSampleFlag_yEmpPersGross

gen eduSampleFlag_yEmpPersGrossL2 = L2.eduSampleFlag_yEmpPersGross


/*--------------------------------------------------*/
/* Education × health scores                        */
/*--------------------------------------------------*/

gen eduSampleFlag_Pcs = eduSampleFlag * healthPhysicalPcs if !missing(eduSampleFlag, healthPhysicalPcs)

gen eduSampleFlag_Mcs = eduSampleFlag * healthMentalMcs if !missing(eduSampleFlag, healthMentalMcs)

gen eduSampleFlag_PcsL1 = L1.eduSampleFlag_Pcs

gen eduSampleFlag_McsL1 = L1.eduSampleFlag_Mcs


/*--------------------------------------------------*/
/* Education × age                                  */
/*--------------------------------------------------*/

gen eduHighestC4Na_demAge = eduHighestC4Na * dag if !missing(eduHighestC4Na, dag)

gen eduHighestC4Low_demAge = eduHighestC4Low * dag if !missing(eduHighestC4Low, dag)

gen eduHighestC4Medium_demAge = eduHighestC4Medium * dag if !missing(eduHighestC4Medium, dag)

gen eduHighestC4High_demAge = eduHighestC4High * dag if !missing(eduHighestC4High, dag)

gen eduHighestC4NaL1_demAge = eduHighestC4NaL1 * demAge if !missing(eduHighestC4NaL1, demAge)

gen eduHighestC4LowL1_demAge = eduHighestC4LowL1 * demAge if !missing(eduHighestC4LowL1, demAge)

gen eduHighestC4MediumL1_demAge = eduHighestC4MediumL1 * demAge if !missing(eduHighestC4MediumL1, demAge)

gen eduHighestC4HighL1_demAge = eduHighestC4HighL1 * demAge if !missing(eduHighestC4HighL1, demAge)


/*--------------------------------------------------*/
/* Part-time work indicator                         */
/*--------------------------------------------------*/

gen pt = (lhw > 0 & lhw <= 25) if !missing(lhw)
gen labPt = (lhw > 0 & lhw <= 25) if !missing(lhw)


*==================================================
* Prepare data for social care regressions 
*==================================================
* Care received variables (Processes S2)
gen need_care = need_socare
//rename need_socare need_care

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
//table dage10prime, c(min dag max dag)
tabstat dag, by(dage10prime) stat(min max)

* - Categorical: 65-66, 67-68, 69-70, 71-72..., 85+
gen dage2old = 0
forval ii = 1/10 {
	replace dage2old = `ii' if (dag >= 65+2*(`ii'-1) & dag < 67+2*(`ii'-1))
}
replace dage2old = 11 if (dag >= 85)
//table dage2old, stat(min dag) stat(max dag)
//table dage2old, c(min dag max dag)
tabstat dag, by(dage2old) stat(min max)

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
//table dage10prime, c(min dag max dag)	
tabstat dag, by(dage10prime) stat(min max)
drop Age_1
rename Age_2 Age35to44
rename Age_3 Age45to54
rename Age_4 Age55to64
rename Age_5 Age65plus

tab dage2old, gen(Age_)
//table dage2old, stat(min dag) stat(max dag)	// RMK: AgeXX categories start at 1, hence shifted by 1
//table dage2old, c(min dag max dag)
tabstat dag, by(dage2old) stat(min max)
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


* refactoring social care 
xtset idperson stm

gen demAge20to24 = Age20to24
gen demAge25to29 = Age25to29
gen demAge30to34 = Age30to34
gen demAge35to39 = Age35to39
gen demAge40to44 = Age40to44
gen demAge45to49 = Age45to49
gen demAge50to54 = Age50to54
gen demAge55to59 = Age55to59
gen demAge60to64 = Age60to64
gen demAge65to69 = Age65to69
gen demAge70to74 = Age70to74
gen demAge75to79 = Age75to79
gen demAge80to84 = Age80to84
cap confirm variable Age85plus
if !_rc gen demAge85plus = Age85plus


gen demAge65to66 = Age65to66
gen demAge67to68 = Age67to68
gen demAge69to70 = Age69to70
gen demAge71to72 = Age71to72
gen demAge73to74 = Age73to74
gen demAge75to76 = Age75to76
gen demAge77to78 = Age77to78
gen demAge79to80 = Age79to80
gen demAge81to82 = Age81to82
gen demAge83to84 = Age83to84

gen careMarket = CareMarket
gen careMarketL1 = l.careMarket


gen careMarketInformal = (careMarket == 2) if !missing(careMarket)
gen careMarketMixed    = (careMarket == 3) if !missing(careMarket)
gen careMarketFormal   = (careMarket == 4) if !missing(careMarket)

gen careMarketInformalL1 = (L1.careMarket == 2) if !missing(L1.careMarket)
gen careMarketMixedL1    = (L1.careMarket == 3) if !missing(L1.careMarket)
gen careMarketFormalL1   = (L1.careMarket == 4) if !missing(L1.careMarket)

gen careHrsInformalIhs = HrsReceivedInformalIHS
gen careHrsInformalIhsL1 = l.HrsReceivedInformalIHS

gen careHrsFormalIhs = HrsReceivedFormalIHS 
gen careHrsFormalIhsL1 = l.HrsReceivedFormalIHS


gen careNeedFlag = NeedCare	
gen careNeedFlagL1 = l.NeedCare

gen careReceivedFlag = ReceiveCare 
gen careReceivedFlagL1 = l.ReceiveCare 

gen careProvidedFlag = ProvideCare	
gen careProvidedFlagL1 = l.ProvideCare

gen careNeedPartnerFlag = NeedCarePartner
gen careReceivedPartnerFlag = ReceiveCarePartner 

gen careMarketInformalPartner = CareMarketInformalPartner
gen careMarketMixedPartner = CareMarketMixedPartner
gen careMarketFormalPartner = CareMarketFormalPartner 

gen careHrsProvidedWeekIhs = HrsProvidedInformalIHS 
gen careHrsProvidedWeekIhsL1 = l.HrsProvidedInformalIHS 




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

*==================================================
* Income change (binary, increased or decreased)
*==================================================

sort idperson swv
xtset idperson swv
gen temp_incchange=econ_realequivinc - L.econ_realequivinc

gen exp_incchange=.
replace exp_incchange=1 if (econ_realequivinc < L.econ_realequivinc) & econ_realequivinc!=. & L.econ_realequivinc!=.
replace exp_incchange=0 if (econ_realequivinc == L.econ_realequivinc) & econ_realequivinc!=. & L.econ_realequivinc!=.
replace exp_incchange=0 if (econ_realequivinc > L.econ_realequivinc) & econ_realequivinc!=. & L.econ_realequivinc!=.

label define incchangecat 1 "Decreased income" 0 "Increased or stable income"
label values exp_incchange incchangecat
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
gen exp_poverty= .
replace exp_poverty=0 if temp_HHinPoverty==0 & L.temp_HHinPoverty==0
replace exp_poverty=1 if temp_HHinPoverty==1 & L.temp_HHinPoverty==0
replace exp_poverty=2 if temp_HHinPoverty==0 & L.temp_HHinPoverty==1
replace exp_poverty=3 if temp_HHinPoverty==1 & L.temp_HHinPoverty==1
label define poverty_trans 0 "No Poverty" 1 "Entering poverty" 2 "Exiting poverty" 3 "Continuous poverty"
label values exp_poverty poverty_trans
label var exp_poverty "Poverty transition"
tab exp_poverty swv, m column
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
* Starting state: employed or self-employed
replace exp_emp=11 if L.les_c4==1 & les_c4==1
replace exp_emp=13 if L.les_c4==1 & les_c4==3

* Starting state: not employed
replace exp_emp=31 if L.les_c4==3 & les_c4==1
replace exp_emp=33 if L.les_c4==3 & les_c4==3
label define exp_emp 11 "Continuous employment" 13 "Exiting employment" 31 "Entering employment" 33 "Continuously non-employed"
label value exp_emp exp_emp
tab exp_emp swv, col miss

*==================================================
* Working hour categories
*==================================================

gen lhw_c5=.
replace lhw_c5=0 if (lhw<=5)
replace lhw_c5=10 if (lhw>=6 & lhw<=15)
replace lhw_c5=20 if (lhw>=16 & lhw<=25)
replace lhw_c5=30 if (lhw>=26 & lhw<=35)
replace lhw_c5=40 if (lhw>=36 & lhw!=.)

label define lhwsp 0 "Zero" 10 "Ten" 20 "Twenty" 30 "Thirty" 40 "Forty"
label value lhw_c5 lhwsp
la var lhw_c5 "Hours worked per week (category)"


*==================================================
* Health and wellbeing variables
*==================================================

gen Dhh_owned_L1= L1.Dhh_owned if !missing(L1.Dhh_owned)
gen Dcpst_Single_L1 = L1.Dcpst_Single if !missing(L1.Dcpst_Single)
gen Dnc_L1 = L1.Dnc if !missing(L1.Dnc)

gen Dhm = dhm
gen Dhm_L1 = L1.Dhm if !missing(L1.Dhm)

gen Dhmghq = dhm_ghq
gen Dhmghq_L1 = L1.Dhmghq if !missing(L1.Dhmghq)

gen Dls = dls
gen Dls_L1 = L1.Dls if !missing(L1.Dls)

gen Dag_L1 = L1.Dag if !missing(L1.Dag)
gen Dag_sq_L1 = L1.Dag_sq if !missing(L1.Dag_sq)


gen EmployedToUnemployed = 0
gen UnemployedToEmployed = 0
gen PersistentUnemployed = 0
gen PersistentEmployed = 0

replace EmployedToUnemployed = 1 if L.les_c4 == 1 & les_c4 == 3
replace UnemployedToEmployed = 1 if L.les_c4== 3 & les_c4 == 1
replace PersistentUnemployed = 1 if L.les_c4== 3 & les_c4 == 3
replace PersistentEmployed = 1 if L.les_c4== 1 & les_c4 == 1

replace EmployedToUnemployed = . if missing(exp_emp)
replace UnemployedToEmployed = . if missing(exp_emp)
replace PersistentUnemployed = . if missing(exp_emp)
replace PersistentEmployed = . if missing(exp_emp)

gen NonPovertyToPoverty = exp_poverty == 1
gen PovertyToNonPoverty = exp_poverty == 2
gen PersistentPoverty = exp_poverty == 3
gen NoPoverty = exp_poverty == 0

replace PovertyToNonPoverty = . if missing(exp_poverty)
replace NonPovertyToPoverty = . if missing(exp_poverty)
replace NoPoverty = . if missing(exp_poverty)
replace PersistentPoverty = . if missing(exp_poverty)

gen RealIncomeChange = exp_incchange == 1
gen RealIncomeDecrease_D = D.log_income

gen FinancialDistress = financial_distress == 1
gen L_FinancialDistress = L.financial_distress == 1 if !missing(L.financial_distress)
gen D_Econ_benefits_NonUC = econ_benefits_nonuc == 1
gen D_Econ_benefits_UC = econ_benefits_uc == 1
gen D_Econ_benefits = econ_benefits_nonuc == 1 | econ_benefits_uc == 1

gen Lhw_c5 = lhw_c5
gen D_Econ_benefits_UC_Lhw_ZERO = D_Econ_benefits_UC == 1 & Lhw_c5 == 0
gen D_Econ_benefits_UC_Lhw_TEN = D_Econ_benefits_UC == 1 & Lhw_c5 == 10
gen D_Econ_benefits_UC_Lhw_TWENTY = D_Econ_benefits_UC == 1 & Lhw_c5 == 20
gen D_Econ_benefits_UC_Lhw_THIRTY = D_Econ_benefits_UC == 1 & Lhw_c5 == 30
gen D_Econ_benefits_UC_Lhw_FORTY = D_Econ_benefits_UC == 1 & Lhw_c5 == 40


gen Lhw_10 = Lhw_c5 == 10
gen Lhw_20 = Lhw_c5 == 20
gen Lhw_30 = Lhw_c5 == 30
gen Lhw_40 = Lhw_c5 == 40

gen Deh_c3_High = (deh_c3 == 1) if !missing(deh_c3)
gen Deh_c3_Medium = (deh_c3 == 2) if !missing(deh_c3)
gen Deh_c3_Low = (deh_c3 == 3) if !missing(deh_c3)

*==================================================
* End  
*==================================================

