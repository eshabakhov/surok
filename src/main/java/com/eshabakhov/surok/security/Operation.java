/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security;

import com.eshabakhov.surok.Printable;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.Schools;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.security.permission.Permission;
import com.eshabakhov.surok.security.permission.PmAnyOf;
import java.util.List;
import java.util.stream.StreamSupport;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * HTTP operation that binds its declarations to a caller.
 * For example, creation covers both a JSON document and a browser form.
 *
 * @since 0.0.1
 */
public interface Operation extends Printable {

    /**
     * Stable operation identifier.
     *
     * @return Identifier declared in the document
     */
    String identifier();

    /**
     * Route template.
     *
     * @return HTTP path
     */
    String path();

    /**
     * Request method.
     *
     * @return HTTP method
     */
    RequestMethod method();

    /**
     * Evaluate the listed roles without a school context.
     *
     * @param actor Visitor, possibly anonymous
     * @return Global permission
     */
    Permission permission(User actor);

    /**
     * Evaluate the listed roles in the target school.
     *
     * @param actor Acting account
     * @param school Target school
     * @return Permission for this school
     */
    Permission permission(UrAccount actor, School school);

    /**
     * Check a school identifier before accessing its repository entry.
     *
     * @param actor Acting account
     * @param school Requested school identifier
     * @return Global permission or permission in the requested school
     */
    default Permission permission(final UrAccount actor, final long school) {
        return new PmAnyOf(
            this.permission(actor), new PmAnyOf(
                () -> StreamSupport.stream(actor.schools().iterate().spliterator(), false)
                    .filter(target -> target.uid() == school)
                    .map(target -> this.permission(actor, target)).iterator()
            )
        );
    }

    /**
     * Evaluate account roles and inspect schools only if necessary.
     *
     * @param actor Acting account
     * @param schools Available schools
     * @return Global access or access in at least one school
     */
    default Permission permission(final UrAccount actor, final Schools schools) {
        return new PmAnyOf(
            List.of(
                this.permission(actor),
                new PmAnyOf(
                    () -> StreamSupport.stream(schools.iterate().spliterator(), false)
                        .map(school -> this.permission(actor, school)).iterator()
                )
            )
        );
    }
}
