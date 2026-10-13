package simpaths.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import simpaths.model.benefitunit.WealthNonPension;
import simpaths.model.enums.SampleExit;
import simpaths.model.person.PrivatePension;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

class InheritanceModuleTest {

    @ParameterizedTest
    @EnumSource(value = SampleExit.class, names = {"Death", "DeathAlignment"})
    void lastAdultDeathTransfersEstateAndPensionExactlyOnce(SampleExit cause,
                                                          @TempDir Path output) throws Exception {
        Person exited = person(901, 76);
        Person child = person(902, 48);
        BenefitUnit estateUnit = mock(BenefitUnit.class);
        BenefitUnit childUnit = mock(BenefitUnit.class);
        WealthNonPension estate = wealth(30_000, 70_000);
        WealthNonPension childWealth = wealth(0, 0);
        wireWealth(estateUnit, estate);
        wireWealth(childUnit, childWealth);
        when(exited.getBenefitUnit()).thenReturn(estateUnit);
        when(child.getBenefitUnit()).thenReturn(childUnit);
        when(estateUnit.getMale()).thenReturn(exited);
        when(estateUnit.getMembers()).thenReturn(Set.of(exited));
        when(childUnit.getMale()).thenReturn(child);
        when(childUnit.getMembers()).thenReturn(Set.of(child));
        when(child.getIdMother()).thenReturn(901L);
        when(exited.getSampleExit()).thenReturn(cause);
        when(exited.getWealthPensValue(false)).thenReturn(20_000.0);
        PrivatePension pot = new PrivatePension();
        pot.setWealth(20_000);
        when(exited.getPrivatePension()).thenReturn(pot);

        InheritanceModule module = new InheritanceModule();
        module.settleYear(2030, Set.of(exited, child), Set.of(estateUnit, childUnit));

        assertEquals(120_000, childWealth.getWealthFinancial().getValue(), 1e-6);
        assertEquals(0, estate.getWealthFinancial().getValue(), 1e-6);
        assertEquals(0, estate.getWealthPrptyValue(), 1e-6);
        verify(estateUnit).removeDeceasedPensionStock(exited);
        assertTrue(module.getAlignmentFlows().isEmpty());
        assertEquals(1, module.getDeathEvents().size());
        assertEquals(cause, module.getDeathEvents().getFirst().deathCause());
        assertTrue(module.getTransfers().stream().allMatch(t ->
                t.deathCause().equals(cause.name())));
        module.settleYear(2030, Set.of(exited, child), Set.of(estateUnit, childUnit));
        assertEquals(120_000, childWealth.getWealthFinancial().getValue(), 1e-6);
        assertEquals(1, module.getDeathEvents().size());
        module.writeLedger(output);
        assertTrue(Files.readString(output.resolve("inheritance_transfers.csv"))
                .contains("death_cause"));
        assertTrue(Files.readString(output.resolve("inheritance_death_events.csv"))
                .contains("2030,901,0," + cause.name()));
        assertTrue(Files.readString(output.resolve("inheritance_method.txt"))
                .contains("alignment_deaths_trigger_bequests=true"));
    }

    @Test
    void alignmentRemovalOfOnePartnerKeepsPooledWealthWithSurvivor() {
        Person exited = person(911, 70);
        Person survivor = person(912, 68);
        BenefitUnit unit = mock(BenefitUnit.class);
        WealthNonPension wealth = wealth(40_000, 60_000);
        wireWealth(unit, wealth);
        when(exited.getBenefitUnit()).thenReturn(unit);
        when(survivor.getBenefitUnit()).thenReturn(unit);
        when(exited.getPartner()).thenReturn(survivor);
        when(unit.getMale()).thenReturn(exited);
        when(unit.getFemale()).thenReturn(survivor);
        when(unit.getMembers()).thenReturn(Set.of(exited, survivor));
        when(exited.getSampleExit()).thenReturn(SampleExit.DeathAlignment);
        when(exited.getWealthPensValue(false)).thenReturn(10_000.0);
        PrivatePension pot = new PrivatePension();
        pot.setWealth(10_000);
        when(exited.getPrivatePension()).thenReturn(pot);

        InheritanceModule module = new InheritanceModule();
        module.settleYear(2030, Set.of(exited, survivor), Set.of(unit));

        assertEquals(50_000, wealth.getWealthFinancial().getValue(), 1e-6);
        assertEquals(60_000, wealth.getWealthPrptyValue(), 1e-6);
        verify(survivor).addInheritanceToPersonalFinancialBalance(50_000);
        verify(survivor).addInheritanceToPersonalFinancialBalance(10_000);
        assertTrue(module.getAlignmentFlows().isEmpty());
        assertEquals(1, module.getTransfers().stream()
                .filter(t -> t.timing().equals("retained_in_unit")).count());
    }

