/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security;

import com.eshabakhov.surok.user.AuthUser;
import com.eshabakhov.surok.user.UrsPostgres;
import com.eshabakhov.surok.user.UserNotFoundException;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * OAuth2 user service for Yandex.
 *
 * <p>After the standard token exchange, fetches the Yandex user-info response,
 * then looks up (or creates) a matching user in the database and returns an
 * {@link AuthUser} as the principal. Because {@link AuthUser} implements both
 * {@link org.springframework.security.core.userdetails.UserDetails} and
 * {@link OAuth2User}, controllers can declare
 * {@code @AuthenticationPrincipal AuthUser} without any casting or special
 * handling - it works identically for form-login and OAuth2 sessions.</p>
 * Usage example:
 * <pre>
 * // Wired automatically via SecurityConfig.oauth2Login()
 * //   .userInfoEndpoint().userService(yandexOAuth2UserService)
 * </pre>
 *
 * @since 0.0.1
 */
@Service
@SuppressWarnings({
    "PMD.AvoidCatchingGenericException",
    "PMD.PreserveStackTrace"
    })
public final class YandexOAuth2UserService extends DefaultOAuth2UserService {

    /** Database context. */
    private final DSLContext ctx;

    /** User details service to load AuthUser by username. */
    private final DatabaseUserDetailsService userdetails;

    YandexOAuth2UserService(
        final DSLContext ctx,
        final DatabaseUserDetailsService userdetails
    ) {
        this.ctx = ctx;
        this.userdetails = userdetails;
    }

    @Override
    public OAuth2User loadUser(
        final OAuth2UserRequest request
    ) throws OAuth2AuthenticationException {
        final OAuth2User yandex = super.loadUser(request);
        final String email = yandex.getAttribute("default_email");
        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error(
                    "missing_email",
                    "Yandex did not return an email address",
                    null
                )
            );
        }
        try {
            return this.resolve(email);
        // @checkstyle IllegalCatchCheck (1 line)
        } catch (final Exception ex) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error("user_resolve_failed", ex.getMessage(), null)
            );
        }
    }

    private AuthUser resolve(final String email) throws Exception {
        final UrsPostgres urs = new UrsPostgres(this.ctx);
        String username;
        try {
            username = urs.identification(email).account().username();
        } catch (final UserNotFoundException ignored) {
            username = urs.register(
                String.format(
                    "%s_%s",
                    email.substring(0, email.indexOf('@')),
                    UUID.randomUUID().toString().replace("-", "").substring(0, 6)
                ),
                String.format(
                    "Ya_1!A_%s",
                    UUID.randomUUID().toString().replace("-", "")
                ),
                email,
                false
                ).account()
                .username();
        }
        return (AuthUser) this.userdetails.loadUserByUsername(username);
    }
}
