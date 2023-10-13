package simpaths.model;

import simpaths.data.Parameters;
import simpaths.experiment.SimPathsCollector;
import simpaths.experiment.SimPathsObserver;
import simpaths.model.enums.Gender;
import simpaths.model.enums.Region;
import microsim.engine.SimulationEngine;
import microsim.statistics.IDoubleSource;



public class Validator implements IDoubleSource {

    private final SimPathsModel model;
    private final SimPathsCollector collector;
    private final SimPathsObserver observer;

    Number value;

    // ---------------------------------------------------------------------
    // CONSTRUCTOR
    // ---------------------------------------------------------------------
    public Validator() {
        super();
        model = (SimPathsModel) SimulationEngine.getInstance().getManager(SimPathsModel.class.getCanonicalName());
        collector = (SimPathsCollector) SimulationEngine.getInstance().getManager(SimPathsCollector.class.getCanonicalName());
        observer = (SimPathsObserver) SimulationEngine.getInstance().getManager(SimPathsObserver.class.getCanonicalName());
    }

    // ---------------------------------------------------------------------
    // Methods used by the validator
    // ---------------------------------------------------------------------
    public int getPopulationProjectionByAge(int startAge, int endAge) {
        double numberOfPeople = 0.;
        for (Gender gender : Gender.values()) {
            for (Region region : Parameters.getCountryRegions()) {
                for (int age = startAge; age <= endAge; age++) {
                    numberOfPeople += Parameters.getPopulationProjections(gender, region, age, model.getYear());
                }
            }
        }
        int numberOfPeopleScaled = (int) Math.round(numberOfPeople / model.getScalingFactor());
        return numberOfPeopleScaled;
    }


    // ---------------------------------------------------------------------
    // implements IDoubleSource for use with the Observer
    // ---------------------------------------------------------------------


    public enum DoublesVariables {
        populationProjectionsByAge_0_18,
        populationProjectionsByAge_0_0,
        populationProjectionsByAge_2_10,
        populationProjectionsByAge_11_15,
        populationProjectionsByAge_19_25,
        populationProjectionsByAge_40_59,
        populationProjectionsByAge_60_79,
        populationProjectionsByAge_80_100,
        studentsByAge_15_19,
        studentsByAge_20_24,
        studentsByAge_25_29,
        studentsByAge_30_34,
        studentsByAge_35_39,
        studentsByAge_40_59,
        studentsByAge_60_79,
        studentsByAge_80_100,
        studentsByAge_All,
        studentsByRegion_ITC,
        studentsByRegion_ITH,
        studentsByRegion_ITI,
        studentsByRegion_ITF,
        studentsByRegion_ITG,
        studentsByRegion_All,
        studentsByRegion_UKC,
        studentsByRegion_UKD,
        studentsByRegion_UKE,
        studentsByRegion_UKF,
        studentsByRegion_UKG,
        studentsByRegion_UKH,
        studentsByRegion_UKI,
        studentsByRegion_UKJ,
        studentsByRegion_UKK,
        studentsByRegion_UKL,
        studentsByRegion_UKM,
        studentsByRegion_UKN,
        educationLevelHigh,
        educationLevelMedium,
        educationLevelLow,
        educationLevelHighByAge_20_29,
        educationLevelHighByAge_30_39,
        educationLevelHighByAge_40_49,
        educationLevelHighByAge_50_59,
        educationLevelMediumByAge_20_29,
        educationLevelMediumByAge_30_39,
        educationLevelMediumByAge_40_49,
        educationLevelMediumByAge_50_59,
        educationLevelLowByAge_20_29,
        educationLevelLowByAge_30_39,
        educationLevelLowByAge_40_49,
        educationLevelLowByAge_50_59,
        educationLevelLowByRegion_ITC,
        educationLevelLowByRegion_ITH,
        educationLevelLowByRegion_ITI,
        educationLevelLowByRegion_ITF,
        educationLevelLowByRegion_ITG,
        educationLevelLowByRegion_UKC,
        educationLevelLowByRegion_UKD,
        educationLevelLowByRegion_UKE,
        educationLevelLowByRegion_UKF,
        educationLevelLowByRegion_UKG,
        educationLevelLowByRegion_UKH,
        educationLevelLowByRegion_UKI,
        educationLevelLowByRegion_UKJ,
        educationLevelLowByRegion_UKK,
        educationLevelLowByRegion_UKL,
        educationLevelLowByRegion_UKM,
        educationLevelLowByRegion_UKN,
        educationLevelHighByRegion_ITC,
        educationLevelHighByRegion_ITH,
        educationLevelHighByRegion_ITI,
        educationLevelHighByRegion_ITF,
        educationLevelHighByRegion_ITG,
        educationLevelHighByRegion_UKC,
        educationLevelHighByRegion_UKD,
        educationLevelHighByRegion_UKE,
        educationLevelHighByRegion_UKF,
        educationLevelHighByRegion_UKG,
        educationLevelHighByRegion_UKH,
        educationLevelHighByRegion_UKI,
        educationLevelHighByRegion_UKJ,
        educationLevelHighByRegion_UKK,
        educationLevelHighByRegion_UKL,
        educationLevelHighByRegion_UKM,
        educationLevelHighByRegion_UKN,
        partneredShare_ITC,
        partneredShare_ITH,
        partneredShare_ITI,
        partneredShare_ITF,
        partneredShare_ITG,
        partneredShare_All,
        partneredShare_UKC,
        partneredShare_UKD,
        partneredShare_UKE,
        partneredShare_UKF,
        partneredShare_UKG,
        partneredShare_UKH,
        partneredShare_UKI,
        partneredShare_UKJ,
        partneredShare_UKK,
        partneredShare_UKL,
        partneredShare_UKM,
        partneredShare_UKN,
        disabledFemale,
        disabledMale,
        disabledFemale_0_49,
        disabledMale_0_49,
        disabledFemale_50_74,
        disabledMale_50_74,
        disabledFemale_75_100,
        disabledMale_75_100,
        healthFemale_0_49,
        healthMale_0_49,
        healthFemale_50_74,
        healthMale_50_74,
        healthFemale_75_100,
        healthMale_75_100,
        mentalHealthMale_20_29,
        mentalHealthMale_30_39,
        mentalHealthMale_40_49,
        mentalHealthMale_50_59,
        mentalHealthFemale_20_29,
        mentalHealthFemale_30_39,
        mentalHealthFemale_40_49,
        mentalHealthFemale_50_59,
        psychDistressMale_20_29,
        psychDistressMale_30_39,
        psychDistressMale_40_49,
        psychDistressMale_50_59,
        psychDistressFemale_20_29,
        psychDistressFemale_30_39,
        psychDistressFemale_40_49,
        psychDistressFemale_50_59,
        psychDistressMale_Low_20_29,
        psychDistressMale_Low_30_39,
        psychDistressMale_Low_40_49,
        psychDistressMale_Low_50_59,
        psychDistressFemale_Low_20_29,
        psychDistressFemale_Low_30_39,
        psychDistressFemale_Low_40_49,
        psychDistressFemale_Low_50_59,

