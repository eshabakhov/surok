/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.security.Operations;

/**
 * School subject whose revision follows the actor's current school role.
 * For example, {@code new SbAuthorized(subject, school, actor, operations).revise(input)}
 * refuses a member who lost the school's director role.
 *
 * @since 0.0.1
 */
public final class SbAuthorized implements Subject {

    /** Shared operation declarations. */
    private final Operations operations;

    /** Underlying school subject. */
    private final Subject subject;

    /** Owning school. */
    private final School school;

    /** Acting user. */
    private final UrAccount actor;

    /**
     * New protected object with shared operation declarations.
     *
     * @param subject Underlying subject
     * @param school Underlying school
     * @param actor Acting user
     * @param operations Shared operation declarations
     */
    public SbAuthorized(
        final Subject subject, final School school, final UrAccount actor,
        final Operations operations
    ) {
        this.subject = subject;
        this.school = school;
        this.actor = actor;
        this.operations = operations;
    }

    @Override
    public Long uid() {
        return this.subject.uid();
    }

    @Override
    public String name() throws Exception {
        return this.operations.operation("SchoolSubjects.subject")
            .permission(this.actor, this.school)
            .permit(this.subject).name();
    }

    @Override
    public Origin origin() throws Exception {
        return this.operations.operation("SchoolSubjects.subject")
            .permission(this.actor, this.school)
            .permit(this.subject).origin();
    }

    @Override
    public Subject revise(final SubjectInput input) throws Exception {
        return new SbAuthorized(
            this.operations.operation("SchoolSubjects.revise")
                .permission(this.actor, this.school)
                .permit(this.subject).revise(input),
            this.school, this.actor, this.operations
        );
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.operations.operation("SchoolSubjects.subject")
            .permission(this.actor, this.school)
            .permit(this.subject).print(media);
    }
}
