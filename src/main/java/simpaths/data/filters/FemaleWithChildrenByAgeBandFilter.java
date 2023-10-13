//package simpaths.data.filters;
//
//import org.apache.commons.collections.Predicate;
//
//import simpaths.model.Person;
//import simpaths.model.enums.Gender;
//import simpaths.model.enums.Indicator;
//
//public class FemaleWithChildrenByAgeBandFilter implements Predicate {
//	
//	private int ageFrom;
//	private int ageTo;
//	
//	public FemaleWithChildrenByAgeBandFilter(int ageFrom, int ageTo) {
//		super();
//		this.ageFrom = ageFrom;
//		this.ageTo = ageTo;
//	}
//
//	@Override
//	public boolean evaluate(Object object) {
//		Person agent = (Person) object;
//
//		return ( (agent.getGender().equals(Gender.Female)) &&
//				( agent.getHousehold().getD_children_3under().equals(Indicator.True) || agent.getHousehold().getD_children_4_12().equals(Indicator.True) ) &&
//				( agent.getAge() >= ageFrom ) &&		//Inclusive
//				( agent.getAge() < ageTo )				//Exclusive
//				);
//	}
//
//}
