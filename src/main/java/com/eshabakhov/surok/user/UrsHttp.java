/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.PrItems;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.Users;
import com.eshabakhov.surok.media.AcHttp;
import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.media.MdXml;
import com.eshabakhov.surok.media.MdXslt;
import com.eshabakhov.surok.media.SrWeb;
import com.eshabakhov.surok.media.XsClasspath;
import com.eshabakhov.surok.school.SlsAuthorized;
import com.eshabakhov.surok.school.SlsBrief;
import com.eshabakhov.surok.school.SlsPostgres;
import com.eshabakhov.surok.security.CsToken;
import com.eshabakhov.surok.security.right.RtAdmin;
import com.eshabakhov.surok.user.subscription.PgSubscriptions;
import com.eshabakhov.surok.user.subscription.SpWeb;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.jooq.DSLContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * HTTP resources for accounts, school membership, roles and subscriptions.
 *
 * @since 0.0.1
 * @checkstyle ParameterNumberCheck (1000 lines)
 * @checkstyle NonStaticMethodCheck (1000 lines)
 * @checkstyle ClassFanOutComplexityCheck (1000 lines)
 */
@RestController
@SuppressWarnings("PMD.TooManyMethods")
public final class UrsHttp {

    /** Database context. */
    private final DSLContext ctx;

    /**
     * New user HTTP resource.
     *
     * @param ctx Database context
     */
    public UrsHttp(final DSLContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Render a login or registration form.
     *
     * @param request HTTP request
     * @return Public form
     * @throws Exception If printing fails
     * @checkstyle NonStaticMethodCheck (2 lines)
     */
    @GetMapping(value = {"/users/login", "/users/register"}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> authenticationPage(final HttpServletRequest request)
        throws Exception {
        final String view;
        if (request.getRequestURI().endsWith("/register")) {
            view = "register";
        } else {
            view = "login";
        }
        return ResponseEntity.ok().contentType(
            new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8)
        ).body(UrsHttp.authentication(request, view).print());
    }

    /**
     * Register a personal account from a CSRF-protected browser form.
     *
     * @param parameters Submitted fields
     * @param request HTTP request
     * @return Login redirect or the form containing a validation error
     * @throws Exception If storage or printing fails
     */
    @PostMapping(
        value = "/users",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
        produces = MediaType.TEXT_HTML_VALUE
    )
    public ResponseEntity<String> registerForm(
        @RequestParam final MultiValueMap<String, String> parameters,
        final HttpServletRequest request
    ) throws Exception {
        final MdXslt page = UrsHttp.authentication(request, "register");
        ResponseEntity<String> response;
        try {
            new UrSignup(this.ctx, parameters).register();
            response = ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/users/login?registered")).build();
        } catch (final RegistrationException | UserCreationException exception) {
            response = ResponseEntity.badRequest().contentType(
                new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8)
            ).body(
                page.with("message", exception.getMessage())
                    .with("username", parameters.getFirst("username"))
                    .with("email", parameters.getFirst("email")).print()
            );
        }
        return response;
    }

