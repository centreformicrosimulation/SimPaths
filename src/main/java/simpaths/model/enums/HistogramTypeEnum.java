package simpaths.model.enums;

import org.jfree.data.statistics.HistogramType;

/**
 * Specify JFreeChart's HistogramType for use in Observer class' Weighted_HistogramPlotter
 */
public enum HistogramTypeEnum {

	Frequency(HistogramType.FREQUENCY);
	
	//The following do not currently work properly for Weighted Histograms
//	RelativeFrequency(HistogramType.RELATIVE_FREQUENCY),
//	ScaleAreaTo1(HistogramType.SCALE_AREA_TO_1);
	
    private final HistogramType histType;

	//This defines the order as Males take the first value, Females the second.
    private HistogramTypeEnum(HistogramType histType)
    {
        this.histType = histType;
    }

    public HistogramType getHistogramType()
    {
        return histType;
    }
    
}
