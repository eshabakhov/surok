/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject;

import com.eshabakhov.surok.media.MdXml;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Subject decoration normalizes names while preserving subject identity and ownership.
 * For example, a federal subject remains federal after its name is normalized.
 *
 * @since 0.0.1
 */
final class SbNormalizedTest {

    @ParameterizedTest
    @ValueSource(
        strings = {
            "Иностранный язык", "  ИНОСТРАННЫЙ   ЯЗЫК  ",
            "\tИностранный\t\tязык\t", " Иностранный  язык "
        }
    )
    void normalizesTheDecoratedSubjectsName(final String name) throws Exception {
        MatcherAssert.assertThat(
            "The subject decorator retained differences in case or whitespace",
            new SbNormalized(new SbFake(29L, name)).name(),
            Matchers.is("иностранный язык")
        );
    }

    @Test
    void preservesTheIdentityAndOrigin() throws Exception {
        final Subject subject = new SbNormalized(
            new SbFake(29L, " МАТЕМАТИКА ", Subject.Origin.FEDERAL)
        );
        MatcherAssert.assertThat(
            "Normalizing the subject changed its identity or catalogue",
            List.of(subject.uid(), subject.origin()),
            Matchers.contains(29L, Subject.Origin.FEDERAL)
        );
    }

    @Test
    void leavesTheOriginalSubjectUnchanged() throws Exception {
        final Subject subject = new SbFake(29L, " МАТЕМАТИКА ");
        MatcherAssert.assertThat(
            "Normalizing the subject changed its original display name",
            List.of(new SbNormalized(subject).name(), subject.name()),
            Matchers.contains("математика", " МАТЕМАТИКА ")
        );
    }

    @Test
    void printsTheCanonicalNameAlongsideTheOriginalIdentity() throws Exception {
        MatcherAssert.assertThat(
            "The normalized representation lost the subject identity or canonical name",
            new SbNormalized(new SbFake(29L, "  МАТЕМАТИКА  "))
                .print(new MdXml("subject")).print(),
            Matchers.is("<subject><id>29</id><name>математика</name></subject>")
        );
    }

    @ParameterizedTest
    @CsvSource({"История России, История", "Ёлка, Елка"})
    void distinguishesDifferentNames(final String first, final String second) throws Exception {
        MatcherAssert.assertThat(
            "The decorator merged different subject names",
            new SbNormalized(new SbFake(29L, first)).name(),
            Matchers.not(new SbNormalized(new SbFake(31L, second)).name())
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "\t\f", "   "})
    void rejectsBlankNamesWhenEvaluated(final String name) {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new SbNormalized(new SbFake(29L, name)).name(),
            "The decorator accepted an empty subject name"
        );
    }
}
