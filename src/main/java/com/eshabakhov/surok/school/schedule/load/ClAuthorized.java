/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.User;
import org.springframework.security.access.AccessDeniedException;

/**
 * Applied teaching load visible to members of its school.
 * For example, a viewer can inspect its class, subject, and hours.
 *
 * @since 0.0.1
 */
public final class ClAuthorized implements ClassLoad {

    /** Underlying teaching load. */
    private final ClassLoad origin;

    /** Owning school. */
    private final School school;

    /** Acting user. */
    private final User actor;

    /**
     * New protected teaching load.
     *
     * @param origin Underlying load
     * @param school Owning school
     * @param actor Acting user
     */
    public ClAuthorized(final ClassLoad origin, final School school, final User actor) {
        this.origin = origin;
        this.school = school;
        this.actor = actor;
    }

    @Override
    public Long uid() {
        return this.origin.uid();
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        if (!this.actor.isAdmin() && !this.school.users().contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view a teaching load");
        }
        return this.origin.print(media);
    }
}
