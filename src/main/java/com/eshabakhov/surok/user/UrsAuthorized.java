/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.User;
import com.eshabakhov.surok.Users;
import org.springframework.security.access.AccessDeniedException;

/**
 * School users whose operations check the acting user's current membership.
 * For example, a director may add a member, while a viewer may only read.
 *
 * @since 0.0.1
 */
public final class UrsAuthorized implements Users {

    /** Underlying school users. */
    private final Users origin;

    /** Acting user. */
    private final User actor;

    /**
     * New protected school users.
     *
     * @param origin Underlying school users
     * @param actor Acting user
     */
    public UrsAuthorized(final Users origin, final User actor) {
        this.origin = origin;
        this.actor = actor;
    }

    @Override
    public boolean contains(final User user) {
        if (!this.actor.isAdmin() && !this.origin.contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view school users");
        }
        return this.origin.contains(user);
    }

    @Override
    public User add(final User user) throws Exception {
        if (!this.actor.isAdmin() && (!this.origin.contains(this.actor)
            || !this.origin.roles(this.actor).contains(Roles.RoleEnum.DIRECTOR))) {
            throw new AccessDeniedException("Only a school director can manage members");
        }
        return this.origin.add(user);
    }

    @Override
    public Roles roles(final User user) {
        if (!this.actor.isAdmin() && !this.origin.contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view school roles");
        }
        return new RlsAuthorized(this.origin.roles(user), this.origin, this.actor);
    }

    @Override
    public Iterable<User> iterate() {
        if (!this.actor.isAdmin() && !this.origin.contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view school users");
        }
        return this.origin.iterate();
    }

    @Override
    public User register(
        final String username, final String password, final String email,
        final Boolean corporate
    ) throws Exception {
        if (!this.actor.isAdmin() && (!this.origin.contains(this.actor)
            || !this.origin.roles(this.actor).contains(Roles.RoleEnum.DIRECTOR))) {
            throw new AccessDeniedException("Only a school director can manage members");
        }
        return this.origin.register(username, password, email, corporate);
    }

    @Override
    public User identification(final String login) throws Exception {
        if (!this.actor.isAdmin() && !this.origin.contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view school users");
        }
        return this.origin.identification(login);
    }

    @Override
    public User identification(final Long id) throws Exception {
        if (!this.actor.isAdmin() && !this.origin.contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view school users");
        }
        return this.origin.identification(id);
    }

    @Override
    public User authentication(final String login) throws Exception {
        if (!this.actor.isAdmin() && !this.origin.contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view school users");
        }
        return this.origin.authentication(login);
    }

    @Override
    public void remove(final Long id) throws Exception {
        if (!this.actor.isAdmin() && (!this.origin.contains(this.actor)
            || !this.origin.roles(this.actor).contains(Roles.RoleEnum.DIRECTOR))) {
            throw new AccessDeniedException("Only a school director can manage members");
        }
        this.origin.remove(id);
    }
}
