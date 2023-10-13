package simpaths.model.enums;

public enum UnionMatchingMethod {

    Parametric, //Use parametric matching based on wage and age differential
    ParametricNoRegion, //USe parametric matching as above first, and then relax region constraint and repeat matching
    SBAM; //Use SBAM Matching method

}
