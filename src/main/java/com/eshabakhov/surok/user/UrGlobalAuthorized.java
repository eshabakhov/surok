/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Schools;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.school.SlsAuthorized;
import com.eshabakhov.surok.security.right.RtAdmin;

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
    public Account account() {
        return new RtAdmin(this.actor).permit(this.origin).account();
    }

    @Override
    public Roles roles() {
        return new RlsGlobalAuthorized(
            new RtAdmin(this.actor).permit(this.origin).roles(), this.actor
        );
    }

    @Override
    public Schools schools() {
        return new SlsAuthorized(
            new RtAdmin(this.actor).permit(this.origin).schools(), this.actor
        );
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return new RtAdmin(this.actor).permit(this.origin).print(media);
    }
}
