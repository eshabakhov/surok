/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security;

import com.eshabakhov.surok.user.Roles;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Validated operations of one path item or the internal operation extension.
 * For example, resolve Buildings.create and its x-roles before granting access.
 *
 * @since 0.0.1
 */
public final class OpsYaml implements Operations {

    /** Route template, empty for internal declarations. */
    private final String path;

    /** Path item or internal operation declarations. */
    private final JsonNode document;

    /** Inherited root security. */
    private final JsonNode security;

    /**
     * New operations from a path item or an internal declaration list.
     *
     * @param path Route template
     * @param document Path item or internal declaration list
     * @param security Inherited security requirements
     */
    public OpsYaml(final String path, final JsonNode document, final JsonNode security) {
        this.path = path;
        this.document = document;
        this.security = security;
    }

    @Override
    public Iterable<Operation> iterate() {
        final List<Operation> operations = new ArrayList<>(this.document.size());
        if (this.document.isArray()) {
            for (final JsonNode declaration : this.document) {
                operations.add(this.operation(RequestMethod.GET, declaration));
            }
        } else {
            for (final RequestMethod method : RequestMethod.values()) {
                if (this.document.has(method.name().toLowerCase(Locale.ROOT))) {
                    operations.add(
                        this.operation(
                            method, this.document.path(method.name().toLowerCase(Locale.ROOT))
                        )
                    );
                }
            }
        }
        return List.copyOf(operations);
    }

    /*
     * Read one HTTP or domain operation with inherited security requirements.
     */
    private Operation operation(final RequestMethod method, final JsonNode operation) {
        final JsonNode identifier = operation.path("operationId");
        final JsonNode names = operation.path("x-roles");
        if (!identifier.isTextual() || identifier.asText().isBlank() || !names.isArray()) {
            throw new IllegalArgumentException(
                String.format("Invalid API permission for %s %s", method, this.path)
            );
        }
        final Set<Roles.RoleEnum> roles = new HashSet<>(names.size());
        for (final JsonNode name : names) {
            if (!name.isTextual()) {
                throw new IllegalArgumentException(
                    String.format("Invalid role for %s: %s", identifier, name)
                );
            }
            roles.add(Roles.RoleEnum.valueOf(name.asText()));
        }
        final JsonNode inherited;
        if (operation.has("security")) {
            inherited = operation.path("security");
        } else {
            inherited = this.security;
        }
        return new OpYaml(
            this.path, method, operation.<ObjectNode>deepCopy().set(
                "security", inherited
            ), Set.copyOf(roles)
        );
    }
}
