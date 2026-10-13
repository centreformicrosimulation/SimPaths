package simpaths.model;

import simpaths.data.Parameters;
import simpaths.model.enums.Education;
import simpaths.model.enums.Les_c4;
import simpaths.model.enums.SampleExit;
import simpaths.model.person.PrivatePension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * The wealth module's pooled bequest rule. Non-pension wealth stays with a
 * surviving responsible adult; the remaining unit estate passes at the last
 * adult's death. A remaining modelled pension pot is distributed at its owner's
 * death before retirement.
 */
public final class InheritanceModule {

    private static final double EPS = 1.0e-6;
    private final Map<Long, Person> knownPeople = new HashMap<>();
    private final Map<Long, List<Person>> childrenByParent = new HashMap<>();
    private final Map<Long, Set<Long>> sourceChildren = new HashMap<>();
    private final Map<Long, Map<String, ChildBranch>> carriedBranches = new HashMap<>();
    private final Map<Long, Double> minorTrusts = new HashMap<>();
    private final Map<Long, BenefitUnit> trustUnits = new HashMap<>();
    private final Map<Long, Integer> deathYears = new HashMap<>();
    private final Map<Long, OrphanReference> orphanReferences = new HashMap<>();
    private final Map<Long, Match> orphanRecipients = new HashMap<>();
    private final Set<Long> settledDeaths = new HashSet<>();
    private final List<Transfer> transfers = new ArrayList<>();
    private final List<BalanceSnapshot> balanceSnapshots = new ArrayList<>();
    private final List<SyntheticMatch> syntheticMatches = new ArrayList<>();
    private final List<AlignmentFlow> alignmentFlows = new ArrayList<>();
    private final List<DeathEvent> deathEvents = new ArrayList<>();
    private final Set<Long> recordedOrphans = new HashSet<>();
    private final List<OrphanExit> orphanExits = new ArrayList<>();
    private final List<OrphanReassignment> orphanReassignments = new ArrayList<>();
    private Set<Long> currentPopulationIds = Set.of();
    private FamilyLinkAudit familyLinkAudit;
    private long seed = 606L;
    private int syntheticRecipientCount = 2;

    public record Transfer(int year, long deceasedId, Long recipientId, String asset,
                           String timing, double value, String reason, String deathCause) { }
    public record DeathEvent(int year, long deceasedId, long benefitUnitId,
                             SampleExit deathCause) { }
    public record OrphanExit(int year, long personId, long benefitUnitId, int age,
                             Long referenceParentId, String referenceReason) { }
    public record OrphanReassignment(int year, long orphanId, Long deceasedId,
                                     long recipientId, long referenceParentId, String referenceReason,
                                     String purpose, String tier, double value) { }
    public record BalanceSnapshot(int year, long benefitUnitId, double claimAssets,
                                  double claimLiabilities, double minorTrustAssets,
                                  double totalNetWealth) { }
    public record FamilyLinkAudit(int year, long validReferences,
                                  long resolvedAcrossHouseholds, long unresolvedReferences,
                                  long validImmigrantReferences,
                                  long unresolvedImmigrantReferences) { }
    public record SyntheticMatch(int year, long deceasedId, Long recipientId, String purpose,
                                 int intendedCount, int actualCount, int slot, int ageGap, String tier,
                                 double value, long referenceId, int targetAge, int actualAgeGap,
                                 String ageRule, int educationDistance, boolean sameRegion) { }
    public record AlignmentFlow(int year, String event, long benefitUnitId, Long personId,
                                double financial, double housing, double pension,
                                double minorTrust) { }
    private record ChildBranch(String key, Long childId, String origin) { }
    private record Heir(Person person, double weight, String origin, Long orphanId) {
        Heir(Person person, double weight, String origin) {
            this(person, weight, origin, null);
        }
    }
    private record Match(Person person, int targetAge, String ageRule, int educationDistance,
                         boolean sameRegion, String tier) {
        Match withTier(String newTier) {
            return new Match(person, targetAge, ageRule, educationDistance, sameRegion, newTier);
        }
    }
    private record OrphanReference(Person person, String reason) { }
    private record UnitSnapshot(double financial, double housing, int adults) {
        double net() { return financial + housing; }
    }

    public void setSeed(long seed) { this.seed = seed; }

    public void setSyntheticRecipientCount(int count) {
        if (count < 1)
            throw new IllegalArgumentException("At least one synthetic recipient is required");
        syntheticRecipientCount = count;
    }

    public int getSyntheticRecipientCount() { return syntheticRecipientCount; }

    /** Index source families before independent sampling obscures some links. */
    public void indexSourceFamilies(List<Household> inputHouseholds) {
        sourceChildren.clear();
        for (Household household : inputHouseholds)
            for (BenefitUnit unit : household.getBenefitUnits())
                for (Person child : unit.getMembers()) {
                    if (child.getInheritanceSourceId() == null)
                        child.setInheritanceSourceId(child.getId());
                    addSourceChild(child.getIdMother(), child.getId());
                    addSourceChild(child.getIdFather(), child.getId());
                }
    }

