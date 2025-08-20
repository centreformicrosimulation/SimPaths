package simpaths.model;

import microsim.engine.SimulationEngine;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import simpaths.data.Parameters;
import simpaths.experiment.SimPathsCollector;
import simpaths.experiment.SimPathsObserver;
import simpaths.model.enums.Country;

import static org.mockito.Mockito.*;

class ValidatorTest {

    private static Validator validator;
    private static SimPathsModel mockModel;
    private static SimPathsCollector mockCollector;
    private static SimPathsObserver mockObserver;
    private static SimulationEngine engineMock;
    private static MockedStatic<Parameters> mockParameters;
    private static MockedStatic<SimulationEngine> mockEngine;

    private static String tempInputDirectory;

    @BeforeAll
    static void setUp() {
        mockModel = mock(SimPathsModel.class);
        mockCollector = mock(SimPathsCollector.class);
        mockObserver = mock(SimPathsObserver.class);
        SimulationEngine engineMock = mock(SimulationEngine.class);
        mockParameters = mockStatic(Parameters.class, Mockito.CALLS_REAL_METHODS);
        mockEngine = mockStatic(SimulationEngine.class, Mockito.CALLS_REAL_METHODS);

        mockEngine.when(() -> SimulationEngine.getInstance()).thenReturn(engineMock);

        when(mockModel.getYear()).thenReturn(2017);

        when(engineMock.getManager(SimPathsModel.class.getCanonicalName())).thenReturn(mockModel);
        when(engineMock.getManager(SimPathsCollector.class.getCanonicalName())).thenReturn(mockCollector);
        when(engineMock.getManager(SimPathsObserver.class.getCanonicalName())).thenReturn(mockObserver);

        tempInputDirectory = Parameters.getInputDirectory();

        Parameters.setInputDirectory("input/");

        Parameters.updateCountryColumnNumbers(Country.UK);
        Parameters.loadValidationStatistics(Country.UK.toString());

        validator = new Validator();
    }

    @AfterAll
    static void tearDown() {
        if (mockEngine != null) mockEngine.close();
        if (mockParameters != null) mockParameters.close();
        Parameters.setInputDirectory(tempInputDirectory);
    }

    @Test
    @DisplayName("Validation of Universal Credit values loaded")
    void validationUniversalCreditTest() {

    Number value = validator.getDoubleValue(Validator.DoublesVariables.ucReceipt);

    assert(value.doubleValue() > 0);

    }

    @Test
    @DisplayName("Validation of Legacy Benefits values loaded")
    void validationLegacyBenefitsTest() {
        Number value = validator.getDoubleValue(Validator.DoublesVariables.lbReceipt);
        assert(value.doubleValue() > 0);
    }

    @Test
    @DisplayName("Validation of Life Satisfaction values loaded")
    void validationLifeSatisfactionTest() {
        Number value = validator.getDoubleValue(Validator.DoublesVariables.lifeSatisfactionFemale_20_29);
        assert(value.doubleValue() > 0);
    }

    @Test
    @DisplayName("Validation of LHW values loaded")
    void validationLhwTest() {
        Number value_male = validator.getDoubleValue(Validator.DoublesVariables.lhw_Male);
        Number value_female = validator.getDoubleValue(Validator.DoublesVariables.lhw_Female);
        assert(value_male.doubleValue() > 0);
        assert(value_female.doubleValue() > 0);
    }
}