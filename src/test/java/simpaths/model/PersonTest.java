package simpaths.model;

import org.junit.jupiter.api.*;
import simpaths.data.Parameters;
import simpaths.model.enums.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Person class")
public class PersonTest {

    static Person testPerson;
    static BenefitUnit testBenefitUnit;
    static Household testHousehold;

    @BeforeAll
    static void setup() {
        testPerson = new Person(true, 1);
        testBenefitUnit = new BenefitUnit(true, 1);
        testBenefitUnit.setRegion(Region.UKC);
        testHousehold= new Household();
        testBenefitUnit.setHousehold(testHousehold);
        testPerson.setBenefitUnit(testBenefitUnit);
    }

    @Nested
    @DisplayName("EQ5D process")
    class Eq5dTests {

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
            public void calculatesHighScoreCorrectly()  {


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
            public void calculatesHighScoreCorrectly(){

                testPerson.setDhe_mcs(100.);
                testPerson.setDhe_pcs(100.);

                testPerson.onEvent(Person.Processes.HealthEQ5D);

                // The maximum possible value given by the Franks coefficients
                assertEquals(0.9035601, testPerson.getHe_eq5d());

            }

        }
    }

    @Nested
    @DisplayName("Mental health stage 2")
    class MentalHealthStage2 {

        @Nested
        @DisplayName("MCS score updates")
        class MCSScoreUpdates {

            @BeforeEach
            public void setupHM2Coefficients() {
                Parameters.loadDHE_MCS2Parameters("UK", 14, 14);
            }

            @Test
            @DisplayName("Calculates valid MCS score")
            public void calculatesValidMCSScore() {

                // This test currently sets a single person with variables required for MCS2 and test whether updating produces a valid score not equal to the starting score

                testPerson.setLes_c4(Les_c4.NotEmployed);
                testPerson.setLes_c4_lag1(Les_c4.EmployedOrSelfEmployed);
                testPerson.setDlltsd(Indicator.False);

                testPerson.setReceivesBenefitsFlagUC(true);
                testPerson.setLabourSupplyWeekly(Labour.ZERO);

                testPerson.setDhe_mcs(50.);
                testPerson.setDgn(Gender.Female);

                testBenefitUnit.setAtRiskOfPoverty(1);
                testBenefitUnit.setAtRiskOfPoverty_lag1(0);

                testBenefitUnit.setYearlyChangeInLogEDI(1.);
                testBenefitUnit.setEquivalisedDisposableIncomeYearly(1000.);
                testBenefitUnit.setEquivalisedDisposableIncomeYearly_lag1(100.);

                testPerson.onEvent(Person.Processes.HealthMCS2);

                assertTrue(testPerson.getDhe_mcs() <= 100);
                assertTrue(testPerson.getDhe_mcs() >= 0);
                assertTrue(testPerson.getDhe_pcs() != 50);

            }


        }

    }


}
