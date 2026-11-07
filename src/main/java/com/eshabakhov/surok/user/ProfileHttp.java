/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.PrItems;
import com.eshabakhov.surok.media.AcHttp;
import com.eshabakhov.surok.media.MdXml;
import com.eshabakhov.surok.media.MdXslt;
import com.eshabakhov.surok.media.SrWeb;
import com.eshabakhov.surok.media.XsClasspath;
import com.eshabakhov.surok.school.SlsBrief;
import com.eshabakhov.surok.school.SlsPostgres;
import com.eshabakhov.surok.security.CsToken;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.stream.StreamSupport;
import org.jooq.DSLContext;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Current account profile rendered with the shared school navigation.
 *
 * @since 0.0.1
 */
@RestController
public final class ProfileHttp {

    /** Database context. */
    private final DSLContext ctx;

    /**
     * New profile resource.
     *
     * @param ctx Database context
     */
    public ProfileHttp(final DSLContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Render the current user's profile and school context.
     *
     * @param user Current user
     * @param request HTTP request
     * @return Profile page
     * @throws Exception If printing fails
     */
    @GetMapping(value = "/users/profile", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> page(
        @AuthenticationPrincipal final AuthUser user,
        final HttpServletRequest request
    ) throws Exception {
        return ResponseEntity.ok().contentType(
            new MediaType(new AcHttp(request).type(), StandardCharsets.UTF_8)
        ).body(
            new SrWeb(
                new UrBrief(user), new SlsBrief(new SlsPostgres(this.ctx)),
                new CsToken((CsrfToken) request.getAttribute(CsrfToken.class.getName()))
            ).print(
                new MdXslt(
                    new MdXml("surok").with("path", request.getRequestURI()),
                    new XsClasspath("xslt/profile.xsl", "profile")
                )
            ).with("account", new UrProfile(user)).with(
                "memberships", new PrItems(
                    "membership", StreamSupport.stream(
                        user.schools().iterate().spliterator(), false
                    ).map(school -> new UrMembership(school, user)).toList()
                )
            ).print()
        );
    }
}
