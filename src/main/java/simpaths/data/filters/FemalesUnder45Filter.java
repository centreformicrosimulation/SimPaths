package simpaths.data.filters;

import org.apache.commons.collections.Predicate;

import simpaths.model.Person;
import simpaths.model.enums.Gender;

public class FemalesUnder45Filter implements Predicate {

	@Override
	public boolean evaluate(Object object) {
		
		Person agent = (Person) object;
		
		return ( (agent.getDgn().equals(Gender.Female)) &&
//				( !agent.getActivity_status().equals(Les_c4.Student) || agent.isToLeaveSchool() ) &&	//Alignment rate for sweden includes many students who are married, so cannot filter out non-students in the alignment algorithm
				( agent.getDag() < 45 )		//Strict inequality here, to coincide with age band in the GUI chart of Cohabitiing Females
				);
				
	}


}
