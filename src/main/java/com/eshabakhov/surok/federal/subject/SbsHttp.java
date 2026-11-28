/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject;

import com.eshabakhov.surok.Filter;
import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.Sorts;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.federal.subject.input.SbInput;
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
import com.eshabakhov.surok.school.SlPostgres;
import com.eshabakhov.surok.school.SlsBrief;
import com.eshabakhov.surok.school.SlsPostgres;
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
    public SbsHttp(final DSLContext ctx) throws IOException {
        this(ctx, new OaYaml().operations());
    }

    /**
     * New controller using the application operation catalog.
     *
     * @param ctx Database context
     * @param operations Shared operation declarations
     */
    public SbsHttp(final DSLContext ctx, final Operations operations) {
        this.ctx = ctx;
        this.operations = operations;
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
        @AuthenticationPrincipal final UrAccount actor
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
        final Subjects origin;
        if (school == null) {
            origin = this.operations.operation("FederalSubjects.selection").permission(actor)
                .permit(new SbsFederal(this.ctx));
        } else {
            origin = this.operations.operation("SchoolSubjects.selection")
                .permission(actor, new SlPostgres(this.ctx, school)).permit(actor)
                .schools().school(school).subjects();
        }
        final Subjects selected = origin.selection(applied, page, sorts);
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
        @AuthenticationPrincipal final UrAccount actor
    ) throws Exception {
        final MediaType type = new AcHttp(request, List.of(MediaType.TEXT_HTML)).type();
        final UrAccount user;
        final Subject.Origin origin;
        if (school == null) {
            user = this.operations.operation("FederalSubjects.form")
                .permission(actor).permit(actor);
            origin = Subject.Origin.FEDERAL;
        } else {
            user = this.operations.operation("SchoolSubjects.form")
                .permission(actor, new SlPostgres(this.ctx, school)).permit(actor);
            origin = Subject.Origin.SCHOOL;
        }
        return ResponseEntity.ok().contentType(new MediaType(type, StandardCharsets.UTF_8)).body(
            this.web(request, school, "create", user)
                .with("subject", new SbDraft(new SbInput(), origin)).print()
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
        @AuthenticationPrincipal final UrAccount actor
    ) throws Exception {
        final MediaType type = new AcHttp(request, "subject-v1").type();
        final Subjects origin;
        if (school == null) {
            origin = this.operations.operation("FederalSubjects.subject").permission(actor)
                .permit(new SbsFederal(this.ctx));
        } else {
            origin = this.operations.operation("SchoolSubjects.subject")
                .permission(actor, new SlPostgres(this.ctx, school)).permit(actor)
                .schools().school(school).subjects();
        }
        final Subject selected = origin.subject(subject);
        return ResponseEntity.ok().contentType(new MediaType(type, StandardCharsets.UTF_8)).body(
            switch (type.toString()) {
                case MediaType.TEXT_HTML_VALUE ->
                    this.web(request, school, "details", actor).with(
                        "subject", selected
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
        @AuthenticationPrincipal final UrAccount actor
    ) throws Exception {
        final MediaType type = new AcHttp(request, List.of(MediaType.TEXT_HTML)).type();
        final Subjects origin;
        if (school == null) {
            origin = this.operations.operation("FederalSubjects.edit").permission(actor)
                .permit(new SbsFederal(this.ctx));
        } else {
            origin = this.operations.operation("SchoolSubjects.edit")
                .permission(actor, new SlPostgres(this.ctx, school)).permit(actor)
                .schools().school(school).subjects();
        }
        final Subject selected = origin.subject(subject);
        if (school != null && selected.origin() == Subject.Origin.FEDERAL) {
            throw new SubjectReadOnlyException(selected.uid());
        }
        return ResponseEntity.ok().contentType(new MediaType(type, StandardCharsets.UTF_8)).body(
            this.web(request, school, "edit", actor).with(
                "subject", selected
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
        @AuthenticationPrincipal final UrAccount actor
    ) throws Exception {
        final MediaType type = new AcHttp(request, "subject-v1").type();
        final Subjects origin;
        if (school == null) {
            origin = this.operations.operation("FederalSubjects.create").permission(actor)
                .permit(new SbsFederal(this.ctx));
        } else {
            origin = this.operations.operation("SchoolSubjects.create")
                .permission(actor, new SlPostgres(this.ctx, school)).permit(actor)
                .schools().school(school).subjects();
        }
        ResponseEntity<?> response;
        try {
            response = SbsHttp.result(
                type,
                origin.create(new SbInputHttp(parameters)), HttpStatus.CREATED,
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
        @AuthenticationPrincipal final UrAccount actor
    ) throws Exception {
        final MediaType type = new AcHttp(request, "subject-v1").type();
        final Subjects origin;
        if (school == null) {
            origin = this.operations.operation("FederalSubjects.create").permission(actor)
                .permit(new SbsFederal(this.ctx));
        } else {
            origin = this.operations.operation("SchoolSubjects.create")
                .permission(actor, new SlPostgres(this.ctx, school)).permit(actor)
                .schools().school(school).subjects();
        }
        return SbsHttp.result(
            type, origin.create(new SbInputHttp(json)), HttpStatus.CREATED,
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
        @AuthenticationPrincipal final UrAccount actor
    ) throws Exception {
        final MediaType type = new AcHttp(request, "subject-v1").type();
        final Subjects origin;
        if (school == null) {
            origin = this.operations.operation("FederalSubjects.revise").permission(actor)
                .permit(new SbsFederal(this.ctx));
        } else {
            origin = this.operations.operation("SchoolSubjects.revise")
                .permission(actor, new SlPostgres(this.ctx, school)).permit(actor)
                .schools().school(school).subjects();
        }
        final Subject revised = origin.revise(subject, new SbInputHttp(json));
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
        @AuthenticationPrincipal final UrAccount actor
    ) throws Exception {
        new AcHttp(request, "subject-v1").type();
        final Subjects origin;
        if (school == null) {
            origin = this.operations.operation("FederalSubjects.remove").permission(actor)
                .permit(new SbsFederal(this.ctx));
        } else {
            origin = this.operations.operation("SchoolSubjects.remove")
                .permission(actor, new SlPostgres(this.ctx, school)).permit(actor)
                .schools().school(school).subjects();
        }
        origin.remove(subject);
        return ResponseEntity.noContent().build();
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
        final UrAccount actor
    ) throws Exception {
        final boolean editable;
        if (school == null) {
            editable = this.operations.operation("FederalSubjects.revise")
                .permission(actor).allows();
        } else {
            editable = this.operations.operation("SchoolSubjects.revise")
                .permission(actor, new SlPostgres(this.ctx, school)).allows();
        }
        Media<String> media = new SrWeb(
            new UrAcBrief(actor),
            new SlsBrief(new SlsPostgres(this.ctx)),
            new CsToken((CsrfToken) request.getAttribute(CsrfToken.class.getName()))
        ).print(
            new MdXslt(
                new MdXml("surok").attribute("path", request.getRequestURI()),
                new XsClasspath("xslt/subjects.xsl", view)
            )
        ).with("federal", school == null).with("error", request.getParameter("error"))
            .attribute("editable", editable);
        if (school != null) {
            media = media.with(
                "school", new SlBrief(
                    actor.schools().school(school)
                )
            );
        }
        return media;
    }
}