    @ParameterizedTest
    @EnumSource(value = SampleExit.class, names = {"DeathAlignment", "EmigrationAlignment"})
    void naturalDeathWithExitingPartnerDistinguishesDeathFromEmigration(SampleExit partnerExit) {
        Person deceased = person(921, 74);
        Person exitedPartner = person(922, 72);
        Person child = person(923, 48);
        BenefitUnit estateUnit = mock(BenefitUnit.class);
        BenefitUnit childUnit = mock(BenefitUnit.class);
        WealthNonPension estate = wealth(30_000, 70_000);
        WealthNonPension childWealth = wealth(0, 0);
        wireWealth(estateUnit, estate);
        wireWealth(childUnit, childWealth);
        when(deceased.getBenefitUnit()).thenReturn(estateUnit);
        when(exitedPartner.getBenefitUnit()).thenReturn(estateUnit);
        when(child.getBenefitUnit()).thenReturn(childUnit);
        when(deceased.getPartner()).thenReturn(exitedPartner);
        when(estateUnit.getMale()).thenReturn(deceased);
        when(estateUnit.getFemale()).thenReturn(exitedPartner);
        when(estateUnit.getMembers()).thenReturn(Set.of(deceased, exitedPartner));
        when(childUnit.getMale()).thenReturn(child);
        when(childUnit.getMembers()).thenReturn(Set.of(child));
        when(child.getIdMother()).thenReturn(921L);
        when(deceased.getSampleExit()).thenReturn(SampleExit.Death);
        when(exitedPartner.getSampleExit()).thenReturn(partnerExit);
        when(exitedPartner.getPrivatePension()).thenReturn(new PrivatePension());
        PrivatePension pot = new PrivatePension();
        pot.setWealth(20_000);
        when(deceased.getPrivatePension()).thenReturn(pot);

        InheritanceModule module = new InheritanceModule();
        module.settleYear(2030, Set.of(deceased, exitedPartner, child),
                Set.of(estateUnit, childUnit));

        if (partnerExit == SampleExit.DeathAlignment) {
            assertEquals(120_000, childWealth.getWealthFinancial().getValue(), 1e-6);
            assertTrue(module.getAlignmentFlows().isEmpty());
            assertEquals(2, module.getDeathEvents().size());
            assertEquals(1, module.getTransfers().stream()
                    .filter(t -> t.reason().equals("nonpension_estate_child")).count());
        } else {
            assertTrue(module.getTransfers().isEmpty());
            assertEquals(0, childWealth.getWealthFinancial().getValue(), 1e-6);
            assertEquals(20_000, module.getAlignmentFlows().stream()
                    .filter(f -> f.event().equals("partner_pension_exit"))
                    .mapToDouble(InheritanceModule.AlignmentFlow::pension).sum(), 1e-6);
            assertEquals(100_000, module.getAlignmentFlows().stream()
                    .filter(f -> f.event().equals("unit_emigration"))
                    .mapToDouble(f -> f.financial() + f.housing()).sum(), 1e-6);
        }
    }

    @Test
    void alignmentEmigrationAndClonedEntryAreAuditedWithoutBequests() {
        Person emigrant = person(931, 45);
        BenefitUnit unit = mock(BenefitUnit.class);
        WealthNonPension wealth = wealth(20_000, 30_000);
        wireWealth(unit, wealth);
        when(emigrant.getBenefitUnit()).thenReturn(unit);
        when(unit.getMale()).thenReturn(emigrant);
        when(unit.getMembers()).thenReturn(Set.of(emigrant));
        when(unit.getWealthPensValue(false)).thenReturn(5_000.0, 0.0);

        InheritanceModule module = new InheritanceModule();
        module.recordAlignmentEntry(2029, unit);
        when(emigrant.getSampleExit()).thenReturn(SampleExit.EmigrationAlignment);
        when(emigrant.getWealthPensValue(false)).thenReturn(5_000.0);
        module.settleYear(2030, Set.of(emigrant), Set.of(unit));

        assertTrue(module.getTransfers().isEmpty());
        assertEquals(55_000, module.getAlignmentFlows().stream()
                .filter(f -> f.event().equals("unit_entry"))
                .mapToDouble(f -> f.financial() + f.housing() + f.pension()).sum(), 1e-6);
        assertEquals(50_000, module.getAlignmentFlows().stream()
                .filter(f -> f.event().equals("unit_emigration"))
                .mapToDouble(f -> f.financial() + f.housing()).sum(), 1e-6);
    }

    @Test
    void deathBeforeRetirementTransfersTheRemainingModelledPot() {
        Person deceased = person(101, 62);
        Person survivor = person(102, 60);
        BenefitUnit unit = mock(BenefitUnit.class);
        WealthNonPension wealth = wealth(0, 0);
        wireWealth(unit, wealth);
        when(deceased.getBenefitUnit()).thenReturn(unit);
        when(survivor.getBenefitUnit()).thenReturn(unit);
        when(deceased.getPartner()).thenReturn(survivor);
        when(survivor.getPartner()).thenReturn(deceased);
        when(unit.getMale()).thenReturn(deceased);
        when(unit.getFemale()).thenReturn(survivor);
        when(unit.getMembers()).thenReturn(Set.of(deceased, survivor));
        when(deceased.getSampleExit()).thenReturn(SampleExit.Death);
        when(deceased.getWealthPensValue(false)).thenReturn(20_000.0);
        PrivatePension pension = new PrivatePension();
        pension.setWealth(20_000);
        when(deceased.getPrivatePension()).thenReturn(pension);

        InheritanceModule module = new InheritanceModule();
        module.settleYear(2030, Set.of(deceased, survivor), Set.of(unit));

        assertEquals(20_000, wealth.getWealthFinancial().getValue(), 1e-6);
        verify(unit).removeDeceasedPensionStock(deceased);
        assertEquals(20_000, module.getTransfers().stream()
                .filter(t -> t.reason().equals("unaccessed_pension_partner"))
                .mapToDouble(InheritanceModule.Transfer::value).sum(), 1e-6);
    }

