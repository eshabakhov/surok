/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school;

import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.Schools;
import com.eshabakhov.surok.Sorts;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.security.right.RtAdmin;
import com.eshabakhov.surok.security.right.RtEither;
import com.eshabakhov.surok.security.right.RtSchoolMembership;
import java.util.stream.StreamSupport;

/**
 * Schools selected for the current user with school operations guarded.
 * For example, {@code new SlsAuthorized(actor.schools(), actor).school(id)}
 * returns a school whose revision checks the actor's current school role.
 *
 * @since 0.0.1
 */
public final class SlsAuthorized implements Schools {

    /** Collection scoped to the actor at creation time. */
    private final Schools origin;

    /** Acting user. */
    private final User actor;

    /**
     * New authorized school collection.
     *
     * @param origin Schools of the actor, or all schools for an administrator
     * @param actor Acting user
     */
    public SlsAuthorized(final Schools origin, final User actor) {
        this.origin = origin;
        this.actor = actor;
    }

    @Override
    public boolean contains(final long id) {
        return this.origin.contains(id)
            && new RtEither(
                new RtAdmin(this.actor), new RtSchoolMembership(this.actor, id)
            ).allows();
    }

    @Override
    public School create(final SchoolInput input) throws Exception {
        return new SlAuthorized(this.actor.schools().create(input), this.actor);
    }

    @Override
    public School school(final long id) throws Exception {
        return new SlAuthorized(
            new RtEither(
                new RtAdmin(this.actor), new RtSchoolMembership(this.actor, id)
            ).permit(this.origin).school(id), this.actor
        );
    }

    @Override
    public Schools selection(
        final Filters filters, final Page page, final Sorts sorts
    ) throws Exception {
        final Schools selected;
        if (new RtAdmin(this.actor).allows()) {
            selected = this.origin.selection(filters, page, sorts);
        } else {
            selected = this.actor.schools().selection(filters, page, sorts);
        }
        return new SlsAuthorized(selected, this.actor);
    }

    @Override
    public Iterable<School> iterate() {
        return StreamSupport.stream(
            this.origin.iterate().spliterator(), false
        ).filter(
            school -> new RtEither(
                new RtAdmin(this.actor), new RtSchoolMembership(this.actor, school.uid())
            ).allows()
        ).<School>map(school -> new SlAuthorized(school, this.actor)).toList();
    }

    @Override
    public void remove(final long id) throws Exception {
        new RtAdmin(this.actor).permit(this.origin).remove(id);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        if (!new RtAdmin(this.actor).allows()) {
            for (final School school : this.origin.iterate()) {
                new RtSchoolMembership(this.actor, school.uid()).permit(school);
            }
        }
        return this.origin.print(media);
    }
}
