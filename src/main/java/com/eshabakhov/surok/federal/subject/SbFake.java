/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject;

import com.eshabakhov.surok.Media;
import lombok.EqualsAndHashCode;

/**
 * A read-only school subject identity without database access.
 * Instances with the same identifier are equal.
 * For example, {@code new SbFake(29L)} represents a subject in an isolated calculation.
 *
 * @since 0.0.1
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class SbFake implements Subject {

    /** Subject identifier. */
    @EqualsAndHashCode.Include
    private final long identifier;

    /** Subject name used in isolated calculations. */
    private final String name;

    /** Owning catalogue. */
    private final Origin source;

    /**
     * New subject identity.
     *
     * @param identifier Subject identifier
     */
    public SbFake(final long identifier) {
        this(identifier, Long.toString(identifier));
    }

    /**
     * New named subject identity.
     *
     * @param identifier Subject identifier
     * @param name Subject name
     */
    public SbFake(final long identifier, final String name) {
        this(identifier, name, Origin.SCHOOL);
    }

    /**
     * New subject with an explicit catalogue.
     *
     * @param identifier Subject identifier
     * @param name Display name
     * @param source Owning catalogue
     */
    public SbFake(final long identifier, final String name, final Origin source) {
        this.identifier = identifier;
        this.name = name;
        this.source = source;
    }

    @Override
    public Long uid() {
        return this.identifier;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public Origin origin() {
        return this.source;
    }

    @Override
    public Subject revise(final SubjectInput input) {
        throw new UnsupportedOperationException(
            String.format("Read-only fake school subject %d cannot be revised", this.identifier)
        );
    }

    @Override
    public <T> Media<T> print(final Media<T> media) {
        return media.with("id", this.identifier);
    }
}
