/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject.input;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.federal.subject.SubjectInput;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.util.MultiValueMap;

/**
 * HTTP input for subject, for example {@code new SbInputHttp(parameters)}.
 *
 * @since 0.0.1
 */
public final class SbInputHttp implements SubjectInput {

    /** Composed input. */
    private final SubjectInput origin;

    /**
     * New form input.
     *
     * @param parameters Form fields
     */
    public SbInputHttp(final MultiValueMap<String, String> parameters) {
        this(new SbInputName(new SbInput(), parameters.getFirst("name")));
    }

    /**
     * New JSON input.
     *
     * @param json Request document
     */
    public SbInputHttp(final JsonNode json) {
        this(new SbInputName(new SbInput(), json.path("name").asText(null)));
    }

    /**
     * New composed HTTP input.
     *
     * @param origin Validated input
     */
    private SbInputHttp(final SubjectInput origin) {
        this.origin = origin;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.origin.print(media);
    }
}
