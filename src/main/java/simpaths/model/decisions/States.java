package simpaths.model.decisions;

import java.security.InvalidParameterException;

import simpaths.data.Parameters;
import simpaths.model.Person;
import simpaths.model.enums.*;
import simpaths.model.BenefitUnit;


/**
 *
 * CLASS TO DEFINE A GIVEN COMBINATION OF STATE VARIABLES
 *
 */
public class States {


    final double eps = 1.0E-10;
    int ageIndex;           // age index for state
    int ageYears;           // age in years for state
    double[] states;        // vector to store combination of state variables (except age), in order as defined for axes in Grids
    GridScale scale;        // dimensional specifications of array used to store states


    public States(GridScale scale, int ageYears) {

        // initialise object attributes
        this.scale = scale;
        this.ageYears = ageYears;
        ageIndex = ageYears - Parameters.AGE_TO_BECOME_RESPONSIBLE;
        if (ageIndex == scale.simLifeSpan) {
            states = new double[1];
        } else {
            states = new double[(int)(scale.gridDimensions[ageIndex][4]+scale.gridDimensions[ageIndex][5])];
        }
    }

    public States(States originalStates) {

        // initialise copy
        ageIndex = originalStates.ageIndex;
        ageYears = originalStates.ageYears;
        states = new double[originalStates.states.length];
        System.arraycopy(originalStates.states, 0, states, 0, originalStates.states.length);
        scale = originalStates.scale;
    }

    public States(BenefitUnit benefitUnit, GridScale scale) {

        this.scale = scale;

        // initialise state vector and working variables
        Person refPerson = benefitUnit.getRefPersonForDecisions();
        ageYears = Math.min(refPerson.getDag(), DecisionParams.maxAge);
        ageIndex = ageYears - Parameters.AGE_TO_BECOME_RESPONSIBLE;
        if (ageIndex == scale.simLifeSpan) {
            states = new double[1];
        } else {
            states = new double[(int)(scale.gridDimensions[ageIndex][4]+scale.gridDimensions[ageIndex][5])];
        }

        // populate states vector

        // populate wealth
        double val;
        val = Math.min(Math.max(benefitUnit.getLiquidWealth(), DecisionParams.getMinWealthByAge(ageYears)), DecisionParams.getMaxWealthByAge(ageYears));
        val = Math.log(val + DecisionParams.C_LIQUID_WEALTH);
        populate(Axis.LiquidWealth, val);

        // populate full-time wage potential
        if (ageYears <= DecisionParams.maxAgeFlexibleLabourSupply) {

            val = Math.min(Math.max(refPerson.getFullTimeHourlyEarningsPotential(), DecisionParams.MIN_WAGE_PHOUR), DecisionParams.MAX_WAGE_PHOUR);
            val = Math.log(val + DecisionParams.C_WAGE_POTENTIAL);
            populate(Axis.WagePotential, val);
        }

        // private pension
        if (DecisionParams.flagPrivatePension && ageYears > DecisionParams.minAgeToRetire) {
            val = Math.min(benefitUnit.getPensionIncomeAnnual(), DecisionParams.maxPensionPYear);
            val = Math.log(val + DecisionParams.C_PENSION);
            populate(Axis.PensionIncome, val);
        }

        // birth year
        populate(Axis.BirthYear, benefitUnit.getYear() - ageYears);

        // wage offer 1
        if (ageYears <= DecisionParams.maxAgeFlexibleLabourSupply && DecisionParams.flagLowWageOffer1)
            populate(Axis.WageOffer1, refPerson.getWageOffer());

        // retirement
        if (DecisionParams.flagRetirement && ageYears > DecisionParams.minAgeToRetire && ageYears <= DecisionParams.maxAgeFlexibleLabourSupply)
            populate(Axis.Retirement, refPerson.getRetired());

        // populate health
        if ( DecisionParams.flagHealth && ageYears >= DecisionParams.minAgeForPoorHealth )
            populate(Axis.Health, Math.min(Math.max(benefitUnit.getHealthValForBehaviour(), DecisionParams.MIN_HEALTH), DecisionParams.MAX_HEALTH));

        // disability
        if (DecisionParams.flagDisability && ageYears >= DecisionParams.minAgeForPoorHealth && ageYears <= DecisionParams.maxAgeForDisability())
            populate(Axis.Disability, (double)refPerson.getDisability());

        // social care receipt
        if (Parameters.flagSocialCare && ageYears >= DecisionParams.minAgeReceiveFormalCare)
            populate(Axis.SocialCareReceiptState, (double)refPerson.getSocialCareReceiptState().getValue());

        // social care provision
        if (Parameters.flagSocialCare)
            populate(Axis.SocialCareProvision, refPerson.getSocialCareProvisionState());

        // region
        if ( DecisionParams.flagRegion )
            populate(Axis.Region, benefitUnit.getRegionIndex());

        // student
        if (ageYears <= Parameters.MAX_AGE_TO_LEAVE_CONTINUOUS_EDUCATION && DecisionParams.flagEducation)
            populate(Axis.Student, refPerson.getStudent());

        // education
        if ( DecisionParams.flagEducation )
            populate(Axis.Education, refPerson.getEducation());

        // children
        int[] children;
        if (benefitUnit.getFemale() != null) {
            children = getChildrenByBirthAge(benefitUnit.getFemale());
        } else {
            children = getChildrenByBirthAge(benefitUnit.getMale());
        }
        for (int ii=0; ii<DecisionParams.NUMBER_BIRTH_AGES; ii++) {
            populate(Axis.Child, ii, children[ii]);
        }

        // cohabitation
        populate(Axis.Cohabitation, benefitUnit.getCoupleDummy());

        // gender
        populate(Axis.Gender, refPerson.getGender());
    }

