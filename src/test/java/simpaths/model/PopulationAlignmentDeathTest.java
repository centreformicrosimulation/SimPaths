package simpaths.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import simpaths.model.enums.SampleExit;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PopulationAlignmentDeathTest {

    @Test
    void secondParentIsProtectedAfterFirstParentIsMarkedDead() {
        Person mother = person(32, SampleExit.NotYet);
        Person father = person(25, SampleExit.NotYet);
        Person child = person(2, SampleExit.NotYet);
        BenefitUnit unit = unit(mother, father, child);

        assertTrue(SimPathsModel.canRemoveForPopulationAlignment(mother, unit));
        when(mother.getSampleExit()).thenReturn(SampleExit.DeathAlignment);
        // Both parents are still members, just as they are before annual cleanup.
        assertFalse(SimPathsModel.canRemoveForPopulationAlignment(father, unit));
    }

    @ParameterizedTest
    @EnumSource(value = SampleExit.class, names = {"Death", "DeathAlignment", "EmigrationAlignment"})
    void exitedAdultCannotProvideCareForRemainingChildren(SampleExit firstAdultExit) {
        Person exited = person(40, firstAdultExit);
        Person survivor = person(38, SampleExit.NotYet);
        BenefitUnit unit = unit(exited, survivor, person(8, SampleExit.NotYet));

        assertFalse(SimPathsModel.canRemoveForPopulationAlignment(survivor, unit));
    }

    @Test
    void singleParentWithDependentChildIsProtected() {
        Person parent = person(30, SampleExit.NotYet);
        assertFalse(SimPathsModel.canRemoveForPopulationAlignment(parent,
                unit(parent, person(17, SampleExit.NotYet))));
    }

    @Test
    void adultAtResponsibilityThresholdCanCareForRemainingChild() {
        Person parent = person(45, SampleExit.NotYet);
        assertTrue(SimPathsModel.canRemoveForPopulationAlignment(parent,
                unit(parent, person(18, SampleExit.NotYet), person(10, SampleExit.NotYet))));
    }

    @Test
    void adultWithoutRemainingDependentsCanBeRemoved() {
        Person adult = person(70, SampleExit.NotYet);
        assertTrue(SimPathsModel.canRemoveForPopulationAlignment(adult, unit(adult)));
        assertTrue(SimPathsModel.canRemoveForPopulationAlignment(adult,
                unit(adult, person(65, SampleExit.Death), person(12, SampleExit.EmigrationAlignment))));
    }

    @Test
    void dependentDeathDoesNotRemoveResponsibleAdult() {
        Person child = person(12, SampleExit.NotYet);
        assertTrue(SimPathsModel.canRemoveForPopulationAlignment(child,
                unit(child, person(35, SampleExit.NotYet))));
    }

    @Test
    void personAlreadyMarkedAsExitedCannotBeRemovedAgain() {
        Person exited = person(70, SampleExit.Death);
        assertFalse(SimPathsModel.canRemoveForPopulationAlignment(exited, unit(exited)));
    }

    private static Person person(int age, SampleExit exit) {
        Person person = mock(Person.class);
        when(person.getDemAge()).thenReturn(age);
        when(person.getSampleExit()).thenReturn(exit);
        return person;
    }

    private static BenefitUnit unit(Person... members) {
        BenefitUnit unit = mock(BenefitUnit.class);
        when(unit.getMembers()).thenReturn(Set.of(members));
        return unit;
    }
}
