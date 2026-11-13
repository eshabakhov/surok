/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.PrItems;
import com.eshabakhov.surok.User;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Live permission catalog backed by Spring MVC mappings and decorator rules.
 * For example, inject this catalog into both Swagger filtering and the matrix resource.
 *
 * @since 0.0.1
 */
@Component
public final class PsSpring implements Permissions {

    /** Registered controllers. */
    private final RequestMappingHandlerMapping mappings;

    /** Current request. */
    private final HttpServletRequest request;

    /** Public paths shared with the filter chains. */
    private final List<String> anonymous;

    /**
     * New application permission catalog.
     *
     * @param mappings Registered MVC mappings
     * @param request Current request
     * @param anonymous Public paths
     */
    public PsSpring(
        @Qualifier("requestMappingHandlerMapping") final RequestMappingHandlerMapping mappings,
        final HttpServletRequest request,
        @Qualifier("publicPaths") final List<String> anonymous
    ) {
        this.mappings = mappings;
        this.request = request;
        this.anonymous = anonymous;
    }

    @Override
    public boolean allows(final String path, final RequestMethod method, final User actor) {
        return this.mappings.getHandlerMethods().entrySet().stream().filter(
            entry -> entry.getKey().getPatternValues().contains(path)
                && (entry.getKey().getMethodsCondition().getMethods().isEmpty()
                || entry.getKey().getMethodsCondition().getMethods().contains(method))
        ).anyMatch(
            entry -> new PmMethod(
                entry.getValue(), path,
                this.anonymous.stream().anyMatch(
                    pattern -> new AntPathMatcher().match(pattern, path)
                )
            ).allows(actor, true)
        );
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        final List<Permission> operations = new ArrayList<>(
            this.mappings.getHandlerMethods().size()
        );
        for (final Map.Entry<RequestMappingInfo, HandlerMethod> entry
            : this.mappings.getHandlerMethods().entrySet().stream().filter(
                item -> item.getValue().getBeanType().getPackageName()
                    .startsWith("com.eshabakhov.surok")
            ).sorted(Comparator.comparing(item -> item.getKey().toString())).toList()) {
            for (final String path : entry.getKey().getPatternValues().stream().sorted().filter(
                route -> !route.endsWith("/") || "/".equals(route)
            ).toList()) {
                operations.addAll(
                    entry.getKey().getMethodsCondition().getMethods().stream().sorted().filter(
                        method -> method != RequestMethod.GET
                            || !path.matches(".*/(create|edit|fragment)$")
                    ).map(
                        method -> new PmRoles(
                            new PmMethod(
                                entry.getValue(), path, this.anonymous.stream().anyMatch(
                                    pattern -> new AntPathMatcher().match(pattern, path)
                                )
                            ),
                            method,
                            this.request.getLocale()
                        )
                    ).toList()
                );
            }
        }
        return media.with("operations", new PrItems("operation", operations));
    }
}
