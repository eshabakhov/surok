/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.building;

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
import com.eshabakhov.surok.school.building.input.BdInputHttp;
import com.eshabakhov.surok.school.building.input.BuildingInputException;
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
 * Unified building HTTP resource; Accept selects HTML or versioned JSON/XML.
 * For example, request application/vnd.surok.buildings.v1+json for the collection.
 *
 * @since 0.0.1
 * @checkstyle ClassFanOutComplexityCheck (1000 lines)
 * @checkstyle ParameterNumberCheck (1000 lines)
 */
@RestController
@RequestMapping("/schools/{school}/buildings")
public class BdsHttp {

    /** JSON representation of the building collection. */
    private static final String JSON = "application/vnd.surok.buildings.v1+json";

    /** XML representation of the building collection. */
    private static final String XML = "application/vnd.surok.buildings.v1+xml";

    /** HTML representation containing only the building results. */
    private static final String FRAGMENT = "text/vnd.surok.buildings-fragment+html";

    /** Database context. */
    private final DSLContext ctx;

    /**
     * New resource.
     *
     * @param ctx Database context
     */
    public BdsHttp(final DSLContext ctx) {
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
     * @param user Acting user
     * @return Negotiated representation
     * @throws Exception If lookup or printing fails
     */
    @GetMapping(produces = {MediaType.TEXT_HTML_VALUE, BdsHttp.JSON, BdsHttp.XML, BdsHttp.FRAGMENT})
    public ResponseEntity<?> selection(
        @PathVariable final long school,
        final Filters filters,
        final Page page,
        final Sorts sorts,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User user
    ) throws Exception {
        final MediaType type = new AcHttp(
            request,
            List.of(
                MediaType.TEXT_HTML,
                MediaType.valueOf(BdsHttp.JSON),
                MediaType.valueOf(BdsHttp.XML),
                MediaType.valueOf(BdsHttp.FRAGMENT)
            )
        ).type();
        final School sch = new SlsAuthorized(new SlsPostgres(this.ctx), user).school(school);
        final Set<Filter> requested = new LinkedHashSet<>(filters.filters());
        requested.addAll(
            Stream.of("name", "name_ct")
                .filter(name -> request.getParameter(name) != null)
                .map(name -> new FilterRequest(name, request.getParameter(name))).toList()
        );
        final Filters applied = new FiltersRequest(requested);
        final Buildings selected = sch.buildings().selection(
            applied, page, sorts
        );
        return ResponseEntity.ok().contentType(new MediaType(type, StandardCharsets.UTF_8))
            .varyBy(HttpHeaders.ACCEPT).body(
                switch (type.toString()) {
                    case MediaType.TEXT_HTML_VALUE, BdsHttp.FRAGMENT -> {
                        final String view;
                        if (MediaType.TEXT_HTML.equals(type)) {
                            view = "list";
                        } else {
                            view = "list-fragment";
                        }
                        yield this.web(request, sch, view).with(
                            "buildings", new BdsWeb(selected, applied, sorts)
                        ).print();
                    }
                    case BdsHttp.JSON -> selected.print(new MdJson()).print();
                    case BdsHttp.XML -> selected.print(new MdXml("buildings")).print();
                    case null, default -> throw new ResponseStatusException(
                        HttpStatus.NOT_ACCEPTABLE, "Unsupported Accept header"
                    );
                }
            );
    }

    /**
     * Shows the browser form for creating a building.
     *
     * @param school School identifier
     * @param request HTTP request
     * @param user Acting user
     * @return HTML creation form
     * @throws Exception If lookup or printing fails
     */
    @GetMapping(value = "/create", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> form(
        @PathVariable final long school, final HttpServletRequest request,
        @AuthenticationPrincipal final User user
    ) throws Exception {
        return ResponseEntity.ok().contentType(
            new MediaType(
                new AcHttp(request, List.of(MediaType.TEXT_HTML)).type(), StandardCharsets.UTF_8
            )
        ).body(
            this.web(
                request, new SlsAuthorized(new SlsPostgres(this.ctx), user).school(school),
                "create"
            ).print()
        );
    }

    /**
     * Shows one building or its editing form.
     *
     * @param school School identifier
     * @param building Entity identifier
     * @param request HTTP request
     * @param user Acting user
     * @return Negotiated representation
     * @throws Exception If lookup or printing fails
     */
    @GetMapping(
        value = {"/{building}", "/{building}/edit"},
        produces = {
            MediaType.TEXT_HTML_VALUE,
            "application/vnd.surok.building-v1+json",
            "application/vnd.surok.building-v1+xml"
        }
    )
    public ResponseEntity<?> building(
        @PathVariable final long school,
        @PathVariable final long building,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User user
    ) throws Exception {
        final MediaType type = new AcHttp(request, "building-v1").type();
        final School sch = new SlsAuthorized(
            new SlsPostgres(this.ctx), user
        ).school(school);
        final Buildings origin = sch.buildings();
        final Building selected = origin.building(building);
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
                        "buildings", new BdsSingle(origin, selected)
                    ).print();
                }
                case "application/vnd.surok.building-v1+json" ->
                    new BdV1(selected).print(new MdJson()).print();
                case "application/vnd.surok.building-v1+xml" ->
                    new BdV1(selected).print(new MdXml("building")).print();
                case null, default -> throw new ResponseStatusException(
                    HttpStatus.NOT_ACCEPTABLE, "Unsupported Accept header"
                );
            }
        );
    }

    /**
     * Creates a building from form fields.
     *
     * @param school School identifier
     * @param parameters Form fields
     * @param request HTTP request
     * @param user Acting user
     * @return Created representation or redirect
     * @throws Exception If creation fails
     */
    @PostMapping(
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
        produces = {
            MediaType.TEXT_HTML_VALUE,
            "application/vnd.surok.building-v1+json",
            "application/vnd.surok.building-v1+xml"
        }
    )
    public ResponseEntity<?> create(
        @PathVariable final long school,
        @RequestParam final MultiValueMap<String, String> parameters,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User user
    ) throws Exception {
        final MediaType type = new AcHttp(request, "building-v1").type();
        ResponseEntity<?> response;
        try {
            response = this.create(type, school, new BdInputHttp(parameters), user);
        } catch (final BuildingInputException exception) {
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
     * Creates a building from a JSON document.
     *
     * @param school School identifier
     * @param json Input document
     * @param request HTTP request
     * @param user Acting user
     * @return Created representation
     * @throws Exception If creation fails
     */
    @PostMapping(
        consumes = "application/vnd.surok.building-v1+json",
        produces = {
            MediaType.TEXT_HTML_VALUE,
            "application/vnd.surok.building-v1+json",
            "application/vnd.surok.building-v1+xml"
        }
    )
    public ResponseEntity<?> create(
        @PathVariable final long school,
        @RequestBody final JsonNode json,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User user
    ) throws Exception {
        return this.create(
            new AcHttp(request, "building-v1").type(), school, new BdInputHttp(json), user
        );
    }

    /**
     * Revises a building from a JSON document.
     *
     * @param school School identifier
     * @param building Entity identifier
     * @param json Input document
     * @param request HTTP request
     * @param user Acting user
     * @return Revised representation or redirect
     * @throws Exception If revision fails
     */
    @PutMapping(
        value = "/{building}",
        consumes = "application/vnd.surok.building-v1+json",
        produces = {
            MediaType.TEXT_HTML_VALUE,
            "application/vnd.surok.building-v1+json",
            "application/vnd.surok.building-v1+xml"
        }
    )
    public ResponseEntity<?> revise(
        @PathVariable final long school,
        @PathVariable final long building,
        @RequestBody final JsonNode json,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User user
    ) throws Exception {
        final MediaType type = new AcHttp(request, "building-v1").type();
        final Building revised = new SlsAuthorized(
            new SlsPostgres(this.ctx), user
        ).school(school).buildings()
            .revise(building, new BdInputHttp(json));
        return BdsHttp.result(
            type, revised, HttpStatus.OK, String.format("/schools/%d/buildings", school),
            String.format("/schools/%d/buildings/%d", school, revised.uid())
        );
    }

    /**
     * Removes a building.
     *
     * @param school School identifier
     * @param building Entity identifier
     * @param user Acting user
     * @return Empty response
     * @throws Exception If removal fails
     */
    @DeleteMapping("/{building}")
    public ResponseEntity<Void> remove(
        @PathVariable final long school,
        @PathVariable final long building,
        @AuthenticationPrincipal final User user
    ) throws Exception {
        new SlsAuthorized(new SlsPostgres(this.ctx), user)
            .school(school).buildings().remove(building);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> create(
        final MediaType type, final long school, final BuildingInput input, final User user
    ) throws Exception {
        final String base = String.format("/schools/%d/buildings", school);
        return BdsHttp.result(
            type,
            new SlsAuthorized(new SlsPostgres(this.ctx), user).school(school).buildings()
                .create(input),
            HttpStatus.CREATED, base, base
        );
    }

    private static ResponseEntity<?> result(
        final MediaType type,
        final Building selected,
        final HttpStatus status,
        final String base,
        final String redirect
    ) throws Exception {
        return switch (type.toString()) {
            case MediaType.TEXT_HTML_VALUE -> ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirect)).build();
            case "application/vnd.surok.building-v1+json" ->
                ResponseEntity.status(status)
                    .location(URI.create(String.format("%s/%d", base, selected.uid())))
                    .contentType(new MediaType(type, StandardCharsets.UTF_8))
                    .body(new BdV1(selected).print(new MdJson()).print());
            case "application/vnd.surok.building-v1+xml" ->
                ResponseEntity.status(status)
                    .location(URI.create(String.format("%s/%d", base, selected.uid())))
                    .contentType(new MediaType(type, StandardCharsets.UTF_8))
                    .body(new BdV1(selected).print(new MdXml("building")).print());
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
                new XsClasspath("xslt/buildings.xsl", view)
            )
        ).with("school", new SlBrief(school))
            .with("error", request.getParameter("error") != null);
    }
}
