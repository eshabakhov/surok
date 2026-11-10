/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum.requirement;

import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.Sorts;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.security.right.RtAdmin;
import java.util.stream.StreamSupport;

/**
 * Federal requirements whose mutations require an administrator.
 * For example, {@code new RqsAuthorized(requirements, user).create(input)}
 * refuses a regular school user.
 *
 * @since 0.0.1
 */
public final class RqsAuthorized implements Requirements {

    /** Underlying requirements. */
    private final Requirements origin;

    /** Acting user. */
    private final User actor;

    /**
     * New protected requirements.
     *
     * @param origin Underlying requirements
     * @param actor Acting user
     */
    public RqsAuthorized(final Requirements origin, final User actor) {
        this.origin = origin;
        this.actor = actor;
    }

    @Override
    public Iterable<Requirement> optional(final int grade) throws Exception {
        return StreamSupport.stream(this.origin.optional(grade).spliterator(), false)
            .<Requirement>map(requirement -> new RqAuthorized(requirement, this.actor)).toList();
    }

    @Override
    public void check(final int grade, final int hours) throws Exception {
        this.origin.check(grade, hours);
    }

    @Override
    public Requirement create(final RequirementInput input) throws Exception {
        return new RqAuthorized(
            new RtAdmin(this.actor).permit(this.origin).create(input), this.actor
        );
    }

    @Override
    public Requirement requirement(final long id) throws Exception {
        return new RqAuthorized(this.origin.requirement(id), this.actor);
    }

    @Override
    public Requirement revise(final long id, final RequirementInput input) throws Exception {
        return new RqAuthorized(
            new RtAdmin(this.actor).permit(this.origin).revise(id, input), this.actor
        );
    }

    @Override
    public Requirements selection(
        final Filters filters, final Page page, final Sorts sorts
    ) throws Exception {
        return new RqsAuthorized(this.origin.selection(filters, page, sorts), this.actor);
    }

    @Override
    public Iterable<Requirement> iterate() {
        return StreamSupport.stream(this.origin.iterate().spliterator(), false)
            .<Requirement>map(requirement -> new RqAuthorized(requirement, this.actor)).toList();
    }

    @Override
    public void remove(final long id) throws Exception {
        new RtAdmin(this.actor).permit(this.origin).remove(id);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.origin.print(media);
    }
}
