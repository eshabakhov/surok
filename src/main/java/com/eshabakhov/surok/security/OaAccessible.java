/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security;

import com.eshabakhov.surok.security.permission.Permissions;
import io.swagger.v3.oas.models.OpenAPI;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Documentation restricted by the controllers' existing authorization expressions.
 * School routes describe operations available for the caller's own school;
 * actual requests still undergo method and resource authorization.
 * Register as an OpenApiCustomizer with springdoc caching disabled.
 *
 * @since 0.0.1
 */
@Component
public final class OaAccessible implements OpenApiCustomizer {

    /** Shared controller permissions. */
    private final Permissions permissions;

    /** Paths permitted without authentication in the security chains. */
    private final List<String> anonymous;

    /**
     * New caller-specific documentation.
     *
     * @param permissions Shared controller permission catalog
     * @param anonymous Public routes
     */
    public OaAccessible(
        final Permissions permissions, @Qualifier("publicPaths") final List<String> anonymous
    ) {
        this.permissions = permissions;
        this.anonymous = anonymous;
    }

    @Override
    public void customise(final OpenAPI document) {
        document.getPaths().forEach(
            (path, item) -> item.readOperationsMap().forEach(
                (method, operation) -> {
                    if (this.permissions.allows(
                        path, RequestMethod.valueOf(method.name()),
                        SecurityContextHolder.getContext().getAuthentication()
                    )) {
                        if (this.anonymous.stream().anyMatch(
                            pattern -> new AntPathMatcher().match(pattern, path)
                        )) {
                            operation.setSecurity(List.of());
                        }
                    } else {
                        item.operation(method, null);
                    }
                }
            )
        );
        document.getPaths().entrySet().removeIf(
            entry -> entry.getValue().readOperations().isEmpty()
        );
    }
}
