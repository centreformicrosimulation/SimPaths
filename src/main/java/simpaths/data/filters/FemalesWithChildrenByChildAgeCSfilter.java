package simpaths.data.filters;

import simpaths.model.Person;
import simpaths.model.enums.Gender;
import microsim.statistics.ICollectionFilter;

import java.util.Arrays;
import java.util.Set;

public class FemalesWithChildrenByChildAgeCSfilter implements ICollectionFilter{

	private int ageFrom;
	private int ageTo;

	public FemalesWithChildrenByChildAgeCSfilter(int ageFrom, int ageTo) {
		super();
		this.ageFrom = ageFrom;
		this.ageTo = ageTo;
	}



	public boolean isFiltered(Object object) {
		Person person = (Person) object;

//		int minAge = 99;
//		int maxAge = -1;

		Set<Person> childrenInBU = person.getBenefitUnit().getChildren();
		int[] ages = new int[childrenInBU.size()];
		if (childrenInBU != null) {
			int arrayPosition = 0;
			for (Person child : childrenInBU) {
				ages[arrayPosition] = child.getDag();
			}
		}

		int minAge = -1;
		int maxAge = 99;

		if (ages.length > 0) {
			minAge = Arrays.stream(ages).min().getAsInt();
			maxAge = Arrays.stream(ages).max().getAsInt();
		}


/*
		Set<Person> childrenInBU = person.getBenefitUnit().getChildren();
		if (childrenInBU != null) {
			for (Person child : childrenInBU) {
				int age = child.getDag();
				if (age < minAge) {
					minAge = age;
				}
				if (age > maxAge) {
					maxAge = age;
				}
			}
		} else {
			minAge = -1;
			maxAge = 99;
		}
*/

		return ( person.getDgn().equals(Gender.Female) &&
				person.getDag() >= 20 && person.getDag() <= 65 &&
				(minAge >= ageFrom && maxAge <= ageTo)
		);
	}
	
}