    /** Cache source identities, including child branches omitted by sampling. */
    public String exportPopulationLineage(Set<Person> persons) {
        if (familyLinkAudit == null)
            throw new IllegalStateException("Cannot cache a population without its family-link audit");
        StringBuilder data = new StringBuilder("A," + familyLinkAudit.year() + "," +
                familyLinkAudit.validReferences() + "," + familyLinkAudit.resolvedAcrossHouseholds() +
                "," + familyLinkAudit.unresolvedReferences() + "\n");
        persons.stream().sorted(Comparator.comparingLong(Person::getId)).forEach(person -> {
            Long sourceId = person.getInheritanceSourceId();
            if (sourceId == null)
                throw new IllegalStateException("Missing source identity for cached person " + person.getId());
            data.append("P,").append(person.getId()).append(',').append(sourceId).append('\n');
        });
        new TreeMap<>(sourceChildren).forEach((parent, children) ->
                new TreeSet<>(children).forEach(child ->
                        data.append("C,").append(parent).append(',').append(child).append('\n')));
        return data.toString();
    }

    public void restorePopulationLineage(String data, Set<Person> persons) {
        Map<Long, Long> sources = new HashMap<>();
        Map<Long, Set<Long>> families = new HashMap<>();
        FamilyLinkAudit audit = null;
        for (String line : data.lines().toList()) {
            String[] fields = line.split(",");
            if (fields.length == 5 && fields[0].equals("A") && audit == null) {
                audit = new FamilyLinkAudit(Integer.parseInt(fields[1]), Long.parseLong(fields[2]),
                        Long.parseLong(fields[3]), Long.parseLong(fields[4]), 0, 0);
            } else if (fields.length == 3 && fields[0].equals("P")) {
                long id = Long.parseLong(fields[1]);
                long source = Long.parseLong(fields[2]);
                if (id <= 0 || source <= 0 || sources.putIfAbsent(id, source) != null)
                    throw new IllegalArgumentException("Invalid source identity in population cache");
            } else if (fields.length == 3 && fields[0].equals("C")) {
                long parent = Long.parseLong(fields[1]);
                long child = Long.parseLong(fields[2]);
                if (parent <= 0 || child <= 0 || parent == child)
                    throw new IllegalArgumentException("Invalid source family in population cache");
                families.computeIfAbsent(parent, ignored -> new TreeSet<>()).add(child);
            } else {
                throw new IllegalArgumentException("Malformed inheritance lineage in population cache");
            }
        }
        if (audit == null || sources.size() != persons.size() ||
                persons.stream().anyMatch(person -> !sources.containsKey(person.getId())))
            throw new IllegalArgumentException("Incomplete inheritance lineage in population cache");
        sourceChildren.clear();
        sourceChildren.putAll(families);
        familyLinkAudit = audit;
        for (Person person : persons)
            person.setInheritanceSourceId(sources.get(person.getId()));
    }

    private void addSourceChild(Long parentId, long childId) {
        if (parentId != null && parentId > 0 && parentId != childId)
            sourceChildren.computeIfAbsent(parentId, ignored -> new TreeSet<>()).add(childId);
    }

    public double getIlliquidAssetWealth(BenefitUnit unit) {
        double result = 0.0;
        for (Person person : unit.getMembers())
            if (SampleExit.NotYet.equals(person.getSampleExit()))
                result += minorTrusts.getOrDefault(person.getId(), 0.0);
        return result;
    }

    public List<Transfer> getTransfers() { return List.copyOf(transfers); }
    public List<AlignmentFlow> getAlignmentFlows() { return List.copyOf(alignmentFlows); }
    public List<DeathEvent> getDeathEvents() { return List.copyOf(deathEvents); }
    public List<OrphanExit> getOrphanExits() { return List.copyOf(orphanExits); }
    public List<OrphanReassignment> getOrphanReassignments() { return List.copyOf(orphanReassignments); }
    public List<SyntheticMatch> getSyntheticMatches() { return List.copyOf(syntheticMatches); }

    public void setFamilyLinkAudit(FamilyLinkAudit audit) { familyLinkAudit = audit; }

    public void updateImmigrantLinkAudit(long valid, long unresolved) {
        if (familyLinkAudit == null)
            throw new IllegalStateException("Starting family-link audit is unavailable");
        familyLinkAudit = new FamilyLinkAudit(familyLinkAudit.year(),
                familyLinkAudit.validReferences(), familyLinkAudit.resolvedAcrossHouseholds(),
                familyLinkAudit.unresolvedReferences(), valid, unresolved);
    }

    /** Copy trusts and lineage when population alignment clones an existing unit. */
    public void cloneBalances(Map<Long, Person> clonedPersons) {
        for (Map.Entry<Long, Person> entry : clonedPersons.entrySet()) {
            double trust = minorTrusts.getOrDefault(entry.getKey(), 0.0);
            if (trust > EPS) {
                minorTrusts.put(entry.getValue().getId(), trust);
                trustUnits.put(entry.getValue().getId(), entry.getValue().getBenefitUnit());
            }
            Map<String, ChildBranch> original = carriedBranches.get(entry.getKey());
            if (original == null)
                continue;
            Map<String, ChildBranch> copy = new LinkedHashMap<>();
            for (ChildBranch branch : original.values()) {
                Person linked = branch.childId() == null ? null : clonedPersons.get(branch.childId());
                ChildBranch cloned = linked == null
                        ? new ChildBranch(branch.key(), null, "unresolved_cloned_child")
                        : new ChildBranch("person:" + linked.getId(), linked.getId(), branch.origin());
                copy.put(cloned.key(), cloned);
            }
            carriedBranches.put(entry.getValue().getId(), copy);
        }
    }

    /** Wealth brought into the simulated sample by an alignment-created unit. */
    public void recordAlignmentEntry(int year, BenefitUnit unit) {
        UnitSnapshot stock = snapshot(unit);
        alignmentFlow(year, "unit_entry", unit, null, stock.financial(),
                stock.housing(), unit.getWealthPensValue(false), trustStock(unit));
    }

