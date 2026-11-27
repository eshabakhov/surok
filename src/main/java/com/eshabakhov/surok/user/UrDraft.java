/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.User;

/**
 * User registration fields before an account exists, without stored credentials.
 * For example, {@code new UrDraft("", "")} supplies an empty registration form.
 *
 * @since 0.0.1
 */
public final class UrDraft implements User {

    /** Entered username. */
    private final String username;

    /** Entered email. */
    private final String email;

    /**
     * New unsaved user with public registration fields.
     *
     * @param username Entered username
     * @param email Entered email
     */
    public UrDraft(final String username, final String email) {
        this.username = username;
        this.email = email;
    }

    @Override
    public Roles roles() {
        return new UrAnonymous().roles();
    }

    @Override
    public <T> Media<T> print(final Media<T> media) {
        return media.with("username", this.username).with("email", this.email);
    }
}
