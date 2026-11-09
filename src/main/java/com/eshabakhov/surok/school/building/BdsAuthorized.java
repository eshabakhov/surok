/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.building;

import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.Sorts;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.security.right.RtAdmin;
import com.eshabakhov.surok.security.right.RtDirectorOrDeputy;
import com.eshabakhov.surok.security.right.RtEither;
import com.eshabakhov.surok.security.right.RtSchoolMember;
import java.util.stream.StreamSupport;

/**
 * School buildings protected by the acting user's current school role.
 * For example, {@code new BdsAuthorized(school.buildings(), school, actor).create(input)}
 * allows a director or deputy director to add a building.
 *
 * @since 0.0.1
 */
public final class BdsAuthorized implements Buildings {

    /** Underlying buildings. */
    private final Buildings origin;

    /** School that owns the buildings. */
    private final School school;

    /** Acting user. */
    private final User actor;

    /**
     * New protected building collection.
     *
     * @param origin Underlying buildings
     * @param school School that owns them
     * @param actor Acting user
     */
    public BdsAuthorized(final Buildings origin, final School school, final User actor) {
        this.origin = origin;
        this.school = school;
        this.actor = actor;
    }

    @Override
    public Building create(final BuildingInput input) throws Exception {
        return new BdAuthorized(
            new RtEither(
                new RtAdmin(this.actor), new RtDirectorOrDeputy(this.actor, this.school)
            ).permit(this.origin).create(input),
            this.school, this.actor
        );
    }

    @Override
    public Building building(final long id) throws Exception {
        return new BdAuthorized(
            new RtEither(
                new RtAdmin(this.actor), new RtSchoolMember(this.actor, this.school)
            ).permit(this.origin).building(id),
            this.school, this.actor
        );
    }

    @Override
    public Buildings selection(
        final Filters filters, final Page page, final Sorts sorts
    ) throws Exception {
        return new BdsAuthorized(
            new RtEither(
                new RtAdmin(this.actor), new RtSchoolMember(this.actor, this.school)
            ).permit(this.origin).selection(
                filters, page, sorts
            ), this.school, this.actor
        );
    }

    @Override
    public Iterable<Building> iterate() {
        return StreamSupport.stream(
            new RtEither(
                new RtAdmin(this.actor), new RtSchoolMember(this.actor, this.school)
            ).permit(this.origin).iterate().spliterator(), false
        ).<Building>map(
            building -> new BdAuthorized(building, this.school, this.actor)
        ).toList();
    }

    @Override
    public void remove(final long id) throws Exception {
        new RtEither(
            new RtAdmin(this.actor), new RtDirectorOrDeputy(this.actor, this.school)
        ).permit(this.origin).remove(id);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return new RtEither(
            new RtAdmin(this.actor), new RtSchoolMember(this.actor, this.school)
        ).permit(this.origin).print(media);
    }
}
