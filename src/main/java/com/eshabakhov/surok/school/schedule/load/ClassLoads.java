/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load;

import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.Printable;

/**
 * Teaching loads belonging to a schedule.
 *
 * @since 0.0.1
 */
public interface ClassLoads extends Printable {

    /**
     * Selects a load in this schedule.
     *
     * @param id Load identifier
     * @return Selected load
     * @throws Exception If the load is absent
     */
    ClassLoad load(long id) throws Exception;

    /**
     * Selects a page of loads.
     *
     * @param page Requested page
     * @return Selected loads
     * @throws Exception If selection fails
     */
    ClassLoads selection(Page page) throws Exception;

    /**
     * Iterates over represented loads.
     *
     * @return Loads
     */
    Iterable<ClassLoad> iterate();
}
