/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user.subscription;

import com.eshabakhov.surok.security.right.RtAdmin;
import com.eshabakhov.surok.sql.ClasspathSql;
import com.eshabakhov.surok.user.AuthUser;
import com.eshabakhov.surok.user.Subscription;
import com.eshabakhov.surok.user.Subscriptions;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.Record;

/**
 * PostgreSQL-backed implementation of {@link Subscriptions}.
 *
 * <p>Each mutating operation (activate, downgrade) runs inside a single
 * jOOQ transaction that atomically swaps roles in {@code user_role} and
 * updates the {@code subscription} row. Corporate users are rejected
 * before any database work is done.</p>
 * Usage example:
 * <pre>
 * final Subscriptions subs = new PgSubscriptions(dsl);
 * subs.activate(userId, SubscriptionPlan.PRO);
 * </pre>
 *
 * @since 0.0.1
 */
public final class PgSubscriptions implements Subscriptions {

    /** JOOQ DSL context for all database operations. */
    private final DSLContext ctx;

    /** Authenticated user. */
    private final AuthUser user;

    /**
     * New subscriptions collection.
     *
     * @param ctx Database context
     * @param user Authenticated user
     */
    public PgSubscriptions(final DSLContext ctx, final AuthUser user) {
        this.ctx = ctx;
        this.user = user;
    }

    @Override
    public Subscription subscription() throws Exception {
        if (this.user.account().corporate() || new RtAdmin(this.user).allows()) {
            throw new PersonalOnlyException(this.user.uid());
        }
        final Record selected = this.ctx.resultQuery(
            new ClasspathSql("subscription/find-by-user-id.sql").text(),
            this.user.uid()
        ).fetchOne();
        if (selected == null) {
            this.ctx.resultQuery(
                new ClasspathSql("subscription/create-if-absent.sql").text(),
                this.user.uid(),
                "BASIC"
            ).execute();
        }
        return new PgSubscription(this.ctx, this.user.uid());
    }

    @Override
    public void subscription(final Subscription.Plan plan) throws Exception {
        if (this.user.account().corporate() || new RtAdmin(this.user).allows()) {
            throw new PersonalOnlyException(this.user.uid());
        }
        final OffsetDateTime expires;
        if (plan.free()) {
            expires = null;
        } else {
            expires = OffsetDateTime.now(ZoneOffset.UTC).plusDays(30);
        }
        this.ctx.transaction(
            conf -> {
                final DSLContext ttx = conf.dsl();
                this.swap(ttx, PgSubscriptions.roles(ttx, plan));
                ttx.resultQuery(
                    new ClasspathSql("subscription/upsert.sql").text(),
                    this.user.uid(),
                    plan.name(),
                    expires,
                    Instant.now().atOffset(ZoneOffset.UTC),
                    plan.name(),
                    expires,
                    Instant.now().atOffset(ZoneOffset.UTC)
                ).execute();
            }
        );
    }

    private static List<String> roles(final DSLContext dsl, final Subscription.Plan plan) {
        return dsl.resultQuery(
            new ClasspathSql("subscription/role/find-by-plan.sql").text(),
            plan.name()
        ).fetch()
            .stream()
            .map(r -> r.get("role_name", String.class))
            .toList();
    }

    private void swap(final DSLContext dsl, final List<String> grants) {
        dsl.execute(
            new ClasspathSql("user/role/delete-subscription-roles.sql").text(),
            this.user.uid()
        );
        for (final String role : grants) {
            final Record rid = dsl.resultQuery(
                new ClasspathSql("role/find-by-name.sql").text(),
                role
            ).fetchOne();
            if (rid == null) {
                throw new IllegalStateException(
                    String.format("Role not found in database (name=%s)", role)
                );
            }
            dsl.resultQuery(
                new ClasspathSql("user/role/create-if-absent.sql").text(),
                this.user.uid(),
                rid.get("id", Long.class)
            ).execute();
        }
    }
}