    @ParameterizedTest
    @EnumSource(value = SampleExit.class, names = {"Death", "DeathAlignment"})
    void retiredPersonDoesNotBequeathAStalePensionStock(SampleExit death) {
        Person deceased = person(111, 75);
        Person survivor = person(112, 74);
        BenefitUnit unit = mock(BenefitUnit.class);
        WealthNonPension wealth = wealth(0, 0);
        wireWealth(unit, wealth);
        when(deceased.getBenefitUnit()).thenReturn(unit);
        when(survivor.getBenefitUnit()).thenReturn(unit);
        when(deceased.getPartner()).thenReturn(survivor);
        when(unit.getMale()).thenReturn(deceased);
        when(unit.getFemale()).thenReturn(survivor);
        when(unit.getMembers()).thenReturn(Set.of(deceased, survivor));
        when(deceased.getSampleExit()).thenReturn(death);
        when(deceased.getWealthPensValue(false)).thenReturn(20_000.0);
        PrivatePension pension = new PrivatePension();
        pension.setWealth(20_000);
        pension.setPensioner(true);
        when(deceased.getPrivatePension()).thenReturn(pension);

        InheritanceModule module = new InheritanceModule();
        module.settleYear(2030, Set.of(deceased, survivor), Set.of(unit));

        assertEquals(0, wealth.getWealthFinancial().getValue(), 1e-6);
        verify(unit).removeDeceasedPensionStock(deceased);
        assertTrue(module.getTransfers().stream().noneMatch(t ->
                t.reason().startsWith("unaccessed_pension")));
    }

    @ParameterizedTest
    @EnumSource(value = SampleExit.class, names = {"Death", "DeathAlignment"})
    void pensionInheritanceAutomaticallyTransfersRemainingPotAlongsideEstate(SampleExit death) {
        Person deceased = person(201, 75);
        Person child = person(202, 45);
        BenefitUnit estateUnit = mock(BenefitUnit.class);
        BenefitUnit childUnit = mock(BenefitUnit.class);
        WealthNonPension estate = wealth(50_000, 50_000);
        WealthNonPension childWealth = wealth(0, 0);
        wireWealth(estateUnit, estate);
        wireWealth(childUnit, childWealth);
        when(deceased.getBenefitUnit()).thenReturn(estateUnit);
        when(child.getBenefitUnit()).thenReturn(childUnit);
        when(estateUnit.getMale()).thenReturn(deceased);
        when(estateUnit.getMembers()).thenReturn(Set.of(deceased));
        when(childUnit.getMale()).thenReturn(child);
        when(childUnit.getMembers()).thenReturn(Set.of(child));
        when(child.getIdMother()).thenReturn(201L);
        when(deceased.getSampleExit()).thenReturn(death);
        when(deceased.getWealthPensValue(false)).thenReturn(100_000.0);
        PrivatePension pension = new PrivatePension();
        pension.setWealth(100_000);
        when(deceased.getPrivatePension()).thenReturn(pension);

        InheritanceModule module = new InheritanceModule();
        module.settleYear(2030, Set.of(deceased, child), Set.of(estateUnit, childUnit));

        assertEquals(200_000, childWealth.getWealthFinancial().getValue(), 1e-6);
        assertEquals(0, estate.getWealthFinancial().getValue(), 1e-6);
        assertEquals(0, estate.getWealthPrptyValue(), 1e-6);
        verify(estateUnit).removeDeceasedPensionStock(deceased);
        assertEquals(100_000, module.getTransfers().stream()
                .filter(t -> t.reason().startsWith("unaccessed_pension"))
                .mapToDouble(InheritanceModule.Transfer::value).sum(), 1e-6);
    }

