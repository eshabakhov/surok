/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.building;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.school.building.cabinet.Cabinets;
import com.eshabakhov.surok.school.building.cabinet.CbsAuthorized;
import com.eshabakhov.surok.security.Operations;

/**
 * Building that checks the acting user's school role for each operation.
 * For example, {@code new BdAuthorized(building, school, actor, operations).cabinets()}
 * returns cabinets that keep the same authorization boundary.
 *
 * @since 0.0.1
 */
public final class BdAuthorized implements Building {

    /** Shared operation permissions. */
    private final Operations operations;

    /** Underlying building. */
    private final Building origin;

    /** School that owns the building. */
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
    public BdAuthorized(
        final Building origin, final School school, final UrAccount actor,
        final Operations operations
    ) {
        this.operations = operations;
        this.origin = origin;
        this.school = school;
        this.actor = actor;
    }

    @Override
    public Long uid() {
        return this.origin.uid();
    }

    @Override
    public Building revise(final BuildingInput input) throws Exception {
        return new BdAuthorized(
            this.operations.operation("Building.revise")
                .permission(this.actor, this.school)
                .permit(this.origin)
                .revise(input),
            this.school,
            this.actor,
            this.operations
        );
    }

    @Override
    public Cabinets cabinets() {
        return new CbsAuthorized(
            this.operations.operation("Cabinets.selection")
                .permission(this.actor, this.school)
                .permit(this.origin)
                .cabinets(),
            this.school,
            this.actor, this.operations
        );
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.operations.operation("Buildings.building")
            .permission(this.actor, this.school)
            .permit(this.origin)
            .print(media);
    }
}
