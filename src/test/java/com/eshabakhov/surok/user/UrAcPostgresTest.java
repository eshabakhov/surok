/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.School;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.media.MdXml;
import com.eshabakhov.surok.school.SlsPostgres;
import com.eshabakhov.surok.school.input.SlInput;
import com.eshabakhov.surok.school.input.SlInputName;
import java.util.UUID;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Database accounts evaluate current assignments through their own operation catalog.
 * For example, revoking a role must affect an account object already held by a caller.
 *
 * @since 0.0.1
 */
@Testcontainers
@DirtiesContext
@SpringBootTest
final class UrAcPostgresTest {

    /** Isolated database for account permissions. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16");

    /**
     * The school directory includes the global administrator flag.
     *
     * @param ctx Database context
     * @throws Exception If the account cannot be created or printed
     */
    @Test
    void printsThePublicFieldsOfAnAdministrator(@Autowired final DSLContext ctx) throws Exception {
        final String name = String.format("Директор-%s", UUID.randomUUID());
        final UrAccount user = new UrsPostgres(ctx).register(
            name, "ValidPassword1!", String.format("%s@example.org", UUID.randomUUID()), false
        );
        user.roles().grant(Roles.RoleEnum.ADMIN);
        Assertions.assertEquals(
            String.format(
                "<user id=\"%d\"><username>%s</username><admin>true</admin></user>",
                user.uid(), name
            ),
            user.print(new MdXml("user")).print(), "The public user fields were incorrect"
        );
    }

    /**
     * Global authorization is reevaluated after a role is revoked.
     *
     * @param ctx Database context
     * @throws Exception If account setup fails
     */
    @Test
    void deniesAnOperationAfterItsGlobalRoleIsRevoked(@Autowired final DSLContext ctx)
        throws Exception {
        final UrAccount actor = new UrsPostgres(ctx).register(
            String.format("Администратор-%s", UUID.randomUUID()), "ValidPassword1!",
            String.format("%s@example.org", UUID.randomUUID()), false
        );
        actor.roles().grant(Roles.RoleEnum.ADMIN);
        actor.permit("Curriculums.create");
        actor.roles().revoke(
            actor.roles().list().stream().filter(
                role -> Roles.RoleEnum.ADMIN.name().equals(role.name())
            ).findFirst().orElseThrow().uid()
        );
        Assertions.assertThrows(
            AccessDeniedException.class, () -> actor.permit("Curriculums.create"),
            "The account retained a revoked global permission"
        );
    }

    /**
     * School authorization is reevaluated after membership is removed.
     *
     * @param ctx Database context
     * @throws Exception If account or school setup fails
     */
    @Test
    void deniesAnOperationAfterSchoolMembershipIsRemoved(@Autowired final DSLContext ctx)
        throws Exception {
        final UrAccount actor = new UrsPostgres(ctx).register(
            String.format("Директор-%s", UUID.randomUUID()), "ValidPassword1!",
            String.format("%s@example.org", UUID.randomUUID()), false
        );
        final School school = new SlsPostgres(ctx, actor).create(
            new SlInputName(new SlInput(), "Школа проверки доступа")
        );
        actor.permit("Buildings.create", school).schools().school(school.uid());
        school.users().remove(actor.uid());
        Assertions.assertThrows(
            AccessDeniedException.class, () -> actor.permit("Buildings.create", school),
            "The account retained permission after losing school membership"
        );
    }

    /**
     * Connects Spring to the isolated database.
     *
     * @param registry Spring properties
     */
    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", UrAcPostgresTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", UrAcPostgresTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", UrAcPostgresTest.POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }
}
