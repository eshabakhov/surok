/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load.autofill;

import com.eshabakhov.surok.PrItems;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.federal.curriculum.CrOffered;
import com.eshabakhov.surok.federal.curriculum.CrsPostgres;
import com.eshabakhov.surok.federal.curriculum.Curriculum;
import com.eshabakhov.surok.federal.curriculum.LvLocalized;
import com.eshabakhov.surok.federal.curriculum.WkLocalized;
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
import com.eshabakhov.surok.security.right.RtAdmin;
import com.eshabakhov.surok.security.right.RtDirectorOrDeputy;
import com.eshabakhov.surok.security.right.RtEither;
import com.eshabakhov.surok.user.UrBrief;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.jooq.DSLContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * One planning draft resource per school schedule, without API route aliases.
 * For example, PUT replaces the draft and POST /application applies its load.
 * JSON input and JSON/XML output use application/vnd.surok.load-draft.v1 media types.
 *
 * @since 0.0.1
 * @checkstyle ClassFanOutComplexityCheck (12 lines)
 */
@RestController
@RequestMapping("/schools/{school}/schedules/{schedule}/loads/draft")
public class LdHttp {

    /** Database context. */
    private final DSLContext ctx;

    /** Supported draft representations in preference order. */
    private final MediaType[] types;

    /**
     * New planning controller.
     *
     * @param ctx Database context
     */
    public LdHttp(final DSLContext ctx) {
        this.ctx = ctx;
        this.types = new MediaType[] {
            new MediaType("application", "vnd.surok.load-draft.v1+json"),
            new MediaType("application", "vnd.surok.load-draft.v1+xml"),
        };
    }

    /**
     * Read the plan, applicable classes, calculated hours, and additional allowance usage.
     *
     * @param school School identifier
     * @param schedule Schedule identifier
     * @param request Incoming request
     * @param actor Acting user
     * @return Saved draft representation
     * @throws Exception If the resource cannot be read
     * @checkstyle ParameterNumberCheck (93 lines)
     */
    @GetMapping(
        value = {"", "/fragment"},
        produces = {
            "application/vnd.surok.load-draft.v1+json",
            "application/vnd.surok.load-draft.v1+xml",
            MediaType.TEXT_HTML_VALUE
        }
    )
    public ResponseEntity<String> read(
        @PathVariable("school") final long school,
        @PathVariable("schedule") final long schedule,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        final MediaType type = new AcHttp(
            request,
            List.of(this.types[0], this.types[1], MediaType.TEXT_HTML)
        ).type();
        final School source = new SlsAuthorized(
            new SlsPostgres(this.ctx), actor
        ).school(school);
        final Schedule selected = source.schedules().schedule(schedule);
        final LoadDraft draft = selected.draft();
        return ResponseEntity.ok().contentType(new MediaType(type, StandardCharsets.UTF_8))
            .varyBy(HttpHeaders.ACCEPT).body(
                switch (type.toString()) {
                    case MediaType.TEXT_HTML_VALUE -> {
                        final String view;
                        if (request.getRequestURI().endsWith("/fragment")) {
                            view = "draft-fragment";
                        } else {
                            view = "page";
                        }
                        final List<CrOffered> offers = new ArrayList<>(0);
                        for (final Curriculum curriculum : new CrsPostgres(this.ctx).iterate()) {
                            offers.add(new CrOffered(curriculum));
                        }
                        yield new LdWeb(draft).print(
                            new SrWeb(
                                new UrBrief(actor),
                                new SlsBrief(new SlsPostgres(this.ctx)),
                                new CsToken(
                                    (CsrfToken) request.getAttribute(CsrfToken.class.getName())
                                )
                            ).print(
                                new MdXslt(
                                    new MdXml("surok").with("path", request.getRequestURI()),
                                    new XsClasspath("xslt/load-draft.xsl", view)
                                )
                            ).with(
                                "levels", new PrItems(
                                    "level", List.of(Curriculum.Level.values()).stream()
                                        .map(level -> new LvLocalized(level, request.getLocale()))
                                        .toList()
                                )
                            ).with(
                                "weeks", new PrItems(
                                    "week", List.of(Curriculum.Week.values()).stream()
                                        .map(week -> new WkLocalized(week, request.getLocale()))
                                        .toList()
                                )
                            ).with("school", new SlBrief(source))
                                .with("schedule", new SdBrief(selected))
                                .with("classes", source.schoolClasses())
                                .with("subjects", source.subjects())
                                .with("offers", new PrItems("curriculum", offers)).with(
                                    "editable",
                                    new RtEither(
                                        new RtAdmin(actor),
                                        new RtDirectorOrDeputy(actor, source)
                                    ).allows()
                                )
                        ).print();
                    }
                    case "application/vnd.surok.load-draft.v1+json" ->
                        draft.print(new MdJson()).print().toString();
                    case "application/vnd.surok.load-draft.v1+xml" ->
                        draft.print(new MdXml("draft")).print();
                    case null, default -> throw new ResponseStatusException(
                        HttpStatus.NOT_ACCEPTABLE, "Unsupported Accept header"
                    );
                }
            );
    }

