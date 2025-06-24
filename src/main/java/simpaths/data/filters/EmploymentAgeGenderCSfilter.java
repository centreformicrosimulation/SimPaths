package simpaths.data.filters;

import microsim.statistics.ICollectionFilter;
import simpaths.model.Person;
import simpaths.model.enums.Gender;
import simpaths.model.enums.Les_c4;

public class EmploymentAgeGenderCSfilter implements ICollectionFilter {

    private Les_c4 employment;
    private final int ageFrom;
    private final int ageTo;
    private final Gender gender;

    public EmploymentAgeGenderCSfilter(Les_c4 employment, int ageFrom, int ageTo, Gender gender) {
        super();
        this.employment = employment;
        this.ageFrom = ageFrom;
        this.ageTo = ageTo;
        this.gender = gender;
    }

    public EmploymentAgeGenderCSfilter(Les_c4 employment, int ageFrom, int ageTo) {
        super();
        this.employment = employment;
        this.ageFrom = ageFrom;
        this.ageTo = ageTo;
        this.gender = null;
    }

    public boolean isFiltered(Object object) {

        if (object instanceof Person){
            Person person = (Person) object;
            if (this.gender == null) {
                return (person.getLes_c4().equals(employment) && (person.getDag() >= ageFrom) && (person.getDag() <= ageTo));
            } else {
                return (person.getLes_c4().equals(employment) && (person.getDag() >= ageFrom) && (person.getDag() <= ageTo) && person.getDgn().equals(gender));
            }
        }
        else throw new IllegalArgumentException("Argument passed to EmploymentCSfilter must be of object type Person");
    }

}

