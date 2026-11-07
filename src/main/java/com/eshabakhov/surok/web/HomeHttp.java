/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.web;

import com.eshabakhov.surok.media.AcHttp;
import com.eshabakhov.surok.media.MdXml;
import com.eshabakhov.surok.media.MdXslt;
import com.eshabakhov.surok.media.XsClasspath;
import com.eshabakhov.surok.user.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
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

    /** New landing resource. */
    public HomeHttp() {
        // Intentionally empty.
    }

    /**
     * Render the landing page.
     *
     * @param user Authenticated user, if present
     * @param request HTTP request
     * @return Landing page
     * @checkstyle NonStaticMethodCheck (2 lines)
     */
    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> page(
        @AuthenticationPrincipal final AuthUser user,
        final HttpServletRequest request
    ) {
        final MediaType type = new AcHttp(request).type();
        final String target;
        if (user == null) {
            target = "/users/login";
        } else {
            target = "/schools";
        }
        return ResponseEntity.ok().contentType(new MediaType(type, StandardCharsets.UTF_8)).body(
            new MdXslt(
                new MdXml("surok").with("path", request.getRequestURI()),
                new XsClasspath("xslt/home.xsl", "home")
            ).with("url", target).print()
        );
    }
}
