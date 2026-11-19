/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import com.eshabakhov.surok.user.Roles;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Grants declared on OpenAPI operations, indexed by operationId.
 * A form can share the compiled permission of a domain operation via sameAs.
 *
 * @since 0.0.1
 */
final class PmGrants {

    /** Compiled permissions, including form aliases. */
    private final Map<String, Permission> rules;

    /**
     * Compile endpoint declarations independently of their order in the specification.
     *
     * @param paths OpenAPI paths
     */
    PmGrants(final JsonNode paths) {
        this(PmGrants.index(paths));
    }

    /**
     * New index of compiled grants.
     *
     * @param rules Grants by operationId
     */
    private PmGrants(final Map<String, Permission> rules) {
        this.rules = rules;
    }

    /**
     * Permission required by an operation.
     *
     * @param id OpenAPI operationId
     * @return Shared compiled grants
     */
    Permission forOperation(final String id) {
        if (!this.rules.containsKey(id)) {
            throw new IllegalArgumentException(String.format("Unknown permission for '%s'", id));
        }
        return this.rules.get(id);
    }

    private static Map<String, Permission> index(final JsonNode paths) {
        final Map<String, Permission> inline = new LinkedHashMap<>();
        final Map<String, String> aliases = new LinkedHashMap<>();
        for (final Map.Entry<String, JsonNode> path : paths.properties()) {
            for (final JsonNode operation : path.getValue().valueStream()
                .filter(node -> node.has("x-permission")).toList()) {
                final String id = PmGrants.identifier(operation, path.getKey());
                final JsonNode binding = operation.path("x-permission");
                if (inline.containsKey(id) || aliases.containsKey(id)) {
                    throw new IllegalArgumentException(
                        String.format("Duplicate operationId: %s", id)
                    );
                }
                if (binding.path("sameAs").isTextual()) {
                    aliases.put(id, binding.get("sameAs").textValue());
                } else {
                    inline.put(id, PmGrants.compile(id, binding.path("grants")));
                }
            }
        }
        final Map<String, Permission> resolved = new LinkedHashMap<>(inline);
        aliases.forEach(
            (id, target) -> {
                if (!inline.containsKey(target)) {
                    throw new IllegalArgumentException(
                        String.format("Missing grants for '%s' referenced by '%s'", target, id)
                    );
                }
                resolved.put(id, inline.get(target));
            }
        );
        return Map.copyOf(resolved);
    }

    private static String identifier(final JsonNode operation, final String path) {
        final JsonNode id = operation.path("operationId");
        final JsonNode binding = operation.path("x-permission");
        if (!id.isTextual() || id.textValue().isBlank() || !binding.isObject()
            || binding.size() != 1) {
            throw new IllegalArgumentException(
                String.format("Invalid permission for '%s' at %s", id.asText(), path)
            );
        }
        return id.textValue();
    }

    private static Permission compile(final String id, final JsonNode grants) {
        if (!grants.isArray()) {
            throw new IllegalArgumentException(String.format("Missing grants for '%s'", id));
        }
        return new PmAnyOf(grants.valueStream().map(PmGrants::rule).toList());
    }

    private static Permission rule(final JsonNode grant) {
        if (!grant.isObject() || grant.size() != 1) {
            throw new IllegalArgumentException("A grant requires exactly one selector");
        }
        final Permission result;
        if (grant.has("globalRoles")) {
            result = new PmGlobalRoles(PmGrants.roles(grant.path("globalRoles")));
        } else if (grant.has("schoolRoles")) {
            result = new PmSchoolRoles(PmGrants.roles(grant.path("schoolRoles")));
        } else if (grant.path("schoolMember").isBoolean()
            && grant.path("schoolMember").booleanValue()) {
            result = new PmSchoolMember();
        } else {
            throw new IllegalArgumentException("Unknown Buildings grant selector");
        }
        return result;
    }

    private static Set<Roles.RoleEnum> roles(final JsonNode names) {
        if (!names.isArray() || names.isEmpty()) {
            throw new IllegalArgumentException("Buildings roles must be a nonempty list");
        }
        final Set<Roles.RoleEnum> roles = EnumSet.noneOf(Roles.RoleEnum.class);
        for (final JsonNode name : names) {
            roles.add(Roles.RoleEnum.valueOf(name.asText()));
        }
        return Set.copyOf(roles);
    }
}
