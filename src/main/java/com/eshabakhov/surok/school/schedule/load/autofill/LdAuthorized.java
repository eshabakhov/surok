/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load.autofill;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.federal.curriculum.Curriculum;
import com.eshabakhov.surok.federal.subject.Subject;
import com.eshabakhov.surok.school.schedule.load.hours.WeeklyHours;
import com.eshabakhov.surok.security.Operations;
import java.util.Map;

/**
 * Teaching load draft protected by the acting user's current school role.
 * For example, a student can inspect a draft while a director can apply it.
 *
 * @since 0.0.1
 */
public final class LdAuthorized implements LoadDraft {

    /** Shared operation declarations. */
    private final Operations operations;

    /** Underlying draft. */
    private final LoadDraft origin;

    /** Owning school. */
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
    public LdAuthorized(
        final LoadDraft origin, final School school, final UrAccount actor,
        final Operations operations
    ) {
        this.origin = origin;
        this.school = school;
        this.actor = actor;
        this.operations = operations;
    }

    @Override
    public void save(final LoadDraftInput input) throws Exception {
        this.operations.operation("LoadDraft.save")
            .permission(this.actor, this.school)
            .permit(this.origin).save(input);
    }

    @Override
    public void save(
        final Curriculum curriculum, final Map<Integer, Map<Subject, WeeklyHours>> additions
    ) throws Exception {
        this.operations.operation("LoadDraft.save")
            .permission(this.actor, this.school)
            .permit(this.origin).save(curriculum, additions);
    }

    @Override
    public void revise(final LoadDraftInput input) throws Exception {
        this.operations.operation("LoadDraft.revise")
            .permission(this.actor, this.school)
            .permit(this.origin).revise(input);
    }

    @Override
    public void revise(final Curriculum curriculum) throws Exception {
        this.operations.operation("LoadDraft.revise")
            .permission(this.actor, this.school)
            .permit(this.origin).revise(curriculum);
    }

    @Override
    public Curriculum curriculum() throws Exception {
        return this.operations.operation("LoadDraft.read")
            .permission(this.actor, this.school)
            .permit(this.origin).curriculum();
    }

    @Override
    public void apply() throws Exception {
        this.operations.operation("LoadDraft.apply")
            .permission(this.actor, this.school)
            .permit(this.origin).apply();
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.operations.operation("LoadDraft.read")
            .permission(this.actor, this.school)
            .permit(this.origin).print(media);
    }
}
