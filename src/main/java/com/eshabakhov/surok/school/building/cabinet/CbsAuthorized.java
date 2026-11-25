/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.building.cabinet;

import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.Sorts;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.security.Operations;
import java.util.stream.StreamSupport;

/**
 * Building cabinets protected by the acting user's school role.
 * For example, {@code new CbsAuthorized(items, school, actor, operations).create(input)}
 * permits creation by a director or deputy director.
 *
 * @since 0.0.1
 */
public final class CbsAuthorized implements Cabinets {

    /** Shared operation declarations. */
    private final Operations operations;

    /** Underlying cabinets. */
    private final Cabinets origin;

    /** School that owns the cabinets. */
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
    public CbsAuthorized(
        final Cabinets origin, final School school, final UrAccount actor,
        final Operations operations
    ) {
        this.origin = origin;
        this.school = school;
        this.actor = actor;
        this.operations = operations;
    }

    @Override
    public Cabinet create(final CabinetInput input) throws Exception {
        return new CbAuthorized(
            this.operations.operation("Cabinets.create")
                .permission(this.actor, this.school)
                .permit(this.origin).create(input),
            this.school, this.actor, this.operations
        );
    }

    @Override
    public Cabinet cabinet(final long id) throws Exception {
        return new CbAuthorized(
            this.operations.operation("Cabinets.cabinet")
                .permission(this.actor, this.school)
                .permit(this.origin).cabinet(id),
            this.school, this.actor, this.operations
        );
    }

    @Override
    public Cabinets selection(
        final Filters filters, final Page page, final Sorts sorts
    ) throws Exception {
        return new CbsAuthorized(
            this.operations.operation("Cabinets.selection")
                .permission(this.actor, this.school)
                .permit(this.origin).selection(
                filters, page, sorts
            ), this.school, this.actor, this.operations
        );
    }

    @Override
    public Iterable<Cabinet> iterate() {
        return StreamSupport.stream(
            this.operations.operation("Cabinets.selection")
                .permission(this.actor, this.school)
                .permit(this.origin).iterate().spliterator(), false
        ).<Cabinet>map(
            cabinet -> new CbAuthorized(cabinet, this.school, this.actor, this.operations)
        ).toList();
    }

    @Override
    public void remove(final long id) throws Exception {
        this.operations.operation("Cabinets.remove")
            .permission(this.actor, this.school)
            .permit(this.origin).remove(id);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.operations.operation("Cabinets.selection")
            .permission(this.actor, this.school)
            .permit(this.origin).print(media);
    }
}
