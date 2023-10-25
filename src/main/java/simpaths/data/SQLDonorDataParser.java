package simpaths.data;


import javax.swing.*;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import simpaths.model.enums.Country;
import simpaths.model.enums.Region;


/**
 *
 * CLASS TO MANAGE TRANSLATION OF CSV DATA FROM EUROMOD TO DATABASE FOR DONORS USED TO IMPUTE TAX AND BENEFIT PAYMENTS
 * csv data are processed and saved to the DONORPERSON_<country code> table in the relational database. These data
 * are used as working variables to construct the DONORTAXUNIT_<country code> table, which is then used exclusively \
 * for imputing tax and benefit payments, drawing heavily on SQL calls made via Hibernate
 *
 */
public class SQLDonorDataParser {


    /**
     * ENTRY POINT FOR MANAGER
     *
     * @param country country object, defines the country code
     * @param startYear first simulated year
     *
     * THE MANAGER IS 'run' FROM SimPathsStart
     */
    public static void run(Country country, int startYear) {

        // display a dialog box to let the user know what is happening
        String title = "Creating donor database tables";
        String text = "<html><h2 style=\"text-align: center; font-size:120%; padding: 10pt\">"
                + "Constructing donor database tables for imputing tax and benefit payments</h2></html>";
        JFrame csvFrame = FormattedDialogBox.create(title, text, 800, 120, null, false, false);

        // initialise tax database
        Parameters.setCountryBenefitUnitName(); //Specify names of benefit unit variables in EUROMOD

        // establish database connection
        Connection conn = null;
        try {
            Class.forName("org.h2.Driver");
            conn = DriverManager.getConnection("jdbc:h2:file:./input" + File.separator + "input;TRACE_LEVEL_FILE=0;TRACE_LEVEL_SYSTEM_OUT=0;CACHE_SIZE=2097152;AUTO_SERVER=TRUE", "sa", "");

            // method to create the donor person tables
            createDonorPersonTables(conn, country, startYear);

            // method to initialise donor tax unit tables
            createDonorTaxUnitTables(conn, country);

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
        csvFrame.setVisible(false);
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
    private static void createDonorPersonTables(Connection conn, Country country, int startYear) {

        // file for importing csv data
        String taxDonorInputFileName = Parameters.getTaxDonorInputFileName();
        String donorInputFileLocation = Parameters.INPUT_DIRECTORY + taxDonorInputFileName + ".csv";

        // create temporary table for manipulating data
        Statement stat = null;
        try {
            stat = conn.createStatement();
            stat.execute(

                //Refresh table
                "DROP TABLE IF EXISTS " + taxDonorInputFileName + ";"

                //Create new database table by reading in from population_country.csv file
                + "CREATE TABLE " + taxDonorInputFileName + " AS SELECT * FROM CSVREAD('" + donorInputFileLocation + "');"
            );

            //---------------------------------------------------------------------------
            //	DonorPerson table
            //---------------------------------------------------------------------------
            String	tableName = "DONORPERSON_" + country;

            // Ensure no duplicate column names
            Set<String> inputPersonStaticColumnNames = new LinkedHashSet<>(Arrays.asList(Parameters.DONOR_STATIC_VARIABLES));
            inputPersonStaticColumnNames.add((String) Parameters.getBenefitUnitVariableNames().getValue(country.getCountryName()));

            // begin SQL translation for person table - start with policy invariant variables (DONOR_STATIC_VARIABLES)
            stat.execute(

                "DROP TABLE IF EXISTS " + tableName + " CASCADE;"
                + "CREATE TABLE " + tableName + " AS (SELECT " + stringAppender(inputPersonStaticColumnNames) + " FROM " + taxDonorInputFileName + ");"

                //Add id column
                + "ALTER TABLE " + tableName + " ALTER COLUMN IDPERSON RENAME TO ID;"
                + "ALTER TABLE " + tableName + " ALTER COLUMN ID BIGINT NOT NULL;"
                + "ALTER TABLE " + tableName + " ADD PRIMARY KEY (ID);"

                //Add rest of PanelEntityKey
                + "ALTER TABLE " + tableName + " ADD COLUMN SIMULATION_TIME INT DEFAULT " + startYear + ";"
                + "ALTER TABLE " + tableName + " ADD COLUMN SIMULATION_RUN INT DEFAULT 0;"

                //Reclassify EUROMOD variables
                // may need to change data structure type otherwise SQL conversion error, so create new column of the correct type,
                // map data from old column and drop old column

                //Age
                + "ALTER TABLE " + tableName + " ALTER COLUMN DAG int NOT NULL;"

                //Country
                + "ALTER TABLE " + tableName + " ADD COUNTRY VARCHAR_IGNORECASE;"
                + "UPDATE " + tableName + " SET COUNTRY = '" + country + "' WHERE DCT = " + country.getEuromodCountryCode() + ";"
                + "ALTER TABLE " + tableName + " DROP COLUMN DCT;"

                //Education
                + "ALTER TABLE " + tableName + " ADD EDUCATION VARCHAR_IGNORECASE;"
                + "UPDATE " + tableName + " SET EDUCATION = 'Low' WHERE deh < 2;"
                + "UPDATE " + tableName + " SET EDUCATION = 'Medium' WHERE deh >= 2 AND deh < 5;"
                + "UPDATE " + tableName + " SET EDUCATION = 'High' WHERE deh = 5;"
                + "ALTER TABLE " + tableName + " DROP COLUMN deh;"

                //Gender
                + "ALTER TABLE " + tableName + " ADD GENDER VARCHAR_IGNORECASE;"
                + "UPDATE " + tableName + " SET GENDER = 'Female' WHERE DGN = 0;"
                + "UPDATE " + tableName + " SET GENDER = 'Male' WHERE DGN = 1;"
                + "ALTER TABLE " + tableName + " DROP COLUMN DGN;"
                + "ALTER TABLE " + tableName + " ALTER COLUMN GENDER RENAME TO DGN;"
            );
            stat.execute(

                //Weights
                "ALTER TABLE " + tableName + " ALTER COLUMN DWT RENAME TO WEIGHT;"
                + "ALTER TABLE " + tableName + " ALTER COLUMN WEIGHT double;"

                //Labour Market Economic Status
                + "ALTER TABLE " + tableName + " ADD ACTIVITY_STATUS VARCHAR_IGNORECASE;"
                + "UPDATE " + tableName + " SET ACTIVITY_STATUS = 'EmployedOrSelfEmployed' WHERE LES >= 1 AND LES <= 3;"
                + "UPDATE " + tableName + " SET ACTIVITY_STATUS = 'Student' WHERE LES = 0 OR LES = 6;"
                + "UPDATE " + tableName + " SET ACTIVITY_STATUS = 'Retired' WHERE LES = 4;"
                + "UPDATE " + tableName + " SET ACTIVITY_STATUS = 'NotEmployed' WHERE LES = 5 OR LES >= 7;"

                //Health
                + "ALTER TABLE " + tableName + " ADD HEALTH VARCHAR_IGNORECASE;"
                + "UPDATE " + tableName + " SET HEALTH = 'Good' WHERE LES != 8;"
                + "UPDATE " + tableName + " SET HEALTH = 'Poor' WHERE LES = 8;"
                + "ALTER TABLE " + tableName + " DROP COLUMN LES;"

                //Long-term sick and disabled
                + "ALTER TABLE " + tableName + " ALTER COLUMN DDI int;"
                + "ALTER TABLE " + tableName + " ALTER COLUMN DDI RENAME TO DLLTSD;"

                //Labour hours
                //XXX: Could set " + Parameters.HOURS_WORKED_WEEKLY + ", earnings, labour cost etc. to 0 if retired.
                // However, the data does not conform - see idperson 101, who is retired pensioner aged 80, but who declares lhw = 40
                // i.e. works 40 hours per week and has a sizeable earnings and employer social contributions.
                + "ALTER TABLE " + tableName + " ALTER COLUMN LHW int;"
                + "ALTER TABLE " + tableName + " ALTER COLUMN LHW RENAME TO " + Parameters.HOURS_WORKED_WEEKLY.toUpperCase() + ";"

                //Region
                + "ALTER TABLE " + tableName + " ADD REGION VARCHAR_IGNORECASE;"

                //adjust name for tax unit identifier
                + "ALTER TABLE " + tableName + " ALTER COLUMN " + Parameters.getBenefitUnitVariableNames().getValue(country.getCountryName()) + " RENAME TO TUID;"
                + "ALTER TABLE " + tableName + " ALTER COLUMN TUID BIGINT NOT NULL;"
            );

            //Region - See Region class for mapping definitions and sources of info
            Parameters.setCountryRegions(country);
            for(Region region: Parameters.getCountryRegions()) {
                stat.execute(
                    "UPDATE " + tableName + " SET REGION = '" + region + "' WHERE DRGN1 = " + region.getDrgn1EUROMODvariable() + ";"
                );
            }
            stat.execute( "ALTER TABLE " + tableName + " DROP COLUMN DRGN1;");

            //Set zeros to null where relevant
            stat.execute(
                // Use yem and yse to define workStatus for non-civil-servants, i.e. those with lcs = 0?
                // If the person has absolute self-employment income > absolute employment income, define workStatus
                // enum as Self_Employed as self-employment income or loss has a bigger effect on personal wealth
                // than employment income (or loss).
                "ALTER TABLE " + tableName + " ADD WORK_SECTOR VARCHAR_IGNORECASE DEFAULT 'Private_Employee';"		//Here we assume by default that people are employed - this is because the MultiKeyMaps holding households have work_sector as a key, and cannot handle null values for work_sector. TODO: Need to check that this assumption is OK.
                + "ALTER TABLE " + tableName + " ALTER COLUMN YEM DOUBLE;"
                + "ALTER TABLE " + tableName + " ALTER COLUMN YSE DOUBLE;"

                //TODO: Check whether we should re-install the check of activity_status = 'Employed' for definitions below, and potentially add a 'Null' value to handle cases where people are not employed.
                + "UPDATE " + tableName + " SET WORK_SECTOR = 'Self_Employed' WHERE abs(YEM) < abs(YSE);"		//Size of earnings derived from self-employment income (including declared self-employment losses) is larger than employment income (or loss - although while yse is sometimes negative, I'm not sure if yem is ever negative), so define as self-employed.
                + "UPDATE " + tableName + " SET WORK_SECTOR = 'Public_Employee' WHERE LCS = 1;"		//Lastly, regardless of yem or yse, if lcs = 1, indicates person is s civil servant so overwrite any value with 'Public_Employee' work_sector value.

                //        		+ "UPDATE " + personTable + " SET earnings = yem + yse;"		//Now use EUROMOD output ils_earns, which includes more than just yem + yse, depending on the country (e.g. in Italy there is a temporary employment field yemtj 'income from co.co.co.').
                + "ALTER TABLE " + tableName + " ALTER COLUMN YEM RENAME TO EMPLOYMENT_INCOME;"
                + "ALTER TABLE " + tableName + " ALTER COLUMN YSE RENAME TO SELF_EMPLOYMENT_INCOME;"
                + "ALTER TABLE " + tableName + " DROP COLUMN LCS;"

                //Re-order by id
                + "SELECT * FROM " + tableName + " ORDER BY ID;"
            );


            //---------------------------------------------------------------------------
            //	DonorPersonPolicy table
            //---------------------------------------------------------------------------
            tableName = "DONORPERSONPOLICY_" + country;

            // initialise table
            StringBuilder varList1 = new StringBuilder("PID VARCHAR, FROM_YEAR INT, SYSTEM_YEAR INT");
            StringBuilder varList2 = new StringBuilder("PID");
            for(String variable: Parameters.DONOR_POLICY_VARIABLES) {
                varList1.append(", ").append(variable.toUpperCase()).append(" VARCHAR");
                varList2.append(", ").append(variable.toUpperCase());
            }

            stat.execute(
            "DROP TABLE IF EXISTS " + tableName + ";"
                + "CREATE TABLE " + tableName + " (ID BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT, " + varList1 + ");"
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
                    "INSERT INTO " + tableName + " (" + varList2 + ")"
                    + " SELECT " + stringAppender(inputPersonDynamicColumnNames) + " FROM " + taxDonorInputFileName + ";"
                );
                stat.execute(
                    "UPDATE " + tableName + " SET SYSTEM_YEAR = " + systemYear + " WHERE SYSTEM_YEAR IS NULL;"
                    + "UPDATE " + tableName + " SET FROM_YEAR = " + fromYear + " WHERE FROM_YEAR IS NULL;"
                );
            }
            stat.execute(
                "ALTER TABLE " + tableName + " ALTER COLUMN PID BIGINT NOT NULL;"
            );
            for(String variable: Parameters.DONOR_POLICY_VARIABLES) {
                stat.execute( "ALTER TABLE " + tableName + " ALTER COLUMN " + variable + " DOUBLE;");
            }

            //Clean-up
            stat.execute( "DROP TABLE IF EXISTS " + taxDonorInputFileName + ";");

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
     * METHOD TO INITIALISE (EMPTY) TAX UNIT TABLES
     *
     */
    private static void createDonorTaxUnitTables(Connection conn, Country country) {

        Statement stat = null;
        try {
            stat = conn.createStatement();

            //---------------------------------------------------------------------------
            //	DonorTaxUnit table
            //---------------------------------------------------------------------------
            // Set name of donor table
            String	personTableName = "DONORPERSON_" + country;
            String	taxUnitTableName = "DONORTAXUNIT_" + country;
            stat.execute(
                // make copy of person table, using tuid
                "DROP TABLE IF EXISTS TEMP;"
                + "CREATE TABLE TEMP AS (SELECT TUID, WEIGHT FROM " + personTableName + ");"

                // extract only unique values of tuid
                +"DROP TABLE IF EXISTS " + taxUnitTableName + " CASCADE;"
                + "CREATE TABLE " + taxUnitTableName + " AS SELECT DISTINCT * FROM TEMP ORDER BY TUID;"
                + "DROP TABLE IF EXISTS TEMP;"

                // establish primary key
                + "ALTER TABLE " + taxUnitTableName + " ALTER COLUMN TUID RENAME TO ID;"
                + "ALTER TABLE " + taxUnitTableName + " ALTER COLUMN ID BIGINT NOT NULL;"
                + "ALTER TABLE " + taxUnitTableName + " ADD PRIMARY KEY (ID);"
                + "SELECT * FROM " + taxUnitTableName + " ORDER BY ID;"
            );

            //---------------------------------------------------------------------------
            //	DonorTaxUnitPolicy table
            //---------------------------------------------------------------------------
            taxUnitTableName = "DONORTAXUNITPOLICY_" + country;
            StringBuilder varList = new StringBuilder("TUID LONG, FROM_YEAR INT, SYSTEM_YEAR INT");
/*
            for(String variable: Parameters.DONOR_POLICY_VARIABLES) {
                varList.append(", ").append(variable.toUpperCase()).append(" DOUBLE");
            }
*/
            stat.execute(
                "DROP TABLE IF EXISTS " + taxUnitTableName + ";"
                    + "CREATE TABLE " + taxUnitTableName + " (ID BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT, " + varList + ");"
            );
        }
        catch(SQLException e) {
            throw new IllegalArgumentException("SQL Exception thrown!" + e.getMessage());
        }
        finally {
            try {
                if(stat != null) stat.close();
            }
            catch (SQLException e) {
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
}
