package simpaths.model.decisions;

import simpaths.data.ManagerRegressions;
import simpaths.data.Parameters;
import simpaths.data.RegressionName;
import simpaths.data.RegressionType;
import simpaths.model.Person;
import simpaths.model.enums.Dcpst;
import simpaths.model.enums.Gender;
import simpaths.model.enums.SocialCareReceiptS2c;
import simpaths.model.enums.SocialCareReceiptState;

import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Map;

/**
 * class to manage expansion of expectations to account for each successive state variable
 */
public class ExpectationsFactory {


    double[] probability;           // array to store probability associated with each anticipated state combination, conditional on survival
    States[] anticipated;           // array to store anticipated state combinations
    Person personProxyNextPeriod;
    GridScale scale;
    int ageYearsThisPeriod;
    int ageYearsNextPeriod;
    int numberExpected;
    States currentStates;
    double pensionIncomePerYear;

    boolean flagRegionVaries;
    boolean flagEducationVaries;
    boolean flagHealthVaries;
    boolean flagDisabilityVaries;
    boolean flagSocialCareReceiptVaries;
    boolean flagSocialCareProvisionVaries;
    boolean flagCohabitationVaries;
    boolean flagChildrenVaries;


    /**
     * CONSTRUCTOR
     */
    public ExpectationsFactory(States[] anticipated, double[] probability, Person personProxyNextPeriod, GridScale scale,
                               int ageYearsThisPeriod, States currentStates, double pensionIncomePerYear) {

        this.probability = probability;
        this.anticipated = anticipated;
        numberExpected = anticipated.length;
        this.personProxyNextPeriod = personProxyNextPeriod;
        this.scale = scale;
        this.ageYearsThisPeriod = ageYearsThisPeriod;
        ageYearsNextPeriod = ageYearsThisPeriod + 1;
        this.currentStates = currentStates;
        this.pensionIncomePerYear = pensionIncomePerYear;
    }


    /**
     * GETTERS AND SETTERS
     */
    public double[] getProbability() {
        return probability;
    }
    public States[] getAnticipated() {
        return anticipated;
    }
    public int getNumberExpected() {
        return numberExpected;
    }


    /**
     * WORKERS
     */
    public void updateRegion() {

        int stateIndexCurrPeriod = scale.getIndex(Axis.Region, ageYearsThisPeriod);
        int stateIndexNextPeriod = scale.getIndex(Axis.Region, ageYearsNextPeriod);
        for (int ii = 0; ii < numberExpected; ii++) {
            anticipated[ii].states[stateIndexNextPeriod] = currentStates.states[stateIndexCurrPeriod];
        }
        personProxyNextPeriod.setRegionLocal(currentStates.getRegionCode());
    }

    public void updateRetirement(boolean retiring) {

        int stateIndexNextPeriod = scale.getIndex(Axis.Retirement, ageYearsNextPeriod);
        if (retiring) {
            // retire this period
            for (int ii = 0; ii < numberExpected; ii++) {
                anticipated[ii].states[stateIndexNextPeriod] = 1.0;
            }
        } else {
            // no change to retirement state
            for (int ii = 0; ii < numberExpected; ii++) {
                anticipated[ii].states[stateIndexNextPeriod] = currentStates.getRetirement();
            }
        }
    }

    public void updateStudent() {

        int stateIndexCurrPeriod = scale.getIndex(Axis.Student, ageYearsThisPeriod);
        int stateIndexNextPeriod = scale.getIndex(Axis.Student, ageYearsNextPeriod);
        if (anyVaries() && currentStates.getStudent()==1) {
            int numberExpectedInitial = numberExpected;
            boolean flagEval;
            LocalExpectations lexpect = new LocalExpectations();
            lexpect.evaluate(personProxyNextPeriod, RegressionName.EducationE1a);
            for (int ii=0; ii<numberExpectedInitial; ii++) {

                flagEval = updatePersonNextPeriod(ii);
                if (flagEval) {
                    lexpect = new LocalExpectations();
                    lexpect.evaluate(personProxyNextPeriod, RegressionName.EducationE1a);
                }
                expandExpectationsSingleIndex(ii, stateIndexNextPeriod, lexpect);
            }
        } else {
            LocalExpectations lexpect = new LocalExpectations();
            if (currentStates.getStudent() == 0) {
                lexpect.assignValue(currentStates.states[stateIndexCurrPeriod]);
            } else {
                lexpect.evaluate(personProxyNextPeriod, RegressionName.EducationE1a);
            }
            expandExpectationsAllIndices(stateIndexNextPeriod, lexpect);
        }
        if (currentStates.getStudent()==1)
            flagEducationVaries = true;
    }

