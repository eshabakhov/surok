/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.building.cabinet;

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
import com.eshabakhov.surok.school.building.Building;
import com.eshabakhov.surok.school.building.cabinet.input.CabinetInputException;
import com.eshabakhov.surok.school.building.cabinet.input.CbInputHttp;
import com.eshabakhov.surok.security.CsToken;
import com.eshabakhov.surok.user.UrBrief;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.jooq.DSLContext;
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
 * Unified cabinet HTTP resource; Accept selects HTML or versioned JSON/XML.
 * For example, request application/vnd.surok.cabinets.v1+json for the collection.
 *
 * @since 0.0.1
 * @checkstyle ClassFanOutComplexityCheck (1000 lines)
 * @checkstyle ParameterNumberCheck (1000 lines)
 */
@RestController
@RequestMapping("/schools/{school}/buildings/{building}/cabinets")
@Tag(name = "Cabinets")
public class CbsHttp {

    /** Database context. */
    private final DSLContext ctx;

    /**
     * New resource.
     *
     * @param ctx Database context
     */
    public CbsHttp(final DSLContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Shows a collection, its fragment or a creation form.
     *
     * @param school School identifier
     * @param building Building identifier
     * @param filters Filtering
     * @param page Pagination
     * @param sorts Sorting
     * @param request HTTP request
     * @param user Acting user
     * @return Negotiated representation
     * @throws Exception If lookup or printing fails
     */
    @GetMapping(
        value = {"", "/", "/fragment", "/create"},
        produces = {
            MediaType.TEXT_HTML_VALUE,
            "application/vnd.surok.cabinets.v1+json",
            "application/vnd.surok.cabinets.v1+xml"
        }
    )
    @Operation(summary = "List cabinets")
    public ResponseEntity<?> selection(
        @PathVariable final long school,
        @PathVariable final long building,
        final Filters filters,
        final Page page,
        final Sorts sorts,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User user
    ) throws Exception {
        final MediaType type = new AcHttp(request, "cabinets.v1").type();
        final School sch = new SlsAuthorized(
            new SlsPostgres(this.ctx), user
        ).school(school);
        final Building build = sch.buildings().building(building);
        final Set<Filter> requested = new LinkedHashSet<>(filters.filters());
        requested.addAll(
            Stream.of("name", "name_ct")
                .filter(name -> request.getParameter(name) != null)
                .map(name -> new FilterRequest(name, request.getParameter(name))).toList()
        );
        final Filters applied = new FiltersRequest(requested);
        final Cabinets selected = build.cabinets().selection(
            applied, page, sorts
        );
        return ResponseEntity.ok().contentType(new MediaType(type, StandardCharsets.UTF_8)).body(
            switch (type.toString()) {
                case MediaType.TEXT_HTML_VALUE -> {
                    final String view;
                    if (request.getRequestURI().endsWith("/fragment")) {
                        view = "list-fragment";
                    } else if (request.getRequestURI().endsWith("/create")) {
                        view = "create";
                    } else {
                        view = "list";
                    }
                    yield this.web(request, sch, view).with("building", build).with(
                        "cabinets", new CbsWeb(selected, applied, sorts)
                    ).print();
                }
                case "application/vnd.surok.cabinets.v1+json" ->
                    selected.print(new MdJson()).print();
                case "application/vnd.surok.cabinets.v1+xml" ->
                    selected.print(new MdXml("cabinets")).print();
                case null, default -> throw new ResponseStatusException(
                    HttpStatus.NOT_ACCEPTABLE, "Unsupported Accept header"
                );
            }
        );
    }

    /**
     * Shows one cabinet or its editing form.
     *
     * @param school School identifier
     * @param building Building identifier
     * @param cabinet Entity identifier
     * @param request HTTP request
     * @param user Acting user
     * @return Negotiated representation
     * @throws Exception If lookup or printing fails
     */
    @GetMapping(
        value = {"/{cabinet}", "/{cabinet}/edit"},
        produces = {
            MediaType.TEXT_HTML_VALUE,
            "application/vnd.surok.cabinet-v1+json",
            "application/vnd.surok.cabinet-v1+xml"
        }
    )
    @Operation(summary = "Get cabinet")
    public ResponseEntity<?> cabinet(
        @PathVariable final long school,
        @PathVariable final long building,
        @PathVariable final long cabinet,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User user
    ) throws Exception {
        final MediaType type = new AcHttp(request, "cabinet-v1").type();
        final School sch = new SlsAuthorized(
            new SlsPostgres(this.ctx), user
        ).school(school);
        final Building build = sch.buildings().building(building);
        final Cabinets origin = build.cabinets();
        final Cabinet selected = origin.cabinet(cabinet);
        return ResponseEntity.ok().contentType(new MediaType(type, StandardCharsets.UTF_8)).body(
            switch (type.toString()) {
                case MediaType.TEXT_HTML_VALUE -> {
                    final String view;
                    if (request.getRequestURI().endsWith("/edit")) {
                        view = "edit";
                    } else {
                        view = "details";
                    }
                    yield this.web(request, sch, view).with("building", build).with(
                        "cabinets", new CbsSingle(origin, selected)
                    ).print();
                }
                case "application/vnd.surok.cabinet-v1+json" ->
                    new CbV1(selected).print(new MdJson()).print();
                case "application/vnd.surok.cabinet-v1+xml" ->
                    new CbV1(selected).print(new MdXml("cabinet")).print();
                case null, default -> throw new ResponseStatusException(
                    HttpStatus.NOT_ACCEPTABLE, "Unsupported Accept header"
                );
            }
        );
    }

    /**
     * Creates a cabinet from form fields.
     *
     * @param school School identifier
     * @param building Building identifier
     * @param parameters Form fields
     * @param request HTTP request
     * @param user Acting user
     * @return Created representation or redirect
     * @throws Exception If creation fails
     */
    @PostMapping(
        value = "/create",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
        produces = {
            MediaType.TEXT_HTML_VALUE,
            "application/vnd.surok.cabinet-v1+json",
            "application/vnd.surok.cabinet-v1+xml"
        }
    )
    @Operation(summary = "Create cabinet from form")
    public ResponseEntity<?> create(
        @PathVariable final long school,
        @PathVariable final long building,
        @RequestParam final MultiValueMap<String, String> parameters,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User user
    ) throws Exception {
        final MediaType type = new AcHttp(request, "cabinet-v1").type();
        ResponseEntity<?> response;
        try {
            response = CbsHttp.result(
                type,
                new SlsAuthorized(new SlsPostgres(this.ctx), user)
                .school(school).buildings().building(building).cabinets()
                    .create(new CbInputHttp(parameters)), HttpStatus.CREATED,
                String.format("/schools/%d/buildings/%d/cabinets", school, building),
                String.format("/schools/%d/buildings/%d/cabinets", school, building)
            );
        } catch (final CabinetInputException exception) {
            if (!MediaType.TEXT_HTML.equals(type)) {
                throw exception;
            }
            response = ResponseEntity.status(HttpStatus.FOUND).location(
                URI.create(String.format("%s?error=empty", request.getRequestURI()))
            ).build();
        }
        return response;
    }

    /**
     * Creates a cabinet from a JSON document.
     *
     * @param school School identifier
     * @param building Building identifier
     * @param json Input document
     * @param request HTTP request
     * @param user Acting user
     * @return Created representation
     * @throws Exception If creation fails
     */
    @PostMapping(
        value = {"", "/", "/create"},
        consumes = "application/vnd.surok.cabinet-v1+json",
        produces = {
            MediaType.TEXT_HTML_VALUE,
            "application/vnd.surok.cabinet-v1+json",
            "application/vnd.surok.cabinet-v1+xml"
        }
    )
    @Operation(summary = "Create cabinet from JSON")
    public ResponseEntity<?> create(
        @PathVariable final long school,
        @PathVariable final long building,
        @RequestBody final JsonNode json,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User user
    ) throws Exception {
        return CbsHttp.result(
            new AcHttp(request, "cabinet-v1").type(),
            new SlsAuthorized(new SlsPostgres(this.ctx), user)
                .school(school).buildings().building(building).cabinets()
                .create(new CbInputHttp(json)), HttpStatus.CREATED,
            String.format("/schools/%d/buildings/%d/cabinets", school, building),
            String.format("/schools/%d/buildings/%d/cabinets", school, building)
        );
    }

    /**
     * Revises a cabinet from form fields.
     *
     * @param school School identifier
     * @param building Building identifier
     * @param cabinet Entity identifier
     * @param parameters Form fields
     * @param request HTTP request
     * @param user Acting user
     * @return Revised representation or redirect
     * @throws Exception If revision fails
     */
    @PostMapping(
        value = "/{cabinet}/edit",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
        produces = {
            MediaType.TEXT_HTML_VALUE,
            "application/vnd.surok.cabinet-v1+json",
            "application/vnd.surok.cabinet-v1+xml"
        }
    )
    @Operation(summary = "Update cabinet from form")
    public ResponseEntity<?> revise(
        @PathVariable final long school,
        @PathVariable final long building,
        @PathVariable final long cabinet,
        @RequestParam final MultiValueMap<String, String> parameters,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User user
    ) throws Exception {
        final MediaType type = new AcHttp(request, "cabinet-v1").type();
        ResponseEntity<?> response;
        try {
            final Cabinet revised = new SlsAuthorized(new SlsPostgres(this.ctx), user)
                .school(school).buildings().building(building).cabinets()
                .revise(cabinet, new CbInputHttp(parameters));
            response = CbsHttp.result(
                type, revised, HttpStatus.OK,
                String.format("/schools/%d/buildings/%d/cabinets", school, building),
                String.format(
                    "/schools/%d/buildings/%d/cabinets/%d", school, building, revised.uid()
                )
            );
        } catch (final CabinetInputException exception) {
            if (!MediaType.TEXT_HTML.equals(type)) {
                throw exception;
            }
            response = ResponseEntity.status(HttpStatus.FOUND).location(
                URI.create(String.format("%s?error=empty", request.getRequestURI()))
            ).build();
        }
        return response;
    }

    /**
     * Revises a cabinet from a JSON document.
     *
     * @param school School identifier
     * @param building Building identifier
     * @param cabinet Entity identifier
     * @param json Input document
     * @param request HTTP request
     * @param user Acting user
     * @return Revised representation or redirect
     * @throws Exception If revision fails
     */
    @PutMapping(
        value = "/{cabinet}",
        consumes = "application/vnd.surok.cabinet-v1+json",
        produces = {
            MediaType.TEXT_HTML_VALUE,
            "application/vnd.surok.cabinet-v1+json",
            "application/vnd.surok.cabinet-v1+xml"
        }
    )
    @Operation(summary = "Update cabinet from JSON")
    public ResponseEntity<?> revise(
        @PathVariable final long school,
        @PathVariable final long building,
        @PathVariable final long cabinet,
        @RequestBody final JsonNode json,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User user
    ) throws Exception {
        final MediaType type = new AcHttp(request, "cabinet-v1").type();
        final Cabinet revised = new SlsAuthorized(new SlsPostgres(this.ctx), user)
            .school(school).buildings().building(building).cabinets()
            .revise(cabinet, new CbInputHttp(json));
        return CbsHttp.result(
            type, revised, HttpStatus.OK,
            String.format("/schools/%d/buildings/%d/cabinets", school, building),
            String.format("/schools/%d/buildings/%d/cabinets/%d", school, building, revised.uid())
        );
    }

    /**
     * Removes a cabinet.
     *
     * @param school School identifier
     * @param building Building identifier
     * @param cabinet Entity identifier
     * @param user Acting user
     * @return Empty response
     * @throws Exception If removal fails
     */
    @DeleteMapping("/{cabinet}")
    @Operation(summary = "Remove cabinet")
    public ResponseEntity<Void> remove(
        @PathVariable final long school,
        @PathVariable final long building,
        @PathVariable final long cabinet,
        @AuthenticationPrincipal final User user
    ) throws Exception {
        new SlsAuthorized(new SlsPostgres(this.ctx), user)
            .school(school).buildings().building(building).cabinets().remove(cabinet);
        return ResponseEntity.noContent().build();
    }

    private static ResponseEntity<?> result(
        final MediaType type,
        final Cabinet selected,
        final HttpStatus status,
        final String base,
        final String redirect
    ) throws Exception {
        return switch (type.toString()) {
            case MediaType.TEXT_HTML_VALUE -> ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirect)).build();
            case "application/vnd.surok.cabinet-v1+json" ->
                ResponseEntity.status(status)
                    .location(URI.create(String.format("%s/%d", base, selected.uid())))
                    .contentType(new MediaType(type, StandardCharsets.UTF_8))
                    .body(new CbV1(selected).print(new MdJson()).print());
            case "application/vnd.surok.cabinet-v1+xml" ->
                ResponseEntity.status(status)
                    .location(URI.create(String.format("%s/%d", base, selected.uid())))
                    .contentType(new MediaType(type, StandardCharsets.UTF_8))
                    .body(new CbV1(selected).print(new MdXml("cabinet")).print());
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
                new XsClasspath("xslt/cabinets.xsl", view)
            )
        ).with("school", new SlBrief(school))
            .with("error", request.getParameter("error") != null);
    }
}
