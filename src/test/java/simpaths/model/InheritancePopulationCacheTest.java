package simpaths.model;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.Test;
import simpaths.data.startingpop.Processed;
import simpaths.model.benefitunit.WealthNonPension;
import simpaths.model.enums.*;
import simpaths.model.person.PrivatePension;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InheritancePopulationCacheTest {

    @Test
    void databaseRoundTripPreservesSourceIdentityAndUnsampledChildShare() {
        Scenario fresh = new Scenario(true);
        Person sourceChild = person(502, 45);
        sourceChild.setParentIds(null, 501L);
        Person unsampledChild = person(503, 40);
        unsampledChild.setParentIds(null, 501L);
        BenefitUnit sourceUnit = mock(BenefitUnit.class);
        when(sourceUnit.getMembers()).thenReturn(Set.of(sourceChild, unsampledChild));
        Household sourceHome = mock(Household.class);
        when(sourceHome.getBenefitUnits()).thenReturn(Set.of(sourceUnit));
        fresh.module.indexSourceFamilies(List.of(sourceHome));
        fresh.module.setFamilyLinkAudit(new InheritanceModule.FamilyLinkAudit(2019, 1, 1, 1, 0, 0));
        String lineage = fresh.module.exportPopulationLineage(fresh.people);

        EntityManagerFactory factory = factory();
        try {
            var em = factory.createEntityManager();
            em.getTransaction().begin();
            Processed cache = new Processed(Country.UK, 2019, 100, false);
            cache.setInheritanceLineage(lineage);
            em.persist(cache);
            em.getTransaction().commit();
            long id = cache.getId();
            em.clear();
            Processed loaded = em.find(Processed.class, id);
            assertTrue(loaded.hasCompatibleInheritanceLineage());

            Scenario cached = new Scenario(false);
            cached.module.restorePopulationLineage(loaded.getInheritanceLineage(), cached.people);
            assertEquals(501L, cached.donor.getInheritanceSourceId());
            assertEquals(502L, cached.child.getInheritanceSourceId());
            assertEquals(lineage, cached.module.exportPopulationLineage(cached.people));
            fresh.settle();
            cached.settle();
            assertEquals(50_000, fresh.childWealth.getWealthFinancial().getValue(), 1e-6);
            assertEquals(50_000, cached.childWealth.getWealthFinancial().getValue(), 1e-6);
            assertEquals(50_000, cached.matchedWealth.getWealthFinancial().getValue(), 1e-6);
            assertEquals(fresh.module.getTransfers(), cached.module.getTransfers());
            em.close();
        } finally {
            factory.close();
        }
    }

    @Test
    void cachedCloningKeepsParentIdsOutsideTheClonedHousehold() throws Exception {
        Person child = person(102, 45);
        Class<?> linkType = Class.forName("simpaths.model.SimPathsModel$PendingParentLink");
        var constructor = linkType.getDeclaredConstructor(Person.class, long.class,
                Long.class, Long.class, SampleEntry.class);
        constructor.setAccessible(true);
        Object link = constructor.newInstance(child, 102L, null, 101L, SampleEntry.ProcessedInputData);
        var remap = SimPathsModel.class.getDeclaredMethod("remapClonedParentLinks", Map.class, List.class);
        remap.setAccessible(true);
        SimPathsModel model = mock(SimPathsModel.class, CALLS_REAL_METHODS);

        // The father's household is cloned separately, so it is absent here.
        remap.invoke(model, Map.of(102L, child), List.of(link));
        assertEquals(101L, child.getIdFather());
        assertNull(child.getIdMother());
    }

    @Test
    void legacyCacheIsInvalidatedSoACompatiblePopulationCanBeRebuilt() throws Exception {
        Field factoryField = SimPathsModel.class.getDeclaredField("emfStartingPopulationPersist");
        factoryField.setAccessible(true);
        Object savedFactory = factoryField.get(null);
        String savedPath = SimPathsModel.getPersistDatabasePath();
        EntityManagerFactory factory = factory();
        try {
            factoryField.set(null, factory);
            var em = factory.createEntityManager();
            em.getTransaction().begin();
            Processed old = new Processed(Country.UK, 2019, 100, false);
            assertFalse(old.hasCompatibleInheritanceLineage());
            em.persist(old);
            em.getTransaction().commit();
            em.close();

            var lookup = SimPathsModel.class.getDeclaredMethod("getProcessed",
                    Country.class, int.class, int.class, boolean.class);
            lookup.setAccessible(true);
            SimPathsModel model = mock(SimPathsModel.class, CALLS_REAL_METHODS);
            assertNull(lookup.invoke(model, Country.UK, 2019, 100, false));
            em = factory.createEntityManager();
            assertEquals(0L, em.createQuery("select count(p) from Processed p", Long.class).getSingleResult());
            em.close();
        } finally {
            factoryField.set(null, savedFactory);
            SimPathsModel.setPersistDatabasePath(savedPath);
            factory.close();
        }
    }

    private static EntityManagerFactory factory() {
        return Persistence.createEntityManagerFactory("starting-population", Map.of(
                "hibernate.connection.url", "jdbc:h2:mem:inheritance_" + UUID.randomUUID(),
                "hibernate.hbm2ddl.auto", "create-drop"));
    }

    private static Person person(long id, int age) {
        Person p = mock(Person.class, CALLS_REAL_METHODS);
        doReturn(id).when(p).getId();
        p.setDemAge(age);
        doReturn(SampleExit.NotYet).when(p).getSampleExit();
        doReturn(Gender.Male).when(p).getDemMaleFlag();
        doReturn(Region.UKC).when(p).getRegion();
        doReturn(Education.Medium).when(p).getEduHighestC4();
        doReturn(new PrivatePension()).when(p).getPrivatePension();
        doReturn(null).when(p).getPartner();
        return p;
    }

    private static class Scenario {
        final InheritanceModule module = new InheritanceModule();
        final Person donor = person(101, 75);
        final Person child = person(102, 45);
        final Person matched = person(103, 50);
        final WealthNonPension childWealth = wealth(0);
        final WealthNonPension matchedWealth = wealth(0);
        final Set<Person> people = Set.of(donor, child, matched);
        final Set<BenefitUnit> units;

        Scenario(boolean sourceIds) {
            child.setParentIds(null, 101L); // parent lives in another benefit unit
            if (sourceIds) {
                donor.setInheritanceSourceId(501L);
                child.setInheritanceSourceId(502L);
                matched.setInheritanceSourceId(600L);
            }
            units = Set.of(unit(donor, wealth(100_000)), unit(child, childWealth), unit(matched, matchedWealth));
        }

        void settle() {
            doReturn(SampleExit.Death).when(donor).getSampleExit();
            module.settleYear(2020, people, units);
        }
    }

    private static WealthNonPension wealth(double value) {
        WealthNonPension wealth = new WealthNonPension();
        wealth.setWealthFinancialValue(value);
        return wealth;
    }

    private static BenefitUnit unit(Person person, WealthNonPension wealth) {
        BenefitUnit unit = mock(BenefitUnit.class);
        doReturn(unit).when(person).getBenefitUnit();
        when(unit.getMale()).thenReturn(person);
        when(unit.getMembers()).thenReturn(Set.of(person));
        when(unit.getWealthNonPension()).thenAnswer(ignored -> new WealthNonPension(wealth));
        doAnswer(call -> {
            wealth.setWealthFinancialValue(wealth.getWealthFinancial().getValue() + call.getArgument(0, Double.class));
            return null;
        }).when(unit).addInheritanceCash(anyDouble());
        doAnswer(call -> {
            wealth.setWealthFinancialValue(0);
            return null;
        }).when(unit).clearEstateWealth();
        return unit;
    }
}
