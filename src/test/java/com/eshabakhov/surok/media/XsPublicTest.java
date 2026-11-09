/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.media;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Public XSLT pages preserve their layout, safe values and protected forms.
 *
 * @since 0.0.1
 */
final class XsPublicTest {

    @ParameterizedTest
    @ValueSource(strings = {"login", "register"})
    void printsProtectedAuthenticationForms(final String view) {
        MatcherAssert.assertThat(
            XsPublicTest.page(view),
            Matchers.allOf(
                Matchers.containsString("name=\"_csrf\" type=\"hidden\" value=\"token\""),
                Matchers.containsString("name=\"_csrf_header\" content=\"X-CSRF-TOKEN\""),
                Matchers.containsString("href=\"/css/login.css\""),
                Matchers.containsString("src=\"/js/authentication.js\" type=\"module\""),
                Matchers.containsString("eye-icon eye-on is-hidden"),
                Matchers.not(Matchers.containsString("<aside")),
                Matchers.not(Matchers.containsString("app-header")),
                Matchers.not(Matchers.containsString("th:")),
                Matchers.not(Matchers.containsString("style=\""))
            )
        );
    }

    @Test
    void printsRegistrationActionAndConfirmationTarget() {
        MatcherAssert.assertThat(
            XsPublicTest.page("register"),
            Matchers.allOf(
                Matchers.containsString("action=\"/users\""),
                Matchers.containsString("minlength=\"8\""),
                Matchers.containsString("id=\"confirm-password\""),
                Matchers.containsString("data-target=\"confirm-password\""),
                Matchers.containsString("name=\"confirmPassword\""),
                Matchers.containsString("href=\"https://surok.com/users/register\""),
                Matchers.not(Matchers.containsString("/api/users"))
            )
        );
    }

    @Test
    void keepsLoginAndOauthActions() {
        MatcherAssert.assertThat(
            XsPublicTest.page("login"),
            Matchers.allOf(
                Matchers.containsString("action=\"/login\""),
                Matchers.containsString("href=\"/oauth2/authorization/yandex\""),
                Matchers.containsString("href=\"/users/register\""),
                Matchers.containsString("href=\"https://surok.com/users/login\""),
                Matchers.not(Matchers.containsString("alert success"))
            )
        );
    }

    @ParameterizedTest
    @CsvSource(
        {
            "oauth_no_email, Яндекс не передал email",
            "bad_credentials, Неверный логин или пароль",
            "user_not_found, Пользователь не найден"
        }
    )
    void printsOnlyRequestedLoginError(final String code, final String message) {
        MatcherAssert.assertThat(
            new XsClasspath("xslt/authentication.xsl", "login").transform(
                String.format("<surok><error>%s</error></surok>", code)
            ),
            Matchers.allOf(
                Matchers.containsString(message),
                Matchers.not(Matchers.containsString("alert success"))
            )
        );
    }

    @ParameterizedTest
    @CsvSource({"logout, Вы вышли из системы", "registered, Регистрация успешна!"})
    void printsLoginStatus(final String flag, final String message) {
        MatcherAssert.assertThat(
            new XsClasspath("xslt/authentication.xsl", "login").transform(
                String.format("<surok><%1$s>true</%1$s></surok>", flag)
            ),
            Matchers.containsString(message)
        );
    }

    @Test
    void escapesRegistrationErrorsAndValues() {
        MatcherAssert.assertThat(
            new XsClasspath("xslt/authentication.xsl", "register").transform(
                """
                <surok><message>&lt;script&gt;Ошибка&lt;/script&gt;</message>
                    <username>&quot; onfocus=&quot;alert(1)</username>
                    <email>&lt;img&gt;@example.com</email>
                </surok>
                """
            ),
            Matchers.allOf(
                Matchers.containsString("&lt;script&gt;Ошибка&lt;/script&gt;"),
                Matchers.containsString("value=\"&quot; onfocus=&quot;alert(1)\""),
                Matchers.not(Matchers.containsString("<script>Ошибка")),
                Matchers.not(Matchers.containsString("value=\"\" onfocus="))
            )
        );
    }

    @Test
    void printsLandingWithSharedFooterAndDestination() {
        MatcherAssert.assertThat(
            new XsClasspath("xslt/home.xsl", "home")
                .transform("<surok><url>/users/login</url></surok>"),
            Matchers.allOf(
                Matchers.containsString("href=\"/users/login\""),
                Matchers.containsString("href=\"/css/landing.css\""),
                Matchers.containsString("href=\"/css/footer.css\""),
                Matchers.containsString("class=\"app-footer\""),
                Matchers.containsString("Отчеты"),
                Matchers.not(Matchers.containsString("<aside")),
                Matchers.not(Matchers.containsString("/css/main.css")),
                Matchers.not(Matchers.containsString("th:"))
            )
        );
    }

    private static String page(final String view) {
        return new XsClasspath("xslt/authentication.xsl", view).transform(
            """
            <surok><csrf><token>token</token><parameter>_csrf</parameter>
                <header>X-CSRF-TOKEN</header></csrf></surok>
            """
        );
    }
}
