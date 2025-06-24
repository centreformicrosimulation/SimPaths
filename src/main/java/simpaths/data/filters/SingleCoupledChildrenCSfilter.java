package simpaths.data.filters;

import microsim.statistics.ICollectionFilter;
import simpaths.model.Person;
import simpaths.model.enums.Gender;
import simpaths.model.enums.Indicator;

public class SingleCoupledChildrenCSfilter implements ICollectionFilter {

    private boolean coupled;
    private boolean children;
    private Gender gender;
    private int ageFrom = 18;
    private int ageTo = 64;

    public SingleCoupledChildrenCSfilter(boolean coupled, boolean children, Gender gender) {
        super();
        this.coupled = coupled;
        this.children = children;
        this.gender = gender;
    }

    public SingleCoupledChildrenCSfilter(boolean coupled, boolean children) {
        super();
        this.coupled = coupled;
        this.children = children;
        this.gender = null;
    }

    public boolean isFiltered(Object object) {

        if (object instanceof Person){
            Person person = (Person) object;
            if (this.gender == null) {
                return (
                        (person.getDag() >= ageFrom) && (person.getDag() <= ageTo) &&
                        (person.getPartner() != null) == this.coupled &&
                                person.getBenefitUnit().getIndicatorChildren(0,12).equals(Indicator.True) == this.children
                );
            }
            return (
                    (person.getDag() >= ageFrom) && (person.getDag() <= ageTo) &&
                    (person.getPartner() != null) == this.coupled &&
                            person.getBenefitUnit().getIndicatorChildren(0,12).equals(Indicator.True) == this.children &&
                            person.getDgn().equals(this.gender)
            );
        }
        else throw new IllegalArgumentException("Argument passed to SingleCoupledChildrenCSfilter must be of object type Person");
    }

}
