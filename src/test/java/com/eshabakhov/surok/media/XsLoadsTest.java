/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.media;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Read-only teaching load views with draft navigation.
 *
 * @since 0.0.1
 */
final class XsLoadsTest {

    @Test
    void directsAllLoadChangesToTheDraft() {
        MatcherAssert.assertThat(
            "The load page still offered a separate manual editor",
            XsLoadsTest.page("list"),
            Matchers.allOf(
                Matchers.containsString("href=\"/schools/3/schedules/9/loads/draft\""),
                Matchers.not(Matchers.containsString("/loads/create")),
                Matchers.not(Matchers.containsString("/loads/7/edit")),
                Matchers.not(Matchers.containsString("load-delete")),
                Matchers.not(Matchers.containsString("<form")),
                Matchers.not(Matchers.containsString("<dialog"))
            )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"list", "details"})
    void printsReadOnlyPagesWithExternalAssets(final String view) {
        MatcherAssert.assertThat(
            "The read-only load page lost its assets or exposed a manual editor",
            XsLoadsTest.page(view),
            Matchers.allOf(
                Matchers.containsString("href=\"/css/school/class-loads.css\""),
                Matchers.containsString("src=\"/js/school/class-loads.js\""),
                Matchers.containsString("href=\"/schools/3/schedules/9/loads/draft\""),
                Matchers.not(Matchers.containsString("name=\"hours\"")),
                Matchers.not(Matchers.containsString("/loads/7/edit")),
                Matchers.not(Matchers.containsString("load-delete")),
                Matchers.not(Matchers.containsString("<form")),
                Matchers.not(Matchers.containsString("/api/"))
            )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"list", "details"})
    void keepsScheduleContextInNavigation(final String view) {
        final String page = XsLoadsTest.page(view);
        MatcherAssert.assertThat(
            page.substring(page.indexOf("<aside"), page.indexOf("</aside>")),
            Matchers.allOf(
                Matchers.containsString("href=\"/schools/3/schedules/9/loads\""),
                Matchers.containsString("Учебный год"),
                Matchers.not(Matchers.containsString("/buildings/7")),
                Matchers.not(Matchers.containsString("/classes/7")),
                Matchers.not(Matchers.containsString("/classes/10")),
                Matchers.not(Matchers.containsString("/subjects/30"))
            )
        );
    }

    @Test
    void printsEscapedFragmentWithPagination() {
        MatcherAssert.assertThat(
            new XsClasspath("xslt/class-loads.xsl", "list-fragment").transform(
                XsLoadsTest.document().replace("Математика", "&lt;script&gt;Математика")
            ),
            Matchers.allOf(
                Matchers.containsString("&lt;script&gt;Математика"),
                Matchers.containsString("id=\"loads-results\""),
                Matchers.containsString("id=\"loads-pagination\""),
                Matchers.containsString("data-offset=\"2\""),
                Matchers.containsString("data-limit=\"30\""),
                Matchers.not(Matchers.containsString("<html")),
                Matchers.not(Matchers.containsString("<dialog"))
            )
        );
    }

    @Test
    void showsEmptyState() {
        final String document = XsLoadsTest.document();
        MatcherAssert.assertThat(
            new XsClasspath("xslt/class-loads.xsl", "list-fragment").transform(
                String.format(
                    "%s%s", document.substring(0, document.indexOf("<load>")),
                    document.substring(document.indexOf("</load>") + "</load>".length())
                )
            ),
            Matchers.containsString("Нагрузка пока не заполнена. Перейдите в автозаполнение.")
        );
    }

    private static String page(final String view) {
        return new XsClasspath("xslt/class-loads.xsl", view).transform(XsLoadsTest.document());
    }

    private static String document() {
        return """
            <surok>
                <school><id>3</id><name>Школа</name><schedules><schedule>
                    <id>9</id><name>Учебный год</name>
                </schedule></schedules></school>
                <schedule><id>9</id><name>Учебный год</name></schedule>
                <csrf><parameter>_csrf</parameter><token>token</token></csrf><error></error>
                <classes><school-class><id>10</id><name>5А</name></school-class></classes>
                <subjects><subject><id>30</id><name>Математика</name></subject></subjects>
                <loads><load><id>7</id><schoolClass><id>10</id><name>5А</name></schoolClass>
                    <subject><id>30</id><name>Математика</name><origin>FEDERAL</origin></subject>
                    <hours>5</hours>
                </load><page><offset>1</offset><limit>15</limit><total>30</total>
                    <totalPages>2</totalPages><hasNext>true</hasNext><hasPrev>false</hasPrev>
                </page></loads>
            </surok>
            """;
    }
}
