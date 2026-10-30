/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load.autofill;

import com.eshabakhov.surok.federal.curriculum.CrPostgres;
import com.eshabakhov.surok.federal.curriculum.Curriculum;
import com.eshabakhov.surok.federal.subject.SbPostgres;
import com.eshabakhov.surok.federal.subject.Subject;
import com.eshabakhov.surok.school.schedule.load.hours.WeeklyHours;
import com.eshabakhov.surok.school.schedule.load.hours.WhFixed;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.jooq.DSLContext;

/**
 * Versioned input decoded at the HTTP boundary without JSON in planning logic.
 * For example, curriculum and subject references contain an id inside a nested object.
 *
 * @since 0.0.1
 */
public final class LdiJson implements LoadDraftInput {

    /** Database context for referenced subjects. */
    private final DSLContext ctx;

    /** Submitted transport document. */
    private final JsonNode document;

    /**
     * New HTTP input.
     *
     * @param ctx Database context
     * @param document Submitted document
     */
    public LdiJson(final DSLContext ctx, final JsonNode document) {
        this.ctx = ctx;
        this.document = document;
    }

    @Override
    public Curriculum curriculum() throws LoadInputException {
        return new CrPostgres(this.ctx, this.child("curriculum").child("id").identifier());
    }

    @Override
    public Map<Integer, Map<Subject, WeeklyHours>> additions() throws LoadInputException {
        if (this.document.has("classIds") || this.document.has("mappings")
            || this.document.has("changes") || this.document.has("optionalSubjects")) {
            throw new LoadInputException(
                "Class selections, mappings and load decisions are obsolete"
            );
        }
        final Map<Integer, Map<Subject, WeeklyHours>> additions = new LinkedHashMap<>();
        for (final JsonNode addition : this.array("additions")) {
            final LdiJson entry = new LdiJson(this.ctx, addition);
            final int grade = entry.child("grade").number();
            final Map<Subject, WeeklyHours> hours = new LinkedHashMap<>();
            final Set<Long> identifiers = new HashSet<>();
            for (final JsonNode subject : entry.array("subjects")) {
                final LdiJson input = new LdiJson(this.ctx, subject);
                final Subject selected = new SbPostgres(
                    this.ctx, input.child("subject").child("id").identifier()
                );
                if (!identifiers.add(selected.uid())) {
                    throw new LoadInputException(
                        String.format(
                            "Duplicate additional subject %d for grade %d", selected.uid(), grade
                        )
                    );
                }
                hours.put(selected, new WhFixed(input.child("hours").number()));
            }
            if (additions.putIfAbsent(grade, Map.copyOf(hours)) != null) {
                throw new LoadInputException(String.format("Duplicate additional grade %d", grade));
            }
        }
        return Map.copyOf(additions);
    }

    private LdiJson child(final String field) {
        return new LdiJson(this.ctx, this.document.path(field));
    }

    private Iterable<JsonNode> array(final String field) throws LoadInputException {
        final JsonNode value = this.document.path(field);
        if (!value.isArray()) {
            throw new LoadInputException(String.format("Draft field '%s' must be an array", field));
        }
        return value;
    }

    private long identifier() throws LoadInputException {
        if (!this.document.isIntegralNumber() || !this.document.canConvertToLong()
            || this.document.longValue() <= 0L) {
            throw new LoadInputException("Draft references must be positive integer identifiers");
        }
        return this.document.longValue();
    }

    private int number() throws LoadInputException {
        if (!this.document.isIntegralNumber() || !this.document.canConvertToInt()
            || this.document.intValue() < 0) {
            throw new LoadInputException("Draft grades and hours must be nonnegative integers");
        }
        return this.document.intValue();
    }
}
