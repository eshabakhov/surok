/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Schools;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.school.SlMembers;
import com.eshabakhov.surok.school.SlsFixed;
import java.util.List;

/**
 * Representative account with one role and membership in a hypothetical school.
 * For example, {@code new UrRole(Roles.RoleEnum.DIRECTOR)} is inspected by normal domain rights.
 * Its identifiers describe only the in-memory model and have no database assignments.
 *
 * @since 0.0.1
 */
public final class UrRole implements User {

    /** Representative account identifier. */
    private final long id;

    /** Inspected role. */
    private final Roles.RoleEnum role;

    /**
     * New representative account.
     *
     * @param role Inspected role
     */
    public UrRole(final Roles.RoleEnum role) {
        this(1L, role);
    }

    /**
     * New representative account with an explicit identity.
     *
     * @param id Account identifier within the model
     * @param role Inspected role
     */
    public UrRole(final long id, final Roles.RoleEnum role) {
        this.id = id;
        this.role = role;
    }

    @Override
    public long uid() {
        return this.id;
    }

    @Override
    public Account account() {
        return new AcRole(this.role);
    }

    @Override
    public Roles roles() {
        final List<Role> assigned;
        if (List.of(
            Roles.RoleEnum.ADMIN, Roles.RoleEnum.BASIC_MAKER,
            Roles.RoleEnum.ADVANCED_MAKER, Roles.RoleEnum.PRO_MAKER
        ).contains(this.role)) {
            assigned = List.of(new RlNamed(this.role));
        } else {
            assigned = List.of();
        }
        return new RlsFixed(assigned);
    }

    @Override
    public Schools schools() {
        final List<Role> assigned;
        if (this.roles().contains(this.role)) {
            assigned = List.of();
        } else {
            assigned = List.of(new RlNamed(this.role));
        }
        return new SlsFixed(
            List.of(new SlMembers(1L, new UrsSingle(this, new RlsFixed(assigned))))
        );
    }

    @Override
    public <T> Media<T> print(final Media<T> media) {
        return media.with("id", this.id).with("role", this.role.name());
    }
}
