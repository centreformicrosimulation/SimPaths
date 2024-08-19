package simpaths.model.enums;

import simpaths.data.Parameters;
import simpaths.model.Person;

/**
 * An enumeration representing different categories of weekly labour supply (work hours) provided by persons.
 */

public enum Labour implements IntegerValuedEnum {

    //Represents hours of work per week that a Person will supply to firms
    ZERO(0, 0, 5), // Note: ZERO always returns 0 continuous hours but maxBound is specified as 5 here to remain consistent with the discretization used in the data
    TEN(10, 6, 15),
	TWENTY(20, 16, 25),
	THIRTY(30, 26, 35),
	FORTY(40, 36, Parameters.MAX_LABOUR_HOURS_IN_WEEK);

    private final int hours, minBound, maxBound;
    Labour(int hours, int minBound, int maxBound) {
        this.hours = hours;
        this.minBound = minBound;
        this.maxBound = maxBound;
    }

    @Override
    public int getValue() {return hours;}

	/**
	 * Converts hours worked (int) to the corresponding labour category.
	 *
	 * @param hoursWorked The hours worked.
	 * @return The labour category.
	 */
    public static Labour convertHoursToLabour(int hoursWorked) {
        return convertHoursToLabourInternal(hoursWorked);
    }

	/**
	 * Converts hours worked (double) to the corresponding labour category.
	 *
	 * @param hoursWorked The hours worked.
	 * @return The labour category.
	 */
    public static Labour convertHoursToLabour(double hoursWorked) {
        return convertHoursToLabourInternal((int) hoursWorked);
    }

    private static Labour convertHoursToLabourInternal(int hoursWorked) {
        if (hoursWorked <= 5) {
            return Labour.ZERO;
        } else if (hoursWorked <= 15) {
            return Labour.TEN;
        } else if (hoursWorked <= 25) {
            return Labour.TWENTY;
        } else if (hoursWorked <= 35) {
            return Labour.THIRTY;
        } else {
            return Labour.FORTY;
        }
    }

    //
	/**
	 * Gets the hours of work based on the labour category and the person's draw from the uniform distribution.
	 * A switch in Parameters class indicates if discretized or continuous hours should be used.
	 * If discretized hours are used, returns number of hours as specified by the hours variable.
	 * If continuous hours are used, returns continuous hours based on person's draw from the uniform distribution and minimum and maximum hours possible in each category, defined by minBound and maxBound.
	 * Note: ZERO category always returns 0 hours of work, irrespective of whether discretized or continuous hours are requested, and irrespective of min and max bound for the ZERO category.
	 *
	 * @param person The person for whom the hours are calculated.
	 * @return The calculated hours of work.
	 */
    public int getHours(Person person) {
        // There were some cases in BenefitUnit where person can be null but hours of work still needed to be obtained where individual is a single adult and labour key composed of two values needs to be defined.
        // I replaced that with a 0. value, instead of converting a ZERO labour key to hours, so person should never be null.
        // However, added a check for null persons which should result in a default number of hours returned in such cases.
        if (this != Labour.ZERO && Parameters.USE_CONTINUOUS_LABOUR_SUPPLY_HOURS && person != null) {

            // Verify that person's draw is not null. If null, draw a value first.
            double personDrawnValue = person.getLabourSupplySingleDraw();

            // Continuous hours are based on person's randomly drawn value. This can be considered person's "type", for example, a person always works hours in the bottom 10% of a (uniformly distributed) labour supply bracket.
            int hours = (int) Math.round(personDrawnValue * (maxBound - minBound) + minBound);
            return hours;

        } else {
            return hours;
        }
    }

}

