/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject;

import com.eshabakhov.surok.federal.subject.input.SbInput;
import com.eshabakhov.surok.federal.subject.input.SbInputName;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * School views protect shared identities while retaining readable subject facts.
 * For example, a federal subject can be read but cannot be renamed by a school.
 *
 * @since 0.0.1
 */
final class SbSchoolTest {

    @Test
    void rejectsFederalRenaming() {
        Assertions.assertThrows(
            SubjectReadOnlyException.class,
            () -> new SbSchool(
                new SbFake(29L, "Математика", Subject.Origin.FEDERAL)
            ).revise(new SbInputName(new SbInput(), "Подмена")),
            "The school view allowed a federal subject to be renamed"
        );
    }

    @Test
    void preservesTheReadableName() throws Exception {
        MatcherAssert.assertThat(
            "The school view lost the shared subject's original name",
            new SbSchool(
                new SbFake(31L, " ФИЗИКА ", Subject.Origin.FEDERAL)
            ).name(),
            Matchers.is(" ФИЗИКА ")
        );
    }
}