    public void updateEducation() {

        int stateIndexCurrPeriod = scale.getIndex(Axis.Education, ageYearsThisPeriod);
        int stateIndexNextPeriod = scale.getIndex(Axis.Education, ageYearsNextPeriod);
        if (!flagEducationVaries) {
            // no change in education state possible

            LocalExpectations lexpect = new LocalExpectations();
            lexpect.assignValue(currentStates.states[stateIndexCurrPeriod]);
            expandExpectationsAllIndices(stateIndexNextPeriod, lexpect);
        } else {
            // allow for change in education state

            int numberExpectedInitial = numberExpected;
            boolean flagEval = false;
            LocalExpectations lexpect = new LocalExpectations();
            lexpect.evaluate(personProxyNextPeriod, RegressionName.EducationE2a);
            for (int ii = 0; ii < numberExpectedInitial; ii++) {

                if (anyVaries()) {
                    flagEval = updatePersonNextPeriod(ii);
                }
                if (flagEval) {
                    lexpect = new LocalExpectations();
                    lexpect.evaluate(personProxyNextPeriod, RegressionName.EducationE2a);
                }

                if (anticipated[ii].getStudent() == 1) {
                    // continuing student
                    anticipated[ii].states[stateIndexNextPeriod] = currentStates.states[stateIndexCurrPeriod];
                } else {
                    // allow for exit from education
                    expandExpectationsSingleIndex(ii, stateIndexNextPeriod, lexpect);
                }
            }
        }
    }

    public void updateChildren() {

        for (int jj = 0; jj < DecisionParams.NUMBER_BIRTH_AGES; jj++) {
            // loop over each birth age

            if (ageYearsNextPeriod >= DecisionParams.BIRTH_AGE[jj] && ageYearsNextPeriod < (DecisionParams.BIRTH_AGE[jj] + Parameters.AGE_TO_BECOME_RESPONSIBLE)) {
                // may have children from this age in next period

                if (ageYearsNextPeriod == DecisionParams.BIRTH_AGE[jj]) {
                    // next year is birth age - number of children uncertain

                    int stateIndexNextPeriod = scale.getIndex(Axis.Child, ageYearsNextPeriod, jj);
                    int options = (int)scale.axes[currentStates.ageIndex+1][stateIndexNextPeriod][0];

                    // begin loop over existing expectations
                    int numberExpectedInitial = numberExpected;
                    for (int ii=0; ii<numberExpectedInitial; ii++) {

                        // update person characteristics
                        if (anyVaries()) {
                            updatePersonNextPeriod(ii);
                        }

                        // expand expectations
                        if (Gender.Female == currentStates.getGenderCode() || anticipated[ii].getCohabitation()) {
                            // birth possible

                            if (anticipated[ii].getStudent()==1) {
                                expandExpectationsFertility(ii, stateIndexNextPeriod, jj, options, RegressionName.FertilityF1a);
                            } else {
                                expandExpectationsFertility(ii, stateIndexNextPeriod, jj, options, RegressionName.FertilityF1b);
                            }
                        } else {
                            // birth not possible

                            stateIndexNextPeriod = scale.getIndex(Axis.Child, ageYearsNextPeriod, jj);
                            for (int kk = 0; kk< numberExpected; kk++) {
                                anticipated[kk].states[stateIndexNextPeriod] = 0.0;
                            }
                        }
                    }
                    flagChildrenVaries = true;
                } else {
                    // assume next year have same number of children as this year

                    int stateIndexCurrPeriod = scale.getIndex(Axis.Child, ageYearsThisPeriod, jj);
                    int stateIndexNextPeriod = scale.getIndex(Axis.Child, ageYearsNextPeriod, jj);
                    LocalExpectations lexpect = new LocalExpectations();
                    lexpect.assignValue(currentStates.states[stateIndexCurrPeriod]);
                    expandExpectationsAllIndices(stateIndexNextPeriod, lexpect);
                }
            }
        }
    }

