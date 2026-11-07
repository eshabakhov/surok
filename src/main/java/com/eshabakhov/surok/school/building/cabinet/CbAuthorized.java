/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.building.cabinet;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.user.Roles;
import org.springframework.security.access.AccessDeniedException;

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
        if (!this.actor.isAdmin() && (!this.school.users().contains(this.actor)
            || !this.school.users().roles(this.actor).contains(Roles.RoleEnum.DIRECTOR)
            && !this.school.users().roles(this.actor).contains(Roles.RoleEnum.DEPUTY_DIRECTOR))) {
            throw new AccessDeniedException(
                "Only a school director or deputy can revise a cabinet"
            );
        }
        return new CbAuthorized(this.origin.revise(input), this.school, this.actor);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        if (!this.actor.isAdmin() && !this.school.users().contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view a cabinet");
        }
        return this.origin.print(media);
    }
}
