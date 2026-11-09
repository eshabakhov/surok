/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.right;

import com.eshabakhov.surok.User;

/**
 * Global administrator right for operations on any school.
 * For example, only an administrator may remove a school.
 *
 * @since 0.0.1
 */
public final class RtAdmin implements Right {

    /** Acting user. */
    private final User actor;

    /**
     * New administrator right.
     *
     * @param actor Acting user
     */
    public RtAdmin(final User actor) {
        this.actor = actor;
    }

    @Override
    public boolean allows() {
        return this.actor.isAdmin();
    }
}
