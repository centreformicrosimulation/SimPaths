package simpaths.model.enums;

public enum Education implements DoubleValuedEnum {
	
	Low(0.0),
	Medium(1.0),
	High(2.0);
	private final double value;
	Education(double val) {value=val;}
	@Override
	public double getValue() {return value;}
	public static Education getCode(int val) {
		if (val == 0)
			return Education.Low;
		else if (val == 1)
			return Education.Medium;
		else return Education.High;
	}
}
