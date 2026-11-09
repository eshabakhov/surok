/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.User;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.ResourceBundle;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.expression.DefaultHttpSecurityExpressionHandler;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;

/**
 * An operation described by its Spring handler and current permission rule.
 * For example, school and federal resources expose their decorator rules.
 *
 * @since 0.0.1
 */
public final class PmMethod implements Permission {

    /** Registered handler. */
    private final HandlerMethod handler;

    /** Concrete route pattern. */
    private final String path;

    /** Whether the filter chain permits anonymous requests. */
    private final boolean anonymous;

    /**
     * New method permission.
     *
     * @param handler Registered controller method
     * @param path Route pattern
     * @param anonymous Public route marker
     */
    public PmMethod(final HandlerMethod handler, final String path, final boolean anonymous) {
        this.handler = handler;
        this.path = path;
        this.anonymous = anonymous;
    }

    @Override
    public boolean allows(
        final Authentication actor, final HttpServletRequest request, final boolean own
    ) {
        boolean allowed;
        try {
            final EvaluationContext context = this.context(actor, request, own);
            final SpelExpressionParser parser = new SpelExpressionParser();
            allowed = (this.anonymous || Boolean.TRUE.equals(
                parser.parseExpression("isAuthenticated()").getValue(context, Boolean.class)
            )) && Boolean.TRUE.equals(
                parser.parseExpression(this.rule()).getValue(context, Boolean.class)
            );
        } catch (final EvaluationException exception) {
            allowed = false;
        }
        return allowed;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) {
        final ResourceBundle labels = ResourceBundle.getBundle(
            "localization.permissions", Locale.ROOT
        );
        String resource = this.handler.getBeanType().getSimpleName();
        if ("SbsHttp".equals(resource) && this.path.startsWith("/schools/")) {
            resource = "SchoolSubjects";
        }
        if ("UrsHttp".equals(resource)) {
            resource = this.userResource();
        }
        final String key = String.format("%s.%s", resource, this.handler.getMethod().getName());
        String title = this.handler.getMethod().getName();
        if (labels.containsKey(String.format("%s%s", key, this.path))) {
            title = labels.getString(String.format("%s%s", key, this.path));
        } else if (labels.containsKey(key)) {
            title = labels.getString(key);
        }
        if (labels.containsKey(resource)) {
            resource = labels.getString(resource);
        }
        final RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(
            this.handler.getMethod(), RequestMapping.class
        );
        String consumes = "";
        if (mapping != null) {
            consumes = String.join(", ", mapping.consumes());
        }
        String condition = "";
        if (labels.containsKey(String.format("%s.condition", key))) {
            condition = labels.getString(String.format("%s.condition", key));
        }
        return media.with("resource", resource).with("action", title).with("path", this.path)
            .with("consumes", consumes).with("condition", condition);
    }

    private EvaluationContext context(
        final Authentication actor, final HttpServletRequest request, final boolean own
    ) {
        final DefaultHttpSecurityExpressionHandler expressions =
            new DefaultHttpSecurityExpressionHandler();
        final EvaluationContext context = expressions.createEvaluationContext(
            () -> actor, new RequestAuthorizationContext(request)
        );
        if (this.path.contains("{school}")) {
            long school = -1L;
            if (actor.getPrincipal() instanceof User user) {
                for (final School available : user.schools().iterate()) {
                    school = available.uid();
                }
            } else {
                school = 1L;
            }
            if (!own) {
                school = -2L;
            }
            context.setVariable("school", school);
        }
        return context;
    }

    private String rule() {
        return switch (this.handler.getBeanType().getSimpleName()) {
            case "SlsHttp" -> this.schools();
            case "BdsHttp", "CbsHttp", "ScsHttp", "ThsHttp", "SdsHttp", "ClsHttp", "LdHttp" ->
                this.resources();
            case "CrsHttp" -> this.curriculums();
            case "RqsHttp" -> this.requirements();
            case "SbsHttp" -> this.subjects();
            case "UrsHttp" -> this.users();
            case "PsHttp" -> "hasRole('ADMIN')";
            case null, default -> "true";
        };
    }

