*This file creates JAS-mine microsimulation input population from 
*UKHLS data available from the UK Data Archive
********************************************************************************

/*Variables and descriptions:
idhh: household id [hidp] 
idperson: person id [pidp] 
idfather: father id [fnspid] 
idmother: mother id [mnspid] 
idpartner: partner id [ppid] [sppid to identify spouses only also available] 
drgn1: region [gor_dv] 
dwt: weight [hhdenub_xw]
dct: country [generated and set to 15]
dgn: gender [sex_dv] 
dag: age [age_dv] 
dhe: health status self-reported [scsf1] 
deh_c3: education status (3-levels) [hiqual_dv] 
dehm_c3: education mother [maedqf] 
dehf_c3: education father [paedqf] 
dun: union? 1 if has spouse, 0 otherwise? [generate from idpartner] => remove? 
les_c3: activity status [jbstat] 
lcs: civil servant [jbsic07_cc] => remove?
lhw: hours of work [jbhrs+jbot+jshrs] => ? not in Cara's file, do we need it or not?

dnc02: number of children aged 0-2 in a household [nch02_dv] 
dhesp: partner's health status [scsf1]

Use -9 for missing values 

/**********************Rules and assumptions*********************************/
1. Each HH can contain: Responsible Male, and/or Responsible Female, Children, Other members.
In the simulation everyone start in other members and is assigned one of the roles in the HH.

	1.1. Responsible male and female create a partnership couple leading the HH. Any additional couple 
		 creates new HH. A couple with / composed of people under the age to leave home (18)
		 will still leave together and set up a new HH. 
		 
		 1.1.1. Children should follow the mother if she's moving to a new HH. 
		 
	1.2. After the above there should be only singles left in addition to the leading couple.
		 If they are above 18, they will leave and set up their own HH. 
	1.3. After the above there should only be children left in addition to the original HH.
		 Children will live with mother if defined in the data, otherwise with father. If neither
		 exists, it will be an orphan. 
	1.4. Orphans are assigned female or male from the household in which they live as a parent. 

*/
clear
set more off

*Define paths:

global folder "C:\Users\Patryk\Dropbox\EUROMODFiles\JAS-mine\LABSim\UKHLS"
global input_data "C:\Users\Patryk\Dropbox\EUROMODFiles\JAS-mine\LABSim\UKHLS\UKDA-6614-stata\stata\stata11_se"
global output "C:\Users\Patryk\Dropbox\EUROMODFiles\JAS-mine\LABSim\UKHLS\output"

global wp i_ //letter for Wave 9
global wvno 9
global year 2018

*Define a macro for age at which individuals can move out (for household splitting)
global age_become_responsible 18

*Merge in partner's id from previous waves
tempfile tmp_partnershipDurationW9
cd $input_data\ukhls_w9
use ${wp}indall, clear
merge 1:1 pidp using $input_data\ukhls_w8\h_indall, keepusing(h_ppid) keep(1 3) nogen
merge 1:1 pidp using $input_data\ukhls_w7\g_indall, keepusing(g_ppid) keep(1 3) nogen
merge 1:1 pidp using $input_data\ukhls_w6\f_indall, keepusing(f_ppid) keep(1 3) nogen 
merge 1:1 pidp using $input_data\ukhls_w5\e_indall, keepusing(e_ppid) keep(1 3) nogen 
merge 1:1 pidp using $input_data\ukhls_w4\d_indall, keepusing(d_ppid) keep(1 3) nogen
merge 1:1 pidp using $input_data\ukhls_w3\c_indall, keepusing(c_ppid) keep(1 3) nogen
merge 1:1 pidp using $input_data\ukhls_w2\b_indall, keepusing(b_ppid) keep(1 3) nogen
merge 1:1 pidp using $input_data\ukhls_w1\a_indall, keepusing(a_ppid) keep(1 3) nogen    

rename a_ppid ppid1
rename b_ppid ppid2
rename c_ppid ppid3
rename d_ppid ppid4
rename e_ppid ppid5
rename f_ppid ppid6
rename g_ppid ppid7
rename h_ppid ppid8
rename i_ppid ppid9

keep pidp ppid*
reshape long ppid, i(pidp) j(waveno)
replace ppid = . if ppid < 0

merge m:1 pidp using $input_data\ukhls_w2\b_indresp, keepusing(b_lmcby41 b_intdaty_dv) keep(1 3) nogen  
replace b_lmcby41 = . if b_lmcby41 < 0
xtset pidp waveno //Set panel
tsspell ppid //Count spells
rename _seq partnershipDuration
replace partnershipDuration = . if ppid == .
keep if waveno == 9
keep pidp partnershipDuration 
save tmp_partnershipDurationW9, replace

*Data cleaning: merge indall, indresp, hhsamp, hhresp
cd $input_data\ukhls_w9
*merge indall and indresp
use ${wp}indall, clear
merge 1:1 pidp using ${wp}indresp 
drop _merge

*merge hhsamp
merge m:1 ${wp}hidp using ${wp}hhsamp, ///
keepusing(${wp}gor_dv) 
keep if _merge==3
drop _merge  

*merge hhresp
merge m:1 ${wp}hidp using ${wp}hhresp 
keep if _merge==3
drop _merge

*merge youth?
merge 1:1 pidp using ${wp}youth
keep if _merge == 1 | _merge == 3
drop _merge


