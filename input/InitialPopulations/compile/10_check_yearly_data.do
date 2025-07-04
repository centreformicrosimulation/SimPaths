***************************************************************************************
* PROJECT:              ESPON: construct initial populations for SimPaths using UKHLS data 
* DO-FILE NAME:         10_check_yearly_data.do
* DESCRIPTION:          This file computes descriptives to compare the initial populations before and after dropping households with missing values 
***************************************************************************************
* COUNTRY:              UK
* DATA:         	    UKHLS EUL version - UKDA-6614-stata [to wave n]
* AUTHORS: 				Daria Popova
* LAST UPDATE:          30 June 2025 DP 
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
idpartner
idmother
idfather
pno                          
swv                            
dgn                           
dag                            
dcpst                          
dnc02                           
dnc                           
ded                            
deh_c3                        
sedex                         
les_c3                        
dlltsd    
dlltsd01                    
dhe                            
ydses_c5                       
yplgrs_dv                       
ypnbihs_dv                      
yptciihs_dv                     
dhhtp_c4                       
ssscp                        
dcpen                        
dcpyy                           
dcpex                     
dcpagdf                     
ynbcpdf_dv                  
der                           
sedag                        
sprfm                         
dagsp                     
dehsp_c3                     
dhesp                          
lessp_c3                     
dehm_c3                      
dehf_c3                        
stm                           
lesdf_c4                      
ppno                         
dhm                           
scghq2_dv                      
dhh_owned                     
scghq2_dv_miss_flag                
lhw                             
drgn1                            
dct                             
dwt_sampling           
les_c4                          
dhm_ghq                          
lessp_c4                         
adultchildflag          
multiplier                      
dwt                              
potential_earnings_hourly
l1_potential_earnings_hourly     
liquid_wealth                    
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
;
#delimit cr // cr stands for carriage return

*varlist for categorical variables 
#delimit ;
local varlist_cat 
dcpst              
deh_c3    
les_c3
dhe                    
ydses_c5 
dhhtp_c4                    
dehsp_c3                     
dhesp 
lessp_c3                     
dehm_c3                    
dehf_c3              
lesdf_c4                       
les_c4                     
lessp_c4          
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
idpartner
idmother
idfather    
pno                       
swv                            
dgn                           
dag                            
dcpst                          
dnc02                           
dnc                           
ded
sedex              
dlltsd  
dlltsd01                       
ypncp                           
ypnoab         
yplgrs_dv                       
ypnbihs_dv                      
yptciihs_dv
ssscp                        
dcpen                        
dcpyy                           
dcpex                     
dcpagdf                     
ynbcpdf_dv                  
der                           
sedag                        
sprfm                         
dagsp                         
stm                                              
dhm                      
lhw                         
dct                             
dwt_sampling                    
dhm_ghq                          
adultchildflag                   
multiplier                      
dwt                              
dcpst_1 
dcpst_2 
dcpst_3 
deh_c3_1 
deh_c3_2 
deh_c3_3 
les_c3_1 
les_c3_2 
les_c3_3 
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
dhhtp_c4_1 
dhhtp_c4_2 
dhhtp_c4_3 
dhhtp_c4_4 
dehsp_c3_1 
dehsp_c3_2 
dehsp_c3_3 
dhesp_1 
dhesp_2 
dhesp_3 
dhesp_4 
dhesp_5 
lessp_c3_1 
lessp_c3_2
lessp_c3_3 
dehm_c3_1 
dehm_c3_2 
dehm_c3_3 
dehf_c3_1 
dehf_c3_2 
dehf_c3_3 
lesdf_c4_1 
lesdf_c4_2 
lesdf_c4_3 
lesdf_c4_4 
les_c4_1 
les_c4_2 
les_c4_3 
les_c4_4 
lessp_c4_1
lessp_c4_2 
lessp_c4_3 
lessp_c4_4 
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
liquid_wealth
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
	;
#delimit cr // cr stands for carriage return 


cap erase "$dir_data/population_initial_UK_orig_sumstats.xls"
cap erase "$dir_data/population_initial_UK_sumstats.xls"
cap erase "$dir_data/population_initial_fs_UK_sumstats.xls"

cap erase "$dir_data/population_initial_UK_orig_sumstats.txt"
cap erase "$dir_data/population_initial_UK_sumstats.txt"
cap erase "$dir_data/population_initial_fs_UK_sumstats.txt"

/*******************************************************
*output summary stats for orignal initial populations *
*******************************************************
forvalues year=2010/2017 {
insheet using "${dir_ipop_orig}/population_initial_UK_`year'.csv", clear 
save "$dir_data/population_initial_UK_`year'_orig.dta", replace 

gen adult = dag>=$age_become_responsible 
gen child = 1 - adult
gen dehmf_c3 = 0
gen dhe_mcs = 0 
gen dhe_pcs = 0 
gen dot = 0  
gen unemp = 0  


foreach var of local varlist_cat {
recode `var' (0=.) (-9=.) 
cap drop `var'_*
tab `var', gen(`var'_)
 }


foreach var of local varlist2 {
recode `var' (-9=.) 
 }
 
foreach var in  need_socare  formal_socare_hrs  partner_socare_hrs daughter_socare_hrs son_socare_hrs other_socare_hrs  formal_socare_cost ///
liquid_wealth {
recode `var' (.=0)
}
 

order `varlist2' 
qui sum `varlist2' , de 
save "$dir_data/population_initial_UK_`year'_orig.dta", replace
outreg2 using "$dir_data/population_initial_UK_orig_sumstats.xls" if stm==`year', sum(log) append cttop(`year') keep (`varlist2')

}
*/

