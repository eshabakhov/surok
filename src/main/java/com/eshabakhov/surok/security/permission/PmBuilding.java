/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Printable;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.Schools;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.security.right.Right;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * A documented operation backed by the same action as the protected building.
 * For example, buildings.create checks creation rights across the supplied schools.
 *
 * @since 0.0.1
 */
final class PmBuilding implements Permission, Printable {

    /** Configured grants for this operation. */
    private final Permission grant;

    /** Path template in the specification. */
    private final String path;

    /** Supported request body formats. */
    private final String consumes;

    /** Domain action bound to the identifier. */
    private final String operation;

    /**
     * New building operation from the OpenAPI contract.
     *
     * @param path Path template
     * @param operation Operation identifier declared in the specification
     * @param grant Configured grants
     * @param consumes Accepted request media types
     */
    PmBuilding(
        final String path, final String operation,
        final Permission grant, final String consumes
    ) {
        this.path = path;
        this.grant = grant;
        this.operation = operation;
        this.consumes = consumes;
    }

    @Override
    public Right right() {
        return this.grant.right();
    }

    @Override
    public Right right(final UrAccount actor, final School school) {
        return this.grant.right(actor, school);
    }

    @Override
    public Right right(final UrAccount actor, final Schools schools) {
        return this.grant.right(actor, schools);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) {
        final ResourceBundle labels = ResourceBundle.getBundle(
            "localization.permissions", Locale.ROOT
        );
        return media.with("resource", labels.getString("buildings"))
            .with("action", labels.getString(this.operation)).with("path", this.path)
            .with("consumes", this.consumes).with("condition", "");
    }
}
