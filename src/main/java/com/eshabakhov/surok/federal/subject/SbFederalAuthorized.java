/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.security.Operations;

/**
 * Federal subject whose revision requires an administrator.
 * For example, {@code new SbFederalAuthorized(subject, actor, operations).revise(input)}
 * rejects a school user without the global administrator role.
 *
 * @since 0.0.1
 */
public final class SbFederalAuthorized implements Subject {

    /** Shared operation declarations. */
    private final Operations operations;

    /** Underlying federal subject. */
    private final Subject subject;

    /** Acting user. */
    private final UrAccount actor;

    /**
     * New protected object with shared operation declarations.
     *
     * @param subject Underlying subject
     * @param actor Acting user
     * @param operations Shared operation declarations
     */
    public SbFederalAuthorized(
        final Subject subject, final UrAccount actor,
        final Operations operations
    ) {
        this.subject = subject;
        this.actor = actor;
        this.operations = operations;
    }

    @Override
    public Long uid() {
        return this.subject.uid();
    }

    @Override
    public String name() throws Exception {
        return this.operations.operation("FederalSubjects.subject")
            .permission(this.actor)
            .permit(this.subject).name();
    }

    @Override
    public Origin origin() throws Exception {
        return this.operations.operation("FederalSubjects.subject")
            .permission(this.actor)
            .permit(this.subject).origin();
    }

    @Override
    public Subject revise(final SubjectInput input) throws Exception {
        return new SbFederalAuthorized(
            this.operations.operation("FederalSubjects.revise")
                .permission(this.actor)
                .permit(this.subject).revise(input), this.actor, this.operations
        );
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.operations.operation("FederalSubjects.subject")
            .permission(this.actor)
            .permit(this.subject).print(media);
    }
}
