package simpaths.model.enums;

public enum Dhe implements IntegerValuedEnum {
    Poor(1),
    Fair(2),
    Good(3),
    VeryGood(4),
    Excellent(5);

    private final int value;
    Dhe(int val) { value = val; }

    @Override
    public int getValue() {return value;}
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
