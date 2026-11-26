/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.teacher;

import com.eshabakhov.surok.Filter;
import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.Sorts;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.filter.FilterRequest;
import com.eshabakhov.surok.filter.FiltersRequest;
import com.eshabakhov.surok.media.AcHttp;
import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.media.MdXml;
import com.eshabakhov.surok.media.MdXslt;
import com.eshabakhov.surok.media.SrWeb;
import com.eshabakhov.surok.media.XsClasspath;
import com.eshabakhov.surok.school.SlBrief;
import com.eshabakhov.surok.school.SlPostgres;
import com.eshabakhov.surok.school.SlsBrief;
import com.eshabakhov.surok.school.SlsPostgres;
import com.eshabakhov.surok.school.teacher.input.TeacherInputException;
import com.eshabakhov.surok.school.teacher.input.ThInput;
import com.eshabakhov.surok.school.teacher.input.ThInputHttp;
import com.eshabakhov.surok.security.CsToken;
import com.eshabakhov.surok.security.OaYaml;
import com.eshabakhov.surok.security.Operations;
import com.eshabakhov.surok.user.UrAcBrief;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
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
 * Unified teacher HTTP resource; Accept selects HTML or versioned JSON/XML.
 * For example, request application/vnd.surok.teachers.v1+json for the collection.
 *
 * @since 0.0.1
 * @checkstyle ClassFanOutComplexityCheck (1000 lines)
 * @checkstyle ParameterNumberCheck (1000 lines)
 */
@RestController
@RequestMapping("/schools/{school}/teachers")
public class ThsHttp {

    /** JSON representation of the teacher collection. */
    private static final String JSON = "application/vnd.surok.teachers.v1+json";

    /** XML representation of the teacher collection. */
    private static final String XML = "application/vnd.surok.teachers.v1+xml";

    /** HTML representation containing only the teacher results. */
    private static final String FRAGMENT = "text/vnd.surok.teachers-fragment+html";

    /** Shared operation declarations. */
    private final Operations operations;

    /** Database context. */
    private final DSLContext ctx;

    /**
     * New resource with explicitly constructed YAML operations.
     *
     * @param ctx Database context
     * @throws IOException If the API description cannot be read
     */
    @Autowired
    public ThsHttp(final DSLContext ctx) throws IOException {
        this(ctx, new OaYaml().operations());
    }

    /**
     * New controller using the application operation catalog.
     *
     * @param ctx Database context
     * @param operations Shared operation declarations
     */
    public ThsHttp(final DSLContext ctx, final Operations operations) {
        this.ctx = ctx;
        this.operations = operations;
    }

    /**
     * Shows a collection or its results fragment.
     *
     * @param school School identifier
     * @param filters Filtering
     * @param page Pagination
     * @param sorts Sorting
     * @param request HTTP request
     * @param actor Acting user
     * @return Negotiated representation
     * @throws Exception If lookup or printing fails
     */
    @GetMapping(produces = {MediaType.TEXT_HTML_VALUE, ThsHttp.JSON, ThsHttp.XML, ThsHttp.FRAGMENT})
    public ResponseEntity<?> selection(
        @PathVariable final long school,
        final Filters filters,
        final Page page,
        final Sorts sorts,
        final HttpServletRequest request,
        @AuthenticationPrincipal final UrAccount actor
    ) throws Exception {
        final MediaType type = new AcHttp(
            request,
            List.of(
                MediaType.TEXT_HTML,
                MediaType.valueOf(ThsHttp.JSON),
                MediaType.valueOf(ThsHttp.XML),
                MediaType.valueOf(ThsHttp.FRAGMENT)
            )
        ).type();
        final School sch = this.operations.operation("Teachers.selection")
            .permission(actor, new SlPostgres(this.ctx, school)).permit(actor)
            .schools().school(school);
        final Set<Filter> requested = new LinkedHashSet<>(filters.filters());
        requested.addAll(
            Stream.of("name", "name_ct")
                .filter(name -> request.getParameter(name) != null)
                .map(name -> new FilterRequest(name, request.getParameter(name))).toList()
        );
        final Filters applied = new FiltersRequest(requested);
        final Teachers selected = sch.teachers().selection(
            applied, page, sorts
        );
        return ResponseEntity.ok().contentType(new MediaType(type, StandardCharsets.UTF_8))
            .varyBy(HttpHeaders.ACCEPT).body(
                switch (type.toString()) {
                    case MediaType.TEXT_HTML_VALUE, ThsHttp.FRAGMENT -> {
                        final String view;
                        if (MediaType.TEXT_HTML.equals(type)) {
                            view = "list";
                        } else {
                            view = "list-fragment";
                        }
                        yield this.web(request, sch, view).with(
                            "teachers", new ThsWeb(selected, applied, sorts)
                        ).print();
                    }
                    case ThsHttp.JSON -> selected.print(new MdJson()).print();
                    case ThsHttp.XML -> selected.print(new MdXml("teachers")).print();
                    case null, default -> throw new ResponseStatusException(
                        HttpStatus.NOT_ACCEPTABLE, "Unsupported Accept header"
                    );
                }
            );
    }

