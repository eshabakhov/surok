/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.media;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests {@link XsClasspath} transformations of school pages.
 * For example, school forms and building fragments retain safe navigation.
 *
 * @since 0.0.1
 */
final class XsClasspathTest {

    /**
     * Verifies headers use build metadata even with unfiltered stylesheets.
     *
     * @param stylesheet Page stylesheet
     * @param directory Isolated build metadata directory
     * @throws Exception If metadata cannot be prepared or the page cannot be rendered
     */
    @ParameterizedTest
    @ValueSource(strings = {"schools", "federal-curriculums"})
    void displaysVersionFromBuildMetadata(
        final String stylesheet, @TempDir final Path directory
    ) throws Exception {
        final String version = String.format("%s-SNAPSHOT", UUID.randomUUID());
        Files.writeString(
            Files.createDirectories(directory.resolve("META-INF"))
                .resolve("build-info.properties"),
            String.format("build.version=%s%n", version)
        );
        final Thread thread = Thread.currentThread();
        @SuppressWarnings("PMD.UnnecessaryLocalRule")
        final ClassLoader previous = thread.getContextClassLoader();
        try (
            URLClassLoader loader = new URLClassLoader(
                new URL[]{
                    directory.toUri().toURL(),
                    Path.of("src", "main", "resources").toUri().toURL(),
                },
                ClassLoader.getPlatformClassLoader()
            )
        ) {
            thread.setContextClassLoader(loader);
            MatcherAssert.assertThat(
                "Page header ignored the version in build metadata",
                new XsClasspath(String.format("xslt/%s.xsl", stylesheet), "list")
                    .transform("<surok><school><name>Лицей</name></school></surok>"),
                Matchers.containsString(
                    String.format("class=\"app-header-logo-version\">v%s</span>", version)
                )
            );
        } finally {
            thread.setContextClassLoader(previous);
        }
    }

    /** Verifies fragments do not contain the page layout. */
    @Test
    void escapesSchoolNamesInListFragments() {
        MatcherAssert.assertThat(
            "School fragment exposed markup or included page chrome",
            new XsClasspath("xslt/schools.xsl", "list-fragment").transform(
                """
                <surok><schools><school><id>7</id><name>Школа &lt;script&gt;</name></school>\
                </schools></surok>\
                """
            ),
            Matchers.allOf(
                Matchers.containsString("Школа &lt;script&gt;"),
                Matchers.not(Matchers.containsString("<html"))
            )
        );
    }

    /**
     * Verifies both forms use the shared CSRF input.
     *
     * @param view Selected form
     */
    @ParameterizedTest
    @ValueSource(strings = {"create", "edit"})
    void includesCsrfInSchoolForms(final String view) {
        final String stylesheet;
        if ("create".equals(view)) {
            stylesheet = "xslt/schools.xsl";
        } else {
            stylesheet = "xslt/school.xsl";
        }
        MatcherAssert.assertThat(
            "School form lost its CSRF field",
            new XsClasspath(stylesheet, view).transform(
                """
                <surok>
                    <csrf><parameter>_csrf</parameter><token>токен</token></csrf>
                    <schools><school><id>7</id><name>Лицей</name></school></schools>
                </surok>
                """
            ),
            Matchers.allOf(
                Matchers.containsString("name=\"_csrf\""),
                Matchers.containsString("value=\"токен\"")
            )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"building", "cabinet"})
    void escapesNamesWithoutIncludingPageChrome(final String entity) {
        MatcherAssert.assertThat(
            "Fragment lost its data, exposed markup or included a full page",
            new XsClasspath(String.format("xslt/%ss.xsl", entity), "list-fragment").transform(
                String.format(
                    """
                    <surok>
                        <school><id>3</id><name>Школа</name></school>
                        <building><id>4</id><name>Корпус</name></building>
                        <%1$ss><%1$s><id>7</id>
                        <name>Зал &lt;script&gt;проверка&lt;/script&gt;</name></%1$s>
                        <page><offset>1</offset><limit>15</limit>
                        <total>1</total><totalPages>1</totalPages>
                        <hasNext>false</hasNext><hasPrev>false</hasPrev></page></%1$ss>
                    </surok>
                    """, entity
                )
            ),
            Matchers.allOf(
                Matchers.containsString("Зал &lt;script&gt;проверка&lt;/script&gt;"),
                Matchers.containsString("data-id=\"7\""),
                Matchers.not(Matchers.containsString("<html"))
            )
        );
    }