    private String users() {
        return switch (this.handler.getMethod().getName()) {
            case "authenticationPage", "registerForm", "register", "profile", "subscription",
                "checkout", "cancel" -> "true";
            case "remove", "assignRole", "listRoles", "removeRole" -> "hasRole('ADMIN')";
            case "schoolUsers", "schoolRoles" ->
                "hasRole('ADMIN') or authentication.principal.schools().contains(#school)";
            case "addSchoolUser", "attachSchoolUser", "removeSchoolUser", "grantSchoolRole",
                "revokeSchoolRole" -> """
                    hasRole('ADMIN') or (
                        authentication.principal.schools().contains(#school) and
                        hasAuthority('SCHOOL_' + #school + '_DIRECTOR')
                    )
                    """;
            case null, default -> "false";
        };
    }

    private String userResource() {
        return switch (this.handler.getMethod().getName()) {
            case "authenticationPage", "registerForm" -> "UserAuthentication";
            case "profile" -> "UserProfile";
            case "subscription", "checkout", "cancel" -> "UserSubscription";
            case "schoolUsers", "addSchoolUser", "attachSchoolUser", "removeSchoolUser",
                "schoolRoles", "grantSchoolRole", "revokeSchoolRole" -> "SchoolUsers";
            case null, default -> "GlobalUsers";
        };
    }

    private String subjects() {
        final String rule;
        if (this.path.startsWith("/federal/")) {
            rule = switch (this.handler.getMethod().getName()) {
                case "create", "revise", "remove", "form", "edit" -> "hasRole('ADMIN')";
                case "selection", "subject" -> "true";
                case null, default -> "false";
            };
        } else {
            rule = switch (this.handler.getMethod().getName()) {
                case "selection", "subject", "form", "edit" ->
                    "hasRole('ADMIN') or authentication.principal.schools().contains(#school)";
                case "create", "revise", "remove" -> """
                    hasRole('ADMIN') or (
                        authentication.principal.schools().contains(#school) and (
                            hasAuthority('SCHOOL_' + #school + '_DIRECTOR') or
                            hasAuthority('SCHOOL_' + #school + '_DEPUTY_DIRECTOR')
                        )
                    )
                    """;
                case null, default -> "false";
            };
        }
        return rule;
    }

    private String curriculums() {
        return switch (this.handler.getMethod().getName()) {
            case "create", "revise", "remove", "form", "edit" -> "hasRole('ADMIN')";
            case "curriculum", "selection" -> "true";
            case null, default -> "false";
        };
    }

    private String requirements() {
        return switch (this.handler.getMethod().getName()) {
            case "selection", "requirement" -> "true";
            case "create", "add", "update", "remove" -> "hasRole('ADMIN')";
            case null, default -> "false";
        };
    }

    private String schools() {
        return switch (this.handler.getMethod().getName()) {
            case "selection", "create", "form" -> "true";
            case "school" ->
                "hasRole('ADMIN') or authentication.principal.schools().contains(#school)";
            case "revise" -> """
                hasRole('ADMIN') or (
                    authentication.principal.schools().contains(#school) and
                    hasAuthority('SCHOOL_' + #school + '_DIRECTOR')
                )
                """;
            case "remove" -> "hasRole('ADMIN')";
            case null, default -> "false";
        };
    }

    private String resources() {
        return switch (this.handler.getMethod().getName()) {
            case "selection", "building", "cabinet", "clazz", "teacher", "schedule", "load",
                "read", "form" ->
                "hasRole('ADMIN') or authentication.principal.schools().contains(#school)";
            case "create", "revise", "remove", "save", "apply" -> """
                hasRole('ADMIN') or (
                    authentication.principal.schools().contains(#school) and (
                        hasAuthority('SCHOOL_' + #school + '_DIRECTOR') or
                        hasAuthority('SCHOOL_' + #school + '_DEPUTY_DIRECTOR')
                    )
                )
                """;
            case null, default -> "false";
        };
    }
}
