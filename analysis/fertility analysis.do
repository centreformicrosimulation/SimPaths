/**************************************************************************************
*
*	PROGRAM TO ANALYSE EMPLOYMENT TRANSATIONS
*
*	Last version:  Justin van de Ven, 06 May 2025
*	First version: Justin van de Ven, 06 May 2025
*
**************************************************************************************/

clear all
global moddir = "C:\Justin\dev\CeMPASimPaths\output\20251029155614\csv"
global outdir = "C:\Justin\analysis\data\"
cd "$outdir"


/**************************************************************************************
*	start
**************************************************************************************/
global year_start = 2015
global year_end = 2026


/**************************************************************************************
*	load data
**************************************************************************************/
import delimited using "$moddir/BenefitUnit.csv", clear
rename *, l
rename id_benefitunit idbenefitunit
gsort idbenefitunit time
save "$outdir/temp0", replace
import delimited using "$moddir/Person.csv", clear
rename *, l
rename id_person idperson
rename socialcareprovision socialcareprovision_p
gsort idbenefitunit time idperson
merge m:1 idbenefitunit time using temp0
gsort time idbenefitunit idperson
gen refbenefitunit = 0
replace refbenefitunit = 1 if (idbenefitunit != idbenefitunit[_n-1])

destring hoursworkedweekly, replace force
recode hoursworkedweekly (missing=0)
gen idNotEmployedAdult = (hoursworkedweekly<0.1 & dag>17)

gen led = (deh_c3=="Low")
gen med = (deh_c3=="Medium")
gen hed = (deh_c3=="High")

gen male = (dgn=="Male")

gen idbaby = (dag==0)
bys time idbenefitunit: egen baby = sum(idbaby)

save "$outdir/temp1", replace


/**************************************************************************************
*	analysis
**************************************************************************************/
use "$outdir/temp1", clear
global year_ref = $year_start-1

// block 1 statistics
gen emp = (hoursworkedweekly>0.5)
gen emp_to_nemp = 0
gen nemp_to_emp = 0
gsort idperson time
replace emp_to_nemp = 1 if (idperson[_n-1]==idperson & time[_n-1]+1==time & emp[_n-1]==1 & emp==0)
replace nemp_to_emp = 1 if (idperson[_n-1]==idperson & time[_n-1]+1==time & emp[_n-1]==0 & emp==1)
order time idperson hoursworkedweekly emp emp_to_nemp
matrix store1 = J($year_end-$year_ref,1,.)
forvalues yy = $year_start/$year_end {
	qui{
		sum baby if (time==`yy' & dag>17 & dag<65), mean
		mat store1[`yy'-${year_ref},1] = r(mean)
	}
}
matlist store1
