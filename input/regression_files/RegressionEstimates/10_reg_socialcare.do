********************************************************************************
* PROJECT:  		SimPaths UK
* SECTION:			SOCIAL CARE RECEIPT
* AUTHORS:			Justin van de Ven, Matteo Richiardi, Daria Popova, Ashley Burdett
* LAST UPDATE:		15 April 2026 (DP) 
* COUNTRY: 			UK  
*
*   NOTES: 		
*	PROGRAM TO EVALUATE SOCIAL CARE RECEIPT FROM UKHLS DATA
*	ANALYSIS BASED ON THE SOCIAL CARE MODULE OF UKHLS
*	First version: Justin van de Ven, 28 Aug 2023
*	Refactored version: Matteo Richiardi, 16 Feb 2026
*   Integration into the pipeline: Daria Popova 18 Feb 2026 DP 
* 
*******************************************************************************/

/* ANALYTICAL STRATEGY
We analyse/simulate the following variables:
- NeedCare
- ReceiveCare
- CareMarket: Formal, Informal, Mixed
- HrsReceivedFormalIHS
- HrsReceivedInformalIHS
- ProvideCare
- HrsProvidedInformalIHS
(IHS stands for Inverse Hyperbolic Sine transformation)

The most complicated case is for Partnered, as an issue of consistency arises (care between partners is most common):

===========================================
		Partner B: Receiving informal care?
___________________________________________
Partner A:		|			No		Yes
providing		|	No	|	(1)		(2)
informal care	|	Yes	|	(3)		(4)
===========================================

In the analysis we do not distinguish whom care is received from, and to whom care is provided. 
However, the cases above imply:
(1) No hrs received, no hrs provided
(2) All hrs received are from non-partner
(3) All hrs provided are to non-partner_socare_hrs
(4) At least some of the hrs received/provided are from/to partner

==========================================================================================
RMK: We first analyse care receipt, and then care provision. This order must be preserved.
==========================================================================================
*/

* CRITICAL: Clear all FIRST
clear all
set more off
set mem 200m
set type double
//set maxvar 120000
set maxvar 30000


/********************************* SET LOG FILE *******************************/
cap log close 
log using "${dir_log}/reg_socialcare.log", replace


/********************************* SET EXCEL FILE *****************************/

putexcel set "$dir_results/reg_socialcare", sheet("Info") modify //replace
putexcel A1 = "Description:", bold
putexcel B1 = "Model parameters for social care module"
putexcel A2 = "Authors:"
putexcel B2 = "Justin van de Ven, Ashley Burdett, Matteo Richiardi, Daria Popova"
putexcel A3 = "Last edit:"
putexcel B3 = "15 April 2026 (DP)"

putexcel A5 = "Process:", bold
putexcel B5 = "Description:", bold

putexcel A6 = "S2a" B6 = "Prob. need care"
putexcel A7 = "S2b" B7 = "Prob. receive care"
putexcel A8 = "S2c" B8 = "Prob. receive Formal/informal care"
putexcel A9 = "S2d" B9 = "Informal care hours received"
putexcel A10 = "S2e" B10 = "Hours of formal care received"

putexcel A11 = "S3a" B11 = "Prob. provide care, Singles"
putexcel A12 = "S3b" B12 = "Prob. provide care, Partnered"
putexcel A13 = "S3c" B13 = "Hours of informal care provided, Singles"
putexcel A14 = "S3d" B14 = "Hours of informal care provided, Partnered"

putexcel A20 = "Notes:", bold
putexcel B20 = "Estimation sample: UK_ipop.dta with grossing up weight dwt" 
putexcel B21 = "Conditions for processes are defined as globals in master.do"

putexcel set "$dir_results/reg_socialcare", sheet("Gof") modify
putexcel A1 = "Goodness of fit", bold


/********************************* PREPARE DATA *******************************/

use "${estimation_sample}", clear

* Time series structure
gsort idperson stm
xtset idperson stm

