/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.media;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Printable;

/**
 * HTML media produced by an XML document and an XSLT transformation.
 *
 * @since 0.0.1
 */
public final class MdXslt implements Media<String> {

    /** XML media. */
    private final Media<String> xml;

    /** XSLT transformation. */
    private final Xslt xslt;

    /**
     * New XSLT media.
     *
     * @param xml XML media
     * @param xslt XSLT transformation
     */
    public MdXslt(final Media<String> xml, final Xslt xslt) {
        this.xml = xml;
        this.xslt = xslt;
    }

    @Override
    public MdXslt attribute(final String name, final String value) {
        return new MdXslt(this.xml.attribute(name, value), this.xslt);
    }

    @Override
    public MdXslt attribute(final String name, final Long value) {
        return new MdXslt(this.xml.attribute(name, value), this.xslt);
    }

    @Override
    public MdXslt attribute(final String name, final Integer value) {
        return new MdXslt(this.xml.attribute(name, value), this.xslt);
    }

    @Override
    public MdXslt attribute(final String name, final Boolean value) {
        return new MdXslt(this.xml.attribute(name, value), this.xslt);
    }

    @Override
    public MdXslt with(final String name, final String value) {
        return new MdXslt(this.xml.with(name, value), this.xslt);
    }

    @Override
    public MdXslt with(final String name, final Long value) {
        return new MdXslt(this.xml.with(name, value), this.xslt);
    }

    @Override
    public MdXslt with(final String name, final Integer value) {
        return new MdXslt(this.xml.with(name, value), this.xslt);
    }

    @Override
    public MdXslt with(final String name, final Boolean value) {
        return new MdXslt(this.xml.with(name, value), this.xslt);
    }

    @Override
    public MdXslt with(final String name, final Printable value)
        throws Exception {
        return new MdXslt(this.xml.with(name, value), this.xslt);
    }

    @Override
    public MdXslt with(final Printable value) throws Exception {
        return new MdXslt(this.xml.with(value), this.xslt);
    }

    @Override
    public MdXslt include(final String... names) {
        return new MdXslt(this.xml.include(names), this.xslt);
    }

    @Override
    public String print() {
        return this.xslt.transform(this.xml.print());
    }
}
