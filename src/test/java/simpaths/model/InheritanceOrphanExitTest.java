package simpaths.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import simpaths.data.Parameters;
import simpaths.model.benefitunit.WealthNonPension;
import simpaths.model.enums.Gender;
import simpaths.model.enums.Region;
import simpaths.model.enums.SampleExit;
import simpaths.model.enums.Occupancy;
import simpaths.model.person.PrivatePension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InheritanceOrphanExitTest {

    @Test
    void orphanMarkerPreservesExistingExitsAndRunsBeforeSettlement(@TempDir Path output) throws Exception {
        Family family = new Family();
        Person alreadyDead = person(7, 9);
        alreadyDead.setSampleExit(SampleExit.Death);
        Person emigrant = person(8, 8);
        emigrant.setSampleExit(SampleExit.EmigrationAlignment);
        family.home.getMembers().addAll(Set.of(alreadyDead, emigrant));
        family.mother.setSampleExit(SampleExit.Death);
        SimPathsModel.markOrphanExits(family.units);

        assertEquals(SampleExit.Orphan, family.child.getSampleExit());
        assertEquals(SampleExit.Death, alreadyDead.getSampleExit());
        assertEquals(SampleExit.EmigrationAlignment, emigrant.getSampleExit());
        // Repeating the marker must not attempt a second exit.
        SimPathsModel.markOrphanExits(family.units);
        family.module.settleYear(2030, family.people, family.units);
        assertEquals(1, family.module.getOrphanExits().size());
        assertEquals(3, family.module.getOrphanExits().getFirst().personId());
        assertTrue(family.module.getDeathEvents().stream().noneMatch(d -> d.deceasedId() == 3));
        family.module.writeLedger(output);
        assertEquals(List.of("year,person_id,benefit_unit_id,age,exit_reason,reference_parent_id,reference_reason",
                        "2030,3,2,10,Orphan,2,last_deceased_parent"),
                Files.readAllLines(output.resolve("inheritance_orphan_exits.csv")));
    }

    @Test
    void livingAdultIncludingAgeEighteenPreventsOrphanExit() {
        Family family = new Family();
        SimPathsModel.markOrphanExits(family.units);
        assertEquals(SampleExit.NotYet, family.child.getSampleExit());
        family.mother.setSampleExit(SampleExit.Death);
        Person adultSibling = person(9, 18);
        family.home.getMembers().add(adultSibling);
        SimPathsModel.markOrphanExits(family.units);
        assertEquals(SampleExit.NotYet, family.child.getSampleExit());
        assertEquals(SampleExit.NotYet, adultSibling.getSampleExit());
    }

    @ParameterizedTest
    @EnumSource(value = SampleExit.class, names = {"Death", "DeathAlignment"})
    void priorTrustAndNewEstateRemainInSampleExactlyOnce(SampleExit cause, @TempDir Path output) throws Exception {
        Family family = new Family();
        family.establishTrust();
        family.homeWealth.setWealthFinancialValue(30_000);
        family.mother.setSampleExit(cause);
        family.settle(2030);

        assertEquals(SampleExit.Orphan, family.child.getSampleExit());
        assertEquals(0, family.module.getIlliquidAssetWealth(family.home));
        assertEquals(40_000, family.safeWealth.getWealthFinancial().getValue(), 1e-6);
        assertEquals(0, family.module.getIlliquidAssetWealth(family.safeHome));
        assertEquals(0, family.homeWealth.getWealthFinancial().getValue(), 1e-6);
        assertEquals(2, family.module.getOrphanReassignments().size());
        assertEquals(40_000, family.module.getOrphanReassignments().stream()
                .mapToDouble(InheritanceModule.OrphanReassignment::value).sum(), 1e-6);
        assertTrue(family.module.getOrphanReassignments().stream().allMatch(r ->
                r.orphanId() == 3 && r.recipientId() == 4 && r.referenceParentId() == 2));
        assertTrue(family.module.getTransfers().stream().noneMatch(t ->
                t.timing().equals("out_of_sample") || t.timing().equals("unmatched")));
        int transfers = family.module.getTransfers().size();
        family.settle(2030);
        family.people.remove(family.child);
        family.people.remove(family.mother);
        family.units.remove(family.home);
        family.settle(2031);
        assertEquals(transfers, family.module.getTransfers().size());
        assertEquals(1, family.module.getOrphanExits().size());
        assertEquals(2, family.module.getOrphanReassignments().size());

        // No old balance can later be released to either child.
        when(family.replacement.getDemAge()).thenReturn(18);
        family.settle(2038);
        family.settle(2038);
        assertEquals(40_000, family.safeWealth.getWealthFinancial().getValue(), 1e-6);
        assertEquals(0, family.module.getIlliquidAssetWealth(family.safeHome));
        family.module.writeLedger(output);
        List<String> ledger = Files.readAllLines(output.resolve("inheritance_orphan_reassignments.csv"));
        assertEquals(3, ledger.size());
        assertTrue(ledger.stream().anyMatch(r -> r.startsWith("2030,3,,4,existing_minor_trust,") && r.endsWith(",10000.0,2,last_deceased_parent")));
        assertTrue(ledger.stream().anyMatch(r -> r.startsWith("2030,3,2,4,nonpension_estate_orphan_child,")));
    }

    @Test
    void newPensionShareIsRedirectedBeforeAnOrphanCanReceiveIt() {
        Family family = new Family();
        PrivatePension pension = mock(PrivatePension.class);
        when(pension.getWealth()).thenReturn(12_000.0);
        when(family.mother.getPrivatePension()).thenReturn(pension);
        family.mother.setSampleExit(SampleExit.Death);
        family.settle(2030);
        assertEquals(12_000, family.safeWealth.getWealthFinancial().getValue(), 1e-6);
        assertEquals(0, family.module.getIlliquidAssetWealth(family.home));
        assertEquals("unaccessed_pension_orphan_child",
                family.module.getOrphanReassignments().getFirst().purpose());
    }

    @Test
    void existingTrustUsesParentReferenceAndExcludesChildrenAndExitedAdults() {
        Family family = new Family();
        family.establishTrust();
        when(family.replacement.getDemAge()).thenReturn(12);
        Person deadExactMatch = person(6, 35);
        deadExactMatch.setSampleExit(SampleExit.Death);
        family.safeHome.getMembers().add(deadExactMatch);
        when(deadExactMatch.getBenefitUnit()).thenReturn(family.safeHome);
        family.people.add(deadExactMatch);
        family.mother.setSampleExit(SampleExit.Death);
        family.settle(2030);
        InheritanceModule.OrphanReassignment transfer = family.module.getOrphanReassignments().getFirst();
        assertEquals(4, transfer.recipientId());
        assertEquals(2, transfer.referenceParentId());
        assertTrue(transfer.tier().startsWith("age_relaxed"));
    }

    @Test
    void childrenAndOtherOrphansCannotReceiveMatchedInheritance() {
        Family family = new Family();
        family.establishTrust();
        when(family.replacement.getRegion()).thenReturn(Region.UKD);
        when(family.replacement.getDemMaleFlag()).thenReturn(Gender.Female);
        Person anotherOrphan = person(6, 10);
        family.home.getMembers().add(anotherOrphan);
        when(anotherOrphan.getBenefitUnit()).thenReturn(family.home);
        family.people.add(anotherOrphan);
        family.mother.setSampleExit(SampleExit.Death);
        family.settle(2030);
        assertEquals(2, family.module.getOrphanExits().size());
        assertEquals(4, family.module.getOrphanReassignments().getFirst().recipientId());
        assertEquals(10_000, family.safeWealth.getWealthFinancial().getValue(), 1e-6);
        assertEquals(0, family.module.getIlliquidAssetWealth(family.safeHome));
    }

    @Test
    void removingOtherChildrenDoesNotChangeAdultRecipient() {
        Family family = new Family();
        family.establishTrust();
        family.people.remove(family.replacement);
        family.safeHome.getMembers().remove(family.replacement);
        family.mother.setSampleExit(SampleExit.Death);
        family.settle(2030);
        assertEquals(10_000, family.safeWealth.getWealthFinancial().getValue(), 1e-6);
        assertEquals(4, family.module.getOrphanReassignments().getFirst().recipientId());
    }

    @Test
    void orphanShareCanReuseOnlyEligibleRecipientWithoutReducingLivingSiblingShare() {
        Family family = new Family();
        when(family.replacement.getIdMother()).thenReturn(2L);
        family.people.remove(family.carer);
        family.safeHome.getMembers().remove(family.carer);
        when(family.replacement.getDemAge()).thenReturn(25);
        when(family.safeHome.getMale()).thenReturn(family.replacement);
        family.homeWealth.setWealthFinancialValue(30_000);
        family.mother.setSampleExit(SampleExit.Death);
        family.settle(2030);
        assertEquals(30_000, family.safeWealth.getWealthFinancial().getValue(), 1e-6);
        assertEquals(15_000, family.module.getOrphanReassignments().getFirst().value(), 1e-6);
        assertTrue(family.module.getOrphanReassignments().getFirst().tier().endsWith("_reused"));
        assertEquals(15_000, family.module.getTransfers().stream()
                .filter(t -> t.reason().equals("nonpension_estate_child"))
                .mapToDouble(InheritanceModule.Transfer::value).sum(), 1e-6);
    }

    @Test
    void noRecipientFailsClearlyWithoutDroppingAnExistingTrust() {
        Family family = new Family();
        family.establishTrust();
        family.people.removeAll(Set.of(family.carer, family.replacement));
        family.units.remove(family.safeHome);
        family.mother.setSampleExit(SampleExit.Death);
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> family.settle(2030));
        assertTrue(failure.getMessage().contains("orphan 3 in year 2030"));
        assertTrue(failure.getMessage().contains("cannot be discarded"));
        assertTrue(family.module.getOrphanReassignments().isEmpty());
        assertTrue(family.module.getTransfers().stream().noneMatch(t -> t.timing().equals("out_of_sample")));
        // A visible failure must not have removed the old balance before finding a recipient.
        family.people.addAll(Set.of(family.carer, family.replacement));
        family.units.add(family.safeHome);
        family.settle(2030);
        assertEquals(10_000, family.safeWealth.getWealthFinancial().getValue(), 1e-6);
    }

    @Test
    void noRecipientForNewEstateFailsRatherThanLoggingAnUnmatchedPayment() {
        Family family = new Family();
        family.people.removeAll(Set.of(family.carer, family.replacement));
        family.units.remove(family.safeHome);
        family.homeWealth.setWealthFinancialValue(30_000);
        family.mother.setSampleExit(SampleExit.Death);
        assertThrows(IllegalStateException.class, () -> family.settle(2030));
        assertEquals(30_000, family.homeWealth.getWealthFinancial().getValue(), 1e-6);
        assertTrue(family.module.getTransfers().isEmpty());
    }

    @Test
    void tiedRecipientsAreReproducibleRegardlessOfPopulationInsertionOrder() {
        Family first = new Family();
        Family second = new Family();
        for (Family family : List.of(first, second)) {
            family.establishTrust();
            Person tiedChild = person(6, 45);
            when(tiedChild.getBenefitUnit()).thenReturn(family.safeHome);
            family.safeHome.getMembers().add(tiedChild);
            family.people.add(tiedChild);
            family.mother.setSampleExit(SampleExit.Death);
        }
        List<Person> reversed = new ArrayList<>(second.people);
        Collections.reverse(reversed);
        second.people = new LinkedHashSet<>(reversed);
        first.settle(2030);
        second.settle(2030);
        assertEquals(first.module.getOrphanReassignments(), second.module.getOrphanReassignments());
    }

    @ParameterizedTest
    @EnumSource(value = SampleExit.class, names = {"Death", "EmigrationAlignment"})
    void aChildsDeathReturnsTrustToParentWhileEmigrationLeavesSample(SampleExit cause) {
        Family family = new Family();
        family.establishTrust();
        family.child.setSampleExit(cause);
        family.settle(2030);
        assertTrue(family.module.getOrphanExits().isEmpty());
        assertTrue(family.module.getOrphanReassignments().isEmpty());
        if (cause == SampleExit.Death) {
            assertEquals(10_000, family.homeWealth.getWealthFinancial().getValue(), 1e-6);
            assertTrue(family.module.getTransfers().stream().anyMatch(t ->
                    t.deceasedId() == 3 && t.recipientId() == 2 &&
                            t.reason().equals("deceased_minor_trust_parent") && t.value() == 10_000));
        } else {
            assertTrue(family.module.getTransfers().stream().anyMatch(t ->
                    t.recipientId() == 3 && t.timing().equals("out_of_sample") && t.value() == 10_000));
        }
    }

    @Test
    void survivingChildStillReceivesTheirOwnTrustAtAdulthoodExactlyOnce() {
        Family family = new Family();
        family.establishTrust();
        when(family.child.getDemAge()).thenReturn(17);
        family.module.releaseMatureTrusts(2036, family.people);
        assertEquals(0, family.homeWealth.getWealthFinancial().getValue(), 1e-6);
        assertEquals(10_000, family.module.getIlliquidAssetWealth(family.home), 1e-6);
        when(family.child.getDemAge()).thenReturn(18);
        // The balance is available before settlement, not just at year end.
        family.module.releaseMatureTrusts(2037, family.people);
        assertEquals(10_000, family.homeWealth.getWealthFinancial().getValue(), 1e-6);
        family.module.releaseMatureTrusts(2037, family.people);
        family.settle(2037);
        family.settle(2037);
        assertEquals(10_000, family.homeWealth.getWealthFinancial().getValue(), 1e-6);
        assertEquals(0, family.module.getIlliquidAssetWealth(family.home));
        assertEquals(1, family.module.getTransfers().stream().filter(t -> t.timing().equals("released")).count());
    }

    @Test
    void laterDeathYearTakesPriorityOverHigherParentId() {
        Family family = new Family();
        when(family.father.getId()).thenReturn(100L);
        when(family.child.getIdFather()).thenReturn(100L);
        family.establishTrust();
        family.mother.setSampleExit(SampleExit.Death);
        family.settle(2030);
        assertEquals(2, family.module.getOrphanExits().getFirst().referenceParentId());
        assertEquals(2, family.module.getOrphanReassignments().getFirst().referenceParentId());
    }

    @Test
    void maturityCreditsNewAdultUnitAndLaterDeathSettlesItsEstateOnce() {
        Family family = new Family();
        family.establishTrust();
        when(family.child.getDemAge()).thenReturn(18);
        family.home.getMembers().remove(family.child);
        WealthNonPension adultWealth = wealth(0);
        BenefitUnit adultUnit = unit(8, adultWealth, family.child);
        family.units.add(adultUnit);
        clearInvocations(family.home, adultUnit);

        family.module.releaseMatureTrusts(2037, family.people);
        assertEquals(10_000, adultWealth.getWealthFinancial().getValue(), 1e-6);
        assertEquals(0, family.homeWealth.getWealthFinancial().getValue(), 1e-6);
        assertEquals(0, family.module.getIlliquidAssetWealth(adultUnit), 1e-6);
        verify(family.home).updateTotalWealth();
        verify(adultUnit).updateTotalWealth();
        assertEquals(1, family.module.getDeathEvents().size()); // only the earlier father's death

        family.child.setSampleExit(SampleExit.Death);
        family.settle(2037);
        family.settle(2037);
        assertEquals(0, adultWealth.getWealthFinancial().getValue(), 1e-6);
        assertEquals(10_000, family.homeWealth.getWealthFinancial().getValue(), 1e-6);
        assertEquals(2, family.module.getDeathEvents().size());
    }

    @Test
    void liquidWealthBecomesAccessibleAtEighteen() {
        Person person = mock(Person.class, CALLS_REAL_METHODS);
        BenefitUnit unit = mock(BenefitUnit.class);
        when(unit.getHousehold()).thenReturn(mock(Household.class));
        when(unit.getMembers()).thenReturn(new HashSet<>());
        when(unit.getWealthTotValueForDecisions()).thenReturn(12_000.0);
        person.setBenefitUnit(unit);
        person.setDemAge(17);
        assertEquals(0, person.getLiquidWealth(), 1e-6);
        person.setDemAge(18);
        assertEquals(12_000, person.getLiquidWealth(), 1e-6);
        person.setDemAge(19);
        assertEquals(12_000, person.getLiquidWealth(), 1e-6);
    }

    @Test
    void matureInheritanceRemainsAvailableThroughAnnualWealthProjection() throws Exception {
        boolean savedProjection = Parameters.projectNonPensionWealth;
        Parameters.projectNonPensionWealth = true;
        try {
            Person adult = mock(Person.class, CALLS_REAL_METHODS);
            adult.setDemAge(18);
            adult.setWealthNonPensValueL1(0.0);
            BenefitUnit unit = mock(BenefitUnit.class, CALLS_REAL_METHODS);
            doReturn(8L).when(unit).getId();
            SimPathsModel model = mock(SimPathsModel.class);
            when(model.getInheritanceModule()).thenReturn(new InheritanceModule());
            setField(unit, "model", model);
            setField(unit, "wealthNonPensionL1", wealth(0));
            setField(unit, "yDispMonth", 0.0);
            setField(unit, "xDiscConsumptionAnnual", 1_200.0);
            doReturn(new LinkedHashSet<Person>()).when(unit).getMembers();
            doReturn(mock(Household.class)).when(unit).getHousehold();
            doReturn(adult).when(unit).getRefPerson();
            doReturn(null).when(unit).getMale();
            doReturn(null).when(unit).getFemale();
            doReturn(false).when(unit).getCoupleBoolean();
            doReturn(Occupancy.Single_Male).when(unit).getOccupancy();
            adult.setBenefitUnit(unit);

            unit.addMatureInheritanceCash(10_000);
            adult.addMatureInheritanceToPersonalFinancialBalance(10_000);
            assertEquals(10_000, adult.getLiquidWealth(), 1e-6);
            assertEquals(10_000, adult.getWealthNonPensValueL1(), 1e-6);

            // The normal projection must still run, retaining the inheritance
            // and deducting consumption once from both sets of accounts.
            unit.updateNonPensionWealth();
            unit.updateNetFinancialAssetsValue();
            unit.updateTotalWealth();
            adult.updateNonPensionWealth();
            assertEquals(8_800, adult.getLiquidWealth(), 1e-6);
            assertEquals(8_800, adult.getWealthNonPensValue(), 1e-6);
        } finally {
            Parameters.projectNonPensionWealth = savedProjection;
        }
    }

    private static void setField(BenefitUnit unit, String name, Object value) throws Exception {
        Field field = BenefitUnit.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(unit, value);
    }

    @Test
    void orphanTrustAndNewEstateUseYoungAdultBandOfLastDeceasedParent() {
        Family family = new Family();
        family.establishTrust();
        List<WealthNonPension> recipientWealth = new ArrayList<>();
        for (int age = 18; age <= 28; age++) {
            Person recipient = person(100 + age, age);
            WealthNonPension wealth = wealth(0);
            family.people.add(recipient);
            family.units.add(unit(100 + age, wealth, recipient));
            recipientWealth.add(wealth);
        }
        family.homeWealth.setWealthFinancialValue(30_000);
        family.mother.setSampleExit(SampleExit.Death);
        family.settle(2030);

        List<InheritanceModule.SyntheticMatch> matches = family.module.getSyntheticMatches();
        assertEquals(2, matches.size());
        assertTrue(matches.stream().allMatch(match -> match.referenceId() == 2 &&
                match.ageRule().equals("young_adult_band") &&
                match.targetAge() >= 18 && match.targetAge() <= 28 &&
                family.people.stream().anyMatch(person ->
                        person.getId() == match.recipientId() &&
                        person.getDemAge() >= 18 && person.getDemAge() <= 28)));
        assertEquals(matches.getFirst().recipientId(), matches.getLast().recipientId());
        assertEquals(40_000, recipientWealth.stream()
                .mapToDouble(wealth -> wealth.getWealthFinancial().getValue()).sum(), 1e-6);
        assertEquals(0, family.module.getIlliquidAssetWealth(family.home));
        assertEquals(SampleExit.Orphan, family.child.getSampleExit());
    }

    @Test
    void sameYearParentDeathsUseHigherPersonIdAndSettlePooledEstateOnce() {
        Family family = new Family();
        family.home.getMembers().add(family.father);
        when(family.home.getMale()).thenReturn(family.father);
        when(family.home.getFemale()).thenReturn(family.mother);
        when(family.father.getBenefitUnit()).thenReturn(family.home);
        family.people.add(family.father);
        family.homeWealth.setWealthFinancialValue(30_000);
        family.father.setSampleExit(SampleExit.Death);
        family.mother.setSampleExit(SampleExit.Death);
        family.settle(2030);
        assertEquals(2, family.module.getOrphanExits().getFirst().referenceParentId());
        assertEquals(2, family.module.getOrphanReassignments().getFirst().referenceParentId());
        assertEquals(30_000, family.safeWealth.getWealthFinancial().getValue(), 1e-6);
        assertEquals(1, family.module.getOrphanReassignments().size());
    }

    @Test
    void missingParentLinksUseAnExplicitDeceasedUnitAdultFallback() {
        Family family = new Family();
        when(family.child.getIdMother()).thenReturn(null);
        family.homeWealth.setWealthFinancialValue(30_000);
        family.mother.setSampleExit(SampleExit.Death);
        family.settle(2030);
        assertEquals(2, family.module.getOrphanExits().getFirst().referenceParentId());
        assertEquals("deceased_unit_adult_fallback", family.module.getOrphanExits().getFirst().referenceReason());
    }

    private static class Family {
        final InheritanceModule module = new InheritanceModule();
        final Person father = person(1, 40);
        final Person mother = person(2, 35);
        final Person child = person(3, 10);
        final Person carer = person(4, 45);
        final Person replacement = person(5, 10);
        final WealthNonPension fatherWealth = wealth(10_000);
        final WealthNonPension homeWealth = wealth(0);
        final WealthNonPension safeWealth = wealth(0);
        final BenefitUnit fatherHome = unit(1, fatherWealth, father);
        final BenefitUnit home = unit(2, homeWealth, mother, child);
        final BenefitUnit safeHome = unit(4, safeWealth, carer, replacement);
        Set<Person> people = new LinkedHashSet<>(List.of(mother, child, carer, replacement));
        final Set<BenefitUnit> units = new LinkedHashSet<>(List.of(home, safeHome));

        Family() {
            when(child.getIdFather()).thenReturn(1L);
            when(child.getIdMother()).thenReturn(2L);
        }

        void establishTrust() {
            father.setSampleExit(SampleExit.Death);
            people.add(father);
            units.add(fatherHome);
            settle(2029);
            assertEquals(10_000, module.getIlliquidAssetWealth(home), 1e-6);
            people.remove(father);
            units.remove(fatherHome);
        }

        void settle(int year) {
            module.releaseMatureTrusts(year, people);
            SimPathsModel.markOrphanExits(units);
            module.settleYear(year, people, units);
        }
    }

    private static Person person(long id, int age) {
        Person person = mock(Person.class);
        AtomicReference<SampleExit> exit = new AtomicReference<>(SampleExit.NotYet);
        when(person.getId()).thenReturn(id);
        when(person.getDemAge()).thenReturn(age);
        when(person.getDemMaleFlag()).thenReturn(Gender.Male);
        when(person.getRegion()).thenReturn(Region.UKC);
        when(person.getPrivatePension()).thenReturn(new PrivatePension());
        when(person.getSampleExit()).thenAnswer(ignored -> exit.get());
        doAnswer(invocation -> {
            assertEquals(SampleExit.NotYet, exit.get(), "A person must not exit twice");
            exit.set(invocation.getArgument(0));
            return null;
        }).when(person).setSampleExit(any(SampleExit.class));
        return person;
    }

    private static WealthNonPension wealth(double value) {
        WealthNonPension wealth = new WealthNonPension();
        wealth.setWealthFinancialValue(value);
        return wealth;
    }

    private static BenefitUnit unit(long id, WealthNonPension wealth, Person adult, Person... children) {
        BenefitUnit unit = mock(BenefitUnit.class);
        when(unit.getId()).thenReturn(id);
        Set<Person> members = new LinkedHashSet<>(List.of(children));
        members.add(adult);
        when(unit.getMembers()).thenReturn(members);
        when(unit.getMale()).thenReturn(adult);
        when(unit.getWealthNonPension()).thenAnswer(ignored -> new WealthNonPension(wealth));
        for (Person person : members)
            when(person.getBenefitUnit()).thenReturn(unit);
        doAnswer(invocation -> {
            wealth.setWealthFinancialValue(wealth.getWealthFinancial().getValue() +
                    invocation.getArgument(0, Double.class));
            return null;
        }).when(unit).addInheritanceCash(anyDouble());
        doAnswer(invocation -> {
            unit.addInheritanceCash(invocation.getArgument(0, Double.class));
            return null;
        }).when(unit).addMatureInheritanceCash(anyDouble());
        doAnswer(ignored -> {
            wealth.setWealthFinancialValue(0);
            return null;
        }).when(unit).clearEstateWealth();
        return unit;
    }
}