        psychDistressMale_Medium_20_29,
        psychDistressMale_Medium_30_39,
        psychDistressMale_Medium_40_49,
        psychDistressMale_Medium_50_59,
        psychDistressFemale_Medium_20_29,
        psychDistressFemale_Medium_30_39,
        psychDistressFemale_Medium_40_49,
        psychDistressFemale_Medium_50_59,

        psychDistressMale_High_20_29,
        psychDistressMale_High_30_39,
        psychDistressMale_High_40_49,
        psychDistressMale_High_50_59,
        psychDistressFemale_High_20_29,
        psychDistressFemale_High_30_39,
        psychDistressFemale_High_40_49,
        psychDistressFemale_High_50_59,
        employmentMale,
        employmentFemale,
        employmentMaleByAge_20_29,
        employmentMaleByAge_30_39,
        employmentMaleByAge_40_49,
        employmentMaleByAge_50_59,
        employmentFemaleByAge_20_29,
        employmentFemaleByAge_30_39,
        employmentFemaleByAge_40_49,
        employmentFemaleByAge_50_59,
        employmentFemaleChild_0_5,
        employmentFemaleChild_6_18,
        employmentFemaleNoChild, //
        employed_female_ITC,
        employed_male_ITC,
        employed_female_ITH,
        employed_male_ITH,
        employed_female_ITI,
        employed_male_ITI,
        employed_female_ITF,
        employed_male_ITF,
        employed_female_ITG,
        employed_male_ITG,
        employed_female_UKC,
        employed_female_UKD,
        employed_female_UKE,
        employed_female_UKF,
        employed_female_UKG,
        employed_female_UKH,
        employed_female_UKI,
        employed_female_UKJ,
        employed_female_UKK,
        employed_female_UKL,
        employed_female_UKM,
        employed_female_UKN,
        employed_male_UKC,
        employed_male_UKD,
        employed_male_UKE,
        employed_male_UKF,
        employed_male_UKG,
        employed_male_UKH,
        employed_male_UKI,
        employed_male_UKJ,
        employed_male_UKK,
        employed_male_UKL,
        employed_male_UKM,
        employed_male_UKN,
        labour_supply_High,
        labour_supply_Medium,
        labour_supply_Low,
        activityStatus_Employed,
        activityStatus_NotEmployedRetired,
        activityStatus_Student,
        homeownership_BenefitUnit,
        grossEarnings_Female_High,
        grossEarnings_Female_Medium,
        grossEarnings_Female_Low,
        grossEarnings_Male_High,
        grossEarnings_Male_Medium,
        grossEarnings_Male_Low,
        lhw_Female_High,
        lhw_Female_Medium,
        lhw_Female_Low,
        lhw_Male_High,
        lhw_Male_Medium,
        lhw_Male_Low,
        lhw_Male,
        lhw_Female,
        hourlyWage_Female_High,
        hourlyWage_Female_Medium,
        hourlyWage_Female_Low,
        hourlyWage_Male_High,
        hourlyWage_Male_Medium,
        hourlyWage_Male_Low,
        }

