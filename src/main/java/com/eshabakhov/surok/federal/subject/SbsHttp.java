/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject;

import com.eshabakhov.surok.Filter;
import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.Sorts;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.federal.subject.input.SbInputHttp;
import com.eshabakhov.surok.federal.subject.input.SubjectInputException;
import com.eshabakhov.surok.filter.FilterRequest;
import com.eshabakhov.surok.filter.FiltersRequest;
import com.eshabakhov.surok.media.AcHttp;
import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.media.MdXml;
import com.eshabakhov.surok.media.MdXslt;
import com.eshabakhov.surok.media.SrWeb;
import com.eshabakhov.surok.media.XsClasspath;
import com.eshabakhov.surok.school.SlBrief;
import com.eshabakhov.surok.school.SlsAuthorized;
import com.eshabakhov.surok.school.SlsBrief;
import com.eshabakhov.surok.school.SlsPostgres;
import com.eshabakhov.surok.security.CsToken;
import com.eshabakhov.surok.user.Roles;
import com.eshabakhov.surok.user.UrBrief;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.jooq.DSLContext;
import org.springframework.http.HttpHeaders;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unified subject HTTP resource; Accept selects HTML or versioned JSON/XML.
 * For example, request application/vnd.surok.subjects.v1+json for the collection.
 *
 * @since 0.0.1
 * @checkstyle ClassFanOutComplexityCheck (1000 lines)
 * @checkstyle ParameterNumberCheck (1000 lines)
 */
@RestController
@RequestMapping({"/federal/subjects", "/schools/{school}/subjects"})
public class SbsHttp {

    /** JSON representation of the subject collection. */
    private static final String JSON = "application/vnd.surok.subjects.v1+json";

    /** XML representation of the subject collection. */
    private static final String XML = "application/vnd.surok.subjects.v1+xml";

    /** HTML representation containing only the subject results. */
    private static final String FRAGMENT = "text/vnd.surok.subjects-fragment+html";

    /** Database context. */
    private final DSLContext ctx;

    /**
     * New resource.
     *
     * @param ctx Database context
     */
    public SbsHttp(final DSLContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Shows a collection or its results fragment.
     *
     * @param school School identifier, absent for the shared federal catalogue
     * @param filters Filtering
     * @param page Pagination
     * @param sorts Sorting
     * @param request HTTP request
     * @param actor Acting user
     * @return Negotiated representation
     * @throws Exception If lookup or printing fails
     */
    @GetMapping(produces = {MediaType.TEXT_HTML_VALUE, SbsHttp.JSON, SbsHttp.XML, SbsHttp.FRAGMENT})
    public ResponseEntity<?> selection(
        @PathVariable(required = false) final Long school,
        final Filters filters,
        final Page page,
        final Sorts sorts,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        final MediaType type = new AcHttp(
            request,
            List.of(
                MediaType.TEXT_HTML, MediaType.valueOf(SbsHttp.JSON),
                MediaType.valueOf(SbsHttp.XML), MediaType.valueOf(SbsHttp.FRAGMENT)
            )
        ).type();
        final Set<Filter> requested = new LinkedHashSet<>(filters.filters());
        requested.addAll(
            Stream.of("name", "name_ct")
                .filter(name -> request.getParameter(name) != null)
                .map(name -> new FilterRequest(name, request.getParameter(name))).toList()
        );
        final Filters applied = new FiltersRequest(requested);
        final Subjects selected = this.subjects(school, actor).selection(
            applied, page, sorts
        );
        return ResponseEntity.ok().contentType(new MediaType(type, StandardCharsets.UTF_8))
            .varyBy(HttpHeaders.ACCEPT).body(
                switch (type.toString()) {
                    case MediaType.TEXT_HTML_VALUE, SbsHttp.FRAGMENT -> {
                        final String view;
                        if (MediaType.TEXT_HTML.equals(type)) {
                            view = "list";
                        } else {
                            view = "list-fragment";
                        }
                        yield this.web(request, school, view, actor).with(
                            "subjects", new SbsWeb(selected, applied, sorts)
                        ).print();
                    }
                    case SbsHttp.JSON -> selected.print(new MdJson()).print();
                    case SbsHttp.XML -> selected.print(new MdXml("subjects")).print();
                    case null, default -> throw new ResponseStatusException(
                        HttpStatus.NOT_ACCEPTABLE, "Unsupported Accept header"
                    );
                }
            );
    }

    /**
     * Shows the browser form for creating a subject.
     *
     * @param school School identifier, absent for the shared federal catalogue
     * @param request HTTP request
     * @param actor Acting user
     * @return HTML creation form
     * @throws Exception If lookup or printing fails
     */
    @GetMapping(value = "/create", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> form(
        @PathVariable(required = false) final Long school,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        final MediaType type = new AcHttp(request, List.of(MediaType.TEXT_HTML)).type();
        if (school == null && !actor.isAdmin()) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Federal catalogue is read-only"
            );
        }
        this.subjects(school, actor);
        return ResponseEntity.ok().contentType(new MediaType(type, StandardCharsets.UTF_8)).body(
            this.web(request, school, "create", actor).print()
        );
    }

