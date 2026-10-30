/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum.requirement.input;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.federal.curriculum.requirement.RequirementInput;
import java.util.Map;
import org.springframework.util.MultiValueMap;

/**
 * Federal curriculum requirement input read from HTTP request parameters.
 *
 * @since 0.0.1
 */
public final class RqInputHttp implements RequirementInput {

    /** HTTP request parameters. */
    private final Map<String, String> parameters;

    /**
     * New HTTP requirement input.
     *
     * @param parameters HTTP request parameters
     */
    public RqInputHttp(final MultiValueMap<String, String> parameters) {
        this(parameters.toSingleValueMap());
    }

    /**
     * New input from scalar API fields.
     *
     * @param parameters Requirement fields
     */
    public RqInputHttp(final Map<String, String> parameters) {
        this.parameters = parameters;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        final RequirementInput common = new RqInputHours(
            new RqInputGrade(
                new RqInputPart(new RqInput(), this.parameters.get("partType")),
                this.parameters.get("grade")
            ),
            this.parameters.get("hours")
        );
        final Map<String, String> fields = common.print(new MdRqInput()).print();
        final RequirementInput complete;
        if ("OPTIONAL".equals(fields.get("partType"))) {
            if (this.parameters.containsKey("subjectId")
                || this.parameters.containsKey("subjectName")) {
                throw new RequirementInputException(
                    "An optional requirement cannot contain a subject"
                );
            }
            complete = common;
        } else {
            if (Integer.parseInt(fields.get("hours")) == 0) {
                throw new RequirementInputException("Mandatory hours must be positive");
            }
            complete = new RqInputSubject(common, this.parameters.get("subjectId"));
        }
        return complete.print(media);
    }
}
