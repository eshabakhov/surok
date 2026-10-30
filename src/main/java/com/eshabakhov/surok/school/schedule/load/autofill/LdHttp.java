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
import com.eshabakhov.surok.media.AcHttp;
import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.media.MdXml;
import com.eshabakhov.surok.media.MdXslt;
import com.eshabakhov.surok.media.SrWeb;
import com.eshabakhov.surok.media.XsClasspath;
import com.eshabakhov.surok.school.SlBrief;
import com.eshabakhov.surok.school.SlsBrief;
import com.eshabakhov.surok.school.SlsPostgres;
import com.eshabakhov.surok.school.schedule.Schedule;
import com.eshabakhov.surok.school.schedule.SdBrief;
import com.eshabakhov.surok.security.CsToken;
import com.eshabakhov.surok.user.UrBrief;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.jooq.DSLContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
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
 * For example, PUT replaces the draft, PUT /curriculum switches its plan,
 * and POST /application applies mandatory hours and saved grade additions.
 * JSON input and JSON/XML output use application/vnd.surok.load-draft.v1 media types.
 *
 * @since 0.0.1
 * @checkstyle ClassFanOutComplexityCheck (12 lines)
 */
@RestController
@RequestMapping("/schools/{school}/schedules/{schedule}/loads/draft")
@PreAuthorize(
    """
    hasRole('ADMIN') or #school == authentication.principal.info().school()
    """
)
@Tag(name = "Teaching load planning")
public class LdHttp {

    /**
     * Saved draft example in JSON, referenced by response annotations.
     *
     * @checkstyle ConstantUsageCheck (2 lines)
     */
    private static final String JSON = """
        {
            "curriculum": {
                "id": 61,
                "title": "Основной план",
                "level": "BASIC",
                "week": "FIVE_DAYS",
                "version": "1",
                "year": "2026/2027",
                "description": "Учебный план основной школы"
            },
            "loads": [
                {
                    "schoolClass": {"id": 17, "name": "5А", "grade": 5, "litera": "А"},
                    "subject": {"id": 29, "name": "Математика", "origin": "FEDERAL"},
                    "hours": {"current": 5, "proposed": 3, "planned": 3},
                    "decision": "plan"
                },
                {
                    "schoolClass": {"id": 17, "name": "5А", "grade": 5, "litera": "А"},
                    "subject": {"id": 31, "name": "Музыка", "origin": "SCHOOL"},
                    "hours": {"current": 0, "planned": 2},
                    "decision": "manual"
                }
            ],
            "additions": [
                {
                    "grade": 5,
                    "requirements": [
                        {"id": 73, "grade": 5, "hours": 3, "partType": "OPTIONAL"}
                    ],
                    "subjects": [
                        {"id": 31, "name": "Музыка", "origin": "SCHOOL", "hours": 2}
                    ],
                    "classes": [
                        {
                            "id": 17,
                            "name": "5А",
                            "grade": 5,
                            "litera": "А",
                            "hours": {"current": 0, "planned": 2}
                        }
                    ]
                }
            ],
            "requirements": [
                {
                    "id": 71,
                    "grade": 5,
                    "subject": {"id": 29, "name": "Математика", "origin": "FEDERAL"},
                    "hours": 3,
                    "partType": "MANDATORY"
                },
                {"id": 73, "grade": 5, "hours": 3, "partType": "OPTIONAL"}
            ],
            "unavailable": []
        }
        """;

