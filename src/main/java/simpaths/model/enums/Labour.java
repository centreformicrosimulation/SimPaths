package simpaths.model.enums;

import microsim.statistics.regression.IntegerValuedEnum;
import simpaths.data.Parameters;
import simpaths.model.Person;

public enum Labour implements IntegerValuedEnum {

    ZERO(0, 0, 0, 0, 0),  // 0 hours for both genders

                                                                                // Female categories            Male categories
    CATEGORY_1(1, 1, 29,   1, 35),   // [1-29] vs [1-35]
    CATEGORY_2(2, 30, 35,  36, 39),  // [30-35] vs [36-39]
    CATEGORY_3(3, 36, 39,  40, 49),  // [36-39] vs [40-49]
    CATEGORY_4(4, 40, Parameters.MAX_LABOUR_HOURS_IN_WEEK, 50, Parameters.MAX_LABOUR_HOURS_IN_WEEK); // [40+] vs [50+]

    private final int categoryId;
    private final int femaleMin, femaleMax;
    private final int maleMin, maleMax;

    Labour(int categoryId, int femaleMin, int femaleMax, int maleMin, int maleMax) {
        this.categoryId = categoryId;
        this.femaleMin = femaleMin;
        this.femaleMax = femaleMax;
        this.maleMin = maleMin;
        this.maleMax = maleMax;
    }

    @Override
    public int getValue() {
        return categoryId;  // Now returns category ID instead of hours
    }

    // Gender-aware conversion methods
    public static Labour convertHoursToLabour(double hoursWorked, Gender gender) {
        if (hoursWorked <= 0) return ZERO;

        return switch (gender) {
            case Female -> convertFemaleHours(hoursWorked);
            default -> convertMaleHours(hoursWorked);
        };
    }

    private static Labour convertFemaleHours(double hours) {
        if (hours <= 29) return CATEGORY_1;
        else if (hours <= 35) return CATEGORY_2;
        else if (hours <= 39) return CATEGORY_3;
        else return CATEGORY_4;
    }

    private static Labour convertMaleHours(double hours) {
        if (hours <= 35) return CATEGORY_1;
        else if (hours <= 39) return CATEGORY_2;
        else if (hours <= 49) return CATEGORY_3;
        else return CATEGORY_4;
    }

    public int getHours(Person person) {
        if (this == ZERO) return 0;

        Gender gender = person.getDgn();
        if (Parameters.USE_CONTINUOUS_LABOUR_SUPPLY_HOURS && person != null) {

            int min = (gender == Gender.Female) ? femaleMin : maleMin;
            int max = (gender == Gender.Female) ? femaleMax : maleMax;

            double draw = person.getLabourSupplySingleDraw();
            return (int) Math.round(draw * (max - min) + min);
        } else {
            // Return midpoint for discrete mode
            return (gender == Gender.Female) ?
                    (femaleMin + femaleMax) / 2 :
                    (maleMin + maleMax) / 2;
        }
    }
}
