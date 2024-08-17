package simpaths.model;


import java.util.Random;
import java.util.random.RandomGenerator;

public class PersonInnovations {

    private final RandomGenerator healthRandomGen;
    private final RandomGenerator socialCareRandomGen;
    private final RandomGenerator wagesRandomGen;
    private final RandomGenerator capitalRandomGen;
    private final RandomGenerator resStanDevRandomGen;
    private final RandomGenerator housingRandomGen;
    private final RandomGenerator labourRandomGen;
    private final RandomGenerator cohabitRandomGen;
    private final RandomGenerator fertilityRandomGen;
    private final RandomGenerator educationRandomGen;
    private final RandomGenerator benefitUnitRandomGen;

    public double healthRandomUniform1;
    public double healthRandomUniform2;
    public double healthRandomUniform3;
    public double healthRandomUniform4;
    public double healthRandomUniform5;
    public double socialCareRandomUniform1;
    public double socialCareRandomUniform2;
    public double socialCareRandomUniform3;
    public double socialCareRandomUniform4;
    public double socialCareRandomUniform5;
    public double socialCareRandomUniform6;
    public double socialCareRandomUniform7;
    public double socialCareRandomUniform8;
    public double socialCareRandomUniform9;
    public double socialCareRandomUniform10;
    public double wagesRandomUniform1;
    public double wagesRandomUniform2;
    public double capitalRandomUniform1;
    public double capitalRandomUniform2;
    public double capitalRandomUniform3;
    public double resStanDevRandomUniform;
    public double housingRandomUniform;
    public double labourRandomUniform1;
    public double labourRandomUniform2;
    public double labourRandomUniform3;
    public double cohabitRandomUniform1;
    public double cohabitRandomUniform2;
    public double fertilityRandomUniform1;
    public double fertilityRandomUniform2;
    public double fertilityRandomUniform3;
    public double educationRandomUniform;
    public double labourSupplySingleDraw = -9.;
    public double benefitUnitRandomUniform;

    public PersonInnovations(long seed) {

        RandomGenerator rndTemp = new Random(seed);
        healthRandomGen = new Random(rndTemp.nextLong());
        socialCareRandomGen = new Random(rndTemp.nextLong());
        wagesRandomGen = new Random(rndTemp.nextLong());
        capitalRandomGen = new Random(rndTemp.nextLong());
        resStanDevRandomGen = new Random(rndTemp.nextLong());
        housingRandomGen = new Random(rndTemp.nextLong());
        labourRandomGen = new Random(rndTemp.nextLong());
        cohabitRandomGen = new Random(rndTemp.nextLong());
        fertilityRandomGen = new Random(rndTemp.nextLong());
        educationRandomGen = new Random(rndTemp.nextLong());
        labourSupplySingleDraw = labourRandomGen.nextDouble();
        benefitUnitRandomGen = new Random(rndTemp.nextLong());
    }


    public void getNewDraws() {

        healthRandomUniform1 = healthRandomGen.nextDouble();
        healthRandomUniform2 = healthRandomGen.nextDouble();
        healthRandomUniform3 = healthRandomGen.nextDouble();
        healthRandomUniform4 = healthRandomGen.nextDouble();
        healthRandomUniform5 = healthRandomGen.nextDouble();
        socialCareRandomUniform1 = socialCareRandomGen.nextDouble();
        socialCareRandomUniform2 = socialCareRandomGen.nextDouble();
        socialCareRandomUniform3 = socialCareRandomGen.nextDouble();
        socialCareRandomUniform4 = socialCareRandomGen.nextDouble();
        socialCareRandomUniform5 = socialCareRandomGen.nextDouble();
        socialCareRandomUniform6 = socialCareRandomGen.nextDouble();
        socialCareRandomUniform7 = socialCareRandomGen.nextDouble();
        socialCareRandomUniform8 = socialCareRandomGen.nextDouble();
        socialCareRandomUniform9 = socialCareRandomGen.nextDouble();
        socialCareRandomUniform10 = socialCareRandomGen.nextDouble();
        wagesRandomUniform1 = wagesRandomGen.nextDouble();
        wagesRandomUniform2 = wagesRandomGen.nextDouble();
        capitalRandomUniform1 = capitalRandomGen.nextDouble();
        capitalRandomUniform2 = capitalRandomGen.nextDouble();
        capitalRandomUniform3 = capitalRandomGen.nextDouble();
        resStanDevRandomUniform = resStanDevRandomGen.nextDouble();
        housingRandomUniform = housingRandomGen.nextDouble();
        labourRandomUniform1 = labourRandomGen.nextDouble();
        labourRandomUniform2 = labourRandomGen.nextDouble();
        labourRandomUniform3 = labourRandomGen.nextDouble();
        cohabitRandomUniform1 = cohabitRandomGen.nextDouble();
        cohabitRandomUniform2 = cohabitRandomGen.nextDouble();
        fertilityRandomUniform1 = fertilityRandomGen.nextDouble();
        fertilityRandomUniform2 = fertilityRandomGen.nextDouble();
        fertilityRandomUniform3 = fertilityRandomGen.nextDouble();
        educationRandomUniform = educationRandomGen.nextDouble();
        benefitUnitRandomUniform = benefitUnitRandomGen.nextDouble();
    }
}
