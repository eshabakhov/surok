/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.security.OaYaml;
import com.eshabakhov.surok.security.Operations;
import java.util.ArrayList;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Acting users authorize an operation before exposing their domain navigation.
 * For example, a director may change buildings only within the director's school.
 *
 * @since 0.0.1
 */
final class UrAcAuthTest {

    /**
     * Preserves global account roles in the Spring principal.
     *
     * @param role Global role
     */
    @ParameterizedTest
    @EnumSource(
        value = Roles.RoleEnum.class,
        names = {"ADMIN", "BASIC_MAKER", "ADVANCED_MAKER", "PRO_MAKER"}
    )
    void exposesGlobalRolesAsAuthorities(final Roles.RoleEnum role) {
        MatcherAssert.assertThat(
            "The Spring principal lost its global account role",
            new UrAcAuth(new UrAcRole(role), List::of).getAuthorities(),
            Matchers.contains(new SimpleGrantedAuthority(String.format("ROLE_%s", role.name())))
        );
    }

    /**
     * Keeps school roles outside the Spring authority collection.
     *
     * @param role School role
     */
    @ParameterizedTest
    @EnumSource(
        value = Roles.RoleEnum.class,
        names = {"DIRECTOR", "DEPUTY_DIRECTOR", "TEACHER", "STUDENT"}
    )
    void excludesSchoolRolesFromAuthorities(final Roles.RoleEnum role) {
        MatcherAssert.assertThat(
            "The Spring principal exposed a school role as an authority",
            new UrAcAuth(new UrAcRole(role), List::of).getAuthorities(),
            Matchers.empty()
        );
    }

    /**
     * Global administrators can start a permitted chain from the same actor.
     *
     * @throws Exception If the operation catalog cannot be read
     */
    @Test
    void permitsAGlobalOperationForAnAdministrator() throws Exception {
        final UrAccount actor = new UrAcAuth(
            new UrAcRole(Roles.RoleEnum.ADMIN), new OaYaml().operations()
        );
        MatcherAssert.assertThat(
            "The global permission lost the acting user",
            actor.permit("Curriculums.create"), Matchers.sameInstance(actor)
        );
    }

    /**
     * School roles cannot grant access to global administration.
     *
     * @throws Exception If the operation catalog cannot be read
     */
    @Test
    void deniesAGlobalOperationForASchoolDirector() throws Exception {
        final UrAccount actor = new UrAcAuth(
            new UrAcRole(Roles.RoleEnum.DIRECTOR), new OaYaml().operations()
        );
        Assertions.assertThrows(
            AccessDeniedException.class, () -> actor.permit("Curriculums.create"),
            "A school director gained global administration rights"
        );
    }

    /**
     * Managers can navigate their school after one permission check.
     *
     * @param role School management role
     * @throws Exception If the operation catalog or school cannot be read
     */
    @ParameterizedTest
    @EnumSource(value = Roles.RoleEnum.class, names = {"DIRECTOR", "DEPUTY_DIRECTOR"})
    void permitsAnOperationWithinTheActorsSchool(final Roles.RoleEnum role) throws Exception {
        final UrAccount account = new UrAcRole(37L, role);
        MatcherAssert.assertThat(
            "The permitted user lost access to the target school",
            new UrAcAuth(account, new OaYaml().operations())
                .permit("Buildings.create", account.schools().school(37L))
                .schools().school(37L).uid(),
            Matchers.is(37L)
        );
    }

    /**
     * A director's role does not apply to another school.
     *
     * @throws Exception If the operation catalog cannot be read
     */
    @Test
    void deniesAnOperationInAnotherSchool() throws Exception {
        final UrAccount actor = new UrAcAuth(
            new UrAcRole(41L, Roles.RoleEnum.DIRECTOR), new OaYaml().operations()
        );
        final UrAccount unrelated = new UrAcRole(43L, Roles.RoleEnum.DIRECTOR);
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> actor.permit("Buildings.create", unrelated.schools().school(43L)),
            "A director gained access to another school's operation"
        );
    }

    /**
     * Administrators can authorize an operation without target-school membership.
     *
     * @throws Exception If the catalog or target school cannot be read
     */
    @Test
    void permitsAnAdministratorWithoutSchoolMembership() throws Exception {
        final UrAccount actor = new UrAcAuth(
            new UrAcRole(47L, Roles.RoleEnum.ADMIN), new OaYaml().operations()
        );
        MatcherAssert.assertThat(
            "An administrator required a role in the target school",
            actor.permit(
                "Buildings.create", new UrAcRole(53L, Roles.RoleEnum.DIRECTOR).schools().school(53L)
            ),
            Matchers.sameInstance(actor)
        );
    }

    /**
     * Navigation does not repeat the operation lookup after authorization.
     *
     * @throws Exception If the catalog or target school cannot be read
     */
    @Test
    void checksTheOperationOnlyBeforeNavigation() throws Exception {
        final Operations catalog = new OaYaml().operations();
        final List<String> checks = new ArrayList<>(1);
        final UrAccount actor = new UrAcAuth(
            new UrAcRole(59L, Roles.RoleEnum.DIRECTOR), () -> {
                checks.add("permission");
                return catalog.iterate();
            }
        );
        actor.permit("Buildings.create", actor.schools().school(59L)).schools().school(59L);
        MatcherAssert.assertThat(
            "Navigation repeated the operation lookup", checks, Matchers.contains("permission")
        );
    }

    /**
     * Missing catalog entries cannot authorize even a global administrator.
     */
    @Test
    void deniesAnOperationAbsentFromTheActorsCatalog() {
        final UrAccount actor = new UrAcAuth(new UrAcRole(Roles.RoleEnum.ADMIN), List::of);
        Assertions.assertThrows(
            IllegalArgumentException.class, () -> actor.permit("Buildings.create"),
            "An operation absent from the actor's catalog was permitted"
        );
    }
}
