package simpaths.data.filters;

import microsim.statistics.ICollectionFilter;
import simpaths.model.Person;
import simpaths.model.enums.Gender;
import simpaths.model.enums.Les_c4;

public class EmploymentHistoryFilter implements ICollectionFilter {

    private Les_c4 employmentLag1;

    public EmploymentHistoryFilter(Les_c4 employmentLag1) {
        super();
        this.employmentLag1 = employmentLag1;

    }

    public boolean isFiltered(Object object) {

        if (object instanceof Person){
            Person person = (Person) object;
            return (person.getLes_c4_lag1().equals(employmentLag1));
        }
        else throw new IllegalArgumentException("Argument passed to EmploymentHistoryFilter must be of object type Person");
     }

}
