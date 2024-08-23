package simpaths.data.statistics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import microsim.data.db.PanelEntityKey;
import simpaths.data.Parameters;
import simpaths.data.filters.FertileFilter;
import simpaths.model.SimPathsModel;
import simpaths.model.enums.AlignmentVariable;
import simpaths.model.enums.Dcpst;
import simpaths.model.enums.TargetShares;
import simpaths.model.enums.TimeSeriesVariable;

@Entity
public class Statistics3 {

    @Id
    private PanelEntityKey key = new PanelEntityKey(1L);

    @Column(name= "social_care_adj_factor")
    private double socialCareAdjustmentFactor;

    @Column(name = "partnership_adj_factor")
    private double partnershipAdjustmentFactor;

    @Column(name = "share_cohabiting_sim")
    private double shareCohabitingSimulated;

    @Column(name = "share_cohabiting_tgt")
    private double shareCohabitingTarget;

    @Column(name = "fertility_adj_factor")
    private double fertilityAdjustmentFactor;

    @Column(name = "fertiilty_rate_sim")
    private double fertilityRateSimulated;

    @Column(name = "fertiilty_rate_tgt")
    private double fertilityRateTarget;

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

    public double getFertilityAdjustmentFactor() {
        return fertilityAdjustmentFactor;
    }

    public void setFertilityAdjustmentFactor(double factor) {
        this.fertilityAdjustmentFactor = factor;
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

    public double getShareCohabitingSimulated() {return shareCohabitingSimulated;}

    public void setShareCohabitingSimulated(double shareCohabitingSimulated) { this.shareCohabitingSimulated = shareCohabitingSimulated; }

    public double getFertilityRateSimulated() {
        return fertilityRateSimulated;
    }

    public void setFertilityRateSimulated(double fertilityRateSimulated) {
        this.fertilityRateSimulated = fertilityRateSimulated;
    }

    public double getFertilityRateTarget() {
        return fertilityRateTarget;
    }

    public void setFertilityRateTarget(double fertilityRateTarget) {
        this.fertilityRateTarget = fertilityRateTarget;
    }

    public double getShareCohabitingTarget() {
        return shareCohabitingTarget;
    }

    public void setShareCohabitingTarget(double shareCohabitingTarget) {
        this.shareCohabitingTarget = shareCohabitingTarget;
    }

    public void update(SimPathsModel model) {

        // cohabitation
        double val = Parameters.getTimeSeriesValue(model.getYear()-1, TimeSeriesVariable.PartnershipAdjustment) +
                Parameters.getAlignmentValue(model.getYear()-1, AlignmentVariable.PartnershipAlignment);
        setPartnershipAdjustmentFactor(val);
        long numPersonsWhoCanHavePartner = model.getPersons().stream()
                .filter(person -> person.getDag() >= Parameters.MIN_AGE_COHABITATION)
                .count();
        long numPersonsPartnered = model.getPersons().stream()
                .filter(person -> (person.getDcpst().equals(Dcpst.Partnered)))
                .count();
        val = (numPersonsWhoCanHavePartner > 0) ? (double) numPersonsPartnered / numPersonsWhoCanHavePartner : 0.0;
        setShareCohabitingSimulated(val);
        setShareCohabitingTarget(Parameters.getTargetShare(model.getYear()-1, TargetShares.Partnership));

        // fertility
        val = Parameters.getTimeSeriesValue(model.getYear()-1, TimeSeriesVariable.FertilityAdjustment) +
                Parameters.getAlignmentValue(model.getYear()-1, AlignmentVariable.FertilityAlignment);
        setFertilityAdjustmentFactor(val);
        FertileFilter filter = new FertileFilter();
        long numFertilePersons = model.getPersons().stream()
                .filter(person -> filter.evaluate(person))
                .count();
        long numBirths = model.getPersons().stream()
                .filter(person -> (person.getDag() < 1))
                .count();
        val = (numFertilePersons > 0) ? (double) numBirths / numFertilePersons : 0.0;
        setFertilityRateSimulated(val);
        setFertilityRateTarget(Parameters.getFertilityRateByYear(model.getYear()-1));

        setSocialCareAdjustmentFactor(Parameters.getTimeSeriesValue(model.getYear()-1, TimeSeriesVariable.CareProvisionAdjustment));
        setUtilityAdjustmentFactorSmales(Parameters.getTimeSeriesValue(model.getYear()-1, TimeSeriesVariable.UtilityAdjustmentSingleMales));
        setUtilityAdjustmentFactorSfemales(Parameters.getTimeSeriesValue(model.getYear()-1, TimeSeriesVariable.UtilityAdjustmentSingleFemales));
        setUtilityAdjustmentFactorCouples(Parameters.getTimeSeriesValue(model.getYear()-1, TimeSeriesVariable.UtilityAdjustmentCouples));
    }
}
