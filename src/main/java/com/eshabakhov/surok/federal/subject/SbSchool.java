/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject;

import com.eshabakhov.surok.Media;

/**
 * A school's view of a subject, protecting common federal entries from local changes.
 * For example, {@code new SbSchool(federal).revise(input)} rejects a local rename.
 *
 * @since 0.0.1
 */
public final class SbSchool implements Subject {

    /** Visible subject. */
    private final Subject subject;

    /**
     * New school view.
     *
     * @param subject Subject already selected within the school's scope
     */
    public SbSchool(final Subject subject) {
        this.subject = subject;
    }

    @Override
    public Long uid() {
        return this.subject.uid();
    }

    @Override
    public String name() throws Exception {
        return this.subject.name();
    }

    @Override
    public Origin origin() throws Exception {
        return this.subject.origin();
    }

    @Override
    public Subject revise(final SubjectInput input) throws Exception {
        if (this.origin() == Origin.FEDERAL) {
            throw new SubjectReadOnlyException(this.uid());
        }
        return new SbSchool(this.subject.revise(input));
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.subject.print(media);
    }
}
