/**********************************************************************
*
*	MASTER PROGRAM FOR PREPARING INPUT DATA FOR LABSIM
*	
*	data drawn from the UKLHS, with wealth data (optionally) imputed from the WAS
*
*	AUTH: Justin van de Ven (JV)
*	LAST EDIT: 09/09/2023 (JV)
*
**********************************************************************/

/* Clear memmory */
set more off, perm
clear all
set mem 700m


/********************************************************************************
	Define paths
********************************************************************************/

//////////////////////////////////////////////////////////////
// DATA PROCESSING CODE
//////////////////////////////////////////////////////////////
* working directory
global workingDir "C:\MyFiles\00 CURRENT\03 PROJECTS\Essex\SimPaths\02 PARAMETERISE\STARTING DATA\data"

* ukhls compiler file
global ukhlsDoFile "C:\MyFiles\00 CURRENT\03 PROJECTS\Essex\SimPaths\02 PARAMETERISE\STARTING DATA\progs\01 ukhls compile.do"

* R script
* NOTE THAT WORKING DATA PATH ALSO NEEDS TO BE ADDED TO R SCRIPT
global rScript "C:/MyFiles/00 CURRENT/03 PROJECTS/Essex/SimPaths/02 PARAMETERISE/STARTING DATA/progs/01b Rscript.R"

* was wealth imputation management
global wealthDoFile "C:\MyFiles\00 CURRENT\03 PROJECTS\Essex\SimPaths\02 PARAMETERISE\STARTING DATA\progs\02 ukhls wealth imputation.do"

* prepare was data
global WASDoFile "C:\MyFiles\00 CURRENT\03 PROJECTS\Essex\SimPaths\02 PARAMETERISE\STARTING DATA\progs\02b was wealth data.do"

* social care data
global socareDoFile "C:\MyFiles\00 CURRENT\03 PROJECTS\Essex\SimPaths\02 PARAMETERISE\STARTING DATA\progs\03 ukhls social care.do"

//////////////////////////////////////////////////////////////
// SURVEY DATA REFERENCES
//////////////////////////////////////////////////////////////
* path to UK_Mcrsmltn_w_hrs.dta
global startData "C:\MyFiles\00 CURRENT\03 PROJECTS\Essex\SimPaths\02 PARAMETERISE\STARTING DATA\data\base data\UK_Mcrsmltn_w_hrs.dta"

* folder containing UKHLS (Understanding Society - 6614) data
global UKHLSDir "C:\MyFiles\01 DATA\UK\us2009-21\stata\stata13_se\ukhls"

/* Please list the location of the WAS *.dta files here */
global WASdir "C:\MyFiles\01 DATA\UK\was\wave7\stata\stata13_se"

/* Please list name of round 6 household level file here */
global hhFileWAS6 "was_round_6_hhold_eul_april_2022.dta"

/* Please list name of round 6 person level file here */
global ppFileWAS6 "was_round_6_person_eul_april_2022.dta"

/* Please list name of wave 5 person level file here */
global ppFileWAS5 "was_wave_5_person_eul_oct_2020.dta"

/* Please list name of wave 4 person level file here */
global ppFileWAS4 "was_wave_4_person_eul_oct_2020.dta"

/* Please list name of wave 3 person level file here */
global ppFileWAS3 "was_wave_3_person_eul_oct_2020.dta"

/* Please list name of wave 2 person level file here */
global ppFileWAS2 "was_wave_2_person_eul_nov_2020.dta"

/* Please list name of wave 1 person level file here */
global ppFileWAS1 "was_wave_1_person_eul_nov_2020.dta"

//////////////////////////////////////////////////////////////
// CONTROLS FOR R CODE
// NOTE THAT R NEEDS TO HAVE THE DATA.TABLE PACKAGE ADDED
//////////////////////////////////////////////////////////////
global RtermPath `"C:/Program Files/R/R-4.3.1/bin/x64/Rterm.exe"'
global RtermOptions `"--vanilla"'
global RshellPath "C:/Program Files/R/R-4.3.1/bin/R.exe"

//////////////////////////////////////////////////////////////
// OTHER REFERENCES
//////////////////////////////////////////////////////////////
* define seed to ensure replicatability of results
global seedBase = 3141592
global seedAdjust = 0

* year to impute wealth for
global yearWealth = 2016
global imputeWealthToDataset "population_initial_UK_$yearWealth"

* youngest age for someone to be independent in the model
global ageMaturity 18

* waves reported by ukhls
global UKHLSWaves "a b c d e f g h i j k l"

* waves reporting social care module in ukhls
global socCareWaves "g i k"

* sample window for input data
global firstSimYear = 2010
global lastSimYear = 2017

* inflation adjustments
global cpi_minyear = 2009
global cpi_maxyear = 2019
global matrix cpi = (0.866 \ /// 2009
0.894 \ /// 2010
0.934 \ /// 2011
0.961 \ /// 2012
0.985 \ /// 2013
1.000 \ /// 2014
1.000 \ /// 2015
1.007 \ /// 2016
1.034 \ /// 2017
1.059 \ /// 2018
1.078)

* social care wage rates (real 2015 prices for consistency with inflation figures)
global careWageRate_minyear = 2010
global matrix careHourlyWageRates = (9.04 \ ///	2010
9.12 \ ///	2011
8.91 \ ///	2012
8.71 \ ///	2013
8.58 \ ///	2014
8.79 \ ///	2015
9.13 \ ///	2016
9.22 \ ///	2017
9.37 \ ///	2018
9.61 \ ///	2019
9.97 \ ///	2020
9.92 \ ///	2021
10.01276101)


/********************************************************************************
	Route to worker files
********************************************************************************/
do "$ukhlsDoFile"
do "$wealthDoFile"
do "$socareDoFile"


/********************************************************************************
*
*	END
*
********************************************************************************/

