package simpaths.model.taxes.database;

import org.apache.commons.lang3.ArrayUtils;
import simpaths.model.decisions.DecisionParams;

import java.util.*;


/**
 *
 * CLASS TO MANAGE PRODUCTION AND SPECIFICATION OF CLONES TO FILL GAPS IN TAX DATABASE
 *
 */
public class CloneBenefitUnit {

    /**
     * ATTRIBUTES
     */
    private List<Map> members = new ArrayList<>();
    private Map target = null;
    private Map spouse = null;
    private List<Map> children = new ArrayList<>();
    private List<Map> childrenUnder5 = new ArrayList<>();
    private List<Map> children5To9 = new ArrayList<>();
    private List<Map> children10To17 = new ArrayList<>();
    String[] variablesAll;
    String[] HOUSEHOLD_VARIABLES = {"drgn1", "dwt", "dct", "dcz", "ddt", "ddt01", "dpd", "dot", "amrrm", "amrtn"};
    String[] SPOUSE_SPECIFIC_VARIABLES = {"dec", "dag", "deh", "dew", "dey"};
    String[] SPOUSE_VARIABLES = ArrayUtils.addAll(HOUSEHOLD_VARIABLES, SPOUSE_SPECIFIC_VARIABLES);
    String[] INCOME_VARIABLES = {"yem", "yiy", "yiytx", "yiynt", "yptmp", "yot01", "ypr", "yprtx", "yprnt", "ypp", "yptot", "yse"};


    /**
     * CONSTRUCTORS
     */
    public CloneBenefitUnit(){}

    public long[] clone(long idTarget, InputDataSet dataSet, String[] variablesAll, long newHouseholdId) {

        this.variablesAll = variablesAll;
        long newPersonId = newHouseholdId*100 + 1;
        long[] ids = findTargetHouseholdId(idTarget, dataSet);
        long householdId = ids[0];
        int benefitUnitId = (int)ids[1];
        long[] result = cloneAllHouseholdMembers(dataSet, householdId, benefitUnitId, idTarget, newHouseholdId, newPersonId);
        result[0]++;
        return result;
    }

    public long[] clone(CloneBenefitUnit household, String[] variableAll, long newHouseholdId) {
        this.variablesAll = variableAll;
        long newPersonId = newHouseholdId*100 + 1;
        long[] result = cloneAllHouseholdMembers(household, newHouseholdId, newPersonId);
        result[0]++;
        return result;
    }


    public List<Map> getMembers() {return members;}
    public Map getTarget() {return target;}
    public Map getSpouse() {return spouse;}
    public List<Map> getChildren() {return children;}


