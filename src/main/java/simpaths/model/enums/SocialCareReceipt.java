package simpaths.model.enums;

public enum SocialCareReceipt implements IntegerValuedEnum {
    None(0),
    Informal(1),
    Mixed(2),
    Formal(3);

    private final int value;
    SocialCareReceipt(int val) {value = val;}

    @Override
    public int getValue() {return value;}
    public static SocialCareReceipt getCode(double val) {
        if (val<0.5)
            return SocialCareReceipt.None;
        if (val<1.5)
            return SocialCareReceipt.Informal;
        else if (val<2.5)
            return SocialCareReceipt.Mixed;
        else return SocialCareReceipt.Formal;
    }
    public static SocialCareReceipt getCode(SocialCareReceiptS2c val) {
        if(SocialCareReceiptS2c.Informal.equals(val))
            return SocialCareReceipt.Informal;
        else if (SocialCareReceiptS2c.Mixed.equals(val))
            return SocialCareReceipt.Mixed;
        else
            return SocialCareReceipt.Formal;
    }
}
