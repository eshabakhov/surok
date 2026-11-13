/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.sql.ClasspathSql;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.stream.Collectors;
import org.jooq.DSLContext;
import org.jooq.Record;

/**
 * User roles collection abstraction.
 *
 * @since 0.0.1
 */
public interface Roles {

    /**
     * Whether this collection contains the named role.
     *
     * @param role Role to check
     * @return True when assigned
     */
    boolean contains(RoleEnum role);

    /**
     * Assign role to user.
     *
     * @param role User role
     * @return Created user role
     * @throws Exception if assignment fails
     */
    Role grant(RoleEnum role) throws Exception;

    /**
     * Get all roles for user.
     *
     * @return List of user roles
     */
    @JsonProperty
    List<Role> list();

    /**
     * Remove role from user.
     *
     * @param identity User role ID
     * @throws Exception if removal fails
     */
    void revoke(long identity) throws Exception;

    /**
     * User role enumeration.
     *
     * @since 0.0.1
     */
    enum RoleEnum {

        /** System administrator. */
        ADMIN,

        /** School director. */
        DIRECTOR,

        /** Deputy director. */
        DEPUTY_DIRECTOR,

        /** Teacher. */
        TEACHER,

        /** Student. */
        STUDENT,

        /** Basic maker. */
        BASIC_MAKER,

        /** Advanced maker. */
        ADVANCED_MAKER,

        /** Pro maker. */
        PRO_MAKER,

        /** Viewer. */
        VIEWER,

        /** Visitor without an authenticated account. */
        ANONYMOUS
    }

    /**
     * PostgreSQL user roles implementation.
     *
     * <p>Manages user role assignments in PostgreSQL database.</p>
     *
     * @since 0.0.1
     */
    final class RlsPostgres implements Roles {

        /** Database context. */
        private final DSLContext ctx;

        /** User. */
        private final Long user;

        /**
         * New roles collection.
         *
         * @param ctx Database context
         * @param user User identifier
         */
        public RlsPostgres(final DSLContext ctx, final Long user) {
            this.ctx = ctx;
            this.user = user;
        }

        @Override
        public boolean contains(final RoleEnum role) {
            return this.list().stream().anyMatch(assigned -> role.name().equals(assigned.name()));
        }

        @Override
        public Role grant(final RoleEnum role) throws Exception {
            if (!List.of(
                RoleEnum.ADMIN, RoleEnum.BASIC_MAKER,
                RoleEnum.ADVANCED_MAKER, RoleEnum.PRO_MAKER
            ).contains(role)) {
                throw new RoleGrantException("Only account roles can be assigned globally");
            }
            final Record selected = this.ctx.resultQuery(
                new ClasspathSql("role/find-by-name-grant.sql").text(),
                role.name()
            ).fetchOne();
            if (selected == null) {
                throw new RoleNotFoundException(
                    String.format("Role %s not found", role.name())
                );
            }
            final Record created = this.ctx.resultQuery(
                new ClasspathSql("user/role/create.sql").text(),
                this.user,
                selected.get("id", Long.class)
            ).fetchOne();
            if (created == null) {
                throw new RoleGrantException("Failed to grant role");
            }
            return new Role.RlPostgres(this.ctx, created.get("id", Long.class));
        }

        @Override
        public List<Role> list() {
            return this.ctx.resultQuery(
                new ClasspathSql("user/role/find-by-user-id.sql").text(),
                this.user
            ).fetch()
                .stream()
                .map(role -> new Role.RlPostgres(this.ctx, role.get("id", Long.class)))
                .collect(Collectors.toList());
        }

        @Override
        public void revoke(final long identity) {
            this.ctx.resultQuery(
                new ClasspathSql("user/role/delete.sql").text(),
                identity
            ).execute();
        }
    }
}