*Prepare and merge income variables:
preserve
tempfile tmp_income
use ${wp}income, clear
gen ${wp}inc_stp = ${wp}frmnthimp_dv if ${wp}ficode == 1

gen ${wp}inc_tu = ${wp}frmnthimp_dv if ${wp}ficode == 25

gen ${wp}inc_ma = ${wp}frmnthimp_dv if ${wp}ficode == 26

keep pidp ${wp}hidp ${wp}inc_stp ${wp}inc_tu ${wp}inc_ma

drop if missing(${wp}inc_stp) & missing(${wp}inc_tu) & missing(${wp}inc_ma)
collapse (sum) ${wp}inc_stp ${wp}inc_tu ${wp}inc_ma, by( pidp ${wp}hidp)
save tmp_income, replace
restore


merge m:1 pidp ${wp}hidp using tmp_income
keep if _merge == 1 | _merge == 3
drop _merge

renpfix ${wp}



*merge indresp from wave 8 for hiqual_dv variable to fill missings in education later:
cd $input_data\ukhls_w8
*Merge with W8 indresp, keep only W9 and matched observations, keep hiqual_dv and dvage variable
merge 1:1 pidp using h_indresp, keepusing(h_hiqual_dv h_age_dv h_jbstat)
keep if _merge == 1 | _merge == 3
drop _merge

cd $input_data\ukhls_w8
*Merge with W8 indall, keep only W9 and matched observations, keep mastat_dv
merge 1:1 pidp using h_indall, keepusing(h_mastat_dv)
keep if _merge == 1 | _merge == 3
drop _merge

*merge previous waves for maternal and paternal education:
cd $input_data\ukhls_wx
merge 1:1 pidp using xwavedat, keepusing(maedqf paedqf)
keep if _merge == 1 | _merge == 3
drop _merge


***Drop BHPS and IEMB: [do we want to drop these?]
*drop if pid != -8
*drop if hhorig == 8

***Keep only full-interview, proxy interview, youth interview, child interview?
keep if ivfio == 1 | ivfio == 2 | ivfio == 21 | ivfio == 24 


***Prepare variables necessary to split households***

/**************************** HOUSEHOLD IDENTIFIER*****************************/
clonevar idhh= hidp 

la var idhh "Household identifier"

/********************************* INDIVIDUALS ID******************************/ 
clonevar idperson=pidp 
la var idperson "Unique cross wave identifier"

/********************************** gender*************************************/
gen dgn=sex_dv
la var dgn "Gender" 
recode dgn 2=0 	//dgn = 0 is female, 1 is male


/***************************************ID PARTNER*****************************/ 
clonevar idpartner=ppid
la var idpartner "Unique cross wave identifier of partner"

/********************************Replace idpartner with ppno if possible*******/
*ID partner is sometimes missing but ppno available - fill in? 
preserve
keep idhh ppno idperson dgn
rename idperson idperson_partner
rename dgn dgn_partner
rename ppno pno 
drop if pno == 0
save $folder/temp_id, replace
restore

merge 1:1 idhh pno using $folder/temp_id
keep if _merge == 1 | _merge == 3
drop _merge
replace idpartner = idperson_partner if idpartner == -9 & ppno != 0 & !missing(idperson_partner) & dgn != dgn_partner

/********************ID father (includes natural/step/adoptive)****************/

clonevar idfather= fnspid
la var idfather "Father unique identifier"


/************************ID MOTHER (includes natural/step/adoptive)************/
clonevar idmother=mnspid 
la var idmother "Mother unique identifier"

/************************ AGE *************************************************/ 
gen dag= age_dv
replace h_age_dv = h_age_dv + 1 if !missing(h_age_dv)
replace dag = h_age_dv if dag == -9 & !missing(h_age_dv) //Don't think we can improve further on missing age
la var dag "Age "


/*******************************Flag for adult children************************/


preserve
keep if dgn == 0
keep idhh idperson dag
rename idperson idmother
rename dag dagmother
save $folder/temp_mother_dag, replace
restore, preserve
keep if dgn == 1
keep idhh idperson dag
rename idperson idfather
rename dag dagfather
save $folder/temp_father_dag, replace 
restore

merge m:1 idhh idmother using $folder/temp_mother_dag
keep if _merge == 1 | _merge == 3
drop _merge
merge m:1 idhh idfather using $folder/temp_father_dag
keep if _merge == 1 | _merge == 3
drop _merge

//Adult child is identified on the successful merge with mother / father in the same household and age
gen adultChildFlag = (!missing(dagmother) | !missing(dagfather)) & dag >= $age_become_responsible & idpartner <= 0
*Introduce a condition that (adult) children cannot be older than parents-15 year of age
replace adultChildFlag = 0 if dag >= dagfather-15 | dag >= dagmother-15 


********************************************************************************
*Export dataset used to estimate parametric union matching					   *
********************************************************************************
preserve

merge 1:1 pidp using $input_data\ukhls_w8\h_indall, keepusing(h_ppid) keep(1 3) nogen

gen newMarriage = (idpartner > 0 & (h_ppid < 0 | h_ppid == .))
*Note: individuals whose dcpyy (number of years in a partnership) equals 1, are newly married

save $folder/parametricUnionDataset, replace 
restore

/******************************Split households********************************/
**What about weights?
*How to generate new hh numbers? Slightly different IDs than in the labsim guide assigned to track where hh came from


