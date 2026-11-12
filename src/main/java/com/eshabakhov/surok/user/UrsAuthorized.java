/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.School;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.Users;
import com.eshabakhov.surok.security.right.RtAdmin;
import com.eshabakhov.surok.security.right.RtEither;
import com.eshabakhov.surok.security.right.RtSchoolDirector;
import com.eshabakhov.surok.security.right.RtSchoolMember;

/**
 * School users whose operations check the acting user's current membership.
 * For example, a director may add a member, while a viewer may only read.
 *
 * @since 0.0.1
 */
public final class UrsAuthorized implements Users {

    /** Underlying school users. */
    private final Users origin;

    /** School whose users are protected. */
    private final School school;

    /** Acting user. */
    private final User actor;

    /**
     * New protected school users.
     *
     * @param origin Underlying school users
     * @param school School whose users are protected
     * @param actor Acting user
     */
    public UrsAuthorized(final Users origin, final School school, final User actor) {
        this.origin = origin;
        this.school = school;
        this.actor = actor;
    }

    @Override
    public boolean contains(final User user) {
        return new RtEither(
            new RtAdmin(this.actor), new RtSchoolMember(this.actor, this.school)
        ).permit(this.origin).contains(user);
    }

    @Override
    public User add(final User user) throws Exception {
        return new RtEither(
            new RtAdmin(this.actor), new RtSchoolDirector(this.actor, this.school)
        ).permit(this.origin).add(user);
    }

    @Override
    public Roles roles(final User user) {
        return new RlsAuthorized(
            new RtEither(
                new RtAdmin(this.actor), new RtSchoolMember(this.actor, this.school)
            ).permit(this.origin).roles(user), this.school, this.actor
        );
    }

    @Override
    public Iterable<User> iterate() {
        return new RtEither(
            new RtAdmin(this.actor), new RtSchoolMember(this.actor, this.school)
        ).permit(this.origin).iterate();
    }

    @Override
    public User register(
        final String username, final String password, final String email,
        final Boolean corporate
    ) throws Exception {
        return new RtEither(
            new RtAdmin(this.actor), new RtSchoolDirector(this.actor, this.school)
        ).permit(this.origin).register(username, password, email, corporate);
    }

    @Override
    public User identification(final String login) throws Exception {
        return new RtEither(
            new RtAdmin(this.actor), new RtSchoolMember(this.actor, this.school)
        ).permit(this.origin).identification(login);
    }

    @Override
    public User identification(final Long id) throws Exception {
        return new RtEither(
            new RtAdmin(this.actor), new RtSchoolMember(this.actor, this.school)
        ).permit(this.origin).identification(id);
    }

    @Override
    public User authentication(final String login) throws Exception {
        return new RtEither(
            new RtAdmin(this.actor), new RtSchoolMember(this.actor, this.school)
        ).permit(this.origin).authentication(login);
    }

    @Override
    public void remove(final Long id) throws Exception {
        new RtEither(
            new RtAdmin(this.actor), new RtSchoolDirector(this.actor, this.school)
        ).permit(this.origin).remove(id);
    }
}
