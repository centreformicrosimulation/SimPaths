package simpaths.data.startingpop;


import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import simpaths.model.enums.Country;

import java.io.Serializable;

@Embeddable
public class PopKey implements Serializable {

    @Column(name = "country") @Enumerated(EnumType.STRING) private Country country;
    @Column(name = "start_year") private int startYear;
    @Column(name = "size") private int size;


    public PopKey(){super();}

    public PopKey(Country country, Integer startYear, Integer size) {
        super();
        this.country = country;
        this.startYear = startYear;
        this.size = size;
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

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
