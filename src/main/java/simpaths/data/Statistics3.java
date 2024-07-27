package simpaths.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import microsim.data.db.PanelEntityKey;
import simpaths.model.SimPathsModel;
import simpaths.model.enums.TimeSeriesVariable;

@Entity
public class Statistics3 {

    @Id
    private PanelEntityKey key = new PanelEntityKey(1L);

    @Column(name= "social_care_adj_factor")
    private double socialCareAdjustmentFactor;

    @Column(name = "partnership_adj_factor")
    private double partnershipAdjustmentFactor;

    @Column(name = "utility_adj_factor_smales")
    private double utilityAdjustmentFactorSmales;

    @Column(name = "utility_adj_factor_sfemales")
    private double utilityAdjustmentFactorSfemales;

    @Column(name = "utility_adj_factor_couples")
    private double utilityAdjustmentFactorCouples;

    public double getPartnershipAdjustmentFactor() {
        return partnershipAdjustmentFactor;
    }

    public void setPartnershipAdjustmentFactor(double partnershipAdjustmentFactor) {
        this.partnershipAdjustmentFactor = partnershipAdjustmentFactor;
    }

    public double getUtilityAdjustmentFactorSmales() {
        return utilityAdjustmentFactorSmales;
    }

    public void setUtilityAdjustmentFactorSmales(double utilityAdjustmentFactorSmales) {
        this.utilityAdjustmentFactorSmales = utilityAdjustmentFactorSmales;
    }

    public double getUtilityAdjustmentFactorSfemales() {
        return utilityAdjustmentFactorSfemales;
    }

    public void setUtilityAdjustmentFactorSfemales(double utilityAdjustmentFactorSfemales) {
        this.utilityAdjustmentFactorSfemales = utilityAdjustmentFactorSfemales;
    }

    public double getUtilityAdjustmentFactorCouples() {
        return utilityAdjustmentFactorCouples;
    }

    public void setUtilityAdjustmentFactorCouples(double utilityAdjustmentFactorCouples) {
        this.utilityAdjustmentFactorCouples = utilityAdjustmentFactorCouples;
    }

    public double getSocialCareAdjustmentFactor() { return socialCareAdjustmentFactor; }

    public void setSocialCareAdjustmentFactor(double factor) {socialCareAdjustmentFactor = factor;}

    public void update(SimPathsModel model) {

        setSocialCareAdjustmentFactor(Parameters.getTimeSeriesValue(model.getYear()-1, TimeSeriesVariable.CareProvisionAdjustment));
        setPartnershipAdjustmentFactor(Parameters.getTimeSeriesValue(model.getYear()-1, TimeSeriesVariable.PartnershipAdjustment));
        setUtilityAdjustmentFactorSmales(Parameters.getTimeSeriesValue(model.getYear()-1, TimeSeriesVariable.UtilityAdjustmentSingleMales));
        setUtilityAdjustmentFactorSfemales(Parameters.getTimeSeriesValue(model.getYear()-1, TimeSeriesVariable.UtilityAdjustmentSingleFemales));
        setUtilityAdjustmentFactorCouples(Parameters.getTimeSeriesValue(model.getYear()-1, TimeSeriesVariable.UtilityAdjustmentCouples));
    }
}
