/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load.autofill;

import com.eshabakhov.surok.user.AuthUser;
import com.eshabakhov.surok.user.UrPostgres;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.ArrayList;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * HTTP planning contracts against PostgreSQL and the real security filters.
 * For example, saving a draft leaves the actual teaching loads untouched.
 *
 * @since 0.0.1
 */
@Testcontainers
@DirtiesContext
@SpringBootTest(properties = "app.security.docs.enabled=true")
@AutoConfigureMockMvc
@Sql(
    statements = {
        "TRUNCATE TABLE public.user RESTART IDENTITY CASCADE",
        "TRUNCATE TABLE school, subject, federal_curriculum RESTART IDENTITY CASCADE",
        "TRUNCATE TABLE subject RESTART IDENTITY CASCADE",
        "INSERT INTO school (id, name) VALUES (1, 'Школа'), (2, 'Чужая школа')",
        """
        INSERT INTO schedule (id, school_id, name, is_deleted)
        VALUES (101, 1, 'Год', false), (103, 1, 'Полугодие', false),
            (105, 1, 'Удалённое расписание', true), (201, 2, 'Чужой год', false);
        """,
        """
        INSERT INTO school_class (id, school_id, grade, litera, is_deleted)
        VALUES (17, 1, 5, 'А', false), (19, 1, 6, 'Б', false),
            (21, 2, 5, 'Чужой', false), (23, 1, 5, 'Удалён', true);
        """,
        """
        INSERT INTO subject (id, school_id, origin, name, is_deleted)
        VALUES (31, 1, 'SCHOOL', 'Музыка', false),
        (33, 2, 'SCHOOL', 'Чужой предмет', false),
        (35, 1, 'SCHOOL', 'Удалённый предмет', true);
        """,
        "INSERT INTO subject (id, school_id, origin, name) VALUES (29, NULL, 'FEDERAL', 'Математика')",
        """
        INSERT INTO federal_curriculum
            (id, title, education_level, study_week_type, version, academic_year, is_deleted)
        VALUES (61, 'Основной план', 'BASIC', 'FIVE_DAYS', '1', '2026/2027', false),
            (67, 'Другой план', 'BASIC', 'FIVE_DAYS', '2', '2026/2027', false),
            (69, 'Удалённый план', 'BASIC', 'FIVE_DAYS', '1', '2026/2027', true);
        """,
        """
        INSERT INTO federal_curriculum_requirement
        (id, federal_curriculum_id, grade, subject_id, hours, part_type)
        VALUES (71, 61, 5, (SELECT id FROM subject WHERE name = 'Математика'
        AND NOT is_deleted), 3, 'MANDATORY'),
        (75, 61, 6, (SELECT id FROM subject WHERE name = 'Математика'
        AND NOT is_deleted), 7, 'MANDATORY'),
        (77, 67, 5, (SELECT id FROM subject WHERE name = 'Математика'
        AND NOT is_deleted), 4, 'MANDATORY');
        """,
        """
        INSERT INTO class_curriculum (schedule_id, school_class_id, subject_id, hours)
        VALUES (101, 17, 29, 5);
        """,
        "INSERT INTO load_draft (schedule_id, curriculum_id) VALUES (101, 61)",
        """
        INSERT INTO federal_curriculum_requirement
        (federal_curriculum_id, grade, hours, part_type)
        VALUES (61, 5, 3, 'OPTIONAL'), (61, 6, 2, 'OPTIONAL'), (67, 5, 2, 'OPTIONAL'), (67, 6, 2,
        'OPTIONAL');
        """,
        """
        INSERT INTO public.user (id, username, password, email, corporate)
        VALUES (41, 'директор', 'secret', 'director@example.org', true),
            (43, 'читатель', 'secret', 'viewer@example.org', true),
            (47, 'администратор', 'secret', 'admin@example.org', true),
            (49, 'создатель', 'secret', 'maker@example.org', true);
        """,
        """
        INSERT INTO school_user (school_id, user_id) VALUES (1, 41), (1, 43), (1, 49);
        """,
        """
        INSERT INTO school_user_role (school_user_id, role_id)
        SELECT school_user.id, role.id FROM school_user, role
        WHERE (school_user.user_id = 41 AND role.name = 'DIRECTOR')
            OR (school_user.user_id = 43 AND role.name = 'VIEWER');
        """,
        """
        INSERT INTO user_role (user_id, role_id)
        SELECT 47, id FROM role WHERE name = 'ADMIN'
        UNION ALL SELECT 49, id FROM role WHERE name = 'BASIC_MAKER';
        """
    }
)
final class LdHttpTest {

