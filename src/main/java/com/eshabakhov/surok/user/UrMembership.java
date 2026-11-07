/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.PrItems;
import com.eshabakhov.surok.Printable;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.User;

/**
 * One school and the user's roles in it for a profile representation.
 *
 * @since 0.0.1
 */
public final class UrMembership implements Printable {

    /** School. */
    private final School school;

    /** User. */
    private final User user;

    /**
     * New membership.
     *
     * @param school School
     * @param user User
     */
    public UrMembership(final School school, final User user) {
        this.school = school;
        this.user = user;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return media.with("school", this.school).with("schedules", this.school.schedules()).with(
            "roles", new PrItems(
                "role", this.school.users().roles(this.user).list().stream()
                    .map(RlBrief::new).toList()
            )
        );
    }
}
