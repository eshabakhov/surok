/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.building;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.school.building.cabinet.Cabinets;
import com.eshabakhov.surok.school.building.cabinet.CbsAuthorized;
import com.eshabakhov.surok.security.permission.PmYaml;

/**
 * Building that checks the acting user's school role for each operation.
 * For example, {@code new BdAuthorized(building, school, actor).cabinets()}
 * returns cabinets that keep the same authorization boundary.
 *
 * @since 0.0.1
 */
public final class BdAuthorized implements Building {

    /** Shared operation permissions. */
    private final PmYaml permissions;

    /** Underlying building. */
    private final Building origin;

    /** School that owns the building. */
    private final School school;

    /** Acting user. */
    private final UrAccount actor;

    /**
     * New protected building.
     *
     * @param origin Underlying building
     * @param school School that owns it
     * @param actor Acting user
     */
    public BdAuthorized(final Building origin, final School school, final UrAccount actor) {
        this(origin, school, actor, PmYaml.PACKAGED);
    }

    /**
     * New protected resource with the supplied operation permissions.
     *
     * @param origin Underlying resource
     * @param school Target school
     * @param actor Acting account
     * @param permissions Shared permission catalog
     */
    BdAuthorized(
        final Building origin, final School school, final UrAccount actor,
        final PmYaml permissions
    ) {
        this.permissions = permissions;
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
            this.permissions.forOperation("Building.revise")
                .right(this.actor, this.school)
                .permit(this.origin).revise(input),
            this.school, this.actor, this.permissions
        );
    }

    @Override
    public Cabinets cabinets() {
        return new CbsAuthorized(
            this.permissions.forOperation("Buildings.building")
                .right(this.actor, this.school)
                .permit(this.origin).cabinets(),
            this.school, this.actor
        );
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.permissions.forOperation("Buildings.building")
            .right(this.actor, this.school)
            .permit(this.origin).print(media);
    }
}
