/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum.requirement;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.federal.curriculum.CrPostgres;
import com.eshabakhov.surok.federal.curriculum.requirement.input.MdRqInput;
import com.eshabakhov.surok.federal.curriculum.requirement.input.RequirementInputException;
import com.eshabakhov.surok.federal.curriculum.requirement.input.RqInputHttp;
import com.eshabakhov.surok.sql.ClasspathSql;
import java.util.Map;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.dao.DuplicateKeyException;

/**
 * Maximum additional weekly hours for each class of a curriculum grade.
 * For example, {@code new RqOptional(ctx, id).revise(input)} changes the grade maximum.
 * A zero maximum prohibits additional hours; deleting the requirement leaves it unspecified.
 *
 * @since 0.0.1
 */
public final class RqOptional implements Requirement {

    /** Database context. */
    private final DSLContext ctx;

    /** Requirement identity. */
    private final Long identity;

    /**
     * New optional requirement.
     *
     * @param ctx Database context
     * @param identity Requirement identity
     */
    public RqOptional(final DSLContext ctx, final Long identity) {
        this.ctx = ctx;
        this.identity = identity;
    }

    @Override
    public Long uid() {
        return this.identity;
    }

    @Override
    public Requirement revise(final RequirementInput input) throws Exception {
        final Map<String, String> fields = new RqInputHttp(
            input.print(new MdRqInput()).print()
        ).print(new MdRqInput()).print();
        if (!"OPTIONAL".equals(fields.get("partType")) || !new CrPostgres(
            this.ctx, this.details().get("federal_curriculum_id", Long.class)
        ).grades().contains(Integer.valueOf(fields.get("grade")))) {
            throw new RequirementInputException(
                String.format("Invalid kind or grade for optional requirement %d", this.identity)
            );
        }
        try {
            this.ctx.resultQuery(
                new ClasspathSql("curriculum/requirement/revise-optional.sql").text(),
                Integer.valueOf(fields.get("grade")), Integer.valueOf(fields.get("hours")),
                this.identity
            ).fetchSingle();
        } catch (final DuplicateKeyException exception) {
            throw new RequirementAlreadyExistsException(exception);
        }
        return this;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        final Record row = this.details();
        return media.with("id", this.identity).with("grade", row.get("grade", Integer.class))
            .with("hours", row.get("hours", Integer.class))
            .with("partType", "OPTIONAL");
    }

    private Record details() throws Exception {
        final Record row = this.ctx.resultQuery(
            new ClasspathSql("curriculum/requirement/find-details.sql").text(),
            this.identity, "OPTIONAL"
        ).fetchOne();
        if (row == null) {
            throw new RequirementNotFoundException(
                String.format("Optional requirement %d is unavailable", this.identity)
            );
        }
        return row;
    }
}
