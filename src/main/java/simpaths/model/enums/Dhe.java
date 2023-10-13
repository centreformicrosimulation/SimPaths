package simpaths.model.enums;

public enum Dhe implements DoubleValuedEnum {
    Poor(1.0),
    Fair(2.0),
    Good(3.0),
    VeryGood(4.0),
    Excellent(5.0);

    private final double value;

    Dhe(double val) {
        value = val;}
    @Override
    public double getValue() {return value;}
    public static Dhe getCode(double val) {
        if (val < 1.5)
            return Dhe.Poor;
        else if (val < 2.5)
            return Dhe.Fair;
        else if (val < 3.5)
            return Dhe.Good;
        else if (val < 4.5)
            return Dhe.VeryGood;
        else return Dhe.Excellent;
    }
}
