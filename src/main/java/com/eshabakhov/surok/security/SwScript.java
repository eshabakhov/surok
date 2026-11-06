/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexPageTransformer;
import org.springdoc.webmvc.ui.SwaggerIndexTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.resource.ResourceTransformerChain;
import org.springframework.web.servlet.resource.TransformedResource;

/**
 * Adds credential verification to springdoc's generated Swagger initializer.
 * Wrap the standard transformer to retain its URL and context-path configuration.
 *
 * @since 0.0.1
 */
@Component
@ConditionalOnProperty(name = "springdoc.swagger-ui.enabled", matchIfMissing = true)
public final class SwScript implements SwaggerIndexTransformer {

    /** Standard springdoc transformation. */
    private final SwaggerIndexTransformer origin;

    /** Application Swagger plugin. */
    private final Resource script;

    /**
     * New springdoc initializer with application authorization.
     *
     * @param config Swagger configuration
     * @param oauth OAuth configuration
     * @param welcome URL resolver
     * @param mapper Object mapper provider
     * @checkstyle ParameterNumberCheck (6 lines)
     */
    @Autowired
    public SwScript(
        final SwaggerUiConfigProperties config, final SwaggerUiOAuthProperties oauth,
        final SwaggerWelcomeCommon welcome, final ObjectMapperProvider mapper
    ) {
        this(
            new SwaggerIndexPageTransformer(config, oauth, welcome, mapper),
            new ClassPathResource("static/js/swagger.js")
        );
    }

    /**
     * New initializer decorator.
     *
     * @param origin Standard transformation
     * @param script Plugin source
     */
    public SwScript(final SwaggerIndexTransformer origin, final Resource script) {
        this.origin = origin;
        this.script = script;
    }

    @Override
    public Resource transform(
        final HttpServletRequest request, final Resource resource,
        final ResourceTransformerChain chain
    ) throws IOException {
        Resource transformed = this.origin.transform(request, resource, chain);
        if ("swagger-initializer.js".equals(resource.getFilename())) {
            final String content = transformed.getContentAsString(StandardCharsets.UTF_8);
            if (!content.contains("plugins: [")) {
                throw new IOException("Swagger initializer has no plugin configuration");
            }
            transformed = new TransformedResource(
                transformed,
                String.format(
                    "%s%n%s", this.script.getContentAsString(StandardCharsets.UTF_8),
                    content.replace(
                        "plugins: [",
                        """
                        requestInterceptor: window.surokSwagger.request,
                        responseInterceptor: window.surokSwagger.response,
                        plugins: [window.surokSwagger.plugin,
                        """
                    )
                ).getBytes(StandardCharsets.UTF_8)
            );
        }
        return transformed;
    }
}
