/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security;

import com.eshabakhov.surok.User;
import com.eshabakhov.surok.security.permission.Permissions;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Locale;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * A description restricted by the application's current permissions.
 * For example, an anonymous request only receives public operations.
 *
 * @since 0.0.1
 */
public final class OaAccessible implements ApiDescription {

    /** Source of an independent document. */
    private final ApiDescription origin;

    /** Shared controller permissions. */
    private final Permissions permissions;

    /** Caller whose operations are inspected. */
    private final User actor;

    /**
     * New caller-specific documentation.
     *
     * @param origin Canonical description
     * @param permissions Shared controller permission catalog
     * @param actor Current caller
     */
    public OaAccessible(
        final ApiDescription origin,
        final Permissions permissions,
        final User actor
    ) {
        this.origin = origin;
        this.permissions = permissions;
        this.actor = actor;
    }

    @Override
    public ObjectNode read() throws IOException {
        final ObjectNode document = this.origin.read();
        final ObjectNode paths = document.withObjectProperty("paths");
        paths.properties().removeIf(
            entry -> !this.restrict(entry.getKey(), paths.withObjectProperty(entry.getKey()))
        );
        return document;
    }

    /*
     * Remove inaccessible methods while retaining shared path metadata.
     */
    private boolean restrict(final String path, final ObjectNode item) {
        boolean visible = false;
        for (final RequestMethod method : RequestMethod.values()) {
            final String name = method.name().toLowerCase(Locale.ROOT);
            if (item.has(name)) {
                if (this.permissions.allows(path, method, this.actor)) {
                    visible = true;
                } else {
                    item.remove(name);
                }
            }
        }
        return visible;
    }
}
