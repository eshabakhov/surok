/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule;

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
import com.eshabakhov.surok.school.schedule.input.ScheduleInputException;
import com.eshabakhov.surok.school.schedule.input.SdInputHttp;
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
 * Unified schedule HTTP resource; Accept selects HTML or versioned JSON/XML.
 * For example, request application/vnd.surok.schedules.v1+json for the collection.
 *
 * @since 0.0.1
 * @checkstyle ClassFanOutComplexityCheck (1000 lines)
 * @checkstyle ParameterNumberCheck (1000 lines)
 */
@RestController
@RequestMapping("/schools/{school}/schedules")
public class SdsHttp {

    /** JSON representation of the schedule collection. */
    private static final String JSON = "application/vnd.surok.schedules.v1+json";

    /** XML representation of the schedule collection. */
    private static final String XML = "application/vnd.surok.schedules.v1+xml";

    /** HTML representation containing only the schedule results. */
    private static final String FRAGMENT = "text/vnd.surok.schedules-fragment+html";

    /** Database context. */
    private final DSLContext ctx;

    /**
     * New resource.
     *
     * @param ctx Database context
     */
    public SdsHttp(final DSLContext ctx) {
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
    @GetMapping(produces = {MediaType.TEXT_HTML_VALUE, SdsHttp.JSON, SdsHttp.XML, SdsHttp.FRAGMENT})
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
                MediaType.valueOf(SdsHttp.JSON),
                MediaType.valueOf(SdsHttp.XML),
                MediaType.valueOf(SdsHttp.FRAGMENT)
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
        final Schedules selected = sch.schedules().selection(
            applied, page, sorts
        );
        return ResponseEntity.ok().contentType(new MediaType(type, StandardCharsets.UTF_8))
            .varyBy(HttpHeaders.ACCEPT).body(
                switch (type.toString()) {
                    case MediaType.TEXT_HTML_VALUE, SdsHttp.FRAGMENT -> {
                        final String view;
                        if (MediaType.TEXT_HTML.equals(type)) {
                            view = "list";
                        } else {
                            view = "list-fragment";
                        }
                        yield this.web(request, sch, view).with(
                            "schedules", new SdsWeb(selected, applied, sorts)
                        ).print();
                    }
                    case SdsHttp.JSON -> selected.print(new MdJson()).print();
                    case SdsHttp.XML -> selected.print(new MdXml("schedules")).print();
                    case null, default -> throw new ResponseStatusException(
                        HttpStatus.NOT_ACCEPTABLE, "Unsupported Accept header"
                    );
                }
            );
    }

