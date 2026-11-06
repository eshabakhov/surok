/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok;

import com.eshabakhov.surok.school.SchoolInput;

/**
 * Interface for managing {@link School} entities.
 *
 * @since 0.0.1
 */
public interface Schools extends Printable {

    /**
     * Whether a school belongs to this collection.
     *
     * @param id School identifier
     * @return True if the school belongs to this collection
     */
    boolean contains(long id);

    /**
     * Creates a new school.
     *
     * @param input School input
     * @return The created {@link School}
     * @throws Exception if creation fails
     */
    School create(SchoolInput input) throws Exception;

    /**
     * Finds a school by its unique identifier.
     *
     * @param id The school ID
     * @return The found {@link School}
     * @throws Exception if not found
     */
    School school(long id) throws Exception;

    /**
     * Revises a school.
     *
     * @param id The school ID
     * @param input School input
     * @return Revised school
     * @throws Exception if revision fails
     */
    default School revise(
        final long id,
        final SchoolInput input
    ) throws Exception {
        return this.school(id).revise(input);
    }

    /**
     * Returns a paginated list of schools.
     *
     * @param filters Filters for selecting schools
     * @param page Pagination (contains limit and offset)
     * @param sorts Sorting parameters
     * @return List of {@link School} instances
     * @throws Exception if listing fails
     */
    Schools selection(Filters filters, Page page, Sorts sorts) throws Exception;

    /**
     * Iterates over schools represented by this collection.
     *
     * @return Schools
     */
    Iterable<School> iterate();

    /**
     * Removes a school by its ID.
     *
     * @param id The school ID
     * @throws Exception if removal fails
     */
    void remove(long id) throws Exception;
}
