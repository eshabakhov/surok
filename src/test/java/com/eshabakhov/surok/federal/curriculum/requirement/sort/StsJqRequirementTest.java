/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum.requirement.sort;

import com.eshabakhov.surok.Sort;
import com.eshabakhov.surok.sort.SortRequest;
import com.eshabakhov.surok.sort.SortsRequest;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link StsJqRequirement}.
 *
 * @since 0.0.1
 */
final class StsJqRequirementTest {

    @Test
    void delegatesSorts() {
        final List<Sort> sorts = List.of(
            new SortRequest("grade", Sort.Direction.ASC)
        );
        MatcherAssert.assertThat(
            "Decorated sorts must be returned without replacement",
            new StsJqRequirement(new SortsRequest(sorts)).sorts(),
            Matchers.sameInstance(sorts)
        );
    }

    @Test
    void usesCanonicalFieldOrder() {
        MatcherAssert.assertThat(
            "Requirement fields must follow their canonical priority",
            new StsJqRequirement(
                new SortsRequest(
                    List.of(
                        new SortRequest("part", Sort.Direction.DESC),
                        new SortRequest("hours", Sort.Direction.ASC),
                        new SortRequest("subject", Sort.Direction.DESC),
                        new SortRequest("grade", Sort.Direction.ASC)
                    )
                )
            ).fields(),
            Matchers.contains(
                DSL.field(
                    DSL.name("public", "federal_curriculum_requirement", "grade"),
                    Integer.class
                ).asc(),
                DSL.field(
                    DSL.name("public", "subject", "name"),
                    String.class
                ).desc(),
                DSL.field(DSL.name("hours"), Integer.class).asc(),
                DSL.field(
                    DSL.name("public", "federal_curriculum_requirement", "part_type"),
                    String.class
                ).desc()
            )
        );
    }

    @Test
    void keepsDuplicateFieldOrder() {
        MatcherAssert.assertThat(
            "Duplicate fields must preserve their request order",
            new StsJqRequirement(
                new SortsRequest(
                    List.of(
                        new SortRequest("grade", Sort.Direction.DESC),
                        new SortRequest("grade", Sort.Direction.ASC)
                    )
                )
            ).fields(),
            Matchers.contains(
                DSL.field(
                    DSL.name("public", "federal_curriculum_requirement", "grade"),
                    Integer.class
                ).desc(),
                DSL.field(
                    DSL.name("public", "federal_curriculum_requirement", "grade"),
                    Integer.class
                ).asc()
            )
        );
    }

    @Test
    void usesDefaultFieldsWithoutSorts() {
        MatcherAssert.assertThat(
            "Empty sorts must produce the default requirement order",
            new StsJqRequirement(
                new SortsRequest(List.of())
            ).fields(),
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
    void usesDefaultFieldsForInapplicableSorts() {
        MatcherAssert.assertThat(
            "Inapplicable sorts must produce the default requirement order",
            new StsJqRequirement(
                new SortsRequest(
                    List.of(
                        new SortRequest(null, Sort.Direction.ASC),
                        new SortRequest("", Sort.Direction.DESC),
                        new SortRequest("unknown", Sort.Direction.ASC),
                        new SortRequest("grade", Sort.Direction.NONE),
                        new SortRequest("subject", null)
                    )
                )
            ).fields(),
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
    void rejectsNullSorts() {
        Assertions.assertThrows(
            NullPointerException.class,
            () -> new StsJqRequirement(
                new SortsRequest(null)
            ).fields()
        );
    }
}
