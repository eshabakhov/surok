/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum.requirement;

import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.Sorts;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.security.Operations;
import java.util.stream.StreamSupport;

/**
 * Federal requirements whose mutations require an administrator.
 * For example, {@code new RqsAuthorized(requirements, user, operations).create(input)}
 * refuses a regular school user.
 *
 * @since 0.0.1
 */
public final class RqsAuthorized implements Requirements {

    /** Shared operation declarations. */
    private final Operations operations;

    /** Underlying requirements. */
    private final Requirements origin;

    /** Acting user. */
    private final UrAccount actor;

    /**
     * New protected object with shared operation declarations.
     *
     * @param origin Underlying origin
     * @param actor Acting user
     * @param operations Shared operation declarations
     */
    public RqsAuthorized(
        final Requirements origin, final UrAccount actor,
        final Operations operations
    ) {
        this.origin = origin;
        this.actor = actor;
        this.operations = operations;
    }

    @Override
    public Iterable<Requirement> optional(final int grade) throws Exception {
        return StreamSupport.stream(
            this.operations.operation("Requirements.selection")
                .permission(this.actor)
                .permit(this.origin).optional(grade).spliterator(), false
        ).<Requirement>map(
            requirement -> new RqAuthorized(
                requirement, this.actor, this.operations
            )
        ).toList();
    }

    @Override
    public void check(final int grade, final int hours) throws Exception {
        this.operations.operation("Requirements.selection")
            .permission(this.actor)
            .permit(this.origin).check(grade, hours);
    }

    @Override
    public Requirement create(final RequirementInput input) throws Exception {
        return new RqAuthorized(
            this.operations.operation("Requirements.create")
                .permission(this.actor)
                .permit(this.origin).create(input), this.actor, this.operations
        );
    }

    @Override
    public Requirement requirement(final long id) throws Exception {
        return new RqAuthorized(
            this.operations.operation("Requirements.requirement")
                .permission(this.actor)
                .permit(this.origin).requirement(id), this.actor, this.operations
        );
    }

    @Override
    public Requirement revise(final long id, final RequirementInput input) throws Exception {
        return new RqAuthorized(
            this.operations.operation("Requirements.revise")
                .permission(this.actor)
                .permit(this.origin).revise(id, input), this.actor, this.operations
        );
    }

    @Override
    public Requirements selection(
        final Filters filters, final Page page, final Sorts sorts
    ) throws Exception {
        return new RqsAuthorized(
            this.operations.operation("Requirements.selection")
                .permission(this.actor)
                .permit(this.origin).selection(filters, page, sorts), this.actor, this.operations
        );
    }

    @Override
    public Iterable<Requirement> iterate() {
        return StreamSupport.stream(
            this.operations.operation("Requirements.selection")
                .permission(this.actor)
                .permit(this.origin).iterate().spliterator(), false
        ).<Requirement>map(
            requirement -> new RqAuthorized(
                requirement, this.actor, this.operations
            )
        ).toList();
    }

    @Override
    public void remove(final long id) throws Exception {
        this.operations.operation("Requirements.remove")
            .permission(this.actor)
            .permit(this.origin).remove(id);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.operations.operation("Requirements.selection")
            .permission(this.actor)
            .permit(this.origin).print(media);
    }
}
