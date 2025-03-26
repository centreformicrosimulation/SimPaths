package simpaths.model;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import simpaths.data.Parameters;
import simpaths.model.enums.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Person class")
public class PersonTest {

    static Person testPerson;
    static BenefitUnit testBenefitUnit;
    static Household testHousehold;


    @Nested
    @DisplayName("EQ5D process")
    class Eq5dTests {

        @BeforeAll
        static void setupPerson() {
            testPerson = new Person(true, 1, 100);
        }

        @Nested
        @DisplayName("With eq5dConversionParameters set to 'lawrence'")
        class WithLawrenceParameters {

            @BeforeEach
            public void setupLawrenceCoefficients() {

                Parameters.eq5dConversionParameters = "lawrence";
                Parameters.loadEQ5DParameters("UK", 8);

            }

            @Test
            @DisplayName("Calculates low score correctly using Lawrence and Fleishman coefficients")
            public void calculatesLowScoreCorrectly() {


                testPerson.setDhe_mcs(1.);
                testPerson.setDhe_pcs(1.);

                testPerson.onEvent(Person.Processes.HealthEQ5D);

                assertEquals(-0.594, testPerson.getHe_eq5d());

            }

            @Test
            @DisplayName("Calculates high score correctly using Lawrence and Fleishman coefficients")
            public void calculatesHighScoreCorrectly() {


                testPerson.setDhe_mcs(100.);
                testPerson.setDhe_pcs(100.);

                testPerson.onEvent(Person.Processes.HealthEQ5D);

                assertEquals(1, testPerson.getHe_eq5d());

            }

        }


        @Nested
        @DisplayName("With eq5dConversionParameters set to 'franks'")
        class WithFranksParameters {

            @BeforeEach
            public void setupFranksCoefficients() {

                Parameters.eq5dConversionParameters = "franks";
                Parameters.loadEQ5DParameters("UK", 8);

            }


            @Test
            @DisplayName("Calculates low score correctly using Franks coefficients")
            public void calculatesLowScoreCorrectly() {

                testPerson.setDhe_mcs(1.);
                testPerson.setDhe_pcs(1.);

                testPerson.onEvent(Person.Processes.HealthEQ5D);

                assertEquals(-0.594, testPerson.getHe_eq5d());

            }

            @Test
            @DisplayName("Calculates high score correctly using Franks coefficients")
            public void calculatesHighScoreCorrectly() {

                testPerson.setDhe_mcs(100.);
                testPerson.setDhe_pcs(100.);

                testPerson.onEvent(Person.Processes.HealthEQ5D);

                // The maximum possible value given by the Franks coefficients
                assertEquals(0.9035601, testPerson.getHe_eq5d());

            }

        }
    }

    @Nested
    @DisplayName("Health: MCS stage 2")
    class HealthMCSStage2 {


        public static class PersonTestData {
            final int id;
            // Existing attributes
            final Les_c4 les_c4;
            final Les_c4 les_c4_lag1;
            final Indicator dlltsd;
            final Indicator dlltsd_lag1;
            final boolean receivesBenefitsFlagUC;
            final Labour labourSupplyWeekly;
            final double dhe_mcs;
            final Gender dgn;
            final int atRiskOfPoverty;
            final int atRiskOfPoverty_lag1;
            final double yearlyChangeInLogEDI;
            final double equivalisedDisposableIncomeYearly;
            final double equivalisedDisposableIncomeYearly_lag1;
            final int buid;

            // New attributes for expected/predicted values
            final Double expectedMcsScore;  // Optional predicted MCS score

