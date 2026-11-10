/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load.autofill;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.federal.curriculum.Curriculum;
import com.eshabakhov.surok.federal.subject.Subject;
import com.eshabakhov.surok.school.schedule.load.hours.WeeklyHours;
import com.eshabakhov.surok.security.right.RtAdmin;
import com.eshabakhov.surok.security.right.RtDirectorOrDeputy;
import com.eshabakhov.surok.security.right.RtEither;
import com.eshabakhov.surok.security.right.RtSchoolMember;
import java.util.Map;

/**
 * Teaching load draft protected by the acting user's current school role.
 * For example, a viewer can inspect a draft while a director can apply it.
 *
 * @since 0.0.1
 */
public final class LdAuthorized implements LoadDraft {

    /** Underlying draft. */
    private final LoadDraft origin;

    /** Owning school. */
    private final School school;

    /** Acting user. */
    private final User actor;

    /**
     * New protected draft.
     *
     * @param origin Underlying draft
     * @param school Owning school
     * @param actor Acting user
     */
    public LdAuthorized(final LoadDraft origin, final School school, final User actor) {
        this.origin = origin;
        this.school = school;
        this.actor = actor;
    }

    @Override
    public void save(final LoadDraftInput input) throws Exception {
        new RtEither(
            new RtAdmin(this.actor), new RtDirectorOrDeputy(this.actor, this.school)
        ).permit(this.origin).save(input);
    }

    @Override
    public void save(
        final Curriculum curriculum, final Map<Integer, Map<Subject, WeeklyHours>> additions
    ) throws Exception {
        new RtEither(
            new RtAdmin(this.actor), new RtDirectorOrDeputy(this.actor, this.school)
        ).permit(this.origin).save(curriculum, additions);
    }

    @Override
    public void revise(final LoadDraftInput input) throws Exception {
        new RtEither(
            new RtAdmin(this.actor), new RtDirectorOrDeputy(this.actor, this.school)
        ).permit(this.origin).revise(input);
    }

    @Override
    public void revise(final Curriculum curriculum) throws Exception {
        new RtEither(
            new RtAdmin(this.actor), new RtDirectorOrDeputy(this.actor, this.school)
        ).permit(this.origin).revise(curriculum);
    }

    @Override
    public Curriculum curriculum() throws Exception {
        return new RtEither(
            new RtAdmin(this.actor), new RtSchoolMember(this.actor, this.school)
        ).permit(this.origin).curriculum();
    }

    @Override
    public void apply() throws Exception {
        new RtEither(
            new RtAdmin(this.actor), new RtDirectorOrDeputy(this.actor, this.school)
        ).permit(this.origin).apply();
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return new RtEither(
            new RtAdmin(this.actor), new RtSchoolMember(this.actor, this.school)
        ).permit(this.origin).print(media);
    }
}
