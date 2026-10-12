package simpaths.model.person;

import org.junit.jupiter.api.Test;
import simpaths.data.Parameters;
import simpaths.model.enums.Gender;
import simpaths.model.enums.Les_c4;
import simpaths.model.utilities.AnnuityRates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PrivatePensionComponentsTest {

    @Test
    void workingPersonGrowsOneModelledPot() {
        PrivatePension prior = new PrivatePension();
        prior.setInitialComponents(10_000, 20_000, 5_000, 3_000);
        prior.setPensionIncomeAnnual(1_000);
        PrivatePension current = new PrivatePension();
        current.setContRatePP(0.10);

        current.projectWealth(prior, 100_000, 0.05, Les_c4.EmployedOrSelfEmployed);

        assertEquals(49_900, current.getDcWealth(), 1e-6);
        assertEquals(0, current.getDbRightsValue(), 1e-6);
        assertEquals(0, current.getInPaymentRightsValue(), 1e-6);
        assertEquals(0, current.getOtherRightsValue(), 1e-6);
    }

    @Test
    void retirementConvertsThePotToIncomeAndClearsItsStock() {
        PrivatePension working = new PrivatePension();
        working.setInitialComponents(60_000, 0, 30_000, 10_000);
        AnnuityRates savedRates = Parameters.annuityRates;
        try {
            AnnuityRates rates = mock(AnnuityRates.class);
            when(rates.getAnnuityRateByGenderAgeYear(Gender.Male, 65, 2030)).thenReturn(10.0);
            Parameters.annuityRates = rates;

            PrivatePension retired = new PrivatePension();
            retired.projectPensionAccess(65, Gender.Male, 2030, working);

            assertEquals(25_000, retired.getLumpSumPayment(), 1e-6);
            assertEquals(7_500, retired.getPensionIncomeAnnual(), 1e-6);
            assertEquals(0, retired.getWealth(), 1e-6);
            assertEquals(0, retired.getRemainingDcFund(), 1e-6);
            assertTrue(retired.isRetired());
        } finally {
            Parameters.annuityRates = savedRates;
        }
    }
}