* Adjust variables 
do "${dir_do}/variable_update.do"


/********************************** ESTIMATION ********************************/

* Run Stata programs to produce Excel file 
do "${dir_do}/programs.do" 

* Stats for if conditions
/*
table stm, stat (count NeedCare) stat (mean NeedCare)								// [2015, 2022]
table stm, stat (count ReceiveCare) stat (mean ReceiveCare)							// [2016, 2021] but with significant decrease in 2020 and 2021
table stm, stat (count receive_formal_care) stat (mean receive_formal_care)			// [2016, 2021] but with significant decrease in 2020 and 2021
table stm, stat (count receive_informal_care) stat (mean receive_informal_care)		// [2016, 2021] but with significant decrease in 2020 and 2021
table stm, stat (count provide_informal_care) stat (mean provide_informal_care)		// [2015, 2024] also 2014, but fewer hours
*/
/*
table stm, c(count NeedCare mean NeedCare)
table stm, c(count ReceiveCare mean ReceiveCare)
table stm, c(count receive_formal_care mean receive_formal_care)
table stm, c(count receive_informal_care mean receive_informal_care)
table stm, c(count provide_informal_care mean provide_informal_care)
*/

/* Age variables (for experimenting -> copy and paste in the specification)
	Dag Dagsq ///
	Age67to68 Age69to70 Age71to72 Age73to74 Age75to76 ///
	Age77to78 Age79to80 Age81to82 Age83to84 Age85plus ///
*/


/************************ Probit need care (S2a) ******************************/
display "${s2a_if_condition}"
/*
probit NeedCare ///
    NeedCare_L1 ///
	Dgn ///
	Age67to68 Age69to70 Age71to72 Age73to74 Age75to76 ///
	Age77to78 Age79to80 Age81to82 Age83to84 Age85plus ///	
	Dhe_Fair Dhe_Good Dhe_VeryGood Dhe_Excellent ///
	Partnered ///
	Deh_c4_Medium Deh_c4_Low ///
	Y2020 Y2021  ${regions} ${ethnicity} ///
	if ${s2a_if_condition} [pw=${weight}], vce(r)
	
process_regression, domain("socialcare") process("S2a") sheet("S2a_orig") ///
	title("Process S2a: Prob. need care") ///
	gofrow(3) goflabel("S2a - Need care") ///
	ifcond("${s2a_if_condition}") probit
*/


probit careNeedFlag ///
    careNeedFlagL1 ///
	demMaleFlag ///
	demAge67to68 demAge69to70 demAge71to72 demAge73to74 demAge75to76 ///
	demAge77to78 demAge79to80 demAge81to82 demAge83to84 demAge85plus ///	
	healthPhysicalPcsL1 healthMentalMcsL1  ///
	demPartnerStatusPartnered ///
	eduHighestC4Medium eduHighestC4Low ///
	demYear2020 demYear2021 $regions  $ethnicity /// 
  if ${s2a_if_condition} [pw=${weight}], vce(r)
	
process_regression, domain("socialcare") process("S2a") sheet("S2a") ///
	title("Process S2a: Prob. need care") ///
	gofrow(3) goflabel("S2a - Need care") ///
	ifcond("${s2a_if_condition}") probit


/************************ Probit receive care (S2b) ***************************/
display "${s2b_if_condition}"
/*
probit ReceiveCare ///
    ReceiveCare_L1 ///
	Dgn ///
	Age67to68 Age69to70 Age71to72 Age73to74 Age75to76 ///
	Age77to78 Age79to80 Age81to82 Age83to84 Age85plus ///
	Dhe_Fair Dhe_Good Dhe_VeryGood Dhe_Excellent ///
	Partnered ///
	Deh_c4_Medium Deh_c4_Low ///
	HHincomeQ2 HHincomeQ3 HHincomeQ4 HHincomeQ5 ///
	Y2020 Y2021 ${regions} ${ethnicity} ///
	if ${s2b_if_condition} [pw=${weight}], vce(r)

process_regression, domain("socialcare") process("S2b") sheet("S2b_orig") ///
	title("Process S2b: Prob. receive care") ///
	gofrow(7) goflabel("S2b - Receive care") ///
	ifcond("${s2b_if_condition}") probit
	*/
	
