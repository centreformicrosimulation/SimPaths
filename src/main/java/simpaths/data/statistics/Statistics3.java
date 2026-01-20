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
    private double careAdj;

    @Column(name = "partnership_adj_factor")
    private double demPartnerAdj;

    @Column(name = "share_cohabiting_sim")
    private double demPartnerSimShare;

    @Column(name = "share_cohabiting_tgt")
    private double demPartnerTargetShare;

    @Column(name = "fertility_adj_factor")
    private double demFertAdj;

    @Column(name = "fertiilty_rate_sim")
    private double demFertRateSim;

    @Column(name = "fertiilty_rate_tgt")
    private double demFertRateTarget;

    @Column(name = "utility_adj_factor_smales")
    private double demUtilAdjSingleM;

    @Column(name = "utility_adj_factor_sfemales")
    private double demUtilAdjSingleF;

    @Column(name = "utility_adj_factor_couples")
    private double demUtilAdjCouple;

    public double getPartnershipAdjustmentFactor() {
        return demPartnerAdj;
    }

    public void setPartnershipAdjustmentFactor(double demPartnerAdj) {
        this.demPartnerAdj = demPartnerAdj;
    }

    public double getFertilityAdjustmentFactor() {
        return demFertAdj;
    }

    public void setFertilityAdjustmentFactor(double factor) {
        this.demFertAdj = factor;
    }

    public double getUtilityAdjustmentFactorSmales() {
        return demUtilAdjSingleM;
    }

    public void setUtilityAdjustmentFactorSmales(double demUtilAdjSingleM) {
        this.demUtilAdjSingleM = demUtilAdjSingleM;
    }

    public double getUtilityAdjustmentFactorSfemales() {
        return demUtilAdjSingleF;
    }

    public void setUtilityAdjustmentFactorSfemales(double demUtilAdjSingleF) {
        this.demUtilAdjSingleF = demUtilAdjSingleF;
    }

    public double getUtilityAdjustmentFactorCouples() {
        return demUtilAdjCouple;
    }

    public void setUtilityAdjustmentFactorCouples(double demUtilAdjCouple) {
        this.demUtilAdjCouple = demUtilAdjCouple;
    }

    public double getSocialCareAdjustmentFactor() { return careAdj; }

    public void setSocialCareAdjustmentFactor(double factor) {
        careAdj = factor;}

    public double getShareCohabitingSimulated() {return demPartnerSimShare;}

    public void setShareCohabitingSimulated(double demPartnerSimShare) { this.demPartnerSimShare = demPartnerSimShare; }

    public double getFertilityRateSimulated() {
        return demFertRateSim;
    }

    public void setFertilityRateSimulated(double demFertRateSim) {
        this.demFertRateSim = demFertRateSim;
    }

    public double getFertilityRateTarget() {
        return demFertRateTarget;
    }

    public void setFertilityRateTarget(double demFertRateTarget) {
        this.demFertRateTarget = demFertRateTarget;
    }

    public double getShareCohabitingTarget() {
        return demPartnerTargetShare;
    }

    public void setShareCohabitingTarget(double demPartnerTargetShare) {
        this.demPartnerTargetShare = demPartnerTargetShare;
    }

    public void update(SimPathsModel model) {

        // cohabitation
        double val = Parameters.getTimeSeriesValue(model.getYear()-1, TimeSeriesVariable.PartnershipAdjustment) +
                Parameters.getAlignmentValue(model.getYear()-1, AlignmentVariable.PartnershipAlignment);
        setPartnershipAdjustmentFactor(val);
        long numPersonsWhoCanHavePartner = model.getPersons().stream()
                .filter(person -> person.getDemAge() >= Parameters.MIN_AGE_COHABITATION)
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
                .filter(person -> (person.getDemAge() < 1))
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
