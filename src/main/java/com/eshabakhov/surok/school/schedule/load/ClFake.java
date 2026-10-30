/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.federal.subject.Subject;
import com.eshabakhov.surok.school.schedule.load.hours.WeeklyHours;
import com.eshabakhov.surok.school.schoolclass.SchoolClass;
import lombok.EqualsAndHashCode;

/**
 * Read-only teaching load facts without database access.
 * Instances with the same identifier are equal.
 * For example, {@code new ClFake(id, clazz, subject, hours)} prints an in-memory teaching load.
 *
 * @since 0.0.1
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class ClFake implements ClassLoad {

    /** Load identity. */
    @EqualsAndHashCode.Include
    private final long identifier;

    /** School class. */
    private final SchoolClass clazz;

    /** School subject. */
    private final Subject subject;

    /** Current weekly hours. */
    private final WeeklyHours hours;

    /**
     * New read-only teaching load.
     *
     * @param identifier Load identity
     * @param clazz School class
     * @param subject School subject
     * @param hours Current weekly hours
     */
    public ClFake(
        final long identifier,
        final SchoolClass clazz,
        final Subject subject,
        final WeeklyHours hours
    ) {
        this.identifier = identifier;
        this.clazz = clazz;
        this.subject = subject;
        this.hours = hours;
    }

    @Override
    public Long uid() {
        return this.identifier;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return media.with("id", this.identifier)
            .with("schoolClass", this.clazz)
            .with("subject", this.subject)
            .with("hours", this.hours.value());
    }
}
