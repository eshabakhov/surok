/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Schools;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.school.SlsAuthorized;
import org.springframework.security.access.AccessDeniedException;

/**
 * Global user whose account data is available to the current administrator.
 * For example, {@code new UrGlobalAuthorized(user, actor).roles()}
 * returns roles that recheck the actor before every operation.
 *
 * @since 0.0.1
 */
public final class UrGlobalAuthorized implements User {

    /** Underlying user. */
    private final User origin;

    /** Acting user. */
    private final User actor;

    /**
     * New protected user.
     *
     * @param origin Underlying user
     * @param actor Acting user
     */
    public UrGlobalAuthorized(final User origin, final User actor) {
        this.origin = origin;
        this.actor = actor;
    }

    @Override
    public long uid() {
        return this.origin.uid();
    }

    @Override
    public Credentials credentials() {
        if (!this.actor.isAdmin()) {
            throw new AccessDeniedException("Only an administrator can view user credentials");
        }
        return this.origin.credentials();
    }

    @Override
    public Roles roles() {
        if (!this.actor.isAdmin()) {
            throw new AccessDeniedException("Only an administrator can view global roles");
        }
        return new RlsGlobalAuthorized(this.origin.roles(), this.actor);
    }

    @Override
    public Schools schools() {
        if (!this.actor.isAdmin()) {
            throw new AccessDeniedException("Only an administrator can view user schools");
        }
        return new SlsAuthorized(this.origin.schools(), this.actor);
    }

    @Override
    public ReferenceInformation info() {
        if (!this.actor.isAdmin()) {
            throw new AccessDeniedException("Only an administrator can view user information");
        }
        return this.origin.info();
    }

    @Override
    public boolean isAdmin() {
        if (!this.actor.isAdmin()) {
            throw new AccessDeniedException("Only an administrator can view user roles");
        }
        return this.origin.isAdmin();
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        if (!this.actor.isAdmin()) {
            throw new AccessDeniedException("Only an administrator can view a global user");
        }
        return this.origin.print(media);
    }
}
