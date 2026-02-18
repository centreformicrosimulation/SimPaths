package simpaths.model;

import microsim.statistics.regression.BinomialRegression;
import microsim.statistics.regression.GeneralisedOrderedRegression;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import simpaths.data.ManagerRegressions;
import simpaths.data.Parameters;
import simpaths.data.RegressionName;
import simpaths.model.enums.*;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Person class")
public class PersonTest {

    // Static mock reference for Parameters
    private MockedStatic<Parameters> parametersMock;
    private MockedStatic<ManagerRegressions> managerRegressionsMock;

    // ---------------------------------------------------------------------
    // Shared helpers (used by all nested suites)
    // ---------------------------------------------------------------------

    /**
     * Mock static dependencies that are called inside the Person constructor.
     * This must run before creating new Person(...) instances in tests.
     */
    private void mockStaticDependenciesForConstructor() {
        // Mock the multivariate distribution needed by Person constructor's setMarriageTargets()
        double[] mockWageAgeValues = new double[]{0.0, 0.0};

        // Using explicit lambda call for robust static stubbing
        parametersMock.when(() ->
                        Parameters.getWageAndAgeDifferentialMultivariateNormalDistribution(Mockito.anyLong()))
                .thenReturn(mockWageAgeValues);
    }

    // Reflection utility to set final/private fields for test isolation
    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /** Support both legacy/new Person innovations field names. */
    private void injectInnovations(Person p, Innovations innovations) throws Exception {
        try {
            setPrivateField(p, "statInnovations", innovations);
        } catch (NoSuchFieldException ex) {
            setPrivateField(p, "innovations", innovations);
        }
    }

    @AfterEach
    void closeStaticsIfLeftOpen() {
        // Safety net: suites below close them, but this prevents leaks on failures.
        if (parametersMock != null) {
            parametersMock.close();
            parametersMock = null;
        }
        if (managerRegressionsMock != null) {
            managerRegressionsMock.close();
            managerRegressionsMock = null;
        }
    }

    // =====================================================================
    // Cohabitation / Partnership tests
    // Main steps:
    //  1) create static mocks (Parameters + ManagerRegressions)
    //  2) stub constructor-required static calls
    //  3) create Person/BenefitUnit/Household objects + inject dependencies
    //  4) run behaviour (cohabitation / dissolution) and assert outcomes
    // =====================================================================

    @Nested
    @DisplayName("Testing Cohabitation and Partnership Dissolution")
    class CohabitationTests {

        // --- Mocks for Dependencies ---
        private SimPathsModel mockModel;
        private Innovations mockInnovations;
        private BenefitUnit mockBenefitUnit;
        private Household mockHousehold;

        // Using the actual regression types for strong type checking and accuracy
        private BinomialRegression mockBinomialRegression;

        // Cohabitation and partnership constants
        private final Double INNOVATION_TO_PARTNER = 0.5;
        private final Double PROBABILITY_TO_PARTNER = 0.7;
        private final Double NEGATE_PROBABILITY_TO_PARTNER = 0.2;

        // --- Test Objects (NON-static to avoid cross-suite pollution) ---
        private Person testPerson;
        private Person testPartner;
        private BenefitUnit testBenefitUnit;
        private Household testHousehold;
        private Map<Gender, LinkedHashMap<Region, Set<Person>>> expectedPersonsToMatch;