    private void populate(Enum<?> axisID, double val) {
        populate(axisID, 0, val);
    }
    private void populate(Enum<?> axisID, int offset, double val) {

        int index = this.scale.getIndex(axisID, ageYears);
        if ( index >= 0 ) {

            if (index+offset>=this.states.length || index+offset<0) throw new InvalidParameterException("Attempt to populate state outside vector");

            // limit to within bounds
            if (val > scale.axes[ageIndex][index+offset][2] + eps) {
                val = scale.axes[ageIndex][index+offset][2];
            } else if (val < scale.axes[ageIndex][index+offset][1] - eps) {
                val = scale.axes[ageIndex][index+offset][1];
            }

            // set states value
            this.states[index+offset] = val;
        }
    }


    /*
     * WORKER METHODS
     */

    /**
     * METHOD TO EVALUATE GRID INDEX FOR GIVEN SET OF STATE CHARACTERISTICS
     * IF STATE IS NOT PRECISELY ON GRID INDEX, THEN THE NEAREST LOWER INTEGER IS RETURNED
     * @return grid index
     */
    public long returnGridIndex() {
        return returnAgeSpecificIndex() + scale.gridDimensions[ageIndex][3];
    }

    /**
     * METHOD TO EVALUATE AGE SPECIFIC INDEX FOR GIVEN SET OF STATE CHARACTERISTICS
     * IF STATE IS NOT PRECISELY ON GRID INDEX, THEN THE NEAREST LOWER INTEGER IS RETURNED
     * @return grid index
     */
    public long returnAgeSpecificIndex() {

        // working variables
        long index;
        int iiCounter;
        double iiIndex;

        // work through age-specific states
        index = 0;
        iiCounter = 1;
        for (int ii = 0; ii < (int)(scale.gridDimensions[ageIndex][4] + scale.gridDimensions[ageIndex][5]); ii++) {
            if (states[ii] > scale.axes[ageIndex][ii][2] + eps) {
                systemReportError();
                throw new InvalidParameterException("call to interpolate state above grid maximum");
            } else if (states[ii] < scale.axes[ageIndex][ii][1] - eps) {
                systemReportError();
                throw new InvalidParameterException("call to interpolate state under grid minimum");
            } else {
                iiIndex = (states[ii] - scale.axes[ageIndex][ii][1]) /
                        (scale.axes[ageIndex][ii][2] - scale.axes[ageIndex][ii][1]) *
                        (scale.axes[ageIndex][ii][0] - 1.0);
                index += iiCounter * (long)(iiIndex+eps);
            }
            iiCounter *= (int)scale.axes[ageIndex][ii][0];
        }

        // return result
        return index;
    }

    /**
     * METHOD TO POPULATE STATE VALUES FOR OUTER GRID LOOP IN ManagerSolveGrids
     * @param iiOuter outer state index, based on grid axis structure - see Grids Constructor for related detail
     */
    public void populateOuterGridStates(int iiOuter) {

        // working variables
        int noInnerStates = (int)scale.gridDimensions[ageIndex][4];
        int noOuterStates = (int)scale.gridDimensions[ageIndex][5];
        double xxMin, xxMax, xxStep;
        int[] base = new int[noOuterStates];

        // evaluate base
        for (int ii = 0; ii < noOuterStates; ii++) {
            base[ii] = (int) Math.round(scale.axes[ageIndex][noInnerStates + ii][0]);
        }

        // evaluate counters
        int[] counters = counterEvaluate(iiOuter, base);

        // evaluate state values
        for (int ii = 0; ii < noOuterStates; ii++) {
            xxMin = scale.axes[ageIndex][noInnerStates + ii][1];
            xxMax = scale.axes[ageIndex][noInnerStates + ii][2];
            if (base[ii] > 1) {
                xxStep = (xxMax - xxMin) / (base[ii] - 1);
            } else {
                xxStep = 0;
            }
            states[noInnerStates + ii] = xxMin + xxStep * counters[ii];
        }
    }

    /**
     * METHOD TO POPULATE STATE VALUES FOR INNER GRID LOOP IN ManagerSolveGrids
     * @param iiInner outer state index, based on grid axis structure - see Grids Constructor for related detail
     */
    public void populateInnerGridStates(int iiInner) {

        // working variables
        int noInnerStates = (int)scale.gridDimensions[ageIndex][4];
        double xxxMin, xxMax, xxStep;
        int[] base = new int[noInnerStates];

        // evaluate base
        for (int ii = 0; ii < noInnerStates; ii++) {
            base[ii] = (int) Math.round(scale.axes[ageIndex][ii][0]);
        }

        // evaluate counters
        int[] counters = counterEvaluate(iiInner, base);

        // evaluate state values
        for (int ii = 0; ii < noInnerStates; ii++) {
            xxxMin = scale.axes[ageIndex][ii][1];
            xxMax = scale.axes[ageIndex][ii][2];
            if (base[ii] > 1) {
                xxStep = (xxMax - xxxMin) / (base[ii] - 1);
            } else {
                xxStep = 0;
            }
            states[ii] = xxxMin + xxStep * counters[ii];
        }
    }

