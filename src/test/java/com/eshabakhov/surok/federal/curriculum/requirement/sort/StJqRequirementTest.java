/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum.requirement.sort;

import com.eshabakhov.surok.Sort;
import com.eshabakhov.surok.sort.SortRequest;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link StJqRequirement}.
 *
 * @since 0.0.1
 */
final class StJqRequirementTest {

    @Test
    void delegatesName() {
        MatcherAssert.assertThat(
            "Sorting name must be delegated",
            new StJqRequirement(
                new SortRequest("grade", Sort.Direction.ASC)
            ).name(),
            Matchers.is("grade")
        );
    }

    @Test
    void delegatesDirection() {
        MatcherAssert.assertThat(
            "Sorting direction must be delegated",
            new StJqRequirement(
                new SortRequest("grade", Sort.Direction.DESC)
            ).direction(),
            Matchers.is(Sort.Direction.DESC)
        );
    }

    @Test
    void createsAscendingFields() {
        MatcherAssert.assertThat(
            "Every requirement field must support ascending sorting",
            List.of(
                new StJqRequirement(
                    new SortRequest("grade", Sort.Direction.ASC)
                ).field(),
                new StJqRequirement(
                    new SortRequest("subject", Sort.Direction.ASC)
                ).field(),
                new StJqRequirement(
                    new SortRequest("hours", Sort.Direction.ASC)
                ).field(),
                new StJqRequirement(
                    new SortRequest("part", Sort.Direction.ASC)
                ).field()
            ),
            Matchers.contains(
                DSL.field(
                    DSL.name("public", "federal_curriculum_requirement", "grade"),
                    Integer.class
                ).asc(),
                DSL.field(
                    DSL.name("public", "subject", "name"),
                    String.class
                ).asc(),
                DSL.field(DSL.name("hours"), Integer.class).asc(),
                DSL.field(
                    DSL.name("public", "federal_curriculum_requirement", "part_type"),
                    String.class
                ).asc()
            )
        );
    }

    @Test
    void createsDescendingField() {
        MatcherAssert.assertThat(
            "Requirement field must support descending sorting",
            new StJqRequirement(
                new SortRequest("grade", Sort.Direction.DESC)
            ).field(),
            Matchers.is(
                DSL.field(
                    DSL.name("public", "federal_curriculum_requirement", "grade"),
                    Integer.class
                ).desc()
            )
        );
    }

    @Test
    void ignoresDisabledSorting() {
        MatcherAssert.assertThat(
            "Disabled sorting must not produce a field",
            new StJqRequirement(
                new SortRequest("grade", Sort.Direction.NONE)
            ).field(),
            Matchers.nullValue()
        );
    }

    @Test
    void ignoresSortingWithoutDirection() {
        MatcherAssert.assertThat(
            "Sorting without a direction must not produce a field",
            new StJqRequirement(
                new SortRequest("grade", null)
            ).field(),
            Matchers.nullValue()
        );
    }

    @Test
    void ignoresSortingWithoutName() {
        MatcherAssert.assertThat(
            "Sorting without a name must not produce a field",
            new StJqRequirement(
                new SortRequest(null, Sort.Direction.ASC)
            ).field(),
            Matchers.nullValue()
        );
    }

    @Test
    void ignoresSortingWithEmptyName() {
        MatcherAssert.assertThat(
            "Sorting with an empty name must not produce a field",
            new StJqRequirement(
                new SortRequest("", Sort.Direction.ASC)
            ).field(),
            Matchers.nullValue()
        );
    }

    @Test
    void ignoresSortingWithUnknownName() {
        MatcherAssert.assertThat(
            "Sorting with an unknown name must not produce a field",
            new StJqRequirement(
                new SortRequest("unknown", Sort.Direction.DESC)
            ).field(),
            Matchers.nullValue()
        );
    }
}
