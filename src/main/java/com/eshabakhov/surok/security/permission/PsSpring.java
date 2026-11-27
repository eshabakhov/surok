/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.PrItems;
import com.eshabakhov.surok.Printable;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.security.Operation;
import com.eshabakhov.surok.security.Operations;
import com.eshabakhov.surok.user.Roles;
import com.eshabakhov.surok.user.UrAcRole;
import com.eshabakhov.surok.user.UrAnonymous;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Permissions evaluated for the selected audience.
 * For example, select the current account to filter its OpenAPI description.
 *
 * @since 0.0.1
 */
public final class PsSpring implements Permissions {

    /** Operations shared by Swagger and the matrix. */
    private final Operations catalog;

    /** Locale for role titles. */
    private final Locale locale;

    /** Operation evaluation for the selected audience. */
    private final Predicate<Operation> access;

    /**
     * New anonymous catalog.
     *
     * @param catalog Application operations
     * @param locale Locale for role titles
     */
    public PsSpring(final Operations catalog, final Locale locale) {
        this(catalog, locale, operation -> operation.permission(new UrAnonymous()).allows());
    }

    /**
     * New catalog with a bound audience.
     *
     * @param catalog Application operations
     * @param locale Locale for role titles
     * @param access Operation evaluation
     */
    private PsSpring(
        final Operations catalog, final Locale locale, final Predicate<Operation> access
    ) {
        this.catalog = catalog;
        this.locale = locale;
        this.access = access;
    }

    @Override
    public Permissions selection(final UrAccount actor) {
        return new PsSpring(
            this.catalog, this.locale,
            operation -> operation.permission(actor, actor.schools()).allows()
        );
    }

    @Override
    public boolean allows(final String path, final RequestMethod method) {
        return StreamSupport.stream(this.catalog.iterate().spliterator(), false).filter(
            operation -> path.equals(operation.path()) && method == operation.method()
        ).anyMatch(this.access);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        final List<Operation> operations = StreamSupport.stream(
            this.catalog.iterate().spliterator(), false
        ).filter(
            item -> item.path().matches("/|.*[^/]")
        ).filter(
            item -> item.method() != RequestMethod.GET
                || !item.path().matches(".*/(create|edit|fragment)$")
        ).toList();
        final List<Printable> rows = new ArrayList<>(operations.size());
        for (final Operation operation : operations) {
            final List<Printable> cells = new ArrayList<>(Roles.RoleEnum.values().length);
            for (final Roles.RoleEnum role : Roles.RoleEnum.values()) {
                final Permission own;
                final Permission external;
                if (role == Roles.RoleEnum.ANONYMOUS) {
                    own = operation.permission(new UrAnonymous());
                    external = own;
                } else {
                    final UrAccount actor = new UrAcRole(1L, role);
                    own = operation.permission(actor, actor.schools());
                    external = operation.permission(actor, new UrAcRole(2L, role).schools());
                }
                final boolean allowed = own.allows();
                final String scope;
                if (allowed && external.allows()) {
                    scope = "all";
                } else if (allowed) {
                    scope = "school";
                } else {
                    scope = "denied";
                }
                cells.add(
                    new Printable() {
                        @Override
                        public <V> Media<V> print(final Media<V> target) {
                            return target.with("role", role.name()).with(
                                "title", ResourceBundle.getBundle(
                                    "localization.roles", PsSpring.this.locale
                                ).getString(role.name())
                            ).with("scope", scope);
                        }
                    }
                );
            }
            rows.add(
                new Printable() {
                    @Override
                    public <V> Media<V> print(final Media<V> target) throws Exception {
                        return operation.print(target).with("access", new PrItems("grant", cells));
                    }
                }
            );
        }
        return media.with("operations", new PrItems("operation", rows));
    }
}
