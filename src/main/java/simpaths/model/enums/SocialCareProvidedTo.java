package simpaths.model.enums;

public enum SocialCareProvidedTo implements DoubleValuedEnum {

    None(0.),
    OnlyPartner(1.),
    PartnerAndOther(2.),
    OnlyOther(3.);

    private final double value;

    SocialCareProvidedTo(double val) {value = val;}
    @Override
    public double getValue() {return value;}
    public static SocialCareMarketAll getCode(double val) {
        if (val<0.5)
            return SocialCareMarketAll.None;
        if (val<1.5)
            return SocialCareMarketAll.Informal;
        else if (val<2.5)
            return SocialCareMarketAll.Mixed;
        else return SocialCareMarketAll.Formal;
    }
    public static SocialCareMarketAll getCode(SocialCareMarket val) {
        if(SocialCareMarket.Informal.equals(val))
            return SocialCareMarketAll.Informal;
        else if (SocialCareMarket.Mixed.equals(val))
            return SocialCareMarketAll.Mixed;
        else
            return SocialCareMarketAll.Formal;
    }
}
