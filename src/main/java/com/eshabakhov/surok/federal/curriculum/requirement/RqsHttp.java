/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum.requirement;

import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.Sorts;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.federal.curriculum.CrWithRequirements;
import com.eshabakhov.surok.federal.curriculum.CrsAuthorized;
import com.eshabakhov.surok.federal.curriculum.CrsPostgres;
import com.eshabakhov.surok.federal.curriculum.CrsSingle;
import com.eshabakhov.surok.federal.curriculum.Curriculum;
import com.eshabakhov.surok.federal.curriculum.requirement.input.RqInputForm;
import com.eshabakhov.surok.federal.curriculum.requirement.input.RqInputHttp;
import com.eshabakhov.surok.federal.subject.SbsFederal;
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
 * HTTP controller for federal curriculum requirements.
 *
 * @since 0.0.1
 * @checkstyle ClassFanOutComplexityCheck (1000 lines)
 * @checkstyle ParameterNumberCheck (1000 lines)
 */
@RestController
@RequestMapping("/federal/curriculums/{curriculum}/requirements")
public class RqsHttp {

    /** JOOQ context. */
    private final DSLContext ctx;

    /**
     * New controller.
     *
     * @param ctx JOOQ context
     * @since 0.0.1
     */
    public RqsHttp(final DSLContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Shows curriculum requirements.
     *
     * @param current Curriculum ID
     * @param filters Filtering
     * @param page Pagination
     * @param sorts Sorting
     * @param request HTTP request
     * @param actor Authenticated user
     * @return Response with the negotiated representation or redirect
     * @throws Exception If selection fails
     */
    @GetMapping(
        value = {"", "/fragment"},
        produces = {
            MediaType.TEXT_HTML_VALUE,
            "application/vnd.surok.federal-curriculum-requirements.v1+json",
            "application/vnd.surok.federal-curriculum-requirements.v1+xml"
        }
    )
    public ResponseEntity<?> selection(
        @PathVariable("curriculum") final long current,
        final Filters filters,
        final Page page,
        final Sorts sorts,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        final MediaType type = new AcHttp(request, "federal-curriculum-requirements.v1").type();
        final Curriculum curriculum = new CrsAuthorized(
            new CrsPostgres(this.ctx), actor
        ).curriculum(current);
        final Requirements selected = curriculum.requirements().selection(filters, page, sorts);
        return ResponseEntity.ok().contentType(new MediaType(type, StandardCharsets.UTF_8)).body(
            switch (type.toString()) {
                case MediaType.TEXT_HTML_VALUE -> {
                    final String view;
                    final String uri = request.getRequestURI();
                    if (uri != null && uri.endsWith("/fragment")) {
                        view = "list-fragment";
                    } else {
                        view = "list";
                    }
                    yield this.web(request, view).with(
                        "subjects", new SbsFederal(this.ctx)
                    ).with("firstGrade", curriculum.grades().getFirst()).with(
                        "lastGrade", curriculum.grades().getLast()
                    ).with(
                        "curriculums",
                        new CrsSingle(
                            new CrsPostgres(this.ctx),
                            new CrWithRequirements(
                                curriculum,
                                new RqsWeb(selected, filters, sorts)
                            )
                        )
                    ).print();
                }
                case "application/vnd.surok.federal-curriculum-requirements.v1+json" ->
                    selected.print(new MdJson()).print();
                case "application/vnd.surok.federal-curriculum-requirements.v1+xml" ->
                    selected.print(new MdXml("requirements")).print();
                case null, default -> throw new ResponseStatusException(
                    HttpStatus.NOT_ACCEPTABLE, "Unsupported Accept header"
                );
            }
        );
    }

    /**
     * Shows one curriculum requirement.
     *
     * @param current Curriculum ID
     * @param requirement Requirement ID
     * @param request HTTP request
     * @param actor Authenticated user
     * @return Response with the negotiated representation or redirect
     * @throws Exception If lookup fails
     */
    @GetMapping(
        value = "/{requirement}",
        produces = {
            "application/vnd.surok.federal-curriculum-requirement-v1+json",
            "application/vnd.surok.federal-curriculum-requirement-v1+xml"
        }
    )
    public ResponseEntity<?> requirement(
        @PathVariable("curriculum") final long current,
        @PathVariable final long requirement,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        final MediaType type = new AcHttp(
            request,
            List.of(
                MediaType.parseMediaType(
                    "application/vnd.surok.federal-curriculum-requirement-v1+json"
                ),
                MediaType.parseMediaType(
                    "application/vnd.surok.federal-curriculum-requirement-v1+xml"
                )
            )
        ).type();
        final Requirement selected = new RqsAuthorized(
            new RqsPostgres(this.ctx, current), actor
        ).requirement(requirement);
        return ResponseEntity.ok().contentType(new MediaType(type, StandardCharsets.UTF_8)).body(
            switch (type.toString()) {
                case "application/vnd.surok.federal-curriculum-requirement-v1+json" ->
                    new RqV1(selected).print(new MdJson()).print();
                case "application/vnd.surok.federal-curriculum-requirement-v1+xml" ->
                    new RqV1(selected).print(new MdXml("requirement")).print();
                case null, default -> throw new ResponseStatusException(
                    HttpStatus.NOT_ACCEPTABLE, "Unsupported Accept header"
                );
            }
        );
    }

    /**
     * Creates a curriculum requirement.
     *
     * @param current Curriculum ID
     * @param parameters Requirement input parameters
     * @param request HTTP request
     * @param actor Authenticated user
     * @return Response with the negotiated representation or redirect
     * @throws Exception If creation fails
     */
    @PostMapping(
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
        produces = {
            MediaType.TEXT_HTML_VALUE,
            "application/vnd.surok.federal-curriculum-requirement-v1+json",
            "application/vnd.surok.federal-curriculum-requirement-v1+xml"
        }
    )
    public ResponseEntity<?> create(
        @PathVariable("curriculum") final long current,
        @RequestParam final MultiValueMap<String, String> parameters,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        return RqsHttp.result(
            new AcHttp(request, "federal-curriculum-requirement-v1").type(),
            new RqsAuthorized(new RqsPostgres(this.ctx, current), actor).create(
                new RqInputForm(parameters.toSingleValueMap())
            ),
            HttpStatus.CREATED, current
        );
    }

    /**
     * Updates a curriculum requirement.
     *
     * @param current Curriculum ID
     * @param requirement Requirement ID
     * @param parameters Requirement input parameters
     * @param request HTTP request
     * @param actor Authenticated user
     * @return Response with the negotiated representation or redirect
     * @throws Exception If update fails
     */
    @PostMapping(
        value = "/{requirement}",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
        produces = {
            MediaType.TEXT_HTML_VALUE,
            "application/vnd.surok.federal-curriculum-requirement-v1+json",
            "application/vnd.surok.federal-curriculum-requirement-v1+xml"
        }
    )
    public ResponseEntity<?> revise(
        @PathVariable("curriculum") final long current,
        @PathVariable final long requirement,
        @RequestParam final MultiValueMap<String, String> parameters,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        return RqsHttp.result(
            new AcHttp(request, "federal-curriculum-requirement-v1").type(),
            new RqsAuthorized(new RqsPostgres(this.ctx, current), actor).revise(
                requirement, new RqInputHttp(parameters)
            ),
            HttpStatus.OK, current
        );
    }

    /**
     * Creates either requirement kind from versioned API fields.
     *
     * @param current Curriculum identity
     * @param fields Requirement fields
     * @param request HTTP request
     * @param actor Authenticated user
     * @return Created requirement
     * @throws Exception If creation fails
     */
    @PostMapping(
        consumes = "application/vnd.surok.federal-curriculum-requirement-v1+json",
        produces = {
            MediaType.TEXT_HTML_VALUE,
            "application/vnd.surok.federal-curriculum-requirement-v1+json",
            "application/vnd.surok.federal-curriculum-requirement-v1+xml"
        }
    )
    public ResponseEntity<?> add(
        @PathVariable("curriculum") final long current,
        @RequestBody final Map<String, String> fields,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        return RqsHttp.result(
            new AcHttp(request, "federal-curriculum-requirement-v1").type(),
            new RqsAuthorized(new RqsPostgres(this.ctx, current), actor).create(
                new RqInputHttp(fields)
            ),
            HttpStatus.CREATED, current
        );
    }

    /**
     * Replaces the fields of an existing requirement without changing its kind.
     *
     * @param current Curriculum identity
     * @param requirement Requirement identity
     * @param fields Requirement fields
     * @param request HTTP request
     * @param actor Authenticated user
     * @return Updated requirement
     * @throws Exception If revision fails
     */
    @PutMapping(
        value = "/{requirement}",
        consumes = "application/vnd.surok.federal-curriculum-requirement-v1+json",
        produces = {
            MediaType.TEXT_HTML_VALUE,
            "application/vnd.surok.federal-curriculum-requirement-v1+json",
            "application/vnd.surok.federal-curriculum-requirement-v1+xml"
        }
    )
    public ResponseEntity<?> update(
        @PathVariable("curriculum") final long current,
        @PathVariable final long requirement,
        @RequestBody final Map<String, String> fields,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        return RqsHttp.result(
            new AcHttp(request, "federal-curriculum-requirement-v1").type(),
            new RqsAuthorized(new RqsPostgres(this.ctx, current), actor).revise(
                requirement, new RqInputHttp(fields)
            ),
            HttpStatus.OK, current
        );
    }

    /**
     * Removes a curriculum requirement.
     *
     * @param current Curriculum ID
     * @param requirement Requirement ID
     * @param request HTTP request
     * @param actor Authenticated user
     * @return Empty response
     * @throws Exception If removal fails
     */
    @DeleteMapping("/{requirement}")
    public ResponseEntity<Void> remove(
        @PathVariable("curriculum") final long current,
        @PathVariable final long requirement,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        new AcHttp(request, "federal-curriculum-requirement-v1").type();
        new RqsAuthorized(new RqsPostgres(this.ctx, current), actor).remove(requirement);
        return ResponseEntity.noContent().build();
    }

    private static ResponseEntity<?> result(
        final MediaType type,
        final Requirement selected,
        final HttpStatus status,
        final long current
    ) throws Exception {
        final String base = String.format("/federal/curriculums/%d/requirements", current);
        final ResponseEntity.BodyBuilder response = ResponseEntity.status(status)
            .contentType(new MediaType(type, StandardCharsets.UTF_8));
        if (HttpStatus.CREATED.equals(status)) {
            response.location(URI.create(String.format("%s/%d", base, selected.uid())));
        }
        return switch (type.toString()) {
            case MediaType.TEXT_HTML_VALUE -> ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(base)).build();
            case "application/vnd.surok.federal-curriculum-requirement-v1+json" ->
                response.body(new RqV1(selected).print(new MdJson()).print());
            case "application/vnd.surok.federal-curriculum-requirement-v1+xml" ->
                response.body(new RqV1(selected).print(new MdXml("requirement")).print());
            case null, default -> throw new ResponseStatusException(
                HttpStatus.NOT_ACCEPTABLE, "Unsupported Accept header"
            );
        };
    }

    private Media<String> web(final HttpServletRequest request, final String view)
        throws Exception {
        return new SrWeb(
            new UrBrief(
                (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()
            ),
            new SlsBrief(new SlsPostgres(this.ctx)),
            new CsToken((CsrfToken) request.getAttribute(CsrfToken.class.getName()))
        ).print(
            new MdXslt(
                new MdXml("surok").with("path", request.getRequestURI()),
                new XsClasspath("xslt/federal-curriculum-requirements.xsl", view)
            )
        );
    }
}
