/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schoolclass;

import com.eshabakhov.surok.Filter;
import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.Sorts;
import com.eshabakhov.surok.User;
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
import com.eshabakhov.surok.school.schoolclass.input.ScInputHttp;
import com.eshabakhov.surok.school.schoolclass.input.SchoolClassInputException;
import com.eshabakhov.surok.security.CsToken;
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
 * Unified school class HTTP resource; Accept selects HTML or versioned JSON/XML.
 * For example, request application/vnd.surok.school-classes.v1+json for the collection.
 *
 * @since 0.0.1
 * @checkstyle ClassFanOutComplexityCheck (1000 lines)
 * @checkstyle ParameterNumberCheck (1000 lines)
 */
@RestController
@RequestMapping("/schools/{school}/classes")
public class ScsHttp {

    /** JSON representation of the school class collection. */
    private static final String JSON = "application/vnd.surok.school-classes.v1+json";

    /** XML representation of the school class collection. */
    private static final String XML = "application/vnd.surok.school-classes.v1+xml";

    /** HTML representation containing only the class results. */
    private static final String FRAGMENT = "text/vnd.surok.school-classes-fragment+html";

    /** Database context. */
    private final DSLContext ctx;

    /**
     * New resource.
     *
     * @param ctx Database context
     */
    public ScsHttp(final DSLContext ctx) {
        this.ctx = ctx;
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
    @GetMapping(produces = {MediaType.TEXT_HTML_VALUE, ScsHttp.JSON, ScsHttp.XML, ScsHttp.FRAGMENT})
    public ResponseEntity<?> selection(
        @PathVariable final long school,
        final Filters filters,
        final Page page,
        final Sorts sorts,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        final MediaType type = new AcHttp(
            request,
            List.of(
                MediaType.TEXT_HTML,
                MediaType.valueOf(ScsHttp.JSON),
                MediaType.valueOf(ScsHttp.XML),
                MediaType.valueOf(ScsHttp.FRAGMENT)
            )
        ).type();
        final School sch = new SlsAuthorized(new SlsPostgres(this.ctx), actor).school(school);
        final Set<Filter> requested = new LinkedHashSet<>(filters.filters());
        requested.addAll(
            Stream.of("name", "name_ct")
                .filter(name -> request.getParameter(name) != null)
                .map(name -> new FilterRequest(name, request.getParameter(name))).toList()
        );
        final Filters applied = new FiltersRequest(requested);
        final SchoolClasses selected = sch.schoolClasses().selection(
            applied, page, sorts
        );
        return ResponseEntity.ok().contentType(new MediaType(type, StandardCharsets.UTF_8))
            .varyBy(HttpHeaders.ACCEPT).body(
                switch (type.toString()) {
                    case MediaType.TEXT_HTML_VALUE, ScsHttp.FRAGMENT -> {
                        final String view;
                        if (MediaType.TEXT_HTML.equals(type)) {
                            view = "list";
                        } else {
                            view = "list-fragment";
                        }
                        yield this.web(request, sch, view).with(
                            "classes", new ScsWeb(selected, applied, sorts)
                        ).print();
                    }
                    case ScsHttp.JSON -> selected.print(new MdJson()).print();
                    case ScsHttp.XML -> selected.print(new MdXml("classes")).print();
                    case null, default -> throw new ResponseStatusException(
                        HttpStatus.NOT_ACCEPTABLE, "Unsupported Accept header"
                    );
                }
            );
    }

    /**
     * Shows the browser form for creating a class.
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
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        return ResponseEntity.ok().contentType(
            new MediaType(
                new AcHttp(request, List.of(MediaType.TEXT_HTML)).type(), StandardCharsets.UTF_8
            )
        ).body(
            this.web(
                request, new SlsAuthorized(new SlsPostgres(this.ctx), actor).school(school),
                "create"
            ).print()
        );
    }

    /**
     * Shows one class or its editing form.
     *
     * @param school School identifier
     * @param clazz Entity identifier
     * @param request HTTP request
     * @param actor Acting user
     * @return Negotiated representation
     * @throws Exception If lookup or printing fails
     */
    @GetMapping(
        value = {"/{clazz}", "/{clazz}/edit"},
        produces = {
            MediaType.TEXT_HTML_VALUE,
            "application/vnd.surok.school-class-v1+json",
            "application/vnd.surok.school-class-v1+xml"
        }
    )
    public ResponseEntity<?> clazz(
        @PathVariable final long school,
        @PathVariable final long clazz,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        final MediaType type = new AcHttp(request, "school-class-v1").type();
        final School sch = new SlsAuthorized(new SlsPostgres(this.ctx), actor).school(school);
        final SchoolClasses origin = sch.schoolClasses();
        final SchoolClass selected = origin.clazz(clazz);
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
                        "classes", new ScsSingle(origin, selected)
                    ).print();
                }
                case "application/vnd.surok.school-class-v1+json" ->
                    new ScV1(selected).print(new MdJson()).print();
                case "application/vnd.surok.school-class-v1+xml" ->
                    new ScV1(selected).print(new MdXml("school-class")).print();
                case null, default -> throw new ResponseStatusException(
                    HttpStatus.NOT_ACCEPTABLE, "Unsupported Accept header"
                );
            }
        );
    }

    /**
     * Creates a class from form fields.
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
            "application/vnd.surok.school-class-v1+json",
            "application/vnd.surok.school-class-v1+xml"
        }
    )
    public ResponseEntity<?> create(
        @PathVariable final long school,
        @RequestParam final MultiValueMap<String, String> parameters,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        final MediaType type = new AcHttp(request, "school-class-v1").type();
        ResponseEntity<?> response;
        try {
            response = this.create(type, school, new ScInputHttp(parameters), actor);
        } catch (final SchoolClassInputException exception) {
            if (!MediaType.TEXT_HTML.equals(type)) {
                throw exception;
            }
            response = ResponseEntity.status(HttpStatus.FOUND).location(
                URI.create(String.format("%s/create?error=empty", request.getRequestURI()))
            ).build();
        }
        return response;
    }

    /**
     * Creates a class from a JSON document.
     *
     * @param school School identifier
     * @param json Input document
     * @param request HTTP request
     * @param actor Acting user
     * @return Created representation
     * @throws Exception If creation fails
     */
    @PostMapping(
        consumes = "application/vnd.surok.school-class-v1+json",
        produces = {
            MediaType.TEXT_HTML_VALUE,
            "application/vnd.surok.school-class-v1+json",
            "application/vnd.surok.school-class-v1+xml"
        }
    )
    public ResponseEntity<?> create(
        @PathVariable final long school,
        @RequestBody final JsonNode json,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        return this.create(
            new AcHttp(request, "school-class-v1").type(), school, new ScInputHttp(json), actor
        );
    }

    /**
     * Revises a class from a JSON document.
     *
     * @param school School identifier
     * @param clazz Entity identifier
     * @param json Input document
     * @param request HTTP request
     * @param actor Acting user
     * @return Revised representation or redirect
     * @throws Exception If revision fails
     */
    @PutMapping(
        value = "/{clazz}",
        consumes = "application/vnd.surok.school-class-v1+json",
        produces = {
            MediaType.TEXT_HTML_VALUE,
            "application/vnd.surok.school-class-v1+json",
            "application/vnd.surok.school-class-v1+xml"
        }
    )
    public ResponseEntity<?> revise(
        @PathVariable final long school,
        @PathVariable final long clazz,
        @RequestBody final JsonNode json,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        final MediaType type = new AcHttp(request, "school-class-v1").type();
        final SchoolClass revised = new SlsAuthorized(
            new SlsPostgres(this.ctx), actor
        ).school(school).schoolClasses().revise(clazz, new ScInputHttp(json));
        return ScsHttp.result(
            type, revised, HttpStatus.OK, String.format("/schools/%d/classes", school),
            String.format("/schools/%d/classes/%d", school, revised.uid())
        );
    }

    /**
     * Removes a class.
     *
     * @param school School identifier
     * @param clazz Entity identifier
     * @param actor Acting user
     * @return Empty response
     * @throws Exception If removal fails
     */
    @DeleteMapping("/{clazz}")
    public ResponseEntity<Void> remove(
        @PathVariable final long school,
        @PathVariable final long clazz,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        new SlsAuthorized(new SlsPostgres(this.ctx), actor)
            .school(school).schoolClasses().remove(clazz);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> create(
        final MediaType type, final long school, final SchoolClassInput input, final User actor
    ) throws Exception {
        final String base = String.format("/schools/%d/classes", school);
        return ScsHttp.result(
            type,
            new SlsAuthorized(new SlsPostgres(this.ctx), actor).school(school).schoolClasses()
                .create(input),
            HttpStatus.CREATED, base, base
        );
    }

    private static ResponseEntity<?> result(
        final MediaType type,
        final SchoolClass selected,
        final HttpStatus status,
        final String base,
        final String redirect
    ) throws Exception {
        return switch (type.toString()) {
            case MediaType.TEXT_HTML_VALUE -> ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirect)).build();
            case "application/vnd.surok.school-class-v1+json" ->
                ResponseEntity.status(status)
                    .location(URI.create(String.format("%s/%d", base, selected.uid())))
                    .contentType(new MediaType(type, StandardCharsets.UTF_8))
                    .body(new ScV1(selected).print(new MdJson()).print());
            case "application/vnd.surok.school-class-v1+xml" ->
                ResponseEntity.status(status)
                    .location(URI.create(String.format("%s/%d", base, selected.uid())))
                    .contentType(new MediaType(type, StandardCharsets.UTF_8))
                    .body(new ScV1(selected).print(new MdXml("school-class")).print());
            case null, default -> throw new ResponseStatusException(
                HttpStatus.NOT_ACCEPTABLE, "Unsupported Accept header"
            );
        };
    }

    private Media<String> web(
        final HttpServletRequest request,
        final School school,
        final String view
    ) throws Exception {
        return new SrWeb(
            new UrBrief(
                (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()
            ),
            new SlsBrief(new SlsPostgres(this.ctx)),
            new CsToken((CsrfToken) request.getAttribute(CsrfToken.class.getName()))
        ).print(
            new MdXslt(
                new MdXml("surok").with("path", request.getRequestURI()),
                new XsClasspath("xslt/school-classes.xsl", view)
            )
        ).with("school", new SlBrief(school))
            .with("error", request.getParameter("error") != null);
    }
}
