package simpaths.model.taxes.database;


import javax.swing.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import simpaths.data.FormattedDialogBox;
import simpaths.data.Parameters;
import simpaths.model.enums.Country;
import simpaths.model.enums.Region;
import simpaths.model.taxes.*;


/**
 *
 * CLASS TO MANAGE TRANSLATION OF CSV DATA FROM EUROMOD TO DATABASE FOR DONORS USED TO IMPUTE TAX AND BENEFIT PAYMENTS
 * csv data are processed and saved to the DONORPERSON_<country code> table in the relational database. These data
 * are used as working variables to construct the DONORTAXUNIT_<country code> table, which is then used exclusively \
 * for imputing tax and benefit payments, drawing heavily on SQL calls made via Hibernate
 *
 */
public class TaxDonorDataParser {


    /**
     * ENTRY POINT FOR MANAGER
     *
     * @param country country object, defines the country code
     * @param startYear first simulated year
     *
     * THE MANAGER IS 'run' FROM SimPathsStart
     */
    public static void databaseFromCSV(Country country, int startYear, boolean isVisible) {

        // display a dialog box to let the user know what is happening
        String title = "Creating donor database tables";
        JFrame csvFrame = null;
        if (isVisible) {
            String text = "<html><h2 style=\"text-align: center; font-size:120%; padding: 10pt\">"
                    + "Constructing donor database tables for imputing tax and benefit payments</h2></html>";
            if (isVisible) csvFrame = FormattedDialogBox.create(title, text, 800, 120, null, false, false, isVisible);
        }
        System.out.println(title);

        // initialise tax database
        Parameters.setCountryBenefitUnitName(); //Specify names of benefit unit variables in EUROMOD

        // establish database connection
        Connection conn = null;
        try {
            Class.forName("org.h2.Driver");
            conn = DriverManager.getConnection("jdbc:h2:file:./input" + File.separator + "input;TRACE_LEVEL_FILE=0;TRACE_LEVEL_SYSTEM_OUT=0;AUTO_SERVER=TRUE", "sa", "");

            createTaxDonorTables(conn, country, startYear);
            updateDefaultDonorTables(conn, country);

            // clean-up
            conn.close();
            conn = null;
        }
        catch(ClassNotFoundException | SQLException e) {
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
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // remove message box
        if (csvFrame != null)
            csvFrame.setVisible(false);
    }


    public static void updateDefaultDonorTables(Connection conn, Country country) {

        Statement stat = null;
        try {
            stat = conn.createStatement();

            String[] tableNamesDonor = new String[]{"DONORTAXUNIT", "DONORPERSON", "DONORPERSONPOLICY", "DONORTAXUNITPOLICY"};
            for (String tableName : tableNamesDonor) {
                stat.execute("DROP TABLE IF EXISTS " + tableName + " CASCADE");
                stat.execute("CREATE TABLE " + tableName + " AS SELECT * FROM " + tableName + "_" + country);
                System.out.println("Completed updating " + tableName);
                stat.execute("ALTER TABLE " + tableName + " ALTER COLUMN ID BIGINT NOT NULL AUTO_INCREMENT;"
                        + "ALTER TABLE " + tableName + " ADD PRIMARY KEY (ID);");
            }
            stat.execute(
            "ALTER TABLE DONORPERSON ADD FOREIGN KEY (TUID) REFERENCES DONORTAXUNIT (ID);"
                + "ALTER TABLE DONORTAXUNITPOLICY ADD FOREIGN KEY (TUID) REFERENCES DONORTAXUNIT (ID);"
                + "ALTER TABLE DONORPERSONPOLICY ADD FOREIGN KEY (PID) REFERENCES DONORPERSON (ID);"
            );
        }
        catch(SQLException e){
            throw new RuntimeException("SQL Exception thrown! " + e.getMessage());
        }
        finally {
            try {
                if(stat != null) stat.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     *
     * METHOD TO INITIALISE PERSON-LEVEL RELATIONAL DATABASE TABLES FOR DONORS AND POPULATE FROM CSV DATA
     *
     * @param conn database connection
     * @param country defines the country code
     * @param startYear first simulated year
     *
     */
    private static void createTaxDonorTables(Connection conn, Country country, int startYear) {

        // file for importing csv data
        String taxDonorInputFileName = Parameters.getTaxDonorInputFileName();
        String donorInputFileLocation = Parameters.INPUT_DIRECTORY + taxDonorInputFileName + ".csv";

        // create temporary table for manipulating data
        Statement stat = null;
        try {
            stat = conn.createStatement();
            stat.execute(

                //Refresh table
                "DROP TABLE IF EXISTS " + taxDonorInputFileName + " CASCADE;"

                //Create new database table by reading in from population_country.csv file
                + "CREATE TABLE " + taxDonorInputFileName + " AS SELECT * FROM CSVREAD('" + donorInputFileLocation + "');"
            );

            //---------------------------------------------------------------------------
            //	DonorPerson table
            //---------------------------------------------------------------------------
            String personTableName = "DONORPERSON_" + country;

            // Ensure no duplicate column names
            Set<String> inputPersonStaticColumnNames = new LinkedHashSet<>(Arrays.asList(Parameters.DONOR_STATIC_VARIABLES));
            inputPersonStaticColumnNames.add((String) Parameters.getBenefitUnitVariableNames().getValue(country.getCountryName()));

            // begin SQL translation for person table - start with policy invariant variables (DONOR_STATIC_VARIABLES)
            stat.execute(

                "DROP TABLE IF EXISTS " + personTableName + " CASCADE;"
                + "CREATE TABLE " + personTableName + " AS (SELECT " + stringAppender(inputPersonStaticColumnNames) + " FROM " + taxDonorInputFileName + ");"

                //Add id column
                + "ALTER TABLE " + personTableName + " ALTER COLUMN idperson RENAME TO ID;"
                + "ALTER TABLE " + personTableName + " ALTER COLUMN ID BIGINT NOT NULL;"
                + "ALTER TABLE " + personTableName + " ADD PRIMARY KEY (ID);"

                //Reclassify EUROMOD variables
                // may need to change data structure type otherwise SQL conversion error, so create new column of the correct type,
                // map data from old column and drop old column

                //adjust name for tax unit identifier
                + "ALTER TABLE " + personTableName + " ALTER COLUMN " + Parameters.getBenefitUnitVariableNames().getValue(country.getCountryName()) + " RENAME TO tuid;"
                + "ALTER TABLE " + personTableName + " ALTER COLUMN tuid BIGINT;"

                //Age
                + "ALTER TABLE " + personTableName + " ALTER COLUMN DAG int NOT NULL;"

                //Country
                + "ALTER TABLE " + personTableName + " ADD COUNTRY VARCHAR_IGNORECASE;"
                + "UPDATE " + personTableName + " SET COUNTRY = '" + country + "' WHERE DCT = " + country.getEuromodCountryCode() + ";"
                + "ALTER TABLE " + personTableName + " DROP COLUMN DCT;"

                //Education
                + "ALTER TABLE " + personTableName + " ADD EDUCATION VARCHAR_IGNORECASE;"
                + "UPDATE " + personTableName + " SET EDUCATION = 'Low' WHERE deh < 2;"
                + "UPDATE " + personTableName + " SET EDUCATION = 'Medium' WHERE deh >= 2 AND deh < 5;"
                + "UPDATE " + personTableName + " SET EDUCATION = 'High' WHERE deh = 5;"
                + "ALTER TABLE " + personTableName + " DROP COLUMN deh;"

                //Gender
                + "ALTER TABLE " + personTableName + " ADD GENDER VARCHAR_IGNORECASE;"
                + "UPDATE " + personTableName + " SET GENDER = 'Female' WHERE DGN = 0;"
                + "UPDATE " + personTableName + " SET GENDER = 'Male' WHERE DGN = 1;"
                + "ALTER TABLE " + personTableName + " DROP COLUMN DGN;"
                + "ALTER TABLE " + personTableName + " ALTER COLUMN GENDER RENAME TO DGN;"
            );
            stat.execute(

                //Weights
                "ALTER TABLE " + personTableName + " ALTER COLUMN DWT RENAME TO WEIGHT;"
                + "ALTER TABLE " + personTableName + " ALTER COLUMN WEIGHT double;"

                //Labour Market Economic Status
                + "ALTER TABLE " + personTableName + " ADD ACTIVITY_STATUS VARCHAR_IGNORECASE;"
                + "UPDATE " + personTableName + " SET ACTIVITY_STATUS = 'EmployedOrSelfEmployed' WHERE LES >= 1 AND LES <= 3;"
                + "UPDATE " + personTableName + " SET ACTIVITY_STATUS = 'Student' WHERE LES = 0 OR LES = 6;"
                + "UPDATE " + personTableName + " SET ACTIVITY_STATUS = 'Retired' WHERE LES = 4;"
                + "UPDATE " + personTableName + " SET ACTIVITY_STATUS = 'NotEmployed' WHERE LES = 5 OR LES >= 7;"

                //Health
                + "ALTER TABLE " + personTableName + " ADD HEALTH VARCHAR_IGNORECASE;"
                + "UPDATE " + personTableName + " SET HEALTH = 'Good' WHERE LES != 8;"
                + "UPDATE " + personTableName + " SET HEALTH = 'Poor' WHERE LES = 8;"
                + "ALTER TABLE " + personTableName + " DROP COLUMN LES;"

                //Long-term sick and disabled
                + "ALTER TABLE " + personTableName + " ALTER COLUMN DDI int;"
                + "ALTER TABLE " + personTableName + " ALTER COLUMN DDI RENAME TO DLLTSD;"

                //social care provision
                + "ALTER TABLE " + personTableName + " ALTER COLUMN LCR01 int;"
                + "ALTER TABLE " + personTableName + " ALTER COLUMN LCR01 RENAME TO CARER;"

                //Labour hours
                //XXX: Could set " + Parameters.HOURS_WORKED_WEEKLY + ", earnings, labour cost etc. to 0 if retired.
                // However, the data does not conform - see idperson 101, who is retired pensioner aged 80, but who declares lhw = 40
                // i.e. works 40 hours per week and has a sizeable earnings and employer social contributions.
                + "ALTER TABLE " + personTableName + " ALTER COLUMN LHW int;"
                + "ALTER TABLE " + personTableName + " ALTER COLUMN LHW RENAME TO " + Parameters.HOURS_WORKED_WEEKLY.toUpperCase() + ";"

                //Region
                + "ALTER TABLE " + personTableName + " ADD REGION VARCHAR_IGNORECASE;"
            );

            //Region - See Region class for mapping definitions and sources of info
            Parameters.setCountryRegions(country);
            for(Region region: Parameters.getCountryRegions()) {
                stat.execute(
                    "UPDATE " + personTableName + " SET REGION = '" + region + "' WHERE DRGN1 = " + region.getValue() + ";"
                );
            }
            stat.execute( "ALTER TABLE " + personTableName + " DROP COLUMN DRGN1;");

            //Set zeros to null where relevant
            stat.execute(
                // Use yem and yse to define workStatus for non-civil-servants, i.e. those with lcs = 0?
                // If the person has absolute self-employment income > absolute employment income, define workStatus
                // enum as Self_Employed as self-employment income or loss has a bigger effect on personal wealth
                // than employment income (or loss).
                "ALTER TABLE " + personTableName + " ADD WORK_SECTOR VARCHAR_IGNORECASE DEFAULT 'Private_Employee';"		//Here we assume by default that people are employed - this is because the MultiKeyMaps holding households have work_sector as a key, and cannot handle null values for work_sector. TODO: Need to check that this assumption is OK.
                + "ALTER TABLE " + personTableName + " ALTER COLUMN YEM DOUBLE;"
                + "ALTER TABLE " + personTableName + " ALTER COLUMN YSE DOUBLE;"

                //TODO: Check whether we should re-install the check of activity_status = 'Employed' for definitions below, and potentially add a 'Null' value to handle cases where people are not employed.
                + "UPDATE " + personTableName + " SET WORK_SECTOR = 'Self_Employed' WHERE abs(YEM) < abs(YSE);"		//Size of earnings derived from self-employment income (including declared self-employment losses) is larger than employment income (or loss - although while yse is sometimes negative, I'm not sure if yem is ever negative), so define as self-employed.
                + "UPDATE " + personTableName + " SET WORK_SECTOR = 'Public_Employee' WHERE LCS = 1;"		//Lastly, regardless of yem or yse, if lcs = 1, indicates person is s civil servant so overwrite any value with 'Public_Employee' work_sector value.

                //        		+ "UPDATE " + personTable + " SET earnings = yem + yse;"		//Now use EUROMOD output ils_earns, which includes more than just yem + yse, depending on the country (e.g. in Italy there is a temporary employment field yemtj 'income from co.co.co.').
                + "ALTER TABLE " + personTableName + " ALTER COLUMN YEM RENAME TO EMPLOYMENT_INCOME;"
                + "ALTER TABLE " + personTableName + " ALTER COLUMN YSE RENAME TO SELF_EMPLOYMENT_INCOME;"
                + "ALTER TABLE " + personTableName + " DROP COLUMN LCS;"

                //Re-order by id
                + "SELECT * FROM " + personTableName + " ORDER BY ID;"
            );


            //---------------------------------------------------------------------------
            //	DonorPersonPolicy table
            //---------------------------------------------------------------------------
            String personPolicyTableName = "DONORPERSONPOLICY_" + country;

            // initialise table
            StringBuilder varList1 = new StringBuilder("PID VARCHAR, FROM_YEAR INT, SYSTEM_YEAR INT");
            StringBuilder varList2 = new StringBuilder("PID");
            for(String variable: Parameters.DONOR_POLICY_VARIABLES) {
                varList1.append(", ").append(variable.toUpperCase()).append(" VARCHAR");
                varList2.append(", ").append(variable.toUpperCase());
            }

            stat.execute(
            "DROP TABLE IF EXISTS " + personPolicyTableName + " CASCADE;"
                + "CREATE TABLE " + personPolicyTableName + " (ID BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT, " + varList1 + ");"
            );

            // loop over each policy reference
            for( Integer fromYear: Parameters.EUROMODpolicyScheduleSystemYearMap.keySet()) {

                // construct variable names
                String systemName = Parameters.EUROMODpolicyScheduleSystemYearMap.get(fromYear).getKey();
                Integer systemYear = Parameters.EUROMODpolicyScheduleSystemYearMap.get(fromYear).getValue();
                Set<String> inputPersonDynamicColumnNames = new LinkedHashSet<>(0);
                inputPersonDynamicColumnNames.add("IDPERSON");
                for(String variable: Parameters.DONOR_POLICY_VARIABLES) {
                    inputPersonDynamicColumnNames.add(variable + "_" + systemName);
                }

                stat.execute(
                    "INSERT INTO " + personPolicyTableName + " (" + varList2 + ")"
                    + " SELECT " + stringAppender(inputPersonDynamicColumnNames) + " FROM " + taxDonorInputFileName + ";"
                );
                stat.execute(
                    "UPDATE " + personPolicyTableName + " SET SYSTEM_YEAR = " + systemYear + " WHERE SYSTEM_YEAR IS NULL;"
                    + "UPDATE " + personPolicyTableName + " SET FROM_YEAR = " + fromYear + " WHERE FROM_YEAR IS NULL;"
                );
            }
            stat.execute(
                "ALTER TABLE " + personPolicyTableName + " ALTER COLUMN PID BIGINT;"
            );
            for(String variable: Parameters.DONOR_POLICY_VARIABLES) {
                stat.execute( "ALTER TABLE " + personPolicyTableName + " ALTER COLUMN " + variable + " DOUBLE;");
            }


            //---------------------------------------------------------------------------
            //	DonorTaxUnit table
            //---------------------------------------------------------------------------
            String	taxUnitTableName = "DONORTAXUNIT_" + country;
            stat.execute(
                // make copy of person table, using tuid
                "DROP TABLE IF EXISTS TEMP CASCADE;"
                + "CREATE TABLE TEMP AS (SELECT TUID, WEIGHT FROM " + personTableName + ");"

                // extract only unique values of tuid
                +"DROP TABLE IF EXISTS " + taxUnitTableName + " CASCADE;"
                + "CREATE TABLE " + taxUnitTableName + " AS SELECT DISTINCT * FROM TEMP ORDER BY TUID;"
                + "DROP TABLE IF EXISTS TEMP CASCADE;"

                // establish primary key
                + "ALTER TABLE " + taxUnitTableName + " ALTER COLUMN TUID RENAME TO ID;"
                + "ALTER TABLE " + taxUnitTableName + " ALTER COLUMN ID BIGINT NOT NULL;"
                + "ALTER TABLE " + taxUnitTableName + " ADD PRIMARY KEY (ID);"
                + "SELECT * FROM " + taxUnitTableName + " ORDER BY ID;"
            );


            //---------------------------------------------------------------------------
            //	DonorTaxUnitPolicy table
            //---------------------------------------------------------------------------
            String taxUnitPolicyTableName = "DONORTAXUNITPOLICY_" + country;
            StringBuilder varList = new StringBuilder("TUID LONG, FROM_YEAR INT, SYSTEM_YEAR INT");

            stat.execute(
                    "DROP TABLE IF EXISTS " + taxUnitPolicyTableName + " CASCADE;"
                            + "CREATE TABLE " + taxUnitPolicyTableName + " (ID BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT, " + varList + ");"
            );


            //---------------------------------------------------------------------------
            //  Set-up foreign keys
            //---------------------------------------------------------------------------
            stat.execute(
                "ALTER TABLE " + personTableName + " ADD FOREIGN KEY (TUID) REFERENCES " + taxUnitTableName + " (ID);"
                + "ALTER TABLE " + taxUnitPolicyTableName + " ADD FOREIGN KEY (TUID) REFERENCES " + taxUnitTableName + " (ID);"
                + "ALTER TABLE " + personPolicyTableName + " ADD FOREIGN KEY (PID) REFERENCES " + personTableName + " (ID);"
            );


            //---------------------------------------------------------------------------
            //  Clean-up
            //---------------------------------------------------------------------------
            stat.execute( "DROP TABLE IF EXISTS " + taxDonorInputFileName + " CASCADE;");

        }
        catch(SQLException e) {
            throw new IllegalArgumentException("SQL Exception thrown!" + e.getMessage());
        }
        finally {
            try {
                if(stat != null) stat.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     *
     * METHOD TO CONVERT COLLECTION OF STRINGS TO COMMA-SEPARATED SINGLE STRING
     *
     * @param strings collection of strings
     *
     */
    public static String stringAppender(Collection<String> strings) {
        Iterator<String> iter = strings.iterator();
        StringBuilder sb = new StringBuilder();
        while (iter.hasNext()) {
            sb.append(iter.next().toUpperCase());
            if (iter.hasNext()) sb.append(",");
        }
        return sb.toString();
    }


    /**
     *
     * Constructs the tax donor population based on the data stored in .txt files produced by EUROMOD.
     * Note that in order to store this information in an efficient way, we make the important
     * assumption that all EUROMOD output for a particular country is derived from the same
     * input population (this is plausible, given that EUROMOD is a static microsimulation).
     * When running EUROMOD, the most recent input population for a particular country should be
     * used (currently IT_2014_a2.txt and UK_2013_a3.txt).
     *
     * This method constructs a .csv file that aggregates the information from multiple EUROMOD
     * output .txt files, picking up the relevant columns for each EUROMOD policy scenario, that
     * will eventually be parsed into the JAS-mine input database.
     *
     * @return The name of the created CSV file (without the .csv extension)
     *
     */
    public static void constructAggregateTaxDonorPopulationCSVfile(Country country, boolean showGui) {

        // display a dialog box to let the user know what is happening
        String title = "Creating " + Parameters.getTaxDonorInputFileName() + ".csv file";
        JFrame csvFrame = null;

        if (showGui) {
            String text = "<html><h2 style=\"text-align: center; font-size:120%; padding: 10pt\">"
                    + "Compiling single working file to facilitate construction of relational database for imputing transfer payments</h2>";
            csvFrame = FormattedDialogBox.create(title, text, 800, 120, null, false, false, showGui);
        }

        System.out.println(title);

        // fields for exporting tables to output .csv files
        final String newLine = "\n";
        final String delimiter = ",";

        // prepare file and buffer to write data
        Map<Integer, String> euromodPolicySchedule = Parameters.calculateEUROMODpolicySchedule(country);
        Path target = FileSystems.getDefault().getPath(Parameters.INPUT_DIRECTORY, Parameters.getTaxDonorInputFileName() + ".csv");
        if (target.toFile().exists()) {
            target.toFile().delete();		// delete previous version of the file to allow the new one to be constructed
        }
        BufferedWriter bufferWriter = null;

        try {
            // read data
            Map<String, List<String[]>> allFilesByName = new LinkedHashMap<String, List<String[]>>();

            // get list of attributes (column names) from EUROMOD output files that are not policy dependent (i.e. they are like the input data)
            Set<String> policyInvariantAttributeNames = new LinkedHashSet<String>(Arrays.asList(Parameters.DONOR_STATIC_VARIABLES));

            //Append the names of country-specific variables
            Parameters.setCountryBenefitUnitName(); //Specify names of benefit unit variables
            policyInvariantAttributeNames.add((String) Parameters.getBenefitUnitVariableNames().getValue(country.getCountryName()));

            // create list of attributes (column names) of EUROMOD output files that depend on the policy parameters and that will vary between different scenarios
            Set<String> policyOutputVariables = new LinkedHashSet<String>(Arrays.asList(Parameters.DONOR_POLICY_VARIABLES));

            int numRows = -1;
            for (String policyName: euromodPolicySchedule.values()) {

                Path source;
                source = FileSystems.getDefault().getPath(Parameters.getEuromodOutputDirectory(), policyName + ".txt");
                List<String> fileContentByLine = Files.readAllLines(source);

                // Get indices of required vars. The first file should include Input & Output vars, the rest only Output vars.
                String[] tmpHeader = fileContentByLine.get(0).split("\t");
                Map<String, Integer> indices = new LinkedHashMap<String, Integer>();
                int p = 0;
                for (String vr: tmpHeader)
                {
                    if (numRows == -1 && policyInvariantAttributeNames.contains(vr)) indices.put(vr, p);
                    if (policyOutputVariables.contains(vr)) indices.put(vr, p);
                    p++;
                }

                // check the number of rows of each .txt file (which corresponds to the number of persons) are the same
                if (numRows == -1) {	//Set the length to the first file
                    numRows = fileContentByLine.size();
                }
                else {
                    int newNumRows = fileContentByLine.size();
                    if (newNumRows != numRows) {
                        throw new IllegalArgumentException("ERROR - the EUROMOD policy scenario textfile " + policyName + ".txt has " + newNumRows + " rows, which is not the same number as at least one other EUROMOD .txt file!  All files must have the same number of rows (each row corresponds to a different person / agent)!");
                    }
                }

                List<String[]> fileContentByLineSplit = new ArrayList<>(numRows);

                for (String line: fileContentByLine) {
                    String[] dataArray = line.split("\t");
                    List<String> usedVars = new ArrayList<>();
                    for (Integer ind: indices.values()) usedVars.add(dataArray[ind]);
                    fileContentByLineSplit.add(usedVars.toArray(new String[0]));
                    //				fileContentByLineSplit.add(dataArray);
                }

                allFilesByName.put(policyName, fileContentByLineSplit);
            }

            // write data
            target.toFile().createNewFile();
            bufferWriter = new BufferedWriter(new FileWriter(target.toFile(), true));

            // structure to hold all data to be parsed from the EUROMOD output files. The first key is the EUROMOD output text filename, while the value is a map that maps the name of the attribute (i.e. the column name) to its array index (i.e. the column number, starting from 0) in the output file, as stored in allFilesByName.
            Map<String, LinkedHashMap<String, Integer>> attributePositionsByNameByFilename = new LinkedHashMap<String, LinkedHashMap<String, Integer>>();
            for (String filename: allFilesByName.keySet()) {
                attributePositionsByNameByFilename.put(filename, new LinkedHashMap<String, Integer>());
            }

            // policy independent attributes
            // the first filename is used to get the data for all the input attributes (i.e. those not affected by EUROMOD policy scenario, not an output).
            Iterator<String> filenameIter = allFilesByName.keySet().iterator();
            String firstFilename = filenameIter.next();
            String[] header = allFilesByName.get(firstFilename).get(0);		//Get header line
            LinkedHashMap<String, Integer> firstFilenameMapColumnNameToIndex = attributePositionsByNameByFilename.get(firstFilename);
            for (int i = 0; i < header.length; i++) {
                String columnName = header[i];
                if (policyInvariantAttributeNames.contains(columnName)) { //PB RMK: If name of variable has been misspelled for example, this will not pick it up - will proceed but produce a file with the variable missing and crash later
                    firstFilenameMapColumnNameToIndex.put(header[i], i);
                    bufferWriter.append(columnName + delimiter);
                }
            }

            // policy dependent attributes
            for (String filename: allFilesByName.keySet()) {
                header = allFilesByName.get(filename).get(0);		//Get header line
                LinkedHashMap<String, Integer> mapColumnNameToIndex = attributePositionsByNameByFilename.get(filename);
                for (int i = 0; i < header.length; i++) {
                    String columnName = header[i];
                    if (policyOutputVariables.contains(columnName)) {
                        mapColumnNameToIndex.put(columnName, i);
                        bufferWriter.append(columnName + "_" + filename + delimiter);		//Note that the policy dependent variable names have an additional label that follows the filename of the specific EUROMOD policy output file.
                    }
                }
            }
            bufferWriter.append(newLine);

            // write data to new file
            for (int row = 1; row < numRows; row++) {
                for (String filename: attributePositionsByNameByFilename.keySet()) {
                    Map<String, Integer> mapColumnNamesByIndex = attributePositionsByNameByFilename.get(filename);
                    for (String columnName: mapColumnNamesByIndex.keySet()) {
                        int column = mapColumnNamesByIndex.get(columnName);
                        if (!columnName.equals(allFilesByName.get(filename).get(0)[column])) {
                            throw new IllegalArgumentException("ERROR - column names do not match!");
                        }
                        bufferWriter.append(allFilesByName.get(filename).get(row)[column] + delimiter);
                    }
                }
                bufferWriter.append(newLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
        finally {
            try {
                bufferWriter.flush();
                bufferWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Throwable e) {
                e.printStackTrace();
                throw e;
            }
        }

        // finish off
        if (csvFrame != null) csvFrame.setVisible(false);
//		return inputFilename;
    }


    /**
     *
     * METHOD TO POPULATE TAX UNIT TABLES FROM PERSON LEVEL DATA
     *
     */
    public static void populateDonorTaxUnitTables(Country country, boolean showGui) {

        // display a dialog box to let the user know what is happening
        String title = "Populating donor database tables";
        JFrame csvFrame = null;
        if (showGui) {
            String text = "<html><h2 style=\"text-align: center; font-size:120%; padding: 10pt\">"
                    + "Populating database with tax-unit data evaluated from person-level data</h2></html>";
            csvFrame = FormattedDialogBox.create(title, text, 800, 120, null, false, false, showGui);
        }

        System.out.println(title);

        // gather all donor tax units
        List<DonorTaxUnit> taxUnits = null;

        // establish session for database link
        EntityTransaction txn = null;
        try {

            EntityManager em = Persistence.createEntityManagerFactory("tax-database").createEntityManager();
            txn = em.getTransaction();
            txn.begin();

            // check that all annual samples are the same size (system files for database should have used the same input data
            Integer checkSum = null;
            for (int fromYear : Parameters.EUROMODpolicyScheduleSystemYearMap.keySet()) {
                int systemYear = Parameters.EUROMODpolicyScheduleSystemYearMap.get(fromYear).getValue();
                String query = "SELECT pu FROM DonorPersonPolicy pu WHERE systemYear=" + systemYear;
                List<DonorPersonPolicy> persons = em.createQuery(query).getResultList();
                if (checkSum==null) {
                    checkSum = persons.size();
                } else {
                    if (checkSum!=persons.size())
                        // All tax database files should be derived using same input dataset
                        throw new RuntimeException("sample sizes for tax database vary between system years");
                }
            }
            System.out.println("Completed checks of donor data sample sizes");

            // populate tax unit data
            taxUnits = em.createQuery("SELECT DISTINCT tu FROM DonorTaxUnit tu LEFT JOIN FETCH tu.persons tp LEFT JOIN FETCH tp.policies pl").getResultList();
            System.out.println("Completed querying full donor database sample");
            int counter = 0, decile=(int)((double)taxUnits.size()*0.1);
            for (DonorTaxUnit taxUnit : taxUnits) {
                // loop through tax units

                counter++;
                int age = 0, numberMembersOver17 = 0, numberChildrenUnder5 = 0, numberChildren5To9 = 0;
                int numberChildren10To17 = 0, dlltsd1 = -1, dlltsd2 = -1, careProvision = -1;
                double hoursWorkedPerWeek1 = 0.0, hoursWorkedPerWeek2 = 0.0;
                boolean flagInitialiseDemographics = true;
                for (int fromYear : Parameters.EUROMODpolicyScheduleSystemYearMap.keySet()) {

                    int systemYear = Parameters.EUROMODpolicyScheduleSystemYearMap.get(fromYear).getValue();
                    double origIncome = 0.0;
                    double earnings = 0.0;
                    double dispIncome = 0.0;
                    double benmt = 0.0;
                    double bennt = 0.0;
                    double principalIncome = -999999.0;
                    double childcare = 0.0;
                    int ageTest = 0;
                    for(DonorPerson person : taxUnit.getPersons()) {
                        // loop through persons

                        origIncome += person.getPolicy(fromYear).getOriginalIncomePerMonth();
                        if (person.getPolicy(fromYear).getOriginalIncomePerMonth() > principalIncome)
                            principalIncome = person.getPolicy(fromYear).getOriginalIncomePerMonth();
                        earnings += person.getPolicy(fromYear).getEarningsPerMonth();
                        dispIncome += person.getPolicy(fromYear).getDisposableIncomePerMonth();
                        benmt += person.getPolicy(fromYear).getMonetaryBenefitsAmount();
                        bennt += person.getPolicy(fromYear).getNonMonetaryBenefitsAmount();
                        childcare += person.getPolicy(fromYear).getChildcareCostPerMonth();
                        int agePerson = person.getAge();
                        if (flagInitialiseDemographics) {
                            // need to instantiate variables to evaluate keys

                            age = Math.max(age, agePerson);
                            if (agePerson < 5) {
                                numberChildrenUnder5 += 1;
                            } else if (agePerson < 10) {
                                numberChildren5To9 += 1;
                            } else if (agePerson < Parameters.AGE_TO_BECOME_RESPONSIBLE) {
                                numberChildren10To17 += 1;
                            } else {
                                numberMembersOver17 += 1;
                            }
                            int hoursWorked = person.getHoursWorkedWeekly();
                            if (hoursWorked > hoursWorkedPerWeek1) {
                                hoursWorkedPerWeek2 = hoursWorkedPerWeek1;
                                hoursWorkedPerWeek1 = hoursWorked;
                            } else if (hoursWorked > hoursWorkedPerWeek2) {
                                hoursWorkedPerWeek2 = hoursWorked;
                            }
                            if (agePerson >= Parameters.AGE_TO_BECOME_RESPONSIBLE) {
                                int dlltsd = person.getDlltsd();
                                if (dlltsd > dlltsd1) {
                                    dlltsd2 = dlltsd1;
                                    dlltsd1 = dlltsd;
                                } else if (dlltsd > dlltsd2) {
                                    dlltsd2 = dlltsd;
                                }
                            }
                            int cphere = person.getCarer();
                            if (cphere>careProvision)
                                careProvision = cphere;
                        } else {
                            ageTest = Math.max(ageTest, agePerson);
                        }
                    }
                    if (!flagInitialiseDemographics && ageTest!=age)
                        throw new RuntimeException("Demographic characteristics vary across system years derived from EUROMOD");
                    flagInitialiseDemographics = false;
                    double secondIncome = Math.max(0.0, origIncome - principalIncome);
                    DonorTaxUnitPolicy taxUnitPolicy = taxUnit.getPolicyByFromYear(fromYear);
                    taxUnitPolicy.setSystemYear(systemYear);
                    if (numberMembersOver17==1 || numberMembersOver17==2) {

                        // evaluate donor keys
                        double originalIncomePerWeek = origIncome / Parameters.WEEKS_PER_MONTH;
                        double childcareCostPerWeek = childcare / Parameters.WEEKS_PER_MONTH;
                        double secondIncomePerWeek = secondIncome / Parameters.WEEKS_PER_MONTH;
                        DonorKeys keys = new DonorKeys();
                        KeyFunction keyFunction = new KeyFunction(systemYear, systemYear, age, numberMembersOver17, numberChildrenUnder5,
                                numberChildren5To9, numberChildren10To17, hoursWorkedPerWeek1, hoursWorkedPerWeek2, dlltsd1, dlltsd2,
                                careProvision, originalIncomePerWeek, secondIncomePerWeek, childcareCostPerWeek);
                        keys.evaluate(keyFunction);

                        // set all taxUnitPolicy attributes
                        taxUnitPolicy.setOriginalIncomePerMonth(origIncome);
                        taxUnitPolicy.setEarningsPerMonth(earnings);
                        taxUnitPolicy.setDisposableIncomePerMonth(dispIncome);
                        taxUnitPolicy.setBenMeansTestPerMonth(benmt);
                        taxUnitPolicy.setBenNonMeansTestPerMonth(bennt);
                        taxUnitPolicy.setSecondIncomePerMonth(secondIncome);
                        taxUnitPolicy.setChildcareCostPerMonth(childcare);
                        for(int ii=0; ii<Parameters.TAXDB_REGIMES; ii++) {
                            taxUnitPolicy.setDonorKey(ii, keys.getKey(ii));
                        }
                    } else {

                        for(int ii=0; ii<Parameters.TAXDB_REGIMES; ii++) {
                            taxUnitPolicy.setDonorKey(ii, -1);
                        }
                    }

                    em.persist(taxUnitPolicy);
                }
                em.persist(taxUnit);
                if ((counter % decile)==0) {
                    int dec = counter/decile;
                    System.out.println("Completed processing decile " + dec + " of donor database");
                }
            }

            // close connection
            txn.commit();
            em.close();
        } catch (Exception e) {
            if (txn != null) {
                txn.rollback();
            }
            e.printStackTrace();
            throw new RuntimeException("Problem populating tax unit database for imputing tax and benefit payments");
        }

        // remove message box
        if (csvFrame != null)
            csvFrame.setVisible(false);
    }
}