*Create unique partnership identifier within each household
gen apartnum = cond(pno<ppno, pno, ppno) if ppno>0

*by idhh, new file with mother id father id and apartnum. Then assign that apartnum to child. 
preserve
keep idhh idperson apartnum
rename idperson idmother
rename apartnum apartnumm
gen idhhmother = idhh
save $folder/temp_mother, replace
rename idmother idfather
rename apartnum apartnumf
gen idhhfather = idhh
save $folder/temp_father, replace 
restore

merge m:1 idhh idmother using $folder/temp_mother
keep if _merge == 1 | _merge == 3
drop _merge
merge m:1 idhh idfather using $folder/temp_father
keep if _merge == 1 | _merge == 3
drop _merge

*Keep children under age to become responsible with parents unless their partner lives in the hh: (children above age to become responsible will create independent households)
replace apartnum = apartnumm if missing(apartnum) & dag < $age_become_responsible & ppno == 0 //ppno == 0 ensures there is no partner living with them
replace apartnum = apartnumf if missing(apartnum) & dag < $age_become_responsible & ppno == 0
drop apartnumm apartnumf

*Corresponds to point 1 of section 3.1.4 of labsim guide
*Give new HH numbers where there is more than 1 couple in the HH:
egen newid = group(idhh apartnum)
tostring(newid), replace
replace newid = "999999"+newid if newid != "."
destring(newid), replace
replace idhh = newid if apartnum > 1 & !missing(apartnum)

*Yes, but a single can have a child - it should go with them. 
*Correspondes to point 2 of section 3.1.4 of labsim guide
*If aged above the age to become responsible, and pno > 1 (so more than 2 person in the HH) & ppno == 0 (partner not in the HH) should move out:

cap drop newid
egen newid = group(idhh pno) if dag >= $age_become_responsible & pno > 1 & ppno == 0
tostring(newid), replace
replace newid = "888888"+newid if newid != "."
destring(newid), replace
replace idhh = newid if !missing(newid)
drop newid

/*
*Still some households with 3 adults?
bys idhh: egen adult_count = count(idperson) if dag > $age_become_responsible
egen newid = group(idhh pno) if adult_count > 2 & ppno == 0 & dag > $age_become_responsible
tostring(newid), replace
replace newid = "777777"+newid if newid != "."
destring(newid), replace
replace idhh = newid if !missing(newid)
drop newid adult_count
*/

/*********************************Orphans**************************************/

*Some checks:
bys idhh: egen adult_count = count(idperson) if dag > $age_become_responsible
tab adult_count

*Correspondes to point 3/4 of section 3.1.4 of labsim guide
*Check for orphans: 
gen orphan_dummy = 1 if dag < $age_become_responsible & idmother == -9 & idfather == -9
bys idhh: egen orphan_hh = max(orphan_dummy)
tab orphan_dummy

*Try to assign adult female id as mother, if not available adult male: 
bys idhh: gen long idmother2 = idperson if dgn == 0 & dag > 18 //Keep at 18 and not age to become responsible as minimum age to give birth is 18?
gsort +idhh -dag
by idhh: carryforward idmother2, replace
replace idmother = idmother2 if dag < $age_become_responsible & idmother == -9 & idfather == -9 & !missing(idmother2)

bys idhh: gen long idfather2 = idperson if dgn == 1 & dag > 18 
gsort +idhh -dag
by idhh: carryforward idfather2, replace
replace idfather = idfather2 if dag < $age_become_responsible & idmother == -9 & idfather == -9 & !missing(idfather2)

*Some orphans still remain (5) - drop?
drop if dag < $age_become_responsible & idmother == -9 & idfather == -9

/*********************************Same-sex couples*****************************/

*Check for same-sex couples?
preserve
keep idhh ppno dgn
rename dgn dgn_partner
rename ppno pno 
drop if pno == 0
save $folder/temp_sex, replace
restore

merge 1:1 idhh pno using $folder/temp_sex
keep if _merge == 1 | _merge == 3
drop _merge
gen same_sex_couple = 1 if dgn == dgn_partner & !missing(dgn) & !missing(dgn_partner)


*Check same-sex couples for children: father/mother should stay with children 
*Double-check as might not work properly 
bys idhh: egen samesex_hh = max(same_sex_couple) 
tab samesex_hh if dag < $age_become_responsible //18 HH where same-sex couple is with someone < 18
/*

gen long parent_id_temp = idmother if samesex_hh == 1 & dag < 18
replace parent_id_temp = idfather if parent_id_temp == -9 & samesex_hh == 1
by idhh: egen long max_parent_id = max(parent_id_temp)
replace parent_id_temp = idperson if missing(parent_id_temp) & samesex_hh == 1 & !missing(max_parent_id)
drop max_parent_id
egen ss_parent = group(idhh parent_id_temp)
*/

replace idmother = . if idmother < 0
replace idfather = . if idfather < 0  

*Break-up same-sex couples into separate households:
gen long idmother3 = idmother if dag < $age_become_responsible
replace idmother3 = idperson if missing(idmother3) & dgn == 0 
gen long idfather3 = idfather if dag < $age_become_responsible
replace idfather3 = idperson if missing(idfather3) & dgn == 1

sort idhh pno 
bys idhh: gen dgn_hh = dgn if pno == 1 & samesex_hh == 1
by idhh: carryforward dgn_hh, replace

