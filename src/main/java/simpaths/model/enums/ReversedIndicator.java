package simpaths.model.enums;

import microsim.statistics.regression.IntegerValuedEnum;

/*
 * The Indicator class is for use with regressions using dummy variables - boolean-like variables that take the value of either 0 or 1, depending on false or true respectively. 
 */
public enum ReversedIndicator implements IntegerValuedEnum {
	True(0),
	False(1);

	private final int value;
	ReversedIndicator(int val) { value = val; }

	@Override
	public int getValue() {return value;}
}
