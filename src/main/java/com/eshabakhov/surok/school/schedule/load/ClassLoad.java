/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load;

import com.eshabakhov.surok.Printable;

/**
 * A subject's weekly teaching load for a school class.
 *
 * @since 0.0.1
 */
public interface ClassLoad extends Printable {

    /**
     * Load identifier.
     *
     * @return Identifier
     */
    Long uid();
}
