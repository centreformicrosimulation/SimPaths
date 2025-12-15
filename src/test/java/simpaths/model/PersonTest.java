package simpaths.model;

import microsim.statistics.regression.BinomialRegression;
import org.junit.jupiter.api.*;
import simpaths.data.ManagerRegressions;
import simpaths.data.Parameters;

import static org.junit.jupiter.api.Assertions.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import simpaths.model.enums.Country;
import simpaths.model.enums.Gender;
import simpaths.model.enums.Les_c4;
import simpaths.model.enums.Region;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@DisplayName("Person class")
public class PersonTest {

    // Static mock reference for Parameters
    private MockedStatic<Parameters> parametersMock;
    private MockedStatic<ManagerRegressions> managerRegressionsMock;

    // --- Mocks for Dependencies ---
    private SimPathsModel mockModel;
    private Innovations mockInnovations;
    private BenefitUnit mockBenefitUnit;
    private Household mockHousehold;

    // Using the actual regression types for strong type checking and accuracy
    private BinomialRegression mockBinomialRegression;

    // Cohabitation and partnership constants
    private final int MIN_AGE_COHABITATION = 18;
    private final Double INNOVATION_TO_PARTNER = 0.5;
    private final Double PROBABILITY_TO_PARTNER = 0.7;
    private final Double NEGATE_PROBABILITY_TO_PARTNER = 0.2;

    // --- Test Objects ---
    static Person testPerson;
    static Person testPartner;
    static BenefitUnit testBenefitUnit;
    static Household testHousehold;
    static Map<Gender, LinkedHashMap<Region, Set<Person>>> expectedPersonsToMatch;

    /**
     * Helper to mock static dependencies that are called inside the Person constructor,
     * specifically to prevent the NullPointerException related to Parameter initialization.
     */
    private void mockStaticDependenciesForConstructor(Runnable action) {
        // Mock the multivariate distribution needed by Person constructor's setMarriageTargets()
        double[] mockWageAgeValues = new double[]{0.0, 0.0};

        // Using explicit lambda call for robust static stubbing
        parametersMock.when(() -> Parameters.getWageAndAgeDifferentialMultivariateNormalDistribution(Mockito.anyLong()))
                .thenReturn(mockWageAgeValues);

        // Execute the rest of the setup (including new Person() and field injection)
        action.run();
    }

    // Reflection utility to set final/private fields for test isolation
    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Nested
    @DisplayName("Testing Cohabitation and Partnership Dissolution")
    class CohabitationTests {

        /**
         * Setup method to be executed before each test method.
         */
        @BeforeEach
        public void setUp() throws Exception {

            // Static mock setup first
            parametersMock = Mockito.mockStatic(Parameters.class);
            parametersMock.when(Parameters::getRegPartnershipU1a)
                    .thenReturn(mockBinomialRegression);

            // Static mock setup for ManagerRegressions
            managerRegressionsMock = Mockito.mockStatic(ManagerRegressions.class);

            // We wrap the entire setup in a mock block to handle the static dependency called by the Person constructor.
            mockStaticDependenciesForConstructor(() -> {
                try {
                    // 1. Initialize Mocks
                    mockModel = Mockito.mock(SimPathsModel.class);
                    mockInnovations = Mockito.mock(Innovations.class);
                    mockBenefitUnit = Mockito.mock(BenefitUnit.class);
                    mockHousehold = Mockito.mock(Household.class);

                    // Initialize regression mocks with specific types
                    mockBinomialRegression = Mockito.mock(BinomialRegression.class);

                    // 2. Set up basic predictable environment
                    Mockito.when(mockModel.getYear()).thenReturn(2025);
                    Mockito.when(mockModel.getCountry()).thenReturn(Country.UK);
                    Mockito.when(mockModel.isAlignCohabitation()).thenReturn(false);
                    Mockito.when(mockBenefitUnit.getHousehold()).thenReturn(mockHousehold);
                    Mockito.when(mockHousehold.getId()).thenReturn(1L);

                    // 3. Initialize Person and inject Mocks using Reflection
                    testPerson = new Person(1L, 123L);
                    testPartner = new Person(2L, 123L);
                    testBenefitUnit = new BenefitUnit(100L, 123L);
                    testHousehold = new Household(1000L);
                    testBenefitUnit.setHousehold(testHousehold);

                    expectedPersonsToMatch = new LinkedHashMap<>();
                    for (Gender gender: Gender.values()) {
                        expectedPersonsToMatch.put(gender, new LinkedHashMap<>());
                        for (Region region: Region.values()) {
                            expectedPersonsToMatch.get(gender).put(region, new LinkedHashSet<>());
                        }
                    }

                    Mockito.when(mockModel.getPersonsToMatch()).thenReturn(expectedPersonsToMatch);

                    setPrivateField(testPerson, "model", mockModel);
                    setPrivateField(testPerson, "innovations", mockInnovations);
                    setPrivateField(testPerson, "benefitUnit", mockBenefitUnit);

                    // Mock the critical dependency from BenefitUnit (Set<Person> is empty for simplicity)
                    Mockito.when(mockBenefitUnit.getChildren()).thenReturn(java.util.Collections.emptySet());
                } catch (Exception e) {
                    throw new RuntimeException("Setup failed during initialization or field injection.", e);
                }
            });
        }