    /** Isolated database using the application migrations. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:14-alpine");

    @ParameterizedTest
    @ValueSource(
        strings = {
            "get::200:saved", "get:/fragment:200:saved", "put::200:saved",
            "put::201:saved", "put:/curriculum:200:switched", "post:/application:200:applied"
        }
    )
    void documentsDraftExamplesInBothFormats(
        final String route, @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        final String[] parts = route.split(":", 4);
        final JsonNode spec = new ObjectMapper().readTree(
            mvc.perform(
                MockMvcRequestBuilders.get("/api-docs").with(
                    SecurityMockMvcRequestPostProcessors.user(
                        new AuthUser(new UrPostgres(ctx, 47L))
                    )
                ).accept("application/json")
            ).andReturn().getResponse().getContentAsString()
        );
        final JsonNode content = spec.path("paths").path(
            String.format("/schools/{school}/schedules/{schedule}/loads/draft%s", parts[1])
        ).path(parts[0]).path("responses").path(parts[2]).path("content");
        final JsonNode document = content.path("application/vnd.surok.load-draft.v1+json")
            .path("examples").path(parts[3]).path("value");
        MatcherAssert.assertThat(
            "OpenAPI omitted draft examples or documented obsolete fields",
            List.of(
                document.path("loads").path(0).path("schoolClass").path("name").asText(),
                document.path("loads").path(0).path("subject").path("name").asText(),
                Boolean.toString(
                    document.has("unavailable") && !document.has("missing")
                        && !document.has("scheduleId") && !document.has("classes")
                ),
                content.path("application/vnd.surok.load-draft.v1+xml")
                    .path("schema").path("$ref").asText(),
                spec.path("components").path("schemas").path("LoadDraftXml")
                    .path("xml").path("name").asText(),
                content.path("application/vnd.surok.load-draft.v1+xml")
                    .path("examples").path(parts[3]).path("value").asText()
            ),
            Matchers.contains(
                Matchers.is("5А"), Matchers.is("Математика"), Matchers.is("true"),
                Matchers.is("#/components/schemas/LoadDraftXml"), Matchers.is("draft"),
                Matchers.allOf(
                    Matchers.containsString("<draft>"),
                    Matchers.containsString("<schoolClass>"),
                    Matchers.containsString("<unavailable></unavailable>")
                )
            )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/curriculum"})
    void documentsNestedReferencesInRequestExamples(
        final String suffix, @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        final JsonNode examples = new ObjectMapper().readTree(
            mvc.perform(
                MockMvcRequestBuilders.get("/api-docs").with(
                    SecurityMockMvcRequestPostProcessors.user(
                        new AuthUser(new UrPostgres(ctx, 47L))
                    )
                ).accept("application/json")
            ).andReturn().getResponse().getContentAsString()
        ).path("paths").path(
            String.format("/schools/{school}/schedules/{schedule}/loads/draft%s", suffix)
        ).path("put").path("requestBody").path("content")
            .path("application/vnd.surok.load-draft.v1+json").path("examples");
        final List<JsonNode> actual = new ArrayList<>(2);
        examples.forEach(example -> actual.add(example.path("value")));
        final List<JsonNode> expected;
        if (suffix.isEmpty()) {
            expected = List.of(
                new ObjectMapper().readTree(
                    """
                    {"curriculum":{"id":61},"additions":[
                        {"grade":5,"subjects":[{"subject":{"id":31},"hours":2}]}]}
                    """
                ),
                new ObjectMapper().readTree("{\"curriculum\":{\"id\":61},\"additions\":[]}")
            );
        } else {
            expected = List.of(new ObjectMapper().readTree("{\"curriculum\":{\"id\":67}}"));
        }
        MatcherAssert.assertThat(
            "OpenAPI lost usable examples for saving or switching the draft",
            actual,
            Matchers.is(expected)
        );
    }

    @ParameterizedTest
    @ValueSource(
        strings = {"get:", "get:/fragment", "put:", "put:/curriculum", "post:/application"}
    )
    void documentsBothIdentifiersAsRequiredPathParameters(
        final String route, @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        final JsonNode operation = new ObjectMapper().readTree(
            mvc.perform(
                MockMvcRequestBuilders.get("/api-docs").with(
                    SecurityMockMvcRequestPostProcessors.user(
                        new AuthUser(new UrPostgres(ctx, 47L))
                    )
                ).accept("application/json")
            ).andReturn().getResponse().getContentAsString()
        ).path("paths").path(
            String.format(
                "/schools/{school}/schedules/{schedule}/loads/draft%s", route.split(":", 2)[1]
            )
        ).path(route.split(":", 2)[0]);
        final List<List<String>> parameters = new ArrayList<>(2);
        for (final JsonNode parameter : operation.path("parameters")) {
            parameters.add(
                List.of(
                    parameter.path("name").asText(), parameter.path("in").asText(),
                    parameter.path("required").asText(),
                    parameter.path("schema").path("type").asText(),
                    parameter.path("schema").path("format").asText()
                )
            );
        }
        MatcherAssert.assertThat(
            "OpenAPI lost the required numeric school and schedule path parameters",
            parameters,
            Matchers.containsInAnyOrder(
                List.of("school", "path", "true", "integer", "int64"),
                List.of("schedule", "path", "true", "integer", "int64")
            )
        );
    }

    @ParameterizedTest
    @ValueSource(
        strings = {"get:", "get:/fragment", "put:", "put:/curriculum", "post:/application"}
    )
    void documentsTheAvailableResponseFormats(
        final String route, @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        final List<String> formats = new ArrayList<>(3);
        new ObjectMapper().readTree(
            mvc.perform(
                MockMvcRequestBuilders.get("/api-docs").with(
                    SecurityMockMvcRequestPostProcessors.user(
                        new AuthUser(new UrPostgres(ctx, 47L))
                    )
                ).accept("application/json")
            ).andReturn().getResponse().getContentAsString()
        ).path("paths").path(
            String.format(
                "/schools/{school}/schedules/{schedule}/loads/draft%s", route.split(":", 2)[1]
            )
        ).path(route.split(":", 2)[0]).path("responses").path("200").path("content")
            .fieldNames().forEachRemaining(formats::add);
        final List<String> expected = new ArrayList<>(
            List.of(
                "application/vnd.surok.load-draft.v1+json",
                "application/vnd.surok.load-draft.v1+xml"
            )
        );
        if (route.startsWith("get:")) {
            expected.add("text/html");
        }
        MatcherAssert.assertThat(
            "OpenAPI did not expose the supported response media types for selection",
            formats, Matchers.containsInAnyOrder(expected.toArray(String[]::new))
        );
    }

    @Test
    void savesWithoutClassSelectionsOrSubjectMappings(
        @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        MatcherAssert.assertThat(
            "The draft still required manual selections or matching decisions",
            mvc.perform(
                MockMvcRequestBuilders.put("/schools/1/schedules/101/loads/draft").with(
                    SecurityMockMvcRequestPostProcessors.user(
                        new AuthUser(new UrPostgres(ctx, 41L))
                    )
                ).with(SecurityMockMvcRequestPostProcessors.csrf())
                    .contentType("application/vnd.surok.load-draft.v1+json")
                    .accept("application/vnd.surok.load-draft.v1+json")
                    .content("{\"curriculum\":{\"id\":61},\"additions\":[]}")
            ).andReturn().getResponse().getStatus(),
            Matchers.is(200)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"json", "xml"})
    void readsTheSavedDraft(
        final String format, @Autowired final MockMvc mvc,
        @Autowired final DSLContext ctx
    ) throws Exception {
        final MockHttpServletResponse response = mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/schedules/101/loads/draft").with(
                SecurityMockMvcRequestPostProcessors.user(new AuthUser(new UrPostgres(ctx, 41L)))
            ).accept(String.format("application/vnd.surok.load-draft.v1+%s", format))
        ).andReturn().getResponse();
        MatcherAssert.assertThat(
            "The saved draft was not returned in the negotiated representation",
            List.of(
                Integer.toString(response.getStatus()), response.getContentType(),
                response.getContentAsString()
            ),
            Matchers.contains(
                Matchers.is("200"),
                Matchers.containsString(
                    String.format("application/vnd.surok.load-draft.v1+%s", format)
                ),
                Matchers.allOf(
                    Matchers.containsString("proposed"), Matchers.containsString("3"),
                    Matchers.containsString("unavailable"),
                    Matchers.not(Matchers.containsString("missing")),
                    Matchers.containsString("schoolClass"),
                    Matchers.not(Matchers.containsString("schoolClassId")),
                    Matchers.not(Matchers.containsString("subjectId")),
                    Matchers.not(Matchers.containsString("scheduleId"))
                )
            )
        );
    }

    @Test
    void embedsClassesAndSubjectsInsideLoads(
        @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        final JsonNode draft = new ObjectMapper().readTree(
            mvc.perform(
                MockMvcRequestBuilders.get("/schools/1/schedules/101/loads/draft").with(
                    SecurityMockMvcRequestPostProcessors.user(
                        new AuthUser(new UrPostgres(ctx, 41L))
                    )
                ).accept("application/vnd.surok.load-draft.v1+json")
            ).andReturn().getResponse().getContentAsString()
        );
        MatcherAssert.assertThat(
            "The draft duplicated its classes or omitted the nested class and subject details",
            List.of(
                draft.has("classes"), draft.path("loads").path(0).path("schoolClass"),
                draft.path("loads").path(0).path("subject")
            ),
            Matchers.contains(
                false,
                JsonNodeFactory.instance.objectNode().put("id", 17).put("name", "5А")
                    .put("grade", 5).put("litera", "А"),
                JsonNodeFactory.instance.objectNode().put("id", 29).put("name", "Математика")
                    .put("origin", "FEDERAL")
            )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/curriculum", "/application"})
    void returnsXmlAfterChangingTheDraft(
        final String suffix, @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        HttpMethod method = HttpMethod.PUT;
        if ("/application".equals(suffix)) {
            method = HttpMethod.POST;
        }
        final MockHttpServletResponse response = mvc.perform(
            MockMvcRequestBuilders.request(
                method, String.format("/schools/1/schedules/101/loads/draft%s", suffix)
            ).with(
                SecurityMockMvcRequestPostProcessors.user(new AuthUser(new UrPostgres(ctx, 41L)))
            ).with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType("application/vnd.surok.load-draft.v1+json")
                .accept("application/vnd.surok.load-draft.v1+xml")
                .content("{\"curriculum\":{\"id\":61},\"additions\":[]}")
        ).andReturn().getResponse();
        MatcherAssert.assertThat(
            "Changing the draft failed to return the requested XML representation",
            List.of(
                Integer.toString(response.getStatus()), response.getContentType(),
                response.getContentAsString()
            ),
            Matchers.contains(
                Matchers.is("200"),
                Matchers.containsString("application/vnd.surok.load-draft.v1+xml"),
                Matchers.containsString("<draft>")
            )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"json", "xml"})
    void identifiesTheCreatedDraftInEitherRepresentation(
        final String format, @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        final MockHttpServletResponse response = mvc.perform(
            MockMvcRequestBuilders.put("/schools/1/schedules/103/loads/draft").with(
                SecurityMockMvcRequestPostProcessors.user(new AuthUser(new UrPostgres(ctx, 41L)))
            ).with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType("application/vnd.surok.load-draft.v1+json")
                .accept(String.format("application/vnd.surok.load-draft.v1+%s", format))
                .content("{\"curriculum\":{\"id\":61},\"additions\":[]}")
        ).andReturn().getResponse();
        MatcherAssert.assertThat(
            "The created draft lost its status, location or requested representation",
            List.of(
                Integer.toString(response.getStatus()), response.getHeader("Location"),
                response.getContentType()
            ),
            Matchers.contains(
                Matchers.is("201"),
                Matchers.is("/schools/1/schedules/103/loads/draft"),
                Matchers.containsString(
                    String.format("application/vnd.surok.load-draft.v1+%s", format)
                )
            )
        );
    }

    @Test
    void savesAdditionsWithoutApplyingThem(
        @Autowired final MockMvc mvc,
        @Autowired final DSLContext ctx
    ) throws Exception {
        MatcherAssert.assertThat(
            "Saving the draft lost its decision or changed actual teaching hours",
            List.of(
                mvc.perform(
                    MockMvcRequestBuilders.put("/schools/1/schedules/101/loads/draft").with(
                        SecurityMockMvcRequestPostProcessors.user(
                            new AuthUser(new UrPostgres(ctx, 41L))
                        )
                    ).with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType("application/vnd.surok.load-draft.v1+json").content(
                            """
                            {"curriculum":{"id":61},"additions":[{"grade":5,
                                "subjects":[{"subject":{"id":31},"hours":2}]}]}
                            """
                        )
                ).andReturn().getResponse().getStatus(),
                ctx.fetchSingle("SELECT hours FROM load_draft_addition").get(0),
                ctx.fetchSingle("SELECT hours FROM class_curriculum").get(0)
            ),
            Matchers.contains(200, 2, 5)
        );
    }

    @Test
    void switchesTheCurriculumWithoutApplyingIt(
        @Autowired final MockMvc mvc,
        @Autowired final DSLContext ctx
    ) throws Exception {
        MatcherAssert.assertThat(
            "Switching the curriculum failed or changed actual hours",
            List.of(
                mvc.perform(
                    MockMvcRequestBuilders.put(
                        "/schools/1/schedules/101/loads/draft/curriculum"
                    ).with(
                        SecurityMockMvcRequestPostProcessors.user(
                            new AuthUser(new UrPostgres(ctx, 41L))
                        )
                    ).with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType("application/vnd.surok.load-draft.v1+json")
                        .content("{\"curriculum\":{\"id\":67}}")
                ).andReturn().getResponse().getStatus(),
                ctx.fetchSingle("SELECT curriculum_id FROM load_draft").get(0),
                ctx.fetchSingle("SELECT hours FROM class_curriculum").get(0)
            ),
            Matchers.contains(200, 67L, 5)
        );
    }

    @Test
    void appliesMandatoryHoursIdempotently(
        @Autowired final MockMvc mvc,
        @Autowired final DSLContext ctx
    ) throws Exception {
        for (int attempt = 0; attempt < 2; ++attempt) {
            mvc.perform(
                MockMvcRequestBuilders.post(
                    "/schools/1/schedules/101/loads/draft/application"
                ).with(
                    SecurityMockMvcRequestPostProcessors.user(
                        new AuthUser(new UrPostgres(ctx, 41L))
                    )
                ).with(SecurityMockMvcRequestPostProcessors.csrf())
            ).andReturn();
        }
        MatcherAssert.assertThat(
            "Repeated application duplicated the load or ignored the saved decision",
            ctx.fetch(
                "SELECT hours FROM class_curriculum ORDER BY school_class_id"
            ).getValues(0),
            Matchers.contains(3, 7)
        );
    }

    @ParameterizedTest
    @ValueSource(longs = {103L, 105L, 201L, 999L})
    void rejectsUnavailableDrafts(
        final long schedule, @Autowired final MockMvc mvc,
        @Autowired final DSLContext ctx
    ) throws Exception {
        MatcherAssert.assertThat(
            "An unavailable or foreign schedule exposed a planning draft",
            mvc.perform(
                MockMvcRequestBuilders.get(
                    String.format("/schools/1/schedules/%d/loads/draft", schedule)
                ).with(
                    SecurityMockMvcRequestPostProcessors.user(
                        new AuthUser(new UrPostgres(ctx, 41L))
                    )
                )
            ).andReturn().getResponse().getStatus(),
            Matchers.is(404)
        );
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "application/json", "application/xml", "text/html", "invalid",
            "*/*;q=0", "application/vnd.surok.load-draft.v2+json"
        }
    )
    void checksAcceptBeforeEveryMutation(
        final String accept, @Autowired final MockMvc mvc,
        @Autowired final DSLContext ctx
    ) throws Exception {
        final List<Integer> statuses = new ArrayList<>(3);
        for (final String suffix : List.of("", "/curriculum")) {
            statuses.add(
                mvc.perform(
                    MockMvcRequestBuilders.put(
                        String.format("/schools/1/schedules/101/loads/draft%s", suffix)
                    ).with(
                        SecurityMockMvcRequestPostProcessors.user(
                            new AuthUser(new UrPostgres(ctx, 41L))
                        )
                    ).with(SecurityMockMvcRequestPostProcessors.csrf())
                        .header("Accept", accept)
                        .contentType("application/vnd.surok.load-draft.v1+json").content(
                            """
                            {"curriculum":{"id":67},"additions":[]}
                            """
                        )
                ).andReturn().getResponse().getStatus()
            );
        }
        statuses.add(
            mvc.perform(
                MockMvcRequestBuilders.post(
                    "/schools/1/schedules/101/loads/draft/application"
                ).with(
                    SecurityMockMvcRequestPostProcessors.user(
                        new AuthUser(new UrPostgres(ctx, 41L))
                    )
                ).with(SecurityMockMvcRequestPostProcessors.csrf())
                    .header("Accept", accept)
            ).andReturn().getResponse().getStatus()
        );
        MatcherAssert.assertThat(
            "Negotiation failed to reject a mutation before writing its draft",
            List.of(
                statuses, ctx.fetchSingle("SELECT curriculum_id FROM load_draft").get(0),
                ctx.fetchSingle("SELECT hours FROM class_curriculum").get(0)
            ),
            Matchers.contains(List.of(406, 406, 406), 61L, 5)
        );
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "application/json", "application/xml", "application/vnd.surok.load-draft.v2+json"
        }
    )
    void rejectsUnversionedOrUnknownInput(
        final String type, @Autowired final MockMvc mvc,
        @Autowired final DSLContext ctx
    ) throws Exception {
        MatcherAssert.assertThat(
            "An unsupported input media type was accepted",
            mvc.perform(
                MockMvcRequestBuilders.put("/schools/1/schedules/101/loads/draft").with(
                    SecurityMockMvcRequestPostProcessors.user(
                        new AuthUser(new UrPostgres(ctx, 41L))
                    )
                ).with(SecurityMockMvcRequestPostProcessors.csrf())
                    .contentType(type).content("{}")
            ).andReturn().getResponse().getStatus(),
            Matchers.is(415)
        );
    }

    @ParameterizedTest
    @ValueSource(longs = {21L, 23L, 999L})
    void rollsBackInvalidSelections(
        final long clazz, @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        MatcherAssert.assertThat(
            "An invalid selection overwrote the saved curriculum or classes",
            List.of(
                mvc.perform(
                    MockMvcRequestBuilders.put("/schools/1/schedules/101/loads/draft").with(
                        SecurityMockMvcRequestPostProcessors.user(
                            new AuthUser(new UrPostgres(ctx, 41L))
                        )
                    ).with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType("application/vnd.surok.load-draft.v1+json").content(
                            String.format(
                                """
                                {"curriculum":{"id":67},"classIds":[%d],"optionalSubjects":[],
                                "mappings":[],"changes":[]}
                                """, clazz
                            )
                        )
                ).andReturn().getResponse().getStatus(),
                ctx.fetchSingle("SELECT curriculum_id FROM load_draft").get(0)
            ),
            Matchers.contains(400, 61L)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/curriculum", "/application"})
    void forbidsMutationsForViewers(
        final String suffix, @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        final String path = String.format("/schools/1/schedules/101/loads/draft%s", suffix);
        MatcherAssert.assertThat(
            "A viewer was allowed to mutate the draft or its actual loads",
            mvc.perform(
                (
                    switch (suffix) {
                        case "/application" -> MockMvcRequestBuilders.post(path);
                        case null, default -> MockMvcRequestBuilders.put(path);
                    }
                ).with(
                    SecurityMockMvcRequestPostProcessors.user(
                        new AuthUser(new UrPostgres(ctx, 43L))
                    )
                ).with(SecurityMockMvcRequestPostProcessors.csrf())
                    .contentType("application/vnd.surok.load-draft.v1+json").content("{}")
            ).andReturn().getResponse().getStatus(),
            Matchers.is(403)
        );
    }

    @Test
    void requiresCsrfProtection(@Autowired final MockMvc mvc, @Autowired final DSLContext ctx)
        throws Exception {
        MatcherAssert.assertThat(
            "Draft application bypassed CSRF protection",
            mvc.perform(
                MockMvcRequestBuilders.post(
                    "/schools/1/schedules/101/loads/draft/application"
                ).with(
                    SecurityMockMvcRequestPostProcessors.user(
                        new AuthUser(new UrPostgres(ctx, 41L))
                    )
                )
            ).andReturn().getResponse().getStatus(),
            Matchers.is(403)
        );
    }

    @Test
    void forbidsAccessToAnotherSchool(
        @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        MatcherAssert.assertThat(
            "A user was allowed to access another school's draft",
            mvc.perform(
                MockMvcRequestBuilders.get("/schools/2/schedules/201/loads/draft").with(
                    SecurityMockMvcRequestPostProcessors.user(
                        new AuthUser(new UrPostgres(ctx, 41L))
                    )
                )
            ).andReturn().getResponse().getStatus(),
            Matchers.is(403)
        );
    }

    @Test
    void deniesUsersWithoutAnAssignedSchool(
        @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        ctx.execute("DELETE FROM school_user WHERE user_id = 41");
        MatcherAssert.assertThat(
            "A user without a school did not receive an access denial",
            mvc.perform(
                MockMvcRequestBuilders.get("/schools/1/schedules/101/loads/draft").with(
                    SecurityMockMvcRequestPostProcessors.user(
                        new AuthUser(new UrPostgres(ctx, 41L))
                    )
                )
            ).andReturn().getResponse().getStatus(),
            Matchers.is(403)
        );
    }

    @Test
    void permitsAdministratorsFromAnotherSchool(
        @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        MatcherAssert.assertThat(
            "An administrator was denied access to another school's draft",
            mvc.perform(
                MockMvcRequestBuilders.get("/schools/1/schedules/101/loads/draft").with(
                    SecurityMockMvcRequestPostProcessors.user(
                        new AuthUser(new UrPostgres(ctx, 47L))
                    )
                )
            ).andReturn().getResponse().getStatus(),
            Matchers.is(200)
        );
    }

    @Test
    void createsTheFirstDraftAtItsCanonicalAddress(
        @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        final MockHttpServletResponse response = mvc.perform(
            MockMvcRequestBuilders.put("/schools/1/schedules/103/loads/draft").with(
                SecurityMockMvcRequestPostProcessors.user(new AuthUser(new UrPostgres(ctx, 41L)))
            ).with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType("application/vnd.surok.load-draft.v1+json").content(
                    """
                    {"curriculum":{"id":61},"additions":[]}
                    """
                )
        ).andReturn().getResponse();
        MatcherAssert.assertThat(
            "Creating the first draft did not identify its canonical resource",
            List.of(response.getStatus(), response.getHeader("Location")),
            Matchers.contains(201, "/schools/1/schedules/103/loads/draft")
        );
    }

    @Test
    void rendersAnEmptyEditorWithoutCreatingADraft(
        @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        MatcherAssert.assertThat(
            "Opening the editor created a draft or omitted its available selections",
            List.of(
                mvc.perform(
                    MockMvcRequestBuilders.get("/schools/1/schedules/103/loads/draft").with(
                        SecurityMockMvcRequestPostProcessors.user(
                            new AuthUser(new UrPostgres(ctx, 41L))
                        )
                    ).accept("text/html")
                ).andReturn().getResponse().getContentAsString(),
                ctx.fetchSingle("SELECT count(*) FROM load_draft").get(0).toString()
            ),
            Matchers.contains(
                Matchers.allOf(
                    Matchers.containsString("Основной план"),
                    Matchers.containsString("Другой план"),
                    Matchers.containsString("Математика"),
                    Matchers.containsString("data-saved=\"false\""),
                    Matchers.not(Matchers.containsString("Удалённый план")),
                    Matchers.not(Matchers.containsString("Чужой предмет"))
                ),
                Matchers.is("1")
            )
        );
    }

    @Test
    void restoresTheSavedEditorAsAFragment(
        @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        MatcherAssert.assertThat(
            "The editor fragment lost its saved correspondence or initial hours",
            mvc.perform(
                MockMvcRequestBuilders.get("/schools/1/schedules/101/loads/draft/fragment").with(
                    SecurityMockMvcRequestPostProcessors.user(
                        new AuthUser(new UrPostgres(ctx, 41L))
                    )
                ).accept("text/html")
            ).andReturn().getResponse().getContentAsString(),
            Matchers.allOf(
                Matchers.containsString("data-current=\"5\""),
                Matchers.containsString("data-proposed=\"3\""),
                Matchers.containsString("id=\"draft-save\""),
                Matchers.containsString("id=\"draft-apply\""),
                Matchers.containsString("Вариативная нагрузка по параллелям"),
                Matchers.not(Matchers.containsString("data-mapping=")),
                Matchers.not(Matchers.containsString("data-decision=")),
                Matchers.not(Matchers.containsString("name=\"classIds\"")),
                Matchers.not(Matchers.containsString("<html"))
            )
        );
    }

    @Test
    void rendersReadOnlyPlanningForViewers(
        @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        MatcherAssert.assertThat(
            "A viewer received controls for saving or applying teaching loads",
            mvc.perform(
                MockMvcRequestBuilders.get("/schools/1/schedules/101/loads/draft").with(
                    SecurityMockMvcRequestPostProcessors.user(
                        new AuthUser(new UrPostgres(ctx, 43L))
                    )
                ).accept("text/html")
            ).andReturn().getResponse().getContentAsString(),
            Matchers.allOf(
                Matchers.containsString("Учебная нагрузка"),
                Matchers.containsString("data-editable=\"false\""),
                Matchers.not(Matchers.containsString("id=\"draft-save\"")),
                Matchers.not(Matchers.containsString("id=\"draft-apply\""))
            )
        );
    }

    @Test
    void rendersReadOnlyPlanningForMakersWithoutSchoolRole(
        @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        MatcherAssert.assertThat(
            "A maker without a school role received draft editing controls",
            mvc.perform(
                MockMvcRequestBuilders.get("/schools/1/schedules/101/loads/draft").with(
                    SecurityMockMvcRequestPostProcessors.user(
                        new AuthUser(new UrPostgres(ctx, 49L))
                    )
                ).accept("text/html")
            ).andReturn().getResponse().getContentAsString(),
            Matchers.allOf(
                Matchers.containsString("data-editable=\"false\""),
                Matchers.not(Matchers.containsString("id=\"draft-save\"")),
                Matchers.not(Matchers.containsString("id=\"draft-apply\""))
            )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"PUT,", "PUT,/curriculum", "POST,/application"})
    void deniesDraftChangesForMakersWithoutSchoolRole(
        final String route, @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        final String[] parts = route.split(",", 2);
        MatcherAssert.assertThat(
            "A maker without a school role changed the draft",
            mvc.perform(
                MockMvcRequestBuilders.request(
                    HttpMethod.valueOf(parts[0]),
                    String.format("/schools/1/schedules/101/loads/draft%s", parts[1])
                ).with(
                    SecurityMockMvcRequestPostProcessors.user(
                        new AuthUser(new UrPostgres(ctx, 49L))
                    )
                ).with(SecurityMockMvcRequestPostProcessors.csrf())
                    .accept("application/vnd.surok.load-draft.v1+json")
                    .contentType("application/vnd.surok.load-draft.v1+json")
                    .content("{\"curriculum\":{\"id\":61},\"additions\":[]}")
            ).andReturn().getResponse().getStatus(),
            Matchers.is(403)
        );
    }

    @Test
    void displaysIndividuallyAssignedAdditionalHours(
        @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        ctx.execute(
            """
            INSERT INTO class_curriculum (schedule_id, school_class_id, subject_id, hours)
            VALUES (101, 17, 31, 2)
            """
        );
        MatcherAssert.assertThat(
            "The draft hid individually assigned additional hours or their allowance usage",
            mvc.perform(
                MockMvcRequestBuilders.get("/schools/1/schedules/101/loads/draft").with(
                    SecurityMockMvcRequestPostProcessors.user(
                        new AuthUser(new UrPostgres(ctx, 41L))
                    )
                ).accept("text/html")
            ).andReturn().getResponse().getContentAsString(),
            Matchers.allOf(
                Matchers.containsString(
                    "Назначена отдельно классу; учитывается в вариативном лимите"
                ),
                Matchers.containsString("сейчас 2 ч., после применения 2 ч."),
                Matchers.containsString("5А"), Matchers.containsString("6Б")
            )
        );
    }

    @Test
    void showsUnavailableFederalSubjectsInTheDraft(
        @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        ctx.execute("UPDATE subject SET is_deleted = true WHERE id = 29");
        MatcherAssert.assertThat(
            "The draft hid unavailable mandatory federal subjects",
            mvc.perform(
                MockMvcRequestBuilders.get("/schools/1/schedules/101/loads/draft").with(
                SecurityMockMvcRequestPostProcessors.user(new AuthUser(new UrPostgres(ctx, 41L)))
            ).accept("text/html")
            ).andReturn().getResponse().getContentAsString(),
            Matchers.allOf(
                Matchers.containsString(
                    "Некоторые обязательные предметы недоступны в федеральном справочнике"
                ),
                Matchers.containsString("Математика"),
                Matchers.containsString("Будет заполнена доступная часть нагрузки")
            )
        );
    }

    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.liquibase.enabled", () -> true);
        registry.add("spring.datasource.url", LdHttpTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", LdHttpTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", LdHttpTest.POSTGRES::getPassword);
    }
}
