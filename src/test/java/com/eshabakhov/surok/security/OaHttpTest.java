/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Exercises the packaged specification without connecting to the database.
 * For example, the same filtered description is available as JSON and YAML.
 *
 * @since 0.0.1
 */
@SpringBootTest(
    properties = {
        "app.bootstrap-admin.enabled=false", "spring.liquibase.enabled=false",
        "app.security.docs.enabled=true", "spring.jooq.sql-dialect=POSTGRES",
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:1/unused",
        "spring.datasource.username=unused", "ya-id=unused", "ya-secret=unused"
    }
)
@AutoConfigureMockMvc
final class OaHttpTest {

    /**
     * Publishes the authored API information with the Maven version.
     *
     * @param mvc HTTP client
     * @param build Build information
     * @throws Exception If the description cannot be read
     */
    @Test
    void publishesPackagedDescriptionAndBuildVersion(
        @Autowired final MockMvc mvc, @Autowired final BuildProperties build
    ) throws Exception {
        final JsonNode info = Json.mapper().readTree(
            mvc.perform(MockMvcRequestBuilders.get("/api-docs"))
                .andReturn().getResponse().getContentAsByteArray()
        ).path("info");
        MatcherAssert.assertThat(
            "The API information lost the packaged description or the build version",
            List.of(info.path("description").asText(), info.path("version").asText()),
            Matchers.contains(
                "Manage schools, federal curricula, and schedules.", build.getVersion()
            )
        );
    }

    /**
     * Describes the same operations in both representations under a context path.
     *
     * @param mvc HTTP client
     * @throws Exception If the descriptions cannot be read
     */
    @Test
    void servesMatchingFormatsWithTheApplicationContextPath(@Autowired final MockMvc mvc)
        throws Exception {
        final String prefix = "/school-demo";
        final JsonNode json = Json.mapper().readTree(
            mvc.perform(
                MockMvcRequestBuilders.get(String.format("%s/api-docs", prefix)).contextPath(prefix)
            ).andReturn().getResponse().getContentAsByteArray()
        );
        MatcherAssert.assertThat(
            "The JSON and YAML descriptions differed or used the wrong context path",
            List.of(
                json.equals(
                    Yaml.mapper().readTree(
                        mvc.perform(
                            MockMvcRequestBuilders.get(String.format("%s/api-docs.yaml", prefix))
                                .contextPath(prefix)
                        ).andReturn().getResponse().getContentAsByteArray()
                    )
                ),
                prefix.equals(json.path("servers").path(0).path("url").asText())
            ),
            Matchers.contains(true, true)
        );
    }

    /**
     * Omits protected operations after serving an authorized description.
     *
     * @param mvc HTTP client
     * @throws Exception If the descriptions cannot be read
     */
    @Test
    void keepsAdministratorOperationsOutOfSubsequentAnonymousYaml(@Autowired final MockMvc mvc)
        throws Exception {
        final JsonNode allowed = Yaml.mapper().readTree(
            mvc.perform(
                MockMvcRequestBuilders.get("/api-docs.yaml").with(
                    SecurityMockMvcRequestPostProcessors.user("администратор").roles("ADMIN")
                )
            ).andReturn().getResponse().getContentAsByteArray()
        ).path("paths");
        final JsonNode anonymous = Yaml.mapper().readTree(
            mvc.perform(MockMvcRequestBuilders.get("/api-docs.yaml"))
                .andReturn().getResponse().getContentAsByteArray()
        ).path("paths");
        MatcherAssert.assertThat(
            "YAML omitted administrator operations or exposed them to an anonymous visitor",
            List.of(
                allowed.has("/permissions"), anonymous.has("/permissions"),
                anonymous.path("/users").has("post")
            ),
            Matchers.contains(true, false, true)
        );
    }

