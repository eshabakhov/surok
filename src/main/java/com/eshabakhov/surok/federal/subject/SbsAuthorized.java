/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject;

import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.Sorts;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.security.Operations;
import java.util.stream.StreamSupport;

/**
 * School subject catalogue protected by the actor's current school role.
 * For example, {@code new SbsAuthorized(subjects, school, actor, operations).create(input)}
 * permits the school's director or deputy to add a local subject.
 *
 * @since 0.0.1
 */
public final class SbsAuthorized implements Subjects {

    /** Shared operation declarations. */
    private final Operations operations;

    /** Underlying school catalogue. */
    private final Subjects origin;

    /** Owning school. */
    private final School school;

    /** Acting user. */
    private final UrAccount actor;

    /**
     * New protected object with shared operation declarations.
     *
     * @param origin Underlying origin
     * @param school Underlying school
     * @param actor Acting user
     * @param operations Shared operation declarations
     */
    public SbsAuthorized(
        final Subjects origin, final School school, final UrAccount actor,
        final Operations operations
    ) {
        this.origin = origin;
        this.school = school;
        this.actor = actor;
        this.operations = operations;
    }

    @Override
    public Subject create(final SubjectInput input) throws Exception {
        return new SbAuthorized(
            this.operations.operation("SchoolSubjects.create")
                .permission(this.actor, this.school)
                .permit(this.origin).create(input),
            this.school, this.actor, this.operations
        );
    }

    @Override
    public Subject subject(final long id) throws Exception {
        return new SbAuthorized(
            this.operations.operation("SchoolSubjects.subject")
                .permission(this.actor, this.school)
                .permit(this.origin).subject(id),
            this.school, this.actor, this.operations
        );
    }

    @Override
    public Subject revise(final long id, final SubjectInput input) throws Exception {
        return new SbAuthorized(
            this.operations.operation("SchoolSubjects.revise")
                .permission(this.actor, this.school)
                .permit(this.origin).revise(id, input),
            this.school, this.actor, this.operations
        );
    }

    @Override
    public Subjects selection(
        final Filters filters, final Page page, final Sorts sorts
    ) throws Exception {
        return new SbsAuthorized(
            this.operations.operation("SchoolSubjects.selection")
                .permission(this.actor, this.school)
                .permit(this.origin).selection(filters, page, sorts),
            this.school, this.actor, this.operations
        );
    }

    @Override
    public Iterable<Subject> iterate() {
        return StreamSupport.stream(
            this.operations.operation("SchoolSubjects.selection")
                .permission(this.actor, this.school)
                .permit(this.origin).iterate().spliterator(), false
        ).<Subject>map(
            subject -> new SbAuthorized(subject, this.school, this.actor, this.operations)
        ).toList();
    }

    @Override
    public void remove(final long id) throws Exception {
        this.operations.operation("SchoolSubjects.remove")
            .permission(this.actor, this.school)
            .permit(this.origin).remove(id);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.operations.operation("SchoolSubjects.selection")
            .permission(this.actor, this.school)
            .permit(this.origin).print(media);
    }
}
