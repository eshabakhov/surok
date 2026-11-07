/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.User;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.List;
import org.jooq.DSLContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * User management API controller.
 *
 * @since 0.0.1
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    /** Database context. */
    private final DSLContext ctx;

    UserController(final DSLContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Register a user.
     *
     * @param request Registration request
     * @return Created user
     * @throws Exception If registration fails
     */
    @PostMapping
    public ResponseEntity<User> register(
        @RequestBody final JsonNode request
    ) throws Exception {
        final JsonNode username = request.get("username");
        if (username == null || username.asText().isBlank()) {
            throw new RegistrationException("Username is required");
        }
        final JsonNode password = request.get("password");
        if (password == null || password.asText().isBlank()) {
            throw new RegistrationException("Password is required");
        }
        final JsonNode email = request.get("email");
        if (email == null || email.asText().isBlank()) {
            throw new RegistrationException("Email is required");
        }
        if (request.has("school")) {
            throw new RegistrationException("School membership is managed separately");
        }
        final User created = new UrsPostgres(this.ctx).register(
            username.asText(), password.asText(), email.asText(), false
        );
        return ResponseEntity
            .created(URI.create(String.format("/api/users/%s", created.uid())))
            .body(created);
    }

    /**
     * Remove user.
     *
     * @param user User's Id
     * @param actor Acting user
     * @return Nothing
     * @throws Exception If remove fails
     */
    @DeleteMapping("/{user}")
    public ResponseEntity<Void> remove(
        @PathVariable final long user, @AuthenticationPrincipal final User actor
    ) throws Exception {
        new UrsGlobalAuthorized(new UrsPostgres(this.ctx), actor).remove(user);
        return ResponseEntity.noContent().build();
    }

    /**
     * Assign role to user.
     *
     * @param user User's ID
     * @param request Role payload
     * @param actor Acting user
     * @return ResponseEntity with {@link Role}
     * @throws Exception if assign fails
     */
    @PostMapping("/{user}/roles")
    public ResponseEntity<Role> assignRole(
        @PathVariable final long user,
        @RequestBody final JsonNode request,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        final User target = new UrsGlobalAuthorized(
            new UrsPostgres(this.ctx), actor
        ).identification(user);
        final JsonNode rolenode = request.get("role");
        if (rolenode == null || rolenode.asText().isBlank()) {
            throw new RoleAssignmentException("Role is required");
        }
        final Role assigned = target.roles().grant(
            switch (rolenode.asText()) {
                case "ADMIN" -> Roles.RoleEnum.ADMIN;
                case "BASIC_MAKER" -> Roles.RoleEnum.BASIC_MAKER;
                case "ADVANCED_MAKER" -> Roles.RoleEnum.ADVANCED_MAKER;
                case "PRO_MAKER" -> Roles.RoleEnum.PRO_MAKER;
                case null, default -> throw new RoleAssignmentException(
                    "School roles must be assigned within a school"
                );
            }
        );
        return ResponseEntity.created(
            URI.create(
                String.format(
                    "/api/users/%d/roles/%d",
                    user,
                    assigned.uid()
                )
            )
        ).body(assigned);
    }

    /**
     * Retrieve list of user's role.
     *
     * @param user User's Id
     * @param actor Acting user
     * @return ResponseEntity with list of {@link Role}
     */
    @GetMapping("/{user}/roles")
    public ResponseEntity<List<Role>> listRoles(
        @PathVariable final long user, @AuthenticationPrincipal final User actor
    ) throws Exception {
        return ResponseEntity.ok(
            new UrsGlobalAuthorized(new UrsPostgres(this.ctx), actor)
                .identification(user)
                .roles()
                .list()
        );
    }

    /**
     * Remove user's role.
     *
     * @param user User's Id
     * @param role Role
     * @param actor Acting user
     * @return Nothing
     * @throws Exception If delete fails
     */
    @DeleteMapping("/{user}/roles/{role}")
    public ResponseEntity<Void> removeRole(
        @PathVariable final long user,
        @PathVariable final long role,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        new UrsGlobalAuthorized(new UrsPostgres(this.ctx), actor)
            .identification(user)
            .roles()
            .revoke(role);
        return ResponseEntity.noContent().build();
    }
}
