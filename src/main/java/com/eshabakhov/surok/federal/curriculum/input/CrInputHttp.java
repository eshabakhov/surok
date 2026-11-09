/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum.input;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.federal.curriculum.CurriculumInput;
import java.util.Map;
import org.springframework.util.MultiValueMap;

/**
 * Federal curriculum input read from HTTP request parameters.
 *
 * @since 0.0.1
 */
public final class CrInputHttp implements CurriculumInput {

    /** HTTP request parameters. */
    private final Map<String, String> parameters;

    /**
     * New HTTP curriculum input.
     *
     * @param parameters HTTP request parameters
     */
    public CrInputHttp(final MultiValueMap<String, String> parameters) {
        this(parameters.toSingleValueMap());
    }

    /**
     * New input from a versioned JSON document.
     *
     * @param parameters Curriculum fields
     */
    public CrInputHttp(final Map<String, String> parameters) {
        this.parameters = parameters;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return new CrInputDescription(
            new CrInputYear(
                new CrInputVersion(
                    new CrInputWeek(
                        new CrInputLevel(
                            new CrInputTitle(
                                new CrInput(),
                                this.parameters.get("title")
                            ),
                            this.parameters.get("level")
                        ),
                        this.parameters.get("week")
                    ),
                    this.parameters.get("version")
                ),
                this.parameters.get("year")
            ),
            this.parameters.get("description")
        ).print(media);
    }
}
