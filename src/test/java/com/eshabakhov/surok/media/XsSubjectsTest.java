/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.media;

import java.util.Map;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * School subject XSLT views preserve form values and safe navigation.
 *
 * @since 0.0.1
 */
final class XsSubjectsTest {

    @ParameterizedTest
    @ValueSource(strings = {"create", "edit"})
    void printsProtectedForms(final String view) {
        MatcherAssert.assertThat(
            new XsClasspath("xslt/subjects.xsl", view).transform(
                XsSubjectsTest.document().replaceAll("(?s)<school>.*?</school>", "")
            ),
            Matchers.allOf(
                Matchers.containsString("name=\"_csrf\" type=\"hidden\" value=\"token\""),
                Matchers.containsString("id=\"subject-name\" name=\"name\""),
                Matchers.containsString("Название не может быть пустым"),
                Matchers.containsString("href=\"/css/school/subjects.css\""),
                Matchers.containsString("src=\"/js/school/subjects.js\""),
                Matchers.not(Matchers.containsString("style=\""))
            )
        );
    }

    @Test
    void createsSchoolSubjectsByNameInsteadOfLinkingCatalogueEntries() {
        MatcherAssert.assertThat(
            "The school form linked a catalogue entry instead of creating an additional subject",
            XsSubjectsTest.page("create"),
            Matchers.allOf(
                Matchers.containsString("action=\"/schools/3/subjects/create\""),
                Matchers.containsString("id=\"subject-name\" name=\"name\""),
                Matchers.not(Matchers.containsString("name=\"subjectId\"")),
                Matchers.not(Matchers.containsString("data-linked="))
            )
        );
    }

    @Test
    void leavesCreationFieldsEmpty() {
        final String page = new XsClasspath("xslt/subjects.xsl", "create").transform(
            XsSubjectsTest.document().replaceAll("(?s)<school>.*?</school>", "")
        );
        MatcherAssert.assertThat(
            page.substring(page.indexOf("<main")),
            Matchers.allOf(
                Matchers.containsString("action=\"/federal/subjects/create\""),
                Matchers.containsString("value=\"\""),
                Matchers.containsString("placeholder=\"Например: Математика\""),
                Matchers.not(Matchers.containsString("value=\"Математика\""))
            )
        );
    }

    @Test
    void printsEditingValue() {
        MatcherAssert.assertThat(
            new XsClasspath("xslt/subjects.xsl", "edit").transform(
                XsSubjectsTest.document().replaceAll("(?s)<school>.*?</school>", "")
            ),
            Matchers.allOf(
                Matchers.containsString("action=\"/federal/subjects/7/edit\""),
                Matchers.containsString("value=\"Математика\""),
                Matchers.containsString("Сохранить"),
                Matchers.not(Matchers.containsString("placeholder=\"Например:"))
            )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"list", "create", "edit", "details"})
    void keepsSchoolNavigation(final String view) {
        final String page = XsSubjectsTest.page(view);
        MatcherAssert.assertThat(
            page.substring(page.indexOf("<aside"), page.indexOf("</aside>")),
            Matchers.allOf(
                Matchers.containsString("href=\"/schools/3/schedules/9\""),
                Matchers.containsString("Учебный год"),
                Matchers.containsString("href=\"/schools/3/subjects\""),
                Matchers.containsString("href=\"/schools/3/buildings\""),
                Matchers.not(Matchers.containsString("/buildings/7"))
            )
        );
    }

    @Test
    void printsDetailsAndCanonicalRemoval() {
        MatcherAssert.assertThat(
            XsSubjectsTest.page("details"),
            Matchers.allOf(
                Matchers.containsString("Название:"),
                Matchers.containsString("data-uri=\"/schools/3/subjects/7\""),
                Matchers.containsString("data-redirect=\"/schools/3/subjects\""),
                Matchers.not(Matchers.containsString("href=\"/federal/subjects/7/edit\"")),
                Matchers.not(Matchers.containsString("/api/"))
            )
        );
    }

