/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject;

import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.Sorts;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.security.right.RtAdmin;
import java.util.stream.StreamSupport;

/**
 * Federal subject catalogue with administrator-only mutations.
 * For example, {@code new SbsFederalAuthorized(subjects, actor).create(input)}
 * checks the actor's current global administrator role.
 *
 * @since 0.0.1
 */
public final class SbsFederalAuthorized implements Subjects {

    /** Underlying federal catalogue. */
    private final Subjects origin;

    /** Acting user. */
    private final User actor;

    /**
     * New protected federal catalogue.
     *
     * @param origin Underlying federal catalogue
     * @param actor Acting user
     */
    public SbsFederalAuthorized(final Subjects origin, final User actor) {
        this.origin = origin;
        this.actor = actor;
    }

    @Override
    public Subject create(final SubjectInput input) throws Exception {
        return new SbFederalAuthorized(
            new RtAdmin(this.actor).permit(this.origin).create(input), this.actor
        );
    }

    @Override
    public Subject subject(final long id) throws Exception {
        return new SbFederalAuthorized(this.origin.subject(id), this.actor);
    }

    @Override
    public Subject revise(final long id, final SubjectInput input) throws Exception {
        return new SbFederalAuthorized(
            new RtAdmin(this.actor).permit(this.origin).revise(id, input), this.actor
        );
    }

    @Override
    public Subjects selection(
        final Filters filters, final Page page, final Sorts sorts
    ) throws Exception {
        return new SbsFederalAuthorized(this.origin.selection(filters, page, sorts), this.actor);
    }

    @Override
    public Iterable<Subject> iterate() {
        return StreamSupport.stream(this.origin.iterate().spliterator(), false)
            .<Subject>map(subject -> new SbFederalAuthorized(subject, this.actor)).toList();
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
