package simpaths.model.person;

import org.junit.jupiter.api.Test;
import simpaths.data.Parameters;
import simpaths.model.enums.Gender;
import simpaths.model.enums.Les_c4;
import simpaths.model.utilities.AnnuityRates;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PrivatePensionTest {

    @Test
    void occupationalAndPersonalContributionsGrowOneModelledPot() {
        PrivatePension prior = new PrivatePension();
        prior.setWealth(38_000);
        PrivatePension current = new PrivatePension();
        current.setContRatePP(0.10);
        current.setContRateOPEe(0.04);
        current.setContRateOPEr(0.06);

        current.projectWealth(prior, 100_000, 0.05, Les_c4.EmployedOrSelfEmployed);

        assertEquals(59_900, current.getWealth(), 1e-6);
    }

    @Test
    void nonWorkerReceivesReturnsWithoutNewPensionContributions() {
        PrivatePension prior = new PrivatePension();
        prior.setWealth(38_000);
        PrivatePension current = new PrivatePension();
        current.setContRatePP(0.10);
        current.setContRateOPEe(0.04);
        current.setContRateOPEr(0.06);

        current.projectWealth(prior, 100_000, 0.05, Les_c4.NotEmployed);

        assertEquals(39_900, current.getWealth(), 1e-6);
        assertEquals(0, current.getContRatePP(), 1e-6);
        assertEquals(0, current.getContRateOPEe(), 1e-6);
        assertEquals(0, current.getContRateOPEr(), 1e-6);
    }

    @Test
    void annualCopyPreservesPensionBalanceAndPensionerStatus() {
        PrivatePension original = new PrivatePension();
        original.setWealth(38_000);
        original.setPensionIncomeAnnual(1_000);
        original.setPensioner(true);

        PrivatePension copy = new PrivatePension(original);

        assertEquals(38_000, copy.getWealth(), 1e-6);
        assertEquals(1_000, copy.getPensionIncomeAnnual(), 1e-6);
        assertTrue(copy.isPensioner());
        copy.setWealth(0);
        assertEquals(38_000, original.getWealth(), 1e-6);
    }

    @Test
    void pensionAccessConvertsThePotToIncomeAndClearsItsStockAndContributions() {
        PrivatePension working = new PrivatePension();
        working.setWealth(100_000);
        AnnuityRates savedRates = Parameters.annuityRates;
        double savedShare = Parameters.pensionLumpSumShare;
        double savedCap = Parameters.pensionLumpSumCap;
        try {
            AnnuityRates rates = mock(AnnuityRates.class);
            when(rates.getAnnuityRateByGenderAgeYear(Gender.Male, 65, 2030)).thenReturn(10.0);
            Parameters.annuityRates = rates;
            Parameters.pensionLumpSumShare = 0.25;
            Parameters.pensionLumpSumCap = 250_000;
            PrivatePension pension = new PrivatePension();
            pension.setContRatePP(0.10);
            pension.setContRateOPEe(0.04);
            pension.setContRateOPEr(0.06);

            pension.projectPensionAccess(65, Gender.Male, 2030, working);

            assertEquals(25_000, pension.getLumpSumPayment(), 1e-6);
            assertEquals(7_500, pension.getPensionIncomeAnnual(), 1e-6);
            assertEquals(0, pension.getWealth(), 1e-6);
            assertTrue(pension.isPensioner());
            assertFalse(pension.isMemberPP());
            assertFalse(pension.isMemberOP());
            assertEquals(0, pension.getContRatePP(), 1e-6);
            assertEquals(0, pension.getContRateOPEe(), 1e-6);
            assertEquals(0, pension.getContRateOPEr(), 1e-6);
        } finally {
            Parameters.annuityRates = savedRates;
            Parameters.pensionLumpSumShare = savedShare;
            Parameters.pensionLumpSumCap = savedCap;
        }
    }
}
