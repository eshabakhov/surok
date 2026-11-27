/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.building;

import com.eshabakhov.surok.Filter;
import com.eshabakhov.surok.Filters;
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
import com.eshabakhov.surok.school.building.input.BdInput;
import com.eshabakhov.surok.school.building.input.BdInputHttp;
import com.eshabakhov.surok.school.building.input.BuildingInputException;
import com.eshabakhov.surok.security.CsToken;
import com.eshabakhov.surok.user.UrAcBrief;
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
     * New resource using the acting user's operation catalog.
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
        @AuthenticationPrincipal final UrAccount user
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
        final School sch = user.permit("Buildings.selection", new SlPostgres(this.ctx, school))
            .schools().school(school);
        final Set<Filter> requested = new LinkedHashSet<>(filters.filters());
        requested.addAll(
            Stream.of("name", "name_ct")
                .filter(name -> request.getParameter(name) != null)
                .map(name -> new FilterRequest(name, request.getParameter(name))).toList()
        );
        final Filters applied = new FiltersRequest(requested);
        final Buildings selected = sch.buildings().selection(applied, page, sorts);
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
                        yield new SrWeb(
                            new UrAcBrief(user),
                            new SlsBrief(new SlsPostgres(this.ctx)),
                            new CsToken((CsrfToken) request.getAttribute(CsrfToken.class.getName()))
                        ).print(
                            new MdXslt(
                                new MdXml("surok").with("path", request.getRequestURI()),
                                new XsClasspath("xslt/buildings.xsl", view)
                            )
                        ).with("school", new SlBrief(sch))
                            .with("error", request.getParameter("error") != null)
                            .with("buildings", new BdsWeb(selected, applied, sorts)).print();
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
        @AuthenticationPrincipal final UrAccount user
    ) throws Exception {
        final MediaType type = new AcHttp(request, List.of(MediaType.TEXT_HTML)).type();
        final UrAccount actor = user.permit("Buildings.form", new SlPostgres(this.ctx, school));
        return ResponseEntity.ok().contentType(new MediaType(type, StandardCharsets.UTF_8)).body(
            new SrWeb(
                new UrAcBrief(actor),
                new SlsBrief(new SlsPostgres(this.ctx)),
                new CsToken((CsrfToken) request.getAttribute(CsrfToken.class.getName()))
            ).print(
                new MdXslt(
                    new MdXml("surok").with("path", request.getRequestURI()),
                    new XsClasspath("xslt/buildings.xsl", "create")
                )
            ).with("school", new SlBrief(actor.schools().school(school)))
                .with("building", new BdDraft(new BdInput()))
                .with("error", request.getParameter("error") != null).print()
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
        @AuthenticationPrincipal final UrAccount user
    ) throws Exception {
        final MediaType type = new AcHttp(request, "building-v1").type();
        final String operation;
        final String view;
        if (request.getRequestURI().endsWith("/edit")) {
            operation = "Buildings.edit";
            view = "edit";
        } else {
            operation = "Buildings.building";
            view = "details";
        }
        final School sch = user.permit(operation, new SlPostgres(this.ctx, school))
            .schools().school(school);
        final Building selected = sch.buildings().building(building);
        return ResponseEntity.ok().contentType(new MediaType(type, StandardCharsets.UTF_8)).body(
            switch (type.toString()) {
                case MediaType.TEXT_HTML_VALUE -> new SrWeb(
                    new UrAcBrief(user),
                    new SlsBrief(new SlsPostgres(this.ctx)),
                    new CsToken((CsrfToken) request.getAttribute(CsrfToken.class.getName()))
                ).print(
                    new MdXslt(
                        new MdXml("surok").with("path", request.getRequestURI()),
                        new XsClasspath("xslt/buildings.xsl", view)
                    )
                ).with("school", new SlBrief(sch))
                    .with("error", request.getParameter("error") != null)
                    .with("building", selected).print();
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
        @AuthenticationPrincipal final UrAccount user
    ) throws Exception {
        final MediaType type = new AcHttp(request, "building-v1").type();
        final School target = user.permit("Buildings.create", new SlPostgres(this.ctx, school))
            .schools().school(school);
        final String base = String.format("/schools/%d/buildings", school);
        ResponseEntity<?> response;
        try {
            final Building selected = target.buildings().create(new BdInputHttp(parameters));
            response = switch (type.toString()) {
                case MediaType.TEXT_HTML_VALUE -> ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(base)).build();
                case "application/vnd.surok.building-v1+json" ->
                    ResponseEntity.status(HttpStatus.CREATED)
                        .location(URI.create(String.format("%s/%d", base, selected.uid())))
                        .contentType(new MediaType(type, StandardCharsets.UTF_8))
                        .body(new BdV1(selected).print(new MdJson()).print());
                case "application/vnd.surok.building-v1+xml" ->
                    ResponseEntity.status(HttpStatus.CREATED)
                        .location(URI.create(String.format("%s/%d", base, selected.uid())))
                        .contentType(new MediaType(type, StandardCharsets.UTF_8))
                        .body(new BdV1(selected).print(new MdXml("building")).print());
                case null, default -> throw new ResponseStatusException(
                    HttpStatus.NOT_ACCEPTABLE, "Unsupported Accept header"
                );
            };
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
        @AuthenticationPrincipal final UrAccount user
    ) throws Exception {
        final MediaType type = new AcHttp(request, "building-v1").type();
        final String base = String.format("/schools/%d/buildings", school);
        final Building selected = user.permit("Buildings.create", new SlPostgres(this.ctx, school))
            .schools().school(school).buildings().create(new BdInputHttp(json));
        return switch (type.toString()) {
            case MediaType.TEXT_HTML_VALUE -> ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(base)).build();
            case "application/vnd.surok.building-v1+json" ->
                ResponseEntity.status(HttpStatus.CREATED)
                    .location(URI.create(String.format("%s/%d", base, selected.uid())))
                    .contentType(new MediaType(type, StandardCharsets.UTF_8))
                    .body(new BdV1(selected).print(new MdJson()).print());
            case "application/vnd.surok.building-v1+xml" ->
                ResponseEntity.status(HttpStatus.CREATED)
                    .location(URI.create(String.format("%s/%d", base, selected.uid())))
                    .contentType(new MediaType(type, StandardCharsets.UTF_8))
                    .body(new BdV1(selected).print(new MdXml("building")).print());
            case null, default -> throw new ResponseStatusException(
                HttpStatus.NOT_ACCEPTABLE, "Unsupported Accept header"
            );
        };
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
        @AuthenticationPrincipal final UrAccount user
    ) throws Exception {
        final MediaType type = new AcHttp(request, "building-v1").type();
        final Building revised = user.permit("Building.revise", new SlPostgres(this.ctx, school))
            .schools().school(school).buildings().revise(building, new BdInputHttp(json));
        final URI location = URI.create(
            String.format("/schools/%d/buildings/%d", school, revised.uid())
        );
        return switch (type.toString()) {
            case MediaType.TEXT_HTML_VALUE -> ResponseEntity.status(HttpStatus.FOUND)
                .location(location).build();
            case "application/vnd.surok.building-v1+json" ->
                ResponseEntity.ok().location(location)
                    .contentType(new MediaType(type, StandardCharsets.UTF_8))
                    .body(new BdV1(revised).print(new MdJson()).print());
            case "application/vnd.surok.building-v1+xml" ->
                ResponseEntity.ok().location(location)
                    .contentType(new MediaType(type, StandardCharsets.UTF_8))
                    .body(new BdV1(revised).print(new MdXml("building")).print());
            case null, default -> throw new ResponseStatusException(
                HttpStatus.NOT_ACCEPTABLE, "Unsupported Accept header"
            );
        };
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
        @AuthenticationPrincipal final UrAccount user
    ) throws Exception {
        user.permit("Buildings.remove", new SlPostgres(this.ctx, school))
            .schools().school(school).buildings().remove(building);
        return ResponseEntity.noContent().build();
    }
}
