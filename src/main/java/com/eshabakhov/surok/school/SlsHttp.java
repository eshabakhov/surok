/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school;

import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.Schools;
import com.eshabakhov.surok.Sorts;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.media.AcHttp;
import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.media.MdXml;
import com.eshabakhov.surok.media.MdXslt;
import com.eshabakhov.surok.media.SrWeb;
import com.eshabakhov.surok.media.XsClasspath;
import com.eshabakhov.surok.school.input.SchoolInputException;
import com.eshabakhov.surok.school.input.SlInputHttp;
import com.eshabakhov.surok.security.CsToken;
import com.eshabakhov.surok.user.UrBrief;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.jooq.DSLContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Schools HTTP resource, for example GET /schools with Accept text/html.
 *
 * @since 0.0.1
 * @checkstyle NoJavadocForPrivateMethodsCheck (1000 lines)
 * @checkstyle ClassFanOutComplexityCheck (1000 lines)
 * @checkstyle ParameterNumberCheck (1000 lines)
 * @checkstyle NonStaticMethodCheck (1000 lines)
 */
@RestController
@RequestMapping("/schools")
public class SlsHttp {

    /** JSON representation of the school collection. */
    private static final String JSON = "application/vnd.surok.schools.v1+json";

    /** XML representation of the school collection. */
    private static final String XML = "application/vnd.surok.schools.v1+xml";

    /** HTML representation containing only the results. */
    private static final String FRAGMENT = "text/vnd.surok.schools-fragment+html";

    /** Database context. */
    private final DSLContext ctx;

