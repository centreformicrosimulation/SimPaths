package simpaths.model;

import com.opencsv.CSVReader;
import microsim.data.excel.ExcelAssistant;
import microsim.statistics.regression.LinearRegression;
import org.junit.jupiter.api.Test;
import simpaths.data.Parameters;
import simpaths.model.enums.Les_c4;

import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConsumptionRegressionTest {
    private static final String[] AGE_NAMES = {"Age20to24", "Age25to29", "Age30to34", "Age35to39",
            "Age40to44", "Age45to49", "Age50to54", "Age55to59", "Age60to64", "Age65to69",
            "Age70to74", "Age75to79", "Age80plus"};

    private BenefitUnit unit(int age, boolean student, int year, double income,
                             Map<String, Double> controls) throws Exception {
        BenefitUnit bu = mock(BenefitUnit.class);
        Person ref = mock(Person.class);
        when(ref.getDemAge()).thenReturn(age);
        when(ref.getLabC4()).thenReturn(student ? Les_c4.Student : Les_c4.NotEmployed);
        when(bu.getRefPerson()).thenReturn(ref);
        when(bu.getYear()).thenReturn(year);
        Field field = BenefitUnit.class.getDeclaredField("yDispMonth");
        field.setAccessible(true);
        field.set(bu, income);
        doAnswer(inv -> {
            String name = ((Enum<?>) inv.getArgument(0)).name();
            if (name.startsWith("Age") || name.equals("LnDispInc") || name.equals("LnDispInc_sq")
                    || name.equals("ReferenceAdultStudent") || name.equals("Year") || name.equals("YearFrom2015")
                    || name.equals("Constant"))
                return inv.callRealMethod();
            Double value = controls.get(name);
            if (value == null) throw new AssertionError("Unmapped fixture regressor " + name);
            return value;
        }).when(bu).getDoubleValue(any(Enum.class));
        return bu;
    }

    @Test
    void predictionsMatchFullPrecisionStataConsumptionModel() throws Exception {
        LinearRegression regression = new LinearRegression(ExcelAssistant.loadCoefficientMap(
                System.getProperty("simpaths.consumption.coefficients",
                        "input/regression_files/reg_wealth_financial.xlsx"), "FW1a", 1));
        Object rmseValue = ExcelAssistant.loadCoefficientMap(
                System.getProperty("simpaths.consumption.rmse",
                        "input/regression_files/reg_RMSE.xlsx"), "UK", 1).getValue("FW1a");
        assertInstanceOf(Number.class, rmseValue);
        double rmse = ((Number) rmseValue).doubleValue();
        boolean oldFix = Parameters.isFixTimeTrend;
        Integer oldStop = Parameters.timeTrendStopsIn;
        try {
            Parameters.isFixTimeTrend = true;
            Parameters.timeTrendStopsIn = 2023;
            try (CSVReader reader = new CSVReader(new InputStreamReader(Objects.requireNonNull(
                    getClass().getResourceAsStream("/simpaths/model/consumption_stata_fixtures.csv")), StandardCharsets.UTF_8))) {
                String[] header = reader.readNext();
                String[] row;
                int count = 0;
                while ((row = reader.readNext()) != null) {
                    Map<String, Double> data = new HashMap<>();
                    for (int i=0; i<header.length; i++) data.put(header[i], Double.parseDouble(row[i]));
                    Map<String, Double> controls = new HashMap<>();
                    controls.put("SingleFemale", data.get("single_female_adult"));
                    controls.put("SingleMale", data.get("single_male_adult"));
                    controls.put("NumberMembersUnder18", data.get("nk"));
                    controls.put("NumberFullTimeWork", data.get("ft"));
                    controls.put("NumberPartTimeWork", data.get("pt"));
                    controls.put("NumberSelfEmployed", data.get("semp"));
                    controls.put("PrivatePensionIncome", data.get("pen_inc_any"));
                    controls.put("MortgageHolder", data.get("housing_status")==1.0 ? 1.0 : 0.0);
                    controls.put("HomeOwnedOutright", data.get("housing_status")==2.0 ? 1.0 : 0.0);
                    String[] regions = {"UKC","UKD","UKE","UKF","UKG","UKH","UKI","UKJ","UKK","UKL","UKM","UKN"};
                    for (int i=0;i<regions.length;i++) controls.put(regions[i], data.get("gorx")==i+1 ? 1.0 : 0.0);
                    for (int year=2009;year<=2022;year++) controls.put("Year"+year, data.get("year")==year ? 1.0 : 0.0);
                    BenefitUnit bu = unit(data.get("age").intValue(), data.get("student")==1.0,
                            data.get("year").intValue(), data.get("income_month"), controls);
                    double score = regression.getScore(bu, BenefitUnit.Variables.class);
                    assertEquals(data.get("stata_xb"), score, 1e-10, "Fixture " + count);
                    assertEquals(data.get("stata_rmse"), rmse, 1e-12, "Installed RMSE");
                    double mean = Math.exp(score + Math.pow(rmse,2)/2);
                    assertEquals(data.get("stata_mean_month"), mean, mean*1e-10, "Mean fixture " + count);
                    count++;
                }
                assertEquals(700, count, "Synthetic grid must cover all age, income, year and student cases");
                System.out.println("Stata/Java consumption parity: " + count + " fixtures passed");
            }
        } finally {
            Parameters.isFixTimeTrend = oldFix;
            Parameters.timeTrendStopsIn = oldStop;
        }
    }

    @Test
    void ageSlopeBoundariesAndIncomeFloor() throws Exception {
        for (int age : new int[]{18,19,20,24,25,29,30,44,45,64,65,79,80,95}) {
            BenefitUnit bu = unit(age, false, 2020, 1000.0, Map.of());
            for (int i=0;i<AGE_NAMES.length;i++) {
                int lower = 20+5*i;
                double expected = age>=lower && (i==12 || age<lower+5) ? Math.log(1000) : 0;
                assertEquals(expected, bu.getDoubleValue(BenefitUnit.Variables.valueOf(AGE_NAMES[i]+"_LnDispInc")),1e-12);
            }
        }
        BenefitUnit zero = unit(25, true, 2020, -5.0, Map.of());
        assertEquals(0,zero.getDoubleValue(BenefitUnit.Variables.Age25to29_LnDispInc));
        assertEquals(1,zero.getDoubleValue(BenefitUnit.Variables.ReferenceAdultStudent));
    }

    @Test
    void centredTrendRetainsExistingFreeze() throws Exception {
        boolean oldFix = Parameters.isFixTimeTrend;
        Integer oldStop = Parameters.timeTrendStopsIn;
        try {
            Parameters.timeTrendStopsIn=2023;
            Parameters.isFixTimeTrend=true;
            assertEquals(8, unit(40,false,2026,1000,Map.of()).getDoubleValue(BenefitUnit.Variables.YearFrom2015));
            Parameters.isFixTimeTrend=false;
            assertEquals(11, unit(40,false,2026,1000,Map.of()).getDoubleValue(BenefitUnit.Variables.YearFrom2015));
        } finally {
            Parameters.isFixTimeTrend=oldFix;
            Parameters.timeTrendStopsIn=oldStop;
        }
    }
}
