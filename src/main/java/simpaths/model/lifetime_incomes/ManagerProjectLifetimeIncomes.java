package simpaths.model.lifetime_incomes;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import microsim.data.db.Experiment;
import org.apache.log4j.Logger;
import simpaths.data.Parameters;
import simpaths.model.enums.Gender;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

public class ManagerProjectLifetimeIncomes {


    /**
     * ENTRY POINT FOR MANAGER
     */
    public static void run(Logger log, Integer startBirthYear, Integer endBirthYear,
                           Integer endAge, Integer simCohortSize, boolean writeToCSV, long seed, double age0StdDev) {

        log.info("Initialising lifetime income projections");
        System.out.println("Initialising lifetime income projections");

        // start projecting lifetime incomes
        initialiseLifetimeIncomeDatabase();
        RandomGenerator generator = new Random(seed);

        for (int by = startBirthYear; by <= endBirthYear; by++) {
            // loop over years

            log.info("Projecting lifetime incomes for birth year " + by);
            System.out.println("Projecting lifetime incomes for birth year " + by);

            // initialise sets for csv reporting
            Set<BirthCohort> cohorts = new LinkedHashSet<>();
            Set<Individual> individuals = new LinkedHashSet<>();
            Set<AnnualIncome> annualIncomes = new LinkedHashSet<>();

            for (Gender gender : Gender.values()) {

                BirthCohort birthCohort = new BirthCohort(by, gender, endAge);
                cohorts.add(birthCohort);
                Individual[] individualsLocal = new Individual[simCohortSize];
                AnnualIncome[] annualIncomesLocal = new AnnualIncome[simCohortSize*(endAge+1)];

                final int birthYear = by;
                IntStream.range(0, simCohortSize).parallel().forEach(ii -> {
//                for (int ii=0; ii < simCohortSize; ii++) {
                    // loop over new individuals

                    Individual individual = new Individual(birthCohort);
                    individualsLocal[ii] = individual;

                    // define incomes
                    for (int aa = 0; aa <= endAge; aa++) {

                        double rnd = generator.nextDouble();
                        AnnualIncome income = new AnnualIncome(birthYear + aa, individual, rnd, age0StdDev);
                        annualIncomesLocal[ii*(endAge+1)+aa] = income;
                    }
//                }
                });
                for (Individual individual : individualsLocal) {
                    birthCohort.addIndividual(individual);
                    individuals.add(individual);
                }
                annualIncomes.addAll(Arrays.asList(annualIncomesLocal));

                // commit to database
                writeLifetimeIncomeDatabase(log, birthCohort);
            }

            // write to csv
            if (writeToCSV) {
                ExportCSV exportCohorts = new ExportCSV(by, cohorts);
                ExportCSV exportIndividuals = new ExportCSV(by, individuals);
                ExportCSV exportAnnualIncomes = new ExportCSV(by, annualIncomes);
            }
        }

        log.info("Completed lifetime income projections");
    }

    private static void writeLifetimeIncomeDatabase(Logger log, BirthCohort cohort) {

        EntityTransaction txn = null;
        try {
            // initialise database for storing results
            String fileName = Parameters.getInputDirectory() + "input";
            Map propertyMap = new HashMap();
            propertyMap.put("hibernate.connection.url", "jdbc:h2:file:" + fileName + ";TRACE_LEVEL_FILE=0;TRACE_LEVEL_SYSTEM_OUT=0;AUTO_SERVER=TRUE");
            EntityManager em = Persistence.createEntityManagerFactory("lifetime-incomes", propertyMap).createEntityManager();
            txn = em.getTransaction();
            txn.begin();
            log.info("Running em.persist()");
            em.persist(cohort);
            txn.commit();
            em.close();
        } catch (Exception e) {
            if (txn != null)
                txn.rollback();
            e.printStackTrace();
            throw new RuntimeException("Problem writing lifetime incomes dataset");
        }
    }

    private static void initialiseLifetimeIncomeDatabase() {

        // initialise database connection
        Connection conn = null;
        try {
            String fileName = Parameters.getInputDirectory() + "input";
            Class.forName("org.h2.Driver");
            conn = DriverManager.getConnection("jdbc:h2:file:" + fileName + ";TRACE_LEVEL_FILE=0;TRACE_LEVEL_SYSTEM_OUT=0;AUTO_SERVER=TRUE", "sa", "");

            // check that database is empty at start
            Statement stat = null;
            try {
                stat = conn.createStatement();
                stat.execute( "DROP TABLE IF EXISTS birthcohort CASCADE;");
                stat.execute( "DROP TABLE IF EXISTS individual CASCADE;");
                stat.execute( "DROP TABLE IF EXISTS annualincome CASCADE;");
                stat.execute( "CREATE TABLE birthcohort (ID BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT, BIRTH_YEAR INT DEFAULT 2019, END_AGE INT DEFAULT 80, GENDER VARCHAR_IGNORECASE DEFAULT 'Male');");
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
