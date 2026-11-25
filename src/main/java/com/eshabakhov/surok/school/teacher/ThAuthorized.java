/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.teacher;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.security.Operations;

/**
 * School teacher that checks the acting user's role for each operation.
 * For example, a deputy can revise a teacher returned by a protected collection.
 *
 * @since 0.0.1
 */
public final class ThAuthorized implements Teacher {

    /** Shared operation declarations. */
    private final Operations operations;

    /** Underlying teacher. */
    private final Teacher origin;

    /** School that owns the teacher. */
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
    public ThAuthorized(
        final Teacher origin, final School school, final UrAccount actor,
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
    public Teacher revise(final TeacherInput input) throws Exception {
        return new ThAuthorized(
            this.operations.operation("Teachers.revise")
                .permission(this.actor, this.school)
                .permit(this.origin).revise(input),
            this.school, this.actor, this.operations
        );
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.operations.operation("Teachers.teacher")
            .permission(this.actor, this.school)
            .permit(this.origin).print(media);
    }
}
