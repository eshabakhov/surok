/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.error;

import java.util.Map;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Error pages and data formats, including unsupported Accept headers.
 *
 * @since 0.0.1
 */
final class ErHttpTest {

    @ParameterizedTest
    @CsvSource(
        {
            "401, Необходима авторизация", "403, Доступ запрещён",
            "404, Страница не найдена", "500, Ошибка обработки запроса"
        }
    )
    void printsErrorPage(final int status, final String title) {
        MatcherAssert.assertThat(
            new ErHttp(ErHttpTest.request("text/html"), HttpStatus.valueOf(status))
                .response().getBody(),
            Matchers.allOf(
                Matchers.containsString(String.format("<title>%d - Surok</title>", status)),
                Matchers.containsString(title),
                Matchers.containsString("href=\"/css/main.css\""),
                Matchers.containsString("href=\"/css/error.css\""),
                Matchers.containsString("class=\"error-card\""),
                Matchers.containsString("class=\"error-footer\""),
                Matchers.not(Matchers.containsString("th:"))
            )
        );
    }

    @Test
    void pointsToTheExistingLoginPage() {
        MatcherAssert.assertThat(
            new ErHttp(ErHttpTest.request("text/html"), HttpStatus.UNAUTHORIZED)
                .response().getBody(),
            Matchers.containsString("href=\"/users/login\"")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/json", "application/vnd.surok.building-v1+json"})
    void printsJsonEnvelope(final String accept) {
        final ResponseEntity<String> response = new ErHttp(
            ErHttpTest.request(accept), HttpStatus.NOT_FOUND
        ).response();
        MatcherAssert.assertThat(
            Map.of(
                "type", response.getHeaders().getContentType().toString(),
                "body", response.getBody()
            ),
            Matchers.allOf(
                Matchers.hasEntry("type", "application/json;charset=UTF-8"),
                Matchers.hasEntry(Matchers.is("body"), Matchers.containsString("\"status\":404")),
                Matchers.hasEntry(Matchers.is("body"), Matchers.containsString("\"message\":")),
                Matchers.hasEntry(Matchers.is("body"), Matchers.containsString("\"timestamp\":"))
            )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/xml", "application/vnd.surok.cabinet-v1+xml"})
    void printsXmlEnvelope(final String accept) {
        MatcherAssert.assertThat(
            new ErHttp(ErHttpTest.request(accept), HttpStatus.FORBIDDEN).response().getBody(),
            Matchers.allOf(
                Matchers.startsWith("<error><status>403</status>"),
                Matchers.containsString("<message>"),
                Matchers.containsString("<timestamp>"),
                Matchers.not(Matchers.containsString("<html"))
            )
        );
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "application/json;q=0.9,text/html;q=0.1",
            "text/html;q=0,*/*;q=0.8"
        }
    )
    void honorsQualityAndExplicitExclusions(final String accept) {
        MatcherAssert.assertThat(
            new ErHttp(ErHttpTest.request(accept), HttpStatus.CONFLICT)
                .response().getHeaders().getContentType().toString(),
            Matchers.is("application/json;charset=UTF-8")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "image/png", "*/*;q=0"})
    void keepsStatusWithoutAnUnacceptableBody(final String accept) {
        MatcherAssert.assertThat(
            new ErHttp(ErHttpTest.request(accept), HttpStatus.NOT_ACCEPTABLE).response(),
            Matchers.allOf(
                Matchers.hasProperty("statusCode", Matchers.is(HttpStatus.NOT_ACCEPTABLE)),
                Matchers.hasProperty("body", Matchers.nullValue())
            )
        );
    }

    @Test
    void escapesPublicErrorMessage() {
        MatcherAssert.assertThat(
            new ErHttp(
                ErHttpTest.request("text/html"), HttpStatus.BAD_REQUEST, "<script>alert(1)</script>"
            ).response().getBody(),
            Matchers.allOf(
                Matchers.containsString("&lt;script&gt;alert(1)&lt;/script&gt;"),
                Matchers.not(Matchers.containsString("<script>alert(1)</script>"))
            )
        );
    }

    private static MockHttpServletRequest request(final String accept) {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.ACCEPT, accept);
        return request;
    }
}
