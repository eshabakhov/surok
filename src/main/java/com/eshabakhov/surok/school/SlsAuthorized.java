/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school;

import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.Schools;
import com.eshabakhov.surok.Sorts;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.security.Operations;
import java.util.stream.StreamSupport;

/**
 * Schools selected for the current user with school operations guarded.
 * For example, {@code new SlsAuthorized(actor.schools(), actor, operations).school(id)}
 * returns a school whose revision checks the actor's current school role.
 *
 * @since 0.0.1
 */
public final class SlsAuthorized implements Schools {

    /** Shared operation declarations. */
    private final Operations operations;

    /** Collection scoped to the actor at creation time. */
    private final Schools origin;

    /** Acting user. */
    private final UrAccount actor;

    /**
     * New protected object with shared operation declarations.
     *
     * @param origin Underlying origin
     * @param actor Acting user
     * @param operations Shared operation declarations
     */
    public SlsAuthorized(
        final Schools origin, final UrAccount actor,
        final Operations operations
    ) {
        this.origin = origin;
        this.actor = actor;
        this.operations = operations;
    }

    @Override
    public boolean contains(final long id) {
        return this.origin.contains(id)
            && this.operations.operation("Schools.school").permission(this.actor, id).allows();
    }

    @Override
    public School create(final SchoolInput input) throws Exception {
        return new SlAuthorized(
            this.operations.operation("Schools.create")
                .permission(this.actor)
                .permit(this.origin).create(input), this.actor, this.operations
        );
    }

    @Override
    public School school(final long id) throws Exception {
        return new SlAuthorized(
            this.operations.operation("Schools.school")
                .permission(this.actor, id)
                .permit(this.origin).school(id), this.actor, this.operations
        );
    }

    @Override
    public Schools selection(
        final Filters filters, final Page page, final Sorts sorts
    ) throws Exception {
        return new SlsAuthorized(
            this.operations.operation("Schools.selection").permission(this.actor)
                .permit(this.origin).selection(filters, page, sorts), this.actor, this.operations
        );
    }

    @Override
    public Iterable<School> iterate() {
        return StreamSupport.stream(
            this.operations.operation("Schools.selection").permission(this.actor)
                .permit(this.origin).iterate().spliterator(), false
        ).filter(
            school -> this.operations.operation("Schools.school")
                .permission(this.actor, school.uid()).allows()
        ).<School>map(school -> new SlAuthorized(school, this.actor, this.operations)).toList();
    }

    @Override
    public void remove(final long id) throws Exception {
        this.operations.operation("Schools.remove")
            .permission(this.actor, id)
            .permit(this.origin).remove(id);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        for (final School school : this.origin.iterate()) {
            this.operations.operation("Schools.school")
                .permission(this.actor, school)
                .permit(school);
        }
        return this.operations.operation("Schools.selection").permission(this.actor)
            .permit(this.origin).print(media);
    }
}
