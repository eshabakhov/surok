/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum.requirement.filter;

import com.eshabakhov.surok.Filter;
import com.eshabakhov.surok.filter.FilterRequest;
import com.eshabakhov.surok.filter.FiltersRequest;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link FlsCdRequirement}.
 *
 * @since 0.0.1
 */
final class FlsCdRequirementTest {

    @Test
    void delegatesFilters() {
        final Set<Filter> filters = Set.of(
            new FilterRequest("grade", "5")
        );
        MatcherAssert.assertThat(
            "Decorated filters must be returned without replacement",
            new FlsCdRequirement(new FiltersRequest(filters)).filters(),
            Matchers.sameInstance(filters)
        );
    }

    @Test
    void acceptsEverythingWithoutFilters() {
        MatcherAssert.assertThat(
            "Empty filters must accept every requirement",
            new FlsCdRequirement(
                new FiltersRequest(Set.of())
            ).condition(),
            Matchers.is(DSL.trueCondition())
        );
    }

    @Test
    void combinesCanonicalFilters() {
        MatcherAssert.assertThat(
            "Canonical filters must form one condition",
            new FlsCdRequirement(
                new FiltersRequest(
                    new LinkedHashSet<>(
                        List.of(
                            new FilterRequest("grade", "5"),
                            new FilterRequest("subject", "Mathematics"),
                            new FilterRequest("part", "MANDATORY")
                        )
                    )
                )
            ).condition(),
            Matchers.is(
                DSL.trueCondition().and(
                    DSL.condition(
                        """
                            federal_curriculum_requirement.grade = ?
                            """,
                        5
                    )
                ).and(
                    DSL.condition(
                        """
                            EXISTS (
                                SELECT 1 FROM public.subject
                                WHERE subject.id = federal_curriculum_requirement.subject_id
                                    AND subject.name = ?
                                    AND subject.is_deleted = false
                            )
                            """,
                        "Mathematics"
                    )
                ).and(
                    DSL.condition(
                        """
                        federal_curriculum_requirement.part_type = CAST(? AS curriculum_part_type)
                        """,
                        "MANDATORY"
                    )
                )
            )
        );
    }

    @Test
    void combinesAliasFilters() {
        MatcherAssert.assertThat(
            "Alias filters must form one condition",
            new FlsCdRequirement(
                new FiltersRequest(
                    new LinkedHashSet<>(
                        List.of(
                            new FilterRequest("subjectName", "History"),
                            new FilterRequest("partType", "OPTIONAL")
                        )
                    )
                )
            ).condition(),
            Matchers.is(
                DSL.trueCondition().and(
                    DSL.condition(
                        """
                            EXISTS (
                                SELECT 1 FROM public.subject
                                WHERE subject.id = federal_curriculum_requirement.subject_id
                                    AND subject.name = ?
                                    AND subject.is_deleted = false
                            )
                            """,
                        "History"
                    )
                ).and(
                    DSL.condition(
                        """
                        federal_curriculum_requirement.part_type = CAST(? AS curriculum_part_type)
                        """,
                        "OPTIONAL"
                    )
                )
            )
        );
    }

    @Test
    void ignoresUnknownFilter() {
        MatcherAssert.assertThat(
            "Unknown filter must not restrict requirements",
            new FlsCdRequirement(
                new FiltersRequest(
                    Set.of(new FilterRequest("unknown", "value"))
                )
            ).condition(),
            Matchers.is(DSL.trueCondition())
        );
    }

    @Test
    void ignoresFilterWithoutName() {
        MatcherAssert.assertThat(
            "Nameless filter must not restrict requirements",
            new FlsCdRequirement(
                new FiltersRequest(
                    Set.of(new FilterRequest(null, "value"))
                )
            ).condition(),
            Matchers.is(DSL.trueCondition())
        );
    }

    @Test
    void ignoresEmptyKnownFilters() {
        MatcherAssert.assertThat(
            "Empty known filters must not restrict requirements",
            new FlsCdRequirement(
                new FiltersRequest(
                    Set.of(
                        new FilterRequest("grade", ""),
                        new FilterRequest("subject", " "),
                        new FilterRequest("part", null)
                    )
                )
            ).condition(),
            Matchers.is(DSL.trueCondition())
        );
    }

    @Test
    void rejectsMalformedGrade() {
        Assertions.assertThrows(
            NumberFormatException.class,
            () -> new FlsCdRequirement(
                new FiltersRequest(
                    Set.of(new FilterRequest("grade", "fifth"))
                )
            ).condition()
        );
    }

    @Test
    void rejectsMalformedPart() {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new FlsCdRequirement(
                new FiltersRequest(
                    Set.of(new FilterRequest("part", "UNKNOWN"))
                )
            ).condition()
        );
    }
}
