/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * A flat operation index over HTTP paths and internal declarations.
 * For example, domain authorization resolves either kind by its unique operationId.
 *
 * @since 0.0.1
 */
public final class OpsCatalog implements Operations {

    /** HTTP paths. */
    private final Paths paths;

    /** Internal operations declared outside the HTTP paths. */
    private final Operations extensions;

    /**
     * New operation index.
     *
     * @param paths HTTP path collection
     * @param extensions Internal declarations
     */
    public OpsCatalog(final Paths paths, final Operations extensions) {
        this.paths = paths;
        this.extensions = extensions;
    }

    @Override
    public Iterable<Operation> iterate() {
        final Map<String, Operation> operations = new LinkedHashMap<>();
        for (final Operation operation : Stream.concat(
            StreamSupport.stream(this.paths.iterate().spliterator(), false).flatMap(
                path -> StreamSupport.stream(path.operations().iterate().spliterator(), false)
            ),
            StreamSupport.stream(this.extensions.iterate().spliterator(), false)
        ).toList()) {
            if (operations.putIfAbsent(operation.identifier(), operation) != null) {
                throw new IllegalArgumentException(
                    String.format("Duplicate API operation: %s", operation.identifier())
                );
            }
        }
        return List.copyOf(operations.values());
    }

    @Override
    public Operation operation(final String path, final RequestMethod method) {
        return this.paths.path(path).operation(method);
    }
}
