/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.security.right.RtAdmin;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Permission matrix visible only to the current administrator.
 * For example, {@code new PsAuthorized(permissions, actor).print(media)}
 * checks the actor's current global role before printing the matrix.
 *
 * @since 0.0.1
 */
public final class PsAuthorized implements Permissions {

    /** Underlying permissions. */
    private final Permissions origin;

    /** Acting user. */
    private final User actor;

    /**
     * New protected permission matrix.
     *
     * @param origin Underlying permission matrix
     * @param actor Acting user
     */
    public PsAuthorized(final Permissions origin, final User actor) {
        this.origin = origin;
        this.actor = actor;
    }

    @Override
    public boolean allows(final String path, final RequestMethod method, final User caller) {
        return new RtAdmin(this.actor).permit(this.origin).allows(path, method, caller);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return new RtAdmin(this.actor).permit(this.origin).print(media);
    }
}
