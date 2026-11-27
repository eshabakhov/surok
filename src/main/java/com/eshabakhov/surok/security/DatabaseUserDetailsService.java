/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security;

import com.eshabakhov.surok.user.UrAcAuth;
import com.eshabakhov.surok.user.UrsPostgres;
import java.io.IOException;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Database user details service for Spring Security.
 *
 * <p>Loads user details from PostgreSQL database.</p>
 *
 * @since 0.0.1
 */
@Service
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public final class DatabaseUserDetailsService implements UserDetailsService {

    /** Database context. */
    private final DSLContext ctx;

    /** Shared application operations. */
    private final Operations operations;

    /**
     * New authentication service using the packaged operation catalog.
     *
     * @param ctx Database context
     * @throws IOException If the operation catalog cannot be read
     */
    @Autowired
    DatabaseUserDetailsService(final DSLContext ctx) throws IOException {
        this(ctx, new OaYaml().operations());
    }

    /**
     * New authentication service with explicit operations.
     *
     * @param ctx Database context
     * @param operations Application operations
     */
    DatabaseUserDetailsService(final DSLContext ctx, final Operations operations) {
        this.ctx = ctx;
        this.operations = operations;
    }

    @Override
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        try {
            return new UrAcAuth(
                new UrsPostgres(this.ctx).authentication(username), this.operations
            );
            //@checkstyle IllegalCatch (1 line)
        } catch (final Exception ex) {
            throw new UsernameNotFoundException(String.format("User '%s' not found", username), ex);
        }
    }
}
