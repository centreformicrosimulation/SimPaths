package simpaths.model.enums;

public enum SocialCareReceiptAll implements DoubleValuedEnum {
    None(0.),
    Informal(1.),
    Mixed(2.),
    Formal(3.);

    private final double value;

    SocialCareReceiptAll(double val) {value = val;}
    @Override
    public double getValue() {return value;}
    public static SocialCareReceiptAll getCode(double val) {
        if (val<0.5)
            return SocialCareReceiptAll.None;
        if (val<1.5)
            return SocialCareReceiptAll.Informal;
        else if (val<2.5)
            return SocialCareReceiptAll.Mixed;
        else return SocialCareReceiptAll.Formal;
    }
    public static SocialCareReceiptAll getCode(SocialCareReceipt val) {
        if(SocialCareReceipt.Informal.equals(val))
            return SocialCareReceiptAll.Informal;
        else if (SocialCareReceipt.Mixed.equals(val))
            return SocialCareReceiptAll.Mixed;
        else
            return SocialCareReceiptAll.Formal;
    }
}