    @ParameterizedTest
    @EnumSource(value = SampleExit.class, names = {"Death", "DeathAlignment"})
    void firstDeathLeavesPooledWealthAndHomeWithSurvivorUntilFinalDeath(SampleExit firstDeath) {
        Person first = person(1, 75);
        Person survivor = person(2, 74);
        Person child = person(3, 45);
        BenefitUnit home = mock(BenefitUnit.class);
        BenefitUnit childUnit = mock(BenefitUnit.class);
        WealthNonPension homeWealth = wealth(200_000, 600_000);
        WealthNonPension childWealth = wealth(0, 0);
        wireWealth(home, homeWealth);
        wireWealth(childUnit, childWealth);
        when(first.getBenefitUnit()).thenReturn(home);
        when(survivor.getBenefitUnit()).thenReturn(home);
        when(child.getBenefitUnit()).thenReturn(childUnit);
        when(first.getPartner()).thenReturn(survivor);
        when(survivor.getPartner()).thenReturn(first);
        when(child.getIdMother()).thenReturn(1L);
        when(home.getMale()).thenReturn(first);
        when(home.getFemale()).thenReturn(survivor);
        when(home.getMembers()).thenReturn(Set.of(first, survivor));
        when(childUnit.getMale()).thenReturn(child);
        when(childUnit.getMembers()).thenReturn(Set.of(child));
        when(first.getSampleExit()).thenReturn(firstDeath);
        when(first.getWealthPensValue(false)).thenReturn(100_000.0);
        PrivatePension pot = new PrivatePension();
        pot.setWealth(100_000.0);
        when(first.getPrivatePension()).thenReturn(pot);
        when(survivor.getPrivatePension()).thenReturn(new PrivatePension());

        InheritanceModule module = new InheritanceModule();
        module.settleYear(2030, Set.of(first, survivor, child), Set.of(home, childUnit));

        assertEquals(300_000, homeWealth.getWealthFinancial().getValue(), 1e-6);
        assertEquals(600_000, homeWealth.getWealthPrptyValue(), 1e-6);
        assertEquals(0, childWealth.getWealthFinancial().getValue(), 1e-6);
        verify(survivor).addInheritanceToPersonalFinancialBalance(400_000);
        verify(survivor).addInheritanceToPersonalFinancialBalance(100_000);
        assertTrue(module.getTransfers().stream().anyMatch(t ->
                t.recipientId() != null && t.recipientId() == 2 &&
                        t.reason().equals("unaccessed_pension_partner")));

        when(survivor.getSampleExit()).thenReturn(SampleExit.DeathAlignment);
        when(home.getMale()).thenReturn(null);
        when(home.getMembers()).thenReturn(Set.of(survivor));
        module.settleYear(2031, Set.of(survivor, child), Set.of(home, childUnit));

        assertEquals(900_000, childWealth.getWealthFinancial().getValue(), 1e-6);
        assertEquals(0, homeWealth.getWealthFinancial().getValue(), 1e-6);
        assertEquals(0, homeWealth.getWealthPrptyValue(), 1e-6);
        assertTrue(module.getTransfers().stream().anyMatch(t ->
                t.recipientId() != null && t.recipientId() == 3 &&
                        t.reason().equals("nonpension_estate_child")));
    }

    @ParameterizedTest
    @EnumSource(value = SampleExit.class, names = {"Death", "DeathAlignment"})
    void childlessPensionAndNonpensionEstateUseTheSameTwoSyntheticRecipients(SampleExit death) {
        Person donor = person(10, 75);
        Person firstRecipient = person(11, 50);
        Person secondRecipient = person(12, 51);
        Person thirdRecipient = person(13, 52);
        BenefitUnit donorUnit = mock(BenefitUnit.class);
        BenefitUnit firstUnit = mock(BenefitUnit.class);
        BenefitUnit secondUnit = mock(BenefitUnit.class);
        BenefitUnit thirdUnit = mock(BenefitUnit.class);
        WealthNonPension donorWealth = wealth(100_000, 0);
        WealthNonPension firstWealth = wealth(0, 0);
        WealthNonPension secondWealth = wealth(0, 0);
        WealthNonPension thirdWealth = wealth(0, 0);
        wireWealth(donorUnit, donorWealth);
        wireWealth(firstUnit, firstWealth);
        wireWealth(secondUnit, secondWealth);
        wireWealth(thirdUnit, thirdWealth);
        when(donor.getBenefitUnit()).thenReturn(donorUnit);
        when(firstRecipient.getBenefitUnit()).thenReturn(firstUnit);
        when(secondRecipient.getBenefitUnit()).thenReturn(secondUnit);
        when(thirdRecipient.getBenefitUnit()).thenReturn(thirdUnit);
        when(donorUnit.getMale()).thenReturn(donor);
        when(donorUnit.getMembers()).thenReturn(Set.of(donor));
        when(firstUnit.getMale()).thenReturn(firstRecipient);
        when(firstUnit.getMembers()).thenReturn(Set.of(firstRecipient));
        when(secondUnit.getMale()).thenReturn(secondRecipient);
        when(secondUnit.getMembers()).thenReturn(Set.of(secondRecipient));
        when(thirdUnit.getMale()).thenReturn(thirdRecipient);
        when(thirdUnit.getMembers()).thenReturn(Set.of(thirdRecipient));
        when(donor.getSampleExit()).thenReturn(death);
        PrivatePension pension = new PrivatePension();
        pension.setWealth(20_000);
        when(donor.getPrivatePension()).thenReturn(pension);

        InheritanceModule module = new InheritanceModule();
        module.setSeed(606);
        module.settleYear(2030, Set.of(donor, firstRecipient, secondRecipient, thirdRecipient),
                Set.of(donorUnit, firstUnit, secondUnit, thirdUnit));

        assertEquals(120_000, firstWealth.getWealthFinancial().getValue() +
                secondWealth.getWealthFinancial().getValue() +
                thirdWealth.getWealthFinancial().getValue(), 1e-6);
        assertEquals(0, donorWealth.getWealthFinancial().getValue(), 1e-6);
        Set<Long> pensionRecipients = module.getTransfers().stream()
                .filter(t -> t.reason().startsWith("unaccessed_pension_synthetic_"))
                .map(InheritanceModule.Transfer::recipientId).collect(java.util.stream.Collectors.toSet());
        Set<Long> estateRecipients = module.getTransfers().stream()
                .filter(t -> t.reason().startsWith("nonpension_estate_synthetic_"))
                .map(InheritanceModule.Transfer::recipientId).collect(java.util.stream.Collectors.toSet());
        assertEquals(2, pensionRecipients.size());
        assertEquals(pensionRecipients, estateRecipients);
    }