    /**
     * Groups account and school membership operations under one Swagger tag.
     *
     * @param mvc HTTP client
     * @throws Exception If the description cannot be read
     */
    @Test
    void publishesCanonicalUserOperationsTogether(@Autowired final MockMvc mvc)
        throws Exception {
        final JsonNode paths = Json.mapper().readTree(
            mvc.perform(
                MockMvcRequestBuilders.get("/api-docs").with(
                    SecurityMockMvcRequestPostProcessors.user("администратор").roles("ADMIN")
                )
            ).andReturn().getResponse().getContentAsByteArray()
        ).path("paths");
        MatcherAssert.assertThat(
            "Swagger retained user API aliases or split user operations across tags",
            List.of(
                paths.has("/users"),
                paths.has("/users/{user}/roles"),
                paths.has("/schools/{school}/users"),
                paths.has("/api/users"),
                paths.properties().stream().filter(
                    path -> path.getKey().startsWith("/users")
                        || path.getKey().contains("/users/")
                        || path.getKey().endsWith("/users")
                ).flatMap(path -> path.getValue().properties().stream()).allMatch(
                    operation -> operation.getValue().path("tags").size() == 1
                        && "Users".equals(operation.getValue().path("tags").path(0).asText())
                )
            ),
            Matchers.contains(true, true, true, false, true)
        );
    }

    /**
     * Exposes models for public operations and keeps Swagger authorization available.
     *
     * @param mvc HTTP client
     * @throws Exception If the description cannot be read
     */
    @Test
    void hidesUnusedModelsAndExamplesFromAnonymousUsers(@Autowired final MockMvc mvc)
        throws Exception {
        final JsonNode components = Json.mapper().readTree(
            mvc.perform(MockMvcRequestBuilders.get("/api-docs"))
                .andReturn().getResponse().getContentAsByteArray()
        ).path("components");
        MatcherAssert.assertThat(
            "Anonymous documentation exposed protected components or lost public components",
            List.of(
                components.path("schemas").has("RegistrationForm"),
                components.path("schemas").has("Permissions"),
                components.path("schemas").has("LoadDraft"),
                components.path("examples").has("CurriculumV2Json"),
                components.path("securitySchemes").has("Basic auth")
            ),
            Matchers.contains(true, false, false, false, true)
        );
    }

    /**
     * Retains referenced dependencies for a reader while omitting administrator models.
     *
     * @param mvc HTTP client
     * @throws Exception If the description cannot be read
     */
    @Test
    void keepsOnlyModelsReachableFromReaderOperations(@Autowired final MockMvc mvc)
        throws Exception {
        final JsonNode document = Yaml.mapper().readTree(
            mvc.perform(
                MockMvcRequestBuilders.get("/api-docs.yaml").with(
                    SecurityMockMvcRequestPostProcessors.user("читатель").roles("VIEWER")
                )
            ).andReturn().getResponse().getContentAsByteArray()
        );
        final JsonNode schemas = document.path("components").path("schemas");
        MatcherAssert.assertThat(
            "Reader documentation exposed administrator models or lost schema dependencies",
            List.of(
                schemas.has("CurriculumList"), schemas.has("Page"), schemas.has("Permissions"),
                document.findValuesAsText("$ref").stream().allMatch(
                    ref -> ref.startsWith("#/") && !document.at(ref.substring(1)).isMissingNode()
                )
            ),
            Matchers.contains(true, true, false, true)
        );
    }

    /**
     * Describes one creation operation with all accepted input formats.
     *
     * @param mvc HTTP client
     * @throws Exception If the description cannot be read
     */
    @Test
    void publishesOneSchoolCreationOperation(@Autowired final MockMvc mvc) throws Exception {
        final JsonNode paths = Json.mapper().readTree(
            mvc.perform(
                MockMvcRequestBuilders.get("/api-docs").with(
                    SecurityMockMvcRequestPostProcessors.user("администратор").roles("ADMIN")
                )
            ).andReturn().getResponse().getContentAsByteArray()
        ).path("paths");
        final JsonNode content = paths.path("/schools").path("post")
            .path("requestBody").path("content");
        MatcherAssert.assertThat(
            "Swagger exposed school creation aliases or lost a request format",
            List.of(
                content.has("application/json"),
                content.has("application/vnd.surok.school-v1+json"),
                content.has("application/x-www-form-urlencoded"),
                paths.path("/schools/create").has("get"),
                paths.path("/schools/create").has("post"),
                paths.path("/schools/").has("post")
            ),
            Matchers.contains(true, true, true, true, false, false)
        );
    }

