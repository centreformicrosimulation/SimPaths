package simpaths.data.filters;

import org.junit.jupiter.api.*;
import simpaths.model.Person;
import simpaths.model.enums.Les_c4;


import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Test filtering by employment history")
class EmploymentHistoryFilterTest {

    private static Person createTestPerson(
            Les_c4 les_c4_lag1
    ) {
        Person testPerson = new Person(true);
        testPerson.setLes_c4_lag1(les_c4_lag1);

        return testPerson;
    }

    @Test
    @DisplayName("Employed filter only returns true for employed or self-employed persons")
    void employedOrSelfEmployed() {
        EmploymentHistoryFilter filter = new EmploymentHistoryFilter(Les_c4.EmployedOrSelfEmployed);
        assertTrue(filter.isFiltered(createTestPerson(Les_c4.EmployedOrSelfEmployed)));
        assertFalse(filter.isFiltered(createTestPerson(Les_c4.NotEmployed)));
        assertFalse(filter.isFiltered(createTestPerson(Les_c4.Student)));
        assertFalse(filter.isFiltered(createTestPerson(Les_c4.Retired)));
    }

    @Test
    @DisplayName("Unemployed filter only returns true for unemployed persons")
    void unEmployed() {
        EmploymentHistoryFilter filter = new EmploymentHistoryFilter(Les_c4.NotEmployed);
        assertFalse(filter.isFiltered(createTestPerson(Les_c4.EmployedOrSelfEmployed)));
        assertTrue(filter.isFiltered(createTestPerson(Les_c4.NotEmployed)));
        assertFalse(filter.isFiltered(createTestPerson(Les_c4.Student)));
        assertFalse(filter.isFiltered(createTestPerson(Les_c4.Retired)));
    }


}