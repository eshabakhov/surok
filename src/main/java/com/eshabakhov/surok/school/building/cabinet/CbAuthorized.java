/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.building.cabinet;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.security.Operations;

/**
 * Cabinet that checks the acting user's school role for each operation.
 * For example, {@code new CbAuthorized(cabinet, school, actor, operations).revise(input)}
 * rejects a user who no longer has an editing role.
 *
 * @since 0.0.1
 */
public final class CbAuthorized implements Cabinet {

    /** Shared operation declarations. */
    private final Operations operations;

    /** Underlying cabinet. */
    private final Cabinet origin;

    /** School that owns the cabinet. */
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
    public CbAuthorized(
        final Cabinet origin, final School school, final UrAccount actor,
        final Operations operations
    ) {
        this.origin = origin;
        this.school = school;
        this.actor = actor;
        this.operations = operations;
    }

    @Override
    public Long uid() {
        return this.origin.uid();
    }

    @Override
    public Cabinet revise(final CabinetInput input) throws Exception {
        return new CbAuthorized(
            this.operations.operation("Cabinets.revise")
                .permission(this.actor, this.school)
                .permit(this.origin).revise(input),
            this.school, this.actor, this.operations
        );
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.operations.operation("Cabinets.cabinet")
            .permission(this.actor, this.school)
            .permit(this.origin).print(media);
    }
}