    @Test
    void aKnownButUnlinkedChildKeepsAnEqualShare() {
        Person donor = person(20, 75);
        Person linked = person(21, 45);
        Person synthetic = person(22, 50);
        Person linkedSourceChild = person(30, 45);
        Person absentSourceChild = person(31, 40);
        when(donor.getInheritanceSourceId()).thenReturn(20L);
        when(linked.getInheritanceSourceId()).thenReturn(30L);
        when(linked.getIdMother()).thenReturn(20L);
        when(linkedSourceChild.getIdMother()).thenReturn(20L);
        when(absentSourceChild.getIdMother()).thenReturn(20L);
        BenefitUnit donorUnit = mock(BenefitUnit.class);
        BenefitUnit linkedUnit = mock(BenefitUnit.class);
        BenefitUnit syntheticUnit = mock(BenefitUnit.class);
        BenefitUnit sourceChildrenUnit = mock(BenefitUnit.class);
        WealthNonPension donorWealth = wealth(100_000, 0);
        WealthNonPension linkedWealth = wealth(0, 0);
        WealthNonPension syntheticWealth = wealth(0, 0);
        wireWealth(donorUnit, donorWealth);
        wireWealth(linkedUnit, linkedWealth);
        wireWealth(syntheticUnit, syntheticWealth);
        when(donor.getBenefitUnit()).thenReturn(donorUnit);
        when(linked.getBenefitUnit()).thenReturn(linkedUnit);
        when(synthetic.getBenefitUnit()).thenReturn(syntheticUnit);
        when(donorUnit.getMale()).thenReturn(donor);
        when(donorUnit.getMembers()).thenReturn(Set.of(donor));
        when(linkedUnit.getMale()).thenReturn(linked);
        when(linkedUnit.getMembers()).thenReturn(Set.of(linked));
        when(syntheticUnit.getMale()).thenReturn(synthetic);
        when(syntheticUnit.getMembers()).thenReturn(Set.of(synthetic));
        when(sourceChildrenUnit.getMembers()).thenReturn(Set.of(linkedSourceChild, absentSourceChild));
        when(donor.getSampleExit()).thenReturn(SampleExit.Death);
        when(donor.getPrivatePension()).thenReturn(new PrivatePension());
        Household sourceHousehold = mock(Household.class);
        when(sourceHousehold.getBenefitUnits()).thenReturn(Set.of(sourceChildrenUnit));

        InheritanceModule module = new InheritanceModule();
        module.indexSourceFamilies(List.of(sourceHousehold));
        module.settleYear(2030, Set.of(donor, linked, synthetic),
                Set.of(donorUnit, linkedUnit, syntheticUnit));

        assertEquals(50_000, linkedWealth.getWealthFinancial().getValue(), 1e-6);
        assertEquals(50_000, syntheticWealth.getWealthFinancial().getValue(), 1e-6);
        assertTrue(module.getTransfers().stream().anyMatch(t ->
                t.recipientId() != null && t.recipientId() == 22 &&
                        t.reason().contains("unresolved_child")));
    }

    @Test
    void sameYearDeathsSettlePooledEstateOnlyOnce() {
        Person first = person(41, 80);
        Person second = person(42, 80);
        Person child = person(43, 50);
        BenefitUnit home = mock(BenefitUnit.class);
        BenefitUnit childUnit = mock(BenefitUnit.class);
        WealthNonPension homeWealth = wealth(200_000, 300_000);
        WealthNonPension childWealth = wealth(0, 0);
        wireWealth(home, homeWealth);
        wireWealth(childUnit, childWealth);
        when(first.getBenefitUnit()).thenReturn(home);
        when(second.getBenefitUnit()).thenReturn(home);
        when(child.getBenefitUnit()).thenReturn(childUnit);
        when(home.getMale()).thenReturn(first);
        when(home.getFemale()).thenReturn(second);
        when(home.getMembers()).thenReturn(Set.of(first, second));
        when(childUnit.getMale()).thenReturn(child);
        when(childUnit.getMembers()).thenReturn(Set.of(child));
        when(first.getPartner()).thenReturn(second);
        when(second.getPartner()).thenReturn(first);
        when(child.getIdMother()).thenReturn(41L);
        when(child.getIdFather()).thenReturn(42L);
        when(first.getSampleExit()).thenReturn(SampleExit.Death);
        when(second.getSampleExit()).thenReturn(SampleExit.Death);
        when(first.getPrivatePension()).thenReturn(new PrivatePension());
        when(second.getPrivatePension()).thenReturn(new PrivatePension());

        InheritanceModule module = new InheritanceModule();
        module.settleYear(2030, Set.of(first, second, child), Set.of(home, childUnit));

        assertEquals(500_000, childWealth.getWealthFinancial().getValue(), 1e-6);
        assertEquals(0, homeWealth.getWealthFinancial().getValue(), 1e-6);
        assertEquals(1, module.getTransfers().stream().filter(t ->
                t.reason().equals("nonpension_estate_child")).count());
    }

