/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.security.right.RtAdmin;
import com.eshabakhov.surok.security.right.RtEither;
import com.eshabakhov.surok.security.right.RtSchoolMember;
import java.util.stream.StreamSupport;

/**
 * Applied teaching loads visible to members of their school.
 * For example, a viewer can list the loads of a selected schedule.
 *
 * @since 0.0.1
 */
public final class ClsAuthorized implements ClassLoads {

    /** Underlying teaching loads. */
    private final ClassLoads origin;

    /** Owning school. */
    private final School school;

    /** Acting user. */
    private final User actor;

    /**
     * New protected teaching loads.
     *
     * @param origin Underlying loads
     * @param school Owning school
     * @param actor Acting user
     */
    public ClsAuthorized(final ClassLoads origin, final School school, final User actor) {
        this.origin = origin;
        this.school = school;
        this.actor = actor;
    }

    @Override
    public ClassLoad load(final long id) throws Exception {
        return new ClAuthorized(
            new RtEither(
                new RtAdmin(this.actor), new RtSchoolMember(this.actor, this.school)
            ).permit(this.origin).load(id),
            this.school, this.actor
        );
    }

    @Override
    public ClassLoads selection(final Page page) throws Exception {
        return new ClsAuthorized(
            new RtEither(
                new RtAdmin(this.actor), new RtSchoolMember(this.actor, this.school)
            ).permit(this.origin).selection(page),
            this.school, this.actor
        );
    }

    @Override
    public Iterable<ClassLoad> iterate() {
        return StreamSupport.stream(
            new RtEither(
                new RtAdmin(this.actor), new RtSchoolMember(this.actor, this.school)
            ).permit(this.origin).iterate().spliterator(), false
        ).<ClassLoad>map(
            load -> new ClAuthorized(load, this.school, this.actor)
        ).toList();
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return new RtEither(
            new RtAdmin(this.actor), new RtSchoolMember(this.actor, this.school)
        ).permit(this.origin).print(media);
    }
}
