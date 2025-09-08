package simpaths.data.statistics;

import microsim.statistics.CrossSection;
import microsim.statistics.IDoubleSource;
import microsim.statistics.functions.MeanArrayFunction;
import microsim.statistics.functions.PercentileArrayFunction;
import microsim.statistics.functions.SumArrayFunction;

public abstract class StatisticsHelper {
    
    protected double calculateMean(CrossSection.Double data) {
        MeanArrayFunction meanFunc = new MeanArrayFunction(data);
        meanFunc.applyFunction();
        return meanFunc.getDoubleValue(IDoubleSource.Variables.Default);
    }
    
    protected void calculateAndSetPercentiles(
            CrossSection.Double data,
            PercentileSetter p10Setter,
            PercentileSetter p25Setter,
            PercentileSetter p50Setter,
            PercentileSetter p75Setter,
            PercentileSetter p90Setter) {
        
        PercentileArrayFunction percFunc = new PercentileArrayFunction(data);
        percFunc.applyFunction();
        
        p10Setter.setPercentile(percFunc.getDoubleValue(PercentileArrayFunction.Variables.P10));
        p25Setter.setPercentile(percFunc.getDoubleValue(PercentileArrayFunction.Variables.P25));
        p50Setter.setPercentile(percFunc.getDoubleValue(PercentileArrayFunction.Variables.P50));
        p75Setter.setPercentile(percFunc.getDoubleValue(PercentileArrayFunction.Variables.P75));
        p90Setter.setPercentile(percFunc.getDoubleValue(PercentileArrayFunction.Variables.P90));
    }

    protected void calculateAndSetCount(CrossSection.Integer data, IntegerValueSetter setter) {
        SumArrayFunction.Integer count = new SumArrayFunction.Integer(data);
        count.applyFunction();

        setter.setValue(count.getIntValue(IDoubleSource.Variables.Default));
    }

    protected void calculateAndSetMean(CrossSection.Double data, DoubleValueSetter setter) {
        MeanArrayFunction meanFunc = new MeanArrayFunction(data);
        meanFunc.applyFunction();

        setter.setValue(meanFunc.getDoubleValue(IDoubleSource.Variables.Default));
    }

    protected void calculateAndSetMean(CrossSection.Integer data, DoubleValueSetter setter) {
        MeanArrayFunction meanFunc = new MeanArrayFunction(data);
        meanFunc.applyFunction();

        setter.setValue(meanFunc.getDoubleValue(IDoubleSource.Variables.Default));
    }
    
    @FunctionalInterface
    protected interface PercentileSetter {
        void setPercentile(double value);
    }

    @FunctionalInterface
    protected interface IntegerValueSetter {
        void setValue(Integer value);
    }

    @FunctionalInterface
    protected interface DoubleValueSetter {
        void setValue(Double value);
    }
}