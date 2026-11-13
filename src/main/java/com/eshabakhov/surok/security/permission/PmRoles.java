/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.PrItems;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.user.Roles;
import com.eshabakhov.surok.user.UrAnonymous;
import com.eshabakhov.surok.user.UrRole;
import java.util.Arrays;
import java.util.Locale;
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
     * @param locale Locale for role titles
     */
    public PmRoles(
        final Permission origin, final RequestMethod method, final Locale locale
    ) {
        this(
            origin,
            method,
            Arrays.stream(Roles.RoleEnum.values()).<Grant>map(
                role -> {
                    final User actor;
                    if (role == Roles.RoleEnum.ANONYMOUS) {
                        actor = new UrAnonymous();
                    } else {
                        actor = new UrRole(role);
                    }
                    return new GrRole(origin, actor, role, locale);
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
    public boolean allows(final User actor, final boolean own) {
        return this.origin.allows(actor, own);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.origin.print(media)
            .with("method", this.method.name()).with(
            "access", new PrItems("grant", this.grants)
        );
    }
}
