package simpaths.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class BenefitUnitPensionComponentsTest {

    @Test
    void inputComponentsMustReproduceTheReportedPensionTotal() {
        BenefitUnit unit = new BenefitUnit(true);
        unit.setWealthPensValue(100_000.0);
        unit.setInitialPensionComponents(20_000, 40_000, 30_000, 10_000);

        assertArrayEquals(new double[]{100_000, 0, 0, 0},
                unit.getInitialPensionComponents(), 1e-6);
        assertArrayEquals(new double[]{100_000, 0, 0, 0},
                unit.getInitialPensionComponents(), 1e-6);
    }

    @Test
    void researchInputUsesTheUnsplitTotalAsTheModelledPot() {
        BenefitUnit unit = mock(BenefitUnit.class, CALLS_REAL_METHODS);
        doReturn(1L).when(unit).getId();
        unit.setWealthPensValue(100_000.0);
        assertArrayEquals(new double[]{100_000, 0, 0, 0},
                unit.getInitialPensionComponents(), 1e-6);
        assertTrue(unit.isLegacyUnsplitPensionInput());
        assertArrayEquals(new double[]{100_000, 0, 0, 0},
                unit.getInitialPensionComponents(), 1e-6);
    }
}
