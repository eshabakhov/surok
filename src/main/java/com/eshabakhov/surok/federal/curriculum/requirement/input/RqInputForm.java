/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum.requirement.input;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.federal.curriculum.requirement.RequirementInput;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Requirement form selection translated into explicit requirement fields.
 * For example, selecting OPTIONAL creates a grade limit without a subject identity.
 *
 * @since 0.0.1
 */
public final class RqInputForm implements RequirementInput {

    /** Submitted form fields. */
    private final Map<String, String> parameters;

    /**
     * New form input.
     *
     * @param parameters Submitted fields
     */
    public RqInputForm(final Map<String, String> parameters) {
        this.parameters = parameters;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        final Map<String, String> fields = new LinkedHashMap<>(this.parameters);
        if (fields.containsKey("selection")) {
            final String selected = fields.remove("selection");
            if ("OPTIONAL".equals(selected)) {
                fields.put("partType", "OPTIONAL");
                fields.remove("subjectId");
            } else {
                fields.put("partType", "MANDATORY");
                fields.put("subjectId", selected);
            }
        }
        return new RqInputHttp(fields).print(media);
    }
}
