/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.building.cabinet;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.security.right.RtAdmin;
import com.eshabakhov.surok.security.right.RtDirectorOrDeputy;
import com.eshabakhov.surok.security.right.RtEither;
import com.eshabakhov.surok.security.right.RtSchoolMember;

/**
 * Cabinet that checks the acting user's school role for each operation.
 * For example, {@code new CbAuthorized(cabinet, school, actor).revise(input)}
 * rejects a user who no longer has an editing role.
 *
 * @since 0.0.1
 */
public final class CbAuthorized implements Cabinet {

    /** Underlying cabinet. */
    private final Cabinet origin;

    /** School that owns the cabinet. */
    private final School school;

    /** Acting user. */
    private final User actor;

    /**
     * New protected cabinet.
     *
     * @param origin Underlying cabinet
     * @param school School that owns it
     * @param actor Acting user
     */
    public CbAuthorized(final Cabinet origin, final School school, final User actor) {
        this.origin = origin;
        this.school = school;
        this.actor = actor;
    }

    @Override
    public Long uid() {
        return this.origin.uid();
    }

    @Override
    public Cabinet revise(final CabinetInput input) throws Exception {
        return new CbAuthorized(
            new RtEither(
                new RtAdmin(this.actor), new RtDirectorOrDeputy(this.actor, this.school)
            ).permit(this.origin).revise(input),
            this.school, this.actor
        );
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return new RtEither(
            new RtAdmin(this.actor), new RtSchoolMember(this.actor, this.school)
        ).permit(this.origin).print(media);
    }
}
