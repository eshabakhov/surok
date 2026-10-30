/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject.sort;

import com.eshabakhov.surok.Sort;
import com.eshabakhov.surok.Sorts;
import com.eshabakhov.surok.sort.StsJooq;
import java.util.ArrayList;
import java.util.List;
import org.jooq.SortField;
import org.jooq.impl.DSL;

/**
 * School subject JOOQ sorting parameters.
 *
 * @since 0.0.1
 */
public final class StsJqSubject implements StsJooq {

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
    public StsJqSubject(final Sorts origin) {
        this.origin = origin;
    }

    @Override
    public List<Sort> sorts() {
        return this.origin.sorts();
    }

    @Override
    public List<SortField<?>> fields() {
        final List<SortField<?>> fields = new ArrayList<>(this.origin.sorts().size());
        for (final Sort sort : this.origin.sorts()) {
            final SortField<?> field = new StJqSubject(sort).field();
            if (field != null) {
                fields.add(field);
            }
        }
        if (fields.isEmpty()) {
            fields.add(
                DSL.field(DSL.name("public", "subject", "name"), String.class).asc()
            );
        }
        fields.add(DSL.field(DSL.name("public", "subject", "id"), Long.class).asc());
        return fields;
    }
}