    /**
     * METHODS TO IDENTIFY ADJUSTMENTS REQUIRED TO FILL DATABASE GAPS
     */
    public void matchIncome(double income) {

        if (Math.abs(income) < 0.01) {

            setIncome(target, 0.0);
            if (spouse!=null)
                setIncome(spouse, 0.0);
        } else {

            double targetIncome = getOriginalIncomePerMonth(target);
            double spouseIncome = 0.0;
            if (spouse!=null)
                spouseIncome = getOriginalIncomePerMonth(spouse);
            double currentIncome = targetIncome + spouseIncome;
            if ( income < 0.0 ) {
                double incomeAdj = income - currentIncome;
                double val = (double)target.get("yprnt") + incomeAdj;
                target.replace("yprnt", val);
                val = (double)target.get("ypr") + incomeAdj;
                target.replace("ypr", val);
            } else {
                if ( targetIncome > 10.0 ) {
                    if ( spouseIncome > 10.0 ) {
                        double incomeAdj = income / currentIncome;
                        setIncome(target, targetIncome, targetIncome * incomeAdj);
                        setIncome(spouse, spouseIncome, spouseIncome * incomeAdj);
                    } else {
                        double incomeAdj = income - spouseIncome;
                        setIncome(target, targetIncome, incomeAdj);
                    }
                } else {
                    if ( spouseIncome > 10.0 ) {
                        double incomeAdj = income - targetIncome;
                        setIncome(spouse, spouseIncome, incomeAdj);
                    } else {
                        double incomeAdj = income - currentIncome;
                        double val = (double)target.get("yprnt") + incomeAdj;
                        target.replace("yprnt", val);
                        val = (double)target.get("ypr") + incomeAdj;
                        target.replace("ypr", val);
                    }
                }
            }
        }
    }
    public boolean matchDualIncomeIndex(int index) {

        boolean change = false;
        if (spouse!=null) {
            double spouseIncome = getOriginalIncomePerMonth(spouse);
            double targetIncome = getOriginalIncomePerMonth(target);
            if ( (index==0) && (spouseIncome>0.01) && (targetIncome>0.01) ) {
                change = true;
                setIncome(spouse, 0.0);
            } else if ( (index==1) && ((spouseIncome<0.01) || (targetIncome<0.01)) ) {
                change = true;
                if (targetIncome<0.01) {
                    target.replace("yprnt", 500.0);
                    target.replace("yprtx", 0.0);
                    target.replace("ypr", 500.0);
                }
                if (spouseIncome<0.01) {
                    spouse.replace("yprnt", 500.0);
                    spouse.replace("yprtx", 0.0);
                    spouse.replace("ypr", 500.0);
                }
            }
        } else if (index==1)
            throw new RuntimeException("single household cannot match to dual income");

        return change;
    }
    private void setIncome(Map person, double incomeEnd) {

        if (Math.abs(incomeEnd) < 0.01) {
            for (String var : INCOME_VARIABLES) {
                person.replace(var, 0.0);
            }
        } else {
            double incomeStart = getOriginalIncomePerMonth(person);
            setIncome(person, incomeStart, incomeEnd);
        }
    }
    private void setIncome(Map person, double incomeStart, double incomeEnd) {

        if (Math.abs(incomeEnd)<0.01) {
            for (String var : INCOME_VARIABLES) {
                person.replace(var, 0.0);
            }
        } else {
            if (incomeStart<0.01)
                throw new RuntimeException("Attempt to adjust income for observation without any starting income");
            double adjFactor = incomeEnd / incomeStart;
            for (String var : INCOME_VARIABLES) {
                person.replace(var, (double)person.get(var)*adjFactor);
            }
        }
    }
    private double getOriginalIncomePerMonth(Map person) {
        double income = 0.0;
        for (String var : INCOME_VARIABLES) {
            income += (double)person.get(var);
        }
        return income;
    }
    public boolean matchChildcareIndex(int index) {

        boolean change = false;
        double childcare = 0.0;
        for (Map person : members) {
            childcare += (double)person.get("xcc");
        }
        if ( (index==0) && (childcare>0) ) {
            change = true;
            for (Map person : members) {
                person.replace("xcc", 0.0);
            }
        } else if ( (index==1) && (childcare<0.01) ) {
            change = true;
            if (children.isEmpty())
                throw new RuntimeException("attempt to add childcare to household without children");
            if (!childrenUnder5.isEmpty()) {
                for (Map child : childrenUnder5) {
                    child.replace("xcc", 250.0);
                }
            } else if (!children5To9.isEmpty()) {
                children5To9.iterator().next().replace("xcc",250.0);
            } else {
                children10To17.iterator().next().replace("xcc",250.0);
            }
        }
        return change;
    }
    public boolean matchCarerIndex(int index) {

        boolean change = false;
        int carerTarget = getInteger(target,"lcr01");
        int carerSpouse = 0;
        if (spouse!=null)
            carerSpouse = getInteger(spouse,"lcr01");
        int carer = Math.max(carerTarget, carerSpouse);
        if ( (index==0) && (carer>0) ) {
            change = true;
            if (carerTarget > 0)
                target.replace("lcr01", 0.0);
            if (carerSpouse > 0)
                spouse.replace("lcr01", 0.0);
        } else if ( (index==1) && (carer==0) ) {
            change = true;
            if (spouse!=null)
                spouse.replace("lcr01", 1.0);
            else
                target.replace("lcr01", 1.0);
        }
        return change;
    }
    public boolean matchDisabledIndex(int index) {

        boolean change = false;
        int ddiTarget = getInteger(target,"ddi");
        int ddiSpouse = 0;
        if (spouse!=null) {
            ddiSpouse = Math.max(ddiSpouse, getInteger(spouse,"ddi"));
        }
        int ddi = Math.max(ddiTarget, ddiSpouse);
        if ( (index==0) && (ddi==1)) {
            change = true;
            if (ddiTarget==1)
                target.replace("ddi", 0.0);
            if (ddiSpouse==1)
                target.replace("ddi", 0.0);
        } else if ( (index==1) && (ddi==0) ) {
            change = true;
            if (spouse!=null)
                spouse.replace("ddi", 1.0);
            else
                target.replace("ddi", 1.0);
        }
        return change;
    }
    public boolean matchEmploymentIndex(int index) {

        boolean change = false;
        int partTimeEmployed = 0, fullTimeEmployed = 0;
        double targetHours = (double)target.get("lhw");
        double spouseHours = 0.0;
        if (targetHours >= DecisionParams.PARTTIME_HOURS_WEEKLY) {
            fullTimeEmployed += 1;
        } else if (targetHours > DecisionParams.MIN_WORK_HOURS_WEEKLY) {
            partTimeEmployed += 1;
        }
        if (spouse!=null) {
            spouseHours = (double)spouse.get("lhw");
            if (spouseHours >= DecisionParams.PARTTIME_HOURS_WEEKLY) {
                fullTimeEmployed += 1;
            } else if (spouseHours > DecisionParams.MIN_WORK_HOURS_WEEKLY) {
                partTimeEmployed += 1;
            }
        }

        if ( (index==0) && (fullTimeEmployed+partTimeEmployed>0) ) {
            change = true;
            adjHours(target, 0.0);
            if (spouse!=null) {
                adjHours(spouse, 0.0);
            }
        } else if ( (index==1) && (fullTimeEmployed>0) ) {
            change = true;
            if ( targetHours >= DecisionParams.PARTTIME_HOURS_WEEKLY ) {
                adjHours(target, DecisionParams.PARTTIME_HOURS_WEEKLY - 1.0);
            }
            if ( spouseHours >= DecisionParams.PARTTIME_HOURS_WEEKLY ) {
                adjHours(spouse, DecisionParams.PARTTIME_HOURS_WEEKLY - 1.0);
            }
        } else if ( (index==2) && ((fullTimeEmployed!=1) || (partTimeEmployed!=0)) ) {
            change = true;
            adjHours(target, Math.max(targetHours, DecisionParams.PARTTIME_HOURS_WEEKLY + 1.0));
            if (spouse!=null) {
                adjHours(spouse, 0.0);
            }
        } else if ( (index==3) && ((fullTimeEmployed!=1) || (partTimeEmployed!=1)) ) {
            change = true;
            adjHours(target, Math.max(targetHours, DecisionParams.PARTTIME_HOURS_WEEKLY + 1.0));
            adjHours(spouse, DecisionParams.PARTTIME_HOURS_WEEKLY - 1.0);
        } else if ( (index==4) && (fullTimeEmployed!=2) ) {
            change = true;
            adjHours(target, Math.max(targetHours, DecisionParams.PARTTIME_HOURS_WEEKLY + 1.0));
            adjHours(spouse, Math.max(spouseHours, DecisionParams.PARTTIME_HOURS_WEEKLY + 1.0));
        }
        return change;
    }
    public long matchChildrenIndex(int index, long newPersonId) {

        boolean change = false;
        int child10To17index = index / 9;
        index -= 9*child10To17index;
        if ( child10To17index == 0 ) {
            if (!children10To17.isEmpty()) {
                change = true;
                Iterator<Map> ii = children10To17.iterator();
                while (ii.hasNext()) {
                    Map child = ii.next();
                    members.remove(child);
                    children.remove(child);
                    ii.remove();
                }
            }
        } else {
            if (children10To17.isEmpty()) {
                change = true;
                Map child = newChild(newPersonId);
                child.replace("dag",10.0);
                child.replace("dec",3.0);
                child.replace("deh",0.0);
                members.add(child);
                children.add(child);
                children10To17.add(child);
                newPersonId++;
            }
        }
        int child5To9index = index / 3;
        index -= 3*child5To9index;
        if ( child5To9index<children5To9.size() && child5To9index<2) {
            change = true;
            int nn = children5To9.size();
            Iterator<Map> ii = children5To9.iterator();
            while (ii.hasNext()) {
                Map child = ii.next();
                members.remove(child);
                children.remove(child);
                ii.remove();
                nn--;
                if (nn == child5To9index)
                    break;
            }
        } else if (child5To9index>children5To9.size()) {
            change = true;
            int nn = child5To9index - children5To9.size();
            for (int ii=0; ii<nn; ii++) {
                Map child = newChild(newPersonId);
                child.replace("dag",5.0);
                child.replace("dec",2.0);
                child.replace("deh",0.0);
                members.add(child);
                children.add(child);
                children5To9.add(child);
                newPersonId++;
            }
        }
        if ( index<childrenUnder5.size() && index<2) {
            change = true;
            int nn = childrenUnder5.size();
            Iterator<Map> ii = childrenUnder5.iterator();
            while (ii.hasNext()) {
                Map child = ii.next();
                members.remove(child);
                children.remove(child);
                ii.remove();
                nn--;
                if (nn == index)
                    break;
            }
        } else if (index>childrenUnder5.size()) {
            change = true;
            int nn = index - childrenUnder5.size();
            for (int ii=0; ii<nn; ii++) {
                Map child = newChild(newPersonId);
                child.replace("dag",2.0);
                child.replace("dec",0.0);
                child.replace("deh",0.0);
                members.add(child);
                children.add(child);
                childrenUnder5.add(child);
                newPersonId++;
            }
        }
        return newPersonId;
    }
    public long matchAdultIndex(int index, long newPersonId) {

        boolean change = false;
        if ( index==0 ) {
            if (spouse!=null) {
                change = true;
                members.remove(spouse);
                spouse = null;
                target.replace("dms",1.0);
                target.replace("idpartner",0.0);
                for (Map child : children) {
                    if (getInteger(target,"dgn")==0) {
                        child.replace("idfather",0.0);
                    } else {
                        child.replace("idmother",0.0);
                    }
                }
            }
        } else {
            if (spouse==null) {
                change = true;
                newSpouse(newPersonId);
                members.add(spouse);
                newPersonId++;
            }
        }
        return newPersonId;
    }
    public boolean matchAgeIndex(int index) {

        boolean change = false;
        int ageMax = getInteger(target,"dag");
        if (spouse!=null)
            ageMax = Math.max(ageMax, getInteger(spouse,"dag"));
        int ageFin = ageMax;
        if ( index==2 ) {
            if (ageMax < 68) {
                ageFin = 68;
            }
        } else if ( index==1 ) {
            if (ageMax < 45) {
                ageFin = 45;
            } else if (getInteger(target,"dag") > 64) {
                ageFin = 64;
            }
        } else {
            if (getInteger(target,"dag") > 44) {
                ageFin = 44;
            }
        }
        if (ageFin!=ageMax) {
            change = true;
            target.replace("dag", (double)ageFin);
            if (spouse!=null)
                spouse.replace("dag", (double)ageFin);
        }
        return change;
    }


