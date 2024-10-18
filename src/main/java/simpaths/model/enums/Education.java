package simpaths.model.enums;

public enum Education implements IntegerValuedEnum {
	
	Low(0),
	Medium(1),
	High(2);
	private final int value;
	Education(int val) {value=val;}

	@Override
	public int getValue() {return value;}
	public static Education getCode(int val) {
		if (val == 0)
			return Education.Low;
		else if (val == 1)
			return Education.Medium;
		else return Education.High;
	}
}
