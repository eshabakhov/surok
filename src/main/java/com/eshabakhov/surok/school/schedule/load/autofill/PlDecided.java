/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load.autofill;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.federal.subject.Subject;
import com.eshabakhov.surok.school.schedule.load.hours.WeeklyHours;
import com.eshabakhov.surok.school.schedule.load.hours.WhPlanned;
import com.eshabakhov.surok.school.schoolclass.SchoolClass;
import lombok.EqualsAndHashCode;

/**
 * An explicit decision retaining the original load and its curriculum proposal.
 * Further decisions are made against that original state.
 * Equality retains the original state, selected hours, and decision intent.
 * For example, {@code load.accept().with(hours)} replaces the proposal with manual hours.
 *
 * @since 0.0.1
 */
@EqualsAndHashCode
final class PlDecided implements PlannedLoad {

    /** Original load before user decisions. */
    private final PlannedLoad origin;

    /** Hours selected by the decision. */
    private final WeeklyHours hours;

    /** Public decision description. */
    private final String decision;

    /**
     * New explicit decision.
     *
     * @param origin Original load before user decisions
     * @param hours Selected weekly hours
     * @param decision Public decision description
     */
    PlDecided(final PlannedLoad origin, final WeeklyHours hours, final String decision) {
        this.origin = origin;
        this.hours = hours;
        this.decision = decision;
    }

    @Override
    public SchoolClass clazz() {
        return this.origin.clazz();
    }

    @Override
    public Subject subject() {
        return this.origin.subject();
    }

    @Override
    public WeeklyHours hours() {
        return new WhPlanned(this.origin.hours(), this.hours);
    }

    @Override
    public PlannedLoad with(final WeeklyHours value) throws Exception {
        return this.origin.with(value);
    }

    @Override
    public PlannedLoad accept() throws Exception {
        return this.origin.accept();
    }

    @Override
    public void apply(final LoadTarget target) throws Exception {
        target.write(this.clazz(), this.subject(), this.hours);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return media.with("schoolClass", this.clazz())
            .with("subject", this.subject())
            .with("hours", this.hours())
            .with("decision", this.decision);
    }
}