//bys idhh same_sex_couple: replace same_sex_couple = . if _n != 1 //Assign new ID to one of the couple
egen newid2 = group(idhh idmother3) if samesex_hh == 1 & dgn_hh == 0
egen newid3 = group(idhh idfather3) if samesex_hh == 1 & dgn_hh == 1
replace newid3 = newid3 + 5000
replace newid2 = newid3 if missing(newid2)
egen newid = group(idhh newid2) if samesex_hh == 1

replace idpartner = 0 if !missing(newid) == 1 //We don't allow same-sex partnerships in the simulation
tostring(newid), replace
replace newid = "666666"+newid if newid != "."
destring(newid), replace
replace idhh = newid if !missing(newid)
drop newid same_sex_couple dgn_partner

*New decision: drop same sex households (still do the split above in case we wanted to revert)
drop if samesex_hh == 1



/************************************Clean up**********************************/
*Set idpartner = 0 if single HH:
bys idhh: egen count = count(idperson) if dag >= $age_become_responsible | (dag < $age_become_responsible & ppno != 0)
replace idpartner = 0 if count == 1



*cap drop apartnum adult_count idmother2 idfather2 hgbioad1 hgbioad2 ppno pno ///
*	 orphan_dummy orphan_hh samesex_hh newid2 newid3 idmother3 idfather3 dgn_hh count

sort idhh idperson

/**********"Home" variable*****************************************/
*We decided to distinguish between a household (fiscal unit) and a "home". For example children 
*living with their parents will share the same "home". Create a new variable that contains the original household id for adult children 

*1. For those who are not adult children, home id should be the same as idhh 
*2. For those who are adult children, home id should be the idhh before the split
*(But: in the data there can be multigenerational families etc. that should still be split (?))
*So the home variable can only be defined after household splitting

*home id == idhh for everyone, but for households with adult children home id == hidp
gen double idhome = idhh
format idhome %15.0g
replace idhome = idhhmother if adultChildFlag == 1 & !missing(idhhmother)
replace idhome = idhhfather if adultChildFlag == 1 & missing(idhhmother) & !missing(idhhfather)

***Prepare remaining variables***


/****************************************Union*********************************/
*Generate union variable to indicate if spouse
*Partnerid are later filled in using ppno information, so dun should distinguish between spouse and just living together
gen dun = 0
replace dun = 1 if sppid > 0
la var dun "=1 if has spouse"

/************************* region (NUTS 1) **************************/ 


gen drgn1=.
replace drgn1=1 if gor_dv==1 
replace drgn1=2 if gor_dv==2 
replace drgn1=4 if gor_dv==3  // following FRS, code 3 not used
replace drgn1=5 if gor_dv==4 
replace drgn1=6 if gor_dv==5 
replace drgn1=7 if gor_dv==6 
replace drgn1=8 if gor_dv==7 
replace drgn1=9 if gor_dv==8 
replace drgn1=10 if gor_dv==9 
replace drgn1=11 if gor_dv==10
replace drgn1=12 if gor_dv==11
replace drgn1=13 if gor_dv==12


la var drgn1 "Region"
/*********************** household level weight ***********************/
clonevar dwt= hhdenub_xw

/***********************country****************************************/
gen dct=15
la var dct "Country code: UK"
/********************************** gender*****************************/

//Partner's gender:
tempfile temp_dgn
preserve
keep pidp dgn
rename pidp idpartner
rename dgn dgnsp
save temp_dgn, replace 
restore

merge m:1 idpartner using temp_dgn
la var dgnsp "Partner's gender"
keep if _merge==1 | _merge == 3
drop _merge

/***************** AGE *************************************************/ 

*Partner's age: 
tempfile temp_age
preserve
keep pidp dag
rename pidp idpartner 
rename dag dagsp 
save temp_age, replace
restore

merge m:1 idpartner using temp_age
la var dagsp "Partner's age"
keep if _merge == 1 | _merge == 3
drop _merge

/**********************************Health status************************/
*Use scsf1 variable, code negative values to missing, reverse code so 5 = excellent and higher number means better health
*Replace with values of ypsrhlth for youth:
replace scsf1 = ypsrhlth if (scsf1 == . | scsf1 < 0) & ypsrhlth != .

replace scsf1 = . if scsf1 < 0
recode scsf1 (5 = 1 "Poor") ///
	(4 = 2 "Fair") ///
	(3 = 3 "Good") ///
	(2 = 4 "Very good") ///
	(1 = 5 "Excellent") ///
	, into(dhe)
la var dhe "Health status"

//Partner's health status:
tempfile temp_dhe
preserve
keep pidp dhe
rename pidp idpartner
rename dhe dhesp
save temp_dhe, replace
restore

merge m:1 idpartner using temp_dhe
la var dhesp "Partner's health status"
keep if _merge == 1 | _merge == 3
drop _merge




/******************************Education status**************************/
*Use hiqual variable, code negative values to missing
*Low education: Other qualification, no qualification
*Medium education: Other higher degree, A-level etc, GCSE etc
*High education: Degree

*Use previous wave to fill in missing values if inidvidual no longer in education:
replace hiqual_dv = . if hiqual_dv < 0
if jbstat != 7{
	replace hiqual_dv = h_hiqual_dv if missing(hiqual_dv)
}

replace hiqual_dv = . if hiqual_dv < 0