probit careReceivedFlag ///
    careReceivedFlagL1 ///
    demMaleFlag ///
	demAge67to68 demAge69to70 demAge71to72 demAge73to74 demAge75to76 ///
	demAge77to78 demAge79to80 demAge81to82 demAge83to84 demAge85plus ///
	healthPhysicalPcsL1 healthMentalMcsL1  ///
	demPartnerStatusPartnered ///	
	eduHighestC4Medium eduHighestC4Low ///
	yHhQuintilesMonthC5Q2 yHhQuintilesMonthC5Q3 yHhQuintilesMonthC5Q4 yHhQuintilesMonthC5Q5 ///
	demYear2020 demYear2021 $regions  $ethnicity /// 
  if ${s2b_if_condition} [pw=${weight}], vce(r)

process_regression, domain("socialcare") process("S2b") sheet("S2b") ///
	title("Process S2b: Prob. receive care") ///
	gofrow(7) goflabel("S2b - Receive care") ///
	ifcond("${s2b_if_condition}") probit


/************************ Mlogit formal/informal (S2c) ************************/

display "${s2c_if_condition}"
/*  
	Informal is base outcome
	Mixed is 1st outcome
	Formal is 2nd outcomes
*/
/*
mlogit CareMarket CareMarketFormal_L1 CareMarketInformal_L1 CareMarketMixed_L1 Dgn ///
	Age67to68 Age69to70 Age71to72 Age73to74 Age75to76 ///
	Age77to78 Age79to80 Age81to82 Age83to84 Age85plus ///
	Dhe_Fair Dhe_Good Dhe_VeryGood Dhe_Excellent ///
	Partnered ///
	Deh_c4_Medium Deh_c4_Low ///
	HHincomeQ2 HHincomeQ3 HHincomeQ4 HHincomeQ5 ///	
	Y2020 Y2021 ${regions} ${ethnicity} ///
	if ${s2c_if_condition} [pw=${weight}], vce(r) base(2)
	
process_gologit, domain("socialcare") process("S2c") sheet("S2c_orig") ///
    title("Process S2c: Formal vs Informal") ///
    gofrow(11) goflabel("S2c - Formal vs Informal") ///
    outcomes(3) ///
    ifcond("${s2c_if_condition}")	
*/
	
mlogit CareMarket ///
    careMarketFormalL1 careMarketInformalL1 careMarketMixedL1 ///
	demMaleFlag ///
	demAge67to68 demAge69to70 demAge71to72 demAge73to74 demAge75to76 ///
	demAge77to78 demAge79to80 demAge81to82 demAge83to84 demAge85plus ///
	healthPhysicalPcsL1 healthMentalMcsL1  ///
	demPartnerStatusPartnered ///		
	eduHighestC4Medium eduHighestC4Low  ///	
	yHhQuintilesMonthC5Q2 yHhQuintilesMonthC5Q3 yHhQuintilesMonthC5Q4 yHhQuintilesMonthC5Q5 ///	
    demYear2020 demYear2021 $regions  $ethnicity /// 
  if ${s2c_if_condition} [pw=${weight}], vce(r) base(2)
	
process_gologit, domain("socialcare") process("S2c") sheet("S2c") ///
    title("Process S2c: Formal vs Informal") ///
    gofrow(11) goflabel("S2c - Formal vs Informal") ///
    outcomes(3) ///
    ifcond("${s2c_if_condition}")	
	

