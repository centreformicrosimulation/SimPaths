package simpaths.model.enums;

public enum NotPartnerInformalCarer implements IntegerValuedEnum {
    DaughterOnly(0),
    DaughterAndSon(1),
    DaughterAndOther(2),
    SonOnly(3),
    SonAndOther(4),
    OtherOnly(5);

    private final int value;
    NotPartnerInformalCarer(int val) { value = val; }

    @Override
    public int getValue() {return value;}
}
