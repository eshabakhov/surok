/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.federal.curriculum.requirement.Requirements;
import com.eshabakhov.surok.federal.curriculum.requirement.RqsAuthorized;
import com.eshabakhov.surok.security.Operations;
import java.util.List;

/**
 * Federal curriculum whose revision requires an administrator.
 * For example, {@code new CrAuthorized(curriculum, user, operations).requirements()}
 * preserves authorization when navigating to its requirements.
 *
 * @since 0.0.1
 */
public final class CrAuthorized implements Curriculum {

    /** Shared operation declarations. */
    private final Operations operations;

    /** Underlying curriculum. */
    private final Curriculum origin;

    /** Acting user. */
    private final UrAccount actor;

    /**
     * New protected object with shared operation declarations.
     *
     * @param origin Underlying origin
     * @param actor Acting user
     * @param operations Shared operation declarations
     */
    public CrAuthorized(
        final Curriculum origin, final UrAccount actor,
        final Operations operations
    ) {
        this.origin = origin;
        this.actor = actor;
        this.operations = operations;
    }

    @Override
    public Long uid() {
        return this.origin.uid();
    }

    @Override
    public List<Integer> grades() throws Exception {
        return this.operations.operation("Curriculums.curriculum")
            .permission(this.actor)
            .permit(this.origin).grades();
    }

    @Override
    public Curriculum revise(final CurriculumInput input) throws Exception {
        return new CrAuthorized(
            this.operations.operation("Curriculums.revise")
                .permission(this.actor)
                .permit(this.origin).revise(input), this.actor, this.operations
        );
    }

    @Override
    public Requirements requirements() {
        return new RqsAuthorized(
            this.operations.operation("Requirements.selection")
                .permission(this.actor)
                .permit(this.origin).requirements(), this.actor, this.operations
        );
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.operations.operation("Curriculums.curriculum")
            .permission(this.actor)
            .permit(this.origin).print(media);
    }
}