    private void indexCurrentPopulation(Set<Person> persons) {
        Set<Long> activeIds = new HashSet<>();
        for (Person person : persons) {
            activeIds.add(person.getId());
            if (knownPeople.putIfAbsent(person.getId(), person) == null) {
                registerChild(person.getIdMother(), person);
                if (!Objects.equals(person.getIdMother(), person.getIdFather()))
                    registerChild(person.getIdFather(), person);
            }
        }
        currentPopulationIds = activeIds;
    }

    /** Make earlier inheritances available as soon as their holders reach adulthood. */
    public void releaseMatureTrusts(int year, Set<Person> persons) {
        indexCurrentPopulation(persons);
        Set<BenefitUnit> movedUnits = new LinkedHashSet<>();
        for (Long id : minorTrusts.keySet()) {
            Person holder = knownPeople.get(id);
            if (holder != null && isLiving(holder)) {
                BenefitUnit current = holder.getBenefitUnit();
                BenefitUnit previous = trustUnits.put(id, current);
                if (previous != null && previous != current) {
                    movedUnits.add(previous);
                    movedUnits.add(current);
                }
            }
        }
        releaseAdultTrusts(year);
        // Ageing can move the holder to a new unit. Remove the trust from
        // the former unit's resources and include it in the current unit.
        movedUnits.forEach(BenefitUnit::updateTotalWealth);
    }

    public void settleYear(int year, Set<Person> persons, Set<BenefitUnit> units) {
        indexCurrentPopulation(persons);
        // Death years are available before trusts are processed, including
        // parents who have already disappeared from the annual population.
        for (Person person : persons)
            if (isDeath(person))
                deathYears.putIfAbsent(person.getId(), year);
        persons.stream().filter(p -> SampleExit.Orphan.equals(p.getSampleExit()))
                .sorted(Comparator.comparingLong(Person::getId)).forEach(person -> {
                    if (recordedOrphans.add(person.getId())) {
                        OrphanReference reference = findOrphanReference(person);
                        if (reference != null)
                            orphanReferences.put(person.getId(), reference);
                        orphanExits.add(new OrphanExit(year, person.getId(),
                                person.getBenefitUnit().getId(), person.getDemAge(),
                                reference == null ? null : reference.person().getId(),
                                reference == null ? "unavailable" : reference.reason()));
                    }
                });

        List<Person> exits = persons.stream()
                .filter(person -> isDeath(person) || isEmigrationExit(person))
                .filter(person -> !settledDeaths.contains(person.getId()))
                .sorted(Comparator.comparingLong(Person::getId)).toList();
        Map<BenefitUnit, UnitSnapshot> snapshots = new LinkedHashMap<>();
        for (Person exited : exits)
            snapshots.computeIfAbsent(exited.getBenefitUnit(), this::snapshot);
        // Snapshot estates before crediting any separate trust or pension
        // transfer into a surviving adult's unit.
        settleExitedTrusts(year);

        // Both mortality and residual population-alignment deaths settle under
        // the same rule. Only emigration removes estate stock from the sample.
        for (Person exited : exits) {
            BenefitUnit unit = exited.getBenefitUnit();
            Person partner = livingPartner(exited);
            if (isDeath(exited)) {
                deathEvents.add(new DeathEvent(year, exited.getId(), unit.getId(),
                        exited.getSampleExit()));
                settlePension(year, exited, partner);
            } else {
                double pensionStock = exited.getWealthPensValue(false);
                alignmentFlow(year, "emigration_alignment_person_exit", unit, exited,
                        0.0, 0.0, pensionStock, 0.0);
                unit.removeDeceasedPensionStock(exited);
            }
            if (partner != null) {
                double retained = snapshots.get(unit).net() /
                        snapshots.get(unit).adults();
                partner.addInheritanceToPersonalFinancialBalance(retained);
                if (isDeath(exited)) {
                    mergeBranches(partner.getId(), allBranches(exited));
                    record(year, exited, partner, "nonpension_wealth", "retained_in_unit",
                            Math.max(0.0, retained), "surviving_partner");
                } else {
                    // The pooled balance is already in the surviving unit;
                    // this only keeps the survivor's personal balance aligned.
                    alignmentFlow(year, "partner_retained_bookkeeping", unit, exited,
                            retained, 0.0, 0.0, 0.0);
                }
            }
            settledDeaths.add(exited.getId());
        }

        for (Map.Entry<BenefitUnit, UnitSnapshot> entry : snapshots.entrySet()) {
            BenefitUnit unit = entry.getKey();
            if (hasLivingResponsibleAdult(unit))
                continue;
            List<Person> unitExits = exits.stream().filter(p -> p.getBenefitUnit() == unit).toList();
            if (unitExits.isEmpty())
                continue;
            if (unitExits.stream().anyMatch(this::isEmigrationExit)) {
                UnitSnapshot stock = entry.getValue();
                alignmentFlow(year, "unit_emigration", unit, null,
                        stock.financial(), stock.housing(),
                        unit.getWealthPensValue(false), 0.0);
                unit.clearEstateWealth();
                continue;
            }
            Person estateAdult = estateReferenceAdult(unit, unitExits);
            Map<String, ChildBranch> branches = new LinkedHashMap<>();
            for (Person deceased : unitExits)
                branches.putAll(allBranches(deceased));
            double raw = entry.getValue().net();
            double estate = Math.max(0.0, raw);
            if (raw < -EPS)
                record(year, estateAdult, null, "nonpension_wealth", "write_off", -raw,
                        "insolvent_unit");
            if (estate > EPS)
                distribute(year, estateAdult, estate, branches, "nonpension_estate",
                        unitExits.stream().filter(this::isDeath)
                                .filter(p -> p == unit.getMale() || p == unit.getFemale()).toList());
            unit.clearEstateWealth();
        }

        for (Person exited : exits)
            carriedBranches.remove(exited.getId());
        for (BenefitUnit unit : units)
            unit.updateTotalWealth();
        captureBalances(year, units);
    }

