/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum.requirement.sort;

import com.eshabakhov.surok.Sort;
import com.eshabakhov.surok.Sorts;
import com.eshabakhov.surok.sort.StJooq;
import com.eshabakhov.surok.sort.StsJooq;
import java.util.ArrayList;
import java.util.List;
import org.jooq.SortField;
import org.jooq.impl.DSL;

/**
 * Federal curriculum requirement JOOQ sorting parameters.
 *
 * @since 0.0.1
 */
public final class StsJqRequirement implements StsJooq {

    /**
     * Origin sorting parameters.
     */
    private final Sorts origin;

    /**
     * Ctor.
     *
     * @param origin Origin sorting parameters
     * @since 0.0.1
     */
    public StsJqRequirement(final Sorts origin) {
        this.origin = origin;
    }

    @Override
    public List<Sort> sorts() {
        return this.origin.sorts();
    }

    @Override
    public List<SortField<?>> fields() {
        final List<SortField<?>> fields = new ArrayList<>(this.origin.sorts().size());
        for (final String name : List.of("grade", "subject", "hours", "part")) {
            for (final Sort sort : this.origin.sorts()) {
                if (name.equals(sort.name())) {
                    final StJooq field = new StJqRequirement(sort);
                    if (field.field() != null) {
                        fields.add(field.field());
                    }
                }
            }
        }
        if (fields.isEmpty()) {
            fields.add(
                DSL.field(
                    DSL.name("public", "federal_curriculum_requirement", "grade"),
                    Integer.class
                ).asc()
            );
            fields.add(
                DSL.field(
                    DSL.name("public", "subject", "name"),
                    String.class
                ).asc()
            );
            fields.add(
                DSL.field(DSL.name("hours"), Integer.class).asc()
            );
            fields.add(
                DSL.field(
                    DSL.name("public", "federal_curriculum_requirement", "part_type"),
                    String.class
                ).asc()
            );
        }
        return fields;
    }
}
