/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Printable;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Education level translated for a web representation.
 * For example, a Russian locale renders PRIMARY as primary general education in Russian.
 *
 * @since 0.0.1
 */
public final class LvLocalized implements Printable {

    /** Education level. */
    private final Curriculum.Level level;

    /** Requested language. */
    private final Locale locale;

    /**
     * New localized education level.
     *
     * @param level Education level
     * @param locale Requested language
     */
    public LvLocalized(final Curriculum.Level level, final Locale locale) {
        this.level = level;
        this.locale = locale;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) {
        return media.with("code", this.level.name()).with(
            "title",
            ResourceBundle.getBundle(
                "localization.levels", this.locale,
                ResourceBundle.Control.getNoFallbackControl(
                    ResourceBundle.Control.FORMAT_PROPERTIES
                )
            ).getString(this.level.name())
        );
    }
}
