/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security;

import com.eshabakhov.surok.User;
import com.eshabakhov.surok.sql.ClasspathSql;
import com.eshabakhov.surok.user.UrsPostgres;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import org.jooq.DSLContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * Handler for success login.
 *
 * @since 0.0.1
 */
@Component
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public final class LoginSuccessHandler implements AuthenticationSuccessHandler {

    /** Database context. */
    private final DSLContext ctx;

    LoginSuccessHandler(final DSLContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void onAuthenticationSuccess(
        final HttpServletRequest request,
        final HttpServletResponse response,
        final Authentication authentication
    ) throws IOException {
        final User user;
        try {
            user = new UrsPostgres(this.ctx).identification(authentication.getName());
        // @checkstyle IllegalCatchCheck (1 line)
        } catch (final Exception ex) {
            throw new IOException(ex);
        }
        this.ctx.resultQuery(
            new ClasspathSql("login-attempt/create.sql").text(),
            user.uid(),
            Instant.now().atOffset(ZoneOffset.UTC),
            true
        ).execute();
        response.sendRedirect("/schools");
    }
}
