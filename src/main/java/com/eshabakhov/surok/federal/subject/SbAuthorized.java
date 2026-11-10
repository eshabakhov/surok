/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.security.right.RtAdmin;
import com.eshabakhov.surok.security.right.RtDirectorOrDeputy;
import com.eshabakhov.surok.security.right.RtEither;
import com.eshabakhov.surok.security.right.RtSchoolMember;

/**
 * School subject whose revision follows the actor's current school role.
 * For example, {@code new SbAuthorized(subject, school, actor).revise(input)}
 * refuses a member who lost the school's director role.
 *
 * @since 0.0.1
 */
public final class SbAuthorized implements Subject {

    /** Underlying school subject. */
    private final Subject subject;

    /** Owning school. */
    private final School school;

    /** Acting user. */
    private final User actor;

    /**
     * New protected school subject.
     *
     * @param subject Underlying school subject
     * @param school Owning school
     * @param actor Acting user
     */
    public SbAuthorized(final Subject subject, final School school, final User actor) {
        this.subject = subject;
        this.school = school;
        this.actor = actor;
    }

    @Override
    public Long uid() {
        return this.subject.uid();
    }

    @Override
    public String name() throws Exception {
        return new RtEither(
            new RtAdmin(this.actor), new RtSchoolMember(this.actor, this.school)
        ).permit(this.subject).name();
    }

    @Override
    public Origin origin() throws Exception {
        return new RtEither(
            new RtAdmin(this.actor), new RtSchoolMember(this.actor, this.school)
        ).permit(this.subject).origin();
    }

    @Override
    public Subject revise(final SubjectInput input) throws Exception {
        return new SbAuthorized(
            new RtEither(
                new RtAdmin(this.actor), new RtDirectorOrDeputy(this.actor, this.school)
            ).permit(this.subject).revise(input),
            this.school, this.actor
        );
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return new RtEither(
            new RtAdmin(this.actor), new RtSchoolMember(this.actor, this.school)
        ).permit(this.subject).print(media);
    }
}