    public void updatePensionIncome() {

        int stateIndexNextPeriod = scale.getIndex(Axis.PensionIncome, ageYearsNextPeriod);
        int numberExpectedInitial = numberExpected;
        double val;
        for (int ii=0; ii<numberExpectedInitial; ii++) {
            val = pensionIncomePerYear;
            if (currentStates.getCohabitation() && !anticipated[ii].getCohabitation()) {
                val /= 2.0;
            } else if (!currentStates.getCohabitation() && anticipated[ii].getCohabitation()) {
                val *= 2.0;
            }
            val = Math.min( Math.max( val, 0.0 ), DecisionParams.maxPensionPYear );
            val = Math.log(val + DecisionParams.C_PENSION);
            anticipated[ii].states[stateIndexNextPeriod] = val;
        }
    }

    public void updateHealth() {
        updateCommon(Axis.Health);
        flagHealthVaries = true;
    }

    public void updateDisability() {
        updateCommon(Axis.Disability);
        flagDisabilityVaries = true;
    }

    public void updateCohabitation() {
        updateCommon(Axis.Cohabitation);
        flagCohabitationVaries = true;
    }

    public void updateSocialCareReceipt() {
        updateCommon(Axis.SocialCareReceiptState);
        flagSocialCareReceiptVaries = true;
    }

    public void updateSocialCareProvision() {
        updateCommon(Axis.SocialCareProvision);
        flagSocialCareProvisionVaries = true;
    }

    public void updateWagePotential() {
        updateCommon(Axis.WagePotential);
    }

    public void updateWageOffer1() {
        updateCommon(Axis.WageOffer1);
    }

    private void updateCommon(Axis axis) {

        // state indices
        int stateIndexNextPeriod = scale.getIndex(axis, ageYearsNextPeriod);

        // populate expectations
        LocalExpectations lexpect = null;
        if (anyVaries()) {
            boolean flagEval;
            int numberExpectedInitial = numberExpected;
            for (int ii=0; ii<numberExpectedInitial; ii++) {

                flagEval = updatePersonNextPeriod(ii);
                if (flagEval || lexpect==null) {
                    lexpect = lexpectEval(axis);
                }
                expandExpectationsSingleIndex(ii, stateIndexNextPeriod, lexpect);
            }
        } else {
            lexpect = lexpectEval(axis);
            expandExpectationsAllIndices(stateIndexNextPeriod, lexpect);
        }
    }

    private LocalExpectations lexpectEval(Axis axis) {

        LocalExpectations lexpectations = new LocalExpectations();
        if (Axis.SocialCareReceiptState.equals(axis)) {

            lexpectations = compileSocialCareReceiptProbs();
        } else if (Axis.SocialCareProvision.equals(axis)) {

            if (Dcpst.Partnered.equals(personProxyNextPeriod.getDcpst()))
                lexpectations.evaluate(personProxyNextPeriod, personProxyNextPeriod.getRegressionName(axis));
            else
                lexpectations.evaluateIndicator(personProxyNextPeriod, personProxyNextPeriod.getRegressionName(axis), 3.0);
        } else if (Axis.WagePotential.equals(axis)) {

            lexpectations.evaluateGaussian(personProxyNextPeriod, personProxyNextPeriod.getRegressionName(axis),
                    Math.log(DecisionParams.MIN_WAGE_PHOUR), Math.log(DecisionParams.MAX_WAGE_PHOUR), DecisionParams.C_WAGE_POTENTIAL);
        } else {

            lexpectations.evaluate(personProxyNextPeriod, personProxyNextPeriod.getRegressionName(axis));
        }
        return lexpectations;
    }

