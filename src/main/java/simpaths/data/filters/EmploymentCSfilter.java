package simpaths.data.filters;

import microsim.statistics.ICollectionFilter;
import simpaths.model.Person;
import simpaths.model.enums.Les_c4;

public class EmploymentCSfilter implements ICollectionFilter {

    private Les_c4 employment;

    public EmploymentCSfilter(Les_c4 employment) {
        super();
        this.employment = employment;

    }

    public boolean isFiltered(Object object) {

        if (object instanceof Person){
            Person person = (Person) object;
            return (person.getLes_c4().equals(employment));
        }
        else throw new IllegalArgumentException("Argument passed to EmploymentCSfilter must be of object type Person");
    }

}