    /**
     * SETTER METHODS FOR POPULATING states VECTOR
     */
    public void setStates(Axis axisID, double val) {
        int axis = scale.getIndex(axisID, ageYears);
        states[axis] = val;
    }

    public void setWageOffer2(double val) {

        states[scale.getIndex(Axis.WageOffer2, ageYears)] = val;
    }

    public void setWageOffer1(double val) { states[scale.getIndex(Axis.WageOffer1, ageYears)] = val; }

    /**
     * METHOD TO DISAGGREGATE REFERENCE INDEX INTO STATE SPECIFIC COUNTERS
     * @param index reference index
     * @param base  array defining the number of indices used to describe each state
     * @return integer array of state specific counters
     */
    static int[] counterEvaluate(int index, int[] base) {

        // initialise return
        int[] counters = new int[base.length];

        // evaluate counter
        int residual = index;
        for (int ii = 0; ii < base.length; ii++) {

            counters[ii] = residual % base[ii];
            residual = (residual - counters[ii]) / base[ii];
        }

        // return result
        return counters;
    }

    /**
     * METHOD TO VALIDATE WHETHER STATE COMBINATION SHOULD BE PASSED TO NUMERICAL OPTIMISATION ROUTINES
     * @return boolean, true if the state combination should be considered
     */
    boolean checkOuterStateCombination() {

        // initialise return
        boolean loopConsider = true;

        // check wage offer
        int wageOffer = getWageOffer();
        if (wageOffer == 0) {

            // skip if no wage offer is received, as numerical solution for this state combination is identical to
            // one of the labour options in the respective state combination where a wage offer is received
            loopConsider = false;
        }

        // check care provision
        if (!getCohabitation() &&
                ( SocialCareProvision.OnlyPartner.equals(getSocialCareProvisionCode()) ||
                        SocialCareProvision.PartnerAndOther.equals(getSocialCareProvisionCode()) ))
            loopConsider = false;

        // return result
        return loopConsider;
    }

    boolean checkStateCombination() {

        // initialise return
        boolean loopConsider = true;

        // check retirement/pension combination
        if (getRetirement()==0 && getPensionPerYear()>0) {
            loopConsider = false;
        }

        // return result
        return loopConsider;
    }


    /**
     * METHOD TO EXTRACT RETIREMENT STATE FROM STATES ARRAY
     * @return the retirement state if during working lifetime, and -1 otherwise
     */
    int getRetirement() {
        int retirement = 0;
        if (ageYears > DecisionParams.minAgeToRetire) {
            if (DecisionParams.flagRetirement) {
                if (ageYears <= DecisionParams.maxAgeFlexibleLabourSupply) {
                    retirement = (int) Math.round(states[scale.getIndex(Axis.Retirement, ageYears)]);
                } else {
                    retirement = 1;
                }
            } else if (DecisionParams.flagPrivatePension) {
                retirement = 1;
            }
        }
        return retirement;
    }

    public double getPensionPerYear() {

        if (DecisionParams.flagPrivatePension && ageYears > DecisionParams.minAgeToRetire) {
            return Math.exp(states[scale.getIndex(Axis.PensionIncome, ageYears)]) - DecisionParams.C_PENSION;
        } else {
            return 0.0;
        }
    }


    /**
     * METHOD TO EXTRACT WAGE OFFER STATE FROM STATES ARRAY
     * @return the wage offer state if during working lifetime, and -1 otherwise
     */
    int getWageOffer() {
        int wageOffer;
        if (ageYears <= DecisionParams.maxAgeFlexibleLabourSupply && DecisionParams.flagLowWageOffer1) {
            wageOffer = (int) Math.round(states[scale.getIndex(Axis.WageOffer1, ageYears)]);
        } else {
            wageOffer = -1;
        }
        return wageOffer;
    }

    /**
     * METHOD TO EXTRACT COHABITATION STATE FROM STATES ARRAY
     */
    int getCohabitationIndex() {
        if (ageYears <= DecisionParams.MAX_AGE_COHABITATION) {
            return (int)Math.round(states[scale.getIndex(Axis.Cohabitation, ageYears)]);
        } else {
            return 0;
        }
    }
    boolean getCohabitation() {
        return getCohabitationIndex() == 1;
    }
    Dcpst getDcpst() {
        return (getCohabitation()) ? Dcpst.Partnered : Dcpst.SingleNeverMarried;
    }

