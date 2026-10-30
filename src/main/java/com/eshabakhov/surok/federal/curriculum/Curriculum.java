/*
 * В© 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Printable;
import com.eshabakhov.surok.federal.curriculum.requirement.Requirements;
import java.util.List;

/**
 * Federal curriculum plan domain entity.
 *
 * <p>Does not expose data via getters. Instead, prints itself
 * into a {@link Media} via {@link Printable#print(Media)}.</p>
 *
 * @since 0.0.1
 */
public interface Curriculum extends Printable {

    /**
     * Returns the unique identifier of this curriculum.
     *
     * @return Curriculum ID
     */
    Long uid();

    /**
     * Lists the grades belonging to this curriculum's education level.
     *
     * @return Ordered grades, or an empty list when the curriculum is unavailable
     * @throws Exception If the education level cannot be read
     */
    List<Integer> grades() throws Exception;

    /**
     * Revises this curriculum with the supplied input.
     *
     * @param input Curriculum input
     * @return Revised curriculum
     * @throws Exception If revision fails
     */
    Curriculum revise(CurriculumInput input) throws Exception;

    /**
     * Returns the requirements collection for this curriculum.
     *
     * @return Federal curriculum requirements
     */
    Requirements requirements();

    /**
     * Education levels supported by federal curriculum plans.
     *
     * @since 0.0.1
     */
    enum Level {

        /**
         * Primary general education, grades 1–4.
         */
        PRIMARY,
        /**
         * Basic general education, grades 5–9.
         */
        BASIC,
        /**
         * Secondary general education, grades 10–11.
         */
        SECONDARY
    }

    /**
     * Study week types supported by federal curriculum plans.
     *
     * @since 0.0.1
     */
    enum Week {

        /**
         * Five-day study week.
         */
        FIVE_DAYS,
        /**
         * Six-day study week.
         */
        SIX_DAYS
    }
}
