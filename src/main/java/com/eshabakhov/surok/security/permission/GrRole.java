/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.user.Roles;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * One role's access to an operation, including the school ownership condition.
 * For example, print DIRECTOR with scope school and VIEWER with scope denied.
 *
 * @since 0.0.1
 */
public final class GrRole implements Grant {

    /** Controller permission. */
    private final Permission permission;

    /** Account whose access is inspected. */
    private final User actor;

    /** Role being inspected, or ANONYMOUS. */
    private final Roles.RoleEnum role;

    /** Locale for role titles. */
    private final Locale locale;

    /**
     * New matrix cell.
     *
     * @param permission Controller permission
     * @param actor Account whose access is inspected
     * @param role Role name
     * @param locale Locale for role titles
     */
    public GrRole(
        final Permission permission,
        final User actor,
        final Roles.RoleEnum role,
        final Locale locale
    ) {
        this.permission = permission;
        this.actor = actor;
        this.role = role;
        this.locale = locale;
    }

    @Override
    public Scope scope() {
        final Scope scope;
        if (this.permission.allows(this.actor, true)) {
            if (this.permission.allows(this.actor, false)) {
                scope = Scope.ALL;
            } else {
                scope = Scope.SCHOOL;
            }
        } else {
            scope = Scope.DENIED;
        }
        return scope;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) {
        return media.with("role", this.role.name()).with(
            "title", ResourceBundle.getBundle("localization.roles", this.locale)
                .getString(this.role.name())
        ).with("scope", this.scope().name().toLowerCase(Locale.ROOT));
    }
}
