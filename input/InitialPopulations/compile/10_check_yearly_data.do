***************************************************************************************
* PROJECT:              SimPaths UK: construct initial populations for SimPaths using UKHLS data 
* DO-FILE NAME:         10_check_yearly_data.do
* DESCRIPTION:          This file computes descriptives to compare the initial populations before and after dropping households with missing values 
***************************************************************************************
* COUNTRY:              UK
* DATA:         	    UKHLS EUL version - UKDA-6614-stata [to wave o]
* AUTHORS: 				Daria Popova
* LAST UPDATE:          15 Jan 2026 DP  
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
idhh
idbenefitunit
idperson
idmother
idfather
swv                            
dgn                           
dag                            
dnc02                           
dnc                           
ded                            
deh_c3                        
sedex                         
dlltsd01                    
dhe                            
ydses_c5                       
yplgrs_dv                       
ypnbihs_dv                      
yptciihs_dv                     
dcpyy                           
dcpagdf                     
ynbcpdf_dv                  
der                           
dehm_c3                      
dehf_c3                        
stm                           
dhm                           
scghq2_dv                      
dhh_owned                     
scghq2_dv_miss_flag                
lhw                             
drgn1                            
les_c4                          
dhm_ghq                          
adultchildflag          
dwt                              
observed_earnings_hourly
l1_observed_earnings_hourly     
total_wealth                    
total_pensions
housing_wealth
mortgage_debt
need_socare                      
formal_socare_hrs                 
partner_socare_hrs               
daughter_socare_hrs                  
son_socare_hrs                   
other_socare_hrs                 
formal_socare_cost 
ypncp                           
ypnoab 
dhe_mcs 
dhe_pcs 
dot 
dot01
unemp  
dls
financial_distress
carehoursprovidedweekly
liwwh
;
#delimit cr // cr stands for carriage return

*varlist for categorical variables 
#delimit ;
local varlist_cat 
deh_c3    
dhe                    
ydses_c5 
dehm_c3                    
dehf_c3              
les_c4                     
drgn1   
dot  
dot01     
;
#delimit cr // cr stands for carriage return 


*new varlist with categorical variables outputted by category 
#delimit ;
local varlist2  
idhh
idbenefitunit
idperson
idmother
idfather    
swv                            
dgn                           
dag                            
dnc02                           
dnc                           
ded
sedex              
dlltsd01   
ypncp                           
ypnoab         
yplgrs_dv                       
ypnbihs_dv                      
yptciihs_dv
dcpyy                           
dcpagdf                     
ynbcpdf_dv                  
der                           
stm                                              
dhm                      
lhw                         
dhm_ghq                          
adultchildflag                   
dwt                              
deh_c3_1 
deh_c3_2 
deh_c3_3 
dhe_1 
dhe_2 
dhe_3 
dhe_4 
dhe_5 
ydses_c5_1 
ydses_c5_2 
ydses_c5_3 
ydses_c5_4 
ydses_c5_5 
dehm_c3_1 
dehm_c3_2 
dehm_c3_3 
dehf_c3_1 
dehf_c3_2 
dehf_c3_3 
les_c4_1 
les_c4_2 
les_c4_3 
les_c4_4 
drgn1_1 
drgn1_2 
drgn1_3 
drgn1_4 
drgn1_5 
drgn1_6 
drgn1_7 
drgn1_8 
drgn1_9 
drgn1_10 
drgn1_11 
drgn1_12
potential_earnings_hourly
l1_potential_earnings_hourly  
need_socare                      
formal_socare_hrs                 
partner_socare_hrs               
daughter_socare_hrs                  
son_socare_hrs                   
other_socare_hrs                 
formal_socare_cost
total_wealth                    
total_pensions
housing_wealth
mortgage_debt
dhemcs 
dhepcs 
dot_1
dot_2
dot_3
dot_4
dot01_1
dot01_2
dot01_3
dot01_4 
dot01_5 
dot01_6 
unemp
dls
financial_distress
carehoursprovidedweekly
liwwh
	;
#delimit cr // cr stands for carriage return 



cap erase "$dir_data/population_initial_UK_sumstats.xls"
cap erase "$dir_data/population_initial_fs_UK_sumstats.xls"

cap erase "$dir_data/population_initial_UK_sumstats.txt"
cap erase "$dir_data/population_initial_fs_UK_sumstats.txt"


*******************************************************
*output summary stats for new initial populations     *
*******************************************************
forvalues year=$firstSimYear/$lastSimYear { 
use "$dir_data/population_initial_UK_`year'.dta", clear  

cap drop dhemcs dhepcs
clonevar dhemcs=dhe_mcs  
clonevar dhepcs=dhe_pcs 


foreach var of local varlist_cat {
recode `var' (0=.) (-9=.) 
cap drop `var'_*
tab `var', gen(`var'_)
 }
 
 
foreach var of local varlist2 {
recode `var' (-9=.) 
 }

foreach var in  need_socare  formal_socare_hrs  partner_socare_hrs daughter_socare_hrs son_socare_hrs other_socare_hrs ///
formal_socare_cost total_wealth total_pensions housing_wealth mortgage_debt carehoursprovidedweekly {
recode `var' (.=0)
}
 
order `varlist2' 
qui sum `varlist2' , de 

//save "$dir_data/population_initial_UK_`year'.dta", replace   
outreg2 using "$dir_data/population_initial_UK_sumstats.xls" if stm==`year', sum(log) append cttop(`year') keep (`varlist2')
}


**********************************************************************
*output summary stats for new initial populations before dropping hhs*
**********************************************************************
forvalues year=$firstSimYear/$lastSimYear { 
use "$dir_data/population_initial_fs_UK_`year'.dta", clear  
rename careHoursProvidedWeekly carehoursprovidedweekly

cap gen uk_pop=0                        
cap gen surv_pop=0                        
cap gen adult = dag>=$age_become_responsible 
cap gen child = 1 - adult    

cap drop dhemcs dhepcs
clonevar dhemcs=dhe_mcs  
clonevar dhepcs=dhe_pcs 

foreach var of local varlist_cat {
recode `var' (0=.) (-9=.) 
cap drop `var'_*
tab `var', gen(`var'_)
 }
 
 
foreach var of local varlist2 {
recode `var' (-9=.) 
 }


foreach var in  need_socare  formal_socare_hrs  partner_socare_hrs daughter_socare_hrs son_socare_hrs other_socare_hrs  formal_socare_cost ///
total_wealth total_pensions housing_wealth mortgage_debt carehoursprovidedweekly {
recode `var' (.=0)
}
 
keep `varlist2' 
order `varlist2' 
qui sum `varlist2' , de 

//save "$dir_data/population_initial_fs_UK_`year'.dta", replace   
outreg2 using "$dir_data/population_initial_fs_UK_sumstats.xls" if stm==`year', sum(log) append cttop(`year') keep (`varlist2')
}


cap erase "$dir_data/population_initial_UK_sumstats.txt"
cap erase "$dir_data/population_initial_fs_UK_sumstats.txt"

cap log close            