    /**
     * Shows the browser form for creating a teacher.
     *
     * @param school School identifier
     * @param request HTTP request
     * @param actor Acting user
     * @return HTML creation form
     * @throws Exception If lookup or printing fails
     */
    @GetMapping(value = "/create", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> form(
        @PathVariable final long school, final HttpServletRequest request,
        @AuthenticationPrincipal final UrAccount actor
    ) throws Exception {
        return ResponseEntity.ok().contentType(
            new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8)
        ).body(
            this.web(
                request, this.operations.operation("Teachers.form")
                    .permission(actor, new SlPostgres(this.ctx, school)).permit(actor)
                    .schools().school(school),
                "create"
            ).with("teacher", new ThDraft(new ThInput())).print()
        );
    }

    /**
     * Shows one teacher or its editing form.
     *
     * @param school School identifier
     * @param teacher Entity identifier
     * @param request HTTP request
     * @param actor Acting user
     * @return Negotiated representation
     * @throws Exception If lookup or printing fails
     */
    @GetMapping(
        value = {"/{teacher}", "/{teacher}/edit"},
        produces = {
            MediaType.TEXT_HTML_VALUE,
            "application/vnd.surok.teacher-v1+json",
            "application/vnd.surok.teacher-v1+xml"
        }
    )
    public ResponseEntity<?> teacher(
        @PathVariable final long school,
        @PathVariable final long teacher,
        final HttpServletRequest request,
        @AuthenticationPrincipal final UrAccount actor
    ) throws Exception {
        final MediaType type = new AcHttp(request, "teacher-v1").type();
        final String operation;
        if (request.getRequestURI().endsWith("/edit")) {
            operation = "Teachers.edit";
        } else {
            operation = "Teachers.teacher";
        }
        final School sch = this.operations.operation(operation)
            .permission(actor, new SlPostgres(this.ctx, school)).permit(actor)
            .schools().school(school);
        final Teacher selected = sch.teachers().teacher(teacher);
        return ResponseEntity.ok().contentType(new MediaType(type, StandardCharsets.UTF_8)).body(
            switch (type.toString()) {
                case MediaType.TEXT_HTML_VALUE -> {
                    final String view;
                    if (request.getRequestURI().endsWith("/edit")) {
                        view = "edit";
                    } else {
                        view = "details";
                    }
                    yield this.web(request, sch, view).with(
                        "teacher", selected
                    ).print();
                }
                case "application/vnd.surok.teacher-v1+json" ->
                    new ThV1(selected).print(new MdJson()).print();
                case "application/vnd.surok.teacher-v1+xml" ->
                    new ThV1(selected).print(new MdXml("teacher")).print();
                case null, default -> throw new ResponseStatusException(
                    HttpStatus.NOT_ACCEPTABLE, "Unsupported Accept header"
                );
            }
        );
    }

    /**
     * Creates a teacher from form fields.
     *
     * @param school School identifier
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
            "application/vnd.surok.teacher-v1+json",
            "application/vnd.surok.teacher-v1+xml"
        }
    )
    public ResponseEntity<?> create(
        @PathVariable final long school,
        @RequestParam final MultiValueMap<String, String> parameters,
        final HttpServletRequest request,
        @AuthenticationPrincipal final UrAccount actor
    ) throws Exception {
        final MediaType type = new AcHttp(request, "teacher-v1").type();
        final School target = this.operations.operation("Teachers.create")
            .permission(actor, new SlPostgres(this.ctx, school)).permit(actor)
            .schools().school(school);
        final String base = String.format("/schools/%d/teachers", school);
        ResponseEntity<?> response;
        try {
            response = ThsHttp.result(
                type, target.teachers().create(new ThInputHttp(parameters)),
                HttpStatus.CREATED, base, base
            );
        } catch (final TeacherInputException | TeacherAlreadyExistsException exception) {
            response = ThsHttp.rejected(type, request, exception);
        }
        return response;
    }

    /**
     * Creates a teacher from a JSON document.
     *
     * @param school School identifier
     * @param json Input document
     * @param request HTTP request
     * @param actor Acting user
     * @return Created representation
     * @throws Exception If creation fails
     */
    @PostMapping(
        consumes = "application/vnd.surok.teacher-v1+json",
        produces = {
            MediaType.TEXT_HTML_VALUE,
            "application/vnd.surok.teacher-v1+json",
            "application/vnd.surok.teacher-v1+xml"
        }
    )
    public ResponseEntity<?> create(
        @PathVariable final long school,
        @RequestBody final JsonNode json,
        final HttpServletRequest request,
        @AuthenticationPrincipal final UrAccount actor
    ) throws Exception {
        final MediaType type = new AcHttp(request, "teacher-v1").type();
        final String base = String.format("/schools/%d/teachers", school);
        return ThsHttp.result(
            type, this.operations.operation("Teachers.create")
                .permission(actor, new SlPostgres(this.ctx, school)).permit(actor)
                .schools().school(school)
                .teachers().create(new ThInputHttp(json)), HttpStatus.CREATED, base, base
        );
    }

