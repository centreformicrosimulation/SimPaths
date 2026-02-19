package simpaths.data.startingpop;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

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

    public static void createDatabaseForPopulationInitialisationByYearFromCSV(Country country, String initialInputFilename, ArrayList<Integer> includeYears, Connection conn) {

        //Initialise repository table for country-year-population size combinations
        initialiseRepository(conn, includeYears.get(0));

        //Construct tables for Simulated Persons & Households (initial population)
        for (Integer year: includeYears) {
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
				+ "DROP TABLE IF EXISTS " + benefitUnitTable + " CASCADE;"
				+ "DROP TABLE IF EXISTS " + householdTable + " CASCADE;"
			);

			// check if input file exists
			File ff = new File(inputFileLocation);
			if (ff.exists()) {

				// create person table
				stat.execute(
						"CREATE TABLE " + inputFileName + " AS SELECT * FROM CSVREAD(\'" + inputFileLocation + "\');"
								+ "DROP TABLE IF EXISTS " + personTable + " CASCADE;"
								+ "CREATE TABLE " + personTable + " AS (SELECT " + stringAppender(inputPersonColumnNamesSet) + " FROM " + inputFileName + ");"

								//Add panel entity key
								+ "ALTER TABLE " + personTable + " ALTER COLUMN idPers RENAME TO id;"
								+ "ALTER TABLE " + personTable + " ALTER COLUMN id BIGINT;"
								+ "ALTER TABLE " + personTable + " ADD COLUMN simulation_time INT DEFAULT " + startyear + ";"
								+ "ALTER TABLE " + personTable + " ADD COLUMN simulation_run INT DEFAULT 0;"
								+ "ALTER TABLE " + personTable + " ADD COLUMN working_id INT DEFAULT 0;"

								//Health
								+ "ALTER TABLE " + personTable + " ADD health VARCHAR_IGNORECASE;"
								+ "UPDATE " + personTable + " SET health = 'Poor' WHERE healthSelfRated = 1;"
								+ "UPDATE " + personTable + " SET health = 'Fair' WHERE healthSelfRated = 2;"
								+ "UPDATE " + personTable + " SET health = 'Good' WHERE healthSelfRated = 3;"
								+ "UPDATE " + personTable + " SET health = 'VeryGood' WHERE healthSelfRated = 4;"
								+ "UPDATE " + personTable + " SET health = 'Excellent' WHERE healthSelfRated = 5;"
								+ "ALTER TABLE " + personTable + " DROP COLUMN healthSelfRated;"
								+ "ALTER TABLE " + personTable + " ALTER COLUMN health RENAME TO healthSelfRated;"

									//Education (4 categories incl. in education)
									+ "ALTER TABLE " + personTable + " ADD education VARCHAR_IGNORECASE;"
									+ "UPDATE " + personTable + " SET education = 'Low' WHERE eduHighestC4 = 3;"
									+ "UPDATE " + personTable + " SET education = 'Medium' WHERE eduHighestC4 = 2;"
									+ "UPDATE " + personTable + " SET education = 'High' WHERE eduHighestC4 = 1;"
									+ "UPDATE " + personTable + " SET education = 'InEducation' WHERE eduHighestC4 = 0;"

								+ "ALTER TABLE " + personTable + " DROP COLUMN eduHighestC4;"
								+ "ALTER TABLE " + personTable + " ALTER COLUMN education RENAME TO eduHighestC4;"

								//Education mother
								+ "ALTER TABLE " + personTable + " ADD education_mother VARCHAR_IGNORECASE;"
								+ "UPDATE " + personTable + " SET education_mother = 'Low' WHERE eduHighestMotherC3 = 3;"
								+ "UPDATE " + personTable + " SET education_mother = 'Medium' WHERE eduHighestMotherC3 = 2;"
								+ "UPDATE " + personTable + " SET education_mother = 'High' WHERE eduHighestMotherC3 = 1;"
								+ "ALTER TABLE " + personTable + " DROP COLUMN eduHighestMotherC3;"
								+ "ALTER TABLE " + personTable + " ALTER COLUMN education_mother RENAME TO eduHighestMotherC4;"

								//Education father
								+ "ALTER TABLE " + personTable + " ADD education_father VARCHAR_IGNORECASE;"
								+ "UPDATE " + personTable + " SET education_father = 'Low' WHERE eduHighestFatherC3 = 3;"
								+ "UPDATE " + personTable + " SET education_father = 'Medium' WHERE eduHighestFatherC3 = 2;"
								+ "UPDATE " + personTable + " SET education_father = 'High' WHERE eduHighestFatherC3 = 1;"
								+ "ALTER TABLE " + personTable + " DROP COLUMN eduHighestFatherC3;"
								+ "ALTER TABLE " + personTable + " ALTER COLUMN education_father RENAME TO eduHighestFatherC4;"

								//In education dummy (to be used with Indicator enum when defined in Person class)
								+ "ALTER TABLE " + personTable + " ADD education_in VARCHAR_IGNORECASE;"
								+ "UPDATE " + personTable + " SET education_in = 'False' WHERE eduSpellFlag = 0;"
								+ "UPDATE " + personTable + " SET education_in = 'True' WHERE eduSpellFlag = 1;"
								+ "ALTER TABLE " + personTable + " DROP COLUMN eduSpellFlag;"
								+ "ALTER TABLE " + personTable + " ALTER COLUMN education_in RENAME TO eduSpellFlag;"

								//Return to education dummy (to be used with Indicator enum when defined in Person class)
								+ "ALTER TABLE " + personTable + " ADD education_return VARCHAR_IGNORECASE;"
								+ "UPDATE " + personTable + " SET education_return = 'False' WHERE eduReturnFlag = 0;"
								+ "UPDATE " + personTable + " SET education_return = 'True' WHERE eduReturnFlag = 1;"
								+ "ALTER TABLE " + personTable + " DROP COLUMN eduReturnFlag;"
								+ "ALTER TABLE " + personTable + " ALTER COLUMN education_return RENAME TO eduReturnFlag;"

								//Gender
								+ "ALTER TABLE " + personTable + " ADD demSex VARCHAR_IGNORECASE;"
								+ "UPDATE " + personTable + " SET demSex = 'Female' WHERE demMaleFlag = 0;"
								+ "UPDATE " + personTable + " SET demSex = 'Male' WHERE demMaleFlag = 1;"
								+ "ALTER TABLE " + personTable + " DROP COLUMN demMaleFlag;"
								+ "ALTER TABLE " + personTable + " ALTER COLUMN demSex RENAME TO demMaleFlag;"

								//Weights
								+"ALTER TABLE " + personTable + " ALTER COLUMN wgtHhCross RENAME TO wgt;"

								//Labour Market Economic Status
								+ "ALTER TABLE " + personTable + " ADD activity_status VARCHAR_IGNORECASE;"
								+ "UPDATE " + personTable + " SET labC4 = 3 WHERE labC4 = 1 AND CAST(labWageHrly AS FLOAT)<0.01;"
								+ "UPDATE " + personTable + " SET activity_status = 'EmployedOrSelfEmployed' WHERE labC4 = 1;"
								+ "UPDATE " + personTable + " SET activity_status = 'Student' WHERE labC4 = 2;"
								+ "UPDATE " + personTable + " SET activity_status = 'NotEmployed' WHERE labC4 = 3;"
								+ "UPDATE " + personTable + " SET activity_status = 'Retired' WHERE labC4 = 4;"
								+ "ALTER TABLE " + personTable + " DROP COLUMN labC4;"
								+ "ALTER TABLE " + personTable + " ALTER COLUMN activity_status RENAME TO labC4;"

								//DEMOGRAPHIC: Long-term sick or disabled (to be used with Indicator enum when defined in Person class)
								+ "ALTER TABLE " + personTable + " ADD sick_longterm VARCHAR_IGNORECASE;"
								+ "UPDATE " + personTable + " SET sick_longterm = 'False' WHERE healthDsblLongtermFlag = 0;"
								+ "UPDATE " + personTable + " SET sick_longterm = 'True' WHERE healthDsblLongtermFlag = 1;"
								+ "ALTER TABLE " + personTable + " DROP COLUMN healthDsblLongtermFlag;"
								+ "ALTER TABLE " + personTable + " ALTER COLUMN sick_longterm RENAME TO healthDsblLongtermFlag;" // There are two versions of this variable in the Initial Population files. We use the healthDsblLongtermFlag, but we rename it healthDsblLongtermFlag fo simplicity



								//DEMOGRAPHIC: Need social care (to be used with Indicator enum when defined in Person class)
								+ "ALTER TABLE " + personTable + " ADD need_care VARCHAR_IGNORECASE;"
								+ "UPDATE " + personTable + " SET need_care = 'False' WHERE careNeedFlag = 0;"
								+ "UPDATE " + personTable + " SET need_care = 'True' WHERE careNeedFlag = 1;"
								+ "ALTER TABLE " + personTable + " DROP COLUMN careNeedFlag;"
								+ "ALTER TABLE " + personTable + " ALTER COLUMN need_care RENAME TO careNeedFlag;"

								//DEMOGRAPHIC: Ethnicity
								+ "ALTER TABLE " + personTable + " ADD ethnicity VARCHAR_IGNORECASE;"
                                + "UPDATE " + personTable + " SET ethnicity = 'White' WHERE demEthnC6 = 1;"
                                + "UPDATE " + personTable + " SET ethnicity = 'Mixed' WHERE demEthnC6 = 2;"
                                + "UPDATE " + personTable + " SET ethnicity = 'Asian' WHERE demEthnC6 = 3;"
                                + "UPDATE " + personTable + " SET ethnicity = 'Black' WHERE demEthnC6 = 4;"
                                + "UPDATE " + personTable + " SET ethnicity = 'Other' WHERE demEthnC6 = 5;"
                                + "UPDATE " + personTable + " SET ethnicity = 'Missing' WHERE demEthnC6 = 6;"
                                + "ALTER TABLE " + personTable + " DROP COLUMN demEthnC6;"
                                + "ALTER TABLE " + personTable + " ALTER COLUMN ethnicity RENAME TO demEthnC6;"

								//SYSTEM: Year left education (to be used with Indicator enum when defined in Person class)
								+ "ALTER TABLE " + personTable + " ADD education_left VARCHAR_IGNORECASE;"
								+ "UPDATE " + personTable + " SET education_left = 'False' WHERE eduExitSampleFlag = 0;"
								+ "UPDATE " + personTable + " SET education_left = 'True' WHERE eduExitSampleFlag = 1;"
								+ "ALTER TABLE " + personTable + " DROP COLUMN eduExitSampleFlag;"
								+ "ALTER TABLE " + personTable + " ALTER COLUMN education_left RENAME TO eduExitSampleFlag;" //Getting data conversion error trying to directly change values of eduExitSampleFlag

								//Adult child flag:
								+ "ALTER TABLE " + personTable + " ADD adult_child VARCHAR_IGNORECASE;"
								+ "UPDATE " + personTable + " SET adult_child = 'False' WHERE demAdultChildFlag = 0;"
								+ "UPDATE " + personTable + " SET adult_child = 'True' WHERE demAdultChildFlag = 1;"
								+ "ALTER TABLE " + personTable + " DROP COLUMN demAdultChildFlag;"
								+ "ALTER TABLE " + personTable + " ALTER COLUMN adult_child RENAME TO demAdultChildFlag;"

								//Financial distress
								+ "ALTER TABLE " + personTable + " ADD financial_distress_add VARCHAR_IGNORECASE;"
								+ "UPDATE " + personTable + " SET financial_distress_add = 'False' WHERE yFinDstrssFlag = 0;"
								+ "UPDATE " + personTable + " SET financial_distress_add = 'True' WHERE yFinDstrssFlag = 1;"
								+ "ALTER TABLE " + personTable + " DROP COLUMN yFinDstrssFlag;"
								+ "ALTER TABLE " + personTable + " ALTER COLUMN financial_distress_add RENAME TO yFinDstrssFlag;"

								//Social care
								+ "ALTER TABLE " + personTable + " ADD socare_provided_to VARCHAR_IGNORECASE;"
								+ "UPDATE " + personTable + " SET socare_provided_to = 'None' WHERE careHrsProvidedWeek <= 0 OR careHrsProvidedWeek IS NULL;"
								+ "UPDATE " + personTable + " SET socare_provided_to = 'OnlyOther' WHERE careHrsProvidedWeek > 0;"
								+ "ALTER TABLE " + personTable + " ALTER COLUMN careHrsProvidedWeek RENAME TO socare_provided_hrs;"

								//SYSTEM : Year
								+ "ALTER TABLE " + personTable + " ALTER COLUMN statInterviewYear RENAME TO system_year;"

								//SYSTEM : Data collection wave
								+ "ALTER TABLE " + personTable + " ALTER COLUMN statCollectionWave RENAME TO system_wave;"

								+ "ALTER TABLE " + personTable + " ALTER COLUMN labHrsWorkWeek RENAME TO " + Parameters.HOURS_WORKED_WEEKLY + ";"
								+ "ALTER TABLE " + personTable + " ADD work_sector VARCHAR_IGNORECASE DEFAULT 'Private_Employee';"		//Here we assume by default that people are employed - this is because the MultiKeyMaps holding households have work_sector as a key, and cannot handle null values for work_sector. TODO: Need to check that this assumption is OK.
								+ "UPDATE " + personTable + " SET idMother = null WHERE idMother = -9;"
								+ "UPDATE " + personTable + " SET idFather = null WHERE idFather = -9;"

                                // Convert mental health and wellbeing scores to decimal (and 0/1 integer for GHQ caseness)
                                + "ALTER TABLE  " + personTable + " ALTER COLUMN HEALTHMENTALMCS DECIMAL(4, 2);"
                                + "ALTER TABLE  " + personTable + " ALTER COLUMN HEALTHMENTALPARTNERMCS DECIMAL(4, 2);"
                                + "ALTER TABLE  " + personTable + " ALTER COLUMN HEALTHPHYSICALPCS DECIMAL(4, 2);"
                                + "ALTER TABLE  " + personTable + " ALTER COLUMN HEALTHPHYSICALPARTNERPCS DECIMAL(4, 2);"
                                + "ALTER TABLE  " + personTable + " ALTER COLUMN DEMLIFESATSCORE0to10 DECIMAL(4, 2);"
                                + "ALTER TABLE  " + personTable + " ALTER COLUMN HEALTHWBSCORE0TO36 INT;"
                                + "ALTER TABLE  " + personTable + " ALTER COLUMN HEALTHPSYDSTRSS0to12 INT;"

                                //Rename idBu to BU_ID
								+ "ALTER TABLE " + personTable + " ALTER COLUMN idBu RENAME TO buid;"
								+ "ALTER TABLE " + personTable + " ADD COLUMN butime INT DEFAULT " + startyear + ";"
								+ "ALTER TABLE " + personTable + " ADD COLUMN burun INT DEFAULT 0;"
								+ "ALTER TABLE " + personTable + " ADD COLUMN prid INT DEFAULT 0;"
								+ "ALTER TABLE " + personTable + " ALTER COLUMN idHh RENAME TO idhousehold;"

                                + "CREATE INDEX IF NOT EXISTS idx_" + personTable + "_bukey ON " + personTable + " (buid, butime, burun, prid);"
                                + "CREATE INDEX IF NOT EXISTS idx_" + personTable + "_idhousehold ON " + personTable + " (idhousehold);"


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
						"CREATE TABLE " + benefitUnitTable + " AS (SELECT " + stringAppender(inputBenefitUnitColumnNamesSet) + " FROM " + inputFileName + ");"
								+ "ALTER TABLE " + benefitUnitTable + " ALTER COLUMN idHh RENAME TO hhid;"
								+ "ALTER TABLE " + benefitUnitTable + " ADD COLUMN hhtime INT DEFAULT " + startyear + ";"
								+ "ALTER TABLE " + benefitUnitTable + " ADD COLUMN hhrun INT DEFAULT 0;"
								+ "ALTER TABLE " + benefitUnitTable + " ADD COLUMN prid INT DEFAULT 0;"
								+ "ALTER TABLE " + benefitUnitTable + " ADD region VARCHAR_IGNORECASE;"
				);

				//Region - See Region class for mapping definitions and sources of info
				Parameters.setCountryRegions(country);
				for(Region region: Parameters.getCountryRegions()) {
					stat.execute(
							"UPDATE " + benefitUnitTable + " SET region = '" + region + "' WHERE demRgn = " + region.getValue() + ";"
					);
				}

				stat.execute(
						"ALTER TABLE " + benefitUnitTable + " DROP COLUMN demRgn;"

								//INCOME: BenefitUnit income - quintiles
								+ "ALTER TABLE " + benefitUnitTable + " ADD household_income_qtiles VARCHAR_IGNORECASE;"
								+ "UPDATE " + benefitUnitTable + " SET household_income_qtiles = 'Q1' WHERE yHhQuintilesMonthC5 = 1;"
								+ "UPDATE " + benefitUnitTable + " SET household_income_qtiles = 'Q2' WHERE yHhQuintilesMonthC5 = 2;"
								+ "UPDATE " + benefitUnitTable + " SET household_income_qtiles = 'Q3' WHERE yHhQuintilesMonthC5 = 3;"
								+ "UPDATE " + benefitUnitTable + " SET household_income_qtiles = 'Q4' WHERE yHhQuintilesMonthC5 = 4;"
								+ "UPDATE " + benefitUnitTable + " SET household_income_qtiles = 'Q5' WHERE yHhQuintilesMonthC5 = 5;"
								+ "ALTER TABLE " + benefitUnitTable + " DROP COLUMN yHhQuintilesMonthC5;"
								+ "ALTER TABLE " + benefitUnitTable + " ALTER COLUMN household_income_qtiles RENAME TO yHhQuintilesMonthC5;"

								//Disposable income
								// + "ALTER TABLE " + benefitUnitTable + " ALTER COLUMN yDispMonth RENAME TO yDispMonth;"

								//Homeownership
								+ "ALTER TABLE " + benefitUnitTable + " ADD dhh_owned_add VARCHAR_IGNORECASE;"
								+ "UPDATE " + benefitUnitTable + " SET dhh_owned_add = 'False' WHERE wealthPrptyFlag = 0;"
								+ "UPDATE " + benefitUnitTable + " SET dhh_owned_add = 'True' WHERE wealthPrptyFlag = 1;"
								+ "ALTER TABLE " + benefitUnitTable + " DROP COLUMN wealthPrptyFlag;"
								+ "ALTER TABLE " + benefitUnitTable + " ALTER COLUMN dhh_owned_add RENAME TO wealthPrptyFlag;"

								//WEALTH
								+ "UPDATE " + benefitUnitTable + " SET wealthTotValue = 0.0 WHERE wealthTotValue = -9.0;"
								+ "UPDATE " + benefitUnitTable + " SET wealthPensValue = 0.0 WHERE wealthPensValue = -9.0;"
								+ "UPDATE " + benefitUnitTable + " SET wealthPrptyValue = 0.0 WHERE wealthPrptyValue = -9.0;"
                                + "UPDATE " + benefitUnitTable + " SET wealthMortgageDebtValue = 0.0 WHERE wealthMortgageDebtValue = -9.0;"

								//Add panel entity key
								+ "ALTER TABLE " + benefitUnitTable + " ALTER COLUMN idBu RENAME TO id;"
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
						"CREATE TABLE " + householdTable + " AS (SELECT " + stringAppender(inputHouseholdColumnNameSet) + " FROM " + inputFileName + ");"
								+ "ALTER TABLE " + householdTable + " ALTER COLUMN idHh RENAME TO id;"
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
			}
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
			conn = DriverManager.getConnection("jdbc:h2:file:" + Parameters.getInputDirectory() + "input;TRACE_LEVEL_FILE=0;TRACE_LEVEL_SYSTEM_OUT=0;AUTO_SERVER=TRUE", "sa", "");

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