recode hiqual_dv (1 = 1 "High") ///
	(2 3 4 = 2 "Medium") ///
	(5 9 = 3 "Low") ///
	, into(deh_c3)
la var deh_c3 "Education status"

/****************************In education**************************************/

gen ded = 1 if jbstat == 7
recode ded (. = 0)
lab def dummy_lb 0 "No" 1 "Yes"
la val ded dummy_lb
la var ded "In education"

replace ded = 1 if dag < 16 //Everyone under 16 should be in education

/****************************Return to education*******************************/
gen der = 0
replace der = 1 if jbstat == 7 & h_jbstat != 7
la val der dummy_lb
la var der "Return to education"

/***************************Partner's highest education status*****************/
tempfile temp_deh
preserve
keep pidp deh_c3
rename pidp idpartner 
rename deh_c3 dehsp_c3 
save temp_deh, replace
restore

merge m:1 idpartner using temp_deh
la var dehsp_c3 "Partner's education"
keep if _merge == 1 | _merge == 3
drop _merge


/********************************Parents' education**************************/ 
replace maedqf = . if maedqf < 0 
replace paedqf = . if paedqf < 0

recode maedqf (5 = 1 "High") ///
	(4 3 = 2 "Medium") ///
	(2 1 97 = 3 "Low") ///
	, into(dehm_c3) //dehm_c3 is mother's highest qualification
la var dehm_c3 "Education status mother"

recode paedqf (5 = 1 "High") ///
	(4 3 = 2 "Medium") ///
	(2 1 97 = 3 "Low") ///
	, into(dehf_c3) //dehf_c3 is father's highest qualification
la var dehf_c3 "Education status father"

save $folder/temp, replace

*If missing and living with parents, use their current level of education:

*1. Create mothers and fathers education levels in new file with person and hh id 
*2. Merge by father and mother id and hh id

keep pidp hidp deh_c3
drop if missing(deh_c3)
rename pidp idmother
rename deh_c3 mother_educ
save $folder/mother_edu, replace
rename idmother idfather
rename mother_educ father_educ
save $folder/father_edu, replace

use $folder/temp, clear
merge m:1 idmother hidp using $folder/mother_edu 
keep if _merge == 1 | _merge == 3
drop _merge
merge m:1 idfather hidp using $folder/father_edu 
keep if _merge == 1 | _merge == 3
drop _merge

replace dehm_c3 = mother_educ if missing(dehm_c3)
replace dehf_c3 = father_educ if missing(dehf_c3)

/*****************************Partnership status*******************************/
recode mastat_dv (2 3 10 = 1 "Partnered") ///
	(0 1 = 2 "Single never married") /// Includes children under 16
	(4 5 6 7 8 9 = 3 "Previously partnered") ///
	, into (dcpst)
la var dcpst "Partnership status"
recode dcpst (-8 -2 -1 = -9)

*If idpartner = 0 (because of household splitting), dcpst should be set to 3 depending on mastat_dv value
replace dcpst = 3 if dcpst == 1 & idpartner <= 0 
replace dcpst = 1 if idpartner > 0 & !missing(idpartner)

/*****************************Enter partnership********************************/
gen dcpen = 0
replace dcpen = 1 if (mastat_dv == 2 | mastat_dv == 3 | mastat_dv == 10) & (h_mastat_dv == 1 | h_mastat_dv == 4 ///
	| h_mastat_dv == 5 | h_mastat_dv == 6 | h_mastat_dv == 7 | h_mastat_dv == 8 | h_mastat_dv == 9) & idpartner > 0
la val dcpen dummy_lb
la var dcpen "Enter partnership"


/*****************************Exit partnership*********************************/
gen dcpex = 0
replace dcpex = 1 if (mastat_dv == 4 | mastat_dv == 5 | mastat_dv == 6 | mastat_dv == 7 | mastat_dv == 8) & (h_mastat_dv == 2 | h_mastat_dv == 3 ///
	| h_mastat_dv == 10) & idpartner <= 0
la val dcpex dummy_lb
la var dcpex "Exit partnership" 

/*****************************Age difference partners**************************/
gen dcpagdf = dag - dagsp if (dag > 0 & dagsp > 0) //Leave with negative values? Or should be absolute?
la var dcpagdf "Partner's age difference"


/*********************************Activity status****************************/
recode jbstat (1 2 5 = 1 "Employed or self-employed") ///
	(7 = 2 "Student") ///
	(3 6 8 10 11 97 9 = 3 "Not employed") /// includes apprenticeships, unpaid family business, govt training scheme
	(4 = 4 "Retired") ///
	, into(les_c3)
la var les "Activity status"

//For people under 16 set activity status to student:
replace les_c3 = 2 if dag <= 16
//People below age to leave home are not at risk of work so set activity status to not employed if not a student
replace les_c3 = 3 if dag < $age_become_responsible & les_c3 != 2

//Partner's activity status:
tempfile temp_lesc3
preserve
keep pidp idhh les_c3
rename les_c3 lessp_c3
rename pidp idpartner
save temp_lesc3, replace
restore
merge m:1 idpartner idhh using temp_lesc3
keep if _merge == 1 | _merge == 3
la var lessp_c3 "Partner's activity status"
drop _merge

