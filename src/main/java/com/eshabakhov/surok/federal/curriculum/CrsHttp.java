/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum;

import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.PrItems;
import com.eshabakhov.surok.Sorts;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.federal.curriculum.input.CrInputHttp;
import com.eshabakhov.surok.media.AcHttp;
import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.media.MdXml;
import com.eshabakhov.surok.media.MdXslt;
import com.eshabakhov.surok.media.SrWeb;
import com.eshabakhov.surok.media.XsClasspath;
import com.eshabakhov.surok.school.SlsBrief;
import com.eshabakhov.surok.school.SlsPostgres;
import com.eshabakhov.surok.security.CsToken;
import com.eshabakhov.surok.user.UrBrief;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.jooq.DSLContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * HTTP controller for federal curriculums.
 *
 * @since 0.0.1
 * @checkstyle ClassFanOutComplexityCheck (1000 lines)
 * @checkstyle ParameterNumberCheck (1000 lines)
 */
@RestController
@RequestMapping("/federal/curriculums")
@Tag(name = "Federal Curriculums")
@PreAuthorize("isAuthenticated()")
public class CrsHttp {

    /** JOOQ context. */
    private final DSLContext ctx;

    /** Origin curriculums. */
    private final Curriculums origin;

    /**
     * New controller.
     *
     * @param ctx JOOQ context
     * @since 0.0.1
     */
    public CrsHttp(final DSLContext ctx) {
        this.ctx = ctx;
        this.origin = new CrsPostgres(ctx);
    }

