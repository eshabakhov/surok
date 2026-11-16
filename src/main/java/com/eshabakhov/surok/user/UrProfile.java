/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.PrItems;
import com.eshabakhov.surok.Printable;
import com.eshabakhov.surok.User;
import java.util.Locale;

/**
 * Public account fields for the profile page, excluding passwords and email.
 *
 * @since 0.0.1
 */
public final class UrProfile implements Printable {

    /** Current user. */
    private final User origin;

    /**
     * New profile representation.
     *
     * @param origin Current user
     */
    public UrProfile(final User origin) {
        this.origin = origin;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        final String name = this.origin.account().username();
        String initial = "U";
        if (!name.isEmpty()) {
            initial = name.substring(0, name.offsetByCodePoints(0, 1)).toUpperCase(Locale.ROOT);
        }
        return media.with("id", this.origin.uid()).with("username", name)
            .with("initial", initial).with(
                "roles",
                new PrItems(
                    "role", this.origin.roles().list().stream().map(RlBrief::new).toList()
                )
            );
    }
}
