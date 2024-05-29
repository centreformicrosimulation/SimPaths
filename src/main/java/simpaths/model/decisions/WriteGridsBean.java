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
    private int birthYear;
    private int gender;
    private int student;
    private int education;
    private int health;
    private int cohabitation;
    private int wageOffer;
    private int nk0;
    private int nk1;
    private int nk2;
    private double liquidWealth;
    private double wagePotentialperHour;
    private double pensionIncomePerYear;
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
    public void setBirthYear(int birthYear) {
        this.birthYear = birthYear;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }

    public void setStudent(int student) {
        this.student = student;
    }

    public void setEducation(int education) {
        this.education = education;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public void setCohabitation(int cohabitation) {
        this.cohabitation = cohabitation;
    }

    public void setWageOffer(int wageOffer) {
        this.wageOffer = wageOffer;
    }

    public void setNk0(int nk0) {
        this.nk0 = nk0;
    }

    public void setNk1(int nk1) {
        this.nk1 = nk1;
    }

    public void setNk2(int nk2) {
        this.nk2 = nk2;
    }

    public void setLiquidWealth(double liquidWealth) {
        this.liquidWealth = liquidWealth;
    }

    public void setWagePotentialperHour(double wagePotentialperHour) {
        this.wagePotentialperHour = wagePotentialperHour;
    }

    public void setPensionIncomePerYear(double pensionIncomePerYear) {
        this.pensionIncomePerYear = pensionIncomePerYear;
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
    public int getBirthYear() {
        return birthYear;
    }

    public int getGender() {
        return gender;
    }

    public int getStudent() {
        return student;
    }

    public int getEducation() {
        return education;
    }

    public int getHealth() {
        return health;
    }

    public int getCohabitation() {
        return cohabitation;
    }

    public int getWageOffer() {
        return wageOffer;
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

    public double getLiquidWealth() {
        return liquidWealth;
    }

    public double getWagePotentialperHour() {
        return wagePotentialperHour;
    }

    public double getPensionIncomePerYear() {
        return pensionIncomePerYear;
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

    public String getEmployment1String() {
        return Double.toString(employment1);
    }
    public String getEmployment2String() {
        return Double.toString(employment2);
    }
    public String getWagePotentialperHourString() {
        return Double.toString(wagePotentialperHour);
    }
    public String getPensionIncomePerYearString() {
        return Double.toString(pensionIncomePerYear);
    }
    public String getEducationString() {
        return String.valueOf(education);
    }
    public String getStudentString() {
        return String.valueOf(student);
    }
    public String getHealthString() {
        return String.valueOf(health);
    }
    public String getValueFunctionString() {
        return Double.toString(valueFunction);
    }
    public String getConsumptionShareString() {
        return Double.toString(consumptionShare);
    }
    public String getLiquidWealthString() {
        return Double.toString(liquidWealth);
    }
    public String getCohabitationString() {
        return String.valueOf(cohabitation);
    }
    public String getWageOfferString() {
        return String.valueOf(wageOffer);
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
    public String getGenderString() {
        return String.valueOf(gender);
    }
    public String getBirthYearString() {
        return String.valueOf(birthYear);
    }
}