        /**
         * Setup method to be executed before each test method.
         */
        @BeforeEach
        public void setUp() throws Exception {

            // 1) Static mock setup first
            parametersMock = Mockito.mockStatic(Parameters.class);
            parametersMock.when(Parameters::getRegPartnershipU1)
                    .thenReturn(mockBinomialRegression);

            // Static mock setup for ManagerRegressions
            managerRegressionsMock = Mockito.mockStatic(ManagerRegressions.class);

            // 2) Stub static calls needed inside Person constructor
            mockStaticDependenciesForConstructor();

            // 3) Initialize mocks + provide stable model environment
            mockModel = Mockito.mock(SimPathsModel.class);
            mockInnovations = Mockito.mock(Innovations.class);
            mockBenefitUnit = Mockito.mock(BenefitUnit.class);
            mockHousehold = Mockito.mock(Household.class);
            mockBinomialRegression = Mockito.mock(BinomialRegression.class);

            Mockito.when(mockModel.getYear()).thenReturn(2025);
            Mockito.when(mockModel.getCountry()).thenReturn(Country.UK);
            Mockito.when(mockModel.isAlignCohabitation()).thenReturn(false);
            Mockito.when(mockBenefitUnit.getHousehold()).thenReturn(mockHousehold);
            Mockito.when(mockHousehold.getId()).thenReturn(1L);

            // 4) Create real objects used by the behaviour under test
            testPerson = new Person(1L, 123L);
            testPartner = new Person(2L, 123L);
            testBenefitUnit = new BenefitUnit(100L, 123L);
            testHousehold = new Household(1000L);
            testBenefitUnit.setHousehold(testHousehold);

            // personsToMatch structure used by matching logic
            expectedPersonsToMatch = new LinkedHashMap<>();
            for (Gender gender : Gender.values()) {
                expectedPersonsToMatch.put(gender, new LinkedHashMap<>());
                for (Region region : Region.values()) {
                    expectedPersonsToMatch.get(gender).put(region, new LinkedHashSet<>());
                }
            }
            Mockito.when(mockModel.getPersonsToMatch()).thenReturn(expectedPersonsToMatch);

            // Inject dependencies into Person (model, innovations, benefit unit)
            setPrivateField(testPerson, "model", mockModel);
            setPrivateField(testPerson, "statInnovations", mockInnovations);
            setPrivateField(testPerson, "benefitUnit", mockBenefitUnit);

            // Mock the critical dependency from BenefitUnit (Set<Person> is empty for simplicity)
            Mockito.when(mockBenefitUnit.getChildren()).thenReturn(java.util.Collections.emptySet());

            // Default partnership regression stub (overridden per test when needed)
            parametersMock.when(Parameters::getRegPartnershipU1a)
                    .thenReturn(mockBinomialRegression);
        }

        @AfterEach
        public void tearDown() {
            if (parametersMock != null) {
                parametersMock.close();
                parametersMock = null;
            }
            if (managerRegressionsMock != null) {
                managerRegressionsMock.close();
                managerRegressionsMock = null;
            }
        }

        @Test
        @DisplayName("OUTCOME A: Person under 18 does not enter partnership")
        public void under18DoesntEnterPartnership() {
            testPerson.setDemAge(17);

            testPerson.cohabitation();

            assertEquals(17, testPerson.getDemAge());
            assertFalse(testPerson.isPartnered());
            assertFalse(testPerson.isToBePartnered());
            assertEquals(0, mockModel.getPersonsToMatch().get(Gender.Female).get(Region.UKD).size());
        }

        @Test
        @DisplayName("OUTCOME B: Female over 18, student and partnered stays partnered")
        public void over18StudentPartneredStaysPartnered() {
            testPerson.setDemAge(20);
            testPerson.setLes_c4(Les_c4.Student);

            testPartner.setDemAge(20);

            testPerson.setBenefitUnit(testBenefitUnit);
            testPartner.setBenefitUnit(testBenefitUnit);

            testPerson.cohabitation();
            testPerson.partnershipDissolution();

            assertEquals(20, testPerson.getDemAge(), "Person's age should not have changed.");
            assertTrue(testPerson.isPartnered(), "Person should be partnered.");
            assertFalse(testPerson.isToBePartnered(), "Person should not be to be partnered.");
            assertEquals(0, mockModel.getPersonsToMatch().get(Gender.Female).get(Region.UKD).size(), "Persons to match should be empty.");
        }