//Own and Spousal Activity Status
gen lesdf_c4 = -9
replace lesdf_c4 = 1 if les_c3 == 1 & lessp_c3 == 1 & dcpst == 1 //Both employed
replace lesdf_c4 = 2 if les_c3 == 1 & (lessp_c3 == 2 | lessp_c3 == 3 | lessp_c3 == 4) & dcpst == 1 //Employed, spouse not employed
replace lesdf_c4 = 3 if (les_c3 == 2 | les_c3 == 3 | les_c3 == 4) & lessp_c3 == 1 & dcpst == 1 //Not employed, and spouse employed
replace lesdf_c4 = 4 if (les_c3 == 2 | les_c3 == 3 | les_c3 == 4) & (lessp_c3 == 2 | lessp_c3 == 3 | les_c3 == 4) & dcpst == 1 //Both not employed

la def lesdf_c4_lb 1"Both employed" 2"Employed and spouse not employed" 3"Not employed and spouse employed" 4"Both not employed" -9"Missing"
la val lesdf_c4 lesdf_c4_lb

la var lesdf_c4 "Own and spousal activity status"

*What activity status should be assigned if missing?
/******************************Civil servant status***************************/
gen lcs=0
// R.K. (11.05.2017) (we can use SIC 2007 condensed version- this is what Paola does for FRS EUROMOD)
replace lcs=1 if jbsic07_cc==84
la var lcs "Civil Servant"

/***********************************Hours of work*****************************/
recode jbhrs (-9/-1 . = .) //is it fine to recode these to 0? don't want to have missing in simulation?
recode jbot (-9/-1 . = .)
recode jshrs (-9/-1 . = .)

//lhw is the sum of the above, but don't want to take -9 into account. Recode into missing value. 

egen lhw=rowtotal(jbhrs jbot jshrs)
replace lhw = ceil(lhw)
la var lhw "Hours worked per week"

/*****************************Number of children*******************************/
//Number of children aged 0-2 (Checked against manually generating count of children 0-2 per HH - same numbers, but nch02_dv distinguishes missing and 0)
gen dnc02 = nch02_dv
recode dnc02 (-9 = 0)
la var dnc02 "Number of children aged 0-2"

//Number of dependent children aged 0-18 (dependent doesn't include children who have spouse / child but live with parents)
//Gen flag for a dependent child aged 0-18, with at least one parent and classified as dependent child
gen depChild = 1 if (age_dv >= 0 & age_dv <= 18) & (pns1pid > 0 | pns2pid > 0) & (depchl_dv == 1)
bys idhh: egen dnc = sum(depChild)
*drop depChild
la var dnc "Number of dependent children 0 - 18"

//Temporary number of children 0-13 and 14-18 to create household OECD equivalence scale
gen depChild_013 = 1 if (age_dv >= 0 & age_dv <= 13) & (pns1pid > 0 | pns2pid > 0) & (depchl_dv == 1)
gen depChild_1418 = 1 if (age_dv >= 14 & age_dv <= 18) & (pns1pid > 0 | pns2pid > 0) & (depchl_dv == 1)
bys idhh: egen dnc013 = sum(depChild_013)
bys idhh: egen dnc1418 = sum(depChild_1418)
drop depChild_013 depChild_1418



/************************Household composition*********************************/
gen dhhtp_c4 = -9
replace dhhtp_c4 = 1 if dcpst == 1 & dnc == 0 //Couple, no children
replace dhhtp_c4 = 2 if dcpst == 1 & dnc > 0 & !missing(dnc) //Couple, children
replace dhhtp_c4 = 3 if (dcpst == 2 | dcpst == 3) & (dnc == 0 | dag <= $age_become_responsible | adultChildFlag== 1) //Single, no children (Note: adult children and children below age to become responsible should be assigned "no children" category, even if there are some children in the household)
replace dhhtp_c4 = 4 if (dcpst == 2 | dcpst == 3) & dnc > 0 & !missing(dnc) & dhhtp_c4 != 3 //Single, children


la def dhhtp_c4_lb 1"Couple with no children" 2"Couple with children" 3"Single with no children" 4"Single with children"
la var dhhtp_c4 "Household composition"

/**************************OECD equivalence scale******************************/

gen moecd_eq = . //Modified OECD equivalence scale
replace moecd_eq = 1.5 if dhhtp_c4 == 1
replace moecd_eq = 0.3*dnc013 + 0.5*dnc1418 + 1.5 if dhhtp_c4 == 2
replace moecd_eq = 1 if dhhtp_c4 == 3
replace moecd_eq = 0.3*dnc013 + 0.5*dnc1418 + 1 if dhhtp_c4 == 4

//Drop children variables used to calculate moecd_eq as no longer needed
drop dnc013 dnc1418


/*****************************Income variables*********************************/
*These come from the income file
/*
gen hhnetinc1 = fihhmnnet1_dv if !missing(fihhmnnet1_dv)
gen adj_hhinc = hhnetinc1/ieqmoecd_dv
xtile ydses_c5 = adj_hhinc, nq(5)
la var ydses_c5 "HH income quintiles"
*/

*Generate individual income variables:
*inc_stp, inc_tu and inc_ma generated at the beginning from income file

egen ypnb = rowtotal(fimnlabgrs_dv fimnpen_dv fimnmisc_dv inc_stp inc_tu inc_ma) //Gross personal non-benefit income
egen yptc = rowtotal(fimnpen_dv fimnmisc_dv inc_stp inc_tu inc_ma) //Gross personal non-employment, non-benefit income
gen yplgrs = fimnlabgrs_dv //Gross personal employment income