    /**
     * Replace the curriculum and grade additions without applying actual loads.
     *
     * @param school School identifier
     * @param schedule Schedule identifier
     * @param request Incoming request
     * @param document Complete draft input
     * @param actor Acting user
     * @return Saved draft representation
     * @throws Exception If validation or persistence fails
     * @checkstyle ParameterNumberCheck (105 lines)
     */
    @PutMapping(
        consumes = "application/vnd.surok.load-draft.v1+json",
        produces = {
            "application/vnd.surok.load-draft.v1+json",
            "application/vnd.surok.load-draft.v1+xml"
        }
    )
    public ResponseEntity<String> save(
        @PathVariable("school") final long school,
        @PathVariable("schedule") final long schedule,
        final HttpServletRequest request,
        @RequestBody final JsonNode document,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        final MediaType type = new AcHttp(request, List.of(this.types)).type();
        final LoadDraft draft = new SlsAuthorized(
            new SlsPostgres(this.ctx), actor
        ).school(school).schedules().schedule(schedule).draft();
        HttpStatus status;
        try {
            draft.curriculum();
            status = HttpStatus.OK;
        } catch (final LoadDraftNotFoundException exception) {
            status = HttpStatus.CREATED;
        }
        draft.save(new LdiJson(this.ctx, document));
        final ResponseEntity.BodyBuilder response = ResponseEntity.status(status)
            .contentType(new MediaType(type, StandardCharsets.UTF_8)).varyBy(HttpHeaders.ACCEPT);
        if (status == HttpStatus.CREATED) {
            response.header(
                HttpHeaders.LOCATION,
                String.format("/schools/%d/schedules/%d/loads/draft", school, schedule)
            );
        }
        return response.body(
            switch (type.toString()) {
                case "application/vnd.surok.load-draft.v1+json" ->
                    draft.print(new MdJson()).print().toString();
                case "application/vnd.surok.load-draft.v1+xml" ->
                    draft.print(new MdXml("draft")).print();
                case null, default -> throw new ResponseStatusException(
                    HttpStatus.NOT_ACCEPTABLE, "Unsupported Accept header"
                );
            }
        );
    }

    /**
     * Switch the curriculum and clear grade additions when the plan changes.
     *
     * @param school School identifier
     * @param schedule Schedule identifier
     * @param request Incoming request
     * @param document Replacement curriculum reference containing its id
     * @param actor Acting user
     * @return Recalculated draft representation
     * @throws Exception If switching fails
     * @checkstyle ParameterNumberCheck (214 lines)
     */
    @PutMapping(
        value = "/curriculum", consumes = "application/vnd.surok.load-draft.v1+json",
        produces = {
            "application/vnd.surok.load-draft.v1+json",
            "application/vnd.surok.load-draft.v1+xml"
        }
    )
    public ResponseEntity<String> revise(
        @PathVariable("school") final long school,
        @PathVariable("schedule") final long schedule,
        final HttpServletRequest request,
        @RequestBody final JsonNode document,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        final MediaType type = new AcHttp(request, List.of(this.types)).type();
        final LoadDraft draft = new SlsAuthorized(
            new SlsPostgres(this.ctx), actor
        ).school(school).schedules().schedule(schedule).draft();
        draft.revise(new LdiJson(this.ctx, document));
        return ResponseEntity.ok().contentType(new MediaType(type, StandardCharsets.UTF_8))
            .varyBy(HttpHeaders.ACCEPT).body(
                switch (type.toString()) {
                    case "application/vnd.surok.load-draft.v1+json" ->
                        draft.print(new MdJson()).print().toString();
                    case "application/vnd.surok.load-draft.v1+xml" ->
                        draft.print(new MdXml("draft")).print();
                    case null, default -> throw new ResponseStatusException(
                        HttpStatus.NOT_ACCEPTABLE, "Unsupported Accept header"
                    );
                }
            );
    }

    /**
     * Apply mandatory hours and saved grade additions after checking per-class allowances.
     *
     * @param school School identifier
     * @param schedule Schedule identifier
     * @param request Incoming request
     * @param actor Acting user
     * @return Draft with the updated actual loads
     * @throws Exception If applying the draft fails
     * @checkstyle ParameterNumberCheck (200 lines)
     */
    @PostMapping(
        value = "/application",
        produces = {
            "application/vnd.surok.load-draft.v1+json",
            "application/vnd.surok.load-draft.v1+xml"
        }
    )
    public ResponseEntity<String> apply(
        @PathVariable("school") final long school,
        @PathVariable("schedule") final long schedule,
        final HttpServletRequest request,
        @AuthenticationPrincipal final User actor
    ) throws Exception {
        final MediaType type = new AcHttp(request, List.of(this.types)).type();
        final LoadDraft draft = new SlsAuthorized(
            new SlsPostgres(this.ctx), actor
        ).school(school).schedules().schedule(schedule).draft();
        draft.apply();
        return ResponseEntity.ok().contentType(new MediaType(type, StandardCharsets.UTF_8))
            .varyBy(HttpHeaders.ACCEPT).body(
                switch (type.toString()) {
                    case "application/vnd.surok.load-draft.v1+json" ->
                        draft.print(new MdJson()).print().toString();
                    case "application/vnd.surok.load-draft.v1+xml" ->
                        draft.print(new MdXml("draft")).print();
                    case null, default -> throw new ResponseStatusException(
                        HttpStatus.NOT_ACCEPTABLE, "Unsupported Accept header"
                    );
                }
            );
    }
}