    /**
     * Shows one subject.
     *
     * @param school School identifier, absent for the shared federal catalogue
     * @param subject Entity identifier
     * @param request HTTP request
     * @param actor Acting user
     * @return Negotiated representation
     * @throws Exception If lookup or printing fails
     */
    @GetMapping(
        value = "/{subject}",
        produces = {
            MediaType.TEXT_HTML_VALUE,
            "application/vnd.surok.subject-v1+json",
            "application/vnd.surok.subject-v1+xml"
        }
    )
    public ResponseEntity<?> subject(
        @PathVariable(required = false) final Long school,
        @PathVariable final long subject,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        final MediaType type = new AcHttp(request, "subject-v1").type();
        final Subjects origin = this.subjects(school, actor);
        final Subject selected = origin.subject(subject);
        return ResponseEntity.ok().contentType(new MediaType(type, StandardCharsets.UTF_8)).body(
            switch (type.toString()) {
                case MediaType.TEXT_HTML_VALUE ->
                    this.web(request, school, "details", actor).with(
                        "subjects", new SbsSingle(origin, selected)
                    ).print();
                case "application/vnd.surok.subject-v1+json" ->
                    new SbV1(selected).print(new MdJson()).print();
                case "application/vnd.surok.subject-v1+xml" ->
                    new SbV1(selected).print(new MdXml("subject")).print();
                case null, default -> throw new ResponseStatusException(
                    HttpStatus.NOT_ACCEPTABLE, "Unsupported Accept header"
                );
            }
        );
    }

