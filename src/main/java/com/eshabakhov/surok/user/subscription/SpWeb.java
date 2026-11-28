/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user.subscription;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Printable;
import com.eshabakhov.surok.user.Subscription;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Subscription status and display values for the browser.
 *
 * @since 0.0.1
 */
public final class SpWeb implements Printable {

    /** Current subscription. */
    private final Subscription origin;

    /**
     * New subscription representation.
     *
     * @param origin Current subscription
     */
    public SpWeb(final Subscription origin) {
        this.origin = origin;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) {
        final Subscription.Plan plan = this.origin.plan();
        String expiry = "";
        if (!plan.free()) {
            expiry = this.origin.expiry().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        }
        return media.with("plan", plan.name()).with("label", plan.label())
            .attribute("style", plan.name().toLowerCase(Locale.ROOT))
            .with("valid", this.origin.valid()).with("expiry", expiry);
    }
}