*Generate ypnbsp for partnered
tempfile temp_ypnb
preserve
keep pidp idhh ypnb
rename ypnb ypnbsp
rename pidp idpartner
save temp_ypnb, replace
restore
merge m:1 idpartner idhh using temp_ypnb
keep if _merge == 1 | _merge == 3
drop _merge



*Household income:
egen yhhnb = rowtotal(ypnb ypnbsp) if dhhtp_c4 == 1 | dhhtp_c4 == 2 //Household income is sum of individual income and partner's income if coupled
replace yhhnb = ypnb if dhhtp_c4 == 3 | dhhtp_c4 == 4 //If single, household income is equal to individual income


*Income CPI from ONS:
gen CPI = .
replace CPI = 0.866 if intdaty_dv == 2009
replace CPI = 0.894 if intdaty_dv == 2010
replace CPI = 0.934 if intdaty_dv == 2011
replace CPI = 0.961 if intdaty_dv == 2012
replace CPI = 0.985 if intdaty_dv == 2013
replace CPI = 1.000 if intdaty_dv == 2014
replace CPI = 1.000 if intdaty_dv == 2015
replace CPI = 1.007 if intdaty_dv == 2016
replace CPI = 1.034 if intdaty_dv == 2017
replace CPI = 1.059 if intdaty_dv == 2018
replace CPI = 1.078 if intdaty_dv == 2019



*For household income, equivalise and adjust for inflation:
replace yhhnb = (yhhnb/moecd_eq)/CPI

*Adjust for inflation:
replace ypnb = ypnb/CPI
replace yptc = yptc/CPI
replace yplgrs = yplgrs/CPI
replace ypnbsp = ypnbsp/CPI

*Inverse hyperbolic sine transformation:
gen yhhnb_asinh = asinh(yhhnb)
gen ypnbihs_dv = asinh(ypnb)
gen ypnbihs_dv_sp = asinh(ypnbsp)
gen yptciihs_dv = asinh(yptc)
gen yplgrs_dv = asinh(yplgrs)

*Quintiles:

xtile ydses_c5 = yhhnb_asinh if depChild != 1, nq(5)
bys idhh: egen ydses_c5_tmp = max(ydses_c5)
replace ydses_c5 = ydses_c5_tmp if missing(ydses_c5)
drop ydses_c5_tmp

*Difference between own and spouse's gross personal non-benefit income
*gen ynbcpdf_dv = asinh(sinh(ypnbihs_dv) - sinh(ypnbihs_dv_sp))

*Keep as simple difference between the two for compatibility with estimates
gen ynbcpdf_dv = ypnbihs_dv - ypnbihs_dv_sp



la var ydses_c5 "Household income quintiles"
la var ypnbihs_dv "Gross personal non-benefit income"
la var yptciihs_dv "Gross personal non-employment, non-benefit income"
la var yplgrs_dv "Gross personal employment income"
la var ynbcpdf_dv "Difference between own and spouse's gross personal non-benefit income"




*Household income - quintiles
*Sum of personal non-benefit income and spouse's personal non-benefit income if coupled, personal non-benefit income otherwise, equivalised and normalised 


/************************Long-term sick or disabled****************************/
gen dlltsd = 0
replace dlltsd = 1 if jbstat == 8
replace dlltsd = 1 if missing(jbstat) & h_jbstat == 8
la var dlltsd "LT sick or disabled"