    /**
     * Shows the browser form for creating a schedule.
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
            new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8)
        ).body(
            this.web(
                request, new SlsAuthorized(new SlsPostgres(this.ctx), actor).school(school),
                "create"
            ).print()
        );
    }

    /**
     * Shows one schedule or its editing form.
     *
     * @param school School identifier
     * @param schedule Entity identifier
     * @param request HTTP request
     * @param actor Acting user
     * @return Negotiated representation
     * @throws Exception If lookup or printing fails
     */
    @GetMapping(
        value = {"/{schedule}", "/{schedule}/edit"},
        produces = {
            MediaType.TEXT_HTML_VALUE,
            "application/vnd.surok.schedule-v1+json",
            "application/vnd.surok.schedule-v1+xml"
        }
    )
    public ResponseEntity<?> schedule(
        @PathVariable final long school,
        @PathVariable final long schedule,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        final MediaType type = new AcHttp(request, "schedule-v1").type();
        final School sch = new SlsAuthorized(new SlsPostgres(this.ctx), actor).school(school);
        final Schedules origin = sch.schedules();
        final Schedule selected = origin.schedule(schedule);
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
                        "schedules", new SdsSingle(origin, selected)
                    ).with("schedule", new SdBrief(selected)).with(
                        "summary", new SdSummary(this.ctx, sch.uid(), selected.uid())
                    ).print();
                }
                case "application/vnd.surok.schedule-v1+json" ->
                    new SdV1(selected).print(new MdJson()).print();
                case "application/vnd.surok.schedule-v1+xml" ->
                    new SdV1(selected).print(new MdXml("schedule")).print();
                case null, default -> throw new ResponseStatusException(
                    HttpStatus.NOT_ACCEPTABLE, "Unsupported Accept header"
                );
            }
        );
    }

    /**
     * Creates a schedule from form fields.
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
            "application/vnd.surok.schedule-v1+json",
            "application/vnd.surok.schedule-v1+xml"
        }
    )
    public ResponseEntity<?> create(
        @PathVariable final long school,
        @RequestParam final MultiValueMap<String, String> parameters,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        final MediaType type = new AcHttp(request, "schedule-v1").type();
        ResponseEntity<?> response;
        try {
            response = this.create(type, school, new SdInputHttp(parameters), actor);
        } catch (final ScheduleInputException | ScheduleAlreadyExistsException exception) {
            response = SdsHttp.rejected(type, request, exception);
        }
        return response;
    }

    /**
     * Creates a schedule from a JSON document.
     *
     * @param school School identifier
     * @param json Input document
     * @param request HTTP request
     * @param actor Acting user
     * @return Created representation
     * @throws Exception If creation fails
     */
    @PostMapping(
        consumes = "application/vnd.surok.schedule-v1+json",
        produces = {
            MediaType.TEXT_HTML_VALUE,
            "application/vnd.surok.schedule-v1+json",
            "application/vnd.surok.schedule-v1+xml"
        }
    )
    public ResponseEntity<?> create(
        @PathVariable final long school,
        @RequestBody final JsonNode json,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        return this.create(
            new AcHttp(request, "schedule-v1").type(), school, new SdInputHttp(json), actor
        );
    }

    /**
     * Revises a schedule from a JSON document.
     *
     * @param school School identifier
     * @param schedule Entity identifier
     * @param json Input document
     * @param request HTTP request
     * @param actor Acting user
     * @return Revised representation or redirect
     * @throws Exception If revision fails
     */
    @PutMapping(
        value = "/{schedule}",
        consumes = "application/vnd.surok.schedule-v1+json",
        produces = {
            MediaType.TEXT_HTML_VALUE,
            "application/vnd.surok.schedule-v1+json",
            "application/vnd.surok.schedule-v1+xml"
        }
    )
    public ResponseEntity<?> revise(
        @PathVariable final long school,
        @PathVariable final long schedule,
        @RequestBody final JsonNode json,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        final MediaType type = new AcHttp(request, "schedule-v1").type();
        final Schedule revised = new SlsAuthorized(
            new SlsPostgres(this.ctx), actor
        ).school(school).schedules()
            .revise(schedule, new SdInputHttp(json));
        return SdsHttp.result(
            type, revised, HttpStatus.OK, String.format("/schools/%d/schedules", school),
            String.format("/schools/%d/schedules/%d", school, revised.uid())
        );
    }

    /**
     * Removes a schedule.
     *
     * @param school School identifier
     * @param schedule Entity identifier
     * @param actor Acting user
     * @return Empty response
     * @throws Exception If removal fails
     */
    @DeleteMapping("/{schedule}")
    public ResponseEntity<Void> remove(
        @PathVariable final long school,
        @PathVariable final long schedule,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        new SlsAuthorized(new SlsPostgres(this.ctx), actor).school(school)
            .schedules().remove(schedule);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> create(
        final MediaType type, final long school, final ScheduleInput input, final User actor
    ) throws Exception {
        final String base = String.format("/schools/%d/schedules", school);
        return SdsHttp.result(
            type,
            new SlsAuthorized(new SlsPostgres(this.ctx), actor).school(school).schedules()
                .create(input),
            HttpStatus.CREATED, base, base
        );
    }

    private static ResponseEntity<?> result(
        final MediaType type,
        final Schedule selected,
        final HttpStatus status,
        final String base,
        final String redirect
    ) throws Exception {
        return switch (type.toString()) {
            case MediaType.TEXT_HTML_VALUE -> ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirect)).build();
            case "application/vnd.surok.schedule-v1+json" ->
                ResponseEntity.status(status)
                    .location(URI.create(String.format("%s/%d", base, selected.uid())))
                    .contentType(new MediaType(type, StandardCharsets.UTF_8))
                    .body(new SdV1(selected).print(new MdJson()).print());
            case "application/vnd.surok.schedule-v1+xml" ->
                ResponseEntity.status(status)
                    .location(URI.create(String.format("%s/%d", base, selected.uid())))
                    .contentType(new MediaType(type, StandardCharsets.UTF_8))
                    .body(new SdV1(selected).print(new MdXml("schedule")).print());
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
        if (exception instanceof ScheduleAlreadyExistsException) {
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
            new UrBrief(
                (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()
            ),
            new SlsBrief(new SlsPostgres(this.ctx)),
            new CsToken((CsrfToken) request.getAttribute(CsrfToken.class.getName()))
        ).print(
            new MdXslt(
                new MdXml("surok").with("path", request.getRequestURI()),
                new XsClasspath("xslt/schedules.xsl", view)
            )
        ).with("school", new SlBrief(school))
            .with("error", request.getParameter("error"));
    }
}
