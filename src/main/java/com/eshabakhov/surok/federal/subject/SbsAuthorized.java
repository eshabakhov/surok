/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject;

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
 * School subject catalogue protected by the actor's current school role.
 * For example, {@code new SbsAuthorized(subjects, school, actor).create(input)}
 * permits the school's director or deputy to add a local subject.
 *
 * @since 0.0.1
 */
public final class SbsAuthorized implements Subjects {

    /** Underlying school catalogue. */
    private final Subjects origin;

    /** Owning school. */
    private final School school;

    /** Acting user. */
    private final User actor;

    /**
     * New protected school catalogue.
     *
     * @param origin Underlying school catalogue
     * @param school Owning school
     * @param actor Acting user
     */
    public SbsAuthorized(final Subjects origin, final School school, final User actor) {
        this.origin = origin;
        this.school = school;
        this.actor = actor;
    }

    @Override
    public Subject create(final SubjectInput input) throws Exception {
        return new SbAuthorized(
            new RtEither(
                new RtAdmin(this.actor), new RtDirectorOrDeputy(this.actor, this.school)
            ).permit(this.origin).create(input),
            this.school, this.actor
        );
    }

    @Override
    public Subject subject(final long id) throws Exception {
        return new SbAuthorized(
            new RtEither(
                new RtAdmin(this.actor), new RtSchoolMember(this.actor, this.school)
            ).permit(this.origin).subject(id),
            this.school, this.actor
        );
    }

    @Override
    public Subject revise(final long id, final SubjectInput input) throws Exception {
        return new SbAuthorized(
            new RtEither(
                new RtAdmin(this.actor), new RtDirectorOrDeputy(this.actor, this.school)
            ).permit(this.origin).revise(id, input),
            this.school, this.actor
        );
    }

    @Override
    public Subjects selection(
        final Filters filters, final Page page, final Sorts sorts
    ) throws Exception {
        return new SbsAuthorized(
            new RtEither(
                new RtAdmin(this.actor), new RtSchoolMember(this.actor, this.school)
            ).permit(this.origin).selection(filters, page, sorts),
            this.school, this.actor
        );
    }

    @Override
    public Iterable<Subject> iterate() {
        return StreamSupport.stream(
            new RtEither(
                new RtAdmin(this.actor), new RtSchoolMember(this.actor, this.school)
            ).permit(this.origin).iterate().spliterator(), false
        ).<Subject>map(
            subject -> new SbAuthorized(subject, this.school, this.actor)
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
