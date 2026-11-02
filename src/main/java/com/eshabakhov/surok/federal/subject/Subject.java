/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject;

import com.eshabakhov.surok.Printable;

/**
 * Shared or additional subject printed into a supplied medium.
 *
 * @since 0.0.1
 */
public interface Subject extends Printable {

    /**
     * Subject identifier.
     *
     * @return Identifier
     */
    Long uid();

    /**
     * Subject name, normalized when decorated with {@link SbNormalized}.
     *
     * @return Subject name
     * @throws Exception If the subject cannot be read
     */
    String name() throws Exception;

    /**
     * Catalogue owning this subject.
     *
     * @return Federal or school origin
     * @throws Exception If the subject cannot be read
     */
    Origin origin() throws Exception;

    /**
     * Revises the subject using the supplied input.
     *
     * @param input Subject input
     * @return Revised subject
     * @throws Exception If revision fails
     */
    Subject revise(SubjectInput input) throws Exception;

    /**
     * Ownership of a subject, independently of its hours in a curriculum.
     *
     * @since 0.0.1
     */
    enum Origin {

        /** Common federal catalogue. */
        FEDERAL,

        /** Additional subject owned by one school. */
        SCHOOL
    }
}
