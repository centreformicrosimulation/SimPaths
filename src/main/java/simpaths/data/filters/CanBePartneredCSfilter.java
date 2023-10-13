package simpaths.data.filters;

import simpaths.model.Person;
import simpaths.model.enums.Les_c4;
import microsim.statistics.ICollectionFilter;

public class CanBePartneredCSfilter implements ICollectionFilter{


	public CanBePartneredCSfilter() {
		super();
	}
	
	public boolean isFiltered(Object object) {
		if(object instanceof Person) {
			Person person = (Person) object;
			boolean inContinuousEducation = (person.getDag() <= 29 && person.getLes_c4().equals(Les_c4.Student) && person.isLeftEducation() == false);
			return (person.getPartner() == null && !inContinuousEducation);
		}
		else throw new IllegalArgumentException("Object passed to GenderEducationWorkingCSfilter must be of type Person!");
	}			
}
