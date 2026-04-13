package simpaths.model.enums;

import microsim.statistics.regression.IntegerValuedEnum;

public enum Education implements IntegerValuedEnum {
	Low(3),
	Medium(2),
	High(1),
	InEducation(0);

	private final int value;
	Education(int val) {value=val;}

	@Override
	public int getValue() {return value;}

	// Rank for comparing highest qualification; InEducation is treated as below Low.
	public int getRank() {
		switch (this) {
			case High:
				return 3;
			case Medium:
				return 2;
			case Low:
				return 1;
			case InEducation:
				return 0;
			default:
				return -1;
		}
	}


}
