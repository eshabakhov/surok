/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security;

import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.user.UrAnonymous;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves the OpenAPI description for the current caller.
 * For example, GET /api-docs.yaml returns the permitted operations as YAML.
 *
 * @since 0.0.1
 */
@RestController
@ConditionalOnProperty(
    name = "app.security.docs.enabled", havingValue = "true", matchIfMissing = true
)
public final class OaHttp {

    /** Source of the canonical description. */
    private final OpenApi description;

    /**
     * New resource with an explicitly constructed YAML description.
     */
    public OaHttp() {
        this(new OaYaml());
    }

    /**
     * New documentation endpoint.
     *
     * @param description Canonical API description
     */
    public OaHttp(final OpenApi description) {
        this.description = description;
    }

    /**
     * Serve the description as JSON.
     *
     * @param request Current request
     * @param actor Typed account identity, absent for anonymous requests
     * @return UTF-8 JSON
     * @throws IOException If the document cannot be read or written
     */
    @GetMapping(value = "/api-docs", produces = MediaType.APPLICATION_JSON_VALUE)
    public byte[] json(
        final HttpServletRequest request,
        @AuthenticationPrincipal final UrAccount actor
    ) throws IOException {
        return this.describe(request, actor).json();
    }

    /**
     * Serve the description as YAML.
     *
     * @param request Current request
     * @param actor Typed account identity, absent for anonymous requests
     * @return UTF-8 YAML
     * @throws IOException If the document cannot be read or written
     */
    @GetMapping(value = "/api-docs.yaml", produces = "application/vnd.oai.openapi")
    public byte[] yaml(
        final HttpServletRequest request,
        @AuthenticationPrincipal final UrAccount actor
    ) throws IOException {
        return this.describe(request, actor).yaml();
    }

    /*
     * Resolve the default same-origin server against the servlet context path.
     */
    private OpenApi describe(
        final HttpServletRequest request,
        final UrAccount actor
    ) throws IOException {
        final OpenApi selected;
        if (actor == null || request.getUserPrincipal() == null) {
            selected = this.description.selection(
                operation -> operation.permission(new UrAnonymous()).allows()
            );
        } else {
            selected = this.description.selection(
                operation -> operation.permission(actor, actor.schools()).allows()
            );
        }
        return selected.relative(request.getContextPath());
    }
}
