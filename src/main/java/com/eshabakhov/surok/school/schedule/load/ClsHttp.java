/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.User;
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
import com.eshabakhov.surok.school.schedule.Schedule;
import com.eshabakhov.surok.school.schedule.SdBrief;
import com.eshabakhov.surok.security.CsToken;
import com.eshabakhov.surok.user.UrBrief;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import org.jooq.DSLContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Read-only applied teaching loads; changes are submitted through the load draft.
 * For example, request application/vnd.surok.class-loads.v1+json for the collection.
 *
 * @since 0.0.1
 * @checkstyle ClassFanOutComplexityCheck (1000 lines)
 * @checkstyle ParameterNumberCheck (1000 lines)
 */
@RestController
@RequestMapping("/schools/{school}/schedules/{schedule}/loads")
public class ClsHttp {

    /** Database context. */
    private final DSLContext ctx;

    /**
     * New resource.
     *
     * @param ctx Database context
     */
    public ClsHttp(final DSLContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Shows a page of applied loads or its fragment.
     *
     * @param school School identifier
     * @param schedule Schedule identifier
     * @param page Pagination
     * @param request HTTP request
     * @param actor Acting user
     * @return Negotiated representation
     * @throws Exception If lookup or printing fails
     */
    @GetMapping(
        value = {"", "/", "/fragment"},
        produces = {
            MediaType.TEXT_HTML_VALUE,
            "application/vnd.surok.class-loads.v1+json",
            "application/vnd.surok.class-loads.v1+xml"
        }
    )
    public ResponseEntity<?> selection(
        @PathVariable final long school,
        @PathVariable final long schedule,
        final Page page,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        final MediaType type = new AcHttp(request, "class-loads.v1").type();
        final School sch = new SlsAuthorized(new SlsPostgres(this.ctx), actor).school(school);
        final Schedule sched = sch.schedules().schedule(schedule);
        final ClassLoads selected = sched.loads().selection(page);
        return ResponseEntity.ok().contentType(new MediaType(type, StandardCharsets.UTF_8)).body(
            switch (type.toString()) {
                case MediaType.TEXT_HTML_VALUE -> {
                    final String view;
                    if (request.getRequestURI().endsWith("/fragment")) {
                        view = "list-fragment";
                    } else {
                        view = "list";
                    }
                    yield this.web(request, sch, sched, view).with("loads", selected).print();
                }
                case "application/vnd.surok.class-loads.v1+json" ->
                    selected.print(new MdJson()).print();
                case "application/vnd.surok.class-loads.v1+xml" ->
                    selected.print(new MdXml("loads")).print();
                case null, default -> throw new ResponseStatusException(
                    HttpStatus.NOT_ACCEPTABLE, "Unsupported Accept header"
                );
            }
        );
    }

    /**
     * Shows one applied teaching load.
     *
     * @param school School identifier
     * @param schedule Schedule identifier
     * @param load Entity identifier
     * @param request HTTP request
     * @param actor Acting user
     * @return Negotiated representation
     * @throws Exception If lookup or printing fails
     */
    @GetMapping(
        value = "/{load:[0-9]+}",
        produces = {
            MediaType.TEXT_HTML_VALUE,
            "application/vnd.surok.class-load-v1+json",
            "application/vnd.surok.class-load-v1+xml"
        }
    )
    public ResponseEntity<?> load(
        @PathVariable final long school,
        @PathVariable final long schedule,
        @PathVariable final long load,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        final MediaType type = new AcHttp(request, "class-load-v1").type();
        final School sch = new SlsAuthorized(new SlsPostgres(this.ctx), actor).school(school);
        final Schedule sched = sch.schedules().schedule(schedule);
        final ClassLoads origin = sched.loads();
        final ClassLoad selected = origin.load(load);
        return ResponseEntity.ok().contentType(new MediaType(type, StandardCharsets.UTF_8)).body(
            switch (type.toString()) {
                case MediaType.TEXT_HTML_VALUE -> this.web(request, sch, sched, "details").with(
                    "loads", new ClsSingle(origin, selected)
                ).print();
                case "application/vnd.surok.class-load-v1+json" ->
                    new ClV1(selected).print(new MdJson()).print();
                case "application/vnd.surok.class-load-v1+xml" ->
                    new ClV1(selected).print(new MdXml("load")).print();
                case null, default -> throw new ResponseStatusException(
                    HttpStatus.NOT_ACCEPTABLE, "Unsupported Accept header"
                );
            }
        );
    }

    private Media<String> web(
        final HttpServletRequest request,
        final School school,
        final Schedule schedule,
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
                new XsClasspath("xslt/class-loads.xsl", view)
            )
        ).with("school", new SlBrief(school))
            .with("schedule", new SdBrief(schedule));
    }
}