    /**
     * WORKER METHODS
     */
    private Map newChild(long newPersonId) {

        Map child = newPerson(HOUSEHOLD_VARIABLES, newPersonId);
        if (getInteger(target,"dgn")==0) {
            child.replace("idmother",target.get("idperson"));
            if (spouse!=null) {
                child.replace("idfather",spouse.get("idperson"));
            }
        } else {
            child.replace("idfather",target.get("idperson"));
            if (spouse!=null) {
                child.replace("idmother",spouse.get("idperson"));
            }
        }
        return child;
    }
    private void newSpouse(long newPersonId) {

        spouse = newPerson(SPOUSE_VARIABLES, newPersonId);
        spouse.replace("idpartner",target.get("idperson"));
        target.replace("idpartner",(double)newPersonId);
        target.replace("dms",2.0);
        spouse.replace("dms",2.0);
        int spouseGender = 1-getInteger(target,"dgn");
        spouse.replace("dgn",(double)spouseGender);
        for (Map child : children) {
            if (spouseGender==0) {
                child.replace("idmother",(double)newPersonId);
            } else {
                child.replace("idfather",(double)newPersonId);
            }
        }
        spouse.replace("les",7.0);
    }
    private Map newPerson(String[] variables, long newPersonId) {

        Map person = zeros();
        person.replace("idperson",(double)newPersonId);
        person.replace("idhh",target.get("idhh"));
        for (String var : variables) {
            person.replace(var,target.get(var));
        }
        return person;
    }
    private Map zeros() {

        Map values = new HashMap<>();
        for (String variable : variablesAll) {
            values.put(variable, 0.0);
        }
        return values;
    }
    private long[] cloneAllHouseholdMembers(CloneBenefitUnit household, long newHouseholdId, long newPersonId) {

        return cloneAllHouseholdMembers(household.getMembers(), getLong(household.getTarget(),"idperson"), newHouseholdId, newPersonId);
    }
    private long[] cloneAllHouseholdMembers(InputDataSet dataSet, long idhh, int idbu, long idTarget, long newHouseholdId, long newPersonId) {

        List<Map> originalMembers = new ArrayList<>();
        for (Map obs : dataSet.getSet()) {
            if ((getLong(obs, "idhh") == idhh) && (getInteger(obs, "idorigbenunit") == idbu)) {
                originalMembers.add(obs);
            }
        }
        return cloneAllHouseholdMembers(originalMembers, idTarget, newHouseholdId, newPersonId);
    }
    private long[] cloneAllHouseholdMembers(List<Map> originalMembers, long idTarget, long newHouseholdId, long newPersonId) {

        long fail = 0;
        Map idKey = new HashMap<Long,Double>();
        for (Map obs : originalMembers) {
            Map clone = clone(obs);
            idKey.put(getLong(obs,"idperson"),(double)newPersonId);
            clone.replace("idperson",(double)newPersonId);
            clone.replace("idhh",(double)newHouseholdId);
            members.add(clone);
            if (getLong(obs, "idperson") == idTarget)
                target = clone;
            if (getLong(obs, "idpartner") == idTarget)
                spouse = clone;
            if ( (getLong(obs,"idmother")==idTarget || getLong(obs,"idfather")==idTarget) && (getInteger(obs, "dag")<=17) ) {
                children.add(clone);
                if (getInteger(clone,"dag") < 5)
                    childrenUnder5.add(clone);
                else if (getInteger(clone,"dag") < 10)
                    children5To9.add(clone);
                else
                    children10To17.add(clone);
            }
            newPersonId++;
        }
        if (members.isEmpty() || target==null)
            fail = 1;
        if (fail==0) {

            for (Map obs : members) {
                if (replaceId(idKey, obs, "idpartner")) {
                    replaceId(idKey, obs, "idfather");
                    replaceId(idKey, obs, "idmother");
                } else {
                    fail = 1;
                }
            }
            if (fail==0) {
                for (Map obs : members) {
                    getLong(obs, "idperson");
                    getLong(obs, "idpartner");
                    getLong(obs, "idmother");
                    getLong(obs, "idfather");
                }
            }
        }
        long[] result = {newHouseholdId, newPersonId, fail};
        return result;
    }
    private boolean replaceId(Map idKey, Map obs, String name) {

        boolean pass = true;
        long idchk = getLong(obs,name);
        if (idchk!=0) {
            Double val = (Double)idKey.get(idchk);
            if (val==null) {
                pass = false;
                val = 0.0;
            }
            obs.replace(name, val);
        }
        return pass;
    }
    private long[] findTargetHouseholdId(long idTarget, InputDataSet dataSet) {

        Map target = null;
        for(Map obs : dataSet.getSet()) {
            if (idTarget == getLong(obs, "idperson")) {
                target = obs;
                break;
            }
        }
        if (target.equals(null))
            throw new RuntimeException("Failed to identify imperfect match observation");
        long[] result = {getLong(target,"idhh"), getInteger(target, "idorigbenunit")};
        return result;
    }
    private long getLong(Map obj, String val) {
        Object oo = obj.get(val);
        if (oo==null)
            throw new RuntimeException("problem copying value of " + val);
        return Double.valueOf((double)oo).longValue();
    }
    private int getInteger(Map obj, String val) {
        return Double.valueOf((double)obj.get(val)).intValue();
    }
    private Map clone(Map obs) {

        Map values = new HashMap<>();
        for (String variable : variablesAll) {
            values.put(variable, obs.get(variable));
        }
        return values;
    }
    private void adjHours(Map person, double hours) {

        double hoursStart = (double)person.get("lhw");
        if (hoursStart > 0.0) {

            double incAdj = hours / hoursStart;
            person.replace("yem", (double)person.get("yem") * incAdj);
            person.replace("yse", (double)person.get("yse") * incAdj);
            person.replace("lhw", hours);
        } else if (hours > 0.0) {

            person.replace("yem", 20.0 * hours);
            person.replace("yse", 0.0);
            person.replace("lhw", hours);
        } else {

            person.replace("yem", 0.0);
            person.replace("yse", 0.0);
            person.replace("lhw", 0.0);
        }
    }
    public int getPriceYear() {
        return getInteger(target,"dpd");
    }
}
