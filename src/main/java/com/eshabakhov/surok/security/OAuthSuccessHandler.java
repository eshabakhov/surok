/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security;

import com.eshabakhov.surok.user.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * OAuth2 login success handler for Yandex.
 *
 * <p>At this point {@link YandexOAuth2UserService} has already resolved the
 * principal to an {@link AuthUser}, so this handler applies the same redirect
 * rule as {@link LoginSuccessHandler}: everyone goes to /schools.</p>
 * Usage example:
 * <pre>
 * // Wired automatically by Spring Security via SecurityConfig.
 * </pre>
 *
 * @since 0.0.1
 */
@Component
public final class OAuthSuccessHandler implements AuthenticationSuccessHandler {

    /**
     * New OAuth success handler.
     *
     * @since 0.0.1
     */
    public OAuthSuccessHandler() {
        // Intentionally empty.
    }

    @Override
    public void onAuthenticationSuccess(
        final HttpServletRequest request,
        final HttpServletResponse response,
        final Authentication authentication
    ) throws IOException {
        response.sendRedirect("/schools");
    }
}
