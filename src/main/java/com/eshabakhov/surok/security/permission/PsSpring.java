/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.PrItems;
import com.eshabakhov.surok.UrAccount;
import java.util.Locale;
import java.util.function.Predicate;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Permissions evaluated for the selected audience.
 * For example, select the current account to filter its OpenAPI description.
 *
 * @since 0.0.1
 */
public final class PsSpring implements Permissions {

    /** Operations shared by Swagger and the matrix. */
    private final PmCatalog catalog;

    /** Locale for role titles. */
    private final Locale locale;

    /** Operation evaluation for the selected audience. */
    private final Predicate<Permission> access;

    /**
     * New anonymous catalog.
     *
     * @param catalog Application operations
     * @param locale Locale for role titles
     */
    public PsSpring(final PmCatalog catalog, final Locale locale) {
        this(catalog, locale, permission -> permission.right().allows());
    }

    /**
     * New catalog with a bound audience.
     *
     * @param catalog Application operations
     * @param locale Locale for role titles
     * @param access Operation evaluation
     */
    private PsSpring(
        final PmCatalog catalog, final Locale locale, final Predicate<Permission> access
    ) {
        this.catalog = catalog;
        this.locale = locale;
        this.access = access;
    }

    @Override
    public Permissions selection(final UrAccount actor) {
        return new PsSpring(
            this.catalog, this.locale,
            permission -> permission.right(actor, actor.schools()).allows()
        );
    }

    @Override
    public boolean allows(final String path, final RequestMethod method) {
        return this.catalog.matching(path, method).anyMatch(this.access);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return media.with("operations", new PrItems("operation", this.catalog.rows(this.locale)));
    }
}
