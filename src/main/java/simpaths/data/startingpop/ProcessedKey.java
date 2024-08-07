package simpaths.data.startingpop;


import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import simpaths.model.enums.Country;

import java.io.Serializable;

@Embeddable
public class ProcessedKey implements Serializable {

    @Column(name = "country") @Enumerated(EnumType.STRING) private Country country;
    @Column(name = "start_year") private int startYear;
    @Column(name = "pop_size") private int popSize;


    public ProcessedKey(){super();}

    public ProcessedKey(Country country, Integer startYear, Integer popSize) {
        super();
        this.country = country;
        this.startYear = startYear;
        this.popSize = popSize;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    public int getStartYear() {
        return startYear;
    }

    public void setStartYear(int startYear) {
        this.startYear = startYear;
    }

    public int getPopSize() {
        return popSize;
    }

    public void setPopSize(int size) {
        this.popSize = size;
    }
}
