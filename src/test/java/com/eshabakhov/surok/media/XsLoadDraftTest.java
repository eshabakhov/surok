/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.media;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Planning page transformations preserve decisions, diagnostics, and safe text.
 * For example, the matrix offers the plan alongside the school's existing hours.
 *
 * @since 0.0.1
 */
final class XsLoadDraftTest {

    @ParameterizedTest
    @CsvSource({"2, false", "4, true"})
    void rendersClassHoursAndDetectsExceededAllowances(final int planned, final boolean exceeded)
        throws Exception {
        final String page = new XsClasspath("xslt/load-draft.xsl", "draft-fragment").transform(
            Files.readString(Path.of("src/test/resources/pages.xml")).replace(
                "<current>2</current><planned>2</planned>",
                String.format("<current>2</current><planned>%d</planned>", planned)
            )
        );
        MatcherAssert.assertThat(
            "The nested class hours lost their values or bypassed the additional allowance",
            List.of(
                page.contains(String.format("сейчас 2 ч., после применения %d ч.", planned)),
                page.contains("превышен лимит или максимум не задан")
            ),
            Matchers.contains(true, exceeded)
        );
    }

    @Test
    void rendersTheSavedMatrixAndPlanRequirements() throws Exception {
        MatcherAssert.assertThat(
            "The matrix lost its saved hours, decisions, requirements, or protection",
            new XsClasspath("xslt/load-draft.xsl", "page").transform(
                Files.readString(Path.of("src/test/resources/pages.xml"))
            ),
            Matchers.allOf(
                Matchers.containsString("id=\"load-draft\""),
                Matchers.containsString("Пятидневная"),
                Matchers.containsString("Обязательная по плану"),
                Matchers.containsString("data-current=\"5\""),
                Matchers.containsString("data-proposed=\"3\""),
                Matchers.containsString("data-proposed=\"\""),
                Matchers.containsString("Вариативная"),
                Matchers.containsString("data-total=\"true\">5</td>"),
                Matchers.not(Matchers.containsString("data-decision=")),
                Matchers.not(Matchers.containsString("data-mapping=")),
                Matchers.containsString("name=\"_csrf\""),
                Matchers.containsString("/js/school/load-draft.js"),
                Matchers.containsString("/css/school/load-draft.css")
            )
        );
    }

    @Test
    void displaysApplicableClassesEvenWithoutLoads() throws Exception {
        MatcherAssert.assertThat(
            "The matrix omitted an empty applicable class or included a class outside the draft",
            new XsClasspath("xslt/load-draft.xsl", "draft-fragment").transform(
                Files.readString(Path.of("src/test/resources/pages.xml"))
                    .replaceAll("(?s)<loads>.*?</loads>", "<loads/>").replace(
                        "</school-class>",
                        "</school-class><school-class><id>2</id><name>11Б</name><grade>11</grade></school-class>"
                    )
            ),
            Matchers.allOf(
                Matchers.containsString("data-row=\"1\""),
                Matchers.containsString("data-total=\"true\">0</td>"),
                Matchers.not(Matchers.containsString("data-row=\"2\""))
            )
        );
    }

    @Test
    void escapesUserSuppliedText() throws Exception {
        MatcherAssert.assertThat(
            "The planning page rendered supplied markup as executable content",
            new XsClasspath("xslt/load-draft.xsl", "draft-fragment").transform(
                Files.readString(Path.of("src/test/resources/pages.xml"))
                    .replace("Математика", "Математика&lt;script&gt;")
            ),
            Matchers.allOf(
                Matchers.containsString("Математика&lt;script&gt;"),
                Matchers.not(Matchers.containsString("Математика<script>")),
                Matchers.not(Matchers.containsString("/api/"))
            )
        );
    }
}
