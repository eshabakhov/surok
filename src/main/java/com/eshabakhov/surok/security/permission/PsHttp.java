/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import com.eshabakhov.surok.media.AcHttp;
import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.media.MdXml;
import com.eshabakhov.surok.media.MdXslt;
import com.eshabakhov.surok.media.SrWeb;
import com.eshabakhov.surok.media.XsClasspath;
import com.eshabakhov.surok.school.SlsBrief;
import com.eshabakhov.surok.school.SlsPostgres;
import com.eshabakhov.surok.security.CsToken;
import com.eshabakhov.surok.user.AuthUser;
import com.eshabakhov.surok.user.UrBrief;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import org.jooq.DSLContext;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Administrator-only permission matrix with negotiated HTML, JSON and XML representations.
 * For example, GET /permissions with the versioned JSON Accept header prints controller rules.
 *
 * @since 0.0.1
 */
@RestController
@Tag(name = "Permissions")
public class PsHttp {

    /** Live controller permissions. */
    private final Permissions origin;

    /** Database context for shared page navigation. */
    private final DSLContext ctx;

    /**
     * New permission resource.
     *
     * @param origin Live permission catalog
     * @param ctx Database context
     */
    public PsHttp(final Permissions origin, final DSLContext ctx) {
        this.origin = origin;
        this.ctx = ctx;
    }

    /**
     * Show controller permissions for all roles.
     *
     * @param user Current administrator
     * @param request Current request
     * @return Negotiated permission matrix
     * @throws Exception If rendering fails
     */
    @GetMapping(
        value = "/permissions",
        produces = {
            MediaType.TEXT_HTML_VALUE,
            "application/vnd.surok.permissions.v1+json",
            "application/vnd.surok.permissions.v1+xml"
        }
    )
    @Operation(
        summary = "View permissions by resource and role",
        description = """
            Administrator-only matrix of registered application controllers and their authorization
            rules. Select the response media type in Swagger's response selector. Each access
            entry contains role, title and scope: all (no school restriction), school (own school
            only), or denied. Additional conditions are described in condition. Main resource
            routes are listed, excluding GET aliases for forms and fragments. Examples show an
            excerpt; the response includes all roles, including ANONYMOUS
            """
    )
    @ApiResponse(
        responseCode = "200", description = "Permission matrix",
        content = {
            @Content(
                mediaType = MediaType.TEXT_HTML_VALUE,
                schema = @Schema(type = "string"),
                examples = @ExampleObject(
                    name = "matrix", value = "<html lang=\"ru\"><body>Права доступа</body></html>"
                )
            ),
            @Content(
                mediaType = "application/vnd.surok.permissions.v1+json",
                schema = @Schema(type = "object"),
                examples = @ExampleObject(
                    name = "matrix",
                    value = """
                        {
                            "operations": [{
                                "resource": "Права доступа",
                                "action": "Просмотр прав доступа",
                                "path": "/permissions",
                                "consumes": "",
                                "condition": "",
                                "method": "GET",
                                "access": [
                                    {"role": "ADMIN", "title": "Администратор", "scope": "all"},
                                    {"role": "VIEWER", "title": "Наблюдатель", "scope": "denied"}
                                ]
                            }]
                        }
                        """
                )
            ),
            @Content(
                mediaType = "application/vnd.surok.permissions.v1+xml",
                schema = @Schema(ref = "#/components/schemas/PermissionsXml"),
                examples = @ExampleObject(
                    name = "matrix",
                    value = """
                        <permissions>
                            <operations>
                                <operation>
                                    <resource>Права доступа</resource>
                                    <action>Просмотр прав доступа</action>
                                    <path>/permissions</path>
                                    <consumes></consumes>
                                    <condition></condition>
                                    <method>GET</method>
                                    <access>
                                        <grant>
                                            <role>ADMIN</role>
                                            <title>Администратор</title>
                                            <scope>all</scope>
                                        </grant>
                                        <grant>
                                            <role>VIEWER</role>
                                            <title>Наблюдатель</title>
                                            <scope>denied</scope>
                                        </grant>
                                    </access>
                                </operation>
                            </operations>
                        </permissions>
                        """
                )
            )
        }
    )
    @ApiResponse(responseCode = "401", description = "Authentication required for API access")
    @ApiResponse(responseCode = "403", description = "Administrator role required")
    @ApiResponse(responseCode = "406", description = "Unsupported representation")
    public ResponseEntity<?> page(
        @AuthenticationPrincipal final AuthUser user, final HttpServletRequest request
    ) throws Exception {
        final MediaType type = new AcHttp(request, "permissions.v1").type();
        final Permissions permissions = new PsAuthorized(this.origin, user);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .contentType(new MediaType(type, StandardCharsets.UTF_8)).body(
                switch (type.toString()) {
                    case MediaType.TEXT_HTML_VALUE -> new SrWeb(
                        new UrBrief(user), new SlsBrief(new SlsPostgres(this.ctx)),
                        new CsToken((CsrfToken) request.getAttribute(CsrfToken.class.getName()))
                    ).print(
                        new MdXslt(
                            new MdXml("surok").with("path", request.getRequestURI()),
                            new XsClasspath("xslt/permissions.xsl", "permissions")
                        )
                    ).with("permissions", permissions).print();
                    case "application/vnd.surok.permissions.v1+json" ->
                        permissions.print(new MdJson()).print();
                    case "application/vnd.surok.permissions.v1+xml" ->
                        permissions.print(new MdXml("permissions")).print();
                    case null, default -> throw new ResponseStatusException(
                        HttpStatus.NOT_ACCEPTABLE, "Unsupported permissions representation"
                    );
                }
            );
    }
}
