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
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
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
 * HTTP controller for federal curriculums.
 *
 * @since 0.0.1
 * @checkstyle ClassFanOutComplexityCheck (1000 lines)
 * @checkstyle ParameterNumberCheck (1000 lines)
 */
@RestController
@RequestMapping("/federal/curriculums")
public class CrsHttp {

    /** HTML fragment representation of the curriculum collection. */
    private static final String FRAGMENT = "text/vnd.surok.federal-curriculums-fragment+html";

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
     * @param actor Authenticated user
     * @return Response with the negotiated representation or redirect
     * @throws Exception If creation fails
     */
    @PostMapping(
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
        produces = {
            MediaType.TEXT_HTML_VALUE,
            "application/vnd.surok.federal-curriculum-v1+json",
            "application/vnd.surok.federal-curriculum-v2+json",
            "application/vnd.surok.federal-curriculum-v1+xml",
            "application/vnd.surok.federal-curriculum-v2+xml"
        }
    )
    public ResponseEntity<?> create(
        @RequestParam final MultiValueMap<String, String> parameters,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        return CrsHttp.result(
            new AcHttp(
                request, "federal-curriculum-v1", "federal-curriculum-v2"
            ).type(),
            new CrsAuthorized(this.origin, actor).create(new CrInputHttp(parameters)),
            HttpStatus.CREATED
        );
    }

    /**
     * Creates a federal curriculum from versioned JSON.
     *
     * @param fields Curriculum fields
     * @param request HTTP request
     * @param actor Authenticated user
     * @return Created representation
     * @throws Exception If creation fails
     */
    @PostMapping(
        consumes = {
            "application/vnd.surok.federal-curriculum-v1+json",
            "application/vnd.surok.federal-curriculum-v2+json"
        },
        produces = {
            MediaType.TEXT_HTML_VALUE,
            "application/vnd.surok.federal-curriculum-v1+json",
            "application/vnd.surok.federal-curriculum-v2+json",
            "application/vnd.surok.federal-curriculum-v1+xml",
            "application/vnd.surok.federal-curriculum-v2+xml"
        }
    )
    public ResponseEntity<?> create(
        @RequestBody final Map<String, String> fields,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        return CrsHttp.result(
            new AcHttp(request, "federal-curriculum-v1", "federal-curriculum-v2").type(),
            new CrsAuthorized(this.origin, actor).create(new CrInputHttp(fields)),
            HttpStatus.CREATED
        );
    }