    /**
     * METHOD TO EXTRACT NUMBERS AND AGES OF DEPENDENT CHILDREN FROM STATES ARRAY
     * @return 2D integer array
     * first column reports (notional) age of children from respective birth age
     * second column reports number of dependent children in benefitUnit from respective birth age
     */
    int getChildrenByBirthIndex(int ii) {
        int index = scale.getIndex(Axis.Child,ageYears,ii);
        if (index>=0) {
            return (int)Math.round(states[index]);
        } else {
            return 0;
        }
    }
    int[][] getChildrenByAge() {

        // initialise return
        int[][] children = new int[DecisionParams.NUMBER_BIRTH_AGES][2];

        // evaluate return
        int dimIndex = states.length - 3;
        for (int ii = DecisionParams.NUMBER_BIRTH_AGES - 1; ii >= 0; ii--) {
            children[ii][0] = ageYears - DecisionParams.BIRTH_AGE[ii];
            if ((ageYears >= DecisionParams.BIRTH_AGE[ii]) && (ageYears < (DecisionParams.BIRTH_AGE[ii] + Parameters.AGE_TO_BECOME_RESPONSIBLE))) {
                children[ii][1] = (int) Math.round(states[dimIndex]);
                dimIndex--;
            } else {
                children[ii][1] = 0;
            }
        }

        // return result
        return children;
    }

    public int getChildrenByAge(int age) {

        int[][] allChildren = getChildrenByAge();
        for (int ii=0; ii<DecisionParams.NUMBER_BIRTH_AGES; ii++) {
            if (allChildren[ii][0] == age) {
                return allChildren[ii][1];
            }
        }
        return 0;
    }

    /**
     * METHOD TO IDENTIFY WHEN CHILDCARE COSTS MIGHT APPLY
     */
    public boolean hasChildrenEligibleForCare() {

        int dimIndex = states.length - 3;
        for (int ii = DecisionParams.NUMBER_BIRTH_AGES - 1; ii >= 0; ii--) {

            int childAge = ageYears - DecisionParams.BIRTH_AGE[ii];
            if ((childAge >= 0) && (childAge < Parameters.AGE_TO_BECOME_RESPONSIBLE)) {

                int childNbr = (int) Math.round(states[dimIndex]);
                if (childNbr > 0 && childAge <= Parameters.MAX_CHILD_AGE_FOR_FORMAL_CARE) {
                    return true;
                }
                dimIndex--;
            }
        }
        return false;
    }


    /**
     * METHOD TO GET THE TOTAL NUMBER OF DEPENDENT CHILDREN IN HOUSEHOLD
     * @return integer - number of all children
     */
    int getChildrenAll() {
        int numberChildren = 0;
        int[][] children = getChildrenByAge();
        for (int ii = 0; ii< DecisionParams.NUMBER_BIRTH_AGES; ii++) {
            numberChildren += children[ii][1];
        }

        return numberChildren;
    }

    /**
     * METHOD TO GET THE NUMBER OF DEPENDENT CHILDREN AGED UNDER 3 IN HOUSEHOLD
     * @return integer - number of all children
     */
    int getChildren02() {
        int[][] children = getChildrenByAge();
        int children02 = 0;
        for (int[] ints : children) {
            if (ints[0] >= 0 && ints[0] <= 2) {
                children02 += ints[1];
            }
        }
        return children02;
    }

    /**
     * METHOD TO GET THE NUMBER OF DEPENDENT CHILDREN AGED UNDER 5 IN HOUSEHOLD
     * @return integer - number of all children
     */
    int getChildren04() {
        int[][] children = getChildrenByAge();
        int children04 = 0;
        for (int[] ints : children) {
            if (ints[0] >= 0 && ints[0] <= 4) {
                children04 += ints[1];
            }
        }
        return children04;
    }
    int getChildren59() {
        int[][] children = getChildrenByAge();
        int children59 = 0;
        for (int[] ints : children) {
            if (ints[0] >= 5 && ints[0] <= 9) {
                children59 += ints[1];
            }
        }
        return children59;
    }
    int getChildren1017() {
        int[][] children = getChildrenByAge();
        int children1017 = 0;
        for (int[] ints : children) {
            if (ints[0] >= 10 && ints[0] <= 17) {
                children1017 += ints[1];
            }
        }
        return children1017;
    }

    /**
     * METHOD TO GET THE NUMBER OF DEPENDENT CHILDREN AGED UNDER 5 IN HOUSEHOLD
     * @return integer - number of all children
     */
    int getChildren5p() {
        int[][] children = getChildrenByAge();
        int children5p = 0;
        for (int[] ints : children) {
            if (ints[0] >= 5) {
                children5p += ints[1];
            }
        }
        return children5p;
    }

    /**
     * METHOD TO GET THE NUMBER OF DEPENDENT CHILDREN AGED UNDER 18 IN HOUSEHOLD
     * @return integer - number of all children
     */
    int getChildren017() {
        int[][] children = getChildrenByAge();
        int children02 = 0;
        for (int[] ints : children) {
            if (ints[0] >= 0 && ints[0] <= 17) {
                children02 += ints[1];
            }
        }
        return children02;
    }

    /**
     * METHOD TO RETURN YEAR IMPLIED BY STATE COMBINATION
     * @return integer
     */
    int getYear() { return ageYears + getBirthYear(); }
    int getYearByAge(int ageYearsH) {return ageYearsH + getBirthYear(); }
    int getBirthYear() {
        return (int) states[scale.getIndex(Axis.BirthYear, ageYears)];
    }

    /**
     * METHOD TO RETURN YEAR IMPLIED BY STATE COMBINATION
     * @return integer
     */
    public int getAgeYears() { return ageYears; }

    /**
     * METHOD TO RETURN GENDER OF REFERENCE PERSON IMPLIED BY STATE COMBINATION
     * @return integer (0=male, 1=female)
     */
    int getGender() { return (int)states[scale.getIndex(Axis.Gender, ageYears)]; }