/******************** OLS informal care hours received (S2d) ******************/
display "${s2d_if_condition}"
/*
reg HrsReceivedInformalIHS HrsReceivedInformalIHS_L1 CareMarketMixed Dgn ///
	Age AgeSquared ///
	Dhe_Fair Dhe_Good Dhe_VeryGood Dhe_Excellent ///
	Partnered ///
	Deh_c4_Medium Deh_c4_Low ///
	HHincomeQ2 HHincomeQ3 HHincomeQ4 HHincomeQ5 ///
	Y2020 Y2021 ${regions} /*${ethnicity} Ethn_White*/ ///
	if ${s2d_if_condition} [pweight=${weight}], vce(r)
	
process_regression, domain("socialcare") process("S2d") sheet("S2d_orig") ///
	title("Process S2d: Informal care hours received") ///
	gofrow(15) goflabel("S2d - Hours of informal care received") ///
	ifcond("${s2d_if_condition}")
*/

reg careHrsInformalIhs ///
    careHrsInformalIhsL1 ///
	careMarketMixed ///
	demMaleFlag ///
	demAge demAgeSq ///
	healthPhysicalPcsL1 healthMentalMcsL1  ///
	demPartnerStatusPartnered ///		
	 eduHighestC4Medium eduHighestC4Low ///	
	yHhQuintilesMonthC5Q2 yHhQuintilesMonthC5Q3 yHhQuintilesMonthC5Q4 yHhQuintilesMonthC5Q5 ///	
	demYear2020 demYear2021 $regions  /*$ethnicity*/ /// 
  if ${s2d_if_condition} [pweight=${weight}], vce(r)
	
process_regression, domain("socialcare") process("S2d") sheet("S2d") ///
	title("Process S2d: Informal care hours received") ///
	gofrow(15) goflabel("S2d - Hours of informal care received") ///
	ifcond("${s2d_if_condition}")


* Calculate RMSE
cap drop residuals squared_residuals  
predict residuals, residuals
gen squared_residuals = residuals^2

preserve
keep if ${s2d_if_condition}

sum squared_residuals [w=${weight}], meanonly
scalar rmse = sqrt(r(mean))
di "RMSE for Informal care hours received: " rmse

putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A9 = ("S2d") B9 = (rmse)

restore	

/********************* OLS formal care hours received (S2e) *******************/
display "${s2e_if_condition}"
/*
reg HrsReceivedFormalIHS HrsReceivedFormalIHS_L1 CareMarketMixed Dgn ///
	Dhe_Fair Dhe_Good Dhe_VeryGood Dhe_Excellent ///
	Partnered ///
	Deh_c4_Medium Deh_c4_Low ///
	HHincomeQ2 HHincomeQ3 HHincomeQ4 HHincomeQ5 ///
	Y2020 Y2021 ${regions} ${ethnicity} ///
	if ${s2e_if_condition} [pweight=${weight}], vce(r)

process_regression, domain("socialcare") process("S2e") sheet("S2e_orig") ///
	title("Process S2e: Formal care hours received") ///
	gofrow(19) goflabel("S2e - Hours of formal care received") ///
	ifcond("${s2e_if_condition}")
	*/
	
reg careHrsFormalIhs ///
    careHrsFormalIhsL1 ///
	careMarketMixed ///
	demMaleFlag ///
	healthPhysicalPcsL1 healthMentalMcsL1  ///
	demPartnerStatusPartnered ///	
	eduHighestC4Medium eduHighestC4Low ///	
	yHhQuintilesMonthC5Q2 yHhQuintilesMonthC5Q3 yHhQuintilesMonthC5Q4 yHhQuintilesMonthC5Q5 ///	
	demYear2020 demYear2021 $regions $ethnicity /// 
  if ${s2e_if_condition} [pweight=${weight}], vce(r)

process_regression, domain("socialcare") process("S2e") sheet("S2e") ///
	title("Process S2e: Formal care hours received") ///
	gofrow(19) goflabel("S2e - Hours of formal care received") ///
	ifcond("${s2e_if_condition}")

* Calculate RMSE
cap drop residuals squared_residuals  
predict residuals, residuals
gen squared_residuals = residuals^2

