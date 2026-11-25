/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schoolclass;

import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.Sorts;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.security.Operations;
import java.util.stream.StreamSupport;

/**
 * School classes protected by the acting user's current school role.
 * For example, a director can create a class and a student can list classes.
 *
 * @since 0.0.1
 */
public final class ScsAuthorized implements SchoolClasses {

    /** Shared operation declarations. */
    private final Operations operations;

    /** Underlying classes. */
    private final SchoolClasses origin;

    /** School that owns the classes. */
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
    public ScsAuthorized(
        final SchoolClasses origin, final School school, final UrAccount actor,
        final Operations operations
    ) {
        this.origin = origin;
        this.school = school;
        this.actor = actor;
        this.operations = operations;
    }

    @Override
    public SchoolClass create(final SchoolClassInput input) throws Exception {
        return new ScAuthorized(
            this.operations.operation("SchoolClasses.create")
                .permission(this.actor, this.school)
                .permit(this.origin).create(input),
            this.school, this.actor, this.operations
        );
    }

    @Override
    public SchoolClass clazz(final long id) throws Exception {
        return new ScAuthorized(
            this.operations.operation("SchoolClasses.clazz")
                .permission(this.actor, this.school)
                .permit(this.origin).clazz(id),
            this.school, this.actor, this.operations
        );
    }

    @Override
    public SchoolClasses selection(
        final Filters filters, final Page page, final Sorts sorts
    ) throws Exception {
        return new ScsAuthorized(
            this.operations.operation("SchoolClasses.selection")
                .permission(this.actor, this.school)
                .permit(this.origin).selection(
                filters, page, sorts
            ), this.school, this.actor, this.operations
        );
    }

    @Override
    public Iterable<SchoolClass> iterate() {
        return StreamSupport.stream(
            this.operations.operation("SchoolClasses.selection")
                .permission(this.actor, this.school)
                .permit(this.origin).iterate().spliterator(), false
        ).<SchoolClass>map(
            clazz -> new ScAuthorized(clazz, this.school, this.actor, this.operations)
        ).toList();
    }

    @Override
    public void remove(final long id) throws Exception {
        this.operations.operation("SchoolClasses.remove")
            .permission(this.actor, this.school)
            .permit(this.origin).remove(id);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.operations.operation("SchoolClasses.selection")
            .permission(this.actor, this.school)
            .permit(this.origin).print(media);
    }
}
