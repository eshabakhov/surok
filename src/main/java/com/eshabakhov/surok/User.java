/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok;

import com.eshabakhov.surok.user.Credentials;
import com.eshabakhov.surok.user.ReferenceInformation;
import com.eshabakhov.surok.user.Roles;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * User abstraction.
 *
 * @since 0.0.1
 */
public interface User extends Printable {

    /**
     * User unique identifier.
     *
     * @return User ID
     */
    @JsonProperty
    long uid();

    /**
     * User credentials.
     *
     * @return Credentials
     */
    @JsonProperty
    Credentials credentials();

    /**
     * User roles.
     *
     * @return Roles
     */
    @JsonProperty
    Roles roles();

    /**
     * Schools available to this user.
     *
     * @return User's schools
     */
    Schools schools();

    /**
     * Reference information.
     *
     * @return Reference information
     */
    @JsonProperty
    ReferenceInformation info();

    /**
     * Administrative access marker.
     *
     * @return Administrative access marker
     */
    boolean isAdmin();
}
