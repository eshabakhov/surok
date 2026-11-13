/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import com.eshabakhov.surok.Printable;
import com.eshabakhov.surok.User;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Permissions of the application's registered controller operations.
 * For example, print a role matrix or filter an OpenAPI operation for its caller.
 *
 * @since 0.0.1
 */
public interface Permissions extends Printable {

    /**
     * Evaluate a documented operation in the caller's own school.
     *
     * @param path Route pattern
     * @param method HTTP method
     * @param actor Current account
     * @return Whether a matching handler grants access
     */
    boolean allows(String path, RequestMethod method, User actor);
}