    private LocalExpectations compileSocialCareReceiptProbs() {

        // raw inputs
        double probNeedCare = Parameters.getRegNeedCareS2a().getProbability(personProxyNextPeriod, Person.DoublesVariables.class);
        double probRecCare = Parameters.getRegReceiveCareS2b().getProbability(personProxyNextPeriod, Person.DoublesVariables.class);
        Map<SocialCareReceiptS2c,Double> probsCareFrom = Parameters.getRegSocialCareMarketS2c().getProbabilites(personProxyNextPeriod, Person.DoublesVariables.class);

        // compile and package outputs
        int ii = 0;
        double probHere, probCheck = 0.0;
        double[] probs = new double[SocialCareReceiptState.values().length];
        double[] vals = new double[SocialCareReceiptState.values().length];

        // no care needed (may work; otherwise work may be limited)
        probHere = 1.0 - probNeedCare;
        probs[ii] = probHere;
        vals[ii] = (double)SocialCareReceiptState.NoneNeeded.getValue();
        probCheck += probHere;
        ii++;

        // no formal care
        probHere = probNeedCare * ((1.0 - probRecCare) + probRecCare * probsCareFrom.get(SocialCareReceiptS2c.Informal));
        probs[ii] = probHere;
        vals[ii] = (double)SocialCareReceiptState.NoFormal.getValue();
        probCheck += probHere;
        ii++;

        // mixed care
        probHere = probNeedCare * probRecCare * probsCareFrom.get(SocialCareReceiptS2c.Mixed);
        probs[ii] = probHere;
        vals[ii] = (double)SocialCareReceiptState.Mixed.getValue();
        probCheck += probHere;
        ii++;

        // formal care
        probHere = probNeedCare * probRecCare * probsCareFrom.get(SocialCareReceiptS2c.Formal);
        probs[ii] = probHere;
        vals[ii] = (double)SocialCareReceiptState.Formal.getValue();
        probCheck += probHere;
        ii++;

        // check results
        if (Math.abs(probCheck-1.0)>1.0E-5)
            throw new RuntimeException("problem evaluating probabilities for social care receipt");

        // return
        LocalExpectations localExpectations = new LocalExpectations();
        localExpectations.screenAndAssign(probs, vals);
        return localExpectations;
    }

    private boolean anyVaries() {
        if ( flagRegionVaries || flagEducationVaries || flagHealthVaries || flagDisabilityVaries || flagSocialCareReceiptVaries ||
                flagSocialCareProvisionVaries || flagCohabitationVaries || flagChildrenVaries )
            return true;
        else
            return false;
    }

    private boolean updatePersonNextPeriod(int ii) {

        boolean flagEval, flagChange;
        flagEval = false;
        if (flagRegionVaries) {
            flagChange = updatePersonNextPeriod(anticipated[ii], Axis.Region);
            if (flagChange) flagEval = true;
        }
        if (flagEducationVaries) {
            flagChange = updatePersonNextPeriod(anticipated[ii], Axis.Education);
            if (flagChange) flagEval = true;
        }
        if (flagHealthVaries) {
            flagChange = updatePersonNextPeriod(anticipated[ii], Axis.Health);
            if (flagChange) flagEval = true;
        }
        if (flagDisabilityVaries) {
            flagChange = updatePersonNextPeriod(anticipated[ii], Axis.Disability);
            if (flagChange) flagEval = true;
        }
        if (flagSocialCareReceiptVaries) {
            flagChange = updatePersonNextPeriod(anticipated[ii], Axis.SocialCareReceiptState);
            if (flagChange) flagEval = true;
        }
        if (flagSocialCareProvisionVaries) {
            flagChange = updatePersonNextPeriod(anticipated[ii], Axis.SocialCareProvision);
            if (flagChange) flagEval = true;
        }
        if (flagCohabitationVaries) {
            flagChange = updatePersonNextPeriod(anticipated[ii], Axis.Cohabitation);
            if (flagChange) flagEval = true;
        }
        if (flagChildrenVaries) {
            flagChange = updatePersonNextPeriod(anticipated[ii], Axis.Child);
            if (flagChange) flagEval = true;
        }
        return flagEval;
    }

