/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.security.Operations;
import java.util.stream.StreamSupport;

/**
 * Applied teaching loads visible to members of their school.
 * For example, a student can list the loads of a selected schedule.
 *
 * @since 0.0.1
 */
public final class ClsAuthorized implements ClassLoads {

    /** Shared operation declarations. */
    private final Operations operations;

    /** Underlying teaching loads. */
    private final ClassLoads origin;

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
    public ClsAuthorized(
        final ClassLoads origin, final School school, final UrAccount actor,
        final Operations operations
    ) {
        this.origin = origin;
        this.school = school;
        this.actor = actor;
        this.operations = operations;
    }

    @Override
    public ClassLoad load(final long id) throws Exception {
        return new ClAuthorized(
            this.operations.operation("ClassLoads.load")
                .permission(this.actor, this.school)
                .permit(this.origin).load(id),
            this.school, this.actor, this.operations
        );
    }

    @Override
    public ClassLoads selection(final Page page) throws Exception {
        return new ClsAuthorized(
            this.operations.operation("ClassLoads.selection")
                .permission(this.actor, this.school)
                .permit(this.origin).selection(page),
            this.school, this.actor, this.operations
        );
    }

    @Override
    public Iterable<ClassLoad> iterate() {
        return StreamSupport.stream(
            this.operations.operation("ClassLoads.selection")
                .permission(this.actor, this.school)
                .permit(this.origin).iterate().spliterator(), false
        ).<ClassLoad>map(
            load -> new ClAuthorized(load, this.school, this.actor, this.operations)
        ).toList();
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.operations.operation("ClassLoads.selection")
            .permission(this.actor, this.school)
            .permit(this.origin).print(media);
    }
}