    /**
     * Documents school updates at the resource and preserves the edit page.
     *
     * @param mvc HTTP client
     * @throws Exception If the description cannot be read
     */
    @Test
    void publishesOneSchoolUpdateOperation(@Autowired final MockMvc mvc) throws Exception {
        final JsonNode paths = Json.mapper().readTree(
            mvc.perform(
                MockMvcRequestBuilders.get("/api-docs").with(
                    SecurityMockMvcRequestPostProcessors.user("администратор").roles("ADMIN")
                )
            ).andReturn().getResponse().getContentAsByteArray()
        ).path("paths");
        final JsonNode content = paths.path("/schools/{school}").path("put")
            .path("requestBody").path("content");
        final JsonNode edit = paths.path("/schools/{school}/edit");
        MatcherAssert.assertThat(
            "Swagger exposed a separate form update or lost an update representation",
            List.of(
                content.has("application/json"),
                content.has("application/vnd.surok.school-v1+json"),
                edit.has("get"),
                edit.has("post")
            ),
            Matchers.contains(true, true, true, false)
        );
    }

    /**
     * Documents canonical collection operations and a separate HTML creation form.
     *
     * @param arguments Collection path segment and media type identifiers
     * @param mvc HTTP client
     * @throws Exception If the description cannot be read
     */
    @ParameterizedTest
    @CsvSource({
        "teachers, teacher, teachers",
        "schedules, schedule, schedules",
        "classes, school-class, school-classes",
        "buildings, building, buildings",
        "buildings/{building}/cabinets, cabinet, cabinets"
    })
    void publishesCanonicalSchoolCollectionOperations(
        final ArgumentsAccessor arguments, @Autowired final MockMvc mvc
    ) throws Exception {
        final String segment = arguments.getString(0);
        final String resource = arguments.getString(1);
        final String media = arguments.getString(2);
        final JsonNode paths = Json.mapper().readTree(
            mvc.perform(
                MockMvcRequestBuilders.get("/api-docs").with(
                    SecurityMockMvcRequestPostProcessors.user("администратор").roles("ADMIN")
                )
            ).andReturn().getResponse().getContentAsByteArray()
        ).path("paths");
        final String path = String.format("/schools/{school}/%s", segment);
        final JsonNode collection = paths.path(path);
        final JsonNode listing = collection.path("get").path("responses").path("200")
            .path("content");
        final JsonNode creation = collection.path("post").path("requestBody").path("content");
        final JsonNode form = paths.path(String.format("%s/create", path));
        final JsonNode content = form.path("get").path("responses").path("200").path("content");
        MatcherAssert.assertThat(
            "Swagger lost a collection representation or exposed a legacy alias",
            List.of(
                listing.has("text/html"),
                listing.has(String.format("text/vnd.surok.%s-fragment+html", media)),
                creation.has(String.format("application/vnd.surok.%s-v1+json", resource)),
                creation.has("application/x-www-form-urlencoded"),
                content.has("text/html") && content.size() == 1,
                form.has("post"),
                paths.has(String.format("%s/", path)),
                paths.has(String.format("%s/fragment", path))
            ),
            Matchers.contains(true, true, true, true, true, false, false, false)
        );
    }

    /**
     * Documents one update operation and keeps the separate edit page.
     *
     * @param arguments Collection segment, item placeholder, media identifier and conflict support
     * @param mvc HTTP client
     * @throws Exception If the description cannot be read
     */
    @ParameterizedTest
    @CsvSource({
        "teachers, teacher, teacher, true",
        "schedules, schedule, schedule, true",
        "classes, clazz, school-class, true",
        "buildings, building, building, false",
        "buildings/{building}/cabinets, cabinet, cabinet, false"
    })
    void publishesOneSchoolResourceUpdateOperation(
        final ArgumentsAccessor arguments, @Autowired final MockMvc mvc
    ) throws Exception {
        final String segment = arguments.getString(0);
        final String parameter = arguments.getString(1);
        final String resource = arguments.getString(2);
        final JsonNode paths = Json.mapper().readTree(
            mvc.perform(
                MockMvcRequestBuilders.get("/api-docs").with(
                    SecurityMockMvcRequestPostProcessors.user("администратор").roles("ADMIN")
                )
            ).andReturn().getResponse().getContentAsByteArray()
        ).path("paths");
        final String path = String.format("/schools/{school}/%s/{%s}", segment, parameter);
        final JsonNode update = paths.path(path).path("put");
        final JsonNode content = update.path("requestBody").path("content");
        final JsonNode edit = paths.path(String.format("%s/edit", path));
        MatcherAssert.assertThat(
            "Swagger exposed a legacy update or changed the JSON update contract",
            List.of(
                content.has(String.format("application/vnd.surok.%s-v1+json", resource)),
                update.path("responses").has("409"),
                edit.has("get"),
                edit.has("post")
            ),
            Matchers.contains(true, arguments.getBoolean(3), true, false)
        );
    }