    private void registerChild(Long parentId, Person child) {
        if (parentId != null)
            childrenByParent.computeIfAbsent(parentId, ignored -> new ArrayList<>()).add(child);
    }

    private UnitSnapshot snapshot(BenefitUnit unit) {
        var wealth = unit.getWealthNonPension();
        int adults = (unit.getMale() == null ? 0 : 1) + (unit.getFemale() == null ? 0 : 1);
        if (adults < 1)
            throw new IllegalStateException("Estate has no responsible adult in unit " + unit.getId());
        return new UnitSnapshot(wealth.getWealthFinancial().getValue(),
                wealth.getWealthHousing().getWealthNetHousing(), adults);
    }

    private Person livingPartner(Person deceased) {
        Person partner = deceased.getPartner();
        return partner != null && isLiving(partner) ? partner : null;
    }

    private OrphanReference findOrphanReference(Person orphan) {
        List<Person> parents = new ArrayList<>();
        for (Long id : Arrays.asList(orphan.getIdMother(), orphan.getIdFather())) {
            Person parent = knownPeople.get(id);
            if (parent != null && deathYears.containsKey(id))
                parents.add(parent);
        }
        Comparator<Person> lastDeath = Comparator
                .comparingInt((Person p) -> deathYears.get(p.getId()))
                .thenComparingLong(Person::getId);
        Person parent = parents.stream().max(lastDeath).orElse(null);
        if (parent != null)
            return new OrphanReference(parent, "last_deceased_parent");
        // Incomplete source links require an explicit, auditable fallback.
        return orphan.getBenefitUnit().getMembers().stream()
                .filter(p -> p.getDemAge() >= Parameters.AGE_TO_BECOME_RESPONSIBLE)
                .filter(p -> deathYears.containsKey(p.getId()))
                .max(lastDeath).map(p -> new OrphanReference(p, "deceased_unit_adult_fallback"))
                .orElse(null);
    }

    private boolean hasLivingResponsibleAdult(BenefitUnit unit) {
        return unit.getMale() != null && isLiving(unit.getMale()) ||
                unit.getFemale() != null && isLiving(unit.getFemale());
    }

    private Person estateReferenceAdult(BenefitUnit unit, List<Person> unitExits) {
        // Annual settlement has no within-year death order. Preserve the
        // deterministic highest-ID convention among deceased responsible
        // adults only; a dependent must never supply estate matching traits.
        return unitExits.stream().filter(this::isDeath)
                .filter(p -> p == unit.getMale() || p == unit.getFemale())
                .max(Comparator.comparingLong(Person::getId))
                .orElseThrow(() -> new IllegalStateException(
                        "Estate has no deceased responsible adult in unit " + unit.getId()));
    }

    private void settlePension(int year, Person deceased, Person partner) {
        PrivatePension pension = deceased.getPrivatePension();
        double pot = !pension.isPensioner() &&
                !Les_c4.Retired.equals(deceased.getLabC4())
                ? Math.max(0.0, pension.getWealth()) : 0.0;
        deceased.getBenefitUnit().removeDeceasedPensionStock(deceased);
        if (pot <= EPS)
            return;
        if (partner != null) {
            creditCash(year, deceased, partner, pot, "unaccessed_pension_partner");
            return;
        }
        Person exitingPartner = deceased.getPartner();
        if (exitingPartner != null && isEmigrationExit(exitingPartner)) {
            // Mortality precedes alignment in the annual schedule. The partner
            // was alive at death but leaves the sample before settlement.
            alignmentFlow(year, "partner_pension_exit", deceased.getBenefitUnit(),
                    exitingPartner, 0.0, 0.0, pot, 0.0);
            return;
        }
        distribute(year, deceased, pot, ownBranches(deceased), "unaccessed_pension");
    }

    private Map<String, ChildBranch> allBranches(Person person) {
        Map<String, ChildBranch> result = ownBranches(person);
        result.putAll(carriedBranches.getOrDefault(person.getId(), Map.of()));
        return deduplicateSourceBranches(result);
    }

    /** Remove an unresolved source branch already represented by a linked child.
     * Keep distinct simulated people, including clones sharing a source ID.
     */
    private Map<String, ChildBranch> deduplicateSourceBranches(Map<String, ChildBranch> branches) {
        Map<String, ChildBranch> result = new LinkedHashMap<>(branches);
        Set<Long> linkedSources = new HashSet<>();
        for (ChildBranch branch : result.values()) {
            if (branch.childId() == null)
                continue;
            Person child = knownPeople.get(branch.childId());
            if (child != null && child.getInheritanceSourceId() != null)
                linkedSources.add(child.getInheritanceSourceId());
        }
        for (Long sourceId : linkedSources)
            result.remove("source:" + sourceId);
        return result;
    }