    /**
     * Revises a teacher from a JSON document.
     *
     * @param school School identifier
     * @param teacher Entity identifier
     * @param json Input document
     * @param request HTTP request
     * @param actor Acting user
     * @return Revised representation or redirect
     * @throws Exception If revision fails
     */
    @PutMapping(
        value = "/{teacher}",
        consumes = "application/vnd.surok.teacher-v1+json",
        produces = {
            MediaType.TEXT_HTML_VALUE,
            "application/vnd.surok.teacher-v1+json",
            "application/vnd.surok.teacher-v1+xml"
        }
    )
    public ResponseEntity<?> revise(
        @PathVariable final long school,
        @PathVariable final long teacher,
        @RequestBody final JsonNode json,
        final HttpServletRequest request,
        @AuthenticationPrincipal final UrAccount actor
    ) throws Exception {
        final MediaType type = new AcHttp(request, "teacher-v1").type();
        final Teacher revised = this.operations.operation("Teachers.revise")
            .permission(actor, new SlPostgres(this.ctx, school)).permit(actor)
            .schools().school(school)
            .teachers().revise(teacher, new ThInputHttp(json));
        return ThsHttp.result(
            type, revised, HttpStatus.OK, String.format("/schools/%d/teachers", school),
            String.format("/schools/%d/teachers/%d", school, revised.uid())
        );
    }

    /**
     * Removes a teacher.
     *
     * @param school School identifier
     * @param teacher Entity identifier
     * @param actor Acting user
     * @return Empty response
     * @throws Exception If removal fails
     */
    @DeleteMapping("/{teacher}")
    public ResponseEntity<Void> remove(
        @PathVariable final long school,
        @PathVariable final long teacher,
        @AuthenticationPrincipal final UrAccount actor
    ) throws Exception {
        this.operations.operation("Teachers.remove")
            .permission(actor, new SlPostgres(this.ctx, school)).permit(actor)
            .schools().school(school)
            .teachers().remove(teacher);
        return ResponseEntity.noContent().build();
    }

    private static ResponseEntity<?> result(
        final MediaType type,
        final Teacher selected,
        final HttpStatus status,
        final String base,
        final String redirect
    ) throws Exception {
        return switch (type.toString()) {
            case MediaType.TEXT_HTML_VALUE -> ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirect)).build();
            case "application/vnd.surok.teacher-v1+json" ->
                ResponseEntity.status(status)
                    .location(URI.create(String.format("%s/%d", base, selected.uid())))
                    .contentType(new MediaType(type, StandardCharsets.UTF_8))
                    .body(new ThV1(selected).print(new MdJson()).print());
            case "application/vnd.surok.teacher-v1+xml" ->
                ResponseEntity.status(status)
                    .location(URI.create(String.format("%s/%d", base, selected.uid())))
                    .contentType(new MediaType(type, StandardCharsets.UTF_8))
                    .body(new ThV1(selected).print(new MdXml("teacher")).print());
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
        if (exception instanceof TeacherAlreadyExistsException) {
            error = "duplicate";
        }
        return ResponseEntity.status(HttpStatus.FOUND).location(
            URI.create(String.format("%s/create?error=%s", request.getRequestURI(), error))
        ).build();
    }

    private Media<String> web(
        final HttpServletRequest request,
        final School school,
        final String view
    ) throws Exception {
        return new SrWeb(
            new UrAcBrief(
                (UrAccount) SecurityContextHolder.getContext().getAuthentication().getPrincipal()
            ),
            new SlsBrief(new SlsPostgres(this.ctx)),
            new CsToken((CsrfToken) request.getAttribute(CsrfToken.class.getName()))
        ).print(
            new MdXslt(
                new MdXml("surok").with("path", request.getRequestURI()),
                new XsClasspath("xslt/teachers.xsl", view)
            )
        ).with("school", new SlBrief(school))
            .with("error", request.getParameter("error"));
    }
}
