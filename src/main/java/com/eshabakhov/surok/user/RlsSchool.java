/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.sql.ClasspathSql;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.Record;

/**
 * Roles assigned to a user in one school.
 *
 * @since 0.0.1
 */
public final class RlsSchool implements Roles {

    /** Database context. */
    private final DSLContext ctx;

    /** School identifier. */
    private final long school;

    /** User identifier. */
    private final long user;

    /**
     * New school roles.
     *
     * @param ctx Database context
     * @param school School identifier
     * @param user User identifier
     */
    public RlsSchool(final DSLContext ctx, final long school, final long user) {
        this.ctx = ctx;
        this.school = school;
        this.user = user;
    }

    @Override
    public boolean contains(final RoleEnum role) {
        return this.ctx.resultQuery(
            new ClasspathSql("school-user/role/has.sql").text(),
            this.school, this.user, role.name()
        ).fetchOneInto(Boolean.class);
    }

    @Override
    public Role grant(final RoleEnum role) throws Exception {
        if (role == RoleEnum.ADMIN || role == RoleEnum.BASIC_MAKER
            || role == RoleEnum.ADVANCED_MAKER || role == RoleEnum.PRO_MAKER) {
            throw new RoleGrantException("Account role cannot be assigned to a school");
        }
        final Record created = this.ctx.resultQuery(
            new ClasspathSql("school-user/role/create.sql").text(),
            this.school, this.user, role.name()
        ).fetchOne();
        if (created == null) {
            throw new RoleGrantException("User is not a member of the school");
        }
        return new RlSchool(this.ctx, created.get("id", Long.class));
    }

    @Override
    public List<Role> list() {
        return this.ctx.resultQuery(
            new ClasspathSql("school-user/role/list.sql").text(), this.school, this.user
        ).fetch(
            row -> new RlSchool(this.ctx, row.get("id", Long.class))
        );
    }

    @Override
    public void revoke(final long identity) {
        this.ctx.resultQuery(
            new ClasspathSql("school-user/role/remove.sql").text(),
            identity, this.school, this.user
        ).execute();
    }
}
