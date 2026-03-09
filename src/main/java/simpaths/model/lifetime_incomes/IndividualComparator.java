package simpaths.model.lifetime_incomes;

import java.util.Comparator;

public class IndividualComparator implements Comparator<Individual> {

    int year;

    @Override
    public int compare(Individual firstIndividual, Individual secondIndividual) {
        return Double.compare(firstIndividual.getAnnualIncome(year).getValue(),
                secondIndividual.getAnnualIncome(year).getValue());
    }

    public IndividualComparator(int year) {
        this.year = year;
    }
}
