package simpaths.data.filters;

import microsim.statistics.ICollectionFilter;
import simpaths.model.Person;
import simpaths.model.enums.Gender;
import simpaths.model.enums.Labour;
import simpaths.model.enums.Les_c4;

public class LabourSupplyAgeGenderCSfilter implements ICollectionFilter {

    private final Gender gender;
    private final int ageFrom;
    private final int ageTo;

    private final Labour labour;

    public LabourSupplyAgeGenderCSfilter(int ageFrom, int ageTo, Labour labour) {
        super();
        this.gender = null;
        this.ageFrom = ageFrom;
        this.ageTo = ageTo;
        this.labour = labour;
    }

    public LabourSupplyAgeGenderCSfilter(int ageFrom, int ageTo, Labour labour, Gender gender) {
        super();
        this.gender = gender;
        this.ageFrom = ageFrom;
        this.ageTo = ageTo;
        this.labour = labour;
    }

    public boolean isFiltered(Object object) {
        Person person = (Person) object;
        if (this.gender == null)
            return ( (person.getDag() >= ageFrom) &&
                    (person.getDag() <= ageTo)  &&
                    (person.getLabourSupplyWeekly().equals(labour)));
        else
            return ( (person.getDag() >= ageFrom) &&
                    (person.getDag() <= ageTo) &&
                    (person.getDgn().equals(gender)) &&
                    (person.getLabourSupplyWeekly().equals(labour)));

    }
}