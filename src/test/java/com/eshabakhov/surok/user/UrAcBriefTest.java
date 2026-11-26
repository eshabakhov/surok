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

/**
 * Brief accounts preserve their representation while delegating authorization.
 * For example, a brief account must respect its origin's restricted catalog.
 *
 * @since 0.0.1
 */
final class UrAcBriefTest {

    /**
     * Preserves navigation after a permitted global operation.
     *
     * @throws Exception If the operation catalog cannot be read
     */
    @Test
    void permitsAGlobalOperationThroughTheOrigin() throws Exception {
        MatcherAssert.assertThat(
            "The permitted account lost its school navigation",
            new UrAcBrief(new UrAcRole(Roles.RoleEnum.ADMIN)).permit("Curriculums.create")
                .schools().contains(1L),
            Matchers.is(true)
        );
    }

    /** A brief view cannot replace the origin's catalog with a broader one. */
    @Test
    void respectsTheOriginsRestrictedCatalog() {
        final UrAccount actor = new UrAcBrief(
            new UrAcAuth(new UrAcRole(Roles.RoleEnum.ADMIN), List::of)
        );
        Assertions.assertThrows(
            IllegalArgumentException.class, () -> actor.permit("Curriculums.create"),
            "The brief account bypassed the origin's restricted catalog"
        );
    }

    /**
     * Delegation checks a school operation once before navigation.
     *
     * @throws Exception If the catalog or school cannot be read
     */
    @Test
    void delegatesASchoolPermissionOnlyOnce() throws Exception {
        final Operations catalog = new OaYaml().operations();
        final List<String> checks = new ArrayList<>(1);
        final UrAccount actor = new UrAcBrief(
            new UrAcAuth(
                new UrAcRole(79L, Roles.RoleEnum.DIRECTOR), () -> {
                    checks.add("permission");
                    return catalog.iterate();
                }
            )
        );
        actor.permit("Buildings.create", actor.schools().school(79L)).schools().school(79L);
        MatcherAssert.assertThat(
            "The brief account repeated the authorization check",
            checks, Matchers.contains("permission")
        );
    }
}
