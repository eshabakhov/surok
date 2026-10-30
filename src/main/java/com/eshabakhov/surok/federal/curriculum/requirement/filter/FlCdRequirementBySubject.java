/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum.requirement.filter;

import com.eshabakhov.surok.filter.FlConditional;
import com.eshabakhov.surok.sql.ClasspathSql;
import org.jooq.Condition;
import org.jooq.impl.DSL;

/**
 * Federal curriculum requirement subject filter.
 *
 * @since 0.0.1
 */
public final class FlCdRequirementBySubject implements FlConditional {

    /**
     * Origin filter.
     */
    private final FlConditional origin;

    /**
     * Subject to search.
     */
    private final String subject;

    /**
     * New subject filter.
     *
     * @param origin Origin filter
     * @param subject Subject to search
     */
    public FlCdRequirementBySubject(final FlConditional origin, final String subject) {
        this.origin = origin;
        this.subject = subject;
    }

    @Override
    public Condition condition() {
        Condition condition = this.origin.condition();
        if (this.subject != null && !this.subject.isBlank()) {
            condition = condition.and(
                DSL.condition(
                    new ClasspathSql(
                        "curriculum/requirement/conditions/by-subject-name.sql"
                    ).text(),
                    this.subject.trim()
                )
            );
        }
        return condition;
    }
}
