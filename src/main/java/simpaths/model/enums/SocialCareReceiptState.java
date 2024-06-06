package simpaths.model.enums;

// used to describe state for IO decisions
// Relative to the population projections, IO decisions ignore two categories:
//      (1) those who do not need care are treated equivalently regardless of whether they receive care
//      (2) those who need care and receive only informal support are treated equivalently to those who receive no support (care gap)
public enum SocialCareReceiptState implements DoubleValuedEnum {
    NoneNeeded(0.),
    NoFormal(1.),
    Mixed(2.),
    Formal(3.);

    private final double value;

    SocialCareReceiptState(double val) {value = val;}
    @Override
    public double getValue() {return value;}
    public static SocialCareReceiptState getCode(double val) {
        if (val<0.5)
            return SocialCareReceiptState.NoneNeeded;
        if (val<1.5)
            return SocialCareReceiptState.NoFormal;
        else if (val<2.5)
            return SocialCareReceiptState.Mixed;
        else return SocialCareReceiptState.Formal;
    }
    public static SocialCareReceiptState getCode(SocialCareReceiptS2c val) {
        if(SocialCareReceiptS2c.Informal.equals(val))
            return SocialCareReceiptState.NoFormal;
        else if (SocialCareReceiptS2c.Mixed.equals(val))
            return SocialCareReceiptState.Mixed;
        else
            return SocialCareReceiptState.Formal;
    }
}