    @Override
    public double getDoubleValue(Enum<?> variableID) {

        switch ((Validator.DoublesVariables) variableID) {

            case populationProjectionsByAge_0_18:
                return getPopulationProjectionByAge(0,18);
            case populationProjectionsByAge_0_0:
                return getPopulationProjectionByAge(0,0);
            case populationProjectionsByAge_2_10:
                return getPopulationProjectionByAge(2,10);
            case populationProjectionsByAge_11_15:
                return getPopulationProjectionByAge(11,15);
            case populationProjectionsByAge_19_25:
                return getPopulationProjectionByAge(19,25);
            case populationProjectionsByAge_40_59:
                return getPopulationProjectionByAge(40,59);
            case populationProjectionsByAge_60_79:
                return getPopulationProjectionByAge(60,79);
            case populationProjectionsByAge_80_100:
                return getPopulationProjectionByAge(80,100);
            case studentsByAge_15_19:
                value = ((Number) Parameters.getValidationStudentsByAge().getValue(model.getYear()-1, "ageGroup_15_19"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case studentsByAge_20_24:
                value = ((Number) Parameters.getValidationStudentsByAge().getValue(model.getYear()-1, "ageGroup_20_24"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case studentsByAge_25_29:
                value = ((Number) Parameters.getValidationStudentsByAge().getValue(model.getYear()-1, "ageGroup_25_29"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case studentsByAge_30_34:
                value = ((Number) Parameters.getValidationStudentsByAge().getValue(model.getYear()-1, "ageGroup_30_34"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case studentsByAge_35_39:
                value = ((Number) Parameters.getValidationStudentsByAge().getValue(model.getYear()-1, "ageGroup_35_39"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case studentsByAge_40_59:
                value = ((Number) Parameters.getValidationStudentsByAge().getValue(model.getYear()-1, "ageGroup_40_59"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case studentsByAge_60_79:
                value = ((Number) Parameters.getValidationStudentsByAge().getValue(model.getYear()-1, "ageGroup_60_79"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case studentsByAge_80_100:
                value = ((Number) Parameters.getValidationStudentsByAge().getValue(model.getYear()-1, "ageGroup_80_100"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case studentsByAge_All:
                value = ((Number) Parameters.getValidationStudentsByAge().getValue(model.getYear()-1, "ageGroup_All"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case studentsByRegion_ITC:
                value = ((Number) Parameters.getValidationStudentsByRegion().getValue(model.getYear()-1, "region_ITC"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case studentsByRegion_ITH:
                value = ((Number) Parameters.getValidationStudentsByRegion().getValue(model.getYear()-1, "region_ITH"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case studentsByRegion_ITI:
                value = ((Number) Parameters.getValidationStudentsByRegion().getValue(model.getYear()-1, "region_ITI"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case studentsByRegion_ITF:
                value = ((Number) Parameters.getValidationStudentsByRegion().getValue(model.getYear()-1, "region_ITF"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case studentsByRegion_ITG:
                value = ((Number) Parameters.getValidationStudentsByRegion().getValue(model.getYear()-1, "region_ITG"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case studentsByRegion_All:
                value = ((Number) Parameters.getValidationStudentsByRegion().getValue(model.getYear()-1, "region_All"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case studentsByRegion_UKC:
                value = ((Number) Parameters.getValidationStudentsByRegion().getValue(model.getYear()-1, "region_UKC"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case studentsByRegion_UKD:
                value = ((Number) Parameters.getValidationStudentsByRegion().getValue(model.getYear()-1, "region_UKD"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case studentsByRegion_UKE:
                value = ((Number) Parameters.getValidationStudentsByRegion().getValue(model.getYear()-1, "region_UKE"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case studentsByRegion_UKF:
                value = ((Number) Parameters.getValidationStudentsByRegion().getValue(model.getYear()-1, "region_UKF"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case studentsByRegion_UKG:
                value = ((Number) Parameters.getValidationStudentsByRegion().getValue(model.getYear()-1, "region_UKG"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case studentsByRegion_UKH:
                value = ((Number) Parameters.getValidationStudentsByRegion().getValue(model.getYear()-1, "region_UKH"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case studentsByRegion_UKI:
                value = ((Number) Parameters.getValidationStudentsByRegion().getValue(model.getYear()-1, "region_UKI"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case studentsByRegion_UKJ:
                value = ((Number) Parameters.getValidationStudentsByRegion().getValue(model.getYear()-1, "region_UKJ"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case studentsByRegion_UKK:
                value = ((Number) Parameters.getValidationStudentsByRegion().getValue(model.getYear()-1, "region_UKK"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case studentsByRegion_UKL:
                value = ((Number) Parameters.getValidationStudentsByRegion().getValue(model.getYear()-1, "region_UKL"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case studentsByRegion_UKM:
                value = ((Number) Parameters.getValidationStudentsByRegion().getValue(model.getYear()-1, "region_UKM"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case studentsByRegion_UKN:
                value = ((Number) Parameters.getValidationStudentsByRegion().getValue(model.getYear()-1, "region_UKN"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelHigh:
                value = ((Number) Parameters.getValidationEducationLevel().getValue(model.getYear()-1, "educ_high"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelMedium:
                value = ((Number) Parameters.getValidationEducationLevel().getValue(model.getYear()-1, "educ_med"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelLow:
                value = ((Number) Parameters.getValidationEducationLevel().getValue(model.getYear()-1, "educ_low"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelHighByAge_20_29:
                 value = ((Number) Parameters.getValidationEducationLevelByAge().getValue(model.getYear()-1, "educ_high_20_29"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelHighByAge_30_39:
                 value = ((Number) Parameters.getValidationEducationLevelByAge().getValue(model.getYear()-1, "educ_high_30_39"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelHighByAge_40_49:
                 value = ((Number) Parameters.getValidationEducationLevelByAge().getValue(model.getYear()-1, "educ_high_40_49"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelHighByAge_50_59:
                 value = ((Number) Parameters.getValidationEducationLevelByAge().getValue(model.getYear()-1, "educ_high_50_59"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelMediumByAge_20_29:
                value = ((Number) Parameters.getValidationEducationLevelByAge().getValue(model.getYear()-1, "educ_med_20_29"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelMediumByAge_30_39:
                value = ((Number) Parameters.getValidationEducationLevelByAge().getValue(model.getYear()-1, "educ_med_30_39"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelMediumByAge_40_49:
                value = ((Number) Parameters.getValidationEducationLevelByAge().getValue(model.getYear()-1, "educ_med_40_49"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelMediumByAge_50_59:
                value = ((Number) Parameters.getValidationEducationLevelByAge().getValue(model.getYear()-1, "educ_med_50_59"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelLowByAge_20_29:
                value = ((Number) Parameters.getValidationEducationLevelByAge().getValue(model.getYear()-1, "educ_low_20_29"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelLowByAge_30_39:
                value = ((Number) Parameters.getValidationEducationLevelByAge().getValue(model.getYear()-1, "educ_low_30_39"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelLowByAge_40_49:
                value = ((Number) Parameters.getValidationEducationLevelByAge().getValue(model.getYear()-1, "educ_low_40_49"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelLowByAge_50_59:
                value = ((Number) Parameters.getValidationEducationLevelByAge().getValue(model.getYear()-1, "educ_low_50_59"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelLowByRegion_ITC:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_low_ITC"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelLowByRegion_ITH:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_low_ITH"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelLowByRegion_ITI:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_low_ITI"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelLowByRegion_ITF:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_low_ITF"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelLowByRegion_ITG:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_low_ITG"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelLowByRegion_UKC:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_low_UKC"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelLowByRegion_UKD:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_low_UKD"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelLowByRegion_UKE:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_low_UKE"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelLowByRegion_UKF:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_low_UKF"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelLowByRegion_UKG:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_low_UKG"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelLowByRegion_UKH:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_low_UKH"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelLowByRegion_UKI:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_low_UKI"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelLowByRegion_UKJ:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_low_UKJ"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelLowByRegion_UKK:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_low_UKK"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelLowByRegion_UKL:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_low_UKL"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelLowByRegion_UKM:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_low_UKM"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelLowByRegion_UKN:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_low_UKN"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelHighByRegion_ITC:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_high_ITC"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelHighByRegion_ITH:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_high_ITH"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelHighByRegion_ITI:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_high_ITI"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelHighByRegion_ITF:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_high_ITF"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelHighByRegion_ITG:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_high_ITG"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelHighByRegion_UKC:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_high_UKC"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelHighByRegion_UKD:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_high_UKD"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelHighByRegion_UKE:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_high_UKE"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelHighByRegion_UKF:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_high_UKF"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelHighByRegion_UKG:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_high_UKG"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelHighByRegion_UKH:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_high_UKH"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelHighByRegion_UKI:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_high_UKI"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelHighByRegion_UKJ:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_high_UKJ"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelHighByRegion_UKK:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_high_UKK"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelHighByRegion_UKL:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_high_UKL"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelHighByRegion_UKM:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_high_UKM"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelHighByRegion_UKN:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_high_UKN"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case partneredShare_ITC:
                value = ((Number) Parameters.getValidationPartneredShareByRegion().getValue(model.getYear()-1, "partnered_ITC"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case partneredShare_ITH:
                value = ((Number) Parameters.getValidationPartneredShareByRegion().getValue(model.getYear()-1, "partnered_ITH"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case partneredShare_ITI:
                value = ((Number) Parameters.getValidationPartneredShareByRegion().getValue(model.getYear()-1, "partnered_ITI"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case partneredShare_ITF:
                value = ((Number) Parameters.getValidationPartneredShareByRegion().getValue(model.getYear()-1, "partnered_ITF"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case partneredShare_ITG:
                value = ((Number) Parameters.getValidationPartneredShareByRegion().getValue(model.getYear()-1, "partnered_ITG"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case partneredShare_All:
                value = ((Number) Parameters.getValidationPartneredShareByRegion().getValue(model.getYear()-1, "partnered_All"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case partneredShare_UKC:
                value = ((Number) Parameters.getValidationPartneredShareByRegion().getValue(model.getYear()-1, "partnered_UKC"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case partneredShare_UKD:
                value = ((Number) Parameters.getValidationPartneredShareByRegion().getValue(model.getYear()-1, "partnered_UKD"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case partneredShare_UKE:
                value = ((Number) Parameters.getValidationPartneredShareByRegion().getValue(model.getYear()-1, "partnered_UKE"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case partneredShare_UKF:
                value = ((Number) Parameters.getValidationPartneredShareByRegion().getValue(model.getYear()-1, "partnered_UKF"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case partneredShare_UKG:
                value = ((Number) Parameters.getValidationPartneredShareByRegion().getValue(model.getYear()-1, "partnered_UKG"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case partneredShare_UKH:
                value = ((Number) Parameters.getValidationPartneredShareByRegion().getValue(model.getYear()-1, "partnered_UKH"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case partneredShare_UKI:
                value = ((Number) Parameters.getValidationPartneredShareByRegion().getValue(model.getYear()-1, "partnered_UKI"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case partneredShare_UKJ:
                value = ((Number) Parameters.getValidationPartneredShareByRegion().getValue(model.getYear()-1, "partnered_UKJ"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case partneredShare_UKK:
                value = ((Number) Parameters.getValidationPartneredShareByRegion().getValue(model.getYear()-1, "partnered_UKK"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case partneredShare_UKL:
                value = ((Number) Parameters.getValidationPartneredShareByRegion().getValue(model.getYear()-1, "partnered_UKL"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case partneredShare_UKM:
                value = ((Number) Parameters.getValidationPartneredShareByRegion().getValue(model.getYear()-1, "partnered_UKM"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case partneredShare_UKN:
                value = ((Number) Parameters.getValidationPartneredShareByRegion().getValue(model.getYear()-1, "partnered_UKN"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case disabledFemale:
                value = ((Number) Parameters.getValidationDisabledByGender().getValue(model.getYear()-1, "dlltsd_female"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case disabledMale:
                value = ((Number) Parameters.getValidationDisabledByGender().getValue(model.getYear()-1, "dlltsd_male"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case disabledFemale_0_49:
                value = ((Number) Parameters.getValidationDisabledByAge().getValue(model.getYear()-1, "disabled_female_0_49"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case disabledMale_0_49:
                value = ((Number) Parameters.getValidationDisabledByAge().getValue(model.getYear()-1, "disabled_male_0_49"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case disabledFemale_50_74:
                value = ((Number) Parameters.getValidationDisabledByAge().getValue(model.getYear()-1, "disabled_female_50_74"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case disabledMale_50_74:
                value = ((Number) Parameters.getValidationDisabledByAge().getValue(model.getYear()-1, "disabled_male_50_74"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case disabledFemale_75_100:
                value = ((Number) Parameters.getValidationDisabledByAge().getValue(model.getYear()-1, "disabled_female_75_100"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case disabledMale_75_100:
                value = ((Number) Parameters.getValidationDisabledByAge().getValue(model.getYear()-1, "disabled_male_75_100"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case healthFemale_0_49:
                value = ((Number) Parameters.getValidationHealthByAge().getValue(model.getYear()-1, "health_female_0_49"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case healthMale_0_49:
                value = ((Number) Parameters.getValidationHealthByAge().getValue(model.getYear()-1, "health_male_0_49"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case healthFemale_50_74:
                value = ((Number) Parameters.getValidationHealthByAge().getValue(model.getYear()-1, "health_female_50_74"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case healthMale_50_74:
                value = ((Number) Parameters.getValidationHealthByAge().getValue(model.getYear()-1, "health_male_50_74"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case healthFemale_75_100:
                value = ((Number) Parameters.getValidationHealthByAge().getValue(model.getYear()-1, "health_female_75_100"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case healthMale_75_100:
                value = ((Number) Parameters.getValidationHealthByAge().getValue(model.getYear()-1, "health_male_75_100"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case mentalHealthMale_20_29:
                value = ((Number) Parameters.getValidationMentalHealthByAge().getValue(model.getYear()-1, "mental_health_male_20_29"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case mentalHealthMale_30_39:
                value = ((Number) Parameters.getValidationMentalHealthByAge().getValue(model.getYear()-1, "mental_health_male_30_39"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case mentalHealthMale_40_49:
                value = ((Number) Parameters.getValidationMentalHealthByAge().getValue(model.getYear()-1, "mental_health_male_40_49"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case mentalHealthMale_50_59:
                value = ((Number) Parameters.getValidationMentalHealthByAge().getValue(model.getYear()-1, "mental_health_male_50_59"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case mentalHealthFemale_20_29:
                value = ((Number) Parameters.getValidationMentalHealthByAge().getValue(model.getYear()-1, "mental_health_female_20_29"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case mentalHealthFemale_30_39:
                value = ((Number) Parameters.getValidationMentalHealthByAge().getValue(model.getYear()-1, "mental_health_female_30_39"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case mentalHealthFemale_40_49:
                value = ((Number) Parameters.getValidationMentalHealthByAge().getValue(model.getYear()-1, "mental_health_female_40_49"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case mentalHealthFemale_50_59:
                value = ((Number) Parameters.getValidationMentalHealthByAge().getValue(model.getYear()-1, "mental_health_female_50_59"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case psychDistressMale_20_29:
                value = ((Number) Parameters.getValidationPsychDistressByAge().getValue(model.getYear()-1, "psych_distress_male_20_29"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case psychDistressMale_30_39:
                value = ((Number) Parameters.getValidationPsychDistressByAge().getValue(model.getYear()-1, "psych_distress_male_30_39"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case psychDistressMale_40_49:
                value = ((Number) Parameters.getValidationPsychDistressByAge().getValue(model.getYear()-1, "psych_distress_male_40_49"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case psychDistressMale_50_59:
                value = ((Number) Parameters.getValidationPsychDistressByAge().getValue(model.getYear()-1, "psych_distress_male_50_59"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case psychDistressFemale_20_29:
                value = ((Number) Parameters.getValidationPsychDistressByAge().getValue(model.getYear()-1, "psych_distress_female_20_29"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case psychDistressFemale_30_39:
                value = ((Number) Parameters.getValidationPsychDistressByAge().getValue(model.getYear()-1, "psych_distress_female_30_39"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case psychDistressFemale_40_49:
                value = ((Number) Parameters.getValidationPsychDistressByAge().getValue(model.getYear()-1, "psych_distress_female_40_49"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case psychDistressFemale_50_59:
                value = ((Number) Parameters.getValidationPsychDistressByAge().getValue(model.getYear()-1, "psych_distress_female_50_59"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            case psychDistressMale_Low_20_29:
                value = ((Number) Parameters.getValidationPsychDistressByAgeLow().getValue(model.getYear()-1, "psych_distress_male_20_29"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case psychDistressMale_Low_30_39:
                value = ((Number) Parameters.getValidationPsychDistressByAgeLow().getValue(model.getYear()-1, "psych_distress_male_30_39"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case psychDistressMale_Low_40_49:
                value = ((Number) Parameters.getValidationPsychDistressByAgeLow().getValue(model.getYear()-1, "psych_distress_male_40_49"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case psychDistressMale_Low_50_59:
                value = ((Number) Parameters.getValidationPsychDistressByAgeLow().getValue(model.getYear()-1, "psych_distress_male_50_59"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case psychDistressFemale_Low_20_29:
                value = ((Number) Parameters.getValidationPsychDistressByAgeLow().getValue(model.getYear()-1, "psych_distress_female_20_29"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case psychDistressFemale_Low_30_39:
                value = ((Number) Parameters.getValidationPsychDistressByAgeLow().getValue(model.getYear()-1, "psych_distress_female_30_39"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case psychDistressFemale_Low_40_49:
                value = ((Number) Parameters.getValidationPsychDistressByAgeLow().getValue(model.getYear()-1, "psych_distress_female_40_49"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case psychDistressFemale_Low_50_59:
                value = ((Number) Parameters.getValidationPsychDistressByAgeLow().getValue(model.getYear()-1, "psych_distress_female_50_59"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap

            case psychDistressMale_Medium_20_29:
                value = ((Number) Parameters.getValidationPsychDistressByAgeMed().getValue(model.getYear()-1, "psych_distress_male_20_29"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case psychDistressMale_Medium_30_39:
                value = ((Number) Parameters.getValidationPsychDistressByAgeMed().getValue(model.getYear()-1, "psych_distress_male_30_39"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case psychDistressMale_Medium_40_49:
                value = ((Number) Parameters.getValidationPsychDistressByAgeMed().getValue(model.getYear()-1, "psych_distress_male_40_49"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case psychDistressMale_Medium_50_59:
                value = ((Number) Parameters.getValidationPsychDistressByAgeMed().getValue(model.getYear()-1, "psych_distress_male_50_59"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case psychDistressFemale_Medium_20_29:
                value = ((Number) Parameters.getValidationPsychDistressByAgeMed().getValue(model.getYear()-1, "psych_distress_female_20_29"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case psychDistressFemale_Medium_30_39:
                value = ((Number) Parameters.getValidationPsychDistressByAgeMed().getValue(model.getYear()-1, "psych_distress_female_30_39"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case psychDistressFemale_Medium_40_49:
                value = ((Number) Parameters.getValidationPsychDistressByAgeMed().getValue(model.getYear()-1, "psych_distress_female_40_49"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case psychDistressFemale_Medium_50_59:
                value = ((Number) Parameters.getValidationPsychDistressByAgeMed().getValue(model.getYear()-1, "psych_distress_female_50_59"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap

            case psychDistressMale_High_20_29:
                value = ((Number) Parameters.getValidationPsychDistressByAgeHigh().getValue(model.getYear()-1, "psych_distress_male_20_29"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case psychDistressMale_High_30_39:
                value = ((Number) Parameters.getValidationPsychDistressByAgeHigh().getValue(model.getYear()-1, "psych_distress_male_30_39"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case psychDistressMale_High_40_49:
                value = ((Number) Parameters.getValidationPsychDistressByAgeHigh().getValue(model.getYear()-1, "psych_distress_male_40_49"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case psychDistressMale_High_50_59:
                value = ((Number) Parameters.getValidationPsychDistressByAgeHigh().getValue(model.getYear()-1, "psych_distress_male_50_59"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case psychDistressFemale_High_20_29:
                value = ((Number) Parameters.getValidationPsychDistressByAgeHigh().getValue(model.getYear()-1, "psych_distress_female_20_29"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case psychDistressFemale_High_30_39:
                value = ((Number) Parameters.getValidationPsychDistressByAgeHigh().getValue(model.getYear()-1, "psych_distress_female_30_39"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case psychDistressFemale_High_40_49:
                value = ((Number) Parameters.getValidationPsychDistressByAgeHigh().getValue(model.getYear()-1, "psych_distress_female_40_49"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case psychDistressFemale_High_50_59:
                value = ((Number) Parameters.getValidationPsychDistressByAgeHigh().getValue(model.getYear()-1, "psych_distress_female_50_59"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            case employmentMale:
                value = ((Number) Parameters.getValidationEmploymentByGender().getValue(model.getYear()-1, "employed_Male"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employmentFemale:
                value = ((Number) Parameters.getValidationEmploymentByGender().getValue(model.getYear()-1, "employed_Female"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employmentMaleByAge_20_29:
                value = ((Number) Parameters.getValidationEmploymentByAgeAndGender().getValue(model.getYear()-1, "employed_male_20_29"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employmentMaleByAge_30_39:
                value = ((Number) Parameters.getValidationEmploymentByAgeAndGender().getValue(model.getYear()-1, "employed_male_30_39"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employmentMaleByAge_40_49:
                value = ((Number) Parameters.getValidationEmploymentByAgeAndGender().getValue(model.getYear()-1, "employed_male_40_49"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employmentMaleByAge_50_59:
                value = ((Number) Parameters.getValidationEmploymentByAgeAndGender().getValue(model.getYear()-1, "employed_male_50_59"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employmentFemaleByAge_20_29:
                value = ((Number) Parameters.getValidationEmploymentByAgeAndGender().getValue(model.getYear()-1, "employed_female_20_29"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employmentFemaleByAge_30_39:
                value = ((Number) Parameters.getValidationEmploymentByAgeAndGender().getValue(model.getYear()-1, "employed_female_30_39"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employmentFemaleByAge_40_49:
                value = ((Number) Parameters.getValidationEmploymentByAgeAndGender().getValue(model.getYear()-1, "employed_female_40_49"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employmentFemaleByAge_50_59:
                value = ((Number) Parameters.getValidationEmploymentByAgeAndGender().getValue(model.getYear()-1, "employed_female_50_59"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employmentFemaleChild_0_5:
                value = ((Number) Parameters.getValidationEmploymentByMaternity().getValue(model.getYear()-1, "emp_with_child_0_5"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employmentFemaleChild_6_18:
                value = ((Number) Parameters.getValidationEmploymentByMaternity().getValue(model.getYear()-1, "emp_with_child_6_18"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employmentFemaleNoChild:
                value = ((Number) Parameters.getValidationEmploymentByMaternity().getValue(model.getYear()-1, "emp_without_child"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_female_ITC:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_female_ITC"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_male_ITC:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_male_ITC"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_female_ITH:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_female_ITH"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_male_ITH:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_male_ITH"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_female_ITI:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_female_ITI"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_male_ITI:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_male_ITI"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_female_ITF:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_female_ITF"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_male_ITF:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_male_ITF"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_female_ITG:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_female_ITG"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_male_ITG:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_male_ITG"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_female_UKC:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_female_UKC"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_female_UKD:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_female_UKD"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_female_UKE:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_female_UKE"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_female_UKF:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_female_UKF"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_female_UKG:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_female_UKG"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_female_UKH:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_female_UKH"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_female_UKI:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_female_UKI"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_female_UKJ:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_female_UKJ"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_female_UKK:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_female_UKK"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_female_UKL:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_female_UKL"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_female_UKM:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_female_UKM"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_female_UKN:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_female_UKN"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_male_UKC:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_male_UKC"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_male_UKD:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_male_UKD"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_male_UKE:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_male_UKE"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_male_UKF:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_male_UKF"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_male_UKG:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_male_UKG"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_male_UKH:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_male_UKH"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_male_UKI:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_male_UKI"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_male_UKJ:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_male_UKJ"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_male_UKK:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_male_UKK"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_male_UKL:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_male_UKK"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_male_UKM:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_male_UKM"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_male_UKN:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_male_UKN"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case labour_supply_High:
                value = ((Number) Parameters.getValidationLabourSupplyByEducation().getValue(model.getYear()-1, "labour_supply_High"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case labour_supply_Medium:
                value = ((Number) Parameters.getValidationLabourSupplyByEducation().getValue(model.getYear()-1, "labour_supply_Medium"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case labour_supply_Low:
                value = ((Number) Parameters.getValidationLabourSupplyByEducation().getValue(model.getYear()-1, "labour_supply_Low"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case activityStatus_Employed:
                value = ((Number) Parameters.getValidationActivityStatus().getValue(model.getYear()-1, "as_employed"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case activityStatus_NotEmployedRetired:
                value = ((Number) Parameters.getValidationActivityStatus().getValue(model.getYear()-1, "as_notemployedretired"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case activityStatus_Student:
                value = ((Number) Parameters.getValidationActivityStatus().getValue(model.getYear()-1, "as_student"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case homeownership_BenefitUnit:
                value = ((Number) Parameters.getValidationHomeownershipBenefitUnits().getValue(model.getYear()-1));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case grossEarnings_Female_High:
                 value = ((Number) Parameters.getValidationGrossEarningsByGenderAndEducation().getValue(model.getYear()-1, "grossearnings_female_dehc3_high"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case grossEarnings_Female_Medium:
                 value = ((Number) Parameters.getValidationGrossEarningsByGenderAndEducation().getValue(model.getYear()-1, "grossearnings_female_dehc3_med"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case grossEarnings_Female_Low:
                 value = ((Number) Parameters.getValidationGrossEarningsByGenderAndEducation().getValue(model.getYear()-1, "grossearnings_female_dehc3_low"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case grossEarnings_Male_High:
                 value = ((Number) Parameters.getValidationGrossEarningsByGenderAndEducation().getValue(model.getYear()-1, "grossearnings_male_dehc3_high"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case grossEarnings_Male_Medium:
                 value = ((Number) Parameters.getValidationGrossEarningsByGenderAndEducation().getValue(model.getYear()-1, "grossearnings_male_dehc3_med"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case grossEarnings_Male_Low:
                 value = ((Number) Parameters.getValidationGrossEarningsByGenderAndEducation().getValue(model.getYear()-1, "grossearnings_male_dehc3_low"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case lhw_Female_High:
                 value = ((Number) Parameters.getValidationLhwByGenderAndEducation().getValue(model.getYear()-1, "lhw_female_dehc3_high"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case lhw_Female_Medium:
                 value = ((Number) Parameters.getValidationLhwByGenderAndEducation().getValue(model.getYear()-1, "lhw_female_dehc3_med"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case lhw_Female_Low:
                 value = ((Number) Parameters.getValidationLhwByGenderAndEducation().getValue(model.getYear()-1, "lhw_female_dehc3_low"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case lhw_Male_High:
                 value = ((Number) Parameters.getValidationLhwByGenderAndEducation().getValue(model.getYear()-1, "lhw_male_dehc3_high"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case lhw_Male_Medium:
                 value = ((Number) Parameters.getValidationLhwByGenderAndEducation().getValue(model.getYear()-1, "lhw_male_dehc3_med"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case lhw_Male_Low:
                 value = ((Number) Parameters.getValidationLhwByGenderAndEducation().getValue(model.getYear()-1, "lhw_male_dehc3_low"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case lhw_Male:
                value = ((Number) Parameters.getValidationLhwByGenderAndEducation().getValue(model.getYear()-1, "lhw_male"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case lhw_Female:
                value = ((Number) Parameters.getValidationLhwByGenderAndEducation().getValue(model.getYear()-1, "lhw_female"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case hourlyWage_Female_High:
                 value = ((Number) Parameters.getHourlyWageByGenderAndEducation().getValue(model.getYear()-1, "hourlywage_female_dehc3_high"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case hourlyWage_Female_Medium:
                 value = ((Number) Parameters.getHourlyWageByGenderAndEducation().getValue(model.getYear()-1, "hourlywage_female_dehc3_med"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case hourlyWage_Female_Low:
                 value = ((Number) Parameters.getHourlyWageByGenderAndEducation().getValue(model.getYear()-1, "hourlywage_female_dehc3_low"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case hourlyWage_Male_High:
                 value = ((Number) Parameters.getHourlyWageByGenderAndEducation().getValue(model.getYear()-1, "hourlywage_male_dehc3_high"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case hourlyWage_Male_Medium:
                 value = ((Number) Parameters.getHourlyWageByGenderAndEducation().getValue(model.getYear()-1, "hourlywage_male_dehc3_med"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case hourlyWage_Male_Low:
                 value = ((Number) Parameters.getHourlyWageByGenderAndEducation().getValue(model.getYear()-1, "hourlywage_male_dehc3_low"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
        }

        return 0;
    }
}
