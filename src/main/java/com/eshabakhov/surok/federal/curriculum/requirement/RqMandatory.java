/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum.requirement;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.federal.curriculum.CrPostgres;
import com.eshabakhov.surok.federal.curriculum.requirement.input.MdRqInput;
import com.eshabakhov.surok.federal.curriculum.requirement.input.RequirementInputException;
import com.eshabakhov.surok.federal.curriculum.requirement.input.RqInputHttp;
import com.eshabakhov.surok.federal.subject.SbPostgres;
import com.eshabakhov.surok.sql.ClasspathSql;
import java.util.Map;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.dao.DuplicateKeyException;

/**
 * Mandatory subject requirement stored in PostgreSQL.
 * For example, {@code new RqMandatory(ctx, id).revise(input)} changes its required hours.
 * Holds only {@code rid} and {@code ctx} - all data is read from DB on demand.
 *
 * @since 0.0.1
 */
public final class RqMandatory implements Requirement {

    /**
     * Federal curriculum requirement id.
     */
    private final Long rid;

    /**
     * Database connection.
     */
    private final DSLContext ctx;

    /**
     * Creates a Postgres-backed requirement.
     *
     * @param ctx JOOQ DSL context
     * @param rid Requirement ID
     */
    public RqMandatory(final DSLContext ctx, final Long rid) {
        this.ctx = ctx;
        this.rid = rid;
    }

    @Override
    public Long uid() {
        return this.rid;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        final Record record = this.details();
        return media.with("id", record.get("id", Long.class))
            .with("grade", record.get("grade", Integer.class)).with(
                "subject", new SbPostgres(this.ctx, record.get("subject_id", Long.class), true)
            ).with("hours", record.get("hours", Integer.class))
            .with("partType", record.get("part_type", String.class));
    }

    @Override
    public Requirement revise(final RequirementInput input) throws Exception {
        final Map<String, String> fields = new RqInputHttp(
            input.print(new MdRqInput()).print()
        ).print(new MdRqInput()).print();
        if (!"MANDATORY".equals(fields.get("partType"))) {
            throw new RequirementInputException("A mandatory requirement cannot change its kind");
        }
        final Integer grade = Integer.valueOf(fields.get("grade"));
        final Record record = this.ctx.resultQuery(
            new ClasspathSql("curriculum/requirement/find-details.sql").text(),
            this.rid, "MANDATORY"
        ).fetchOne();
        if (record == null || !new CrPostgres(
            this.ctx, record.get("federal_curriculum_id", Long.class)
        ).grades().contains(grade)) {
            throw new RequirementInputException(
                String.format("Cannot revise requirement %d with grade %d", this.rid, grade)
            );
        }
        final Record revised;
        try {
            revised = this.ctx.resultQuery(
                new ClasspathSql("curriculum/requirement/revise-mandatory.sql").text(),
                grade,
                fields.get("subjectId"),
                Integer.valueOf(fields.get("hours")),
                this.rid
            ).fetchOne();
        } catch (final DuplicateKeyException exception) {
            throw new RequirementAlreadyExistsException(exception);
        }
        if (revised == null) {
            throw new RequirementInputException(
                String.format(
                    "Cannot revise requirement %d with active subject `%s`",
                    this.rid, fields.get("subjectId")
                )
            );
        }
        return new RqMandatory(this.ctx, revised.get("id", Long.class));
    }

    private Record details() throws Exception {
        return this.ctx.resultQuery(
            new ClasspathSql("curriculum/requirement/find-details.sql").text(),
            this.rid, "MANDATORY"
        ).fetchSingle();
    }
}
