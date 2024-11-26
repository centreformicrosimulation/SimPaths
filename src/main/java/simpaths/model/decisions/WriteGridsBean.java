package simpaths.model.decisions;


/**
 *
 * CLASS TO STORE GRID SOLUTIONS FOR WRITING TO CSV FILE
 *
 */
public class WriteGridsBean {

    /**
     * ATTRIBUTES
     */
    //@CsvBindByName(column = "birthyear")
    private double liquidWealth;
    private double wagePotentialperHour;
    private double pensionIncomePerYear;
    private int birthYear;
    private int wageOffer1;
    private int wageOffer2;
    private int retirement;
    private int health;
    private int disability;
    private int socialCareReceipt;
    private int socialCareProvision;
    private int region;
    private int student;
    private int education;
    private int nk0;
    private int nk1;
    private int nk2;
    private int cohabitation;
    private int gender;
    private double consumptionShare;
    private double employment1;
    private double employment2;
    private double valueFunction;

    /**
     * CONSTRUCTOR
     */
    public WriteGridsBean(){}


    /**
     * SETTERS
     */
    public void setLiquidWealth(double liquidWealth) {
        this.liquidWealth = liquidWealth;
    }
    public void setWagePotentialperHour(double wagePotentialperHour) {this.wagePotentialperHour = wagePotentialperHour;}
    public void setPensionIncomePerYear(double pensionIncomePerYear) {this.pensionIncomePerYear = pensionIncomePerYear;}
    public void setBirthYear(int birthYear) {
        this.birthYear = birthYear;
    }
    public void setWageOffer1(int wageOffer1) {this.wageOffer1 = wageOffer1;}
    public void setWageOffer2(int wageOffer2) {this.wageOffer2 = wageOffer2;}
    public void setRetirement(int retirement) {this.retirement = retirement;}
    public void setHealth(int health) {
        this.health = health;
    }
    public void setDisability(int disability) {this.disability = disability;}
    public void setSocialCareReceipt(int socialCareReceipt) {this.socialCareReceipt = socialCareReceipt;}
    public void setSocialCareProvision(int socialCareProvision) {this.socialCareProvision = socialCareProvision;}
    public void setRegion(int region) {this.region = region;}
    public void setStudent(int student) {
        this.student = student;
    }
    public void setEducation(int education) {
        this.education = education;
    }
    public void setNk0(int nk0) {this.nk0 = nk0;}
    public void setNk1(int nk1) {
        this.nk1 = nk1;
    }
    public void setNk2(int nk2) {
        this.nk2 = nk2;
    }
    public void setCohabitation(int cohabitation) {this.cohabitation = cohabitation;}
    public void setGender(int gender) {
        this.gender = gender;
    }
    public void setConsumptionShare(double consumptionShare) {
        this.consumptionShare = consumptionShare;
    }
    public void setEmployment1(double employment1) {
        this.employment1 = employment1;
    }
    public void setEmployment2(double employment2) {
        this.employment2 = employment2;
    }
    public void setValueFunction(double valueFunction) {
        this.valueFunction = valueFunction;
    }


    /**
     * GETTERS
     */
    public double getLiquidWealth() {
        return liquidWealth;
    }
    public double getWagePotentialperHour() {
        return wagePotentialperHour;
    }
    public double getPensionIncomePerYear() {
        return pensionIncomePerYear;
    }
    public int getBirthYear() {
        return birthYear;
    }
    public int getWageOffer1() {
        return wageOffer1;
    }
    public int getWageOffer2() {return wageOffer2;}
    public int getRetirement() {return retirement;}
    public int getHealth() {
        return health;
    }
    public int getDisability() {return disability;}
    public int getSocialCareReceipt() {return socialCareReceipt;}
    public int getSocialCareProvision() {return socialCareProvision;}
    public int getRegion() {return region;}
    public int getStudent() {
        return student;
    }
    public int getEducation() {
        return education;
    }
    public int getNk0() {
        return nk0;
    }
    public int getNk1() {
        return nk1;
    }
    public int getNk2() {
        return nk2;
    }
    public int getCohabitation() {
        return cohabitation;
    }
    public int getGender() {
        return gender;
    }
    public double getConsumptionShare() {
        return consumptionShare;
    }
    public double getEmployment1() {
        return employment1;
    }
    public double getEmployment2() {
        return employment2;
    }
    public double getValueFunction() {
        return valueFunction;
    }


    /**
     * STRING GETTERS
     */
    public String getLiquidWealthString() {
        return Double.toString(liquidWealth);
    }
    public String getWagePotentialperHourString() {
        return Double.toString(wagePotentialperHour);
    }
    public String getPensionIncomePerYearString() {
        return Double.toString(pensionIncomePerYear);
    }
    public String getBirthYearString() {
        return String.valueOf(birthYear);
    }
    public String getWageOffer1String() {
        return String.valueOf(wageOffer1);
    }
    public String getWageOffer2String() {
        return String.valueOf(wageOffer2);
    }
    public String getRetirementString() {
        return String.valueOf(retirement);
    }
    public String getHealthString() {
        return String.valueOf(health);
    }
    public String getDisabilityString() {
        return String.valueOf(disability);
    }
    public String getSocialCareReceiptString() {return String.valueOf(socialCareReceipt);}
    public String getSocialCareProvisionString() {return String.valueOf(socialCareProvision);}
    public String getRegionString() {return String.valueOf(region);}
    public String getStudentString() {
        return String.valueOf(student);
    }
    public String getEducationString() {
        return String.valueOf(education);
    }
    public String getNk0String() {
        return String.valueOf(nk0);
    }
    public String getNk1String() {
        return String.valueOf(nk1);
    }
    public String getNk2String() {
        return String.valueOf(nk2);
    }
    public String getCohabitationString() {
        return String.valueOf(cohabitation);
    }
    public String getGenderString() {
        return String.valueOf(gender);
    }
    public String getConsumptionShareString() {
        return Double.toString(consumptionShare);
    }
    public String getEmployment1String() {
        return Double.toString(employment1);
    }
    public String getEmployment2String() {
        return Double.toString(employment2);
    }
    public String getValueFunctionString() {
        return Double.toString(valueFunction);
    }
}
