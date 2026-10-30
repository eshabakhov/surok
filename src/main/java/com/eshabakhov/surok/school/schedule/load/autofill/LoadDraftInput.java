/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load.autofill;

import com.eshabakhov.surok.federal.curriculum.Curriculum;
import com.eshabakhov.surok.federal.subject.Subject;
import com.eshabakhov.surok.school.schedule.load.hours.WeeklyHours;
import java.util.Map;

/**
 * Transport input decoded into a plan and manual grade additions.
 * For example, {@code draft.save(input.curriculum(), input.additions())} saves typed input.
 *
 * @since 0.0.1
 */
public interface LoadDraftInput {

    /**
     * Selected curriculum.
     *
     * @return Curriculum handle
     * @throws Exception If its identifier is malformed
     */
    Curriculum curriculum() throws Exception;

    /**
     * Additional subjects and hours for each submitted grade.
     *
     * @return Immutable grade selections
     * @throws Exception If the submitted additions are malformed or duplicated
     */
    Map<Integer, Map<Subject, WeeklyHours>> additions() throws Exception;
}