    /**
     * Creates a federal curriculum.
     *
     * @param parameters Curriculum input parameters
     * @param request HTTP request
     * @return Response with the negotiated representation or redirect
     * @throws Exception If creation fails
     */
    @PostMapping(
        value = "/create",
        produces = {
            MediaType.TEXT_HTML_VALUE,
            "application/vnd.surok.federal-curriculum-v1+json",
            "application/vnd.surok.federal-curriculum-v2+json",
            "application/vnd.surok.federal-curriculum-v1+xml",
            "application/vnd.surok.federal-curriculum-v2+xml"
        }
    )
    @Operation(summary = "Create federal curriculum")
    @ApiResponse(
        responseCode = "201",
        description = "Federal curriculum created"
    )
    @ApiResponse(
        responseCode = "302",
        description = "HTML client redirected to the created curriculum"
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(
        @RequestParam final MultiValueMap<String, String> parameters,
        final HttpServletRequest request
    ) throws Exception {
        return CrsHttp.result(
            new AcHttp(
                request, "federal-curriculum-v1", "federal-curriculum-v2"
            ).type(),
            this.origin.create(new CrInputHttp(parameters)), HttpStatus.CREATED
        );
    }

    /**
     * Shows a federal curriculum.
     *
     * @param curriculum Curriculum ID
     * @param request HTTP request
     * @return Response with the negotiated representation or redirect
     * @throws Exception If lookup fails
     */
    @GetMapping(
        value = {"/{curriculum}", "/{curriculum}/edit"},
        produces = {
            MediaType.TEXT_HTML_VALUE,
            "application/vnd.surok.federal-curriculum-v1+json",
            "application/vnd.surok.federal-curriculum-v2+json",
            "application/vnd.surok.federal-curriculum-v1+xml",
            "application/vnd.surok.federal-curriculum-v2+xml"
        }
    )
    @Operation(summary = "Get federal curriculum")
    @ApiResponse(
        responseCode = "200",
        description = "Federal curriculum",
        content = @Content(schema = @Schema(type = "object"))
    )
    @ApiResponse(responseCode = "404", description = "Curriculum not found")
    public ResponseEntity<?> curriculum(
        @PathVariable final long curriculum,
        final HttpServletRequest request
    ) throws Exception {
        final MediaType type = new AcHttp(
            request, "federal-curriculum-v1", "federal-curriculum-v2"
        ).type();
        final Curriculum selected = this.origin.curriculum(curriculum);
        return ResponseEntity.ok().contentType(new MediaType(type, StandardCharsets.UTF_8)).body(
            switch (type.toString()) {
                case MediaType.TEXT_HTML_VALUE -> {
                    final String view;
                    final String uri = request.getRequestURI();
                    if (uri != null && uri.endsWith("/edit")) {
                        if (!request.isUserInRole("ADMIN")) {
                            throw new ResponseStatusException(
                                HttpStatus.FORBIDDEN, "Only administrators can change curricula"
                            );
                        }
                        view = "edit";
                    } else {
                        view = "details";
                    }
                    yield this.web(request, "xslt/federal-curriculum.xsl", view).with(
                        "curriculums", new CrsSingle(this.origin, selected)
                    ).print();
                }
                case "application/vnd.surok.federal-curriculum-v1+json" ->
                    new CrV1(selected).print(new MdJson()).print();
                case "application/vnd.surok.federal-curriculum-v2+json" ->
                    new CrV2(selected).print(new MdJson()).print();
                case "application/vnd.surok.federal-curriculum-v1+xml" ->
                    new CrV1(selected).print(new MdXml("curriculum")).print();
                case "application/vnd.surok.federal-curriculum-v2+xml" ->
                    new CrV2(selected).print(new MdXml("curriculum")).print();
                case null, default -> throw new ResponseStatusException(
                    HttpStatus.NOT_ACCEPTABLE, "Unsupported Accept header"
                );
            }
        );
    }

    /**
     * Updates a federal curriculum.
     *
     * @param curriculum Curriculum ID
     * @param parameters Curriculum input parameters
     * @param request HTTP request
     * @return Response with the negotiated representation or redirect
     * @throws Exception If update fails
     */
    @PostMapping(
        value = "/{curriculum}/edit",
        produces = {
            MediaType.TEXT_HTML_VALUE,
            "application/vnd.surok.federal-curriculum-v1+json",
            "application/vnd.surok.federal-curriculum-v2+json",
            "application/vnd.surok.federal-curriculum-v1+xml",
            "application/vnd.surok.federal-curriculum-v2+xml"
        }
    )
    @Operation(summary = "Update federal curriculum")
    @ApiResponse(
        responseCode = "200",
        description = "Federal curriculum updated"
    )
    @ApiResponse(
        responseCode = "302",
        description = "HTML client redirected to the updated curriculum"
    )
    @ApiResponse(responseCode = "404", description = "Curriculum not found")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> revise(
        @PathVariable final long curriculum,
        @RequestParam final MultiValueMap<String, String> parameters,
        final HttpServletRequest request
    ) throws Exception {
        return CrsHttp.result(
            new AcHttp(
                request, "federal-curriculum-v1", "federal-curriculum-v2"
            ).type(),
            this.origin.revise(curriculum, new CrInputHttp(parameters)), HttpStatus.OK
        );
    }

    /**
     * Shows federal curriculums.
     *
     * @param filters Filtering
     * @param page Pagination
     * @param sorts Sorting
     * @param request HTTP request
     * @return Response with the negotiated representation or redirect
     * @throws Exception If selection fails
     */
    @GetMapping(
        value = {"", "/fragment", "/create"},
        produces = {
            MediaType.TEXT_HTML_VALUE,
            "application/vnd.surok.federal-curriculums.v1+json",
            "application/vnd.surok.federal-curriculums.v1+xml"
        }
    )
    @Operation(summary = "List federal curriculums")
    @ApiResponse(
        responseCode = "200",
        description = "Federal curriculums",
        content = @Content(schema = @Schema(type = "object"))
    )
    public ResponseEntity<?> selection(
        final Filters filters,
        final Page page,
        final Sorts sorts,
        final HttpServletRequest request
    ) throws Exception {
        final MediaType type = new AcHttp(request, "federal-curriculums.v1").type();
        final Curriculums selected = this.origin.selection(filters, page, sorts);
        return ResponseEntity.ok().contentType(new MediaType(type, StandardCharsets.UTF_8)).body(
            switch (type.toString()) {
                case MediaType.TEXT_HTML_VALUE -> {
                    final String view;
                    final String uri = request.getRequestURI();
                    if (uri != null && uri.endsWith("/fragment")) {
                        view = "list-fragment";
                    } else if (uri != null && uri.endsWith("/create")) {
                        if (!request.isUserInRole("ADMIN")) {
                            throw new ResponseStatusException(
                                HttpStatus.FORBIDDEN, "Only administrators can change curricula"
                            );
                        }
                        view = "create";
                    } else {
                        view = "list";
                    }
                    yield this.web(request, "xslt/federal-curriculums.xsl", view).with(
                        "curriculums", new CrsWeb(selected, filters, sorts)
                    ).print();
                }
                case "application/vnd.surok.federal-curriculums.v1+json" ->
                    selected.print(new MdJson()).print();
                case "application/vnd.surok.federal-curriculums.v1+xml" ->
                    selected.print(new MdXml("curriculums")).print();
                case null, default -> throw new ResponseStatusException(
                    HttpStatus.NOT_ACCEPTABLE, "Unsupported Accept header"
                );
            }
        );
    }

    /**
     * Removes a federal curriculum.
     *
     * @param id Curriculum ID
     * @return Empty response
     * @throws Exception If removal fails
     */
    @DeleteMapping("/{curriculum}")
    @Operation(summary = "Remove federal curriculum")
    @ApiResponse(
        responseCode = "204",
        description = "Federal curriculum removed"
    )
    @ApiResponse(responseCode = "404", description = "Curriculum not found")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> remove(
        @PathVariable(name = "curriculum") final long id
    ) throws Exception {
        this.origin.remove(id);
        return ResponseEntity.noContent().build();
    }

    private static ResponseEntity<?> result(
        final MediaType type, final Curriculum selected, final HttpStatus status
    ) throws Exception {
        final URI location = URI.create(
            String.format("/federal/curriculums/%d", selected.uid())
        );
        final ResponseEntity.BodyBuilder response = ResponseEntity.status(status)
            .contentType(new MediaType(type, StandardCharsets.UTF_8));
        if (HttpStatus.CREATED.equals(status)) {
            response.location(location);
        }
        return switch (type.toString()) {
            case MediaType.TEXT_HTML_VALUE -> ResponseEntity.status(HttpStatus.FOUND)
                .location(location).build();
            case "application/vnd.surok.federal-curriculum-v1+json" ->
                response.body(new CrV1(selected).print(new MdJson()).print());
            case "application/vnd.surok.federal-curriculum-v2+json" ->
                response.body(new CrV2(selected).print(new MdJson()).print());
            case "application/vnd.surok.federal-curriculum-v1+xml" ->
                response.body(new CrV1(selected).print(new MdXml("curriculum")).print());
            case "application/vnd.surok.federal-curriculum-v2+xml" ->
                response.body(new CrV2(selected).print(new MdXml("curriculum")).print());
            case null, default -> throw new ResponseStatusException(
                HttpStatus.NOT_ACCEPTABLE, "Unsupported Accept header"
            );
        };
    }

    private Media<String> web(
        final HttpServletRequest request, final String stylesheet, final String view
    ) throws Exception {
        return new SrWeb(
            new UrBrief(
                (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()
            ),
            new SlsBrief(new SlsPostgres(this.ctx)),
            new CsToken((CsrfToken) request.getAttribute(CsrfToken.class.getName()))
        ).print(
            new MdXslt(
                new MdXml("surok").with("path", request.getRequestURI()).with(
                    "levels", new PrItems(
                        "level", List.of(Curriculum.Level.values()).stream()
                            .map(level -> new LvLocalized(level, request.getLocale())).toList()
                    )
                ).with(
                    "weeks", new PrItems(
                        "week", List.of(Curriculum.Week.values()).stream()
                            .map(week -> new WkLocalized(week, request.getLocale())).toList()
                    )
                ),
                new XsClasspath(stylesheet, view)
            )
        );
    }
}