    /**
     * METHOD TO RETURN GEOGRAPHIC REGION IMPLIED BY STATE COMBINATION
     * @return integer
     */
    int getRegion() { return (int)states[scale.getIndex(Axis.Region, ageYears)]; }

    /**
     * METHOD TO RETURN DISABILITY STATUS IMPLIED BY STATE COMBINATION
     * @return integer (0 not disabled, 1 disabled)
     */
    int getDisability() {
        if (DecisionParams.flagDisability && ageYears >= DecisionParams.minAgeForPoorHealth && ageYears <= DecisionParams.maxAgeForDisability()) {
            return (int)states[scale.getIndex(Axis.Disability, ageYears)];
        } else {
            return 0;
        }
    }

    /**
     * METHOD TO RETURN SOCIAL CARE RECEIPT
     * @return integer (0 none needed, 1 no formal (needed but not received or only informal), 2 formal and informal, 3 only formal
     */
    int getSocialCareReceiptState() {
        if (Parameters.flagSocialCare && ageYears >= DecisionParams.minAgeReceiveFormalCare) {
            return (int)states[scale.getIndex(Axis.SocialCareReceiptState, ageYears)];
        } else {
            return 0;
        }
    }

    /**
     * METHOD TO INDICATE IF PRINCIPAL IS ELIGIBLE FOR WORK
     * @return boolean
     */
    public boolean getPrincipalEligibleForWork() {
        if (!Parameters.flagSuppressSocialCareCosts) {
            if (getDisability()==1)
                return false;
            if (getSocialCareReceiptState()>0)
                return false;
        }
        return true;
    }

    /**
     * METHOD TO RETURN SOCIAL CARE PROVISION
     * @return integer (0 no care, 1 only to partner, 2 to partner and other, 3 only to other
     */
    int getSocialCareProvision() {
        if (Parameters.flagSocialCare) {
            return (int)states[scale.getIndex(Axis.SocialCareProvision, ageYears)];
        } else {
            return 0;
        }
    }

    /**
     * METHOD TO RETURN EDUCATION STATUS IMPLIED BY STATE COMBINATION
     * @return integer
     */
    int getEducation() {
        if (DecisionParams.flagEducation) {
            return (int)states[scale.getIndex(Axis.Education, ageYears)];
        } else {
            return 0;
        }
    }

    /**
     * METHOD TO RETURN EDUCATION STATUS IMPLIED BY STATE COMBINATION
     * @return integer
     */
    int getStudent() {
        int student = 0;
        if (ageYears <= Parameters.MAX_AGE_TO_LEAVE_CONTINUOUS_EDUCATION && DecisionParams.flagEducation) {
            student = (int)states[scale.getIndex(Axis.Student, ageYears)];
        }
        return student;
    }

    Indicator getStudentIndicator() {
        return (getStudent()==1) ? Indicator.True : Indicator.False;
    }

    double getVal(Axis ee) {
        return states[scale.getIndex(ee, ageYears)];
    }

    int getHealthVal() {
        if (DecisionParams.flagHealth && ageYears >= DecisionParams.minAgeForPoorHealth) {
            return (int)states[scale.getIndex(Axis.Health, ageYears)];
        } else {
            return 0;
        }
    }
    double getLiquidWealth() { return Math.exp(states[scale.getIndex(Axis.LiquidWealth, ageYears)]) - DecisionParams.C_LIQUID_WEALTH; }

    double getFullTimeHourlyEarningsPotential() {
        if (ageYears <= DecisionParams.maxAgeFlexibleLabourSupply) {
            return Math.exp(states[scale.getIndex(Axis.WagePotential, ageYears)]) - DecisionParams.C_WAGE_POTENTIAL;
        } else {
            return 0.0;
        }
    }


    /**
     * METHOD TO EVALUATE OECD SCALE FROM STATES ARRAY
     * @return the evaulated scale
     */
    double oecdEquivalenceScale() {

        // initialise return
        double scale = 1;

        // evaluate return
        if (getCohabitation()) {
            scale += 0.5;
        }
        int[][] children = getChildrenByAge();
        for (int ii = 0; ii < DecisionParams.NUMBER_BIRTH_AGES; ii++) {
            if (children[ii][1] > 0) {
                if (children[ii][0] > 13) {
                    scale += 0.5 * (double) children[ii][1];
                } else {
                    scale += 0.3 * (double) children[ii][1];
                }
            }
        }

        // evaluate return
        return scale;
    }

    /**
     * METHOD TO IDENTIFY Gender enum CODE USED BY SimPaths IMPLIED BY STATE COMBINATION
     * @return Gender
     */
    Gender getGenderCode() {
        Gender code;
        if (getGender()==0) {
            code = Gender.Male;
        } else {
            code = Gender.Female;
        }
        return code;
    }

    /**
     * METHOD TO IDENTIFY Region enum CODE USED BY SimPaths IMPLIED BY STATE COMBINATION
     * @return Region
     */
    Region getRegionCode() {
        Region regionCode = null;
        if (DecisionParams.flagRegion) {
            int regionId = getRegion();
            for (Region code : Region.values()) {
                if (code.getValue()==regionId) {
                    regionCode = code;
                }
            }
        }
        if (regionCode == null) {
            regionCode = DecisionParams.DEFAULT_REGION;
        }
        return regionCode;
    }