    /**
     * Keeps subject forms in administrator documentation without duplicate mutations.
     *
     * @param mvc HTTP client
     * @throws Exception If the description cannot be read
     */
    @Test
    void publishesSubjectFormsForAdministrators(@Autowired final MockMvc mvc) throws Exception {
        final JsonNode paths = Json.mapper().readTree(
            mvc.perform(
                MockMvcRequestBuilders.get("/api-docs").with(
                    SecurityMockMvcRequestPostProcessors.user("администратор").roles("ADMIN")
                )
            ).andReturn().getResponse().getContentAsByteArray()
        ).path("paths");
        MatcherAssert.assertThat(
            "Swagger hid the subject forms from administrators or restored duplicate writes",
            List.of(
                paths.path("/federal/subjects/create").has("get"),
                paths.path("/federal/subjects/{subject}/edit").has("get"),
                paths.path("/schools/{school}/subjects/create").has("get"),
                paths.path("/schools/{school}/subjects/{subject}/edit").has("get"),
                paths.path("/federal/subjects/create").has("post"),
                paths.path("/schools/{school}/subjects/{subject}/edit").has("post")
            ),
            Matchers.contains(true, true, true, true, false, false)
        );
    }

    /**
     * Keeps federal editing forms out of reader documentation.
     *
     * @param mvc HTTP client
     * @throws Exception If the description cannot be read
     */
    @Test
    void hidesFederalSubjectFormsFromReaders(@Autowired final MockMvc mvc) throws Exception {
        final JsonNode paths = Json.mapper().readTree(
            mvc.perform(
                MockMvcRequestBuilders.get("/api-docs").with(
                    SecurityMockMvcRequestPostProcessors.user("читатель").roles("VIEWER")
                )
            ).andReturn().getResponse().getContentAsByteArray()
        ).path("paths");
        MatcherAssert.assertThat(
            "Swagger exposed federal subject editing forms to a reader",
            List.of(
                paths.has("/federal/subjects/create"),
                paths.has("/federal/subjects/{subject}/edit")
            ),
            Matchers.contains(false, false)
        );
    }

    /**
     * Publishes canonical curriculum and requirement operations for administrators.
     *
     * @param mvc HTTP client
     * @throws Exception If the description cannot be read
     */
    @Test
    void publishesCanonicalFederalStandardOperations(@Autowired final MockMvc mvc)
        throws Exception {
        final JsonNode paths = Json.mapper().readTree(
            mvc.perform(
                MockMvcRequestBuilders.get("/api-docs").with(
                    SecurityMockMvcRequestPostProcessors.user("администратор").roles("ADMIN")
                )
            ).andReturn().getResponse().getContentAsByteArray()
        ).path("paths");
        final JsonNode plans = paths.path("/federal/curriculums");
        final JsonNode requirements = paths.path("/federal/curriculums/{curriculum}/requirements");
        MatcherAssert.assertThat(
            "Swagger lost canonical federal operations or retained duplicate routes",
            List.of(
                plans.path("get").path("responses").path("200").path("content")
                    .has("text/vnd.surok.federal-curriculums-fragment+html"),
                plans.path("post").path("requestBody").path("content")
                    .has("application/vnd.surok.federal-curriculum-v2+json"),
                plans.path("post").path("requestBody").path("content")
                    .has("application/x-www-form-urlencoded"),
                paths.path("/federal/curriculums/{curriculum}").has("put"),
                paths.path("/federal/curriculums/create").has("get"),
                paths.path("/federal/curriculums/create").has("post"),
                paths.path("/federal/curriculums/{curriculum}/edit").has("post"),
                paths.has("/federal/curriculums/fragment"),
                requirements.path("get").path("responses").path("200").path("content")
                    .has("text/vnd.surok.federal-curriculum-requirements-fragment+html"),
                requirements.path("post").path("requestBody").path("content")
                    .has("application/x-www-form-urlencoded"),
                paths.path("/federal/curriculums/{curriculum}/requirements/{requirement}")
                    .has("post"),
                paths.has("/federal/curriculums/{curriculum}/requirements/fragment")
            ),
            Matchers.contains(
                true, true, true, true, true, false, false, false,
                true, true, false, false
            )
        );
    }