    /**
     * Register a user.
     *
     * @param request Registration request
     * @return Created user
     * @throws Exception If registration fails
     */
    @PostMapping(
        value = "/users",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> register(
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
        final Account account = created.account();
        return ResponseEntity.created(
            URI.create(String.format("/users/%s", created.uid()))
        ).body(
            Map.of(
                "uid", created.uid(), "credentials", Map.of("username", account.username()),
                "roles", created.roles(),
                "info", Map.of("corporate", account.corporate(), "alive", account.alive()),
                "admin", new RtAdmin(created).allows()
            )
        );
    }

    /**
     * Remove user.
     *
     * @param user User's Id
     * @param actor Acting user
     * @return Nothing
     * @throws Exception If remove fails
     */
    @DeleteMapping("/users/{user}")
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
    @PostMapping("/users/{user}/roles")
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
                    "/users/%d/roles/%d",
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
    @GetMapping("/users/{user}/roles")
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
    @DeleteMapping("/users/{user}/roles/{role}")
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

    /**
     * Render the current user's profile and school context.
     *
     * @param user Current user
     * @param request HTTP request
     * @return Profile page
     * @throws Exception If printing fails
     */
    @GetMapping(value = "/users/profile", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> profile(
        @AuthenticationPrincipal final AuthUser user,
        final HttpServletRequest request
    ) throws Exception {
        return ResponseEntity.ok().contentType(
            new MediaType(new AcHttp(request).type(), StandardCharsets.UTF_8)
        ).body(
            new SrWeb(
                new UrBrief(user), new SlsBrief(new SlsPostgres(this.ctx)),
                new CsToken((CsrfToken) request.getAttribute(CsrfToken.class.getName()))
            ).print(
                new MdXslt(
                    new MdXml("surok").with("path", request.getRequestURI()),
                    new XsClasspath("xslt/profile.xsl", "profile")
                )
            ).with("account", new UrProfile(user)).with(
                "memberships", new PrItems(
                    "membership", StreamSupport.stream(
                        user.schools().iterate().spliterator(), false
                    ).map(school -> new UrMembership(school, user)).toList()
                )
            ).print()
        );
    }

    /**
     * Render the personal subscription page.
     *
     * @param user Current user
     * @param request HTTP request
     * @return Subscription page or profile redirect
     * @throws Exception If printing or lookup fails
     */
    @GetMapping(value = "/users/subscription", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> subscription(
        @AuthenticationPrincipal final AuthUser user,
        final HttpServletRequest request
    ) throws Exception {
        final MediaType type = new AcHttp(request).type();
        final ResponseEntity<String> response;
        if (user.account().corporate() || new RtAdmin(user).allows()) {
            response = ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/users/profile")).build();
        } else {
            response = ResponseEntity.ok()
                .contentType(new MediaType(type, StandardCharsets.UTF_8)).body(
                    new SrWeb(
                        new UrBrief(user), new SlsBrief(new SlsPostgres(this.ctx)),
                        new CsToken((CsrfToken) request.getAttribute(CsrfToken.class.getName()))
                    ).print(
                        new MdXslt(
                            new MdXml("surok").with("path", request.getRequestURI()),
                            new XsClasspath("xslt/subscription.xsl", "subscription")
                        )
                    ).with(
                        "subscription",
                        new SpWeb(new PgSubscriptions(this.ctx, user).subscription())
                    ).with("activated", request.getParameterMap().containsKey("activated"))
                        .with("cancelled", request.getParameterMap().containsKey("cancelled"))
                        .with("error", request.getParameterMap().containsKey("error")).print()
                );
        }
        return response;
    }

    /**
     * Activate a non-basic plan for thirty days in the existing test mode.
     *
     * @param user Current user
     * @param plan Selected plan
     * @param request HTTP request
     * @return Subscription or profile redirect
     * @throws Exception If activation fails
     */
    @PostMapping(
        value = "/users/subscription/checkout",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
        produces = MediaType.TEXT_HTML_VALUE
    )
    public ResponseEntity<Void> checkout(
        @AuthenticationPrincipal final AuthUser user,
        @RequestParam final String plan,
        final HttpServletRequest request
    ) throws Exception {
        new AcHttp(request).type();
        String target = "/users/profile";
        if (!user.account().corporate() && !new RtAdmin(user).allows()) {
            final Subscription.Plan selected;
            try {
                selected = Subscription.Plan.valueOf(plan);
            } catch (final IllegalArgumentException exception) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Unknown subscription plan", exception
                );
            }
            target = "/users/subscription";
            if (selected != Subscription.Plan.BASIC) {
                new PgSubscriptions(this.ctx, user).subscription(selected);
                target = String.format("/users/subscription?activated=%s", selected.name());
            }
        }
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(target)).build();
    }