    /**
     * METHOD TO IDENTIFY Dhhtp_c4 enum CODE USED BY SimPaths AS IMPLIED BY STATE COMBINATION
     * Code describes benefitUnit type
     * @return Gender
     */
    Dhhtp_c4 getHouseholdTypeCode() {
        Dhhtp_c4 code;
        if (getCohabitation()) {
            if (getChildrenAll() == 0) {
                code = Dhhtp_c4.CoupleNoChildren;
            } else {
                code = Dhhtp_c4.CoupleChildren;
            }
        } else {
            if (getChildrenAll() == 0) {
                code = Dhhtp_c4.SingleNoChildren;
            } else {
                code = Dhhtp_c4.SingleChildren;
            }
        }
        return code;
    }

    /**
     * METHOD TO IDENTIFY Indicator enum CODE FOR DISABILITY USED BY SimPaths AS IMPLIED BY STATE COMBINATION
     * Code describes whether reference person is long-term sick or disabled
     * @return Indicator
     */
    Indicator getDlltsd() {
        Indicator code;
        if (DecisionParams.flagDisability && ageYears >= DecisionParams.minAgeForPoorHealth && ageYears <= DecisionParams.maxAgeForDisability()) {
            if (getDisability()==0) {
                code = Indicator.False;
            } else {
                code = Indicator.True;
            }
        } else {
            code = DecisionParams.DEFAULT_DISABILITY;
        }
        return code;
    }

    /**
     * METHOD TO IDENTIFY Education enum CODE FOR EDUCATION STATUS USED BY SimPaths AS IMPLIED BY STATE COMBINATION
     * Code describes whether reference person is long-term sick or disabled
     * @return Education
     */
    Education getEducationCode() {
        Education code;
        if (DecisionParams.flagEducation) {
            int state = getEducation();
            if (state== DecisionParams.PTS_EDUCATION-1) {
                code = Education.High;
            } else {
                if (DecisionParams.PTS_EDUCATION==2) {
                    code = DecisionParams.DEFAULT_EDUCATION;
                } else {
                    if (state==1) {
                        code = Education.Medium;
                    } else {
                        code = Education.Low;
                    }
                }
            }
        } else {
            code = DecisionParams.DEFAULT_EDUCATION;
        }
        return code;
    }

    /**
     * METHOD TO IDENTIFY Les_c4 enum CODE USED BY SimPaths AS IMPLIED BY STATE COMBINATION
     * Code describes whether reference person is long-term sick or disabled
     * @return Education
     */
    Les_c4 getLesCode(double employment) {
        Les_c4 code;
        int student = 0;
        if (DecisionParams.flagEducation) {
            if (ageYears <= Parameters.MAX_AGE_TO_LEAVE_CONTINUOUS_EDUCATION) {
                student = getStudent();
            }
        }
        if (student > 0) {
            code = Les_c4.Student;
        } else if (employment<0.01) {
            code = Les_c4.NotEmployed;
        } else {
            code = Les_c4.EmployedOrSelfEmployed;
        }
        return code;
    }

    Lesdf_c4 getLesC4Code(double emp1, double emp2) {
        Lesdf_c4 code;
        if ( emp1 > 0 && emp2 > 0) code = Lesdf_c4.BothEmployed;
        else if ( emp1 > 0 ) code = Lesdf_c4.NotEmployedSpouseEmployed;
        else if ( emp2 > 0 ) code = Lesdf_c4.EmployedSpouseNotEmployed;
        else code = Lesdf_c4.BothNotEmployed;
        return code;
    }

    /**
     * METHOD TO IDENTIFY HEALTH MEASURE USED BY SimPaths AS IMPLIED BY STATE COMBINATION
     */
    Dhe getHealthCode() {
        Dhe code;
        if (DecisionParams.flagHealth && ageYears >= DecisionParams.minAgeForPoorHealth)
            code = Dhe.getCode(getVal(Axis.Health));
        else
            code = DecisionParams.DEFAULT_HEALTH;
        return code;
    }

    /**
     * METHOD TO IDENTIFY SOCIAL CARE RECEIPT STATE CODE IMPLIED BY STATE COMBINATION
     */
    SocialCareReceiptState getSocialCareReceiptStateCode() {
        SocialCareReceiptState code;
        if (Parameters.flagSocialCare && ageYears >= DecisionParams.minAgeReceiveFormalCare)
            code = SocialCareReceiptState.getCode(getVal(Axis.SocialCareReceiptState));
        else
            code = SocialCareReceiptState.NoneNeeded;
        return code;
    }

    /**
     * METHOD TO IDENTIFY SOCIAL CARE RECEIPT STATE CODE IMPLIED BY STATE COMBINATION
     */
    SocialCareReceipt getSocialCareReceiptCode() {
        SocialCareReceipt code;
        if (Parameters.flagSocialCare && ageYears >= DecisionParams.minAgeReceiveFormalCare)
            code = SocialCareReceipt.getCode(getVal(Axis.SocialCareReceiptState));
        else
            code = SocialCareReceipt.None;
        return code;
    }