    @ParameterizedTest
    @CsvSource({"building, list", "building, create", "cabinet, list", "cabinet, create"})
    void keepsCollectionRowsOutOfSelectedNavigation(final String entity, final String view) {
        final String printed = XsClasspathTest.page(entity, view, false);
        MatcherAssert.assertThat(
            "Collection navigation selected a row or lost school schedules",
            printed.substring(printed.indexOf("<aside"), printed.indexOf("</aside>")),
            Matchers.allOf(
                Matchers.containsString("href=\"/schools/3/schedules/5\""),
                Matchers.containsString("Первое полугодие"),
                Matchers.not(Matchers.containsString("Южный зал"))
            )
        );
    }

    @ParameterizedTest
    @CsvSource({"building, details", "building, edit", "cabinet, details", "cabinet, edit"})
    void keepsSelectedNavigationOnEntityPages(final String entity, final String view) {
        final String printed = XsClasspathTest.page(entity, view, false);
        MatcherAssert.assertThat(
            "Entity pages lost the selected building or cabinet in navigation",
            printed.substring(printed.indexOf("<aside"), printed.indexOf("</aside>")),
            Matchers.containsString("Южный зал")
        );
    }

    @ParameterizedTest
    @CsvSource({"building, 'Например: Корпус 1'", "cabinet, 'Например: Кабинет 301'"})
    void keepsCreationFormsEmpty(final String entity, final String example) {
        MatcherAssert.assertThat(
            "Creation form lost its guidance or copied an existing name",
            XsClasspathTest.page(entity, "create", false),
            Matchers.allOf(
                Matchers.containsString("Заполните данные для создания"),
                Matchers.containsString(String.format("placeholder=\"%s\"", example)),
                Matchers.containsString("value=\"\""),
                Matchers.not(Matchers.containsString("value=\"Южный зал\"")),
                Matchers.not(Matchers.containsString("class=\"error-message\""))
            )
        );
    }

    @ParameterizedTest
    @CsvSource({"building, create", "building, edit", "cabinet, create", "cabinet, edit"})
    void printsValidationErrorsOnForms(final String entity, final String view) {
        MatcherAssert.assertThat(
            "Invalid form submissions must show the existing validation message",
            XsClasspathTest.page(entity, view, true),
            Matchers.allOf(
                Matchers.containsString("class=\"error-message\""),
                Matchers.containsString("Название не может быть пустым"),
                Matchers.containsString("name=\"_csrf\" type=\"hidden\" value=\"token\"")
            )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"building", "cabinet"})
    void keepsEditingValues(final String entity) {
        MatcherAssert.assertThat(
            "Editing must keep the current name without creation guidance",
            XsClasspathTest.page(entity, "edit", false),
            Matchers.allOf(
                Matchers.containsString("value=\"Южный зал\""),
                Matchers.not(Matchers.containsString("placeholder=\"Например:")),
                Matchers.containsString("Сохранить")
            )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"building", "cabinet"})
    void keepsPaginationControls(final String entity) {
        MatcherAssert.assertThat(
            "Collection fragments lost page navigation or page-size choices",
            XsClasspathTest.page(entity, "list-fragment", false),
            Matchers.allOf(
                Matchers.containsString("class=\"pagination-btn pagination-prev\""),
                Matchers.containsString("class=\"pagination-btn pagination-next\""),
                Matchers.containsString("class=\"pagination-btn active\""),
                Matchers.containsString("data-limit=\"15\""),
                Matchers.containsString("data-limit=\"30\""),
                Matchers.containsString("data-limit=\"50\""),
                Matchers.containsString("class=\"pagination-ellipsis\"")
            )
        );
    }

    @ParameterizedTest
    @CsvSource({
        "building, ASC, &uarr;", "building, DESC, &darr;", "building, NONE, ↕",
        "cabinet, ASC, &uarr;", "cabinet, DESC, &darr;", "cabinet, NONE, ↕"
    })
    void showsNameSorting(final String entity, final String direction, final String icon) {
        String active = "";
        if (!"NONE".equals(direction)) {
            active = " active";
        }
        for (final String view : List.of("list", "list-fragment")) {
            MatcherAssert.assertThat(
                new XsClasspath(String.format("xslt/%ss.xsl", entity), view).transform(
                    XsClasspathTest.document(entity, false).replace(
                        String.format("</%ss>", entity),
                        String.format(
                            """
                            <sorts><item><name>name</name><direction>%s</direction></item>
                            </sorts></%ss>
                            """, direction, entity
                        )
                    )
                ),
                Matchers.allOf(
                    Matchers.containsString("data-sort=\"name\""),
                    Matchers.containsString(
                        String.format("class=\"%ss-sort%s\"", entity, active)
                    ),
                    Matchers.containsString(
                        String.format("class=\"%ss-sort-icon\">%s</span>", entity, icon)
                    ),
                    Matchers.not(Matchers.containsString("data-sort=\"id\""))
                )
            );
        }
    }

