/*********************************************************************
 Additional variables from the legacy version of variable_update.do

 These are required for the following estimation scripts:
 - reg_financial_distress.do
 - reg_health_mental.do
 - reg_health_wellbeing.do

 Longer-term, these should be migrated into variable_update.do, and
 the patterns established there (naming conventions etc.) used.
*********************************************************************/

* Convert to real values
gen econ_realnetinc=fihhmnnet1_dv/CPI
label var econ_realnetinc "Real net household income"

*Winsorise income variable
winsor econ_realnetinc , gen(inc_wins) p(0.001)
summ inc_wins, detail
label var inc_wins "Income: winsorised (net)"


* Generate equivalised household income
gen econ_realequivinc=inc_wins/ieqmoecd_dv
label var econ_realequivinc "Real equivalised household income"

* Generate inverse hyperbolic sine transformation
gen econ_realequivinct=asinh(econ_realequivinc)
label var econ_realequivinct "Transformed real equivalised household income"
* See Bellemare (2019) for coefficient interpretation


* Task 4
* Generate income change exposure
sort idperson swv
gen econ_incchange=econ_realequivinc - L.econ_realequivinc
label var econ_incchange "Income change level"


gen exp_incchange=.
replace exp_incchange=1 if (econ_realequivinc < L.econ_realequivinc) & econ_realequivinc!=. & L.econ_realequivinc!=.
replace exp_incchange=0 if (econ_realequivinc == L.econ_realequivinc) & econ_realequivinc!=. & L.econ_realequivinc!=.
replace exp_incchange=0 if (econ_realequivinc > L.econ_realequivinc) & econ_realequivinc!=. & L.econ_realequivinc!=.

label define incchangecat 1 "Decreased income" 0 "Increased or stable income"
label values exp_incchange incchangecat
* Very few obs. with stable income, perhaps define within a percentage change

tab exp_incchange, miss

gen log_income=ln(econ_realequivinc)
label var log_income "Log of real equivalised household net income"

* Income increase
gen inc_increase=D.log_income
label var inc_increase "Change rate of income increase"
* Set to zero for those without an increase in income
replace inc_increase=0 if exp_incchange==1 | ((econ_realequivinc == L.econ_realequivinc) & econ_realequivinc!=. & L.econ_realequivinc!=.)

* Income decrease
gen inc_decrease=D.log_income
label var inc_increase "Change rate of income decrease"
* Set to zero for those without a decrease in income
replace inc_decrease=0 if exp_incchange==0
summ inc_increase inc_decrease econ_incchange

* note: individuals moving from an increase to decrease (or vice versa) will have to effects applied.

* Task 5
* Poverty transitions

* Generate median income for sample
bysort swv: egen samp_medianinc=wpctile(econ_realequivinc), p(50) weights(dhhwt)
label var samp_medianinc "Median household income for sample in swv"
* ONS uses net income, before or after housing costs. Net income used here.
gen samp_poverty =samp_medianinc*0.60
label var samp_poverty "Poverty threshold"

tabstat samp_poverty, by(swv)
summ samp_poverty, detail

* Generate poverty marker
gen econ_poverty =(samp_poverty>=econ_realequivinc)
label var econ_poverty "Below poverty threshold (before housing costs)"
replace econ_poverty=. if econ_realequivinc==. | samp_poverty==.
label define yesno 0 "No" 1 "Yes"
label values econ_poverty yesno
tab econ_poverty swv, col
* Before housing costs used in LABSim

* Generate poverty exposure variable
sort idperson swv
gen exp_poverty= .
replace exp_poverty=0 if econ_poverty==0 & L.econ_poverty==0
replace exp_poverty=1 if econ_poverty==1 & L.econ_poverty==0
replace exp_poverty=2 if econ_poverty==0 & L.econ_poverty==1
replace exp_poverty=3 if econ_poverty==1 & L.econ_poverty==1
label define poverty_trans 0 "No Poverty" 1 "Entering poverty" 2 "Exiting poverty" 3 "Continuous poverty"
label values exp_poverty poverty_trans
label var exp_poverty "Poverty transition"
tab exp_poverty swv, m column


* Generate poverty gap marker
* define the poverty gap (Gi) as the poverty line (samp_poverty) less 
* actual income (econ_realequivinc) for individuals below the poverty line
* the gap is considered to be zero for everyone else
* Source: https://www.ilo.org/wcmsp5/groups/public/---americas/---ro-lima/---sro-port_of_spain/documents/presentation/wcms_304851.pdf
gen exp_povgap=.
replace exp_povgap=(samp_poverty-econ_realequivinc)/samp_poverty if econ_poverty==1
replace exp_povgap=0 if econ_poverty==0
label var exp_povgap "Poverty gap"

* Task 2 
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


* Generate working hours categories
gen lhw_zero=(lhw<=5)
gen lhw_ten=(lhw>=6 & lhw<=15)
gen lhw_twenty=(lhw>=16 & lhw<=25)
gen lhw_thirty=(lhw>=26 & lhw<=35)
gen lhw_forty=(lhw>=36 & lhw!=.)

gen lhw_c5=.
replace lhw_c5=0 if lhw_zero==1
replace lhw_c5=10 if lhw_ten==1
replace lhw_c5=20 if lhw_twenty==1
replace lhw_c5=30 if lhw_thirty==1
replace lhw_c5=40 if lhw_forty==1