    /**
     * New school resource.
     *
     * @param ctx Database context
     */
    public SlsHttp(final DSLContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Prints the collection or its results fragment.
     *
     * @param filters Applied filters
     * @param page Requested page
     * @param sorts Applied sorts
     * @param request HTTP request
     * @param user Acting user
     * @return Negotiated representation
     * @throws Exception If selection or printing fails
     */
    @GetMapping(produces = {MediaType.TEXT_HTML_VALUE, SlsHttp.JSON, SlsHttp.XML, SlsHttp.FRAGMENT})
    public ResponseEntity<?> selection(
        final Filters filters, final Page page, final Sorts sorts,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User user
    ) throws Exception {
        final MediaType type = new AcHttp(
            request,
            List.of(
                MediaType.TEXT_HTML,
                MediaType.valueOf(SlsHttp.JSON),
                MediaType.valueOf(SlsHttp.XML),
                MediaType.valueOf(SlsHttp.FRAGMENT)
            )
        ).type();
        final Schools selected = new SlsAuthorized(
            new SlsPostgres(this.ctx), user
        ).selection(filters, page, sorts);
        return ResponseEntity.ok().contentType(new MediaType(type, StandardCharsets.UTF_8))
            .varyBy(HttpHeaders.ACCEPT).body(
                switch (type.toString()) {
                    case MediaType.TEXT_HTML_VALUE, SlsHttp.FRAGMENT -> {
                        final String view;
                        if (MediaType.TEXT_HTML.equals(type)) {
                            view = "list";
                        } else {
                            view = "list-fragment";
                        }
                        yield this.web(
                            request, "xslt/schools.xsl", view,
                            new SlsWeb(selected, filters, sorts)
                        ).print();
                    }
                    case SlsHttp.JSON -> selected.print(new MdJson()).print();
                    case SlsHttp.XML -> selected.print(new MdXml("schools")).print();
                    case null, default -> throw new ResponseStatusException(
                        HttpStatus.NOT_ACCEPTABLE, "Unsupported Accept header"
                    );
                }
            );
    }

    /**
     * Prints the browser form for creating a school.
     *
     * @param request HTTP request
     * @return HTML creation form
     * @throws Exception If rendering fails
     */
    @GetMapping(value = "/create", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> form(final HttpServletRequest request) throws Exception {
        return ResponseEntity.ok().contentType(
            new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8)
        ).body(
            this.web(
                request, "xslt/schools.xsl", "create", new SlsBrief(new SlsPostgres(this.ctx))
            ).print()
        );
    }

    /**
     * Prints one school or its editing form.
     *
     * @param school School identifier
     * @param request HTTP request
     * @param user Acting user
     * @return Negotiated representation
     * @throws Exception If lookup or printing fails
     */
    @GetMapping(
        value = {"/{school}", "/{school}/edit"},
        produces = {MediaType.TEXT_HTML_VALUE,
            "application/vnd.surok.school-v1+json", "application/vnd.surok.school-v1+xml"}
    )
    public ResponseEntity<?> school(
        @PathVariable final long school, final HttpServletRequest request,
        @AuthenticationPrincipal final User user
    ) throws Exception {
        final MediaType type = new AcHttp(request, "school-v1").type();
        final Schools origin = new SlsAuthorized(new SlsPostgres(this.ctx), user);
        final School selected = origin.school(school);
        return ResponseEntity.ok().contentType(new MediaType(type, StandardCharsets.UTF_8)).body(
            switch (type.toString()) {
                case MediaType.TEXT_HTML_VALUE -> {
                    final String view;
                    if (request.getRequestURI().endsWith("/edit")) {
                        view = "edit";
                    } else {
                        view = "details";
                    }
                    yield this.web(
                        request, "xslt/school.xsl", view, new SlsSingle(origin, selected)
                    ).with("school", new SlBrief(selected)).print();
                }
                case "application/vnd.surok.school-v1+json" ->
                    new SlV1(selected).print(new MdJson()).print();
                case "application/vnd.surok.school-v1+xml" ->
                    new SlV1(selected).print(new MdXml("school")).print();
                case null, default -> throw new ResponseStatusException(
                    HttpStatus.NOT_ACCEPTABLE, "Unsupported Accept header"
                );
            }
        );
    }

    /**
     * Creates a school from form fields.
     *
     * @param parameters Form fields
     * @param request HTTP request
     * @param user Acting user
     * @return Created representation or redirect
     * @throws Exception If creation fails
     */
    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> create(
        @RequestParam final MultiValueMap<String, String> parameters,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User user
    ) throws Exception {
        return this.save(request, new SlInputHttp(parameters), 0L, true, user);
    }

    /**
     * Creates a school from a JSON document.
     *
     * @param json School fields
     * @param request HTTP request
     * @param user Acting user
     * @return Created representation
     * @throws Exception If creation fails
     */
    @PostMapping(
        consumes = {MediaType.APPLICATION_JSON_VALUE, "application/vnd.surok.school-v1+json"}
    )
    public ResponseEntity<?> create(
        @RequestBody final JsonNode json, final HttpServletRequest request,
        @AuthenticationPrincipal final User user
    ) throws Exception {
        return this.save(request, new SlInputHttp(json), 0L, true, user);
    }

    /**
     * Revises an existing school from JSON without creating a replacement.
     *
     * @param school School identifier
     * @param json School fields
     * @param request HTTP request
     * @param user Acting user
     * @return Revised representation
     * @throws Exception If revision fails
     */
    @PutMapping(
        value = "/{school}",
        consumes = {MediaType.APPLICATION_JSON_VALUE, "application/vnd.surok.school-v1+json"}
    )
    public ResponseEntity<?> revise(
        @PathVariable final long school, @RequestBody final JsonNode json,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User user
    ) throws Exception {
        return this.save(request, new SlInputHttp(json), school, false, user);
    }

    /**
     * Soft deletes a school.
     *
     * @param school School identifier
     * @param user Acting user
     * @return Empty response
     * @throws Exception If removal fails
     */
    @DeleteMapping("/{school}")
    public ResponseEntity<Void> remove(
        @PathVariable final long school,
        @AuthenticationPrincipal final User user
    ) throws Exception {
        new SlsAuthorized(new SlsPostgres(this.ctx), user).remove(school);
        return ResponseEntity.noContent().build();
    }

    /**
     * Negotiates the response before applying a validated mutation.
     *
     * @param request HTTP request
     * @param input School input
     * @param id School identifier
     * @param creation Whether to create a school
     * @param user Acting user
     * @return Mutation response
     * @throws Exception If validation, lookup or printing fails
     */
    private ResponseEntity<?> save(
        final HttpServletRequest request, final SchoolInput input,
        final long id, final boolean creation, final User user
    ) throws Exception {
        final MediaType type = new AcHttp(request, "school-v1").type();
        ResponseEntity<?> response;
        try {
            final School selected;
            final HttpStatus status;
            if (creation) {
                selected = new SlsAuthorized(new SlsPostgres(this.ctx), user).create(input);
                status = HttpStatus.CREATED;
            } else {
                selected = new SlsAuthorized(
                    new SlsPostgres(this.ctx), user
                ).revise(id, input);
                status = HttpStatus.OK;
            }
            final URI location = URI.create(String.format("/schools/%d", selected.uid()));
            final ResponseEntity.BodyBuilder builder = ResponseEntity.status(status)
                .contentType(new MediaType(type, StandardCharsets.UTF_8));
            if (HttpStatus.CREATED.equals(status)) {
                builder.location(location);
            }
            response = switch (type.toString()) {
                case MediaType.TEXT_HTML_VALUE -> ResponseEntity.status(HttpStatus.FOUND)
                    .location(location).build();
                case "application/vnd.surok.school-v1+json" ->
                    builder.body(new SlV1(selected).print(new MdJson()).print());
                case "application/vnd.surok.school-v1+xml" ->
                    builder.body(new SlV1(selected).print(new MdXml("school")).print());
                case null, default -> throw new ResponseStatusException(
                    HttpStatus.NOT_ACCEPTABLE, "Unsupported Accept header"
                );
            };
        } catch (final SchoolInputException exception) {
            if (!MediaType.TEXT_HTML.equals(type)) {
                throw exception;
            }
            if (creation) {
                response = ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create("/schools/create?error=empty")).build();
            } else {
                response = ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(String.format("/schools/%d/edit?error=empty", id)))
                    .build();
            }
        }
        return response;
    }

    /**
     * Builds shared browser data and renders the requested stylesheet.
     *
     * @param request HTTP request
     * @param stylesheet Stylesheet path
     * @param view Selected view
     * @param schools Printed school collection
     * @return Browser medium
     * @throws Exception If printing fails
     */
    private Media<String> web(
        final HttpServletRequest request, final String stylesheet,
        final String view, final Schools schools
    ) throws Exception {
        return new SrWeb(
            new UrBrief(
                (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()
            ), schools,
            new CsToken((CsrfToken) request.getAttribute(CsrfToken.class.getName()))
        ).print(
            new MdXslt(
                new MdXml("surok").with("path", request.getRequestURI()),
                new XsClasspath(stylesheet, view)
            )
        ).with("error", request.getParameter("error") != null);
    }
}