preserve
keep if ${s2e_if_condition}

sum squared_residuals [w=${weight}], meanonly
scalar rmse = sqrt(r(mean))
di "RMSE for Formal care hours received: " rmse

putexcel set "$dir_results/reg_RMSE.xlsx", sheet("UK") modify
putexcel A10 = ("S2e") B10 = (rmse)

restore	


/***************** Probit provide care, Singles (S3a) *************************/

display "${s3a_if_condition}"
/*
probit ProvideCare ProvideCare_L1 NeedCare ReceiveCare Dgn ///
	Age30to34 Age35to39 Age40to44 Age45to49 Age50to54 ///
	Age55to59 Age60to64 Age65to69 Age70to74 Age75to79 Age80to84 Age85plus ///
	Dhe_Fair Dhe_Good Dhe_VeryGood Dhe_Excellent ///
	Deh_c4_High Deh_c4_Medium Deh_c4_Low ///
	HHincomeQ2 HHincomeQ3 HHincomeQ4 HHincomeQ5 ///
	Y2020 Y2021 ${regions} ${ethnicity} ///
	if ${s3a_if_condition} [pweight=${weight}], vce(r)
	
process_regression, domain("socialcare") process("S3a") sheet("S3a_orig") ///
	title("Process S3a: Prob. provide care, Singles") ///
	gofrow(23) goflabel("S3a - Provide care, Singles") ///
	ifcond("${s3a_if_condition}") probit
*/

probit careProvidedFlag ///
    careProvidedFlagL1 ///
	careNeedFlag careReceivedFlag ///
	demMaleFlag ///
	demAge30to34 demAge35to39 demAge40to44 demAge45to49 demAge50to54 ///
	demAge55to59 demAge60to64 demAge65to69 demAge70to74 demAge75to79 demAge80to84 demAge85plus ///
	healthPhysicalPcsL1 healthMentalMcsL1  ///
	eduHighestC4High eduHighestC4Medium eduHighestC4Low   ///	
	yHhQuintilesMonthC5Q2 yHhQuintilesMonthC5Q3 yHhQuintilesMonthC5Q4 yHhQuintilesMonthC5Q5 ///	
	demYear2020 demYear2021 $regions $ethnicity /// 
   if ${s3a_if_condition} [pweight=${weight}], vce(r)
	
process_regression, domain("socialcare") process("S3a") sheet("S3a") ///
	title("Process S3a: Prob. provide care, Singles") ///
	gofrow(23) goflabel("S3a - Provide care, Singles") ///
	ifcond("${s3a_if_condition}") probit
	
	

/***************** Probit provide care, Partnered (S3b) ***********************/
/*
tab CareMarket ProvideCare if ${s3b_if_condition}
tab deh_c4 ProvideCare if ${s3b_if_condition}
deh_c4 =0 is excluded because there's just 1 obs providing care and probit would not converge 
*/
display "${s3b_if_condition}"
/*
capture drop in_sample p
probit ProvideCare ProvideCare_L1 NeedCare ReceiveCare Dgn ///
	ReceiveCarePartner CareMarketFormalPartner CareMarketInformalPartner CareMarketMixedPartner ///
	Dhe_Poor Dhe_Fair Dhe_Good Dhe_VeryGood ///
	Dhesp_Fair Dhesp_Good Dhesp_VeryGood Dhesp_Excellent ///
	Deh_c4_High Deh_c4_Medium  ///
	HHincomeQ2 HHincomeQ3 HHincomeQ4 HHincomeQ5 ///
	Y2020 Y2021 ${regions} ${ethnicity} ///
	if ${s3b_if_condition} [pweight=${weight}], vce(r)
	
process_regression, domain("socialcare") process("S3b") sheet("S3b_orig") ///
	title("Process S3b: Prob. provide care, Partnered") ///
	gofrow(27) goflabel("S3b - Provide care, Partnered") ///
	ifcond("${s3b_if_condition}") probit
*/

