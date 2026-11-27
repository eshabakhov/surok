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
 * Profile and subscription XSLT views preserve navigation, forms and display states.
 *
 * @since 0.0.1
 */
final class XsAccountTest {

    @Test
    void printsProfileFieldsAndLogout() {
        MatcherAssert.assertThat(
            new XsClasspath("xslt/profile.xsl", "profile").transform(XsAccountTest.profile()),
            Matchers.allOf(
                Matchers.containsString("Соколова"),
                Matchers.containsString("TEACHER"),
                Matchers.containsString("href=\"/schools/3/schedules/9\""),
                Matchers.containsString("href=\"/schools/3/subjects\""),
                Matchers.containsString("action=\"/logout\""),
                Matchers.containsString("name=\"_csrf\" type=\"hidden\" value=\"token\""),
                Matchers.containsString("href=\"/css/user/profile.css\""),
                Matchers.not(Matchers.containsString("style=\"")),
                Matchers.not(Matchers.containsString("<svg:"))
            )
        );
    }

    @Test
    void omitsSchoolNavigationWhenAccountHasNoSchool() {
        MatcherAssert.assertThat(
            new XsClasspath("xslt/profile.xsl", "profile").transform(
                """
                <surok><user><username>Admin</username><admin>true</admin></user>
                    <schools><href>/schools</href></schools><account><id>1</id>
                    <username>Admin</username><initial>A</initial><roles></roles></account></surok>
                """
            ),
            Matchers.allOf(
                Matchers.containsString("href=\"/schools\""),
                Matchers.containsString("href=\"/federal/curriculums\""),
                Matchers.containsString("Нет ролей"),
                Matchers.not(Matchers.containsString("/schools//")),
                Matchers.not(Matchers.containsString("title=\"Расписания\""))
            )
        );
    }

    @Test
    void escapesProfileNames() {
        MatcherAssert.assertThat(
            new XsClasspath("xslt/profile.xsl", "profile").transform(
                XsAccountTest.profile().replace("Соколова", "&lt;script&gt;Соколова&lt;/script&gt;")
            ),
            Matchers.allOf(
                Matchers.containsString("&lt;script&gt;Соколова&lt;/script&gt;"),
                Matchers.not(Matchers.containsString("<script>Соколова"))
            )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"BASIC", "ADVANCED", "PRO", "VIEWER"})
    void printsSubscriptionPlanAndProtectedForms(final String plan) {
        MatcherAssert.assertThat(
            XsAccountTest.subscription(plan, true),
            Matchers.allOf(
                Matchers.containsString("Функционал в разработке"),
                Matchers.containsString("Доступные планы"),
                Matchers.containsString("pill-curr"),
                Matchers.containsString("action=\"/users/subscription/checkout\""),
                Matchers.containsString("name=\"_csrf\" type=\"hidden\" value=\"token\""),
                Matchers.containsString("href=\"/users/profile\""),
                Matchers.containsString("href=\"/css/user/subscription.css\""),
                Matchers.containsString("src=\"/js/user/subscription.js\""),
                Matchers.not(Matchers.containsString("style=\"")),
                Matchers.not(Matchers.containsString("th:")),
                Matchers.not(Matchers.containsString("<svg:"))
            )
        );
    }

    @Test
    void printsPerpetualBasicWithoutCancellation() {
        MatcherAssert.assertThat(
            XsAccountTest.subscription("BASIC", true),
            Matchers.allOf(
                Matchers.containsString("Бессрочно"),
                    Matchers.containsString("Бесплатно"),
                Matchers.not(Matchers.containsString("id=\"subscription-cancel\"")),
                Matchers.not(Matchers.containsString("Активна до")),
                Matchers.not(Matchers.containsString("Истекла"))
            )
        );
    }

    @ParameterizedTest
    @CsvSource({"true, Активна до", "false, Истекла"})
    void printsPaidStateAndCancellation(final boolean valid, final String state) {
        MatcherAssert.assertThat(
            XsAccountTest.subscription("PRO", valid),
            Matchers.allOf(
                Matchers.containsString(state),
                Matchers.containsString("action=\"/users/subscription/cancel\""),
                Matchers.containsString("id=\"subscription-cancel\""),
                Matchers.not(Matchers.containsString("name=\"plan\" type=\"hidden\" value=\"PRO\""))
            )
        );
    }

    private static String subscription(final String plan, final boolean valid) {
        return new XsClasspath("xslt/subscription.xsl", "subscription").transform(
            String.format(
                """
                <surok><user><username>Visitor</username><subscription>true</subscription></user>
                    <csrf><parameter>_csrf</parameter><token>token</token></csrf>
                    <subscription><plan>%s</plan><style>pro</style><label>Тариф</label>
                        <valid>%s</valid><expiry>12.04.2030</expiry></subscription></surok>
                """, plan, valid
            )
        );
    }

    private static String profile() {
        return """
            <surok><user><username>Соколова</username><subscription>true</subscription></user>
                <school id="3"><name>Школа</name><schedules><schedule>
                    <id>9</id><name>Учебный год</name></schedule></schedules></school>
                <account><id>7</id><username>Соколова</username><initial>С</initial>
                    <roles><role><name>TEACHER</name></role></roles></account>
                <csrf><parameter>_csrf</parameter><token>token</token></csrf></surok>
            """;
    }
}
