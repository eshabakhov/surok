/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum.requirement;

import com.eshabakhov.surok.Printable;

/**
 * Federal curriculum requirement domain entity.
 * For example, {@code requirement.revise(input)} updates the fields of its requirement kind.
 *
 * @since 0.0.1
 */
public interface Requirement extends Printable {

    /**
     * Returns the unique identifier of this requirement.
     *
     * @return Requirement ID
     */
    Long uid();

    /**
     * Revises this requirement.
     *
     * @param input Requirement input
     * @return Revised requirement
     * @throws Exception If revision fails
     */
    Requirement revise(RequirementInput input) throws Exception;

    /**
     * Parts of a federal curriculum requirement.
     *
     * @since 0.0.1
     */
    enum PartType {

        /**
         * Mandatory part of a curriculum plan.
         */
        MANDATORY,
        /**
         * Optional part of a curriculum plan.
         */
        OPTIONAL
    }
}
