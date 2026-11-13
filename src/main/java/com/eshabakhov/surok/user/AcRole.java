/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

/**
 * Representative account for the permission matrix without stored credentials.
 * For example, a representative director can be inspected without inventing a password.
 *
 * @since 0.0.1
 */
public final class AcRole implements Account {

    /** Role represented by the account. */
    private final Roles.RoleEnum role;

    /**
     * New representative account.
     *
     * @param role Inspected role
     */
    public AcRole(final Roles.RoleEnum role) {
        this.role = role;
    }

    @Override
    public boolean authenticated() {
        return this.role != Roles.RoleEnum.ANONYMOUS;
    }

    @Override
    public String username() {
        throw new IllegalStateException(
            String.format("The representative %s account has no username", this.role)
        );
    }

    @Override
    public String password() {
        throw new IllegalStateException(
            String.format("The representative %s account has no password", this.role)
        );
    }

    @Override
    public boolean corporate() {
        return false;
    }

    @Override
    public boolean alive() {
        return this.authenticated();
    }
}
