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
 * Localized education levels preserve codes and fall back to the default catalogue.
 * For example, an untranslated language uses the Russian title.
 *
 * @since 0.0.1
 */
final class LvLocalizedTest {

    /**
     * Verifies titles for the supported and untranslated languages.
     *
     * @param level Education level
     * @param language Requested language
     * @param title Russian title
     */
    @ParameterizedTest
    @CsvSource({
        "PRIMARY, ru-RU, Начальное общее образование",
        "BASIC, ru-RU, Основное общее образование",
        "SECONDARY, ru-RU, Среднее общее образование",
        "PRIMARY, de-DE, Начальное общее образование",
        "BASIC, en-US, Основное общее образование"
    })
    void printsTitlesWithoutChangingCodes(
        final Curriculum.Level level, final String language, final String title
    ) {
        MatcherAssert.assertThat(
            "The localized education level lost its code or default title",
            new LvLocalized(level, Locale.forLanguageTag(language)).print(new MdJson()).print(),
            Matchers.equalTo(
                JsonNodeFactory.instance.objectNode().put("code", level.name()).put("title", title)
            )
        );
    }
}
