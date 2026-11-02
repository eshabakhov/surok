/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Printable;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Study week translated for a web representation.
 * For example, a Russian locale renders FIVE_DAYS as a five-day study week in Russian.
 *
 * @since 0.0.1
 */
public final class WkLocalized implements Printable {

    /** Study week. */
    private final Curriculum.Week week;

    /** Requested language. */
    private final Locale locale;

    /**
     * New localized study week.
     *
     * @param week Study week
     * @param locale Requested language
     */
    public WkLocalized(final Curriculum.Week week, final Locale locale) {
        this.week = week;
        this.locale = locale;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) {
        return media.with("code", this.week.name()).with(
            "title",
            ResourceBundle.getBundle(
                "localization.weeks", this.locale,
                ResourceBundle.Control.getNoFallbackControl(
                    ResourceBundle.Control.FORMAT_PROPERTIES
                )
            ).getString(this.week.name())
        );
    }
}