        @Test
        @DisplayName("OUTCOME C: Aged 18-29, student and not partnered - estimate to be partnered")
        public void over18StudentToBePartnered() {
            testPerson.setDemAge(20);
            testPerson.setLes_c4(Les_c4.Student);
            testPerson.setEduLeftEduFlag(false);
            testPerson.setDemMaleFlag(Gender.Female);
            testPerson.setBenefitUnit(testBenefitUnit);

            testBenefitUnit.setRegion(Region.UKD);


            parametersMock.when(() -> Parameters.getRegPartnershipU1()).thenReturn(mockBinomialRegression);
            Mockito.when(mockBinomialRegression.getProbability(Mockito.anyDouble())).thenReturn(PROBABILITY_TO_PARTNER);
            Mockito.when(mockInnovations.getDoubleDraw(25)).thenReturn(INNOVATION_TO_PARTNER);

            testPerson.cohabitation();
            testPerson.partnershipDissolution();

            assertEquals(20, testPerson.getDemAge(), "Person's age should not have changed.");
            assertFalse(testPerson.isPartnered(), "Person should not yet be partnered.");
            assertTrue(testPerson.isToBePartnered(), "Person should be set to be partnered.");
            assertEquals(1, mockModel.getPersonsToMatch().get(Gender.Female).get(Region.UKD).size(), "One person should be in persons to match.");
            assertEquals(testPerson, mockModel.getPersonsToMatch().get(Gender.Female).get(Region.UKD).stream().findFirst().get(), "Person should be in persons to match.");
        }

        @Test
        @DisplayName("OUTCOME D: Aged 18-29, student and not partnered - estimate not to be partnered")
        public void over18StudentNotToBePartnered() {
            testPerson.setDemAge(20);
            testPerson.setLes_c4(Les_c4.Student);
            testPerson.setEduLeftEduFlag(false);
            testPerson.setDemMaleFlag(Gender.Female);
            testPerson.setBenefitUnit(testBenefitUnit);

            testBenefitUnit.setRegion(Region.UKD);


            parametersMock.when(() -> Parameters.getRegPartnershipU1()).thenReturn(mockBinomialRegression);
            Mockito.when(mockBinomialRegression.getProbability(Mockito.anyDouble())).thenReturn(NEGATE_PROBABILITY_TO_PARTNER);
            Mockito.when(mockInnovations.getDoubleDraw(25)).thenReturn(INNOVATION_TO_PARTNER);

            testPerson.cohabitation();
            testPerson.partnershipDissolution();

            assertEquals(20, testPerson.getDemAge(), "Person's age should not have changed.");
            assertFalse(testPerson.isPartnered(), "Person should not yet be partnered.");
            assertFalse(testPerson.isToBePartnered(), "Person should not be set to be partnered.");
            assertEquals(0, mockModel.getPersonsToMatch().get(Gender.Female).get(Region.UKD).size(), "Persons to match should be empty.");
        }

        @Test
        @DisplayName("OUTCOME C: Over 29 and not partnered - estimate to be partnered")
        public void over29StudentToBePartnered() {
            testPerson.setDemAge(30);
            testPerson.setDemMaleFlag(Gender.Female);
            testPerson.setBenefitUnit(testBenefitUnit);

            testBenefitUnit.setRegion(Region.UKD);


            parametersMock.when(() -> Parameters.getRegPartnershipU1()).thenReturn(mockBinomialRegression);
            Mockito.when(mockBinomialRegression.getProbability(Mockito.anyDouble())).thenReturn(PROBABILITY_TO_PARTNER);
            Mockito.when(mockInnovations.getDoubleDraw(25)).thenReturn(INNOVATION_TO_PARTNER);

            testPerson.cohabitation();
            testPerson.partnershipDissolution();

            assertEquals(30, testPerson.getDemAge(), "Person's age should not have changed.");
            assertFalse(testPerson.isPartnered(), "Person should not yet be partnered.");
            assertTrue(testPerson.isToBePartnered(), "Person should be set to be partnered.");
            assertEquals(1, mockModel.getPersonsToMatch().get(Gender.Female).get(Region.UKD).size(), "One person should be in persons to match.");
            assertEquals(testPerson, mockModel.getPersonsToMatch().get(Gender.Female).get(Region.UKD).stream().findFirst().get(), "Person should be in persons to match.");
        }

