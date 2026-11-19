/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import com.eshabakhov.surok.Printable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Catalog of operations used by Swagger and the role matrix.
 * Buildings come from the canonical specification, including both formats of a single POST.
 *
 * @since 0.0.1
 */
@Component
public final class PmCatalog {

    /** Operations declared in the shared catalog. */
    private final PmYaml source;

    /** MVC mappings for resources awaiting migration. */
    private final RequestMappingHandlerMapping mappings;

    /** Public paths shared with the filter chains. */
    private final List<String> anonymous;

    /**
     * New application catalog.
     *
     * @param mappings Registered MVC mappings
     * @param anonymous Public paths
     * @param source Shared permission catalog
     */
    public PmCatalog(
        @Qualifier("requestMappingHandlerMapping") final RequestMappingHandlerMapping mappings,
        @Qualifier("publicPaths") final List<String> anonymous,
        final PmYaml source
    ) {
        this.mappings = mappings;
        this.anonymous = anonymous;
        this.source = source;
    }

    /**
     * Permissions for one HTTP operation.
     *
     * @param path Path template
     * @param method HTTP method
     * @return Matching permissions
     */
    Stream<Permission> matching(final String path, final RequestMethod method) {
        final Stream<Permission> result;
        if (this.source.routes().containsKey(path)) {
            result = Stream.ofNullable(this.source.routes().get(path).get(method));
        } else {
            result = this.mappings.getHandlerMethods().entrySet().stream().filter(
                entry -> entry.getKey().getPatternValues().contains(path)
                    && (entry.getKey().getMethodsCondition().getMethods().isEmpty()
                    || entry.getKey().getMethodsCondition().getMethods().contains(method))
            ).map(entry -> this.legacy(entry.getValue(), path));
        }
        return result;
    }

    /**
     * Operations displayed in the role matrix.
     *
     * @param locale Locale for role titles
     * @return One row per declared operation or legacy mapping
     */
    List<Printable> rows(final Locale locale) {
        final List<Printable> rows = new ArrayList<>(this.source.routes().size());
        this.source.routes().forEach(
            (path, methods) -> methods.forEach(
                (method, rule) -> {
                    if (PmCatalog.visible(path, method)) {
                        rows.add(new PmRoles<>(rule, method, locale));
                    }
                }
            )
        );
        for (final Map.Entry<RequestMappingInfo, HandlerMethod> entry
            : this.mappings.getHandlerMethods().entrySet().stream()
                .sorted(Comparator.comparing(item -> item.getKey().toString())).toList()) {
            for (final String path : entry.getKey().getPatternValues().stream().sorted().toList()) {
                if (!this.source.routes().containsKey(path) && entry.getValue().getBeanType()
                    .getPackageName().startsWith("com.eshabakhov.surok")) {
                    rows.addAll(
                        entry.getKey().getMethodsCondition().getMethods().stream().sorted()
                            .filter(method -> PmCatalog.visible(path, method)).map(
                                method -> new PmRoles<>(
                                    this.legacy(entry.getValue(), path), method, locale
                                )
                            ).toList()
                    );
                }
            }
        }
        return rows;
    }

    /**
     * Share the canonical domain catalog with HTTP controllers.
     *
     * @return Packaged operation permissions
     */
    @Bean
    static PmYaml permissions() {
        return PmYaml.PACKAGED;
    }

    private PmMethod legacy(final HandlerMethod handler, final String path) {
        return new PmMethod(
            handler, path, this.anonymous.stream().anyMatch(
                pattern -> new AntPathMatcher().match(pattern, path)
            )
        );
    }

    private static boolean visible(final String path, final RequestMethod method) {
        return (!path.endsWith("/") || "/".equals(path))
            && (method != RequestMethod.GET || !path.matches(".*/(create|edit|fragment)$"));
    }
}
