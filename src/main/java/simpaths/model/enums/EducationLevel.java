package simpaths.model.enums;

import microsim.statistics.regression.IntegerValuedEnum;

public enum EducationLevel implements IntegerValuedEnum {
	Low(1),
	Medium(2),
	High(3);

	private final int value;
	EducationLevel(int val) {value=val;}

	@Override
	public int getValue() {return value;}

	public int getRank() {
		switch (this) {
			case High:
				return 3;
			case Medium:
				return 2;
			case Low:
				return 1;
			default:
				return -1;
		}
	}
}