    /**
     * METHOD TO IDENTIFY SOCIAL CARE PROVISION CODE IMPLIED BY STATE COMBINATION
     */
    SocialCareProvision getSocialCareProvisionCode() {
        SocialCareProvision code;
        if (Parameters.flagSocialCare)
            code = SocialCareProvision.getCode(getVal(Axis.SocialCareProvision));
        else
            code = SocialCareProvision.None;
        return code;
    }

    /**
     * METHOD TO IDENTIFY HEALTH MEASURE USED BY SimPaths AS IMPLIED BY STATE COMBINATION
     * @return double
     */
    Occupancy getOccupancyCode() {
        Occupancy code;
        if (getCohabitation())
            code = Occupancy.Couple;
        else if (getGender()==0) {
            code = Occupancy.Single_Male;
        } else {
            code = Occupancy.Single_Female;
        }
        return code;
    }

    /**
     * METOD TO RETURN INDICATOR FOR CHILDREN UNDER 3 YEARS OLD
     * @return Indicator
     */
    Indicator getChildrenUnder3Indicator() {
        if (getChildren02() > 0) {
            return Indicator.True;
        } else {
            return Indicator.False;
        }
    }

    public void systemReportError() {
        systemReportError(-1);
    }
    public void systemReportError(long errorIndex) {

        String fmtFinancial = "%.2f";
        String fmtInteger = "%.1f";
        String fmtIndicator = "%.1f";
        String fmtProportion = "%.3f";

        if (errorIndex == -1) {
            System.out.println("--------------------------------------");
            System.out.println("CALL TO INTERPOLATE OUTSIDE OF GRID");
            System.out.println("--------------------------------------");
        } else {
            System.out.println("--------------------------------------");
            System.out.println("REFERENCE TO UNINITIALISED POINT OF GRID");
            System.out.println("--------------------------------------");
            System.out.println("Referenced index: " + Long.toString(errorIndex));
        }
        Integer vali = ageYears;
        String msg = "Current age: " + vali.toString();
        System.out.println(msg);

        // liquid wealth
        int stateIndex = 0;
        printOutOfBounds(stateIndex);
        msg = "liquid wealth: " + String.format(fmtFinancial,Math.exp(states[stateIndex]) - DecisionParams.C_LIQUID_WEALTH);
        System.out.println(msg);

        // full-time wage potential
        if (ageYears <= DecisionParams.maxAgeFlexibleLabourSupply) {
            stateIndex = scale.getIndex(Axis.WagePotential, ageYears);
            printOutOfBounds(stateIndex);
            msg = "wage potential: " + String.format(fmtFinancial,Math.exp(states[stateIndex]) - DecisionParams.C_WAGE_POTENTIAL);
            System.out.println(msg);
        }

        // pension
        if (DecisionParams.flagPrivatePension && ageYears > DecisionParams.minAgeToRetire) {
            stateIndex = scale.getIndex(Axis.PensionIncome, ageYears);
            printOutOfBounds(stateIndex);
            msg = "private pension: " + String.format(fmtFinancial,Math.exp(states[stateIndex]) - DecisionParams.C_PENSION);
            System.out.println(msg);
        }

        // health state
        if (DecisionParams.flagHealth && ageYears >= DecisionParams.minAgeForPoorHealth) {
            stateIndex = scale.getIndex(Axis.Health, ageYears);
            printOutOfBounds(stateIndex);
            msg = "health: " + String.format(fmtProportion,states[stateIndex]);
            System.out.println(msg);
        }

        // birth year
        stateIndex = scale.getIndex(Axis.BirthYear, ageYears);
        printOutOfBounds(stateIndex);
        msg = "birth year: " + String.format(fmtInteger,states[stateIndex]);
        System.out.println(msg);

        // wage offer
        if (ageYears <= DecisionParams.maxAgeFlexibleLabourSupply && DecisionParams.flagLowWageOffer1) {
            stateIndex = scale.getIndex(Axis.WageOffer1, ageYears);
            printOutOfBounds(stateIndex);
            msg = "wage offer: " + String.format(fmtIndicator,states[stateIndex]);
            System.out.println(msg);
        }

        // retirement
        if (DecisionParams.flagRetirement && ageYears > DecisionParams.minAgeToRetire && ageYears <= DecisionParams.maxAgeFlexibleLabourSupply) {
            stateIndex = scale.getIndex(Axis.Retirement, ageYears);
            printOutOfBounds(stateIndex);
            msg = "retirement: " + String.format(fmtIndicator,states[stateIndex]);
            System.out.println(msg);
        }

        // disability
        if (DecisionParams.flagDisability && ageYears >= DecisionParams.minAgeForPoorHealth && ageYears <= DecisionParams.maxAgeForDisability()) {
            stateIndex = scale.getIndex(Axis.Disability, ageYears);
            printOutOfBounds(stateIndex);
            msg = "disability: " + String.format(fmtIndicator,states[stateIndex]);
            System.out.println(msg);
        }

        // social care receipt
        if (Parameters.flagSocialCare && ageYears >= DecisionParams.minAgeReceiveFormalCare) {
            stateIndex = scale.getIndex(Axis.SocialCareReceiptState, ageYears);
            printOutOfBounds(stateIndex);
            msg = "social care receipt: " + String.format(fmtIndicator,states[stateIndex]);
            System.out.println(msg);
        }

        // social care provision
        if (Parameters.flagSocialCare) {
            stateIndex = scale.getIndex(Axis.SocialCareProvision, ageYears);
            printOutOfBounds(stateIndex);
            msg = "social care provision: " + String.format(fmtIndicator,states[stateIndex]);
            System.out.println(msg);
        }

        // region
        if (DecisionParams.flagRegion) {
            stateIndex = scale.getIndex(Axis.Region, ageYears);
            printOutOfBounds(stateIndex);
            msg = "region: " + String.format(fmtInteger,states[stateIndex]);
            System.out.println(msg);
        }

        // student
        if (ageYears <= Parameters.MAX_AGE_TO_LEAVE_CONTINUOUS_EDUCATION && DecisionParams.flagEducation) {
            stateIndex = scale.getIndex(Axis.Student, ageYears);
            printOutOfBounds(stateIndex);
            msg = "student: " + String.format(fmtIndicator,states[stateIndex]);
            System.out.println(msg);
        }

        // education
        if (DecisionParams.flagEducation) {
            stateIndex = scale.getIndex(Axis.Education, ageYears);
            printOutOfBounds(stateIndex);
            msg = "education: " + String.format(fmtInteger,states[stateIndex]);
            System.out.println(msg);
        }

        // children
        for (int jj = 0; jj < DecisionParams.NUMBER_BIRTH_AGES; jj++) {

            if (ageYears >= DecisionParams.BIRTH_AGE[jj] && ageYears < (DecisionParams.BIRTH_AGE[jj] + Parameters.AGE_TO_BECOME_RESPONSIBLE)) {

                stateIndex = scale.getIndex(Axis.Child, ageYears, jj);
                printOutOfBounds(stateIndex);
                msg = "children" + String.format("%d", jj) + ": " + String.format(fmtInteger,states[stateIndex]);
                System.out.println(msg);
            }
        }

        // cohabitation
        if (ageYears <= DecisionParams.MAX_AGE_COHABITATION) {
            stateIndex = scale.getIndex(Axis.Cohabitation, ageYears);
            printOutOfBounds(stateIndex);
            msg = "cohabitation: " + String.format(fmtIndicator,states[stateIndex]);
            System.out.println(msg);
        }

        // gender
        stateIndex = scale.getIndex(Axis.Gender, ageYears);
        printOutOfBounds(stateIndex);
        msg = "gender: " + String.format(fmtIndicator,states[stateIndex]);
        System.out.println(msg);
    }

