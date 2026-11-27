/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.federal.curriculum.input.MdCrInput;
import com.eshabakhov.surok.federal.curriculum.requirement.Requirements;
import java.util.List;

/**
 * Unsaved curriculum with the creation form's default level and week.
 * For example, {@code draft.revise(input)} returns new fields without writing to storage.
 *
 * @since 0.0.1
 */
public final class CrDraft implements Curriculum {

    /** Fields entered before creation. */
    private final CurriculumInput input;

    /**
     * New unsaved curriculum.
     *
     * @param input Fields entered before creation
     */
    public CrDraft(final CurriculumInput input) {
        this.input = input;
    }

    @Override
    public Long uid() {
        throw new IllegalStateException("An unsaved curriculum has no identifier");
    }

    @Override
    public List<Integer> grades() throws Exception {
        return switch (Level.valueOf(
            this.input.print(new MdCrInput()).print().getOrDefault("level", Level.PRIMARY.name())
        )) {
            case PRIMARY -> List.of(1, 2, 3, 4);
            case BASIC -> List.of(5, 6, 7, 8, 9);
            case SECONDARY -> List.of(10, 11);
        };
    }

    @Override
    public Curriculum revise(final CurriculumInput fields) {
        return new CrDraft(fields);
    }

    @Override
    public Requirements requirements() {
        throw new IllegalStateException("An unsaved curriculum has no requirements");
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.input.print(
            media.with("title", "").with("level", Level.PRIMARY.name())
                .with("week", Week.FIVE_DAYS.name()).with("version", "")
                .with("year", "").with("description", "")
        );
    }
}
