/*
 * В© 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum.requirement;

import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.Printable;
import com.eshabakhov.surok.Sorts;

/**
 * Interface for managing {@link Requirement} entities.
 *
 * @since 0.0.1
 */
public interface Requirements extends Printable {

    /**
     * Selects the optional requirement for a grade, or an empty collection if unspecified.
     *
     * @param grade School grade
     * @return Optional requirements
     * @throws Exception If selection fails
     */
    Iterable<Requirement> optional(int grade) throws Exception;

    /**
     * Checks total additional hours for each class against the grade's optional requirement.
     *
     * @param grade School grade
     * @param hours Additional hours per class
     * @throws Exception If the requirement is missing or the total exceeds its maximum
     */
    void check(int grade, int hours) throws Exception;

    /**
     * Creates a new federal curriculum requirement.
     *
     * @param input Federal curriculum requirement input
     * @return The created {@link Requirement}
     * @throws Exception if creation fails
     */
    Requirement create(RequirementInput input) throws Exception;

    /**
     * Finds a federal curriculum requirement by its unique identifier.
     *
     * @param id The federal curriculum requirement ID
     * @return The found {@link Requirement}
     * @throws Exception if not found
     */
    Requirement requirement(long id) throws Exception;

    /**
     * Revises a federal curriculum requirement.
     *
     * @param id The federal curriculum requirement ID
     * @param input Federal curriculum requirement input
     * @return Revised federal curriculum requirement
     * @throws Exception if revision fails
     */
    default Requirement revise(
        final long id,
        final RequirementInput input
    ) throws Exception {
        return this.requirement(id).revise(input);
    }

    /**
     * Returns a paginated list of federal curriculum requirements.
     *
     * @param filters Filters for selecting federal curriculum requirements
     * @param page Pagination (contains limit and offset)
     * @param sorts Sorting parameters
     * @return List of {@link Requirement} instances
     * @throws Exception if listing fails
     */
    Requirements selection(
        Filters filters,
        Page page,
        Sorts sorts
    ) throws Exception;

    /**
     * Iterates over requirements represented by this collection.
     *
     * @return Federal curriculum requirements
     */
    Iterable<Requirement> iterate();

    /**
     * Removes a federal curriculum requirement by its ID.
     *
     * @param id The federal curriculum requirement ID
     * @throws Exception if removal fails
     */
    void remove(long id) throws Exception;
}
