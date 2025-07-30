package simpaths.model.lifetime_incomes;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import microsim.data.db.Experiment;
import org.apache.log4j.Logger;
import simpaths.data.Parameters;
import simpaths.model.enums.Gender;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.random.RandomGenerator;

public class ManagerProjectLifetimeIncomes {


    /**
     * ENTRY POINT FOR MANAGER
     */
    public static void run(Logger log, Experiment experiment, Integer startYear, Integer endYear,
                           Integer endAge, Integer simCohortSize, boolean writeToCSV, long seed, double age0StdDev) {

        log.info("Initialising lifetime income projections");

        // initialise sets for csv reporting
        Set<BirthCohort> cohorts = new LinkedHashSet<>();
        Set<Individual> individuals = new LinkedHashSet<>();
        Set<AnnualIncome> annualIncomes = new LinkedHashSet<>();

        // start projecting lifetime incomes
        RandomGenerator generator = new Random(seed);
        Population population = new Population(startYear, endYear, endAge);

        // test
//        BirthCohort cohort = new BirthCohort(1950, Gender.Male, population);
//        Individual indiv = new Individual(cohort);
//        double rnd2 = generator.nextDouble();
//        AnnualIncome income = new AnnualIncome(1960, indiv, rnd2, age0StdDev);
        // end test

        for (int yy = startYear; yy <= endYear+endAge; yy++) {
            // loop over years

            for (int aa = 0; aa <= endAge; aa++) {
                // loop over ages

                int birthYear = yy - aa;
                BirthCohort bcMales, bcFemales;
                if (aa==0 && yy<=endYear) {

                    // initialise new birth cohorts
                    bcMales = new BirthCohort(yy, Gender.Male, population);
                    bcFemales = new BirthCohort(yy, Gender.Female, population);
                    if (writeToCSV) {
                        cohorts.add(bcMales);
                        cohorts.add(bcFemales);
                    }

                    // initialise individuals for each new birth cohort
                    for (int ii=0; ii < simCohortSize; ii++) {
                        // loop over new individuals

                        Individual male = new Individual(bcMales);
                        Individual female = new Individual(bcFemales);
                        if (writeToCSV) {
                            individuals.add(male);
                            individuals.add(female);
                        }
                    }
                }
                else {
                    bcMales = population.getBirthCohort(birthYear,Gender.Male);
                    bcFemales = population.getBirthCohort(birthYear,Gender.Female);
                }
                if (bcMales!=null) {
                    for (Individual ind : bcMales.getIndividuals()) {
                        double rnd = generator.nextDouble();
                        AnnualIncome incomeMale = new AnnualIncome(yy, ind, rnd, age0StdDev);
                        annualIncomes.add(incomeMale);
                    }
                }
                if (bcFemales!=null) {
                    for (Individual ind : bcFemales.getIndividuals()) {
                        double rnd = generator.nextDouble();
                        AnnualIncome incomeFemale = new AnnualIncome(yy, ind, rnd, age0StdDev);
                        annualIncomes.add(incomeFemale);
                    }
                }
            }
        }

        // commit to database
        writeLifetimeIncomeDatabase(log, population);

        // write to csv
        if (writeToCSV) {
            ExportCSV exportCohorts = new ExportCSV(cohorts);
            ExportCSV exportIndividuals = new ExportCSV(individuals);
            ExportCSV exportAnnualIncomes = new ExportCSV(annualIncomes);
        }
        log.info("Completed lifetime income projections");
    }

    private static void writeLifetimeIncomeDatabase(Logger log, Population population) {

        EntityTransaction txn = null;
        try {
            // initialise database for storing results
            String fileName = Parameters.getInputDirectory() + "lifetime_incomes";
            initialiseDatabase(fileName);
            Map propertyMap = new HashMap();
            propertyMap.put("hibernate.connection.url", "jdbc:h2:file:" + fileName + ";TRACE_LEVEL_FILE=0;TRACE_LEVEL_SYSTEM_OUT=0;AUTO_SERVER=TRUE");
            EntityManager em = Persistence.createEntityManagerFactory("lifetime-incomes", propertyMap).createEntityManager();
            txn = em.getTransaction();
            txn.begin();
            log.info("Running em.persist()");
            em.persist(population);
            txn.commit();
            em.close();
        } catch (Exception e) {
            if (txn != null)
                txn.rollback();
            e.printStackTrace();
            throw new RuntimeException("Problem writing lifetime incomes dataset");
        }
    }

    private static void initialiseDatabase(String fileName) {

        // initialise database connection
        Connection conn = null;
        try {
            Class.forName("org.h2.Driver");
            conn = DriverManager.getConnection("jdbc:h2:file:" + fileName + ";TRACE_LEVEL_FILE=0;TRACE_LEVEL_SYSTEM_OUT=0;AUTO_SERVER=TRUE", "sa", "");

            // check that database is empty at start
            Statement stat = null;
            try {
                stat = conn.createStatement();
                stat.execute( "DROP TABLE IF EXISTS population CASCADE;");
                stat.execute( "DROP TABLE IF EXISTS birthcohort CASCADE;");
                stat.execute( "DROP TABLE IF EXISTS individual CASCADE;");
                stat.execute( "DROP TABLE IF EXISTS income CASCADE;");
                stat.execute( "CREATE TABLE population (ID BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT, START_YEAR INT DEFAULT 2019, END_YEAR INT DEFAULT 2019, END_AGE INT DEFAULT 80);");
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
        catch(ClassNotFoundException | SQLException e){
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
    }
}
