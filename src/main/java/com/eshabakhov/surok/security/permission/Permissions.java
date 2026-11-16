/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import com.eshabakhov.surok.Printable;
import com.eshabakhov.surok.UrAccount;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Permissions of the application's registered controller operations.
 * For example, print a role matrix or filter an OpenAPI operation for its caller.
 *
 * @since 0.0.1
 */
public interface Permissions extends Printable {

    /**
     * Evaluate a documented operation for this catalog's audience.
     *
     * @param path Route pattern
     * @param method HTTP method
     * @return Whether a matching handler grants access
     */
    boolean allows(String path, RequestMethod method);

    /**
     * Select permissions for an identified account user.
     *
     * @param actor Current account user
     * @return Account-specific permission catalog
     */
    Permissions selection(UrAccount actor);
}
