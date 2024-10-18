package simpaths.model.enums;

// for interaction with multi-nomial logit (S2c)
public enum SocialCareReceiptS2c implements IntegerValuedEnum {
    Informal(0),
    Mixed(1),
    Formal(2);

    private final int value;
    SocialCareReceiptS2c(int val) { value = val; }

    @Override
    public int getValue() {return value;}
}
