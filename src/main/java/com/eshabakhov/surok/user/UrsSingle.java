/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.Users;
import java.util.List;

/**
 * Read-only school membership of one account with specified local roles.
 * For example, a representative director belongs to a school without any database rows.
 *
 * @since 0.0.1
 */
public final class UrsSingle implements Users {

    /** Represented account. */
    private final UrAccount user;

    /** Roles in the represented school. */
    private final Roles assigned;

    /**
     * New single-account membership.
     *
     * @param user Represented account
     * @param assigned School roles
     */
    public UrsSingle(final UrAccount user, final Roles assigned) {
        this.user = user;
        this.assigned = assigned;
    }

    @Override
    public boolean contains(final UrAccount actor) {
        return this.user.uid() == actor.uid();
    }

    @Override
    public UrAccount add(final UrAccount actor) {
        throw new UnsupportedOperationException("Read-only membership cannot add an account");
    }

    @Override
    public Roles roles(final UrAccount actor) {
        final Roles roles;
        if (this.contains(actor)) {
            roles = this.assigned;
        } else {
            roles = new RlsFake(List.of());
        }
        return roles;
    }

    @Override
    public Iterable<UrAccount> iterate() {
        return List.of(this.user);
    }

    @Override
    public UrAccount register(
        final String username, final String password, final String email, final Boolean corporate
    ) {
        throw new UnsupportedOperationException("Read-only membership cannot register an account");
    }

    @Override
    public UrAccount identification(final String login) {
        throw new UnsupportedOperationException("Representative membership has no account logins");
    }

    @Override
    public UrAccount identification(final Long id) throws UserNotFoundException {
        if (this.user.uid() != id) {
            throw new UserNotFoundException(String.format("User with id=%d not found", id));
        }
        return this.user;
    }

    @Override
    public UrAccount authentication(final String login) {
        throw new UnsupportedOperationException("Representative membership cannot authenticate");
    }

    @Override
    public void remove(final Long id) {
        throw new UnsupportedOperationException(
            String.format("Read-only membership cannot remove account %d", id)
        );
    }
}