    /**
     * Shows the browser form for editing a subject.
     *
     * @param school School identifier, absent for the shared federal catalogue
     * @param subject Entity identifier
     * @param request HTTP request
     * @param actor Acting user
     * @return HTML editing form
     * @throws Exception If lookup or printing fails
     */
    @GetMapping(value = "/{subject}/edit", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> edit(
        @PathVariable(required = false) final Long school,
        @PathVariable final long subject,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        final MediaType type = new AcHttp(request, List.of(MediaType.TEXT_HTML)).type();
        final Subjects origin = this.subjects(school, actor);
        final Subject selected = origin.subject(subject);
        if (school == null && !actor.isAdmin()
            || school != null && selected.origin() == Subject.Origin.FEDERAL) {
            throw new SubjectReadOnlyException(selected.uid());
        }
        return ResponseEntity.ok().contentType(new MediaType(type, StandardCharsets.UTF_8)).body(
            this.web(request, school, "edit", actor).with(
                "subjects", new SbsSingle(origin, selected)
            ).print()
        );
    }

    /**
     * Creates a subject from form fields.
     *
     * @param school School identifier, absent for the shared federal catalogue
     * @param parameters Form fields
     * @param request HTTP request
     * @param actor Acting user
     * @return Created representation or redirect
     * @throws Exception If creation fails
     */
    @PostMapping(
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
        produces = {
            MediaType.TEXT_HTML_VALUE,
            "application/vnd.surok.subject-v1+json",
            "application/vnd.surok.subject-v1+xml"
        }
    )
    public ResponseEntity<?> create(
        @PathVariable(required = false) final Long school,
        @RequestParam final MultiValueMap<String, String> parameters,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        final MediaType type = new AcHttp(request, "subject-v1").type();
        ResponseEntity<?> response;
        try {
            response = SbsHttp.result(
                type,
                this.subjects(school, actor)
                    .create(new SbInputHttp(parameters)), HttpStatus.CREATED,
                SbsHttp.base(school),
                SbsHttp.base(school)
            );
        } catch (final SubjectInputException | SubjectAlreadyExistsException exception) {
            response = SbsHttp.rejected(type, request, exception);
        }
        return response;
    }

    /**
     * Creates a subject from a JSON document.
     *
     * @param school School identifier, absent for the shared federal catalogue
     * @param json Input document
     * @param request HTTP request
     * @param actor Acting user
     * @return Created representation
     * @throws Exception If creation fails
     */
    @PostMapping(
        consumes = "application/vnd.surok.subject-v1+json",
        produces = {
            MediaType.TEXT_HTML_VALUE,
            "application/vnd.surok.subject-v1+json",
            "application/vnd.surok.subject-v1+xml"
        }
    )
    public ResponseEntity<?> create(
        @PathVariable(required = false) final Long school,
        @RequestBody final JsonNode json,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        return SbsHttp.result(
            new AcHttp(request, "subject-v1").type(),
            this.subjects(school, actor)
                .create(new SbInputHttp(json)), HttpStatus.CREATED,
            SbsHttp.base(school),
            SbsHttp.base(school)
        );
    }

    /**
     * Revises a subject from a JSON document.
     *
     * @param school School identifier, absent for the shared federal catalogue
     * @param subject Entity identifier
     * @param json Input document
     * @param request HTTP request
     * @param actor Acting user
     * @return Revised representation or redirect
     * @throws Exception If revision fails
     */
    @PutMapping(
        value = "/{subject}",
        consumes = "application/vnd.surok.subject-v1+json",
        produces = {
            MediaType.TEXT_HTML_VALUE,
            "application/vnd.surok.subject-v1+json",
            "application/vnd.surok.subject-v1+xml"
        }
    )
    public ResponseEntity<?> revise(
        @PathVariable(required = false) final Long school,
        @PathVariable final long subject,
        @RequestBody final JsonNode json,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        final MediaType type = new AcHttp(request, "subject-v1").type();
        final Subject revised = this.subjects(school, actor)
            .revise(subject, new SbInputHttp(json));
        return SbsHttp.result(
            type, revised, HttpStatus.OK, SbsHttp.base(school),
            String.format("%s/%d", SbsHttp.base(school), revised.uid())
        );
    }

    /**
     * Removes a subject.
     *
     * @param school School identifier, absent for the shared federal catalogue
     * @param subject Entity identifier
     * @param request HTTP request
     * @param actor Acting user
     * @return Empty response
     * @throws Exception If removal fails
     */
    @DeleteMapping(
        value = "/{subject}", produces = {
            MediaType.TEXT_HTML_VALUE, "application/vnd.surok.subject-v1+json",
            "application/vnd.surok.subject-v1+xml"
        }
    )
    public ResponseEntity<Void> remove(
        @PathVariable(required = false) final Long school,
        @PathVariable final long subject, final HttpServletRequest request,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        new AcHttp(request, "subject-v1").type();
        this.subjects(school, actor).remove(subject);
        return ResponseEntity.noContent().build();
    }

    private Subjects subjects(final Long school, final User actor) throws Exception {
        final Subjects subjects;
        if (school == null) {
            subjects = new SbsFederalAuthorized(new SbsFederal(this.ctx), actor);
        } else {
            subjects = new SlsAuthorized(new SlsPostgres(this.ctx), actor)
                .school(school).subjects();
        }
        return subjects;
    }

    private static String base(final Long school) {
        final String path;
        if (school == null) {
            path = "/federal/subjects";
        } else {
            path = String.format("/schools/%d/subjects", school);
        }
        return path;
    }

    private static ResponseEntity<?> result(
        final MediaType type,
        final Subject selected,
        final HttpStatus status,
        final String base,
        final String redirect
    ) throws Exception {
        return switch (type.toString()) {
            case MediaType.TEXT_HTML_VALUE -> ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirect)).build();
            case "application/vnd.surok.subject-v1+json" ->
                ResponseEntity.status(status)
                    .location(URI.create(String.format("%s/%d", base, selected.uid())))
                    .contentType(new MediaType(type, StandardCharsets.UTF_8))
                    .body(new SbV1(selected).print(new MdJson()).print());
            case "application/vnd.surok.subject-v1+xml" ->
                ResponseEntity.status(status)
                    .location(URI.create(String.format("%s/%d", base, selected.uid())))
                    .contentType(new MediaType(type, StandardCharsets.UTF_8))
                    .body(new SbV1(selected).print(new MdXml("subject")).print());
            case null, default -> throw new ResponseStatusException(
                HttpStatus.NOT_ACCEPTABLE, "Unsupported Accept header"
            );
        };
    }

    private static ResponseEntity<?> rejected(
        final MediaType type,
        final HttpServletRequest request,
        final Exception exception
    ) throws Exception {
        if (!MediaType.TEXT_HTML.equals(type)) {
            throw exception;
        }
        String error = "empty";
        if (exception instanceof SubjectAlreadyExistsException) {
            error = "duplicate";
        }
        return ResponseEntity.status(HttpStatus.FOUND).location(
            URI.create(String.format("%s/create?error=%s", request.getRequestURI(), error))
        ).build();
    }

    private Media<String> web(
        final HttpServletRequest request,
        final Long school,
        final String view,
        final User actor
    ) throws Exception {
        boolean editable = actor.isAdmin();
        if (school != null && !editable) {
            final Roles roles = new SlsAuthorized(
                new SlsPostgres(this.ctx), actor
            ).school(school).users().roles(actor);
            editable = roles.contains(Roles.RoleEnum.DIRECTOR)
                || roles.contains(Roles.RoleEnum.DEPUTY_DIRECTOR);
        }
        Media<String> media = new SrWeb(
            new UrBrief(actor),
            new SlsBrief(new SlsPostgres(this.ctx)),
            new CsToken((CsrfToken) request.getAttribute(CsrfToken.class.getName()))
        ).print(
            new MdXslt(
                new MdXml("surok").with("path", request.getRequestURI()),
                new XsClasspath("xslt/subjects.xsl", view)
            )
        ).with("federal", school == null).with("error", request.getParameter("error"))
            .with("editable", editable);
        if (school != null) {
            media = media.with(
                "school", new SlBrief(
                    new SlsAuthorized(new SlsPostgres(this.ctx), actor).school(school)
                )
            );
        }
        return media;
    }
}
