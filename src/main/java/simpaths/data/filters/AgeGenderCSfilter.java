package simpaths.data.filters;

import microsim.statistics.ICollectionFilter;
import simpaths.model.Person;
import simpaths.model.enums.Gender;

public class AgeGenderCSfilter implements ICollectionFilter {

    private final Gender demSex;
    private final int ageFrom;
    private final int ageTo;
    public AgeGenderCSfilter(int ageFrom, int ageTo) {
        super();
        this.ageFrom = ageFrom;
        this.ageTo = ageTo;
        this.demSex = null;
    }
    public AgeGenderCSfilter(int ageFrom, int ageTo, Gender demSex) {
        super();
        this.ageFrom = ageFrom;
        this.ageTo = ageTo;
        this.demSex = demSex;
    }

    @Override
    public boolean isFiltered(Object object) {
        Person person = (Person) object;
        if (this.demSex == null)
            return ( (person.getDemAge() >= ageFrom) && (person.getDemAge() <= ageTo) );
        else
            return ( (person.getDemAge() >= ageFrom) && (person.getDemAge() <= ageTo) && (person.getDemMaleFlag().equals(demSex)));
    }
}
