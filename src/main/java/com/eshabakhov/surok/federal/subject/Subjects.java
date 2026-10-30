/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject;

import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.Printable;
import com.eshabakhov.surok.Sorts;

/**
 * Interface for managing {@link Subject} entities.
 *
 * @since 0.0.1
 */
public interface Subjects extends Printable {

    /**
     * Creates a new subject in this catalogue.
     *
     * @param input Federal and additional subject input
     * @return The created {@link Subject}
     * @throws Exception if creation fails
     */
    Subject create(SubjectInput input) throws Exception;

    /**
     * Finds a subject in this catalogue by its unique identifier.
     *
     * @param id The subject in this catalogue ID
     * @return The found {@link Subject}
     * @throws Exception if not found
     */
    Subject subject(long id) throws Exception;

    /**
     * Revises a subject in this catalogue.
     *
     * @param id The subject in this catalogue ID
     * @param input Federal and additional subject input
     * @return Revised subject in this catalogue
     * @throws Exception if revision fails
     */
    default Subject revise(
        final long id,
        final SubjectInput input
    ) throws Exception {
        return this.subject(id).revise(input);
    }

    /**
     * Returns a paginated list of subjects in this catalogue.
     *
     * @param filters Filters for selecting subjects in this catalogue
     * @param page Pagination (contains limit and offset)
     * @param sorts Sorting parameters
     * @return List of {@link Subject} instances
     * @throws Exception if listing fails
     */
    Subjects selection(Filters filters, Page page, Sorts sorts)
        throws Exception;

    /**
     * Iterates over subjects in this catalogue represented by this collection.
     *
     * @return Federal and additional subjects
     */
    Iterable<Subject> iterate();

    /**
     * Removes a subject in this catalogue by its ID.
     *
     * @param id The subject in this catalogue ID
     * @throws Exception if removal fails
     */
    void remove(long id) throws Exception;
}
