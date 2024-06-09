package simpaths.model.enums;

public enum PartnerSupplementaryCarer implements IntegerValuedEnum {
    None(0),
    Daughter(1),
    Son(2),
    Other(3);

    private final int value;
    PartnerSupplementaryCarer(int val) { value = val; }

    @Override
    public int getValue() {return value;}
}
