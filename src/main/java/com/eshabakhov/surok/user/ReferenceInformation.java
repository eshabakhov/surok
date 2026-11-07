/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.User;
import com.eshabakhov.surok.sql.ClasspathSql;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jooq.DSLContext;

/**
 * Reference information for {@link User}.
 *
 * @since 0.0.1
 */
public interface ReferenceInformation {

    /**
     * Corporate account marker.
     *
     * @return Corporate account marker
     */
    @JsonProperty
    Boolean corporate();

    /**
     * Account availability marker.
     *
     * @return Account availability marker
     */
    @JsonProperty
    Boolean alive();

    /**
     * Postgres implementation of {@link ReferenceInformation}.
     *
     * @since 0.0.1
     */
    final class RefInfoPostgres implements ReferenceInformation {

        /** Database context. */
        private final DSLContext ctx;

        /** User id. */
        private final Long user;

        /**
         * New reference information.
         *
         * @param ctx Database context
         * @param user User identifier
         */
        public RefInfoPostgres(final DSLContext ctx, final Long user) {
            this.ctx = ctx;
            this.user = user;
        }

        @Override
        public Boolean corporate() {
            return this.ctx.resultQuery(
                new ClasspathSql("user/find-corporate.sql").text(),
                this.user
            ).fetchOneInto(Boolean.class);
        }

        @Override
        public Boolean alive() {
            return this.ctx.resultQuery(
                new ClasspathSql("user/find-alive.sql").text(),
                this.user
            ).fetchOne() != null;
        }
    }
}
