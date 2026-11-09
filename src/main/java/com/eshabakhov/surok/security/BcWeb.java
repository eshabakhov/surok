/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationConverter;

/**
 * Basic credentials for API requests without an established website session.
 *
 * @since 0.0.1
 */
public final class BcWeb implements AuthenticationConverter {

    /** Basic credentials converter. */
    private final AuthenticationConverter origin;

    /**
     * New website-aware converter.
     *
     * @param origin Basic credentials converter
     */
    public BcWeb(final AuthenticationConverter origin) {
        this.origin = origin;
    }

    @Override
    public Authentication convert(final HttpServletRequest request) {
        boolean browser = false;
        final String accept = request.getHeader(HttpHeaders.ACCEPT);
        if (accept != null) {
            try {
                browser = MediaType.parseMediaTypes(accept).stream().anyMatch(
                    wanted -> !wanted.isWildcardType()
                        && wanted.isCompatibleWith(MediaType.TEXT_HTML)
                        && wanted.getQualityValue() > 0.0
                );
            } catch (final InvalidMediaTypeException ignored) {
                browser = false;
            }
        }
        Authentication result = null;
        if (!browser && SecurityContextHolder.getContext().getAuthentication() == null) {
            result = this.origin.convert(request);
        }
        return result;
    }
}
