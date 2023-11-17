package simpaths.model;

import simpaths.data.IEvaluation;
import simpaths.data.Parameters;

import java.util.Set;


public class SocialCareAlignment implements IEvaluation {

    private double aggregateCareReceived;
    private double aggregateCareProvided;
    private double careProvisionAdjustment;
    boolean careProvisionAdjustmentChanged;
    private int maturePopulation;
    Set<Person> persons;

    public SocialCareAlignment(Set<Person> persons, double careProvisionAdjustment) {
        this.persons = persons;
        this.careProvisionAdjustment = careProvisionAdjustment;
        aggregateCareReceived = evalAggregateCareReceived();
    }

    @Override
    public double evaluate(double[] args) {

        if (Math.abs(args[0] - careProvisionAdjustment) > 1.0E-5) {
            adjustCareProvision(args[0]);
        }
        aggregateCareProvided = evalAggregateCareProvided();
        return (aggregateCareReceived - aggregateCareProvided) / (double) maturePopulation;
    }

    private double evalAggregateCareReceived() {

        double careReceived = 0.0;
        maturePopulation = 0;
        for (Person person : persons) {

            careReceived += person.getCareHoursFromPartnerWeekly();
            careReceived += person.getCareHoursFromDaughterWeekly();
            careReceived += person.getCareHoursFromSonWeekly();
            careReceived += person.getCareHoursFromOtherWeekly();
            if (person.getDag() >= Parameters.AGE_TO_BECOME_RESPONSIBLE)
                maturePopulation++;
        }
        return careReceived;
    }

    private double evalAggregateCareProvided() {
        double careProvided = 0.0;
        for (Person person : persons) {
            careProvided += person.getCareHoursProvidedWeekly();
        }
        return careProvided;
    }

    private void adjustCareProvision(double newCareProvisionAdjustment) {
        for (Person person : persons) {
            person.evaluateSocialCareProvision(newCareProvisionAdjustment);
        }
        careProvisionAdjustment = newCareProvisionAdjustment;
        careProvisionAdjustmentChanged = true;
    }
}
