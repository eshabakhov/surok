/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security;

import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.security.permission.Permissions;
import com.eshabakhov.surok.security.permission.PsSpring;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

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
    private final ApiDescription description;

    /** Registered controllers. */
    private final RequestMappingHandlerMapping mappings;

    /** Public paths shared with the filter chains. */
    private final List<String> anonymous;

    /**
     * New documentation endpoint.
     *
     * @param mappings Registered MVC mappings
     * @param anonymous Public paths
     */
    public OaHttp(
        @Qualifier("requestMappingHandlerMapping") final RequestMappingHandlerMapping mappings,
        @Qualifier("publicPaths") final List<String> anonymous
    ) {
        this.description = new OaYaml(new ClassPathResource("openapi.yml"));
        this.mappings = mappings;
        this.anonymous = anonymous;
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
        return Json.mapper().writeValueAsBytes(this.describe(request, actor));
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
        return Yaml.mapper().writeValueAsBytes(this.describe(request, actor));
    }

    /*
     * Resolve the default same-origin server against the servlet context path.
     */
    private ObjectNode describe(
        final HttpServletRequest request,
        final UrAccount actor
    ) throws IOException {
        final Permissions permissions = new PsSpring(
            this.mappings, this.anonymous, request.getLocale()
        );
        final Permissions access;
        if (actor == null || request.getUserPrincipal() == null) {
            access = permissions;
        } else {
            access = permissions.selection(actor);
        }
        final ObjectNode document = new OaComponents(
            new OaAccessible(this.description, access)
        ).read();
        final String path = request.getContextPath();
        final ObjectNode server = document.withObject("/servers/0");
        if (!path.isEmpty() && "/".equals(server.path("url").asText())) {
            server.put("url", path);
        }
        return document;
    }
}
