/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum.requirement;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.federal.curriculum.requirement.input.MdRqInput;
import com.eshabakhov.surok.federal.subject.Subjects;
import java.util.Map;

/**
 * Unsaved requirement with blank fields until a grade, subject and hours are entered.
 * For example, {@code draft.revise(input)} preserves the subject's object representation.
 *
 * @since 0.0.1
 */
public final class RqDraft implements Requirement {

    /** Fields entered before creation. */
    private final RequirementInput input;

    /** Federal subjects available for this requirement. */
    private final Subjects subjects;

    /**
     * New unsaved requirement.
     *
     * @param input Fields entered before creation
     * @param subjects Available federal subjects
     */
    public RqDraft(final RequirementInput input, final Subjects subjects) {
        this.input = input;
        this.subjects = subjects;
    }

    @Override
    public Long uid() {
        throw new IllegalStateException("An unsaved requirement has no identifier");
    }

    @Override
    public Requirement revise(final RequirementInput fields) {
        return new RqDraft(fields, this.subjects);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        final Map<String, String> fields = this.input.print(new MdRqInput()).print();
        Media<T> printed = media.with("grade", fields.getOrDefault("grade", ""))
            .with("hours", fields.getOrDefault("hours", ""))
            .with("partType", fields.getOrDefault("partType", ""));
        if (fields.containsKey("subjectId")) {
            printed = printed.with(
                "subject", this.subjects.subject(Long.parseLong(fields.get("subjectId")))
            );
        }
        return printed;
    }
}
