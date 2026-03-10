package simpaths.model.enums;

import microsim.statistics.regression.IntegerValuedEnum;

public enum DhmGhq implements IntegerValuedEnum {

    Score0(0),
    Score1(1),
    Score2(2),
    Score3(3),
    Score4(4),
    Score5(5),
    Score6(6),
    Score7(7),
    Score8(8),
    Score9(9),
    Score10(10),
    Score11(11),
    Score12(12);

    private final int value;
    DhmGhq(int val) {value = val;}

    @Override
    public int getValue() {return value;}

    public Boolean getDepression() {return value >= 4;}

    public Boolean getMentalDisorder() {return value >= 3;}

}
