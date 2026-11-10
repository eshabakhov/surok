/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.building.cabinet;

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
 * Building cabinets protected by the acting user's school role.
 * For example, {@code new CbsAuthorized(building.cabinets(), school, actor).create(input)}
 * permits creation by a director or deputy director.
 *
 * @since 0.0.1
 */
public final class CbsAuthorized implements Cabinets {

    /** Underlying cabinets. */
    private final Cabinets origin;

    /** School that owns the cabinets. */
    private final School school;

    /** Acting user. */
    private final User actor;

    /**
     * New protected cabinet collection.
     *
     * @param origin Underlying cabinets
     * @param school School that owns them
     * @param actor Acting user
     */
    public CbsAuthorized(final Cabinets origin, final School school, final User actor) {
        this.origin = origin;
        this.school = school;
        this.actor = actor;
    }

    @Override
    public Cabinet create(final CabinetInput input) throws Exception {
        return new CbAuthorized(
            new RtEither(
                new RtAdmin(this.actor), new RtDirectorOrDeputy(this.actor, this.school)
            ).permit(this.origin).create(input),
            this.school, this.actor
        );
    }

    @Override
    public Cabinet cabinet(final long id) throws Exception {
        return new CbAuthorized(
            new RtEither(
                new RtAdmin(this.actor), new RtSchoolMember(this.actor, this.school)
            ).permit(this.origin).cabinet(id),
            this.school, this.actor
        );
    }

    @Override
    public Cabinets selection(
        final Filters filters, final Page page, final Sorts sorts
    ) throws Exception {
        return new CbsAuthorized(
            new RtEither(
                new RtAdmin(this.actor), new RtSchoolMember(this.actor, this.school)
            ).permit(this.origin).selection(
                filters, page, sorts
            ), this.school, this.actor
        );
    }

    @Override
    public Iterable<Cabinet> iterate() {
        return StreamSupport.stream(
            new RtEither(
                new RtAdmin(this.actor), new RtSchoolMember(this.actor, this.school)
            ).permit(this.origin).iterate().spliterator(), false
        ).<Cabinet>map(
            cabinet -> new CbAuthorized(cabinet, this.school, this.actor)
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
