/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schoolclass;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.security.Operations;

/**
 * School class that checks the acting user's role for each operation.
 * For example, a deputy can revise a class returned by a protected collection.
 *
 * @since 0.0.1
 */
public final class ScAuthorized implements SchoolClass {

    /** Shared operation declarations. */
    private final Operations operations;

    /** Underlying class. */
    private final SchoolClass origin;

    /** School that owns the class. */
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
    public ScAuthorized(
        final SchoolClass origin, final School school, final UrAccount actor,
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
    public int grade() throws Exception {
        return this.operations.operation("SchoolClasses.clazz")
            .permission(this.actor, this.school)
            .permit(this.origin).grade();
    }

    @Override
    public SchoolClass revise(final SchoolClassInput input) throws Exception {
        return new ScAuthorized(
            this.operations.operation("SchoolClasses.revise")
                .permission(this.actor, this.school)
                .permit(this.origin).revise(input),
            this.school, this.actor, this.operations
        );
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.operations.operation("SchoolClasses.clazz")
            .permission(this.actor, this.school)
            .permit(this.origin).print(media);
    }
}
