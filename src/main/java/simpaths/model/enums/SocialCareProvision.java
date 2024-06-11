package simpaths.model.enums;

public enum SocialCareProvision implements IntegerValuedEnum {

    None(0),
    OnlyPartner(1),
    PartnerAndOther(2),
    OnlyOther(3);

    private final int value;
    SocialCareProvision(int val) {value = val;}

    @Override
    public int getValue() {return value;}
    public static SocialCareProvision getCode(double val) {
        if (val<0.5)
            return SocialCareProvision.None;
        if (val<1.5)
            return SocialCareProvision.OnlyPartner;
        else if (val<2.5)
            return SocialCareProvision.PartnerAndOther;
        else return SocialCareProvision.OnlyOther;
    }
}
