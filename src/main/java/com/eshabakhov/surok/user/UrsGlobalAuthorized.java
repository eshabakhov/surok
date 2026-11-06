/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.User;
import com.eshabakhov.surok.Users;
import java.util.stream.StreamSupport;
import org.springframework.security.access.AccessDeniedException;

/**
 * Global users available only to the current administrator.
 * For example, {@code new UrsGlobalAuthorized(users, actor).remove(id)}
 * checks the actor's administrator role before removing an account.
 *
 * @since 0.0.1
 */
public final class UrsGlobalAuthorized implements Users {

    /** Underlying global users. */
    private final Users origin;

    /** Acting user. */
    private final User actor;

    /**
     * New protected global users.
     *
     * @param origin Underlying global users
     * @param actor Acting user
     */
    public UrsGlobalAuthorized(final Users origin, final User actor) {
        this.origin = origin;
        this.actor = actor;
    }

    @Override
    public boolean contains(final User user) {
        if (!this.actor.isAdmin()) {
            throw new AccessDeniedException("Only an administrator can inspect global users");
        }
        return this.origin.contains(user);
    }

    @Override
    public User add(final User user) throws Exception {
        if (!this.actor.isAdmin()) {
            throw new AccessDeniedException("Only an administrator can add a global user");
        }
        return new UrGlobalAuthorized(this.origin.add(user), this.actor);
    }

    @Override
    public Roles roles(final User user) {
        if (!this.actor.isAdmin()) {
            throw new AccessDeniedException("Only an administrator can inspect global roles");
        }
        return new RlsGlobalAuthorized(this.origin.roles(user), this.actor);
    }

    @Override
    public Iterable<User> iterate() {
        if (!this.actor.isAdmin()) {
            throw new AccessDeniedException("Only an administrator can list global users");
        }
        return StreamSupport.stream(this.origin.iterate().spliterator(), false)
            .<User>map(user -> new UrGlobalAuthorized(user, this.actor)).toList();
    }

    @Override
    public User register(
        final String username, final String password, final String email, final Boolean corporate
    ) throws Exception {
        if (!this.actor.isAdmin()) {
            throw new AccessDeniedException("Only an administrator can register a global user");
        }
        return new UrGlobalAuthorized(
            this.origin.register(username, password, email, corporate), this.actor
        );
    }

    @Override
    public User identification(final String login) throws Exception {
        if (!this.actor.isAdmin()) {
            throw new AccessDeniedException("Only an administrator can identify a global user");
        }
        return new UrGlobalAuthorized(this.origin.identification(login), this.actor);
    }

    @Override
    public User identification(final Long id) throws Exception {
        if (!this.actor.isAdmin()) {
            throw new AccessDeniedException("Only an administrator can identify a global user");
        }
        return new UrGlobalAuthorized(this.origin.identification(id), this.actor);
    }

    @Override
    public User authentication(final String login) throws Exception {
        if (!this.actor.isAdmin()) {
            throw new AccessDeniedException("Only an administrator can authenticate a global user");
        }
        return new UrGlobalAuthorized(this.origin.authentication(login), this.actor);
    }

    @Override
    public void remove(final Long id) throws Exception {
        if (!this.actor.isAdmin()) {
            throw new AccessDeniedException("Only an administrator can remove a global user");
        }
        this.origin.remove(id);
    }
}