    @Test
    void childrenFromAnEarlierPartnerRemainEligibleAfterRepartnering() {
        Person first = person(51, 75);
        Person survivor = person(52, 72);
        Person newPartner = person(53, 70);
        Person earlierChild = person(54, 40);
        Person laterChild = person(55, 35);
        BenefitUnit firstHome = mock(BenefitUnit.class);
        BenefitUnit laterHome = mock(BenefitUnit.class);
        BenefitUnit earlierChildUnit = mock(BenefitUnit.class);
        BenefitUnit laterChildUnit = mock(BenefitUnit.class);
        WealthNonPension firstWealth = wealth(100_000, 0);
        WealthNonPension laterWealth = wealth(200_000, 0);
        WealthNonPension earlierWealth = wealth(0, 0);
        WealthNonPension laterChildWealth = wealth(0, 0);
        wireWealth(firstHome, firstWealth);
        wireWealth(laterHome, laterWealth);
        wireWealth(earlierChildUnit, earlierWealth);
        wireWealth(laterChildUnit, laterChildWealth);
        when(first.getBenefitUnit()).thenReturn(firstHome);
        when(survivor.getBenefitUnit()).thenReturn(firstHome);
        when(newPartner.getBenefitUnit()).thenReturn(laterHome);
        when(earlierChild.getBenefitUnit()).thenReturn(earlierChildUnit);
        when(laterChild.getBenefitUnit()).thenReturn(laterChildUnit);
        when(first.getPartner()).thenReturn(survivor);
        when(survivor.getPartner()).thenReturn(newPartner);
        when(newPartner.getPartner()).thenReturn(survivor);
        when(earlierChild.getIdMother()).thenReturn(51L);
        when(laterChild.getIdFather()).thenReturn(53L);
        when(firstHome.getMale()).thenReturn(first);
        when(firstHome.getFemale()).thenReturn(survivor);
        when(firstHome.getMembers()).thenReturn(Set.of(first, survivor));
        when(laterHome.getMale()).thenReturn(newPartner);
        when(laterHome.getFemale()).thenReturn(survivor);
        when(laterHome.getMembers()).thenReturn(Set.of(newPartner, survivor));
        when(earlierChildUnit.getMale()).thenReturn(earlierChild);
        when(earlierChildUnit.getMembers()).thenReturn(Set.of(earlierChild));
        when(laterChildUnit.getMale()).thenReturn(laterChild);
        when(laterChildUnit.getMembers()).thenReturn(Set.of(laterChild));
        when(first.getSampleExit()).thenReturn(SampleExit.Death);
        when(first.getPrivatePension()).thenReturn(new PrivatePension());
        when(survivor.getPrivatePension()).thenReturn(new PrivatePension());
        when(newPartner.getPrivatePension()).thenReturn(new PrivatePension());

        InheritanceModule module = new InheritanceModule();
        module.settleYear(2030, Set.of(first, survivor, newPartner, earlierChild, laterChild),
                Set.of(firstHome, laterHome, earlierChildUnit, laterChildUnit));
        when(survivor.getBenefitUnit()).thenReturn(laterHome);
        when(survivor.getSampleExit()).thenReturn(SampleExit.Death);
        module.settleYear(2031, Set.of(survivor, newPartner, earlierChild, laterChild),
                Set.of(laterHome, earlierChildUnit, laterChildUnit));
        when(newPartner.getSampleExit()).thenReturn(SampleExit.Death);
        when(laterHome.getFemale()).thenReturn(null);
        when(laterHome.getMembers()).thenReturn(Set.of(newPartner));
        module.settleYear(2032, Set.of(newPartner, earlierChild, laterChild),
                Set.of(laterHome, earlierChildUnit, laterChildUnit));

        assertEquals(100_000, earlierWealth.getWealthFinancial().getValue(), 1e-6);
        assertEquals(100_000, laterChildWealth.getWealthFinancial().getValue(), 1e-6);
    }

