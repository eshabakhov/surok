/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.media;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * School schedule XSLT views preserve form values and safe navigation.
 *
 * @since 0.0.1
 */
final class XsSchedulesTest {

    /** Verifies the native creation form submits a hidden CSRF token. */
    @Test
    void printsTheProtectedCreationForm() {
        MatcherAssert.assertThat(
            "The schedule creation page lost the hidden CSRF field or form assets",
            XsSchedulesTest.page("create"),
            Matchers.allOf(
                Matchers.containsString("name=\"_csrf\" type=\"hidden\" value=\"token\""),
                Matchers.containsString("id=\"schedule-name\" name=\"name\""),
                Matchers.containsString("Название не может быть пустым"),
                Matchers.containsString("href=\"/css/school/schedules.css\""),
                Matchers.containsString("src=\"/js/school/schedules.js\""),
                Matchers.not(Matchers.containsString("style=\""))
            )
        );
    }

    @Test
    void leavesCreationFieldsEmpty() {
        final String page = XsSchedulesTest.page("create");
        MatcherAssert.assertThat(
            page.substring(page.indexOf("<main")),
            Matchers.allOf(
                Matchers.containsString("action=\"/schools/3/schedules\""),
                Matchers.containsString("value=\"\""),
                Matchers.containsString("placeholder=\"Например: Расписание 2026/2027\""),
                Matchers.not(Matchers.containsString("value=\"Учебный год\""))
            )
        );
    }

    @Test
    void printsEditingValue() {
        MatcherAssert.assertThat(
            "The schedule edit page lost its canonical action, CSRF metadata or value",
            XsSchedulesTest.page("edit"),
            Matchers.allOf(
                Matchers.containsString("action=\"/schools/3/schedules/7\""),
                Matchers.containsString("id=\"schedule-edit-form\""),
                Matchers.containsString("src=\"/js/school/schedule-edit.js\""),
                Matchers.containsString("<meta name=\"_csrf\" content=\"token\""),
                Matchers.containsString("<meta name=\"_csrf_header\" content=\"X-XSRF-TOKEN\""),
                Matchers.containsString("value=\"Учебный год\""),
                Matchers.containsString("Сохранить"),
                Matchers.not(Matchers.containsString("placeholder=\"Например:"))
            )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"list", "create", "edit", "details"})
    void keepsSchoolNavigation(final String view) {
        final String page = XsSchedulesTest.page(view);
        MatcherAssert.assertThat(
            page.substring(page.indexOf("<aside"), page.indexOf("</aside>")),
            Matchers.allOf(
                Matchers.containsString("href=\"/schools/3/schedules/9\""),
                Matchers.containsString("Учебный год"),
                Matchers.containsString("href=\"/schools/3/schedules\""),
                Matchers.containsString("href=\"/schools/3/buildings\""),
                Matchers.not(Matchers.containsString("/buildings/7")),
                Matchers.not(Matchers.containsString("Учебная нагрузка"))
            )
        );
    }

    @Test
    void printsDetailsAndCanonicalRemoval() {
        MatcherAssert.assertThat(
            XsSchedulesTest.page("details"),
            Matchers.allOf(
                Matchers.containsString("Название:"),
                Matchers.containsString("class=\"schedule-loads-link\""),
                Matchers.containsString("Перейти к учебной нагрузке"),
                Matchers.not(Matchers.containsString("class=\"tabs-list schedule-loads\"")),
                Matchers.containsString("class=\"stat-value\">2</div>"),
                Matchers.containsString("class=\"stat-value\">3</div>"),
                Matchers.containsString("href=\"/schools/3/schedules/7/loads\""),
                Matchers.containsString("data-uri=\"/schools/3/schedules/7\""),
                Matchers.containsString("data-redirect=\"/schools/3/schedules\""),
                Matchers.containsString("href=\"/schools/3/schedules/7/edit\""),
                Matchers.not(Matchers.containsString("/api/"))
            )
        );
    }

    @ParameterizedTest
    @CsvSource({"ASC, &uarr;", "DESC, &darr;", "NONE, ↕"})
    void printsSortingAndPagination(final String direction, final String icon) {
        MatcherAssert.assertThat(
            new XsClasspath("xslt/schedules.xsl", "list-fragment").transform(
                XsSchedulesTest.document().replace(
                    "<direction>NONE", String.format("<direction>%s", direction)
                )
            ),
            Matchers.allOf(
                Matchers.containsString("data-sort=\"name\""),
                Matchers.containsString(icon),
                Matchers.containsString("id=\"schedules-results\""),
                Matchers.containsString("id=\"schedules-pagination\""),
                Matchers.containsString("data-limit=\"30\""),
                Matchers.containsString("class=\"pagination-btn pagination-next\""),
                Matchers.not(Matchers.containsString("<html"))
            )
        );
    }

    @Test
    void escapesScheduleNames() {
        MatcherAssert.assertThat(
            new XsClasspath("xslt/schedules.xsl", "list-fragment").transform(
                XsSchedulesTest.document().replace("Учебный год", "Учебный год&lt;script&gt;")
            ),
            Matchers.allOf(
                Matchers.containsString("Учебный год&lt;script&gt;"),
                Matchers.not(Matchers.containsString("title=\"Открыть\">Учебный год<script>")),
                Matchers.containsString("href=\"/schools/3/schedules/7\""),
                Matchers.not(Matchers.containsString("/api/"))
            )
        );
    }

    private static String page(final String view) {
        return new XsClasspath("xslt/schedules.xsl", view)
            .transform(XsSchedulesTest.document());
    }

    private static String document() {
        return """
            <surok>
                <school><id>3</id><name>Школа</name><schedules><schedule>
                    <id>9</id><name>Учебный год</name>
                </schedule></schedules></school>
                <summary><classCount>2</classCount><subjectCount>3</subjectCount></summary>
                <error>empty</error><csrf><parameter>_csrf</parameter><token>token</token>
                    <header>X-XSRF-TOKEN</header></csrf>
                <schedules><schedule>
                    <id>7</id><name>Учебный год</name>
                </schedule><sorts><item><name>name</name><direction>NONE</direction>
                </item></sorts><page><offset>1</offset><limit>15</limit><total>60</total>
                    <totalPages>4</totalPages><hasNext>true</hasNext><hasPrev>false</hasPrev>
                </page></schedules>
            </surok>
            """;
    }
}
