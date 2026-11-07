/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security;

import com.eshabakhov.surok.security.permission.Permissions;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
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

    /** Caller-specific description. */
    private final ApiDescription description;

    /**
     * New documentation endpoint.
     *
     * @param permissions Application permission catalog
     */
    public OaHttp(final Permissions permissions) {
        this.description = new OaComponents(
            new OaAccessible(new OaYaml(new ClassPathResource("openapi.yml")), permissions)
        );
    }

    /**
     * Serve the description as JSON.
     *
     * @param request Current request
     * @return UTF-8 JSON
     * @throws IOException If the document cannot be read or written
     */
    @GetMapping(value = "/api-docs", produces = MediaType.APPLICATION_JSON_VALUE)
    public byte[] json(final HttpServletRequest request) throws IOException {
        return Json.mapper().writeValueAsBytes(this.describe(request));
    }

    /**
     * Serve the description as YAML.
     *
     * @param request Current request
     * @return UTF-8 YAML
     * @throws IOException If the document cannot be read or written
     */
    @GetMapping(value = "/api-docs.yaml", produces = "application/vnd.oai.openapi")
    public byte[] yaml(final HttpServletRequest request) throws IOException {
        return Yaml.mapper().writeValueAsBytes(this.describe(request));
    }

    /*
     * Resolve the default same-origin server against the servlet context path.
     */
    private ObjectNode describe(final HttpServletRequest request) throws IOException {
        final ObjectNode document = this.description.read();
        final String path = request.getContextPath();
        final ObjectNode server = document.withObject("/servers/0");
        if (!path.isEmpty() && "/".equals(server.path("url").asText())) {
            server.put("url", path);
        }
        return document;
    }
}
