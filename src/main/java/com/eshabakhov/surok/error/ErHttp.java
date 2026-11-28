/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.error;

import com.eshabakhov.surok.media.AcHttp;
import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.media.MdXml;
import com.eshabakhov.surok.media.XsClasspath;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

/**
 * An HTTP error, rendered as a page or a machine-readable error envelope.
 *
 * <p>Vendor JSON and XML requests receive the same error envelope as ordinary
 * JSON and XML requests. Successful resource representations remain unchanged.</p>
 *
 * @since 0.0.1
 */
public final class ErHttp {

    /** Incoming request. */
    private final HttpServletRequest request;

    /** Error status. */
    private final HttpStatus status;

    /** Public error message. */
    private final String message;

    /**
     * New error with a safe default message.
     *
     * @param request Incoming request
     * @param status Error status
     */
    public ErHttp(final HttpServletRequest request, final HttpStatus status) {
        this(request, status, ErHttp.description(status));
    }

    /**
     * New error with a public message.
     *
     * @param request Incoming request
     * @param status Error status
     * @param message Public error message
     */
    public ErHttp(
        final HttpServletRequest request, final HttpStatus status, final String message
    ) {
        this.request = request;
        this.status = status;
        this.message = message;
    }

    /**
     * Print the error without replacing its status when Accept is unusable.
     *
     * @return Error response, with no body when no format is acceptable
     */
    public ResponseEntity<String> response() {
        ResponseEntity<String> result;
        try {
            result = this.print(this.type());
        } catch (final InvalidMediaTypeException | ResponseStatusException exception) {
            result = ResponseEntity.status(this.status).varyBy(HttpHeaders.ACCEPT).build();
        }
        return result;
    }

    private MediaType type() {
        final List<MediaType> supported = new ArrayList<>(
            List.of(MediaType.TEXT_HTML, MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML)
        );
        MediaType.parseMediaTypes(
            this.request.getHeader(HttpHeaders.ACCEPT)
        ).stream().filter(
            type -> "application".equals(type.getType()) && !type.isWildcardSubtype()
        ).filter(
            type -> type.getSubtype().endsWith("+json") || type.getSubtype().endsWith("+xml")
        ).map(MediaType::removeQualityValue).forEach(supported::add);
        return new AcHttp(this.request, supported).type();
    }

    private ResponseEntity<String> print(final MediaType selected) {
        final SimpleError error = new SimpleError(this.message);
        final String body;
        final MediaType type;
        if (MediaType.TEXT_HTML.isCompatibleWith(selected)) {
            type = MediaType.TEXT_HTML;
            body = new XsClasspath("xslt/error.xsl", "error").transform(
                error.print(
                    new MdXml("surok").attribute("path", this.request.getRequestURI())
                        .with("login", this.status == HttpStatus.UNAUTHORIZED)
                ).print()
            );
        } else if (selected.getSubtype().endsWith("json")) {
            type = MediaType.APPLICATION_JSON;
            body = error.print(new MdJson()).print().toString();
        } else {
            type = MediaType.APPLICATION_XML;
            body = error.print(new MdXml("error")).print();
        }
        return ResponseEntity.status(this.status)
            .contentType(new MediaType(type, StandardCharsets.UTF_8))
            .varyBy(HttpHeaders.ACCEPT).body(body);
    }

    private static String description(final HttpStatus status) {
        return switch (status) {
            case UNAUTHORIZED -> "Для доступа к этой странице нужно войти в систему.";
            case FORBIDDEN -> "У вас нет прав для просмотра этой страницы.";
            case NOT_FOUND -> "Такой страницы не существует или она была удалена.";
            default -> "Не удалось выполнить запрос. Попробуйте ещё раз позднее.";
        };
    }
}
