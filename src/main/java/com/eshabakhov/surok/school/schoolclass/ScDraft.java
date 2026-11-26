/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schoolclass;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.school.schoolclass.input.MdScInput;
import java.util.Map;

/**
 * Unsaved class whose grade and litera populate the creation form.
 * For example, {@code draft.revise(input)} returns new fields without writing to storage.
 *
 * @since 0.0.1
 */
public final class ScDraft implements SchoolClass {

    /** Fields entered before creation. */
    private final SchoolClassInput input;

    /**
     * New unsaved class.
     *
     * @param input Fields entered before creation
     */
    public ScDraft(final SchoolClassInput input) {
        this.input = input;
    }

    @Override
    public Long uid() {
        throw new IllegalStateException("An unsaved class has no identifier");
    }

    @Override
    public int grade() throws Exception {
        final String grade = this.input.print(new MdScInput()).print().getOrDefault("grade", "");
        if (grade.isEmpty()) {
            throw new IllegalStateException("The class draft has no grade yet");
        }
        return Integer.parseInt(grade);
    }

    @Override
    public SchoolClass revise(final SchoolClassInput fields) {
        return new ScDraft(fields);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        final Map<String, String> fields = this.input.print(new MdScInput()).print();
        return media.with(
            "name", String.format(
                "%s%s", fields.getOrDefault("grade", ""), fields.getOrDefault("litera", "")
            )
        ).with("grade", fields.getOrDefault("grade", ""))
            .with("litera", fields.getOrDefault("litera", ""));
    }
}
