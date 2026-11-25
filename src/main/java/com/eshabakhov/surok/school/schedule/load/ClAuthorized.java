/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.security.Operations;

/**
 * Applied teaching load visible to members of its school.
 * For example, a student can inspect its class, subject, and hours.
 *
 * @since 0.0.1
 */
public final class ClAuthorized implements ClassLoad {

    /** Shared operation declarations. */
    private final Operations operations;

    /** Underlying teaching load. */
    private final ClassLoad origin;

    /** Owning school. */
    private final School school;

    /** Acting user. */
    private final UrAccount actor;

    /**
     * New protected object with shared operation declarations.
     *
     * @param origin Underlying origin
     * @param school Underlying school
     * @param actor Acting user
     * @param operations Shared operation declarations
     */
    public ClAuthorized(
        final ClassLoad origin, final School school, final UrAccount actor,
        final Operations operations
    ) {
        this.origin = origin;
        this.school = school;
        this.actor = actor;
        this.operations = operations;
    }

    @Override
    public Long uid() {
        return this.origin.uid();
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.operations.operation("ClassLoads.load")
            .permission(this.actor, this.school)
            .permit(this.origin).print(media);
    }
}
