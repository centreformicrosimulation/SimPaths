package simpaths.data.statistics;

import microsim.statistics.CrossSection;
import microsim.statistics.IDoubleSource;
import microsim.statistics.functions.MeanArrayFunction;
import microsim.statistics.functions.PercentileArrayFunction;

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
    
    @FunctionalInterface
    protected interface PercentileSetter {
        void setPercentile(double value);
    }
}