    private Map<String, ChildBranch> ownBranches(Person person) {
        Map<String, ChildBranch> result = new LinkedHashMap<>();
        Set<Long> representedSources = new HashSet<>();
        for (Person child : childrenByParent.getOrDefault(person.getId(), List.of()).stream()
                .sorted(Comparator.comparingLong(Person::getId)).toList()) {
            Long sourceId = child.getInheritanceSourceId();
            if (sourceId != null)
                representedSources.add(sourceId);
            ChildBranch branch = new ChildBranch("person:" + child.getId(), child.getId(),
                    "identified_child");
            result.put(branch.key(), branch);
        }
        Long sourceParent = person.getInheritanceSourceId();
        if (sourceParent != null)
            for (Long sourceChild : sourceChildren.getOrDefault(sourceParent, Set.of()))
                if (!representedSources.contains(sourceChild)) {
                    ChildBranch branch = new ChildBranch("source:" + sourceChild, null,
                            "unresolved_child");
                    result.put(branch.key(), branch);
                }
        return result;
    }

    private void mergeBranches(long recipientId, Map<String, ChildBranch> branches) {
        Map<String, ChildBranch> carried = carriedBranches.computeIfAbsent(recipientId,
                ignored -> new LinkedHashMap<>());
        carried.putAll(branches);
    }

    private void distribute(int year, Person deceased, double amount,
                            Map<String, ChildBranch> branches, String purpose) {
        distribute(year, deceased, amount, branches, purpose, List.of(deceased));
    }

    private void distribute(int year, Person deceased, double amount,
                            Map<String, ChildBranch> branches, String purpose, List<Person> estateOwners) {
        // Parents' separate lists may represent their shared child by different
        // keys. Deduplicate again after pooling, before computing equal shares.
        branches = deduplicateSourceBranches(branches);
        List<List<Heir>> eligible = new ArrayList<>();
        for (ChildBranch branch : branches.values()) {
            List<Heir> heirs = resolveBranch(branch, new HashSet<>());
            if (!heirs.isEmpty())
                eligible.add(heirs);
        }
        Set<Long> reservedRecipients = new HashSet<>();
        for (List<Heir> branch : eligible)
            for (Heir heir : branch)
                if (heir.person() != null)
                    reservedRecipients.add(heir.person().getId());
        if (eligible.isEmpty()) {
            // Parent links cross benefit-unit and household boundaries. For a
            // pooled simultaneous-death estate, include both adults' parents.
            Map<Long, Person> parents = new TreeMap<>();
            for (Person owner : estateOwners)
                for (Long id : Arrays.asList(owner.getIdMother(), owner.getIdFather())) {
                    Person parent = knownPeople.get(id);
                    if (isLiving(parent) && parent != owner)
                        parents.put(parent.getId(), parent);
                }
            if (!parents.isEmpty()) {
                for (Person parent : parents.values())
                    creditCash(year, deceased, parent, amount / parents.size(), purpose + "_parent");
                return;
            }
            distributeSynthetic(year, deceased, amount, purpose,
                    syntheticRecipientCount, reservedRecipients);
            return;
        }
        double perBranch = amount / eligible.size();
        for (List<Heir> branch : eligible)
            for (Heir heir : branch) {
                double value = perBranch * heir.weight();
                if (heir.person() == null)
                    distributeSynthetic(year, deceased, value,
                            purpose + "_" + heir.origin(), 1, reservedRecipients, heir.orphanId());
                else
                    creditCash(year, deceased, heir.person(), value, purpose + "_child");
            }
    }

    private List<Heir> resolveBranch(ChildBranch branch, Set<Long> visited) {
        if (branch.childId() == null)
            return List.of(new Heir(null, 1.0,
                    branch.origin() + "_" + branch.key().replace(':', '_')));
        Person child = knownPeople.get(branch.childId());
        if (child == null || !visited.add(child.getId()))
            return List.of(new Heir(null, 1.0, "unresolved_child"));
        if (isLiving(child))
            return List.of(new Heir(child, 1.0, branch.origin()));
        if (SampleExit.Orphan.equals(child.getSampleExit()))
            return List.of(new Heir(null, 1.0, "orphan_child", child.getId()));
        if (!isDeath(child))
            return List.of(new Heir(null, 1.0, "out_of_sample_child"));
        // Representation follows the deceased child's own descendants, not
        // stepchildren carried from an unrelated later partner.
        Map<String, ChildBranch> descendants = ownBranches(child);
        if (descendants.isEmpty())
            return List.of();
        List<List<Heir>> livingBranches = new ArrayList<>();
        for (ChildBranch descendant : descendants.values()) {
            List<Heir> resolved = resolveBranch(descendant, new HashSet<>(visited));
            if (!resolved.isEmpty())
                livingBranches.add(resolved);
        }
        List<Heir> heirs = new ArrayList<>();
        for (List<Heir> descendant : livingBranches)
            for (Heir heir : descendant)
                heirs.add(new Heir(heir.person(),
                        heir.weight() / livingBranches.size(), heir.origin(), heir.orphanId()));
        return heirs;
    }

    private void distributeSynthetic(int year, Person deceased, double amount,
                                     String purpose, int count, Set<Long> used) {
        distributeSynthetic(year, deceased, amount, purpose, count, used, null);
    }

