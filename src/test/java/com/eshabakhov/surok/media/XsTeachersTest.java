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
 * School teacher XSLT views preserve form values and safe navigation.
 *
 * @since 0.0.1
 */
final class XsTeachersTest {

    /** Verifies the native creation form submits a hidden CSRF token. */
    @Test
    void printsTheProtectedCreationForm() {
        MatcherAssert.assertThat(
            "The teacher creation page lost the hidden CSRF field or form assets",
            XsTeachersTest.page("create"),
            Matchers.allOf(
                Matchers.containsString("name=\"_csrf\" type=\"hidden\" value=\"token\""),
                Matchers.containsString("id=\"teacher-name\" name=\"name\""),
                Matchers.containsString("Имя не может быть пустым"),
                Matchers.containsString("href=\"/css/school/teachers.css\""),
                Matchers.containsString("src=\"/js/school/teachers.js\""),
                Matchers.not(Matchers.containsString("style=\""))
            )
        );
    }

    @Test
    void leavesCreationFieldsEmpty() {
        final String page = XsTeachersTest.page("create");
        MatcherAssert.assertThat(
            page.substring(page.indexOf("<main")),
            Matchers.allOf(
                Matchers.containsString("action=\"/schools/3/teachers\""),
                Matchers.containsString("value=\"\""),
                Matchers.containsString("placeholder=\"Например: Иванов Иван Иванович\""),
                Matchers.not(Matchers.containsString("value=\"Иванов Иван Иванович\""))
            )
        );
    }

    @Test
    void printsEditingValue() {
        MatcherAssert.assertThat(
            "The teacher edit page lost its canonical action, CSRF metadata or value",
            XsTeachersTest.page("edit"),
            Matchers.allOf(
                Matchers.containsString("action=\"/schools/3/teachers/7\""),
                Matchers.containsString("id=\"teacher-edit-form\""),
                Matchers.containsString("src=\"/js/school/teacher-edit.js\""),
                Matchers.containsString("<meta name=\"_csrf\" content=\"token\""),
                Matchers.containsString("<meta name=\"_csrf_header\" content=\"X-XSRF-TOKEN\""),
                Matchers.containsString("value=\"Иванов Иван Иванович\""),
                Matchers.containsString("Сохранить"),
                Matchers.not(Matchers.containsString("placeholder=\"Например:"))
            )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"list", "create", "edit", "details"})
    void keepsSchoolNavigation(final String view) {
        final String page = XsTeachersTest.page(view);
        MatcherAssert.assertThat(
            page.substring(page.indexOf("<aside"), page.indexOf("</aside>")),
            Matchers.allOf(
                Matchers.containsString("href=\"/schools/3/schedules/9\""),
                Matchers.containsString("Учебный год"),
                Matchers.containsString("href=\"/schools/3/teachers\""),
                Matchers.containsString("href=\"/schools/3/buildings\""),
                Matchers.not(Matchers.containsString("/buildings/7"))
            )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"list", "create"})
    void leavesCollectionRowsOutOfSelectedNavigation(final String view) {
        final String page = XsTeachersTest.page(view);
        MatcherAssert.assertThat(
            page.substring(page.indexOf("<aside"), page.indexOf("</aside>")),
            Matchers.not(Matchers.containsString("/teachers/7"))
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"details", "edit"})
    void selectsTeacherInNavigation(final String view) {
        final String page = XsTeachersTest.page(view);
        MatcherAssert.assertThat(
            page.substring(page.indexOf("<aside"), page.indexOf("</aside>")),
            Matchers.containsString("href=\"/schools/3/teachers/7\"")
        );
    }

    @Test
    void printsDetailsAndCanonicalRemoval() {
        MatcherAssert.assertThat(
            XsTeachersTest.page("details"),
            Matchers.allOf(
                Matchers.containsString("ФИО:"),
                Matchers.containsString("data-uri=\"/schools/3/teachers/7\""),
                Matchers.containsString("data-redirect=\"/schools/3/teachers\""),
                Matchers.containsString("href=\"/schools/3/teachers/7/edit\""),
                Matchers.not(Matchers.containsString("/api/"))
            )
        );
    }

    @ParameterizedTest
    @CsvSource({"ASC, &uarr;", "DESC, &darr;", "NONE, ↕"})
    void printsSortingAndPagination(final String direction, final String icon) {
        MatcherAssert.assertThat(
            new XsClasspath("xslt/teachers.xsl", "list-fragment").transform(
                XsTeachersTest.document().replace(
                    "<direction>NONE", String.format("<direction>%s", direction)
                )
            ),
            Matchers.allOf(
                Matchers.containsString("data-sort=\"name\""),
                Matchers.containsString("title=\"Сортировать по ФИО\""),
                Matchers.containsString(icon),
                Matchers.containsString("id=\"teachers-results\""),
                Matchers.containsString("id=\"teachers-pagination\""),
                Matchers.containsString("data-limit=\"30\""),
                Matchers.containsString("class=\"pagination-btn pagination-next\""),
                Matchers.not(Matchers.containsString("<html"))
            )
        );
    }

    @Test
    void escapesTeacherNames() {
        MatcherAssert.assertThat(
            new XsClasspath("xslt/teachers.xsl", "list-fragment").transform(
                XsTeachersTest.document().replace(
                    "Иванов Иван Иванович", "Иванов Иван Иванович&lt;script&gt;"
                )
            ),
            Matchers.allOf(
                Matchers.containsString("Иванов Иван Иванович&lt;script&gt;"),
                Matchers.not(
                    Matchers.containsString("title=\"Открыть\">Иванов Иван Иванович<script>")
                ),
                Matchers.containsString("href=\"/schools/3/teachers/7\""),
                Matchers.not(Matchers.containsString("/api/"))
            )
        );
    }

    private static String page(final String view) {
        return new XsClasspath("xslt/teachers.xsl", view)
            .transform(XsTeachersTest.document());
    }

    private static String document() {
        return """
            <surok>
                <school><id>3</id><name>Школа</name><schedules><schedule>
                    <id>9</id><name>Учебный год</name>
                </schedule></schedules></school>
                <error>empty</error><csrf><parameter>_csrf</parameter><token>token</token>
                    <header>X-XSRF-TOKEN</header></csrf>
                <teachers><teacher>
                    <id>7</id><name>Иванов Иван Иванович</name>
                </teacher><sorts><item><name>name</name><direction>NONE</direction>
                </item></sorts><page><offset>1</offset><limit>15</limit><total>60</total>
                    <totalPages>4</totalPages><hasNext>true</hasNext><hasPrev>false</hasPrev>
                </page></teachers>
            </surok>
            """;
    }
}
