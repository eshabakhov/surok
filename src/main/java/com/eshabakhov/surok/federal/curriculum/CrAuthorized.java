/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.federal.curriculum.requirement.Requirements;
import com.eshabakhov.surok.federal.curriculum.requirement.RqsAuthorized;
import com.eshabakhov.surok.security.right.RtAdmin;
import java.util.List;

/**
 * Federal curriculum whose revision requires an administrator.
 * For example, {@code new CrAuthorized(curriculum, user).requirements()}
 * preserves authorization when navigating to its requirements.
 *
 * @since 0.0.1
 */
public final class CrAuthorized implements Curriculum {

    /** Underlying curriculum. */
    private final Curriculum origin;

    /** Acting user. */
    private final User actor;

    /**
     * New protected federal curriculum.
     *
     * @param origin Underlying curriculum
     * @param actor Acting user
     */
    public CrAuthorized(final Curriculum origin, final User actor) {
        this.origin = origin;
        this.actor = actor;
    }

    @Override
    public Long uid() {
        return this.origin.uid();
    }

    @Override
    public List<Integer> grades() throws Exception {
        return this.origin.grades();
    }

    @Override
    public Curriculum revise(final CurriculumInput input) throws Exception {
        return new CrAuthorized(
            new RtAdmin(this.actor).permit(this.origin).revise(input), this.actor
        );
    }

    @Override
    public Requirements requirements() {
        return new RqsAuthorized(this.origin.requirements(), this.actor);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.origin.print(media);
    }
}
