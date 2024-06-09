package simpaths.model.enums;

/*
This enum defines possible states for transitions in the Covid-19 module from employment. It is used when evaluating destinations of transitions in the reduced-form labour supply module.

*/

public enum Les_transitions_S1 implements IntegerValuedEnum {
	NotEmployed(0),
	Employee(1),
	SelfEmployed(2); //Definition of this could be omitted - omitted category would act as the baseline in the multiprobit regression. But specified for clarity.

	private final int value;
	Les_transitions_S1(int val) { value = val; }

	@Override
	public int getValue() {return value;}
	public Les_c7_covid convertToLes_c7_covid() {
		Les_c7_covid outcome = null;
		if (this.equals(Les_transitions_S1.NotEmployed)) {
			outcome = Les_c7_covid.NotEmployed;
		} else if (this.equals(Les_transitions_S1.Employee)) {
			outcome = Les_c7_covid.Employee;
		} else if (this.equals(Les_transitions_S1.SelfEmployed)) {
			outcome = Les_c7_covid.SelfEmployed;
		}
		return outcome;
	}

}