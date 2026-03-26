package simpaths.data.statistics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import microsim.data.db.PanelEntityKey;
import simpaths.data.Parameters;
import simpaths.data.filters.FertileFilter;
import simpaths.model.BenefitUnit;
import simpaths.model.Person;
import simpaths.model.SimPathsModel;
import simpaths.model.enums.*;

/**
 *
 * CLASS TO REPORT ALIGNMENT ADJUSTMENT FACTORS AND SIMULATED VS TARGET SHARES
 *
 * Covers: partnership, fertility, in-school, utility adjustment factors, and
 * employment shares by occupancy type. Disability and retirement are excluded.
 *
 */
@Entity
public class AlignmentAdjustmentFactors {

    @Id
    private PanelEntityKey key = new PanelEntityKey(1L);

    // ------------------------------------------------------------------
    // Partnership
    // ------------------------------------------------------------------
    @Column(name = "partnership_adj_factor")
    private double partnershipAdjFactor;

    @Column(name = "share_cohabiting_sim")
    private double shareCohabitingSim;

    @Column(name = "share_cohabiting_tgt")
    private double shareCohabitingTgt;

    // ------------------------------------------------------------------
    // Fertility
    // ------------------------------------------------------------------
    @Column(name = "fertility_adj_factor")
    private double fertilityAdjFactor;

    @Column(name = "fertility_rate_sim")
    private double fertilityRateSim;

    @Column(name = "fertility_rate_tgt")
    private double fertilityRateTgt;

    // ------------------------------------------------------------------
    // In-school
    // ------------------------------------------------------------------
    @Column(name = "in_school_adj_factor")
    private double inSchoolAdjFactor;

    @Column(name = "in_school_share_sim")
    private double inSchoolShareSim;

    @Column(name = "in_school_share_tgt")
    private double inSchoolShareTgt;

    // ------------------------------------------------------------------
    // Utility adjustment factors (one per occupancy type)
    // ------------------------------------------------------------------
    @Column(name = "utility_adj_single_males")
    private double utilityAdjSingleMales;

    @Column(name = "utility_adj_ac_males")
    private double utilityAdjACMales;

    @Column(name = "utility_adj_single_females")
    private double utilityAdjSingleFemales;

    @Column(name = "utility_adj_ac_females")
    private double utilityAdjACFemales;

    @Column(name = "utility_adj_couples")
    private double utilityAdjCouples;

    @Column(name = "utility_adj_single_dep_males")
    private double utilityAdjSingleDepMales;

    @Column(name = "utility_adj_single_dep_females")
    private double utilityAdjSingleDepFemales;

    // ------------------------------------------------------------------
    // Employment shares — simulated
    // ------------------------------------------------------------------
    @Column(name = "emp_share_sim_single_males")
    private double empShareSimSingleMales;

    @Column(name = "emp_share_sim_single_females")
    private double empShareSimSingleFemales;

    @Column(name = "emp_share_sim_ac_males")
    private double empShareSimACMales;

    @Column(name = "emp_share_sim_ac_females")
    private double empShareSimACFemales;

    @Column(name = "emp_share_sim_couples")
    private double empShareSimCouples;

    @Column(name = "emp_share_sim_single_dep_males")
    private double empShareSimSingleDepMales;

    @Column(name = "emp_share_sim_single_dep_females")
    private double empShareSimSingleDepFemales;

    // ------------------------------------------------------------------
    // Employment shares — target
    // ------------------------------------------------------------------
    @Column(name = "emp_share_tgt_single_males")
    private double empShareTgtSingleMales;

    @Column(name = "emp_share_tgt_single_females")
    private double empShareTgtSingleFemales;

    @Column(name = "emp_share_tgt_ac_males")
    private double empShareTgtACMales;

    @Column(name = "emp_share_tgt_ac_females")
    private double empShareTgtACFemales;

    @Column(name = "emp_share_tgt_couples")
    private double empShareTgtCouples;

    @Column(name = "emp_share_tgt_single_dep_males")
    private double empShareTgtSingleDepMales;

    @Column(name = "emp_share_tgt_single_dep_females")
    private double empShareTgtSingleDepFemales;


    // ------------------------------------------------------------------
    // Getters and setters
    // ------------------------------------------------------------------