    @ParameterizedTest
    @CsvSource({"ASC, &uarr;", "DESC, &darr;", "NONE, ↕"})
    void printsSortingAndPagination(final String direction, final String icon) {
        MatcherAssert.assertThat(
            new XsClasspath("xslt/subjects.xsl", "list-fragment").transform(
                XsSubjectsTest.document().replace(
                    "<direction>NONE", String.format("<direction>%s", direction)
                )
            ),
            Matchers.allOf(
                Matchers.containsString("data-sort=\"name\""),
                Matchers.containsString(icon),
                Matchers.containsString("id=\"subjects-results\""),
                Matchers.containsString("id=\"subjects-pagination\""),
                Matchers.containsString("data-limit=\"30\""),
                Matchers.containsString("class=\"pagination-btn pagination-next\""),
                Matchers.not(Matchers.containsString("<html"))
            )
        );
    }

    @Test
    void escapesSubjectNames() {
        MatcherAssert.assertThat(
            new XsClasspath("xslt/subjects.xsl", "list-fragment").transform(
                XsSubjectsTest.document().replace("Математика", "Математика&lt;script&gt;")
            ),
            Matchers.allOf(
                Matchers.containsString("Математика&lt;script&gt;"),
                Matchers.not(Matchers.containsString("title=\"Открыть\">Математика<script>")),
                Matchers.containsString("href=\"/schools/3/subjects/7\""),
                Matchers.not(Matchers.containsString("/api/"))
            )
        );
    }

    @Test
    void printsCatalogueManagementLinksForAnAdministrator() {
        MatcherAssert.assertThat(
            "The shared subject used school routes or hid administrator actions",
            new XsClasspath("xslt/subjects.xsl", "details").transform(
                """
                <surok><user><admin>true</admin></user>
                    <subjects><subject><id>29</id><name>Математика</name>
                    </subject></subjects></surok>
                """
            ),
            Matchers.allOf(
                Matchers.containsString("href=\"/federal/subjects/29/edit\""),
                Matchers.containsString("data-uri=\"/federal/subjects/29\""),
                Matchers.not(Matchers.containsString("/schools//"))
            )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"list", "details"})
    void hidesCatalogueManagementActionsFromReaders(final String view) {
        MatcherAssert.assertThat(
            "A reader received controls for changing the federal catalogue",
            new XsClasspath("xslt/subjects.xsl", view).transform(
                """
                <surok><user><admin>false</admin></user>
                    <subjects><subject><id>29</id><name>Математика</name>
                    </subject></subjects></surok>
                """
            ),
            Matchers.allOf(
                Matchers.containsString("Математика"),
                Matchers.not(Matchers.containsString("/federal/subjects/create")),
                Matchers.not(Matchers.containsString("/federal/subjects/29/edit")),
                Matchers.not(Matchers.containsString("subject-delete"))
            )
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void usesTheSameConciseCreationLabel(final boolean federal) {
        MatcherAssert.assertThat(
            "The subject creation button included an unnecessary catalogue qualifier",
            new XsClasspath("xslt/subjects.xsl", "list").transform(
                String.format(
                    """
                    <surok><user><admin>true</admin></user><editable>true</editable>%s
                    <subjects/></surok>
                    """, Map.of(
                        true, "", false, "<school><id>31</id><name>Лицей</name></school>"
                    ).get(federal)
                )
            ),
            Matchers.allOf(
                Matchers.containsString("Добавить предмет"),
                Matchers.not(Matchers.containsString("Добавить федеральный предмет")),
                Matchers.not(Matchers.containsString("Добавить дополнительный предмет"))
            )
        );
    }

    private static String page(final String view) {
        return new XsClasspath("xslt/subjects.xsl", view)
            .transform(XsSubjectsTest.document());
    }

    private static String document() {
        return """
            <surok>
                <editable>true</editable><user><admin>true</admin></user>
                <school><id>3</id><name>Школа</name><schedules><schedule>
                    <id>9</id><name>Учебный год</name>
                </schedule></schedules></school>
                <error>empty</error><csrf><parameter>_csrf</parameter><token>token</token></csrf>
                <subjects><subject>
                    <id>7</id><name>Математика</name>
                </subject><sorts><item><name>name</name><direction>NONE</direction>
                </item></sorts><page><offset>1</offset><limit>15</limit><total>60</total>
                    <totalPages>4</totalPages><hasNext>true</hasNext><hasPrev>false</hasPrev>
                </page></subjects>
            </surok>
            """;
    }
}
