/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.PrItems;
import com.eshabakhov.surok.Printable;

/**
 * Curriculum offered for load planning with its current requirements.
 * For example, printing an offer lets the school inspect required hours before choosing a plan.
 *
 * @since 0.0.1
 */
public final class CrOffered implements Printable {

    /** Federal curriculum. */
    private final Curriculum origin;

    /**
     * New planning offer.
     *
     * @param origin Available curriculum
     */
    public CrOffered(final Curriculum origin) {
        this.origin = origin;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.origin.print(media).with(
            "requirements", new PrItems("requirement", this.origin.requirements().iterate())
        );
    }
}
