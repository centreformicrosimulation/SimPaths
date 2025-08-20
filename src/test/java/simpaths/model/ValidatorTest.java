package simpaths.model;

import microsim.data.MultiKeyCoefficientMap;
import microsim.data.excel.ExcelAssistant;
import microsim.engine.SimulationEngine;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import simpaths.data.Parameters;
import simpaths.experiment.SimPathsCollector;
import simpaths.experiment.SimPathsObserver;
import simpaths.model.enums.Country;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ValidatorTest {

    private static Validator validator;
    private static SimPathsModel mockModel;
    private static SimPathsCollector mockCollector;
    private static SimPathsObserver mockObserver;
    private static SimulationEngine engineMock;

    @BeforeAll
    static void setUp() {
        mockModel = mock(SimPathsModel.class);
        mockCollector = mock(SimPathsCollector.class);
        mockObserver = mock(SimPathsObserver.class);
        SimulationEngine engineMock = mock(SimulationEngine.class);
        MockedStatic<Parameters> mockParameters = mockStatic(Parameters.class, Mockito.CALLS_REAL_METHODS);

        when(mockModel.getYear()).thenReturn(2017);

        when(engineMock.getManager(SimPathsModel.class.getCanonicalName())).thenReturn(mockModel);
        when(engineMock.getManager(SimPathsCollector.class.getCanonicalName())).thenReturn(mockCollector);
        when(engineMock.getManager(SimPathsObserver.class.getCanonicalName())).thenReturn(mockObserver);

        String countryString = "UK";
        int columnsValidationLifeSatisfactionByAgeGroup = 18;

        MultiKeyCoefficientMap validationLifeSatisfactionByAge = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", countryString + "_lifeSatisfactionByAgeGroup", 1, columnsValidationLifeSatisfactionByAgeGroup);
        MultiKeyCoefficientMap validationUniversalCredit = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", countryString + "_benefitsUC", 1, 1);
        MultiKeyCoefficientMap validationLegacyBenefits = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", countryString + "_benefitsNonUC", 1, 1);

        mockParameters.when(() -> Parameters.getValidationLifeSatisfactionByAge())
                .thenReturn(validationLifeSatisfactionByAge);
        mockParameters.when(() -> Parameters.getValidationUniversalCredit())
                .thenReturn(validationUniversalCredit);
        mockParameters.when(() -> Parameters.getValidationLegacyBenefits())
                .thenReturn(validationLegacyBenefits);

        validator = new Validator();
    }

    @Test
    @DisplayName("Validation of Universal Credit values loaded")
    void validationUniversalCreditTest() {

    Number value = ((Number) Parameters.getValidationUniversalCredit().getValue(mockModel.getYear()-1));

    assert(value.doubleValue() > 0);

    }

    @Test
    @DisplayName("Validation of Legacy Benefits values loaded")
    void validationLegacyBenefitsTest() {
        Number value = ((Number) Parameters.getValidationLegacyBenefits().getValue(mockModel.getYear()-1));
        assert(value.doubleValue() > 0);
    }

    @Test
    @DisplayName("Validation of Life Satisfaction values loaded")
    void validationLifeSatisfactionTest() {
        Number value = ((Number) Parameters.getValidationLifeSatisfactionByAge().getValue(mockModel.getYear()-1, "life_satisfaction_female_20_29"));
        assert(value.doubleValue() > 0);
    }
}