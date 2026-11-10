/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schoolclass;

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
 * School classes protected by the acting user's current school role.
 * For example, a director can create a class and a viewer can list classes.
 *
 * @since 0.0.1
 */
public final class ScsAuthorized implements SchoolClasses {

    /** Underlying classes. */
    private final SchoolClasses origin;

    /** School that owns the classes. */
    private final School school;

    /** Acting user. */
    private final User actor;

    /**
     * New protected school classes.
     *
     * @param origin Underlying classes
     * @param school School that owns them
     * @param actor Acting user
     */
    public ScsAuthorized(final SchoolClasses origin, final School school, final User actor) {
        this.origin = origin;
        this.school = school;
        this.actor = actor;
    }

    @Override
    public SchoolClass create(final SchoolClassInput input) throws Exception {
        return new ScAuthorized(
            new RtEither(
                new RtAdmin(this.actor), new RtDirectorOrDeputy(this.actor, this.school)
            ).permit(this.origin).create(input),
            this.school, this.actor
        );
    }

    @Override
    public SchoolClass clazz(final long id) throws Exception {
        return new ScAuthorized(
            new RtEither(
                new RtAdmin(this.actor), new RtSchoolMember(this.actor, this.school)
            ).permit(this.origin).clazz(id),
            this.school, this.actor
        );
    }

    @Override
    public SchoolClasses selection(
        final Filters filters, final Page page, final Sorts sorts
    ) throws Exception {
        return new ScsAuthorized(
            new RtEither(
                new RtAdmin(this.actor), new RtSchoolMember(this.actor, this.school)
            ).permit(this.origin).selection(
                filters, page, sorts
            ), this.school, this.actor
        );
    }

    @Override
    public Iterable<SchoolClass> iterate() {
        return StreamSupport.stream(
            new RtEither(
                new RtAdmin(this.actor), new RtSchoolMember(this.actor, this.school)
            ).permit(this.origin).iterate().spliterator(), false
        ).<SchoolClass>map(
            clazz -> new ScAuthorized(clazz, this.school, this.actor)
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