            // Constructor with additional prediction parameters
            PersonTestData(
                    int id,
                    Les_c4 les_c4,
                    Les_c4 les_c4_lag1,
                    Indicator dlltsd,
                    Indicator dlltsd_lag1,
                    boolean receivesBenefitsFlagUC,
                    Labour labourSupplyWeekly,
                    double dhe_mcs,
                    Gender dgn,
                    int atRiskOfPoverty,
                    int atRiskOfPoverty_lag1,
                    double yearlyChangeInLogEDI,
                    double equivalisedDisposableIncomeYearly,
                    double equivalisedDisposableIncomeYearly_lag1,
                    int buid,
                    Double expectedMcsScore
            ) {
                // Assign fields
                this.id = id;
                this.les_c4 = les_c4;
                this.les_c4_lag1 = les_c4_lag1;
                this.dlltsd = dlltsd;
                this.dlltsd_lag1 = dlltsd_lag1;
                this.receivesBenefitsFlagUC = receivesBenefitsFlagUC;
                this.labourSupplyWeekly = labourSupplyWeekly;
                this.dhe_mcs = dhe_mcs;
                this.dgn = dgn;
                this.atRiskOfPoverty = atRiskOfPoverty;
                this.atRiskOfPoverty_lag1 = atRiskOfPoverty_lag1;
                this.yearlyChangeInLogEDI = yearlyChangeInLogEDI;
                this.equivalisedDisposableIncomeYearly = equivalisedDisposableIncomeYearly;
                this.equivalisedDisposableIncomeYearly_lag1 = equivalisedDisposableIncomeYearly_lag1;
                this.buid = buid;
                this.expectedMcsScore = expectedMcsScore;
            }
        }

        private static Person createTestPerson(
                int id,
                Les_c4 les_c4,
                Les_c4 les_c4_lag1,
                Indicator dlltsd,
                Indicator dlltsd_lag1,
                boolean receivesBenefitsFlagUC,
                Labour labourSupplyWeekly,
                double dhe_mcs,
                Gender dgn,
                int atRiskOfPoverty,
                int atRiskOfPoverty_lag1,
                double yearlyChangeInLogEDI,
                double equivalisedDisposableIncomeYearly,
                double equivalisedDisposableIncomeYearly_lag1,
                int buid,
                int seed
        ) {
            // Create person, benefit unit, and household
            Household testHousehold = new Household();
            BenefitUnit testBenefitUnit = new BenefitUnit(true, buid);
            testBenefitUnit.setRegion(Region.UKC);
            testBenefitUnit.setHousehold(testHousehold);

            Person testPerson = new Person(true, id, seed);
            testPerson.setBenefitUnit(testBenefitUnit);

            // Set person attributes
            testPerson.setLes_c4(les_c4);
            testPerson.setLes_c4_lag1(les_c4_lag1);
            testPerson.setDlltsd(dlltsd);
            testPerson.setDlltsd_lag1(dlltsd_lag1);
            testPerson.setReceivesBenefitsFlagUC(receivesBenefitsFlagUC);
            testPerson.setLabourSupplyWeekly(labourSupplyWeekly);
            testPerson.setDhe_mcs(dhe_mcs);
            testPerson.setDgn(dgn);

            // Set benefit unit attributes
            testBenefitUnit.setAtRiskOfPoverty(atRiskOfPoverty);
            testBenefitUnit.setAtRiskOfPoverty_lag1(atRiskOfPoverty_lag1);
            testBenefitUnit.setYearlyChangeInLogEDI(yearlyChangeInLogEDI);
            testBenefitUnit.setEquivalisedDisposableIncomeYearly(equivalisedDisposableIncomeYearly);
            testBenefitUnit.setEquivalisedDisposableIncomeYearly_lag1(equivalisedDisposableIncomeYearly_lag1);

            return testPerson;
        }


        @Nested
        @DisplayName("MCS score updates")
        class MCSScoreUpdates {

