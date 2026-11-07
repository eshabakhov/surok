/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security;

import com.eshabakhov.surok.error.ErHttp;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Security errors use the same representation as controller and servlet errors.
 *
 * @since 0.0.1
 */
public final class ScError implements AuthenticationEntryPoint, AccessDeniedHandler {

    /** New security error handler. */
    public ScError() {
        // Intentionally empty.
    }

    @Override
    public void commence(
        final HttpServletRequest request, final HttpServletResponse response,
        final AuthenticationException exception
    ) throws IOException {
        if (!"XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Surok\"");
        }
        ScError.write(request, response, HttpStatus.UNAUTHORIZED);
    }

    @Override
    public void handle(
        final HttpServletRequest request, final HttpServletResponse response,
        final AccessDeniedException exception
    ) throws IOException {
        ScError.write(request, response, HttpStatus.FORBIDDEN);
    }

    private static void write(
        final HttpServletRequest request, final HttpServletResponse response,
        final HttpStatus status
    ) throws IOException {
        final ResponseEntity<String> error = new ErHttp(request, status).response();
        response.setStatus(error.getStatusCode().value());
        response.setCharacterEncoding("UTF-8");
        error.getHeaders().forEach(
            (name, values) -> values.forEach(value -> response.addHeader(name, value))
        );
        if (error.getBody() != null) {
            response.getWriter().write(error.getBody());
        }
    }
}
