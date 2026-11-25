/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security;

import com.eshabakhov.surok.user.UrAnonymous;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * A path item groups methods and resolves inherited security.
 * For example, public GET can override security without exposing protected POST.
 *
 * @since 0.0.1
 */
final class PiYamlTest {

    /**
     * Resolves an operation through its containing path.
     *
     * @throws Exception If the document cannot be loaded
     */
    @Test
    void selectsAnOperationByItsMethod() throws Exception {
        MatcherAssert.assertThat(
            "The path item selected the wrong operation or interpreted metadata as a method",
            new OaYaml(
                new ByteArrayResource(
                    """
                    paths:
                        /школы:
                            parameters: []
                            post: {operationId: Schools.create, x-roles: [DIRECTOR]}
                            get: {operationId: Schools.read, x-roles: []}
                    """.getBytes(StandardCharsets.UTF_8)
                )
            ).paths().path("/школы").operation(RequestMethod.POST).identifier(),
            Matchers.is("Schools.create")
        );
    }

    /**
     * Preserves global security unless the selected operation explicitly overrides it.
     *
     * @throws Exception If the document cannot be loaded
     */
    @Test
    void preservesAndOverridesInheritedSecurity() throws Exception {
        final PathItem item = new OaYaml(
            new ByteArrayResource(
                """
                security: [{basic: []}]
                paths:
                    /школы:
                        get: {operationId: Schools.read, x-roles: [], security: []}
                        post: {operationId: Schools.create, x-roles: []}
                """.getBytes(StandardCharsets.UTF_8)
            )
        ).paths().path("/школы");
        MatcherAssert.assertThat(
            "The path item lost inherited security or ignored an operation override",
            List.of(
                item.operation(RequestMethod.GET).permission(new UrAnonymous()).allows(),
                item.operation(RequestMethod.POST).permission(new UrAnonymous()).allows()
            ),
            Matchers.contains(true, false)
        );
    }
}