    public double getPartnershipAdjFactor() { return partnershipAdjFactor; }
    public void setPartnershipAdjFactor(double v) { partnershipAdjFactor = v; }

    public double getShareCohabitingSim() { return shareCohabitingSim; }
    public void setShareCohabitingSim(double v) { shareCohabitingSim = v; }

    public double getShareCohabitingTgt() { return shareCohabitingTgt; }
    public void setShareCohabitingTgt(double v) { shareCohabitingTgt = v; }

    public double getFertilityAdjFactor() { return fertilityAdjFactor; }
    public void setFertilityAdjFactor(double v) { fertilityAdjFactor = v; }

    public double getFertilityRateSim() { return fertilityRateSim; }
    public void setFertilityRateSim(double v) { fertilityRateSim = v; }

    public double getFertilityRateTgt() { return fertilityRateTgt; }
    public void setFertilityRateTgt(double v) { fertilityRateTgt = v; }

    public double getInSchoolAdjFactor() { return inSchoolAdjFactor; }
    public void setInSchoolAdjFactor(double v) { inSchoolAdjFactor = v; }

    public double getInSchoolShareSim() { return inSchoolShareSim; }
    public void setInSchoolShareSim(double v) { inSchoolShareSim = v; }

    public double getInSchoolShareTgt() { return inSchoolShareTgt; }
    public void setInSchoolShareTgt(double v) { inSchoolShareTgt = v; }

    public double getUtilityAdjSingleMales() { return utilityAdjSingleMales; }
    public void setUtilityAdjSingleMales(double v) { utilityAdjSingleMales = v; }

    public double getUtilityAdjACMales() { return utilityAdjACMales; }
    public void setUtilityAdjACMales(double v) { utilityAdjACMales = v; }

    public double getUtilityAdjSingleFemales() { return utilityAdjSingleFemales; }
    public void setUtilityAdjSingleFemales(double v) { utilityAdjSingleFemales = v; }

    public double getUtilityAdjACFemales() { return utilityAdjACFemales; }
    public void setUtilityAdjACFemales(double v) { utilityAdjACFemales = v; }

    public double getUtilityAdjCouples() { return utilityAdjCouples; }
    public void setUtilityAdjCouples(double v) { utilityAdjCouples = v; }

    public double getUtilityAdjSingleDepMales() { return utilityAdjSingleDepMales; }
    public void setUtilityAdjSingleDepMales(double v) { utilityAdjSingleDepMales = v; }

    public double getUtilityAdjSingleDepFemales() { return utilityAdjSingleDepFemales; }
    public void setUtilityAdjSingleDepFemales(double v) { utilityAdjSingleDepFemales = v; }

    public double getEmpShareSimSingleMales() { return empShareSimSingleMales; }
    public void setEmpShareSimSingleMales(double v) { empShareSimSingleMales = v; }

    public double getEmpShareSimSingleFemales() { return empShareSimSingleFemales; }
    public void setEmpShareSimSingleFemales(double v) { empShareSimSingleFemales = v; }

    public double getEmpShareSimACMales() { return empShareSimACMales; }
    public void setEmpShareSimACMales(double v) { empShareSimACMales = v; }

    public double getEmpShareSimACFemales() { return empShareSimACFemales; }
    public void setEmpShareSimACFemales(double v) { empShareSimACFemales = v; }

    public double getEmpShareSimCouples() { return empShareSimCouples; }
    public void setEmpShareSimCouples(double v) { empShareSimCouples = v; }

    public double getEmpShareSimSingleDepMales() { return empShareSimSingleDepMales; }
    public void setEmpShareSimSingleDepMales(double v) { empShareSimSingleDepMales = v; }

    public double getEmpShareSimSingleDepFemales() { return empShareSimSingleDepFemales; }
    public void setEmpShareSimSingleDepFemales(double v) { empShareSimSingleDepFemales = v; }

    public double getEmpShareTgtSingleMales() { return empShareTgtSingleMales; }
    public void setEmpShareTgtSingleMales(double v) { empShareTgtSingleMales = v; }

    public double getEmpShareTgtSingleFemales() { return empShareTgtSingleFemales; }
    public void setEmpShareTgtSingleFemales(double v) { empShareTgtSingleFemales = v; }

