/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;

/**
 * Application entrypoint.
 *
 * @since 0.0.1
 */
@SuppressWarnings("PMD.UncommentedEmptyConstructor")
@SpringBootApplication(exclude = ErrorMvcAutoConfiguration.class)
public class SurokApplication {

    protected SurokApplication() { }

    /**
     * Start application.
     *
     * @param args Command line arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(SurokApplication.class, args);
    }
}