            private static final List<PersonTestData> TEST_SCENARIOS = Arrays.asList(
                    new PersonTestData(
                            1,
                            Les_c4.EmployedOrSelfEmployed,
                            Les_c4.EmployedOrSelfEmployed,
                            Indicator.False,
                            Indicator.False,
                            false,
                            Labour.FORTY,
                            50.,
                            Gender.Female,
                            0,
                            0,
                            -0.103720664978027,
                            7.9143123626709,
                            8.01803302764893,
                            1,
                            49.9455851343837
                    ),
                    new PersonTestData(
                            2,
                            Les_c4.EmployedOrSelfEmployed,
                            Les_c4.EmployedOrSelfEmployed,
                            Indicator.False,
                            Indicator.False,
                            false,
                            Labour.FORTY,
                            50.,
                            Gender.Female,
                            0,
                            0,
                            -0.371922969818115,
                            7.91643095016479,
                            8.28835391998291,
                            2,
                            49.8917089752174
                    ),
                    new PersonTestData(
                            3,
                            Les_c4.EmployedOrSelfEmployed,
                            Les_c4.EmployedOrSelfEmployed,
                            Indicator.False,
                            Indicator.False,
                            false,
                            Labour.FORTY,
                            50.,
                            Gender.Female,
                            0,
                            0,
                            0.0416626930236816,
                            7.61244821548462,
                            7.57078552246094,
                            3,
                            50.008369152092
                    )
//                    new PersonTestData(
//                            4,
//                            Les_c4.EmployedOrSelfEmployed,
//                            Les_c4.EmployedOrSelfEmployed,
//                            Indicator.False,
//                            Indicator.False,
//                            true,
//                            Labour.TEN,
//                            50.,
//                            Gender.Female,
//                            1,
//                            1,
//                            0.325350761413574,
//                            6.73911380767822,
//                            6.41376304626465,
//                            4,
//                            49.9032974207386
//                    ),
//                    new PersonTestData(
//                            5,
//                            Les_c4.EmployedOrSelfEmployed,
//                            Les_c4.EmployedOrSelfEmployed,
//                            Indicator.False,
//                            Indicator.False,
//                            true,
//                            Labour.TWENTY,
//                            50.,
//                            Gender.Female,
//                            1,
//                            0,
//                            -0.369383335113525,
//                            6.5464038848877,
//                            6.91578722000122,
//                            5,
//                            49.7361645253471
//                    ),
//                    new PersonTestData(
//                            6,
//                            Les_c4.NotEmployed,
//                            Les_c4.EmployedOrSelfEmployed,
//                            Indicator.False,
//                            Indicator.False,
//                            true,
//                            Labour.ZERO,
//                            50.,
//                            Gender.Female,
//                            1,
//                            1,
//                            -0.853382587432861,
//                            5.90827131271362,
//                            6.76165390014648,
//                            6,
//                            48.2806603511376
//                    )
            );

            @BeforeEach
            public void setupHM2Coefficients() {
                Parameters.loadDHE_MCS2Parameters("UK", 14, 14);
            }

            @ParameterizedTest
            @DisplayName("Calculates valid MCS score for multiple scenarios")
            @MethodSource("getTestScenarios")
            public void calculatesValidMCSScore(PersonTestData testData) {
                // Create test person (existing logic)
                Person testPerson = createTestPerson(
                        testData.id,
                        testData.les_c4,
                        testData.les_c4_lag1,
                        testData.dlltsd,
                        testData.dlltsd_lag1,
                        testData.receivesBenefitsFlagUC,
                        testData.labourSupplyWeekly,
                        testData.dhe_mcs,
                        testData.dgn,
                        testData.atRiskOfPoverty,
                        testData.atRiskOfPoverty_lag1,
                        testData.yearlyChangeInLogEDI,
                        testData.equivalisedDisposableIncomeYearly,
                        testData.equivalisedDisposableIncomeYearly_lag1,
                        testData.buid,
                        101
                );

                // Trigger the event
                testPerson.onEvent(Person.Processes.HealthMCS2);

                // Basic range and change assertions
                assertTrue(testPerson.getDhe_mcs() <= 100);
                assertTrue(testPerson.getDhe_mcs() >= 0);
                assertTrue(testPerson.getDhe_pcs() != 50);


                // Allow some tolerance for floating-point comparisons
                assertEquals(
                        testData.expectedMcsScore,
                        testPerson.getDhe_mcs(),
                        0.1,  // Delta for floating-point comparison
                        "MCS score should match expected value"
                );

            }

            private static Stream<Arguments> getTestScenarios() {
                return TEST_SCENARIOS.stream()
                        .map(Arguments::of);
            }

        }

    }
}