    /**
     * Keeps reading operations visible while hiding federal changes from readers.
     *
     * @param mvc HTTP client
     * @throws Exception If the description cannot be read
     */
    @Test
    void keepsFederalStandardReadOnlyForReaders(@Autowired final MockMvc mvc) throws Exception {
        final JsonNode paths = Json.mapper().readTree(
            mvc.perform(
                MockMvcRequestBuilders.get("/api-docs").with(
                    SecurityMockMvcRequestPostProcessors.user("читатель").roles("VIEWER")
                )
            ).andReturn().getResponse().getContentAsByteArray()
        ).path("paths");
        MatcherAssert.assertThat(
            "Reader documentation exposed federal forms or mutations",
            List.of(
                paths.path("/federal/curriculums").has("get"),
                paths.path("/federal/curriculums").has("post"),
                paths.path("/federal/curriculums/{curriculum}").has("put"),
                paths.has("/federal/curriculums/create"),
                paths.has("/federal/curriculums/{curriculum}/edit"),
                paths.path("/federal/curriculums/{curriculum}/requirements").has("get"),
                paths.path("/federal/curriculums/{curriculum}/requirements").has("post")
            ),
            Matchers.contains(true, false, false, false, false, true, false)
        );
    }

    /**
     * Documents the page and fragment as distinct media types of a single list operation.
     *
     * @param mvc HTTP client
     * @throws Exception If the description cannot be read
     */
    @Test
    void publishesOneSchoolListOperation(@Autowired final MockMvc mvc) throws Exception {
        final JsonNode paths = Json.mapper().readTree(
            mvc.perform(
                MockMvcRequestBuilders.get("/api-docs").with(
                    SecurityMockMvcRequestPostProcessors.user("администратор").roles("ADMIN")
                )
            ).andReturn().getResponse().getContentAsByteArray()
        ).path("paths");
        final JsonNode list = paths.path("/schools").path("get");
        final JsonNode content = list.path("responses").path("200").path("content");
        final JsonNode form = paths.path("/schools/create").path("get")
            .path("responses").path("200").path("content");
        MatcherAssert.assertThat(
            "Swagger listed a legacy fragment selector or omitted a school HTML representation",
            List.of(
                list.path("parameters").findValuesAsText("name").contains("fragment"),
                paths.path("/schools/").has("get"),
                paths.has("/schools/fragment"),
                form.has("text/html") && form.size() == 1,
                content.has("text/html"),
                content.path("text/vnd.surok.schools-fragment+html").path("examples").has("example")
            ),
            Matchers.contains(false, false, false, true, true, true)
        );
    }

    /**
     * Keeps Swagger pointed at the filtered description after generation is disabled.
     *
     * @param mvc HTTP client
     * @throws Exception If the UI configuration cannot be read
     */
    @Test
    void pointsSwaggerToTheFilteredDescription(@Autowired final MockMvc mvc) throws Exception {
        final String prefix = "/school-demo";
        MatcherAssert.assertThat(
            "Swagger did not use the filtered endpoint under the context path",
            Json.mapper().readTree(
                mvc.perform(
                    MockMvcRequestBuilders.get(String.format("%s/api-docs/swagger-config", prefix))
                        .contextPath(prefix)
                ).andReturn().getResponse().getContentAsByteArray()
            ).path("url").asText(),
            Matchers.is(String.format("%s/api-docs", prefix))
        );
    }

    /**
     * Prevents direct access to the complete document outside the filtered endpoints.
     *
     * @param mvc HTTP client
     * @throws Exception If the resource cannot be requested
     */
    @Test
    void keepsTheCanonicalResourcePrivate(@Autowired final MockMvc mvc) throws Exception {
        MatcherAssert.assertThat(
            "The complete specification was publicly served as a static resource",
            HttpStatusCode.valueOf(
                mvc.perform(
                    MockMvcRequestBuilders.get("/openapi.yml").with(
                        SecurityMockMvcRequestPostProcessors.user("администратор").roles("ADMIN")
                    )
                ).andReturn().getResponse().getStatus()
            ).is2xxSuccessful(),
            Matchers.is(false)
        );
    }
}
