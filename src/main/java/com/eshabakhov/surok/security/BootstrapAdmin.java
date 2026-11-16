/*
 * В© 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security;

import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.sql.ClasspathSql;
import com.eshabakhov.surok.user.Roles;
import com.eshabakhov.surok.user.UrsPostgres;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Creates the first admin when explicitly enabled.
 *
 * @since 0.0.1
 */
@Component
public final class BootstrapAdmin implements ApplicationRunner {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(BootstrapAdmin.class);

    /**
     * Database context.
     */
    private final DSLContext ctx;

    /**
     * Environment properties.
     */
    private final Environment env;

    /**
     * Ctor.
     *
     * @param ctx Database context
     * @param env Environment properties
     */
    public BootstrapAdmin(final DSLContext ctx, final Environment env) {
        this.ctx = ctx;
        this.env = env;
    }

    @Override
    public void run(final ApplicationArguments args) throws Exception {
        if (this.bootstrapEnabled()) {
            if (this.hasActiveAdmin()) {
                BootstrapAdmin.LOG.info("Bootstrap admin skipped: active admin already exists");
            } else {
                this.validateConfig();
                final UrsPostgres users = new UrsPostgres(this.ctx);
                final UrAccount admin = users.register(
                    this.username(),
                    this.password(),
                    this.email(),
                    false
                );
                admin.roles().grant(Roles.RoleEnum.ADMIN);
                BootstrapAdmin.LOG.info(
                    "Bootstrap admin created for username '{}' and email '{}'",
                    this.username(),
                    this.email()
                );
            }
        }
    }

    private void validateConfig() {
        if (this.username().isBlank()) {
            throw new IllegalStateException("Bootstrap admin username is required");
        }
        if (this.email().isBlank()) {
            throw new IllegalStateException("Bootstrap admin email is required");
        }
        if (this.password().isBlank()) {
            throw new IllegalStateException("Bootstrap admin password is required");
        }
    }

    private boolean bootstrapEnabled() {
        return Boolean.parseBoolean(this.env.getProperty("app.bootstrap-admin.enabled", "false"));
    }

    private String username() {
        return this.env.getProperty("app.bootstrap-admin.username", "");
    }

    private String email() {
        return this.env.getProperty("app.bootstrap-admin.email", "");
    }

    private String password() {
        return this.env.getProperty("app.bootstrap-admin.password", "");
    }

    private boolean hasActiveAdmin() {
        return this.ctx.resultQuery(
            new ClasspathSql("user/role/has-active-admin.sql").text(),
            "ADMIN"
        ).fetchSingle(0, Boolean.class);
    }
}
