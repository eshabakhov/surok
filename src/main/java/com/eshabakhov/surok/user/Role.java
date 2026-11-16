/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Printable;
import com.eshabakhov.surok.sql.ClasspathSql;
import org.jooq.DSLContext;

/**
 * User role assignment abstraction.
 *
 * @since 0.0.1
 */
public interface Role extends Printable {

    @Override
    default <T> Media<T> print(final Media<T> media) {
        return media.with("uid", this.uid()).with("name", this.name());
    }

    /**
     * User role assignment unique identifier.
     *
     * @return UserRole ID
     */
    long uid();

    /**
     * Role name.
     *
     * @return Role name
     */
    String name();

    /**
     * Simple user role implementation.
     *
     * <p>Immutable object representing user role assignment.</p>
     *
     * @since 0.0.1
     */
    final class RlPostgres implements Role {

        /** Database context. */
        private final DSLContext ctx;

        /** User role ID. */
        private final long id;

        /**
         * New role.
         *
         * @param ctx Database context
         * @param id User role identifier
         */
        public RlPostgres(final DSLContext ctx, final long id) {
            this.ctx = ctx;
            this.id = id;
        }

        @Override
        public long uid() {
            return this.id;
        }

        @Override
        public String name() {
            return this.ctx.resultQuery(
                new ClasspathSql("role/find-name.sql").text(),
                this.id
            ).fetchOneInto(String.class);
        }
    }
}
