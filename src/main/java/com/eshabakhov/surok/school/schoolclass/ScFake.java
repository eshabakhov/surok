/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schoolclass;

import com.eshabakhov.surok.Media;
import lombok.EqualsAndHashCode;

/**
 * A read-only school class identity without database access.
 * Instances with the same identifier are equal.
 * For example, {@code new ScFake(17L, 7)} represents a seventh-grade class
 * in an isolated calculation.
 *
 * @since 0.0.1
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class ScFake implements SchoolClass {

    /** Class identifier. */
    @EqualsAndHashCode.Include
    private final long identifier;

    /** School grade. */
    private final int grade;

    /**
     * New first-grade class identity.
     *
     * @param identifier Class identifier
     */
    public ScFake(final long identifier) {
        this(identifier, 1);
    }

    /**
     * New class identity for the supplied grade.
     *
     * @param identifier Class identifier
     * @param grade School grade
     */
    public ScFake(final long identifier, final int grade) {
        this.identifier = identifier;
        this.grade = grade;
    }

    @Override
    public Long uid() {
        return this.identifier;
    }

    @Override
    public int grade() {
        return this.grade;
    }

    @Override
    public SchoolClass revise(final SchoolClassInput input) {
        throw new UnsupportedOperationException(
            String.format("Read-only fake school class %d cannot be revised", this.identifier)
        );
    }

    @Override
    public <T> Media<T> print(final Media<T> media) {
        return media.with("id", this.identifier);
    }
}