        @AfterEach
        public void tearDown() throws Exception {
            if (parametersMock != null) {
                parametersMock.close();
            }
            if (managerRegressionsMock != null) {
                managerRegressionsMock.close();
            }
        }

        @Test
        @DisplayName("OUTCOME A: Person under 18 does not enter partnership")
        public void under18DoesntEnterPartnership() {
            testPerson.setDag(17);

            testPerson.cohabitation();

            assertEquals(17, testPerson.getDag());
            assertFalse(testPerson.isPartnered());
            assertFalse(testPerson.isToBePartnered());
            assertEquals(0, mockModel.getPersonsToMatch().get(Gender.Female).get(Region.UKD).size());
        }

        @Test
        @DisplayName("OUTCOME B: Female over 18, student and partnered stays partnered")
        public void over18StudentPartneredStaysPartnered() {
            testPerson.setDag(20);
            testPerson.setLes_c4(Les_c4.Student);

            testPartner.setDag(20);

            testPerson.setBenefitUnit(testBenefitUnit);
            testPartner.setBenefitUnit(testBenefitUnit);

            testPerson.cohabitation();
            testPerson.partnershipDissolution();

            assertEquals(20, testPerson.getDag(), "Person's age should not have changed.");
            assertTrue(testPerson.isPartnered(), "Person should be partnered.");
            assertFalse(testPerson.isToBePartnered(), "Person should not be to be partnered.");
            assertEquals(0, mockModel.getPersonsToMatch().get(Gender.Female).get(Region.UKD).size(), "Persons to match should be empty.");

        }

        @Test
        @DisplayName("OUTCOME C: Aged 18-29, student and not partnered - estimate to be partnered")
        public void over18StudentToBePartnered() {
            testPerson.setDag(20);
            testPerson.setLes_c4(Les_c4.Student);
            testPerson.setLeftEducation(false);
            testPerson.setDgn(Gender.Female);
            testPerson.setBenefitUnit(testBenefitUnit);

            testBenefitUnit.setRegion(Region.UKD);


            parametersMock.when(() -> Parameters.getRegPartnershipU1a()).thenReturn(mockBinomialRegression);
            Mockito.when(mockBinomialRegression.getProbability(Mockito.anyDouble())).thenReturn(PROBABILITY_TO_PARTNER);
            Mockito.when(mockInnovations.getDoubleDraw(25)).thenReturn(INNOVATION_TO_PARTNER);

            testPerson.cohabitation();
            testPerson.partnershipDissolution();

            assertEquals(20, testPerson.getDag(), "Person's age should not have changed.");
            assertFalse(testPerson.isPartnered(), "Person should not yet be partnered.");
            assertTrue(testPerson.isToBePartnered(), "Person should be set to be partnered.");
            assertEquals(1, mockModel.getPersonsToMatch().get(Gender.Female).get(Region.UKD).size(), "One person should be in persons to match.");
            assertEquals(testPerson, mockModel.getPersonsToMatch().get(Gender.Female).get(Region.UKD).stream().findFirst().get(), "Person should be in persons to match.");

        }

