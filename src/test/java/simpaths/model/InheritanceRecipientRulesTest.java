package simpaths.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import simpaths.model.benefitunit.WealthNonPension;
import simpaths.model.enums.Education;
import simpaths.model.enums.Region;
import simpaths.model.enums.SampleExit;
import simpaths.model.person.PrivatePension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InheritanceRecipientRulesTest {

    @Test
    void survivingParentsInAnotherUnitShareEstateAndEligiblePension() {
        World world = new World();
        Person donor = world.person(1, 25, 60_000);
        Person mother = world.person(2, 50, 0);
        Person father = world.person(3, 52, 0);
        world.join(mother, father);
        world.person(4, 25, 0); // A close matched adult must not displace parents.
        when(donor.getIdMother()).thenReturn(2L);
        when(donor.getIdFather()).thenReturn(3L);
        PrivatePension pension = mock(PrivatePension.class);
        when(pension.getWealth()).thenReturn(30_000.0);
        when(donor.getPrivatePension()).thenReturn(pension);
        world.die(donor);
        world.settle();

        assertEquals(90_000, world.balance(mother), 1e-6);
        verify(mother).addInheritanceToPersonalFinancialBalance(15_000.0);
        verify(mother).addInheritanceToPersonalFinancialBalance(30_000.0);
        verify(father).addInheritanceToPersonalFinancialBalance(15_000.0);
        verify(father).addInheritanceToPersonalFinancialBalance(30_000.0);
        assertTrue(world.module.getSyntheticMatches().isEmpty());
        assertTrue(world.module.getTransfers().stream().allMatch(t -> t.reason().endsWith("_parent")));
        world.settle();
        assertEquals(90_000, world.balance(mother), 1e-6);
    }

    @Test
    void soleSurvivingParentReceivesWholeEstateAndDeadParentIsExcluded() {
        World world = new World();
        Person donor = world.person(1, 25, 60_000);
        Person mother = world.person(2, 50, 0);
        Person father = world.person(3, 52, 0);
        when(donor.getIdMother()).thenReturn(2L);
        when(donor.getIdFather()).thenReturn(3L);
        world.die(donor);
        when(father.getSampleExit()).thenReturn(SampleExit.DeathAlignment);
        world.settle();
        assertEquals(60_000, world.balance(mother), 1e-6);
        assertEquals(0, world.balance(father));
    }

    @Test
    void parentWhoHasLeftPopulationCannotInheritEvenIfStillIndexed() {
        World world = new World();
        Person donor = world.person(1, 25, 60_000);
        Person mother = world.person(2, 50, 0);
        Person substitute = world.person(3, 25, 0);
        when(donor.getIdMother()).thenReturn(2L);
        world.module.settleYear(2029, world.people, world.units);
        world.people.remove(mother);
        world.units.remove(mother.getBenefitUnit());
        world.die(donor);
        world.settle();
        assertEquals(60_000, world.balance(substitute), 1e-6);
        assertEquals(0, world.balance(mother));
    }

    @Test
    void identifiedChildTakesPriorityOverSurvivingParent() {
        World world = new World();
        Person donor = world.person(1, 45, 60_000);
        Person child = world.person(2, 20, 0);
        Person mother = world.person(3, 70, 0);
        when(child.getIdFather()).thenReturn(1L);
        when(donor.getIdMother()).thenReturn(3L);
        world.die(donor);
        world.settle();
        assertEquals(60_000, world.balance(child), 1e-6);
        assertEquals(0, world.balance(mother));
    }

    @Test
    void unresolvedChildBranchRetainsItsShareRatherThanFallingBackToParent() {
        World world = new World();
        Person donor = world.person(1, 75, 60_000);
        Person mother = world.person(2, 95, 0);
        Person substitute = world.person(3, 50, 0);
        when(donor.getIdMother()).thenReturn(2L);
        when(donor.getInheritanceSourceId()).thenReturn(100L);
        Person sourceChild = mock(Person.class);
        when(sourceChild.getId()).thenReturn(101L);
        when(sourceChild.getIdFather()).thenReturn(100L);
        BenefitUnit sourceUnit = mock(BenefitUnit.class);
        when(sourceUnit.getMembers()).thenReturn(Set.of(sourceChild));
        Household sourceHome = mock(Household.class);
        when(sourceHome.getBenefitUnits()).thenReturn(Set.of(sourceUnit));
        world.module.indexSourceFamilies(List.of(sourceHome));
        world.die(donor);
        world.settle();
        assertEquals(60_000, world.balance(substitute), 1e-6);
        assertEquals(0, world.balance(mother));
        assertTrue(world.module.getTransfers().getFirst().reason().contains("unresolved_child"));
    }

    @Test
    void survivingPartnerKeepsPooledWealthAheadOfParents() {
        World world = new World();
        Person donor = world.person(1, 25, 60_000);
        Person partner = world.person(2, 25, 0);
        Person mother = world.person(3, 50, 0);
        world.join(donor, partner);
        when(donor.getPartner()).thenReturn(partner);
        when(partner.getPartner()).thenReturn(donor);
        when(donor.getIdMother()).thenReturn(3L);
        world.die(donor);
        world.settle();
        assertEquals(60_000, world.balance(partner), 1e-6);
        assertEquals(0, world.balance(mother));
        assertTrue(world.module.getTransfers().stream().allMatch(t -> t.timing().equals("retained_in_unit")));
    }

    @Test
    void simultaneousAdultDeathsIncludeBothSetsOfParentsAndSettlePoolOnce() {
        World world = new World();
        Person first = world.person(1, 30, 80_000);
        Person second = world.person(2, 32, 0);
        world.join(first, second);
        when(first.getIdMother()).thenReturn(10L);
        when(first.getIdFather()).thenReturn(11L);
        when(second.getIdMother()).thenReturn(20L);
        when(second.getIdFather()).thenReturn(21L);
        List<Person> parents = List.of(world.person(10, 60, 0), world.person(11, 61, 0),
                world.person(20, 62, 0), world.person(21, 63, 0));
        world.die(first);
        world.die(second);
        world.settle();
        for (Person parent : parents)
            assertEquals(20_000, world.balance(parent), 1e-6);
        assertEquals(4, world.module.getTransfers().size());
        assertEquals(0, world.balance(first));
    }

    @Test
    void ageOutranksEducationAndRegion() {
        World world = matchingWorld();
        Person close = world.person(2, 50, 0, Education.Low, Region.UKD);
        Person educated = world.person(3, 90, 0, Education.High, Region.UKC);
        world.settle();
        assertEquals(10_000, world.balance(close), 1e-6);
        assertEquals(0, world.balance(educated));
    }

    @Test
    void educationOutranksRegionWhenAgeIsTied() {
        World world = matchingWorld();
        Person educated = world.person(2, 55, 0, Education.High, Region.UKD);
        Person local = world.person(3, 55, 0, Education.Low, Region.UKC);
        world.settle();
        assertEquals(10_000, world.balance(educated), 1e-6);
        assertEquals(0, world.balance(local));
        assertEquals(0, world.module.getSyntheticMatches().getFirst().educationDistance());
        assertFalse(world.module.getSyntheticMatches().getFirst().sameRegion());
    }

    @Test
    void closestEducationCategoryIsUsedBeforeRegion() {
        World world = matchingWorld();
        Person medium = world.person(2, 55, 0, Education.Medium, Region.UKD);
        Person low = world.person(3, 55, 0, Education.Low, Region.UKC);
        world.settle();
        assertEquals(10_000, world.balance(medium), 1e-6);
        assertEquals(0, world.balance(low));
        assertEquals(1, world.module.getSyntheticMatches().getFirst().educationDistance());
    }

    @Test
    void observedEducationOutranksMissingEducationAtTheSameAge() {
        World world = matchingWorld();
        Person known = world.person(2, 55, 0, Education.Medium, Region.UKD);
        Person missing = world.person(3, 55, 0, null, Region.UKC);
        world.settle();
        assertEquals(10_000, world.balance(known), 1e-6);
        assertEquals(0, world.balance(missing));
    }

    @Test
    void regionBreaksAnAgeAndEducationTie() {
        World world = matchingWorld();
        Person distant = world.person(2, 55, 0, Education.High, Region.UKD);
        Person local = world.person(3, 55, 0, Education.High, Region.UKC);
        world.settle();
        assertEquals(10_000, world.balance(local), 1e-6);
        assertEquals(0, world.balance(distant));
    }

    @ParameterizedTest
    @ValueSource(ints = {18, 19, 25, 40, 47, 48, 49, 75})
    void targetAgesRespectYoungDonorBoundaryAndMatchedRecipientsAreAdults(int donorAge) {
        World world = new World();
        Person donor = world.person(1, donorAge, 10_000);
        world.die(donor);
        for (int age = 18; age <= 100; age++)
            for (int copy = 0; copy < 2; copy++)
                world.person(age * 10L + copy, age, 0);
        world.settle();
        assertEquals(2, world.module.getSyntheticMatches().size());
        for (InheritanceModule.SyntheticMatch match : world.module.getSyntheticMatches()) {
            int lower = donorAge < 48 ? 18 : donorAge - 30;
            int upper = donorAge < 48 ? 28 : donorAge - 20;
            assertTrue(match.targetAge() >= lower && match.targetAge() <= upper);
            assertEquals(donorAge < 48 ? "young_adult_band" : "donor_minus_20_to_30", match.ageRule());
            assertEquals(donorAge - match.targetAge(), match.ageGap());
            assertEquals(match.ageGap(), match.actualAgeGap());
            assertTrue(donorAge - match.actualAgeGap() >= 18);
            assertEquals(2, match.actualCount());
        }
        assertEquals(10_000, world.module.getTransfers().stream().mapToDouble(InheritanceModule.Transfer::value).sum(), 1e-6);
    }

    @Test
    void youngerDonorPrefersYoungAdultBandOverSameAgeEducationAndRegionMatch() {
        World world = new World();
        Person donor = world.person(1, 47, 10_000, Education.High, Region.UKC);
        Person peer = world.person(2, 47, 0, Education.High, Region.UKC);
        for (int age = 18; age <= 28; age++)
            world.person(100 + age, age, 0, Education.Low, Region.UKD);
        world.die(donor);
        world.settle();

        assertEquals(0, world.balance(peer));
        assertEquals(2, world.module.getSyntheticMatches().size());
        assertTrue(world.module.getSyntheticMatches().stream().allMatch(match ->
                match.ageRule().equals("young_adult_band") &&
                match.actualAgeGap() >= 19 && match.actualAgeGap() <= 29));
        assertEquals(10_000, world.module.getTransfers().stream()
                .mapToDouble(InheritanceModule.Transfer::value).sum(), 1e-6);
    }

    @Test
    void closerMinorCannotDisplaceAnAdultMatchedRecipient() {
        World world = new World();
        Person donor = world.person(1, 25, 10_000);
        Person carer = world.person(2, 45, 0);
        Person child = world.person(3, 17, 0);
        world.join(carer, child);
        world.module.setSyntheticRecipientCount(1);
        world.die(donor);
        world.settle();
        assertEquals(10_000, world.balance(carer), 1e-6);
        assertEquals(2, world.module.getTransfers().getFirst().recipientId());
        assertEquals(0, world.module.getIlliquidAssetWealth(carer.getBenefitUnit()));
    }

    @Test
    void tooFewDistinctAdultsReusesRecipientWithoutLosingMoney() {
        World world = new World();
        Person donor = world.person(1, 75, 10_000);
        Person recipient = world.person(2, 50, 0);
        world.die(donor);
        world.settle();
        assertEquals(10_000, world.balance(recipient), 1e-6);
        assertEquals(2, world.module.getSyntheticMatches().size());
        assertTrue(world.module.getSyntheticMatches().stream().allMatch(m -> m.actualCount() == 1));
        assertTrue(world.module.getSyntheticMatches().getLast().tier().endsWith("_reused"));
    }

    @Test
    void noEligibleAdultFailsClearlyWithoutClearingEstate() {
        World world = new World();
        Person donor = world.person(1, 75, 10_000);
        world.die(donor);
        IllegalStateException failure = assertThrows(IllegalStateException.class, world::settle);
        assertTrue(failure.getMessage().contains("inheritance cannot be discarded"));
        assertEquals(10_000, world.balance(donor), 1e-6);
        assertTrue(world.module.getTransfers().isEmpty());
    }

    @Test
    void tiedSelectionIsReproducibleWithReversedPopulationOrder() {
        World first = matchingWorld();
        World second = matchingWorld();
        for (World world : List.of(first, second)) {
            world.person(2, 55, 0);
            world.person(3, 55, 0);
        }
        List<Person> reversed = new ArrayList<>(second.people);
        Collections.reverse(reversed);
        second.people = new LinkedHashSet<>(reversed);
        first.settle();
        second.settle();
        assertEquals(first.module.getSyntheticMatches(), second.module.getSyntheticMatches());
    }

    private static World matchingWorld() {
        World world = new World();
        world.module.setSyntheticRecipientCount(1);
        world.die(world.person(1, 75, 10_000, Education.High, Region.UKC));
        return world;
    }

    private static class World {
        final InheritanceModule module = new InheritanceModule();
        Set<Person> people = new LinkedHashSet<>();
        final Set<BenefitUnit> units = new LinkedHashSet<>();
        final Map<BenefitUnit, WealthNonPension> stocks = new HashMap<>();

        Person person(long id, int age, double cash) {
            return person(id, age, cash, Education.Medium, Region.UKC);
        }

        Person person(long id, int age, double cash, Education education, Region region) {
            Person person = mock(Person.class);
            when(person.getId()).thenReturn(id);
            when(person.getDemAge()).thenReturn(age);
            when(person.getSampleExit()).thenReturn(SampleExit.NotYet);
            when(person.getEduHighestC4()).thenReturn(education);
            when(person.getRegion()).thenReturn(region);
            when(person.getPrivatePension()).thenReturn(new PrivatePension());
            BenefitUnit unit = mock(BenefitUnit.class);
            WealthNonPension wealth = new WealthNonPension();
            wealth.setWealthFinancialValue(cash);
            when(unit.getId()).thenReturn(id);
            when(unit.getMale()).thenReturn(person);
            when(unit.getMembers()).thenReturn(new LinkedHashSet<>(Set.of(person)));
            when(person.getBenefitUnit()).thenReturn(unit);
            when(unit.getWealthNonPension()).thenAnswer(ignored -> new WealthNonPension(wealth));
            doAnswer(invocation -> {
                wealth.setWealthFinancialValue(wealth.getWealthFinancial().getValue() + invocation.getArgument(0, Double.class));
                return null;
            }).when(unit).addInheritanceCash(anyDouble());
            doAnswer(ignored -> { wealth.setWealthFinancialValue(0); return null; }).when(unit).clearEstateWealth();
            people.add(person);
            units.add(unit);
            stocks.put(unit, wealth);
            return person;
        }

        void join(Person first, Person second) {
            BenefitUnit old = second.getBenefitUnit();
            BenefitUnit home = first.getBenefitUnit();
            home.getMembers().add(second);
            when(home.getFemale()).thenReturn(second);
            when(second.getBenefitUnit()).thenReturn(home);
            stocks.get(home).setWealthFinancialValue(balance(first) + stocks.get(old).getWealthFinancial().getValue());
            units.remove(old);
        }

        double balance(Person person) {
            return stocks.get(person.getBenefitUnit()).getWealthFinancial().getValue();
        }

        void die(Person person) { when(person.getSampleExit()).thenReturn(SampleExit.Death); }
        void settle() { module.settleYear(2030, people, units); }
    }
}
