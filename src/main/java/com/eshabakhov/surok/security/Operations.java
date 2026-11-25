/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security;

import java.util.stream.StreamSupport;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Operations available in an API catalog.
 * For example, resolve Buildings.create independently of its controller and URL.
 *
 * @since 0.0.1
 */
@FunctionalInterface
public interface Operations {

    /**
     * Enumerate described operations.
     *
     * @return Operations in declaration order
     */
    Iterable<Operation> iterate();

    /**
     * Find an operation by its identifier.
     *
     * @param identifier Declared identifier
     * @return Described operation
     */
    default Operation operation(final String identifier) {
        return StreamSupport.stream(this.iterate().spliterator(), false)
            .filter(operation -> identifier.equals(operation.identifier())).findFirst().orElseThrow(
                () -> new IllegalArgumentException(
                    String.format("Unknown API operation: %s", identifier)
                )
            );
    }

    /**
     * Find an operation by its HTTP binding.
     *
     * @param path Path template
     * @param method HTTP method
     * @return Described operation
     */
    default Operation operation(final String path, final RequestMethod method) {
        return StreamSupport.stream(this.iterate().spliterator(), false).filter(
            operation -> path.equals(operation.path()) && method == operation.method()
        ).findFirst().orElseThrow(
            () -> new IllegalArgumentException(
                String.format("Unknown API operation: %s %s", method, path)
            )
        );
    }
}