    private boolean updatePersonNextPeriod(States states, Axis axis) {
        boolean changed = false;
        Object val0, val1;
        if (Axis.Region.equals(axis)) {
            val0 = personProxyNextPeriod.getRegion();
            val1 = states.getRegionCode();
        } else if (Axis.Education.equals(axis)) {
            val0 = personProxyNextPeriod.getDeh_c3();
            val1 = states.getEducationCode();
            if (val0==val1) {
                val0 = personProxyNextPeriod.getDed();
                val1 = states.getStudentIndicator();
            }
        } else if (Axis.Health.equals(axis)) {
            val0 = personProxyNextPeriod.getDhe();
            val1 = states.getHealthCode();
        } else if (Axis.Disability.equals(axis)) {
            val0 = personProxyNextPeriod.getDlltsd();
            val1 = states.getDlltsd();
        } else if (Axis.SocialCareReceiptState.equals(axis)) {
            val0 = personProxyNextPeriod.getSocialCareReceipt();
            val1 = states.getSocialCareReceiptCode();
        } else if (Axis.SocialCareProvision.equals(axis)) {
            val0 = personProxyNextPeriod.getSocialCareProvision();
            val1 = states.getSocialCareProvisionCode();
        } else if (Axis.Cohabitation.equals(axis)) {
            val0 = personProxyNextPeriod.getDcpst();
            val1 = states.getDcpst();
        } else if (Axis.Child.equals(axis)) {
            val0 = personProxyNextPeriod.getNumberChildren017Local();
            val1 = states.getChildren017();
            if (val0==val1) {
                val0 = personProxyNextPeriod.getIndicatorChildren02Local();
                val1 = states.getChildrenUnder3Indicator();
            }
            if (val0==val1) {
                val0 = personProxyNextPeriod.getNumberChildrenAllLocal();
                val1 = states.getChildren017();
            }
        } else {
            throw new RuntimeException("unrecognised axis for considering change in person proxy states");
        }
        if (val0!=val1) {
            changed = true;

            if (Axis.Region.equals(axis)) {
                personProxyNextPeriod.setRegion(states.getRegionCode());
            } else if (Axis.Education.equals(axis)) {
                personProxyNextPeriod.setDeh_c3(states.getEducationCode());
                personProxyNextPeriod.setDed(states.getStudentIndicator());
            } else if (Axis.Health.equals(axis)) {
                personProxyNextPeriod.setDhe(states.getHealthCode());
            } else if (Axis.Disability.equals(axis)) {
                personProxyNextPeriod.setDlltsd(states.getDlltsd());
            } else if (Axis.SocialCareReceiptState.equals(axis)) {
                personProxyNextPeriod.setSocialCareReceipt(states.getSocialCareReceiptCode());
            } else if (Axis.SocialCareProvision.equals(axis)) {
                personProxyNextPeriod.setSocialCareProvision(states.getSocialCareProvisionCode());
            } else if (Axis.Cohabitation.equals(axis)) {
                personProxyNextPeriod.setDcpstLocal(states.getDcpst());
            } else if (Axis.Child.equals(axis)) {
                personProxyNextPeriod.setNumberChildren017Local(states.getChildren017());
                personProxyNextPeriod.setIndicatorChildren02Local(states.getChildrenUnder3Indicator());
                personProxyNextPeriod.setNumberChildrenAllLocal(states.getChildren017());
            }
        }
        return changed;
    }

    private void expandExpectationsSingleIndex(int expandIndex, int stateIndex, LocalExpectations lexpect) {
        expandExpectationsSingleIndex(expandIndex, stateIndex, lexpect.probabilities, lexpect.values);
    }