    private void distributeSynthetic(int year, Person deceased, double amount,
                                     String purpose, int count, Set<Long> used, Long orphanId) {
        OrphanReference orphanReference = orphanId == null ? null : orphanReference(orphanId, year);
        Person reference = orphanReference == null ? deceased : orphanReference.person();
        boolean priorTrust = purpose.equals("existing_minor_trust");
        // The same orphan uses the same recipient for prior trusts and new
        // shares. Childless pension and pooled-estate matching also share a seed.
        String matchingPurpose = orphanId != null ? "orphan:" + orphanId :
                purpose.equals("unaccessed_pension") ? "nonpension_estate" : purpose;
        SplittableRandom rng = new SplittableRandom(seed ^ (reference.getId() * 0x9E3779B97F4A7C15L)
                ^ ((long) year << 32) ^ matchingPurpose.hashCode());
        List<Match> matches = new ArrayList<>();
        for (int slot = 1; slot <= count; slot++) {
            Match match = orphanId == null ? null : orphanRecipients.get(orphanId);
            if (match != null && isEligibleMatchedAdult(match.person())) {
                match = match.withTier(match.tier() + "_reused_orphan_match");
            } else {
                match = matchRecipient(reference, used, rng);
                if (match == null) {
                    // A tiny sample may contain fewer distinct adults than
                    // requested slots. Reuse an adult rather than lose a share.
                    match = matchRecipient(reference, Set.of(), rng);
                    if (match == null)
                        throw new IllegalStateException("No eligible in-sample adult recipient for " +
                                (orphanId == null ? "donor " + deceased.getId() : "orphan " + orphanId) +
                                " in year " + year + "; inheritance cannot be discarded");
                    match = match.withTier(match.tier() + "_reused");
                }
                if (orphanId != null)
                    orphanRecipients.put(orphanId, match);
            }
            matches.add(match);
            used.add(match.person().getId());
            double share = amount / count;
            creditCash(year, priorTrust ? null : deceased, match.person(), share,
                    purpose + "_synthetic_" + match.tier());
            if (orphanId != null)
                orphanReassignments.add(new OrphanReassignment(year, orphanId,
                        priorTrust ? null : deceased.getId(), match.person().getId(), reference.getId(),
                        orphanReference.reason(), purpose, match.tier(), share));
        }
        int actual = (int) matches.stream().map(m -> m.person().getId()).distinct().count();
        for (int slot = 1; slot <= count; slot++) {
            Match match = matches.get(slot - 1);
            syntheticMatches.add(new SyntheticMatch(year, priorTrust ? -1L : deceased.getId(),
                    match.person().getId(), purpose, count, actual, slot,
                    reference.getDemAge() - match.targetAge(), match.tier(), amount / count,
                    reference.getId(), match.targetAge(), reference.getDemAge() - match.person().getDemAge(),
                    match.ageRule(), match.educationDistance(), match.sameRegion()));
        }
    }

    private OrphanReference orphanReference(long orphanId, int year) {
        OrphanReference reference = orphanReferences.get(orphanId);
        if (reference == null)
            throw new IllegalStateException("No deceased parent or responsible-adult reference for orphan " +
                    orphanId + " in year " + year);
        return reference;
    }

    private boolean isEligibleMatchedAdult(Person person) {
        return isLiving(person) && person.getDemAge() >= Parameters.AGE_TO_BECOME_RESPONSIBLE &&
                person.getBenefitUnit() != null && hasLivingResponsibleAdult(person.getBenefitUnit());
    }

    private Match matchRecipient(Person reference, Set<Long> used, SplittableRandom rng) {
        int adultAge = Parameters.AGE_TO_BECOME_RESPONSIBLE;
        boolean young = reference.getDemAge() - 30 < adultAge;
        // Use the same young-adult band whenever the intergenerational band
        // would include minors: ages 18-28 for UK donors younger than 48.
        int lower = young ? adultAge : reference.getDemAge() - 30;
        int upper = young ? adultAge + 10 : reference.getDemAge() - 20;
        int targetAge = lower + rng.nextInt(upper - lower + 1);
        Comparator<Person> rank = Comparator
                .comparingInt((Person p) -> Math.abs(p.getDemAge() - targetAge))
                .thenComparingInt(p -> educationDistance(reference, p))
                .thenComparingInt(p -> Objects.equals(reference.getRegion(), p.getRegion()) ? 0 : 1);
        List<Person> pool = knownPeople.values().stream().filter(this::isEligibleMatchedAdult)
                .filter(p -> !used.contains(p.getId()))
                .sorted(Comparator.comparingLong(Person::getId)).toList();
        Person best = pool.stream().min(rank).orElse(null);
        if (best == null)
            return null;
        List<Person> tied = pool.stream().filter(p -> rank.compare(p, best) == 0).toList();
        Person recipient = tied.get(rng.nextInt(tied.size()));
        int education = educationDistance(reference, recipient);
        boolean region = Objects.equals(reference.getRegion(), recipient.getRegion());
        String tier = (recipient.getDemAge() == targetAge ? "exact_age" : "age_relaxed") +
                (education == Integer.MAX_VALUE ? "_education_missing" : "_education_distance_" + education) +
                (region ? "_same_region" : "_other_region");
        return new Match(recipient, targetAge, young ? "young_adult_band" : "donor_minus_20_to_30",
                education == Integer.MAX_VALUE ? -1 : education, region, tier);
    }

    private int educationDistance(Person reference, Person candidate) {
        Education a = reference.getEduHighestC4();
        Education b = candidate.getEduHighestC4();
        // Missing education must not outrank an observed qualification match.
        return a == null || b == null ? Integer.MAX_VALUE : Math.abs(a.getRank() - b.getRank());
    }

    private void creditCash(int year, Person deceased, Person recipient,
                            double value, String reason) {
        if (value <= EPS)
            return;
        if (!isLiving(recipient)) {
            record(year, deceased, recipient, "cash", "out_of_sample", value, reason);
        } else if (recipient.getDemAge() < Parameters.AGE_TO_BECOME_RESPONSIBLE) {
            minorTrusts.merge(recipient.getId(), value, Double::sum);
            trustUnits.put(recipient.getId(), recipient.getBenefitUnit());
            record(year, deceased, recipient, "minor_trust", "deferred", value, reason);
        } else {
            recipient.getBenefitUnit().addInheritanceCash(value);
            recipient.addInheritanceToPersonalFinancialBalance(value);
            record(year, deceased, recipient, "cash", "immediate", value, reason);
        }
    }

