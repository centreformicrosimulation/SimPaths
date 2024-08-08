package simpaths.model;

import jakarta.persistence.*;
import microsim.data.db.PanelEntityKey;
import simpaths.data.startingpop.Processed;
import simpaths.experiment.SimPathsCollector;
import microsim.engine.SimulationEngine;
import microsim.event.EventListener;
import microsim.statistics.IDoubleSource;
import org.apache.log4j.Logger;
import simpaths.model.enums.SampleEntry;

import java.util.LinkedHashSet;
import java.util.Set;

/*
Household class is a "wrapper" bundling multiple benefitUnits into one household.
Currently it is used to keep track of adult children who create separate benefitUnits in the simulation for technical reasons, but still live with their parents in "reality".

Persons in the database have idhh field, identifying the household. In the SimPathsModel class, we iterate over the list of household ids and create households in the simulation with the matching key. These are then matched with benefit units
from the data, on the basis of the idhh.

 */

@Entity
public class Household implements EventListener, IDoubleSource {

    @Transient private static Logger log = Logger.getLogger(Household.class);
    @Transient private final SimPathsModel model;
    @Transient private final SimPathsCollector collector;
    @Transient public static long householdIdCounter = 1; //Because this is static all instances of a household access and increment the same counter

    @EmbeddedId @Column(unique = true, nullable = false) private final PanelEntityKey key;
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "household")
    @OrderBy("key ASC")
    private Set<BenefitUnit> benefitUnits = new LinkedHashSet<>();
    @ManyToOne(fetch = FetchType.EAGER, cascade=CascadeType.REFRESH)
    @JoinColumns({
            @JoinColumn(name = "prid", referencedColumnName = "id")
    })
    private Processed processed;

    private Long idOriginalHH;


    /*
    CONSTRUCTORS
     */

    public Household() {
        model = (SimPathsModel) SimulationEngine.getInstance().getManager(SimPathsModel.class.getCanonicalName());
        collector = (SimPathsCollector) SimulationEngine.getInstance().getManager(SimPathsCollector.class.getCanonicalName());
        key  = new PanelEntityKey(householdIdCounter++);
    }

    public Household(Household originalHousehold, SampleEntry sampleEntry) {

        this();
        switch (sampleEntry) {
            case InputData -> {
                idOriginalHH = originalHousehold.key.getId();
            }
            case ProcessedInputData -> {

                householdIdCounter = originalHousehold.getId();
                key.setId(householdIdCounter);
                this.idOriginalHH = originalHousehold.getIdOriginalHH();
            }
            default -> {
                throw new RuntimeException("invalid SampleEntry value supplied to Household constructor");
            }
        }
    }

    public Household(long householdId) {
        model = (SimPathsModel) SimulationEngine.getInstance().getManager(SimPathsModel.class.getCanonicalName());
        collector = (SimPathsCollector) SimulationEngine.getInstance().getManager(SimPathsCollector.class.getCanonicalName());
        key  = new PanelEntityKey(householdId);
    }

    public Household(LinkedHashSet<BenefitUnit> benefitUnitsToAdd) {
        this(); //Refers to the basic constructor Household()
        for(BenefitUnit benefitUnit : benefitUnitsToAdd) {
            addBenefitUnit(benefitUnit);
        }
    }

    //Overloaded constructor taking a benefit unit as input and adding it to the new household
    public Household(BenefitUnit benefitUnitToAdd) {
        this(); //Refers to the basic constructor Household()
        addBenefitUnit(benefitUnitToAdd);
    }

    /*
    METHODS
     */
    public Long getIdOriginalHH() {return idOriginalHH;}

    public void resetWeights(double newWeight) {

        for (BenefitUnit benefitUnit : benefitUnits) {
            for( Person person : benefitUnit.getMembers()) {
                person.setWeight(newWeight);
            }
        }
    }

    public void addBenefitUnit(BenefitUnit benefitUnit) {

        benefitUnits.add(benefitUnit);
        if ( benefitUnit.getHousehold() != this ) {
            if ( benefitUnit.getHousehold() != null )
                benefitUnit.getHousehold().removeBenefitUnit(benefitUnit);
            benefitUnit.setHousehold(this);
        }
    }

    //Remove a benefitUnit from the household
    public void removeBenefitUnit(BenefitUnit benefitUnit) {

        if (benefitUnits.contains(benefitUnit)) {

            boolean removed = benefitUnits.remove(benefitUnit);
            if (!removed)
                throw new IllegalArgumentException("BenefitUnit " + benefitUnit.getKey().getId() + " could not be removed from household");
        }
        if (benefitUnit.getHousehold() == this)
            benefitUnit.setHousehold(null);

        //Check for benefit units remaining in the household - if none, remove the household
        if (benefitUnits.isEmpty())
            model.removeHousehold(this);
    }

    @Override
    public double getDoubleValue(Enum<?> variableID) {
        return 0;
    }

    @Override
    public void onEvent(Enum<?> anEnum) {

    }

    public double getWeight() {
        double cumulativeWeight = 0.0;
        double size = 0.0;
        for (BenefitUnit benefitUnit : benefitUnits) {
            for( Person person : benefitUnit.getMembers()) {
                cumulativeWeight += person.getWeight();
                size++;
            }
        }
        return cumulativeWeight / size;
    }

    public long getId() { //Get household ID as set in the simulation. Note that it is different than in the input data.
        return key.getId();
    }

    public Set<BenefitUnit> getBenefitUnits() { return benefitUnits; }

    public void setProcessed(Processed processed) {
        this.processed = processed;
        key.setWorkingId(processed.getId());
    }
}
