package simpaths.data.filters;

import microsim.statistics.ICollectionFilter;
import simpaths.model.Person;
import simpaths.model.enums.Gender;
import simpaths.model.enums.Les_c4;

public class EmploymentHistoryFilter implements ICollectionFilter {

    private Les_c4 employmentLag1;
    private Gender gender;
    private int ageFrom;
    private int ageTo;

    public EmploymentHistoryFilter(Les_c4 employmentLag1) {
        super();
        this.employmentLag1 = employmentLag1;
        this.gender = null;
        this.ageFrom = 0;
        this.ageTo = 130;

    }
    public EmploymentHistoryFilter(Les_c4 employmentLag1, int ageFrom, int ageTo) {
        super();
        this.employmentLag1 = employmentLag1;
        this.gender = null;
        this.ageFrom = 0;
        this.ageTo = 130;

    }
    public EmploymentHistoryFilter(Les_c4 employmentLag1, int ageFrom, int ageTo, Gender gender) {
        super();
        this.employmentLag1 = employmentLag1;
        this.gender = gender;
        this.ageFrom = ageFrom;
        this.ageTo = ageTo;

    }

    public boolean isFiltered(Object object) {

        if (object instanceof Person){
            Person person = (Person) object;
            if (this.gender == null) {
                return (person.getLes_c4_lag1().equals(employmentLag1) && (person.getDag() >= ageFrom) && (person.getDag() <= ageTo));
            } else {
                return (person.getLes_c4_lag1().equals(employmentLag1) && (person.getDag() >= ageFrom) && (person.getDag() <= ageTo) && person.getDgn().equals(gender));
            }
        }
        else throw new IllegalArgumentException("Argument passed to EmploymentHistoryFilter must be of object type Person");
     }

}
