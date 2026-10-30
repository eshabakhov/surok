/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load.autofill;

import com.eshabakhov.surok.Printable;

/**
 * Prepared teaching loads and their explicit user decisions.
 * For example, {@code loads.iterate()} visits each prepared class and subject pair.
 *
 * @since 0.0.1
 */
public interface PlannedLoads extends Printable {

    /**
     * Iterates over prepared loads in their original order.
     * Removed loads remain present with an explicit removal decision.
     *
     * @return Prepared loads
     * @throws Exception If the plan is invalid
     */
    Iterable<PlannedLoad> iterate() throws Exception;
}