    private void releaseAdultTrusts(int year) {
        for (Long id : new ArrayList<>(minorTrusts.keySet())) {
            Person holder = knownPeople.get(id);
            if (holder != null && isLiving(holder) &&
                    holder.getDemAge() >= Parameters.AGE_TO_BECOME_RESPONSIBLE) {
                double value = minorTrusts.remove(id);
                trustUnits.remove(id);
                holder.getBenefitUnit().addMatureInheritanceCash(value);
                holder.addMatureInheritanceToPersonalFinancialBalance(value);
                record(year, null, holder, "minor_trust", "released", value,
                        "age_of_majority");
            }
        }
    }

    private void settleExitedTrusts(int year) {
        for (Long id : minorTrusts.keySet().stream().sorted().toList()) {
            Person holder = knownPeople.get(id);
            if (holder == null || isLiving(holder))
                continue;
            double value = minorTrusts.get(id);
            if (SampleExit.Orphan.equals(holder.getSampleExit())) {
                Person reference = orphanReference(id, year).person();
                Set<Long> reserved = new HashSet<>();
                for (ChildBranch branch : allBranches(reference).values())
                    for (Heir heir : resolveBranch(branch, new HashSet<>()))
                        if (heir.person() != null)
                            reserved.add(heir.person().getId());
                distributeSynthetic(year, reference, value, "existing_minor_trust", 1, reserved, id);
            } else if (isDeath(holder)) {
                // A child's separately reserved inheritance is not the parents'
                // pooled estate. Return it through the normal heir hierarchy.
                Person partner = livingPartner(holder);
                if (partner != null)
                    creditCash(year, holder, partner, value, "deceased_minor_trust_partner");
                else
                    distribute(year, holder, value, ownBranches(holder), "deceased_minor_trust");
            } else {
                record(year, null, holder, "minor_trust", "out_of_sample", value, "holder_left_population");
                if (isEmigrationExit(holder))
                    alignmentFlow(year, "minor_trust_alignment_exit",
                            holder.getBenefitUnit(), holder, 0.0, 0.0, 0.0, value);
            }
            // Remove only after successful settlement, so no failed match
            // silently destroys the original balance.
            minorTrusts.remove(id);
            trustUnits.remove(id);
        }
    }

    private void captureBalances(int year, Set<BenefitUnit> units) {
        for (BenefitUnit unit : units) {
            double trusts = 0.0;
            for (Person person : unit.getMembers())
                if (isLiving(person))
                    trusts += minorTrusts.getOrDefault(person.getId(), 0.0);
            balanceSnapshots.add(new BalanceSnapshot(year, unit.getId(), 0.0, 0.0,
                    trusts, unit.getWealthTotValue(false)));
        }
    }

    private boolean isLiving(Person person) {
        return person != null && currentPopulationIds.contains(person.getId()) &&
                SampleExit.NotYet.equals(person.getSampleExit());
    }

    private boolean isDeath(Person person) {
        return SampleExit.Death.equals(person.getSampleExit()) ||
                SampleExit.DeathAlignment.equals(person.getSampleExit());
    }

    private boolean isEmigrationExit(Person person) {
        return SampleExit.EmigrationAlignment.equals(person.getSampleExit());
    }

    private double trustStock(BenefitUnit unit) {
        double total = 0.0;
        for (Person person : unit.getMembers())
            total += minorTrusts.getOrDefault(person.getId(), 0.0);
        return total;
    }

    private void alignmentFlow(int year, String event, BenefitUnit unit, Person person,
                               double financial, double housing, double pension,
                               double minorTrust) {
        if (!Double.isFinite(financial) || !Double.isFinite(housing) ||
                !Double.isFinite(pension) || !Double.isFinite(minorTrust))
            throw new IllegalArgumentException("Non-finite population-alignment wealth flow");
        alignmentFlows.add(new AlignmentFlow(year, event, unit.getId(),
                person == null ? null : person.getId(), financial, housing,
                pension, minorTrust));
    }

    private void record(int year, Person deceased, Person recipient, String asset,
                        String timing, double value, String reason) {
        if (!Double.isFinite(value) || value < -EPS)
            throw new IllegalArgumentException("Invalid inheritance ledger value");
        transfers.add(new Transfer(year, deceased == null ? -1L : deceased.getId(),
                recipient == null ? null : recipient.getId(), asset, timing, value, reason,
                deceased == null ? "not_applicable" : deceased.getSampleExit().name()));
    }