*******************************************************
*output summary stats for new initial populations     *
*******************************************************
forvalues year=2010/2023 { 
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

foreach var in  need_socare  formal_socare_hrs  partner_socare_hrs daughter_socare_hrs son_socare_hrs other_socare_hrs  formal_socare_cost liquid_wealth {
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
forvalues year=2010/2023 { 
use "$dir_data/population_initial_fs_UK_`year'.dta", clear  


cap gen dwt_sampling =0
cap gen uk_pop=0                        
cap gen surv_pop=0                        
cap gen multiplier=0                     
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
liquid_wealth {
recode `var' (.=0)
}
 
keep `varlist2' 
order `varlist2' 
qui sum `varlist2' , de 

//save "$dir_data/population_initial_fs_UK_`year'.dta", replace   
outreg2 using "$dir_data/population_initial_fs_UK_sumstats.xls" if stm==`year', sum(log) append cttop(`year') keep (`varlist2')
}



cap erase "$dir_data/population_initial_UK_orig_sumstats.txt"
cap erase "$dir_data/population_initial_UK_sumstats.txt"
cap erase "$dir_data/population_initial_fs_UK_sumstats.txt"

cap log close            
 
/*  
*************************************************************
*clean up new initial populations - keep only required vars * 
*************************************************************
forvalues year=2010/2023 {
insheet using "$dir_data/population_initial_UK_`year'.csv", clear  

	*limit saved variables
	keep idhh idbenefitunit idperson idpartner idmother idfather pno swv dgn dag dcpst dnc02 dnc ded deh_c3 sedex jbstat les_c3 dlltsd dlltsd01 dhe ydses_c5 ///
	yplgrs_dv ypnbihs_dv yptciihs_dv dhhtp_c4 ssscp dcpen dcpyy dcpex dcpagdf ynbcpdf_dv der sedag sprfm dagsp dehsp_c3 dhesp lessp_c3 dehm_c3 dehf_c3 ///
	stm lesdf_c4 ppno dhm scghq2_dv dhh_owned lhw drgn1 dct dwt_sampling les_c4 dhm_ghq lessp_c4 adultchildflag multiplier dwt ///
	potential_earnings_hourly l1_potential_earnings_hourly liquid_wealth need_socare formal_socare_hrs partner_socare_hrs daughter_socare_hrs son_socare_hrs other_socare_hrs formal_socare_cost ///
	ypncp ypnoab aidhrs carewho dhe_mcs dhe_pcs dot dot01 unemp dhe_mcssp dhe_pcssp 
	
	order idhh idbenefitunit idperson idpartner idmother idfather pno swv dgn dag dcpst dnc02 dnc ded deh_c3 sedex jbstat les_c3 dlltsd dlltsd01 dhe ydses_c5 yplgrs_dv ypnbihs_dv yptciihs_dv dhhtp_c4 ssscp dcpen ///
	dcpyy dcpex dcpagdf ynbcpdf_dv der sedag sprfm dagsp dehsp_c3 dhesp lessp_c3 dehm_c3 dehf_c3 stm lesdf_c4 ppno dhm scghq2_dv dhh_owned lhw drgn1 dct dwt_sampling les_c4 dhm_ghq lessp_c4 adultchildflag ///
	multiplier dwt potential_earnings_hourly l1_potential_earnings_hourly liquid_wealth need_socare formal_socare_hrs partner_socare_hrs daughter_socare_hrs son_socare_hrs other_socare_hrs formal_socare_cost ///
	ypncp ypnoab aidhrs carewho dhe_mcs dhe_pcs dhe_mcssp dhe_pcssp dot dot01 unemp 
	
	recode idhh idbenefitunit idperson idpartner idmother idfather pno swv dgn dag dcpst dnc02 dnc ded deh_c3 sedex jbstat les_c3 dlltsd dlltsd01 dhe ydses_c5 yplgrs_dv ypnbihs_dv yptciihs_dv dhhtp_c4 ssscp ///
	dcpen dcpyy dcpex dcpagdf ynbcpdf_dv der sedag sprfm dagsp dehsp_c3 dhesp lessp_c3 dehm_c3 dehf_c3 stm lesdf_c4 ppno dhm scghq2_dv dhh_owned lhw drgn1 dct dwt_sampling les_c4 dhm_ghq lessp_c4 ///
	adultchildflag multiplier dwt potential_earnings_hourly l1_potential_earnings_hourly liquid_wealth need_socare formal_socare_hrs partner_socare_hrs daughter_socare_hrs son_socare_hrs other_socare_hrs ///
	formal_socare_cost ypncp ypnoab aidhrs carewho dhe_mcs dhe_pcs dhe_mcssp dhe_pcssp dot dot01 unemp  (missing=-9)
	
	gsort idhh idbenefitunit idperson
	save "$dir_data/population_initial_UK_`year'.dta", replace
	export delimited using "$dir_data/population_initial_UK_`year'.csv", nolabel replace
}
*/

