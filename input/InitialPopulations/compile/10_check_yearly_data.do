***************************************************************************************
* PROJECT:              ESPON: construct initial populations for SimPaths using UKHLS data 
* DO-FILE NAME:         10_check_yearly_data.do
* DESCRIPTION:          This file computes descriptives to compare the initial populations before and after dropping households with missing values 
***************************************************************************************
* COUNTRY:              UK
* DATA:         	    UKHLS EUL version - UKDA-6614-stata [to wave n]
* AUTHORS: 				Daria Popova
* LAST UPDATE:          3 Nov 2025 DP 
* NOTE:					Called from 00_master.do - see master file for further details
***************************************************************************************/*
set matsize 11000, permanently
********************************************************************************/
cap log close 
log using "${dir_log}/10_check_yearly_data.log", replace
********************************************************************************
 

*all variables 
#delimit ;
local varlist 
idHh
idBu
idPers
idMother
idFather
statCollectionWave                            
demMaleFlag                           
demAge                            
demNChild0to2                           
demNChild                           
eduSpellFlag                        
eduExitSampleFlag                         
healthDsblLongtermFlag                    
healthSelfRated                            
yHhQuintilesMonthC5                       
yEmpPersGrossMonth                       
yNonBenPersGrossMonth                      
yMiscPersGrossMonth                     
demPartnerNYear                           
demAgePartnerDiff                     
yPersAndPartnerGrossDiffMonth                  
eduReturnFlag                           
eduHighestMotherC3                      
eduHighestFatherC3                        
statInterviewYear                           
healthWbScore0to36                           
demWbScore0to12                      
wealthPrptyFlag                     
demWbScore0to12_miss_flag                
labHrsWorkWeek                             
demRgn                            
labC4                          
healthPsyDstrssFlag                          
demAdultChildFlag          
wgtHhCross                              
labWageHrly
labWageHrlyL1     
wealthTotValue                    
wealthPensValue
wealthPrptyValue
wealthMortgageDebtValue
careNeedFlag                      
careHrsFormal                 
careHrsFromPartner               
careHrsFromDaughter                  
careHrsFromSon                   
careHrsFromOther                 
careCareFormal 
yCapitalPersMonth                           
yPensPersGrossMonth 
healthMentalMcs 
healthPhysicalPcs 
demEthnC4 
demEthnC6
labUnempFlag  
demLifeSatScore1to7
yFinDstrssFlag
careHrsProvidedWeek
labEmpNyear
;
#delimit cr // cr stands for carriage return

*varlist for categorical variables 
#delimit ;
local varlist_cat 
eduHighestC3  
healthSelfRated                    
yHhQuintilesMonthC5 
eduHighestMotherC3                    
eduHighestFatherC3              
labC4                     
demRgn   
demEthnC4  
demEthnC6     
;
#delimit cr // cr stands for carriage return 


*new varlist with categorical variables outputted by category 
#delimit ;
local varlist2  
idHh
idBu
idPers
idMother
idFather    
statCollectionWave                            
demMaleFlag                           
demAge                            
demNChild0to2                           
demNChild 
eduSpellFlag                       
eduExitSampleFlag              
healthDsblLongtermFlag   
yCapitalPersMonth                           
yPensPersGrossMonth         
yEmpPersGrossMonth                       
yNonBenPersGrossMonth                      
yMiscPersGrossMonth
demPartnerNYear                           
demAgePartnerDiff                     
yPersAndPartnerGrossDiffMonth                  
eduReturnFlag                           
statInterviewYear                                              
healthWbScore0to36                      
labHrsWorkWeek                         
healthPsyDstrssFlag                          
demAdultChildFlag                   
wgtHhCross                              
eduHighestC3_1 
eduHighestC3_2 
eduHighestC3_3 
healthSelfRated_1 
healthSelfRated_2 
healthSelfRated_3 
healthSelfRated_4 
healthSelfRated_5 
yHhQuintilesMonthC5_1 
yHhQuintilesMonthC5_2 
yHhQuintilesMonthC5_3 
yHhQuintilesMonthC5_4 
yHhQuintilesMonthC5_5 
eduHighestMotherC3_1 
eduHighestMotherC3_2 
eduHighestMotherC3_3 
eduHighestFatherC3_1 
eduHighestFatherC3_2 
eduHighestFatherC3_3 
labC4_1 
labC4_2 
labC4_3 
labC4_4 
demRgn_1 
demRgn_2 
demRgn_3 
demRgn_4 
demRgn_5 
demRgn_6 
demRgn_7 
demRgn_8 
demRgn_9 
demRgn_10 
demRgn_11 
demRgn_12
labWageHrly
labWageHrlyL1  
careNeedFlag                      
careHrsFormal                 
careHrsFromPartner               
careHrsFromDaughter                  
careHrsFromSon                   
careHrsFromOther                 
careCareFormal
wealthTotValue                    
wealthPensValue
wealthPrptyValue
wealthMortgageDebtValue
healthSelfRatedmcs 
healthSelfRatedpcs 
demEthnC4_1
demEthnC4_2
demEthnC4_3
demEthnC4_4
demEthnC6_1
demEthnC6_2
demEthnC6_3
demEthnC6_4 
demEthnC6_5 
demEthnC6_6 
labUnempFlag
demLifeSatScore1to7
yFinDstrssFlag
careHrsProvidedWeek
labEmpNyear
	;
#delimit cr // cr stands for carriage return 


cap erase "$dir_data/population_initial_UK_sumstats.xls"
cap erase "$dir_data/population_initial_UK_sumstats.txt"


*******************************************************
*output summary stats for new initial populations     *
*******************************************************
forvalues year=2010/2023 { 
use "$dir_data/population_initial_UK_`year'.dta", clear  

cap drop healthSelfRatedmcs healthSelfRatedpcs
clonevar healthSelfRatedmcs=healthMentalMcs  
clonevar healthSelfRatedpcs=healthPhysicalPcs 


foreach var of local varlist_cat {
recode `var' (0=.) (-9=.) 
cap drop `var'_*
tab `var', gen(`var'_)
 }
 
 
foreach var of local varlist2 {
recode `var' (-9=.) 
 }

foreach var in  careNeedFlag  careHrsFormal  careHrsFromPartner careHrsFromDaughter careHrsFromSon careHrsFromOther ///
careCareFormal wealthTotValue wealthPensValue wealthPrptyValue wealthMortgageDebtValue careHrsProvidedWeek {
recode `var' (.=0)
}
 
order `varlist2' 
qui sum `varlist2' , de 

//save "$dir_data/population_initial_UK_`year'.dta", replace   
outreg2 using "$dir_data/population_initial_UK_sumstats.xls" if statInterviewYear==`year', sum(log) append cttop(`year') keep (`varlist2')
}


cap erase "$dir_data/population_initial_UK_sumstats.txt"
cap erase "$dir_data/population_initial_fs_UK_sumstats.txt"

cap log close            

