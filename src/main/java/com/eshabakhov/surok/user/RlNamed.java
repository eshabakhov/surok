/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

/**
 * A named role without a persisted assignment.
 * For example, {@code new RlNamed(Roles.RoleEnum.DIRECTOR)} describes a matrix role.
 *
 * @since 0.0.1
 */
public final class RlNamed implements Role {

    /** Role definition. */
    private final Roles.RoleEnum role;

    /**
     * New role definition.
     *
     * @param role Role name
     */
    public RlNamed(final Roles.RoleEnum role) {
        this.role = role;
    }

    @Override
    public long uid() {
        throw new IllegalStateException(
            String.format("Role %s has no persisted assignment", this.role)
        );
    }

    @Override
    public String name() {
        return this.role.name();
    }
}
