package simpaths.model.decisions;


//import com.opencsv.bean.CsvBindByName;

/**
 *
 * CLASS TO STORE GRID SOLUTIONS FOR WRITING TO CSV FILE
 *
 */
public class WriteBean {

    /**
     * ATTRIBUTES
     */
    //@CsvBindByName(column = "birthyear")
    private int birthYear;
    //@CsvBindByName(column = "gender")
    private int gender;
    //@CsvBindByName(column = "education")
    private int education;
    //@CsvBindByName(column = "health")
    private int health;
    //@CsvBindByName(column = "cohabitation")
    private int cohabitation;
    //@CsvBindByName(column = "liquidwealth")
    private double liquidWealth;
    //@CsvBindByName(column = "wagepotential")
    private double wagePotentialperHour;
    //@CsvBindByName(column = "pension")
    private double pensionIncomePerYear;
    //@CsvBindByName(column = "consumption")
    private double consumptionShare;
    //@CsvBindByName(column = "employment1")
    private double employment1;
    //@CsvBindByName(column = "employment2")
    private double employment2;
    //@CsvBindByName(column = "valuefunction")
    private double valueFunction;

    /**
     * CONSTRUCTOR
     */
    public WriteBean(){}

    /**
     * GETTERS AND SETTERS
     */

    public double getEmployment1() {
        return employment1;
    }
    public String getEmployment1String() {
        return Double.toString(employment1);
    }

    public void setEmployment1(double employment1) {
        this.employment1 = employment1;
    }

    public double getEmployment2() {
        return employment2;
    }
    public String getEmployment2String() {
        return Double.toString(employment2);
    }

    public void setEmployment2(double employment2) {
        this.employment2 = employment2;
    }

    public double getWagePotentialperHour() {
        return wagePotentialperHour;
    }
    public String getWagePotentialperHourString() {
        return Double.toString(wagePotentialperHour);
    }

    public void setWagePotentialperHour(double wagePotentialperHour) {
        this.wagePotentialperHour = wagePotentialperHour;
    }

    public double getPensionIncomePerYear() {
        return pensionIncomePerYear;
    }
    public String getPensionIncomePerYearString() {
        return Double.toString(pensionIncomePerYear);
    }

    public void setPensionIncomePerYear(double pensionIncomePerYear) {
        this.pensionIncomePerYear = pensionIncomePerYear;
    }

    public int getEducation() {
        return education;
    }
    public String getEducationString() {
        return String.valueOf(education);
    }

    public void setEducation(int education) {
        this.education = education;
    }

    public int getHealth() {
        return health;
    }
    public String getHealthString() {
        return String.valueOf(health);
    }

    public void setHealth(int health) {
        this.health = health;
    }
    public double getValueFunction() {
        return valueFunction;
    }
    public String getValueFunctionString() {
        return Double.toString(valueFunction);
    }

    public void setValueFunction(double valueFunction) {
        this.valueFunction = valueFunction;
    }

    public double getConsumptionShare() {
        return consumptionShare;
    }
    public String getConsumptionShareString() {
        return Double.toString(consumptionShare);
    }

    public void setConsumptionShare(double consumptionShare) {
        this.consumptionShare = consumptionShare;
    }

    public double getLiquidWealth() {
        return liquidWealth;
    }
    public String getLiquidWealthString() {
        return Double.toString(liquidWealth);
    }

    public void setLiquidWealth(double liquidWealth) {
        this.liquidWealth = liquidWealth;
    }

    public int getCohabitation() {
        return cohabitation;
    }
    public String getCohabitationString() {
        return String.valueOf(cohabitation);
    }

    public void setCohabitation(int cohabitation) {
        this.cohabitation = cohabitation;
    }

    public int getGender() {
        return gender;
    }
    public String getGenderString() {
        return String.valueOf(gender);
    }

    public void setGender(int gender) {
        this.gender = gender;
    }

    public int getBirthYear() {
        return birthYear;
    }
    public String getBirthYearString() {
        return String.valueOf(birthYear);
    }

    public void setBirthYear(int birthYear) {
        this.birthYear = birthYear;
    }
}
