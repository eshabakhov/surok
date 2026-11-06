/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import java.util.Objects;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.util.MultiValueMap;

/**
 * Personal registration that creates an account without a school.
 * Checked validation failures are unwrapped from transaction exceptions.
 *
 * @since 0.0.1
 */
public final class UrSignup {

    /** Database context. */
    private final DSLContext ctx;

    /** Submitted form. */
    private final MultiValueMap<String, String> fields;

    /**
     * New registration.
     *
     * @param ctx Database context
     * @param fields Submitted fields
     */
    public UrSignup(final DSLContext ctx, final MultiValueMap<String, String> fields) {
        this.ctx = ctx;
        this.fields = fields;
    }

    /**
     * Create a personal user.
     *
     * @throws Exception If validation or creation fails
     */
    @SuppressWarnings("PMD.PreserveStackTrace")
    public void register() throws Exception {
        final String username = Objects.toString(this.fields.getFirst("username"), "").trim();
        final String password = Objects.toString(this.fields.getFirst("password"), "");
        if (username.length() < 3) {
            throw new RegistrationException("Логин должен содержать минимум 3 символа");
        }
        if (!password.equals(this.fields.getFirst("confirmPassword"))) {
            throw new RegistrationException("Пароли не совпадают");
        }
        try {
            this.ctx.transaction(
                config ->
                    new UrsPostgres(DSL.using(config)).register(
                        username, password,
                        Objects.toString(this.fields.getFirst("email"), "").trim(), false
                    )
            );
        } catch (final DuplicateKeyException exception) {
            throw new RegistrationException(
                "Пользователь с таким логином или email уже существует", exception
            );
        } catch (final DataAccessException exception) {
            if ("23505".equals(exception.sqlState())) {
                throw new RegistrationException(
                    "Пользователь с таким логином или email уже существует", exception
                );
            }
            switch (exception.getCause()) {
                case UserCreationException invalid -> throw invalid;
                case null, default -> throw exception;
            }
        }
    }
}
