/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.PrItems;
import com.eshabakhov.surok.UrAccount;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Live permission catalog backed by Spring MVC mappings and decorator rules.
 * For example, construct this catalog with a request's locale to render the role matrix.
 *
 * @since 0.0.1
 */
public final class PsSpring implements Permissions {

    /** Registered controllers. */
    private final RequestMappingHandlerMapping mappings;

    /** Public paths shared with the filter chains. */
    private final List<String> anonymous;

    /** Locale for role titles. */
    private final Locale locale;

    /** Operation evaluation for the selected audience. */
    private final Predicate<Permission> access;

    /**
     * New application permission catalog.
     *
     * @param mappings Registered MVC mappings
     * @param anonymous Public paths
     * @param locale Locale for role titles
     */
    public PsSpring(
        final RequestMappingHandlerMapping mappings,
        final List<String> anonymous,
        final Locale locale
    ) {
        this(mappings, anonymous, locale, Permission::allows);
    }

    /**
     * New catalog with a bound audience.
     *
     * @param mappings Registered MVC mappings
     * @param anonymous Public paths
     * @param locale Locale for role titles
     * @param access Operation evaluation
     */
    private PsSpring(
        final RequestMappingHandlerMapping mappings, final List<String> anonymous,
        final Locale locale, final Predicate<Permission> access
    ) {
        this.mappings = mappings;
        this.anonymous = anonymous;
        this.locale = locale;
        this.access = access;
    }

    @Override
    public Permissions selection(final UrAccount actor) {
        return new PsSpring(
            this.mappings, this.anonymous, this.locale,
            permission -> permission.allows(actor, actor.schools())
        );
    }

    @Override
    public boolean allows(final String path, final RequestMethod method) {
        return this.mappings.getHandlerMethods().entrySet().stream().filter(
            entry -> entry.getKey().getPatternValues().contains(path)
                && (entry.getKey().getMethodsCondition().getMethods().isEmpty()
                || entry.getKey().getMethodsCondition().getMethods().contains(method))
        ).anyMatch(
            entry -> this.access.test(
                new PmMethod(
                    entry.getValue(), path,
                    this.anonymous.stream().anyMatch(
                        pattern -> new AntPathMatcher().match(pattern, path)
                    )
                )
            )
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
                            this.locale
                        )
                    ).toList()
                );
            }
        }
        return media.with("operations", new PrItems("operation", operations));
    }
}
