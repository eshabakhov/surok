/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load.autofill;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.federal.subject.Subject;
import com.eshabakhov.surok.school.schedule.load.hours.WeeklyHours;
import com.eshabakhov.surok.school.schedule.load.hours.WhFixed;
import com.eshabakhov.surok.school.schedule.load.hours.WhProposed;
import com.eshabakhov.surok.school.schoolclass.SchoolClass;
import lombok.EqualsAndHashCode;

/**
 * Current and proposed hours before an explicit user decision.
 * Existing hours are preserved; a proposal fills an absent load.
 * Equality includes the class, subject, current hours, and proposal.
 * For example, {@code new PlInitial(clazz, subject, current, proposed).accept()}
 * accepts the curriculum's hours while retaining the current hours for comparison.
 *
 * @since 0.0.1
 */
@EqualsAndHashCode
public final class PlInitial implements PlannedLoad {

    /** School class. */
    private final SchoolClass clazz;

    /** School subject. */
    private final Subject subject;

    /** Existing hours, with zero representing an absent load. */
    private final WeeklyHours current;

    /** Curriculum hours, with zero representing no applicable proposal. */
    private final WeeklyHours proposed;

    /**
     * New planned load.
     *
     * @param clazz School class
     * @param subject School subject
     * @param current Existing weekly hours
     * @param proposed Proposed weekly hours
     */
    public PlInitial(
        final SchoolClass clazz,
        final Subject subject,
        final WeeklyHours current,
        final WeeklyHours proposed
    ) {
        this.clazz = clazz;
        this.subject = subject;
        this.current = current;
        this.proposed = proposed;
    }

    @Override
    public SchoolClass clazz() {
        return this.clazz;
    }

    @Override
    public Subject subject() {
        return this.subject;
    }

    @Override
    public WeeklyHours hours() {
        return new WhProposed(this.current, this.proposed);
    }

    @Override
    public PlannedLoad with(final WeeklyHours hours) {
        final PlannedLoad changed;
        if (hours.value() == 0) {
            changed = new PlDecided(this, new WhFixed(0), "removed");
        } else {
            changed = new PlDecided(this, hours, "manual");
        }
        return changed;
    }

    @Override
    public PlannedLoad accept() throws LoadInputException {
        if (this.proposed.value() == 0) {
            throw new LoadInputException(
                String.format(
                    "The selected curriculum has no proposed hours for class %d and subject %d",
                    this.clazz.uid(), this.subject.uid()
                )
            );
        }
        return new PlDecided(this, this.proposed, "plan");
    }

    @Override
    public void apply(final LoadTarget target) throws Exception {
        if (this.current.value() == 0 && this.proposed.value() > 0) {
            target.write(this.clazz, this.subject, this.proposed);
        }
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return media.with("schoolClass", this.clazz)
            .with("subject", this.subject)
            .with("hours", this.hours())
            .with("decision", "none");
    }
}