/******************************Disability benefit******************************/
gen bdi = 0 
foreach var in 1 2 3 5 7 8 10 12 97 { //If any of bendis1-bendis3, bendis5-bendis12, or bendis97 = 1 then received benefits
replace bdi = 1 if bendis`var' == 1
}
la val bdi dummy_lb
la var bdi "Disability benefits"


/*****************************Year left education******************************/
gen sedex = 0
replace sedex = 1 if jbstat != 7 & h_jbstat == 7
la val sedex dummy_lb
la var sedex "Left education"

/****************************Same-sex partnership******************************/
gen ssscp = 0
replace ssscp = 1 if (mastat_dv == 2 | mastat_dv == 3 | mastat_dv == 10) & (dgn == dgnsp) & !missing(dgn) & !missing(dgnsp)
la val ssscp dummy_lb
la var ssscp "Same-sex partnership"

/****************************Year prior to exiting partnership******************/
*Impossible to know for the most recent wave so set to 0 to have the variable
gen scpexpy = 0
la val scpexpy dummy_lb
la var scpexpy "Year prior to exiting partnership"

/*****************************Years in partnership [dcpyy]*********************/

cd $input_data\ukhls_w9
merge 1:1 pidp using tmp_partnershipDurationW9, keep(1 3) nogen 
rename partnershipDuration dcpyy

la var dcpyy "Years in partnership"

/*****************************Women aged 18 - 44*******************************/
gen sprfm = 0
replace sprfm = 1 if sex_dv == 2 & age_dv >= 18 & age_dv <= 44

la var sprfm "Woman in fertility range dummy (18- 44)"

/*****************************In educational age range*************************/
*What about age < 16? Set to 0? 
gen sedag = 1 if dvage >= 16 & dvage <= 29
*replace sedag = 0 if dvage >= 30 
replace sedag = 0 if missing(sedag)
la val sedag dummy_lb
la var sedag "Educ age range"


/*****************************SYSTEM VARIABLES*********************************/
*swv: Data collection wave (set to match wave defined in macro at the beginning)
gen swv = $wvno
la var swv "Data collection wave"

*stm: Year
gen stm = intdaty_dv
la var stm "Interview year"


*Keep required variables*
keep idhh idhome idperson idpartner idfather idmother dct drgn1 dwt dnc02 dnc dgn dag dhe dhesp dcpst ded deh_c3 der dehsp_c3 dehm_c3 dehf_c3 dcpen dcpyy dcpex dcpagdf dlltsd dhhtp_c4 les_c3 lessp_c3 lesdf_c4 ydses_c5 ypnbihs_dv yptciihs_dv yplgrs_dv ynbcpdf_dv swv sedex ssscp sprfm sedag stm dagsp lhw pno ppno hgbioad1 hgbioad2 der adultChildFlag 

sort idhh idperson 
//save "C:\Users\Patryk\Dropbox\EUROMODFiles\JAS-mine\LABSim\UKHLS\W9_input_ForStatistics.dta", replace


/*
keep idhh idperson idfather idmother idpartner drgn1 dwt dct dgn dag dagsp dhe deh_c3 dehm_c3 dehf_c3 les lcs lhw ///
	pno ppno hgbioad1 hgbioad2 dagsp der dehsp_c3 dcpen dcpex dcpagdf ydses_c5 ded bdi swv sedex ssscp scpexpy sedag stm dnc02
sort idhh idperson 

foreach var in idfather idmother idpartner drgn1 dgn dag dagsp dhe deh_c3 dehm_c3 dehf_c3 les dagsp der dehsp_c3 dcpen ///
dcpagdf ydses_c5 dcpex ded bdi swv sedex ssscp scpexpy sedag stm dnc02{
recode `var' (-9/-1=-9) (.=-9) 
}
*/

foreach var in idhh idhome idperson idpartner idfather idmother dct drgn1 dwt dnc02 dnc dgn dag dhe dhesp dcpst ded deh_c3 der dehsp_c3 dehm_c3 dehf_c3 dcpen dcpyy dcpex dlltsd dhhtp_c4 les_c3 lessp_c3 lesdf_c4 ydses_c5 ypnbihs_dv yptciihs_dv yplgrs_dv swv sedex ssscp sprfm sedag stm dagsp lhw pno ppno hgbioad1 hgbioad2 der{
qui recode `var' (-9/-1=-9) (.=-9) 
}

order idhome, before(idhh)

*drop if dag < 0 //Drop if missing age?



/********************************Drop households with missing values***********/
gen dropObs = . //Generate variable indicating whether household should be dropped

*Remove household if missing values present: (not using previous wave's values as migration possible)
replace dropObs = 1 if drgn1 == -9

*Missing age:
replace dropObs = 1 if dag == -9

*Missing age of partner (but has a partner):
recode idpartner (0 = -9)
replace dropObs = 1 if dagsp == -9 & idpartner != -9

*Health status - remove household if missing for adults but ignore children:
replace dropObs = 1 if dhe == -9 & dag > $age_become_responsible

*Health status of spouse - remove household if missing but individual has a spouse
replace dropObs = 1 if dhesp == -9 & idpartner != -9

*Education - remove household if missing education level for adults who are not in education:
replace dropObs = 1 if deh_c3 == -9 & dag >= $age_become_responsible & ded == 0

*Education of spouse - remove household if missing but individual has a spouse
replace dropObs = 1 if dehsp_c3 == -9 & idpartner != -9

*Parental education:
replace dropObs = 1 if dehm_c3 == -9 | dehf_c3 == -9

*Partnership status:
replace dropObs = 1 if dcpst == -9

*Activity status:
replace dropObs = 1 if les_c3 == -9 & dag >= $age_become_responsible

*Partner's activity status:
replace dropObs = 1 if lessp_c3 == -9 & idpartner != -9

*Own and spousal activity status:
replace dropObs = 1 if lesdf_c4 == -9 & idpartner != -9

*Household composition:
replace dropObs = 1 if dhhtp_c4 == -9

*Income:
replace dropObs = 1 if ypnbihs_dv == -9 & dag >= $age_become_responsible

replace dropObs = 1 if yplgrs_dv == -9 & dag >= $age_become_responsible

replace dropObs = 1 if ydses_c5 == -9

*If any person in the household has missing values, drop the whole household:
bys idhh: egen dropHH = max(dropObs)
drop if dropHH == 1 
*drop if dropObs == 1
drop dropObs dropHH

*Drop if weight = 0: 
*drop if dwt == 0 //Commented out as done later in master_conversion.do file

recode idmother idfather (. = -9)

*Cannot have missing values in continuous variables - recode to 0 for now: (But note this missings are valid in general - e.g. people without a partner don't have years in partnership etc.)
recode dcpyy dcpagdf ynbcpdf_dv dnc02 dnc ypnbihs_dv yptciihs_dv yplgrs_dv stm swv dhe dhesp (-9 . = 0) 

save "C:\Users\Patryk\Dropbox\EUROMODFiles\JAS-mine\LABSim\UKHLS\W9_input_HH_Drop.dta", replace
save $folder/final_temp, replace 
export delimited using "$output\population_UK_initial_raw.csv", nolabel replace