    public void writeLedger(Path outputDirectory) throws IOException {
        Files.createDirectories(outputDirectory);
        List<String> lines = new ArrayList<>();
        lines.add("year,deceased_id,recipient_id,asset,timing,value_2015_gbp,reason,death_cause");
        for (Transfer t : transfers)
            lines.add(t.year() + "," + t.deceasedId() + "," +
                    (t.recipientId() == null ? "" : t.recipientId()) + "," +
                    t.asset() + "," + t.timing() + "," + t.value() + "," + t.reason() +
                    "," + t.deathCause());
        Files.write(outputDirectory.resolve("inheritance_transfers.csv"), lines);

        lines.clear();
        lines.add("year,deceased_id,benefit_unit_id,death_cause");
        for (DeathEvent death : deathEvents)
            lines.add(death.year() + "," + death.deceasedId() + "," +
                    death.benefitUnitId() + "," + death.deathCause());
        Files.write(outputDirectory.resolve("inheritance_death_events.csv"), lines);

        lines.clear();
        lines.add("year,person_id,benefit_unit_id,age,exit_reason,reference_parent_id,reference_reason");
        for (OrphanExit orphan : orphanExits)
            lines.add(orphan.year() + "," + orphan.personId() + "," +
                    orphan.benefitUnitId() + "," + orphan.age() + ",Orphan," +
                    (orphan.referenceParentId() == null ? "" : orphan.referenceParentId()) + "," +
                    orphan.referenceReason());
        Files.write(outputDirectory.resolve("inheritance_orphan_exits.csv"), lines);

        lines.clear();
        lines.add("year,orphan_id,deceased_id,recipient_id,purpose,match_tier,value_2015_gbp," +
                "reference_parent_id,reference_reason");
        for (OrphanReassignment reassignment : orphanReassignments)
            lines.add(reassignment.year() + "," + reassignment.orphanId() + "," +
                    (reassignment.deceasedId() == null ? "" : reassignment.deceasedId()) + "," +
                    reassignment.recipientId() + "," + reassignment.purpose() + "," +
                    reassignment.tier() + "," + reassignment.value() + "," +
                    reassignment.referenceParentId() + "," + reassignment.referenceReason());
        Files.write(outputDirectory.resolve("inheritance_orphan_reassignments.csv"), lines);

        lines.clear();
        lines.add("year,benefit_unit_id,claim_assets_2015_gbp,claim_liabilities_2015_gbp," +
                "minor_trust_assets_2015_gbp,total_net_wealth_2015_gbp");
        for (BalanceSnapshot b : balanceSnapshots)
            lines.add(b.year() + "," + b.benefitUnitId() + ",0,0," +
                    b.minorTrustAssets() + "," + b.totalNetWealth());
        Files.write(outputDirectory.resolve("inheritance_balances.csv"), lines);

        lines.clear();
        lines.add("year,valid_parent_references,resolved_cross_household_references," +
                "unresolved_parent_references,valid_immigrant_parent_references," +
                "unresolved_immigrant_parent_references");
        if (familyLinkAudit != null)
            lines.add(familyLinkAudit.year() + "," + familyLinkAudit.validReferences() + "," +
                    familyLinkAudit.resolvedAcrossHouseholds() + "," +
                    familyLinkAudit.unresolvedReferences() + "," +
                    familyLinkAudit.validImmigrantReferences() + "," +
                    familyLinkAudit.unresolvedImmigrantReferences());
        Files.write(outputDirectory.resolve("inheritance_family_links.csv"), lines);

        lines.clear();
        lines.add("year,deceased_id,recipient_id,purpose,intended_n,actual_n,slot,age_gap,match_tier," +
                "value_2015_gbp,reference_id,target_age,actual_age_gap,age_rule,education_distance,same_region");
        for (SyntheticMatch m : syntheticMatches)
            lines.add(m.year() + "," + m.deceasedId() + "," +
                    (m.recipientId() == null ? "" : m.recipientId()) + "," + m.purpose() +
                    "," + m.intendedCount() + "," + m.actualCount() + "," +
                    m.slot() + "," + m.ageGap() +
                    "," + m.tier() + "," + m.value() + "," + m.referenceId() + "," + m.targetAge() +
                    "," + m.actualAgeGap() + "," + m.ageRule() + "," + m.educationDistance() + "," + m.sameRegion());
        Files.write(outputDirectory.resolve("inheritance_synthetic_matches.csv"), lines);
        lines.clear();
        lines.add("year,event,benefit_unit_id,person_id,net_financial_2015_gbp," +
                "net_housing_2015_gbp,pension_stock_2015_gbp,minor_trust_2015_gbp");
        for (AlignmentFlow flow : alignmentFlows)
            lines.add(flow.year() + "," + flow.event() + "," + flow.benefitUnitId() + "," +
                    (flow.personId() == null ? "" : flow.personId()) + "," +
                    flow.financial() + "," + flow.housing() + "," + flow.pension() +
                    "," + flow.minorTrust());
        Files.write(outputDirectory.resolve("alignment_wealth_flows.csv"), lines);
        Files.writeString(outputDirectory.resolve("inheritance_method.txt"),
                "pooled_last_adult; pension_inheritance=remaining_modelled_pot_before_retirement" +
                        "; synthetic_recipient_count=" + syntheticRecipientCount +
                        "; natural_deaths_trigger_bequests=true" +
                        "; alignment_deaths_trigger_bequests=true" +
                        "; emigration_exits_trigger_bequests=false" +
                        "; pooled_estate_reference=highest_id_deceased_responsible_adult" +
                        "; orphan_exit=before_inheritance_settlement" +
                        "; heir_order=partner_children_descendants_parents_matched_adults" +
                        "; pooled_parent_fallback=unique_surviving_parents_of_current_deceased_adults_equal_shares" +
                        "; matching_priority=age_education_region_seeded_tie" +
                        "; matched_recipients=active_adults_only" +
                        "; matching_age=minus_20_to_30_unless_range_includes_minors_then_adult_age_to_plus_10" +
                        "; orphan_reference=latest_parent_death_year_then_highest_person_id" +
                        "; orphan_existing_trust=reuse_parent_referenced_adult_match" +
                        "; deceased_minor_trust=normal_heir_hierarchy" +
                        "; unmatched=fail_run; insufficient_distinct_adults=reuse" +
                        "; matching_seed=" + seed + System.lineSeparator());
    }
}
