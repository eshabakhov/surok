/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schoolclass;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.school.schoolclass.input.MdScInput;
import com.eshabakhov.surok.sql.ClasspathSql;
import java.util.Map;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;

/**
 * Postgres school class.
 *
 * @since 0.0.1
 * @checkstyle LambdaBodyLengthCheck (100 lines)
 */
public final class ScPostgres implements SchoolClass {

    /** Database context. */
    private final DSLContext ctx;

    /** Class identifier. */
    private final Long id;

    /** Whether archived records are readable. */
    private final boolean archived;

    /**
     * New school class.
     *
     * @param ctx Database context
     * @param id Class identifier
     */
    public ScPostgres(final DSLContext ctx, final Long id) {
        this(ctx, id, false);
    }

    /**
     * New school class with optional access to historical records.
     *
     * @param ctx Database context
     * @param id Entity identifier
     * @param archived Whether archived records are readable
     */
    public ScPostgres(final DSLContext ctx, final Long id, final boolean archived) {
        this.ctx = ctx;
        this.id = id;
        this.archived = archived;
    }

    @Override
    public Long uid() {
        return this.id;
    }

    @Override
    public int grade() throws Exception {
        return this.details().get("grade", Integer.class);
    }

    @Override
    public SchoolClass revise(final SchoolClassInput input) throws Exception {
        final Map<String, String> fields = input.print(new MdScInput()).print();
        return this.ctx.transactionResult(
            config -> {
                final DSLContext transaction = DSL.using(config);
                final Record current = transaction.resultQuery(
                    new ClasspathSql("school-class/find-details.sql").text(), this.id, false
                ).fetchOne();
                if (current == null) {
                    throw new SchoolClassNotFoundException(
                        String.format("School class with id=%d not found", this.id)
                    );
                }
                final Record existing = transaction.resultQuery(
                    new ClasspathSql("school-class/find-existing.sql").text(),
                    current.get("school_id", Long.class),
                    Integer.valueOf(fields.get("grade")), fields.get("litera"), false
                ).fetchOne();
                if (existing != null && !this.id.equals(existing.get("id", Long.class))) {
                    throw new SchoolClassAlreadyExistsException(
                        String.format(
                            "School class `%s%s` already exists",
                            fields.get("grade"), fields.get("litera")
                        )
                    );
                }
                final Long revised = transaction.resultQuery(
                    new ClasspathSql("school-class/revise.sql").text(),
                    Integer.valueOf(fields.get("grade")), fields.get("litera"), this.id, false
                ).fetchOne("id", Long.class);
                if (revised == null) {
                    throw new SchoolClassNotFoundException(
                        String.format("School class with id=%d not found", this.id)
                    );
                }
                return new ScPostgres(this.ctx, revised);
            }
        );
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        final Record selected = this.details();
        return media.with("id", selected.get("id", Long.class)).with(
            "name", String.format(
                "%d%s", selected.get("grade", Integer.class),
                selected.get("litera", String.class)
            )
        ).with("grade", selected.get("grade", Integer.class))
            .with("litera", selected.get("litera", String.class));
    }

    private Record details() throws Exception {
        final Record selected = this.ctx.resultQuery(
            new ClasspathSql("school-class/find-details.sql").text(), this.id, this.archived
        ).fetchOne();
        if (selected == null) {
            throw new SchoolClassNotFoundException(
                String.format("School class with id=%d not found", this.id)
            );
        }
        return selected;
    }
}
