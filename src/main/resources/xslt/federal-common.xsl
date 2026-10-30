<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:import href="web-common.xsl"/>
    <xsl:template name="school-navigation"/>

    <xsl:template name="sidebar">
        <aside class="sidebar" id="sidebar">
            <nav class="sidebar-nav">
                <div class="sidebar-section">Федеральный стандарт</div>
                <a class="sidebar-link"
                   href="/federal/curriculums"
                   title="Федеральные планы">
                    <svg xmlns="http://www.w3.org/2000/svg"
                         fill="none"
                         stroke="currentColor"
                         stroke-width="2"
                         viewBox="0 0 24 24">
                        <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>
                        <path d="M4 4.5A2.5 2.5 0 0 1 6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5z"/>
                        <path d="M8 7h8"/>
                        <path d="M8 11h8"/>
                        <path d="M8 15h5"/>
                    </svg>
                    <span class="sidebar-link-label">Планы</span>
                </a>

                <a class="sidebar-link" href="/federal/subjects" title="Предметы">
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" stroke="currentColor"
                         stroke-width="2" viewBox="0 0 24 24">
                        <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>
                        <path d="M4 4.5A2.5 2.5 0 0 1 6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5z"/>
                    </svg>
                    <span class="sidebar-link-label">Предметы</span>
                </a>
                <xsl:if test="/surok/user/admin = 'true'">
                    <div class="sidebar-section">Общее</div>
                    <a class="sidebar-link" title="Школы">
                        <xsl:attribute name="href">
                            <xsl:value-of select="/surok/schools/href"/>
                        </xsl:attribute>
                        <svg xmlns="http://www.w3.org/2000/svg"
                             fill="none"
                             stroke="currentColor"
                             stroke-width="2"
                             viewBox="0 0 24 24">
                            <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
                            <polyline points="9 22 9 12 15 12 15 22"/>
                        </svg>
                        <span class="sidebar-link-label">Школы</span>
                    </a>
                </xsl:if>

                <xsl:call-template name="school-navigation"/>

            </nav>
        </aside>
    </xsl:template>
</xsl:stylesheet>