    public double getEmpShareTgtACMales() { return empShareTgtACMales; }
    public void setEmpShareTgtACMales(double v) { empShareTgtACMales = v; }

    public double getEmpShareTgtACFemales() { return empShareTgtACFemales; }
    public void setEmpShareTgtACFemales(double v) { empShareTgtACFemales = v; }

    public double getEmpShareTgtCouples() { return empShareTgtCouples; }
    public void setEmpShareTgtCouples(double v) { empShareTgtCouples = v; }

    public double getEmpShareTgtSingleDepMales() { return empShareTgtSingleDepMales; }
    public void setEmpShareTgtSingleDepMales(double v) { empShareTgtSingleDepMales = v; }

    public double getEmpShareTgtSingleDepFemales() { return empShareTgtSingleDepFemales; }
    public void setEmpShareTgtSingleDepFemales(double v) { empShareTgtSingleDepFemales = v; }


    // ------------------------------------------------------------------
    // update()
    // ------------------------------------------------------------------

    public void update(SimPathsModel model) {

        int year = model.getYear() - 1;  // year just simulated (consistent with annual collector exports)

        // --- Partnership ---
        setPartnershipAdjFactor(
                Parameters.getTimeSeriesValue(year, TimeSeriesVariable.PartnershipAdjustment)
                + model.getPartnershipAdjustment(year));
        long numPersonsCohabEligible = model.getPersons().stream()
                .filter(p -> p.getDemAge() >= Parameters.MIN_AGE_COHABITATION)
                .count();
        long numPersonsPartnered = model.getPersons().stream()
                .filter(p -> Dcpst.Partnered.equals(p.getDcpst()))
                .count();
        setShareCohabitingSim(numPersonsCohabEligible > 0
                ? (double) numPersonsPartnered / numPersonsCohabEligible : 0.0);
        setShareCohabitingTgt(Parameters.getTargetShare(year, TargetShares.Partnership));

        // --- Fertility ---
        setFertilityAdjFactor(
                Parameters.getTimeSeriesValue(year, TimeSeriesVariable.FertilityAdjustment)
                + model.getFertilityAdjustment(year));
        FertileFilter fertileFilter = new FertileFilter();
        long numFertile = model.getPersons().stream()
                .filter(p -> fertileFilter.evaluate(p))
                .count();
        long numBirths = model.getPersons().stream()
                .filter(p -> p.getDemAge() < 1)
                .count();
        setFertilityRateSim(numFertile > 0 ? (double) numBirths / numFertile : 0.0);
        setFertilityRateTgt(Parameters.getFertilityRateByYear(year));

        // --- In-school ---
        setInSchoolAdjFactor(Parameters.getTimeSeriesValue(year, TimeSeriesVariable.InSchoolAdjustment));
        long numStudents = model.getPersons().stream()
                .filter(p -> p.getDemAge() >= Parameters.MIN_AGE_TO_LEAVE_EDUCATION
                        && p.getDemAge() <= Parameters.MAX_AGE_TO_STAY_IN_CONTINUOUS_EDUCATION
                        && !p.isToLeaveSchool()
                        && Les_c4.Student.equals(p.getLes_c4()))
                .count();
        long numInSchoolAge = model.getPersons().stream()
                .filter(p -> p.getDemAge() >= Parameters.MIN_AGE_TO_LEAVE_EDUCATION
                        && p.getDemAge() <= Parameters.MAX_AGE_TO_STAY_IN_CONTINUOUS_EDUCATION
                        && p.getLes_c4() != null)
                .count();
        setInSchoolShareSim(numInSchoolAge > 0 ? (double) numStudents / numInSchoolAge : 0.0);
        setInSchoolShareTgt(Parameters.getTargetShare(year, TargetShares.Students));

        // --- Utility adjustment factors ---
        setUtilityAdjSingleMales(Parameters.getTimeSeriesValue(year, TimeSeriesVariable.UtilityAdjustmentSingleMales));
        setUtilityAdjACMales(Parameters.getTimeSeriesValue(year, TimeSeriesVariable.UtilityAdjustmentACMales));
        setUtilityAdjSingleFemales(Parameters.getTimeSeriesValue(year, TimeSeriesVariable.UtilityAdjustmentSingleFemales));
        setUtilityAdjACFemales(Parameters.getTimeSeriesValue(year, TimeSeriesVariable.UtilityAdjustmentACFemales));
        setUtilityAdjCouples(Parameters.getTimeSeriesValue(year, TimeSeriesVariable.UtilityAdjustmentCouples));
        setUtilityAdjSingleDepMales(Parameters.getTimeSeriesValue(year, TimeSeriesVariable.UtilityAdjustmentSingleDepMen));
        setUtilityAdjSingleDepFemales(Parameters.getTimeSeriesValue(year, TimeSeriesVariable.UtilityAdjustmentSingleDepWomen));

        // --- Employment shares ---
        double[] totSM  = new double[2];  // [count, fracEmployed sum]
        double[] totSF  = new double[2];
        double[] totACM = new double[2];
        double[] totACF = new double[2];
        double[] totCou = new double[2];
        double[] totSDM = new double[2];
        double[] totSDF = new double[2];

        for (BenefitUnit bu : model.getBenefitUnits()) {
            Occupancy occ = bu.getOccupancy();
            Person male   = bu.getMale();
            Person female = bu.getFemale();
            boolean maleAtRisk   = (male   != null) && male.atRiskOfWork();
            boolean femaleAtRisk = (female != null) && female.atRiskOfWork();
            int acFlag = 0;
            if (occ == Occupancy.Single_Male   && male   != null) acFlag = male.getAdultChildFlag();
            if (occ == Occupancy.Single_Female && female != null) acFlag = female.getAdultChildFlag();

            double frac = bu.fracEmployed();

            if (occ == Occupancy.Single_Male && acFlag != 1) {
                totSM[0]++; totSM[1] += frac;
            } else if (occ == Occupancy.Single_Male && acFlag == 1) {
                totACM[0]++; totACM[1] += frac;
            } else if (occ == Occupancy.Single_Female && acFlag != 1) {
                totSF[0]++; totSF[1] += frac;
            } else if (occ == Occupancy.Single_Female && acFlag == 1) {
                totACF[0]++; totACF[1] += frac;
            } else if (occ == Occupancy.Couple && maleAtRisk && femaleAtRisk) {
                totCou[0]++; totCou[1] += frac;
            } else if (occ == Occupancy.Couple && maleAtRisk && !femaleAtRisk) {
                totSDM[0]++; totSDM[1] += frac;
            } else if (occ == Occupancy.Couple && !maleAtRisk && femaleAtRisk) {
                totSDF[0]++; totSDF[1] += frac;
            }
        }

        setEmpShareSimSingleMales(  totSM[0]  > 0 ? totSM[1]  / totSM[0]  : 0.0);
        setEmpShareSimSingleFemales(totSF[0]  > 0 ? totSF[1]  / totSF[0]  : 0.0);
        setEmpShareSimACMales(      totACM[0] > 0 ? totACM[1] / totACM[0] : 0.0);
        setEmpShareSimACFemales(    totACF[0] > 0 ? totACF[1] / totACF[0] : 0.0);
        setEmpShareSimCouples(      totCou[0] > 0 ? totCou[1] / totCou[0] : 0.0);
        setEmpShareSimSingleDepMales(  totSDM[0] > 0 ? totSDM[1] / totSDM[0] : 0.0);
        setEmpShareSimSingleDepFemales(totSDF[0] > 0 ? totSDF[1] / totSDF[0] : 0.0);

        setEmpShareTgtSingleMales(  Parameters.getTargetShare(year, TargetShares.EmploymentSingleMales));
        setEmpShareTgtSingleFemales(Parameters.getTargetShare(year, TargetShares.EmploymentSingleFemales));
        setEmpShareTgtACMales(      Parameters.getTargetShare(year, TargetShares.EmploymentACMales));
        setEmpShareTgtACFemales(    Parameters.getTargetShare(year, TargetShares.EmploymentACFemales));
        setEmpShareTgtCouples(      Parameters.getTargetShare(year, TargetShares.EmploymentCouples));
        setEmpShareTgtSingleDepMales(  Parameters.getTargetShare(year, TargetShares.EmploymentSingleDepMales));
        setEmpShareTgtSingleDepFemales(Parameters.getTargetShare(year, TargetShares.EmploymentSingleDepFemales));
    }
}
