/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.teacher;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.security.right.RtAdmin;
import com.eshabakhov.surok.security.right.RtDirectorOrDeputy;
import com.eshabakhov.surok.security.right.RtEither;
import com.eshabakhov.surok.security.right.RtSchoolMember;

/**
 * School teacher that checks the acting user's role for each operation.
 * For example, a deputy can revise a teacher returned by a protected collection.
 *
 * @since 0.0.1
 */
public final class ThAuthorized implements Teacher {

    /** Underlying teacher. */
    private final Teacher origin;

    /** School that owns the teacher. */
    private final School school;

    /** Acting user. */
    private final User actor;

    /**
     * New protected school teacher.
     *
     * @param origin Underlying teacher
     * @param school School that owns it
     * @param actor Acting user
     */
    public ThAuthorized(final Teacher origin, final School school, final User actor) {
        this.origin = origin;
        this.school = school;
        this.actor = actor;
    }

    @Override
    public Long uid() {
        return this.origin.uid();
    }

    @Override
    public Teacher revise(final TeacherInput input) throws Exception {
        return new ThAuthorized(
            new RtEither(
                new RtAdmin(this.actor), new RtDirectorOrDeputy(this.actor, this.school)
            ).permit(this.origin).revise(input),
            this.school, this.actor
        );
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return new RtEither(
            new RtAdmin(this.actor), new RtSchoolMember(this.actor, this.school)
        ).permit(this.origin).print(media);
    }
}