        @Test
        @DisplayName("OUTCOME D: Over 29 and not partnered - estimate not to be partnered")
        public void over29StudentNotToBePartnered() {
            testPerson.setDemAge(30);
            testPerson.setDemMaleFlag(Gender.Female);
            testPerson.setBenefitUnit(testBenefitUnit);

            testBenefitUnit.setRegion(Region.UKD);


            parametersMock.when(() -> Parameters.getRegPartnershipU1()).thenReturn(mockBinomialRegression);
            Mockito.when(mockBinomialRegression.getProbability(Mockito.anyDouble())).thenReturn(NEGATE_PROBABILITY_TO_PARTNER);
            Mockito.when(mockInnovations.getDoubleDraw(25)).thenReturn(INNOVATION_TO_PARTNER);

            testPerson.cohabitation();
            testPerson.partnershipDissolution();

            assertEquals(30, testPerson.getDemAge(), "Person's age should not have changed.");
            assertFalse(testPerson.isPartnered(), "Person should not yet be partnered.");
            assertFalse(testPerson.isToBePartnered(), "Person should not be set to be partnered.");
            assertEquals(0, mockModel.getPersonsToMatch().get(Gender.Female).get(Region.UKD).size(), "Persons to match should be empty.");
        }

        @Test
        @DisplayName("OUTCOME E: Over 29 or non-student or left education - estimate remain with partner")
        public void over29RemainWithPartner() {
            testPerson.setDemAge(30);
            testPerson.setDemMaleFlag(Gender.Female);
            testPartner.setDemAge(20);
            testPartner.setDemMaleFlag(Gender.Male);

            testPerson.setBenefitUnit(testBenefitUnit);
            testPartner.setBenefitUnit(testBenefitUnit);

            testBenefitUnit.setRegion(Region.UKD);

            assertTrue(testPerson.isPartnered(), "Person should start partnered.");
            assertTrue(testPartner.isPartnered(), "Partner should start partnered.");

            parametersMock.when(() -> Parameters.getRegPartnershipU2()).thenReturn(mockBinomialRegression);
            Mockito.when(mockBinomialRegression.getProbability(Mockito.anyDouble())).thenReturn(NEGATE_PROBABILITY_TO_PARTNER);
            Mockito.when(mockInnovations.getDoubleDraw(25)).thenReturn(INNOVATION_TO_PARTNER);

            testPerson.cohabitation();
            testPerson.partnershipDissolution();

            assertEquals(30, testPerson.getDemAge(), "Person's age should not have changed.");
            assertEquals(true, testPerson.isPartnered(), "Person should still be partnered.");
            assertEquals(true, testPartner.isPartnered(), "Partner should still be partnered.");
            assertEquals(false, testPerson.isToBePartnered(), "Person should not be to be partnered.");
            assertEquals(0, mockModel.getPersonsToMatch().get(Gender.Female).get(Region.UKD).size(), "Persons to match should be empty.");
            assertEquals(0, mockModel.getPersonsToMatch().get(Gender.Male).get(Region.UKD).size(), "Persons to match should be empty.");
        }

