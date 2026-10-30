/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load.autofill;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.federal.curriculum.Curriculum;
import com.eshabakhov.surok.federal.subject.Subject;
import com.eshabakhov.surok.school.schedule.load.hours.WeeklyHours;
import java.util.Map;

/**
 * Draft decorated with its saved state for the browser editor.
 * For example, printing an absent draft offers an empty editor without writing data.
 *
 * @since 0.0.1
 */
public final class LdWeb implements LoadDraft {

    /** Original draft. */
    private final LoadDraft origin;

    /**
     * New browser draft.
     *
     * @param origin Original draft
     */
    public LdWeb(final LoadDraft origin) {
        this.origin = origin;
    }

    @Override
    public void save(
        final Curriculum curriculum, final Map<Integer, Map<Subject, WeeklyHours>> additions
    ) throws Exception {
        this.origin.save(curriculum, additions);
    }

    @Override
    public void revise(final Curriculum curriculum) throws Exception {
        this.origin.revise(curriculum);
    }

    @Override
    public Curriculum curriculum() throws Exception {
        return this.origin.curriculum();
    }

    @Override
    public void apply() throws Exception {
        this.origin.apply();
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        Media<T> result;
        try {
            result = media.with("draft", this.origin).with("saved", true);
        } catch (final LoadDraftNotFoundException exception) {
            result = media.with("saved", false);
        }
        return result;
    }
}
