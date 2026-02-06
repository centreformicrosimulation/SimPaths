****************************************************************************************************
* PROJECT:              UKMOD update: construct a UKMOD-UKHLS database from UKHLS dataset
* DO-FILE NAME:         00_Master.do
* DESCRIPTION:          Main do-file governing the creation of employment history data
*                       which is required for the generation of some UKMOD variables 
*
* PURPOSE:              The code reconstructs each respondent’s employment history month by month 
*                       by combining information from the UKHLS and the older BHPS surveys.
*                       The scripts rebuild employment history using respondents’ reported current activity and interview dates across waves.
*                       The process links together:
*                       - the timing of interviews,
*                       - reported employment and non-employment spells, and
*                       - transitions between BHPS and UKHLS for legacy sample members.
*                       The result is a dataset showing, for every person, whether they were employed in each month 
*                       since Jan 2007. 
*        
*
*                       The final output liwwh — the total number of months a person has been employed since January 2007.
*                       This provides a consistent measure of accumulated work experience over the observation window, 
*                       suitable for use in UKMOD and labour-supply model.
*
*
* NOTES:                Potentially the timeline could be extended backwards using data from 
*                       the UKHLS Lifetime Employment Status History modules in Waves 1 and 5 
*                       which collected retrospective work histories from subsets of respondents. 
*                       A sample scripts by Liam Wright are available but outdated: 
*                       https://www.understandingsociety.ac.uk/documentation/mainstage/syntax/user-deposited-syntax/working-life-histories/
***********************************************************************************************************
* UKHLS VERSION:        UKHLS EUL version - UKDA-6614-stata [to wave o]
* AUTHORS:              Nick Buck, Ricky Kanabar, Patryk Bronka, Daria Popova 
* LAST REVISION:        15 Jan 2026 DP 
***********************************************************************************************************

************************************************************************
* Run sub-scripts
************************************************************************
cd "${dir_data_emphist}"
/* */
* 01_Intdate.do: set up cross-wave file of interview dates 
* ==> needed to link previous wave interview date to each respondent*/
do "${dir_do_emphist}/01_Intdate.do"


* 02_Lwintdat.do: create files of previous wave interview dates for waves c-n
* ==> helps align spells across waves for UKHLS respondents
do "${dir_do_emphist}/02_Lwintdat.do"

* 03_Bhps_lintdate.do: get last interview date under BHPS
* ==> also creates previous wave interview dates for wave b
do "${dir_do_emphist}/03_Bhps_lintdate.do"

* 04_Sp0_1_2a.do: create wave-specific spell files for everyone
* ==> each spell = period of employment/non-employment, continuous across months
* ==> Note: This does not pick up all possible variables from employment history, could be modified to pick up additional ones 
do "${dir_do_emphist}/04_Sp0_1_2a.do"

* 05_Newentrant1.do: create spell file based on wave of entry (start of first job)
* ==> captures employment history for new entrants; fills gaps where possible
do "${dir_do_emphist}/05_Newentrant1.do"

* 06_Aspells1.do: create file containing all spells across waves
* ==> obtains spell start date from previous spell end date
* ==> fills some missing dates; drops cases with insufficient data
do "${dir_do_emphist}/06_Aspells1.do"

* 07_Empcal1a.do: create monthly employment calendar ==> used to calculate total months in employment per individual
do "${dir_do_emphist}/07_Empcal1a.do"