    /**
     * Shows a federal curriculum.
     *
     * @param curriculum Curriculum ID
     * @param request HTTP request
     * @param actor Authenticated user
     * @return Response with the negotiated representation or redirect
     * @throws Exception If lookup fails
     */
    @GetMapping(
        value = "/{curriculum}",
        produces = {
            MediaType.TEXT_HTML_VALUE,
            "application/vnd.surok.federal-curriculum-v1+json",
            "application/vnd.surok.federal-curriculum-v2+json",
            "application/vnd.surok.federal-curriculum-v1+xml",
            "application/vnd.surok.federal-curriculum-v2+xml"
        }
    )
    public ResponseEntity<?> curriculum(
        @PathVariable final long curriculum,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        final MediaType type = new AcHttp(
            request, "federal-curriculum-v1", "federal-curriculum-v2"
        ).type();
        final Curriculums curriculums = new CrsAuthorized(this.origin, actor);
        final Curriculum selected = curriculums.curriculum(curriculum);
        return ResponseEntity.ok().contentType(new MediaType(type, StandardCharsets.UTF_8)).body(
            switch (type.toString()) {
                case MediaType.TEXT_HTML_VALUE ->
                    this.web(request, "xslt/federal-curriculum.xsl", "details").with(
                        "curriculums", new CrsSingle(curriculums, selected)
                    ).print();
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
     * Shows the HTML editing form.
     *
     * @param curriculum Curriculum ID
     * @param request HTTP request
     * @param actor Authenticated user
     * @return HTML editing form
     * @throws Exception If lookup fails
     */
    @GetMapping(value = "/{curriculum}/edit", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> edit(
        @PathVariable final long curriculum,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        final MediaType type = new AcHttp(request, List.of(MediaType.TEXT_HTML)).type();
        if (!actor.isAdmin()) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Only administrators can change curricula"
            );
        }
        final Curriculums curriculums = new CrsAuthorized(this.origin, actor);
        return ResponseEntity.ok().contentType(new MediaType(type, StandardCharsets.UTF_8)).body(
            this.web(request, "xslt/federal-curriculum.xsl", "edit").with(
                "curriculums", new CrsSingle(curriculums, curriculums.curriculum(curriculum))
            ).print()
        );
    }

    /**
     * Updates a federal curriculum.
     *
     * @param curriculum Curriculum ID
     * @param fields Curriculum input fields
     * @param request HTTP request
     * @param actor Authenticated user
     * @return Response with the negotiated representation or redirect
     * @throws Exception If update fails
     */
    @PutMapping(
        value = "/{curriculum}",
        consumes = {
            "application/vnd.surok.federal-curriculum-v1+json",
            "application/vnd.surok.federal-curriculum-v2+json"
        },
        produces = {
            MediaType.TEXT_HTML_VALUE,
            "application/vnd.surok.federal-curriculum-v1+json",
            "application/vnd.surok.federal-curriculum-v2+json",
            "application/vnd.surok.federal-curriculum-v1+xml",
            "application/vnd.surok.federal-curriculum-v2+xml"
        }
    )
    public ResponseEntity<?> revise(
        @PathVariable final long curriculum,
        @RequestBody final Map<String, String> fields,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        return CrsHttp.result(
            new AcHttp(
                request, "federal-curriculum-v1", "federal-curriculum-v2"
            ).type(),
            new CrsAuthorized(this.origin, actor).revise(
                curriculum, new CrInputHttp(fields)
            ), HttpStatus.OK
        );
    }

    /**
     * Shows federal curriculums.
     *
     * @param filters Filtering
     * @param page Pagination
     * @param sorts Sorting
     * @param request HTTP request
     * @param actor Authenticated user
     * @return Response with the negotiated representation or redirect
     * @throws Exception If selection fails
     */
    @GetMapping(
        value = "",
        produces = {
            MediaType.TEXT_HTML_VALUE,
            "application/vnd.surok.federal-curriculums.v1+json",
            "application/vnd.surok.federal-curriculums.v1+xml",
            CrsHttp.FRAGMENT
        }
    )
    public ResponseEntity<?> selection(
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
                MediaType.valueOf("application/vnd.surok.federal-curriculums.v1+json"),
                MediaType.valueOf("application/vnd.surok.federal-curriculums.v1+xml"),
                MediaType.valueOf(CrsHttp.FRAGMENT)
            )
        ).type();
        final Curriculums selected = new CrsAuthorized(this.origin, actor).selection(
            filters, page, sorts
        );
        return ResponseEntity.ok().contentType(new MediaType(type, StandardCharsets.UTF_8))
            .varyBy(HttpHeaders.ACCEPT).body(
                switch (type.toString()) {
                    case MediaType.TEXT_HTML_VALUE, CrsHttp.FRAGMENT -> {
                        final String view;
                        if (MediaType.TEXT_HTML.equals(type)) {
                            view = "list";
                        } else {
                            view = "list-fragment";
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
     * Shows the HTML creation form.
     *
     * @param request HTTP request
     * @param actor Authenticated user
     * @return HTML creation form
     * @throws Exception If printing fails
     */
    @GetMapping(value = "/create", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> form(
        final HttpServletRequest request,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        final MediaType type = new AcHttp(request, List.of(MediaType.TEXT_HTML)).type();
        if (!actor.isAdmin()) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Only administrators can change curricula"
            );
        }
        return ResponseEntity.ok().contentType(new MediaType(type, StandardCharsets.UTF_8)).body(
            this.web(request, "xslt/federal-curriculums.xsl", "create").print()
        );
    }

    /**
     * Removes a federal curriculum.
     *
     * @param id Curriculum ID
     * @param actor Authenticated user
     * @return Empty response
     * @throws Exception If removal fails
     */
    @DeleteMapping("/{curriculum}")
    public ResponseEntity<Void> remove(
        @PathVariable(name = "curriculum") final long id,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        new CrsAuthorized(this.origin, actor).remove(id);
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
