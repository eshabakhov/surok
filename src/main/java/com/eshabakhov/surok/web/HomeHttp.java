/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.web;

import com.eshabakhov.surok.User;
import com.eshabakhov.surok.media.AcHttp;
import com.eshabakhov.surok.media.MdXml;
import com.eshabakhov.surok.media.MdXslt;
import com.eshabakhov.surok.media.XsClasspath;
import com.eshabakhov.surok.security.OaYaml;
import com.eshabakhov.surok.security.Operations;
import com.eshabakhov.surok.user.Roles;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public landing resource with a destination appropriate for the current visitor.
 *
 * @since 0.0.1
 */
@RestController
public final class HomeHttp {

    /** Shared operation declarations. */
    private final Operations operations;

    /**
     * New resource with explicitly constructed YAML operations.
     *
     * @throws IOException If the API description cannot be read
     */
    public HomeHttp() throws IOException {
        this(new OaYaml().operations());
    }

    /**
     * New landing resource.
     *
     * @param operations Shared operation declarations
     */
    public HomeHttp(final Operations operations) {
        this.operations = operations;
    }

    /**
     * Render the landing page.
     *
     * @param user Current visitor
     * @param request HTTP request
     * @return Landing page
     */
    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> page(
        @AuthenticationPrincipal final User user,
        final HttpServletRequest request
    ) {
        final MediaType type = new AcHttp(request).type();
        final String target;
        if (user.roles().contains(Roles.RoleEnum.ANONYMOUS)) {
            target = "/users/login";
        } else {
            target = "/schools";
        }
        return ResponseEntity.ok().contentType(new MediaType(type, StandardCharsets.UTF_8)).body(
            this.operations.operation("Home.read").permission(user).permit(
                new MdXslt(
                    new MdXml("surok").with("path", request.getRequestURI()),
                    new XsClasspath("xslt/home.xsl", "home")
                )
            ).with("url", target).print()
        );
    }
}
