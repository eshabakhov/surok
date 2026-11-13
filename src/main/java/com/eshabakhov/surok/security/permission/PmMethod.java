/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.security.right.Right;
import com.eshabakhov.surok.security.right.RtAdmin;
import com.eshabakhov.surok.security.right.RtDirectorOrDeputy;
import com.eshabakhov.surok.security.right.RtEither;
import com.eshabakhov.surok.security.right.RtSchoolDirector;
import com.eshabakhov.surok.security.right.RtSchoolMember;
import com.eshabakhov.surok.security.right.RtSchools;
import java.util.Locale;
import java.util.ResourceBundle;
import org.springframework.core.annotation.AnnotatedElementUtils;
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
    public boolean allows(final User actor, final boolean own) {
        return (this.anonymous || actor.account().authenticated())
            && this.rule(actor, own).allows();
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

    /*
     * Select the domain right corresponding to this controller operation.
     */
    private Right rule(final User actor, final boolean own) {
        final Right admin = new RtAdmin(actor);
        final Right member = new RtEither(
            admin, new RtSchools(actor, own, school -> new RtSchoolMember(actor, school))
        );
        final Right director = new RtEither(
            admin, new RtSchools(actor, own, school -> new RtSchoolDirector(actor, school))
        );
        final Right editor = new RtEither(
            admin, new RtSchools(actor, own, school -> new RtDirectorOrDeputy(actor, school))
        );
        return switch (this.handler.getBeanType().getSimpleName()) {
            case "SlsHttp" -> this.schools(admin, member, director);
            case "BdsHttp", "CbsHttp", "ScsHttp", "ThsHttp", "SdsHttp", "ClsHttp", "LdHttp" ->
                this.resources(member, editor);
            case "CrsHttp" -> this.curriculums(admin);
            case "RqsHttp" -> this.requirements(admin);
            case "SbsHttp" -> this.subjects(admin, member, editor);
            case "UrsHttp" -> this.users(admin, member, director);
            case "PsHttp" -> admin;
            case null, default -> () -> true;
        };
    }

    /*
     * User operations with global or school permissions.
     */
    private Right users(final Right admin, final Right member, final Right director) {
        return switch (this.handler.getMethod().getName()) {
            case "authenticationPage", "registerForm", "register", "profile", "subscription",
                "checkout", "cancel" -> () -> true;
            case "remove", "assignRole", "listRoles", "removeRole" -> admin;
            case "schoolUsers", "schoolRoles" -> member;
            case "addSchoolUser", "attachSchoolUser", "removeSchoolUser", "grantSchoolRole",
                "revokeSchoolRole" -> director;
            case null, default -> () -> false;
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

    /*
     * Federal and school subject operations.
     */
    private Right subjects(final Right admin, final Right member, final Right editor) {
        final Right rule;
        if (this.path.startsWith("/federal/")) {
            rule = switch (this.handler.getMethod().getName()) {
                case "create", "revise", "remove", "form", "edit" -> admin;
                case "selection", "subject" -> () -> true;
                case null, default -> () -> false;
            };
        } else {
            rule = switch (this.handler.getMethod().getName()) {
                case "selection", "subject", "form", "edit" -> member;
                case "create", "revise", "remove" -> editor;
                case null, default -> () -> false;
            };
        }
        return rule;
    }

    /*
     * Federal curriculum operations.
     */
    private Right curriculums(final Right admin) {
        return switch (this.handler.getMethod().getName()) {
            case "create", "revise", "remove", "form", "edit" -> admin;
            case "curriculum", "selection" -> () -> true;
            case null, default -> () -> false;
        };
    }

    /*
     * Federal curriculum requirement operations.
     */
    private Right requirements(final Right admin) {
        return switch (this.handler.getMethod().getName()) {
            case "selection", "requirement" -> () -> true;
            case "create", "add", "update", "remove" -> admin;
            case null, default -> () -> false;
        };
    }

    /*
     * School collection and entity operations.
     */
    private Right schools(final Right admin, final Right member, final Right director) {
        return switch (this.handler.getMethod().getName()) {
            case "selection", "create", "form" -> () -> true;
            case "school" -> member;
            case "revise" -> director;
            case "remove" -> admin;
            case null, default -> () -> false;
        };
    }

    /*
     * Resources owned by a school.
     */
    private Right resources(final Right member, final Right editor) {
        return switch (this.handler.getMethod().getName()) {
            case "selection", "building", "cabinet", "clazz", "teacher", "schedule", "load",
                "read", "form" -> member;
            case "create", "revise", "remove", "save", "apply" -> editor;
            case null, default -> () -> false;
        };
    }
}
