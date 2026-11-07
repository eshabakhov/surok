/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok;

import com.eshabakhov.surok.user.Roles;

/**
 * Users collection abstraction.
 *
 * @since 0.0.1
 */
public interface Users {

    /**
     * Whether a user belongs to this collection.
     *
     * @param user User to check
     * @return True if the user belongs to the collection
     */
    boolean contains(User user);

    /**
     * Adds an existing user to a school collection.
     *
     * @param user User to add
     * @return Added user
     * @throws Exception If adding fails
     */
    User add(User user) throws Exception;

    /**
     * Roles of a user in this collection.
     *
     * @param user User whose roles are requested
     * @return Scoped roles
     */
    Roles roles(User user);

    /**
     * Users in this collection.
     *
     * @return Users
     */
    Iterable<User> iterate();

    /**
     * Register new user.
     *
     * @param username User to create
     * @param password User to create
     * @param email User to create
     * @param corporate User to create
     * @return Created user
     * @throws Exception if creation fails
     * @checkstyle ParameterNumberCheck (2 lines)
     */
    User register(
        String username,
        String password,
        String email,
        Boolean corporate
    ) throws Exception;

    /**
     * Identify user by login.
     *
     * @param login Username or email
     * @return User
     * @throws Exception if user not found
     */
    User identification(String login) throws Exception;

    /**
     * Identify user by id.
     *
     * @param id User id
     * @return User
     * @throws Exception if user not found
     */
    User identification(Long id) throws Exception;

    /**
     * Authenticate user by login.
     *
     * @param login Username or email
     * @return User
     * @throws Exception If authentication fails
     */
    User authentication(String login) throws Exception;

    /**
     * Removes user by id.
     *
     * @param id User id
     * @throws Exception if user not found
     */
    void remove(Long id) throws Exception;
}