//capture drop in_sample p
probit careProvidedFlag ///
    careProvidedFlagL1 ///
	careNeedFlag careReceivedFlag ///
    demMaleFlag ///
    careReceivedPartnerFlag careMarketFormalPartner careMarketInformalPartner careMarketMixedPartner ///
    healthPhysicalPcsL1 healthMentalMcsL1  ///
    healthPhysicalPartnerPcsL1 healthMentalPartnerMcsL1 ///
    eduHighestC4High eduHighestC4Medium  ///	
    yHhQuintilesMonthC5Q2 yHhQuintilesMonthC5Q3 yHhQuintilesMonthC5Q4 yHhQuintilesMonthC5Q5 ///	
    demYear2020 demYear2021 $regions  $ethnicity /// 
  if ${s3b_if_condition} [pweight=${weight}], vce(r)
	
process_regression, domain("socialcare") process("S3b") sheet("S3b") ///
	title("Process S3b: Prob. provide care, Partnered") ///
	gofrow(27) goflabel("S3b - Provide care, Partnered") ///
	ifcond("${s3b_if_condition}") probit
	
			
	
/************** Ordered logit care hours provided, Singles (S3c) **************/
display "${s3c_if_condition}"
/*
reg HrsProvidedInformalIHS HrsProvidedInformalIHS_L1 Dgn ///
	Age20to24 Age25to29 Age30to34 Age35to39 Age40to44 Age45to49 Age50to54 ///
	Age55to59 Age60to64 Age65to69 Age70to74 Age75to79 Age80to84 Age85plus ///
	Dhe_Fair Dhe_Good Dhe_VeryGood Dhe_Excellent ///
	Deh_c4_High Deh_c4_Medium Deh_c4_Low ///
	HHincomeQ2 HHincomeQ3 HHincomeQ4 HHincomeQ5 ///
	Y2020 Y2021 ${regions} ${ethnicity} ///
	if ${s3c_if_condition} [pweight=${weight}], vce(r)

process_regression, domain("socialcare") process("S3c") sheet("S3c_orig") ///
	title("Process S3c: Informal care hours provided, Singles") ///
	gofrow(31) goflabel("S3c - Hours of informal care provided, Singles") ///
	ifcond("${s3c_if_condition}")
	*/

* Ordered outcome for weekly informal-care hours provided.  The category values
* are representative hours for the seven bands: 2, 7, 14.5, 27, 42, 74.5, 120.
cap drop careHrsProvidedWeekCategory
gen byte careHrsProvidedWeekCategory = .
replace careHrsProvidedWeekCategory = 1 if careHoursProvidedWeekly > 0 & careHoursProvidedWeekly <= 4
replace careHrsProvidedWeekCategory = 2 if careHoursProvidedWeekly > 4 & careHoursProvidedWeekly <= 9
replace careHrsProvidedWeekCategory = 3 if careHoursProvidedWeekly > 9 & careHoursProvidedWeekly <= 19
replace careHrsProvidedWeekCategory = 4 if careHoursProvidedWeekly > 19 & careHoursProvidedWeekly <= 34
replace careHrsProvidedWeekCategory = 5 if careHoursProvidedWeekly > 34 & careHoursProvidedWeekly <= 49
replace careHrsProvidedWeekCategory = 6 if careHoursProvidedWeekly > 49 & careHoursProvidedWeekly <= 99
replace careHrsProvidedWeekCategory = 7 if careHoursProvidedWeekly >= 100 & careHoursProvidedWeekly < .
label define careHrsProvidedWeekCategoryLabel 1 "2" 2 "7" 3 "14.5" 4 "27" 5 "42" 6 "74.5" 7 "120", replace
label values careHrsProvidedWeekCategory careHrsProvidedWeekCategoryLabel

