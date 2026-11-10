/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum;

import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.Sorts;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.security.right.RtAdmin;
import java.util.stream.StreamSupport;

/**
 * Federal curriculums whose mutations require an administrator.
 * For example, {@code new CrsAuthorized(curriculums, user).create(input)}
 * rejects a user without the global administrator role.
 *
 * @since 0.0.1
 */
public final class CrsAuthorized implements Curriculums {

    /** Underlying curriculums. */
    private final Curriculums origin;

    /** Acting user. */
    private final User actor;

    /**
     * New protected federal curriculum collection.
     *
     * @param origin Underlying curriculums
     * @param actor Acting user
     */
    public CrsAuthorized(final Curriculums origin, final User actor) {
        this.origin = origin;
        this.actor = actor;
    }

    @Override
    public Curriculum create(final CurriculumInput input) throws Exception {
        return new CrAuthorized(
            new RtAdmin(this.actor).permit(this.origin).create(input), this.actor
        );
    }

    @Override
    public Curriculum curriculum(final long id) throws Exception {
        return new CrAuthorized(this.origin.curriculum(id), this.actor);
    }

    @Override
    public Curriculum revise(final long id, final CurriculumInput input) throws Exception {
        return new CrAuthorized(
            new RtAdmin(this.actor).permit(this.origin).revise(id, input), this.actor
        );
    }

    @Override
    public Curriculums selection(
        final Filters filters, final Page page, final Sorts sorts
    ) throws Exception {
        return new CrsAuthorized(this.origin.selection(filters, page, sorts), this.actor);
    }

    @Override
    public Iterable<Curriculum> iterate() {
        return StreamSupport.stream(this.origin.iterate().spliterator(), false)
            .<Curriculum>map(curriculum -> new CrAuthorized(curriculum, this.actor)).toList();
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
