/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject;

import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.Sorts;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.security.Operations;
import java.util.stream.StreamSupport;

/**
 * Federal subject catalogue with administrator-only mutations.
 * For example, {@code new SbsFederalAuthorized(subjects, actor, operations).create(input)}
 * checks the actor's current global administrator role.
 *
 * @since 0.0.1
 */
public final class SbsFederalAuthorized implements Subjects {

    /** Shared operation declarations. */
    private final Operations operations;

    /** Underlying federal catalogue. */
    private final Subjects origin;

    /** Acting user. */
    private final UrAccount actor;

    /**
     * New protected object with shared operation declarations.
     *
     * @param origin Underlying origin
     * @param actor Acting user
     * @param operations Shared operation declarations
     */
    public SbsFederalAuthorized(
        final Subjects origin, final UrAccount actor,
        final Operations operations
    ) {
        this.origin = origin;
        this.actor = actor;
        this.operations = operations;
    }

    @Override
    public Subject create(final SubjectInput input) throws Exception {
        return new SbFederalAuthorized(
            this.operations.operation("FederalSubjects.create")
                .permission(this.actor)
                .permit(this.origin).create(input), this.actor, this.operations
        );
    }

    @Override
    public Subject subject(final long id) throws Exception {
        return new SbFederalAuthorized(
            this.operations.operation("FederalSubjects.subject")
                .permission(this.actor)
                .permit(this.origin).subject(id), this.actor, this.operations
        );
    }

    @Override
    public Subject revise(final long id, final SubjectInput input) throws Exception {
        return new SbFederalAuthorized(
            this.operations.operation("FederalSubjects.revise")
                .permission(this.actor)
                .permit(this.origin).revise(id, input), this.actor, this.operations
        );
    }

    @Override
    public Subjects selection(
        final Filters filters, final Page page, final Sorts sorts
    ) throws Exception {
        return new SbsFederalAuthorized(
            this.operations.operation("FederalSubjects.selection")
                .permission(this.actor)
                .permit(this.origin).selection(filters, page, sorts), this.actor, this.operations
        );
    }

    @Override
    public Iterable<Subject> iterate() {
        return StreamSupport.stream(
            this.operations.operation("FederalSubjects.selection")
                .permission(this.actor)
                .permit(this.origin).iterate().spliterator(), false
        ).<Subject>map(
            subject -> new SbFederalAuthorized(
                subject, this.actor, this.operations
            )
        ).toList();
    }

    @Override
    public void remove(final long id) throws Exception {
        this.operations.operation("FederalSubjects.remove")
            .permission(this.actor)
            .permit(this.origin).remove(id);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.operations.operation("FederalSubjects.selection")
            .permission(this.actor)
            .permit(this.origin).print(media);
    }
}