    @ParameterizedTest
    @ValueSource(
        strings = {"federal-curriculums", "federal-curriculum", "federal-curriculum-requirements"}
    )
    void hidesFederalEditingControlsFromReaders(final String stylesheet) {
        MatcherAssert.assertThat(
            "A reader page exposed federal editing controls or lost the standard navigation",
            new XsClasspath(String.format("xslt/%s.xsl", stylesheet), "list").transform(
                """
                <surok><user><admin>false</admin></user>
                <curriculums><curriculum><id>17</id><title>Основной план</title>
                <requirements><requirement><id>23</id><grade>5</grade>
                <subject><id>1</id><name>Математика</name><origin>FEDERAL</origin></subject>
                <hours>5</hours>
                <partType>MANDATORY</partType></requirement></requirements>
                </curriculum></curriculums></surok>
                """
            ),
            Matchers.allOf(
                Matchers.containsString("Федеральный стандарт"),
                Matchers.containsString("href=\"/federal/curriculums\""),
                Matchers.containsString("href=\"/federal/subjects\""),
                Matchers.not(Matchers.containsString("href=\"/federal/curriculums/create\"")),
                Matchers.not(Matchers.containsString("href=\"/federal/curriculums/17/edit\"")),
                Matchers.not(Matchers.containsString("class=\"requirements-add\"")),
                Matchers.not(Matchers.containsString("class=\"requirement-action")),
                Matchers.not(Matchers.containsString("btn-delete-curriculum"))
            )
        );
    }

    @Test
    void offersCatalogueSubjectsAndExplicitAdditionalLimits() {
        MatcherAssert.assertThat(
            "Requirement forms accepted free text or omitted the additional hours limit",
            new XsClasspath("xslt/federal-curriculum-requirements.xsl", "list").transform(
                """
                <surok><user><admin>true</admin></user>
                <csrf><parameter>_csrf</parameter><token>защита</token></csrf>
                <subjects><subject><id>29</id><name>Физика</name></subject></subjects>
                <firstGrade>5</firstGrade><lastGrade>9</lastGrade>
                <curriculums><curriculum><id>17</id><title>План</title>
                <requirements/></curriculum></curriculums></surok>
                """
            ),
            Matchers.allOf(
                Matchers.containsString("<select id=\"subject-name\""),
                Matchers.containsString("<select id=\"filter-subject\""),
                Matchers.containsString("value=\"Физика\""),
                Matchers.containsString("/federal/curriculums/17/requirements"),
                Matchers.containsString("value=\"OPTIONAL\""),
                Matchers.containsString("name=\"hours\""),
                Matchers.containsString("name=\"selection\""),
                Matchers.containsString(">Дополнительные предметы</option>"),
                Matchers.not(Matchers.containsString("id=\"requirement-kind\"")),
                Matchers.not(Matchers.containsString("id=\"maximum-hours\"")),
                Matchers.containsString("name=\"_csrf\""),
                Matchers.containsString("min=\"0\""),
                Matchers.not(Matchers.containsString("<input id=\"subject-name\""))
            )
        );
    }

    @Test
    void identifiesAdditionalSubjectsOnTheLoadPage() throws Exception {
        MatcherAssert.assertThat(
            "The load page did not explain how additional hours are counted",
            new XsClasspath("xslt/class-loads.xsl", "list").transform(
                Files.readString(Path.of("src/test/resources/pages.xml"))
                    .replace("<origin>FEDERAL</origin>", "<origin>SCHOOL</origin>")
            ),
            Matchers.allOf(
                Matchers.containsString("class=\"load-additional\">Вариативная</span>"),
                Matchers.containsString("учитываются в вариативной нагрузке класса")
            )
        );
    }

    private static String page(final String entity, final String view, final boolean error) {
        return new XsClasspath(String.format("xslt/%ss.xsl", entity), view)
            .transform(XsClasspathTest.document(entity, error));
    }

    private static String document(final String entity, final boolean error) {
        String building = "";
        if ("cabinet".equals(entity)) {
            building = "<building><id>4</id><name>Учебный корпус</name></building>";
        }
        return String.format(
            """
            <surok>
                <school><id>3</id><name>Школа</name><schedules><schedule>
                <id>5</id><name>Первое полугодие</name></schedule></schedules></school>
                %2$s<error>%3$s</error>
                <csrf><parameter>_csrf</parameter><token>token</token></csrf>
                <%1$ss><%1$s><id>7</id><name>Южный зал</name></%1$s>
                <page><offset>5</offset><limit>15</limit><total>150</total>
                <totalPages>10</totalPages><hasNext>true</hasNext>
                <hasPrev>true</hasPrev></page></%1$ss>
            </surok>
            """, entity, building, error
        );
    }
}