    private void printOutOfBounds(int stateIndex) {

        if (states[stateIndex] > scale.axes[ageIndex][stateIndex][2] + eps ||
                states[stateIndex] < scale.axes[ageIndex][stateIndex][1] - eps) {
            System.out.println("NEXT STATE IS OUT OF BOUNDS");
        }
    }

    public double getAvailableCredit() {

        if (ageYears == DecisionParams.maxAge) {
            return 0.0;
        } else {
            return -(Math.exp(scale.axes[ageIndex][0][1]) - DecisionParams.C_LIQUID_WEALTH);
        }
    }

    public int[] getChildrenByBirthAge(Person mother) {

        int[] children;
        children = new int[DecisionParams.NUMBER_BIRTH_AGES];
        for (int ii=0; ii<DecisionParams.NUMBER_BIRTH_AGES; ii++) {
            children[ii] = 0;
        }
        if (mother != null) {
            // evaluate children

            int ageWoman = mother.getDag();
            for (int ii=0; ii<DecisionParams.NUMBER_BIRTH_AGES; ii++) {
                // loop over each birth year

                int[] ageVector = getFertilityAgeBand(ii);
                for (int aa=ageVector[0]; aa<=ageVector[1]; aa++) {
                    // loop over each age included in birth year

                    int childAge = ageWoman - aa;
                    if (childAge>=0 && childAge< Parameters.AGE_TO_BECOME_RESPONSIBLE) {
                        // check for children of this age

                        children[ii] += this.getChildrenByAge(childAge);
                    }
                }
            }
        }
        return children;
    }

    public int[] getFertilityAgeBand(int birthYear) {

        int age0, age1;
        int[] ageVector = new int[2];
        if (birthYear==0) {
            // youngest year - birth pool extends to minimum age of fertility

            age0 = Parameters.MIN_AGE_MATERNITY;
            if (DecisionParams.NUMBER_BIRTH_AGES == 1) {
                // just one birth age allowed

                age1 = Parameters.MAX_AGE_MATERNITY;
            } else {

                age1 = (DecisionParams.BIRTH_AGE[birthYear] + DecisionParams.BIRTH_AGE[birthYear + 1]) / 2;
            }
        } else {
            // beyond lowest birth year

            age0 = (DecisionParams.BIRTH_AGE[birthYear - 1] + DecisionParams.BIRTH_AGE[birthYear]) / 2 + 1;
            if (birthYear == DecisionParams.NUMBER_BIRTH_AGES - 1) {
                // highest year - birth pool extends to maximum age of fertility

                age1 = Parameters.MAX_AGE_MATERNITY;
            } else {

                age1 = (DecisionParams.BIRTH_AGE[birthYear] + DecisionParams.BIRTH_AGE[birthYear + 1]) / 2;
            }
        }
        ageVector[0] = age0;
        ageVector[1] = age1;
        return ageVector;
    }
}
