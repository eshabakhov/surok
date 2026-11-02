/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum.requirement.input;

import com.eshabakhov.surok.ResultPage;
import java.util.Collections;
import java.util.Map;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link MdRqInput}.
 *
 * @since 0.0.1
 */
final class MdRqInputTest {

    @Test
    void printsEmptyFields() {
        MatcherAssert.assertThat(
            "A new medium must contain no fields",
            new MdRqInput().print(),
            Matchers.is(Collections.emptyMap())
        );
    }

    @Test
    void returnsNewMediumForStringField() {
        final MdRqInput origin = new MdRqInput();
        MatcherAssert.assertThat(
            "A string field must produce a new input medium",
            origin.with("text", "value"),
            Matchers.not(Matchers.sameInstance(origin))
        );
    }

    @Test
    void printsStringField() {
        MatcherAssert.assertThat(
            "Input medium must retain a string field",
            new MdRqInput().with("text", "value").print(),
            Matchers.is(Map.of("text", "value"))
        );
    }

    @Test
    void printsLongField() {
        MatcherAssert.assertThat(
            "Input medium must retain a long field",
            new MdRqInput().with("long", Long.MIN_VALUE).print(),
            Matchers.is(Map.of("long", Long.toString(Long.MIN_VALUE)))
        );
    }

    @Test
    void printsIntegerField() {
        MatcherAssert.assertThat(
            "Input medium must retain an integer field",
            new MdRqInput().with("integer", Integer.MAX_VALUE).print(),
            Matchers.is(
                Map.of("integer", Integer.toString(Integer.MAX_VALUE))
            )
        );
    }

    @Test
    void printsBooleanField() {
        MatcherAssert.assertThat(
            "Input medium must retain a boolean field",
            new MdRqInput().with("boolean", true).print(),
            Matchers.is(Map.of("boolean", "true"))
        );
    }

    @Test
    void keepsNullString() {
        MatcherAssert.assertThat(
            "Null string must be retained",
            new MdRqInput().with("value", (String) null).print(),
            Matchers.is(Collections.singletonMap("value", null))
        );
    }

    @Test
    void keepsNullLong() {
        MatcherAssert.assertThat(
            "Null long must be retained",
            new MdRqInput().with("value", (Long) null).print(),
            Matchers.is(Collections.singletonMap("value", null))
        );
    }

    @Test
    void keepsNullInteger() {
        MatcherAssert.assertThat(
            "Null integer must be retained",
            new MdRqInput().with("value", (Integer) null).print(),
            Matchers.is(Collections.singletonMap("value", null))
        );
    }

    @Test
    void keepsNullBoolean() {
        MatcherAssert.assertThat(
            "Null boolean must be retained",
            new MdRqInput().with("value", (Boolean) null).print(),
            Matchers.is(Collections.singletonMap("value", null))
        );
    }

    @Test
    void printsNamedPrintable() throws Exception {
        MatcherAssert.assertThat(
            "Named printable must write into the current level",
            new MdRqInput().with(
                "ignored",
                new RqInputSubject(new RqInput(), "504")
            ).print(),
            Matchers.is(Map.of("subjectId", "504"))
        );
    }

    @Test
    void printsUnnamedPrintable() throws Exception {
        MatcherAssert.assertThat(
            "Unnamed printable must write into the current level",
            new MdRqInput().with(
                new RqInputSubject(new RqInput(), "504")
            ).print(),
            Matchers.is(Map.of("subjectId", "504"))
        );
    }

    @Test
    void ignoresPagination() {
        final MdRqInput media = new MdRqInput();
        MatcherAssert.assertThat(
            "Pagination must not affect an input medium",
            media.with("page", (ResultPage) null),
            Matchers.sameInstance(media)
        );
    }

    @Test
    void ignoresAttributes() {
        final MdRqInput media = new MdRqInput();
        MatcherAssert.assertThat(
            "Attributes must not affect an input medium",
            media.attributes(Collections.emptyMap()),
            Matchers.sameInstance(media)
        );
    }

    @Test
    void ignoresTitle() {
        final MdRqInput media = new MdRqInput();
        MatcherAssert.assertThat(
            "A title must not affect an input medium",
            media.title(null),
            Matchers.sameInstance(media)
        );
    }

    @Test
    void ignoresIncludedFields() {
        final MdRqInput media = new MdRqInput();
        MatcherAssert.assertThat(
            "Included fields must not affect an input medium",
            media.include(),
            Matchers.sameInstance(media)
        );
    }

    @Test
    void exposesUnmodifiableFields() {
        final Map<String, String> fields = new MdRqInput()
            .with("subjectId", "504").print();
        Assertions.assertThrows(
            UnsupportedOperationException.class,
            () -> fields.put("grade", "5")
        );
    }
}
