package simpaths.model;

import microsim.data.db.PanelEntityKey;
import org.junit.jupiter.api.Test;
import simpaths.model.enums.Country;
import simpaths.model.enums.Les_c4;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimPathsModelTest {

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Person createPerson(long id, int age, Les_c4 initialStatus) throws Exception {
        Person person = new Person(true);
        setPrivateField(person, "key", new PanelEntityKey(id));
        person.setDemAge(age);
        person.setLes_c4(initialStatus);
        return person;
    }

    @Test
    void applyYearlyInterventionMovesConfiguredShareOfEligiblePersons() throws Exception {
        SimPathsModel model = new SimPathsModel(Country.UK, 2011);
        model.setYear(2015);
        model.setYoungNotEmployedToEmployedShare(0.5);

        Person eligible1 = createPerson(1L, 18, Les_c4.NotEmployed);
        Person eligible2 = createPerson(2L, 20, Les_c4.NotEmployed);
        Person eligible3 = createPerson(3L, 22, Les_c4.NotEmployed);
        Person eligible4 = createPerson(4L, 24, Les_c4.NotEmployed);
        Person tooYoung = createPerson(5L, 17, Les_c4.NotEmployed);
        Person tooOld = createPerson(6L, 25, Les_c4.NotEmployed);
        Person alreadyEmployed = createPerson(7L, 21, Les_c4.EmployedOrSelfEmployed);

        Set<Person> persons = new LinkedHashSet<>();
        persons.add(eligible1);
        persons.add(eligible2);
        persons.add(eligible3);
        persons.add(eligible4);
        persons.add(tooYoung);
        persons.add(tooOld);
        persons.add(alreadyEmployed);

        setPrivateField(model, "persons", persons);
        setPrivateField(model, "popAlignInnov", new Random(1234L));

        model.onEvent(SimPathsModel.Processes.ApplyYearlyIntervention);

        int eligibleNowEmployed = 0;
        for (Person person : Set.of(eligible1, eligible2, eligible3, eligible4)) {
            if (Les_c4.EmployedOrSelfEmployed.equals(person.getLes_c4())) {
                eligibleNowEmployed++;
            }
        }

        assertEquals(2, eligibleNowEmployed);
        assertEquals(Les_c4.NotEmployed, tooYoung.getLes_c4());
        assertEquals(Les_c4.NotEmployed, tooOld.getLes_c4());
        assertEquals(Les_c4.EmployedOrSelfEmployed, alreadyEmployed.getLes_c4());
    }
}
