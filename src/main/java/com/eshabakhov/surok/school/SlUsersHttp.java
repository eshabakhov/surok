/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school;

import com.eshabakhov.surok.PrItems;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.Users;
import com.eshabakhov.surok.media.AcHttp;
import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.media.MdXml;
import com.eshabakhov.surok.user.Role;
import com.eshabakhov.surok.user.RoleAssignmentException;
import com.eshabakhov.surok.user.Roles;
import com.eshabakhov.surok.user.UrsPostgres;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.jooq.DSLContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Membership and roles of one school.
 *
 * @since 0.0.1
 * @checkstyle ParameterNumberCheck (1000 lines)
 * @checkstyle NonStaticMethodCheck (1000 lines)
 */
@RestController
@RequestMapping("/schools/{school}/users")
@Tag(name = "School users")
public class SlUsersHttp {

    /** Database context. */
    private final DSLContext ctx;

    /**
     * New school users resource.
     *
     * @param ctx Database context
     */
    public SlUsersHttp(final DSLContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Lists a school's members.
     *
     * @param school School identifier
     * @param request HTTP request
     * @param actor Acting user
     * @return Members
     * @throws Exception If printing fails
     */
    @GetMapping(
        produces = {
            "application/vnd.surok.school-users.v1+json",
            "application/vnd.surok.school-users.v1+xml"
        }
    )
    @Operation(summary = "List school users")
    public ResponseEntity<?> list(
        @PathVariable final long school, final HttpServletRequest request,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        final MediaType type = this.accepted(request);
        final PrItems items = new PrItems(
            "user", this.members(actor, school).iterate()
        );
        return ResponseEntity.ok().contentType(new MediaType(type, StandardCharsets.UTF_8)).body(
            switch (type.toString()) {
                case "application/vnd.surok.school-users.v1+json" ->
                    items.print(new MdJson()).print();
                case "application/vnd.surok.school-users.v1+xml" ->
                    items.print(new MdXml("users")).print();
                case null, default -> throw new ResponseStatusException(
                    HttpStatus.NOT_ACCEPTABLE, "Unsupported Accept header"
                );
            }
        );
    }

    /**
     * Adds an existing account to a school.
     *
     * @param school School identifier
     * @param user Account identifier
     * @param request HTTP request
     * @param actor Acting user
     * @return School member
     * @throws Exception If assignment fails
     */
    @PutMapping(
        value = "/{user}",
        produces = {
            "application/vnd.surok.school-user.v1+json",
            "application/vnd.surok.school-user.v1+xml"
        }
    )
    @Operation(summary = "Add existing user to school")
    public ResponseEntity<?> add(
        @PathVariable final long school, @PathVariable final long user,
        final HttpServletRequest request, @AuthenticationPrincipal final User actor
    ) throws Exception {
        final MediaType type = new AcHttp(
            request, List.of(
                MediaType.parseMediaType("application/vnd.surok.school-user.v1+json"),
                MediaType.parseMediaType("application/vnd.surok.school-user.v1+xml")
            )
        ).type();
        final Users members = this.members(actor, school);
        final User selected = new UrsPostgres(this.ctx).identification(user);
        final HttpStatus status;
        if (members.contains(selected)) {
            status = HttpStatus.OK;
        } else {
            status = HttpStatus.CREATED;
        }
        final User added = members.add(selected);
        return ResponseEntity.status(status)
            .contentType(new MediaType(type, StandardCharsets.UTF_8)).location(
                URI.create(String.format("/schools/%d/users/%d", school, user))
            ).body(
                switch (type.toString()) {
                    case "application/vnd.surok.school-user.v1+json" ->
                        added.print(new MdJson()).print();
                    case "application/vnd.surok.school-user.v1+xml" ->
                        added.print(new MdXml("user")).print();
                    case null, default -> throw new ResponseStatusException(
                        HttpStatus.NOT_ACCEPTABLE, "Unsupported Accept header"
                    );
                }
            );
    }

    /**
     * Adds an existing account identified by username or email.
     *
     * @param school School identifier
     * @param document Login document
     * @param request HTTP request
     * @param actor Acting user
     * @return School member
     * @throws Exception If identification or assignment fails
     */
    @PostMapping(
        consumes = "application/vnd.surok.school-user.v1+json",
        produces = {
            "application/vnd.surok.school-user.v1+json",
            "application/vnd.surok.school-user.v1+xml"
        }
    )
    @Operation(summary = "Add an existing account by username or email")
    public ResponseEntity<?> attach(
        @PathVariable final long school, @RequestBody final JsonNode document,
        final HttpServletRequest request, @AuthenticationPrincipal final User actor
    ) throws Exception {
        final JsonNode login = document.get("login");
        if (login == null || login.asText().isBlank()) {
            throw new RoleAssignmentException("Login is required");
        }
        return this.add(
            school, new UrsPostgres(this.ctx).identification(login.asText()).uid(),
            request, actor
        );
    }

    /**
     * Removes a member from a school without deleting the account.
     *
     * @param school School identifier
     * @param user Account identifier
     * @param actor Acting user
     * @return Empty response
     * @throws Exception If removal fails
     */
    @DeleteMapping("/{user}")
    @Operation(summary = "Remove user from school")
    public ResponseEntity<Void> remove(
        @PathVariable final long school, @PathVariable final long user,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        this.members(actor, school).remove(user);
        return ResponseEntity.noContent().build();
    }

    /**
     * Lists a member's school roles.
     *
     * @param school School identifier
     * @param user Account identifier
     * @param actor Acting user
     * @return Roles
     * @throws Exception If lookup fails
     */
    @GetMapping(
        value = "/{user}/roles",
        produces = "application/vnd.surok.school-roles.v1+json"
    )
    @Operation(summary = "List school roles of user")
    public ResponseEntity<List<Role>> roles(
        @PathVariable final long school, @PathVariable final long user,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        final Users members = this.members(actor, school);
        return ResponseEntity.ok(members.roles(members.identification(user)).list());
    }

    /**
     * Grants a school role to a member.
     *
     * @param school School identifier
     * @param user Account identifier
     * @param document Role document
     * @param actor Acting user
     * @return Granted role
     * @throws Exception If assignment fails
     */
    @PostMapping(
        value = "/{user}/roles",
        consumes = "application/vnd.surok.school-role.v1+json",
        produces = "application/vnd.surok.school-role.v1+json"
    )
    @Operation(summary = "Grant school role")
    public ResponseEntity<Role> grant(
        @PathVariable final long school, @PathVariable final long user,
        @RequestBody final JsonNode document, @AuthenticationPrincipal final User actor
    ) throws Exception {
        final JsonNode name = document.get("role");
        if (name == null || name.asText().isBlank()) {
            throw new RoleAssignmentException("Role is required");
        }
        final Users members = this.members(actor, school);
        final Role granted = members.roles(members.identification(user)).grant(
            switch (name.asText()) {
                case "DIRECTOR" -> Roles.RoleEnum.DIRECTOR;
                case "DEPUTY_DIRECTOR" -> Roles.RoleEnum.DEPUTY_DIRECTOR;
                case "TEACHER" -> Roles.RoleEnum.TEACHER;
                case "STUDENT" -> Roles.RoleEnum.STUDENT;
                case "VIEWER" -> Roles.RoleEnum.VIEWER;
                case null, default -> throw new RoleAssignmentException("Unknown school role");
            }
        );
        return ResponseEntity.created(
            URI.create(String.format("/schools/%d/users/%d/roles/%d", school, user, granted.uid()))
        ).body(granted);
    }

    /**
     * Revokes a member's school role.
     *
     * @param school School identifier
     * @param user Account identifier
     * @param role Role assignment identifier
     * @param actor Acting user
     * @return Empty response
     * @throws Exception If revocation fails
     */
    @DeleteMapping("/{user}/roles/{role}")
    @Operation(summary = "Revoke school role")
    public ResponseEntity<Void> revoke(
        @PathVariable final long school, @PathVariable final long user,
        @PathVariable final long role, @AuthenticationPrincipal final User actor
    ) throws Exception {
        final Users members = this.members(actor, school);
        members.roles(members.identification(user)).revoke(role);
        return ResponseEntity.noContent().build();
    }

    private MediaType accepted(final HttpServletRequest request) {
        return new AcHttp(
            request, List.of(
                MediaType.parseMediaType("application/vnd.surok.school-users.v1+json"),
                MediaType.parseMediaType("application/vnd.surok.school-users.v1+xml")
            )
        ).type();
    }

    private Users members(final User actor, final long id) throws Exception {
        return new SlsAuthorized(new SlsPostgres(this.ctx), actor).school(id).users();
    }
}