        @Test
        @DisplayName("OUTCOME D: Aged 18-29, student and not partnered - estimate not to be partnered")
        public void over18StudentNotToBePartnered() {
            testPerson.setDag(20);
            testPerson.setLes_c4(Les_c4.Student);
            testPerson.setLeftEducation(false);
            testPerson.setDgn(Gender.Female);
            testPerson.setBenefitUnit(testBenefitUnit);

            testBenefitUnit.setRegion(Region.UKD);


            parametersMock.when(() -> Parameters.getRegPartnershipU1a()).thenReturn(mockBinomialRegression);
            Mockito.when(mockBinomialRegression.getProbability(Mockito.anyDouble())).thenReturn(NEGATE_PROBABILITY_TO_PARTNER);
            Mockito.when(mockInnovations.getDoubleDraw(25)).thenReturn(INNOVATION_TO_PARTNER);

            testPerson.cohabitation();
            testPerson.partnershipDissolution();

            assertEquals(20, testPerson.getDag(), "Person's age should not have changed.");
            assertFalse(testPerson.isPartnered(), "Person should not yet be partnered.");
            assertFalse(testPerson.isToBePartnered(), "Person should not be set to be partnered.");
            assertEquals(0, mockModel.getPersonsToMatch().get(Gender.Female).get(Region.UKD).size(), "Persons to match should be empty.");

        }

        @Test
        @DisplayName("OUTCOME C: Over 29 and not partnered - estimate to be partnered")
        public void over29StudentToBePartnered() {
            testPerson.setDag(30);
            testPerson.setDgn(Gender.Female);
            testPerson.setBenefitUnit(testBenefitUnit);

            testBenefitUnit.setRegion(Region.UKD);


            parametersMock.when(() -> Parameters.getRegPartnershipU1b()).thenReturn(mockBinomialRegression);
            Mockito.when(mockBinomialRegression.getProbability(Mockito.anyDouble())).thenReturn(PROBABILITY_TO_PARTNER);
            Mockito.when(mockInnovations.getDoubleDraw(25)).thenReturn(INNOVATION_TO_PARTNER);

            testPerson.cohabitation();
            testPerson.partnershipDissolution();

            assertEquals(30, testPerson.getDag(), "Person's age should not have changed.");
            assertFalse(testPerson.isPartnered(), "Person should not yet be partnered.");
            assertTrue(testPerson.isToBePartnered(), "Person should be set to be partnered.");
            assertEquals(1, mockModel.getPersonsToMatch().get(Gender.Female).get(Region.UKD).size(), "One person should be in persons to match.");
            assertEquals(testPerson, mockModel.getPersonsToMatch().get(Gender.Female).get(Region.UKD).stream().findFirst().get(), "Person should be in persons to match.");

        }

        @Test
        @DisplayName("OUTCOME D: Over 29 and not partnered - estimate not to be partnered")
        public void over29StudentNotToBePartnered() {
            testPerson.setDag(30);
            testPerson.setDgn(Gender.Female);
            testPerson.setBenefitUnit(testBenefitUnit);

            testBenefitUnit.setRegion(Region.UKD);


            parametersMock.when(() -> Parameters.getRegPartnershipU1b()).thenReturn(mockBinomialRegression);
            Mockito.when(mockBinomialRegression.getProbability(Mockito.anyDouble())).thenReturn(NEGATE_PROBABILITY_TO_PARTNER);
            Mockito.when(mockInnovations.getDoubleDraw(25)).thenReturn(INNOVATION_TO_PARTNER);

            testPerson.cohabitation();
            testPerson.partnershipDissolution();

            assertEquals(30, testPerson.getDag(), "Person's age should not have changed.");
            assertFalse(testPerson.isPartnered(), "Person should not yet be partnered.");
            assertFalse(testPerson.isToBePartnered(), "Person should not be set to be partnered.");
            assertEquals(0, mockModel.getPersonsToMatch().get(Gender.Female).get(Region.UKD).size(), "Persons to match should be empty.");

        }