    /**
     * Cancel the current plan and restore the basic subscription roles.
     *
     * @param user Current user
     * @param request HTTP request
     * @return Subscription or profile redirect
     * @throws Exception If cancellation fails
     */
    @PostMapping(
        value = "/users/subscription/cancel",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
        produces = MediaType.TEXT_HTML_VALUE
    )
    public ResponseEntity<Void> cancel(
        @AuthenticationPrincipal final AuthUser user,
        final HttpServletRequest request
    ) throws Exception {
        new AcHttp(request).type();
        String target = "/users/profile";
        if (!user.account().corporate() && !new RtAdmin(user).allows()) {
            new PgSubscriptions(this.ctx, user).subscription(Subscription.Plan.BASIC);
            target = "/users/subscription?cancelled";
        }
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(target)).build();
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
        value = "/schools/{school}/users",
        produces = {
            "application/vnd.surok.school-users.v1+json",
            "application/vnd.surok.school-users.v1+xml"
        }
    )
    public ResponseEntity<?> schoolUsers(
        @PathVariable final long school, final HttpServletRequest request,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        final MediaType type = new AcHttp(
            request, List.of(
                MediaType.parseMediaType("application/vnd.surok.school-users.v1+json"),
                MediaType.parseMediaType("application/vnd.surok.school-users.v1+xml")
            )
        ).type();
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
        value = "/schools/{school}/users/{user}",
        produces = {
            "application/vnd.surok.school-user.v1+json",
            "application/vnd.surok.school-user.v1+xml"
        }
    )
    public ResponseEntity<?> addSchoolUser(
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
        value = "/schools/{school}/users",
        consumes = "application/vnd.surok.school-user.v1+json",
        produces = {
            "application/vnd.surok.school-user.v1+json",
            "application/vnd.surok.school-user.v1+xml"
        }
    )
    public ResponseEntity<?> attachSchoolUser(
        @PathVariable final long school, @RequestBody final JsonNode document,
        final HttpServletRequest request, @AuthenticationPrincipal final User actor
    ) throws Exception {
        final JsonNode login = document.get("login");
        if (login == null || login.asText().isBlank()) {
            throw new RoleAssignmentException("Login is required");
        }
        return this.addSchoolUser(
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
    @DeleteMapping("/schools/{school}/users/{user}")
    public ResponseEntity<Void> removeSchoolUser(
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
        value = "/schools/{school}/users/{user}/roles",
        produces = "application/vnd.surok.school-roles.v1+json"
    )
    public ResponseEntity<List<Role>> schoolRoles(
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
        value = "/schools/{school}/users/{user}/roles",
        consumes = "application/vnd.surok.school-role.v1+json",
        produces = "application/vnd.surok.school-role.v1+json"
    )
    public ResponseEntity<Role> grantSchoolRole(
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
    @DeleteMapping("/schools/{school}/users/{user}/roles/{role}")
    public ResponseEntity<Void> revokeSchoolRole(
        @PathVariable final long school, @PathVariable final long user,
        @PathVariable final long role, @AuthenticationPrincipal final User actor
    ) throws Exception {
        final Users members = this.members(actor, school);
        members.roles(members.identification(user)).revoke(role);
        return ResponseEntity.noContent().build();
    }

    private static MdXslt authentication(final HttpServletRequest request, final String view)
        throws Exception {
        new AcHttp(request).type();
        return new MdXslt(
            new MdXml("surok").with("path", request.getRequestURI()),
            new XsClasspath("xslt/authentication.xsl", view)
        ).with(
            "csrf", new CsToken((CsrfToken) request.getAttribute(CsrfToken.class.getName()))
        ).with("error", request.getParameter("error"))
            .with("logout", request.getParameterMap().containsKey("logout"))
            .with("registered", request.getParameterMap().containsKey("registered"));
    }

    private Users members(final User actor, final long id) throws Exception {
        return new SlsAuthorized(new SlsPostgres(this.ctx), actor).school(id).users();
    }
}
