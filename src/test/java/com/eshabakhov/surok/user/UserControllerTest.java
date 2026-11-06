/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.User;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Global user endpoints enforce the actor's live role through protected objects.
 * For example, a regular user cannot assign a global subscription role.
 *
 * @since 0.0.1
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
final class UserControllerTest {

    /** Isolated database. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    /**
     * Rejects a regular user's attempt to list global roles.
     *
     * @param mvc HTTP client
     * @param ctx Database context
     * @throws Exception If request or registration fails
     */
    @Test
    void rejectsRoleListingByARegularUser(
        @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        MatcherAssert.assertThat(
            "A regular user listed global roles",
            mvc.perform(
                MockMvcRequestBuilders.get("/api/users/{user}/roles", actor.uid()).with(
                    SecurityMockMvcRequestPostProcessors.user(new AuthUser(actor))
                )
            ).andReturn().getResponse().getStatus(),
            Matchers.is(HttpStatus.FORBIDDEN.value())
        );
    }

    /**
     * Rejects a regular user's attempt to assign a global role.
     *
     * @param mvc HTTP client
     * @param ctx Database context
     * @throws Exception If request or registration fails
     */
    @Test
    void rejectsRoleAssignmentByARegularUser(
        @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        MatcherAssert.assertThat(
            "A regular user assigned a global role",
            mvc.perform(
                MockMvcRequestBuilders.post("/api/users/{user}/roles", actor.uid())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"role\":\"PRO_MAKER\"}").with(
                        SecurityMockMvcRequestPostProcessors.user(new AuthUser(actor))
                    )
            ).andReturn().getResponse().getStatus(),
            Matchers.is(HttpStatus.FORBIDDEN.value())
        );
    }

    /**
     * Allows an administrator to assign a global role.
     *
     * @param mvc HTTP client
     * @param ctx Database context
     * @throws Exception If request or registration fails
     */
    @Test
    void assignsGlobalRoleForAnAdministrator(
        @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final User target = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        actor.roles().grant(Roles.RoleEnum.ADMIN);
        MatcherAssert.assertThat(
            "An administrator could not assign a global role",
            mvc.perform(
                MockMvcRequestBuilders.post("/api/users/{user}/roles", target.uid())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"role\":\"PRO_MAKER\"}").with(
                        SecurityMockMvcRequestPostProcessors.user(new AuthUser(actor))
                    )
            ).andReturn().getResponse().getStatus(),
            Matchers.is(HttpStatus.CREATED.value())
        );
    }

    /**
     * Configures the isolated database.
     *
     * @param registry Spring properties
     */
    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", UserControllerTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", UserControllerTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", UserControllerTest.POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }
}
