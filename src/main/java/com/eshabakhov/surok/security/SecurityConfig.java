/*
 * В© 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.XML;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Spring security config.
 * Includes OpenAPI security schemes and XML response metadata.
 *
 * @since 0.0.1
 * @checkstyle LambdaBodyLengthCheck (1000 lines)
 * @checkstyle ClassFanOutComplexityCheck (6 lines)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** Permissions policy. */
    private static final String PERMISSIONS = String.join(
        ", ",
        "accelerometer=()",
        "camera=()",
        "geolocation=()",
        "gyroscope=()",
        "microphone=()",
        "payment=()",
        "usb=()"
    );

    /** User details service. */
    private final DatabaseUserDetailsService userdetails;

    /** Login success handler. */
    private final LoginSuccessHandler success;

    /** Login failure handler. */
    private final LoginFailureHandler failure;

    /** OAuth2 login success handler. */
    private final OAuthSuccessHandler oauthhandler;

    /** OAuth2 user service that resolves Yandex principal to AuthUser. */
    private final YandexOAuth2UserService oauthservice;

    /** Whether API docs are enabled. */
    @Value("${app.security.docs.enabled:false}")
    private boolean docs;

    /** Whether prometheus endpoint is enabled. */
    @Value("${app.security.prometheus.enabled:false}")
    private boolean prom;

    /** Allowed CORS origins. */
    @Value("${app.security.allowed-origins:}")
    private String origins;

    /**
     * New security configuration.
     *
     * @param userdetails User details service
     * @param success Login success handler
     * @param failure Login failure handler
     * @param oauthhandler OAuth success handler
     * @param oauthservice OAuth user service
     * @since 0.0.1
     */
    public SecurityConfig(
        final DatabaseUserDetailsService userdetails,
        final LoginSuccessHandler success,
        final LoginFailureHandler failure,
        final OAuthSuccessHandler oauthhandler,
        final YandexOAuth2UserService oauthservice
    ) {
        this.userdetails = userdetails;
        this.success = success;
        this.failure = failure;
        this.oauthhandler = oauthhandler;
        this.oauthservice = oauthservice;
    }

    /**
     * Security filter chain configuration.
     *
     * @param http HttpSecurity to configure
     * @param encoder Password encoder
     * @return Configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    @Order(1)
    SecurityFilterChain apiSecurity(
        final HttpSecurity http,
        final PasswordEncoder encoder
    ) throws Exception {
        this.authentication(http, encoder);
        return http
            .securityMatcher(PathPatternRequestMatcher.withDefaults().matcher("/api/**"))
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated()).exceptionHandling(
                exceptions -> exceptions.authenticationEntryPoint(new ScError())
                    .accessDeniedHandler(new ScError())
            )
            .httpBasic(basic -> basic.authenticationEntryPoint(new ScError()))
            .build();
    }

    /**
     * Security filter chain configuration.
     *
     * @param http HttpSecurity to configure
     * @param encoder Password encoder
     * @return Configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    @Order(2)
    SecurityFilterChain webSecurity(
        final HttpSecurity http,
        final PasswordEncoder encoder
    ) throws Exception {
        this.authentication(http, encoder);
        final MediaTypeRequestMatcher wildcard = new MediaTypeRequestMatcher(MediaType.ALL);
        wildcard.setUseEquals(true);
        final MediaTypeRequestMatcher data = new MediaTypeRequestMatcher(
            MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML,
            MediaType.parseMediaType("application/*+json"),
            MediaType.parseMediaType("application/*+xml")
        );
        data.setIgnoredMediaTypes(Set.of(MediaType.ALL));
        return http
            .httpBasic(basic -> basic.authenticationEntryPoint(new ScError()))
            .cors(Customizer.withDefaults()).csrf(
                csrf -> csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            ).authorizeHttpRequests(
                auth -> {
                    auth.requestMatchers(
                        "/users/login",
                        "/users/register",
                        "/favicon.svg",
                        "/css/**",
                        "/js/**",
                        "/"
                    ).permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll();
                    if (this.docs) {
                        auth.requestMatchers(
                            "/api-docs/**",
                            "/swagger-ui.html",
                            "/swagger-ui/**"
                        ).hasRole("ADMIN");
                    } else {
                        auth.requestMatchers(
                            "/api-docs/**",
                            "/swagger-ui.html",
                            "/swagger-ui/**"
                        ).denyAll();
                    }
                    if (this.prom) {
                        auth.requestMatchers("/actuator/prometheus").permitAll();
                    } else {
                        auth.requestMatchers("/actuator/prometheus").denyAll();
                    }
                    auth.anyRequest().authenticated();
                }
            ).headers(
                headers -> headers.referrerPolicy(
                    policy -> policy.policy(
                        ReferrerPolicyHeaderWriter.ReferrerPolicy
                            .STRICT_ORIGIN_WHEN_CROSS_ORIGIN
                        )
                    ).frameOptions(frame -> frame.deny()).httpStrictTransportSecurity(
                        hsts -> hsts
                            .includeSubDomains(true)
                            .preload(true)
                            .maxAgeInSeconds(31_536_000L)
                    ).permissionsPolicyHeader(
                        permissions -> permissions.policy(SecurityConfig.PERMISSIONS)
                    )
            ).formLogin(
                form -> form
                    .loginPage("/users/login")
                    .loginProcessingUrl("/login")
                    .successHandler(this.success)
                    .failureHandler(this.failure)
                    .permitAll()
            ).oauth2Login(
                oauth -> oauth
                    .loginPage("/users/login").userInfoEndpoint(
                        endpoint -> endpoint.userService(this.oauthservice)
                    )
                    .successHandler(this.oauthhandler)
            ).logout(
                logout -> logout
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/users/login?logout")
            ).exceptionHandling(
                exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                    new ScError(), data
                ).defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint("/users/login"), wildcard
                ).accessDeniedHandler(new ScError())
            )
            .build();
    }

    /**
     * CORS configuration source.
     *
     * @return Configured CorsConfigurationSource
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        final CorsConfiguration config = new CorsConfiguration();
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(
            List.of("Authorization", "Content-Type", "X-CSRF-TOKEN", "version")
        );
        final List<String> allowed = Arrays.stream(this.origins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isBlank())
            .toList();
        if (!allowed.isEmpty()) {
            config.setAllowedOrigins(allowed);
        }
        config.setAllowCredentials(!allowed.isEmpty());
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * PasswordEncoder.
     *
     * @return PasswordEncoder
     * @checkstyle NonStaticMethodCheck (2 lines)
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * OpenAPI definition with basic authentication support.
     *
     * @param build Maven build information
     * @return OpenAPI definition
     * @checkstyle NonStaticMethodCheck (2 lines)
     */
    @Bean
    OpenAPI openApi(final BuildProperties build) {
        return new OpenAPI().info(
            new Info()
                .title("Surok API")
                .version(build.getVersion())
            ).components(
                new Components().addSchemas(
                    "LoadDraftXml", new ObjectSchema().xml(new XML().name("draft"))
                ).addSecuritySchemes(
                    "Basic auth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("basic")
                    )
            )
            .addSecurityItem(new SecurityRequirement().addList("Basic auth"));
    }

    private void authentication(final HttpSecurity http, final PasswordEncoder encoder) {
        final DaoAuthenticationProvider provider = new DaoAuthenticationProvider(this.userdetails);
        provider.setPasswordEncoder(encoder);
        provider.setHideUserNotFoundExceptions(false);
        http.getSharedObject(AuthenticationManagerBuilder.class)
            .parentAuthenticationManager(new ProviderManager(provider));
    }
}