    @Test
    void childWhoDiesInAlignmentIsRepresentedByLivingDescendant() {
        Person donor = person(301, 85);
        Person child = person(302, 55);
        Person grandchild = person(303, 30);
        BenefitUnit donorUnit = mock(BenefitUnit.class);
        BenefitUnit childUnit = mock(BenefitUnit.class);
        BenefitUnit heirUnit = mock(BenefitUnit.class);
        WealthNonPension estate = wealth(40_000, 60_000);
        WealthNonPension childWealth = wealth(0, 0);
        WealthNonPension heirWealth = wealth(0, 0);
        wireWealth(donorUnit, estate);
        wireWealth(childUnit, childWealth);
        wireWealth(heirUnit, heirWealth);
        when(donor.getBenefitUnit()).thenReturn(donorUnit);
        when(child.getBenefitUnit()).thenReturn(childUnit);
        when(grandchild.getBenefitUnit()).thenReturn(heirUnit);
        when(donorUnit.getMale()).thenReturn(donor);
        when(childUnit.getMale()).thenReturn(child);
        when(heirUnit.getMale()).thenReturn(grandchild);
        when(donorUnit.getMembers()).thenReturn(Set.of(donor));
        when(childUnit.getMembers()).thenReturn(Set.of(child));
        when(heirUnit.getMembers()).thenReturn(Set.of(grandchild));
        when(child.getIdFather()).thenReturn(301L);
        when(grandchild.getIdFather()).thenReturn(302L);
        when(donor.getSampleExit()).thenReturn(SampleExit.Death);
        when(child.getSampleExit()).thenReturn(SampleExit.DeathAlignment);
        when(donor.getPrivatePension()).thenReturn(new PrivatePension());
        when(child.getPrivatePension()).thenReturn(new PrivatePension());

        InheritanceModule module = new InheritanceModule();
        module.settleYear(2030, Set.of(donor, child, grandchild),
                Set.of(donorUnit, childUnit, heirUnit));

        assertEquals(100_000, heirWealth.getWealthFinancial().getValue(), 1e-6);
        assertEquals(0, estate.getWealthFinancial().getValue(), 1e-6);
        assertTrue(module.getAlignmentFlows().isEmpty());
        assertTrue(module.getTransfers().stream().allMatch(t -> t.recipientId() == 303));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void pooledEstateCountsSharedLinkedAndSourceChildOnce(boolean additionalUnresolvedChild) {
        Person mother = person(601, 75);
        Person father = person(602, 74);
        Person child = person(603, 45);
        Person synthetic = person(604, 50);
        when(mother.getInheritanceSourceId()).thenReturn(701L);
        when(father.getInheritanceSourceId()).thenReturn(702L);
        when(child.getInheritanceSourceId()).thenReturn(703L);
        when(child.getIdMother()).thenReturn(601L);
        when(mother.getSampleExit()).thenReturn(SampleExit.Death);
        when(father.getSampleExit()).thenReturn(SampleExit.DeathAlignment);
        when(mother.getPrivatePension()).thenReturn(new PrivatePension());
        when(father.getPrivatePension()).thenReturn(new PrivatePension());
        BenefitUnit home = mock(BenefitUnit.class);
        BenefitUnit childUnit = mock(BenefitUnit.class);
        BenefitUnit syntheticUnit = mock(BenefitUnit.class);
        WealthNonPension estate = wealth(100_000, 0);
        WealthNonPension childWealth = wealth(0, 0);
        WealthNonPension syntheticWealth = wealth(0, 0);
        wireWealth(home, estate);
        wireWealth(childUnit, childWealth);
        wireWealth(syntheticUnit, syntheticWealth);
        when(mother.getBenefitUnit()).thenReturn(home);
        when(father.getBenefitUnit()).thenReturn(home);
        when(child.getBenefitUnit()).thenReturn(childUnit);
        when(synthetic.getBenefitUnit()).thenReturn(syntheticUnit);
        when(home.getMale()).thenReturn(father);
        when(home.getFemale()).thenReturn(mother);
        when(home.getMembers()).thenReturn(Set.of(mother, father));
        when(childUnit.getMale()).thenReturn(child);
        when(childUnit.getMembers()).thenReturn(Set.of(child));
        when(syntheticUnit.getMale()).thenReturn(synthetic);
        when(syntheticUnit.getMembers()).thenReturn(Set.of(synthetic));
        Person sourceChild = person(703, 45);
        Person otherSourceChild = person(704, 40);
        when(sourceChild.getIdMother()).thenReturn(701L);
        when(sourceChild.getIdFather()).thenReturn(702L);
        when(otherSourceChild.getIdMother()).thenReturn(701L);
        when(otherSourceChild.getIdFather()).thenReturn(702L);
        BenefitUnit sourceUnit = mock(BenefitUnit.class);
        when(sourceUnit.getMembers()).thenReturn(additionalUnresolvedChild
                ? Set.of(sourceChild, otherSourceChild) : Set.of(sourceChild));
        Household sourceHome = mock(Household.class);
        when(sourceHome.getBenefitUnits()).thenReturn(Set.of(sourceUnit));
        InheritanceModule module = new InheritanceModule();
        module.indexSourceFamilies(List.of(sourceHome));
        module.settleYear(2030, Set.of(mother, father, child, synthetic),
                Set.of(home, childUnit, syntheticUnit));

        assertEquals(additionalUnresolvedChild ? 50_000 : 100_000,
                childWealth.getWealthFinancial().getValue(), 1e-6);
        assertEquals(additionalUnresolvedChild ? 50_000 : 0,
                syntheticWealth.getWealthFinancial().getValue(), 1e-6);
        assertEquals(100_000, module.getTransfers().stream()
                .filter(t -> t.asset().equals("cash") && t.timing().equals("immediate"))
                .mapToDouble(InheritanceModule.Transfer::value).sum(), 1e-6);
        assertEquals(0, estate.getWealthFinancial().getValue(), 1e-6);
    }

    @Test
    void differentSimulatedChildrenWithSameSourceIdKeepSeparateShares() {
        Person donor = person(801, 75);
        Person firstClone = person(802, 45);
        Person secondClone = person(803, 45);
        when(firstClone.getInheritanceSourceId()).thenReturn(900L);
        when(secondClone.getInheritanceSourceId()).thenReturn(900L);
        when(firstClone.getIdMother()).thenReturn(801L);
        when(secondClone.getIdMother()).thenReturn(801L);
        when(donor.getSampleExit()).thenReturn(SampleExit.Death);
        when(donor.getPrivatePension()).thenReturn(new PrivatePension());
        BenefitUnit home = mock(BenefitUnit.class);
        BenefitUnit firstUnit = mock(BenefitUnit.class);
        BenefitUnit secondUnit = mock(BenefitUnit.class);
        WealthNonPension estate = wealth(100_000, 0);
        WealthNonPension firstWealth = wealth(0, 0);
        WealthNonPension secondWealth = wealth(0, 0);
        wireWealth(home, estate);
        wireWealth(firstUnit, firstWealth);
        wireWealth(secondUnit, secondWealth);
        when(donor.getBenefitUnit()).thenReturn(home);
        when(firstClone.getBenefitUnit()).thenReturn(firstUnit);
        when(secondClone.getBenefitUnit()).thenReturn(secondUnit);
        when(home.getMale()).thenReturn(donor);
        when(home.getMembers()).thenReturn(Set.of(donor));
        when(firstUnit.getMale()).thenReturn(firstClone);
        when(firstUnit.getMembers()).thenReturn(Set.of(firstClone));
        when(secondUnit.getMale()).thenReturn(secondClone);
        when(secondUnit.getMembers()).thenReturn(Set.of(secondClone));
        InheritanceModule module = new InheritanceModule();
        module.settleYear(2030, Set.of(donor, firstClone, secondClone), Set.of(home, firstUnit, secondUnit));

        assertEquals(50_000, firstWealth.getWealthFinancial().getValue(), 1e-6);
        assertEquals(50_000, secondWealth.getWealthFinancial().getValue(), 1e-6);
        assertEquals(2, module.getTransfers().stream()
                .filter(t -> t.reason().equals("nonpension_estate_child")).count());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void pooledEstateUsesResponsibleAdultWhenHigherIdDependentAlsoDies(int adultCount) {
        Person father = person(10, 80);
        Person mother = person(11, 78);
        Person dependent = person(20, 17);
        when(father.getSampleExit()).thenReturn(SampleExit.Death);
        when(mother.getSampleExit()).thenReturn(SampleExit.Death);
        when(dependent.getSampleExit()).thenReturn(SampleExit.DeathAlignment);
        when(dependent.getIdFather()).thenReturn(10L);
        if (adultCount == 2)
            when(dependent.getIdMother()).thenReturn(11L);
        BenefitUnit home = mock(BenefitUnit.class);
        WealthNonPension estate = wealth(30_000, 70_000);
        wireWealth(home, estate);
        when(home.getMale()).thenReturn(father);
        if (adultCount == 2)
            when(home.getFemale()).thenReturn(mother);
        Set<Person> family = adultCount == 2
                ? Set.of(father, mother, dependent) : Set.of(father, dependent);
        when(home.getMembers()).thenReturn(family);
        Set<Person> population = new HashSet<>(family);
        Set<BenefitUnit> units = new HashSet<>(Set.of(home));
        for (Person member : family) {
            when(member.getBenefitUnit()).thenReturn(home);
            when(member.getPrivatePension()).thenReturn(new PrivatePension());
        }
        // All exact target ages have two eligible people, so matching must
        // retain the 20-30-year gap rather than fall back from a minor's age.
        for (int age = 48; age <= 60; age++) {
            for (int copy = 0; copy < 2; copy++) {
                Person recipient = person(100 + age * 2L + copy, age);
                BenefitUnit recipientUnit = mock(BenefitUnit.class);
                wireWealth(recipientUnit, wealth(0, 0));
                when(recipient.getBenefitUnit()).thenReturn(recipientUnit);
                when(recipientUnit.getMale()).thenReturn(recipient);
                when(recipientUnit.getMembers()).thenReturn(Set.of(recipient));
                population.add(recipient);
                units.add(recipientUnit);
            }
        }
        InheritanceModule module = new InheritanceModule();
        module.settleYear(2030, population, units);
        Person reference = adultCount == 2 ? mother : father;
        assertEquals(2, module.getTransfers().size());
        assertEquals(100_000, module.getTransfers().stream()
                .mapToDouble(InheritanceModule.Transfer::value).sum(), 1e-6);
        for (InheritanceModule.Transfer transfer : module.getTransfers()) {
            assertEquals(reference.getId(), transfer.deceasedId());
            assertEquals(SampleExit.Death.name(), transfer.deathCause());
            Person recipient = population.stream()
                    .filter(p -> p.getId() == transfer.recipientId()).findFirst().orElseThrow();
            int gap = reference.getDemAge() - recipient.getDemAge();
            assertTrue(gap >= 20 && gap <= 30);
            assertEquals("cash", transfer.asset());
            assertEquals("immediate", transfer.timing());
            verify(recipient.getBenefitUnit()).addInheritanceCash(50_000.0);
        }
        assertEquals(family.size(), module.getDeathEvents().size());
        assertEquals(0, estate.getWealthFinancial().getValue(), 1e-6);
        assertEquals(0, estate.getWealthPrptyValue(), 1e-6);
        module.settleYear(2030, population, units);
        assertEquals(2, module.getTransfers().size());
    }

    private static Person person(long id, int age) {
        Person person = mock(Person.class);
        when(person.getId()).thenReturn(id);
        when(person.getDemAge()).thenReturn(age);
        when(person.getSampleExit()).thenReturn(SampleExit.NotYet);
        return person;
    }

    private static WealthNonPension wealth(double financial, double house) {
        WealthNonPension wealth = new WealthNonPension();
        wealth.setWealthFinancialValue(financial);
        wealth.getWealthHousing().setWealthPrptyValue(house);
        return wealth;
    }

    private static void wireWealth(BenefitUnit unit, WealthNonPension wealth) {
        when(unit.getWealthNonPension()).thenAnswer(ignored -> new WealthNonPension(wealth));
        doAnswer(invocation -> {
            wealth.setWealthFinancialValue(wealth.getWealthFinancial().getValue() +
                    invocation.getArgument(0, Double.class));
            return null;
        }).when(unit).addInheritanceCash(anyDouble());
        doAnswer(ignored -> {
            wealth.getWealthHousing().setWealthPrptyValue(0);
            wealth.getWealthHousing().setWealthMortgageDebtValue(0);
            wealth.setWealthFinancialValue(0);
            return null;
        }).when(unit).clearEstateWealth();
    }
}
