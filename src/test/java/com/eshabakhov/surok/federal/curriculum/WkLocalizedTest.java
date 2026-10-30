/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum;

import com.eshabakhov.surok.media.MdJson;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.Locale;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Localized study weeks preserve codes and fall back to the default catalogue.
 * For example, an untranslated language uses the Russian title.
 *
 * @since 0.0.1
 */
final class WkLocalizedTest {

    /**
     * Verifies titles for the supported and untranslated languages.
     *
     * @param week Study week
     * @param language Requested language
     * @param title Russian title
     */
    @ParameterizedTest
    @CsvSource({
        "FIVE_DAYS, ru-RU, Пятидневная",
        "SIX_DAYS, ru-RU, Шестидневная",
        "FIVE_DAYS, en-US, Пятидневная",
        "SIX_DAYS, de-DE, Шестидневная"
    })
    void printsTitlesWithoutChangingCodes(
        final Curriculum.Week week, final String language, final String title
    ) {
        MatcherAssert.assertThat(
            "The localized study week lost its code or default title",
            new WkLocalized(week, Locale.forLanguageTag(language)).print(new MdJson()).print(),
            Matchers.equalTo(
                JsonNodeFactory.instance.objectNode().put("code", week.name()).put("title", title)
            )
        );
    }
}
