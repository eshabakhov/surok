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
                anonymous.path("/users/register").has("post")
            ),
            Matchers.contains(true, false, true)
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
