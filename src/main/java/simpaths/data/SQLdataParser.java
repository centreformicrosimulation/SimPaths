package simpaths.data;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.Set;

import simpaths.model.enums.Country;
import simpaths.model.enums.Region;

public class SQLdataParser {

	public static void createDatabaseForPopulationInitialisationByYearFromCSV(Country country, String initialInputFilename, int startYear, int endYear, Connection conn) {

		//Construct tables for Simulated Persons & Households (initial population)
		for (int year = startYear; year <= endYear; year++) {
			SQLdataParser.parse(Parameters.getInputDirectoryInitialPopulations() + initialInputFilename + "_" + year + ".csv", initialInputFilename, conn, country, year);
		}
	}

	//CREATE PERSON AND HOUSEHOLD TABLES IN INPUT DATABASE BY USING SQL COMMANDS ON EUROMOD POPULATION DATA
	//donorTables set to true means that this method is being used to create donor population tables, 
	//as opposed to the initial population for simulation
	private static void parse(String inputFileLocation, String inputFileName, Connection conn, Country country, int startyear) {

		//Set name of tables
		String personTable = "person_" + country + "_" + startyear;
		String benefitUnitTable = "benefitUnit_" + country + "_" + startyear;
		String householdTable = "household_" + country + "_" + startyear;

		//Ensure no duplicate column names
		Set<String> inputPersonColumnNamesSet = new LinkedHashSet<String>(Arrays.asList(Parameters.PERSON_VARIABLES_INITIAL));
		Set<String> inputBenefitUnitColumnNamesSet = new LinkedHashSet<String>(Arrays.asList(Parameters.BENEFIT_UNIT_VARIABLES_INITIAL));
		Set<String> inputHouseholdColumnNameSet = new LinkedHashSet<>(Arrays.asList(Parameters.HOUSEHOLD_VARIABLES_INITIAL));

		Statement stat = null;
		try {
			stat = conn.createStatement();
			stat.execute(
				//SQL statements creating database tables go here
				//Refresh table
				"DROP TABLE IF EXISTS " + inputFileName + ";"
				+ "CREATE TABLE " + inputFileName + " AS SELECT * FROM CSVREAD(\'" + inputFileLocation + "\');"
				+ "DROP TABLE IF EXISTS " + personTable + ";"
				+ "CREATE TABLE " + personTable + " AS (SELECT " + stringAppender(inputPersonColumnNamesSet) + " FROM " + inputFileName + ");"
				//Add id column
				+ "ALTER TABLE " + personTable + " ALTER COLUMN idperson RENAME TO id;"
				//Add rest of PanelEntityKey
				+ "ALTER TABLE " + personTable + " ADD COLUMN simulation_time INT DEFAULT " + startyear + ";"
				+ "ALTER TABLE " + personTable + " ADD COLUMN simulation_run INT DEFAULT 0;"

				//Rename EUROMOD variables
				//Age of partner
				+ "ALTER TABLE " + personTable + " ALTER COLUMN dagsp RENAME TO age_partner;"

				//Reclassify EUROMOD variables - may need to change data structure type otherwise SQL conversion error, so create new column of the correct type, map data from old column and drop old column
				//Country
				+ "ALTER TABLE " + personTable + " ADD country VARCHAR_IGNORECASE;"
				+ "UPDATE " + personTable + " SET country = \'" + country + "\' WHERE dct = " + country.getEuromodCountryCode() + ";"
				+ "ALTER TABLE " + personTable + " DROP COLUMN dct;"

				//Health
				+ "ALTER TABLE " + personTable + " ADD health VARCHAR_IGNORECASE;"
				+ "UPDATE " + personTable + " SET health = 'Poor' WHERE dhe = 1;"
				+ "UPDATE " + personTable + " SET health = 'Fair' WHERE dhe = 2;"
				+ "UPDATE " + personTable + " SET health = 'Good' WHERE dhe = 3;"
				+ "UPDATE " + personTable + " SET health = 'VeryGood' WHERE dhe = 4;"
				+ "UPDATE " + personTable + " SET health = 'Excellent' WHERE dhe = 5;"
				+ "ALTER TABLE " + personTable + " DROP COLUMN dhe;"
				+ "ALTER TABLE " + personTable + " ALTER COLUMN health RENAME TO dhe;"

				+ "ALTER TABLE " + personTable + " ADD healthsp VARCHAR_IGNORECASE;"
				+ "UPDATE " + personTable + " SET healthsp = 'Poor' WHERE dhesp = 1;"
				+ "UPDATE " + personTable + " SET healthsp = 'Fair' WHERE dhesp = 2;"
				+ "UPDATE " + personTable + " SET healthsp = 'Good' WHERE dhesp = 3;"
				+ "UPDATE " + personTable + " SET healthsp = 'VeryGood' WHERE dhesp = 4;"
				+ "UPDATE " + personTable + " SET healthsp = 'Excellent' WHERE dhesp = 5;"
				+ "UPDATE " + personTable + " SET healthsp = NULL WHERE dhesp = 0;"
				+ "ALTER TABLE " + personTable + " DROP COLUMN dhesp;"
				+ "ALTER TABLE " + personTable + " ALTER COLUMN healthsp RENAME TO dhesp;"

				//Education
				+ "ALTER TABLE " + personTable + " ADD education VARCHAR_IGNORECASE;"
				+ "UPDATE " + personTable + " SET education = 'Low' WHERE deh_c3 = 3;"
				+ "UPDATE " + personTable + " SET education = 'Medium' WHERE deh_c3 = 2;"
				+ "UPDATE " + personTable + " SET education = 'High' WHERE deh_c3 = 1;"
				//Note: Have to consider missing values as children don't have a level of education before they leave school
				+ "UPDATE " + personTable + " SET education = 'Low' WHERE deh_c3 = -9;"
				+ "ALTER TABLE " + personTable + " DROP COLUMN deh_c3;"
				+ "ALTER TABLE " + personTable + " ALTER COLUMN education RENAME TO deh_c3;"

				//Education mother
				+ "ALTER TABLE " + personTable + " ADD education_mother VARCHAR_IGNORECASE;"
				+ "UPDATE " + personTable + " SET education_mother = 'Low' WHERE dehm_c3 = 3;"
				+ "UPDATE " + personTable + " SET education_mother = 'Medium' WHERE dehm_c3 = 2;"
				+ "UPDATE " + personTable + " SET education_mother = 'High' WHERE dehm_c3 = 1;"
				+ "ALTER TABLE " + personTable + " DROP COLUMN dehm_c3;"
				+ "ALTER TABLE " + personTable + " ALTER COLUMN education_mother RENAME TO dehm_c3;"

				//Education father
				+ "ALTER TABLE " + personTable + " ADD education_father VARCHAR_IGNORECASE;"
				+ "UPDATE " + personTable + " SET education_father = 'Low' WHERE dehf_c3 = 3;"
				+ "UPDATE " + personTable + " SET education_father = 'Medium' WHERE dehf_c3 = 2;"
				+ "UPDATE " + personTable + " SET education_father = 'High' WHERE dehf_c3 = 1;"
				+ "ALTER TABLE " + personTable + " DROP COLUMN dehf_c3;"
				+ "ALTER TABLE " + personTable + " ALTER COLUMN education_father RENAME TO dehf_c3;"

				//Education partner
				+ "ALTER TABLE " + personTable + " ADD education_partner VARCHAR_IGNORECASE;"
				+ "UPDATE " + personTable + " SET education_partner = 'Low' WHERE dehsp_c3 = 3;"
				+ "UPDATE " + personTable + " SET education_partner = 'Medium' WHERE dehsp_c3 = 2;"
				+ "UPDATE " + personTable + " SET education_partner = 'High' WHERE dehsp_c3 = 1;"
				//Note: Have to consider missing values as for single persons partner's education is undefined
				+ "UPDATE " + personTable + " SET education_partner = null WHERE dehsp_c3 = -9;"
				+ "ALTER TABLE " + personTable + " DROP COLUMN dehsp_c3;"
				+ "ALTER TABLE " + personTable + " ALTER COLUMN education_partner RENAME TO dehsp_c3;"

				//In education dummy (to be used with Indicator enum when defined in Person class)
				+ "ALTER TABLE " + personTable + " ADD education_in VARCHAR_IGNORECASE;"
				+ "UPDATE " + personTable + " SET education_in = 'False' WHERE ded = 0;"
				+ "UPDATE " + personTable + " SET education_in = 'True' WHERE ded = 1;"
				+ "ALTER TABLE " + personTable + " DROP COLUMN ded;"
				+ "ALTER TABLE " + personTable + " ALTER COLUMN education_in RENAME TO ded;"

				//Return to education dummy (to be used with Indicator enum when defined in Person class)
				+ "ALTER TABLE " + personTable + " ADD education_return VARCHAR_IGNORECASE;"
				+ "UPDATE " + personTable + " SET education_return = 'False' WHERE der = 0;"
				+ "UPDATE " + personTable + " SET education_return = 'True' WHERE der = 1;"
				+ "ALTER TABLE " + personTable + " DROP COLUMN der;"
				+ "ALTER TABLE " + personTable + " ALTER COLUMN education_return RENAME TO der;"

				//Gender
				+ "ALTER TABLE " + personTable + " ADD gender VARCHAR_IGNORECASE;"
				+ "UPDATE " + personTable + " SET gender = 'Female' WHERE dgn = 0;"
				+ "UPDATE " + personTable + " SET gender = 'Male' WHERE dgn = 1;"
				+ "ALTER TABLE " + personTable + " DROP COLUMN dgn;"
				+ "ALTER TABLE " + personTable + " ALTER COLUMN gender RENAME TO dgn;"

				//Weights
				+"ALTER TABLE " + personTable + " ALTER COLUMN dwt RENAME TO person_weight;"

				//Labour Market Economic Status
				+ "ALTER TABLE " + personTable + " ADD activity_status VARCHAR_IGNORECASE;"
				+ "UPDATE " + personTable + " SET les_c4 = 3 WHERE les_c4 = 1 AND CAST(potential_earnings_hourly AS FLOAT)<0.01;"
				+ "UPDATE " + personTable + " SET activity_status = 'EmployedOrSelfEmployed' WHERE les_c4 = 1;"
				+ "UPDATE " + personTable + " SET activity_status = 'Student' WHERE les_c4 = 2;"
				+ "UPDATE " + personTable + " SET activity_status = 'NotEmployed' WHERE les_c4 = 3;"
				+ "UPDATE " + personTable + " SET activity_status = 'Retired' WHERE les_c4 = 4;"
				+ "ALTER TABLE " + personTable + " DROP COLUMN les_c4;"
				+ "ALTER TABLE " + personTable + " ALTER COLUMN activity_status RENAME TO les_c4;"

				//Partner's Labour Market Economic Status
				+ "ALTER TABLE " + personTable + " ADD activity_status_partner VARCHAR_IGNORECASE;"
				+ "UPDATE " + personTable + " SET activity_status_partner = 'EmployedOrSelfEmployed' WHERE lessp_c4 = 1;"
				+ "UPDATE " + personTable + " SET activity_status_partner = 'Student' WHERE lessp_c4 = 2;"
				+ "UPDATE " + personTable + " SET activity_status_partner = 'NotEmployed' WHERE lessp_c4 = 3;"
				+ "UPDATE " + personTable + " SET activity_status_partner = 'Retired' WHERE lessp_c4 = 4;"

				//Null values because not everyone has a partner
				+ "UPDATE " + personTable + " SET activity_status_partner = null WHERE lessp_c4 = -9;"
				+ "ALTER TABLE " + personTable + " DROP COLUMN lessp_c4;"
				+ "ALTER TABLE " + personTable + " ALTER COLUMN activity_status_partner RENAME TO lessp_c4;"

				//Own and partner's Labour Market Economic Status
				+ "ALTER TABLE " + personTable + " ADD activity_status_couple VARCHAR_IGNORECASE;"
				+ "UPDATE " + personTable + " SET activity_status_couple = 'BothEmployed' WHERE lesdf_c4 = 1;"
				+ "UPDATE " + personTable + " SET activity_status_couple = 'EmployedSpouseNotEmployed' WHERE lesdf_c4 = 2;"
				+ "UPDATE " + personTable + " SET activity_status_couple = 'NotEmployedSpouseEmployed' WHERE lesdf_c4 = 3;"
				+ "UPDATE " + personTable + " SET activity_status_couple = 'BothNotEmployed' WHERE lesdf_c4 = 4;"

				//Null values because not everyone has a partner
				+ "UPDATE " + personTable + " SET activity_status_couple = null WHERE lesdf_c4 = -9;"
				+ "ALTER TABLE " + personTable + " DROP COLUMN lesdf_c4;"
				+ "ALTER TABLE " + personTable + " ALTER COLUMN activity_status_couple RENAME TO lesdf_c4;"

				//Partnership status
				+ "ALTER TABLE " + personTable + " ADD partnership_status VARCHAR_IGNORECASE;"
				+ "UPDATE " + personTable + " SET partnership_status = 'Partnered' WHERE dcpst = 1;"
				+ "UPDATE " + personTable + " SET partnership_status = 'SingleNeverMarried' WHERE dcpst = 2;"
				+ "UPDATE " + personTable + " SET partnership_status = 'PreviouslyPartnered' WHERE dcpst = 3;"
				+ "ALTER TABLE " + personTable + " DROP COLUMN dcpst;"
				+ "ALTER TABLE " + personTable + " ALTER COLUMN partnership_status RENAME TO dcpst;"

				//Enter partnership dummy (to be used with Indicator enum when defined in Person class)
				+ "ALTER TABLE " + personTable + " ADD partnership_enter VARCHAR_IGNORECASE;"
				+ "UPDATE " + personTable + " SET partnership_enter = 'False' WHERE dcpen = 0;"
				+ "UPDATE " + personTable + " SET partnership_enter = 'True' WHERE dcpen = 1;"
				+ "ALTER TABLE " + personTable + " DROP COLUMN dcpen;"
				+ "ALTER TABLE " + personTable + " ALTER COLUMN partnership_enter RENAME TO dcpen;"

				//Exit partnership dummy (to be used with Indicator enum when defined in Person class)
				+ "ALTER TABLE " + personTable + " ADD partnership_exit VARCHAR_IGNORECASE;"
				+ "UPDATE " + personTable + " SET partnership_exit = 'False' WHERE dcpex = 0;"
				+ "UPDATE " + personTable + " SET partnership_exit = 'True' WHERE dcpex = 1;"
				+ "ALTER TABLE " + personTable + " DROP COLUMN dcpex;"
				+ "ALTER TABLE " + personTable + " ALTER COLUMN partnership_exit RENAME TO dcpex;"

				//DEMOGRAPHIC: Long-term sick or disabled (to be used with Indicator enum when defined in Person class)
				+ "ALTER TABLE " + personTable + " ADD sick_longterm VARCHAR_IGNORECASE;"
				+ "UPDATE " + personTable + " SET sick_longterm = 'False' WHERE dlltsd = 0;"
				+ "UPDATE " + personTable + " SET sick_longterm = 'True' WHERE dlltsd = 1;"
				+ "ALTER TABLE " + personTable + " DROP COLUMN dlltsd;"
				+ "ALTER TABLE " + personTable + " ALTER COLUMN sick_longterm RENAME TO dlltsd;"

				//DEMOGRAPHIC: Need social care (to be used with Indicator enum when defined in Person class)
				+ "ALTER TABLE " + personTable + " ADD need_care VARCHAR_IGNORECASE;"
				+ "UPDATE " + personTable + " SET need_care = 'False' WHERE need_socare = 0;"
				+ "UPDATE " + personTable + " SET need_care = 'True' WHERE need_socare = 1;"
				+ "ALTER TABLE " + personTable + " DROP COLUMN need_socare;"
				+ "ALTER TABLE " + personTable + " ALTER COLUMN need_care RENAME TO need_socare;"

				//SYSTEM: Year left education (to be used with Indicator enum when defined in Person class)
				+ "ALTER TABLE " + personTable + " ADD education_left VARCHAR_IGNORECASE;"
				+ "UPDATE " + personTable + " SET education_left = 'False' WHERE sedex = 0;"
				+ "UPDATE " + personTable + " SET education_left = 'True' WHERE sedex = 1;"
				+ "ALTER TABLE " + personTable + " DROP COLUMN sedex;"
				+ "ALTER TABLE " + personTable + " ALTER COLUMN education_left RENAME TO sedex;" //Getting data conversion error trying to directly change values of sedex

				//SYSTEM: In same-sex partnership (to be used with Indicator enum when defined in Person class)
				+ "ALTER TABLE " + personTable + " ADD partnership_samesex VARCHAR_IGNORECASE;"
				+ "UPDATE " + personTable + " SET partnership_samesex = 'False' WHERE ssscp = 0;"
				+ "UPDATE " + personTable + " SET partnership_samesex = 'True' WHERE ssscp = 1;"
				+ "ALTER TABLE " + personTable + " DROP COLUMN ssscp;"

				//SYSTEM: Women in fertility range (to be used with Indicator enum when defined in Person class)
				+ "ALTER TABLE " + personTable + " ADD women_fertility VARCHAR_IGNORECASE;"
				+ "UPDATE " + personTable + " SET women_fertility = 'False' WHERE sprfm = 0;"
				+ "UPDATE " + personTable + " SET women_fertility = 'True' WHERE sprfm = 1;"
				+ "ALTER TABLE " + personTable + " DROP COLUMN sprfm;"

				//SYSTEM: In educational age range (to be used with Indicator enum when defined in Person class)
				+ "ALTER TABLE " + personTable + " ADD education_inrange VARCHAR_IGNORECASE;"
				+ "UPDATE " + personTable + " SET education_inrange = 'False' WHERE sedag = 0;"
				+ "UPDATE " + personTable + " SET education_inrange = 'True' WHERE sedag = 1;"
				+ "ALTER TABLE " + personTable + " DROP COLUMN sedag;"

				//Adult child flag:
				+ "ALTER TABLE " + personTable + " ADD adult_child VARCHAR_IGNORECASE;"
				+ "UPDATE " + personTable + " SET adult_child = 'False' WHERE adultchildflag = 0;"
				+ "UPDATE " + personTable + " SET adult_child = 'True' WHERE adultchildflag = 1;"
				+ "ALTER TABLE " + personTable + " DROP COLUMN adultchildflag;"
				+ "ALTER TABLE " + personTable + " ALTER COLUMN adult_child RENAME TO adultchildflag;"

				//Homeownership
				+ "ALTER TABLE " + personTable + " ADD dhh_owned_add VARCHAR_IGNORECASE;"
				+ "UPDATE " + personTable + " SET dhh_owned_add = 'False' WHERE dhh_owned = 0;"
				+ "UPDATE " + personTable + " SET dhh_owned_add = 'True' WHERE dhh_owned = 1;"
				+ "ALTER TABLE " + personTable + " DROP COLUMN dhh_owned;"
				+ "ALTER TABLE " + personTable + " ALTER COLUMN dhh_owned_add RENAME TO dhh_owned;"

				//SYSTEM : Year
				+ "ALTER TABLE " + personTable + " ALTER COLUMN stm RENAME TO system_year;"

				//SYSTEM : Data collection wave
				+ "ALTER TABLE " + personTable + " ALTER COLUMN swv RENAME TO system_wave;"

				+ "ALTER TABLE " + personTable + " ALTER COLUMN lhw RENAME TO " + Parameters.HOURS_WORKED_WEEKLY + ";"
				+ "ALTER TABLE " + personTable + " ADD work_sector VARCHAR_IGNORECASE DEFAULT 'Private_Employee';"		//Here we assume by default that people are employed - this is because the MultiKeyMaps holding households have work_sector as a key, and cannot handle null values for work_sector. TODO: Need to check that this assumption is OK.
				+ "UPDATE " + personTable + " SET idpartner = null WHERE idpartner = -9;"
				+ "UPDATE " + personTable + " SET idmother = null WHERE idmother = -9;"
				+ "UPDATE " + personTable + " SET idfather = null WHERE idfather = -9;"

				//Rename idbenefitunit to BU_ID
				+ "ALTER TABLE " + personTable + " ALTER COLUMN idbenefitunit RENAME TO " + Parameters.BENEFIT_UNIT_VARIABLE_NAME + ";"

				//Id of the household is loaded from the input population without any modification as idhh

				//Re-order by id
				+ "SELECT * FROM " + personTable + " ORDER BY id;"
			);

			//BenefitUnit table:
			stat.execute(
				"DROP TABLE IF EXISTS " + benefitUnitTable + ";"
				//Create household table with columns representing EUROMOD variables listed in Parameters class EUROMOD_VARIABLES_HOUSEHOLD set.
				+ "CREATE TABLE " + benefitUnitTable + " AS (SELECT " + stringAppender(inputBenefitUnitColumnNamesSet) + " FROM " + inputFileName + ");"
				+ "ALTER TABLE " + benefitUnitTable + " ADD COLUMN simulation_time INT DEFAULT " + startyear + ";"
				+ "ALTER TABLE " + benefitUnitTable + " ADD COLUMN simulation_run INT DEFAULT 0;"

				+ "ALTER TABLE " + benefitUnitTable + " ADD region VARCHAR_IGNORECASE;"
			);

			stat.execute(
				"DROP TABLE IF EXISTS " + householdTable + ";"
				+ "CREATE TABLE " + householdTable + " AS (SELECT " + stringAppender(inputHouseholdColumnNameSet) + " FROM " + inputFileName + ");"
				+ "ALTER TABLE " + householdTable + " ADD COLUMN simulation_time INT DEFAULT " + startyear + ";"
				+ "ALTER TABLE " + householdTable + " ADD COLUMN simulation_run INT DEFAULT 0;"
				+ "ALTER TABLE " + householdTable + " DROP COLUMN idperson;"
				+ "ALTER TABLE " + householdTable + " ALTER COLUMN idhh RENAME TO id;"
				+ "SELECT * FROM " + householdTable + " ORDER BY id;"
			);

			//Region - See Region class for mapping definitions and sources of info
			Parameters.setCountryRegions(country);
			for(Region region: Parameters.getCountryRegions()) {
				stat.execute(
					"UPDATE " + benefitUnitTable + " SET region = '" + region + "' WHERE drgn1 = " + region.getDrgn1EUROMODvariable() + ";"
				);
			}

			stat.execute(
				"ALTER TABLE " + benefitUnitTable + " DROP COLUMN drgn1;"
				+ "ALTER TABLE " + benefitUnitTable + " DROP COLUMN idfather;"
				+ "ALTER TABLE " + benefitUnitTable + " DROP COLUMN idmother;"
				+ "ALTER TABLE " + benefitUnitTable + " DROP COLUMN idperson;"

				//Rename EUROMOD variables
				+ "ALTER TABLE " + benefitUnitTable + " ALTER COLUMN dwt RENAME TO household_weight;"

				//BenefitUnit composition
				+ "ALTER TABLE " + benefitUnitTable + " ADD household_composition VARCHAR_IGNORECASE;"
				+ "UPDATE " + benefitUnitTable + " SET household_composition = 'CoupleNoChildren' WHERE dhhtp_c4 = 1;"
				+ "UPDATE " + benefitUnitTable + " SET household_composition = 'CoupleChildren' WHERE dhhtp_c4 = 2;"
				+ "UPDATE " + benefitUnitTable + " SET household_composition = 'SingleNoChildren' WHERE dhhtp_c4 = 3;"
				+ "UPDATE " + benefitUnitTable + " SET household_composition = 'SingleChildren' WHERE dhhtp_c4 = 4;"
				+ "ALTER TABLE " + benefitUnitTable + " DROP COLUMN dhhtp_c4;"
				+ "ALTER TABLE " + benefitUnitTable + " ALTER COLUMN household_composition RENAME TO dhhtp_c4;"

				//INCOME: BenefitUnit income - quintiles
				+ "ALTER TABLE " + benefitUnitTable + " ADD household_income_qtiles VARCHAR_IGNORECASE;"
				+ "UPDATE " + benefitUnitTable + " SET household_income_qtiles = 'Q1' WHERE ydses_c5 = 1;"
				+ "UPDATE " + benefitUnitTable + " SET household_income_qtiles = 'Q2' WHERE ydses_c5 = 2;"
				+ "UPDATE " + benefitUnitTable + " SET household_income_qtiles = 'Q3' WHERE ydses_c5 = 3;"
				+ "UPDATE " + benefitUnitTable + " SET household_income_qtiles = 'Q4' WHERE ydses_c5 = 4;"
				+ "UPDATE " + benefitUnitTable + " SET household_income_qtiles = 'Q5' WHERE ydses_c5 = 5;"
				+ "ALTER TABLE " + benefitUnitTable + " DROP COLUMN ydses_c5;"
				+ "ALTER TABLE " + benefitUnitTable + " ALTER COLUMN household_income_qtiles RENAME TO ydses_c5;"

				//Homeownership
				+ "ALTER TABLE " + benefitUnitTable + " ADD dhh_owned_add VARCHAR_IGNORECASE;"
				+ "UPDATE " + benefitUnitTable + " SET dhh_owned_add = 'False' WHERE dhh_owned = 0;"
				+ "UPDATE " + benefitUnitTable + " SET dhh_owned_add = 'True' WHERE dhh_owned = 1;"
				+ "ALTER TABLE " + benefitUnitTable + " DROP COLUMN dhh_owned;"
				+ "ALTER TABLE " + benefitUnitTable + " ALTER COLUMN dhh_owned_add RENAME TO dhh_owned;"

				//WEALTH
				+ "UPDATE " + benefitUnitTable + " SET liquid_wealth = 0.0 WHERE liquid_wealth = -9.0;"

				//Rename id column
				+ "ALTER TABLE " + benefitUnitTable + " ALTER COLUMN idbenefitunit RENAME TO id;"

				//Re-order by id
				+ "SELECT * FROM " + benefitUnitTable + " ORDER BY id;"
			);


			//Remove duplicate rows in household tables (as they are derived from persons, there is one row per person, so for households with N people, there would be N rows with same data)
			stat.execute(
				"CREATE TABLE NEW AS SELECT DISTINCT * FROM " + benefitUnitTable + " ORDER BY ID;"
				+ "DROP TABLE IF EXISTS " + benefitUnitTable + ";"
				+ "ALTER TABLE NEW RENAME TO " + benefitUnitTable + ";"
			);

			stat.execute(
					"CREATE TABLE NEW AS SELECT DISTINCT * FROM " + householdTable + " ORDER BY ID;"
				+ "DROP TABLE IF EXISTS " + householdTable + ";"
				+ "ALTER TABLE NEW RENAME TO " + householdTable + ";"
			);

			stat.execute("DROP TABLE IF EXISTS " + inputFileName + ";");

		} catch(Exception e){
		//	 throw new IllegalArgumentException("SQL Exception thrown!" + e.getMessage());
			 e.printStackTrace();
		}
		finally {
			try {
				if(stat != null)
					stat.close();
			} catch (SQLException e) {

				e.printStackTrace();
			}
		}
	}

	public static String stringAppender(Collection<String> strings) {
		Iterator<String> iter = strings.iterator();
		StringBuilder sb = new StringBuilder();
		while (iter.hasNext()) {
			sb
			.append(iter.next())
			;
			if (iter.hasNext())
			sb.append(",");
		}
		return sb.toString();
	}

}