    /**
     * Saved draft example in XML, referenced by response annotations.
     *
     * @checkstyle ConstantUsageCheck (2 lines)
     */
    private static final String XML = """
        <draft>
            <curriculum>
                <id>61</id>
                <title>Основной план</title>
                <level>BASIC</level>
                <week>FIVE_DAYS</week>
                <version>1</version>
                <year>2026/2027</year>
                <description>Учебный план основной школы</description>
            </curriculum>
            <loads>
                <load>
                    <schoolClass>
                        <id>17</id>
                        <name>5А</name>
                        <grade>5</grade>
                        <litera>А</litera>
                    </schoolClass>
                    <subject>
                        <id>29</id>
                        <name>Математика</name>
                        <origin>FEDERAL</origin>
                    </subject>
                    <hours>
                        <current>5</current>
                        <proposed>3</proposed>
                        <planned>3</planned>
                    </hours>
                    <decision>plan</decision>
                </load>
                <load>
                    <schoolClass>
                        <id>17</id>
                        <name>5А</name>
                        <grade>5</grade>
                        <litera>А</litera>
                    </schoolClass>
                    <subject>
                        <id>31</id>
                        <name>Музыка</name>
                        <origin>SCHOOL</origin>
                    </subject>
                    <hours><current>0</current><planned>2</planned></hours>
                    <decision>manual</decision>
                </load>
            </loads>
            <additions>
                <addition>
                    <grade>5</grade>
                    <requirements>
                        <requirement>
                            <id>73</id>
                            <grade>5</grade>
                            <hours>3</hours>
                            <partType>OPTIONAL</partType>
                        </requirement>
                    </requirements>
                    <subjects>
                        <subject>
                            <id>31</id>
                            <name>Музыка</name>
                            <origin>SCHOOL</origin>
                            <hours>2</hours>
                        </subject>
                    </subjects>
                    <classes>
                        <class>
                            <id>17</id>
                            <name>5А</name>
                            <grade>5</grade>
                            <litera>А</litera>
                            <hours><current>0</current><planned>2</planned></hours>
                        </class>
                    </classes>
                </addition>
            </additions>
            <requirements>
                <requirement>
                    <id>71</id>
                    <grade>5</grade>
                    <subject>
                        <id>29</id>
                        <name>Математика</name>
                        <origin>FEDERAL</origin>
                    </subject>
                    <hours>3</hours>
                    <partType>MANDATORY</partType>
                </requirement>
                <requirement>
                    <id>73</id>
                    <grade>5</grade>
                    <hours>3</hours>
                    <partType>OPTIONAL</partType>
                </requirement>
            </requirements>
            <unavailable></unavailable>
        </draft>
        """;

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
     * @return Saved draft representation
     * @throws Exception If the resource cannot be read
     */
    @GetMapping(
        value = {"", "/fragment"},
        produces = {
            "application/vnd.surok.load-draft.v1+json",
            "application/vnd.surok.load-draft.v1+xml",
            MediaType.TEXT_HTML_VALUE
        }
    )
    @Operation(
        summary = "Read the schedule's teaching load draft",
        description = """
            Returns the saved draft. All hours are per week
            Identifiers in examples must be replaced with existing school, schedule,
            curriculum and subject identifiers
            """
    )
    @ApiResponse(
        responseCode = "200", description = "Saved teaching load draft",
        content = {
            @Content(
                mediaType = "application/vnd.surok.load-draft.v1+json",
                schema = @Schema(type = "object"),
                examples = @ExampleObject(name = "saved", value = LdHttp.JSON)
            ),
            @Content(
                mediaType = "application/vnd.surok.load-draft.v1+xml",
                schema = @Schema(ref = "#/components/schemas/LoadDraftXml"),
                examples = @ExampleObject(name = "saved", value = LdHttp.XML)
            ),
            @Content(mediaType = "text/html", schema = @Schema(type = "string"))
        }
    )
    public ResponseEntity<String> read(
        @Parameter(example = "1") @PathVariable("school") final long school,
        @Parameter(example = "101") @PathVariable("schedule") final long schedule,
        final HttpServletRequest request
    ) throws Exception {
        final MediaType type = new AcHttp(
            request,
            List.of(this.types[0], this.types[1], MediaType.TEXT_HTML)
        ).type();
        final School source = new SlsPostgres(this.ctx).school(school);
        final Schedule selected = source.schedules().schedule(schedule);
        final LoadDraft draft = new LdPostgres(this.ctx, selected);
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
                                new UrBrief(
                                    (User) SecurityContextHolder.getContext()
                                        .getAuthentication().getPrincipal()
                                ),
                                new SlsBrief(new SlsPostgres(this.ctx)),
                                new CsToken(
                                    (CsrfToken) request.getAttribute(CsrfToken.class.getName())
                                )
                            ).print(
                                new MdXslt(
                                    new MdXml("surok").with("path", request.getRequestURI()),
                                    new XsClasspath("xslt/load-draft.xsl", view)
                                )
                            ).with("school", new SlBrief(source))
                                .with("schedule", new SdBrief(selected))
                                .with("classes", source.schoolClasses())
                                .with("subjects", source.subjects())
                                .with("offers", new PrItems("curriculum", offers)).with(
                                    "editable",
                                    Stream.of(
                                        "ADMIN", "DIRECTOR", "DEPUTY_DIRECTOR",
                                        "BASIC_MAKER", "ADVANCED_MAKER", "PRO_MAKER"
                                    ).anyMatch(request::isUserInRole)
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
    @Operation(
        summary = "Save the teaching load draft without applying it",
        description = """
            Replaces the complete draft settings without changing actual loads
            Additional subjects must belong to the school; their hours apply to each
            class in the grade. An empty additions array clears the selections
            Returns 201 with Location for a new draft, or 200 for an existing draft
            A valid CSRF token is required, including when using Basic authentication
            """,
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, content = @Content(
                mediaType = "application/vnd.surok.load-draft.v1+json",
                schema = @Schema(type = "object"),
                examples = {
                    @ExampleObject(
                        name = "withAdditions", summary = "Mandatory and additional hours",
                        value = """
                            {
                                "curriculum": {
                                    "id": 61
                                },
                                "additions": [
                                    {
                                        "grade": 5,
                                        "subjects": [
                                            {
                                                "subject": {
                                                    "id": 31
                                                },
                                                "hours": 2
                                            }
                                        ]
                                    }
                                ]
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "mandatoryOnly", summary = "Mandatory hours only",
                        value = """
                            {
                                "curriculum": {
                                    "id": 61
                                },
                                "additions": []
                            }
                            """
                    )
                }
            )
        )
    )
    @ApiResponse(
        responseCode = "200", description = "Teaching load draft saved",
        content = {
            @Content(
                mediaType = "application/vnd.surok.load-draft.v1+json",
                schema = @Schema(type = "object"),
                examples = @ExampleObject(name = "saved", value = LdHttp.JSON)
            ),
            @Content(
                mediaType = "application/vnd.surok.load-draft.v1+xml",
                schema = @Schema(ref = "#/components/schemas/LoadDraftXml"),
                examples = @ExampleObject(name = "saved", value = LdHttp.XML)
            )
        }
    )
    @ApiResponse(
        responseCode = "201", description = "Teaching load draft created",
        content = {
            @Content(
                mediaType = "application/vnd.surok.load-draft.v1+json",
                schema = @Schema(type = "object"),
                examples = @ExampleObject(name = "saved", value = LdHttp.JSON)
            ),
            @Content(
                mediaType = "application/vnd.surok.load-draft.v1+xml",
                schema = @Schema(ref = "#/components/schemas/LoadDraftXml"),
                examples = @ExampleObject(name = "saved", value = LdHttp.XML)
            )
        }
    )
    @PreAuthorize(
        """
        hasAnyRole('ADMIN', 'DIRECTOR', 'DEPUTY_DIRECTOR',
            'BASIC_MAKER', 'ADVANCED_MAKER', 'PRO_MAKER')
        and (hasRole('ADMIN')
            or #school == authentication.principal.info().school())
        """
    )
    public ResponseEntity<String> save(
        @Parameter(example = "1") @PathVariable("school") final long school,
        @Parameter(example = "101") @PathVariable("schedule") final long schedule,
        final HttpServletRequest request,
        @RequestBody final JsonNode document
    ) throws Exception {
        final MediaType type = new AcHttp(request, List.of(this.types)).type();
        final LoadDraft draft = new LdPostgres(
            this.ctx, new SlsPostgres(this.ctx).school(school).schedules().schedule(schedule)
        );
        HttpStatus status;
        try {
            draft.curriculum();
            status = HttpStatus.OK;
        } catch (final LoadDraftNotFoundException exception) {
            status = HttpStatus.CREATED;
        }
        final LdiJson input = new LdiJson(this.ctx, document);
        draft.save(input.curriculum(), input.additions());
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
    @Operation(
        summary = "Switch the draft's federal curriculum",
        description = """
            Recalculates mandatory hours and clears additional selections when the
            plan changes. The request contains only the new curriculum reference
            A valid CSRF token is required, including when using Basic authentication
            """,
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, content = @Content(
                mediaType = "application/vnd.surok.load-draft.v1+json",
                schema = @Schema(type = "object"),
                examples = {
                    @ExampleObject(
                        name = "curriculum", summary = "Select another federal curriculum",
                        value = """
                            {
                                "curriculum": {
                                    "id": 67
                                }
                            }
                            """
                    )
                }
            )
        )
    )
    @ApiResponse(
        responseCode = "200", description = "Draft recalculated for the selected curriculum",
        content = {
            @Content(
                mediaType = "application/vnd.surok.load-draft.v1+json",
                schema = @Schema(type = "object"),
                examples = @ExampleObject(
                    name = "switched", value = """
                    {
                        "curriculum": {
                            "id": 67,
                            "title": "Другой план",
                            "level": "BASIC",
                            "week": "FIVE_DAYS",
                            "version": "1",
                            "year": "2026/2027",
                            "description": "Учебный план основной школы"
                        },
                        "loads": [
                            {
                                "schoolClass": {"id": 17, "name": "5А", "grade": 5, "litera": "А"},
                                "subject": {"id": 29, "name": "Математика", "origin": "FEDERAL"},
                                "hours": {"current": 5, "proposed": 4, "planned": 4},
                                "decision": "plan"
                            },
                            {
                                "schoolClass": {"id": 17, "name": "5А", "grade": 5, "litera": "А"},
                                "subject": {"id": 31, "name": "Музыка", "origin": "SCHOOL"},
                                "hours": {"current": 0, "planned": 0},
                                "decision": "none"
                            }
                        ],
                        "additions": [
                            {
                                "grade": 5,
                                "requirements": [
                                    {"id": 79, "grade": 5, "hours": 3, "partType": "OPTIONAL"}
                                ],
                                "subjects": [],
                                "classes": [
                                    {
                                        "id": 17,
                                        "name": "5А",
                                        "grade": 5,
                                        "litera": "А",
                                        "hours": {"current": 0, "planned": 0}
                                    }
                                ]
                            }
                        ],
                        "requirements": [
                            {
                                "id": 77,
                                "grade": 5,
                                "subject": {"id": 29, "name": "Математика", "origin": "FEDERAL"},
                                "hours": 4,
                                "partType": "MANDATORY"
                            },
                            {"id": 79, "grade": 5, "hours": 3, "partType": "OPTIONAL"}
                        ],
                        "unavailable": []
                    }
                    """
                )
            ),
            @Content(
                mediaType = "application/vnd.surok.load-draft.v1+xml",
                schema = @Schema(ref = "#/components/schemas/LoadDraftXml"),
                examples = @ExampleObject(
                    name = "switched", value = """
                    <draft>
                        <curriculum>
                            <id>67</id>
                            <title>Другой план</title>
                            <level>BASIC</level>
                            <week>FIVE_DAYS</week>
                            <version>1</version>
                            <year>2026/2027</year>
                            <description>Учебный план основной школы</description>
                        </curriculum>
                        <loads>
                            <load>
                                <schoolClass>
                                    <id>17</id>
                                    <name>5А</name>
                                    <grade>5</grade>
                                    <litera>А</litera>
                                </schoolClass>
                                <subject>
                                    <id>29</id>
                                    <name>Математика</name>
                                    <origin>FEDERAL</origin>
                                </subject>
                                <hours>
                                    <current>5</current>
                                    <proposed>4</proposed>
                                    <planned>4</planned>
                                </hours>
                                <decision>plan</decision>
                            </load>
                            <load>
                                <schoolClass>
                                    <id>17</id>
                                    <name>5А</name>
                                    <grade>5</grade>
                                    <litera>А</litera>
                                </schoolClass>
                                <subject>
                                    <id>31</id>
                                    <name>Музыка</name>
                                    <origin>SCHOOL</origin>
                                </subject>
                                <hours><current>0</current><planned>0</planned></hours>
                                <decision>none</decision>
                            </load>
                        </loads>
                        <additions>
                            <addition>
                                <grade>5</grade>
                                <requirements>
                                    <requirement>
                                        <id>79</id>
                                        <grade>5</grade>
                                        <hours>3</hours>
                                        <partType>OPTIONAL</partType>
                                    </requirement>
                                </requirements>
                                <subjects></subjects>
                                <classes>
                                    <class>
                                        <id>17</id>
                                        <name>5А</name>
                                        <grade>5</grade>
                                        <litera>А</litera>
                                        <hours><current>0</current><planned>0</planned></hours>
                                    </class>
                                </classes>
                            </addition>
                        </additions>
                        <requirements>
                            <requirement>
                                <id>77</id>
                                <grade>5</grade>
                                <subject>
                                    <id>29</id>
                                    <name>Математика</name>
                                    <origin>FEDERAL</origin>
                                </subject>
                                <hours>4</hours>
                                <partType>MANDATORY</partType>
                            </requirement>
                            <requirement>
                                <id>79</id>
                                <grade>5</grade>
                                <hours>3</hours>
                                <partType>OPTIONAL</partType>
                            </requirement>
                        </requirements>
                        <unavailable></unavailable>
                    </draft>
                    """
                )
            )
        }
    )
    @PreAuthorize(
        """
        hasAnyRole('ADMIN', 'DIRECTOR', 'DEPUTY_DIRECTOR',
            'BASIC_MAKER', 'ADVANCED_MAKER', 'PRO_MAKER')
        and (hasRole('ADMIN')
            or #school == authentication.principal.info().school())
        """
    )
    public ResponseEntity<String> revise(
        @Parameter(example = "1") @PathVariable("school") final long school,
        @Parameter(example = "101") @PathVariable("schedule") final long schedule,
        final HttpServletRequest request,
        @RequestBody final JsonNode document
    ) throws Exception {
        final MediaType type = new AcHttp(request, List.of(this.types)).type();
        final LoadDraft draft = new LdPostgres(
            this.ctx, new SlsPostgres(this.ctx).school(school).schedules().schedule(schedule)
        );
        draft.revise(new LdiJson(this.ctx, document).curriculum());
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
     * @return Draft with the updated actual loads
     * @throws Exception If applying the draft fails
     */
    @PostMapping(
        value = "/application",
        produces = {
            "application/vnd.surok.load-draft.v1+json",
            "application/vnd.surok.load-draft.v1+xml"
        }
    )
    @Operation(
        summary = "Apply the saved draft to the schedule's teaching loads",
        description = """
            POST without a request body. Applies mandatory and saved additional hours
            The response shows current hours after application
            A valid CSRF token is required, including when using Basic authentication
            """
    )
    @ApiResponse(
        responseCode = "200", description = "Teaching loads applied",
        content = {
            @Content(
                mediaType = "application/vnd.surok.load-draft.v1+json",
                schema = @Schema(type = "object"),
                examples = @ExampleObject(
                    name = "applied", value = """
                    {
                        "curriculum": {
                            "id": 61,
                            "title": "Основной план",
                            "level": "BASIC",
                            "week": "FIVE_DAYS",
                            "version": "1",
                            "year": "2026/2027",
                            "description": "Учебный план основной школы"
                        },
                        "loads": [
                            {
                                "schoolClass": {"id": 17, "name": "5А", "grade": 5, "litera": "А"},
                                "subject": {"id": 29, "name": "Математика", "origin": "FEDERAL"},
                                "hours": {"current": 3, "proposed": 3, "planned": 3},
                                "decision": "plan"
                            },
                            {
                                "schoolClass": {"id": 17, "name": "5А", "grade": 5, "litera": "А"},
                                "subject": {"id": 31, "name": "Музыка", "origin": "SCHOOL"},
                                "hours": {"current": 2, "planned": 2},
                                "decision": "manual"
                            }
                        ],
                        "additions": [
                            {
                                "grade": 5,
                                "requirements": [
                                    {"id": 73, "grade": 5, "hours": 3, "partType": "OPTIONAL"}
                                ],
                                "subjects": [
                                    {"id": 31, "name": "Музыка", "origin": "SCHOOL", "hours": 2}
                                ],
                                "classes": [
                                    {
                                        "id": 17,
                                        "name": "5А",
                                        "grade": 5,
                                        "litera": "А",
                                        "hours": {"current": 2, "planned": 2}
                                    }
                                ]
                            }
                        ],
                        "requirements": [
                            {
                                "id": 71,
                                "grade": 5,
                                "subject": {"id": 29, "name": "Математика", "origin": "FEDERAL"},
                                "hours": 3,
                                "partType": "MANDATORY"
                            },
                            {"id": 73, "grade": 5, "hours": 3, "partType": "OPTIONAL"}
                        ],
                        "unavailable": []
                    }
                    """
                )
            ),
            @Content(
                mediaType = "application/vnd.surok.load-draft.v1+xml",
                schema = @Schema(ref = "#/components/schemas/LoadDraftXml"),
                examples = @ExampleObject(
                    name = "applied", value = """
                    <draft>
                        <curriculum>
                            <id>61</id>
                            <title>Основной план</title>
                            <level>BASIC</level>
                            <week>FIVE_DAYS</week>
                            <version>1</version>
                            <year>2026/2027</year>
                            <description>Учебный план основной школы</description>
                        </curriculum>
                        <loads>
                            <load>
                                <schoolClass>
                                    <id>17</id>
                                    <name>5А</name>
                                    <grade>5</grade>
                                    <litera>А</litera>
                                </schoolClass>
                                <subject>
                                    <id>29</id>
                                    <name>Математика</name>
                                    <origin>FEDERAL</origin>
                                </subject>
                                <hours>
                                    <current>3</current>
                                    <proposed>3</proposed>
                                    <planned>3</planned>
                                </hours>
                                <decision>plan</decision>
                            </load>
                            <load>
                                <schoolClass>
                                    <id>17</id>
                                    <name>5А</name>
                                    <grade>5</grade>
                                    <litera>А</litera>
                                </schoolClass>
                                <subject>
                                    <id>31</id>
                                    <name>Музыка</name>
                                    <origin>SCHOOL</origin>
                                </subject>
                                <hours><current>2</current><planned>2</planned></hours>
                                <decision>manual</decision>
                            </load>
                        </loads>
                        <additions>
                            <addition>
                                <grade>5</grade>
                                <requirements>
                                    <requirement>
                                        <id>73</id>
                                        <grade>5</grade>
                                        <hours>3</hours>
                                        <partType>OPTIONAL</partType>
                                    </requirement>
                                </requirements>
                                <subjects>
                                    <subject>
                                        <id>31</id>
                                        <name>Музыка</name>
                                        <origin>SCHOOL</origin>
                                        <hours>2</hours>
                                    </subject>
                                </subjects>
                                <classes>
                                    <class>
                                        <id>17</id>
                                        <name>5А</name>
                                        <grade>5</grade>
                                        <litera>А</litera>
                                        <hours><current>2</current><planned>2</planned></hours>
                                    </class>
                                </classes>
                            </addition>
                        </additions>
                        <requirements>
                            <requirement>
                                <id>71</id>
                                <grade>5</grade>
                                <subject>
                                    <id>29</id>
                                    <name>Математика</name>
                                    <origin>FEDERAL</origin>
                                </subject>
                                <hours>3</hours>
                                <partType>MANDATORY</partType>
                            </requirement>
                            <requirement>
                                <id>73</id>
                                <grade>5</grade>
                                <hours>3</hours>
                                <partType>OPTIONAL</partType>
                            </requirement>
                        </requirements>
                        <unavailable></unavailable>
                    </draft>
                    """
                )
            )
        }
    )
    @PreAuthorize(
        """
        hasAnyRole('ADMIN', 'DIRECTOR', 'DEPUTY_DIRECTOR',
            'BASIC_MAKER', 'ADVANCED_MAKER', 'PRO_MAKER')
        and (hasRole('ADMIN')
            or #school == authentication.principal.info().school())
        """
    )
    public ResponseEntity<String> apply(
        @Parameter(example = "1") @PathVariable("school") final long school,
        @Parameter(example = "101") @PathVariable("schedule") final long schedule,
        final HttpServletRequest request
    ) throws Exception {
        final MediaType type = new AcHttp(request, List.of(this.types)).type();
        final LoadDraft draft = new LdPostgres(
            this.ctx, new SlsPostgres(this.ctx).school(school).schedules().schedule(schedule)
        );
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
