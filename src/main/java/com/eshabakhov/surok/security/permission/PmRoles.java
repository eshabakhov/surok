/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.PrItems;
import com.eshabakhov.surok.Printable;
import com.eshabakhov.surok.user.Roles;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.stream.Stream;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * An operation decorated with access for every application role and anonymous users.
 * For example, print the POST cabinet route as one matrix row.
 *
 * @since 0.0.1
 */
public final class PmRoles implements Printable {

    /** Controller permission. */
    private final Permission origin;

    /** HTTP method. */
    private final RequestMethod method;

    /** Current request. */
    private final HttpServletRequest request;

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
        this.origin = origin;
        this.method = method;
        this.request = request;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.origin.print(media).with("method", this.method.name()).with(
            "access", new PrItems(
                "grant", Stream.concat(
                    Arrays.stream(Roles.RoleEnum.values()).map(Enum::name), Stream.of("ANONYMOUS")
                ).map(role -> new PaRole(this.origin, role, this.request)).toList()
            )
        );
    }
}
