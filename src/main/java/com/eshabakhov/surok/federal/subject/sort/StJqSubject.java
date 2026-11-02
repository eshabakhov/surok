/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject.sort;

import com.eshabakhov.surok.Sort;
import com.eshabakhov.surok.sort.StJooq;
import java.util.Optional;
import org.jooq.Field;
import org.jooq.SortField;
import org.jooq.impl.DSL;

/**
 * School subject JOOQ sorting parameter.
 *
 * @since 0.0.1
 */
public final class StJqSubject implements StJooq {

    /**
     * Origin sorting parameter.
     */
    private final Sort origin;

    /**
     * Ctor.
     *
     * @param origin Origin sorting parameter
     * @since 0.0.1
     */
    public StJqSubject(final Sort origin) {
        this.origin = origin;
    }

    @Override
    public String name() {
        return this.origin.name();
    }

    @Override
    public Direction direction() {
        return this.origin.direction();
    }

    @Override
    public SortField<?> field() {
        final Field<?> field = switch (this.origin.name()) {
            case null -> null;
            case "name" -> DSL.field(
                DSL.name("public", "subject", "name"), String.class
            );
            default -> null;
        };
        return switch (this.origin.direction()) {
            case null -> null;
            case NONE -> null;
            case ASC -> Optional.ofNullable(field).map(Field::asc).orElse(null);
            case DESC -> Optional.ofNullable(field).map(Field::desc).orElse(null);
        };
    }
}