        @Test
        @DisplayName("OUTCOME E: Over 29 or non-student or left education - estimate leave partner")
        public void over29LeavesPartner() {
            testPerson.setDemAge(30);
            testPerson.setDemMaleFlag(Gender.Female);
            testPartner.setDemAge(20);
            testPartner.setDemMaleFlag(Gender.Male);

            testPerson.setBenefitUnit(testBenefitUnit);
            testPartner.setBenefitUnit(testBenefitUnit);

            testBenefitUnit.setRegion(Region.UKD);

            assertEquals(true, testPerson.isPartnered(), "Person should start partnered.");
            assertEquals(true, testPartner.isPartnered(), "Partner should start partnered.");

            parametersMock.when(() -> Parameters.getRegPartnershipU2()).thenReturn(mockBinomialRegression);
            Mockito.when(mockBinomialRegression.getProbability(Mockito.anyDouble())).thenReturn(PROBABILITY_TO_PARTNER);
            Mockito.when(mockInnovations.getDoubleDraw(25)).thenReturn(INNOVATION_TO_PARTNER);

            testPerson.cohabitation();
            testPerson.partnershipDissolution();

            assertEquals(30, testPerson.getDemAge(), "Person's age should not have changed.");
            assertFalse(testPerson.isPartnered(), "Person should no longer be partnered.");
            assertFalse(testPartner.isPartnered(), "Partner should no longer be partnered.");
            assertFalse(testPerson.isToBePartnered(), "Person should not be to be partnered.");
            assertEquals(0, mockModel.getPersonsToMatch().get(Gender.Female).get(Region.UKD).size(), "Persons to match should be empty.");
            assertEquals(0, mockModel.getPersonsToMatch().get(Gender.Male).get(Region.UKD).size(), "Persons to match should be empty.");
        }
    }

    // =====================================================================
    // EQ5D tests
    // Main steps:
    //  1) create Person(true)
    //  2) load EQ5D conversion coefficients
    //  3) trigger HealthEQ5D process and assert output
    // =====================================================================

    @Nested
    @DisplayName("EQ5D process")
    class Eq5dTests {

        private Person testPerson;

        @BeforeEach
        void setup() {

            testPerson = new Person(true);
            testPerson.setDemAge(30);
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
                testPerson.setHealthMentalMcs(1.);
                testPerson.setHealthPhysicalPcs(1.);

                testPerson.onEvent(Person.Processes.HealthEQ5D);

                assertEquals(-0.594, testPerson.getDemLifeSatEQ5D());

            }

            @Test
            @DisplayName("Calculates high score correctly using Lawrence and Fleishman coefficients")
            public void calculatesHighScoreCorrectly() {
                testPerson.setHealthMentalMcs(100.);
                testPerson.setHealthPhysicalPcs(100.);

                testPerson.onEvent(Person.Processes.HealthEQ5D);

                assertEquals(1, testPerson.getDemLifeSatEQ5D());

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
                testPerson.setHealthMentalMcs(1.);
                testPerson.setHealthPhysicalPcs(1.);

                testPerson.onEvent(Person.Processes.HealthEQ5D);

                assertEquals(-0.594, testPerson.getDemLifeSatEQ5D());

            }

