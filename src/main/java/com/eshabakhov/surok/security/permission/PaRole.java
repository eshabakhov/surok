/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Printable;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.ResourceBundle;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * One role's access to an operation, including the school ownership condition.
 * For example, print DIRECTOR with scope school and VIEWER with scope denied.
 *
 * @since 0.0.1
 */
public final class PaRole implements Printable {

    /** Controller permission. */
    private final Permission permission;

    /** Role being inspected, or ANONYMOUS. */
    private final String role;

    /** Current request for Spring's expression context. */
    private final HttpServletRequest request;

    /**
     * New matrix cell.
     *
     * @param permission Controller permission
     * @param role Role name
     * @param request Current request
     */
    public PaRole(
        final Permission permission, final String role, final HttpServletRequest request
    ) {
        this.permission = permission;
        this.role = role;
        this.request = request;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) {
        final List<SimpleGrantedAuthority> authorities;
        if (List.of(
            "DIRECTOR", "DEPUTY_DIRECTOR", "TEACHER", "STUDENT", "VIEWER"
        ).contains(this.role)) {
            authorities = List.of(
                new SimpleGrantedAuthority(String.format("SCHOOL_1_%s", this.role))
            );
        } else {
            authorities = List.of(
                new SimpleGrantedAuthority(String.format("ROLE_%s", this.role))
            );
        }
        final Authentication actor;
        if ("ANONYMOUS".equals(this.role)) {
            actor = new AnonymousAuthenticationToken(
                "permissions", new RiRole(this.role), authorities
            );
        } else {
            actor = UsernamePasswordAuthenticationToken.authenticated(
                new RiRole(this.role), "", authorities
            );
        }
        final String scope;
        if (this.permission.allows(actor, this.request, true)) {
            if (this.permission.allows(actor, this.request, false)) {
                scope = "all";
            } else {
                scope = "school";
            }
        } else {
            scope = "denied";
        }
        return media.with("role", this.role).with(
            "title", ResourceBundle.getBundle("localization.roles", this.request.getLocale())
                .getString(this.role)
        ).with("scope", scope);
    }
}
