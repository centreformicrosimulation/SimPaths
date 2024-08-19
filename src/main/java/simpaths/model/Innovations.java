package simpaths.model;

import java.util.Random;
import java.util.random.RandomGenerator;

public class Innovations {

    RandomGenerator generator;
    double[] doubleInnovs;
    double[] singleDrawDoubleInnovs;
    long[] singleDrawLongInnovs;

    public Innovations(int nmbr, long seed) {
        generator = new Random(seed);
        doubleInnovs = new double[nmbr];
        getNewDoubleDraws();
    }

    public Innovations(int nDoubleInnovs, int nSingleDrawDoubleInnovs, int nSingleDrawLongInnovs, long seed) {
        this(nDoubleInnovs, seed);
        singleDrawDoubleInnovs = new double[nSingleDrawDoubleInnovs];
        for (int ii = 0; ii < nSingleDrawDoubleInnovs; ii++) {
            singleDrawDoubleInnovs[ii] = generator.nextDouble();
        }
        singleDrawLongInnovs = new long[nSingleDrawLongInnovs];
        for (int ii = 0; ii < nSingleDrawLongInnovs; ii++) {
            singleDrawLongInnovs[ii] = generator.nextLong();
        }
    }

    public void getNewDoubleDraws() {
        for (int ii = 0; ii < doubleInnovs.length; ii++) {
            doubleInnovs[ii] = generator.nextDouble();
        }
    }

    public double getDoubleDraw(int ii) {
        return doubleInnovs[ii];
    }

    public double getSingleDrawDoubleInnov(int ii) {
        return singleDrawDoubleInnovs[ii];
    }
    public long getSingleDrawLongInnov(int ii) {
        return singleDrawLongInnovs[ii];
    }
}
