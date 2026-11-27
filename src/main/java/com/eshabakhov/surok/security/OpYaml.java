/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.security.permission.Permission;
import com.eshabakhov.surok.security.permission.PmAssigned;
import com.eshabakhov.surok.user.Roles;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * OpenAPI operation with resolved roles and its HTTP description.
 * For example, Buildings.create binds the declared roles to the target school.
 *
 * @since 0.0.1
 */
public final class OpYaml implements Operation {

    /** Route template. */
    private final String path;

    /** HTTP method. */
    private final RequestMethod method;

    /** Operation metadata. */
    private final JsonNode document;

    /** Validated roles, shared with aliases. */
    private final Set<Roles.RoleEnum> roles;

    /**
     * New operation with resolved declarations.
     *
     * @param path Route template
     * @param method HTTP method
     * @param document Operation metadata
     * @param roles Permitted roles
     */
    OpYaml(
        final String path, final RequestMethod method,
        final JsonNode document, final Set<Roles.RoleEnum> roles
    ) {
        this.path = path;
        this.method = method;
        this.document = document;
        this.roles = roles;
    }

    @Override
    public String identifier() {
        return this.document.path("operationId").asText();
    }

    @Override
    public String path() {
        return this.path;
    }

    @Override
    public RequestMethod method() {
        return this.method;
    }

    @Override
    public Permission permission(final User actor) {
        return () -> (this.document.path("security").isEmpty()
            || !actor.roles().contains(Roles.RoleEnum.ANONYMOUS))
            && (this.roles.isEmpty() || new PmAssigned(actor, this.roles).allows());
    }

    @Override
    public Permission permission(final UrAccount actor, final School school) {
        return () -> this.roles.isEmpty() || new PmAssigned(actor, school, this.roles).allows();
    }

    @Override
    public <T> Media<T> print(final Media<T> media) {
        final ResourceBundle labels = ResourceBundle.getBundle(
            "localization.permissions", Locale.ROOT
        );
        String resource = this.document.path("x-resource").asText(
            this.document.path("tags").path(0).asText("API")
        );
        final String key = resource.toLowerCase(Locale.ROOT);
        if (labels.containsKey(key)) {
            resource = labels.getString(key);
        }
        String action = this.document.path("summary").asText(this.identifier());
        if (labels.containsKey(this.identifier())) {
            action = labels.getString(this.identifier());
        }
        String condition = "";
        if (labels.containsKey(String.format("%s.condition", this.identifier()))) {
            condition = labels.getString(String.format("%s.condition", this.identifier()));
        }
        return media.with("resource", resource).with("action", action)
            .with("path", this.path).with("method", this.method.name()).with(
                "consumes", this.document.path("requestBody").path("content").propertyStream()
                    .map(Map.Entry::getKey).sorted().collect(Collectors.joining(", "))
            ).with("condition", condition);
    }
}
