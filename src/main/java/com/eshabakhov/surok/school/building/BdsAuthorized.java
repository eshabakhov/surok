/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.building;

import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.Sorts;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.security.Operations;
import java.util.stream.StreamSupport;

/**
 * School buildings protected by the acting user's current school role.
 * For example, {@code new BdsAuthorized(items, school, actor, operations).create(input)}
 * allows a director or deputy director to add a building.
 *
 * @since 0.0.1
 */
public final class BdsAuthorized implements Buildings {

    /** Shared operation permissions. */
    private final Operations operations;

    /** Underlying buildings. */
    private final Buildings origin;

    /** School that owns the buildings. */
    private final School school;

    /** Acting user. */
    private final UrAccount actor;

    /**
     * New protected resource with the supplied operation permissions.
     *
     * @param origin Underlying resource
     * @param school Target school
     * @param actor Acting account
     * @param operations Described operations
     */
    public BdsAuthorized(
        final Buildings origin, final School school, final UrAccount actor,
        final Operations operations
    ) {
        this.operations = operations;
        this.origin = origin;
        this.school = school;
        this.actor = actor;
    }

    @Override
    public Building create(final BuildingInput input) throws Exception {
        return new BdAuthorized(
            this.operations.operation("Buildings.create")
                .permission(this.actor, this.school)
                .permit(this.origin).create(input),
            this.school, this.actor, this.operations
        );
    }

    @Override
    public Building revise(final long id, final BuildingInput input) throws Exception {
        return new BdAuthorized(
            this.operations.operation("Building.revise").permission(this.actor, this.school)
                .permit(this.origin).revise(id, input),
            this.school, this.actor, this.operations
        );
    }

    @Override
    public Building building(final long id) throws Exception {
        return new BdAuthorized(
            this.operations.operation("Buildings.building")
                .permission(this.actor, this.school)
                .permit(this.origin).building(id),
            this.school, this.actor, this.operations
        );
    }

    @Override
    public Buildings selection(
        final Filters filters, final Page page, final Sorts sorts
    ) throws Exception {
        return new BdsAuthorized(
            this.operations.operation("Buildings.selection")
                .permission(this.actor, this.school)
                .permit(this.origin).selection(filters, page, sorts),
            this.school, this.actor, this.operations
        );
    }

    @Override
    public Iterable<Building> iterate() {
        return StreamSupport.stream(
            this.operations.operation("Buildings.selection")
                .permission(this.actor, this.school).permit(this.origin)
                .iterate().spliterator(), false
        ).<Building>map(
            building -> new BdAuthorized(building, this.school, this.actor, this.operations)
        ).toList();
    }

    @Override
    public void remove(final long id) throws Exception {
        this.operations.operation("Buildings.remove")
            .permission(this.actor, this.school)
            .permit(this.origin).remove(id);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.operations.operation("Buildings.selection")
            .permission(this.actor, this.school)
            .permit(this.origin).print(media);
    }
}
