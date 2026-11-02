/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum.requirement;

import com.eshabakhov.surok.Filter;
import com.eshabakhov.surok.Sort;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.filter.FiltersRequest;
import com.eshabakhov.surok.page.PageRequest;
import com.eshabakhov.surok.sort.SortsRequest;
import com.eshabakhov.surok.user.Roles;
import com.eshabakhov.surok.user.UrPostgres;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Test for {@link RqsHttp}.
 *
 * @since 0.0.1
 * @checkstyle ProhibitFieldsInTestClassesCheck (1000 lines)
 */
@Testcontainers
@DirtiesContext
@SpringBootTest
@Sql(
    statements = """
        INSERT INTO subject (id, origin, name)
        VALUES (507, 'FEDERAL', 'History'),
        (502, 'FEDERAL', 'Algebra'),
        (504, 'FEDERAL', 'Mathematics')
        ON CONFLICT DO NOTHING;
        """
)
@SuppressWarnings({
    "PMD.UnitTestContainsTooManyAsserts",
    "PMD.UnusedPrivateMethod",
    "UnusedMethod"
})
final class RqsHttpTest {

    /** Postgres container. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("surok")
            .withUsername("test")
            .withPassword("test");

    /** JOOQ context. */
    private final DSLContext ctx;

    @Autowired
    RqsHttpTest(final DSLContext ctx) {
        this.ctx = ctx;
    }

    @BeforeEach
    void insertCurriculumAndRequirement() {
        this.ctx.resultQuery(
            """
            INSERT INTO public.federal_curriculum (id, title, education_level, study_week_type,
            version, academic_year, description, is_deleted)
            VALUES (?, ?, CAST(? AS education_level_type), CAST(? AS study_week_type), ?, ?, ?, ?)
            """,
            1L,
            "Basic plan",
            "BASIC",
            "FIVE_DAYS",
            "1",
            "2026/2027",
            "Basic federal plan",
            false
        ).execute();
        this.ctx.resultQuery(
            """
                INSERT INTO public.federal_curriculum_requirement (id, federal_curriculum_id, grade,
                subject_id, hours, part_type, is_deleted)
                VALUES (?, ?, ?, (SELECT id FROM subject WHERE name = ?
                AND NOT is_deleted), ?, CAST(? AS curriculum_part_type), ?)
                """,
            7L,
            1L,
            5,
            "Mathematics",
            5,
            "MANDATORY",
            false
        ).execute();
    }

