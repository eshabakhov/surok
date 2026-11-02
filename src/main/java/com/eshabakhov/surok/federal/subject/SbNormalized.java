/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject;

import com.eshabakhov.surok.Media;
import java.util.Locale;

/**
 * Subject decorator providing a canonical name for matching.
 * For example, {@code new SbNormalized(subject).name()} ignores case and repeated whitespace
 * while retaining the wrapped subject's identity, ownership and revision behavior.
 *
 * @since 0.0.1
 */
public final class SbNormalized implements Subject {

    /** Original subject. */
    private final Subject subject;

    /**
     * New canonical subject view.
     *
     * @param subject Original subject
     */
    public SbNormalized(final Subject subject) {
        this.subject = subject;
    }

    @Override
    public Long uid() {
        return this.subject.uid();
    }

    @Override
    public String name() throws Exception {
        final String name = this.subject.name().replaceAll("(?U)\\s+", " ")
            .trim().toLowerCase(Locale.ROOT);
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Subject name must not be blank");
        }
        return name;
    }

    @Override
    public Origin origin() throws Exception {
        return this.subject.origin();
    }

    @Override
    public Subject revise(final SubjectInput input) throws Exception {
        return new SbNormalized(this.subject.revise(input));
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.subject.print(media).with("name", this.name());
    }
}
