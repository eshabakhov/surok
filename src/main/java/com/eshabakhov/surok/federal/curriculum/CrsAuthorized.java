/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum;

import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.Sorts;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.security.Operations;
import java.util.stream.StreamSupport;

/**
 * Federal curriculums whose mutations require an administrator.
 * For example, {@code new CrsAuthorized(curriculums, user, operations).create(input)}
 * rejects a user without the global administrator role.
 *
 * @since 0.0.1
 */
public final class CrsAuthorized implements Curriculums {

    /** Shared operation declarations. */
    private final Operations operations;

    /** Underlying curriculums. */
    private final Curriculums origin;

    /** Acting user. */
    private final UrAccount actor;

    /**
     * New protected object with shared operation declarations.
     *
     * @param origin Underlying origin
     * @param actor Acting user
     * @param operations Shared operation declarations
     */
    public CrsAuthorized(
        final Curriculums origin, final UrAccount actor,
        final Operations operations
    ) {
        this.origin = origin;
        this.actor = actor;
        this.operations = operations;
    }

    @Override
    public Curriculum create(final CurriculumInput input) throws Exception {
        return new CrAuthorized(
            this.operations.operation("Curriculums.create")
                .permission(this.actor)
                .permit(this.origin).create(input), this.actor, this.operations
        );
    }

    @Override
    public Curriculum curriculum(final long id) throws Exception {
        return new CrAuthorized(
            this.operations.operation("Curriculums.curriculum")
                .permission(this.actor)
                .permit(this.origin).curriculum(id), this.actor, this.operations
        );
    }

    @Override
    public Curriculum revise(final long id, final CurriculumInput input) throws Exception {
        return new CrAuthorized(
            this.operations.operation("Curriculums.revise")
                .permission(this.actor)
                .permit(this.origin).revise(id, input), this.actor, this.operations
        );
    }

    @Override
    public Curriculums selection(
        final Filters filters, final Page page, final Sorts sorts
    ) throws Exception {
        return new CrsAuthorized(
            this.operations.operation("Curriculums.selection")
                .permission(this.actor)
                .permit(this.origin).selection(filters, page, sorts), this.actor, this.operations
        );
    }

    @Override
    public Iterable<Curriculum> iterate() {
        return StreamSupport.stream(
            this.operations.operation("Curriculums.selection")
                .permission(this.actor)
                .permit(this.origin).iterate().spliterator(), false
        ).<Curriculum>map(
            curriculum -> new CrAuthorized(
                curriculum, this.actor, this.operations
            )
        ).toList();
    }

    @Override
    public void remove(final long id) throws Exception {
        this.operations.operation("Curriculums.remove")
            .permission(this.actor)
            .permit(this.origin).remove(id);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.operations.operation("Curriculums.selection")
            .permission(this.actor)
            .permit(this.origin).print(media);
    }
}