    @AfterEach
    void cleanContextAndCurriculums() {
        SecurityContextHolder.clearContext();
        this.ctx.resultQuery(
            """
                TRUNCATE TABLE public.federal_curriculum RESTART IDENTITY CASCADE
                """
        ).execute();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"text/html", "*/*"})
    void returnsRequirementsHtmlPage(final String accept) throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        if (accept != null) {
            request.addHeader(HttpHeaders.ACCEPT, accept);
        }
        request.setRequestURI("/federal/curriculums/1/requirements");
        final User user = new UrPostgres(
            this.ctx,
            this.ctx.resultQuery(
                """
                INSERT INTO public."user" (username, password, email, corporate, school_id)
                VALUES (?, 'secret', ?, true, ?) RETURNING id
                """,
                UUID.randomUUID().toString(),
                String.format("%s@example.org", UUID.randomUUID()),
                null
            ).fetchSingle("id", Long.class)
        );
        user.roles().grant(Roles.RoleEnum.ADMIN);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                user,
                null,
                List.of()
            )
        );
        MatcherAssert.assertThat(
            "HTML request must return the complete requirements page",
            (String) new RqsHttp(this.ctx).selection(
                1L,
                new FiltersRequest(Set.<Filter>of()),
                new PageRequest(15, 1),
                new SortsRequest(List.<Sort>of()),
                request
            ).getBody(),
            Matchers.allOf(
                Matchers.containsString("<html lang=\"ru\">"),
                Matchers.containsString("Basic plan"),
                Matchers.containsString("Mathematics")
            )
        );
    }

    @Test
    void returnsRequirementsHtmlFragment() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.ACCEPT, "text/html");
        request.setRequestURI(
            "/federal/curriculums/1/requirements/fragment"
        );
        final User user = new UrPostgres(
            this.ctx,
            this.ctx.resultQuery(
                """
                INSERT INTO public."user" (username, password, email, corporate, school_id)
                VALUES (?, 'secret', ?, true, ?) RETURNING id
                """,
                UUID.randomUUID().toString(),
                String.format("%s@example.org", UUID.randomUUID()),
                null
            ).fetchSingle("id", Long.class)
        );
        user.roles().grant(Roles.RoleEnum.ADMIN);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                user,
                null,
                List.of()
            )
        );
        MatcherAssert.assertThat(
            "Fragment request must return only requirement results",
            (String) new RqsHttp(this.ctx).selection(
                1L,
                new FiltersRequest(Set.<Filter>of()),
                new PageRequest(15, 1),
                new SortsRequest(List.<Sort>of()),
                request
            ).getBody(),
            Matchers.allOf(
                Matchers.not(Matchers.containsString("<html")),
                Matchers.containsString("Mathematics")
            )
        );
    }

    @Test
    void returnsRequirementsHtmlForNullUri() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.ACCEPT, "text/html");
        request.setRequestURI(null);
        final User user = new UrPostgres(
            this.ctx,
            this.ctx.resultQuery(
                """
                INSERT INTO public."user" (username, password, email, corporate, school_id)
                VALUES (?, 'secret', ?, true, ?) RETURNING id
                """,
                UUID.randomUUID().toString(),
                String.format("%s@example.org", UUID.randomUUID()),
                null
            ).fetchSingle("id", Long.class)
        );
        user.roles().grant(Roles.RoleEnum.ADMIN);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                user,
                null,
                List.of()
            )
        );
        MatcherAssert.assertThat(
            "Null request URI must select the complete requirements page",
            (String) new RqsHttp(this.ctx).selection(
                1L,
                new FiltersRequest(Set.<Filter>of()),
                new PageRequest(15, 1),
                new SortsRequest(List.<Sort>of()),
                request
            ).getBody(),
            Matchers.containsString("<html lang=\"ru\">")
        );
    }

    @Test
    void returnsRequirementsJson() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(
            HttpHeaders.ACCEPT,
            "application/vnd.surok.federal-curriculum-requirements.v1+json"
        );
        final ObjectNode response = (ObjectNode) new RqsHttp(this.ctx).selection(
            1L,
            new FiltersRequest(Set.<Filter>of()),
            new PageRequest(15, 1),
            new SortsRequest(List.<Sort>of()),
            request
        ).getBody();
        MatcherAssert.assertThat(
            "JSON requirements must contain the selected requirement",
            response.path("items").get(0).path("subject").path("name").asText(),
            Matchers.is("Mathematics")
        );
        MatcherAssert.assertThat(
            "JSON requirements must contain pagination",
            response.path("page").path("total").asInt(),
            Matchers.is(1)
        );
    }

    @Test
    void returnsRequirementsXml() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(
            HttpHeaders.ACCEPT,
            "application/vnd.surok.federal-curriculum-requirements.v1+xml"
        );
        MatcherAssert.assertThat(
            "XML requirements must use the collection root",
            (String) new RqsHttp(this.ctx).selection(
                1L,
                new FiltersRequest(Set.<Filter>of()),
                new PageRequest(15, 1),
                new SortsRequest(List.<Sort>of()),
                request
            ).getBody(),
            Matchers.allOf(
                Matchers.startsWith("<requirements>"),
                Matchers.containsString("<requirement>"),
                Matchers.endsWith("</requirements>")
            )
        );
    }

    @Test
    void rejectsUnsupportedRequirementsAccept() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.ACCEPT, "application/json");
        MatcherAssert.assertThat(
            "Unsupported Accept must result in 406",
            Assertions.assertThrows(
                ResponseStatusException.class,
                () -> new RqsHttp(this.ctx).selection(
                    1L,
                    new FiltersRequest(Set.<Filter>of()),
                    new PageRequest(15, 1),
                    new SortsRequest(List.<Sort>of()),
                    request
                )
            ).getStatusCode(),
            Matchers.is(HttpStatus.NOT_ACCEPTABLE)
        );
    }

    @Test
    void returnsRequirementJson() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(
            HttpHeaders.ACCEPT,
            "application/vnd.surok.federal-curriculum-requirement-v1+json"
        );
        MatcherAssert.assertThat(
            "JSON requirement must contain its identity",
            ((ObjectNode) new RqsHttp(this.ctx).requirement(1L, 7L, request).getBody())
                .path("id").longValue(),
            Matchers.is(7L)
        );
    }

    @Test
    void returnsRequirementXml() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(
            HttpHeaders.ACCEPT,
            "application/vnd.surok.federal-curriculum-requirement-v1+xml"
        );
        MatcherAssert.assertThat(
            "XML requirement must use the entity root",
            (String) new RqsHttp(this.ctx).requirement(1L, 7L, request).getBody(),
            Matchers.allOf(
                Matchers.startsWith("<requirement>"),
                Matchers.containsString("<id>7</id>"),
                Matchers.endsWith("</requirement>")
            )
        );
    }

    @Test
    void defaultsToRequirementJsonWithoutAccept() throws Exception {
        MatcherAssert.assertThat(
            ((ObjectNode) new RqsHttp(this.ctx).requirement(
                1L, 7L, new MockHttpServletRequest()
            ).getBody())
                .path("id").longValue(),
            Matchers.is(7L)
        );
    }

    @Test
    void rejectsUnsupportedRequirementAccept() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.ACCEPT, "application/xml");
        Assertions.assertThrows(
            ResponseStatusException.class,
            () -> new RqsHttp(this.ctx).requirement(1L, 7L, request)
        );
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"text/html", "*/*"})
    void createsRequirementForHtml(final String accept) throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        if (accept != null) {
            request.addHeader(HttpHeaders.ACCEPT, accept);
        }
        final MultiValueMap<String, String> parameters =
            new LinkedMultiValueMap<>();
        parameters.add("grade", "6");
        parameters.add("subjectId", "507");
        parameters.add("hours", "2");
        parameters.add("partType", "MANDATORY");
        final ResponseEntity<?> response = new RqsHttp(this.ctx).create(
            1L, parameters, request
        );
        MatcherAssert.assertThat(
            "HTML creation must not return a response body",
            response.getBody(),
            Matchers.nullValue()
        );
        MatcherAssert.assertThat(
            "HTML creation must redirect to requirements",
            response.getHeaders().getLocation().toString(),
            Matchers.is("/federal/curriculums/1/requirements")
        );
    }

    @Test
    void createsRequirementAsJson() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(
            HttpHeaders.ACCEPT,
            "application/vnd.surok.federal-curriculum-requirement-v1+json"
        );
        final MultiValueMap<String, String> parameters =
            new LinkedMultiValueMap<>();
        parameters.add("grade", "6");
        parameters.add("subjectId", "507");
        parameters.add("hours", "2");
        parameters.add("partType", "MANDATORY");
        final ResponseEntity<?> response = new RqsHttp(this.ctx).create(
            1L,
            parameters,
            request
        );
        MatcherAssert.assertThat(
            "JSON creation must return status 201",
            response.getStatusCode().value(),
            Matchers.is(HttpStatus.CREATED.value())
        );
        MatcherAssert.assertThat(
            "JSON creation must expose the created location",
            response.getHeaders().getFirst(HttpHeaders.LOCATION),
            Matchers.matchesPattern(
                "/federal/curriculums/1/requirements/[0-9]+"
            )
        );
    }

    @Test
    void createsRequirementAsXml() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(
            HttpHeaders.ACCEPT,
            "application/vnd.surok.federal-curriculum-requirement-v1+xml"
        );
        final MultiValueMap<String, String> parameters =
            new LinkedMultiValueMap<>();
        parameters.add("grade", "6");
        parameters.add("subjectId", "507");
        parameters.add("hours", "2");
        parameters.add("partType", "MANDATORY");
        final ResponseEntity<?> response = new RqsHttp(this.ctx).create(1L, parameters, request);
        MatcherAssert.assertThat(
            "XML creation must return the created requirement",
            (String) response.getBody(),
            Matchers.containsString(
                "<subject><id>507</id><name>History</name><origin>FEDERAL</origin></subject>"
            )
        );
        MatcherAssert.assertThat(
            "XML creation must return status 201",
            response.getStatusCode().value(),
            Matchers.is(HttpStatus.CREATED.value())
        );
    }

    @Test
    void rejectsUnsupportedCreationAccept() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.ACCEPT, "application/json");
        final MultiValueMap<String, String> parameters =
            new LinkedMultiValueMap<>();
        parameters.add("grade", "6");
        parameters.add("subjectId", "507");
        parameters.add("hours", "2");
        parameters.add("partType", "MANDATORY");
        Assertions.assertThrows(
            ResponseStatusException.class,
            () -> new RqsHttp(this.ctx).create(1L, parameters, request)
        );
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"text/html", "*/*"})
    void revisesRequirementForHtml(final String accept) throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        if (accept != null) {
            request.addHeader(HttpHeaders.ACCEPT, accept);
        }
        final MultiValueMap<String, String> parameters =
            new LinkedMultiValueMap<>();
        parameters.add("grade", "7");
        parameters.add("subjectId", "502");
        parameters.add("hours", "4");
        parameters.add("partType", "MANDATORY");
        final ResponseEntity<?> response = new RqsHttp(this.ctx).revise(
            1L, 7L, parameters, request
        );
        MatcherAssert.assertThat(
            "HTML revision must not return a response body",
            response.getBody(),
            Matchers.nullValue()
        );
        MatcherAssert.assertThat(
            "HTML revision must redirect to requirements",
            response.getHeaders().getLocation().toString(),
            Matchers.is("/federal/curriculums/1/requirements")
        );
    }

    @Test
    void revisesRequirementAsJson() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(
            HttpHeaders.ACCEPT,
            "application/vnd.surok.federal-curriculum-requirement-v1+json"
        );
        final MultiValueMap<String, String> parameters =
            new LinkedMultiValueMap<>();
        parameters.add("grade", "7");
        parameters.add("subjectId", "502");
        parameters.add("hours", "4");
        parameters.add("partType", "MANDATORY");
        MatcherAssert.assertThat(
            "JSON revision must return revised fields",
            ((ObjectNode) new RqsHttp(this.ctx).revise(1L, 7L, parameters, request).getBody())
                .path("subject").path("name").asText(),
            Matchers.is("Algebra")
        );
    }

    @Test
    void revisesRequirementAsXml() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(
            HttpHeaders.ACCEPT,
            "application/vnd.surok.federal-curriculum-requirement-v1+xml"
        );
        final MultiValueMap<String, String> parameters =
            new LinkedMultiValueMap<>();
        parameters.add("grade", "7");
        parameters.add("subjectId", "502");
        parameters.add("hours", "4");
        parameters.add("partType", "MANDATORY");
        MatcherAssert.assertThat(
            "XML revision must return revised fields",
            (String) new RqsHttp(this.ctx).revise(1L, 7L, parameters, request).getBody(),
            Matchers.containsString(
                "<subject><id>502</id><name>Algebra</name><origin>FEDERAL</origin></subject>"
            )
        );
    }

    @Test
    void rejectsUnsupportedRevisionAccept() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.ACCEPT, "application/xml");
        final MultiValueMap<String, String> parameters =
            new LinkedMultiValueMap<>();
        parameters.add("grade", "7");
        parameters.add("subjectId", "502");
        parameters.add("hours", "4");
        parameters.add("partType", "MANDATORY");
        Assertions.assertThrows(
            ResponseStatusException.class,
            () -> new RqsHttp(this.ctx).revise(1L, 7L, parameters, request)
        );
    }

    @Test
    void removesRequirement() throws Exception {
        new RqsHttp(this.ctx).remove(1L, 7L, new MockHttpServletRequest());
        MatcherAssert.assertThat(
            "HTTP removal must mark the requirement as deleted",
            this.ctx.resultQuery(
                """
                    SELECT federal_curriculum_requirement.is_deleted
                    FROM public.federal_curriculum_requirement
                    WHERE federal_curriculum_requirement.id = ?
                    """,
                7L
            ).fetchOneInto(Boolean.class),
            Matchers.is(true)
        );
    }

    @DynamicPropertySource
    private static void properties(final DynamicPropertyRegistry registry) {
        registry.add(
            "spring.datasource.driver-class-name",
            () -> "org.postgresql.Driver"
        );
        registry.add(
            "spring.datasource.url",
            RqsHttpTest.POSTGRES::getJdbcUrl
        );
        registry.add(
            "spring.datasource.username",
            RqsHttpTest.POSTGRES::getUsername
        );
        registry.add(
            "spring.datasource.password",
            RqsHttpTest.POSTGRES::getPassword
        );
        registry.add("spring.liquibase.enabled", () -> true);
    }
}
