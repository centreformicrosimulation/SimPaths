package simpaths.data.filters;

import microsim.statistics.ICollectionFilter;
import simpaths.model.Person;
import simpaths.model.enums.Gender;

public class AgeGenderCSfilter implements ICollectionFilter {

    private final Gender gender;
    private final int ageFrom;
    private final int ageTo;
    public AgeGenderCSfilter(int ageFrom, int ageTo) {
        super();
        this.ageFrom = ageFrom;
        this.ageTo = ageTo;
        this.gender = null;
    }
    public AgeGenderCSfilter(int ageFrom, int ageTo, Gender gender) {
        super();
        this.ageFrom = ageFrom;
        this.ageTo = ageTo;
        this.gender = gender;
    }

    @Override
    public boolean isFiltered(Object object) {
        Person person = (Person) object;
        if (this.gender == null)
            return ( (person.getDag() >= ageFrom) && (person.getDag() <= ageTo) );
        else
            return ( (person.getDag() >= ageFrom) && (person.getDag() <= ageTo) && (person.getDgn().equals(gender)));
    }
}
