/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.building.cabinet;

import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.Sorts;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.user.Roles;
import java.util.stream.StreamSupport;
import org.springframework.security.access.AccessDeniedException;

/**
 * Building cabinets protected by the acting user's school role.
 * For example, {@code new CbsAuthorized(building.cabinets(), school, actor).create(input)}
 * permits creation by a director or deputy director.
 *
 * @since 0.0.1
 */
public final class CbsAuthorized implements Cabinets {

    /** Underlying cabinets. */
    private final Cabinets origin;

    /** School that owns the cabinets. */
    private final School school;

    /** Acting user. */
    private final User actor;

    /**
     * New protected cabinet collection.
     *
     * @param origin Underlying cabinets
     * @param school School that owns them
     * @param actor Acting user
     */
    public CbsAuthorized(final Cabinets origin, final School school, final User actor) {
        this.origin = origin;
        this.school = school;
        this.actor = actor;
    }

    @Override
    public Cabinet create(final CabinetInput input) throws Exception {
        if (!this.actor.isAdmin() && (!this.school.users().contains(this.actor)
            || !this.school.users().roles(this.actor).contains(Roles.RoleEnum.DIRECTOR)
            && !this.school.users().roles(this.actor).contains(Roles.RoleEnum.DEPUTY_DIRECTOR))) {
            throw new AccessDeniedException(
                "Only a school director or deputy can create a cabinet"
            );
        }
        return new CbAuthorized(this.origin.create(input), this.school, this.actor);
    }

    @Override
    public Cabinet cabinet(final long id) throws Exception {
        if (!this.actor.isAdmin() && !this.school.users().contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view a cabinet");
        }
        return new CbAuthorized(this.origin.cabinet(id), this.school, this.actor);
    }

    @Override
    public Cabinets selection(
        final Filters filters, final Page page, final Sorts sorts
    ) throws Exception {
        if (!this.actor.isAdmin() && !this.school.users().contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view cabinets");
        }
        return new CbsAuthorized(
            this.origin.selection(filters, page, sorts), this.school, this.actor
        );
    }

    @Override
    public Iterable<Cabinet> iterate() {
        if (!this.actor.isAdmin() && !this.school.users().contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view cabinets");
        }
        return StreamSupport.stream(
            this.origin.iterate().spliterator(), false
        ).<Cabinet>map(
            cabinet -> new CbAuthorized(cabinet, this.school, this.actor)
        ).toList();
    }

    @Override
    public void remove(final long id) throws Exception {
        if (!this.actor.isAdmin() && (!this.school.users().contains(this.actor)
            || !this.school.users().roles(this.actor).contains(Roles.RoleEnum.DIRECTOR)
            && !this.school.users().roles(this.actor).contains(Roles.RoleEnum.DEPUTY_DIRECTOR))) {
            throw new AccessDeniedException(
                "Only a school director or deputy can remove a cabinet"
            );
        }
        this.origin.remove(id);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        if (!this.actor.isAdmin() && !this.school.users().contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view cabinets");
        }
        return this.origin.print(media);
    }
}
