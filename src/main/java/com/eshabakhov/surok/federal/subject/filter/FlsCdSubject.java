/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject.filter;

import com.eshabakhov.surok.Filter;
import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.filter.FlCdTrue;
import com.eshabakhov.surok.filter.FlConditional;
import com.eshabakhov.surok.filter.FlsConditional;
import java.util.Set;
import org.jooq.Condition;

/**
 * School subject conditional filters.
 *
 * @since 0.0.1
 */
public final class FlsCdSubject implements FlsConditional {

    /**
     * Origin filters.
     */
    private final Filters origin;

    /**
     * Ctor.
     *
     * @param origin Origin filters
     * @since 0.0.1
     */
    public FlsCdSubject(final Filters origin) {
        this.origin = origin;
    }

    @Override
    public Set<Filter> filters() {
        return this.origin.filters();
    }

    @Override
    public Condition condition() {
        FlConditional filter = new FlCdTrue();
        for (final Filter item : this.origin.filters()) {
            if ("name".equals(item.name()) || "name_ct".equals(item.name())) {
                filter = new FlCdSubjectByName(filter, item.value());
            }
        }
        return filter.condition();
    }
}