        @Test
        @DisplayName("OUTCOME E: Over 29 or non-student or left education - estimate remain with partner")
        public void over29RemainWithPartner() {

            testPerson.setDag(30);
            testPerson.setDgn(Gender.Female);
            testPartner.setDag(20);
            testPartner.setDgn(Gender.Male);


            testPerson.setBenefitUnit(testBenefitUnit);
            testPartner.setBenefitUnit(testBenefitUnit);

            testBenefitUnit.setRegion(Region.UKD);

            assertTrue(testPerson.isPartnered(), "Person should start partnered.");
            assertTrue(testPartner.isPartnered(), "Partner should start partnered.");

            parametersMock.when(() -> Parameters.getRegPartnershipU2b()).thenReturn(mockBinomialRegression);
            Mockito.when(mockBinomialRegression.getProbability(Mockito.anyDouble())).thenReturn(NEGATE_PROBABILITY_TO_PARTNER);
            Mockito.when(mockInnovations.getDoubleDraw(25)).thenReturn(INNOVATION_TO_PARTNER);

            testPerson.cohabitation();
            testPerson.partnershipDissolution();

            assertEquals(30, testPerson.getDag(), "Person's age should not have changed.");
            assertEquals(true, testPerson.isPartnered(), "Person should still be partnered.");
            assertEquals(true, testPartner.isPartnered(), "Partner should still be partnered.");
            assertEquals(false, testPerson.isToBePartnered(), "Person should not be to be partnered.");
            assertEquals(0, mockModel.getPersonsToMatch().get(Gender.Female).get(Region.UKD).size(), "Persons to match should be empty.");
            assertEquals(0, mockModel.getPersonsToMatch().get(Gender.Male).get(Region.UKD).size(), "Persons to match should be empty.");

        }

        @Test
        @DisplayName("OUTCOME E: Over 29 or non-student or left education - estimate leave partner")
        public void over29LeavesPartner() {

            testPerson.setDag(30);
            testPerson.setDgn(Gender.Female);
            testPartner.setDag(20);
            testPartner.setDgn(Gender.Male);


            testPerson.setBenefitUnit(testBenefitUnit);
            testPartner.setBenefitUnit(testBenefitUnit);

            testBenefitUnit.setRegion(Region.UKD);

            assertEquals(true, testPerson.isPartnered(), "Person should start partnered.");
            assertEquals(true, testPartner.isPartnered(), "Partner should start partnered.");

            parametersMock.when(() -> Parameters.getRegPartnershipU2b()).thenReturn(mockBinomialRegression);
            Mockito.when(mockBinomialRegression.getProbability(Mockito.anyDouble())).thenReturn(PROBABILITY_TO_PARTNER);
            Mockito.when(mockInnovations.getDoubleDraw(25)).thenReturn(INNOVATION_TO_PARTNER);

            testPerson.cohabitation();
            testPerson.partnershipDissolution();

            assertEquals(30, testPerson.getDag(), "Person's age should not have changed.");
            assertFalse(testPerson.isPartnered(), "Person should no longer be partnered.");
            assertFalse(testPartner.isPartnered(), "Partner should no longer be partnered.");
            assertFalse(testPerson.isToBePartnered(), "Person should not be to be partnered.");
            assertEquals(0, mockModel.getPersonsToMatch().get(Gender.Female).get(Region.UKD).size(), "Persons to match should be empty.");
            assertEquals(0, mockModel.getPersonsToMatch().get(Gender.Male).get(Region.UKD).size(), "Persons to match should be empty.");

        }

    }



    @Nested
    @DisplayName("EQ5D process")
    class Eq5dTests {

        @BeforeEach
        void setup() {

            testPerson = new Person(true);
            testPerson.setDag(30);
        }

        @Nested
        @DisplayName("With eq5dConversionParameters set to 'lawrence'")
        class WithLawrenceParameters {

            @BeforeEach
            public void setupLawrenceCoefficients() {

                Parameters.setInputDirectory("src/test/java/simpaths/testinput");

                Parameters.eq5dConversionParameters = "lawrence";
                Parameters.loadEQ5DParameters("UK");

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

                Parameters.setInputDirectory("src/test/java/simpaths/testinput");

                Parameters.eq5dConversionParameters = "franks";
                Parameters.loadEQ5DParameters("UK");

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


}
