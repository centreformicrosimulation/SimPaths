package simpaths.data.filters;

import simpaths.data.Parameters;
import simpaths.model.Person;
import simpaths.model.enums.Gender;
import simpaths.model.enums.Indicator;
import simpaths.model.enums.Les_c4;
import microsim.statistics.ICollectionFilter;

public class FlexibleInLabourSupplyByAgeAndGenderFilter implements ICollectionFilter {

    private int ageFrom;
    private int ageTo;
    private Gender gender;

    public FlexibleInLabourSupplyByAgeAndGenderFilter(int ageFrom, int ageTo, Gender gender) {
        super();
        this.ageFrom = ageFrom;
        this.ageTo = ageTo;
        this.gender = gender;
    }


    @Override
    public boolean isFiltered(Object o) {
        Person person = (Person) o;

        /*
        Person "flexible in labour supply" must meet the following conditions:
        age >= 16 and <= 75
        not a student or retired
        not disabled
         */

        return (person.getDag() >= Parameters.MIN_AGE_FLEXIBLE_LABOUR_SUPPLY && person.getDag() <= Parameters.MAX_AGE_FLEXIBLE_LABOUR_SUPPLY &&
                person.getDag() >= ageFrom && person.getDag() <= ageTo &&
                person.getDgn().equals(gender) &&
                person.getLes_c4() != Les_c4.Student && person.getLes_c4() != Les_c4.Retired &&
                person.getDlltsd() != Indicator.True);
    }
}