ologit careHrsProvidedWeekCategory ///
    careHrsProvidedWeekIhsL1 ///
    demMaleFlag ///
	demAge20to24 demAge25to29 demAge30to34 demAge35to39 demAge40to44 demAge45to49 demAge50to54 ///
	demAge55to59 demAge60to64 demAge65to69 demAge70to74 demAge75to79 demAge80to84 demAge85plus ///
	healthPhysicalPcsL1 healthMentalMcsL1  ///
	eduHighestC4High eduHighestC4Medium eduHighestC4Low   ///	
	yHhQuintilesMonthC5Q2 yHhQuintilesMonthC5Q3 yHhQuintilesMonthC5Q4 yHhQuintilesMonthC5Q5 ///	
	demYear2020 demYear2021 $regions  $ethnicity /// 
  if ${s3c_if_condition} [pweight=${weight}], vce(robust)

process_ologit, domain("socialcare") process("S3c") sheet("S3c") ///
	title("Process S3c: Informal care hours provided, Singles") ///
	gofrow(31) goflabel("S3c - Hours of informal care provided, Singles") ///
	ifcond("${s3c_if_condition}")

	* RMSE is not defined for the ordered-logit outcome.
		

/************* Ordered logit care hours provided, Partnered (S3d) *************/
display "${s3d_if_condition}"
/*
reg HrsProvidedInformalIHS HrsProvidedInformalIHS_L1 Dgn ///
	Age20to24 Age25to29 Age30to34 Age35to39 Age40to44 Age45to49 Age50to54 ///
	Age55to59 Age60to64 Age65to69 Age70to74 Age75to79 Age80to84 Age85plus ///
	ReceiveCarePartner CareMarketFormalPartner CareMarketInformalPartner CareMarketMixedPartner ///
	Dhe_Poor Dhe_Fair Dhe_Good Dhe_VeryGood ///
	Dhesp_Fair Dhesp_Good Dhesp_VeryGood Dhesp_Excellent ///
	Deh_c4_High Deh_c4_Medium Deh_c4_Low ///
	HHincomeQ2 HHincomeQ3 HHincomeQ4 HHincomeQ5 ///
	Y2020 Y2021 ${regions} ${ethnicity} ///
	if ${s3d_if_condition} [pweight=${weight}], vce(r)

process_regression, domain("socialcare") process("S3d") sheet("S3d_orig") ///
	title("Process S3d: Informal care hours provided, Partnered") ///
	gofrow(35) goflabel("S3d - Hours of informal care provided, Partnered") ///
	ifcond("${s3d_if_condition}")
	*/

ologit careHrsProvidedWeekCategory ///
    careHrsProvidedWeekIhsL1 ///
	demMaleFlag ///
	demAge20to24 demAge25to29 demAge30to34 demAge35to39 demAge40to44 demAge45to49 demAge50to54 ///
	demAge55to59 demAge60to64 demAge65to69 demAge70to74 demAge75to79 demAge80to84 demAge85plus ///
    careReceivedPartnerFlag careMarketFormalPartner careMarketInformalPartner careMarketMixedPartner ///
	healthPhysicalPcsL1 healthMentalMcsL1  ///
	healthPhysicalPartnerPcsL1 healthMentalPartnerMcsL1 ///
	eduHighestC4High eduHighestC4Medium  ///	
	yHhQuintilesMonthC5Q2 yHhQuintilesMonthC5Q3 yHhQuintilesMonthC5Q4 yHhQuintilesMonthC5Q5 ///	
	demYear2020 demYear2021 $regions  $ethnicity /// 
   if ${s3d_if_condition} [pweight=${weight}], vce(robust)

process_ologit, domain("socialcare") process("S3d") sheet("S3d") ///
	title("Process S3d: Informal care hours provided, Partnered") ///
	gofrow(35) goflabel("S3d - Hours of informal care provided, Partnered") ///
	ifcond("${s3d_if_condition}")
	
	* RMSE is not defined for the ordered-logit outcome.
	

display "Social care analysis complete!"
