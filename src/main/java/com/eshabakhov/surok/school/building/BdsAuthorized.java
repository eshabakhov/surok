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
import com.eshabakhov.surok.security.permission.PmYaml;
import java.util.stream.StreamSupport;

/**
 * School buildings protected by the acting user's current school role.
 * For example, {@code new BdsAuthorized(school.buildings(), school, actor).create(input)}
 * allows a director or deputy director to add a building.
 *
 * @since 0.0.1
 */
public final class BdsAuthorized implements Buildings {

    /** Shared operation permissions. */
    private final PmYaml permissions;

    /** Underlying buildings. */
    private final Buildings origin;

    /** School that owns the buildings. */
    private final School school;

    /** Acting user. */
    private final UrAccount actor;

    /**
     * New protected building collection.
     *
     * @param origin Underlying buildings
     * @param school School that owns them
     * @param actor Acting user
     */
    public BdsAuthorized(final Buildings origin, final School school, final UrAccount actor) {
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
    BdsAuthorized(
        final Buildings origin, final School school, final UrAccount actor,
        final PmYaml permissions
    ) {
        this.permissions = permissions;
        this.origin = origin;
        this.school = school;
        this.actor = actor;
    }

    @Override
    public Building create(final BuildingInput input) throws Exception {
        return new BdAuthorized(
            this.permissions.forOperation("Buildings.create")
                .right(this.actor, this.school)
                .permit(this.origin).create(input),
            this.school, this.actor, this.permissions
        );
    }

    @Override
    public Building building(final long id) throws Exception {
        return new BdAuthorized(
            this.permissions.forOperation("Buildings.building")
                .right(this.actor, this.school)
                .permit(this.origin).building(id),
            this.school, this.actor, this.permissions
        );
    }

    @Override
    public Buildings selection(
        final Filters filters, final Page page, final Sorts sorts
    ) throws Exception {
        return new BdsAuthorized(
            this.permissions.forOperation("Buildings.selection")
                .right(this.actor, this.school)
                .permit(this.origin).selection(filters, page, sorts),
            this.school, this.actor, this.permissions
        );
    }

    @Override
    public Iterable<Building> iterate() {
        return StreamSupport.stream(
            this.permissions.forOperation("Buildings.selection")
                .right(this.actor, this.school).permit(this.origin)
                .iterate().spliterator(), false
        ).<Building>map(
            building -> new BdAuthorized(building, this.school, this.actor, this.permissions)
        ).toList();
    }

    @Override
    public void remove(final long id) throws Exception {
        this.permissions.forOperation("Buildings.remove")
            .right(this.actor, this.school)
            .permit(this.origin).remove(id);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.permissions.forOperation("Buildings.selection")
            .right(this.actor, this.school)
            .permit(this.origin).print(media);
    }
}
