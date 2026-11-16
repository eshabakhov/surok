/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school;

import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.PrItems;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.Schools;
import com.eshabakhov.surok.Sorts;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.Users;
import com.eshabakhov.surok.page.ResponsePage;
import com.eshabakhov.surok.school.filter.FlsCdSchool;
import com.eshabakhov.surok.school.input.MdSlInput;
import com.eshabakhov.surok.school.sort.StsJqSchool;
import com.eshabakhov.surok.security.right.RtAdmin;
import com.eshabakhov.surok.sql.ClasspathSql;
import com.eshabakhov.surok.user.Roles;
import com.eshabakhov.surok.user.UrsPostgres;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.security.access.AccessDeniedException;

/**
 * Postgres implementation of {@link Schools}.
 *
 * @since 0.0.1
 */
public final class SlsPostgres implements Schools {

    /** JOOQ DSL context for executing database queries. */
    private final DSLContext ctx;

    /** Acting user, absent for the unrestricted repository. */
    private final User actor;

    /**
     * New schools collection.
     *
     * @param ctx Database context
     * @since 0.0.1
     */
    public SlsPostgres(final DSLContext ctx) {
        this(ctx, null);
    }

    /**
     * Schools available to one user.
     *
     * @param ctx Database context
     * @param actor Acting user
     */
    public SlsPostgres(final DSLContext ctx, final User actor) {
        this.ctx = ctx;
        this.actor = actor;
    }

    @Override
    public boolean contains(final long id) {
        final boolean found;
        if (this.actor == null) {
            found = this.ctx.resultQuery(
                new ClasspathSql("school/find-by-id.sql").text(), id, false
            ).fetchOne() != null;
        } else {
            found = this.ctx.resultQuery(
                new ClasspathSql("school-user/contains.sql").text(), id, this.actor.uid()
            ).fetchOneInto(Boolean.class);
        }
        return found;
    }

    @Override
    public School create(final SchoolInput input) throws Exception {
        final String name = input.print(new MdSlInput()).print().get("name");
        return this.ctx.transactionResult(
            config -> {
                final Record created = DSL.using(config).resultQuery(
                    new ClasspathSql("school/create.sql").text(),
                    name,
                    false
                ).fetchOne();
                if (created == null) {
                    throw new SchoolFailedCreateException();
                }
                final School school = new SlPostgres(this.ctx, created.get("id", Long.class));
                if (this.actor != null) {
                    final Users members = new UrsPostgres(DSL.using(config), school.uid());
                    members.add(this.actor);
                    members.roles(this.actor).grant(Roles.RoleEnum.DIRECTOR);
                }
                return school;
            }
        );
    }

    @Override
    public School school(final long sid) throws Exception {
        if (!this.contains(sid)) {
            throw new SchoolNotFoundException(String.format("School with id=%d not found", sid));
        }
        final Record selected = this.ctx.resultQuery(
            new ClasspathSql("school/find-by-id.sql").text(),
            sid,
            false
        ).fetchOne();
        if (selected == null) {
            throw new SchoolNotFoundException(String.format("School with id=%d not found", sid));
        }
        return new SlPostgres(this.ctx, selected.get("id", Long.class));
    }

    @Override
    public Schools selection(
        final Filters filters,
        final Page page,
        final Sorts sorts
    ) throws Exception {
        final Condition scoped = new FlsCdSchool(filters).condition().and(
            DSL.condition(
                new ClasspathSql("school/conditions/by-is-deleted.sql").text(),
                false
            )
        );
        final Condition visible;
        if (this.actor == null) {
            visible = scoped;
        } else {
            visible = scoped.and(
                DSL.condition(
                    new ClasspathSql("school/conditions/by-user-id.sql").text(),
                    this.actor.uid()
                )
            );
        }
        return new SlsSelected(
            this,
            this.ctx.resultQuery(
                new ClasspathSql("school/list.sql").text(),
                visible,
                DSL.list(new StsJqSchool(sorts).fields()),
                DSL.val(page.limit()),
                DSL.val((page.offset() - 1) * page.limit())
            ).fetch(
                selected -> new SlPostgres(
                    this.ctx,
                    selected.get("id", Long.class)
                )
            ),
            new ResponsePage(
                page,
                this.ctx.resultQuery(
                    new ClasspathSql("school/count.sql").text(),
                    visible
                ).fetchSingle(0, Integer.class)
            )
        );
    }

    @Override
    public Iterable<School> iterate() {
        final Iterable<School> schools;
        if (this.actor == null) {
            schools = this.ctx.resultQuery(
                new ClasspathSql("school/list-active.sql").text(), false
            ).fetch(selected -> new SlPostgres(this.ctx, selected.get("id", Long.class)));
        } else {
            schools = this.ctx.resultQuery(
                new ClasspathSql("school/list-by-user.sql").text(), this.actor.uid()
            ).fetch(selected -> new SlPostgres(this.ctx, selected.get("id", Long.class)));
        }
        return schools;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return media.with(new PrItems("school", this.iterate()));
    }

    @Override
    public void remove(final long sid) throws Exception {
        if (this.actor != null && !new RtAdmin(this.actor).allows()) {
            throw new AccessDeniedException("Only an administrator can delete a school");
        }
        final Record selected = this.ctx.resultQuery(
            new ClasspathSql("school/find-by-id.sql").text(),
            sid,
            false
        ).fetchOne();
        if (selected == null) {
            throw new SchoolNotFoundException(String.format("School with id=%d not found", sid));
        }
        this.ctx.transaction(
            config ->
                DSL.using(config).resultQuery(
                    new ClasspathSql("school/soft-delete.sql").text(),
                    true,
                    sid
                ).execute()
        );
    }
}
