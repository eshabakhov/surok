/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok;

import com.eshabakhov.surok.federal.curriculum.input.CrInput;
import com.eshabakhov.surok.federal.curriculum.input.CrInputYear;
import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.media.MdXml;
import com.eshabakhov.surok.media.MdXslt;
import com.eshabakhov.surok.media.XsClasspath;
import com.eshabakhov.surok.page.PageRequest;
import com.eshabakhov.surok.page.ResponsePage;
import com.eshabakhov.surok.school.SlsFake;
import com.eshabakhov.surok.school.SlsSelected;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.StringReader;
import java.util.List;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * Tests media printing and the common metadata contract.
 * For example, a school identifier is numeric in JSON and an attribute in XML.
 *
 * @since 0.0.1
 */
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
final class MediaTest {

    /**
     * Parses the serialized document to verify character and whitespace round trips.
     *
     * @throws Exception If the emitted XML is malformed
     */
    @Test
    void preservesUnicodeAndMarkupInAttributesAndText() throws Exception {
        final String value = String.format("Lycée «Север» 🏫 & <\"'>%c%c%c", '\t', '\n', '\r');
        final Element root = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
            new InputSource(
                new StringReader(
                    new MdXml("building").attribute("label", value).with("name", value).print()
                )
            )
        ).getDocumentElement();
        MatcherAssert.assertThat(
            "XML parsing changed the attribute or the text",
            List.of(root.getAttribute("label"), root.getTextContent()),
            Matchers.contains(value, value)
        );
    }

    /** Invalid XML characters cannot appear even as numeric references. */
    @Test
    void rejectsForbiddenXmlCharacters() {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new MdXml("building").with("name", Character.toString(0)),
            "An invalid XML character reached the document"
        );
    }

    @Test
    void keepsJsonMediaImmutable() {
        final Media<ObjectNode> initial = new MdJson();
        final Media<ObjectNode> changed = initial.with("id", 1L);
        Assertions.assertFalse(initial.print().has("id"));
        Assertions.assertTrue(changed.print().has("id"));
    }

    @Test
    void printsSelectedJsonFields() {
        final ObjectNode printed = new MdJson().include("id")
            .with("id", 1L)
            .with("description", "Hidden")
            .print();
        Assertions.assertTrue(printed.has("id"));
        Assertions.assertFalse(printed.has("description"));
    }

    @Test
    void printsSelectedXmlFields() throws Exception {
        final String printed = new MdXml("curriculum").include("id")
            .with("id", 1L)
            .with("description", "Hidden")
            .print();
        Assertions.assertTrue(printed.startsWith("<curriculum>"));
        Assertions.assertFalse(printed.contains("response"));
        Assertions.assertTrue(printed.contains("<id>1</id>"));
        Assertions.assertFalse(printed.contains("description"));
    }

    @Test
    void printsCollectionItemsUnderInterfaceName() throws Exception {
        Assertions.assertEquals(
            "<curriculums><curriculum><year>2026/2027</year></curriculum></curriculums>",
            new MdXml("curriculums").with(
                new PrItems(
                    "curriculum", List.of(new CrInputYear(new CrInput(), "2026/2027"))
                )
            ).print()
        );
    }

    @Test
    void transformsXmlIntoHtml() {
        final String printed = new MdXslt(
            new MdXml("surok"),
            new XsClasspath("xslt/federal-curriculums.xsl", "list")
        ).print();
        Assertions.assertTrue(
            printed.contains("id=\"federal-curriculums-results\"")
        );
        Assertions.assertTrue(printed.contains("data-sort=\"title\""));
        Assertions.assertTrue(printed.contains("/js/federal/curriculums.js"));
    }

    @Test
    void transformsCurriculumXmlIntoHtml() {
        final String printed = new XsClasspath(
            "xslt/federal-curriculum.xsl", "details"
        ).transform(
            """
            <surok><curriculums><curriculum id="1"
            ><title>Plan</title></curriculum></curriculums></surok>
            """
        );
        Assertions.assertTrue(printed.contains("class=\"school-info\""));
        Assertions.assertTrue(printed.contains("/js/federal/curriculum.js"));
    }

    @Test
    void transformsRequirementsXmlIntoHtml() {
        final String printed = new XsClasspath(
            "xslt/federal-curriculum-requirements.xsl", "list"
        ).transform(
            """
            <surok><curriculums><curriculum id="1"
            ><title>Plan</title><requirements/></curriculum>
            </curriculums></surok>
            """
        );
        Assertions.assertTrue(
            printed.contains("id=\"requirements-results\"")
        );
        Assertions.assertTrue(
            printed.contains("/js/federal/curriculum-requirements.js")
        );
    }

    /**
     * Preserves scalar types and the immutable source.
     *
     * @param media Output format
     */
    @ParameterizedTest
    @MethodSource("media")
    void printsTypedMetadataWithoutChangingTheSource(final Media<?> media) {
        MatcherAssert.assertThat(
            "Metadata lost its types, content or the original representation",
            List.of(
                media.attribute("id", 12L).attribute("limit", 15)
                    .attribute("active", true).attribute("kind", "school")
                    .with("name", "Лицей").print().toString(),
                media.print().toString()
            ),
            Matchers.anyOf(
                Matchers.contains(
                    "{\"id\":12,\"limit\":15,\"active\":true,\"kind\":\"school\",\"name\":\"Лицей\"}",
                    "{}"
                ),
                Matchers.contains(
                    String.join(
                        "", "<schools id=\"12\" limit=\"15\" active=\"true\" kind=\"school\">",
                        "<name>Лицей</name></schools>"
                    ),
                    "<schools></schools>"
                )
            )
        );
    }

    /**
     * Applies field selection before and after metadata is collected.
     *
     * @param media Output format
     */
    @ParameterizedTest
    @MethodSource("media")
    void includesAttributesAlongsideElements(final Media<?> media) {
        MatcherAssert.assertThat(
            "Field selection ignored attribute names or changed the selected value",
            List.of(
                media.attribute("id", 12L).with("name", "Hidden").include("id")
                    .attribute("hidden", false).print().toString(),
                media.include("id").attribute("id", 12L).with("name", "Hidden")
                    .attribute("hidden", false).print().toString()
            ),
            Matchers.everyItem(
                Matchers.in(List.of("{\"id\":12}", "<schools id=\"12\"></schools>"))
            )
        );
    }

    /**
     * Escapes markup, quotes and attribute whitespace without losing their values.
     *
     * @param media Output format
     */
    @ParameterizedTest
    @MethodSource("media")
    void escapesAttributeValues(final Media<?> media) {
        MatcherAssert.assertThat(
            "Attribute content was interpreted as markup or normalized whitespace",
            media.attribute(
                "label", String.format("Лицей & <\"Юг\">%c%c%c", '\t', '\n', '\r')
            ).print().toString(),
            Matchers.in(
                List.of(
                    "{\"label\":\"Лицей & <\\\"Юг\\\">\\t\\n\\r\"}",
                    "<schools label=\"Лицей &amp; &lt;&quot;Юг&quot;&gt;&#9;&#10;&#13;\"></schools>"
                )
            )
        );
    }

    /**
     * Rejects an element that would overwrite metadata.
     *
     * @param media Output format
     */
    @ParameterizedTest
    @MethodSource("media")
    void rejectsAnElementWithAnAttributeName(final Media<?> media) {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> media.attribute("id", 12L).with("id", 31L),
            "An element overwrote an attribute with the same name"
        );
    }

    /**
     * Rejects metadata that would overwrite an element.
     *
     * @param media Output format
     */
    @ParameterizedTest
    @MethodSource("media")
    void rejectsAnAttributeWithAnElementName(final Media<?> media) {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> media.with("id", 12L).attribute("id", 31L),
            "An attribute overwrote an element with the same name"
        );
    }

    /**
     * Composes empty collection contents and all pagination properties at one level.
     *
     * @param media Output format
     * @throws Exception If printing fails
     */
    @ParameterizedTest
    @MethodSource("media")
    void printsPaginationOnAnEmptyCollection(final Media<?> media) throws Exception {
        MatcherAssert.assertThat(
            "The empty collection lost pagination or acquired a page wrapper",
            new SlsSelected(
                new SlsFake(List.of()), List.of(), new ResponsePage(new PageRequest(15, 1), 0)
            ).print(media).print().toString(),
            Matchers.in(
                List.of(
                    String.join(
                        "", "{\"items\":[],\"limit\":15,\"offset\":1,\"total\":0,\"totalPages\":0,",
                        "\"hasNext\":false,\"hasPrev\":false}"
                    ),
                    String.join(
                        "", "<schools limit=\"15\" offset=\"1\" total=\"0\" totalPages=\"0\" ",
                        "hasNext=\"false\" hasPrev=\"false\"></schools>"
                    )
                )
            )
        );
    }

    private static Stream<Media<?>> media() {
        return Stream.of(
            new MdJson(), new MdXml("schools"), new MdXslt(new MdXml("schools"), xml -> xml)
        );
    }
}