            @Test
            @DisplayName("Calculates high score correctly using Franks coefficients")
            public void calculatesHighScoreCorrectly() {
                testPerson.setHealthMentalMcs(100.);
                testPerson.setHealthPhysicalPcs(100.);

                testPerson.onEvent(Person.Processes.HealthEQ5D);

                // The maximum possible value given by the Franks coefficients
                assertEquals(0.9035601, testPerson.getDemLifeSatEQ5D());

    @Nested
    @DisplayName("Education transitions (E1a/E1b/E2)")
    class EducationTests {

        private static final int MIN_AGE_TO_LEAVE_EDUCATION = 16;
        private static final int MAX_AGE_TO_LEAVE_CONTINUOUS_EDUCATION = 29;

        private SimPathsModel mockModel;
        private Innovations mockInnovations;
        private BenefitUnit mockBenefitUnit;

        private BinomialRegression mockBinomialRegression;
        private GeneralisedOrderedRegression<Education> mockGeneralisedOrderedRegression;

        private Person testPerson;

        @BeforeEach
        void setUp() throws Exception {
            // Static mocks first (constructor depends on Parameters)
            parametersMock = Mockito.mockStatic(Parameters.class);
            managerRegressionsMock = Mockito.mockStatic(ManagerRegressions.class);
            mockStaticDependenciesForConstructor();

            mockModel = Mockito.mock(SimPathsModel.class);
            mockInnovations = Mockito.mock(Innovations.class);
            mockBenefitUnit = Mockito.mock(BenefitUnit.class);

            mockBinomialRegression = Mockito.mock(BinomialRegression.class);
            mockGeneralisedOrderedRegression = Mockito.mock(GeneralisedOrderedRegression.class);

            Mockito.when(mockModel.getYear()).thenReturn(2025);
            Mockito.when(mockModel.isAlignInSchool()).thenReturn(false);

            // Create person and inject dependencies
            testPerson = new Person(1L, 123L);
            setPrivateField(testPerson, "model", mockModel);
            injectInnovations(testPerson, mockInnovations);
            setPrivateField(testPerson, "benefitUnit", mockBenefitUnit);

            // Force initial state for flags used by education transitions
            setPrivateField(testPerson, "eduLeftEduFlag", Boolean.FALSE);
            setPrivateField(testPerson, "eduLeaveSchoolFlag", Boolean.FALSE);

            Mockito.when(mockBenefitUnit.getChildren()).thenReturn(Collections.emptySet());

            // default E2a stub
            parametersMock.when(Parameters::getRegEducationE2a).thenReturn(mockGeneralisedOrderedRegression);
        }

        @AfterEach
        void tearDown() {
            if (parametersMock != null) {
                parametersMock.close();
                parametersMock = null;
            }
            if (managerRegressionsMock != null) {
                managerRegressionsMock.close();
                managerRegressionsMock = null;
            }
        }

        private void setupEducationLevelRegressionMock(double draw) {
            Mockito.when(mockInnovations.getDoubleDraw(30)).thenReturn(draw);

            Map<Education, Double> mockProbs = new HashMap<>();
            mockProbs.put(Education.Low, 0.3);
            mockProbs.put(Education.Medium, 0.3);
            mockProbs.put(Education.High, 0.4);

            managerRegressionsMock.when(() ->
                            ManagerRegressions.getProbabilities(Mockito.any(Person.class), Mockito.eq(RegressionName.EducationE2a)))
                    .thenReturn(mockProbs);

            parametersMock.when(Parameters::getRegEducationE2a).thenReturn(mockGeneralisedOrderedRegression);
            Mockito.when(mockGeneralisedOrderedRegression.getProbabilities(Mockito.any(), Mockito.any()))
                    .thenReturn((Map) mockProbs);
        }

        // ----------------------- inSchool() -----------------------

        @Test
        @DisplayName("OUTCOME A: Lagged Student < Min Age (Always Stays)")
        void remainsBelowMinAge() {
            testPerson.setDemAge(MIN_AGE_TO_LEAVE_EDUCATION - 1);
            testPerson.setLes_c4_lag1(Les_c4.Student);
            assertTrue(testPerson.inSchool());
        }

        @Test
        @DisplayName("OUTCOME B: Lagged Student, Stays in E1a (Continue Spell)")
        void continuesCurrentSpellE1a() {
            final double PROBABILITY_TO_STAY = 0.9;
            final double INNOVATION_TO_STAY = 0.1;

            testPerson.setDemAge(25);
            testPerson.setLes_c4_lag1(Les_c4.Student);
            testPerson.setLes_c4(Les_c4.Student);

            parametersMock.when(Parameters::getRegEducationE1a).thenReturn(mockBinomialRegression);
            Mockito.when(mockBinomialRegression.getProbability(Mockito.anyDouble())).thenReturn(PROBABILITY_TO_STAY);
            Mockito.when(mockInnovations.getDoubleDraw(24)).thenReturn(INNOVATION_TO_STAY);

            assertTrue(testPerson.inSchool());
            assertEquals(Les_c4.Student, testPerson.getLes_c4());
            assertFalse(testPerson.isToLeaveSchool());
        }

        @Test
        @DisplayName("E2 Trigger: Lagged Student, Fails E1a (Leaves Spell)")
        void triggersE2FromE1aFailure() {
            final double PROBABILITY_TO_STAY = 0.5;
            final double INNOVATION_TO_LEAVE = 0.95;

            testPerson.setDemAge(25);
            testPerson.setLes_c4_lag1(Les_c4.Student);

            parametersMock.when(Parameters::getRegEducationE1a).thenReturn(mockBinomialRegression);
            Mockito.when(mockBinomialRegression.getProbability(Mockito.anyDouble())).thenReturn(PROBABILITY_TO_STAY);
            Mockito.when(mockInnovations.getDoubleDraw(24)).thenReturn(INNOVATION_TO_LEAVE);

            assertFalse(testPerson.inSchool());
            assertTrue(testPerson.isToLeaveSchool());
        }

        @Test
        @DisplayName("E2 Trigger: Lagged Student, at Max Age (Forced Exit)")
        void triggersE2FromMaxAge() {
            testPerson.setDemAge(MAX_AGE_TO_LEAVE_CONTINUOUS_EDUCATION + 1);
            testPerson.setLes_c4_lag1(Les_c4.Student);

            assertFalse(testPerson.inSchool());
            assertTrue(testPerson.isToLeaveSchool());
        }

        @Test
        @DisplayName("OUTCOME C: Lagged Retired (Cannot Re-enter)")
        void cannotEnterIfLaggedRetired() {
            testPerson.setLes_c4_lag1(Les_c4.Retired);

            assertFalse(testPerson.inSchool());
            assertFalse(testPerson.isToLeaveSchool());
        }

        @Test
        @DisplayName("OUTCOME E: Lagged Not Student, Succeeds in E1b (Becomes Student)")
        void becomesStudentE1bSuccess() {
            final double PROBABILITY_TO_BECOME_STUDENT = 0.8;
            final double INNOVATION_TO_BECOME_STUDENT = 0.1;

            testPerson.setLes_c4_lag1(Les_c4.NotEmployed);
            testPerson.setLes_c4(Les_c4.NotEmployed);

            parametersMock.when(Parameters::getRegEducationE1b).thenReturn(mockBinomialRegression);
            Mockito.when(mockBinomialRegression.getProbability(Mockito.anyDouble())).thenReturn(PROBABILITY_TO_BECOME_STUDENT);
            Mockito.when(mockInnovations.getDoubleDraw(24)).thenReturn(INNOVATION_TO_BECOME_STUDENT);

            assertTrue(testPerson.inSchool());
            assertEquals(Les_c4.Student, testPerson.getLes_c4());
            assertEquals(Indicator.False, testPerson.getDed());
            assertEquals(Indicator.True, testPerson.getDer());
        }

        @Test
        @DisplayName("OUTCOME D: Lagged Not Student, Fails in E1b (Remains Unchanged)")
        void remainsUnchangedE1bFailure() {
            final double PROBABILITY_TO_BECOME_STUDENT = 0.2;
            final double INNOVATION_REMAIN_UNCHANGED = 0.9;

            testPerson.setLes_c4_lag1(Les_c4.EmployedOrSelfEmployed);
            testPerson.setLes_c4(Les_c4.EmployedOrSelfEmployed);

            parametersMock.when(Parameters::getRegEducationE1b).thenReturn(mockBinomialRegression);
            Mockito.when(mockBinomialRegression.getProbability(Mockito.anyDouble())).thenReturn(PROBABILITY_TO_BECOME_STUDENT);
            Mockito.when(mockInnovations.getDoubleDraw(24)).thenReturn(INNOVATION_REMAIN_UNCHANGED);

            assertFalse(testPerson.inSchool());
            assertEquals(Les_c4.EmployedOrSelfEmployed, testPerson.getLes_c4());
            assertFalse(testPerson.isToLeaveSchool());
        }

        // ----------------------- setEducationLevel() -----------------------

        @Test
        @DisplayName("OUTCOME F (First Spell): Adopts New Level (Low -> High)")
        void firstSpellAdoptsNewLevel() {
            testPerson.setDeh_c3(Education.Low);
            testPerson.setDer(Indicator.False);

            setupEducationLevelRegressionMock(0.9); // -> High

            testPerson.setEducationLevel();

            assertEquals(Education.High, testPerson.getDeh_c3());
        }

        @Test
        @DisplayName("OUTCOME F (Return Spell Improvement): Adopts New, Higher Level")
        void returnSpellAdoptsHigherLevel() {
            testPerson.setDeh_c3(Education.Low);
            testPerson.setDer(Indicator.True);

            setupEducationLevelRegressionMock(0.5); // -> Medium

            testPerson.setEducationLevel();

            assertEquals(Education.Medium, testPerson.getDeh_c3());
        }

        @Test
        @DisplayName("OUTCOME G (Return Spell Downgrade): Retains Current Level")
        void returnSpellRetainsCurrentLevelOnDowngrade() {
            testPerson.setDeh_c3(Education.Medium);
            testPerson.setDer(Indicator.True);

            setupEducationLevelRegressionMock(0.2); // -> Low

            testPerson.setEducationLevel();

            assertEquals(Education.Medium, testPerson.getDeh_c3());
        }

        @Test
        @DisplayName("OUTCOME G (Return Spell Same Level): Retains Current Level")
        void returnSpellRetainsCurrentLevelOnSameLevel() {
            testPerson.setDeh_c3(Education.Medium);
            testPerson.setDer(Indicator.True);

            setupEducationLevelRegressionMock(0.5); // -> Medium

            testPerson.setEducationLevel();

            assertEquals(Education.Medium, testPerson.getDeh_c3());
        }

        // ----------------------- leavingSchool() -----------------------

        @Test
        @DisplayName("When toLeaveSchool=True: Executes all state transitions")
        void successfulExitExecution() {
            testPerson.setToLeaveSchool(true);
            testPerson.setDemAge(20);
            testPerson.setDeh_c3(Education.Low);
            testPerson.setDed(Indicator.True);
            testPerson.setDer(Indicator.False);
            testPerson.setLes_c4(Les_c4.Student);
            testPerson.setLes_c4_lag1(Les_c4.Student);

            setupEducationLevelRegressionMock(0.5); // -> Medium

            testPerson.leavingSchool();

            assertFalse(testPerson.isToLeaveSchool());
            assertEquals(Indicator.False, testPerson.getDed());
            assertEquals(Indicator.False, testPerson.getDer());
            assertTrue(testPerson.isLeftEducation());
            assertEquals(Les_c4.NotEmployed, testPerson.getLes_c4());
            assertEquals(Education.Medium, testPerson.getDeh_c3());
            assertEquals(Indicator.True, testPerson.getSedex());
        }

        @Test
        @DisplayName("When toLeaveSchool=False: Skips execution and leaves state intact")
        void noExecution() {
            testPerson.setToLeaveSchool(false);
            testPerson.setLes_c4(Les_c4.EmployedOrSelfEmployed);
            testPerson.setDeh_c3(Education.High);

            testPerson.leavingSchool();

            assertEquals(Les_c4.EmployedOrSelfEmployed, testPerson.getLes_c4());
            assertEquals(Education.High, testPerson.getDeh_c3());
            assertFalse(testPerson.isLeftEducation());
            Mockito.verify(mockInnovations, Mockito.never()).getDoubleDraw(30);
        }
    }
}
