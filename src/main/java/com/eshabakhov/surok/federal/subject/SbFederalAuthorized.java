/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.User;
import org.springframework.security.access.AccessDeniedException;

/**
 * Federal subject whose revision requires an administrator.
 * For example, {@code new SbFederalAuthorized(subject, actor).revise(input)}
 * rejects a school user without the global administrator role.
 *
 * @since 0.0.1
 */
public final class SbFederalAuthorized implements Subject {

    /** Underlying federal subject. */
    private final Subject subject;

    /** Acting user. */
    private final User actor;

    /**
     * New protected federal subject.
     *
     * @param subject Underlying federal subject
     * @param actor Acting user
     */
    public SbFederalAuthorized(final Subject subject, final User actor) {
        this.subject = subject;
        this.actor = actor;
    }

    @Override
    public Long uid() {
        return this.subject.uid();
    }

    @Override
    public String name() throws Exception {
        return this.subject.name();
    }

    @Override
    public Origin origin() throws Exception {
        return this.subject.origin();
    }

    @Override
    public Subject revise(final SubjectInput input) throws Exception {
        if (!this.actor.isAdmin()) {
            throw new AccessDeniedException("Only an administrator can revise a federal subject");
        }
        return new SbFederalAuthorized(this.subject.revise(input), this.actor);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.subject.print(media);
    }
}
