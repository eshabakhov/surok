/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.PrItems;
import com.eshabakhov.surok.user.Roles;
import com.eshabakhov.surok.user.UrAcAuth;
import com.eshabakhov.surok.user.UrAcRole;
import com.eshabakhov.surok.user.UrAnonymous;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * An operation decorated with access for every application role and anonymous users.
 * For example, print the POST cabinet route as one matrix row.
 *
 * @since 0.0.1
 */
public final class PmRoles implements Permission {

    /** Controller permission. */
    private final Permission origin;

    /** HTTP method. */
    private final RequestMethod method;

    /** Role access entries. */
    private final Iterable<Grant> grants;

    /**
     * New matrix row.
     *
     * @param origin Controller permission
     * @param method HTTP method
     * @param request Current request
     */
    public PmRoles(
        final Permission origin, final RequestMethod method, final HttpServletRequest request
    ) {
        this(
            origin,
            method,
            Arrays.stream(Roles.RoleEnum.values()).<Grant>map(
                role -> {
                    final Authentication actor;
                    if (role == Roles.RoleEnum.ANONYMOUS) {
                        actor = new AnonymousAuthenticationToken(
                            "permissions", new UrAnonymous(),
                            List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
                        );
                    } else {
                        final UrAcAuth user = new UrAcAuth(new UrAcRole(role));
                        actor = UsernamePasswordAuthenticationToken.authenticated(
                            user, "", user.getAuthorities()
                        );
                    }
                    return new GrRole(
                        () -> origin.allows(actor, request, true),
                        () -> origin.allows(actor, request, false),
                        role,
                        request.getLocale()
                    );
                }
            ).toList()
        );
    }

    /**
     * New matrix row with assembled access entries.
     *
     * @param origin Controller permission
     * @param method HTTP method
     * @param grants Role access entries
     */
    private PmRoles(
        final Permission origin,
        final RequestMethod method,
        final Iterable<Grant> grants
    ) {
        this.origin = origin;
        this.method = method;
        this.grants = grants;
    }

    @Override
    public boolean allows(
        final Authentication actor, final HttpServletRequest context, final boolean own
    ) {
        return this.origin.allows(actor, context, own);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.origin.print(media)
            .with("method", this.method.name()).with(
            "access", new PrItems("grant", this.grants)
        );
    }
}
