/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load.autofill;

import com.eshabakhov.surok.Printable;
import com.eshabakhov.surok.federal.curriculum.Curriculum;
import com.eshabakhov.surok.federal.subject.Subject;
import com.eshabakhov.surok.school.schedule.load.hours.WeeklyHours;
import java.util.Map;

/**
 * A curriculum and manual additions for all applicable classes of one schedule.
 * For example, {@code draft.save(curriculum, additions)} saves a preview and
 * {@code draft.apply()} writes mandatory hours and grade additions atomically.
 *
 * @since 0.0.1
 */
public interface LoadDraft extends Printable {

    /**
     * Replaces the plan and additional hours without changing actual loads.
     *
     * @param curriculum Federal plan
     * @param additions Manual additional subjects per grade
     * @throws Exception If a selection is unavailable or exceeds its allowance
     */
    void save(Curriculum curriculum, Map<Integer, Map<Subject, WeeklyHours>> additions)
        throws Exception;

    /**
     * Switches the plan and clears grade additions when the plan changes.
     *
     * @param curriculum New plan
     * @throws Exception If switching fails
     */
    void revise(Curriculum curriculum) throws Exception;

    /**
     * Restores the selected plan.
     *
     * @return Saved curriculum
     * @throws Exception If no active draft exists
     */
    Curriculum curriculum() throws Exception;

    /**
     * Applies mandatory and additional hours after checking every class's allowance.
     *
     * @throws Exception If validation or any write fails
     */
    void apply() throws Exception;
}