    private void expandExpectationsSingleIndex(int expandIndex, int stateIndex, double[] probabilities, double[] values) {

        // expand expectations array
        if (probabilities.length > 1) {
            probability = Arrays.copyOf(probability, numberExpected + probabilities.length - 1);
            anticipated = Arrays.copyOf(anticipated, numberExpected + probabilities.length - 1);
            for (int ii=0; ii<probabilities.length-1; ii++) {
                probability[numberExpected +ii] = probability[expandIndex];
                anticipated[numberExpected +ii] = new States(anticipated[expandIndex]);
            }
        }

        // update expectations arrays
        double probabilityCheck = 0.0;
        for (int ii=probabilities.length-1; ii>=0; ii--) {
            probabilityCheck += probabilities[ii];
            if (ii>0) {
                probability[numberExpected - 1 + ii] = probability[numberExpected - 1 + ii] * probabilities[ii];
                anticipated[numberExpected - 1 + ii].states[stateIndex] = values[ii];
            } else {
                probability[expandIndex] = probability[expandIndex] * probabilities[ii];
                anticipated[expandIndex].states[stateIndex] = values[ii];
            }
        }

        // check supplied probabilities
        if (Math.abs(probabilityCheck-1) > 1.0E-5) {
            throw new InvalidParameterException("problem with probabilities supplied to outer expectations 2");
        }

        // update indices
        numberExpected = numberExpected + probabilities.length - 1;
    }

    /**
     * METHOD TO EXPAND EXPECTATIONS ARRAYS TO ALLOW FOR FERTILITY BIRTH YEARS
     * @param expandIndex the index of the anticipated array taken as a starting point
     * @param stateIndex the state index for the respective birth year
     * @param birthYear the current birth year (e.g. 0, 1 or 2)
     * @param options the number of potential alternatives at birth age (= max no. births + 1)
     * @param regression the regression equation used to update probabilities
     */
    private void expandExpectationsFertility(int expandIndex, int stateIndex, int birthYear, int options, RegressionName regression) {

        // initialise storage arrays - 100% probability to zero children at birth year
        double[] probabilities = new double[options];
        double[] values = new double[options];
        for (int ii=0; ii<options; ii++) {
            if (ii==0) {
                probabilities[ii] = 1.0;
            } else {
                probabilities[ii] = 0.0;
            }
            values[ii] = ii;
        }

        // identify age pool for birth year
        int[] ageVector = currentStates.getFertilityAgeBand(birthYear);
        int age0 = ageVector[0];
        int age1 = ageVector[1];

        // evaluate probabilities
        int childrenAll = currentStates.getChildrenAll();
        int children02 = currentStates.getChildren02();

        // loop over age pool for birth year
        for (int age=age0; age<=age1; age++) {

            personProxyNextPeriod.setDag(age);
            // at each age in the pool, data for n+1 births are a flow from n births
            // loop consequently works in reverse order through number of births, starting
            // at the pen-ultimate group (as flows from upper bound are ignored)
            for (int ii=options-2; ii>=0; ii--) {

                // ii = number of previous births for this birth age
                int birthsHere02 = Math.min(ii + children02, 2);  // assume at most 2 children under 3
                personProxyNextPeriod.setNumberChildrenAllLocal_lag1(childrenAll + ii);
                personProxyNextPeriod.setNumberChildren02Local_lag1(birthsHere02);
                double proportionBirths = ManagerRegressions.getProbability(personProxyNextPeriod, regression);
                probabilities[ii+1] += probabilities[ii] * proportionBirths;
                probabilities[ii] *= (1 - proportionBirths);
            }
        }

        // expand expectations array
        expandExpectationsSingleIndex(expandIndex, stateIndex, probabilities, values);

        // restore benefitUnit and person characteristics
        personProxyNextPeriod.setDag(ageYearsNextPeriod);
        personProxyNextPeriod.setNumberChildrenAllLocal_lag1(childrenAll);
        personProxyNextPeriod.setNumberChildren02Local_lag1(children02);
    }

    private void expandExpectationsAllIndices(int stateIndex, LocalExpectations lexpect) {
        int numberExpectedInitial = numberExpected;
        for (int ii=0; ii<numberExpectedInitial; ii++) {
            expandExpectationsSingleIndex(ii, stateIndex, lexpect);
        }
    }
}
