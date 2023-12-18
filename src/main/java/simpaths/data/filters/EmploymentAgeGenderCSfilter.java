package simpaths.data.filters;

import microsim.statistics.ICollectionFilter;
import simpaths.model.Person;
import simpaths.model.enums.Gender;
import simpaths.model.enums.Les_c4;

public class EmploymentAgeGenderCSfilter implements ICollectionFilter {

    private final Gender gender;
    private final int ageFrom;
    private final int ageTo;

    private final Les_c4 les_c4;

    public EmploymentAgeGenderCSfilter(int ageFrom, int ageTo, Les_c4 les_c4) {
        super();
        this.gender = null;
        this.ageFrom = ageFrom;
        this.ageTo = ageTo;
        this.les_c4 = les_c4;
    }

    public EmploymentAgeGenderCSfilter(int ageFrom, int ageTo, Les_c4 les_c4, Gender gender) {
        super();
        this.gender = gender;
        this.ageFrom = ageFrom;
        this.ageTo = ageTo;
        this.les_c4 = les_c4;
    }

    public boolean isFiltered(Object object) {
        Person person = (Person) object;
        if (this.gender == null)
            return ( (person.getDag() >= ageFrom) &&
                    (person.getDag() <= ageTo)  &&
                    person.getLes_c4().equals(Les_c4.EmployedOrSelfEmployed));
        else
            return ( (person.getDag() >= ageFrom) &&
                    (person.getDag() <= ageTo) &&
                    (person.getDgn().equals(gender)) &&
                    person.getLes_c4().equals(Les_c4.EmployedOrSelfEmployed));

    }
}