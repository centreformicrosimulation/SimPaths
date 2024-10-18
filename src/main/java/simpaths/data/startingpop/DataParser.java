package simpaths.data.startingpop;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.Set;

import simpaths.data.FormattedDialogBox;
import simpaths.data.Parameters;
import simpaths.model.enums.Country;
import simpaths.model.enums.Region;

import javax.swing.*;

public class DataParser {

	public static void createDatabaseForPopulationInitialisationByYearFromCSV(Country country, String initialInputFilename, int startYear, int endYear, Connection conn) {

		//Initialise repository table for country-year-population size combinations
		initialiseRepository(conn, startYear);

		//Construct tables for Simulated Persons & Households (initial population)
		for (int year = startYear; year <= endYear; year++) {
			DataParser.parse(Parameters.getInputDirectoryInitialPopulations() + initialInputFilename + "_" + year + ".csv", initialInputFilename, conn, country, year);
		}
	}

	private static void initialiseRepository(Connection conn, int startYear) {

		Statement stat = null;
		try {
			stat = conn.createStatement();
			stat.execute( "DROP TABLE IF EXISTS processed CASCADE;");
			stat.execute( "CREATE TABLE processed (ID BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT, COUNTRY VARCHAR_IGNORECASE DEFAULT 'UK', START_YEAR INT DEFAULT " + startYear + ", POP_SIZE INT DEFAULT 0);");
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

		//FLAG to switch off identification of primary and foreign keys to improve read performance
		//setting this to false reduces load times of survey data from 5.5 to 4 minutes
		//the improvement in performance is sacrificed to benefit from checks of internal consistency of the data
		final boolean PROCESS_KEY_IDENTIFICATION = true;

		Statement stat = null;
		try {
			stat = conn.createStatement();
			stat.execute(
				//SQL statements creating database tables go here
				//Refresh table
				"DROP TABLE IF EXISTS " + inputFileName + " CASCADE;"
				+ "CREATE TABLE " + inputFileName + " AS SELECT * FROM CSVREAD(\'" + inputFileLocation + "\');"
				+ "DROP TABLE IF EXISTS " + personTable + " CASCADE;"
				+ "CREATE TABLE " + personTable + " AS (SELECT " + stringAppender(inputPersonColumnNamesSet) + " FROM " + inputFileName + ");"

				//Add panel entity key
				+ "ALTER TABLE " + personTable + " ALTER COLUMN idperson RENAME TO id;"
				+ "ALTER TABLE " + personTable + " ALTER COLUMN id BIGINT;"
				+ "ALTER TABLE " + personTable + " ADD COLUMN simulation_time INT DEFAULT " + startyear + ";"
				+ "ALTER TABLE " + personTable + " ADD COLUMN simulation_run INT DEFAULT 0;"
				+ "ALTER TABLE " + personTable + " ADD COLUMN working_id INT DEFAULT 0;"

				//Health
				+ "ALTER TABLE " + personTable + " ADD health VARCHAR_IGNORECASE;"
				+ "UPDATE " + personTable + " SET health = 'Poor' WHERE dhe = 1;"
				+ "UPDATE " + personTable + " SET health = 'Fair' WHERE dhe = 2;"
				+ "UPDATE " + personTable + " SET health = 'Good' WHERE dhe = 3;"
				+ "UPDATE " + personTable + " SET health = 'VeryGood' WHERE dhe = 4;"
				+ "UPDATE " + personTable + " SET health = 'Excellent' WHERE dhe = 5;"
				+ "ALTER TABLE " + personTable + " DROP COLUMN dhe;"
				+ "ALTER TABLE " + personTable + " ALTER COLUMN health RENAME TO dhe;"

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

				//Social care
				+ "ALTER TABLE " + personTable + " ADD socare_provided_to VARCHAR_IGNORECASE;"
				+ "UPDATE " + personTable + " SET socare_provided_to = 'None' WHERE careWho = 0;"
				+ "UPDATE " + personTable + " SET socare_provided_to = 'OnlyPartner' WHERE careWho = 1;"
				+ "UPDATE " + personTable + " SET socare_provided_to = 'PartnerAndOther' WHERE careWho = 2;"
				+ "UPDATE " + personTable + " SET socare_provided_to = 'OnlyOther' WHERE careWho = 3;"
				+ "ALTER TABLE " + personTable + " DROP COLUMN careWho;"
				+ "ALTER TABLE " + personTable + " ALTER COLUMN aidhrs RENAME TO socare_provided_hrs;"

				//SYSTEM : Year
				+ "ALTER TABLE " + personTable + " ALTER COLUMN stm RENAME TO system_year;"

				//SYSTEM : Data collection wave
				+ "ALTER TABLE " + personTable + " ALTER COLUMN swv RENAME TO system_wave;"

				+ "ALTER TABLE " + personTable + " ALTER COLUMN lhw RENAME TO " + Parameters.HOURS_WORKED_WEEKLY + ";"
				+ "ALTER TABLE " + personTable + " ADD work_sector VARCHAR_IGNORECASE DEFAULT 'Private_Employee';"		//Here we assume by default that people are employed - this is because the MultiKeyMaps holding households have work_sector as a key, and cannot handle null values for work_sector. TODO: Need to check that this assumption is OK.
				+ "UPDATE " + personTable + " SET idmother = null WHERE idmother = -9;"
				+ "UPDATE " + personTable + " SET idfather = null WHERE idfather = -9;"

				//Rename idbenefitunit to BU_ID
				+ "ALTER TABLE " + personTable + " ALTER COLUMN idbenefitunit RENAME TO buid;"
				+ "ALTER TABLE " + personTable + " ADD COLUMN butime INT DEFAULT " + startyear + ";"
				+ "ALTER TABLE " + personTable + " ADD COLUMN burun INT DEFAULT 0;"
				+ "ALTER TABLE " + personTable + " ADD COLUMN prid INT DEFAULT 0;"
				+ "ALTER TABLE " + personTable + " ALTER COLUMN idhh RENAME TO idhousehold;"

				//Re-order by id
				+ "SELECT * FROM " + personTable + " ORDER BY id;"
			);

			if (PROCESS_KEY_IDENTIFICATION) {
				stat.execute(
						"ALTER TABLE " + personTable + " ALTER COLUMN id BIGINT NOT NULL;"
							+ "ALTER TABLE " + personTable + " ALTER COLUMN simulation_time INT NOT NULL;"
							+ "ALTER TABLE " + personTable + " ALTER COLUMN simulation_run INT NOT NULL;"
							+ "ALTER TABLE " + personTable + " ALTER COLUMN working_id INT NOT NULL;"
							+ "ALTER TABLE " + personTable + " ADD PRIMARY KEY (id, simulation_time, simulation_run, working_id);"
				);
			}

			// CREATE BENEFITUNIT TABLE
			stat.execute(
				"DROP TABLE IF EXISTS " + benefitUnitTable + " CASCADE;"
				+ "CREATE TABLE " + benefitUnitTable + " AS (SELECT " + stringAppender(inputBenefitUnitColumnNamesSet) + " FROM " + inputFileName + ");"
				+ "ALTER TABLE " + benefitUnitTable + " ALTER COLUMN idhh RENAME TO hhid;"
				+ "ALTER TABLE " + benefitUnitTable + " ADD COLUMN hhtime INT DEFAULT " + startyear + ";"
				+ "ALTER TABLE " + benefitUnitTable + " ADD COLUMN hhrun INT DEFAULT 0;"
				+ "ALTER TABLE " + benefitUnitTable + " ADD COLUMN prid INT DEFAULT 0;"
				+ "ALTER TABLE " + benefitUnitTable + " ADD region VARCHAR_IGNORECASE;"
			);

			//Region - See Region class for mapping definitions and sources of info
			Parameters.setCountryRegions(country);
			for(Region region: Parameters.getCountryRegions()) {
				stat.execute(
					"UPDATE " + benefitUnitTable + " SET region = '" + region + "' WHERE drgn1 = " + region.getValue() + ";"
				);
			}

			stat.execute(
				"ALTER TABLE " + benefitUnitTable + " DROP COLUMN drgn1;"

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
				+ "UPDATE " + benefitUnitTable + " SET tot_pen = 0.0 WHERE tot_pen = -9.0;"
				+ "UPDATE " + benefitUnitTable + " SET nvmhome = 0.0 WHERE nvmhome = -9.0;"

				//Add panel entity key
				+ "ALTER TABLE " + benefitUnitTable + " ALTER COLUMN idbenefitunit RENAME TO id;"
				+ "ALTER TABLE " + benefitUnitTable + " ADD COLUMN simulation_time INT DEFAULT " + startyear + ";"
				+ "ALTER TABLE " + benefitUnitTable + " ADD COLUMN simulation_run INT DEFAULT 0;"
				+ "ALTER TABLE " + benefitUnitTable + " ADD COLUMN working_id INT DEFAULT 0;"

				//Re-order by id
				+ "SELECT * FROM " + benefitUnitTable + " ORDER BY id;"
			);

			//Remove duplicate rows
			stat.execute(
				"CREATE TABLE NEW AS SELECT DISTINCT * FROM " + benefitUnitTable + ";"
				+ "DROP TABLE IF EXISTS " + benefitUnitTable + ";"
				+ "ALTER TABLE NEW RENAME TO " + benefitUnitTable + ";"
			);

			if (PROCESS_KEY_IDENTIFICATION) {
				stat.execute(
						"ALTER TABLE " + benefitUnitTable + " ALTER COLUMN id BIGINT NOT NULL;"
								+ "ALTER TABLE " + benefitUnitTable + " ALTER COLUMN simulation_time INT NOT NULL;"
								+ "ALTER TABLE " + benefitUnitTable + " ALTER COLUMN simulation_run INT NOT NULL;"
								+ "ALTER TABLE " + benefitUnitTable + " ALTER COLUMN working_id INT NOT NULL;"
								+ "ALTER TABLE " + benefitUnitTable + " ADD PRIMARY KEY (id, simulation_time, simulation_run, working_id);"
				);
			}

			// CREATE HOUSEHOLD TABLE
			stat.execute(
					"DROP TABLE IF EXISTS " + householdTable + ";"
							+ "CREATE TABLE " + householdTable + " AS (SELECT " + stringAppender(inputHouseholdColumnNameSet) + " FROM " + inputFileName + ");"
							+ "ALTER TABLE " + householdTable + " ALTER COLUMN idhh RENAME TO id;"
							+ "ALTER TABLE " + householdTable + " ADD COLUMN simulation_time INT DEFAULT " + startyear + ";"
							+ "ALTER TABLE " + householdTable + " ADD COLUMN simulation_run INT DEFAULT 0;"
							+ "ALTER TABLE " + householdTable + " ADD COLUMN working_id INT DEFAULT 0;"
							+ "SELECT * FROM " + householdTable + " ORDER BY id;"

			);

			//Remove duplicate rows
			stat.execute(
					"CREATE TABLE NEW AS SELECT DISTINCT * FROM " + householdTable + ";"
				+ "DROP TABLE IF EXISTS " + householdTable + ";"
				+ "ALTER TABLE NEW RENAME TO " + householdTable + ";"
			);

			if (PROCESS_KEY_IDENTIFICATION) {

				stat.execute(
						"ALTER TABLE " + householdTable + " ALTER COLUMN id BIGINT NOT NULL;"
								+ "ALTER TABLE " + householdTable + " ALTER COLUMN simulation_time INT NOT NULL;"
								+ "ALTER TABLE " + householdTable + " ALTER COLUMN simulation_run INT NOT NULL;"
								+ "ALTER TABLE " + householdTable + " ALTER COLUMN working_id INT NOT NULL;"
								+ "ALTER TABLE " + householdTable + " ADD PRIMARY KEY (id, simulation_time, simulation_run, working_id);"
				);
			}

			//Set-up foreign keys
			if (PROCESS_KEY_IDENTIFICATION) {

				stat.execute(
						"ALTER TABLE " + benefitUnitTable + " ADD FOREIGN KEY (hhid, hhtime, hhrun, prid) REFERENCES "
								+ householdTable + " (id, simulation_time, simulation_run, working_id);"
								+ "ALTER TABLE " + personTable + " ADD FOREIGN KEY (buid, butime, burun, prid) REFERENCES "
								+ benefitUnitTable + " (id, simulation_time, simulation_run, working_id);"
				);
			}

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



	/**
	 *
	 * GENERATE DATABASE TABLES TO INITIALISE SIMULATED POPULATION CROSS-SECTION FROM CSV FILES
	 * @param country
	 *
	 */
	public static void databaseFromCSV(Country country, boolean showGui) {

		String title = "Building database tables for starting populations";
		JFrame databaseFrame = null;
		if (showGui) {
			// display a dialog box to let the user know what is happening
			String text = "<html><h2 style=\"text-align: center; font-size:120%; padding: 10pt\">"
					+ "Building database tables to initialise simulated population cross-section for " + country.getCountryName()
					+ "</h2></html>";

			databaseFrame = FormattedDialogBox.create(title, text, 800, 120, null, false, false, showGui);
		}
		System.out.println(title);

		// start work
		Connection conn = null;
		try {
			Class.forName("org.h2.Driver");
			conn = DriverManager.getConnection("jdbc:h2:file:./input" + File.separator + "input;TRACE_LEVEL_FILE=0;TRACE_LEVEL_SYSTEM_OUT=0;AUTO_SERVER=TRUE", "sa", "");

			Parameters.setPopulationInitialisationInputFileName("population_initial_" + country.toString());

			//This calls a method creating both the donor population tables and initial populations for every year between minStartYear and maxStartYear.
			DataParser.createDatabaseForPopulationInitialisationByYearFromCSV(country, Parameters.getPopulationInitialisationInputFileName(), Parameters.getMinStartYear(), Parameters.getMaxStartYear(), conn);

			conn.close();
		}
		catch(ClassNotFoundException|SQLException e){
			if(e instanceof ClassNotFoundException) {
				System.out.println( "ERROR: Class not found: " + e.getMessage() + "\nCheck that the input.h2.db "
						+ "exists in the input folder.  If not, unzip the input.h2.zip file and store the resulting "
						+ "input.h2.db in the input folder!\n");
			}
			else {
				throw new IllegalArgumentException("SQL Exception thrown! " + e.getMessage());
			}
		}
		finally {
			try {
				if (conn != null) { conn.close(); }
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		// remove message box
		if (databaseFrame != null)
			databaseFrame.setVisible(false);
	}
}