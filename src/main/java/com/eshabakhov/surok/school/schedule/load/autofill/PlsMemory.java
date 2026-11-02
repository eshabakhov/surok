/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load.autofill;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.PrItems;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable snapshot of prepared loads and user decisions held in memory.
 * The supplied pairs define the editable scope, including zero-hour loads where needed.
 * For example, {@code new PlsMemory(loads).iterate()} reads a snapshot of prepared loads
 * retaining their original current hours, proposals and selected hours.
 *
 * @since 0.0.1
 */
public final class PlsMemory implements PlannedLoads {

    /** Snapshot of prepared loads in their original order. */
    private final List<PlannedLoad> loads;

    /**
     * New plan containing every editable class and subject pair.
     *
     * @param loads Prepared loads with at most one item per pair
     */
    public PlsMemory(final Collection<? extends PlannedLoad> loads) {
        this.loads = new ArrayList<>(loads);
    }

    @Override
    public Iterable<PlannedLoad> iterate() throws LoadInputException {
        return List.copyOf(this.indexed().values());
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return media.with(new PrItems("load", this.iterate()));
    }

    private Map<List<Long>, PlannedLoad> indexed() throws LoadInputException {
        final Map<List<Long>, PlannedLoad> indexed = new LinkedHashMap<>();
        for (final PlannedLoad load : this.loads) {
            final List<Long> key = List.of(load.clazz().uid(), load.subject().uid());
            if (indexed.containsKey(key)) {
                throw new LoadInputException(
                    String.format(
                        "Duplicate prepared loads for class %d and subject %d",
                        load.clazz().uid(), load.subject().uid()
                    )
                );
            }
            indexed.put(key, load);
        }
        return indexed;
    }
}
